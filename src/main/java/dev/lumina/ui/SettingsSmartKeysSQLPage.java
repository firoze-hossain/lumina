// SettingsSmartKeysSQLPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * IntelliJ-style Editor > General > Smart Keys > SQL settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsSmartKeysSQLPage extends VBox {

    public SettingsSmartKeysSQLPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        VBox sqlBox = new VBox(4);
        sqlBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox insertStringConcat = new CheckBox("Insert string concatenation on Enter");
        insertStringConcat.setSelected(true);
        insertStringConcat.getStyleClass().add("settings-check");

        CheckBox closeCodeBlocks = new CheckBox("Close code blocks on Enter");
        closeCodeBlocks.setSelected(true);
        closeCodeBlocks.getStyleClass().add("settings-check");

        sqlBox.getChildren().addAll(
            insertStringConcat,
            closeCodeBlocks
        );

        getChildren().addAll(sqlBox);
    }
}