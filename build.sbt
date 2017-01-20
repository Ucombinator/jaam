// TODO: error if sbt called directly on a sub-project

////////////////////////////////////////
// Global settings
////////////////////////////////////////

version in Global := "0.1-SNAPSHOT"

organization in Global := "org.ucombinator"
scalaVersion in Global := "2.11.8"

// Do not create assembly for default root project
disablePlugins(sbtassembly.AssemblyPlugin)

////////////////////////////////////////
// Settings shared between sub-projects
////////////////////////////////////////

// A "discard" merge strategy that doesn't cause a warning
lazy val quietDiscard = new sbtassembly.MergeStrategy {
  val name = "quietDiscard"
  def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] =
    Right(Nil)
  override def detailLogLevel = Level.Info
  override def summaryLogLevel = Level.Info
  override def notifyThreshold = 1
}

lazy val commonSettings = Seq(
  // Use repository containing soot-all-in-one nightly snapshot
  resolvers += "Ucombinator maven repository on github" at "https://ucombinator.github.io/maven-repo",

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
  ),

  // Discard META-INF (except for stuff in services which we need for jaam-agent).
  // Deduplicate everything else.
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "services", xs @ _*) => MergeStrategy.singleOrError
    case PathList("META-INF", xs @ _*) => quietDiscard
    case x => MergeStrategy.deduplicate
  },

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
)

////////////////////////////////////////
// Sub-projects
////////////////////////////////////////

lazy val serializer = (project in file("src/serializer"))
  .settings(commonSettings)

lazy val tools = (project in file("src/tools"))
  .settings(commonSettings)
  .dependsOn(serializer)

lazy val interpreter = (project in file("src/interpreter"))
  .settings(commonSettings)
  .dependsOn(serializer)

lazy val visualizer = (project in file("src/visualizer"))
  .settings(commonSettings)
  .dependsOn(serializer)

lazy val analyzer = (project in file("src/analyzer"))
  .settings(commonSettings)
  .dependsOn(serializer)

lazy val json_exporter = (project in file("src/json_exporter"))
  .settings(commonSettings)
  .dependsOn(serializer)

lazy val agent = (project in file("src/agent"))
  .settings(commonSettings)
