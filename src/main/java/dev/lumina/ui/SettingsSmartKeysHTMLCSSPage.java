// SettingsSmartKeysHTMLCSSPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * IntelliJ-style Editor > General > Smart Keys > HTML/CSS settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsSmartKeysHTMLCSSPage extends VBox {

    public SettingsSmartKeysHTMLCSSPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // XML/HTML section
        // ============================================================
        Label xmlHtmlLabel = new Label("XML/HTML");
        xmlHtmlLabel.getStyleClass().add("settings-section");

        VBox xmlHtmlBox = new VBox(4);
        xmlHtmlBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox insertClosingTag = new CheckBox("Insert closing tag on tag completion");
        insertClosingTag.setSelected(true);
        insertClosingTag.getStyleClass().add("settings-check");

        CheckBox insertRequiredAttributes = new CheckBox("Insert required attributes on tag completion");
        insertRequiredAttributes.setSelected(true);
        insertRequiredAttributes.getStyleClass().add("settings-check");

        CheckBox insertRequiredSubtags = new CheckBox("Insert required subtags on tag completion");
        insertRequiredSubtags.setSelected(true);
        insertRequiredSubtags.getStyleClass().add("settings-check");

        CheckBox startAttribute = new CheckBox("Start attribute on tag completion");
        startAttribute.setSelected(true);
        startAttribute.getStyleClass().add("settings-check");

        CheckBox addQuotes = new CheckBox("Add quotes for attribute value on typing '=' and attribute completion");
        addQuotes.setSelected(true);
        addQuotes.getStyleClass().add("settings-check");

        CheckBox autoCloseTag = new CheckBox("Auto-close tag on typing '</'");
        autoCloseTag.setSelected(true);
        autoCloseTag.getStyleClass().add("settings-check");

        CheckBox simultaneousEditing = new CheckBox("Simultaneous '<tag></tag>' editing");
        simultaneousEditing.setSelected(true);
        simultaneousEditing.getStyleClass().add("settings-check");

        xmlHtmlBox.getChildren().addAll(
            insertClosingTag,
            insertRequiredAttributes,
            insertRequiredSubtags,
            startAttribute,
            addQuotes,
            autoCloseTag,
            simultaneousEditing
        );

        // ============================================================
        // CSS section
        // ============================================================
        Label cssLabel = new Label("CSS");
        cssLabel.getStyleClass().add("settings-section");

        VBox cssBox = new VBox(4);
        cssBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox selectWholeCSS = new CheckBox("Select whole CSS identifiers on double click");
        selectWholeCSS.setSelected(true);
        selectWholeCSS.getStyleClass().add("settings-check");

        cssBox.getChildren().add(selectWholeCSS);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            xmlHtmlLabel,
            xmlHtmlBox,
            cssLabel,
            cssBox
        );
    }
}