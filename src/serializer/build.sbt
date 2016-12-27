name := "jaam-serializer"

libraryDependencies ++= Seq(
  "com.twitter" %% "chill" % "0.8.0",
  "de.javakaffee" % "kryo-serializers" % "0.38",
  "org.ow2.asm" % "asm-tree" % "5.1",
  "org.ucombinator.soot" % "soot" % "nightly.20161021"
)
