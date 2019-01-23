package org.ucombinator.jaam.visualizer.state;

import javafx.scene.paint.Color;
import javafx.util.Pair;
import org.ucombinator.jaam.util.Loop;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.serializer.LoopLoopNode;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.MethodEntity;
import soot.SootClass;
import soot.Value;

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

    public ArrayList<Value> getLoopValues() {

        ArrayList<Value> result = new ArrayList<>();

        if (this.isUnidentifiedLoop()) {
            return result;
        }

        Loop.LoopInfo loopInfo = getCompilationUnit().loopInfo();

        if (loopInfo instanceof Loop.IteratorLoop) {
            Value value = ((Loop.IteratorLoop) loopInfo).iterable();
            result.add(value);
        }
        else if (loopInfo instanceof Loop.ArrayLoop) {
            Value value = ((Loop.ArrayLoop) loopInfo).iterable();
            result.add(value);
        }
        else if (loopInfo instanceof Loop.SimpleCountUpForLoop) {
            Value valueLower = ((Loop.SimpleCountUpForLoop) loopInfo).lowerBound();
            Value valueUpper = ((Loop.SimpleCountUpForLoop) loopInfo).upperBound();
            Value valueIncrement = ((Loop.SimpleCountUpForLoop) loopInfo).increment();
            result.add(valueLower);
            result.add(valueUpper);
            result.add(valueIncrement);
        }
        else if (loopInfo instanceof Loop.SimpleCountDownForLoop) {
            Value valueLower = ((Loop.SimpleCountDownForLoop) loopInfo).lowerBound();
            Value valueUpper = ((Loop.SimpleCountDownForLoop) loopInfo).upperBound();
            Value valueIncrement = ((Loop.SimpleCountDownForLoop) loopInfo).increment();
            result.add(valueLower);
            result.add(valueUpper);
            result.add(valueIncrement);
        }

        return result;
    }

    public boolean isUnidentifiedLoop() {

        Loop.LoopInfo loopInfo = getCompilationUnit().loopInfo();
        if (loopInfo instanceof Loop.UnidentifiedLoop) return true;

        if (       loopInfo instanceof Loop.IteratorLoop
                || loopInfo instanceof Loop.ArrayLoop
                || loopInfo instanceof Loop.SimpleCountUpForLoop
                || loopInfo instanceof Loop.SimpleCountUpForLoop
        ) {
            return false;
        }
        return true;
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

    public static ArrayList<Pair<Color, String>> getColorLegend() {

        ArrayList<Pair<Color, String>> legend = new ArrayList<>();

        legend.add(new Pair(iteratorLoopColor, "Iterator Loop"));
        legend.add(new Pair(arrayLoopColor, "Array Loop"));
        legend.add(new Pair(simpleCountUpLoopColor, "Simple Count Up Loop"));
        legend.add(new Pair(simpleCountDownLoopColor, "Simple Count Down Loop"));
        legend.add(new Pair(unknownLoopColor, "Unknown Loop"));

        return legend;
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
