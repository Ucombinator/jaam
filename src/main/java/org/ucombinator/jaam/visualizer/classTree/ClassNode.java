package org.ucombinator.jaam.visualizer.classTree;

import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import org.controlsfx.glyphfont.FontAwesome;
import org.ucombinator.jaam.visualizer.layout.CodeEntity;
import org.ucombinator.jaam.visualizer.layout.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.HashSet;

public class ClassNode extends ClassTreeNode {

    HashSet<StateVertex> vertices;

    ClassNode(String name, String prefix) {
        super(name,prefix);

        vertices = new HashSet<>();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public HashSet<StateVertex> getChildVertices() {
        return vertices;
    }

    public boolean addVertex(StateVertex vertex) {

        assert vertex instanceof CodeEntity;
        assert ((CodeEntity) vertex).getClassName().startsWith(this.getFullName()); //Maybe shoulbe fullCompare...

        vertices.add(vertex);
        return true;
    }

    public void build(TreeItem<ClassTreeNode> parent) {
        CheckBoxTreeItem<ClassTreeNode> item = buildTreeItem(parent);

        item.setGraphic(Main.getIconFont().create(FontAwesome.Glyph.COPYRIGHT).color(Color.ORANGE));

    }


}
