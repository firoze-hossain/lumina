package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > Ruby settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysRubyPage extends VBox {

    // Comments
    private final CheckBox continueLineCommentsCheck = new CheckBox("Continue line comments on Enter");
    private final CheckBox deleteEmptyLineCommentsCheck = new CheckBox("Delete empty line comments on Enter");
    private final Label shiftEnterHintLabel = new Label("Use Shift+Enter to start a new line and keep the empty comment");

    // Strings
    private final CheckBox startInterpolationOnTypingHashCheck = new CheckBox("Start interpolation on typing '#'");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysRubyPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        buildUi();
        setupListeners();
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 4, 0));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(lbl, line);
        return header;
    }

    private void buildUi() {
        styleCheckBox(continueLineCommentsCheck);
        styleCheckBox(deleteEmptyLineCommentsCheck);
        styleCheckBox(startInterpolationOnTypingHashCheck);

        shiftEnterHintLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        HBox commentsHeader = createSectionHeader("Comments");
        VBox commentsGroup = new VBox(8,
                continueLineCommentsCheck,
                deleteEmptyLineCommentsCheck,
                shiftEnterHintLabel
        );

        HBox stringsHeader = createSectionHeader("Strings");
        VBox stringsGroup = new VBox(8,
                startInterpolationOnTypingHashCheck
        );

        getChildren().addAll(
                commentsHeader, commentsGroup,
                stringsHeader, stringsGroup
        );
    }

    private void setupListeners() {
        continueLineCommentsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        deleteEmptyLineCommentsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        startInterpolationOnTypingHashCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void loadFromSettings(SmartKeysSettings settings) {
        suppressEvents = true;
        try {
            continueLineCommentsCheck.setSelected(settings.isRubyContinueLineCommentsOnEnter());
            deleteEmptyLineCommentsCheck.setSelected(settings.isRubyDeleteEmptyLineCommentsOnEnter());
            startInterpolationOnTypingHashCheck.setSelected(settings.isRubyStartInterpolationOnTypingHash());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return continueLineCommentsCheck.isSelected() != s.isRubyContinueLineCommentsOnEnter()
                || deleteEmptyLineCommentsCheck.isSelected() != s.isRubyDeleteEmptyLineCommentsOnEnter()
                || startInterpolationOnTypingHashCheck.isSelected() != s.isRubyStartInterpolationOnTypingHash();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setRubyContinueLineCommentsOnEnter(continueLineCommentsCheck.isSelected());
        s.setRubyDeleteEmptyLineCommentsOnEnter(deleteEmptyLineCommentsCheck.isSelected());
        s.setRubyStartInterpolationOnTypingHash(startInterpolationOnTypingHashCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    // --- Component Getters ---
    public CheckBox getContinueLineCommentsCheck() { return continueLineCommentsCheck; }
    public CheckBox getDeleteEmptyLineCommentsCheck() { return deleteEmptyLineCommentsCheck; }
    public CheckBox getStartInterpolationOnTypingHashCheck() { return startInterpolationOnTypingHashCheck; }
}
