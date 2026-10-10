package dev.lumina.database.versioning;

import java.util.Objects;

/**
 * Model representing a Diff Change rule in Database Versioning in Lumina IDE.
 */
public class DiffChangeRule implements Cloneable {

    private String category;
    private String action;
    private String location = "Primary";
    private String dangerLevel = "SAFE";
    private String context = "";
    private String labels = "";
    private String color = "#6AAB73";

    public DiffChangeRule() {
        this("", "", "Primary", "SAFE", "#6AAB73");
    }

    public DiffChangeRule(String category, String action, String location, String dangerLevel, String color) {
        this.category = category != null ? category : "";
        this.action = action != null ? action : "";
        this.location = location != null ? location : "Primary";
        this.dangerLevel = dangerLevel != null ? dangerLevel : "SAFE";
        this.color = color != null ? color : "#6AAB73";
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null ? category : "";
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action != null ? action : "";
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location != null ? location : "Primary";
    }

    public String getDangerLevel() {
        return dangerLevel;
    }

    public void setDangerLevel(String dangerLevel) {
        this.dangerLevel = dangerLevel != null ? dangerLevel : "SAFE";
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context != null ? context : "";
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels != null ? labels : "";
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color != null ? color : "#6AAB73";
    }

    @Override
    public DiffChangeRule clone() {
        try {
            return (DiffChangeRule) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiffChangeRule that = (DiffChangeRule) o;
        return Objects.equals(category, that.category) &&
                Objects.equals(action, that.action) &&
                Objects.equals(location, that.location) &&
                Objects.equals(dangerLevel, that.dangerLevel) &&
                Objects.equals(context, that.context) &&
                Objects.equals(labels, that.labels) &&
                Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, action, location, dangerLevel, context, labels, color);
    }
}
