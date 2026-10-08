package dev.lumina.profiler;

import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a Java Profiler configuration in Lumina IDE (e.g. IntelliJ Profiler / Async Profiler).
 */
public class JavaProfilerConfig implements Cloneable {

    private String id;
    private String name;
    private String agentOptions;
    private String agentPath;
    private boolean collectNativeCalls;

    public JavaProfilerConfig() {
        this.id = UUID.randomUUID().toString();
        this.name = "IntelliJ Profiler";
        this.agentOptions = "event=wall,interval=10ms,jfrsync=profile";
        this.agentPath = "Bundled (Version: 4.1)";
        this.collectNativeCalls = false;
    }

    public JavaProfilerConfig(String name, String agentOptions, String agentPath, boolean collectNativeCalls) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.agentOptions = agentOptions;
        this.agentPath = agentPath;
        this.collectNativeCalls = collectNativeCalls;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgentOptions() {
        return agentOptions;
    }

    public void setAgentOptions(String agentOptions) {
        this.agentOptions = agentOptions;
    }

    public String getAgentPath() {
        return agentPath;
    }

    public void setAgentPath(String agentPath) {
        this.agentPath = agentPath;
    }

    public boolean isCollectNativeCalls() {
        return collectNativeCalls;
    }

    public void setCollectNativeCalls(boolean collectNativeCalls) {
        this.collectNativeCalls = collectNativeCalls;
    }

    @Override
    public JavaProfilerConfig clone() {
        try {
            return (JavaProfilerConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            JavaProfilerConfig copy = new JavaProfilerConfig();
            copy.id = this.id;
            copy.name = this.name;
            copy.agentOptions = this.agentOptions;
            copy.agentPath = this.agentPath;
            copy.collectNativeCalls = this.collectNativeCalls;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaProfilerConfig that = (JavaProfilerConfig) o;
        return collectNativeCalls == that.collectNativeCalls &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(agentOptions, that.agentOptions) &&
                Objects.equals(agentPath, that.agentPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, agentOptions, agentPath, collectNativeCalls);
    }

    @Override
    public String toString() {
        return name != null && !name.isBlank() ? name : "Unnamed Profiler";
    }
}
