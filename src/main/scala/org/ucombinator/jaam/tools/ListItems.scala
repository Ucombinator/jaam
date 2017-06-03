package org.ucombinator.jaam.tools.listitems

import java.io.File
import java.lang.reflect.Executable
import java.net.URLClassLoader
import java.util.jar.JarFile

import scala.collection.JavaConversions._
import scala.collection.mutable

import org.objectweb.asm._
import org.objectweb.asm.tree._

case class ListPrintOption(classes: Boolean, methods: Boolean)

object ListItems {
  private val classEnding = ".class"

  def getString(executable: Executable): String = {
    "<" + executable.getClass.getSimpleName.substring(0,6) + "> " + executable.toString
  }

  private val accessTable = Map(
    (Opcodes.ACC_ABSTRACT, "abstract"),
    (Opcodes.ACC_ANNOTATION, "annotation"),
    (Opcodes.ACC_DEPRECATED, "deprecated"),
    (Opcodes.ACC_ENUM, "enum"),
    (Opcodes.ACC_FINAL, "final"),
    (Opcodes.ACC_INTERFACE, "interface"),
    (Opcodes.ACC_MANDATED, "mandated"),
    (Opcodes.ACC_NATIVE, "native"),
    (Opcodes.ACC_PRIVATE, "private"),
    (Opcodes.ACC_PROTECTED, "protected"),
    (Opcodes.ACC_PUBLIC, "public"),
    (Opcodes.ACC_STATIC, "static"),
    (Opcodes.ACC_STRICT, "strict"),
    (Opcodes.ACC_SUPER, "super"),
    (Opcodes.ACC_SYNTHETIC, "synthetic"),
    (Opcodes.ACC_TRANSIENT, "transient"),
    (Opcodes.ACC_VOLATILE, "volatile"))

  private val methodAccessTable = accessTable +
    (Opcodes.ACC_VARARGS -> "varargs") +
    (Opcodes.ACC_BRIDGE -> "bridge") +
    (Opcodes.ACC_SYNCHRONIZED -> "synchronized")

  def fromAccess(access: Int, method: Boolean): String = {
    val table = if (method) { methodAccessTable } else { accessTable }
    var results = List[String]()
    for (i <- 1 to 32) {
      val key = 1 << i
      if ((access & key) != 0) {
        results ++= List(table(key))
      }
    }
    return results.mkString(" ")
  }

  def className(name: String) = name.replace('/', '.')

  // signature is usually null, but if non-null it contains information about type parameters to generics
  def sig(signature: String, desc: String) = {
    val s = if (signature != null) { signature } else { desc }
    descToString(s)
  }

  // TODO: parse according to https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3
  def descToString(d: String) =
    if (d == null) { "<null>" } else { Type.getType(d) }

  def main(jarFileName: String, printOption: ListPrintOption): Unit = {
    val jarFileURL = new File(jarFileName).toURI.toURL
    val classLoader = new URLClassLoader(Array(jarFileURL))

    val verify = false
    val jarFile = new JarFile(jarFileName, verify)

    // Iterate through the JarEntries to find things.
    for (jarEntry <- jarFile.entries().filter(_.getName.endsWith(classEnding))) {
      val fileName = jarEntry.getName
      val binaryName = fileName.dropRight(classEnding.length).replaceAll(File.separator, ".")
      if (printOption.classes) {
        println("<Class> " + binaryName)
      }
      // Load the class and print the methods and constructors inside of it.
      if (printOption.methods) {
        try {
          val cr = new ClassReader(jarFile.getInputStream(jarEntry))
          val cn = new ClassNode()
          cr.accept(cn, 0)
          println(f"class ${className(cn.name)} extends ${className(cn.superName)} implements ${cn.interfaces.mkString(", ")} flags(${fromAccess(cn.access, false)}) type: ${descToString(cn.signature)}")
          for (field <- cn.fields.asInstanceOf[java.util.List[FieldNode]]) { // TODO: remove asInstanceOf
            println(f"  field ${className(cn.name)}.${field.name} = ${field.value}; flags(${fromAccess(field.access, false)}) type: ${sig(field.signature, field.desc)}")
          }
          for (method <- cn.methods.asInstanceOf[java.util.List[MethodNode]]) {
            println(f"  method ${className(cn.name)}.${method.name}() flags(${fromAccess(method.access, true)}) type: ${sig(method.signature, method.desc)} exceptions(${method.exceptions.mkString(" ")})")
/*
Signature vs descriptor:

sig:
  (Ljava/lang/String;Ljava/lang/String;Lcom/cyberpointllc/stac/gabfeed/model/GabUser;Ljava/util/List<Lcom/cyberpointllc/stac/gabfeed/handler/Link;>;)Lcom/cyberpointllc/stac/webserver/handler/HttpHandlerResponse;)
desc:
  (Ljava/lang/String;Ljava/lang/String;Lcom/cyberpointllc/stac/gabfeed/model/GabUser;Ljava/util/List;)Lcom/cyberpointllc/stac/webserver/handler/HttpHandlerResponse;)
 */
          }
        } catch {
          case e: Throwable => e.printStackTrace()
        }
      }
    }
  }
}

/*
Can't load class b/c this happens:

$jaam-tools list enagement_2/gabfeed_1/challenge_program/lib/gabfeed_1.jar 

<Class> com.cyberpointllc.stac.host.Main
java.lang.NoClassDefFoundError: org/apache/commons/cli/ParseException
	at java.lang.Class.getDeclaredConstructors0(Native Method)
	at java.lang.Class.privateGetDeclaredConstructors(Class.java:2671)
	at java.lang.Class.getDeclaredConstructors(Class.java:2020)
	at org.ucombinator.jaam.tools.ListItems$$anonfun$main$2.apply(ListItems.scala:39)
	at org.ucombinator.jaam.tools.ListItems$$anonfun$main$2.apply(ListItems.scala:28)
	at scala.collection.Iterator$class.foreach(Iterator.scala:893)
	at scala.collection.AbstractIterator.foreach(Iterator.scala:1336)
	at org.ucombinator.jaam.tools.ListItems$.main(ListItems.scala:28)
	at org.ucombinator.jaam.tools.Conf$ListItems.run(Main.scala:140)
	at org.ucombinator.jaam.tools.Main$.main(Main.scala:185)
	at org.ucombinator.jaam.tools.Main.main(Main.scala)
Caused by: java.lang.ClassNotFoundException: org.apache.commons.cli.ParseException
	at java.net.URLClassLoader.findClass(URLClassLoader.java:381)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
	... 11 more
 */
