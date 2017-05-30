package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Paint;

import java.util.HashSet;
import java.util.LinkedHashSet;

import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

/**
 * Created by timothyjohnson on 5/4/17.
 *
 * This vertex represents a set of methods that have no loops, and can be removed from our drawing.
 */
public class LayoutShrinkVertex extends AbstractLayoutVertex {

    public LayoutShrinkVertex(boolean drawEdges) {
        super("Layout shrink vertex", VertexType.SHRINK, drawEdges);
        this.setExpanded(false);
    }

    public String getRightPanelContent() {
        return "Layout shrink vertex";
    }

    public String getShortDescription() {
        return "Layout shrink vertex";
    }

    public GUINode.ShapeType getShape() {
        return GUINode.ShapeType.RECTANGLE;
    }

    public boolean searchByMethod(String query, VizPanel mainPanel) {
        boolean found = false;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            found = v.searchByMethod(query, mainPanel) || found;
        }

        this.setHighlighted(found);
        mainPanel.getHighlighted().add(this);

        return found;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices()
    {
        HashSet<LayoutMethodVertex> methodVertices = new LinkedHashSet<LayoutMethodVertex>();
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            methodVertices.addAll(v.getMethodVertices());
        }

        return methodVertices;
    }
}
