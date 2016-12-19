import java.awt.Dimension;
import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class Main
{
	public static Graph graph;
	
	public static void main(String[] args) {		
		
		/*************************************************************/
		//javax.swing.SwingUtilities.invokeLater(new Runnable() {
		//Platform.runLater(new Runnable() {  
          //  public void run() {
		TakeInput ti = new TakeInput();
		boolean load = false;
		String file = "";
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("--refresh") || args[i].equalsIgnoreCase("-r")) {
				Parameters.interval = Long.parseLong(args[i].trim());
			} else if (args[i].equalsIgnoreCase("--limit") || args[i].equalsIgnoreCase("-l")) {
				Parameters.limitV = Long.parseLong(args[i].trim());
			} else if (args[i].equalsIgnoreCase("--file") || args[i].equalsIgnoreCase("-f")) {
				i++;

				if (args[i].startsWith("./") || args[i].startsWith("/")) {
					file = args[i];
				} else {
					file = "./" + args[i];
				}

				Parameters.pwd = Parameters.folderFromPath(file);

				load = true;
			} else if (args[i].equalsIgnoreCase("--pipe") || args[i].equalsIgnoreCase("-p")) {
				load = true;
			} else if (args[i].equalsIgnoreCase("--debug") || args[i].equalsIgnoreCase("-d")) {
				i++;
				Parameters.debug1 = Integer.parseInt(args[i].trim());
				i++;
				Parameters.debug2 = Integer.parseInt(args[i].trim());

			} else {
				System.out.println("ignoring invalid input option:\"" + args[i] + "\"");
			}
		}
		
		ti.run(file, true);

		JandGioApp app = new JandGioApp();
			app.start();
//		JFrame frame = new JFrame("HelloWorldSwing");
//		          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		          
//		          frame.setPreferredSize(new Dimension(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(),
//		        			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight())
//		        			);
//		        //Add the ubiquitous "Hello World" label.
//
//		          frame.add(new GraphExplorer(frame, true));
//
//		          //Display the window.
//		          frame.setResizable(false);
//		          frame.pack();
//		          frame.setVisible(true);
//		    //  }
//		//});
		/*************************************************************/
		return;
		
//		Parameters.stFrame = new StacFrame();
//		boolean load = false;
//		TakeInput ti = new TakeInput();
//		String file = "";
//
//		for (int i = 0; i < args.length; i++) {
//			if (args[i].equalsIgnoreCase("--refresh") || args[i].equalsIgnoreCase("-r")) {
//				Parameters.interval = Long.parseLong(args[i].trim());
//			} else if (args[i].equalsIgnoreCase("--limit") || args[i].equalsIgnoreCase("-l")) {
//				Parameters.limitV = Long.parseLong(args[i].trim());
//			} else if (args[i].equalsIgnoreCase("--file") || args[i].equalsIgnoreCase("-f")) {
//				i++;
//
//				if (args[i].startsWith("./") || args[i].startsWith("/")) {
//					file = args[i];
//				} else {
//					file = "./" + args[i];
//				}
//
//				Parameters.pwd = Parameters.folderFromPath(file);
//
//				load = true;
//			} else if (args[i].equalsIgnoreCase("--pipe") || args[i].equalsIgnoreCase("-p")) {
//				load = true;
//			} else if (args[i].equalsIgnoreCase("--debug") || args[i].equalsIgnoreCase("-d")) {
//				i++;
//				Parameters.debug1 = Integer.parseInt(args[i].trim());
//				i++;
//				Parameters.debug2 = Integer.parseInt(args[i].trim());
//
//			} else {
//				System.out.println("ignoring invalid input option:\"" + args[i] + "\"");
//			}
//		}
//
//		if (load) {
//			//From the command line, we can only load message file
//			ti.run(file, true);
//			Parameters.repaintAll();
//		}
	}
}
