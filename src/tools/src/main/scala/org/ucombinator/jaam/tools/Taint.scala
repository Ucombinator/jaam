package org.ucombinator.jaam.tools

import scala.collection.JavaConversions._

import scala.collection.immutable
import scala.collection.mutable

import soot._
import soot.jimple._

import org.ucombinator.jaam.serializer._

class Taint extends Main("taint") {
  banner("Identify explicit intra-procedural information flows in a method")
  footer("")

  // TODO: specify required options
  val className = opt[String](descr = "TODO:document")
  val method = opt[String](descr = "TODO:document")
  val implicitFlows = opt[Boolean](descr = "TODO:implement", default = Some(false))
  val file = trailArg[java.io.File](descr = "a .jaam file to be printed")

  def run(conf: Conf) {
    Taint.run(className(), method(), implicitFlows(), file())
  }
}

object Taint {
  def run(className: String, method: String, implicitFlows: Boolean, file: java.io.File) {
    ??? // TODO:Petey
  }

  def readAddrs(unit: Unit): Set[TaintAddress] = {
    unit match {
      case sootStmt : InvokeStmt => addrsOf(sootStmt.getInvokeExpr)
      case sootStmt : DefinitionStmt =>
        addrsOf(sootStmt.getRightOp)
        // TODO is it possible to read something from the lhs that matters?
        // addrsOf(sootStmt.getLeftOp) ++ addrsOf(sootStmt.getRightOp)
      case sootStmt : IfStmt => addrsOf(sootStmt.getCondition)
      case sootStmt : SwitchStmt => addrsOf(sootStmt.getKey)
      case sootStmt : ReturnStmt => addrsOf(sootStmt.getOp())
      case _ : ReturnVoidStmt => Set.empty
      case _ : NopStmt => Set.empty
      case _ : GotoStmt => Set.empty
      case sootStmt : EnterMonitorStmt => addrsOf(sootStmt.getOp)
      case sootStmt : ExitMonitorStmt => addrsOf(sootStmt.getOp)
      case sootStmt : ThrowStmt => addrsOf(sootStmt.getOp)
      case _ => ???
    }
  }

  def addrsOf(expr: Value): Set[TaintAddress] = {
    expr match {
      case l : Local => Set(LocalTaintAddress(l))
      // TODO this could throw an exception
      case r : Ref => Set(RefTaintAddress(r))
      case _ : Constant => Set.empty
      case unop : UnopExpr => addrsOf(unop.getOp)
      case binop : BinopExpr =>
        // TODO in the case of division, this could throw an exception
        addrsOf(binop.getOp1) ++ addrsOf(binop.getOp2)
      case io : InstanceOfExpr => addrsOf(io.getOp)
        // TODO this could throw an exception
      case cast : CastExpr => addrsOf(cast.getOp)
      case _ => ???
    }
  }

  def writeAddrs(unit: Unit): Set[TaintAddress] = {
    unit match {
      case sootStmt : DefinitionStmt => addrsOf(sootStmt.getLeftOp)
      case _ : InvokeStmt => Set.empty
      case _ : IfStmt => Set.empty
      case _ : SwitchStmt => Set.empty
      // this is only true for intraprocedural
      case _ : ReturnStmt => Set.empty
      case _ : ReturnVoidStmt => Set.empty
      case _ : NopStmt => Set.empty
      case _ : GotoStmt => Set.empty
      case sootStmt : EnterMonitorStmt => addrsOf(sootStmt.getOp)
      case sootStmt : ExitMonitorStmt => addrsOf(sootStmt.getOp)
      case _ : ThrowStmt => Set.empty
        // TODO
        // Set(RefTaintAddress(CaughtExceptionRef))
      case _ => ???
    }
  }

  def taintGraph(method: SootMethod): immutable.Map[TaintAddress, Set[TaintAddress]] = {
    val taintStore = mutable.Map[TaintAddress, Set[TaintAddress]]()

    for (unit <- method.getActiveBody.getUnits.toList) {
      val read = readAddrs(unit)
      for (addr <- writeAddrs(unit)) {
        taintStore(addr) ++= read
      }
    }

    taintStore.toMap
  }

  def propagateTaints(
    graph: immutable.Map[TaintAddress, Set[TaintAddress]],
    initialStore: immutable.Map[TaintAddress, Set[TaintValue]]):
      immutable.Map[TaintAddress, Set[TaintValue]] = {
    val taintStore = mutable.Map(initialStore.toSeq: _*)

    taintStore.toMap
  }

/*
    //def update(to: TaintAddress, from: TaintValue) { update(to, Set(from)) }

    def update(to: Set[TaintAddress], from: Set[TaintValue]) {
      for (addr <- to) {
        update(to, from)
      }
    }

    def update(to: TaintAddress, from: Set[TaintValue]) {
      taintStore.get(to) match {
        case None =>
          modified = true
          taintStore(to) = from
        case Some(addrs) =>
          if (!from.subsetOf(addrs)) {
            modified = true
            taintStore(to) ++= from
          }
      }
    }


    val units = method.getActiveBody.getUnits.toList

    val taintStore = mutable.Map[TaintAddress, Set[TaintValue]]()

    for (unit <- units) {
      for (addr <- readAddrs(unit)) update(addr, Set(addr))
      for (addr <- writeAddrs(unit)) update(addr, Set(addr))
    }

    var modified = true

    while (modified) {
      modified = false

      for (unit <- units) {
        update(writeAddrs(unit), readAddrs(unit).asInstanceOf[Set[TaintValue]])
      }
    }

    taintStore.toMap
 */
}

abstract sealed class TaintValue

abstract sealed class TaintAddress extends TaintValue
case class LocalTaintAddress(local: Local) extends TaintAddress
case class RefTaintAddress(ref: Ref) extends TaintAddress
