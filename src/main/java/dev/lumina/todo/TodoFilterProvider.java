package dev.lumina.todo;

import java.util.List;

/**
 * SPI interface for dynamically contributing TODO filters in Lumina IDE.
 */
public interface TodoFilterProvider {

    String getProviderName();

    List<TodoFilter> getFilters();
}
