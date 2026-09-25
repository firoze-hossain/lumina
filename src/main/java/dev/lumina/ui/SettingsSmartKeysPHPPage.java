package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > PHP settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysPHPPage extends VBox {

    private final CheckBox selectVarWithoutDollarCheck = new CheckBox("Select variable name without '$' sign on double click");
    private final CheckBox escapeTextOnPasteCheck = new CheckBox("Escape text on paste in string literals");
    private final CheckBox replaceQuotesOnPasteCheck = new CheckBox("Replace unnecessary double quotes on paste");
    private final CheckBox autoInsertPhpTagCheck = new CheckBox("Auto-insert '<?php' tag after typing '<?'");
    private final CheckBox autoInsertArrowCheck = new CheckBox("Auto-insert '->' when typing '-' after object variable");
    private final CheckBox autoInsertSemicolonCheck = new CheckBox("Auto-insert semicolon when it is typed inside a function call");
    private final CheckBox autoInsertClosingTagDocCheck = new CheckBox("Auto-insert closing HTML tag in PHPDoc blocks");
    private final CheckBox smartParamsCompletionCheck = new CheckBox("Enable smart function parameters completion");
    private final CheckBox smartIndentCheck = new CheckBox("Smart indent on typing");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysPHPPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        styleCheckBox(selectVarWithoutDollarCheck);
        styleCheckBox(escapeTextOnPasteCheck);
        styleCheckBox(replaceQuotesOnPasteCheck);
        styleCheckBox(autoInsertPhpTagCheck);
        styleCheckBox(autoInsertArrowCheck);
        styleCheckBox(autoInsertSemicolonCheck);
        styleCheckBox(autoInsertClosingTagDocCheck);
        styleCheckBox(smartParamsCompletionCheck);
        styleCheckBox(smartIndentCheck);

        setupListeners();

        getChildren().addAll(
                selectVarWithoutDollarCheck,
                escapeTextOnPasteCheck,
                replaceQuotesOnPasteCheck,
                autoInsertPhpTagCheck,
                autoInsertArrowCheck,
                autoInsertSemicolonCheck,
                autoInsertClosingTagDocCheck,
                smartParamsCompletionCheck,
                smartIndentCheck
        );

        loadFromSettings(SmartKeysSettings.getInstance());
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void setupListeners() {
        selectVarWithoutDollarCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        escapeTextOnPasteCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        replaceQuotesOnPasteCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoInsertPhpTagCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoInsertArrowCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoInsertSemicolonCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoInsertClosingTagDocCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        smartParamsCompletionCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        smartIndentCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void loadFromSettings(SmartKeysSettings settings) {
        suppressEvents = true;
        try {
            selectVarWithoutDollarCheck.setSelected(settings.isPhpSelectVarWithoutDollarOnDoubleClick());
            escapeTextOnPasteCheck.setSelected(settings.isPhpEscapeTextOnPasteInStringLiterals());
            replaceQuotesOnPasteCheck.setSelected(settings.isPhpReplaceUnnecessaryDoubleQuotesOnPaste());
            autoInsertPhpTagCheck.setSelected(settings.isPhpAutoInsertPhpTagAfterTyping());
            autoInsertArrowCheck.setSelected(settings.isPhpAutoInsertArrowOnTypingMinusAfterObject());
            autoInsertSemicolonCheck.setSelected(settings.isPhpAutoInsertSemicolon());
            autoInsertClosingTagDocCheck.setSelected(settings.isPhpAutoInsertClosingHtmlTagInDoc());
            smartParamsCompletionCheck.setSelected(settings.isPhpEnableSmartFunctionParametersCompletion());
            smartIndentCheck.setSelected(settings.isPhpSmartIndent());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return selectVarWithoutDollarCheck.isSelected() != s.isPhpSelectVarWithoutDollarOnDoubleClick()
                || escapeTextOnPasteCheck.isSelected() != s.isPhpEscapeTextOnPasteInStringLiterals()
                || replaceQuotesOnPasteCheck.isSelected() != s.isPhpReplaceUnnecessaryDoubleQuotesOnPaste()
                || autoInsertPhpTagCheck.isSelected() != s.isPhpAutoInsertPhpTagAfterTyping()
                || autoInsertArrowCheck.isSelected() != s.isPhpAutoInsertArrowOnTypingMinusAfterObject()
                || autoInsertSemicolonCheck.isSelected() != s.isPhpAutoInsertSemicolon()
                || autoInsertClosingTagDocCheck.isSelected() != s.isPhpAutoInsertClosingHtmlTagInDoc()
                || smartParamsCompletionCheck.isSelected() != s.isPhpEnableSmartFunctionParametersCompletion()
                || smartIndentCheck.isSelected() != s.isPhpSmartIndent();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setPhpSelectVarWithoutDollarOnDoubleClick(selectVarWithoutDollarCheck.isSelected());
        s.setPhpEscapeTextOnPasteInStringLiterals(escapeTextOnPasteCheck.isSelected());
        s.setPhpReplaceUnnecessaryDoubleQuotesOnPaste(replaceQuotesOnPasteCheck.isSelected());
        s.setPhpAutoInsertPhpTagAfterTyping(autoInsertPhpTagCheck.isSelected());
        s.setPhpAutoInsertArrowOnTypingMinusAfterObject(autoInsertArrowCheck.isSelected());
        s.setPhpAutoInsertSemicolon(autoInsertSemicolonCheck.isSelected());
        s.setPhpAutoInsertClosingHtmlTagInDoc(autoInsertClosingTagDocCheck.isSelected());
        s.setPhpEnableSmartFunctionParametersCompletion(smartParamsCompletionCheck.isSelected());
        s.setPhpSmartIndent(smartIndentCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    // --- Component Getters ---
    public CheckBox getSelectVarWithoutDollarCheck() { return selectVarWithoutDollarCheck; }
    public CheckBox getEscapeTextOnPasteCheck() { return escapeTextOnPasteCheck; }
    public CheckBox getReplaceQuotesOnPasteCheck() { return replaceQuotesOnPasteCheck; }
    public CheckBox getAutoInsertPhpTagCheck() { return autoInsertPhpTagCheck; }
    public CheckBox getAutoInsertArrowCheck() { return autoInsertArrowCheck; }
    public CheckBox getAutoInsertSemicolonCheck() { return autoInsertSemicolonCheck; }
    public CheckBox getAutoInsertClosingTagDocCheck() { return autoInsertClosingTagDocCheck; }
    public CheckBox getSmartParamsCompletionCheck() { return smartParamsCompletionCheck; }
    public CheckBox getSmartIndentCheck() { return smartIndentCheck; }
}
