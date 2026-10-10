package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Tools > Diff & Merge settings in Lumina IDE.
 */
public class DiffMergeSettingsManager {

    private static final String KEY_PREFIX = "tools.diffmerge.";
    private static final String KEY_CONTEXT_LINES = KEY_PREFIX + "context_lines";
    private static final String KEY_NEXT_FILE = KEY_PREFIX + "go_to_next_file";
    private static final String KEY_NAV_HISTORY = KEY_PREFIX + "navigation_history";
    private static final String KEY_AUTO_APPLY = KEY_PREFIX + "auto_apply_non_conflicting";
    private static final String KEY_AUTO_RESOLVE_IMP = KEY_PREFIX + "auto_resolve_imports";
    private static final String KEY_HIGHLIGHT_GUTTER = KEY_PREFIX + "highlight_modified_gutter";

    private static volatile DiffMergeSettingsManager instance;

    private DiffMergeSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DiffMergeSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DiffMergeSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DiffMergeSettingsManager.class) {
                if (instance == null) {
                    instance = new DiffMergeSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DiffMergeSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DiffMergeSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DiffMergeSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
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

    private DiffMergeSettings loadSettings() {
        DiffMergeSettings s = new DiffMergeSettings();

        String ctx = Settings.get(KEY_CONTEXT_LINES);
        if (ctx != null) {
            try {
                s.setContextLines(Integer.parseInt(ctx));
            } catch (NumberFormatException ignored) {}
        }

        String nextFile = Settings.get(KEY_NEXT_FILE);
        if (nextFile != null) s.setGoToNextFileAfterLastChange(Boolean.parseBoolean(nextFile));

        String navHist = Settings.get(KEY_NAV_HISTORY);
        if (navHist != null) s.setNavigationHistoryPolicy(navHist);

        String autoApply = Settings.get(KEY_AUTO_APPLY);
        if (autoApply != null) s.setAutoApplyNonConflictingChanges(Boolean.parseBoolean(autoApply));

        String autoImp = Settings.get(KEY_AUTO_RESOLVE_IMP);
        if (autoImp != null) s.setAutoResolveConflictsInImports(Boolean.parseBoolean(autoImp));

        String gutter = Settings.get(KEY_HIGHLIGHT_GUTTER);
        if (gutter != null) s.setHighlightModifiedLinesInGutter(Boolean.parseBoolean(gutter));

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_CONTEXT_LINES, String.valueOf(currentSettings.getContextLines()));
        Settings.put(KEY_NEXT_FILE, String.valueOf(currentSettings.isGoToNextFileAfterLastChange()));
        Settings.put(KEY_NAV_HISTORY, currentSettings.getNavigationHistoryPolicy());
        Settings.put(KEY_AUTO_APPLY, String.valueOf(currentSettings.isAutoApplyNonConflictingChanges()));
        Settings.put(KEY_AUTO_RESOLVE_IMP, String.valueOf(currentSettings.isAutoResolveConflictsInImports()));
        Settings.put(KEY_HIGHLIGHT_GUTTER, String.valueOf(currentSettings.isHighlightModifiedLinesInGutter()));
    }
}
