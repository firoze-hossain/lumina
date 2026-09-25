package dev.lumina.ui;

import dev.lumina.settings.StickyLinesSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Editor > General > Sticky Lines settings page.
 * Full dynamic configuration matching the modern IDE design.
 */
public class SettingsStickyLinesPage extends VBox {

    private final CheckBox showStickyLinesCheck = new CheckBox("Show sticky lines while scrolling");
    private final Label maxLabel = new Label("Maximum number of lines:");
    private final Spinner<Integer> maxSpinner = new Spinner<>(1, 50, 5, 1);
    private final Label languagesLabel = new Label("Languages:");
    private final Map<String, CheckBox> languageChecks = new LinkedHashMap<>();
    private final Hyperlink manageColorsLink = new Hyperlink("Manage colors");

    private Runnable onModifiedListener;
    private Runnable onManageColors;
    private boolean suppressEvents = false;

    public SettingsStickyLinesPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(12);

        buildUi();
        setupListeners();
        loadFromSettings(StickyLinesSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnManageColors(Runnable onManageColors) {
        this.onManageColors = onManageColors;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void buildUi() {
        // Master Checkbox
        styleCheckBox(showStickyLinesCheck);

        // Max lines row
        maxLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        maxSpinner.setPrefWidth(55);
        maxSpinner.setEditable(true);
        maxSpinner.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox maxRow = new HBox(8, maxLabel, maxSpinner);
        maxRow.setAlignment(Pos.CENTER_LEFT);

        // Languages Label
        languagesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // 3-Column Language Grid matching Screenshot 4
        GridPane languageGrid = new GridPane();
        languageGrid.setHgap(48);
        languageGrid.setVgap(8);
        languageGrid.setPadding(new Insets(2, 0, 8, 0));

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setPrefWidth(130);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(130);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPrefWidth(130);
        languageGrid.getColumnConstraints().addAll(col0, col1, col2);

        // Column 1 (10 languages)
        List<String> col1List = StickyLinesSettings.COLUMN_1_LANGUAGES;
        for (int r = 0; r < col1List.size(); r++) {
            String lang = col1List.get(r);
            CheckBox cb = new CheckBox(lang);
            styleCheckBox(cb);
            languageChecks.put(lang, cb);
            languageGrid.add(cb, 0, r);
        }

        // Column 2 (10 languages)
        List<String> col2List = StickyLinesSettings.COLUMN_2_LANGUAGES;
        for (int r = 0; r < col2List.size(); r++) {
            String lang = col2List.get(r);
            CheckBox cb = new CheckBox(lang);
            styleCheckBox(cb);
            languageChecks.put(lang, cb);
            languageGrid.add(cb, 1, r);
        }

        // Column 3 (9 languages)
        List<String> col3List = StickyLinesSettings.COLUMN_3_LANGUAGES;
        for (int r = 0; r < col3List.size(); r++) {
            String lang = col3List.get(r);
            CheckBox cb = new CheckBox(lang);
            styleCheckBox(cb);
            languageChecks.put(lang, cb);
            languageGrid.add(cb, 2, r);
        }

        // Manage colors hyperlink
        manageColorsLink.setStyle("-fx-text-fill: #589DF6; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false; -fx-font-size: 12px;");
        manageColorsLink.setOnMouseEntered(e -> manageColorsLink.setStyle("-fx-text-fill: #70B0FF; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: true; -fx-font-size: 12px;"));
        manageColorsLink.setOnMouseExited(e -> manageColorsLink.setStyle("-fx-text-fill: #589DF6; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false; -fx-font-size: 12px;"));
        manageColorsLink.setOnAction(e -> {
            if (onManageColors != null) {
                onManageColors.run();
            }
        });

        // Binding to disable children when master checkbox is unchecked
        maxLabel.disableProperty().bind(showStickyLinesCheck.selectedProperty().not());
        maxSpinner.disableProperty().bind(showStickyLinesCheck.selectedProperty().not());
        languagesLabel.disableProperty().bind(showStickyLinesCheck.selectedProperty().not());
        languageGrid.disableProperty().bind(showStickyLinesCheck.selectedProperty().not());
        manageColorsLink.disableProperty().bind(showStickyLinesCheck.selectedProperty().not());

        getChildren().addAll(
                showStickyLinesCheck,
                maxRow,
                languagesLabel,
                languageGrid,
                manageColorsLink
        );
    }

    private void setupListeners() {
        showStickyLinesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        maxSpinner.valueProperty().addListener((obs, old, val) -> notifyModified());
        for (CheckBox cb : languageChecks.values()) {
            cb.selectedProperty().addListener((obs, old, val) -> notifyModified());
        }
    }

    public void loadFromSettings(StickyLinesSettings settings) {
        suppressEvents = true;
        try {
            showStickyLinesCheck.setSelected(settings.isShowStickyLines());
            maxSpinner.getValueFactory().setValue(settings.getMaxLines());
            for (Map.Entry<String, CheckBox> entry : languageChecks.entrySet()) {
                entry.getValue().setSelected(settings.isLanguageEnabled(entry.getKey()));
            }
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        StickyLinesSettings s = StickyLinesSettings.getInstance();
        if (showStickyLinesCheck.isSelected() != s.isShowStickyLines()) return true;
        if (!Objects.equals(maxSpinner.getValue(), s.getMaxLines())) return true;
        for (Map.Entry<String, CheckBox> entry : languageChecks.entrySet()) {
            if (entry.getValue().isSelected() != s.isLanguageEnabled(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        StickyLinesSettings s = StickyLinesSettings.getInstance();
        s.setShowStickyLines(showStickyLinesCheck.isSelected());
        s.setMaxLines(maxSpinner.getValue());
        for (Map.Entry<String, CheckBox> entry : languageChecks.entrySet()) {
            s.setLanguageEnabled(entry.getKey(), entry.getValue().isSelected());
        }
        s.save();
    }

    public void reset() {
        loadFromSettings(StickyLinesSettings.getInstance());
    }

    // --- Component Getters for Tests ---

    public CheckBox getShowStickyLinesCheck() {
        return showStickyLinesCheck;
    }

    public Spinner<Integer> getMaxSpinner() {
        return maxSpinner;
    }

    public Label getMaxLabel() {
        return maxLabel;
    }

    public Label getLanguagesLabel() {
        return languagesLabel;
    }

    public Map<String, CheckBox> getLanguageChecks() {
        return Collections.unmodifiableMap(languageChecks);
    }

    public Hyperlink getManageColorsLink() {
        return manageColorsLink;
    }
}