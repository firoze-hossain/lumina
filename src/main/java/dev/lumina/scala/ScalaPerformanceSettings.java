package dev.lumina.scala;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > Scala > Performance settings.
 * Matches Image 2:
 *  - Implicit parameters search depth (-1 for none): default -1
 *  - Execution of scala.meta programs: default "Enabled" ("Enabled", "Disabled")
 *  - Local Ivy cache indexing mode: default "Metadata" ("Metadata", "Full", "None")
 *  - Trim method bodies expanded by scala.meta: default true
 *  - Search all symbols (include locals): default false
 *  - Disable parsing of documentation comments: default false
 *  - Disable language injection in Scala files: default false
 *  - Don't cache compound types: default false
 */
public class ScalaPerformanceSettings {

    private int implicitParametersSearchDepth = -1;
    private String scalaMetaProgramsExecution = "Enabled";
    private String localIvyCacheIndexingMode = "Metadata";
    private boolean trimMethodBodiesExpandedByScalaMeta = true;
    private boolean searchAllSymbolsIncludeLocals = false;
    private boolean disableParsingOfDocComments = false;
    private boolean disableLanguageInjectionInScalaFiles = false;
    private boolean dontCacheCompoundTypes = false;

    public ScalaPerformanceSettings() {
    }

    public ScalaPerformanceSettings(int implicitParametersSearchDepth,
                                    String scalaMetaProgramsExecution,
                                    String localIvyCacheIndexingMode,
                                    boolean trimMethodBodiesExpandedByScalaMeta,
                                    boolean searchAllSymbolsIncludeLocals,
                                    boolean disableParsingOfDocComments,
                                    boolean disableLanguageInjectionInScalaFiles,
                                    boolean dontCacheCompoundTypes) {
        this.implicitParametersSearchDepth = implicitParametersSearchDepth;
        this.scalaMetaProgramsExecution = scalaMetaProgramsExecution;
        this.localIvyCacheIndexingMode = localIvyCacheIndexingMode;
        this.trimMethodBodiesExpandedByScalaMeta = trimMethodBodiesExpandedByScalaMeta;
        this.searchAllSymbolsIncludeLocals = searchAllSymbolsIncludeLocals;
        this.disableParsingOfDocComments = disableParsingOfDocComments;
        this.disableLanguageInjectionInScalaFiles = disableLanguageInjectionInScalaFiles;
        this.dontCacheCompoundTypes = dontCacheCompoundTypes;
    }

    public int getImplicitParametersSearchDepth() {
        return implicitParametersSearchDepth;
    }

    public void setImplicitParametersSearchDepth(int implicitParametersSearchDepth) {
        this.implicitParametersSearchDepth = implicitParametersSearchDepth;
    }

    public String getScalaMetaProgramsExecution() {
        return scalaMetaProgramsExecution;
    }

    public void setScalaMetaProgramsExecution(String scalaMetaProgramsExecution) {
        this.scalaMetaProgramsExecution = scalaMetaProgramsExecution;
    }

    public String getLocalIvyCacheIndexingMode() {
        return localIvyCacheIndexingMode;
    }

    public void setLocalIvyCacheIndexingMode(String localIvyCacheIndexingMode) {
        this.localIvyCacheIndexingMode = localIvyCacheIndexingMode;
    }

    public boolean isTrimMethodBodiesExpandedByScalaMeta() {
        return trimMethodBodiesExpandedByScalaMeta;
    }

    public void setTrimMethodBodiesExpandedByScalaMeta(boolean trimMethodBodiesExpandedByScalaMeta) {
        this.trimMethodBodiesExpandedByScalaMeta = trimMethodBodiesExpandedByScalaMeta;
    }

    public boolean isSearchAllSymbolsIncludeLocals() {
        return searchAllSymbolsIncludeLocals;
    }

    public void setSearchAllSymbolsIncludeLocals(boolean searchAllSymbolsIncludeLocals) {
        this.searchAllSymbolsIncludeLocals = searchAllSymbolsIncludeLocals;
    }

    public boolean isDisableParsingOfDocComments() {
        return disableParsingOfDocComments;
    }

    public void setDisableParsingOfDocComments(boolean disableParsingOfDocComments) {
        this.disableParsingOfDocComments = disableParsingOfDocComments;
    }

    public boolean isDisableLanguageInjectionInScalaFiles() {
        return disableLanguageInjectionInScalaFiles;
    }

    public void setDisableLanguageInjectionInScalaFiles(boolean disableLanguageInjectionInScalaFiles) {
        this.disableLanguageInjectionInScalaFiles = disableLanguageInjectionInScalaFiles;
    }

    public boolean isDontCacheCompoundTypes() {
        return dontCacheCompoundTypes;
    }

    public void setDontCacheCompoundTypes(boolean dontCacheCompoundTypes) {
        this.dontCacheCompoundTypes = dontCacheCompoundTypes;
    }

    public ScalaPerformanceSettings copy() {
        return new ScalaPerformanceSettings(
                implicitParametersSearchDepth,
                scalaMetaProgramsExecution,
                localIvyCacheIndexingMode,
                trimMethodBodiesExpandedByScalaMeta,
                searchAllSymbolsIncludeLocals,
                disableParsingOfDocComments,
                disableLanguageInjectionInScalaFiles,
                dontCacheCompoundTypes
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaPerformanceSettings that = (ScalaPerformanceSettings) o;
        return implicitParametersSearchDepth == that.implicitParametersSearchDepth &&
                trimMethodBodiesExpandedByScalaMeta == that.trimMethodBodiesExpandedByScalaMeta &&
                searchAllSymbolsIncludeLocals == that.searchAllSymbolsIncludeLocals &&
                disableParsingOfDocComments == that.disableParsingOfDocComments &&
                disableLanguageInjectionInScalaFiles == that.disableLanguageInjectionInScalaFiles &&
                dontCacheCompoundTypes == that.dontCacheCompoundTypes &&
                Objects.equals(scalaMetaProgramsExecution, that.scalaMetaProgramsExecution) &&
                Objects.equals(localIvyCacheIndexingMode, that.localIvyCacheIndexingMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(implicitParametersSearchDepth, scalaMetaProgramsExecution,
                localIvyCacheIndexingMode, trimMethodBodiesExpandedByScalaMeta,
                searchAllSymbolsIncludeLocals, disableParsingOfDocComments,
                disableLanguageInjectionInScalaFiles, dontCacheCompoundTypes);
    }

    @Override
    public String toString() {
        return "ScalaPerformanceSettings{" +
                "implicitParametersSearchDepth=" + implicitParametersSearchDepth +
                ", scalaMetaProgramsExecution='" + scalaMetaProgramsExecution + '\'' +
                ", localIvyCacheIndexingMode='" + localIvyCacheIndexingMode + '\'' +
                ", trimMethodBodiesExpandedByScalaMeta=" + trimMethodBodiesExpandedByScalaMeta +
                ", searchAllSymbolsIncludeLocals=" + searchAllSymbolsIncludeLocals +
                ", disableParsingOfDocComments=" + disableParsingOfDocComments +
                ", disableLanguageInjectionInScalaFiles=" + disableLanguageInjectionInScalaFiles +
                ", dontCacheCompoundTypes=" + dontCacheCompoundTypes +
                '}';
    }
}
