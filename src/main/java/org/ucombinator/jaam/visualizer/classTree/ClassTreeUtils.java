package org.ucombinator.jaam.visualizer.classTree;

import javafx.scene.control.TreeItem;

public class ClassTreeUtils {

    public static TreeItem<ClassTreeNode> buildFilteredTree(TreeItem<ClassTreeNode> treeItem, String query) {
        TreeItem<ClassTreeNode> itemCopy = new TreeItem<>();
        itemCopy.setValue(treeItem.getValue());

        // DFS our tree to find matching descendant items.
        for (TreeItem<ClassTreeNode> child : treeItem.getChildren()) {
            TreeItem<ClassTreeNode> childItem = buildFilteredTree(child, query);
            if (childItem != null) {
                itemCopy.getChildren().add(childItem);
            }
        }

        // We return our item if it matches, or if any of its descendants are matches.
        if (itemCopy.getValue().getName().contains(query) || itemCopy.getChildren().size() > 0) {
            System.out.println("Returning item: " + itemCopy.getValue().getName());
            return itemCopy;
        }
        else {
            return null;
        }
    }
}
