package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Smart Keys settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsSmartKeysPage extends VBox {

    public SettingsSmartKeysPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(0, 0, 0, 0));
        setSpacing(14);

        // ============================================================
        // Smart Keys Section
        // ============================================================
        
        // Home moves caret to first non-whitespace character
        CheckBox homeMovesCaret = new CheckBox("Home moves caret to first non-whitespace character");
        homeMovesCaret.getStyleClass().add("settings-check");

        // End on blank line moves caret to indent position
        CheckBox endOnBlankLine = new CheckBox("End on blank line moves caret to indent position");
        endOnBlankLine.getStyleClass().add("settings-check");

        // Insert paired brackets
        CheckBox insertPairedBrackets = new CheckBox("Insert paired brackets (), [], {}, <>");
        insertPairedBrackets.getStyleClass().add("settings-check");

        // Insert pair quote
        CheckBox insertPairQuote = new CheckBox("Insert pair quote");
        insertPairQuote.getStyleClass().add("settings-check");

        // Reformat block on typing '}'
        CheckBox reformatBlock = new CheckBox("Reformat block on typing '}'");
        reformatBlock.getStyleClass().add("settings-check");

        // Use "CamelHumps" words
        CheckBox useCamelHumps = new CheckBox("Use \"CamelHumps\" words");
        useCamelHumps.getStyleClass().add("settings-check");

        // Honor "CamelHumps" words settings when selecting on double click
        CheckBox honorCamelHumps = new CheckBox("Honor \"CamelHumps\" words settings when selecting on double click");
        honorCamelHumps.getStyleClass().add("settings-check");

        // Surround selection on typing quote or brace
        CheckBox surroundSelection = new CheckBox("Surround selection on typing quote or brace");
        surroundSelection.getStyleClass().add("settings-check");

        // Add multiple carets on double Ctrl with arrow keys
        CheckBox multipleCarets = new CheckBox("Add multiple carets on double Ctrl with arrow keys");
        multipleCarets.getStyleClass().add("settings-check");

        // Jump outside closing bracket/quote with Tab when typing
        CheckBox jumpOutsideBracket = new CheckBox("Jump outside closing bracket/quote with Tab when typing");
        jumpOutsideBracket.getStyleClass().add("settings-check");

        // ============================================================
        // Enter Section
        // ============================================================
        Label enterLabel = new Label("Enter");
        enterLabel.getStyleClass().add("settings-section");

        CheckBox smartIndent = new CheckBox("Smart indent");
        smartIndent.getStyleClass().add("settings-check");

        CheckBox insertPairBrace = new CheckBox("Insert pair '}'");
        insertPairBrace.getStyleClass().add("settings-check");

        CheckBox closeBlockComment = new CheckBox("Close block comment");
        closeBlockComment.getStyleClass().add("settings-check");

        CheckBox insertDocComment = new CheckBox("Insert documentation comment stub");
        insertDocComment.getStyleClass().add("settings-check");

        // ============================================================
        // Backspace Section
        // ============================================================
        Label backspaceLabel = new Label("Backspace");
        backspaceLabel.getStyleClass().add("settings-section");

        CheckBox unindentOnBackspace = new CheckBox("Unindent on Backspace: To proper indent position");
        unindentOnBackspace.getStyleClass().add("settings-check");

        // ============================================================
        // Reformat on paste Section
        // ============================================================
        Label reformatPasteLabel = new Label("Reformat on paste");
        reformatPasteLabel.getStyleClass().add("settings-section");

        CheckBox indentEachLine = new CheckBox("Indent each line");
        indentEachLine.getStyleClass().add("settings-check");

        CheckBox reformatRemoveBreaks = new CheckBox("Reformat again to remove custom line breaks");
        reformatRemoveBreaks.getStyleClass().add("settings-check");

        // ============================================================
        // JavaDoc Section
        // ============================================================
        Label javadocLabel = new Label("JavaDoc");
        javadocLabel.getStyleClass().add("settings-section");

        CheckBox autoInsertClosingTag = new CheckBox("Automatically insert closing tag in JavaDoc");
        autoInsertClosingTag.getStyleClass().add("settings-check");

        // ============================================================
        // JSP Section
        // ============================================================
        Label jspLabel = new Label("JSP");
        jspLabel.getStyleClass().add("settings-section");

        CheckBox insertPairOnEnter = new CheckBox("Insert pair %> on Enter in JSP");
        insertPairOnEnter.getStyleClass().add("settings-check");

        // ============================================================
        // Kotlin Section
        // ============================================================
        Label kotlinLabel = new Label("Kotlin");
        kotlinLabel.getStyleClass().add("settings-section");

        CheckBox convertPastedJava = new CheckBox("Convert pasted Java code to Kotlin");
        convertPastedJava.getStyleClass().add("settings-check");

        CheckBox dontShowConversionDialog = new CheckBox("Don't show Java to Kotlin conversion dialog on paste");
        dontShowConversionDialog.getStyleClass().add("settings-check");

        CheckBox autoAddVal = new CheckBox("Auto add 'val' keyword to data/value class constructor parameters");
        autoAddVal.getStyleClass().add("settings-check");

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            homeMovesCaret,
            endOnBlankLine,
            insertPairedBrackets,
            insertPairQuote,
            reformatBlock,
            useCamelHumps,
            honorCamelHumps,
            surroundSelection,
            multipleCarets,
            jumpOutsideBracket,
            enterLabel,
            smartIndent,
            insertPairBrace,
            closeBlockComment,
            insertDocComment,
            backspaceLabel,
            unindentOnBackspace,
            reformatPasteLabel,
            indentEachLine,
            reformatRemoveBreaks,
            javadocLabel,
            autoInsertClosingTag,
            jspLabel,
            insertPairOnEnter,
            kotlinLabel,
            convertPastedJava,
            dontShowConversionDialog,
            autoAddVal
        );
    }
}