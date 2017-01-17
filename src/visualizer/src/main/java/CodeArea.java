
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.DefaultCaret;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class CodeArea extends JTextArea
{
	private AbstractVertex selectedVertex;
	ArrayList<Instruction> description;
	ArrayList<Integer> rowToIndex; // Since some Jimple indices can be missing, we need to store an offset
	HashSet<AbstractVertex> highlighted;
    
    private int currentCaret = 0;
	
	public CodeArea()
	{
		this.setFont(Parameters.font);
		this.setEditable(false);
		description = new ArrayList<Instruction>();
		rowToIndex = new ArrayList<Integer>();
		highlighted = new HashSet<AbstractVertex>();
        
        ((DefaultCaret)this.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        
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
							if(line.isInstr)
							{
								if(line.isSelected)
								{
									CodeArea.this.searchByJimpleIndex(line.methodName, line.jimpleIndex,false);
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
                        Main.graph.clearSelects();
					
						if(row >= 0 && row < rowToIndex.size())
						{
							Instruction line = description.get(rowToIndex.get(row));
							if(line.isInstr)
							{
								Parameters.vertexHighlight = true;
								CodeArea.this.searchByJimpleIndex(line.methodName, line.jimpleIndex, true);
							}
						}
					}
					
					Main.graph.redoCycleHighlights();
					Parameters.repaintAll();
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
	//or unhighlights them if addHighlight is false.
	public void searchByJimpleIndex(String method, int index, boolean addHighlight)
	{
		// Unhighlight currently highlighted vertices
		for(AbstractVertex v : highlighted) {
			//v.setHighlighted(false);
		}

		//Next we either add the highlighted vertices
		AbstractVertex panelRoot = Parameters.stFrame.mainPanel.getPanelRoot();
		highlighted = panelRoot.getVerticesWithInstructionID(index, method);

		for(AbstractVertex v : highlighted) {
			//v.setHighlighted(true);
		}
	}

	public void clear()
	{
		this.selectedVertex = null;
		this.description = new ArrayList<Instruction>();
		this.rowToIndex = new ArrayList<Integer>();
		this.writeText();
	}

	public void setVertex(AbstractVertex v) {
		this.selectedVertex = v;
	}

	// Rewrite the text area based on which vertex is selected
	public void setDescription()
	{
		if(this.selectedVertex != null)
		{
			//description.add(new Instruction(currMethod + "\n", currMethod, false, -1));

			//Add all instructions in the method
			description = new ArrayList<Instruction>(selectedVertex.getInstructions());
			Collections.sort(description);

			for(Instruction inst: description)
				System.out.println(inst.methodName + ", " + inst.jimpleIndex + ": " + inst.str);

			//Add blank line after each method
			//description.add(new Instruction("\n", currMethod, false, -1));

			int rowNumber = 0;
			rowToIndex = new ArrayList<Integer>();
			for (int i = 0; i < description.size(); i++, rowNumber++) {
				if (description.get(i).str.length() > 0)
					rowToIndex.add(rowNumber);
			}

			this.computeDescriptionIndex();
			this.writeText();
			this.drawHighlights(Parameters.colorSelection, Parameters.colorFocus, Parameters.colorHighlight);
		}
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
    
    public void fixCaretPosition()
    {
        try
        {
            int line = this.getLineOfOffset(this.getCaretPosition());
            int close = -1;
            int dist1, dist2;
            
            for(int i = 0; i < this.description.size(); i++)
            {
                if(!this.description.get(i).isSelected)
                    continue;
                
                dist1 = line - close;
                dist2 = line - i;
                
                if((dist2 * dist2 < dist1 * dist1) || !this.description.get(line).isSelected)
                    close = i;
            }
            
            if(close >= 0)
            {
                this.setCaretPosition(this.getLineStartOffset(close));
            }
        }
        catch(BadLocationException ex)
        {
            System.out.println("doc = "+CodeArea.this.getText());
            System.out.println(ex);
        }

    }
	
	private void drawHighlights(Color c1, Color c2, Color c3)
	{
        Highlighter h = CodeArea.this.getHighlighter();
        h.removeAllHighlights();
		for(int i = 0; i < this.description.size(); i++)
		{
			//TODO: Find new color for applying both highlights?
			Instruction line = this.description.get(i);
            if(line.isSelected)
                this.drawLineHighlight(i, c3);
			else if(line.isHighlighted)
				this.drawLineHighlight(i, c1);
			else if(line.isCycleHighlighted)
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
