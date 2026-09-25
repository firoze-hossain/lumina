package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > PHP settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysPHPPage extends VBox {

    private final CheckBox smartParamsCompletionCheck = new CheckBox("Enable smart function parameters completion");
    private final CheckBox selectVarWithoutDollarCheck = new CheckBox("Select variable name without '$' sign on double click");
    private final CheckBox removePhpTagsOnPasteCheck = new CheckBox("Remove PHP open/close tags while pasting in PHP context");
    private final CheckBox escapeSymbolsOnPasteCheck = new CheckBox("Escape symbols on paste in string literals");
    private final CheckBox replaceQuotesOnPasteCheck = new CheckBox("Replace unnecessary double quotes on paste");
    private final CheckBox autoInsertPhpTagCheck = new CheckBox("Auto-insert '<?php' tag after typing '<?'");
    private final CheckBox autoInsertSemicolonCheck = new CheckBox("Auto-insert semicolon when it is typed inside a function call");
    private final CheckBox showAdditionalOptionsMethodUsagesCheck = new CheckBox("Show additional options when searching for method usages");
    private final CheckBox autoInsertClosingTagDocCheck = new CheckBox("Auto-insert closing HTML tag in PHPDoc blocks");

    // Internal compatibility references
    private final CheckBox autoInsertArrowCheck = new CheckBox("Auto-insert '->' when typing '-' after object variable");
    private final CheckBox smartIndentCheck = new CheckBox("Smart indent on typing");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysPHPPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        buildUi();
        setupListeners();
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 4, 0));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(lbl, line);
        return header;
    }

    private void buildUi() {
        styleCheckBox(smartParamsCompletionCheck);
        styleCheckBox(selectVarWithoutDollarCheck);
        styleCheckBox(removePhpTagsOnPasteCheck);
        styleCheckBox(escapeSymbolsOnPasteCheck);
        styleCheckBox(replaceQuotesOnPasteCheck);
        styleCheckBox(autoInsertPhpTagCheck);
        styleCheckBox(autoInsertSemicolonCheck);
        styleCheckBox(showAdditionalOptionsMethodUsagesCheck);
        styleCheckBox(autoInsertClosingTagDocCheck);

        HBox phpHeader = createSectionHeader("PHP");
        VBox phpGroup = new VBox(8,
                smartParamsCompletionCheck,
                selectVarWithoutDollarCheck,
                removePhpTagsOnPasteCheck,
                escapeSymbolsOnPasteCheck,
                replaceQuotesOnPasteCheck,
                autoInsertPhpTagCheck,
                autoInsertSemicolonCheck,
                showAdditionalOptionsMethodUsagesCheck,
                autoInsertClosingTagDocCheck
        );

        getChildren().addAll(phpHeader, phpGroup);
    }

    private void setupListeners() {
        smartParamsCompletionCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        selectVarWithoutDollarCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        removePhpTagsOnPasteCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        escapeSymbolsOnPasteCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        replaceQuotesOnPasteCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoInsertPhpTagCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoInsertSemicolonCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        showAdditionalOptionsMethodUsagesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoInsertClosingTagDocCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoInsertArrowCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
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
            smartParamsCompletionCheck.setSelected(settings.isPhpEnableSmartFunctionParametersCompletion());
            selectVarWithoutDollarCheck.setSelected(settings.isPhpSelectVarWithoutDollarOnDoubleClick());
            removePhpTagsOnPasteCheck.setSelected(settings.isPhpRemovePhpOpenCloseTagsWhilePasting());
            escapeSymbolsOnPasteCheck.setSelected(settings.isPhpEscapeSymbolsOnPasteInStringLiterals());
            replaceQuotesOnPasteCheck.setSelected(settings.isPhpReplaceUnnecessaryDoubleQuotesOnPaste());
            autoInsertPhpTagCheck.setSelected(settings.isPhpAutoInsertPhpTagAfterTyping());
            autoInsertSemicolonCheck.setSelected(settings.isPhpAutoInsertSemicolon());
            showAdditionalOptionsMethodUsagesCheck.setSelected(settings.isPhpShowAdditionalOptionsSearchingMethodUsages());
            autoInsertClosingTagDocCheck.setSelected(settings.isPhpAutoInsertClosingHtmlTagInDoc());
            autoInsertArrowCheck.setSelected(settings.isPhpAutoInsertArrowOnTypingMinusAfterObject());
            smartIndentCheck.setSelected(settings.isPhpSmartIndent());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return smartParamsCompletionCheck.isSelected() != s.isPhpEnableSmartFunctionParametersCompletion()
                || selectVarWithoutDollarCheck.isSelected() != s.isPhpSelectVarWithoutDollarOnDoubleClick()
                || removePhpTagsOnPasteCheck.isSelected() != s.isPhpRemovePhpOpenCloseTagsWhilePasting()
                || escapeSymbolsOnPasteCheck.isSelected() != s.isPhpEscapeSymbolsOnPasteInStringLiterals()
                || replaceQuotesOnPasteCheck.isSelected() != s.isPhpReplaceUnnecessaryDoubleQuotesOnPaste()
                || autoInsertPhpTagCheck.isSelected() != s.isPhpAutoInsertPhpTagAfterTyping()
                || autoInsertSemicolonCheck.isSelected() != s.isPhpAutoInsertSemicolon()
                || showAdditionalOptionsMethodUsagesCheck.isSelected() != s.isPhpShowAdditionalOptionsSearchingMethodUsages()
                || autoInsertClosingTagDocCheck.isSelected() != s.isPhpAutoInsertClosingHtmlTagInDoc()
                || autoInsertArrowCheck.isSelected() != s.isPhpAutoInsertArrowOnTypingMinusAfterObject()
                || smartIndentCheck.isSelected() != s.isPhpSmartIndent();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setPhpEnableSmartFunctionParametersCompletion(smartParamsCompletionCheck.isSelected());
        s.setPhpSelectVarWithoutDollarOnDoubleClick(selectVarWithoutDollarCheck.isSelected());
        s.setPhpRemovePhpOpenCloseTagsWhilePasting(removePhpTagsOnPasteCheck.isSelected());
        s.setPhpEscapeSymbolsOnPasteInStringLiterals(escapeSymbolsOnPasteCheck.isSelected());
        s.setPhpReplaceUnnecessaryDoubleQuotesOnPaste(replaceQuotesOnPasteCheck.isSelected());
        s.setPhpAutoInsertPhpTagAfterTyping(autoInsertPhpTagCheck.isSelected());
        s.setPhpAutoInsertSemicolon(autoInsertSemicolonCheck.isSelected());
        s.setPhpShowAdditionalOptionsSearchingMethodUsages(showAdditionalOptionsMethodUsagesCheck.isSelected());
        s.setPhpAutoInsertClosingHtmlTagInDoc(autoInsertClosingTagDocCheck.isSelected());
        s.setPhpAutoInsertArrowOnTypingMinusAfterObject(autoInsertArrowCheck.isSelected());
        s.setPhpSmartIndent(smartIndentCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    // --- Component Getters ---
    public CheckBox getSmartParamsCompletionCheck() { return smartParamsCompletionCheck; }
    public CheckBox getSelectVarWithoutDollarCheck() { return selectVarWithoutDollarCheck; }
    public CheckBox getRemovePhpTagsOnPasteCheck() { return removePhpTagsOnPasteCheck; }
    public CheckBox getEscapeSymbolsOnPasteCheck() { return escapeSymbolsOnPasteCheck; }
    public CheckBox getEscapeTextOnPasteCheck() { return escapeSymbolsOnPasteCheck; }
    public CheckBox getReplaceQuotesOnPasteCheck() { return replaceQuotesOnPasteCheck; }
    public CheckBox getAutoInsertPhpTagCheck() { return autoInsertPhpTagCheck; }
    public CheckBox getAutoInsertSemicolonCheck() { return autoInsertSemicolonCheck; }
    public CheckBox getShowAdditionalOptionsMethodUsagesCheck() { return showAdditionalOptionsMethodUsagesCheck; }
    public CheckBox getAutoInsertClosingTagDocCheck() { return autoInsertClosingTagDocCheck; }
    public CheckBox getAutoInsertArrowCheck() { return autoInsertArrowCheck; }
    public CheckBox getSmartIndentCheck() { return smartIndentCheck; }
}
