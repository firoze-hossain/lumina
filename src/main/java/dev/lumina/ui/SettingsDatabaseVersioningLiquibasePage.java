package dev.lumina.ui;

import dev.lumina.database.versioning.DatabaseLiquibaseSettings;
import dev.lumina.database.versioning.DatabaseLiquibaseSettingsManager;
import dev.lumina.database.versioning.LiquibaseChangesetTemplateItem;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.*;

/**
 * Tools > Database Versioning > Liquibase settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsDatabaseVersioningLiquibasePage extends VBox {

    private final DatabaseLiquibaseSettingsManager manager;
    private DatabaseLiquibaseSettings initialSettings;
    private DatabaseLiquibaseSettings currentSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private TextField liquibaseVersionField;
    private TextField changesetAuthorField;
    private ComboBox<String> fileTypeCombo;
    private CheckBox addEmptyRollbackCheck;

    private TextField primaryDirectoryField;
    private TextField primaryNameField;
    private TextField secondaryDirectoryField;
    private TextField secondaryNameField;

    private final Map<String, CheckBox> dbTypeCheckBoxes = new LinkedHashMap<>();

    private TableView<LiquibaseChangesetTemplateItem> templatesTable;
    private ObservableList<LiquibaseChangesetTemplateItem> templatesData;

    public SettingsDatabaseVersioningLiquibasePage() {
        this.manager = DatabaseLiquibaseSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Liquibase version
        Label versionLabel = createFieldLabel("Liquibase version:", 140);
        liquibaseVersionField = createTextField(700);
        liquibaseVersionField.setPromptText("Liquibase library not found");
        HBox versionRow = new HBox(12, versionLabel, liquibaseVersionField);
        versionRow.setAlignment(Pos.CENTER_LEFT);

        Label versionHint = new Label("Specify the version explicitly to activate the liquibase functionality");
        versionHint.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 152;");

        // 2. Changeset author
        Label authorLabel = createFieldLabel("Changeset author:", 140);
        changesetAuthorField = createTextField(700);
        HBox authorRow = new HBox(12, authorLabel, changesetAuthorField);
        authorRow.setAlignment(Pos.CENTER_LEFT);

        // 3. File type
        Label fileTypeLabel = createFieldLabel("File type:", 140);
        fileTypeCombo = new ComboBox<>();
        fileTypeCombo.getItems().addAll("XML", "YAML", "JSON", "SQL");
        fileTypeCombo.setValue("XML");
        fileTypeCombo.setPrefWidth(90);
        styleComboBox(fileTypeCombo);
        fileTypeCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox fileTypeRow = new HBox(12, fileTypeLabel, fileTypeCombo);
        fileTypeRow.setAlignment(Pos.CENTER_LEFT);

        // 4. Empty rollback checkbox
        addEmptyRollbackCheck = new CheckBox("Add empty rollback to changesets which don't support implicit one");
        addEmptyRollbackCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        addEmptyRollbackCheck.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        // 5. Changelog Templates section
        Label changelogHeader = new Label("Changelog Templates");
        changelogHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 0 2 0;");

        Label priDirLabel = createFieldLabel("Primary directory:", 140);
        primaryDirectoryField = createTextField(700);
        HBox priDirRow = new HBox(12, priDirLabel, primaryDirectoryField, createHelpIcon("Macro pattern supported: #date(\"yyyy\"), #date(\"MM\"), #date(\"dd\"), #increment(start, step, padding)"));
        priDirRow.setAlignment(Pos.CENTER_LEFT);

        Label priNameLabel = createFieldLabel("Primary name:", 140);
        primaryNameField = createTextField(700);
        HBox priNameRow = new HBox(12, priNameLabel, primaryNameField, createHelpIcon("Macro pattern supported: #date(\"yyyy\"), #date(\"MM\"), #date(\"dd\"), #increment(start, step, padding)"));
        priNameRow.setAlignment(Pos.CENTER_LEFT);

        Label secDirLabel = createFieldLabel("Secondary directory:", 140);
        secondaryDirectoryField = createTextField(700);
        HBox secDirRow = new HBox(12, secDirLabel, secondaryDirectoryField, createHelpIcon("Macro pattern supported: #date(\"yyyy\"), #date(\"MM\"), #date(\"dd\"), #increment(start, step, padding)"));
        secDirRow.setAlignment(Pos.CENTER_LEFT);

        Label secNameLabel = createFieldLabel("Secondary name:", 140);
        secondaryNameField = createTextField(700);
        HBox secNameRow = new HBox(12, secNameLabel, secondaryNameField, createHelpIcon("Macro pattern supported: #date(\"yyyy\"), #date(\"MM\"), #date(\"dd\"), #increment(start, step, padding)"));
        secNameRow.setAlignment(Pos.CENTER_LEFT);

        // 6. Db Types section
        Label dbTypesHeader = new Label("Db Types");
        dbTypesHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 0 2 0;");

        HBox dbRow1 = new HBox(18);
        dbRow1.setAlignment(Pos.CENTER_LEFT);
        dbRow1.getChildren().addAll(
                createDbTypeItem("mysql", "#00758F"),
                createDbTypeItem("postgres", "#336791"),
                createDbTypeItem("oracle", "#F80000"),
                createDbTypeItem("db2", "#41B883")
        );

        HBox dbRow2 = new HBox(18);
        dbRow2.setAlignment(Pos.CENTER_LEFT);
        dbRow2.getChildren().addAll(
                createDbTypeItem("maria", "#C07E4C"),
                createDbTypeItem("mssql", "#CE422B"),
                createDbTypeItem("h2", "#006699"),
                createDbTypeItem("hsql", "#4A90E2")
        );

        // 7. Changeset Templates section
        Label templatesHeader = new Label("Changeset Templates");
        templatesHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 0 2 0;");

        templatesTable = createTemplatesTable();
        VBox.setVgrow(templatesTable, Priority.ALWAYS);

        getChildren().addAll(
                versionRow,
                versionHint,
                authorRow,
                fileTypeRow,
                addEmptyRollbackCheck,
                changelogHeader,
                priDirRow,
                priNameRow,
                secDirRow,
                secNameRow,
                dbTypesHeader,
                dbRow1,
                dbRow2,
                templatesHeader,
                templatesTable
        );
    }

    private HBox createDbTypeItem(String name, String badgeColor) {
        Circle dot = new Circle(4, Color.web(badgeColor));
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nameLbl.setPrefWidth(65);

        CheckBox cb = new CheckBox();
        cb.setStyle("-fx-text-fill: #DFE1E5;");
        cb.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        dbTypeCheckBoxes.put(name, cb);

        HBox box = new HBox(6, dot, nameLbl, cb);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(110);
        return box;
    }

    private Label createFieldLabel(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private TextField createTextField(double width) {
        TextField tf = new TextField();
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 7 3 7;");
        tf.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return tf;
    }

    private Label createHelpIcon(String tooltipText) {
        Label helpIcon = new Label("?");
        helpIcon.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        helpIcon.setTooltip(new Tooltip(tooltipText));
        return helpIcon;
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    @SuppressWarnings("unchecked")
    private TableView<LiquibaseChangesetTemplateItem> createTemplatesTable() {
        TableView<LiquibaseChangesetTemplateItem> tv = new TableView<>();
        tv.setEditable(true);
        tv.setPrefHeight(240);
        tv.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<LiquibaseChangesetTemplateItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(220);
        nameCol.setEditable(false);
        nameCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-text-fill: #DFE1E5;");

        TableColumn<LiquibaseChangesetTemplateItem, Boolean> failCol = new TableColumn<>("failOnError");
        failCol.setCellValueFactory(data -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(data.getValue().isFailOnError());
            prop.addListener((obs, o, n) -> {
                data.getValue().setFailOnError(n);
                if (!updating) notifyModified();
            });
            return prop;
        });
        failCol.setCellFactory(CheckBoxTableCell.forTableColumn(failCol));
        failCol.setPrefWidth(160);
        failCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<LiquibaseChangesetTemplateItem, Boolean> runCol = new TableColumn<>("runOnChange");
        runCol.setCellValueFactory(data -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(data.getValue().isRunOnChange());
            prop.addListener((obs, o, n) -> {
                data.getValue().setRunOnChange(n);
                if (!updating) notifyModified();
            });
            return prop;
        });
        runCol.setCellFactory(CheckBoxTableCell.forTableColumn(runCol));
        runCol.setPrefWidth(160);
        runCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<LiquibaseChangesetTemplateItem, Boolean> preCol = new TableColumn<>("Create Preconditions");
        preCol.setCellValueFactory(data -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(data.getValue().isCreatePreconditions());
            prop.addListener((obs, o, n) -> {
                data.getValue().setCreatePreconditions(n);
                if (!updating) notifyModified();
            });
            return prop;
        });
        preCol.setCellFactory(CheckBoxTableCell.forTableColumn(preCol));
        preCol.setPrefWidth(180);
        preCol.setStyle("-fx-alignment: CENTER;");

        tv.getColumns().addAll(nameCol, failCol, runCol, preCol);

        templatesData = FXCollections.observableArrayList();
        tv.setItems(templatesData);
        return tv;
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            currentSettings = initialSettings.clone();

            liquibaseVersionField.setText(currentSettings.getLiquibaseVersion());
            changesetAuthorField.setText(currentSettings.getChangesetAuthor());
            fileTypeCombo.setValue(currentSettings.getFileType());
            addEmptyRollbackCheck.setSelected(currentSettings.isAddEmptyRollback());

            primaryDirectoryField.setText(currentSettings.getPrimaryDirectory());
            primaryNameField.setText(currentSettings.getPrimaryName());
            secondaryDirectoryField.setText(currentSettings.getSecondaryDirectory());
            secondaryNameField.setText(currentSettings.getSecondaryName());

            Set<String> enabledTypes = currentSettings.getEnabledDbTypes();
            for (Map.Entry<String, CheckBox> e : dbTypeCheckBoxes.entrySet()) {
                e.getValue().setSelected(enabledTypes.contains(e.getKey()));
            }

            templatesData.clear();
            for (LiquibaseChangesetTemplateItem item : currentSettings.getChangesetTemplates()) {
                templatesData.add(item.clone());
            }
        } finally {
            updating = false;
        }
    }

    private DatabaseLiquibaseSettings getCurrentSettingsFromUI() {
        DatabaseLiquibaseSettings s = new DatabaseLiquibaseSettings();
        s.setLiquibaseVersion(liquibaseVersionField.getText());
        s.setChangesetAuthor(changesetAuthorField.getText());
        s.setFileType(fileTypeCombo.getValue());
        s.setAddEmptyRollback(addEmptyRollbackCheck.isSelected());

        s.setPrimaryDirectory(primaryDirectoryField.getText());
        s.setPrimaryName(primaryNameField.getText());
        s.setSecondaryDirectory(secondaryDirectoryField.getText());
        s.setSecondaryName(secondaryNameField.getText());

        Set<String> enabled = new HashSet<>();
        for (Map.Entry<String, CheckBox> e : dbTypeCheckBoxes.entrySet()) {
            if (e.getValue().isSelected()) {
                enabled.add(e.getKey());
            }
        }
        s.setEnabledDbTypes(enabled);

        List<LiquibaseChangesetTemplateItem> templates = new ArrayList<>();
        for (LiquibaseChangesetTemplateItem item : templatesData) {
            templates.add(item.clone());
        }
        s.setChangesetTemplates(templates);

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DatabaseLiquibaseSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
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
    public TextField getLiquibaseVersionField() { return liquibaseVersionField; }
    public TextField getChangesetAuthorField() { return changesetAuthorField; }
    public ComboBox<String> getFileTypeCombo() { return fileTypeCombo; }
    public CheckBox getAddEmptyRollbackCheck() { return addEmptyRollbackCheck; }
    public TextField getPrimaryDirectoryField() { return primaryDirectoryField; }
    public TextField getPrimaryNameField() { return primaryNameField; }
    public TextField getSecondaryDirectoryField() { return secondaryDirectoryField; }
    public TextField getSecondaryNameField() { return secondaryNameField; }
    public Map<String, CheckBox> getDbTypeCheckBoxes() { return dbTypeCheckBoxes; }
    public TableView<LiquibaseChangesetTemplateItem> getTemplatesTable() { return templatesTable; }
}
