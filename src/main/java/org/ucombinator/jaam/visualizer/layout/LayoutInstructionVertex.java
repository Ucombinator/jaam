package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.Instruction;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * Created by timothyjohnson on 2/15/17.
 */
public class LayoutInstructionVertex extends AbstractLayoutVertex {

    private static Color color = Color.YELLOW;
    private Instruction instruction;
    private LayoutMethodVertex methodVertex;

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
                + "\n" + this.instruction.getText() + "\nLoop height: " + this.getLoopHeight() + "\n";
    }

    public String getShortDescription() {
        return this.instruction.getText();
    }

    public GUINode.ShapeType getShape() {
        return GUINode.ShapeType.RECTANGLE;
    }

    public boolean searchByMethod(String query, VizPanel mainPanel) {
        boolean found = this.instruction.getMethodName().contains(query);
        if(found) {
            this.setHighlighted(found);
            mainPanel.getHighlighted().add(this);
        }

        return found;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices() {
        HashSet<LayoutMethodVertex> result = new LinkedHashSet<LayoutMethodVertex>();
        result.add(this.methodVertex);
        return result;
    }
}
