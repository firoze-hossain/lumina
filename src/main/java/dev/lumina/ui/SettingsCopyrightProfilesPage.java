// SettingsCopyrightProfilesPage.java
package dev.lumina.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * IntelliJ-style Editor > Copyright > Copyright Profiles settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsCopyrightProfilesPage extends VBox {

    private final ListView<String> profileList = new ListView<>();
    private final Label detailLabel = new Label("Select a profile to view or edit its details here");

    public SettingsCopyrightProfilesPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

        // ============================================================
        // Header
        // ============================================================
        Label headerLabel = new Label("Copyright Profiles");
        headerLabel.getStyleClass().add("settings-section");

        // ============================================================
        // Main layout
        // ============================================================
        HBox mainLayout = new HBox(16);
        mainLayout.setPadding(new Insets(8, 0, 0, 0));

        // ---- Profile list (left) ----
        VBox listBox = new VBox(6);
        listBox.setPrefWidth(280);
        listBox.setMinWidth(240);

        profileList.getStyleClass().add("settings-list");
        profileList.setPrefHeight(200);
        profileList.setItems(FXCollections.observableArrayList());
        profileList.setPlaceholder(new Label("No copyright profiles added."));

        // Buttons for profiles
        HBox buttons = new HBox(6);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("Add profile (Alt+Insert)");
        addBtn.getStyleClass().add("dialog-secondary");
        addBtn.setOnAction(e -> showAddProfileDialog());

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("dialog-secondary");
        removeBtn.setOnAction(e -> {
            String selected = profileList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                profileList.getItems().remove(selected);
                if (profileList.getItems().isEmpty()) {
                    detailLabel.setText("No copyright profiles added.");
                }
            }
        });

        buttons.getChildren().addAll(addBtn, removeBtn);

        listBox.getChildren().addAll(headerLabel, profileList, buttons);

        // ---- Detail area (right) ----
        VBox detailBox = new VBox(6);
        detailBox.setPrefWidth(350);
        detailBox.setMinWidth(300);
        HBox.setHgrow(detailBox, Priority.ALWAYS);

        detailLabel.getStyleClass().add("settings-hint");
        detailLabel.setWrapText(true);
        detailLabel.setPadding(new Insets(20, 0, 0, 0));

        detailBox.getChildren().add(detailLabel);

        mainLayout.getChildren().addAll(listBox, detailBox);

        getChildren().addAll(mainLayout);
    }

    private void showAddProfileDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Copyright Profile");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label nameLabel = new Label("Profile name:");
        nameLabel.getStyleClass().add("settings-label");

        TextField nameField = new TextField();
        nameField.getStyleClass().add("text-field");
        nameField.setPrefWidth(300);
        nameField.setPromptText("Enter profile name");

        TextArea copyrightText = new TextArea();
        copyrightText.setPromptText("Enter copyright text...");
        copyrightText.setPrefHeight(150);
        copyrightText.setStyle("-fx-background-color: #1F2230; -fx-text-fill: #D8DBE6; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 10 6 10;");

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("dialog-primary");
        okBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                profileList.getItems().add(name);
                profileList.getSelectionModel().select(name);
                detailLabel.setText("Profile: " + name + "\n\nCopyright text:\n" + copyrightText.getText());
                dialog.close();
            }
        });
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        buttons.getChildren().addAll(okBtn, cancelBtn);

        content.getChildren().addAll(nameLabel, nameField, copyrightText, buttons);

        Scene scene = new Scene(content, 400, 320);
        scene.getStylesheets().add(
            getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}