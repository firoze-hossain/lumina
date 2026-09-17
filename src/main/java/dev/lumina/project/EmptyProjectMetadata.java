package dev.lumina.project;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Metadata and helper utilities for generating Empty Projects matching IntelliJ IDEA.
 */
public final class EmptyProjectMetadata {

    private EmptyProjectMetadata() {}

    /**
     * Dynamically finds the next available unique name (e.g. untitled, untitled1, untitled2)
     * in the specified target directory.
     */
    public static String suggestUniqueProjectName(Path locationDir, String baseName) {
        if (baseName == null || baseName.isBlank()) {
            baseName = "untitled";
        }
        if (locationDir == null || !Files.exists(locationDir)) {
            return baseName;
        }
        if (!Files.exists(locationDir.resolve(baseName))) {
            return baseName;
        }
        int idx = 1;
        while (Files.exists(locationDir.resolve(baseName + idx))) {
            idx++;
        }
        return baseName + idx;
    }

    /**
     * Generates .idea/modules.xml for an empty project with free structure (no modules initially).
     */
    public static String generateIdeaModulesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectModuleManager">
                    <modules />
                  </component>
                </project>
                """;
    }

    /**
     * Generates .idea/misc.xml for an empty project.
     */
    public static String generateIdeaMiscXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectRootManager" version="2" />
                </project>
                """;
    }

    /**
     * Generates .idea/vcs.xml mapping the root directory to Git.
     */
    public static String generateIdeaVcsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="VcsDirectoryMappings">
                    <mapping directory="" vcs="Git" />
                  </component>
                </project>
                """;
    }

    /**
     * Generates .idea/.gitignore matching IntelliJ's standard.
     */
    public static String generateIdeaGitIgnore() {
        return """
                # Default ignored files
                /shelf/
                /workspace.xml
                """;
    }

    /**
     * Generates root .gitignore for an empty project.
     */
    public static String generateRootGitIgnore() {
        return """
                .idea/
                *.iml
                """;
    }
}
