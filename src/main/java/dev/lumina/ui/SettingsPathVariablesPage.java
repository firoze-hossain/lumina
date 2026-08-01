// SettingsPathVariablesPage.java
package dev.lumina.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Path Variables settings page.
 */
public class SettingsPathVariablesPage extends VBox {

    public SettingsPathVariablesPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);

        // Table
        TableView<PathVarEntry> table = new TableView<>();
        table.getStyleClass().add("settings-list");
        table.setPrefHeight(150);

        TableColumn<PathVarEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
        nameCol.setPrefWidth(200);

        TableColumn<PathVarEntry, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());
        valueCol.setPrefWidth(400);

        table.getColumns().addAll(nameCol, valueCol);
        table.getItems().add(new PathVarEntry("MAVEN_REPOSITORY", "/home/firoze/.m2/repository"));

        // Add Variable section
        Label addLabel = new Label("Add Variable");
        addLabel.getStyleClass().add("settings-section");

        GridPane addGrid = new GridPane();
        addGrid.setHgap(12);
        addGrid.setVgap(8);
        addGrid.setPadding(new Insets(8, 0, 0, 0));

        Label nameLabel = new Label("Name:");
        nameLabel.getStyleClass().add("settings-label");
        TextField nameField = new TextField();
        nameField.getStyleClass().add("text-field");
        nameField.setPrefWidth(200);

        Label valueLabel = new Label("Value:");
        valueLabel.getStyleClass().add("settings-label");
        TextField valueField = new TextField();
        valueField.getStyleClass().add("text-field");
        valueField.setPrefWidth(300);

        Button addBtn = new Button("OK");
        addBtn.getStyleClass().add("dialog-primary");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-secondary");

        addGrid.add(nameLabel, 0, 0);
        addGrid.add(nameField, 1, 0);
        addGrid.add(valueLabel, 0, 1);
        addGrid.add(valueField, 1, 1);
        
        HBox addButtons = new HBox(8, addBtn, cancelBtn);
        addButtons.setAlignment(Pos.CENTER_LEFT);
        addGrid.add(addButtons, 1, 2);

        // Ignored Variables
        Label ignoredLabel = new Label("Ignored Variables:");
        ignoredLabel.getStyleClass().add("settings-label");
        
        TextField ignoredField = new TextField();
        ignoredField.setPromptText("Use ; to separate ignored variables");
        ignoredField.getStyleClass().add("text-field");
        ignoredField.setPrefWidth(400);

        VBox content = new VBox(10, table, addLabel, addGrid, ignoredLabel, ignoredField);
        getChildren().addAll(content);
    }

    private static class PathVarEntry {
        private final SimpleStringProperty name;
        private final SimpleStringProperty value;

        public PathVarEntry(String name, String value) {
            this.name = new SimpleStringProperty(name);
            this.value = new SimpleStringProperty(value);
        }

        public SimpleStringProperty nameProperty() { return name; }
        public SimpleStringProperty valueProperty() { return value; }
    }
}