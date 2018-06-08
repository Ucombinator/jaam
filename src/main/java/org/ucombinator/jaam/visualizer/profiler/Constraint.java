package org.ucombinator.jaam.visualizer.profiler;

import java.util.ArrayList;
import java.util.Set;

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

    public static double applyConstraintsRight(Set<Constraint> constraints) {
        // The right value should be the same for every constraint in our set.
        if (constraints.size() > 0) {
            ProfilerVertexValue rightValue = constraints.iterator().next().getRightValue();
            double rightColumn = 0;
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

                if (numEdgeTypes == 0) {
                    rightColumn = Math.max(rightColumn, leftColumn);
                }
                else if (numEdgeTypes == 1) {
                    rightColumn = Math.max(rightColumn, leftColumn + 0.5);
                }
                else if (numEdgeTypes == 2) {
                    rightColumn = Math.max(rightColumn, leftColumn + 1);
                }
            }
            rightValue.assignSolution(rightColumn);
            return rightColumn;
        }
        else {
            return -1;
        }
    }

    public static double applyConstraintsLeft(Set<Constraint> constraints) {
        if (constraints.size() > 0) {
            // The left value should be the same for every constraint in our set.
            ProfilerVertexValue leftValue = constraints.iterator().next().getLeftValue();
            double leftColumn = Integer.MAX_VALUE;
            for (Constraint constraint : constraints) {
                ProfilerVertexValue rightValue = constraint.getRightValue();
                double rightColumn = rightValue.getColumn();

                int numEdgeTypes = 0;
                if (leftValue.getValueType() == ProfilerVertexValue.ValueType.INCOMING_EDGE) {
                    numEdgeTypes++;
                }
                if (rightValue.getValueType() == ProfilerVertexValue.ValueType.INCOMING_EDGE) {
                    numEdgeTypes++;
                }

                if (numEdgeTypes == 0) {
                    leftColumn = Math.min(leftColumn, rightColumn);
                }
                else if (numEdgeTypes == 1) {
                    leftColumn = Math.min(leftColumn, rightColumn - 0.5);
                }
                else if (numEdgeTypes == 2) {
                    leftColumn = Math.min(leftColumn, rightColumn - 1);
                }
            }
            leftValue.assignSolution(leftColumn);
            return leftColumn;
        }
        else {
            return -1;
        }
    }
}
