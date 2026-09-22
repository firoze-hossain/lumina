package dev.lumina.presentation;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Central singleton managing Presentation Assistant settings.
 * Strictly matches media_1790046850265.png, media_1790046850445.png,
 * and media_1790046851065.png.
 */
public class PresentationAssistantManager {

    private static final PresentationAssistantManager INSTANCE = new PresentationAssistantManager();

    private final Preferences prefs = Preferences.userNodeForPackage(PresentationAssistantManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    private boolean showActionNamesAndShortcuts = false;
    private String popupSize = "Medium";
    private int displayDurationSeconds = 4;
    private String position = "Bottom Center";

    private String mainKeymap = "macOS";
    private String mainLabel = "macOS";

    private boolean additionalKeymapEnabled = false;
    private String additionalKeymap = "Windows";
    private String additionalLabel = "Win/Linux";

    // Keymap catalog strictly adhering to media_1790046851065.png with brand isolation
    public static final List<String> AVAILABLE_KEYMAPS = List.of(
            "macOS",
            "Default Classic",
            "macOS System Shortcuts",
            "Emacs",
            "Sublime Text",
            "Sublime Text (macOS)",
            "NetBeans",
            "Visual Studio",
            "Windows",
            "GNOME"
    );

    public static final List<String> AVAILABLE_POSITIONS = List.of(
            "Top Left",
            "Top Center",
            "Top Right",
            "Bottom Left",
            "Bottom Center",
            "Bottom Right"
    );

    public static final List<String> AVAILABLE_SIZES = List.of(
            "Small",
            "Medium",
            "Large"
    );

    private PresentationAssistantManager() {
        loadPreferences();
    }

    public static PresentationAssistantManager getInstance() {
        return INSTANCE;
    }

    public synchronized boolean isShowActionNamesAndShortcuts() {
        return showActionNamesAndShortcuts;
    }

    public synchronized void setShowActionNamesAndShortcuts(boolean value) {
        this.showActionNamesAndShortcuts = value;
        savePreferences();
        notifyListeners();
    }

    public synchronized String getPopupSize() {
        return popupSize;
    }

    public synchronized void setPopupSize(String value) {
        if (value != null && AVAILABLE_SIZES.contains(value)) {
            this.popupSize = value;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized int getDisplayDurationSeconds() {
        return displayDurationSeconds;
    }

    public synchronized void setDisplayDurationSeconds(int value) {
        this.displayDurationSeconds = Math.max(1, value);
        savePreferences();
        notifyListeners();
    }

    public synchronized String getPosition() {
        return position;
    }

    public synchronized void setPosition(String value) {
        if (value != null && AVAILABLE_POSITIONS.contains(value)) {
            this.position = value;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized String getMainKeymap() {
        return mainKeymap;
    }

    public synchronized void setMainKeymap(String value) {
        if (value != null) {
            this.mainKeymap = value;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized String getMainLabel() {
        return mainLabel;
    }

    public synchronized void setMainLabel(String value) {
        this.mainLabel = value != null ? value : "";
        savePreferences();
        notifyListeners();
    }

    public synchronized boolean isAdditionalKeymapEnabled() {
        return additionalKeymapEnabled;
    }

    public synchronized void setAdditionalKeymapEnabled(boolean value) {
        this.additionalKeymapEnabled = value;
        savePreferences();
        notifyListeners();
    }

    public synchronized String getAdditionalKeymap() {
        return additionalKeymap;
    }

    public synchronized void setAdditionalKeymap(String value) {
        if (value != null) {
            this.additionalKeymap = value;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized String getAdditionalLabel() {
        return additionalLabel;
    }

    public synchronized void setAdditionalLabel(String value) {
        this.additionalLabel = value != null ? value : "";
        savePreferences();
        notifyListeners();
    }

    public synchronized void revertToDefaults() {
        showActionNamesAndShortcuts = false;
        popupSize = "Medium";
        displayDurationSeconds = 4;
        position = "Bottom Center";
        mainKeymap = "macOS";
        mainLabel = "macOS";
        additionalKeymapEnabled = false;
        additionalKeymap = "Windows";
        additionalLabel = "Win/Linux";
        savePreferences();
        notifyListeners();
    }

    public synchronized void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable l : new ArrayList<>(listeners)) {
            try {
                l.run();
            } catch (Exception ignored) {}
        }
    }

    private void loadPreferences() {
        showActionNamesAndShortcuts = prefs.getBoolean("pa_show_actions", false);
        popupSize = prefs.get("pa_popup_size", "Medium");
        displayDurationSeconds = prefs.getInt("pa_display_duration", 4);
        position = prefs.get("pa_position", "Bottom Center");
        mainKeymap = prefs.get("pa_main_keymap", "macOS");
        mainLabel = prefs.get("pa_main_label", "macOS");
        additionalKeymapEnabled = prefs.getBoolean("pa_add_enabled", false);
        additionalKeymap = prefs.get("pa_add_keymap", "Windows");
        additionalLabel = prefs.get("pa_add_label", "Win/Linux");
    }

    private void savePreferences() {
        prefs.putBoolean("pa_show_actions", showActionNamesAndShortcuts);
        prefs.put("pa_popup_size", popupSize);
        prefs.putInt("pa_display_duration", displayDurationSeconds);
        prefs.put("pa_position", position);
        prefs.put("pa_main_keymap", mainKeymap);
        prefs.put("pa_main_label", mainLabel);
        prefs.putBoolean("pa_add_enabled", additionalKeymapEnabled);
        prefs.put("pa_add_keymap", additionalKeymap);
        prefs.put("pa_add_label", additionalLabel);
    }
}
