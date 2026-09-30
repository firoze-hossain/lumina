package dev.lumina.inspections;

import java.util.List;

/**
 * Extension point interface for code inspections.
 * Plugins and language extensions implement this interface to dynamically register
 * their inspection tools without hardcoding them into the core UI.
 */
public interface InspectionProvider {

    /**
     * Returns the list of inspection tools provided by this extension.
     */
    List<InspectionTool> getInspections();
}
