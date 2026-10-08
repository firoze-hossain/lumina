package dev.lumina.ui;

import dev.lumina.build.MavenInstallation;
import dev.lumina.build.MavenSettings;
import dev.lumina.build.MavenSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * Settings page for Build, Execution, Deployment > Build Tools > Maven.
 * Matches 1:1 with reference specification.
 */
public class SettingsMavenPage extends VBox {

    private final MavenSettingsManager manager = MavenSettingsManager.getInstance();

    private final CheckBox workOfflineCheck = new CheckBox("Work offline");
    private final CheckBox executeRecursivelyCheck = new CheckBox("Execute goals recursively");
    private final CheckBox printStackTracesCheck = new CheckBox("Print exception stack traces");
    private final CheckBox updateSnapshotsCheck = new CheckBox("Always update snapshots");

    private final ComboBox<String> outputLevelCombo = new ComboBox<>();
    private final ComboBox<String> checksumPolicyCombo = new ComboBox<>();
    private final ComboBox<String> failPolicyCombo = new ComboBox<>();
    private final TextField threadCountField = new TextField();

    private final ComboBox<String> mavenHomeCombo = new ComboBox<>();
    private final Button mavenHomeBrowseBtn = new Button("…");
    private final Label mavenVersionLabel = new Label("(Version: 3.9.11)");

    private final TextField userSettingsField = new TextField();
    private final Button userSettingsBrowseBtn = new Button();
    private final CheckBox userSettingsOverrideCheck = new CheckBox("Override");

    private final TextField localRepoField = new TextField();
    private final Button localRepoBrowseBtn = new Button();
    private final CheckBox localRepoOverrideCheck = new CheckBox("Override");

    private final CheckBox useMavenConfigCheck = new CheckBox("Use settings from .mvn/maven.config");

    private MavenSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsMavenPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // Top 4 checkboxes
        styleCheckBox(workOfflineCheck);
        styleCheckBox(executeRecursivelyCheck);
        styleCheckBox(printStackTracesCheck);
        styleCheckBox(updateSnapshotsCheck);

        VBox topChecks = new VBox(10, workOfflineCheck, executeRecursivelyCheck, printStackTracesCheck, updateSnapshotsCheck);
        getChildren().add(topChecks);

        // Form Grid
        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(10, 0, 10, 0));

        ColumnConstraints colLabel = new ColumnConstraints(200);
        ColumnConstraints colField = new ColumnConstraints(300, 360, Double.MAX_VALUE);
        colField.setHgrow(Priority.ALWAYS);
        ColumnConstraints colExtra = new ColumnConstraints();
        colExtra.setHgrow(Priority.NEVER);

        formGrid.getColumnConstraints().addAll(colLabel, colField, colExtra);

        int row = 0;

        // Row 1: Output level
        Label outputLabel = new Label("Output level:");
        styleLabel(outputLabel);
        outputLevelCombo.getItems().setAll("Debug", "Info", "Warn", "Error", "Fatal");
        styleComboBox(outputLevelCombo);
        outputLevelCombo.setPrefWidth(160);
        formGrid.add(outputLabel, 0, row);
        formGrid.add(outputLevelCombo, 1, row);
        row++;

        // Row 2: Checksum policy
        Label checksumLabel = new Label("Checksum policy:");
        styleLabel(checksumLabel);
        checksumPolicyCombo.getItems().setAll("Strict", "Relaxed", "No Global Policy");
        styleComboBox(checksumPolicyCombo);
        checksumPolicyCombo.setPrefWidth(160);
        formGrid.add(checksumLabel, 0, row);
        formGrid.add(checksumPolicyCombo, 1, row);
        row++;

        // Row 3: Multiproject build fail policy
        Label failLabel = new Label("Multiproject build fail policy:");
        styleLabel(failLabel);
        failPolicyCombo.getItems().setAll("Default", "Fail at end", "Fail never");
        styleComboBox(failPolicyCombo);
        failPolicyCombo.setPrefWidth(160);
        formGrid.add(failLabel, 0, row);
        formGrid.add(failPolicyCombo, 1, row);
        row++;

        // Row 4: Thread count
        Label threadLabel = new Label("Thread count:");
        styleLabel(threadLabel);
        styleTextField(threadCountField);
        threadCountField.setPrefWidth(160);
        Label threadHint = new Label("-T option");
        threadHint.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 13px;");
        HBox threadBox = new HBox(8, threadCountField, threadHint);
        threadBox.setAlignment(Pos.CENTER_LEFT);
        formGrid.add(threadLabel, 0, row);
        formGrid.add(threadBox, 1, row);
        row++;

        // Row 5: Maven home path
        Label mavenHomeLabel = new Label("Maven home path:");
        styleLabel(mavenHomeLabel);

        populateMavenHomes();
        styleComboBox(mavenHomeCombo);
        mavenHomeCombo.setEditable(true);
        HBox.setHgrow(mavenHomeCombo, Priority.ALWAYS);

        styleBrowseButton(mavenHomeBrowseBtn);
        mavenHomeBrowseBtn.setOnAction(e -> handleBrowseMavenHome());

        HBox mavenHomeRow = new HBox(6, mavenHomeCombo, mavenHomeBrowseBtn);
        mavenHomeRow.setAlignment(Pos.CENTER_LEFT);

        mavenVersionLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        VBox mavenHomeBox = new VBox(4, mavenHomeRow, mavenVersionLabel);
        formGrid.add(mavenHomeLabel, 0, row);
        formGrid.add(mavenHomeBox, 1, row);
        row++;

        // Row 6: User settings file
        Label userSettingsLabel = new Label("User settings file:");
        styleLabel(userSettingsLabel);

        styleTextField(userSettingsField);
        HBox.setHgrow(userSettingsField, Priority.ALWAYS);

        userSettingsBrowseBtn.setGraphic(createFolderIcon());
        styleFolderButton(userSettingsBrowseBtn);
        userSettingsBrowseBtn.setOnAction(e -> handleBrowseUserSettings());

        styleCheckBox(userSettingsOverrideCheck);

        // Bind disable state of field and browse button to override checkbox
        userSettingsField.disableProperty().bind(userSettingsOverrideCheck.selectedProperty().not());
        userSettingsBrowseBtn.disableProperty().bind(userSettingsOverrideCheck.selectedProperty().not());

        HBox userSettingsBox = new HBox(6, userSettingsField, userSettingsBrowseBtn);
        userSettingsBox.setAlignment(Pos.CENTER_LEFT);

        formGrid.add(userSettingsLabel, 0, row);
        formGrid.add(userSettingsBox, 1, row);
        formGrid.add(userSettingsOverrideCheck, 2, row);
        row++;

        // Row 7: Local repository
        Label localRepoLabel = new Label("Local repository:");
        styleLabel(localRepoLabel);

        styleTextField(localRepoField);
        HBox.setHgrow(localRepoField, Priority.ALWAYS);

        localRepoBrowseBtn.setGraphic(createFolderIcon());
        styleFolderButton(localRepoBrowseBtn);
        localRepoBrowseBtn.setOnAction(e -> handleBrowseLocalRepo());

        styleCheckBox(localRepoOverrideCheck);

        // Bind disable state of field and browse button to override checkbox
        localRepoField.disableProperty().bind(localRepoOverrideCheck.selectedProperty().not());
        localRepoBrowseBtn.disableProperty().bind(localRepoOverrideCheck.selectedProperty().not());

        HBox localRepoBox = new HBox(6, localRepoField, localRepoBrowseBtn);
        localRepoBox.setAlignment(Pos.CENTER_LEFT);

        formGrid.add(localRepoLabel, 0, row);
        formGrid.add(localRepoBox, 1, row);
        formGrid.add(localRepoOverrideCheck, 2, row);
        row++;

        getChildren().add(formGrid);

        // Bottom checkbox: Use settings from .mvn/maven.config
        styleCheckBox(useMavenConfigCheck);
        getChildren().add(useMavenConfigCheck);

        // Change listeners for dirty tracking
        workOfflineCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        executeRecursivelyCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        printStackTracesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        updateSnapshotsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        outputLevelCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        checksumPolicyCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        failPolicyCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        threadCountField.textProperty().addListener((obs, o, n) -> notifyModified());
        mavenHomeCombo.valueProperty().addListener((obs, o, n) -> {
            updateMavenVersion(n);
            notifyModified();
        });
        userSettingsField.textProperty().addListener((obs, o, n) -> notifyModified());
        userSettingsOverrideCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        localRepoField.textProperty().addListener((obs, o, n) -> notifyModified());
        localRepoOverrideCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        useMavenConfigCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void populateMavenHomes() {
        mavenHomeCombo.getItems().clear();
        List<MavenInstallation> installs = manager.getDiscoveredInstallations();
        for (MavenInstallation inst : installs) {
            mavenHomeCombo.getItems().add(inst.getName());
        }
        if (mavenHomeCombo.getItems().isEmpty()) {
            mavenHomeCombo.getItems().add("Bundled (Maven 3)");
        }
    }

    private void updateMavenVersion(String selected) {
        String ver = manager.detectVersion(selected);
        mavenVersionLabel.setText("(Version: " + ver + ")");
    }

    private void handleBrowseMavenHome() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Maven Home Directory");
        File chosen = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (chosen != null) {
            String path = chosen.getAbsolutePath();
            if (!mavenHomeCombo.getItems().contains(path)) {
                mavenHomeCombo.getItems().add(path);
            }
            mavenHomeCombo.setValue(path);
        }
    }

    private void handleBrowseUserSettings() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select settings.xml");
        File chosen = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (chosen != null) {
            userSettingsField.setText(chosen.getAbsolutePath());
        }
    }

    private void handleBrowseLocalRepo() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Local Repository Directory");
        File chosen = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (chosen != null) {
            localRepoField.setText(chosen.getAbsolutePath());
        }
    }

    private Node createFolderIcon() {
        Canvas c = new Canvas(14, 14);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.web("#8C919D"));
        // Folder tab
        gc.fillRoundRect(1, 2, 6, 4, 2, 2);
        // Folder body
        gc.fillRoundRect(1, 4, 12, 8, 2, 2);
        return c;
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
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
    }

    private void styleBrowseButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10;");
    }

    private void styleFolderButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-padding: 4 8;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        workOfflineCheck.setSelected(initialSettings.isWorkOffline());
        executeRecursivelyCheck.setSelected(initialSettings.isExecuteGoalsRecursively());
        printStackTracesCheck.setSelected(initialSettings.isPrintExceptionStackTraces());
        updateSnapshotsCheck.setSelected(initialSettings.isAlwaysUpdateSnapshots());

        outputLevelCombo.setValue(initialSettings.getOutputLevel());
        checksumPolicyCombo.setValue(initialSettings.getChecksumPolicy());
        failPolicyCombo.setValue(initialSettings.getMultiprojectFailPolicy());
        threadCountField.setText(initialSettings.getThreadCount());

        mavenHomeCombo.setValue(initialSettings.getMavenHome());
        mavenVersionLabel.setText("(Version: " + initialSettings.getMavenVersion() + ")");

        userSettingsOverrideCheck.setSelected(initialSettings.isUserSettingsOverride());
        userSettingsField.setText(initialSettings.getUserSettingsFile());

        localRepoOverrideCheck.setSelected(initialSettings.isLocalRepoOverride());
        localRepoField.setText(initialSettings.getLocalRepo());

        useMavenConfigCheck.setSelected(initialSettings.isUseMavenConfig());

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        MavenSettings current = getCurrentSettings();
        return !initialSettings.equals(current);
    }

    public void apply() {
        MavenSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
    }

    public void reset() {
        loadData();
    }

    public MavenSettings getCurrentSettings() {
        MavenSettings s = new MavenSettings();
        s.setWorkOffline(workOfflineCheck.isSelected());
        s.setExecuteGoalsRecursively(executeRecursivelyCheck.isSelected());
        s.setPrintExceptionStackTraces(printStackTracesCheck.isSelected());
        s.setAlwaysUpdateSnapshots(updateSnapshotsCheck.isSelected());
        s.setOutputLevel(outputLevelCombo.getValue());
        s.setChecksumPolicy(checksumPolicyCombo.getValue());
        s.setMultiprojectFailPolicy(failPolicyCombo.getValue());
        s.setThreadCount(threadCountField.getText());
        s.setMavenHome(mavenHomeCombo.getValue());
        s.setMavenVersion(manager.detectVersion(mavenHomeCombo.getValue()));
        s.setUserSettingsOverride(userSettingsOverrideCheck.isSelected());
        s.setUserSettingsFile(userSettingsField.getText());
        s.setLocalRepoOverride(localRepoOverrideCheck.isSelected());
        s.setLocalRepo(localRepoField.getText());
        s.setUseMavenConfig(useMavenConfigCheck.isSelected());
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
