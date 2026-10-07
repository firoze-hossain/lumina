package dev.lumina.naturallang.rules;

import java.util.List;

/**
 * Service Provider Interface (SPI) for contributing proofreading, grammar, and style rules
 * to Lumina dynamically without hardcoding them into UI components.
 */
public interface ProofreadingRuleProvider {

    /**
     * Unique name identifying the provider.
     */
    String getProviderName();

    /**
     * Rules contributed by this provider.
     */
    List<ProofreadingRule> getRules();
}
