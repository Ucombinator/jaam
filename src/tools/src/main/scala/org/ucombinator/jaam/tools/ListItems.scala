package org.ucombinator.jaam.tools

import java.io.FileInputStream
import scala.collection.mutable

import org.ucombinator.jaam.serializer._

case class ListPrintOption(classes: Boolean, methods: Boolean, full: Boolean)

object ListItems {
  def printClass(c: soot.SootClass, full: Boolean): Unit = {
    if (full) {
      println(c)
    } else {
      println(c.getShortName)
    }
  }

  def printMethod(m: soot.SootMethod, classes: Boolean, full: Boolean): Unit = {
    var s = ""
    if (classes) {
      s += "  "
    }
    if (full) {
      s += m
    } else {
      s += m.getName
    }
    println(s)
  }

  def main(jaamFile: String, printOption: ListPrintOption): Unit = {
    val mapping = mutable.HashMap.empty[soot.SootClass, mutable.HashSet[soot.SootMethod]]

    val stream = new FileInputStream(jaamFile)
    val pi = new PacketInput(stream)
    var packet: Packet = null
    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      packet match {
        case n: Node => n match {
          case s: State =>
            val m = s.stmt.method
            val c = m.getDeclaringClass
            mapping.get(c) match {
              case Some(l) => l += m
              case None => mapping += (c -> mutable.HashSet(m))
            }
          case _ => ()
        }
        case _ => ()
      }
    }
    pi.close()
    stream.close()

    for ( (c, l) <- mapping) {
      if (printOption.classes) {
        printClass(c, printOption.full)
      }
      if (printOption.methods) {
        l.foreach(printMethod(_, printOption.classes, printOption.full))
      }
    }
  }
}
