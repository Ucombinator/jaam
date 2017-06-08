package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;

public class OurClass
{
    private String className;
    private String code;
    public ArrayList<Method> methods;

    public OurClass() {
        this.className = "";
        this.code = "";
        this.methods = new ArrayList<Method>();
    }

    public OurClass(String className, String classCode)
    {
        System.out.println("Creating new class: " + className);
        this.className = className;
        this.code = classCode;
        this.methods = new ArrayList<Method>();
        System.out.println("Finished creating class!");
    }

    public String getClassName()
    {
        return this.className;
    }

    public String getCode() {
        return this.code;
    }

    public ArrayList<Method> getMethods()
    {
        return this.methods;
    }

    public void setName(String name) {
        this.className = name;
    }

    public void addMethod(Method method)
    {
        this.methods.add(method);
    }
}
