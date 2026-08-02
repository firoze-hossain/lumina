package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General settings page.
 * Complete implementation matching the two screenshots.
 */
public class SettingsEditorGeneralPage extends VBox {

    public SettingsEditorGeneralPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(0, 0, 0, 0));
        setSpacing(18);

        // ============================================================
        // Mouse Control Section
        // ============================================================
        Label mouseControlLabel = new Label("Mouse Control");
        mouseControlLabel.getStyleClass().add("settings-section");

        CheckBox changeFontSize = new CheckBox("Change font size with Ctrl+Mouse Wheel in:");
        changeFontSize.getStyleClass().add("settings-check");

        VBox fontSizeOptions = new VBox(4);
        fontSizeOptions.setPadding(new Insets(4, 0, 8, 20));

        CheckBox activeEditor = new CheckBox("Active editor");
        activeEditor.getStyleClass().add("settings-check");

        CheckBox allEditors = new CheckBox("All editors");
        allEditors.getStyleClass().add("settings-check");

        fontSizeOptions.getChildren().addAll(activeEditor, allEditors);

        CheckBox dragDrop = new CheckBox("Move code fragments with drag-and-drop");
        dragDrop.setSelected(true);
        dragDrop.getStyleClass().add("settings-check");
        Label dragDropHint = new Label("To copy, hold Ctrl while dragging");
        dragDropHint.getStyleClass().add("settings-hint");
        dragDropHint.setPadding(new Insets(0, 0, 0, 20));

        VBox mouseControlBox = new VBox(4, changeFontSize, fontSizeOptions, dragDrop, dragDropHint);

        // ============================================================
        // Soft Wraps Section
        // ============================================================
        Label softWrapsLabel = new Label("Soft Wraps");
        softWrapsLabel.getStyleClass().add("settings-section");

        CheckBox softWrapFiles = new CheckBox("Soft-wrap these files:");
        softWrapFiles.getStyleClass().add("settings-check");

        TextField softWrapPatterns = new TextField("*.md; *.txt; *.rst; *.adoc");
        softWrapPatterns.getStyleClass().add("text-field");
        softWrapPatterns.setPrefWidth(400);
        Label softWrapHint = new Label("Use * and ? as wildcards and ; to separate patterns");
        softWrapHint.getStyleClass().add("settings-hint");
        softWrapHint.setPadding(new Insets(0, 0, 0, 20));

        VBox softWrapBox = new VBox(4, softWrapFiles, softWrapPatterns, softWrapHint);
        softWrapBox.setPadding(new Insets(4, 0, 8, 0));

        CheckBox useOriginalIndent = new CheckBox("Use the original line's indent for wrapped fragments");
        useOriginalIndent.setSelected(true);
        useOriginalIndent.getStyleClass().add("settings-check");

        CheckBox addAdditionalIndent = new CheckBox("Add additional indent:");
        addAdditionalIndent.getStyleClass().add("settings-check");

        HBox indentOptions = new HBox(16);
        indentOptions.setPadding(new Insets(4, 0, 8, 20));
        indentOptions.setAlignment(Pos.CENTER_LEFT);

        RadioButton indent0 = new RadioButton("0");
        RadioButton indentSymbols = new RadioButton("symbols");
        indent0.setSelected(true);

        ToggleGroup indentGroup = new ToggleGroup();
        indent0.setToggleGroup(indentGroup);
        indentSymbols.setToggleGroup(indentGroup);

        indentOptions.getChildren().addAll(indent0, indentSymbols);

        CheckBox softWrapIndicators = new CheckBox("Only show soft-wrap indicators for the current line");
        softWrapIndicators.setSelected(true);
        softWrapIndicators.getStyleClass().add("settings-check");

        VBox softWrapsBox = new VBox(4,
            softWrapBox,
            useOriginalIndent,
            addAdditionalIndent,
            indentOptions,
            softWrapIndicators
        );

        // ============================================================
        // Virtual Space Section
        // ============================================================
        Label virtualSpaceLabel = new Label("Virtual Space");
        virtualSpaceLabel.getStyleClass().add("settings-section");

        Label caretPlacementLabel = new Label("Allow caret placement:");
        caretPlacementLabel.getStyleClass().add("settings-label");

        HBox caretOptions = new HBox(20);
        caretOptions.setPadding(new Insets(4, 0, 8, 0));
        caretOptions.setAlignment(Pos.CENTER_LEFT);

        CheckBox afterEndOfLine = new CheckBox("After the end of line");
        afterEndOfLine.getStyleClass().add("settings-check");

        CheckBox insideTabs = new CheckBox("Inside tabs");
        insideTabs.getStyleClass().add("settings-check");

        caretOptions.getChildren().addAll(afterEndOfLine, insideTabs);

        CheckBox showVirtualSpace = new CheckBox("Show virtual space at the bottom of the file");
        showVirtualSpace.getStyleClass().add("settings-check");

        VBox virtualSpaceBox = new VBox(4, caretPlacementLabel, caretOptions, showVirtualSpace);

        // ============================================================
        // Scroll Offset Section
        // ============================================================
        Label scrollOffsetLabel = new Label("Scroll Offset");
        scrollOffsetLabel.getStyleClass().add("settings-section");

        HBox verticalOffsetRow = new HBox(8);
        verticalOffsetRow.setAlignment(Pos.CENTER_LEFT);
        Label verticalOffsetLabel = new Label("Vertical scroll offset:");
        verticalOffsetLabel.getStyleClass().add("settings-label");
        Spinner<Integer> verticalOffset = new Spinner<>(0, 10, 1);
        verticalOffset.setPrefWidth(70);
        verticalOffset.getStyleClass().add("settings-spinner");
        verticalOffsetRow.getChildren().addAll(verticalOffsetLabel, verticalOffset);

        HBox verticalJumpRow = new HBox(8);
        verticalJumpRow.setAlignment(Pos.CENTER_LEFT);
        Label verticalJumpLabel = new Label("Vertical scroll jump:");
        verticalJumpLabel.getStyleClass().add("settings-label");
        Spinner<Integer> verticalJump = new Spinner<>(0, 10, 0);
        verticalJump.setPrefWidth(70);
        verticalJump.getStyleClass().add("settings-spinner");
        verticalJumpRow.getChildren().addAll(verticalJumpLabel, verticalJump);

        HBox horizontalOffsetRow = new HBox(8);
        horizontalOffsetRow.setAlignment(Pos.CENTER_LEFT);
        Label horizontalOffsetLabel = new Label("Horizontal scroll offset:");
        horizontalOffsetLabel.getStyleClass().add("settings-label");
        Spinner<Integer> horizontalOffset = new Spinner<>(0, 10, 3);
        horizontalOffset.setPrefWidth(70);
        horizontalOffset.getStyleClass().add("settings-spinner");
        horizontalOffsetRow.getChildren().addAll(horizontalOffsetLabel, horizontalOffset);

        HBox horizontalJumpRow = new HBox(8);
        horizontalJumpRow.setAlignment(Pos.CENTER_LEFT);
        Label horizontalJumpLabel = new Label("Horizontal scroll jump:");
        horizontalJumpLabel.getStyleClass().add("settings-label");
        Spinner<Integer> horizontalJump = new Spinner<>(0, 10, 0);
        horizontalJump.setPrefWidth(70);
        horizontalJump.getStyleClass().add("settings-spinner");
        horizontalJumpRow.getChildren().addAll(horizontalJumpLabel, horizontalJump);

        VBox scrollOffsetBox = new VBox(6,
            verticalOffsetRow,
            verticalJumpRow,
            horizontalOffsetRow,
            horizontalJumpRow
        );

        // ============================================================
        // Caret Movement Section
        // ============================================================
        Label caretMovementLabel = new Label("Caret Movement");
        caretMovementLabel.getStyleClass().add("settings-section");

        HBox wordBoundaryRow = new HBox(8);
        wordBoundaryRow.setAlignment(Pos.CENTER_LEFT);
        Label wordBoundaryLabel = new Label("When moving by words:");
        wordBoundaryLabel.getStyleClass().add("settings-label");
        RadioButton jumpToWord = new RadioButton("Jump to the current word boundaries");
        RadioButton wordDefault = new RadioButton("IDE default");
        jumpToWord.setSelected(true);
        ToggleGroup wordGroup = new ToggleGroup();
        jumpToWord.setToggleGroup(wordGroup);
        wordDefault.setToggleGroup(wordGroup);
        wordBoundaryRow.getChildren().addAll(wordBoundaryLabel, jumpToWord, wordDefault);

        HBox lineBreakRow = new HBox(8);
        lineBreakRow.setAlignment(Pos.CENTER_LEFT);
        Label lineBreakLabel = new Label("Upon line break:");
        lineBreakLabel.getStyleClass().add("settings-label");
        RadioButton jumpToLine = new RadioButton("Jump to the next/previous line boundaries");
        RadioButton lineDefault = new RadioButton("IDE default");
        jumpToLine.setSelected(true);
        ToggleGroup lineGroup = new ToggleGroup();
        jumpToLine.setToggleGroup(lineGroup);
        lineDefault.setToggleGroup(lineGroup);
        lineBreakRow.getChildren().addAll(lineBreakLabel, jumpToLine, lineDefault);

        VBox caretMovementBox = new VBox(6, wordBoundaryRow, lineBreakRow);

        // ============================================================
        // Scrolling Section
        // ============================================================
        Label scrollingLabel = new Label("Scrolling");
        scrollingLabel.getStyleClass().add("settings-section");

        CheckBox smoothScrolling = new CheckBox("Enable smooth scrolling");
        smoothScrolling.setSelected(true);
        smoothScrolling.getStyleClass().add("settings-check");

        Label caretBehaviorLabel = new Label("Caret behavior:");
        caretBehaviorLabel.getStyleClass().add("settings-label");
        caretBehaviorLabel.setPadding(new Insets(8, 0, 4, 0));

        RadioButton keepCaret = new RadioButton("Keep the caret in place, scroll editor canvas");
        RadioButton moveCaret = new RadioButton("Move caret, minimize editor scrolling");
        keepCaret.setSelected(true);
        ToggleGroup caretGroup = new ToggleGroup();
        keepCaret.setToggleGroup(caretGroup);
        moveCaret.setToggleGroup(caretGroup);

        VBox caretBehaviorBox = new VBox(4, caretBehaviorLabel, keepCaret, moveCaret);
        caretBehaviorBox.setPadding(new Insets(0, 0, 0, 20));

        VBox scrollingBox = new VBox(4, smoothScrolling, caretBehaviorBox);

        // ============================================================
        // Rich-Text Copy Section
        // ============================================================
        Label richTextLabel = new Label("Rich-Text Copy");
        richTextLabel.getStyleClass().add("settings-section");

        CheckBox copyRichText = new CheckBox("Copy (Ctrl+C) as rich text");
        copyRichText.setSelected(true);
        copyRichText.getStyleClass().add("settings-check");

        Label copyHint = new Label("All formatting will be copied, including font, colors and so on");
        copyHint.getStyleClass().add("settings-hint");
        copyHint.setPadding(new Insets(0, 0, 0, 20));

        HBox colorSchemeRow = new HBox(8);
        colorSchemeRow.setAlignment(Pos.CENTER_LEFT);
        Label colorSchemeLabel = new Label("Color scheme for copied fragment:");
        colorSchemeLabel.getStyleClass().add("settings-label");
        ComboBox<String> colorSchemeCombo = new ComboBox<>();
        colorSchemeCombo.getItems().addAll("Active scheme", "Darcula", "IntelliJ Light");
        colorSchemeCombo.getSelectionModel().selectFirst();
        colorSchemeCombo.getStyleClass().add("settings-combo");
        colorSchemeRow.getChildren().addAll(colorSchemeLabel, colorSchemeCombo);

        VBox richTextBox = new VBox(4, copyRichText, copyHint, colorSchemeRow);

        // ============================================================
        // On Save Section
        // ============================================================
        Label onSaveLabel = new Label("On Save");
        onSaveLabel.getStyleClass().add("settings-section");

        CheckBox removeTrailingSpaces = new CheckBox("Remove trailing spaces on:");
        removeTrailingSpaces.setSelected(true);
        removeTrailingSpaces.getStyleClass().add("settings-check");

        HBox trailingOptions = new HBox(16);
        trailingOptions.setPadding(new Insets(4, 0, 4, 20));
        trailingOptions.setAlignment(Pos.CENTER_LEFT);

        RadioButton modifiedLines = new RadioButton("Modified lines");
        RadioButton allLines = new RadioButton("All lines");
        modifiedLines.setSelected(true);
        ToggleGroup trailingGroup = new ToggleGroup();
        modifiedLines.setToggleGroup(trailingGroup);
        allLines.setToggleGroup(trailingGroup);

        trailingOptions.getChildren().addAll(modifiedLines, allLines);

        CheckBox keepTrailingSpaces = new CheckBox("Keep trailing spaces on caret line");
        keepTrailingSpaces.setSelected(true);
        keepTrailingSpaces.getStyleClass().add("settings-check");

        CheckBox removeBlankLines = new CheckBox("Remove trailing blank lines at the end of saved files");
        removeBlankLines.getStyleClass().add("settings-check");

        CheckBox ensureLineBreak = new CheckBox("Ensure every saved file ends with a line break");
        ensureLineBreak.getStyleClass().add("settings-check");

        VBox onSaveBox = new VBox(4,
            removeTrailingSpaces,
            trailingOptions,
            keepTrailingSpaces,
            removeBlankLines,
            ensureLineBreak
        );

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            mouseControlLabel, mouseControlBox,
            softWrapsLabel, softWrapsBox,
            virtualSpaceLabel, virtualSpaceBox,
            scrollOffsetLabel, scrollOffsetBox,
            caretMovementLabel, caretMovementBox,
            scrollingLabel, scrollingBox,
            richTextLabel, richTextBox,
            onSaveLabel, onSaveBox
        );
    }
}