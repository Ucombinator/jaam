package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TaintRootVertex extends TaintVertex {

    private static final Color defaultColor = Color.WHITE;

    public TaintRootVertex() {
        super("root", VertexType.ROOT, false);
        this.color = defaultColor;
    }

    public TaintVertex getVisibleRoot(Set<TaintVertex> verticesToDraw) {
        return this.getImmutableInnerGraph()
                .constructVisibleGraph((TaintVertex v) -> verticesToDraw.contains(v))
                .getRoot();
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
        this.getImmutableInnerGraph().getVertices().stream().forEach(v -> v.getFields(store));
    }
}
