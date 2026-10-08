package dev.lumina.debugger;

import java.util.Objects;
import java.util.UUID;

/**
 * Single async stack trace capture/insert rule matching IntelliJ IDEA (Image 2).
 */
public class AsyncStackTraceRule implements Cloneable {

    private String id;
    private boolean enabled = true;
    private String captureClassName = "";
    private String captureMethodName = "";
    private String captureKeyExpression = "";
    private String insertClassName = "";
    private String insertMethodName = "";
    private String insertKeyExpression = "";

    public AsyncStackTraceRule() {
        this.id = UUID.randomUUID().toString();
    }

    public AsyncStackTraceRule(boolean enabled, String captureClassName, String captureMethodName,
                               String captureKeyExpression, String insertClassName,
                               String insertMethodName, String insertKeyExpression) {
        this.id = UUID.randomUUID().toString();
        this.enabled = enabled;
        this.captureClassName = captureClassName != null ? captureClassName : "";
        this.captureMethodName = captureMethodName != null ? captureMethodName : "";
        this.captureKeyExpression = captureKeyExpression != null ? captureKeyExpression : "";
        this.insertClassName = insertClassName != null ? insertClassName : "";
        this.insertMethodName = insertMethodName != null ? insertMethodName : "";
        this.insertKeyExpression = insertKeyExpression != null ? insertKeyExpression : "";
    }

    public AsyncStackTraceRule(AsyncStackTraceRule other) {
        if (other != null) {
            this.id = other.id != null ? other.id : UUID.randomUUID().toString();
            this.enabled = other.enabled;
            this.captureClassName = other.captureClassName;
            this.captureMethodName = other.captureMethodName;
            this.captureKeyExpression = other.captureKeyExpression;
            this.insertClassName = other.insertClassName;
            this.insertMethodName = other.insertMethodName;
            this.insertKeyExpression = other.insertKeyExpression;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCaptureClassName() {
        return captureClassName;
    }

    public void setCaptureClassName(String captureClassName) {
        this.captureClassName = captureClassName != null ? captureClassName : "";
    }

    public String getCaptureMethodName() {
        return captureMethodName;
    }

    public void setCaptureMethodName(String captureMethodName) {
        this.captureMethodName = captureMethodName != null ? captureMethodName : "";
    }

    public String getCaptureKeyExpression() {
        return captureKeyExpression;
    }

    public void setCaptureKeyExpression(String captureKeyExpression) {
        this.captureKeyExpression = captureKeyExpression != null ? captureKeyExpression : "";
    }

    public String getInsertClassName() {
        return insertClassName;
    }

    public void setInsertClassName(String insertClassName) {
        this.insertClassName = insertClassName != null ? insertClassName : "";
    }

    public String getInsertMethodName() {
        return insertMethodName;
    }

    public void setInsertMethodName(String insertMethodName) {
        this.insertMethodName = insertMethodName != null ? insertMethodName : "";
    }

    public String getInsertKeyExpression() {
        return insertKeyExpression;
    }

    public void setInsertKeyExpression(String insertKeyExpression) {
        this.insertKeyExpression = insertKeyExpression != null ? insertKeyExpression : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AsyncStackTraceRule that)) return false;
        return enabled == that.enabled &&
                Objects.equals(captureClassName, that.captureClassName) &&
                Objects.equals(captureMethodName, that.captureMethodName) &&
                Objects.equals(captureKeyExpression, that.captureKeyExpression) &&
                Objects.equals(insertClassName, that.insertClassName) &&
                Objects.equals(insertMethodName, that.insertMethodName) &&
                Objects.equals(insertKeyExpression, that.insertKeyExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, captureClassName, captureMethodName,
                captureKeyExpression, insertClassName, insertMethodName, insertKeyExpression);
    }

    @Override
    public AsyncStackTraceRule clone() {
        return new AsyncStackTraceRule(this);
    }
}
