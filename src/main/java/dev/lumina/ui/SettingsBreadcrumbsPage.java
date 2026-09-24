package dev.lumina.ui;

import dev.lumina.settings.BreadcrumbsSettings;
import dev.lumina.settings.BreadcrumbsSettings.BreadcrumbsPlacement;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pixel-perfect IntelliJ IDEA-style Editor > General > Breadcrumbs settings page.
 * Completely dynamic, backed by BreadcrumbsSettings, with 3-column language grid,
 * live placement toggling, and dirty tracking.
 */
public class SettingsBreadcrumbsPage extends VBox {

    private final CheckBox showBreadcrumbsCheck = new CheckBox("Show breadcrumbs");
    private final RadioButton topRadio = new RadioButton("Top");
    private final RadioButton bottomRadio = new RadioButton("Bottom");
    private final ToggleGroup placementGroup = new ToggleGroup();

    private final Map<String, CheckBox> languageChecks = new LinkedHashMap<>();
    private final Hyperlink manageColorsLink = new Hyperlink("Manage colors");

    private Runnable onModifiedListener;
    private Runnable onManageColors;
    private boolean suppressEvents = false;

    public SettingsBreadcrumbsPage() {
        this(null);
    }

    public SettingsBreadcrumbsPage(Runnable onManageColors) {
        this.onManageColors = onManageColors;

        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(8);

        buildUi();
        setupListeners();
        loadFromSettings(BreadcrumbsSettings.getInstance());
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
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void buildUi() {
        // Show breadcrumbs checkbox
        styleCheckBox(showBreadcrumbsCheck);

        // Placement row
        Label placementLabel = new Label("Placement:");
        placementLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        topRadio.setToggleGroup(placementGroup);
        bottomRadio.setToggleGroup(placementGroup);
        topRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        bottomRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox placementRow = new HBox(12, placementLabel, topRadio, bottomRadio);
        placementRow.setAlignment(Pos.CENTER_LEFT);
        placementRow.setPadding(new Insets(2, 0, 4, 18));

        // Languages section
        Label languagesLabel = new Label("Languages:");
        languagesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        GridPane languageGrid = new GridPane();
        languageGrid.setHgap(36);
        languageGrid.setVgap(6);
        languageGrid.setPadding(new Insets(2, 0, 8, 18));

        // Exactly 3 columns as in IntelliJ IDEA screenshot:
        // Col 1: items 0..9 (10 items)
        // Col 2: items 10..19 (10 items)
        // Col 3: items 20..28 (9 items)
        int index = 0;
        for (String lang : BreadcrumbsSettings.ALL_LANGUAGES) {
            CheckBox cb = new CheckBox(lang);
            styleCheckBox(cb);
            cb.setMinWidth(110);
            languageChecks.put(lang, cb);

            int col = index < 10 ? 0 : (index < 20 ? 1 : 2);
            int row = index < 10 ? index : (index < 20 ? index - 10 : index - 20);
            languageGrid.add(cb, col, row);
            index++;
        }

        // Manage colors link
        manageColorsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
        manageColorsLink.setOnMouseEntered(e -> manageColorsLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: true;"));
        manageColorsLink.setOnMouseExited(e -> manageColorsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;"));
        manageColorsLink.setOnAction(e -> {
            if (onManageColors != null) onManageColors.run();
        });

        // Toggle enabled state of sub-controls
        showBreadcrumbsCheck.selectedProperty().addListener((obs, o, n) -> {
            topRadio.setDisable(!n);
            bottomRadio.setDisable(!n);
            for (CheckBox cb : languageChecks.values()) {
                cb.setDisable(!n);
            }
        });

        getChildren().addAll(
                showBreadcrumbsCheck,
                placementRow,
                languagesLabel,
                languageGrid,
                manageColorsLink
        );
    }

    private void setupListeners() {
        showBreadcrumbsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        topRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        bottomRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());

        for (CheckBox cb : languageChecks.values()) {
            cb.selectedProperty().addListener((obs, o, n) -> notifyModified());
        }
    }

    public void loadFromSettings(BreadcrumbsSettings s) {
        suppressEvents = true;
        try {
            showBreadcrumbsCheck.setSelected(s.isShowBreadcrumbs());
            if (s.getPlacement() == BreadcrumbsPlacement.TOP) {
                topRadio.setSelected(true);
            } else {
                bottomRadio.setSelected(true);
            }

            boolean enabled = s.isShowBreadcrumbs();
            topRadio.setDisable(!enabled);
            bottomRadio.setDisable(!enabled);

            for (Map.Entry<String, CheckBox> entry : languageChecks.entrySet()) {
                entry.getValue().setSelected(s.isLanguageEnabled(entry.getKey()));
                entry.getValue().setDisable(!enabled);
            }
        } finally {
            suppressEvents = false;
        }
    }

    public void saveToSettings(BreadcrumbsSettings s) {
        s.setShowBreadcrumbs(showBreadcrumbsCheck.isSelected());
        s.setPlacement(topRadio.isSelected() ? BreadcrumbsPlacement.TOP : BreadcrumbsPlacement.BOTTOM);

        for (Map.Entry<String, CheckBox> entry : languageChecks.entrySet()) {
            s.setLanguageEnabled(entry.getKey(), entry.getValue().isSelected());
        }
    }

    public boolean isModified() {
        BreadcrumbsSettings current = new BreadcrumbsSettings();
        saveToSettings(current);
        return current.isModified(BreadcrumbsSettings.getInstance());
    }

    public void apply() {
        saveToSettings(BreadcrumbsSettings.getInstance());
        BreadcrumbsSettings.getInstance().save();
    }

    public void reset() {
        loadFromSettings(BreadcrumbsSettings.getInstance());
    }
}