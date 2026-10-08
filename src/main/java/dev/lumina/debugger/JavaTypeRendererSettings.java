package dev.lumina.debugger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings for Build, Execution, Deployment > Debugger > Data Views > Java Type Renderers (Image 5).
 */
public class JavaTypeRendererSettings implements Cloneable {

    private List<JavaTypeRenderer> renderers = new ArrayList<>();

    public JavaTypeRendererSettings() {
        initDefaults();
    }

    public JavaTypeRendererSettings(JavaTypeRendererSettings other) {
        if (other != null) {
            this.renderers = new ArrayList<>();
            for (JavaTypeRenderer r : other.renderers) {
                this.renderers.add(r.clone());
            }
        }
    }

    public void initDefaults() {
        renderers = new ArrayList<>();
        // Default "unnamed" renderer as shown in Image 5
        JavaTypeRenderer unnamed = new JavaTypeRenderer("unnamed", "java.lang.Object");
        unnamed.setEnabled(true);
        unnamed.setShowTypeAndObjectId(true);
        unnamed.setNodeRendererType(JavaTypeRenderer.NodeRendererType.DEFAULT);
        unnamed.setExpandRendererType(JavaTypeRenderer.ExpandRendererType.DEFAULT);
        unnamed.setAppendDefaultChildren(false);
        renderers.add(unnamed);
    }

    public List<JavaTypeRenderer> getRenderers() {
        return renderers;
    }

    public void setRenderers(List<JavaTypeRenderer> renderers) {
        this.renderers = renderers != null ? new ArrayList<>(renderers) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaTypeRendererSettings that)) return false;
        return Objects.equals(renderers, that.renderers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(renderers);
    }

    @Override
    public JavaTypeRendererSettings clone() {
        return new JavaTypeRendererSettings(this);
    }
}
