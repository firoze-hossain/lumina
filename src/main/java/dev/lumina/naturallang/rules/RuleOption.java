package dev.lumina.naturallang.rules;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configurable parameter/choice option associated with a proofreading rule
 * (e.g., ComboBox choices next to a rule title).
 */
public record RuleOption(String name, List<String> choices, String defaultChoice) {
    public RuleOption {
        Objects.requireNonNull(name, "name cannot be null");
        choices = choices != null ? Collections.unmodifiableList(choices) : List.of();
        if (defaultChoice == null && !choices.isEmpty()) {
            defaultChoice = choices.get(0);
        }
    }
}
