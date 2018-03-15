package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TaintStmtVertex extends TaintVertex {

    private static final Color defaultColor = Color.GREEN;

    ArrayList<TaintAddress> taintAddresses;
    String stmt;

    public TaintStmtVertex(ArrayList<TaintAddress> taintAddresses) {
        super(taintAddresses.toString(), VertexType.TAINT_STMT, true);
        taintAddresses.forEach(this.getInnerGraph()::addVertex);
        this.taintAddresses = new ArrayList<>();
        this.taintAddresses.addAll(taintAddresses);
        stmt = this.taintAddresses.get(0).getAddress().stmt().toString();
        this.color = defaultColor;
    }

    public TaintStmtVertex(String stmt, Set<TaintVertex> taintVertices) {
        super(taintVertices.toString(), VertexType.TAINT_STMT, true);
        taintVertices.forEach(this.getInnerGraph()::addVertex);
        this.taintAddresses = new ArrayList<>();
        taintVertices.forEach(v -> this.taintAddresses.add((TaintAddress) v));
        this.stmt = stmt;
        this.color = defaultColor;
    }

    public TaintStmtVertex copy() {
        return new TaintStmtVertex(new ArrayList<>(taintAddresses));
    }

    @Override
    public HashSet<String> getMethodNames() {
        return this.taintAddresses.get(0).getMethodNames();
    }

    public ArrayList<TaintAddress> getAddresses() {
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
}
