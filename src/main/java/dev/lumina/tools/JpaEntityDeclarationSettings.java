package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > JPA Entity Declaration settings matching Image 1.
 */
public class JpaEntityDeclarationSettings implements Cloneable {

    private boolean generateJpaAnnotationsOnGetterMethod = false;
    private boolean generateSerialVersionUidField = false;
    private boolean registerHibernateCustomTypesOnEntity = false;
    private boolean useFetchTypeLazy = false;
    private boolean generateReturnThisInAttributeSetters = false;
    private String scaffoldingLanguage = "Always Ask";
    private String defaultEntityAttributeAccessModifier = "Private";

    private List<JpaNameTemplateEntry> nameTemplates = new ArrayList<>();
    private String indexConstraintNamesCase = "lower";

    // Lombok
    private boolean lombokGenerateGetterAndSetter = true;
    private boolean lombokGenerateBuilder = false;
    private boolean lombokGenerateAllArgsConstructor = false;
    private boolean lombokGenerateNoArgsConstructor = false;
    private boolean lombokGenerateToString = false;
    private boolean lombokGenerateToStringWithOnlyExplicitlyIncluded = false;

    // Constants Generation
    private boolean generateConstantsForNewObjectNames = false;
    private boolean constantsEntityName = true;
    private boolean constantsTableName = true;
    private boolean constantsColumnName = true;
    private String constantsPlacement = "Same class";

    public JpaEntityDeclarationSettings() {
        this.nameTemplates = createDefaultTemplates();
    }

    public static List<JpaNameTemplateEntry> createDefaultTemplates() {
        List<JpaNameTemplateEntry> list = new ArrayList<>();
        list.add(new JpaNameTemplateEntry(true, "Table", "Lower", "", "", true, false));
        list.add(new JpaNameTemplateEntry(true, "Column", "Lower", "", "", true, false));
        list.add(new JpaNameTemplateEntry(false, "Entity", "As is", "", "", false, false));
        return list;
    }

    public boolean isGenerateJpaAnnotationsOnGetterMethod() {
        return generateJpaAnnotationsOnGetterMethod;
    }

    public void setGenerateJpaAnnotationsOnGetterMethod(boolean generateJpaAnnotationsOnGetterMethod) {
        this.generateJpaAnnotationsOnGetterMethod = generateJpaAnnotationsOnGetterMethod;
    }

    public boolean isGenerateSerialVersionUidField() {
        return generateSerialVersionUidField;
    }

    public void setGenerateSerialVersionUidField(boolean generateSerialVersionUidField) {
        this.generateSerialVersionUidField = generateSerialVersionUidField;
    }

    public boolean isRegisterHibernateCustomTypesOnEntity() {
        return registerHibernateCustomTypesOnEntity;
    }

    public void setRegisterHibernateCustomTypesOnEntity(boolean registerHibernateCustomTypesOnEntity) {
        this.registerHibernateCustomTypesOnEntity = registerHibernateCustomTypesOnEntity;
    }

    public boolean isUseFetchTypeLazy() {
        return useFetchTypeLazy;
    }

    public void setUseFetchTypeLazy(boolean useFetchTypeLazy) {
        this.useFetchTypeLazy = useFetchTypeLazy;
    }

    public boolean isGenerateReturnThisInAttributeSetters() {
        return generateReturnThisInAttributeSetters;
    }

    public void setGenerateReturnThisInAttributeSetters(boolean generateReturnThisInAttributeSetters) {
        this.generateReturnThisInAttributeSetters = generateReturnThisInAttributeSetters;
    }

    public String getScaffoldingLanguage() {
        return scaffoldingLanguage;
    }

    public void setScaffoldingLanguage(String scaffoldingLanguage) {
        this.scaffoldingLanguage = scaffoldingLanguage != null ? scaffoldingLanguage : "Always Ask";
    }

    public String getDefaultEntityAttributeAccessModifier() {
        return defaultEntityAttributeAccessModifier;
    }

    public void setDefaultEntityAttributeAccessModifier(String defaultEntityAttributeAccessModifier) {
        this.defaultEntityAttributeAccessModifier = defaultEntityAttributeAccessModifier != null ? defaultEntityAttributeAccessModifier : "Private";
    }

    public List<JpaNameTemplateEntry> getNameTemplates() {
        return nameTemplates;
    }

    public void setNameTemplates(List<JpaNameTemplateEntry> nameTemplates) {
        this.nameTemplates = new ArrayList<>();
        if (nameTemplates != null) {
            for (JpaNameTemplateEntry e : nameTemplates) {
                this.nameTemplates.add(e != null ? e.copy() : new JpaNameTemplateEntry());
            }
        }
    }

    public String getIndexConstraintNamesCase() {
        return indexConstraintNamesCase;
    }

    public void setIndexConstraintNamesCase(String indexConstraintNamesCase) {
        this.indexConstraintNamesCase = indexConstraintNamesCase != null ? indexConstraintNamesCase : "lower";
    }

    public boolean isLombokGenerateGetterAndSetter() {
        return lombokGenerateGetterAndSetter;
    }

    public void setLombokGenerateGetterAndSetter(boolean lombokGenerateGetterAndSetter) {
        this.lombokGenerateGetterAndSetter = lombokGenerateGetterAndSetter;
    }

    public boolean isLombokGenerateBuilder() {
        return lombokGenerateBuilder;
    }

    public void setLombokGenerateBuilder(boolean lombokGenerateBuilder) {
        this.lombokGenerateBuilder = lombokGenerateBuilder;
    }

    public boolean isLombokGenerateAllArgsConstructor() {
        return lombokGenerateAllArgsConstructor;
    }

    public void setLombokGenerateAllArgsConstructor(boolean lombokGenerateAllArgsConstructor) {
        this.lombokGenerateAllArgsConstructor = lombokGenerateAllArgsConstructor;
    }

    public boolean isLombokGenerateNoArgsConstructor() {
        return lombokGenerateNoArgsConstructor;
    }

    public void setLombokGenerateNoArgsConstructor(boolean lombokGenerateNoArgsConstructor) {
        this.lombokGenerateNoArgsConstructor = lombokGenerateNoArgsConstructor;
    }

    public boolean isLombokGenerateToString() {
        return lombokGenerateToString;
    }

    public void setLombokGenerateToString(boolean lombokGenerateToString) {
        this.lombokGenerateToString = lombokGenerateToString;
    }

    public boolean isLombokGenerateToStringWithOnlyExplicitlyIncluded() {
        return lombokGenerateToStringWithOnlyExplicitlyIncluded;
    }

    public void setLombokGenerateToStringWithOnlyExplicitlyIncluded(boolean lombokGenerateToStringWithOnlyExplicitlyIncluded) {
        this.lombokGenerateToStringWithOnlyExplicitlyIncluded = lombokGenerateToStringWithOnlyExplicitlyIncluded;
    }

    public boolean isGenerateConstantsForNewObjectNames() {
        return generateConstantsForNewObjectNames;
    }

    public void setGenerateConstantsForNewObjectNames(boolean generateConstantsForNewObjectNames) {
        this.generateConstantsForNewObjectNames = generateConstantsForNewObjectNames;
    }

    public boolean isConstantsEntityName() {
        return constantsEntityName;
    }

    public void setConstantsEntityName(boolean constantsEntityName) {
        this.constantsEntityName = constantsEntityName;
    }

    public boolean isConstantsTableName() {
        return constantsTableName;
    }

    public void setConstantsTableName(boolean constantsTableName) {
        this.constantsTableName = constantsTableName;
    }

    public boolean isConstantsColumnName() {
        return constantsColumnName;
    }

    public void setConstantsColumnName(boolean constantsColumnName) {
        this.constantsColumnName = constantsColumnName;
    }

    public String getConstantsPlacement() {
        return constantsPlacement;
    }

    public void setConstantsPlacement(String constantsPlacement) {
        this.constantsPlacement = constantsPlacement != null ? constantsPlacement : "Same class";
    }

    public JpaEntityDeclarationSettings copy() {
        return clone();
    }

    @Override
    public JpaEntityDeclarationSettings clone() {
        JpaEntityDeclarationSettings c = new JpaEntityDeclarationSettings();
        c.generateJpaAnnotationsOnGetterMethod = this.generateJpaAnnotationsOnGetterMethod;
        c.generateSerialVersionUidField = this.generateSerialVersionUidField;
        c.registerHibernateCustomTypesOnEntity = this.registerHibernateCustomTypesOnEntity;
        c.useFetchTypeLazy = this.useFetchTypeLazy;
        c.generateReturnThisInAttributeSetters = this.generateReturnThisInAttributeSetters;
        c.scaffoldingLanguage = this.scaffoldingLanguage;
        c.defaultEntityAttributeAccessModifier = this.defaultEntityAttributeAccessModifier;
        c.setNameTemplates(this.nameTemplates);
        c.indexConstraintNamesCase = this.indexConstraintNamesCase;
        c.lombokGenerateGetterAndSetter = this.lombokGenerateGetterAndSetter;
        c.lombokGenerateBuilder = this.lombokGenerateBuilder;
        c.lombokGenerateAllArgsConstructor = this.lombokGenerateAllArgsConstructor;
        c.lombokGenerateNoArgsConstructor = this.lombokGenerateNoArgsConstructor;
        c.lombokGenerateToString = this.lombokGenerateToString;
        c.lombokGenerateToStringWithOnlyExplicitlyIncluded = this.lombokGenerateToStringWithOnlyExplicitlyIncluded;
        c.generateConstantsForNewObjectNames = this.generateConstantsForNewObjectNames;
        c.constantsEntityName = this.constantsEntityName;
        c.constantsTableName = this.constantsTableName;
        c.constantsColumnName = this.constantsColumnName;
        c.constantsPlacement = this.constantsPlacement;
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JpaEntityDeclarationSettings that = (JpaEntityDeclarationSettings) o;
        return generateJpaAnnotationsOnGetterMethod == that.generateJpaAnnotationsOnGetterMethod &&
                generateSerialVersionUidField == that.generateSerialVersionUidField &&
                registerHibernateCustomTypesOnEntity == that.registerHibernateCustomTypesOnEntity &&
                useFetchTypeLazy == that.useFetchTypeLazy &&
                generateReturnThisInAttributeSetters == that.generateReturnThisInAttributeSetters &&
                lombokGenerateGetterAndSetter == that.lombokGenerateGetterAndSetter &&
                lombokGenerateBuilder == that.lombokGenerateBuilder &&
                lombokGenerateAllArgsConstructor == that.lombokGenerateAllArgsConstructor &&
                lombokGenerateNoArgsConstructor == that.lombokGenerateNoArgsConstructor &&
                lombokGenerateToString == that.lombokGenerateToString &&
                lombokGenerateToStringWithOnlyExplicitlyIncluded == that.lombokGenerateToStringWithOnlyExplicitlyIncluded &&
                generateConstantsForNewObjectNames == that.generateConstantsForNewObjectNames &&
                constantsEntityName == that.constantsEntityName &&
                constantsTableName == that.constantsTableName &&
                constantsColumnName == that.constantsColumnName &&
                Objects.equals(scaffoldingLanguage, that.scaffoldingLanguage) &&
                Objects.equals(defaultEntityAttributeAccessModifier, that.defaultEntityAttributeAccessModifier) &&
                Objects.equals(nameTemplates, that.nameTemplates) &&
                Objects.equals(indexConstraintNamesCase, that.indexConstraintNamesCase) &&
                Objects.equals(constantsPlacement, that.constantsPlacement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(generateJpaAnnotationsOnGetterMethod, generateSerialVersionUidField,
                registerHibernateCustomTypesOnEntity, useFetchTypeLazy, generateReturnThisInAttributeSetters,
                scaffoldingLanguage, defaultEntityAttributeAccessModifier, nameTemplates, indexConstraintNamesCase,
                lombokGenerateGetterAndSetter, lombokGenerateBuilder, lombokGenerateAllArgsConstructor,
                lombokGenerateNoArgsConstructor, lombokGenerateToString, lombokGenerateToStringWithOnlyExplicitlyIncluded,
                generateConstantsForNewObjectNames, constantsEntityName, constantsTableName, constantsColumnName, constantsPlacement);
    }

    @Override
    public String toString() {
        return "JpaEntityDeclarationSettings{" +
                "scaffoldingLanguage='" + scaffoldingLanguage + '\'' +
                ", accessModifier='" + defaultEntityAttributeAccessModifier + '\'' +
                ", templates=" + nameTemplates.size() +
                '}';
    }
}
