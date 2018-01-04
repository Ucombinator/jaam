package org.ucombinator.jaam.visualizer.layout;

import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.graph.Instruction;

import java.util.HashSet;
import java.util.LinkedHashSet;

public abstract class StateVertex extends AbstractLayoutVertex<StateVertex> {

    public StateVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
    }

    public StateVertex(int id, String label, VertexType type) {
        super(id, label, type);
    }

    public void searchByID(int id, MainTabController mainTab)
    {
        this.searchByIDRange(id, id, mainTab);
    }

    public void searchByIDRange(int id1, int id2, MainTabController mainTab)
    {
        if(this.getId() >= id1 && this.getId() <= id2) {
            this.setHighlighted(true);
            mainTab.getVizHighlighted().add(this);
            System.out.println("Search successful: " + this.getId());
        }

        for(StateVertex v : this.getInnerGraph().getVisibleVertices())
            v.searchByIDRange(id1, id2, mainTab);
    }

    public void searchByInstruction(String query, MainTabController mainTab)
    {
        if(this instanceof LayoutInstructionVertex) {
            String instStr = ((LayoutInstructionVertex) this).getInstruction().getText();
            if(instStr.contains(query)) {
                this.setHighlighted(true);
                mainTab.getVizHighlighted().add(this);
            }
        }

        for(StateVertex v : this.getInnerGraph().getVisibleVertices())
            v.searchByInstruction(query, mainTab);
    }

    // Subclasses must implement these so that we have descriptions for each of them,
    // and so that our generic collapsing can work for all of them
    public abstract String getRightPanelContent();

    // These searches may be different for different subclasses, so we implement them there.
    public abstract boolean searchByMethod(String query, MainTabController mainTab);

    // This is needed so that we can show the code for the methods that correspond to selected vertices
    public abstract HashSet<LayoutMethodVertex> getMethodVertices();

    public HashSet<String> getMethodNames() {
        HashSet<LayoutMethodVertex> methodVertices = this.getMethodVertices();
        HashSet<String> methodNames = new HashSet<String>();
        for(LayoutMethodVertex v : methodVertices) {
            methodNames.add(v.getMethodName());
        }

        return methodNames;
    }

    public HashSet<Instruction> getInstructions() {
        return this.getInstructions(new LinkedHashSet<Instruction>());
    }

    private HashSet<Instruction> getInstructions(HashSet<Instruction> instructions) {
        if(this.getType().equals(VertexType.ROOT) || this.getType().equals(VertexType.METHOD)
                || this.getType().equals(VertexType.CHAIN)) {
            for(StateVertex v : this.getInnerGraph().getVisibleVertices()) {
                v.getInstructions(instructions);
            }
        } else if(this.getType().equals(VertexType.INSTRUCTION)){
            instructions.add(((LayoutInstructionVertex) this).getInstruction());
        } else {
            System.out.println("Unrecognized type in method getInstructions: " + this.getType());
        }

        return instructions;
    }

    public HashSet<AbstractLayoutVertex> getVerticesWithInstructionID(int id, String method_name) {
        return getVerticesWithInstructionID(id, method_name, new LinkedHashSet<AbstractLayoutVertex>());
    }

    private HashSet<AbstractLayoutVertex> getVerticesWithInstructionID(int id, String method_name,
                                                                       HashSet<AbstractLayoutVertex> set)  {
        if(this.getType().equals(VertexType.ROOT) || this.getType().equals(VertexType.METHOD)
                || this.getType().equals(VertexType.CHAIN)){
            for(StateVertex v : this.getInnerGraph().getVisibleVertices()) {
                v.getVerticesWithInstructionID(id, method_name, set);
            }
        } else if(this.getType().equals(VertexType.INSTRUCTION)) {
            Instruction inst = ((LayoutInstructionVertex) this).getInstruction();
            if(inst.getMethodName() == method_name && inst.getJimpleIndex() == id) {
                set.add(this);
            }
        } else {
            System.out.println("Unrecognized type in method getInstructions: " + this.getType());
        }

        return set;
    }
}
