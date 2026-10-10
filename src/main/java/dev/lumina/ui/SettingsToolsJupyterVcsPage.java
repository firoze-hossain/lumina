package dev.lumina.ui;

import dev.lumina.tools.JupyterVcsSettings;
import dev.lumina.tools.JupyterVcsSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > Jupyter > Jupyter VCS settings page in Lumina IDE.
 * Matches 1:1 with dynamic configuration for clearing notebook outputs before commit.
 */
public class SettingsToolsJupyterVcsPage extends VBox {

    private final JupyterVcsSettingsManager manager;
    private JupyterVcsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private CheckBox clearNotebookOutputsCheck;
    private ToggleGroup clearModeGroup;
    private RadioButton suggestRadio;
    private RadioButton alwaysRadio;
    private Spinner<Integer> sizeSpinner;
    private VBox subOptionsBox;

    public SettingsToolsJupyterVcsPage() {
        this.manager = JupyterVcsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        clearNotebookOutputsCheck = new CheckBox("Clear notebook outputs before commit");
        clearNotebookOutputsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        subOptionsBox = new VBox(10);
        subOptionsBox.setPadding(new Insets(4, 0, 0, 22));

        clearModeGroup = new ToggleGroup();

        suggestRadio = new RadioButton("Suggest clearing outputs when the file size exceeds");
        suggestRadio.setToggleGroup(clearModeGroup);
        suggestRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        sizeSpinner = new Spinner<>();
        sizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 10));
        sizeSpinner.setEditable(true);
        sizeSpinner.setPrefWidth(65);
        sizeSpinner.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label mbLabel = new Label("MB");
        mbLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox suggestRow = new HBox(8, suggestRadio, sizeSpinner, mbLabel);
        suggestRow.setAlignment(Pos.CENTER_LEFT);

        alwaysRadio = new RadioButton("Always clear outputs before commit");
        alwaysRadio.setToggleGroup(clearModeGroup);
        alwaysRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        subOptionsBox.getChildren().addAll(suggestRow, alwaysRadio);

        getChildren().addAll(clearNotebookOutputsCheck, subOptionsBox);

        setupListeners();
    }

    private void setupListeners() {
        clearNotebookOutputsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateEnabledStates();
            notifyModified();
        });

        clearModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            updateEnabledStates();
            notifyModified();
        });

        sizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
    }

    private void updateEnabledStates() {
        boolean enabled = clearNotebookOutputsCheck.isSelected();
        subOptionsBox.setDisable(!enabled);
        if (enabled) {
            sizeSpinner.setDisable(!suggestRadio.isSelected());
        }
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

    private void applySettingsToUI(JupyterVcsSettings s) {
        if (s == null) return;
        clearNotebookOutputsCheck.setSelected(s.isClearNotebookOutputsBeforeCommit());
        if (JupyterVcsSettings.MODE_ALWAYS.equalsIgnoreCase(s.getClearOutputsMode())) {
            alwaysRadio.setSelected(true);
        } else {
            suggestRadio.setSelected(true);
        }
        if (sizeSpinner.getValueFactory() != null) {
            sizeSpinner.getValueFactory().setValue(s.getFileSizeExceedsMb() > 0 ? s.getFileSizeExceedsMb() : 10);
        }
        updateEnabledStates();
    }

    private JupyterVcsSettings getCurrentSettingsFromUI() {
        JupyterVcsSettings s = new JupyterVcsSettings();
        s.setClearNotebookOutputsBeforeCommit(clearNotebookOutputsCheck.isSelected());
        s.setClearOutputsMode(alwaysRadio.isSelected() ? JupyterVcsSettings.MODE_ALWAYS : JupyterVcsSettings.MODE_SUGGEST);
        if (sizeSpinner.getValue() != null) {
            s.setFileSizeExceedsMb(sizeSpinner.getValue());
        }
        if (initialSettings != null) {
            s.setShowRichDiffForNotebooks(initialSettings.isShowRichDiffForNotebooks());
            s.setIgnoreOutputsInDiff(initialSettings.isIgnoreOutputsInDiff());
        }
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        JupyterVcsSettings current = getCurrentSettingsFromUI();
        return !Objects.equals(initialSettings, current);
    }

    public void apply() {
        JupyterVcsSettings current = getCurrentSettingsFromUI();
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

    public CheckBox getClearNotebookOutputsCheck() {
        return clearNotebookOutputsCheck;
    }

    public CheckBox getClearOutputsOnCommitCheck() {
        return clearNotebookOutputsCheck;
    }

    public RadioButton getSuggestRadio() {
        return suggestRadio;
    }

    public RadioButton getAlwaysRadio() {
        return alwaysRadio;
    }

    public Spinner<Integer> getSizeSpinner() {
        return sizeSpinner;
    }
}
