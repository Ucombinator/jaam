package org.ucombinator.jaam.main

import org.rogach.scallop._
import java.io._
import scala.collection.JavaConverters._

class MainConf(args : Seq[String]) extends ScallopConf(args = args) with JaamConf {
  shortSubcommandsHelp(true)
  // TODO: version: jvm version? Jaam-file header hash?

  banner("Usage: jaam-tools [subcommand] [options]")
  // TODO: short summary of each subcommand (w/ no options) in --help
  addSubcommand(Visualizer)
  addSubcommand(Interpreter)
  addSubcommand(Agent)
  addSubcommand(Cat)
  addSubcommand(App)
  addSubcommand(Coverage)
  addSubcommand(Coverage2)
  addSubcommand(Decompile)
  addSubcommand(FindMain)
  addSubcommand(Info)
  addSubcommand(ListItems)
  addSubcommand(LoopDepthCounter)
  addSubcommand(LoopAnalyzer)
  addSubcommand(Loop3)
  addSubcommand(MissingReturns)
  addSubcommand(Print)
  addSubcommand(Taint)
  addSubcommand(Validate)
  addSubcommand(DecompileToFile)

  verify()
}

abstract class Main(name: String /* TODO: = Main.SubcommandName(getClass())*/) extends Subcommand(name) with JaamConf {
  // TODO: descr()
  def run(): Unit

  import scala.collection.immutable
  // Move to conf?
  val waitForUser = toggle(
    descrYes = "wait for user to press enter before starting (default: off)", // TODO: note: usefull for debugging and profiling
    noshort = true, prefix = "no-", default = Some(false))

  // Move to JaamConf
  val color = toggle(prefix = "no-", default = Some(true))

  val logLevel = enum[Log.Level](
    descr = "the level of logging verbosity",
    default = Some("warn"),
    argType = "log level",
    elems = immutable.ListMap(
      "none" -> Log.LEVEL_NONE,
      "error" -> Log.LEVEL_ERROR,
      "warn" -> Log.LEVEL_WARN,
      "info" -> Log.LEVEL_INFO,
      "debug" -> Log.LEVEL_DEBUG,
      "trace" -> Log.LEVEL_TRACE))
}

object Main {
  // TODO: support '-jar" with main-class
  def conf = _conf
  private var _conf: MainConf = _

  // short-subcommand help
  def main(args : Array[String]) {

    _conf = new MainConf(args)
    _conf.subcommand match {
      case None => println("ERROR: No subcommand specified")
      case Some(m : Main) =>
        if (m.waitForUser()) { // TODO: move to main.Main
          print("Press enter to start.")
          scala.io.StdIn.readLine()
        }

        Log.setLogging(m.logLevel()) // TODO: move to main.Main
        Log.color = m.color()


        m.run()
      case Some(other) => println("ERROR: Bad subcommand specified: " + other)
    }
  }

  def SubcommandName(o: Class[_]): String = {
    val name = o.getName().flatMap(c => if (c.isUpper) Seq('-', c.toLower) else Seq(c))
    name match {
      case s if s.startsWith("-") => s.stripPrefix("-")
      case s => s
    }
  }
}

/****************
 * Subcommands  *
 ****************/

object Visualizer extends Main("visualizer") {
  def run() {
    import javafx.application.Application

    Application.launch(classOf[org.ucombinator.jaam.visualizer.main.Main], Main.conf.args:_*)
  }
}

object Interpreter extends Main("interpreter") {
  // TODO: banner("")
  // TODO: how do we turn off sorting of options by name?
  import org.ucombinator.jaam.interpreter.AbstractState
  import scala.collection.immutable

  val classpath   = opt[String](required = true, short = 'P', descr = "the TODO class directory")
  val rtJar       = opt[String](required = true, short = 'J', descr = "the rt.jar file")
  val mainClass   = opt[String](required = true, short = 'c', descr = "the main class")
  val method      = opt[String](required = true, short = 'm', descr = "the main method", default = Some("main"))
  val libClasses  = opt[String](short = 'L', descr = "app's library classes")
  val _outfile     = opt[String](name = "outfile", short = 'o', descr = "the output file for the serialized data")
  def outfile() = _outfile.getOrElse(mainClass() + ".jaam") // TODO: extend scallop to do this for us

  val globalSnowflakeAddrLast = toggle(
    descrYes = "process states that read from the `GlobalSnowflakeAddr` last (default: on)",
    noshort = true, prefix = "no-", default = Some(true))

  val stateOrdering = enum[Boolean => Ordering[AbstractState]](
    default = Some("max"),
    argType = "ordering",
    elems = immutable.ListMap(
      "none" -> StateOrdering.none,
      "max" -> StateOrdering.max
        // TODO: state orderings: min insertion reverseInsertion
    ))

  val maxSteps = opt[Int](descr = "maximum number of interpretation steps")

  val stringTop = toggle(prefix = "no-", default = Some(true))

  val snowflakeLibrary = toggle(prefix = "no-", default = Some(true))

  val exceptions = toggle(prefix = "no-", default = Some(true))

  val initialStore = opt[java.io.File]()

  val analyzer = enum[Analyzer](
    descr = "which analyzer to use",
    default = Some("cha"),
    argType = "analyzer",
    elems = immutable.ListMap(
      "aam" -> AAM,
      "cha" -> CHA))
  sealed trait Analyzer {}
  final case object AAM extends Analyzer {}
  final case object CHA extends Analyzer {}

  object StateOrdering {
    def none(b: Boolean) = new Ordering[AbstractState] {
      override def compare(x: AbstractState, y: AbstractState): Int =
        0
    }

    def max(b: Boolean) = new Ordering[AbstractState] {
      override def compare(x: AbstractState, y: AbstractState): Int =
        // TODO: use `b`
        if (x.id < y.id) { -1 }
        else if (x.id == y.id) { 0 }
        else { +1 }
    }
  }

  def run() {
    analyzer() match { // TODO: as separate commands
      case AAM => org.ucombinator.jaam.interpreter.Main.aam()
      case CHA => org.ucombinator.jaam.interpreter.Main.cha()
    }
  }
}

object Agent extends Main("agent") {
  val arguments = trailArg[List[String]](default = Some(List()))

  def run() {
    var java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"

    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      java += ".exe"
    }

    val agentClass = classOf[org.ucombinator.jaam.agent.Main]
    val jar = agentClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()

    val cmd = List(
      java,
      "-javaagent:" + jar,
      "-classpath", jar,
      agentClass.getCanonicalName()) ++ arguments()

    System.exit(new ProcessBuilder(cmd.asJava).inheritIO().start().waitFor())
  }
}

// TODO: agent is special because we have to launch a new process

object Cat extends Main("cat") {
  banner("Combine multile JAAM files into a single, cohesive file.")
  footer("")

  val input = inputOpt()
  val output = outputOpt()

  def run() {
    org.ucombinator.jaam.tools.cat.Cat.concatenateFiles(input(), output())
  }
}

object App extends Main("app") {
  banner("TODO")
  footer("")

  val input = opt[List[String]](descr = "class files, or directories (role is auto-detected)", default = Some(List()))
  val app = opt[List[String]](short = 'a', descr = "application jars, class files, or directories", default = Some(List()))
  val lib = opt[List[String]](short = 'l', descr = "library jars, class files, or directories", default = Some(List()))
  val jvm = opt[List[String]](short = 'r', descr = "Java runtime jars, class files, or directories", default = Some(List()))
  val defaultJvm = toggle(prefix = "no-", default = Some(true))

  val detectMain = toggle(default = Some(true))
  val mainClass = opt[String](short = 'c', descr = "the main class")
  val mainMethod = opt[String](short = 'm', descr = "the main method")

  val output = outputOpt()

  // TODO: val java-8-rt (in resource?)

  def run() {
    org.ucombinator.jaam.tools.app.Main.main(input(), app(), lib(), jvm(), defaultJvm(), detectMain(), mainClass.toOption, mainMethod.toOption, output())
  }
}


object Coverage extends Main("coverage") {
  banner("Analyze a JAAM file against target JAR files to find JAAM coverage.")
  footer("")

  val jaamFile = trailArg[java.io.File](descr = "The JAAM file to analyze")
  val jars = trailArg[String](descr = "Colon-separated list of JAR files to directly compare coverage against")
  val additionalJars = opt[String](descr = "Colon-separated list of JAR files to complete class loading for inspection JAR files")

  def run() {
    org.ucombinator.jaam.tools.coverage.Coverage.findCoverage(jaamFile().toString, jars().split(":"), JaamConf.extractSeqFromOptString(additionalJars))
  }
}

object Coverage2 extends Main("coverage2") {
  banner("Analyze a JAAM file against target JAR files to find JAAM coverage.")
  footer("")

  val rtJar = trailArg[String](descr = "The RT.jar file to use for analysis")
  val jaamFile = trailArg[java.io.File](descr = "The JAAM file to analyze")
  val mainClass = trailArg[String](descr = "The name of the main class in the JAAM file")
  val jars = trailArg[String](descr = "Colon separated list of JAR files to directly compare coverage against")
  val additionalJars = opt[String](descr = "Colon-separated list of JAR files to complete class loading for inspection JAR files")

  def run() {
    org.ucombinator.jaam.tools.coverage2.Coverage2.main(rtJar(), jaamFile().toString, mainClass(), jars().split(":"), JaamConf.extractSeqFromOptString(additionalJars))
  }
}


object Decompile extends Main("decompile") {
  banner("TODO")
  footer("")

//  val append = toggle(
//    descrYes = "wait for user to press enter before starting (default: off)",
//    noshort = true, prefix = "no-", default = Some(false))

  val jvm = toggle(
    descrYes = "wait for user to press enter before starting (default: off)",
    noshort = true, prefix = "no-", default = Some(false))
  val lib = toggle(
    descrYes = "wait for user to press enter before starting (default: off)",
    noshort = true, prefix = "no-", default = Some(false))
  val app = toggle(
    descrYes = "wait for user to press enter before starting (default: true)",
    noshort = true, prefix = "no-", default = Some(true))

  val exclude = opt[List[String]](descr = "Class names to omit", default = Some(List()))
  val input = inputOpt()
  val output = outputOpt()

  def run() {
    org.ucombinator.jaam.tools.decompile.Main.main(input(), output(), exclude(), jvm(), lib(), app())
  }
}


object FindMain extends Main("find-main") {
  banner("Attempt to find the Main class from which to run the JAR file")
  footer("")

  val showerrs = opt[Boolean](name = "show-errors", short = 's', descr = "Show errors for unloadable classes")
  val force = opt[Boolean](name = "force-possibilities", short = 'f', descr = "Show all possibilities found manually, even if a main class is found in the manifest")
  val verifymanual = opt[Boolean](name = "validate", short = 'v', descr = "Check potential Main classes for a valid `main` method")
  val anyClass = opt[Boolean](descr = "Check all classes not just those named Main")

  val jars = trailArg[String](descr = "Colon-separated list of JAR files to directly search for `main` methods")

  def run() {
    org.ucombinator.jaam.tools.findmain.FindMain.main(jars().split(":"), showerrs(), force(), verifymanual(), anyClass())
  }
}


object Info extends Main("info") {
  banner("Get simple information about a JAAM interpretation.")
  footer("")

  val file = trailArg[java.io.File](descr = "a .jaam file to be analyzed")

  def run() {
    org.ucombinator.jaam.tools.info.Info.analyzeForInfo(file().toString)
  }
}


object ListItems extends Main("list") {
  banner("List all classes and methods in the JAR file")
  footer("")

  val noclasses = opt[Boolean](descr = "Do not print all classes")
  val nomethods = opt[Boolean](descr = "Do not print all methods")

  val jarFile = trailArg[java.io.File](descr = "The .jar file to analyze")

  def run() {
    org.ucombinator.jaam.tools.listitems.ListItems.main(jarFile().toString, org.ucombinator.jaam.tools.listitems.ListPrintOption(!noclasses(), !nomethods()))
  }
}


object LoopDepthCounter extends Main("loop") {
  banner("Analyze the number of depth of each loop in the application code")
  footer("")

  val graph = opt[Boolean](descr = "Print loops to GraphViz file")
  val loop = opt[Boolean](descr = "Run loop detection")
  val rec = opt[Boolean](descr = "Run recursion detection")
  val alloc = opt[Boolean](descr = "Run allocation detection")
  val nocolor = opt[Boolean](descr = "No coloring option if you want to redirect the output to some file or text editor",
                             default = Some(false))
  var remove_duplicates = opt[Boolean](name = "remove-duplicates", descr = "Only output deepest loop, may lose suspicious loops", default = Some(false))

  val mainClass = trailArg[String](descr = "The name of the main class")
  val mainMethod = trailArg[String](descr = "The name of entrance method")
  val jars = trailArg[String](descr = "Colon separated list of application's JAR files, not includes library")

  def run() {
    val all = !(loop() || rec() || alloc())
    var color = !nocolor()
    org.ucombinator.jaam.tools.loop.LoopDepthCounter.main(mainClass(), mainMethod(), jars().split(":"), 
                          org.ucombinator.jaam.tools.loop.PrintOption(all, loop(), rec(), alloc(), color, remove_duplicates(), graph()))
  }
}


object LoopAnalyzer extends Main("loop2") {
  banner("Analyze the depth of each loop in the application code")
  footer("")

  // val graph = opt[Boolean](descr = "Print loops to GraphViz dot file")
  // val rec = opt[Boolean](descr = "Run recursion detection")
  // TODO name this
  val prune = toggle(
      descrYes = "Remove methods without outgoing edges from graph",
      descrNo = "Do not remove methods without outgoing edges from graph",
      default = Some(true))
  val shrink = toggle(descrYes = "Skip methods without loops",
      descrNo = "Include methods without loops", default = Some(true))
  val prettyPrint = toggle(descrYes = "Pretty print found loops", default = Some(false))

  val mainClass = trailArg[String](descr = "The name of the main class")
  val mainMethod = trailArg[String](descr = "The name of the main method")
  val classpath = trailArg[String](descr =
      "Colon-separated list of JAR files and directories")
  val output = opt[String](descr = "An output file for the dot output")
  val coverage = opt[String](descr = "An output file for the coverage output")
  val jaam = opt[String](short = 'j', descr = "the output file for the serialized data")

  def run(): Unit = {
    val outStream: PrintStream = output.toOption match {
      case None => System.out
      case Some(f) => new PrintStream(new FileOutputStream(f))
    }
    val coverageStream: PrintStream = coverage.toOption match {
      case None => System.out
      case Some(f) => new PrintStream(new FileOutputStream(f))
    }
    org.ucombinator.jaam.tools.LoopAnalyzer.main(mainClass(), mainMethod(), classpath(), outStream, coverageStream, jaam.toOption,
        prune(), shrink(), prettyPrint())
  }
}


object Loop3 extends Main("loop3") {
  //val classpath = opt[List[String]](descr = "TODO")
  val input = inputOpt()
  val output = outputOpt()

  val prune = toggle(
      descrYes = "Remove methods without outgoing edges from graph",
      descrNo = "Do not remove methods without outgoing edges from graph",
      default = Some(true))
  val shrink = toggle(descrYes = "Skip methods without loops",
      descrNo = "Include methods without loops", default = Some(true))
  val prettyPrint = toggle(descrYes = "Pretty print found loops", default = Some(false))

  def run() {
    //Main.main(classpath.getOrElse(List()))
    org.ucombinator.jaam.tools.loop3.Main.main(input.getOrElse(List()), output.toOption, prune(), shrink(), prettyPrint())
  }
}


object MissingReturns extends Main("missing-returns") {
  banner("Find calls with no matching return")
  footer("")

  val jaamFile = trailArg[java.io.File](descr = "The JAAM file to analyze")

  def run() {
    org.ucombinator.jaam.tools.missingreturns.MissingReturns.missingReturns(jaamFile().toString)
  }
}


object Print extends Main("print") {
  banner("Print a JAAM file in human-readable format")
  footer("")

  val state = opt[Int](argName = "state id", descr = "a specific state ID to print")
  val file = trailArg[java.io.File](descr = "a .jaam file to be printed")

  def run() {
    state.toOption match {
      case None => org.ucombinator.jaam.tools.printer.Print.printFile(file().toString)
      case Some(st) => org.ucombinator.jaam.tools.printer.Print.printNodeFromFile(file().toString, st)
    }
  }
}


object Taint extends Main("taint") {
  banner("Identify explicit intra-procedural information flows in a method")
  footer("")

  // TODO: specify required options
  val className = opt[String](descr = "FQN (package and class) of the class being analyzed")
  val method = opt[String](descr = "signature of the method being analyzed; e.g., \"void main(java.lang.String[])\"")
  val instruction = opt[Int](descr = "index into the Unit Chain that identifies the instruction", validate = { _ >= 0 })
  val implicitFlows = opt[Boolean](descr = "TODO:implement")
  val output = opt[java.io.File](descr = "a .dot file to be printed")
  // really, this just gets used as the class path
  val path = opt[String](descr = "java classpath (including jar files), colon-separated")
  // val rtJar = opt[String](descr = "The RT.jar file to use for analysis",
      // default = Some("resources/rt.jar"), required = true)

  def run() {
    val cp = path.toOption match {
      case Some(str) => str
      case None => ""
    }

    val ps = output.toOption match {
      case Some(file) => new PrintStream(new FileOutputStream(file))
      case None => System.out
    }

    org.ucombinator.jaam.tools.Taint.run(className(), method(), instruction(), implicitFlows(), cp, ps)
  }
}


object Validate extends Main("validate") {
  banner("Amend an aborted JAAM serialization to allow reading.")
  footer("")

  val fixEof = opt[Boolean](descr = "whether to amend a JAAM file that ends abruptly")
  val addMissingStates = opt[Boolean](descr = "find hanging edges and add MissingState states so they go somewhere")
  val removeMissingStates = opt[Boolean](descr = "remove any MissingState states found in the serialization; overrides --addMissingStates")
  val targetFile = opt[String](descr = "the .jaam file to output a corrected version, if desired")
  val file = trailArg[java.io.File](descr = "a .jaam file to be truncated")

  def run() {
    org.ucombinator.jaam.tools.validate.Validate.validateFile(
      jaamFile = file().toString,
      targetFile = targetFile.toOption,
      shouldAppendMissingEOF = fixEof(),
      shouldAddMissingStates = addMissingStates(),
      shouldRemoveMissingStates = removeMissingStates())
  }
}

object DecompileToFile extends Main("decompile-to-file") {
  banner("TODO")
  footer("")

  val input = opt[List[String]](default = Some(List()))
  val output = opt[String](required = true)

  def run() {
    org.ucombinator.jaam.tools.decompileToFile.DecompileToFile.main(
      input = input(), output = output())
  }
}
