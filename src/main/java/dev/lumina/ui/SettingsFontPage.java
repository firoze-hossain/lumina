package dev.lumina.ui;

import dev.lumina.settings.EditorFontSettings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor > Font settings page.
 * Dynamic persistent implementation with real-time interactive preview matching the modern IDE design.
 */
public class SettingsFontPage extends HBox {

    // Left Column Controls
    private final ComboBox<String> fontCombo = new ComboBox<>();
    private final TextField sizeField = new TextField("13.0");
    private final TextField lineHeightField = new TextField("1.2");
    private final CheckBox enableLigaturesCheck = new CheckBox("Enable ligatures");

    // Typography Settings
    private final ComboBox<String> mainWeightCombo = new ComboBox<>();
    private final ComboBox<String> boldWeightCombo = new ComboBox<>();
    private final ComboBox<String> fallbackFontCombo = new ComboBox<>();

    // Right Column Preview
    private final TextArea previewTextArea = new TextArea();

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    private static final String DEFAULT_PREVIEW_TEXT =
            "Lumina is an Integrated\n" +
            "Development Environment (IDE) designed\n" +
            "to maximize productivity. It provides\n" +
            "clever code completion, static code\n" +
            "analysis, and refactorings, and lets\n" +
            "you focus on the bright side of\n" +
            "software development making\n" +
            "it an enjoyable experience.\n\n" +
            "Default:\n" +
            "abcdefghijklmnopqrstuvwxyz\n" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n" +
            "0123456789 (){}[]\n" +
            "+ - * / = . , ; : !? #&$%@|^\n\n" +
            "Bold:\n" +
            "abcdefghijklmnopqrstuvwxyz\n" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n" +
            "0123456789 (){}[]\n" +
            "+ - * / = . , ; : !? #&$%@|^\n\n" +
            "<!-- != := === >= >- >=> |-> -> <$>\n" +
            "</> #[ |||> |= ~@";

    public SettingsFontPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(24);

        buildUi();
        setupListeners();
        loadFromSettings(EditorFontSettings.getInstance());
        updatePreview();
    }

    private void buildUi() {
        // --- Left column (Settings) ---
        VBox leftCol = new VBox(14);
        leftCol.setPrefWidth(420);
        leftCol.setMinWidth(380);

        // 1. Font Family Row
        Label fontLabel = new Label("Font:");
        fontLabel.setPrefWidth(85);
        fontLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        populateFontFamilies();
        fontCombo.setPrefWidth(260);
        fontCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        HBox fontRow = new HBox(8, fontLabel, fontCombo);
        fontRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Size and Line Height Row
        Label sizeLabel = new Label("Size:");
        sizeLabel.setPrefWidth(85);
        sizeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        sizeField.setPrefWidth(55);
        sizeField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");

        Label lineHeightLabel = new Label("Line height:");
        lineHeightLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 0 0 16;");

        lineHeightField.setPrefWidth(55);
        lineHeightField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");

        HBox sizeLineHeightRow = new HBox(8, sizeLabel, sizeField, lineHeightLabel, lineHeightField);
        sizeLineHeightRow.setAlignment(Pos.CENTER_LEFT);

        // 3. Ligatures CheckBox & Help Icon
        enableLigaturesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Label helpIcon = new Label("?");
        helpIcon.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: #848BA3; -fx-border-radius: 8; -fx-padding: 0 4 0 4; -fx-cursor: hand;");
        Tooltip helpTooltip = new Tooltip("Enable font ligatures (e.g. -> to arrow glyphs) if supported by the font");
        Tooltip.install(helpIcon, helpTooltip);

        HBox ligaturesRow = new HBox(6, enableLigaturesCheck, helpIcon);
        ligaturesRow.setAlignment(Pos.CENTER_LEFT);

        // Reader mode note link
        Label readerPrefix = new Label("See line height and ligatures also in ");
        readerPrefix.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        Hyperlink readerLink = new Hyperlink("Reader mode");
        readerLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");

        HBox readerNoteBox = new HBox(readerPrefix, readerLink);
        readerNoteBox.setAlignment(Pos.CENTER_LEFT);

        // 4. Typography Settings Section (Expandable)
        HBox typographyHeader = new HBox(8);
        typographyHeader.setAlignment(Pos.CENTER_LEFT);
        typographyHeader.setPadding(new Insets(10, 0, 4, 0));

        Label chevron = new Label("\u25BC"); // Down arrow
        chevron.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 10px;");
        Label typographyLabel = new Label("Typography Settings");
        typographyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        Region typographyLine = new Region();
        typographyLine.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(typographyLine, Priority.ALWAYS);

        typographyHeader.getChildren().addAll(chevron, typographyLabel, typographyLine);

        // Main weight
        Label mainWeightLabel = new Label("Main weight:");
        mainWeightLabel.setPrefWidth(100);
        mainWeightLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        mainWeightCombo.getItems().addAll("Thin", "ExtraLight", "Light", "Regular", "Medium", "SemiBold", "Bold", "ExtraBold");
        mainWeightCombo.setValue("Regular");
        mainWeightCombo.setPrefWidth(210);
        mainWeightCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        HBox mainWeightRow = new HBox(8, mainWeightLabel, mainWeightCombo);
        mainWeightRow.setAlignment(Pos.CENTER_LEFT);

        // Bold weight
        Label boldWeightLabel = new Label("Bold weight:");
        boldWeightLabel.setPrefWidth(100);
        boldWeightLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        boldWeightCombo.getItems().addAll("Bold Recommended", "Medium", "SemiBold", "Bold", "ExtraBold");
        boldWeightCombo.setValue("Bold Recommended");
        boldWeightCombo.setPrefWidth(210);
        boldWeightCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        HBox boldWeightRow = new HBox(8, boldWeightLabel, boldWeightCombo);
        boldWeightRow.setAlignment(Pos.CENTER_LEFT);

        Label colorSchemePrefix = new Label("Used for the bold settings in ");
        colorSchemePrefix.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 108;");
        Hyperlink colorSchemeLink = new Hyperlink("color scheme");
        colorSchemeLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
        HBox colorSchemeBox = new HBox(colorSchemePrefix, colorSchemeLink);
        colorSchemeBox.setAlignment(Pos.CENTER_LEFT);

        // Fallback font
        Label fallbackLabel = new Label("Fallback font:");
        fallbackLabel.setPrefWidth(100);
        fallbackLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        fallbackFontCombo.getItems().add("<None>");
        fallbackFontCombo.getItems().addAll(Font.getFamilies());
        fallbackFontCombo.setValue("<None>");
        fallbackFontCombo.setPrefWidth(210);
        fallbackFontCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        HBox fallbackRow = new HBox(8, fallbackLabel, fallbackFontCombo);
        fallbackRow.setAlignment(Pos.CENTER_LEFT);

        Label fallbackHint = new Label("Used for symbols not supported\nby the main font");
        fallbackHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 108;");

        VBox typographyBox = new VBox(8,
                mainWeightRow,
                boldWeightRow,
                colorSchemeBox,
                fallbackRow,
                fallbackHint
        );

        leftCol.getChildren().addAll(
                fontRow,
                sizeLineHeightRow,
                ligaturesRow,
                readerNoteBox,
                typographyHeader,
                typographyBox
        );

        // --- Right column (Live Interactive Preview) ---
        VBox rightCol = new VBox(6);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        previewTextArea.setText(DEFAULT_PREVIEW_TEXT);
        previewTextArea.setWrapText(false);
        previewTextArea.setStyle("-fx-control-inner-background: #18191B; -fx-background-color: #18191B; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(previewTextArea, Priority.ALWAYS);

        Label previewPrompt = new Label("Enter any text to preview");
        previewPrompt.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");

        rightCol.getChildren().addAll(previewTextArea, previewPrompt);

        getChildren().addAll(leftCol, rightCol);
    }

    private void populateFontFamilies() {
        List<String> families = new ArrayList<>();
        List<String> all = Font.getFamilies();

        // Place common monospace coding fonts at top
        String[] preferred = {"JetBrains Mono", "Fira Code", "Source Code Pro", "Consolas", "Monaco", "Menlo", "Ubuntu Mono", "Courier New"};
        for (String p : preferred) {
            if (all.contains(p) && !families.contains(p)) {
                families.add(p);
            }
        }
        for (String f : all) {
            if (!families.contains(f)) {
                families.add(f);
            }
        }
        fontCombo.setItems(FXCollections.observableArrayList(families));
        if (families.contains("JetBrains Mono")) {
            fontCombo.setValue("JetBrains Mono");
        } else if (!families.isEmpty()) {
            fontCombo.setValue(families.get(0));
        }
    }

    private void setupListeners() {
        fontCombo.valueProperty().addListener((obs, old, val) -> {
            updatePreview();
            notifyModified();
        });
        sizeField.textProperty().addListener((obs, old, val) -> {
            updatePreview();
            notifyModified();
        });
        lineHeightField.textProperty().addListener((obs, old, val) -> {
            updatePreview();
            notifyModified();
        });
        enableLigaturesCheck.selectedProperty().addListener((obs, old, val) -> {
            updatePreview();
            notifyModified();
        });
        mainWeightCombo.valueProperty().addListener((obs, old, val) -> {
            updatePreview();
            notifyModified();
        });
        boldWeightCombo.valueProperty().addListener((obs, old, val) -> {
            updatePreview();
            notifyModified();
        });
        fallbackFontCombo.valueProperty().addListener((obs, old, val) -> {
            updatePreview();
            notifyModified();
        });
    }

    private void updatePreview() {
        String family = fontCombo.getValue() != null ? fontCombo.getValue() : "Monospaced";
        double size = parseSafeDouble(sizeField.getText(), 13.0);
        double lineHeight = parseSafeDouble(lineHeightField.getText(), 1.2);
        String weight = mainWeightCombo.getValue() != null ? mainWeightCombo.getValue().toLowerCase() : "regular";

        String cssWeight = "normal";
        if (weight.contains("bold")) cssWeight = "bold";
        else if (weight.contains("light") || weight.contains("thin")) cssWeight = "lighter";

        previewTextArea.setStyle(String.format(
                "-fx-control-inner-background: #18191B; -fx-background-color: #18191B; -fx-text-fill: #DFE1E5; " +
                "-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-font-weight: %s; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;",
                family, size, cssWeight
        ));
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void loadFromSettings(EditorFontSettings s) {
        suppressEvents = true;
        try {
            if (s.getFontFamily() != null) fontCombo.setValue(s.getFontFamily());
            sizeField.setText(String.valueOf(s.getFontSize()));
            lineHeightField.setText(String.valueOf(s.getLineHeight()));
            enableLigaturesCheck.setSelected(s.isEnableLigatures());
            if (s.getMainWeight() != null) mainWeightCombo.setValue(s.getMainWeight());
            if (s.getBoldWeight() != null) boldWeightCombo.setValue(s.getBoldWeight());
            if (s.getFallbackFont() != null) fallbackFontCombo.setValue(s.getFallbackFont());
            updatePreview();
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        EditorFontSettings s = EditorFontSettings.getInstance();
        double size = parseSafeDouble(sizeField.getText(), s.getFontSize());
        double lineHeight = parseSafeDouble(lineHeightField.getText(), s.getLineHeight());

        return !java.util.Objects.equals(fontCombo.getValue(), s.getFontFamily())
                || Double.compare(size, s.getFontSize()) != 0
                || Double.compare(lineHeight, s.getLineHeight()) != 0
                || enableLigaturesCheck.isSelected() != s.isEnableLigatures()
                || !java.util.Objects.equals(mainWeightCombo.getValue(), s.getMainWeight())
                || !java.util.Objects.equals(boldWeightCombo.getValue(), s.getBoldWeight())
                || !java.util.Objects.equals(fallbackFontCombo.getValue(), s.getFallbackFont());
    }

    public void apply() {
        EditorFontSettings s = EditorFontSettings.getInstance();
        if (fontCombo.getValue() != null) s.setFontFamily(fontCombo.getValue());
        s.setFontSize(parseSafeDouble(sizeField.getText(), s.getFontSize()));
        s.setLineHeight(parseSafeDouble(lineHeightField.getText(), s.getLineHeight()));
        s.setEnableLigatures(enableLigaturesCheck.isSelected());
        if (mainWeightCombo.getValue() != null) s.setMainWeight(mainWeightCombo.getValue());
        if (boldWeightCombo.getValue() != null) s.setBoldWeight(boldWeightCombo.getValue());
        if (fallbackFontCombo.getValue() != null) s.setFallbackFont(fallbackFontCombo.getValue());
        s.save();
    }

    public void reset() {
        loadFromSettings(EditorFontSettings.getInstance());
    }

    private double parseSafeDouble(String text, double defaultVal) {
        if (text == null || text.isBlank()) return defaultVal;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    // Component Getters for testing
    public ComboBox<String> getFontCombo() { return fontCombo; }
    public TextField getSizeField() { return sizeField; }
    public TextField getLineHeightField() { return lineHeightField; }
    public CheckBox getEnableLigaturesCheck() { return enableLigaturesCheck; }
    public ComboBox<String> getMainWeightCombo() { return mainWeightCombo; }
    public ComboBox<String> getBoldWeightCombo() { return boldWeightCombo; }
    public ComboBox<String> getFallbackFontCombo() { return fallbackFontCombo; }
    public TextArea getPreviewTextArea() { return previewTextArea; }
}