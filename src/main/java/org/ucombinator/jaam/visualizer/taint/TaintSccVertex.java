package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.layout.LayoutAlgorithm;

import java.util.*;

public class TaintSccVertex extends TaintVertex {

    private Color defaultColor = Color.DARKGREY;
    private LayoutAlgorithm.LAYOUT_ALGORITHM innerLayout = LayoutAlgorithm.LAYOUT_ALGORITHM.DFS;

    public TaintSccVertex(String label)
    {
        super(label, VertexType.SCC, true);
        this.color = defaultColor;
    }

    public TaintSccVertex(String label, LayoutAlgorithm.LAYOUT_ALGORITHM preferredLayout) {
        this(label);

        innerLayout = preferredLayout;
    }

    public TaintSccVertex copy() {
        return new TaintSccVertex(this.getLabel());
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

    public ArrayList<TaintVertex> getLineSortedChildren() {

        ArrayList<TaintVertex> vertices = new ArrayList<>(this.getInnerGraph().getVertices());

        vertices.sort(Comparator.comparing(TaintVertex::getLabel));

        return vertices;
    }
}
