package dev.lumina.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaCodeGeneratorTest {

    private final String studentCode = """
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

    @Test
    void testGenerateGettersAndSetters() {
        String updated = JavaCodeGenerator.generateGettersAndSetters(studentCode, "firstName");
        assertTrue(updated.contains("public String getFirstName() {\n        return firstName;\n    }"));
        assertTrue(updated.contains("public void setFirstName(String firstName) {\n        this.firstName = firstName;\n    }"));

        var preview = JavaCodeGenerator.previewGettersAndSetters(studentCode, "firstName");
        assertTrue(preview.startLine() > 15);
        assertTrue(preview.code().contains("getFirstName()"));
        assertTrue(preview.code().contains("setFirstName(String firstName)"));
    }

    @Test
    void testGenerateConstructor() {
        String updated = JavaCodeGenerator.generateConstructor(studentCode, "firstName", "lastName");
        assertTrue(updated.contains("public Student(String firstName, String lastName) {"));
        assertTrue(updated.contains("this.firstName = firstName;"));
        assertTrue(updated.contains("this.lastName = lastName;"));
    }

    @Test
    void testGenerateAddConstructorParam() {
        String updated = JavaCodeGenerator.generateAddConstructorParam(studentCode, "firstName");
        assertTrue(updated.contains("public Student(String firstName) {"));
        assertTrue(updated.contains("this.firstName = firstName;"));

        // Now test adding second param to existing constructor
        String updated2 = JavaCodeGenerator.generateAddConstructorParam(updated, "lastName");
        assertTrue(updated2.contains("public Student(String firstName, String lastName) {"));
        assertTrue(updated2.contains("this.firstName = firstName;"));
        assertTrue(updated2.contains("this.lastName = lastName;"));
    }

    @Test
    void testGenerateToString() {
        String updated = JavaCodeGenerator.generateToString(studentCode);
        assertTrue(updated.contains("@Override\n    public String toString() {"));
        assertTrue(updated.contains("Student{"));
        assertTrue(updated.contains("firstName='\" + firstName + '\\''"));
    }

    @Test
    void testGenerateEqualsAndHashCode() {
        String updated = JavaCodeGenerator.generateEqualsAndHashCode(studentCode);
        assertTrue(updated.contains("import java.util.Objects;"));
        assertTrue(updated.contains("@Override\n    public boolean equals(Object o) {"));
        assertTrue(updated.contains("@Override\n    public int hashCode() {"));
        assertTrue(updated.contains("Objects.equals(id, student.id)"));
    }

    @Test
    void testGenerateLogger() {
        String updated = JavaCodeGenerator.generateLogger(studentCode);
        assertTrue(updated.contains("import org.slf4j.Logger;"));
        assertTrue(updated.contains("import org.slf4j.LoggerFactory;"));
        assertTrue(updated.contains("private static final Logger log = LoggerFactory.getLogger(Student.class);"));
    }

    @Test
    void testRemoveField() {
        String updated = JavaCodeGenerator.removeField(studentCode, "firstName");
        assertFalse(updated.contains("private String firstName;"));
        assertTrue(updated.contains("private String lastName;"));
    }
}
