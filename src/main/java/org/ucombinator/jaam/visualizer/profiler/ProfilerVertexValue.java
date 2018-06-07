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

    public int getColumn() {
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

    public void assignSolution(int value) {
        if (this.valueType == ValueType.LEFT_SIDE && value > this.vertex.getLeftColumn()) {
            this.vertex.setLeftColumn(value);
        }
        else if (this.valueType == ValueType.INCOMING_EDGE && value > this.vertex.getEdgeColumn()) {
            this.vertex.setIncomingEdgeColumn(value);
        }
        else if (this.valueType == ValueType.RIGHT_SIDE && value > this.vertex.getRightColumn()) {
            this.vertex.setRightColumn(value);
        }
    }
}
