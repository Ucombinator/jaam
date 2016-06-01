# Jaam: JVM Abstracting Abstract Machine

Jaam analyzes JVM bytecode to try to discover vulnerabilities and side channels.

## Contents

* [Disclaimer](#disclaimer)
* [License](#license)
* [Requirements](#requirements)
* [Building](#building) -- how to get Jaam running
* [Usage](#usage) -- how to use Jaam
  * [Abstract Interpreter](#abstract-interpreter)
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

To run the abstract interpreter, use the `bin/jaam-interpreter` script. You can
supply a few options:

| Option            | Argument                                                |
|-------------------|---------------------------------------------------------|
| `--classpath`     | The classpath for the application to be analyzed.       |
| `-c`, `--class`   | Fully qualified name of the main class to be analyzed.  |
| `-m`, `--method`  | Method name of the main method to be analyzed.          |
| `-o`, `--outfile` | Filename at which to save analysis output.              |

The `--classpath` option takes a `:`-delimited list of directories or `.jar`
files that are the classpath of the application to be analyzed.

The `--class` option takes the fully qualified name (e.g.
`com.example.project.Main`) of the class containing the `main` method from
which to start analyzing.

The `--method` option takes the name of the `main` method from which to start
analyzing. It is usually `main`.

The `--outfile` options takes a filename at which to save the analysis output.
This file is in a binary format and is conventionally named with a `.jaam`
suffix. If you give no `--outfile` specification, the interpreter will use the
fully qualified class name for the filename
(e.g. `com.example.project.Main.jaam").

#### Example Usage for a `.class` File

To analyze the `Factorial` class located in the `examples` directory in this
repository, first compile it to a `.class` file by running `javac *.java` in
the `examples` directory. Then run the interpreter from the top-level directory
of the repository:

```
./bin/jaam-interpreter --classpath examples -c Factorial -m main
```

You will see some output to the console, and a file named `Factorial.jaam` will
be created inside your current working directory. This file contains the raw
output that will be used by the visualizer.

You can try this with most of the files in `examples/`. However, `Static`
currently generates an error when run, and `Fib` and `Fibonacci` currently
produce huge (and incorrect) graphs. The remaining tests work correctly, to our
knowledge.

#### Example Usage for a `.jar` File

As another example, suppose `test.jar` contains the code to be analyzed and
that the main class is `com.example.project.Main` with a main method named
`main`.  You would then invoke the interpreter with:

```
./bin/jaam-interpreter --classpath test.jar -c com.example.project.Main -m main
```

This would generate a file named `com.example.project.Main.jaam` that contains
the analysis results.

#### Out-of-Memory Errors

You may get an out-of-memory error. If you do, you can run Jaam with extra
heap memory by specifying your `JAVA_OPTS`. For example:

```
JAVA_OPTS="-Xmx8g" {jaam-interpreter invocation}
```

You can change '8g' to whatever amount of memory you need. You can also add
other Java options for controlling stack size, etc.


### Visualizer

To run the visualizer, simply do:

```
./bin/jaam-visualizer
```

This will launch a GUI for visualization of Jaam's static analysis. To give it
input, click `File` then `Load graph from message file` and specify the `.jaam`
file you created with the interpreter (`Factorial.jaam` if you used the given
example).

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
