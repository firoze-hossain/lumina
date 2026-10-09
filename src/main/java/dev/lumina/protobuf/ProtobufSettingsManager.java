package dev.lumina.protobuf;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Protocol Buffers settings in Lumina IDE.
 * Handles persistence, default restoration, and state synchronization.
 */
public class ProtobufSettingsManager {

    public static final String KEY_PROTO_APPLY_THIRD_PARTY = "protobuf.apply.third.party.configs";
    public static final String KEY_PROTO_INCLUDE_STD_DIRS = "protobuf.include.standard.proto.dirs";
    public static final String KEY_PROTO_INCLUDE_CONTENT_ROOTS = "protobuf.include.project.content.roots";
    public static final String KEY_PROTO_SEARCH_IN_INDEXES = "protobuf.search.in.indexes";
    public static final String KEY_PROTO_INCLUDE_WELL_KNOWN = "protobuf.include.bundled.well.known";
    public static final String KEY_PROTO_DESCRIPTOR_PATH = "protobuf.descriptor.path";
    public static final String KEY_PROTO_WARN_MISSING_SCHEMA = "protobuf.text.format.warn.missing.schema";
    public static final String KEY_PROTO_CUSTOM_IMPORT_PATHS = "protobuf.custom.import.paths";

    private static ProtobufSettingsManager instance;

    private ProtobufSettings currentSettings = new ProtobufSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private ProtobufSettingsManager() {
        loadSettings();
    }

    public static synchronized ProtobufSettingsManager getInstance() {
        if (instance == null) {
            instance = new ProtobufSettingsManager();
        }
        return instance;
    }

    public synchronized ProtobufSettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(ProtobufSettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new ProtobufSettings();
        saveSettings();
        notifyListeners();
    }

    public synchronized void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public synchronized void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }

    public synchronized void saveSettings() {
        Settings.put(KEY_PROTO_APPLY_THIRD_PARTY, String.valueOf(currentSettings.isApplyThirdPartyConfigurations()));
        Settings.put(KEY_PROTO_INCLUDE_STD_DIRS, String.valueOf(currentSettings.isIncludeStandardProtoDirectories()));
        Settings.put(KEY_PROTO_INCLUDE_CONTENT_ROOTS, String.valueOf(currentSettings.isIncludeProjectContentRoots()));
        Settings.put(KEY_PROTO_SEARCH_IN_INDEXES, String.valueOf(currentSettings.isSearchForImportedFilesInIndexes()));
        Settings.put(KEY_PROTO_INCLUDE_WELL_KNOWN, String.valueOf(currentSettings.isIncludeBundledWellKnownProtoFiles()));
        Settings.put(KEY_PROTO_DESCRIPTOR_PATH, currentSettings.getDescriptorPath());
        Settings.put(KEY_PROTO_WARN_MISSING_SCHEMA, String.valueOf(currentSettings.isWarnAboutMissingSchemaAssociations()));

        // Serialize custom import paths (non-system entries)
        StringBuilder sb = new StringBuilder();
        for (ProtobufImportPath p : currentSettings.getImportPaths()) {
            if (!p.isSystem()) {
                if (sb.length() > 0) sb.append(";;");
                sb.append(p.getLocation()).append("::").append(p.getPrefix());
            }
        }
        Settings.put(KEY_PROTO_CUSTOM_IMPORT_PATHS, sb.toString());
    }

    public synchronized void loadSettings() {
        ProtobufSettings s = new ProtobufSettings();

        String applyThird = Settings.get(KEY_PROTO_APPLY_THIRD_PARTY);
        if (applyThird != null) s.setApplyThirdPartyConfigurations(Boolean.parseBoolean(applyThird));

        String incStd = Settings.get(KEY_PROTO_INCLUDE_STD_DIRS);
        if (incStd != null) s.setIncludeStandardProtoDirectories(Boolean.parseBoolean(incStd));

        String incRoots = Settings.get(KEY_PROTO_INCLUDE_CONTENT_ROOTS);
        if (incRoots != null) s.setIncludeProjectContentRoots(Boolean.parseBoolean(incRoots));

        String searchIdx = Settings.get(KEY_PROTO_SEARCH_IN_INDEXES);
        if (searchIdx != null) s.setSearchForImportedFilesInIndexes(Boolean.parseBoolean(searchIdx));

        String wellKnown = Settings.get(KEY_PROTO_INCLUDE_WELL_KNOWN);
        if (wellKnown != null) s.setIncludeBundledWellKnownProtoFiles(Boolean.parseBoolean(wellKnown));

        String descPath = Settings.get(KEY_PROTO_DESCRIPTOR_PATH);
        if (descPath != null) s.setDescriptorPath(descPath);

        String warnSchema = Settings.get(KEY_PROTO_WARN_MISSING_SCHEMA);
        if (warnSchema != null) s.setWarnAboutMissingSchemaAssociations(Boolean.parseBoolean(warnSchema));

        String customPathsStr = Settings.get(KEY_PROTO_CUSTOM_IMPORT_PATHS);
        if (customPathsStr != null && !customPathsStr.isBlank()) {
            List<ProtobufImportPath> list = ProtobufSettings.createDefaultImportPaths();
            String[] entries = customPathsStr.split(";;");
            for (String entry : entries) {
                if (!entry.isBlank()) {
                    String[] parts = entry.split("::", 2);
                    String loc = parts[0];
                    String pfx = parts.length > 1 ? parts[1] : "";
                    list.add(new ProtobufImportPath(loc, pfx, false));
                }
            }
            s.setImportPaths(list);
        }

        this.currentSettings = s;
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new ProtobufSettings();
        saveSettings();
        notifyListeners();
    }
}
