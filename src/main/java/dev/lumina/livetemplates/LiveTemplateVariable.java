package dev.lumina.livetemplates;

import java.util.Objects;

/**
 * Represents a variable inside a Live Template (e.g. $INDEX$, $VAR$, $END$, $SELECTION$).
 */
public class LiveTemplateVariable {

    private String name = "";
    private String expression = "";
    private String defaultValue = "";
    private boolean skipIfDefined = false;

    public LiveTemplateVariable() {
    }

    public LiveTemplateVariable(String name, String expression, String defaultValue, boolean skipIfDefined) {
        this.name = name != null ? name : "";
        this.expression = expression != null ? expression : "";
        this.defaultValue = defaultValue != null ? defaultValue : "";
        this.skipIfDefined = skipIfDefined;
    }

    public LiveTemplateVariable copy() {
        return new LiveTemplateVariable(name, expression, defaultValue, skipIfDefined);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression != null ? expression : "";
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue != null ? defaultValue : "";
    }

    public boolean isSkipIfDefined() {
        return skipIfDefined;
    }

    public void setSkipIfDefined(boolean skipIfDefined) {
        this.skipIfDefined = skipIfDefined;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiveTemplateVariable that = (LiveTemplateVariable) o;
        return skipIfDefined == that.skipIfDefined &&
                Objects.equals(name, that.name) &&
                Objects.equals(expression, that.expression) &&
                Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, expression, defaultValue, skipIfDefined);
    }
}
