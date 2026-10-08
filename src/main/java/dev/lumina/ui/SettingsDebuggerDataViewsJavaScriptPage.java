package dev.lumina.ui;

import dev.lumina.debugger.DebuggerSettingsManager;
import dev.lumina.debugger.JavaScriptDataViewsSettings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Debugger > Data Views > JavaScript.
 * Accurately replicates the UI and behavior shown in Image 1.
 */
public class SettingsDebuggerDataViewsJavaScriptPage extends VBox {

    private final DebuggerSettingsManager manager = DebuggerSettingsManager.getInstance();

    private final CheckBox showObjectPropertiesCheck = new CheckBox("Show the following properties for an object node:");
    private final ObservableList<String> propertyItems = FXCollections.observableArrayList();
    private final ListView<String> propertyListView = new ListView<>(propertyItems);
    private final VBox listContainer = new VBox();

    private JavaScriptDataViewsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDebuggerDataViewsJavaScriptPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(10);

        buildUI();
        loadData();
    }

    private void buildUI() {
        showObjectPropertiesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        showObjectPropertiesCheck.selectedProperty().addListener((obs, o, n) -> {
            listContainer.setDisable(!n);
            checkModified();
        });

        // Toolbar
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-padding: 3 6 3 6;");

        Button addBtn = createToolbarButton("+", "Add property");
        Button removeBtn = createToolbarButton("—", "Remove property");

        addBtn.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog("property");
            tid.setTitle("Add Object Property");
            tid.setHeaderText("Enter property name to display for JavaScript object nodes:");
            tid.showAndWait().ifPresent(prop -> {
                if (!prop.isBlank() && !propertyItems.contains(prop.trim())) {
                    propertyItems.add(prop.trim());
                    checkModified();
                }
            });
        });

        removeBtn.setOnAction(e -> {
            String sel = propertyListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                propertyItems.remove(sel);
                checkModified();
            }
        });

        toolbar.getChildren().addAll(addBtn, removeBtn);

        // ListView
        propertyListView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1;");
        propertyListView.setPrefHeight(180);
        propertyListView.setMaxHeight(260);

        propertyListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 2 8 2 8;");
                }
            }
        });

        listContainer.getChildren().addAll(toolbar, propertyListView);
        listContainer.setMaxWidth(480);
        VBox.setMargin(listContainer, new Insets(0, 0, 0, 22));

        getChildren().addAll(showObjectPropertiesCheck, listContainer);
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373B; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"));
        return btn;
    }

    public void loadData() {
        suppressEvents = true;
        initialSettings = manager.getJavaScriptDataViewsSettings();

        showObjectPropertiesCheck.setSelected(initialSettings.isShowObjectProperties());
        listContainer.setDisable(!initialSettings.isShowObjectProperties());

        propertyItems.clear();
        propertyItems.addAll(initialSettings.getObjectProperties());

        suppressEvents = false;
        checkModified();
    }

    public JavaScriptDataViewsSettings getCurrentSettings() {
        JavaScriptDataViewsSettings s = new JavaScriptDataViewsSettings();
        s.setShowObjectProperties(showObjectPropertiesCheck.isSelected());
        s.setObjectProperties(new ArrayList<>(propertyItems));
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        if (isModified()) {
            initialSettings = getCurrentSettings();
            manager.setJavaScriptDataViewsSettings(initialSettings);
            checkModified();
        }
    }

    public void reset() {
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void checkModified() {
        if (suppressEvents) return;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
