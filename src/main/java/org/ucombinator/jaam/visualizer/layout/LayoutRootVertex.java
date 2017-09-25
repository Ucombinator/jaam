package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class LayoutRootVertex extends AbstractLayoutVertex {

    public LayoutRootVertex() {
        super("root", VertexType.ROOT, false);
        this.color = Color.WHITE;
    }

    public String getRightPanelContent() {
        return "Root vertex";
    }

    public String getShortDescription() {
        return "Root vertex";
    }

    public boolean searchByMethod(String query, VizPanelController mainPanel) {
        boolean found = false;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices()) {
            found = found || v.searchByMethod(query, mainPanel);
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

}
