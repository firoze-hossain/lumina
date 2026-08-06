// SettingsVelocityPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsVelocityPage extends VBox {
    public SettingsVelocityPage() {
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

        String[] items = {"Bad character", "Braces", "Brackets", "Built-in directive", "Comma",
            "Comment", "Dot", "Escape character", "Keyword", "Number literal",
            "Operation sign", "Parentheses", "Reference and macro call",
            "Scripting background", "Semicolon"};
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

        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(12));
        previewBox.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label p1 = new Label("#* comment *");
        p1.setStyle("-fx-text-fill: #7A7E85; -fx-font-style: italic;");
        Label p2 = new Label("comment *#");
        p2.setStyle("-fx-text-fill: #7A7E85; -fx-font-style: italic;");
        Label p3 = new Label("$reference.method('string').property");
        p3.setStyle("-fx-text-fill: #6AAB73;");
        Label p4 = new Label("## another comment");
        p4.setStyle("-fx-text-fill: #7A7E85; -fx-font-style: italic;");
        Label p5 = new Label("#foreach($loopvar in [-4..7])");
        p5.setStyle("-fx-text-fill: #CF8E6D;");
        Label p6 = new Label("#set($var = {\"double-quoted $loopvar.toString()\"}, 2:false #fff})");
        p6.setStyle("-fx-text-fill: #6AAB73;");
        Label p7 = new Label("#end");
        p7.setStyle("-fx-text-fill: #CF8E6D;");
        previewBox.getChildren().addAll(p1, p2, p3, p4, p5, p6, p7);

        getChildren().addAll(schemeRow, itemsBox, previewBox);
    }
}