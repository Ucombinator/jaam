package org.ucombinator.jaam.tools.loop3

import org.ucombinator.jaam.tools
import soot.{ PackManager, Scene, SootClass, SootMethod, SootResolver }
import soot.options.Options

import scala.collection.JavaConverters._

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

    Scene.v.loadBasicClasses()
    PackManager.v.runPacks()

    def getSootClass(s : String) = Scene.v().loadClass(s, SootClass.SIGNATURES)
    def getBody(m : SootMethod) = {
      if (m.isNative) { throw new Exception("Attempt to Soot.getBody on native method: " + m) }
      if (m.isAbstract) { throw new Exception("Attempt to Soot.getBody on abstract method: " + m) }
      // TODO: do we need to test for phantom here?
      if (!m.hasActiveBody()) {
        SootResolver.v().resolveClass(m.getDeclaringClass.getName, SootClass.BODIES)
        m.retrieveActiveBody()
      }
      m.getActiveBody
    }

    //println("dc" + Scene.v.dynamicClasses().asScala)
    println("ap" + Scene.v.getApplicationClasses().asScala)
    println("al" + Scene.v.getClasses().asScala)
    println("dan" + Scene.v.getClasses(SootClass.DANGLING).asScala)
    println("lib" + Scene.v.getLibraryClasses().asScala)
    println("phan" + Scene.v.getPhantomClasses().asScala)

    val c = getSootClass("java.lang.Object")
    println("hier " + Scene.v.getActiveHierarchy())
    println("hier sub " + Scene.v.getActiveHierarchy().getSubclassesOf(c))
    println("fast hier " + Scene.v.getOrMakeFastHierarchy())
    println("hier sub " + Scene.v.getFastHierarchy().getSubclassesOf(c))
    println("entry " + Scene.v.getEntryPoints().asScala)
    //println("main " + Scene.v.getMainClass())
    println("pkg " + Scene.v.getPkgList())


//    val c = getSootClass("edu.cyberapex.home.StacMain")
//    println("Class: " + c)
//    for (m <- c.getMethods.asScala) {
//      println("Method: " + m)
//      for (s <- getBody(m).getUnits.asScala) {
//        println("Stmt: " + s)
//      }
//    }

    var class_count = 0
    var method_count = 0
    var stmt_count = 0

    for (p <- classpath) {
      println(f"p $p")
      val jar = new java.util.jar.JarInputStream(
        new java.io.FileInputStream(p))

      var entry: java.util.jar.JarEntry = null
      while ({entry = jar.getNextJarEntry(); entry != null}) {
        class_count += 1
        val name = entry.getName.replace("/", ".").replaceAll("\\.class$", "")
        println(f"class $class_count: $name")

        val c = getSootClass(name)
        for (m <- c.getMethods.asScala) {
          method_count += 1
          println(f"method $method_count: $m")
          if (m.isNative) { println("skipping body because native") }
          else if (m.isAbstract) { println("skipping body because abstract") }
          else {
            for (s <- getBody(m).getUnits.asScala) {
              stmt_count += 1
              println(f"stmt $stmt_count: $s")
            }
          }
        }
        //val bytes = new Array[Byte](entry.getSize.toInt)
        //jar.read(bytes, 0, entry.getSize.toInt)
        //typeLoader.add(bytes)
      }
    }

    println(f"END classes=$class_count methods=$method_count stmts=$stmt_count")
  }
}


// ./bin/jaam-tools loop3 --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/airplan_1.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-cli-1.3.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-codec-1.9.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-fileupload-1.3.1.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-io-2.2.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-lang3-3.4.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-logging-1.2.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/httpclient-4.5.1.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/httpcore-4.4.3.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/jline-2.8.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/log4j-1.2.17.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/mapdb-2.0-beta8.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/netty-all-4.0.34.Final.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/protobuf-java-3.0.0-beta-2.jar --classpath resources/rt.jar
