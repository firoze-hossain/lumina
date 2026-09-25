package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > JavaScript settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysJavaScriptPage extends VBox {

    private final CheckBox replaceStringLiteralCheck = new CheckBox("Automatically replace string literal with template string on typing '${'");
    private final CheckBox startTemplateStringCheck = new CheckBox("Start template string interpolation on typing '$'");
    private final CheckBox escapeTextCheck = new CheckBox("Escape text on paste in string literals");
    private final CheckBox closeHTMLTagsCheck = new CheckBox("Close HTML single tags when pasting code into JSX files");
    private final CheckBox convertHTMLAttributesCheck = new CheckBox("Convert HTML attribute names when pasting code into React JSX files");
    private final CheckBox escapeJSDocCheck = new CheckBox("Escape JSDoc leading asterisks on copy and paste");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysJavaScriptPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        styleCheckBox(replaceStringLiteralCheck);
        styleCheckBox(startTemplateStringCheck);
        styleCheckBox(escapeTextCheck);
        styleCheckBox(closeHTMLTagsCheck);
        styleCheckBox(convertHTMLAttributesCheck);
        styleCheckBox(escapeJSDocCheck);

        setupListeners();

        getChildren().addAll(
                replaceStringLiteralCheck,
                startTemplateStringCheck,
                escapeTextCheck,
                closeHTMLTagsCheck,
                convertHTMLAttributesCheck,
                escapeJSDocCheck
        );

        loadFromSettings(SmartKeysSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void setupListeners() {
        replaceStringLiteralCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        startTemplateStringCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        escapeTextCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        closeHTMLTagsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        convertHTMLAttributesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        escapeJSDocCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
    }

    public void loadFromSettings(SmartKeysSettings settings) {
        suppressEvents = true;
        try {
            replaceStringLiteralCheck.setSelected(settings.isJsReplaceStringLiteralOnTemplate());
            startTemplateStringCheck.setSelected(settings.isJsStartTemplateStringInterpolation());
            escapeTextCheck.setSelected(settings.isJsEscapeTextOnPasteInStringLiterals());
            closeHTMLTagsCheck.setSelected(settings.isJsCloseHtmlSingleTagsInJsx());
            convertHTMLAttributesCheck.setSelected(settings.isJsConvertHtmlAttributeNamesInJsx());
            escapeJSDocCheck.setSelected(settings.isJsEscapeJsDocLeadingAsterisks());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return replaceStringLiteralCheck.isSelected() != s.isJsReplaceStringLiteralOnTemplate()
                || startTemplateStringCheck.isSelected() != s.isJsStartTemplateStringInterpolation()
                || escapeTextCheck.isSelected() != s.isJsEscapeTextOnPasteInStringLiterals()
                || closeHTMLTagsCheck.isSelected() != s.isJsCloseHtmlSingleTagsInJsx()
                || convertHTMLAttributesCheck.isSelected() != s.isJsConvertHtmlAttributeNamesInJsx()
                || escapeJSDocCheck.isSelected() != s.isJsEscapeJsDocLeadingAsterisks();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setJsReplaceStringLiteralOnTemplate(replaceStringLiteralCheck.isSelected());
        s.setJsStartTemplateStringInterpolation(startTemplateStringCheck.isSelected());
        s.setJsEscapeTextOnPasteInStringLiterals(escapeTextCheck.isSelected());
        s.setJsCloseHtmlSingleTagsInJsx(closeHTMLTagsCheck.isSelected());
        s.setJsConvertHtmlAttributeNamesInJsx(convertHTMLAttributesCheck.isSelected());
        s.setJsEscapeJsDocLeadingAsterisks(escapeJSDocCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    public CheckBox getReplaceStringLiteralCheck() { return replaceStringLiteralCheck; }
    public CheckBox getStartTemplateStringCheck() { return startTemplateStringCheck; }
    public CheckBox getEscapeTextCheck() { return escapeTextCheck; }
    public CheckBox getCloseHTMLTagsCheck() { return closeHTMLTagsCheck; }
    public CheckBox getConvertHTMLAttributesCheck() { return convertHTMLAttributesCheck; }
    public CheckBox getEscapeJSDocCheck() { return escapeJSDocCheck; }
}