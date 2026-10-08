package dev.lumina.build;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager responsible for loading and saving Groovy Compiler configuration.
 */
public class GroovyCompilerSettingsManager {

    public static final String KEY_GROOVY_CONFIGSCRIPT = "compiler.groovy.configscript.path";
    public static final String KEY_GROOVY_INVOKE_DYNAMIC = "compiler.groovy.invoke.dynamic";
    public static final String KEY_GROOVY_STUB_EXCLUDES = "compiler.groovy.stub.excludes";

    private static volatile GroovyCompilerSettingsManager instance;
    private GroovyCompilerSettings currentSettings;

    private GroovyCompilerSettingsManager() {
        loadSettings();
    }

    public static GroovyCompilerSettingsManager getInstance() {
        if (instance == null) {
            synchronized (GroovyCompilerSettingsManager.class) {
                if (instance == null) {
                    instance = new GroovyCompilerSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized GroovyCompilerSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(GroovyCompilerSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        GroovyCompilerSettings s = new GroovyCompilerSettings();
        String script = Settings.get(KEY_GROOVY_CONFIGSCRIPT);
        if (script != null) s.setConfigScriptPath(script);

        String dynamic = Settings.get(KEY_GROOVY_INVOKE_DYNAMIC);
        if (dynamic != null) s.setInvokeDynamicSupport(Boolean.parseBoolean(dynamic));

        String rawExcludes = Settings.get(KEY_GROOVY_STUB_EXCLUDES);
        if (rawExcludes != null && !rawExcludes.isBlank()) {
            List<CompilerExcludeEntry> list = new ArrayList<>();
            for (String item : rawExcludes.split(";;")) {
                if (item.isBlank()) continue;
                String[] parts = item.split("\\|\\|", -1);
                if (parts.length >= 2) {
                    list.add(new CompilerExcludeEntry(parts[0], Boolean.parseBoolean(parts[1])));
                } else if (parts.length == 1) {
                    list.add(new CompilerExcludeEntry(parts[0], true));
                }
            }
            s.setStubGenerationExcludes(list);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_GROOVY_CONFIGSCRIPT, currentSettings.getConfigScriptPath());
        Settings.set(KEY_GROOVY_INVOKE_DYNAMIC, String.valueOf(currentSettings.isInvokeDynamicSupport()));

        StringBuilder sb = new StringBuilder();
        for (CompilerExcludeEntry e : currentSettings.getStubGenerationExcludes()) {
            if (!sb.isEmpty()) sb.append(";;");
            sb.append(e.getPath()).append("||").append(e.isRecursive());
        }
        Settings.set(KEY_GROOVY_STUB_EXCLUDES, sb.toString());
    }
}
