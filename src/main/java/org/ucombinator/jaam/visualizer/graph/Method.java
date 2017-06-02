package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;

import org.ucombinator.jaam.visualizer.main.Main;

public class Method
{
	private final String methodName;
	private Class our_class;
	//private final String functionName;
	private ArrayList<Vertex> vertices;
	private ArrayList<Instruction> instructionList;

	public Method()
	{
		this.our_class = new Class();
		this.methodName = "Dummy_method";
		vertices = new ArrayList<Vertex>();
		instructionList = new ArrayList<Instruction>();
	}

	public Method(Graph graph, String methodName)
	{
		this.our_class = new Class();
		this.methodName = methodName;
		vertices = new ArrayList<Vertex>();
		instructionList = new ArrayList<Instruction>();
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

	public Class getOurClass() {
		return this.our_class;
	}

	public void setClass(Class c) {
		this.our_class = c;
	}
	
	public void addVertex(Vertex v)
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
