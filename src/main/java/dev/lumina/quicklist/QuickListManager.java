package dev.lumina.quicklist;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Central singleton managing all IDE Quick Lists.
 * Strictly matches media_1790045975645.png, media_1790045975646.png,
 * media_1790045975647.png, and media_1790045975661.png.
 */
public class QuickListManager {

    private static final QuickListManager INSTANCE = new QuickListManager();

    private final Preferences prefs = Preferences.userNodeForPackage(QuickListManager.class);
    private final List<QuickList> quickLists = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private QuickListManager() {
        loadPreferences();
        if (quickLists.isEmpty()) {
            initDefaults();
        }
    }

    public static QuickListManager getInstance() {
        return INSTANCE;
    }

    public synchronized List<QuickList> getQuickLists() {
        return new ArrayList<>(quickLists);
    }

    public synchronized QuickList getQuickList(String name) {
        if (name == null) return null;
        for (QuickList ql : quickLists) {
            if (ql.getName().equalsIgnoreCase(name.trim())) {
                return ql;
            }
        }
        return null;
    }

    public synchronized void addQuickList(QuickList ql) {
        if (ql == null) return;
        quickLists.removeIf(existing -> existing.getName().equalsIgnoreCase(ql.getName()));
        quickLists.add(ql);
        savePreferences();
        notifyListeners();
    }

    public synchronized void removeQuickList(QuickList ql) {
        if (ql == null) return;
        if (quickLists.remove(ql)) {
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void revertToDefaults() {
        quickLists.clear();
        initDefaults();
        savePreferences();
        notifyListeners();
    }

    public synchronized void save() {
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

    /**
     * Initializes the authentic default "Deployment" Quick List from media_1790045975661.png.
     */
    private void initDefaults() {
        QuickList deployment = new QuickList("Deployment", "Deployment actions");
        deployment.addItem(QuickListItem.action("PublishGroup.Upload", "Upload to Default Server", "📤"));
        deployment.addItem(QuickListItem.action("PublishGroup.UploadTo", "Upload To...", ""));
        deployment.addItem(QuickListItem.action("PublishGroup.Download", "Download from Default Server", "📥"));
        deployment.addItem(QuickListItem.action("PublishGroup.DownloadFrom", "Download From...", ""));
        deployment.addItem(QuickListItem.action("PublishGroup.DiffWith", "Compare Local File with Deployed Version", "↹"));
        deployment.addItem(QuickListItem.action("WebOpenInBrowser", "Open in default browser", "🌐"));
        deployment.addItem(QuickListItem.separator());
        deployment.addItem(QuickListItem.action("PublishGroup.SelectInServer", "Select in Remote Host", ""));
        deployment.addItem(QuickListItem.action("PublishGroup.BrowseServers", "Browse Remote Host", "🗄"));
        deployment.addItem(QuickListItem.action("PublishGroup.Directory", "Directory", "📁"));
        deployment.addItem(QuickListItem.action("PublishGroup.ChangePermissions", "Change Permissions...", "🔑"));
        deployment.addItem(QuickListItem.separator());
        deployment.addItem(QuickListItem.action("PublishGroup.Configuration", "Configuration...", ""));
        quickLists.add(deployment);
    }

    private void loadPreferences() {
        quickLists.clear();
        String raw = prefs.get("quick_lists_data", "");
        if (raw.isBlank()) return;

        String[] lines = raw.split("\n");
        QuickList current = null;
        for (String line : lines) {
            if (line.startsWith("QL:")) {
                String meta = line.substring(3);
                String[] parts = meta.split("\\|", 2);
                String name = parts[0];
                String desc = parts.length > 1 ? parts[1] : "";
                current = new QuickList(name, desc);
                quickLists.add(current);
            } else if (current != null && !line.isBlank()) {
                QuickListItem item = QuickListItem.deserialize(line);
                if (item != null) {
                    current.addItem(item);
                }
            }
        }
    }

    private void savePreferences() {
        StringBuilder sb = new StringBuilder();
        for (QuickList ql : quickLists) {
            sb.append("QL:").append(ql.getName()).append("|").append(ql.getDescription()).append("\n");
            for (QuickListItem item : ql.getItems()) {
                sb.append(item.serialize()).append("\n");
            }
        }
        prefs.put("quick_lists_data", sb.toString());
    }
}
