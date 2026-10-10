package dev.lumina.scala;

import java.util.Objects;

/**
 * Settings model for Languages & Frameworks > Scala > X-Ray Mode in Lumina IDE.
 * Controls key activation, inline hints display, type hints details, and status widget.
 */
public class ScalaXRaySettings {

    // Activate on
    private boolean doublePressAndHoldCtrl = true;
    private boolean pressAndHoldCtrl = false;

    // Show
    private boolean parameterNameHints = true;
    private boolean forAllParameters = false;
    private boolean byNameArgumentHints = true;
    private boolean applyMethodHints = true;
    private boolean typeHints = true;
    private boolean memberVariables = true;
    private boolean localVariables = true;
    private boolean methodResults = true;
    private boolean lambdaParameters = true;
    private boolean lambdaPlaceholders = true;
    private boolean variablePatterns = true;
    private boolean methodChainHints = true;
    private boolean typeArguments = false;
    private boolean implicitHints = true;
    private boolean indentGuides = true;
    private boolean methodSeparators = false;

    // Widget
    private String widgetDisplay = "Always"; // "Always", "When active", "Never"

    public ScalaXRaySettings() {
    }

    public ScalaXRaySettings(ScalaXRaySettings other) {
        if (other != null) {
            this.doublePressAndHoldCtrl = other.doublePressAndHoldCtrl;
            this.pressAndHoldCtrl = other.pressAndHoldCtrl;
            this.parameterNameHints = other.parameterNameHints;
            this.forAllParameters = other.forAllParameters;
            this.byNameArgumentHints = other.byNameArgumentHints;
            this.applyMethodHints = other.applyMethodHints;
            this.typeHints = other.typeHints;
            this.memberVariables = other.memberVariables;
            this.localVariables = other.localVariables;
            this.methodResults = other.methodResults;
            this.lambdaParameters = other.lambdaParameters;
            this.lambdaPlaceholders = other.lambdaPlaceholders;
            this.variablePatterns = other.variablePatterns;
            this.methodChainHints = other.methodChainHints;
            this.typeArguments = other.typeArguments;
            this.implicitHints = other.implicitHints;
            this.indentGuides = other.indentGuides;
            this.methodSeparators = other.methodSeparators;
            this.widgetDisplay = other.widgetDisplay;
        }
    }

    public ScalaXRaySettings copy() {
        return new ScalaXRaySettings(this);
    }

    public boolean isDoublePressAndHoldCtrl() {
        return doublePressAndHoldCtrl;
    }

    public void setDoublePressAndHoldCtrl(boolean doublePressAndHoldCtrl) {
        this.doublePressAndHoldCtrl = doublePressAndHoldCtrl;
    }

    public boolean isPressAndHoldCtrl() {
        return pressAndHoldCtrl;
    }

    public void setPressAndHoldCtrl(boolean pressAndHoldCtrl) {
        this.pressAndHoldCtrl = pressAndHoldCtrl;
    }

    public boolean isParameterNameHints() {
        return parameterNameHints;
    }

    public void setParameterNameHints(boolean parameterNameHints) {
        this.parameterNameHints = parameterNameHints;
    }

    public boolean isForAllParameters() {
        return forAllParameters;
    }

    public void setForAllParameters(boolean forAllParameters) {
        this.forAllParameters = forAllParameters;
    }

    public boolean isByNameArgumentHints() {
        return byNameArgumentHints;
    }

    public void setByNameArgumentHints(boolean byNameArgumentHints) {
        this.byNameArgumentHints = byNameArgumentHints;
    }

    public boolean isApplyMethodHints() {
        return applyMethodHints;
    }

    public void setApplyMethodHints(boolean applyMethodHints) {
        this.applyMethodHints = applyMethodHints;
    }

    public boolean isTypeHints() {
        return typeHints;
    }

    public void setTypeHints(boolean typeHints) {
        this.typeHints = typeHints;
    }

    public boolean isMemberVariables() {
        return memberVariables;
    }

    public void setMemberVariables(boolean memberVariables) {
        this.memberVariables = memberVariables;
    }

    public boolean isLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(boolean localVariables) {
        this.localVariables = localVariables;
    }

    public boolean isMethodResults() {
        return methodResults;
    }

    public void setMethodResults(boolean methodResults) {
        this.methodResults = methodResults;
    }

    public boolean isLambdaParameters() {
        return lambdaParameters;
    }

    public void setLambdaParameters(boolean lambdaParameters) {
        this.lambdaParameters = lambdaParameters;
    }

    public boolean isLambdaPlaceholders() {
        return lambdaPlaceholders;
    }

    public void setLambdaPlaceholders(boolean lambdaPlaceholders) {
        this.lambdaPlaceholders = lambdaPlaceholders;
    }

    public boolean isVariablePatterns() {
        return variablePatterns;
    }

    public void setVariablePatterns(boolean variablePatterns) {
        this.variablePatterns = variablePatterns;
    }

    public boolean isMethodChainHints() {
        return methodChainHints;
    }

    public void setMethodChainHints(boolean methodChainHints) {
        this.methodChainHints = methodChainHints;
    }

    public boolean isTypeArguments() {
        return typeArguments;
    }

    public void setTypeArguments(boolean typeArguments) {
        this.typeArguments = typeArguments;
    }

    public boolean isImplicitHints() {
        return implicitHints;
    }

    public void setImplicitHints(boolean implicitHints) {
        this.implicitHints = implicitHints;
    }

    public boolean isIndentGuides() {
        return indentGuides;
    }

    public void setIndentGuides(boolean indentGuides) {
        this.indentGuides = indentGuides;
    }

    public boolean isMethodSeparators() {
        return methodSeparators;
    }

    public void setMethodSeparators(boolean methodSeparators) {
        this.methodSeparators = methodSeparators;
    }

    public String getWidgetDisplay() {
        return widgetDisplay;
    }

    public void setWidgetDisplay(String widgetDisplay) {
        this.widgetDisplay = widgetDisplay != null ? widgetDisplay : "Always";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaXRaySettings that = (ScalaXRaySettings) o;
        return doublePressAndHoldCtrl == that.doublePressAndHoldCtrl &&
                pressAndHoldCtrl == that.pressAndHoldCtrl &&
                parameterNameHints == that.parameterNameHints &&
                forAllParameters == that.forAllParameters &&
                byNameArgumentHints == that.byNameArgumentHints &&
                applyMethodHints == that.applyMethodHints &&
                typeHints == that.typeHints &&
                memberVariables == that.memberVariables &&
                localVariables == that.localVariables &&
                methodResults == that.methodResults &&
                lambdaParameters == that.lambdaParameters &&
                lambdaPlaceholders == that.lambdaPlaceholders &&
                variablePatterns == that.variablePatterns &&
                methodChainHints == that.methodChainHints &&
                typeArguments == that.typeArguments &&
                implicitHints == that.implicitHints &&
                indentGuides == that.indentGuides &&
                methodSeparators == that.methodSeparators &&
                Objects.equals(widgetDisplay, that.widgetDisplay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(doublePressAndHoldCtrl, pressAndHoldCtrl, parameterNameHints,
                forAllParameters, byNameArgumentHints, applyMethodHints, typeHints,
                memberVariables, localVariables, methodResults, lambdaParameters,
                lambdaPlaceholders, variablePatterns, methodChainHints, typeArguments,
                implicitHints, indentGuides, methodSeparators, widgetDisplay);
    }
}
