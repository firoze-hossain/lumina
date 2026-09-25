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
 * Editor > General > Smart Keys > HTML/CSS settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysHTMLCSSPage extends VBox {

    // XML/HTML
    private final CheckBox insertClosingTagCheck = new CheckBox("Insert closing tag on tag completion");
    private final CheckBox insertRequiredAttributesCheck = new CheckBox("Insert required attributes on tag completion");
    private final CheckBox insertRequiredSubtagsCheck = new CheckBox("Insert required subtags on tag completion");
    private final CheckBox startAttributeCheck = new CheckBox("Start attribute on tag completion");
    private final CheckBox addQuotesCheck = new CheckBox("Add quotes for attribute value on typing '=' and attribute completion");
    private final CheckBox autoCloseTagCheck = new CheckBox("Auto-close tag on typing '</'");
    private final CheckBox simultaneousEditingCheck = new CheckBox("Simultaneous '<tag></tag>' editing");

    // CSS
    private final CheckBox selectWholeCSSCheck = new CheckBox("Select whole CSS identifiers on double click");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysHTMLCSSPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        buildUi();
        setupListeners();
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
        styleCheckBox(insertClosingTagCheck);
        styleCheckBox(insertRequiredAttributesCheck);
        styleCheckBox(insertRequiredSubtagsCheck);
        styleCheckBox(startAttributeCheck);
        styleCheckBox(addQuotesCheck);
        styleCheckBox(autoCloseTagCheck);
        styleCheckBox(simultaneousEditingCheck);
        styleCheckBox(selectWholeCSSCheck);

        HBox xmlHtmlHeader = createSectionHeader("XML/HTML");
        VBox xmlHtmlGroup = new VBox(8,
                insertClosingTagCheck,
                insertRequiredAttributesCheck,
                insertRequiredSubtagsCheck,
                startAttributeCheck,
                addQuotesCheck,
                autoCloseTagCheck,
                simultaneousEditingCheck
        );

        HBox cssHeader = createSectionHeader("CSS");
        VBox cssGroup = new VBox(8,
                selectWholeCSSCheck
        );

        getChildren().addAll(
                xmlHtmlHeader,
                xmlHtmlGroup,
                cssHeader,
                cssGroup
        );
    }

    private void setupListeners() {
        insertClosingTagCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertRequiredAttributesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertRequiredSubtagsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        startAttributeCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        addQuotesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoCloseTagCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        simultaneousEditingCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        selectWholeCSSCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
    }

    public void loadFromSettings(SmartKeysSettings settings) {
        suppressEvents = true;
        try {
            insertClosingTagCheck.setSelected(settings.isXmlHtmlInsertClosingTag());
            insertRequiredAttributesCheck.setSelected(settings.isXmlHtmlInsertRequiredAttributes());
            insertRequiredSubtagsCheck.setSelected(settings.isXmlHtmlInsertRequiredSubtags());
            startAttributeCheck.setSelected(settings.isXmlHtmlStartAttribute());
            addQuotesCheck.setSelected(settings.isXmlHtmlAddQuotesForAttributeValue());
            autoCloseTagCheck.setSelected(settings.isXmlHtmlAutoCloseTag());
            simultaneousEditingCheck.setSelected(settings.isXmlHtmlSimultaneousTagEditing());
            selectWholeCSSCheck.setSelected(settings.isCssSelectWholeCssIdentifiersOnDoubleClick());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return insertClosingTagCheck.isSelected() != s.isXmlHtmlInsertClosingTag()
                || insertRequiredAttributesCheck.isSelected() != s.isXmlHtmlInsertRequiredAttributes()
                || insertRequiredSubtagsCheck.isSelected() != s.isXmlHtmlInsertRequiredSubtags()
                || startAttributeCheck.isSelected() != s.isXmlHtmlStartAttribute()
                || addQuotesCheck.isSelected() != s.isXmlHtmlAddQuotesForAttributeValue()
                || autoCloseTagCheck.isSelected() != s.isXmlHtmlAutoCloseTag()
                || simultaneousEditingCheck.isSelected() != s.isXmlHtmlSimultaneousTagEditing()
                || selectWholeCSSCheck.isSelected() != s.isCssSelectWholeCssIdentifiersOnDoubleClick();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setXmlHtmlInsertClosingTag(insertClosingTagCheck.isSelected());
        s.setXmlHtmlInsertRequiredAttributes(insertRequiredAttributesCheck.isSelected());
        s.setXmlHtmlInsertRequiredSubtags(insertRequiredSubtagsCheck.isSelected());
        s.setXmlHtmlStartAttribute(startAttributeCheck.isSelected());
        s.setXmlHtmlAddQuotesForAttributeValue(addQuotesCheck.isSelected());
        s.setXmlHtmlAutoCloseTag(autoCloseTagCheck.isSelected());
        s.setXmlHtmlSimultaneousTagEditing(simultaneousEditingCheck.isSelected());
        s.setCssSelectWholeCssIdentifiersOnDoubleClick(selectWholeCSSCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    // --- Component Getters ---
    public CheckBox getInsertClosingTagCheck() { return insertClosingTagCheck; }
    public CheckBox getInsertRequiredAttributesCheck() { return insertRequiredAttributesCheck; }
    public CheckBox getInsertRequiredSubtagsCheck() { return insertRequiredSubtagsCheck; }
    public CheckBox getStartAttributeCheck() { return startAttributeCheck; }
    public CheckBox getAddQuotesCheck() { return addQuotesCheck; }
    public CheckBox getAutoCloseTagCheck() { return autoCloseTagCheck; }
    public CheckBox getSimultaneousEditingCheck() { return simultaneousEditingCheck; }
    public CheckBox getSelectWholeCSSCheck() { return selectWholeCSSCheck; }
}