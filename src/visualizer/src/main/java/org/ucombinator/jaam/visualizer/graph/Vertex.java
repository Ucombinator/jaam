package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;
import java.util.Iterator;

import org.ucombinator.jaam.visualizer.main.Main;

public class Vertex extends AbstractVertex
{
	private int inputId;
	private Instruction instruction;
	private boolean drawEdges;

	//Used for shading vertices based on the number of nested loops they contain
	//loopHeight is stored for all vertices
	private int loopHeight;
	private Vertex loopHeader;
	private ArrayList<Vertex> loopChildren;
	private int dfsPathPos;
	private boolean traversed;

	public Vertex(int inputId) {
		super(Integer.toString(inputId));
		this.inputId = inputId;
		this.instruction = new Instruction("");
		this.drawEdges = true;

		//Used for shading vertices based on the number of nested loops they contain
		//loopHeight is stored for all vertices
		this.loopHeight = -1;
		this.loopHeader = null;
		this.loopChildren = new ArrayList<Vertex>();
		this.dfsPathPos = -1;
		this.traversed = false;
	}

	public Vertex(Instruction inst, int inputId, boolean drawEdges) {
		this(inputId);
		this.instruction = inst;
		this.drawEdges = drawEdges;
	}
	
	public String getMethodName()
	{
		return this.instruction.getMethodName();
	}
	
	public String getInstructionText()
	{
		return this.instruction.getText();
	}

	public void setDrawEdges(boolean drawEdges) {
		this.drawEdges = drawEdges;
	}
	
	public void setLoopHeader(Vertex v)
	{
		this.loopHeader = v;
	}
	
	public Vertex getLoopHeader()
	{
		return this.loopHeader;
	}
	
	public void addLoopChild(Vertex v)
	{
		if(this != v)
			loopChildren.add(v);
		else
			System.out.println("Error! Cannot add self as header in loop decomposition.");
	}
	
	public ArrayList<Vertex> getLoopChildren()
	{
		return loopChildren;
	}
	
	public Instruction getInstruction()
	{
		return this.instruction;
	}

	public void setRealInstruction(Instruction inst) {this.instruction = inst; }
	
}