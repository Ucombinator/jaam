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
  * [JSON Exporter](#json-exporter)
  * [Tools](#tools)
* [Developers](#developers) -- more about Jaam's internals

## Disclaimer

This is an early work in progress. There are a lot of rough edges and bugs.

## License

This project is licensed under the [Two-Clause BSD License](licenses/LICENSE.md)
with the exception of bundled files from external projects, which are
distributed under the external project's license.  See
[`licenses/README.md`](licenses/README.md) for details.

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
| `-l`, `--log`     | Specify the logging level.                              |

The `--classpath` option takes a `:`-delimited list of directories or `.jar`
files that are the classpath of the application to be analyzed.

The `--class` option takes the fully qualified name (e.g.
`com.example.project.Main`) of the class containing the `main` method from
which to start analyzing.

The `--method` option takes the name of the `main` method from which to start
analyzing. It is usually `main`.

The `--outfile` option takes a filename at which to save the analysis output.
This file is in a binary format and is conventionally named with a `.jaam`
suffix. If you give no `--outfile` specification, the interpreter will use the
fully qualified class name for the filename
(e.g. `com.example.project.Main.jaam`).

The `--log` option takes a logging level to determine how much information to
output to `stdout`. The levels are (in increasing order of verbosity): `none`,
`error`, `warn`, `info`, `debug`, and `trace`. The default level is `info`.

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

### JSON Exporter

The JSON exporter exists to help people looking to interface with our
interpreter's results without the ability to interact with our serialization
library.

After [building](#building) the project and [producing a `.jaam` file with the
interpreter](#abstract-interpreter), produce JSON with:

```
./bin/jaam-json_exporter $file
```

where `$file` is the path to your `.jaam` file. A JSON serialization will be
output to the console's `stdout`. The serialization is a list containing JSON
objects of types `state`, `errorState`, `abstractState`, and `edge`.

### Tools

We created a Tools package to help you interact with `.jaam` files a little
better, which can be used via:

```
./bin/jaam-tools <command>
```

The supported commands are:

- `print` -- outputs all of the packets
- `info` -- provides a quick overview of a `.jaam` file
- `validate` -- determines whether a `.jaam` file provides a valid set of nodes
and edges which all connect properly
- `cat` -- combines multiple `.jaam` files sequentially

#### Print

`print` provides the ability to output all of the packets in a `.jaam` file for
reading. We foresee this mostly being used for human debugging purposes, since
any program ought to simply interact with our serializer.

```
./bin/jaam-tools print [--node number] <file>
```

`<file>` is a `.jaam` file of an interpretation. If you want to see just the
information for a specific node, you can either provide the `--node` option
with a node ID number, or you can pipe the command through grep, e.g.:

```
./bin/jaam-tools print MyFile.jaam | grep '^node-3'
./bin/jaam-tools print MyFile.jaam | grep '^edge-7'
```

#### Validate

The `validate` command takes a `.jaam` file and determines whether all of the
nodes and edges inside connect to everything they ought to. This command will
return successfully if everything checks out, and it will return with a non-zero
exit code if something goes wrong.

```
./bin/jaam-tools [--fixEOF] [--addMissingStates] [-removeMissingStates] <file>
```

- `--fixEOF` will ensure that the `.jaam` file terminates, even if the
    interpretation ended prematurely
- `--addMissingStates` checks all of the edges and nodes, and will insert
    placeholder states wherever a state is referenced but doesn't exist
- `--removeMissingStates` undoes the work of the previous command, in case you
    wish to revert a `.jaam` file to its pre-`addMissingStates` form

#### Info

`info` provides a quick overview of a `.jaam` file's interpretation. Here is an
example output, using the Factorial class in the `examples/` folder:

```
./bin/jaam-tools info Factorial.jaam

Info for Factorial.jaam
    # of States: 17
    # of Edges: 21
    # of Missing States: 0
    # of Missing State References: 0
    # of Hanging Edges: 0
    Initial State:
        sootMethod: <Factorial: void main(java.lang.String[])>
        sootStmt: r0 := @parameter0: java.lang.String[]
```

#### Cat

The `cat` command allows you to combine multiple `.jaam` files sequentially. To
use it, simply give it an output filename (where your final product will go) and
a list of input file names (separated by commas without spaces).

```
./bin/jaam-tools cat <outfile> <infile1>[,<infile2>,...]
```

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
4. Tools: helps you to manipulate/get data from .jaam files
