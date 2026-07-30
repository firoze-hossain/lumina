package dev.lumina.project;

import java.nio.file.Path;

/** Everything the New Project dialog collects, handed to the generator. */
public record ProjectSpec(
        Generator generator,
        String name,
        Path location,          // parent folder; project goes in location/name
        boolean initGit,
        BuildSystem buildSystem,
        Language language,
        Packaging packaging,
        ConfigFormat configFormat,
        String group,
        String artifact,
        String packageName,
        String javaVersion,
        String springDependencies  // comma-separated, Spring Boot only
) {
    public enum Generator { JAVA, SPRING_BOOT }

    public enum BuildSystem { MAVEN, GRADLE }

    public enum Language { JAVA, KOTLIN, GROOVY }

    public enum Packaging { JAR, WAR }

    public enum ConfigFormat { PROPERTIES, YAML }

    public Path projectDir() {
        return location.resolve(name);
    }
}
