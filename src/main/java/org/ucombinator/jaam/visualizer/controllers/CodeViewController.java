package org.ucombinator.jaam.visualizer.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.ucombinator.jaam.visualizer.codeView.CodeAreaGenerator;
import java.io.IOException;

public class CodeViewController {

    @FXML public final VBox root = null; // Initialized by Controllers.loadFXML()
    @FXML public final TabPane codeTabs = null; // Initialized by Controllers.loadFXML()

    CodeAreaGenerator codeAreaGenerator;

    public CodeViewController(CodeAreaGenerator codeAreaGenerator) throws IOException {
        Controllers.loadFXML("/CodeView.fxml", this);

        this.codeAreaGenerator = codeAreaGenerator;

        Tab testTab = new Tab("Test", this.codeAreaGenerator.generateCodeArea());

        codeTabs.getTabs().add(testTab);
    }

}
