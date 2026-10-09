package dev.lumina.ui;

import dev.lumina.javascript.JavaScriptSettingsManager;
import dev.lumina.javascript.StyledComponentsSettings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Languages & Frameworks > JavaScript > Styled Components settings page in Lumina IDE.
 */
public class SettingsLanguagesJSStyledComponentsPage extends VBox {

    private final JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private ListView<String> listView;
    private final ObservableList<String> prefixes = FXCollections.observableArrayList();
    private Button addButton;
    private Button removeButton;

    private List<String> initialPrefixes;

    public SettingsLanguagesJSStyledComponentsPage() {
        setSpacing(10);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        Label titleLabel = new Label("Additional template tag prefixes:");
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Toolbar (+, -)
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-padding: 4 6 4 6;");

        addButton = new Button("+");
        addButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-cursor: hand;");
        addButton.setOnMouseEntered(e -> addButton.setStyle("-fx-background-color: #35373B; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-cursor: hand;"));
        addButton.setOnMouseExited(e -> addButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-cursor: hand;"));
        addButton.setOnAction(e -> handleAdd());

        removeButton = new Button("−");
        removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-cursor: hand;");
        removeButton.setOnMouseEntered(e -> removeButton.setStyle("-fx-background-color: #35373B; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-cursor: hand;"));
        removeButton.setOnMouseExited(e -> removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-cursor: hand;"));
        removeButton.setOnAction(e -> handleRemove());

        toolbar.getChildren().addAll(addButton, removeButton);

        // List
        listView = new ListView<>(prefixes);
        listView.setEditable(true);
        listView.setCellFactory(TextFieldListCell.forListView());
        listView.setOnEditCommit(event -> {
            int idx = event.getIndex();
            String newVal = event.getNewValue();
            if (newVal != null && !newVal.isBlank()) {
                prefixes.set(idx, newVal.trim());
            } else {
                prefixes.remove(idx);
            }
            fireModified();
        });
        listView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1;");
        listView.setMaxWidth(Double.MAX_VALUE);
        listView.setPrefHeight(240);
        VBox.setVgrow(listView, Priority.ALWAYS);

        loadFromManager();

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateButtonStates());
        updateButtonStates();

        VBox listContainer = new VBox(toolbar, listView);
        VBox.setVgrow(listContainer, Priority.ALWAYS);

        getChildren().addAll(titleLabel, listContainer);
    }

    private void updateButtonStates() {
        removeButton.setDisable(listView.getSelectionModel().getSelectedItem() == null);
    }

    private void loadFromManager() {
        prefixes.clear();
        prefixes.addAll(manager.getStyledComponentsSettings().getAdditionalTagPrefixes());
    }

    private void handleAdd() {
        TextInputDialog dlg = new TextInputDialog("");
        dlg.setTitle("Add Tag Prefix");
        dlg.setHeaderText("Add additional template tag prefix");
        dlg.setContentText("Tag prefix:");
        Optional<String> res = dlg.showAndWait();
        res.ifPresent(prefix -> {
            if (!prefix.isBlank() && !prefixes.contains(prefix.trim())) {
                prefixes.add(prefix.trim());
                listView.getSelectionModel().select(prefix.trim());
                fireModified();
            }
        });
    }

    private void handleRemove() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            prefixes.remove(idx);
            fireModified();
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialPrefixes = new ArrayList<>(prefixes);
    }

    public boolean isModified() {
        return !Objects.equals(new ArrayList<>(prefixes), initialPrefixes);
    }

    public void apply() {
        manager.setStyledComponentsSettings(new StyledComponentsSettings(new ArrayList<>(prefixes)));
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        prefixes.clear();
        if (initialPrefixes != null) {
            prefixes.addAll(initialPrefixes);
        }
        updateButtonStates();
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public ListView<String> getListView() {
        return listView;
    }

    public ObservableList<String> getPrefixes() {
        return prefixes;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }
}
