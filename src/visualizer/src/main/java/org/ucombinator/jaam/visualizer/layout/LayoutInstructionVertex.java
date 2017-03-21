package org.ucombinator.jaam.visualizer.layout;

import org.ucombinator.jaam.visualizer.graph.Instruction;

import java.util.HashSet;

/**
 * Created by timothyjohnson on 2/15/17.
 */
public class LayoutInstructionVertex extends AbstractLayoutVertex {

    Instruction instruction;
    LayoutMethodVertex methodVertex;

    public LayoutInstructionVertex(Instruction instruction, LayoutMethodVertex methodVertex, boolean drawEdges) {
        super(instruction.getText(), VertexType.INSTRUCTION, drawEdges);
        this.instruction = instruction;
        this.methodVertex = methodVertex;
    }

    @Override
    public int getMinInstructionLine() {
        return this.instruction.getJimpleIndex();
    }

    public Instruction getInstruction() {
        return this.instruction;
    }

    public String getRightPanelContent() {
        return "Method: " + this.instruction.getMethodName() + "\nInstruction: " + this.instruction.getJimpleIndex()
                + "\n" + this.instruction.getText() + "\n";
    }

    public String getShortDescription() {
        return this.instruction.getText();
    }

    public boolean searchByMethod(String query) {
        boolean found = this.instruction.getMethodName().contains(query);
        this.setHighlighted(found);
        return found;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices() {
        HashSet<LayoutMethodVertex> result = new HashSet<LayoutMethodVertex>();
        result.add(this.methodVertex);
        return result;
    }
}
