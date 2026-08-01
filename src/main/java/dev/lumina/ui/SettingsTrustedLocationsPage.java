// SettingsTrustedLocationsPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Trusted Locations settings page.
 */
public class SettingsTrustedLocationsPage extends VBox {

    public SettingsTrustedLocationsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);

        Label desc = new Label("Projects located under these local directories will be considered as trusted");
        desc.getStyleClass().add("settings-label");
        desc.setWrapText(true);

        ListView<String> locationsList = new ListView<>();
        locationsList.getStyleClass().add("settings-list");
        locationsList.setPrefHeight(200);
        locationsList.getItems().addAll(
            "/home/firoze/projects/others/stratosdb",
            "/home/firoze/projects/others",
            "/home/firoze/Downloads/DBNavigatorPro-v2.8",
            "/home/firoze/projects",
            "/home/firoze/Music",
            "/home/firoze/Downloads/thundercall-full-project/thundercall-full-project",
            "/home/firoze/Downloads/DBNavigatorPro-v3.0"
        );

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);
        Button add = new Button("Add...");
        add.getStyleClass().add("dialog-secondary");
        Button remove = new Button("Remove");
        remove.getStyleClass().add("dialog-secondary");
        buttons.getChildren().addAll(add, remove);

        VBox content = new VBox(10, desc, locationsList, buttons);
        getChildren().addAll(content);
    }
}