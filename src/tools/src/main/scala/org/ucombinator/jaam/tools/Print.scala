package org.ucombinator.jaam.tools

import java.io.FileInputStream
import scala.collection.mutable

import org.ucombinator.jaam.serializer._

object Print {
  var currentID : Int = -1
  val uniqueNodes = mutable.Set.empty[Id[Node]]
  val uniqueEdges = mutable.Set.empty[Id[Edge]]

  def printFile(jaamFile : String) = {
    val stream = new FileInputStream(jaamFile)
    val pi = new PacketInput(stream)
    var packet: Packet = null
    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      packet match {
        case p : Node =>
          currentID = p.id.id
          printNode(p)
        case p : Edge =>
          currentID = p.id.id
          printEdge(p)
      }
    }
    pi.close()
  }

  def printNode(node : Node) = {
    if (! uniqueNodes.contains(node.id)) {
      uniqueNodes.add(node.id)

      node match {
        case n: State => printState(n)
        case n: ErrorState => printErrorState(n)
      }
    }
  }

  def printState(state : State) = {
    printIndentedLine(0, "State")
    printIndentedLine(1, "id: " + state.id.id)
    printIndentedLine(1, "index: " + state.stmt.index)
    printIndentedLine(1, "class: " + state.stmt.method.getDeclaringClass.toString)
    printIndentedLine(1, "method: " + state.stmt.method.getName)
    printIndentedLine(2, "returns: " + state.stmt.method.getReturnType.toString)
    if (state.stmt.method.getParameterCount == 0) {
      printIndentedLine(2, "takes: <none>")
    } else {
      printIndentedLine(2, "takes: " + state.stmt.method.getParameterTypes.toArray.mkString(", "))
    }
    printIndentedLine(1, "soot:")
    printIndentedLine(2, "sootMethod: " + state.stmt.method.toString)
    printIndentedLine(2, "sootStmt: " + state.stmt.stmt.toString())
  }

  def printErrorState(errorState : ErrorState) = {
    printIndentedLine(0, "ErrorState")
    printIndentedLine(1, "id: " + errorState.id.id)
  }

  def printEdge(edge : Edge) = {
    if (! uniqueEdges.contains(edge.id)) {
      uniqueEdges.add(edge.id)

      printIndentedLine(0, "Edge")
      printIndentedLine(1, "id: " + edge.id.id)
      printIndentedLine(1, "from: " + edge.src)
      printIndentedLine(1, "to: " + edge.dst)
    }
  }
  def printIndentedLine(indent : Int, s : String = "") = {
    println(currentID + (if (s == "") "" else " " + ("    " * indent) + s))
  }
}
