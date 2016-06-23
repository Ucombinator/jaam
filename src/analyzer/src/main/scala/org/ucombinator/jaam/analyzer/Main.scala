package org.ucombinator.jaam.analyzer

import java.io.{FileInputStream, FileOutputStream}

import org.ucombinator.jaam.serializer._

case class Config(
                   sourceFile : String = null,
                   targetFile : String = null)

object Main {
  def main(args : Array[String]) = {
    val parser = new scopt.OptionParser[Config]("jaam-analyzer") {
      override def showUsageOnError = true

      help("help") text("prints this usage text")
      arg[String]("<input file>") action {
        (x, c) => c.copy(sourceFile = x)
      } text("the input .jaam file to analyze")
      arg[String]("<output file>") action {
        (x, c) => c.copy(targetFile = x)
      } text("the output .jaam file with tags")
    }

    parser.parse(args, Config()) match {
      case None =>
        println("Bad arguments given.")

      case Some(config) =>
        writeOut(config.targetFile, analyzeFile(config.sourceFile))
    }
  }

  def analyzeFile(file : String) : scala.collection.mutable.MutableList[Id[Node]] = {
    val stream = new FileInputStream(file)
    val pi = new PacketInput(stream)
    var packet : Packet = null
    val idList = scala.collection.mutable.MutableList.empty[Id[Node]]

    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      packet match {
        case p : Node =>
          p match {
            case node : State =>
              if(node.stmt.stmt.toString().contains(" = new")){
                idList += node.id
              }
            case _ => ()
          }
        case _ => ()
      }
    }

    pi.close()
    idList
  }

  def writeOut(file : String, idList : scala.collection.mutable.MutableList[Id[Node]]) = {
    val stream = new FileOutputStream(file)
    val po = new PacketOutput(stream)
    var counter = 0

    for(id <- idList){
      val packet = NodeTag(Id[Tag](counter), id, AllocationTag())
      po.write(packet)
      counter += 1
    }
    po.write(EOF())
    po.close()
  }
}
