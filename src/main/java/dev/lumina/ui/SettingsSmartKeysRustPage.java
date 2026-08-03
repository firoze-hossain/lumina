// SettingsSmartKeysRustPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * IntelliJ-style Editor > General > Smart Keys > Rust settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsSmartKeysRustPage extends VBox {

    public SettingsSmartKeysRustPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        VBox rustBox = new VBox(4);
        rustBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox pairedHash = new CheckBox("Insert paired hash '#' signs for raw strings");
        pairedHash.setSelected(true);
        pairedHash.getStyleClass().add("settings-check");

        rustBox.getChildren().add(pairedHash);

        getChildren().addAll(rustBox);
    }
}