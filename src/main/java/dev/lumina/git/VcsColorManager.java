package dev.lumina.git;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Manages VCS File Status Colors matching IntelliJ IDEA Image 5.
 * Tracks default and customized colors for all 25 IntelliJ VCS statuses.
 * Backed by Java Preferences for dynamic persistence.
 */
public class VcsColorManager {

    public static final class StatusColorDef {
        private final String key;
        private final String defaultHex;
        private final String description;

        public StatusColorDef(String key, String defaultHex, String description) {
            this.key = key;
            this.defaultHex = defaultHex.toUpperCase().replace("#", "");
            this.description = description;
        }

        public String getKey() { return key; }
        public String getDefaultHex() { return defaultHex; }
        public String getDescription() { return description; }
    }

    // The 25 canonical IntelliJ File Statuses in order matching Image 5
    private static final List<StatusColorDef> ALL_STATUS_DEFS = List.of(
            new StatusColorDef("Added", "73BD79", "New file added to VCS"),
            new StatusColorDef("Added in not active changelist", "73BD79", "Added file in non-active changelist"),
            new StatusColorDef("Changelist conflict", "D5756C", "Conflicting changes across changelists"),
            new StatusColorDef("Copied", "73BD79", "File copied from versioned file"),
            new StatusColorDef("Deleted", "787878", "File deleted locally"),
            new StatusColorDef("Deleted from file system", "787878", "File removed from disk"),
            new StatusColorDef("External (svn)", "73BD79", "External Subversion item"),
            new StatusColorDef("Have changed descendants", "56A8F5", "Directory containing modified files"),
            new StatusColorDef("Have immediate changed children", "56A8F5", "Directory containing immediate modified files"),
            new StatusColorDef("Hijacked", "DFE1E5", "Hijacked file in Perforce"),
            new StatusColorDef("Ignored", "8C7C54", "File matching .gitignore or ignored list"),
            new StatusColorDef("Merged", "9973B8", "File successfully merged"),
            new StatusColorDef("Merged with conflicts", "D5756C", "Merge conflict in text"),
            new StatusColorDef("Merged with property conflicts", "D5756C", "Merge conflict in properties"),
            new StatusColorDef("Merged with text and property conflicts", "D5756C", "Merge conflict in both"),
            new StatusColorDef("Modified", "56A8F5", "File edited after commit"),
            new StatusColorDef("Modified in not active changelist", "56A8F5", "Modified file in inactive changelist"),
            new StatusColorDef("Obsolete", "787878", "Obsolete file version"),
            new StatusColorDef("Obstructed (svn)", "8C7C54", "Obstructed file in Subversion"),
            new StatusColorDef("Renamed", "56A8F5", "File renamed in VCS"),
            new StatusColorDef("Replaced (svn)", "73BD79", "Replaced file in Subversion"),
            new StatusColorDef("Suppressed", "73BD79", "Suppressed file status"),
            new StatusColorDef("Switched", "787878", "Switched branch file"),
            new StatusColorDef("Unknown", "D5756C", "Unversioned / untracked file"),
            new StatusColorDef("Up to date", "DFE1E5", "Committed up-to-date file")
    );

    private static final VcsColorManager INSTANCE = new VcsColorManager();

    private final Preferences prefs = Preferences.userNodeForPackage(VcsColorManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    private final Map<String, String> customColors = new HashMap<>();

    private VcsColorManager() {
        loadPreferences();
    }

    public static VcsColorManager getInstance() {
        return INSTANCE;
    }

    public List<StatusColorDef> getAllStatusDefinitions() {
        return ALL_STATUS_DEFS;
    }

    public synchronized String getColorHex(String statusKey) {
        if (statusKey == null) return "DFE1E5";
        String custom = customColors.get(statusKey);
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        for (StatusColorDef def : ALL_STATUS_DEFS) {
            if (def.getKey().equalsIgnoreCase(statusKey)) {
                return def.getDefaultHex();
            }
        }
        return "DFE1E5";
    }

    public synchronized String getColorHex(GitFileStatus status) {
        if (status == null) return "#DFE1E5";
        String key = switch (status) {
            case ADDED -> "Added";
            case MODIFIED -> "Modified";
            case DELETED -> "Deleted";
            case UNTRACKED -> "Unknown";
            case IGNORED -> "Ignored";
            default -> "Up to date";
        };
        if (isCustomized(key)) {
            return "#" + getColorHex(key);
        }
        return status.getOriginalColorHex();
    }

    public synchronized void setColor(String statusKey, String hexCode) {
        if (statusKey == null || hexCode == null) return;
        String clean = hexCode.toUpperCase().replace("#", "").trim();
        customColors.put(statusKey, clean);
        savePreferences();
        notifyListeners();
    }

    public synchronized void restoreDefault(String statusKey) {
        if (statusKey != null && customColors.containsKey(statusKey)) {
            customColors.remove(statusKey);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isCustomized(String statusKey) {
        if (statusKey == null) return false;
        String custom = customColors.get(statusKey);
        if (custom == null) return false;
        for (StatusColorDef def : ALL_STATUS_DEFS) {
            if (def.getKey().equalsIgnoreCase(statusKey)) {
                return !def.getDefaultHex().equalsIgnoreCase(custom);
            }
        }
        return true;
    }

    public synchronized void revertAllToDefaults() {
        customColors.clear();
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
        customColors.clear();
        for (StatusColorDef def : ALL_STATUS_DEFS) {
            String saved = prefs.get("vcs_color_" + def.getKey().replace(" ", "_"), "");
            if (!saved.isBlank()) {
                customColors.put(def.getKey(), saved.toUpperCase().replace("#", ""));
            }
        }
    }

    private void savePreferences() {
        for (StatusColorDef def : ALL_STATUS_DEFS) {
            String key = "vcs_color_" + def.getKey().replace(" ", "_");
            String custom = customColors.get(def.getKey());
            if (custom != null && !custom.isBlank()) {
                prefs.put(key, custom);
            } else {
                prefs.remove(key);
            }
        }
    }
}
