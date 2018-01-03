package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;
import java.util.HashSet;

public class TaintSccVertex extends TaintVertex {

    private Color defaultColor = Color.DARKGREY;

    public TaintSccVertex(int id, String label)
    {
        super(label, VertexType.SCC, true);
        this.color = defaultColor;
    }

    public HashSet<String> getMethodNames() {
        HashSet<String> methodNames = new HashSet<>();
        for(TaintVertex v : this.getInnerGraph().getVertices()) {
            methodNames.addAll(v.getMethodNames());
        }
        return methodNames;
    }
}
