package dev.lumina.inspections;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active and available inspection profiles with persistence.
 */
public final class InspectionProfileManager {

    private static final InspectionProfileManager INSTANCE = new InspectionProfileManager();

    public static InspectionProfileManager getInstance() {
        return INSTANCE;
    }

    public interface ProfileChangeListener {
        void onProfileChanged(InspectionProfile profile);
    }

    private final Map<String, InspectionProfile> profiles = new LinkedHashMap<>();
    private InspectionProfile activeProfile;
    private final List<ProfileChangeListener> listeners = new CopyOnWriteArrayList<>();

    private InspectionProfileManager() {
        initDefaultProfiles();
        load();
    }

    private void initDefaultProfiles() {
        InspectionProfile projectDefault = new InspectionProfile("Project Default", true);
        InspectionProfile ideDefault = new InspectionProfile("Default", false);

        profiles.put(projectDefault.getName(), projectDefault);
        profiles.put(ideDefault.getName(), ideDefault);
        activeProfile = projectDefault;
    }

    public synchronized InspectionProfile getActiveProfile() {
        if (activeProfile == null) {
            activeProfile = profiles.get("Project Default");
            if (activeProfile == null && !profiles.isEmpty()) {
                activeProfile = profiles.values().iterator().next();
            }
        }
        return activeProfile;
    }

    public synchronized void setActiveProfile(InspectionProfile profile) {
        if (profile == null) return;
        this.activeProfile = profile;
        profiles.put(profile.getName(), profile);
        save();
        notifyListeners();
    }

    public synchronized void setActiveProfileName(String name) {
        if (name == null) return;
        for (InspectionProfile p : profiles.values()) {
            if (p.getName().equalsIgnoreCase(name.trim())) {
                setActiveProfile(p);
                return;
            }
        }
    }

    public synchronized List<InspectionProfile> getProfiles() {
        return Collections.unmodifiableList(new ArrayList<>(profiles.values()));
    }

    public synchronized InspectionProfile getProfile(String name) {
        if (name == null) return null;
        for (InspectionProfile p : profiles.values()) {
            if (p.getName().equalsIgnoreCase(name.trim())) {
                return p;
            }
        }
        return null;
    }

    public synchronized InspectionProfile createProfile(String name, boolean projectLevel, InspectionProfile template) {
        if (name == null || name.isBlank()) {
            name = "Custom Profile";
        }
        name = name.trim();
        if (template != null) {
            InspectionProfile cloned = template.cloneProfile(name, projectLevel);
            profiles.put(name, cloned);
            save();
            return cloned;
        } else {
            InspectionProfile p = new InspectionProfile(name, projectLevel);
            profiles.put(name, p);
            save();
            return p;
        }
    }

    public synchronized boolean renameProfile(String oldName, String newName) {
        if (oldName == null || newName == null || newName.isBlank()) return false;
        InspectionProfile p = profiles.remove(oldName);
        if (p != null) {
            p.setName(newName.trim());
            profiles.put(p.getName(), p);
            if (activeProfile != null && activeProfile.getName().equals(oldName)) {
                activeProfile = p;
            }
            save();
            notifyListeners();
            return true;
        }
        return false;
    }

    public synchronized boolean deleteProfile(String name) {
        if ("Project Default".equalsIgnoreCase(name) || "Default".equalsIgnoreCase(name)) {
            // Cannot delete built-in defaults
            return false;
        }
        InspectionProfile removed = profiles.remove(name);
        if (removed != null) {
            if (activeProfile != null && activeProfile.getName().equals(name)) {
                activeProfile = profiles.get("Project Default");
            }
            save();
            notifyListeners();
            return true;
        }
        return false;
    }

    public synchronized void addListener(ProfileChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(ProfileChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        InspectionProfile p = getActiveProfile();
        for (ProfileChangeListener listener : listeners) {
            try {
                listener.onProfileChanged(p);
            } catch (Throwable ignored) {
            }
        }
    }

    public synchronized void load() {
        try {
            String activeName = Settings.get("inspections.profile.active");
            if (activeName != null && !activeName.isBlank()) {
                setActiveProfileName(activeName);
            }
            for (InspectionProfile p : profiles.values()) {
                String prefix = "inspections.profile." + p.getName().replace(" ", "_") + ".";
                String disableNewStr = Settings.get(prefix + "disable_new");
                if (disableNewStr != null) {
                    p.setDisableNewInspections(Boolean.parseBoolean(disableNewStr));
                }

                for (InspectionTool tool : InspectionRegistry.getInstance().getAllTools()) {
                    String toolPrefix = prefix + "tool." + tool.getId() + ".";
                    String enabledStr = Settings.get(toolPrefix + "enabled");
                    if (enabledStr != null) {
                        boolean enabled = Boolean.parseBoolean(enabledStr);
                        String sevName = Settings.get(toolPrefix + "severity");
                        String scope = Settings.get(toolPrefix + "scope");
                        String highlighting = Settings.get(toolPrefix + "highlighting");

                        p.putState(tool.getId(), new InspectionProfile.ToolState(
                                enabled,
                                sevName != null ? HighlightSeverity.fromDisplayName(sevName) : tool.getDefaultSeverity(),
                                scope != null ? scope : tool.getDefaultScope(),
                                highlighting != null ? highlighting : tool.getDefaultHighlighting()
                        ));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public synchronized void save() {
        try {
            if (activeProfile != null) {
                Settings.put("inspections.profile.active", activeProfile.getName());
            }
            for (InspectionProfile p : profiles.values()) {
                String prefix = "inspections.profile." + p.getName().replace(" ", "_") + ".";
                Settings.put(prefix + "disable_new", Boolean.toString(p.isDisableNewInspections()));

                for (Map.Entry<String, InspectionProfile.ToolState> entry : p.getToolStates().entrySet()) {
                    String toolPrefix = prefix + "tool." + entry.getKey() + ".";
                    InspectionProfile.ToolState state = entry.getValue();
                    Settings.put(toolPrefix + "enabled", Boolean.toString(state.isEnabled()));
                    Settings.put(toolPrefix + "severity", state.getSeverity().name());
                    Settings.put(toolPrefix + "scope", state.getScope());
                    Settings.put(toolPrefix + "highlighting", state.getHighlighting());
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
