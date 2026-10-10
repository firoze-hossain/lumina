package dev.lumina.database.versioning;

import java.util.Objects;

/**
 * Model representing Tools > Database Versioning > Hibernate Envers settings in Lumina IDE.
 */
public class DatabaseHibernateEnversSettings implements Cloneable {

    private boolean useValuesInPropertiesFiles = false;
    private String auditTablePrefix = "";
    private String auditTableSuffix = "";
    private String revisionFieldName = "";
    private String revisionTypeFieldName = "";
    private String defaultSchemaName = "";
    private boolean treatOptimisticLockingUnversioned = false;
    private boolean trackEntityNamesChanged = false;
    private boolean activateModifiedPropertiesFlag = false;
    private String suffixModifiedFlagColumns = "";

    public DatabaseHibernateEnversSettings() {
    }

    public boolean isUseValuesInPropertiesFiles() {
        return useValuesInPropertiesFiles;
    }

    public void setUseValuesInPropertiesFiles(boolean useValuesInPropertiesFiles) {
        this.useValuesInPropertiesFiles = useValuesInPropertiesFiles;
    }

    public String getAuditTablePrefix() {
        return auditTablePrefix;
    }

    public void setAuditTablePrefix(String auditTablePrefix) {
        this.auditTablePrefix = auditTablePrefix != null ? auditTablePrefix : "";
    }

    public String getAuditTableSuffix() {
        return auditTableSuffix;
    }

    public void setAuditTableSuffix(String auditTableSuffix) {
        this.auditTableSuffix = auditTableSuffix != null ? auditTableSuffix : "";
    }

    public String getRevisionFieldName() {
        return revisionFieldName;
    }

    public void setRevisionFieldName(String revisionFieldName) {
        this.revisionFieldName = revisionFieldName != null ? revisionFieldName : "";
    }

    public String getRevisionTypeFieldName() {
        return revisionTypeFieldName;
    }

    public void setRevisionTypeFieldName(String revisionTypeFieldName) {
        this.revisionTypeFieldName = revisionTypeFieldName != null ? revisionTypeFieldName : "";
    }

    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }

    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName != null ? defaultSchemaName : "";
    }

    public boolean isTreatOptimisticLockingUnversioned() {
        return treatOptimisticLockingUnversioned;
    }

    public void setTreatOptimisticLockingUnversioned(boolean treatOptimisticLockingUnversioned) {
        this.treatOptimisticLockingUnversioned = treatOptimisticLockingUnversioned;
    }

    public boolean isTrackEntityNamesChanged() {
        return trackEntityNamesChanged;
    }

    public void setTrackEntityNamesChanged(boolean trackEntityNamesChanged) {
        this.trackEntityNamesChanged = trackEntityNamesChanged;
    }

    public boolean isActivateModifiedPropertiesFlag() {
        return activateModifiedPropertiesFlag;
    }

    public void setActivateModifiedPropertiesFlag(boolean activateModifiedPropertiesFlag) {
        this.activateModifiedPropertiesFlag = activateModifiedPropertiesFlag;
    }

    public String getSuffixModifiedFlagColumns() {
        return suffixModifiedFlagColumns;
    }

    public void setSuffixModifiedFlagColumns(String suffixModifiedFlagColumns) {
        this.suffixModifiedFlagColumns = suffixModifiedFlagColumns != null ? suffixModifiedFlagColumns : "";
    }

    @Override
    public DatabaseHibernateEnversSettings clone() {
        try {
            return (DatabaseHibernateEnversSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseHibernateEnversSettings that = (DatabaseHibernateEnversSettings) o;
        return useValuesInPropertiesFiles == that.useValuesInPropertiesFiles &&
                treatOptimisticLockingUnversioned == that.treatOptimisticLockingUnversioned &&
                trackEntityNamesChanged == that.trackEntityNamesChanged &&
                activateModifiedPropertiesFlag == that.activateModifiedPropertiesFlag &&
                Objects.equals(auditTablePrefix, that.auditTablePrefix) &&
                Objects.equals(auditTableSuffix, that.auditTableSuffix) &&
                Objects.equals(revisionFieldName, that.revisionFieldName) &&
                Objects.equals(revisionTypeFieldName, that.revisionTypeFieldName) &&
                Objects.equals(defaultSchemaName, that.defaultSchemaName) &&
                Objects.equals(suffixModifiedFlagColumns, that.suffixModifiedFlagColumns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useValuesInPropertiesFiles, auditTablePrefix, auditTableSuffix,
                revisionFieldName, revisionTypeFieldName, defaultSchemaName,
                treatOptimisticLockingUnversioned, trackEntityNamesChanged,
                activateModifiedPropertiesFlag, suffixModifiedFlagColumns);
    }
}
