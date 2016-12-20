import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.BrokenBarrierException;

import javax.swing.JFrame;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


public class JandGioApp{

	public JandGioApp() {
		// TODO Auto-generated constructor stub
	}

	public void start(){
		JFrame frame = new JFrame("HelloWorldSwing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setPreferredSize(new Dimension(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(),
      			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight())
      			);
        

        Group  root  =  new  Group();
        Scene  scene  =  new Scene(root, GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(),GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight(), Color.ALICEBLUE);
        GraphExplorer gr_expl = new GraphExplorer(root);
        gr_expl.setScene(scene);
        
        frame.add(gr_expl);

        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
	}

}
