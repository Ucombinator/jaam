package org.ucombinator.jaam.visualizer.classTree;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import org.ucombinator.jaam.visualizer.controllers.CodeViewController;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;
import org.ucombinator.jaam.visualizer.state.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.HashSet;

public abstract class ClassTreeNode
{
    private CheckBoxTreeItem<ClassTreeNode> parent;

    protected CheckBoxTreeItem<ClassTreeNode> buildTreeItem(CheckBoxTreeItem<ClassTreeNode> parent) {
        CheckBoxTreeItem<ClassTreeNode> item = new CheckBoxTreeItem<>();
        item.setSelected(true);
        item.setValue(this);
        item.setIndependent(false);

        item.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean prevVal, Boolean currVal) {

                // Remove duplicate events if a node is not at the top level, and is equal to its parent.
                if ((currVal != ClassTreeNode.this.parent.isSelected()) || (ClassTreeNode.this.isTopLevel())) {
                    HashSet<StateVertex> childVertices = item.getValue().getChildVertices();

                    if (currVal) {
                        Main.getSelectedMainTabController().getHidden().removeAll(childVertices);
                    } else {
                        Main.getSelectedMainTabController().getHidden().addAll(childVertices);
                    }
                }
            }
        });
        parent.getChildren().add(item);
        this.parent = parent;

        return item;
    }

    public abstract void handleDoubleClick(CodeViewController codeView);

    public abstract HashSet<StateVertex> getChildVertices();

    public abstract String getName();

    public abstract Node getGraphic();

    public boolean isTopLevel() {
        return this.parent.getParent() == null;
    }
}
