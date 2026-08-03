// SettingsSmartKeysJavaScriptPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * IntelliJ-style Editor > General > Smart Keys > JavaScript settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsSmartKeysJavaScriptPage extends VBox {

    public SettingsSmartKeysJavaScriptPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        VBox jsBox = new VBox(4);
        jsBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox replaceStringLiteral = new CheckBox("Automatically replace string literal with template string on typing '${'");
        replaceStringLiteral.setSelected(true);
        replaceStringLiteral.getStyleClass().add("settings-check");

        CheckBox startTemplateString = new CheckBox("Start template string interpolation on typing '$'");
        startTemplateString.setSelected(true);
        startTemplateString.getStyleClass().add("settings-check");

        CheckBox escapeText = new CheckBox("Escape text on paste in string literals");
        escapeText.setSelected(true);
        escapeText.getStyleClass().add("settings-check");

        CheckBox closeHTMLTags = new CheckBox("Close HTML single tags when pasting code into JSX files");
        closeHTMLTags.setSelected(true);
        closeHTMLTags.getStyleClass().add("settings-check");

        CheckBox convertHTMLAttributes = new CheckBox("Convert HTML attribute names when pasting code into React JSX files");
        convertHTMLAttributes.setSelected(true);
        convertHTMLAttributes.getStyleClass().add("settings-check");

        CheckBox escapeJSDoc = new CheckBox("Escape JSDoc leading asterisks on copy and paste");
        escapeJSDoc.setSelected(true);
        escapeJSDoc.getStyleClass().add("settings-check");

        jsBox.getChildren().addAll(
            replaceStringLiteral,
            startTemplateString,
            escapeText,
            closeHTMLTags,
            convertHTMLAttributes,
            escapeJSDoc
        );

        getChildren().addAll(jsBox);
    }
}