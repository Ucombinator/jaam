package org.ucombinator.jaam.visualizer.graph;

//TODO: Save list of matching vertices here to speed up highlighting?
public class Instruction implements Comparable<Instruction>
{
	private String text;
	private String methodName;
	private boolean isInstr;
	private int jimpleIndex;

	private int startIndex;
	private int endIndex;
	
	public Instruction(String methodName)
	{
		this.text = "";
		this.methodName = methodName;
		this.isInstr = false;
		this.jimpleIndex = -1;
		this.startIndex = -1;
		this.endIndex = -1;
	}
	
	public Instruction(String str, String method, int jimpleIndex, boolean isInstr)
	{
		this.text = str;
		this.methodName = method;
		this.isInstr = isInstr;
		this.jimpleIndex = jimpleIndex;
		this.startIndex = -1;
		this.endIndex = -1;
	}

	public String getText() {
		return text;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public int getJimpleIndex() {
		return jimpleIndex;
	}

	public int compareTo(Instruction otherInstruction)
	{
		return (new Integer(this.jimpleIndex)).compareTo(otherInstruction.jimpleIndex);
	}

	@Override
	public int hashCode()
	{
		String toCompare = this.methodName + Integer.toString(this.jimpleIndex);
		return toCompare.hashCode();
	}

	@Override
	public boolean equals(Object otherInstruction)
	{
		if(otherInstruction instanceof Instruction) {
			return (this.methodName.equals(((Instruction) otherInstruction).methodName)
					&& this.jimpleIndex == ((Instruction) otherInstruction).jimpleIndex);
		}
		else
			return false;
	}
}
