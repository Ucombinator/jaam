package org.ucombinator.jaam.json_exporter

/*****************************************
 * Reads .jaam files and outputs the information as JSON.
 * ****************************************/

import java.io.FileInputStream

import org.ucombinator.jaam.serializer._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.collection.mutable.{ListBuffer, Set}
import scala.collection.JavaConversions._

object Main {
  def main(args : Array[String]) = {
    if (args.length != 1) {
      // We need something to read in.
      System.err.println("Takes one argument: a .jaam file to read in.")
    }
    // Assume the argument is the name of a file.
    // TODO: Should probably do some error handling here.
    val jaamFile = args.head
    val jsonList = readJsonsFromFile(jaamFile)
    println(pretty(render(jsonList)))
  }

  // These are used to keep track of unique objects (prevents duplication).
  val uniqueNodes : Set[Id[Node]] = Set.empty[Id[Node]]
  val uniqueEdges : Set[Id[Edge]] = Set.empty[Id[Edge]]

  /**
    * Opens a file (assumed to be a .jaam file) and converts its contents into a
    * list of JValues. This allows each item to be printed separately.
    *
    * @param file a JAAM serialization file to be read
    * @return A list of JValues, each corresponding to a JAAM node.
    */
  def readJsonsFromFile(file : String) : List[JValue] = {
    val stream = new FileInputStream(file)
    val pi = new PacketInput(stream)
    var packet : Packet = null
    val buffer = ListBuffer.empty[JValue]
    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      packet match {
        case p : Node => processNode(p) match {
          case Some(json) => buffer += json
          case None => ()
        }
        case p : Edge => processEdge(p) match {
          case Some(json) => buffer += json
          case None => ()
        }
      }
    }
    pi.close()
    buffer.toList
  }

  /**
    * Takes a node and checks whether that node's ID exists already within this
    * serialization. If it does not, this method returns the JSON serialization
    * of the node. If the ID does exist, this method returns a None.
    *
    * @param node a JAAM Node
    * @return An Option of a JValue (if the Node is unique) or a None.
    */
  def processNode(node : Node) : Option[JValue] = {
    if (uniqueNodes.contains(node.id)) {
      return None
    }
    uniqueNodes.add(node.id)
    node match {
      case n : State => Some(jsonForState(n))
      case n : ErrorState => Some(jsonForErrorState(n))
    }
  }

  /**
    * Takes an edge and checks whether that edge's ID exists already within this
    * serialization. If it does not, this method returns the JSON serialization
    * of the edge. If the ID does exist, this method returns a None.
    *
    * @param edge a JAAM Edge
    * @return An Option of a JValue (if the Edge is unique) or a None.
    */
  def processEdge(edge : Edge) : Option[JValue] = {
    if (uniqueEdges.contains(edge.id)) {
      return None
    }
    uniqueEdges.add(edge.id)
    Some(jsonForEdge(edge))
  }

  /**
    * Converts a JAAM Serializer State into JSON.
    *
    * @param state a JAAM State
    * @return A JSON serialization.
    */
  def jsonForState(state : State) : JValue = {
    "state" ->
      ("id"             -> state.id.id) ~
      ("index"          -> state.stmt.index) ~
      ("class"          -> state.stmt.method.getDeclaringClass.toString) ~
      ("method"         -> state.stmt.method.getName) ~
      ("returns"        -> state.stmt.method.getReturnType.toString) ~
      ("parameterCount" -> state.stmt.method.getParameterCount) ~
      ("parameters"     -> jsonForStmtParameters(state.stmt)) ~
      ("sootMethod"     -> state.stmt.method.toString) ~
      ("sootStmt"       -> state.stmt.stmt.toString())
  }

  /**
    * Takes a JAAM Serializer Stmt and obtains a list of its parameter types.
    * This is done separately due to poor compatibility between json4s and
    * java.util.list (the return type of `stmt.method.getParameterTypes`).
    *
    * @param stmt a JAAM Serializer Stmt to get parameter types from
    * @return The list of parameter types as JValues.
    */
  def jsonForStmtParameters(stmt : Stmt) : List[JValue] = {
    val buffer = ListBuffer[JValue]()
    val parameters = stmt.method.getParameterTypes
    for (parameter <- parameters) {
      buffer += parameter.toString
    }
    buffer.toList
  }

  /**
    * Converts a JAAM Serializer ErrorState into JSON.
    *
    * @param errorState a JAAM ErrorState
    * @return A JSON serialization.
    */
  def jsonForErrorState(errorState : ErrorState) : JValue = {
    "errorState" ->
      ("id" -> errorState.id.id)
  }

  /**
    * Converts a JAAM Serializer Edge into JSON.
    *
    * @param edge a JAAM Edge
    * @return A JSON serialization.
    */
  def jsonForEdge(edge : Edge) : JValue = {
    "edge" ->
      ("id" -> edge.id.id) ~
      ("source" -> edge.src.id) ~
      ("destination" -> edge.dst.id)
  }
}
