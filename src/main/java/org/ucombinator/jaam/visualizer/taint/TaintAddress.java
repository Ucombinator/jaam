package org.ucombinator.jaam.visualizer.taint;

import org.ucombinator.jaam.tools.taint3.Address;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

public class TaintAddress extends AbstractLayoutVertex {

    public TaintAddress(Address address) {
        super(address.toString(), VertexType.TAINT_ADDRESS, true);
    }

    public TaintAddress(String label) {
        super(label, VertexType.TAINT_ADDRESS, true);
    }
}
