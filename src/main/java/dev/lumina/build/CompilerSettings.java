package dev.lumina.build;

import java.util.Objects;

/**
 * Configuration model for Build, Execution, Deployment > Compiler.
 * Matches standard IDE settings 1:1 (media_1791429963570.png):
 *  - Resource patterns
 *  - Clear output directory on rebuild
 *  - Add runtime assertions for notnull-annotated methods and parameters
 *  - Automatically show first error in editor
 *  - Display notification on build completion
 *  - Build project automatically
 *  - Rebuild module on dependency change
 *  - Compile independent modules in parallel: Automatic / Always / Never
 *  - Build Process:
 *    - Shared heap size (Mbytes)
 *    - Shared VM options
 *    - User-local heap size (Mbytes)
 *    - User-local VM options
 */
public class CompilerSettings implements Cloneable {

    public static final String DEFAULT_RESOURCE_PATTERNS =
            "!?*.java;!?*.form;!?*.class;!?*.groovy;!?*.scala;!?*.flex;!?*.kt;!?*.clj;!?*.aj";

    private String resourcePatterns = DEFAULT_RESOURCE_PATTERNS;
    private boolean clearOutputDirectoryOnRebuild = true;
    private boolean addRuntimeAssertionsNotNull = true;
    private boolean autoShowFirstErrorInEditor = true;
    private boolean displayNotificationOnBuildCompletion = true;
    private boolean buildProjectAutomatically = false;
    private boolean rebuildModuleOnDependencyChange = true;
    private String compileModulesInParallel = "Automatic";

    // Build Process
    private String sharedHeapSizeMb = "700";
    private String sharedVmOptions = "";
    private String userLocalHeapSizeMb = "";
    private String userLocalVmOptions = "";

    public CompilerSettings() {
    }

    public CompilerSettings(CompilerSettings other) {
        if (other != null) {
            this.resourcePatterns = other.resourcePatterns;
            this.clearOutputDirectoryOnRebuild = other.clearOutputDirectoryOnRebuild;
            this.addRuntimeAssertionsNotNull = other.addRuntimeAssertionsNotNull;
            this.autoShowFirstErrorInEditor = other.autoShowFirstErrorInEditor;
            this.displayNotificationOnBuildCompletion = other.displayNotificationOnBuildCompletion;
            this.buildProjectAutomatically = other.buildProjectAutomatically;
            this.rebuildModuleOnDependencyChange = other.rebuildModuleOnDependencyChange;
            this.compileModulesInParallel = other.compileModulesInParallel;
            this.sharedHeapSizeMb = other.sharedHeapSizeMb;
            this.sharedVmOptions = other.sharedVmOptions;
            this.userLocalHeapSizeMb = other.userLocalHeapSizeMb;
            this.userLocalVmOptions = other.userLocalVmOptions;
        }
    }

    public String getResourcePatterns() {
        return resourcePatterns != null ? resourcePatterns : DEFAULT_RESOURCE_PATTERNS;
    }

    public void setResourcePatterns(String resourcePatterns) {
        this.resourcePatterns = resourcePatterns != null ? resourcePatterns.trim() : DEFAULT_RESOURCE_PATTERNS;
    }

    public boolean isClearOutputDirectoryOnRebuild() {
        return clearOutputDirectoryOnRebuild;
    }

    public void setClearOutputDirectoryOnRebuild(boolean clearOutputDirectoryOnRebuild) {
        this.clearOutputDirectoryOnRebuild = clearOutputDirectoryOnRebuild;
    }

    public boolean isAddRuntimeAssertionsNotNull() {
        return addRuntimeAssertionsNotNull;
    }

    public void setAddRuntimeAssertionsNotNull(boolean addRuntimeAssertionsNotNull) {
        this.addRuntimeAssertionsNotNull = addRuntimeAssertionsNotNull;
    }

    public boolean isAutoShowFirstErrorInEditor() {
        return autoShowFirstErrorInEditor;
    }

    public void setAutoShowFirstErrorInEditor(boolean autoShowFirstErrorInEditor) {
        this.autoShowFirstErrorInEditor = autoShowFirstErrorInEditor;
    }

    public boolean isDisplayNotificationOnBuildCompletion() {
        return displayNotificationOnBuildCompletion;
    }

    public void setDisplayNotificationOnBuildCompletion(boolean displayNotificationOnBuildCompletion) {
        this.displayNotificationOnBuildCompletion = displayNotificationOnBuildCompletion;
    }

    public boolean isBuildProjectAutomatically() {
        return buildProjectAutomatically;
    }

    public void setBuildProjectAutomatically(boolean buildProjectAutomatically) {
        this.buildProjectAutomatically = buildProjectAutomatically;
    }

    public boolean isRebuildModuleOnDependencyChange() {
        return rebuildModuleOnDependencyChange;
    }

    public void setRebuildModuleOnDependencyChange(boolean rebuildModuleOnDependencyChange) {
        this.rebuildModuleOnDependencyChange = rebuildModuleOnDependencyChange;
    }

    public String getCompileModulesInParallel() {
        return compileModulesInParallel != null ? compileModulesInParallel : "Automatic";
    }

    public void setCompileModulesInParallel(String compileModulesInParallel) {
        this.compileModulesInParallel = compileModulesInParallel != null ? compileModulesInParallel : "Automatic";
    }

    public String getSharedHeapSizeMb() {
        return sharedHeapSizeMb != null ? sharedHeapSizeMb : "700";
    }

    public void setSharedHeapSizeMb(String sharedHeapSizeMb) {
        this.sharedHeapSizeMb = sharedHeapSizeMb != null ? sharedHeapSizeMb.trim() : "700";
    }

    public String getSharedVmOptions() {
        return sharedVmOptions != null ? sharedVmOptions : "";
    }

    public void setSharedVmOptions(String sharedVmOptions) {
        this.sharedVmOptions = sharedVmOptions != null ? sharedVmOptions : "";
    }

    public String getUserLocalHeapSizeMb() {
        return userLocalHeapSizeMb != null ? userLocalHeapSizeMb : "";
    }

    public void setUserLocalHeapSizeMb(String userLocalHeapSizeMb) {
        this.userLocalHeapSizeMb = userLocalHeapSizeMb != null ? userLocalHeapSizeMb.trim() : "";
    }

    public String getUserLocalVmOptions() {
        return userLocalVmOptions != null ? userLocalVmOptions : "";
    }

    public void setUserLocalVmOptions(String userLocalVmOptions) {
        this.userLocalVmOptions = userLocalVmOptions != null ? userLocalVmOptions : "";
    }

    @Override
    public CompilerSettings clone() {
        return new CompilerSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompilerSettings that = (CompilerSettings) o;
        return clearOutputDirectoryOnRebuild == that.clearOutputDirectoryOnRebuild &&
                addRuntimeAssertionsNotNull == that.addRuntimeAssertionsNotNull &&
                autoShowFirstErrorInEditor == that.autoShowFirstErrorInEditor &&
                displayNotificationOnBuildCompletion == that.displayNotificationOnBuildCompletion &&
                buildProjectAutomatically == that.buildProjectAutomatically &&
                rebuildModuleOnDependencyChange == that.rebuildModuleOnDependencyChange &&
                Objects.equals(resourcePatterns, that.resourcePatterns) &&
                Objects.equals(compileModulesInParallel, that.compileModulesInParallel) &&
                Objects.equals(sharedHeapSizeMb, that.sharedHeapSizeMb) &&
                Objects.equals(sharedVmOptions, that.sharedVmOptions) &&
                Objects.equals(userLocalHeapSizeMb, that.userLocalHeapSizeMb) &&
                Objects.equals(userLocalVmOptions, that.userLocalVmOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourcePatterns, clearOutputDirectoryOnRebuild, addRuntimeAssertionsNotNull,
                autoShowFirstErrorInEditor, displayNotificationOnBuildCompletion, buildProjectAutomatically,
                rebuildModuleOnDependencyChange, compileModulesInParallel, sharedHeapSizeMb,
                sharedVmOptions, userLocalHeapSizeMb, userLocalVmOptions);
    }
}
