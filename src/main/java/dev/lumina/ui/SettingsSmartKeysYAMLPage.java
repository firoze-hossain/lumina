// SettingsSmartKeysYAMLPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Smart Keys > YAML settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsSmartKeysYAMLPage extends VBox {

    public SettingsSmartKeysYAMLPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Auto expand key sequences upon paste
        // ============================================================
        CheckBox autoExpand = new CheckBox("Auto expand key sequences upon paste");
        autoExpand.setSelected(true);
        autoExpand.getStyleClass().add("settings-check");

        // ============================================================
        // Assemble
        // ============================================================
        getChildren().addAll(autoExpand);
    }
}