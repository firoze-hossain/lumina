package dev.lumina.build;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager responsible for loading and saving Compiler Excludes configuration.
 */
public class CompilerExcludesSettingsManager {

    public static final String KEY_COMPILER_EXCLUDES = "compiler.excludes.entries";

    private static volatile CompilerExcludesSettingsManager instance;
    private CompilerExcludesSettings currentSettings;

    private CompilerExcludesSettingsManager() {
        loadSettings();
    }

    public static CompilerExcludesSettingsManager getInstance() {
        if (instance == null) {
            synchronized (CompilerExcludesSettingsManager.class) {
                if (instance == null) {
                    instance = new CompilerExcludesSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized CompilerExcludesSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(CompilerExcludesSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        CompilerExcludesSettings s = new CompilerExcludesSettings();
        String raw = Settings.get(KEY_COMPILER_EXCLUDES);
        if (raw != null && !raw.isBlank()) {
            List<CompilerExcludeEntry> list = new ArrayList<>();
            for (String item : raw.split(";;")) {
                if (item.isBlank()) continue;
                String[] parts = item.split("\\|\\|", -1);
                if (parts.length >= 2) {
                    list.add(new CompilerExcludeEntry(parts[0], Boolean.parseBoolean(parts[1])));
                } else if (parts.length == 1) {
                    list.add(new CompilerExcludeEntry(parts[0], true));
                }
            }
            s.setEntries(list);
        }
        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        StringBuilder sb = new StringBuilder();
        for (CompilerExcludeEntry e : currentSettings.getEntries()) {
            if (!sb.isEmpty()) sb.append(";;");
            sb.append(e.getPath()).append("||").append(e.isRecursive());
        }
        Settings.set(KEY_COMPILER_EXCLUDES, sb.toString());
    }
}
