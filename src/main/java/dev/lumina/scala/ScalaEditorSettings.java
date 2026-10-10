package dev.lumina.scala;

import java.util.Objects;

/**
 * Settings model for Languages & Frameworks > Scala > Editor in Lumina IDE.
 * Controls built-in highlighting, type mismatch hints, implicit resolution hints,
 * copy-paste conversion, and completion options.
 */
public class ScalaEditorSettings {

    // Built-in Highlighting
    private boolean showHintsOnTypeMismatch = true;
    private boolean showHintsIfNoImplicitArgumentsFound = true;
    private boolean showHintsIfAmbiguousImplicitArgumentsFound = true;
    private String exportAliases = "Exports"; // "Exports", "Aliases", "None"

    // Advanced (Collapsible)
    private boolean highlightImplicitConversions = false;
    private boolean highlightArgumentsToByNameParameters = false;
    private boolean includeBlockExpressions = false;
    private boolean includeLiterals = false;
    private boolean customScalaTestKeywordsHighlighting = false;
    private String collectionTypeHighlighting = "None"; // "None", "Standard collections", "All collections"
    private boolean aheadOfTimeCompletion = true;
    private boolean useScalaClassesPriorityOverJavaClasses = true;
    private boolean convertJavaCodeToScalaOnCopyPaste = true;
    private boolean dontShowDialogOnPasteAndAutomaticallyConvert = false;
    private boolean addOverrideKeywordToMethodImplementation = true;

    public ScalaEditorSettings() {
    }

    public ScalaEditorSettings(ScalaEditorSettings other) {
        if (other != null) {
            this.showHintsOnTypeMismatch = other.showHintsOnTypeMismatch;
            this.showHintsIfNoImplicitArgumentsFound = other.showHintsIfNoImplicitArgumentsFound;
            this.showHintsIfAmbiguousImplicitArgumentsFound = other.showHintsIfAmbiguousImplicitArgumentsFound;
            this.exportAliases = other.exportAliases;
            this.highlightImplicitConversions = other.highlightImplicitConversions;
            this.highlightArgumentsToByNameParameters = other.highlightArgumentsToByNameParameters;
            this.includeBlockExpressions = other.includeBlockExpressions;
            this.includeLiterals = other.includeLiterals;
            this.customScalaTestKeywordsHighlighting = other.customScalaTestKeywordsHighlighting;
            this.collectionTypeHighlighting = other.collectionTypeHighlighting;
            this.aheadOfTimeCompletion = other.aheadOfTimeCompletion;
            this.useScalaClassesPriorityOverJavaClasses = other.useScalaClassesPriorityOverJavaClasses;
            this.convertJavaCodeToScalaOnCopyPaste = other.convertJavaCodeToScalaOnCopyPaste;
            this.dontShowDialogOnPasteAndAutomaticallyConvert = other.dontShowDialogOnPasteAndAutomaticallyConvert;
            this.addOverrideKeywordToMethodImplementation = other.addOverrideKeywordToMethodImplementation;
        }
    }

    public ScalaEditorSettings copy() {
        return new ScalaEditorSettings(this);
    }

    public boolean isShowHintsOnTypeMismatch() {
        return showHintsOnTypeMismatch;
    }

    public void setShowHintsOnTypeMismatch(boolean showHintsOnTypeMismatch) {
        this.showHintsOnTypeMismatch = showHintsOnTypeMismatch;
    }

    public boolean isShowHintsIfNoImplicitArgumentsFound() {
        return showHintsIfNoImplicitArgumentsFound;
    }

    public void setShowHintsIfNoImplicitArgumentsFound(boolean showHintsIfNoImplicitArgumentsFound) {
        this.showHintsIfNoImplicitArgumentsFound = showHintsIfNoImplicitArgumentsFound;
    }

    public boolean isShowHintsIfAmbiguousImplicitArgumentsFound() {
        return showHintsIfAmbiguousImplicitArgumentsFound;
    }

    public void setShowHintsIfAmbiguousImplicitArgumentsFound(boolean showHintsIfAmbiguousImplicitArgumentsFound) {
        this.showHintsIfAmbiguousImplicitArgumentsFound = showHintsIfAmbiguousImplicitArgumentsFound;
    }

    public String getExportAliases() {
        return exportAliases;
    }

    public void setExportAliases(String exportAliases) {
        this.exportAliases = exportAliases != null ? exportAliases : "Exports";
    }

    public boolean isHighlightImplicitConversions() {
        return highlightImplicitConversions;
    }

    public void setHighlightImplicitConversions(boolean highlightImplicitConversions) {
        this.highlightImplicitConversions = highlightImplicitConversions;
    }

    public boolean isHighlightArgumentsToByNameParameters() {
        return highlightArgumentsToByNameParameters;
    }

    public void setHighlightArgumentsToByNameParameters(boolean highlightArgumentsToByNameParameters) {
        this.highlightArgumentsToByNameParameters = highlightArgumentsToByNameParameters;
    }

    public boolean isIncludeBlockExpressions() {
        return includeBlockExpressions;
    }

    public void setIncludeBlockExpressions(boolean includeBlockExpressions) {
        this.includeBlockExpressions = includeBlockExpressions;
    }

    public boolean isIncludeLiterals() {
        return includeLiterals;
    }

    public void setIncludeLiterals(boolean includeLiterals) {
        this.includeLiterals = includeLiterals;
    }

    public boolean isCustomScalaTestKeywordsHighlighting() {
        return customScalaTestKeywordsHighlighting;
    }

    public void setCustomScalaTestKeywordsHighlighting(boolean customScalaTestKeywordsHighlighting) {
        this.customScalaTestKeywordsHighlighting = customScalaTestKeywordsHighlighting;
    }

    public String getCollectionTypeHighlighting() {
        return collectionTypeHighlighting;
    }

    public void setCollectionTypeHighlighting(String collectionTypeHighlighting) {
        this.collectionTypeHighlighting = collectionTypeHighlighting != null ? collectionTypeHighlighting : "None";
    }

    public boolean isAheadOfTimeCompletion() {
        return aheadOfTimeCompletion;
    }

    public void setAheadOfTimeCompletion(boolean aheadOfTimeCompletion) {
        this.aheadOfTimeCompletion = aheadOfTimeCompletion;
    }

    public boolean isUseScalaClassesPriorityOverJavaClasses() {
        return useScalaClassesPriorityOverJavaClasses;
    }

    public void setUseScalaClassesPriorityOverJavaClasses(boolean useScalaClassesPriorityOverJavaClasses) {
        this.useScalaClassesPriorityOverJavaClasses = useScalaClassesPriorityOverJavaClasses;
    }

    public boolean isConvertJavaCodeToScalaOnCopyPaste() {
        return convertJavaCodeToScalaOnCopyPaste;
    }

    public void setConvertJavaCodeToScalaOnCopyPaste(boolean convertJavaCodeToScalaOnCopyPaste) {
        this.convertJavaCodeToScalaOnCopyPaste = convertJavaCodeToScalaOnCopyPaste;
    }

    public boolean isDontShowDialogOnPasteAndAutomaticallyConvert() {
        return dontShowDialogOnPasteAndAutomaticallyConvert;
    }

    public void setDontShowDialogOnPasteAndAutomaticallyConvert(boolean dontShowDialogOnPasteAndAutomaticallyConvert) {
        this.dontShowDialogOnPasteAndAutomaticallyConvert = dontShowDialogOnPasteAndAutomaticallyConvert;
    }

    public boolean isAddOverrideKeywordToMethodImplementation() {
        return addOverrideKeywordToMethodImplementation;
    }

    public void setAddOverrideKeywordToMethodImplementation(boolean addOverrideKeywordToMethodImplementation) {
        this.addOverrideKeywordToMethodImplementation = addOverrideKeywordToMethodImplementation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaEditorSettings that = (ScalaEditorSettings) o;
        return showHintsOnTypeMismatch == that.showHintsOnTypeMismatch &&
                showHintsIfNoImplicitArgumentsFound == that.showHintsIfNoImplicitArgumentsFound &&
                showHintsIfAmbiguousImplicitArgumentsFound == that.showHintsIfAmbiguousImplicitArgumentsFound &&
                highlightImplicitConversions == that.highlightImplicitConversions &&
                highlightArgumentsToByNameParameters == that.highlightArgumentsToByNameParameters &&
                includeBlockExpressions == that.includeBlockExpressions &&
                includeLiterals == that.includeLiterals &&
                customScalaTestKeywordsHighlighting == that.customScalaTestKeywordsHighlighting &&
                aheadOfTimeCompletion == that.aheadOfTimeCompletion &&
                useScalaClassesPriorityOverJavaClasses == that.useScalaClassesPriorityOverJavaClasses &&
                convertJavaCodeToScalaOnCopyPaste == that.convertJavaCodeToScalaOnCopyPaste &&
                dontShowDialogOnPasteAndAutomaticallyConvert == that.dontShowDialogOnPasteAndAutomaticallyConvert &&
                addOverrideKeywordToMethodImplementation == that.addOverrideKeywordToMethodImplementation &&
                Objects.equals(exportAliases, that.exportAliases) &&
                Objects.equals(collectionTypeHighlighting, that.collectionTypeHighlighting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showHintsOnTypeMismatch, showHintsIfNoImplicitArgumentsFound,
                showHintsIfAmbiguousImplicitArgumentsFound, exportAliases,
                highlightImplicitConversions, highlightArgumentsToByNameParameters,
                includeBlockExpressions, includeLiterals, customScalaTestKeywordsHighlighting,
                collectionTypeHighlighting, aheadOfTimeCompletion,
                useScalaClassesPriorityOverJavaClasses, convertJavaCodeToScalaOnCopyPaste,
                dontShowDialogOnPasteAndAutomaticallyConvert, addOverrideKeywordToMethodImplementation);
    }
}
