package org.ucombinator.jaam.tools.app

import java.io.FileOutputStream
import java.nio.file._

import scala.collection.JavaConverters._

import org.ucombinator.jaam.{serializer, tools}

object App extends tools.Main("app") {
  banner("TODO")
  footer("")

  val app = opt[List[String]](short = 'a', descr = "application jars, class files, or directories")
  val lib = opt[List[String]](short = 'l', descr = "library jars, class files, or directories")
  val java = opt[List[String]](short = 'r', descr = "Java runtime jars, class files, or directories")

  //val mainClass = opt[String](required = true, short = 'c', descr = "the main class")
  //val mainMethod = opt[String](required = true, short = 'm', descr = "the main method", default = Some("main"))

  val output = opt[String](required = true, short = 'o', descr = "the output file for the serialized data")

  def run(conf: tools.Conf) {
    Main.main(app.getOrElse(List()), lib.getOrElse(List()), java.getOrElse(List()), output())
  }
}

case class PathElement(path: String, root: String, data: Array[Byte]) {
  def classData(): List[Array[Byte]] = {
    if (path.endsWith(".class")) List(data)
    else if (path.endsWith(".jar")) {
      for ((e, d) <- org.ucombinator.jaam.util.Jar.entries(new java.io.ByteArrayInputStream(data));
        if e.getName.endsWith(".class")) yield {
        d
      }
    } else {
      List()
    }

/*
      var result = List[Array[Byte]]()
      var entry: java.util.jar.JarEntry = null
      while ({entry = jar.getNextJarEntry(); entry != null}) {
        println(f"classData $path($root) ${entry.getName()} ${entry.getSize}")
        val bytes = new Array[Byte](entry.getSize.toInt)
        jar.read(bytes, 0, entry.getSize.toInt)
        if (entry.getName.endsWith(".class")) { // TODO: be smarter about this
          result ++= List(bytes)
        }
      }

      result
    }
 */
  }
}

//case class PathElement(root: String, path: String, isJar: Boolean, data: Array[Byte])
//case class ClassData(rt: Array[PathElement], lib: Array[PathElement], app: Array[PathElement]) extends serializer.Packet

case class App() extends serializer.Packet {
  var name = None: Option[String]
  object classpath {
    var java = Array[PathElement]()
    var lib = Array[PathElement]()
    var app = Array[PathElement]()
  }
  object main {
    var className = None: Option[String]
    var methodName = None: Option[String]
  }
//  object java {
//    var opts = null: String
//  }
}


//jaam-tools app
// TODO: rename to create
// TODO: automatically find main and StacMain
// TODO: automatically determine app vs lib classes by finding main
// TODO: automatically find all jar files in subdirectory
object Main {
  // relative to root
  def read(root: Path, path: Path): List[PathElement] = {
    println("Reading " + root + " and " + path) // TODO: as lib/app/java
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
      return List(PathElement(path.toString, root.toString, data))
    } else if (path.toString.endsWith(".jar")) {
      val data = Files.readAllBytes(path)
      data.startsWith(List(0x50, 0x4B, 0x03, 0x04))
      return List(PathElement(root.toString, root.toString, data))
    } else {
      throw new Exception("not a directory, class, or jar")
    }
  }

  def main(app: List[String], lib: List[String], rt: List[String], jaam: String) {
    def readList(list: List[String]) =
      list.map({ x => read(Paths.get(x), Paths.get(x))}).flatten.toArray

//    val appElements = readList(app)
//    val libElements = readList(lib)
//    val rtElement = readList(rt)

    val appConfig = App()
    appConfig.classpath.app = readList(app)
    appConfig.classpath.lib = readList(lib)
    appConfig.classpath.java = readList(rt)

    val outStream = new FileOutputStream(jaam)
    val po = new serializer.PacketOutput(outStream)
    po.write(appConfig)
    // TODO: write config to jaam
    po.close()
  }
}
