package org.ucombinator.jaam.visualizer.state;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.serializer.LoopMethodNode;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.MethodEntity;
import soot.SootClass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StateMethodVertex extends StateVertex implements MethodEntity {

    public static final Color defaultColor = Color.DEEPSKYBLUE;
    private static final Color recursiveColor = Color.DARKGRAY;

    private final LoopMethodNode compilationUnit;

    public StateMethodVertex(int id, String label, LoopMethodNode compilationUnit){
    	super(id, label, AbstractLayoutVertex.VertexType.METHOD);
    	this.setDefaultColor();
    	this.compilationUnit = compilationUnit;
    }

    public StateMethodVertex copy() {
        return new StateMethodVertex(this.getId(), this.getLabel(), this.compilationUnit);
    }

    public String getClassName() {
        return this.compilationUnit.method().getDeclaringClass().getName();
    }

    public String getShortClassName() {
        return this.compilationUnit.method().getDeclaringClass().getShortName();
    }

    public Set<String> getClassNames() {
        HashSet<String> set = new HashSet<>();
        set.add(this.getClassName());
        return set;
    }

    @Override
    public String getLongText() {
        return "Method:\n  Class: "
                + getClassDeclaration() + "\n  Method: "
                + getMethodName()       + "\n  Signature: " + getLabel();
    }

    public String getClassDeclaration() {
        StringBuilder classDecl = new StringBuilder("class");

        SootClass declaringClass = this.compilationUnit.method().getDeclaringClass();

        classDecl.append(" " + declaringClass.getShortName());

        if(declaringClass.hasSuperclass() && declaringClass.getSuperclass().getShortName().compareTo("Object") != 0) {
            classDecl.append(" extends " + declaringClass.getSuperclass().getShortName());
        }

        if(declaringClass.getInterfaceCount() > 0) {
            classDecl.append(" implements ");
            declaringClass.getInterfaces().stream().forEach(i -> classDecl.append(i.getShortName() + ", "));
        }

        return classDecl.toString();
    }

    public String getMethodName() {
        return this.compilationUnit.method().getName();
    }

    public LoopMethodNode getCompilationUnit() {
        return compilationUnit;
    }

    public String getRightPanelContent() {
        return "Method vertex: " + this.getId();
    }

    @Override
    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = this.getMethodName().toLowerCase().contains(query);
        System.out.println(this.getMethodName() + " Method Checking:");
        if(found) {
            System.out.println("Found " + this);
            this.setHighlighted(found);
            mainTab.getStateHighlighted().add(this);
        }

        for(StateVertex v : this.getInnerGraph().getVertices()) {
            v.searchByMethod(query, mainTab);
        }

        return found;
    }

    public Set<StateMethodVertex> getMethodVertices() {
        HashSet<StateMethodVertex> result = new HashSet<>();
        result.add(this);
        return result;
    }

    public Color getColor() {
        return this.color;
    }

    private void setDefaultColor(){
        this.color = defaultColor;
    }

    @Override
    public String toString()
    {
        return "Method " + this.getClassName() + ":" + getMethodName();
    }

    public List<StateVertex> expand() {
        List<StateVertex> expandedVertices = new ArrayList<>();
        expandedVertices.add(this);
        return expandedVertices;
    }

    public void setRecursiveColor() {
        this.color = recursiveColor;
    }
}
