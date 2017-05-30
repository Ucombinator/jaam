package org.ucombinator.jaam.visualizer.layout;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.paint.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Created by timothyjohnson on 2/15/17.
 */
public class LayoutMethodVertex extends AbstractLayoutVertex {

    private String methodName;

    public LayoutMethodVertex(String methodName, boolean drawEdges) {
        super(methodName, VertexType.METHOD, drawEdges);
        this.methodName = methodName;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getRightPanelContent() {
        return "Method vertex: " + this.getMethodName() + "\nLoop height: " + this.getLoopHeight() + "\n";
    }

    public String getShortDescription() {
        return this.getMethodName();
    }

	public GUINode.ShapeType getShape() {
		return GUINode.ShapeType.RECTANGLE;
	}

    public boolean searchByMethod(String query, VizPanel mainPanel) {
        boolean found = query.contains(this.getMethodName());
        this.setHighlighted(found);
        mainPanel.getHighlighted().add(this);

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            v.searchByMethod(query, mainPanel);
        }

        return found;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices() {
        HashSet<LayoutMethodVertex> result = new LinkedHashSet<LayoutMethodVertex>();
        result.add(this);
        return result;
    }

    // Next three methods modified from "A New Algorithm for Identifying Loops in Decompilation"
    public void identifyLoops()
	{
		//Each vertex is already initialized
        Collection<AbstractLayoutVertex> vertices = this.getInnerGraph().getVertices().values();
		for(AbstractLayoutVertex v : vertices) {
            v.setVertexStatus(VertexStatus.WHITE);
            v.setDFSPosition(-1);
        }

		for(AbstractLayoutVertex v : vertices)
        {
			if(v.getVertexStatus() == VertexStatus.WHITE)
				travLoopsDFS(v, 1);
		}

		this.calcLoopHeights();
		this.calcMaxLoopHeight();
	}

	public AbstractLayoutVertex travLoopsDFS(AbstractLayoutVertex v0, int dfsPathPos)
	{
		//System.out.println("Expanding vertex: " + Integer.toString(v0.id));
		v0.setVertexStatus(VertexStatus.BLACK);
		v0.setDFSPosition(dfsPathPos);

		for(AbstractLayoutVertex ver : v0.getOutgoingNeighbors())
		{
			AbstractLayoutVertex v = ver;
			//System.out.println("New child: " + Integer.toString(v.id));
			if(v.getVertexStatus() == VertexStatus.WHITE)
			{
				//Case A: v is not yet traversed
				AbstractLayoutVertex header = travLoopsDFS(v, dfsPathPos + 1);
				tagLoopHeader(v0, header);
			}
			else
			{
				if(v.getDFSPosition() > 0)
				{
					//Case B: Mark b as a loop header
					tagLoopHeader(v0, v);
				}
				else if(v.getLoopHeader() == null)
				{
					//Case C: Do nothing
				}
				else
				{
					AbstractLayoutVertex header = v.getLoopHeader();
					if(header.getDFSPosition() > 0)
					{
						//Case D
						tagLoopHeader(v0, header);
					}
					else
					{
						//Case E: Re-entry
						while(header.getLoopHeader() != null)
						{
							header = header.getLoopHeader();
							if(header.getDFSPosition() > 0)
							{
								tagLoopHeader(v0, header);
								break;
							}
						}
					}
				}
			}
		}

		v0.setDFSPosition(0);
		return v0.getLoopHeader();
	}

	public void tagLoopHeader(AbstractLayoutVertex v, AbstractLayoutVertex header)
	{
		if(v == header || header == null)
			return;

		AbstractLayoutVertex cur1 = v;
		AbstractLayoutVertex cur2 = header;
		while(cur1.getLoopHeader() != null)
		{
			AbstractLayoutVertex newHeader = cur1.getLoopHeader();
			if(newHeader == cur2)
				return;

			if(newHeader.getDFSPosition() < cur2.getDFSPosition())
			{
				cur1.setLoopHeader(cur2);
				cur1 = cur2;
				cur2 = newHeader;
			}
			else
				cur1 = newHeader;
		}
		cur1.setLoopHeader(cur2);
	}

	public void calcLoopHeights()
	{
		//System.out.println("Calculating loop heights");
		// The loop height is -1 if it has not yet been calculated.
		// We do a breadth-first search of the graph, since the vertices might not be in order in our list.

		// We begin our search from the vertices that do not have a loop header.
		ArrayList<AbstractLayoutVertex> toSearch = new ArrayList<AbstractLayoutVertex>();
		ArrayList<AbstractLayoutVertex> newSearch = new ArrayList<AbstractLayoutVertex>();
		for(AbstractLayoutVertex v: this.getInnerGraph().getVertices().values())
		{
			AbstractLayoutVertex header = v.getLoopHeader();
			if(header == null)
			{
				v.setLoopHeight(0);
				toSearch.add(v);
			}
			else
			{
				header.addLoopChild(v);
			}
		}

		//This loop should terminate because every vertex has exactly one loop header, and there should not
		//be a loop in following header pointers. Each pass sets the height for the vertices at the next
		//level.
		int currLoopHeight = 1;
		while(toSearch.size() > 0)
		{
			for(AbstractLayoutVertex v : toSearch)
			{
				ArrayList<AbstractLayoutVertex> loopChildren = v.getLoopChildren();
				if(loopChildren.size() > 0)
				{
					v.setLoopHeight(currLoopHeight);
					for(AbstractLayoutVertex w : loopChildren)
						newSearch.add(w);
				}
				else
					v.setLoopHeight(currLoopHeight - 1);
			}

			toSearch = newSearch;
			newSearch = new ArrayList<AbstractLayoutVertex>();
			currLoopHeight++;
		}

		//System.out.println("Loop heights found!");
	}



}
