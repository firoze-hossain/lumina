package dev.lumina.readermode;

import java.util.List;

/**
 * SPI interface for dynamically discovering code style schemes available for Reader Mode formatting.
 */
public interface ReaderModeSchemeProvider {

    String getActiveSchemeName();

    List<String> getAvailableSchemes();
}
