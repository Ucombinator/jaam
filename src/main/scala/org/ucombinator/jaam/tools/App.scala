package org.ucombinator.jaam.tools.app

import java.io.FileOutputStream
import java.nio.file._

import scala.collection.JavaConverters._

import org.ucombinator.jaam.{serializer, tools}
import org.ucombinator.jaam.util._

sealed trait Origin

object Origin { // TODO: Move into App object?
  case object APP extends Origin
  case object LIB extends Origin
  case object JVM extends Origin
}

case class PathElement(path: String, root: String, origin: Origin, data: Array[Byte]) {
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
  }
}

case class App() extends serializer.Packet {
  var name = None: Option[String]
  var classpath = Array[PathElement]()
  object main {
    var className = None: Option[String]
    var methodName = None: Option[String]
  }
//  object java {
//    var opts = null: String
//  }
}


//jaam-tools app
// TODO: automatically find main and StacMain
// TODO: automatically determine app vs lib classes by finding main
// TODO: automatically find all jar files in subdirectory
object Main {
  var mains = List[String]() // TODO: set class and method name from mains (error if multiple)

  // relative to root
  def read(root: Path, path: Path, origin: Option[Origin]): List[PathElement] = {
    if (path.toFile.isDirectory) {
      return (for (p <- Files.newDirectoryStream(path).asScala) yield {
        try { read(root, p, origin) }
        catch {
          case e: Exception =>
            println("Ignoring " + root + " and " + p + " because " + e)
            List()
        }
      }).toList.flatten
    } else if (path.toString.endsWith(".class")) {
      val data = Files.readAllBytes(path)
      if (!data.startsWith(List(0xCA, 0xFE, 0xBA, 0xBE))) {
        throw new Exception(f"Malformed class file $path at $root")
      }
      return List(PathElement(path.toString, root.toString, origin.getOrElse(Origin.APP), data))
    } else if (path.toString.endsWith(".jar")) {
      val data = Files.readAllBytes(path)
      if (!data.startsWith(List(0x50, 0x4B, 0x03, 0x04))) {
        throw new Exception(f"Malformed class file $path at $root")
      }

      val jar = Jar.jar(data)

      def getMains(): List[String] = {
        val main = jar.getManifest.getMainAttributes.getValue("Main-Class")
        if (main != null) { return List(main) }
        else {
          return Jar.entries(jar).map(_._1.getName)
            .filter(_.endsWith("StacMain.class"))
            .map(_.stripSuffix(".class").replace('/', '.'))
        }
      }

      val detectedOrigin = origin match {
        case Some(r) =>
          if (r == Origin.APP) { mains ++= getMains()}
          r
        case None =>
          getMains() match {
            case List() => Origin.LIB
            case es => mains ++= es; Origin.APP
          }
      }

      // TODO: set class main (do error if not found, maybe do more searching)

      return List(PathElement(path.toString, root.toString, detectedOrigin, data))
    } else {
      throw new Exception("not a directory, class, or jar")
    }
  }

  def main(input: List[String], app: List[String], lib: List[String], jvm: List[String], defaultJvm: Boolean, detectMain: Boolean, mainClass: Option[String], mainMethod: Option[String], jaam: String) {
    def readList(list: List[String], origin: Option[Origin]) =
      list.map({ x => read(Paths.get(x), Paths.get(x), origin)}).flatten.toArray

    val appConfig = App()
    appConfig.classpath ++= readList(input, None)
    appConfig.classpath ++= readList(app, Some(Origin.APP))
    appConfig.classpath ++= readList(lib, Some(Origin.LIB))
    appConfig.classpath ++= readList(jvm, Some(Origin.JVM))

    if (defaultJvm) {
      val JVM_JARS = "java-1.7.0-openjdk-headless-1.7.0.85-2.6.1.2.el7_1.x86_64.zip"
      val res = getClass.getResourceAsStream(JVM_JARS)
      for (entry <- Zip.entries(Zip.zip(res))) {
        if (entry._1.getName.endsWith(".jar")) {
          appConfig.classpath :+= PathElement("resource:"+entry._1.getName, JVM_JARS, Origin.JVM, entry._2)
        }
      }
    }

    for (c <- appConfig.classpath) {
      println(f"In ${c.root} found a ${c.origin} file: ${c.path}")
    }

    appConfig.main.className = mainClass match {
      case Some(s) => Some(s)
      case None =>
        if (!detectMain) {
          None
        } else {
          mains match {
            case List() =>
              println("WARNING: No main class found")
              None
            case List(x) => Some(x)
            case xs =>
              println("WARNING: multiple main classes found")
              for (x <- xs) {
                println(f" - $x\n")
              }
              None
          }
        }
    }

    appConfig.main.methodName = mainMethod.orElse(appConfig.main.className match {
      case Some(_) => Some("main")
      case None => None
    })

    println(f"Main class: ${appConfig.main.className}")
    println(f"Main method: ${appConfig.main.methodName}")

    val outStream = new FileOutputStream(jaam)
    val po = new serializer.PacketOutput(outStream)
    po.write(appConfig)
    po.close()
  }
}

// jaam-tools app --input airplan_1/ --output airplan_1.app.jaam
