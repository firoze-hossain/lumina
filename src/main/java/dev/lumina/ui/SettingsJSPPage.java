// SettingsJSPPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsJSPPage extends VBox {
    public SettingsJSPPage() {
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

        String[] items = {"Action and directive content", "Action and directive name", "Attribute name",
            "Attribute value", "Comment", "Expression Language", "Scripting"};
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
        inheritCombo.getItems().addAll("Markup → Attribute", "Language Defaults");
        inheritCombo.getSelectionModel().selectFirst();
        inheritCombo.getStyleClass().add("settings-combo");
        inheritCombo.setPrefWidth(250);
        inheritRow.getChildren().addAll(inheritLabel, inheritCombo);

        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(12));
        previewBox.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label p1 = new Label("<%-- Sample comment --%>");
        p1.setStyle("-fx-text-fill: #7A7E85; -fx-font-style: italic;");
        Label p2 = new Label("<%@ page import=\"com.intellij.*\" %>");
        p2.setStyle("-fx-text-fill: #CF8E6D;");
        Label p3 = new Label("<jsp:useBean id=\"info\" class=\"com.intellij.Info\" />");
        p3.setStyle("-fx-text-fill: #56A8F5;");
        Label p4 = new Label("<!DOCTYPE html>");
        p4.setStyle("-fx-text-fill: #6AAB73;");
        Label p5 = new Label("<html>");
        p5.setStyle("-fx-text-fill: #CF8E6D;");
        Label p6 = new Label("    <head>");
        p6.setStyle("-fx-text-fill: #CF8E6D;");
        Label p7 = new Label("        <title>Title</title>");
        p7.setStyle("-fx-text-fill: #6AAB73;");
        Label p8 = new Label("    </head>");
        p8.setStyle("-fx-text-fill: #CF8E6D;");
        Label p9 = new Label("    <body>");
        p9.setStyle("-fx-text-fill: #CF8E6D;");
        Label p10 = new Label("        <h1>Hello, World!</h1>");
        p10.setStyle("-fx-text-fill: #6AAB73;");
        Label p11 = new Label("    </body>");
        p11.setStyle("-fx-text-fill: #CF8E6D;");
        Label p12 = new Label("</html>");
        p12.setStyle("-fx-text-fill: #CF8E6D;");
        previewBox.getChildren().addAll(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12);

        getChildren().addAll(schemeRow, itemsBox, inheritRow, previewBox);
    }
}