package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;

import java.util.HashSet;

public class LayoutClassVertex extends StateVertex {

    String className;
    private Color defaultColor = Color.DARKGREY;

    public LayoutClassVertex(String className) {
        super("className", VertexType.CLASS, true);
        this.className = className;
    }

    public String getRightPanelContent() {
        return "Class: " + className;
    }

    public HashSet<String> getClassNames() {
        HashSet<String> set = new HashSet<>();
        set.add(className);
        return set;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices() {
        HashSet<LayoutMethodVertex> methodVertices = new HashSet<>();
        for(StateVertex v : this.getInnerGraph().getVertices()) {
            methodVertices.addAll(v.getMethodVertices());
        }
        return methodVertices;
    }

    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = false;
        for(StateVertex v : this.getInnerGraph().getVisibleVertices()) {
            found = v.searchByMethod(query, mainTab) || found;
        }

        if(found) {
            //this.setHighlighted(found);
            //mainTab.getHighlighted().add(this);
        }

        return found;
    }
}
