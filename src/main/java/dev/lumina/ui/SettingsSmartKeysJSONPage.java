package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > JSON settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysJSONPage extends VBox {

    private final CheckBox insertMissingCommaOnEnterCheck = new CheckBox("Insert missing comma on Enter");
    private final CheckBox insertMissingCommaAfterMatchingBracesQuotesCheck = new CheckBox("Insert missing comma after matching braces and quotes");
    private final CheckBox autoManageCommasPastingFragmentsCheck = new CheckBox("Automatically manage commas when pasting JSON fragments");
    private final CheckBox escapeTextOnPasteInStringLiteralsCheck = new CheckBox("Escape text on paste in string literals");
    private final CheckBox autoAddQuotesToPropertyNamesOnColonCheck = new CheckBox("Automatically add quotes to property names when typing ':'");
    private final CheckBox autoAddWhitespaceOnColonAfterPropertyCheck = new CheckBox("Automatically add whitespace when typing ':' after property names");
    private final CheckBox autoMoveColonAfterPropertyNameInsideQuotesCheck = new CheckBox("Automatically move ':' after the property name if typed inside quotes");
    private final CheckBox autoMoveCommaAfterValueInsideQuotesCheck = new CheckBox("Automatically move comma after the property value or array element if inside quotes");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysJSONPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        styleCheckBox(insertMissingCommaOnEnterCheck);
        styleCheckBox(insertMissingCommaAfterMatchingBracesQuotesCheck);
        styleCheckBox(autoManageCommasPastingFragmentsCheck);
        styleCheckBox(escapeTextOnPasteInStringLiteralsCheck);
        styleCheckBox(autoAddQuotesToPropertyNamesOnColonCheck);
        styleCheckBox(autoAddWhitespaceOnColonAfterPropertyCheck);
        styleCheckBox(autoMoveColonAfterPropertyNameInsideQuotesCheck);
        styleCheckBox(autoMoveCommaAfterValueInsideQuotesCheck);

        setupListeners();

        getChildren().addAll(
                insertMissingCommaOnEnterCheck,
                insertMissingCommaAfterMatchingBracesQuotesCheck,
                autoManageCommasPastingFragmentsCheck,
                escapeTextOnPasteInStringLiteralsCheck,
                autoAddQuotesToPropertyNamesOnColonCheck,
                autoAddWhitespaceOnColonAfterPropertyCheck,
                autoMoveColonAfterPropertyNameInsideQuotesCheck,
                autoMoveCommaAfterValueInsideQuotesCheck
        );

        loadFromSettings(SmartKeysSettings.getInstance());
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
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void setupListeners() {
        insertMissingCommaOnEnterCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertMissingCommaAfterMatchingBracesQuotesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoManageCommasPastingFragmentsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        escapeTextOnPasteInStringLiteralsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoAddQuotesToPropertyNamesOnColonCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoAddWhitespaceOnColonAfterPropertyCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoMoveColonAfterPropertyNameInsideQuotesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoMoveCommaAfterValueInsideQuotesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
    }

    public void loadFromSettings(SmartKeysSettings settings) {
        suppressEvents = true;
        try {
            insertMissingCommaOnEnterCheck.setSelected(settings.isJsonInsertMissingCommaOnEnter());
            insertMissingCommaAfterMatchingBracesQuotesCheck.setSelected(settings.isJsonInsertMissingCommaAfterMatchingBracesQuotes());
            autoManageCommasPastingFragmentsCheck.setSelected(settings.isJsonAutoManageCommasPastingFragments());
            escapeTextOnPasteInStringLiteralsCheck.setSelected(settings.isJsonEscapeTextOnPasteInStringLiterals());
            autoAddQuotesToPropertyNamesOnColonCheck.setSelected(settings.isJsonAutoAddQuotesToPropertyNamesOnColon());
            autoAddWhitespaceOnColonAfterPropertyCheck.setSelected(settings.isJsonAutoAddWhitespaceOnColonAfterProperty());
            autoMoveColonAfterPropertyNameInsideQuotesCheck.setSelected(settings.isJsonAutoMoveColonAfterPropertyNameInsideQuotes());
            autoMoveCommaAfterValueInsideQuotesCheck.setSelected(settings.isJsonAutoMoveCommaAfterValueInsideQuotes());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return insertMissingCommaOnEnterCheck.isSelected() != s.isJsonInsertMissingCommaOnEnter()
                || insertMissingCommaAfterMatchingBracesQuotesCheck.isSelected() != s.isJsonInsertMissingCommaAfterMatchingBracesQuotes()
                || autoManageCommasPastingFragmentsCheck.isSelected() != s.isJsonAutoManageCommasPastingFragments()
                || escapeTextOnPasteInStringLiteralsCheck.isSelected() != s.isJsonEscapeTextOnPasteInStringLiterals()
                || autoAddQuotesToPropertyNamesOnColonCheck.isSelected() != s.isJsonAutoAddQuotesToPropertyNamesOnColon()
                || autoAddWhitespaceOnColonAfterPropertyCheck.isSelected() != s.isJsonAutoAddWhitespaceOnColonAfterProperty()
                || autoMoveColonAfterPropertyNameInsideQuotesCheck.isSelected() != s.isJsonAutoMoveColonAfterPropertyNameInsideQuotes()
                || autoMoveCommaAfterValueInsideQuotesCheck.isSelected() != s.isJsonAutoMoveCommaAfterValueInsideQuotes();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setJsonInsertMissingCommaOnEnter(insertMissingCommaOnEnterCheck.isSelected());
        s.setJsonInsertMissingCommaAfterMatchingBracesQuotes(insertMissingCommaAfterMatchingBracesQuotesCheck.isSelected());
        s.setJsonAutoManageCommasPastingFragments(autoManageCommasPastingFragmentsCheck.isSelected());
        s.setJsonEscapeTextOnPasteInStringLiterals(escapeTextOnPasteInStringLiteralsCheck.isSelected());
        s.setJsonAutoAddQuotesToPropertyNamesOnColon(autoAddQuotesToPropertyNamesOnColonCheck.isSelected());
        s.setJsonAutoAddWhitespaceOnColonAfterProperty(autoAddWhitespaceOnColonAfterPropertyCheck.isSelected());
        s.setJsonAutoMoveColonAfterPropertyNameInsideQuotes(autoMoveColonAfterPropertyNameInsideQuotesCheck.isSelected());
        s.setJsonAutoMoveCommaAfterValueInsideQuotes(autoMoveCommaAfterValueInsideQuotesCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    // --- Component Getters ---
    public CheckBox getInsertMissingCommaOnEnterCheck() { return insertMissingCommaOnEnterCheck; }
    public CheckBox getInsertMissingCommaAfterMatchingBracesQuotesCheck() { return insertMissingCommaAfterMatchingBracesQuotesCheck; }
    public CheckBox getAutoManageCommasPastingFragmentsCheck() { return autoManageCommasPastingFragmentsCheck; }
    public CheckBox getEscapeTextOnPasteInStringLiteralsCheck() { return escapeTextOnPasteInStringLiteralsCheck; }
    public CheckBox getAutoAddQuotesToPropertyNamesOnColonCheck() { return autoAddQuotesToPropertyNamesOnColonCheck; }
    public CheckBox getAutoAddWhitespaceOnColonAfterPropertyCheck() { return autoAddWhitespaceOnColonAfterPropertyCheck; }
    public CheckBox getAutoMoveColonAfterPropertyNameInsideQuotesCheck() { return autoMoveColonAfterPropertyNameInsideQuotesCheck; }
    public CheckBox getAutoMoveCommaAfterValueInsideQuotesCheck() { return autoMoveCommaAfterValueInsideQuotesCheck; }
}