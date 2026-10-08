package dev.lumina.ui;

import dev.lumina.build.BspSettings;
import dev.lumina.build.BspSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > BSP.
 * Matches 1:1 with reference screenshot media_1791428082245.png:
 *  - General Settings header
 *  - BSP trace log: Enable checkbox
 */
public class SettingsBspPage extends VBox {

    private final BspSettingsManager manager = BspSettingsManager.getInstance();

    private final CheckBox enableTraceLogCheck = new CheckBox("Enable");

    private BspSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsBspPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        Label generalSettingsHeader = new Label("General Settings");
        generalSettingsHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label traceLogLabel = new Label("BSP trace log");
        traceLogLabel.setMinWidth(110);
        traceLogLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        enableTraceLogCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox row = new HBox(16, traceLogLabel, enableTraceLogCheck);
        row.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(generalSettingsHeader, row);

        enableTraceLogCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        enableTraceLogCheck.setSelected(initialSettings.isBspTraceLogEnabled());

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        BspSettings cur = getCurrentSettings();
        return !initialSettings.equals(cur);
    }

    public void apply() {
        BspSettings cur = getCurrentSettings();
        manager.setSettings(cur);
        initialSettings = cur.clone();
    }

    public void reset() {
        loadData();
    }

    public BspSettings getCurrentSettings() {
        BspSettings s = new BspSettings();
        s.setBspTraceLogEnabled(enableTraceLogCheck.isSelected());
        return s;
    }

    public CheckBox getEnableTraceLogCheck() {
        return enableTraceLogCheck;
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
