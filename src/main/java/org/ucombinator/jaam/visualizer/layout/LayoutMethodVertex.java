package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;

import java.util.LinkedHashSet;

public class LayoutMethodVertex extends AbstractLayoutVertex {

    private static final Color defaultColor = Color.DEEPSKYBLUE;

    private String label;
    private String className;
    private String methodName;

    public LayoutMethodVertex(int id, String label, String className, String methodName){
    	super(id, label, VertexType.METHOD);
    	this.setDefaultColor();

    	this.label      = label;
    	this.className  = className;
    	this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getRightPanelContent() {
        return "Method vertex: " + this.getId();
    }

    public String getShortDescription() {
        return this.getMethodName();
    }

    @Override
    public boolean searchByMethod(String query, VizPanelController mainPanel) {
        boolean found = query.contains(this.getMethodName());
        this.setHighlighted(found);
        mainPanel.getHighlighted().add(this);

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices()) {
            v.searchByMethod(query, mainPanel);
        }

        return found;
    }

    public LinkedHashSet<LayoutMethodVertex> getMethodVertices() {
        LinkedHashSet<LayoutMethodVertex> result = new LinkedHashSet<LayoutMethodVertex>();
        result.add(this);
        return result;
    }

    public void setDefaultColor(){
        this.color = defaultColor;
    }
}
