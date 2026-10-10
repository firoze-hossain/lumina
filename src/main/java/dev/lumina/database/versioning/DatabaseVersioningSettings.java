package dev.lumina.database.versioning;

import java.util.Objects;

/**
 * Model representing Database Versioning settings in Lumina IDE.
 */
public class DatabaseVersioningSettings implements Cloneable {

    private String physicalNamingStrategy = "Use persistent unit strategy";
    private String sequenceNamingStrategy = "Auto detect";
    private int maxDbIdentifierLength = 63;
    private boolean createIndexForAssociationFk = false;
    private boolean primaryKeyConstraintNamed = true;
    private String pkConstraintPrefix = "PK";
    private String pkConstraintPattern = "_${TABLE_NAME}_";
    private String pkConstraintSuffix = "";

    public DatabaseVersioningSettings() {
    }

    public String getPhysicalNamingStrategy() {
        return physicalNamingStrategy;
    }

    public void setPhysicalNamingStrategy(String physicalNamingStrategy) {
        this.physicalNamingStrategy = physicalNamingStrategy != null ? physicalNamingStrategy : "Use persistent unit strategy";
    }

    public String getSequenceNamingStrategy() {
        return sequenceNamingStrategy;
    }

    public void setSequenceNamingStrategy(String sequenceNamingStrategy) {
        this.sequenceNamingStrategy = sequenceNamingStrategy != null ? sequenceNamingStrategy : "Auto detect";
    }

    public int getMaxDbIdentifierLength() {
        return maxDbIdentifierLength;
    }

    public void setMaxDbIdentifierLength(int maxDbIdentifierLength) {
        this.maxDbIdentifierLength = maxDbIdentifierLength;
    }

    public boolean isCreateIndexForAssociationFk() {
        return createIndexForAssociationFk;
    }

    public void setCreateIndexForAssociationFk(boolean createIndexForAssociationFk) {
        this.createIndexForAssociationFk = createIndexForAssociationFk;
    }

    public boolean isPrimaryKeyConstraintNamed() {
        return primaryKeyConstraintNamed;
    }

    public void setPrimaryKeyConstraintNamed(boolean primaryKeyConstraintNamed) {
        this.primaryKeyConstraintNamed = primaryKeyConstraintNamed;
    }

    public String getPkConstraintPrefix() {
        return pkConstraintPrefix;
    }

    public void setPkConstraintPrefix(String pkConstraintPrefix) {
        this.pkConstraintPrefix = pkConstraintPrefix != null ? pkConstraintPrefix : "PK";
    }

    public String getPkConstraintPattern() {
        return pkConstraintPattern;
    }

    public void setPkConstraintPattern(String pkConstraintPattern) {
        this.pkConstraintPattern = pkConstraintPattern != null ? pkConstraintPattern : "_${TABLE_NAME}_";
    }

    public String getPkConstraintSuffix() {
        return pkConstraintSuffix;
    }

    public void setPkConstraintSuffix(String pkConstraintSuffix) {
        this.pkConstraintSuffix = pkConstraintSuffix != null ? pkConstraintSuffix : "";
    }

    @Override
    public DatabaseVersioningSettings clone() {
        try {
            return (DatabaseVersioningSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseVersioningSettings that = (DatabaseVersioningSettings) o;
        return maxDbIdentifierLength == that.maxDbIdentifierLength &&
                createIndexForAssociationFk == that.createIndexForAssociationFk &&
                primaryKeyConstraintNamed == that.primaryKeyConstraintNamed &&
                Objects.equals(physicalNamingStrategy, that.physicalNamingStrategy) &&
                Objects.equals(sequenceNamingStrategy, that.sequenceNamingStrategy) &&
                Objects.equals(pkConstraintPrefix, that.pkConstraintPrefix) &&
                Objects.equals(pkConstraintPattern, that.pkConstraintPattern) &&
                Objects.equals(pkConstraintSuffix, that.pkConstraintSuffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(physicalNamingStrategy, sequenceNamingStrategy, maxDbIdentifierLength,
                createIndexForAssociationFk, primaryKeyConstraintNamed, pkConstraintPrefix,
                pkConstraintPattern, pkConstraintSuffix);
    }
}
