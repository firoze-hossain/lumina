package dev.lumina.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JpaDiagnosticsTest {

    @TempDir
    Path tempDir;

    @Test
    void testDirectId_noDiagnostics() {
        String entityCode = """
                package com.example.model;

                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;

                @Entity
                public class User {
                    @Id
                    private Long id;
                }
                """;

        List<JavaDiagnostics.Diag> diags = JpaDiagnostics.checkPersistentEntities(
                Path.of("User.java"), entityCode, List.of(), "demo");
        assertTrue(diags.isEmpty());
    }

    @Test
    void testDirectEmbeddedId_noDiagnostics() {
        String entityCode = """
                package com.example.model;

                import jakarta.persistence.Entity;
                import jakarta.persistence.EmbeddedId;

                @Entity
                public class OrderItem {
                    @EmbeddedId
                    private OrderItemId id;
                }
                """;

        List<JavaDiagnostics.Diag> diags = JpaDiagnostics.checkPersistentEntities(
                Path.of("OrderItem.java"), entityCode, List.of(), "demo");
        assertTrue(diags.isEmpty());
    }

    @Test
    void testDirectIdClass_noDiagnostics() {
        String entityCode = """
                package com.example.model;

                import jakarta.persistence.Entity;
                import jakarta.persistence.IdClass;

                @Entity
                @IdClass(CompositeKey.class)
                public class RecordEntity {
                    private String keyA;
                    private String keyB;
                }
                """;

        List<JavaDiagnostics.Diag> diags = JpaDiagnostics.checkPersistentEntities(
                Path.of("RecordEntity.java"), entityCode, List.of(), "demo");
        assertTrue(diags.isEmpty());
    }

    @Test
    void testInheritedId_fromBaseEntityInDifferentPackage() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Path commonDir = srcDir.resolve("com/roze/nexacommerce/common");
        Path customerDir = srcDir.resolve("com/roze/nexacommerce/customer/entity");
        Files.createDirectories(commonDir);
        Files.createDirectories(customerDir);

        // 1. BaseEntity with @Id
        String baseEntityCode = """
                package com.roze.nexacommerce.common;

                import jakarta.persistence.MappedSuperclass;
                import jakarta.persistence.Id;
                import jakarta.persistence.GeneratedValue;
                import jakarta.persistence.GenerationType;

                @MappedSuperclass
                public class BaseEntity {
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    private Long id;
                }
                """;
        Path baseEntityFile = commonDir.resolve("BaseEntity.java");
        Files.writeString(baseEntityFile, baseEntityCode);

        // 2. CustomerProfile extending BaseEntity
        String customerProfileCode = """
                package com.roze.nexacommerce.customer.entity;

                import com.roze.nexacommerce.common.BaseEntity;
                import jakarta.persistence.Entity;
                import jakarta.persistence.Table;

                @Entity
                @Table(name = "customers")
                public class CustomerProfile extends BaseEntity {
                    private String username;
                }
                """;
        Path customerProfileFile = customerDir.resolve("CustomerProfile.java");
        Files.writeString(customerProfileFile, customerProfileCode);

        List<JavaDiagnostics.Diag> diags = JpaDiagnostics.checkPersistentEntities(
                customerProfileFile, customerProfileCode, List.of(srcDir), "NexaCommerce");

        assertTrue(diags.isEmpty(), "Child entity inheriting @Id from BaseEntity should not have missing primary key error");
    }

    @Test
    void testInheritedId_multiLevelInheritance() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Path modelDir = srcDir.resolve("com/example/model");
        Files.createDirectories(modelDir);

        // BaseEntity
        Files.writeString(modelDir.resolve("RootEntity.java"), """
                package com.example.model;
                import jakarta.persistence.Id;
                public abstract class RootEntity {
                    @Id
                    private Long id;
                }
                """);

        // MiddleEntity
        Files.writeString(modelDir.resolve("AuditedEntity.java"), """
                package com.example.model;
                public abstract class AuditedEntity extends RootEntity {
                    private String createdBy;
                }
                """);

        // LeafEntity
        String leafCode = """
                package com.example.model;
                import jakarta.persistence.Entity;
                @Entity
                public class Product extends AuditedEntity {
                    private String name;
                }
                """;
        Path leafFile = modelDir.resolve("Product.java");
        Files.writeString(leafFile, leafCode);

        List<JavaDiagnostics.Diag> diags = JpaDiagnostics.checkPersistentEntities(
                leafFile, leafCode, List.of(srcDir), "demo");

        assertTrue(diags.isEmpty(), "Multi-level inheritance should resolve @Id from root entity");
    }

    @Test
    void testMissingId_emitsError() {
        String entityWithoutId = """
                package com.example.model;

                import jakarta.persistence.Entity;

                @Entity
                public class Student {
                    private String name;
                }
                """;

        List<JavaDiagnostics.Diag> diags = JpaDiagnostics.checkPersistentEntities(
                Path.of("Student.java"), entityWithoutId, List.of(), "demo");

        assertEquals(1, diags.size());
        JavaDiagnostics.Diag d = diags.get(0);
        assertEquals(JavaDiagnostics.Severity.ERROR, d.severity());
        assertEquals("Persistent entity 'Student' should have primary key", d.title());
        assertEquals("Persistent entity should have primary key", d.message());
        assertEquals("add-id-attribute:Student", d.quickFix());
    }

    @Test
    void testMissingId_superclassHasNoId_emitsError() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Path modelDir = srcDir.resolve("com/example/model");
        Files.createDirectories(modelDir);

        Files.writeString(modelDir.resolve("PlainBase.java"), """
                package com.example.model;
                public class PlainBase {
                    private String note;
                }
                """);

        String childCode = """
                package com.example.model;
                import jakarta.persistence.Entity;
                @Entity
                public class InvalidChild extends PlainBase {
                    private String title;
                }
                """;
        Path childFile = modelDir.resolve("InvalidChild.java");
        Files.writeString(childFile, childCode);

        List<JavaDiagnostics.Diag> diags = JpaDiagnostics.checkPersistentEntities(
                childFile, childCode, List.of(srcDir), "demo");

        assertEquals(1, diags.size(), "Should report error when superclass has no primary key");
        assertEquals("Persistent entity 'InvalidChild' should have primary key", diags.get(0).title());
    }

    @Test
    void testNonEntity_ignored() {
        String plainClass = """
                package com.example.dto;

                public class UserDto {
                    private Long id;
                    private String name;
                }
                """;

        List<JavaDiagnostics.Diag> diags = JpaDiagnostics.checkPersistentEntities(
                Path.of("UserDto.java"), plainClass, List.of(), "demo");
        assertTrue(diags.isEmpty());
    }
}
