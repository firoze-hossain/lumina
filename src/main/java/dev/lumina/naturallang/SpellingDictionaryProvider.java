package dev.lumina.naturallang;

import java.util.List;

/**
 * SPI interface for contributing spelling dictionaries dynamically without hardcoded registration.
 */
public interface SpellingDictionaryProvider {

    String getProviderName();

    List<String> getDictionaries();
}
