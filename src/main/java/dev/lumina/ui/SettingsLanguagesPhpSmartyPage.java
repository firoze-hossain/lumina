package dev.lumina.ui;

import dev.lumina.php.PhpSettingsManager;
import dev.lumina.php.PhpSmartySettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Settings page for Languages & Frameworks > PHP > Smarty.
 * Faithfully matches Image 2.
 */
public class SettingsLanguagesPhpSmartyPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private TextField leftDelimiterField;
    private TextField rightDelimiterField;
    private CheckBox useSmarty3WhitespacesCheck;

    private PhpSmartySettings initialSettings = new PhpSmartySettings();
    private Runnable onModified;

    public SettingsLanguagesPhpSmartyPage() {
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
        // Row 1: Left delimiter
        HBox leftRow = new HBox(12);
        leftRow.setAlignment(Pos.CENTER_LEFT);

        Label leftLabel = new Label("Smarty left delimiter:");
        leftLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        leftLabel.setPrefWidth(160);

        leftDelimiterField = new TextField("{");
        leftDelimiterField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        leftDelimiterField.setPrefWidth(120);
        leftDelimiterField.setMaxWidth(120);
        leftDelimiterField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        leftRow.getChildren().addAll(leftLabel, leftDelimiterField);

        // Row 2: Right delimiter
        HBox rightRow = new HBox(12);
        rightRow.setAlignment(Pos.CENTER_LEFT);

        Label rightLabel = new Label("Smarty right delimiter:");
        rightLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        rightLabel.setPrefWidth(160);

        rightDelimiterField = new TextField("}");
        rightDelimiterField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        rightDelimiterField.setPrefWidth(120);
        rightDelimiterField.setMaxWidth(120);
        rightDelimiterField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        rightRow.getChildren().addAll(rightLabel, rightDelimiterField);

        // Row 3: Whitespaces policy checkbox
        useSmarty3WhitespacesCheck = new CheckBox("Use Smarty3 whitespaces policy");
        useSmarty3WhitespacesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        useSmarty3WhitespacesCheck.setSelected(true);
        useSmarty3WhitespacesCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        getChildren().addAll(leftRow, rightRow, useSmarty3WhitespacesCheck);
    }

    public void loadFromManager() {
        PhpSmartySettings current = manager.getSmartySettings();
        leftDelimiterField.setText(current.getLeftDelimiter());
        rightDelimiterField.setText(current.getRightDelimiter());
        useSmarty3WhitespacesCheck.setSelected(current.isUseSmarty3WhitespacesPolicy());

        initialSettings = current.copy();
    }

    private PhpSmartySettings buildCurrentSettings() {
        PhpSmartySettings current = new PhpSmartySettings();
        current.setLeftDelimiter(leftDelimiterField.getText());
        current.setRightDelimiter(rightDelimiterField.getText());
        current.setUseSmarty3WhitespacesPolicy(useSmarty3WhitespacesCheck.isSelected());
        return current;
    }

    public boolean isModified() {
        return !buildCurrentSettings().equals(initialSettings);
    }

    public void apply() {
        PhpSmartySettings current = buildCurrentSettings();
        manager.setSmartySettings(current);
        initialSettings = current.copy();
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}
