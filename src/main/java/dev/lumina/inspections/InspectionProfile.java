package dev.lumina.inspections;

import java.util.*;

/**
 * Inspection configuration profile. Manages enabled states, severities, scopes,
 * and highlighting for all registered inspection tools.
 */
public class InspectionProfile {

    public enum TriState {
        CHECKED,
        UNCHECKED,
        INDETERMINATE
    }

    public static class ToolState {
        private boolean enabled;
        private HighlightSeverity severity;
        private String scope;
        private String highlighting;

        public ToolState(boolean enabled, HighlightSeverity severity, String scope, String highlighting) {
            this.enabled = enabled;
            this.severity = severity != null ? severity : HighlightSeverity.WARNING;
            this.scope = scope != null ? scope : "In All Scopes";
            this.highlighting = highlighting != null ? highlighting : this.severity.getDisplayName();
        }

        public ToolState copy() {
            return new ToolState(enabled, severity, scope, highlighting);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public HighlightSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(HighlightSeverity severity) {
            this.severity = severity != null ? severity : HighlightSeverity.WARNING;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope != null ? scope : "In All Scopes";
        }

        public String getHighlighting() {
            return highlighting;
        }

        public void setHighlighting(String highlighting) {
            this.highlighting = highlighting;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ToolState toolState)) return false;
            return enabled == toolState.enabled &&
                    severity == toolState.severity &&
                    Objects.equals(scope, toolState.scope) &&
                    Objects.equals(highlighting, toolState.highlighting);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, severity, scope, highlighting);
        }
    }

    private String name;
    private boolean projectLevel;
    private boolean disableNewInspections;
    private final Map<String, ToolState> toolStates = new LinkedHashMap<>();

    public InspectionProfile(String name, boolean projectLevel) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.projectLevel = projectLevel;
        this.disableNewInspections = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public boolean isProjectLevel() {
        return projectLevel;
    }

    public void setProjectLevel(boolean projectLevel) {
        this.projectLevel = projectLevel;
    }

    public boolean isDisableNewInspections() {
        return disableNewInspections;
    }

    public void setDisableNewInspections(boolean disableNewInspections) {
        this.disableNewInspections = disableNewInspections;
    }

    public synchronized ToolState getOrCreateState(InspectionTool tool) {
        return toolStates.computeIfAbsent(tool.getId(), id -> new ToolState(
                tool.isDefaultEnabled(),
                tool.getDefaultSeverity(),
                tool.getDefaultScope(),
                tool.getDefaultHighlighting()
        ));
    }

    public synchronized ToolState getState(String toolId) {
        return toolStates.get(toolId);
    }

    public synchronized void putState(String toolId, ToolState state) {
        toolStates.put(toolId, state.copy());
    }

    public synchronized boolean isEnabled(InspectionTool tool) {
        ToolState state = toolStates.get(tool.getId());
        return state != null ? state.isEnabled() : tool.isDefaultEnabled();
    }

    public synchronized void setEnabled(String toolId, boolean enabled) {
        ToolState state = toolStates.get(toolId);
        if (state != null) {
            state.setEnabled(enabled);
        } else {
            InspectionTool tool = InspectionRegistry.getInstance().getTool(toolId);
            if (tool != null) {
                ToolState newState = new ToolState(
                        enabled,
                        tool.getDefaultSeverity(),
                        tool.getDefaultScope(),
                        tool.getDefaultHighlighting()
                );
                toolStates.put(toolId, newState);
            }
        }
    }

    public synchronized HighlightSeverity getSeverity(InspectionTool tool) {
        ToolState state = toolStates.get(tool.getId());
        return state != null ? state.getSeverity() : tool.getDefaultSeverity();
    }

    public synchronized void setSeverity(String toolId, HighlightSeverity severity) {
        ToolState state = toolStates.get(toolId);
        if (state != null) {
            state.setSeverity(severity);
        } else {
            InspectionTool tool = InspectionRegistry.getInstance().getTool(toolId);
            if (tool != null) {
                ToolState newState = new ToolState(
                        tool.isDefaultEnabled(),
                        severity,
                        tool.getDefaultScope(),
                        severity != null ? severity.getDisplayName() : tool.getDefaultHighlighting()
                );
                toolStates.put(toolId, newState);
            }
        }
    }

    public synchronized String getScope(InspectionTool tool) {
        ToolState state = toolStates.get(tool.getId());
        return state != null ? state.getScope() : tool.getDefaultScope();
    }

    public synchronized void setScope(String toolId, String scope) {
        ToolState state = toolStates.get(toolId);
        if (state != null) {
            state.setScope(scope);
        } else {
            InspectionTool tool = InspectionRegistry.getInstance().getTool(toolId);
            if (tool != null) {
                ToolState newState = new ToolState(
                        tool.isDefaultEnabled(),
                        tool.getDefaultSeverity(),
                        scope,
                        tool.getDefaultHighlighting()
                );
                toolStates.put(toolId, newState);
            }
        }
    }

    public synchronized String getHighlighting(InspectionTool tool) {
        ToolState state = toolStates.get(tool.getId());
        return state != null ? state.getHighlighting() : tool.getDefaultHighlighting();
    }

    public synchronized void setHighlighting(String toolId, String highlighting) {
        ToolState state = toolStates.get(toolId);
        if (state != null) {
            state.setHighlighting(highlighting);
        } else {
            InspectionTool tool = InspectionRegistry.getInstance().getTool(toolId);
            if (tool != null) {
                ToolState newState = new ToolState(
                        tool.isDefaultEnabled(),
                        tool.getDefaultSeverity(),
                        tool.getDefaultScope(),
                        highlighting
                );
                toolStates.put(toolId, newState);
            }
        }
    }

    /**
     * Calculates the tri-state (CHECKED, UNCHECKED, INDETERMINATE) for a category.
     */
    public synchronized TriState getCategoryState(String category, InspectionRegistry registry) {
        List<InspectionTool> tools = registry.getToolsForCategory(category);
        if (tools.isEmpty()) {
            return TriState.UNCHECKED;
        }

        int enabledCount = 0;
        for (InspectionTool tool : tools) {
            if (isEnabled(tool)) {
                enabledCount++;
            }
        }

        if (enabledCount == 0) {
            return TriState.UNCHECKED;
        } else if (enabledCount == tools.size()) {
            return TriState.CHECKED;
        } else {
            return TriState.INDETERMINATE;
        }
    }

    /**
     * Toggles all tools in a category to enabled or disabled.
     */
    public synchronized void setCategoryEnabled(String category, boolean enabled, InspectionRegistry registry) {
        List<InspectionTool> tools = registry.getToolsForCategory(category);
        for (InspectionTool tool : tools) {
            setEnabled(tool.getId(), enabled);
        }
    }

    /**
     * Bulk updates severity for multiple tools.
     */
    public synchronized void setToolsSeverity(Collection<String> toolIds, HighlightSeverity severity) {
        if (toolIds == null || severity == null) return;
        for (String id : toolIds) {
            setSeverity(id, severity);
        }
    }

    /**
     * Bulk updates scope for multiple tools.
     */
    public synchronized void setToolsScope(Collection<String> toolIds, String scope) {
        if (toolIds == null || scope == null) return;
        for (String id : toolIds) {
            setScope(id, scope);
        }
    }

    /**
     * Bulk updates editor highlighting for multiple tools.
     */
    public synchronized void setToolsHighlighting(Collection<String> toolIds, String highlighting) {
        if (toolIds == null || highlighting == null) return;
        for (String id : toolIds) {
            setHighlighting(id, highlighting);
        }
    }

    /**
     * Resets all tools to their default settings.
     */
    public synchronized void resetToDefaults(InspectionRegistry registry) {
        toolStates.clear();
        for (InspectionTool tool : registry.getAllTools()) {
            toolStates.put(tool.getId(), new ToolState(
                    tool.isDefaultEnabled(),
                    tool.getDefaultSeverity(),
                    tool.getDefaultScope(),
                    tool.getDefaultHighlighting()
            ));
        }
        disableNewInspections = false;
    }

    /**
     * Checks if this profile has any modifications compared to default inspection settings.
     */
    public synchronized boolean isModified(InspectionRegistry registry) {
        if (disableNewInspections) return true;
        for (InspectionTool tool : registry.getAllTools()) {
            ToolState state = toolStates.get(tool.getId());
            if (state != null) {
                if (state.isEnabled() != tool.isDefaultEnabled()) return true;
                if (state.getSeverity() != tool.getDefaultSeverity()) return true;
                if (!Objects.equals(state.getScope(), tool.getDefaultScope())) return true;
                if (!Objects.equals(state.getHighlighting(), tool.getDefaultHighlighting())) return true;
            }
        }
        return false;
    }

    /**
     * Deep clones this profile with a new name and scope.
     */
    public synchronized InspectionProfile cloneProfile(String newName, boolean newProjectLevel) {
        InspectionProfile copy = new InspectionProfile(newName, newProjectLevel);
        copy.setDisableNewInspections(this.disableNewInspections);
        for (Map.Entry<String, ToolState> entry : this.toolStates.entrySet()) {
            copy.putState(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    /**
     * Copies all state from another profile.
     */
    public synchronized void copyFrom(InspectionProfile other) {
        this.name = other.name;
        this.projectLevel = other.projectLevel;
        this.disableNewInspections = other.disableNewInspections;
        this.toolStates.clear();
        for (Map.Entry<String, ToolState> entry : other.toolStates.entrySet()) {
            this.toolStates.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public synchronized Map<String, ToolState> getToolStates() {
        return Collections.unmodifiableMap(toolStates);
    }
}
