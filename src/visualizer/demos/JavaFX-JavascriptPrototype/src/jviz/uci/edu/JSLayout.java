package jviz.uci.edu;

import java.awt.GraphicsEnvironment;
import java.util.Map;

import graphs.jviz.uci.edu.Edge;
import graphs.jviz.uci.edu.Graph;
import graphs.jviz.uci.edu.Node;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

public class JSLayout {
	WebEngine webEngine;
	Graph graph;
	
	
	
	
	public JSLayout(WebEngine webEngine, Graph graph) {
		super();
		this.webEngine = webEngine;
		this.graph = graph;
		
		JSObject jsobj = (JSObject) this.webEngine.executeScript("window");
		jsobj.setMember("java", new Bridge());
	}
	

	public void initLayout(){
		UCIJSObject d3 = new UCIJSObject((JSObject) webEngine.executeScript("d3"));
		System.out.println(d3.obj);

		
		int WIDTH = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth()-50;
		int HEIGHT = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight()-100;
		
		System.out.println("Width = " + WIDTH + ", Height = " + HEIGHT);
		UCIJSObject svgcontainer = d3.select("#svgcontainer")
				.attr("color", "#20B2AA").attr("width", ""+WIDTH).attr("height", ""+HEIGHT);;

		draw(d3, svgcontainer);
		webEngine.executeScript("java.print('POOOOOOOOOOOO')");	

	}
	
	
    int VERTEX_WIDTH = 100;
    int VERTEX_HEIGHT = 50;
    int VERTEX_MARGIN = 5;
    private void draw(UCIJSObject d3, UCIJSObject svgcontainer){
    	Map<String,Edge> edges = graph.getEdges();
    	for (Map.Entry<String, Edge> entry : edges.entrySet()) {
    	    String id = entry.getKey();
    	    Edge edge = entry.getValue();

    	    edge.setGraphics(svgcontainer.append("g"));
    	    
    	    UCIJSObject path = edge.getGraphics().append("path");
    	    path.attr("id",edge.getId())
	    	    .attr("d", "M "+edge.getStart().getCenter().getX()+" "+edge.getStart().getCenter().getY() +
	    	    		" L "+edge.getTo().getCenter().getX()+" "+edge.getTo().getCenter().getY()
	    	    		)
	    	    .attr("stroke", "red")
	    	    .attr("stroke-width", "2");
    		
    		String varname = "path"+edge.getId();
    		
    		System.out.println(edge.getId());

    	}
    	
    	Map<String,Node> nodes = graph.getNodes();
    	for (Map.Entry<String, Node> entry : nodes.entrySet()) {
    	    String id = entry.getKey();
    	    Node node = entry.getValue();

    	    node.setGraphics(svgcontainer.append("g"));
    	    
    	    //Rounded rectangle
    	    UCIJSObject rect = node.getGraphics().append("rect").attr("rx", 6);
    	    		rect.attr("id",node.getId())
    	    		.attr("ry", 6)
    	    		.attr("x", ""+(node.getCenter().getX()-VERTEX_WIDTH/2))
    	    		.attr("y", ""+(node.getCenter().getY()-VERTEX_HEIGHT/2))
    	    		.attr("width", ""+VERTEX_WIDTH)
    	    		.attr("height",""+VERTEX_HEIGHT)
    	    		.attr("stroke", "#1E90FF")
    	    		.attr("fill", "white")
    	    		.attr("stroke-width", "5");
    	    //Text
    	    UCIJSObject text = node.getGraphics().append("text")
    	    		.attr("x", ""+(node.getCenter().getX()-VERTEX_WIDTH/2+10))
    	    		.attr("y", ""+(node.getCenter().getY()+5))
    	    		.text(node.getLabel());
    	    		

    		String varname = "rect"+node.getId();

    	
    		webEngine.executeScript("var "+varname+" = d3.select('#"+node.getId()+"'); "+varname+".on('mouseenter',function(){"+varname+".transition().duration(500).ease(d3.easeBounce).attr('fill', 'yellow').attr('width',"+(int)(1.5*VERTEX_WIDTH)+").attr('x',"+varname+".attr('x')-"+(int)(VERTEX_WIDTH/4)+")})");
    		webEngine.executeScript("var "+varname+" = d3.select('#"+node.getId()+"'); "+varname+".on('mouseleave',function(){"+varname+".transition().duration(500).ease(d3.easeBounce).attr('fill', 'white').attr('width',"+VERTEX_WIDTH+").attr('x',"+(node.getCenter().getX()-VERTEX_WIDTH/2)+")})");  		
    	}
    }
}
