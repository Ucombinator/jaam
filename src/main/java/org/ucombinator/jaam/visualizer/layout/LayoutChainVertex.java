package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class LayoutChainVertex extends AbstractLayoutVertex {

    private static Color color = Color.GREEN;
    private static Stop[] stops = new Stop[]{new Stop(0.6, color), new Stop(0.4, Color.WHITE)};

    public LayoutChainVertex(boolean drawEdges) {
        super("", VertexType.CHAIN, drawEdges);
    }

    public String getRightPanelContent() {
        return "Chain vertex, size = " + this.getInnerGraph().getVisibleVertices().size() + "\n";
    }

    public String getShortDescription() {
        return "Chain vertex, size = " + this.getInnerGraph().getVisibleVertices().size();
    }

    @Override
    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = false;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices()) {
            found = v.searchByMethod(query, mainTab) || found;
        }

        this.setHighlighted(found);
        mainTab.getHighlighted().add(this);

        return found;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices()
    {
        HashSet<LayoutMethodVertex> methodVertices = new LinkedHashSet<LayoutMethodVertex>();
        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices()) {
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
