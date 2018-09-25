package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.tools.taint3.Address;
import org.ucombinator.jaam.tools.taint3.Address.Return;
import org.ucombinator.jaam.tools.taint3.Address.Parameter;
import org.ucombinator.jaam.tools.taint3.Address.Throws;
import org.ucombinator.jaam.tools.taint3.Address.This;
import org.ucombinator.jaam.tools.taint3.Address.Lambda;
import org.ucombinator.jaam.tools.taint3.Address.StaticField;
import org.ucombinator.jaam.tools.taint3.Address.InstanceField;
import org.ucombinator.jaam.tools.taint3.Address.ArrayRef;
import org.ucombinator.jaam.visualizer.layout.LayoutAlgorithm;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class TaintAddress extends TaintVertex {

    // Taken from Taint3, we summarixe only the one we care about
    public enum Type {
        Return, Parameter, Throws,
        Inner, // Any of: Stmt, Value, Local, New, NewArray, NewMultiArray,
        This, Lambda,
        StaticField, InstanceField,
        // The next two we split between primitive and class types
        ArrayRefPrim, ArrayRefClass
    }

    private Address address;
    // This is for the click on a field and see its graph functionality. It should probably
    // be improved by having a new fieldType thta inherits from address.. maybe later
    private final String fieldId;
    private final SootClass sootClass;
    private final SootMethod sootMethod;
    private final Type type;

    public TaintAddress(Address address) {
        super(address.toString(), VertexType.TAINT_ADDRESS, true);
        this.address = address;
        this.type = extractType();
        this.fieldId = extractFieldId();
        this.color = defaultColor();

        sootClass = address.sootClass(); // Might be null
        sootMethod = address.sootMethod();
    }


    public TaintAddress copy() {
        TaintAddress newCopy =  new TaintAddress(this.address);
        newCopy.setColor(this.color);
        return newCopy;
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

    public SootClass getSootClass() {
        return sootClass;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public boolean isArrayRef() {
        return address instanceof ArrayRef;
    }

    @Override
    public String getLongText() {
        return "Taint address:\n" + toString();
    }

    @Override
    public String toString() {
        return this.address.toString();
    }

    private boolean isField() {
        return type == Type.InstanceField || type == Type.StaticField;
    }

    @Override
    public boolean hasField() { return isField(); }

    @Override
    public void getFields(Collection<TaintAddress> store) {
        if(isField())
            store.add(this);
    }

    @Override
    public String getStmtString() {
        if(this.address.stmt() != null) {
            return this.address.stmt().toString();
        } else {
            return null;
        }
    }

    @Override
    public String getClassName() {
        if (sootClass == null) {
            return null;
        }
        else {
            return sootClass.getName();
        }
    }

    @Override
    public String getMethodName() {
        if (sootMethod != null) {
            return sootMethod.getName();
        }
        else {
            return null;
        }
    }

    @Override
    public List<TaintVertex> expand() {
        List<TaintVertex> expandedVertices = new ArrayList<>();
        expandedVertices.add(this);
        return expandedVertices;
    }

    // No preferred layout
    @Override
    public LayoutAlgorithm.LAYOUT_ALGORITHM getPreferredLayout() {
        return null;
    }

    private Type extractType() {

        if (address instanceof Return) { return Type.Return; }
        if (address instanceof Parameter) { return Type.Return; }
        if (address instanceof Throws) { return Type.Return; }
        if (address instanceof This) { return Type.Return; }
        if (address instanceof Lambda) { return Type.Return; }
        if (address instanceof StaticField) { return Type.StaticField; }
        if (address instanceof InstanceField) { return Type.InstanceField; }
        if (address instanceof ArrayRef) {
            soot.Type a = ((ArrayRef)address).typ();
            if (a instanceof soot.PrimType) { return Type.ArrayRefPrim; }
            else { return Type.ArrayRefClass; }
        }
        return Type.Inner;
    }

    private String extractFieldId() {
        if (type != Type.StaticField || type != Type.InstanceField) return null;

        String name;
        if (type == Type.StaticField) {
            name = ((StaticField)address).sootField().getName();
        }
        else {
            name = ((InstanceField)address).sootField().getName();
        }
        return address.sootClass().getName() + ":" + name;
    }

    private Color defaultColor() {
        switch (type) {
            case Return: case Parameter: case Throws: return Color.LIGHTGREEN;
            case Inner: return Color.BLUE;
            case This: return Color.HOTPINK;
            case Lambda: return Color.ORCHID;
            case StaticField: return Color.BROWN;
            case InstanceField: return Color.SADDLEBROWN;
            case ArrayRefPrim: return Color.LIGHTGRAY;
            case ArrayRefClass: return Color.DARKGRAY;
        }
        // Shouldn't happen
        return Color.BLACK;
    }

}
