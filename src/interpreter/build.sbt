name := "jaam-interpreter"
version := "0.1-SNAPSHOT"
assemblyOutputPath in assembly := new File("./jars/jaam-interpreter.jar")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.11.8",
  "org.ucombinator.soot" % "soot" % "nightly.20161021",
  "org.ucombinator.heros" % "heros" % "nightly.20161021",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.ow2.asm" % "asm-commons" % "5.1",
  "com.google.guava" % "guava" % "20.0",
  "org.rogach" %% "scallop" % "2.0.1",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "com.esotericsoftware" % "minlog" % "1.3.0",
  "org.json4s" %% "json4s-jackson" % "3.3.0"
)
