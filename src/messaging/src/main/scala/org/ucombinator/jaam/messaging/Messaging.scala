/* This library handles message serialization for communicating between
 * tools */
package org.ucombinator.jaam.messaging

import java.io.InputStream
import java.io.OutputStream
import scala.collection.mutable.ArrayBuffer
import de.javakaffee.kryoserializers._
import com.esotericsoftware.minlog.Log
import com.esotericsoftware.kryo._
import com.esotericsoftware.kryo.io._
import com.twitter.chill.ScalaKryoInstantiator
import soot.{Unit => _, _}
import soot.jimple.{Stmt => SootStmt}

import com.esotericsoftware.kryo.serializers.FieldSerializer

////////////////////////////////////////
// Methods and types for reading and writing message
////////////////////////////////////////

class MyKryo extends com.twitter.chill.KryoBase {
  var seenClasses = Set[Class[_]]()

  {
    this.setRegistrationRequired(false)
    this.setInstantiatorStrategy(new org.objenesis.strategy.StdInstantiatorStrategy)
    // Handle cases where we may have an odd classloader setup like with libjars
    // for hadoop
    val classLoader = Thread.currentThread.getContextClassLoader
    this.setClassLoader(classLoader)
    val reg = new com.twitter.chill.AllScalaRegistrar
    reg(this)
    this.setAutoReset(false)
    this.addDefaultSerializer(classOf[soot.util.Chain[java.lang.Object]], classOf[com.esotericsoftware.kryo.serializers.FieldSerializer[java.lang.Object]])
    UnmodifiableCollectionsSerializer.registerSerializers(this)
  }

  def classSignature(typ : java.lang.reflect.Type) : String = {
    typ match {
      case null => ""
      case typ : Class[_] =>
        "  "+typ.getCanonicalName() + "\n" +
         (for (i <- typ.getDeclaredFields().toList.sortBy(_.toString)) yield {
          "   "+i+"\n"
         }).mkString("") +
         classSignature(typ.getGenericSuperclass())
      case _ => ""
    }
  }

  override def writeClass(output : Output, t : Class[_]) : Registration = {
    val r = super.writeClass(output, t)

    if (r == null || seenClasses.contains(r.getType)) {
      output.writeString(null)
    } else {
      seenClasses += r.getType
      output.writeString(classSignature(r.getType))
    }

    r
  }

  override def readClass(input : Input) : Registration = {
    val r = super.readClass(input)

    val found = input.readString()

    if (r == null) { return null }

    if (found != null) {
      val expected = classSignature(r.getType)

      if (expected != found) {
        //throw new java.io.IOException("Differing Jaam class signatures\n Expected:\n%s Found:\n%s".format(expected, found))
        println("Differing Jaam class signatures\n Expected:\n%s Found:\n%s".format(expected, found))
      }
    }

    r
  }
}

object Message {
  // File signature using the same style as PNG
  // \212 = 0x8a = 'J' + 0x40: High bit set so 'file' knows we are binary
  // "JAAM": Help humans figure out what format the file is
  // "\r\n": Detect bad line conversion
  // \032 = 0x1a = "^Z": Stops output on DOS
  // "\n": Detect bad line conversion
  val formatSignature = "\212JAAM\r\n\032\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)
  val formatVersion = java.nio.ByteBuffer.allocate(4).putInt(1 /* this is the version number */).array()

  // Types for reading and writing messages
  // These extend from Kryo instead of being type alias to keep Java happy
  class Input(in : InputStream) extends com.esotericsoftware.kryo.io.Input(in) {}
  class Output(out : OutputStream) extends com.esotericsoftware.kryo.io.Output(out) {
    override def close() : Unit = {
      Message.write(this, Done())
      super.close()
    }
  }

  // Lift an InputStream to Message.Input
  def openInput(in : InputStream) : Input = {
    def checkHeader(name : String, expected : Array[Byte]) {
      val found = new Array[Byte](expected.length)
      val len = in.read(found, 0, found.length)

      if (len != expected.length) {
        throw new java.io.IOException(
          "Reading %s yielded only %d bytes. Expected %d bytes."
            .format(name, len, formatSignature.length))
      }

      if (found.toList != expected.toList) {
        val e = (for (i <- expected) yield { "%x".format(i) }).mkString("")
        val f = (for (i <- found)    yield { "%x".format(i) }).mkString("")
        throw new java.io.IOException("Invalid %s\n Expected: 0x%s\n Found:    0x%s".format(name, e, f))
      }
    }

    checkHeader("Jaam file-format signature", formatSignature)
    checkHeader("Jaam file-format version", formatVersion)

    new Input(new java.util.zip.InflaterInputStream(in))
  }

  // Lift an OutputStream to Message.Output
  def openOutput(out : OutputStream) : Output = {
    out.write(formatSignature)
    out.write(formatVersion)

    new Output(new java.util.zip.DeflaterOutputStream(out))
  }

  // Reads a 'Message' from 'in'
  // TODO: check for exceptions
  def read(in : Input) : Message = {
    kryo.readClassAndObject(in) match {
      case o : Message => o
      case _ => throw new Exception("TODO: Message.read failed")
    }
  }

  // Writes the 'Message' 'm' to 'out'
  def write(out : Output, m : Message) : Unit = {
    kryo.writeClassAndObject(out, m)
  }

  val kryo = new MyKryo()
}


////////////////////////////////////////
// Message types
////////////////////////////////////////

// The super type of all messages
abstract class Message {}

// Signals that all messages are done
// TODO: check if "object" is okay here
case class Done() extends Message {}

// Declare a transition edge between two 'State' nodes
case class Edge(id : Id[Edge], src : Id[AbstractState], dst : Id[AbstractState]) extends Message {}

// Declare 'AbstractState' nodes
abstract class AbstractState(id : Id[AbstractState]) extends Message {}
case class ErrorState(id : Id[AbstractState]) extends AbstractState(id) {}
case class State(id : Id[AbstractState], stmt : Stmt, framePointer : String, kontStack : String) extends AbstractState(id) {}


////////////////////////////////////////
// Types inside messages
////////////////////////////////////////

// Identifiers qualified by a namespace
case class Id[Namespace](id : Int) {
  // val namespace = classOf[Namespace]
}

// Type for statements (needed because 'SootStmt' doesn't specify the
// 'SootMethod' that it is in)
case class Stmt(method : SootMethod, index : Int, stmt : SootStmt) {}


/*
AbstractState
  ErrorState
  State(Stmt, FramePointer, KontStack)

Stmt(SootStmt, SootMethod)

FramePointer
  InvariantFramePointer
  ZeroCFAFramePointer(SootMethod)
  OneCFAFramePointer(SootMethod, Stmt)
  InitialFramePointer

From
  FromNative
  FromJava

BasePointer
  OneCFABasePointer(Stmt, FramePointer, From)
  InitialBasePointer
  StringBasePointer(String)
  ClassBasePointer(String)

Value
  AtomicValue
    AnyAtomicValue
  ObjectValue(SootClass, BasePointer)
  ArrayValue(SootType, BasePointer)

KontAddr
  OneCFAKontAddr

Addr
  FrameAddr
    LocalFrameAddr(FramePointer, Local)
    ParameterFrameAddr(FramePointer, Int)
    ThisFrameAddr
    CaughtExceptionFrameAddr
  InstanceFieldAddr
  ArrayRefAddr
  ArrayLengthAddr
  StaticFieldAddr

Store
KontStack(Kont)

Kont
  RetKont(Frame, KontAddr)
  HaltKont

Frame(Stmt, FramePointer, Option[Set[Addr]]))
 */
