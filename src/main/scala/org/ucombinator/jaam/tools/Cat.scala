package org.ucombinator.jaam.tools

import java.io.{FileInputStream, FileOutputStream}

import org.ucombinator.jaam.serializer._

import scala.collection.mutable


class Cat extends Main("cat") {
  banner("Combine multile JAAM files into a single, cohesive file.")
  footer("")

  val outFile = trailArg[java.io.File](descr = "The desired output filename")
  val inFiles = trailArg[List[String]](descr = "The list of files to be concatenated.")

  def run(conf: Conf) {
    Cat.concatenateFiles(inFiles(), outFile().toString)
  }
}

object Cat {
  private val packetBuffer = mutable.MutableList.empty[Packet]

  def concatenateFiles(files : Seq[String], outputFile : String) = {
    for (file <- files) {
      readPacketsToBufferFromFile(file)
    }
    writeBufferToFile(outputFile)
  }

  private def readPacketsToBufferFromFile(file : String) = {
    println(f"Reading $file")
    val stream = new FileInputStream(file)
    val pi = new PacketInput(stream)
    var packet: Packet = null

    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      packetBuffer += packet
    }

    pi.close()
  }

  private def writeBufferToFile(file : String) = {
    println(f"Writing $file")
    val outStream = new FileOutputStream(file)
    val po = new PacketOutput(outStream)

    for (packet <- packetBuffer) {
      po.write(packet)
    }
    po.write(EOF())

    po.close()
  }
}
