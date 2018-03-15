package org.ucombinator.jaam.visualizer.graph;

public interface AbstractVertex
{
    String getLabel();
    int getId();

    /*
    default boolean addTreeNodes(TreeItem<T> parentNode, MainTabController mainTab) {
        boolean addedNodes = false;
        TreeItem<T> newNode = new TreeItem<>((T)this);
        for (T v : this.getVisibleInnerGraph().getVertices()) { // TODO: Is this the right one?
            addedNodes |= v.addTreeNodes(newNode, mainTab);
        }

        if(mainTab.getVizHighlighted().contains(this) || addedNodes) {
            parentNode.getChildren().add(newNode);
            return true;
        } else {
            return false;
        }
    }
    */
}
