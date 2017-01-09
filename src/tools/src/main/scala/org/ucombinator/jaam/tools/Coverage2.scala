package org.ucombinator.jaam.tools

import scala.collection.JavaConversions._

import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
//import soot.jimple.{Stmt => SootStmt, _}
import soot.options.Options
import soot.jimple.toolkits.callgraph._

import java.io.{File, FileInputStream}

import org.ucombinator.jaam.serializer._

object Coverage2 {

  def sootMethods(rtJar: String, sootClassPath: String, mainClassName: String) : Set[soot.SootMethod] = {
    Options.v().set_verbose(false)
    // Put class bodies in Jimple format
    Options.v().set_output_format(Options.output_format_jimple)
    // Process all packages and do not exclude packages like java.*
    Options.v().set_include_all(true)
    // we need to link instructions to source line for display
    Options.v().set_keep_line_number(true)
    // Called methods without jar files or source are considered phantom
    Options.v().set_allow_phantom_refs(true)
    // Use the class path from the command line
    Options.v().set_soot_classpath(rtJar + ":" + sootClassPath)
    // Use only the class path from the command line
    Options.v().set_prepend_classpath(false)
    // Take definitions only from class files
    Options.v().set_src_prec(Options.src_prec_only_class)
    // TODO: document
    Options.v().set_whole_program(true)

    // Compute dependent options
    soot.Main.v().autoSetOptions()

    val mainClass = Scene.v().loadClassAndSupport(mainClassName)
    //mainClass.setApplicationClass();
    Scene.v().setMainClass(mainClass)

    // Run transformations and analyses according to the configured options.
    // Transformation could include jimple, shimple, and CFG generation
    //Scene.v().loadBasicClasses()
    Scene.v().loadNecessaryClasses()

    CHATransformer.v().transform()

    val cg = Scene.v().getCallGraph()

    cg.listener.map(_.tgt).toSet ++ cg.listener.map(_.src).toSet
  }

  def freshenType(t: Type): Type = {
    t match {
      case t: VoidType => VoidType.v()

      // Sub-classes of PrimType
      case t: BooleanType => BooleanType.v()
      case t: ByteType => ByteType.v()
      case t: CharType => CharType.v()
      case t: DoubleType => DoubleType.v()
      case t: FloatType => FloatType.v()
      case t: IntType => IntType.v()
      case t: LongType => LongType.v()
      case t: ShortType => ShortType.v()

      // Sub-classes of RefLikeType
      case t: ArrayType => ArrayType.v(freshenType(t.baseType), t.numDimensions)
      case t: RefType => RefType.v(t.getClassName)
    }
  }

  def jaamMethods(jaamFile: String) : Map[soot.SootMethod, Map[Int, SootUnit]] = {
    var methods = Map[soot.SootMethod, Map[Int, SootUnit]]()
    val stream = new FileInputStream(jaamFile)
    val pi = new PacketInput(stream)
    var packet: Packet = null
    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      packet match {
        case s: State =>
          val m = s.stmt.method
          // Use forceResolve instead of loadClass b/c previous soot calls mean it has already completed resolving
          val c2 = Scene.v().forceResolve(m.getDeclaringClass.getName, SootClass.BODIES)
          val params = m.getParameterTypes.map(freshenType(_))
          val m2 = c2.getMethod(m.getName, params, freshenType(m.getReturnType))
          methods += (m2 -> (methods.getOrElse(m2, Map[Int, SootUnit]()) + (s.stmt.index -> s.stmt.stmt)))
        case _ => {} // Ignore anything else.
      }
    }
    pi.close()
    methods
  }

/*
  def getBody(m : SootMethod) = {
    if (m.isNative) { throw new Exception("Unimplemented native method: " + m) }
    if (!m.hasActiveBody()) {
      SootResolver.v().resolveClass(m.getDeclaringClass.getName, SootClass.BODIES)
      m.retrieveActiveBody()
    }
    m.getActiveBody
  }
 */

  def main(rtJar: String, jaamFile: String, mainClass: String, jarFileNames: Seq[String], additionalJarFileNames: Seq[String]) = {
    val sootClassPath = jarFileNames.mkString(":") + ":" + additionalJarFileNames.mkString(":")
    println("sootClassPath: " + sootClassPath)
    val sm = sootMethods(rtJar, sootClassPath, mainClass)
      //"com.cyberpointllc.stac.host.Main")
      //"com.stac.Main")
    val jm = jaamMethods(jaamFile)

    // check that all methods present
    for (m <- sm; if !jm.contains(m)) {
      if (!List("java.", "javax.", "sun.", "com.sun.").exists((m.getDeclaringClass.getPackageName + ".").startsWith(_))) {
        println("Method not covered: " + m)
      }
    }

    // check for extra methods
    for ((m, _) <- jm; if !sm.contains(m)) {
      println("Method covered, but should not be: " + m)
    }

    // check that methods have all states
    for ((m, ss) <- jm) {
      if (m.isAbstract) {
        println("Method is abstract: " + m)
      } else {
        if (!m.hasActiveBody) {
//          Scene.v().forceResolve(m.getDeclaringClass.getName, SootClass.BODIES)
//        }
//        SootResolver.v().resolveClass(m.getDeclaringClass.getName, SootClass.BODIES)
        m.retrieveActiveBody()
        }

        if (!m.hasActiveBody) {
          println("Method still has no active body: " + m)
        } else {
//      for (s <- m.getActiveBody().getUnits; if !ss.contains(s)) {
//        println("Statement not covered: " + m + ":" + s)
//      }
        //println(ss)
        var i = 0
        for (s <- m.getActiveBody.getUnits.iterator) {
          if (!ss.contains(i)) {
            println("Statement not covered: " + m + ":" + i + ":" + s)
          } else {
            //println("Statement covered: " + m + ":" + i + ":" + s)
          }
          i += 1
        }
        }
      }
    }
  }
}
