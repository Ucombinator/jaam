package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class LayoutLoopVertex extends AbstractLayoutVertex implements Cloneable {

    private static final Color defaultColor = Color.LIGHTYELLOW;

    private int startJimpleIndex;
    private String className;
    private String methodName;
    private int statementIndex;

    public LayoutLoopVertex(int id, String label, int statementIndex, String className, String methodName){
    	super(id, label, VertexType.LOOP);
    	this.setDefaultColor();

    	this.statementIndex = statementIndex;
        this.className  = className;
        this.methodName = methodName;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getRightPanelContent() {
        return "Loop vertex: " + this.getMethodName();
    }

    public String getShortDescription() {
        return this.getMethodName();
    }

    @Override
    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = this.methodName.contains(query);
        if(found) {
            this.setHighlighted(found);
            mainTab.getHighlighted().add(this);
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
