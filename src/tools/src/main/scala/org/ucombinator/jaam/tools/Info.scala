package org.ucombinator.jaam.tools

import java.io.FileInputStream
import scala.collection.mutable

import org.ucombinator.jaam.serializer._

object Info {
  private val uniqueNodes = mutable.Map.empty[Id[Node], Node]
  private val uniqueEdges = mutable.Map.empty[Id[Edge], Edge]
  private val missingNodes = mutable.Set.empty[Id[Node]]
  private var hangingEdges = 0

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
      missingNodes.add(id)
      return false
    }
    true
  }

  private def countHangingEdgesAndFindMissingStates() = {
    for (edge <- uniqueEdges.values) {
      if (! nodeExists(edge.src) || ! nodeExists(edge.dst)) {
        hangingEdges += 1
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
    println("    # of Hanging Edges: " + hangingEdges)
    if (initialState != null) {
      println("    Initial State:")
      println("        class: " + initialState.stmt.method.getDeclaringClass.toString)
      println("        method: " + initialState.stmt.method.getName)
      println("        sootMethod: " + initialState.stmt.method.toString)
      println("        sootStmt: " + initialState.stmt.stmt.toString())
    }
  }
}
