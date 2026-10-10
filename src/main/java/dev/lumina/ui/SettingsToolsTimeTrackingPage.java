package dev.lumina.ui;

import dev.lumina.tools.TimeTrackingSettings;
import dev.lumina.tools.TimeTrackingSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Settings UI page for Tools > Tasks > Time Tracking in Lumina IDE.
 * 1:1 visual match to the IDE specification:
 * - Enable Time Tracking checkbox
 * - Time Tracking settings section
 * - Suspend delay [600] seconds
 */
public class SettingsToolsTimeTrackingPage extends VBox {

    private final CheckBox enableTimeTrackingCheck;
    private final TextField suspendDelayField;
    private final Label suspendDelayLabel;
    private final Label secondsLabel;

    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsToolsTimeTrackingPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Row 1: Enable Time Tracking
        enableTimeTrackingCheck = new CheckBox("Enable Time Tracking");
        enableTimeTrackingCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Section: Time Tracking settings
        HBox sectionHeader = createSectionHeader("Time Tracking settings");

        // Row 2: Suspend delay: [600] seconds (indented)
        suspendDelayLabel = new Label("Suspend delay:");
        suspendDelayLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        suspendDelayField = new TextField("600");
        suspendDelayField.setPrefWidth(60);
        suspendDelayField.setMaxWidth(70);
        suspendDelayField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");

        secondsLabel = new Label("seconds");
        secondsLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        HBox suspendDelayRow = new HBox(8, suspendDelayLabel, suspendDelayField, secondsLabel);
        suspendDelayRow.setAlignment(Pos.CENTER_LEFT);
        suspendDelayRow.setPadding(new Insets(0, 0, 0, 16));

        // Disable dependent controls when Enable Time Tracking is unchecked
        suspendDelayLabel.disableProperty().bind(enableTimeTrackingCheck.selectedProperty().not());
        suspendDelayField.disableProperty().bind(enableTimeTrackingCheck.selectedProperty().not());
        secondsLabel.disableProperty().bind(enableTimeTrackingCheck.selectedProperty().not());

        getChildren().addAll(
                enableTimeTrackingCheck,
                sectionHeader,
                suspendDelayRow
        );

        setupListeners();
        loadSettings();
    }

    private HBox createSectionHeader(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox box = new HBox(12, titleLabel, line);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 0, 4, 0));
        return box;
    }

    private void setupListeners() {
        enableTimeTrackingCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        suspendDelayField.textProperty().addListener((o, ov, nv) -> notifyModified());
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            TimeTrackingSettings s = TimeTrackingSettingsManager.getInstance().getSettings();
            enableTimeTrackingCheck.setSelected(s.isEnableTimeTracking());
            suspendDelayField.setText(String.valueOf(s.getSuspendDelaySeconds()));
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        TimeTrackingSettings saved = TimeTrackingSettingsManager.getInstance().getSettings();
        if (enableTimeTrackingCheck.isSelected() != saved.isEnableTimeTracking()) return true;

        int currentDelay = parseDelay();
        return currentDelay != saved.getSuspendDelaySeconds();
    }

    public void apply() {
        TimeTrackingSettings s = new TimeTrackingSettings();
        s.setEnableTimeTracking(enableTimeTrackingCheck.isSelected());
        s.setSuspendDelaySeconds(parseDelay());

        TimeTrackingSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    private int parseDelay() {
        try {
            int val = Integer.parseInt(suspendDelayField.getText().trim());
            return val > 0 ? val : 600;
        } catch (NumberFormatException e) {
            return 600;
        }
    }

    public CheckBox getEnableTimeTrackingCheck() {
        return enableTimeTrackingCheck;
    }

    public TextField getSuspendDelayField() {
        return suspendDelayField;
    }
}
