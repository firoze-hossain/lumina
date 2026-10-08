package dev.lumina.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration model for Scala Compiler settings in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791449910603_f5140118.png:
 *  - Incrementality type: Zinc / Standard
 *  - Profiles tree with modules
 *  - Compile order
 *  - Features (Dynamics, Postfix operator notation, Reflective calls, Implicit conversions,
 *              Higher-kinded types, Existential types, Macros, Experimental Features)
 *  - Options (Enable warnings, Deprecation warnings, Unchecked warnings, Feature warnings,
 *             Optimise bytecode, Explain type errors, Enable specialization, Enable continuations)
 *  - Debugging info level
 *  - Additional compiler options
 *  - Compiler plugins table
 */
public class ScalaCompilerSettings implements Cloneable {

    private String incrementalityType = "Zinc";
    private List<ScalaCompilerProfile> profiles = new ArrayList<>();

    public ScalaCompilerSettings() {
        initDefaults();
    }

    public ScalaCompilerSettings(ScalaCompilerSettings other) {
        if (other != null) {
            this.incrementalityType = other.incrementalityType;
            this.profiles = new ArrayList<>();
            for (ScalaCompilerProfile p : other.profiles) {
                this.profiles.add(p.clone());
            }
        } else {
            initDefaults();
        }
    }

    private void initDefaults() {
        profiles = new ArrayList<>();
        ScalaCompilerProfile def = new ScalaCompilerProfile("Default");
        String moduleName = ScalaCompilerProfile.detectCurrentModuleName();
        def.getModules().add(moduleName);
        profiles.add(def);
    }

    public String getIncrementalityType() { return incrementalityType; }
    public void setIncrementalityType(String incrementalityType) {
        this.incrementalityType = incrementalityType != null ? incrementalityType : "Zinc";
    }

    public List<ScalaCompilerProfile> getProfiles() {
        return profiles != null ? profiles : new ArrayList<>();
    }

    public void setProfiles(List<ScalaCompilerProfile> profiles) {
        this.profiles = profiles != null ? new ArrayList<>(profiles) : new ArrayList<>();
    }

    @Override
    public ScalaCompilerSettings clone() {
        return new ScalaCompilerSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaCompilerSettings that = (ScalaCompilerSettings) o;
        return Objects.equals(incrementalityType, that.incrementalityType) &&
                Objects.equals(profiles, that.profiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incrementalityType, profiles);
    }
}
