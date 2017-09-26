package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;

import java.util.LinkedHashSet;

public class LayoutMethodVertex extends AbstractLayoutVertex {

    String methodName;
    private Color defaultColor = Color.DEEPSKYBLUE;

    public LayoutMethodVertex(int id, String label){
    	super(id, label, VertexType.METHOD);
    	this.methodName = label;
    	this.color = defaultColor;
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
