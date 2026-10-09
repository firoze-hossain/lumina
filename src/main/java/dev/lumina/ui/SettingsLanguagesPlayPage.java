package dev.lumina.ui;

import dev.lumina.play.PlaySettings;
import dev.lumina.play.PlaySettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Languages & Frameworks > Play settings page in Lumina IDE.
 * Matches the reference IDE 1:1 with Compiler, Routes, and Other segment tabs.
 */
public class SettingsLanguagesPlayPage extends VBox {

    private final PlaySettingsManager manager = PlaySettingsManager.getInstance();
    private Runnable onModifiedListener;

    // Segment tab buttons
    private Button compilerTabBtn;
    private Button routesTabBtn;
    private Button otherTabBtn;
    private StackPane contentStack;

    // Compiler Tab Controls
    private VBox compilerPane;
    private CheckBox usePlayCompilerCheckBox;
    private CheckBox dontCompileWithinIdeCheckBox;
    private ComboBox<String> playModuleComboBox;
    private TextField projectUriField;
    private TextArea additionalSbtOptionsArea;

    // Routes Tab Controls
    private VBox routesPane;
    private Spinner<Integer> minSpacesSpinner;
    private CheckBox ignoreUrlDepthCheckBox;
    private CheckBox reformatRoutesOnEnterCheckBox;

    // Other Tab Controls
    private VBox otherPane;
    private CheckBox excludeTargetDirCheckBox;
    private CheckBox coloredOutputCheckBox;
    private CheckBox showCodeRefsCheckBox;
    private CheckBox setTemplateImportsManuallyCheckBox;
    private Button addImportBtn;
    private Button removeImportBtn;
    private ListView<String> importsListView;
    private ObservableList<String> importsList;
    private CheckBox gutterActionMethodsCheckBox;
    private CheckBox gutterViewCallsCheckBox;
    private CheckBox gutterResultCallsCheckBox;

    private PlaySettings initialSettings;

    public SettingsLanguagesPlayPage() {
        setSpacing(16);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        PlaySettings current = manager.getSettings();

        // 1. Top Segment Navigation Pills
        compilerTabBtn = createTabButton("Compiler", true);
        routesTabBtn = createTabButton("Routes", false);
        otherTabBtn = createTabButton("Other", false);

        compilerTabBtn.setOnAction(e -> selectTab(0));
        routesTabBtn.setOnAction(e -> selectTab(1));
        otherTabBtn.setOnAction(e -> selectTab(2));

        HBox tabPillBar = new HBox(4, compilerTabBtn, routesTabBtn, otherTabBtn);
        tabPillBar.setPadding(new Insets(2));
        tabPillBar.setStyle("-fx-background-color: #2B2D30; -fx-background-radius: 6;");

        // 2. Build Tab Panes
        buildCompilerPane(current);
        buildRoutesPane(current);
        buildOtherPane(current);

        contentStack = new StackPane(compilerPane, routesPane, otherPane);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        selectTab(0);

        getChildren().addAll(tabPillBar, contentStack);
    }

    private Button createTabButton(String text, boolean active) {
        Button btn = new Button(text);
        updateTabButtonStyle(btn, active);
        return btn;
    }

    private void updateTabButtonStyle(Button btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: #2b5b84; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 14 4 14; -fx-background-radius: 4; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 14 4 14; -fx-background-radius: 4; -fx-cursor: hand;");
        }
    }

    private void selectTab(int index) {
        updateTabButtonStyle(compilerTabBtn, index == 0);
        updateTabButtonStyle(routesTabBtn, index == 1);
        updateTabButtonStyle(otherTabBtn, index == 2);

        compilerPane.setVisible(index == 0);
        compilerPane.setManaged(index == 0);

        routesPane.setVisible(index == 1);
        routesPane.setManaged(index == 1);

        otherPane.setVisible(index == 2);
        otherPane.setManaged(index == 2);
    }

    // --- TAB 1: COMPILER ---
    private void buildCompilerPane(PlaySettings current) {
        compilerPane = new VBox(14);
        VBox.setVgrow(compilerPane, Priority.ALWAYS);

        usePlayCompilerCheckBox = new CheckBox("Use Play compiler for this project");
        usePlayCompilerCheckBox.setSelected(current.isUsePlayCompiler());
        styleCheckBox(usePlayCompilerCheckBox);
        usePlayCompilerCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        dontCompileWithinIdeCheckBox = new CheckBox("Don't compile the project within IDE before run");
        dontCompileWithinIdeCheckBox.setSelected(current.isDontCompileWithinIdeBeforeRun());
        styleCheckBox(dontCompileWithinIdeCheckBox);
        dontCompileWithinIdeCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        // Grid of labeled form fields
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(140);
        col1.setPrefWidth(150);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setMinWidth(350);
        col2.setMaxWidth(550);
        grid.getColumnConstraints().addAll(col1, col2);

        // Row 1: Play module
        Label playModuleLabel = createFormLabel("Play module:");
        List<String> modules = PlaySettings.detectAvailableModules();
        playModuleComboBox = new ComboBox<>(FXCollections.observableArrayList(modules));
        if (modules.contains(current.getPlayModule())) {
            playModuleComboBox.setValue(current.getPlayModule());
        } else if (!modules.isEmpty()) {
            playModuleComboBox.setValue(modules.get(0));
        } else {
            playModuleComboBox.setValue("Module: 'lumina-ide'");
        }
        playModuleComboBox.setMaxWidth(Double.MAX_VALUE);
        styleComboBox(playModuleComboBox);
        playModuleComboBox.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        grid.add(playModuleLabel, 0, 0);
        grid.add(playModuleComboBox, 1, 0);

        // Row 2: Project uri
        Label projectUriLabel = createFormLabel("Project uri:");
        projectUriField = new TextField(current.getProjectUri());
        projectUriField.setMaxWidth(Double.MAX_VALUE);
        styleTextField(projectUriField);
        projectUriField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        grid.add(projectUriLabel, 0, 1);
        grid.add(projectUriField, 1, 1);

        // Row 3: Additional sbt options
        Label sbtOptionsLabel = createFormLabel("Additional sbt options:");
        additionalSbtOptionsArea = new TextArea(current.getAdditionalSbtOptions());
        additionalSbtOptionsArea.setPrefRowCount(3);
        additionalSbtOptionsArea.setMaxWidth(Double.MAX_VALUE);
        styleTextArea(additionalSbtOptionsArea);
        additionalSbtOptionsArea.textProperty().addListener((obs, oldV, newV) -> fireModified());

        grid.add(sbtOptionsLabel, 0, 2);
        grid.add(additionalSbtOptionsArea, 1, 2);

        compilerPane.getChildren().addAll(
                usePlayCompilerCheckBox,
                dontCompileWithinIdeCheckBox,
                grid
        );
    }

    // --- TAB 2: ROUTES ---
    private void buildRoutesPane(PlaySettings current) {
        routesPane = new VBox(14);
        VBox.setVgrow(routesPane, Priority.ALWAYS);

        HBox minSpacesRow = new HBox(16);
        minSpacesRow.setAlignment(Pos.CENTER_LEFT);

        Label minSpacesLabel = createFormLabel("Minimum spaces for routes formatting");
        minSpacesLabel.setMinWidth(240);

        minSpacesSpinner = new Spinner<>(1, 64, current.getMinSpacesForRoutesFormatting());
        minSpacesSpinner.setEditable(true);
        minSpacesSpinner.setPrefWidth(300);
        minSpacesSpinner.setMaxWidth(400);
        HBox.setHgrow(minSpacesSpinner, Priority.ALWAYS);
        styleSpinner(minSpacesSpinner);
        minSpacesSpinner.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        minSpacesRow.getChildren().addAll(minSpacesLabel, minSpacesSpinner);

        ignoreUrlDepthCheckBox = new CheckBox("Ignore URL depth in route files");
        ignoreUrlDepthCheckBox.setSelected(current.isIgnoreUrlDepthInRouteFiles());
        styleCheckBox(ignoreUrlDepthCheckBox);
        ignoreUrlDepthCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        reformatRoutesOnEnterCheckBox = new CheckBox("Reformat routes file on Enter");
        reformatRoutesOnEnterCheckBox.setSelected(current.isReformatRoutesFileOnEnter());
        styleCheckBox(reformatRoutesOnEnterCheckBox);
        reformatRoutesOnEnterCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        routesPane.getChildren().addAll(
                minSpacesRow,
                ignoreUrlDepthCheckBox,
                reformatRoutesOnEnterCheckBox
        );
    }

    // --- TAB 3: OTHER ---
    private void buildOtherPane(PlaySettings current) {
        otherPane = new VBox(12);
        VBox.setVgrow(otherPane, Priority.ALWAYS);

        excludeTargetDirCheckBox = new CheckBox("Exclude 'target' dir on refresh");
        excludeTargetDirCheckBox.setSelected(current.isExcludeTargetDirOnRefresh());
        styleCheckBox(excludeTargetDirCheckBox);
        excludeTargetDirCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        coloredOutputCheckBox = new CheckBox("Colored output console");
        coloredOutputCheckBox.setSelected(current.isColoredOutputConsole());
        styleCheckBox(coloredOutputCheckBox);
        coloredOutputCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        showCodeRefsCheckBox = new CheckBox("Show code refs in output console");
        showCodeRefsCheckBox.setSelected(current.isShowCodeRefsInOutputConsole());
        styleCheckBox(showCodeRefsCheckBox);
        showCodeRefsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        setTemplateImportsManuallyCheckBox = new CheckBox("Set template imports manually");
        setTemplateImportsManuallyCheckBox.setSelected(current.isSetTemplateImportsManually());
        styleCheckBox(setTemplateImportsManuallyCheckBox);
        setTemplateImportsManuallyCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
            updateTemplateImportsState();
            fireModified();
        });

        // Template imports toolbar and list
        HBox importsToolbar = new HBox(4);
        importsToolbar.setAlignment(Pos.CENTER_LEFT);

        addImportBtn = createIconButton("+", "Add template import");
        removeImportBtn = createIconButton("\u2014", "Remove template import");

        addImportBtn.setOnAction(e -> handleAddImport());
        removeImportBtn.setOnAction(e -> handleRemoveImport());

        importsToolbar.getChildren().addAll(addImportBtn, removeImportBtn);

        VBox importsContainer = new VBox(4);
        Label importsHeader = new Label("Imports");
        importsHeader.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px;");

        importsList = FXCollections.observableArrayList(current.getTemplateImports());
        importsListView = new ListView<>(importsList);
        importsListView.setPrefHeight(120);
        importsListView.setMaxHeight(160);
        importsListView.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-text-fill: #DFE1E5;");

        Label placeholder = new Label("Nothing to show");
        placeholder.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        importsListView.setPlaceholder(placeholder);

        importsContainer.getChildren().addAll(importsToolbar, importsHeader, importsListView);

        updateTemplateImportsState();

        // Gutter markers section
        Label gutterLabel = new Label("Show gutter markers for");
        gutterLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 6 0 2 0;");

        VBox gutterBox = new VBox(6);
        gutterBox.setPadding(new Insets(0, 0, 0, 16));

        gutterActionMethodsCheckBox = new CheckBox("Action methods");
        gutterActionMethodsCheckBox.setSelected(current.isGutterActionMethods());
        styleCheckBox(gutterActionMethodsCheckBox);
        gutterActionMethodsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        gutterViewCallsCheckBox = new CheckBox("View calls");
        gutterViewCallsCheckBox.setSelected(current.isGutterViewCalls());
        styleCheckBox(gutterViewCallsCheckBox);
        gutterViewCallsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        gutterResultCallsCheckBox = new CheckBox("Result calls");
        gutterResultCallsCheckBox.setSelected(current.isGutterResultCalls());
        styleCheckBox(gutterResultCallsCheckBox);
        gutterResultCallsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        gutterBox.getChildren().addAll(
                gutterActionMethodsCheckBox,
                gutterViewCallsCheckBox,
                gutterResultCallsCheckBox
        );

        otherPane.getChildren().addAll(
                excludeTargetDirCheckBox,
                coloredOutputCheckBox,
                showCodeRefsCheckBox,
                setTemplateImportsManuallyCheckBox,
                importsContainer,
                gutterLabel,
                gutterBox
        );
    }

    private void updateTemplateImportsState() {
        boolean enabled = setTemplateImportsManuallyCheckBox.isSelected();
        addImportBtn.setDisable(!enabled);
        removeImportBtn.setDisable(!enabled);
        importsListView.setDisable(!enabled);
    }

    private void handleAddImport() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Template Import");
        dialog.setHeaderText("Enter template import package or class:");
        dialog.setContentText("Import:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(pkg -> {
            String trimmed = pkg.trim();
            if (!trimmed.isEmpty() && !importsList.contains(trimmed)) {
                importsList.add(trimmed);
                fireModified();
            }
        });
    }

    private void handleRemoveImport() {
        String selected = importsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            importsList.remove(selected);
            fireModified();
        }
    }

    private Button createIconButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 26px; -fx-min-height: 24px; -fx-padding: 2 6 2 6; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(tooltipText));
        return btn;
    }

    private Label createFormLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return label;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8; -fx-font-size: 13px;");
    }

    private void styleTextArea(TextArea ta) {
        ta.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-font-size: 13px;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-font-size: 13px;");
    }

    private void styleSpinner(Spinner<?> spinner) {
        spinner.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-font-size: 13px;");
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModified(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void revertChanges() {
        reset();
    }

    public void takeSnapshot() {
        this.initialSettings = buildSettingsFromUi();
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, buildSettingsFromUi());
    }

    public PlaySettings buildSettingsFromUi() {
        PlaySettings s = new PlaySettings();
        s.setUsePlayCompiler(usePlayCompilerCheckBox.isSelected());
        s.setDontCompileWithinIdeBeforeRun(dontCompileWithinIdeCheckBox.isSelected());
        s.setPlayModule(playModuleComboBox.getValue());
        s.setProjectUri(projectUriField.getText());
        s.setAdditionalSbtOptions(additionalSbtOptionsArea.getText());

        if (minSpacesSpinner != null && minSpacesSpinner.getValue() != null) {
            s.setMinSpacesForRoutesFormatting(minSpacesSpinner.getValue());
        }
        s.setIgnoreUrlDepthInRouteFiles(ignoreUrlDepthCheckBox.isSelected());
        s.setReformatRoutesFileOnEnter(reformatRoutesOnEnterCheckBox.isSelected());

        s.setExcludeTargetDirOnRefresh(excludeTargetDirCheckBox.isSelected());
        s.setColoredOutputConsole(coloredOutputCheckBox.isSelected());
        s.setShowCodeRefsInOutputConsole(showCodeRefsCheckBox.isSelected());
        s.setSetTemplateImportsManually(setTemplateImportsManuallyCheckBox.isSelected());
        s.setTemplateImports(new ArrayList<>(importsList));

        s.setGutterActionMethods(gutterActionMethodsCheckBox.isSelected());
        s.setGutterViewCalls(gutterViewCallsCheckBox.isSelected());
        s.setGutterResultCalls(gutterResultCallsCheckBox.isSelected());

        return s;
    }

    public void apply() {
        PlaySettings s = buildSettingsFromUi();
        manager.setSettings(s);
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        PlaySettings current = manager.getSettings();

        usePlayCompilerCheckBox.setSelected(current.isUsePlayCompiler());
        dontCompileWithinIdeCheckBox.setSelected(current.isDontCompileWithinIdeBeforeRun());
        playModuleComboBox.setValue(current.getPlayModule());
        projectUriField.setText(current.getProjectUri());
        additionalSbtOptionsArea.setText(current.getAdditionalSbtOptions());

        minSpacesSpinner.getValueFactory().setValue(current.getMinSpacesForRoutesFormatting());
        ignoreUrlDepthCheckBox.setSelected(current.isIgnoreUrlDepthInRouteFiles());
        reformatRoutesOnEnterCheckBox.setSelected(current.isReformatRoutesFileOnEnter());

        excludeTargetDirCheckBox.setSelected(current.isExcludeTargetDirOnRefresh());
        coloredOutputCheckBox.setSelected(current.isColoredOutputConsole());
        showCodeRefsCheckBox.setSelected(current.isShowCodeRefsInOutputConsole());
        setTemplateImportsManuallyCheckBox.setSelected(current.isSetTemplateImportsManually());
        importsList.setAll(current.getTemplateImports());
        updateTemplateImportsState();

        gutterActionMethodsCheckBox.setSelected(current.isGutterActionMethods());
        gutterViewCallsCheckBox.setSelected(current.isGutterViewCalls());
        gutterResultCallsCheckBox.setSelected(current.isGutterResultCalls());

        takeSnapshot();
        fireModified();
    }

    // Public getters for tests
    public Button getCompilerTabBtn() { return compilerTabBtn; }
    public Button getRoutesTabBtn() { return routesTabBtn; }
    public Button getOtherTabBtn() { return otherTabBtn; }
    public CheckBox getUsePlayCompilerCheckBox() { return usePlayCompilerCheckBox; }
    public CheckBox getDontCompileWithinIdeCheckBox() { return dontCompileWithinIdeCheckBox; }
    public ComboBox<String> getPlayModuleComboBox() { return playModuleComboBox; }
    public TextField getProjectUriField() { return projectUriField; }
    public TextArea getAdditionalSbtOptionsArea() { return additionalSbtOptionsArea; }
    public Spinner<Integer> getMinSpacesSpinner() { return minSpacesSpinner; }
    public CheckBox getIgnoreUrlDepthCheckBox() { return ignoreUrlDepthCheckBox; }
    public CheckBox getReformatRoutesOnEnterCheckBox() { return reformatRoutesOnEnterCheckBox; }
    public CheckBox getExcludeTargetDirCheckBox() { return excludeTargetDirCheckBox; }
    public CheckBox getColoredOutputCheckBox() { return coloredOutputCheckBox; }
    public CheckBox getShowCodeRefsCheckBox() { return showCodeRefsCheckBox; }
    public CheckBox getSetTemplateImportsManuallyCheckBox() { return setTemplateImportsManuallyCheckBox; }
    public ListView<String> getImportsListView() { return importsListView; }
    public ObservableList<String> getImportsList() { return importsList; }
    public CheckBox getGutterActionMethodsCheckBox() { return gutterActionMethodsCheckBox; }
    public CheckBox getGutterViewCallsCheckBox() { return gutterViewCallsCheckBox; }
    public CheckBox getGutterResultCallsCheckBox() { return gutterResultCallsCheckBox; }
}
