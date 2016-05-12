#!/bin/bash

# Bootstrapping script for JAAM project.

NEED_VERSION="1.7"
MY_DIR="$(cd $(dirname $0); pwd -P)"
SBT_REL_PATH="bin/sbt"
SBT_PATH="${MY_DIR}/${SBT_REL_PATH}"
RUNNER_PATH="${MY_DIR}/jaam.sh"

function info {
    echo -e "[info] $*"
}

function warn {
    echo -e "\[033[0;33mwarn\033[0m] $*"
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
    read -e -p "$2 [$3]: " choice
    if [[ "$choice" ]]
    then
        eval "$1=\"$choice\""
    else
        eval "$1=\"$3\""
    fi
}

if type -p java 1>/dev/null 2>&1
then
    # There is a Java at `java`.
    default_java="$(type -p java)"
elif [[ -n "${JAVA_HOME}" ]] && [[ -x "${JAVA_HOME}/bin/java" ]]
then
    # Java is installed in a directory not on the path.
    default_java="${JAVA_HOME}/bin/java"
else
    # No Java. Let the user figure it out.
    default_java=""
fi

# Begin interacting with user to generate runner script.
info "Getting default values for JAAM runner"
# Get default values.
(>&2 echo)
interactive_prompt jaam_mode "Choose default mode ('user' or 'developer')" "user"
interactive_prompt jaam_java "Choose default Java" "${default_java}"
default_rt_jar=$("${jaam_java}" -XshowSettings:properties -version 2<&1 | awk '/rt\.jar/ {print $NF}')
interactive_prompt jaam_rt_jar "Choose default rt.jar" "$default_rt_jar"
interactive_prompt jaam_java_opts "Choose default JAVA_OPTS" ""
interactive_prompt jaam_sbt_opts "Choose default SBT_OPTS (called when invoking \`sbt\`)" "-Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256M"
interactive_prompt jaam_runner "Choose location for jaam.sh runner" "${RUNNER_PATH}"
(>&2 echo)

info "Default mode: ${jaam_mode}"
info "Default Java: ${jaam_java}"
info "Default rt.jar: ${jaam_rt_jar}"
info "Default JAVA_OPTS: ${jaam_java_opts}"
info "Default SBT OPTS: ${jaam_sbt_opts}"
info "JAAM runner path: ${jaam_runner}"

# Check that Java is installed.
info "Checking Java is installed"
if type -p "${jaam_java}" 1>/dev/null 2>&1
then
    # Java is installed at `java`.
    _java="$(type -p ${jaam_java})"
else
    # No Java.
    error "No Java could be found."
    bad_java_prompt
fi
success "Java is installed"

# Generate "developer-mode" sbt executable
info "Generating developer-mode sbt executable at ${SBT_PATH}"
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
info "Bootstrapping SBT/Scala"
cd "${MY_DIR}" && "./${SBT_REL_PATH}" about

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
info "Checking rt.jar version"
exec 5>&1 # Open extra file descriptor to redirect output live while capturing.
cd "${MY_DIR}" && rtjarcheck_out=$("./${SBT_REL_PATH}" check_rt_jar/assembly | tee /dev/fd/5)
exec 5>&- # And now close that file descriptor because we like to tidy after ourselves.
regex="${MY_DIR}.*\.jar"
if [[ "$(echo "$rtjarcheck_out" | grep -E 'Packaging|Assembly')" =~ (${regex}) ]]
then
    rtjarcheck_loc="${BASH_REMATCH[1]}"
    rtjar_version=$("${jaam_java}" -jar "${rtjarcheck_loc}" "${jaam_rt_jar}" | sed -E 's/([[:digit:]]+\.[[:digit:]]+).*/\1/')
    if [ $? -ne 0 ]
    then
        error "Bad rt.jar file: ${jaam_rt_jar}"
        jaam_rt_jar=""
    else
        # Verify version is correct.
        if [[ "${rtjar_version}" != "${NEED_VERSION}" ]]
        then
            warn "Bad rt.jar file version: ${rtjar_version}"
            warn "JAAM recommends rt.jar version 1.7 to run"
        else
            success "rt.jar is verified correct version"
        fi
    fi
else
    error "Could not find rt.jar checker."
    exit 1
fi

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
	: \${JAAM_rt_jar_version:="${NEED_VERSION}"}
	: \${JAAM_java_opts:="${jaam_java_opts}"}
	: \${JAAM_sbt:="${jaam_sbt}"}
	: \${JAAM_check_rt_jar:="${rtjarcheck_loc}"}

	# Check rt.jar version.
	if [[ "\${JAAM_check_rt_jar}" ]]
	then
	    version=$("\${JAAM_check_rt_jar}" "\${JAAM_rt_jar}")
	    if [[ "\$version" != "\${JAAM_rt_jar_version}" ]]
	    then
	        echo "Bad rt.jar version: \$version"
	        echo "Continuing..."
	    fi
	else
	    echo "No rt.jar checker."
	    exit 1
	fi
	
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
	    exec "SBT_JAVA=\"\${JAAM_java}\" \${execute_java_opts}\${JAAM_sbt}" "run \$*"
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
