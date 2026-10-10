package dev.lumina.ui;

import dev.lumina.database.versioning.DatabaseTypeMappingItem;
import dev.lumina.database.versioning.DatabaseTypeMappingsSettings;
import dev.lumina.database.versioning.DatabaseTypeMappingsSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.*;

/**
 * Tools > Database Versioning > Type Mappings settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsDatabaseVersioningTypeMappingsPage extends HBox {

    private final DatabaseTypeMappingsSettingsManager manager;
    private DatabaseTypeMappingsSettings initialSettings;
    private DatabaseTypeMappingsSettings currentSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private ListView<String> dbListView;
    private TableView<DatabaseTypeMappingItem> mappingsTable;
    private ObservableList<DatabaseTypeMappingItem> currentTableData;

    private Button addBtn;
    private Button removeBtn;
    private Button editBtn;

    public SettingsDatabaseVersioningTypeMappingsPage() {
        this.manager = DatabaseTypeMappingsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 20, 20, 20));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Left Database list
        dbListView = new ListView<>();
        dbListView.setPrefWidth(200);
        dbListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        dbListView.getItems().addAll(DatabaseTypeMappingsSettings.SUPPORTED_DATABASES);

        dbListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setGraphic(createDbBadge(item));
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 6 10 6 10;");
                }
            }
        });

        dbListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !updating) {
                syncCurrentTableToSettings(oldVal);
                loadTableForDatabase(newVal);
                if (currentSettings != null) {
                    currentSettings.setSelectedDatabase(newVal);
                }
                notifyModified();
            }
        });

        // 2. Right Pane: Header, Toolbar, TableView
        VBox rightPane = new VBox(8);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        Label mappingLabel = new Label("Mapping types:");
        mappingLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Toolbar (+, -, edit)
        HBox toolbar = new HBox(6);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        addBtn = createIconButton("+", "Add mapping override");
        removeBtn = createIconButton("—", "Remove selected mapping");
        editBtn = createIconButton("✎", "Edit selected mapping");
        removeBtn.setDisable(true);
        editBtn.setDisable(true);

        addBtn.setOnAction(e -> handleAdd());
        removeBtn.setOnAction(e -> handleRemove());
        editBtn.setOnAction(e -> handleEdit());

        toolbar.getChildren().addAll(addBtn, removeBtn, editBtn);

        // TableView
        mappingsTable = createTableView();
        VBox.setVgrow(mappingsTable, Priority.ALWAYS);

        rightPane.getChildren().addAll(mappingLabel, toolbar, mappingsTable);

        getChildren().addAll(dbListView, rightPane);
    }

    private Circle createDbBadge(String db) {
        String colorWeb = switch (db.toLowerCase()) {
            case "mysql" -> "#00758F";
            case "mariadb" -> "#C07E4C";
            case "postgresql" -> "#336791";
            case "mssql" -> "#CE422B";
            case "oracle" -> "#F80000";
            case "h2" -> "#006699";
            case "db2" -> "#41B883";
            case "hsqldb" -> "#4A90E2";
            default -> "#8C8C8C";
        };
        return new Circle(4, Color.web(colorWeb));
    }

    private Button createIconButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3;"));
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    @SuppressWarnings("unchecked")
    private TableView<DatabaseTypeMappingItem> createTableView() {
        TableView<DatabaseTypeMappingItem> tv = new TableView<>();
        tv.setEditable(true);
        tv.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label placeholder = new Label("No override mappings");
        placeholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        tv.setPlaceholder(placeholder);

        TableColumn<DatabaseTypeMappingItem, String> attrCol = new TableColumn<>("Attribute/Converter/Hibernate Type");
        attrCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAttributeType()));
        attrCol.setCellFactory(TextFieldTableCell.forTableColumn());
        attrCol.setOnEditCommit(evt -> {
            evt.getRowValue().setAttributeType(evt.getNewValue());
            notifyModified();
        });
        attrCol.setPrefWidth(260);

        TableColumn<DatabaseTypeMappingItem, String> targetCol = new TableColumn<>("Target Type");
        targetCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTargetType()));
        targetCol.setCellFactory(TextFieldTableCell.forTableColumn());
        targetCol.setOnEditCommit(evt -> {
            evt.getRowValue().setTargetType(evt.getNewValue());
            notifyModified();
        });
        targetCol.setPrefWidth(220);

        TableColumn<DatabaseTypeMappingItem, String> paramsCol = new TableColumn<>("Type Parameters");
        paramsCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeParameters()));
        paramsCol.setCellFactory(TextFieldTableCell.forTableColumn());
        paramsCol.setOnEditCommit(evt -> {
            evt.getRowValue().setTypeParameters(evt.getNewValue());
            notifyModified();
        });
        paramsCol.setPrefWidth(220);

        tv.getColumns().addAll(attrCol, targetCol, paramsCol);

        tv.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSel = newV != null;
            removeBtn.setDisable(!hasSel);
            editBtn.setDisable(!hasSel);
        });

        currentTableData = FXCollections.observableArrayList();
        tv.setItems(currentTableData);
        return tv;
    }

    private void handleAdd() {
        DatabaseTypeMappingItem item = new DatabaseTypeMappingItem("java.lang.String", "VARCHAR", "255");
        currentTableData.add(item);
        mappingsTable.getSelectionModel().select(item);
        syncCurrentTableToSettings(dbListView.getSelectionModel().getSelectedItem());
        notifyModified();
    }

    private void handleRemove() {
        DatabaseTypeMappingItem sel = mappingsTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            currentTableData.remove(sel);
            syncCurrentTableToSettings(dbListView.getSelectionModel().getSelectedItem());
            notifyModified();
        }
    }

    private void handleEdit() {
        int idx = mappingsTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            mappingsTable.edit(idx, mappingsTable.getColumns().get(0));
        }
    }

    private void syncCurrentTableToSettings(String db) {
        if (db != null && currentSettings != null) {
            List<DatabaseTypeMappingItem> list = new ArrayList<>();
            for (DatabaseTypeMappingItem item : currentTableData) {
                list.add(item.clone());
            }
            currentSettings.getDatabaseMappings().put(db, list);
        }
    }

    private void loadTableForDatabase(String db) {
        currentTableData.clear();
        if (db != null && currentSettings != null) {
            List<DatabaseTypeMappingItem> items = currentSettings.getMappingsForDatabase(db);
            for (DatabaseTypeMappingItem it : items) {
                currentTableData.add(it.clone());
            }
        }
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            currentSettings = initialSettings.clone();

            String selDb = currentSettings.getSelectedDatabase();
            if (selDb == null || !DatabaseTypeMappingsSettings.SUPPORTED_DATABASES.contains(selDb)) {
                selDb = "mysql";
            }
            dbListView.getSelectionModel().select(selDb);
            loadTableForDatabase(selDb);
        } finally {
            updating = false;
        }
    }

    public boolean isModified() {
        if (initialSettings == null || currentSettings == null) return false;
        syncCurrentTableToSettings(dbListView.getSelectionModel().getSelectedItem());
        return !Objects.equals(initialSettings, currentSettings);
    }

    public void apply() {
        syncCurrentTableToSettings(dbListView.getSelectionModel().getSelectedItem());
        manager.setSettings(currentSettings);
        initialSettings = currentSettings.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for testing
    public ListView<String> getDbListView() { return dbListView; }
    public TableView<DatabaseTypeMappingItem> getMappingsTable() { return mappingsTable; }
    public Button getAddBtn() { return addBtn; }
    public Button getRemoveBtn() { return removeBtn; }
    public Button getEditBtn() { return editBtn; }
}
