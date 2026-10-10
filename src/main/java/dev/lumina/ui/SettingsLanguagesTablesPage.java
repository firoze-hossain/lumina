package dev.lumina.ui;

import dev.lumina.tables.TablesSettings;
import dev.lumina.tables.TablesSettingsManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > Tables.
 * Matches reference screenshot media_1791602830119_bbeb89b7.png:
 *  - Default column statistics mode: [ Off v ]
 *  - [x] Automatically display 'Compact' statistics for small tables
 *        Applies to tables with 50 to 600,000 rows
 *  - [x] Display NumPy, PyTorch, TensorFlow as table
 *  - [x] Limit the number of rendered columns
 *        [ 1200 ]
 *        Set the maximum number of columns to display
 *  - Data View ──────────────────────────────
 *  - [ ] Enable local filters by default in Data View
 *        Local filters apply only to rows currently visible in a table
 */
public class SettingsLanguagesTablesPage extends VBox {

    private final TablesSettingsManager manager = TablesSettingsManager.getInstance();

    private final ComboBox<String> statisticsModeCombo = new ComboBox<>();
    private final CheckBox autoCompactCheck = new CheckBox("Automatically display 'Compact' statistics for small tables");
    private final CheckBox displayTensorsCheck = new CheckBox("Display NumPy, PyTorch, TensorFlow as table");
    private final CheckBox limitRenderedColumnsCheck = new CheckBox("Limit the number of rendered columns");
    private final TextField maxColumnsField = new TextField("1200");
    private final CheckBox localFiltersCheck = new CheckBox("Enable local filters by default in Data View");

    private TablesSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesTablesPage() {
        setSpacing(14);
        setPadding(new Insets(16, 24, 24, 24));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Statistics Mode Row ---
        HBox statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        Label statsLabel = new Label("Default column statistics mode:");
        statsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        statisticsModeCombo.setItems(FXCollections.observableArrayList(
                TablesSettings.MODE_OFF,
                TablesSettings.MODE_COMPACT,
                TablesSettings.MODE_DETAILED
        ));
        statisticsModeCombo.setValue(TablesSettings.MODE_OFF);
        statisticsModeCombo.setPrefWidth(120);
        statisticsModeCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;"
        );
        statisticsModeCombo.valueProperty().addListener((obs, o, n) -> notifyModified());

        statsRow.getChildren().addAll(statsLabel, statisticsModeCombo);

        // --- 2. Auto Compact Checkbox + Subtext ---
        VBox autoCompactBox = new VBox(4);
        autoCompactCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        autoCompactCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        Label autoCompactSubtext = new Label("Applies to tables with 50 to 600,000 rows");
        autoCompactSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");
        autoCompactBox.getChildren().addAll(autoCompactCheck, autoCompactSubtext);

        // --- 3. Display NumPy, PyTorch, TensorFlow as table ---
        displayTensorsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        displayTensorsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        // --- 4. Limit rendered columns Checkbox + TextField + Subtext ---
        VBox limitBox = new VBox(6);
        limitRenderedColumnsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        limitRenderedColumnsCheck.selectedProperty().addListener((obs, o, n) -> {
            maxColumnsField.setDisable(!n);
            notifyModified();
        });

        VBox indentedFieldBox = new VBox(4);
        indentedFieldBox.setPadding(new Insets(0, 0, 0, 22));

        maxColumnsField.setPrefWidth(70);
        maxColumnsField.setMaxWidth(70);
        maxColumnsField.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;"
        );
        // Only allow numbers
        maxColumnsField.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.matches("\\d*")) {
                maxColumnsField.setText(n.replaceAll("[^\\d]", ""));
            }
            notifyModified();
        });

        Label maxColumnsSubtext = new Label("Set the maximum number of columns to display");
        maxColumnsSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");
        indentedFieldBox.getChildren().addAll(maxColumnsField, maxColumnsSubtext);

        limitBox.getChildren().addAll(limitRenderedColumnsCheck, indentedFieldBox);

        // --- 5. Data View Header Divider ---
        HBox dataViewHeader = new HBox(8);
        dataViewHeader.setAlignment(Pos.CENTER_LEFT);
        dataViewHeader.setPadding(new Insets(10, 0, 0, 0));

        Label dataViewLabel = new Label("Data View");
        dataViewLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region divider = new Region();
        HBox.setHgrow(divider, Priority.ALWAYS);
        divider.setStyle("-fx-border-color: #393B40 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        dataViewHeader.getChildren().addAll(dataViewLabel, divider);

        // --- 6. Enable local filters Checkbox + Subtext ---
        VBox localFiltersBox = new VBox(4);
        localFiltersCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        localFiltersCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        Label localFiltersSubtext = new Label("Local filters apply only to rows currently visible in a table");
        localFiltersSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");
        localFiltersBox.getChildren().addAll(localFiltersCheck, localFiltersSubtext);

        getChildren().addAll(
                statsRow,
                autoCompactBox,
                displayTensorsCheck,
                limitBox,
                dataViewHeader,
                localFiltersBox
        );
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

    private void applySettingsToUI(TablesSettings s) {
        if (s == null) return;
        statisticsModeCombo.setValue(s.getColumnStatisticsMode());
        autoCompactCheck.setSelected(s.isAutoCompactForSmallTables());
        displayTensorsCheck.setSelected(s.isDisplayTensorsAsTable());
        limitRenderedColumnsCheck.setSelected(s.isLimitRenderedColumns());
        maxColumnsField.setText(String.valueOf(s.getMaxRenderedColumns()));
        maxColumnsField.setDisable(!s.isLimitRenderedColumns());
        localFiltersCheck.setSelected(s.isEnableLocalFiltersByDefault());
    }

    public TablesSettings getCurrentSettingsFromUI() {
        TablesSettings s = new TablesSettings();
        s.setColumnStatisticsMode(statisticsModeCombo.getValue());
        s.setAutoCompactForSmallTables(autoCompactCheck.isSelected());
        s.setDisplayTensorsAsTable(displayTensorsCheck.isSelected());
        s.setLimitRenderedColumns(limitRenderedColumnsCheck.isSelected());
        try {
            s.setMaxRenderedColumns(Integer.parseInt(maxColumnsField.getText().trim()));
        } catch (NumberFormatException e) {
            s.setMaxRenderedColumns(TablesSettings.DEFAULT_MAX_COLUMNS);
        }
        s.setEnableLocalFiltersByDefault(localFiltersCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        TablesSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = new TablesSettings(updated);
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

    // Getters for testing and programmatic inspection
    public ComboBox<String> getStatisticsModeCombo() {
        return statisticsModeCombo;
    }

    public CheckBox getAutoCompactCheck() {
        return autoCompactCheck;
    }

    public CheckBox getDisplayTensorsCheck() {
        return displayTensorsCheck;
    }

    public CheckBox getLimitRenderedColumnsCheck() {
        return limitRenderedColumnsCheck;
    }

    public TextField getMaxColumnsField() {
        return maxColumnsField;
    }

    public CheckBox getLocalFiltersCheck() {
        return localFiltersCheck;
    }
}
