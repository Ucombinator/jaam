package org.ucombinator.jaam.visualizer.classTree;

import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import org.controlsfx.glyphfont.FontAwesome;
import org.ucombinator.jaam.visualizer.controllers.CodeViewController;
import org.ucombinator.jaam.visualizer.layout.CodeEntity;
import org.ucombinator.jaam.visualizer.state.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.HashMap;
import java.util.HashSet;

public class ClassNode extends ClassTreeNode {

    private String packageName;
    private String className;

    private HashMap<String, MethodNode> methods;
    private HashSet<FieldNode> fields;

    public ClassNode(String fullPackage, String shortClassName) {
        this.packageName = fullPackage;
        this.className = shortClassName;

        methods = new HashMap<>();
        fields = new HashSet<>();
    }

    @Override
    public String toString() {
        return className;
    }

    @Override
    public HashSet<StateVertex> getChildVertices() {

        HashSet<StateVertex> all = new HashSet<>();
        methods.values().stream().forEach(m -> all.addAll(m.getChildVertices()));

        return all;
    }

    @Override
    public void handleDoubleClick(CodeViewController codeView) {
        codeView.displayCodeTab(getName());
    }

    public boolean addVertex(StateVertex vertex) {

        assert vertex instanceof CodeEntity;
        assert ((CodeEntity) vertex).getClassName().startsWith(this.getName()); //Maybe should be fullCompare...

        CodeEntity c = (CodeEntity)vertex;

        MethodNode node = methods.get(c.getMethodName());

        if (node == null) {
            methods.put(c.getMethodName(), new MethodNode(vertex));
        }
        else {
            node.addVertex(vertex);
        }

        return true;
    }

    public void addFields(CodeViewController codeViewController) {
        codeViewController.getFields(this.getName()).forEach(
                fieldName -> fields.add(new FieldNode(fieldName, this.getName()) )
        );

    }

    public void build(TreeItem<ClassTreeNode> parent) {
        CheckBoxTreeItem<ClassTreeNode> item = buildTreeItem(parent);

        item.setGraphic(Main.getIconFont().create(FontAwesome.Glyph.COPYRIGHT).color(Color.ORANGE));

        methods.values().stream().sorted().forEach(m -> m.build(item));
        fields.stream().sorted().forEach(f -> f.build(item));
    }

    public String getName() {
        return packageName + "." + className;
    }

}
