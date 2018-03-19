package org.ucombinator.jaam.visualizer.state;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class StateSccVertex extends StateVertex {

    private Color defaultColor = Color.DARKGREY;



    public StateSccVertex(int id, String label)
    {
        super(id, label, AbstractLayoutVertex.VertexType.SCC);
        this.color = defaultColor;
    }

    public StateSccVertex copy() {
        return new StateSccVertex(this.getId(), this.getLabel());
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

    public String getRightPanelContent() {
        return "SCC vertex: " + this.getId();
    }

    public HashSet<StateMethodVertex> getMethodVertices() {
        HashSet<StateMethodVertex> methods = new LinkedHashSet<StateMethodVertex>();
        return methods;
    }

    public HashSet<String> getClassNames() {
        HashSet<String> classNames = new HashSet<>();
        for(StateVertex v : this.getChildGraph().getVertices()) {
            classNames.addAll(v.getClassNames());
        }
        return classNames;
    }
}
