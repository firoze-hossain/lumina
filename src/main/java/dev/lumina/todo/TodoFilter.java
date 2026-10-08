package dev.lumina.todo;

import java.util.*;

/**
 * Represents a named filter for grouping specific TODO patterns in Lumina IDE.
 */
public class TodoFilter {

    private final String id;
    private String name;
    private final Set<String> patternIds = new LinkedHashSet<>();

    public TodoFilter(String id, String name, Collection<String> patternIds) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = Objects.requireNonNull(name, "name cannot be null").trim();
        if (patternIds != null) {
            this.patternIds.addAll(patternIds);
        }
    }

    public TodoFilter(TodoFilter other) {
        this.id = other.id;
        this.name = other.name;
        this.patternIds.addAll(other.patternIds);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null").trim();
    }

    public Set<String> getPatternIds() {
        return Collections.unmodifiableSet(patternIds);
    }

    public void setPatternIds(Collection<String> ids) {
        this.patternIds.clear();
        if (ids != null) {
            this.patternIds.addAll(ids);
        }
    }

    public void addPatternId(String patternId) {
        if (patternId != null) {
            patternIds.add(patternId);
        }
    }

    public void removePatternId(String patternId) {
        patternIds.remove(patternId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TodoFilter that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(patternIds, that.patternIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, patternIds);
    }

    @Override
    public String toString() {
        return name + " (" + patternIds.size() + " patterns)";
    }
}
