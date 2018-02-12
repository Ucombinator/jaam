package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;

import java.util.Collection;
import java.util.HashSet;

public class TaintRootVertex extends TaintVertex {

    private static final Color defaultColor = Color.WHITE;

    public TaintRootVertex() {
        super("root", VertexType.ROOT, false);
        this.color = defaultColor;
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
        this.getInnerGraph().getVertices().stream().forEach(v -> v.getFields(store));
    }
}
