package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.serializer.LoopLoopNode;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class LayoutLoopVertex extends AbstractLayoutVertex implements Cloneable {

    private static final Color defaultColor = Color.LIGHTYELLOW;

    private int statementIndex;

    private LoopLoopNode compilationUnit;

    public LayoutLoopVertex(int id, String label, int statementIndex, LoopLoopNode compilationUnit){
    	super(id, label, VertexType.LOOP);
    	this.setDefaultColor();

    	this.statementIndex = statementIndex;

    	this.compilationUnit = compilationUnit;
    }
    public String getClassName() {
        return this.compilationUnit.method().getDeclaringClass().getName();
    }

    public String getShortClassName() {
        return this.compilationUnit.method().getDeclaringClass().getShortName();
    }

    public String getMethodName() {
        return this.compilationUnit.method().getName();
    }


    public LoopLoopNode getCompilationUnit() {
        return compilationUnit;
    }

    public String getRightPanelContent() {
        return "Loop vertex: " + this.getMethodName();
    }

    public String getShortDescription() {
        return this.getMethodName();
    }

    @Override
    public boolean searchByMethod(String query, VizPanelController mainPanel) {
        boolean found = this.getMethodName().contains(query);
        if(found) {
            this.setHighlighted(found);
            mainPanel.getHighlighted().add(this);
        }

        return found;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices() {
        HashSet<LayoutMethodVertex> methods = new LinkedHashSet<LayoutMethodVertex>();
        return methods;
    }


    public void setDefaultColor(){
        this.setColor(defaultColor);
    }
    
    public int getStatementIndex() {
		return statementIndex;
	}
}
