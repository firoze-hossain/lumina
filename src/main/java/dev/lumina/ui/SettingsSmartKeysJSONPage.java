// SettingsSmartKeysJSONPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * IntelliJ-style Editor > General > Smart Keys > JSON settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsSmartKeysJSONPage extends VBox {

    public SettingsSmartKeysJSONPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        VBox jsonBox = new VBox(4);
        jsonBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox insertMissingComma = new CheckBox("Insert missing comma on Enter");
        insertMissingComma.setSelected(true);
        insertMissingComma.getStyleClass().add("settings-check");

        CheckBox insertMissingCommaAfter = new CheckBox("Insert missing comma after matching braces and quotes");
        insertMissingCommaAfter.setSelected(true);
        insertMissingCommaAfter.getStyleClass().add("settings-check");

        CheckBox manageCommas = new CheckBox("Automatically manage commas when pasting JSON fragments");
        manageCommas.setSelected(true);
        manageCommas.getStyleClass().add("settings-check");

        CheckBox escapeText = new CheckBox("Escape text on paste in string literals");
        escapeText.setSelected(true);
        escapeText.getStyleClass().add("settings-check");

        CheckBox autoAddQuotes = new CheckBox("Automatically add quotes to property names when typing ':'");
        autoAddQuotes.setSelected(true);
        autoAddQuotes.getStyleClass().add("settings-check");

        CheckBox autoAddWhitespace = new CheckBox("Automatically add whitespace when typing ':' after property names");
        autoAddWhitespace.setSelected(true);
        autoAddWhitespace.getStyleClass().add("settings-check");

        CheckBox autoMoveColon = new CheckBox("Automatically move ':' after the property name if typed inside quotes");
        autoMoveColon.setSelected(true);
        autoMoveColon.getStyleClass().add("settings-check");

        CheckBox autoMoveComma = new CheckBox("Automatically move comma after the property value or array element if inside quotes");
        autoMoveComma.setSelected(true);
        autoMoveComma.getStyleClass().add("settings-check");

        jsonBox.getChildren().addAll(
            insertMissingComma,
            insertMissingCommaAfter,
            manageCommas,
            escapeText,
            autoAddQuotes,
            autoAddWhitespace,
            autoMoveColon,
            autoMoveComma
        );

        getChildren().addAll(jsonBox);
    }
}