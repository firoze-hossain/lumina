package dev.lumina.ui;

import dev.lumina.markdown.MarkdownLanguageSettings;
import dev.lumina.markdown.MarkdownLanguageSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Languages & Frameworks > Markdown settings page in Lumina IDE.
 * Matching the reference IDE layout with preview engine selection, extensions, Custom CSS, and Pandoc integration.
 */
public class SettingsLanguagesMarkdownPage extends VBox {

    private final MarkdownLanguageSettingsManager manager = MarkdownLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;
    private Runnable onNavigateToSmartKeys;

    private ComboBox<String> previewEngineCombo;
    private ComboBox<String> defaultLayoutCombo;
    private ComboBox<String> previewLayoutCombo;
    private ComboBox<Integer> previewFontSizeCombo;

    private CheckBox syncScrollCheckBox;
    private CheckBox injectLanguagesCheckBox;
    private CheckBox showProblemsCheckBox;
    private CheckBox groupDocumentsCheckBox;
    private CheckBox detectCommandsCheckBox;

    private CheckBox plantUmlCheckBox;

    // Custom CSS
    private CheckBox loadCustomCssCheckBox;
    private TextField customCssPathField;
    private Button customCssBrowseBtn;
    private CheckBox customCssRulesCheckBox;
    private TextArea customCssRulesArea;

    // Pandoc
    private TextField pandocPathField;
    private Button pandocBrowseBtn;
    private Button pandocTestBtn;
    private Label pandocTestResultLabel;
    private TextField saveImagesPathField;
    private Button saveImagesBrowseBtn;

    private Hyperlink smartKeysLink;

    private MarkdownLanguageSettings initialSettings;

    public SettingsLanguagesMarkdownPage() {
        this(null);
    }

    public SettingsLanguagesMarkdownPage(Runnable onNavigateToSmartKeys) {
        this.onNavigateToSmartKeys = onNavigateToSmartKeys;

        setSpacing(10);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        MarkdownLanguageSettings current = manager.getSettings();

        // 1. Dropdown rows
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        Label engineLabel = createLabel("Preview rendering engine:");
        previewEngineCombo = new ComboBox<>();
        previewEngineCombo.getItems().addAll(MarkdownLanguageSettings.PREVIEW_ENGINES);
        previewEngineCombo.setValue(current.getPreviewEngine());
        previewEngineCombo.setPrefWidth(260);
        styleComboBox(previewEngineCombo);
        previewEngineCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        grid.add(engineLabel, 0, 0);
        grid.add(previewEngineCombo, 1, 0);

        Label defaultLayoutLabel = createLabel("Default layout:");
        defaultLayoutCombo = new ComboBox<>();
        defaultLayoutCombo.getItems().addAll(MarkdownLanguageSettings.DEFAULT_LAYOUTS);
        defaultLayoutCombo.setValue(current.getDefaultLayout());
        defaultLayoutCombo.setPrefWidth(260);
        styleComboBox(defaultLayoutCombo);
        defaultLayoutCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        grid.add(defaultLayoutLabel, 0, 1);
        grid.add(defaultLayoutCombo, 1, 1);

        Label previewLayoutLabel = createLabel("Preview layout:");
        previewLayoutCombo = new ComboBox<>();
        previewLayoutCombo.getItems().addAll(MarkdownLanguageSettings.PREVIEW_LAYOUTS);
        previewLayoutCombo.setValue(current.getPreviewLayout());
        previewLayoutCombo.setPrefWidth(260);
        styleComboBox(previewLayoutCombo);
        previewLayoutCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        grid.add(previewLayoutLabel, 0, 2);
        grid.add(previewLayoutCombo, 1, 2);

        Label fontSizeLabel = createLabel("Preview font size:");
        previewFontSizeCombo = new ComboBox<>();
        previewFontSizeCombo.getItems().addAll(MarkdownLanguageSettings.FONT_SIZES);
        previewFontSizeCombo.setValue(current.getPreviewFontSize());
        previewFontSizeCombo.setPrefWidth(80);
        styleComboBox(previewFontSizeCombo);
        previewFontSizeCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        grid.add(fontSizeLabel, 0, 3);
        grid.add(previewFontSizeCombo, 1, 3);

        // 2. Checkboxes
        VBox checksBox = new VBox(8);
        checksBox.setPadding(new Insets(4, 0, 4, 0));

        syncScrollCheckBox = createCheckBox("Sync scroll in the editor and preview", current.isSyncScroll());
        injectLanguagesCheckBox = createCheckBox("Inject languages in code fences", current.isInjectLanguagesInCodeFences());
        showProblemsCheckBox = createCheckBox("Show problems in code fences", current.isShowProblemsInCodeFences());
        groupDocumentsCheckBox = createCheckBox("Group documents with the same name, but different extensions (.md, .html, .docx, .pdf)", current.isGroupDocumentsWithSameName());
        detectCommandsCheckBox = createCheckBox("Detect commands that can be run right from Markdown files", current.isDetectCommands());

        checksBox.getChildren().addAll(
                syncScrollCheckBox,
                injectLanguagesCheckBox,
                showProblemsCheckBox,
                groupDocumentsCheckBox,
                detectCommandsCheckBox
        );

        // 3. Markdown Extensions
        VBox extensionsBox = new VBox(6);
        Label extTitle = createLabel("Markdown Extensions:");
        HBox plantUmlRow = new HBox(8);
        plantUmlRow.setAlignment(Pos.CENTER_LEFT);

        plantUmlCheckBox = createCheckBox("PlantUML", current.isPlantUmlEnabled());
        Label helpIcon = new Label("\u24D8");
        helpIcon.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand;");
        helpIcon.setTooltip(new Tooltip("Enables PlantUML diagram rendering in Markdown previews."));

        Hyperlink installLink = new Hyperlink("Install");
        installLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0;");
        installLink.setOnAction(e -> plantUmlCheckBox.setSelected(true));

        plantUmlRow.getChildren().addAll(plantUmlCheckBox, helpIcon, installLink);
        extensionsBox.getChildren().addAll(extTitle, plantUmlRow);

        // 4. Custom CSS Section
        VBox customCssBox = buildCustomCssSection(current);

        // 5. Pandoc Settings Section
        VBox pandocBox = buildPandocSection(current);

        // 6. Smart Keys footer link
        HBox smartKeysRow = new HBox(4);
        smartKeysRow.setAlignment(Pos.CENTER_LEFT);
        smartKeysRow.setPadding(new Insets(6, 0, 0, 0));

        Label smartKeysPrefix = new Label("Configure in-editor assistance in ");
        smartKeysPrefix.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        smartKeysLink = new Hyperlink("Smart Keys Settings");
        smartKeysLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        smartKeysLink.setOnAction(e -> {
            if (onNavigateToSmartKeys != null) {
                onNavigateToSmartKeys.run();
            }
        });

        smartKeysRow.getChildren().addAll(smartKeysPrefix, smartKeysLink);

        getChildren().addAll(
                grid,
                checksBox,
                extensionsBox,
                customCssBox,
                pandocBox,
                smartKeysRow
        );
    }

    private VBox buildCustomCssSection(MarkdownLanguageSettings current) {
        VBox box = new VBox(8);
        VBox content = new VBox(8);
        content.setPadding(new Insets(4, 0, 4, 16));

        HBox header = createCollapsibleHeader("Custom CSS", content);

        HBox loadFromRow = new HBox(8);
        loadFromRow.setAlignment(Pos.CENTER_LEFT);
        loadCustomCssCheckBox = createCheckBox("Load from:", current.isLoadCustomCssFrom());
        loadCustomCssCheckBox.setPrefWidth(120);

        customCssPathField = new TextField(current.getCustomCssPath());
        styleTextField(customCssPathField);
        customCssPathField.setPrefWidth(420);
        customCssPathField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        customCssBrowseBtn = createBrowseButton();
        customCssBrowseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Custom CSS File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSS Files", "*.css"));
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                customCssPathField.setText(file.getAbsolutePath());
                loadCustomCssCheckBox.setSelected(true);
                fireModified();
            }
        });

        loadFromRow.getChildren().addAll(loadCustomCssCheckBox, customCssPathField, customCssBrowseBtn);

        customCssRulesCheckBox = createCheckBox("CSS rules:", current.isCustomCssRulesEnabled());

        HBox editorBox = new HBox(0);
        Label lineGutter = new Label(" 1 ");
        lineGutter.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #606366; -fx-padding: 4 6; -fx-font-family: monospace; -fx-font-size: 12px;");

        customCssRulesArea = new TextArea(current.getCustomCssRules());
        customCssRulesArea.setPrefRowCount(4);
        customCssRulesArea.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-family: monospace; -fx-font-size: 12px;");
        customCssRulesArea.textProperty().addListener((obs, oldV, newV) -> fireModified());
        HBox.setHgrow(customCssRulesArea, Priority.ALWAYS);

        editorBox.getChildren().addAll(lineGutter, customCssRulesArea);

        content.getChildren().addAll(loadFromRow, customCssRulesCheckBox, editorBox);
        box.getChildren().addAll(header, content);
        return box;
    }

    private VBox buildPandocSection(MarkdownLanguageSettings current) {
        VBox box = new VBox(8);
        VBox content = new VBox(8);
        content.setPadding(new Insets(4, 0, 4, 16));

        HBox header = createCollapsibleHeader("Pandoc Settings", content);

        HBox pathRow = new HBox(8);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        Label pathLabel = createLabel("Path to Pandoc executable:");
        pathLabel.setPrefWidth(220);

        pandocPathField = new TextField(current.getPandocExecutablePath());
        pandocPathField.setPromptText("Cannot find the Pandoc executable");
        styleTextField(pandocPathField);
        pandocPathField.setPrefWidth(380);
        pandocPathField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        pandocBrowseBtn = createBrowseButton();
        pandocBrowseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Pandoc Executable");
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                pandocPathField.setText(file.getAbsolutePath());
                fireModified();
            }
        });

        pandocTestBtn = new Button("Test");
        pandocTestBtn.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 12px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 3 12;"
        );
        pandocTestBtn.setOnAction(e -> {
            String ver = MarkdownLanguageSettings.testPandocVersion(pandocPathField.getText());
            if (ver != null) {
                pandocTestResultLabel.setText("Pandoc detected: " + ver);
                pandocTestResultLabel.setStyle("-fx-text-fill: #62B543; -fx-font-size: 12px;");
            } else {
                pandocTestResultLabel.setText("Cannot run pandoc at specified path.");
                pandocTestResultLabel.setStyle("-fx-text-fill: #E05555; -fx-font-size: 12px;");
            }
            pandocTestResultLabel.setVisible(true);
        });

        pandocTestResultLabel = new Label();
        pandocTestResultLabel.setVisible(false);

        pathRow.getChildren().addAll(pathLabel, pandocPathField, pandocBrowseBtn, pandocTestBtn);

        HBox saveImagesRow = new HBox(8);
        saveImagesRow.setAlignment(Pos.CENTER_LEFT);

        Label saveImagesLabel = createLabel("Save images from Microsoft Word to:");
        saveImagesLabel.setPrefWidth(220);

        saveImagesPathField = new TextField(current.getSaveImagesFromWordPath());
        styleTextField(saveImagesPathField);
        saveImagesPathField.setPrefWidth(380);
        saveImagesPathField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        saveImagesBrowseBtn = createBrowseButton();
        saveImagesBrowseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Image Destination Folder");
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                saveImagesPathField.setText(file.getAbsolutePath());
                fireModified();
            }
        });

        saveImagesRow.getChildren().addAll(saveImagesLabel, saveImagesPathField, saveImagesBrowseBtn);

        content.getChildren().addAll(pathRow, pandocTestResultLabel, saveImagesRow);
        box.getChildren().addAll(header, content);
        return box;
    }

    private HBox createCollapsibleHeader(String title, VBox content) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(6, 0, 2, 0));

        Label arrow = new Label("\u25BC"); // ▼
        arrow.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px; -fx-cursor: hand;");

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(arrow, label, line);

        Runnable toggle = () -> {
            boolean visible = !content.isVisible();
            content.setVisible(visible);
            content.setManaged(visible);
            arrow.setText(visible ? "\u25BC" : "\u25B6");
        };

        arrow.setOnMouseClicked(e -> toggle.run());
        label.setOnMouseClicked(e -> toggle.run());

        return box;
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private CheckBox createCheckBox(String text, boolean initial) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(initial);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        cb.selectedProperty().addListener((obs, oldV, newV) -> fireModified());
        return cb;
    }

    private void styleComboBox(ComboBox<?> combo) {
        combo.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 13px;"
        );
    }

    private void styleTextField(TextField tf) {
        tf.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 13px;"
        );
    }

    private Button createBrowseButton() {
        Button btn = new Button("\uD83D\uDCC1");
        btn.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 12px; " +
                "-fx-cursor: hand; " +
                "-fx-pref-width: 28px;"
        );
        return btn;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnNavigateToSmartKeys(Runnable onNavigateToSmartKeys) {
        this.onNavigateToSmartKeys = onNavigateToSmartKeys;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialSettings = getFormSettings();
    }

    private MarkdownLanguageSettings getFormSettings() {
        MarkdownLanguageSettings s = new MarkdownLanguageSettings();
        if (previewEngineCombo.getValue() != null) s.setPreviewEngine(previewEngineCombo.getValue());
        if (defaultLayoutCombo.getValue() != null) s.setDefaultLayout(defaultLayoutCombo.getValue());
        if (previewLayoutCombo.getValue() != null) s.setPreviewLayout(previewLayoutCombo.getValue());
        if (previewFontSizeCombo.getValue() != null) s.setPreviewFontSize(previewFontSizeCombo.getValue());

        s.setSyncScroll(syncScrollCheckBox.isSelected());
        s.setInjectLanguagesInCodeFences(injectLanguagesCheckBox.isSelected());
        s.setShowProblemsInCodeFences(showProblemsCheckBox.isSelected());
        s.setGroupDocumentsWithSameName(groupDocumentsCheckBox.isSelected());
        s.setDetectCommands(detectCommandsCheckBox.isSelected());
        s.setPlantUmlEnabled(plantUmlCheckBox.isSelected());

        s.setLoadCustomCssFrom(loadCustomCssCheckBox.isSelected());
        s.setCustomCssPath(customCssPathField.getText() != null ? customCssPathField.getText() : "");
        s.setCustomCssRulesEnabled(customCssRulesCheckBox.isSelected());
        s.setCustomCssRules(customCssRulesArea.getText() != null ? customCssRulesArea.getText() : "");

        s.setPandocExecutablePath(pandocPathField.getText() != null ? pandocPathField.getText() : "");
        s.setSaveImagesFromWordPath(saveImagesPathField.getText() != null ? saveImagesPathField.getText() : "");

        return s;
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setSettings(getFormSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        if (initialSettings != null) {
            previewEngineCombo.setValue(initialSettings.getPreviewEngine());
            defaultLayoutCombo.setValue(initialSettings.getDefaultLayout());
            previewLayoutCombo.setValue(initialSettings.getPreviewLayout());
            previewFontSizeCombo.setValue(initialSettings.getPreviewFontSize());

            syncScrollCheckBox.setSelected(initialSettings.isSyncScroll());
            injectLanguagesCheckBox.setSelected(initialSettings.isInjectLanguagesInCodeFences());
            showProblemsCheckBox.setSelected(initialSettings.isShowProblemsInCodeFences());
            groupDocumentsCheckBox.setSelected(initialSettings.isGroupDocumentsWithSameName());
            detectCommandsCheckBox.setSelected(initialSettings.isDetectCommands());
            plantUmlCheckBox.setSelected(initialSettings.isPlantUmlEnabled());

            loadCustomCssCheckBox.setSelected(initialSettings.isLoadCustomCssFrom());
            customCssPathField.setText(initialSettings.getCustomCssPath());
            customCssRulesCheckBox.setSelected(initialSettings.isCustomCssRulesEnabled());
            customCssRulesArea.setText(initialSettings.getCustomCssRules());

            pandocPathField.setText(initialSettings.getPandocExecutablePath());
            saveImagesPathField.setText(initialSettings.getSaveImagesFromWordPath());
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public ComboBox<String> getPreviewEngineCombo() { return previewEngineCombo; }
    public ComboBox<String> getDefaultLayoutCombo() { return defaultLayoutCombo; }
    public ComboBox<String> getPreviewLayoutCombo() { return previewLayoutCombo; }
    public ComboBox<Integer> getPreviewFontSizeCombo() { return previewFontSizeCombo; }
    public CheckBox getSyncScrollCheckBox() { return syncScrollCheckBox; }
    public CheckBox getInjectLanguagesCheckBox() { return injectLanguagesCheckBox; }
    public CheckBox getShowProblemsCheckBox() { return showProblemsCheckBox; }
    public CheckBox getGroupDocumentsCheckBox() { return groupDocumentsCheckBox; }
    public CheckBox getDetectCommandsCheckBox() { return detectCommandsCheckBox; }
    public CheckBox getPlantUmlCheckBox() { return plantUmlCheckBox; }
    public CheckBox getLoadCustomCssCheckBox() { return loadCustomCssCheckBox; }
    public TextField getCustomCssPathField() { return customCssPathField; }
    public CheckBox getCustomCssRulesCheckBox() { return customCssRulesCheckBox; }
    public TextArea getCustomCssRulesArea() { return customCssRulesArea; }
    public TextField getPandocPathField() { return pandocPathField; }
    public Button getPandocTestBtn() { return pandocTestBtn; }
    public Label getPandocTestResultLabel() { return pandocTestResultLabel; }
    public TextField getSaveImagesPathField() { return saveImagesPathField; }
    public Hyperlink getSmartKeysLink() { return smartKeysLink; }
}
