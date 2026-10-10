package dev.lumina.ui;

import dev.lumina.tools.TerminalSettings;
import dev.lumina.tools.TerminalSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Settings UI page for Tools > Terminal in Lumina IDE.
 * 1:1 visual match with reference screenshot layout.
 */
public class SettingsToolsTerminalPage extends VBox {

    // Terminal engine
    private final ComboBox<String> terminalEngineCombo;

    // Command Completion
    private final CheckBox showCompletionPopupCheck;
    private final ToggleGroup completionModeGroup;
    private final RadioButton alwaysRadio;
    private final RadioButton onlyForParametersRadio;
    private final ComboBox<String> showCompletionPopupCombo;
    private final ComboBox<String> insertSuggestionCombo;

    // Project Settings
    private final TextField startDirectoryField;
    private final TextField environmentVariablesField;

    // Font Settings
    private final ComboBox<String> fontCombo;
    private final ComboBox<String> fallbackFontCombo;
    private final TextField fontSizeField;
    private final TextField lineHeightField;
    private final TextField columnWidthField;

    // Application Settings
    private final ComboBox<String> shellPathCombo;
    private final TextField defaultTabNameField;
    private final CheckBox enforceContrastCheck;
    private final TextField contrastRatioField;
    private final CheckBox showSeparatorsCheck;
    private final CheckBox audibleBellCheck;
    private final CheckBox closeSessionCheck;
    private final CheckBox mouseReportingCheck;
    private final CheckBox moveFocusEscapeCheck;
    private final CheckBox pasteMiddleClickCheck;
    private final CheckBox overrideShortcutsCheck;
    private final CheckBox shellIntegrationCheck;
    private final CheckBox highlightHyperlinksCheck;
    private final CheckBox activateVirtualenvCheck;
    private final CheckBox addPhpToPathCheck;
    private final ComboBox<String> cursorShapeCombo;

    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsToolsTerminalPage() {
        setSpacing(12);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Top Row: Terminal engine: [Reworked 2025 v]
        Label engineLabel = new Label("Terminal engine:");
        engineLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 120px;");
        terminalEngineCombo = new ComboBox<>();
        terminalEngineCombo.getItems().addAll("Reworked 2025", "Classic", "Default");
        terminalEngineCombo.setValue("Reworked 2025");
        styleComboBox(terminalEngineCombo);
        HBox engineRow = new HBox(10, engineLabel, terminalEngineCombo);
        engineRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Command Completion
        HBox completionHeader = createSectionHeader("Command Completion");

        showCompletionPopupCheck = new CheckBox("Show a completion popup as you type:");
        showCompletionPopupCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        showCompletionPopupCheck.setSelected(true);

        completionModeGroup = new ToggleGroup();
        alwaysRadio = new RadioButton("Always");
        alwaysRadio.setToggleGroup(completionModeGroup);
        alwaysRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        onlyForParametersRadio = new RadioButton("Only for parameters");
        onlyForParametersRadio.setToggleGroup(completionModeGroup);
        onlyForParametersRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        onlyForParametersRadio.setSelected(true);

        VBox radioBox = new VBox(6, alwaysRadio, onlyForParametersRadio);
        radioBox.setPadding(new Insets(0, 0, 0, 22));
        radioBox.disableProperty().bind(showCompletionPopupCheck.selectedProperty().not());

        Label showPopupLabel = new Label("Show completion popup with:");
        showPopupLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 180px;");
        showCompletionPopupCombo = new ComboBox<>();
        showCompletionPopupCombo.getItems().addAll("Ctrl+Space", "Tab", "Ctrl+Space or Tab");
        showCompletionPopupCombo.setValue("Ctrl+Space");
        styleComboBox(showCompletionPopupCombo);
        HBox showPopupRow = new HBox(10, showPopupLabel, showCompletionPopupCombo);
        showPopupRow.setAlignment(Pos.CENTER_LEFT);

        Label insertSuggestionLabel = new Label("Insert suggestion with:");
        insertSuggestionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 180px;");
        insertSuggestionCombo = new ComboBox<>();
        insertSuggestionCombo.getItems().addAll("Enter", "Tab", "Enter or Tab", "Space");
        insertSuggestionCombo.setValue("Enter");
        styleComboBox(insertSuggestionCombo);
        HBox insertSuggestionRow = new HBox(10, insertSuggestionLabel, insertSuggestionCombo);
        insertSuggestionRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Project Settings
        HBox projectHeader = createSectionHeader("Project Settings");

        Label startDirLabel = new Label("Start directory:");
        startDirLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 150px;");
        startDirectoryField = new TextField();
        startDirectoryField.setPromptText("Default project directory");
        styleTextField(startDirectoryField);
        HBox.setHgrow(startDirectoryField, Priority.ALWAYS);

        Button browseDirBtn = new Button("📁");
        styleIconButton(browseDirBtn);
        browseDirBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Terminal Start Directory");
            File current = new File(startDirectoryField.getText());
            if (current.isDirectory()) chooser.setInitialDirectory(current);
            File chosen = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (chosen != null) {
                startDirectoryField.setText(chosen.getAbsolutePath());
                notifyModified();
            }
        });
        HBox startDirRow = new HBox(8, startDirLabel, startDirectoryField, browseDirBtn);
        startDirRow.setAlignment(Pos.CENTER_LEFT);

        Label envLabel = new Label("Environment variables:");
        envLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 150px;");
        environmentVariablesField = new TextField();
        environmentVariablesField.setPromptText("Environment variables");
        styleTextField(environmentVariablesField);
        HBox.setHgrow(environmentVariablesField, Priority.ALWAYS);

        Button envModalBtn = new Button("🗋");
        styleIconButton(envModalBtn);
        envModalBtn.setOnAction(e -> showEnvVariablesDialog());
        HBox envRow = new HBox(8, envLabel, environmentVariablesField, envModalBtn);
        envRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Font Settings
        HBox fontHeader = createSectionHeader("Font Settings");

        Label fontLabel = new Label("Font:");
        fontLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 60px;");
        fontCombo = new ComboBox<>();
        populateFonts(fontCombo);
        styleComboBox(fontCombo);
        fontCombo.setPrefWidth(220);

        Label fallbackLabel = new Label("Fallback:");
        fallbackLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 60px;");
        fallbackFontCombo = new ComboBox<>();
        populateFonts(fallbackFontCombo);
        styleComboBox(fallbackFontCombo);
        fallbackFontCombo.setPrefWidth(220);

        HBox fontRow = new HBox(12, fontLabel, fontCombo, fallbackLabel, fallbackFontCombo);
        fontRow.setAlignment(Pos.CENTER_LEFT);

        Label sizeLabel = new Label("Size:");
        sizeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        fontSizeField = new TextField("13.0");
        fontSizeField.setPrefWidth(55);
        styleTextField(fontSizeField);

        Label lineHLabel = new Label("Line height:");
        lineHLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        lineHeightField = new TextField("1.0");
        lineHeightField.setPrefWidth(50);
        styleTextField(lineHeightField);

        Label colWLabel = new Label("Column width:");
        colWLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        columnWidthField = new TextField("1.0");
        columnWidthField.setPrefWidth(50);
        styleTextField(columnWidthField);

        HBox metricsRow = new HBox(12, sizeLabel, fontSizeField, lineHLabel, lineHeightField, colWLabel, columnWidthField);
        metricsRow.setAlignment(Pos.CENTER_LEFT);

        Hyperlink configureColorsLink = new Hyperlink("Configure colors...");
        configureColorsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        configureColorsLink.setOnMouseEntered(e -> configureColorsLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-underline: true; -fx-border-color: transparent; -fx-padding: 0;"));
        configureColorsLink.setOnMouseExited(e -> configureColorsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;"));

        // Section: Application Settings
        HBox appHeader = createSectionHeader("Application Settings");

        Label shellLabel = new Label("Shell path:");
        shellLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        shellPathCombo = new ComboBox<>();
        shellPathCombo.setEditable(true);
        shellPathCombo.getItems().addAll("/bin/bash", "/bin/sh", "/bin/zsh", "/usr/bin/fish");
        shellPathCombo.setValue("/bin/bash");
        styleComboBox(shellPathCombo);
        HBox.setHgrow(shellPathCombo, Priority.ALWAYS);

        Button browseShellBtn = new Button("…");
        styleIconButton(browseShellBtn);
        browseShellBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Shell Executable");
            File chosen = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (chosen != null) {
                shellPathCombo.setValue(chosen.getAbsolutePath());
                notifyModified();
            }
        });
        HBox shellRow = new HBox(8, shellLabel, shellPathCombo, browseShellBtn);
        shellRow.setAlignment(Pos.CENTER_LEFT);

        Label tabNameLabel = new Label("Default tab name:");
        tabNameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        defaultTabNameField = new TextField("Local");
        styleTextField(defaultTabNameField);
        HBox.setHgrow(defaultTabNameField, Priority.ALWAYS);
        HBox tabNameRow = new HBox(8, tabNameLabel, defaultTabNameField);
        tabNameRow.setAlignment(Pos.CENTER_LEFT);

        // Checkboxes list
        enforceContrastCheck = new CheckBox("Enforce minimum contrast ratio:");
        styleCheckBox(enforceContrastCheck);
        enforceContrastCheck.setSelected(true);

        contrastRatioField = new TextField("4.5");
        contrastRatioField.setPrefWidth(50);
        styleTextField(contrastRatioField);
        contrastRatioField.disableProperty().bind(enforceContrastCheck.selectedProperty().not());

        Label contrastHelp = new Label("?");
        contrastHelp.setStyle("-fx-background-color: #393B40; -fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 1 5; -fx-background-radius: 8;");
        contrastHelp.setTooltip(new Tooltip("Ensures text color in the terminal maintains at least this contrast ratio against background."));

        HBox contrastRow = new HBox(8, enforceContrastCheck, contrastRatioField, contrastHelp);
        contrastRow.setAlignment(Pos.CENTER_LEFT);

        showSeparatorsCheck = new CheckBox("Show separators between executed commands");
        styleCheckBox(showSeparatorsCheck);
        showSeparatorsCheck.setSelected(true);

        audibleBellCheck = new CheckBox("Audible bell");
        styleCheckBox(audibleBellCheck);
        audibleBellCheck.setSelected(true);

        closeSessionCheck = new CheckBox("Close session when it ends");
        styleCheckBox(closeSessionCheck);
        closeSessionCheck.setSelected(true);

        mouseReportingCheck = new CheckBox("Mouse reporting");
        styleCheckBox(mouseReportingCheck);
        mouseReportingCheck.setSelected(true);

        moveFocusEscapeCheck = new CheckBox("Move focus to the editor with Escape");
        styleCheckBox(moveFocusEscapeCheck);
        moveFocusEscapeCheck.setSelected(true);

        pasteMiddleClickCheck = new CheckBox("Paste on middle mouse button click");
        styleCheckBox(pasteMiddleClickCheck);
        pasteMiddleClickCheck.setSelected(true);

        overrideShortcutsCheck = new CheckBox("Override IDE shortcuts");
        styleCheckBox(overrideShortcutsCheck);
        overrideShortcutsCheck.setSelected(true);

        Hyperlink configureKeybindingsLink = new Hyperlink("Configure terminal keybindings");
        configureKeybindingsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        configureKeybindingsLink.setOnMouseEntered(e -> configureKeybindingsLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-underline: true; -fx-border-color: transparent; -fx-padding: 0;"));
        configureKeybindingsLink.setOnMouseExited(e -> configureKeybindingsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;"));

        HBox overrideRow = new HBox(8, overrideShortcutsCheck, configureKeybindingsLink);
        overrideRow.setAlignment(Pos.CENTER_LEFT);

        shellIntegrationCheck = new CheckBox("Shell integration");
        styleCheckBox(shellIntegrationCheck);
        shellIntegrationCheck.setSelected(true);

        highlightHyperlinksCheck = new CheckBox("Highlight hyperlinks");
        styleCheckBox(highlightHyperlinksCheck);
        highlightHyperlinksCheck.setSelected(true);

        activateVirtualenvCheck = new CheckBox("Activate virtualenv");
        styleCheckBox(activateVirtualenvCheck);
        activateVirtualenvCheck.setSelected(true);

        addPhpToPathCheck = new CheckBox("Add default project PHP interpreter to $PATH");
        styleCheckBox(addPhpToPathCheck);
        addPhpToPathCheck.setSelected(true);

        Label cursorShapeLabel = new Label("Cursor shape:");
        cursorShapeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 100px;");
        cursorShapeCombo = new ComboBox<>();
        cursorShapeCombo.getItems().addAll("Block", "Underline", "Vertical Bar");
        cursorShapeCombo.setValue("Block");
        styleComboBox(cursorShapeCombo);
        HBox cursorRow = new HBox(10, cursorShapeLabel, cursorShapeCombo);
        cursorRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
                engineRow,
                completionHeader,
                showCompletionPopupCheck,
                radioBox,
                showPopupRow,
                insertSuggestionRow,
                projectHeader,
                startDirRow,
                envRow,
                fontHeader,
                fontRow,
                metricsRow,
                configureColorsLink,
                appHeader,
                shellRow,
                tabNameRow,
                contrastRow,
                showSeparatorsCheck,
                audibleBellCheck,
                closeSessionCheck,
                mouseReportingCheck,
                moveFocusEscapeCheck,
                pasteMiddleClickCheck,
                overrideRow,
                shellIntegrationCheck,
                highlightHyperlinksCheck,
                activateVirtualenvCheck,
                addPhpToPathCheck,
                cursorRow
        );

        setupListeners();
        loadSettings();
    }

    private void populateFonts(ComboBox<String> combo) {
        combo.getItems().add("JetBrains Mono");
        combo.getItems().add("Fira Code");
        combo.getItems().add("Cascadia Code");
        combo.getItems().add("Consolas");
        combo.getItems().add("Monospaced");
        for (String f : Font.getFamilies()) {
            if (!combo.getItems().contains(f)) combo.getItems().add(f);
        }
        combo.setValue("JetBrains Mono");
    }

    private HBox createSectionHeader(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox box = new HBox(12, titleLabel, line);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 0, 4, 0));
        return box;
    }

    private void styleTextField(TextField field) {
        field.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
    }

    private void styleComboBox(ComboBox<String> combo) {
        combo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleIconButton(Button btn) {
        btn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8; -fx-cursor: hand;");
    }

    private void showEnvVariablesDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Environment Variables");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextArea area = new TextArea(environmentVariablesField.getText());
        area.setPromptText("KEY=VALUE (one per line)");
        area.setPrefSize(350, 200);
        dialog.getDialogPane().setContent(area);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? area.getText() : null);
        dialog.showAndWait().ifPresent(res -> {
            environmentVariablesField.setText(res.replace("\n", ";"));
            notifyModified();
        });
    }

    private void setupListeners() {
        terminalEngineCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        showCompletionPopupCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        completionModeGroup.selectedToggleProperty().addListener((o, ov, nv) -> notifyModified());
        showCompletionPopupCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        insertSuggestionCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        startDirectoryField.textProperty().addListener((o, ov, nv) -> notifyModified());
        environmentVariablesField.textProperty().addListener((o, ov, nv) -> notifyModified());
        fontCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        fallbackFontCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        fontSizeField.textProperty().addListener((o, ov, nv) -> notifyModified());
        lineHeightField.textProperty().addListener((o, ov, nv) -> notifyModified());
        columnWidthField.textProperty().addListener((o, ov, nv) -> notifyModified());
        shellPathCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        defaultTabNameField.textProperty().addListener((o, ov, nv) -> notifyModified());
        enforceContrastCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        contrastRatioField.textProperty().addListener((o, ov, nv) -> notifyModified());
        showSeparatorsCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        audibleBellCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        closeSessionCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        mouseReportingCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        moveFocusEscapeCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        pasteMiddleClickCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        overrideShortcutsCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        shellIntegrationCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        highlightHyperlinksCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        activateVirtualenvCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        addPhpToPathCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        cursorShapeCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            TerminalSettings s = TerminalSettingsManager.getInstance().getSettings();
            terminalEngineCombo.setValue(s.getTerminalEngine());
            showCompletionPopupCheck.setSelected(s.isShowCompletionPopup());
            if ("Always".equals(s.getCompletionPopupMode())) {
                alwaysRadio.setSelected(true);
            } else {
                onlyForParametersRadio.setSelected(true);
            }
            showCompletionPopupCombo.setValue(s.getShowCompletionPopupShortcut());
            insertSuggestionCombo.setValue(s.getInsertSuggestionShortcut());
            startDirectoryField.setText(s.getStartDirectory());
            environmentVariablesField.setText(s.getEnvironmentVariables());
            fontCombo.setValue(s.getFontFamily());
            fallbackFontCombo.setValue(s.getFallbackFontFamily());
            fontSizeField.setText(String.valueOf(s.getFontSize()));
            lineHeightField.setText(String.valueOf(s.getLineHeight()));
            columnWidthField.setText(String.valueOf(s.getColumnWidth()));
            shellPathCombo.setValue(s.getShellPath());
            defaultTabNameField.setText(s.getDefaultTabName());
            enforceContrastCheck.setSelected(s.isEnforceMinimumContrastRatio());
            contrastRatioField.setText(String.valueOf(s.getContrastRatio()));
            showSeparatorsCheck.setSelected(s.isShowSeparatorsBetweenExecutedCommands());
            audibleBellCheck.setSelected(s.isAudibleBell());
            closeSessionCheck.setSelected(s.isCloseSessionWhenItEnds());
            mouseReportingCheck.setSelected(s.isMouseReporting());
            moveFocusEscapeCheck.setSelected(s.isMoveFocusToEditorWithEscape());
            pasteMiddleClickCheck.setSelected(s.isPasteOnMiddleMouseButtonClick());
            overrideShortcutsCheck.setSelected(s.isOverrideIdeShortcuts());
            shellIntegrationCheck.setSelected(s.isShellIntegration());
            highlightHyperlinksCheck.setSelected(s.isHighlightHyperlinks());
            activateVirtualenvCheck.setSelected(s.isActivateVirtualenv());
            addPhpToPathCheck.setSelected(s.isAddDefaultPhpInterpreterToPath());
            cursorShapeCombo.setValue(s.getCursorShape());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        TerminalSettings saved = TerminalSettingsManager.getInstance().getSettings();
        if (!Objects.equals(terminalEngineCombo.getValue(), saved.getTerminalEngine())) return true;
        if (showCompletionPopupCheck.isSelected() != saved.isShowCompletionPopup()) return true;
        String mode = alwaysRadio.isSelected() ? "Always" : "Only for parameters";
        if (!Objects.equals(mode, saved.getCompletionPopupMode())) return true;
        if (!Objects.equals(showCompletionPopupCombo.getValue(), saved.getShowCompletionPopupShortcut())) return true;
        if (!Objects.equals(insertSuggestionCombo.getValue(), saved.getInsertSuggestionShortcut())) return true;
        if (!Objects.equals(startDirectoryField.getText().trim(), saved.getStartDirectory())) return true;
        if (!Objects.equals(environmentVariablesField.getText().trim(), saved.getEnvironmentVariables())) return true;
        if (!Objects.equals(fontCombo.getValue(), saved.getFontFamily())) return true;
        if (!Objects.equals(fallbackFontCombo.getValue(), saved.getFallbackFontFamily())) return true;
        if (parseNumber(fontSizeField.getText(), 13.0) != saved.getFontSize()) return true;
        if (parseNumber(lineHeightField.getText(), 1.0) != saved.getLineHeight()) return true;
        if (parseNumber(columnWidthField.getText(), 1.0) != saved.getColumnWidth()) return true;
        if (!Objects.equals(shellPathCombo.getValue(), saved.getShellPath())) return true;
        if (!Objects.equals(defaultTabNameField.getText().trim(), saved.getDefaultTabName())) return true;
        if (enforceContrastCheck.isSelected() != saved.isEnforceMinimumContrastRatio()) return true;
        if (parseNumber(contrastRatioField.getText(), 4.5) != saved.getContrastRatio()) return true;
        if (showSeparatorsCheck.isSelected() != saved.isShowSeparatorsBetweenExecutedCommands()) return true;
        if (audibleBellCheck.isSelected() != saved.isAudibleBell()) return true;
        if (closeSessionCheck.isSelected() != saved.isCloseSessionWhenItEnds()) return true;
        if (mouseReportingCheck.isSelected() != saved.isMouseReporting()) return true;
        if (moveFocusEscapeCheck.isSelected() != saved.isMoveFocusToEditorWithEscape()) return true;
        if (pasteMiddleClickCheck.isSelected() != saved.isPasteOnMiddleMouseButtonClick()) return true;
        if (overrideShortcutsCheck.isSelected() != saved.isOverrideIdeShortcuts()) return true;
        if (shellIntegrationCheck.isSelected() != saved.isShellIntegration()) return true;
        if (highlightHyperlinksCheck.isSelected() != saved.isHighlightHyperlinks()) return true;
        if (activateVirtualenvCheck.isSelected() != saved.isActivateVirtualenv()) return true;
        if (addPhpToPathCheck.isSelected() != saved.isAddDefaultPhpInterpreterToPath()) return true;
        return !Objects.equals(cursorShapeCombo.getValue(), saved.getCursorShape());
    }

    public void apply() {
        TerminalSettings s = new TerminalSettings();
        s.setTerminalEngine(terminalEngineCombo.getValue());
        s.setShowCompletionPopup(showCompletionPopupCheck.isSelected());
        s.setCompletionPopupMode(alwaysRadio.isSelected() ? "Always" : "Only for parameters");
        s.setShowCompletionPopupShortcut(showCompletionPopupCombo.getValue());
        s.setInsertSuggestionShortcut(insertSuggestionCombo.getValue());
        s.setStartDirectory(startDirectoryField.getText().trim());
        s.setEnvironmentVariables(environmentVariablesField.getText().trim());
        s.setFontFamily(fontCombo.getValue());
        s.setFallbackFontFamily(fallbackFontCombo.getValue());
        s.setFontSize(parseNumber(fontSizeField.getText(), 13.0));
        s.setLineHeight(parseNumber(lineHeightField.getText(), 1.0));
        s.setColumnWidth(parseNumber(columnWidthField.getText(), 1.0));
        s.setShellPath(shellPathCombo.getValue() != null ? shellPathCombo.getValue().trim() : "/bin/bash");
        s.setDefaultTabName(defaultTabNameField.getText().trim());
        s.setEnforceMinimumContrastRatio(enforceContrastCheck.isSelected());
        s.setContrastRatio(parseNumber(contrastRatioField.getText(), 4.5));
        s.setShowSeparatorsBetweenExecutedCommands(showSeparatorsCheck.isSelected());
        s.setAudibleBell(audibleBellCheck.isSelected());
        s.setCloseSessionWhenItEnds(closeSessionCheck.isSelected());
        s.setMouseReporting(mouseReportingCheck.isSelected());
        s.setMoveFocusToEditorWithEscape(moveFocusEscapeCheck.isSelected());
        s.setPasteOnMiddleMouseButtonClick(pasteMiddleClickCheck.isSelected());
        s.setOverrideIdeShortcuts(overrideShortcutsCheck.isSelected());
        s.setShellIntegration(shellIntegrationCheck.isSelected());
        s.setHighlightHyperlinks(highlightHyperlinksCheck.isSelected());
        s.setActivateVirtualenv(activateVirtualenvCheck.isSelected());
        s.setAddDefaultPhpInterpreterToPath(addPhpToPathCheck.isSelected());
        s.setCursorShape(cursorShapeCombo.getValue());

        TerminalSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    private double parseNumber(String text, double fallback) {
        if (text == null || text.isBlank()) return fallback;
        try {
            double v = Double.parseDouble(text.trim());
            return v > 0 ? v : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public TextField getStartDirectoryField() {
        return startDirectoryField;
    }

    public TextField getDefaultTabNameField() {
        return defaultTabNameField;
    }

    public CheckBox getAudibleBellCheck() {
        return audibleBellCheck;
    }

    public ComboBox<String> getCursorShapeCombo() {
        return cursorShapeCombo;
    }
}
