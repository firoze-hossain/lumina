package dev.lumina.ui;

import dev.lumina.settings.EditorAppearanceSettings;
import dev.lumina.settings.EditorAppearanceSettings.LineNumbersMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

/**
 * Pixel-perfect IntelliJ IDEA-style Editor > General > Appearance settings page.
 * Completely dynamic, backed by EditorAppearanceSettings, with live real-time synchronization,
 * dropdown scopes, spinners, and dirty tracking.
 */
public class SettingsAppearancePage extends VBox {

    // 1. Caret & Occurrences
    private final CheckBox caretBlinkingCheck = new CheckBox("Caret blinking (ms):");
    private final TextField caretBlinkingField = new TextField("500");
    private final CheckBox blockCaretCheck = new CheckBox("Use block caret");
    private final CheckBox fullLineCaretCheck = new CheckBox("Use full line height caret");
    private final CheckBox highlightOccurrencesCheck = new CheckBox("Highlight occurrences of selected text");
    private final CheckBox showHardWrapCheck = new CheckBox("Show hard wrap and visual guides (configured in Code Style options)");

    // 2. Line numbers
    private final CheckBox showLineNumbersCheck = new CheckBox("Show line numbers:");
    private final ComboBox<LineNumbersMode> lineNumbersCombo = new ComboBox<>();

    // 3. Method separators & Whitespaces
    private final CheckBox showMethodSeparatorsCheck = new CheckBox("Show method separators");
    private final CheckBox showWhitespacesCheck = new CheckBox("Show whitespaces");
    private final CheckBox whitespaceLeadingCheck = new CheckBox("Leading");
    private final CheckBox whitespaceInnerCheck = new CheckBox("Inner");
    private final CheckBox whitespaceTrailingCheck = new CheckBox("Trailing");
    private final CheckBox whitespaceSelectionCheck = new CheckBox("Selection");

    // 4. Guides, bulbs, docs & hints
    private final CheckBox showIndentGuidesCheck = new CheckBox("Show indent guides");
    private final CheckBox showIntentionBulbCheck = new CheckBox("Show intention bulb");
    private final CheckBox showIntentionPreviewCheck = new CheckBox("Show preview for intention actions when available");
    private final CheckBox renderDocCommentsCheck = new CheckBox("Render documentation comments");
    private final Hyperlink readerModeLink = new Hyperlink("Also in Reader mode");
    private final CheckBox showCodeLensCheck = new CheckBox("Show code lens on scrollbar hover");
    private final CheckBox useEditorFontInlayCheck = new CheckBox("Use editor font for inlay hints");

    // 5. HTML/XML tag tree & Colors & Languages
    private final CheckBox enableTagTreeCheck = new CheckBox("Enable HTML/XML tag tree highlighting");
    private final Spinner<Integer> tagLevelsSpinner = new Spinner<>(1, 10, 6);
    private final Spinner<Double> tagOpacitySpinner = new Spinner<>(0.0, 1.0, 0.1, 0.05);

    private final CheckBox showCssColorPreviewCheck = new CheckBox("Show CSS color preview as background");
    private final CheckBox highlightRDocCheck = new CheckBox("Highlight RDoc syntax in comments");
    private final CheckBox showPhpSeparatorsCheck = new CheckBox("Show PHP class and namespace separators");
    private final CheckBox enablePhpBackgroundCheck = new CheckBox("Always enable PHP code background highlighting");
    private final CheckBox enableBladeHighlightingCheck = new CheckBox("Always enable Blade template highlighting");

    private Runnable onModifiedListener;
    private Runnable onNavigateReaderMode;
    private boolean suppressEvents = false;

    public SettingsAppearancePage() {
        this(null);
    }

    public SettingsAppearancePage(Runnable onNavigateReaderMode) {
        this.onNavigateReaderMode = onNavigateReaderMode;

        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(7);

        buildUi();
        setupListeners();
        loadFromSettings(EditorAppearanceSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnNavigateReaderMode(Runnable onNavigateReaderMode) {
        this.onNavigateReaderMode = onNavigateReaderMode;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void buildUi() {
        // Caret blinking row
        styleCheckBox(caretBlinkingCheck);
        caretBlinkingField.setPrefWidth(55);
        caretBlinkingField.setMaxWidth(65);
        caretBlinkingField.setPrefHeight(24);
        caretBlinkingField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 2 6 2 6;");

        HBox caretRow = new HBox(8, caretBlinkingCheck, caretBlinkingField);
        caretRow.setAlignment(Pos.CENTER_LEFT);

        caretBlinkingCheck.selectedProperty().addListener((obs, o, n) -> {
            caretBlinkingField.setDisable(!n);
        });

        // Caret style
        styleCheckBox(blockCaretCheck);
        styleCheckBox(fullLineCaretCheck);
        styleCheckBox(highlightOccurrencesCheck);
        styleCheckBox(showHardWrapCheck);

        // Line numbers row
        styleCheckBox(showLineNumbersCheck);
        lineNumbersCombo.getItems().setAll(LineNumbersMode.values());
        lineNumbersCombo.setConverter(new StringConverter<>() {
            @Override public String toString(LineNumbersMode object) { return object != null ? object.getLabel() : ""; }
            @Override public LineNumbersMode fromString(String string) { return LineNumbersMode.fromLabel(string); }
        });
        lineNumbersCombo.setPrefHeight(26);
        lineNumbersCombo.setPrefWidth(110);
        lineNumbersCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox lineNumbersRow = new HBox(8, showLineNumbersCheck, lineNumbersCombo);
        lineNumbersRow.setAlignment(Pos.CENTER_LEFT);

        showLineNumbersCheck.selectedProperty().addListener((obs, o, n) -> {
            lineNumbersCombo.setDisable(!n);
        });

        // Method separators & Whitespaces
        styleCheckBox(showMethodSeparatorsCheck);
        styleCheckBox(showWhitespacesCheck);

        styleCheckBox(whitespaceLeadingCheck);
        styleCheckBox(whitespaceInnerCheck);
        styleCheckBox(whitespaceTrailingCheck);
        styleCheckBox(whitespaceSelectionCheck);

        HBox whitespaceOptionsBox = new HBox(16,
                whitespaceLeadingCheck,
                whitespaceInnerCheck,
                whitespaceTrailingCheck,
                whitespaceSelectionCheck
        );
        whitespaceOptionsBox.setPadding(new Insets(2, 0, 4, 18));
        whitespaceOptionsBox.setAlignment(Pos.CENTER_LEFT);

        showWhitespacesCheck.selectedProperty().addListener((obs, o, n) -> {
            whitespaceLeadingCheck.setDisable(!n);
            whitespaceInnerCheck.setDisable(!n);
            whitespaceTrailingCheck.setDisable(!n);
            whitespaceSelectionCheck.setDisable(!n);
        });

        // Indent guides, bulbs, previews
        styleCheckBox(showIndentGuidesCheck);
        styleCheckBox(showIntentionBulbCheck);
        styleCheckBox(showIntentionPreviewCheck);

        // Render documentation comments with inline link
        styleCheckBox(renderDocCommentsCheck);
        readerModeLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
        readerModeLink.setOnMouseEntered(e -> readerModeLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: true;"));
        readerModeLink.setOnMouseExited(e -> readerModeLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;"));
        readerModeLink.setOnAction(e -> {
            if (onNavigateReaderMode != null) onNavigateReaderMode.run();
        });

        Label alsoInLabel = new Label("Also in");
        alsoInLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        HBox docCommentsRow = new HBox(6, renderDocCommentsCheck, alsoInLabel, readerModeLink);
        docCommentsRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(showCodeLensCheck);
        styleCheckBox(useEditorFontInlayCheck);

        // Tag tree highlighting
        styleCheckBox(enableTagTreeCheck);

        tagLevelsSpinner.setPrefWidth(65);
        tagLevelsSpinner.setPrefHeight(26);
        tagLevelsSpinner.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        tagOpacitySpinner.setPrefWidth(75);
        tagOpacitySpinner.setPrefHeight(26);
        tagOpacitySpinner.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Label levelsLabel = new Label("Levels to highlight:");
        levelsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox levelsRow = new HBox(8, levelsLabel, tagLevelsSpinner);
        levelsRow.setAlignment(Pos.CENTER_LEFT);

        Label opacityLabel = new Label("Opacity:");
        opacityLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox opacityRow = new HBox(8, opacityLabel, tagOpacitySpinner);
        opacityRow.setAlignment(Pos.CENTER_LEFT);

        VBox tagSubBox = new VBox(5, levelsRow, opacityRow);
        tagSubBox.setPadding(new Insets(2, 0, 4, 18));

        enableTagTreeCheck.selectedProperty().addListener((obs, o, n) -> {
            tagLevelsSpinner.setDisable(!n);
            tagOpacitySpinner.setDisable(!n);
        });

        // Other language settings
        styleCheckBox(showCssColorPreviewCheck);
        styleCheckBox(highlightRDocCheck);
        styleCheckBox(showPhpSeparatorsCheck);
        styleCheckBox(enablePhpBackgroundCheck);
        styleCheckBox(enableBladeHighlightingCheck);

        getChildren().addAll(
                caretRow,
                blockCaretCheck,
                fullLineCaretCheck,
                highlightOccurrencesCheck,
                showHardWrapCheck,
                lineNumbersRow,
                showMethodSeparatorsCheck,
                showWhitespacesCheck,
                whitespaceOptionsBox,
                showIndentGuidesCheck,
                showIntentionBulbCheck,
                showIntentionPreviewCheck,
                docCommentsRow,
                showCodeLensCheck,
                useEditorFontInlayCheck,
                enableTagTreeCheck,
                tagSubBox,
                showCssColorPreviewCheck,
                highlightRDocCheck,
                showPhpSeparatorsCheck,
                enablePhpBackgroundCheck,
                enableBladeHighlightingCheck
        );
    }

    private void setupListeners() {
        caretBlinkingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        caretBlinkingField.textProperty().addListener((obs, o, n) -> notifyModified());
        blockCaretCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        fullLineCaretCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        highlightOccurrencesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showHardWrapCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showLineNumbersCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        lineNumbersCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        showMethodSeparatorsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showWhitespacesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        whitespaceLeadingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        whitespaceInnerCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        whitespaceTrailingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        whitespaceSelectionCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showIndentGuidesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showIntentionBulbCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showIntentionPreviewCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        renderDocCommentsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showCodeLensCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        useEditorFontInlayCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        enableTagTreeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        tagLevelsSpinner.valueProperty().addListener((obs, o, n) -> notifyModified());
        tagOpacitySpinner.valueProperty().addListener((obs, o, n) -> notifyModified());
        showCssColorPreviewCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        highlightRDocCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showPhpSeparatorsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        enablePhpBackgroundCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        enableBladeHighlightingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    public void loadFromSettings(EditorAppearanceSettings s) {
        suppressEvents = true;
        try {
            caretBlinkingCheck.setSelected(s.isCaretBlinking());
            caretBlinkingField.setText(String.valueOf(s.getCaretBlinkingMs()));
            caretBlinkingField.setDisable(!s.isCaretBlinking());

            blockCaretCheck.setSelected(s.isUseBlockCaret());
            fullLineCaretCheck.setSelected(s.isUseFullLineHeightCaret());
            highlightOccurrencesCheck.setSelected(s.isHighlightOccurrences());
            showHardWrapCheck.setSelected(s.isShowHardWrapAndVisualGuides());

            showLineNumbersCheck.setSelected(s.isShowLineNumbers());
            lineNumbersCombo.setValue(s.getLineNumbersMode());
            lineNumbersCombo.setDisable(!s.isShowLineNumbers());

            showMethodSeparatorsCheck.setSelected(s.isShowMethodSeparators());
            showWhitespacesCheck.setSelected(s.isShowWhitespaces());
            whitespaceLeadingCheck.setSelected(s.isWhitespaceLeading());
            whitespaceInnerCheck.setSelected(s.isWhitespaceInner());
            whitespaceTrailingCheck.setSelected(s.isWhitespaceTrailing());
            whitespaceSelectionCheck.setSelected(s.isWhitespaceSelection());
            boolean wsEnabled = s.isShowWhitespaces();
            whitespaceLeadingCheck.setDisable(!wsEnabled);
            whitespaceInnerCheck.setDisable(!wsEnabled);
            whitespaceTrailingCheck.setDisable(!wsEnabled);
            whitespaceSelectionCheck.setDisable(!wsEnabled);

            showIndentGuidesCheck.setSelected(s.isShowIndentGuides());
            showIntentionBulbCheck.setSelected(s.isShowIntentionBulb());
            showIntentionPreviewCheck.setSelected(s.isShowIntentionPreview());
            renderDocCommentsCheck.setSelected(s.isRenderDocComments());
            showCodeLensCheck.setSelected(s.isShowCodeLensOnScrollbarHover());
            useEditorFontInlayCheck.setSelected(s.isUseEditorFontForInlayHints());

            enableTagTreeCheck.setSelected(s.isEnableTagTreeHighlighting());
            tagLevelsSpinner.getValueFactory().setValue(s.getTagTreeHighlightLevels());
            tagOpacitySpinner.getValueFactory().setValue(s.getTagTreeHighlightOpacity());
            boolean tagEnabled = s.isEnableTagTreeHighlighting();
            tagLevelsSpinner.setDisable(!tagEnabled);
            tagOpacitySpinner.setDisable(!tagEnabled);

            showCssColorPreviewCheck.setSelected(s.isShowCssColorPreviewAsBackground());
            highlightRDocCheck.setSelected(s.isHighlightRDocSyntaxInComments());
            showPhpSeparatorsCheck.setSelected(s.isShowPhpClassAndNamespaceSeparators());
            enablePhpBackgroundCheck.setSelected(s.isAlwaysEnablePhpCodeBackgroundHighlighting());
            enableBladeHighlightingCheck.setSelected(s.isAlwaysEnableBladeTemplateHighlighting());
        } finally {
            suppressEvents = false;
        }
    }

    public void saveToSettings(EditorAppearanceSettings s) {
        s.setCaretBlinking(caretBlinkingCheck.isSelected());
        try {
            s.setCaretBlinkingMs(Integer.parseInt(caretBlinkingField.getText().trim()));
        } catch (Exception ignored) {}

        s.setUseBlockCaret(blockCaretCheck.isSelected());
        s.setUseFullLineHeightCaret(fullLineCaretCheck.isSelected());
        s.setHighlightOccurrences(highlightOccurrencesCheck.isSelected());
        s.setShowHardWrapAndVisualGuides(showHardWrapCheck.isSelected());

        s.setShowLineNumbers(showLineNumbersCheck.isSelected());
        if (lineNumbersCombo.getValue() != null) {
            s.setLineNumbersMode(lineNumbersCombo.getValue());
        }

        s.setShowMethodSeparators(showMethodSeparatorsCheck.isSelected());
        s.setShowWhitespaces(showWhitespacesCheck.isSelected());
        s.setWhitespaceLeading(whitespaceLeadingCheck.isSelected());
        s.setWhitespaceInner(whitespaceInnerCheck.isSelected());
        s.setWhitespaceTrailing(whitespaceTrailingCheck.isSelected());
        s.setWhitespaceSelection(whitespaceSelectionCheck.isSelected());

        s.setShowIndentGuides(showIndentGuidesCheck.isSelected());
        s.setShowIntentionBulb(showIntentionBulbCheck.isSelected());
        s.setShowIntentionPreview(showIntentionPreviewCheck.isSelected());
        s.setRenderDocComments(renderDocCommentsCheck.isSelected());
        s.setShowCodeLensOnScrollbarHover(showCodeLensCheck.isSelected());
        s.setUseEditorFontForInlayHints(useEditorFontInlayCheck.isSelected());

        s.setEnableTagTreeHighlighting(enableTagTreeCheck.isSelected());
        if (tagLevelsSpinner.getValue() != null) s.setTagTreeHighlightLevels(tagLevelsSpinner.getValue());
        if (tagOpacitySpinner.getValue() != null) s.setTagTreeHighlightOpacity(tagOpacitySpinner.getValue());

        s.setShowCssColorPreviewAsBackground(showCssColorPreviewCheck.isSelected());
        s.setHighlightRDocSyntaxInComments(highlightRDocCheck.isSelected());
        s.setShowPhpClassAndNamespaceSeparators(showPhpSeparatorsCheck.isSelected());
        s.setAlwaysEnablePhpCodeBackgroundHighlighting(enablePhpBackgroundCheck.isSelected());
        s.setAlwaysEnableBladeTemplateHighlighting(enableBladeHighlightingCheck.isSelected());
    }

    public boolean isModified() {
        EditorAppearanceSettings current = new EditorAppearanceSettings();
        saveToSettings(current);
        return current.isModified(EditorAppearanceSettings.getInstance());
    }

    public void apply() {
        saveToSettings(EditorAppearanceSettings.getInstance());
        EditorAppearanceSettings.getInstance().save();
    }

    public void reset() {
        loadFromSettings(EditorAppearanceSettings.getInstance());
    }
}