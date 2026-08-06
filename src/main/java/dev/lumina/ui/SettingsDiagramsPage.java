// SettingsDiagramsPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsDiagramsPage extends VBox {

    public SettingsDiagramsPage() {
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

        // Bend section
        Label bendLabel = new Label("Bends");
        bendLabel.getStyleClass().add("settings-section");

        VBox bendBox = new VBox(4);
        bendBox.setPadding(new Insets(4, 0, 12, 0));
        String[] bendItems = {"Bend", "Bend selection"};
        for (String item : bendItems) {
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
            bendBox.getChildren().add(row);
        }

        // Coarse grid section
        Label coarseLabel = new Label("Coarse grid");
        coarseLabel.getStyleClass().add("settings-section");

        VBox coarseBox = new VBox(4);
        coarseBox.setPadding(new Insets(4, 0, 12, 0));
        String[] coarseItems = {
            "Edges", "Annotation edge", "Bad edge", "Default edge",
            "Edge selection", "Generalization edge", "Inner class edge", "Realization edge"
        };
        for (String item : coarseItems) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label colorPreview = new Label("■");
            colorPreview.setStyle("-fx-text-fill: #6AAB73; -fx-font-size: 16px;");
            Button chooseBtn = new Button("Choose...");
            chooseBtn.getStyleClass().add("dialog-secondary");
            chooseBtn.setPrefWidth(80);
            Label itemLabel = new Label(item);
            itemLabel.getStyleClass().add("settings-label");
            row.getChildren().addAll(colorPreview, chooseBtn, itemLabel);
            coarseBox.getChildren().add(row);
        }

        // Fine grid section
        Label fineLabel = new Label("Fine grid");
        fineLabel.getStyleClass().add("settings-section");

        VBox fineBox = new VBox(4);
        fineBox.setPadding(new Insets(4, 0, 12, 0));
        String[] fineItems = {"Hot spots", "Nodes", "Notes", "Port", "Selection Box", "Snapping lines"};
        for (String item : fineItems) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label colorPreview = new Label("■");
            colorPreview.setStyle("-fx-text-fill: #E8B450; -fx-font-size: 16px;");
            Button chooseBtn = new Button("Choose...");
            chooseBtn.getStyleClass().add("dialog-secondary");
            chooseBtn.setPrefWidth(80);
            Label itemLabel = new Label(item);
            itemLabel.getStyleClass().add("settings-label");
            row.getChildren().addAll(colorPreview, chooseBtn, itemLabel);
            fineBox.getChildren().add(row);
        }

        getChildren().addAll(schemeRow, bendLabel, bendBox, coarseLabel, coarseBox, fineLabel, fineBox);
    }
}