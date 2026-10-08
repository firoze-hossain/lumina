package dev.lumina.ui;

import dev.lumina.build.ScalaCompileServerSettings;
import dev.lumina.build.ScalaCompileServerSettingsManager;
import dev.lumina.project.JdkMetadata;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Compiler > Scala Compiler > Scala Compile Server.
 * Matches 1:1 with reference screenshot media_1791449956938_ef297082.png:
 *  - Use compile server
 *  - Compile independent modules in parallel, in up to [4] threads
 *  - Stop if idle for [120] minutes
 *  - Start process in project directory (?)
 *  - JVM:
 *    - JDK (?)
 *    - Maximum heap size, MB [2048]
 *    - VM options (with expand button)
 */
public class SettingsScalaCompileServerPage extends VBox {

    private final ScalaCompileServerSettingsManager manager = ScalaCompileServerSettingsManager.getInstance();

    private final CheckBox useCompileServerCheck = new CheckBox("Use compile server");
    private final CheckBox parallelModulesCheck = new CheckBox("Compile independent modules in parallel, in up to");
    private final Spinner<Integer> parallelThreadsSpinner = new Spinner<>(1, 64, 4);
    private final Label threadsLabel = new Label("threads");

    private final CheckBox stopIfIdleCheck = new CheckBox("Stop if idle for");
    private final Spinner<Integer> idleMinutesSpinner = new Spinner<>(1, 1440, 120);
    private final Label minutesLabel = new Label("minutes");

    private final CheckBox startProcessInProjectDirCheck = new CheckBox("Start process in project directory");
    private final Label helpStartDir = new Label("?");

    // JVM Section
    private final ComboBox<String> jdkCombo = new ComboBox<>();
    private final Label helpJdk = new Label("?");
    private final TextField maxHeapSizeField = new TextField("2048");
    private final TextField vmOptionsField = new TextField("-server -Xss2m -XX:+UseParallelGC -XX:MaxInlineLevel=20");
    private final Button expandVmOptionsBtn = new Button("⤢");

    private final VBox serverOptionsBox = new VBox(10);
    private final VBox jvmBox = new VBox(10);

    private ScalaCompileServerSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsScalaCompileServerPage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // 1. Top checkbox: Use compile server
        styleCheckBox(useCompileServerCheck);
        useCompileServerCheck.setOnAction(e -> {
            updateEnabledStates();
            fireModified();
        });

        // 2. Parallel modules row
        styleCheckBox(parallelModulesCheck);
        parallelModulesCheck.setOnAction(e -> fireModified());
        styleSpinner(parallelThreadsSpinner);
        parallelThreadsSpinner.valueProperty().addListener((obs, o, n) -> fireModified());
        threadsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox parallelRow = new HBox(8, parallelModulesCheck, parallelThreadsSpinner, threadsLabel);
        parallelRow.setAlignment(Pos.CENTER_LEFT);

        // 3. Stop if idle row
        styleCheckBox(stopIfIdleCheck);
        stopIfIdleCheck.setOnAction(e -> fireModified());
        styleSpinner(idleMinutesSpinner);
        idleMinutesSpinner.valueProperty().addListener((obs, o, n) -> fireModified());
        minutesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox idleRow = new HBox(8, stopIfIdleCheck, idleMinutesSpinner, minutesLabel);
        idleRow.setAlignment(Pos.CENTER_LEFT);

        // 4. Start in project dir row
        styleCheckBox(startProcessInProjectDirCheck);
        startProcessInProjectDirCheck.setOnAction(e -> fireModified());
        styleHelpIcon(helpStartDir, "When enabled, sets the compile server process current directory to the project directory");
        HBox startDirRow = new HBox(6, startProcessInProjectDirCheck, helpStartDir);
        startDirRow.setAlignment(Pos.CENTER_LEFT);

        serverOptionsBox.setPadding(new Insets(2, 0, 0, 16));
        serverOptionsBox.getChildren().addAll(parallelRow, idleRow, startDirRow);

        // 5. JVM Section
        Label jvmHeader = new Label("JVM");
        jvmHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 2 0;");

        GridPane jvmGrid = new GridPane();
        jvmGrid.setHgap(16);
        jvmGrid.setVgap(10);
        jvmGrid.getColumnConstraints().addAll(
                new ColumnConstraints(160),
                new ColumnConstraints(240, 420, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true)
        );

        // JDK row
        Label jdkLabel = new Label("JDK:");
        jdkLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        populateJdkOptions();
        styleComboBox(jdkCombo);
        jdkCombo.setMaxWidth(Double.MAX_VALUE);
        jdkCombo.setOnAction(e -> fireModified());
        styleHelpIcon(helpJdk, "JDK used to run the compile server process");
        HBox jdkBox = new HBox(6, jdkCombo, helpJdk);
        jdkBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(jdkCombo, Priority.ALWAYS);
        jvmGrid.addRow(0, jdkLabel, jdkBox);

        // Maximum heap size
        Label heapLabel = new Label("Maximum heap size, MB:");
        heapLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        styleTextField(maxHeapSizeField);
        maxHeapSizeField.setPrefWidth(120);
        maxHeapSizeField.setMaxWidth(160);
        maxHeapSizeField.textProperty().addListener((obs, o, n) -> fireModified());
        jvmGrid.addRow(1, heapLabel, maxHeapSizeField);

        // VM options
        Label vmLabel = new Label("VM options:");
        vmLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        styleTextField(vmOptionsField);
        HBox.setHgrow(vmOptionsField, Priority.ALWAYS);
        vmOptionsField.textProperty().addListener((obs, o, n) -> fireModified());
        styleMiniButton(expandVmOptionsBtn);
        expandVmOptionsBtn.setOnAction(e -> openExpandDialog("VM options", vmOptionsField));
        HBox vmBoxRow = new HBox(6, vmOptionsField, expandVmOptionsBtn);
        vmBoxRow.setAlignment(Pos.CENTER_LEFT);
        jvmGrid.addRow(2, vmLabel, vmBoxRow);

        jvmBox.getChildren().addAll(jvmHeader, jvmGrid);

        getChildren().addAll(useCompileServerCheck, serverOptionsBox, jvmBox);
    }

    private void populateJdkOptions() {
        String rec = ScalaCompileServerSettings.detectRecommendedJdk();
        jdkCombo.getItems().clear();
        jdkCombo.getItems().add(rec);
        try {
            List<JdkMetadata.JdkInstallation> list = JdkMetadata.detectInstallations(false);
            for (JdkMetadata.JdkInstallation inst : list) {
                String label = inst.formatDisplay();
                if (!jdkCombo.getItems().contains(label) && !rec.contains(label)) {
                    jdkCombo.getItems().add(label);
                }
            }
        } catch (Throwable ignored) {}
        jdkCombo.setValue(rec);
    }

    private void updateEnabledStates() {
        boolean enabled = useCompileServerCheck.isSelected();
        serverOptionsBox.setDisable(!enabled);
        jvmBox.setDisable(!enabled);
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleTextField(TextField f) {
        f.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
    }

    private void styleComboBox(ComboBox<?> c) {
        c.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
    }

    private void styleSpinner(Spinner<?> s) {
        s.setPrefWidth(72);
        s.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void styleMiniButton(Button b) {
        b.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
    }

    private void styleHelpIcon(Label l, String tooltipText) {
        l.setStyle("-fx-text-fill: #8C8C8C; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 0 4;");
        Tooltip.install(l, new Tooltip(tooltipText));
    }

    private void openExpandDialog(String title, TextField targetField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");

        TextArea area = new TextArea(targetField.getText());
        area.setWrapText(true);
        area.setPrefSize(420, 200);
        area.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-family: monospace; -fx-border-color: #43454A;");

        VBox box = new VBox(8, area);
        box.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(box);

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == okType) return area.getText();
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            targetField.setText(res);
            fireModified();
        });
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        useCompileServerCheck.setSelected(initialSettings.isUseCompileServer());
        parallelModulesCheck.setSelected(initialSettings.isCompileIndependentModulesInParallel());
        parallelThreadsSpinner.getValueFactory().setValue(initialSettings.getParallelThreads());
        stopIfIdleCheck.setSelected(initialSettings.isStopIfIdle());
        idleMinutesSpinner.getValueFactory().setValue(initialSettings.getIdleTimeoutMinutes());
        startProcessInProjectDirCheck.setSelected(initialSettings.isStartProcessInProjectDirectory());

        if (initialSettings.getJdk() != null && !initialSettings.getJdk().isBlank()) {
            if (!jdkCombo.getItems().contains(initialSettings.getJdk())) {
                jdkCombo.getItems().add(initialSettings.getJdk());
            }
            jdkCombo.setValue(initialSettings.getJdk());
        }
        maxHeapSizeField.setText(String.valueOf(initialSettings.getMaximumHeapSizeMb()));
        vmOptionsField.setText(initialSettings.getVmOptions());

        updateEnabledStates();
        updating = false;
    }

    public ScalaCompileServerSettings getCurrentSettings() {
        ScalaCompileServerSettings s = new ScalaCompileServerSettings();
        s.setUseCompileServer(useCompileServerCheck.isSelected());
        s.setCompileIndependentModulesInParallel(parallelModulesCheck.isSelected());
        s.setParallelThreads(parallelThreadsSpinner.getValue());
        s.setStopIfIdle(stopIfIdleCheck.isSelected());
        s.setIdleTimeoutMinutes(idleMinutesSpinner.getValue());
        s.setStartProcessInProjectDirectory(startProcessInProjectDirCheck.isSelected());

        s.setJdk(jdkCombo.getValue());
        try {
            s.setMaximumHeapSizeMb(Integer.parseInt(maxHeapSizeField.getText().trim()));
        } catch (NumberFormatException ignored) {}
        s.setVmOptions(vmOptionsField.getText());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        ScalaCompileServerSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadData();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public CheckBox getUseCompileServerCheck() { return useCompileServerCheck; }
    public CheckBox getParallelModulesCheck() { return parallelModulesCheck; }
    public Spinner<Integer> getParallelThreadsSpinner() { return parallelThreadsSpinner; }
    public CheckBox getStopIfIdleCheck() { return stopIfIdleCheck; }
    public Spinner<Integer> getIdleMinutesSpinner() { return idleMinutesSpinner; }
    public CheckBox getStartProcessInProjectDirCheck() { return startProcessInProjectDirCheck; }
    public ComboBox<String> getJdkCombo() { return jdkCombo; }
    public TextField getMaxHeapSizeField() { return maxHeapSizeField; }
    public TextField getVmOptionsField() { return vmOptionsField; }
}
