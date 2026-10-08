package dev.lumina.build;

import java.util.Objects;

/**
 * Configuration model for Scala Compiler > Bytecode Indices in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791449926979_4ed8cb9f.png:
 *  - Index .class files (with Delete indices button)
 *  - Use indices to search for usages of:
 *    - Implicit definitions
 *    - apply / unapply methods
 *    - SAM types
 *    - For-comprehension methods (map, withFilter, flatMap, foreach)
 */
public class ScalaBytecodeIndicesSettings implements Cloneable {

    private boolean indexClassFiles = true;
    private boolean implicitDefinitions = true;
    private boolean applyUnapplyMethods = true;
    private boolean samTypes = true;
    private boolean forComprehensionMethods = true;

    public ScalaBytecodeIndicesSettings() {
    }

    public ScalaBytecodeIndicesSettings(ScalaBytecodeIndicesSettings other) {
        if (other != null) {
            this.indexClassFiles = other.indexClassFiles;
            this.implicitDefinitions = other.implicitDefinitions;
            this.applyUnapplyMethods = other.applyUnapplyMethods;
            this.samTypes = other.samTypes;
            this.forComprehensionMethods = other.forComprehensionMethods;
        }
    }

    public boolean isIndexClassFiles() { return indexClassFiles; }
    public void setIndexClassFiles(boolean indexClassFiles) { this.indexClassFiles = indexClassFiles; }

    public boolean isImplicitDefinitions() { return implicitDefinitions; }
    public void setImplicitDefinitions(boolean implicitDefinitions) { this.implicitDefinitions = implicitDefinitions; }

    public boolean isApplyUnapplyMethods() { return applyUnapplyMethods; }
    public void setApplyUnapplyMethods(boolean applyUnapplyMethods) { this.applyUnapplyMethods = applyUnapplyMethods; }

    public boolean isSamTypes() { return samTypes; }
    public void setSamTypes(boolean samTypes) { this.samTypes = samTypes; }

    public boolean isForComprehensionMethods() { return forComprehensionMethods; }
    public void setForComprehensionMethods(boolean forComprehensionMethods) { this.forComprehensionMethods = forComprehensionMethods; }

    @Override
    public ScalaBytecodeIndicesSettings clone() {
        return new ScalaBytecodeIndicesSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaBytecodeIndicesSettings that = (ScalaBytecodeIndicesSettings) o;
        return indexClassFiles == that.indexClassFiles &&
                implicitDefinitions == that.implicitDefinitions &&
                applyUnapplyMethods == that.applyUnapplyMethods &&
                samTypes == that.samTypes &&
                forComprehensionMethods == that.forComprehensionMethods;
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexClassFiles, implicitDefinitions, applyUnapplyMethods, samTypes, forComprehensionMethods);
    }
}
