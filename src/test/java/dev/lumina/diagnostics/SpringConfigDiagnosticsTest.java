package dev.lumina.diagnostics;

import dev.lumina.diagnostics.JavaDiagnostics.Diag;
import dev.lumina.diagnostics.JavaDiagnostics.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SpringConfigDiagnosticsTest {

    @Test
    void testDuplicatePropertyKeyProperties() {
        String content = """
                spring.application.name=spring_boot_depency
                spring.datasource.url=jdbc:mysql://localhost:3306/smconman
                spring.application.name=hello-ol
                """;
        List<Diag> diags = SpringConfigDiagnostics.analyze(content, false, List.of(), "", "spring_boot_depency");

        // Should find duplicate key error on line 3
        Diag dup = diags.stream()
                .filter(d -> "Duplicate property key".equals(d.title()) && d.line() == 3)
                .findFirst()
                .orElse(null);

        assertNotNull(dup, "Duplicate property diagnostic should be present");
        assertEquals(Severity.ERROR, dup.severity(), "Duplicate property must be ERROR (red wavy underline)");
        assertEquals("remove-property-line:3", dup.quickFix(), "Quick fix should be remove-property-line:3");
        assertEquals("String", dup.propertyType(), "Property type should be String");
        assertNotNull(dup.description(), "Description should be populated");
        assertTrue(dup.description().contains("Application name"), "Description should describe application name");
        assertNotNull(dup.origin(), "Origin should be populated");
        assertTrue(dup.origin().contains("spring-boot"), "Origin should reference spring-boot jar");
    }

    @Test
    void testMissingJdbcDriverWarning() {
        String content = """
                spring.datasource.url=jdbc:mysql://localhost:3306/smconman
                """;
        List<Diag> diags = SpringConfigDiagnostics.analyze(content, false, List.of(), "", "my_project_module");

        Diag driverDiag = diags.stream()
                .filter(d -> d.title() != null && d.title().contains("Driver class com.mysql.cj.jdbc.Driver not found"))
                .findFirst()
                .orElse(null);

        assertNotNull(driverDiag, "Missing JDBC driver diagnostic should be present");
        assertEquals(Severity.ERROR, driverDiag.severity(), "Missing driver should be ERROR (red wavy underline)");
        assertEquals(0, driverDiag.start(), "Diagnostic should start at the beginning of the property key");
        assertEquals(content.trim().length(), driverDiag.end(), "Diagnostic should cover the entire property line through value");
        assertEquals("add-dependency:mysql", driverDiag.quickFix(), "Quick fix should be add-dependency:mysql");
        assertEquals("Driver class com.mysql.cj.jdbc.Driver not found in dependencies", driverDiag.title());
        assertEquals("spring.datasource.url=\"jdbc:mysql://localhost:3306/smconman\"", driverDiag.context());
        assertEquals("my_project_module", driverDiag.origin(), "Origin should match module name dynamically");
    }

    @Test
    void testDriverPresentSuppressesWarning() {
        String content = """
                spring.datasource.url=jdbc:mysql://localhost:3306/smconman
                """;
        String fakeClasspath = "/home/user/.m2/repository/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar";
        List<Diag> diags = SpringConfigDiagnostics.analyze(content, false, List.of(), fakeClasspath, "my_module");

        boolean hasDriverDiag = diags.stream()
                .anyMatch(d -> d.title() != null && d.title().contains("com.mysql.cj.jdbc.Driver"));

        assertFalse(hasDriverDiag, "Missing driver diagnostic should not be present when driver is on classpath");
    }

    @Test
    void testDuplicateKeyInYaml() {
        String content = """
                spring:
                  application:
                    name: app-one
                    name: app-two
                """;
        List<Diag> diags = SpringConfigDiagnostics.analyze(content, true, List.of(), "", "sample_module");

        Diag dup = diags.stream()
                .filter(d -> "Duplicate property key".equals(d.title()))
                .findFirst()
                .orElse(null);

        assertNotNull(dup, "Duplicate property diagnostic in YAML should be detected");
        assertEquals(Severity.ERROR, dup.severity());
        assertEquals("remove-property-line:4", dup.quickFix());
    }
}
