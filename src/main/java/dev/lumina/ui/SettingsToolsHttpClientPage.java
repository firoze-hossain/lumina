package dev.lumina.ui;

import dev.lumina.tools.HttpClientSettings;
import dev.lumina.tools.HttpClientSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > HTTP Client settings page in Lumina IDE matching Image 5.
 */
public class SettingsToolsHttpClientPage extends VBox {

    private final HttpClientSettingsManager manager;
    private HttpClientSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private TextField customHttpMethodsField;
    private Button expandButton;

    public SettingsToolsHttpClientPage() {
        this.manager = HttpClientSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(6);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // Form Row
        HBox formRow = new HBox(12);
        formRow.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("Custom HTTP methods:");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        label.setPrefWidth(160);

        customHttpMethodsField = new TextField();
        customHttpMethodsField.setPrefWidth(550);
        HBox.setHgrow(customHttpMethodsField, Priority.ALWAYS);
        customHttpMethodsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
        customHttpMethodsField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        expandButton = new Button("⤢");
        expandButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C9099; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-padding: 3 8; -fx-cursor: hand; -fx-font-size: 12px;");
        expandButton.setTooltip(new Tooltip("Edit custom HTTP methods"));
        expandButton.setOnAction(e -> openMultiLineEditor());

        formRow.getChildren().addAll(label, customHttpMethodsField, expandButton);

        // Subtext / hint
        Label hintLabel = new Label("Comma-separated list of custom HTTP methods");
        hintLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px;");
        hintLabel.setPadding(new Insets(0, 0, 0, 172));

        getChildren().addAll(formRow, hintLabel);
    }

    private void openMultiLineEditor() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Custom HTTP Methods");
        dialog.setHeaderText("Enter custom HTTP methods (one per line or comma-separated):");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextArea textArea = new TextArea(customHttpMethodsField.getText());
        textArea.setStyle("-fx-control-inner-background: #1E1F22; -fx-text-fill: #DFE1E5;");
        textArea.setPrefRowCount(6);
        textArea.setPrefColumnCount(30);

        dialog.getDialogPane().setContent(textArea);
        dialog.setResultConverter(btn -> btn == saveType ? textArea.getText().replace("\n", ", ").trim() : null);

        dialog.showAndWait().ifPresent(res -> {
            customHttpMethodsField.setText(res);
            notifyModified();
        });
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(HttpClientSettings s) {
        if (s == null) return;
        customHttpMethodsField.setText(s.getCustomHttpMethods());
    }

    private HttpClientSettings getCurrentSettingsFromUI() {
        return new HttpClientSettings(customHttpMethodsField.getText().trim());
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        HttpClientSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void resetDefaults() {
        applySettingsToUI(new HttpClientSettings());
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Accessors for testing
    public TextField getCustomHttpMethodsField() {
        return customHttpMethodsField;
    }

    public Button getExpandButton() {
        return expandButton;
    }
}
