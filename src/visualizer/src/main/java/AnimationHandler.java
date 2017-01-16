
import java.util.HashMap;
import java.util.Iterator;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class AnimationHandler implements javafx.event.EventHandler<javafx.scene.input.MouseEvent>
{
	public static int transitionTime = 300; // Milliseconds per transition

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
	
	private void collapsing(AbstractVertex v)
	{
		System.out.println("Collapsing node: " + v.id + ", " + v.getGraphics().toString());
		Iterator<Node> it = v.getGraphics().getChildren().iterator();
		while(it.hasNext())
		{
			final Node n = it.next();
			if(!n.getClass().equals(Rectangle.class))
			{
				FadeTransition ft = new FadeTransition(Duration.millis(transitionTime), n);
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

		v.setExpanded(false);
		final AbstractVertex panelRoot = Parameters.stFrame.mainPanel.getPanelRoot();
		LayoutAlgorithm.layout(panelRoot);
		ParallelTransition pt = new ParallelTransition();
		animateRecursive(panelRoot, pt);
		pt.play();

		pt.setOnFinished(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Edge.redrawEdges(panelRoot, true);
			}
		});
	}

	private void expanding(AbstractVertex v)
	{
		Iterator<Node> it = v.getGraphics().getChildren().iterator();
		while(it.hasNext())
		{
			final Node n = it.next();
			if(!n.getClass().equals(Rectangle.class))
			{
				FadeTransition ft = new FadeTransition(Duration.millis(transitionTime), n);
				ft.setToValue(1.0);
				
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
		final AbstractVertex panelRoot = Parameters.stFrame.mainPanel.getPanelRoot();
		LayoutAlgorithm.layout(panelRoot);
		ParallelTransition pt = new ParallelTransition();
		animateRecursive(panelRoot, pt);
		pt.play();

		pt.setOnFinished(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Edge.redrawEdges(panelRoot, true);
			}
		});
	}
	
	private void handlePrimaryDoubleClick(MouseEvent event)
	{
		AbstractVertex v = (((GUINode)(event.getSource())).getVertex());
		if(v.isExpanded())
			collapsing(v);
		else
			expanding(v);
				
		event.consume();
	}
	
	private void animateRecursive(final AbstractVertex v, ParallelTransition pt)
	{
		Iterator<AbstractVertex> it = v.getInnerGraph().getVertices().values().iterator();
		while(it.hasNext()){
			AbstractVertex next = it.next();
			if(v.isExpanded()){
				animateRecursive(next, pt);
			}
		}

		// TODO: For efficiency, we should check if each transition is required before we create it.
		// TODO: Move arrows as well as nodes.
		GUINode node = v.getGraphics();
		double pixelWidth = Parameters.stFrame.mainPanel.scaleX(v.getWidth());
		double pixelHeight = Parameters.stFrame.mainPanel.scaleY(v.getHeight());
		double toScaleX = (pixelWidth/node.getWidth());
		double toScaleY = pixelHeight/node.getHeight();
		double pivotX = 0;
		double pivotY = 0;

		double xShift = 0.5*node.getWidth()*(toScaleX - 1);
		double yShift = 0.5*node.getHeight()*(toScaleY - 1);
		double toX = Parameters.stFrame.mainPanel.scaleX(v.getX()) + xShift;
		double toY = Parameters.stFrame.mainPanel.scaleY(v.getY()) + yShift;

		if(toScaleX != node.getScaleX() || toScaleY != node.getScaleY())
		{
			ScaleTransition st = new ScaleTransition(Duration.millis(transitionTime), node);
			st.setToX(toScaleX);
			st.setToY(toScaleY);
			pt.getChildren().addAll(st);
		}

		if(toX != node.getTranslateX() || toY != node.getTranslateY())
		{
			TranslateTransition tt = new TranslateTransition(Duration.millis(transitionTime), node);
			tt.setToX(toX);
			tt.setToY(toY);
			pt.getChildren().addAll(tt);
		}
	}

	private void handlePrimarySingleClick(MouseEvent event)
	{
		event.consume();
		AbstractVertex v = ((GUINode)(event.getSource())).getVertex();
		HashMap<AbstractVertex, Instruction> instructions = v.getInstructions();
		System.out.println(instructions.values());
		
	}
}