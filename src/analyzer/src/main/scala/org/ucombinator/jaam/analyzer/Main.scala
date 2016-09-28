package org.ucombinator.jaam.analyzer

import java.io.{FileInputStream, FileOutputStream}

import org.ucombinator.jaam.serializer
import org.ucombinator.jaam.serializer._

import scala.collection.mutable

class AnalysisNode(var node : Node = null, override val id : Id[Node]) extends Node(id){
  var abstractionLvl = 0
  var abstractNodes = mutable.MutableList[Id[AnalysisNode]]()
  var duplicateNodes = mutable.MutableList[Id[AnalysisNode]]()
  var inNodes = mutable.MutableList[Id[AnalysisNode]]()
  var outNodes = mutable.MutableList[Id[AnalysisNode]]()

  def inDegree() : Int = {
    inNodes.size
  }

  def outDegree() : Int = {
    outNodes.size
  }

  def addInNodeID(id : Id[AnalysisNode]): Unit = {
    inNodes += id
  }

  def addOutNodeID(id : Id[AnalysisNode]): Unit = {
    outNodes += id
  }

  def addAbstractNodeID(id : Id[AnalysisNode]): Unit = {
    abstractNodes += id
  }

  def addDuplicateID(id : Id[AnalysisNode]) = {
    duplicateNodes += id
  }

  def getIndex(): Int = {
    if(id != -1)
      id.asInstanceOf[Int]
    else if(node != null)
      node.id.id
    else
      -1
  }

  // Wrap the analyzer AnalysisNode in a serializer AnalysisNode
  def toPacket(tag : Tag): Packet = {
    val abstractnodes = abstractNodes.map( x => x.id )
    val inedges = inNodes.map( x => x.id )
    val outedges = outNodes.map( x => x.id )
    serializer.AnalysisNode(node, id, abstractnodes, inedges, outedges, tag)
  }
}

class AnalysisGraph() {
  //todo
  private var index = 0
  val graph = mutable.Map[Id[AnalysisNode], AnalysisNode]()
  var abstractMap = mutable.Map[Id[AnalysisNode], Id[AnalysisNode]]()
  var rootId : Id[AnalysisNode] = Id[AnalysisNode](1)


  def addBlankNode(): Id[AnalysisNode] = {
    index += 1
    val node = new AnalysisNode(id = Id[Node](index))
    val id = Id[AnalysisNode](index)
    graph(id) = node

    id
  }

  def addNode(node : AnalysisNode) : AnalysisNode = {
    graph(Id[AnalysisNode](node.getIndex())) = node
    node
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
            graph(id) = new AnalysisNode(n, n.id)
        }
      case e: Edge =>
        val in = Id[AnalysisNode](e.src.id)
        val out = Id[AnalysisNode](e.dst.id)
        graph get in match {
          case Some(node) =>
            node.addOutNodeID(out)
          case None =>
            val node = new AnalysisNode(null, Id[Node](-1)) // CH probably wrong
            node.addOutNodeID(out)
            graph(in) = node
        }
        graph get out match {
          case Some(node) =>
            node.addInNodeID(in)
          case None =>
            val node = new AnalysisNode(null, Id[Node](-1)) // CH probably wrong
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
            val result = Chain.MakeChain(graph)
            writeOut(result, config.targetFile)

            // TODO
            // Add extra cases here as needed to add support for new sub-
            // commands.
        }
    }
  }

  def analyzeFile(file : String) : AnalysisGraph = {
    val stream = new FileInputStream(file)
    val pi = new PacketInput(stream)
    var packet : Packet = null

    val graph = new AnalysisGraph()

    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      graph.readPacket(packet)
    }

    pi.close()

    graph
  }

  /*
  Write the output of analyzed file to a .jaam file
 */
  def writeOut(graph : AnalysisGraph, file : String) {

    val outSerializer = new serializer.PacketOutput(new FileOutputStream(file))
    val chaintag = new serializer.ChainTag

    for((k,v) <- graph.graph) {
      outSerializer.write(v.toPacket(chaintag))
    }
    outSerializer.write(EOF())
    outSerializer.close()
  }

//  def writeOut(file : String, idList : scala.collection.mutable.MutableList[Id[Node]]) = {
//  def writeOut(file : String, nodeList : scala.collection.mutable.MutableList[AnalysisNode]) = {
//  val stream = new FileOutputStream(file)
//    val po = new PacketOutput(stream)
//    var counter = 0
//
//    //for(id <- idList){
//      val packet = NodeTag(Id[Tag](counter), id, AllocationTag())
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

