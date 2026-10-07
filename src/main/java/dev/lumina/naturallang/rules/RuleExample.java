package dev.lumina.naturallang.rules;

/**
 * Encapsulates an example demonstrating a proofreading rule with before and corrected states.
 */
public record RuleExample(String example, String corrected) {
    public boolean hasCorrection() {
        return corrected != null && !corrected.trim().isEmpty();
    }
}
