package org.ucombinator.jaam.tools

import java.io.{File, FileInputStream}
import java.lang.reflect.{Constructor, Method}
import java.net.{URL, URLClassLoader}
import java.util.jar.JarFile

import org.ucombinator.jaam.serializer._

import scala.collection.JavaConversions._
import scala.collection.mutable

//sealed trait MethodSet
//case class EmptyMethodSet() extends MethodSet
//case class SomeMethodsInitializedByJAR(methods: Set[Method]) extends MethodSet
//case class SomeMethodsInitializedByJAAM(methods: Set[Method]) extends MethodSet

sealed trait OriginalMethod
case class SootMethod(method: soot.SootMethod) extends OriginalMethod
case class ReflectMethod(method: Method) extends OriginalMethod
case class ReflectConstructor(constructor: Constructor[_]) extends OriginalMethod
case class ClassStaticInitializer(callingClass: Class[_]) extends OriginalMethod

class ComparableMethod(representation: String, val originalMethod: OriginalMethod, val jarFile: String, val classFile: String) {
  def this(reflectMethod: Method, jarFile: String, classFile: String) = this(reflectMethod.toGenericString, ReflectMethod(reflectMethod), jarFile, classFile)
  def this(reflectConstructor: Constructor[_], jarFile: String, classFile: String) = this(reflectConstructor.toGenericString, ReflectConstructor(reflectConstructor), jarFile, classFile)
  def this(sootStmt: Stmt, jarFile: String = "", classFile: String = "") = this(sootStmt.method.toString, SootMethod(sootStmt.method), jarFile, classFile)
  def this(loadedClass: Class[_]) = this(loadedClass.toString, ClassStaticInitializer(loadedClass), "", "")

  override def equals(other: Any): Boolean = {
    other match {
      case other: ComparableMethod =>
        this.getRepresentation == other.getRepresentation
      case _ => false
    }
  }

  override def hashCode: Int = {
    this.getRepresentation.hashCode
  }

  def getRepresentation: String = {
//    print("getting representation of ")
    originalMethod match {
      //      case SootMethod(s) => s.getName + " returns: " + s.getReturnType + " takes: " + s.getParameterTypes
      //      case SootMethod(s) => s.getDeclaration
      //      case ReflectMethod(r) => r.toGenericString + r.getClass.getName
      case SootMethod(s)    =>
//        println("SootMethod")
        s.getDeclaringClass.toString.replace("$", ".") + " " + s.getReturnType.toString + " " + s.getName + "(" + s.getParameterTypes.map(t => t.toString).mkString(", ") + ")"
      //      case ReflectMethod(r) => r.getReturnType.getName + " " + r.getDeclaringClass.getName + "." + r.getName + "(" + r.getParameterTypes.map(t => t.getName).mkString(", ")  + ")"
      //      case ReflectMethod(r) => jarFile + " " + " " + classFile + " " + r.getReturnType.getName + " " + r.getDeclaringClass.getName + " " + r.getName + "(" + r.getParameterTypes.map(t => t.getName).mkString(", ")  + ")"
      case ReflectMethod(r) =>
//        println("ReflectMethod")
//        r.getDeclaringClass.getCanonicalName.replace("$", ".") + " " + r.getReturnType.getCanonicalName.replace("$", ".") + " " + r.getName + "(" + r.getParameterTypes.map(t => t.getCanonicalName.replace("$", ".")).mkString(", ")  + ")"
        r.getDeclaringClass.getName.replace("$", ".") + " " + r.getReturnType.getName.replace("$", ".") + " " + r.getName + "(" + r.getParameterTypes.map(t => t.getName.replace("$", ".")).mkString(", ")  + ")"
//        "REFLECTMETHOD"
      case ReflectConstructor(c) =>
//        println("ReflectConstructor")
//        println("constructor: " + c)
//        println("constructor.getDeclaringClass: " + c.getDeclaringClass)
//        println("constructor.getDeclaringClass.getCanonicalName: " + c.getDeclaringClass.getCanonicalName)
//        println("constructor.getDeclaringClass.getName: " + c.getDeclaringClass.getName)
//        println("constructor.getDeclaringClass.getSimpleName: " + c.getDeclaringClass.getSimpleName)
//        println("constructor.getDeclaringClass.getCanonicalName.replace(blah): " + c.getDeclaringClass.getCanonicalName.replace("$", "."))
//        println("constructor.parameters.getCanonicalName(s): " + c.getParameterTypes.map(t => t.getCanonicalName.replace("$", ".")).mkString(", "))
        c.getDeclaringClass.getName.replace("$", ".") + " " + "void" + " " + "<init>" + "(" + c.getParameterTypes.map(t => t.getName.replace("$", ".")).mkString(", ") + ")"
      case ClassStaticInitializer(i) =>
        i.getName.replace("$", ".") + " " + "void <clinit>()"
    }
  }

  override def toString: String = {
    originalMethod match {
      //      case SootMethod(s) => s.getName + " returns: " + s.getReturnType + " takes: " + s.getParameterTypes
      //      case SootMethod(s) => s.getDeclaration
      //      case ReflectMethod(r) => r.toGenericString + r.getClass.getName
      case SootMethod(s)    => s.getReturnType         + " " + s.getDeclaringClass.getName + "." + s.getName + "(" + s.getParameterTypes.map(t => t.toString).mkString(", ") + ")"
      //      case ReflectMethod(r) => r.getReturnType.getName + " " + r.getDeclaringClass.getName + "." + r.getName + "(" + r.getParameterTypes.map(t => t.getName).mkString(", ")  + ")"
      //      case ReflectMethod(r) => jarFile + " " + " " + classFile + " " + r.getReturnType.getName + " " + r.getDeclaringClass.getName + " " + r.getName + "(" + r.getParameterTypes.map(t => t.getName).mkString(", ")  + ")"
      case ReflectMethod(r) => r.getReturnType.getCanonicalName + " " + r.getName + "(" + r.getParameterTypes.map(t => t.getCanonicalName).mkString(", ")  + ")"
      case ReflectConstructor(c) => c.getName + "(" + c.getParameterTypes.map(t => t.getCanonicalName).mkString(", ") + ")"
      case ClassStaticInitializer(i) => this.getRepresentation
    }
  }
}

object Coverage {
  // To avoid retyping the Set[Stmt] type all over, here's a type alias.
  // But these need a definition for when `apply` is called, so it's here too.
  type StmtSet = mutable.Set[Stmt]
  def StmtSet(stmts: Stmt*) = mutable.Set[Stmt](stmts: _*)

  private val methodMap = mutable.Map[ComparableMethod, StmtSet]()
  private val classEnding = ".class"

  def findCoverage(jaamFile: String, jarFileNames: Seq[String], additionalJarFileNames: Seq[String]) = {
    initializeHashFromJarFiles(jarFileNames, additionalJarFileNames)
    loadStmtsFromJaamFile(jaamFile)
    compareMethods()
    // Iterate over statements in jaam file.
    //   For each statement, add statement to method in hash.
    // Print results.
  }

  def initializeHashFromJarFiles(jarFileNames: Seq[String], additionalJarFileNames: Seq[String]) = {
    // Iterate over the jarfile names.
    val jarFileURLs = jarFileNames.map(name => new File(name).toURI.toURL).toArray
    val additionalJarFileURLs = additionalJarFileNames.map(name => new File(name).toURI.toURL).toArray
    val classLoader = new URLClassLoader(additionalJarFileURLs ++ jarFileURLs)
    for (jarFileName <- jarFileNames) {
//      println("jarFileName: " + jarFileName)
      // Initialize class loader.  Unfortunately, this only works if you know
      // the names of the classes to load... which is unhelpful to us.
      val jarFile = new JarFile(jarFileName, false)
      for (jarEntry <- jarFile.entries().filter(_.getName.endsWith(classEnding))) {
        // The class loader requires the names to be in "binary name" format,
        // e.g. "com.example.MyGreatClass".
        val fileName = jarEntry.getName
        val binaryName = fileName.dropRight(classEnding.length).replaceAll(File.separator, ".")
//        println("  classname: " + fileName)
        try {
          val loadedClass = classLoader.loadClass(binaryName)
//          println("    declaredMethods: " + loadedClass.getDeclaredMethods.map(m => m.toGenericString).toList)
//          println("    declaredConstructors: " + loadedClass.getDeclaredConstructors.map(m => m.toGenericString).toList)
//          loadedClass.getDeclaredMethods.foreach(m => {val cm = new ComparableMethod(m, jarFileName, fileName); methodMap(cm) = StmtSet(); println("      " + cm.getRepresentation)})
//          loadedClass.getDeclaredConstructors.foreach(m => {val cm = new ComparableMethod(m, jarFileName, fileName); methodMap(cm) = StmtSet(); println("      " + cm.getRepresentation)})
          methodMap(new ComparableMethod(loadedClass)) = StmtSet()
          loadedClass.getDeclaredMethods.foreach(m => methodMap(new ComparableMethod(m, jarFileName, fileName)) = StmtSet())
          loadedClass.getDeclaredConstructors.foreach(m => methodMap(new ComparableMethod(m, jarFileName, fileName)) = StmtSet())
        } catch {
          //TODO: Decide whether to catch these exceptions explicitly.
          // Skip NoClassDefFoundErrors because they do not concern us.
//          case e: NoClassDefFoundError => ()
          // Loading of classes does not always seem to succeed. Unsure why.
          case e: Throwable => e.printStackTrace()
        }
      }
    }
//    for ((key, value) <- methodMap) {
//      println(key.getRepresentation)
//    }
//    println("*******")
//    println("methodMap.size: " + methodMap.size)
  }

  def loadStmtsFromJaamFile(jaamFile: String) = {
    val stream = new FileInputStream(jaamFile)
    val pi = new PacketInput(stream)
    var packet: Packet = null
    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      packet match {
        case s: State =>
          val comparable = new ComparableMethod(s.stmt)
//          println(comparable.getRepresentation)
          methodMap get comparable match {
            case Some(_) => methodMap(comparable) += s.stmt
            case None =>
//              println(comparable.getRepresentation)
//              println(comparable.toString)
          }
        case _ => () // Ignore anything else.
      }
    }
    pi.close()
  }

  def compareMethods() = {
    var count = 0
    for ((method, stmtSet) <- methodMap) {
      if (stmtSet.nonEmpty) {
        method.originalMethod match {
          case SootMethod(s) =>
//            println("JAAM-initialized method:")
//            println("    " + method.getRepresentation)
          case ReflectMethod(r) =>
//            println("JAR-initialized method:")
//            println("    " + method.getRepresentation)
          case _ => ()
        }
//        println(method.getRepresentation)
      } else {
        count += 1
        method.originalMethod match {
          case ReflectMethod(r) =>
//            println("Empty JAR-initialized method:")
//            println("    " + method.getRepresentation)
          case SootMethod(s) =>
//            println("Empty JAAM-initialized method:")
//            println("    " + method.getRepresentation)
          case _ => ()
        }
      }
//      println(method.getRepresentation)
    }
    println("Missing " + count + " of " + methodMap.size)
  }
}

/*
Notes from Michael:

h: method -> Set<Stmt>

- tool coverage <jaam> <jar>

---

classes = ...jar...
methods = ...classes...
for m <- methods
  h[m] = (empty set)
for stmt <- jaam
  h[stmt.method] += stmt
for (m, ss) <- h
  if ss not (empty set)
    print method
  else
    (count) ++
print "(count) of h.size"
 */
