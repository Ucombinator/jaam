package org.ucombinator.jaam.visualizer.classTree;

import javafx.scene.control.CheckBoxTreeItem;
import org.controlsfx.glyphfont.FontAwesome;
import org.ucombinator.jaam.visualizer.controllers.CodeViewController;
import org.ucombinator.jaam.visualizer.layout.CodeEntity;
import org.ucombinator.jaam.visualizer.layout.LayoutLoopVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutMethodVertex;
import org.ucombinator.jaam.visualizer.layout.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.HashSet;

public class MethodNode extends ClassTreeNode {

    private LayoutMethodVertex methodVertex;
    private HashSet<LayoutLoopVertex> loopVertices;

    private String className;
    private String methodName;

    public MethodNode(StateVertex v) {
        super(((CodeEntity)v).getMethodName(), ((CodeEntity)v).getClassName());

        this.className = ((CodeEntity) v).getClassName();
        this.methodName = ((CodeEntity) v).getMethodName();

        methodVertex = null;
        this.loopVertices = new HashSet<>();

        addVertex(v);
    }

    public void addVertex(StateVertex v) {
        if(v instanceof LayoutMethodVertex) {
            addVertex((LayoutMethodVertex)v);
        }
        else if (v instanceof LayoutLoopVertex) {
            addVertex((LayoutLoopVertex)v);
        }
    }

    public void addVertex(LayoutLoopVertex v) {
        loopVertices.add(v);
    }

    public void addVertex(LayoutMethodVertex v) {
        assert methodVertex == null;
        methodVertex = v;
    }



    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public HashSet<StateVertex> getChildVertices() {
        HashSet<StateVertex> result = new HashSet<>();
        result.add(methodVertex);

        result.addAll(loopVertices);

        return result;
    }

    @Override
    public boolean hasCode() {
        return true;
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
            System.out.println("Warning: Method vertex was null " + this.fullName);
            return;
        }

        item.setGraphic(Main.getIconFont().create(FontAwesome.Glyph.SQUARE).color(methodVertex.getColor()));
    }
}
