package org.ucombinator.jaam.tools.decompile

import java.io.{ ByteArrayOutputStream, FileOutputStream }
import java.io.OutputStreamWriter
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.IOException
import java.nio.file.{Files, Paths}

import com.strobel.assembler.InputTypeLoader
import com.strobel.assembler.metadata._
import com.strobel.core.VerifyArgument
import com.strobel.decompiler.languages.java.JavaFormattingOptions
import com.strobel.decompiler.languages.java.ast.{CompilationUnit, Keys}
import com.strobel.decompiler.languages.Languages

import org.ucombinator.jaam.tools
import org.ucombinator.jaam.tools.app
import org.ucombinator.jaam.tools.app.Origin

import com.strobel.decompiler._

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.ClassReader
import scala.collection.mutable

import org.ucombinator.jaam.serializer
import scala.collection.JavaConverters._

case class ClassData(name: String, origin: Origin, data: Array[Byte])

class HashMapTypeLoader extends ITypeLoader {
  val classes = mutable.HashMap[String, ClassData]()

  def tryLoadType(internalName: String, buffer: Buffer): Boolean = {
    classes.get(internalName) match {
      case None => false
      case Some(c) =>
        buffer.reset(c.data.length)
        buffer.putByteArray(c.data, 0, c.data.length)
        buffer.position(0)
        true
    }
  }

  def add(name: String, origin: Origin, data: Array[Byte]) {
    try {
      val cr = new ClassReader(data)
      val cn = new ClassNode()
      cr.accept(cn, 0)
      classes += cn.name -> ClassData(name, origin, data)
    } catch { case e: Exception =>
      println("Error while loading:\n")
      e.printStackTrace()
    }
  }
}

case class DecompiledClass(name: String,
                           origin: Origin,
                           compilationUnit: CompilationUnit)
  extends serializer.Packet

object Main {
  // Set the type loader in the global static fields in Procyon.
  // This deals with a SEGFAULT in Zip_Close when garbage collection is run.
  // The problem is that the default type loader is a ClasspathTypeLoader,
  // which contains a URLClassPath.  Aside from that not being the ITypeLoader
  // that we setup to read from the jaam file, that URLClassPath seems to still
  // have references into the file handles from when the data was serialized.
  // When the garbage collector is run, the finalizer on a JarFile object
  // is run which calls the native method ZipFile.close (i.e., Zip_Close at
  // the C level).
  def setTypeLoader(typeLoader: ITypeLoader): Unit = {
    val metadataSystem = new MetadataSystem(typeLoader)

    val instanceField = classOf[MetadataSystem].getDeclaredField("_instance")
    instanceField.setAccessible(true)
    instanceField.set(metadataSystem, metadataSystem)

    val resolverField = classOf[TypeDefinition].getDeclaredField("_resolver")
    resolverField.setAccessible(true)
    for (i <- List(
      BuiltinTypes.Boolean,
      BuiltinTypes.Byte,
      BuiltinTypes.Character,
      BuiltinTypes.Short,
      BuiltinTypes.Integer,
      BuiltinTypes.Long,
      BuiltinTypes.Float,
      BuiltinTypes.Double,
      BuiltinTypes.Void,
      BuiltinTypes.Object,
      BuiltinTypes.Class)) {
      resolverField.set(i, metadataSystem)
    }
  }

  def main(input: List[String], output: String, exclude: List[String], jvm: Boolean, lib: Boolean, app: Boolean, ignoreOverflow: Boolean) {
    val typeLoader = new HashMapTypeLoader()
    Main.setTypeLoader(typeLoader)

    for (file <- input) {
      for (a0 <- org.ucombinator.jaam.serializer.Serializer.readAll(file).asScala) {
        if (a0.isInstanceOf[org.ucombinator.jaam.tools.app.App]) {
          val a = a0.asInstanceOf[org.ucombinator.jaam.tools.app.App]
          for (pe <- a.classpath) {
            pe match {
              // TODO: use PathElement.classData()
              case tools.app.PathElement(path, root, origin, data) =>
                if (path.endsWith(".class")) {
                  println(f"Reading class file (from root $root) $path")
                  typeLoader.add(path, origin, data)
                } else if (!path.endsWith(".jar")) {
                  println(f"Skipping non-class, non-jar file (from root $root) $path")
                } else {
                  println(f"Reading jar file (from root $root) $path")
                  val jar = new java.util.jar.JarInputStream(
                    new java.io.ByteArrayInputStream(data))

                  var entry: java.util.jar.JarEntry = null
                  while ({entry = jar.getNextJarEntry; entry != null}) {
                    if (!entry.getName.endsWith(".class")) {
                      println(f"Skipping non-class file in jar file ${entry.getName}")
                    } else {
                      println(f"Reading class file in jar file ${entry.getName}")
                      typeLoader.add(path + "!" + entry.getName, origin, org.ucombinator.jaam.util.Misc.toByteArray(jar))
                    }
                  }

                  jar.close()
                }
            }
          }
        }
      }
    }

    val stream = new FileOutputStream(output)
    val po = new serializer.PacketOutput(stream)

    val total = typeLoader.classes.keys.size
    var index = 1
    val settings = DecompilerSettings.javaDefaults()

    val options = new DecompilationOptions()

    options.setSettings(settings)
    options.setFullDecompilation(true)

    if (settings.getJavaFormattingOptions == null) {
      settings.setJavaFormattingOptions(JavaFormattingOptions.createDefault())
    }

    for (name <- typeLoader.classes.keys) {
      if (exclude.contains(name)) {
        println(f"Excluding ($index of $total) $name")
      } else if (typeLoader.classes(name).origin != Origin.APP) {
        println(f"Skipping non-APP ($index of $total) $name")
      } else {
        try {
          println(f"Decompiling ($index of $total) $name")
          decompile(options, name) match {
            case None =>
              println(f"Skipping inner class ($index of $total) $name")
            case Some(cu) =>
              println(f"Finished decompiling ($index of $total) $name")
              po.write(DecompiledClass(name, typeLoader.classes(name).origin, cu))
          }
        } catch {
          case e: java.lang.OutOfMemoryError => println(f"Out of memory, skipping")
          case e: java.lang.StackOverflowError =>
            if (ignoreOverflow) { e.printStackTrace(); println(f"Stack overflow, skipping") }
            else { throw e }
        }
      }
      index += 1
    }

    po.close()
  }

  def decompile(options: DecompilationOptions,
                internalName: String): Option[CompilationUnit] = {
    val typ: TypeReference =
      if (internalName.length == 1) {
        val parser = new MetadataParser(IMetadataResolver.EMPTY)
        val reference = parser.parseTypeDescriptor(internalName)
        MetadataSystem.instance().resolve(reference)
      } else MetadataSystem.instance().lookupType(internalName)

    if (typ == null) {
      println(f"!!! ERROR: Failed to load type $internalName.")
      return null
    }

    val resolvedType = typ.resolve()

    if (resolvedType == null) {
      println(f"!!! ERROR: Failed resolve class $internalName with $typ.")
      return null
    }

    DeobfuscationUtilities.processType(resolvedType)

    if (resolvedType.isNested) { return None }
    else { return Some(Languages.java().decompileTypeToAst(resolvedType, options)) }
  }
}
