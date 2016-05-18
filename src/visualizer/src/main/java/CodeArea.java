
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Color;

import java.util.ArrayList;
import java.util.HashSet;

public class CodeArea extends JTextArea
{
	private HashSet<Method> methods = new HashSet<Method>();
	ArrayList<Instruction> description;
	ArrayList<Integer> rowToIndex; //Since some Jimple indices can be missing, we need to store an offset
	
	public CodeArea()
	{
		this.setFont(Parameters.font);
		System.out.println("Initializing empty code area");
		this.setEditable(false);
		description = new ArrayList<Instruction>();
		rowToIndex = new ArrayList<Integer>();
		
		this.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent e)
				{
					int y = e.getY();
					Font font = CodeArea.this.getFont();
					int lineHeight = CodeArea.this.getFontMetrics(font).getHeight();
					int row = y/lineHeight;

					if(e.isShiftDown())
					{
						if(row >= 0 && row < rowToIndex.size())
						{
							Instruction line = description.get(rowToIndex.get(row));
							System.out.println("Instruction selected on row " + row + ", " + line);
							if(line.isInstr)
							{
								if(line.isHighlighted)
								{
									CodeArea.this.searchByJimpleIndex(line.methodName, line.jimpleIndex, false);
								}
								else
								{
									Parameters.vertexHighlight = true;
									CodeArea.this.searchByJimpleIndex(line.methodName, line.jimpleIndex, true);
								}
							}
						}
					}
					else
					{
						Highlighter h = CodeArea.this.getHighlighter();
						h.removeAllHighlights();
						StacViz.graph.clearHighlights();
					
						if(row >= 0 && row < rowToIndex.size())
						{
							Instruction line = description.get(rowToIndex.get(row));
							if(line.isInstr)
							{
								CodeArea.this.drawLineHighlight(row, Parameters.colorSelection);
								
								Parameters.vertexHighlight = true;
								CodeArea.this.searchByJimpleIndex(line.methodName, line.jimpleIndex, true);
							}
						}
					}
					
					StacViz.graph.redoCycleHighlights();
					CodeArea.this.setDescription();
					Parameters.setRightText();
					Parameters.refreshBoth();
				}
				
				public void mousePressed(MouseEvent e){}
				
				public void mouseReleased(MouseEvent e){}
				
				public void mouseEntered(MouseEvent e){}
				
				public void mouseExited(MouseEvent e){}
			}
		);
	}
	
	//Cannot be called directly, but is called when the user clicks on a line in the left area
	//Find vertices corresponding to the given highlight, and highlights them if addHighlight is true,
	//or unhighlights them if addHighlight is false
	public void searchByJimpleIndex(String method, int index, boolean addHighlight)
	{
		//Next we either add or remove the highlighted vertices
		for(Vertex v : StacViz.graph.vertices)
		{
			if(v.getMethodName().contains(method) && v.jimpleIndex == index)
			{				
				if(addHighlight)
					v.addHighlight(true, true, true);
				else
					v.clearAllHighlights();
			}
		}
	}

	public void clear()
	{
		//System.out.println("Clearing left area");
		this.methods = new HashSet<Method>();
		this.description = new ArrayList<Instruction>();
		this.rowToIndex = new ArrayList<Integer>();
		this.writeText();
	}
		
	//Rewrite our description based on which vertices are highlighted
	public void setDescription()
	{
		//System.out.println("Setting new description for left area");
		description = new ArrayList<Instruction>();
		this.methods = StacViz.graph.collectHighlightedMethods();
		
		for(Method method : this.methods)
		{
			method.highlightInstructions();
			//Add header line with method name
			String currMethod = method.getName();
			description.add(new Instruction(currMethod + "\n", currMethod, false, -1));
			
			//Add all instructions in the method
			description.addAll(method.getInstructionList());
			
			//Add blank line after each method
			description.add(new Instruction("\n", currMethod, false, -1));
		}
		
		int rowNumber = 0;
		for(int i = 0; i < description.size(); i++, rowNumber++)
		{
			if(description.get(i).str.length() > 0)
				rowToIndex.add(rowNumber);
		}
		
		this.computeDescriptionIndex();
		this.writeText();
		this.drawHighlights(Parameters.colorSelection, Parameters.colorFocus);
	}
	
	private void computeDescriptionIndex()
	{
		if(this.description.size() > 0)
		{
			this.description.get(0).startIndex = 0;
			this.description.get(0).endIndex = this.description.get(0).str.length();
			for(int i = 1; i < this.description.size(); i++)
			{
				this.description.get(i).startIndex = this.description.get(i - 1).endIndex;
				this.description.get(i).endIndex = this.description.get(i).startIndex + this.description.get(i).str.length();
			}
		}
	}
	
	//Set the text for the area to the sum of all of the lines in the description
	private void writeText()
	{
		StringBuilder fullText = new StringBuilder();
		for(Instruction line : description)
			fullText.append(line.str);
		
		this.setText(fullText.toString());
	}
	
	//Search our description for the line that needs to be highlighted
	public void highlightMatchingInstruction(Vertex v)
	{
		for(Instruction inst : this.description)
		{
			if(v.getMethodName().equals(inst.methodName) && v.jimpleIndex == inst.jimpleIndex)
				inst.isHighlighted = true;
		}
	}
	
	public void drawHighlights(Color c1, Color c2)
	{
		for(int i = 0; i < this.description.size(); i++)
		{
			Instruction line = this.description.get(i);
			if(line.isHighlighted)
				this.drawLineHighlight(i, c1);
			if(line.isCycleHighlighted)
				this.drawLineHighlight(i, c2);
		}
	}
	
	private void drawLineHighlight(int rowIndex, Color c)
	{
		DefaultHighlighter.DefaultHighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(c);
		Highlighter h = this.getHighlighter();
		
		try
		{
			h.addHighlight(description.get(rowIndex).startIndex, description.get(rowIndex).endIndex, highlightPainter);
		}
		catch(BadLocationException e)
		{
			System.out.println(e);
		}
	}
}
