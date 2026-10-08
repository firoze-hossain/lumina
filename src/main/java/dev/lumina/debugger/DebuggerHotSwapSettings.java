package dev.lumina.debugger;

import java.util.Objects;

/**
 * Settings for Build, Execution, Deployment > Debugger > HotSwap (Image 2).
 */
public class DebuggerHotSwapSettings implements Cloneable {

    public enum ReloadMode {
        ALWAYS,
        NEVER,
        ASK
    }

    public enum ReloadClassesMode {
        ALWAYS,
        NEVER,
        ASK
    }

    // Java section
    private boolean buildProjectBeforeReloading = true;
    private boolean enableJvmWillHangWarning = false;
    private boolean suggestHotSwapInEditor = true;
    private ReloadMode reloadClassesAfterCompilation = ReloadMode.ASK;

    // Groovy section
    private boolean enableHotSwapAgentForGroovy = true;

    public DebuggerHotSwapSettings() {
    }

    public DebuggerHotSwapSettings(DebuggerHotSwapSettings other) {
        if (other != null) {
            this.buildProjectBeforeReloading = other.buildProjectBeforeReloading;
            this.enableJvmWillHangWarning = other.enableJvmWillHangWarning;
            this.suggestHotSwapInEditor = other.suggestHotSwapInEditor;
            this.reloadClassesAfterCompilation = other.reloadClassesAfterCompilation != null ? other.reloadClassesAfterCompilation : ReloadMode.ASK;
            this.enableHotSwapAgentForGroovy = other.enableHotSwapAgentForGroovy;
        }
    }

    public boolean isBuildProjectBeforeReloading() {
        return buildProjectBeforeReloading;
    }

    public void setBuildProjectBeforeReloading(boolean buildProjectBeforeReloading) {
        this.buildProjectBeforeReloading = buildProjectBeforeReloading;
    }

    public boolean isEnableJvmWillHangWarning() {
        return enableJvmWillHangWarning;
    }

    public void setEnableJvmWillHangWarning(boolean enableJvmWillHangWarning) {
        this.enableJvmWillHangWarning = enableJvmWillHangWarning;
    }

    public boolean isSuggestHotSwapInEditor() {
        return suggestHotSwapInEditor;
    }

    public void setSuggestHotSwapInEditor(boolean suggestHotSwapInEditor) {
        this.suggestHotSwapInEditor = suggestHotSwapInEditor;
    }

    public ReloadMode getReloadClassesAfterCompilation() {
        return reloadClassesAfterCompilation;
    }

    public void setReloadClassesAfterCompilation(ReloadMode reloadClassesAfterCompilation) {
        this.reloadClassesAfterCompilation = reloadClassesAfterCompilation != null ? reloadClassesAfterCompilation : ReloadMode.ASK;
    }

    public boolean isEnableHotSwapAgentForGroovy() {
        return enableHotSwapAgentForGroovy;
    }

    public void setEnableHotSwapAgentForGroovy(boolean enableHotSwapAgentForGroovy) {
        this.enableHotSwapAgentForGroovy = enableHotSwapAgentForGroovy;
    }

    // Convenience aliases
    public boolean isCompileBeforeHotSwap() {
        return isBuildProjectBeforeReloading();
    }

    public void setCompileBeforeHotSwap(boolean compileBeforeHotSwap) {
        setBuildProjectBeforeReloading(compileBeforeHotSwap);
    }

    public boolean isShowHangWarning() {
        return isEnableJvmWillHangWarning();
    }

    public void setShowHangWarning(boolean showHangWarning) {
        setEnableJvmWillHangWarning(showHangWarning);
    }

    public boolean isSuggestHotSwap() {
        return isSuggestHotSwapInEditor();
    }

    public void setSuggestHotSwap(boolean suggestHotSwap) {
        setSuggestHotSwapInEditor(suggestHotSwap);
    }

    public ReloadClassesMode getReloadClasses() {
        return reloadClassesAfterCompilation != null ? ReloadClassesMode.valueOf(reloadClassesAfterCompilation.name()) : ReloadClassesMode.ASK;
    }

    public void setReloadClasses(ReloadClassesMode reloadClasses) {
        if (reloadClasses != null) {
            this.reloadClassesAfterCompilation = ReloadMode.valueOf(reloadClasses.name());
        }
    }

    public boolean isEnableGroovyHotSwap() {
        return isEnableHotSwapAgentForGroovy();
    }

    public void setEnableGroovyHotSwap(boolean enableGroovyHotSwap) {
        setEnableHotSwapAgentForGroovy(enableGroovyHotSwap);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DebuggerHotSwapSettings that)) return false;
        return buildProjectBeforeReloading == that.buildProjectBeforeReloading &&
                enableJvmWillHangWarning == that.enableJvmWillHangWarning &&
                suggestHotSwapInEditor == that.suggestHotSwapInEditor &&
                enableHotSwapAgentForGroovy == that.enableHotSwapAgentForGroovy &&
                reloadClassesAfterCompilation == that.reloadClassesAfterCompilation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildProjectBeforeReloading, enableJvmWillHangWarning,
                suggestHotSwapInEditor, reloadClassesAfterCompilation,
                enableHotSwapAgentForGroovy);
    }

    @Override
    public DebuggerHotSwapSettings clone() {
        return new DebuggerHotSwapSettings(this);
    }
}
