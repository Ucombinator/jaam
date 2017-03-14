package org.ucombinator.jaam.visualizer.layout;

import java.util.HashSet;

/**
 * Created by timothyjohnson on 2/15/17.
 */
public class LayoutChainVertex extends AbstractLayoutVertex {

    public LayoutChainVertex() {
        super("", VertexType.CHAIN);
    }

    public String getRightPanelContent() {
        return "Chain vertex, size = " + this.getInnerGraph().getVertices().size() + "\n";
    }

    public String getShortDescription() {
        return "Chain vertex, size = " + this.getInnerGraph().getVertices().size();
    }

    public boolean searchByMethod(String query) {
        boolean found = false;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            found = found || v.searchByMethod(query);
        }

        this.setHighlighted(found);
        return found;
    }

    public HashSet<AbstractLayoutVertex> getMethodVertices()
    {
        HashSet<AbstractLayoutVertex> methodVertices = new HashSet<AbstractLayoutVertex>();
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            if(v instanceof LayoutMethodVertex)
                methodVertices.add(v);
            else if(v instanceof LayoutChainVertex)
                methodVertices.addAll(((LayoutChainVertex) v).getMethodVertices());
        }

        return methodVertices;
    }
}
