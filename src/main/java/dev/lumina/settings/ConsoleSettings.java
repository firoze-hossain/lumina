package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for IntelliJ IDEA-style Editor > General > Console settings.
 * Backed by ~/.lumina/lumina.properties, supporting listeners, dirty tracking, and console line filtering.
 */
public final class ConsoleSettings {

    public static final List<String> DEFAULT_FOLDING_PATTERNS = List.of(
            "/gems/cucumber",
            "/gems/gherkin",
            "/gems/rspec",
            "/gems/shoulda",
            "/gems/test",
            "at com.intellij.jpa.",
            "at com.intellij.junit3.",
            "at com.intellij.junit4.",
            "at com.intellij.junit5.",
            "at com.intellij.junit6.",
            "at com.intellij.rt.",
            "at com.intellij.runtime.execution.",
            "at com.jgoodies.binding.beans.ExtendedPropertyChangeSupport.firePropertyChange0(",
            "at com.sun.proxy.$Proxy",
            "at org.mockito.internal.",
            "at org.springframework.web.filter.",
            "at org.springframework.web.method.",
            "at org.springframework.web.reactive.",
            "at org.springframework.web.servlet.",
            "at org.testng.internal.",
            "at org.testng.SuiteRunner.",
            "at org.testng.TestNG.run",
            "at org.testng.TestRunner."
    );

    public static final List<String> DEFAULT_EXCEPTION_PATTERNS = List.of(
            "at org.codehaus.groovy.runtime.DefaultGroovyMethods.",
            "at org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport.",
            "at org.codehaus.groovy.vmplugin.v5.PluginDefaultGroovyMethods."
    );

    private static final ConsoleSettings INSTANCE = new ConsoleSettings();

    public static ConsoleSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(ConsoleSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // Settings fields
    private boolean useSoftWraps = false;
    private int historySize = 300;
    private boolean overrideCycleBufferSize = false;
    private int cycleBufferSizeKb = 1024;
    private String defaultEncoding = "<System Default: UTF-8>";
    private final List<String> foldingPatterns = new ArrayList<>();
    private final List<String> exceptionPatterns = new ArrayList<>();
    private boolean foldStackTraceLongerThan = true;
    private int stackTraceLinesLimit = 8;

    public ConsoleSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        useSoftWraps = false;
        historySize = 300;
        overrideCycleBufferSize = false;
        cycleBufferSizeKb = 1024;
        defaultEncoding = "<System Default: UTF-8>";

        foldingPatterns.clear();
        foldingPatterns.addAll(DEFAULT_FOLDING_PATTERNS);

        exceptionPatterns.clear();
        exceptionPatterns.addAll(DEFAULT_EXCEPTION_PATTERNS);

        foldStackTraceLongerThan = true;
        stackTraceLinesLimit = 8;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void load() {
        useSoftWraps = getBool("use_soft_wraps", useSoftWraps);
        historySize = getInt("history_size", historySize);
        overrideCycleBufferSize = getBool("override_cycle_buffer", overrideCycleBufferSize);
        cycleBufferSizeKb = getInt("cycle_buffer_size_kb", cycleBufferSizeKb);
        defaultEncoding = getStr("default_encoding", defaultEncoding);

        loadPatternList("folding_patterns", foldingPatterns, DEFAULT_FOLDING_PATTERNS);
        loadPatternList("exception_patterns", exceptionPatterns, DEFAULT_EXCEPTION_PATTERNS);

        foldStackTraceLongerThan = getBool("fold_stack_trace_longer_than", foldStackTraceLongerThan);
        stackTraceLinesLimit = getInt("stack_trace_lines_limit", stackTraceLinesLimit);
    }

    public void save() {
        putBool("use_soft_wraps", useSoftWraps);
        putInt("history_size", historySize);
        putBool("override_cycle_buffer", overrideCycleBufferSize);
        putInt("cycle_buffer_size_kb", cycleBufferSizeKb);
        putStr("default_encoding", defaultEncoding);

        savePatternList("folding_patterns", foldingPatterns);
        savePatternList("exception_patterns", exceptionPatterns);

        putBool("fold_stack_trace_longer_than", foldStackTraceLongerThan);
        putInt("stack_trace_lines_limit", stackTraceLinesLimit);

        notifyListeners();
    }

    private void loadPatternList(String key, List<String> target, List<String> defaultList) {
        String raw = Settings.get("editor.console." + key);
        if (raw == null) {
            target.clear();
            target.addAll(defaultList);
            return;
        }
        target.clear();
        if ("__EMPTY__".equals(raw.trim())) {
            return;
        }
        for (String line : raw.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                target.add(trimmed);
            }
        }
    }

    private void savePatternList(String key, List<String> source) {
        if (source.isEmpty()) {
            Settings.put("editor.console." + key, "__EMPTY__");
        } else {
            Settings.put("editor.console." + key, String.join("\n", source));
        }
    }

    public void copyFrom(ConsoleSettings o) {
        this.useSoftWraps = o.useSoftWraps;
        this.historySize = o.historySize;
        this.overrideCycleBufferSize = o.overrideCycleBufferSize;
        this.cycleBufferSizeKb = o.cycleBufferSizeKb;
        this.defaultEncoding = o.defaultEncoding;

        this.foldingPatterns.clear();
        this.foldingPatterns.addAll(o.foldingPatterns);

        this.exceptionPatterns.clear();
        this.exceptionPatterns.addAll(o.exceptionPatterns);

        this.foldStackTraceLongerThan = o.foldStackTraceLongerThan;
        this.stackTraceLinesLimit = o.stackTraceLinesLimit;
    }

    public ConsoleSettings copy() {
        ConsoleSettings c = new ConsoleSettings();
        c.copyFrom(this);
        return c;
    }

    public boolean isModified(ConsoleSettings o) {
        if (o == null) return true;
        return this.useSoftWraps != o.useSoftWraps
                || this.historySize != o.historySize
                || this.overrideCycleBufferSize != o.overrideCycleBufferSize
                || this.cycleBufferSizeKb != o.cycleBufferSizeKb
                || !Objects.equals(this.defaultEncoding, o.defaultEncoding)
                || !Objects.equals(this.foldingPatterns, o.foldingPatterns)
                || !Objects.equals(this.exceptionPatterns, o.exceptionPatterns)
                || this.foldStackTraceLongerThan != o.foldStackTraceLongerThan
                || this.stackTraceLinesLimit != o.stackTraceLinesLimit;
    }

    /**
     * Determines whether a given console line should be folded based on current rules.
     * Exceptions take precedence over folding patterns.
     */
    public boolean shouldFoldLine(String line) {
        if (line == null || line.isEmpty()) return false;
        for (String exc : exceptionPatterns) {
            if (line.contains(exc)) {
                return false;
            }
        }
        for (String pat : foldingPatterns) {
            if (line.contains(pat)) {
                return true;
            }
        }
        return false;
    }

    private boolean getBool(String key, boolean def) {
        String val = Settings.get("editor.console." + key);
        return val != null ? Boolean.parseBoolean(val) : def;
    }

    private void putBool(String key, boolean val) {
        Settings.put("editor.console." + key, String.valueOf(val));
    }

    private int getInt(String key, int def) {
        String val = Settings.get("editor.console." + key);
        if (val != null) {
            try { return Integer.parseInt(val.trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private void putInt(String key, int val) {
        Settings.put("editor.console." + key, String.valueOf(val));
    }

    private String getStr(String key, String def) {
        String val = Settings.get("editor.console." + key);
        return val != null ? val : def;
    }

    private void putStr(String key, String val) {
        Settings.put("editor.console." + key, val != null ? val : "");
    }

    // Getters and Setters
    public boolean isUseSoftWraps() { return useSoftWraps; }
    public void setUseSoftWraps(boolean useSoftWraps) { this.useSoftWraps = useSoftWraps; }

    public int getHistorySize() { return historySize; }
    public void setHistorySize(int historySize) { this.historySize = historySize; }

    public boolean isOverrideCycleBufferSize() { return overrideCycleBufferSize; }
    public void setOverrideCycleBufferSize(boolean overrideCycleBufferSize) { this.overrideCycleBufferSize = overrideCycleBufferSize; }

    public int getCycleBufferSizeKb() { return cycleBufferSizeKb; }
    public void setCycleBufferSizeKb(int cycleBufferSizeKb) { this.cycleBufferSizeKb = cycleBufferSizeKb; }

    public String getDefaultEncoding() { return defaultEncoding; }
    public void setDefaultEncoding(String defaultEncoding) { this.defaultEncoding = defaultEncoding != null ? defaultEncoding : "<System Default: UTF-8>"; }

    public List<String> getFoldingPatterns() { return foldingPatterns; }
    public void setFoldingPatterns(List<String> patterns) {
        this.foldingPatterns.clear();
        if (patterns != null) {
            this.foldingPatterns.addAll(patterns);
        }
    }

    public List<String> getExceptionPatterns() { return exceptionPatterns; }
    public void setExceptionPatterns(List<String> patterns) {
        this.exceptionPatterns.clear();
        if (patterns != null) {
            this.exceptionPatterns.addAll(patterns);
        }
    }

    public boolean isFoldStackTraceLongerThan() { return foldStackTraceLongerThan; }
    public void setFoldStackTraceLongerThan(boolean foldStackTraceLongerThan) { this.foldStackTraceLongerThan = foldStackTraceLongerThan; }

    public int getStackTraceLinesLimit() { return stackTraceLinesLimit; }
    public void setStackTraceLinesLimit(int stackTraceLinesLimit) { this.stackTraceLinesLimit = stackTraceLinesLimit; }
}
