package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;

import org.ucombinator.jaam.visualizer.main.Main;

public class Method
{
	public Class ourClass;
	private final String methodName;
	//private final String functionName;
	ArrayList<Vertex> vertices;
	ArrayList<Instruction> instructionList;

	public Method()
	{
		this.methodName = "Dummy_method";
		this.ourClass = new Class("Dummy_class");
		vertices = new ArrayList<Vertex>();
		instructionList = new ArrayList<Instruction>();
	}

	public Method(Graph graph, String methodName)
	{
		this.methodName = methodName;

		if(!methodName.equals("ErrorState"))
		{
			String[] splitMethodName = methodName.split(" ");

			//Remove beginning angle bracket and ending colon
			String className = splitMethodName[0].substring(1, splitMethodName[0].length() - 1);
			this.addClass(graph, className);

			//this.functionName = splitMethodName[1];
		}

		vertices = new ArrayList<Vertex>();
		instructionList = new ArrayList<Instruction>();
	}
	
	public String getFullName()
	{
		return this.methodName;
	}

	public String getClassName()
	{
		return this.ourClass.getClassName();
	}

	public void addClass(Graph graph, String className)
	{
		this.ourClass = graph.getClasses().get(className);
		if(this.ourClass == null)
		{
			this.ourClass = new Class(className);
			graph.getClasses().put(className, this.ourClass);
		}

		this.ourClass.addMethod(this);
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
