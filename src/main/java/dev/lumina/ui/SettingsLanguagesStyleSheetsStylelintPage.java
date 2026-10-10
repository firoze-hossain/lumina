package dev.lumina.ui;

import dev.lumina.stylesheets.StylelintSettings;
import dev.lumina.stylesheets.StylelintSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > Style Sheets > Stylelint.
 * Matches reference screenshot media_1791600461236_fc374953.png:
 *  - [ ] Enable
 *  - Stylelint package: [ ComboBox / TextField + ... button ]
 *  - Configuration file: [ Auto-detect + 📁 button ]
 *  - Run for files: [ **\/*.{css} ]
 *      Use a glob pattern ↗ , for example, **\/*.{css,html}
 *  - [ ] Run stylelint --fix on save
 */
public class SettingsLanguagesStyleSheetsStylelintPage extends VBox {

    private final StylelintSettingsManager manager = StylelintSettingsManager.getInstance();

    private final CheckBox enableCheck = new CheckBox("Enable");
    private final VBox optionsContainer = new VBox(12);

    private final TextField packageField = new TextField();
    private final Button packageBrowseBtn = new Button("...");

    private final TextField configFileField = new TextField("Auto-detect");
    private final Button configBrowseBtn = new Button("📁");

    private final TextField runForFilesField = new TextField("**/*.{css}");
    private final Hyperlink globLink = new Hyperlink("↗");

    private final CheckBox fixOnSaveCheck = new CheckBox("Run stylelint --fix on save");

    private StylelintSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesStyleSheetsStylelintPage() {
        setSpacing(14);
        setPadding(new Insets(16, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Enable checkbox ---
        enableCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        enableCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            updateEnabledStates();
            fireModified();
        });

        // --- 2. Stylelint package ---
        Label packageLabel = createLabel("Stylelint package:");
        packageField.setPromptText("Path to stylelint package");
        styleTextField(packageField);
        packageField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        styleBrowseButton(packageBrowseBtn);
        packageBrowseBtn.setOnAction(e -> choosePackageDir());

        HBox packageInputBox = new HBox(6, packageField, packageBrowseBtn);
        packageInputBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(packageField, Priority.ALWAYS);

        HBox packageRow = new HBox(12, packageLabel, packageInputBox);
        packageRow.setAlignment(Pos.CENTER_LEFT);

        // --- 3. Configuration file ---
        Label configLabel = createLabel("Configuration file:");
        configFileField.setPromptText("Auto-detect or config path");
        styleTextField(configFileField);
        configFileField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        styleBrowseButton(configBrowseBtn);
        configBrowseBtn.setOnAction(e -> chooseConfigFile());

        HBox configInputBox = new HBox(6, configFileField, configBrowseBtn);
        configInputBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(configFileField, Priority.ALWAYS);

        HBox configRow = new HBox(12, configLabel, configInputBox);
        configRow.setAlignment(Pos.CENTER_LEFT);

        // --- 4. Run for files ---
        Label runForLabel = createLabel("Run for files:");
        runForFilesField.setPromptText("**/*.{css}");
        styleTextField(runForFilesField);
        runForFilesField.textProperty().addListener((obs, oldV, newV) -> fireModified());
        HBox.setHgrow(runForFilesField, Priority.ALWAYS);

        HBox runForRow = new HBox(12, runForLabel, runForFilesField);
        runForRow.setAlignment(Pos.CENTER_LEFT);

        // Subtext below Run for files
        Label globPrefix = new Label("Use a glob pattern ");
        globPrefix.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");

        globLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
        globLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://en.wikipedia.org/wiki/Glob_(programming)"));
                }
            } catch (Exception ignored) {}
        });

        Label globSuffix = new Label(", for example, **/*.{css,html}");
        globSuffix.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");

        HBox globHelpBox = new HBox(0, globPrefix, globLink, globSuffix);
        globHelpBox.setAlignment(Pos.CENTER_LEFT);
        globHelpBox.setPadding(new Insets(0, 0, 4, 142));

        VBox runForGroup = new VBox(3, runForRow, globHelpBox);

        // --- 5. Fix on save ---
        fixOnSaveCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        fixOnSaveCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        optionsContainer.getChildren().addAll(packageRow, configRow, runForGroup, fixOnSaveCheck);
        optionsContainer.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(enableCheck, optionsContainer);
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        lbl.setPrefWidth(130);
        lbl.setMinWidth(130);
        return lbl;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
    }

    private void styleBrowseButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
    }

    private void updateEnabledStates() {
        boolean enabled = enableCheck.isSelected();
        optionsContainer.setDisable(!enabled);
    }

    private void choosePackageDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Stylelint Package Directory");
        File selected = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            packageField.setText(selected.getAbsolutePath());
            fireModified();
        }
    }

    private void chooseConfigFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Stylelint Configuration File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Stylelint Config (*.json, *.js, *.cjs, *.mjs, *.yaml, *.yml)",
                        "*.json", "*.js", "*.cjs", "*.mjs", "*.yaml", "*.yml"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );
        File selected = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            configFileField.setText(selected.getAbsolutePath());
            fireModified();
        }
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        enableCheck.setSelected(initialSettings.isEnabled());
        packageField.setText(initialSettings.getPackagePath());
        configFileField.setText(initialSettings.getConfigurationFile());
        runForFilesField.setText(initialSettings.getRunForFiles());
        fixOnSaveCheck.setSelected(initialSettings.isFixOnSave());

        updateEnabledStates();
        updating = false;
    }

    public StylelintSettings getCurrentSettings() {
        StylelintSettings s = new StylelintSettings();
        s.setEnabled(enableCheck.isSelected());
        s.setPackagePath(packageField.getText().trim());
        s.setConfigurationFile(configFileField.getText().trim());
        s.setRunForFiles(runForFilesField.getText().trim());
        s.setFixOnSave(fixOnSaveCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        StylelintSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadData();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for programmatic inspection and testing
    public CheckBox getEnableCheck() {
        return enableCheck;
    }

    public VBox getOptionsContainer() {
        return optionsContainer;
    }

    public TextField getPackageField() {
        return packageField;
    }

    public Button getPackageBrowseBtn() {
        return packageBrowseBtn;
    }

    public TextField getConfigFileField() {
        return configFileField;
    }

    public Button getConfigBrowseBtn() {
        return configBrowseBtn;
    }

    public TextField getRunForFilesField() {
        return runForFilesField;
    }

    public Hyperlink getGlobLink() {
        return globLink;
    }

    public CheckBox getFixOnSaveCheck() {
        return fixOnSaveCheck;
    }
}
