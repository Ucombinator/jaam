# JAAM: JVM Abstracting Abstract Machine

## Disclaimer

This is an early work in progress. There are a lot of rough edges and bugs. The
interface is bare bones as most of the current work is on the core analyzer.

## License

This project is licensed under the [BSD Two-Clause License](LICENSE.md) _with
the exception of_ the [bundled `rt.jar` file](resources/rt.jar), which is
distributed under the [GNU General Public License v. 2](LICENSE-GPLv2.md).

## Developers

Additional instructions for developers [here](docs/DEVELOPERS.md).

## Requirements

* [Java JRE](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [SBT](http://www.scala-sbt.org/)
  - Installer script included
* [Scala](http://www.scala-lang.org/)
  - Installed during first run of SBT

## Initialization

To get SBT and Scala set up, as well as compile the project for use, simply run
the following from the top level of your JAAM directory:

```
./bin/sbt assembly
```

## Usage

Currently the project functions in two parts: an analyzer (which produces a
static analysis of the Java application of your choosing) and a visualizer (which
makes it easy to look at the analysis).

Note that you must have completed [initialization](#Initialization) before
proceeding.

### Analyzer

To run the analyzer, use the `bin/jaam-analyzer.sh` script. You can supply a few
options:

| Option            | Effect                                                                |
|-------------------|-----------------------------------------------------------------------|
| `--classpath`     | Give a classpath to analyze within.                                   |
| `-c`, `--class`   | Give a specific class within the classpath.                           |
| `-m`, `--method`  | Give a specific method within the class from which to start analysis. |
| `-o`, `--outfile` | Where to save the output from the analysis for use in the visualizer. |

If you give no `--outfile` specification, the analyzer will use the given class
name as a filename.

For example, to analyze the `Factorial` class located in the `to-analyze`
classpath in this repository:

```
./bin/jaam-analyzer.sh --classpath examples -c Factorial -m main
```

It may take a moment. You will see some output to the console, and a file named
`Factorial.jaam` will be created inside your current working directory. This
file contains the raw output that will be used by the visualizer.

You can try this with most of the class files in `examples/`. `Static`
currently generates an error when run. `Fib` and `Fibonacci` currently produce
huge (and incorrect) graphs. The remaining tests work correctly, to our
knowledge.

#### Out-of-Memory Errors

You may get an out-of-memory error. If you do, you can run JAAM with extra
heap memory by specifying your `JAVA_OPTS`. For example:

```
JAVA_OPTS="-Xmx8g" {jaam-analyzer.sh invocation}
```

You can change '8g' to whatever amount of memory you need. You can also add
other Java options for controlling stack size, etc.

#### Analyzing JAR Files

To analyze a `.jar` file, you must know the fully-qualified classpath of the
main class, e.g. `com.company.project.Main`. Given a JAR file named `test.jar`
and a Main class at `com.company.test.Main`, you would invoke the analyzer with:

```
./bin/jaam-analyzer.sh --classpath test.jar -c com.company.project.Main -m main
```

### Visualizer

To run the visualizer, simply do:

```
./bin/jaam-visualizer.sh
```

This will launch a GUI for visualization of JAAM's static analysis. To give it
input, click `File` then `Load graph from message file` and specify the
`test.dat` file you created with the analyzer.

By default, all possible nodes are collapsed. Double-click on them to expand the
visualization graph.
