package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.Method;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

public class LayoutMethodVertex extends AbstractLayoutVertex {

    private Method method = null;

    private Color methodColor = Color.DEEPSKYBLUE;
    private Color loopColor   = Color.ORANGE;

    
    public LayoutMethodVertex(int id, String label){
    	super(id, label);
    }
    
    public LayoutMethodVertex(Method method, boolean drawEdges) {
        super(method.getFullName(), VertexType.METHOD, drawEdges);
        this.method = method;
    }
    
    public LayoutMethodVertex(String method, boolean drawEdges) {
        super(method, VertexType.METHOD, drawEdges);
        this.method = null;
    }

    public String getMethodName() {
        return this.method.getFullName();
    }

    public String getRightPanelContent() {
        return "Method vertex: " + this.getMethodName() + "\nLoop height: " + this.getLoopHeight() + "\n\n"
                + "Decompiled class code:\n" + this.method.getOurClass().getCode();
    }

    public String getShortDescription() {
        return this.getMethodName();
    }

    public boolean searchByMethod(String query, VizPanel mainPanel) {
        boolean found = query.contains(this.getMethodName());
        this.setHighlighted(found);
        mainPanel.getHighlighted().add(this);

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices()) {
            v.searchByMethod(query, mainPanel);
        }

        return found;
    }

    public LinkedHashSet<LayoutMethodVertex> getMethodVertices() {
        LinkedHashSet<LayoutMethodVertex> result = new LinkedHashSet<LayoutMethodVertex>();
        result.add(this);
        return result;
    }

    // Next three methods modified from "A New Algorithm for Identifying Loops in Decompilation"
    public void identifyLoops()
    {
        //Each vertex is already initialized
        Collection<AbstractLayoutVertex> vertices = this.getInnerGraph().getVertices();
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

        for(AbstractLayoutVertex v : this.getInnerGraph().getOutNeighbors(v0))
        {
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
        for(AbstractLayoutVertex v: this.getInnerGraph().getVertices())
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

    public void setDefaultColor(){

        if(this.getInnerGraph().getRoot().getLabel().startsWith("Method"))
        {
            this.setColor(methodColor);
        }
        else if(this.getInnerGraph().getRoot().getLabel().startsWith("Loop"))
        {
            this.setColor(loopColor);
        }
        else
        {
            this.setColor(Color.DARKRED);
        }
        System.out.println("Get Color " + this.getInnerGraph().getRoot().getLabel());
/*            System.out.println("**************************METHOD*********************+");
            if(!this.getInnerGraph().getVertices().isEmpty()) {
                AbstractLayoutVertex v = this.getInnerGraph().getVertices().values().iterator().next();
                System.out.println("Name: " + v.getClass().getName());
                if (v instanceof LayoutLoopVertex) {
                    this.setColor(loopColor);
                } else if (v instanceof LayoutMethodVertex) {
                    this.setColor(methodColor);
                }
            }
            */
    }
}
