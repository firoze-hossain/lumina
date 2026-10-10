package dev.lumina.ui;

import dev.lumina.scala.ScalaLanguageSettingsManager;
import dev.lumina.scala.ScalaPerformanceSettings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Objects;

/**
 * Languages & Frameworks > Scala > Performance settings page in Lumina IDE.
 * Matches Image 2:
 *  - Top performance suggestions with hyperlinks
 *  - Collapsible Advanced section
 *    - Implicit parameters search depth (-1 for none)
 *    - Execution of scala.meta programs
 *    - Local Ivy cache indexing mode
 *    - Trim method bodies expanded by scala.meta
 *    - Search all symbols (include locals)
 *    - Disable parsing of documentation comments
 *    - Disable language injection in Scala files
 *    - Don't cache compound types
 */
public class SettingsLanguagesScalaPerformancePage extends VBox {

    private final ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;

    // Advanced Section
    private Button advancedToggleBtn;
    private VBox advancedContentBox;
    private boolean advancedExpanded = true;

    private Spinner<Integer> implicitSearchDepthSpinner;
    private ComboBox<String> scalaMetaProgramsComboBox;
    private ComboBox<String> ivyCacheIndexingModeComboBox;

    private CheckBox trimMethodBodiesCheckBox;
    private CheckBox searchAllSymbolsCheckBox;
    private CheckBox disableDocCommentsCheckBox;
    private CheckBox disableLanguageInjectionCheckBox;
    private CheckBox dontCacheCompoundTypesCheckBox;

    private ScalaPerformanceSettings initialSettings;

    public SettingsLanguagesScalaPerformancePage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ScalaPerformanceSettings current = manager.getPerformanceSettings();

        // 1. Informational advice at top
        VBox adviceBox = new VBox(6);
        adviceBox.getChildren().addAll(
                createBulletRow("Follow these suggestions on ", "how to make code highlighting-friendly", "."),
                createBulletRow("Highlighting too slow? Use Alt+Enter to ", "highlight only what's visible", " in the current file."),
                createBulletRow("Learn about ", "reporting performance problems", "."),
                createBulletRow("To search for existing performance issues or report a new one, use our ", "issue tracker", "."),
                createBulletRow("Need quick help from the community? Join the ", "Scala community channel", ".")
        );

        // 2. Advanced Collapsible Header
        HBox advancedHeader = new HBox(6);
        advancedHeader.setAlignment(Pos.CENTER_LEFT);
        advancedHeader.setPadding(new Insets(10, 0, 4, 0));

        advancedToggleBtn = new Button("\u25BE Advanced");
        advancedToggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0; -fx-cursor: hand;");
        advancedToggleBtn.setOnAction(e -> toggleAdvanced());

        Separator advancedLine = new Separator();
        advancedLine.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(advancedLine, Priority.ALWAYS);

        advancedHeader.getChildren().addAll(advancedToggleBtn, advancedLine);

        // 3. Advanced Content Box
        advancedContentBox = new VBox(10);
        advancedContentBox.setPadding(new Insets(6, 0, 0, 16));

        // Form Row 1: Implicit parameters search depth
        HBox depthRow = new HBox(12);
        depthRow.setAlignment(Pos.CENTER_LEFT);
        Label depthLabel = createLabel("Implicit parameters search depth (-1 for none):");
        implicitSearchDepthSpinner = new Spinner<>(-1, 100, current.getImplicitParametersSearchDepth(), 1);
        implicitSearchDepthSpinner.setEditable(true);
        implicitSearchDepthSpinner.setPrefWidth(90);
        styleSpinner(implicitSearchDepthSpinner);
        implicitSearchDepthSpinner.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        depthRow.getChildren().addAll(depthLabel, implicitSearchDepthSpinner);

        // Form Row 2: Execution of scala.meta programs
        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label metaLabel = createLabel("Execution of scala.meta programs:");
        scalaMetaProgramsComboBox = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        scalaMetaProgramsComboBox.setValue(current.getScalaMetaProgramsExecution());
        scalaMetaProgramsComboBox.setPrefWidth(120);
        styleComboBox(scalaMetaProgramsComboBox);
        scalaMetaProgramsComboBox.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        metaRow.getChildren().addAll(metaLabel, scalaMetaProgramsComboBox);

        // Form Row 3: Local Ivy cache indexing mode
        HBox ivyRow = new HBox(12);
        ivyRow.setAlignment(Pos.CENTER_LEFT);
        Label ivyLabel = createLabel("Local Ivy cache indexing mode:");
        ivyCacheIndexingModeComboBox = new ComboBox<>(FXCollections.observableArrayList("Metadata", "Full", "None"));
        ivyCacheIndexingModeComboBox.setValue(current.getLocalIvyCacheIndexingMode());
        ivyCacheIndexingModeComboBox.setPrefWidth(120);
        styleComboBox(ivyCacheIndexingModeComboBox);
        ivyCacheIndexingModeComboBox.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        ivyRow.getChildren().addAll(ivyLabel, ivyCacheIndexingModeComboBox);

        // Checkbox: Trim method bodies expanded by scala.meta
        trimMethodBodiesCheckBox = new CheckBox("Trim method bodies expanded by scala.meta");
        trimMethodBodiesCheckBox.setSelected(current.isTrimMethodBodiesExpandedByScalaMeta());
        styleCheckBox(trimMethodBodiesCheckBox);
        trimMethodBodiesCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        // Checkbox: Search all symbols (include locals)
        searchAllSymbolsCheckBox = new CheckBox("Search all symbols (include locals)");
        searchAllSymbolsCheckBox.setSelected(current.isSearchAllSymbolsIncludeLocals());
        styleCheckBox(searchAllSymbolsCheckBox);
        searchAllSymbolsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        // Checkbox: Disable parsing of documentation comments
        HBox docCommentsRow = createCheckboxWithHelp(
                "Disable parsing of documentation comments",
                "Disables parsing ScalaDoc comments to reduce memory footprint and improve performance",
                cb -> disableDocCommentsCheckBox = cb,
                current.isDisableParsingOfDocComments()
        );

        // Checkbox: Disable language injection in Scala files
        HBox langInjectionRow = createCheckboxWithHelp(
                "Disable language injection in Scala files",
                "Disables automatic language injection in interpolated strings and comments",
                cb -> disableLanguageInjectionCheckBox = cb,
                current.isDisableLanguageInjectionInScalaFiles()
        );

        // Checkbox: Don't cache compound types
        HBox compoundTypesRow = createCheckboxWithHelp(
                "Don't cache compound types",
                "Disables caching of complex compound and refined types during type checking",
                cb -> dontCacheCompoundTypesCheckBox = cb,
                current.isDontCacheCompoundTypes()
        );

        advancedContentBox.getChildren().addAll(
                depthRow,
                metaRow,
                ivyRow,
                trimMethodBodiesCheckBox,
                searchAllSymbolsCheckBox,
                docCommentsRow,
                langInjectionRow,
                compoundTypesRow
        );

        getChildren().addAll(adviceBox, advancedHeader, advancedContentBox);
    }

    private void toggleAdvanced() {
        advancedExpanded = !advancedExpanded;
        advancedContentBox.setVisible(advancedExpanded);
        advancedContentBox.setManaged(advancedExpanded);
        advancedToggleBtn.setText(advancedExpanded ? "\u25BE Advanced" : "\u25B8 Advanced");
    }

    private HBox createBulletRow(String prefix, String linkText, String suffix) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Label bullet = new Label("•");
        bullet.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");

        Text prefixText = new Text(prefix);
        prefixText.setStyle("-fx-fill: #DFE1E5; -fx-font-size: 13px;");

        Hyperlink link = new Hyperlink(linkText);
        link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0;");
        link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: true;"));
        link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;"));

        Text suffixText = new Text(suffix);
        suffixText.setStyle("-fx-fill: #DFE1E5; -fx-font-size: 13px;");

        TextFlow flow = new TextFlow(prefixText, link, suffixText);
        row.getChildren().addAll(bullet, flow);
        return row;
    }

    private HBox createCheckboxWithHelp(String text, String tooltipText, java.util.function.Consumer<CheckBox> consumer, boolean initialValue) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        CheckBox cb = new CheckBox(text);
        cb.setSelected(initialValue);
        styleCheckBox(cb);
        cb.selectedProperty().addListener((obs, oldV, newV) -> fireModified());
        consumer.accept(cb);

        Label helpIcon = new Label("(?)");
        helpIcon.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-cursor: hand;");
        helpIcon.setTooltip(new Tooltip(tooltipText));

        row.getChildren().addAll(cb, helpIcon);
        return row;
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

    public ScalaPerformanceSettings getCurrentUiSettings() {
        ScalaPerformanceSettings s = new ScalaPerformanceSettings();
        if (implicitSearchDepthSpinner != null && implicitSearchDepthSpinner.getValue() != null) {
            s.setImplicitParametersSearchDepth(implicitSearchDepthSpinner.getValue());
        }
        if (scalaMetaProgramsComboBox != null && scalaMetaProgramsComboBox.getValue() != null) {
            s.setScalaMetaProgramsExecution(scalaMetaProgramsComboBox.getValue());
        }
        if (ivyCacheIndexingModeComboBox != null && ivyCacheIndexingModeComboBox.getValue() != null) {
            s.setLocalIvyCacheIndexingMode(ivyCacheIndexingModeComboBox.getValue());
        }
        s.setTrimMethodBodiesExpandedByScalaMeta(trimMethodBodiesCheckBox.isSelected());
        s.setSearchAllSymbolsIncludeLocals(searchAllSymbolsCheckBox.isSelected());
        s.setDisableParsingOfDocComments(disableDocCommentsCheckBox.isSelected());
        s.setDisableLanguageInjectionInScalaFiles(disableLanguageInjectionCheckBox.isSelected());
        s.setDontCacheCompoundTypes(dontCacheCompoundTypesCheckBox.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentUiSettings());
    }

    public void apply() {
        manager.setPerformanceSettings(getCurrentUiSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ScalaPerformanceSettings saved = manager.getPerformanceSettings();
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
        applySettingsToUi(new ScalaPerformanceSettings());
        fireModified();
    }

    private void applySettingsToUi(ScalaPerformanceSettings s) {
        if (s == null) return;
        if (implicitSearchDepthSpinner != null) {
            implicitSearchDepthSpinner.getValueFactory().setValue(s.getImplicitParametersSearchDepth());
        }
        if (scalaMetaProgramsComboBox != null) {
            scalaMetaProgramsComboBox.setValue(s.getScalaMetaProgramsExecution());
        }
        if (ivyCacheIndexingModeComboBox != null) {
            ivyCacheIndexingModeComboBox.setValue(s.getLocalIvyCacheIndexingMode());
        }
        trimMethodBodiesCheckBox.setSelected(s.isTrimMethodBodiesExpandedByScalaMeta());
        searchAllSymbolsCheckBox.setSelected(s.isSearchAllSymbolsIncludeLocals());
        disableDocCommentsCheckBox.setSelected(s.isDisableParsingOfDocComments());
        disableLanguageInjectionCheckBox.setSelected(s.isDisableLanguageInjectionInScalaFiles());
        dontCacheCompoundTypesCheckBox.setSelected(s.isDontCacheCompoundTypes());
    }

    // Direct UI accessors for tests
    public Spinner<Integer> getImplicitSearchDepthSpinner() {
        return implicitSearchDepthSpinner;
    }

    public ComboBox<String> getScalaMetaProgramsComboBox() {
        return scalaMetaProgramsComboBox;
    }

    public ComboBox<String> getIvyCacheIndexingModeComboBox() {
        return ivyCacheIndexingModeComboBox;
    }

    public CheckBox getTrimMethodBodiesCheckBox() {
        return trimMethodBodiesCheckBox;
    }

    public CheckBox getSearchAllSymbolsCheckBox() {
        return searchAllSymbolsCheckBox;
    }

    public CheckBox getDisableDocCommentsCheckBox() {
        return disableDocCommentsCheckBox;
    }

    public CheckBox getDisableLanguageInjectionCheckBox() {
        return disableLanguageInjectionCheckBox;
    }

    public CheckBox getDontCacheCompoundTypesCheckBox() {
        return dontCacheCompoundTypesCheckBox;
    }

    public Button getAdvancedToggleBtn() {
        return advancedToggleBtn;
    }

    public VBox getAdvancedContentBox() {
        return advancedContentBox;
    }
}
