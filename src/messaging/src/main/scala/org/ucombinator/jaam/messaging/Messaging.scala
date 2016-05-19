package org.ucombinator.jaam.messaging

import de.javakaffee.kryoserializers._

abstract class Message {}

object Message {

case class Edge(x : Int, y : Int) {}

}

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
