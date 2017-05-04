package org.ucombinator.jaam.analyzer

import java.io.{FileInputStream, FileOutputStream}

import org.ucombinator.jaam.serializer
import org.ucombinator.jaam.serializer._

import scala.collection.mutable

import org.rogach.scallop._

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
    if(id.asInstanceOf[Int] != -1)
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

object Conf {
  class Chain extends Main("chain") {
    banner("Collapse long chains into single nodes")
    footer("")

    val sourceFile = trailArg[java.io.File](descr = "the input .jaam file to analyze")
    val targetFile = trailArg[String](descr = "the output .jaam file to store the result")

    def run(conf: Conf): Unit = {
      val graph = Common.analyzeFile(sourceFile().toString)
      val result = Chain.MakeChain(graph)
      Common.writeOut(result, targetFile())
    }
  }
}

class Conf(args : Seq[String]) extends ScallopConf(args = args) {

}

abstract class Main(name: String) extends Subcommand(name) {
  def run(conf : Conf)
}

object Common {
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
}

object Main {
  def main(args : Array[String]) {
    val conf = new Conf(args)
    conf.subcommand match {
      case Some(m: Main) => m.run(conf)
      case None => println("ERROR: No subcommand specified")
    }
  }
}

