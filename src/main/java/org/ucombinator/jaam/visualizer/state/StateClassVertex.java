package org.ucombinator.jaam.visualizer.state;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.ClassEntity;

import java.util.HashSet;

public class StateClassVertex extends StateVertex implements ClassEntity {

    private String className;
    private String shortClassName;
    private Color defaultColor = Color.DARKGREY;

    public StateClassVertex(String className) {
        super(className, AbstractLayoutVertex.VertexType.CLASS, true);
        this.className = className;
        this.shortClassName = ""; // TODO: Compute short class name?
    }

    public StateClassVertex(String className, String shortClassName) {
        super(className, AbstractLayoutVertex.VertexType.CLASS, true);
        this.className = className;
        this.shortClassName = shortClassName;
    }

    public StateClassVertex copy() {
        return new StateClassVertex(className, shortClassName);
    }

    public String getRightPanelContent() {
        return "Class: " + className;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getShortClassName() {
        return shortClassName;
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
