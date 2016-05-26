package org.ucombinator.jaam.messaging

/*****************************************
 * This library handles reading and writting ".jaam" files.  For usage see
 * MessageInput and MessageOutput in this package.
 * ****************************************/

import java.lang.Object
import java.io.{InputStream, OutputStream, IOException}
import java.lang.reflect.Type
import java.util.zip.{DeflaterOutputStream, InflaterInputStream}

import soot.SootMethod
import soot.jimple.{Stmt => SootStmt}
import soot.util.Chain

import com.esotericsoftware.minlog.Log

import com.esotericsoftware.kryo.{Kryo, Registration}
import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.serializers.FieldSerializer

import org.objenesis.strategy.StdInstantiatorStrategy
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer
import com.twitter.chill.{AllScalaRegistrar, KryoBase, ScalaKryoInstantiator}


////////////////////////////////////////
// 'MessageInput' is used to read a ".jaam" file
//
// Usage of this class:
//   in = new MessageInput(new FileInputStream("<filename>.jaam"))
//   in.read()
class MessageInput(private val input : InputStream) {
  // Reads a 'Message'
  // TODO: check for exceptions
  def read() : Message = {
    this.kryo.readClassAndObject(in) match {
      case o : Message => o
      case o => throw new IOException("Read object is not a Message: " + o)
    }
  }

  // Closes this 'MessageInput'
  def close() = in.close()

  ////////////////////////////////////////
  // Implementation internals
  ////////////////////////////////////////

  checkHeader("Jaam file-format signature", Signatures.formatSignature)
  checkHeader("Jaam file-format version", Signatures.formatVersion)

  private val in = new Input(new InflaterInputStream(input))
  private val kryo : Kryo = new JaamKryo()

  private def checkHeader(name : String, expected : Array[Byte]) {
    val found = new Array[Byte](expected.length)
    val len = input.read(found, 0, found.length)

    if (len != expected.length) {
      throw new IOException(
        "Reading %s yielded only %d bytes. Expected %d bytes."
          .format(name, len, expected.length))
    }

    if (found.toList != expected.toList) {
      val e = (for (i <- expected) yield { "%x".format(i) }).mkString("")
      val f = (for (i <- found)    yield { "%x".format(i) }).mkString("")
      throw new IOException("Invalid %s\n Expected: 0x%s\n Found:    0x%s"
        .format(name, e, f))
    }
  }
}


////////////////////////////////////////
// 'MessageOutput' is used to write ".jaam" files
//
// Usage of this class:
//   out = new MessageOutput(new FileOutputStream("<filename>.jaam"))
//   out.write(message)
class MessageOutput(private val output : OutputStream) {
  // Writes a 'Message'
  def write(m : Message) : Unit = {
    this.kryo.writeClassAndObject(this.out, m)
  }

  // Flushes output
  def flush() = out.flush()

  // Closes this 'MessageInput'
  def close() : Unit = {
    this.write(Done())
    out.close()
  }

  ////////////////////////////////////////
  // Implementation internals
  ////////////////////////////////////////

  output.write(Signatures.formatSignature)
  output.write(Signatures.formatVersion)

  private val out = new Output(new DeflaterOutputStream(output))
  private val kryo : Kryo = new JaamKryo()
}


private[this] object Signatures {
  // File signature using the same style as PNG
  // \212 = 0x8a = 'J' + 0x40: High bit set so 'file' knows we are binary
  // "JAAM": Help humans figure out what format the file is
  // "\r\n": Detect bad line conversion
  // \032 = 0x1a = "^Z": Stops output on DOS
  // "\n": Detect bad line conversion
  val formatSignature = "\212JAAM\r\n\032\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)
  val formatVersion = java.nio.ByteBuffer.allocate(4).putInt(1 /* this is the version number */).array()
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
Classes that we may eventually need to support in 'Message':

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

////////////////////////////////////////
// Internal classes
////////////////////////////////////////

// Internal Kryo object that adds extra checking of the types and field
// structures of read and written objects to be sure they match what was
// expected.
class JaamKryo extends KryoBase {
  var seenClasses = Set[Class[_]]()

  // This is copies from Chill
  this.setRegistrationRequired(false)
  this.setInstantiatorStrategy(new StdInstantiatorStrategy)
  // Handle cases where we may have an odd classloader setup like with libjars
  // for hadoop
  val classLoader = Thread.currentThread.getContextClassLoader
  this.setClassLoader(classLoader)
  val reg = new AllScalaRegistrar
  reg(this)
  this.setAutoReset(false)
  this.addDefaultSerializer(classOf[Chain[Object]], classOf[FieldSerializer[java.lang.Object]])
  UnmodifiableCollectionsSerializer.registerSerializers(this)

  // Produces a string that documents the field structure of 'typ'
  def classSignature(typ : Type) : String = {
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
        throw new IOException("Differing Jaam class signatures\n Expected:\n%s Found:\n%s".format(expected, found))
      }
    }

    r
  }
}
