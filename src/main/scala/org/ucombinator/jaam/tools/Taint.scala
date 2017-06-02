package org.ucombinator.jaam.tools

import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.PrintStream
import java.util.zip.ZipInputStream

import scala.collection.JavaConversions._
import scala.collection.immutable
import scala.collection.mutable

import scala.language.postfixOps

import soot.{Unit => SootUnit, _}
import soot.jimple.{Stmt => SootStmt, _}
import soot.options.Options

import org.ucombinator.jaam.serializer._

object Taint {
  private var _returnsMap = Map.empty[SootMethod, Set[ReturnStmt]]
  private var _taintGraph = Map.empty[TaintAddress, Set[TaintAddress]]

  def getByIndex(sootMethod : SootMethod, index: Int) : SootStmt = {
    assert(index >= 0, "index must be nonnegative")
    val units = sootMethod.retrieveActiveBody.getUnits.toList
    assert(index < units.length, "index must not overflow the list of units")
    val unit = units(index)
    assert(unit.isInstanceOf[SootStmt], "the index specifies a Soot Unit that is not a Stmt. It is a " + unit.getClass)
    unit.asInstanceOf[SootStmt]
  }

  def getAllClasses(classpath: String): Set[String] = {
    def fileToClassName(fn: String): String = {
      val fqn = fn.replace('/', '.')
      fqn.substring(0, fqn.length - ".class".length)
    }
    def getDirClasses(d: File): Set[String] = {
      if (d.exists) {
        if (d.isDirectory) {
          d.listFiles.toSet flatMap getDirClasses
        } else {
          if (d.getName.endsWith(".class")) {
            Set(fileToClassName(d.getName))
          } else Set.empty
        }
      } else Set.empty
    }
    def getJarClasses(j: File): Set[String] = {
      val zip = new ZipInputStream(new FileInputStream(j))
      var result: Set[String] = Set.empty[String]
      var entry = zip.getNextEntry
      while (entry != null) {
        if (!entry.isDirectory && entry.getName.endsWith(".class")) {
          val className = fileToClassName(entry.getName)
          result = result + className
        }
        entry = zip.getNextEntry
      }
      result
    }
    classpath.split(":").toSet flatMap { (path: String) =>
      val f = new File(path)
      if (path.endsWith(".jar")) {
        getJarClasses(f)
      } else {
        getDirClasses(f)
      }
    }
  }

  // TODO implement implicit flows
  def run(className: String, method: String, instruction: Int,
      implicitFlows: Boolean, classpath: String, output: PrintStream) {
    Options.v().set_verbose(false)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_include_all(true)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    Options.v().set_soot_classpath(classpath)
    Options.v().set_prepend_classpath(false)
    Options.v().set_src_prec(Options.src_prec_only_class)
    // TODO I just copied this from Coverage2
    Options.v().set_whole_program(true)
    soot.Main.v().autoSetOptions()
    Scene.v.setSootClassPath(classpath)
    Scene.v().loadNecessaryClasses()

    // val mName = className + "." + method
    val clazz = Scene.v().forceResolve(className, SootClass.BODIES)
    val m = Coverage2.freshenMethod(clazz.getMethod(method))
    m.retrieveActiveBody()
    val stmt = getByIndex(m, instruction)
    val addrs: Set[TaintAddress] = stmt match {
      case sootStmt: IfStmt => addrsOf(sootStmt.getCondition, m)
      case sootStmt: SwitchStmt => addrsOf(sootStmt.getKey, m)
      case _ =>
        // TODO should we use some standard output method for this?
        println("WARNING: No condition found at the specified statement.")
        Set.empty
    }

    def getInvocations(expr: Value): Set[InvokeExpr] = {
      expr match {
        case unop : UnopExpr => getInvocations(unop.getOp)
        case binop : BinopExpr =>
          getInvocations(binop.getOp1) ++
            getInvocations(binop.getOp2)
        case io : InstanceOfExpr => getInvocations(io.getOp)
          // TODO this could throw an exception
        case cast : CastExpr => getInvocations(cast.getOp)
        case invoke : InvokeExpr =>
          Set(invoke)
        case _ => Set.empty
      }
    }

    def getInvokeExprs(stmt: SootStmt): Set[InvokeExpr] = {
      stmt match {
        case sootStmt : DefinitionStmt =>
          getInvocations(sootStmt.getRightOp) ++
            getInvocations(sootStmt.getLeftOp)
        case sootStmt : InvokeStmt =>
          getInvocations(sootStmt.getInvokeExpr)
        case sootStmt : IfStmt =>
          getInvocations(sootStmt.getCondition)
        case sootStmt : SwitchStmt =>
          getInvocations(sootStmt.getKey)
        // this is only true for intraprocedural
        case sootStmt : ReturnStmt =>
          getInvocations(sootStmt.getOp)
        case sootStmt : EnterMonitorStmt =>
          getInvocations(sootStmt.getOp)
        case sootStmt : ExitMonitorStmt =>
          getInvocations(sootStmt.getOp)
        case _ : ReturnVoidStmt => Set.empty
        case _ : NopStmt => Set.empty
        case _ : GotoStmt => Set.empty
        // TODO
        // Set(RefTaintAddress(CaughtExceptionRef))
        case sootStmt : ThrowStmt =>
          getInvocations(sootStmt.getOp)
        case _ =>
          println(stmt)
          ???
      }
    }

    // This block of code performs taint propagations across method boundaries.
    // When it calls updateTaintGraph, it does not call graph(). This allows
    // taint propagation to call graph lazily, potentially saving lots of
    // computation.
    val classNames = getAllClasses(classpath)
    for {
      clazz <- classNames.map(Scene.v.forceResolve(_, SootClass.BODIES))
      method <- clazz.getMethods map { Coverage2.freshenMethod(_) }
    } {
      var units = List.empty[SootUnit]
      try {
        units = method.retrieveActiveBody.getUnits.toList
      } catch {
        case _: RuntimeException => {}
      }
      for {
        unit <- units
      } {
        unit match {
          case stmt: SootStmt =>
            val iExprs = getInvokeExprs(stmt)
            for {
              iExpr <- iExprs
              called <- LoopDepthCounter.getDispatchedMethods(iExpr) map Coverage2.freshenMethod
            } {
              // track propagation of arguments from invocation
              for {
                index <- Range(0, iExpr.getArgCount)
              } {
                val arg = iExpr.getArg(index)
                val from = addrsOf(arg, method)
                val to = ParameterTaintAddress(called, index)
                updateTaintGraph(to, from, false)
              }

              // track propagation of receiver
              iExpr match {
                case iie: InstanceInvokeExpr =>
                  val from = addrsOf(iie.getBase, method)
                  val to = ThisRefTaintAddress(called)
                  updateTaintGraph(to, from, false)
                case _ => {}
              }

              // track propagation from returns
              val ita = InvokeTaintAddress(method, iExpr)
              val returns = getReturns(called)
              val returnAddrs: Set[TaintAddress] = returns flatMap {
                // note that ReturnVoidStmt can be ignored
                (r: ReturnStmt) =>
                  addrsOf(r.getOp, called)
              }
              updateTaintGraph(ita, returnAddrs, false)
            }
          case _ =>
            Console.withOut(System.out) {
              println("WARNING: expected SootStmt but got " + unit.getClass)
          }
        }
      }
    }
    

    Console.withOut(output) {
      printOrigins(addrs)
    }
    
    /*
    for {
      (to, froms) <- _taintGraph
      from <- froms
    } println(dotString(to) + " -> " + dotString(from))
     */
  }

  def getReturns(m: SootMethod): Set[ReturnStmt] = {
    _returnsMap.get(m) match {
      case Some(returns) => returns
      case None =>
        var units = Set.empty[SootUnit]
        try {
          units = m.retrieveActiveBody.getUnits.toSet
        } catch {
          case _: RuntimeException => {}
        }
        val result = units flatMap { (unit: SootUnit) =>
          unit match {
            case r: ReturnStmt => Some(r)
            case _ => None
          }
        }
        _returnsMap = _returnsMap + (m -> result)
        result
    }
  }

  def constString(const: Constant): String = {
    const match {
      case c: ClassConstant => c.getValue
      case m: MethodHandle => m.getMethodRef.name
      case _: NullConstant => "null"
      case s: StringConstant => "\\\"" + s.value + "\\\""
      case _ => const.toString
    }
  }

  def dotString(addr: TaintAddress): String = {
    addr match {
      case LocalTaintAddress(m, local) =>
        "\"Local[" + local + ", " + fqn(m) + "]\""
      case ThisRefTaintAddress(m) => "\"This[" + fqn(m) + "]\""
      case RefTaintAddress(m, ref) => "\"Ref[" + ref + ", " + fqn(m) + "]\""
      case ParameterTaintAddress(m, index) =>
        "\"Param[" + index + ", " + fqn(m) + "]\""
      // case ConstantTaintAddress(m) =>
        // "\"Const[" + fqn(m) + "]\""
      case ConstantTaintAddress(m, c) =>
        "\"Const[" + constString(c) + ", " + fqn(m) + "]\""
      case InvokeTaintAddress(m, ie) =>
        "\"Invoke[" + ie.getMethod + ", " + fqn(m) + "]\""
      case _ => "\"NO DOT STRING\""
    }
  }

  private def skipIntermediates(addrs: Set[TaintAddress],
      fakes: Set[TaintAddress] = Set.empty): Set[TaintAddress] = {
    val (real, fake) = addrs partition {
      case a@ (_: InvokeTaintAddress | _: ParameterTaintAddress) =>
        readTaintGraph(a) isEmpty
      case _ => true
    }
    if (fake isEmpty) {
      real
    } else {
      val next = (fake -- fakes) flatMap readTaintGraph
      val skipped = skipIntermediates(next, fakes ++ fake)
      real ++ skipped
    }
  }
  def printOrigins(queue: Set[TaintAddress]): Unit = {
    def innerPrint(queue: Set[TaintAddress], seen: Set[TaintAddress]): Unit = {
      if (queue.nonEmpty) {
        val current = queue.head
        val rest = queue.tail
        if (seen contains current) {
          innerPrint(rest, seen)
        } else {
          val immediates = readTaintGraph(current)
          val successors = skipIntermediates(immediates)
          for {
            succ <- successors
          } println(dotString(current) + " -> " + dotString(succ) + ";")
          innerPrint(rest ++ successors, seen + current)
        }
      }
    }
    println("digraph origins {")
    for {
      root <- queue
    } println(dotString(root) + " [shape=box];")
    innerPrint(queue, Set.empty)
    println("}")
  }

  // TODO petey/michael: is InvokeExpr the only expr with side effects?
  def addrsOf(expr: Value, m: SootMethod): Set[TaintAddress] = {
    expr match {
      case l : Local => Set(LocalTaintAddress(m, l))
      // TODO this could throw an exception
      case pr: ParameterRef => Set(ParameterTaintAddress(m, pr.getIndex))
      case t: ThisRef => Set(ThisRefTaintAddress(m))
      case r : Ref => Set(RefTaintAddress(m, r))
      // case _ : Constant => Set(ConstantTaintAddress(m))
      case c : Constant => Set(ConstantTaintAddress(m, c))
      case unop : UnopExpr => addrsOf(unop.getOp, m)
      case binop : BinopExpr =>
        // TODO in the case of division, this could throw an exception
        addrsOf(binop.getOp1, m) ++
          addrsOf(binop.getOp2, m)
      case io : InstanceOfExpr => addrsOf(io.getOp, m)
        // TODO this could throw an exception
      case cast : CastExpr => addrsOf(cast.getOp, m)
      case invoke : InvokeExpr => Set(InvokeTaintAddress(m, invoke))
      case na : NewArrayExpr =>
        addrsOf(na.getSize, m)
      case _ : NewExpr => Set.empty
      case nma : NewMultiArrayExpr =>
        nma.getSizes.toSet flatMap { (exp: Value) => addrsOf(exp, m) }
      case _ =>
        println(expr)
        ???
    }
  }

  def fqn(method: SootMethod): String = {
    method.getDeclaringClass.getName + "." + method.getName
  }

  // This variable keeps track of which methods have already been graphed to
  // prevent infinite looping.
  private var graphed = Set.empty[SootMethod]
  // This method updates _taintGraph, the global map from addresses to the
  // addresses that affect them.
  // It also calls graph(), which allows us to lazily propagate taints only for
  // addresses reachable from the loop condition we're analyzing.
  def updateTaintGraph(to: TaintAddress, from: Set[TaintAddress],
      shouldGraph: Boolean = true): Unit = {
    // Most of the time, we check to make sure the method in question has been
    // graphed before anything else.
    if (shouldGraph) {
      graph(Coverage2.freshenMethod(to.m))
    }
    // Constants can't store explicit taints. Also, their current construction
    // makes two instances of the same value in the same method equivalent,
    // which would create spurious taint flows.
    to match {
      case _: ConstantTaintAddress => {}
      case _ =>
        val newFrom = _taintGraph.getOrElse(to, Set.empty) ++ from
        _taintGraph = _taintGraph + (to -> newFrom)
    }
  }
  // This method reads from _taintGraph. Like updateTaintGraph, it calls
  // graph() for lazy taint propagation.
  def readTaintGraph(to: TaintAddress): Set[TaintAddress] = {
    // Make sure that the method being read has been graphed.
    graph(Coverage2.freshenMethod(to.m))
    to match {
      case _: ConstantTaintAddress => Set.empty
      case _ => _taintGraph.getOrElse(to, Set.empty)
    }
  }
  // This is the primary workhorse function. It takes a method and adds nodes
  // and edges to _taintGraph (via updateTaintGraph) for its data flows.
  def graph(method: SootMethod): Unit = {
    if (!graphed.contains(method)) {
      // Mark this method as graphed - this must happen before recurring!!
      graphed = graphed + method

      try {
        val units = method.retrieveActiveBody.getUnits.toList
        var index = 0;
        Console.withOut(System.out) {
          println()
          println("graphing " + fqn(method))
        }

        for (unit <- units) {
          /*
           * This commented block is useful for debugging; it prints Jimple code
           * and instruction numbers.
          Console.withOut(System.out) {
            println(index + ":\t" + unit)
            index = index+1
          }
           */

          unit match {
            case sootStmt : DefinitionStmt =>
              val from = addrsOf(sootStmt.getRightOp, method)
              for {
                addr <- addrsOf(sootStmt.getLeftOp, method)
              } updateTaintGraph(addr, from)
            case sootStmt : InvokeStmt =>
              addrsOf(sootStmt.getInvokeExpr, method)
            case sootStmt : IfStmt =>
              addrsOf(sootStmt.getCondition, method)
            case sootStmt : SwitchStmt =>
              addrsOf(sootStmt.getKey, method)
            // this is only true for intraprocedural
            case sootStmt : ReturnStmt =>
              addrsOf(sootStmt.getOp, method)
            case sootStmt : EnterMonitorStmt =>
              addrsOf(sootStmt.getOp, method)
            case sootStmt : ExitMonitorStmt =>
              addrsOf(sootStmt.getOp, method)
            case _ : ReturnVoidStmt => {}
            case _ : NopStmt => {}
            case _ : GotoStmt => {}
            // TODO
            // Set(RefTaintAddress(CaughtExceptionRef))
            case sootStmt : ThrowStmt =>
              addrsOf(sootStmt.getOp, method)
            case _ =>
              println(unit)
              ???
          }
        }
      } catch {
        // if no active body can be created, assume it's library code
        case _: RuntimeException => {}
      }
    }
  }
}

