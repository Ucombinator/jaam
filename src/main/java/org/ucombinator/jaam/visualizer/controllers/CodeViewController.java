package org.ucombinator.jaam.visualizer.controllers;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.ucombinator.jaam.visualizer.codeView.CodeAreaGenerator;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.CodeArea;
import org.ucombinator.jaam.visualizer.gui.SearchResults;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutLoopVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutMethodVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutSccVertex;

import java.io.File;
import java.io.IOException;

public class CodeViewController {

    @FXML public final VBox root = null; // Initialized by Controllers.loadFXML()
    @FXML public final TabPane codeTabs = null; // Initialized by Controllers.loadFXML()

    CodeAreaGenerator codeAreaGenerator;

    public CodeViewController(CodeAreaGenerator codeAreaGenerator) throws IOException {
        Controllers.loadFXML("/CodeView.fxml", this);

        this.codeAreaGenerator = codeAreaGenerator;

    }

    public void addSelectHandler(BorderPane centerPane) {
        centerPane.addEventHandler(SelectEvent.VERTEX_SELECTED, onVertexSelect);
    }

    EventHandler<SelectEvent> onVertexSelect = new EventHandler<SelectEvent>() {
        @Override
        public void handle(SelectEvent selectEvent) {

            AbstractLayoutVertex av = selectEvent.getVertex();

            if(av instanceof LayoutMethodVertex)
            {
                LayoutMethodVertex v = (LayoutMethodVertex)av;

                Tab newTab = new Tab(v.getClassName(), codeAreaGenerator.generateCodeArea(v.getClassName()) );
                codeTabs.getTabs().add(newTab);
            }
            if(av instanceof LayoutLoopVertex)
            {
                LayoutLoopVertex v = (LayoutLoopVertex) av;

                Tab newTab = new Tab(v.getClassName(), codeAreaGenerator.generateCodeArea(v.getClassName()) );
                codeTabs.getTabs().add(newTab);
            }
        }
    };


}
