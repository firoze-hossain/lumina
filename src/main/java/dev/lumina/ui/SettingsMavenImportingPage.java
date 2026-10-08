package dev.lumina.ui;

import dev.lumina.build.MavenSettings;
import dev.lumina.build.MavenSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.util.List;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > Maven > Importing.
 * Matches 1:1 with reference specification and dynamically configures importer options and JDK.
 */
public class SettingsMavenImportingPage extends VBox {

    private final MavenSettingsManager manager = MavenSettingsManager.getInstance();

    private final CheckBox detectCompilerCheck = new CheckBox("Detect compiler automatically");
    private final CheckBox excludeBuildDirCheck = new CheckBox("Exclude build directory (%PROJECT_ROOT%/target)");
    private final CheckBox useOutputDirsCheck = new CheckBox("Use Maven output directories");

    private final ComboBox<String> generatedSourcesCombo = new ComboBox<>();
    private final ComboBox<String> foldersPhaseCombo = new ComboBox<>();

    private final CheckBox downloadSourcesCheck = new CheckBox("Sources");
    private final CheckBox downloadDocsCheck = new CheckBox("Documentation");
    private final CheckBox downloadAnnotationsCheck = new CheckBox("Annotations");

    private final TextField dependencyTypesField = new TextField();
    private final TextField vmOptionsField = new TextField();
    private final ComboBox<String> jdkCombo = new ComboBox<>();

    private MavenSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsMavenImportingPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // Top 3 checkboxes
        styleCheckBox(detectCompilerCheck);
        styleCheckBox(excludeBuildDirCheck);
        styleCheckBox(useOutputDirsCheck);

        VBox topChecks = new VBox(10, detectCompilerCheck, excludeBuildDirCheck, useOutputDirsCheck);
        getChildren().add(topChecks);

        // Form Grid
        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(12);
        formGrid.setPadding(new Insets(10, 0, 10, 0));

        ColumnConstraints colLabel = new ColumnConstraints(190);
        ColumnConstraints colField = new ColumnConstraints(300, 480, Double.MAX_VALUE);
        colField.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().addAll(colLabel, colField);

        int row = 0;

        // Row 1: Generated sources folders
        Label genLabel = new Label("Generated sources folders:");
        styleLabel(genLabel);
        generatedSourcesCombo.getItems().setAll(
                "Detect automatically",
                "subdirectories under \"target/generated-sources\"",
                "\"target/generated-sources\" directory"
        );
        styleComboBox(generatedSourcesCombo);
        generatedSourcesCombo.setPrefWidth(240);
        formGrid.add(genLabel, 0, row);
        formGrid.add(generatedSourcesCombo, 1, row);
        row++;

        // Row 2: Phase to be used for folders update
        Label phaseLabel = new Label("Phase to be used for folders update:");
        styleLabel(phaseLabel);
        foldersPhaseCombo.getItems().setAll(
                "generate-sources",
                "process-sources",
                "generate-resources",
                "process-resources",
                "process-classes",
                "compile",
                "prepare-package",
                "package",
                "none"
        );
        styleComboBox(foldersPhaseCombo);
        foldersPhaseCombo.setPrefWidth(180);

        Label phaseNote = new Label("Lumina IDE needs to execute one of the listed phases in order to discover all source folders that are configured via Maven plugins.\nNote that all test-* phases firstly generate and compile production sources.");
        phaseNote.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        VBox phaseBox = new VBox(5, foldersPhaseCombo, phaseNote);
        formGrid.add(phaseLabel, 0, row);
        formGrid.add(phaseBox, 1, row);
        row++;

        // Row 3: Automatically download
        Label downloadLabel = new Label("Automatically download:");
        styleLabel(downloadLabel);
        styleCheckBox(downloadSourcesCheck);
        styleCheckBox(downloadDocsCheck);
        styleCheckBox(downloadAnnotationsCheck);

        HBox downloadBox = new HBox(16, downloadSourcesCheck, downloadDocsCheck, downloadAnnotationsCheck);
        downloadBox.setAlignment(Pos.CENTER_LEFT);
        formGrid.add(downloadLabel, 0, row);
        formGrid.add(downloadBox, 1, row);
        row++;

        // Row 4: Dependency types
        Label depLabel = new Label("Dependency types:");
        styleLabel(depLabel);
        styleTextField(dependencyTypesField);
        Label depNote = new Label("Comma-separated list of dependency types that should be imported");
        depNote.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        VBox depBox = new VBox(5, dependencyTypesField, depNote);
        formGrid.add(depLabel, 0, row);
        formGrid.add(depBox, 1, row);
        row++;

        // Row 5: VM options for importer
        Label vmLabel = new Label("VM options for importer:");
        styleLabel(vmLabel);
        styleTextField(vmOptionsField);
        Label vmNote = new Label("Options specified in this field override the ones in .mvn/jvm.config files");
        vmNote.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        VBox vmBox = new VBox(5, vmOptionsField, vmNote);
        formGrid.add(vmLabel, 0, row);
        formGrid.add(vmBox, 1, row);
        row++;

        // Row 6: JDK for importer
        Label jdkLabel = new Label("JDK for importer:");
        styleLabel(jdkLabel);
        setupJdkCombo();
        HBox.setHgrow(jdkCombo, Priority.ALWAYS);
        formGrid.add(jdkLabel, 0, row);
        formGrid.add(jdkCombo, 1, row);
        row++;

        getChildren().add(formGrid);

        // Change listeners
        detectCompilerCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        excludeBuildDirCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        useOutputDirsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        generatedSourcesCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        foldersPhaseCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        downloadSourcesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        downloadDocsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        downloadAnnotationsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        dependencyTypesField.textProperty().addListener((obs, o, n) -> notifyModified());
        vmOptionsField.textProperty().addListener((obs, o, n) -> notifyModified());
        jdkCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void setupJdkCombo() {
        styleComboBox(jdkCombo);
        List<String> options = MavenSettingsManager.getAvailableJdkOptions();
        jdkCombo.getItems().setAll(options);

        jdkCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setTextFill(Color.web("#DFE1E5"));
                    setGraphic(createFolderSdkIcon());
                }
            }
        });

        jdkCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setTextFill(Color.web("#DFE1E5"));
                    setGraphic(createFolderSdkIcon());
                }
            }
        });
    }

    private Node createFolderSdkIcon() {
        SVGPath p = new SVGPath();
        p.setContent("M 2,3 L 6,3 L 7.5,4.5 L 14,4.5 L 14,12 L 2,12 Z");
        p.setStroke(Color.web("#3574F0"));
        p.setStrokeWidth(1.2);
        p.setFill(null);
        return p;
    }

    private void styleLabel(Label l) {
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        detectCompilerCheck.setSelected(initialSettings.isDetectCompilerAutomatically());
        excludeBuildDirCheck.setSelected(initialSettings.isExcludeTargetDirectory());
        useOutputDirsCheck.setSelected(initialSettings.isUseMavenOutputDirectories());

        generatedSourcesCombo.setValue(initialSettings.getGeneratedSourcesMode());
        foldersPhaseCombo.setValue(initialSettings.getFoldersUpdatePhase());

        downloadSourcesCheck.setSelected(initialSettings.isDownloadSources());
        downloadDocsCheck.setSelected(initialSettings.isDownloadDocumentation());
        downloadAnnotationsCheck.setSelected(initialSettings.isDownloadAnnotations());

        dependencyTypesField.setText(initialSettings.getDependencyTypes());
        vmOptionsField.setText(initialSettings.getImporterVmOptions());

        String savedJdk = initialSettings.getImporterJdk();
        if (savedJdk == null || savedJdk.isBlank() || !jdkCombo.getItems().contains(savedJdk)) {
            String def = MavenSettingsManager.getDefaultProjectJdkDisplay();
            jdkCombo.setValue(def);
            initialSettings.setImporterJdk(def);
        } else {
            jdkCombo.setValue(savedJdk);
        }

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        MavenSettings cur = getCurrentSettings();
        return initialSettings.isDetectCompilerAutomatically() != cur.isDetectCompilerAutomatically()
                || initialSettings.isExcludeTargetDirectory() != cur.isExcludeTargetDirectory()
                || initialSettings.isUseMavenOutputDirectories() != cur.isUseMavenOutputDirectories()
                || !initialSettings.getGeneratedSourcesMode().equals(cur.getGeneratedSourcesMode())
                || !initialSettings.getFoldersUpdatePhase().equals(cur.getFoldersUpdatePhase())
                || initialSettings.isDownloadSources() != cur.isDownloadSources()
                || initialSettings.isDownloadDocumentation() != cur.isDownloadDocumentation()
                || initialSettings.isDownloadAnnotations() != cur.isDownloadAnnotations()
                || !initialSettings.getDependencyTypes().equals(cur.getDependencyTypes())
                || !initialSettings.getImporterVmOptions().equals(cur.getImporterVmOptions())
                || !initialSettings.getImporterJdk().equals(cur.getImporterJdk());
    }

    public void apply() {
        MavenSettings cur = getCurrentSettings();
        MavenSettings s = manager.getSettings();
        s.setDetectCompilerAutomatically(cur.isDetectCompilerAutomatically());
        s.setExcludeTargetDirectory(cur.isExcludeTargetDirectory());
        s.setUseMavenOutputDirectories(cur.isUseMavenOutputDirectories());
        s.setGeneratedSourcesMode(cur.getGeneratedSourcesMode());
        s.setFoldersUpdatePhase(cur.getFoldersUpdatePhase());
        s.setDownloadSources(cur.isDownloadSources());
        s.setDownloadDocumentation(cur.isDownloadDocumentation());
        s.setDownloadAnnotations(cur.isDownloadAnnotations());
        s.setDependencyTypes(cur.getDependencyTypes());
        s.setImporterVmOptions(cur.getImporterVmOptions());
        s.setImporterJdk(cur.getImporterJdk());
        manager.setSettings(s);
        initialSettings = s.clone();
    }

    public void reset() {
        loadData();
    }

    public MavenSettings getCurrentSettings() {
        MavenSettings s = (initialSettings != null) ? initialSettings.clone() : new MavenSettings();
        s.setDetectCompilerAutomatically(detectCompilerCheck.isSelected());
        s.setExcludeTargetDirectory(excludeBuildDirCheck.isSelected());
        s.setUseMavenOutputDirectories(useOutputDirsCheck.isSelected());
        s.setGeneratedSourcesMode(generatedSourcesCombo.getValue());
        s.setFoldersUpdatePhase(foldersPhaseCombo.getValue());
        s.setDownloadSources(downloadSourcesCheck.isSelected());
        s.setDownloadDocumentation(downloadDocsCheck.isSelected());
        s.setDownloadAnnotations(downloadAnnotationsCheck.isSelected());
        s.setDependencyTypes(dependencyTypesField.getText());
        s.setImporterVmOptions(vmOptionsField.getText());
        s.setImporterJdk(jdkCombo.getValue() != null ? jdkCombo.getValue() : "");
        return s;
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
