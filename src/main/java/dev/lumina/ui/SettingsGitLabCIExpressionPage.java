// SettingsGitLabCIExpressionPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsGitLabCIExpressionPage extends VBox {

    public SettingsGitLabCIExpressionPage() {
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
            "Bad character", "Comparison operator (==, !=, ~, !~)",
            "Identifier", "Logical operator (||)", "Parentheses",
            "Regular expression", "String", "Variable prefix ($)"
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

        // Inherit values
        HBox inheritRow = new HBox(8);
        inheritRow.setPadding(new Insets(8, 0, 8, 0));
        inheritRow.setAlignment(Pos.CENTER_LEFT);
        Label inheritLabel = new Label("Inherit values from:");
        inheritLabel.getStyleClass().add("settings-label");
        ComboBox<String> inheritCombo = new ComboBox<>();
        inheritCombo.getItems().addAll("String → String text", "Language Defaults");
        inheritCombo.getSelectionModel().selectFirst();
        inheritCombo.getStyleClass().add("settings-combo");
        inheritCombo.setPrefWidth(250);
        Label inheritHint = new Label("(Language Defaults)");
        inheritHint.getStyleClass().add("settings-hint");
        inheritRow.getChildren().addAll(inheritLabel, inheritCombo, inheritHint);

        // Preview area
        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(12));
        previewBox.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label previewLine1 = new Label("$VAR0 := \"my-branch\" || (($VAR1 := \"thing\" || $VAR2 := \"thing\") && $VAR3) && ($VAR1 := \"thing\")");
        previewLine1.setStyle("-fx-text-fill: #6AAB73;");
        previewBox.getChildren().add(previewLine1);

        getChildren().addAll(schemeRow, itemsBox, inheritRow, previewBox);
    }
}