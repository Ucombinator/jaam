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

First, you need to prepare an `.all.jaam` file.  If the application to be
analyzed is in the `<application>` directory, then run the following commands.

    ./bin/jaam app       --input <application>/         --output <application>.app.jaam
    ./bin/jaam decompile --input <application>.app.jaam --output <application>.decompile.jaam
    ./bin/jaam loop3     --input <application>.app.jaam --output <application>.loop3.jaam
    ./bin/jaam cat       --input <application>.app.jaam \
                         --input <application>.decompile.jaam \
                         --input <application>.loop3.jaam \
                         --output <application>.all.jaam

Now that you have an `.all.jaam` file, run the visualizer with the following.

    ./bin/jaam visualizer

Then click `File` -> `Load loop graph` and select your
`<application>.all.jaam` file.  After a few seconds you should see the loop
graph in the visualizer.
