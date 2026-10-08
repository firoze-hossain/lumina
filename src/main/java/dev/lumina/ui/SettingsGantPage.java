package dev.lumina.ui;

import dev.lumina.build.GantSettings;
import dev.lumina.build.GantSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > Gant.
 * Matches 1:1 with reference screenshot media_1791428069810.png:
 *  - Gant home: text field with folder chooser button
 */
public class SettingsGantPage extends VBox {

    private final GantSettingsManager manager = GantSettingsManager.getInstance();

    private final TextField gantHomeField = new TextField();
    private final Button browseGantHomeBtn = new Button("📁");

    private GantSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsGantPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        Label gantHomeLabel = new Label("Gant home:");
        gantHomeLabel.setMinWidth(90);
        gantHomeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        gantHomeField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(gantHomeField, Priority.ALWAYS);

        browseGantHomeBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10; -fx-cursor: hand; -fx-font-size: 12px;");
        browseGantHomeBtn.setOnAction(e -> browseGantHome());

        HBox row = new HBox(12, gantHomeLabel, gantHomeField, browseGantHomeBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(row);

        gantHomeField.textProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void browseGantHome() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Gant Home Directory");
        String cur = gantHomeField.getText();
        if (cur != null && !cur.isBlank()) {
            File curDir = new File(cur.trim());
            if (curDir.exists() && curDir.isDirectory()) {
                chooser.setInitialDirectory(curDir);
            }
        }
        File selected = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            gantHomeField.setText(selected.getAbsolutePath());
        }
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        gantHomeField.setText(initialSettings.getGantHome());

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        GantSettings cur = getCurrentSettings();
        return !initialSettings.equals(cur);
    }

    public void apply() {
        GantSettings cur = getCurrentSettings();
        manager.setSettings(cur);
        initialSettings = cur.clone();
    }

    public void reset() {
        loadData();
    }

    public GantSettings getCurrentSettings() {
        GantSettings s = new GantSettings();
        s.setGantHome(gantHomeField.getText());
        return s;
    }

    public TextField getGantHomeField() {
        return gantHomeField;
    }

    public Button getBrowseGantHomeBtn() {
        return browseGantHomeBtn;
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
