package org.ucombinator.jaam.visualizer.gui;

import java.util.ArrayList;

import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;
import org.ucombinator.jaam.visualizer.layout.AnimationHandler;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.main.Parameters;

public class GUINode extends Pane
{
    protected static boolean showId = true;
    protected static final double TEXT_VERTICAL_PADDING = 15;
    protected static final double TEXT_HORIZONTAL_PADDING = 15;
	double dragX, dragY;
    public Rectangle rect;
    protected Rectangle backRect;
    protected Text rectLabel;
    private AbstractLayoutVertex vertex;
	private GUINode parent;

	private ArrayList<LayoutEdge> edges = new ArrayList<LayoutEdge>();

    boolean labelsEnabled = false;
    boolean isDragging;

    private double totalScaleX;
    private double totalScaleY;

    public GUINode(GUINode parent, AbstractLayoutVertex v)
    {
        super();
        this.parent = parent;
        this.vertex = v;
        this.vertex.setGraphics(this);
        
        this.rect = new Rectangle();
        this.backRect = new Rectangle();
        this.rectLabel = new Text(v.getLabel());
        if(labelsEnabled)
        {
        	this.getChildren().addAll(this.backRect, this.rect, this.rectLabel);
        }
        else
        {
        	this.getChildren().addAll(this.backRect, this.rect);
        }

        if(GUINode.showId) {
            this.getChildren().addAll(new Text("ID: " + this.getVertex().getId()));
        }

        this.isDragging = false;
        this.totalScaleX = 1;
        this.totalScaleY = 1;

        this.addMouseEvents();
        this.setVisible(true);
    }
    
    public AbstractLayoutVertex getVertex() {
		return vertex;
	}

	public void setVertex(AbstractLayoutVertex vertex) {
		this.vertex = vertex;
	}

    public String toString()
    {
        return rectLabel.getText().toString();
    }

    public void setLabel(String text)
    {
        this.rectLabel.setText(text);
    }

    // Next several methods: Pass on calls to underlying rectangle
    public void setFill(Color c)
    {
    	this.backRect.setFill(Color.WHITE);
    	this.rect.setFill(c);
    	if(vertex.getType()== AbstractLayoutVertex.VertexType.CHAIN){
        	Stop[] stops = new Stop[]{new Stop(0.6,c), new Stop(0.4,Color.WHITE)};
            this.rect.setFill(new LinearGradient(0, 0, 8, 8, false, CycleMethod.REPEAT, stops));
        } else if(vertex.getType() == AbstractLayoutVertex.VertexType.ROOT){
        	this.rect.setFill(javafx.scene.paint.Color.WHITE);
        }

    }

    public void setStroke(Color c)
    {
        this.rect.setStroke(c);
    }

    public void setStrokeWidth(double strokeWidth)
    {
        this.rect.setStrokeWidth(0.001);
    }

    public void setArcHeight(double height)
    {
        this.rect.setArcHeight(height);
        this.backRect.setArcHeight(height);
    }

    public void setArcWidth(double width)
    {
        this.rect.setArcWidth(width);
        this.backRect.setArcWidth(width);
    }

    public void setLocation(double x, double y, double width, double height)
    {
        this.setTranslateX(x);
        this.setTranslateY(y);
        this.rect.setWidth(width);
        this.rect.setHeight(height);
        this.backRect.setWidth(width);
        this.backRect.setHeight(height);
    }

    // Halve the distance from the current opacity to 1.
    public void increaseOpacity()
    {
        this.rect.setOpacity((1 + this.rect.getOpacity()) / 2.0);	
    }

    // Halve the current opacity.
    public void decreaseOpacity()
    {
        this.rect.setOpacity((this.rect.getOpacity()) / 2.0);
    }

    public void addMouseEvents()
    {
        this.setOnMousePressed(onMousePressedEventHandler);
        this.setOnMouseDragged(onMouseDraggedEventHandler);
        this.setOnMouseReleased(onMouseReleasedEventHandler);
        this.setOnMouseEntered(onMouseEnteredEventHandler);
        this.setOnMouseExited(onMouseExitedEventHandler);
        this.setOnMouseClicked(new AnimationHandler());
    }

    // The next two functions compute the shift that must be applied to keep the
    // top left corner stationary when the node is scaled about its center.
    public double getXShift()
    {
        double currentWidth = this.getScaleX() * this.vertex.getWidth();
        double oldWidth = this.vertex.getWidth();
        return (oldWidth - currentWidth) / 2;
        //return 0;
    }

    public double getYShift()
    {
        double currentHeight = this.getScaleY() * this.vertex.getHeight();
        double oldHeight = this.vertex.getHeight();
        return (oldHeight - currentHeight) / 2;
        //return 0;
    }

    EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            event.consume();
            GUINode node = (GUINode) event.getSource();

            dragX = node.getBoundsInParent().getMinX() - event.getScreenX();
            dragY = node.getBoundsInParent().getMinY() - event.getScreenY();
        }
    };

    EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            event.consume();
            GUINode node = (GUINode) event.getSource();

            node.isDragging = true;
            double offsetX = event.getScreenX() + dragX;
            double offsetY = event.getScreenY() + dragY;
            node.setTranslateX(offsetX - node.getXShift());
            node.setTranslateY(offsetY - node.getYShift());

            AbstractLayoutVertex v = GUINode.this.vertex;
            VizPanel mainPanel = ((StacFrame) Main.getOuterFrame().getCurrentFrame()).mainPanel;
            v.setX(mainPanel.invScaleX(offsetX));
            v.setY(mainPanel.invScaleY(offsetY));
            LayoutEdge.redrawEdges(v, false);
        }
    };

    EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            event.consume();
            GUINode node = (GUINode) event.getSource();

            if (node.isDragging)
            {
                node.isDragging = false;
            }
        }
    };

    EventHandler onMouseEnteredEventHandler = new javafx.event.EventHandler()
    {
        @Override
        public void handle(Event event)
        {
            event.consume();
            if(!getVertex().getType().equals(AbstractLayoutVertex.VertexType.ROOT)){
            getChildren().add(rectLabel);
            rectLabel.setTranslateX(TEXT_HORIZONTAL_PADDING);
            rectLabel.setTranslateY(TEXT_VERTICAL_PADDING);
            }
        	if (vertex.getSelfGraph() != null)
        	{
	        	for(LayoutEdge e : vertex.getSelfGraph().getEdges().values())
                {
	        		if(e.getSourceVertex() == vertex || e.getDestVertex() == vertex)
	        		{
	        		    Line line = e.getLine();
	        		    line.setStroke(Color.ORANGERED);
	        		    line.setStrokeWidth(line.getStrokeWidth() * 4.0);
	        		}
	        	}
        	}
        	
            GUINode obj = (GUINode) (event.getSource());
//            obj.rect.setOpacity(1);
//            if (obj.parent != null)
//                obj.parent.rect.setOpacity(0.3);
        }
    };

	EventHandler onMouseExitedEventHandler = new javafx.event.EventHandler()
    {
        @Override
        public void handle(Event event)
        {
            event.consume();
            getChildren().remove(rectLabel);
            
        	if(vertex.getSelfGraph() != null)
        	{
	        	for(LayoutEdge e : vertex.getSelfGraph().getEdges().values())
                {
	        		if (e.getSourceVertex() == vertex || e.getDestVertex() == vertex)
	        		{
	        		    Line line = e.getLine();
	        			line.setStroke(Color.BLACK);
                        line.setStrokeWidth(line.getStrokeWidth() / 4.0);
	        		}
	        	}
        	}
        	

            
            GUINode obj = (GUINode) (event.getSource());
            
//            obj.rect.setOpacity(.3);
//            if(obj.parent != null)
//                obj.parent.rect.setOpacity(1);
        }
    };

	public GUINode getParentNode() {
	    return this.parent;
    }

    public double getTotalParentScaleX() {
	    if (this.parent != null)
	        return this.parent.totalScaleX;
	    else return 1;
    }

    public double getTotalParentScaleY() {
	    if (this.parent != null)
	        return this.parent.totalScaleY;
	    else return 1;
    }

	public void setTotalScaleX(double scale) {
	    this.totalScaleX = scale;
    }

	public double getTotalScaleX() {
	    return this.totalScaleX;
    }

    public void setTotalScaleY(double scale) {
	    this.totalScaleY = scale;
    }

    public double getTotalScaleY() {
	    return this.totalScaleY;
    }
}
