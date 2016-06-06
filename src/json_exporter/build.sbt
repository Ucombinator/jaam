name := "jaam-json_exporter"
version := "0.1-SNAPSHOT"
assemblyOutputPath in assembly := new File("./jars/jaam-json_exporter.jar")

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "3.3.0"
)
