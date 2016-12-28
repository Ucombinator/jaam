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
		if(true)
		{
		Iterator<Node> it = v.getGraphics().getChildren().iterator();
		while(it.hasNext())
		{
			Node n = it.next();
			if(!n.getClass().equals(Rectangle.class))
			{
				FadeTransition ft = new FadeTransition(Duration.millis(300), n);
				ft.setToValue(0.0);
				ft.play();
				event.consume();
				ft.setOnFinished(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						//v.getGraphics().setVisible(false);
					}
				});
			}
		}

		ScaleTransition st = new ScaleTransition(Duration.millis(300), v.getGraphics());

		st.setFromX(1.0);
		st.setToX(AbstractVertex.DEFAULT_WIDTH/v.getWidth());
		st.setFromY(1.0);
		st.setToY(AbstractVertex.DEFAULT_HEIGHT/v.getHeight());
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
					animate(itMethodVertices.next(), oldPositions);
					}			
				}
		});
		
		}
//		else
//		{
//			HashMap<String, Point2D> oldPositions = new HashMap<>();
//			savePositions(Parameters.stFrame.mainPanel.getPanelRoot(), oldPositions);
////			v.setExpanded(false);
//			LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
//			
//			Iterator<AbstractVertex> itMethodVertices = Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values().iterator();
//			while(itMethodVertices.hasNext()){
//				animate(itMethodVertices.next(), oldPositions);
//			}
//		}	
	}
	
	private void savePositions(AbstractVertex v, HashMap<String, Point2D> oldPositions){
		Point2D p = new Point2D(v.getX(), v.getY());
		oldPositions.put(v.getStrID(), p);
		
		Iterator<AbstractVertex> it = v.getInnerGraph().getVertices().values().iterator();
		while (it.hasNext())
		{
			savePositions(it.next(),oldPositions);
		}
	}

	public void animate(AbstractVertex v, HashMap<String, Point2D> oldPositions)
	{
		
		v.getGraphics().setFill(Color.ORANGE);
		TranslateTransition tt = new TranslateTransition(Duration.millis(300), v.getGraphics());
		Point2D p =  oldPositions.get(v.getStrID());
		
		System.out.println("Vertex: " + v.getStrID());
		System.out.println("NewX: " + Parameters.stFrame.mainPanel.scaleX(v.getX()));
		System.out.println("NewY: " + Parameters.stFrame.mainPanel.scaleX(v.getY()));
		tt.setByX(Parameters.stFrame.mainPanel.scaleX(-v.getX()+oldPositions.get(v.getStrID()).getX()));
		tt.setByY(Parameters.stFrame.mainPanel.scaleY(-v.getY()+oldPositions.get(v.getStrID()).getY()));
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
