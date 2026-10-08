package dev.lumina.ui;

import dev.lumina.readermode.ReaderModeManager;
import dev.lumina.readermode.ReaderModeOption;
import dev.lumina.readermode.ReaderModeSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings page for Editor > Reader Mode in Lumina IDE.
 * Faithfully matches reference IDE design and controls (Screenshot 1).
 * Features dynamic provider integration, complete dirty tracking, and hierarchical enable/disable logic.
 */
public class SettingsReaderModePage extends VBox {

    private final ReaderModeManager manager = ReaderModeManager.getInstance();

    private final CheckBox enableReaderModeCheckBox = new CheckBox("Enable Reader mode");
    private final Label subtitleLabel = new Label("Available for library and read-only files. Makes the code convenient to read using the options below.");

    private final VBox optionsGroup = new VBox(10);
    private final Label showInReaderModeLabel = new Label("Show in Reader mode:");

    private final CheckBox renderedDocsCheckBox = new CheckBox("Rendered documentation comments");
    private final CheckBox errorHighlightingCheckBox = new CheckBox("Error and warning highlighting, inspection widget");
    private final CheckBox fontLigaturesCheckBox = new CheckBox("Font ligatures");
    private final CheckBox increasedLineHeightCheckBox = new CheckBox("Increased line height");
    private final Label lineHeightMultiplierLabel = new Label("Increased by 1.2");
    private final CheckBox codeVisionHintsCheckBox = new CheckBox("Code vision hints for usages, inheritors, and related problems");

    private final Map<String, CheckBox> dynamicCheckBoxes = new HashMap<>();

    private final CheckBox formatCodeCheckBox = new CheckBox("Format code according to preferred style");
    private final VBox formattingGroup = new VBox(8);
    private final ToggleGroup schemeToggleGroup = new ToggleGroup();
    private final RadioButton useActiveSchemeRadio = new RadioButton();
    private final RadioButton chooseSchemeRadio = new RadioButton("Choose scheme:");
    private final ComboBox<String> schemeComboBox = new ComboBox<>();

    private Runnable onModifiedListener;
    private boolean updatingUi = false;

    public SettingsReaderModePage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");

        buildUi();
        setupListeners();
        loadFromManager();
    }

    private void buildUi() {
        // --- Header Section ---
        enableReaderModeCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        subtitleLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
        subtitleLabel.setWrapText(true);
        VBox.setMargin(subtitleLabel, new Insets(-6, 0, 8, 22));

        // --- Group: Show in Reader mode ---
        showInReaderModeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: normal;");

        styleCheckBox(renderedDocsCheckBox);
        styleCheckBox(errorHighlightingCheckBox);
        styleCheckBox(fontLigaturesCheckBox);
        styleCheckBox(increasedLineHeightCheckBox);
        styleCheckBox(codeVisionHintsCheckBox);
        styleCheckBox(formatCodeCheckBox);

        lineHeightMultiplierLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
        HBox lineHeightBox = new HBox(8, increasedLineHeightCheckBox, lineHeightMultiplierLabel);
        lineHeightBox.setAlignment(Pos.CENTER_LEFT);

        VBox checklistContainer = new VBox(9);
        checklistContainer.setPadding(new Insets(0, 0, 0, 20));
        checklistContainer.getChildren().addAll(
                renderedDocsCheckBox,
                errorHighlightingCheckBox,
                fontLigaturesCheckBox,
                lineHeightBox,
                codeVisionHintsCheckBox
        );

        // Dynamically add contributed options
        List<ReaderModeOption> contributed = manager.getAllContributedOptions();
        for (ReaderModeOption opt : contributed) {
            CheckBox cb = new CheckBox(opt.title());
            styleCheckBox(cb);
            if (opt.subtitle() != null && !opt.subtitle().isBlank()) {
                Label sub = new Label(opt.subtitle());
                sub.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
                HBox row = new HBox(8, cb, sub);
                row.setAlignment(Pos.CENTER_LEFT);
                checklistContainer.getChildren().add(row);
            } else {
                checklistContainer.getChildren().add(cb);
            }
            dynamicCheckBoxes.put(opt.id(), cb);
        }

        // --- Formatting Scheme Section ---
        useActiveSchemeRadio.setToggleGroup(schemeToggleGroup);
        chooseSchemeRadio.setToggleGroup(schemeToggleGroup);
        styleRadioButton(useActiveSchemeRadio);
        styleRadioButton(chooseSchemeRadio);

        schemeComboBox.setPrefWidth(160);
        schemeComboBox.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        HBox chooseSchemeRow = new HBox(8, chooseSchemeRadio, schemeComboBox);
        chooseSchemeRow.setAlignment(Pos.CENTER_LEFT);

        formattingGroup.setPadding(new Insets(0, 0, 0, 20));
        formattingGroup.getChildren().addAll(useActiveSchemeRadio, chooseSchemeRow);

        VBox formatSection = new VBox(8);
        formatSection.getChildren().addAll(formatCodeCheckBox, formattingGroup);

        optionsGroup.setSpacing(10);
        optionsGroup.getChildren().addAll(
                showInReaderModeLabel,
                checklistContainer,
                formatSection
        );

        getChildren().addAll(
                enableReaderModeCheckBox,
                subtitleLabel,
                optionsGroup
        );
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void setupListeners() {
        enableReaderModeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi) {
                manager.getWorkingSettings().setEnabled(newVal);
                updateControlStates();
                notifyModified();
            }
        });

        renderedDocsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi) {
                manager.getWorkingSettings().setRenderedDocs(newVal);
                notifyModified();
            }
        });

        errorHighlightingCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi) {
                manager.getWorkingSettings().setErrorHighlighting(newVal);
                notifyModified();
            }
        });

        fontLigaturesCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi) {
                manager.getWorkingSettings().setFontLigatures(newVal);
                notifyModified();
            }
        });

        increasedLineHeightCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi) {
                manager.getWorkingSettings().setIncreasedLineHeight(newVal);
                notifyModified();
            }
        });

        codeVisionHintsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi) {
                manager.getWorkingSettings().setCodeVisionHints(newVal);
                notifyModified();
            }
        });

        formatCodeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi) {
                manager.getWorkingSettings().setFormatCode(newVal);
                updateControlStates();
                notifyModified();
            }
        });

        useActiveSchemeRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi && newVal) {
                manager.getWorkingSettings().setUseActiveScheme(true);
                updateControlStates();
                notifyModified();
            }
        });

        chooseSchemeRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi && newVal) {
                manager.getWorkingSettings().setUseActiveScheme(false);
                updateControlStates();
                notifyModified();
            }
        });

        schemeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi && newVal != null) {
                manager.getWorkingSettings().setChosenScheme(newVal);
                notifyModified();
            }
        });

        for (Map.Entry<String, CheckBox> entry : dynamicCheckBoxes.entrySet()) {
            final String id = entry.getKey();
            entry.getValue().selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (!updatingUi) {
                    manager.getWorkingSettings().setDynamicOption(id, newVal);
                    notifyModified();
                }
            });
        }
    }

    private void updateControlStates() {
        boolean enabled = enableReaderModeCheckBox.isSelected();
        optionsGroup.setDisable(!enabled);

        boolean formatEnabled = enabled && formatCodeCheckBox.isSelected();
        formattingGroup.setDisable(!formatEnabled);

        boolean isChooseScheme = chooseSchemeRadio.isSelected();
        schemeComboBox.setDisable(!formatEnabled || !isChooseScheme);
    }

    public void loadFromManager() {
        updatingUi = true;
        try {
            ReaderModeSettings s = manager.getWorkingSettings();
            enableReaderModeCheckBox.setSelected(s.isEnabled());
            renderedDocsCheckBox.setSelected(s.isRenderedDocs());
            errorHighlightingCheckBox.setSelected(s.isErrorHighlighting());
            fontLigaturesCheckBox.setSelected(s.isFontLigatures());
            increasedLineHeightCheckBox.setSelected(s.isIncreasedLineHeight());
            lineHeightMultiplierLabel.setText("Increased by " + s.getLineHeightMultiplier());
            codeVisionHintsCheckBox.setSelected(s.isCodeVisionHints());

            formatCodeCheckBox.setSelected(s.isFormatCode());

            String activeScheme = manager.getActiveSchemeName();
            useActiveSchemeRadio.setText("Use active scheme: " + activeScheme);

            List<String> available = manager.getAvailableSchemes();
            schemeComboBox.getItems().setAll(available);
            if (available.contains(s.getChosenScheme())) {
                schemeComboBox.setValue(s.getChosenScheme());
            } else if (!available.isEmpty()) {
                schemeComboBox.setValue(available.get(0));
            }

            if (s.isUseActiveScheme()) {
                useActiveSchemeRadio.setSelected(true);
            } else {
                chooseSchemeRadio.setSelected(true);
            }

            for (Map.Entry<String, CheckBox> entry : dynamicCheckBoxes.entrySet()) {
                entry.getValue().setSelected(s.getDynamicOption(entry.getKey(), false));
            }

            updateControlStates();
        } finally {
            updatingUi = false;
        }
    }

    public boolean isModified() {
        return manager.isModified();
    }

    public void apply() {
        manager.apply();
    }

    public void reset() {
        manager.reset();
        loadFromManager();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
        manager.setOnModifiedListener(listener);
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Accessors for testing
    public CheckBox getEnableReaderModeCheckBox() {
        return enableReaderModeCheckBox;
    }

    public CheckBox getRenderedDocsCheckBox() {
        return renderedDocsCheckBox;
    }

    public CheckBox getErrorHighlightingCheckBox() {
        return errorHighlightingCheckBox;
    }

    public CheckBox getFontLigaturesCheckBox() {
        return fontLigaturesCheckBox;
    }

    public CheckBox getIncreasedLineHeightCheckBox() {
        return increasedLineHeightCheckBox;
    }

    public CheckBox getCodeVisionHintsCheckBox() {
        return codeVisionHintsCheckBox;
    }

    public CheckBox getFormatCodeCheckBox() {
        return formatCodeCheckBox;
    }

    public RadioButton getUseActiveSchemeRadio() {
        return useActiveSchemeRadio;
    }

    public RadioButton getChooseSchemeRadio() {
        return chooseSchemeRadio;
    }

    public ComboBox<String> getSchemeComboBox() {
        return schemeComboBox;
    }
}
