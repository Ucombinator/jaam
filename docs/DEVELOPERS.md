# JAAM for Developers

This document is for helping people who want to help contribute to JAAM. This guide will help you get up to speed and
ensure a proper development environment for working on the project.

## Disclaimer

JAAM is very much in its early stages of development. It's actively being worked on, so the edges are rough and there
are plenty of bugs.

## Requirements

* [Java 1.7 Development Kit](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
  - Java 1.8 is unsupported
* [SBT](http://www.scala-sbt.org/)
  - Installed by bundled SBT installer script
* [Scala](http://www.scala-lang.org/)
  - Installed by SBT during your first run
* [Python 2](https://www.python.org/downloads/)
  - For the `jaam.py` wrapper script

You have to install Java (and Python if you want it) by yourself, unfortunately. You may already have it installed, but
if not just follow the link(s) above.

To install SBT and Scala, simply execute from the JAAM root directory `./bin/sbt compile`. This will run the bundled
SBT script, which will automatically install both SBT and Scala as needed.