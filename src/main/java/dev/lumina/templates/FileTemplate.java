package dev.lumina.templates;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single file or code template in Lumina, matching IntelliJ IDEA's FileTemplate.
 */
public class FileTemplate {

    private String id;
    private String name;
    private String extension;
    private String fileNameTemplate;
    private FileTemplateCategory category;
    private String text;
    private String defaultText;
    private boolean builtin;
    private boolean reformatCode = false;
    private boolean liveTemplatesEnabled = false;
    private String iconKey;
    private String description;
    private String group = "";
    private final Map<String, String> variables = new LinkedHashMap<>();

    public FileTemplate() {
        this.id = UUID.randomUUID().toString();
        this.category = FileTemplateCategory.FILES;
        this.text = "";
        this.defaultText = "";
        this.builtin = false;
    }

    public FileTemplate(String name, String extension, FileTemplateCategory category,
                        String defaultText, String description, String iconKey) {
        this(name, extension, "", category, defaultText, description, iconKey, "", true);
    }

    public FileTemplate(String name, String extension, String fileNameTemplate, FileTemplateCategory category,
                        String defaultText, String description, String iconKey, boolean builtin) {
        this(name, extension, fileNameTemplate, category, defaultText, description, iconKey, "", builtin);
    }

    public FileTemplate(String name, String extension, String fileNameTemplate, FileTemplateCategory category,
                        String defaultText, String description, String iconKey, String group, boolean builtin) {
        this.id = category.name().toLowerCase() + "_" + name.replaceAll("[^a-zA-Z0-9_.-]", "_");
        this.name = name;
        this.extension = extension == null ? "" : extension;
        this.fileNameTemplate = fileNameTemplate == null ? "" : fileNameTemplate;
        this.category = category;
        this.defaultText = defaultText == null ? "" : defaultText;
        this.text = this.defaultText;
        this.description = description == null ? "" : description;
        this.iconKey = iconKey == null ? "generic" : iconKey;
        this.group = group == null ? "" : group;
        this.builtin = builtin;
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

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getFileNameTemplate() {
        return fileNameTemplate;
    }

    public void setFileNameTemplate(String fileNameTemplate) {
        this.fileNameTemplate = fileNameTemplate;
    }

    public FileTemplateCategory getCategory() {
        return category;
    }

    public void setCategory(FileTemplateCategory category) {
        this.category = category;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    public String getDefaultText() {
        return defaultText;
    }

    public void setDefaultText(String defaultText) {
        this.defaultText = defaultText == null ? "" : defaultText;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
    }

    public boolean isReformatCode() {
        return reformatCode;
    }

    public void setReformatCode(boolean reformatCode) {
        this.reformatCode = reformatCode;
    }

    public boolean isLiveTemplatesEnabled() {
        return liveTemplatesEnabled;
    }

    public void setLiveTemplatesEnabled(boolean liveTemplatesEnabled) {
        this.liveTemplatesEnabled = liveTemplatesEnabled;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public void setVariables(Map<String, String> vars) {
        this.variables.clear();
        if (vars != null) {
            this.variables.putAll(vars);
        }
    }

    public void addVariable(String varName, String varDesc) {
        this.variables.put(varName, varDesc);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group == null ? "" : group;
    }

    /**
     * Checks if this template has been modified by the user compared to its original default.
     */
    public boolean isModified() {
        if (!builtin) {
            return true;
        }
        return !Objects.equals(text, defaultText);
    }

    /**
     * Reverts this template back to its factory default content.
     */
    public void revertToDefault() {
        if (builtin) {
            this.text = this.defaultText;
            this.reformatCode = false;
            this.liveTemplatesEnabled = false;
        }
    }

    /**
     * Creates a duplicate copy of this template for user cloning.
     */
    public FileTemplate copy(String newName) {
        FileTemplate copy = new FileTemplate();
        copy.setId(UUID.randomUUID().toString());
        copy.setName(newName);
        copy.setExtension(this.extension);
        copy.setFileNameTemplate(this.fileNameTemplate);
        copy.setCategory(this.category);
        copy.setText(this.text);
        copy.setDefaultText(this.text);
        copy.setBuiltin(false);
        copy.setReformatCode(this.reformatCode);
        copy.setLiveTemplatesEnabled(this.liveTemplatesEnabled);
        copy.setIconKey(this.iconKey);
        copy.setDescription(this.description);
        copy.setGroup(this.group);
        copy.setVariables(this.variables);
        return copy;
    }

    @Override
    public String toString() {
        return name;
    }
}
