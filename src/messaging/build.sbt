name := "jaam-messaging"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.ucombinator.soot" % "soot-all-in-one" % "nightly.20150205",
  "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.scala-lang" % "scala-reflect" % "2.10.6",
  "com.twitter" % "chill_2.10" % "0.8.0",
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
