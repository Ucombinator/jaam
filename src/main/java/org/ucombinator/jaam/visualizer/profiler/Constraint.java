package org.ucombinator.jaam.visualizer.profiler;

import java.util.ArrayList;

public class Constraint {

    // We create an adjacency object when either a vertex or its edge is adjacent to some other vertex
    // within some row.
    private ProfilerVertexValue leftValue;
    private ProfilerVertexValue rightValue;

    public Constraint(ProfilerVertexValue left, ProfilerVertexValue right) {
        this.leftValue = left;
        this.rightValue = right;
    }

    public ProfilerVertexValue getLeftValue() {
        return this.leftValue;
    }

    public ProfilerVertexValue getRightValue() {
        return this.rightValue;
    }

    public static int applyConstraints(ArrayList<Constraint> constraints) {
        // The right value should always be the same.
        if (constraints.size() > 0) {
            ProfilerVertexValue rightValue = constraints.get(0).getRightValue();
            for (Constraint constraint : constraints) {
                ProfilerVertexValue leftValue = constraint.getLeftValue();
                int leftColumn = leftValue.getColumn();
                // TODO: Should this if branch also apply if the left value is the parent of our right value?
                // That way the outgoing edge check will work correctly.
                if (leftValue.getVertex() == rightValue.getVertex()) {
                    if (leftValue.getValueType() == ProfilerVertexValue.ValueType.LEFT_SIDE) {
                        rightValue.assignSolution(leftColumn);
                    } else {
                        rightValue.assignSolution(leftColumn + 1);
                    }
                } else {
                    rightValue.assignSolution(leftColumn + 1);
                }
            }
            return rightValue.getColumn();
        }
        else {
            return -1;
        }
    }
}
