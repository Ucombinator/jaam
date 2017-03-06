package org.ucombinator.jaam.tools

import java.io.{FileInputStream, FileOutputStream, IOException}

import scala.collection.mutable
import org.ucombinator.jaam.serializer._
import com.esotericsoftware.kryo.KryoException
import com.esotericsoftware.minlog.Log

object Validate {
  private val uniqueNodes = mutable.Set[Id[Node]]()
  private var shouldAddMissingStates = false
  private var shouldRemoveMissingStates = false

  def validateFile(jaamFile : String,
                   shouldAppendMissingEOF : Boolean = false,
                   addMissingStates : Boolean = false,
                   removeMissingStates : Boolean = false) = {
    shouldAddMissingStates = addMissingStates
    shouldRemoveMissingStates = removeMissingStates
    val packets = mutable.MutableList[Packet]()
    val willWrite = shouldAppendMissingEOF || shouldAddMissingStates || shouldRemoveMissingStates
    val stream = new FileInputStream(jaamFile)
    val pi = new PacketInput(stream)

    var endedAcceptably = false // Because you can't just `break` a loop...
    var endedPrematurely = false
    var packet: Packet = null
    try {
      while (! endedAcceptably) {
        packet = pi.read() // Throws IOException if an invalid read occurs
        if (willWrite) { packets += packet }
        packet match {
          case p : EOF => endedAcceptably = true
          case p : Node => uniqueNodes += p.id
          case _ => ()
        }
      }
    } catch {
      case e : IOException => // Unable to pi.read() successfully
        endedPrematurely = true
      case ke: KryoException => // Unable to pi.read() successfully
        endedPrematurely = true
      case e : Throwable =>
        e.printStackTrace()
        sys.error("Something unexpected went wrong.")
    }

    pi.close()

    if (willWrite) {
      //TODO: Maybe write to a temp file and move it after completion?
      val outStream = new FileOutputStream(jaamFile)
      val po = new PacketOutput(outStream)

      for (packet <- packets) {
        packet match {
          case p : Node => handleNode(p, po)
          case p : Edge => handleEdge(p, po)
        }
      }

      if (endedPrematurely) {
        if (shouldAppendMissingEOF) {
          // Need to add the EOF
          po.write(EOF())
        } else {
          Log.error("The file ended prematurely. Run with --fixEOF to recover the usable portions of the JAAM file.")
        }
      }

      po.close()
    }

    if (! endedAcceptably) {
      sys.exit(1)
    }
  }

  private def handleNode(node : Node, po : PacketOutput) = {
    // Only write node if either
    //  a) it isn't a MissingState, or
    //  b) it is a MissingState, but we aren't removing MissingStates
    if (! (shouldRemoveMissingStates && node.isInstanceOf[MissingReferencedState])) {
      po.write(node)
    }
  }

  private def handleEdge(edge : Edge, po : PacketOutput) = {
    po.write(edge)

    // Only handle missing edges if we should handle missing edges
    if (shouldAddMissingStates) {
      // Check if either of the edge's nodes are missing
      if (! uniqueNodes.contains(edge.src)) {
        val missingState = MissingReferencedState(edge.src)
        handleNode(missingState, po)
      }
      if (! uniqueNodes.contains(edge.dst)) {
        val missingState = MissingReferencedState(edge.dst)
        handleNode(missingState, po)
      }
    }
  }
}
