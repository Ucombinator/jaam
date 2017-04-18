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

class Taint extends Main("taint") {
  banner("Identify explicit intra-procedural information flows in a method")
  footer("")

  // TODO: specify required options
  val className = opt[String](descr = "FQN (package and class) of the class " +
      "being analyzed")
  val method = opt[String](descr = "signature of the method being analyzed; " +
      "e.g., \"void main(java.lang.String[])\"")
  val instruction = opt[Int](descr = "index into the Unit Chain that identifies"
     + " the instruction", validate = { _ >= 0 })
  val implicitFlows = opt[Boolean](descr = "TODO:implement")
  val output = opt[java.io.File](descr = "a .dot file to be printed")
  // really, this just gets used as the class path
  val path = opt[String](descr = "java classpath (including jar files), " +
      "colon-separated")
  // val rtJar = opt[String](descr = "The RT.jar file to use for analysis",
      // default = Some("resources/rt.jar"), required = true)

  def run(conf: Conf) {
    val classpath = path.toOption match {
      // case Some(str) => rtJar() + ":" + str
      // case None => rtJar()
      case Some(str) => str
      case None => ""
    }
    Taint.run(className(), method(), instruction(), implicitFlows(),
        classpath, output.toOption)
  }
}

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
      implicitFlows: Boolean, classpath: String, output: Option[java.io.File]) {
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

    // initialize invocations map and returns map
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
        if (unit.isInstanceOf[SootStmt]) {
          val stmt: SootStmt = unit.asInstanceOf[SootStmt]
          val iExprs = getInvokeExprs(stmt)
          for {
            iExpr <- iExprs
          } {
            // TODO getMethod needs to consider virtual dispatch
            val called = Coverage2.freshenMethod(iExpr.getMethod)

            for {
              index <- Range(0, iExpr.getArgCount)
            } {
              val arg = iExpr.getArg(index)
              val from = addrsOf(arg, method)
              val to = ParameterTaintAddress(method, index)
              updateTaintGraph(to, from)
            }

            val ita = InvokeTaintAddress(method, iExpr)
            val returns = getReturns(called)
            val returnAddrs: Set[TaintAddress] = returns flatMap {
              (r: ReturnStmt) =>
                addrsOf(r.getOp, called)
            }
            updateTaintGraph(ita, returnAddrs)
          }
        }
      }
    }

    output match {
      case None =>
        printOrigins(addrs)
      case Some(file) =>
        Console.withOut(new PrintStream(new FileOutputStream(file))) {
          printOrigins(addrs)
        }
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

  def printOrigins(queue: Set[TaintAddress]): Unit = {
    def innerPrint(queue: Set[TaintAddress], seen: Set[TaintAddress]): Unit = {
      if (queue.nonEmpty) {
        val current = queue.head
        val rest = queue.tail
        if (seen contains current) {
          innerPrint(rest, seen)
        } else {
          val immediates = readTaintGraph(current)
          for {
            immediate <- immediates
          } println(dotString(current) + " -> " + dotString(immediate) + ";")
          innerPrint(rest ++ immediates, seen + current)
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

  private var graphed = Set.empty[SootMethod]
  def updateTaintGraph(to: TaintAddress, from: Set[TaintAddress]): Unit = {
    graph(Coverage2.freshenMethod(to.m))
    to match {
      case _: ConstantTaintAddress => {}
      case _ =>
        val newFrom = _taintGraph.getOrElse(to, Set.empty) ++ from
        _taintGraph = _taintGraph + (to -> newFrom)
    }
  }
  def readTaintGraph(to: TaintAddress): Set[TaintAddress] = {
    graph(Coverage2.freshenMethod(to.m))
    to match {
      case _: ConstantTaintAddress => Set.empty
      case _ => _taintGraph.getOrElse(to, Set.empty)
    }
  }
  def graph(method: SootMethod): Unit = {
    if (!graphed.contains(method)) {
      graphed = graphed + method

      try {
        val units = method.retrieveActiveBody.getUnits.toList
        var index = 0;
        Console.withOut(System.out) {
          println("graphing " + fqn(method))
          println()
        }

        for (unit <- units) {
          /*
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

abstract sealed class TaintValue

abstract sealed class TaintAddress extends TaintValue {
  val m: SootMethod
}
case class LocalTaintAddress(override val m: SootMethod, val local: Local)
  extends TaintAddress
case class RefTaintAddress(override val m: SootMethod, val ref: Ref)
  extends TaintAddress
case class ParameterTaintAddress(override val m: SootMethod, val index: Int)
  extends TaintAddress
case class ConstantTaintAddress(override val m: SootMethod, c: Constant)
  extends TaintAddress
// case class ConstantTaintAddress(override val m: SootMethod)
  // extends TaintAddress
case class InvokeTaintAddress(override val m: SootMethod, ie: InvokeExpr)
  extends TaintAddress
