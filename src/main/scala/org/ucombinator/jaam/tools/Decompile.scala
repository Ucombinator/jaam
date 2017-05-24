package org.ucombinator.jaam.tools.decompile

import java.io.FileOutputStream
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

import org.ucombinator.jaam.tools //.{Main,Conf}
import org.ucombinator.jaam.tools.app //.{Main,Conf}

import com.strobel.decompiler._

import scala.collection.mutable

import org.ucombinator.jaam.serializer
import scala.collection.JavaConverters._


object Decompile extends tools.Main("decompile") {
  banner("TODO")
  footer("")

//  val append = toggle(
//    descrYes = "wait for user to press enter before starting (default: off)",
//    noshort = true, prefix = "no-", default = Some(false))

  val rt = toggle(
    descrYes = "wait for user to press enter before starting (default: off)",
    noshort = true, prefix = "no-", default = Some(false))
  val lib = toggle(
    descrYes = "wait for user to press enter before starting (default: off)",
    noshort = true, prefix = "no-", default = Some(false))
  val app = toggle(
    descrYes = "wait for user to press enter before starting (default: true)",
    noshort = true, prefix = "no-", default = Some(true))

  val exclude = opt[List[String]](descr = "Class names to omit", default = Some(List()))
  val input = opt[List[String]](required = true, descr = "List of jaam files")
  val output = opt[String](required = true, descr = "TODO")

  def run(conf: tools.Conf) {
    Main.main(input(), output(), exclude(), rt(), lib(), app())
  }
}

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
    } catch { case e: Exception => println("While loading: " + e) }
  }
}

case class DecompiledClass(compilationUnit: CompilationUnit) extends serializer.Packet

object Main {
  def main(input: List[String], output: String, exclude: List[String], rt: Boolean, lib: Boolean, app: Boolean) {
    val typeLoader = new HashMapTypeLoader()

    def loadData(p: tools.app.App.PathElement) = p match {
      case tools.app.App.PathElement(path, root, data) =>
        println(f"PathElement: $path ($root)")
        if (!path.endsWith(".jar")) typeLoader.add(data)
        else {
          val jar = new java.util.jar.JarInputStream(
            new java.io.ByteArrayInputStream(data))

          var entry: java.util.jar.JarEntry = null
          while ({entry = jar.getNextJarEntry(); entry != null}) {
            println(entry.getName())
            val bytes = new Array[Byte](entry.getSize.toInt)
            jar.read(bytes, 0, entry.getSize.toInt)
            typeLoader.add(bytes)
          }
        }
    }

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
