package org.ucombinator.jaam.visualizer.classTree;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import org.ucombinator.jaam.visualizer.controllers.CodeViewController;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;
import org.ucombinator.jaam.visualizer.layout.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.Comparator;
import java.util.HashSet;

// ClassTree Code -------------------------------------
// Has a double function, either a folder (inner node) in which case it has no vertex;
// Or a leaf node in which case it is associated to a one or more methods
public class ClassTreeNode
{
    public String shortName;
    public String name;

    public ClassTreeNode(String shortName, String prefix)
    {
        this.shortName = shortName;
        if(prefix == null)
            name = new String("");
        else if(prefix.compareTo("") == 0)
            name = shortName;
        else
            name = prefix + "." + shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    public String getName() { return name; }

    public boolean hasCode() {return false;}

    protected CheckBoxTreeItem<ClassTreeNode> buildTreeItem(TreeItem<ClassTreeNode> parent) {
        CheckBoxTreeItem<ClassTreeNode> item = new CheckBoxTreeItem<>();
        item.setSelected(true);
        item.setValue(this);
        item.setIndependent(false);
        item.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean prevVal, Boolean currVal) {

                HashSet<StateVertex> childVertices = item.getValue().getChildVertices();
                VizPanelController vizPanelController = Main.getSelectedVizPanelController();

                vizPanelController.startBatchMode();
                if(currVal) {
                    Main.getSelectedMainTabController().getHidden().removeAll(childVertices);
                } else {
                    Main.getSelectedMainTabController().getHidden().addAll(childVertices);
                }
                vizPanelController.endBatchMode();
                vizPanelController.resetAndRedraw();
            }
        });
        parent.getChildren().add(item);


        item.getChildren().sort(Comparator.comparing(t->t.getValue().shortName));

        return item;
    }

    public void handleDoubleClick(CodeViewController codeView) {
        // Do Nothing
    }

    public HashSet<StateVertex> getChildVertices() {
        return new HashSet<>();
    }

} // End of class TreeNode