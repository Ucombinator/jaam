package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class LayoutRootVertex extends StateVertex {

    private static final Color defaultColor = Color.WHITE;

    public LayoutRootVertex() {
        super("root", AbstractLayoutVertex.VertexType.ROOT, false);
        this.color = defaultColor;
    }

    public LayoutRootVertex copy() {
        return new LayoutRootVertex();
    }

    @Override
    public void onMouseClick(MouseEvent event) {}

    public LayoutRootVertex constructVisibleGraphExcept(Set<StateVertex> verticesToHide) {
        return (LayoutRootVertex) GraphUtils.constructVisibleGraph(this, (StateVertex v) -> !verticesToHide.contains(v), StateEdge::new);
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

    public HashSet<LayoutMethodVertex> getMethodVertices()
    {
        HashSet<LayoutMethodVertex> methodVertices = new LinkedHashSet<LayoutMethodVertex>();
        for(StateVertex v : this.getChildGraph().getVertices()) {
            if(v instanceof LayoutMethodVertex)
                methodVertices.add((LayoutMethodVertex) v);
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
