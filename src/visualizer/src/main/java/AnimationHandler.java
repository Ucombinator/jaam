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

	private void handlePrimaryDoubleClickTim(MouseEvent event)
	{
		AbstractVertex v = ((GUINode)(event.getSource())).getVertex();
		if(v.isExpanded())
		{
			System.out.println("Collapsing node: " + v.id);
			// Move center up from half of current height to half of new height
			TranslateTransition tt = new TranslateTransition(Duration.millis(300), v.getGraphics());
			tt.setByX(Parameters.stFrame.mainPanel.scaleX((AbstractVertex.DEFAULT_WIDTH - v.getWidth()) / 2.0));
			tt.setByY(Parameters.stFrame.mainPanel.scaleY(-v.getHeight()/2.0 + AbstractVertex.DEFAULT_HEIGHT/2.0 - LayoutAlgorithm.MARGIN_PADDING));
			tt.play();

			// Scale to new height and width
			ScaleTransition st = new ScaleTransition(Duration.millis(300), v.getGraphics());
			st.setToX(AbstractVertex.DEFAULT_WIDTH / v.getWidth());
			st.setToY(AbstractVertex.DEFAULT_HEIGHT / v.getHeight());
			st.play();

			v.setExpanded(false);

			LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
			st.setOnFinished(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{
					Iterator<AbstractVertex> itMethodVertices = Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values().iterator();

					while (itMethodVertices.hasNext())
					{
						AbstractVertex vertex = itMethodVertices.next();
						animate(vertex);
					}
				}
			});
		}
		else
		{
			// Expand vertex
			System.out.println("Expanding node: " + v.id);
			ScaleTransition st = new ScaleTransition(Duration.millis(300), v.getGraphics());
			st.setToX(AbstractVertex.DEFAULT_WIDTH);
			st.setToY(AbstractVertex.DEFAULT_HEIGHT);
			st.play();

			v.setExpanded(true);
			LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
			st.setOnFinished(new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(ActionEvent event)
				{
					Iterator<AbstractVertex> itMethodVertices = Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values().iterator();

					while (itMethodVertices.hasNext())
					{
						AbstractVertex vertex = itMethodVertices.next();
						animate(vertex);
					}
				}
			});
		}
		event.consume();
	}
	
	private void collapsing(MouseEvent event)
	{
		AbstractVertex v = ((GUINode)(event.getSource())).getVertex();
		
		Iterator<Node> it = v.getGraphics().getChildren().iterator();
		while(it.hasNext())
		{
			Node n = it.next();
			if(!n.getClass().equals(Rectangle.class))
			{
				FadeTransition ft = new FadeTransition(Duration.millis(300), n);
				ft.setToValue(0.0);
				
				ft.setOnFinished(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {		
						n.setVisible(false);
					}
				});
				
				ft.play();
				event.consume();
			}
		}
		
		TranslateTransition tt = new TranslateTransition(Duration.millis(300), v.getGraphics());
		tt.setByY(Parameters.stFrame.mainPanel.scaleY(-v.getHeight()/2.0 + AbstractVertex.DEFAULT_HEIGHT/2.0));
		tt.play();
		
		
		ScaleTransition st = new ScaleTransition(Duration.millis(300), v.getGraphics());

		//st.setFromX(Parameters.stFrame.mainPanel.scaleX(1.0));
		st.setByX(AbstractVertex.DEFAULT_WIDTH/v.getWidth() -1 );
		//st.setFromY(Parameters.stFrame.mainPanel.scaleY(1.0));
		st.setByY(AbstractVertex.DEFAULT_HEIGHT/v.getHeight() -1);
		st.play();

		System.out.println("BEFORE");
		Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().printCoordinates();
		
		HashMap<String, Point2D> oldPositions = new HashMap<>();
		savePositions(Parameters.stFrame.mainPanel.getPanelRoot(), oldPositions);
		oldPositions.put(v.getStrID(), new Point2D(Parameters.stFrame.mainPanel.scaleX(v.getX()+LayoutAlgorithm.MARGIN_PADDING), Parameters.stFrame.mainPanel.scaleY(v.getY()-v.getHeight()/2.0 + AbstractVertex.DEFAULT_HEIGHT/2.0+LayoutAlgorithm.MARGIN_PADDING)));
		
		/*****************************************************/
		v.setExpanded(false);
		System.out.println("Coordinated of the double-clicked vertex:");
		v.printCoordinates();
		LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
		v.printCoordinates();
		/*****************************************************/
		
		System.out.println("AFTER");
		Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().printCoordinates();
		
		st.setOnFinished(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {		
				Iterator<AbstractVertex> itMethodVertices = Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values().iterator();
				
				while(itMethodVertices.hasNext()){
					AbstractVertex vertex = itMethodVertices.next();
					animate(vertex);
					}			
				}
		});

	}
	
	private void expansing(MouseEvent event)
	{
		AbstractVertex v = ((GUINode)(event.getSource())).getVertex();
		
		Iterator<Node> it = v.getGraphics().getChildren().iterator();
		while(it.hasNext())
		{
			Node n = it.next();
			if(!n.getClass().equals(Rectangle.class))
			{
				FadeTransition ft = new FadeTransition(Duration.millis(300), n);
				ft.setToValue(1);
				
				ft.setOnFinished(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {		
						n.setVisible(true);
					}
				});
				
				ft.play();
				event.consume();
			}
		}
		
		
					System.out.println("Expanding node: " + v.id);
					ScaleTransition st = new ScaleTransition(Duration.millis(300), v.getGraphics());
					st.setToX(AbstractVertex.DEFAULT_WIDTH);
					st.setToY(AbstractVertex.DEFAULT_HEIGHT);
					st.play();

					v.setExpanded(true);
					LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
					st.setOnFinished(new EventHandler<ActionEvent>()
					{
						@Override
						public void handle(ActionEvent event)
						{
							Iterator<AbstractVertex> itMethodVertices = Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values().iterator();

							while (itMethodVertices.hasNext())
							{
								AbstractVertex vertex = itMethodVertices.next();
								animate(vertex);
							}
						}
					});
					


	}
	
	private void handlePrimaryDoubleClick(MouseEvent event)
	{
		if((((GUINode)(event.getSource())).getVertex()).isExpanded()){
			collapsing(event);
		}else{
			expansing(event);
		}
				
		event.consume();
	}

	
	private void savePositions(AbstractVertex v, HashMap<String, Point2D> oldPositions){
		
		
		oldPositions.put(v.getStrID(), 
				new Point2D(Parameters.stFrame.mainPanel.scaleX(v.getX()), 
							Parameters.stFrame.mainPanel.scaleY(v.getY())));
		
		Iterator<AbstractVertex> it = v.getInnerGraph().getVertices().values().iterator();
		while (it.hasNext())
		{
			savePositions(it.next(),oldPositions);
		}
	}

	public void animate(AbstractVertex v)
	{
		
//		v.getGraphics().setLayoutX(Parameters.stFrame.mainPanel.scaleX(v.getX()));
//		v.getGraphics().setLayoutY(Parameters.stFrame.mainPanel.scaleY(v.getY()));
		TranslateTransition tt = new TranslateTransition(Duration.millis(300), v.getGraphics());
		double oldWidth = v.getGraphics().getWidth();
		double currentWidth = v.getGraphics().getScaleX()*oldWidth;
		double oldHeight = v.getGraphics().getHeight();
		double currentHeight = v.getGraphics().getScaleY()*oldHeight;
		
//		tt.setToX(Parameters.stFrame.mainPanel.scaleX(v.getX()) - (oldWidth-currentWidth)/2);
//		tt.setToY(Parameters.stFrame.mainPanel.scaleY(v.getY()) - (oldHeight-currentHeight)/2);
		tt.setToX(Parameters.stFrame.mainPanel.scaleX(v.getX()) - (oldWidth-currentWidth)/2);
		tt.setToY(Parameters.stFrame.mainPanel.scaleY(v.getY()) - (oldHeight-currentHeight)/2);
		tt.play();

	}

	private void handlePrimarySingleClick(MouseEvent event)
	{
		AbstractVertex v = ((GUINode)(event.getSource())).getVertex();
		System.out.println("Single click: " + v.getStrID());
		event.consume();
	}
}
