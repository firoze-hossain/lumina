package dev.lumina.semantics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LibrarySourceServiceTest {

    @TempDir
    Path tempDir;

    private LibrarySourceService service;

    @BeforeEach
    void setUp() {
        service = new LibrarySourceService(tempDir.resolve("cache/sources"));
    }

    @AfterEach
    void tearDown() {
        service.clearCache();
    }

    @Test
    void testFindMemberLine_exactMethod() {
        String code = """
                package org.springframework.data.repository;

                /**
                 * Javadoc comment for repository interface.
                 * Has save( mentioned in comments.
                 */
                public interface CrudRepository<T, ID> {

                    /**
                     * Saves a given entity.
                     *
                     * @param entity must not be null.
                     * @return the saved entity.
                     */
                    @Deprecated
                    <S extends T> S save(S entity);

                    <S extends T> Iterable<S> saveAll(Iterable<S> entities);
                }
                """;

        int saveLine = LibrarySourceService.findMemberLine(code, "save", 1);
        assertEquals(16, saveLine);

        int saveAllLine = LibrarySourceService.findMemberLine(code, "saveAll", 1);
        assertEquals(18, saveAllLine);
    }

    @Test
    void testFindMemberLine_overloadsByParamCount() {
        String code = """
                package com.example;

                public class Service {
                    public void execute() {}
                    public void execute(String param) {}
                    public void execute(String a, int b) {}
                }
                """;

        assertEquals(4, LibrarySourceService.findMemberLine(code, "execute", 0));
        assertEquals(5, LibrarySourceService.findMemberLine(code, "execute", 1));
        assertEquals(6, LibrarySourceService.findMemberLine(code, "execute", 2));
    }

    @Test
    void testFindMemberLine_classAndField() {
        String code = """
                package com.example;

                public class MyEntity {
                    private Long id;
                    private String username;

                    public MyEntity() {}
                }
                """;

        assertEquals(3, LibrarySourceService.findMemberLine(code, "MyEntity", -2));
        assertEquals(4, LibrarySourceService.findMemberLine(code, "id", -1));
        assertEquals(5, LibrarySourceService.findMemberLine(code, "username", -1));
        assertEquals(7, LibrarySourceService.findMemberLine(code, "MyEntity", 0));
    }

    @Test
    void testPersistentCaching_instantRetrieval() throws IOException {
        String fqcn = "org.springframework.data.repository.CrudRepository";
        Path cachedFile = service.getCachedSourcePath(fqcn);

        assertFalse(service.isCached(fqcn));

        // Manually populate cache file as if resolved
        Files.createDirectories(cachedFile.getParent());
        Files.writeString(cachedFile, "public interface CrudRepository {}");

        assertTrue(service.isCached(fqcn));
        assertTrue(service.isLibraryCacheFile(cachedFile));

        // When retrieving, statusLogger should NOT be called because it is cached
        List<String> logMessages = new ArrayList<>();
        Path result = service.getOrResolveSource(fqcn, null, List.of(), null, logMessages::add);

        assertEquals(cachedFile, result);
        assertTrue(logMessages.isEmpty(), "No resolve logs should be emitted for cached sources");
    }

    @Test
    void testCacheInvalidation() throws IOException {
        String fqcn = "com.example.CachedLib";
        Path cachedFile = service.getCachedSourcePath(fqcn);
        Files.createDirectories(cachedFile.getParent());
        Files.writeString(cachedFile, "public class CachedLib {}");

        assertTrue(service.isCached(fqcn));

        service.clearCache();

        assertFalse(service.isCached(fqcn));
        assertFalse(Files.exists(cachedFile));
    }

    @Test
    void testTopLevelFqcn() {
        assertEquals("java.util.Map", LibrarySourceService.getTopLevelFqcn("java.util.Map.Entry"));
        assertEquals("java.util.Map", LibrarySourceService.getTopLevelFqcn("java.util.Map$Entry"));
        assertEquals("org.springframework.data.repository.CrudRepository",
                LibrarySourceService.getTopLevelFqcn("org.springframework.data.repository.CrudRepository"));
    }

    @Test
    void testFindJdkSource_ifAvailable() {
        String javaSource = service.findJdkSource("java.util.List");
        if (javaSource != null) {
            assertTrue(javaSource.contains("interface List") || javaSource.contains("public interface List"));
            int addLine = LibrarySourceService.findMemberLine(javaSource, "add", 1);
            assertTrue(addLine > 1, "Line for add(E e) should be found in List.java");
        }
    }
}
