package dev.lumina.livetemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Group of Live Templates (e.g. "Angular", "Java", "Zen XSL").
 */
public class LiveTemplateGroup {

    private String name = "";
    private boolean enabled = true;
    private boolean builtin = true;
    private List<LiveTemplate> templates = new ArrayList<>();

    public LiveTemplateGroup() {
    }

    public LiveTemplateGroup(String name) {
        this.name = name != null ? name : "";
    }

    public LiveTemplateGroup(String name, boolean builtin) {
        this.name = name != null ? name : "";
        this.builtin = builtin;
    }

    public LiveTemplateGroup copy() {
        LiveTemplateGroup clone = new LiveTemplateGroup(this.name, this.builtin);
        clone.enabled = this.enabled;
        for (LiveTemplate t : this.templates) {
            clone.templates.add(t.copy());
        }
        return clone;
    }

    public LiveTemplate findTemplate(String abbreviation) {
        if (abbreviation == null) return null;
        for (LiveTemplate t : templates) {
            if (abbreviation.equals(t.getAbbreviation())) {
                return t;
            }
        }
        return null;
    }

    public boolean allTemplatesEnabled() {
        if (templates.isEmpty()) return enabled;
        for (LiveTemplate t : templates) {
            if (!t.isEnabled()) return false;
        }
        return true;
    }

    public boolean anyTemplateEnabled() {
        for (LiveTemplate t : templates) {
            if (t.isEnabled()) return true;
        }
        return false;
    }

    public void setAllTemplatesEnabled(boolean value) {
        this.enabled = value;
        for (LiveTemplate t : templates) {
            t.setEnabled(value);
        }
    }

    public boolean isEquivalentTo(LiveTemplateGroup other) {
        if (other == null) return false;
        if (!Objects.equals(this.name, other.name)) return false;
        if (this.enabled != other.enabled) return false;
        if (this.templates.size() != other.templates.size()) return false;
        for (int i = 0; i < this.templates.size(); i++) {
            if (!this.templates.get(i).isEquivalentTo(other.templates.get(i))) {
                return false;
            }
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
    }

    public List<LiveTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<LiveTemplate> templates) {
        this.templates = templates != null ? new ArrayList<>(templates) : new ArrayList<>();
    }
}
