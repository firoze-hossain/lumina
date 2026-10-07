package dev.lumina.injections;

import java.util.List;

/**
 * Extension point interface for language injections.
 * Plugins and language extensions implement this interface to dynamically register
 * their injection configurations without hardcoding them into the core UI.
 */
public interface LanguageInjectionProvider {

    /**
     * Provider identification name (e.g. "Database Tools", "Spring Framework", "Hibernate").
     */
    String getProviderName();

    /**
     * Returns the list of language injections contributed by this extension.
     */
    List<LanguageInjection> getInjections();
}
