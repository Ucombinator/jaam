#!/usr/bin/env python

import argparse
import subprocess
import sys

def version():
    return "jaam.py, v. 0.1"

def run(rt_jar, targets, classname, main, stdout=None, stderr=None):
    command = "sbt 'run --classpath {rt_jar}:{targets} -c {classname} -m {main}'".format(
        rt_jar      = rt_jar,
        targets     = ':'.join(targets),
        classname   = classname,
        main        = main
    )
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

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-v', '--version', action='store_true')
    parser.add_argument('-J', '--java-rt')
    parser.add_argument('-T', '--target', action='append')
    parser.add_argument('-c', '--classname')
    parser.add_argument('-m', '--main')
    parser.add_argument('-o', '--outfile')
    parser.add_argument('-E', '--error-outfile')
    args = parser.parse_args()

    if args.version:
        print(version())
        sys.exit(0)
    if not args.java_rt:
        raise RuntimeError("No rt.jar file given!")
    if not args.target:
        raise RuntimeError("No target directory or .jar file given!")
    if not args.classname:
        raise RuntimeError("No target class name given!")
    if not args.main:
        raise RuntimeError("No target main method given!")
    run(args.java_rt, args.target, args.classname, args.main, args.outfile, args.error_outfile)
