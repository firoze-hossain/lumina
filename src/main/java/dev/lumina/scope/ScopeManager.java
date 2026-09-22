package dev.lumina.scope;

import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Centralized manager for named scopes (built-in and custom local/shared scopes).
 */
public class ScopeManager {

    private static final ScopeManager INSTANCE = new ScopeManager();

    private final Preferences prefs = Preferences.userNodeForPackage(ScopeManager.class);
    private final List<NamedScope> scopes = new ArrayList<>();
    private final List<NamedScope> builtInScopes = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private ScopeManager() {
        initBuiltInScopes();
        loadPreferences();
    }

    public static ScopeManager getInstance() {
        return INSTANCE;
    }

    private void initBuiltInScopes() {
        builtInScopes.clear();
        builtInScopes.add(NamedScope.builtIn("Project Files", ""));
        builtInScopes.add(NamedScope.builtIn("Tests", "file:*src/test//*"));
        builtInScopes.add(NamedScope.builtIn("Non-Project Files", ""));
        builtInScopes.add(NamedScope.builtIn("Generated Files", "file:*target/generated-sources//*"));
        builtInScopes.add(NamedScope.builtIn("Open Files", ""));
        builtInScopes.add(NamedScope.builtIn("Current File", ""));
        builtInScopes.add(NamedScope.builtIn("Scratches and Consoles", ""));
    }

    public synchronized List<NamedScope> getAllScopes() {
        List<NamedScope> all = new ArrayList<>(builtInScopes);
        all.addAll(scopes);
        return all;
    }

    public synchronized List<NamedScope> getCustomScopes() {
        return new ArrayList<>(scopes);
    }

    public synchronized List<NamedScope> getBuiltInScopes() {
        return new ArrayList<>(builtInScopes);
    }

    public synchronized NamedScope getScope(String name) {
        if (name == null) return null;
        for (NamedScope s : getAllScopes()) {
            if (s.getName().equalsIgnoreCase(name.trim())) {
                return s;
            }
        }
        return null;
    }

    public synchronized void addCustomScope(NamedScope scope) {
        if (scope == null) return;
        scopes.add(scope);
        savePreferences();
        notifyListeners();
    }

    public synchronized void removeCustomScope(NamedScope scope) {
        if (scope == null || scope.isBuiltIn()) return;
        scopes.remove(scope);
        savePreferences();
        notifyListeners();
    }

    public synchronized void moveUp(int index) {
        if (index > 0 && index < scopes.size()) {
            NamedScope item = scopes.remove(index);
            scopes.add(index - 1, item);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void moveDown(int index) {
        if (index >= 0 && index < scopes.size() - 1) {
            NamedScope item = scopes.remove(index);
            scopes.add(index + 1, item);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean matches(String scopeName, Path file, Path projectRoot) {
        NamedScope scope = getScope(scopeName);
        if (scope != null) {
            return scope.matches(file, projectRoot);
        }
        return false;
    }

    public synchronized void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable l : new ArrayList<>(listeners)) {
            try {
                l.run();
            } catch (Exception ignored) {
            }
        }
    }

    private void loadPreferences() {
        scopes.clear();
        String saved = prefs.get("custom_scopes_data", "");
        if (!saved.isEmpty()) {
            String[] lines = saved.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", 3);
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    boolean shared = Boolean.parseBoolean(parts[1].trim());
                    String pattern = parts.length > 2 ? parts[2].trim() : "";
                    scopes.add(new NamedScope(name, pattern, shared, false));
                }
            }
        }
    }

    private void savePreferences() {
        StringBuilder sb = new StringBuilder();
        for (NamedScope s : scopes) {
            sb.append(s.getName()).append("|")
                    .append(s.isShared()).append("|")
                    .append(s.getPattern()).append("\n");
        }
        prefs.put("custom_scopes_data", sb.toString());
    }
}
