package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;

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
            //this.setHighlighted(found);
            //mainTab.getVizHighlighted().add(this);
        }

        return found;
    }

    public HashSet<StateMethodVertex> getMethodVertices()
    {
        HashSet<StateMethodVertex> methodVertices = new LinkedHashSet<StateMethodVertex>();
        for(StateVertex v : this.getChildGraph().getVertices()) {
            if(v instanceof StateMethodVertex)
                methodVertices.add((StateMethodVertex) v);
            else
                methodVertices.addAll(v.getMethodVertices());
        }

        return methodVertices;
    }

    public HashSet<String> getClassNames() {
        HashSet<String> classNames = new HashSet<>();
        for(StateVertex v : this.getChildGraph().getVertices()) {
            classNames.addAll(v.getClassNames());
        }
        return classNames;
    }
}
