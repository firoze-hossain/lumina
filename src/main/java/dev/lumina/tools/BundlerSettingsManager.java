package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Ruby Bundler in Lumina IDE.
 */
public class BundlerSettingsManager {

    private static final String KEY_ALWAYS_INSTALL = "tools.bundler.always_install";
    private static final String KEY_USE_DEFAULT_ARGS = "tools.bundler.use_default_args";
    private static final String KEY_DEFAULT_ARGS = "tools.bundler.default_args";

    private static volatile BundlerSettingsManager instance;

    private BundlerSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private BundlerSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static BundlerSettingsManager getInstance() {
        if (instance == null) {
            synchronized (BundlerSettingsManager.class) {
                if (instance == null) {
                    instance = new BundlerSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized BundlerSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(BundlerSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new BundlerSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
    }

    /**
     * Dynamically checks if Gemfile.lock exists in the project workspace.
     */
    public boolean hasGemfileLock() {
        String userDir = System.getProperty("user.dir", ".");
        File lockFile = new File(userDir, "Gemfile.lock");
        return lockFile.exists() && lockFile.isFile();
    }

    /**
     * Detects the Bundler version specified in Gemfile.lock (under BUNDLED WITH section).
     */
    public String detectBundledVersion() {
        String userDir = System.getProperty("user.dir", ".");
        File lockFile = new File(userDir, "Gemfile.lock");
        if (lockFile.exists() && lockFile.isFile()) {
            try {
                List<String> lines = Files.readAllLines(lockFile.toPath());
                boolean foundHeader = false;
                for (String line : lines) {
                    if (line.trim().equals("BUNDLED WITH")) {
                        foundHeader = true;
                        continue;
                    }
                    if (foundHeader && !line.trim().isEmpty()) {
                        return line.trim();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Dynamically discovers Bundler executable locations on the system.
     */
    public List<String> discoverBundlerExecutables() {
        Set<String> paths = new LinkedHashSet<>();
        String[] candidates = {
                "/opt/homebrew/bin/bundle",
                "/usr/local/bin/bundle",
                "/usr/bin/bundle"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.canExecute()) {
                paths.add(f.getAbsolutePath());
            }
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File b = new File(dir, "bundle");
                if (b.exists() && b.canExecute()) {
                    paths.add(b.getAbsolutePath());
                }
            }
        }
        return new ArrayList<>(paths);
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private BundlerSettings loadSettings() {
        BundlerSettings settings = new BundlerSettings();
        String always = Settings.get(KEY_ALWAYS_INSTALL);
        if (always != null) {
            settings.setAlwaysInstallRequiredVersion(Boolean.parseBoolean(always));
        }
        String useArgs = Settings.get(KEY_USE_DEFAULT_ARGS);
        if (useArgs != null) {
            settings.setUseDefaultArguments(Boolean.parseBoolean(useArgs));
        }
        String args = Settings.get(KEY_DEFAULT_ARGS);
        if (args != null) {
            settings.setDefaultArguments(args);
        }
        return settings;
    }

    private void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_ALWAYS_INSTALL, String.valueOf(currentSettings.isAlwaysInstallRequiredVersion()));
        Settings.put(KEY_USE_DEFAULT_ARGS, String.valueOf(currentSettings.isUseDefaultArguments()));
        Settings.put(KEY_DEFAULT_ARGS, currentSettings.getDefaultArguments());
    }
}
