package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.tools.taint3.Address;
import org.ucombinator.jaam.tools.taint3.Address.StaticField;
import org.ucombinator.jaam.tools.taint3.Address.InstanceField;
import soot.SootField;

import java.util.Collection;
import java.util.HashSet;

public class TaintAddress extends TaintVertex {

    private static final Color defaultColor = Color.LIGHTGREEN;

    private Address address;
    private final boolean isField;
    private final String fieldId;

    public TaintAddress(Address address) {
        super(address.toString(), VertexType.TAINT_ADDRESS, true);
        this.color = defaultColor;
        this.address = address;

        this.isField = address instanceof StaticField || address instanceof InstanceField;
        if (address instanceof StaticField) {
            SootField f = ((StaticField) address).sootField();

            fieldId = address.sootClass().getName() + ":" + f.getName();
        }
        else if (address instanceof InstanceField) {
            SootField f = ((InstanceField) address).sootField();
            fieldId = address.sootClass().getName() + ":" + f.getName();
        }
        else {
           fieldId = null;
        }
    }

    public TaintAddress copy() {
        return new TaintAddress(this.address);
    }

    public HashSet<String> getMethodNames() {
        if(address.sootMethod() != null) {
            HashSet<String> result = new HashSet<>();
            result.add(address.sootMethod().getName());
            return result;
        }
        else {
            return new HashSet<>();
        }
    }

    public Address getAddress() { return this.address; }

    public String getFieldId() { return fieldId; }

    @Override
    public String toString() {
        return this.address.toString();
    }

    @Override
    public boolean hasField() { return this.isField; }

    @Override
    public void getFields(Collection<TaintAddress> store) {
        if(isField)
            store.add(this);
    }


}
