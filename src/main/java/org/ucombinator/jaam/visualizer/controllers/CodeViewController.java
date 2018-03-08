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
import org.ucombinator.jaam.visualizer.codeView.CodeTab;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.layout.*;
import soot.SootClass;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CodeViewController {

    @FXML public final VBox root = null; // Initialized by Controllers.loadFXML()
    @FXML public final TabPane codeTabs = null; // Initialized by Controllers.loadFXML()

    HashMap<String, CodeTab> tabMap;
    HashSet<String> classNames;

    public CodeViewController(List<CompilationUnit> compilationUnits, Set<SootClass> sootClasses) throws IOException {
        Controllers.loadFXML("/CodeView.fxml", this);

        this.tabMap     = new HashMap<>();
        this.classNames = new HashSet<>();

        /*
        for (SootClass s : sootClasses)
        {
            System.out.println(s.getName());
        }
        */

        HashMap<String,SootClass> lookup = new HashMap<String,SootClass>();

        sootClasses.stream().forEach(s -> lookup.put(s.getName(), s));


        for (CompilationUnit u : compilationUnits) {
            String fullClassName = getFullClassName(u);

            SootClass s = lookup.get(fullClassName);

            if (s != null) {
                addClass(u, s);
            } else {
                System.out.println("Warning: Didn't find soot class matching " + fullClassName);
            }

        }
    }

    public void addSelectHandler(BorderPane centerPane) {
        centerPane.addEventHandler(SelectEvent.STATE_VERTEX_SELECTED, onVertexSelect);
    }

    public void addClass(CompilationUnit unit, SootClass s)
    {
        AstNodeCollection<TypeDeclaration> types = unit.getTypes();
        assert types.size() == 1;

        TypeDeclaration typeDeclaration = types.firstOrNullObject();

        typeDeclaration.getText();

        String className = typeDeclaration.getName();
        String fullClassName = new String(unit.getPackage().getName() + "." + className);

        CodeTab tab = new CodeTab(unit, s, className, fullClassName);
        tab.setTooltip(new Tooltip(fullClassName));
        this.tabMap.put(fullClassName, tab);

        this.classNames.add(fullClassName);
    }

    public HashSet<String> getClassNames() {
        return classNames;
    }

    EventHandler<SelectEvent<StateVertex>> onVertexSelect = new EventHandler<SelectEvent<StateVertex>>() {
        @Override
        public void handle(SelectEvent<StateVertex> selectEvent) {

            StateVertex av = selectEvent.getVertex();

            if(av instanceof CodeEntity)
            {
                CodeEntity v = (CodeEntity)av;

                displayCodeTab(v.getClassName(), v.getMethodName());

            }
        }
    };

    public void displayCodeTab(String className) {
        displayCodeTab(className, null);
    }

    public void displayCodeTab(String className, String highlightMethod)
    {
        CodeTab t = tabMap.get(className);

        if(t == null)
        {
            System.out.println("Didn't find code associated to " + className);
            return;
        }

        if(!isDisplayed(t))
        {
            codeTabs.getTabs().add(t);
        }

        codeTabs.getSelectionModel().select(t);

        if(highlightMethod != null)
            t.highlightMethod(highlightMethod);
    }

    boolean isDisplayed(CodeTab t)
    {
        return codeTabs.getTabs().stream().filter(
                c-> ((CodeTab)c).fullClassName.equals(t.fullClassName)).findFirst().orElse(null)
                != null;
    }


    private String getFullClassName(CompilationUnit u)
    {
        return u.getPackage().getName() + "." + u.getTypes().firstOrNullObject().getName();
    }
}
