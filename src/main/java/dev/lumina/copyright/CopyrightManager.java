package dev.lumina.copyright;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages copyright profiles, default project copyright, and scope mappings,
 * mirroring IntelliJ IDEA's dynamic Copyright configuration.
 */
public class CopyrightManager {

    public static final String NO_COPYRIGHT = "No copyright";

    private static final String CONFIG_DIR =
            System.getProperty("user.home") + File.separator + ".lumina";
    private static final String CONFIG_FILE =
            CONFIG_DIR + File.separator + "copyright.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final CopyrightManager INSTANCE = new CopyrightManager();

    private String defaultProjectCopyright = NO_COPYRIGHT;
    private final ObservableList<CopyrightProfile> profiles = FXCollections.observableArrayList();
    private final ObservableList<ScopeCopyrightMapping> scopeMappings = FXCollections.observableArrayList();

    private CopyrightManager() {
        load();
    }

    public static CopyrightManager getInstance() {
        return INSTANCE;
    }

    public String getDefaultProjectCopyright() {
        return defaultProjectCopyright != null ? defaultProjectCopyright : NO_COPYRIGHT;
    }

    public void setDefaultProjectCopyright(String defaultProjectCopyright) {
        this.defaultProjectCopyright = defaultProjectCopyright != null ? defaultProjectCopyright : NO_COPYRIGHT;
    }

    public ObservableList<CopyrightProfile> getProfiles() {
        return profiles;
    }

    public ObservableList<ScopeCopyrightMapping> getScopeMappings() {
        return scopeMappings;
    }

    public CopyrightProfile findProfileByName(String name) {
        if (name == null || name.isEmpty() || NO_COPYRIGHT.equalsIgnoreCase(name.trim())) {
            return null;
        }
        for (CopyrightProfile p : profiles) {
            if (p.getName().equalsIgnoreCase(name.trim())) {
                return p;
            }
        }
        return null;
    }

    public void addProfile(CopyrightProfile profile) {
        if (profile == null || profile.getName().isEmpty()) return;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getName().equalsIgnoreCase(profile.getName())) {
                profiles.set(i, profile);
                return;
            }
        }
        profiles.add(profile);
    }

    public boolean removeProfile(String name) {
        if (name == null) return false;
        boolean removed = profiles.removeIf(p -> p.getName().equalsIgnoreCase(name));
        if (removed) {
            if (Objects.equals(defaultProjectCopyright, name)) {
                defaultProjectCopyright = NO_COPYRIGHT;
            }
            scopeMappings.removeIf(m -> Objects.equals(m.getCopyright(), name));
        }
        return removed;
    }

    public void addScopeMapping(String scope, String profile) {
        if (scope == null || profile == null) return;
        scopeMappings.add(new ScopeCopyrightMapping(scope, profile));
    }

    public boolean removeScopeMapping(int index) {
        if (index >= 0 && index < scopeMappings.size()) {
            scopeMappings.remove(index);
            return true;
        }
        return false;
    }

    public boolean moveScopeMappingUp(int index) {
        if (index > 0 && index < scopeMappings.size()) {
            ScopeCopyrightMapping item = scopeMappings.remove(index);
            scopeMappings.add(index - 1, item);
            return true;
        }
        return false;
    }

    public boolean moveScopeMappingDown(int index) {
        if (index >= 0 && index < scopeMappings.size() - 1) {
            ScopeCopyrightMapping item = scopeMappings.remove(index);
            scopeMappings.add(index + 1, item);
            return true;
        }
        return false;
    }

    /**
     * Resolves copyright notice for a given file and project, replacing Velocity variables.
     */
    public String evaluateNotice(String profileName, String fileName, String projectName) {
        CopyrightProfile profile = findProfileByName(profileName);
        if (profile == null) return null;

        String raw = profile.getNotice();
        LocalDate now = LocalDate.now();

        return raw.replace("$today.year", String.valueOf(now.getYear()))
                .replace("$today.month", String.format("%02d", now.getMonthValue()))
                .replace("$today.day", String.format("%02d", now.getDayOfMonth()))
                .replace("$project.name", projectName != null ? projectName : "Project")
                .replace("$file.fileName", fileName != null ? fileName : "File");
    }

    public void resetToDefaults() {
        defaultProjectCopyright = NO_COPYRIGHT;
        profiles.clear();
        scopeMappings.clear();
    }

    public void save() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            ConfigState state = new ConfigState();
            state.defaultProjectCopyright = this.defaultProjectCopyright;
            state.profiles = new ArrayList<>(this.profiles);
            state.scopeMappings = new ArrayList<>(this.scopeMappings);

            try (FileWriter writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(state, writer);
            }
        } catch (IOException e) {
            // Log or ignore gracefully
        }
    }

    public void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            ConfigState state = GSON.fromJson(reader, ConfigState.class);
            if (state != null) {
                if (state.defaultProjectCopyright != null) {
                    this.defaultProjectCopyright = state.defaultProjectCopyright;
                }
                if (state.profiles != null) {
                    this.profiles.setAll(state.profiles);
                }
                if (state.scopeMappings != null) {
                    this.scopeMappings.setAll(state.scopeMappings);
                }
            }
        } catch (Exception e) {
            // Ignore parse errors and retain defaults
        }
    }

    private static class ConfigState {
        String defaultProjectCopyright;
        List<CopyrightProfile> profiles;
        List<ScopeCopyrightMapping> scopeMappings;
    }
}
