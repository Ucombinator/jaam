package jviz.uci.edu;

import java.awt.GraphicsEnvironment;

import javafx.scene.paint.Color;
import javafx.scene.Group;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import javafx.scene.Scene;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import graphs.jviz.uci.edu.Graph;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

public class Main {

	public static JSLayout jsLayout;
	
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initAndShowGUI();
            }
        });
    }    
    
    private static void initAndShowGUI() {
        // This method is invoked on the EDT thread
        JFrame frame = new JFrame("J-Viz");
        final JFXPanel fxPanel = new JFXPanel();
        frame.add(fxPanel);
        frame.setSize(
        		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(),
        		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight()
        		);
        fxPanel.setSize(
        		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(),
        		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight()
        		);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(fxPanel);
            }
       });
    }
    
    private static void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        Scene scene = createScene();
        fxPanel.setScene(scene);
    }

    private static Scene createScene() {
        Group  root  =  new  Group();
        Scene  scene  =  new  Scene(root, Color.WHITE);
        
        WebView browser = new WebView();
        browser.setPrefSize(
        		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(),
        		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight()
        		);
        WebEngine webEngine = browser.getEngine();
        //webEngine.load("file:/"+(System.getProperty("user.dir")+"/html/intex.html").replace("/", "//"));
        
        //String d3src = "file://"+(System.getProperty("user.dir")+"/libs/d3.v4.min.js").replaceAll(" ","%20");
        String d3src = "https://d3js.org/d3.v4.min.js";
        String html = "<!DOCTYPE html>"
        		+"<html lang='en'>"
        		+"<head>"
        		+"<script src='"+d3src+"'></script>"
        		+"</head>"
        		+"<body onload='java.initLayout()'>"
        		+    "<center>"
        		+        "<svg id='svgcontainer'  style='border-style: solid;'>"
        		+        "</svg>"
        		+    "</center>"
        		+"</body>"
        		+"</html>"; 
        webEngine.loadContent(
        		html
        		);
        
        root.getChildren().add(browser);
        
        int nodes = 100;
        int edges = 150;
        Graph graph = Graph.getSampleGraph(nodes, edges);
        //Graph graph = Graph.getSampleGraph();
        jsLayout = new JSLayout(webEngine,graph);
        
        JSObject jsobj = (JSObject) webEngine.executeScript("window");
        jsobj.setMember("java", jsLayout);
        
        return (scene);
    }

}

