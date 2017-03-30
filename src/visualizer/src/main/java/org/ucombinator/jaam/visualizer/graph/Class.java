package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;

public class Class
{
    private String className;
    public ArrayList<Method> methods;

    public Class(String className)
    {
        this.className = className;
        this.methods = new ArrayList<Method>();
    }

    public String getClassName()
    {
        return this.className;
    }

    public ArrayList<Method> getMethods()
    {
        return this.methods;
    }

    public void addMethod(Method method)
    {
        this.methods.add(method);
    }

    // TODO: Finish this
    public void parseJavaFile(String file)
    {
        System.out.println("Parsing Java file for class: " + this.className);
    }
}
