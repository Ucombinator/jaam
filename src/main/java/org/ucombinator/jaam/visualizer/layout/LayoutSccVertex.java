package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class LayoutSccVertex extends StateVertex {

    private Color defaultColor = Color.DARKGREY;

    public LayoutSccVertex(int id, String label)
    {
        super(id, label, VertexType.SCC);
        this.color = defaultColor;
    }

    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = false;
        for(StateVertex v : this.getVisibleInnerGraph().getVertices()) {
            found = v.searchByMethod(query, mainTab) || found;
        }

        if(found) {
            //this.setHighlighted(found);
            //mainTab.getHighlighted().add(this);
        }

        return found;
    }

    public String getRightPanelContent() {
        return "SCC vertex: " + this.getId();
    }

    public HashSet<LayoutMethodVertex> getMethodVertices() {
        HashSet<LayoutMethodVertex> methods = new LinkedHashSet<LayoutMethodVertex>();
        return methods;
    }

    public HashSet<String> getClassNames() {
        HashSet<String> classNames = new HashSet<>();
        for(StateVertex v : this.getImmutableInnerGraph().getVertices()) {
            classNames.addAll(v.getClassNames());
        }
        return classNames;
    }
}
