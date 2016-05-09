#!/bin/bash

# Bootstrapping script for JAAM project.

NEED_VERSION="1.7"
MY_DIR="$(cd $(dirname $0); pwd -P)"
SBT_PATH="${MY_DIR}/bin/sbt"
SBT_EXEC="./bin/sbt"
FIND_RT_JAR="${MY_DIR}/bin/find-rt-jar.sh"
RUNNER_PATH="${MY_DIR}/jaam.sh"

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

function interactive_prompt {
    (>&2 echo -n "$2 [$3]: ")
    read choice
    if [[ "$choice" ]]
    then
        eval "$1=\"$choice\""
    else
        eval "$1=\"$3\""
    fi
}

# Check that Java is installed.
info "Checking Java is installed"
if type -p java 1>/dev/null 2>&1
then
    # Java is installed at `java`.
    _java="$(type -p java)"
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
success "Java is installed and is the correct version"

# Run SBT to set it up and get Scala (as needed).
info "Bootstrapping SBT/Scala and compiling project"
cd "${MY_DIR}" && "${SBT_EXEC}" about

# Check SBT bootstrapping ran successfully.
rt=$?
if [ $rt -eq 0 ]
then
    # Yep.
    success "SBT Bootstrapping complete"
else
    # Nope.
    error "SBT Bootstrapping failed"
    exit $rt
fi

# Begin interacting with user to generate runner script.
info "Getting default values for JAAM runner"
# Get default values.
(>&2 echo)
interactive_prompt jaam_mode "Choose default mode ('user' or 'developer')" "user"
interactive_prompt jaam_java "Choose default Java" "$_java"
interactive_prompt jaam_rt_jar "Choose default rt.jar" "$($FIND_RT_JAR)"
interactive_prompt jaam_java_opts "Choose default JAVA_OPTS" ""
interactive_prompt jaam_sbt "Choose default SBT" "${SBT_PATH}"
interactive_prompt jaam_runner "Choose location for jaam.sh runner" "${RUNNER_PATH}"
(>&2 echo)

info "Default mode: ${jaam_mode}"
info "Default Java: ${jaam_java}"
info "Default rt.jar: ${jaam_rt_jar}"
info "Default JAVA_OPTS: ${jaam_java_opts}"
info "Default SBT: ${jaam_sbt}"
info "JAAM runner path: ${jaam_runner}"

info "Generating JAAM runner script at ${jaam_runner}"
cat <<- EOF > "${jaam_runner}"
	#!/bin/bash
	
	# Handles execution of JAAM for you!
	# Generated $(date)
	
	: \${JAAM_mode:="${jaam_mode}"}
	: \${JAAM_dir:="${MY_DIR}"}
	: \${JAAM_jar:="\${JAAM_dir}/jaam.jar"}
	: \${JAAM_java:="${jaam_java}"}
	: \${JAAM_rt_jar:="${jaam_rt_jar}"}
	: \${JAAM_java_opts:="${jaam_java_opts}"}
	: \${JAAM_sbt:="${jaam_sbt}"}
	
	# Check for JAVA_OPTS (don't want to override it).
	if [[ "\$JAVA_OPTS" ]]
	then
	    execute_java_opts="JAVA_OPTS='\${JAVA_OPTS}' "
	elif [[ "\$JAAM_java_opts" ]]
	then
	    execute_java_opts="JAVA_OPTS='\${JAAM_java_opts}' "
	else
	    execute_java_opts=""
	fi
	
	# Do the execution.
	if [[ "\$JAAM_mode" == "user" ]]
	then
	    exec "\${execute_java_opts}\${JAAM_java}" -jar "\${JAAM_jar}" "\$@"
	elif [[ "\$JAAM_mode" == "developer" ]]
	then
	    exec "\${execute_java_opts}\${JAAM_sbt}" "run \$*"
	else
	    echo "Error: Invalid mode set: \$JAAM_mode"
	    exit 1
	fi
EOF
rt=$?
if [ $rt -eq 0 ]
then
    success "Generated JAAM runner script"
else
    error "Could not generate JAAM runner script"
    exit $rt
fi

# Make it executable.
executify="/bin/chmod +x ${jaam_runner}"
$executify >/dev/null 2>&1
rt=$?
if [ $rt -eq 0 ]
then
    success "$executify"
else
    error "$executify"
    error "To try to solve this problem on your own, execute the following:"
    error "chmod +x ${jaam_runner}"
fi
