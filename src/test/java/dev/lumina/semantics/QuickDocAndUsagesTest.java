package dev.lumina.semantics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuickDocAndUsagesTest {

    @Test
    void testSymbolDocFormattedSignature() {
        Docs.SymbolDoc docNoParams = new Docs.SymbolDoc(
                "method", "com.example.Repo", "void", "clear",
                List.of(), "demo", null, null, 1
        );
        assertEquals("void clear()", docNoParams.formattedSignature());

        Docs.SymbolDoc docSingleParam = new Docs.SymbolDoc(
                "interface", "com.roze.nexacommerce.user.repository.RoleRepository",
                "Optional<Role>", "findByName", List.of("String superadmin"),
                "NexaCommerce", null, null, 17
        );
        String expected = "Optional<Role> findByName(\n    String superadmin\n)";
        assertEquals(expected, docSingleParam.formattedSignature());

        Docs.SymbolDoc docMultiParams = new Docs.SymbolDoc(
                "class", "com.roze.Service", "Role", "updateRole",
                List.of("Long id", "String newName"), "demo", null, null, 25
        );
        String expectedMulti = "Role updateRole(\n    Long id,\n    String newName\n)";
        assertEquals(expectedMulti, docMultiParams.formattedSignature());
    }

    @Test
    void testSymbolDocAtMethodDeclaration(@TempDir Path tempDir) throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/roze/nexacommerce/user/repository");
        Files.createDirectories(srcDir);
        Path repoFile = srcDir.resolve("RoleRepository.java");

        String code = """
                package com.roze.nexacommerce.user.repository;
                
                import java.util.Optional;
                
                public interface RoleRepository {
                    /**
                     * Find role by its unique name.
                     * @param superadmin the role name
                     * @return role if present
                     */
                    Optional<Role> findByName(String superadmin);
                }
                """;
        Files.writeString(repoFile, code);

        SemanticEngine engine = SemanticEngine.create(tempDir, null, null);
        // Find line of findByName
        int line = -1;
        int col = -1;
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("findByName")) {
                line = i + 1;
                col = lines[i].indexOf("findByName") + 1;
                break;
            }
        }
        assertTrue(line > 0 && col > 0);

        Docs.SymbolDoc doc = engine.symbolDocAt(repoFile, code, line, col);
        assertNotNull(doc, "SymbolDoc should not be null for method declaration");
        assertEquals("interface", doc.kind());
        assertEquals("com.roze.nexacommerce.user.repository.RoleRepository", doc.containerFqcn());
        assertEquals("findByName", doc.name());
        assertEquals("Optional<Role>", doc.returnType());
        assertNotNull(doc.params());
        assertEquals(1, doc.params().size());
        assertEquals("String superadmin", doc.params().get(0));
        assertNotNull(doc.javadoc());
        assertTrue(doc.javadoc().contains("Find role by its unique name"));
    }

    @Test
    void testSymbolDocAtCallSiteAndUsages(@TempDir Path tempDir) throws IOException {
        Path repoDir = tempDir.resolve("src/main/java/com/roze/nexacommerce/user/repository");
        Path serviceDir = tempDir.resolve("src/main/java/com/roze/nexacommerce/user/service/impl");
        Files.createDirectories(repoDir);
        Files.createDirectories(serviceDir);

        Path repoFile = repoDir.resolve("RoleRepository.java");
        String repoCode = """
                package com.roze.nexacommerce.user.repository;
                
                import java.util.Optional;
                
                public interface RoleRepository {
                    Optional<Role> findByName(String superadmin);
                }
                """;
        Files.writeString(repoFile, repoCode);

        Path serviceFile = serviceDir.resolve("CustomerServiceImpl.java");
        String serviceCode = """
                package com.roze.nexacommerce.user.service.impl;
                
                import com.roze.nexacommerce.user.repository.RoleRepository;
                
                public class CustomerServiceImpl {
                    private RoleRepository roleRepository;
                
                    public void register() {
                        var customerRole = roleRepository.findByName("CUSTOMER");
                    }
                }
                """;
        Files.writeString(serviceFile, serviceCode);

        SemanticEngine engine = SemanticEngine.create(tempDir, null, null);

        // Test symbolDocAt call site in CustomerServiceImpl
        int callLine = -1;
        int callCol = -1;
        String[] sLines = serviceCode.split("\n");
        for (int i = 0; i < sLines.length; i++) {
            if (sLines[i].contains("findByName")) {
                callLine = i + 1;
                callCol = sLines[i].indexOf("findByName") + 1;
                break;
            }
        }
        assertTrue(callLine > 0 && callCol > 0);

        Docs.SymbolDoc doc = engine.symbolDocAt(serviceFile, serviceCode, callLine, callCol);
        assertNotNull(doc, "SymbolDoc should not be null for method call site");
        assertEquals("com.roze.nexacommerce.user.repository.RoleRepository", doc.containerFqcn());
        assertEquals("findByName", doc.name());
        assertEquals("Optional<Role>", doc.returnType());
        assertNotNull(doc.params());
        assertEquals(1, doc.params().size());
        assertEquals("String superadmin", doc.params().get(0));

        // Test findUsages from declaration in RoleRepository
        int declLine = -1;
        int declCol = -1;
        String[] rLines = repoCode.split("\n");
        for (int i = 0; i < rLines.length; i++) {
            if (rLines[i].contains("findByName")) {
                declLine = i + 1;
                declCol = rLines[i].indexOf("findByName") + 1;
                break;
            }
        }
        List<SemanticEngine.Usage> usages = engine.findUsages(repoFile, repoCode, declLine, declCol);
        assertNotNull(usages);
        assertFalse(usages.isEmpty(), "Should find usages across project");
        // Must contain declaration in repoFile and call site in serviceFile
        assertTrue(usages.stream().anyMatch(u -> u.file().equals(repoFile) && u.declaration()));
        assertTrue(usages.stream().anyMatch(u -> u.file().equals(serviceFile) && !u.declaration()));
    }
}
