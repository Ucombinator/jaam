# Scala-based analyzer framework for shimple

## Disclaimer

This is an early work in progress.  There are a lot of rough edges and
bugs.  The interface is bare bones as most of the current work is on
the core analyzer.

## Requirements

SBT (http://www.scala-sbt.org/)

Scala (http://www.scala-lang.org/)

A Java 1.7 installation.  (Java 1.8 may not work.)

## Initialization

Before running the analyzer for the first time, you must run:

    ./classGrabber.sh

This will populate `javacache/` with class files from your Java
installation.  For copyright reasons, we cannot distribute these with
the code.

Note that if you get an error like the following is is likely
that `./classGrabber.sh` pulled the class files from a Java 1.8
installation instead of Java 1.7.

    [error] (run-main-0) java.lang.RuntimeException: Assertion failed.

If this happens, ensure that only Java 1.7 is visible, then
delete the contents of `javacache/` and rerun `./classGrabber.sh`.

## Usage

Simply run:

    sbt 'run <class-directory> <class> <main>'

 - `<class-directory>` is the directory containing the class files to
   analyze.  For the examples included with the source, this should be
   `to-analyze`.

 - `<class>` is the name of the class containing the `main` function
   from which to start the analysis.

 - `<main>` is the name of the `main` function from which to start the
   analysis.

For example, you could run:

    sbt 'run to-analyze Factorial main'

The first time you run this `sbt` will download a number of packages
on which our tool depends.  This may take a while, but these are
cached and will not need to be downloaded on successive runs.

After a while, this will launch a GUI showing the state graph and will
print out graph data to stdout.

To exit the program press Ctrl-C at the terminal.  (Closing the GUI
window is not enough.)

You can try this with most of the class files in `to-analyze/`, but some
of them trigger bugs that we have yet to fix.  The following are known
to work:

  Arrays, BoolTest, Casting, CFGTest, Exceptions, Factorial, Fib, Goto,
  InnerClassCasting, Objects, ObjectsNewStmt, Statics, SwitchTest

You may occasionally see exceptions at the terminal that are coming
from the Java GUI system (i.e. AWT or Swing).  These are harmless and
can safely be ignored.
