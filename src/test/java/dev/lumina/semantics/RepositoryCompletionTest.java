package dev.lumina.semantics;

import dev.lumina.diagnostics.JavaDiagnostics;
import dev.lumina.diagnostics.JpaDiagnostics;
import dev.lumina.ui.CompletionPopup;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryCompletionTest {

    @Test
    void testInterfaceKindGlyph() {
        assertEquals("I", CompletionPopup.glyphFor(Completion.Kind.INTERFACE));
        assertEquals("C", CompletionPopup.glyphFor(Completion.Kind.CLASS));
        assertEquals("@", CompletionPopup.glyphFor(Completion.Kind.ANNOTATION));
    }

    @Test
    void testDetectExtendsInterfaceInContext() {
        String code1 = "public interface StudentRepository extends ";
        Completion.Context ctx1 = Completion.contextAt(code1, code1.length());
        assertNotNull(ctx1);
        assertEquals("StudentRepository", ctx1.extendsInterface());
        assertEquals("", ctx1.prefix());

        String code2 = "public interface StudentRepository extends J";
        Completion.Context ctx2 = Completion.contextAt(code2, code2.length());
        assertNotNull(ctx2);
        assertEquals("StudentRepository", ctx2.extendsInterface());
        assertEquals("J", ctx2.prefix());

        String code3 = "public interface UserRepository extends Crud";
        Completion.Context ctx3 = Completion.contextAt(code3, code3.length());
        assertNotNull(ctx3);
        assertEquals("UserRepository", ctx3.extendsInterface());
        assertEquals("Crud", ctx3.prefix());
    }

    @Test
    void testFallbackRepositoryCompletions() {
        List<Completion.Item> itemsEmpty = SemanticEngine.fallbackRepositoryCompletions("StudentRepository", "");
        assertFalse(itemsEmpty.isEmpty());
        assertTrue(itemsEmpty.stream().anyMatch(i -> i.insert().equals("JpaRepository<Student, Long>")));
        assertTrue(itemsEmpty.stream().anyMatch(i -> i.insert().equals("CrudRepository<Student, Long>")));

        List<Completion.Item> itemsJ = SemanticEngine.fallbackRepositoryCompletions("StudentRepository", "J");
        assertEquals(1, itemsJ.size());
        Completion.Item jpa = itemsJ.get(0);
        assertEquals("JpaRepository<Student, Long>", jpa.insert());
        assertEquals(Completion.Kind.INTERFACE, jpa.kind());
        assertEquals("org.springframework.data.jpa.repository.JpaRepository", jpa.importFqcn());
        assertEquals(" (org.springframework.data.jpa.repository)", jpa.detail());

        List<Completion.Item> itemsCrud = SemanticEngine.fallbackRepositoryCompletions("ProductRepo", "Crud");
        assertFalse(itemsCrud.isEmpty());
        Completion.Item crud = itemsCrud.get(0);
        assertEquals("CrudRepository<Product, Long>", crud.insert());
        assertEquals(Completion.Kind.INTERFACE, crud.kind());
        assertEquals("org.springframework.data.repository.CrudRepository", crud.importFqcn());
    }

    @Test
    void testJpaDiagnosticsMissingPrimaryKey() {
        String entityWithoutId = """
                package com.example.demo.model;

                import jakarta.persistence.Entity;

                @Entity
                public class Student {
                }
                """;

        List<JavaDiagnostics.Diag> diags = JpaDiagnostics.checkPersistentEntities(
                Path.of("Student.java"), entityWithoutId, "demo");

        assertEquals(1, diags.size());
        JavaDiagnostics.Diag d = diags.get(0);
        assertEquals(JavaDiagnostics.Severity.ERROR, d.severity());
        assertEquals("Persistent entity 'Student' should have primary key", d.title());
        assertEquals("Persistent entity should have primary key", d.message());
        assertEquals("add-id-attribute:Student", d.quickFix());
        assertTrue(d.start() >= 0 && d.end() > d.start());
        assertEquals("Student", entityWithoutId.substring(d.start(), d.end()));
    }

    @Test
    void testJpaDiagnosticsWithPrimaryKey() {
        String entityWithId = """
                package com.example.demo.model;

                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;

                @Entity
                public class Student {
                    @Id
                    private Long id;
                }
                """;

        List<JavaDiagnostics.Diag> diags1 = JpaDiagnostics.checkPersistentEntities(
                Path.of("Student.java"), entityWithId, "demo");
        assertTrue(diags1.isEmpty());

        String entityWithEmbeddedId = """
                package com.example.demo.model;

                import jakarta.persistence.Entity;
                import jakarta.persistence.EmbeddedId;

                @Entity
                public class Student {
                    @EmbeddedId
                    private StudentId id;
                }
                """;

        List<JavaDiagnostics.Diag> diags2 = JpaDiagnostics.checkPersistentEntities(
                Path.of("Student.java"), entityWithEmbeddedId, "demo");
        assertTrue(diags2.isEmpty());

        String nonEntity = """
                package com.example.demo.model;

                public class RegularClass {
                    private String name;
                }
                """;

        List<JavaDiagnostics.Diag> diags3 = JpaDiagnostics.checkPersistentEntities(
                Path.of("RegularClass.java"), nonEntity, "demo");
        assertTrue(diags3.isEmpty());
    }

    @Test
    void testBytecodeKindDetection() {
        byte[] dummyClass = new byte[]{
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                0, 0, 0, 65,
                0, 1,
                0, 0x20      // ACC_SUPER
        };
        assertEquals(Completion.Kind.CLASS, SemanticEngine.bytecodeKind(dummyClass));

        byte[] dummyInterface = new byte[]{
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                0, 0, 0, 65,
                0, 1,
                0x02, 0      // ACC_INTERFACE (0x0200)
        };
        assertEquals(Completion.Kind.INTERFACE, SemanticEngine.bytecodeKind(dummyInterface));

        byte[] dummyAnnotation = new byte[]{
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                0, 0, 0, 65,
                0, 1,
                0x22, 0      // ACC_ANNOTATION (0x2000) | ACC_INTERFACE (0x0200)
        };
        assertEquals(Completion.Kind.ANNOTATION, SemanticEngine.bytecodeKind(dummyAnnotation));
    }
}
