package org.ucombinator.jaam.visualizer.graph;

//TODO: Save list of matching vertices here to speed up highlighting?
public class Instruction implements Comparable<Instruction>
{
	private String text;
	private String methodName;
	private boolean isInstr;
	private int jimpleIndex;
	private int descriptionIndex;
	private boolean isSelected;
	
	public Instruction(String methodName)
	{
		this.text = "";
		this.methodName = methodName;
		this.isInstr = false;
		this.jimpleIndex = -1;
		this.descriptionIndex = -1;
		this.isSelected = false;
	}
	
	public Instruction(String str, String method, int jimpleIndex, boolean isInstr)
	{
		this.text = str;
		this.methodName = method;
		this.isInstr = isInstr;
		this.jimpleIndex = jimpleIndex;
		this.descriptionIndex = -1;
		this.isSelected = false;
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

	public boolean isRealInstruction() {
		return this.isInstr;
	}

	public void setDescriptionIndex(int descriptionIndex) {
		this.descriptionIndex = descriptionIndex;
	}

	public int getStartIndex() {
		return this.descriptionIndex;
	}

	public int getEndIndex() {
		return this.descriptionIndex + this.text.length();
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public boolean isSelected() {
		return this.isSelected;
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
