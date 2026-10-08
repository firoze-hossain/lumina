package dev.lumina.textmate;

import java.util.List;

/**
 * SPI interface for dynamically contributing TextMate bundles without hardcoded coupling.
 */
public interface TextMateBundleProvider {

    String getProviderName();

    List<TextMateBundle> getBundles();
}
