package dev.lumina.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration model for Validation compiler settings in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791449977870_5ea029a4.png:
 *  - Validate on build
 *  - Validators:
 *    - FreeMarker
 *    - Hibernate
 *    - JPA
 *    - Jasper
 *    - Spring Model
 *    - Web.xml
 *  - Exclude From Validation: table with Path and Recursiv...
 */
public class ValidationSettings implements Cloneable {

    private boolean validateOnBuild = false;

    // Validators
    private boolean freeMarker = true;
    private boolean hibernate = true;
    private boolean jpa = true;
    private boolean jasper = true;
    private boolean springModel = true;
    private boolean webXml = true;

    // Excludes
    private List<CompilerExcludeEntry> excludes = new ArrayList<>();

    public ValidationSettings() {
    }

    public ValidationSettings(ValidationSettings other) {
        if (other != null) {
            this.validateOnBuild = other.validateOnBuild;
            this.freeMarker = other.freeMarker;
            this.hibernate = other.hibernate;
            this.jpa = other.jpa;
            this.jasper = other.jasper;
            this.springModel = other.springModel;
            this.webXml = other.webXml;
            this.excludes = new ArrayList<>();
            for (CompilerExcludeEntry e : other.excludes) {
                this.excludes.add(e.clone());
            }
        }
    }

    public boolean isValidateOnBuild() { return validateOnBuild; }
    public void setValidateOnBuild(boolean validateOnBuild) { this.validateOnBuild = validateOnBuild; }

    public boolean isFreeMarker() { return freeMarker; }
    public void setFreeMarker(boolean freeMarker) { this.freeMarker = freeMarker; }

    public boolean isHibernate() { return hibernate; }
    public void setHibernate(boolean hibernate) { this.hibernate = hibernate; }

    public boolean isJpa() { return jpa; }
    public void setJpa(boolean jpa) { this.jpa = jpa; }

    public boolean isJasper() { return jasper; }
    public void setJasper(boolean jasper) { this.jasper = jasper; }

    public boolean isSpringModel() { return springModel; }
    public void setSpringModel(boolean springModel) { this.springModel = springModel; }

    public boolean isWebXml() { return webXml; }
    public void setWebXml(boolean webXml) { this.webXml = webXml; }

    public List<CompilerExcludeEntry> getExcludes() {
        return excludes != null ? excludes : new ArrayList<>();
    }

    public void setExcludes(List<CompilerExcludeEntry> excludes) {
        this.excludes = excludes != null ? new ArrayList<>(excludes) : new ArrayList<>();
    }

    @Override
    public ValidationSettings clone() {
        return new ValidationSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationSettings that = (ValidationSettings) o;
        return validateOnBuild == that.validateOnBuild &&
                freeMarker == that.freeMarker &&
                hibernate == that.hibernate &&
                jpa == that.jpa &&
                jasper == that.jasper &&
                springModel == that.springModel &&
                webXml == that.webXml &&
                Objects.equals(excludes, that.excludes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validateOnBuild, freeMarker, hibernate, jpa, jasper, springModel, webXml, excludes);
    }
}
