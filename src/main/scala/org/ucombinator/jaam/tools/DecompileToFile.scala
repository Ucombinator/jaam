package org.ucombinator.jaam.tools.decompileToFile

import java.io._

import scala.collection.JavaConverters._

object DecompileToFile {
  def main(input: List[String], output: String) {
    val outputDir = new File(output)
    for (file <- input) {
      for (org.ucombinator.jaam.tools.decompile.DecompiledClass(_, _, cu)
           <- org.ucombinator.jaam.serializer.Serializer.readAll(file)) {
        val dir = new File(outputDir, cu.getPackage.getName.replace('.', '/'))
        dir.mkdirs()

        val typeName = cu.getTypes.asScala.toList.head.getName
        val outputFile = new File(dir, typeName + ".java")
        println(f"Writting $outputFile")
        val writer = new PrintWriter(outputFile)
        writer.write(cu.getText)
        writer.close()
      }
    }
  }
}
