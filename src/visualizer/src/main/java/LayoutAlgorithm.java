
import java.util.ArrayList;
import java.util.Collections;

public class LayoutAlgorithm
{
	// This works on a graph whose vertices have been assigned a bounding box
	final static double MARGIN_PADDING = .25;
	final static double NODES_PADDING = .5;
	
	private static AbstractVertex getVertexWithID(String id)
	{
		for(AbstractVertex v : Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values())
		{
			if(v.getStrID().equals(id))
			{
				System.out.println(":-D '" + v.getStrID() + "'");
				return v;
			}
		}
		return null;
	}
	
	public static void layout(AbstractVertex parentVertex)
	{
		initializeSizes(parentVertex);
		defaultLayout(parentVertex, parentVertex.getInnerGraph());
	}
	
	private static void initializeSizes(AbstractVertex vertex)
	{
		vertex.setWidth(AbstractVertex.DEFAULT_WIDTH);
		vertex.setHeight(AbstractVertex.DEFAULT_HEIGHT);
		for(AbstractVertex childVertex : vertex.getInnerGraph().getVertices().values())
			initializeSizes(childVertex);
	}
	
	public static void defaultLayout(AbstractVertex parentVertex, AbstractGraph graph)
	{
		for(AbstractVertex v : graph.getVertices().values())
		{
			AbstractGraph innerGraph = v.getInnerGraph();
			if (innerGraph.getVertices().size() != 0)
			{
				//Layout the inner graphs of each node and assign width and height to each node
				//coordinates are RELATIVE to the parent
				if(v.isExpanded()){
					defaultLayout(v, v.getInnerGraph());
				}
			}
		}

		//Layout the current graph
		defaultNonRecursiveLayout(parentVertex, graph);
	}
	
	// We assign x,y to each vertex of graph
	// We assign width and height to the parentVertex
	public static void defaultNonRecursiveLayout(AbstractVertex parentVertex, AbstractGraph graph)
	{
		// Initialize all the nodes to be white
	    for(AbstractVertex v : graph.getVertices().values())
			v.vertexStatus = AbstractVertex.VertexStatus.WHITE;
		
		ArrayList<AbstractVertex> arrayList = new ArrayList<AbstractVertex>();
		arrayList.addAll(graph.getVertices().values());
		Collections.sort(arrayList);
		AbstractVertex root = arrayList.get(0);

		double[] xyPair = visit(root, MARGIN_PADDING, MARGIN_PADDING);
		parentVertex.setWidth(xyPair[0] + 2 * MARGIN_PADDING);
		parentVertex.setHeight(xyPair[1] + 2 * MARGIN_PADDING);
		//System.out.println("Finished laying out parent vertex: " + parentVertex.id);
		//System.out.println("Bounding box for children: " + xyPair[0] + ", " + xyPair[1]);
		//System.out.println("Final dimensions: " + parentVertex.getWidth() + ", " + parentVertex.getHeight());
	}
	
	public static double[] visit(AbstractVertex root, double left, double top)
	{
		root.vertexStatus = AbstractVertex.VertexStatus.GRAY;
		ArrayList<AbstractVertex> grayChildren = new ArrayList<AbstractVertex>();
		for(AbstractVertex child : root.getAbstractNeighbors())
		{
			if (child.vertexStatus == AbstractVertex.VertexStatus.WHITE)
			{
				child.vertexStatus = AbstractVertex.VertexStatus.GRAY;
				grayChildren.add(child);
			}
		}
		
		double currentWidth = 0;
		double currentHeight = 0; 
		for(AbstractVertex currVertex : grayChildren)
		{
			double[] boundBox = visit(currVertex,currentWidth + left,NODES_PADDING + top + root.getHeight());
			currentWidth += boundBox[0] + NODES_PADDING;
			currentHeight = Math.max(currentHeight, boundBox[1]);
		}
		
		root.subtreeBoundBox[0] = Math.max(root.getWidth(), currentWidth - NODES_PADDING);
		if(grayChildren.size() == 0)
		{
			root.subtreeBoundBox[1] = root.getHeight();
		}
		else
		{
			root.subtreeBoundBox[1] = NODES_PADDING + root.getHeight() + currentHeight;
		}
		
		root.setX(left + ((root.subtreeBoundBox[0] - root.getWidth()) / 2));  //left-most corner x
		root.setY(top);							    					//top-most corner y
		root.vertexStatus = AbstractVertex.VertexStatus.BLACK;
		return root.subtreeBoundBox;
	}
}
