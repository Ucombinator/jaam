package application;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import gui.CellType;
import graph.Graph;
import graph.Model;

import java.util.Random;

public class Main extends Application {

    Graph graph = new Graph();

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        graph = new Graph();

        root.setCenter(graph.getScrollPane());

        Scene scene = new Scene(root, graph.width, graph.height);

        int nodes = 10000;
        createSampleGraph(nodes);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void createSampleGraph(int nodes) {

        Model model = graph.getModel();

        graph.beginUpdate();

        for(int i = 0; i < nodes; i++)
            model.addCell("Cell " + Integer.toString(i), CellType.RECTANGLE);

        if(nodes > 1)
        {
            int edges = 2*nodes;
            Random rnd = new Random();
            for(int i = 0; i < edges; i++)
            {
                int index1 = rnd.nextInt(nodes);
                int index2 = rnd.nextInt(nodes);
                model.addEdge("Cell " + Integer.toString(index1), "Cell " + Integer.toString(index2));
            }
        }

        System.out.println("Created all nodes and edges...");

        graph.endUpdate();
        graph.gridLayout();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
