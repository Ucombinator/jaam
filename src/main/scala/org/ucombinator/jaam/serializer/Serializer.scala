package org.ucombinator.jaam.serializer

/*****************************************
 * This library handles reading and writting ".jaam" files.  For usage see
 * PacketInput and PacketOutput in this package.
 * ****************************************/

import java.lang.Object
import java.io.{IOException, InputStream, OutputStream, FileInputStream}
import java.lang.reflect.Type
import java.util.zip.{DeflaterOutputStream, InflaterInputStream}

import scala.collection.JavaConversions._
import soot.{SootMethod, Local}
import soot.jimple.{Stmt => SootStmt, Ref, Constant, InvokeExpr}
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.util.Chain
import org.objectweb.asm.tree.{AbstractInsnNode, InsnList}
import com.esotericsoftware.minlog.Log
import com.esotericsoftware.kryo
import com.esotericsoftware.kryo.{Kryo, Registration}
import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.serializers.FieldSerializer
import org.objenesis.strategy.StdInstantiatorStrategy
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer
import com.twitter.chill.{AllScalaRegistrar, KryoBase, ScalaKryoInstantiator}
import com.strobel.decompiler.languages.java.ast.AstNode
import com.strobel.decompiler.patterns.Role

import scala.collection.mutable
import scala.collection.immutable

object Serializer {
  def readAll(file: String): List[Packet] = {
    val stream = new FileInputStream(file)
    val pi = new PacketInput(stream)

    var packet: Packet = null
    var packets = List[Packet]()
    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      // TODO: for (packet <- pi) {
      packets +:= packet
    }

    return packets.reverse
  }
}

////////////////////////////////////////
// 'PacketInput' is used to read a ".jaam" file
//
// Usage of this class:
//   in = new PacketInput(new FileInputStream("<filename>.jaam"))
//   in.read()
class PacketInput(private val input : InputStream) {
  // Reads a 'Packet'
  // TODO: check for exceptions
  def read() : Packet = {
    this.kryo.readClassAndObject(in) match {
      case o : Packet =>
        o
      case o => throw new IOException("Read object is not a Packet: " + o)
    }
  }

  // Closes this 'PacketInput'
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
// 'PacketOutput' is used to write ".jaam" files
//
// Usage of this class:
//   out = new PacketOutput(new FileOutputStream("<filename>.jaam"))
//   out.write(packet)
class PacketOutput(private val output : OutputStream) {
  // Writes a 'Packet'
  def write(m : Packet) : Unit = {
    this.kryo.writeClassAndObject(this.out, m)
  }

  // Flushes output
  def flush() = out.flush()

  // Closes this 'PacketOutput'
  def close() : Unit = {
    this.write(EOF())
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
  // \x008a = 'J' + 0x40: High bit set so 'file' knows we are binary
  // JAAM: Help humans figure out what format the file is
  // \r\n: Detect bad line conversion
  // \x001a = '^Z': Stops output on DOS
  // \n: Detect bad line conversion
  val formatSignature = "\u008aJAAM\r\n\u001a\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)
  val formatVersion = { val version = 1; java.nio.ByteBuffer.allocate(4).putInt(version).array() }
}


////////////////////////////////////////
// Packet types
////////////////////////////////////////

// The super type of all packets
abstract class Packet {}

// Signals that all packets are done
// TODO: check if "object" is okay here
case class EOF () extends Packet {}

// Declare a transition edge between two 'State' nodes
case class Edge(id : Id[Edge], src : Id[Node], dst : Id[Node]) extends Packet {}

// Declare the Node - the counterpart to the edge
abstract class Node(val id : Id[Node]) extends Packet {}

// Declare 'AbstractState' nodes
class AbstractState(override val id : Id[Node]) extends Node(id) {}

// AnalysisNodes from the analyzer
case class AnalysisNode(var node : Node = null,
                        override val id : Id[Node],
                        val abstNodes : mutable.MutableList[Int],
                        val inEdges : mutable.MutableList[Int],
                        val outEdges : mutable.MutableList[Int],
                        val tag : Tag) extends Node(id) {}

case class ErrorState(override val id : Id[Node]) extends AbstractState(id) {}
case class State(override val id : Id[Node], stmt : Stmt, framePointer : String, kontStack : String) extends AbstractState(id) {}

// Declare 'MissingState' nodes, used by jaam.tools.Validate
case class MissingReferencedState(override val id : Id[Node]) extends Node(id) {}

//case class Group(id : Id[Node], states : java.util.List[Node], labels : String)

//tags for the analyzer
case class NodeTag(id : Id[Tag], node : Id[Node], tag : Tag) extends Packet {}
abstract class Tag {}
case class AllocationTag(val sootType : soot.Type) extends Tag {}
case class ChainTag() extends Tag {}

abstract class LoopNode extends Packet {}
case class LoopLoopNode(id: Id[LoopNode], method: SootMethod, depends: Set[TaintAddress], statementIndex: Int) extends LoopNode {}
case class LoopMethodNode(id: Id[LoopNode], method: SootMethod) extends LoopNode {}
case class LoopEdge(src: Id[LoopNode], dst: Id[LoopNode], isRecursion: Boolean) extends Packet {}

////////////////////////////////////////
// Types inside packets
////////////////////////////////////////

// Identifiers qualified by a namespace
case class Id[Namespace](id : Int) {
  // val namespace = classOf[Namespace]
}

// Type for statements (needed because 'SootStmt' doesn't specify the
// 'SootMethod' that it is in)
case class Stmt(method : SootMethod, index : Int, stmt : SootStmt) {}

// Type for taint addresses
abstract sealed class TaintValue

abstract sealed class TaintAddress extends TaintValue {
  val m: SootMethod
}
case class LocalTaintAddress(override val m: SootMethod, val local: Local)
  extends TaintAddress
case class RefTaintAddress(override val m: SootMethod, val ref: Ref)
  extends TaintAddress
case class ThisRefTaintAddress(override val m: SootMethod) extends TaintAddress
case class ParameterTaintAddress(override val m: SootMethod, val index: Int)
  extends TaintAddress
case class ConstantTaintAddress(override val m: SootMethod, c: Constant)
  extends TaintAddress
// case class ConstantTaintAddress(override val m: SootMethod)
  // extends TaintAddress
case class InvokeTaintAddress(override val m: SootMethod, ie: InvokeExpr)
  extends TaintAddress

////////////////////////////////////////
// Internal classes
////////////////////////////////////////

// Internal Kryo object that adds extra checking of the types and field
// structures of read and written objects to be sure they match what was
// expected.
class JaamKryo extends KryoBase {
  var seenClasses = Set[Class[_]]()

  // This is copied from Chill
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

  object CharsetSerializer extends kryo.Serializer[java.nio.charset.Charset] {
    override def write(kryo: Kryo, output: Output, obj: java.nio.charset.Charset) {
      kryo.writeObject(output, obj.asInstanceOf[java.nio.charset.Charset].name())
    }

    override def read(kryo: Kryo, input: Input, typ: Class[java.nio.charset.Charset]): java.nio.charset.Charset =
      java.nio.charset.Charset.forName(kryo.readObject(input, classOf[String]))
  }

  // Register those Charset classes which are not public.
  // See https://github.com/jagrutmehta/kryo-UTF/
  // Run the following to determine what classes need to be here:
  //   for i in ../rt.jar/sun/nio/cs/*.class; do javap $i; done|grep ' extends '|grep -v '^public'
  this.register(java.nio.charset.Charset.forName("UTF-8").getClass(), CharsetSerializer)
  this.register(java.nio.charset.Charset.forName("UTF-16").getClass(), CharsetSerializer)
  this.register(java.nio.charset.Charset.forName("UTF-16BE").getClass(), CharsetSerializer)
  this.register(java.nio.charset.Charset.forName("UTF-16LE").getClass(), CharsetSerializer)
  this.register(java.nio.charset.Charset.forName("x-UTF-16LE-BOM").getClass(), CharsetSerializer)
  this.register(java.nio.charset.Charset.forName("ISO_8859_1").getClass(), CharsetSerializer)
  //this.register(java.nio.charset.Charset.forName("US_ASCII").getClass(), CharsetSerializer)

  object UnmodifiableListSerializer extends kryo.Serializer[java.util.AbstractList[Object]] {
    override def write(kryo: Kryo, output: Output, obj: java.util.AbstractList[Object]) {
      kryo.writeObject(output, new java.util.ArrayList[Object](obj))
    }

    override def read(kryo: Kryo, input: Input, typ: Class[java.util.AbstractList[Object]]): java.util.AbstractList[Object] =
      com.strobel.core.ArrayUtilities.asUnmodifiableList[Object](kryo.readObject(input, classOf[java.util.ArrayList[Object]]):_*).asInstanceOf[java.util.AbstractList[Object]]
  }

  this.register(com.strobel.core.ArrayUtilities.asUnmodifiableList().getClass, UnmodifiableListSerializer)

  def forceFieldSerializer(clazz: Class[_]) { this.register(clazz, new FieldSerializer(this, clazz)) }

  forceFieldSerializer(classOf[com.strobel.assembler.metadata.ParameterDefinitionCollection])
  forceFieldSerializer(classOf[com.strobel.assembler.metadata.GenericParameterCollection])
  forceFieldSerializer(classOf[com.strobel.assembler.metadata.AnonymousLocalTypeCollection])
  forceFieldSerializer(classOf[com.strobel.assembler.metadata.VariableDefinitionCollection])

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

  override def newDefaultSerializer(typ : Class[_]) : kryo.Serializer[_] = {
    if (classOf[InsnList] == typ)
      // We can't use addDefaultSerializer due to shading in the assembly (TODO: check if still true)
      new InsnListSerializer()
    else if (classOf[AbstractInsnNode].isAssignableFrom(typ)) {
      // Subclasses of AbstractInsnNode should not try to serialize prev or
      // next. However, this requires working around a bug in
      // rebuildCachedFields. (See AbstractInsnNodeSerializer.)
      val s = new AbstractInsnNodeSerializer(this, typ)
      s.removeField("prev")
      s.removeField("next")
      s
    } else if (classOf[AstNode].isAssignableFrom(typ)) {
      new ProcyonRoleSerializer(typ)
    } else {
      super.newDefaultSerializer(typ)
    }
  }

  // Serializer for InsnList that avoids stack overflows due to recursively
  // following AbstractInsnNode.next.  This works on concert with
  // AbstractInsnNodeSerializer.
  class InsnListSerializer() extends kryo.Serializer[InsnList] {
    override def write(kryo : Kryo, output : Output, collection : InsnList) {
      output.writeVarInt(collection.size(), true)
      for (element <- collection.iterator)
        kryo.writeClassAndObject(output, element);
    }

    override def read(kryo : Kryo, input : Input, typ : Class[InsnList]) : InsnList = {
      val collection = new InsnList()
      kryo.reference(collection)
      val length = input.readVarInt(true)
      for (i <- Seq.range(0, length))
        collection.add(kryo.readClassAndObject(input).asInstanceOf[AbstractInsnNode])
      return collection
    }
  }

  // FieldSerializer.rebuildCachedFields has bugs relating to removed fields.
  // The following implementation works around these
  class AbstractInsnNodeSerializer(kryo : Kryo, typ : Class[_]) // TODO: _ < AbstractInsnNode // TODO: kryo is unused
      extends FieldSerializer(this, typ) {
    override def rebuildCachedFields(minorRebuild : Boolean) {
      // Save and clear removedFields since the below calls to removeField
      // will repopulate it.  Otherwise, we get a ConcurentModificationException.
      val removed = this.removedFields
      if (!minorRebuild) {
        this.removedFields = new java.util.HashSet()
      }
      super.rebuildCachedFields(minorRebuild)
      if (!minorRebuild) {
        for (field <- removed) {
          // Make sure to use toString otherwise you call a version of
          // removeField that uses pointer equality and thus no effect
          removeField(field.toString)
        }
      }
    }
  }

  // TODO: check if can put in register instead of newDefaultserializer
  // TODO: AstNode stores the index in flags instead of the Role (beware of the ROLE_INDEX_MASK); use setRoleUnsafe
  class ProcyonRoleSerializer[T](typ: Class[T])
      extends FieldSerializer[T](this, typ) {
    override def write(kryo : Kryo, output : Output, obj : T) {
      super.write(kryo, output, obj)
      output.writeAscii(obj.asInstanceOf[AstNode].getRole.getNodeType.toString)
      output.writeAscii(obj.asInstanceOf[AstNode].getRole.toString)
    }

    override def read(kryo : Kryo, input : Input, typ : Class[T]) : T = {
      val obj = super.read(kryo, input, typ)
      // must read string for role since otherwise only index stored
      val c = input.readString()
      val s = input.readString()
      // TODO: error if not found
      obj.asInstanceOf[AstNode].setRole(Role.get(roles((c,s)))) // TODO: if setRole throws errors, use setRoleUnsafe
      return obj
    }
  }


  val roles: immutable.Map[(String, String), Int] = {
    // Ensure that all Role objects have been put in the global table
    val classes = List(
      "com.strobel.decompiler.languages.java.ast.ArrayCreationExpression",
      "com.strobel.decompiler.languages.java.ast.AssertStatement",
      "com.strobel.decompiler.languages.java.ast.AssignmentExpression",
      "com.strobel.decompiler.languages.java.ast.AstNode",
      "com.strobel.decompiler.languages.java.ast.BinaryOperatorExpression",
      "com.strobel.decompiler.languages.java.ast.BlockStatement",
      "com.strobel.decompiler.languages.java.ast.BreakStatement",
      "com.strobel.decompiler.languages.java.ast.CaseLabel",
      "com.strobel.decompiler.languages.java.ast.CatchClause",
      "com.strobel.decompiler.languages.java.ast.ClassOfExpression",
      "com.strobel.decompiler.languages.java.ast.CompilationUnit",
      "com.strobel.decompiler.languages.java.ast.ComposedType",
      "com.strobel.decompiler.languages.java.ast.ConditionalExpression",
      "com.strobel.decompiler.languages.java.ast.ContinueStatement",
      "com.strobel.decompiler.languages.java.ast.DoWhileStatement",
      "com.strobel.decompiler.languages.java.ast.EntityDeclaration",
      "com.strobel.decompiler.languages.java.ast.ForEachStatement",
      "com.strobel.decompiler.languages.java.ast.ForStatement",
      "com.strobel.decompiler.languages.java.ast.GotoStatement",
      "com.strobel.decompiler.languages.java.ast.IfElseStatement",
      "com.strobel.decompiler.languages.java.ast.ImportDeclaration",
      "com.strobel.decompiler.languages.java.ast.InstanceOfExpression",
      "com.strobel.decompiler.languages.java.ast.LambdaExpression",
      "com.strobel.decompiler.languages.java.ast.MethodDeclaration",
      "com.strobel.decompiler.languages.java.ast.MethodGroupExpression",
      "com.strobel.decompiler.languages.java.ast.ObjectCreationExpression",
      "com.strobel.decompiler.languages.java.ast.ReturnStatement",
      "com.strobel.decompiler.languages.java.ast.Roles",
      "com.strobel.decompiler.languages.java.ast.SwitchSection",
      "com.strobel.decompiler.languages.java.ast.SwitchStatement",
      "com.strobel.decompiler.languages.java.ast.SynchronizedStatement",
      "com.strobel.decompiler.languages.java.ast.ThrowStatement",
      "com.strobel.decompiler.languages.java.ast.TryCatchStatement",
      "com.strobel.decompiler.languages.java.ast.UnaryOperatorExpression",
      "com.strobel.decompiler.languages.java.ast.WhileStatement",
      "com.strobel.decompiler.languages.java.ast.WildcardType"
    )
    // TODO: index map by java.lang.Class (AstNode.nodeType)
    classes.foreach(Class.forName(_))

    // Build a mapping from Role name to Role
    (for (
      i <- 0 until (1 << Role.ROLE_INDEX_BITS);
      role = Role.get(i);
      if role != null)
    yield { (role.getNodeType.toString, role.toString) -> i }).toMap
  }
}


