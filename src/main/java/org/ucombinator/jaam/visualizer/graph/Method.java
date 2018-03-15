package org.ucombinator.jaam.visualizer.graph;

import org.ucombinator.jaam.visualizer.layout.StateVertex;

import java.util.ArrayList;

public class Method
{
    private final String methodName;
    private ArrayList<StateVertex> vertices;

    public Method() {
        this.methodName = "Dummy_method";
        vertices = new ArrayList<>();
    }

    public Method(String methodName) {
        this.methodName = methodName;
        vertices = new ArrayList<>();
    }
    
    public void addVertex(StateVertex v)
    {
        this.vertices.add(v);
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
