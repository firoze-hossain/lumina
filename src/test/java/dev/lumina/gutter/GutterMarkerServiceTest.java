package dev.lumina.gutter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GutterMarkerServiceTest {

    @TempDir
    Path tempDir;

    private Path srcDir;
    private GutterMarkerService service;

    @BeforeEach
    void setUp() throws IOException {
        srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
    }

    @Test
    void testInterfaceAndImplementationMethodMarkers() throws IOException {
        String serviceInterface = """
                package com.example;

                public interface CustomerService {
                    CustomerDetailResponse register(CustomerRegistrationRequest request);
                }
                """;

        String serviceImpl = """
                package com.example;

                import org.springframework.stereotype.Service;
                import org.springframework.transaction.annotation.Transactional;

                @Service
                public class CustomerServiceImpl implements CustomerService {

                    @Override
                    @Transactional
                    public CustomerDetailResponse register(CustomerRegistrationRequest request) {
                        return null;
                    }
                }
                """;

        Path interfacePath = srcDir.resolve("CustomerService.java");
        Path implPath = srcDir.resolve("CustomerServiceImpl.java");

        Files.writeString(interfacePath, serviceInterface);
        Files.writeString(implPath, serviceImpl);

        service = new GutterMarkerService(tempDir, List.of(srcDir));

        // 1. Check Interface markers
        Map<Integer, List<GutterMarker>> ifaceMarkers = service.analyzeFile(interfacePath, serviceInterface);
        assertNotNull(ifaceMarkers);

        // Should have IMPLEMENTED_METHOD on register line
        boolean foundIfaceMethodMarker = false;
        for (List<GutterMarker> list : ifaceMarkers.values()) {
            for (GutterMarker m : list) {
                if (m.type() == GutterMarker.MarkerType.IMPLEMENTED_METHOD) {
                    foundIfaceMethodMarker = true;
                    assertEquals("Is implemented in CustomerServiceImpl (com.example)", m.tooltipTitle());
                    assertEquals("Press Ctrl+Alt+B to navigate", m.tooltipSubtitle());
                    assertEquals(1, m.targets().size());
                    assertEquals("CustomerServiceImpl", m.targets().get(0).targetName());
                    assertEquals(implPath, m.targets().get(0).file());
                }
            }
        }
        assertTrue(foundIfaceMethodMarker, "Interface method should have IMPLEMENTED_METHOD gutter marker");

        // 2. Check Implementation markers
        Map<Integer, List<GutterMarker>> implMarkers = service.analyzeFile(implPath, serviceImpl);
        assertNotNull(implMarkers);

        boolean foundImplementsMethodMarker = false;
        boolean foundInferredAnnotationMarker = false;

        for (List<GutterMarker> list : implMarkers.values()) {
            for (GutterMarker m : list) {
                if (m.type() == GutterMarker.MarkerType.IMPLEMENTS_METHOD) {
                    foundImplementsMethodMarker = true;
                    assertEquals("Implements method in CustomerService (com.example)", m.tooltipTitle());
                    assertEquals("Press Ctrl+U to navigate", m.tooltipSubtitle());
                    assertEquals(1, m.targets().size());
                    assertEquals("CustomerService", m.targets().get(0).targetName());
                    assertEquals(interfacePath, m.targets().get(0).file());
                }
                if (m.type() == GutterMarker.MarkerType.INFERRED_ANNOTATION) {
                    foundInferredAnnotationMarker = true;
                    assertEquals("Inferred annotations available. Full signature:", m.tooltipTitle());
                    assertNotNull(m.signaturePreview());
                    assertTrue(m.signaturePreview().contains("@Transactional"));
                    assertTrue(m.signaturePreview().contains("@NotNull CustomerRegistrationRequest request"));
                }
            }
        }

        assertTrue(foundImplementsMethodMarker, "Impl method should have IMPLEMENTS_METHOD gutter marker");
        assertTrue(foundInferredAnnotationMarker, "Impl method should have INFERRED_ANNOTATION gutter marker (@)");
    }

    @Test
    void testSpringBeanDependencyInjectionMarker() throws IOException {
        String repoInterface = """
                package com.example;

                public interface CustomerRepository {
                }
                """;

        String serviceImpl = """
                package com.example;

                import org.springframework.stereotype.Service;
                import lombok.RequiredArgsConstructor;

                @Service
                @RequiredArgsConstructor
                public class CustomerServiceImpl {
                    private final CustomerRepository customerRepository;
                }
                """;

        Path repoPath = srcDir.resolve("CustomerRepository.java");
        Path servicePath = srcDir.resolve("CustomerServiceImpl.java");

        Files.writeString(repoPath, repoInterface);
        Files.writeString(servicePath, serviceImpl);

        service = new GutterMarkerService(tempDir, List.of(srcDir));

        Map<Integer, List<GutterMarker>> markers = service.analyzeFile(servicePath, serviceImpl);
        boolean foundInjectionMarker = false;

        for (List<GutterMarker> list : markers.values()) {
            for (GutterMarker m : list) {
                if (m.type() == GutterMarker.MarkerType.BEAN_INJECTION) {
                    foundInjectionMarker = true;
                    assertEquals("Navigate to autowired dependencies for 'customerRepository'", m.tooltipTitle());
                    assertEquals(1, m.targets().size());
                    assertEquals("CustomerRepository", m.targets().get(0).targetName());
                    assertEquals(repoPath, m.targets().get(0).file());
                }
            }
        }

        assertTrue(foundInjectionMarker, "Spring bean final field with @RequiredArgsConstructor should have BEAN_INJECTION marker");
    }

    @Test
    void testMultipleImplementationsForChooser() throws IOException {
        String serviceInterface = """
                package com.example;

                public interface Greeter {
                    void greet();
                }
                """;

        String impl1 = """
                package com.example;

                public class EnglishGreeter implements Greeter {
                    @Override
                    public void greet() {}
                }
                """;

        String impl2 = """
                package com.example;

                public class SpanishGreeter implements Greeter {
                    @Override
                    public void greet() {}
                }
                """;

        Path ifacePath = srcDir.resolve("Greeter.java");
        Files.writeString(ifacePath, serviceInterface);
        Files.writeString(srcDir.resolve("EnglishGreeter.java"), impl1);
        Files.writeString(srcDir.resolve("SpanishGreeter.java"), impl2);

        service = new GutterMarkerService(tempDir, List.of(srcDir));

        Map<Integer, List<GutterMarker>> markers = service.analyzeFile(ifacePath, serviceInterface);
        boolean foundMultiImpl = false;

        for (List<GutterMarker> list : markers.values()) {
            for (GutterMarker m : list) {
                if (m.type() == GutterMarker.MarkerType.IMPLEMENTED_METHOD) {
                    foundMultiImpl = true;
                    assertEquals("Has implementations in 2 classes", m.tooltipTitle());
                    assertEquals(2, m.targets().size());
                }
            }
        }

        assertTrue(foundMultiImpl, "Interface method with multiple implementations should report count");
    }
}
