package dev.lumina.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central plugin registry - manages language/generator plugins.
 */
public class PluginRegistry {
    private static final Path LUMINA_DIR = Path.of(System.getProperty("user.home"), ".lumina");
    private static final Path PLUGIN_DIR = LUMINA_DIR.resolve("plugins");
    private static final Path METADATA_FILE = LUMINA_DIR.resolve("plugins.json");

    private static PluginRegistry instance;
    private final ConcurrentHashMap<String, PluginManifest> plugins = new ConcurrentHashMap<>();

    private PluginRegistry() {
        try {
            Files.createDirectories(PLUGIN_DIR);
        } catch (IOException ignored) {}
        loadLocalPlugins();
        ensureBuiltInPlugins();
    }

    public static PluginRegistry getInstance() {
        if (instance == null) {
            synchronized (PluginRegistry.class) {
                if (instance == null) {
                    instance = new PluginRegistry();
                }
            }
        }
        return instance;
    }

    private void ensureBuiltInPlugins() {
        for (PluginManifest p : getBuiltInPlugins()) {
            plugins.putIfAbsent(p.getId(), p);
        }
        saveMetadata();
    }

    private void loadLocalPlugins() {
        try {
            if (Files.exists(METADATA_FILE)) {
                String json = Files.readString(METADATA_FILE);
                Type listType = new TypeToken<List<PluginManifest>>(){}.getType();
                List<PluginManifest> loaded = new Gson().fromJson(json, listType);
                for (PluginManifest p : loaded) {
                    plugins.put(p.getId(), p);
                }
            }
        } catch (IOException ignored) {}

        try {
            if (Files.exists(PLUGIN_DIR)) {
                try (var stream = Files.list(PLUGIN_DIR)) {
                    stream.filter(p -> p.toString().endsWith(".jar"))
                          .forEach(this::loadPluginFromJar);
                }
            }
        } catch (IOException ignored) {}
    }

    private void loadPluginFromJar(Path jarPath) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath.toFile())) {
            java.util.jar.JarEntry entry = jar.getJarEntry("plugin.json");
            if (entry != null) {
                String json = new String(jar.getInputStream(entry).readAllBytes());
                Gson gson = new Gson();
                PluginManifest manifest = gson.fromJson(json, PluginManifest.class);
                manifest.setInstalled(true);
                plugins.put(manifest.getId(), manifest);
            }
        } catch (IOException ignored) {}
    }

    /**
     * Language plugins only - matches the first image.
     */
    public List<PluginManifest> getBuiltInPlugins() {
        List<PluginManifest> builtIn = new ArrayList<>();

        // Language plugins (from the first image)
        builtIn.add(new PluginManifest(
            "go", "Go", "2024.1.0",
            "Intelligent Go language support with modules, debugging, and testing tools.",
            "JetBrains", "Languages", null,
            List.of("go", "golang"), "https://lumina.dev/plugins/go", "G",
            "4.5", 500000, true, true, "#00ADD8"
        ));

        builtIn.add(new PluginManifest(
            "php", "PHP", "2024.1.2",
            "PHP 5.3-8.4 editing and debugging, PHPUnit, Smarty, Twig and various frameworks support.",
            "JetBrains", "Languages", null,
            List.of("php", "laravel", "symfony"), "https://lumina.dev/plugins/php", "PHP",
            "4.4", 700000, true, true, "#8892BF"
        ));

        builtIn.add(new PluginManifest(
            "python", "Python", "2024.1.2",
            "Professional Python development with debugging, testing, and virtual environment support.",
            "JetBrains", "Languages", null,
            List.of("python", "django", "flask"), "https://lumina.dev/plugins/python", "P",
            "4.6", 900000, false, true, "#3776AB"
        ));

        builtIn.add(new PluginManifest(
            "plugin-devkit", "Plugin DevKit", "2024.1.0",
            "Tools for developing IntelliJ Platform plugins and extensions.",
            "JetBrains", "Development", null,
            List.of("plugin", "development", "sdk"), "https://lumina.dev/plugins/plugin-devkit", "🔌",
            "4.3", 300000, false, true, "#6C6C6C"
        ));

        builtIn.add(new PluginManifest(
            "ruby", "Ruby", "2024.1.1",
            "Ruby and Rails development with testing, debugging, and code navigation.",
            "JetBrains", "Languages", null,
            List.of("ruby", "rails"), "https://lumina.dev/plugins/ruby", "R",
            "4.3", 400000, false, true, "#CC342D"
        ));

        builtIn.add(new PluginManifest(
            "scala", "Scala", "2024.1.0",
            "Scala language support with SBT, Maven, and advanced type inference.",
            "JetBrains", "Languages", null,
            List.of("scala", "functional"), "https://lumina.dev/plugins/scala", "S",
            "4.2", 300000, false, true, "#DC322F"
        ));

        return builtIn;
    }

    public List<PluginManifest> getAvailablePlugins() {
        List<PluginManifest> all = new ArrayList<>(plugins.values());
        for (PluginManifest p : getBuiltInPlugins()) {
            if (all.stream().noneMatch(existing -> existing.getId().equals(p.getId()))) {
                all.add(p);
            }
        }
        return all;
    }

    public List<PluginManifest> getInstalledPlugins() {
        return plugins.values().stream()
                .filter(PluginManifest::isInstalled)
                .toList();
    }

    public boolean installPlugin(String pluginId) {
        PluginManifest plugin = findPlugin(pluginId);
        if (plugin != null) {
            plugin.setInstalled(true);
            plugins.put(pluginId, plugin);
            saveMetadata();
            return true;
        }
        return false;
    }

    public boolean uninstallPlugin(String pluginId) {
        PluginManifest plugin = plugins.get(pluginId);
        if (plugin != null && !plugin.isBuiltIn()) {
            plugin.setInstalled(false);
            plugins.remove(pluginId);
            saveMetadata();
            return true;
        }
        return false;
    }

    public PluginManifest findPlugin(String pluginId) {
        PluginManifest p = plugins.get(pluginId);
        if (p != null) return p;
        for (PluginManifest builtIn : getBuiltInPlugins()) {
            if (builtIn.getId().equals(pluginId)) {
                return builtIn;
            }
        }
        return null;
    }

    public boolean isInstalled(String pluginId) {
        PluginManifest p = plugins.get(pluginId);
        return p != null && p.isInstalled();
    }

    private void saveMetadata() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<PluginManifest> installed = new ArrayList<>();
            for (PluginManifest p : plugins.values()) {
                if (p.isInstalled() && !p.isBuiltIn()) {
                    installed.add(p);
                }
            }
            String json = gson.toJson(installed);
            Files.writeString(METADATA_FILE, json);
        } catch (IOException ignored) {}
    }

    public Path getPluginDir() {
        return PLUGIN_DIR;
    }
}