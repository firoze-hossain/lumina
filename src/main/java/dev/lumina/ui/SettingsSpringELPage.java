// SettingsSpringELPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsSpringELPage extends VBox {
    public SettingsSpringELPage() {
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

        String[] items = {"Background", "Bean prefix", "Braces and Operators", "Expression bounds",
            "Identifier", "Keyword", "Method call", "Number", "Property key",
            "Qualified type", "Static field", "String text"};
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

        Label settingsLabel = new Label("Settings");
        settingsLabel.getStyleClass().add("settings-section");

        HBox settingsRow = new HBox(16);
        settingsRow.setPadding(new Insets(4, 0, 8, 20));
        settingsRow.setAlignment(Pos.CENTER_LEFT);
        String[] settings = {"Bold", "Italic", "Foreground", "Background", "Error stripe mark", "Effects", "Borderred"};
        for (String s : settings) {
            if (s.equals("Foreground") || s.equals("Background") || s.equals("Error stripe mark")) {
                HBox box = new HBox(4);
                box.setAlignment(Pos.CENTER_LEFT);
                Label color = new Label("■");
                color.setStyle("-fx-text-fill: #1FB0FF; -fx-font-size: 14px;");
                Button btn = new Button("Choose...");
                btn.getStyleClass().add("dialog-secondary");
                btn.setPrefWidth(70);
                box.getChildren().addAll(color, btn);
                settingsRow.getChildren().add(box);
            } else {
                CheckBox cb = new CheckBox(s);
                cb.setSelected(true);
                cb.getStyleClass().add("settings-check");
                settingsRow.getChildren().add(cb);
            }
        }

        HBox inheritRow = new HBox(8);
        inheritRow.setPadding(new Insets(8, 0, 8, 0));
        inheritRow.setAlignment(Pos.CENTER_LEFT);
        Label inheritLabel = new Label("Inherit values from:");
        inheritLabel.getStyleClass().add("settings-label");
        ComboBox<String> inheritCombo = new ComboBox<>();
        inheritCombo.getItems().addAll("Template language", "Language Defaults");
        inheritCombo.getSelectionModel().selectFirst();
        inheritCombo.getStyleClass().add("settings-combo");
        inheritCombo.setPrefWidth(200);
        inheritRow.getChildren().addAll(inheritLabel, inheritCombo);

        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(12));
        previewBox.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label p1 = new Label("#result = 1 + 2");
        p1.setStyle("-fx-text-fill: #D8DBE6;");
        Label p2 = new Label("#{0.0221415E+23D instanceof T(double)}");
        p2.setStyle("-fx-text-fill: #56A8F5;");
        Label p3 = new Label("intArray[index]-- le 0xFF");
        p3.setStyle("-fx-text-fill: #6AAB73;");
        Label p4 = new Label("{1, 2, 3}");
        p4.setStyle("-fx-text-fill: #2AACB8;");
        Label p5 = new Label("{true and false} || variable not null ? 1.0f : 0.0f");
        p5.setStyle("-fx-text-fill: #CF8E6D;");
        Label p6 = new Label("'5.00' matches '\\^?\\\\d+(\\\\.\\\\d{2})?\\$'");
        p6.setStyle("-fx-text-fill: #6AAB73;");
        previewBox.getChildren().addAll(p1, p2, p3, p4, p5, p6);

        getChildren().addAll(schemeRow, itemsBox, settingsLabel, settingsRow, inheritRow, previewBox);
    }
}