package dev.lumina.ui;

import dev.lumina.git.VcsColorManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Version Control > File Status Colors settings page matching IntelliJ IDEA Image 5.
 */
public class SettingsVcsFileStatusColorsPage extends VBox {

    private final VcsColorManager colorManager = VcsColorManager.getInstance();

    private final ListView<VcsColorManager.StatusColorDef> statusListView = new ListView<>();
    private final CheckBox enableColorCheck = new CheckBox("File Status Color:");
    private final Button colorSwatchBtn = new Button();
    private final ColorPicker colorPicker = new ColorPicker();
    private final Button restoreDefaultBtn = new Button("Restore Default");
    private final Label customizedFooterLabel = new Label("* Customized");

    private VcsColorManager.StatusColorDef selectedDef;

    public SettingsVcsFileStatusColorsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Left List
        buildStatusList();

        VBox leftBox = new VBox(8, statusListView, customizedFooterLabel);
        leftBox.setPrefWidth(340);
        leftBox.setMinWidth(300);
        customizedFooterLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 4 0 0 4;");

        // 2. Right Detail Panel
        VBox rightBox = buildDetailPanel();

        HBox mainSplit = new HBox(24, leftBox, rightBox);
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        getChildren().add(mainSplit);

        colorManager.addListener(this::refreshView);

        // Default selection
        if (!statusListView.getItems().isEmpty()) {
            statusListView.getSelectionModel().select(0);
        }
    }

    private void buildStatusList() {
        statusListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 3;");
        statusListView.setPrefHeight(480);
        VBox.setVgrow(statusListView, Priority.ALWAYS);

        statusListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(VcsColorManager.StatusColorDef item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    boolean customized = colorManager.isCustomized(item.getKey());
                    String prefix = customized ? "* " : (hasDefaultPrefix(item.getKey()) ? "* " : "  ");
                    String hex = colorManager.getColorHex(item.getKey());
                    setText(prefix + item.getKey());

                    boolean isSelected = isSelected();
                    String bgStyle = isSelected ? "-fx-background-color: #2E436E;" : "-fx-background-color: transparent;";
                    setStyle(bgStyle + " -fx-text-fill: #" + hex + "; -fx-font-size: 12px; -fx-padding: 3 8 3 8; -fx-cursor: hand;");
                }
            }
        });

        statusListView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                selectedDef = item;
                updateDetailPanel(item);
            }
        });

        List<VcsColorManager.StatusColorDef> defs = colorManager.getAllStatusDefinitions();
        statusListView.getItems().setAll(defs);
    }

    private boolean hasDefaultPrefix(String key) {
        // In IntelliJ Image 5, statuses that have default or customized color definitions show an asterisk
        return List.of("Added", "Added in not active changelist", "Changelist conflict", "Copied",
                "Deleted", "Deleted from file system", "Have changed descendants", "Have immediate changed children",
                "Ignored", "Merged", "Merged with conflicts", "Merged with property conflicts",
                "Merged with text and property con...", "Modified", "Modified in not active changelist",
                "Renamed", "Unknown").contains(key);
    }

    private VBox buildDetailPanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(10, 10, 10, 10));

        enableColorCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        enableColorCheck.setSelected(true);

        colorSwatchBtn.setPrefWidth(74);
        colorSwatchBtn.setPrefHeight(24);
        colorSwatchBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 3; -fx-cursor: hand; -fx-border-color: #393B40; -fx-border-radius: 3;");

        colorPicker.setStyle("-fx-opacity: 0; -fx-pref-width: 1; -fx-pref-height: 1;");
        colorPicker.valueProperty().addListener((obs, oldC, newC) -> {
            if (newC != null && selectedDef != null) {
                String hex = String.format("%02X%02X%02X",
                        (int) (newC.getRed() * 255),
                        (int) (newC.getGreen() * 255),
                        (int) (newC.getBlue() * 255));
                colorManager.setColor(selectedDef.getKey(), hex);
                updateDetailPanel(selectedDef);
                statusListView.refresh();
            }
        });

        colorSwatchBtn.setOnAction(e -> {
            if (selectedDef != null) {
                try {
                    String curHex = colorManager.getColorHex(selectedDef.getKey());
                    colorPicker.setValue(Color.web("#" + curHex));
                } catch (Exception ignored) {}
                colorPicker.show();
            }
        });

        HBox colorRow = new HBox(12, enableColorCheck, colorSwatchBtn, colorPicker);
        colorRow.setAlignment(Pos.CENTER_LEFT);

        restoreDefaultBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;");
        restoreDefaultBtn.setOnAction(e -> {
            if (selectedDef != null) {
                colorManager.restoreDefault(selectedDef.getKey());
                updateDetailPanel(selectedDef);
                statusListView.refresh();
            }
        });

        panel.getChildren().addAll(colorRow, restoreDefaultBtn);
        return panel;
    }

    private void updateDetailPanel(VcsColorManager.StatusColorDef def) {
        if (def == null) return;
        String hex = colorManager.getColorHex(def.getKey());
        colorSwatchBtn.setText(hex);

        // Text color contrasting against swatch background
        Color c;
        try {
            c = Color.web("#" + hex);
        } catch (Exception e) {
            c = Color.web("#73BD79");
        }
        double brightness = (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114);
        String textCol = brightness > 0.6 ? "#1E1F22" : "#FFFFFF";

        colorSwatchBtn.setStyle("-fx-background-color: #" + hex + "; -fx-text-fill: " + textCol + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 3; -fx-cursor: hand; -fx-border-color: #393B40; -fx-border-radius: 3;");

        boolean customized = colorManager.isCustomized(def.getKey());
        restoreDefaultBtn.setDisable(!customized);
        restoreDefaultBtn.setOpacity(customized ? 1.0 : 0.6);
    }

    private void refreshView() {
        statusListView.refresh();
        if (selectedDef != null) {
            updateDetailPanel(selectedDef);
        }
    }
}
