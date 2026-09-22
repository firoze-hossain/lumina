package dev.lumina.keymap;

import java.util.*;

/**
 * Model representing a Keymap profile.
 * Supports parent inheritance and custom overrides.
 */
public class Keymap {

    private final String id;
    private String name;
    private final String parentKeymapId;
    private final boolean mutable;

    // actionId -> list of shortcuts assigned in this keymap
    private final Map<String, List<KeyboardShortcut>> shortcuts = new LinkedHashMap<>();

    public Keymap(String id, String name, String parentKeymapId, boolean mutable) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.parentKeymapId = parentKeymapId;
        this.mutable = mutable;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
    }

    public String getParentKeymapId() {
        return parentKeymapId;
    }

    public boolean isMutable() {
        return mutable;
    }

    /**
     * Checks if this keymap has explicitly overridden shortcuts for actionId.
     */
    public synchronized boolean hasOverride(String actionId) {
        return shortcuts.containsKey(actionId);
    }

    /**
     * Returns shortcuts explicitly assigned in this keymap instance.
     */
    public synchronized List<KeyboardShortcut> getDirectShortcuts(String actionId) {
        List<KeyboardShortcut> list = shortcuts.get(actionId);
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    /**
     * Returns all direct shortcuts mapped by action ID.
     */
    public synchronized Map<String, List<KeyboardShortcut>> getAllDirectShortcuts() {
        Map<String, List<KeyboardShortcut>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<KeyboardShortcut>> entry : shortcuts.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Adds a shortcut for an action in this keymap.
     */
    public synchronized void addShortcut(String actionId, KeyboardShortcut shortcut) {
        if (actionId == null || shortcut == null) return;
        shortcuts.computeIfAbsent(actionId, k -> new ArrayList<>());
        List<KeyboardShortcut> list = shortcuts.get(actionId);
        if (!list.contains(shortcut)) {
            list.add(shortcut);
        }
    }

    /**
     * Removes a shortcut for an action in this keymap.
     */
    public synchronized void removeShortcut(String actionId, KeyboardShortcut shortcut) {
        if (actionId == null || shortcut == null) return;
        List<KeyboardShortcut> list = shortcuts.get(actionId);
        if (list != null) {
            list.remove(shortcut);
        }
    }

    /**
     * Clears all shortcuts for an action (marking it explicitly empty).
     */
    public synchronized void clearShortcuts(String actionId) {
        if (actionId == null) return;
        shortcuts.put(actionId, new ArrayList<>());
    }

    /**
     * Reverts any custom overrides for an action, falling back to parent keymap.
     */
    public synchronized void resetAction(String actionId) {
        if (actionId != null) {
            shortcuts.remove(actionId);
        }
    }

    /**
     * Creates an identical copy of this keymap.
     */
    public synchronized Keymap cloneAs(String newId, String newName) {
        Keymap clone = new Keymap(newId, newName, this.id, true);
        for (Map.Entry<String, List<KeyboardShortcut>> entry : shortcuts.entrySet()) {
            clone.shortcuts.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return clone;
    }

    /**
     * Serializes this keymap's shortcuts to a string for preferences storage.
     */
    public synchronized String serializeShortcuts() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<KeyboardShortcut>> entry : shortcuts.entrySet()) {
            sb.append(entry.getKey()).append("=");
            List<KeyboardShortcut> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i).toStorageString());
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Deserializes shortcuts from a stored string.
     */
    public synchronized void deserializeShortcuts(String data) {
        shortcuts.clear();
        if (data == null || data.isBlank()) return;
        String[] lines = data.split("\n");
        for (String line : lines) {
            if (line.isBlank() || !line.contains("=")) continue;
            String[] parts = line.split("=", 2);
            String actionId = parts[0];
            String scList = parts[1];
            List<KeyboardShortcut> list = new ArrayList<>();
            if (!scList.isBlank()) {
                String[] tokens = scList.split(",");
                for (String token : tokens) {
                    KeyboardShortcut sc = KeyboardShortcut.fromStorageString(token);
                    if (sc != null) {
                        list.add(sc);
                    }
                }
            }
            shortcuts.put(actionId, list);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keymap keymap = (Keymap) o;
        return Objects.equals(id, keymap.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
