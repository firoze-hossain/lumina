package dev.lumina.diagnostics;

import dev.lumina.semantics.Completion;
import dev.lumina.semantics.LombokSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JavaDiagnosticsLombokTest {

    @TempDir
    Path tempDir;

    @Test
    public void testLombokSymbolResolutionAndDiagnosticsSuppression() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir.resolve("com/example/dto"));
        Files.createDirectories(srcDir.resolve("com/example/entity"));
        Files.createDirectories(srcDir.resolve("com/example/service"));

        // 1. Create CustomerRegistrationRequest.java with Lombok annotations
        Path dtoFile = srcDir.resolve("com/example/dto/CustomerRegistrationRequest.java");
        Files.writeString(dtoFile, """
                package com.example.dto;

                import lombok.AllArgsConstructor;
                import lombok.Builder;
                import lombok.Data;
                import lombok.NoArgsConstructor;

                @Data
                @Builder
                @NoArgsConstructor
                @AllArgsConstructor
                public class CustomerRegistrationRequest {
                    private String fullName;
                    private String email;
                    private String password;
                    private String phone;
                    private Boolean newsletterSubscribed;
                }
                """);

        // 2. Create User.java with Lombok annotations
        Path userFile = srcDir.resolve("com/example/entity/User.java");
        Files.writeString(userFile, """
                package com.example.entity;

                import lombok.AllArgsConstructor;
                import lombok.Builder;
                import lombok.Data;
                import lombok.NoArgsConstructor;

                @Data
                @AllArgsConstructor
                @NoArgsConstructor
                @Builder
                public class User {
                    private String name;
                    private String email;
                    private String password;
                    private String phone;
                    private Boolean active;
                }
                """);

        // 3. Service code using Lombok methods (like NexaCommerce CustomerServiceImpl)
        String serviceCode = """
                package com.example.service;

                import com.example.dto.CustomerRegistrationRequest;
                import com.example.entity.User;
                import lombok.RequiredArgsConstructor;

                @RequiredArgsConstructor
                public class CustomerServiceImpl {
                    public void register(CustomerRegistrationRequest request) {
                        String email = request.getEmail();
                        String name = request.getFullName();
                        String phone = request.getPhone();
                        Boolean sub = request.getNewsletterSubscribed();
                        Boolean subIs = request.isNewsletterSubscribed();

                        User user = User.builder()
                                .name(request.getFullName())
                                .email(request.getEmail())
                                .phone(request.getPhone())
                                .active(true)
                                .build();
                    }
                }
                """;

        Path serviceFile = srcDir.resolve("com/example/service/CustomerServiceImpl.java");

        // Compile and verify diagnostics: Should have 0 errors!
        List<JavaDiagnostics.Diag> diags = JavaDiagnostics.compile(
                serviceFile, serviceCode, "", List.of(srcDir));

        long errorCount = diags.stream()
                .filter(d -> d.severity() == JavaDiagnostics.Severity.ERROR)
                .count();

        assertEquals(0, errorCount,
                "Expected 0 errors on Lombok-generated getters, setters, and builder calls, but got: "
                        + diags.stream().map(d -> d.title() + " at line " + d.line()).toList());
    }

    @Test
    public void testNonExistentMethodStillReportsErrorAccurately() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir.resolve("com/example/dto"));
        Files.createDirectories(srcDir.resolve("com/example/service"));

        Path dtoFile = srcDir.resolve("com/example/dto/CustomerRegistrationRequest.java");
        Files.writeString(dtoFile, """
                package com.example.dto;

                import lombok.Data;

                @Data
                public class CustomerRegistrationRequest {
                    private String email;
                }
                """);

        String serviceCode = """
                package com.example.service;

                import com.example.dto.CustomerRegistrationRequest;

                public class CustomerServiceImpl {
                    public void test(CustomerRegistrationRequest request) {
                        request.getNonExistentMethod();
                    }
                }
                """;

        Path serviceFile = srcDir.resolve("com/example/service/CustomerServiceImpl.java");

        List<JavaDiagnostics.Diag> diags = JavaDiagnostics.compile(
                serviceFile, serviceCode, "", List.of(srcDir));

        List<JavaDiagnostics.Diag> errors = diags.stream()
                .filter(d -> d.severity() == JavaDiagnostics.Severity.ERROR)
                .toList();

        assertFalse(errors.isEmpty(), "Should report error for genuinely non-existent method");
        JavaDiagnostics.Diag err = errors.get(0);
        assertTrue(err.title().contains("getNonExistentMethod"),
                "Error title should mention getNonExistentMethod: " + err.title());
        assertNull(err.quickFix(),
                "Should not suggest import-class for a method call");
    }

    @Test
    public void testLombokSlf4jLoggerSuppression() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir.resolve("com/example/service"));

        String serviceCode = """
                package com.example.service;

                import lombok.extern.slf4j.Slf4j;

                @Slf4j
                public class LoggingService {
                    public void doWork() {
                        log.toString();
                    }
                }
                """;

        Path serviceFile = srcDir.resolve("com/example/service/LoggingService.java");

        List<JavaDiagnostics.Diag> diags = JavaDiagnostics.compile(
                serviceFile, serviceCode, "", List.of(srcDir));

        long errorCount = diags.stream()
                .filter(d -> d.severity() == JavaDiagnostics.Severity.ERROR)
                .count();

        assertEquals(0, errorCount, "Expected log variable to be recognized via @Slf4j");
    }

    @Test
    public void testLombokNavigationResolution() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir.resolve("com/example/dto"));

        Path dtoFile = srcDir.resolve("com/example/dto/UserDTO.java");
        Files.writeString(dtoFile, """
                package com.example.dto;

                import lombok.Builder;
                import lombok.Data;

                @Data
                @Builder
                public class UserDTO {
                    private String email;
                }
                """);

        String serviceCode = """
                package com.example.service;
                import com.example.dto.UserDTO;
                public class Test {
                    void m(UserDTO u) {
                        u.getEmail();
                    }
                }
                """;

        // Resolve getEmail -> should locate email field in UserDTO.java
        LombokSupport.Location loc = LombokSupport.resolveLombokDeclaration(
                "UserDTO", "getEmail", List.of(srcDir), serviceCode);

        assertNotNull(loc, "Should resolve getEmail location");
        assertEquals(dtoFile, loc.file());
        assertTrue(loc.line() > 0, "Should point to line of email field");

        // Resolve builder -> should locate @Builder annotation in UserDTO.java
        LombokSupport.Location builderLoc = LombokSupport.resolveLombokDeclaration(
                "UserDTO", "builder", List.of(srcDir), serviceCode);

        assertNotNull(builderLoc, "Should resolve builder location");
        assertEquals(dtoFile, builderLoc.file());
    }

    @Test
    public void testLombokSuperclassInheritance() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir.resolve("com/example/model"));

        Path baseFile = srcDir.resolve("com/example/model/BaseEntity.java");
        Files.writeString(baseFile, """
                package com.example.model;

                import lombok.Data;

                @Data
                public class BaseEntity {
                    private Long id;
                }
                """);

        Path subFile = srcDir.resolve("com/example/model/User.java");
        Files.writeString(subFile, """
                package com.example.model;

                import lombok.Data;

                @Data
                public class User extends BaseEntity {
                    private String username;
                }
                """);

        String serviceCode = """
                package com.example.model;

                public class UserService {
                    public void test(User user) {
                        Long id = user.getId();
                        String name = user.getUsername();
                    }
                }
                """;

        Path serviceFile = srcDir.resolve("com/example/model/UserService.java");

        List<JavaDiagnostics.Diag> diags = JavaDiagnostics.compile(
                serviceFile, serviceCode, "", List.of(srcDir));

        long errorCount = diags.stream()
                .filter(d -> d.severity() == JavaDiagnostics.Severity.ERROR)
                .count();

        assertEquals(0, errorCount,
                "Expected inherited getId() from BaseEntity to resolve without errors: "
                        + diags.stream().map(JavaDiagnostics.Diag::title).toList());
    }

    @Test
    public void testBuilderCompletionContext() {
        String code = "User.builder().";
        Completion.Context ctx = Completion.contextAt(code, code.length());
        assertNotNull(ctx);
        assertTrue(ctx.member());
        assertEquals("UserBuilder", ctx.receiver());

        String genericCode = "PaginatedResponse.<BrandResponse>builder().";
        Completion.Context genericCtx = Completion.contextAt(genericCode, genericCode.length());
        assertNotNull(genericCtx);
        assertTrue(genericCtx.member());
        assertEquals("PaginatedResponseBuilder", genericCtx.receiver());
    }

    @Test
    public void testGenericClassLombokBuilderResolution() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir.resolve("com/example/common"));
        Files.createDirectories(srcDir.resolve("com/example/dto"));
        Files.createDirectories(srcDir.resolve("com/example/service"));

        // 1. Generic PaginatedResponse<T> with Lombok annotations (matching NexaCommerce)
        Path pageFile = srcDir.resolve("com/example/common/PaginatedResponse.java");
        Files.writeString(pageFile, """
                package com.example.common;

                import lombok.AllArgsConstructor;
                import lombok.Builder;
                import lombok.Data;
                import lombok.NoArgsConstructor;
                import java.util.List;

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @Builder
                public class PaginatedResponse<T> {
                    private List<T> items;
                    private Long totalItems;
                    private int currentPage;
                    private int pageSize;
                    private int totalPages;
                }
                """);

        // 2. BrandResponse DTO
        Path brandFile = srcDir.resolve("com/example/dto/BrandResponse.java");
        Files.writeString(brandFile, """
                package com.example.dto;

                import lombok.AllArgsConstructor;
                import lombok.Builder;
                import lombok.Data;
                import lombok.NoArgsConstructor;

                @Data
                @Builder
                @NoArgsConstructor
                @AllArgsConstructor
                public class BrandResponse {
                    private Long id;
                    private String name;
                }
                """);

        // 3. Service calling PaginatedResponse.<BrandResponse>builder()
        String serviceCode = """
                package com.example.service;

                import com.example.common.PaginatedResponse;
                import com.example.dto.BrandResponse;
                import java.util.List;

                public class BrandServiceImpl {
                    public PaginatedResponse<BrandResponse> getBrands(List<BrandResponse> list) {
                        return PaginatedResponse.<BrandResponse>builder()
                                .items(list)
                                .totalItems(100L)
                                .currentPage(0)
                                .pageSize(10)
                                .totalPages(10)
                                .build();
                    }
                }
                """;

        Path serviceFile = srcDir.resolve("com/example/service/BrandServiceImpl.java");

        List<JavaDiagnostics.Diag> diags = JavaDiagnostics.compile(
                serviceFile, serviceCode, "", List.of(srcDir));

        long errorCount = diags.stream()
                .filter(d -> d.severity() == JavaDiagnostics.Severity.ERROR)
                .count();

        assertEquals(0, errorCount,
                "Expected 0 errors on PaginatedResponse.<BrandResponse>builder(), but got: "
                        + diags.stream().map(d -> d.title() + " at line " + d.line()).toList());
    }
}
