package dev.lumina.inspections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an individual code inspection tool.
 */
public class InspectionTool {

    private final String id;
    private final String displayName;
    private final String groupPath;
    private final String description;
    private final HighlightSeverity defaultSeverity;
    private final boolean defaultEnabled;
    private final String defaultScope;
    private final String defaultHighlighting;
    private final String language;
    private final List<String> tags;
    private final boolean batchModeOnly;
    private final boolean cleanupTool;

    public InspectionTool(
            String id,
            String displayName,
            String groupPath,
            String description,
            HighlightSeverity defaultSeverity,
            boolean defaultEnabled,
            String defaultScope,
            String defaultHighlighting,
            String language,
            List<String> tags,
            boolean batchModeOnly,
            boolean cleanupTool
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.displayName = displayName != null ? displayName : id;
        this.groupPath = groupPath != null ? groupPath : "General";
        this.description = description != null ? description : "";
        this.defaultSeverity = defaultSeverity != null ? defaultSeverity : HighlightSeverity.WARNING;
        this.defaultEnabled = defaultEnabled;
        this.defaultScope = defaultScope != null ? defaultScope : "In All Scopes";
        this.defaultHighlighting = defaultHighlighting != null ? defaultHighlighting : this.defaultSeverity.getDisplayName();
        this.language = language;
        this.tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
        this.batchModeOnly = batchModeOnly;
        this.cleanupTool = cleanupTool;
    }

    public InspectionTool(
            String id,
            String displayName,
            String groupPath,
            String description,
            HighlightSeverity defaultSeverity,
            boolean defaultEnabled,
            String defaultScope,
            String defaultHighlighting,
            String language,
            List<String> tags
    ) {
        this(id, displayName, groupPath, description, defaultSeverity, defaultEnabled, defaultScope, defaultHighlighting, language, tags, false, false);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGroupPath() {
        return groupPath;
    }

    public String getDescription() {
        return description;
    }

    public HighlightSeverity getDefaultSeverity() {
        return defaultSeverity;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public String getDefaultScope() {
        return defaultScope;
    }

    public String getDefaultHighlighting() {
        return defaultHighlighting;
    }

    public String getLanguage() {
        return language;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean isBatchModeOnly() {
        return batchModeOnly;
    }

    public boolean isCleanupTool() {
        return cleanupTool;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InspectionTool that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return displayName + " (" + id + ")";
    }

    public static class Builder {
        private final String id;
        private String displayName;
        private String groupPath = "General";
        private String description = "";
        private HighlightSeverity defaultSeverity = HighlightSeverity.WARNING;
        private boolean defaultEnabled = true;
        private String defaultScope = "In All Scopes";
        private String defaultHighlighting = "Warning";
        private String language;
        private final List<String> tags = new ArrayList<>();
        private boolean batchModeOnly = false;
        private boolean cleanupTool = false;

        public Builder(String id) {
            this.id = id;
            this.displayName = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder groupPath(String groupPath) {
            this.groupPath = groupPath;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultSeverity(HighlightSeverity defaultSeverity) {
            this.defaultSeverity = defaultSeverity;
            if (defaultSeverity != null) {
                this.defaultHighlighting = defaultSeverity.getDisplayName();
            }
            return this;
        }

        public Builder defaultEnabled(boolean defaultEnabled) {
            this.defaultEnabled = defaultEnabled;
            return this;
        }

        public Builder defaultScope(String defaultScope) {
            this.defaultScope = defaultScope;
            return this;
        }

        public Builder defaultHighlighting(String defaultHighlighting) {
            this.defaultHighlighting = defaultHighlighting;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder batchModeOnly(boolean batchModeOnly) {
            this.batchModeOnly = batchModeOnly;
            return this;
        }

        public Builder cleanupTool(boolean cleanupTool) {
            this.cleanupTool = cleanupTool;
            return this;
        }

        public Builder addTag(String tag) {
            if (tag != null && !tag.isBlank()) {
                this.tags.add(tag.trim());
            }
            return this;
        }

        public InspectionTool build() {
            return new InspectionTool(
                    id, displayName, groupPath, description,
                    defaultSeverity, defaultEnabled, defaultScope,
                    defaultHighlighting, language, tags,
                    batchModeOnly, cleanupTool
            );
        }
    }
}
