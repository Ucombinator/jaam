package org.ucombinator.jaam.visualizer.state;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.util.Loop;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.serializer.LoopLoopNode;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.MethodEntity;
import soot.SootClass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

public class StateLoopVertex extends StateVertex implements Cloneable, MethodEntity {

    enum TYPE { ITERATOR, ARRAY, SIMPLE_COUNT_UP, SIMPLE_COUNT_DOWN, UNKNOWN}

    private static final Color defaultColor = Color.LIGHTYELLOW;
    private static final Color unknownLoopColor = Color.RED;
    private static final Color iteratorLoopColor = Color.LIGHTGOLDENRODYELLOW;
    private static final Color arrayLoopColor = Color.GOLD;
    private static final Color simpleCountUpLoopColor = Color.PERU;
    private static final Color simpleCountDownLoopColor = Color.ORANGE;

    private final int statementIndex;

    private final LoopLoopNode compilationUnit;

    public StateLoopVertex(int id, String label, int statementIndex, LoopLoopNode compilationUnit){
    	super(id, label, AbstractLayoutVertex.VertexType.LOOP);

    	this.statementIndex = statementIndex;
    	this.compilationUnit = compilationUnit;
        this.setColor();
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

    @Override
    public String getLongText() {
        return "Loop:\n  Class: "
                + getClassDeclaration() + "\n  Method: "
                + getMethodName()       + "\n  Index: "
                + getStatementIndex()   + "\n  Signature: " + getLabel()
                + "\n  Loop info: " + getCompilationUnit().loopInfo() + "\n";
    }

    @Override
    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = this.getMethodName().toLowerCase().contains(query);
        System.out.println(this.getMethodName() + " Loop Checking:");
        if(found) {
            System.out.println("\t\t\tFound " + this);
            this.setHighlighted(found);
            mainTab.getStateHighlighted().add(this);
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

    public void setColor(){


        if (this.compilationUnit.loopInfo() instanceof Loop.UnidentifiedLoop) {
            this.color = unknownLoopColor;
        }
        else if (this.compilationUnit.loopInfo() instanceof Loop.IteratorLoop) {
            this.color = iteratorLoopColor;
        }
        else if(this.compilationUnit.loopInfo() instanceof Loop.ArrayLoop) {
            this.color = arrayLoopColor;
        }
        else if(this.compilationUnit.loopInfo() instanceof Loop.SimpleCountUpForLoop) {
            this.color = simpleCountUpLoopColor;
        }
        else if(this.compilationUnit.loopInfo() instanceof Loop.SimpleCountDownForLoop) {
            this.color = simpleCountDownLoopColor;
        }
        else {
            this.color = defaultColor;
        }
    }

    public String toString()
    {
        return "Loop " + getClassName() + ":" + getMethodName();
    }

    public int getStatementIndex() {
		return statementIndex;
	}

    public List<StateVertex> expand() {
        List<StateVertex> expandedVertices = new ArrayList<>();
        expandedVertices.add(this);
        return expandedVertices;
    }
}
