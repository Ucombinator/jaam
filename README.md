# JAAM: JVM Abstracting Abstract Machine

## Disclaimer

This is an early work in progress. There are a lot of rough edges and bugs. The
interface is bare bones as most of the current work is on the core analyzer.

## Requirements

* [SBT](http://www.scala-sbt.org/)
* [Scala](http://www.scala-lang.org/)
* [Java 1.7 Development Kit](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
  - Java 1.8 is unsupported
* [Python 2](https://www.python.org/downloads/) — for the `jaam.py` wrapper script

## Initialization

The first time you run the tool, `sbt` will download a number of packages that
the JAAM tool depends on. This may take a while, but these files are cached and
will not need to be re-downloaded on successive runs.

### Finding the Path to the Java 1.7 `rt.jar` File

If your are using Java 1.7, then the `find-rt-jar.sh` script in `bin` can automatically find it for you. This script can
also be automatically be used by the `jaam.py` wrapper (see below).

If you need to manually find the path your 'rt.jar' file, it is located inside the Java 1.7 "home" directory:

```
<your Java 1.7 home directory>/jre/lib/rt.jar
```

How to find the Java 1.7 home directory varies by operating system:

* On OS X, run the command `/usr/libexec/java_home -v 1.7`

* On Linux, the path might look like
  - `/usr/lib/jvm/java-7-oracle`
  - `/usr/lib/jvm/java-7-openjdk-amd64`

For example, assume we are running OS X, and the `java_home` command above returns the path
`/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home`. Appending
`/jre/lib/rt.jar` to the end of the home directory path will give the full path to `rt.jar`:

```
/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/jre/lib/rt.jar
```

## Usage

### Control Flow Analysis Mode

Simply run:

```
jaam.py -J <rt.jar path> -P <classpath> -c <class> -m <main>
```

* `<rt.jar path>` is the full path to your `rt.jar` file (see above).
* `<classpath>` is the path to be used by the analyzer. This must contain the
  paths to the things to be analyzed. You can specify multiple
  `<library-classpath>`s by using multiple `-P` arguments.
* `<class>` is the name of the class containing the `main` function from which
  to start the analysis.
* `<main>` is the name of the `main` function from which to start the analysis.

Note that the `jaam.py` wrapper can automatically use the `bin/find-rt-jar.sh` script to find your `rt.jar` file for
you by simply not using the `-J` option. This will be the assumed course of action in the rest of the examples.

For example, analyzing `Factorial` located in `to-analyze` on OS X might look like:

```
jaam.py -P to-analyze -c Factorial -m main
```

After a while, this will launch a GUI showing the state graph and will print out
information about the graph to `stdout`.

To exit the program press Ctrl-C at the terminal or fully quit the GUI.

You may get an out-of-memory error. If you do, you can run JAAM with extra heap
memory. For example:

```
jaam.py -P to-analyze -c Factorial -m main --java-opts="-Xmx8g"
```

You can change '8g' to whatever amount of memory you need. You can also add
other Java options for controlling stack size, etc.

You can try this with most of the class files in `to-analyze/`. `Static`
currently generates an error when run. `Fib` and `Fibonacci` currently produce
huge (and incorrect) graphs. The remaining tests work correctly, to our knowledge.

You may occasionally see exceptions at the terminal that are coming from the
Java GUI system (i.e. AWT or Swing). These are harmless and can safely be ignored.

For example, you may sees the following error followed by an exception stack trace:

```
[error] (AWT-EventQueue-0) java.lang.NullPointerException
```

Because this error is in an `AWT-EventQueue`, it is one of the
harmless errors and can be safely ignored.

### Behind the Scenes

The `jaam.py` wrapper script actually just handles calls to `sbt` for you. It
was developed primarily because calling `sbt` proved cumbersome; since the input
to `sbt` requires multiple arguments to be passed through in a single string,
tab-completion couldn't be used. Additionally, the `sbt` command has to be
called from the `jaam` directory, which is obnoxious when you want to analyze
something in a different directory.

The `jaam.py` wrapper handles both of these drawbacks. The arguments are handled
separately, meaning tab-completion works in the shell, and the wrapper script
automatically handles running `sbt` from the `jaam` directory. (Technically, it
runs the command from wherever `jaam.py` is located — so don't move it!)
