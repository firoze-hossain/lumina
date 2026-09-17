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
        String javafxDependencies,  // comma-separated, JavaFX only
        String quarkusServerUrl,
        String quarkusStream,
        String quarkusExtensions,    // comma-separated, Quarkus only
        String quarkusBuildTool,     // MAVEN, GRADLE, GRADLE_KOTLIN_DSL
        boolean addSampleCode,
        String jakartaVersion,       // Jakarta EE version, e.g. "Jakarta EE 11"
        String jakartaTemplate,      // "REST service", "Web application", "Library"
        String jakartaDependencies,  // comma-separated IDs
        String jakartaAppServer,     // e.g. "<No application server>"
        String micronautServerUrl,
        String micronautVersion,
        String micronautTestFramework,
        String micronautAppType,
        String micronautFeatures,
        String micronautBuild,
        String ktorServerUrl,
        String ktorEngine,
        boolean ktorAddSampleCode,
        String ktorBuildSystem,
        String ktorVersion,
        String ktorConfigIn,
        String ktorPlugins,
        String htmlProjectType,
        String htmlVersion
) {
    public ProjectSpec(
            Generator generator, String name, Path location, boolean initGit,
            BuildSystem buildSystem, Language language, Packaging packaging,
            ConfigFormat configFormat, String group, String artifact,
            String packageName, String javaVersion, String springDependencies,
            String springBootVersion, String archetypeCatalog, String archetypeId,
            String archetypeVersion, String projectVersion, String additionalProperties,
            String rustToolchainPath, String rustTemplate, String rustEnvironment,
            String javafxDependencies, String quarkusServerUrl, String quarkusStream,
            String quarkusExtensions, String quarkusBuildTool, boolean addSampleCode,
            String jakartaVersion, String jakartaTemplate, String jakartaDependencies,
            String jakartaAppServer, String micronautServerUrl, String micronautVersion,
            String micronautTestFramework, String micronautAppType, String micronautFeatures,
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins
    ) {
        this(generator, name, location, initGit, buildSystem, language, packaging,
                configFormat, group, artifact, packageName, javaVersion,
                springDependencies, springBootVersion, archetypeCatalog, archetypeId,
                archetypeVersion, projectVersion, additionalProperties,
                rustToolchainPath, rustTemplate, rustEnvironment, javafxDependencies,
                quarkusServerUrl, quarkusStream, quarkusExtensions, quarkusBuildTool,
                addSampleCode, jakartaVersion, jakartaTemplate, jakartaDependencies,
                jakartaAppServer, micronautServerUrl, micronautVersion,
                micronautTestFramework, micronautAppType, micronautFeatures,
                micronautBuild, ktorServerUrl, ktorEngine, ktorAddSampleCode,
                ktorBuildSystem, ktorVersion, ktorConfigIn, ktorPlugins,
                "HTML5 Boilerplate", "v9.0.1");
    }

    public ProjectSpec(
            Generator generator, String name, Path location, boolean initGit,
            BuildSystem buildSystem, Language language, Packaging packaging,
            ConfigFormat configFormat, String group, String artifact,
            String packageName, String javaVersion, String springDependencies,
            String springBootVersion, String archetypeCatalog, String archetypeId,
            String archetypeVersion, String projectVersion, String additionalProperties,
            String rustToolchainPath, String rustTemplate, String rustEnvironment,
            String javafxDependencies, String quarkusServerUrl, String quarkusStream,
            String quarkusExtensions, String quarkusBuildTool, boolean addSampleCode
    ) {
        this(generator, name, location, initGit, buildSystem, language, packaging,
                configFormat, group, artifact, packageName, javaVersion,
                springDependencies, springBootVersion, archetypeCatalog, archetypeId,
                archetypeVersion, projectVersion, additionalProperties,
                rustToolchainPath, rustTemplate, rustEnvironment, javafxDependencies,
                quarkusServerUrl, quarkusStream, quarkusExtensions, quarkusBuildTool,
                addSampleCode, JakartaMetadata.EE_11, JakartaMetadata.TEMPLATE_REST, "", "<No application server>",
                MicronautMetadata.DEFAULT_SERVER_URL, MicronautMetadata.DEFAULT_VERSION, "JUNIT", "default", "", "gradle",
                KtorMetadata.DEFAULT_SERVER_URL, "Netty", true, "Gradle", "3.5.2", "YAML File", "");
    }

    public ProjectSpec(
            Generator generator, String name, Path location, boolean initGit,
            BuildSystem buildSystem, Language language, Packaging packaging,
            ConfigFormat configFormat, String group, String artifact,
            String packageName, String javaVersion, String springDependencies,
            String springBootVersion, String archetypeCatalog, String archetypeId,
            String archetypeVersion, String projectVersion, String additionalProperties,
            String rustToolchainPath, String rustTemplate, String rustEnvironment,
            String javafxDependencies, String quarkusServerUrl, String quarkusStream,
            String quarkusExtensions, String quarkusBuildTool, boolean addSampleCode,
            String jakartaVersion, String jakartaTemplate, String jakartaDependencies,
            String jakartaAppServer
    ) {
        this(generator, name, location, initGit, buildSystem, language, packaging,
                configFormat, group, artifact, packageName, javaVersion,
                springDependencies, springBootVersion, archetypeCatalog, archetypeId,
                archetypeVersion, projectVersion, additionalProperties,
                rustToolchainPath, rustTemplate, rustEnvironment, javafxDependencies,
                quarkusServerUrl, quarkusStream, quarkusExtensions, quarkusBuildTool,
                addSampleCode, jakartaVersion, jakartaTemplate, jakartaDependencies,
                jakartaAppServer,
                MicronautMetadata.DEFAULT_SERVER_URL, MicronautMetadata.DEFAULT_VERSION,
                "JUNIT", "default", "", "gradle",
                KtorMetadata.DEFAULT_SERVER_URL, "Netty", true, "Gradle", "3.5.2", "YAML File", "");
    }

    public ProjectSpec(
            Generator generator, String name, Path location, boolean initGit,
            BuildSystem buildSystem, Language language, Packaging packaging,
            ConfigFormat configFormat, String group, String artifact,
            String packageName, String javaVersion, String springDependencies,
            String springBootVersion, String archetypeCatalog, String archetypeId,
            String archetypeVersion, String projectVersion, String additionalProperties,
            String rustToolchainPath, String rustTemplate, String rustEnvironment,
            String javafxDependencies, String quarkusServerUrl, String quarkusStream,
            String quarkusExtensions, String quarkusBuildTool, boolean addSampleCode,
            String jakartaVersion, String jakartaTemplate, String jakartaDependencies,
            String jakartaAppServer, String micronautServerUrl, String micronautVersion,
            String micronautTestFramework, String micronautAppType, String micronautFeatures,
            String micronautBuild
    ) {
        this(generator, name, location, initGit, buildSystem, language, packaging,
                configFormat, group, artifact, packageName, javaVersion,
                springDependencies, springBootVersion, archetypeCatalog, archetypeId,
                archetypeVersion, projectVersion, additionalProperties,
                rustToolchainPath, rustTemplate, rustEnvironment, javafxDependencies,
                quarkusServerUrl, quarkusStream, quarkusExtensions, quarkusBuildTool,
                addSampleCode, jakartaVersion, jakartaTemplate, jakartaDependencies,
                jakartaAppServer,
                micronautServerUrl, micronautVersion, micronautTestFramework,
                micronautAppType, micronautFeatures, micronautBuild,
                KtorMetadata.DEFAULT_SERVER_URL, "Netty", true, "Gradle", "3.5.2", "YAML File", "");
    }

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
                rustToolchainPath, rustTemplate, rustEnvironment, "",
                QuarkusMetadata.DEFAULT_SERVER_URL, "", "", "MAVEN", true);
    }

    public ProjectSpec(
            Generator generator, String name, Path location, boolean initGit,
            BuildSystem buildSystem, Language language, Packaging packaging,
            ConfigFormat configFormat, String group, String artifact,
            String packageName, String javaVersion, String springDependencies,
            String springBootVersion, String archetypeCatalog, String archetypeId,
            String archetypeVersion, String projectVersion, String additionalProperties,
            String rustToolchainPath, String rustTemplate, String rustEnvironment,
            String javafxDependencies
    ) {
        this(generator, name, location, initGit, buildSystem, language, packaging,
                configFormat, group, artifact, packageName, javaVersion,
                springDependencies, springBootVersion, archetypeCatalog, archetypeId,
                archetypeVersion, projectVersion, additionalProperties,
                rustToolchainPath, rustTemplate, rustEnvironment, javafxDependencies,
                QuarkusMetadata.DEFAULT_SERVER_URL, "", "", "MAVEN", true);
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
