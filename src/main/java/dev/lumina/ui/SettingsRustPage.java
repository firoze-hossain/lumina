package dev.lumina.ui;

import dev.lumina.rust.RustSettings;
import dev.lumina.rust.RustSettingsManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

/**
 * Settings page for Languages & Frameworks > Rust.
 * Faithfully matches Image 3.
 */
public class SettingsRustPage extends VBox {

    private final RustSettingsManager manager = RustSettingsManager.getInstance();

    private ComboBox<String> toolchainLocationCombo;
    private Button browseToolchainBtn;
    private Label toolchainVersionLabel;
    private TextField standardLibraryField;
    private Button browseStdLibBtn;
    private TextField environmentVariablesField;
    private Button envVarsBtn;

    private CheckBox expandMacrosCheck;
    private CheckBox injectRustDocCheck;

    private RustSettings initialSettings = new RustSettings();
    private Runnable onModified;

    public SettingsRustPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadFromManager();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // 1. Toolchain Location
        HBox toolchainRow = new HBox(12);
        toolchainRow.setAlignment(Pos.CENTER_LEFT);

        Label toolchainLabel = new Label("Toolchain location:");
        toolchainLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        toolchainLabel.setPrefWidth(160);

        List<String> locations = RustSettingsManager.getAvailableToolchainLocations();
        toolchainLocationCombo = new ComboBox<>(FXCollections.observableArrayList(locations));
        toolchainLocationCombo.setEditable(true);
        toolchainLocationCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 13px;");
        HBox.setHgrow(toolchainLocationCombo, Priority.ALWAYS);
        toolchainLocationCombo.valueProperty().addListener((obs, ov, nv) -> {
            updateDetectedVersion(nv);
            notifyModified();
        });

        browseToolchainBtn = new Button("...");
        browseToolchainBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 3 8; -fx-cursor: hand;");
        browseToolchainBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Rust Toolchain Directory");
            String current = toolchainLocationCombo.getValue();
            if (current != null && !current.isBlank()) {
                File curFile = new File(current);
                if (curFile.isDirectory()) chooser.setInitialDirectory(curFile);
            }
            Window win = getScene() != null ? getScene().getWindow() : null;
            File chosen = chooser.showDialog(win);
            if (chosen != null) {
                toolchainLocationCombo.setValue(chosen.getAbsolutePath());
            }
        });

        toolchainRow.getChildren().addAll(toolchainLabel, toolchainLocationCombo, browseToolchainBtn);

        // 2. Toolchain Version
        HBox versionRow = new HBox(12);
        versionRow.setAlignment(Pos.CENTER_LEFT);

        Label versionTitle = new Label("Toolchain version:");
        versionTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        versionTitle.setPrefWidth(160);

        toolchainVersionLabel = new Label("");
        toolchainVersionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        versionRow.getChildren().addAll(versionTitle, toolchainVersionLabel);

        // 3. Standard Library
        HBox stdLibRow = new HBox(12);
        stdLibRow.setAlignment(Pos.CENTER_LEFT);

        Label stdLibLabel = new Label("Standard library:");
        stdLibLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        stdLibLabel.setPrefWidth(160);

        standardLibraryField = new TextField();
        standardLibraryField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(standardLibraryField, Priority.ALWAYS);
        standardLibraryField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        browseStdLibBtn = new Button("📁");
        browseStdLibBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-cursor: hand; -fx-padding: 3 6;");
        browseStdLibBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Rust Standard Library Directory");
            String current = standardLibraryField.getText();
            if (current != null && !current.isBlank()) {
                File curFile = new File(current);
                if (curFile.isDirectory()) chooser.setInitialDirectory(curFile);
            }
            Window win = getScene() != null ? getScene().getWindow() : null;
            File chosen = chooser.showDialog(win);
            if (chosen != null) {
                standardLibraryField.setText(chosen.getAbsolutePath());
            }
        });

        stdLibRow.getChildren().addAll(stdLibLabel, standardLibraryField, browseStdLibBtn);

        // 4. Environment Variables
        HBox envRow = new HBox(12);
        envRow.setAlignment(Pos.CENTER_LEFT);

        Label envLabel = new Label("Environment variables:");
        envLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        envLabel.setPrefWidth(160);

        environmentVariablesField = new TextField();
        environmentVariablesField.setPromptText("Environment variables");
        environmentVariablesField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(environmentVariablesField, Priority.ALWAYS);
        environmentVariablesField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        envVarsBtn = new Button("🗔");
        envVarsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-cursor: hand; -fx-padding: 3 6;");
        envVarsBtn.setOnAction(e -> showEnvironmentVariablesDialog());

        envRow.getChildren().addAll(envLabel, environmentVariablesField, envVarsBtn);

        // 5. Expand Macros Checkbox & Subtext
        expandMacrosCheck = new CheckBox("Expand macros");
        expandMacrosCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        expandMacrosCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        Label expandMacrosSub = new Label("Enable the processing of macro invocations. This will allow for proper name resolution and type inference for macros");
        expandMacrosSub.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-padding: 0 0 0 22;");

        VBox expandMacrosBox = new VBox(3, expandMacrosCheck, expandMacrosSub);

        // 6. Inject Rust doc comments Checkbox
        injectRustDocCheck = new CheckBox("Inject Rust language into documentation comments");
        injectRustDocCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        injectRustDocCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        getChildren().addAll(
                toolchainRow,
                versionRow,
                stdLibRow,
                envRow,
                expandMacrosBox,
                injectRustDocCheck
        );
    }

    private void updateDetectedVersion(String location) {
        String ver = RustSettingsManager.detectToolchainVersion(location);
        toolchainVersionLabel.setText(ver.isBlank() ? "<not detected>" : ver);
    }

    private void showEnvironmentVariablesDialog() {
        TextInputDialog dlg = new TextInputDialog(environmentVariablesField.getText());
        dlg.setTitle("Environment Variables");
        dlg.setHeaderText("Specify environment variables (e.g. KEY1=VAL1;KEY2=VAL2):");
        dlg.showAndWait().ifPresent(val -> environmentVariablesField.setText(val));
    }

    public void loadFromManager() {
        RustSettings current = manager.getSettings();
        toolchainLocationCombo.setValue(current.getToolchainLocation());
        toolchainVersionLabel.setText(current.getToolchainVersion().isBlank() ?
                RustSettingsManager.detectToolchainVersion(current.getToolchainLocation()) :
                current.getToolchainVersion());
        standardLibraryField.setText(current.getStandardLibrary());
        environmentVariablesField.setText(current.getEnvironmentVariables());
        expandMacrosCheck.setSelected(current.isExpandMacros());
        injectRustDocCheck.setSelected(current.isInjectRustIntoDocComments());

        initialSettings = current.copy();
    }

    private RustSettings buildCurrentSettings() {
        RustSettings current = new RustSettings();
        current.setToolchainLocation(toolchainLocationCombo.getValue() != null ? toolchainLocationCombo.getValue() : "");
        current.setToolchainVersion(toolchainVersionLabel.getText());
        current.setStandardLibrary(standardLibraryField.getText().trim());
        current.setEnvironmentVariables(environmentVariablesField.getText().trim());
        current.setExpandMacros(expandMacrosCheck.isSelected());
        current.setInjectRustIntoDocComments(injectRustDocCheck.isSelected());

        // Preserve external linters and rustfmt
        RustSettings existing = manager.getSettings();
        current.setExternalLinters(existing.getExternalLinters());
        current.setRustfmt(existing.getRustfmt());

        return current;
    }

    public boolean isModified() {
        RustSettings current = buildCurrentSettings();
        return !current.getToolchainLocation().equals(initialSettings.getToolchainLocation()) ||
                !current.getStandardLibrary().equals(initialSettings.getStandardLibrary()) ||
                !current.getEnvironmentVariables().equals(initialSettings.getEnvironmentVariables()) ||
                current.isExpandMacros() != initialSettings.isExpandMacros() ||
                current.isInjectRustIntoDocComments() != initialSettings.isInjectRustIntoDocComments();
    }

    public void apply() {
        RustSettings current = buildCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.copy();
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}