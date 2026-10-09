package dev.lumina.ui;

import dev.lumina.php.PhpDebugSettings;
import dev.lumina.php.PhpSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > PHP > Debug > Xdebug Cloud.
 * Faithfully matches Image 2.
 */
public class SettingsLanguagesPhpDebugXdebugCloudPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private CheckBox connectToXdebugCloudCheck;
    private TextField cloudIdField;

    // Initial state for dirty tracking
    private boolean initialConnect = false;
    private String initialCloudId = "";

    private Runnable onModified;

    public SettingsLanguagesPhpDebugXdebugCloudPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

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
        connectToXdebugCloudCheck = new CheckBox("Connect to Xdebug Cloud");
        connectToXdebugCloudCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        connectToXdebugCloudCheck.selectedProperty().addListener((obs, ov, nv) -> {
            cloudIdField.setDisable(!nv);
            notifyModified();
        });

        HBox cloudIdRow = new HBox(12);
        cloudIdRow.setAlignment(Pos.CENTER_LEFT);

        Label cloudIdLabel = new Label("Cloud ID:");
        cloudIdLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cloudIdLabel.setPrefWidth(70);

        cloudIdField = new TextField();
        cloudIdField.setPromptText("");
        cloudIdField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        cloudIdField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cloudIdField, Priority.ALWAYS);

        cloudIdField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        cloudIdRow.getChildren().addAll(cloudIdLabel, cloudIdField);

        getChildren().addAll(connectToXdebugCloudCheck, cloudIdRow);
    }

    public void loadFromManager() {
        PhpDebugSettings ds = manager.getDebugSettings();
        connectToXdebugCloudCheck.setSelected(ds.isConnectToXdebugCloud());
        cloudIdField.setText(ds.getXdebugCloudId());
        cloudIdField.setDisable(!ds.isConnectToXdebugCloud());

        initialConnect = ds.isConnectToXdebugCloud();
        initialCloudId = ds.getXdebugCloudId() != null ? ds.getXdebugCloudId() : "";
    }

    public boolean isModified() {
        return connectToXdebugCloudCheck.isSelected() != initialConnect ||
                !Objects.equals(cloudIdField.getText().trim(), initialCloudId);
    }

    public void apply() {
        PhpDebugSettings ds = manager.getDebugSettings();
        ds.setConnectToXdebugCloud(connectToXdebugCloudCheck.isSelected());
        ds.setXdebugCloudId(cloudIdField.getText().trim());
        manager.setDebugSettings(ds);

        initialConnect = ds.isConnectToXdebugCloud();
        initialCloudId = ds.getXdebugCloudId() != null ? ds.getXdebugCloudId() : "";
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}
