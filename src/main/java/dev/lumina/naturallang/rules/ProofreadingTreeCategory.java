package dev.lumina.naturallang.rules;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a linguistic category node within the "Other rules" tree
 * containing child proofreading rules.
 */
public class ProofreadingTreeCategory {

    private final String name;
    private final List<ProofreadingTreeRule> rules;

    public ProofreadingTreeCategory(String name, List<ProofreadingTreeRule> rules) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.rules = rules != null ? Collections.unmodifiableList(rules) : List.of();
    }

    public String getName() {
        return name;
    }

    public List<ProofreadingTreeRule> getRules() {
        return rules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProofreadingTreeCategory that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
