package dev.lumina.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration model for Annotation Processors profiles in Lumina IDE.
 */
public class AnnotationProcessingSettings implements Cloneable {

    private List<AnnotationProcessingProfile> profiles = new ArrayList<>();

    public AnnotationProcessingSettings() {
        initDefaultProfiles();
    }

    public AnnotationProcessingSettings(AnnotationProcessingSettings other) {
        if (other != null && other.profiles != null) {
            this.profiles = new ArrayList<>();
            for (AnnotationProcessingProfile p : other.profiles) {
                this.profiles.add(p.clone());
            }
        } else {
            initDefaultProfiles();
        }
    }

    private void initDefaultProfiles() {
        profiles = new ArrayList<>();

        AnnotationProcessingProfile def = new AnnotationProcessingProfile("Default");
        profiles.add(def);

        AnnotationProcessingProfile mavenDef = new AnnotationProcessingProfile("Maven default annotation processors profile");
        String moduleName = detectCurrentModuleName();
        mavenDef.getModules().add(moduleName);
        profiles.add(mavenDef);
    }

    /**
     * Dynamically determines current project module name without hardcoding.
     */
    public static String detectCurrentModuleName() {
        String dir = System.getProperty("user.dir", "");
        if (!dir.isBlank()) {
            File f = new File(dir);
            return f.getName();
        }
        return "lumina-ide";
    }

    public List<AnnotationProcessingProfile> getProfiles() {
        return profiles != null ? profiles : new ArrayList<>();
    }

    public void setProfiles(List<AnnotationProcessingProfile> profiles) {
        if (profiles != null) {
            this.profiles = new ArrayList<>(profiles);
        } else {
            this.profiles = new ArrayList<>();
        }
    }

    public AnnotationProcessingProfile getProfileByName(String name) {
        if (name == null || profiles == null) return null;
        for (AnnotationProcessingProfile p : profiles) {
            if (p.getName().equals(name)) return p;
        }
        return null;
    }

    @Override
    public AnnotationProcessingSettings clone() {
        return new AnnotationProcessingSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotationProcessingSettings that = (AnnotationProcessingSettings) o;
        return Objects.equals(profiles, that.profiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profiles);
    }
}
