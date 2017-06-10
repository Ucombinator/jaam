package org.ucombinator.jaam.util

import java.io._

import scala.collection.JavaConverters._

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.ucombinator.jaam.serializer
import org.ucombinator.jaam.tools.app.{App, PathElement, Origin}
import soot.{Unit => SootUnit, _}
import soot.options.Options
import org.ucombinator.jaam.main.Log

// Helpers for working with Soot.
//
// Note that these methods relate to Soot only and do not include any
// classes or logic for the analyzer


object Soot {
  def useJaamClassProvider(): Unit = {
    SourceLocator.v.setClassProviders(List[ClassProvider](new JaamClassProvider).asJava)
  }

  case class ClassData(source: String, origin: Origin, data: Array[Byte])
  var loadedClasses = Map[String, ClassData]()

  private def load(p: PathElement) {
    //println(f"p ${p.path} and ${p.root}")
    for (d <- p.classData()) {
      //println(f"d ${d.length}")
      val cr = new ClassReader(new ByteArrayInputStream(d))
      val cn = new ClassNode()
      cr.accept(cn, 0)
      //println(f"cn.name: ${cn.name}")
      loadedClasses += cn.name.replace('/', '.') -> ClassData("TODO:JaamClassProvider", p.origin, d)
    }
  }

  def addClasses(app: App): Unit = { app.classpath.map(load) }

  // TODO: optional flags to load only some parts?
  def addJaamClasses(file: String): Unit = {
    val stream = new FileInputStream(file)
    val pi = new serializer.PacketInput(stream)

    var packet: serializer.Packet = null
    while ({packet = pi.read(); !packet.isInstanceOf[serializer.EOF]}) {
      // TODO: for (packet <- pi) {
      packet match { case packet: App => addClasses(packet) }
    }
  }

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

  def initialize(rtJar: String, classpath: String, customizations: => Unit = {}) {
    // We choose to not use Soot in verbose mode.
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
    Options.v().set_soot_classpath(rtJar + ":" + classpath)
    // Use only the class path from the command line
    Options.v().set_prepend_classpath(false)
    // Take definitions only from class files
    Options.v().set_src_prec(Options.src_prec_only_class)

    // TODO: when should we have this?
    //Options.v.set_whole_program(true)

    customizations // Let the caller specify modifications to settings

    // Compute dependent options
    soot.Main.v().autoSetOptions()

    // Run transformations and analyses according to the configured options.
    // Transformation could include jimple, shimple, and CFG generation
    Scene.v().loadBasicClasses()
    PackManager.v().runPacks()
  }

  def isClass(s: String) = SourceLocator.v().getClassSource(s) != null

  def getSootType(t : String): Type = t match {
    case "int" => soot.IntType.v()
    case "bool" => soot.BooleanType.v()
    case "double" => soot.DoubleType.v()
    case "float" => soot.FloatType.v()
    case "long" => soot.LongType.v()
    case "byte" => soot.ByteType.v()
    case "short" => soot.ShortType.v()
    case "char" => soot.CharType.v()
    case _ => soot.RefType.v(t)
  }

  def isPrimitive(t : Type) : Boolean = !t.isInstanceOf[RefLikeType]

  def canStoreType(child: Type, parent: Type): Boolean = {
    Scene.v().getOrMakeFastHierarchy().canStoreType(child, parent)
  }

  def canStoreClass(child: SootClass, parent: SootClass): Boolean = {
    Scene.v().getOrMakeFastHierarchy().canStoreType(child.getType, parent.getType)
  }

  object classes {
    lazy val Object = getSootClass("java.lang.Object")
    lazy val Class = getSootClass("java.lang.Class")
    lazy val String = getSootClass("java.lang.String")
    lazy val Clonable = getSootClass("java.lang.Clonable")
    lazy val ClassCastException = getSootClass("java.lang.ClassCastException")
    lazy val ArithmeticException = getSootClass("java.lang.ArithmeticException")
    lazy val Serializable = getSootClass("java.io.Serializable")
  }

  // is a of type b?
  def isSubType(a : Type, b : Type) : Boolean = {
    if (a equals b) {
      return true
    } else {
      if (isPrimitive(a) || isPrimitive(b)) {
        return false
      } else {
        (a, b) match {
          case (at : ArrayType, bt : ArrayType) => {
            (at.numDimensions == bt.numDimensions) &&
              isSubType(at.baseType, bt.baseType)
          }
          case (ot : Type, at : ArrayType) => {
            ot.equals(classes.Object.getType) ||
              ot.equals(classes.Clonable.getType) ||
              ot.equals(classes.Serializable.getType)
          }
          case (at : ArrayType, ot : Type) => {
            Log.warn("Checking if a non-array type "+ot+" is an array")
            false // maybe
          }
          case _ => {
            val lub: Type = a.merge(b, Scene.v)
            val result = (lub != null) && !lub.equals(a)
            return result
          }
        }
      }
    }
  }
}
