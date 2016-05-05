#!/usr/bin/env python

import argparse
import subprocess
import os
import sys

SEP     = ';' if os.name == 'nt' else ':'
DIR     = os.path.abspath(os.path.dirname(__file__))
BIN     = os.path.join(DIR, 'bin')
SBT     = os.path.join(BIN, 'sbt')
FIND_RT = os.path.join(BIN, 'find-rt-jar.sh')
VERBOSE = False

attributes = {
    'version'   : '0.5.0',
    'name'      : os.path.basename(sys.argv[0]),
    'long_name' : 'JAAM',
}
__version__ = attributes['version']

def run_command(command, outfile, errfile):
    subprocess.call(command, shell=True, cwd=DIR, stdout=outfile, stderr=errfile)

def handle_command(command, stdout, stderr):
    if VERBOSE:
        print("Running command from '{}':".format(DIR))
        print("    {}".format(command))
    if stdout:
        with open(stdout, 'w') as outfile:
            if stderr:
                with open(stderr, 'w') as errfile:
                    run_command(command, outfile, errfile)
            else:
                run_command(command, outfile, sys.stderr)
    else:
        if stderr:
            with open(stderr, 'w') as errfile:
                run_command(command, sys.stdout, errfile)
        else:
            run_command(command, sys.stdout, sys.stderr)

def run(rt_jar, classpaths, classname, main_method, java_opts=None, stdout=None, stderr=None):
    command = "{java_opts}{sbt} 'run --classpath {rt_jar}{sep}{classpaths} -c {classname} -m {main_method}'".format(
        java_opts   = 'JAVA_OPTS="{opts}" '.format(opts=java_opts) if java_opts else '',
        sbt         = SBT,
        rt_jar      = rt_jar,
        sep         = SEP,
        classpaths  = SEP.join(classpaths),
        classname   = classname,
        main_method = main_method
    )
    handle_command(command, stdout, stderr)

def version():
    return "{name}, version {version}\n".format(
        name    = attributes['long_name'],
        version = attributes['version']
    )

def usage(command=None):
    print(version())
    print('\n'.join([
        "usage: {name} [-hv] [-J rt_jar] [-P classpath, -P classpath, ...]",
        "    [-c class] [-m main_method] [-o outfile] [-E stderr_outfile]",
        "",
        "OPTIONS",
        "    -h, --help             Print this help information.",
        "    -v, --version          Print the version information.",
        "    -V, --verbose          Print extra information while running.",
        "    -J, --rt-jar           The path to your 'rt.jar' file.",
        "    -P, --classpath        A path you want to analyze. You can specify this",
        "                           option multiple times to add multiple paths. They",
        "                           could be .jar files.",
        "    -c, --classname        The name of the class you want to analyze.",
        "    -m, --main-method      Main method in the class.",
        "    --java-opts            Extra options passed as:",
        "                           JAVA_OPTS=\"{{java_opts}}\"",
        "    -o, --outfile          Where to redirect stdout (if desired).",
        "    -E, --error-outfile    Where to redirect stderr (if desired).",
        "",
        "EXAMPLES",
        "    From the main JAAM directory, you can do:",
        "        {name} -J <path-to-rt.jar> -P to-analyze -c Factorial -m main",
        "    You can also run this wrapper from anywhere. If your JAAM directory is",
        "    located at $JAAMDIR, you could do:",
        "        {name} -J <path-to-rt.jar> -P $JAAMDIR/to-analyze -c Factorial -m main",
        "",
        "NOTE ON rt.jar",
        "    The Java runtime's `rt.jar` file is imperative to the function of this",
        "    program. You can provide a path to it directly via the `--rt-jar` option",
        "    or else the local `bin/find-rt-jar.sh` will be used to attempt to",
        "    automatically find it.",
        "",
    ]).format(name=attributes['name']))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument('-h', '--help', action='store_true')
    parser.add_argument('-v', '--version', action='store_true')
    parser.add_argument('-V', '--verbose', action='store_true')
    parser.add_argument('-J', '--rt-jar')
    parser.add_argument('-P', '--classpath', action='append')
    parser.add_argument('-c', '--classname')
    parser.add_argument('-m', '--main-method')
    parser.add_argument('--java-opts')
    parser.add_argument('-o', '--outfile')
    parser.add_argument('-E', '--error-outfile')

    args = parser.parse_args()

    if args.help:
        usage()
        sys.exit(0)
    if args.version:
        print(version())
        sys.exit(0)

    VERBOSE = args.verbose

    if args.rt_jar is None:
        try:
            args.rt_jar = os.path.abspath(subprocess.check_output(FIND_RT, shell=True).split()[0])
            if VERBOSE:
                print("Using rt.jar: {}".format(args.rt_jar))
        except:
            print("No rt.jar file given.")
            sys.exit(1)
    else:
        args.rt_jar = os.path.abspath(args.rt_jar)

    if args.classpath:
        args.classpath  = [os.path.abspath(classpath) for classpath in args.classpath]

    if not all([args.classpath, args.classname, args.main_method]):
        usage()
        print("Not all required options given. Need '--classpath', '--classname', '--main-method'.")
        sys.exit(1)
    run(
        rt_jar      = args.rt_jar,
        classpaths  = args.classpath,
        classname   = args.classname,
        main_method = args.main_method,
        java_opts   = args.java_opts,
        stdout      = args.outfile,
        stderr      = args.error_outfile
    )
