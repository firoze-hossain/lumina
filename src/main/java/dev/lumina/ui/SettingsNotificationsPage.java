package dev.lumina.ui;

import dev.lumina.notification.NotificationDisplayType;
import dev.lumina.notification.NotificationGroup;
import dev.lumina.notification.NotificationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;

/**
 * IntelliJ IDEA-identical Notifications settings page under:
 * Appearance & Behavior > Notifications
 *
 * Implements:
 * - Breadcrumb header: Appearance & Behavior › Notifications with ← → arrows
 * - Global checkboxes: Display balloon notifications, Enable system notifications
 * - Two-column group configuration: ListView of notification groups on left,
 *   Popup type dropdown ("No popup", "Balloon", "Sticky balloon"),
 *   Show in tool window, Play sound, Read aloud on right
 * - Bottom "Don't ask again notifications:" container with "Remove ⌘⌫" button
 */
public class SettingsNotificationsPage extends VBox {

    private boolean updatingControls = false;

    public SettingsNotificationsPage() {
        NotificationService service = NotificationService.getInstance();

        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        // Global Checkboxes
        CheckBox displayBalloon = new CheckBox("Display balloon notifications");
        displayBalloon.setSelected(service.isDisplayBalloonNotifications());
        displayBalloon.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        displayBalloon.selectedProperty().addListener((obs, old, val) -> service.setDisplayBalloonNotifications(val));

        CheckBox enableSystem = new CheckBox("Enable system notifications");
        enableSystem.setSelected(service.isEnableSystemNotifications());
        enableSystem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        enableSystem.selectedProperty().addListener((obs, old, val) -> service.setEnableSystemNotifications(val));

        // 3. Two-Column Notification Group Configuration
        HBox middleBox = new HBox(20);
        middleBox.setAlignment(Pos.TOP_LEFT);

        // Left column: ListView of notification groups
        ListView<NotificationGroup> groupList = new ListView<>(service.getObservableGroups());
        groupList.setPrefWidth(320);
        groupList.setMinWidth(300);
        groupList.setPrefHeight(230);
        groupList.setStyle("""
            -fx-background-color: #1E1F22;
            -fx-border-color: #393B40;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
        """);

        groupList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NotificationGroup item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getTitle());
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                    } else {
                        setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                    }
                }
            }
        });

        // Right column: Selected group settings
        VBox groupConfigBox = new VBox(10);
        groupConfigBox.setAlignment(Pos.TOP_LEFT);

        HBox popupTypeRow = new HBox(8);
        popupTypeRow.setAlignment(Pos.CENTER_LEFT);

        Label popupTypeLabel = new Label("Popup type:");
        popupTypeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        ComboBox<NotificationDisplayType> popupTypeCombo = new ComboBox<>();
        popupTypeCombo.getItems().addAll(
                NotificationDisplayType.NONE,
                NotificationDisplayType.BALLOON,
                NotificationDisplayType.STICKY_BALLOON
        );
        popupTypeCombo.setPrefWidth(125);
        popupTypeCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        popupTypeRow.getChildren().addAll(popupTypeLabel, popupTypeCombo);

        CheckBox showInToolWindow = new CheckBox("Show in tool window");
        showInToolWindow.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        CheckBox playSound = new CheckBox("Play sound");
        playSound.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        CheckBox readAloud = new CheckBox("Read aloud");
        readAloud.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        groupConfigBox.getChildren().addAll(popupTypeRow, showInToolWindow, playSound, readAloud);

        // Selection listener to update controls
        groupList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) return;
            updatingControls = true;
            try {
                popupTypeCombo.getSelectionModel().select(selected.getDisplayType());
                showInToolWindow.setSelected(selected.isShowInToolWindow());
                playSound.setSelected(selected.isPlaySound());
                readAloud.setSelected(selected.isReadAloud());
            } finally {
                updatingControls = false;
            }
        });

        // Control change listeners to persist
        popupTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (updatingControls || val == null) return;
            NotificationGroup selected = groupList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setDisplayType(val);
                service.saveSettings();
            }
        });

        showInToolWindow.selectedProperty().addListener((obs, old, val) -> {
            if (updatingControls) return;
            NotificationGroup selected = groupList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setShowInToolWindow(val);
                service.saveSettings();
            }
        });

        playSound.selectedProperty().addListener((obs, old, val) -> {
            if (updatingControls) return;
            NotificationGroup selected = groupList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setPlaySound(val);
                service.saveSettings();
            }
        });

        readAloud.selectedProperty().addListener((obs, old, val) -> {
            if (updatingControls) return;
            NotificationGroup selected = groupList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setReadAloud(val);
                service.saveSettings();
            }
        });

        // Select first group by default
        if (!service.getObservableGroups().isEmpty()) {
            groupList.getSelectionModel().selectFirst();
        }

        middleBox.getChildren().addAll(groupList, groupConfigBox);

        // 4. Bottom Section: Don't ask again notifications:
        Label dontAskLabel = new Label("Don't ask again notifications:");
        dontAskLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        VBox dontAskBox = new VBox();
        dontAskBox.setStyle("""
            -fx-background-color: #1E1F22;
            -fx-border-color: #393B40;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
        """);
        dontAskBox.setPrefHeight(250);
        VBox.setVgrow(dontAskBox, Priority.ALWAYS);

        // Toolbar with Remove button
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: transparent transparent #393B40 transparent; -fx-border-width: 1;");

        Button removeBtn = new Button("Remove ⌘⌫");
        removeBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
        removeBtn.setDisable(true);

        toolbar.getChildren().add(removeBtn);

        StackPane contentStack = new StackPane();
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        ListView<String> dontAskList = new ListView<>(service.getDontAskAgainList());
        dontAskList.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        dontAskList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                    } else {
                        setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                    }
                }
            }
        });

        Label dashLabel = new Label("—");
        dashLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-padding: 8 12 8 12;");
        StackPane.setAlignment(dashLabel, Pos.TOP_LEFT);

        Runnable syncEmptyState = () -> {
            if (service.getDontAskAgainList().isEmpty()) {
                contentStack.getChildren().setAll(dashLabel);
            } else {
                contentStack.getChildren().setAll(dontAskList);
            }
        };
        syncEmptyState.run();
        service.getDontAskAgainList().addListener((javafx.collections.ListChangeListener<String>) c -> syncEmptyState.run());

        dontAskList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) ->
                removeBtn.setDisable(selected == null));

        Runnable doRemove = () -> {
            String selected = dontAskList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                service.removeDontAskAgain(selected);
            }
        };
        removeBtn.setOnAction(e -> doRemove.run());

        dontAskList.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.BACK_SPACE || e.getCode() == KeyCode.DELETE) {
                doRemove.run();
                e.consume();
            }
        });

        dontAskBox.getChildren().addAll(toolbar, contentStack);

        getChildren().addAll(displayBalloon, enableSystem, middleBox, dontAskLabel, dontAskBox);
    }
}