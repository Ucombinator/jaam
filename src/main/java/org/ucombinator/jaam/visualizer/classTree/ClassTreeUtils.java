package org.ucombinator.jaam.visualizer.classTree;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import org.ucombinator.jaam.visualizer.controllers.CodeViewController;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.MethodEntity;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.state.StateEdge;
import org.ucombinator.jaam.visualizer.state.StateRootVertex;
import org.ucombinator.jaam.visualizer.state.StateVertex;

import java.util.ArrayList;
import java.util.HashMap;

public class ClassTreeUtils {

    public static void buildClassTree(TreeView<ClassTreeNode> classTree, CodeViewController codeViewController, StateRootVertex immutableRoot)
    {
        classTree.setCellFactory(CheckBoxTreeCell.forTreeView());

        PackageNode root = new PackageNode("", "");

        HashMap<String, PackageNode> rootLevel = new HashMap<>();

        for (String c : codeViewController.getClassNames()) {
            String[] split = c.split("\\.");

            PackageNode current;
            String rootPackageName = split[0];
            if (rootLevel.containsKey(rootPackageName)) {
                current = rootLevel.get(rootPackageName);
            }
            else {
                current = new PackageNode(rootPackageName, "");
                rootLevel.put(rootPackageName, current);
            }

            for (int i = 1; i < split.length - 1; i++) {
                current = current.addPackageIfAbsent(split[i]);
            }

            String className = split[split.length - 1];
            current.addClassIfAbsent(className);
        }

        ArrayList<PackageNode> topLevel = new ArrayList<>(rootLevel.values());

        // Compression Step
        topLevel.forEach(PackageNode::compress);

        // Add the vertices
        addVerticesToClassTree(topLevel, immutableRoot);
        addFieldsToClassTree(topLevel, codeViewController);

        // Build the Tree
        CheckBoxTreeItem<ClassTreeNode> treeRoot = new CheckBoxTreeItem<>();
        treeRoot.setSelected(true);
        treeRoot.setValue(root);
        treeRoot.setExpanded(true);

        topLevel.forEach(f -> f.build(treeRoot));

        classTree.setRoot(treeRoot);
        ClassTreeUtils.setEventHandlers(classTree);
    }

    public static void setEventHandlers(TreeView<ClassTreeNode> classTree) {
        classTree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<ClassTreeNode>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<ClassTreeNode>> observableValue,
                                TreeItem<ClassTreeNode> oldValue, TreeItem<ClassTreeNode> newValue) {

                if (oldValue != null) {
                    Main.getSelectedMainTabController().setClassHighlight(oldValue.getValue().getChildVertices(), false);
                }

                if (newValue != null) {
                    Main.getSelectedMainTabController().setClassHighlight(newValue.getValue().getChildVertices(), true);
                }
            }
        });

        classTree.setOnMouseClicked(m -> {
            if (m.getClickCount() == 2) {
                final TreeItem<ClassTreeNode> item = classTree.getSelectionModel().getSelectedItem();

                item.getValue().handleDoubleClick(Main.getSelectedMainTabController().codeViewController);
            }
        });
    }

    private static void addFieldsToClassTree(ArrayList<PackageNode> topLevel, CodeViewController codeViewController) {
        for (PackageNode n : topLevel) {
            n.addFields(codeViewController);
        }
    }

    private static void addVerticesToClassTree(ArrayList<PackageNode> topLevel, StateVertex root) {

        if(root instanceof MethodEntity) {
            PackageNode topLevelNode = getTopLevel(topLevel, ((MethodEntity) root).getClassName());
            boolean success = false;
            if (topLevelNode != null) {
                success = topLevelNode.addVertex(root);
            }

            if (!success) {
                System.out.println("Warning didn't find package for: " + ((MethodEntity) root).getClassName());
            }
        }

        Graph<StateVertex, StateEdge> childGraph = root.getInnerGraph();
        for (StateVertex v : childGraph.getVertices()) {
            addVerticesToClassTree(topLevel, v);
        }
    }

    private static PackageNode getTopLevel(ArrayList<PackageNode> topLevel, String className) {
        for (PackageNode n : topLevel) {
            if (className.startsWith(n.getShortName()))
                return n;
        }
        return null;
    }

    public static TreeItem<ClassTreeNode> buildFilteredTree(TreeItem<ClassTreeNode> treeItem, String query) {
        TreeItem<ClassTreeNode> itemCopy = new TreeItem<>();
        itemCopy.setValue(treeItem.getValue());
        itemCopy.setGraphic(itemCopy.getValue().getGraphic());

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
