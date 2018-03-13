package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

public class SearchResults extends BorderPane
{
    public TreeView<String> searchTree;
    private TreeItem<String> root;
    
    public SearchResults()
    {
        this.root = new TreeItem<>("Search Results");
        this.searchTree = new TreeView<>(root);
        this.searchTree.setShowRoot(true);
        this.root.setExpanded(true);
        this.setCenter(searchTree);

        /*this.searchTree.addMouseListener
        (
            new MouseListener()
            {
                public void mouseClicked(MouseEvent e)
                {
                    //System.out.println("Location: " + e.getX() + ", " + e.getY());
                    //System.out.println("Search tree: " + searchTree.toString());
                    TreePath path = searchTree.getPathForLocation(e.getX(), e.getY());
                    //System.out.println("TreePath = " + path);
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)(path.getLastPathComponent());
                    org.ucombinator.jaam.visualizer.graph.AbstractVertex ver = (org.ucombinator.jaam.visualizer.graph.AbstractVertex)(node.getUserObject());
                    
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
        );*/
     }

    //Set the text for the area
    public void writeText(MainTabController mainTab)
    {
        this.root.getChildren().clear();
        if(mainTab.getVizHighlighted().size() > 0) {
            // We don't want to include the panel root, so we start our check with its children
            for (AbstractLayoutVertex v : mainTab.vizPanelController.getPanelRoot().getVisibleInnerGraph().getVertices()) {
                v.addTreeNodes(this.root, mainTab);
            }

            // TODO: Auto-expand nodes?
            //DefaultTreeModel model = (DefaultTreeModel)this.searchTree.getModel();
            //model.reload(this.root);
        }
    }
}
