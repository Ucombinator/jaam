name := "jaam-serializer"
version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.ucombinator.soot" % "soot-all-in-one" % "nightly.20150205",
  "com.twitter" % "chill_2.10" % "0.8.0",
  "de.javakaffee" % "kryo-serializers" % "0.38"
)
