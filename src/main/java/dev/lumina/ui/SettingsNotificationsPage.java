// SettingsNotificationsPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Notifications settings page.
 */
public class SettingsNotificationsPage extends VBox {

    public SettingsNotificationsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);

        // Display balloon notifications
        CheckBox displayBalloon = new CheckBox("Display balloon notifications");
        displayBalloon.setSelected(true);
        displayBalloon.getStyleClass().add("settings-check");

        // Enable system notifications
        CheckBox enableSystem = new CheckBox("Enable system notifications");
        enableSystem.setSelected(true);
        enableSystem.getStyleClass().add("settings-check");

        // Popup type
        HBox popupRow = new HBox(8);
        popupRow.setAlignment(Pos.CENTER_LEFT);
        Label popupLabel = new Label("Popup type:");
        popupLabel.getStyleClass().add("settings-label");
        ComboBox<String> popupType = new ComboBox<>();
        popupType.getItems().addAll("Sticky balloon", "Non-sticky balloon", "Tool window");
        popupType.getSelectionModel().selectFirst();
        popupType.getStyleClass().add("settings-combo");
        popupRow.getChildren().addAll(popupLabel, popupType);

        // ACP logs
        CheckBox acpLogs = new CheckBox("ACP logs");
        acpLogs.getStyleClass().add("settings-check");

        // Show in tool window
        CheckBox showToolWindow = new CheckBox("Show in tool window");
        showToolWindow.getStyleClass().add("settings-check");

        // AI Assistant
        Label aiSection = new Label("AI Assistant");
        aiSection.getStyleClass().add("settings-section");

        CheckBox aiActivator = new CheckBox("AI Assistant activator");
        aiActivator.getStyleClass().add("settings-check");
        CheckBox aiInstaller = new CheckBox("AI Assistant installer");
        aiInstaller.getStyleClass().add("settings-check");

        // Play sound
        CheckBox playSound = new CheckBox("Play sound");
        playSound.getStyleClass().add("settings-check");

        // Angular CLI
        CheckBox angularCli = new CheckBox("Angular CLI");
        angularCli.getStyleClass().add("settings-check");

        // Automatic indent detection disabled
        CheckBox autoIndent = new CheckBox("Automatic indent detection disabled");
        autoIndent.getStyleClass().add("settings-check");

        // Backup and Sync messages
        CheckBox backupSync = new CheckBox("Backup and Sync messages");
        backupSync.getStyleClass().add("settings-check");

        // Batch quick fix
        CheckBox batchQuickFix = new CheckBox("Batch quick fix");
        batchQuickFix.getStyleClass().add("settings-check");

        // Don't ask again notifications
        Label dontAskLabel = new Label("Don't ask again notifications:");
        dontAskLabel.getStyleClass().add("settings-section");

        ListView<String> dontAskList = new ListView<>();
        dontAskList.getStyleClass().add("settings-list");
        dontAskList.setPrefHeight(80);
        dontAskList.getItems().addAll(
            "Confirm before exiting the IDE",
            "When closing a tool window with a running process"
        );

        VBox content = new VBox(8,
            displayBalloon,
            enableSystem,
            popupRow,
            acpLogs,
            showToolWindow,
            aiSection,
            aiActivator,
            aiInstaller,
            playSound,
            angularCli,
            autoIndent,
            backupSync,
            batchQuickFix,
            dontAskLabel,
            dontAskList
        );
        
        getChildren().addAll(content);
    }
}