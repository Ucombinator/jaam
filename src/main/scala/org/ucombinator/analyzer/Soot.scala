package org.ucombinator.analyzer

import scala.collection.JavaConversions._
import scala.math.BigInt

import org.ucombinator.analyzer.Stmt.unitToStmt

import soot._
import soot.options._
import soot.jimple.{Stmt => SootStmt}
import soot.tagkit.GenericAttribute

import org.json4s._
import org.json4s.native._

// Helpers for working with Soot.
// Note that these methods relate to Soot only

object Stmt {
  val indexTag = "org.ucombinator.analyzer.Stmt.indexTag"
  val serializer = FieldSerializer[Stmt]()

  import scala.language.implicitConversions
  implicit def unitToStmt(unit : Unit) : SootStmt = {
    assert(unit.isInstanceOf[SootStmt])
    unit.asInstanceOf[SootStmt]
  }
}

case class Stmt(val sootStmt : SootStmt, val method : SootMethod) {
  val javaSourceLineNumber = sootStmt.getJavaSourceStartLineNumber
  val index : Int =
    if (sootStmt.hasTag(Stmt.indexTag)) {
      BigInt(sootStmt.getTag(Stmt.indexTag).getValue).intValue
    } else {
      val x = Soot.getBody(method).getUnits().toList.indexOf(sootStmt)
      if (x == -1) {
        println("Not found:"+sootStmt+" in " +Soot.getBody(method).getUnits())
      }
      sootStmt.addTag(new GenericAttribute(Stmt.indexTag, BigInt(x).toByteArray))
      x
    }
  def nextSyntactic() : Stmt = this.copy(sootStmt = Soot.getBody(method).getUnits().getSuccOf(sootStmt))
  override def toString : String = method + ":" + index + ":" + sootStmt
}

object Soot {
  def toStringSerializer[T](implicit mf: Manifest[T]) = {
    val cls = mf.runtimeClass
    new CustomSerializer[T](format => (
      { case s => ??? },
      { case x if cls.isAssignableFrom(x.getClass) => JString(x.toString) }
    ))
  }

  val methodSerializer = new CustomSerializer[SootMethod](implicit format => (
    { case s => ??? },
    { case m:SootMethod =>
      JObject(List(
        (format.typeHintFieldName, JString(format.typeHints.hintFor(m.getClass))),
        ("declaringClass", Extraction.decompose(m.getDeclaringClass)),
        ("name", Extraction.decompose(m.getName)),
        ("parameterTypes", Extraction.decompose(m.getParameterTypes)),
        ("returnType", Extraction.decompose(m.getReturnType)),
        ("modifiers", Extraction.decompose(m.getModifiers)),
        ("exceptions", Extraction.decompose(m.getExceptions)))) }
  ))

  def initialize(config : Config) {
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
    Options.v().set_soot_classpath(config.sootClassPath)
    // Use only the class path from the command line
    Options.v().set_prepend_classpath(false)
    // Take definitions only from class files
    Options.v().set_src_prec(Options.src_prec_only_class)
    config.cfg match {
      case None => {}
      case Some(dir) =>
        // Whole program mode is slow but needed when we are in CFG mode
        Options.v().set_whole_program(true)
        Options.v().set_process_dir(dir.split(System.getProperty("path.separator")).toList)
    }

    // Compute dependent options
    soot.Main.v().autoSetOptions();

    config.cfg match {
      case None => {}
      case Some(_) =>
        // Load classes according to the configured options
        Scene.v().loadNecessaryClasses()
        Scene.v().setMainClass(Scene.v().getSootClass(config.className))
    }

    // Run transformations and analyses according to the configured options.
    // Transformation could include jimple, shimple, and CFG generation
    PackManager.v().runPacks();
    Scene.v().loadBasicClasses()
  }

  def getBody(m : SootMethod) = {
    if (m.isNative) { throw new Exception("Unimplemented native method: " + m) }
    if (!m.hasActiveBody()) {
      SootResolver.v().resolveClass(m.getDeclaringClass.getName, SootClass.BODIES)
      m.retrieveActiveBody()
    }
    m.getActiveBody
  }

  def getSootClass(s : String) = Scene.v().loadClass(s, SootClass.SIGNATURES)

  def isPrimitive(t : Type) : Boolean = !t.isInstanceOf[RefLikeType]

  def isSubclass(sub : SootClass, sup : SootClass) : Boolean =
    Scene.v().getActiveHierarchy.isClassSubclassOfIncluding(sub, sup)

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
            println("Warning: checking if a non-array type is an array")
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
