package org.ucombinator.jaam.visualizer.classTree;

import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import org.controlsfx.glyphfont.FontAwesome;
import org.ucombinator.jaam.visualizer.controllers.CodeViewController;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.state.StateVertex;

import java.util.HashSet;

public class FieldNode extends ClassTreeNode implements Comparable<FieldNode>{

    private String className;
    private String fieldName;

    public FieldNode(String name, String className) {
       this.className = className;
       this.fieldName = name;
    }

    public void build(CheckBoxTreeItem<ClassTreeNode> parent) {
        CheckBoxTreeItem<ClassTreeNode> item = buildTreeItem(parent);
        item.setGraphic(this.getGraphic());
    }

    @Override
    public String getName() {
        return className + "." + fieldName;
    }

    @Override
    public String toString() {
        return fieldName;
    }

    @Override
    public void handleDoubleClick(CodeViewController codeView) {
        Main.getSelectedMainTabController().selectFieldInTaintGraph(className, fieldName);
    }

    @Override
    public HashSet<StateVertex> getChildVertices() { return new HashSet<>(); }

    @Override
    public int compareTo(FieldNode o) {
        return fieldName.compareTo(o.fieldName);
    }

    @Override
    public Node getGraphic() {
        return Main.getIconFont().create(FontAwesome.Glyph.FACEBOOK_F).color(Color.DARKGRAY);
    }
}
