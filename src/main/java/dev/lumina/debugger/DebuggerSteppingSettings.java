package dev.lumina.debugger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings for Build, Execution, Deployment > Debugger > Stepping (Images 3, 4, 5).
 */
public class DebuggerSteppingSettings implements Cloneable {

    public enum EvaluateFinallyMode {
        ALWAYS,
        NEVER,
        ASK
    }

    // Java section
    private boolean alwaysSmartStepInto = true;
    private boolean skipSyntheticMethods = false;
    private boolean skipConstructors = false;
    private boolean skipClassLoaders = true;
    private boolean skipSimpleGetters = false;
    private boolean filterClasses = true;
    private List<SteppingFilter> classFilters = new ArrayList<>();
    private boolean hideStackFramesUsingSteppingFilters = true;
    private EvaluateFinallyMode evaluateFinallyBlocks = EvaluateFinallyMode.ASK;
    private boolean resumeOnlyCurrentThread = false;

    // Groovy section
    private boolean skipSpecificGroovyClasses = true;

    // Oracle section
    private String oracleSteppingMode = "Graceful";
    private boolean oraclePauseAtBegin = false;

    // Kotlin section
    private boolean kotlinSkipRuntimeLibraryClasses = true;
    private boolean kotlinAlwaysSmartStepInto = true;

    // JavaScript section
    private boolean jsAlwaysSmartStepInto = true;
    private boolean jsSkipLibraryScripts = true;
    private boolean jsSkipScripts = true;
    private List<SteppingFilter> jsScriptFilters = new ArrayList<>();

    public DebuggerSteppingSettings() {
        initDefaults();
    }

    public DebuggerSteppingSettings(DebuggerSteppingSettings other) {
        if (other != null) {
            this.alwaysSmartStepInto = other.alwaysSmartStepInto;
            this.skipSyntheticMethods = other.skipSyntheticMethods;
            this.skipConstructors = other.skipConstructors;
            this.skipClassLoaders = other.skipClassLoaders;
            this.skipSimpleGetters = other.skipSimpleGetters;
            this.filterClasses = other.filterClasses;
            this.classFilters = new ArrayList<>();
            for (SteppingFilter f : other.classFilters) {
                this.classFilters.add(f.clone());
            }
            this.hideStackFramesUsingSteppingFilters = other.hideStackFramesUsingSteppingFilters;
            this.evaluateFinallyBlocks = other.evaluateFinallyBlocks != null ? other.evaluateFinallyBlocks : EvaluateFinallyMode.ASK;
            this.resumeOnlyCurrentThread = other.resumeOnlyCurrentThread;

            this.skipSpecificGroovyClasses = other.skipSpecificGroovyClasses;

            this.oracleSteppingMode = other.oracleSteppingMode != null ? other.oracleSteppingMode : "Graceful";
            this.oraclePauseAtBegin = other.oraclePauseAtBegin;

            this.kotlinSkipRuntimeLibraryClasses = other.kotlinSkipRuntimeLibraryClasses;
            this.kotlinAlwaysSmartStepInto = other.kotlinAlwaysSmartStepInto;

            this.jsAlwaysSmartStepInto = other.jsAlwaysSmartStepInto;
            this.jsSkipLibraryScripts = other.jsSkipLibraryScripts;
            this.jsSkipScripts = other.jsSkipScripts;
            this.jsScriptFilters = new ArrayList<>();
            for (SteppingFilter f : other.jsScriptFilters) {
                this.jsScriptFilters.add(f.clone());
            }
        }
    }

    public void initDefaults() {
        this.alwaysSmartStepInto = true;
        this.skipSyntheticMethods = false;
        this.skipConstructors = false;
        this.skipClassLoaders = true;
        this.skipSimpleGetters = false;
        this.filterClasses = true;

        this.classFilters = new ArrayList<>(List.of(
                new SteppingFilter(true, "com.sun.*"),
                new SteppingFilter(true, "java.*"),
                new SteppingFilter(true, "javax.*"),
                new SteppingFilter(true, "org.omg.*"),
                new SteppingFilter(true, "sun.*"),
                new SteppingFilter(true, "jdk.internal.*"),
                new SteppingFilter(true, "junit.*"),
                new SteppingFilter(true, "org.junit.*"),
                new SteppingFilter(true, "com.intellij.rt.*"),
                new SteppingFilter(true, "com.yourkit.runtime.*"),
                new SteppingFilter(true, "com.springsource.loaded.*"),
                new SteppingFilter(true, "org.springsource.loaded.*"),
                new SteppingFilter(true, "javassist.*"),
                new SteppingFilter(true, "com.ibm.ws.*"),
                new SteppingFilter(true, "org.mockito.*"),
                new SteppingFilter(true, "com.jetbrains.internal.IoOverNio*"),
                new SteppingFilter(true, "com.azul.*"),
                new SteppingFilter(true, "kotlin.*"),
                new SteppingFilter(true, "kotlinx.*"),
                new SteppingFilter(true, "androidx.compose.runtime.*")
        ));

        this.hideStackFramesUsingSteppingFilters = true;
        this.evaluateFinallyBlocks = EvaluateFinallyMode.ASK;
        this.resumeOnlyCurrentThread = false;

        this.skipSpecificGroovyClasses = true;

        this.oracleSteppingMode = "Graceful";
        this.oraclePauseAtBegin = false;

        this.kotlinSkipRuntimeLibraryClasses = true;
        this.kotlinAlwaysSmartStepInto = true;

        this.jsAlwaysSmartStepInto = true;
        this.jsSkipLibraryScripts = true;
        this.jsSkipScripts = true;
        this.jsScriptFilters = new ArrayList<>();
    }

    public boolean isAlwaysSmartStepInto() {
        return alwaysSmartStepInto;
    }

    public void setAlwaysSmartStepInto(boolean alwaysSmartStepInto) {
        this.alwaysSmartStepInto = alwaysSmartStepInto;
    }

    public boolean isSkipSyntheticMethods() {
        return skipSyntheticMethods;
    }

    public void setSkipSyntheticMethods(boolean skipSyntheticMethods) {
        this.skipSyntheticMethods = skipSyntheticMethods;
    }

    public boolean isSkipConstructors() {
        return skipConstructors;
    }

    public void setSkipConstructors(boolean skipConstructors) {
        this.skipConstructors = skipConstructors;
    }

    public boolean isSkipClassLoaders() {
        return skipClassLoaders;
    }

    public void setSkipClassLoaders(boolean skipClassLoaders) {
        this.skipClassLoaders = skipClassLoaders;
    }

    public boolean isSkipSimpleGetters() {
        return skipSimpleGetters;
    }

    public void setSkipSimpleGetters(boolean skipSimpleGetters) {
        this.skipSimpleGetters = skipSimpleGetters;
    }

    public boolean isFilterClasses() {
        return filterClasses;
    }

    public void setFilterClasses(boolean filterClasses) {
        this.filterClasses = filterClasses;
    }

    public List<SteppingFilter> getClassFilters() {
        return classFilters;
    }

    public void setClassFilters(List<SteppingFilter> classFilters) {
        this.classFilters = classFilters != null ? new ArrayList<>(classFilters) : new ArrayList<>();
    }

    public boolean isHideStackFramesUsingSteppingFilters() {
        return hideStackFramesUsingSteppingFilters;
    }

    public void setHideStackFramesUsingSteppingFilters(boolean hideStackFramesUsingSteppingFilters) {
        this.hideStackFramesUsingSteppingFilters = hideStackFramesUsingSteppingFilters;
    }

    public EvaluateFinallyMode getEvaluateFinallyBlocks() {
        return evaluateFinallyBlocks;
    }

    public void setEvaluateFinallyBlocks(EvaluateFinallyMode evaluateFinallyBlocks) {
        this.evaluateFinallyBlocks = evaluateFinallyBlocks != null ? evaluateFinallyBlocks : EvaluateFinallyMode.ASK;
    }

    public boolean isResumeOnlyCurrentThread() {
        return resumeOnlyCurrentThread;
    }

    public void setResumeOnlyCurrentThread(boolean resumeOnlyCurrentThread) {
        this.resumeOnlyCurrentThread = resumeOnlyCurrentThread;
    }

    public boolean isSkipSpecificGroovyClasses() {
        return skipSpecificGroovyClasses;
    }

    public void setSkipSpecificGroovyClasses(boolean skipSpecificGroovyClasses) {
        this.skipSpecificGroovyClasses = skipSpecificGroovyClasses;
    }

    public String getOracleSteppingMode() {
        return oracleSteppingMode;
    }

    public void setOracleSteppingMode(String oracleSteppingMode) {
        this.oracleSteppingMode = oracleSteppingMode != null ? oracleSteppingMode : "Graceful";
    }

    public boolean isOraclePauseAtBegin() {
        return oraclePauseAtBegin;
    }

    public void setOraclePauseAtBegin(boolean oraclePauseAtBegin) {
        this.oraclePauseAtBegin = oraclePauseAtBegin;
    }

    public boolean isKotlinSkipRuntimeLibraryClasses() {
        return kotlinSkipRuntimeLibraryClasses;
    }

    public void setKotlinSkipRuntimeLibraryClasses(boolean kotlinSkipRuntimeLibraryClasses) {
        this.kotlinSkipRuntimeLibraryClasses = kotlinSkipRuntimeLibraryClasses;
    }

    public boolean isKotlinAlwaysSmartStepInto() {
        return kotlinAlwaysSmartStepInto;
    }

    public void setKotlinAlwaysSmartStepInto(boolean kotlinAlwaysSmartStepInto) {
        this.kotlinAlwaysSmartStepInto = kotlinAlwaysSmartStepInto;
    }

    public boolean isJsAlwaysSmartStepInto() {
        return jsAlwaysSmartStepInto;
    }

    public void setJsAlwaysSmartStepInto(boolean jsAlwaysSmartStepInto) {
        this.jsAlwaysSmartStepInto = jsAlwaysSmartStepInto;
    }

    public boolean isJsSkipLibraryScripts() {
        return jsSkipLibraryScripts;
    }

    public void setJsSkipLibraryScripts(boolean jsSkipLibraryScripts) {
        this.jsSkipLibraryScripts = jsSkipLibraryScripts;
    }

    public boolean isJsSkipScripts() {
        return jsSkipScripts;
    }

    public void setJsSkipScripts(boolean jsSkipScripts) {
        this.jsSkipScripts = jsSkipScripts;
    }

    public List<SteppingFilter> getJsScriptFilters() {
        return jsScriptFilters;
    }

    public void setJsScriptFilters(List<SteppingFilter> jsScriptFilters) {
        this.jsScriptFilters = jsScriptFilters != null ? new ArrayList<>(jsScriptFilters) : new ArrayList<>();
    }

    // Convenience aliases
    public boolean isFilterSyntheticMethods() { return isSkipSyntheticMethods(); }
    public void setFilterSyntheticMethods(boolean v) { setSkipSyntheticMethods(v); }
    public boolean isFilterClassConstructors() { return isSkipConstructors(); }
    public void setFilterClassConstructors(boolean v) { setSkipConstructors(v); }
    public boolean isFilterClassInitializers() { return !isSkipClassLoaders(); }
    public void setFilterClassInitializers(boolean v) { setSkipClassLoaders(!v); }
    public boolean isFilterSimpleGetters() { return isSkipSimpleGetters(); }
    public void setFilterSimpleGetters(boolean v) { setSkipSimpleGetters(v); }
    public boolean isFilterSimpleSynthetics() { return isSkipSyntheticMethods(); }
    public void setFilterSimpleSynthetics(boolean v) { setSkipSyntheticMethods(v); }

    public EvaluateFinallyMode getEvaluateFinallyOnPopFrame() { return getEvaluateFinallyBlocks(); }
    public void setEvaluateFinallyOnPopFrame(EvaluateFinallyMode m) { setEvaluateFinallyBlocks(m); }

    public boolean isGracefulStepIntoMethodCalls() { return "Graceful".equalsIgnoreCase(oracleSteppingMode); }
    public void setGracefulStepIntoMethodCalls(boolean v) { this.oracleSteppingMode = v ? "Graceful" : "None"; }

    public boolean isFilterGroovyCoreClasses() { return isSkipSpecificGroovyClasses(); }
    public void setFilterGroovyCoreClasses(boolean v) { setSkipSpecificGroovyClasses(v); }

    public boolean isFilterKotlinStdlib() { return isKotlinSkipRuntimeLibraryClasses(); }
    public void setFilterKotlinStdlib(boolean v) { setKotlinSkipRuntimeLibraryClasses(v); }

    public boolean isFilterJsLibraryScripts() { return isJsSkipLibraryScripts(); }
    public void setFilterJsLibraryScripts(boolean v) { setJsSkipLibraryScripts(v); }

    public List<SteppingFilter> getStepFilters() { return getClassFilters(); }
    public void setStepFilters(List<SteppingFilter> filters) { setClassFilters(filters); }

    /**
     * Tests if any active stepping filter matches the specified class name.
     */
    public boolean matchesAnyFilter(String className) {
        if (!filterClasses || className == null || classFilters == null) {
            return false;
        }
        for (SteppingFilter f : classFilters) {
            if (f.matches(className)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DebuggerSteppingSettings that)) return false;
        return alwaysSmartStepInto == that.alwaysSmartStepInto &&
                skipSyntheticMethods == that.skipSyntheticMethods &&
                skipConstructors == that.skipConstructors &&
                skipClassLoaders == that.skipClassLoaders &&
                skipSimpleGetters == that.skipSimpleGetters &&
                filterClasses == that.filterClasses &&
                hideStackFramesUsingSteppingFilters == that.hideStackFramesUsingSteppingFilters &&
                resumeOnlyCurrentThread == that.resumeOnlyCurrentThread &&
                skipSpecificGroovyClasses == that.skipSpecificGroovyClasses &&
                oraclePauseAtBegin == that.oraclePauseAtBegin &&
                kotlinSkipRuntimeLibraryClasses == that.kotlinSkipRuntimeLibraryClasses &&
                kotlinAlwaysSmartStepInto == that.kotlinAlwaysSmartStepInto &&
                jsAlwaysSmartStepInto == that.jsAlwaysSmartStepInto &&
                jsSkipLibraryScripts == that.jsSkipLibraryScripts &&
                jsSkipScripts == that.jsSkipScripts &&
                evaluateFinallyBlocks == that.evaluateFinallyBlocks &&
                Objects.equals(classFilters, that.classFilters) &&
                Objects.equals(oracleSteppingMode, that.oracleSteppingMode) &&
                Objects.equals(jsScriptFilters, that.jsScriptFilters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alwaysSmartStepInto, skipSyntheticMethods, skipConstructors,
                skipClassLoaders, skipSimpleGetters, filterClasses, classFilters,
                hideStackFramesUsingSteppingFilters, evaluateFinallyBlocks, resumeOnlyCurrentThread,
                skipSpecificGroovyClasses, oracleSteppingMode, oraclePauseAtBegin,
                kotlinSkipRuntimeLibraryClasses, kotlinAlwaysSmartStepInto,
                jsAlwaysSmartStepInto, jsSkipLibraryScripts, jsSkipScripts, jsScriptFilters);
    }

    @Override
    public DebuggerSteppingSettings clone() {
        return new DebuggerSteppingSettings(this);
    }
}
