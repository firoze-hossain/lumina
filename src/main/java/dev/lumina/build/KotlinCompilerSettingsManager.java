package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving Kotlin Compiler settings in Lumina IDE.
 */
public class KotlinCompilerSettingsManager {

    public static final String KEY_REPORT_WARNINGS = "compiler.kotlin.report.warnings";
    public static final String KEY_VERSION = "compiler.kotlin.version";
    public static final String KEY_LANGUAGE_VERSION = "compiler.kotlin.language.version";
    public static final String KEY_API_VERSION = "compiler.kotlin.api.version";
    public static final String KEY_ADDITIONAL_PARAMS = "compiler.kotlin.additional.params";
    public static final String KEY_KEEP_PROCESS_ALIVE = "compiler.kotlin.keep.process.alive";

    public static final String KEY_JVM_INCREMENTAL = "compiler.kotlin.jvm.incremental";
    public static final String KEY_JVM_TARGET = "compiler.kotlin.jvm.target";

    public static final String KEY_JS_INCREMENTAL = "compiler.kotlin.js.incremental";
    public static final String KEY_JS_SOURCE_MAPS = "compiler.kotlin.js.source.maps";
    public static final String KEY_JS_SOURCE_MAP_PREFIX = "compiler.kotlin.js.source.map.prefix";
    public static final String KEY_JS_EMBED_SOURCE = "compiler.kotlin.js.embed.source";
    public static final String KEY_JS_COPY_RUNTIME = "compiler.kotlin.js.copy.runtime";
    public static final String KEY_JS_DEST_DIR = "compiler.kotlin.js.dest.dir";
    public static final String KEY_JS_MODULE_KIND = "compiler.kotlin.js.module.kind";

    public static final String KEY_SCRIPT_TEMPLATES = "compiler.kotlin.script.templates";
    public static final String KEY_SCRIPT_CLASSPATH = "compiler.kotlin.script.classpath";

    private static volatile KotlinCompilerSettingsManager instance;
    private KotlinCompilerSettings currentSettings;

    private KotlinCompilerSettingsManager() {
        loadSettings();
    }

    public static KotlinCompilerSettingsManager getInstance() {
        if (instance == null) {
            synchronized (KotlinCompilerSettingsManager.class) {
                if (instance == null) {
                    instance = new KotlinCompilerSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized KotlinCompilerSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(KotlinCompilerSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        KotlinCompilerSettings s = new KotlinCompilerSettings();

        String repWarn = Settings.get(KEY_REPORT_WARNINGS);
        if (repWarn != null) s.setReportCompilerWarnings(Boolean.parseBoolean(repWarn));

        String ver = Settings.get(KEY_VERSION);
        if (ver != null && !ver.isBlank()) s.setKotlinCompilerVersion(ver);

        String langVer = Settings.get(KEY_LANGUAGE_VERSION);
        if (langVer != null && !langVer.isBlank()) s.setLanguageVersion(langVer);

        String apiVer = Settings.get(KEY_API_VERSION);
        if (apiVer != null && !apiVer.isBlank()) s.setApiVersion(apiVer);

        String addParams = Settings.get(KEY_ADDITIONAL_PARAMS);
        if (addParams != null) s.setAdditionalCommandLineParameters(addParams);

        String keepAlive = Settings.get(KEY_KEEP_PROCESS_ALIVE);
        if (keepAlive != null) s.setKeepCompilerProcessAlive(Boolean.parseBoolean(keepAlive));

        String jvmInc = Settings.get(KEY_JVM_INCREMENTAL);
        if (jvmInc != null) s.setJvmEnableIncrementalCompilation(Boolean.parseBoolean(jvmInc));

        String jvmTgt = Settings.get(KEY_JVM_TARGET);
        if (jvmTgt != null && !jvmTgt.isBlank()) s.setTargetJvmVersion(jvmTgt);

        String jsInc = Settings.get(KEY_JS_INCREMENTAL);
        if (jsInc != null) s.setJsEnableIncrementalCompilation(Boolean.parseBoolean(jsInc));

        String jsMaps = Settings.get(KEY_JS_SOURCE_MAPS);
        if (jsMaps != null) s.setGenerateSourceMaps(Boolean.parseBoolean(jsMaps));

        String jsPrefix = Settings.get(KEY_JS_SOURCE_MAP_PREFIX);
        if (jsPrefix != null) s.setSourceMapPrefix(jsPrefix);

        String jsEmbed = Settings.get(KEY_JS_EMBED_SOURCE);
        if (jsEmbed != null && !jsEmbed.isBlank()) s.setEmbedSourceCodeIntoSourceMap(jsEmbed);

        String jsCopy = Settings.get(KEY_JS_COPY_RUNTIME);
        if (jsCopy != null) s.setCopyLibraryRuntimeFiles(Boolean.parseBoolean(jsCopy));

        String jsDest = Settings.get(KEY_JS_DEST_DIR);
        if (jsDest != null && !jsDest.isBlank()) s.setDestinationDirectory(jsDest);

        String jsKind = Settings.get(KEY_JS_MODULE_KIND);
        if (jsKind != null && !jsKind.isBlank()) s.setModuleKind(jsKind);

        String scTemplates = Settings.get(KEY_SCRIPT_TEMPLATES);
        if (scTemplates != null) s.setScriptDefinitionTemplateClasses(scTemplates);

        String scCp = Settings.get(KEY_SCRIPT_CLASSPATH);
        if (scCp != null) s.setScriptClasspath(scCp);

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_REPORT_WARNINGS, String.valueOf(currentSettings.isReportCompilerWarnings()));
        Settings.set(KEY_VERSION, currentSettings.getKotlinCompilerVersion());
        Settings.set(KEY_LANGUAGE_VERSION, currentSettings.getLanguageVersion());
        Settings.set(KEY_API_VERSION, currentSettings.getApiVersion());
        Settings.set(KEY_ADDITIONAL_PARAMS, currentSettings.getAdditionalCommandLineParameters());
        Settings.set(KEY_KEEP_PROCESS_ALIVE, String.valueOf(currentSettings.isKeepCompilerProcessAlive()));

        Settings.set(KEY_JVM_INCREMENTAL, String.valueOf(currentSettings.isJvmEnableIncrementalCompilation()));
        Settings.set(KEY_JVM_TARGET, currentSettings.getTargetJvmVersion());

        Settings.set(KEY_JS_INCREMENTAL, String.valueOf(currentSettings.isJsEnableIncrementalCompilation()));
        Settings.set(KEY_JS_SOURCE_MAPS, String.valueOf(currentSettings.isGenerateSourceMaps()));
        Settings.set(KEY_JS_SOURCE_MAP_PREFIX, currentSettings.getSourceMapPrefix());
        Settings.set(KEY_JS_EMBED_SOURCE, currentSettings.getEmbedSourceCodeIntoSourceMap());
        Settings.set(KEY_JS_COPY_RUNTIME, String.valueOf(currentSettings.isCopyLibraryRuntimeFiles()));
        Settings.set(KEY_JS_DEST_DIR, currentSettings.getDestinationDirectory());
        Settings.set(KEY_JS_MODULE_KIND, currentSettings.getModuleKind());

        Settings.set(KEY_SCRIPT_TEMPLATES, currentSettings.getScriptDefinitionTemplateClasses());
        Settings.set(KEY_SCRIPT_CLASSPATH, currentSettings.getScriptClasspath());
    }
}
