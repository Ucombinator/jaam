package org.ucombinator.jaam.tools

import org.rogach.scallop._

object Conf {
  def extractSeqFromOptString(optString: ScallopOption[String], separator: String = ":"): Seq[String] = {
    optString.toOption.getOrElse("").split(separator).filter(_.nonEmpty)
  }

  class Print extends Main("print") {
    banner("Print a JAAM file in human-readable format")
    footer("")

    val state = opt[Int](argName = "state id", descr = "a specific state ID to print")
    val file = trailArg[java.io.File](descr = "a .jaam file to be printed")

    def run(conf: Conf) {
      state.toOption match {
        case None => Print.printFile(file().toString)
        case Some(st) => Print.printNodeFromFile(file().toString, st)
      }
    }
  }

  class Validate extends Main("validate") {
    banner("Amend an aborted JAAM serialization to allow reading.")
    footer("")

    val fixEof = opt[Boolean](descr = "whether to amend a JAAM file that ends abruptly")
    val addMissingStates = opt[Boolean](descr = "find hanging edges and add MissingState states so they go somewhere")
    val removeMissingStates = opt[Boolean](descr = "remove any MissingState states found in the serialization; overrides --addMissingStates")
    val targetFile = opt[String](descr = "the .jaam file to output a corrected version, if desired")
    val file = trailArg[java.io.File](descr = "a .jaam file to be truncated")

    def run(conf: Conf) {
      Validate.validateFile(
        jaamFile = file().toString,
        targetFile = targetFile.toOption,
        shouldAppendMissingEOF = fixEof(),
        shouldAddMissingStates = addMissingStates(),
        shouldRemoveMissingStates = removeMissingStates())
    }
  }

  class Info extends Main("info") {
    banner("Get simple information about a JAAM interpretation.")
    footer("")

    val file = trailArg[java.io.File](descr = "a .jaam file to be analyzed")

    def run(conf: Conf) {
      Info.analyzeForInfo(file().toString)
    }
  }

  class Cat extends Main("cat") {
    banner("Combine multile JAAM files into a single, cohesive file.")
    footer("")

    val outFile = trailArg[java.io.File](descr = "The desired output filename")
    val inFiles = trailArg[List[String]](descr = "The list of files to be concatenated.")

    def run(conf: Conf) {
      Cat.concatenateFiles(inFiles(), outFile().toString)
    }
  }

  class Coverage extends Main("coverage") {
    banner("Analyze a JAAM file against target JAR files to find JAAM coverage.")
    footer("")

    val jaamFile = trailArg[java.io.File](descr = "The JAAM file to analyze")
    val jars = trailArg[String](descr = "Colon-separated list of JAR files to directly compare coverage against")
    val additionalJars = opt[String](descr = "Colon-separated list of JAR files to complete class loading for inspection JAR files")

    def run(conf: Conf) {
      Coverage.findCoverage(jaamFile().toString, jars().split(":"), extractSeqFromOptString(additionalJars))
    }
  }

  class Coverage2 extends Main("coverage2") {
    banner("Analyze a JAAM file against target JAR files to find JAAM coverage.")
    footer("")

    val rtJar = trailArg[String](descr = "The RT.jar file to use for analysis")
    val jaamFile = trailArg[java.io.File](descr = "The JAAM file to analyze")
    val mainClass = trailArg[String](descr = "The name of the main class in the JAAM file")
    val jars = trailArg[String](descr = "Colon separated list of JAR files to directly compare coverage against")
    val additionalJars = opt[String](descr = "Colon-separated list of JAR files to complete class loading for inspection JAR files")

    def run(conf: Conf) {
      Coverage2.main(rtJar(), jaamFile().toString, mainClass(), jars().split(":"), extractSeqFromOptString(additionalJars))
    }
  }

  class MissingReturns extends Main("missing-returns") {
    banner("Find calls with no matching return")
    footer("")

    val jaamFile = trailArg[java.io.File](descr = "The JAAM file to analyze")

    def run(conf: Conf) {
      MissingReturns.missingReturns(jaamFile().toString)
    }
  }

  class LoopDepthCounter extends Main("loop") {
    banner("Analyze the number of depth of each loop in the application code")
    footer("")

    val loop = opt[Boolean](descr = "Run loop detection")
    val rec = opt[Boolean](descr = "Run recursion detection")
    val alloc = opt[Boolean](descr = "Run allocation detection")
    val nocolor = opt[Boolean](descr = "No coloring option if you want to redirect the output to some file or text editor",
                               default = Some(false))
    var remove_duplicates = opt[Boolean](name = "remove-duplicates", descr = "Only output deepest loop, may lose suspicious loops", default = Some(false))

    val mainClass = trailArg[String](descr = "The name of the main class")
    val mainMethod = trailArg[String](descr = "The name of entrance method")
    val jars = trailArg[String](descr = "Colon separated list of application's JAR files, not includes library")

    def run(conf: Conf) {
      val all = !(loop() || rec() || alloc())
      var color = !nocolor()
      LoopDepthCounter.main(mainClass(), mainMethod(), jars().split(":"), 
                            PrintOption(all, loop(), rec(), alloc(), color, remove_duplicates()))
    }
  }

  class ListItems extends Main("list") {
    banner("List all classes and methods in the JAR file")
    footer("")

    val noclasses = opt[Boolean](descr = "Do not print all classes")
    val nomethods = opt[Boolean](descr = "Do not print all methods")

    val jarFile = trailArg[java.io.File](descr = "The .jar file to analyze")

    def run(conf: Conf) {
      ListItems.main(jarFile().toString, ListPrintOption(!noclasses(), !nomethods()))
    }
  }

  class FindMain extends Main("find-main") {
    banner("Attempt to find the Main class from which to run the JAR file")
    footer("")

    val showerrs = opt[Boolean](name = "show-errors", short = 's', descr = "Show errors for unloadable classes")
    val force = opt[Boolean](name = "force-possibilities", short = 'f', descr = "Show all possibilities found manually, even if a main class is found in the manifest")
    val verifymanual = opt[Boolean](name = "validate", short = 'v', descr = "Check potential Main classes for a valid `main` method")

    val jars = trailArg[String](descr = "Colon-separated list of JAR files to directly search for `main` methods")

    def run(conf: Conf) {
      FindMain.main(jars().split(":"), showerrs(), force(), verifymanual())
    }
  }
}

class Conf(args : Seq[String]) extends ScallopConf(args = args) {
  banner("Usage: jaam-tools [subcommand] [options]")
  addSubcommand(new Conf.Print)
  addSubcommand(new Conf.Validate)
  addSubcommand(new Conf.Info)
  addSubcommand(new Conf.Cat)
  addSubcommand(new Conf.Coverage)
  addSubcommand(new Conf.Coverage2)
  addSubcommand(new Conf.MissingReturns)
  addSubcommand(new Conf.LoopDepthCounter)
  addSubcommand(new Conf.ListItems)
  addSubcommand(new Conf.FindMain)
  verify()
}

abstract class Main(name: String) extends Subcommand(name) {
  def run(conf : Conf)
}

object Main {
  def main(args : Array[String]) {
    val options = new Conf(args)
    options.subcommand match {
      case None => println("ERROR: No subcommand specified")
      case Some(m : Main) => m.run(options)
      case Some(other) => println("ERROR: Bad subcommand specified: " + other)
    }
  }
}
