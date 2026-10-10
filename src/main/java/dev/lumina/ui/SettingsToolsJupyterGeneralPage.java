package dev.lumina.ui;

import dev.lumina.tools.JupyterGeneralSettings;
import dev.lumina.tools.JupyterGeneralSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > Jupyter > Jupyter General settings page matching Image 4.
 */
public class SettingsToolsJupyterGeneralPage extends VBox {

    private final JupyterGeneralSettingsManager manager;
    private JupyterGeneralSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Appearance
    private CheckBox showAddCellPopupCheck;
    private CheckBox showRunDebugActionsCheck;
    private CheckBox invertImageOutputsCheck;
    private TextField maxOutputHeightField;
    private CheckBox asciiColoringErrorsCheck;

    // Markdown
    private TextField markdownFontScaleField;
    private CheckBox renderMarkdownAutoCheck;

    // Variables
    private CheckBox openVariablesFirstCheck;
    private CheckBox showInlineValuesCheck;
    private ToggleGroup inlineValuesGroup;
    private RadioButton currentLineRadio;
    private RadioButton entireNotebookRadio;
    private HBox inlineValuesScopeBox;

    // Execution
    private CheckBox notifyExecutionExceedsCheck;
    private TextField executionTimeoutField;
    private CheckBox showTimestampLabelCheck;
    private ToggleGroup executionTimeGroup;
    private RadioButton detailedTimeRadio;
    private RadioButton compactTimeRadio;
    private RadioButton hiddenTimeRadio;
    private CheckBox includeSourceRootsPathCheck;

    // Other
    private CheckBox uploadSupportLibsCheck;

    public SettingsToolsJupyterGeneralPage() {
        this.manager = JupyterGeneralSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Appearance Section
        HBox appHeader = createDividerHeader("Appearance");

        showAddCellPopupCheck = createCheckBox("Show the \"Add Cell\" pop-up in the editor");
        Label addCellSub = createSubtitle("Reopen the editor to apply");

        showRunDebugActionsCheck = createCheckBox("Show Run & Debug actions in Jupyter files");
        Label runDebugSub = createSubtitle("Stops hiding Run & Debug actions from the Main Toolbar when a Jupyter file is open");

        invertImageOutputsCheck = createCheckBox("Invert image outputs for dark themes");
        Label invertSub = createSubtitle("Reopen the editor to apply");

        HBox maxOutputRow = new HBox(8);
        maxOutputRow.setAlignment(Pos.CENTER_LEFT);
        Label maxOutputLabel = createLabel("Max output height in text lines:");
        maxOutputHeightField = createNumberTextField("-1", 50);
        Label maxOutputHint = createSubtitle("Use -1 for auto-scaling to 30% of screen height");
        maxOutputRow.getChildren().addAll(maxOutputLabel, maxOutputHeightField, maxOutputHint);

        asciiColoringErrorsCheck = createCheckBox("ASCII coloring in error outputs");

        VBox appearanceBox = new VBox(6,
                appHeader,
                showAddCellPopupCheck, addCellSub,
                showRunDebugActionsCheck, runDebugSub,
                invertImageOutputsCheck, invertSub,
                maxOutputRow,
                asciiColoringErrorsCheck
        );

        // 2. Markdown Section
        HBox mdHeader = createDividerHeader("Markdown");

        HBox fontScaleRow = new HBox(8);
        fontScaleRow.setAlignment(Pos.CENTER_LEFT);
        Label fontScaleLabel = createLabel("Markdown font scale:");
        markdownFontScaleField = createNumberTextField("100", 50);
        Label percentLabel = createLabel("%");
        Label fontScaleSub = createSubtitle("Reopen the editor to apply");
        fontScaleRow.getChildren().addAll(fontScaleLabel, markdownFontScaleField, percentLabel, fontScaleSub);

        renderMarkdownAutoCheck = createCheckBox("Render Markdown cells automatically");
        Label renderAutoSub = createSubtitle("Automatically run Markdown cells when not in the edit mode");

        VBox markdownBox = new VBox(6,
                mdHeader,
                fontScaleRow,
                renderMarkdownAutoCheck, renderAutoSub
        );

        // 3. Variables Section
        HBox varHeader = createDividerHeader("Variables");

        openVariablesFirstCheck = createCheckBox("Open the Variables tool window on the first cell execution");
        Label openVarSub = createSubtitle("Automatically opens the Variables tool window on the first cell execution");

        showInlineValuesCheck = createCheckBox("Show inline values:");

        inlineValuesGroup = new ToggleGroup();
        currentLineRadio = createRadioButton("In the current line", inlineValuesGroup);
        entireNotebookRadio = createRadioButton("In the entire notebook", inlineValuesGroup);
        inlineValuesScopeBox = new HBox(16, currentLineRadio, entireNotebookRadio);
        inlineValuesScopeBox.setPadding(new Insets(2, 0, 0, 20));

        VBox variablesBox = new VBox(6,
                varHeader,
                openVariablesFirstCheck, openVarSub,
                showInlineValuesCheck, inlineValuesScopeBox
        );

        // 4. Execution Section
        HBox execHeader = createDividerHeader("Execution");

        HBox notifyRow = new HBox(8);
        notifyRow.setAlignment(Pos.CENTER_LEFT);
        notifyExecutionExceedsCheck = createCheckBox("Notify when cell execution time exceeds");
        executionTimeoutField = createNumberTextField("60", 45);
        Label secondsLabel = createLabel("seconds");
        notifyRow.getChildren().addAll(notifyExecutionExceedsCheck, executionTimeoutField, secondsLabel);

        showTimestampLabelCheck = createCheckBox("Show the timestamp on the execution label");

        HBox execModeRow = new HBox(12);
        execModeRow.setAlignment(Pos.CENTER_LEFT);
        Label execModeLabel = createLabel("Execution time display mode:");
        executionTimeGroup = new ToggleGroup();
        detailedTimeRadio = createRadioButton("Detailed", executionTimeGroup);
        compactTimeRadio = createRadioButton("Compact (recommended)", executionTimeGroup);
        hiddenTimeRadio = createRadioButton("Hidden", executionTimeGroup);
        execModeRow.getChildren().addAll(execModeLabel, detailedTimeRadio, compactTimeRadio, hiddenTimeRadio);

        includeSourceRootsPathCheck = createCheckBox("Include project source roots to PYTHONPATH");

        VBox executionBox = new VBox(6,
                execHeader,
                notifyRow,
                showTimestampLabelCheck,
                execModeRow,
                includeSourceRootsPathCheck
        );

        // 5. Other Section
        HBox otherHeader = createDividerHeader("Other");
        uploadSupportLibsCheck = createCheckBox("Upload support libs to the Jupyter server");
        Label uploadSub = createSubtitle("Restart the kernel to apply");

        VBox otherBox = new VBox(6, otherHeader, uploadSupportLibsCheck, uploadSub);

        getChildren().addAll(appearanceBox, markdownBox, variablesBox, executionBox, otherBox);

        setupListeners();
    }

    private void setupListeners() {
        showAddCellPopupCheck.setOnAction(e -> notifyModified());
        showRunDebugActionsCheck.setOnAction(e -> notifyModified());
        invertImageOutputsCheck.setOnAction(e -> notifyModified());
        maxOutputHeightField.textProperty().addListener((obs, oldV, newV) -> notifyModified());
        asciiColoringErrorsCheck.setOnAction(e -> notifyModified());

        markdownFontScaleField.textProperty().addListener((obs, oldV, newV) -> notifyModified());
        renderMarkdownAutoCheck.setOnAction(e -> notifyModified());

        openVariablesFirstCheck.setOnAction(e -> notifyModified());
        showInlineValuesCheck.setOnAction(e -> {
            inlineValuesScopeBox.setDisable(!showInlineValuesCheck.isSelected());
            notifyModified();
        });
        currentLineRadio.setOnAction(e -> notifyModified());
        entireNotebookRadio.setOnAction(e -> notifyModified());

        notifyExecutionExceedsCheck.setOnAction(e -> notifyModified());
        executionTimeoutField.textProperty().addListener((obs, oldV, newV) -> notifyModified());
        showTimestampLabelCheck.setOnAction(e -> notifyModified());
        detailedTimeRadio.setOnAction(e -> notifyModified());
        compactTimeRadio.setOnAction(e -> notifyModified());
        hiddenTimeRadio.setOnAction(e -> notifyModified());
        includeSourceRootsPathCheck.setOnAction(e -> notifyModified());

        uploadSupportLibsCheck.setOnAction(e -> notifyModified());
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return cb;
    }

    private RadioButton createRadioButton(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return rb;
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private Label createSubtitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px;");
        l.setPadding(new Insets(0, 0, 2, 22));
        return l;
    }

    private TextField createNumberTextField(String value, double width) {
        TextField tf = new TextField(value);
        tf.setPrefWidth(width);
        tf.setMaxWidth(width);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6;");
        return tf;
    }

    private HBox createDividerHeader(String text) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 0, 4, 0));

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        header.getChildren().addAll(label, sep);
        return header;
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

    private void applySettingsToUI(JupyterGeneralSettings s) {
        if (s == null) return;
        showAddCellPopupCheck.setSelected(s.isShowAddCellPopup());
        showRunDebugActionsCheck.setSelected(s.isShowRunAndDebugActions());
        invertImageOutputsCheck.setSelected(s.isInvertImageOutputsForDarkThemes());
        maxOutputHeightField.setText(String.valueOf(s.getMaxOutputHeightInTextLines()));
        asciiColoringErrorsCheck.setSelected(s.isAsciiColoringInErrorOutputs());

        markdownFontScaleField.setText(String.valueOf(s.getMarkdownFontScale()));
        renderMarkdownAutoCheck.setSelected(s.isRenderMarkdownCellsAutomatically());

        openVariablesFirstCheck.setSelected(s.isOpenVariablesOnFirstCellExecution());
        showInlineValuesCheck.setSelected(s.isShowInlineValues());
        if ("In the entire notebook".equals(s.getInlineValuesScope())) {
            entireNotebookRadio.setSelected(true);
        } else {
            currentLineRadio.setSelected(true);
        }
        inlineValuesScopeBox.setDisable(!s.isShowInlineValues());

        notifyExecutionExceedsCheck.setSelected(s.isNotifyWhenCellExecutionExceeds());
        executionTimeoutField.setText(String.valueOf(s.getCellExecutionTimeoutSeconds()));
        showTimestampLabelCheck.setSelected(s.isShowTimestampOnExecutionLabel());

        String mode = s.getExecutionTimeDisplayMode();
        if ("Detailed".equalsIgnoreCase(mode)) {
            detailedTimeRadio.setSelected(true);
        } else if ("Hidden".equalsIgnoreCase(mode)) {
            hiddenTimeRadio.setSelected(true);
        } else {
            compactTimeRadio.setSelected(true);
        }

        includeSourceRootsPathCheck.setSelected(s.isIncludeProjectSourceRootsToPythonPath());
        uploadSupportLibsCheck.setSelected(s.isUploadSupportLibsToJupyterServer());
    }

    private JupyterGeneralSettings getCurrentSettingsFromUI() {
        JupyterGeneralSettings s = new JupyterGeneralSettings();
        s.setShowAddCellPopup(showAddCellPopupCheck.isSelected());
        s.setShowRunAndDebugActions(showRunDebugActionsCheck.isSelected());
        s.isInvertImageOutputsForDarkThemes();
        s.setInvertImageOutputsForDarkThemes(invertImageOutputsCheck.isSelected());

        try {
            s.setMaxOutputHeightInTextLines(Integer.parseInt(maxOutputHeightField.getText().trim()));
        } catch (NumberFormatException e) {
            s.setMaxOutputHeightInTextLines(-1);
        }

        s.setAsciiColoringInErrorOutputs(asciiColoringErrorsCheck.isSelected());

        try {
            s.setMarkdownFontScale(Integer.parseInt(markdownFontScaleField.getText().trim()));
        } catch (NumberFormatException e) {
            s.setMarkdownFontScale(100);
        }

        s.setRenderMarkdownCellsAutomatically(renderMarkdownAutoCheck.isSelected());

        s.setOpenVariablesOnFirstCellExecution(openVariablesFirstCheck.isSelected());
        s.setShowInlineValues(showInlineValuesCheck.isSelected());
        s.setInlineValuesScope(entireNotebookRadio.isSelected() ? "In the entire notebook" : "In the current line");

        s.setNotifyWhenCellExecutionExceeds(notifyExecutionExceedsCheck.isSelected());
        try {
            s.setCellExecutionTimeoutSeconds(Integer.parseInt(executionTimeoutField.getText().trim()));
        } catch (NumberFormatException e) {
            s.setCellExecutionTimeoutSeconds(60);
        }

        s.setShowTimestampOnExecutionLabel(showTimestampLabelCheck.isSelected());

        if (detailedTimeRadio.isSelected()) {
            s.setExecutionTimeDisplayMode("Detailed");
        } else if (hiddenTimeRadio.isSelected()) {
            s.setExecutionTimeDisplayMode("Hidden");
        } else {
            s.setExecutionTimeDisplayMode("Compact (recommended)");
        }

        s.setIncludeProjectSourceRootsToPythonPath(includeSourceRootsPathCheck.isSelected());
        s.setUploadSupportLibsToJupyterServer(uploadSupportLibsCheck.isSelected());

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        JupyterGeneralSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void resetDefaults() {
        applySettingsToUI(new JupyterGeneralSettings());
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Accessors for testing
    public CheckBox getShowAddCellPopupCheck() {
        return showAddCellPopupCheck;
    }

    public CheckBox getShowRunDebugActionsCheck() {
        return showRunDebugActionsCheck;
    }

    public CheckBox getInvertImageOutputsCheck() {
        return invertImageOutputsCheck;
    }

    public TextField getMaxOutputHeightField() {
        return maxOutputHeightField;
    }

    public CheckBox getAsciiColoringErrorsCheck() {
        return asciiColoringErrorsCheck;
    }

    public TextField getMarkdownFontScaleField() {
        return markdownFontScaleField;
    }

    public CheckBox getRenderMarkdownAutoCheck() {
        return renderMarkdownAutoCheck;
    }

    public CheckBox getOpenVariablesFirstCheck() {
        return openVariablesFirstCheck;
    }

    public CheckBox getShowInlineValuesCheck() {
        return showInlineValuesCheck;
    }

    public RadioButton getCurrentLineRadio() {
        return currentLineRadio;
    }

    public RadioButton getEntireNotebookRadio() {
        return entireNotebookRadio;
    }

    public CheckBox getNotifyExecutionExceedsCheck() {
        return notifyExecutionExceedsCheck;
    }

    public TextField getExecutionTimeoutField() {
        return executionTimeoutField;
    }

    public RadioButton getCompactTimeRadio() {
        return compactTimeRadio;
    }

    public CheckBox getIncludeSourceRootsPathCheck() {
        return includeSourceRootsPathCheck;
    }

    public CheckBox getUploadSupportLibsCheck() {
        return uploadSupportLibsCheck;
    }
}
