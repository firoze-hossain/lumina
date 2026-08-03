package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Appearance settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsAppearancePage extends VBox {

    public SettingsAppearancePage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(12);

        // ============================================================
        // Editor Appearance Section
        // ============================================================

        // Caret blinking (ms)
        HBox caretRow = new HBox(8);
        caretRow.setAlignment(Pos.CENTER_LEFT);
        Label caretLabel = new Label("Caret blinking (ms):");
        caretLabel.getStyleClass().add("settings-label");
        Spinner<Integer> caretSpinner = new Spinner<>(0, 2000, 500, 50);
        caretSpinner.setPrefWidth(80);
        caretSpinner.getStyleClass().add("settings-spinner");
        caretRow.getChildren().addAll(caretLabel, caretSpinner);

        // Use block caret
        CheckBox blockCaret = new CheckBox("Use block caret");
        blockCaret.getStyleClass().add("settings-check");

        // Use full line height caret
        CheckBox fullLineCaret = new CheckBox("Use full line height caret");
        fullLineCaret.getStyleClass().add("settings-check");

        // Highlight occurrences of selected text
        CheckBox highlightOccurrences = new CheckBox("Highlight occurrences of selected text");
        highlightOccurrences.getStyleClass().add("settings-check");

        // Show hard wrap and visual guides
        CheckBox showHardWrap = new CheckBox("Show hard wrap and visual guides (configured in Code Style options)");
        showHardWrap.getStyleClass().add("settings-check");

        // Show line numbers
        HBox lineNumbersRow = new HBox(8);
        lineNumbersRow.setAlignment(Pos.CENTER_LEFT);
        Label lineNumbersLabel = new Label("Show line numbers:");
        lineNumbersLabel.getStyleClass().add("settings-label");
        ComboBox<String> lineNumbersCombo = new ComboBox<>();
        lineNumbersCombo.getItems().addAll("Absolute", "Relative", "None");
        lineNumbersCombo.getSelectionModel().selectFirst();
        lineNumbersCombo.getStyleClass().add("settings-combo");
        lineNumbersRow.getChildren().addAll(lineNumbersLabel, lineNumbersCombo);

        // Show method separators
        CheckBox methodSeparators = new CheckBox("Show method separators");
        methodSeparators.getStyleClass().add("settings-check");

        // Show whitespaces
        CheckBox showWhitespaces = new CheckBox("Show whitespaces");
        showWhitespaces.getStyleClass().add("settings-check");

        HBox whitespaceOptions = new HBox(16);
        whitespaceOptions.setPadding(new Insets(4, 0, 0, 20));
        whitespaceOptions.setAlignment(Pos.CENTER_LEFT);

        CheckBox leadingWhitespace = new CheckBox("Leading");
        leadingWhitespace.getStyleClass().add("settings-check");
        CheckBox innerWhitespace = new CheckBox("Inner");
        innerWhitespace.getStyleClass().add("settings-check");
        CheckBox trailingWhitespace = new CheckBox("Trailing");
        trailingWhitespace.getStyleClass().add("settings-check");

        whitespaceOptions.getChildren().addAll(leadingWhitespace, innerWhitespace, trailingWhitespace);

        // Selection - just a label with text
        Label selectionLabel = new Label("Selection");
        selectionLabel.getStyleClass().add("settings-section");

        // Show indent guides
        CheckBox indentGuides = new CheckBox("Show indent guides");
        indentGuides.getStyleClass().add("settings-check");

        // Show intention bulb
        CheckBox intentionBulb = new CheckBox("Show intention bulb");
        intentionBulb.getStyleClass().add("settings-check");

        // Show preview for intention actions
        CheckBox intentionPreview = new CheckBox("Show preview for intention actions when available");
        intentionPreview.getStyleClass().add("settings-check");

        // Render documentation comments
        CheckBox renderDocComments = new CheckBox("Render documentation comments");
        renderDocComments.getStyleClass().add("settings-check");
        Label docCommentsHint = new Label("Also in Reader mode");
        docCommentsHint.getStyleClass().add("settings-hint");
        docCommentsHint.setPadding(new Insets(0, 0, 0, 20));

        // Show code lens on scrollbar hover
        CheckBox codeLens = new CheckBox("Show code lens on scrollbar hover");
        codeLens.getStyleClass().add("settings-check");

        // Use editor font for inlay hints
        CheckBox editorFontInlay = new CheckBox("Use editor font for inlay hints");
        editorFontInlay.getStyleClass().add("settings-check");

        // Enable HTML/XML tag tree highlighting
        CheckBox tagTreeHighlighting = new CheckBox("Enable HTML/XML tag tree highlighting");
        tagTreeHighlighting.getStyleClass().add("settings-check");

        // Levels to highlight
        HBox levelsRow = new HBox(8);
        levelsRow.setPadding(new Insets(4, 0, 0, 20));
        levelsRow.setAlignment(Pos.CENTER_LEFT);
        Label levelsLabel = new Label("Levels to highlight:");
        levelsLabel.getStyleClass().add("settings-label");
        Spinner<Integer> levelsSpinner = new Spinner<>(1, 10, 6);
        levelsSpinner.setPrefWidth(70);
        levelsSpinner.getStyleClass().add("settings-spinner");
        levelsRow.getChildren().addAll(levelsLabel, levelsSpinner);

        // Opacity
        HBox opacityRow = new HBox(8);
        opacityRow.setPadding(new Insets(4, 0, 0, 20));
        opacityRow.setAlignment(Pos.CENTER_LEFT);
        Label opacityLabel = new Label("Opacity:");
        opacityLabel.getStyleClass().add("settings-label");
        Spinner<Double> opacitySpinner = new Spinner<>(0.0, 1.0, 0.1, 0.05);
        opacitySpinner.setPrefWidth(80);
        opacitySpinner.getStyleClass().add("settings-spinner");
        opacityRow.getChildren().addAll(opacityLabel, opacitySpinner);

        // Show CSS color preview as background
        CheckBox cssColorPreview = new CheckBox("Show CSS color preview as background");
        cssColorPreview.getStyleClass().add("settings-check");

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            caretRow,
            blockCaret,
            fullLineCaret,
            highlightOccurrences,
            showHardWrap,
            lineNumbersRow,
            methodSeparators,
            showWhitespaces,
            whitespaceOptions,
            selectionLabel,
            indentGuides,
            intentionBulb,
            intentionPreview,
            renderDocComments,
            docCommentsHint,
            codeLens,
            editorFontInlay,
            tagTreeHighlighting,
            levelsRow,
            opacityRow,
            cssColorPreview
        );
    }
}