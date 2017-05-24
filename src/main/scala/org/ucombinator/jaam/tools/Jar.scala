package org.ucombinator.jaam.tools.jar

import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.IOException
import java.nio.file._

import com.strobel.assembler.InputTypeLoader
import com.strobel.assembler.metadata._
import com.strobel.core.VerifyArgument
import com.strobel.decompiler.languages.java.JavaFormattingOptions
import com.strobel.decompiler.languages.Languages

import org.ucombinator.jaam.tools //.{Main,Conf}
import org.ucombinator.jaam.serializer

import com.strobel.decompiler._

import scala.collection.mutable
import scala.collection.JavaConverters._

object Jar extends tools.Main("jar") {
  banner("TODO")
  footer("")

  val app = opt[List[String]](short = 'a', descr = "application jars, class files, or directories")
  val lib = opt[List[String]](short = 'l', descr = "library jars, class files, or directories")
  val rt = opt[List[String]](short = 'r', descr = "Java runtime jars, class files, or directories")

  //val mainClass = opt[String](required = true, short = 'c', descr = "the main class")
  //val mainMethod = opt[String](required = true, short = 'm', descr = "the main method", default = Some("main"))

  val output = opt[String](required = true, short = 'o', descr = "the output file for the serialized data")

  def run(conf: tools.Conf) {
    Main.main(app.getOrElse(List()), lib.getOrElse(List()), rt.getOrElse(List()), output())
  }
}

case class PathElement(root: String, path: String, isJar: Boolean, data: Array[Byte])
case class ClassData(rt: Array[PathElement], lib: Array[PathElement], app: Array[PathElement]) extends serializer.Packet

// TODO: rename to create
// TODO: automatically find main and StacMain
object Main {
  // relative to root
  def read(root: Path, path: Path): List[PathElement] = {
    println("Reading " + root + " and " + path)
    if (path.toFile.isDirectory) {
      return (for (p <- Files.newDirectoryStream(path).asScala) yield {
        try { read(root, p) }
        catch {
          case e: Exception =>
            println("Skipping " + root + " and " + p + " because " + e)
            List()
        }
      }).toList.flatten
    } else if (path.toString.endsWith(".class")) {
      val data = Files.readAllBytes(path)
      data.startsWith(List(0xCA, 0xFE, 0xBA, 0xBE))
      return List(PathElement(root.toString, path.relativize(root).toString, false, data.to))
    } else if (path.toString.endsWith(".jar")) {
      val data = Files.readAllBytes(path)
      data.startsWith(List(0x50, 0x4B, 0x03, 0x04))
      return List(PathElement(root.toString, path.relativize(root).toString, true, data))
    } else {
      throw new Exception("not a directory, class, or jar")
    }
  }

  def main(app: List[String], lib: List[String], rt: List[String], jaam: String) {
    def readList(list: List[String]) =
      list.map({ x => read(Paths.get(x), Paths.get(x))}).flatten.toArray

    val appElements = readList(app)
    val libElements = readList(lib)
    val rtElement = readList(rt)

    val outStream = new FileOutputStream(jaam)
    val po = new serializer.PacketOutput(outStream)

    po.write(ClassData(rtElement, libElements, appElements))
    // TODO: write config to jaam
    po.close()
  }
}
