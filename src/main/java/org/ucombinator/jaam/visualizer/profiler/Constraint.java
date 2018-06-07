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

    public static double applyConstraints(ArrayList<Constraint> constraints) {
        // The right value should always be the same.
        if (constraints.size() > 0) {
            ProfilerVertexValue rightValue = constraints.get(0).getRightValue();
            for (Constraint constraint : constraints) {
                ProfilerVertexValue leftValue = constraint.getLeftValue();
                double leftColumn = leftValue.getColumn();

                // If both are vertices or both are edges, we require a distance of 1.
                // Otherwise, we require a distance of 0.5.
                int numEdgeTypes = 0;
                if (leftValue.getValueType() == ProfilerVertexValue.ValueType.INCOMING_EDGE) {
                    numEdgeTypes++;
                }
                if (rightValue.getValueType() == ProfilerVertexValue.ValueType.INCOMING_EDGE) {
                    numEdgeTypes++;
                }

                if (numEdgeTypes == 1) {
                    rightValue.assignSolution(leftColumn + 0.5);
                }
                else {
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
