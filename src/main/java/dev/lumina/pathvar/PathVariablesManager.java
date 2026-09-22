package dev.lumina.pathvar;

import java.io.File;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Central singleton managing IDE Path Variables and Ignored Variables.
 * Strictly matches media_1790046850137.png.
 */
public class PathVariablesManager {

    private static final PathVariablesManager INSTANCE = new PathVariablesManager();

    private final Preferences prefs = Preferences.userNodeForPackage(PathVariablesManager.class);
    private final List<PathVariable> variables = new ArrayList<>();
    private String ignoredVariables = "";
    private final List<Runnable> listeners = new ArrayList<>();

    private PathVariablesManager() {
        loadPreferences();
        if (variables.isEmpty()) {
            initDefaults();
        }
    }

    public static PathVariablesManager getInstance() {
        return INSTANCE;
    }

    private void initDefaults() {
        variables.clear();
        String m2Repo = System.getProperty("user.home").replace('\\', '/') + "/.m2/repository";
        variables.add(new PathVariable("MAVEN_REPOSITORY", m2Repo));
        ignoredVariables = "";
    }

    public synchronized List<PathVariable> getVariables() {
        return new ArrayList<>(variables);
    }

    public synchronized PathVariable getVariable(String name) {
        if (name == null) return null;
        for (PathVariable pv : variables) {
            if (pv.getName().equalsIgnoreCase(name.trim())) {
                return pv;
            }
        }
        return null;
    }

    public synchronized void addVariable(PathVariable variable) {
        if (variable == null || variable.getName().isBlank()) return;
        variables.removeIf(v -> v.getName().equalsIgnoreCase(variable.getName()));
        variables.add(variable);
        savePreferences();
        notifyListeners();
    }

    public synchronized void updateVariable(PathVariable oldVar, PathVariable newVar) {
        if (oldVar == null || newVar == null) return;
        int idx = variables.indexOf(oldVar);
        if (idx != -1) {
            variables.set(idx, newVar);
        } else {
            addVariable(newVar);
        }
        savePreferences();
        notifyListeners();
    }

    public synchronized void removeVariable(PathVariable variable) {
        if (variable == null) return;
        if (variables.remove(variable)) {
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized String getIgnoredVariables() {
        return ignoredVariables;
    }

    public synchronized void setIgnoredVariables(String ignoredVariables) {
        this.ignoredVariables = (ignoredVariables != null) ? ignoredVariables.trim() : "";
        savePreferences();
        notifyListeners();
    }

    /**
     * Replaces variable references (e.g. $MAVEN_REPOSITORY$/lib) with actual filesystem path.
     */
    public synchronized String expand(String pathWithVariables) {
        if (pathWithVariables == null || !pathWithVariables.contains("$")) {
            return pathWithVariables;
        }
        String result = pathWithVariables;
        for (PathVariable pv : variables) {
            String token = "$" + pv.getName() + "$";
            if (result.contains(token)) {
                result = result.replace(token, pv.getValue());
            }
        }
        return result;
    }

    /**
     * Substitutes variable paths (e.g. /home/user/.m2/repository/foo) with $MAVEN_REPOSITORY$/foo.
     */
    public synchronized String substitute(String fullPath) {
        if (fullPath == null) return null;
        String norm = fullPath.replace('\\', '/');
        for (PathVariable pv : variables) {
            String val = pv.getValue().replace('\\', '/');
            if (!val.isEmpty() && norm.startsWith(val)) {
                return "$" + pv.getName() + "$" + norm.substring(val.length());
            }
        }
        return fullPath;
    }

    public synchronized void revertToDefaults() {
        initDefaults();
        savePreferences();
        notifyListeners();
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
            } catch (Exception ignored) {}
        }
    }

    private void loadPreferences() {
        variables.clear();
        String data = prefs.get("path_variables_data", "");
        if (!data.isBlank()) {
            String[] lines = data.split("\n");
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", 2);
                if (parts.length == 2) {
                    variables.add(new PathVariable(parts[0], parts[1]));
                }
            }
        }
        ignoredVariables = prefs.get("ignored_variables_data", "");
    }

    private void savePreferences() {
        StringBuilder sb = new StringBuilder();
        for (PathVariable pv : variables) {
            sb.append(pv.getName()).append("|").append(pv.getValue()).append("\n");
        }
        prefs.put("path_variables_data", sb.toString());
        prefs.put("ignored_variables_data", ignoredVariables);
    }
}
