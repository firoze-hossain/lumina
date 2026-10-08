package dev.lumina.ui;

import dev.lumina.build.CargoSettings;
import dev.lumina.build.CargoSettingsManager;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > Cargo.
 * Matches 1:1 with reference screenshot media_1791428094406.png:
 *  - When a build fails, automatically show the first error in the editor
 *  - Offline mode (Pass the --offline option to Cargo commands to avoid network requests)
 */
public class SettingsCargoPage extends VBox {

    private final CargoSettingsManager manager = CargoSettingsManager.getInstance();

    private final CheckBox autoShowFirstErrorCheck = new CheckBox("When a build fails, automatically show the first error in the editor");
    private final CheckBox offlineModeCheck = new CheckBox("Offline mode");

    private CargoSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsCargoPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        autoShowFirstErrorCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        offlineModeCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Label offlineSubtext = new Label("Pass the --offline option to Cargo commands to avoid network requests");
        offlineSubtext.setWrapText(true);
        offlineSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 24;");

        VBox offlineBox = new VBox(4, offlineModeCheck, offlineSubtext);

        getChildren().addAll(autoShowFirstErrorCheck, offlineBox);

        autoShowFirstErrorCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        offlineModeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        autoShowFirstErrorCheck.setSelected(initialSettings.isAutoShowFirstError());
        offlineModeCheck.setSelected(initialSettings.isOfflineMode());

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        CargoSettings cur = getCurrentSettings();
        return !initialSettings.equals(cur);
    }

    public void apply() {
        CargoSettings cur = getCurrentSettings();
        manager.setSettings(cur);
        initialSettings = cur.clone();
    }

    public void reset() {
        loadData();
    }

    public CargoSettings getCurrentSettings() {
        CargoSettings s = new CargoSettings();
        s.setAutoShowFirstError(autoShowFirstErrorCheck.isSelected());
        s.setOfflineMode(offlineModeCheck.isSelected());
        return s;
    }

    public CheckBox getAutoShowFirstErrorCheck() {
        return autoShowFirstErrorCheck;
    }

    public CheckBox getOfflineModeCheck() {
        return offlineModeCheck;
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
