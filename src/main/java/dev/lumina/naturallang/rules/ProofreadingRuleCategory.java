package dev.lumina.naturallang.rules;

/**
 * Categories for Proofreading, Grammar and Style rules matching reference IDE design.
 */
public enum ProofreadingRuleCategory {
    GENERAL("General"),
    PUNCTUATION("Punctuation"),
    TYPOGRAPHY("Typography"),
    READABILITY("Readability"),
    FORMALITY("Formality"),
    INCLUSIVITY("Inclusivity");

    private final String displayName;

    ProofreadingRuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
