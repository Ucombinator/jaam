package org.ucombinator.jaam.visualizer.state;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

import java.util.HashSet;

public class StateClassVertex extends StateVertex {

    String className;
    private Color defaultColor = Color.DARKGREY;

    public StateClassVertex(String className) {
        super("className", AbstractLayoutVertex.VertexType.CLASS, true);
        this.className = className;
    }

    public StateClassVertex copy() {
        return new StateClassVertex("className");
    }

    public String getRightPanelContent() {
        return "Class: " + className;
    }

    public HashSet<String> getClassNames() {
        HashSet<String> set = new HashSet<>();
        set.add(className);
        return set;
    }

    public HashSet<StateMethodVertex> getMethodVertices() {
        HashSet<StateMethodVertex> methodVertices = new HashSet<>();
        for(StateVertex v : this.getChildGraph().getVertices()) {
            methodVertices.addAll(v.getMethodVertices());
        }
        return methodVertices;
    }

    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = false;
        for(StateVertex v : this.getChildGraph().getVertices()) {
            found = v.searchByMethod(query, mainTab) || found;
        }

        if(found) {
            //this.setHighlighted(found);
            //mainTab.getHighlighted().add(this);
        }

        return found;
    }
}
