package dev.lumina.todo;

import java.util.List;

/**
 * SPI interface for dynamically contributing TODO patterns in Lumina IDE.
 */
public interface TodoPatternProvider {

    String getProviderName();

    List<TodoPattern> getPatterns();
}
