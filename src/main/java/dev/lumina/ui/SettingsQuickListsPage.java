// SettingsQuickListsPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Quick Lists settings page.
 */
public class SettingsQuickListsPage extends VBox {

    public SettingsQuickListsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);

        // Display name
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Display name:");
        nameLabel.getStyleClass().add("settings-label");
        TextField nameField = new TextField("Deployment");
        nameField.getStyleClass().add("text-field");
        nameField.setPrefWidth(300);
        nameRow.getChildren().addAll(nameLabel, nameField);

        // Description
        HBox descRow = new HBox(8);
        descRow.setAlignment(Pos.CENTER_LEFT);
        Label descLabel = new Label("Description:");
        descLabel.getStyleClass().add("settings-label");
        TextField descField = new TextField("Deployment actions");
        descField.getStyleClass().add("text-field");
        descField.setPrefWidth(300);
        descRow.getChildren().addAll(descLabel, descField);

        // Actions list
        Label actionsLabel = new Label("Actions:");
        actionsLabel.getStyleClass().add("settings-label");

        ListView<String> actionsList = new ListView<>();
        actionsList.getStyleClass().add("settings-list");
        actionsList.setPrefHeight(180);
        actionsList.getItems().addAll(
            "Upload to Default Server",
            "Upload To...",
            "Download from Default Server",
            "Download From...",
            "Compare Local File with Deployed Version",
            "Open in default browser",
            "Select in Remote Host",
            "Browse Remote Host",
            "Directory",
            "Change Permissions..."
        );

        // Buttons for actions
        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(Pos.CENTER_LEFT);
        Button addAction = new Button("Add...");
        addAction.getStyleClass().add("dialog-secondary");
        Button removeAction = new Button("Remove");
        removeAction.getStyleClass().add("dialog-secondary");
        Button editAction = new Button("Edit...");
        editAction.getStyleClass().add("dialog-secondary");
        Button moveUp = new Button("Move Up");
        moveUp.getStyleClass().add("dialog-secondary");
        Button moveDown = new Button("Move Down");
        moveDown.getStyleClass().add("dialog-secondary");
        actionButtons.getChildren().addAll(addAction, removeAction, editAction, moveUp, moveDown);

        VBox content = new VBox(10, nameRow, descRow, actionsLabel, actionsList, actionButtons);
        getChildren().addAll(content);
    }
}