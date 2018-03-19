package org.ucombinator.jaam.visualizer.classTree;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import org.ucombinator.jaam.visualizer.controllers.CodeViewController;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;
import org.ucombinator.jaam.visualizer.state.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.util.HashSet;

public abstract class ClassTreeNode
{
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

                // TODO: Somehow wrap this so that when we uncheck a parent checkbox,
                // we ignore these events for its children.
                vizPanelController.startBatchMode();
                if (currVal) {
                    Main.getSelectedMainTabController().getHidden().removeAll(childVertices);
                } else {
                    Main.getSelectedMainTabController().getHidden().addAll(childVertices);
                }
                vizPanelController.endBatchMode();
            }
        });
        parent.getChildren().add(item);

        return item;
    }

    public abstract void handleDoubleClick(CodeViewController codeView);

    public abstract HashSet<StateVertex> getChildVertices();
}
