package org.ucombinator.jaam.tools

import java.io.FileInputStream

import org.ucombinator.jaam.serializer._

object Truncate {
  def truncateFile(jaamFile : String) = {
    val stream = new FileInputStream(jaamFile)
    val pi = new PacketInput(stream)
    var packet: Packet = null
    while (true) {
      packet = pi.read() // does this throw a distinct error if it tries to read and there's nothing left?
    }
  }
}
