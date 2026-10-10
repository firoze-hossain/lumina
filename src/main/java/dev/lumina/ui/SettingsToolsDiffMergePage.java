package dev.lumina.ui;

import dev.lumina.tools.DiffMergeSettings;
import dev.lumina.tools.DiffMergeSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Objects;

/**
 * Tools > Diff & Merge settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsDiffMergePage extends VBox {

    private final DiffMergeSettingsManager manager;
    private DiffMergeSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private Slider contextLinesSlider;
    private CheckBox goToNextFileCheck;
    private ComboBox<String> navHistoryCombo;

    private CheckBox autoApplyNonConflictingCheck;
    private CheckBox autoResolveImportsCheck;
    private CheckBox highlightModifiedGutterCheck;

    public SettingsToolsDiffMergePage() {
        this.manager = DiffMergeSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        // --- Diff Section ---
        Label diffHeader = new Label("Diff");
        diffHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Context lines slider
        Label contextLabel = createFieldLabel("Context lines:");
        contextLabel.setPrefWidth(120);

        contextLinesSlider = new Slider(0, 4, 2);
        contextLinesSlider.setMajorTickUnit(1);
        contextLinesSlider.setMinorTickCount(0);
        contextLinesSlider.setSnapToTicks(true);
        contextLinesSlider.setShowTickMarks(true);
        contextLinesSlider.setShowTickLabels(true);
        contextLinesSlider.setPrefWidth(220);
        contextLinesSlider.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double n) {
                if (n == null) return "";
                int idx = n.intValue();
                return switch (idx) {
                    case 0 -> "1";
                    case 1 -> "2";
                    case 2 -> "4";
                    case 3 -> "8";
                    case 4 -> "Disable";
                    default -> "";
                };
            }

            @Override
            public Double fromString(String s) {
                if (s == null) return 2.0;
                return switch (s) {
                    case "1" -> 0.0;
                    case "2" -> 1.0;
                    case "4" -> 2.0;
                    case "8" -> 3.0;
                    case "Disable" -> 4.0;
                    default -> 2.0;
                };
            }
        });
        contextLinesSlider.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        HBox contextRow = new HBox(12, contextLabel, contextLinesSlider);
        contextRow.setAlignment(Pos.CENTER_LEFT);

        goToNextFileCheck = createCheckBox("Go to the next file after reaching last change");

        Label navLabel = createFieldLabel("Include diffs in navigation history:");
        navLabel.setPrefWidth(220);

        navHistoryCombo = new ComboBox<>();
        navHistoryCombo.getItems().addAll("Until the diff is closed", "Always", "Never");
        navHistoryCombo.setValue("Until the diff is closed");
        navHistoryCombo.setPrefWidth(200);
        styleComboBox(navHistoryCombo);
        navHistoryCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        HBox navRow = new HBox(12, navLabel, navHistoryCombo);
        navRow.setAlignment(Pos.CENTER_LEFT);

        // --- Merge Section ---
        Label mergeHeader = new Label("Merge");
        mergeHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        autoApplyNonConflictingCheck = createCheckBox("Automatically apply non-conflicting changes");
        autoResolveImportsCheck = createCheckBox("Automatically resolve conflicts in import statements");
        highlightModifiedGutterCheck = createCheckBox("Highlight modified lines in gutter");

        getChildren().addAll(
                diffHeader,
                contextRow,
                goToNextFileCheck,
                navRow,
                mergeHeader,
                autoApplyNonConflictingCheck,
                autoResolveImportsCheck,
                highlightModifiedGutterCheck
        );
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return cb;
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private int sliderIndexToContextLines(double idx) {
        int i = (int) Math.round(idx);
        return switch (i) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 8;
            default -> -1;
        };
    }

    private double contextLinesToSliderIndex(int lines) {
        return switch (lines) {
            case 1 -> 0.0;
            case 2 -> 1.0;
            case 4 -> 2.0;
            case 8 -> 3.0;
            default -> 4.0;
        };
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            contextLinesSlider.setValue(contextLinesToSliderIndex(initialSettings.getContextLines()));
            goToNextFileCheck.setSelected(initialSettings.isGoToNextFileAfterLastChange());
            navHistoryCombo.setValue(initialSettings.getNavigationHistoryPolicy());

            autoApplyNonConflictingCheck.setSelected(initialSettings.isAutoApplyNonConflictingChanges());
            autoResolveImportsCheck.setSelected(initialSettings.isAutoResolveConflictsInImports());
            highlightModifiedGutterCheck.setSelected(initialSettings.isHighlightModifiedLinesInGutter());
        } finally {
            updating = false;
        }
    }

    private DiffMergeSettings getCurrentSettingsFromUI() {
        DiffMergeSettings s = new DiffMergeSettings();
        s.setContextLines(sliderIndexToContextLines(contextLinesSlider.getValue()));
        s.setGoToNextFileAfterLastChange(goToNextFileCheck.isSelected());
        s.setNavigationHistoryPolicy(navHistoryCombo.getValue());

        s.setAutoApplyNonConflictingChanges(autoApplyNonConflictingCheck.isSelected());
        s.setAutoResolveConflictsInImports(autoResolveImportsCheck.isSelected());
        s.setHighlightModifiedLinesInGutter(highlightModifiedGutterCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DiffMergeSettings updated = getCurrentSettingsFromUI();
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
    public Slider getContextLinesSlider() { return contextLinesSlider; }
    public CheckBox getGoToNextFileCheck() { return goToNextFileCheck; }
    public ComboBox<String> getNavHistoryCombo() { return navHistoryCombo; }
    public CheckBox getAutoApplyNonConflictingCheck() { return autoApplyNonConflictingCheck; }
    public CheckBox getAutoResolveImportsCheck() { return autoResolveImportsCheck; }
    public CheckBox getHighlightModifiedGutterCheck() { return highlightModifiedGutterCheck; }
}
