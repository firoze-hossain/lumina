package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving Scala Bytecode Indices settings in Lumina IDE.
 */
public class ScalaBytecodeIndicesSettingsManager {

    public static final String KEY_INDEX_CLASS_FILES = "compiler.scala.bytecode.indices.index.class.files";
    public static final String KEY_IMPLICIT_DEFINITIONS = "compiler.scala.bytecode.indices.implicit.definitions";
    public static final String KEY_APPLY_UNAPPLY = "compiler.scala.bytecode.indices.apply.unapply";
    public static final String KEY_SAM_TYPES = "compiler.scala.bytecode.indices.sam.types";
    public static final String KEY_FOR_COMPREHENSIONS = "compiler.scala.bytecode.indices.for.comprehensions";

    private static volatile ScalaBytecodeIndicesSettingsManager instance;
    private ScalaBytecodeIndicesSettings currentSettings;

    private ScalaBytecodeIndicesSettingsManager() {
        loadSettings();
    }

    public static ScalaBytecodeIndicesSettingsManager getInstance() {
        if (instance == null) {
            synchronized (ScalaBytecodeIndicesSettingsManager.class) {
                if (instance == null) {
                    instance = new ScalaBytecodeIndicesSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized ScalaBytecodeIndicesSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(ScalaBytecodeIndicesSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        ScalaBytecodeIndicesSettings s = new ScalaBytecodeIndicesSettings();

        String idx = Settings.get(KEY_INDEX_CLASS_FILES);
        if (idx != null) s.setIndexClassFiles(Boolean.parseBoolean(idx));

        String impl = Settings.get(KEY_IMPLICIT_DEFINITIONS);
        if (impl != null) s.setImplicitDefinitions(Boolean.parseBoolean(impl));

        String apply = Settings.get(KEY_APPLY_UNAPPLY);
        if (apply != null) s.setApplyUnapplyMethods(Boolean.parseBoolean(apply));

        String sam = Settings.get(KEY_SAM_TYPES);
        if (sam != null) s.setSamTypes(Boolean.parseBoolean(sam));

        String forComp = Settings.get(KEY_FOR_COMPREHENSIONS);
        if (forComp != null) s.setForComprehensionMethods(Boolean.parseBoolean(forComp));

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_INDEX_CLASS_FILES, String.valueOf(currentSettings.isIndexClassFiles()));
        Settings.set(KEY_IMPLICIT_DEFINITIONS, String.valueOf(currentSettings.isImplicitDefinitions()));
        Settings.set(KEY_APPLY_UNAPPLY, String.valueOf(currentSettings.isApplyUnapplyMethods()));
        Settings.set(KEY_SAM_TYPES, String.valueOf(currentSettings.isSamTypes()));
        Settings.set(KEY_FOR_COMPREHENSIONS, String.valueOf(currentSettings.isForComprehensionMethods()));
    }
}
