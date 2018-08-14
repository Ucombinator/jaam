package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;
import soot.SootClass;
import soot.SootMethod;

import java.util.*;

public class TaintStmtVertex extends TaintVertex {

    private static final Color defaultColor = Color.GREEN;

    ArrayList<TaintAddress> taintAddresses;
    String stmt;

    SootClass sootClass;
    SootMethod sootMethod;

    public TaintStmtVertex(List<? extends TaintVertex> taintAddresses) {
        super(taintAddresses.toString(), VertexType.TAINT_STMT, true);
        taintAddresses.forEach(this.getInnerGraph()::addVertex);
        this.taintAddresses = new ArrayList<>();
        taintAddresses.forEach(address -> this.taintAddresses.add((TaintAddress) address));
        this.stmt = this.taintAddresses.get(0).getAddress().stmt().toString();
        this.color = defaultColor;
        this.setExpanded(false);

        this.sootClass = null;
        this.sootMethod = null;

        this.taintAddresses.forEach(a -> {
            if (a.getSootClass() != null) {
                if (this.sootClass == null) {
                    this.sootClass = a.getSootClass();
                }
                assert this.sootClass == a.getSootClass();
            }

            if (a.getSootMethod() != null) {
                if (this.sootMethod == null) {
                    this.sootMethod = a.getSootMethod();
                }
                assert this.sootMethod == a.getSootMethod();
            }
        });
        assert this.sootClass != null;
        assert this.sootMethod != null;
    }

    public TaintStmtVertex copy() {
        TaintStmtVertex newCopy = new TaintStmtVertex(new ArrayList<>(taintAddresses));
        newCopy.setColor(this.color);
        return newCopy;
    }

    @Override
    public HashSet<String> getMethodNames() {
        return this.taintAddresses.get(0).getMethodNames();
    }

    public List<TaintAddress> getAddresses() {
        return this.taintAddresses;
    }

    public String getStmt() {
        return stmt;
    }

    @Override
    public boolean hasField() {
       for (TaintAddress a : taintAddresses) {
           if(a.hasField())
               return true;
       }
       return false;
    }

    @Override
    public void getFields(Collection<TaintAddress> store) {
        taintAddresses.forEach(a -> a.getFields(store));
    }

    @Override
    public String getStmtString() {
        return this.stmt;
    }

    @Override
    public String getClassName() {
        return sootClass.getName();
    }

    @Override
    public String getMethodName() {
        return sootMethod.getName();
    }

    @Override
    public List<TaintVertex> expand() {
        List<TaintVertex> expandedVertices = new ArrayList<>();
        expandedVertices.add(this);
        return expandedVertices;
    }
}
