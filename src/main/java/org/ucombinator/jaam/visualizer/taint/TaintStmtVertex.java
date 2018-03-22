package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;

import java.util.*;

public class TaintStmtVertex extends TaintVertex {

    private static final Color defaultColor = Color.GREEN;

    ArrayList<TaintAddress> taintAddresses;
    String stmt;

    public TaintStmtVertex(List<? extends TaintVertex> taintAddresses) {
        super(taintAddresses.toString(), VertexType.TAINT_STMT, true);
        taintAddresses.forEach(this.getChildGraph()::addVertex);
        this.taintAddresses = new ArrayList<>();
        taintAddresses.forEach(address -> this.taintAddresses.add((TaintAddress) address));
        stmt = this.taintAddresses.get(0).getAddress().stmt().toString();
        this.color = defaultColor;
    }

    public TaintStmtVertex copy() {
        return new TaintStmtVertex(new ArrayList<>(taintAddresses));
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
}
