package dev.lumina.naturallang;

import java.util.List;

/**
 * Service Provider Interface (SPI) for contributing Natural Languages to Lumina.
 * Allows language packs and linguistic plugins to register natural languages dynamically
 * without modifying UI components.
 */
public interface NaturalLanguageProvider {

    /**
     * Unique provider identifier.
     */
    String getProviderName();

    /**
     * List of natural languages contributed by this provider.
     */
    List<NaturalLanguage> getLanguages();
}
