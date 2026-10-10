package dev.lumina.advanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Descriptor and container for an extensible, dynamically configurable advanced setting.
 */
public class AdvancedSettingItem {
    private final String id;
    private final String group;
    private final String title;
    private final String description;
    private final AdvancedSettingType type;
    private final Object defaultValue;
    private Object currentValue;
    private final List<String> options;
    private final String trailingUnit;
    private final boolean requiresRestart;
    private final String linkText;
    private final String linkUrl;

    public AdvancedSettingItem(String id, String group, String title, String description,
                               AdvancedSettingType type, Object defaultValue, Object currentValue,
                               List<String> options, String trailingUnit, boolean requiresRestart,
                               String linkText, String linkUrl) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.group = Objects.requireNonNull(group, "group must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = description;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.defaultValue = defaultValue;
        this.currentValue = currentValue != null ? currentValue : defaultValue;
        this.options = options != null ? Collections.unmodifiableList(new ArrayList<>(options)) : Collections.emptyList();
        this.trailingUnit = trailingUnit;
        this.requiresRestart = requiresRestart;
        this.linkText = linkText;
        this.linkUrl = linkUrl;
    }

    public static Builder builder(String id, String group, String title) {
        return new Builder(id, group, title);
    }

    public String getId() {
        return id;
    }

    public String getGroup() {
        return group;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public AdvancedSettingType getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Object getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Object currentValue) {
        this.currentValue = currentValue;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getTrailingUnit() {
        return trailingUnit;
    }

    public boolean isRequiresRestart() {
        return requiresRestart;
    }

    public String getLinkText() {
        return linkText;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public boolean isModifiedFromDefault() {
        return !Objects.equals(this.currentValue, this.defaultValue);
    }

    public boolean asBoolean() {
        if (currentValue instanceof Boolean) {
            return (Boolean) currentValue;
        }
        return Boolean.parseBoolean(String.valueOf(currentValue));
    }

    public int asInt() {
        if (currentValue instanceof Number) {
            return ((Number) currentValue).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(currentValue));
        } catch (NumberFormatException e) {
            if (defaultValue instanceof Number) {
                return ((Number) defaultValue).intValue();
            }
            return 0;
        }
    }

    public String asString() {
        return currentValue != null ? String.valueOf(currentValue) : "";
    }

    public AdvancedSettingItem copy() {
        return new AdvancedSettingItem(
                id, group, title, description, type,
                defaultValue, currentValue, options, trailingUnit,
                requiresRestart, linkText, linkUrl
        );
    }

    public static class Builder {
        private final String id;
        private final String group;
        private final String title;
        private String description;
        private AdvancedSettingType type = AdvancedSettingType.BOOLEAN;
        private Object defaultValue = Boolean.FALSE;
        private Object currentValue;
        private List<String> options = new ArrayList<>();
        private String trailingUnit;
        private boolean requiresRestart;
        private String linkText;
        private String linkUrl;

        public Builder(String id, String group, String title) {
            this.id = id;
            this.group = group;
            this.title = title;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(AdvancedSettingType type) {
            this.type = type;
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder currentValue(Object currentValue) {
            this.currentValue = currentValue;
            return this;
        }

        public Builder options(List<String> options) {
            this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
            return this;
        }

        public Builder trailingUnit(String trailingUnit) {
            this.trailingUnit = trailingUnit;
            return this;
        }

        public Builder requiresRestart(boolean requiresRestart) {
            this.requiresRestart = requiresRestart;
            return this;
        }

        public Builder link(String linkText, String linkUrl) {
            this.linkText = linkText;
            this.linkUrl = linkUrl;
            return this;
        }

        public AdvancedSettingItem build() {
            return new AdvancedSettingItem(
                    id, group, title, description, type,
                    defaultValue, currentValue != null ? currentValue : defaultValue,
                    options, trailingUnit, requiresRestart, linkText, linkUrl
            );
        }
    }
}
