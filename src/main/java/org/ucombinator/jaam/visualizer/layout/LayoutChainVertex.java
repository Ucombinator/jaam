package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * Created by timothyjohnson on 2/15/17.
 */
public class LayoutChainVertex extends AbstractLayoutVertex {

    private static Color color = Color.GREEN;
    private static Stop[] stops = new Stop[]{new Stop(0.6, color), new Stop(0.4, Color.WHITE)};

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
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices()) {
            found = v.searchByMethod(query, mainPanel) || found;
        }

        this.setHighlighted(found);
        mainPanel.getHighlighted().add(this);

        return found;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices()
    {
        HashSet<LayoutMethodVertex> methodVertices = new LinkedHashSet<LayoutMethodVertex>();
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices()) {
            if(v instanceof LayoutMethodVertex)
                methodVertices.add((LayoutMethodVertex) v);
            else
                methodVertices.addAll(v.getMethodVertices());
        }

        return methodVertices;
    }

    @Override
    public Paint getFill() {
        return new LinearGradient(0, 0, 8, 8, false, CycleMethod.REPEAT, stops);
    }
}
