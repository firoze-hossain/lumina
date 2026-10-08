package dev.lumina.build;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager responsible for loading and saving Java Compiler settings in Lumina IDE.
 */
public class JavaCompilerSettingsManager {

    public static final String KEY_USE_COMPILER = "compiler.java.use.compiler";
    public static final String KEY_USE_RELEASE_OPTION = "compiler.java.use.release.option";
    public static final String KEY_PROJECT_BYTECODE_VERSION = "compiler.java.project.bytecode.version";
    public static final String KEY_PER_MODULE_BYTECODE_VERSIONS = "compiler.java.per.module.bytecode.versions";
    public static final String KEY_USE_MODULE_TARGET_JDK = "compiler.java.use.module.target.jdk";
    public static final String KEY_GENERATE_DEBUGGING_INFO = "compiler.java.generate.debugging.info";
    public static final String KEY_REPORT_DEPRECATED = "compiler.java.report.deprecated";
    public static final String KEY_GENERATE_NO_WARNINGS = "compiler.java.generate.no.warnings";
    public static final String KEY_ADDITIONAL_PARAMS = "compiler.java.additional.params";
    public static final String KEY_PER_MODULE_COMPILER_PARAMS = "compiler.java.per.module.compiler.parameters";

    private static volatile JavaCompilerSettingsManager instance;
    private JavaCompilerSettings currentSettings;

    private JavaCompilerSettingsManager() {
        loadSettings();
    }

    public static JavaCompilerSettingsManager getInstance() {
        if (instance == null) {
            synchronized (JavaCompilerSettingsManager.class) {
                if (instance == null) {
                    instance = new JavaCompilerSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized JavaCompilerSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(JavaCompilerSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        JavaCompilerSettings s = new JavaCompilerSettings();

        String compiler = Settings.get(KEY_USE_COMPILER);
        if (compiler != null && !compiler.isBlank()) s.setUseCompiler(compiler);

        String release = Settings.get(KEY_USE_RELEASE_OPTION);
        if (release != null) s.setUseReleaseOption(Boolean.parseBoolean(release));

        String bytecodeVer = Settings.get(KEY_PROJECT_BYTECODE_VERSION);
        if (bytecodeVer != null && !bytecodeVer.isBlank()) s.setProjectBytecodeVersion(bytecodeVer);

        String moduleBytecode = Settings.get(KEY_PER_MODULE_BYTECODE_VERSIONS);
        if (moduleBytecode != null && !moduleBytecode.isBlank()) {
            List<JavaCompilerSettings.ModuleBytecodeVersion> list = new ArrayList<>();
            for (String entry : moduleBytecode.split(";;")) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) {
                    list.add(new JavaCompilerSettings.ModuleBytecodeVersion(parts[0], parts[1]));
                }
            }
            s.setPerModuleBytecodeVersions(list);
        }

        String targetJdk = Settings.get(KEY_USE_MODULE_TARGET_JDK);
        if (targetJdk != null) s.setUseCompilerFromModuleTargetJdk(Boolean.parseBoolean(targetJdk));

        String debug = Settings.get(KEY_GENERATE_DEBUGGING_INFO);
        if (debug != null) s.setGenerateDebuggingInfo(Boolean.parseBoolean(debug));

        String deprecated = Settings.get(KEY_REPORT_DEPRECATED);
        if (deprecated != null) s.setReportDeprecated(Boolean.parseBoolean(deprecated));

        String noWarn = Settings.get(KEY_GENERATE_NO_WARNINGS);
        if (noWarn != null) s.setGenerateNoWarnings(Boolean.parseBoolean(noWarn));

        String additional = Settings.get(KEY_ADDITIONAL_PARAMS);
        if (additional != null) s.setAdditionalCommandLineParameters(additional);

        String moduleParams = Settings.get(KEY_PER_MODULE_COMPILER_PARAMS);
        if (moduleParams != null && !moduleParams.isBlank()) {
            List<JavaCompilerSettings.ModuleCompilerParameter> list = new ArrayList<>();
            for (String entry : moduleParams.split(";;")) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) {
                    list.add(new JavaCompilerSettings.ModuleCompilerParameter(parts[0], parts[1]));
                }
            }
            s.setPerModuleCompilerParameters(list);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_USE_COMPILER, currentSettings.getUseCompiler());
        Settings.set(KEY_USE_RELEASE_OPTION, String.valueOf(currentSettings.isUseReleaseOption()));
        Settings.set(KEY_PROJECT_BYTECODE_VERSION, currentSettings.getProjectBytecodeVersion());

        StringBuilder sbBytecode = new StringBuilder();
        for (JavaCompilerSettings.ModuleBytecodeVersion v : currentSettings.getPerModuleBytecodeVersions()) {
            if (!sbBytecode.isEmpty()) sbBytecode.append(";;");
            sbBytecode.append(v.getModule()).append("=").append(v.getTargetBytecodeVersion());
        }
        Settings.set(KEY_PER_MODULE_BYTECODE_VERSIONS, sbBytecode.toString());

        Settings.set(KEY_USE_MODULE_TARGET_JDK, String.valueOf(currentSettings.isUseCompilerFromModuleTargetJdk()));
        Settings.set(KEY_GENERATE_DEBUGGING_INFO, String.valueOf(currentSettings.isGenerateDebuggingInfo()));
        Settings.set(KEY_REPORT_DEPRECATED, String.valueOf(currentSettings.isReportDeprecated()));
        Settings.set(KEY_GENERATE_NO_WARNINGS, String.valueOf(currentSettings.isGenerateNoWarnings()));
        Settings.set(KEY_ADDITIONAL_PARAMS, currentSettings.getAdditionalCommandLineParameters());

        StringBuilder sbParams = new StringBuilder();
        for (JavaCompilerSettings.ModuleCompilerParameter p : currentSettings.getPerModuleCompilerParameters()) {
            if (!sbParams.isEmpty()) sbParams.append(";;");
            sbParams.append(p.getModule()).append("=").append(p.getCompilationOptions());
        }
        Settings.set(KEY_PER_MODULE_COMPILER_PARAMS, sbParams.toString());
    }
}
