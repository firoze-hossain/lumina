package dev.lumina.ui;

import dev.lumina.presentation.PresentationAssistantManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Presentation Assistant settings page.
 * Strictly matches media_1790046850265.png, media_1790046850445.png,
 * and media_1790046851065.png.
 */
public class SettingsPresentationAssistantPage extends VBox {

    private final PresentationAssistantManager manager = PresentationAssistantManager.getInstance();

    private final CheckBox showActionsCheck = new CheckBox("Show action names and shortcuts in popup");
    private final ComboBox<String> popupSizeCombo = new ComboBox<>();
    private final TextField displayDurationField = new TextField();
    private final ComboBox<String> positionCombo = new ComboBox<>();

    private final ComboBox<String> mainKeymapCombo = new ComboBox<>();
    private final TextField mainLabelField = new TextField();

    private final CheckBox additionalKeymapCheck = new CheckBox("Additional:");
    private final ComboBox<String> additionalKeymapCombo = new ComboBox<>();
    private final TextField additionalLabelField = new TextField();

    public SettingsPresentationAssistantPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 16, 24));
        setSpacing(14);
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Top CheckBox
        showActionsCheck.setSelected(manager.isShowActionNamesAndShortcuts());
        styleCheck(showActionsCheck);
        showActionsCheck.setOnAction(e -> manager.setShowActionNamesAndShortcuts(showActionsCheck.isSelected()));

        // 2. Popup size
        HBox sizeRow = new HBox(12);
        sizeRow.setAlignment(Pos.CENTER_LEFT);
        Label sizeLbl = new Label("Popup size:");
        sizeLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 80px;");

        popupSizeCombo.getItems().setAll(PresentationAssistantManager.AVAILABLE_SIZES);
        popupSizeCombo.setValue(manager.getPopupSize());
        popupSizeCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        popupSizeCombo.setPrefWidth(120);
        popupSizeCombo.setOnAction(e -> {
            if (popupSizeCombo.getValue() != null) {
                manager.setPopupSize(popupSizeCombo.getValue());
            }
        });
        sizeRow.getChildren().addAll(sizeLbl, popupSizeCombo);

        // 3. Display for [4] seconds
        HBox durationRow = new HBox(8);
        durationRow.setAlignment(Pos.CENTER_LEFT);
        Label durationLbl = new Label("Display for:");
        durationLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 80px;");

        displayDurationField.setText(String.valueOf(manager.getDisplayDurationSeconds()));
        displayDurationField.setPrefWidth(55);
        displayDurationField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        displayDurationField.textProperty().addListener((obs, oldV, newV) -> {
            try {
                int val = Integer.parseInt(newV.trim());
                manager.setDisplayDurationSeconds(val);
            } catch (Exception ignored) {}
        });

        Label secondsLbl = new Label("seconds");
        secondsLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        durationRow.getChildren().addAll(durationLbl, displayDurationField, secondsLbl);

        // 4. Position dropdown
        HBox positionRow = new HBox(12);
        positionRow.setAlignment(Pos.CENTER_LEFT);
        Label positionLbl = new Label("Position:");
        positionLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 80px;");

        positionCombo.getItems().setAll(PresentationAssistantManager.AVAILABLE_POSITIONS);
        positionCombo.setValue(manager.getPosition());
        positionCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        positionCombo.setPrefWidth(160);
        positionCombo.setOnAction(e -> {
            if (positionCombo.getValue() != null) {
                manager.setPosition(positionCombo.getValue());
            }
        });
        positionRow.getChildren().addAll(positionLbl, positionCombo);

        // 5. Keymaps Group with Header Line
        VBox keymapsSection = new VBox(10);
        keymapsSection.setPadding(new Insets(10, 0, 0, 0));

        HBox keymapsHeaderRow = new HBox(10);
        keymapsHeaderRow.setAlignment(Pos.CENTER_LEFT);
        Label keymapsTitle = new Label("Keymaps");
        keymapsTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: normal;");

        Separator line = new Separator();
        line.setStyle("-fx-background-color: #393B40; -fx-border-color: transparent;");
        HBox.setHgrow(line, Priority.ALWAYS);
        keymapsHeaderRow.getChildren().addAll(keymapsTitle, line);

        // Row 1: Main Keymap & Label
        HBox mainRow = new HBox(12);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        mainRow.setPadding(new Insets(4, 0, 0, 20));

        Label mainLbl = new Label("Main:");
        mainLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 70px;");

        mainKeymapCombo.getItems().setAll(PresentationAssistantManager.AVAILABLE_KEYMAPS);
        mainKeymapCombo.setValue(manager.getMainKeymap());
        mainKeymapCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        mainKeymapCombo.setPrefWidth(160);
        mainKeymapCombo.setOnAction(e -> {
            if (mainKeymapCombo.getValue() != null) {
                manager.setMainKeymap(mainKeymapCombo.getValue());
            }
        });

        Label mainLabelTag = new Label("Label:");
        mainLabelTag.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 0 0 0 10;");

        mainLabelField.setText(manager.getMainLabel());
        mainLabelField.setPrefWidth(180);
        mainLabelField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        mainLabelField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setMainLabel(newV);
        });

        mainRow.getChildren().addAll(mainLbl, mainKeymapCombo, mainLabelTag, mainLabelField);

        // Row 2: Additional Keymap & Label
        HBox additionalRow = new HBox(12);
        additionalRow.setAlignment(Pos.CENTER_LEFT);
        additionalRow.setPadding(new Insets(2, 0, 0, 20));

        additionalKeymapCheck.setSelected(manager.isAdditionalKeymapEnabled());
        additionalKeymapCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 90px;");
        additionalKeymapCheck.setOnAction(e -> {
            boolean active = additionalKeymapCheck.isSelected();
            manager.setAdditionalKeymapEnabled(active);
            updateAdditionalState(active);
        });

        additionalKeymapCombo.getItems().setAll(PresentationAssistantManager.AVAILABLE_KEYMAPS);
        additionalKeymapCombo.setValue(manager.getAdditionalKeymap());
        additionalKeymapCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        additionalKeymapCombo.setPrefWidth(160);
        additionalKeymapCombo.setOnAction(e -> {
            if (additionalKeymapCombo.getValue() != null) {
                manager.setAdditionalKeymap(additionalKeymapCombo.getValue());
            }
        });

        Label addLabelTag = new Label("Label:");
        addLabelTag.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 0 0 0 10;");

        additionalLabelField.setText(manager.getAdditionalLabel());
        additionalLabelField.setPrefWidth(180);
        additionalLabelField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        additionalLabelField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setAdditionalLabel(newV);
        });

        updateAdditionalState(additionalKeymapCheck.isSelected());

        additionalRow.getChildren().addAll(additionalKeymapCheck, additionalKeymapCombo, addLabelTag, additionalLabelField);

        keymapsSection.getChildren().addAll(keymapsHeaderRow, mainRow, additionalRow);

        getChildren().addAll(showActionsCheck, sizeRow, durationRow, positionRow, keymapsSection);

        manager.addListener(this::syncUIFromManager);
    }

    private void updateAdditionalState(boolean enabled) {
        additionalKeymapCombo.setDisable(!enabled);
        additionalLabelField.setDisable(!enabled);
        if (enabled) {
            additionalKeymapCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
            additionalLabelField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        } else {
            additionalKeymapCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #6F737A; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-opacity: 0.6;");
            additionalLabelField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #6F737A; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px; -fx-opacity: 0.6;");
        }
    }

    private void styleCheck(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void syncUIFromManager() {
        showActionsCheck.setSelected(manager.isShowActionNamesAndShortcuts());
        popupSizeCombo.setValue(manager.getPopupSize());
        displayDurationField.setText(String.valueOf(manager.getDisplayDurationSeconds()));
        positionCombo.setValue(manager.getPosition());
        mainKeymapCombo.setValue(manager.getMainKeymap());
        mainLabelField.setText(manager.getMainLabel());
        additionalKeymapCheck.setSelected(manager.isAdditionalKeymapEnabled());
        additionalKeymapCombo.setValue(manager.getAdditionalKeymap());
        additionalLabelField.setText(manager.getAdditionalLabel());
        updateAdditionalState(manager.isAdditionalKeymapEnabled());
    }
}