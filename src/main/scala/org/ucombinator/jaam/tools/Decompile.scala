package org.ucombinator.jaam.tools.decompile

import java.io.{ ByteArrayOutputStream, FileOutputStream }
import java.io.OutputStreamWriter
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.IOException
import java.nio.file.{Files, Paths}

import com.strobel.assembler.InputTypeLoader
//import com.strobel.assembler.metaITypeLoader
import com.strobel.assembler.metadata._
import com.strobel.core.VerifyArgument
import com.strobel.decompiler.languages.java.JavaFormattingOptions
import com.strobel.decompiler.languages.java.ast.{CompilationUnit, Keys}
import com.strobel.decompiler.languages.Languages

import org.ucombinator.jaam.tools
import org.ucombinator.jaam.tools.app
import org.ucombinator.jaam.tools.app.FileRole

import com.strobel.decompiler._

import scala.collection.mutable

import org.ucombinator.jaam.serializer
import scala.collection.JavaConverters._

class HashMapTypeLoader extends ITypeLoader {
  val classes = mutable.HashMap[String, Array[Byte]]()

  def tryLoadType(internalName: String, buffer: Buffer): Boolean = {
    classes.get(internalName) match {
      case None => return false
      case Some(c) =>
        buffer.reset(c.length)
        buffer.putByteArray(c, 0, c.length)
        buffer.position(0)
        return true
    }
  }

  def add(data: Array[Byte]) {
    try {
      val typeDefinition = ClassFileReader.readClass(IMetadataResolver.EMPTY, new Buffer(data))
      classes += typeDefinition.getInternalName -> data
    } catch { case e: Exception =>
        println("Error while loading:\n")
        e.printStackTrace()
    }
  }
}

case class DecompiledClass(compilationUnit: CompilationUnit) extends serializer.Packet

object Main {
  def main(input: List[String], output: String, exclude: List[String], jvm: Boolean, lib: Boolean, app: Boolean) {
    val typeLoader = new HashMapTypeLoader()

    def loadData(p: tools.app.PathElement) = p match {
      // TODO: use PathElement.classData()
      case tools.app.PathElement(path, root, role, data) =>
        if (path.endsWith(".class")) {
          println(f"Reading class file (from root $root) $path")
          typeLoader.add(data)
        } else if (!path.endsWith(".jar")) {
          println(f"Skipping non-class, non-jar file (from root $root) $path")
        } else {
          println(f"Reading jar file (from root $root) $path")
          val jar = new java.util.jar.JarInputStream(
            new java.io.ByteArrayInputStream(data))

          var entry: java.util.jar.JarEntry = null
          while ({entry = jar.getNextJarEntry(); entry != null}) {
            if (!entry.getName.endsWith(".class")) {
              println(f"Skipping non-class file in jar file ${entry.getName}")
            } else {
              println(f"Reading class file in jar file ${entry.getName}")
              val bytes = new Array[Byte](entry.getSize.toInt)
              var pos = 0
              while (pos != entry.getSize) {
                val length = jar.read(bytes, pos, entry.getSize.toInt - pos)
                if (length == -1) { throw new Exception(f"Reached end of stream at position $pos, which is before entry size ${entry.getSize}") }
                pos += length
              }
              typeLoader.add(bytes)
            }
          }
        }
    }

    //for ((entry, bytes) <- org.ucombinator.jaam.util.Jar.entries(new java.io.FileInputStream(input(0)))) {
    //  println(f"name ${entry.getName()} size ${entry.getSize.toInt} comsize ${entry.getCompressedSize} bytes ${bytes.length}")
    //}

/*
    var entry: java.util.jar.JarEntry = null
    while ({entry = jar.getNextJarEntry(); entry != null}) {
      println(f"name ${entry.getName()} size ${entry.getSize.toInt} comsize ${entry.getCompressedSize}")
      if (entry.getSize == -1) {
        val bytes = org.ucombinator.jaam.util.Misc.toByteArray(jar)
        println(f"bytes: ${bytes.length}")
      } else {
        val bytes = new Array[Byte](entry.getSize.toInt)
        jar.read(bytes, 0, entry.getSize.toInt)
        typeLoader.add(bytes)
      }
    }
 */

    for (file <- input) {
      for (a0 <- org.ucombinator.jaam.serializer.Serializer.readAll(file)) {
        if (a0.isInstanceOf[org.ucombinator.jaam.tools.app.App]) {
          val a = a0.asInstanceOf[org.ucombinator.jaam.tools.app.App]
          for (pe <- a.classpath) {
            pe.role match {
              case FileRole.APP => if (app) loadData(pe)
              case FileRole.LIB => if (lib) loadData(pe)
              case FileRole.JVM => if (jvm) loadData(pe)
            }
          }
        }
      }
    }

/*
    for (file <- input) {
      val stream = new FileInputStream(file)
      val pi = new serializer.PacketInput(stream)

      var packet: serializer.Packet = null
      while ({packet = pi.read(); !packet.isInstanceOf[serializer.EOF]}) {
      //for (packet <- pi) {
        packet match {
          case packet: tools.app.App =>
            if (rt) { packet.classpath.java.map(loadData(_)) }
            if (lib) { packet.classpath.lib.map(loadData(_)) }
            if (app) { packet.classpath.app.map(loadData(_)) }
        }
      }
    }
 */

    val stream = new FileOutputStream(output)
    val po = new serializer.PacketOutput(stream)

    val total = typeLoader.classes.keys.size
    var index = 1
    val settings = DecompilerSettings.javaDefaults()
    val metadataSystem = new MetadataSystem(typeLoader)

    val options = new DecompilationOptions()

    options.setSettings(settings)
    options.setFullDecompilation(true)

    if (settings.getJavaFormattingOptions() == null) {
      settings.setJavaFormattingOptions(JavaFormattingOptions.createDefault())
    }

    for (name <- typeLoader.classes.keys) {
      if (exclude.contains(name)) {
        println(f"Excluding ($index of $total) $name")
      } else {
        try {
          println(f"Decompiling ($index of $total) $name")
          val cu = decompile(metadataSystem, options, name)
          println(f"Finished decompiling ($index of $total) $name")
          po.write(DecompiledClass(cu))
        } catch {
          case e: java.lang.OutOfMemoryError => println(f"Out of memory, skipping")
        }
      }
      index += 1
    }

    po.close()
  }

  def decompile(metadataSystem: MetadataSystem, options: DecompilationOptions, internalName: String): CompilationUnit = {
      var typ: TypeReference = null

      if (internalName.length() == 1) {
        val parser = new MetadataParser(IMetadataResolver.EMPTY)
        val reference = parser.parseTypeDescriptor(internalName)
        typ = metadataSystem.resolve(reference)
      } else {
        typ = metadataSystem.lookupType(internalName)
      }

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

      return Languages.java().decompileTypeToAst(resolvedType, options)
  }
}
