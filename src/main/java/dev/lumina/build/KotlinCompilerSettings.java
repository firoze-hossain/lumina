package dev.lumina.build;

import java.util.Objects;

/**
 * Configuration model for Kotlin Compiler settings in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791449881924_e5b30675.png:
 *  - Report compiler warnings
 *  - Kotlin compiler version (Bundled / custom)
 *  - Language version
 *  - API version
 *  - Additional command line parameters
 *  - Keep compiler process alive between invocations
 *  - Kotlin to JVM:
 *    - Enable incremental compilation
 *    - Target JVM version
 *  - Kotlin to JavaScript:
 *    - Enable incremental compilation
 *    - Generate source maps
 *    - Source map prefix
 *    - Embed source code into source map
 *    - Copy library runtime files
 *    - Destination directory
 *    - Module kind
 *  - Kotlin Script (Beta):
 *    - Script definition template classes to load explicitly
 *    - Classpath required for loading script definition template classes
 */
public class KotlinCompilerSettings implements Cloneable {

    private boolean reportCompilerWarnings = true;
    private String kotlinCompilerVersion = "Bundled (2.1.21-release-317)";
    private String languageVersion = "2.1";
    private String apiVersion = "2.1";
    private String additionalCommandLineParameters = "";
    private boolean keepCompilerProcessAlive = true;

    // Kotlin to JVM
    private boolean jvmEnableIncrementalCompilation = true;
    private String targetJvmVersion = "1.8";

    // Kotlin to JavaScript
    private boolean jsEnableIncrementalCompilation = true;
    private boolean generateSourceMaps = false;
    private String sourceMapPrefix = "";
    private String embedSourceCodeIntoSourceMap = "When inlining a function from other module with embedded sources";
    private boolean copyLibraryRuntimeFiles = true;
    private String destinationDirectory = "lib";
    private String moduleKind = "Plain (put to global scope)";

    // Kotlin Script (Beta)
    private String scriptDefinitionTemplateClasses = "";
    private String scriptClasspath = "";

    public KotlinCompilerSettings() {
    }

    public KotlinCompilerSettings(KotlinCompilerSettings other) {
        if (other != null) {
            this.reportCompilerWarnings = other.reportCompilerWarnings;
            this.kotlinCompilerVersion = other.kotlinCompilerVersion;
            this.languageVersion = other.languageVersion;
            this.apiVersion = other.apiVersion;
            this.additionalCommandLineParameters = other.additionalCommandLineParameters;
            this.keepCompilerProcessAlive = other.keepCompilerProcessAlive;
            this.jvmEnableIncrementalCompilation = other.jvmEnableIncrementalCompilation;
            this.targetJvmVersion = other.targetJvmVersion;
            this.jsEnableIncrementalCompilation = other.jsEnableIncrementalCompilation;
            this.generateSourceMaps = other.generateSourceMaps;
            this.sourceMapPrefix = other.sourceMapPrefix;
            this.embedSourceCodeIntoSourceMap = other.embedSourceCodeIntoSourceMap;
            this.copyLibraryRuntimeFiles = other.copyLibraryRuntimeFiles;
            this.destinationDirectory = other.destinationDirectory;
            this.moduleKind = other.moduleKind;
            this.scriptDefinitionTemplateClasses = other.scriptDefinitionTemplateClasses;
            this.scriptClasspath = other.scriptClasspath;
        }
    }

    public boolean isReportCompilerWarnings() { return reportCompilerWarnings; }
    public void setReportCompilerWarnings(boolean reportCompilerWarnings) { this.reportCompilerWarnings = reportCompilerWarnings; }

    public String getKotlinCompilerVersion() { return kotlinCompilerVersion; }
    public void setKotlinCompilerVersion(String kotlinCompilerVersion) {
        this.kotlinCompilerVersion = kotlinCompilerVersion != null ? kotlinCompilerVersion : "Bundled (2.1.21-release-317)";
    }

    public String getLanguageVersion() { return languageVersion; }
    public void setLanguageVersion(String languageVersion) {
        this.languageVersion = languageVersion != null ? languageVersion : "2.1";
    }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion != null ? apiVersion : "2.1";
    }

    public String getAdditionalCommandLineParameters() { return additionalCommandLineParameters; }
    public void setAdditionalCommandLineParameters(String additionalCommandLineParameters) {
        this.additionalCommandLineParameters = additionalCommandLineParameters != null ? additionalCommandLineParameters : "";
    }

    public boolean isKeepCompilerProcessAlive() { return keepCompilerProcessAlive; }
    public void setKeepCompilerProcessAlive(boolean keepCompilerProcessAlive) { this.keepCompilerProcessAlive = keepCompilerProcessAlive; }

    public boolean isJvmEnableIncrementalCompilation() { return jvmEnableIncrementalCompilation; }
    public void setJvmEnableIncrementalCompilation(boolean jvmEnableIncrementalCompilation) {
        this.jvmEnableIncrementalCompilation = jvmEnableIncrementalCompilation;
    }

    public String getTargetJvmVersion() { return targetJvmVersion; }
    public void setTargetJvmVersion(String targetJvmVersion) {
        this.targetJvmVersion = targetJvmVersion != null ? targetJvmVersion : "1.8";
    }

    public boolean isJsEnableIncrementalCompilation() { return jsEnableIncrementalCompilation; }
    public void setJsEnableIncrementalCompilation(boolean jsEnableIncrementalCompilation) {
        this.jsEnableIncrementalCompilation = jsEnableIncrementalCompilation;
    }

    public boolean isGenerateSourceMaps() { return generateSourceMaps; }
    public void setGenerateSourceMaps(boolean generateSourceMaps) { this.generateSourceMaps = generateSourceMaps; }

    public String getSourceMapPrefix() { return sourceMapPrefix; }
    public void setSourceMapPrefix(String sourceMapPrefix) {
        this.sourceMapPrefix = sourceMapPrefix != null ? sourceMapPrefix : "";
    }

    public String getEmbedSourceCodeIntoSourceMap() { return embedSourceCodeIntoSourceMap; }
    public void setEmbedSourceCodeIntoSourceMap(String embedSourceCodeIntoSourceMap) {
        this.embedSourceCodeIntoSourceMap = embedSourceCodeIntoSourceMap != null ? embedSourceCodeIntoSourceMap : "When inlining a function from other module with embedded sources";
    }

    public boolean isCopyLibraryRuntimeFiles() { return copyLibraryRuntimeFiles; }
    public void setCopyLibraryRuntimeFiles(boolean copyLibraryRuntimeFiles) { this.copyLibraryRuntimeFiles = copyLibraryRuntimeFiles; }

    public String getDestinationDirectory() { return destinationDirectory; }
    public void setDestinationDirectory(String destinationDirectory) {
        this.destinationDirectory = destinationDirectory != null ? destinationDirectory : "lib";
    }

    public String getModuleKind() { return moduleKind; }
    public void setModuleKind(String moduleKind) {
        this.moduleKind = moduleKind != null ? moduleKind : "Plain (put to global scope)";
    }

    public String getScriptDefinitionTemplateClasses() { return scriptDefinitionTemplateClasses; }
    public void setScriptDefinitionTemplateClasses(String scriptDefinitionTemplateClasses) {
        this.scriptDefinitionTemplateClasses = scriptDefinitionTemplateClasses != null ? scriptDefinitionTemplateClasses : "";
    }

    public String getScriptClasspath() { return scriptClasspath; }
    public void setScriptClasspath(String scriptClasspath) {
        this.scriptClasspath = scriptClasspath != null ? scriptClasspath : "";
    }

    @Override
    public KotlinCompilerSettings clone() {
        return new KotlinCompilerSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KotlinCompilerSettings that = (KotlinCompilerSettings) o;
        return reportCompilerWarnings == that.reportCompilerWarnings &&
                keepCompilerProcessAlive == that.keepCompilerProcessAlive &&
                jvmEnableIncrementalCompilation == that.jvmEnableIncrementalCompilation &&
                jsEnableIncrementalCompilation == that.jsEnableIncrementalCompilation &&
                generateSourceMaps == that.generateSourceMaps &&
                copyLibraryRuntimeFiles == that.copyLibraryRuntimeFiles &&
                Objects.equals(kotlinCompilerVersion, that.kotlinCompilerVersion) &&
                Objects.equals(languageVersion, that.languageVersion) &&
                Objects.equals(apiVersion, that.apiVersion) &&
                Objects.equals(additionalCommandLineParameters, that.additionalCommandLineParameters) &&
                Objects.equals(targetJvmVersion, that.targetJvmVersion) &&
                Objects.equals(sourceMapPrefix, that.sourceMapPrefix) &&
                Objects.equals(embedSourceCodeIntoSourceMap, that.embedSourceCodeIntoSourceMap) &&
                Objects.equals(destinationDirectory, that.destinationDirectory) &&
                Objects.equals(moduleKind, that.moduleKind) &&
                Objects.equals(scriptDefinitionTemplateClasses, that.scriptDefinitionTemplateClasses) &&
                Objects.equals(scriptClasspath, that.scriptClasspath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportCompilerWarnings, kotlinCompilerVersion, languageVersion, apiVersion,
                additionalCommandLineParameters, keepCompilerProcessAlive, jvmEnableIncrementalCompilation,
                targetJvmVersion, jsEnableIncrementalCompilation, generateSourceMaps, sourceMapPrefix,
                embedSourceCodeIntoSourceMap, copyLibraryRuntimeFiles, destinationDirectory, moduleKind,
                scriptDefinitionTemplateClasses, scriptClasspath);
    }
}
