package dev.lumina.ui;

import dev.lumina.tools.KotlinNotebookSettings;
import dev.lumina.tools.KotlinNotebookSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > Kotlin Notebook settings page in Lumina IDE.
 * Matches 1:1 with screenshots from IDE settings.
 */
public class SettingsToolsKotlinNotebookPage extends VBox {

    private final KotlinNotebookSettingsManager manager;
    private KotlinNotebookSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // JVM and Build
    private ComboBox<String> kernelVersionCombo;
    private Button revertKernelButton;
    private Button browseKernelButton;
    private ComboBox<String> jdkPathCombo;
    private ComboBox<String> jvmTargetCombo;
    private Spinner<Integer> maxHeapSizeSpinner;
    private TextField jvmExtraArgsField;
    private Button expandExtraArgsButton;
    private TextField envVarsField;
    private Button browseEnvVarsButton;

    // Debug Options
    private CheckBox showNotebookSessionVarsCheck;
    private CheckBox openVarsTabCheck;

    // Kernel Session
    private CheckBox stopExecutionOnFailureCheck;
    private CheckBox resolveSourcesCheck;
    private CheckBox resolveMultiplatformDepsCheck;

    // Outputs
    private CheckBox renderKandyPlotsCheck;
    private CheckBox renderDataFrameTablesCheck;

    // Type Hints
    private CheckBox showTypeHintsOnlyActiveCellCheck;

    // Appearance
    private CheckBox displayExecutionCountCheck;
    private CheckBox showFoldableRegionsCheck;

    public SettingsToolsKotlinNotebookPage() {
        this.manager = KotlinNotebookSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        VBox contentBox = new VBox(14);
        contentBox.setPadding(new Insets(16, 24, 24, 24));
        contentBox.setStyle("-fx-background-color: #1E1F22;");

        // 1. JVM and Build
        HBox jvmBuildHeader = createSectionHeader("JVM and Build");
        GridPane jvmGrid = new GridPane();
        jvmGrid.setHgap(12);
        jvmGrid.setVgap(8);
        jvmGrid.setAlignment(Pos.CENTER_LEFT);

        // Kernel Version
        Label kernelLabel = createFieldLabel("Kernel version");
        kernelVersionCombo = new ComboBox<>();
        kernelVersionCombo.getItems().addAll("0.15.1-761-1 (bundled)", "0.15.0", "0.14.2", "0.14.0");
        kernelVersionCombo.setValue("0.15.1-761-1 (bundled)");
        styleControl(kernelVersionCombo);
        kernelVersionCombo.setPrefWidth(260);

        revertKernelButton = new Button("↶");
        styleIconButton(revertKernelButton, "Reset to bundled kernel version");
        revertKernelButton.setOnAction(e -> kernelVersionCombo.setValue(KotlinNotebookSettings.DEFAULT_KERNEL_VERSION));

        browseKernelButton = new Button("📁");
        styleIconButton(browseKernelButton, "Browse custom kernel directory");

        HBox kernelBox = new HBox(6, kernelVersionCombo, revertKernelButton, browseKernelButton);
        kernelBox.setAlignment(Pos.CENTER_LEFT);

        Label kernelHint = createHintLabel("Choosing a kernel version other than bundled may lead to compatibility issues");
        VBox kernelFieldBox = new VBox(4, kernelBox, kernelHint);

        jvmGrid.add(kernelLabel, 0, 0);
        jvmGrid.add(kernelFieldBox, 1, 0);

        // JDK Path
        Label jdkLabel = createFieldLabel("JDK path");
        jdkPathCombo = new ComboBox<>();
        jdkPathCombo.getItems().addAll("Project SDK 25", "Project SDK 21", "Project SDK 17", "Add SDK...");
        jdkPathCombo.setValue("Project SDK 25");
        styleControl(jdkPathCombo);
        jdkPathCombo.setPrefWidth(320);

        Label jdkHint = createHintLabel("JDK version should be at least 11");
        VBox jdkFieldBox = new VBox(4, jdkPathCombo, jdkHint);

        jvmGrid.add(jdkLabel, 0, 1);
        jvmGrid.add(jdkFieldBox, 1, 1);

        // JVM Target for Snippets
        Label targetLabel = createFieldLabel("JVM target for snippets");
        jvmTargetCombo = new ComboBox<>();
        jvmTargetCombo.getItems().addAll("Selected JDK default", "25", "21", "17", "11", "8");
        jvmTargetCombo.setValue("Selected JDK default");
        styleControl(jvmTargetCombo);
        jvmTargetCombo.setPrefWidth(320);

        Label targetHint = createHintLabel("Target for snippets could be at most 21 (IDE JDK version)");
        VBox targetFieldBox = new VBox(4, jvmTargetCombo, targetHint);

        jvmGrid.add(targetLabel, 0, 2);
        jvmGrid.add(targetFieldBox, 1, 2);

        // Max heap size
        Label heapLabel = createFieldLabel("Max heap size");
        maxHeapSizeSpinner = new Spinner<>();
        maxHeapSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(256, 65536, 3256, 256));
        maxHeapSizeSpinner.setEditable(true);
        maxHeapSizeSpinner.setPrefWidth(85);
        styleControl(maxHeapSizeSpinner);

        Label mibLabel = new Label("MiB");
        mibLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox heapBox = new HBox(8, maxHeapSizeSpinner, mibLabel);
        heapBox.setAlignment(Pos.CENTER_LEFT);

        jvmGrid.add(heapLabel, 0, 3);
        jvmGrid.add(heapBox, 1, 3);

        // JVM extra arguments
        Label extraArgsLabel = createFieldLabel("JVM extra arguments");
        jvmExtraArgsField = new TextField();
        jvmExtraArgsField.setPromptText("");
        jvmExtraArgsField.setPrefWidth(290);
        styleControl(jvmExtraArgsField);

        expandExtraArgsButton = new Button("⤢");
        styleIconButton(expandExtraArgsButton, "Edit extra arguments");
        expandExtraArgsButton.setOnAction(e -> openTextEditDialog("JVM extra arguments", jvmExtraArgsField));

        HBox extraArgsBox = new HBox(6, jvmExtraArgsField, expandExtraArgsButton);
        extraArgsBox.setAlignment(Pos.CENTER_LEFT);

        Label extraArgsHint = createHintLabel("These arguments are applied only in \"Separate process\" mode");
        VBox extraArgsFieldBox = new VBox(4, extraArgsBox, extraArgsHint);

        jvmGrid.add(extraArgsLabel, 0, 4);
        jvmGrid.add(extraArgsFieldBox, 1, 4);

        // Environment variables
        Label envVarsLabel = createFieldLabel("Environment variables");
        envVarsField = new TextField();
        envVarsField.setPromptText("Environment variables");
        envVarsField.setPrefWidth(290);
        styleControl(envVarsField);

        browseEnvVarsButton = new Button("🗂");
        styleIconButton(browseEnvVarsButton, "Edit environment variables");
        browseEnvVarsButton.setOnAction(e -> openTextEditDialog("Environment variables", envVarsField));

        HBox envVarsBox = new HBox(6, envVarsField, browseEnvVarsButton);
        envVarsBox.setAlignment(Pos.CENTER_LEFT);

        Label envVarsHint = createHintLabel("Separate variables with semicolon: VAR=value; VAR1=value1");
        VBox envVarsFieldBox = new VBox(4, envVarsBox, envVarsHint);

        jvmGrid.add(envVarsLabel, 0, 5);
        jvmGrid.add(envVarsFieldBox, 1, 5);

        // 2. Debug Options
        HBox debugHeader = createSectionHeader("Debug Options");
        Label debugModeHint = createHintLabel("Debug features are available only when a notebook runs in Separate Process mode");

        showNotebookSessionVarsCheck = createCheckBox("Show notebook session variables");
        Label sessionVarsHint = createHintLabel("Kernel version should be at least 0.12.0-137 for debug instrumentation to take effect");
        VBox sessionVarsBox = new VBox(4, showNotebookSessionVarsCheck, sessionVarsHint);

        openVarsTabCheck = createCheckBox("Open the variables tab after cell execution");
        VBox debugGroup = new VBox(8, debugModeHint, sessionVarsBox, openVarsTabCheck);

        // 3. Kernel Session
        HBox kernelSessionHeader = createSectionHeader("Kernel Session");
        stopExecutionOnFailureCheck = createCheckBox("Stop execution on failure");
        Label stopHint = createHintLabel("When multiple cells are run, execution will be stopped after the first failure");
        VBox stopExecutionBox = new VBox(4, stopExecutionOnFailureCheck, stopHint);

        resolveSourcesCheck = createCheckBox("Resolve sources");
        resolveMultiplatformDepsCheck = createCheckBox("Resolve multiplatform dependencies");
        VBox kernelSessionGroup = new VBox(8, stopExecutionBox, resolveSourcesCheck, resolveMultiplatformDepsCheck);

        // 4. Outputs
        HBox outputsHeader = createSectionHeader("Outputs");
        renderKandyPlotsCheck = createCheckBox("Render Kandy plots natively");
        renderDataFrameTablesCheck = createCheckBox("Render DataFrame tables natively");
        VBox outputsGroup = new VBox(8, renderKandyPlotsCheck, renderDataFrameTablesCheck);

        // 5. Type Hints
        HBox typeHintsHeader = createSectionHeader("Type Hints");
        showTypeHintsOnlyActiveCellCheck = createCheckBox("Show type hints only in the active cell");
        VBox typeHintsGroup = new VBox(8, showTypeHintsOnlyActiveCellCheck);

        // 6. Appearance
        HBox appearanceHeader = createSectionHeader("Appearance");
        displayExecutionCountCheck = createCheckBox("Display execution count");
        showFoldableRegionsCheck = createCheckBox("Show foldable regions");
        VBox appearanceGroup = new VBox(8, displayExecutionCountCheck, showFoldableRegionsCheck);

        contentBox.getChildren().addAll(
                jvmBuildHeader, jvmGrid,
                debugHeader, debugGroup,
                kernelSessionHeader, kernelSessionGroup,
                outputsHeader, outputsGroup,
                typeHintsHeader, typeHintsGroup,
                appearanceHeader, appearanceGroup
        );

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().add(scrollPane);

        setupListeners();
    }

    private void setupListeners() {
        kernelVersionCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        jdkPathCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        jvmTargetCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        maxHeapSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        jvmExtraArgsField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        envVarsField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        showNotebookSessionVarsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            openVarsTabCheck.setDisable(!newVal);
            notifyModified();
        });

        openVarsTabCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        stopExecutionOnFailureCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        resolveSourcesCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        resolveMultiplatformDepsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        renderKandyPlotsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        renderDataFrameTablesCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        showTypeHintsOnlyActiveCellCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        displayExecutionCountCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        showFoldableRegionsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
    }

    private void openTextEditDialog(String title, TextField targetField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Edit " + title);

        TextArea textArea = new TextArea(targetField.getText());
        textArea.setPrefRowCount(8);
        textArea.setPrefColumnCount(40);
        textArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-family: monospace;");

        dialog.getDialogPane().setContent(new VBox(10, textArea));
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == javafx.scene.control.ButtonType.OK ? textArea.getText().trim() : null);

        dialog.showAndWait().ifPresent(res -> {
            targetField.setText(res);
            notifyModified();
        });
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(KotlinNotebookSettings s) {
        if (s == null) return;
        kernelVersionCombo.setValue(s.getKernelVersion());
        jdkPathCombo.setValue(s.getJdkPath());
        jvmTargetCombo.setValue(s.getJvmTargetForSnippets());
        if (maxHeapSizeSpinner.getValueFactory() != null) {
            maxHeapSizeSpinner.getValueFactory().setValue(s.getMaxHeapSize());
        }
        jvmExtraArgsField.setText(s.getJvmExtraArguments());
        envVarsField.setText(s.getEnvironmentVariables());

        showNotebookSessionVarsCheck.setSelected(s.isShowNotebookSessionVariables());
        openVarsTabCheck.setSelected(s.isOpenVariablesTabAfterCellExecution());
        openVarsTabCheck.setDisable(!s.isShowNotebookSessionVariables());

        stopExecutionOnFailureCheck.setSelected(s.isStopExecutionOnFailure());
        resolveSourcesCheck.setSelected(s.isResolveSources());
        resolveMultiplatformDepsCheck.setSelected(s.isResolveMultiplatformDependencies());

        renderKandyPlotsCheck.setSelected(s.isRenderKandyPlotsNatively());
        renderDataFrameTablesCheck.setSelected(s.isRenderDataFrameTablesNatively());

        showTypeHintsOnlyActiveCellCheck.setSelected(s.isShowTypeHintsOnlyInActiveCell());

        displayExecutionCountCheck.setSelected(s.isDisplayExecutionCount());
        showFoldableRegionsCheck.setSelected(s.isShowFoldableRegions());
    }

    private KotlinNotebookSettings getCurrentSettingsFromUI() {
        KotlinNotebookSettings s = new KotlinNotebookSettings();
        if (kernelVersionCombo.getValue() != null) s.setKernelVersion(kernelVersionCombo.getValue());
        if (jdkPathCombo.getValue() != null) s.setJdkPath(jdkPathCombo.getValue());
        if (jvmTargetCombo.getValue() != null) s.setJvmTargetForSnippets(jvmTargetCombo.getValue());
        if (maxHeapSizeSpinner.getValue() != null) s.setMaxHeapSize(maxHeapSizeSpinner.getValue());
        s.setJvmExtraArguments(jvmExtraArgsField.getText());
        s.setEnvironmentVariables(envVarsField.getText());

        s.setShowNotebookSessionVariables(showNotebookSessionVarsCheck.isSelected());
        s.setOpenVariablesTabAfterCellExecution(openVarsTabCheck.isSelected());

        s.setStopExecutionOnFailure(stopExecutionOnFailureCheck.isSelected());
        s.setResolveSources(resolveSourcesCheck.isSelected());
        s.setResolveMultiplatformDependencies(resolveMultiplatformDepsCheck.isSelected());

        s.setRenderKandyPlotsNatively(renderKandyPlotsCheck.isSelected());
        s.setRenderDataFrameTablesNatively(renderDataFrameTablesCheck.isSelected());

        s.setShowTypeHintsOnlyInActiveCell(showTypeHintsOnlyActiveCellCheck.isSelected());

        s.setDisplayExecutionCount(displayExecutionCountCheck.isSelected());
        s.setShowFoldableRegions(showFoldableRegionsCheck.isSelected());

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        KotlinNotebookSettings current = getCurrentSettingsFromUI();
        return !Objects.equals(initialSettings, current);
    }

    public void apply() {
        KotlinNotebookSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            updating = true;
            try {
                applySettingsToUI(initialSettings);
            } finally {
                updating = false;
            }
            notifyModified();
        }
    }

    public void reset() {
        revertChanges();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        l.setMinWidth(160);
        return l;
    }

    private Label createHintLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #7A7E85; -fx-font-size: 11px;");
        l.setWrapText(true);
        return l;
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return cb;
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        header.getChildren().addAll(label, sep);
        return header;
    }

    private void styleControl(javafx.scene.control.Control control) {
        control.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void styleIconButton(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-min-width: 26px; -fx-min-height: 26px;");
        if (tooltipText != null) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
    }

    // Getters for testing and external access
    public ComboBox<String> getKernelVersionCombo() {
        return kernelVersionCombo;
    }

    public ComboBox<String> getJdkPathCombo() {
        return jdkPathCombo;
    }

    public ComboBox<String> getJvmTargetCombo() {
        return jvmTargetCombo;
    }

    public Spinner<Integer> getMaxHeapSizeSpinner() {
        return maxHeapSizeSpinner;
    }

    public TextField getJvmExtraArgsField() {
        return jvmExtraArgsField;
    }

    public TextField getEnvVarsField() {
        return envVarsField;
    }

    public CheckBox getShowNotebookSessionVarsCheck() {
        return showNotebookSessionVarsCheck;
    }

    public CheckBox getOpenVarsTabCheck() {
        return openVarsTabCheck;
    }

    public CheckBox getStopExecutionOnFailureCheck() {
        return stopExecutionOnFailureCheck;
    }

    public CheckBox getResolveSourcesCheck() {
        return resolveSourcesCheck;
    }

    public CheckBox getResolveMultiplatformDepsCheck() {
        return resolveMultiplatformDepsCheck;
    }

    public CheckBox getRenderKandyPlotsCheck() {
        return renderKandyPlotsCheck;
    }

    public CheckBox getRenderDataFrameTablesCheck() {
        return renderDataFrameTablesCheck;
    }

    public CheckBox getShowTypeHintsOnlyActiveCellCheck() {
        return showTypeHintsOnlyActiveCellCheck;
    }

    public CheckBox getDisplayExecutionCountCheck() {
        return displayExecutionCountCheck;
    }

    public CheckBox getShowFoldableRegionsCheck() {
        return showFoldableRegionsCheck;
    }

    public Button getRevertKernelButton() {
        return revertKernelButton;
    }
}
