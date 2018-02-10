package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class TaintStmtVertex extends TaintVertex {

    private static final Color defaultColor = Color.GREEN;

    ArrayList<TaintAddress> taintAddresses;
    String stmt;

    public TaintStmtVertex(ArrayList<TaintAddress> taintAddresses) {
        super(taintAddresses.toString(), VertexType.TAINT_STMT, true);
        this.taintAddresses = taintAddresses;
        stmt = this.taintAddresses.get(0).getAddress().stmt().toString();
        this.color = defaultColor;
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
}
