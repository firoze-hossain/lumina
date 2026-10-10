package dev.lumina.ui;

import dev.lumina.scala.ScalaLanguageSettingsManager;
import dev.lumina.scala.ScalaWorksheetSettings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Languages & Frameworks > Scala > Worksheet settings page in Lumina IDE.
 * Matches Image 3:
 *  - Treat .sc files as: [Always Worksheet]
 *  - [x] Run worksheet in the compiler process (Plain mode only)
 *  - [ ] Use "eclipse compatibility" mode
 *  - [x] Treat Scala scratch files as worksheet files
 *  - [x] Collapse long output by default
 *  - Output cutoff limit: [35] lines
 *  - Delay before auto-run: [1400] milliseconds
 */
public class SettingsLanguagesScalaWorksheetPage extends VBox {

    private final ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private ComboBox<String> treatScFilesAsComboBox;
    private CheckBox runInCompilerProcessCheckBox;
    private CheckBox useEclipseCompatibilityCheckBox;
    private CheckBox treatScratchFilesAsWorksheetCheckBox;
    private CheckBox collapseLongOutputCheckBox;
    private Spinner<Integer> outputCutoffLimitSpinner;
    private Spinner<Integer> delayBeforeAutoRunSpinner;

    private ScalaWorksheetSettings initialSettings;

    public SettingsLanguagesScalaWorksheetPage() {
        setSpacing(12);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ScalaWorksheetSettings current = manager.getWorksheetSettings();

        // 1. Treat .sc files as row
        HBox treatRow = new HBox(12);
        treatRow.setAlignment(Pos.CENTER_LEFT);
        Label treatLabel = createLabel("Treat .sc files as:");
        treatScFilesAsComboBox = new ComboBox<>(FXCollections.observableArrayList("Always Worksheet", "Always Ammonite"));
        treatScFilesAsComboBox.setValue(current.getTreatScFilesAs());
        treatScFilesAsComboBox.setPrefWidth(160);
        styleComboBox(treatScFilesAsComboBox);
        treatScFilesAsComboBox.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        treatRow.getChildren().addAll(treatLabel, treatScFilesAsComboBox);

        // 2. Checkboxes
        runInCompilerProcessCheckBox = new CheckBox("Run worksheet in the compiler process (Plain mode only)");
        runInCompilerProcessCheckBox.setSelected(current.isRunWorksheetInCompilerProcessPlainMode());
        styleCheckBox(runInCompilerProcessCheckBox);
        runInCompilerProcessCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        useEclipseCompatibilityCheckBox = new CheckBox("Use \"eclipse compatibility\" mode");
        useEclipseCompatibilityCheckBox.setSelected(current.isUseEclipseCompatibilityMode());
        styleCheckBox(useEclipseCompatibilityCheckBox);
        useEclipseCompatibilityCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        treatScratchFilesAsWorksheetCheckBox = new CheckBox("Treat Scala scratch files as worksheet files");
        treatScratchFilesAsWorksheetCheckBox.setSelected(current.isTreatScalaScratchFilesAsWorksheet());
        styleCheckBox(treatScratchFilesAsWorksheetCheckBox);
        treatScratchFilesAsWorksheetCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        collapseLongOutputCheckBox = new CheckBox("Collapse long output by default");
        collapseLongOutputCheckBox.setSelected(current.isCollapseLongOutputByDefault());
        styleCheckBox(collapseLongOutputCheckBox);
        collapseLongOutputCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        // 3. Cutoff limit row
        HBox cutoffRow = new HBox(10);
        cutoffRow.setAlignment(Pos.CENTER_LEFT);
        Label cutoffLabel = createLabel("Output cutoff limit:");
        outputCutoffLimitSpinner = new Spinner<>(1, 10000, current.getOutputCutoffLimit(), 5);
        outputCutoffLimitSpinner.setEditable(true);
        outputCutoffLimitSpinner.setPrefWidth(80);
        styleSpinner(outputCutoffLimitSpinner);
        outputCutoffLimitSpinner.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        Label linesLabel = createLabel("lines");
        cutoffRow.getChildren().addAll(cutoffLabel, outputCutoffLimitSpinner, linesLabel);

        // 4. Delay auto-run row
        HBox delayRow = new HBox(10);
        delayRow.setAlignment(Pos.CENTER_LEFT);
        Label delayLabel = createLabel("Delay before auto-run:");
        delayBeforeAutoRunSpinner = new Spinner<>(0, 60000, current.getDelayBeforeAutoRunMs(), 100);
        delayBeforeAutoRunSpinner.setEditable(true);
        delayBeforeAutoRunSpinner.setPrefWidth(90);
        styleSpinner(delayBeforeAutoRunSpinner);
        delayBeforeAutoRunSpinner.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        Label msLabel = createLabel("milliseconds");
        delayRow.getChildren().addAll(delayLabel, delayBeforeAutoRunSpinner, msLabel);

        getChildren().addAll(
                treatRow,
                runInCompilerProcessCheckBox,
                useEclipseCompatibilityCheckBox,
                treatScratchFilesAsWorksheetCheckBox,
                collapseLongOutputCheckBox,
                cutoffRow,
                delayRow
        );
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4;");
    }

    private void styleSpinner(Spinner<Integer> sp) {
        sp.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4;");
        if (sp.getEditor() != null) {
            sp.getEditor().setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void takeSnapshot() {
        this.initialSettings = getCurrentUiSettings();
    }

    public ScalaWorksheetSettings getCurrentUiSettings() {
        ScalaWorksheetSettings s = new ScalaWorksheetSettings();
        if (treatScFilesAsComboBox != null && treatScFilesAsComboBox.getValue() != null) {
            s.setTreatScFilesAs(treatScFilesAsComboBox.getValue());
        }
        s.setRunWorksheetInCompilerProcessPlainMode(runInCompilerProcessCheckBox.isSelected());
        s.setUseEclipseCompatibilityMode(useEclipseCompatibilityCheckBox.isSelected());
        s.setTreatScalaScratchFilesAsWorksheet(treatScratchFilesAsWorksheetCheckBox.isSelected());
        s.setCollapseLongOutputByDefault(collapseLongOutputCheckBox.isSelected());
        if (outputCutoffLimitSpinner != null && outputCutoffLimitSpinner.getValue() != null) {
            s.setOutputCutoffLimit(outputCutoffLimitSpinner.getValue());
        }
        if (delayBeforeAutoRunSpinner != null && delayBeforeAutoRunSpinner.getValue() != null) {
            s.setDelayBeforeAutoRunMs(delayBeforeAutoRunSpinner.getValue());
        }
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentUiSettings());
    }

    public void apply() {
        manager.setWorksheetSettings(getCurrentUiSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ScalaWorksheetSettings saved = manager.getWorksheetSettings();
        applySettingsToUi(saved);
        takeSnapshot();
        fireModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            applySettingsToUi(initialSettings);
            fireModified();
        }
    }

    public void resetDefaults() {
        applySettingsToUi(new ScalaWorksheetSettings());
        fireModified();
    }

    private void applySettingsToUi(ScalaWorksheetSettings s) {
        if (s == null) return;
        if (treatScFilesAsComboBox != null) {
            treatScFilesAsComboBox.setValue(s.getTreatScFilesAs());
        }
        runInCompilerProcessCheckBox.setSelected(s.isRunWorksheetInCompilerProcessPlainMode());
        useEclipseCompatibilityCheckBox.setSelected(s.isUseEclipseCompatibilityMode());
        treatScratchFilesAsWorksheetCheckBox.setSelected(s.isTreatScalaScratchFilesAsWorksheet());
        collapseLongOutputCheckBox.setSelected(s.isCollapseLongOutputByDefault());
        if (outputCutoffLimitSpinner != null) {
            outputCutoffLimitSpinner.getValueFactory().setValue(s.getOutputCutoffLimit());
        }
        if (delayBeforeAutoRunSpinner != null) {
            delayBeforeAutoRunSpinner.getValueFactory().setValue(s.getDelayBeforeAutoRunMs());
        }
    }

    // Direct UI accessors for tests
    public ComboBox<String> getTreatScFilesAsComboBox() {
        return treatScFilesAsComboBox;
    }

    public CheckBox getRunInCompilerProcessCheckBox() {
        return runInCompilerProcessCheckBox;
    }

    public CheckBox getUseEclipseCompatibilityCheckBox() {
        return useEclipseCompatibilityCheckBox;
    }

    public CheckBox getTreatScratchFilesAsWorksheetCheckBox() {
        return treatScratchFilesAsWorksheetCheckBox;
    }

    public CheckBox getCollapseLongOutputCheckBox() {
        return collapseLongOutputCheckBox;
    }

    public Spinner<Integer> getOutputCutoffLimitSpinner() {
        return outputCutoffLimitSpinner;
    }

    public Spinner<Integer> getDelayBeforeAutoRunSpinner() {
        return delayBeforeAutoRunSpinner;
    }
}
