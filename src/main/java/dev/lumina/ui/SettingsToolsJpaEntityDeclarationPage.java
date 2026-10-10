package dev.lumina.ui;

import dev.lumina.tools.JpaEntityDeclarationSettings;
import dev.lumina.tools.JpaEntityDeclarationSettingsManager;
import dev.lumina.tools.JpaNameTemplateEntry;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tools > JPA Entity Declaration settings page matching Image 1.
 */
public class SettingsToolsJpaEntityDeclarationPage extends VBox {

    private final JpaEntityDeclarationSettingsManager manager;
    private JpaEntityDeclarationSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Top checkboxes
    private CheckBox annotationsOnGettersCheck;
    private CheckBox serialVersionUidCheck;
    private CheckBox hibernateCustomTypesCheck;
    private CheckBox fetchTypeLazyCheck;
    private CheckBox returnThisSettersCheck;
    private ComboBox<String> scaffoldingLangCombo;
    private RadioButton privateAccessRadio;
    private RadioButton protectedAccessRadio;
    private ToggleGroup accessModifierGroup;

    // Name Templates
    private TableView<JpaNameTemplateEntry> templateTable;
    private ObservableList<JpaNameTemplateEntry> templateItems;
    private ComboBox<String> indexNamesCaseCombo;

    // Lombok
    private CheckBox lombokGetterSetterCheck;
    private CheckBox lombokBuilderCheck;
    private CheckBox lombokAllArgsCheck;
    private CheckBox lombokNoArgsCheck;
    private CheckBox lombokToStringCheck;
    private CheckBox lombokToStringExplicitCheck;

    // Constants Generation
    private CheckBox constantsGenerateCheck;
    private CheckBox constantsEntityNameCheck;
    private CheckBox constantsTableNameCheck;
    private CheckBox constantsColumnNameCheck;
    private ComboBox<String> constantsPlacementCombo;
    private VBox constantsContainer;

    public SettingsToolsJpaEntityDeclarationPage() {
        this.manager = JpaEntityDeclarationSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Top Options
        annotationsOnGettersCheck = createCheckBox("Generate JPA annotations on getter method:");
        serialVersionUidCheck = createCheckBox("Generate Serial Version UID field:");
        hibernateCustomTypesCheck = createCheckBox("Register hibernate custom types on entity:");
        fetchTypeLazyCheck = createCheckBox("Use FetchType.LAZY for @OneToOne and @ManyToOne associations:");
        returnThisSettersCheck = createCheckBox("Generate \"return this;\" in attribute setters:");

        // Scaffolding language row
        HBox scaffoldRow = new HBox(12);
        scaffoldRow.setAlignment(Pos.CENTER_LEFT);
        Label scaffoldLabel = createLabel("Scaffolding language:");
        scaffoldingLangCombo = new ComboBox<>(FXCollections.observableArrayList("Always Ask", "Java", "Kotlin"));
        styleComboBox(scaffoldingLangCombo);
        scaffoldingLangCombo.setPrefWidth(160);
        scaffoldRow.getChildren().addAll(scaffoldLabel, scaffoldingLangCombo);

        // Default entity attribute access modifier row
        HBox accessRow = new HBox(12);
        accessRow.setAlignment(Pos.CENTER_LEFT);
        Label accessLabel = createLabel("Default entity attribute access modifier:");
        accessModifierGroup = new ToggleGroup();
        privateAccessRadio = createRadioButton("Private", accessModifierGroup);
        protectedAccessRadio = createRadioButton("Protected", accessModifierGroup);
        accessRow.getChildren().addAll(accessLabel, privateAccessRadio, protectedAccessRadio);

        VBox topBox = new VBox(8);
        topBox.getChildren().addAll(
                annotationsOnGettersCheck,
                serialVersionUidCheck,
                hibernateCustomTypesCheck,
                fetchTypeLazyCheck,
                returnThisSettersCheck,
                scaffoldRow,
                accessRow
        );

        // 2. Name Templates Section
        HBox templatesHeader = createDividerHeader("Name Templates");

        templateTable = new TableView<>();
        templateTable.setEditable(true);
        templateTable.setPrefHeight(125);
        templateTable.setMinHeight(115);
        templateTable.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        templateItems = FXCollections.observableArrayList();
        templateTable.setItems(templateItems);

        // Checkbox column
        TableColumn<JpaNameTemplateEntry, Boolean> enableCol = new TableColumn<>("");
        enableCol.setPrefWidth(35);
        enableCol.setCellValueFactory(p -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(p.getValue().isEnabled());
            prop.addListener((obs, oldV, newV) -> {
                p.getValue().setEnabled(newV);
                notifyModified();
            });
            return prop;
        });
        enableCol.setCellFactory(CheckBoxTableCell.forTableColumn(enableCol));

        // Target column
        TableColumn<JpaNameTemplateEntry, String> targetCol = new TableColumn<>("Target");
        targetCol.setPrefWidth(100);
        targetCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTarget()));

        // Case column
        TableColumn<JpaNameTemplateEntry, String> caseCol = new TableColumn<>("Case");
        caseCol.setPrefWidth(100);
        caseCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCaseFormat()));

        // Prefix column
        TableColumn<JpaNameTemplateEntry, String> prefixCol = new TableColumn<>("Prefix");
        prefixCol.setPrefWidth(120);
        prefixCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getPrefix()));
        prefixCol.setCellFactory(TextFieldTableCell.forTableColumn());
        prefixCol.setOnEditCommit(e -> {
            e.getRowValue().setPrefix(e.getNewValue());
            notifyModified();
        });

        // Postfix column
        TableColumn<JpaNameTemplateEntry, String> postfixCol = new TableColumn<>("Postfix");
        postfixCol.setPrefWidth(120);
        postfixCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getPostfix()));
        postfixCol.setCellFactory(TextFieldTableCell.forTableColumn());
        postfixCol.setOnEditCommit(e -> {
            e.getRowValue().setPostfix(e.getNewValue());
            notifyModified();
        });

        // Underscore column
        TableColumn<JpaNameTemplateEntry, Boolean> underscoreCol = new TableColumn<>("Underscore");
        underscoreCol.setPrefWidth(90);
        underscoreCol.setCellValueFactory(p -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(p.getValue().isUnderscore());
            prop.addListener((obs, oldV, newV) -> {
                p.getValue().setUnderscore(newV);
                notifyModified();
            });
            return prop;
        });
        underscoreCol.setCellFactory(CheckBoxTableCell.forTableColumn(underscoreCol));

        // Pluralize column
        TableColumn<JpaNameTemplateEntry, Boolean> pluralizeCol = new TableColumn<>("Pluralize");
        pluralizeCol.setPrefWidth(80);
        pluralizeCol.setCellValueFactory(p -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(p.getValue().isPluralize());
            prop.addListener((obs, oldV, newV) -> {
                p.getValue().setPluralize(newV);
                notifyModified();
            });
            return prop;
        });
        pluralizeCol.setCellFactory(CheckBoxTableCell.forTableColumn(pluralizeCol));

        templateTable.getColumns().addAll(enableCol, targetCol, caseCol, prefixCol, postfixCol, underscoreCol, pluralizeCol);

        // Index names case row
        HBox indexCaseRow = new HBox(12);
        indexCaseRow.setAlignment(Pos.CENTER_LEFT);
        Label indexCaseLabel = createLabel("Index/constraint names case:");
        indexNamesCaseCombo = new ComboBox<>(FXCollections.observableArrayList("lower", "UPPER", "mixed"));
        styleComboBox(indexNamesCaseCombo);
        indexNamesCaseCombo.setPrefWidth(110);
        indexCaseRow.getChildren().addAll(indexCaseLabel, indexNamesCaseCombo);

        VBox templateSection = new VBox(8, templatesHeader, templateTable, indexCaseRow);

        // 3. Lombok Section
        HBox lombokHeader = createDividerHeader("Lombok");
        lombokGetterSetterCheck = createCheckBox("Generate @Getter and @Setter:");
        lombokBuilderCheck = createCheckBox("Generate @Builder:");
        lombokAllArgsCheck = createCheckBox("Generate @AllArgsConstructor:");
        lombokNoArgsCheck = createCheckBox("Generate @NoArgsConstructor:");
        lombokToStringCheck = createCheckBox("Generate @ToString:");
        lombokToStringExplicitCheck = createCheckBox("Generate @ToString with onlyExplicitlyIncluded = true:");

        VBox lombokSection = new VBox(8,
                lombokHeader,
                lombokGetterSetterCheck,
                lombokBuilderCheck,
                lombokAllArgsCheck,
                lombokNoArgsCheck,
                lombokToStringCheck,
                lombokToStringExplicitCheck
        );

        // 4. Constants Generation Section
        HBox constantsHeader = createDividerHeader("Constants Generation");
        constantsGenerateCheck = createCheckBox("Generate constants for new object names:");

        constantsEntityNameCheck = createCheckBox("Entity name:");
        constantsTableNameCheck = createCheckBox("Table name:");
        constantsColumnNameCheck = createCheckBox("Column name:");

        HBox placeRow = new HBox(12);
        placeRow.setAlignment(Pos.CENTER_LEFT);
        Label placeLabel = createLabel("Where to place constants:");
        constantsPlacementCombo = new ComboBox<>(FXCollections.observableArrayList("Same class", "Separate interface", "Separate class"));
        styleComboBox(constantsPlacementCombo);
        constantsPlacementCombo.setPrefWidth(180);
        placeRow.getChildren().addAll(placeLabel, constantsPlacementCombo);

        constantsContainer = new VBox(6, constantsEntityNameCheck, constantsTableNameCheck, constantsColumnNameCheck, placeRow);
        constantsContainer.setPadding(new Insets(2, 0, 0, 16));

        VBox constantsSection = new VBox(8, constantsHeader, constantsGenerateCheck, constantsContainer);

        getChildren().addAll(topBox, templateSection, lombokSection, constantsSection);

        // Event wiring
        setupListeners();
    }

    private void setupListeners() {
        annotationsOnGettersCheck.setOnAction(e -> notifyModified());
        serialVersionUidCheck.setOnAction(e -> notifyModified());
        hibernateCustomTypesCheck.setOnAction(e -> notifyModified());
        fetchTypeLazyCheck.setOnAction(e -> notifyModified());
        returnThisSettersCheck.setOnAction(e -> notifyModified());
        scaffoldingLangCombo.setOnAction(e -> notifyModified());
        privateAccessRadio.setOnAction(e -> notifyModified());
        protectedAccessRadio.setOnAction(e -> notifyModified());

        indexNamesCaseCombo.setOnAction(e -> notifyModified());

        lombokGetterSetterCheck.setOnAction(e -> notifyModified());
        lombokBuilderCheck.setOnAction(e -> notifyModified());
        lombokAllArgsCheck.setOnAction(e -> notifyModified());
        lombokNoArgsCheck.setOnAction(e -> notifyModified());
        lombokToStringCheck.setOnAction(e -> notifyModified());
        lombokToStringExplicitCheck.setOnAction(e -> notifyModified());

        constantsGenerateCheck.setOnAction(e -> {
            updateConstantsState();
            notifyModified();
        });
        constantsEntityNameCheck.setOnAction(e -> notifyModified());
        constantsTableNameCheck.setOnAction(e -> notifyModified());
        constantsColumnNameCheck.setOnAction(e -> notifyModified());
        constantsPlacementCombo.setOnAction(e -> notifyModified());
    }

    private void updateConstantsState() {
        boolean enabled = constantsGenerateCheck.isSelected();
        constantsContainer.setDisable(!enabled);
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

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private HBox createDividerHeader(String text) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 4, 0));

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        header.getChildren().addAll(label, sep);
        return header;
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

    private void applySettingsToUI(JpaEntityDeclarationSettings s) {
        if (s == null) return;
        annotationsOnGettersCheck.setSelected(s.isGenerateJpaAnnotationsOnGetterMethod());
        serialVersionUidCheck.setSelected(s.isGenerateSerialVersionUidField());
        hibernateCustomTypesCheck.setSelected(s.isRegisterHibernateCustomTypesOnEntity());
        fetchTypeLazyCheck.setSelected(s.isUseFetchTypeLazy());
        returnThisSettersCheck.setSelected(s.isGenerateReturnThisInAttributeSetters());

        scaffoldingLangCombo.setValue(s.getScaffoldingLanguage());
        if ("Protected".equalsIgnoreCase(s.getDefaultEntityAttributeAccessModifier())) {
            protectedAccessRadio.setSelected(true);
        } else {
            privateAccessRadio.setSelected(true);
        }

        templateItems.clear();
        for (JpaNameTemplateEntry e : s.getNameTemplates()) {
            templateItems.add(e.copy());
        }

        indexNamesCaseCombo.setValue(s.getIndexConstraintNamesCase());

        lombokGetterSetterCheck.setSelected(s.isLombokGenerateGetterAndSetter());
        lombokBuilderCheck.setSelected(s.isLombokGenerateBuilder());
        lombokAllArgsCheck.setSelected(s.isLombokGenerateAllArgsConstructor());
        lombokNoArgsCheck.setSelected(s.isLombokGenerateNoArgsConstructor());
        lombokToStringCheck.setSelected(s.isLombokGenerateToString());
        lombokToStringExplicitCheck.setSelected(s.isLombokGenerateToStringWithOnlyExplicitlyIncluded());

        constantsGenerateCheck.setSelected(s.isGenerateConstantsForNewObjectNames());
        constantsEntityNameCheck.setSelected(s.isConstantsEntityName());
        constantsTableNameCheck.setSelected(s.isConstantsTableName());
        constantsColumnNameCheck.setSelected(s.isConstantsColumnName());
        constantsPlacementCombo.setValue(s.getConstantsPlacement());

        updateConstantsState();
    }

    private JpaEntityDeclarationSettings getCurrentSettingsFromUI() {
        JpaEntityDeclarationSettings s = new JpaEntityDeclarationSettings();
        s.setGenerateJpaAnnotationsOnGetterMethod(annotationsOnGettersCheck.isSelected());
        s.setGenerateSerialVersionUidField(serialVersionUidCheck.isSelected());
        s.setRegisterHibernateCustomTypesOnEntity(hibernateCustomTypesCheck.isSelected());
        s.setUseFetchTypeLazy(fetchTypeLazyCheck.isSelected());
        s.setGenerateReturnThisInAttributeSetters(returnThisSettersCheck.isSelected());

        s.setScaffoldingLanguage(scaffoldingLangCombo.getValue());
        s.setDefaultEntityAttributeAccessModifier(protectedAccessRadio.isSelected() ? "Protected" : "Private");

        List<JpaNameTemplateEntry> list = new ArrayList<>();
        for (JpaNameTemplateEntry e : templateItems) {
            list.add(e.copy());
        }
        s.setNameTemplates(list);

        s.setIndexConstraintNamesCase(indexNamesCaseCombo.getValue());

        s.setLombokGenerateGetterAndSetter(lombokGetterSetterCheck.isSelected());
        s.setLombokGenerateBuilder(lombokBuilderCheck.isSelected());
        s.setLombokGenerateAllArgsConstructor(lombokAllArgsCheck.isSelected());
        s.setLombokGenerateNoArgsConstructor(lombokNoArgsCheck.isSelected());
        s.setLombokGenerateToString(lombokToStringCheck.isSelected());
        s.setLombokGenerateToStringWithOnlyExplicitlyIncluded(lombokToStringExplicitCheck.isSelected());

        s.setGenerateConstantsForNewObjectNames(constantsGenerateCheck.isSelected());
        s.setConstantsEntityName(constantsEntityNameCheck.isSelected());
        s.setConstantsTableName(constantsTableNameCheck.isSelected());
        s.setConstantsColumnName(constantsColumnNameCheck.isSelected());
        s.setConstantsPlacement(constantsPlacementCombo.getValue());

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        JpaEntityDeclarationSettings current = getCurrentSettingsFromUI();
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
        applySettingsToUI(new JpaEntityDeclarationSettings());
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
    public CheckBox getAnnotationsOnGettersCheck() {
        return annotationsOnGettersCheck;
    }

    public CheckBox getSerialVersionUidCheck() {
        return serialVersionUidCheck;
    }

    public CheckBox getHibernateCustomTypesCheck() {
        return hibernateCustomTypesCheck;
    }

    public CheckBox getFetchTypeLazyCheck() {
        return fetchTypeLazyCheck;
    }

    public CheckBox getReturnThisSettersCheck() {
        return returnThisSettersCheck;
    }

    public ComboBox<String> getScaffoldingLangCombo() {
        return scaffoldingLangCombo;
    }

    public RadioButton getPrivateAccessRadio() {
        return privateAccessRadio;
    }

    public RadioButton getProtectedAccessRadio() {
        return protectedAccessRadio;
    }

    public TableView<JpaNameTemplateEntry> getTemplateTable() {
        return templateTable;
    }

    public ObservableList<JpaNameTemplateEntry> getTemplateItems() {
        return templateItems;
    }

    public ComboBox<String> getIndexNamesCaseCombo() {
        return indexNamesCaseCombo;
    }

    public CheckBox getLombokGetterSetterCheck() {
        return lombokGetterSetterCheck;
    }

    public CheckBox getLombokBuilderCheck() {
        return lombokBuilderCheck;
    }

    public CheckBox getConstantsGenerateCheck() {
        return constantsGenerateCheck;
    }

    public CheckBox getConstantsEntityNameCheck() {
        return constantsEntityNameCheck;
    }

    public ComboBox<String> getConstantsPlacementCombo() {
        return constantsPlacementCombo;
    }
}
