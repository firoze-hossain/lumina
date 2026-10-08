package dev.lumina.ui;

import dev.lumina.build.MavenSettings;
import dev.lumina.build.MavenSettings.RepositoryItem;
import dev.lumina.build.MavenSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > Maven > Repositories.
 * Matches 1:1 with reference specification and dynamically manages indexed Maven repositories.
 */
public class SettingsMavenRepositoriesPage extends VBox {

    private final MavenSettingsManager manager = MavenSettingsManager.getInstance();

    private final ObservableList<RepositoryItem> reposList = FXCollections.observableArrayList();
    private final TableView<RepositoryItem> reposTable = new TableView<>();
    private final Button updateBtn = new Button("Update");

    private Map<String, String> initialTimestamps = new LinkedHashMap<>();
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsMavenRepositoriesPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // Header with label on left and Update button on top right
        Label headerLabel = new Label("Indexed Maven Repositories:");
        headerLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        updateBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 16; -fx-font-size: 13px; -fx-cursor: hand;");
        updateBtn.setDisable(true);
        updateBtn.setOnAction(e -> handleUpdate());

        HBox topBar = new HBox(10, headerLabel, spacer, updateBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(topBar);

        // Table
        setupTable();
        reposTable.setItems(reposList);
        reposTable.setPrefHeight(380);
        VBox.setVgrow(reposTable, Priority.ALWAYS);
        getChildren().add(reposTable);

        // Table selection listener
        reposTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            updateBtn.setDisable(newSel == null);
        });
    }

    private void setupTable() {
        reposTable.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-table-cell-border-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        reposTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<RepositoryItem, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().url()));
        urlCol.setMinWidth(280);
        urlCol.setPrefWidth(460);
        urlCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(Color.web("#DFE1E5"));
                }
            }
        });

        TableColumn<RepositoryItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type()));
        typeCol.setMinWidth(100);
        typeCol.setPrefWidth(120);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(Color.web("#DFE1E5"));
                }
            }
        });

        TableColumn<RepositoryItem, String> updatedCol = new TableColumn<>("Updated");
        updatedCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().updated()));
        updatedCol.setMinWidth(120);
        updatedCol.setPrefWidth(180);
        updatedCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(Color.web("#8C919D"));
                }
            }
        });

        reposTable.getColumns().setAll(urlCol, typeCol, updatedCol);
    }

    private void handleUpdate() {
        RepositoryItem sel = reposTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        int idx = reposTable.getSelectionModel().getSelectedIndex();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US));
        RepositoryItem updated = new RepositoryItem(sel.url(), sel.type(), now);
        reposList.set(idx, updated);
        reposTable.getSelectionModel().select(idx);
        notifyModified();
    }

    public void loadData() {
        updating = true;
        MavenSettings s = manager.getSettings();
        initialTimestamps = s.getRepositoryUpdatedTimestamps();

        reposList.setAll(s.getRepositories());
        if (!reposList.isEmpty()) {
            reposTable.getSelectionModel().select(0);
        }
        updating = false;
    }

    public boolean isModified() {
        Map<String, String> currentMap = getCurrentTimestamps();
        return !initialTimestamps.equals(currentMap);
    }

    public void apply() {
        MavenSettings s = manager.getSettings();
        Map<String, String> currentMap = getCurrentTimestamps();
        s.setRepositoryUpdatedTimestamps(currentMap);
        manager.setSettings(s);
        initialTimestamps = new LinkedHashMap<>(currentMap);
    }

    public void reset() {
        loadData();
    }

    public Map<String, String> getCurrentTimestamps() {
        Map<String, String> map = new LinkedHashMap<>();
        for (RepositoryItem item : reposList) {
            if (item.updated() != null && !item.updated().isBlank()) {
                map.put(item.url(), item.updated());
            }
        }
        return map;
    }

    public ObservableList<RepositoryItem> getReposList() {
        return reposList;
    }

    public TableView<RepositoryItem> getReposTable() {
        return reposTable;
    }

    public Button getUpdateBtn() {
        return updateBtn;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
