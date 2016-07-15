package org.ucombinator.jaam.tools

import java.io.{File, FileInputStream}
import java.lang.reflect.{Constructor, Method}
import java.net.URLClassLoader
import java.util.jar.JarFile

import org.ucombinator.jaam.serializer._

import scala.collection.JavaConversions._
import scala.collection.mutable

sealed trait OriginalRepresentation
case class SootMethod(method: soot.SootMethod)              extends OriginalRepresentation
case class ReflectMethod(method: Method)                    extends OriginalRepresentation
case class ReflectConstructor(constructor: Constructor[_])  extends OriginalRepresentation
case class ClassStaticInitializer(callingClass: Class[_])   extends OriginalRepresentation

class ComparableMethod(val originalMethod: OriginalRepresentation) {
  def this(reflectMethod: Method)               = this(ReflectMethod(reflectMethod))
  def this(reflectConstructor: Constructor[_])  = this(ReflectConstructor(reflectConstructor))
  def this(sootStmt: Stmt)                      = this(SootMethod(sootStmt.method))
  def this(loadedClass: Class[_])               = this(ClassStaticInitializer(loadedClass))

  override def equals(other: Any): Boolean = {
    other match {
      case other: ComparableMethod =>
        this.toString == other.toString
      case _ => false
    }
  }

  override def hashCode: Int = {
    this.toString.hashCode
  }

  override def toString: String = {
    originalMethod match {
      case SootMethod(s) =>
        s.getDeclaringClass.toString.replace("$", ".") +
          " " + s.getReturnType.toString +
          " " + s.getName +
          "(" + s.getParameterTypes.map(t => t.toString).mkString(", ") + ")"
      case ReflectMethod(r) =>
        r.getDeclaringClass.getName.replace("$", ".") +
          " " + r.getReturnType.getName.replace("$", ".") +
          " " + r.getName +
          "(" + r.getParameterTypes.map(t => t.getName.replace("$", ".")).mkString(", ")  + ")"
      case ReflectConstructor(c) =>
        c.getDeclaringClass.getName.replace("$", ".") +
          " " + "void <init>" +
          "(" + c.getParameterTypes.map(t => t.getName.replace("$", ".")).mkString(", ") + ")"
      case ClassStaticInitializer(i) =>
        i.getName.replace("$", ".") +
          " " + "void <clinit>()"
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
  }

  def initializeHashFromJarFiles(jarFileNames: Seq[String], additionalJarFileNames: Seq[String]) = {
    // Iterate over the jarfile names.
    val jarFileURLs = jarFileNames.map(name => new File(name).toURI.toURL).toArray
    val additionalJarFileURLs = additionalJarFileNames.map(name => new File(name).toURI.toURL).toArray
    val classLoader = new URLClassLoader(additionalJarFileURLs ++ jarFileURLs)
    for (jarFileName <- jarFileNames) {
      // Initialize class loader.  Unfortunately, this only works if you know
      // the names of the classes to load... which is unhelpful to us.
      val jarFile = new JarFile(jarFileName, false)
      for (jarEntry <- jarFile.entries().filter(_.getName.endsWith(classEnding))) {
        // The class loader requires the names to be in "binary name" format,
        // e.g. "com.example.MyGreatClass".
        val fileName = jarEntry.getName
        val binaryName = fileName.dropRight(classEnding.length).replaceAll(File.separator, ".")
        try {
          val loadedClass = classLoader.loadClass(binaryName)
          methodMap(new ComparableMethod(loadedClass)) = StmtSet()
          loadedClass.getDeclaredMethods.foreach(m => methodMap(new ComparableMethod(m)) = StmtSet())
          loadedClass.getDeclaredConstructors.foreach(m => methodMap(new ComparableMethod(m)) = StmtSet())
        } catch {
          //TODO: Decide whether to catch these exceptions explicitly.
          case e: Throwable => e.printStackTrace()
        }
      }
    }
  }

  def loadStmtsFromJaamFile(jaamFile: String) = {
    val stream = new FileInputStream(jaamFile)
    val pi = new PacketInput(stream)
    var packet: Packet = null
    while ({packet = pi.read(); !packet.isInstanceOf[EOF]}) {
      packet match {
        case s: State =>
          val comparable = new ComparableMethod(s.stmt)
          methodMap get comparable match {
            case Some(_) => methodMap(comparable) += s.stmt
            case None => () // No corresponding value in map from JAR reading.
              //TODO: Do something with this?
          }
        case _ => () // Ignore anything else.
      }
    }
    pi.close()
  }

  def compareMethods() = {
    var count = 0
    for ((method, stmtSet) <- methodMap) {
      if (stmtSet.isEmpty) {
        println(method)
        count += 1
      }
    }
    println("Missing " + count + " of " + methodMap.size)
  }
}
