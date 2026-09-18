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
        String htmlVersion,
        String reactProjectType,
        String reactNodeInterpreter,
        String reactCliVersion,
        boolean reactTypeScript,
        String expressNodeInterpreter,
        String expressCliVersion,
        String expressViewEngine,
        String expressStylesheetEngine,
        GradleDsl gradleDsl,
        String gradleDistribution,
        String gradleVersion,
        String gradleHome,
        boolean generateMultiModule,
        String groovyVersion,
        String sbtVersion,
        String scalaVersion,
        boolean scalaDownloadSbtSources,
        boolean scalaDownloadScalaSources,
        boolean scalaOptionalBraces,
        String scalaPackagePrefix,
        String scalaModuleName,
        PythonInterpreterType pythonInterpreterType,
        String pythonPath,
        String pythonVersion,
        String uvPath,
        String condaPath,
        boolean customEnvGenerateNew,
        String customEnvType,
        String customEnvLocation,
        boolean customEnvInheritGlobal,
        boolean customEnvMakeAvailable,
        boolean phpAddComposerJson,
        String rubyInterpreterPath,
        boolean rubyAddSampleCode,
        String goRoot,
        boolean goVendoring,
        String goEnvironment,
        String angularNodeInterpreter,
        String angularCliVersion,
        String angularAdditionalParameters,
        boolean angularStandalone,
        boolean angularDefaults,
        String playVersion,
        String gemTestingFramework,
        boolean gemCodeOfConduct,
        boolean gemMitLicense,
        boolean gemBinaryExecutable,
        boolean gemCExtension,
        String railsVersion,
        String railsType,
        String railsDatabase,
        boolean railsJsFrameworkEnabled,
        String railsJsFramework,
        String railsExtraOptions,
        String appEngineGoRoot,
        boolean appEngineIndexEntireGopath,
        boolean appEnginePythonSupport,
        String appEnginePythonSdk,
        boolean appEngineSqlSupport,
        String appEngineSqlDialect,
        String appEngineModuleName,
        String appEngineContentRoot,
        String appEngineModuleFileLocation,
        String appEngineProjectFormat,
        String vueNodeInterpreter,
        String vueCliPackage,
        boolean vueDefaultSetup,
        String viteNodeInterpreter,
        String viteCliPackage,
        String viteTemplate,
        boolean viteTypeScript,
        String symfonyProjectType,
        String symfonyVersion
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
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson,
            String rubyInterpreterPath, boolean rubyAddSampleCode,
            String goRoot, boolean goVendoring, String goEnvironment,
            String angularNodeInterpreter, String angularCliVersion,
            String angularAdditionalParameters, boolean angularStandalone,
            boolean angularDefaults, String playVersion,
            String gemTestingFramework, boolean gemCodeOfConduct, boolean gemMitLicense,
            boolean gemBinaryExecutable, boolean gemCExtension,
            String railsVersion, String railsType, String railsDatabase,
            boolean railsJsFrameworkEnabled, String railsJsFramework,
            String railsExtraOptions,
            String appEngineGoRoot,
            boolean appEngineIndexEntireGopath,
            boolean appEnginePythonSupport,
            String appEnginePythonSdk,
            boolean appEngineSqlSupport,
            String appEngineSqlDialect,
            String appEngineModuleName,
            String appEngineContentRoot,
            String appEngineModuleFileLocation,
            String appEngineProjectFormat,
            String vueNodeInterpreter,
            String vueCliPackage,
            boolean vueDefaultSetup
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                rubyInterpreterPath, rubyAddSampleCode, goRoot, goVendoring, goEnvironment,
                angularNodeInterpreter, angularCliVersion, angularAdditionalParameters,
                angularStandalone, angularDefaults, playVersion,
                gemTestingFramework, gemCodeOfConduct, gemMitLicense,
                gemBinaryExecutable, gemCExtension,
                railsVersion, railsType, railsDatabase, railsJsFrameworkEnabled,
                railsJsFramework, railsExtraOptions,
                appEngineGoRoot, appEngineIndexEntireGopath, appEnginePythonSupport,
                appEnginePythonSdk, appEngineSqlSupport, appEngineSqlDialect,
                appEngineModuleName, appEngineContentRoot, appEngineModuleFileLocation,
                appEngineProjectFormat,
                vueNodeInterpreter, vueCliPackage, vueDefaultSetup,
                "", ViteMetadata.RUNNER_CREATE_VITE, ViteMetadata.DEFAULT_TEMPLATE, false,
                SymfonyMetadata.TYPE_WEB, SymfonyMetadata.DEFAULT_VERSION);
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson,
            String rubyInterpreterPath, boolean rubyAddSampleCode,
            String goRoot, boolean goVendoring, String goEnvironment,
            String angularNodeInterpreter, String angularCliVersion,
            String angularAdditionalParameters, boolean angularStandalone,
            boolean angularDefaults, String playVersion,
            String gemTestingFramework, boolean gemCodeOfConduct, boolean gemMitLicense,
            boolean gemBinaryExecutable, boolean gemCExtension,
            String railsVersion, String railsType, String railsDatabase,
            boolean railsJsFrameworkEnabled, String railsJsFramework,
            String railsExtraOptions,
            String appEngineGoRoot,
            boolean appEngineIndexEntireGopath,
            boolean appEnginePythonSupport,
            String appEnginePythonSdk,
            boolean appEngineSqlSupport,
            String appEngineSqlDialect,
            String appEngineModuleName,
            String appEngineContentRoot,
            String appEngineModuleFileLocation,
            String appEngineProjectFormat,
            String vueNodeInterpreter,
            String vueCliPackage,
            boolean vueDefaultSetup,
            String viteNodeInterpreter,
            String viteCliPackage,
            String viteTemplate,
            boolean viteTypeScript
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                rubyInterpreterPath, rubyAddSampleCode, goRoot, goVendoring, goEnvironment,
                angularNodeInterpreter, angularCliVersion, angularAdditionalParameters,
                angularStandalone, angularDefaults, playVersion,
                gemTestingFramework, gemCodeOfConduct, gemMitLicense,
                gemBinaryExecutable, gemCExtension,
                railsVersion, railsType, railsDatabase, railsJsFrameworkEnabled,
                railsJsFramework, railsExtraOptions,
                appEngineGoRoot, appEngineIndexEntireGopath, appEnginePythonSupport,
                appEnginePythonSdk, appEngineSqlSupport, appEngineSqlDialect,
                appEngineModuleName, appEngineContentRoot, appEngineModuleFileLocation,
                appEngineProjectFormat,
                vueNodeInterpreter, vueCliPackage, vueDefaultSetup,
                viteNodeInterpreter, viteCliPackage, viteTemplate, viteTypeScript,
                SymfonyMetadata.TYPE_WEB, SymfonyMetadata.DEFAULT_VERSION);
    }

    public static ProjectSpec forVite(
            String name,
            Path location,
            String nodeInterpreter,
            String cliPackage,
            String template,
            boolean typeScript
    ) {
        return new ProjectSpec(
                Generator.VITE,
                name,
                location,
                false,
                BuildSystem.INTELLIJ,
                Language.JAVA,
                Packaging.JAR,
                ConfigFormat.PROPERTIES,
                "com.example",
                name,
                "com.example." + name,
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "", "", "", "", "", false,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "", "", "",
                "", "", "", false, "", "", "", "",
                GradleDsl.GROOVY, "Wrapper", "9.2.0", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, false, false,
                "", name, PythonInterpreterType.PROJECT_VENV,
                "", "", "", "", false, "Virtualenv", "", false,
                false, false, "", false, "", false, "", "", "",
                "", false, false, "", "", false, false, false,
                false, "", "", "", false, "", "", "", false,
                false, "", false, "", name,
                location.resolve(name).toString(),
                location.resolve(name).toString(),
                "Directory (.idea)",
                "", "", false,
                nodeInterpreter,
                cliPackage,
                template,
                typeScript,
                SymfonyMetadata.TYPE_WEB,
                SymfonyMetadata.DEFAULT_VERSION
        );
    }

    public static ProjectSpec forSymfony(
            String name,
            Path location,
            boolean initGit,
            String projectType,
            String version
    ) {
        return new ProjectSpec(
                Generator.SYMFONY,
                name,
                location,
                initGit,
                BuildSystem.INTELLIJ,
                Language.PHP,
                Packaging.JAR,
                ConfigFormat.PROPERTIES,
                "com.example",
                name,
                "com.example." + name,
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "", "", "", "", "", false,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "", "", "",
                "", "", "", false, "", "", "", "",
                GradleDsl.GROOVY, "Wrapper", "9.2.0", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, false, false,
                "", name, PythonInterpreterType.PROJECT_VENV,
                "", "", "", "", false, "Virtualenv", "", false,
                false, false, "", false, "", false, "", "", "",
                "", false, false, "", "", false, false, false,
                false, "", "", "", false, "", "", "", false,
                false, "", false, "", name,
                location.resolve(name).toString(),
                location.resolve(name).toString(),
                "Directory (.idea)",
                "", "", false,
                "", ViteMetadata.RUNNER_CREATE_VITE, ViteMetadata.DEFAULT_TEMPLATE, false,
                projectType,
                version
        );
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson,
            String rubyInterpreterPath, boolean rubyAddSampleCode,
            String goRoot, boolean goVendoring, String goEnvironment,
            String angularNodeInterpreter, String angularCliVersion,
            String angularAdditionalParameters, boolean angularStandalone,
            boolean angularDefaults, String playVersion,
            String gemTestingFramework, boolean gemCodeOfConduct, boolean gemMitLicense,
            boolean gemBinaryExecutable, boolean gemCExtension
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                rubyInterpreterPath, rubyAddSampleCode, goRoot, goVendoring, goEnvironment,
                angularNodeInterpreter, angularCliVersion, angularAdditionalParameters,
                angularStandalone, angularDefaults, playVersion,
                gemTestingFramework, gemCodeOfConduct, gemMitLicense,
                gemBinaryExecutable, gemCExtension,
                "", RailsMetadata.TYPE_FULL, "SQLite3", true, "Importmap", "",
                "", false, false, "", false, "", "", "", AppEngineMetadata.DEFAULT_PROJECT_FORMAT);
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson,
            String rubyInterpreterPath, boolean rubyAddSampleCode,
            String goRoot, boolean goVendoring, String goEnvironment,
            String angularNodeInterpreter, String angularCliVersion,
            String angularAdditionalParameters, boolean angularStandalone,
            boolean angularDefaults, String playVersion,
            String gemTestingFramework, boolean gemCodeOfConduct, boolean gemMitLicense,
            boolean gemBinaryExecutable, boolean gemCExtension,
            String railsVersion, String railsType, String railsDatabase,
            boolean railsJsFrameworkEnabled, String railsJsFramework,
            String railsExtraOptions
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                rubyInterpreterPath, rubyAddSampleCode, goRoot, goVendoring, goEnvironment,
                angularNodeInterpreter, angularCliVersion, angularAdditionalParameters,
                angularStandalone, angularDefaults, playVersion,
                gemTestingFramework, gemCodeOfConduct, gemMitLicense,
                gemBinaryExecutable, gemCExtension,
                railsVersion, railsType, railsDatabase, railsJsFrameworkEnabled,
                railsJsFramework, railsExtraOptions,
                "", false, false, "", false, AppEngineMetadata.DEFAULT_SQL_DIALECT, "", "", "", AppEngineMetadata.DEFAULT_PROJECT_FORMAT,
                "", VueMetadata.RUNNER_CREATE_VUE, true);
    }

    public ProjectSpec(
            Generator generator, String name, Path location, boolean initGit,
            BuildSystem buildSystem, Language language, Packaging packaging,
            ConfigFormat configFormat, String group, String artifact,
            String packageName, String javaVersion, String springDependencies,
            String springBootVersion, String archetypeCatalog, String archetypeId,
            String archetypeVersion, String projectVersion, String additionalProperties,
            String rustToolchainPath, String rustTemplate, String rustEnvironment,
            String javafxDependencies, String quarkusServerUrl, String snapQuarkusStream,
            String quarkusExtensions, String quarkusBuildTool, boolean addSampleCode,
            String jakartaVersion, String jakartaTemplate, String jakartaDependencies,
            String jakartaAppServer, String micronautServerUrl, String micronautVersion,
            String micronautTestFramework, String micronautAppType, String micronautFeatures,
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson,
            String rubyInterpreterPath, boolean rubyAddSampleCode,
            String goRoot, boolean goVendoring, String goEnvironment,
            String angularNodeInterpreter, String angularCliVersion,
            String angularAdditionalParameters, boolean angularStandalone,
            boolean angularDefaults, String playVersion,
            String gemTestingFramework, boolean gemCodeOfConduct, boolean gemMitLicense,
            boolean gemBinaryExecutable, boolean gemCExtension,
            String railsVersion, String railsType, String railsDatabase,
            boolean railsJsFrameworkEnabled, String railsJsFramework,
            String railsExtraOptions,
            String appEngineGoRoot,
            boolean appEngineIndexEntireGopath,
            boolean appEnginePythonSupport,
            String appEnginePythonSdk,
            boolean appEngineSqlSupport,
            String appEngineModuleName,
            String appEngineContentRoot,
            String appEngineModuleFileLocation,
            String appEngineProjectFormat
    ) {
        this(generator, name, location, initGit, buildSystem, language, packaging,
                configFormat, group, artifact, packageName, javaVersion,
                springDependencies, springBootVersion, archetypeCatalog, archetypeId,
                archetypeVersion, projectVersion, additionalProperties,
                rustToolchainPath, rustTemplate, rustEnvironment, javafxDependencies,
                quarkusServerUrl, snapQuarkusStream, quarkusExtensions, quarkusBuildTool,
                addSampleCode, jakartaVersion, jakartaTemplate, jakartaDependencies,
                jakartaAppServer, micronautServerUrl, micronautVersion,
                micronautTestFramework, micronautAppType, micronautFeatures,
                micronautBuild, ktorServerUrl, ktorEngine, ktorAddSampleCode,
                ktorBuildSystem, ktorVersion, ktorConfigIn, ktorPlugins,
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                rubyInterpreterPath, rubyAddSampleCode, goRoot, goVendoring, goEnvironment,
                angularNodeInterpreter, angularCliVersion, angularAdditionalParameters,
                angularStandalone, angularDefaults, playVersion,
                gemTestingFramework, gemCodeOfConduct, gemMitLicense,
                gemBinaryExecutable, gemCExtension,
                railsVersion, railsType, railsDatabase, railsJsFrameworkEnabled,
                railsJsFramework, railsExtraOptions,
                appEngineGoRoot, appEngineIndexEntireGopath, appEnginePythonSupport,
                appEnginePythonSdk, appEngineSqlSupport, AppEngineMetadata.DEFAULT_SQL_DIALECT,
                appEngineModuleName, appEngineContentRoot, appEngineModuleFileLocation,
                appEngineProjectFormat,
                "", VueMetadata.RUNNER_CREATE_VUE, true);
    }

    public ProjectSpec(
            Generator generator, String name, Path location, boolean initGit,
            BuildSystem buildSystem, Language language, Packaging packaging,
            ConfigFormat configFormat, String group, String artifact,
            String packageName, String javaVersion, String springDependencies,
            String springBootVersion, String archetypeCatalog, String archetypeId,
            String archetypeVersion, String projectVersion, String additionalProperties,
            String rustToolchainPath, String rustTemplate, String rustEnvironment,
            String javafxDependencies, String quarkusServerUrl, String snapQuarkusStream,
            String quarkusExtensions, String quarkusBuildTool, boolean addSampleCode,
            String jakartaVersion, String jakartaTemplate, String jakartaDependencies,
            String jakartaAppServer, String micronautServerUrl, String micronautVersion,
            String micronautTestFramework, String micronautAppType, String micronautFeatures,
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson,
            String rubyInterpreterPath, boolean rubyAddSampleCode,
            String goRoot, boolean goVendoring, String goEnvironment,
            String angularNodeInterpreter, String angularCliVersion,
            String angularAdditionalParameters, boolean angularStandalone,
            boolean angularDefaults, String playVersion,
            String gemTestingFramework, boolean gemCodeOfConduct, boolean gemMitLicense,
            boolean gemBinaryExecutable, boolean gemCExtension,
            String railsVersion, String railsType, String railsDatabase,
            boolean railsJsFrameworkEnabled, String railsJsFramework,
            String railsExtraOptions,
            String appEngineGoRoot,
            boolean appEngineIndexEntireGopath,
            boolean appEnginePythonSupport,
            String appEnginePythonSdk,
            boolean appEngineSqlSupport,
            String appEngineSqlDialect,
            String appEngineModuleName,
            String appEngineContentRoot,
            String appEngineModuleFileLocation,
            String appEngineProjectFormat
    ) {
        this(generator, name, location, initGit, buildSystem, language, packaging,
                configFormat, group, artifact, packageName, javaVersion,
                springDependencies, springBootVersion, archetypeCatalog, archetypeId,
                archetypeVersion, projectVersion, additionalProperties,
                rustToolchainPath, rustTemplate, rustEnvironment, javafxDependencies,
                quarkusServerUrl, snapQuarkusStream, quarkusExtensions, quarkusBuildTool,
                addSampleCode, jakartaVersion, jakartaTemplate, jakartaDependencies,
                jakartaAppServer, micronautServerUrl, micronautVersion,
                micronautTestFramework, micronautAppType, micronautFeatures,
                micronautBuild, ktorServerUrl, ktorEngine, ktorAddSampleCode,
                ktorBuildSystem, ktorVersion, ktorConfigIn, ktorPlugins,
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                rubyInterpreterPath, rubyAddSampleCode, goRoot, goVendoring, goEnvironment,
                angularNodeInterpreter, angularCliVersion, angularAdditionalParameters,
                angularStandalone, angularDefaults, playVersion,
                gemTestingFramework, gemCodeOfConduct, gemMitLicense,
                gemBinaryExecutable, gemCExtension,
                railsVersion, railsType, railsDatabase, railsJsFrameworkEnabled,
                railsJsFramework, railsExtraOptions,
                appEngineGoRoot, appEngineIndexEntireGopath, appEnginePythonSupport,
                appEnginePythonSdk, appEngineSqlSupport, appEngineSqlDialect,
                appEngineModuleName, appEngineContentRoot, appEngineModuleFileLocation,
                appEngineProjectFormat,
                "", VueMetadata.RUNNER_CREATE_VUE, true);
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson,
            String rubyInterpreterPath, boolean rubyAddSampleCode,
            String goRoot, boolean goVendoring, String goEnvironment,
            String angularNodeInterpreter, String angularCliVersion,
            String angularAdditionalParameters, boolean angularStandalone,
            boolean angularDefaults, String playVersion
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                rubyInterpreterPath, rubyAddSampleCode, goRoot, goVendoring, goEnvironment,
                angularNodeInterpreter, angularCliVersion, angularAdditionalParameters,
                angularStandalone, angularDefaults, playVersion,
                "minitest", true, true, false, false);
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson,
            String rubyInterpreterPath, boolean rubyAddSampleCode,
            String goRoot, boolean goVendoring, String goEnvironment,
            String angularNodeInterpreter, String angularCliVersion,
            String angularAdditionalParameters, boolean angularStandalone,
            boolean angularDefaults
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                rubyInterpreterPath, rubyAddSampleCode, goRoot, goVendoring, goEnvironment,
                angularNodeInterpreter, angularCliVersion, angularAdditionalParameters,
                angularStandalone, angularDefaults, "");
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson,
            String rubyInterpreterPath, boolean rubyAddSampleCode,
            String goRoot, boolean goVendoring, String goEnvironment
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                rubyInterpreterPath, rubyAddSampleCode, goRoot, goVendoring, goEnvironment,
                "", "", "", true, true, "");
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson,
            String rubyInterpreterPath, boolean rubyAddSampleCode
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                rubyInterpreterPath, rubyAddSampleCode, "", true, "");
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable, boolean phpAddComposerJson
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, phpAddComposerJson,
                "", false);
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath,
            String condaPath, boolean customEnvGenerateNew, String customEnvType,
            String customEnvLocation, boolean customEnvInheritGlobal,
            boolean customEnvMakeAvailable
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                condaPath, customEnvGenerateNew, customEnvType, customEnvLocation,
                customEnvInheritGlobal, customEnvMakeAvailable, false);
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName,
            PythonInterpreterType pythonInterpreterType,
            String pythonPath, String pythonVersion, String uvPath
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, pythonInterpreterType, pythonPath, pythonVersion, uvPath,
                "", true, "Virtualenv", "", false, false);
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion,
            String sbtVersion, String scalaVersion, boolean scalaDownloadSbtSources,
            boolean scalaDownloadScalaSources, boolean scalaOptionalBraces,
            String scalaPackagePrefix, String scalaModuleName
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion, sbtVersion, scalaVersion, scalaDownloadSbtSources,
                scalaDownloadScalaSources, scalaOptionalBraces, scalaPackagePrefix,
                scalaModuleName, PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false);
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule, String groovyVersion
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule,
                groovyVersion != null && !groovyVersion.isBlank() ? groovyVersion : "5.1.1",
                "2.0.9", "3.9.0", false, true, false, "", "");
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome, boolean generateMultiModule
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, generateMultiModule, "5.1.1");
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine,
            GradleDsl gradleDsl, String gradleDistribution, String gradleVersion,
            String gradleHome
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion,
                expressViewEngine, expressStylesheetEngine,
                gradleDsl, gradleDistribution, gradleVersion, gradleHome, false, "5.1.1");
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript,
            String expressNodeInterpreter, String expressCliVersion,
            String expressViewEngine, String expressStylesheetEngine
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                expressNodeInterpreter, expressCliVersion, expressViewEngine,
                expressStylesheetEngine,
                GradleDsl.GROOVY, "Wrapper", "9.2.0", "", false);
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion, String reactProjectType, String reactNodeInterpreter,
            String reactCliVersion, boolean reactTypeScript
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
                htmlProjectType, htmlVersion, reactProjectType, reactNodeInterpreter,
                reactCliVersion, reactTypeScript,
                reactNodeInterpreter, "4.16.1", "Pug (Jade)", "Plain CSS");
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
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false);
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
            String micronautBuild, String ktorServerUrl, String ktorEngine,
            boolean ktorAddSampleCode, String ktorBuildSystem, String ktorVersion,
            String ktorConfigIn, String ktorPlugins, String htmlProjectType,
            String htmlVersion
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
                htmlProjectType, htmlVersion, "React", "", "5.1.0", false);
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
        JAVA, KOTLIN, GROOVY, SCALA, PYTHON, PHP, RUBY, EMPTY_PROJECT, SPRING_BOOT, MAVEN_ARCHETYPE,
        JAVAFX, QUARKUS, MICRONAUT, JAKARTA_EE, KTOR, PLAY, HTML, REACT, EXPRESS,
        ANGULAR_CLI, GEM, RUBY_ON_RAILS, APP_ENGINE, VUE, VITE, SYMFONY, NUXT, RUST, GO
    }

    public enum BuildSystem { INTELLIJ, MAVEN, GRADLE, SBT, SCALA_CLI }

    public enum GradleDsl { KOTLIN, GROOVY }

    public enum Language { JAVA, KOTLIN, GROOVY, SCALA, PYTHON, PHP, RUBY, RUST, GO }

    public enum PythonInterpreterType { PROJECT_VENV, UV, BASE_CONDA, CUSTOM_ENVIRONMENT }

    public enum Packaging { JAR, WAR }

    public enum ConfigFormat { PROPERTIES, YAML }

    public GradleDsl safeGradleDsl() {
        return gradleDsl != null ? gradleDsl : GradleDsl.KOTLIN;
    }

    public String safeGradleVersion() {
        return gradleVersion != null && !gradleVersion.isBlank() ? gradleVersion.trim() : "9.2.0";
    }

    public String safeGradleDistribution() {
        return gradleDistribution != null && !gradleDistribution.isBlank() ? gradleDistribution.trim() : "Wrapper";
    }

    public String safeGradleLocation() {
        return safeGradleHome();
    }

    public String safeGradleHome() {
        return gradleHome != null ? gradleHome.trim() : "";
    }

    public boolean safeGenerateMultiModule() {
        return generateMultiModule;
    }

    public String safeGroovyVersion() {
        if (groovyVersion == null || groovyVersion.isBlank() || GroovyMetadata.SPECIFY_HOME_OPTION.equals(groovyVersion.trim())) {
            return "5.1.1";
        }
        return groovyVersion.trim();
    }

    public String safeSbtVersion() {
        return sbtVersion != null && !sbtVersion.isBlank() ? sbtVersion.trim() : "2.0.9";
    }

    public String safeScalaVersion() {
        return scalaVersion != null && !scalaVersion.isBlank() ? scalaVersion.trim() : "3.9.0";
    }

    public boolean safeScalaDownloadSbtSources() {
        return scalaDownloadSbtSources;
    }

    public boolean safeScalaDownloadScalaSources() {
        return scalaDownloadScalaSources;
    }

    public boolean safeScalaOptionalBraces() {
        return scalaOptionalBraces;
    }

    public String safeScalaPackagePrefix() {
        return scalaPackagePrefix != null ? scalaPackagePrefix.trim() : "";
    }

    public String safeScalaModuleName() {
        return scalaModuleName != null && !scalaModuleName.isBlank() ? scalaModuleName.trim() : (name != null ? name.trim() : "untitled1");
    }

    public PythonInterpreterType safePythonInterpreterType() {
        return pythonInterpreterType != null ? pythonInterpreterType : PythonInterpreterType.PROJECT_VENV;
    }

    public String safePythonPath() {
        return pythonPath != null && !pythonPath.isBlank() ? pythonPath.trim() : "/usr/bin/python3";
    }

    public String safePythonVersion() {
        return pythonVersion != null && !pythonVersion.isBlank() ? pythonVersion.trim() : "3.12";
    }

    public String safeUvPath() {
        return uvPath != null ? uvPath.trim() : "";
    }

    public String safeCondaPath() {
        return condaPath != null ? condaPath.trim() : "";
    }

    public boolean isCustomEnvGenerateNew() {
        return customEnvGenerateNew;
    }

    public String safeCustomEnvType() {
        return customEnvType != null && !customEnvType.isBlank() ? customEnvType.trim() : "Virtualenv";
    }

    public String safeCustomEnvLocation() {
        return customEnvLocation != null ? customEnvLocation.trim() : "";
    }

    public boolean isCustomEnvInheritGlobal() {
        return customEnvInheritGlobal;
    }

    public boolean isCustomEnvMakeAvailable() {
        return customEnvMakeAvailable;
    }

    public boolean safePhpAddComposerJson() {
        return phpAddComposerJson;
    }

    public String safeRubyInterpreterPath() {
        return rubyInterpreterPath != null ? rubyInterpreterPath.trim() : "";
    }

    public boolean safeRubyAddSampleCode() {
        return rubyAddSampleCode;
    }

    public String safePlayVersion() {
        return playVersion != null && !playVersion.isBlank() ? playVersion.trim() : PlayMetadata.DEFAULT_PLAY_VERSION;
    }

    public String safeGemTestingFramework() {
        return gemTestingFramework != null && !gemTestingFramework.isBlank() ? gemTestingFramework.trim().toLowerCase() : "minitest";
    }

    public boolean safeGemCodeOfConduct() {
        return gemCodeOfConduct;
    }

    public boolean safeGemMitLicense() {
        return gemMitLicense;
    }

    public boolean safeGemBinaryExecutable() {
        return gemBinaryExecutable;
    }

    public boolean safeGemCExtension() {
        return gemCExtension;
    }

    public String safeRailsVersion() {
        return railsVersion != null ? railsVersion.trim() : "";
    }

    public String safeRailsType() {
        return railsType != null && !railsType.isBlank() ? railsType.trim() : RailsMetadata.TYPE_FULL;
    }

    public String safeRailsDatabase() {
        return railsDatabase != null && !railsDatabase.isBlank() ? railsDatabase.trim() : "SQLite3";
    }

    public boolean safeRailsJsFrameworkEnabled() {
        return railsJsFrameworkEnabled;
    }

    public String safeRailsJsFramework() {
        return railsJsFramework != null && !railsJsFramework.isBlank() ? railsJsFramework.trim() : "Importmap";
    }

    public String safeRailsExtraOptions() {
        return railsExtraOptions != null ? railsExtraOptions.trim() : "";
    }

    public String safeAppEngineGoRoot() {
        return appEngineGoRoot != null && !appEngineGoRoot.isBlank() ? appEngineGoRoot.trim() : (goRoot != null ? goRoot.trim() : "");
    }

    public boolean safeAppEngineIndexEntireGopath() {
        return appEngineIndexEntireGopath;
    }

    public boolean safeAppEnginePythonSupport() {
        return appEnginePythonSupport;
    }

    public String safeAppEnginePythonSdk() {
        return appEnginePythonSdk != null ? appEnginePythonSdk.trim() : "";
    }

    public boolean safeAppEngineSqlSupport() {
        return appEngineSqlSupport;
    }

    public String safeAppEngineSqlDialect() {
        return appEngineSqlDialect != null && !appEngineSqlDialect.isBlank() ? appEngineSqlDialect.trim() : AppEngineMetadata.DEFAULT_SQL_DIALECT;
    }

    public String safeAppEngineModuleName() {
        return appEngineModuleName != null && !appEngineModuleName.isBlank() ? appEngineModuleName.trim() : name();
    }

    public String safeAppEngineContentRoot() {
        return appEngineContentRoot != null && !appEngineContentRoot.isBlank() ? appEngineContentRoot.trim() : projectDir().toString();
    }

    public String safeAppEngineModuleFileLocation() {
        return appEngineModuleFileLocation != null && !appEngineModuleFileLocation.isBlank() ? appEngineModuleFileLocation.trim() : projectDir().toString();
    }

    public String safeAppEngineProjectFormat() {
        return appEngineProjectFormat != null && !appEngineProjectFormat.isBlank() ? appEngineProjectFormat.trim() : AppEngineMetadata.DEFAULT_PROJECT_FORMAT;
    }

    public String safeVueNodeInterpreter() {
        return vueNodeInterpreter != null ? vueNodeInterpreter.trim() : "";
    }

    public String safeVueCliPackage() {
        return vueCliPackage != null && !vueCliPackage.isBlank() ? vueCliPackage.trim() : VueMetadata.RUNNER_CREATE_VUE;
    }

    public boolean safeVueDefaultSetup() {
        return vueDefaultSetup;
    }

    public String safeViteNodeInterpreter() {
        return viteNodeInterpreter != null ? viteNodeInterpreter.trim() : "";
    }

    public String safeViteCliPackage() {
        return viteCliPackage != null && !viteCliPackage.isBlank() ? viteCliPackage.trim() : ViteMetadata.RUNNER_CREATE_VITE;
    }

    public String safeViteTemplate() {
        return viteTemplate != null && !viteTemplate.isBlank() ? viteTemplate.trim() : ViteMetadata.DEFAULT_TEMPLATE;
    }

    public boolean safeViteTypeScript() {
        return viteTypeScript;
    }

    public String safeSymfonyProjectType() {
        return symfonyProjectType != null && !symfonyProjectType.isBlank() ? symfonyProjectType.trim() : SymfonyMetadata.TYPE_WEB;
    }

    public String safeSymfonyVersion() {
        return symfonyVersion != null && !symfonyVersion.isBlank() ? symfonyVersion.trim() : SymfonyMetadata.DEFAULT_VERSION;
    }

    public Path projectDir() {
        return location.resolve(generator == Generator.MAVEN_ARCHETYPE ? artifact : name);
    }
}

