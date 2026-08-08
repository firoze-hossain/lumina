// SettingsCopyrightPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > Copyright settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsCopyrightPage extends VBox {

    public SettingsCopyrightPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

        // ============================================================
        // Default project copyright
        // ============================================================
        HBox copyrightRow = new HBox(12);
        copyrightRow.setAlignment(Pos.CENTER_LEFT);

        Label copyrightLabel = new Label("Default project copyright:");
        copyrightLabel.getStyleClass().add("settings-label");

        ComboBox<String> copyrightCombo = new ComboBox<>();
        copyrightCombo.getItems().addAll("No copyright", "Copyright (c) 2026", "Apache License 2.0", "MIT License");
        copyrightCombo.getSelectionModel().selectFirst();
        copyrightCombo.getStyleClass().add("settings-combo");
        copyrightCombo.setPrefWidth(200);

        copyrightRow.getChildren().addAll(copyrightLabel, copyrightCombo);

        // ============================================================
        // Scope section
        // ============================================================
        Label scopeLabel = new Label("Scope");
        scopeLabel.getStyleClass().add("settings-section");
        scopeLabel.setPadding(new Insets(16, 0, 4, 0));

        Label nothingLabel = new Label("Nothing to show");
        nothingLabel.getStyleClass().add("settings-hint");
        nothingLabel.setPadding(new Insets(8, 0, 8, 0));

        // ============================================================
        // Assemble
        // ============================================================
        getChildren().addAll(copyrightRow, scopeLabel, nothingLabel);
    }
}