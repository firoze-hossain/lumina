package dev.lumina.ui;

import dev.lumina.build.PythonDebuggerManager;
import dev.lumina.build.PythonDebuggerSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Settings page for Build, Execution, Deployment > Python Debugger.
 * Accurately replicates the UI and behavior shown in reference specification.
 */
public class SettingsPythonDebuggerPage extends VBox {

    private final PythonDebuggerManager manager = PythonDebuggerManager.getInstance();

    private final CheckBox attachToSubprocessCheck = new CheckBox("Attach to subprocess automatically while debugging");
    private final CheckBox collectRunTimeTypesCheck = new CheckBox("Collect run-time types information for code insight");
    private final CheckBox geventCheck = new CheckBox("Gevent compatible");
    private final CheckBox dropIntoDebuggerCheck = new CheckBox("Drop into debugger on failed tests");
    private final CheckBox pyQtCompatibleCheck = new CheckBox("PyQt compatible");
    private final ComboBox<String> pyQtCombo = new ComboBox<>();
    private final TextField attachProcessFilterField = new TextField();
    private final Spinner<Integer> timeoutSpinner = new Spinner<>(1000, 3600000, 60000, 1000);
    private final Hyperlink clearCachesLink = new Hyperlink("Clear caches");
    private final Label cacheFeedbackLabel = new Label();

    private PythonDebuggerSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsPythonDebuggerPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        styleCheckBox(attachToSubprocessCheck);
        styleCheckBox(collectRunTimeTypesCheck);
        styleCheckBox(geventCheck);
        styleCheckBox(dropIntoDebuggerCheck);
        styleCheckBox(pyQtCompatibleCheck);

        // 1. Attach subprocess
        getChildren().add(attachToSubprocessCheck);

        // 2. Collect run-time types row with warning icon and Clear caches link
        HBox typesRow = new HBox(12);
        typesRow.setAlignment(Pos.CENTER_LEFT);

        // Warning triangle icon
        Polygon warningTriangle = new Polygon();
        warningTriangle.getPoints().addAll(
                0.0, 12.0,
                6.0, 0.0,
                12.0, 12.0
        );
        warningTriangle.setFill(Color.web("#E5A158"));

        clearCachesLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;");
        clearCachesLink.setOnMouseEntered(e -> clearCachesLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: true;"));
        clearCachesLink.setOnMouseExited(e -> clearCachesLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;"));
        clearCachesLink.setOnAction(e -> {
            manager.clearCaches();
            cacheFeedbackLabel.setText("Caches cleared");
            cacheFeedbackLabel.setVisible(true);
        });

        cacheFeedbackLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        cacheFeedbackLabel.setVisible(false);

        HBox cacheActions = new HBox(6, warningTriangle, clearCachesLink, cacheFeedbackLabel);
        cacheActions.setAlignment(Pos.CENTER_LEFT);
        cacheActions.setPadding(new Insets(0, 0, 0, 40));

        typesRow.getChildren().addAll(collectRunTimeTypesCheck, cacheActions);
        getChildren().add(typesRow);

        // 3. Gevent
        getChildren().add(geventCheck);

        // 4. Drop into debugger on failed tests
        getChildren().add(dropIntoDebuggerCheck);

        // 5. PyQt compatible row
        HBox pyQtRow = new HBox(12);
        pyQtRow.setAlignment(Pos.CENTER_LEFT);

        pyQtCombo.getItems().setAll(manager.getAvailablePyQtBackends());
        pyQtCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");
        pyQtCombo.setPrefWidth(120);
        pyQtCombo.disableProperty().bind(pyQtCompatibleCheck.selectedProperty().not());

        pyQtRow.getChildren().addAll(pyQtCompatibleCheck, pyQtCombo);
        getChildren().add(pyQtRow);

        // 6. Attach to process filter row
        HBox attachFilterRow = new HBox(10);
        attachFilterRow.setAlignment(Pos.CENTER_LEFT);
        attachFilterRow.setPadding(new Insets(6, 0, 0, 0));

        Text prefixText = new Text("For ");
        prefixText.setFill(Color.web("#DFE1E5"));
        prefixText.setFont(Font.font("System", 13));

        Text boldText = new Text("Attach To Process");
        boldText.setFill(Color.web("#DFE1E5"));
        boldText.setFont(Font.font("System", FontWeight.BOLD, 13));

        Text suffixText = new Text(" show processes with names containing:");
        suffixText.setFill(Color.web("#DFE1E5"));
        suffixText.setFont(Font.font("System", 13));

        TextFlow labelFlow = new TextFlow(prefixText, boldText, suffixText);

        styleTextField(attachProcessFilterField);
        attachProcessFilterField.setPrefWidth(180);

        attachFilterRow.getChildren().addAll(labelFlow, attachProcessFilterField);
        getChildren().add(attachFilterRow);

        // 7. Timeout row
        HBox timeoutRow = new HBox(10);
        timeoutRow.setAlignment(Pos.CENTER_LEFT);

        Label timeoutLabel = new Label("Debugger evaluation response timeout (ms):");
        timeoutLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        timeoutSpinner.setEditable(true);
        timeoutSpinner.setPrefWidth(120);
        timeoutSpinner.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");

        timeoutRow.getChildren().addAll(timeoutLabel, timeoutSpinner);
        getChildren().add(timeoutRow);

        // Change listeners for dirty tracking
        attachToSubprocessCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        collectRunTimeTypesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        geventCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        dropIntoDebuggerCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        pyQtCompatibleCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        pyQtCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        attachProcessFilterField.textProperty().addListener((obs, o, n) -> notifyModified());
        timeoutSpinner.valueProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        attachToSubprocessCheck.setSelected(initialSettings.isAttachToSubprocess());
        collectRunTimeTypesCheck.setSelected(initialSettings.isCollectRunTimeTypes());
        geventCheck.setSelected(initialSettings.isGeventCompatible());
        dropIntoDebuggerCheck.setSelected(initialSettings.isDropIntoDebuggerOnFailedTests());
        pyQtCompatibleCheck.setSelected(initialSettings.isPyQtCompatible());
        pyQtCombo.setValue(initialSettings.getPyQtBackend());
        attachProcessFilterField.setText(initialSettings.getAttachProcessFilter());
        timeoutSpinner.getValueFactory().setValue(initialSettings.getEvalResponseTimeoutMs());

        cacheFeedbackLabel.setVisible(false);
        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        PythonDebuggerSettings current = getCurrentSettings();
        return !initialSettings.equals(current);
    }

    public void apply() {
        PythonDebuggerSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
    }

    public void reset() {
        loadData();
    }

    public PythonDebuggerSettings getCurrentSettings() {
        PythonDebuggerSettings s = new PythonDebuggerSettings();
        s.setAttachToSubprocess(attachToSubprocessCheck.isSelected());
        s.setCollectRunTimeTypes(collectRunTimeTypesCheck.isSelected());
        s.setGeventCompatible(geventCheck.isSelected());
        s.setDropIntoDebuggerOnFailedTests(dropIntoDebuggerCheck.isSelected());
        s.setPyQtCompatible(pyQtCompatibleCheck.isSelected());
        s.setPyQtBackend(pyQtCombo.getValue() != null ? pyQtCombo.getValue() : "Auto");
        s.setAttachProcessFilter(attachProcessFilterField.getText());
        s.setEvalResponseTimeoutMs(timeoutSpinner.getValue() != null ? timeoutSpinner.getValue() : 60000);
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
