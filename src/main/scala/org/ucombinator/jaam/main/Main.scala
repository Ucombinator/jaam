package org.ucombinator.jaam.main

import org.rogach.scallop._

class Conf(args : Seq[String]) extends ScallopConf(args = args) {
  banner("Usage: jaam-tools [subcommand] [options]")
  // TODO: short summary of each subcommand (w/ no options) in --help
  //addSubcommand(org.ucombinator.jaam.tools.app.App)
  //addSubcommand(org.ucombinator.jaam.tools.decompile.Decompile)
  //addSubcommand(org.ucombinator.jaam.tools.loop3.Loop3)
  addSubcommand(Visualizer)
  verify()
}

// TODO: auto-compute name
// TODO: 
abstract class Main(name: String /*= Main.SubcommandName(getClass())*/) extends Subcommand(name) {
  // TODO: rename to "main"?
  def run(): Unit

  //def toggle(args) = super.toggle(args, prefix = "no-")
}

object Main {
  // TODO: support '-jar" with main-class
  def conf = _conf
  private var _conf: Conf = _

  // short-subcommand help
  def main(args : Array[String]) {
    _conf = new Conf(args)
    _conf.subcommand match {
      case None => println("ERROR: No subcommand specified")
      case Some(m : Main) => m.run()
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

object Visualizer extends Main("visualizer") {
  import javafx.application.Application

  def run() {
    Application.launch(classOf[org.ucombinator.jaam.visualizer.main.Main], Main.conf.args:_*)
  }
}
