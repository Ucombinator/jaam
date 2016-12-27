name := "jaam-interpreter"
assemblyOutputPath in assembly := new File("./jars/jaam-interpreter.jar")

libraryDependencies ++= Seq(
  "com.esotericsoftware" % "minlog" % "1.3.0",
  "com.google.guava" % "guava" % "20.0",
  "org.json4s" %% "json4s-jackson" % "3.3.0",
  "org.ow2.asm" % "asm-commons" % "5.1",
  "org.rogach" %% "scallop" % "2.0.1",
  "org.scala-lang" % "scala-reflect" % "2.11.8",
  "org.slf4j" % "slf4j-nop" % "1.7.22",
  "org.ucombinator.heros" % "heros" % "nightly.20161021",
  "org.ucombinator.soot" % "soot" % "nightly.20161021"
)
