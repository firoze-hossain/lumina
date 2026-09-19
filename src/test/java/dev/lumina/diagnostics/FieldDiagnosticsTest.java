package dev.lumina.diagnostics;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldDiagnosticsTest {

    @Test
    void testDetectsUnusedFields() {
        String code = """
                package org.example.spring_boot_depency.entity;

                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.GeneratedValue;
                import jakarta.persistence.GenerationType;

                @Entity
                public class Student {
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    private Long id;

                    private String firstName;
                    private String lastName;

                    public Long getId() {
                        return id;
                    }

                    public void setId(Long id) {
                        this.id = id;
                    }
                }
                """;

        List<JavaDiagnostics.Diag> diags = FieldDiagnostics.checkUnusedFields(
                Path.of("Student.java"), code, "spring_boot_depency");

        assertEquals(2, diags.size(), "Should find exactly 2 unused fields: firstName and lastName");

        JavaDiagnostics.Diag d1 = diags.get(0);
        assertEquals("Private field 'firstName' is never used", d1.title());
        assertEquals("unused-field:firstName", d1.quickFix());
        assertEquals("private String firstName", d1.description());
        assertEquals("org.example.spring_boot_depency.entity.Student", d1.context());
        assertEquals("spring_boot_depency", d1.origin());
        assertEquals(JavaDiagnostics.Severity.WARNING, d1.severity());

        JavaDiagnostics.Diag d2 = diags.get(1);
        assertEquals("Private field 'lastName' is never used", d2.title());
        assertEquals("unused-field:lastName", d2.quickFix());
        assertEquals("private String lastName", d2.description());
    }

    @Test
    void testNoUnusedFieldsWhenAllUsed() {
        String code = """
                package com.example;

                public class Person {
                    private String name;

                    public Person(String name) {
                        this.name = name;
                    }

                    public String getName() {
                        return name;
                    }
                }
                """;

        List<JavaDiagnostics.Diag> diags = FieldDiagnostics.checkUnusedFields(
                Path.of("Person.java"), code, "demo");

        assertTrue(diags.isEmpty(), "Should have no unused field warnings when field is referenced");
    }

    @Test
    void testLombokAnnotationIgnoresUnused() {
        String code = """
                package com.example;

                import lombok.Data;

                @Data
                public class Item {
                    private String title;
                    private double price;
                }
                """;

        List<JavaDiagnostics.Diag> diags = FieldDiagnostics.checkUnusedFields(
                Path.of("Item.java"), code, "demo");

        assertTrue(diags.isEmpty(), "Should not flag unused fields on Lombok @Data classes");
    }
}
