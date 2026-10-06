package dev.lumina.filetypes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileTypeManagerTest {

    private FileTypeManager manager;

    @BeforeEach
    void setUp() {
        manager = FileTypeManager.getInstance();
    }

    @Test
    void testCatalogInitialization() {
        assertNotNull(manager.getFileTypes());
        assertTrue(manager.getFileTypes().size() >= 40, "Should have 40+ recognized file types");

        assertNotNull(manager.findFileTypeByName("Java"));
        assertNotNull(manager.findFileTypeByName("Python"));
        assertNotNull(manager.findFileTypeByName(".dockerignore (DockerIgnore)"));
        assertNotNull(manager.findFileTypeByName(".gitignore (Gitignore)"));
        assertNotNull(manager.findFileTypeByName("Angular HTML Template"));
        assertNotNull(manager.findFileTypeByName("Cascading Style Sheet"));
        assertNotNull(manager.findFileTypeByName("C#"));
        assertNotNull(manager.findFileTypeByName("C/C++"));
        assertNotNull(manager.findFileTypeByName("Shell Script"));
    }

    @Test
    void testResolveByFileName() {
        FileType java = manager.findFileTypeByFileName("Main.java");
        assertNotNull(java);
        assertEquals("Java", java.getName());

        FileType py = manager.findFileTypeByFileName("/path/to/script.py");
        assertNotNull(py);
        assertEquals("Python", py.getName());

        FileType dockerfile = manager.findFileTypeByFileName("Dockerfile.prod");
        assertNotNull(dockerfile);
        assertEquals("Dockerfile", dockerfile.getName());

        FileType gitignore = manager.findFileTypeByFileName(".gitignore");
        assertNotNull(gitignore);
        assertEquals(".gitignore (Gitignore)", gitignore.getName());

        FileType css = manager.findFileTypeByFileName("app/styles/theme.scss");
        assertNotNull(css);
        assertEquals("Cascading Style Sheet", css.getName());

        FileType csharp = manager.findFileTypeByFileName("Program.cs");
        assertNotNull(csharp);
        assertEquals("C#", csharp.getName());

        FileType cpp = manager.findFileTypeByFileName("native/engine.cpp");
        assertNotNull(cpp);
        assertEquals("C/C++", cpp.getName());
    }

    @Test
    void testResolveByHashbang() {
        FileType sh = manager.findFileTypeByHashbang("#!/bin/bash");
        assertNotNull(sh);
        assertEquals("Shell Script", sh.getName());

        FileType py = manager.findFileTypeByHashbang("#!/usr/bin/env python3");
        assertNotNull(py);
        assertEquals("Python", py.getName());

        FileType ruby = manager.findFileTypeByHashbang("#!/usr/bin/ruby");
        assertNotNull(ruby);
        assertEquals("Ruby", ruby.getName());

        assertNull(manager.findFileTypeByHashbang("echo hello"));
    }

    @Test
    void testIgnoredPatterns() {
        assertTrue(manager.isIgnored(".git"));
        assertTrue(manager.isIgnored("test.pyc"));
        assertTrue(manager.isIgnored(".DS_Store"));
        assertTrue(manager.isIgnored("__pycache__"));
        assertTrue(manager.isIgnored("dump.hprof"));

        assertFalse(manager.isIgnored("Main.java"));
        assertFalse(manager.isIgnored("README.md"));
        assertFalse(manager.isIgnored("pom.xml"));
    }

    @Test
    void testUserDefinedFileTypeLifecycle() {
        FileType custom = new FileType("MyCustomLang", "Custom language file", "generic", false,
                List.of("*.mylang", "*.ml"), List.of("mylang"));
        custom.setLineComment("--");
        custom.setKeywordsGroup1("let def fn return if else");

        manager.registerFileType(custom);

        FileType found = manager.findFileTypeByName("MyCustomLang");
        assertNotNull(found);
        assertEquals("MyCustomLang", found.getName());
        assertEquals("--", found.getLineComment());
        assertTrue(found.getPatterns().contains("*.mylang"));

        // Match filename
        FileType resolved = manager.findFileTypeByFileName("test.mylang");
        assertNotNull(resolved);
        assertEquals("MyCustomLang", resolved.getName());

        // Clone and equivalence check
        FileType clone = custom.copy();
        assertTrue(custom.isEquivalentTo(clone));
        clone.setDescription("Updated desc");
        assertFalse(custom.isEquivalentTo(clone));

        // Removal
        boolean removed = manager.removeFileType("MyCustomLang");
        assertTrue(removed);
        assertNull(manager.findFileTypeByName("MyCustomLang"));
    }

    @Test
    void testWildcardMatchingLogic() {
        assertTrue(FileTypeManager.matchesWildcard("test.java", "*.java"));
        assertTrue(FileTypeManager.matchesWildcard("Dockerfile.dev", "Dockerfile*"));
        assertTrue(FileTypeManager.matchesWildcard(".dockerignore", ".dockerignore"));
        assertTrue(FileTypeManager.matchesWildcard("file1.txt", "file?.txt"));
        assertFalse(FileTypeManager.matchesWildcard("file12.txt", "file?.txt"));
        assertFalse(FileTypeManager.matchesWildcard("test.kt", "*.java"));
    }
}
