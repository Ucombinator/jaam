# Scala-based analyzer framework for Java

## Disclaimer

This is an early work in progress.  There are a lot of rough edges and
bugs.  The interface is bare bones as most of the current work is on
the core analyzer.

## Requirements

SBT (http://www.scala-sbt.org/)

Scala (http://www.scala-lang.org/)

A Java 1.7 installation.  (Java 1.8 is unsupported.)


## Initialization

### Finding the Path to the Java 1.7 'rt.jar' File

You need to find the path your 'rt.jar' file, which is located inside the Java 1.7 "home" directory:

    <your Java 1.7 home directory>/jre/lib/rt.jar

To find your Java 1.7 home directory, run the appropriate command for your operating system:

* On OS X, run the command

    /usr/libexec/java_home -v 1.7

* On Linux, the path might look like

    /usr/lib/jvm/java-7-oracle

or

    /usr/lib/jvm/java-7-openjdk-amd64


For example, assume we are running OS X, and the 'java_home' command above returns the path

    /Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home

appending '/jre/lib/rt.jar' to the end of the home directory path will give the full path to 'rt.jar':

    /Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/jre/lib/rt.jar


## Usage

### Default (Control Flow Analysis) Mode

Simply run:

    sbt 'run --classpath <classpath> -c <class> -m <main>'

 - `<classpath>` is the classpath to be used by the analyzer (which
   may differ from the classpath used by sbt and Scala).  The
   classpath must contain the path to your `rt.jar` file (described
   above).  The classpath must also contain the paths to the things to
   be analyzed.  The paths that comprise the classpath must be
   separated by the path separator for your operating system (`:` for
   Linux and OS X, `;` for Windows).

   For example, on OS X, when analyzing the files in the
   `to-analyze` directory, the classpath might be:

   --classpath /Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/jre/lib/rt.jar:to-analyze

 - `<class>` is the name of the class containing the `main` function
   from which to start the analysis.

 - `<main>` is the name of the `main` function from which to start the
   analysis.

For example, analyzing `Factorial` located in `to-analyze` on OS X might look like:

    sbt 'run --classpath /Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/jre/lib/rt.jar:to-analyze -c Factorial -m main'

The first time you run this `sbt` will download a number of packages
on which our tool depends.  This may take a while, but these are
cached and will not need to be downloaded on successive runs.

After a while, this will launch a GUI showing the state graph and will
print out graph data to stdout.

To exit the program press Ctrl-C at the terminal.  (Closing the GUI
window is not enough.)

You may get an out of memory error--if so, you can run sbt with extra heap memory.  For example,

    JAVA_OPTS="-Xmx8g" sbt 'run --classpath /Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/jre/lib/rt.jar:to-analyze -c Factorial -m main'

You can change '8g' to whatever amount of memory you need.  You can also add other Java options for controlling stack size, etc.

You can try this with most of the class files in `to-analyze/`.  `Static` currently generates an error when run.  `Fib` and `Fibonacci` currently produce huge (and incorrect) graphs.  The remaining tests work correctly, to our knowledge.

You may occasionally see exceptions at the terminal that are coming
from the Java GUI system (i.e. AWT or Swing).  These are harmless and
can safely be ignored.

### CFG Mode

Run:
    
    sbt 'run --cfg <application-classpath> --classpath <library-classpath> -c <class>'

 - `<application-classpath>` is the directory containing the class
   files to analyze.  For the examples included with the source, this
   should be `to-cfg`.

 - `<library-classpath>` must contain the path to `rt.jar` (see
   above).  It may also contain the path to other libraries needed for
   the analysis.  The paths that comprise the classpath must be
   separated by the path separator for your operating system (`:` for
   Linux and OS X, `;` for Windows).

 - `<class>` is the name of the class containing the `main` function
   from which to start the analysis.

The call graph in JSON format will be dump to stdout.

For example, a call on OS X might look like:

    sbt 'run --cfg to-cfg --classpath /Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/jre/lib/rt.jar -c Factorial'

The JSON output is an array, and each object in the array represent a statement
int the program with an unqiue `id`, the object also has following items:

 - `method` is the signature of method.

 - `inst` is the statement.

 - `targets` is an array contains the target statements. If the target is a function,
    it points to the first statement of callee. If the target is the caller, then it
    will point back to the statement which invokes to here.

 - `succ` is an array contains the successor statements of current statement.
