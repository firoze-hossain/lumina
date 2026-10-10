package dev.lumina.database.versioning;

import java.util.*;

/**
 * Settings configuration model for Tools > Database Versioning > Liquibase in Lumina IDE.
 */
public class DatabaseLiquibaseSettings implements Cloneable {

    private String liquibaseVersion = "";
    private String changesetAuthor = System.getProperty("user.name", "user") + " (generated)";
    private String fileType = "XML";
    private boolean addEmptyRollback = false;
    private String primaryDirectory = "db/changelog/#date(\"yyyy\")/#date(\"MM\")";
    private String primaryName = "#date(\"dd\")-#increment(1, 1, \"00\")-changelog";
    private String secondaryDirectory = "db/changelog-secondary/#date(\"yyyy\")/#date(\"MM\")";
    private String secondaryName = "#date(\"dd\")-#increment(1, 1, \"00\")-changelog";

    private Set<String> enabledDbTypes = new HashSet<>();
    private List<LiquibaseChangesetTemplateItem> changesetTemplates = new ArrayList<>();

    public DatabaseLiquibaseSettings() {
        initDefaults();
    }

    private void initDefaults() {
        changesetTemplates.clear();
        String[] defaultNames = {
                "addAutoIncrement",
                "addColumn",
                "addDefaultValue",
                "addForeignKeyConstraint",
                "addLookupTable",
                "addNotNullConstraint",
                "addPrimaryKey",
                "addUniqueConstraint",
                "createIndex",
                "createProcedure",
                "createSequence",
                "createTable",
                "createView",
                "dropAllForeignKeyConstraints",
                "dropColumn",
                "dropDefaultValue",
                "dropForeignKeyConstraint",
                "dropIndex",
                "dropNotNullConstraint",
                "dropPrimaryKey",
                "dropProcedure",
                "dropSequence",
                "dropTable",
                "dropUniqueConstraint",
                "dropView",
                "modifyDataType",
                "renameColumn",
                "renameSequence",
                "renameTable",
                "renameView",
                "setColumnRemarks",
                "setTableRemarks",
                "update"
        };

        for (String name : defaultNames) {
            changesetTemplates.add(new LiquibaseChangesetTemplateItem(name, false, false, false));
        }
    }

    public String getLiquibaseVersion() {
        return liquibaseVersion;
    }

    public void setLiquibaseVersion(String liquibaseVersion) {
        this.liquibaseVersion = liquibaseVersion != null ? liquibaseVersion : "";
    }

    public String getChangesetAuthor() {
        return changesetAuthor;
    }

    public void setChangesetAuthor(String changesetAuthor) {
        this.changesetAuthor = changesetAuthor != null ? changesetAuthor : "";
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType != null ? fileType : "XML";
    }

    public boolean isAddEmptyRollback() {
        return addEmptyRollback;
    }

    public void setAddEmptyRollback(boolean addEmptyRollback) {
        this.addEmptyRollback = addEmptyRollback;
    }

    public String getPrimaryDirectory() {
        return primaryDirectory;
    }

    public void setPrimaryDirectory(String primaryDirectory) {
        this.primaryDirectory = primaryDirectory != null ? primaryDirectory : "";
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public void setPrimaryName(String primaryName) {
        this.primaryName = primaryName != null ? primaryName : "";
    }

    public String getSecondaryDirectory() {
        return secondaryDirectory;
    }

    public void setSecondaryDirectory(String secondaryDirectory) {
        this.secondaryDirectory = secondaryDirectory != null ? secondaryDirectory : "";
    }

    public String getSecondaryName() {
        return secondaryName;
    }

    public void setSecondaryName(String secondaryName) {
        this.secondaryName = secondaryName != null ? secondaryName : "";
    }

    public Set<String> getEnabledDbTypes() {
        return enabledDbTypes;
    }

    public void setEnabledDbTypes(Set<String> enabledDbTypes) {
        this.enabledDbTypes = enabledDbTypes != null ? new HashSet<>(enabledDbTypes) : new HashSet<>();
    }

    public List<LiquibaseChangesetTemplateItem> getChangesetTemplates() {
        return changesetTemplates;
    }

    public void setChangesetTemplates(List<LiquibaseChangesetTemplateItem> changesetTemplates) {
        this.changesetTemplates = changesetTemplates != null ? new ArrayList<>(changesetTemplates) : new ArrayList<>();
    }

    @Override
    public DatabaseLiquibaseSettings clone() {
        try {
            DatabaseLiquibaseSettings copy = (DatabaseLiquibaseSettings) super.clone();
            copy.enabledDbTypes = new HashSet<>(this.enabledDbTypes);
            copy.changesetTemplates = new ArrayList<>();
            for (LiquibaseChangesetTemplateItem item : this.changesetTemplates) {
                copy.changesetTemplates.add(item.clone());
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseLiquibaseSettings that = (DatabaseLiquibaseSettings) o;
        return addEmptyRollback == that.addEmptyRollback &&
                Objects.equals(liquibaseVersion, that.liquibaseVersion) &&
                Objects.equals(changesetAuthor, that.changesetAuthor) &&
                Objects.equals(fileType, that.fileType) &&
                Objects.equals(primaryDirectory, that.primaryDirectory) &&
                Objects.equals(primaryName, that.primaryName) &&
                Objects.equals(secondaryDirectory, that.secondaryDirectory) &&
                Objects.equals(secondaryName, that.secondaryName) &&
                Objects.equals(enabledDbTypes, that.enabledDbTypes) &&
                Objects.equals(changesetTemplates, that.changesetTemplates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(liquibaseVersion, changesetAuthor, fileType, addEmptyRollback,
                primaryDirectory, primaryName, secondaryDirectory, secondaryName,
                enabledDbTypes, changesetTemplates);
    }
}
