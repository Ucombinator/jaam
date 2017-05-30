package org.ucombinator.jaam.util {
  import soot.{ClassProvider, ClassSource}

  // Use Soot.useJaamClassProvider to install this provider
  class JaamClassProvider extends ClassProvider {
    override def find(cls: String): ClassSource = {
      Soot.classes.get(cls) match {
        case None => null
        case Some(c) =>
          new soot.asm.JaamClassSource(c.source, new soot.JaamFoundFile(cls, c.data))
      }
    }
  }
}

// The constructors for `AsmClassSource` and `SourceLocator.FoundFile` are
// Java default access.  To access them we have to inject into the `soot` and
// `soot.asm` packages.
package soot {
  import java.io.{ByteArrayInputStream, File, InputStream}

  class JaamFoundFile(path: String, data: Array[Byte]) extends SourceLocator.FoundFile(new File(f"JaamFoundFile:$path")) {
    override def inputStream(): InputStream = new ByteArrayInputStream(data)
  }
}

package soot.asm {
  import soot.SourceLocator

  class JaamClassSource(cls: String, foundFile: SourceLocator.FoundFile) extends AsmClassSource(cls, foundFile)
}
