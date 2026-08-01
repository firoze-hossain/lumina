// SettingsFileColorsPage.java
package dev.lumina.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * IntelliJ-style File Colors settings page.
 */
public class SettingsFileColorsPage extends VBox {

    public SettingsFileColorsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);

        // Description
        Label desc = new Label("Files can belong to several scopes. If there are two colors for one scope, the color of the first scope in the list is used.");
        desc.getStyleClass().add("settings-hint");
        desc.setWrapText(true);

        Button manageScopes = new Button("Manage scopes...");
        manageScopes.getStyleClass().add("dialog-secondary");

        // Table
        TableView<FileColorEntry> table = new TableView<>();
        table.getStyleClass().add("settings-list");
        table.setPrefHeight(200);

        TableColumn<FileColorEntry, String> toggleCol = new TableColumn<>("");
        toggleCol.setCellValueFactory(cell -> cell.getValue().toggleProperty());
        toggleCol.setPrefWidth(50);

        TableColumn<FileColorEntry, String> colorCol = new TableColumn<>("Color");
        colorCol.setCellValueFactory(cell -> cell.getValue().colorProperty());
        colorCol.setPrefWidth(120);

        TableColumn<FileColorEntry, String> scopeCol = new TableColumn<>("Scope");
        scopeCol.setCellValueFactory(cell -> cell.getValue().scopeProperty());
        scopeCol.setPrefWidth(300);

        table.getColumns().addAll(toggleCol, colorCol, scopeCol);

        // Sample data
        table.getItems().addAll(
            new FileColorEntry("✓", "Yellow", "Production"),
            new FileColorEntry("", "Green", "Test")
        );

        VBox tableBox = new VBox(8, desc, manageScopes, table);
        
        getChildren().addAll(tableBox);
    }

    private static class FileColorEntry {
        private final SimpleStringProperty toggle;
        private final SimpleStringProperty color;
        private final SimpleStringProperty scope;

        public FileColorEntry(String toggle, String color, String scope) {
            this.toggle = new SimpleStringProperty(toggle);
            this.color = new SimpleStringProperty(color);
            this.scope = new SimpleStringProperty(scope);
        }

        public SimpleStringProperty toggleProperty() { return toggle; }
        public SimpleStringProperty colorProperty() { return color; }
        public SimpleStringProperty scopeProperty() { return scope; }
    }
}