#!/usr/bin/env python

import argparse
import subprocess
import os
import sys

SEP     = ';' if os.name == 'nt' else ':'
DIR     = os.path.abspath(os.path.dirname(__file__))
VERBOSE = False

attributes = {
    'version'   : '0.4.0',
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
    command = "{java_opts}sbt 'run --classpath {rt_jar}{sep}{classpaths} -c {classname} -m {main_method}'".format(
        java_opts   = 'JAVA_OPTS="{opts}" '.format(opts=java_opts) if java_opts else '',
        rt_jar      = rt_jar,
        sep         = SEP,
        classpaths  = SEP.join(classpaths),
        classname   = classname,
        main_method = main_method
    )
    handle_command(command, stdout, stderr)

def cfg(rt_jar, app_classpath, classpaths, classname, java_opts=None, stdout=None, stderr=None):
    command = "{java_opts}sbt 'run --cfg {app_classpath} --classpath {rt_jar}{sep}{classpaths} -c {classname}'".format(
        java_opts       = 'JAVA_OPTS="{opts}" '.format(opts=java_opts) if java_opts else '',
        rt_jar          = rt_jar,
        app_classpath   = app_classpath,
        sep             = SEP if classpaths else '',
        classpaths      = SEP.join(classpaths) if classpaths else '',
        classname       = classname
    )
    handle_command(command, stdout, stderr)

def version():
    return "{name}, version {version}\n".format(
        name    = attributes['long_name'],
        version = attributes['version']
    )

def usage(command=None):
    print(version())

    information = {}
    information['run'] = '\n'.join([
        "usage: {name} run [-hv] [-J rt_jar] [-P classpath, -P classpath, ...]",
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
        "        {name} run -J <path-to-rt.jar> -P to-analyze -c Factorial -m main",
        "    You can also run this wrapper from anywhere. If your JAAM directory is",
        "    located at $JAAMDIR, you could do:",
        "        {name} run -J <path-to-rt.jar> -P $JAAMDIR/to-analyze -c Factorial -m main",
        "",
    ]).format(name=attributes['name'])
    information['cfg'] = '\n'.join([
        "usage: {name} cfg [-hv] [-J rt_jar] [-P classpath, -P classpath, ...]",
        "    [-c class] [-m main_method] [-o outfile] [-E stderr_outfile]",
        "",
        "OPTIONS",
        "    -h, --help             Print this help information.",
        "    -v, --version          Print the version information.",
        "    -V, --verbose          Print extra information while running.",
        "    -J, --rt-jar           The path to your 'rt.jar' file.",
        "    -a, --app-classpath    A directory containing the path you want to analyze.",
        "                           This could be a .jar file.",
        "    -P, --classpath        A path you want to analyze. You can specify this",
        "                           option multiple times to add multiple paths. They",
        "                           could be .jar files.",
        "    -c, --classname        The name of the class you want to analyze.",
        "    --java-opts            Extra options passed as:",
        "                           JAVA_OPTS=\"{{java_opts}}\"",
        "    -o, --outfile          Where to redirect stdout (if desired).",
        "    -E, --error-outfile    Where to redirect stderr (if desired).",
        "",
        "EXAMPLES",
        "    From the main JAAM directory, you can do:",
        "        {name} cfg -J <path-to-rt.jar> -a to-cfg -c Factorial",
        "    You can also run this wrapper from anywhere. If your JAAM directory is",
        "    located at $JAAMDIR, you could do:",
        "        {name} run -J <path-to-rt.jar> -a $JAAMDIR/to-cfg -c Factorial",
        "",
    ]).format(name=attributes['name'])

    if command in information:
        print(information[command])
    else:
        print('\n'.join([
            "usage: {name} {{ {usage_subcommands} }} [-hv]",
            "",
            "OPTIONS",
            "    -h, --help             Print this help information.",
            "    -v, --version          Print the version information.",
            "    -V, --verbose          Print extra information while running.",
            "",
            "SUBCOMMANDS",
            "    run        Use the general analyzer and show the graphical output.",
            "    cfg        Use CFG mode.",
            "",
            "For specific information about how to use a subcommands, do:",
            "    {name} {{ subcommand }} --help",
            "",
        ])).format(
            name                = attributes['name'],
            usage_subcommands   = ' | '.join(information.keys())
        )

if __name__ == '__main__':
    if len(sys.argv) < 2:
        usage()
        sys.exit(1)

    if len(sys.argv) == 2 and sys.argv[1] == '--help' or sys.argv[1] == '-h':
        usage()
        sys.exit(0)

    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument('-v', '--version', action='store_true')

    args = parser.parse_known_args()
    if args[0].version:
        print(version())
        sys.exit(0)

    subparsers = parser.add_subparsers(dest='subcommand')

    # Regular analyzer arguments.
    parser_run = subparsers.add_parser('run', add_help=False)
    parser_run.add_argument('-m', '--main-method')

    # CFG-mode analyzer arguments.
    parser_cfg = subparsers.add_parser('cfg', add_help=False)
    parser_cfg.add_argument('-a', '--app-classpath')

    for subparser in [parser_run, parser_cfg]:
        subparser.add_argument('-h', '--help', action='store_true')
        subparser.add_argument('-V', '--verbose', action='store_true')
        subparser.add_argument('-J', '--rt-jar')
        subparser.add_argument('-P', '--classpath', action='append')
        subparser.add_argument('-c', '--classname')
        subparser.add_argument('-o', '--outfile')
        subparser.add_argument('-E', '--error-outfile')
        subparser.add_argument('--java-opts')

    args = parser.parse_args(args[1])

    if args.help:
        usage(command=args.subcommand)
        sys.exit(0)

    VERBOSE = args.verbose
    args.rt_jar = os.path.abspath(args.rt_jar)

    if args.classpath:
        args.classpath  = [os.path.abspath(classpath) for classpath in args.classpath]

    if args.subcommand == 'run':
        if not all([args.rt_jar, args.classpath, args.classname, args.main_method]):
            usage(args.subcommand)
            print("Not all required options given. Need '--rt-jar', '--classpath', '--classname', '--main-method'.")
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
    elif args.subcommand == 'cfg':
        if not all([args.rt_jar, args.app_classpath, args.classname]):
            usage(args.subcommand)
            print("Not all required options given. Need '--rt-jar', '--app-classpath', '--classname'.")
            sys.exit(1)
        cfg(
            rt_jar          = args.rt_jar,
            app_classpath   = args.app_classpath,
            classpaths      = args.classpath,
            classname       = args.classname,
            java_opts       = args.java_opts,
            stdout          = args.outfile,
            stderr          = args.error_outfile
        )
