// SettingsPresentationAssistantPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Presentation Assistant settings page.
 */
public class SettingsPresentationAssistantPage extends VBox {

    public SettingsPresentationAssistantPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);

        // Show action names and shortcuts in popup
        CheckBox showActions = new CheckBox("Show action names and shortcuts in popup");
        showActions.setSelected(true);
        showActions.getStyleClass().add("settings-check");

        // Popup size
        HBox sizeRow = new HBox(8);
        sizeRow.setAlignment(Pos.CENTER_LEFT);
        Label sizeLabel = new Label("Popup size:");
        sizeLabel.getStyleClass().add("settings-label");
        ComboBox<String> sizeCombo = new ComboBox<>();
        sizeCombo.getItems().addAll("Small", "Medium", "Large");
        sizeCombo.getSelectionModel().select("Medium");
        sizeCombo.getStyleClass().add("settings-combo");
        sizeRow.getChildren().addAll(sizeLabel, sizeCombo);

        // Display for
        HBox displayRow = new HBox(8);
        displayRow.setAlignment(Pos.CENTER_LEFT);
        Label displayLabel = new Label("Display for:");
        displayLabel.getStyleClass().add("settings-label");
        ComboBox<String> displayCombo = new ComboBox<>();
        displayCombo.getItems().addAll("2 seconds", "3 seconds", "4 seconds", "5 seconds");
        displayCombo.getSelectionModel().select("4 seconds");
        displayCombo.getStyleClass().add("settings-combo");
        displayRow.getChildren().addAll(displayLabel, displayCombo);

        // Position
        HBox positionRow = new HBox(8);
        positionRow.setAlignment(Pos.CENTER_LEFT);
        Label positionLabel = new Label("Position:");
        positionLabel.getStyleClass().add("settings-label");
        ComboBox<String> positionCombo = new ComboBox<>();
        positionCombo.getItems().addAll("Top Center", "Bottom Center", "Top Left", "Bottom Left", "Top Right", "Bottom Right");
        positionCombo.getSelectionModel().select("Bottom Center");
        positionCombo.getStyleClass().add("settings-combo");
        positionRow.getChildren().addAll(positionLabel, positionCombo);

        // Keymaps section
        Label keymapLabel = new Label("Keymaps");
        keymapLabel.getStyleClass().add("settings-section");

        // Main keymap
        HBox mainRow = new HBox(12);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        Label mainLabel = new Label("Main:");
        mainLabel.getStyleClass().add("settings-label");
        ComboBox<String> mainCombo = new ComboBox<>();
        mainCombo.getItems().addAll("Windows", "macOS", "Linux");
        mainCombo.getSelectionModel().select("Windows");
        mainCombo.getStyleClass().add("settings-combo");
        mainCombo.setPrefWidth(120);
        
        Label mainSubLabel = new Label("Label:");
        mainSubLabel.getStyleClass().add("settings-label");
        TextField mainField = new TextField("Win/Linux");
        mainField.getStyleClass().add("text-field");
        mainField.setPrefWidth(120);
        
        mainRow.getChildren().addAll(mainLabel, mainCombo, mainSubLabel, mainField);

        // Additional keymap
        HBox additionalRow = new HBox(12);
        additionalRow.setAlignment(Pos.CENTER_LEFT);
        Label additionalLabel = new Label("Additional:");
        additionalLabel.getStyleClass().add("settings-label");
        ComboBox<String> additionalCombo = new ComboBox<>();
        additionalCombo.getItems().addAll("macOS", "Windows", "Linux");
        additionalCombo.getSelectionModel().select("macOS");
        additionalCombo.getStyleClass().add("settings-combo");
        additionalCombo.setPrefWidth(120);
        
        Label additionalSubLabel = new Label("Label:");
        additionalSubLabel.getStyleClass().add("settings-label");
        TextField additionalField = new TextField("macOS");
        additionalField.getStyleClass().add("text-field");
        additionalField.setPrefWidth(120);
        
        additionalRow.getChildren().addAll(additionalLabel, additionalCombo, additionalSubLabel, additionalField);

        VBox content = new VBox(10,
            showActions,
            sizeRow,
            displayRow,
            positionRow,
            keymapLabel,
            mainRow,
            additionalRow
        );
        
        getChildren().addAll(content);
    }
}