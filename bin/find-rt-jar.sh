#!/bin/bash

PROPS=$(java -XshowSettings:properties -version 2>&1)

VERSION=$(echo "$PROPS" | awk '/java\.version/ {print $NF}')

case $VERSION in
    1.7.*) ;;
    *) echo "WARNING: expected Java version 1.7 but found '$VERSION'." 1>&2
esac

echo "$PROPS" | awk '/rt\.jar/ {print $NF}'
