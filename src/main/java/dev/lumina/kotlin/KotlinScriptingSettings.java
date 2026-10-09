package dev.lumina.kotlin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings model for Kotlin Scripting definitions in Lumina IDE.
 */
public class KotlinScriptingSettings {

    private List<KotlinScriptDefinition> definitions = new ArrayList<>();

    public KotlinScriptingSettings() {
        initDefaults();
    }

    public KotlinScriptingSettings(List<KotlinScriptDefinition> definitions) {
        if (definitions != null) {
            for (KotlinScriptDefinition def : definitions) {
                this.definitions.add(def.copy());
            }
        } else {
            initDefaults();
        }
    }

    public KotlinScriptingSettings(KotlinScriptingSettings other) {
        if (other != null && other.definitions != null) {
            for (KotlinScriptDefinition def : other.definitions) {
                this.definitions.add(def.copy());
            }
        } else {
            initDefaults();
        }
    }

    public void initDefaults() {
        this.definitions.clear();
        this.definitions.add(new KotlinScriptDefinition("MainKtsScript", ".main.kts", true, false));
        this.definitions.add(new KotlinScriptDefinition("Kotlin Notebooks", ".jupyter.kts", true, false));
        this.definitions.add(new KotlinScriptDefinition("Qodana .inspection.kts", ".inspection.kts", true, false));
        this.definitions.add(new KotlinScriptDefinition("Default Kotlin Script", ".kts", true, true));
    }

    public KotlinScriptingSettings copy() {
        return new KotlinScriptingSettings(this);
    }

    public List<KotlinScriptDefinition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<KotlinScriptDefinition> definitions) {
        this.definitions.clear();
        if (definitions != null) {
            for (KotlinScriptDefinition def : definitions) {
                this.definitions.add(def.copy());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KotlinScriptingSettings that = (KotlinScriptingSettings) o;
        return Objects.equals(definitions, that.definitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitions);
    }
}
