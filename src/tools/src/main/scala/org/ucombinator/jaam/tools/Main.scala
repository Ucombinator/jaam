package org.ucombinator.jaam.tools

import org.rogach.scallop._

object Conf {
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
    val file = trailArg[java.io.File](descr = "a .jaam file to be truncated")

    def run(conf: Conf) {
      Validate.validateFile(
        jaamFile = file().toString,
        shouldAppendMissingEOF = fixEof(),
        addMissingStates = addMissingStates(),
        removeMissingStates = removeMissingStates())
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
      Coverage.findCoverage(jaamFile().toString, jars().split(":"), additionalJars.toString().split(":"))
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
      val s_additionalJars: Seq[String] = additionalJars.toOption match {
        case Some(s) => s.split(":")
        case None => Seq[String]()
      }

      Coverage2.main(rtJar(), jaamFile().toString, mainClass(), jars().split(":"), s_additionalJars)
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
    }
  }
}
