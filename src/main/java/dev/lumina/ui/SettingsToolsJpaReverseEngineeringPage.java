package dev.lumina.ui;

import dev.lumina.tools.JpaReverseEngineeringSettings;
import dev.lumina.tools.JpaReverseEngineeringSettingsManager;
import dev.lumina.tools.JpaTypeMappingEntry;
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
 * Tools > JPA Reverse Engineering settings page matching Image 2.
 */
public class SettingsToolsJpaReverseEngineeringPage extends VBox {

    private final JpaReverseEngineeringSettingsManager manager;
    private JpaReverseEngineeringSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Top checkboxes
    private CheckBox fetchTypeLazyCheck;
    private CheckBox validationAnnotationsCheck;
    private CheckBox singularClassNameCheck;
    private CheckBox replaceOrmBasicTypesCheck;

    // Table & Column Comments
    private ToggleGroup commentsGroup;
    private RadioButton commentAnnotationRadio;
    private RadioButton javaDocRadio;
    private RadioButton ignoreCommentsRadio;

    // Naming Rules
    private ToggleGroup namingRulesGroup;
    private RadioButton configsRadio;
    private RadioButton algorithmRadio;

    private TextField prefixesTableField;
    private TextField prefixesColumnField;
    private TextField suffixesTableField;
    private TextField suffixesColumnField;
    private TextField reservedKeywordSuffixField;
    private VBox configsBox;

    // Mapping Types
    private ListView<String> dbEnginesList;
    private TableView<JpaTypeMappingEntry> mappingsTable;
    private ObservableList<JpaTypeMappingEntry> mappingsItems;
    private Button addMappingBtn;
    private Button removeMappingBtn;
    private Button editMappingBtn;

    public SettingsToolsJpaReverseEngineeringPage() {
        this.manager = JpaReverseEngineeringSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Top Checkboxes
        fetchTypeLazyCheck = createCheckBox("Use FetchType.LAZY for @OneToOne and @ManyToOne associations");
        validationAnnotationsCheck = createCheckBox("Use validation annotations (NotNull, Size, etc...)");
        singularClassNameCheck = createCheckBox("Convert the table name to a singular form to generate the class name");
        replaceOrmBasicTypesCheck = createCheckBox("Replace ORM references with basic type attributes");

        VBox topBox = new VBox(6,
                fetchTypeLazyCheck,
                validationAnnotationsCheck,
                singularClassNameCheck,
                replaceOrmBasicTypesCheck
        );

        // 2. Table & Column Comments
        Label commentsHeader = createSectionTitle("Table & Column Comments");
        HBox commentsRow = new HBox(16);
        commentsRow.setAlignment(Pos.CENTER_LEFT);
        Label addAsLabel = createLabel("Add as:");
        commentsGroup = new ToggleGroup();
        commentAnnotationRadio = createRadioButton("@Comment annotation", commentsGroup);
        javaDocRadio = createRadioButton("Java Doc", commentsGroup);
        ignoreCommentsRadio = createRadioButton("Ignore", commentsGroup);
        commentsRow.getChildren().addAll(addAsLabel, commentAnnotationRadio, javaDocRadio, ignoreCommentsRadio);

        VBox commentsSection = new VBox(8, commentsHeader, commentsRow);

        // 3. Naming Rules
        Label namingRulesHeader = createSectionTitle("Naming Rules");
        HBox rulesModeRow = new HBox(16);
        rulesModeRow.setAlignment(Pos.CENTER_LEFT);
        namingRulesGroup = new ToggleGroup();
        configsRadio = createRadioButton("Configs", namingRulesGroup);
        algorithmRadio = createRadioButton("Algorithm", namingRulesGroup);
        rulesModeRow.getChildren().addAll(configsRadio, algorithmRadio);

        prefixesTableField = createStyledTextField("e.g. sys_, sec_, report_");
        prefixesColumnField = createStyledTextField("e.g. d_, t_");
        suffixesTableField = createStyledTextField("e.g. _sys, _sec, _report");
        suffixesColumnField = createStyledTextField("e.g. _d, _t");
        reservedKeywordSuffixField = createStyledTextField("");
        reservedKeywordSuffixField.setText("Field");

        configsBox = new VBox(8);
        configsBox.getChildren().addAll(
                createExpandableFieldRow("Prefixes to skip in table name:", prefixesTableField, "Prefixes to strip from table names"),
                createExpandableFieldRow("Prefixes to skip in column name:", prefixesColumnField, "Prefixes to strip from column names"),
                createExpandableFieldRow("Suffixes to skip in table name:", suffixesTableField, "Suffixes to strip from table names"),
                createExpandableFieldRow("Suffixes to skip in column name:", suffixesColumnField, "Suffixes to strip from column names"),
                createLabelAndFieldRow("Reserved keyword field suffix:", reservedKeywordSuffixField)
        );

        VBox namingSection = new VBox(8, namingRulesHeader, rulesModeRow, configsBox);

        // 4. Mapping Types
        Label mappingTypesHeader = createSectionTitle("Mapping Types");

        HBox mappingPane = new HBox(10);
        mappingPane.setPrefHeight(180);
        mappingPane.setMinHeight(160);

        // Left list: database engines
        dbEnginesList = new ListView<>();
        dbEnginesList.setPrefWidth(160);
        dbEnginesList.getItems().addAll("mysql", "mariadb", "postgresql", "mssql", "oracle", "h2", "db2", "hsqldb");
        dbEnginesList.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        dbEnginesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-padding: 4 8; -fx-font-size: 13px;");
                }
            }
        });

        // Right pane: Table with toolbar
        VBox rightMappingBox = new VBox(4);
        HBox.setHgrow(rightMappingBox, Priority.ALWAYS);

        HBox toolbar = new HBox(6);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        addMappingBtn = createToolbarButton("+");
        removeMappingBtn = createToolbarButton("—");
        editMappingBtn = createToolbarButton("✎");
        toolbar.getChildren().addAll(addMappingBtn, removeMappingBtn, editMappingBtn);

        mappingsTable = new TableView<>();
        mappingsTable.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(mappingsTable, Priority.ALWAYS);

        mappingsItems = FXCollections.observableArrayList();
        mappingsTable.setItems(mappingsItems);

        TableColumn<JpaTypeMappingEntry, String> sqlCol = new TableColumn<>("SQL Type");
        sqlCol.setPrefWidth(220);
        sqlCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSqlType()));

        TableColumn<JpaTypeMappingEntry, String> targetCol = new TableColumn<>("Attribute/Converter/Hibernate Type");
        targetCol.setPrefWidth(280);
        targetCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTargetType()));

        mappingsTable.getColumns().addAll(sqlCol, targetCol);

        Label emptyLabel = new Label("No override mappings");
        emptyLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        mappingsTable.setPlaceholder(emptyLabel);

        rightMappingBox.getChildren().addAll(toolbar, mappingsTable);
        mappingPane.getChildren().addAll(dbEnginesList, rightMappingBox);

        VBox mappingSection = new VBox(8, mappingTypesHeader, mappingPane);

        getChildren().addAll(topBox, commentsSection, namingSection, mappingSection);

        setupListeners();
    }

    private HBox createExpandableFieldRow(String labelText, TextField tf, String title) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label l = createLabel(labelText);
        l.setPrefWidth(200);

        Button expandBtn = new Button("⤢");
        expandBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C9099; -fx-border-color: transparent; -fx-cursor: hand; -fx-font-size: 13px;");
        expandBtn.setOnAction(e -> openTextEditorDialog(title, tf));

        Label helpBtn = new Label("?");
        helpBtn.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px; -fx-padding: 2 5; -fx-border-color: #5A5D63; -fx-border-radius: 10; -fx-cursor: hand;");
        Tooltip.install(helpBtn, new Tooltip("Patterns or values separated by commas"));

        HBox.setHgrow(tf, Priority.ALWAYS);
        row.getChildren().addAll(l, tf, expandBtn, helpBtn);
        return row;
    }

    private HBox createLabelAndFieldRow(String labelText, TextField tf) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label l = createLabel(labelText);
        l.setPrefWidth(200);

        HBox.setHgrow(tf, Priority.ALWAYS);
        row.getChildren().addAll(l, tf);
        return row;
    }

    private void openTextEditorDialog(String title, TextField targetField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Edit entries (comma-separated):");

        ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        TextArea textArea = new TextArea(targetField.getText());
        textArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-family: monospace;");
        textArea.setPrefRowCount(8);
        textArea.setPrefColumnCount(30);

        VBox content = new VBox(textArea);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> btn == okBtn ? textArea.getText() : null);
        dialog.showAndWait().ifPresent(text -> {
            targetField.setText(text.trim());
            notifyModified();
        });
    }

    private void setupListeners() {
        fetchTypeLazyCheck.setOnAction(e -> notifyModified());
        validationAnnotationsCheck.setOnAction(e -> notifyModified());
        singularClassNameCheck.setOnAction(e -> notifyModified());
        replaceOrmBasicTypesCheck.setOnAction(e -> notifyModified());

        commentAnnotationRadio.setOnAction(e -> notifyModified());
        javaDocRadio.setOnAction(e -> notifyModified());
        ignoreCommentsRadio.setOnAction(e -> notifyModified());

        configsRadio.setOnAction(e -> {
            configsBox.setDisable(false);
            notifyModified();
        });
        algorithmRadio.setOnAction(e -> {
            configsBox.setDisable(true);
            notifyModified();
        });

        prefixesTableField.textProperty().addListener((obs, oldV, newV) -> notifyModified());
        prefixesColumnField.textProperty().addListener((obs, oldV, newV) -> notifyModified());
        suffixesTableField.textProperty().addListener((obs, oldV, newV) -> notifyModified());
        suffixesColumnField.textProperty().addListener((obs, oldV, newV) -> notifyModified());
        reservedKeywordSuffixField.textProperty().addListener((obs, oldV, newV) -> notifyModified());

        dbEnginesList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                filterMappingsForEngine(newV);
            }
        });

        addMappingBtn.setOnAction(e -> openAddMappingDialog());
        removeMappingBtn.setOnAction(e -> {
            JpaTypeMappingEntry sel = mappingsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                mappingsItems.remove(sel);
                notifyModified();
            }
        });
        editMappingBtn.setOnAction(e -> {
            JpaTypeMappingEntry sel = mappingsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                openEditMappingDialog(sel);
            }
        });
    }

    private void filterMappingsForEngine(String engine) {
        mappingsTable.refresh();
    }

    private void openAddMappingDialog() {
        String currentEngine = dbEnginesList.getSelectionModel().getSelectedItem();
        if (currentEngine == null) currentEngine = "mysql";

        Dialog<JpaTypeMappingEntry> dialog = new Dialog<>();
        dialog.setTitle("Add Type Mapping");
        dialog.setHeaderText("Add SQL to Java/Hibernate Type Mapping:");

        ButtonType okBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        TextField sqlTf = createStyledTextField("e.g. VARCHAR(255)");
        TextField tgtTf = createStyledTextField("e.g. java.lang.String");

        VBox box = new VBox(10);
        box.getChildren().addAll(
                new Label("SQL Type:"), sqlTf,
                new Label("Attribute/Converter/Hibernate Type:"), tgtTf
        );
        dialog.getDialogPane().setContent(box);

        final String eng = currentEngine;
        dialog.setResultConverter(btn -> btn == okBtn ? new JpaTypeMappingEntry(eng, sqlTf.getText().trim(), tgtTf.getText().trim()) : null);
        dialog.showAndWait().ifPresent(entry -> {
            mappingsItems.add(entry);
            notifyModified();
        });
    }

    private void openEditMappingDialog(JpaTypeMappingEntry entry) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Type Mapping");

        ButtonType okBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        TextField sqlTf = createStyledTextField("");
        sqlTf.setText(entry.getSqlType());
        TextField tgtTf = createStyledTextField("");
        tgtTf.setText(entry.getTargetType());

        VBox box = new VBox(10);
        box.getChildren().addAll(
                new Label("SQL Type:"), sqlTf,
                new Label("Attribute/Converter/Hibernate Type:"), tgtTf
        );
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> btn == okBtn);
        dialog.showAndWait().ifPresent(ok -> {
            if (ok) {
                entry.setSqlType(sqlTf.getText().trim());
                entry.setTargetType(tgtTf.getText().trim());
                mappingsTable.refresh();
                notifyModified();
            }
        });
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return cb;
    }

    private RadioButton createRadioButton(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return rb;
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private Label createSectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return l;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #5A5D63; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
        return tf;
    }

    private Button createToolbarButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-border-color: transparent; " +
                "-fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 2 8;");
        return b;
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(JpaReverseEngineeringSettings s) {
        if (s == null) return;
        fetchTypeLazyCheck.setSelected(s.isUseFetchTypeLazy());
        validationAnnotationsCheck.setSelected(s.isUseValidationAnnotations());
        singularClassNameCheck.setSelected(s.isConvertTableNameToSingular());
        replaceOrmBasicTypesCheck.setSelected(s.isReplaceOrmReferencesWithBasicTypes());

        String comm = s.getTableAndColumnComments();
        if ("@Comment annotation".equals(comm)) {
            commentAnnotationRadio.setSelected(true);
        } else if ("Java Doc".equals(comm)) {
            javaDocRadio.setSelected(true);
        } else {
            ignoreCommentsRadio.setSelected(true);
        }

        if ("Algorithm".equalsIgnoreCase(s.getNamingRulesMode())) {
            algorithmRadio.setSelected(true);
            configsBox.setDisable(true);
        } else {
            configsRadio.setSelected(true);
            configsBox.setDisable(false);
        }

        prefixesTableField.setText(s.getPrefixesToSkipInTableName());
        prefixesColumnField.setText(s.getPrefixesToSkipInColumnName());
        suffixesTableField.setText(s.getSuffixesToSkipInTableName());
        suffixesColumnField.setText(s.getSuffixesToSkipInColumnName());
        reservedKeywordSuffixField.setText(s.getReservedKeywordFieldSuffix());

        dbEnginesList.getSelectionModel().select(s.getSelectedDatabaseEngine());

        mappingsItems.clear();
        for (JpaTypeMappingEntry e : s.getTypeMappings()) {
            mappingsItems.add(e.copy());
        }
    }

    private JpaReverseEngineeringSettings getCurrentSettingsFromUI() {
        JpaReverseEngineeringSettings s = new JpaReverseEngineeringSettings();
        s.setUseFetchTypeLazy(fetchTypeLazyCheck.isSelected());
        s.setUseValidationAnnotations(validationAnnotationsCheck.isSelected());
        s.setConvertTableNameToSingular(singularClassNameCheck.isSelected());
        s.setReplaceOrmReferencesWithBasicTypes(replaceOrmBasicTypesCheck.isSelected());

        if (commentAnnotationRadio.isSelected()) {
            s.setTableAndColumnComments("@Comment annotation");
        } else if (javaDocRadio.isSelected()) {
            s.setTableAndColumnComments("Java Doc");
        } else {
            s.setTableAndColumnComments("Ignore");
        }

        s.setNamingRulesMode(algorithmRadio.isSelected() ? "Algorithm" : "Configs");
        s.setPrefixesToSkipInTableName(prefixesTableField.getText().trim());
        s.setPrefixesToSkipInColumnName(prefixesColumnField.getText().trim());
        s.setSuffixesToSkipInTableName(suffixesTableField.getText().trim());
        s.setSuffixesToSkipInColumnName(suffixesColumnField.getText().trim());
        s.setReservedKeywordFieldSuffix(reservedKeywordSuffixField.getText().trim());

        String selEngine = dbEnginesList.getSelectionModel().getSelectedItem();
        s.setSelectedDatabaseEngine(selEngine != null ? selEngine : "mysql");

        List<JpaTypeMappingEntry> list = new ArrayList<>();
        for (JpaTypeMappingEntry e : mappingsItems) {
            list.add(e.copy());
        }
        s.setTypeMappings(list);

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        JpaReverseEngineeringSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void resetDefaults() {
        applySettingsToUI(new JpaReverseEngineeringSettings());
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Accessors for testing
    public CheckBox getFetchTypeLazyCheck() {
        return fetchTypeLazyCheck;
    }

    public CheckBox getValidationAnnotationsCheck() {
        return validationAnnotationsCheck;
    }

    public CheckBox getSingularClassNameCheck() {
        return singularClassNameCheck;
    }

    public CheckBox getReplaceOrmBasicTypesCheck() {
        return replaceOrmBasicTypesCheck;
    }

    public RadioButton getCommentAnnotationRadio() {
        return commentAnnotationRadio;
    }

    public RadioButton getIgnoreCommentsRadio() {
        return ignoreCommentsRadio;
    }

    public RadioButton getConfigsRadio() {
        return configsRadio;
    }

    public RadioButton getAlgorithmRadio() {
        return algorithmRadio;
    }

    public TextField getPrefixesTableField() {
        return prefixesTableField;
    }

    public TextField getPrefixesColumnField() {
        return prefixesColumnField;
    }

    public TextField getReservedKeywordSuffixField() {
        return reservedKeywordSuffixField;
    }

    public ListView<String> getDbEnginesList() {
        return dbEnginesList;
    }

    public TableView<JpaTypeMappingEntry> getMappingsTable() {
        return mappingsTable;
    }

    public ObservableList<JpaTypeMappingEntry> getMappingsItems() {
        return mappingsItems;
    }

    public Button getAddMappingBtn() {
        return addMappingBtn;
    }
}
