package dev.lumina.ui;

import dev.lumina.settings.CodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings.CodeStyleScheme;
import dev.lumina.settings.CodeStyleSettings.LineSeparator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Editor > Code Style main settings page.
 * Matches 1:1 with reference screenshots:
 * - media_1790561994663.png
 * - media_1790562004310.png
 * - media_1790562014615.png
 *
 * Features:
 * - Scheme selector with grouped categories (Stored in Project / Stored in IDE) and actions gear menu.
 * - Indents Detection override warning banner with toggle link.
 * - Segmented tab headers: [General] and [Formatter].
 * - General tab:
 *   - Line separator combo: System-Dependent, Unix/macOS, Windows, Classic Mac OS.
 *   - Hard wrap at: 120 columns + Wrap on typing.
 *   - Visual guides: Optional columns.
 *   - Detect and use existing file indents for editing.
 *   - Enable EditorConfig support.
 * - Formatter tab:
 *   - Do not format patterns.
 *   - Formatter markers in comments: @formatter:off / @formatter:on.
 */
public class SettingsCodeStylePage extends VBox {

    private final CodeStyleHeaderBar headerBar;

    // Tabs
    private final ToggleGroup tabGroup = new ToggleGroup();
    private final ToggleButton generalTab = new ToggleButton("General");
    private final ToggleButton formatterTab = new ToggleButton("Formatter");
    private final StackPane tabContentPane = new StackPane();

    // General Tab Controls
    private final ComboBox<LineSeparator> lineSeparatorCombo = new ComboBox<>();
    private final TextField hardWrapField = new TextField("120");
    private final CheckBox wrapOnTypingCheck = new CheckBox("Wrap on typing");
    private final TextField visualGuidesField = new TextField();
    private final CheckBox detectIndentsCheck = new CheckBox("Detect and use existing file indents for editing");
    private final CheckBox enableEditorConfigCheck = new CheckBox("Enable EditorConfig support");

    // Formatter Tab Controls
    private final TextField doNotFormatField = new TextField();
    private final CheckBox enableMarkersCheck = new CheckBox("Turn formatter on/off with markers in code comments");
    private final TextField offMarkerField = new TextField("@formatter:off");
    private final TextField onMarkerField = new TextField("@formatter:on");
    private final CheckBox enableRegexInMarkersCheck = new CheckBox("Enable regular expressions in formatter markers");

    // Containers
    private final VBox generalPane = new VBox(16);
    private final VBox formatterPane = new VBox(14);

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    // Snapshot for isModified
    private LineSeparator originalLineSeparator;
    private int originalHardWrap;
    private boolean originalWrapOnTyping;
    private String originalVisualGuides;
    private boolean originalDetectIndents;
    private boolean originalEnableEditorConfig;
    private boolean originalEnableMarkers;
    private String originalOffMarker;
    private String originalOnMarker;
    private boolean originalEnableRegexInMarkers;
    private String originalDoNotFormat;

    public SettingsCodeStylePage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(14);

        headerBar = new CodeStyleHeaderBar();
        headerBar.setOnSchemeChanged(scheme -> {
            loadFromScheme(scheme);
            notifyModified();
        });
        headerBar.setOnSettingsModified(this::notifyModified);

        buildUi();
        setupListeners();
        loadFromScheme(CodeStyleSettings.getInstance().getActiveScheme());
    }

    private void buildUi() {
        getChildren().add(headerBar);

        // --- Tab Bar ---
        HBox tabBar = new HBox(4);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(2, 0, 8, 0));

        generalTab.setToggleGroup(tabGroup);
        generalTab.setSelected(true);
        formatterTab.setToggleGroup(tabGroup);

        styleTabButton(generalTab);
        styleTabButton(formatterTab);

        tabBar.getChildren().addAll(generalTab, formatterTab);
        getChildren().add(tabBar);

        // --- Build General Tab ---
        generalPane.setSpacing(16);

        // 1. Line separator row
        VBox lineSepBox = new VBox(4);
        HBox lineSepRow = new HBox(12);
        lineSepRow.setAlignment(Pos.CENTER_LEFT);

        Label lineSepLabel = new Label("Line separator:");
        lineSepLabel.setPrefWidth(100);
        lineSepLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        lineSeparatorCombo.getItems().addAll(LineSeparator.values());
        lineSeparatorCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LineSeparator item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplay());
                }
                setStyle("-fx-text-fill: #DFE1E5; -fx-background-color: #2B2D30;");
            }
        });
        lineSeparatorCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LineSeparator item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getDisplay());
                setStyle("-fx-text-fill: #DFE1E5;");
            }
        });
        lineSeparatorCombo.setPrefWidth(220);
        lineSeparatorCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        lineSepRow.getChildren().addAll(lineSepLabel, lineSeparatorCombo);

        Label lineSepCaption = new Label("Applied to new files");
        lineSepCaption.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 112;");

        lineSepBox.getChildren().addAll(lineSepRow, lineSepCaption);
        generalPane.getChildren().add(lineSepBox);

        // 2. Hard wrap at row
        HBox hardWrapRow = new HBox(12);
        hardWrapRow.setAlignment(Pos.CENTER_LEFT);

        Label hardWrapLabel = new Label("Hard wrap at:");
        hardWrapLabel.setPrefWidth(100);
        hardWrapLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        hardWrapField.setPrefWidth(70);
        hardWrapField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 6 4 6; -fx-font-size: 12px;");

        Label colsLabel = new Label("columns");
        colsLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        styleCheckBox(wrapOnTypingCheck);
        wrapOnTypingCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 0 0 16;");

        hardWrapRow.getChildren().addAll(hardWrapLabel, hardWrapField, colsLabel, wrapOnTypingCheck);
        generalPane.getChildren().add(hardWrapRow);

        // 3. Visual guides row
        VBox visualGuidesBox = new VBox(4);
        HBox visualGuidesRow = new HBox(12);
        visualGuidesRow.setAlignment(Pos.CENTER_LEFT);

        Label visualGuidesLabel = new Label("Visual guides:");
        visualGuidesLabel.setPrefWidth(100);
        visualGuidesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        visualGuidesField.setPromptText("Optional");
        visualGuidesField.setPrefWidth(180);
        visualGuidesField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 6 4 6; -fx-font-size: 12px;");

        Label visualColsLabel = new Label("columns");
        visualColsLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        visualGuidesRow.getChildren().addAll(visualGuidesLabel, visualGuidesField, visualColsLabel);

        Label visualGuidesCaption = new Label("Specify one guide (80) or several (80, 120)");
        visualGuidesCaption.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 112;");

        visualGuidesBox.getChildren().addAll(visualGuidesRow, visualGuidesCaption);
        generalPane.getChildren().add(visualGuidesBox);

        // Spacer
        Region spacer1 = new Region();
        spacer1.setPrefHeight(6);
        generalPane.getChildren().add(spacer1);

        // 4. Checkboxes
        styleCheckBox(detectIndentsCheck);
        generalPane.getChildren().add(detectIndentsCheck);

        VBox editorConfigBox = new VBox(4);
        styleCheckBox(enableEditorConfigCheck);

        Label editorConfigCaption = new Label("EditorConfig may override the IDE code style settings");
        editorConfigCaption.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 24;");

        editorConfigBox.getChildren().addAll(enableEditorConfigCheck, editorConfigCaption);
        generalPane.getChildren().add(editorConfigBox);

        // --- Build Formatter Tab ---
        formatterPane.setSpacing(14);

        // Do not format: [ textfield                               ⤢ ]
        VBox doNotFormatBox = new VBox(4);
        HBox doNotFormatRow = new HBox(12);
        doNotFormatRow.setAlignment(Pos.CENTER_LEFT);

        Label doNotFormatLabel = new Label("Do not format:");
        doNotFormatLabel.setPrefWidth(90);
        doNotFormatLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox fieldContainer = new HBox(0);
        fieldContainer.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(fieldContainer, Priority.ALWAYS);
        fieldContainer.setMaxWidth(600);
        fieldContainer.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");

        doNotFormatField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-padding: 5 8; -fx-font-size: 12px;");
        HBox.setHgrow(doNotFormatField, Priority.ALWAYS);

        Button expandBtn = new Button("⤢");
        expandBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 4 8;");
        expandBtn.setOnAction(e -> showDoNotFormatDialog());

        fieldContainer.getChildren().addAll(doNotFormatField, expandBtn);
        doNotFormatRow.getChildren().addAll(doNotFormatLabel, fieldContainer);

        HBox globCaptionBox = new HBox(4);
        globCaptionBox.setPadding(new Insets(0, 0, 0, 102));
        Label globText = new Label("Specify file name patterns and directories with a ");
        globText.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        Hyperlink globLink = new Hyperlink("glob pattern ↗");
        globLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
        globLink.setOnMouseEntered(e -> globLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 11px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: true;"));
        globLink.setOnMouseExited(e -> globLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;"));
        globCaptionBox.getChildren().addAll(globText, globLink);

        doNotFormatBox.getChildren().addAll(doNotFormatRow, globCaptionBox);
        formatterPane.getChildren().add(doNotFormatBox);

        // Turn formatter on/off with markers in code comments
        VBox markersBox = new VBox(8);
        styleCheckBox(enableMarkersCheck);

        VBox markersDetailsBox = new VBox(8);
        markersDetailsBox.setPadding(new Insets(0, 0, 0, 24));

        HBox offBox = new HBox(8);
        offBox.setAlignment(Pos.CENTER_LEFT);
        Label offLbl = new Label("Off:");
        offLbl.setPrefWidth(28);
        offLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        offMarkerField.setPrefWidth(170);
        offMarkerField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 6; -fx-font-size: 12px;");
        offBox.getChildren().addAll(offLbl, offMarkerField);

        HBox onBox = new HBox(8);
        onBox.setAlignment(Pos.CENTER_LEFT);
        Label onLbl = new Label("On:");
        onLbl.setPrefWidth(28);
        onLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        onMarkerField.setPrefWidth(170);
        onMarkerField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 6; -fx-font-size: 12px;");
        onBox.getChildren().addAll(onLbl, onMarkerField);

        styleCheckBox(enableRegexInMarkersCheck);

        markersDetailsBox.getChildren().addAll(offBox, onBox, enableRegexInMarkersCheck);
        markersDetailsBox.disableProperty().bind(enableMarkersCheck.selectedProperty().not());

        markersBox.getChildren().addAll(enableMarkersCheck, markersDetailsBox);
        formatterPane.getChildren().add(markersBox);

        // Container
        tabContentPane.getChildren().add(generalPane);
        getChildren().add(tabContentPane);
    }

    private void styleTabButton(ToggleButton btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 13px; -fx-padding: 4 14 4 14; -fx-cursor: hand; -fx-border-color: transparent;");
        btn.selectedProperty().addListener((obs, old, isSelected) -> {
            if (isSelected) {
                btn.setStyle("-fx-background-color: #35538F; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 4 14 4 14; -fx-background-radius: 4;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 13px; -fx-padding: 4 14 4 14; -fx-cursor: hand; -fx-border-color: transparent;");
            }
        });
        if (btn.isSelected()) {
            btn.setStyle("-fx-background-color: #35538F; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 4 14 4 14; -fx-background-radius: 4;");
        }
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void setupListeners() {
        generalTab.setOnAction(e -> {
            tabContentPane.getChildren().setAll(generalPane);
        });

        formatterTab.setOnAction(e -> {
            tabContentPane.getChildren().setAll(formatterPane);
        });

        lineSeparatorCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        hardWrapField.textProperty().addListener((obs, o, n) -> notifyModified());
        wrapOnTypingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        visualGuidesField.textProperty().addListener((obs, o, n) -> notifyModified());
        detectIndentsCheck.selectedProperty().addListener((obs, o, n) -> {
            headerBar.updateWarningBanner();
            notifyModified();
        });
        enableEditorConfigCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        doNotFormatField.textProperty().addListener((obs, o, n) -> notifyModified());
        enableMarkersCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        offMarkerField.textProperty().addListener((obs, o, n) -> notifyModified());
        onMarkerField.textProperty().addListener((obs, o, n) -> notifyModified());
        enableRegexInMarkersCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void showDoNotFormatDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Do Not Format");
        dialog.setHeaderText("Specify file name patterns and directories with a glob pattern (one per line):");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #1E1F22;");
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea editArea = new TextArea(doNotFormatField.getText().replace(", ", "\n").replace(",", "\n"));
        editArea.setPrefRowCount(8);
        editArea.setPrefColumnCount(30);
        editArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-family: monospace;");
        pane.setContent(editArea);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String[] lines = editArea.getText().split("\n");
                StringBuilder sb = new StringBuilder();
                for (String l : lines) {
                    String trim = l.trim();
                    if (!trim.isEmpty()) {
                        if (!sb.isEmpty()) sb.append(", ");
                        sb.append(trim);
                    }
                }
                return sb.toString();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            doNotFormatField.setText(res);
            notifyModified();
        });
    }

    public void loadFromScheme(CodeStyleScheme scheme) {
        if (scheme == null) return;
        suppressEvents = true;
        try {
            lineSeparatorCombo.setValue(scheme.getLineSeparator());
            hardWrapField.setText(String.valueOf(scheme.getHardWrapAt()));
            wrapOnTypingCheck.setSelected(scheme.isWrapOnTyping());
            visualGuidesField.setText(scheme.getVisualGuides());
            detectIndentsCheck.setSelected(scheme.isDetectAndUseExistingFileIndents());
            enableEditorConfigCheck.setSelected(scheme.isEnableEditorConfigSupport());

            doNotFormatField.setText(scheme.getDoNotFormatPatterns());
            enableMarkersCheck.setSelected(scheme.isEnableFormatterMarkers());
            offMarkerField.setText(scheme.getFormatterOffMarker());
            onMarkerField.setText(scheme.getFormatterOnMarker());
            enableRegexInMarkersCheck.setSelected(scheme.isEnableRegexInFormatterMarkers());

            // Save snapshot
            originalLineSeparator = scheme.getLineSeparator();
            originalHardWrap = scheme.getHardWrapAt();
            originalWrapOnTyping = scheme.isWrapOnTyping();
            originalVisualGuides = scheme.getVisualGuides();
            originalDetectIndents = scheme.isDetectAndUseExistingFileIndents();
            originalEnableEditorConfig = scheme.isEnableEditorConfigSupport();
            originalEnableMarkers = scheme.isEnableFormatterMarkers();
            originalOffMarker = scheme.getFormatterOffMarker();
            originalOnMarker = scheme.getFormatterOnMarker();
            originalEnableRegexInMarkers = scheme.isEnableRegexInFormatterMarkers();
            originalDoNotFormat = scheme.getDoNotFormatPatterns();

            headerBar.updateWarningBanner();
        } finally {
            suppressEvents = false;
        }
    }

    public void apply() {
        CodeStyleScheme scheme = CodeStyleSettings.getInstance().getActiveScheme();
        if (scheme == null) return;

        scheme.setLineSeparator(lineSeparatorCombo.getValue());
        try {
            scheme.setHardWrapAt(Integer.parseInt(hardWrapField.getText().trim()));
        } catch (Exception ignored) {}
        scheme.setWrapOnTyping(wrapOnTypingCheck.isSelected());
        scheme.setVisualGuides(visualGuidesField.getText().trim());
        scheme.setDetectAndUseExistingFileIndents(detectIndentsCheck.isSelected());
        scheme.setEnableEditorConfigSupport(enableEditorConfigCheck.isSelected());

        scheme.setDoNotFormatPatterns(doNotFormatField.getText().trim());
        scheme.setEnableFormatterMarkers(enableMarkersCheck.isSelected());
        scheme.setFormatterOffMarker(offMarkerField.getText().trim());
        scheme.setFormatterOnMarker(onMarkerField.getText().trim());
        scheme.setEnableRegexInFormatterMarkers(enableRegexInMarkersCheck.isSelected());

        CodeStyleSettings.getInstance().saveSettings();

        // Update baseline
        originalLineSeparator = scheme.getLineSeparator();
        originalHardWrap = scheme.getHardWrapAt();
        originalWrapOnTyping = scheme.isWrapOnTyping();
        originalVisualGuides = scheme.getVisualGuides();
        originalDetectIndents = scheme.isDetectAndUseExistingFileIndents();
        originalEnableEditorConfig = scheme.isEnableEditorConfigSupport();
        originalEnableMarkers = scheme.isEnableFormatterMarkers();
        originalOffMarker = scheme.getFormatterOffMarker();
        originalOnMarker = scheme.getFormatterOnMarker();
        originalEnableRegexInMarkers = scheme.isEnableRegexInFormatterMarkers();
        originalDoNotFormat = scheme.getDoNotFormatPatterns();

        headerBar.updateWarningBanner();
    }

    public void reset() {
        loadFromScheme(CodeStyleSettings.getInstance().getActiveScheme());
    }

    public boolean isModified() {
        if (lineSeparatorCombo.getValue() != originalLineSeparator) return true;
        try {
            if (Integer.parseInt(hardWrapField.getText().trim()) != originalHardWrap) return true;
        } catch (Exception e) {
            return true;
        }
        if (wrapOnTypingCheck.isSelected() != originalWrapOnTyping) return true;
        if (!Objects.equals(visualGuidesField.getText().trim(), originalVisualGuides)) return true;
        if (detectIndentsCheck.isSelected() != originalDetectIndents) return true;
        if (enableEditorConfigCheck.isSelected() != originalEnableEditorConfig) return true;

        if (enableMarkersCheck.isSelected() != originalEnableMarkers) return true;
        if (!Objects.equals(offMarkerField.getText().trim(), originalOffMarker)) return true;
        if (!Objects.equals(onMarkerField.getText().trim(), originalOnMarker)) return true;
        if (enableRegexInMarkersCheck.isSelected() != originalEnableRegexInMarkers) return true;
        if (!Objects.equals(doNotFormatField.getText().trim(), originalDoNotFormat)) return true;

        return false;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public CodeStyleHeaderBar getHeaderBar() {
        return headerBar;
    }
}
