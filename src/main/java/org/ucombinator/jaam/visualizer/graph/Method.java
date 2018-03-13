package org.ucombinator.jaam.visualizer.graph;

import org.ucombinator.jaam.visualizer.layout.StateVertex;

import java.util.ArrayList;

public class Method
{
    private final String methodName;
    private OurClass our_class;
    private ArrayList<StateVertex> vertices;
    private ArrayList<Instruction> instructionList;

    public Method() {
        this.our_class = new OurClass();
        this.methodName = "Dummy_method";
        vertices = new ArrayList<>();
        instructionList = new ArrayList<>();
    }

    public Method(String methodName) {
        this.our_class = new OurClass();
        this.methodName = methodName;
        vertices = new ArrayList<>();
        instructionList = new ArrayList<>();
    }

    public String parseClassName() {
        return methodName.substring(methodName.indexOf("<") + 1, methodName.indexOf(" ") - 1);
    }
    
    public String getFullName()
    {
        return this.methodName;
    }

    public String getClassName()
    {
        return this.our_class.getClassName();
    }

    public OurClass getOurClass() {
        return this.our_class;
    }

    public void setClass(OurClass c) {
        this.our_class = c;
    }
    
    public void addVertex(StateVertex v)
    {
        this.vertices.add(v);
    }
    
    public ArrayList<Instruction> getInstructionList()
    {
        return this.instructionList;
    }

    //Since we override the equals method, we must also override the hashCode method.
    public int hashCode()
    {
        return this.methodName.hashCode();
    }
    
    public boolean equals(Object other)
    {
        if(other instanceof Method)
            return this.methodName.equals(((Method) other).methodName);
        else
            return false;
    }
}
