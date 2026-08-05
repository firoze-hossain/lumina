// SettingsAngularTemplatePage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsAngularTemplatePage extends VBox {

    public SettingsAngularTemplatePage() {
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
            "Block braces", "Block name", "Braces in interpolation",
            "Braces in plural expression", "Comma in plural expression",
            "Event binding 'event'", "Plural expression",
            "Property binding '[property]'", "Pseudo selector '::ng-deep'",
            "Semantic highlighting", "Signal", "Structural directive '*directive'",
            "Template expression", "Template variable", "Two wav data bindings '{{...}}'"
        };

        for (String item : items) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label colorPreview = new Label("■");
            colorPreview.setStyle("-fx-text-fill: #CF8E6D; -fx-font-size: 16px;");
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
        
        Label previewLine1 = new Label("<li attr=\"value\"");
        previewLine1.setStyle("-fx-text-fill: #6AAB73;");
        Label previewLine2 = new Label("    *ngFor=\"let hero of heroes as test\"");
        previewLine2.setStyle("-fx-text-fill: #CF8E6D;");
        Label previewLine3 = new Label("    [class.selected]=\"hero === selectedHero\"");
        previewLine3.setStyle("-fx-text-fill: #56A8F5;");
        Label previewLine4 = new Label("    (click)=\"onSelect(hero)\">");
        previewLine4.setStyle("-fx-text-fill: #E8B450;");
        Label previewLine5 = new Label("</li>");
        previewLine5.setStyle("-fx-text-fill: #6AAB73;");
        Label previewLine6 = new Label("");
        Label previewLine7 = new Label("@for (hero of heroes; track heroes.name) {");
        previewLine7.setStyle("-fx-text-fill: #CF8E6D;");
        Label previewLine8 = new Label("    {{ hero.name }}");
        previewLine8.setStyle("-fx-text-fill: #6AAB73;");
        Label previewLine9 = new Label("}");
        previewLine9.setStyle("-fx-text-fill: #D8DBE6;");
        
        previewBox.getChildren().addAll(previewLine1, previewLine2, previewLine3, previewLine4, previewLine5, previewLine6, previewLine7, previewLine8, previewLine9);

        getChildren().addAll(schemeRow, itemsBox, previewBox);
    }
}