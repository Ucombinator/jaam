
import java.util.HashMap;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
	
	private void collapse(AbstractVertex v)
	{
		for(final Node n : v.getGraphics().getChildren())
		{
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
			}
		}

		TranslateTransition tt = new TranslateTransition(Duration.millis(300), v.getGraphics());
		tt.setByY(Parameters.stFrame.mainPanel.scaleY(-v.getHeight()/2.0 + AbstractVertex.DEFAULT_HEIGHT/2.0));
		tt.play();

		ScaleTransition st = new ScaleTransition(Duration.millis(300), v.getGraphics());
		st.setByX(AbstractVertex.DEFAULT_WIDTH/v.getWidth() - 1);
		st.setByY(AbstractVertex.DEFAULT_HEIGHT/v.getHeight() - 1);
		st.play();


		System.out.println("BEFORE");
		Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().printCoordinates();
		
		HashMap<String, Point2D> oldPositions = new HashMap<>();
		//savePositions(Parameters.stFrame.mainPanel.getPanelRoot(), oldPositions);
		oldPositions.put(v.getStrID(), new Point2D(Parameters.stFrame.mainPanel.scaleX(v.getX()+LayoutAlgorithm.MARGIN_PADDING), Parameters.stFrame.mainPanel.scaleY(v.getY()-v.getHeight()/2.0 + AbstractVertex.DEFAULT_HEIGHT/2.0+LayoutAlgorithm.MARGIN_PADDING)));
		
		/*****************************************************/
		v.setExpanded(false);
		System.out.println("Coordinated of the double-clicked vertex:");
		v.printCoordinates();
		LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
		v.printCoordinates();
		/*****************************************************/
		
		st.setOnFinished(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event)
			{
				for(AbstractVertex vertex : Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values())
					animate(vertex);
			}
		});

		Edge.redrawEdges(v);
	}
	
	private void expand(AbstractVertex v)
	{
		for(final Node n: v.getGraphics().getChildren())
		{
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
			}
		}

		v.setExpanded(true);
		LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());

		ScaleTransition st = new ScaleTransition(Duration.millis(300), v.getGraphics());
		st.setToX(AbstractVertex.DEFAULT_WIDTH);
		st.setToY(AbstractVertex.DEFAULT_HEIGHT);
		st.play();

		st.setOnFinished(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				for(AbstractVertex vertex : Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values())
					animate(vertex);
			}
		});

		Edge.redrawEdges(v);
	}
	
	private void handlePrimaryDoubleClick(MouseEvent event)
	{
		event.consume();
		AbstractVertex v = ((GUINode)(event.getSource())).getVertex();
		if(v.isExpanded())
			collapse(v);
		else
			expand(v);
	}

	public void animate(AbstractVertex v)
	{

		// Because vertices scale around their center, we have to shift them to keep the top left corner in the correct place.
		double currentWidth = v.getGraphics().getScaleX() * v.getGraphics().getWidth();
		double oldWidth = v.getGraphics().getWidth();
		double currentHeight = v.getGraphics().getScaleY() * v.getGraphics().getHeight();
		double oldHeight = v.getGraphics().getHeight();
		double xShift = (oldWidth - currentWidth) / 2;
		double yShift = (oldHeight - currentHeight) / 2;


		TranslateTransition tt = new TranslateTransition(Duration.millis(300), v.getGraphics());
		tt.setToX(Parameters.stFrame.mainPanel.scaleX(v.getX()) - xShift);
		tt.setToY(Parameters.stFrame.mainPanel.scaleY(v.getY()) - yShift);
		tt.play();
	}

	private void handlePrimarySingleClick(MouseEvent event)
	{
		event.consume();
		AbstractVertex v = ((GUINode)(event.getSource())).getVertex();

		v.toggleSelected();
		// TODO: Find better ways to highlight/unhighlight
		if(v.isSelected())
			v.getGraphics().increaseOpacity();
		else
			v.getGraphics().decreaseOpacity();

		if(v.isSelected())
			Parameters.bytecodeArea.setVertex(v);
		else
			Parameters.bytecodeArea.clear();

		Parameters.bytecodeArea.setDescription();
		// The repaint seems to happen automatically? I'm not sure why.
		//Parameters.repaintAll();
	}
}
