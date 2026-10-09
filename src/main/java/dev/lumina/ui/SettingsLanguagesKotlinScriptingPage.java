package dev.lumina.ui;

import dev.lumina.kotlin.KotlinScriptDefinition;
import dev.lumina.kotlin.KotlinScriptingSettings;
import dev.lumina.kotlin.KotlinScriptingSettingsManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Languages & Frameworks > Kotlin > Kotlin Scripting settings page in Lumina IDE.
 * Allows managing, reordering, and scanning Kotlin script definitions.
 */
public class SettingsLanguagesKotlinScriptingPage extends VBox {

    private final KotlinScriptingSettingsManager manager = KotlinScriptingSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private Button moveUpButton;
    private Button moveDownButton;
    private TableView<KotlinScriptDefinition> tableView;
    private ObservableList<KotlinScriptDefinition> tableItems;
    private Button scanClasspathButton;
    private Label scanStatusLabel;

    private KotlinScriptingSettings initialSettings;

    public SettingsLanguagesKotlinScriptingPage() {
        setSpacing(10);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        KotlinScriptingSettings current = manager.getSettings();

        // 1. Header label
        Label headerLabel = new Label("Manage Script Definitions:");
        headerLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // 2. Toolbar with Up and Down buttons
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 2, 0));

        moveUpButton = new Button("\u2191"); // ↑
        moveUpButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8;");
        moveUpButton.setTooltip(new Tooltip("Move Up"));
        moveUpButton.setDisable(true);
        moveUpButton.setOnAction(e -> moveSelectedRow(-1));

        moveDownButton = new Button("\u2193"); // ↓
        moveDownButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8;");
        moveDownButton.setTooltip(new Tooltip("Move Down"));
        moveDownButton.setDisable(true);
        moveDownButton.setOnAction(e -> moveSelectedRow(1));

        toolbar.getChildren().addAll(moveUpButton, moveDownButton);

        // 3. TableView
        tableView = new TableView<>();
        tableView.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4; " +
                "-fx-font-size: 13px;"
        );
        tableView.setPrefHeight(240);
        tableView.setMaxHeight(300);
        VBox.setVgrow(tableView, Priority.NEVER);

        tableItems = FXCollections.observableArrayList();
        for (KotlinScriptDefinition def : current.getDefinitions()) {
            tableItems.add(def.copy());
        }
        tableView.setItems(tableItems);

        TableColumn<KotlinScriptDefinition, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(260);
        nameCol.setReorderable(false);
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5;");
                }
            }
        });

        TableColumn<KotlinScriptDefinition, String> patternCol = new TableColumn<>("Pattern/Extension");
        patternCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPattern()));
        patternCol.setPrefWidth(300);
        patternCol.setReorderable(false);
        patternCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5;");
                }
            }
        });

        TableColumn<KotlinScriptDefinition, Boolean> enabledCol = new TableColumn<>("Is Enabled");
        enabledCol.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isEnabled()));
        enabledCol.setPrefWidth(90);
        enabledCol.setReorderable(false);
        enabledCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setAlignment(Pos.CENTER);
                checkBox.setOnAction(e -> {
                    KotlinScriptDefinition rowItem = getTableRow() != null ? getTableRow().getItem() : null;
                    if (rowItem != null && !rowItem.isLocked()) {
                        rowItem.setEnabled(checkBox.isSelected());
                        fireModified();
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    KotlinScriptDefinition rowItem = getTableRow() != null ? getTableRow().getItem() : null;
                    checkBox.setSelected(item);
                    if (rowItem != null && rowItem.isLocked()) {
                        checkBox.setDisable(true);
                        checkBox.setStyle("-fx-opacity: 0.6;");
                    } else {
                        checkBox.setDisable(false);
                        checkBox.setStyle("-fx-cursor: hand;");
                    }
                    setAlignment(Pos.CENTER);
                    setGraphic(checkBox);
                }
            }
        });

        tableView.getColumns().addAll(nameCol, patternCol, enabledCol);

        tableView.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> updateToolbarButtons());

        // 4. Note label
        Label noteLabel = new Label("The first definition that matches script pattern/extension is applied, starting from the top");
        noteLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        // 5. Scan Classpath button row
        HBox scanBox = new HBox(12);
        scanBox.setAlignment(Pos.CENTER_LEFT);
        scanBox.setPadding(new Insets(6, 0, 0, 0));

        scanClasspathButton = new Button("Scan Classpath");
        scanClasspathButton.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 13px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 4 14;"
        );

        scanStatusLabel = new Label();
        scanStatusLabel.setStyle("-fx-text-fill: #62B543; -fx-font-size: 12px;");
        scanStatusLabel.setVisible(false);

        scanClasspathButton.setOnAction(e -> {
            int count = manager.scanClasspath();
            scanStatusLabel.setText("Classpath scanned successfully (" + count + " definitions).");
            scanStatusLabel.setVisible(true);
        });

        scanBox.getChildren().addAll(scanClasspathButton, scanStatusLabel);

        getChildren().addAll(headerLabel, toolbar, tableView, noteLabel, scanBox);
    }

    private void updateToolbarButtons() {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        int size = tableItems.size();
        moveUpButton.setDisable(idx <= 0);
        moveDownButton.setDisable(idx < 0 || idx >= size - 1);
    }

    private void moveSelectedRow(int delta) {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        int newIdx = idx + delta;
        if (idx >= 0 && newIdx >= 0 && newIdx < tableItems.size()) {
            KotlinScriptDefinition item = tableItems.remove(idx);
            tableItems.add(newIdx, item);
            tableView.getSelectionModel().select(newIdx);
            updateToolbarButtons();
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
        this.initialSettings = getFormSettings();
    }

    private KotlinScriptingSettings getFormSettings() {
        List<KotlinScriptDefinition> defs = new ArrayList<>();
        for (KotlinScriptDefinition def : tableItems) {
            defs.add(def.copy());
        }
        return new KotlinScriptingSettings(defs);
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setSettings(getFormSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        if (initialSettings != null) {
            tableItems.clear();
            for (KotlinScriptDefinition def : initialSettings.getDefinitions()) {
                tableItems.add(def.copy());
            }
            updateToolbarButtons();
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public TableView<KotlinScriptDefinition> getTableView() {
        return tableView;
    }

    public ObservableList<KotlinScriptDefinition> getTableItems() {
        return tableItems;
    }

    public Button getMoveUpButton() {
        return moveUpButton;
    }

    public Button getMoveDownButton() {
        return moveDownButton;
    }

    public Button getScanClasspathButton() {
        return scanClasspathButton;
    }

    public Label getScanStatusLabel() {
        return scanStatusLabel;
    }
}
