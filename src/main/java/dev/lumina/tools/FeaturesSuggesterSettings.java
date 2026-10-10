package dev.lumina.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing Tools > Features Suggester settings in Lumina IDE.
 */
public class FeaturesSuggesterSettings implements Cloneable {

    private boolean showSuggestions = true;
    private Map<String, Boolean> suggestedActions = new LinkedHashMap<>();

    public FeaturesSuggesterSettings() {
        initDefaults();
    }

    private void initDefaults() {
        suggestedActions.clear();
        suggestedActions.put("Comment with line comments", false);
        suggestedActions.put("Introduce variables", false);
        suggestedActions.put("Paste from history", false);
        suggestedActions.put("Quick Evaluate", true);
        suggestedActions.put("Surround with", false);
        suggestedActions.put("Unwrap", false);
        suggestedActions.put("File structure", false);
        suggestedActions.put("Show the completion popup", false);
        suggestedActions.put("Choose a lookup item and replace", false);
        suggestedActions.put("Run to cursor", false);
        suggestedActions.put("Edit a breakpoint", false);
        suggestedActions.put("Mute breakpoints", false);
    }

    public boolean isShowSuggestions() {
        return showSuggestions;
    }

    public void setShowSuggestions(boolean showSuggestions) {
        this.showSuggestions = showSuggestions;
    }

    public Map<String, Boolean> getSuggestedActions() {
        return suggestedActions;
    }

    public void setSuggestedActions(Map<String, Boolean> suggestedActions) {
        this.suggestedActions = suggestedActions != null ? new LinkedHashMap<>(suggestedActions) : new LinkedHashMap<>();
    }

    @Override
    public FeaturesSuggesterSettings clone() {
        try {
            FeaturesSuggesterSettings copy = (FeaturesSuggesterSettings) super.clone();
            copy.suggestedActions = new LinkedHashMap<>(this.suggestedActions);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeaturesSuggesterSettings that = (FeaturesSuggesterSettings) o;
        return showSuggestions == that.showSuggestions &&
                Objects.equals(suggestedActions, that.suggestedActions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showSuggestions, suggestedActions);
    }
}
