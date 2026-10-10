package dev.lumina.database.versioning;

import java.util.Objects;

/**
 * Model representing a Type Mapping row in Tools > Database Versioning > Type Mappings in Lumina IDE.
 */
public class DatabaseTypeMappingItem implements Cloneable {

    private String attributeType;
    private String targetType;
    private String typeParameters;

    public DatabaseTypeMappingItem() {
        this("", "", "");
    }

    public DatabaseTypeMappingItem(String attributeType, String targetType, String typeParameters) {
        this.attributeType = attributeType != null ? attributeType : "";
        this.targetType = targetType != null ? targetType : "";
        this.typeParameters = typeParameters != null ? typeParameters : "";
    }

    public String getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(String attributeType) {
        this.attributeType = attributeType != null ? attributeType : "";
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType != null ? targetType : "";
    }

    public String getTypeParameters() {
        return typeParameters;
    }

    public void setTypeParameters(String typeParameters) {
        this.typeParameters = typeParameters != null ? typeParameters : "";
    }

    @Override
    public DatabaseTypeMappingItem clone() {
        try {
            return (DatabaseTypeMappingItem) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseTypeMappingItem that = (DatabaseTypeMappingItem) o;
        return Objects.equals(attributeType, that.attributeType) &&
                Objects.equals(targetType, that.targetType) &&
                Objects.equals(typeParameters, that.typeParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeType, targetType, typeParameters);
    }
}
