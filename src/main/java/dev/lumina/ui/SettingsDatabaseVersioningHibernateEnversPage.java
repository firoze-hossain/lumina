package dev.lumina.ui;

import dev.lumina.database.versioning.DatabaseHibernateEnversSettings;
import dev.lumina.database.versioning.DatabaseHibernateEnversSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > Database Versioning > Hibernate Envers settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsDatabaseVersioningHibernateEnversPage extends VBox {

    private final DatabaseHibernateEnversSettingsManager manager;
    private DatabaseHibernateEnversSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private CheckBox useValuesSpecifiedCheck;
    private TextField auditTablePrefixField;
    private TextField auditTableSuffixField;
    private TextField revisionFieldNameField;
    private TextField revisionTypeFieldNameField;
    private TextField defaultSchemaNameField;

    private CheckBox treatOptimisticLockingUnversionedCheck;
    private CheckBox trackEntityNamesChangedCheck;
    private CheckBox activateModifiedPropertiesFlagCheck;

    private TextField suffixModifiedFlagColumnsField;

    public SettingsDatabaseVersioningHibernateEnversPage() {
        this.manager = DatabaseHibernateEnversSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Use values specified in *.properties files
        useValuesSpecifiedCheck = new CheckBox("Use values specified in *.properties files");
        useValuesSpecifiedCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        useValuesSpecifiedCheck.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        // 2. Audit table prefix
        Label prefixLabel = createFieldLabel("Audit table prefix:", 330);
        auditTablePrefixField = createTextField("Use by default: <empty>", 550);
        HBox prefixRow = new HBox(12, prefixLabel, auditTablePrefixField);
        prefixRow.setAlignment(Pos.CENTER_LEFT);

        // 3. Audit table suffix
        Label suffixLabel = createFieldLabel("Audit table suffix:", 330);
        auditTableSuffixField = createTextField("Use by default: _aud", 550);
        HBox suffixRow = new HBox(12, suffixLabel, auditTableSuffixField);
        suffixRow.setAlignment(Pos.CENTER_LEFT);

        // 4. Revision field name
        Label revNameLabel = createFieldLabel("Revision field name:", 330);
        revisionFieldNameField = createTextField("Use by default: rev", 550);
        HBox revNameRow = new HBox(12, revNameLabel, revisionFieldNameField);
        revNameRow.setAlignment(Pos.CENTER_LEFT);

        // 5. Revision type field name
        Label revTypeLabel = createFieldLabel("Revision type field name:", 330);
        revisionTypeFieldNameField = createTextField("Use by default: revtype", 550);
        HBox revTypeRow = new HBox(12, revTypeLabel, revisionTypeFieldNameField);
        revTypeRow.setAlignment(Pos.CENTER_LEFT);

        // 6. Default schema name
        Label schemaLabel = createFieldLabel("Default name of the schema containing audit tables:", 330);
        defaultSchemaNameField = createTextField("Use by default: <empty>", 550);
        HBox schemaRow = new HBox(12, schemaLabel, defaultSchemaNameField);
        schemaRow.setAlignment(Pos.CENTER_LEFT);

        // 7. Optimistic locking checkbox
        treatOptimisticLockingUnversionedCheck = new CheckBox("Treat optimistic locking properties as unversioned");
        treatOptimisticLockingUnversionedCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        treatOptimisticLockingUnversionedCheck.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        // 8. Track entity names changed checkbox
        trackEntityNamesChangedCheck = new CheckBox("Track entity names that have been changed during each revision");
        trackEntityNamesChangedCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        trackEntityNamesChangedCheck.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        // 9. Activate modified properties flag checkbox
        activateModifiedPropertiesFlagCheck = new CheckBox("Activate modified properties flag feature");
        activateModifiedPropertiesFlagCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        activateModifiedPropertiesFlagCheck.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        // 10. Suffix of modified flag columns
        Label modSuffixLabel = createFieldLabel("Suffix of modified flag columns:", 330);
        suffixModifiedFlagColumnsField = createTextField("Use by default: _mod", 550);
        HBox modSuffixRow = new HBox(12, modSuffixLabel, suffixModifiedFlagColumnsField);
        modSuffixRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
                useValuesSpecifiedCheck,
                prefixRow,
                suffixRow,
                revNameRow,
                revTypeRow,
                schemaRow,
                treatOptimisticLockingUnversionedCheck,
                trackEntityNamesChangedCheck,
                activateModifiedPropertiesFlagCheck,
                modSuffixRow
        );
    }

    private Label createFieldLabel(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private TextField createTextField(String prompt, double width) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 7 3 7;");
        tf.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return tf;
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            useValuesSpecifiedCheck.setSelected(initialSettings.isUseValuesInPropertiesFiles());
            auditTablePrefixField.setText(initialSettings.getAuditTablePrefix());
            auditTableSuffixField.setText(initialSettings.getAuditTableSuffix());
            revisionFieldNameField.setText(initialSettings.getRevisionFieldName());
            revisionTypeFieldNameField.setText(initialSettings.getRevisionTypeFieldName());
            defaultSchemaNameField.setText(initialSettings.getDefaultSchemaName());

            treatOptimisticLockingUnversionedCheck.setSelected(initialSettings.isTreatOptimisticLockingUnversioned());
            trackEntityNamesChangedCheck.setSelected(initialSettings.isTrackEntityNamesChanged());
            activateModifiedPropertiesFlagCheck.setSelected(initialSettings.isActivateModifiedPropertiesFlag());

            suffixModifiedFlagColumnsField.setText(initialSettings.getSuffixModifiedFlagColumns());
        } finally {
            updating = false;
        }
    }

    private DatabaseHibernateEnversSettings getCurrentSettingsFromUI() {
        DatabaseHibernateEnversSettings s = new DatabaseHibernateEnversSettings();
        s.setUseValuesInPropertiesFiles(useValuesSpecifiedCheck.isSelected());
        s.setAuditTablePrefix(auditTablePrefixField.getText());
        s.setAuditTableSuffix(auditTableSuffixField.getText());
        s.setRevisionFieldName(revisionFieldNameField.getText());
        s.setRevisionTypeFieldName(revisionTypeFieldNameField.getText());
        s.setDefaultSchemaName(defaultSchemaNameField.getText());

        s.setTreatOptimisticLockingUnversioned(treatOptimisticLockingUnversionedCheck.isSelected());
        s.setTrackEntityNamesChanged(trackEntityNamesChangedCheck.isSelected());
        s.setActivateModifiedPropertiesFlag(activateModifiedPropertiesFlagCheck.isSelected());

        s.setSuffixModifiedFlagColumns(suffixModifiedFlagColumnsField.getText());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DatabaseHibernateEnversSettings updated = getCurrentSettingsFromUI();
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

    // Getters for testing and automation
    public CheckBox getUseValuesSpecifiedCheck() { return useValuesSpecifiedCheck; }
    public TextField getAuditTablePrefixField() { return auditTablePrefixField; }
    public TextField getAuditTableSuffixField() { return auditTableSuffixField; }
    public TextField getRevisionFieldNameField() { return revisionFieldNameField; }
    public TextField getRevisionTypeFieldNameField() { return revisionTypeFieldNameField; }
    public TextField getDefaultSchemaNameField() { return defaultSchemaNameField; }
    public CheckBox getTreatOptimisticLockingUnversionedCheck() { return treatOptimisticLockingUnversionedCheck; }
    public CheckBox getTrackEntityNamesChangedCheck() { return trackEntityNamesChangedCheck; }
    public CheckBox getActivateModifiedPropertiesFlagCheck() { return activateModifiedPropertiesFlagCheck; }
    public TextField getSuffixModifiedFlagColumnsField() { return suffixModifiedFlagColumnsField; }
}
