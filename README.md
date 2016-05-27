# Jaam: JVM Abstracting Abstract Machine

Jaam analyzes JVM bytecode to try to discover vulnerabilities and side channels.

## Contents

* [Disclaimer](#disclaimer)
* [License](#license)
* [Requirements](#requirements)
* [Building](#building) -- how to get Jaam running
* [Usage](#usage) -- how to use Jaam
  * [Analyzer](#analyzer)
  * [Visualizer](#visualizer)
* [Developers](#developers) -- more about Jaam's internals

## Disclaimer

This is an early work in progress. There are a lot of rough edges and bugs.

## License

This project is licensed under the [BSD Two-Clause License](LICENSE.md) _with
the exception of_ the [bundled `rt.jar` file](resources/rt.jar), which is
distributed under the [GNU General Public License v. 2](LICENSE-GPLv2.md) as it
originates from OpenJDK.

## Requirements

The only requirement is to have a copy of the [Java
JRE](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
installed so that `java` can be run.

Building the tool automatically downloads the other parts that are needed.

## Building

To compile the project for use, simply run the following from the top level of
your Jaam directory:

```
./bin/sbt assembly
```

## Usage

Currently the project functions in two parts: an abstract interpreter (which
produces a static analysis of the Java application of your choosing) and a
visualizer (which makes it easy to look at the analysis results).

Note that you must have completed [building Jaam](#Building) before proceeding.

### Abstract Interpreter

To run the analyzer, use the `bin/jaam-interpreter.sh` script. You can supply a
few options:

| Option            | Effect                                                                |
|-------------------|-----------------------------------------------------------------------|
| `--classpath`     | Give a classpath to analyze within.                                   |
| `-c`, `--class`   | Give a specific class within the classpath.                           |
| `-m`, `--method`  | Give a specific method within the class from which to start analysis. |
| `-o`, `--outfile` | Where to save the output from the analysis for use in the visualizer. |

If you give no `--outfile` specification, the analyzer will use the fully
qualified class name as a filename.

For example, to analyze the `Factorial` class located in the `to-analyze`
classpath in this repository, first compile it to a .class file. Then run the provided script:

```
./bin/jaam-interpreter.sh --classpath examples -c Factorial -m main
```

It may take a moment. You will see some output to the console, and a file named
`Factorial.jaam` will be created inside your current working directory. This
file contains the raw output that will be used by the visualizer.

You can try this with most of the class files in `examples/`. `Static`
currently generates an error when run. `Fib` and `Fibonacci` currently produce
huge (and incorrect) graphs. The remaining tests work correctly, to our
knowledge.

#### Out-of-Memory Errors

You may get an out-of-memory error. If you do, you can run Jaam with extra
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

This will launch a GUI for visualization of Jaam's static analysis. To give it
input, click `File` then `Load graph from message file` and specify the
`.jaam` file you created with the analyzer (`Factorial.jaam` if you used the
given example).

By default, all possible nodes are collapsed. Double-click on them to expand the
visualization graph.

## Developers

Some people may want to know more about how the project is organized or how it
functions on a deeper level. This is the section for those people.

### Build System

The project is managed by SBT: the Simple Build Tool. SBT allows for the easy
compilation of a multi-faceted project such as ours with external dependencies
and such all handled automagically.

The build settings are all stored in the various `build.sbt` files you'll see
throughout the project hierarchy: one at the top level, and one for each
subproject inside the `src` directory.

We use the SBT module [assembly](https://github.com/sbt/sbt-assembly) to produce
"fat JARs" -- JAR files that are self-contained and fully executable. The
subprojects appropriately build with each other contained inside if needed.

### Organization

We've split our project into a few subprojects:

1. Interpreter: performs static analysis on Java classes
2. Visualizer: shows the results of the analysis
3. Serializer: defines the .jaam file format and allows interoperability between Interpreter and Visualizer
