package org.ucombinator.jaam.tools

import org.ucombinator.jaam.serializer._

object Coverage {
  def findCoverage(jaamFile: String, jarFiles: Seq[String]) = {
    // Iterate over jarfiles.
    //   Iterate over each class in each jarfile.
    //     Initialize hashtable with methods for keys.
    // Iterate over statements in jaam file.
    //   For each statement, add statement to method in hash.
    // Print results.
  }
}
