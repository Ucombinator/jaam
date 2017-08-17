package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class LayoutLoopVertex extends AbstractLayoutVertex {

    private int startJimpleIndex;
    private LayoutMethodVertex methodVertex;
    private int statementIndex;
    
    private Color loopColor = Color.LIGHTYELLOW;


    public LayoutLoopVertex(int id, String label, int statementIndex){
    	super(id, label);
    	this.statementIndex = statementIndex;
    }

	public LayoutLoopVertex(LayoutMethodVertex methodVertex, int startIndex, boolean drawEdges) {
        super(methodVertex.getMethodName() + ", instruction #" + startIndex, VertexType.LOOP, drawEdges);
        this.methodVertex = methodVertex;
        this.startJimpleIndex = startIndex;
    }

    public String getMethodName() {
        return this.methodVertex.getMethodName();
    }

    public String getRightPanelContent() {
        return "Method vertex: " + this.getMethodName() + "\nLoop height: " + this.getLoopHeight() + "\n";
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
        this.setColor(loopColor);
    }
    
    public int getStatementIndex() {
		return statementIndex;
	}
}
