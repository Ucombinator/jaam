package org.ucombinator.jaam.visualizer.classTree;

import javafx.scene.control.CheckBoxTreeItem;
import org.controlsfx.glyphfont.FontAwesome;
import org.ucombinator.jaam.visualizer.controllers.CodeViewController;
import org.ucombinator.jaam.visualizer.layout.CodeEntity;
import org.ucombinator.jaam.visualizer.state.StateLoopVertex;
import org.ucombinator.jaam.visualizer.state.StateMethodVertex;
import org.ucombinator.jaam.visualizer.state.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.HashSet;

public class MethodNode extends ClassTreeNode implements Comparable<MethodNode> {

    private String className;
    private String methodName;

    private StateMethodVertex methodVertex;
    private HashSet<StateLoopVertex> loopVertices;

    public MethodNode(StateVertex v) {
        this.className = ((CodeEntity) v).getClassName();
        this.methodName = ((CodeEntity) v).getMethodName();

        methodVertex = null;
        this.loopVertices = new HashSet<>();

        addVertex(v);
    }

    public void addVertex(StateVertex v) {
        if (v instanceof StateMethodVertex) {
            addVertex((StateMethodVertex)v);
        }
        else if (v instanceof StateLoopVertex) {
            addVertex((StateLoopVertex)v);
        }
    }

    public void addVertex(StateLoopVertex v) {
        loopVertices.add(v);
    }

    public void addVertex(StateMethodVertex v) {
        assert methodVertex == null;
        methodVertex = v;
    }

    public String getName() {
        return className + "." + methodName;
    }

    @Override
    public String toString() {
        return getMethodName();
    }

    @Override
    public HashSet<StateVertex> getChildVertices() {
        HashSet<StateVertex> result = new HashSet<>();
        result.add(methodVertex);

        result.addAll(loopVertices);

        return result;
    }

    @Override
    public void handleDoubleClick(CodeViewController codeView) {
        codeView.displayCodeTab(this.getClassName(), this.getMethodName());
    }

    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public void build(CheckBoxTreeItem<ClassTreeNode> parent) {
        CheckBoxTreeItem<ClassTreeNode> item = buildTreeItem(parent);

        if (methodVertex == null) {
            System.out.println("Warning: Method vertex was null " + this.getName());
            return;
        }

        item.setGraphic(Main.getIconFont().create(FontAwesome.Glyph.SQUARE).color(methodVertex.getColor()));
    }

    @Override
    public int compareTo(MethodNode o) {
        return methodName.compareTo(o.methodName);
    }
}
