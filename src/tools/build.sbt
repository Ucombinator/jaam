name := "jaam-tools"
version := "0.1-SNAPSHOT"
assemblyOutputPath in assembly := new File("./jars/jaam-tools.jar")

libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "2.0.1"
)
