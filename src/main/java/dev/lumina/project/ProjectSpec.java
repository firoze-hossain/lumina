package dev.lumina.project;

import java.nio.file.Path;

/** Everything the New Project dialog collects, handed to the generator. */
public record ProjectSpec(
        Generator generator,
        String name,
        Path location,          // parent folder; project goes in location/name
        boolean initGit,
        BuildSystem buildSystem,
        String group,
        String artifact,
        String packageName,
        String javaVersion,
        String springDependencies  // comma-separated, Spring Boot only
) {
    public enum Generator { JAVA, SPRING_BOOT }

    public enum BuildSystem { MAVEN, GRADLE }

    public Path projectDir() {
        return location.resolve(name);
    }
}
