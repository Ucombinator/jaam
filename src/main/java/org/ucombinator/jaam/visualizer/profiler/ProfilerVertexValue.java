package org.ucombinator.jaam.visualizer.profiler;

public class ProfilerVertexValue {

    public enum ValueType {LEFT_SIDE, INCOMING_EDGE, RIGHT_SIDE};
    private ProfilerVertex vertex;
    private ValueType valueType;

    public ProfilerVertexValue(ProfilerVertex vertex, ValueType constraint) {
        this.vertex = vertex;
        this.valueType = constraint;
    }

    public ProfilerVertex getVertex() {
        return this.vertex;
    }

    public ValueType getValueType() {
        return this.valueType;
    }

    public double getColumn() {
        if (this.valueType == ValueType.LEFT_SIDE) {
            return this.vertex.getLeftColumn();
        }
        else if (this.valueType == ValueType.INCOMING_EDGE) {
            return this.vertex.getEdgeColumn();
        }
        else if (this.valueType == ValueType.RIGHT_SIDE) {
            return this.vertex.getRightColumn();
        }
        else {
            return -1;
        }
    }

    public void assignSolution(double value) {
        if (this.valueType == ValueType.LEFT_SIDE) {
            this.vertex.setLeftColumn(value);
        }
        else if (this.valueType == ValueType.INCOMING_EDGE) {
            this.vertex.setIncomingEdgeColumn(value);
        }
        else if (this.valueType == ValueType.RIGHT_SIDE) {
            this.vertex.setRightColumn(value);
        }
    }
}
