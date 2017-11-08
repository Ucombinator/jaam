package org.ucombinator.jaam.visualizer.codeView;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.strobel.decompiler.languages.java.ast.*;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public class CodeAreaGenerator {

    HashMap<String, CompilationUnit> classes;

    public CodeAreaGenerator()
    {
        classes = new HashMap<>();
    }

    public StackPane generateCodeArea(String fullClassName)
    {
        CompilationUnit unit = classes.get(fullClassName);
        String text;

        assert unit != null;
        CodeArea codeArea = new CodeArea();

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
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

    private static final String[] KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    private static final String sampleCode = String.join("\n", new String[] {
            "package com.example;",
            "",
            "import java.util.*;",
            "",
            "public class Foo extends Bar implements Baz {",
            "",
            "    /*",
            "     * multi-line comment",
            "     */",
            "    public static void main(String[] args) {",
            "        // single-line comment",
            "        for(String arg: args) {",
            "            if(arg.length() != 0)",
            "                System.out.println(arg);",
            "            else",
            "                System.err.println(\"Warning: empty string as argument\");",
            "        }",
            "    }",
            "",
            "}"
    });

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("PAREN") != null ? "paren" :
                    matcher.group("BRACE") != null ? "brace" :
                    matcher.group("BRACKET") != null ? "bracket" :
                    matcher.group("SEMICOLON") != null ? "semicolon" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("COMMENT") != null ? "comment" :
                    null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }



}
