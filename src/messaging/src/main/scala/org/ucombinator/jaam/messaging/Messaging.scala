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


/*
//A 	getAnnotation(Class<A> annotationClass)
//Annotation[] 	getAnnotations()
String 	getCanonicalName()
//Class<?>[] 	getClasses()
//ClassLoader 	getClassLoader()
//Class<?> 	getComponentType()
//Constructor<T> 	getConstructor(Class<?>... parameterTypes)
//Constructor<?>[] 	getConstructors()
//Annotation[] 	getDeclaredAnnotations()
//Class<?>[] 	getDeclaredClasses()
//Constructor<T> 	getDeclaredConstructor(Class<?>... parameterTypes)
//Constructor<?>[] 	getDeclaredConstructors()
//Field 	getDeclaredField(String name)
Field[] 	getDeclaredFields()
//Method 	getDeclaredMethod(String name, Class<?>... parameterTypes)
//Method[] 	getDeclaredMethods()
//Class<?> 	getDeclaringClass()
//Class<?> 	getEnclosingClass()
//Constructor<?> 	getEnclosingConstructor()
//Method 	getEnclosingMethod()
//T[] 	getEnumConstants()
//Field 	getField(String name)
Field[] 	getFields()
//Type[] 	getGenericInterfaces()
Type 	getGenericSuperclass()
//Class<?>[] 	getInterfaces()
//Method 	getMethod(String name, Class<?>... parameterTypes)
//Method[] 	getMethods()
int 	getModifiers()
//String 	getName()
Package 	getPackage()
//ProtectionDomain 	getProtectionDomain()
//URL 	getResource(String name)
//InputStream 	getResourceAsStream(String name)
//Object[] 	getSigners()
//String 	getSimpleName()
Class<? super T> 	getSuperclass()
//TypeVariable<Class<T>>[] 	getTypeParameters()




final Kryo kryo;
final Class type;
/** type variables declared for this type */
final TypeVariable[] typeParameters;
final Class componentType;
protected final FieldSerializerConfig config;
private CachedField[] fields = new CachedField[0];
private CachedField[] transientFields = new CachedField[0];
protected HashSet<CachedField> removedFields = new HashSet();
Object access;
private FieldSerializerUnsafeUtil unsafeUtil;

private FieldSerializerGenericsUtil genericsUtil;

private FieldSerializerAnnotationsUtil annotationsUtil;

/** Concrete classes passed as values for type variables */
private Class[] generics;

private Generics genericsScope;

/** If set, this serializer tries to use a variable length encoding for int and long fields */
private boolean varIntsEnabled;

/** If set, adjacent primitive fields are written in bulk This flag may only work with Oracle JVMs, because they layout
 * primitive fields in memory in such a way that primitive fields are grouped together. This option has effect only when used
 * with Unsafe-based FieldSerializer.
 * <p>
 * FIXME: Not all versions of Sun/Oracle JDK properly work with this option. Disable it for now. Later add dynamic checks to
 * see if this feature is supported by a current JDK version.
 * </p> */
private boolean useMemRegions = false;

private boolean hasObjectFields = false;

static CachedFieldFactory asmFieldFactory;
static CachedFieldFactory objectFieldFactory;
static CachedFieldFactory unsafeFieldFactory;

static boolean unsafeAvailable;
static Class<?> unsafeUtilClass;
static Method sortFieldsByOffsetMethod;


 */

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

/*


diff --git a/build.sbt b/build.sbt
index 3cc4d85..0786c26 100644
--- a/build.sbt
+++ b/build.sbt
@@ -23,7 +23,8 @@ libraryDependencies ++= Seq(
   "com.github.scopt" %% "scopt" % "3.3.0",
   "org.scala-lang" % "scala-reflect" % "2.10.6",
   "com.esotericsoftware" % "kryo" % "3.0.3",
-  "com.twitter" %% "chill" % "0.3.6"
+  "com.twitter" %% "chill" % "0.3.6",
+  "de.javakaffee" % "kryo-serializers" % "0.37"
 )
 
 scalacOptions ++= Seq("-unchecked", "-deprecation")
diff --git a/src/main/scala/org/ucombinator/jaam/Main.scala b/src/main/scala/org/ucombinator/jaam/Main.scala
index c268509..c179b5a 100644
--- a/src/main/scala/org/ucombinator/jaam/Main.scala
+++ b/src/main/scala/org/ucombinator/jaam/Main.scala
@@ -56,6 +56,8 @@ import com.mxgraph.layout.mxCompactTreeLayout
 import org.json4s._
 import org.json4s.native._
 
+import de.javakaffee.kryoserializers._
+import com.esotericsoftware.minlog.Log
 import com.esotericsoftware.kryo._
 import com.esotericsoftware.kryo.io._
 import com.twitter.chill.ScalaKryoInstantiator
@@ -1323,6 +1325,8 @@ object Main {
     val instantiator = new ScalaKryoInstantiator
     instantiator.setRegistrationRequired(false)
     val kryo = instantiator.newKryo()
+kryo.addDefaultSerializer(classOf[soot.util.Chain[java.lang.Object]], classOf[com.esotericsoftware.kryo.serializers.FieldSerializer[java.lang.Object]])
+UnmodifiableCollectionsSerializer.registerSerializers( kryo )
 
     val mainMainMethod : SootMethod = Soot.getSootClass(config.className).getMethodByName(config.methodName)
 
@@ -1351,14 +1355,16 @@ object Main {
       // Serialization test
       val arrOut: ByteArrayOutputStream = new ByteArrayOutputStream()
       val output = new Output(arrOut)
-      kryo.writeObject(output, current)
+      kryo.writeClassAndObject(output, current)
       output.close()
       
       val arrIn: ByteArrayInputStream = new ByteArrayInputStream(arrOut.toByteArray())
       val input = new Input(arrIn)
       
       if (current.isInstanceOf[State]) {
-        val current1 = kryo.readObject(input, classOf[State])
+        println("Just before the error")
+        val current1 = kryo.readClassAndObject(input)
+        println("Just after the error")
         println(current1)
       }
       else {
 */



/*

name := "jaam-messaging"

version := "0.1"

libraryDependencies ++= Seq(
  "org.ucombinator.soot" % "soot-all-in-one" % "nightly.20150205",
  "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.scala-lang" % "scala-reflect" % "2.10.6",
  "com.twitter" %% "chill" % "0.3.6",
  "de.javakaffee" % "kryo-serializers" % "0.37"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

//mainClass in (Compile, assembly) := Some("org.ucombinator.jaam.Main")

// Assembly-specific configuration
//test in assembly := {}
//assemblyOutputPath in assembly := new File("./jaam.jar")

// META-INF discarding
//assemblyMergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
//{
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x => MergeStrategy.first
//}
//}

 */
