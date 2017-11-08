package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class LayoutSccVertex extends AbstractLayoutVertex {

    private Color defaultColor = Color.DARKGREY;

    public LayoutSccVertex(int id, String label)
    {
        super(id, label, VertexType.SCC);
        this.color = defaultColor;
    }

    public boolean searchByMethod(String query, MainTabController mainTab) {
        return false;
    }

    public String getRightPanelContent() {
        return "SCC vertex: " + this.getId();
    }

    public String getShortDescription() {
        return "I am a SCC vertex!";
    }

    public HashSet<LayoutMethodVertex> getMethodVertices() {
        HashSet<LayoutMethodVertex> methods = new LinkedHashSet<LayoutMethodVertex>();
        return methods;
    }

}
