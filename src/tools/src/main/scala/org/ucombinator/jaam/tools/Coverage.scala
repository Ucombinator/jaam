package org.ucombinator.jaam.tools

import java.io.{File, FileInputStream}
import java.lang.reflect.Method
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

class ComparableMethod(val representation: String, original: OriginalMethod) {
  def this(reflectMethod: Method) = this(reflectMethod.toGenericString, ReflectMethod(reflectMethod))
  def this(sootStmt: Stmt) = this(sootStmt.method.toString, SootMethod(sootStmt.method))

  override def equals(other: Any): Boolean = {
    other match {
      case other: ComparableMethod =>
        this.representation == other.representation
      case _ => false
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
      println(jarFileName)
      // Initialize class loader.  Unfortunately, this only works if you know
      // the names of the classes to load... which is unhelpful to us.
      val jarFile = new JarFile(jarFileName, false)
      for (jarEntry <- jarFile.entries().filter(_.getName.endsWith(classEnding))) {
        // The class loader requires the names to be in "binary name" format,
        // e.g. "com.example.MyGreatClass".
        val fileName = jarEntry.getName
        val binaryName = fileName.dropRight(classEnding.length).replaceAll(File.separator, ".")
        println("    " + binaryName)
        try {
          val loadedClass = classLoader.loadClass(binaryName)
          loadedClass.getMethods.foreach(m => methodMap(new ComparableMethod(m)) = StmtSet())
        } catch {
          //TODO: Decide whether to catch these exceptions explicitly.
          // Skip NoClassDefFoundErrors because they do not concern us.
//          case e: NoClassDefFoundError => ()
          // Loading of classes does not always seem to succeed. Unsure why.
          case e: Throwable => println("        " + e)
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
            case None => methodMap(comparable) = StmtSet()
          }
        case _ => () // Ignore anything else.
      }
    }
    pi.close()
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
