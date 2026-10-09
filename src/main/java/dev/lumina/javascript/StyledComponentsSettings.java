package dev.lumina.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Styled Components settings in Lumina IDE.
 */
public class StyledComponentsSettings {

    private List<String> additionalTagPrefixes = new ArrayList<>();

    public StyledComponentsSettings() {
    }

    public StyledComponentsSettings(List<String> additionalTagPrefixes) {
        this.additionalTagPrefixes = additionalTagPrefixes != null ? new ArrayList<>(additionalTagPrefixes) : new ArrayList<>();
    }

    public StyledComponentsSettings copy() {
        return new StyledComponentsSettings(new ArrayList<>(additionalTagPrefixes));
    }

    public List<String> getAdditionalTagPrefixes() {
        return additionalTagPrefixes;
    }

    public void setAdditionalTagPrefixes(List<String> additionalTagPrefixes) {
        this.additionalTagPrefixes = additionalTagPrefixes != null ? new ArrayList<>(additionalTagPrefixes) : new ArrayList<>();
    }

    public void addTagPrefix(String prefix) {
        if (prefix != null && !prefix.isBlank() && !additionalTagPrefixes.contains(prefix)) {
            additionalTagPrefixes.add(prefix);
        }
    }

    public void removeTagPrefix(String prefix) {
        additionalTagPrefixes.remove(prefix);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StyledComponentsSettings that)) return false;
        return Objects.equals(additionalTagPrefixes, that.additionalTagPrefixes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(additionalTagPrefixes);
    }
}
