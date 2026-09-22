package dev.lumina.git;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Manages VCS directory mappings matching IntelliJ IDEA's "Version Control > Directory Mappings".
 * Tracks whether a directory (or <Project>) is managed by Git, Mercurial, Perforce, Subversion, or <none>.
 * Provides automatic repository detection and Java Preferences persistence.
 */
public class VcsDirectoryMappingManager {

    public static final String PROJECT_MAPPING = "<Project>";
    public static final String NONE_VCS = "<none>";
    public static final List<String> SUPPORTED_VCS = List.of("Git", "Mercurial", "Perforce", "Subversion", NONE_VCS);

    public static final class VcsMapping {
        private String directory;
        private String vcs;

        public VcsMapping(String directory, String vcs) {
            this.directory = directory != null ? directory.trim() : PROJECT_MAPPING;
            this.vcs = vcs != null ? vcs.trim() : "Git";
        }

        public String getDirectory() { return directory; }
        public void setDirectory(String dir) { this.directory = dir; }

        public String getVcs() { return vcs; }
        public void setVcs(String v) { this.vcs = v; }

        public boolean isProject() {
            return PROJECT_MAPPING.equalsIgnoreCase(directory);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VcsMapping that)) return false;
            return Objects.equals(directory, that.directory) && Objects.equals(vcs, that.vcs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(directory, vcs);
        }
    }

    private static final VcsDirectoryMappingManager INSTANCE = new VcsDirectoryMappingManager();

    private final Preferences prefs = Preferences.userNodeForPackage(VcsDirectoryMappingManager.class);
    private final List<Runnable> listeners = new ArrayList<>();
    private final List<VcsMapping> mappings = new ArrayList<>();

    private boolean automaticMappingDetection = true;
    private int detectedRepositoriesCount = 1;
    private Path currentProjectPath;

    private VcsDirectoryMappingManager() {
        loadPreferences();
    }

    public static VcsDirectoryMappingManager getInstance() {
        return INSTANCE;
    }

    public synchronized void setCurrentProjectPath(Path projectPath) {
        this.currentProjectPath = projectPath;
        if (automaticMappingDetection && projectPath != null) {
            detectRepositories(projectPath);
        }
    }

    public synchronized Path getCurrentProjectPath() {
        return currentProjectPath;
    }

    public synchronized List<VcsMapping> getMappings() {
        return Collections.unmodifiableList(new ArrayList<>(mappings));
    }

    public synchronized void setMappings(List<VcsMapping> newMappings) {
        mappings.clear();
        if (newMappings != null) {
            mappings.addAll(newMappings);
        }
        ensureProjectMapping();
        savePreferences();
        notifyListeners();
    }

    public synchronized void addMapping(String directory, String vcs) {
        if (directory == null || directory.isBlank()) return;
        String dir = directory.trim();
        String v = (vcs == null || vcs.isBlank()) ? "Git" : vcs.trim();

        // Check if exists
        for (VcsMapping m : mappings) {
            if (m.getDirectory().equalsIgnoreCase(dir)) {
                m.setVcs(v);
                savePreferences();
                notifyListeners();
                return;
            }
        }
        mappings.add(new VcsMapping(dir, v));
        savePreferences();
        notifyListeners();
    }

    public synchronized void updateMapping(int index, String directory, String vcs) {
        if (index >= 0 && index < mappings.size()) {
            VcsMapping m = mappings.get(index);
            m.setDirectory(directory);
            m.setVcs(vcs);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void removeMapping(int index) {
        if (index >= 0 && index < mappings.size()) {
            VcsMapping m = mappings.get(index);
            if (m.isProject()) {
                // <Project> cannot be removed, but VCS can be set to <none>
                m.setVcs(NONE_VCS);
            } else {
                mappings.remove(index);
            }
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isAutomaticMappingDetection() {
        return automaticMappingDetection;
    }

    public synchronized void setAutomaticMappingDetection(boolean val) {
        if (this.automaticMappingDetection != val) {
            this.automaticMappingDetection = val;
            savePreferences();
            notifyListeners();
            if (val && currentProjectPath != null) {
                detectRepositories(currentProjectPath);
            }
        }
    }

    public synchronized int getDetectedRepositoriesCount() {
        return detectedRepositoriesCount;
    }

    public synchronized void detectRepositories(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            detectedRepositoriesCount = 0;
            return;
        }
        int count = 0;
        try {
            if (Files.isDirectory(root.resolve(".git"))) count++;
            if (Files.isDirectory(root.resolve(".hg"))) count++;
            if (Files.isDirectory(root.resolve(".svn"))) count++;

            // Check immediate subdirectories for nested repositories
            try (var stream = Files.list(root)) {
                for (Path child : stream.toList()) {
                    if (Files.isDirectory(child)) {
                        if (Files.isDirectory(child.resolve(".git")) ||
                            Files.isDirectory(child.resolve(".hg")) ||
                            Files.isDirectory(child.resolve(".svn"))) {
                            count++;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        this.detectedRepositoriesCount = Math.max(1, count);
        notifyListeners();
    }

    public synchronized void revertToDefaults() {
        automaticMappingDetection = true;
        detectedRepositoriesCount = 1;
        mappings.clear();
        mappings.add(new VcsMapping(PROJECT_MAPPING, "Git"));
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

    private void ensureProjectMapping() {
        boolean hasProject = false;
        for (VcsMapping m : mappings) {
            if (m.isProject()) {
                hasProject = true;
                break;
            }
        }
        if (!hasProject) {
            mappings.add(0, new VcsMapping(PROJECT_MAPPING, "Git"));
        }
    }

    private void loadPreferences() {
        automaticMappingDetection = prefs.getBoolean("vcs_auto_mapping_detection", true);
        String raw = prefs.get("vcs_directory_mappings", "");
        mappings.clear();
        if (!raw.isBlank()) {
            String[] entries = raw.split(";;;");
            for (String e : entries) {
                String[] parts = e.split(":::", 2);
                if (parts.length == 2) {
                    mappings.add(new VcsMapping(parts[0], parts[1]));
                }
            }
        }
        ensureProjectMapping();
    }

    private void savePreferences() {
        prefs.putBoolean("vcs_auto_mapping_detection", automaticMappingDetection);
        List<String> entries = new ArrayList<>();
        for (VcsMapping m : mappings) {
            entries.add(m.getDirectory() + ":::" + m.getVcs());
        }
        prefs.put("vcs_directory_mappings", String.join(";;;", entries));
    }
}
