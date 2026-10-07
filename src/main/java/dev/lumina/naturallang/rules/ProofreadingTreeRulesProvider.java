package dev.lumina.naturallang.rules;

import java.util.List;

/**
 * Service Provider Interface (SPI) for contributing categorized linguistic rules
 * to the "Other rules" tree dynamically.
 */
public interface ProofreadingTreeRulesProvider {

    /**
     * Unique identifier for the provider.
     */
    String getProviderName();

    /**
     * Rule categories contributed by this provider.
     */
    List<ProofreadingTreeCategory> getTreeCategories();
}
