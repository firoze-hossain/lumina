package dev.lumina.naturallang.rules;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Descriptor model for a dynamic proofreading rule.
 * Contains rule metadata, category, explanations, examples, configurable options,
 * and cloud connectivity requirements.
 */
public class ProofreadingRule {

    private final String id;
    private final ProofreadingRuleCategory category;
    private final String title;
    private final String description;
    private final String learnMoreUrl;
    private final List<RuleExample> examples;
    private final boolean defaultEnabled;
    private final RuleOption option;
    private final boolean requiresCloud;
    private final String expandedDetails;

    public ProofreadingRule(String id,
                            ProofreadingRuleCategory category,
                            String title,
                            String description,
                            String learnMoreUrl,
                            List<RuleExample> examples,
                            boolean defaultEnabled,
                            RuleOption option,
                            boolean requiresCloud,
                            String expandedDetails) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.description = description != null ? description : "";
        this.learnMoreUrl = learnMoreUrl;
        this.examples = examples != null ? Collections.unmodifiableList(examples) : List.of();
        this.defaultEnabled = defaultEnabled;
        this.option = option;
        this.requiresCloud = requiresCloud;
        this.expandedDetails = expandedDetails;
    }

    public String getId() {
        return id;
    }

    public ProofreadingRuleCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLearnMoreUrl() {
        return learnMoreUrl;
    }

    public List<RuleExample> getExamples() {
        return examples;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public RuleOption getOption() {
        return option;
    }

    public boolean hasOption() {
        return option != null;
    }

    public boolean isRequiresCloud() {
        return requiresCloud;
    }

    public String getExpandedDetails() {
        return expandedDetails;
    }

    public boolean hasExpandedDetails() {
        return expandedDetails != null && !expandedDetails.trim().isEmpty();
    }

    public boolean hasExamples() {
        return !examples.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProofreadingRule that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return category + " > " + title;
    }
}
