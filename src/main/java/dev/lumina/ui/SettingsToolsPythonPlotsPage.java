package dev.lumina.ui;

import dev.lumina.tools.PythonPlotsSettings;
import dev.lumina.tools.PythonPlotsSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > Python Plots settings page in Lumina IDE.
 * Matches screenshot 3 1:1 with dynamic configuration.
 */
public class SettingsToolsPythonPlotsPage extends VBox {

    private final PythonPlotsSettingsManager manager;
    private PythonPlotsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private CheckBox showPlotsInToolWindowCheck;
    private VBox subOptionsBox;
    private CheckBox useMpld3Check;
    private Spinner<Integer> maxPlotsSpinner;
    private CheckBox suggestKaleidoCheck;
    private CheckBox suggestMpld3Check;
    private CheckBox suggestPillowCheck;

    public SettingsToolsPythonPlotsPage() {
        this.manager = PythonPlotsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        showPlotsInToolWindowCheck = createCheckBox("Show plots in tool window");

        subOptionsBox = new VBox(10);
        subOptionsBox.setPadding(new Insets(4, 0, 0, 22));

        useMpld3Check = createCheckBox("Use 'mpld3' interactive plots for Matplotlib");

        Label maxPlotsLabel = new Label("Max plots count:");
        maxPlotsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        maxPlotsSpinner = new Spinner<>();
        maxPlotsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 200, 10));
        maxPlotsSpinner.setEditable(true);
        maxPlotsSpinner.setPrefWidth(85);
        maxPlotsSpinner.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");

        HBox maxPlotsRow = new HBox(8, maxPlotsLabel, maxPlotsSpinner);
        maxPlotsRow.setAlignment(Pos.CENTER_LEFT);

        suggestKaleidoCheck = createCheckBox("Suggest install 'kaleido' for plotly");
        suggestMpld3Check = createCheckBox("Suggest install 'mpld3' for interactive matplotlib plots");

        subOptionsBox.getChildren().addAll(useMpld3Check, maxPlotsRow, suggestKaleidoCheck, suggestMpld3Check);

        suggestPillowCheck = createCheckBox("Suggest install 'pillow' for debug image viewer");

        getChildren().addAll(showPlotsInToolWindowCheck, subOptionsBox, suggestPillowCheck);

        setupListeners();
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return cb;
    }

    private void setupListeners() {
        showPlotsInToolWindowCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            subOptionsBox.setDisable(!newVal);
            notifyModified();
        });

        useMpld3Check.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        maxPlotsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        suggestKaleidoCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        suggestMpld3Check.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        suggestPillowCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
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

    private void applySettingsToUI(PythonPlotsSettings s) {
        if (s == null) return;
        showPlotsInToolWindowCheck.setSelected(s.isShowPlotsInToolWindow());
        subOptionsBox.setDisable(!s.isShowPlotsInToolWindow());
        useMpld3Check.setSelected(s.isUseMpld3InteractivePlots());
        if (maxPlotsSpinner.getValueFactory() != null) {
            maxPlotsSpinner.getValueFactory().setValue(s.getMaxPlotsCount() > 0 ? s.getMaxPlotsCount() : 200);
        }
        suggestKaleidoCheck.setSelected(s.isSuggestInstallKaleido());
        suggestMpld3Check.setSelected(s.isSuggestInstallMpld3());
        suggestPillowCheck.setSelected(s.isSuggestInstallPillow());
    }

    private PythonPlotsSettings getCurrentSettingsFromUI() {
        PythonPlotsSettings s = new PythonPlotsSettings();
        s.setShowPlotsInToolWindow(showPlotsInToolWindowCheck.isSelected());
        s.setUseMpld3InteractivePlots(useMpld3Check.isSelected());
        if (maxPlotsSpinner.getValue() != null) {
            s.setMaxPlotsCount(maxPlotsSpinner.getValue());
        }
        s.setSuggestInstallKaleido(suggestKaleidoCheck.isSelected());
        s.setSuggestInstallMpld3(suggestMpld3Check.isSelected());
        s.setSuggestInstallPillow(suggestPillowCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        PythonPlotsSettings current = getCurrentSettingsFromUI();
        return !Objects.equals(initialSettings, current);
    }

    public void apply() {
        PythonPlotsSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            updating = true;
            try {
                applySettingsToUI(initialSettings);
            } finally {
                updating = false;
            }
            notifyModified();
        }
    }

    public void reset() {
        revertChanges();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public CheckBox getShowPlotsInToolWindowCheck() {
        return showPlotsInToolWindowCheck;
    }

    public CheckBox getUseMpld3Check() {
        return useMpld3Check;
    }

    public Spinner<Integer> getMaxPlotsSpinner() {
        return maxPlotsSpinner;
    }

    public CheckBox getSuggestKaleidoCheck() {
        return suggestKaleidoCheck;
    }

    public CheckBox getSuggestMpld3Check() {
        return suggestMpld3Check;
    }

    public CheckBox getSuggestPillowCheck() {
        return suggestPillowCheck;
    }
}
