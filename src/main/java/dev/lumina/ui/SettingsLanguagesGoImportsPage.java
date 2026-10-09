package dev.lumina.ui;

import dev.lumina.go.GoSettings;
import dev.lumina.go.GoSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings page for Languages & Frameworks > Go > Imports.
 * Faithfully matches Image 3.
 */
public class SettingsLanguagesGoImportsPage extends VBox {

    private final GoSettingsManager manager = GoSettingsManager.getInstance();

    private CheckBox showImportPopupCheck;
    private CheckBox addUnambiguousImportsCheck;
    private CheckBox optimizeImportsCheck;

    private final ObservableList<String> excludedImports = FXCollections.observableArrayList();
    private ListView<String> listView;

    private boolean initialShowPopup = true;
    private boolean initialAddUnambiguous = true;
    private boolean initialOptimize = true;
    private List<String> initialExcludedImports = new ArrayList<>();

    private Runnable onModified;

    public SettingsLanguagesGoImportsPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadFromManager();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // Section: Auto Imports and Completion
        HBox autoImportsHeader = createSectionHeader("Auto Imports and Completion");

        showImportPopupCheck = new CheckBox("Show import popup");
        showImportPopupCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        showImportPopupCheck.setSelected(true);
        showImportPopupCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        addUnambiguousImportsCheck = new CheckBox("Add unambiguous imports on the fly");
        addUnambiguousImportsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        addUnambiguousImportsCheck.setSelected(true);
        addUnambiguousImportsCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        optimizeImportsCheck = new CheckBox("Optimize imports on the fly");
        optimizeImportsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        optimizeImportsCheck.setSelected(true);
        optimizeImportsCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        VBox checksBox = new VBox(8, showImportPopupCheck, addUnambiguousImportsCheck, optimizeImportsCheck);
        checksBox.setPadding(new Insets(2, 0, 8, 0));

        // Sub-section: Exclude from import and completion:
        Label excludeLabel = new Label("Exclude from import and completion:");
        excludeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Toolbar (+ / —)
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(3, 6, 3, 6));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;");
        addBtn.setOnAction(e -> showAddExcludedImportDialog());

        Button removeBtn = new Button("—");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;");
        removeBtn.setOnAction(e -> removeSelectedExcludedImport());

        toolbar.getChildren().addAll(addBtn, removeBtn);

        // ListView
        listView = new ListView<>(excludedImports);
        listView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4;");
        listView.setPrefHeight(180);
        VBox.setVgrow(listView, Priority.ALWAYS);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
                }
            }
        });

        VBox excludeBox = new VBox(excludeLabel, toolbar, listView);
        excludeBox.setSpacing(4);
        VBox.setVgrow(excludeBox, Priority.ALWAYS);

        getChildren().addAll(autoImportsHeader, checksBox, excludeBox);
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(2, 0, 4, 0));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(lbl, line);
        return header;
    }

    private Stage getOwnerStage() {
        if (getScene() != null && getScene().getWindow() instanceof Stage s) {
            return s;
        }
        return null;
    }

    private void showAddExcludedImportDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(getOwnerStage());
        dialog.setTitle("Exclude Import");
        dialog.setHeaderText("Exclude from import and completion");
        dialog.setContentText("Package import path:");
        dialog.showAndWait().ifPresent(val -> {
            String trimmed = val.trim();
            if (!trimmed.isEmpty() && !excludedImports.contains(trimmed)) {
                excludedImports.add(trimmed);
                notifyModified();
            }
        });
    }

    private void removeSelectedExcludedImport() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < excludedImports.size()) {
            excludedImports.remove(idx);
            notifyModified();
        }
    }

    public void loadFromManager() {
        GoSettings s = manager.getSettings();
        showImportPopupCheck.setSelected(s.isShowImportPopup());
        addUnambiguousImportsCheck.setSelected(s.isAddUnambiguousImportsOnTheFly());
        optimizeImportsCheck.setSelected(s.isOptimizeImportsOnTheFly());
        excludedImports.setAll(s.getExcludedImports());

        initialShowPopup = s.isShowImportPopup();
        initialAddUnambiguous = s.isAddUnambiguousImportsOnTheFly();
        initialOptimize = s.isOptimizeImportsOnTheFly();
        initialExcludedImports = new ArrayList<>(excludedImports);
    }

    public boolean isModified() {
        return showImportPopupCheck.isSelected() != initialShowPopup ||
                addUnambiguousImportsCheck.isSelected() != initialAddUnambiguous ||
                optimizeImportsCheck.isSelected() != initialOptimize ||
                !excludedImports.equals(initialExcludedImports);
    }

    public void apply() {
        GoSettings s = manager.getSettings();
        s.setShowImportPopup(showImportPopupCheck.isSelected());
        s.setAddUnambiguousImportsOnTheFly(addUnambiguousImportsCheck.isSelected());
        s.setOptimizeImportsOnTheFly(optimizeImportsCheck.isSelected());
        s.setExcludedImports(new ArrayList<>(excludedImports));
        manager.setSettings(s);

        initialShowPopup = showImportPopupCheck.isSelected();
        initialAddUnambiguous = addUnambiguousImportsCheck.isSelected();
        initialOptimize = optimizeImportsCheck.isSelected();
        initialExcludedImports = new ArrayList<>(excludedImports);
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }

    public boolean isShowImportPopup() {
        return showImportPopupCheck.isSelected();
    }

    public boolean isAddUnambiguousImports() {
        return addUnambiguousImportsCheck.isSelected();
    }

    public boolean isOptimizeImports() {
        return optimizeImportsCheck.isSelected();
    }

    public List<String> getExcludedImports() {
        return new ArrayList<>(excludedImports);
    }
}
