package org.ucombinator.jaam.analyzer

import java.io.FileInputStream
import org.ucombinator.jaam.serializer._

case class Config(sourceFile : String = null)

object Main {
  def main(args : Array[String]) = {
    val parser = new scopt.OptionParser[Config]("jaam-analyzer") {
      override def showUsageOnError = true

      help("help") text("prints this usage text")
      arg[String]("<file>") action {
        (x, c) => c.copy(sourceFile = x)
      } text("the input .jaam file to analyze")
    }

    parser.parse(args, Config()) match {
      case None =>
        println("Bad arguments given.")

      case Some(config) =>

    }
  }

  def analyzeFile(file : String) = {
    val stream = new FileInputStream(file)]
    val pi = new PacketInput(stream)
    var packet : Packet = null

    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      //TODO: analysis
    }

    pi.close()
  }
}
