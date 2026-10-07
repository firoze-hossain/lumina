package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for Editor > Duplicates settings.
 * Backed by persistent storage, supporting dirty tracking, dynamic language registration, and real-time updates.
 */
public final class DuplicatesSettings {

    public static class LanguageProfile {
        private final String name;
        private boolean enabled;
        private boolean differentVariableNames;
        private boolean differentFunctionNames;
        private boolean differentConstantValues;

        public LanguageProfile(String name, boolean enabled, boolean differentVariableNames,
                               boolean differentFunctionNames, boolean differentConstantValues) {
            this.name = name;
            this.enabled = enabled;
            this.differentVariableNames = differentVariableNames;
            this.differentFunctionNames = differentFunctionNames;
            this.differentConstantValues = differentConstantValues;
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDifferentVariableNames() {
            return differentVariableNames;
        }

        public void setDifferentVariableNames(boolean differentVariableNames) {
            this.differentVariableNames = differentVariableNames;
        }

        public boolean isDifferentFunctionNames() {
            return differentFunctionNames;
        }

        public void setDifferentFunctionNames(boolean differentFunctionNames) {
            this.differentFunctionNames = differentFunctionNames;
        }

        public boolean isDifferentConstantValues() {
            return differentConstantValues;
        }

        public void setDifferentConstantValues(boolean differentConstantValues) {
            this.differentConstantValues = differentConstantValues;
        }

        public LanguageProfile copy() {
            return new LanguageProfile(name, enabled, differentVariableNames, differentFunctionNames, differentConstantValues);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LanguageProfile that = (LanguageProfile) o;
            return enabled == that.enabled &&
                    differentVariableNames == that.differentVariableNames &&
                    differentFunctionNames == that.differentFunctionNames &&
                    differentConstantValues == that.differentConstantValues &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, enabled, differentVariableNames, differentFunctionNames, differentConstantValues);
        }
    }

    private static final DuplicatesSettings INSTANCE = new DuplicatesSettings();

    public static DuplicatesSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(DuplicatesSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, LanguageProfile> profiles = new LinkedHashMap<>();

    public DuplicatesSettings() {
        initDefaults();
        load();
    }

    public void resetToDefaults() {
        initDefaults();
    }

    public void initDefaults() {
        profiles.clear();
        // Exact defaults matching IntelliJ IDEA screenshot:
        addDefaultProfile("Kotlin", true, true, false, false);
        addDefaultProfile("XML", false, true, false, false);
        addDefaultProfile("HTML", false, true, false, false);
        addDefaultProfile("XHTML", false, true, false, false);
        addDefaultProfile("Java", true, true, false, false);
        addDefaultProfile("Style Sheets", true, true, false, false);
        addDefaultProfile("Rust", true, true, false, false);
        addDefaultProfile("Go", true, true, false, false);
        addDefaultProfile("Ruby", true, true, false, false);
        addDefaultProfile("Python", true, true, false, false);
        addDefaultProfile("Groovy", true, true, false, false);
        addDefaultProfile("JavaScript", true, true, false, false);
        addDefaultProfile("TypeScript", true, true, false, false);
        addDefaultProfile("PHP", true, true, false, false);
        addDefaultProfile("Scala", true, true, false, false);
    }

    private void addDefaultProfile(String name, boolean enabled, boolean varDiff, boolean funcDiff, boolean constDiff) {
        profiles.put(name, new LanguageProfile(name, enabled, varDiff, funcDiff, constDiff));
    }

    public void load() {
        for (LanguageProfile p : profiles.values()) {
            String prefix = "duplicates.lang." + p.getName().toLowerCase().replace(" ", "_");
            String enabledVal = Settings.get(prefix + ".enabled");
            if (enabledVal != null) {
                p.setEnabled(Boolean.parseBoolean(enabledVal));
            }
            String varVal = Settings.get(prefix + ".var_diff");
            if (varVal != null) {
                p.setDifferentVariableNames(Boolean.parseBoolean(varVal));
            }
            String funcVal = Settings.get(prefix + ".func_diff");
            if (funcVal != null) {
                p.setDifferentFunctionNames(Boolean.parseBoolean(funcVal));
            }
            String constVal = Settings.get(prefix + ".const_diff");
            if (constVal != null) {
                p.setDifferentConstantValues(Boolean.parseBoolean(constVal));
            }
        }
    }

    public void save() {
        for (LanguageProfile p : profiles.values()) {
            String prefix = "duplicates.lang." + p.getName().toLowerCase().replace(" ", "_");
            Settings.put(prefix + ".enabled", String.valueOf(p.isEnabled()));
            Settings.put(prefix + ".var_diff", String.valueOf(p.isDifferentVariableNames()));
            Settings.put(prefix + ".func_diff", String.valueOf(p.isDifferentFunctionNames()));
            Settings.put(prefix + ".const_diff", String.valueOf(p.isDifferentConstantValues()));
        }
        notifyListeners();
    }

    public DuplicatesSettings copy() {
        DuplicatesSettings clone = new DuplicatesSettings();
        clone.applyFrom(this);
        return clone;
    }

    public void applyFrom(DuplicatesSettings other) {
        if (other == null) return;
        this.profiles.clear();
        for (LanguageProfile p : other.profiles.values()) {
            this.profiles.put(p.getName(), p.copy());
        }
    }

    public boolean isModified(DuplicatesSettings other) {
        if (other == null) return true;
        if (this.profiles.size() != other.profiles.size()) return true;
        for (Map.Entry<String, LanguageProfile> entry : this.profiles.entrySet()) {
            LanguageProfile otherProfile = other.profiles.get(entry.getKey());
            if (otherProfile == null || !entry.getValue().equals(otherProfile)) {
                return true;
            }
        }
        return false;
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Exception ignored) {}
        }
    }

    public List<LanguageProfile> getProfiles() {
        return new ArrayList<>(profiles.values());
    }

    public LanguageProfile getProfile(String name) {
        return profiles.get(name);
    }

    public void registerLanguage(String name, boolean enabled, boolean varDiff, boolean funcDiff, boolean constDiff) {
        if (name != null && !profiles.containsKey(name)) {
            profiles.put(name, new LanguageProfile(name, enabled, varDiff, funcDiff, constDiff));
            notifyListeners();
        }
    }
}
