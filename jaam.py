#!/usr/bin/env python

import argparse
import subprocess
from os import name as os_name
from os.path import basename as path_basename
import sys

SEP = ';' if os_name == 'nt' else ':'

attributes = {
    'version'   : '0.2',
    'name'      : path_basename(sys.argv[0]),
    'long_name' : 'JAAM',
}
__version__ = attributes['version']

def run_command(command, stdout, stderr):
    print("Running command:")
    print("    {}".format(command))
    if stdout:
        with open(stdout, 'w') as outfile:
            if stderr:
                with open(stderr, 'w') as errfile:
                    subprocess.call(command, shell=True, stdout=outfile, stderr=errfile)
            else:
                subprocess.call(command, shell=True, stdout=outfile)
    else:
        if stderr:
            with open(stderr, 'w') as errfile:
                subprocess.call(command, shell=True, stderr=errfile)
        else:
            subprocess.call(command, shell=True)

def run(rt_jar, classpaths, classname, main_method, stdout=None, stderr=None):
    command = "sbt 'run --classpath {rt_jar}{sep}{classpaths} -c {classname} -m {main_method}'".format(
        rt_jar      = rt_jar,
        sep         = SEP,
        classpaths  = SEP.join(classpaths),
        classname   = classname,
        main_method = main_method
    )
    run_command(command, stdout, stderr)

def cfg(rt_jar, app_classpath, classpaths, classname, stdout=None, stderr=None):
    command = "sbt 'run --cfg {app_classpath} --classpath {rt_jar}{sep}{classpaths} -c {classname}'".format(
        rt_jar          = rt_jar,
        app_classpath   = app_classpath,
        sep             = SEP,
        classpaths      = SEP.join(classpaths),
        classname       = classname
    )
    run_command(command, stdout, stderr)

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
        "    -h, --help             Print this help information.",
        "    -v, --version          Print the version information.",
        "    -J, --rt-jar           The path to your 'rt.jar' file.",
        "    -P, --classpath        A path you want to analyze. You can specify this",
        "                           option multiple times to add multiple paths. They",
        "                           could be .jar files.",
        "    -c, --classname        The name of the class you want to analyze.",
        "    -m, --main-method      Main method in the class.",
        "    -o, --outfile          Where to redirect stdout (if desired).",
        "    -E, --error-outfile    Where to redirect stderr (if desired).",
        "",
    ]).format(name=attributes['name'])
    information['cfg'] = '\n'.join([
        "usage: {name} cfg [-hv] [-J rt_jar] [-P classpath, -P classpath, ...]",
        "    [-c class] [-m main_method] [-o outfile] [-E stderr_outfile]",
        "",
        "    -h, --help             Print this help information.",
        "    -v, --version          Print the version information.",
        "    -J, --rt-jar           The path to your 'rt.jar' file.",
        "    -a, --app-classpath    A directory containing the path you want to analyze.",
        "                           This could be a .jar file.",
        "    -P, --classpath        A path you want to analyze. You can specify this",
        "                           option multiple times to add multiple paths. They",
        "                           could be .jar files.",
        "    -c, --classname        The name of the class you want to analyze.",
        "    -o, --outfile          Where to redirect stdout (if desired).",
        "    -E, --error-outfile    Where to redirect stderr (if desired).",
        "",
    ]).format(name=attributes['name'])

    if command in information:
        print(information[command])
    else:
        print('\n'.join([
            "usage: {name} {{ run | cfg }}",
            "",
            "    -h, --help             Print this help information.",
            "    -v, --version          Print the version information.",
            "",
        ])).format(name=attributes['name'])

if __name__ == '__main__':
    if len(sys.argv) < 2:
        usage()
        sys.exit(1)

    if len(sys.argv) == 2 and sys.argv[1] == '--help' or sys.argv[1] == '-h':
        usage()
        sys.exit(0)

    parser = argparse.ArgumentParser(add_help=False,)
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
        subparser.add_argument('-J', '--rt-jar')
        subparser.add_argument('-P', '--classpath', action='append')
        subparser.add_argument('-c', '--classname')
        subparser.add_argument('-o', '--outfile')
        subparser.add_argument('-E', '--error-outfile')

    args = parser.parse_args(args[1])

    if args.help:
        usage(command=args.subcommand)
        sys.exit(0)

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
            stdout      = args.outfile,
            stderr      = args.error_outfile
        )
    elif args.subcommand == 'cfg':
        if not all([args.rt_jar, args.app_classpath, args.classpath, args.classname]):
            usage(args.subcommand)
            print("Not all required options given. Need '--rt-jar', '--app-classpath', '--classpath', '--classname'.")
            sys.exit(1)
        cfg(
            rt_jar          = args.rt_jar,
            app_classpath   = args.app_classpath,
            classpths       = args.classpath,
            classname       = args.classname,
            stdout          = args.outfile,
            stderr          = args.error_outfile
        )
