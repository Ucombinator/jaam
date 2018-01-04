package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.tools.taint3.Address;
import java.util.HashSet;

public class TaintAddress extends TaintVertex {

    private static final Color defaultColor = Color.LIGHTGREEN;

    private Address address;

    public TaintAddress(Address address) {
        super(address.toString(), VertexType.TAINT_ADDRESS, true);
        this.color = defaultColor;
        this.address = address;
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

    public Address getAddress() {
        return this.address;
    }

    @Override
    public String toString() {
        return this.address.toString();
    }
}
