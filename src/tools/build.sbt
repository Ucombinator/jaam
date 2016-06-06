name := "jaam-tools"
version := "0.1-SNAPSHOT"
assemblyOutputPath in assembly := new File("./jars/jaam-tools.jar")

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.3.0"
)
