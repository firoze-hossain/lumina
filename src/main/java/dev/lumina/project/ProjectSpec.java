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
        String springDependencies, // comma-separated, Spring Boot only
        String springBootVersion,  // blank = let start.spring.io pick the default
        String archetypeCatalog,
        String archetypeId,
        String archetypeVersion,
        String projectVersion,
        String additionalProperties,
        String rustToolchainPath,
        String rustTemplate,
        String rustEnvironment,
        String javafxDependencies  // comma-separated, JavaFX only
) {
    public ProjectSpec(
            Generator generator, String name, Path location, boolean initGit,
            BuildSystem buildSystem, Language language, Packaging packaging,
            ConfigFormat configFormat, String group, String artifact,
            String packageName, String javaVersion, String springDependencies,
            String springBootVersion, String archetypeCatalog, String archetypeId,
            String archetypeVersion, String projectVersion, String additionalProperties,
            String rustToolchainPath, String rustTemplate, String rustEnvironment
    ) {
        this(generator, name, location, initGit, buildSystem, language, packaging,
                configFormat, group, artifact, packageName, javaVersion,
                springDependencies, springBootVersion, archetypeCatalog, archetypeId,
                archetypeVersion, projectVersion, additionalProperties,
                rustToolchainPath, rustTemplate, rustEnvironment, "");
    }

    public enum Generator {
        JAVA, KOTLIN, GROOVY, EMPTY_PROJECT, SPRING_BOOT, MAVEN_ARCHETYPE,
        JAVAFX, QUARKUS, MICRONAUT, JAKARTA_EE, KTOR, HTML, REACT, EXPRESS,
        ANGULAR_CLI, VUE, VITE, NUXT, RUST
    }

    public enum BuildSystem { MAVEN, GRADLE }

    public enum Language { JAVA, KOTLIN, GROOVY }

    public enum Packaging { JAR, WAR }

    public enum ConfigFormat { PROPERTIES, YAML }

    public Path projectDir() {
        return location.resolve(generator == Generator.MAVEN_ARCHETYPE ? artifact : name);
    }
}
