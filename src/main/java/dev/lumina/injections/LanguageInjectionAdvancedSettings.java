package dev.lumina.injections;

import java.util.Objects;

/**
 * Settings configuration for Advanced Language Injection options.
 * Matches reference IDE Editor > Language Injections > Advanced settings:
 * - Annotation classes (Language, Pattern, Substitution)
 * - Runtime pattern validation (No runtime, Assertions, IllegalArgumentException)
 * - Performance (Do not analyze, Analyze references, Look for assignments, Dataflow)
 * - Additional options (Convert undefined operands, Add @Language annotation/comment)
 */
public class LanguageInjectionAdvancedSettings {

    public enum RuntimePatternValidation {
        NO_RUNTIME("No runtime instrumentation"),
        ASSERTIONS("Instrument with assertions"),
        ILLEGAL_ARGUMENT_EXCEPTION("Instrument with IllegalArgumentException");

        private final String displayName;

        RuntimePatternValidation(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum PerformanceMode {
        DO_NOT_ANALYZE("Do not analyze anything (fast)"),
        ANALYZE_REFERENCES("Analyze references"),
        LOOK_FOR_ASSIGNMENTS("Look for variable assignments"),
        DATAFLOW_ANALYSIS("Use dataflow analysis (slow)");

        private final String displayName;

        PerformanceMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static final String DEFAULT_LANGUAGE_ANNOTATION = "org.intellij.lang.annotations.Language";
    public static final String DEFAULT_PATTERN_ANNOTATION = "org.intellij.lang.annotations.Pattern";
    public static final String DEFAULT_SUBSTITUTION_ANNOTATION = "org.intellij.lang.annotations.Subst";

    private static final LanguageInjectionAdvancedSettings INSTANCE = new LanguageInjectionAdvancedSettings();

    public static LanguageInjectionAdvancedSettings getInstance() {
        return INSTANCE;
    }

    // Committed settings
    private String committedLanguageAnnotation = DEFAULT_LANGUAGE_ANNOTATION;
    private String committedPatternAnnotation = DEFAULT_PATTERN_ANNOTATION;
    private String committedSubstitutionAnnotation = DEFAULT_SUBSTITUTION_ANNOTATION;
    private RuntimePatternValidation committedValidation = RuntimePatternValidation.ASSERTIONS;
    private PerformanceMode committedPerformance = PerformanceMode.ANALYZE_REFERENCES;
    private boolean committedConvertUndefinedOperands = false;
    private boolean committedAddLanguageAnnotationOrComment = false;

    // Working settings (currently modified in UI)
    private String workingLanguageAnnotation = DEFAULT_LANGUAGE_ANNOTATION;
    private String workingPatternAnnotation = DEFAULT_PATTERN_ANNOTATION;
    private String workingSubstitutionAnnotation = DEFAULT_SUBSTITUTION_ANNOTATION;
    private RuntimePatternValidation workingValidation = RuntimePatternValidation.ASSERTIONS;
    private PerformanceMode workingPerformance = PerformanceMode.ANALYZE_REFERENCES;
    private boolean workingConvertUndefinedOperands = false;
    private boolean workingAddLanguageAnnotationOrComment = false;

    private Runnable onModifiedListener;

    private LanguageInjectionAdvancedSettings() {
    }

    public synchronized boolean isModified() {
        return !Objects.equals(committedLanguageAnnotation, workingLanguageAnnotation)
                || !Objects.equals(committedPatternAnnotation, workingPatternAnnotation)
                || !Objects.equals(committedSubstitutionAnnotation, workingSubstitutionAnnotation)
                || committedValidation != workingValidation
                || committedPerformance != workingPerformance
                || committedConvertUndefinedOperands != workingConvertUndefinedOperands
                || committedAddLanguageAnnotationOrComment != workingAddLanguageAnnotationOrComment;
    }

    public synchronized void apply() {
        committedLanguageAnnotation = workingLanguageAnnotation;
        committedPatternAnnotation = workingPatternAnnotation;
        committedSubstitutionAnnotation = workingSubstitutionAnnotation;
        committedValidation = workingValidation;
        committedPerformance = workingPerformance;
        committedConvertUndefinedOperands = workingConvertUndefinedOperands;
        committedAddLanguageAnnotationOrComment = workingAddLanguageAnnotationOrComment;
        fireModified();
    }

    public synchronized void reset() {
        workingLanguageAnnotation = committedLanguageAnnotation;
        workingPatternAnnotation = committedPatternAnnotation;
        workingSubstitutionAnnotation = committedSubstitutionAnnotation;
        workingValidation = committedValidation;
        workingPerformance = committedPerformance;
        workingConvertUndefinedOperands = committedConvertUndefinedOperands;
        workingAddLanguageAnnotationOrComment = committedAddLanguageAnnotationOrComment;
        fireModified();
    }

    public synchronized void resetToDefaults() {
        workingLanguageAnnotation = DEFAULT_LANGUAGE_ANNOTATION;
        workingPatternAnnotation = DEFAULT_PATTERN_ANNOTATION;
        workingSubstitutionAnnotation = DEFAULT_SUBSTITUTION_ANNOTATION;
        workingValidation = RuntimePatternValidation.ASSERTIONS;
        workingPerformance = PerformanceMode.ANALYZE_REFERENCES;
        workingConvertUndefinedOperands = false;
        workingAddLanguageAnnotationOrComment = false;
        apply();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters and Setters for Working state

    public synchronized String getLanguageAnnotation() {
        return workingLanguageAnnotation;
    }

    public synchronized void setLanguageAnnotation(String val) {
        this.workingLanguageAnnotation = val != null ? val.trim() : "";
        fireModified();
    }

    public synchronized String getPatternAnnotation() {
        return workingPatternAnnotation;
    }

    public synchronized void setPatternAnnotation(String val) {
        this.workingPatternAnnotation = val != null ? val.trim() : "";
        fireModified();
    }

    public synchronized String getSubstitutionAnnotation() {
        return workingSubstitutionAnnotation;
    }

    public synchronized void setSubstitutionAnnotation(String val) {
        this.workingSubstitutionAnnotation = val != null ? val.trim() : "";
        fireModified();
    }

    public synchronized RuntimePatternValidation getRuntimePatternValidation() {
        return workingValidation;
    }

    public synchronized void setRuntimePatternValidation(RuntimePatternValidation val) {
        this.workingValidation = val != null ? val : RuntimePatternValidation.ASSERTIONS;
        fireModified();
    }

    public synchronized PerformanceMode getPerformanceMode() {
        return workingPerformance;
    }

    public synchronized void setPerformanceMode(PerformanceMode val) {
        this.workingPerformance = val != null ? val : PerformanceMode.ANALYZE_REFERENCES;
        fireModified();
    }

    public synchronized boolean isConvertUndefinedOperandsToText() {
        return workingConvertUndefinedOperands;
    }

    public synchronized void setConvertUndefinedOperandsToText(boolean val) {
        this.workingConvertUndefinedOperands = val;
        fireModified();
    }

    public synchronized boolean isAddLanguageAnnotationOrComment() {
        return workingAddLanguageAnnotationOrComment;
    }

    public synchronized void setAddLanguageAnnotationOrComment(boolean val) {
        this.workingAddLanguageAnnotationOrComment = val;
        fireModified();
    }
}
