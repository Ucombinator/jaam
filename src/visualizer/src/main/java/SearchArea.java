
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Color;
import java.awt.BorderLayout;
//import javax.swing.BoxLayout;


import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.Component;
import javax.swing.BorderFactory;

import java.util.ArrayList;

public class SearchArea extends JPanel
{
    private ArrayList<AbstractVertex> searchVertices;
    private ArrayList<Integer> startIndex, endIndex;
    public JTree searchTree;
    private DefaultMutableTreeNode root;
//    private JTextArea area;
    
	public SearchArea()
	{
//        this.area = new JTextArea();
        this.root = new DefaultMutableTreeNode("Search\nResults");
//        this.root = new DefaultMutableTreeNode("<html>This is<p>the root node.</html>");

        this.searchTree = new JTree(root);
        this.searchTree.setShowsRootHandles(true);
        this.searchTree.setRootVisible(false);
        this.searchTree.setRowHeight(40);
        this.searchTree.setCellRenderer(new SearchRenderer());
        
        this.setLayout(new BorderLayout());
//		this.area.setFont(Parameters.font);
//		this.area.setEditable(false);
//        this.setBackground(Color.WHITE);
        this.clear();
        
        this.add(this.searchTree, BorderLayout.CENTER);
//        this.searchVertices = new ArrayList<AbstractVertex>();
//        this.startIndex = new ArrayList<Integer>();
//        this.endIndex = new ArrayList<Integer>();

        

/*
        this.area.addMouseListener
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
 */
	}
	
    
    public void getSearchVertices()
    {
        this.searchVertices = new ArrayList<AbstractVertex>();
        this.startIndex = new ArrayList<Integer>();
        this.endIndex = new ArrayList<Integer>();
        int count = 0;

/*
        for(Vertex ver : Main.graph.vertices)
        {
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
            if(ver.isVisible && Parameters.vertexHighlight && (ver.isSelected() || ver.isHighlighted()))
            {
                this.searchVertices.add(ver);
                this.startIndex.add(new Integer(count));
                count += ver.getShortDescription().length();
                this.endIndex.add(new Integer(count));
            }
        }
*/
        
        for(AbstractVertex pver : Main.graph.methodPathVertices)
        {
            if(Parameters.vertexHighlight && (pver.isBranchSelected() || pver.isBranchHighlighted()))
            {
                this.searchVertices.add(pver);
                this.startIndex.add(new Integer(count));
                count += pver.getShortDescription().length();
                this.endIndex.add(new Integer(count));
            }
            
            //ArrayList<AbstractVertex> methodVertices = pver.getMergeChildren();
            
            for(AbstractVertex mver : pver.getMergeChildren())
            {
                if(Parameters.vertexHighlight && (mver.isBranchSelected() || mver.isBranchHighlighted()))
                {
                    this.searchVertices.add(mver);
                    this.startIndex.add(new Integer(count));
                    count += mver.getShortDescription().length();
                    this.endIndex.add(new Integer(count));
                }
                
                for(AbstractVertex ver : mver.getMergeChildren())
                {
                    if(Parameters.vertexHighlight && (ver.isBranchSelected() || ver.isBranchHighlighted()))
                    {
                        this.searchVertices.add(ver);
                        this.startIndex.add(new Integer(count));
                        count += ver.getShortDescription().length();
                        this.endIndex.add(new Integer(count));
                    }
                }
            }
        }
        
    }
    
    
    private void expandAllNodes(JTree tree)
    {
        for (int i = 0; i < tree.getRowCount(); i++)
        {
            tree.expandRow(i);
        }
    }

	public void clear()
	{
        this.searchVertices = new ArrayList<AbstractVertex>();
        this.startIndex = new ArrayList<Integer>();
        this.endIndex = new ArrayList<Integer>();

//        this.root.removeAllChildren();
//        JTextArea area = new JTextArea("hello");
//        this.add(area);
//        area.setEditable(false);
	}
    
    
	//Set the text for the area
	public void writeText()
	{
        this.root.removeAllChildren();
//        boolean empty = true;
//        this.removeAll();
//        this.updateUI();
        this.clear();
//        JTree currentTree;
        DefaultMutableTreeNode currentPNode = new DefaultMutableTreeNode();
        DefaultMutableTreeNode currentMNode = new DefaultMutableTreeNode();
        DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        for(AbstractVertex pver : Main.graph.methodPathVertices)
        {
            if(Parameters.vertexHighlight && (pver.isBranchSelected() || pver.isBranchHighlighted()))
            {
//                empty = false;
                currentPNode = pver.toDefaultMutableTreeNode();
                root.add(currentPNode);
                //currentTree = new JTree(root);
                //this.add(currentTree);
//                this.add(new JTextArea(pver.getShortDescription()));
//                this.clear(pver.getShortDescription());
            }
            
            //ArrayList<AbstractVertex> methodVertices = pver.getMergeChildren();
            
            for(AbstractVertex mver : pver.getMergeChildren())
            {
                if(Parameters.vertexHighlight && (mver.isBranchSelected() || mver.isBranchHighlighted()))
                {
                    currentMNode = mver.toDefaultMutableTreeNode();
                    currentPNode.add(currentMNode);
//                    this.add(new JTextArea(mver.getShortDescription()));
                }
                
                for(AbstractVertex ver : mver.getMergeChildren())
                {
                    if(Parameters.vertexHighlight && (ver.isBranchSelected() || ver.isBranchHighlighted()))
                    {
                        node = ver.toDefaultMutableTreeNode();
                        currentMNode.add(node);
                    }
                }
            }
        }
        
        this.expandAllNodes(this.searchTree);
        DefaultTreeModel model = (DefaultTreeModel)this.searchTree.getModel();
        model.reload(this.root);
        this.expandAllNodes(this.searchTree);
//        this.searchTree = new JTree(root);
//        this.add(this.searchTree, BorderLayout.CENTER);

        
//        if(empty)
//        {
//            this.clear();
//        }

/*
        StringBuilder fullText = new StringBuilder();
//        this.searchVertices = new ArrayList<AbstractVertex>();
        
        for(AbstractVertex ver : this.searchVertices)
        {
            String str = ver.getShortDescription();
			fullText.append(str);
        }
		
		this.area.setText(fullText.toString());
        this.drawHighlights(Parameters.colorHighlight);
        this.area.setCaretPosition(0);
*/
	}
	
/*
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
        Highlighter h = this.area.getHighlighter();
        
        try
        {
            h.addHighlight(startIndex.get(row).intValue(), endIndex.get(row).intValue(), highlightPainter);
        }
        catch(BadLocationException e)
        {
            System.out.println(e);
        }
    }
*/
    
    private class SearchRenderer extends DefaultTreeCellRenderer
    {
        private String text = "";
        public SearchRenderer()
        {
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus)
        {
            JLabel label = (JLabel)super.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
            
            if(node==root)
                return label;
            AbstractVertex ver = (AbstractVertex)node.getUserObject();
            
            label.setText(ver.getShortDescription());
            label.setFont(Parameters.font);
            label.setOpaque(true);
            if(ver.isSelected)
            {
                label.setBackground(Parameters.colorHighlight);
                label.setForeground(Color.BLACK);
            }
            else if(ver.isHighlighted)
            {
                label.setBackground(Color.WHITE);
                label.setForeground(Color.BLACK);
            }
            else
            {
                label.setBackground(Color.WHITE);
//            if(!ver.isSelected && ! ver.isHighlighted)
                label.setForeground(Color.GRAY);
            }
//            JTextArea area = new JTextArea(ver.getShortDescription());
//            panel.add(label);
//            panel.setBorder(BorderFactory.createLineBorder(Color.black));
            return label;
        }
    }
    
/*
        private class SearchRenderer extends JTextArea implements TreeCellRenderer//extends DefaultTreeCellRenderer
        {
            private String text = "";
            public SearchRenderer()
            {
            }
            
            public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded,
                                                          boolean leaf, int row, boolean hasFocus)
            {
                //            Component comp = super.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
                
                if(node==root)
                    this.text = "Search\nResult";
                else
                {
                    AbstractVertex ver = (AbstractVertex)node.getUserObject();
                    text = ver.getShortDescription();
                }
                
                this.setText(text);
                return this;
                
                //            if(node==root)
                //                return comp;
                //            AbstractVertex ver = (AbstractVertex)node.getUserObject();
                //            return new JTextArea(ver.getShortDescription());
            }
    }
*/
    
    
}
