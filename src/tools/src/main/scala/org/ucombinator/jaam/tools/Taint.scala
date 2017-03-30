package org.ucombinator.jaam.tools

import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.PrintStream

import scala.collection.JavaConversions._

import scala.collection.immutable
import scala.collection.mutable

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
  val jaamFile = opt[String](descr = "jaam file with a call graph",
      required = true)
  val rtJar = opt[String](descr = "The RT.jar file to use for analysis",
      default = Some("resources/rt.jar"), required = true)

  def run(conf: Conf) {
    val classpath = path.toOption match {
      case Some(str) => rtJar() + ":" + str
      case None => rtJar()
    }
    Taint.run(className(), method(), instruction(), implicitFlows(),
        classpath, jaamFile(), output.toOption)
  }
}

object Taint {
  def getByIndex(sootMethod : SootMethod, index: Int) : SootStmt = {
    assert(index >= 0, "index must be nonnegative")
    val units = Soot.getBody(sootMethod).getUnits().toList
    assert(index < units.length, "index must not overflow the list of units")
    /*
    for {
      (unit, index) <- units.zipWithIndex
    } println(index + ":\t" + unit)
    */
    val unit = units(index)
    assert(unit.isInstanceOf[SootStmt], "the index specifies a Soot Unit that is not a Stmt. It is a " + unit.getClass)
    unit.asInstanceOf[SootStmt]
  }

  // TODO implement implicit flows
  def run(className: String, method: String, instruction: Int,
      implicitFlows: Boolean, classpath: String, jaamFile: String,
      output: Option[java.io.File]) {
    Options.v().set_verbose(true)
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
    val mainClass = Scene.v().loadClassAndSupport(className)
    Scene.v().setMainClass(mainClass)
    Scene.v().loadNecessaryClasses()

    // val mName = className + "." + method
    val clazz = Scene.v().forceResolve(className, SootClass.BODIES)
    val m = clazz.getMethod(method)
    m.retrieveActiveBody()
    val stmt = getByIndex(m, instruction)
    val addrs: Set[TaintAddress] = stmt match {
      case sootStmt: IfStmt => addrsOf(sootStmt.getCondition, m, None)
      case sootStmt: SwitchStmt => addrsOf(sootStmt.getKey, m, None)
      case _ =>
        // TODO should we use some standard output method for this?
        println("WARNING: No condition found at the specified statement.")
        Set.empty
    }

    /*
    var reverseCallGraph = Map.empty[SootMethod, Set[SootMethod]]
    val pi = new PacketInput(new FileInputStream(jaamFile))
    var packet: Packet = null
    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      packet match {
        case s: State =>
          val m = Coverage2.freshenMethod(s.stmt.method)
          // reverseCallGraph
        case _ => {}
      }
    }
     */

    println(getReturns(m))
    val graph = taintGraph(m)
    output match {
      case None =>
        printOrigins(graph, addrs)
      case Some(file) =>
        Console.withOut(new PrintStream(new FileOutputStream(file))) {
          printOrigins(graph, addrs)
        }
    }
  }

  def getReturns(m: SootMethod): Set[SootStmt] = {
    Soot.getBody(m).getUnits().toSet flatMap { (unit: SootUnit) =>
      unit match {
        case r: ReturnStmt => Some[SootStmt](r)
        case _ => None
      }
    }
  }

  def dotString(addr: TaintAddress): String = {
    addr match {
      case LocalTaintAddress(m, local) =>
        "\"Local[" + local + ", " + m.getName + "]\""
      case RefTaintAddress(m, ref) => "\"Ref[" + ref + ", " + m.getName + "]\""
      case ParameterTaintAddress(m, index) =>
        "\"Param[" + m + ", " + index + "]\""
      case ReturnTaintAddress(m, ie) =>
        "\"Return[" + ie.getMethod.getName + "]\""
    }
  }

  def printOrigins(graph: Map[TaintAddress, Set[TaintAddress]],
      queue: Set[TaintAddress]): Unit = {
    def innerPrint(queue: Set[TaintAddress], seen: Set[TaintAddress]): Unit = {
      if (queue.nonEmpty) {
        val current = queue.head
        val rest = queue.tail
        if (seen contains current) {
          innerPrint(rest, seen)
        } else {
          val immediates = graph.getOrElse(current, Set.empty)
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
  def addrsOf(expr: Value, m: SootMethod,
      taintStore: Option[mutable.Map[TaintAddress, Set[TaintAddress]]]):
      Set[TaintAddress] = {
    expr match {
      case l : Local => Set(LocalTaintAddress(m, l))
      // TODO this could throw an exception
      case r : Ref => Set(RefTaintAddress(m, r))
      case _ : Constant => Set.empty
      case unop : UnopExpr => addrsOf(unop.getOp, m, taintStore)
      case binop : BinopExpr =>
        // TODO in the case of division, this could throw an exception
        addrsOf(binop.getOp1, m, taintStore) ++
          addrsOf(binop.getOp2, m, taintStore)
      case io : InstanceOfExpr => addrsOf(io.getOp, m, taintStore)
        // TODO this could throw an exception
      case cast : CastExpr => addrsOf(cast.getOp, m, taintStore)
      case invoke : InvokeExpr =>
        val target = invoke.getMethod
        taintStore match {
          case None => {}
          case Some(ts) =>
            for {
              index <- Range(0, invoke.getArgCount)
            } {
              val arg = invoke.getArg(index)
              val from = addrsOf(arg, m, taintStore)
              val to = ParameterTaintAddress(target, index)
              ts(to) = ts.getOrElse(to, Set.empty) ++ from
            }
        }
        Set(ReturnTaintAddress(m, invoke))
      case _ =>
        println(expr)
        ???
    }
  }

  private var taintGraphs = Map.empty[SootMethod,
          Map[TaintAddress, Set[TaintAddress]]]
  def taintGraph(method: SootMethod):
      immutable.Map[TaintAddress, Set[TaintAddress]] = {
    taintGraphs.get(method) match {
      case Some(g) => g
      case None =>
        val graph = mutable.Map[TaintAddress, Set[TaintAddress]]()

        for (unit <- method.getActiveBody.getUnits.toList) {
          unit match {
            case sootStmt : DefinitionStmt =>
              val from = addrsOf(sootStmt.getRightOp, method, Some(graph))
              for {
                addr <- addrsOf(sootStmt.getLeftOp, method, Some(graph))
              } graph(addr) = graph.getOrElse(addr, Set.empty) ++ from
            case sootStmt : InvokeStmt =>
              addrsOf(sootStmt.getInvokeExpr, method, Some(graph))
            case sootStmt : IfStmt =>
              addrsOf(sootStmt.getCondition, method, Some(graph))
            case sootStmt : SwitchStmt =>
              addrsOf(sootStmt.getKey, method, Some(graph))
            // this is only true for intraprocedural
            case sootStmt : ReturnStmt =>
              addrsOf(sootStmt.getOp, method, Some(graph))
            case sootStmt : EnterMonitorStmt =>
              addrsOf(sootStmt.getOp, method, Some(graph))
            case sootStmt : ExitMonitorStmt =>
              addrsOf(sootStmt.getOp, method, Some(graph))
            case _ : ReturnVoidStmt => {}
            case _ : NopStmt => {}
            case _ : GotoStmt => {}
            // TODO
            // Set(RefTaintAddress(CaughtExceptionRef))
            case sootStmt : ThrowStmt =>
              addrsOf(sootStmt.getOp, method, Some(graph))
            case _ =>
              println(unit)
              ???
          }
        }

        val tg = graph.toMap
        taintGraphs += (method -> tg)
        tg
    }
  }

  def propagateTaints(
    graph: immutable.Map[TaintAddress, Set[TaintAddress]],
    initialStore: immutable.Map[TaintAddress, Set[TaintValue]]):
      immutable.Map[TaintAddress, Set[TaintValue]] = {
    val taintStore = mutable.Map(initialStore.toSeq: _*)
    var changed : Boolean = true

    while (changed) {
      changed = false
      for {
        (to, froms) <- graph
        from <- froms
      } {
        val taints = taintStore.getOrElse(from, Set.empty)
        val current = taintStore.getOrElse(to, Set.empty)
        if (!taints.subsetOf(current)) {
          changed = true
          taintStore(to) = current ++ taints
        }
      }
    }

    taintStore.toMap
  }

}

abstract sealed class TaintValue

abstract sealed class TaintAddress extends TaintValue
case class LocalTaintAddress(val m: SootMethod, val local: Local)
  extends TaintAddress
case class RefTaintAddress(val m: SootMethod, val ref: Ref) extends TaintAddress
case class ParameterTaintAddress(val target: SootMethod, val index: Int)
  extends TaintAddress
case class ReturnTaintAddress(val m: SootMethod, val ie: InvokeExpr)
  extends TaintAddress
