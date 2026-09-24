package dev.lumina.ui;

import dev.lumina.gutter.GutterMarker;
import dev.lumina.gutter.GutterMarkerIcons;
import dev.lumina.gutter.GutterMarkerService;
import dev.lumina.run.RunConfiguration;
import dev.lumina.run.TestReport;
import javafx.application.Platform;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TestRunnerAndGutterTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (IllegalStateException alreadyStarted) {
            javaFxAvailable = true;
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    @Test
    void testRunConfigurationForTests() {
        Path root = Path.of("/workspace/project");
        RunConfiguration classConfig = RunConfiguration.testClass(root, "com.example.OrderServiceTest");
        assertEquals(RunConfiguration.Type.TEST_CLASS, classConfig.type());
        assertTrue(classConfig.isTest());
        assertEquals("com.example.OrderServiceTest", classConfig.testClass());
        assertNull(classConfig.testMethod());
        assertEquals("OrderServiceTest", classConfig.label());

        RunConfiguration methodConfig = RunConfiguration.testMethod(root, "com.example.OrderServiceTest", "testPaymentProcessing");
        assertEquals(RunConfiguration.Type.TEST_METHOD, methodConfig.type());
        assertTrue(methodConfig.isTest());
        assertEquals("com.example.OrderServiceTest", methodConfig.testClass());
        assertEquals("testPaymentProcessing", methodConfig.testMethod());
        assertEquals("OrderServiceTest.testPaymentProcessing", methodConfig.label());

        // Test Maven command generation
        List<String> cmdClass = RunConfiguration.maven(root, "-Dtest=OrderServiceTest", "test");
        assertTrue(cmdClass.contains("test"));
        assertTrue(cmdClass.contains("-Dtest=OrderServiceTest"));

        List<String> cmdMethod = RunConfiguration.maven(root, "-Dtest=OrderServiceTest#testPaymentProcessing", "test");
        assertTrue(cmdMethod.contains("test"));
        assertTrue(cmdMethod.contains("-Dtest=OrderServiceTest#testPaymentProcessing"));
    }

    @Test
    void testGutterMarkerTestTypesAndOutcomes() {
        GutterMarker classMarker = new GutterMarker(10, GutterMarker.MarkerType.TEST_CLASS, "Run Test Class", "Class", "public class OrderTest", List.of());
        assertTrue(classMarker.isTestMarker());

        GutterMarker methodMarker = new GutterMarker(15, GutterMarker.MarkerType.TEST_METHOD, "Run Test Method", "Method", "void testOne()", List.of());
        assertTrue(methodMarker.isTestMarker());

        GutterMarker passedMarker = new GutterMarker(20, GutterMarker.MarkerType.TEST_METHOD_PASSED, "Test Passed", "Passed", "void testOne()", List.of());
        assertTrue(passedMarker.isTestMarker());

        GutterMarker failedMarker = new GutterMarker(25, GutterMarker.MarkerType.TEST_METHOD_FAILED, "Test Failed", "Failed", "void testOne()", List.of());
        assertTrue(failedMarker.isTestMarker());

        GutterMarker beanMarker = new GutterMarker(30, GutterMarker.MarkerType.BEAN_INJECTION, "Autowired Bean", "Bean", "private Service svc", List.of());
        assertFalse(beanMarker.isTestMarker());

        // Test outcome tracking
        GutterMarkerService.clearTestOutcomes();
        assertNull(GutterMarkerService.getTestOutcome("com.example.CalculatorTest", "testAdd"));

        GutterMarkerService.recordTestOutcome("com.example.CalculatorTest", "testAdd", true);
        assertEquals(Boolean.TRUE, GutterMarkerService.getTestOutcome("com.example.CalculatorTest", "testAdd"));

        GutterMarkerService.recordTestOutcome("com.example.CalculatorTest", "testSubtract", false);
        assertEquals(Boolean.FALSE, GutterMarkerService.getTestOutcome("com.example.CalculatorTest", "testSubtract"));

        GutterMarkerService.clearTestOutcomes();
        assertNull(GutterMarkerService.getTestOutcome("com.example.CalculatorTest", "testAdd"));
    }

    @Test
    void testGutterMarkerDetectionInTestClass() {
        String testSource = """
                package dev.example;
                
                import org.junit.jupiter.api.Test;
                
                public class ServiceTest {
                    @Test
                    void testOne() {
                        assert true;
                    }
                    
                    @org.junit.jupiter.api.Test
                    public void testTwo() {
                        assert true;
                    }
                }
                """;

        Path dummyPath = Path.of("src/test/java/dev/example/ServiceTest.java");
        GutterMarkerService service = new GutterMarkerService(dummyPath.getParent(), List.of());
        Map<Integer, List<GutterMarker>> markers = service.analyzeFile(dummyPath, testSource);
        assertNotNull(markers);

        // Verify class marker is placed
        boolean hasClassMarker = markers.values().stream()
                .flatMap(List::stream)
                .anyMatch(m -> m.type() == GutterMarker.MarkerType.TEST_CLASS);
        assertTrue(hasClassMarker, "Expected TEST_CLASS gutter marker");

        // Verify method markers are placed
        long methodMarkers = markers.values().stream()
                .flatMap(List::stream)
                .filter(m -> m.type() == GutterMarker.MarkerType.TEST_METHOD)
                .count();
        assertEquals(2, methodMarkers, "Expected 2 TEST_METHOD gutter markers");
    }

    @Test
    void testTestResultsPanelLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                TestResultsPanel panel = new TestResultsPanel();
                assertNotNull(panel);

                panel.showRunning("OrderServiceTest");
                panel.appendConsole("Running tests in OrderServiceTest...\n");

                List<TestReport.Case> cases = List.of(
                        new TestReport.Case("com.example.OrderTest", "testCreateOrder", TestReport.Status.PASSED, 0.12, null),
                        new TestReport.Case("com.example.OrderTest", "testRefundOrder", TestReport.Status.FAILED, 0.24, "AssertionError: expected true but was false"),
                        new TestReport.Case("com.example.OrderTest", "testCancelOrder", TestReport.Status.SKIPPED, 0.0, "Test skipped")
                );
                TestReport.Suite suite = new TestReport.Suite("OrderTest", 0.36, cases);

                panel.showResults(List.of(suite));
                panel.appendConsole("Test run finished.\n");
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testGutterMarkerIconsCreation() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Node classIcon = GutterMarkerIcons.getIcon(GutterMarker.MarkerType.TEST_CLASS);
                assertNotNull(classIcon);

                Node methodIcon = GutterMarkerIcons.getIcon(GutterMarker.MarkerType.TEST_METHOD);
                assertNotNull(methodIcon);

                Node passedIcon = GutterMarkerIcons.getIcon(GutterMarker.MarkerType.TEST_METHOD_PASSED);
                assertNotNull(passedIcon);

                Node failedIcon = GutterMarkerIcons.getIcon(GutterMarker.MarkerType.TEST_METHOD_FAILED);
                assertNotNull(failedIcon);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testBrandIsolationInModifiedComponents() throws IOException {
        String[] paths = new String[]{
                "src/main/java/dev/lumina/run/RunConfiguration.java",
                "src/main/java/dev/lumina/gutter/GutterMarker.java",
                "src/main/java/dev/lumina/gutter/GutterMarkerIcons.java",
                "src/main/java/dev/lumina/gutter/GutterMarkerService.java",
                "src/main/java/dev/lumina/ui/TestResultsPanel.java",
                "src/main/java/dev/lumina/ui/ConsolePane.java"
        };

        for (String p : paths) {
            Path file = Path.of(p);
            if (!Files.exists(file)) continue;
            String content = Files.readString(file);
            assertFalse(content.toLowerCase().contains("intellij"), "File " + p + " must not contain 'intellij'");
            assertFalse(content.toLowerCase().contains("jetbrains"), "File " + p + " must not contain 'jetbrains'");
        }
    }
}
