// SettingsColorSchemeFontPage.java
package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.SchemeFontPreferences;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

/**
 * Editor > Color Scheme > Color Scheme Font settings page.
 * Matches 1:1 with reference screenshot:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - "Use color scheme font instead of the default (JetBrains Mono, 13)" toggle disabling child controls.
 * - Font combo with "Show only monospaced fonts".
 * - Fallback font combo with subtext description.
 * - Size and Line height spinners.
 * - Enable ligatures checkbox.
 * - "See line height and ligatures also in Reader mode" checkbox.
 * - Live interactive typography sample and character set preview with real-time updates.
 */
public class SettingsColorSchemeFontPage extends VBox {

    private final ColorSchemeHeaderBar headerBar;
    private final CheckBox useColorSchemeFontCheck;

    private final VBox fontControlsContainer;
    private final ComboBox<String> fontCombo;
    private final CheckBox showMonospacedCheck;
    private final ComboBox<String> fallbackCombo;
    private final Spinner<Double> sizeSpinner;
    private final Spinner<Double> lineHeightSpinner;
    private final CheckBox enableLigaturesCheck;
    private final CheckBox readerModeCheck;

    private final TextArea previewTextArea;
    private final Label defaultPreviewLabel;
    private final TextField customPreviewInput;

    private SchemeFontPreferences initialPrefs;
    private boolean suppressEvents = false;
    private Runnable onModifiedListener;

    private static final String DEFAULT_PREVIEW_PARAGRAPH =
            "Lumina IDE is an Integrated\n" +
            "Development Environment (IDE) designed\n" +
            "to maximize productivity. It provides\n" +
            "clever code completion, static code\n" +
            "analysis, and refactorings, and lets\n" +
            "you focus on the bright side of\n" +
            "software development making\n" +
            "it an enjoyable experience.";

    private static final String DEFAULT_CHARSET_SAMPLE =
            "abcdefghijklmnopqrstuvwxyz\n" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n" +
            "0123456789 ( ) { } [ ]\n" +
            "+ - * / = . , ; ! ? # & $ % @ ^";

    public SettingsColorSchemeFontPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(12);

        // 1. Header Bar
        headerBar = new ColorSchemeHeaderBar();
        headerBar.setOnSchemeChanged(scheme -> {
            loadPreferences();
            notifyModified();
        });

        // 2. Use color scheme font toggle
        useColorSchemeFontCheck = new CheckBox("Use color scheme font instead of the default (JetBrains Mono, 13)");
        useColorSchemeFontCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        // 3. Font Controls Container
        fontControlsContainer = new VBox(10);
        fontControlsContainer.setPadding(new Insets(4, 0, 4, 16));

        // Font row
        HBox fontRow = new HBox(8);
        fontRow.setAlignment(Pos.CENTER_LEFT);
        Label fontLabel = new Label("Font:");
        fontLabel.setPrefWidth(100);
        fontLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px;");

        fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll("JetBrains Mono", "Consolas", "Menlo", "Monaco", "Courier New", "Fira Code");
        fontCombo.setPrefWidth(200);
        fontCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        showMonospacedCheck = new CheckBox("Show only monospaced fonts");
        showMonospacedCheck.setSelected(true);
        showMonospacedCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        fontRow.getChildren().addAll(fontLabel, fontCombo, showMonospacedCheck);

        // Fallback font row
        HBox fallbackRow = new HBox(8);
        fallbackRow.setAlignment(Pos.CENTER_LEFT);
        Label fallbackLabel = new Label("Fallback font:");
        fallbackLabel.setPrefWidth(100);
        fallbackLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px;");

        fallbackCombo = new ComboBox<>();
        fallbackCombo.getItems().addAll("<None>", "Noto Sans", "Segoe UI", "Arial Unicode MS");
        fallbackCombo.setPrefWidth(200);
        fallbackCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        fallbackRow.getChildren().addAll(fallbackLabel, fallbackCombo);

        Label fallbackHint = new Label("Used for symbols not supported by the main font");
        fallbackHint.setStyle("-fx-text-fill: #868991; -fx-font-size: 11.5px; -fx-padding: 0 0 0 108;");

        // Size & Line height row
        HBox sizeRow = new HBox(12);
        sizeRow.setAlignment(Pos.CENTER_LEFT);

        Label sizeLabel = new Label("Size:");
        sizeLabel.setPrefWidth(100);
        sizeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px;");
        sizeSpinner = new Spinner<>(6.0, 72.0, 13.0, 1.0);
        sizeSpinner.setPrefWidth(80);
        sizeSpinner.setEditable(true);
        sizeSpinner.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4;");

        Label lineHeightLabel = new Label("Line height:");
        lineHeightLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-padding: 0 0 0 16;");
        lineHeightSpinner = new Spinner<>(0.5, 3.0, 1.2, 0.1);
        lineHeightSpinner.setPrefWidth(80);
        lineHeightSpinner.setEditable(true);
        lineHeightSpinner.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4;");

        sizeRow.getChildren().addAll(sizeLabel, sizeSpinner, lineHeightLabel, lineHeightSpinner);

        // Enable ligatures
        enableLigaturesCheck = new CheckBox("Enable ligatures");
        enableLigaturesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");

        // Reader mode checkbox
        readerModeCheck = new CheckBox("See line height and ligatures also in Reader mode");
        readerModeCheck.setSelected(true);
        readerModeCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");

        fontControlsContainer.getChildren().addAll(
                fontRow,
                fallbackRow,
                fallbackHint,
                sizeRow,
                enableLigaturesCheck,
                readerModeCheck
        );

        // Bind disable state of font controls to useColorSchemeFontCheck
        fontControlsContainer.disableProperty().bind(useColorSchemeFontCheck.selectedProperty().not());

        // 4. Live Typography Preview Area
        VBox previewSection = new VBox(8);
        previewSection.setPadding(new Insets(10, 0, 0, 0));

        previewTextArea = new TextArea(DEFAULT_PREVIEW_PARAGRAPH);
        previewTextArea.setEditable(false);
        previewTextArea.setWrapText(true);
        previewTextArea.setPrefHeight(140);
        previewTextArea.setStyle(
                "-fx-background-color: #1F2230; -fx-control-inner-background: #1F2230; " +
                "-fx-border-color: #2C3042; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5;"
        );

        Label defaultLabel = new Label("Default:");
        defaultLabel.setStyle("-fx-text-fill: #868991; -fx-font-size: 12px;");

        defaultPreviewLabel = new Label(DEFAULT_CHARSET_SAMPLE);
        defaultPreviewLabel.setWrapText(true);
        defaultPreviewLabel.setPadding(new Insets(8, 12, 8, 12));
        defaultPreviewLabel.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5;");

        HBox customInputRow = new HBox(8);
        customInputRow.setAlignment(Pos.CENTER_LEFT);
        Label customInputLabel = new Label("Enter any text to preview");
        customInputLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        customPreviewInput = new TextField();
        customPreviewInput.setPromptText("Type here to preview the font");
        customPreviewInput.setPrefWidth(320);
        customPreviewInput.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
        customPreviewInput.textProperty().addListener((o, ov, nv) -> {
            if (nv != null && !nv.isBlank()) {
                previewTextArea.setText(nv);
            } else {
                previewTextArea.setText(DEFAULT_PREVIEW_PARAGRAPH);
            }
        });
        customInputRow.getChildren().addAll(customInputLabel, customPreviewInput);

        previewSection.getChildren().addAll(
                previewTextArea,
                defaultLabel,
                defaultPreviewLabel,
                customInputRow
        );

        getChildren().addAll(
                headerBar,
                useColorSchemeFontCheck,
                fontControlsContainer,
                previewSection
        );

        setupListeners();
        loadPreferences();
    }

    private void setupListeners() {
        useColorSchemeFontCheck.selectedProperty().addListener((o, ov, nv) -> {
            updatePreviewStyles();
            notifyModified();
        });
        fontCombo.valueProperty().addListener((o, ov, nv) -> {
            updatePreviewStyles();
            notifyModified();
        });
        fallbackCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        showMonospacedCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        sizeSpinner.valueProperty().addListener((o, ov, nv) -> {
            updatePreviewStyles();
            notifyModified();
        });
        lineHeightSpinner.valueProperty().addListener((o, ov, nv) -> {
            updatePreviewStyles();
            notifyModified();
        });
        enableLigaturesCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        readerModeCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
    }

    public void loadPreferences() {
        suppressEvents = true;
        try {
            SchemeFontPreferences prefs = EditorColorSchemeSettings.getInstance().getSchemeFontPreferences();
            this.initialPrefs = prefs.clone();

            useColorSchemeFontCheck.setSelected(prefs.isUseSchemeFont());
            fontCombo.setValue(prefs.getFontFamily());
            fallbackCombo.setValue(prefs.getFallbackFont());
            showMonospacedCheck.setSelected(prefs.isShowOnlyMonospaced());
            sizeSpinner.getValueFactory().setValue(prefs.getFontSize());
            lineHeightSpinner.getValueFactory().setValue(prefs.getLineHeight());
            enableLigaturesCheck.setSelected(prefs.isEnableLigatures());

            updatePreviewStyles();
        } finally {
            suppressEvents = false;
        }
    }

    private void updatePreviewStyles() {
        String family = (useColorSchemeFontCheck.isSelected() && fontCombo.getValue() != null) ? fontCombo.getValue() : "JetBrains Mono";
        double size = (useColorSchemeFontCheck.isSelected() && sizeSpinner.getValue() != null) ? sizeSpinner.getValue() : 13.0;

        String style = String.format(
                "-fx-background-color: #1F2230; -fx-control-inner-background: #1F2230; " +
                "-fx-border-color: #2C3042; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-text-fill: #DFE1E5; -fx-font-family: '%s'; -fx-font-size: %.1fpx;",
                family, size
        );
        previewTextArea.setStyle(style);
        defaultPreviewLabel.setStyle(String.format(
                "-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-text-fill: #DFE1E5; -fx-font-family: '%s'; -fx-font-size: %.1fpx;",
                family, size
        ));
    }

    private SchemeFontPreferences buildCurrentPreferences() {
        return new SchemeFontPreferences(
                useColorSchemeFontCheck.isSelected(),
                fontCombo.getValue() != null ? fontCombo.getValue() : "JetBrains Mono",
                sizeSpinner.getValue() != null ? sizeSpinner.getValue() : 13.0,
                lineHeightSpinner.getValue() != null ? lineHeightSpinner.getValue() : 1.2,
                fallbackCombo.getValue() != null ? fallbackCombo.getValue() : "<None>",
                showMonospacedCheck.isSelected(),
                enableLigaturesCheck.isSelected()
        );
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public boolean isModified() {
        if (initialPrefs == null) return false;
        return !initialPrefs.equals(buildCurrentPreferences());
    }

    public void apply() {
        SchemeFontPreferences current = buildCurrentPreferences();
        EditorColorSchemeSettings.getInstance().setSchemeFontPreferences(current);
        EditorColorSchemeSettings.getInstance().save();
        this.initialPrefs = current.clone();
    }

    public void reset() {
        loadPreferences();
    }

    public ColorSchemeHeaderBar getHeaderBar() {
        return headerBar;
    }

    public CheckBox getUseColorSchemeFontCheck() {
        return useColorSchemeFontCheck;
    }

    public ComboBox<String> getFontCombo() {
        return fontCombo;
    }

    public ComboBox<String> getFallbackCombo() {
        return fallbackCombo;
    }

    public Spinner<Double> getSizeSpinner() {
        return sizeSpinner;
    }

    public Spinner<Double> getLineHeightSpinner() {
        return lineHeightSpinner;
    }

    public CheckBox getShowMonospacedCheck() {
        return showMonospacedCheck;
    }

    public CheckBox getEnableLigaturesCheck() {
        return enableLigaturesCheck;
    }

    public CheckBox getReaderModeCheck() {
        return readerModeCheck;
    }

    public TextArea getPreviewTextArea() {
        return previewTextArea;
    }
}