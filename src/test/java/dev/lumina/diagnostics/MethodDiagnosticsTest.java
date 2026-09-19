package dev.lumina.diagnostics;

import dev.lumina.codegen.JavaCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MethodDiagnosticsTest {

    @Test
    void testDetectsUnusedMethodsInRepository() throws IOException {
        Path tempDir = Path.of("target/test-repo-temp");
        Files.createDirectories(tempDir);
        String repoCode = """
                package com.roze.nexacommerce.user.repository;

                import org.springframework.data.jpa.repository.JpaRepository;
                import org.springframework.data.jpa.repository.Query;
                import org.springframework.data.repository.query.Param;
                import org.springframework.stereotype.Repository;
                import org.springframework.data.domain.Page;
                import org.springframework.data.domain.Pageable;
                import java.util.Collection;
                import java.util.List;
                import java.util.Optional;

                @Repository
                public interface RoleRepository extends JpaRepository<Role, Long> {
                    boolean existsByName(String superadmin);

                    Optional<Role> findByName(String name);

                    @Query("SELECT r FROM Role r WHERE r.name LIKE %:searchTerm% OR r.description LIKE %:searchTerm%")
                    Page<Role> searchRoles(
                            @Param("searchTerm") String searchTerm,
                            Pageable pageable
                    );

                    List<Role> findByPermissionName(String permissionName);

                    List<Role> findByNames(Collection<String> names);
                }
                """;

        String serviceCode = """
                package com.roze.nexacommerce.user.service;

                import com.roze.nexacommerce.user.repository.RoleRepository;
                import org.springframework.stereotype.Service;

                @Service
                public class RoleService {
                    private final RoleRepository roleRepository;

                    public RoleService(RoleRepository roleRepository) {
                        this.roleRepository = roleRepository;
                    }

                    public void findRole(String name) {
                        roleRepository.findByName(name);
                    }
                }
                """;

        Path repoFile = tempDir.resolve("RoleRepository.java");
        Files.writeString(repoFile, repoCode);

        Path serviceFile = tempDir.resolve("RoleService.java");
        Files.writeString(serviceFile, serviceCode);

        List<JavaDiagnostics.Diag> diags = MethodDiagnostics.checkUnusedMethods(
                repoFile, repoCode, List.of(tempDir), "NexaCommerce");

        assertEquals(4, diags.size(), "Should find 4 unused methods (existsByName, searchRoles, findByPermissionName, findByNames)");

        // 1. existsByName
        JavaDiagnostics.Diag d1 = diags.stream()
                .filter(d -> d.quickFix().startsWith("unused-method:existsByName"))
                .findFirst().orElse(null);
        assertNotNull(d1);
        assertEquals("Method 'existsByName(java.lang.String)' is never used", d1.title());
        assertEquals("unused-method:existsByName(String)", d1.quickFix());
        assertEquals("com.roze.nexacommerce.user.repository.RoleRepository", d1.context());
        assertEquals("NexaCommerce", d1.origin());
        assertEquals(JavaDiagnostics.Severity.WARNING, d1.severity());

        // 2. searchRoles
        JavaDiagnostics.Diag d2 = diags.stream()
                .filter(d -> d.quickFix().startsWith("unused-method:searchRoles"))
                .findFirst().orElse(null);
        assertNotNull(d2);
        assertEquals("Method 'searchRoles(java.lang.String, org.springframework.data.domain.Pageable)' is never used", d2.title());
        assertEquals("unused-method:searchRoles(String, Pageable)", d2.quickFix());

        // 3. findByName should NOT be in diagnostics (it is called in RoleService)
        boolean hasFindByName = diags.stream().anyMatch(d -> d.quickFix().startsWith("unused-method:findByName("));
        assertFalse(hasFindByName, "findByName has usages and must not be flagged as unused");

        // 4. findByPermissionName and findByNames
        assertTrue(diags.stream().anyMatch(d -> d.quickFix().startsWith("unused-method:findByPermissionName")));
        assertTrue(diags.stream().anyMatch(d -> d.quickFix().startsWith("unused-method:findByNames")));
    }

    @Test
    void testExcludesEntryPointsAndOverrides() throws IOException {
        Path tempDir = Path.of("target/test-entry-temp");
        Files.createDirectories(tempDir);
        String code = """
                package com.example.app;

                import org.junit.jupiter.api.Test;
                import org.springframework.context.annotation.Bean;

                public class AppService {
                    public static void main(String[] args) {
                    }

                    @Override
                    public String toString() {
                        return "app";
                    }

                    @Test
                    void someTest() {
                    }

                    @Bean
                    public String myBean() {
                        return "bean";
                    }

                    public void trulyUnusedMethod() {
                    }
                }
                """;

        Path appFile = tempDir.resolve("AppService.java");
        Files.writeString(appFile, code);

        List<JavaDiagnostics.Diag> diags = MethodDiagnostics.checkUnusedMethods(
                appFile, code, List.of(tempDir), "MyApp");

        assertEquals(1, diags.size(), "Only trulyUnusedMethod should be flagged");
        assertEquals("unused-method:trulyUnusedMethod()", diags.get(0).quickFix());
    }

    @Test
    void testDetectsUnusedPrivateMethodInClass() {
        String code = """
                package com.example.app;

                public class InternalWorker {
                    public void doWork() {
                        helperUsed();
                    }

                    private void helperUsed() {
                    }

                    private void helperUnused() {
                    }
                }
                """;

        List<JavaDiagnostics.Diag> diags = MethodDiagnostics.checkUnusedMethods(
                Path.of("InternalWorker.java"), code, List.of(), "MyApp");

        // doWork has 0 usages outside, helperUnused has 0 usages, helperUsed is called by doWork
        assertTrue(diags.stream().anyMatch(d -> d.quickFix().startsWith("unused-method:helperUnused")));
        assertFalse(diags.stream().anyMatch(d -> d.quickFix().startsWith("unused-method:helperUsed")));
    }

    @Test
    void testSafeDeleteMethod() {
        String code = """
                package com.roze.nexacommerce.user.repository;

                import org.springframework.stereotype.Repository;

                @Repository
                public interface RoleRepository {
                    boolean existsByName(String superadmin);

                    @Query("SELECT r FROM Role r")
                    Page<Role> searchRoles(String searchTerm);

                    List<Role> findByNames(Collection<String> names);
                }
                """;

        String updated = JavaCodeGenerator.removeMethod(code, "searchRoles");
        assertFalse(updated.contains("searchRoles"), "searchRoles should be removed");
        assertFalse(updated.contains("@Query(\"SELECT r FROM Role r\")"), "Attached @Query annotation should be removed");
        assertTrue(updated.contains("existsByName"), "existsByName should remain");
        assertTrue(updated.contains("findByNames"), "findByNames should remain");
    }
}
