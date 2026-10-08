package dev.lumina.ui;

import dev.lumina.build.RmiCompilerSettings;
import dev.lumina.build.RmiCompilerSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Compiler > RMI Compiler.
 * Matches 1:1 with reference screenshot media_1791449893875_0016cee0.png:
 *  - Enable RMI stubs generation
 *  - Generate IIOP stubs
 *  - Generate debugging info
 *  - Generate no warnings
 *  - Additional command line parameters
 */
public class SettingsRmiCompilerPage extends VBox {

    private final RmiCompilerSettingsManager manager = RmiCompilerSettingsManager.getInstance();

    private final CheckBox enableRmiStubsCheck = new CheckBox("Enable RMI stubs generation");
    private final CheckBox generateIiopCheck = new CheckBox("Generate IIOP stubs");
    private final CheckBox generateDebugCheck = new CheckBox("Generate debugging info");
    private final CheckBox generateNoWarningsCheck = new CheckBox("Generate no warnings");

    private final Label additionalParamsLabel = new Label("Additional command line parameters:");
    private final TextField additionalParamsField = new TextField();
    private final Button expandParamsBtn = new Button("⤢");

    private final VBox indentedContainer = new VBox(10);

    private RmiCompilerSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsRmiCompilerPage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        styleCheckBox(enableRmiStubsCheck);
        enableRmiStubsCheck.setOnAction(e -> {
            updateChildrenEnabledState();
            fireModified();
        });

        styleCheckBox(generateIiopCheck);
        styleCheckBox(generateDebugCheck);
        styleCheckBox(generateNoWarningsCheck);

        generateIiopCheck.setOnAction(e -> fireModified());
        generateDebugCheck.setOnAction(e -> fireModified());
        generateNoWarningsCheck.setOnAction(e -> fireModified());

        additionalParamsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        styleTextField(additionalParamsField);
        HBox.setHgrow(additionalParamsField, Priority.ALWAYS);
        additionalParamsField.textProperty().addListener((obs, o, n) -> fireModified());

        styleMiniButton(expandParamsBtn);
        expandParamsBtn.setOnAction(e -> openExpandDialog("Additional command line parameters", additionalParamsField));

        HBox paramsRow = new HBox(12, additionalParamsLabel, additionalParamsField, expandParamsBtn);
        paramsRow.setAlignment(Pos.CENTER_LEFT);

        indentedContainer.setPadding(new Insets(2, 0, 0, 16));
        indentedContainer.getChildren().addAll(
                generateIiopCheck,
                generateDebugCheck,
                generateNoWarningsCheck,
                paramsRow
        );

        getChildren().addAll(enableRmiStubsCheck, indentedContainer);
    }

    private void updateChildrenEnabledState() {
        boolean enabled = enableRmiStubsCheck.isSelected();
        indentedContainer.setDisable(!enabled);
    }

    private void styleTextField(TextField f) {
        f.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleMiniButton(Button b) {
        b.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
    }

    private void openExpandDialog(String title, TextField targetField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");

        TextArea area = new TextArea(targetField.getText());
        area.setWrapText(true);
        area.setPrefSize(420, 200);
        area.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-family: monospace; -fx-border-color: #43454A;");

        VBox box = new VBox(8, area);
        box.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(box);

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == okType) return area.getText();
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            targetField.setText(res);
            fireModified();
        });
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        enableRmiStubsCheck.setSelected(initialSettings.isEnableRmiStubsGeneration());
        generateIiopCheck.setSelected(initialSettings.isGenerateIiopStubs());
        generateDebugCheck.setSelected(initialSettings.isGenerateDebuggingInfo());
        generateNoWarningsCheck.setSelected(initialSettings.isGenerateNoWarnings());
        additionalParamsField.setText(initialSettings.getAdditionalCommandLineParameters());

        updateChildrenEnabledState();
        updating = false;
    }

    public RmiCompilerSettings getCurrentSettings() {
        RmiCompilerSettings s = new RmiCompilerSettings();
        s.setEnableRmiStubsGeneration(enableRmiStubsCheck.isSelected());
        s.setGenerateIiopStubs(generateIiopCheck.isSelected());
        s.setGenerateDebuggingInfo(generateDebugCheck.isSelected());
        s.setGenerateNoWarnings(generateNoWarningsCheck.isSelected());
        s.setAdditionalCommandLineParameters(additionalParamsField.getText());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        RmiCompilerSettings current = getCurrentSettings();
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

    public CheckBox getEnableRmiStubsCheck() { return enableRmiStubsCheck; }
    public CheckBox getGenerateIiopCheck() { return generateIiopCheck; }
    public CheckBox getGenerateDebugCheck() { return generateDebugCheck; }
    public CheckBox getGenerateNoWarningsCheck() { return generateNoWarningsCheck; }
    public TextField getAdditionalParamsField() { return additionalParamsField; }
}
