
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Color;
import java.awt.BorderLayout;


import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Component;


public class SearchArea extends JPanel
{
    public JTree searchTree;
    private DefaultMutableTreeNode root;
    
	public SearchArea()
	{
        this.setLayout(new BorderLayout());
        
        this.root = new DefaultMutableTreeNode("Search\nResults");
        this.searchTree = new JTree(root);

        this.searchTree.setShowsRootHandles(true);
        this.searchTree.setRootVisible(false);
        this.searchTree.setRowHeight(40);
        this.searchTree.setCellRenderer(new SearchRenderer());
        
        this.add(this.searchTree, BorderLayout.CENTER);

        this.searchTree.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent e)
				{
                    TreePath path = searchTree.getPathForLocation(e.getX(), e.getY());
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)(path.getLastPathComponent());
                    AbstractVertex ver = (AbstractVertex)(node.getUserObject());
                    
                    if(!ver.isVisible)
                        return;

					if(e.isShiftDown())
					{
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
					else
					{
                        Main.graph.clearSelects();
                        Parameters.vertexHighlight = true;
                        ver.addHighlight(true, false, true, true);
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
	
    
    
    
    private void expandAllNodes(JTree tree)
    {
        for (int i = 0; i < tree.getRowCount(); i++)
        {
            tree.expandRow(i);
        }
    }

    
    
	//Set the text for the area
	public void writeText()
	{
        this.root.removeAllChildren();

        DefaultMutableTreeNode currentPNode = new DefaultMutableTreeNode();
        DefaultMutableTreeNode currentMNode = new DefaultMutableTreeNode();
        DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        
        for(AbstractVertex pver : Main.graph.methodPathVertices)
        {
            if(Parameters.vertexHighlight && (pver.isBranchSelected() || pver.isBranchHighlighted()))
            {
                currentPNode = pver.toDefaultMutableTreeNode();
                root.add(currentPNode);
            }
            
            for(AbstractVertex mver : pver.getMergeChildren())
            {
                if(Parameters.vertexHighlight && (mver.isBranchSelected() || mver.isBranchHighlighted()))
                {
                    currentMNode = mver.toDefaultMutableTreeNode();
                    currentPNode.add(currentMNode);
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
	}
	
    
    private class SearchRenderer extends DefaultTreeCellRenderer
    {
        private String text = "";
        
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
                label.setForeground(Color.GRAY);
            }
            return label;
        }
    }
    
}
