
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


// TODO: Place vertex labels on top of vertices.
public class GUINode extends Pane
{
    double dragX, dragY;
    protected Rectangle rect;
    protected Text rectLabel;
    private AbstractVertex vertex;
	private GUINode parent;

	private ArrayList<Edge> edges = new ArrayList<Edge>();

    boolean labelsEnabled = false;
    boolean isDragging;

    public GUINode(GUINode parent, AbstractVertex v)
    {
        super();
        this.parent = parent;
        this.vertex = v;
        this.vertex.setGraphics(this);
        
        this.rect = new Rectangle();
     
        this.rectLabel = new Text();
        if(labelsEnabled)
        {
        	this.getChildren().addAll(this.rect, this.rectLabel);
        }
        else
        {
        	this.getChildren().addAll(this.rect);
        }


        
        //this.makeDraggable(); ASK TIM

        this.isDragging = false;

        this.addMouseEvents();
        this.setVisible(true);
    }
    
    public AbstractVertex getVertex() {
		return vertex;
	}

	public void setVertex(AbstractVertex vertex) {
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
    	//this.back_rect.setFill(Color.WHITE);
    	this.rect.setFill(c);
    	if(vertex.getType()==AbstractVertex.VertexType.CHAIN){
        	Stop[] stops = new Stop[]{new Stop(0.6,c), new Stop(0.4,Color.WHITE)};
            this.rect.setFill(new LinearGradient(0, 0, 8, 8, false, CycleMethod.REPEAT, stops));
        } else if(vertex.getType()==AbstractVertex.VertexType.ROOT){
        	this.rect.setFill(javafx.scene.paint.Color.WHITE);
        }

    }

    public void setStroke(Color c)
    {
        this.rect.setStroke(c);
    }

    public void setStrokeWidth(double strokeWidth)
    {
        this.rect.setStrokeWidth(strokeWidth);
    }

    public void setArcHeight(double height)
    {
        this.rect.setArcHeight(height);
    }

    public void setArcWidth(double height)
    {
        this.rect.setArcWidth(height);
    }

    public void setLocation(double x, double y, double width, double height)
    {
        this.setTranslateX(x);
        this.setTranslateY(y);
        this.rect.setWidth(width);
        this.rect.setHeight(height);
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

    // Divides the actual width in pixels by the width in vertex units
    public double getWidthPerVertex()
    {
        return this.getWidth() / vertex.getWidth();
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
            node.setTranslateX(offsetX);
            node.setTranslateY(offsetY);

            AbstractVertex v = GUINode.this.vertex;
            v.location.x = Parameters.stFrame.mainPanel.invScaleX(offsetX);
            v.location.y = Parameters.stFrame.mainPanel.invScaleY(offsetY);
            Edge.redrawEdges(v);
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
        	if (vertex.getSelfGraph()!=null)
        	{
	        	for(Edge e : vertex.getSelfGraph().getEdges().values())
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
        	if(vertex.getSelfGraph() != null)
        	{
	        	for(Edge e : vertex.getSelfGraph().getEdges().values())
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
}
