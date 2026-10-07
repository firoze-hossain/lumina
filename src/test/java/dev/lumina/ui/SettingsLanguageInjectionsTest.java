package dev.lumina.ui;

import dev.lumina.injections.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Language Injections dynamic architecture, catalog completeness,
 * places calculations, scope transitions, provider extensibility, and strict brand isolation.
 */
public class SettingsLanguageInjectionsTest {

    private LanguageInjectionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = LanguageInjectionRegistry.getInstance();
        registry.resetToDefaults();
    }

    @Test
    void testCatalogCompletenessAndCounts() {
        // Matches exact stats from user screenshots 1, 2, 3, 4, 5:
        // "294 injections (720 of 722 places enabled)"
        assertEquals(294, registry.totalInjectionsCount(), "Total injections count should be exactly 294");
        assertEquals(722, registry.totalPlacesCount(), "Total places count should be exactly 722");
        assertEquals(720, registry.enabledPlacesCount(), "Default enabled places count should be exactly 720");
    }

    @Test
    void testGoInjectionsFromScreenshots() {
        List<LanguageInjection> injections = registry.getWorkingInjections();

        List<String> expectedGo = List.of(
                "go: SQL alter/drop/truncate table",
                "go: SQL create table",
                "go: SQL create/drop database",
                "go: SQL delete",
                "go: SQL insert",
                "go: SQL select",
                "go: SQL update"
        );

        for (String expectedName : expectedGo) {
            LanguageInjection found = injections.stream()
                    .filter(i -> i.getDisplayName().equals(expectedName))
                    .findFirst()
                    .orElse(null);
            assertNotNull(found, "Missing Go injection: " + expectedName);
            assertEquals("SQL", found.getInjectedLanguage());
            assertEquals("go", found.getHostLanguage());
            assertEquals(LanguageInjectionScope.BUILT_IN, found.getScope());
            assertTrue(found.isEnabled());
        }
    }

    @Test
    void testGroovyInjectionsFromScreenshots() {
        List<LanguageInjection> injections = registry.getWorkingInjections();

        // groovy: RegExp should be in IDE scope
        LanguageInjection regexp = injections.stream()
                .filter(i -> i.getDisplayName().equals("groovy: RegExp"))
                .findFirst()
                .orElse(null);
        assertNotNull(regexp);
        assertEquals("RegExp", regexp.getInjectedLanguage());
        assertEquals(LanguageInjectionScope.IDE, regexp.getScope());

        // groovy: Connection (java.sql)
        LanguageInjection conn = injections.stream()
                .filter(i -> i.getDisplayName().equals("groovy: Connection (java.sql)"))
                .findFirst()
                .orElse(null);
        assertNotNull(conn);
        assertEquals("SQL", conn.getInjectedLanguage());
        assertEquals(LanguageInjectionScope.BUILT_IN, conn.getScope());
    }

    @Test
    void testJavaInjectionsFromScreenshots() {
        List<LanguageInjection> injections = registry.getWorkingInjections();

        // AssertJ RegExp & XML
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().contains("AssertJ") && i.getInjectedLanguage().equals("RegExp")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().contains("AssertJ") && i.getInjectedLanguage().equals("XML")));

        // Micronaut MongoDB JSON
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().contains("MongoAggregateQuery") && i.getInjectedLanguage().equals("Micronaut-MongoDB-JSON")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().contains("MongoFindQuery") && i.getInjectedLanguage().equals("Micronaut-MongoDB-JSON")));

        // Spring EL
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().contains("AuthorizedUrl.access") && i.getInjectedLanguage().equals("Spring EL")));

        // Reactiveverse PostgreSQL (IDE scope)
        LanguageInjection postgres = injections.stream()
                .filter(i -> i.getDisplayName().contains("Reactiveverse Postgres Client"))
                .findFirst()
                .orElse(null);
        assertNotNull(postgres);
        assertEquals("PostgreSQL", postgres.getInjectedLanguage());
        assertEquals(LanguageInjectionScope.IDE, postgres.getScope());

        // Charset Name in IDE scope is disabled by default (explaining 720 of 722 places enabled)
        LanguageInjection charsetIde = registry.findInjection("java.charset.ide");
        assertNotNull(charsetIde);
        assertFalse(charsetIde.isEnabled(), "Charset Name IDE entry should be disabled initially to match screenshot");
        assertEquals(2, charsetIde.getPlacesCount());
    }

    @Test
    void testSpringAndAdditionalJavaInjectionsFromLatestScreenshots() {
        List<LanguageInjection> injections = registry.getWorkingInjections();

        // Spring @Cacheable (Spring EL, IDE)
        LanguageInjection cacheable = injections.stream()
                .filter(i -> i.getDisplayName().equals("java: Spring @Cacheable and @CacheEvict"))
                .findFirst().orElse(null);
        assertNotNull(cacheable);
        assertEquals("Spring EL", cacheable.getInjectedLanguage());
        assertEquals(LanguageInjectionScope.IDE, cacheable.getScope());

        // Spring @EventListener (Spring EL, Built-in)
        LanguageInjection eventListener = injections.stream()
                .filter(i -> i.getDisplayName().equals("java: Spring @EventListener"))
                .findFirst().orElse(null);
        assertNotNull(eventListener);
        assertEquals("Spring EL", eventListener.getInjectedLanguage());
        assertEquals(LanguageInjectionScope.BUILT_IN, eventListener.getScope());

        // SmallRye Mutiny SqlConnection (SQL, IDE)
        LanguageInjection mutinyConn = injections.stream()
                .filter(i -> i.getDisplayName().contains("SmallRye Mutiny SqlConnection"))
                .findFirst().orElse(null);
        assertNotNull(mutinyConn);
        assertEquals("SQL", mutinyConn.getInjectedLanguage());
        assertEquals(LanguageInjectionScope.IDE, mutinyConn.getScope());

        // WireMock (JSON, IDE) & (RegExp, IDE) & (XML, IDE)
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().contains("WireMock") && i.getInjectedLanguage().equals("JSON") && i.getScope() == LanguageInjectionScope.IDE));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().contains("WireMock") && i.getInjectedLanguage().equals("RegExp") && i.getScope() == LanguageInjectionScope.IDE));

        // jOOQ (SQL, IDE)
        LanguageInjection jooqSql = injections.stream()
                .filter(i -> i.getDisplayName().contains("jOOQ") && i.getInjectedLanguage().equals("SQL"))
                .findFirst().orElse(null);
        assertNotNull(jooqSql);
        assertEquals(LanguageInjectionScope.IDE, jooqSql.getScope());

        // setStyle (javafx.css) (CSS, Built-in)
        LanguageInjection setStyle = injections.stream()
                .filter(i -> i.getDisplayName().equals("java: setStyle (javafx.css)"))
                .findFirst().orElse(null);
        assertNotNull(setStyle);
        assertEquals("CSS", setStyle.getInjectedLanguage());
    }

    @Test
    void testJsKotlinPhpPythonRubyInjectionsFromLatestScreenshots() {
        List<LanguageInjection> injections = registry.getWorkingInjections();

        // JS
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("js: HTML5 SQL Database (SQLite)") && i.getInjectedLanguage().equals("SQLite")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("js: React JSX in JS strings") && i.getInjectedLanguage().equals("JavaScript")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("js: flash.data (SQLite)") && i.getScope() == LanguageInjectionScope.IDE));

        // Kotlin
        LanguageInjection deprecated = injections.stream()
                .filter(i -> i.getDisplayName().equals("kotlin: Kotlin @Deprecated ReplaceWith"))
                .findFirst().orElse(null);
        assertNotNull(deprecated);
        assertEquals("Kotlin", deprecated.getInjectedLanguage());

        LanguageInjection kotlinRegExp = injections.stream()
                .filter(i -> i.getDisplayName().equals("kotlin: Kotlin RegExp"))
                .findFirst().orElse(null);
        assertNotNull(kotlinRegExp);
        assertEquals(LanguageInjectionScope.IDE, kotlinRegExp.getScope());

        // PHP
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("php: <<< REGEXP") && i.getInjectedLanguage().equals("PhpRegExp")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("php: PHP in eval") && i.getInjectedLanguage().equals("Injectable PHP")));

        // Python
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("python: sqlite3") && i.getScope() == LanguageInjectionScope.IDE));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("python: Playwright") && i.getInjectedLanguage().equals("JavaScript") && i.getScope() == LanguageInjectionScope.IDE));

        // Ruby
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("ruby: Heredoc") && i.getInjectedLanguage().equals("ERB")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("ruby: Heredoc") && i.getInjectedLanguage().equals("JavaScript") && i.getScope() == LanguageInjectionScope.IDE));
    }

    @Test
    void testAll16InjectionTypesPresent() {
        assertEquals(16, LanguageInjectionType.ALL_TYPES.size(), "Should have all 16 injection types from the + dropdown");

        List<String> displayNames = LanguageInjectionType.ALL_TYPES.stream()
                .map(LanguageInjectionType::displayName)
                .toList();

        assertTrue(displayNames.contains("1 Generic Ftl"));
        assertTrue(displayNames.contains("2 Generic Go"));
        assertTrue(displayNames.contains("3 Generic Groovy"));
        assertTrue(displayNames.contains("4 Generic Js"));
        assertTrue(displayNames.contains("5 Generic Kotlin"));
        assertTrue(displayNames.contains("6 Generic Php"));
        assertTrue(displayNames.contains("7 Generic Python"));
        assertTrue(displayNames.contains("8 Generic Scala"));
        assertTrue(displayNames.contains("9 Generic Smarty"));
        assertTrue(displayNames.contains("0 JS Tagged Literal Injection"));
        assertTrue(displayNames.contains("Java Parameter"));
        assertTrue(displayNames.contains("Ruby Injection"));
        assertTrue(displayNames.contains("SQL Injection"));
        assertTrue(displayNames.contains("SQL Type Injection"));
        assertTrue(displayNames.contains("XML Attribute Injection"));
        assertTrue(displayNames.contains("XML Tag Injection"));
    }

    @Test
    void testDynamicProviderExtension() {
        // Demonstrate how external plugins dynamically contribute injections without modifying core UI
        LanguageInjectionProvider customPluginProvider = new LanguageInjectionProvider() {
            @Override
            public String getProviderName() {
                return "GraphQL Plugin";
            }

            @Override
            public List<LanguageInjection> getInjections() {
                return List.of(new LanguageInjection(
                        "graphql.custom.query",
                        "ts: gql tagged template",
                        "typescript",
                        "GraphQL",
                        LanguageInjectionScope.PROJECT,
                        true,
                        3,
                        "generic.js",
                        "gql`...`"
                ));
            }
        };

        registry.registerProvider(customPluginProvider);

        LanguageInjection contributed = registry.findInjection("graphql.custom.query");
        assertNotNull(contributed, "Custom plugin injection must be registered dynamically");
        assertEquals("GraphQL", contributed.getInjectedLanguage());
        assertEquals(LanguageInjectionScope.PROJECT, contributed.getScope());
        assertEquals(295, registry.totalInjectionsCount());
    }

    @Test
    void testPlacesCalculationOnToggle() {
        assertEquals(720, registry.enabledPlacesCount());

        // Enabling java.charset.ide (2 places) makes it 722 of 722
        registry.setInjectionEnabled("java.charset.ide", true);
        assertEquals(722, registry.enabledPlacesCount());

        LanguageInjection goSelect = registry.findInjection("go.sql.select");
        assertNotNull(goSelect);
        int goSelectPlaces = goSelect.getPlacesCount();

        registry.setInjectionEnabled("go.sql.select", false);
        assertEquals(722 - goSelectPlaces, registry.enabledPlacesCount());

        // Disabling selected
        LanguageInjection goAlter = registry.findInjection("go.sql.alter");
        LanguageInjection goDelete = registry.findInjection("go.sql.delete");
        assertNotNull(goAlter);
        assertNotNull(goDelete);
        registry.disableSelected(List.of("go.sql.alter", "go.sql.delete"));
        assertEquals(722 - goSelectPlaces - goAlter.getPlacesCount() - goDelete.getPlacesCount(), registry.enabledPlacesCount());
    }

    @Test
    void testScalaSqlXmlInjectionsFromLatestScreenshots() {
        List<LanguageInjection> injections = registry.getWorkingInjections();

        // Verify first and last entries in the sorted table
        assertEquals("go: SQL alter/drop/truncate table", injections.get(0).getDisplayName(),
                "Top item should be go: SQL alter/drop/truncate table matching Image 1");
        assertEquals("xml: style in .fxml", injections.get(injections.size() - 1).getDisplayName(),
                "Bottom item should be xml: style in .fxml matching Image 3");

        // Scala
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("scala: Regex (scala.util.matching)") && i.getInjectedLanguage().equals("RegExp")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("scala: String.r (scala)") && i.getInjectedLanguage().equals("RegExp")));

        // SQL Dialects
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("sql: (?i)regclass") && i.getInjectedLanguage().equals("PostgreSQL")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("sql: ClickHouse JSON") && i.getScope() == LanguageInjectionScope.IDE));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("sql: ClickHouse RegExp") && i.getInjectedLanguage().equals("RegExp")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("sql: Derby XPath") && i.getInjectedLanguage().equals("XPath2")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("sql: MySQL XPath") && i.getInjectedLanguage().equals("XPath2")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("sql: Oracle XPath") && i.getInjectedLanguage().equals("XPath2") && i.getScope() == LanguageInjectionScope.IDE));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("sql: PostgreSQL dblink") && i.getInjectedLanguage().equals("PostgreSQL") && i.getScope() == LanguageInjectionScope.IDE));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("sql: Sybase XPath") && i.getInjectedLanguage().equals("XPath2") && i.getScope() == LanguageInjectionScope.IDE));

        // XML
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("xml: SpEL for Spring Cache") && i.getInjectedLanguage().equals("Spring EL") && i.getScope() == LanguageInjectionScope.IDE));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("xml: MyBatis sql|select|insert|update|delete") && i.getInjectedLanguage().equals("SQL")));
        assertTrue(injections.stream().anyMatch(i -> i.getDisplayName().equals("xml: style in .fxml") && i.getInjectedLanguage().equals("CSS") && i.getScope() == LanguageInjectionScope.BUILT_IN));
    }

    @Test
    void testScopeChangeAndDuplicate() {
        LanguageInjection goSelect = registry.findInjection("go.sql.select");
        assertNotNull(goSelect);
        assertEquals(LanguageInjectionScope.BUILT_IN, goSelect.getScope());

        // Duplicate
        LanguageInjection copy = registry.duplicateInjection(goSelect);
        assertNotNull(copy);
        assertTrue(copy.getDisplayName().endsWith(" Copy"));
        assertEquals(LanguageInjectionScope.IDE, copy.getScope());
        assertEquals(295, registry.totalInjectionsCount());

        // Move scope to PROJECT
        registry.moveToScope(copy.getId(), LanguageInjectionScope.PROJECT);
        assertEquals(LanguageInjectionScope.PROJECT, registry.findInjection(copy.getId()).getScope());

        // Remove custom copy
        boolean removed = registry.removeInjection(copy.getId());
        assertTrue(removed);
        assertEquals(294, registry.totalInjectionsCount());
    }

    @Test
    void testDirtyTrackingAndApplyReset() {
        assertFalse(registry.isModified(), "Initially should not be modified");

        registry.setInjectionEnabled("go.sql.delete", false);
        assertTrue(registry.isModified(), "Should be modified after disabling an injection");

        // Apply commits the change
        registry.apply();
        assertFalse(registry.isModified(), "Should not be modified after apply()");
        assertFalse(registry.findInjection("go.sql.delete").isEnabled());

        // Change again then reset
        registry.setInjectionEnabled("go.sql.delete", true);
        assertTrue(registry.isModified());

        registry.reset();
        assertFalse(registry.isModified());
        assertFalse(registry.findInjection("go.sql.delete").isEnabled(), "Reset should restore to applied state");
    }

    @Test
    void testBrandIsolation() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/injections/LanguageInjectionScope.java",
                "src/main/java/dev/lumina/injections/LanguageInjectionType.java",
                "src/main/java/dev/lumina/injections/LanguageInjection.java",
                "src/main/java/dev/lumina/injections/LanguageInjectionProvider.java",
                "src/main/java/dev/lumina/injections/BuiltInLanguageInjectionProvider.java",
                "src/main/java/dev/lumina/injections/LanguageInjectionRegistry.java",
                "src/main/java/dev/lumina/ui/SettingsLanguageInjectionsPage.java"
        );

        String b1 = "intel" + "lij";
        String b2 = "jet" + "brains";
        String b3 = "id" + "ea";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)\\b(" + b1 + "|" + b2 + "|" + b3 + "(?!vim))\\b");

        for (String file : filesToCheck) {
            File f = new File(file);
            assertTrue(f.exists(), "File must exist: " + file);
            String content = Files.readString(f.toPath());
            java.util.regex.Matcher m = pattern.matcher(content);
            assertFalse(m.find(), "Competitor brand found in " + file + ": " + (m.reset().find() ? m.group() : ""));
        }
    }

    @Test
    void testLanguageInjectionComparable() {
        LanguageInjection a = new LanguageInjection("id.a", "a: Injection", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 1, "type", "");
        LanguageInjection b = new LanguageInjection("id.b", "b: Injection", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 1, "type", "");
        LanguageInjection a2 = new LanguageInjection("id.a", "A: Injection", "java", "SQL", LanguageInjectionScope.BUILT_IN, true, 1, "type", "");

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a2));
    }

    @Test
    void testLanguageInjectionAdvancedSettings() {
        LanguageInjectionAdvancedSettings advanced = LanguageInjectionAdvancedSettings.getInstance();
        advanced.resetToDefaults();

        // Verify default options match screenshot
        assertEquals("org.intellij.lang.annotations.Language", advanced.getLanguageAnnotation());
        assertEquals("org.intellij.lang.annotations.Pattern", advanced.getPatternAnnotation());
        assertEquals("org.intellij.lang.annotations.Subst", advanced.getSubstitutionAnnotation());
        assertEquals(LanguageInjectionAdvancedSettings.RuntimePatternValidation.ASSERTIONS, advanced.getRuntimePatternValidation());
        assertEquals(LanguageInjectionAdvancedSettings.PerformanceMode.ANALYZE_REFERENCES, advanced.getPerformanceMode());
        assertFalse(advanced.isConvertUndefinedOperandsToText());
        assertFalse(advanced.isAddLanguageAnnotationOrComment());
        assertFalse(advanced.isModified());

        // Modify settings
        advanced.setLanguageAnnotation("custom.annotations.Lang");
        assertTrue(advanced.isModified());
        advanced.setRuntimePatternValidation(LanguageInjectionAdvancedSettings.RuntimePatternValidation.ILLEGAL_ARGUMENT_EXCEPTION);
        advanced.setPerformanceMode(LanguageInjectionAdvancedSettings.PerformanceMode.DATAFLOW_ANALYSIS);
        advanced.setConvertUndefinedOperandsToText(true);
        advanced.setAddLanguageAnnotationOrComment(true);

        // Apply
        advanced.apply();
        assertFalse(advanced.isModified());
        assertEquals("custom.annotations.Lang", advanced.getLanguageAnnotation());
        assertEquals(LanguageInjectionAdvancedSettings.RuntimePatternValidation.ILLEGAL_ARGUMENT_EXCEPTION, advanced.getRuntimePatternValidation());
        assertEquals(LanguageInjectionAdvancedSettings.PerformanceMode.DATAFLOW_ANALYSIS, advanced.getPerformanceMode());
        assertTrue(advanced.isConvertUndefinedOperandsToText());
        assertTrue(advanced.isAddLanguageAnnotationOrComment());

        // Reset
        advanced.setLanguageAnnotation("another.Lang");
        assertTrue(advanced.isModified());
        advanced.reset();
        assertFalse(advanced.isModified());
        assertEquals("custom.annotations.Lang", advanced.getLanguageAnnotation());

        // Reset to defaults
        advanced.resetToDefaults();
        assertFalse(advanced.isModified());
        assertEquals("org.intellij.lang.annotations.Language", advanced.getLanguageAnnotation());
    }
}
