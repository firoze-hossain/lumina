package dev.lumina.schemas;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Schemas and DTDs > XML Catalog settings in Lumina IDE.
 */
public class XmlCatalogSettingsManager {

    public static final String KEY_CATALOG_PROPERTY_FILE = "schemas.xml.catalog.property.file";

    private static volatile XmlCatalogSettingsManager instance;
    private XmlCatalogSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private XmlCatalogSettingsManager() {
        loadSettings();
    }

    public static XmlCatalogSettingsManager getInstance() {
        if (instance == null) {
            synchronized (XmlCatalogSettingsManager.class) {
                if (instance == null) {
                    instance = new XmlCatalogSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized XmlCatalogSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(XmlCatalogSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        XmlCatalogSettings s = new XmlCatalogSettings();
        String file = Settings.get(KEY_CATALOG_PROPERTY_FILE);
        if (file != null) {
            s.setCatalogPropertyFile(file);
        }
        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_CATALOG_PROPERTY_FILE, currentSettings.getCatalogPropertyFile());
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
