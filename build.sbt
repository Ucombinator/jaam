////////////////////////////////////////
// Global settings
////////////////////////////////////////
name := "Jaam"
version := "0.1-SNAPSHOT"

organization := "org.ucombinator"
scalaVersion := "2.11.8"

assemblyOutputPath in assembly := new File("./jars/jaam.jar")

mainClass in Compile := Some("org.ucombinator.jaam.main.Main") // Silence warning about multiple main classes
mainClass in assembly := Some("org.ucombinator.jaam.main.Main") // Actually set main class in assembly

libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "2.0.1",
  "com.esotericsoftware" % "minlog" % "1.3.0",
  "com.google.guava" % "guava" % "20.0",
  "org.ow2.asm" % "asm-tree" % "5.1",
  "org.ow2.asm" % "asm-commons" % "5.1",
  "org.rogach" %% "scallop" % "2.0.1",
  "org.scala-lang" % "scala-reflect" % "2.11.8",
  "org.slf4j" % "slf4j-nop" % "1.7.22",
  "org.json4s" %% "json4s-jackson" % "3.3.0",
  "com.twitter" %% "chill" % "0.8.0",
  "de.javakaffee" % "kryo-serializers" % "0.38",
  "org.ucombinator.heros" % "heros" % "nightly.20161021",
  "org.ucombinator.soot" % "soot" % "nightly.20161021",
  "org.bitbucket.mstrobel" % "procyon-compilertools" % "0.5.32"
)

// Turn on all warnings
javacOptions in compile += "-Xlint"

// Jaam Agent mucks around with internals so we expect and it can safely
// ignore the warning:
//
//   <class> is internal proprietary API and may be removed in a future release
//
// NOTE: That warning is ignored only in code annotated with:
//
//   @SuppressWarnings("sunapi")
javacOptions in compile += "-XDenableSunApiLintControl"

/* Migrated from agent
assemblyOption in assembly :=
  (assemblyOption in assembly).value
  .copy(includeScala = false, includeDependency = false)
*/

// A "discard" merge strategy that doesn't cause a warning
lazy val quietDiscard = new sbtassembly.MergeStrategy {
  val name = "quietDiscard"
  def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] =
    Right(Nil)
  override def detailLogLevel = Level.Info
  override def summaryLogLevel = Level.Info
  override def notifyThreshold = 1
}

// Use repository containing soot-all-in-one nightly snapshot
resolvers += "Ucombinator maven repository on github" at "https://ucombinator.github.io/maven-repo"

// Flags to 'scalac'.  Try to get as much error and warn detection as possible.
scalacOptions ++= Seq(
  // Emit warning and location for usages of deprecated APIs.
  "-deprecation",
  // Explain type errors in more detail.
  "–explaintypes",
  // Emit warning and location for usages of features that should be imported explicitly.
  "-feature",
  // Generates faster bytecode by applying optimisations to the program
  "-optimise",
  // Enable additional warnings where generated code depends on assumptions.
  "-unchecked",
  "-Xlint:_"
)

// Discard META-INF (except for stuff in services which we need for jaam-agent).
// Deduplicate everything else.
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "services", xs @ _*) => MergeStrategy.singleOrError
  case PathList("META-INF", xs @ _*) => quietDiscard
  case x => MergeStrategy.deduplicate
}

// Use shading to avoid file conflicts in some problematic dependencies
assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("com.esotericsoftware.**" -> "shaded-kryo.@0")
    .inLibrary("com.esotericsoftware" % "kryo-shaded" % "3.0.3")
)

/* TODO: Get this to work
// Exclude tools.jar (from JDK) since not allowed to ship without JDK
  assemblyExcludedJars in assembly := {
      val cp = (fullClasspath in assembly).value
      cp filter {_.data.getName == "tools.jar"}
  }
*/
