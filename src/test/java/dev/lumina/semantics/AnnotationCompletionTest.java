package dev.lumina.semantics;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationCompletionTest {

    @Test
    void testIsAnnotationClass() throws Exception {
        byte[] dummyClass = new byte[]{
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                0, 0, 0, 65, // Java 21
                0, 1,        // constant pool count = 1 (empty)
                0, 0x20      // access flags: ACC_SUPER (0x0020), not annotation
        };
        assertFalse(SemanticEngine.isAnnotationClass(dummyClass));

        byte[] dummyAnnotation = new byte[]{
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                0, 0, 0, 65, // Java 21
                0, 1,        // constant pool count = 1 (empty)
                0x22, 0      // access flags: ACC_ANNOTATION (0x2000) | ACC_INTERFACE (0x0200)
        };
        assertTrue(SemanticEngine.isAnnotationClass(dummyAnnotation));
    }

    @Test
    void testContextAtAnnotation() {
        String code1 = "@";
        Completion.Context ctx1 = Completion.contextAt(code1, 1);
        assertNotNull(ctx1);
        assertTrue(ctx1.annotation());
        assertEquals("", ctx1.prefix());
        assertEquals(1, ctx1.prefixStart());

        String code2 = "package com.example;\n\n@Ent";
        Completion.Context ctx2 = Completion.contextAt(code2, code2.length());
        assertNotNull(ctx2);
        assertTrue(ctx2.annotation());
        assertEquals("Ent", ctx2.prefix());
        assertEquals(code2.length() - 3, ctx2.prefixStart());

        String code3 = "@Entity";
        Completion.Context ctx3 = Completion.contextAt(code3, code3.length());
        assertNotNull(ctx3);
        assertTrue(ctx3.annotation());
        assertEquals("Entity", ctx3.prefix());
        assertEquals(1, ctx3.prefixStart());

        // Regular identifier should not be annotation
        String code4 = "Entity";
        Completion.Context ctx4 = Completion.contextAt(code4, code4.length());
        assertNotNull(ctx4);
        assertFalse(ctx4.annotation());
        assertEquals("Entity", ctx4.prefix());
    }

    @Test
    void testAnnotationAutoImport() {
        String source = "package com.example;\n\npublic class Student {\n}\n";
        assertTrue(Completion.needsImport(source, "jakarta.persistence.Entity"));
        assertFalse(Completion.needsImport(source, "java.lang.Override"));

        String sourceWithImport = "package com.example;\n\nimport jakarta.persistence.Entity;\n\npublic class Student {\n}\n";
        assertFalse(Completion.needsImport(sourceWithImport, "jakarta.persistence.Entity"));

        int offset = Completion.importInsertOffset(source);
        assertTrue(offset > 0);
        String updated = source.substring(0, offset) + "import jakarta.persistence.Entity;\n" + source.substring(offset);
        assertTrue(updated.contains("import jakarta.persistence.Entity;"));
    }

    @Test
    void testFallbackAnnotationCompletions() {
        List<Completion.Item> items = SemanticEngine.fallbackAnnotationCompletions("");
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(it -> it.name().equals("Override")));
        assertTrue(items.stream().anyMatch(it -> it.name().equals("Deprecated")));

        List<Completion.Item> filtered = SemanticEngine.fallbackAnnotationCompletions("Over");
        assertEquals(1, filtered.size());
        assertEquals("Override", filtered.get(0).name());
        assertEquals(Completion.Kind.ANNOTATION, filtered.get(0).kind());
        assertEquals("(java.lang)", filtered.get(0).detail());
    }

    @Test
    void testAnnotationCompletionsWithDynamicDependencies() {
        // Create an engine for the current project
        SemanticEngine engine = SemanticEngine.create(Path.of("."), null, null);
        assertNotNull(engine);

        // Empty prefix provides popular annotations
        List<Completion.Item> emptyItems = engine.annotationCompletions("");
        assertFalse(emptyItems.isEmpty());
        assertTrue(emptyItems.stream().anyMatch(it -> it.name().equals("Target")));
        assertTrue(emptyItems.stream().anyMatch(it -> it.name().equals("Override")));

        // Search for "Over"
        List<Completion.Item> overItems = engine.annotationCompletions("Over");
        assertFalse(overItems.isEmpty());
        assertEquals("Override", overItems.get(0).name());
        assertEquals("(java.lang)", overItems.get(0).detail());
        assertEquals("java.lang.Override", overItems.get(0).importFqcn());

        // Dynamic dependency behavior:
        // By default without JPA dependency in classpath, jakarta.persistence.Entity is not present
        List<Completion.Item> entItems = engine.annotationCompletions("Ent");
        boolean hasJpaEntity = entItems.stream().anyMatch(it -> "jakarta.persistence.Entity".equals(it.importFqcn()));
        // In this project (lumina-ide itself), jakarta.persistence is not a dependency!
        assertFalse(hasJpaEntity, "jakarta.persistence.Entity must not appear when JPA dependency is absent");

        // Now simulate classpath where JPA dependency is added to annotationIndex
        engine.annotationIndex().put("Entity", List.of("jakarta.persistence.Entity"));
        engine.annotationIndex().put("EntityListeners", List.of("jakarta.persistence.EntityListeners"));

        List<Completion.Item> jpaEntItems = engine.annotationCompletions("Ent");
        assertFalse(jpaEntItems.isEmpty());
        assertEquals("Entity", jpaEntItems.get(0).name(), "Exact prefix match Entity must be ranked first");
        assertEquals("(jakarta.persistence)", jpaEntItems.get(0).detail());
        assertEquals("jakarta.persistence.Entity", jpaEntItems.get(0).importFqcn());
        assertTrue(jpaEntItems.stream().anyMatch(it -> it.name().equals("EntityListeners")));
    }
}
