#!/bin/bash

# Handles execution of JAAM's Analyzer for you!

: ${JAAM_dir:="$(cd $(dirname $(dirname $0)); pwd -P)"}
: ${JAAM_analyzer:="${JAAM_dir}/assembled/jaam-analyzer.jar"}
: ${JAAM_java:="java"}
: ${JAAM_rt_jar:="${JAAM_dir}/resources/rt.jar"}
: ${JAAM_java_opts:=""}

# Use JAVA_OPTS if provided, otherwise use the default from above.
chosen_java_opts=${JAVA_OPTS:-"${JAAM_java_opts}"}
JAVA_OPTS="${chosen_java_opts}"

# Do the execution.
exec "${JAAM_java}" -jar "${JAAM_analyzer}" -J "${JAAM_rt_jar}" "$@"
