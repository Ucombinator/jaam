package org.ucombinator.jaam.visualizer.codeView;

import com.strobel.decompiler.languages.java.ast.*;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class CodeTab extends Tab{

    private CompilationUnit unit;
    private StackPane stackPane;
    private CodeArea codeArea;
    public final String shortClassName;
    public final String fullClassName;

    public CodeTab(CompilationUnit unit, String shortClassName, String fullClassName) {
        super(shortClassName);
        this.unit = unit;
        generateCodeArea(unit);

        this.setContent(this.stackPane);

        this.shortClassName = shortClassName;
        this.fullClassName = fullClassName;

        this.setId(this.fullClassName);
    }

    private String generateCodeArea(CompilationUnit unit)
    {
        assert unit != null;
        this.codeArea = new CodeArea();

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

        codeArea.setMaxHeight(Double.MAX_VALUE);

        VirtualizedScrollPane scrollPane = new VirtualizedScrollPane(codeArea);
        scrollPane.setMaxHeight(Double.MAX_VALUE);

        this.stackPane = new StackPane(scrollPane);

        this.stackPane.setMaxWidth(Double.MAX_VALUE);
        this.stackPane.setMaxHeight(Double.MAX_VALUE);

        return typeDeclaration.getName();
    }
}
