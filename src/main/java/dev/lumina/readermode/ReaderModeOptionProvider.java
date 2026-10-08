package dev.lumina.readermode;

import java.util.List;

/**
 * Extension point SPI for dynamically contributing Reader Mode options.
 */
public interface ReaderModeOptionProvider {

    String getProviderName();

    List<ReaderModeOption> getOptions();
}
