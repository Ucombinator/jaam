# JAAM: JVM Abstracting Abstract Machine

## Disclaimer

This is an early work in progress. There are a lot of rough edges and bugs. The
interface is bare bones as most of the current work is on the core analyzer.

## Developers

Additional instructions for developers [here](docs/DEVELOPERS.md).

## Requirements

* [Java 1.7 Development Kit](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
  - Java 1.8 is unsupported
* [SBT](http://www.scala-sbt.org/)
  - Installed automatically during bootstrapping
* [Scala](http://www.scala-lang.org/)
  - Installed automatically during bootstrapping

## Initialization

Run `./bootstrap.sh` to set up the repository for local development.

The bootstrapping script attempts to make your development with JAAM
significantly easier. When you run it, it will ask you for a number of inputs.
Most of these have default values supplied for you (and which are mostly
taken from your system automatically, e.g. the current Java). The
bootstrapping process will then ensure you have SBT and Scala installed,
assemble the JAAM project into a top-level `jaam.jar`, check the version of
the given `rt.jar` file (this is important, as JAAM is only recommended to be
run with an `rt.jar` file of version 1.7), and then writes out a nifty script
to help you use JAAM in the future.

### Finding the Path to the Java 1.7 `rt.jar` File

If you need to manually find the path your 'rt.jar' file (which should usually
be unnecessary), it is located inside the Java 1.7 "home" directory:

```
<your Java 1.7 home directory>/jre/lib/rt.jar
```

How to find the Java 1.7 home directory varies by operating system:

* On OS X, run the command `/usr/libexec/java_home -v 1.7`

* On Linux, the path might look like
  - `/usr/lib/jvm/java-7-oracle`
  - `/usr/lib/jvm/java-7-openjdk-amd64`

For example, assume we are running OS X, and the `java_home` command above
returns the path
`/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home`.
Appending `/jre/lib/rt.jar` to the end of the home directory path will give
the full path to `rt.jar`:

```
/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/jre/lib/rt.jar
```

## Usage

After bootstrapping, you only need to use the `jaam.sh` executable to run
JAAM. You can supply a few options:

| Option            | Effect                                                                |
|-------------------|-----------------------------------------------------------------------|
| `-J`, `--rt_jar`  | Specify an `rt.jar` file to use.                                      |
| `--classpath`     | Give a classpath to analyze within.                                   |
| `-c`, `--class`   | Give a specific class within the classpath.                           |
| `-m`, `--method`  | Give a specific method within the class from which to start analysis. |

Note, however, that the `--rt_jar` option usually does not need to be used, as
the `jaam.sh` script includes a reference to the `rt.jar` file you specified
during bootstrapping.

For example, to analyze the `Factorial` class located in the `to-analyze`
classpath in this repository:

```
./jaam.sh --classpath to-analyze -c Factorial -m main
```

It may take a moment, but this will launch a GUI showing the state graph.
Additional information about the graph will be printed to `stdout`.

To exit the program, press Ctrl-C at the terminal or fully quit the GUI.

You may get an out-of-memory error. If you do, you can run JAAM with extra
heap memory by specifying your `JAVA_OPTS`. For example:

```
JAVA_OPTS="-Xmx8g" ./jaam.sh --classpath to-analyze -c Factorial -m main
```

You can change '8g' to whatever amount of memory you need. You can also add
other Java options for controlling stack size, etc.

You can try this with most of the class files in `to-analyze/`. `Static`
currently generates an error when run. `Fib` and `Fibonacci` currently produce
huge (and incorrect) graphs. The remaining tests work correctly, to our
knowledge.

You may occasionally see exceptions at the terminal that are coming from the
Java GUI system (i.e. AWT or Swing). These are harmless and can safely be
ignored.

For example, you may see the following error followed by an exception stack
trace:

```
[error] (AWT-EventQueue-0) java.lang.NullPointerException
```

Because this error is in an `AWT-EventQueue`, it is one of the
harmless errors and can be safely ignored.
