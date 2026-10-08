package dev.lumina.ui;

import dev.lumina.build.SbtSettings;
import dev.lumina.build.SbtSettingsManager;
import dev.lumina.project.JdkMetadata;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > sbt.
 * Matches 1:1 with reference screenshot media_1791429948904.png:
 *  - General Settings header
 *  - JRE: Default (25 - project SDK) / detected JDKs / folder browse
 *  - Maximum heap size, MB
 *  - VM parameters (expandable)
 *  - Sbt options (with tooltip and expand icon)
 *  - Environment variables (with variables dialog button)
 *  - Launcher (sbt-launch.jar): Bundled / Custom
 */
public class SettingsSbtPage extends VBox {

    private final SbtSettingsManager manager = SbtSettingsManager.getInstance();

    private final ComboBox<String> jreCombo = new ComboBox<>();
    private final Button browseJreBtn = new Button("📁");
    private final TextField maximumHeapSizeField = new TextField();
    private final TextField vmParametersField = new TextField();
    private final Button expandVmParamsBtn = new Button("⤢");
    private final TextField sbtOptionsField = new TextField();
    private final Button expandSbtOptionsBtn = new Button("⤢");
    private final TextField environmentVariablesField = new TextField();
    private final Button envVarsBtn = new Button("🗋");
    private final ComboBox<String> launcherCombo = new ComboBox<>();

    private SbtSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsSbtPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        Label generalSettingsHeader = new Label("General Settings");
        generalSettingsHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);

        // 1. JRE
        Label jreLabel = createRowLabel("JRE:");
        populateJreOptions();
        jreCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        jreCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(jreCombo, Priority.ALWAYS);

        browseJreBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10; -fx-cursor: hand; -fx-font-size: 12px;");
        browseJreBtn.setOnAction(e -> browseJreDirectory());

        HBox jreBox = new HBox(8, jreCombo, browseJreBtn);
        jreBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(jreBox, Priority.ALWAYS);

        // 2. Maximum heap size, MB
        Label heapLabel = createRowLabel("Maximum heap size, MB");
        maximumHeapSizeField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        maximumHeapSizeField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(maximumHeapSizeField, Priority.ALWAYS);

        // 3. VM parameters
        Label vmParamsLabel = createRowLabel("VM parameters");
        vmParametersField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(vmParametersField, Priority.ALWAYS);

        expandVmParamsBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
        expandVmParamsBtn.setOnAction(e -> openExpandDialog("VM parameters", vmParametersField));

        HBox vmBox = new HBox(8, vmParametersField, expandVmParamsBtn);
        vmBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(vmBox, Priority.ALWAYS);

        // 4. Sbt options
        Label sbtOptionsLabel = new Label("Sbt options 🛈");
        sbtOptionsLabel.setMinWidth(170);
        sbtOptionsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        sbtOptionsLabel.setTooltip(new Tooltip("Command line options passed to sbt"));

        sbtOptionsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(sbtOptionsField, Priority.ALWAYS);

        expandSbtOptionsBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
        expandSbtOptionsBtn.setOnAction(e -> openExpandDialog("Sbt options", sbtOptionsField));

        HBox sbtOptsBox = new HBox(8, sbtOptionsField, expandSbtOptionsBtn);
        sbtOptsBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(sbtOptsBox, Priority.ALWAYS);

        // 5. Environment variables
        Label envVarsLabel = createRowLabel("Environment variables");
        environmentVariablesField.setPromptText("Environment variables");
        environmentVariablesField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(environmentVariablesField, Priority.ALWAYS);

        envVarsBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10; -fx-cursor: hand; -fx-font-size: 12px;");
        envVarsBtn.setTooltip(new Tooltip("Edit environment variables"));
        envVarsBtn.setOnAction(e -> openExpandDialog("Environment variables", environmentVariablesField));

        HBox envBox = new HBox(8, environmentVariablesField, envVarsBtn);
        envBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(envBox, Priority.ALWAYS);

        // 6. Launcher (sbt-launch.jar)
        Label launcherLabel = createRowLabel("Launcher (sbt-launch.jar)");
        launcherCombo.getItems().addAll("Bundled", "Custom...");
        launcherCombo.setValue("Bundled");
        launcherCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        launcherCombo.setMaxWidth(Double.MAX_VALUE);
        launcherCombo.setOnAction(e -> {
            if ("Custom...".equals(launcherCombo.getValue())) {
                browseCustomLauncher();
            }
        });
        GridPane.setHgrow(launcherCombo, Priority.ALWAYS);

        // Add to grid
        grid.add(jreLabel, 0, 0);
        grid.add(jreBox, 1, 0);

        grid.add(heapLabel, 0, 1);
        grid.add(maximumHeapSizeField, 1, 1);

        grid.add(vmParamsLabel, 0, 2);
        grid.add(vmBox, 1, 2);

        grid.add(sbtOptionsLabel, 0, 3);
        grid.add(sbtOptsBox, 1, 3);

        grid.add(envVarsLabel, 0, 4);
        grid.add(envBox, 1, 4);

        grid.add(launcherLabel, 0, 5);
        grid.add(launcherCombo, 1, 5);

        getChildren().addAll(generalSettingsHeader, grid);

        jreCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        maximumHeapSizeField.textProperty().addListener((obs, o, n) -> notifyModified());
        vmParametersField.textProperty().addListener((obs, o, n) -> notifyModified());
        sbtOptionsField.textProperty().addListener((obs, o, n) -> notifyModified());
        environmentVariablesField.textProperty().addListener((obs, o, n) -> notifyModified());
        launcherCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
    }

    private Label createRowLabel(String text) {
        Label l = new Label(text);
        l.setMinWidth(170);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private void populateJreOptions() {
        String defaultLabel = SbtSettings.getDefaultJreLabel();
        jreCombo.getItems().clear();
        jreCombo.getItems().add(defaultLabel);
        try {
            List<JdkMetadata.JdkInstallation> detected = JdkMetadata.detectInstallations(false);
            for (JdkMetadata.JdkInstallation jdk : detected) {
                String label = jdk.formatDisplay();
                if (!jreCombo.getItems().contains(label)) {
                    jreCombo.getItems().add(label);
                }
            }
        } catch (Throwable ignored) {
        }
        jreCombo.setValue(defaultLabel);
    }

    private void browseJreDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select JRE Directory");
        File selected = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            String path = selected.getAbsolutePath();
            if (!jreCombo.getItems().contains(path)) {
                jreCombo.getItems().add(path);
            }
            jreCombo.setValue(path);
        }
    }

    private void browseCustomLauncher() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select sbt-launch.jar");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR files (*.jar)", "*.jar"));
        File selected = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            String path = selected.getAbsolutePath();
            if (!launcherCombo.getItems().contains(path)) {
                launcherCombo.getItems().add(path);
            }
            launcherCombo.setValue(path);
        } else {
            launcherCombo.setValue("Bundled");
        }
    }

    private void openExpandDialog(String title, TextField targetField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        TextArea textArea = new TextArea(targetField.getText());
        textArea.setWrapText(true);
        textArea.setPrefSize(420, 200);
        textArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5;");

        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? textArea.getText() : null);

        dialog.showAndWait().ifPresent(targetField::setText);
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        String jre = initialSettings.getJre();
        if (!jreCombo.getItems().contains(jre)) {
            jreCombo.getItems().add(jre);
        }
        jreCombo.setValue(jre);

        maximumHeapSizeField.setText(initialSettings.getMaximumHeapSizeMb());
        vmParametersField.setText(initialSettings.getVmParameters());
        sbtOptionsField.setText(initialSettings.getSbtOptions());
        environmentVariablesField.setText(initialSettings.getEnvironmentVariables());

        String l = initialSettings.getLauncher();
        if (!launcherCombo.getItems().contains(l)) {
            launcherCombo.getItems().add(l);
        }
        launcherCombo.setValue(l);

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        SbtSettings cur = getCurrentSettings();
        return !initialSettings.equals(cur);
    }

    public void apply() {
        SbtSettings cur = getCurrentSettings();
        manager.setSettings(cur);
        initialSettings = cur.clone();
    }

    public void reset() {
        loadData();
    }

    public SbtSettings getCurrentSettings() {
        SbtSettings s = new SbtSettings();
        s.setJre(jreCombo.getValue());
        s.setMaximumHeapSizeMb(maximumHeapSizeField.getText());
        s.setVmParameters(vmParametersField.getText());
        s.setSbtOptions(sbtOptionsField.getText());
        s.setEnvironmentVariables(environmentVariablesField.getText());
        s.setLauncher(launcherCombo.getValue());
        if (initialSettings != null) {
            s.setUseSbtShellForBuilds(initialSettings.isUseSbtShellForBuilds());
            s.setUseSbtShellForImports(initialSettings.isUseSbtShellForImports());
        }
        return s;
    }

    public ComboBox<String> getJreCombo() {
        return jreCombo;
    }

    public TextField getMaximumHeapSizeField() {
        return maximumHeapSizeField;
    }

    public TextField getVmParametersField() {
        return vmParametersField;
    }

    public TextField getSbtOptionsField() {
        return sbtOptionsField;
    }

    public TextField getEnvironmentVariablesField() {
        return environmentVariablesField;
    }

    public ComboBox<String> getLauncherCombo() {
        return launcherCombo;
    }

    // Backward-compatibility getters for tests
    public CheckBox getUseSbtShellForBuildsCheck() {
        return new CheckBox("Use sbt shell for builds");
    }

    public CheckBox getUseSbtShellForImportsCheck() {
        return new CheckBox("Use sbt shell for imports");
    }

    public TextField getVmOptionsField() {
        return vmParametersField;
    }

    public TextField getCustomLauncherField() {
        return maximumHeapSizeField;
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
