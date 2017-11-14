package org.ucombinator.jaam.visualizer.controllers;

import com.strobel.decompiler.languages.java.ast.*;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.ucombinator.jaam.visualizer.codeView.CodeHighlighter;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.layout.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class CodeViewController {

    @FXML public final VBox root = null; // Initialized by Controllers.loadFXML()
    @FXML public final TabPane codeTabs = null; // Initialized by Controllers.loadFXML()

    HashMap<String, CompilationUnit> classes;

    public CodeViewController(List<CompilationUnit> compilationUnits) throws IOException {
        Controllers.loadFXML("/CodeView.fxml", this);

        this.classes = new HashMap<>();

        for(CompilationUnit u : compilationUnits)
            addClass(u);
    }

    public void addSelectHandler(BorderPane centerPane) {
        centerPane.addEventHandler(SelectEvent.VERTEX_SELECTED, onVertexSelect);
    }

    public StackPane generateCodeArea(String fullClassName)
    {
        CompilationUnit unit = classes.get(fullClassName);

        assert unit != null;
        CodeArea codeArea = new CodeArea();

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, CodeHighlighter.computeHighlighting(codeArea.getText()));
                });


        AstNodeCollection<TypeDeclaration> types = unit.getTypes();
        assert types.size() == 1;
        TypeDeclaration typeDeclaration = types.firstOrNullObject();

        for(AstNode i: typeDeclaration.getChildrenByRole(Roles.TYPE_MEMBER))
        {
            EntityDeclaration entity = (EntityDeclaration)i;
            //System.out.println(i.getRole() + " " + i.getClass() + " " + entity.getName() + " " + entity.getEntityType());
            //System.out.println("\t" + entity.getText());

            codeArea.appendText(entity.getText() + "\n");
        }

        //String className = getClassName(unit);
        //graph.addClass(className, unit.getText());

        codeArea.setMaxHeight(Double.MAX_VALUE);

        VirtualizedScrollPane scrollPane = new VirtualizedScrollPane(codeArea);
        scrollPane.setMaxHeight(Double.MAX_VALUE);

        StackPane result = new StackPane(scrollPane);

        result.setMaxWidth(Double.MAX_VALUE);
        result.setMaxHeight(Double.MAX_VALUE);

        return result;
    }


    public void addClass(CompilationUnit unit)
    {
        AstNodeCollection<TypeDeclaration> types = unit.getTypes();
        assert types.size() == 1;

        TypeDeclaration typeDeclaration = types.firstOrNullObject();

        typeDeclaration.getText();

        String className = typeDeclaration.getName();
        String fullClassName = new String(unit.getPackage().getName() + "." + className);

        //System.out.println("JUAN FullClassName " + fullClassName);

        this.classes.put(fullClassName, unit);
    }

    EventHandler<SelectEvent> onVertexSelect = new EventHandler<SelectEvent>() {
        @Override
        public void handle(SelectEvent selectEvent) {

            AbstractLayoutVertex av = selectEvent.getVertex();

            if(av instanceof CodeEntity)
            {
                CodeEntity v = (CodeEntity)av;

                Tab t = codeTabs.getTabs().stream().filter(c-> c.getId().equals(v.getClassName())).findFirst().orElse(null);


                if(t == null)
                {
                    t= new Tab(v.getShortClassName(), generateCodeArea(v.getClassName()) );
                    t.setId(v.getClassName());

                    final String tabId = t.getId();
                    t.setOnClosed( e -> {
                        e.consume();

                        System.out.println("Closed tab " + tabId);

                    });

                    codeTabs.getTabs().add(t);
                }

                codeTabs.getSelectionModel().select(t);
            }
        }
    };


}
