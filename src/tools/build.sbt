name := "jaam-tools"
version := "0.1-SNAPSHOT"
assemblyOutputPath in assembly := new File("./jars/jaam-tools.jar")

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.3.0",
  "com.google.guava" % "guava" % "20.0",
  "org.ow2.asm" % "asm-commons" % "5.1",
  "org.ucombinator.heros" % "heros" % "nightly.20161021",
  "org.slf4j" % "slf4j-api" % "1.7.21"
)
