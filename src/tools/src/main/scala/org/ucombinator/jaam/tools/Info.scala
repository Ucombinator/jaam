package org.ucombinator.jaam.tools

import java.io.FileInputStream
import scala.collection.mutable

import org.ucombinator.jaam.serializer._

class Info extends Main("info") {
  banner("Get simple information about a JAAM interpretation.")
  footer("")

  val file = trailArg[java.io.File](descr = "a .jaam file to be analyzed")

  def run(conf: Conf) {
    Info.analyzeForInfo(file().toString)
  }
}

object Info {
  private val uniqueNodes = mutable.Map.empty[Id[Node], Node]
  private val uniqueEdges = mutable.Map.empty[Id[Edge], Edge]
  private val missingNodes = mutable.Map.empty[Id[Node], Int]
  private var hangingEdges = mutable.MutableList.empty[Id[Edge]]

  def analyzeForInfo(jaamFile : String) = {
    val stream = new FileInputStream(jaamFile)
    val pi = new PacketInput(stream)
    var packet: Packet = null
    var firstState: State = null
    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      packet match {
        case p : Node =>
          uniqueNodes(p.id) = p
          if (firstState == null) {
            p match {
              case s : State => firstState = s
              case _ => ()
            }
          }
        case p : Edge => uniqueEdges(p.id) = p
      }
    }
    countHangingEdgesAndFindMissingStates()
    printInfo(jaamFile, firstState)
  }

  private def nodeExists(id : Id[Node]) : Boolean = {
    if (! uniqueNodes.contains(id)) {
      missingNodes(id) += 1
      return false
    }
    true
  }

  private def countHangingEdgesAndFindMissingStates() = {
    for (edge <- uniqueEdges.values) {
      if (! nodeExists(edge.src) || ! nodeExists(edge.dst)) {
        hangingEdges += edge.id
      }
    }
  }

  private def printInfo(file : String, initialState : State) = {
    val states = uniqueNodes.size
    val edges = uniqueEdges.size
    val missingStates = missingNodes.size
    println("Info for " + file)
    println("    # of States: " + states)
    println("    # of Edges: " + edges)
    println("    # of Missing States: " + missingStates)
    if (missingStates > 0) {
      println("        " +
        (for ((missingNode, references) <- missingNodes)
          yield missingNode.id + " (" + references + ")").toArray.mkString(", "))
    }
    println("    # of Missing State References: " + missingNodes.foldLeft(0)(_ + _._2))
    println("    # of Hanging Edges: " + hangingEdges.size)
    if (hangingEdges.size > 0) {
      println("        " + hangingEdges.toArray.mkString(", "))
    }
    if (initialState != null) {
      println("    Initial State:")
      println("        sootMethod: " + initialState.stmt.method.toString)
      println("        sootStmt: " + initialState.stmt.stmt.toString())
    }
  }
}
