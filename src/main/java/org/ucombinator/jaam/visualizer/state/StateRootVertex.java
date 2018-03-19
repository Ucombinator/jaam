package org.ucombinator.jaam.visualizer.state;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class StateRootVertex extends StateVertex {

    private static final Color defaultColor = Color.WHITE;

    public StateRootVertex() {
        super("root", AbstractLayoutVertex.VertexType.ROOT, false);
        this.color = defaultColor;
    }

    public StateRootVertex copy() {
        return new StateRootVertex();
    }

    @Override
    public void onMouseClick(MouseEvent event) {}

    public StateRootVertex constructVisibleGraphExcept(Set<StateVertex> verticesToHide) {
        return (StateRootVertex) GraphUtils.constructVisibleGraph(this, (StateVertex v) -> !verticesToHide.contains(v), StateEdge::new);
    }

    public String getRightPanelContent() {
        return "Root vertex";
    }

    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = false;
        for(StateVertex v : this.getChildGraph().getVertices()) {
            found = v.searchByMethod(query, mainTab) || found;
        }

        if(found) {
            this.setHighlighted(true);
            mainTab.getVizHighlighted().add(this);
        }

        return found;
    }
}
