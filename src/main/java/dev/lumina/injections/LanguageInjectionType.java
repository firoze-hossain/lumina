package dev.lumina.injections;

import java.util.List;

/**
 * Definition of an injection type shown in the toolbar '+' dropdown menu.
 */
public record LanguageInjectionType(
        String id,
        String displayName,
        String hostLanguage,
        String shortcutNumber,
        String iconSymbol,
        String description
) {
    public static final List<LanguageInjectionType> ALL_TYPES = List.of(
            new LanguageInjectionType("generic.ftl", "1 Generic Ftl", "ftl", "1", "<#>", "FreeMarker template string injection"),
            new LanguageInjectionType("generic.go", "2 Generic Go", "go", "2", "Go", "Generic Go string injection"),
            new LanguageInjectionType("generic.groovy", "3 Generic Groovy", "groovy", "3", "Groovy", "Generic Groovy string injection"),
            new LanguageInjectionType("generic.js", "4 Generic Js", "javascript", "4", "JS", "Generic JavaScript string injection"),
            new LanguageInjectionType("generic.kotlin", "5 Generic Kotlin", "kotlin", "5", "K", "Generic Kotlin string injection"),
            new LanguageInjectionType("generic.php", "6 Generic Php", "php", "6", "php", "Generic PHP string injection"),
            new LanguageInjectionType("generic.python", "7 Generic Python", "python", "7", "py", "Generic Python string injection"),
            new LanguageInjectionType("generic.scala", "8 Generic Scala", "scala", "8", "S", "Generic Scala string injection"),
            new LanguageInjectionType("generic.smarty", "9 Generic Smarty", "smarty", "9", "?", "Generic Smarty template injection"),
            new LanguageInjectionType("js.tagged.literal", "0 JS Tagged Literal Injection", "javascript", "0", "JS", "JavaScript tagged template literal injection"),
            new LanguageInjectionType("java.parameter", "Java Parameter", "java", null, "P", "Java method parameter language injection"),
            new LanguageInjectionType("ruby.injection", "Ruby Injection", "ruby", null, "Rb", "Ruby string language injection"),
            new LanguageInjectionType("sql.injection", "SQL Injection", "sql", null, "SQL", "SQL query language injection"),
            new LanguageInjectionType("sql.type.injection", "SQL Type Injection", "sql", null, "SQL", "SQL type language injection"),
            new LanguageInjectionType("xml.attribute.injection", "XML Attribute Injection", "xml", null, "@", "XML attribute value language injection"),
            new LanguageInjectionType("xml.tag.injection", "XML Tag Injection", "xml", null, "<>", "XML tag content language injection")
    );
}
