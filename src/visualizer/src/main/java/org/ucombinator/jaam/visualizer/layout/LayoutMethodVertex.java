package org.ucombinator.jaam.visualizer.layout;

import org.ucombinator.jaam.visualizer.gui.VizPanel;

import java.util.HashSet;

/**
 * Created by timothyjohnson on 2/15/17.
 */
public class LayoutMethodVertex extends AbstractLayoutVertex {

    private String methodName;

    public LayoutMethodVertex(String methodName, boolean drawEdges) {
        super(methodName, VertexType.METHOD, drawEdges);
        this.methodName = methodName;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getRightPanelContent() {
        return "Method vertex: " + this.getMethodName();
    }

    public String getShortDescription() {
        return this.getMethodName();
    }

    public boolean searchByMethod(String query, VizPanel mainPanel) {
        boolean found = query.contains(this.getMethodName());
        this.setHighlighted(found, mainPanel);

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            v.searchByMethod(query, mainPanel);
        }

        return found;
    }

    public void computeLoopHeight()
    {
        this.setLoopHeight(0);
    }

    public HashSet<LayoutMethodVertex> getMethodVertices() {
        HashSet<LayoutMethodVertex> result = new HashSet<LayoutMethodVertex>();
        result.add(this);
        return result;
    }
}
