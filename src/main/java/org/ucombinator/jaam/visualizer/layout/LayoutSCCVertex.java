package org.ucombinator.jaam.visualizer.layout;

import java.util.HashSet;

import org.ucombinator.jaam.visualizer.graph.Method;
import org.ucombinator.jaam.visualizer.gui.GUINode.ShapeType;
import org.ucombinator.jaam.visualizer.gui.VizPanel;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex.VertexType;

public class LayoutSCCVertex extends AbstractLayoutVertex{

	public LayoutSCCVertex(String name, boolean drawEdges) {
        super(name, VertexType.SCC, drawEdges);
    }

	@Override
	public String getRightPanelContent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getShortDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean searchByMethod(String query, VizPanel mainPanel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HashSet<LayoutMethodVertex> getMethodVertices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ShapeType getShape() {
		// TODO Auto-generated method stub
		return null;
	}
}
