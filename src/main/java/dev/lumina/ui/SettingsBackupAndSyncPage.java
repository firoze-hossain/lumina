package dev.lumina.ui;

import dev.lumina.tools.BackupAndSyncSettings;
import dev.lumina.tools.BackupAndSyncSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Settings UI page for Backup and Sync in Lumina IDE.
 * 1:1 visual match with reference screenshot layout.
 */
public class SettingsBackupAndSyncPage extends VBox {

    private final CheckBox enableBackupCheck;
    private final ComboBox<String> accountCombo;

    private final CheckBox syncUiCheck;
    private final CheckBox syncCodeCheck;
    private final CheckBox syncKeymapsCheck;
    private final CheckBox syncPluginsCheck;
    private final CheckBox syncToolsCheck;

    private final Label syncStatusLabel;
    private final Button syncNowBtn;

    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsBackupAndSyncPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Subtitle: Sync UI, Code and System settings, Keymaps, Plugins, and Tools.
        Label subtitle = new Label("Sync UI, Code and System settings, Keymaps, Plugins, and Tools.");
        subtitle.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");

        // Row: Enable backup and sync: [account dropdown]
        enableBackupCheck = new CheckBox("Enable backup and sync:");
        enableBackupCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        accountCombo = new ComboBox<>();
        accountCombo.getItems().addAll("15103202@iubat.edu");
        accountCombo.setValue("15103202@iubat.edu");
        accountCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        accountCombo.setPrefWidth(260);

        accountCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("👤 " + item);
                    setStyle("-fx-text-fill: #DFE1E5;");
                }
            }
        });
        accountCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("👤 " + item);
                    setStyle("-fx-text-fill: #DFE1E5;");
                }
            }
        });

        accountCombo.disableProperty().bind(enableBackupCheck.selectedProperty().not());

        HBox enableRow = new HBox(10, enableBackupCheck, accountCombo);
        enableRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Sync Components
        HBox componentsHeader = createSectionHeader("Sync Components");

        syncUiCheck = new CheckBox("UI and Editor Theme settings");
        styleCheckBox(syncUiCheck);
        syncUiCheck.setSelected(true);

        syncCodeCheck = new CheckBox("Code and System settings");
        styleCheckBox(syncCodeCheck);
        syncCodeCheck.setSelected(true);

        syncKeymapsCheck = new CheckBox("Keymaps and shortcuts");
        styleCheckBox(syncKeymapsCheck);
        syncKeymapsCheck.setSelected(true);

        syncPluginsCheck = new CheckBox("Plugins and extensions");
        styleCheckBox(syncPluginsCheck);
        syncPluginsCheck.setSelected(true);

        syncToolsCheck = new CheckBox("Tools and External configurations");
        styleCheckBox(syncToolsCheck);
        syncToolsCheck.setSelected(true);

        VBox componentsBox = new VBox(8,
                syncUiCheck,
                syncCodeCheck,
                syncKeymapsCheck,
                syncPluginsCheck,
                syncToolsCheck
        );
        componentsBox.setPadding(new Insets(0, 0, 0, 16));
        componentsBox.disableProperty().bind(enableBackupCheck.selectedProperty().not());

        // Status row & actions
        HBox statusHeader = createSectionHeader("Status");

        syncStatusLabel = new Label("Last synced: Just now");
        syncStatusLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        syncNowBtn = new Button("Sync Now");
        syncNowBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 14; -fx-background-radius: 4; -fx-cursor: hand;");
        syncNowBtn.disableProperty().bind(enableBackupCheck.selectedProperty().not());
        syncNowBtn.setOnAction(e -> {
            syncStatusLabel.setText("Last synced: " + java.time.LocalTime.now().toString().substring(0, 8));
            notifyModified();
        });

        HBox statusRow = new HBox(14, syncStatusLabel, syncNowBtn);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setPadding(new Insets(0, 0, 0, 16));

        getChildren().addAll(
                subtitle,
                enableRow,
                componentsHeader,
                componentsBox,
                statusHeader,
                statusRow
        );

        setupListeners();
        loadSettings();
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private HBox createSectionHeader(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox box = new HBox(12, titleLabel, line);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 0, 4, 0));
        return box;
    }

    private void setupListeners() {
        enableBackupCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        accountCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        syncUiCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        syncCodeCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        syncKeymapsCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        syncPluginsCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        syncToolsCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            BackupAndSyncSettings s = BackupAndSyncSettingsManager.getInstance().getSettings();
            enableBackupCheck.setSelected(s.isEnableBackupAndSync());
            if (!s.getAvailableAccounts().isEmpty()) {
                accountCombo.getItems().setAll(s.getAvailableAccounts());
            }
            accountCombo.setValue(s.getSyncAccount());
            syncUiCheck.setSelected(s.isSyncUi());
            syncCodeCheck.setSelected(s.isSyncCodeAndSystem());
            syncKeymapsCheck.setSelected(s.isSyncKeymaps());
            syncPluginsCheck.setSelected(s.isSyncPlugins());
            syncToolsCheck.setSelected(s.isSyncTools());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        BackupAndSyncSettings saved = BackupAndSyncSettingsManager.getInstance().getSettings();
        if (enableBackupCheck.isSelected() != saved.isEnableBackupAndSync()) return true;
        if (!Objects.equals(accountCombo.getValue(), saved.getSyncAccount())) return true;
        if (syncUiCheck.isSelected() != saved.isSyncUi()) return true;
        if (syncCodeCheck.isSelected() != saved.isSyncCodeAndSystem()) return true;
        if (syncKeymapsCheck.isSelected() != saved.isSyncKeymaps()) return true;
        if (syncPluginsCheck.isSelected() != saved.isSyncPlugins()) return true;
        return syncToolsCheck.isSelected() != saved.isSyncTools();
    }

    public void apply() {
        BackupAndSyncSettings s = new BackupAndSyncSettings();
        s.setEnableBackupAndSync(enableBackupCheck.isSelected());
        s.setSyncAccount(accountCombo.getValue());
        s.setAvailableAccounts(new java.util.ArrayList<>(accountCombo.getItems()));
        s.setSyncUi(syncUiCheck.isSelected());
        s.setSyncCodeAndSystem(syncCodeCheck.isSelected());
        s.setSyncKeymaps(syncKeymapsCheck.isSelected());
        s.setSyncPlugins(syncPluginsCheck.isSelected());
        s.setSyncTools(syncToolsCheck.isSelected());
        s.setLastSyncTimeMs(System.currentTimeMillis());

        BackupAndSyncSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    public CheckBox getEnableBackupCheck() {
        return enableBackupCheck;
    }

    public ComboBox<String> getAccountCombo() {
        return accountCombo;
    }
}
