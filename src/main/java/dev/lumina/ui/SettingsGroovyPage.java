// SettingsGroovyPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsGroovyPage extends VBox {

    public SettingsGroovyPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

        // Scheme row
        HBox schemeRow = new HBox(12);
        schemeRow.setAlignment(Pos.CENTER_LEFT);
        Label schemeLabel = new Label("Scheme:");
        schemeLabel.getStyleClass().add("settings-label");
        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll("Islands Dark Theme default", "Darcula", "IntelliJ Light");
        schemeCombo.getSelectionModel().selectFirst();
        schemeCombo.getStyleClass().add("settings-combo");
        schemeCombo.setPrefWidth(260);
        Hyperlink themeLink = new Hyperlink("Change IDE Theme...");
        themeLink.getStyleClass().add("settings-link");
        schemeRow.getChildren().addAll(schemeLabel, schemeCombo, themeLink);

        // Color items
        VBox itemsBox = new VBox(4);
        itemsBox.setPadding(new Insets(12, 0, 12, 0));

        String[] items = {
            "Annotation attribute name", "Annotation name", "Bad character",
            "Braces and Operators", "Classes and Interfaces", "Comments",
            "Fields", "Keyword", "Label", "List/Map to object conversion",
            "Map key/Named argument", "Methods", "Number", "References"
        };
        for (String item : items) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label colorPreview = new Label("■");
            colorPreview.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 16px;");
            Button chooseBtn = new Button("Choose...");
            chooseBtn.getStyleClass().add("dialog-secondary");
            chooseBtn.setPrefWidth(80);
            Label itemLabel = new Label(item);
            itemLabel.getStyleClass().add("settings-label");
            row.getChildren().addAll(colorPreview, chooseBtn, itemLabel);
            itemsBox.getChildren().add(row);
        }

        // Inherit values section
        HBox inheritRow = new HBox(8);
        inheritRow.setPadding(new Insets(8, 0, 8, 0));
        inheritRow.setAlignment(Pos.CENTER_LEFT);
        Label inheritLabel = new Label("Inherit values from:");
        inheritLabel.getStyleClass().add("settings-label");
        ComboBox<String> inheritCombo = new ComboBox<>();
        inheritCombo.getItems().addAll("Annotations → Annotation attribute name (Java)", "Language Defaults");
        inheritCombo.getSelectionModel().selectFirst();
        inheritCombo.getStyleClass().add("settings-combo");
        inheritCombo.setPrefWidth(300);
        inheritRow.getChildren().addAll(inheritLabel, inheritCombo);

        // Preview area
        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(12));
        previewBox.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label previewLine1 = new Label("###");
        previewLine1.setStyle("-fx-text-fill: #7A7E85; -fx-font-style: italic;");
        Label previewLine2 = new Label("/* This is GroovyDoc comment */");
        previewLine2.setStyle("-fx-text-fill: #7A7E85; -fx-font-style: italic;");
        Label previewLine3 = new Label("/* @see java.lang.String#equals */");
        previewLine3.setStyle("-fx-text-fill: #7A7E85; -fx-font-style: italic;");
        Label previewLine4 = new Label("/* Annotation(parameter = 'value' */");
        previewLine4.setStyle("-fx-text-fill: #56A8F5;");
        previewBox.getChildren().addAll(previewLine1, previewLine2, previewLine3, previewLine4);

        getChildren().addAll(schemeRow, itemsBox, inheritRow, previewBox);
    }
}