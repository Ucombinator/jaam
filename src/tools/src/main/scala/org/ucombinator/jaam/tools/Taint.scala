package org.ucombinator.jaam.tools

import scala.collection.JavaConversions._

import scala.collection.immutable
import scala.collection.mutable

import soot._
import soot.jimple._
import soot.options.Options

import org.ucombinator.jaam.serializer._

class Taint extends Main("taint") {
  banner("Identify explicit intra-procedural information flows in a method")
  footer("")

  // TODO: specify required options
  val className = opt[String](descr = "FQN (package and class) of the class being analyzed")
  val method = opt[String](descr = "signature of the method being analyzed; e.g., \"void main(java.lang.String[])\"")
  val instruction = opt[Int](descr = "index into the Unit Chain that identifies the instruction", validate = { _ >= 0 })
  val implicitFlows = opt[Boolean](descr = "TODO:implement", default = Some(false))
  val file = trailArg[java.io.File](descr = "a .jaam file to be printed")
  // really, this just gets used as the class path
  val jars = opt[String](descr = "colon-separated list of jar files")
  val rtJar = opt[String](descr = "The RT.jar file to use for analysis",
      default = Some("resources/rt.jar"), required = true)

  def run(conf: Conf) {
    val classpath = jars.toOption match {
      case Some(str) => rtJar() + ":" + str
      case None => rtJar()
    }
    Taint.run(className(), method(), instruction(), implicitFlows(),
        classpath, file())
  }
}

object Taint {
  // TODO implement implicit flows
  def run(className: String, method: String, instruction: Int,
      implicitFlows: Boolean, jars: String, file: java.io.File) {
    Options.v().set_verbose(true)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_include_all(true)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    Options.v().set_soot_classpath(jars)
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
    val graph = taintGraph(m)
    // val taintStore = propagateTaints(graph, ???) // TODO:Petey
    // Need to figure out what the incoming arguments to the function
    // look like - are they Locals? Refs? - and mark them in an initial
    // taint store.
    println(graph)
  }

  // TODO petey/michael: is InvokeExpr the only expr with side effects?
  def addrsOf(expr: Value,
      taintStore: mutable.Map[TaintAddress, Set[TaintAddress]]):
      Set[TaintAddress] = {
    expr match {
      case l : Local => Set(LocalTaintAddress(l))
      // TODO this could throw an exception
      case r : Ref => Set(RefTaintAddress(r))
      case _ : Constant => Set.empty
      case unop : UnopExpr => addrsOf(unop.getOp, taintStore)
      case binop : BinopExpr =>
        // TODO in the case of division, this could throw an exception
        addrsOf(binop.getOp1, taintStore) ++ addrsOf(binop.getOp2, taintStore)
      case io : InstanceOfExpr => addrsOf(io.getOp, taintStore)
        // TODO this could throw an exception
      case cast : CastExpr => addrsOf(cast.getOp, taintStore)
      case invoke : InvokeExpr =>
        val target = invoke.getMethod
        for {
          index <- Range(0, invoke.getArgCount)
        } {
          val arg = invoke.getArg(index)
          val from = addrsOf(arg, taintStore)
          val to = ParameterTaintAddress(target, index)
          taintStore(to) = taintStore.getOrElse(to, Set.empty) ++ from
        }
        Set(ReturnTaintAddress(invoke))
      case _ =>
        println(expr)
        ???
    }
  }

  def taintGraph(method: SootMethod):
      immutable.Map[TaintAddress, Set[TaintAddress]] = {
    val taintStore = mutable.Map[TaintAddress, Set[TaintAddress]]()

    for (unit <- method.getActiveBody.getUnits.toList) {
      unit match {
        case sootStmt : DefinitionStmt =>
          val from = addrsOf(sootStmt.getRightOp, taintStore)
          for {
            addr <- addrsOf(sootStmt.getLeftOp, taintStore)
          } taintStore(addr) = taintStore.getOrElse(addr, Set.empty) ++ from
        case sootStmt : InvokeStmt =>
          addrsOf(sootStmt.getInvokeExpr, taintStore)
        case sootStmt : IfStmt =>
          addrsOf(sootStmt.getCondition, taintStore)
        case sootStmt : SwitchStmt =>
          addrsOf(sootStmt.getKey, taintStore)
        // this is only true for intraprocedural
        case sootStmt : ReturnStmt =>
          addrsOf(sootStmt.getOp, taintStore)
        case sootStmt : EnterMonitorStmt =>
          addrsOf(sootStmt.getOp, taintStore)
        case sootStmt : ExitMonitorStmt =>
          addrsOf(sootStmt.getOp, taintStore)
        case _ : ReturnVoidStmt => {}
        case _ : NopStmt => {}
        case _ : GotoStmt => {}
        // TODO
        // Set(RefTaintAddress(CaughtExceptionRef))
        case sootStmt : ThrowStmt =>
          addrsOf(sootStmt.getOp, taintStore)
        case _ =>
          println(unit)
          ???
      }
    }

    taintStore.toMap
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
case class LocalTaintAddress(local: Local) extends TaintAddress
case class RefTaintAddress(ref: Ref) extends TaintAddress
case class ParameterTaintAddress(m: SootMethod, index: Int) extends TaintAddress
case class ReturnTaintAddress(ie: InvokeExpr) extends TaintAddress
