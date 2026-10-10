package dev.lumina.ui;

import dev.lumina.tools.FeaturesTrainerSettings;
import dev.lumina.tools.FeaturesTrainerSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > Features Trainer settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsFeaturesTrainerPage extends VBox {

    private final FeaturesTrainerSettingsManager manager;
    private FeaturesTrainerSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private ComboBox<String> languageCombo;
    private Button resetLessonsButton;
    private CheckBox showNotificationsCheck;
    private boolean lessonsResetTriggered = false;

    public SettingsToolsFeaturesTrainerPage() {
        this.manager = FeaturesTrainerSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Language Row
        Label langLabel = new Label("Learning main programming language");
        langLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("Java", "Kotlin", "Python", "Go", "Rust", "JavaScript", "TypeScript", "C/C++");
        languageCombo.setValue("Java");
        languageCombo.setPrefWidth(120);
        languageCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        languageCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        HBox langRow = new HBox(12, langLabel, languageCombo);
        langRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Reset Lessons Progress Button
        resetLessonsButton = new Button("Reset Lessons Progress");
        resetLessonsButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12 4 12; -fx-cursor: hand;");
        resetLessonsButton.setOnAction(e -> {
            lessonsResetTriggered = true;
            if (getScene() != null && getScene().getWindow() != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Features Trainer");
                alert.setHeaderText(null);
                alert.setContentText("Lessons progress has been reset.");
                alert.initOwner(getScene().getWindow());
                alert.show();
            }
        });

        // 3. Show notifications on new lessons CheckBox
        showNotificationsCheck = new CheckBox("Show notifications on new lessons");
        showNotificationsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        showNotificationsCheck.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        getChildren().addAll(langRow, resetLessonsButton, showNotificationsCheck);
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            languageCombo.setValue(initialSettings.getMainLanguage());
            showNotificationsCheck.setSelected(initialSettings.isShowNotificationsOnNewLessons());
            lessonsResetTriggered = false;
        } finally {
            updating = false;
        }
    }

    private FeaturesTrainerSettings getCurrentSettingsFromUI() {
        FeaturesTrainerSettings s = new FeaturesTrainerSettings();
        s.setMainLanguage(languageCombo.getValue());
        s.setShowNotificationsOnNewLessons(showNotificationsCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        FeaturesTrainerSettings updated = getCurrentSettingsFromUI();
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

    // Getters for testing
    public ComboBox<String> getLanguageCombo() { return languageCombo; }
    public Button getResetLessonsButton() { return resetLessonsButton; }
    public CheckBox getShowNotificationsCheck() { return showNotificationsCheck; }
    public boolean isLessonsResetTriggered() { return lessonsResetTriggered; }
}
