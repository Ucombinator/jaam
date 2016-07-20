
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Color;

import java.util.ArrayList;

public class SearchArea extends JTextArea
{
    private ArrayList<AbstractVertex> searchVertices;
    private ArrayList<Integer> startIndex, endIndex;
	
	public SearchArea()
	{
		this.setFont(Parameters.font);
		this.setEditable(false);
        
        this.searchVertices = new ArrayList<AbstractVertex>();
        this.startIndex = new ArrayList<Integer>();
        this.endIndex = new ArrayList<Integer>();

        
		this.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent e)
				{
					int y = e.getY();
					Font font = SearchArea.this.getFont();
					int lineHeight = SearchArea.this.getFontMetrics(font).getHeight();
					int row = y/(2*lineHeight);

					if(e.isShiftDown())
					{
						if(row >= 0 && row < SearchArea.this.searchVertices.size())
						{
                            AbstractVertex ver = SearchArea.this.searchVertices.get(row);
                            if(ver.isSelected())
                            {
                                ver.clearAllSelect();
                            }
                            else
                            {
                                Parameters.vertexHighlight = true;
                                ver.addHighlight(true, false, true, true);
                            }
						}
					}
					else
					{
                        Main.graph.clearSelects();
					
						if(row >= 0 && row < SearchArea.this.searchVertices.size())
						{
                            AbstractVertex ver = SearchArea.this.searchVertices.get(row);
                            
                            Parameters.vertexHighlight = true;
                            ver.addHighlight(true, false, true, true);
						}
					}
					
					Main.graph.redoCycleHighlights();
                    SearchArea.this.getSearchVertices();
					Parameters.repaintAll();
				}
				
				public void mousePressed(MouseEvent e){}
				
				public void mouseReleased(MouseEvent e){}
				
				public void mouseEntered(MouseEvent e){}
				
				public void mouseExited(MouseEvent e){}
			}
		);
	}
	
    
    public void getSearchVertices()
    {
        this.searchVertices = new ArrayList<AbstractVertex>();
        this.startIndex = new ArrayList<Integer>();
        this.endIndex = new ArrayList<Integer>();
        int count = 0;
        
        for(Vertex ver : Main.graph.vertices)
        {
//            if(ver.isVisible && Parameters.vertexHighlight && (ver.isSelected() || ver.isChildSelected() || ver.isHighlighted() || ver.isChildHighlighted()))
            if(ver.isVisible && Parameters.vertexHighlight && (ver.isSelected() || ver.isHighlighted()))
            {
                this.searchVertices.add(ver);
                this.startIndex.add(new Integer(count));
                count += ver.getShortDescription().length();
                this.endIndex.add(new Integer(count));
            }
        }
        
        for(MethodVertex ver : Main.graph.methodVertices)
        {
//            if(ver.isVisible && Parameters.vertexHighlight && (ver.isSelected() || ver.isChildSelected() || ver.isHighlighted() || ver.isChildHighlighted()))
            if(ver.isVisible && Parameters.vertexHighlight && (ver.isSelected() || ver.isHighlighted()))
            {
                this.searchVertices.add(ver);
                this.startIndex.add(new Integer(count));
                count += ver.getShortDescription().length();
                this.endIndex.add(new Integer(count));
            }
        }
        
        for(MethodPathVertex ver : Main.graph.methodPathVertices)
        {
//            if(ver.isVisible && Parameters.vertexHighlight && (ver.isSelected() || ver.isChildSelected() || ver.isHighlighted() || ver.isChildHighlighted()))
            if(ver.isVisible && Parameters.vertexHighlight && (ver.isSelected() || ver.isHighlighted()))
            {
                this.searchVertices.add(ver);
                this.startIndex.add(new Integer(count));
                count += ver.getShortDescription().length();
                this.endIndex.add(new Integer(count));
            }
        }
        
    }
    

	public void clear()
	{
        this.searchVertices = new ArrayList<AbstractVertex>();
        this.startIndex = new ArrayList<Integer>();
        this.endIndex = new ArrayList<Integer>();
		this.writeText();
	}
		
	
	//Set the text for the area
	public void writeText()
	{
		StringBuilder fullText = new StringBuilder();
//        this.searchVertices = new ArrayList<AbstractVertex>();
        
        for(AbstractVertex ver : this.searchVertices)
        {
            String str = ver.getShortDescription();
			fullText.append(str);
        }
		
		this.setText(fullText.toString());
        this.drawHighlights(Parameters.colorHighlight);
        this.setCaretPosition(0);
	}
	
	private void drawHighlights(Color c)
	{
		for(int i = 0; i < this.searchVertices.size(); i++)
		{
			AbstractVertex ver = this.searchVertices.get(i);
            if(ver.isSelected())
                this.drawLineHighlight(i, c);
		}
	}
	
    private void drawLineHighlight(int row, Color c)
    {
        DefaultHighlighter.DefaultHighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(c);
        Highlighter h = this.getHighlighter();
        
        try
        {
            h.addHighlight(startIndex.get(row).intValue(), endIndex.get(row).intValue(), highlightPainter);
        }
        catch(BadLocationException e)
        {
            System.out.println(e);
        }
    }
}
