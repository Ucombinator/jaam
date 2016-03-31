package org.ucombinator.jaam

import scala.collection.JavaConversions._
import scala.math.BigInt

import org.ucombinator.jaam.Stmt.unitToStmt

import soot._
import soot.options._
import soot.jimple._
import soot.jimple.{Stmt => SootStmt}
import soot.tagkit.GenericAttribute
import soot.tagkit.SourceFileTag

import org.json4s._
import org.json4s.native._
import org.json4s.reflect.Reflector

// Helpers for working with Soot.
//
// Note that these methods relate to Soot only and do not include any
// classes or logic for the analyzer

object Stmt {
  val indexTag = "org.ucombinator.jaam.Stmt.indexTag"
  val serializer = FieldSerializer[Stmt]()

  import scala.language.implicitConversions
  implicit def unitToStmt(unit : Unit) : SootStmt = {
    assert(unit ne null, "unit is null")
    assert(unit.isInstanceOf[SootStmt], "unit not instance of Stmt. Unit is of class: " + unit.getClass)
    unit.asInstanceOf[SootStmt]
  }

  def getIndex(sootStmt : SootStmt, sootMethod : SootMethod) : Int = {
    if (sootStmt.hasTag(Stmt.indexTag)) {
      BigInt(sootStmt.getTag(Stmt.indexTag).getValue).intValue
    } else {
      // label everything in the sootMethod so the amortized work is linear
      for ((u, i) <- Soot.getBody(sootMethod).getUnits().toList.zipWithIndex) {
        u.addTag(new GenericAttribute(Stmt.indexTag, BigInt(i).toByteArray))
      }

      assert(sootStmt.hasTag(Stmt.indexTag), "SootStmt "+sootStmt+" not found in SootMethod " + sootMethod)
      BigInt(sootStmt.getTag(Stmt.indexTag).getValue).intValue
    }
  }

  def methodEntry(sootMethod : SootMethod) = Stmt(Soot.getBody(sootMethod).getUnits.getFirst, sootMethod)
}

case class Stmt(val sootStmt : SootStmt, val sootMethod : SootMethod) {
  val index = Stmt.getIndex(sootStmt, sootMethod)
  val line = sootStmt.getJavaSourceStartLineNumber
  val column = sootStmt.getJavaSourceStartColumnNumber
  val sourceFile = {
    val tag = sootMethod.getDeclaringClass.getTag("SourceFileTag")
    if (tag != null) {
      val sourceTag = tag.asInstanceOf[SourceFileTag]
      if (sourceTag.getAbsolutePath != null) { sourceTag.getAbsolutePath + "/" } else { "" } + sourceTag.getSourceFile
    } else { "<unknown>" }
  }
  def nextSyntactic : Stmt = this.copy(sootStmt = Soot.getBody(sootMethod).getUnits().getSuccOf(sootStmt))
  def nextSemantic : List[Stmt] =
      sootStmt match {
        case sootStmt : ReturnStmt => List()
        case sootStmt : ReturnVoidStmt => List ()
        case sootStmt : ThrowStmt => List ()
        case sootStmt : GotoStmt => List(this.copy(sootStmt = sootStmt.getTarget))
        case sootStmt : SwitchStmt => sootStmt.getTargets.toList.map(u => this.copy(sootStmt = u))
        case sootStmt : IfStmt => List(this.nextSyntactic, this.copy(sootStmt = sootStmt.getTarget))
        case sootStmt => List(this.nextSyntactic)
      }

  override def toString : String = sootMethod + ":" + index + ":" + sootStmt
}

object Soot {
  def toStringSerializer[T](implicit mf: Manifest[T]) = {
    val cls = mf.runtimeClass
    new CustomSerializer[T](implicit format => (
      { case s => ??? },
      { case x if cls.isAssignableFrom(x.getClass) => JString(x.toString) }
    ))
  }

  def mapSerializer[T,K,V](implicit mfT: Manifest[T], mfMap: Manifest[Map[K,V]]) = {
    val runtimeClass = mfT.runtimeClass
    val fields = runtimeClass.getDeclaredFields
    //assert(fields.length == 1)
    val f = fields(0)
    f.setAccessible(true)
    new CustomSerializer[Map[K,V]](implicit format => (
      { case s => ??? },
      { case x if runtimeClass.isAssignableFrom(x.getClass) =>
        val m = f.get(x).asInstanceOf[Map[K, V]]
        JArray(m.keys.toList.sortBy(_.toString).map({case k => JArray(List(Extraction.decompose(k), Extraction.decompose(m(k))))})) }
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

  private val typeHints = new TypeHints() {
    val hints = List() // this is a hack
    override def containsHint(clazz: Class[_]): Boolean = true
    override def + (hints: TypeHints): TypeHints = ???
    def hintFor(clazz: Class[_]) = clazz.getName
    def classFor(hint: String) = Reflector.scalaTypeOf(hint).map(_.erasure)
  }

  val formats =
    Serialization.formats(typeHints).withTypeHintFieldName("$type") +
    Stmt.serializer +
    Soot.methodSerializer +
    Soot.toStringSerializer[Type] +
    Soot.toStringSerializer[Unit] +
    Soot.toStringSerializer[SootField] +
    Soot.toStringSerializer[Local] +
    Soot.toStringSerializer[SootClass]

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

    // Compute dependent options
    soot.Main.v().autoSetOptions()

    // Run transformations and analyses according to the configured options.
    // Transformation could include jimple, shimple, and CFG generation
    PackManager.v().runPacks()
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
