package dev.lumina.ui;

import dev.lumina.tools.FeaturesSuggesterSettings;
import dev.lumina.tools.FeaturesSuggesterSettingsManager;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Tools > Features Suggester settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsFeaturesSuggesterPage extends VBox {

    private final FeaturesSuggesterSettingsManager manager;
    private FeaturesSuggesterSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private CheckBox masterCheck;
    private final Map<String, CheckBox> actionCheckBoxes = new LinkedHashMap<>();

    public SettingsToolsFeaturesSuggesterPage() {
        this.manager = FeaturesSuggesterSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        Label descLabel = new Label("Configure suggestions for actions. It will suggest the following actions in cases where their application can be effective.");
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 0 4 0;");

        masterCheck = new CheckBox("Show suggestions for the following actions:");
        masterCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        masterCheck.selectedProperty().addListener((obs, o, n) -> {
            boolean en = n != null && n;
            for (CheckBox cb : actionCheckBoxes.values()) {
                cb.setDisable(!en);
            }
            if (!updating) notifyModified();
        });

        VBox actionsBox = new VBox(8);
        actionsBox.setPadding(new Insets(0, 0, 0, 24));

        String[] actions = {
                "Comment with line comments",
                "Introduce variables",
                "Paste from history",
                "Quick Evaluate",
                "Surround with",
                "Unwrap",
                "File structure",
                "Show the completion popup",
                "Choose a lookup item and replace",
                "Run to cursor",
                "Edit a breakpoint",
                "Mute breakpoints"
        };

        for (String action : actions) {
            CheckBox cb = new CheckBox(action);
            cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
            cb.selectedProperty().addListener((obs, o, n) -> {
                if (!updating) notifyModified();
            });
            actionCheckBoxes.put(action, cb);
            actionsBox.getChildren().add(cb);
        }

        getChildren().addAll(descLabel, masterCheck, actionsBox);
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            masterCheck.setSelected(initialSettings.isShowSuggestions());
            Map<String, Boolean> map = initialSettings.getSuggestedActions();
            for (Map.Entry<String, CheckBox> e : actionCheckBoxes.entrySet()) {
                Boolean val = map.get(e.getKey());
                e.getValue().setSelected(val != null && val);
                e.getValue().setDisable(!initialSettings.isShowSuggestions());
            }
        } finally {
            updating = false;
        }
    }

    private FeaturesSuggesterSettings getCurrentSettingsFromUI() {
        FeaturesSuggesterSettings s = new FeaturesSuggesterSettings();
        s.setShowSuggestions(masterCheck.isSelected());

        Map<String, Boolean> map = new LinkedHashMap<>();
        for (Map.Entry<String, CheckBox> e : actionCheckBoxes.entrySet()) {
            map.put(e.getKey(), e.getValue().isSelected());
        }
        s.setSuggestedActions(map);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        FeaturesSuggesterSettings updated = getCurrentSettingsFromUI();
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
    public CheckBox getMasterCheck() { return masterCheck; }
    public Map<String, CheckBox> getActionCheckBoxes() { return actionCheckBoxes; }
}
