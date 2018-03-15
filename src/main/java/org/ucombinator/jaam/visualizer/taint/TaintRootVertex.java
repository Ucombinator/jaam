package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TaintRootVertex extends TaintVertex {

    private static final Color defaultColor = Color.WHITE;

    public TaintRootVertex() {
        super("root", VertexType.ROOT, false);
        this.color = defaultColor;
    }

    public TaintRootVertex copy() {
        return new TaintRootVertex();
    }

    @Override
    public void onMouseClick(MouseEvent event) {}

    public TaintRootVertex constructVisibleGraph(Set<TaintVertex> verticesToDraw) {
        return (TaintRootVertex) GraphUtils.constructVisibleGraph(this, verticesToDraw::contains, TaintEdge::new);
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
        // TODO: Is this the right graph?
        this.getInnerGraph().getVertices().forEach(v -> v.getFields(store));
    }

    @Override
    public String getStmtString() {
        return null;
    }
}
