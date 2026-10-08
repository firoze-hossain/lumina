package dev.lumina.debugger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings for Build, Execution, Deployment > Debugger > Data Views > Java (Image 4).
 */
public class JavaDataViewsSettings implements Cloneable {

    public enum ToStringMode {
        ALL_OVERRIDING,
        CLASSES_FROM_LIST
    }

    private boolean autoscrollToNewLocalVariables = true;
    private boolean predictConditionValues = true;
    private boolean grayOutPredictedUnreachableCode = true;

    // Show section
    private boolean showDeclaredType = false;
    private boolean showSyntheticFields = true;
    private boolean showValFieldsAsLocalVariables = true;
    private boolean showFullyQualifiedNames = false;
    private boolean showObjectId = true;
    private boolean showStaticFields = false;
    private boolean showStaticFinalFields = false;

    // Individual toggles
    private boolean showTypeForStrings = false;
    private boolean showHexForPrimitives = false;
    private boolean hideNullElements = true;
    private boolean autoPopulateThrowableStackTrace = true;
    private boolean enableAlternativeViewForCollections = true;

    // toString() object view
    private boolean enableToStringObjectView = true;
    private ToStringMode toStringMode = ToStringMode.ALL_OVERRIDING;
    private List<String> toStringClassPatterns = new ArrayList<>();

    public JavaDataViewsSettings() {
    }

    public JavaDataViewsSettings(JavaDataViewsSettings other) {
        if (other != null) {
            this.autoscrollToNewLocalVariables = other.autoscrollToNewLocalVariables;
            this.predictConditionValues = other.predictConditionValues;
            this.grayOutPredictedUnreachableCode = other.grayOutPredictedUnreachableCode;
            this.showDeclaredType = other.showDeclaredType;
            this.showSyntheticFields = other.showSyntheticFields;
            this.showValFieldsAsLocalVariables = other.showValFieldsAsLocalVariables;
            this.showFullyQualifiedNames = other.showFullyQualifiedNames;
            this.showObjectId = other.showObjectId;
            this.showStaticFields = other.showStaticFields;
            this.showStaticFinalFields = other.showStaticFinalFields;
            this.showTypeForStrings = other.showTypeForStrings;
            this.showHexForPrimitives = other.showHexForPrimitives;
            this.hideNullElements = other.hideNullElements;
            this.autoPopulateThrowableStackTrace = other.autoPopulateThrowableStackTrace;
            this.enableAlternativeViewForCollections = other.enableAlternativeViewForCollections;
            this.enableToStringObjectView = other.enableToStringObjectView;
            this.toStringMode = other.toStringMode != null ? other.toStringMode : ToStringMode.ALL_OVERRIDING;
            this.toStringClassPatterns = new ArrayList<>(other.toStringClassPatterns);
        }
    }

    public boolean isAutoscrollToNewLocalVariables() {
        return autoscrollToNewLocalVariables;
    }

    public void setAutoscrollToNewLocalVariables(boolean autoscrollToNewLocalVariables) {
        this.autoscrollToNewLocalVariables = autoscrollToNewLocalVariables;
    }

    public boolean isPredictConditionValues() {
        return predictConditionValues;
    }

    public void setPredictConditionValues(boolean predictConditionValues) {
        this.predictConditionValues = predictConditionValues;
    }

    public boolean isGrayOutPredictedUnreachableCode() {
        return grayOutPredictedUnreachableCode;
    }

    public void setGrayOutPredictedUnreachableCode(boolean grayOutPredictedUnreachableCode) {
        this.grayOutPredictedUnreachableCode = grayOutPredictedUnreachableCode;
    }

    public boolean isShowDeclaredType() {
        return showDeclaredType;
    }

    public void setShowDeclaredType(boolean showDeclaredType) {
        this.showDeclaredType = showDeclaredType;
    }

    public boolean isShowSyntheticFields() {
        return showSyntheticFields;
    }

    public void setShowSyntheticFields(boolean showSyntheticFields) {
        this.showSyntheticFields = showSyntheticFields;
    }

    public boolean isShowValFieldsAsLocalVariables() {
        return showValFieldsAsLocalVariables;
    }

    public void setShowValFieldsAsLocalVariables(boolean showValFieldsAsLocalVariables) {
        this.showValFieldsAsLocalVariables = showValFieldsAsLocalVariables;
    }

    public boolean isShowFullyQualifiedNames() {
        return showFullyQualifiedNames;
    }

    public void setShowFullyQualifiedNames(boolean showFullyQualifiedNames) {
        this.showFullyQualifiedNames = showFullyQualifiedNames;
    }

    public boolean isShowObjectId() {
        return showObjectId;
    }

    public void setShowObjectId(boolean showObjectId) {
        this.showObjectId = showObjectId;
    }

    public boolean isShowStaticFields() {
        return showStaticFields;
    }

    public void setShowStaticFields(boolean showStaticFields) {
        this.showStaticFields = showStaticFields;
    }

    public boolean isShowStaticFinalFields() {
        return showStaticFinalFields;
    }

    public void setShowStaticFinalFields(boolean showStaticFinalFields) {
        this.showStaticFinalFields = showStaticFinalFields;
    }

    public boolean isShowTypeForStrings() {
        return showTypeForStrings;
    }

    public void setShowTypeForStrings(boolean showTypeForStrings) {
        this.showTypeForStrings = showTypeForStrings;
    }

    public boolean isShowHexForPrimitives() {
        return showHexForPrimitives;
    }

    public void setShowHexForPrimitives(boolean showHexForPrimitives) {
        this.showHexForPrimitives = showHexForPrimitives;
    }

    public boolean isHideNullElements() {
        return hideNullElements;
    }

    public void setHideNullElements(boolean hideNullElements) {
        this.hideNullElements = hideNullElements;
    }

    public boolean isAutoPopulateThrowableStackTrace() {
        return autoPopulateThrowableStackTrace;
    }

    public void setAutoPopulateThrowableStackTrace(boolean autoPopulateThrowableStackTrace) {
        this.autoPopulateThrowableStackTrace = autoPopulateThrowableStackTrace;
    }

    public boolean isEnableAlternativeViewForCollections() {
        return enableAlternativeViewForCollections;
    }

    public void setEnableAlternativeViewForCollections(boolean enableAlternativeViewForCollections) {
        this.enableAlternativeViewForCollections = enableAlternativeViewForCollections;
    }

    public boolean isEnableToStringObjectView() {
        return enableToStringObjectView;
    }

    public void setEnableToStringObjectView(boolean enableToStringObjectView) {
        this.enableToStringObjectView = enableToStringObjectView;
    }

    public ToStringMode getToStringMode() {
        return toStringMode;
    }

    public void setToStringMode(ToStringMode toStringMode) {
        this.toStringMode = toStringMode != null ? toStringMode : ToStringMode.ALL_OVERRIDING;
    }

    public List<String> getToStringClassPatterns() {
        return toStringClassPatterns;
    }

    public void setToStringClassPatterns(List<String> toStringClassPatterns) {
        this.toStringClassPatterns = toStringClassPatterns != null ? new ArrayList<>(toStringClassPatterns) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaDataViewsSettings that)) return false;
        return autoscrollToNewLocalVariables == that.autoscrollToNewLocalVariables &&
                predictConditionValues == that.predictConditionValues &&
                grayOutPredictedUnreachableCode == that.grayOutPredictedUnreachableCode &&
                showDeclaredType == that.showDeclaredType &&
                showSyntheticFields == that.showSyntheticFields &&
                showValFieldsAsLocalVariables == that.showValFieldsAsLocalVariables &&
                showFullyQualifiedNames == that.showFullyQualifiedNames &&
                showObjectId == that.showObjectId &&
                showStaticFields == that.showStaticFields &&
                showStaticFinalFields == that.showStaticFinalFields &&
                showTypeForStrings == that.showTypeForStrings &&
                showHexForPrimitives == that.showHexForPrimitives &&
                hideNullElements == that.hideNullElements &&
                autoPopulateThrowableStackTrace == that.autoPopulateThrowableStackTrace &&
                enableAlternativeViewForCollections == that.enableAlternativeViewForCollections &&
                enableToStringObjectView == that.enableToStringObjectView &&
                toStringMode == that.toStringMode &&
                Objects.equals(toStringClassPatterns, that.toStringClassPatterns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoscrollToNewLocalVariables, predictConditionValues,
                grayOutPredictedUnreachableCode, showDeclaredType, showSyntheticFields,
                showValFieldsAsLocalVariables, showFullyQualifiedNames, showObjectId,
                showStaticFields, showStaticFinalFields, showTypeForStrings,
                showHexForPrimitives, hideNullElements, autoPopulateThrowableStackTrace,
                enableAlternativeViewForCollections, enableToStringObjectView,
                toStringMode, toStringClassPatterns);
    }

    @Override
    public JavaDataViewsSettings clone() {
        return new JavaDataViewsSettings(this);
    }
}
