package org.ucombinator.jaam.visualizer.state;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

import java.util.ArrayList;
import java.util.List;

public class StateSccVertex extends StateVertex {

    public static final Color defaultColor = Color.DARKGREY;

    public StateSccVertex(String label)
    {
        super(label, AbstractLayoutVertex.VertexType.SCC, true);
        this.color = defaultColor;
    }

    public StateSccVertex copy() {
        return new StateSccVertex(this.getLabel());
    }

    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = false;
        for (StateVertex v : this.getInnerGraph().getVertices()) {
            found = v.searchByMethod(query, mainTab) || found;
        }

        if (found) {
            this.setHighlighted(true);
            mainTab.getStateHighlighted().add(this);
        }

        return found;
    }

    @Override
    public String getLongText() {
        StringBuilder text = new StringBuilder("SCC contains:\n");
        int k = 0;
        for (StateVertex i : getInnerGraph().getVertices()) {
            text.append(k++ + "  " + i.getLabel() + "\n");
        }

        return text.toString();
    }

    public List<StateVertex> expand() {
        List<StateVertex> expandedVertices = new ArrayList<>();
        for (StateVertex v : this.getInnerGraph().getVertices()) {
            expandedVertices.addAll(v.expand());
        }
        return expandedVertices;
    }
}
