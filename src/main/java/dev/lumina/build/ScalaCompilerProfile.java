package dev.lumina.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a Scala Compiler configuration profile in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791449910603_f5140118.png.
 */
public class ScalaCompilerProfile implements Cloneable {

    private String name = "Default";
    private List<String> modules = new ArrayList<>();
    private String compileOrder = "Mixed";

    // Features
    private boolean dynamics = false;
    private boolean postfixOperatorNotation = false;
    private boolean reflectiveCalls = false;
    private boolean implicitConversions = false;
    private boolean higherKindedTypes = false;
    private boolean existentialTypes = false;
    private boolean macros = false;
    private boolean experimentalFeatures = false;

    // Options
    private boolean enableWarnings = true;
    private boolean deprecationWarnings = false;
    private boolean uncheckedWarnings = false;
    private boolean featureWarnings = false;
    private boolean optimiseBytecode = false;
    private boolean explainTypeErrors = false;
    private boolean enableSpecialization = true;
    private boolean enableContinuations = false;

    private String debuggingInfoLevel = "Source, line number and local variable information";
    private String additionalCompilerOptions = "";
    private List<String> compilerPlugins = new ArrayList<>();

    public ScalaCompilerProfile() {
    }

    public ScalaCompilerProfile(String name) {
        this.name = name != null ? name : "Default";
    }

    public ScalaCompilerProfile(ScalaCompilerProfile other) {
        if (other != null) {
            this.name = other.name;
            this.modules = new ArrayList<>(other.modules);
            this.compileOrder = other.compileOrder;

            this.dynamics = other.dynamics;
            this.postfixOperatorNotation = other.postfixOperatorNotation;
            this.reflectiveCalls = other.reflectiveCalls;
            this.implicitConversions = other.implicitConversions;
            this.higherKindedTypes = other.higherKindedTypes;
            this.existentialTypes = other.existentialTypes;
            this.macros = other.macros;
            this.experimentalFeatures = other.experimentalFeatures;

            this.enableWarnings = other.enableWarnings;
            this.deprecationWarnings = other.deprecationWarnings;
            this.uncheckedWarnings = other.uncheckedWarnings;
            this.featureWarnings = other.featureWarnings;
            this.optimiseBytecode = other.optimiseBytecode;
            this.explainTypeErrors = other.explainTypeErrors;
            this.enableSpecialization = other.enableSpecialization;
            this.enableContinuations = other.enableContinuations;

            this.debuggingInfoLevel = other.debuggingInfoLevel;
            this.additionalCompilerOptions = other.additionalCompilerOptions;
            this.compilerPlugins = new ArrayList<>(other.compilerPlugins);
        }
    }

    /**
     * Dynamically detects the current project module name without hardcoding.
     */
    public static String detectCurrentModuleName() {
        String dir = System.getProperty("user.dir", "");
        if (!dir.isBlank()) {
            File f = new File(dir);
            String name = f.getName();
            if (!name.isBlank() && !name.equals("others") && !name.equals("projects")) {
                return name;
            }
        }
        return "ERPApplicationServer";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name : "Default"; }

    public List<String> getModules() { return modules != null ? modules : new ArrayList<>(); }
    public void setModules(List<String> modules) { this.modules = modules != null ? new ArrayList<>(modules) : new ArrayList<>(); }

    public String getCompileOrder() { return compileOrder; }
    public void setCompileOrder(String compileOrder) { this.compileOrder = compileOrder != null ? compileOrder : "Mixed"; }

    public boolean isDynamics() { return dynamics; }
    public void setDynamics(boolean dynamics) { this.dynamics = dynamics; }

    public boolean isPostfixOperatorNotation() { return postfixOperatorNotation; }
    public void setPostfixOperatorNotation(boolean postfixOperatorNotation) { this.postfixOperatorNotation = postfixOperatorNotation; }

    public boolean isReflectiveCalls() { return reflectiveCalls; }
    public void setReflectiveCalls(boolean reflectiveCalls) { this.reflectiveCalls = reflectiveCalls; }

    public boolean isImplicitConversions() { return implicitConversions; }
    public void setImplicitConversions(boolean implicitConversions) { this.implicitConversions = implicitConversions; }

    public boolean isHigherKindedTypes() { return higherKindedTypes; }
    public void setHigherKindedTypes(boolean higherKindedTypes) { this.higherKindedTypes = higherKindedTypes; }

    public boolean isExistentialTypes() { return existentialTypes; }
    public void setExistentialTypes(boolean existentialTypes) { this.existentialTypes = existentialTypes; }

    public boolean isMacros() { return macros; }
    public void setMacros(boolean macros) { this.macros = macros; }

    public boolean isExperimentalFeatures() { return experimentalFeatures; }
    public void setExperimentalFeatures(boolean experimentalFeatures) { this.experimentalFeatures = experimentalFeatures; }

    public boolean isEnableWarnings() { return enableWarnings; }
    public void setEnableWarnings(boolean enableWarnings) { this.enableWarnings = enableWarnings; }

    public boolean isDeprecationWarnings() { return deprecationWarnings; }
    public void setDeprecationWarnings(boolean deprecationWarnings) { this.deprecationWarnings = deprecationWarnings; }

    public boolean isUncheckedWarnings() { return uncheckedWarnings; }
    public void setUncheckedWarnings(boolean uncheckedWarnings) { this.uncheckedWarnings = uncheckedWarnings; }

    public boolean isFeatureWarnings() { return featureWarnings; }
    public void setFeatureWarnings(boolean featureWarnings) { this.featureWarnings = featureWarnings; }

    public boolean isOptimiseBytecode() { return optimiseBytecode; }
    public void setOptimiseBytecode(boolean optimiseBytecode) { this.optimiseBytecode = optimiseBytecode; }

    public boolean isExplainTypeErrors() { return explainTypeErrors; }
    public void setExplainTypeErrors(boolean explainTypeErrors) { this.explainTypeErrors = explainTypeErrors; }

    public boolean isEnableSpecialization() { return enableSpecialization; }
    public void setEnableSpecialization(boolean enableSpecialization) { this.enableSpecialization = enableSpecialization; }

    public boolean isEnableContinuations() { return enableContinuations; }
    public void setEnableContinuations(boolean enableContinuations) { this.enableContinuations = enableContinuations; }

    public String getDebuggingInfoLevel() { return debuggingInfoLevel; }
    public void setDebuggingInfoLevel(String debuggingInfoLevel) {
        this.debuggingInfoLevel = debuggingInfoLevel != null ? debuggingInfoLevel : "Source, line number and local variable information";
    }

    public String getAdditionalCompilerOptions() { return additionalCompilerOptions; }
    public void setAdditionalCompilerOptions(String additionalCompilerOptions) {
        this.additionalCompilerOptions = additionalCompilerOptions != null ? additionalCompilerOptions : "";
    }

    public List<String> getCompilerPlugins() { return compilerPlugins != null ? compilerPlugins : new ArrayList<>(); }
    public void setCompilerPlugins(List<String> compilerPlugins) {
        this.compilerPlugins = compilerPlugins != null ? new ArrayList<>(compilerPlugins) : new ArrayList<>();
    }

    @Override
    public ScalaCompilerProfile clone() {
        return new ScalaCompilerProfile(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaCompilerProfile that = (ScalaCompilerProfile) o;
        return dynamics == that.dynamics &&
                postfixOperatorNotation == that.postfixOperatorNotation &&
                reflectiveCalls == that.reflectiveCalls &&
                implicitConversions == that.implicitConversions &&
                higherKindedTypes == that.higherKindedTypes &&
                existentialTypes == that.existentialTypes &&
                macros == that.macros &&
                experimentalFeatures == that.experimentalFeatures &&
                enableWarnings == that.enableWarnings &&
                deprecationWarnings == that.deprecationWarnings &&
                uncheckedWarnings == that.uncheckedWarnings &&
                featureWarnings == that.featureWarnings &&
                optimiseBytecode == that.optimiseBytecode &&
                explainTypeErrors == that.explainTypeErrors &&
                enableSpecialization == that.enableSpecialization &&
                enableContinuations == that.enableContinuations &&
                Objects.equals(name, that.name) &&
                Objects.equals(modules, that.modules) &&
                Objects.equals(compileOrder, that.compileOrder) &&
                Objects.equals(debuggingInfoLevel, that.debuggingInfoLevel) &&
                Objects.equals(additionalCompilerOptions, that.additionalCompilerOptions) &&
                Objects.equals(compilerPlugins, that.compilerPlugins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, modules, compileOrder, dynamics, postfixOperatorNotation, reflectiveCalls,
                implicitConversions, higherKindedTypes, existentialTypes, macros, experimentalFeatures,
                enableWarnings, deprecationWarnings, uncheckedWarnings, featureWarnings, optimiseBytecode,
                explainTypeErrors, enableSpecialization, enableContinuations, debuggingInfoLevel,
                additionalCompilerOptions, compilerPlugins);
    }
}
