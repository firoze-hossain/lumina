package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager responsible for dynamically managing, configuring, and executing
 * Actions on Save in Lumina IDE without hardcoding.
 */
public class ActionsOnSaveManager {

    private static final String PREF_PREFIX = "actions_on_save.";
    private static volatile ActionsOnSaveManager instance;

    private ActionsOnSaveSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private ActionsOnSaveManager() {
        this.currentSettings = loadSettings();
    }

    public static ActionsOnSaveManager getInstance() {
        if (instance == null) {
            synchronized (ActionsOnSaveManager.class) {
                if (instance == null) {
                    instance = new ActionsOnSaveManager();
                }
            }
        }
        return instance;
    }

    public synchronized ActionsOnSaveSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(ActionsOnSaveSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new ActionsOnSaveSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
    }

    /**
     * Dynamically registers a custom or plugin-contributed Action on Save.
     */
    public synchronized void registerAction(ActionOnSaveItem item) {
        if (item == null || item.getId() == null) return;
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        ActionOnSaveItem existing = currentSettings.findItemById(item.getId());
        if (existing == null) {
            currentSettings.getItems().add(item.clone());
            saveSettings();
            notifyListeners();
        }
    }

    /**
     * Refreshes the dynamic statuses of actions (such as checking if Black formatter,
     * ESLint, Prettier, or Stylelint are installed or enabled).
     */
    public synchronized void refreshDynamicStatuses() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        boolean modified = false;

        // Check Black formatter status
        ActionOnSaveItem blackItem = currentSettings.findItemById("black");
        if (blackItem != null) {
            BlackSettings blackSettings = BlackSettingsManager.getInstance().getSettings();
            boolean isInstalled = BlackSettingsManager.getInstance().isBlackInstalled(blackSettings.getPythonInterpreter());
            if (!isInstalled) {
                blackItem.setSubtext("Black formatter package is not installed on the current interpreter");
                blackItem.setWarning(true);
            } else {
                blackItem.setSubtext("Black formatter is installed and ready");
                blackItem.setWarning(false);
            }
            if (blackItem.isEnabled() != blackSettings.isOnSave()) {
                blackItem.setEnabled(blackSettings.isOnSave());
                modified = true;
            }
        }

        if (modified) {
            saveSettings();
            notifyListeners();
        }
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private ActionsOnSaveSettings loadSettings() {
        ActionsOnSaveSettings settings = new ActionsOnSaveSettings();
        for (ActionOnSaveItem item : settings.getItems()) {
            String key = PREF_PREFIX + item.getId() + ".enabled";
            String savedEnabled = Settings.get(key);
            if (savedEnabled != null) {
                item.setEnabled(Boolean.parseBoolean(savedEnabled));
            }
            String scopeKey = PREF_PREFIX + item.getId() + ".scope";
            String savedScope = Settings.get(scopeKey);
            if (savedScope != null && !savedScope.isBlank()) {
                item.setSelectedScope(savedScope);
            }
            String modeKey = PREF_PREFIX + item.getId() + ".mode";
            String savedMode = Settings.get(modeKey);
            if (savedMode != null && !savedMode.isBlank()) {
                item.setSelectedMode(savedMode);
            }
            String profileKey = PREF_PREFIX + item.getId() + ".profile";
            String savedProfile = Settings.get(profileKey);
            if (savedProfile != null && !savedProfile.isBlank()) {
                item.setSelectedProfile(savedProfile);
            }
        }
        return settings;
    }

    private void saveSettings() {
        if (currentSettings == null) return;
        for (ActionOnSaveItem item : currentSettings.getItems()) {
            Settings.put(PREF_PREFIX + item.getId() + ".enabled", String.valueOf(item.isEnabled()));
            if (item.getSelectedScope() != null) {
                Settings.put(PREF_PREFIX + item.getId() + ".scope", item.getSelectedScope());
            }
            if (item.getSelectedMode() != null) {
                Settings.put(PREF_PREFIX + item.getId() + ".mode", item.getSelectedMode());
            }
            if (item.getSelectedProfile() != null) {
                Settings.put(PREF_PREFIX + item.getId() + ".profile", item.getSelectedProfile());
            }
        }
    }
}
