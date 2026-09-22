package dev.lumina.ui;

import dev.lumina.security.TrustedLocationsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

/**
 * Trusted Locations settings page.
 * Strictly matches media_1790046849995.png.
 */
public class SettingsTrustedLocationsPage extends VBox {

    private final TrustedLocationsManager manager = TrustedLocationsManager.getInstance();
    private final ListView<String> locationsListView = new ListView<>();
    private final ObservableList<String> locationsData = FXCollections.observableArrayList();

    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("−");
    private final Button moveUpButton = new Button("↑");
    private final Button moveDownButton = new Button("↓");

    public SettingsTrustedLocationsPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 16, 24));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Top Description (strictly from screenshot)
        Label desc = new Label("Projects located under these local directories will be considered as trusted");
        desc.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // 2. Toolbar (+, -, Up, Down)
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 0, 4, 0));

        styleToolButton(addButton, "Add trusted location");
        addButton.setOnAction(e -> onAddLocation());

        styleToolButton(removeButton, "Remove");
        removeButton.setOnAction(e -> onRemoveLocation());

        styleToolButton(moveUpButton, "Move Up");
        moveUpButton.setOnAction(e -> onMoveUp());

        styleToolButton(moveDownButton, "Move Down");
        moveDownButton.setOnAction(e -> onMoveDown());

        toolbar.getChildren().addAll(addButton, removeButton, moveUpButton, moveDownButton);

        // 3. ListView of Trusted Paths
        locationsListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(locationsListView, Priority.ALWAYS);

        locationsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    setText(item);
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    String textFill = isSelected() ? "#FFFFFF" : "#DFE1E5";
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                }
            }
        });

        locationsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateButtonStates());

        getChildren().addAll(desc, toolbar, locationsListView);

        manager.addListener(this::reloadLocations);
        reloadLocations();
    }

    private void styleToolButton(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisabled()) btn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        });
    }

    private void reloadLocations() {
        locationsData.setAll(manager.getLocations());
        locationsListView.setItems(locationsData);
        updateButtonStates();
    }

    private void updateButtonStates() {
        int idx = locationsListView.getSelectionModel().getSelectedIndex();
        int size = locationsData.size();
        removeButton.setDisable(idx < 0);
        moveUpButton.setDisable(idx <= 0);
        moveDownButton.setDisable(idx < 0 || idx >= size - 1);
    }

    private void onAddLocation() {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        NewTrustedLocationDialog dialog = new NewTrustedLocationDialog(owner);
        String path = dialog.showAndWait();
        if (path != null && !path.isBlank()) {
            manager.addLocation(path);
            locationsListView.getSelectionModel().select(TrustedLocationsManager.normalizePath(path));
        }
    }

    private void onRemoveLocation() {
        int idx = locationsListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            manager.removeLocation(idx);
            int newIdx = Math.min(idx, manager.getLocations().size() - 1);
            if (newIdx >= 0) {
                locationsListView.getSelectionModel().select(newIdx);
            }
        }
    }

    private void onMoveUp() {
        int idx = locationsListView.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            manager.moveUp(idx);
            locationsListView.getSelectionModel().select(idx - 1);
        }
    }

    private void onMoveDown() {
        int idx = locationsListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < locationsData.size() - 1) {
            manager.moveDown(idx);
            locationsListView.getSelectionModel().select(idx + 1);
        }
    }
}