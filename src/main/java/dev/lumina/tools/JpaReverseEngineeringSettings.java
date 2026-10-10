package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > JPA Reverse Engineering settings matching Image 2.
 */
public class JpaReverseEngineeringSettings implements Cloneable {

    private boolean useFetchTypeLazy = true;
    private boolean useValidationAnnotations = true;
    private boolean convertTableNameToSingular = true;
    private boolean replaceOrmReferencesWithBasicTypes = false;

    // Table & Column Comments: "@Comment annotation", "Java Doc", "Ignore"
    private String tableAndColumnComments = "Ignore";

    // Naming Rules: "Configs", "Algorithm"
    private String namingRulesMode = "Configs";
    private String prefixesToSkipInTableName = "";
    private String prefixesToSkipInColumnName = "";
    private String suffixesToSkipInTableName = "";
    private String suffixesToSkipInColumnName = "";
    private String reservedKeywordFieldSuffix = "Field";

    // Mapping Types
    private String selectedDatabaseEngine = "mysql";
    private List<JpaTypeMappingEntry> typeMappings = new ArrayList<>();

    public JpaReverseEngineeringSettings() {
    }

    public boolean isUseFetchTypeLazy() {
        return useFetchTypeLazy;
    }

    public void setUseFetchTypeLazy(boolean useFetchTypeLazy) {
        this.useFetchTypeLazy = useFetchTypeLazy;
    }

    public boolean isUseValidationAnnotations() {
        return useValidationAnnotations;
    }

    public void setUseValidationAnnotations(boolean useValidationAnnotations) {
        this.useValidationAnnotations = useValidationAnnotations;
    }

    public boolean isConvertTableNameToSingular() {
        return convertTableNameToSingular;
    }

    public void setConvertTableNameToSingular(boolean convertTableNameToSingular) {
        this.convertTableNameToSingular = convertTableNameToSingular;
    }

    public boolean isReplaceOrmReferencesWithBasicTypes() {
        return replaceOrmReferencesWithBasicTypes;
    }

    public void setReplaceOrmReferencesWithBasicTypes(boolean replaceOrmReferencesWithBasicTypes) {
        this.replaceOrmReferencesWithBasicTypes = replaceOrmReferencesWithBasicTypes;
    }

    public String getTableAndColumnComments() {
        return tableAndColumnComments;
    }

    public void setTableAndColumnComments(String tableAndColumnComments) {
        this.tableAndColumnComments = tableAndColumnComments != null ? tableAndColumnComments : "Ignore";
    }

    public String getNamingRulesMode() {
        return namingRulesMode;
    }

    public void setNamingRulesMode(String namingRulesMode) {
        this.namingRulesMode = namingRulesMode != null ? namingRulesMode : "Configs";
    }

    public String getPrefixesToSkipInTableName() {
        return prefixesToSkipInTableName;
    }

    public void setPrefixesToSkipInTableName(String prefixesToSkipInTableName) {
        this.prefixesToSkipInTableName = prefixesToSkipInTableName != null ? prefixesToSkipInTableName : "";
    }

    public String getPrefixesToSkipInColumnName() {
        return prefixesToSkipInColumnName;
    }

    public void setPrefixesToSkipInColumnName(String prefixesToSkipInColumnName) {
        this.prefixesToSkipInColumnName = prefixesToSkipInColumnName != null ? prefixesToSkipInColumnName : "";
    }

    public String getSuffixesToSkipInTableName() {
        return suffixesToSkipInTableName;
    }

    public void setSuffixesToSkipInTableName(String suffixesToSkipInTableName) {
        this.suffixesToSkipInTableName = suffixesToSkipInTableName != null ? suffixesToSkipInTableName : "";
    }

    public String getSuffixesToSkipInColumnName() {
        return suffixesToSkipInColumnName;
    }

    public void setSuffixesToSkipInColumnName(String suffixesToSkipInColumnName) {
        this.suffixesToSkipInColumnName = suffixesToSkipInColumnName != null ? suffixesToSkipInColumnName : "";
    }

    public String getReservedKeywordFieldSuffix() {
        return reservedKeywordFieldSuffix;
    }

    public void setReservedKeywordFieldSuffix(String reservedKeywordFieldSuffix) {
        this.reservedKeywordFieldSuffix = reservedKeywordFieldSuffix != null ? reservedKeywordFieldSuffix : "Field";
    }

    public String getSelectedDatabaseEngine() {
        return selectedDatabaseEngine;
    }

    public void setSelectedDatabaseEngine(String selectedDatabaseEngine) {
        this.selectedDatabaseEngine = selectedDatabaseEngine != null ? selectedDatabaseEngine : "mysql";
    }

    public List<JpaTypeMappingEntry> getTypeMappings() {
        return typeMappings;
    }

    public void setTypeMappings(List<JpaTypeMappingEntry> typeMappings) {
        this.typeMappings = new ArrayList<>();
        if (typeMappings != null) {
            for (JpaTypeMappingEntry e : typeMappings) {
                this.typeMappings.add(e != null ? e.copy() : new JpaTypeMappingEntry());
            }
        }
    }

    public JpaReverseEngineeringSettings copy() {
        return clone();
    }

    @Override
    public JpaReverseEngineeringSettings clone() {
        JpaReverseEngineeringSettings c = new JpaReverseEngineeringSettings();
        c.useFetchTypeLazy = this.useFetchTypeLazy;
        c.useValidationAnnotations = this.useValidationAnnotations;
        c.convertTableNameToSingular = this.convertTableNameToSingular;
        c.replaceOrmReferencesWithBasicTypes = this.replaceOrmReferencesWithBasicTypes;
        c.tableAndColumnComments = this.tableAndColumnComments;
        c.namingRulesMode = this.namingRulesMode;
        c.prefixesToSkipInTableName = this.prefixesToSkipInTableName;
        c.prefixesToSkipInColumnName = this.prefixesToSkipInColumnName;
        c.suffixesToSkipInTableName = this.suffixesToSkipInTableName;
        c.suffixesToSkipInColumnName = this.suffixesToSkipInColumnName;
        c.reservedKeywordFieldSuffix = this.reservedKeywordFieldSuffix;
        c.selectedDatabaseEngine = this.selectedDatabaseEngine;
        c.setTypeMappings(this.typeMappings);
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JpaReverseEngineeringSettings that = (JpaReverseEngineeringSettings) o;
        return useFetchTypeLazy == that.useFetchTypeLazy &&
                useValidationAnnotations == that.useValidationAnnotations &&
                convertTableNameToSingular == that.convertTableNameToSingular &&
                replaceOrmReferencesWithBasicTypes == that.replaceOrmReferencesWithBasicTypes &&
                Objects.equals(tableAndColumnComments, that.tableAndColumnComments) &&
                Objects.equals(namingRulesMode, that.namingRulesMode) &&
                Objects.equals(prefixesToSkipInTableName, that.prefixesToSkipInTableName) &&
                Objects.equals(prefixesToSkipInColumnName, that.prefixesToSkipInColumnName) &&
                Objects.equals(suffixesToSkipInTableName, that.suffixesToSkipInTableName) &&
                Objects.equals(suffixesToSkipInColumnName, that.suffixesToSkipInColumnName) &&
                Objects.equals(reservedKeywordFieldSuffix, that.reservedKeywordFieldSuffix) &&
                Objects.equals(selectedDatabaseEngine, that.selectedDatabaseEngine) &&
                Objects.equals(typeMappings, that.typeMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useFetchTypeLazy, useValidationAnnotations, convertTableNameToSingular,
                replaceOrmReferencesWithBasicTypes, tableAndColumnComments, namingRulesMode,
                prefixesToSkipInTableName, prefixesToSkipInColumnName, suffixesToSkipInTableName,
                suffixesToSkipInColumnName, reservedKeywordFieldSuffix, selectedDatabaseEngine, typeMappings);
    }

    @Override
    public String toString() {
        return "JpaReverseEngineeringSettings{" +
                "namingRulesMode='" + namingRulesMode + '\'' +
                ", comments='" + tableAndColumnComments + '\'' +
                ", mappings=" + typeMappings.size() +
                '}';
    }
}
