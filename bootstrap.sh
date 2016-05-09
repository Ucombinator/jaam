#!/bin/bash

# Bootstrapping script for JAAM project.

NEED_VERSION="1.7"

function info {
    echo -e "[info] $*"
}

function error {
    echo -e "[\033[0;31merror\033[0m] $*"
}

function success {
    echo -e "[\033[0;32msuccess\033[0m] $*"
}

function bad_java_prompt {
    error "Download Java Development Kit ${NEED_VERSION} from:"
    error "    http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html"
    exit 1
}

# Check that Java is installed.
info "Checking Java is installed"
if type -p java 1>/dev/null 2>&1
then
    # Java is installed at `java`.
    _java=java
elif [[ -n "${JAVA_HOME}" ]] && [[ -x "${JAVA_HOME}/bin/java" ]]
then
    # Java is installed in a directory not on the path.
    _java="${JAVA_HOME}/bin/java"
else
    # No Java.
    error "No Java could be found."
    bad_java_prompt
fi

# Check that the Java version is correct.
info "Checking Java version is ${NEED_VERSION}"
version=$("${_java}" -version 2>&1 | awk -F '"' '/version/ {print $2}' | sed -E 's/([[:digit:]]+\.[[:digit:]]+).*/\1/')
if [[ "${version}" != "${NEED_VERSION}" ]]
then
    error "Improper version of Java found: ${version}"
    bad_java_prompt
fi
success "Java is installed and correct version"

# Run SBT to set it up and get Scala (as needed).
info "Bootstrapping SBT/Scala and compiling project"
bin/sbt compile
rt=$?
if [ $rt -eq 0 ]
then
    success "Bootstrapping complete"
else
    error "Bootstrapping failed"
    exit $rt
fi
