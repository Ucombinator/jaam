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
    ??? // TODO:Petey
  }

  def writeAddrs(unit: Unit): Set[TaintAddress] = {
    ??? // TODO:Petey
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
