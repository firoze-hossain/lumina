// SettingsRequiredPluginsPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Required Plugins settings page.
 */
public class SettingsRequiredPluginsPage extends VBox {

    public SettingsRequiredPluginsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);

        // Plugin
        HBox pluginRow = new HBox(8);
        pluginRow.setAlignment(Pos.CENTER_LEFT);
        Label pluginLabel = new Label("Plugin:");
        pluginLabel.getStyleClass().add("settings-label");
        ComboBox<String> pluginCombo = new ComboBox<>();
        pluginCombo.getItems().addAll("Angular", "Spring Boot", "Kotlin", "Scala", "Python");
        pluginCombo.getSelectionModel().selectFirst();
        pluginCombo.getStyleClass().add("settings-combo");
        pluginCombo.setPrefWidth(200);
        pluginRow.getChildren().addAll(pluginLabel, pluginCombo);

        // Minimum version
        HBox minRow = new HBox(8);
        minRow.setAlignment(Pos.CENTER_LEFT);
        Label minLabel = new Label("Minimum version:");
        minLabel.getStyleClass().add("settings-label");
        ComboBox<String> minCombo = new ComboBox<>();
        minCombo.getItems().addAll("<any>", "1.0", "2.0", "3.0");
        minCombo.getSelectionModel().selectFirst();
        minCombo.getStyleClass().add("settings-combo");
        minCombo.setPrefWidth(150);
        minRow.getChildren().addAll(minLabel, minCombo);

        // Maximum version
        HBox maxRow = new HBox(8);
        maxRow.setAlignment(Pos.CENTER_LEFT);
        Label maxLabel = new Label("Maximum version:");
        maxLabel.getStyleClass().add("settings-label");
        ComboBox<String> maxCombo = new ComboBox<>();
        maxCombo.getItems().addAll("<any>", "1.0", "2.0", "3.0");
        maxCombo.getSelectionModel().selectFirst();
        maxCombo.getStyleClass().add("settings-combo");
        maxCombo.setPrefWidth(150);
        maxRow.getChildren().addAll(maxLabel, maxCombo);

        VBox content = new VBox(12, pluginRow, minRow, maxRow);
        getChildren().addAll(content);
    }
}