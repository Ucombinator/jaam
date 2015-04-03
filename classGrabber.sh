#!/bin/bash

set -x
set -e

SOURCEJAR=$(scala -e 'import scala.collection.JavaConversions._; println(System.getProperty("sun.boot.class.path").split(":").filter(_.endsWith("rt.jar")).head)')

DESTDIR=javacache

if [ ! -e "$DESTDIR" ]; then
  mkdir "$DESTDIR"
else
  echo "Error: $DESTDIR exists." >&2
  exit 1
fi

unzip "$SOURCEJAR" -d "$DESTDIR"
