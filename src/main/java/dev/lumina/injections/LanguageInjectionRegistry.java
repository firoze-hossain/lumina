package dev.lumina.injections;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central dynamic registry managing language injections, provider extensions,
 * live workspace state, dirty tracking, and settings dialog lifecycle (apply / reset).
 */
public class LanguageInjectionRegistry {

    private static final LanguageInjectionRegistry INSTANCE = new LanguageInjectionRegistry();

    public static LanguageInjectionRegistry getInstance() {
        return INSTANCE;
    }

    private final List<LanguageInjectionProvider> providers = new CopyOnWriteArrayList<>();

    // Master state committed in the IDE
    private final Map<String, LanguageInjection> committedInjections = new LinkedHashMap<>();

    // Working state currently being edited in settings dialog
    private final Map<String, LanguageInjection> workingInjections = new LinkedHashMap<>();

    private Runnable onModifiedListener;

    private LanguageInjectionRegistry() {
        registerProvider(new BuiltInLanguageInjectionProvider());
        resetToDefaults();
    }

    /**
     * Dynamically registers a language injection provider (e.g. from a plugin).
     */
    public synchronized void registerProvider(LanguageInjectionProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
            for (LanguageInjection injection : provider.getInjections()) {
                if (!committedInjections.containsKey(injection.getId())) {
                    committedInjections.put(injection.getId(), injection.copy());
                    workingInjections.put(injection.getId(), injection.copy());
                }
            }
            fireModified();
        }
    }

    /**
     * Resets the entire registry to provider defaults.
     */
    public synchronized void resetToDefaults() {
        committedInjections.clear();
        for (LanguageInjectionProvider provider : providers) {
            for (LanguageInjection inj : provider.getInjections()) {
                committedInjections.put(inj.getId(), inj.copy());
            }
        }
        reset();
    }

    /**
     * Resets working state to the last committed/applied state.
     */
    public synchronized void reset() {
        workingInjections.clear();
        for (Map.Entry<String, LanguageInjection> entry : committedInjections.entrySet()) {
            workingInjections.put(entry.getKey(), entry.getValue().copy());
        }
        fireModified();
    }

    /**
     * Applies/commits working state as active settings.
     */
    public synchronized void apply() {
        committedInjections.clear();
        for (Map.Entry<String, LanguageInjection> entry : workingInjections.entrySet()) {
            committedInjections.put(entry.getKey(), entry.getValue().copy());
        }
        fireModified();
    }

    /**
     * Returns true if working state differs from applied state.
     */
    public synchronized boolean isModified() {
        if (workingInjections.size() != committedInjections.size()) {
            return true;
        }
        for (Map.Entry<String, LanguageInjection> entry : workingInjections.entrySet()) {
            LanguageInjection committed = committedInjections.get(entry.getKey());
            if (committed == null || !committed.equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an unmodifiable snapshot list of working injections.
     */
    public synchronized List<LanguageInjection> getWorkingInjections() {
        List<LanguageInjection> list = new ArrayList<>(workingInjections.values());
        list.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
        return Collections.unmodifiableList(list);
    }

    public synchronized LanguageInjection findInjection(String id) {
        return workingInjections.get(id);
    }

    public synchronized void addInjection(LanguageInjection injection) {
        if (injection != null) {
            workingInjections.put(injection.getId(), injection.copy());
            fireModified();
        }
    }

    public synchronized void updateInjection(LanguageInjection updated) {
        if (updated != null && workingInjections.containsKey(updated.getId())) {
            workingInjections.put(updated.getId(), updated.copy());
            fireModified();
        }
    }

    public synchronized boolean removeInjection(String id) {
        if (id != null && workingInjections.containsKey(id)) {
            LanguageInjection current = workingInjections.get(id);
            // Built-in injections can't be deleted, only disabled, but custom/IDE/Project can be removed
            if (current.isBuiltIn()) {
                current.setEnabled(false);
            } else {
                workingInjections.remove(id);
            }
            fireModified();
            return true;
        }
        return false;
    }

    public synchronized LanguageInjection duplicateInjection(LanguageInjection source) {
        if (source == null) return null;
        String newId = source.getId() + ".copy." + System.currentTimeMillis();
        String newName = source.getDisplayName() + " Copy";
        LanguageInjection copy = source.copy();
        copy.setId(newId);
        copy.setDisplayName(newName);
        copy.setScope(LanguageInjectionScope.IDE);
        workingInjections.put(newId, copy);
        fireModified();
        return copy;
    }

    public synchronized void moveToScope(String id, LanguageInjectionScope targetScope) {
        LanguageInjection inj = workingInjections.get(id);
        if (inj != null && targetScope != null) {
            inj.setScope(targetScope);
            fireModified();
        }
    }

    public synchronized void setInjectionEnabled(String id, boolean enabled) {
        LanguageInjection inj = workingInjections.get(id);
        if (inj != null && inj.isEnabled() != enabled) {
            inj.setEnabled(enabled);
            fireModified();
        }
    }

    public synchronized void enableSelected(Collection<String> ids) {
        if (ids == null) return;
        boolean changed = false;
        for (String id : ids) {
            LanguageInjection inj = workingInjections.get(id);
            if (inj != null && !inj.isEnabled()) {
                inj.setEnabled(true);
                changed = true;
            }
        }
        if (changed) fireModified();
    }

    public synchronized void disableSelected(Collection<String> ids) {
        if (ids == null) return;
        boolean changed = false;
        for (String id : ids) {
            LanguageInjection inj = workingInjections.get(id);
            if (inj != null && inj.isEnabled()) {
                inj.setEnabled(false);
                changed = true;
            }
        }
        if (changed) fireModified();
    }

    public synchronized int totalInjectionsCount() {
        return workingInjections.size();
    }

    public synchronized int totalPlacesCount() {
        return workingInjections.values().stream().mapToInt(LanguageInjection::getPlacesCount).sum();
    }

    public synchronized int enabledPlacesCount() {
        return workingInjections.values().stream().mapToInt(LanguageInjection::getEnabledPlacesCount).sum();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
