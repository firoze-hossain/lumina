package dev.lumina.ui;

import dev.lumina.database.DatabaseOutputResultsSettings;
import dev.lumina.database.DatabaseOutputResultsSettingsManager;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Tools > Database > Query Execution > Output and Results settings page in Lumina IDE
 * matching 1:1 design of the reference IDE.
 */
public class SettingsDatabaseOutputResultsPage extends VBox {

    private final DatabaseOutputResultsSettingsManager manager;
    private DatabaseOutputResultsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private CheckBox showTimestampCheck;
    private CheckBox enableDbmsOutputCheck;

    private CheckBox showResultsInEditorCheck;
    private CheckBox createTitleFromCommentCheck;
    private TextField treatTextAsTitleAfterField;

    private ComboBox<String> showServicesToolWindowCombo;
    private CheckBox focusOnServicesWindowCheck;
    private CheckBox openNewServicesTabCheck;
    private CheckBox activateServicesOutputPaneCheck;

    public SettingsDatabaseOutputResultsPage() {
        this.manager = DatabaseOutputResultsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Output Section
        VBox outputBox = new VBox(8);
        Label outputHeader = new Label("Output");
        outputHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        showTimestampCheck = new CheckBox("Show timestamp for query output");
        styleCheck(showTimestampCheck);
        showTimestampCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        HBox dbmsBox = new HBox(8);
        dbmsBox.setAlignment(Pos.CENTER_LEFT);
        enableDbmsOutputCheck = new CheckBox("Enable DBMS_OUTPUT");
        styleCheck(enableDbmsOutputCheck);
        enableDbmsOutputCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        Label dbmsSubtext = new Label("Applicable for Oracle and IBM Db2 LUW only");
        dbmsSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px;");
        dbmsBox.getChildren().addAll(enableDbmsOutputCheck, dbmsSubtext);

        outputBox.getChildren().addAll(outputHeader, showTimestampCheck, dbmsBox);

        // 2. Results Section
        VBox resultsBox = new VBox(8);
        VBox.setMargin(resultsBox, new Insets(6, 0, 0, 0));
        Label resultsHeader = new Label("Results");
        resultsHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        showResultsInEditorCheck = new CheckBox("Show results in editor");
        styleCheck(showResultsInEditorCheck);
        showResultsInEditorCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        createTitleFromCommentCheck = new CheckBox("Create title for results from comment before query");
        styleCheck(createTitleFromCommentCheck);
        createTitleFromCommentCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            treatTextAsTitleAfterField.setDisable(!newVal);
            notifyModified();
        });

        HBox treatBox = new HBox(8);
        treatBox.setAlignment(Pos.CENTER_LEFT);
        treatBox.setPadding(new Insets(0, 0, 0, 22));

        Label treatLabel = new Label("Treat text as title after");
        treatLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        treatTextAsTitleAfterField = new TextField();
        treatTextAsTitleAfterField.setPromptText("comment beginning");
        treatTextAsTitleAfterField.setPrefWidth(140);
        treatTextAsTitleAfterField.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-prompt-text-fill: #6F737A; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 3 8 3 8;"
        );
        treatTextAsTitleAfterField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        treatBox.getChildren().addAll(treatLabel, treatTextAsTitleAfterField);

        resultsBox.getChildren().addAll(resultsHeader, showResultsInEditorCheck, createTitleFromCommentCheck, treatBox);

        // 3. Services Tool Window Section with divider
        VBox servicesBox = new VBox(10);
        VBox.setMargin(servicesBox, new Insets(10, 0, 0, 0));

        HBox servicesHeaderBox = new HBox(12);
        servicesHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label servicesHeader = new Label("Services Tool Window");
        servicesHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Separator line = new Separator();
        line.setStyle("-fx-background-color: #393B40; -fx-border-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);
        servicesHeaderBox.getChildren().addAll(servicesHeader, line);

        VBox consoleOutputBox = new VBox(4);
        Label consoleOutputLabel = new Label("Show Services tool window for query console output:");
        consoleOutputLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        showServicesToolWindowCombo = new ComboBox<>();
        showServicesToolWindowCombo.getItems().addAll("For all output", "For errors only", "Never");
        showServicesToolWindowCombo.setPrefWidth(180);
        showServicesToolWindowCombo.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-size: 13px;"
        );
        showServicesToolWindowCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        consoleOutputBox.getChildren().addAll(consoleOutputLabel, showServicesToolWindowCombo);

        focusOnServicesWindowCheck = new CheckBox("Focus on Services tool window in window mode");
        styleCheck(focusOnServicesWindowCheck);
        focusOnServicesWindowCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        openNewServicesTabCheck = new CheckBox("Open new Services tab for sessions");
        styleCheck(openNewServicesTabCheck);
        openNewServicesTabCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        activateServicesOutputPaneCheck = new CheckBox("Activate Services output pane for selected query console only");
        styleCheck(activateServicesOutputPaneCheck);
        activateServicesOutputPaneCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        servicesBox.getChildren().addAll(
                servicesHeaderBox, consoleOutputBox,
                focusOnServicesWindowCheck, openNewServicesTabCheck, activateServicesOutputPaneCheck
        );

        getChildren().addAll(outputBox, resultsBox, servicesBox);
    }

    private void styleCheck(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void loadData() {
        updating = true;
        DatabaseOutputResultsSettings s = manager.getSettings();
        showTimestampCheck.setSelected(s.isShowTimestampForQueryOutput());
        enableDbmsOutputCheck.setSelected(s.isEnableDbmsOutput());
        showResultsInEditorCheck.setSelected(s.isShowResultsInEditor());
        createTitleFromCommentCheck.setSelected(s.isCreateTitleFromComment());
        treatTextAsTitleAfterField.setText(s.getTreatTextAsTitleAfter());
        treatTextAsTitleAfterField.setDisable(!s.isCreateTitleFromComment());
        showServicesToolWindowCombo.setValue(s.getShowServicesToolWindow());
        focusOnServicesWindowCheck.setSelected(s.isFocusOnServicesWindow());
        openNewServicesTabCheck.setSelected(s.isOpenNewServicesTab());
        activateServicesOutputPaneCheck.setSelected(s.isActivateServicesOutputPane());
        initialSettings = getCurrentSettingsFromUI();
        updating = false;
    }

    public DatabaseOutputResultsSettings getCurrentSettingsFromUI() {
        DatabaseOutputResultsSettings s = new DatabaseOutputResultsSettings();
        s.setShowTimestampForQueryOutput(showTimestampCheck.isSelected());
        s.setEnableDbmsOutput(enableDbmsOutputCheck.isSelected());
        s.setShowResultsInEditor(showResultsInEditorCheck.isSelected());
        s.setCreateTitleFromComment(createTitleFromCommentCheck.isSelected());
        s.setTreatTextAsTitleAfter(treatTextAsTitleAfterField.getText());
        s.setShowServicesToolWindow(showServicesToolWindowCombo.getValue());
        s.setFocusOnServicesWindow(focusOnServicesWindowCheck.isSelected());
        s.setOpenNewServicesTab(openNewServicesTabCheck.isSelected());
        s.setActivateServicesOutputPane(activateServicesOutputPaneCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DatabaseOutputResultsSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public CheckBox getShowTimestampCheck() {
        return showTimestampCheck;
    }

    public CheckBox getEnableDbmsOutputCheck() {
        return enableDbmsOutputCheck;
    }

    public CheckBox getShowResultsInEditorCheck() {
        return showResultsInEditorCheck;
    }

    public CheckBox getCreateTitleFromCommentCheck() {
        return createTitleFromCommentCheck;
    }

    public TextField getTreatTextAsTitleAfterField() {
        return treatTextAsTitleAfterField;
    }

    public ComboBox<String> getShowServicesToolWindowCombo() {
        return showServicesToolWindowCombo;
    }

    public CheckBox getFocusOnServicesWindowCheck() {
        return focusOnServicesWindowCheck;
    }

    public CheckBox getOpenNewServicesTabCheck() {
        return openNewServicesTabCheck;
    }

    public CheckBox getActivateServicesOutputPaneCheck() {
        return activateServicesOutputPaneCheck;
    }
}
