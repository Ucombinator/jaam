package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;

import java.util.LinkedHashSet;

public class LayoutMethodVertex extends AbstractLayoutVertex {

    String methodName;
    private Color methodColor = Color.DEEPSKYBLUE;
    private Color loopColor   = Color.ORANGE;

    
    public LayoutMethodVertex(int id, String label){
    	super(id, label);
    	this.methodName = label;
    	this.color = methodColor;
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

        if(this.getInnerGraph().getRoot().getLabel().startsWith("Method"))
        {
            this.setColor(methodColor);
        }
        else if(this.getInnerGraph().getRoot().getLabel().startsWith("Loop"))
        {
            this.setColor(loopColor);
        }
        else
        {
            this.setColor(Color.DARKRED);
        }
    }
}
