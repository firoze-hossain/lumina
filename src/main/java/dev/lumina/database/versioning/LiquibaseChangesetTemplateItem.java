package dev.lumina.database.versioning;

import java.util.Objects;

/**
 * Model representing a Liquibase changeset template configuration row in Lumina IDE.
 */
public class LiquibaseChangesetTemplateItem implements Cloneable {

    private String name;
    private boolean failOnError;
    private boolean runOnChange;
    private boolean createPreconditions;

    public LiquibaseChangesetTemplateItem() {
        this("", false, false, false);
    }

    public LiquibaseChangesetTemplateItem(String name, boolean failOnError, boolean runOnChange, boolean createPreconditions) {
        this.name = name != null ? name : "";
        this.failOnError = failOnError;
        this.runOnChange = runOnChange;
        this.createPreconditions = createPreconditions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public boolean isRunOnChange() {
        return runOnChange;
    }

    public void setRunOnChange(boolean runOnChange) {
        this.runOnChange = runOnChange;
    }

    public boolean isCreatePreconditions() {
        return createPreconditions;
    }

    public void setCreatePreconditions(boolean createPreconditions) {
        this.createPreconditions = createPreconditions;
    }

    @Override
    public LiquibaseChangesetTemplateItem clone() {
        try {
            return (LiquibaseChangesetTemplateItem) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiquibaseChangesetTemplateItem that = (LiquibaseChangesetTemplateItem) o;
        return failOnError == that.failOnError &&
                runOnChange == that.runOnChange &&
                createPreconditions == that.createPreconditions &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, failOnError, runOnChange, createPreconditions);
    }
}
