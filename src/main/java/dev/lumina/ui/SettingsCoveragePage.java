package dev.lumina.ui;

import dev.lumina.build.CoverageSettings;
import dev.lumina.build.CoverageSettingsManager;
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
 * Settings page for Build, Execution, Deployment > Coverage.
 * Matches 1:1 with reference screenshot media_1791450052884_14c89804.png:
 *  - When new coverage is gathered:
 *    - Radio: Show options before applying coverage to the editor
 *    - Radio: Do not apply collected coverage
 *    - Radio: Replace active suites with the new one
 *    - Radio: Add to the active suites
 *    - Checkbox: Activate Coverage View
 *    - Checkbox: Show coverage in the project view
 *  - Python coverage:
 *    - Use bundled coverage.py
 *    - Branch coverage
 *  - Java Coverage:
 *    - Choose coverage runner (Lumina / JaCoCo)
 *    - Branch coverage (with hint)
 *    - Track per test coverage (with hint)
 *    - Collect coverage in test folders
 *    - Ignore implicitly declared default constructors
 *    - Exclude annotations (+, - toolbar, table/list)
 */
public class SettingsCoveragePage extends VBox {

    private final CoverageSettingsManager manager = CoverageSettingsManager.getInstance();

    // Section 1: When new coverage is gathered
    private final ToggleGroup gatherPolicyGroup = new ToggleGroup();
    private final RadioButton showOptionsRadio = new RadioButton("Show options before applying coverage to the editor");
    private final RadioButton doNotApplyRadio = new RadioButton("Do not apply collected coverage");
    private final RadioButton replaceActiveSuitesRadio = new RadioButton("Replace active suites with the new one");
    private final RadioButton addToActiveSuitesRadio = new RadioButton("Add to the active suites");

    private final CheckBox activateCoverageViewCheck = new CheckBox("Activate Coverage View");
    private final CheckBox showCoverageInProjectViewCheck = new CheckBox("Show coverage in the project view");

    // Section 2: Python coverage
    private final CheckBox pythonBundledCoverageCheck = new CheckBox("Use bundled coverage.py");
    private final CheckBox pythonBranchCoverageCheck = new CheckBox("Branch coverage");

    // Section 3: Java Coverage
    private final ComboBox<String> runnerCombo = new ComboBox<>();
    private final CheckBox javaBranchCoverageCheck = new CheckBox("Branch coverage");
    private final Label javaBranchHint = new Label("Collect coverage for all branches of if/switch statements");
    private final CheckBox javaTrackPerTestCheck = new CheckBox("Track per test coverage");
    private final Label javaTrackHint = new Label("Collect data about which code lines were covered by specific tests");
    private final CheckBox javaTestFoldersCheck = new CheckBox("Collect coverage in test folders");
    private final CheckBox javaIgnoreDefaultConstructorsCheck = new CheckBox("Ignore implicitly declared default constructors");

    // Exclude annotations list & toolbar
    private final ObservableList<String> excludeAnnotationsList = FXCollections.observableArrayList();
    private final ListView<String> excludeAnnotationsListView = new ListView<>(excludeAnnotationsList);
    private final Button addAnnotationBtn = new Button("+");
    private final Button removeAnnotationBtn = new Button("−");

    private CoverageSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsCoveragePage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. When new coverage is gathered ---
        Label gatherHeader = new Label("When new coverage is gathered");
        gatherHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 0 2 0;");

        showOptionsRadio.setToggleGroup(gatherPolicyGroup);
        doNotApplyRadio.setToggleGroup(gatherPolicyGroup);
        replaceActiveSuitesRadio.setToggleGroup(gatherPolicyGroup);
        addToActiveSuitesRadio.setToggleGroup(gatherPolicyGroup);

        styleRadioButton(showOptionsRadio);
        styleRadioButton(doNotApplyRadio);
        styleRadioButton(replaceActiveSuitesRadio);
        styleRadioButton(addToActiveSuitesRadio);

        showOptionsRadio.setOnAction(e -> fireModified());
        doNotApplyRadio.setOnAction(e -> fireModified());
        replaceActiveSuitesRadio.setOnAction(e -> fireModified());
        addToActiveSuitesRadio.setOnAction(e -> fireModified());

        styleCheckBox(activateCoverageViewCheck);
        styleCheckBox(showCoverageInProjectViewCheck);
        activateCoverageViewCheck.setOnAction(e -> fireModified());
        showCoverageInProjectViewCheck.setOnAction(e -> fireModified());

        VBox gatherBox = new VBox(6,
                showOptionsRadio,
                doNotApplyRadio,
                replaceActiveSuitesRadio,
                addToActiveSuitesRadio,
                activateCoverageViewCheck,
                showCoverageInProjectViewCheck
        );
        gatherBox.setPadding(new Insets(0, 0, 4, 16));

        // --- 2. Python coverage ---
        Label pyCoverageHeader = new Label("Python coverage");
        pyCoverageHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 6 0 2 0;");

        styleCheckBox(pythonBundledCoverageCheck);
        styleCheckBox(pythonBranchCoverageCheck);
        pythonBundledCoverageCheck.setOnAction(e -> fireModified());
        pythonBranchCoverageCheck.setOnAction(e -> fireModified());

        VBox pyCoverageBox = new VBox(6,
                pythonBundledCoverageCheck,
                pythonBranchCoverageCheck
        );
        pyCoverageBox.setPadding(new Insets(0, 0, 4, 16));

        // --- 3. Java Coverage ---
        Label javaCoverageHeader = new Label("Java Coverage");
        javaCoverageHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 6 0 2 0;");

        Label runnerLabel = new Label("Choose coverage runner:");
        runnerLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        runnerCombo.getItems().addAll(CoverageSettings.getAvailableRunners());
        runnerCombo.setValue(CoverageSettings.RUNNER_LUMINA);
        runnerCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        runnerCombo.setPrefWidth(140);
        runnerCombo.setOnAction(e -> fireModified());

        HBox runnerRow = new HBox(8, runnerLabel, runnerCombo);
        runnerRow.setAlignment(Pos.CENTER_LEFT);
        runnerRow.setPadding(new Insets(0, 0, 4, 0));

        styleCheckBox(javaBranchCoverageCheck);
        javaBranchCoverageCheck.setOnAction(e -> fireModified());
        javaBranchHint.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");

        styleCheckBox(javaTrackPerTestCheck);
        javaTrackPerTestCheck.setOnAction(e -> fireModified());
        javaTrackHint.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");

        styleCheckBox(javaTestFoldersCheck);
        javaTestFoldersCheck.setOnAction(e -> fireModified());

        styleCheckBox(javaIgnoreDefaultConstructorsCheck);
        javaIgnoreDefaultConstructorsCheck.setOnAction(e -> fireModified());

        // Exclude annotations
        Label excludeAnnotationsLabel = new Label("Exclude annotations:");
        excludeAnnotationsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 0 2 0;");

        styleToolbarButton(addAnnotationBtn);
        styleToolbarButton(removeAnnotationBtn);
        addAnnotationBtn.setOnAction(e -> onAddAnnotation());
        removeAnnotationBtn.setOnAction(e -> onRemoveAnnotation());

        HBox toolbar = new HBox(2, addAnnotationBtn, removeAnnotationBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 4, 2, 4));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-width: 1 1 0 1;");

        excludeAnnotationsListView.setPrefHeight(120);
        excludeAnnotationsListView.setMaxHeight(160);
        excludeAnnotationsListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #43454A; -fx-border-width: 0 1 1 1;");
        excludeAnnotationsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8;");
                }
            }
        });

        VBox annotationsBox = new VBox(toolbar, excludeAnnotationsListView);

        VBox javaCoverageBox = new VBox(6,
                runnerRow,
                javaBranchCoverageCheck,
                javaBranchHint,
                javaTrackPerTestCheck,
                javaTrackHint,
                javaTestFoldersCheck,
                javaIgnoreDefaultConstructorsCheck,
                excludeAnnotationsLabel,
                annotationsBox
        );
        javaCoverageBox.setPadding(new Insets(0, 0, 4, 16));

        getChildren().addAll(
                gatherHeader,
                gatherBox,
                pyCoverageHeader,
                pyCoverageBox,
                javaCoverageHeader,
                javaCoverageBox
        );
    }

    private void onAddAnnotation() {
        TextInputDialog dialog = new TextInputDialog("*Generated*");
        dialog.setTitle("Exclude Annotation");
        dialog.setHeaderText("Add Annotation Pattern to Exclude");
        dialog.setContentText("Pattern:");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(pattern -> {
            String trimmed = pattern.trim();
            if (!trimmed.isEmpty() && !excludeAnnotationsList.contains(trimmed)) {
                excludeAnnotationsList.add(trimmed);
                fireModified();
            }
        });
    }

    private void onRemoveAnnotation() {
        String selected = excludeAnnotationsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            excludeAnnotationsList.remove(selected);
            fireModified();
        }
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleToolbarButton(Button b) {
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8C8C; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 1 6;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #35373B; -fx-text-fill: #DFE1E5; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 1 6;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8C8C; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 1 6;"));
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        switch (initialSettings.getGatherPolicy()) {
            case SHOW_OPTIONS -> showOptionsRadio.setSelected(true);
            case DO_NOT_APPLY -> doNotApplyRadio.setSelected(true);
            case REPLACE_ACTIVE_SUITES -> replaceActiveSuitesRadio.setSelected(true);
            case ADD_TO_ACTIVE_SUITES -> addToActiveSuitesRadio.setSelected(true);
        }

        activateCoverageViewCheck.setSelected(initialSettings.isActivateCoverageView());
        showCoverageInProjectViewCheck.setSelected(initialSettings.isShowCoverageInProjectView());

        pythonBundledCoverageCheck.setSelected(initialSettings.isPythonUseBundledCoverage());
        pythonBranchCoverageCheck.setSelected(initialSettings.isPythonBranchCoverage());

        runnerCombo.setValue(initialSettings.getJavaCoverageRunner());
        javaBranchCoverageCheck.setSelected(initialSettings.isJavaBranchCoverage());
        javaTrackPerTestCheck.setSelected(initialSettings.isJavaTrackPerTestCoverage());
        javaTestFoldersCheck.setSelected(initialSettings.isJavaCollectInTestFolders());
        javaIgnoreDefaultConstructorsCheck.setSelected(initialSettings.isJavaIgnoreDefaultConstructors());

        excludeAnnotationsList.setAll(initialSettings.getExcludeAnnotations());

        updating = false;
    }

    public CoverageSettings getCurrentSettings() {
        CoverageSettings s = new CoverageSettings();

        if (showOptionsRadio.isSelected()) {
            s.setGatherPolicy(CoverageSettings.GatherPolicy.SHOW_OPTIONS);
        } else if (doNotApplyRadio.isSelected()) {
            s.setGatherPolicy(CoverageSettings.GatherPolicy.DO_NOT_APPLY);
        } else if (replaceActiveSuitesRadio.isSelected()) {
            s.setGatherPolicy(CoverageSettings.GatherPolicy.REPLACE_ACTIVE_SUITES);
        } else if (addToActiveSuitesRadio.isSelected()) {
            s.setGatherPolicy(CoverageSettings.GatherPolicy.ADD_TO_ACTIVE_SUITES);
        }

        s.setActivateCoverageView(activateCoverageViewCheck.isSelected());
        s.setShowCoverageInProjectView(showCoverageInProjectViewCheck.isSelected());

        s.setPythonUseBundledCoverage(pythonBundledCoverageCheck.isSelected());
        s.setPythonBranchCoverage(pythonBranchCoverageCheck.isSelected());

        s.setJavaCoverageRunner(runnerCombo.getValue());
        s.setJavaBranchCoverage(javaBranchCoverageCheck.isSelected());
        s.setJavaTrackPerTestCoverage(javaTrackPerTestCheck.isSelected());
        s.setJavaCollectInTestFolders(javaTestFoldersCheck.isSelected());
        s.setJavaIgnoreDefaultConstructors(javaIgnoreDefaultConstructorsCheck.isSelected());

        s.setExcludeAnnotations(new ArrayList<>(excludeAnnotationsList));
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        CoverageSettings current = getCurrentSettings();
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

    public RadioButton getShowOptionsRadio() { return showOptionsRadio; }
    public RadioButton getDoNotApplyRadio() { return doNotApplyRadio; }
    public RadioButton getReplaceActiveSuitesRadio() { return replaceActiveSuitesRadio; }
    public RadioButton getAddToActiveSuitesRadio() { return addToActiveSuitesRadio; }
    public CheckBox getActivateCoverageViewCheck() { return activateCoverageViewCheck; }
    public CheckBox getShowCoverageInProjectViewCheck() { return showCoverageInProjectViewCheck; }
    public CheckBox getPythonBundledCoverageCheck() { return pythonBundledCoverageCheck; }
    public CheckBox getPythonBranchCoverageCheck() { return pythonBranchCoverageCheck; }
    public ComboBox<String> getRunnerCombo() { return runnerCombo; }
    public CheckBox getJavaBranchCoverageCheck() { return javaBranchCoverageCheck; }
    public CheckBox getJavaTrackPerTestCheck() { return javaTrackPerTestCheck; }
    public CheckBox getJavaTestFoldersCheck() { return javaTestFoldersCheck; }
    public CheckBox getJavaIgnoreDefaultConstructorsCheck() { return javaIgnoreDefaultConstructorsCheck; }
    public ObservableList<String> getExcludeAnnotationsList() { return excludeAnnotationsList; }
    public Button getAddAnnotationBtn() { return addAnnotationBtn; }
    public Button getRemoveAnnotationBtn() { return removeAnnotationBtn; }
}
