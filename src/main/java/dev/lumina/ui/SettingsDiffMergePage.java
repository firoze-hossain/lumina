// SettingsDiffMergePage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsDiffMergePage extends VBox {

    public SettingsDiffMergePage() {
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
        VBox itemsBox = new VBox(6);
        itemsBox.setPadding(new Insets(12, 0, 12, 0));

        Label changedLabel = new Label("Changed lines");
        changedLabel.getStyleClass().add("settings-section");

        String[] items = {"Changed", "Conflict", "Deleted", "Inserted", "Folded unchanged fragments", "Wave"};
        for (String item : items) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label colorPreview = new Label("■");
            String color = item.equals("Changed") ? "#D8A657" :
                          item.equals("Conflict") ? "#E5534B" :
                          item.equals("Deleted") ? "#E5534B" :
                          item.equals("Inserted") ? "#6AAB73" :
                          item.equals("Folded unchanged fragments") ? "#565D75" : "#D8A657";
            colorPreview.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px;");
            Button chooseBtn = new Button("Choose...");
            chooseBtn.getStyleClass().add("dialog-secondary");
            chooseBtn.setPrefWidth(80);
            Label itemLabel = new Label(item);
            itemLabel.getStyleClass().add("settings-label");
            row.getChildren().addAll(colorPreview, chooseBtn, itemLabel);
            itemsBox.getChildren().add(row);
        }

        // Preview area
        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(12));
        previewBox.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");
        
        Label previewLine1 = new Label("class MyClass {");
        previewLine1.setStyle("-fx-text-fill: #D8DBE6;");
        Label previewLine2 = new Label("    int value;");
        previewLine2.setStyle("-fx-text-fill: #D8DBE6;");
        Label previewLine3 = new Label("    void foo() {");
        previewLine3.setStyle("-fx-text-fill: #6AAB73;");
        Label previewLine4 = new Label("        // Left changes");
        previewLine4.setStyle("-fx-text-fill: #E5534B;");
        Label previewLine5 = new Label("    }");
        previewLine5.setStyle("-fx-text-fill: #D8DBE6;");
        
        previewBox.getChildren().addAll(previewLine1, previewLine2, previewLine3, previewLine4, previewLine5);

        getChildren().addAll(schemeRow, changedLabel, itemsBox, previewBox);
    }
}