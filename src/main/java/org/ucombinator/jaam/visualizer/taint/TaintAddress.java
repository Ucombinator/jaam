package org.ucombinator.jaam.visualizer.taint;

import org.ucombinator.jaam.tools.taint3.Address;
import java.util.HashSet;

public class TaintAddress extends TaintVertex {

    private Address address;

    public TaintAddress(Address address) {
        super(address.toString(), VertexType.TAINT_ADDRESS, true);

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
}
