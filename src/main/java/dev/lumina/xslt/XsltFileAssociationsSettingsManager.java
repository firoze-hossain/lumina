package dev.lumina.xslt;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > XSLT File Associations in Lumina IDE.
 */
public class XsltFileAssociationsSettingsManager {

    public static final String KEY_XSLT_FILE_ASSOCIATIONS = "xslt.file.associations";

    private static volatile XsltFileAssociationsSettingsManager instance;
    private XsltFileAssociationsSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private XsltFileAssociationsSettingsManager() {
        loadSettings();
    }

    public static XsltFileAssociationsSettingsManager getInstance() {
        if (instance == null) {
            synchronized (XsltFileAssociationsSettingsManager.class) {
                if (instance == null) {
                    instance = new XsltFileAssociationsSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized XsltFileAssociationsSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(XsltFileAssociationsSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        XsltFileAssociationsSettings s = new XsltFileAssociationsSettings();

        String raw = Settings.get(KEY_XSLT_FILE_ASSOCIATIONS);
        if (raw != null && !raw.isBlank()) {
            List<XsltFileAssociation> list = new ArrayList<>();
            String[] entries = raw.split("###");
            for (String entry : entries) {
                if (!entry.isBlank()) {
                    String[] parts = entry.split("===", 2);
                    String xsltPath = parts[0];
                    List<String> files = new ArrayList<>();
                    if (parts.length > 1 && !parts[1].isBlank()) {
                        String[] fList = parts[1].split(";;;");
                        for (String f : fList) {
                            if (!f.isBlank()) {
                                files.add(f);
                            }
                        }
                    }
                    list.add(new XsltFileAssociation(xsltPath, files));
                }
            }
            s.setAssociations(list);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        StringBuilder sb = new StringBuilder();
        for (XsltFileAssociation a : currentSettings.getAssociations()) {
            if (sb.length() > 0) sb.append("###");
            sb.append(a.getXsltFilePath() != null ? a.getXsltFilePath() : "").append("===");
            List<String> files = a.getAssociatedFiles();
            for (int i = 0; i < files.size(); i++) {
                if (i > 0) sb.append(";;;");
                sb.append(files.get(i));
            }
        }
        Settings.put(KEY_XSLT_FILE_ASSOCIATIONS, sb.toString());
    }

    public synchronized List<String> getAssociatedFiles(String xsltPath) {
        if (currentSettings == null) {
            loadSettings();
        }
        XsltFileAssociation assoc = currentSettings.getAssociationFor(xsltPath);
        return assoc != null ? new ArrayList<>(assoc.getAssociatedFiles()) : new ArrayList<>();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {}
        }
    }
}
