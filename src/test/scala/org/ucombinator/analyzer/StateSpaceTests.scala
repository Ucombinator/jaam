package org.ucombinator.analyzer

import org.scalatest.FlatSpec

class StateSpaceTests extends FlatSpec {
    behavior of "Pointers"

    it should "produce unique pointers" in {
        assert(new ConcreteFramePointer() != new ConcreteFramePointer())
        assert(InvariantFramePointer == InvariantFramePointer)
    }
}

// vim: set ts=2 sw=2 et:
