package dev.lumina.ui;

import dev.lumina.tools.BundlerSettings;
import dev.lumina.tools.BundlerSettingsManager;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Bundler settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsBundlerPage extends VBox {

    private final BundlerSettingsManager manager;
    private BundlerSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private CheckBox alwaysInstallRequiredVersionCheck;
    private CheckBox useDefaultArgumentsCheck;
    private TextField defaultArgumentsField;

    public SettingsToolsBundlerPage() {
        this.manager = BundlerSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. [x] Always install the required version of Bundler
        VBox section1 = new VBox(4);
        alwaysInstallRequiredVersionCheck = new CheckBox("Always install the required version of Bundler");
        alwaysInstallRequiredVersionCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        alwaysInstallRequiredVersionCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        Label section1Desc = new Label("When enabled, install the Bundler version from Gemfile.lock instead of prompting on the first Bundler command invocation");
        section1Desc.setWrapText(true);
        section1Desc.setMaxWidth(750);
        section1Desc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px; -fx-padding: 0 0 0 22;");

        section1.getChildren().addAll(alwaysInstallRequiredVersionCheck, section1Desc);

        // 2. Bundler Command Options divider
        HBox dividerBox = new HBox(12);
        dividerBox.setAlignment(Pos.CENTER_LEFT);
        dividerBox.setPadding(new Insets(10, 0, 4, 0));

        Label sectionHeader = new Label("Bundler Command Options");
        sectionHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Separator line = new Separator();
        line.setStyle("-fx-background-color: #393B40; -fx-border-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        dividerBox.getChildren().addAll(sectionHeader, line);

        // 3. [x] Use default arguments
        VBox section2 = new VBox(6);
        useDefaultArgumentsCheck = new CheckBox("Use default arguments");
        useDefaultArgumentsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        useDefaultArgumentsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            defaultArgumentsField.setDisable(!newVal);
            notifyModified();
        });

        Label section2Desc = new Label("When enabled, use the provided arguments instead of prompting on each Bundler command invocation");
        section2Desc.setWrapText(true);
        section2Desc.setMaxWidth(750);
        section2Desc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px; -fx-padding: 0 0 0 22;");

        defaultArgumentsField = new TextField();
        defaultArgumentsField.setPrefWidth(280);
        defaultArgumentsField.setMaxWidth(350);
        defaultArgumentsField.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 4 8 4 8;"
        );
        VBox.setMargin(defaultArgumentsField, new Insets(4, 0, 0, 22));
        defaultArgumentsField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        section2.getChildren().addAll(useDefaultArgumentsCheck, section2Desc, defaultArgumentsField);

        getChildren().addAll(section1, dividerBox, section2);
    }

    private void loadData() {
        updating = true;
        BundlerSettings s = manager.getSettings();
        alwaysInstallRequiredVersionCheck.setSelected(s.isAlwaysInstallRequiredVersion());
        useDefaultArgumentsCheck.setSelected(s.isUseDefaultArguments());
        defaultArgumentsField.setText(s.getDefaultArguments());
        defaultArgumentsField.setDisable(!s.isUseDefaultArguments());
        initialSettings = getCurrentSettingsFromUI();
        updating = false;
    }

    public BundlerSettings getCurrentSettingsFromUI() {
        BundlerSettings s = new BundlerSettings();
        s.setAlwaysInstallRequiredVersion(alwaysInstallRequiredVersionCheck.isSelected());
        s.setUseDefaultArguments(useDefaultArgumentsCheck.isSelected());
        s.setDefaultArguments(defaultArgumentsField.getText());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        BundlerSettings updated = getCurrentSettingsFromUI();
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

    public CheckBox getAlwaysInstallRequiredVersionCheck() {
        return alwaysInstallRequiredVersionCheck;
    }

    public CheckBox getUseDefaultArgumentsCheck() {
        return useDefaultArgumentsCheck;
    }

    public TextField getDefaultArgumentsField() {
        return defaultArgumentsField;
    }
}
