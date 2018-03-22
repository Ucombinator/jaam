package org.ucombinator.jaam.visualizer.state;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.serializer.LoopLoopNode;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.MethodEntity;
import soot.SootClass;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class StateLoopVertex extends StateVertex implements Cloneable, MethodEntity {

    private static final Color defaultColor = Color.LIGHTYELLOW;

    private final int statementIndex;

    private final LoopLoopNode compilationUnit;

    public StateLoopVertex(int id, String label, int statementIndex, LoopLoopNode compilationUnit){
    	super(id, label, AbstractLayoutVertex.VertexType.LOOP);
    	this.setDefaultColor();

    	this.statementIndex = statementIndex;
    	this.compilationUnit = compilationUnit;
    }

    public StateLoopVertex copy() {
        return new StateLoopVertex(this.getId(), this.getLabel(), this.statementIndex, this.compilationUnit);
    }

    public String getClassName() {
        return this.compilationUnit.method().getDeclaringClass().getName();
    }

    public String getShortClassName() {
        return this.compilationUnit.method().getDeclaringClass().getShortName();
    }

    public HashSet<String> getClassNames() {
        HashSet<String> set = new HashSet<>();
        set.add(this.getClassName());
        return set;
    }

    public String getMethodName() {
        return this.compilationUnit.method().getName();
    }

    public String getClassDeclaration() {
        StringBuilder classDecl = new StringBuilder("class");

        this.compilationUnit.method().getDeclaringClass();

        SootClass declaringClass = this.compilationUnit.method().getDeclaringClass();

        classDecl.append(" " + declaringClass.getShortName());

        if(declaringClass.hasSuperclass()) {
            classDecl.append(" extends " + declaringClass.getSuperclass().getShortName());
        }

        if(declaringClass.getInterfaceCount() > 0) {
            classDecl.append(" implements ");
            declaringClass.getInterfaces().stream().forEach(i -> classDecl.append(i.getName() + ", "));
        }

        return classDecl.toString();
    }


    public LoopLoopNode getCompilationUnit() {
        return compilationUnit;
    }

    public String getRightPanelContent() {
        return "Loop vertex: " + this.getMethodName();
    }

    @Override
    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = this.getMethodName().toLowerCase().contains(query);
        System.out.println(this.getMethodName() + " Loop Checking:");
        if(found) {
            System.out.println("\t\t\tFound " + this);
            this.setHighlighted(found);
            mainTab.getVizHighlighted().add(this);
        }

        return found;
    }

    public HashSet<StateMethodVertex> getMethodVertices() {
        HashSet<StateMethodVertex> methods = new LinkedHashSet<>();
        return methods;
    }

    @Override
    public HashSet<String> getMethodNames() {
        HashSet<String> methodNames = new HashSet<>();
        methodNames.add(this.getMethodName());
        return methodNames;
    }

    public void setDefaultColor(){
        this.color = defaultColor;
    }

    public String toString()
    {
        return "Loop " + getClassName() + ":" + getMethodName();
    }

    public int getStatementIndex() {
		return statementIndex;
	}
}
