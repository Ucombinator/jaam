package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class LayoutLoopVertex extends AbstractLayoutVertex {

    private int startJimpleIndex;
    private LayoutMethodVertex methodVertex;
    private int statementIndex;
    
    private Color defaultColor = Color.LIGHTYELLOW;

    public LayoutLoopVertex(int id, String label, int statementIndex){
    	super(id, label, VertexType.LOOP);
    	this.statementIndex = statementIndex;
    	this.color = defaultColor;
    }

    public String getMethodName() {
        return this.methodVertex.getMethodName();
    }

    public String getRightPanelContent() {
        return "Loop vertex: " + this.getMethodName();
    }

    public String getShortDescription() {
        return this.getMethodName();
    }

    @Override
    public boolean searchByMethod(String query, VizPanelController mainPanel) {
        boolean found = this.methodVertex.getMethodName().contains(query);
        if(found) {
            this.setHighlighted(found);
            mainPanel.getHighlighted().add(this);
        }

        return found;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices() {
        HashSet<LayoutMethodVertex> methods = new LinkedHashSet<LayoutMethodVertex>();
        methods.add(this.methodVertex);
        return methods;
    }


    public void setDefaultColor(){
        this.setColor(defaultColor);
    }
    
    public int getStatementIndex() {
		return statementIndex;
	}
}
