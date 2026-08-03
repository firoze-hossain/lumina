// SettingsCodeEditingPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > Code Editing settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsCodeEditingPage extends VBox {

    public SettingsCodeEditingPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Highlight on Caret Movement section
        // ============================================================
        Label highlightLabel = new Label("Highlight on Caret Movement");
        highlightLabel.getStyleClass().add("settings-section");

        VBox highlightBox = new VBox(4);
        highlightBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox matchedBrace = new CheckBox("Matched brace");
        matchedBrace.setSelected(true);
        matchedBrace.getStyleClass().add("settings-check");

        CheckBox currentScope = new CheckBox("Current scope");
        currentScope.setSelected(false);
        currentScope.getStyleClass().add("settings-check");

        CheckBox usagesAtCaret = new CheckBox("Usages of element at caret");
        usagesAtCaret.setSelected(true);
        usagesAtCaret.getStyleClass().add("settings-check");

        highlightBox.getChildren().addAll(matchedBrace, currentScope, usagesAtCaret);

        // ============================================================
        // Quick Documentation section
        // ============================================================
        Label quickDocLabel = new Label("Quick Documentation");
        quickDocLabel.getStyleClass().add("settings-section");

        VBox quickDocBox = new VBox(4);
        quickDocBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox showDocOnHover = new CheckBox("Show quick documentation on hover");
        showDocOnHover.setSelected(true);
        showDocOnHover.getStyleClass().add("settings-check");

        quickDocBox.getChildren().add(showDocOnHover);

        // ============================================================
        // Refactorings section
        // ============================================================
        Label refactorLabel = new Label("Refactorings");
        refactorLabel.getStyleClass().add("settings-section");

        VBox refactorBox = new VBox(4);
        refactorBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox specifyOptions = new CheckBox("Specify refactoring options:");
        specifyOptions.setSelected(true);
        specifyOptions.getStyleClass().add("settings-check");

        VBox specifyOptionsBox = new VBox(4);
        specifyOptionsBox.setPadding(new Insets(4, 0, 4, 20));

        CheckBox inEditor = new CheckBox("In the editor");
        inEditor.setSelected(false);
        inEditor.getStyleClass().add("settings-check");

        CheckBox inModalDialogs = new CheckBox("In modal dialogs");
        inModalDialogs.setSelected(false);
        inModalDialogs.getStyleClass().add("settings-check");

        specifyOptionsBox.getChildren().addAll(inEditor, inModalDialogs);

        CheckBox preselectSymbol = new CheckBox("Preselect current symbol name for Rename refactoring");
        preselectSymbol.setSelected(true);
        preselectSymbol.getStyleClass().add("settings-check");

        CheckBox showInlineDialog = new CheckBox("Show inline dialog for local variables");
        showInlineDialog.setSelected(true);
        showInlineDialog.getStyleClass().add("settings-check");

        refactorBox.getChildren().addAll(
            specifyOptions,
            specifyOptionsBox,
            preselectSymbol,
            showInlineDialog
        );

        // ============================================================
        // Error Highlighting section
        // ============================================================
        Label errorHighlightLabel = new Label("Error Highlighting");
        errorHighlightLabel.getStyleClass().add("settings-section");

        VBox errorHighlightBox = new VBox(4);
        errorHighlightBox.setPadding(new Insets(4, 0, 8, 20));

        HBox minHeightRow = new HBox(8);
        minHeightRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox errorStripeMark = new CheckBox("Error stripe mark min height:");
        errorStripeMark.setSelected(true);
        errorStripeMark.getStyleClass().add("settings-check");
        Spinner<Integer> minHeightSpinner = new Spinner<>(1, 10, 2, 1);
        minHeightSpinner.setPrefWidth(60);
        minHeightSpinner.getStyleClass().add("settings-spinner");
        Label pixelsLabel = new Label("pixels");
        pixelsLabel.getStyleClass().add("settings-label");
        minHeightRow.getChildren().addAll(errorStripeMark, minHeightSpinner, pixelsLabel);

        HBox autoreparseRow = new HBox(8);
        autoreparseRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox autoreparse = new CheckBox("Autoreparse delay:");
        autoreparse.setSelected(true);
        autoreparse.getStyleClass().add("settings-check");
        Spinner<Integer> autoreparseSpinner = new Spinner<>(100, 2000, 300, 50);
        autoreparseSpinner.setPrefWidth(70);
        autoreparseSpinner.getStyleClass().add("settings-spinner");
        Label msLabel = new Label("milliseconds");
        msLabel.getStyleClass().add("settings-label");
        autoreparseRow.getChildren().addAll(autoreparse, autoreparseSpinner, msLabel);

        HBox nextErrorRow = new HBox(8);
        nextErrorRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox nextError = new CheckBox("The 'Next Error' action goes through:");
        nextError.setSelected(true);
        nextError.getStyleClass().add("settings-check");
        CheckBox highestPriority = new CheckBox("The problems with the highest priority");
        highestPriority.setSelected(false);
        highestPriority.getStyleClass().add("settings-check");
        nextErrorRow.getChildren().addAll(nextError, highestPriority);

        CheckBox suppressWarnings = new CheckBox("Suppress with @SuppressWarnings");
        suppressWarnings.setSelected(true);
        suppressWarnings.getStyleClass().add("settings-check");

        errorHighlightBox.getChildren().addAll(
            minHeightRow,
            autoreparseRow,
            nextErrorRow,
            suppressWarnings
        );

        // ============================================================
        // Editor Tooltips section
        // ============================================================
        Label tooltipsLabel = new Label("Editor Tooltips");
        tooltipsLabel.getStyleClass().add("settings-section");

        HBox tooltipRow = new HBox(8);
        tooltipRow.setPadding(new Insets(4, 0, 8, 20));
        tooltipRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox tooltipDelay = new CheckBox("Tooltip delay:");
        tooltipDelay.setSelected(false);
        tooltipDelay.getStyleClass().add("settings-check");
        Spinner<Integer> tooltipSpinner = new Spinner<>(100, 3000, 500, 100);
        tooltipSpinner.setPrefWidth(70);
        tooltipSpinner.getStyleClass().add("settings-spinner");
        Label tooltipMsLabel = new Label("milliseconds");
        tooltipMsLabel.getStyleClass().add("settings-label");

        tooltipRow.getChildren().addAll(tooltipDelay, tooltipSpinner, tooltipMsLabel);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            highlightLabel,
            highlightBox,
            quickDocLabel,
            quickDocBox,
            refactorLabel,
            refactorBox,
            errorHighlightLabel,
            errorHighlightBox,
            tooltipsLabel,
            tooltipRow
        );
    }
}