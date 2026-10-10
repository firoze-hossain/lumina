package dev.lumina.ui;

import dev.lumina.tools.PythonIntegratedToolsSettings;
import dev.lumina.tools.PythonIntegratedToolsSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Tools > Python Integrated Tools settings page in Lumina IDE.
 * Matches screenshot 2 1:1 with dynamic configuration.
 */
public class SettingsToolsPythonIntegratedToolsPage extends VBox {

    private final PythonIntegratedToolsSettingsManager manager;
    private PythonIntegratedToolsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Packaging
    private TextField packageRequirementsField;
    private Button browsePackageReqButton;

    // Pipenv
    private TextField pipenvPathField;
    private Button browsePipenvButton;

    // Testing
    private ComboBox<String> testRunnerCombo;
    private CheckBox detectTestsInJupyterCheck;

    // Docstrings
    private ComboBox<String> docstringFormatCombo;
    private CheckBox analyzeDocstringsCheck;
    private CheckBox renderExtDocStdlibCheck;

    // reStructuredText
    private TextField sphinxWorkingDirField;
    private Button browseSphinxDirButton;
    private CheckBox treatTxtAsRstCheck;

    public SettingsToolsPythonIntegratedToolsPage() {
        this.manager = PythonIntegratedToolsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        VBox contentBox = new VBox(14);
        contentBox.setPadding(new Insets(16, 24, 24, 24));
        contentBox.setStyle("-fx-background-color: #1E1F22;");

        // 1. Packaging
        HBox packagingHeader = createSectionHeader("Packaging");
        GridPane pkgGrid = new GridPane();
        pkgGrid.setHgap(12);
        pkgGrid.setVgap(8);
        pkgGrid.setAlignment(Pos.CENTER_LEFT);

        Label pkgLabel = createFieldLabel("Package requirements file for SDK:");
        packageRequirementsField = createTextField();
        browsePackageReqButton = createBrowseButton("Browse requirements file");
        browsePackageReqButton.setOnAction(e -> chooseFile("Select Requirements File", packageRequirementsField));
        HBox pkgRow = new HBox(6, packageRequirementsField, browsePackageReqButton);
        pkgRow.setAlignment(Pos.CENTER_LEFT);

        pkgGrid.add(pkgLabel, 0, 0);
        pkgGrid.add(pkgRow, 1, 0);

        // 2. Pipenv
        HBox pipenvHeader = createSectionHeader("Pipenv");
        GridPane pipenvGrid = new GridPane();
        pipenvGrid.setHgap(12);
        pipenvGrid.setVgap(8);
        pipenvGrid.setAlignment(Pos.CENTER_LEFT);

        Label pipenvLabel = createFieldLabel("Path to Pipenv executable:");
        pipenvPathField = createTextField();
        browsePipenvButton = createBrowseButton("Browse Pipenv executable");
        browsePipenvButton.setOnAction(e -> chooseFile("Select Pipenv Executable", pipenvPathField));
        HBox pipenvRow = new HBox(6, pipenvPathField, browsePipenvButton);
        pipenvRow.setAlignment(Pos.CENTER_LEFT);

        pipenvGrid.add(pipenvLabel, 0, 0);
        pipenvGrid.add(pipenvRow, 1, 0);

        // 3. Testing
        HBox testingHeader = createSectionHeader("Testing");
        GridPane testingGrid = new GridPane();
        testingGrid.setHgap(12);
        testingGrid.setVgap(8);
        testingGrid.setAlignment(Pos.CENTER_LEFT);

        Label runnerLabel = createFieldLabel("Default test runner:");
        testRunnerCombo = new ComboBox<>();
        testRunnerCombo.getItems().addAll("Autodetect", "pytest", "unittest", "nosetests", "tox");
        testRunnerCombo.setValue("Autodetect");
        styleControl(testRunnerCombo);
        testRunnerCombo.setPrefWidth(350);

        detectTestsInJupyterCheck = createCheckBox("Detect tests in Jupyter Notebooks");

        testingGrid.add(runnerLabel, 0, 0);
        testingGrid.add(testRunnerCombo, 1, 0);
        testingGrid.add(new Label(""), 0, 1);
        testingGrid.add(detectTestsInJupyterCheck, 1, 1);

        // 4. Docstrings
        HBox docstringsHeader = createSectionHeader("Docstrings");
        GridPane docGrid = new GridPane();
        docGrid.setHgap(12);
        docGrid.setVgap(8);
        docGrid.setAlignment(Pos.CENTER_LEFT);

        Label formatLabel = createFieldLabel("Docstring format:");
        docstringFormatCombo = new ComboBox<>();
        docstringFormatCombo.getItems().addAll("reStructuredText", "Plain", "Epytext", "Google", "NumPy");
        docstringFormatCombo.setValue("reStructuredText");
        styleControl(docstringFormatCombo);
        docstringFormatCombo.setPrefWidth(350);

        analyzeDocstringsCheck = createCheckBox("Analyze Python code in docstrings");
        renderExtDocStdlibCheck = createCheckBox("Render external documentation for stdlib");

        docGrid.add(formatLabel, 0, 0);
        docGrid.add(docstringFormatCombo, 1, 0);
        docGrid.add(new Label(""), 0, 1);
        docGrid.add(analyzeDocstringsCheck, 1, 1);
        docGrid.add(new Label(""), 0, 2);
        docGrid.add(renderExtDocStdlibCheck, 1, 2);

        // 5. reStructuredText
        HBox rstHeader = createSectionHeader("reStructuredText");
        GridPane rstGrid = new GridPane();
        rstGrid.setHgap(12);
        rstGrid.setVgap(8);
        rstGrid.setAlignment(Pos.CENTER_LEFT);

        Label sphinxLabel = createFieldLabel("Sphinx working directory:");
        sphinxWorkingDirField = createTextField();
        browseSphinxDirButton = createBrowseButton("Browse Sphinx working directory");
        browseSphinxDirButton.setOnAction(e -> chooseFile("Select Sphinx Working Directory", sphinxWorkingDirField));
        HBox sphinxRow = new HBox(6, sphinxWorkingDirField, browseSphinxDirButton);
        sphinxRow.setAlignment(Pos.CENTER_LEFT);

        treatTxtAsRstCheck = createCheckBox("Treat *.txt files as reStructuredText");

        rstGrid.add(sphinxLabel, 0, 0);
        rstGrid.add(sphinxRow, 1, 0);
        rstGrid.add(new Label(""), 0, 1);
        rstGrid.add(treatTxtAsRstCheck, 1, 1);

        contentBox.getChildren().addAll(
                packagingHeader, pkgGrid,
                pipenvHeader, pipenvGrid,
                testingHeader, testingGrid,
                docstringsHeader, docGrid,
                rstHeader, rstGrid
        );

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().add(scrollPane);

        setupListeners();
    }

    private void chooseFile(String title, TextField targetField) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            targetField.setText(file.getAbsolutePath());
            notifyModified();
        }
    }

    private void setupListeners() {
        packageRequirementsField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        pipenvPathField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        testRunnerCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        detectTestsInJupyterCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        docstringFormatCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        analyzeDocstringsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        renderExtDocStdlibCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        sphinxWorkingDirField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        treatTxtAsRstCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
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

    private void applySettingsToUI(PythonIntegratedToolsSettings s) {
        if (s == null) return;
        packageRequirementsField.setText(s.getPackageRequirementsFile());
        pipenvPathField.setText(s.getPipenvExecutablePath());
        testRunnerCombo.setValue(s.getDefaultTestRunner());
        detectTestsInJupyterCheck.setSelected(s.isDetectTestsInJupyterNotebooks());
        docstringFormatCombo.setValue(s.getDocstringFormat());
        analyzeDocstringsCheck.setSelected(s.isAnalyzePythonCodeInDocstrings());
        renderExtDocStdlibCheck.setSelected(s.isRenderExternalDocumentationForStdlib());
        sphinxWorkingDirField.setText(s.getSphinxWorkingDirectory());
        treatTxtAsRstCheck.setSelected(s.isTreatTxtFilesAsReStructuredText());
    }

    private PythonIntegratedToolsSettings getCurrentSettingsFromUI() {
        PythonIntegratedToolsSettings s = new PythonIntegratedToolsSettings();
        s.setPackageRequirementsFile(packageRequirementsField.getText());
        s.setPipenvExecutablePath(pipenvPathField.getText());
        if (testRunnerCombo.getValue() != null) s.setDefaultTestRunner(testRunnerCombo.getValue());
        s.setDetectTestsInJupyterNotebooks(detectTestsInJupyterCheck.isSelected());
        if (docstringFormatCombo.getValue() != null) s.setDocstringFormat(docstringFormatCombo.getValue());
        s.setAnalyzePythonCodeInDocstrings(analyzeDocstringsCheck.isSelected());
        s.setRenderExternalDocumentationForStdlib(renderExtDocStdlibCheck.isSelected());
        s.setSphinxWorkingDirectory(sphinxWorkingDirField.getText());
        s.setTreatTxtFilesAsReStructuredText(treatTxtAsRstCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        PythonIntegratedToolsSettings current = getCurrentSettingsFromUI();
        return !Objects.equals(initialSettings, current);
    }

    public void apply() {
        PythonIntegratedToolsSettings current = getCurrentSettingsFromUI();
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
        l.setMinWidth(220);
        return l;
    }

    private TextField createTextField() {
        TextField tf = new TextField();
        tf.setPrefWidth(350);
        styleControl(tf);
        return tf;
    }

    private Button createBrowseButton(String tooltipText) {
        Button btn = new Button("📁");
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-min-width: 26px; -fx-min-height: 26px;");
        if (tooltipText != null) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        return btn;
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

    // Getters for testing
    public TextField getPackageRequirementsField() {
        return packageRequirementsField;
    }

    public TextField getPipenvPathField() {
        return pipenvPathField;
    }

    public ComboBox<String> getTestRunnerCombo() {
        return testRunnerCombo;
    }

    public CheckBox getDetectTestsInJupyterCheck() {
        return detectTestsInJupyterCheck;
    }

    public ComboBox<String> getDocstringFormatCombo() {
        return docstringFormatCombo;
    }

    public CheckBox getAnalyzeDocstringsCheck() {
        return analyzeDocstringsCheck;
    }

    public CheckBox getRenderExtDocStdlibCheck() {
        return renderExtDocStdlibCheck;
    }

    public TextField getSphinxWorkingDirField() {
        return sphinxWorkingDirField;
    }

    public CheckBox getTreatTxtAsRstCheck() {
        return treatTxtAsRstCheck;
    }
}
