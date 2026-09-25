package dev.lumina.ui;

import dev.lumina.settings.InlineCompletionSettings;
import dev.lumina.settings.InlineCompletionSettings.DownloadModelsMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.StringConverter;

import java.util.*;

/**
 * Modern dynamic Editor > General > Inline Completion settings page.
 * Completely dynamic, backed by InlineCompletionSettings, supporting real-time dirty tracking,
 * language model download handling, activation flow, and full settings persistence.
 */
public class SettingsInlineCompletionPage extends VBox {

    // Navigation callback
    private Runnable onNavigateCodeCompletion;

    // 1. Local Full Line Completion
    private final CheckBox enableLocalFullLineCheck = new CheckBox("Enable local Full Line completion suggestions");
    private final Label localHint = new Label("Runs entirely on your local device without sending anything over the internet");
    private final Map<String, CheckBox> languageChecks = new LinkedHashMap<>();
    private final Map<String, Hyperlink> downloadLinks = new LinkedHashMap<>();
    private final ComboBox<DownloadModelsMode> downloadModelsCombo = new ComboBox<>();
    private final VBox languagesBox = new VBox(6);

    // 2. Cloud Completion
    private final CheckBox enableCloudCompletionCheck = new CheckBox("Enable cloud completion suggestions");
    private final Button activationButton = new Button("Go to Activation...");

    // 3. Typing & Behavior
    private final CheckBox enableAutomaticOnTypingCheck = new CheckBox("Enable automatic completion on typing");
    private final Label autoTypingHint = new Label("If disabled, completion suggestions can still be invoked via Alt+Shift+\\ shortcut");

    private final CheckBox enableMultilineCheck = new CheckBox("Enable multi-line suggestions");
    private final Label multilineHint = new Label("If disabled, only single-line suggestions will be shown");

    private final CheckBox syncInlineAndPopupCheck = new CheckBox("Synchronize inline and popup completions");
    private final Label syncHint = new Label("When enabled, inline completions will be shown in the popup completion list to avoid shortcut conflicts");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;
    private boolean cloudActivatedState = false;

    public SettingsInlineCompletionPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(10);

        buildUi();
        setupListeners();
        loadFromSettings(InlineCompletionSettings.getInstance());
    }

    public void setOnNavigateCodeCompletion(Runnable onNavigateCodeCompletion) {
        this.onNavigateCodeCompletion = onNavigateCodeCompletion;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void styleHint(Label lbl) {
        lbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        lbl.setWrapText(true);
        lbl.setPadding(new Insets(0, 0, 0, 20));
    }

    private void buildUi() {
        // --- 0. Top Navigation Link ---
        Text prefix = new Text("Go to ");
        prefix.setStyle("-fx-fill: #DFE1E5; -fx-font-size: 12px;");

        Hyperlink codeCompletionLink = new Hyperlink("Code Completion settings page");
        codeCompletionLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        codeCompletionLink.setOnMouseEntered(e -> codeCompletionLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
        codeCompletionLink.setOnMouseExited(e -> codeCompletionLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
        codeCompletionLink.setOnAction(e -> {
            if (onNavigateCodeCompletion != null) {
                onNavigateCodeCompletion.run();
            }
        });

        Text suffix = new Text(" to adjust lookup completion settings");
        suffix.setStyle("-fx-fill: #DFE1E5; -fx-font-size: 12px;");

        TextFlow navFlow = new TextFlow(prefix, codeCompletionLink, suffix);
        navFlow.setPadding(new Insets(0, 0, 6, 0));

        // --- 1. Local Full Line Completion Suggestions ---
        styleCheckBox(enableLocalFullLineCheck);
        styleHint(localHint);

        languagesBox.setPadding(new Insets(2, 0, 4, 20));
        languagesBox.setSpacing(5);

        for (String lang : InlineCompletionSettings.ALL_LANGUAGES) {
            CheckBox cb = new CheckBox(lang);
            styleCheckBox(cb);
            cb.setMinWidth(180);
            languageChecks.put(lang, cb);

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().add(cb);

            if (!InlineCompletionSettings.BUNDLED_LANGUAGES.contains(lang)) {
                Hyperlink dlLink = new Hyperlink("Download (100 MB)");
                dlLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
                dlLink.setOnMouseEntered(ev -> dlLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 11px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
                dlLink.setOnMouseExited(ev -> dlLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
                dlLink.setOnAction(ev -> {
                    // Simulate fast download and install
                    dlLink.setText("Installed");
                    dlLink.setDisable(true);
                    dlLink.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
                    cb.setSelected(true);
                    notifyModified();
                });
                downloadLinks.put(lang, dlLink);
                row.getChildren().add(dlLink);
            }

            languagesBox.getChildren().add(row);
        }

        // Download models dropdown
        Label downloadModelsLabel = new Label("Download models:");
        downloadModelsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        downloadModelsCombo.getItems().setAll(DownloadModelsMode.values());
        downloadModelsCombo.setConverter(new StringConverter<>() {
            @Override public String toString(DownloadModelsMode m) { return m != null ? m.getLabel() : ""; }
            @Override public DownloadModelsMode fromString(String s) { return DownloadModelsMode.fromLabel(s); }
        });
        downloadModelsCombo.setPrefWidth(180);
        downloadModelsCombo.setPrefHeight(25);
        downloadModelsCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox downloadModelsRow = new HBox(8, downloadModelsLabel, downloadModelsCombo);
        downloadModelsRow.setAlignment(Pos.CENTER_LEFT);
        downloadModelsRow.setPadding(new Insets(6, 0, 6, 20));

        // Master toggle listener for enabling/disabling languages
        enableLocalFullLineCheck.selectedProperty().addListener((obs, o, enabled) -> {
            languagesBox.setDisable(!enabled);
            downloadModelsRow.setDisable(!enabled);
            notifyModified();
        });

        VBox localSection = new VBox(4,
                enableLocalFullLineCheck,
                localHint,
                languagesBox,
                downloadModelsRow
        );

        // --- 2. Cloud Completion Suggestions ---
        HBox cloudCheckRow = new HBox(6);
        cloudCheckRow.setAlignment(Pos.CENTER_LEFT);
        styleCheckBox(enableCloudCompletionCheck);

        // AI swirl / badge glyph
        Label aiBadge = new Label("@");
        aiBadge.setStyle("-fx-text-fill: #B388FF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #31284A; -fx-padding: 1 4 1 4; -fx-background-radius: 4;");
        cloudCheckRow.getChildren().addAll(enableCloudCompletionCheck, aiBadge);

        activationButton.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 14 5 14; -fx-background-radius: 4; -fx-cursor: hand;");
        activationButton.setOnAction(e -> showActivationDialog());

        VBox activationBox = new VBox(activationButton);
        activationBox.setPadding(new Insets(2, 0, 6, 20));

        VBox cloudSection = new VBox(4, cloudCheckRow, activationBox);
        cloudSection.setPadding(new Insets(4, 0, 4, 0));

        // --- 3. Typing & Behavior ---
        styleCheckBox(enableAutomaticOnTypingCheck);
        styleHint(autoTypingHint);

        VBox autoTypingBox = new VBox(3, enableAutomaticOnTypingCheck, autoTypingHint);

        styleCheckBox(enableMultilineCheck);
        styleHint(multilineHint);

        VBox multilineBox = new VBox(3, enableMultilineCheck, multilineHint);

        styleCheckBox(syncInlineAndPopupCheck);
        styleHint(syncHint);

        VBox syncBox = new VBox(3, syncInlineAndPopupCheck, syncHint);

        VBox behaviorSection = new VBox(8, autoTypingBox, multilineBox, syncBox);
        behaviorSection.setPadding(new Insets(4, 0, 4, 0));

        // Assemble root
        getChildren().addAll(
                navFlow,
                localSection,
                cloudSection,
                behaviorSection
        );
    }

    private void showActivationDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Cloud Completion Activation");
        dialog.setHeaderText("Activate Cloud Completion Suggestions");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #1E1F22;");
        try {
            pane.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        } catch (Exception ignored) {}

        Label info = new Label("Lumina Cloud Completion connects to advanced models to provide\nstate-of-the-art multi-token code completion.\n\nClick Activate to enable cloud completion suggestions.");
        info.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        pane.setContent(new VBox(12, info));
        ButtonType activateBtnType = new ButtonType("Activate", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(activateBtnType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == activateBtnType);
        dialog.showAndWait().ifPresent(activated -> {
            if (activated) {
                cloudActivatedState = true;
                enableCloudCompletionCheck.setDisable(false);
                enableCloudCompletionCheck.setSelected(true);
                activationButton.setText("Activated");
                activationButton.setDisable(true);
                activationButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #6F737A; -fx-font-size: 12px; -fx-padding: 5 14 5 14; -fx-background-radius: 4; -fx-cursor: default;");
                notifyModified();
            }
        });
    }

    private void setupListeners() {
        downloadModelsCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        enableCloudCompletionCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        enableAutomaticOnTypingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        enableMultilineCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        syncInlineAndPopupCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        for (CheckBox cb : languageChecks.values()) {
            cb.selectedProperty().addListener((obs, o, n) -> notifyModified());
        }
    }

    public void loadFromSettings(InlineCompletionSettings s) {
        suppressEvents = true;
        try {
            enableLocalFullLineCheck.setSelected(s.isEnableLocalFullLine());
            languagesBox.setDisable(!s.isEnableLocalFullLine());
            downloadModelsCombo.setValue(s.getDownloadModelsMode());

            for (Map.Entry<String, CheckBox> entry : languageChecks.entrySet()) {
                String lang = entry.getKey();
                CheckBox cb = entry.getValue();
                cb.setSelected(s.isLanguageEnabled(lang));

                Hyperlink dl = downloadLinks.get(lang);
                if (dl != null) {
                    if (s.isLanguageDownloaded(lang)) {
                        dl.setText("Installed");
                        dl.setDisable(true);
                        dl.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
                    } else {
                        dl.setText("Download (100 MB)");
                        dl.setDisable(false);
                        dl.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
                    }
                }
            }

            cloudActivatedState = s.isCloudActivated();
            if (cloudActivatedState) {
                enableCloudCompletionCheck.setDisable(false);
                enableCloudCompletionCheck.setSelected(s.isEnableCloudCompletion());
                activationButton.setText("Activated");
                activationButton.setDisable(true);
                activationButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #6F737A; -fx-font-size: 12px; -fx-padding: 5 14 5 14; -fx-background-radius: 4; -fx-cursor: default;");
            } else {
                enableCloudCompletionCheck.setSelected(false);
                enableCloudCompletionCheck.setDisable(true);
                activationButton.setText("Go to Activation...");
                activationButton.setDisable(false);
                activationButton.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 14 5 14; -fx-background-radius: 4; -fx-cursor: hand;");
            }

            enableAutomaticOnTypingCheck.setSelected(s.isEnableAutomaticCompletionOnTyping());
            enableMultilineCheck.setSelected(s.isEnableMultilineSuggestions());
            syncInlineAndPopupCheck.setSelected(s.isSynchronizeInlineAndPopup());
        } finally {
            suppressEvents = false;
        }
    }

    public void saveToSettings(InlineCompletionSettings s) {
        s.setEnableLocalFullLine(enableLocalFullLineCheck.isSelected());
        if (downloadModelsCombo.getValue() != null) {
            s.setDownloadModelsMode(downloadModelsCombo.getValue());
        }

        for (Map.Entry<String, CheckBox> entry : languageChecks.entrySet()) {
            String lang = entry.getKey();
            s.setLanguageEnabled(lang, entry.getValue().isSelected());

            Hyperlink dl = downloadLinks.get(lang);
            if (dl != null && "Installed".equals(dl.getText())) {
                s.setLanguageDownloaded(lang, true);
            }
        }

        s.setCloudActivated(cloudActivatedState);
        s.setEnableCloudCompletion(enableCloudCompletionCheck.isSelected() && cloudActivatedState);

        s.setEnableAutomaticCompletionOnTyping(enableAutomaticOnTypingCheck.isSelected());
        s.setEnableMultilineSuggestions(enableMultilineCheck.isSelected());
        s.setSynchronizeInlineAndPopup(syncInlineAndPopupCheck.isSelected());
    }

    public boolean isModified() {
        InlineCompletionSettings current = new InlineCompletionSettings();
        saveToSettings(current);
        return current.isModified(InlineCompletionSettings.getInstance());
    }

    public void apply() {
        saveToSettings(InlineCompletionSettings.getInstance());
        InlineCompletionSettings.getInstance().save();
    }

    public void reset() {
        loadFromSettings(InlineCompletionSettings.getInstance());
    }

    // Accessors for testing
    CheckBox getEnableLocalFullLineCheck() { return enableLocalFullLineCheck; }
    ComboBox<DownloadModelsMode> getDownloadModelsCombo() { return downloadModelsCombo; }
    CheckBox getLanguageCheck(String lang) { return languageChecks.get(lang); }
    Hyperlink getDownloadLink(String lang) { return downloadLinks.get(lang); }
    CheckBox getEnableCloudCompletionCheck() { return enableCloudCompletionCheck; }
    Button getActivationButton() { return activationButton; }
    CheckBox getEnableAutomaticOnTypingCheck() { return enableAutomaticOnTypingCheck; }
    CheckBox getEnableMultilineCheck() { return enableMultilineCheck; }
    CheckBox getSyncInlineAndPopupCheck() { return syncInlineAndPopupCheck; }
}