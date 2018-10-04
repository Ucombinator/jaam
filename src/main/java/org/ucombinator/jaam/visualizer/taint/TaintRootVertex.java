package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.GraphTransform;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.layout.LayoutAlgorithm;

import java.util.*;

public class TaintRootVertex extends TaintVertex {

    private static final Color defaultColor = Color.WHITE;

    public TaintRootVertex() {
        super("root", VertexType.ROOT, false);
        this.color = defaultColor;
    }

    @Override
    public TaintRootVertex copy() {
        return new TaintRootVertex();
    }

    @Override
    public void onMouseClick(MouseEvent event) {}

    public GraphTransform<TaintRootVertex, TaintVertex> constructVisibleGraph(Set<TaintVertex> verticesToDraw) {
        return GraphUtils.constructVisibleGraph(this, verticesToDraw::contains, TaintEdge::new);
    }

    public GraphTransform<TaintRootVertex, TaintVertex> constructVisibleGraphExcept(Set<TaintVertex> verticesToHide) {
        return GraphUtils.constructVisibleGraph(this, (TaintVertex v) -> !verticesToHide.contains(v), TaintEdge::new);
    }

    public HashSet<String> getMethodNames() {
        return new HashSet<>();
    }

    @Override
    public boolean hasField() {
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
    public List<TaintVertex> expand() {
        List<TaintVertex> expandedVertices = new ArrayList<>();
        for (TaintVertex v : this.getInnerGraph().getVertices()) {
            expandedVertices.addAll(v.expand());
        }
        return expandedVertices;
    }

    @Override
    public LayoutAlgorithm.LAYOUT_ALGORITHM getPreferredLayout() {
        return LayoutAlgorithm.LAYOUT_ALGORITHM.DFS;
    }
}
