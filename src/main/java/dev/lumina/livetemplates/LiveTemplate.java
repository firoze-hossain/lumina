package dev.lumina.livetemplates;

import java.util.*;

/**
 * Model representing an IntelliJ IDEA-style Live Template.
 */
public class LiveTemplate {

    private String id = UUID.randomUUID().toString();
    private String groupId = "";
    private String subgroup = null;
    private String abbreviation = "";
    private String description = "";
    private String templateText = "";
    private String expandWith = "Default (Tab)";
    private boolean reformat = true;
    private boolean shortenFQNames = true;
    private boolean useStaticImport = false;
    private boolean enabled = true;
    private boolean builtin = true;
    private Set<String> contexts = new LinkedHashSet<>();
    private List<LiveTemplateVariable> variables = new ArrayList<>();

    public LiveTemplate() {
    }

    public LiveTemplate(String abbreviation, String description, String templateText) {
        this.abbreviation = abbreviation != null ? abbreviation : "";
        this.description = description != null ? description : "";
        this.templateText = templateText != null ? templateText : "";
    }

    public LiveTemplate(String abbreviation, String description, String templateText, String subgroup) {
        this(abbreviation, description, templateText);
        this.subgroup = subgroup;
    }

    public LiveTemplate copy() {
        LiveTemplate clone = new LiveTemplate();
        clone.id = this.id;
        clone.groupId = this.groupId;
        clone.subgroup = this.subgroup;
        clone.abbreviation = this.abbreviation;
        clone.description = this.description;
        clone.templateText = this.templateText;
        clone.expandWith = this.expandWith;
        clone.reformat = this.reformat;
        clone.shortenFQNames = this.shortenFQNames;
        clone.useStaticImport = this.useStaticImport;
        clone.enabled = this.enabled;
        clone.builtin = this.builtin;
        clone.contexts = new LinkedHashSet<>(this.contexts);
        for (LiveTemplateVariable v : this.variables) {
            clone.variables.add(v.copy());
        }
        return clone;
    }

    public boolean isEquivalentTo(LiveTemplate other) {
        if (other == null) return false;
        return this.enabled == other.enabled &&
                this.reformat == other.reformat &&
                this.shortenFQNames == other.shortenFQNames &&
                this.useStaticImport == other.useStaticImport &&
                Objects.equals(this.abbreviation, other.abbreviation) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.templateText, other.templateText) &&
                Objects.equals(this.expandWith, other.expandWith) &&
                Objects.equals(this.subgroup, other.subgroup) &&
                Objects.equals(this.contexts, other.contexts) &&
                Objects.equals(this.variables, other.variables);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(String subgroup) {
        this.subgroup = subgroup;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation != null ? abbreviation : "";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public String getTemplateText() {
        return templateText;
    }

    public void setTemplateText(String templateText) {
        this.templateText = templateText != null ? templateText : "";
    }

    public String getExpandWith() {
        return expandWith;
    }

    public void setExpandWith(String expandWith) {
        this.expandWith = expandWith != null ? expandWith : "Default (Tab)";
    }

    public boolean isReformat() {
        return reformat;
    }

    public void setReformat(boolean reformat) {
        this.reformat = reformat;
    }

    public boolean isShortenFQNames() {
        return shortenFQNames;
    }

    public void setShortenFQNames(boolean shortenFQNames) {
        this.shortenFQNames = shortenFQNames;
    }

    public boolean isUseStaticImport() {
        return useStaticImport;
    }

    public void setUseStaticImport(boolean useStaticImport) {
        this.useStaticImport = useStaticImport;
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

    public Set<String> getContexts() {
        return contexts;
    }

    public void setContexts(Set<String> contexts) {
        this.contexts = contexts != null ? new LinkedHashSet<>(contexts) : new LinkedHashSet<>();
    }

    public List<LiveTemplateVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<LiveTemplateVariable> variables) {
        this.variables = variables != null ? new ArrayList<>(variables) : new ArrayList<>();
    }
}
