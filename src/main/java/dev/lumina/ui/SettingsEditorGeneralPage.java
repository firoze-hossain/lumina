package dev.lumina.ui;

import dev.lumina.settings.EditorGeneralSettings;
import dev.lumina.settings.EditorGeneralSettings.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Pixel-perfect IntelliJ IDEA-style Editor > General settings page.
 * Dynamically binds to EditorGeneralSettings model, supporting isModified(),
 * apply(), and reset() lifecycle with live modification notifications.
 */
public class SettingsEditorGeneralPage extends VBox {

    // 1. Mouse Control
    private final CheckBox changeFontSizeCheck = new CheckBox("Change font size with Ctrl+Mouse Wheel in:");
    private final RadioButton activeEditorRadio = new RadioButton("Active editor");
    private final RadioButton allEditorsRadio = new RadioButton("All editors");
    private final CheckBox dragDropCheck = new CheckBox("Move code fragments with drag-and-drop");

    // 2. Soft Wraps
    private final CheckBox softWrapFilesCheck = new CheckBox("Soft-wrap these files:");
    private final TextField softWrapPatternsField = new TextField();
    private final CheckBox useOriginalIndentCheck = new CheckBox("Use the original line's indent for wrapped fragments");
    private final TextField addIndentField = new TextField();
    private final CheckBox onlyShowIndicatorsCurrentLineCheck = new CheckBox("Only show soft-wrap indicators for the current line");

    // 3. Virtual Space
    private final CheckBox afterEndOfLineCheck = new CheckBox("After the end of line");
    private final CheckBox insideTabsCheck = new CheckBox("Inside tabs");
    private final CheckBox showVirtualSpaceAtBottomCheck = new CheckBox("Show virtual space at the bottom of the file");

    // 4. Scroll Offset
    private final TextField vertOffsetField = new TextField();
    private final TextField vertJumpField = new TextField();
    private final TextField horizOffsetField = new TextField();
    private final TextField horizJumpField = new TextField();

    // 5. Caret Movement
    private final ComboBox<WordBoundaryPolicy> wordCombo = new ComboBox<>();
    private final ComboBox<LineBreakPolicy> lineBreakCombo = new ComboBox<>();

    // 6. Scrolling
    private final CheckBox smoothScrollingCheck = new CheckBox("Enable smooth scrolling");
    private final RadioButton keepCaretRadio = new RadioButton("Keep the caret in place, scroll editor canvas");
    private final RadioButton moveCaretRadio = new RadioButton("Move caret, minimize editor scrolling");

    // 7. Rich-Text Copy
    private final CheckBox copyRichTextCheck = new CheckBox("Copy (Ctrl+C) as rich text");
    private final ComboBox<String> colorSchemeCombo = new ComboBox<>();

    // 8. On Save
    private final CheckBox removeTrailingSpacesCheck = new CheckBox("Remove trailing spaces on:");
    private final ComboBox<TrailingSpacesMode> trailingSpacesCombo = new ComboBox<>();
    private final CheckBox keepTrailingSpacesCaretCheck = new CheckBox("Keep trailing spaces on caret line");
    private final CheckBox removeBlankLinesEofCheck = new CheckBox("Remove trailing blank lines at the end of saved files");
    private final CheckBox ensureLineBreakEofCheck = new CheckBox("Ensure every saved file ends with a line break");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsEditorGeneralPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(6);

        buildUi();
        setupListeners();
        loadFromSettings(EditorGeneralSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private HBox createSectionHeader(String title) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    private TextField createSmallNumericField() {
        TextField tf = new TextField();
        tf.setPrefWidth(54);
        tf.setMaxWidth(54);
        tf.setPrefHeight(26);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-alignment: CENTER_LEFT; -fx-padding: 2 6 2 6;");
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.matches("\\d*")) {
                tf.setText(newV.replaceAll("[^\\d]", ""));
            }
        });
        return tf;
    }

    private void buildUi() {
        // ============================================================
        // 1. Mouse Control
        // ============================================================
        HBox mouseHeader = createSectionHeader("Mouse Control");

        ToggleGroup fontSizeScopeGroup = new ToggleGroup();
        activeEditorRadio.setToggleGroup(fontSizeScopeGroup);
        allEditorsRadio.setToggleGroup(fontSizeScopeGroup);
        styleRadio(activeEditorRadio);
        styleRadio(allEditorsRadio);

        HBox fontSizeRadioBox = new HBox(16, activeEditorRadio, allEditorsRadio);
        fontSizeRadioBox.setAlignment(Pos.CENTER_LEFT);
        fontSizeRadioBox.setPadding(new Insets(2, 0, 4, 24));

        changeFontSizeCheck.selectedProperty().addListener((obs, oldV, isSel) -> {
            fontSizeRadioBox.setDisable(!isSel);
            activeEditorRadio.setDisable(!isSel);
            allEditorsRadio.setDisable(!isSel);
        });
        styleCheckBox(changeFontSizeCheck);

        Label dragDropHint = new Label("To copy, hold Ctrl while dragging");
        dragDropHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        styleCheckBox(dragDropCheck);

        HBox dragDropBox = new HBox(8, dragDropCheck, dragDropHint);
        dragDropBox.setAlignment(Pos.CENTER_LEFT);

        // ============================================================
        // 2. Soft Wraps
        // ============================================================
        HBox softWrapsHeader = createSectionHeader("Soft Wraps");

        softWrapPatternsField.setPrefWidth(320);
        softWrapPatternsField.setPrefHeight(26);
        softWrapPatternsField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 2 8 2 8;");
        styleCheckBox(softWrapFilesCheck);

        HBox softWrapFilesBox = new HBox(8, softWrapFilesCheck, softWrapPatternsField);
        softWrapFilesBox.setAlignment(Pos.CENTER_LEFT);

        Label softWrapHint = new Label("Use * and ? as wildcards and ; to separate patterns");
        softWrapHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        HBox softWrapHintBox = new HBox(softWrapHint);
        softWrapHintBox.setPadding(new Insets(0, 0, 4, 24));

        styleCheckBox(useOriginalIndentCheck);

        Label addIndentLabel = new Label("Add additional indent:");
        addIndentLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        addIndentField.setPrefWidth(44);
        addIndentField.setMaxWidth(44);
        addIndentField.setPrefHeight(26);
        addIndentField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-alignment: CENTER_LEFT; -fx-padding: 2 6 2 6;");
        addIndentField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.matches("\\d*")) {
                addIndentField.setText(newV.replaceAll("[^\\d]", ""));
            }
        });

        Label symbolsLabel = new Label("symbols");
        symbolsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox indentDetailsBox = new HBox(8, addIndentLabel, addIndentField, symbolsLabel);
        indentDetailsBox.setAlignment(Pos.CENTER_LEFT);
        indentDetailsBox.setPadding(new Insets(2, 0, 4, 24));

        styleCheckBox(onlyShowIndicatorsCurrentLineCheck);

        // ============================================================
        // 3. Virtual Space
        // ============================================================
        HBox virtualSpaceHeader = createSectionHeader("Virtual Space");

        Label allowCaretPlacementLabel = new Label("Allow caret placement:");
        allowCaretPlacementLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        styleCheckBox(afterEndOfLineCheck);
        styleCheckBox(insideTabsCheck);

        HBox caretPlacementRow = new HBox(16, allowCaretPlacementLabel, afterEndOfLineCheck, insideTabsCheck);
        caretPlacementRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(showVirtualSpaceAtBottomCheck);

        // ============================================================
        // 4. Scroll Offset
        // ============================================================
        HBox scrollOffsetHeader = createSectionHeader("Scroll Offset");

        copyNumericProperties(vertOffsetField);
        copyNumericProperties(vertJumpField);
        copyNumericProperties(horizOffsetField);
        copyNumericProperties(horizJumpField);

        HBox vertOffsetRow = createOffsetRow("Vertical scroll offset:", vertOffsetField);
        HBox vertJumpRow = createOffsetRow("Vertical scroll jump:", vertJumpField);
        HBox horizOffsetRow = createOffsetRow("Horizontal scroll offset:", horizOffsetField);
        HBox horizJumpRow = createOffsetRow("Horizontal scroll jump:", horizJumpField);

        // ============================================================
        // 5. Caret Movement
        // ============================================================
        HBox caretMovementHeader = createSectionHeader("Caret Movement");

        wordCombo.getItems().setAll(WordBoundaryPolicy.values());
        setupPolicyCombo(wordCombo);
        wordCombo.setPrefWidth(380);

        lineBreakCombo.getItems().setAll(LineBreakPolicy.values());
        setupLineBreakPolicyCombo(lineBreakCombo);
        lineBreakCombo.setPrefWidth(380);

        HBox wordRow = createOffsetRow("When moving by words:", wordCombo);
        HBox lineBreakRow = createOffsetRow("Upon line break:", lineBreakCombo);

        // ============================================================
        // 6. Scrolling
        // ============================================================
        HBox scrollingHeader = createSectionHeader("Scrolling");

        styleCheckBox(smoothScrollingCheck);

        Label caretBehaviorLabel = new Label("Caret behavior:");
        caretBehaviorLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        ToggleGroup caretBehaviorGroup = new ToggleGroup();
        keepCaretRadio.setToggleGroup(caretBehaviorGroup);
        moveCaretRadio.setToggleGroup(caretBehaviorGroup);
        styleRadio(keepCaretRadio);
        styleRadio(moveCaretRadio);

        VBox caretBehaviorBox = new VBox(4, keepCaretRadio, moveCaretRadio);
        caretBehaviorBox.setPadding(new Insets(2, 0, 4, 24));

        // ============================================================
        // 7. Rich-Text Copy
        // ============================================================
        HBox richTextHeader = createSectionHeader("Rich-Text Copy");

        styleCheckBox(copyRichTextCheck);
        Label richCopyHint = new Label("All formatting will be copied, including font, colors and so on");
        richCopyHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

        HBox copyRichTextBox = new HBox(8, copyRichTextCheck, richCopyHint);
        copyRichTextBox.setAlignment(Pos.CENTER_LEFT);

        colorSchemeCombo.getItems().setAll(EditorGeneralSettings.RICH_TEXT_COLOR_SCHEMES);
        styleComboBox(colorSchemeCombo);
        colorSchemeCombo.setPrefWidth(180);

        Label colorSchemeLabel = new Label("Color scheme for copied fragment:");
        colorSchemeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        colorSchemeLabel.setPrefWidth(210);

        HBox colorSchemeRow = new HBox(8, colorSchemeLabel, colorSchemeCombo);
        colorSchemeRow.setAlignment(Pos.CENTER_LEFT);

        // ============================================================
        // 8. On Save
        // ============================================================
        HBox onSaveHeader = createSectionHeader("On Save");

        styleCheckBox(removeTrailingSpacesCheck);
        trailingSpacesCombo.getItems().setAll(TrailingSpacesMode.values());
        trailingSpacesCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TrailingSpacesMode item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getLabel());
                setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            }
        });
        trailingSpacesCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(TrailingSpacesMode item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getLabel());
                setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            }
        });
        styleComboBox(trailingSpacesCombo);
        trailingSpacesCombo.setPrefWidth(140);

        HBox trailingSpacesRow = new HBox(8, removeTrailingSpacesCheck, trailingSpacesCombo);
        trailingSpacesRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(keepTrailingSpacesCaretCheck);
        HBox keepTrailingCaretBox = new HBox(keepTrailingSpacesCaretCheck);
        keepTrailingCaretBox.setPadding(new Insets(2, 0, 4, 24));

        removeTrailingSpacesCheck.selectedProperty().addListener((obs, oldV, isSel) -> {
            trailingSpacesCombo.setDisable(!isSel);
            keepTrailingSpacesCaretCheck.setDisable(!isSel);
        });

        styleCheckBox(removeBlankLinesEofCheck);
        styleCheckBox(ensureLineBreakEofCheck);

        // Add all components in order
        getChildren().addAll(
                mouseHeader,
                changeFontSizeCheck,
                fontSizeRadioBox,
                dragDropBox,

                softWrapsHeader,
                softWrapFilesBox,
                softWrapHintBox,
                useOriginalIndentCheck,
                indentDetailsBox,
                onlyShowIndicatorsCurrentLineCheck,

                virtualSpaceHeader,
                caretPlacementRow,
                showVirtualSpaceAtBottomCheck,

                scrollOffsetHeader,
                vertOffsetRow,
                vertJumpRow,
                horizOffsetRow,
                horizJumpRow,

                caretMovementHeader,
                wordRow,
                lineBreakRow,

                scrollingHeader,
                smoothScrollingCheck,
                caretBehaviorLabel,
                caretBehaviorBox,

                richTextHeader,
                copyRichTextBox,
                colorSchemeRow,

                onSaveHeader,
                trailingSpacesRow,
                keepTrailingCaretBox,
                removeBlankLinesEofCheck,
                ensureLineBreakEofCheck
        );
    }

    private void copyNumericProperties(TextField tf) {
        tf.setPrefWidth(54);
        tf.setMaxWidth(54);
        tf.setPrefHeight(26);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-alignment: CENTER_LEFT; -fx-padding: 2 6 2 6;");
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.matches("\\d*")) {
                tf.setText(newV.replaceAll("[^\\d]", ""));
            }
        });
    }

    private HBox createOffsetRow(String labelText, Control control) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        label.setPrefWidth(160);

        HBox row = new HBox(8, label, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private void styleRadio(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private void styleComboBox(ComboBox<?> cb) {
        cb.setPrefHeight(26);
        cb.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
    }

    private void setupPolicyCombo(ComboBox<WordBoundaryPolicy> cb) {
        styleComboBox(cb);
        cb.setCellFactory(lv -> new ListCell<>() {
            private final Label textLabel = new Label();
            private final Region spacer = new Region();
            private final Label badgeLabel = new Label();
            private final HBox box = new HBox(6, textLabel, spacer, badgeLabel);
            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                box.setAlignment(Pos.CENTER_LEFT);
                textLabel.setStyle("-fx-text-fill: inherit; -fx-font-size: 12px;");
                badgeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(WordBoundaryPolicy item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    textLabel.setText(item.getLabel());
                    badgeLabel.setText(item.getBadge() != null ? item.getBadge() : "");
                    setGraphic(box);
                }
            }
        });
        cb.setButtonCell(new ListCell<>() {
            private final Label textLabel = new Label();
            private final Region spacer = new Region();
            private final Label badgeLabel = new Label();
            private final HBox box = new HBox(6, textLabel, spacer, badgeLabel);
            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                box.setAlignment(Pos.CENTER_LEFT);
                textLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                badgeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(WordBoundaryPolicy item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    textLabel.setText(item.getLabel());
                    badgeLabel.setText(item.getBadge() != null ? item.getBadge() : "");
                    setGraphic(box);
                }
            }
        });
    }

    private void setupLineBreakPolicyCombo(ComboBox<LineBreakPolicy> cb) {
        styleComboBox(cb);
        cb.setCellFactory(lv -> new ListCell<>() {
            private final Label textLabel = new Label();
            private final Region spacer = new Region();
            private final Label badgeLabel = new Label();
            private final HBox box = new HBox(6, textLabel, spacer, badgeLabel);
            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                box.setAlignment(Pos.CENTER_LEFT);
                textLabel.setStyle("-fx-text-fill: inherit; -fx-font-size: 12px;");
                badgeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(LineBreakPolicy item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    textLabel.setText(item.getLabel());
                    badgeLabel.setText(item.getBadge() != null ? item.getBadge() : "");
                    setGraphic(box);
                }
            }
        });
        cb.setButtonCell(new ListCell<>() {
            private final Label textLabel = new Label();
            private final Region spacer = new Region();
            private final Label badgeLabel = new Label();
            private final HBox box = new HBox(6, textLabel, spacer, badgeLabel);
            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                box.setAlignment(Pos.CENTER_LEFT);
                textLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                badgeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(LineBreakPolicy item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    textLabel.setText(item.getLabel());
                    badgeLabel.setText(item.getBadge() != null ? item.getBadge() : "");
                    setGraphic(box);
                }
            }
        });
    }

    private void setupListeners() {
        changeFontSizeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        activeEditorRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        allEditorsRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        dragDropCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        softWrapFilesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        softWrapPatternsField.textProperty().addListener((obs, o, n) -> notifyModified());
        useOriginalIndentCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        addIndentField.textProperty().addListener((obs, o, n) -> notifyModified());
        onlyShowIndicatorsCurrentLineCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        afterEndOfLineCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        insideTabsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showVirtualSpaceAtBottomCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        vertOffsetField.textProperty().addListener((obs, o, n) -> notifyModified());
        vertJumpField.textProperty().addListener((obs, o, n) -> notifyModified());
        horizOffsetField.textProperty().addListener((obs, o, n) -> notifyModified());
        horizJumpField.textProperty().addListener((obs, o, n) -> notifyModified());

        wordCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        lineBreakCombo.valueProperty().addListener((obs, o, n) -> notifyModified());

        smoothScrollingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        keepCaretRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        moveCaretRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());

        copyRichTextCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        colorSchemeCombo.valueProperty().addListener((obs, o, n) -> notifyModified());

        removeTrailingSpacesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        trailingSpacesCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        keepTrailingSpacesCaretCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        removeBlankLinesEofCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        ensureLineBreakEofCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    public void loadFromSettings(EditorGeneralSettings s) {
        suppressEvents = true;
        try {
            changeFontSizeCheck.setSelected(s.isMouseControlChangeFontSize());
            if (s.getMouseControlFontSizeScope() == MouseWheelFontSizeScope.ALL_EDITORS) {
                allEditorsRadio.setSelected(true);
            } else {
                activeEditorRadio.setSelected(true);
            }
            dragDropCheck.setSelected(s.isMoveCodeFragmentsDragAndDrop());

            softWrapFilesCheck.setSelected(s.isSoftWrapFilesEnabled());
            softWrapPatternsField.setText(s.getSoftWrapFilePatterns() != null ? s.getSoftWrapFilePatterns() : "");
            useOriginalIndentCheck.setSelected(s.isUseOriginalLineIndentForWraps());
            addIndentField.setText(String.valueOf(s.getAdditionalIndentSymbols()));
            onlyShowIndicatorsCurrentLineCheck.setSelected(s.isOnlyShowSoftWrapIndicatorsCurrentLine());

            afterEndOfLineCheck.setSelected(s.isCaretPlacementAfterEndOfLine());
            insideTabsCheck.setSelected(s.isCaretPlacementInsideTabs());
            showVirtualSpaceAtBottomCheck.setSelected(s.isVirtualSpaceAtBottom());

            vertOffsetField.setText(String.valueOf(s.getVerticalScrollOffset()));
            vertJumpField.setText(String.valueOf(s.getVerticalScrollJump()));
            horizOffsetField.setText(String.valueOf(s.getHorizontalScrollOffset()));
            horizJumpField.setText(String.valueOf(s.getHorizontalScrollJump()));

            wordCombo.setValue(s.getWordBoundaryPolicy());
            lineBreakCombo.setValue(s.getLineBreakPolicy());

            smoothScrollingCheck.setSelected(s.isSmoothScrolling());
            if (s.getCaretBehavior() == CaretBehavior.MOVE_CARET_MINIMIZE_SCROLL) {
                moveCaretRadio.setSelected(true);
            } else {
                keepCaretRadio.setSelected(true);
            }

            copyRichTextCheck.setSelected(s.isCopyAsRichText());
            colorSchemeCombo.setValue(s.getRichTextColorScheme());

            removeTrailingSpacesCheck.setSelected(s.isRemoveTrailingSpacesOnSave());
            trailingSpacesCombo.setValue(s.getTrailingSpacesMode());
            keepTrailingSpacesCaretCheck.setSelected(s.isKeepTrailingSpacesOnCaretLine());
            removeBlankLinesEofCheck.setSelected(s.isRemoveTrailingBlankLinesAtEof());
            ensureLineBreakEofCheck.setSelected(s.isEnsureEndsWithLineBreak());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        EditorGeneralSettings s = EditorGeneralSettings.getInstance();
        if (changeFontSizeCheck.isSelected() != s.isMouseControlChangeFontSize()) return true;
        MouseWheelFontSizeScope scope = allEditorsRadio.isSelected() ? MouseWheelFontSizeScope.ALL_EDITORS : MouseWheelFontSizeScope.ACTIVE_EDITOR;
        if (scope != s.getMouseControlFontSizeScope()) return true;
        if (dragDropCheck.isSelected() != s.isMoveCodeFragmentsDragAndDrop()) return true;

        if (softWrapFilesCheck.isSelected() != s.isSoftWrapFilesEnabled()) return true;
        if (!Objects.equals(softWrapPatternsField.getText().trim(), s.getSoftWrapFilePatterns())) return true;
        if (useOriginalIndentCheck.isSelected() != s.isUseOriginalLineIndentForWraps()) return true;
        if (parseSafeInt(addIndentField.getText(), 0) != s.getAdditionalIndentSymbols()) return true;
        if (onlyShowIndicatorsCurrentLineCheck.isSelected() != s.isOnlyShowSoftWrapIndicatorsCurrentLine()) return true;

        if (afterEndOfLineCheck.isSelected() != s.isCaretPlacementAfterEndOfLine()) return true;
        if (insideTabsCheck.isSelected() != s.isCaretPlacementInsideTabs()) return true;
        if (showVirtualSpaceAtBottomCheck.isSelected() != s.isVirtualSpaceAtBottom()) return true;

        if (parseSafeInt(vertOffsetField.getText(), 1) != s.getVerticalScrollOffset()) return true;
        if (parseSafeInt(vertJumpField.getText(), 0) != s.getVerticalScrollJump()) return true;
        if (parseSafeInt(horizOffsetField.getText(), 3) != s.getHorizontalScrollOffset()) return true;
        if (parseSafeInt(horizJumpField.getText(), 0) != s.getHorizontalScrollJump()) return true;

        if (wordCombo.getValue() != s.getWordBoundaryPolicy()) return true;
        if (lineBreakCombo.getValue() != s.getLineBreakPolicy()) return true;

        if (smoothScrollingCheck.isSelected() != s.isSmoothScrolling()) return true;
        CaretBehavior cb = moveCaretRadio.isSelected() ? CaretBehavior.MOVE_CARET_MINIMIZE_SCROLL : CaretBehavior.KEEP_CARET_SCROLL_CANVAS;
        if (cb != s.getCaretBehavior()) return true;

        if (copyRichTextCheck.isSelected() != s.isCopyAsRichText()) return true;
        if (!Objects.equals(colorSchemeCombo.getValue(), s.getRichTextColorScheme())) return true;

        if (removeTrailingSpacesCheck.isSelected() != s.isRemoveTrailingSpacesOnSave()) return true;
        if (trailingSpacesCombo.getValue() != s.getTrailingSpacesMode()) return true;
        if (keepTrailingSpacesCaretCheck.isSelected() != s.isKeepTrailingSpacesOnCaretLine()) return true;
        if (removeBlankLinesEofCheck.isSelected() != s.isRemoveTrailingBlankLinesAtEof()) return true;
        if (ensureLineBreakEofCheck.isSelected() != s.isEnsureEndsWithLineBreak()) return true;

        return false;
    }

    public void apply() {
        EditorGeneralSettings s = EditorGeneralSettings.getInstance();
        s.setMouseControlChangeFontSize(changeFontSizeCheck.isSelected());
        s.setMouseControlFontSizeScope(allEditorsRadio.isSelected() ? MouseWheelFontSizeScope.ALL_EDITORS : MouseWheelFontSizeScope.ACTIVE_EDITOR);
        s.setMoveCodeFragmentsDragAndDrop(dragDropCheck.isSelected());

        s.setSoftWrapFilesEnabled(softWrapFilesCheck.isSelected());
        s.setSoftWrapFilePatterns(softWrapPatternsField.getText().trim());
        s.setUseOriginalLineIndentForWraps(useOriginalIndentCheck.isSelected());
        s.setAdditionalIndentSymbols(parseSafeInt(addIndentField.getText(), 0));
        s.setOnlyShowSoftWrapIndicatorsCurrentLine(onlyShowIndicatorsCurrentLineCheck.isSelected());

        s.setCaretPlacementAfterEndOfLine(afterEndOfLineCheck.isSelected());
        s.setCaretPlacementInsideTabs(insideTabsCheck.isSelected());
        s.setVirtualSpaceAtBottom(showVirtualSpaceAtBottomCheck.isSelected());

        s.setVerticalScrollOffset(parseSafeInt(vertOffsetField.getText(), 1));
        s.setVerticalScrollJump(parseSafeInt(vertJumpField.getText(), 0));
        s.setHorizontalScrollOffset(parseSafeInt(horizOffsetField.getText(), 3));
        s.setHorizontalScrollJump(parseSafeInt(horizJumpField.getText(), 0));

        if (wordCombo.getValue() != null) s.setWordBoundaryPolicy(wordCombo.getValue());
        if (lineBreakCombo.getValue() != null) s.setLineBreakPolicy(lineBreakCombo.getValue());

        s.setSmoothScrolling(smoothScrollingCheck.isSelected());
        s.setCaretBehavior(moveCaretRadio.isSelected() ? CaretBehavior.MOVE_CARET_MINIMIZE_SCROLL : CaretBehavior.KEEP_CARET_SCROLL_CANVAS);

        s.setCopyAsRichText(copyRichTextCheck.isSelected());
        if (colorSchemeCombo.getValue() != null) s.setRichTextColorScheme(colorSchemeCombo.getValue());

        s.setRemoveTrailingSpacesOnSave(removeTrailingSpacesCheck.isSelected());
        if (trailingSpacesCombo.getValue() != null) s.setTrailingSpacesMode(trailingSpacesCombo.getValue());
        s.setKeepTrailingSpacesOnCaretLine(keepTrailingSpacesCaretCheck.isSelected());
        s.setRemoveTrailingBlankLinesAtEof(removeBlankLinesEofCheck.isSelected());
        s.setEnsureEndsWithLineBreak(ensureLineBreakEofCheck.isSelected());

        s.save();
        s.fireChanged();
    }

    public void reset() {
        loadFromSettings(EditorGeneralSettings.getInstance());
    }

    private int parseSafeInt(String text, int fallback) {
        if (text == null || text.isBlank()) return fallback;
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}