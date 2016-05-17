#!/bin/bash

# Bootstrapping script for JAAM project.

NEED_VERSION="1.7"
MY_DIR="$(cd $(dirname $0); pwd -P)"
SBT_REL_PATH="bin/sbt"
SBT_PATH="${MY_DIR}/${SBT_REL_PATH}"
RUNNER_PATH="${MY_DIR}/bin/jaam.sh"
declare -a SUPPORTED_MODES=("user" "developer")
MODES_PROMPT=$(printf " | %s" "${SUPPORTED_MODES[@]}")
MODES_PROMPT="${MODES_PROMPT:3}"

# info
#
# For outputting info.
function info {
    echo -e "[info] $*"
}

# warn
#
# For outputting warning notices. "warn" is written in yellow.
function warn {
    echo -e "[\033[0;33mwarn\033[0m] $*"
}

# error
#
# For outputting errors. "error" is written in red.
function error {
    echo -e "[\033[0;31merror\033[0m] $*"
}

# success
#
# For outputting successes. "success" is written in friendly green.
function success {
    echo -e "[\033[0;32msuccess\033[0m] $*"
}

# interactive_prompt
#
# Arguments:
#  1: Variable to store result in
#  2: Prompt to give user
#  3: Default value
#
# Prompts the user for information, and assigns the response to the variable
# specified upon invocation.
function interactive_prompt {
    read -e -p "$2 [$3]: " choice
    if [[ "$choice" ]]
    then
        eval "$1=\"$choice\""
    else
        eval "$1=\"$3\""
    fi
}

# check_mode
#
# Arguments:
#  1: Mode to check
#
# Returns 1 if the given mode is unsupported.
function check_mode {
    # Convenience assignment.
    mode="$1"
    # Iterate over the supported modes, checking if the given mode is there.
    for (( i = 0; i < ${#SUPPORTED_MODES[@]}; ++i ))
    do
        [ "${SUPPORTED_MODES[$i]}" = "${mode}" ] && return 0
    done
    return 1
}

# check_java
#
# Arguments:
#  1: Java path to check
#
# Returns 1 if the given Java cannot be run or does not execute successfully.
function check_java {
    # Convenience assignment.
    java="$1"
    # Check that Java is installed.
    info "Checking Java is installed ..."
    if ! type -p "${java}" 1>/dev/null 2>&1
    then
        # No Java.
        error "Given Java could not be found"
        return 1
    fi
    # Check Java runs properly
    if "${java}" -version 1>/dev/null 2>&1
    then
        # Success!
        success "Java is installed and runnable"
    else
        # Could not execute.
        error "Given Java could not be executed"
        return 1
    fi
}

# check_java_opts
#
# Arguments:
#  1: Java to use
#  2: All opts (as a single string)
#
# Returns 1 if the Java did not execute with the opts.
function check_java_opts {
    # Convenience assignments.
    java="$1"
    opts="$2"
    # Check if opts are empty. Java doesn't like to run with an empty string.
    if [[ "${opts}" ]]
    then
        # Attempt to run Java.
        "${java}" "${opts}" -version 1>/dev/null 2>&1
        return $?
    fi
}

# check_rt_jar
#
# Arguments:
#  1: Java to use
#  2: rt.jar checker .jar file location
#  3: rt.jar file
#
# Returns:
#  1 if the rt.jar file cannot be found or read
#  2 if the rt.jar file has the wrong version
function check_rt_jar {
    # Convenience assignment.
    java="$1"
    checker="$2"
    rtjar="$3"
    # Get the version.
    rtjar_check_out=$("${java}" -jar "${checker}" "${rtjar}")
    if [ $? -ne 0 ]
    then
        # The checker was unsuccessful in reading the version information.
        error "Bad rt.jar file: ${rtjar}"
        return 1
    else
        # The version information came out okay.
        rtjar_version=$(echo "${rtjar_check_out}" | sed -E 's/([[:digit:]]+\.[[:digit:]]+).*/\1/')
        # Verify version is correct.
        if [[ "${rtjar_version}" != "${NEED_VERSION}" ]]
        then
            warn "Bad rt.jar file version: ${rtjar_version}"
            warn "JAAM recommends rt.jar version 1.7 to run"
            return 2
        else
            success "rt.jar is verified correct version"
        fi
    fi
}

# Get a default Java version.
default_java="$(type -p java)"
if [[ ! "${default_java}" ]]
then
    # There was nothing in the PATH.
    if [[ -n "${JAVA_HOME}" ]] && [[ -x "${JAVA_HOME}/bin/java" ]]
    then
        # But there was something in the JAVA_HOME folder!
        default_java="${JAVA_HOME}/bin/java"
    fi
fi

# Begin interacting with user to generate runner script.
info "Getting default values for JAAM runner ..."
# Get default values.
interactive_prompt jaam_mode "Choose default mode { ${MODES_PROMPT} }" "user"
# Check given mode.
while ! check_mode "${jaam_mode}"
do
    interactive_prompt jaam_mode "Invalid mode given. Choose mode from { ${MODES_PROMPT} }" "user"
done
interactive_prompt jaam_java "Choose default Java" "${default_java}"
# Check given Java.
while ! check_java "${jaam_java}"
do
    interactive_prompt jaam_java "Invalid Java given. Choose new Java" ""
done
default_rt_jar=$("${jaam_java}" -XshowSettings:properties -version 2<&1 | awk '/rt\.jar/ {print $NF}')
interactive_prompt jaam_java_opts "Choose default JAVA_OPTS" ""
# Check given Java opts.
while ! check_java_opts "${jaam_java}" "${jaam_java_opts}"
do
    interactive_prompt jaam_java_opts "Invalid JAVA_OPTS given. Choose again" ""
done
interactive_prompt jaam_rt_jar "Choose default rt.jar" "$default_rt_jar"
interactive_prompt jaam_sbt_opts "Choose default SBT_OPTS (called when invoking \`sbt\`)" "-Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256M"
interactive_prompt jaam_runner "Choose location for jaam.sh runner" "${RUNNER_PATH}"

info "Default mode: ${jaam_mode}"
info "Default Java: ${jaam_java}"
info "Default JAVA_OPTS: ${jaam_java_opts}"
info "Default rt.jar: ${jaam_rt_jar}"
info "Default SBT OPTS: ${jaam_sbt_opts}"
info "JAAM runner path: ${jaam_runner}"

# Generate "developer-mode" sbt executable
info "Generating developer-mode sbt executable at ${SBT_PATH} ..."
cat <<- EOF > "${SBT_PATH}"
	#!/bin/bash
	: \${SBT_JAVA:="${jaam_java}"}
	: \${SBT_OPTS:="${jaam_sbt_opts}"}
	"\${SBT_JAVA}" \${SBT_OPTS} -jar "\$(dirname \$0)/sbt-launch.jar" "\$@"
EOF
rt=$?
if [ $rt -eq 0 ]
then
    success "Generated SBT runner script"
else
    error "Could not generate SBT runner script"
    exit $rt
fi

# Make it executable.
executify="/bin/chmod +x ${SBT_PATH}"
$executify >/dev/null 2>&1
rt=$?
if [ $rt -eq 0 ]
then
    success "$executify"
else
    error "$executify"
    error "To try to solve this problem on your own, execute the following:"
    error "chmod +x ${SBT_PATH}"
fi

# Run SBT to set it up and get Scala (as needed).
info "Bootstrapping SBT/Scala ..."
cd "${MY_DIR}" && "./${SBT_REL_PATH}" assembly

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

# Assemble the rt.jar checker.
info "Assembling rt.jar checker ..."
exec 5>&1 # Open extra file descriptor to redirect output live while capturing.
cd "${MY_DIR}" && rtjarcheck_out=$("./${SBT_REL_PATH}" check_rt_jar/assembly | tee /dev/fd/5)
exec 5>&- # And now close that file descriptor because we like to tidy after ourselves.
regex="${MY_DIR}.*\.jar"
if [[ "$(echo "$rtjarcheck_out" | grep -E 'Packaging|Assembly')" =~ (${regex}) ]]
then
    rtjarcheck_loc="${BASH_REMATCH[1]}"
    check_rt_jar "${jaam_java}" "${rtjarcheck_loc}" "${jaam_rt_jar}"
    rtjarcheck_rt=$?
    while [ $rtjarcheck_rt -ne 0 ]
    do
        if [ $rtjarcheck_rt -eq 1 ]
        then
            # rt.jar file was inaccessible.
            error "rt.jar file was inaccessible."
        elif [ $rtjarcheck_rt -eq 2 ]
        then
            # rt.jar file had incorrect version.
            warn "rt.jar file has wrong version."
            read -p "Continue anyway? [y/N] " -n 1 -r
            if [[ "${REPLY}" ]]
            then
                (>&2 echo)
            fi
            if [[ $REPLY =~ ^[Yy]$ ]]
            then
                break
            fi
        fi
        interactive_prompt jaam_rt_jar "Choose new rt.jar file" "${jaam_rt_jar}"
        check_rt_jar "${jaam_java}" "${rtjarcheck_loc}" "${jaam_rt_jar}"
        rtjarcheck_rt=$?
    done
else
    error "Could not find rt.jar checker."
    exit 1
fi

# Create the script that makes this all worthwhile.
info "Generating JAAM runner script at ${jaam_runner} ..."
cat <<- EOF > "${jaam_runner}"
	#!/bin/bash
	
	# Handles execution of JAAM for you!
	# Generated $(date)
	
	: \${JAAM_mode:="${jaam_mode}"}
	: \${JAAM_dir:="${MY_DIR}"}
	: \${JAAM_jar:="\${JAAM_dir}/jaam.jar"}
	: \${JAAM_java:="${jaam_java}"}
	: \${JAAM_rt_jar:="${jaam_rt_jar}"}
	: \${JAAM_rt_jar_version:="${NEED_VERSION}"}
	: \${JAAM_java_opts:="${jaam_java_opts}"}
	: \${JAAM_sbt:="${SBT_PATH}"}
	: \${JAAM_check_rt_jar:="${rtjarcheck_loc}"}

	# Check rt.jar version.
	if [[ "\${JAAM_check_rt_jar}" ]]
	then
	    version=\$("\${JAAM_java}" -jar "\${JAAM_check_rt_jar}" "\${JAAM_rt_jar}")
	    if [[ "\$version" != "\${JAAM_rt_jar_version}" ]]
	    then
	        echo "Bad rt.jar version: \$version"
	        echo "Continuing..."
	    fi
	else
	    echo "No rt.jar checker."
	    exit 1
	fi
	
	# Use JAVA_OPTS if provided, otherwise use the default from above.
	chosen_java_opts=\${JAVA_OPTS:-"\${JAAM_java_opts}"}
	JAVA_OPTS="\${chosen_java_opts}"

	# Define arguments. If none, call with `--help`.
	user_args="\$@"
	dev_args="\$*"
	if [[ -z "\$*" ]]
	then
	    user_args="--help"
	    dev_args="--help"
	fi
	
	# Do the execution.
	if [[ "\$JAAM_mode" == "user" ]]
	then
	    exec "\${JAAM_java}" -jar "\${JAAM_jar}" -J "\${JAAM_rt_jar}" "\${user_args}"
	elif [[ "\$JAAM_mode" == "developer" ]]
	then
	    SBT_JAVA="\${JAAM_java}"
	    exec "\${JAAM_sbt}" "run -J \${JAAM_rt_jar} \${dev_args}"
	else
	    echo "Error: Invalid mode set: \${JAAM_mode}"
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
