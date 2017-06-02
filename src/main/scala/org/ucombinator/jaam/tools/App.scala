package org.ucombinator.jaam.tools.app

import java.io.FileOutputStream
import java.nio.file._

import scala.collection.JavaConverters._

import org.ucombinator.jaam.{serializer, tools}
import org.ucombinator.jaam.util._

object App extends tools.Main("app") {
  banner("TODO")
  footer("")

  val input = opt[List[String]](short = 'i', descr = "class files, or directories (role is auto-detected)", default = Some(List()))
  val app = opt[List[String]](short = 'a', descr = "application jars, class files, or directories", default = Some(List()))
  val lib = opt[List[String]](short = 'l', descr = "library jars, class files, or directories", default = Some(List()))
  val jvm = opt[List[String]](short = 'r', descr = "Java runtime jars, class files, or directories", default = Some(List()))
  val defaultJvm = toggle(prefix = "no-", default = Some(true))

  val detectMain = toggle(prefix = "no-", default = Some(true))
  val mainClass = opt[String](short = 'c', descr = "the main class")
  val mainMethod = opt[String](short = 'm', descr = "the main method")

  val output = opt[String](required = true, short = 'o', descr = "the output file for the serialized data")

  // TODO: val java-8-rt (in resource?)

  def run(conf: tools.Conf) {
    Main.main(input(), app(), lib(), jvm(), defaultJvm(), detectMain(), mainClass.toOption, mainMethod.toOption, output())
  }
}

sealed trait FileRole

object FileRole { // TODO: Move into App object?
  case object APP extends FileRole
  case object LIB extends FileRole
  case object JVM extends FileRole
}

case class PathElement(path: String, root: String, role: FileRole, data: Array[Byte]) {
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
  def read(root: Path, path: Path, role: Option[FileRole]): List[PathElement] = {
    if (path.toFile.isDirectory) {
      return (for (p <- Files.newDirectoryStream(path).asScala) yield {
        try { read(root, p, role) }
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
      return List(PathElement(path.toString, root.toString, role.getOrElse(FileRole.APP), data))
    } else if (path.toString.endsWith(".jar")) {
      val data = Files.readAllBytes(path)
      if (!data.startsWith(List(0x50, 0x4B, 0x03, 0x04))) {
        throw new Exception(f"Malformed class file $path at $root")
      }

      val jar = Jar.jar(data)

      def getMains(): List[String] = {
        Jar.entries(jar).map(_._1.getName)
          .filter(_.endsWith("StacMain.class"))
          .map(_.stripSuffix(".class").replace('/', '.'))
      }

      val detectedRole = role match {
        case Some(r) =>
          if (r == FileRole.APP) { mains ++= getMains()}
          r
        case None =>
          val main = jar.getManifest.getMainAttributes.getValue("Main-Class")
          if (main != null) { mains :+= main; println("manifest"); FileRole.APP }
          else {
            getMains() match {
              case List() => FileRole.LIB
              case es => mains ++= es; FileRole.APP
            }
          }
      }

      // TODO: set class main (do error if not found, maybe do more searching)

      return List(PathElement(path.toString, root.toString, detectedRole, data))
    } else {
      throw new Exception("not a directory, class, or jar")
    }
  }

  def main(input: List[String], app: List[String], lib: List[String], jvm: List[String], defaultJvm: Boolean, detectMain: Boolean, mainClass: Option[String], mainMethod: Option[String], jaam: String) {
    def readList(list: List[String], role: Option[FileRole]) =
      list.map({ x => read(Paths.get(x), Paths.get(x), role)}).flatten.toArray

    val appConfig = App()
    appConfig.classpath ++= readList(input, None)
    appConfig.classpath ++= readList(app, Some(FileRole.APP))
    appConfig.classpath ++= readList(lib, Some(FileRole.LIB))
    appConfig.classpath ++= readList(jvm, Some(FileRole.JVM))
    // TODO: add rt.jar (and others?)

    if (defaultJvm) {
      val JVM_JARS = "/java-1.7.0-openjdk-headless-1.7.0.85-2.6.1.2.el7_1.x86_64.zip"
      val res = getClass.getResourceAsStream(JVM_JARS)
      for (entry <- Zip.entries(Zip.zip(res))) {
        if (entry._1.getName.endsWith(".jar")) {
          appConfig.classpath :+= PathElement("resource:"+entry._1.getName, JVM_JARS, FileRole.JVM, entry._2)
        }
      }
    }

    for (c <- appConfig.classpath) {
      println(f"In ${c.root} found a ${c.role} file: ${c.path}")
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
