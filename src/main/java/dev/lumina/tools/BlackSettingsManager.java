package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration and interpreter discovery manager for the Black code formatter.
 */
public class BlackSettingsManager {

    private static final String KEY_EXECUTION_MODE = "tools.black.execution_mode";
    private static final String KEY_INTERPRETER = "tools.black.python_interpreter";
    private static final String KEY_ON_CODE_REFORMAT = "tools.black.on_code_reformat";
    private static final String KEY_ON_SAVE = "tools.black.on_save";
    private static final String KEY_ARGUMENTS = "tools.black.arguments";

    private static volatile BlackSettingsManager instance;

    private BlackSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private BlackSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static BlackSettingsManager getInstance() {
        if (instance == null) {
            synchronized (BlackSettingsManager.class) {
                if (instance == null) {
                    instance = new BlackSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized BlackSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(BlackSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new BlackSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        syncWithActionsOnSave();
        notifyListeners();
    }

    /**
     * Dynamically discovers Python interpreters across the operating system and project directory.
     */
    public List<String> discoverPythonInterpreters() {
        Set<String> interpreters = new LinkedHashSet<>();
        interpreters.add(BlackSettings.NO_INTERPRETER);

        // Project virtualenvs
        String userDir = System.getProperty("user.dir", ".");
        String[] localCandidates = {
                userDir + "/.venv/bin/python",
                userDir + "/.venv/bin/python3",
                userDir + "/venv/bin/python",
                userDir + "/venv/bin/python3"
        };
        for (String c : localCandidates) {
            File f = new File(c);
            if (f.exists() && f.canExecute()) {
                interpreters.add(f.getAbsolutePath());
            }
        }

        // Standard system interpreter locations
        String[] systemCandidates = {
                "/opt/homebrew/bin/python3",
                "/usr/local/bin/python3",
                "/usr/bin/python3",
                "/usr/bin/python"
        };
        for (String c : systemCandidates) {
            File f = new File(c);
            if (f.exists() && f.canExecute()) {
                interpreters.add(f.getAbsolutePath());
            }
        }

        // Search PATH environment variable
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File py3 = new File(dir, "python3");
                if (py3.exists() && py3.canExecute()) {
                    interpreters.add(py3.getAbsolutePath());
                }
            }
        }

        return new ArrayList<>(interpreters);
    }

    /**
     * Checks if the Black formatter package is installed and accessible for the given interpreter.
     */
    public boolean isBlackInstalled(String interpreter) {
        if (interpreter == null || interpreter.isBlank() || BlackSettings.NO_INTERPRETER.equals(interpreter)) {
            return false;
        }
        File interpFile = new File(interpreter);
        if (!interpFile.exists()) {
            return false;
        }

        // Check for sibling 'black' executable in the same bin directory
        File binDir = interpFile.getParentFile();
        if (binDir != null) {
            File blackBin = new File(binDir, "black");
            if (blackBin.exists() && blackBin.canExecute()) {
                return true;
            }
        }

        // Check for 'black' in PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File b = new File(dir, "black");
                if (b.exists() && b.canExecute()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void syncWithActionsOnSave() {
        if (currentSettings != null) {
            ActionsOnSaveSettings aosSettings = ActionsOnSaveManager.getInstance().getSettings();
            ActionOnSaveItem blackItem = aosSettings.findItemById("black");
            if (blackItem != null) {
                blackItem.setEnabled(currentSettings.isOnSave());
                ActionsOnSaveManager.getInstance().setSettings(aosSettings);
            }
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

    private BlackSettings loadSettings() {
        BlackSettings settings = new BlackSettings();
        String mode = Settings.get(KEY_EXECUTION_MODE);
        if (mode != null && !mode.isBlank()) {
            settings.setExecutionMode(mode);
        }
        String interp = Settings.get(KEY_INTERPRETER);
        if (interp != null && !interp.isBlank()) {
            settings.setPythonInterpreter(interp);
        }
        String onReformat = Settings.get(KEY_ON_CODE_REFORMAT);
        if (onReformat != null) {
            settings.setOnCodeReformat(Boolean.parseBoolean(onReformat));
        }
        String onSave = Settings.get(KEY_ON_SAVE);
        if (onSave != null) {
            settings.setOnSave(Boolean.parseBoolean(onSave));
        }
        String args = Settings.get(KEY_ARGUMENTS);
        if (args != null) {
            settings.setArguments(args);
        }
        return settings;
    }

    private void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_EXECUTION_MODE, currentSettings.getExecutionMode());
        Settings.put(KEY_INTERPRETER, currentSettings.getPythonInterpreter());
        Settings.put(KEY_ON_CODE_REFORMAT, String.valueOf(currentSettings.isOnCodeReformat()));
        Settings.put(KEY_ON_SAVE, String.valueOf(currentSettings.isOnSave()));
        Settings.put(KEY_ARGUMENTS, currentSettings.getArguments());
    }
}
