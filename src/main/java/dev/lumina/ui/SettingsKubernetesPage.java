// SettingsKubernetesPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsKubernetesPage extends VBox {
    public SettingsKubernetesPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

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

        VBox itemsBox = new VBox(4);
        itemsBox.setPadding(new Insets(12, 0, 12, 0));

        String[] items = {"Boolean", "Enumeration", "Group, version, kind", "Number", "Semantic highlighting"};
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

        HBox inheritRow = new HBox(8);
        inheritRow.setPadding(new Insets(8, 0, 8, 0));
        inheritRow.setAlignment(Pos.CENTER_LEFT);
        Label inheritLabel = new Label("Inherit values from:");
        inheritLabel.getStyleClass().add("settings-label");
        ComboBox<String> inheritCombo = new ComboBox<>();
        inheritCombo.getItems().addAll("Number", "Language Defaults");
        inheritCombo.getSelectionModel().selectFirst();
        inheritCombo.getStyleClass().add("settings-combo");
        inheritCombo.setPrefWidth(200);
        inheritRow.getChildren().addAll(inheritLabel, inheritCombo);

        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(12));
        previewBox.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label p1 = new Label("apiVersion: v1");
        p1.setStyle("-fx-text-fill: #CF8E6D;");
        Label p2 = new Label("kind: Pod");
        p2.setStyle("-fx-text-fill: #56A8F5;");
        Label p3 = new Label("metadata:");
        p3.setStyle("-fx-text-fill: #D8DBE6;");
        Label p4 = new Label("  name: demo");
        p4.setStyle("-fx-text-fill: #6AAB73;");
        Label p5 = new Label("spec:");
        p5.setStyle("-fx-text-fill: #D8DBE6;");
        Label p6 = new Label("  containers:");
        p6.setStyle("-fx-text-fill: #D8DBE6;");
        Label p7 = new Label("  - name: demo");
        p7.setStyle("-fx-text-fill: #6AAB73;");
        previewBox.getChildren().addAll(p1, p2, p3, p4, p5, p6, p7);

        getChildren().addAll(schemeRow, itemsBox, inheritRow, previewBox);
    }
}