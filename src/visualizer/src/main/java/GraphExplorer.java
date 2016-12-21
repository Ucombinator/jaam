import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.EventHandler;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Iterator;

import javafx.animation.FadeTransition;
import javafx.animation.PathTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class GraphExplorer extends JFXPanel {

	private boolean context;
	private Frame parent;
	private AbstractVertex main;
	private Group root;
	private Color[] colors = {Color.LIGHTBLUE,Color.GREEN,Color.AZURE, Color.BLUEVIOLET, Color.DARKTURQUOISE};
	private int index = 0;
	
	public GraphExplorer(Group root) {
		super();
		this.root = root;
		//Platform.runLater(new Runnable() {  
		//      @Override public void run() {
			initFX();          
		//      }
		//});	
	}
	

	
	private int scaleX(double coordinate)
	{
		return (int)(coordinate*1000/this.main.getWidth());
	}
	
	private int scaleY(double coordinate)
	{
		return (int)(coordinate*1000/this.main.getHeight());
	}
	
    private void initFX() {
        Graph g = new Graph();
        g.addVertex(1, "f1", "main1", "desc1", 1, true);
        g.addVertex(2, "f1", "main1", "desc1", 1, true);
        g.addVertex(3, "f1", "main1", "desc1", 1, true);
        g.addVertex(4, "f1", "main1", "desc1", 1, true);
        
        g.addVertex(5, "f2", "main1", "desc1", 1, true);
        g.addVertex(6, "f2", "main1", "desc1", 1, true);
        g.addVertex(7, "f2", "main1", "desc1", 1, true);
        
        g.addVertex(8, "f3", "main1", "desc1", 1, true);
        g.addVertex(9, "f3", "main1", "desc1", 1, true);
        g.addVertex(10, "f3", "main1", "desc1", 1, true);
        g.addVertex(11, "f3", "main1", "desc1", 1, true);
        g.addVertex(12, "f3", "main1", "desc1", 1, true);
        
        g.addEdge(1,2);
        g.addEdge(1,3);
        g.addEdge(1,4);
        
        g.addEdge(2,5);
        g.addEdge(5,6);
        g.addEdge(6,7);
        
        g.addEdge(4,8);
        g.addEdge(8,9);
        g.addEdge(8,10);
        g.addEdge(10,11);
        g.addEdge(10,12);
        
        
//        g.addVertex(1, "f1", "main1", "desc1", 1, true);
//        g.addVertex(4, "f1", "main1", "desc1", 1, true);
//        g.addVertex(2, "f2", "main2", "desc2", 1, true);
//        g.addVertex(3, "f3", "main3", "desc3", 1, true);
//        
//        g.addEdge(1,2);
//        g.addEdge(1,4);
//        g.addEdge(1,3);
//        g.addEdge(2,3);
//        
//    
//    	
    	g = Main.graph;
        this.main = LayerFactory.get2layer(g);
        LayoutAlgorithm.layout(main);
                
        draw(main,null);

        System.out.println("Done!");
    }
    
    public void draw(AbstractVertex v, GUINode parentGroup){

    	GUINode gr = new GUINode(parentGroup,v);
    	v.setGraphics(gr);
    	if(parentGroup !=null){
    		parentGroup.getChildren().add(gr);
    	}else{
    		root.getChildren().add(gr);
    	}
    	
    	
    	gr.setLayoutX(scaleX(v.getX()));
    	gr.setLayoutY(scaleY(v.getY()));
    	
    
    	
    	Rectangle r_back = new Rectangle(0, 0, scaleX(v.getWidth()), scaleY(v.getHeight()));
    	r_back.setArcWidth(scaleX(0.5));
    	r_back.setArcHeight(scaleY(0.5));
    	r_back.setFill(Color.WHITE);
    	r_back.setStroke(Color.BLACK);
    	r_back.setStrokeWidth(.5);
    	r_back.setOpacity(1);
    	
    	
    	Rectangle r = new Rectangle(0, 0, scaleX(v.getWidth()), scaleY(v.getHeight()));
    	r.setArcWidth(scaleX(0.5));
    	r.setArcHeight(scaleY(0.5));
    	Label label = new Label("  "+v.getLabel());
    	r.setFill(colors[index++ % colors.length]);
    	r.setStroke(Color.BLACK);
    	r.setStrokeWidth(.5);
    	r.setOpacity(.3);
    	
//    	r.setOnMouseEntered(new javafx.event.EventHandler() {
//
//			@Override
//			public void handle(Event event) {
//				Rectangle obj = ((Rectangle)(event.getSource()));
//				obj.setOpacity(1);
////				obj.setScaleX(1);
////				obj.setScaleY(1.1);
//				
//			}
//		});
//    	
//    	r.setOnMouseExited(new javafx.event.EventHandler() {
//
//			@Override
//			public void handle(Event event) {
//				Rectangle obj = ((Rectangle)(event.getSource()));
//				obj.setOpacity(.3);
////				obj.setScaleX(0.9090909091);
////				obj.setScaleY(0.9090909091);
//			}
//		});
    	
    	
    	gr.setOnMouseClicked(new AnimationHandler());
    	
    	gr.getChildren().add(r_back);
    	gr.getChildren().add(r);
    	//gr.getChildren().add(label);
    	
    	
    	
    	if(v.getInnerGraph().getVertices().size()==0){
    		return;
    	}
    	
       	Iterator<Edge> itEdge = v.getInnerGraph().getEdges().values().iterator();
    	while(itEdge.hasNext()){
	    		Edge e = itEdge.next();
	    		AbstractVertex start = e.getSourceVertex();
	    		AbstractVertex end = e.getDestVertex();
	    	    				
        		double startX = start.getX()+start.getWidth()/2;
        		double startY = start.getY()+start.getHeight()/2;
        		double endX= end.getX()+end.getWidth()/2;
        		double endY= end.getY()+end.getHeight()/2;

        		Line  l = new Line(scaleX(startX),scaleY(startY),scaleX(endX),scaleY(endY));
        		if(e.getType() == Edge.EDGE_TYPE.EDGE_DUMMY){
        			l.getStrokeDashArray().addAll(5d, 4d);
        		}
        		gr.getChildren().add(l);
        	}
    	
    
    	Iterator<AbstractVertex> it = v.getInnerGraph().getVertices().values().iterator();
	    	while(it.hasNext()){
    		draw(it.next(), gr);
	    	}
    	}
}
    

