package org.ucombinator.jaam.visualizer.layout;

import java.util.HashSet;

/**
 * Created by timothyjohnson on 3/20/17.
 */
public class LayoutRootVertex extends AbstractLayoutVertex {

    public LayoutRootVertex() {
        super("root", VertexType.ROOT, false);
    }

    public String getRightPanelContent() {
        return "Root vertex";
    }

    public String getShortDescription() {
        return "Root vertex";
    }

    public boolean searchByMethod(String query) {
        boolean found = false;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            found = found || v.searchByMethod(query);
        }

        this.setHighlighted(found);
        return found;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices()
    {
        HashSet<LayoutMethodVertex> methodVertices = new HashSet<LayoutMethodVertex>();
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            if(v instanceof LayoutMethodVertex)
                methodVertices.add((LayoutMethodVertex) v);
            else
                methodVertices.addAll(v.getMethodVertices());
        }

        return methodVertices;
    }
}
