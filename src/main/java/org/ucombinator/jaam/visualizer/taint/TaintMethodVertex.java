package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.layout.LayoutAlgorithm;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.*;

public class TaintMethodVertex extends TaintVertex {

    private Color defaultColor = Color.DEEPSKYBLUE;
    private LayoutAlgorithm.LAYOUT_ALGORITHM innerLayout = LayoutAlgorithm.LAYOUT_ALGORITHM.DFS;

    private String className;
    private String methodName;

    public TaintMethodVertex(String label)
    {
        super(label, VertexType.SCC, true);
        this.color = defaultColor;

        this.className = null;
        this.methodName = null;
    }

    public TaintMethodVertex(String className, String methodName, LayoutAlgorithm.LAYOUT_ALGORITHM preferredLayout) {
        this(className + "." + methodName);

        innerLayout = preferredLayout;

        this.className = className;
        this.methodName = methodName;

        if (!Main.getSelectedMainTabController().codeViewController.haveCode(className)) {
            this.color = Color.DARKGRAY;
            this.setExpanded(false);
        }

    }

    public TaintMethodVertex copy() {
        return new TaintMethodVertex(this.getLabel());
    }

    public HashSet<String> getMethodNames() {
        HashSet<String> methodNames = new HashSet<>();
        for (TaintVertex v : this.getInnerGraph().getVertices()) {
            methodNames.addAll(v.getMethodNames());
        }
        return methodNames;
    }

    @Override
    public boolean hasField() {
        for (TaintVertex v : this.getInnerGraph().getVertices()) {
            if (v.hasField()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void getFields(Collection<TaintAddress> store) {
        this.getInnerGraph().getVertices().forEach(v -> v.getFields(store));
    }

    @Override
    public String getStmtString() {
        return null;
    }

    @Override
    public LayoutAlgorithm.LAYOUT_ALGORITHM getPreferredLayout() {
        return innerLayout;
    }

    public String getRightPanelString() {

        if(getInnerGraph().isEmpty()) return "Empty method vertex\n";

        Set<TaintVertex> vertices = this.getInnerGraph().getVertices();

        StringBuilder builder = new StringBuilder(className + "\n" + methodName + "\n");



        for(TaintVertex v : vertices) {
            if (v.getStmtString() == null) continue;

            builder.append("\t" + v.getStmtString() + "\n");
        }

        return builder.toString();
    }

    @Override
    public List<TaintVertex> expand() {
        List<TaintVertex> expandedVertices = new ArrayList<>();
        for (TaintVertex v : this.getInnerGraph().getVertices()) {
            expandedVertices.addAll(v.expand());
        }
        return expandedVertices;
    }
}
