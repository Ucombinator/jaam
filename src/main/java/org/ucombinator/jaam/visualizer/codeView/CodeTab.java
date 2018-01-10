package org.ucombinator.jaam.visualizer.codeView;

import com.strobel.decompiler.languages.EntityType;
import com.strobel.decompiler.languages.java.ast.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableSet;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import soot.SootClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class CodeTab extends Tab{

    private CompilationUnit unit;
    private SootClass soot;
    private CodeArea javaCodeArea;
    private CodeArea sootCodeArea;
    public final String shortClassName;
    public final String fullClassName;
    private HashMap<String, IndexRange> methodParagraphs;
    private IndexRange currentlySelected;


    public CodeTab(CompilationUnit unit, SootClass soot, String shortClassName, String fullClassName) {
        super(shortClassName);
        this.unit = unit;
        this.soot = soot;
        this.methodParagraphs = new HashMap<>();

        this.setContent(generateCodeAreas(unit, soot));

        this.shortClassName = shortClassName;
        this.fullClassName = fullClassName;

        this.setId(this.fullClassName);

        this.currentlySelected = new IndexRange(0,0); // Empty range
    }

    private Node generateCodeAreas(CompilationUnit unit, SootClass soot)
    {
        assert unit != null;

        this.javaCodeArea = buildJavaCodeArea(unit);
        VirtualizedScrollPane<CodeArea> javaScrollPane = new VirtualizedScrollPane<>(javaCodeArea);
        //javaScrollPane.setMaxHeight(Double.MAX_VALUE);

        this.sootCodeArea = buildSootCodeArea(soot);
        VirtualizedScrollPane<CodeArea> sootScrollPane = new VirtualizedScrollPane<>(sootCodeArea);
        //sootScrollPane.setMaxHeight(Double.MAX_VALUE);
        sootScrollPane.setVisible(false);

        StackPane stackPane = new StackPane(javaScrollPane, sootScrollPane);
        stackPane.setMaxWidth(Double.MAX_VALUE);
        stackPane.setMaxHeight(Double.MAX_VALUE);

        ToggleButton viewToggle = new ToggleButton("Soot");

        viewToggle.selectedProperty().addListener( (observable, oldValue, newValue) -> {
                    javaScrollPane.setVisible(!newValue);
                    sootScrollPane.setVisible( newValue);
                    viewToggle.setText( newValue ? "Java" : "Soot");
                } );

        stackPane.setAlignment(viewToggle, Pos.TOP_RIGHT);

        stackPane.getChildren().add(viewToggle);

        return stackPane;
    }


    private CodeArea buildJavaCodeArea(CompilationUnit unit)
    {
        CodeArea codeArea = new CodeArea();
        codeArea.setEditable(false);

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, CodeHighlighter.computeHighlighting(codeArea.getText()));
                });


        AstNodeCollection<TypeDeclaration> types = unit.getTypes();
        assert types.size() == 1;
        TypeDeclaration typeDeclaration = types.firstOrNullObject();

        // Preamble

        codeArea.appendText(unit.getPackage().getText());

        unit.getImports().stream().forEach(i -> codeArea.appendText(i.getText()));

        // Class Declaration

        StringBuilder classDeclaration = new StringBuilder("class ");

        {
            Identifier className = typeDeclaration.getChildrenByRole(Roles.IDENTIFIER).firstOrNullObject();
            if (!className.isNull()) {
                classDeclaration.append(className.getText());
            } else {
                classDeclaration.append("?????");
            }
        }
        {
            AstType baseType = typeDeclaration.getChildrenByRole(Roles.BASE_TYPE).firstOrNullObject();
            if (!baseType.isNull()) {
                classDeclaration.append(" extends ");
                classDeclaration.append(baseType.getText());
            }
        }
        {
            AstNodeCollection<AstType> interfaces = typeDeclaration.getChildrenByRole(Roles.IMPLEMENTED_INTERFACE);
            if(!interfaces.isEmpty())
            {
                classDeclaration.append(" implements ");
                interfaces.stream().forEach(i -> classDeclaration.append(i.getText() + ", "));
            }
        }

        codeArea.appendText(classDeclaration.toString() + "\n\n");

        // Methods

        for(AstNode i: typeDeclaration.getChildrenByRole(Roles.TYPE_MEMBER))
        {
            EntityDeclaration entity = (EntityDeclaration)i;

            int startParagraph = codeArea.getParagraphs().size()-1;
            if(entity.getEntityType() == EntityType.METHOD || entity.getEntityType() == EntityType.CONSTRUCTOR)
                codeArea.appendText("\n");
            codeArea.appendText(entity.getText());
            int endParagraph = codeArea.getParagraphs().size()-1;

            if(entity.getEntityType() == EntityType.METHOD || entity.getEntityType() == EntityType.CONSTRUCTOR)
            {
                this.methodParagraphs.put(entity.getName(), new IndexRange(startParagraph, endParagraph));
            }
        }

        codeArea.setMaxHeight(Double.MAX_VALUE);

        return codeArea;
    }

    private CodeArea buildSootCodeArea(SootClass soot) {

        CodeArea codeArea = new CodeArea();
        codeArea.setEditable(false);

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, CodeHighlighter.computeHighlighting(codeArea.getText()));
                });


        codeArea.appendText(soot.getName() + "\n");

        codeArea.appendText(soot.getClass().toString());

        //Arrays.stream(soot.getClass().getFields()).forEach(f -> System.out.println(f.getName() + "-->" + f.toString() + "-->" + f.toGenericString()));

        return codeArea;

    }

    public void highlightMethod(String methodName) {

        for (int i = currentlySelected.getStart(); i < currentlySelected.getEnd(); ++i)
        {
            javaCodeArea.setParagraphStyle(i, Collections.EMPTY_LIST);
        }
        this.currentlySelected = new IndexRange(0,0);

        IndexRange range = methodParagraphs.get(methodName);
        if (range == null) {
            System.out.println("Didn't find method " + methodName);
            return;
        }

        javaCodeArea.showParagraphAtTop(range.getStart() - 1);

        for (int i = range.getStart(); i < range.getEnd(); ++i) {
            javaCodeArea.setParagraphStyle(i, Collections.singleton("is-selected"));
        }

        this.currentlySelected = range;
    }
}
