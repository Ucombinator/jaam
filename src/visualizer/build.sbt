name := "jaam-visualizer"
version := "0.1-SNAPSHOT"
mainClass in assembly := Some("Main")
assemblyOutputPath in assembly := new File("./jars/jaam-visualizer.jar")

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "20.0"
)
