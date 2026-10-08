package dev.lumina.todo;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Central manager and dynamic registry for TODO settings, patterns, and filters in Lumina IDE.
 * Manages pattern compilation, validation, filter grouping, and configuration lifecycle.
 */
public class TodoSettingsManager {

    private static final TodoSettingsManager INSTANCE = new TodoSettingsManager();

    public static TodoSettingsManager getInstance() {
        return INSTANCE;
    }

    private final List<TodoPatternProvider> patternProviders = new CopyOnWriteArrayList<>();
    private final List<TodoFilterProvider> filterProviders = new CopyOnWriteArrayList<>();

    // Committed settings
    private boolean committedTreatIndentedText = true;
    private final List<TodoPattern> committedPatterns = new ArrayList<>();
    private final List<TodoFilter> committedFilters = new ArrayList<>();

    // Working settings
    private boolean workingTreatIndentedText = true;
    private final List<TodoPattern> workingPatterns = new ArrayList<>();
    private final List<TodoFilter> workingFilters = new ArrayList<>();

    private Runnable onModifiedListener;

    private TodoSettingsManager() {
        registerPatternProvider(new DefaultTodoPatternProvider());
        reloadFromProviders();
        commit();
    }

    public synchronized void registerPatternProvider(TodoPatternProvider provider) {
        if (provider != null && !patternProviders.contains(provider)) {
            patternProviders.add(provider);
            reloadFromProviders();
        }
    }

    public synchronized void unregisterPatternProvider(TodoPatternProvider provider) {
        if (provider != null) {
            patternProviders.remove(provider);
            reloadFromProviders();
        }
    }

    public synchronized void registerFilterProvider(TodoFilterProvider provider) {
        if (provider != null && !filterProviders.contains(provider)) {
            filterProviders.add(provider);
            reloadFromProviders();
        }
    }

    public synchronized void unregisterFilterProvider(TodoFilterProvider provider) {
        if (provider != null) {
            filterProviders.remove(provider);
            reloadFromProviders();
        }
    }

    private synchronized void reloadFromProviders() {
        Map<String, TodoPattern> customPatterns = new LinkedHashMap<>();
        for (TodoPattern p : workingPatterns) {
            if (!p.isBuiltIn()) {
                customPatterns.put(p.getId(), p);
            }
        }

        workingPatterns.clear();
        for (TodoPatternProvider p : patternProviders) {
            List<TodoPattern> provided = p.getPatterns();
            if (provided != null) {
                for (TodoPattern item : provided) {
                    workingPatterns.add(new TodoPattern(item));
                }
            }
        }
        workingPatterns.addAll(customPatterns.values());

        Map<String, TodoFilter> customFilters = new LinkedHashMap<>();
        for (TodoFilter f : workingFilters) {
            customFilters.put(f.getId(), f);
        }

        workingFilters.clear();
        for (TodoFilterProvider p : filterProviders) {
            List<TodoFilter> provided = p.getFilters();
            if (provided != null) {
                for (TodoFilter item : provided) {
                    workingFilters.add(new TodoFilter(item));
                }
            }
        }
        for (TodoFilter f : customFilters.values()) {
            if (workingFilters.stream().noneMatch(x -> x.getId().equals(f.getId()))) {
                workingFilters.add(f);
            }
        }
    }

    public synchronized boolean isTreatIndentedText() {
        return workingTreatIndentedText;
    }

    public synchronized void setTreatIndentedText(boolean treatIndentedText) {
        if (this.workingTreatIndentedText != treatIndentedText) {
            this.workingTreatIndentedText = treatIndentedText;
            notifyModified();
        }
    }

    public synchronized List<TodoPattern> getWorkingPatterns() {
        return Collections.unmodifiableList(workingPatterns);
    }

    public synchronized List<TodoPattern> getCommittedPatterns() {
        return Collections.unmodifiableList(committedPatterns);
    }

    public synchronized List<TodoFilter> getWorkingFilters() {
        return Collections.unmodifiableList(workingFilters);
    }

    public synchronized List<TodoFilter> getCommittedFilters() {
        return Collections.unmodifiableList(committedFilters);
    }

    public static boolean validatePatternRegex(String regex) {
        if (regex == null || regex.isBlank()) {
            return false;
        }
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    public synchronized boolean addPattern(TodoPattern pattern) {
        if (pattern == null || !validatePatternRegex(pattern.getPattern())) {
            return false;
        }
        workingPatterns.add(pattern);
        notifyModified();
        return true;
    }

    public synchronized boolean updatePattern(String id, TodoPattern updated) {
        if (id == null || updated == null || !validatePatternRegex(updated.getPattern())) {
            return false;
        }
        for (int i = 0; i < workingPatterns.size(); i++) {
            if (workingPatterns.get(i).getId().equals(id)) {
                workingPatterns.set(i, updated);
                notifyModified();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean removePattern(String id) {
        if (id == null) return false;
        boolean removed = workingPatterns.removeIf(p -> p.getId().equals(id));
        if (removed) {
            // Also clean up references in filters
            for (TodoFilter f : workingFilters) {
                f.removePatternId(id);
            }
            notifyModified();
        }
        return removed;
    }

    public synchronized boolean addFilter(TodoFilter filter) {
        if (filter == null || filter.getName().isBlank()) {
            return false;
        }
        boolean exists = workingFilters.stream()
                .anyMatch(f -> f.getName().equalsIgnoreCase(filter.getName().trim()));
        if (exists) {
            return false;
        }
        workingFilters.add(filter);
        notifyModified();
        return true;
    }

    public synchronized boolean updateFilter(String id, TodoFilter updated) {
        if (id == null || updated == null || updated.getName().isBlank()) {
            return false;
        }
        for (int i = 0; i < workingFilters.size(); i++) {
            if (workingFilters.get(i).getId().equals(id)) {
                workingFilters.set(i, updated);
                notifyModified();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean removeFilter(String id) {
        if (id == null) return false;
        boolean removed = workingFilters.removeIf(f -> f.getId().equals(id));
        if (removed) {
            notifyModified();
        }
        return removed;
    }

    public synchronized boolean isModified() {
        if (workingTreatIndentedText != committedTreatIndentedText) {
            return true;
        }
        if (workingPatterns.size() != committedPatterns.size()) {
            return true;
        }
        for (int i = 0; i < workingPatterns.size(); i++) {
            if (!workingPatterns.get(i).equals(committedPatterns.get(i))) {
                return true;
            }
        }
        if (workingFilters.size() != committedFilters.size()) {
            return true;
        }
        for (int i = 0; i < workingFilters.size(); i++) {
            if (!workingFilters.get(i).equals(committedFilters.get(i))) {
                return true;
            }
        }
        return false;
    }

    public synchronized void apply() {
        commit();
        notifyModified();
    }

    public synchronized void reset() {
        workingTreatIndentedText = committedTreatIndentedText;
        workingPatterns.clear();
        for (TodoPattern p : committedPatterns) {
            workingPatterns.add(new TodoPattern(p));
        }
        workingFilters.clear();
        for (TodoFilter f : committedFilters) {
            workingFilters.add(new TodoFilter(f));
        }
        notifyModified();
    }

    private void commit() {
        committedTreatIndentedText = workingTreatIndentedText;
        committedPatterns.clear();
        for (TodoPattern p : workingPatterns) {
            committedPatterns.add(new TodoPattern(p));
        }
        committedFilters.clear();
        for (TodoFilter f : workingFilters) {
            committedFilters.add(new TodoFilter(f));
        }
    }

    public synchronized void resetToFactoryDefaults() {
        workingTreatIndentedText = true;
        workingPatterns.clear();
        workingFilters.clear();
        reloadFromProviders();
        commit();
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
