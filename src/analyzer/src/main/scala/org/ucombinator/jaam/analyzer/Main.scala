package org.ucombinator.jaam.analyzer

import java.io.{FileInputStream, FileOutputStream}

import org.ucombinator.jaam.serializer._
import soot.jimple._
import soot.Type

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class AnalysisNode(var node : Node = null, var manualIndex : Int = -1) {
  val inNodes = mutable.MutableList[Id[AnalysisNode]]()
  val outNodes = mutable.MutableList[Id[AnalysisNode]]()

  def inDegree() : Int = {
    inNodes.size
  }
  def outDegree() : Int = {
    outNodes.size
  }

  var abstractionLvl = 0
  val abstractNodes = mutable.MutableList[Id[AnalysisNode]]()

  def addInNodeID(id : Id[AnalysisNode]): Unit = {
    inNodes += id
  }

  def addOutNodeID(id : Id[AnalysisNode]): Unit = {
    outNodes += id
  }
  def addAbstractNodeID(id : Id[AnalysisNode]): Unit = {
    abstractNodes += id
  }
  def getIndex(): Int = {
    if(manualIndex != -1)
      manualIndex
    else if(node != null)
      node.id.id
    else
      -1
  }
}

class AnalysisGraph() {
  //todo
  var rootId : Id[AnalysisNode] = Id[AnalysisNode](1)

  private var index = 0

  val graph = mutable.Map[Id[AnalysisNode], AnalysisNode]()

  var abstractMap = mutable.Map[Id[AnalysisNode], Id[AnalysisNode]]()

  def addBlankNode(): Id[AnalysisNode] = {
    index += 1
    val node = new AnalysisNode(manualIndex = index)
    val id = Id[AnalysisNode](index)
    graph(id) = node

    id
  }

  def readPacket(packet : Packet): Unit = {
    packet match {

      case n: Node =>
        val id = Id[AnalysisNode](n.id.id)
        if(id.id > index) {
          index = id.id
        }
        graph get id match {
          case Some(node) =>
            node.node = n
          case None =>
            graph(id) = new AnalysisNode(n)
        }
      case e: Edge =>
        val in = Id[AnalysisNode](e.src.id)
        val out = Id[AnalysisNode](e.dst.id)
        graph get in match {
          case Some(node) =>
            node.addOutNodeID(out)
          case None =>
            val node = new AnalysisNode(null)
            node.addOutNodeID(out)
            graph(in) = node
        }
        graph get out match {
          case Some(node) =>
            node.addInNodeID(in)
          case None =>
            val node = new AnalysisNode(null)
            node.addInNodeID(in)
            graph(out) = node
        }
      case _ => ()
    }
  }
}

case class Config(
                   mode : String        = null,
                   sourceFile : String  = null,
                   targetFile : String  = null)

object Main {
  //type AnalysisGraph = mutable.Map[Id[AnalysisNode], AnalysisNode]
  //def AnalysisGraph()

  def main(args : Array[String]) {
    val parser = new scopt.OptionParser[Config]("jaam-analyzer") {
      override def showUsageOnError = true

      help("help") text("prints this usage text")

      note("")
      cmd("chain") action { (_, c) =>
        c.copy(mode = "chain")
      } text("Collapse long chains into single nodes.") children (
        arg[String]("<input file>") action {
          (x, c) => c.copy(sourceFile = x)
        } text ("the input .jaam file to analyze"),
        arg[String]("<output file>") action {
          (x, c) => c.copy(targetFile = x)
        } text ("the output .jaam file to store the result")
      )

      // TODO
      // Add new command here with cmd("<command name>"), imitating the code
      // from above.
    }

    parser.parse(args, Config()) match {
      case None =>
        println("Bad arguments given.")

      case Some(config) =>
        // Create the AnalysisGraph from the source file.
        val graph = analyzeFile(config.sourceFile)

        config.mode match {
          case "chain" =>
            Chain.MakeChain(graph)

            // TODO
            // Add extra cases here as needed to add support for new sub-
            // commands.
        }
    }
  }

  //def analyzeFile(file : String) : scala.collection.mutable.MutableList[Id[Node]] = {
  def analyzeFile(file : String) : AnalysisGraph = {
    val stream = new FileInputStream(file)
    val pi = new PacketInput(stream)
    var packet : Packet = null
    //val idList = scala.collection.mutable.MutableList.empty[Id[Node]]
//    val nodeList = scala.collection.mutable.MutableList.empty[AnalysisNode]

    //val graph = mutable.Map[Id[AnalysisNode], AnalysisNode]()
    val graph = new AnalysisGraph()

    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {

      graph.readPacket(packet)

        /*case p : Node => {
          p match {
            case node: State => {
              node.stmt.stmt match {
                case sootStmt: DefinitionStmt =>
                  sootStmt.getRightOp match {
                    case rightOp: AnyNewExpr =>
                      // add rightOp.getType() call to get the type
                      println("Case reached")
                      var n = new AnalysisNode(node, rightOp.getType())
                      nodeList += n
                      //idList += node.id
                    case _ => println("Node not AnyNewExpr")
                  }
                case _ => println("Node not DefinitionStmt")
              }
            }
            case _ => println("Node NOT STATE")
          }
        }
        case p : Edge => println("EDGE")
        case _ => println("nothing")*/
    }

    pi.close()
    //idList

    graph
  }

//  def writeOut(file : String, idList : scala.collection.mutable.MutableList[Id[Node]]) = {
//  def writeOut(file : String, nodeList : scala.collection.mutable.MutableList[AnalysisNode]) = {
//  val stream = new FileOutputStream(file)
//    val po = new PacketOutput(stream)
//    var counter = 0
//
//    //for(id <- idList){
//    //  val packet = NodeTag(Id[Tag](counter), id, AllocationTag())
//    //  po.write(packet)
//    //  counter += 1
//    //}
//
//    for(aNode <- nodeList) {
//      val packet = NodeTag(Id[Tag](counter), aNode.getNode().id, AllocationTag(aNode.getType()))
//      po.write(packet)
//      counter += 1
//    }
//
//    po.write(EOF())
//    po.close()
//  }
}
