package org.ucombinator.jaam.visualizer.layout;

import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

import java.util.HashSet;

/**
 * Created by timothyjohnson on 2/15/17.
 */
public class LayoutChainVertex extends AbstractLayoutVertex {

    public LayoutChainVertex(boolean drawEdges) {
        super("", VertexType.CHAIN, drawEdges);
    }

    public String getRightPanelContent() {
        return "Chain vertex, size = " + this.getInnerGraph().getVertices().size() + "\n";
    }

    public String getShortDescription() {
        return "Chain vertex, size = " + this.getInnerGraph().getVertices().size();
    }

    public GUINode.ShapeType getShape() {
        return GUINode.ShapeType.RECTANGLE;
    }

    public boolean searchByMethod(String query, VizPanel mainPanel) {
        boolean found = false;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            found = v.searchByMethod(query, mainPanel) || found;
        }

        this.setHighlighted(found, mainPanel);
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
