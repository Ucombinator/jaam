import java.util.Iterator;
import java.util.Observable;

import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class AnimationHandler implements javafx.event.EventHandler<javafx.scene.input.MouseEvent>{

	AbstractVertex vertex;
	public AnimationHandler(AbstractVertex v){
		this.vertex = v;
	}
	@Override
	public void handle(MouseEvent event) {
		EventType<MouseEvent> type = (EventType<MouseEvent>)event.getEventType();
		if(type.equals(MouseEvent.MOUSE_CLICKED)){
			if(event.getButton().equals(MouseButton.PRIMARY)){
				switch (event.getClickCount()) {
				case 1:
					handlePrimarySingleClick(event);
					break;
				case 2:
					handlePrimaryDoubleClick(event);
					break;
				default:
					break;
				}
			}else if(event.getButton().equals(MouseButton.SECONDARY)){
			}else if(event.getButton().equals(MouseButton.MIDDLE)){
			}
		}else{
			System.out.println("This line should never be printed since we add the handler by setOnMouseClicked");
		}
	}

	private void handlePrimaryDoubleClick(MouseEvent event) {
		
		System.out.println("Vertex: "+this.vertex.getLabel() );
		
		
				FadeTransition ft = new FadeTransition(Duration.millis(300), this.vertex.getGraphics());
//				System.out.println("Pane: " + this.vertex.getGraphics().getClass());
//				Iterator<Node> it = this.vertex.getGraphics().getChildren().iterator();
//				while(it.hasNext()){
//					System.out.println(it.next().getClass());
//				}
				//ft.setFromValue(1.0);
				ft.setToValue(0.0);
				//ft.setCycleCount(Timeline.INDEFINITE);
				//ft.setAutoReverse(true);
				ft.play();
				event.consume();
				ft.setOnFinished(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						vertex.getGraphics().setVisible(false);
					}
				});
				
		
	}

	private void handlePrimarySingleClick(MouseEvent event) {
		
		
	}


}
