package dev.lumina.ui;

import dev.lumina.database.versioning.DatabaseFlywaySettings;
import dev.lumina.database.versioning.DatabaseFlywaySettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > Database Versioning > Flyway settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsDatabaseVersioningFlywayPage extends VBox {

    private final DatabaseFlywaySettingsManager manager;
    private DatabaseFlywaySettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private TextField migrationPrefixField;
    private ComboBox<String> versionPatternCombo;
    private TextField migrationSeparatorField;
    private TextField migrationDescriptionField;
    private CheckBox useFlywayWithoutDependencyCheck;

    public SettingsDatabaseVersioningFlywayPage() {
        this.manager = DatabaseFlywaySettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Migration prefix
        Label prefixLabel = createFieldLabel("Migration prefix:");
        prefixLabel.setPrefWidth(160);
        migrationPrefixField = createTextField(650);
        HBox prefixRow = new HBox(12, prefixLabel, migrationPrefixField);
        prefixRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Version pattern
        Label verLabel = createFieldLabel("Version pattern:");
        verLabel.setPrefWidth(160);
        versionPatternCombo = new ComboBox<>();
        versionPatternCombo.setEditable(true);
        versionPatternCombo.getItems().addAll(
                "#increment(1, 1, \"0\")",
                "#increment(1, 1, \"\")",
                "yyyyMMddHHmmss",
                "yyyy.MM.dd.HH.mm.ss"
        );
        versionPatternCombo.setPrefWidth(650);
        styleComboBox(versionPatternCombo);
        versionPatternCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        Label helpIcon = new Label("?");
        helpIcon.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        helpIcon.setTooltip(new Tooltip("Patterns supported: #increment(start, step, padding), date formats (e.g., yyyyMMddHHmmss)"));

        HBox verRow = new HBox(12, verLabel, versionPatternCombo, helpIcon);
        verRow.setAlignment(Pos.CENTER_LEFT);

        // 3. Migration separator
        Label sepLabel = createFieldLabel("Migration separator:");
        sepLabel.setPrefWidth(160);
        migrationSeparatorField = createTextField(650);
        HBox sepRow = new HBox(12, sepLabel, migrationSeparatorField);
        sepRow.setAlignment(Pos.CENTER_LEFT);

        // 4. Migration description
        Label descLabel = createFieldLabel("Migration description:");
        descLabel.setPrefWidth(160);
        migrationDescriptionField = createTextField(650);
        HBox descRow = new HBox(12, descLabel, migrationDescriptionField);
        descRow.setAlignment(Pos.CENTER_LEFT);

        // 5. Use Flyway without dependency
        useFlywayWithoutDependencyCheck = new CheckBox("Use Flyway without dependency");
        useFlywayWithoutDependencyCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        useFlywayWithoutDependencyCheck.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        getChildren().addAll(
                prefixRow,
                verRow,
                sepRow,
                descRow,
                useFlywayWithoutDependencyCheck
        );
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private TextField createTextField(double width) {
        TextField tf = new TextField();
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 7 3 7;");
        tf.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return tf;
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            migrationPrefixField.setText(initialSettings.getMigrationPrefix());
            versionPatternCombo.setValue(initialSettings.getVersionPattern());
            migrationSeparatorField.setText(initialSettings.getMigrationSeparator());
            migrationDescriptionField.setText(initialSettings.getMigrationDescription());
            useFlywayWithoutDependencyCheck.setSelected(initialSettings.isUseFlywayWithoutDependency());
        } finally {
            updating = false;
        }
    }

    private DatabaseFlywaySettings getCurrentSettingsFromUI() {
        DatabaseFlywaySettings s = new DatabaseFlywaySettings();
        s.setMigrationPrefix(migrationPrefixField.getText());
        s.setVersionPattern(versionPatternCombo.getValue());
        s.setMigrationSeparator(migrationSeparatorField.getText());
        s.setMigrationDescription(migrationDescriptionField.getText());
        s.setUseFlywayWithoutDependency(useFlywayWithoutDependencyCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DatabaseFlywaySettings updated = getCurrentSettingsFromUI();
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
    public TextField getMigrationPrefixField() { return migrationPrefixField; }
    public ComboBox<String> getVersionPatternCombo() { return versionPatternCombo; }
    public TextField getMigrationSeparatorField() { return migrationSeparatorField; }
    public TextField getMigrationDescriptionField() { return migrationDescriptionField; }
    public CheckBox getUseFlywayWithoutDependencyCheck() { return useFlywayWithoutDependencyCheck; }
}
