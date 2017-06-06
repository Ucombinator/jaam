package org.ucombinator.jaam.agent;

import java.lang.instrument.Instrumentation;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.jar.*;
import java.util.regex.*;

// !!! WARNING !!!
// Reflection filters out certain fields so we have to put them back in.
// See <https://stackoverflow.com/questions/17796815/protecting-fields-from-reflection-the-strange-case-of-the-system-security>.
// The code that does this is in the `ReflectionFix` class.
// If we move to a new version of Java, we may have to update
// manually update the code for any newly needed items.
// These can be found by:
//  - looking in sun.reflection.Reflection.<clinit>
//  - looking for calls to registerFieldsToFilter

// NOTE: This code uses IdentityHashMap instead of HashMap in some places to
// avoid recursion in `hashCode()`

// TODO: output literal string constants when possible
// TODO: array
// TODO: inner class
// TODO: The Trampoline class is duplicated in output for some reason
// TODO: could this be implemented using ASM?  Would that allow us to get past the REFLECTION_FIX problems?

// TODO: ensure not loading extra classes due to being in the same jar as everything else in jaam

public class Main {
  // Use javaagent to get an Instrumentation
  private static Instrumentation instrumentation;
  public static void premain(String agentArgs, Instrumentation inst) {
    Main.instrumentation = inst;
  }

  private static void usage() {
    System.err.println("Usage: java -javaagent:jaam-agent.jar [-classpath <other-jars>] -jar jaam-agent.jar [<rules-file>]");
    System.err.println();
    System.err.println("  [-classpath JARS] is optional.  If present those jar files will also be scanned");
    System.err.println("  [RULES] is optional.  If present it is a file containing lines");
    System.err.println("  empty line or line starting with # is comment");
    System.err.println("  line starting with + is include rule");
    System.err.println("  line starting with - is exclude rule");
    System.err.println("  part of line after + or - is Java regex");
    System.err.println("  the first line to match a class name determines whether that class will be included");
    System.exit(1);
  }

  public static void main(String[] args) {
    // Argument parsing
    if (args.length == 0) {
      // do nothing
    } else if (args.length == 1) {
      Rule.read(args[0]);
    } else {
      System.err.println("Too many arguments (expected zero or one): "+args.length+" arguments");
      for (String arg : args) {
        System.err.println("  arg: "+arg);
      }
      usage();
    }

    // Check initialization
    if (instrumentation == null) {
      System.err.println("missing instrumention (missing javaagent option?)");
      usage();
    }

    // Read class names from the jars in our class paths
    final String pathSep = Pattern.quote(File.pathSeparator);
    Jar.readJars(System.getProperty("sun.boot.class.path").split(pathSep));
    Jar.readDirs(System.getProperty("java.ext.dirs").split(pathSep));
    Jar.readJars(System.getProperty("java.class.path").split(pathSep));

    // Trigger static initialization of the named classes
    final HashSet<String> failedClasses = new HashSet<>();
    for (String className : Jar.classNames) {
      try {
        Class.forName(className);
      } catch (Throwable e) {
        failedClasses.add(className);
        System.err.println("Error loading class: "+className);
        e.printStackTrace();
      }
    }

    // Print object graph
    for (Class<?> c : Main.instrumentation.getAllLoadedClasses()) {
      if (Rule.include(c.getName())) {
        if (failedClasses.contains(c.getName())) {
          // Avoid uninitialized classes.  I'm not sure how they get into
          // the list of loaded classes, but they do.
          System.out.println("\nskipping failed class: class="+c.getName());
        } else {
          printClass(c);
        }
      }
    }

    System.out.println("done: object count="+Id.getCounter());
  }

  // Prints all the objects in the static fields
  private static void printClass(Class<?> c) {
    Field[] fields;
    try {
      fields = c.getDeclaredFields();
    } catch (NoClassDefFoundError e) {
      // I'm not sure why this can happen, but it can so skip the class
      System.out.println("\nskipping not found class: class="+c.getName());
      return;
    }

    System.out.println("\nstatic fields: class="+c.getName());
    for (Field field : fields) {
      if (isStatic(field)) {
        Object o = getField(field, null/*static*/); // TODO: naming consistency with printObject.o2 (parent/object?)
        System.out.println("static field: class="+c.getName()+"\tfield="+field.getName()+"\ttype="+typeName(field.getType())+"\tvalue="+Id.id(o));
        if (o != null) { // TODO: fold into single argument version of printObject
          printObject(o, o.getClass());
        }
      }
    }

    ReflectionFix.printClass(c);
  }

  private static void printObject(Object o, Class<?> c) {
    if (Done.isDone(c,o)) {
      // Do nothing
    } else {
      Done.setDone(c,o);
      if (c.isArray()) {
        System.out.println("array elements: array="+Id.id(o)+"\ttype="+typeName(c)+"\tlength="+Array.getLength(o));
        boolean printedNull = false;
        for (int i = 0; i < Array.getLength(o); i++) {
          Object o2 = getElement(c, o, i);
          if (!printedNull || o2 != null) {
            System.out.println("array element: array="+Id.id(o)+"\ttype="+typeName(c)+"\tindex="+i+"\telement="+Id.id(o2));
            printedNull = true;
          }
          if (o2 != null) {
            printObject(o2, o2.getClass());
          }
        }
      } else {
        if (c.getSuperclass() != null) { printObject(o, c.getSuperclass()); }
        System.out.println("instance fields: object="+Id.id(o)+"\ttype="+typeName(c));
        for (Field field : c.getDeclaredFields()) {
          if (!isStatic(field)) {
            Object o2 = getField(field, o);
            System.out.println("instance field: class="+typeName(c)+"\tobject="+Id.id(o)+"\tfield="+field.getName()+"\ttype="+typeName(field.getType())+"\tvalue="+Id.id(o2));
            if (o2 != null) {
              printObject(o2, o2.getClass());
            }
          }
        }

        ReflectionFix.printObject(o, c);
      }
    }
  }

  // TODO: Move to a `Util` class
  public static String typeName(Class<?> c) {
    return Array.newInstance(c,0).getClass().getName().substring(1);
  }

  private static boolean isStatic(Field f) {
    return (f.getModifiers() & Modifier.STATIC) != 0;
  }

  private static Object getElement(Class<?> c, Object o, int i) {
    if (c.getComponentType().isPrimitive()) {
      return null;
    } else {
      return Array.get(o, i);
    }
  }
  public static Object getField(Field f, Object o) {
    if (f.getType().isPrimitive()) { return null; }
    else {
      f.setAccessible(true);
      try {
        return f.get(o);
      } catch (Throwable e) {
        throw new RuntimeException("Error getting field of class: object("+o+") field("+f+")", e);
      }
    }
  }
}

@SuppressWarnings("sunapi")
class ReflectionFix {
  public static void printClass(Class<?> c) {
    if (c == java.lang.System.class) {
      // java.lang.System.security
      Object o = java.lang.System.getSecurityManager();
      System.out.println("static field: class="+c.getName()+"\tfield="+"security"+"\ttype="+"Ljava.lang.SecurityManager;"+"\tvalue="+Id.id(o));
      if (o != null) {
        printObject(o, o.getClass());
      }
    } else if (c == sun.reflect.Reflection.class) {
      // There is no way to retrieve the value of these, so we have to
      // manually reconstruct them.  Values are added by the static
      // initializer of Reflection or calls to registerFieldsToFilter or
      // registerMethodsToFilter.
      {
        // sun.reflect.Reflection.fieldFilterMap
        Object o = fieldFilterMap;
        System.out.println("static field: class="+c.getName()+"\tfield="+"fieldFilterMap"+"\ttype="+"Ljava.util.Map;"+"\tvalue="+Id.id(o));
        if (o != null) {
          printObject(o, o.getClass());
        }
      }
      {
        // sun.reflect.Reflection.methodFilterMap
        Object o = methodFilterMap;
        System.out.println("static field: class="+c.getName()+"\tfield="+"methodFilterMap"+"\ttype="+"Ljava.util.Map;"+"\tvalue="+Id.id(o));
        if (o != null) {
          printObject(o, o.getClass());
        }
      }
    }
  }

  public static void printObject(Object o, Class<?> c) {
    if (c == java.lang.Class.class) {
      // java.lang.Class.classLoader
      Object o2 = ((java.lang.Class)o).getClassLoader();
      System.out.println("instance field: class="+Main.typeName(c)+"\tobject="+Id.id(o)+"\tfield="+"classLoader"+"\ttype="+"Ljava.lang.ClassLoader;"+"\tvalue="+Id.id(o2));
      if (o2 != null) { // TODO: fold into single argument version of printObject
        printObject(o2, o2.getClass());
      }
    } else if (c == UnsafeStaticFieldAccessorImpl_class) {
      // sun.reflect.UnsafeStaticFieldAccessorImpl.base
      java.lang.reflect.Field f; // TODO: factor f and u out
      try {
        f = (java.lang.reflect.Field)Main.getField(c.getSuperclass().getDeclaredField("field"), o);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException("Couldn't get field sun.reflect.UnsafeStaticField.AccessorImpl.super.field", e);
      }
      sun.misc.Unsafe u;
      try {
        u = (sun.misc.Unsafe)Main.getField(sun.misc.Unsafe.class.getDeclaredField("theUnsafe"), null);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException("Couldn't get field sun.misc.Unsafe.theUnsafe", e);
      }
      Object o2 = u.staticFieldBase(f);
      System.out.println("instance field: class="+Main.typeName(c)+"\tobject="+Id.id(o)+"\tfield="+"base"+"\ttype="+"Ljava.lang.Object;"+"\tvalue="+Id.id(o2));
      if (o2 != null) { // TODO: fold into single argument version of printObject
        printObject(o2, o2.getClass());
      }
    } else if (c == sun.reflect.ConstantPool.class) {
      throw new RuntimeException("printObject() not tested on sun.reflect.ConstantPool");
      // sun.reflect.ConstantPool.constantPoolOop
      /* NOTE: This implementation is untested due to no ConstantPool objects being reachable from static fields:
         Object o2 = null; // no way to access, but is only used as argument to native methods so we can use <null> for it

         System.out.println("instance field: class="+Main.typeName(c)+"\tobject="+Id.id(o)+"\tfield="+"constantPoolOop"+"\ttype="+"Ljava.lang.Object;"+"\tvalue="+Id.id(o2));
         if (o2 != null) { // TODO: fold into single argument version of printObject
         printObject(o2, o2.getClass());
         }
      */
    }
  }

  // NOTE: needed because sun.reflect.UnsafeStaticFieldAccessorImpl is not a public class
  private final static Class<?> UnsafeStaticFieldAccessorImpl_class;
  static {
    try { // TODO: version of forName, etc that throws runtime exception
      UnsafeStaticFieldAccessorImpl_class = Class.forName("sun.reflect.UnsafeStaticFieldAccessorImpl");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Couldn't get java.lang.Class object for sun.reflect.UnsafeStaticFieldAccessorImpl", e);
    }
  }

  private final static Map<Class<?>,String[]> fieldFilterMap = new HashMap<Class<?>,String[]>();
  static {
    fieldFilterMap.put(sun.reflect.Reflection.class,
          new String[] {"fieldFilterMap", "methodFilterMap"});
    fieldFilterMap.put(java.lang.System.class,
          new String[] {"security"});
    fieldFilterMap.put(UnsafeStaticFieldAccessorImpl_class,
          new String[] {"base"});
    fieldFilterMap.put(sun.reflect.ConstantPool.class,
          new String[] {"constantPoolOop"});
  }

  private static Map<Class<?>,String[]> methodFilterMap = new HashMap<Class<?>,String[]>();
  static {
    methodFilterMap.put(sun.misc.Unsafe.class,
            new String[] {"getUnsafe"});
  }
}

// Rules for what to include and what not to
class Rule {
  public Rule(boolean include, Pattern pattern) {
    this.include = include;
    this.pattern = pattern;
  }
  public final boolean include;
  public final Pattern pattern;

  private static final ArrayList<Rule> rules = new ArrayList<>();
  private static final String ownPackage = Main.class.getPackage().getName() + "."; // The package for this class (which we should skip by default)

  public static boolean include(String s) {
    for (Rule rule : rules) {
      if (rule.pattern.matcher(s).matches()) {
        return rule.include;
      }
    }

    if (s.startsWith(ownPackage)) { return false; }
    return true;
  }

  public static void read(String file) {
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.length() == 0) {
          // comment do nothing
        } else if (line.charAt(0) == '#') {
          // comment do nothing
        } else if (line.charAt(0) == '+') {
          rules.add(new Rule(true, Pattern.compile(line.substring(1))));
        } else if (line.charAt(0) == '-') {
          rules.add(new Rule(false, Pattern.compile(line.substring(1))));
        } else {
          throw new RuntimeException("Invalid rule: "+line);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error reading file: "+file, e);
    }
  }
}

// Keep track of what objects we have already printed
class Done {
  private static final class Unit {}
  private static final Unit unit = new Unit();
  private static final HashMap<Class<?>,IdentityHashMap<Object,Unit>> objects = new HashMap<>();

  private static void addClass(Class<?> c) {
    if (objects.get(c) == null) { objects.put(c, new IdentityHashMap<>()); }
  }

  public static boolean isDone(Class<?> c, Object o) {
    addClass(c);
    return objects.get(c).get(o) != null;
  }

  public static void setDone(Class<?> c, Object o) {
    addClass(c);
    objects.get(c).put(o, unit);
  }
}

// Generate unique ids for each object
class Id {
  private static final  IdentityHashMap<Object, String> ids = new IdentityHashMap<>();
  private static long counter = 0;

  public static long getCounter() { return counter; }

  public static String id(Object o) {
    String id = ids.get(o);
    if (o == null) { return "<null>"; }
    else if (id != null) {
      return id;
    } else {
      id = String.valueOf(counter++);
      ids.put(o, id);
      return id;
    }
  }
}

// Read class names from jar files
class Jar {
  public static final ArrayList<String> classNames = new ArrayList<>();

  public static void readDirs(String[] dirs) {
    for (String dir : dirs) { readDir(dir); }
  }

  public static void readDir(String dir) {
    System.out.println("reading dir: dir="+dir);
    File[] files = new File(dir).listFiles();
    if (files != null) {
      for (File f : files) {
        readJar(f);
      }
    }
  }

  public static void readJars(String[] files) {
    for (String file : files) { readJar(new File(file)); }
  }

  public static void readJar(File file) {
    if (!file.isFile() || !file.getName().endsWith(".jar")) {
      System.out.println("skipping jar: file="+file);
    } else {
      System.out.println("reading jar: file="+file);
      try {
        JarFile jar = new JarFile(file);

        if (jar.getManifest() != null) {
          Attributes attr = jar.getManifest().getMainAttributes();
          if (attr.getValue(Attributes.Name.CLASS_PATH) != null) {
            // TODO: load `Class-Path` instead of just warning.
            // TODO: it may be ignored in `extensions`
            // TODO: path is relative to what?  the jar containing it?
            // TODO: uses spaces to separate, but what about spaces in paths?
            System.out.println("classpath in jar manifest: jar="+file+"\tclasspath="+attr.getValue(Attributes.Name.CLASS_PATH));
          }
        }

        for (JarEntry e : java.util.Collections.list(jar.entries())) {
          if (e.getName().endsWith(".class")) {
            // TODO: static constant for "."?
            String name = e.getName().replace(File.separator,".").replaceAll(".class$", "");
            if (Rule.include(name)) {
              System.out.println("included class: name="+name);
              classNames.add(name);
            } else {
              System.out.println("excluded class: name="+name);
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
