package dev.lumina.ui;

import dev.lumina.schemas.JsonSchemaMapping;
import dev.lumina.schemas.JsonSchemaMappingsSettings;
import dev.lumina.schemas.JsonSchemaPattern;
import dev.lumina.schemas.SchemasAndDtdsSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Languages & Frameworks > Schemas and DTDs > JSON Schema Mappings settings page in Lumina IDE.
 * Matches Image 5:
 *  - Master-Detail SplitPane layout:
 *    - Left pane: Toolbar +, -, List of schema mappings (default: "New Schema")
 *    - Right pane:
 *      - Name: TextField
 *      - Schema file or URL: TextField + file chooser + URL button
 *      - Schema version: ComboBox ("JSON Schema v4", etc.)
 *      - Patterns Table with toolbar +, -, edit
 *      - Centered placeholder: "No schema mappings defined" + "Add mapping for a file, file path pattern, directory"
 *      - Bottom help text: "Path to file or directory relative to project root, or file name pattern like *.config.json"
 */
public class SettingsLanguagesJsonSchemaMappingsPage extends VBox {

    private final SchemasAndDtdsSettingsManager manager = SchemasAndDtdsSettingsManager.getInstance();
    private Runnable onModifiedListener;

    // Master
    private Button addMappingBtn;
    private Button removeMappingBtn;
    private ListView<String> masterListView;
    private ObservableList<String> masterItems;

    // Detail
    private VBox detailPane;
    private TextField nameField;
    private TextField schemaFileOrUrlField;
    private Button browseFileBtn;
    private Button fetchUrlBtn;
    private ComboBox<String> schemaVersionComboBox;

    private Button addPatternBtn;
    private Button removePatternBtn;
    private Button editPatternBtn;
    private TableView<PatternRow> patternsTable;
    private ObservableList<PatternRow> patternsItems;

    // Internal data cache
    private List<JsonSchemaMapping> workingMappings = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean updatingUi = false;

    private JsonSchemaMappingsSettings initialSettings;

    public static class PatternRow {
        private final SimpleStringProperty pattern;
        private final SimpleStringProperty type;

        public PatternRow(String pattern, String type) {
            this.pattern = new SimpleStringProperty(pattern != null ? pattern : "");
            this.type = new SimpleStringProperty(type != null ? type : "");
        }

        public String getPattern() {
            return pattern.get();
        }

        public void setPattern(String pattern) {
            this.pattern.set(pattern);
        }

        public SimpleStringProperty patternProperty() {
            return pattern;
        }

        public String getType() {
            return type.get();
        }

        public void setType(String type) {
            this.type.set(type);
        }

        public SimpleStringProperty typeProperty() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PatternRow that = (PatternRow) o;
            return Objects.equals(getPattern(), that.getPattern()) &&
                    Objects.equals(getType(), that.getType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPattern(), getType());
        }
    }

    public SettingsLanguagesJsonSchemaMappingsPage() {
        setSpacing(10);
        setPadding(new Insets(16, 20, 16, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        loadDataFromSettings();
        takeSnapshot();
    }

    private void buildContent() {
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // 1. Left Master Pane
        VBox masterBox = new VBox(6);
        masterBox.setPrefWidth(240);
        masterBox.setMinWidth(180);
        masterBox.setMaxWidth(350);

        HBox masterToolbar = new HBox(4);
        masterToolbar.setAlignment(Pos.CENTER_LEFT);

        addMappingBtn = new Button("+");
        styleToolbarButton(addMappingBtn);
        addMappingBtn.setOnAction(e -> handleAddSchema());

        removeMappingBtn = new Button("-");
        styleToolbarButton(removeMappingBtn);
        removeMappingBtn.setOnAction(e -> handleRemoveSchema());

        masterToolbar.getChildren().addAll(addMappingBtn, removeMappingBtn);

        masterListView = new ListView<>();
        masterListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        VBox.setVgrow(masterListView, Priority.ALWAYS);

        masterItems = FXCollections.observableArrayList();
        masterListView.setItems(masterItems);

        masterListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            if (!updatingUi) {
                saveCurrentDetailToCache();
                int newIndex = newV != null ? newV.intValue() : -1;
                if (newIndex >= 0) {
                    selectedIndex = newIndex;
                    loadDetailFromCache(selectedIndex);
                }
            }
        });

        masterBox.getChildren().addAll(masterToolbar, masterListView);

        // 2. Right Detail Pane
        detailPane = new VBox(12);
        detailPane.setPadding(new Insets(0, 0, 0, 16));
        HBox.setHgrow(detailPane, Priority.ALWAYS);

        // Name row
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = createLabel("Name:");
        nameLabel.setPrefWidth(130);
        nameField = new TextField();
        nameField.setPrefWidth(350);
        styleTextField(nameField);
        nameField.textProperty().addListener((obs, oldV, newV) -> {
            if (!updatingUi && selectedIndex >= 0 && selectedIndex < workingMappings.size()) {
                workingMappings.get(selectedIndex).setName(newV);
                int cur = selectedIndex;
                updatingUi = true;
                try {
                    masterItems.set(cur, newV);
                    masterListView.getSelectionModel().select(cur);
                } finally {
                    updatingUi = false;
                }
                fireModified();
            }
        });
        nameRow.getChildren().addAll(nameLabel, nameField);

        // Schema file or URL row
        HBox schemaRow = new HBox(8);
        schemaRow.setAlignment(Pos.CENTER_LEFT);
        Label schemaLabel = createLabel("Schema file or URL:");
        schemaLabel.setPrefWidth(130);
        schemaFileOrUrlField = new TextField();
        HBox.setHgrow(schemaFileOrUrlField, Priority.ALWAYS);
        styleTextField(schemaFileOrUrlField);
        schemaFileOrUrlField.textProperty().addListener((obs, oldV, newV) -> {
            if (!updatingUi && selectedIndex >= 0 && selectedIndex < workingMappings.size()) {
                workingMappings.get(selectedIndex).setSchemaFileOrUrl(newV);
                fireModified();
            }
        });

        browseFileBtn = new Button("\uD83D\uDCC1"); // folder icon
        styleToolbarButton(browseFileBtn);
        browseFileBtn.setTooltip(new Tooltip("Browse schema file..."));
        browseFileBtn.setOnAction(e -> handleBrowseSchemaFile());

        fetchUrlBtn = new Button("\uD83C\uDF10"); // globe icon
        styleToolbarButton(fetchUrlBtn);
        fetchUrlBtn.setTooltip(new Tooltip("Download or configure remote schema URL"));
        fetchUrlBtn.setOnAction(e -> handleFetchUrl());

        schemaRow.getChildren().addAll(schemaLabel, schemaFileOrUrlField, browseFileBtn, fetchUrlBtn);

        // Schema version row
        HBox versionRow = new HBox(12);
        versionRow.setAlignment(Pos.CENTER_LEFT);
        Label versionLabel = createLabel("Schema version:");
        versionLabel.setPrefWidth(130);
        schemaVersionComboBox = new ComboBox<>(FXCollections.observableArrayList(
                JsonSchemaMapping.VERSION_V4,
                JsonSchemaMapping.VERSION_V7,
                JsonSchemaMapping.VERSION_2019_09,
                JsonSchemaMapping.VERSION_2020_12
        ));
        schemaVersionComboBox.setPrefWidth(180);
        styleComboBox(schemaVersionComboBox);
        schemaVersionComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updatingUi && selectedIndex >= 0 && selectedIndex < workingMappings.size()) {
                workingMappings.get(selectedIndex).setSchemaVersion(newV);
                fireModified();
            }
        });
        versionRow.getChildren().addAll(versionLabel, schemaVersionComboBox);

        // Patterns toolbar & Table
        HBox patternsToolbar = new HBox(4);
        patternsToolbar.setAlignment(Pos.CENTER_LEFT);

        addPatternBtn = new Button("+");
        styleToolbarButton(addPatternBtn);
        addPatternBtn.setOnAction(e -> handleAddPatternMenu());

        removePatternBtn = new Button("-");
        styleToolbarButton(removePatternBtn);
        removePatternBtn.setOnAction(e -> handleRemovePattern());

        editPatternBtn = new Button("\u270E");
        styleToolbarButton(editPatternBtn);
        editPatternBtn.setOnAction(e -> handleEditPattern());

        patternsToolbar.getChildren().addAll(addPatternBtn, removePatternBtn, editPatternBtn);

        patternsTable = new TableView<>();
        patternsTable.setEditable(true);
        patternsTable.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        VBox.setVgrow(patternsTable, Priority.ALWAYS);

        // Placeholder for patterns table
        VBox placeholderBox = new VBox(6);
        placeholderBox.setAlignment(Pos.CENTER);
        Label pTitle = new Label("No schema mappings defined");
        pTitle.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");

        HBox linkBox = new HBox(4);
        linkBox.setAlignment(Pos.CENTER);
        Label prefixLabel = new Label("Add mapping for a ");
        prefixLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        Hyperlink fileLink = createLink("file");
        fileLink.setOnAction(e -> addPatternDialog(JsonSchemaPattern.PatternType.FILE));

        Label comma1 = new Label(", ");
        comma1.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        Hyperlink patternLink = createLink("file path pattern");
        patternLink.setOnAction(e -> addPatternDialog(JsonSchemaPattern.PatternType.PATTERN));

        Label comma2 = new Label(", ");
        comma2.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        Hyperlink dirLink = createLink("directory");
        dirLink.setOnAction(e -> addPatternDialog(JsonSchemaPattern.PatternType.DIRECTORY));

        linkBox.getChildren().addAll(prefixLabel, fileLink, comma1, patternLink, comma2, dirLink);
        placeholderBox.getChildren().addAll(pTitle, linkBox);
        patternsTable.setPlaceholder(placeholderBox);

        TableColumn<PatternRow, String> patCol = new TableColumn<>("File Path Pattern");
        patCol.setCellValueFactory(data -> data.getValue().patternProperty());
        patCol.setCellFactory(TextFieldTableCell.forTableColumn());
        patCol.setOnEditCommit(e -> {
            e.getRowValue().setPattern(e.getNewValue());
            syncPatternsToCurrentMapping();
            fireModified();
        });
        patCol.prefWidthProperty().bind(patternsTable.widthProperty().subtract(140));

        TableColumn<PatternRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> data.getValue().typeProperty());
        typeCol.setPrefWidth(120);

        patternsTable.getColumns().addAll(patCol, typeCol);

        patternsItems = FXCollections.observableArrayList();
        patternsItems.addListener((javafx.collections.ListChangeListener<PatternRow>) c -> {
            if (!updatingUi) {
                syncPatternsToCurrentMapping();
                fireModified();
            }
        });
        patternsTable.setItems(patternsItems);

        Label bottomHelpLabel = new Label("Path to file or directory relative to project root, or file name pattern like *.config.json");
        bottomHelpLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px;");

        detailPane.getChildren().addAll(
                nameRow,
                schemaRow,
                versionRow,
                patternsToolbar,
                patternsTable,
                bottomHelpLabel
        );

        splitPane.getItems().addAll(masterBox, detailPane);
        splitPane.setDividerPositions(0.28);

        getChildren().add(splitPane);
    }

    private Hyperlink createLink(String text) {
        Hyperlink link = new Hyperlink(text);
        link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        return link;
    }

    private void handleAddSchema() {
        String baseName = "New Schema";
        String candidate = baseName;
        int count = 1;
        while (nameExists(candidate)) {
            candidate = baseName + " (" + count++ + ")";
        }

        JsonSchemaMapping newMapping = new JsonSchemaMapping(candidate, "", JsonSchemaMapping.VERSION_V4, new ArrayList<>());
        workingMappings.add(newMapping);
        masterItems.add(candidate);
        masterListView.getSelectionModel().select(candidate);
        fireModified();
    }

    private boolean nameExists(String name) {
        for (JsonSchemaMapping m : workingMappings) {
            if (m.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private void handleRemoveSchema() {
        if (selectedIndex >= 0 && selectedIndex < workingMappings.size()) {
            workingMappings.remove(selectedIndex);
            masterItems.remove(selectedIndex);
            if (!workingMappings.isEmpty()) {
                int nextIndex = Math.min(selectedIndex, workingMappings.size() - 1);
                masterListView.getSelectionModel().select(nextIndex);
            } else {
                selectedIndex = -1;
                clearDetail();
            }
            fireModified();
        }
    }

    private void handleBrowseSchemaFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose JSON Schema File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Schema Files", "*.json", "*.schema.json"));
        File f = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (f != null) {
            schemaFileOrUrlField.setText(f.getAbsolutePath());
        }
    }

    private void handleFetchUrl() {
        TextInputDialog dialog = new TextInputDialog("https://json.schemastore.org/");
        dialog.setTitle("Schema URL");
        dialog.setHeaderText("Enter remote JSON Schema URL:");
        dialog.showAndWait().ifPresent(url -> {
            if (!url.trim().isEmpty()) {
                schemaFileOrUrlField.setText(url.trim());
            }
        });
    }

    private void handleAddPatternMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem fileItem = new MenuItem("File...");
        fileItem.setOnAction(e -> addPatternDialog(JsonSchemaPattern.PatternType.FILE));

        MenuItem patternItem = new MenuItem("File path pattern...");
        patternItem.setOnAction(e -> addPatternDialog(JsonSchemaPattern.PatternType.PATTERN));

        MenuItem dirItem = new MenuItem("Directory...");
        dirItem.setOnAction(e -> addPatternDialog(JsonSchemaPattern.PatternType.DIRECTORY));

        menu.getItems().addAll(fileItem, patternItem, dirItem);
        menu.show(addPatternBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void addPatternDialog(JsonSchemaPattern.PatternType type) {
        if (selectedIndex < 0 || selectedIndex >= workingMappings.size()) return;

        if (type == JsonSchemaPattern.PatternType.FILE) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select File to Map");
            File f = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                patternsItems.add(new PatternRow(f.getName(), type.getDisplayName()));
                syncPatternsToCurrentMapping();
                fireModified();
            }
        } else if (type == JsonSchemaPattern.PatternType.DIRECTORY) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Directory to Map");
            File d = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (d != null) {
                patternsItems.add(new PatternRow(d.getName(), type.getDisplayName()));
                syncPatternsToCurrentMapping();
                fireModified();
            }
        } else {
            TextInputDialog dialog = new TextInputDialog("*.config.json");
            dialog.setTitle("Add File Path Pattern");
            dialog.setHeaderText("Enter file path pattern relative to project root:");
            dialog.showAndWait().ifPresent(pat -> {
                if (!pat.trim().isEmpty()) {
                    patternsItems.add(new PatternRow(pat.trim(), type.getDisplayName()));
                    syncPatternsToCurrentMapping();
                    fireModified();
                }
            });
        }
    }

    private void handleRemovePattern() {
        PatternRow selected = patternsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            patternsItems.remove(selected);
            syncPatternsToCurrentMapping();
            fireModified();
        }
    }

    private void handleEditPattern() {
        int idx = patternsTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            patternsTable.edit(idx, patternsTable.getColumns().get(0));
        }
    }

    private void syncPatternsToCurrentMapping() {
        if (selectedIndex >= 0 && selectedIndex < workingMappings.size()) {
            List<JsonSchemaPattern> list = new ArrayList<>();
            for (PatternRow row : patternsItems) {
                JsonSchemaPattern.PatternType t = JsonSchemaPattern.PatternType.PATTERN;
                for (JsonSchemaPattern.PatternType type : JsonSchemaPattern.PatternType.values()) {
                    if (type.getDisplayName().equalsIgnoreCase(row.getType())) {
                        t = type;
                        break;
                    }
                }
                list.add(new JsonSchemaPattern(row.getPattern(), t));
            }
            workingMappings.get(selectedIndex).setPatterns(list);
        }
    }

    private void saveCurrentDetailToCache() {
        if (selectedIndex >= 0 && selectedIndex < workingMappings.size()) {
            JsonSchemaMapping cur = workingMappings.get(selectedIndex);
            cur.setName(nameField.getText());
            cur.setSchemaFileOrUrl(schemaFileOrUrlField.getText());
            cur.setSchemaVersion(schemaVersionComboBox.getValue());
            syncPatternsToCurrentMapping();
        }
    }

    private void loadDetailFromCache(int idx) {
        updatingUi = true;
        try {
            if (idx >= 0 && idx < workingMappings.size()) {
                detailPane.setDisable(false);
                JsonSchemaMapping cur = workingMappings.get(idx);
                nameField.setText(cur.getName());
                schemaFileOrUrlField.setText(cur.getSchemaFileOrUrl());
                schemaVersionComboBox.setValue(cur.getSchemaVersion());

                patternsItems.clear();
                for (JsonSchemaPattern p : cur.getPatterns()) {
                    patternsItems.add(new PatternRow(p.getPattern(), p.getType().getDisplayName()));
                }
            } else {
                clearDetail();
            }
        } finally {
            updatingUi = false;
        }
    }

    private void clearDetail() {
        detailPane.setDisable(true);
        nameField.setText("");
        schemaFileOrUrlField.setText("");
        schemaVersionComboBox.setValue(JsonSchemaMapping.VERSION_V4);
        patternsItems.clear();
    }

    private void loadDataFromSettings() {
        updatingUi = true;
        try {
            JsonSchemaMappingsSettings settings = manager.getJsonSchemaMappingsSettings();
            workingMappings.clear();
            masterItems.clear();
            for (JsonSchemaMapping m : settings.getMappings()) {
                workingMappings.add(m.copy());
                masterItems.add(m.getName());
            }
            if (!workingMappings.isEmpty()) {
                selectedIndex = 0;
                masterListView.getSelectionModel().select(0);
                loadDetailFromCache(0);
            } else {
                selectedIndex = -1;
                clearDetail();
            }
        } finally {
            updatingUi = false;
        }
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-padding: 4 8;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4;");
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand; -fx-font-weight: bold;");
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void takeSnapshot() {
        this.initialSettings = getCurrentUiSettings();
    }

    public JsonSchemaMappingsSettings getCurrentUiSettings() {
        saveCurrentDetailToCache();
        List<JsonSchemaMapping> list = new ArrayList<>();
        for (JsonSchemaMapping m : workingMappings) {
            list.add(m.copy());
        }
        return new JsonSchemaMappingsSettings(list);
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentUiSettings());
    }

    public void apply() {
        manager.setJsonSchemaMappingsSettings(getCurrentUiSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        loadDataFromSettings();
        takeSnapshot();
        fireModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            applySettingsToUi(initialSettings);
            fireModified();
        }
    }

    public void resetDefaults() {
        applySettingsToUi(new JsonSchemaMappingsSettings());
        fireModified();
    }

    private void applySettingsToUi(JsonSchemaMappingsSettings s) {
        if (s == null) return;
        updatingUi = true;
        try {
            workingMappings.clear();
            masterItems.clear();
            for (JsonSchemaMapping m : s.getMappings()) {
                workingMappings.add(m.copy());
                masterItems.add(m.getName());
            }
            if (!workingMappings.isEmpty()) {
                selectedIndex = 0;
                masterListView.getSelectionModel().select(0);
                loadDetailFromCache(0);
            } else {
                selectedIndex = -1;
                clearDetail();
            }
        } finally {
            updatingUi = false;
        }
    }

    // Direct UI accessors for tests
    public ListView<String> getMasterListView() {
        return masterListView;
    }

    public ObservableList<String> getMasterItems() {
        return masterItems;
    }

    public TextField getNameField() {
        return nameField;
    }

    public TextField getSchemaFileOrUrlField() {
        return schemaFileOrUrlField;
    }

    public ComboBox<String> getSchemaVersionComboBox() {
        return schemaVersionComboBox;
    }

    public TableView<PatternRow> getPatternsTable() {
        return patternsTable;
    }

    public ObservableList<PatternRow> getPatternsItems() {
        return patternsItems;
    }

    public Button getAddMappingBtn() {
        return addMappingBtn;
    }

    public Button getRemoveMappingBtn() {
        return removeMappingBtn;
    }
}
