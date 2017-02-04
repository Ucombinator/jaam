
//TODO: Save list of matching vertices here to speed up highlighting?
public class Instruction implements Comparable<Instruction>
{
	public String str;
	public String methodName;
	public boolean isInstr;
	public int jimpleIndex;
	public boolean isCycleHighlighted;
	public boolean isHighlighted;
    public boolean isSelected;

	public int startIndex;
	public int endIndex;
	
	public Instruction(String methodName)
	{
		this.str = "";
		this.methodName = methodName;
		this.isInstr = false;
		this.jimpleIndex = -1;
		this.isHighlighted = false;
        this.isCycleHighlighted = false;
        this.isSelected = false;
		this.startIndex = -1;
		this.endIndex = -1;
	}
	
	public Instruction(String str, String method, boolean isInstr, int jimpleIndex)
	{
		this.str = str;
		this.methodName = method;
		this.isInstr = isInstr;
		this.jimpleIndex = jimpleIndex;
		this.isHighlighted = false;
        this.isCycleHighlighted = false;
        this.isSelected = false;
		this.startIndex = -1;
		this.endIndex = -1;
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
