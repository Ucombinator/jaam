package org.ucombinator.jaam.visualizer.classTree;

import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import org.controlsfx.glyphfont.FontAwesome;
import org.ucombinator.jaam.visualizer.controllers.CodeViewController;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.state.StateVertex;

import java.util.HashSet;

public class FieldNode extends ClassTreeNode implements Comparable<FieldNode>{

    public FieldNode(String name, String className) {
       super(name, className);
    }

    public void build(TreeItem<ClassTreeNode> parent) {
        CheckBoxTreeItem<ClassTreeNode> item = buildTreeItem(parent);

        item.setGraphic(Main.getIconFont().create(FontAwesome.Glyph.FACEBOOK_F).color(Color.DARKGRAY));
    }

    @Override
    public void handleDoubleClick(CodeViewController codeView) { /* do nothing */ }

    @Override
    public HashSet<StateVertex> getChildVertices() { return new HashSet<>(); }

    @Override
    public int compareTo(FieldNode o) {
        return name.compareTo(o.name);
    }
}
