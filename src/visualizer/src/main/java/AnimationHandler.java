import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class AnimationHandler implements javafx.event.EventHandler<javafx.scene.input.MouseEvent>
{
	@Override
	public void handle(MouseEvent event) {
		EventType<MouseEvent> type = (EventType<MouseEvent>) event.getEventType();
		if(type.equals(MouseEvent.MOUSE_CLICKED))
		{
			if(event.getButton().equals(MouseButton.PRIMARY))
			{
				switch (event.getClickCount())
				{
					case 1:
						handlePrimarySingleClick(event);
						break;
					case 2:
						handlePrimaryDoubleClick(event);
						break;
					default:
						break;
				}
			}
			else if(event.getButton().equals(MouseButton.SECONDARY)) {}
			else if(event.getButton().equals(MouseButton.MIDDLE)) {}
		}
		else
		{
			System.out.println("This line should never be printed since we add the handler by setOnMouseClicked");
		}
	}

	private void handlePrimaryDoubleClick(MouseEvent event)
	{
		AbstractVertex v = ((GUINode)(event.getSource())).getVertex();
		Iterator<Node> it = v.getGraphics().getChildren().iterator();
//		while(it.hasNext())
//		{
//			Node n = it.next();
//			if(!n.getClass().equals(Rectangle.class))
//			{
//				FadeTransition ft = new FadeTransition(Duration.millis(300), n);
//				ft.setToValue(0.0);
//				ft.play();
//				event.consume();
//			}
//		}
		
		TranslateTransition tt = new TranslateTransition(Duration.millis(300), v.getGraphics());
		tt.setToY(Parameters.stFrame.mainPanel.scaleY(-v.getHeight()/2.0 + AbstractVertex.DEFAULT_HEIGHT/2.0));
		tt.play();
		
		
		ScaleTransition st = new ScaleTransition(Duration.millis(300), v.getGraphics());

		//st.setFromX(Parameters.stFrame.mainPanel.scaleX(1.0));
		st.setByX(AbstractVertex.DEFAULT_WIDTH/v.getWidth() -1 );
		//st.setFromY(Parameters.stFrame.mainPanel.scaleY(1.0));
		st.setByY(AbstractVertex.DEFAULT_HEIGHT/v.getHeight() -1);
		st.play();

		
		HashMap<String, Point2D> oldPositions = new HashMap<>();
		savePositions(Parameters.stFrame.mainPanel.getPanelRoot(), oldPositions);
		v.setExpanded(false);
		LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
		
		st.setOnFinished(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {		
				Iterator<AbstractVertex> itMethodVertices = Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values().iterator();
				
				while(itMethodVertices.hasNext()){
					AbstractVertex vertex = itMethodVertices.next();
					vertex.getGraphics().setFill(Color.ORANGE);
					animate(vertex, oldPositions.get(vertex.getStrID()));
					}			
				}
		});
		
		event.consume();
	}
	
	private void savePositions(AbstractVertex v, HashMap<String, Point2D> oldPositions){
		Point2D p = new Point2D(Parameters.stFrame.mainPanel.scaleX(v.getX()), Parameters.stFrame.mainPanel.scaleY(v.getY()));
		oldPositions.put(v.getStrID(), p);
		
		Iterator<AbstractVertex> it = v.getInnerGraph().getVertices().values().iterator();
		while (it.hasNext())
		{
			savePositions(it.next(),oldPositions);
		}
	}

	public void animate(AbstractVertex v, Point2D p)
	{
		
		//v.getGraphics().setFill(Color.ORANGE);
		TranslateTransition tt = new TranslateTransition(Duration.millis(300), v.getGraphics());
		
		tt.setByX(Parameters.stFrame.mainPanel.scaleX(v.getX())-p.getX());
		tt.setByY(Parameters.stFrame.mainPanel.scaleY(v.getY())-p.getY());
		tt.play();
		
//		Iterator<AbstractVertex> it = v.getInnerGraph().getVertices().values().iterator();
//		while (it.hasNext())
//		{
//			animate(it.next(),oldPositions);
//		}
	}

	private void handlePrimarySingleClick(MouseEvent event)
	{

	}
}
