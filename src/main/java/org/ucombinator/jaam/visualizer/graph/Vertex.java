package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;

public class Vertex extends AbstractVertex
{
    private int inputId;
    private Instruction instruction;
    private boolean drawEdges;

    public Vertex(int inputId) {
        super(Integer.toString(inputId));
        this.inputId = inputId;
        this.instruction = new Instruction("");
        this.drawEdges = true;
    }

    public Vertex(Instruction inst, int inputId, boolean drawEdges) {
        this(inputId);
        this.instruction = inst;
        this.drawEdges = drawEdges;
    }

    public int getInputId() {
        return this.inputId;
    }

    public String getMethodName()
    {
        return this.instruction.getMethodName();
    }
    
    public String getInstructionText()
    {
        return this.instruction.getText();
    }

    public void setDrawEdges(boolean drawEdges) {
        this.drawEdges = drawEdges;
    }
    
    public Instruction getInstruction()
    {
        return this.instruction;
    }

    public void setRealInstruction(Instruction inst) {this.instruction = inst; }

    /*public void save(PacketOutput output) {

        org.ucombinator.jaam.serializer.Node currentNode = new org.ucombinator.jaam.serializer.Node(this.getId());
        org.ucombinator.jaam.serializer.State currentState = new org.ucombinator.jaam.serializer.State(
                org.ucombinator.jaam.serializer.Id[org.ucombinator.jaam.serializer.Node](this.getId()), this, "", "");
        org.ucombinator.jaam.serializer.Edge;
    }*/
}