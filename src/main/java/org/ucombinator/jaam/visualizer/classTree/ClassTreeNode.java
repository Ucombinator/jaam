package org.ucombinator.jaam.visualizer.classTree;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;
import org.ucombinator.jaam.visualizer.layout.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.Comparator;
import java.util.HashSet;

// ClassTree Code -------------------------------------
// Has a double function, either a folder (inner node) in which case it has no vertex;
// Or a leaf node in which case it is associated to a one or more vertices
public class ClassTreeNode
{
    public String name;
    public String fullName;

    public ClassTreeNode(String name, String prefix)
    {
        this.name = name;
        if(prefix == null)
            fullName = new String("");
        else if(prefix.compareTo("") == 0)
            fullName = name;
        else
            fullName = prefix + "." + name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getFullName() { return  fullName; }

    public boolean isLeaf() { return false;}

    protected CheckBoxTreeItem<ClassTreeNode> buildTreeItem(TreeItem<ClassTreeNode> parent) {
        CheckBoxTreeItem<ClassTreeNode> item = new CheckBoxTreeItem<>();
        item.setSelected(true);
        item.setValue(this);
        item.setIndependent(false);
        item.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean prevVal, Boolean currVal) {

                HashSet<StateVertex> childVertices = item.getValue().getChildVertices();
                System.out.println("\t\tJUAN children is: " + childVertices);
                System.out.println("Current value: " + currVal);
                System.out.println("Previous value: " + prevVal);

                VizPanelController vizPanelController = Main.getSelectedVizPanelController();

                vizPanelController.startBatchMode();
                if(currVal) {
                    System.out.println("Showing nodes...");
                    Main.getSelectedMainTabController().getHidden().removeAll(childVertices);
                } else {
                    System.out.println("Hiding nodes...");
                    Main.getSelectedMainTabController().getHidden().addAll(childVertices);
                }
                vizPanelController.endBatchMode();
                vizPanelController.resetAndRedraw();
            }
        });
        parent.getChildren().add(item);


        item.getChildren().sort(Comparator.comparing(t->t.getValue().name));

        return item;
    }

    public HashSet<StateVertex> getChildVertices() {
        return new HashSet<>();
    }

} // End of class TreeNode