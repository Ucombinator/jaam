package org.ucombinator.jaam.tools.loop3

import org.ucombinator.jaam.tools
import soot.PackManager
import soot.options.Options


object Loop3 extends tools.Main("loop3") {
  val classpath = opt[List[String]](descr = "TODO")

  def run(conf: tools.Conf) {
    Main.main(classpath.getOrElse(List()))
  }
}

object Main {
  def main(classpath: List[String]) {
    Options.v().set_verbose(false)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    Options.v().set_soot_classpath(classpath.mkString(":"))
    Options.v().set_include_all(true)
    Options.v().set_prepend_classpath(false)
    Options.v().set_src_prec(Options.src_prec_only_class)
    //Options.v().set_whole_program(true)-
    //Options.v().set_app(true)-
    soot.Main.v().autoSetOptions()

    //Options.v().setPhaseOption("cg", "verbose:true")
    //Options.v().setPhaseOption("cg.cha", "enabled:true")

    //Options.v.set_main_class(mainClass)
    //Scene.v.setMainClass(clazz)
    //Scene.v.addBasicClass(className, SootClass.HIERARCHY)
    //Scene.v.setSootClassPath(classpath)
    //Scene.v.loadNecessaryClasses

    PackManager.v.runPacks

    println("END")
  }
}
