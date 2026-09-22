package dev.lumina.plugin;

import dev.lumina.notification.Notification;
import dev.lumina.notification.NotificationGroup;
import dev.lumina.notification.NotificationService;
import dev.lumina.notification.NotificationType;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Central manager for plugins required by the project.
 */
public class RequiredPluginsManager {

    private static final RequiredPluginsManager INSTANCE = new RequiredPluginsManager();

    private final Preferences prefs = Preferences.userNodeForPackage(RequiredPluginsManager.class);
    private final List<RequiredPlugin> requiredPlugins = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    // Standard available plugins (matching media_1790045975642.png and plugin ecosystem)
    private static final List<String> STANDARD_PLUGIN_NAMES = List.of(
            "Angular",
            "AOP Pointcut Language",
            "Apache Velocity",
            "Artifacts Repository Search",
            "Async Profiler for IDE Performance Testing",
            "Backup and Sync",
            "Bytecode Viewer",
            "Chinese (Simplified) Language Pack / 中文语言包",
            "Go",
            "Kotlin",
            "Python",
            "Rust",
            "Scala",
            "Spring Boot"
    );

    private RequiredPluginsManager() {
        loadPreferences();
    }

    public static RequiredPluginsManager getInstance() {
        return INSTANCE;
    }

    public synchronized List<RequiredPlugin> getRequiredPlugins() {
        return new ArrayList<>(requiredPlugins);
    }

    public synchronized List<String> getAvailablePluginNames() {
        Set<String> set = new LinkedHashSet<>(STANDARD_PLUGIN_NAMES);
        try {
            PluginRegistry reg = PluginRegistry.getInstance();
            if (reg != null && reg.getBuiltInPlugins() != null) {
                for (PluginManifest m : reg.getBuiltInPlugins()) {
                    if (m != null && m.getName() != null && !m.getName().isBlank()) {
                        set.add(m.getName());
                    }
                }
            }
        } catch (Throwable ignored) {}
        List<String> list = new ArrayList<>(set);
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    public synchronized void addRequiredPlugin(RequiredPlugin plugin) {
        if (plugin == null) return;
        // Check if already exists; if so, replace
        requiredPlugins.removeIf(p -> p.getPluginName().equalsIgnoreCase(plugin.getPluginName())
                || p.getPluginId().equalsIgnoreCase(plugin.getPluginId()));
        requiredPlugins.add(plugin);
        savePreferences();
        notifyListeners();
    }

    public synchronized void removeRequiredPlugin(RequiredPlugin plugin) {
        if (plugin == null) return;
        if (requiredPlugins.remove(plugin)) {
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void updateRequiredPlugin(RequiredPlugin oldPlugin, RequiredPlugin newPlugin) {
        if (oldPlugin == null || newPlugin == null) return;
        int idx = requiredPlugins.indexOf(oldPlugin);
        if (idx != -1) {
            requiredPlugins.set(idx, newPlugin);
        } else {
            requiredPlugins.add(newPlugin);
        }
        savePreferences();
        notifyListeners();
    }

    public synchronized void clear() {
        requiredPlugins.clear();
        savePreferences();
        notifyListeners();
    }

    /**
     * Validates required plugins against currently installed plugins in PluginRegistry.
     * Returns list of human-readable diagnostic status strings.
     */
    public synchronized List<String> validateRequirements() {
        List<String> issues = new ArrayList<>();
        PluginRegistry registry = PluginRegistry.getInstance();

        for (RequiredPlugin req : requiredPlugins) {
            PluginManifest installed = null;
            if (registry != null) {
                for (PluginManifest m : registry.getBuiltInPlugins()) {
                    if (m.getId().equalsIgnoreCase(req.getPluginId()) ||
                            m.getName().equalsIgnoreCase(req.getPluginName())) {
                        installed = m;
                        break;
                    }
                }
            }

            if (installed == null) {
                issues.add("Required plugin '" + req.getPluginName() + "' is not installed.");
            } else if (!req.isVersionCompatible(installed.getVersion())) {
                issues.add("Required plugin '" + req.getPluginName() + "' version " +
                        installed.getVersion() + " does not satisfy constraints (min: " +
                        (req.getMinVersion().isEmpty() ? "<any>" : req.getMinVersion()) +
                        ", max: " +
                        (req.getMaxVersion().isEmpty() ? "<any>" : req.getMaxVersion()) + ").");
            }
        }
        return issues;
    }

    /**
     * Sends IDE notification if required plugins are missing or out of version range.
     */
    public synchronized void checkAndNotify() {
        List<String> issues = validateRequirements();
        if (!issues.isEmpty()) {
            try {
                NotificationService ns = NotificationService.getInstance();
                NotificationGroup group = ns.getGroup("Plugins");
                if (group == null) {
                    group = new NotificationGroup("Plugins", "Plugin Updates & Requirements");
                    ns.registerGroup(group);
                }
                String message = String.join("\n", issues);
                ns.notify(new Notification("Plugins", "Required Plugins Check", message, NotificationType.WARNING));
            } catch (Throwable ignored) {}
        }
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
        requiredPlugins.clear();
        String data = prefs.get("required_plugins_data", "");
        if (!data.isBlank()) {
            String[] lines = data.split("\n");
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 4) {
                    requiredPlugins.add(new RequiredPlugin(parts[0], parts[1], parts[2], parts[3]));
                } else if (parts.length >= 1) {
                    requiredPlugins.add(new RequiredPlugin(parts[0]));
                }
            }
        }
    }

    private void savePreferences() {
        StringBuilder sb = new StringBuilder();
        for (RequiredPlugin p : requiredPlugins) {
            sb.append(p.getPluginId()).append("|")
                    .append(p.getPluginName()).append("|")
                    .append(p.getMinVersion()).append("|")
                    .append(p.getMaxVersion()).append("\n");
        }
        prefs.put("required_plugins_data", sb.toString());
    }
}
