package dev.lumina.ui;

import dev.lumina.inspections.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SettingsInspectionsTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            javaFxAvailable = true;
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    @Test
    void testHighlightSeverityEnum() {
        HighlightSeverity error = HighlightSeverity.ERROR;
        assertEquals("Error", error.getDisplayName());
        assertEquals("#F75464", error.getHexColor());
        assertEquals(400, error.getLevel());

        HighlightSeverity warning = HighlightSeverity.WARNING;
        assertEquals("Warning", warning.getDisplayName());

        assertEquals(HighlightSeverity.ERROR, HighlightSeverity.fromDisplayName("Error"));
        assertEquals(HighlightSeverity.WARNING, HighlightSeverity.fromDisplayName("Warning"));
        assertEquals(HighlightSeverity.WEAK_WARNING, HighlightSeverity.fromDisplayName("Weak Warning"));
        assertEquals(HighlightSeverity.SERVER_PROBLEM, HighlightSeverity.fromDisplayName("Server Problem"));
        assertEquals(HighlightSeverity.GRAMMAR_ERROR, HighlightSeverity.fromDisplayName("Grammar Error"));
        assertEquals(HighlightSeverity.TYPO, HighlightSeverity.fromDisplayName("Typo"));
        assertEquals(HighlightSeverity.STYLE_SUGGESTION, HighlightSeverity.fromDisplayName("Style Suggestion"));
        assertEquals(HighlightSeverity.CONSIDERATION, HighlightSeverity.fromDisplayName("Consideration"));
        assertEquals(HighlightSeverity.NO_HIGHLIGHTING, HighlightSeverity.fromDisplayName("No highlighting (fix available)"));

        if (javaFxAvailable) {
            assertNotNull(error.createIcon(14));
            assertNotNull(warning.createIcon(14));
            assertNotNull(HighlightSeverity.SERVER_PROBLEM.createIcon(14));
            assertNotNull(HighlightSeverity.WEAK_WARNING.createIcon(14));
            assertNotNull(HighlightSeverity.TYPO.createIcon(14));
            assertNotNull(HighlightSeverity.STYLE_SUGGESTION.createIcon(14));
        }
    }

    @Test
    void testInspectionToolBuilderAndProperties() {
        InspectionTool tool = InspectionTool.builder("Custom.MyRule")
                .displayName("My Custom Rule")
                .groupPath("Java")
                .description("Detailed explanation of custom inspection.")
                .defaultSeverity(HighlightSeverity.ERROR)
                .defaultEnabled(true)
                .defaultScope("Project Files")
                .defaultHighlighting("Error")
                .language("Java")
                .addTag("bugs")
                .build();

        assertEquals("Custom.MyRule", tool.getId());
        assertEquals("My Custom Rule", tool.getDisplayName());
        assertEquals("Java", tool.getGroupPath());
        assertEquals("Detailed explanation of custom inspection.", tool.getDescription());
        assertEquals(HighlightSeverity.ERROR, tool.getDefaultSeverity());
        assertTrue(tool.isDefaultEnabled());
        assertEquals("Project Files", tool.getDefaultScope());
        assertEquals("Error", tool.getDefaultHighlighting());
        assertEquals("Java", tool.getLanguage());
        assertTrue(tool.getTags().contains("bugs"));
    }

    @Test
    void testInspectionRegistryAll93CategoriesAndDynamicRegistration() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        assertNotNull(registry);

        List<String> categories = registry.getAllCategories();
        assertTrue(categories.size() >= 93, "Registry should contain all 93 categories from reference screenshots");

        // Verify key categories from screenshots 1, 2, 3
        List<String> requiredCategories = List.of(
                "User defined", "Angular", "AOP", "Application servers", "Bean Validation",
                "CDI (Contexts and Dependency Injection)", "Code Coverage", "Code metrics",
                "Compose Multiplatform Preview", "Cron", "CSS", "Dev Container",
                "Docker-compose", "Dockerfile", "EditorConfig", "EL", "FreeMarker",
                "General", "GitHub actions", "GitLab CI/CD", "Go", "Go modules",
                "Gradle", "Gradle Declarative", "Groovy", "Hibernate", "HTML",
                "HTTP Client", "Inappropriate gRPC request scheme", "Internationalization",
                "Jakarta Data", "Java", "Java EE", "JavaFX", "JavaScript and TypeScript",
                "JPA", "JRuby", "JSON and JSON5", "JSONPath", "JSP", "JUnit",
                "JVM languages", "Kotlin", "Ktor", "Kubernetes", "Language injection",
                "Less", "Liquibase", "Manifest", "Markdown", "Maven", "Micronaut",
                "MongoJS", "MySQL", "OpenAPI specifications", "Oracle", "Pandas",
                "Pattern validation", "PHP", "PostCSS", "PostgreSQL", "Proofreading",
                "Properties files", "Protocol Buffers", "Python", "Qodana", "Quarkus",
                "RBS", "RegExp", "RELAX NG", "Requirements", "RESTful Web Service (JAX-RS)",
                "Ruby", "Rust", "Sass/SCSS", "sbt", "Scala", "Security", "Shell script",
                "Spring", "Spring Data", "Spring Modulith", "SQL", "SQL server",
                "Thymeleaf", "TOML", "Velocity", "Version control", "Vue", "XML",
                "XPath", "XSLT", "YAML"
        );

        for (String cat : requiredCategories) {
            assertTrue(categories.stream().anyMatch(c -> c.equalsIgnoreCase(cat)),
                    "Category must be present: " + cat);
            List<InspectionTool> tools = registry.getToolsForCategory(cat);
            assertFalse(tools.isEmpty(), "Category " + cat + " must have inspection tools");
        }

        // Test search
        List<InspectionTool> appServerTools = registry.search("Application servers");
        assertFalse(appServerTools.isEmpty());

        // Test dynamic registration
        String dynamicId = "Plugin.CustomInspectionRule";
        InspectionTool dynamicTool = InspectionTool.builder(dynamicId)
                .displayName("Dynamic Rule")
                .groupPath("User defined")
                .defaultEnabled(true)
                .build();
        registry.register(dynamicTool);
        assertNotNull(registry.getTool(dynamicId));

        registry.unregister(dynamicId);
        assertNull(registry.getTool(dynamicId));
    }

    @Test
    void testInspectionProfileTriStateCalculation() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("Test Profile", true);

        // Application servers: all tools enabled -> CHECKED
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Application servers", registry));

        // Code metrics: all tools disabled -> UNCHECKED
        assertEquals(InspectionProfile.TriState.UNCHECKED, profile.getCategoryState("Code metrics", registry));

        // Qodana: all tools disabled -> UNCHECKED
        assertEquals(InspectionProfile.TriState.UNCHECKED, profile.getCategoryState("Qodana", registry));

        // OpenAPI specifications: all tools enabled -> CHECKED
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("OpenAPI specifications", registry));

        // Java: some enabled, some disabled -> INDETERMINATE
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Java", registry));

        // CSS: some enabled, some disabled -> INDETERMINATE
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("CSS", registry));

        // Toggle Code metrics to enabled
        profile.setCategoryEnabled("Code metrics", true, registry);
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Code metrics", registry));

        // Toggle Application servers to disabled
        profile.setCategoryEnabled("Application servers", false, registry);
        assertEquals(InspectionProfile.TriState.UNCHECKED, profile.getCategoryState("Application servers", registry));
    }

    @Test
    void testInspectionProfileBulkOperationsAndCloning() {
        InspectionProfile profile = new InspectionProfile("Working Profile", true);
        List<String> ids = List.of("AppServer.DeploymentDescriptor", "AppServer.ServerConfig");

        profile.setToolsSeverity(ids, HighlightSeverity.ERROR);
        assertEquals(HighlightSeverity.ERROR, profile.getState("AppServer.DeploymentDescriptor").getSeverity());
        assertEquals(HighlightSeverity.ERROR, profile.getState("AppServer.ServerConfig").getSeverity());

        profile.setToolsScope(ids, "Production");
        assertEquals("Production", profile.getState("AppServer.DeploymentDescriptor").getScope());

        profile.setToolsHighlighting(ids, "Server Problem");
        assertEquals("Server Problem", profile.getState("AppServer.DeploymentDescriptor").getHighlighting());

        // Clone
        InspectionProfile clone = profile.cloneProfile("Clone Profile", false);
        assertEquals("Clone Profile", clone.getName());
        assertFalse(clone.isProjectLevel());
        assertEquals(HighlightSeverity.ERROR, clone.getState("AppServer.DeploymentDescriptor").getSeverity());
        assertEquals("Production", clone.getState("AppServer.DeploymentDescriptor").getScope());
    }

    @Test
    void testInspectionProfileManager() {
        InspectionProfileManager manager = InspectionProfileManager.getInstance();
        assertNotNull(manager);

        InspectionProfile active = manager.getActiveProfile();
        assertNotNull(active);

        // Create custom profile
        String customName = "Test Custom Suite";
        InspectionProfile created = manager.createProfile(customName, true, active);
        assertNotNull(created);
        assertEquals(customName, created.getName());
        assertNotNull(manager.getProfile(customName));

        // Rename
        String renamed = "Test Renamed Suite";
        assertTrue(manager.renameProfile(customName, renamed));
        assertNull(manager.getProfile(customName));
        assertNotNull(manager.getProfile(renamed));

        // Delete
        assertTrue(manager.deleteProfile(renamed));
        assertNull(manager.getProfile(renamed));
    }

    @Test
    void testDetailedInspectionsFromScreenshots() {
        InspectionRegistry registry = InspectionRegistry.getInstance();

        // Angular: 32 inspections from screenshot 4
        List<InspectionTool> angularTools = registry.getToolsForCategory("Angular");
        assertEquals(32, angularTools.size(), "Angular should contain all 32 inspections from screenshot 4");
        assertTrue(angularTools.stream().anyMatch(t -> t.getDisplayName().equals("Accessing length property of an uncalled signal")));
        assertTrue(angularTools.stream().anyMatch(t -> t.getDisplayName().equals("Ambiguous component tag")));
        assertTrue(angularTools.stream().anyMatch(t -> t.getDisplayName().equals("Illegal @for loop access")));
        assertTrue(angularTools.stream().anyMatch(t -> t.getDisplayName().equals("Unused import in an Angular component declaration")));

        // AOP: 6 inspections from screenshot 5
        List<InspectionTool> aopTools = registry.getToolsForCategory("AOP");
        assertEquals(6, aopTools.size(), "AOP should contain all 6 inspections from screenshot 5");
        assertTrue(aopTools.stream().anyMatch(t -> t.getDisplayName().equals("Advice parameters (argNames, returning, throwing) consistency check")));
        assertTrue(aopTools.stream().anyMatch(t -> t.getDisplayName().equals("Around advice style inspection")));
        assertTrue(aopTools.stream().anyMatch(t -> t.getDisplayName().equals("Warning: argNames not defined")));

        // Application servers: contains JBoss/WildFly from screenshot 5
        List<InspectionTool> appServerTools = registry.getToolsForCategory("Application servers");
        assertTrue(appServerTools.stream().anyMatch(t -> t.getDisplayName().equals("JBoss/WildFly")));

        // Bean Validation: 3 inspections from screenshot 5
        List<InspectionTool> beanTools = registry.getToolsForCategory("Bean Validation");
        assertEquals(3, beanTools.size(), "Bean Validation should contain all 3 inspections from screenshot 5");
        assertTrue(beanTools.stream().anyMatch(t -> t.getDisplayName().equals("Incorrect 'min' and 'max' values in Bean Validation annotations")));

        // CDI: 17 inspections from screenshot 5
        List<InspectionTool> cdiTools = registry.getToolsForCategory("CDI (Contexts and Dependency Injection)");
        assertEquals(17, cdiTools.size(), "CDI should contain all 17 inspections from screenshot 5");
        assertTrue(cdiTools.stream().anyMatch(t -> t.getDisplayName().equals("Bean has collision of scope in stereotypes")));
        assertTrue(cdiTools.stream().anyMatch(t -> t.getDisplayName().equals("Incorrect @Specializes usage")));
        assertTrue(cdiTools.stream().anyMatch(t -> t.getDisplayName().equals("Vetoed @Alternative bean")));

        // Code Coverage: 5 inspections from screenshot 1
        List<InspectionTool> covTools = registry.getToolsForCategory("Code Coverage");
        assertEquals(5, covTools.size());
        assertTrue(covTools.stream().allMatch(InspectionTool::isBatchModeOnly));

        // Code metrics: 1 inspection from screenshot 1
        List<InspectionTool> metricsTools = registry.getToolsForCategory("Code metrics");
        assertEquals(1, metricsTools.size());
        assertFalse(metricsTools.get(0).isDefaultEnabled());

        // Compose Multiplatform Preview: 2 inspections from screenshot 1
        List<InspectionTool> composeTools = registry.getToolsForCategory("Compose Multiplatform Preview");
        assertEquals(2, composeTools.size());
        assertTrue(composeTools.stream().allMatch(t -> t.getDefaultSeverity() == HighlightSeverity.ERROR));

        // Cron: 1 inspection from screenshot 1
        List<InspectionTool> cronTools = registry.getToolsForCategory("Cron");
        assertEquals(1, cronTools.size());

        // CSS: subcategories and 31 inspections from screenshots 1 & 2
        List<String> cssSubCats = registry.getAllSubCategories("CSS");
        assertEquals(4, cssSubCats.size());
        List<InspectionTool> cssAll = registry.getToolsForCategory("CSS");
        assertEquals(31, cssAll.size());
        assertEquals(1, registry.getToolsDirectlyInCategory("CSS/Code quality tools").size());
        assertEquals(2, registry.getToolsDirectlyInCategory("CSS/Code style issues").size());
        assertEquals(17, registry.getToolsDirectlyInCategory("CSS/Invalid elements").size());
        assertEquals(11, registry.getToolsDirectlyInCategory("CSS/Probable bugs").size());

        // Dev Container: 2 inspections from screenshot 2
        List<InspectionTool> devContainerTools = registry.getToolsForCategory("Dev Container");
        assertEquals(2, devContainerTools.size());
        assertTrue(devContainerTools.stream().allMatch(t -> t.getDefaultSeverity() == HighlightSeverity.ERROR));

        // Docker-compose: 5 inspections from screenshot 3
        List<InspectionTool> composeDockerTools = registry.getToolsForCategory("Docker-compose");
        assertEquals(5, composeDockerTools.size());

        // Dockerfile: 9 inspections from screenshot 3
        List<InspectionTool> dockerfileTools = registry.getToolsForCategory("Dockerfile");
        assertEquals(9, dockerfileTools.size());

        // EditorConfig: 29 inspections from screenshots 3 & 4
        List<InspectionTool> editorConfigTools = registry.getToolsForCategory("EditorConfig");
        assertEquals(29, editorConfigTools.size());

        // EL: 1 inspection from screenshot 4
        List<InspectionTool> elTools = registry.getToolsForCategory("EL");
        assertEquals(1, elTools.size());

        // FreeMarker: 8 inspections from screenshot 4
        List<InspectionTool> fmTools = registry.getToolsForCategory("FreeMarker");
        assertEquals(8, fmTools.size());

        // General: 14 inspections from screenshots 4 & 5
        List<InspectionTool> genTools = registry.getToolsForCategory("General");
        assertEquals(14, genTools.size());
        assertTrue(genTools.stream().anyMatch(t -> t.getDisplayName().startsWith("Annotator") && t.isBatchModeOnly()));

        // GitHub actions: 6 inspections from screenshot 5
        List<InspectionTool> ghTools = registry.getToolsForCategory("GitHub actions");
        assertEquals(6, ghTools.size());

        // GitLab CI/CD: 3 inspections from screenshot 5
        List<InspectionTool> gitlabTools = registry.getToolsForCategory("GitLab CI/CD");
        assertEquals(3, gitlabTools.size());

        // Go: 88 inspections with 7 subcategories from screenshots 1, 2, 3
        List<InspectionTool> goTools = registry.getToolsForCategory("Go");
        assertEquals(88, goTools.size());
        assertEquals(15, registry.getToolsDirectlyInCategory("Go/Code style issues").size());
        assertEquals(4, registry.getToolsDirectlyInCategory("Go/Control flow issues").size());
        assertEquals(5, registry.getToolsDirectlyInCategory("Go/Data flow analysis").size());
        assertEquals(20, registry.getToolsDirectlyInCategory("Go/Declaration redundancy").size());
        assertEquals(11, registry.getToolsDirectlyInCategory("Go/General").size());
        assertEquals(31, registry.getToolsDirectlyInCategory("Go/Probable bugs").size());
        assertEquals(2, registry.getToolsDirectlyInCategory("Go/Security").size());

        // Go modules: 7 inspections from screenshot 3
        List<InspectionTool> goModTools = registry.getToolsForCategory("Go modules");
        assertEquals(7, goModTools.size());
        assertEquals(1, registry.getToolsDirectlyInCategory("Go modules/Declaration redundancy").size());
        assertEquals(4, registry.getToolsDirectlyInCategory("Go modules/Dependency issues (go list -m -u)").size());
        assertEquals(2, registry.getToolsDirectlyInCategory("Go modules/General").size());

        // Gradle: 9 inspections from screenshot 4
        List<InspectionTool> gradleTools = registry.getToolsForCategory("Gradle");
        assertEquals(9, gradleTools.size());
        assertEquals(1, registry.getToolsDirectlyInCategory("Gradle/Best practises").size());
        assertEquals(5, registry.getToolsDirectlyInCategory("Gradle/Probable bugs").size());
        assertEquals(1, registry.getToolsDirectlyInCategory("Gradle/Style").size());
        assertEquals(2, registry.getToolsDirectlyInCategory("Gradle/Validity issues").size());

        // Gradle Declarative: 1 inspection from screenshot 4
        List<InspectionTool> gradleDecTools = registry.getToolsForCategory("Gradle Declarative");
        assertEquals(1, gradleDecTools.size());

        // Groovy: 136 inspections across 15 subcategories
        List<InspectionTool> groovyTools = registry.getToolsForCategory("Groovy");
        assertEquals(136, groovyTools.size());
        assertEquals(5, registry.getToolsDirectlyInCategory("Groovy/Annotations").size());
        assertEquals(8, registry.getToolsDirectlyInCategory("Groovy/Assignment issues").size());
        assertEquals(20, registry.getToolsDirectlyInCategory("Groovy/Control flow issues").size());
        assertEquals(5, registry.getToolsDirectlyInCategory("Groovy/Data flow").size());
        assertEquals(1, registry.getToolsDirectlyInCategory("Groovy/Declaration redundancy").size());
        assertEquals(7, registry.getToolsDirectlyInCategory("Groovy/Error handling").size());
        assertEquals(4, registry.getToolsDirectlyInCategory("Groovy/GPath").size());
        assertEquals(6, registry.getToolsDirectlyInCategory("Groovy/Method metrics").size());
        assertEquals(8, registry.getToolsDirectlyInCategory("Groovy/Naming conventions").size());
        assertEquals(2, registry.getToolsDirectlyInCategory("Groovy/Other").size());
        assertEquals(21, registry.getToolsDirectlyInCategory("Groovy/Potentially confusing code constructs").size());
        assertEquals(17, registry.getToolsDirectlyInCategory("Groovy/Probable bugs").size());
        assertEquals(12, registry.getToolsDirectlyInCategory("Groovy/Style").size());
        assertEquals(18, registry.getToolsDirectlyInCategory("Groovy/Threading issues").size());
        assertEquals(2, registry.getToolsDirectlyInCategory("Groovy/Validity issues").size());

        // Hibernate: 6 inspections
        List<InspectionTool> hibernateTools = registry.getToolsForCategory("Hibernate");
        assertEquals(6, hibernateTools.size());
        assertTrue(hibernateTools.stream().anyMatch(t -> t.getDisplayName().equals("Incorrect use of @Find annotation")));
        assertTrue(hibernateTools.stream().anyMatch(t -> t.getDisplayName().equals("Invalid Hibernate XML mappings")));

        // HTML: 22 inspections (6 in Accessibility, 16 direct)
        List<InspectionTool> htmlTools = registry.getToolsForCategory("HTML");
        assertEquals(22, htmlTools.size());
        assertEquals(6, registry.getToolsDirectlyInCategory("HTML/Accessibility").size());
        assertEquals(16, registry.getToolsDirectlyInCategory("HTML").size());
        assertTrue(htmlTools.stream().anyMatch(t -> t.getDisplayName().equals("Missing required 'alt' attribute")));
        assertTrue(htmlTools.stream().anyMatch(t -> t.getDisplayName().equals("Missing closing tag")));

        // HTTP Client: 16 inspections from Image 1
        List<InspectionTool> httpClientTools = registry.getToolsForCategory("HTTP Client");
        assertEquals(16, httpClientTools.size());
        assertTrue(httpClientTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertTrue(httpClientTools.stream().anyMatch(t -> t.getDisplayName().equals("Unresolved Auth identifier")
                && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertTrue(httpClientTools.stream().anyMatch(t -> t.getDisplayName().equals("Ambiguity Encoding Inspection")
                && t.getDefaultSeverity() == HighlightSeverity.WARNING));
        assertTrue(httpClientTools.stream().anyMatch(t -> t.getDisplayName().equals("'$placeholder' in HTTP Request")));

        // Inappropriate gRPC request scheme: 1 inspection from Image 1
        List<InspectionTool> grpcTools = registry.getToolsForCategory("Inappropriate gRPC request scheme");
        assertEquals(1, grpcTools.size());
        assertEquals("GRPC request schema can be substituted or omitted", grpcTools.get(0).getDisplayName());
        assertTrue(grpcTools.get(0).isDefaultEnabled());

        // Internationalization: 2 inspections from Image 1
        List<InspectionTool> i18nTools = registry.getToolsForCategory("Internationalization");
        assertEquals(2, i18nTools.size());
        assertTrue(i18nTools.stream().anyMatch(t -> t.getDisplayName().equals("Lossy encoding")));
        assertTrue(i18nTools.stream().anyMatch(t -> t.getDisplayName().equals("Non-ASCII characters")));

        // Jakarta Data: 2 inspections from Image 1
        List<InspectionTool> jdTools = registry.getToolsForCategory("Jakarta Data");
        assertEquals(2, jdTools.size());
        assertTrue(jdTools.stream().anyMatch(t -> t.getDisplayName().equals("Incorrect repository method parameter")));
        assertTrue(jdTools.stream().anyMatch(t -> t.getDisplayName().equals("Jakarta Data repository method errors")));

        // Java: 48 subcategories from Images 2 & 3
        List<String> javaSubCats = registry.getAllSubCategories("Java");
        assertEquals(48, javaSubCats.size());
        InspectionProfile defaultProfile = new InspectionProfile("Default", true);
        assertEquals(InspectionProfile.TriState.INDETERMINATE, defaultProfile.getCategoryState("Java", registry));

        // Verify checked subcategories in Java
        List<String> checkedJavaSubCats = List.of(
                "Java/Bitwise operation issues", "Java/Code style issues", "Java/Declaration redundancy",
                "Java/Javadoc", "Java/Probable bugs", "Java/Properties files",
                "Java/toString() issues", "Java/Verbose or redundant code constructs", "Java/Visibility"
        );
        for (String subCat : checkedJavaSubCats) {
            assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState(subCat, registry),
                    "Expected CHECKED for " + subCat);
        }

        // Verify unchecked subcategories in Java
        List<String> uncheckedJavaSubCats = List.of(
                "Java/Class metrics", "Java/Java language level migration aids", "Java/Lombok",
                "Java/Method metrics", "Java/Numeric issues", "Java/Performance",
                "Java/Resource management", "Java/Threading issues"
        );
        for (String subCat : uncheckedJavaSubCats) {
            assertEquals(InspectionProfile.TriState.UNCHECKED, defaultProfile.getCategoryState(subCat, registry),
                    "Expected UNCHECKED for " + subCat);
        }

        // Java EE: 6 inspections from Image 1
        List<InspectionTool> javaEeTools = registry.getToolsForCategory("Java EE");
        assertEquals(6, javaEeTools.size());
        assertTrue(javaEeTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(5, javaEeTools.stream().filter(t -> t.getDefaultSeverity() == HighlightSeverity.ERROR).count());
        assertEquals(1, javaEeTools.stream().filter(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING).count());

        // JavaFX: 8 inspections from Image 1
        List<InspectionTool> javaFxTools = registry.getToolsForCategory("JavaFX");
        assertEquals(8, javaFxTools.size());
        assertTrue(javaFxTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertTrue(javaFxTools.stream().allMatch(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING));

        // JavaScript and TypeScript: 26 subcategories from Image 2
        List<String> jsSubCats = registry.getAllSubCategories("JavaScript and TypeScript");
        assertEquals(26, jsSubCats.size());
        assertEquals(InspectionProfile.TriState.INDETERMINATE, defaultProfile.getCategoryState("JavaScript and TypeScript", registry));

        List<String> uncheckedJsSubCats = List.of(
                "JavaScript and TypeScript/Code quality tools",
                "JavaScript and TypeScript/DOM issues",
                "JavaScript and TypeScript/Function metrics"
        );
        for (String subCat : uncheckedJsSubCats) {
            assertEquals(InspectionProfile.TriState.UNCHECKED, defaultProfile.getCategoryState(subCat, registry),
                    "Expected UNCHECKED for " + subCat);
        }

        // JPA: 26 inspections (2 under Kotlin, 24 direct) from Image 3
        List<InspectionTool> jpaTools = registry.getToolsForCategory("JPA");
        assertEquals(26, jpaTools.size());
        assertEquals(2, registry.getToolsDirectlyInCategory("JPA/Kotlin").size());
        assertEquals(24, registry.getToolsDirectlyInCategory("JPA").size());
        assertTrue(jpaTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("JPA", registry));

        // JRuby: 8 inspections from Image 3
        List<InspectionTool> jrubyTools = registry.getToolsForCategory("JRuby");
        assertEquals(8, jrubyTools.size());
        assertTrue(jrubyTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(6, jrubyTools.stream().filter(t -> t.getDefaultSeverity() == HighlightSeverity.ERROR).count());
        assertEquals(2, jrubyTools.stream().filter(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING).count());

        // JSON and JSON5: 7 inspections from Image 4
        List<InspectionTool> jsonTools = registry.getToolsForCategory("JSON and JSON5");
        assertEquals(7, jsonTools.size());
        assertTrue(jsonTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("JSON and JSON5", registry));

        // JSONPath: 3 inspections from Image 4
        List<InspectionTool> jsonPathTools = registry.getToolsForCategory("JSONPath");
        assertEquals(3, jsonPathTools.size());
        assertTrue(jsonPathTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("JSONPath", registry));

        // JSP: 13 inspections from Image 4 (12 enabled, 1 disabled)
        List<InspectionTool> jspTools = registry.getToolsForCategory("JSP");
        assertEquals(13, jspTools.size());
        assertEquals(12, jspTools.stream().filter(InspectionTool::isDefaultEnabled).count());
        assertEquals(1, jspTools.stream().filter(t -> !t.isDefaultEnabled()).count());
        assertEquals(InspectionProfile.TriState.INDETERMINATE, defaultProfile.getCategoryState("JSP", registry));

        // JUnit: 16 inspections from Image 5 (5 enabled, 11 disabled)
        List<InspectionTool> junitTools = registry.getToolsForCategory("JUnit");
        assertEquals(16, junitTools.size());
        assertEquals(5, junitTools.stream().filter(InspectionTool::isDefaultEnabled).count());
        assertEquals(11, junitTools.stream().filter(t -> !t.isDefaultEnabled()).count());
        assertEquals(InspectionProfile.TriState.INDETERMINATE, defaultProfile.getCategoryState("JUnit", registry));

        // JVM languages: 34 inspections (6 in Logging, 7 in Test frameworks, 21 direct) from Image 1
        List<InspectionTool> jvmTools = registry.getToolsForCategory("JVM languages");
        assertEquals(34, jvmTools.size());
        assertEquals(6, registry.getToolsDirectlyInCategory("JVM languages/Logging").size());
        assertEquals(7, registry.getToolsDirectlyInCategory("JVM languages/Test frameworks").size());
        assertEquals(21, registry.getToolsDirectlyInCategory("JVM languages").size());
        assertEquals(InspectionProfile.TriState.INDETERMINATE, defaultProfile.getCategoryState("JVM languages", registry));

        // Kotlin: 26 inspections (11 subcategories, 9 direct) from Image 2
        List<InspectionTool> kotlinTools = registry.getToolsForCategory("Kotlin");
        assertEquals(26, kotlinTools.size());
        assertEquals(11, registry.getAllSubCategories("Kotlin").size());
        assertEquals(9, registry.getToolsDirectlyInCategory("Kotlin").size());
        assertEquals(InspectionProfile.TriState.INDETERMINATE, defaultProfile.getCategoryState("Kotlin", registry));

        // Ktor: 2 inspections from Image 2
        List<InspectionTool> ktorTools = registry.getToolsForCategory("Ktor");
        assertEquals(2, ktorTools.size());
        assertEquals(1, ktorTools.stream().filter(InspectionTool::isDefaultEnabled).count());
        assertEquals(1, ktorTools.stream().filter(t -> !t.isDefaultEnabled()).count());
        assertEquals(InspectionProfile.TriState.INDETERMINATE, defaultProfile.getCategoryState("Ktor", registry));

        // Kubernetes: 13 inspections from Image 3
        List<InspectionTool> k8sTools = registry.getToolsForCategory("Kubernetes");
        assertEquals(13, k8sTools.size());
        assertTrue(k8sTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Kubernetes", registry));

        // Language injection: 3 inspections from Image 3
        List<InspectionTool> liTools = registry.getToolsForCategory("Language injection");
        assertEquals(3, liTools.size());
        assertTrue(liTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Language injection", registry));

        // Less: 3 inspections from Image 3
        List<InspectionTool> lessTools = registry.getToolsForCategory("Less");
        assertEquals(3, lessTools.size());
        assertTrue(lessTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Less", registry));

        // Liquibase: 2 inspections from Image 3
        List<InspectionTool> lbTools = registry.getToolsForCategory("Liquibase");
        assertEquals(2, lbTools.size());
        assertTrue(lbTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Liquibase", registry));

        // Manifest: 2 inspections from Image 3
        List<InspectionTool> manifestTools = registry.getToolsForCategory("Manifest");
        assertEquals(2, manifestTools.size());
        assertTrue(manifestTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Manifest", registry));

        // Markdown: 8 inspections from Image 3
        List<InspectionTool> mdTools = registry.getToolsForCategory("Markdown");
        assertEquals(8, mdTools.size());
        assertTrue(mdTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Markdown", registry));

        // Maven: 16 inspections from Image 4
        List<InspectionTool> mavenTools = registry.getToolsForCategory("Maven");
        assertEquals(16, mavenTools.size());
        assertTrue(mavenTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Maven", registry));

        // Micronaut: 9 inspections (1 in Micronaut, 3 in Micronaut Data, 5 direct) from Image 4
        List<InspectionTool> mnTools = registry.getToolsForCategory("Micronaut");
        assertEquals(9, mnTools.size());
        assertEquals(1, registry.getToolsDirectlyInCategory("Micronaut/Micronaut").size());
        assertEquals(3, registry.getToolsDirectlyInCategory("Micronaut/Micronaut Data").size());
        assertEquals(5, registry.getToolsDirectlyInCategory("Micronaut").size());
        assertTrue(mnTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Micronaut", registry));

        // MongoJS: 6 inspections from Image 4
        List<InspectionTool> mongoTools = registry.getToolsForCategory("MongoJS");
        assertEquals(6, mongoTools.size());
        assertTrue(mongoTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("MongoJS", registry));

        // OpenAPI specifications: 4 inspections from Image 5
        List<InspectionTool> openApiTools = registry.getToolsForCategory("OpenAPI specifications");
        assertEquals(4, openApiTools.size());
        assertTrue(openApiTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("OpenAPI specifications", registry));

        // Oracle: 3 inspections from Image 5
        List<InspectionTool> oracleTools = registry.getToolsForCategory("Oracle");
        assertEquals(3, oracleTools.size());
        assertTrue(oracleTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Oracle", registry));

        // Pandas: 2 inspections from Image 5
        List<InspectionTool> pandasTools = registry.getToolsForCategory("Pandas");
        assertEquals(2, pandasTools.size());
        assertTrue(pandasTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Pandas", registry));

        // Pattern validation: 3 inspections from Image 5
        List<InspectionTool> pvTools = registry.getToolsForCategory("Pattern validation");
        assertEquals(3, pvTools.size());
        assertTrue(pvTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(InspectionProfile.TriState.CHECKED, defaultProfile.getCategoryState("Pattern validation", registry));

        // PHP: 20 subcategories from Image 5
        List<String> phpSubCats = registry.getAllSubCategories("PHP");
        assertEquals(20, phpSubCats.size());
        assertEquals(InspectionProfile.TriState.INDETERMINATE, defaultProfile.getCategoryState("PHP", registry));

        List<String> uncheckedPhpSubCats = List.of(
                "PHP/Naming conventions",
                "PHP/Psalm",
                "PHP/Quality tools"
        );
        for (String subCat : uncheckedPhpSubCats) {
            assertEquals(InspectionProfile.TriState.UNCHECKED, defaultProfile.getCategoryState(subCat, registry),
                    "Expected UNCHECKED for " + subCat);
        }
    }

    @Test
    void testSettingsInspectionsPageUI() throws Exception {
        if (!javaFxAvailable) {
            System.out.println("JavaFX not available, skipping testSettingsInspectionsPageUI");
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInspectionsPage page = new SettingsInspectionsPage();
                assertNotNull(page);

                // Tree view verification
                assertNotNull(page.getTreeView());
                assertNotNull(page.getTreeView().getRoot());
                assertFalse(page.getTreeView().getRoot().getChildren().isEmpty());

                // Add Menu verification (screenshot 2)
                assertNotNull(page.getAddMenuButton());
                assertEquals(4, page.getAddMenuButton().getItems().size());
                assertEquals("Add Structural Search Inspection...", page.getAddMenuButton().getItems().get(0).getText());
                assertEquals("Add Structural Replace Inspection...", page.getAddMenuButton().getItems().get(1).getText());
                assertEquals("Add RegExp Search Inspection...", page.getAddMenuButton().getItems().get(2).getText());
                assertEquals("Add RegExp Replace Inspection...", page.getAddMenuButton().getItems().get(3).getText());

                // Filter Menu verification (screenshot 1)
                assertNotNull(page.getFilterMenuButton());
                assertFalse(page.getFilterMenuButton().getItems().isEmpty());
                assertTrue(page.getFilterMenuButton().getItems().stream()
                        .anyMatch(mi -> "Reset Filter".equals(mi.getText())));
                assertTrue(page.getFilterMenuButton().getItems().stream()
                        .anyMatch(mi -> mi.getText() != null && mi.getText().contains("Show New Inspections")));
                assertTrue(page.getFilterMenuButton().getItems().stream()
                        .anyMatch(mi -> "Show only enabled".equals(mi.getText())));
                assertTrue(page.getFilterMenuButton().getItems().stream()
                        .anyMatch(mi -> "Show only disabled".equals(mi.getText())));
                assertTrue(page.getFilterMenuButton().getItems().stream()
                        .anyMatch(mi -> "Show only modified inspections".equals(mi.getText())));
                assertTrue(page.getFilterMenuButton().getItems().stream()
                        .anyMatch(mi -> "Filter by Language".equals(mi.getText())));

                // Search field test
                page.getSearchField().setText("Application servers");
                assertFalse(page.getTreeView().getRoot().getChildren().isEmpty());

                // Bottom controls test
                assertNotNull(page.getScopeButton());
                assertNotNull(page.getSeverityButton());
                assertNotNull(page.getHighlightingCombo());
                assertNotNull(page.getDisableNewInspectionsCheck());

                // Mixed icon verification
                assertNotNull(HighlightSeverity.createMixedIcon(12));

                // Modification lifecycle test
                assertFalse(page.isModified());
                page.getDisableNewInspectionsCheck().setSelected(true);
                page.getWorkingProfile().setDisableNewInspections(true);
                assertTrue(page.isModified());

                page.reset();
                assertFalse(page.getWorkingProfile().isDisableNewInspections());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testPostCssInspections() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        List<InspectionTool> tools = registry.getToolsForCategory("PostCSS");
        assertEquals(5, tools.size(), "PostCSS must have 5 inspections matching screenshot 1");

        InspectionTool media = registry.getTool("PostCSS.InvalidCustomMedia");
        assertNotNull(media);
        assertEquals("Invalid custom media", media.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, media.getDefaultSeverity());
        assertTrue(media.isDefaultEnabled());

        InspectionTool selector = registry.getTool("PostCSS.InvalidCustomSelector");
        assertNotNull(selector);
        assertEquals(HighlightSeverity.ERROR, selector.getDefaultSeverity());
        assertTrue(selector.isDefaultEnabled());

        InspectionTool range = registry.getTool("PostCSS.InvalidMediaQueryRange");
        assertNotNull(range);
        assertEquals(HighlightSeverity.ERROR, range.getDefaultSeverity());
        assertTrue(range.isDefaultEnabled());

        InspectionTool nested = registry.getTool("PostCSS.InvalidNestedRule");
        assertNotNull(nested);
        assertFalse(nested.isDefaultEnabled(), "Invalid nested rule must be disabled by default");

        InspectionTool moduleVal = registry.getTool("PostCSS.UnresolvedCssModuleValue");
        assertNotNull(moduleVal);
        assertEquals(HighlightSeverity.ERROR, moduleVal.getDefaultSeverity());
        assertTrue(moduleVal.isDefaultEnabled());

        InspectionProfile profile = new InspectionProfile("TestProfile", true);
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("PostCSS", registry));
    }

    @Test
    void testPostgreSqlAndProofreadingAndPropertiesInspections() {
        InspectionRegistry registry = InspectionRegistry.getInstance();

        // PostgreSQL
        List<InspectionTool> pgTools = registry.getToolsForCategory("PostgreSQL");
        assertEquals(1, pgTools.size());
        assertEquals("Postgres: Select from procedure call", pgTools.get(0).getDisplayName());
        assertEquals(HighlightSeverity.WARNING, pgTools.get(0).getDefaultSeverity());
        assertTrue(pgTools.get(0).isDefaultEnabled());

        // Proofreading
        List<InspectionTool> proofTools = registry.getToolsForCategory("Proofreading");
        assertEquals(3, proofTools.size());
        assertTrue(proofTools.stream().anyMatch(t -> "Grammar".equals(t.getDisplayName())));
        assertTrue(proofTools.stream().anyMatch(t -> "Natural language detection".equals(t.getDisplayName())));
        assertTrue(proofTools.stream().anyMatch(t -> "Typo".equals(t.getDisplayName())));

        // Properties files
        List<InspectionTool> propTools = registry.getToolsForCategory("Properties files");
        assertEquals(9, propTools.size(), "Properties files must have 9 inspections matching screenshot 1");

        InspectionTool dupProp = registry.getTool("Properties.DuplicateProperty");
        assertNotNull(dupProp);
        assertFalse(dupProp.isDefaultEnabled());
        assertTrue(dupProp.isBatchModeOnly());

        InspectionTool bundle = registry.getTool("Properties.InconsistentResourceBundle");
        assertNotNull(bundle);
        assertTrue(bundle.isDefaultEnabled());
        assertEquals(HighlightSeverity.ERROR, bundle.getDefaultSeverity());
        assertTrue(bundle.isBatchModeOnly());

        InspectionTool msgFmt = registry.getTool("Properties.MissingMessageFormatParameter");
        assertNotNull(msgFmt);
        assertTrue(msgFmt.isDefaultEnabled());
        assertEquals(HighlightSeverity.WARNING, msgFmt.getDefaultSeverity());

        InspectionTool unsorted = registry.getTool("Properties.AlphabeticallyUnsorted");
        assertNotNull(unsorted);
        assertFalse(unsorted.isDefaultEnabled());

        InspectionTool delimiter = registry.getTool("Properties.DelimiterMismatch");
        assertNotNull(delimiter);
        assertTrue(delimiter.isDefaultEnabled());

        InspectionTool suspicious = registry.getTool("Properties.SuspiciousLocaleLanguages");
        assertNotNull(suspicious);
        assertFalse(suspicious.isDefaultEnabled());

        InspectionTool threeDots = registry.getTool("Properties.ThreeDotCharacters");
        assertNotNull(threeDots);
        assertFalse(threeDots.isDefaultEnabled());

        InspectionTool trailing = registry.getTool("Properties.TrailingSpaces");
        assertNotNull(trailing);
        assertTrue(trailing.isDefaultEnabled());

        InspectionTool unused = registry.getTool("Properties.UnusedProperty");
        assertNotNull(unused);
        assertTrue(unused.isDefaultEnabled());

        InspectionProfile profile = new InspectionProfile("TestProfile", true);
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Properties files", registry));
    }

    @Test
    void testProtocolBuffersAndPyramidInspections() {
        InspectionRegistry registry = InspectionRegistry.getInstance();

        List<InspectionTool> protoTools = registry.getToolsForCategory("Protocol Buffers");
        assertEquals(1, protoTools.size());
        assertEquals("Duplicated import statements", protoTools.get(0).getDisplayName());
        assertTrue(protoTools.get(0).isDefaultEnabled());

        List<InspectionTool> pyramidTools = registry.getToolsForCategory("Pyramid");
        assertEquals(1, pyramidTools.size());
        assertEquals("Project is not installed for development", pyramidTools.get(0).getDisplayName());
        assertTrue(pyramidTools.get(0).isDefaultEnabled());
    }

    @Test
    void testPythonInspectionsAndOptions() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        List<InspectionTool> pyTools = registry.getToolsForCategory("Python");
        assertTrue(pyTools.size() >= 75, "Python must have 75+ inspections matching screenshots 2, 3, 4");

        // Subcategory Security
        List<String> subCats = registry.getAllSubCategories("Python");
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Python/Security")));
        List<InspectionTool> secTools = registry.getToolsDirectlyInCategory("Python/Security");
        assertEquals(1, secTools.size());
        assertEquals("Vulnerable API usage", secTools.get(0).getDisplayName());

        // CommandLineInspection
        InspectionTool cliTool = registry.getTool("CommandLineInspection");
        assertNotNull(cliTool);
        assertEquals("Incorrect CLI syntax", cliTool.getDisplayName());
        assertEquals(HighlightSeverity.WARNING, cliTool.getDefaultSeverity());
        assertTrue(cliTool.isDefaultEnabled());
        assertTrue(cliTool.getDescription().contains("manage.py in Django"));

        // PyRedundantParenthesesInspection with options
        InspectionTool parensTool = registry.getTool("PyRedundantParenthesesInspection");
        assertNotNull(parensTool);
        assertEquals("Redundant parentheses", parensTool.getDisplayName());
        assertEquals(HighlightSeverity.WEAK_WARNING, parensTool.getDefaultSeverity());
        assertTrue(parensTool.isDefaultEnabled());
        assertTrue(parensTool.hasOptions());
        assertEquals(3, parensTool.getOptions().size());
        assertEquals("ignoreArgumentOfPercentOperator", parensTool.getOptions().get(0).getId());
        assertEquals("Ignore argument of % operator", parensTool.getOptions().get(0).getLabel());
        assertEquals("ignoreTuples", parensTool.getOptions().get(1).getId());
        assertEquals("Ignore tuples", parensTool.getOptions().get(1).getLabel());
        assertEquals("ignoreEmptyListsOfBaseClasses", parensTool.getOptions().get(2).getId());
        assertEquals("Ignore empty lists of base classes", parensTool.getOptions().get(2).getLabel());

        // Option profile storage
        InspectionProfile profile = new InspectionProfile("TestProfile", true);
        assertFalse(profile.getOptionBoolean("PyRedundantParenthesesInspection", "ignoreTuples"));
        profile.setOptionBoolean("PyRedundantParenthesesInspection", "ignoreTuples", true);
        assertTrue(profile.getOptionBoolean("PyRedundantParenthesesInspection", "ignoreTuples"));
        assertTrue(profile.isModified(registry));

        // Category state for Python must be INDETERMINATE
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Python", registry));
    }

    @Test
    void testQodanaQuarkusAndRbsInspections() {
        InspectionRegistry registry = InspectionRegistry.getInstance();

        // Qodana
        List<InspectionTool> qodanaTools = registry.getToolsForCategory("Qodana");
        assertEquals(1, qodanaTools.size());
        assertEquals("Code metrics", qodanaTools.get(0).getDisplayName());
        assertFalse(qodanaTools.get(0).isDefaultEnabled(), "Qodana Code metrics must be disabled by default");

        InspectionProfile profile = new InspectionProfile("TestProfile", true);
        assertEquals(InspectionProfile.TriState.UNCHECKED, profile.getCategoryState("Qodana", registry));

        // Quarkus
        List<InspectionTool> quarkusTools = registry.getToolsForCategory("Quarkus");
        assertEquals(5, quarkusTools.size(), "Quarkus must have 5 inspections matching screenshot 5");
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Quarkus", registry));

        // RBS (27 inspections across 5 subcategories)
        List<InspectionTool> rbsTools = registry.getToolsForCategory("RBS");
        assertEquals(27, rbsTools.size(), "RBS must have 27 inspections matching screenshots");
        assertTrue(rbsTools.stream().anyMatch(t -> t.getDisplayName().equals("Invalid method overload")));
        assertTrue(rbsTools.stream().anyMatch(t -> t.getDisplayName().equals("Invalid type argument usage")));
        assertTrue(rbsTools.stream().anyMatch(t -> t.getDisplayName().equals("Unresolved reference")));
        List<String> rbsSubCats = registry.getAllSubCategories("RBS");
        assertEquals(5, rbsSubCats.size(), "RBS must have 5 subcategories matching screenshots");
        assertTrue(rbsSubCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("RBS/Code style issues")));
        assertTrue(rbsSubCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("RBS/Data flow")));
        assertTrue(rbsSubCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("RBS/Inheritance issues")));
        assertTrue(rbsSubCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("RBS/Naming conventions")));
        assertTrue(rbsSubCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("RBS/Probable bugs")));

        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("RBS", registry));
    }

    @Test
    void testSettingsInspectionsPageOptionsAndDetailsUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInspectionsPage page = new SettingsInspectionsPage();
                assertNotNull(page);

                // 1. Search and select PyRedundantParenthesesInspection
                page.getSearchField().setText("Redundant parentheses");
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> foundItem = null;
                for (TreeItem<SettingsInspectionsPage.InspectionTreeNode> cat : page.getTreeView().getRoot().getChildren()) {
                    for (TreeItem<SettingsInspectionsPage.InspectionTreeNode> child : cat.getChildren()) {
                        if (child.getValue() != null && child.getValue().getTool() != null &&
                                "PyRedundantParenthesesInspection".equals(child.getValue().getTool().getId())) {
                            foundItem = child;
                            break;
                        }
                    }
                    if (foundItem != null) break;
                }
                assertNotNull(foundItem, "PyRedundantParenthesesInspection must be in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(foundItem);

                // Description contains Inspection ID
                assertTrue(page.getToolDescArea().getText().contains("Inspection ID: PyRedundantParenthesesInspection"));
                assertTrue(page.getToolDescArea().getText().contains("Reports about redundant parentheses"));

                // Options container visible with 3 checkboxes
                assertTrue(page.getOptionsContainer().isVisible());
                assertEquals(4, page.getOptionsContainer().getChildren().size(), "1 Options label + 3 CheckBoxes");

                // Toggle option and check dirty tracking
                assertFalse(page.isModified());
                javafx.scene.control.CheckBox cb = (javafx.scene.control.CheckBox) page.getOptionsContainer().getChildren().get(1);
                cb.setSelected(true);
                cb.fireEvent(new javafx.event.ActionEvent());
                assertTrue(page.isModified(), "Page must become modified when option is changed");

                // Apply
                page.apply();
                assertFalse(page.isModified());

                // Reset
                cb.setSelected(false);
                cb.fireEvent(new javafx.event.ActionEvent());
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testReactiveStreamsInspections() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        List<InspectionTool> tools = registry.getToolsForCategory("Reactive Streams");
        assertEquals(12, tools.size(), "Reactive Streams must have 12 inspections across 3 subcategories");

        List<String> subCats = registry.getAllSubCategories("Reactive Streams");
        assertEquals(3, subCats.size());
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Reactive Streams/Common")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Reactive Streams/Mutiny")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Reactive Streams/Reactor")));

        // Common
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Class implements Publisher")));
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Class implements Subscriber")));
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Return null or something nullable from a lambda in transformation method")));
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Throw statement in Reactive operator")));
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Too long same methods chain")));
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Unused publisher")));

        // Mutiny
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Calling 'subscribe' in \"reactive\" methods") && t.getGroupPath().equals("Reactive Streams/Mutiny")));

        // Reactor
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Calling transformation function on receiver with Mono/Flux publisher")));
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Unfinished StepVerifier")));
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Unnecessary debug initialization")));
        assertTrue(tools.stream().anyMatch(t -> t.getDisplayName().equals("Zip contains parameter with Mono<Void> type")));

        InspectionProfile profile = new InspectionProfile("TestProfile", true);
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Reactive Streams", registry));
    }

    @Test
    void testRegExpRelaxNgRequirementsAndJaxRsInspections() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        // RegExp (15 tools, 1 disabled -> indeterminate)
        List<InspectionTool> regExpTools = registry.getToolsForCategory("RegExp");
        assertEquals(15, regExpTools.size(), "RegExp must have 15 inspections matching screenshot 2");
        InspectionTool anonGroup = registry.getTool("RegExp.AnonymousCapturingGroup");
        assertNotNull(anonGroup);
        assertFalse(anonGroup.isDefaultEnabled());
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("RegExp", registry));

        // RELAX NG (2 tools: 1 enabled, 1 disabled -> indeterminate)
        List<InspectionTool> relaxNgTools = registry.getToolsForCategory("RELAX NG");
        assertEquals(2, relaxNgTools.size(), "RELAX NG must have 2 inspections matching screenshot 2");
        assertTrue(relaxNgTools.stream().anyMatch(t -> t.getDisplayName().equals("Unresolved reference") && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertTrue(relaxNgTools.stream().anyMatch(t -> t.getDisplayName().equals("Unused define") && !t.isDefaultEnabled()));
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("RELAX NG", registry));

        // Requirements (2 tools: both enabled -> checked)
        List<InspectionTool> reqTools = registry.getToolsForCategory("Requirements");
        assertEquals(2, reqTools.size(), "Requirements must have 2 inspections matching screenshot 2");
        assertTrue(reqTools.stream().anyMatch(t -> t.getDisplayName().equals("Requirement is not satisfied")));
        assertTrue(reqTools.stream().anyMatch(t -> t.getDisplayName().equals("Requirement is outdated")));
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Requirements", registry));

        // RESTful Web Service (JAX-RS) (7 tools: all enabled -> checked)
        List<InspectionTool> jaxRsTools = registry.getToolsForCategory("RESTful Web Service (JAX-RS)");
        assertEquals(7, jaxRsTools.size(), "RESTful Web Service (JAX-RS) must have 7 inspections matching screenshot 2");
        assertTrue(jaxRsTools.stream().anyMatch(t -> t.getDisplayName().equals("@GET annotated method returns a void value")));
        assertTrue(jaxRsTools.stream().anyMatch(t -> t.getDisplayName().equals("Incorrect @Path URI template")));
        assertTrue(jaxRsTools.stream().anyMatch(t -> t.getDisplayName().equals("Incorrect value of @DefaultValue parameter")));
        assertTrue(jaxRsTools.stream().anyMatch(t -> t.getDisplayName().equals("Incorrect WADL configuration") && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertTrue(jaxRsTools.stream().anyMatch(t -> t.getDisplayName().equals("@Path class without resource methods") && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertTrue(jaxRsTools.stream().anyMatch(t -> t.getDisplayName().equals("Resource method with multiple HTTP method annotations") && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertTrue(jaxRsTools.stream().anyMatch(t -> t.getDisplayName().equals("Unresolved @PathParam reference") && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("RESTful Web Service (JAX-RS)", registry));
    }

    @Test
    void testRubyInspectionsAndOptions() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        // Ruby tools and subcategories
        List<InspectionTool> rubyTools = registry.getToolsForCategory("Ruby");
        assertTrue(rubyTools.size() >= 70, "Ruby must have complete inspections tree");

        List<String> subCats = registry.getAllSubCategories("Ruby");
        assertEquals(14, subCats.size(), "Ruby must have 14 subcategories matching screenshots");
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Code metrics")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Code style issues")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Control flow issues")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Data flow")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Gems and gem management")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/General")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Inheritance issues")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Language level migration aids")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Naming conventions")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Probable bugs")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Rails")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/RBS")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/Redundant code")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Ruby/YARD")));

        // Code style issues has 2 disabled tools -> INDETERMINATE
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Ruby/Code style issues", registry));
        // And overall Ruby category is INDETERMINATE
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Ruby", registry));

        // RubyMismatchedGlobalVariableType inspection tool verification (screenshot 5)
        InspectionTool globalVarTool = registry.getTool("RubyMismatchedGlobalVariableType");
        assertNotNull(globalVarTool, "RubyMismatchedGlobalVariableType tool must exist");
        assertEquals("Mismatched global variable type", globalVarTool.getDisplayName());
        assertEquals("Ruby/Probable bugs", globalVarTool.getGroupPath());
        assertEquals(HighlightSeverity.WARNING, globalVarTool.getDefaultSeverity());
        assertTrue(globalVarTool.isDefaultEnabled());
        assertTrue(globalVarTool.hasOptions(), "RubyMismatchedGlobalVariableType must have custom options");
        assertEquals(2, globalVarTool.getOptions().size(), "Must have exactly 2 options matching screenshot 5");

        InspectionTool.Option opt1 = globalVarTool.getOptions().get(0);
        assertEquals("checkTypesFromRbs", opt1.getId());
        assertEquals("Check types from RBS", opt1.getLabel());
        assertTrue(opt1.isDefaultBooleanValue());

        InspectionTool.Option opt2 = globalVarTool.getOptions().get(1);
        assertEquals("checkNilability", opt2.getId());
        assertEquals("Check nilability", opt2.getLabel());
        assertTrue(opt2.isDefaultBooleanValue());
    }

    @Test
    void testRubyGlobalVariableTypeOptionsInUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInspectionsPage page = new SettingsInspectionsPage();
                assertNotNull(page);

                page.getSearchField().setText("Mismatched global variable type");
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> foundItem = findNodeRecursively(page.getTreeView().getRoot(), "RubyMismatchedGlobalVariableType");
                assertNotNull(foundItem, "RubyMismatchedGlobalVariableType must be in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(foundItem);

                assertTrue(page.getToolDescArea().getText().contains("Reports global variable assignments whose inferred types don't match"));
                assertTrue(page.getToolDescArea().getText().contains("Inspection ID: RubyMismatchedGlobalVariableType"));

                // Options container: 1 label ("Options") + 2 CheckBoxes
                assertTrue(page.getOptionsContainer().isVisible());
                assertEquals(3, page.getOptionsContainer().getChildren().size(), "1 Options label + 2 CheckBoxes");

                javafx.scene.control.CheckBox cbRbs = (javafx.scene.control.CheckBox) page.getOptionsContainer().getChildren().get(1);
                assertEquals("Check types from RBS", cbRbs.getText());
                assertTrue(cbRbs.isSelected());

                javafx.scene.control.CheckBox cbNil = (javafx.scene.control.CheckBox) page.getOptionsContainer().getChildren().get(2);
                assertEquals("Check nilability", cbNil.getText());
                assertTrue(cbNil.isSelected());

                // Dirty tracking check
                assertFalse(page.isModified());
                cbNil.setSelected(false);
                cbNil.fireEvent(new javafx.event.ActionEvent());
                assertTrue(page.isModified());

                // Apply
                page.apply();
                assertFalse(page.isModified());

                // Reset
                cbNil.setSelected(true);
                cbNil.fireEvent(new javafx.event.ActionEvent());
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRustInspectionsTreeAndMetadata() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        // Rust overall category: 230 inspections
        List<InspectionTool> rustTools = registry.getToolsForCategory("Rust");
        assertEquals(230, rustTools.size(), "Rust must contain 230 inspections matching screenshots");

        // Subcategories
        List<String> subCats = registry.getAllSubCategories("Rust");
        assertEquals(2, subCats.size());
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Rust/Cargo.toml")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Rust/Lints")));

        // Nested subcategory under Rust/Lints
        List<String> lintsSubCats = registry.getAllSubCategories("Rust/Lints");
        assertEquals(1, lintsSubCats.size());
        assertTrue(lintsSubCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Rust/Lints/Naming conventions")));

        // Cargo.toml: 9 inspections, all enabled -> CHECKED
        List<InspectionTool> cargoTools = registry.getToolsForCategory("Rust/Cargo.toml");
        assertEquals(9, cargoTools.size());
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Rust/Cargo.toml", registry));
        assertTrue(cargoTools.stream().anyMatch(t -> t.getDisplayName().equals("Crate not found")));
        assertTrue(cargoTools.stream().anyMatch(t -> t.getDisplayName().equals("Cyclic feature dependency") && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertTrue(cargoTools.stream().anyMatch(t -> t.getDisplayName().equals("Duplicate key") && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertTrue(cargoTools.stream().anyMatch(t -> t.getDisplayName().equals("Invalid crate version") && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertTrue(cargoTools.stream().anyMatch(t -> t.getDisplayName().equals("Newer crate version available")));
        assertTrue(cargoTools.stream().anyMatch(t -> t.getDisplayName().equals("Schema violation") && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertTrue(cargoTools.stream().anyMatch(t -> t.getDisplayName().equals("Unclosed string literal") && t.getDefaultSeverity() == HighlightSeverity.ERROR));
        assertTrue(cargoTools.stream().anyMatch(t -> t.getDisplayName().equals("Unnecessary `package` field")));
        assertTrue(cargoTools.stream().anyMatch(t -> t.getDisplayName().equals("Unused dependency")));

        // Naming conventions: 18 inspections, all enabled -> CHECKED
        List<InspectionTool> namingTools = registry.getToolsForCategory("Rust/Lints/Naming conventions");
        assertEquals(18, namingTools.size());
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Rust/Lints/Naming conventions", registry));
        assertTrue(namingTools.stream().anyMatch(t -> t.getDisplayName().equals("Argument naming convention")));
        assertTrue(namingTools.stream().anyMatch(t -> t.getDisplayName().equals("Function naming convention")));
        assertTrue(namingTools.stream().anyMatch(t -> t.getDisplayName().equals("Variable naming convention")));

        // Lints (including naming conventions): 39 inspections, all enabled -> CHECKED
        List<InspectionTool> lintsTools = registry.getToolsForCategory("Rust/Lints");
        assertEquals(39, lintsTools.size());
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Rust/Lints", registry));
        assertTrue(lintsTools.stream().anyMatch(t -> t.getDisplayName().equals("Unknown crate types") && t.getDefaultSeverity() == HighlightSeverity.ERROR));

        // Screenshot-selected inspection tools
        InspectionTool liveness = registry.getTool("RsLiveness");
        assertNotNull(liveness);
        assertEquals("Liveness analysis", liveness.getDisplayName());
        assertEquals("Rust/Lints", liveness.getGroupPath());
        assertEquals(HighlightSeverity.WARNING, liveness.getDefaultSeverity());
        assertEquals("Reports unused local variables and parameters.", liveness.getDescription());

        InspectionTool charLiteral = registry.getTool("RsInvalidCharLiteralLength");
        assertNotNull(charLiteral);
        assertEquals("Char literal empty or too long", charLiteral.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, charLiteral.getDefaultSeverity());
        assertEquals("Reports char literals which are empty or too long.", charLiteral.getDescription());

        InspectionTool formatMacro = registry.getTool("RsFormatMacroErrors");
        assertNotNull(formatMacro);
        assertEquals("Format macro error", formatMacro.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, formatMacro.getDefaultSeverity());
        assertEquals("Reports errors in the use of macros that support formatting.", formatMacro.getDescription());

        InspectionTool placeExpr = registry.getTool("RsPlaceExpression");
        assertNotNull(placeExpr);
        assertEquals("Invalid place expression", placeExpr.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, placeExpr.getDefaultSeverity());
        assertEquals("Reports invalid place expressions. For example: `1 + 1 = 2`.", placeExpr.getDescription());

        InspectionTool extLinter = registry.getTool("RsExternalLinter");
        assertNotNull(extLinter);
        assertTrue(extLinter.isBatchModeOnly());

        // Screenshot 1 & 2 selected inspections:
        InspectionTool paramSized = registry.getTool("RsFunctionParametersAreSized");
        assertNotNull(paramSized);
        assertEquals("Parameter type with unknown size", paramSized.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, paramSized.getDefaultSeverity());
        assertTrue(paramSized.isDefaultEnabled());
        assertTrue(paramSized.getDescription().contains("Inspection ID: RsFunctionParametersAreSized"));

        InspectionTool traitOblig = registry.getTool("RsTraitObligations");
        assertNotNull(traitOblig);
        assertEquals("Type does not implement trait", traitOblig.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, traitOblig.getDefaultSeverity());
        assertTrue(traitOblig.isDefaultEnabled());
        assertTrue(traitOblig.getDescription().contains("Inspection ID: RsTraitObligations"));

        // Disabled experimental tools in screenshots 1 & 2
        assertFalse(registry.getTool("RsTypeCheckerExperimental").isDefaultEnabled());
        assertFalse(registry.getTool("RsTraitObligationsExperimental").isDefaultEnabled());
        assertFalse(registry.getTool("RsTypeMismatchedTraitAssociatedType").isDefaultEnabled());
        assertFalse(registry.getTool("RsUnresolvedMethodExperimental").isDefaultEnabled());
        assertFalse(registry.getTool("RsUnresolvedPathExperimental").isDefaultEnabled());

        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Rust", registry));
    }

    @Test
    void testRustInspectionsUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInspectionsPage page = new SettingsInspectionsPage();
                assertNotNull(page);

                // Select Liveness analysis
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> livenessNode = findNodeRecursively(page.getTreeView().getRoot(), "RsLiveness");
                assertNotNull(livenessNode, "RsLiveness must be found in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(livenessNode);

                assertTrue(page.getToolDescArea().getText().contains("Reports unused local variables and parameters."));
                assertTrue(page.getToolDescArea().getText().contains("Inspection ID: RsLiveness"));
                assertEquals("Warning", page.getSeverityButton().getText().trim());

                // Select Invalid place expression
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> placeNode = findNodeRecursively(page.getTreeView().getRoot(), "RsPlaceExpression");
                assertNotNull(placeNode, "RsPlaceExpression must be found in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(placeNode);

                assertTrue(page.getToolDescArea().getText().contains("Reports invalid place expressions. For example: `1 + 1 = 2`."));
                assertTrue(page.getToolDescArea().getText().contains("Inspection ID: RsPlaceExpression"));
                assertEquals("Error", page.getSeverityButton().getText().trim());

                // Select Parameter type with unknown size (Image 1)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> paramNode = findNodeRecursively(page.getTreeView().getRoot(), "RsFunctionParametersAreSized");
                assertNotNull(paramNode, "RsFunctionParametersAreSized must be found in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(paramNode);

                assertTrue(page.getToolDescArea().getText().contains("Reports function parameters whose types are unknown at compile time."));
                assertTrue(page.getToolDescArea().getText().contains("Inspection ID: RsFunctionParametersAreSized"));
                assertEquals("Error", page.getSeverityButton().getText().trim());

                // Select Type does not implement trait (Image 2)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> traitNode = findNodeRecursively(page.getTreeView().getRoot(), "RsTraitObligations");
                assertNotNull(traitNode, "RsTraitObligations must be found in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(traitNode);

                assertTrue(page.getToolDescArea().getText().contains("Reports the use of types that do not implement the necessary traits."));
                assertTrue(page.getToolDescArea().getText().contains("Inspection ID: RsTraitObligations"));
                assertEquals("Error", page.getSeverityButton().getText().trim());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testBatchFourUIParityAndDetails() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInspectionsPage page = new SettingsInspectionsPage();
                assertNotNull(page);

                // Select Security category: multiple items selected banner
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> secCatNode = findCategoryNode(page.getTreeView().getRoot(), "Security");
                assertNotNull(secCatNode);
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(secCatNode);
                assertTrue(page.getMultiSelectionLabel().isVisible());
                assertEquals("Multiple inspections are selected. You can edit them as a single inspection.", page.getMultiSelectionLabel().getText());

                // Select Vulnerable imported dependency (Image 3)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> vulnImpNode = findNodeRecursively(page.getTreeView().getRoot(), "Security.VulnerableImportedDependency");
                assertNotNull(vulnImpNode);
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(vulnImpNode);
                assertFalse(page.getMultiSelectionLabel().isVisible());
                assertTrue(page.getToolDescArea().getText().contains("Reports transitive imported dependencies containing security vulnerabilities."));
                assertEquals("Warning", page.getSeverityButton().getText().trim());

                // Select Spring Data JDBC associated DB elements (Image 4)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> jdbcNode = findNodeRecursively(page.getTreeView().getRoot(), "SpringData.JdbcAssociatedDbElements");
                assertNotNull(jdbcNode);
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(jdbcNode);
                assertEquals("Error", page.getSeverityButton().getText().trim());
                assertTrue(page.getToolDescArea().getText().contains("Validates database tables, columns, and relationships"));

                // Select Adding not null column without default value (Image 5)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> addNotNullNode = findNodeRecursively(page.getTreeView().getRoot(), "SqlAddNotNullColumn");
                assertNotNull(addNotNullNode);
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(addNotNullNode);
                assertEquals("Warning", page.getSeverityButton().getText().trim());
                assertTrue(page.getToolDescArea().getText().contains("Reports attempts to add a NOT NULL column"));
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSassInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> sassTools = registry.getToolsForCategory("Sass/SCSS");
        assertEquals(4, sassTools.size(), "Sass/SCSS must contain 4 inspections matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Sass/SCSS", registry));

        for (InspectionTool tool : sassTools) {
            assertTrue(tool.isDefaultEnabled());
            assertEquals(HighlightSeverity.WARNING, tool.getDefaultSeverity());
            assertEquals("SCSS", tool.getLanguage());
        }

        assertNotNull(registry.getTool("Sass.MissingImport"));
        assertNotNull(registry.getTool("Sass.UnresolvedMixin"));
        assertNotNull(registry.getTool("Sass.UnresolvedPlaceholderSelector"));
        assertNotNull(registry.getTool("Sass.UnresolvedVariable"));
    }

    @Test
    void testSbtInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> sbtTools = registry.getToolsForCategory("sbt");
        assertEquals(2, sbtTools.size(), "sbt must contain 2 inspections matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("sbt", registry));

        for (InspectionTool tool : sbtTools) {
            assertTrue(tool.isDefaultEnabled());
            assertEquals(HighlightSeverity.WARNING, tool.getDefaultSeverity());
            assertEquals("sbt", tool.getLanguage());
        }

        assertNotNull(registry.getTool("Sbt.NewerVersionAvailable"));
        assertNotNull(registry.getTool("Sbt.ReplaceProjectWithProjectIn"));
    }

    @Test
    void testSecurityInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> secTools = registry.getToolsForCategory("Security");
        assertEquals(5, secTools.size(), "Security must contain 5 inspections matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Security", registry));

        for (InspectionTool tool : secTools) {
            assertTrue(tool.isDefaultEnabled());
            assertEquals(HighlightSeverity.WARNING, tool.getDefaultSeverity());
        }

        InspectionTool importedDep = registry.getTool("Security.VulnerableImportedDependency");
        assertNotNull(importedDep);
        assertTrue(importedDep.isBatchModeOnly());
        assertEquals("Vulnerable imported dependency", importedDep.getDisplayName());
    }

    @Test
    void testShellScriptInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> shellTools = registry.getToolsForCategory("Shell script");
        assertEquals(1, shellTools.size(), "Shell script must contain 1 inspection matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Shell script", registry));

        InspectionTool sc = registry.getTool("Shell.ShellCheck");
        assertNotNull(sc);
        assertEquals("ShellCheck", sc.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, sc.getDefaultSeverity());
        assertTrue(sc.isDefaultEnabled());
    }

    @Test
    void testSpringInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<String> subCats = registry.getAllSubCategories("Spring");
        assertEquals(10, subCats.size(), "Spring must have 10 subcategories matching screenshot");
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring AOP")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring Boot")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring Cloud")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring Cloud Stream")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring Core")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring Data")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring Integration")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring MVC")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring Security")));
        assertTrue(subCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring WebSocket")));

        // Spring overall category state is CHECKED
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Spring", registry));
        for (String sub : subCats) {
            assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState(sub, registry), "Subcategory " + sub + " must be CHECKED");
        }
    }

    @Test
    void testSpringAopAndBootParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        // Spring AOP (5 tools from Image 1)
        List<InspectionTool> aopTools = registry.getToolsForCategory("Spring/Spring AOP");
        assertEquals(5, aopTools.size(), "Spring AOP must contain 5 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Spring/Spring AOP", registry));
        assertEquals(HighlightSeverity.ERROR, registry.getTool("Spring.Aop.IncorrectJdkProxiedBeanType").getDefaultSeverity());
        assertEquals(HighlightSeverity.ERROR, registry.getTool("Spring.Aop.IncorrectAdviceAdvisor").getDefaultSeverity());
        assertEquals(HighlightSeverity.WARNING, registry.getTool("Spring.Aop.IncorrectAspectPointcut").getDefaultSeverity());
        assertEquals(HighlightSeverity.WARNING, registry.getTool("Spring.Aop.IncorrectPointcutExpression").getDefaultSeverity());
        assertEquals(HighlightSeverity.WARNING, registry.getTool("Spring.Aop.MissingAspectjAutoproxy").getDefaultSeverity());

        // Spring Boot (6 tools from Image 1)
        List<InspectionTool> bootTools = registry.getToolsForCategory("Spring/Spring Boot");
        assertEquals(6, bootTools.size(), "Spring Boot must contain 6 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Spring/Spring Boot", registry));
        assertEquals(HighlightSeverity.ERROR, registry.getTool("Spring.Boot.InvalidConfigurationProperties").getDefaultSeverity());
        assertEquals(HighlightSeverity.ERROR, registry.getTool("Spring.Boot.InvalidMetadataJson").getDefaultSeverity());
        assertEquals(HighlightSeverity.ERROR, registry.getTool("Spring.Boot.InvalidPropertiesConfiguration").getDefaultSeverity());
        assertEquals(HighlightSeverity.ERROR, registry.getTool("Spring.Boot.InvalidSetup").getDefaultSeverity());
        assertEquals(HighlightSeverity.WARNING, registry.getTool("Spring.Boot.InvalidYamlConfiguration").getDefaultSeverity());
        assertEquals(HighlightSeverity.WARNING, registry.getTool("Spring.Boot.SuspiciousHooksOperatorDebug").getDefaultSeverity());
    }

    @Test
    void testSpringCoreCompleteHierarchyParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        // Subcategories of Spring Core: Code and XML
        List<String> coreSubCats = registry.getAllSubCategories("Spring/Spring Core");
        assertEquals(2, coreSubCats.size(), "Spring Core must have 2 subcategories: Code and XML");
        assertTrue(coreSubCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring Core/Code")));
        assertTrue(coreSubCats.stream().anyMatch(sc -> sc.equalsIgnoreCase("Spring/Spring Core/XML")));

        // Spring Core > Code (28 tools from Image 2)
        List<InspectionTool> codeTools = registry.getToolsDirectlyInCategory("Spring/Spring Core/Code");
        assertEquals(28, codeTools.size(), "Spring Core > Code must contain 28 tools matching Image 2");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Spring/Spring Core/Code", registry));

        // Spring Core > XML (24 tools from Image 3)
        List<InspectionTool> xmlTools = registry.getToolsDirectlyInCategory("Spring/Spring Core/XML");
        assertEquals(24, xmlTools.size(), "Spring Core > XML must contain 24 tools matching Image 3");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Spring/Spring Core/XML", registry));

        // Spring Core direct tools (3 tools from Image 1, 2, 3)
        List<InspectionTool> directCoreTools = registry.getToolsDirectlyInCategory("Spring/Spring Core");
        assertEquals(3, directCoreTools.size(), "Spring Core must have 3 direct tools");
        assertNotNull(registry.getTool("Spring.Core.IncorrectInjectingSpelStaticField"));
        assertNotNull(registry.getTool("Spring.Core.IncorrectSpelSyntax"));
        assertNotNull(registry.getTool("Spring.Core.UnresolvedSpringHandlers"));

        // Total tools under Spring Core: 28 + 24 + 3 = 55
        List<InspectionTool> allCoreTools = registry.getToolsForCategory("Spring/Spring Core");
        assertEquals(55, allCoreTools.size(), "Spring Core total tools must be 55");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Spring/Spring Core", registry));
    }

    @Test
    void testSpringOtherSubcategoriesParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        // Spring Cloud (1 tool)
        List<InspectionTool> cloudTools = registry.getToolsForCategory("Spring/Spring Cloud");
        assertEquals(1, cloudTools.size());
        assertEquals("Bootstrap configuration included in application context", cloudTools.get(0).getDisplayName());
        assertEquals(HighlightSeverity.WARNING, cloudTools.get(0).getDefaultSeverity());

        // Spring Cloud Stream (2 tools)
        List<InspectionTool> streamTools = registry.getToolsForCategory("Spring/Spring Cloud Stream");
        assertEquals(2, streamTools.size());

        // Spring Data subcategory (6 tools from Image 1)
        List<InspectionTool> dataTools = registry.getToolsForCategory("Spring/Spring Data");
        assertEquals(6, dataTools.size(), "Spring Data subcategory must contain 6 tools matching Image 1");
        assertNotNull(registry.getTool("Spring.Data.PageMustHavePageable"));
        assertNotNull(registry.getTool("Spring.Data.MongoDbJsonUnresolvedFields"));
        assertNotNull(registry.getTool("Spring.Data.RepositoryMethodErrors"));
        assertNotNull(registry.getTool("Spring.Data.RepositoryMethodParametersErrors"));
        assertNotNull(registry.getTool("Spring.Data.RepositoryMethodReturnTypeErrors"));
        assertNotNull(registry.getTool("Spring.Data.UpdateDeleteAnnotatedModifying"));

        // Spring Integration (4 tools from Image 4)
        List<InspectionTool> intTools = registry.getToolsForCategory("Spring/Spring Integration");
        assertEquals(4, intTools.size());

        // Spring MVC (3 tools from Image 4)
        List<InspectionTool> mvcTools = registry.getToolsForCategory("Spring/Spring MVC");
        assertEquals(3, mvcTools.size());

        // Spring Security (4 tools from Image 4)
        List<InspectionTool> secTools = registry.getToolsForCategory("Spring/Spring Security");
        assertEquals(4, secTools.size());

        // Spring WebSocket (1 tool from Image 4)
        List<InspectionTool> wsTools = registry.getToolsForCategory("Spring/Spring WebSocket");
        assertEquals(1, wsTools.size());
        assertEquals("Incorrect Spring WebSocket XML-based application context", wsTools.get(0).getDisplayName());
        assertEquals(HighlightSeverity.ERROR, wsTools.get(0).getDefaultSeverity());
    }

    @Test
    void testSpringTreeUISelectionParity() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInspectionsPage page = new SettingsInspectionsPage();
                assertNotNull(page);

                // 1. Select Spring Core category (Image 1 and 2)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> coreNode = findCategoryNode(page.getTreeView().getRoot(), "Spring/Spring Core");
                assertNotNull(coreNode, "Spring Core category node must exist in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(coreNode);
                assertTrue(page.getMultiSelectionLabel().isVisible());
                assertEquals("Multiple inspections are selected. You can edit them as a single inspection.", page.getMultiSelectionLabel().getText());
                assertEquals("Mixed", page.getSeverityButton().getText().trim());
                assertEquals("Mixed", page.getHighlightingCombo().getValue());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());

                // 2. Select Spring Core > XML category (Image 3)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> xmlNode = findCategoryNode(page.getTreeView().getRoot(), "Spring/Spring Core/XML");
                assertNotNull(xmlNode, "Spring Core XML subcategory node must exist in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(xmlNode);
                assertTrue(page.getMultiSelectionLabel().isVisible());
                assertEquals("Mixed", page.getSeverityButton().getText().trim());
                assertEquals("Mixed", page.getHighlightingCombo().getValue());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());

                // 3. Select Spring WebSocket category (Image 4)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> wsNode = findCategoryNode(page.getTreeView().getRoot(), "Spring/Spring WebSocket");
                assertNotNull(wsNode, "Spring WebSocket subcategory node must exist in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(wsNode);
                assertTrue(page.getMultiSelectionLabel().isVisible());
                assertEquals("Error", page.getSeverityButton().getText().trim());
                assertEquals("Error", page.getHighlightingCombo().getValue());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (err.get() != null) {
            if (err.get() instanceof AssertionError ae) throw ae;
            throw new RuntimeException(err.get());
        }
    }

    @Test
    void testSpringDataAndModulithSeparateCategoriesParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        // Spring Data separate top-level category: 1 tool, ERROR severity
        List<InspectionTool> springDataTools = registry.getToolsForCategory("Spring Data");
        assertEquals(1, springDataTools.size(), "Spring Data category must contain 1 tool matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Spring Data", registry));
        InspectionTool jdbc = registry.getTool("SpringData.JdbcAssociatedDbElements");
        assertNotNull(jdbc);
        assertEquals("Spring Data JDBC associated DB elements", jdbc.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, jdbc.getDefaultSeverity());
        assertTrue(jdbc.isDefaultEnabled());

        // Spring Modulith separate top-level category: 3 tools
        List<InspectionTool> modulithTools = registry.getToolsForCategory("Spring Modulith");
        assertEquals(3, modulithTools.size(), "Spring Modulith category must contain 3 tools matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Spring Modulith", registry));

        InspectionTool evtListener = registry.getTool("SpringModulith.EventListenerCanBeSimplified");
        assertNotNull(evtListener);
        assertEquals(HighlightSeverity.WARNING, evtListener.getDefaultSeverity());

        InspectionTool invDep = registry.getTool("SpringModulith.InvalidDependencyDeclaration");
        assertNotNull(invDep);
        assertEquals(HighlightSeverity.ERROR, invDep.getDefaultSeverity());

        InspectionTool restUsage = registry.getTool("SpringModulith.RestrictedModuleApiUsage");
        assertNotNull(restUsage);
        assertEquals(HighlightSeverity.ERROR, restUsage.getDefaultSeverity());
    }

    @Test
    void testSqlInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> sqlTools = registry.getToolsForCategory("SQL");
        assertEquals(52, sqlTools.size(), "SQL category must contain 52 tools directly under SQL matching reference screenshot");

        // 4 disabled items
        assertFalse(registry.getTool("SqlExcessiveJoin").isDefaultEnabled());
        assertFalse(registry.getTool("SqlMissingColumnAliases").isDefaultEnabled());
        assertFalse(registry.getTool("SqlNamedArguments").isDefaultEnabled());
        assertFalse(registry.getTool("SqlGotoStatements").isDefaultEnabled());

        // Error severities matching screenshot (!)
        InspectionTool retStmt = registry.getTool("SqlMissingReturnStatement");
        assertNotNull(retStmt);
        assertEquals(HighlightSeverity.ERROR, retStmt.getDefaultSeverity());
        assertTrue(retStmt.isDefaultEnabled());

        InspectionTool resolve = registry.getTool("SqlResolve");
        assertNotNull(resolve);
        assertEquals(HighlightSeverity.ERROR, resolve.getDefaultSeverity());
        assertTrue(resolve.isDefaultEnabled());

        InspectionTool namedPos = registry.getTool("SqlNamedAndPositionalArguments");
        assertNotNull(namedPos);
        assertEquals(HighlightSeverity.ERROR, namedPos.getDefaultSeverity());
        assertTrue(namedPos.isDefaultEnabled());

        InspectionTool valCard = registry.getTool("SqlValuesClauseCardinality");
        assertNotNull(valCard);
        assertEquals(HighlightSeverity.ERROR, valCard.getDefaultSeverity());
        assertTrue(valCard.isDefaultEnabled());

        // Selected item in Image 1: Redundant row limiting in queries
        InspectionTool redLimit = registry.getTool("SqlRedundantLimit");
        assertNotNull(redLimit);
        assertEquals("Redundant row limiting in queries", redLimit.getDisplayName());
        assertEquals(HighlightSeverity.WARNING, redLimit.getDefaultSeverity());
        assertTrue(redLimit.isDefaultEnabled());
        assertTrue(redLimit.getDescription().contains("Reports redundant row limiting clauses like FETCH and LIMIT in queries."));
        assertTrue(redLimit.getDescription().contains("SELECT * FROM foo WHERE EXISTS(SELECT * FROM foo LIMIT 2);"));
        assertTrue(redLimit.getDescription().contains("To fix the warning, you can add OFFSET to limiting clauses."));

        // Overall category state is INDETERMINATE because 4 tools are disabled
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("SQL", registry));
    }

    @Test
    void testSqlServerInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> tools = registry.getToolsForCategory("SQL server");
        assertEquals(2, tools.size(), "SQL server category must contain 2 tools matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("SQL server", registry));

        InspectionTool builtin = registry.getTool("SqlServer.BuiltinFunctions");
        assertNotNull(builtin);
        assertEquals("Builtin functions", builtin.getDisplayName());
        assertEquals(HighlightSeverity.WARNING, builtin.getDefaultSeverity());
        assertTrue(builtin.isDefaultEnabled());

        InspectionTool orderBy = registry.getTool("SqlServer.OrderByInQueries");
        assertNotNull(orderBy);
        assertEquals("ORDER BY in queries", orderBy.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, orderBy.getDefaultSeverity());
        assertTrue(orderBy.isDefaultEnabled());
    }

    @Test
    void testThymeleafInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> tools = registry.getToolsForCategory("Thymeleaf");
        assertEquals(3, tools.size(), "Thymeleaf category must contain 3 tools matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Thymeleaf", registry));

        InspectionTool dialectExt = registry.getTool("Thymeleaf.DialectExtensionsErrors");
        assertNotNull(dialectExt);
        assertEquals("Thymeleaf Dialect Extensions errors", dialectExt.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, dialectExt.getDefaultSeverity());
        assertTrue(dialectExt.isDefaultEnabled());

        InspectionTool msgKeys = registry.getTool("Thymeleaf.UnresolvedMessageResourceKeys");
        assertNotNull(msgKeys);
        assertEquals("Unresolved message resource keys", msgKeys.getDisplayName());
        assertEquals(HighlightSeverity.ERROR, msgKeys.getDefaultSeverity());
        assertTrue(msgKeys.isDefaultEnabled());

        InspectionTool exprVars = registry.getTool("Thymeleaf.UnresolvedReferencesInExpressionVariables");
        assertNotNull(exprVars);
        assertEquals("Unresolved references in Thymeleaf expression variables", exprVars.getDisplayName());
        assertEquals(HighlightSeverity.WARNING, exprVars.getDefaultSeverity());
        assertTrue(exprVars.isDefaultEnabled());
    }

    @Test
    void testTomlInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> tools = registry.getToolsForCategory("TOML");
        assertEquals(1, tools.size(), "TOML category must contain 1 tool matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("TOML", registry));

        InspectionTool unres = registry.getTool("Toml.UnresolvedReference");
        assertNotNull(unres);
        assertEquals("Unresolved reference", unres.getDisplayName());
        assertEquals(HighlightSeverity.WARNING, unres.getDefaultSeverity());
        assertTrue(unres.isDefaultEnabled());
    }

    @Test
    void testVelocityInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> tools = registry.getToolsForCategory("Velocity");
        assertEquals(5, tools.size(), "Velocity category must contain 5 tools matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Velocity", registry));

        InspectionTool dirArgs = registry.getTool("Velocity.DirectiveArgumentsInspection");
        assertNotNull(dirArgs);
        assertEquals(HighlightSeverity.WARNING, dirArgs.getDefaultSeverity());

        InspectionTool fileRefs = registry.getTool("Velocity.FileReferencesInspection");
        assertNotNull(fileRefs);
        assertEquals(HighlightSeverity.WARNING, fileRefs.getDefaultSeverity());

        InspectionTool refs = registry.getTool("Velocity.ReferencesInspection");
        assertNotNull(refs);
        assertEquals(HighlightSeverity.WARNING, refs.getDefaultSeverity());

        InspectionTool types = registry.getTool("Velocity.TypesInspection");
        assertNotNull(types);
        assertEquals(HighlightSeverity.WARNING, types.getDefaultSeverity());

        InspectionTool wellFormed = registry.getTool("Velocity.WellFormednessInspection");
        assertNotNull(wellFormed);
        assertEquals(HighlightSeverity.ERROR, wellFormed.getDefaultSeverity());
    }

    @Test
    void testVersionControlInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> tools = registry.getToolsForCategory("Version control");
        assertEquals(1, tools.size(), "Version control category must contain 1 tool matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Version control", registry));

        InspectionTool ign = registry.getTool("Vcs.IgnoreFileDuplicates");
        assertNotNull(ign);
        assertEquals("Ignore file duplicates", ign.getDisplayName());
        assertEquals(HighlightSeverity.WARNING, ign.getDefaultSeverity());
        assertTrue(ign.isDefaultEnabled());
    }

    @Test
    void testVueInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> tools = registry.getToolsForCategory("Vue");
        assertEquals(6, tools.size(), "Vue category must contain 6 tools matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Vue", registry));
        assertTrue(tools.stream().allMatch(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING));
        assertTrue(tools.stream().allMatch(InspectionTool::isDefaultEnabled));

        assertNotNull(registry.getTool("Vue.DataFunction"));
        assertNotNull(registry.getTool("Vue.DeprecatedSymbol"));
        assertNotNull(registry.getTool("Vue.DuplicateTemplateScriptTag"));
        assertNotNull(registry.getTool("Vue.MissingComponentImport"));
        assertNotNull(registry.getTool("Vue.UnrecognizedDirective"));
        assertNotNull(registry.getTool("Vue.UnrecognizedSlot"));
    }

    @Test
    void testXmlInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> tools = registry.getToolsForCategory("XML");
        assertEquals(14, tools.size(), "XML category must contain 14 tools matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("XML", registry));

        // Mixed severities
        long errorCount = tools.stream().filter(t -> t.getDefaultSeverity() == HighlightSeverity.ERROR).count();
        long warningCount = tools.stream().filter(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING).count();
        assertEquals(7, errorCount, "XML must have 7 ERROR inspections");
        assertEquals(7, warningCount, "XML must have 7 WARNING inspections");

        // Batch mode tool
        InspectionTool xmlHighlighting = registry.getTool("Xml.Highlighting");
        assertNotNull(xmlHighlighting);
        assertTrue(xmlHighlighting.isBatchModeOnly());
        assertEquals("XML highlighting", xmlHighlighting.getDisplayName());
    }

    @Test
    void testXPathInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> tools = registry.getToolsForCategory("XPath");
        assertEquals(5, tools.size(), "XPath category must contain 5 tools matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("XPath", registry));
        assertTrue(tools.stream().allMatch(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING));
        assertTrue(tools.stream().allMatch(InspectionTool::isDefaultEnabled));

        assertNotNull(registry.getTool("XPath.HardcodedNamespacePrefix"));
        assertNotNull(registry.getTool("XPath.ImplicitTypeConversion"));
        assertNotNull(registry.getTool("XPath.RedundantTypeConversion"));
        assertNotNull(registry.getTool("XPath.UnknownElementOrAttributeName"));
        assertNotNull(registry.getTool("XPath.PredicateWithIndex0"));
    }

    @Test
    void testXsltInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> tools = registry.getToolsForCategory("XSLT");
        assertEquals(4, tools.size(), "XSLT category must contain 4 tools matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("XSLT", registry));

        assertEquals(HighlightSeverity.ERROR, registry.getTool("Xslt.IncorrectDeclaration").getDefaultSeverity());
        assertEquals(HighlightSeverity.ERROR, registry.getTool("Xslt.IncorrectTemplateInvocation").getDefaultSeverity());
        assertEquals(HighlightSeverity.WARNING, registry.getTool("Xslt.ShadowedVariable").getDefaultSeverity());
        assertEquals(HighlightSeverity.WARNING, registry.getTool("Xslt.UnusedVariableOrParameter").getDefaultSeverity());
    }

    @Test
    void testYamlInspectionsParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        List<InspectionTool> tools = registry.getToolsForCategory("YAML");
        assertEquals(7, tools.size(), "YAML category must contain 7 tools matching screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("YAML", registry));

        InspectionTool depKey = registry.getTool("Yaml.DeprecatedKey");
        assertNotNull(depKey);
        assertEquals("Deprecated YAML key", depKey.getDisplayName());
        assertEquals(HighlightSeverity.WARNING, depKey.getDefaultSeverity());
        assertTrue(depKey.isDefaultEnabled());

        assertEquals(HighlightSeverity.ERROR, registry.getTool("Yaml.DuplicatedKeys").getDefaultSeverity());
        assertEquals(HighlightSeverity.ERROR, registry.getTool("Yaml.RecursiveAlias").getDefaultSeverity());
        assertEquals(HighlightSeverity.WARNING, registry.getTool("Yaml.SuspiciousTypeMismatch").getDefaultSeverity());
        assertEquals(HighlightSeverity.ERROR, registry.getTool("Yaml.UnresolvedAlias").getDefaultSeverity());
        assertEquals(HighlightSeverity.WARNING, registry.getTool("Yaml.UnusedAnchor").getDefaultSeverity());
        assertEquals(HighlightSeverity.WARNING, registry.getTool("Yaml.ValidationByJsonSchema").getDefaultSeverity());
    }

    @Test
    void testBatchFiveUIParityAndDetails() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInspectionsPage page = new SettingsInspectionsPage();
                assertNotNull(page);

                // 1. Select Redundant row limiting in queries (Image 1)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> redLimitNode = findNodeRecursively(page.getTreeView().getRoot(), "SqlRedundantLimit");
                assertNotNull(redLimitNode);
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(redLimitNode);
                assertFalse(page.getMultiSelectionLabel().isVisible());
                assertTrue(page.getToolDescArea().getText().contains("Reports redundant row limiting clauses like FETCH and LIMIT in queries."));
                assertTrue(page.getToolDescArea().getText().contains("SELECT * FROM foo WHERE EXISTS(SELECT * FROM foo LIMIT 2);"));
                assertEquals("Warning", page.getSeverityButton().getText().trim());
                assertEquals("Warning", page.getHighlightingCombo().getValue());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());

                // 2. Select Version control category (Image 2)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> vcsCatNode = findCategoryNode(page.getTreeView().getRoot(), "Version control");
                assertNotNull(vcsCatNode);
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(vcsCatNode);
                assertTrue(page.getMultiSelectionLabel().isVisible());
                assertEquals("Multiple inspections are selected. You can edit them as a single inspection.", page.getMultiSelectionLabel().getText());
                assertEquals("Warning", page.getSeverityButton().getText().trim());
                assertEquals("Warning", page.getHighlightingCombo().getValue());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());

                // 3. Select XML category (Image 3 - Mixed severity)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> xmlCatNode = findCategoryNode(page.getTreeView().getRoot(), "XML");
                assertNotNull(xmlCatNode);
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(xmlCatNode);
                assertTrue(page.getMultiSelectionLabel().isVisible());
                assertEquals("Mixed", page.getSeverityButton().getText().trim());
                assertEquals("Mixed", page.getHighlightingCombo().getValue());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());

                // 4. Select Deprecated YAML key (Image 3)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> yamlDepNode = findNodeRecursively(page.getTreeView().getRoot(), "Yaml.DeprecatedKey");
                assertNotNull(yamlDepNode);
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(yamlDepNode);
                assertFalse(page.getMultiSelectionLabel().isVisible());
                assertTrue(page.getToolDescArea().getText().contains("Reports deprecated keys in YAML files"));
                assertEquals("Warning", page.getSeverityButton().getText().trim());
                assertEquals("Warning", page.getHighlightingCombo().getValue());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (err.get() != null) {
            if (err.get() instanceof AssertionError ae) throw ae;
            throw new RuntimeException(err.get());
        }
    }

    @Test
    void testScalaCategoryCompleteHierarchyParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        // Overall category state: INDETERMINATE (due to disabled inspections in Collections, General, Syntactic clarification/simplification)
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Scala", registry),
                "Scala top-level category state must be INDETERMINATE [-] matching Screenshot 1");

        // Subcategories under Scala: exactly 14 subcategories
        List<String> subCats = registry.getAllSubCategories("Scala");
        assertEquals(14, subCats.size(), "Scala must have 14 direct subcategories matching Screenshot 1");

        // 1. Akka ([✓] 4 tools, all Warning, all enabled)
        List<InspectionTool> akkaTools = registry.getToolsForCategory("Scala/Akka");
        assertEquals(4, akkaTools.size(), "Scala/Akka must contain 4 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Akka", registry));
        assertTrue(akkaTools.stream().allMatch(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING));
        assertTrue(akkaTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertNotNull(registry.getTool("Scala.Akka.ActorMutableState"));
        assertNotNull(registry.getTool("Scala.Akka.AppropriateActorConstructorNotFound"));
        assertNotNull(registry.getTool("Scala.Akka.CouldBeReplacedWithFactoryMethodCall"));
        assertNotNull(registry.getTool("Scala.Akka.DynamicInvocationCouldBeReplacedWithConstructorCall"));

        // 2. Code style ([✓] 2 tools, all Warning, all enabled)
        List<InspectionTool> codeStyleTools = registry.getToolsForCategory("Scala/Code style");
        assertEquals(2, codeStyleTools.size(), "Scala/Code style must contain 2 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Code style", registry));
        assertTrue(codeStyleTools.stream().allMatch(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING));
        assertTrue(codeStyleTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertNotNull(registry.getTool("Scala.CodeStyle.ScalaStyleInspection"));
        assertNotNull(registry.getTool("Scala.CodeStyle.Scala2SyntaxWithXsource3"));

        // 3. Directive ([✓] 1 tool, Warning, enabled)
        List<InspectionTool> directiveTools = registry.getToolsForCategory("Scala/Directive");
        assertEquals(1, directiveTools.size(), "Scala/Directive must contain 1 tool");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Directive", registry));
        assertNotNull(registry.getTool("Scala.Directive.NewerStableVersionForLibraryDependencyIsAvailable"));

        // 4. Method signature ([✓] 16 tools, all enabled matching media_1790826033450.png)
        List<InspectionTool> methodSigTools = registry.getToolsForCategory("Scala/Method signature");
        assertEquals(16, methodSigTools.size(), "Scala/Method signature must contain 16 tools matching Screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Method signature", registry));
        assertTrue(methodSigTools.stream().allMatch(InspectionTool::isDefaultEnabled));
        assertEquals(14, methodSigTools.stream().filter(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING).count());
        assertEquals(2, methodSigTools.stream().filter(t -> t.getDefaultSeverity() == HighlightSeverity.NO_HIGHLIGHTING).count());

        // 5. Play ([✓] 4 tools matching media_1790826033450.png)
        List<InspectionTool> playTools = registry.getToolsForCategory("Scala/Play");
        assertEquals(4, playTools.size(), "Scala/Play must contain 4 tools matching Screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Play", registry));
        assertTrue(playTools.stream().allMatch(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING));
        assertTrue(playTools.stream().allMatch(InspectionTool::isDefaultEnabled));

        // 6. Properties files ([✓] 1 tool, Error !, matching media_1790826033450.png)
        List<InspectionTool> propTools = registry.getToolsForCategory("Scala/Properties files");
        assertEquals(1, propTools.size(), "Scala/Properties files must contain 1 tool");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Properties files", registry));
        assertEquals(HighlightSeverity.ERROR, propTools.get(0).getDefaultSeverity());
        assertEquals("Invalid property key", propTools.get(0).getDisplayName());

        // 7. Resource leaks ([✓] 1 tool, Warning ▲, matching media_1790826033450.png)
        List<InspectionTool> leakTools = registry.getToolsForCategory("Scala/Resource leaks");
        assertEquals(1, leakTools.size(), "Scala/Resource leaks must contain 1 tool");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Resource leaks", registry));
        assertEquals(HighlightSeverity.WARNING, leakTools.get(0).getDefaultSeverity());
        assertEquals("Source is not closed", leakTools.get(0).getDisplayName());

        // 8. Scaladoc ([✓] 7 tools matching media_1790826033450.png)
        List<InspectionTool> scaladocTools = registry.getToolsForCategory("Scala/Scaladoc");
        assertEquals(7, scaladocTools.size(), "Scala/Scaladoc must contain 7 tools matching Screenshot");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Scaladoc", registry));
        assertTrue(scaladocTools.stream().allMatch(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING));
        assertTrue(scaladocTools.stream().allMatch(InspectionTool::isDefaultEnabled));

        // 9. Specs2 ([✓] 1 tool matching media_1790826063653.png)
        List<InspectionTool> specs2Tools = registry.getToolsForCategory("Scala/Specs2");
        assertEquals(1, specs2Tools.size(), "Scala/Specs2 must contain 1 tool");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Specs2", registry));
        assertEquals("Specs2 matchers", specs2Tools.get(0).getDisplayName());

        // 10. Syntactic clarification ([-] 2 tools: 1 enabled, 1 disabled matching media_1790826063653.png)
        List<InspectionTool> synClarTools = registry.getToolsForCategory("Scala/Syntactic clarification");
        assertEquals(2, synClarTools.size(), "Scala/Syntactic clarification must contain 2 tools");
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Scala/Syntactic clarification", registry));
        assertFalse(registry.getTool("Scala.SyntacticClarification.AutoTupling").isDefaultEnabled());
        assertTrue(registry.getTool("Scala.SyntacticClarification.ConvertNullInitializerToUnderscore").isDefaultEnabled());

        // 11. Syntactic simplification ([✓] 8 tools, all enabled matching media_1790826063653.png)
        List<InspectionTool> synSimpTools = registry.getToolsForCategory("Scala/Syntactic simplification");
        assertEquals(8, synSimpTools.size(), "Scala/Syntactic simplification must contain 8 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Syntactic simplification", registry));
        assertTrue(synSimpTools.stream().allMatch(InspectionTool::isDefaultEnabled));

        // 12. Worksheet ([✓] 2 tools matching media_1790826063653.png)
        List<InspectionTool> wsTools = registry.getToolsForCategory("Scala/Worksheet");
        assertEquals(2, wsTools.size(), "Scala/Worksheet must contain 2 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Worksheet", registry));
        assertNotNull(registry.getTool("Scala.Worksheet.AmmoniteUnresolvedLibrary"));
        assertNotNull(registry.getTool("Scala.Worksheet.WorksheetPackageDeclaration"));
        assertEquals(HighlightSeverity.ERROR, registry.getTool("Scala.Worksheet.WorksheetPackageDeclaration").getDefaultSeverity());
    }

    @Test
    void testScalaCollectionsHierarchyParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        // Scala/Collections state: INDETERMINATE (due to disabled items in Comparing, Options, Simplifications: filter and exists, and direct)
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Scala/Collections", registry),
                "Scala/Collections must be INDETERMINATE [-] matching Screenshot 1");

        // Direct tools under Collections: 2 (1 enabled, 1 disabled)
        List<InspectionTool> direct = registry.getToolsDirectlyInCategory("Scala/Collections");
        assertEquals(2, direct.size(), "Scala/Collections must have 2 direct tools");
        InspectionTool conv = registry.getTool("Scala.Collections.RedundantCollectionConversion");
        assertNotNull(conv);
        assertEquals("Redundant collection conversion", conv.getDisplayName());
        assertEquals(HighlightSeverity.WARNING, conv.getDefaultSeverity());
        assertTrue(conv.isDefaultEnabled());

        InspectionTool sideEffects = registry.getTool("Scala.Collections.SideEffectsInMonadicTransformation");
        assertNotNull(sideEffects);
        assertEquals("Side effects in a monadic transformation", sideEffects.getDisplayName());
        assertEquals(HighlightSeverity.WARNING, sideEffects.getDefaultSeverity());
        assertFalse(sideEffects.isDefaultEnabled(), "Side effects in monadic transformation must be disabled by default");

        // Subcategories of Collections: 10
        List<String> collSubCats = registry.getAllSubCategories("Scala/Collections");
        assertEquals(10, collSubCats.size(), "Collections must have 10 subcategories");

        // Comparing (5 tools, 1 disabled)
        List<InspectionTool> comparingTools = registry.getToolsDirectlyInCategory("Scala/Collections/Comparing");
        assertEquals(5, comparingTools.size(), "Comparing must have 5 tools");
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Scala/Collections/Comparing", registry));
        assertFalse(registry.getTool("Scala.Collections.Comparing.ComparingLengthToLengthCompare").isDefaultEnabled());

        // Indices (4 tools, all enabled)
        List<InspectionTool> indicesTools = registry.getToolsDirectlyInCategory("Scala/Collections/Indices");
        assertEquals(4, indicesTools.size(), "Indices must have 4 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Collections/Indices", registry));

        // Maps (7 tools, all enabled)
        List<InspectionTool> mapsTools = registry.getToolsDirectlyInCategory("Scala/Collections/Maps");
        assertEquals(7, mapsTools.size(), "Maps must have 7 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Collections/Maps", registry));

        // Options (8 tools, 2 disabled)
        List<InspectionTool> optionsTools = registry.getToolsDirectlyInCategory("Scala/Collections/Options");
        assertEquals(8, optionsTools.size(), "Options must have 8 tools");
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Scala/Collections/Options", registry));
        assertFalse(registry.getTool("Scala.Collections.Options.MapAndGetOrElseToFold").isDefaultEnabled());
        assertFalse(registry.getTool("Scala.Collections.Options.SomeToOption").isDefaultEnabled());

        // Other (2 tools, all enabled)
        List<InspectionTool> otherTools = registry.getToolsDirectlyInCategory("Scala/Collections/Other");
        assertEquals(2, otherTools.size(), "Other must have 2 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Collections/Other", registry));

        // Simplifications: filter and exists (8 tools, 1 disabled)
        List<InspectionTool> filterExistsTools = registry.getToolsDirectlyInCategory("Scala/Collections/Simplifications: filter and exists");
        assertEquals(8, filterExistsTools.size(), "Filter and exists must have 8 tools");
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Scala/Collections/Simplifications: filter and exists", registry));
        assertFalse(registry.getTool("Scala.Collections.SimplificationsFilterAndExists.RedundantContainsInFilter").isDefaultEnabled());

        // Simplifications: find and map to apply (1 tool, enabled)
        List<InspectionTool> findMapTools = registry.getToolsDirectlyInCategory("Scala/Collections/Simplifications: find and map to apply");
        assertEquals(1, findMapTools.size(), "Find and map must have 1 tool");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Collections/Simplifications: find and map to apply", registry));

        // Simplifications: forall and exists (1 tool, enabled)
        List<InspectionTool> forallExistsTools = registry.getToolsDirectlyInCategory("Scala/Collections/Simplifications: forall and exists");
        assertEquals(1, forallExistsTools.size(), "Forall and exists must have 1 tool");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Collections/Simplifications: forall and exists", registry));

        // Simplifications: other (15 tools, all enabled)
        List<InspectionTool> simpOtherTools = registry.getToolsDirectlyInCategory("Scala/Collections/Simplifications: other");
        assertEquals(15, simpOtherTools.size(), "Simplifications: other must have 15 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Collections/Simplifications: other", registry));

        // Size (2 tools, all enabled)
        List<InspectionTool> sizeTools = registry.getToolsDirectlyInCategory("Scala/Collections/Size");
        assertEquals(2, sizeTools.size(), "Size must have 2 tools");
        assertEquals(InspectionProfile.TriState.CHECKED, profile.getCategoryState("Scala/Collections/Size", registry));
    }

    @Test
    void testScalaGeneralHierarchyAndSelectionParity() {
        InspectionRegistry registry = InspectionRegistry.getInstance();
        InspectionProfile profile = new InspectionProfile("TestProfile", true);

        // General category: INDETERMINATE (due to 10 disabled items)
        assertEquals(InspectionProfile.TriState.INDETERMINATE, profile.getCategoryState("Scala/General", registry),
                "Scala/General category must be INDETERMINATE [-] matching Screenshot 1 and 4");

        List<InspectionTool> generalTools = registry.getToolsForCategory("Scala/General");
        assertEquals(67, generalTools.size(), "Scala/General must contain exactly 67 tools matching Screenshots 4 and 5");

        // 10 disabled tools
        long disabledCount = generalTools.stream().filter(t -> !t.isDefaultEnabled()).count();
        assertEquals(10, disabledCount, "General category must have 10 disabled inspections");

        // 7 Error tools
        long errorCount = generalTools.stream().filter(t -> t.getDefaultSeverity() == HighlightSeverity.ERROR).count();
        assertEquals(7, errorCount, "General category must have 7 ERROR inspections");

        // 60 Warning tools total (50 enabled, 10 disabled)
        long warningCount = generalTools.stream().filter(t -> t.getDefaultSeverity() == HighlightSeverity.WARNING).count();
        assertEquals(60, warningCount, "General category must have 60 WARNING inspections total");

        long enabledWarningCount = generalTools.stream().filter(t -> t.isDefaultEnabled() && t.getDefaultSeverity() == HighlightSeverity.WARNING).count();
        assertEquals(50, enabledWarningCount, "General category must have 50 enabled WARNING inspections");

        // Selected item in Screenshot 5: Multiple @targetName annotations
        InspectionTool targetNameTool = registry.getTool("MultipleTargetNameAnnotations");
        assertNotNull(targetNameTool, "Multiple @targetName annotations tool must exist");
        assertEquals("Multiple @targetName annotations", targetNameTool.getDisplayName());
        assertEquals(HighlightSeverity.WARNING, targetNameTool.getDefaultSeverity());
        assertTrue(targetNameTool.isDefaultEnabled());
        assertEquals("Scala/General", targetNameTool.getGroupPath());
        assertTrue(targetNameTool.getDescription().contains("Reports the usage of multiple @targetName annotations on a single element."));
        assertTrue(targetNameTool.getDescription().contains("All but the last @targetName annotation are ignored. Consider using at most one annotation per definition."));
        assertTrue(targetNameTool.getDescription().contains("The quick-fix removes selected @targetName annotation."));
        assertTrue(targetNameTool.getDescription().contains("Inspection ID: MultipleTargetNameAnnotations"));
    }

    @Test
    void testScalaTreeUISelectionParity() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInspectionsPage page = new SettingsInspectionsPage();
                assertNotNull(page);

                // 1. Select Multiple @targetName annotations (Screenshot 5)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> targetNameNode =
                        findNodeRecursively(page.getTreeView().getRoot(), "MultipleTargetNameAnnotations");
                assertNotNull(targetNameNode, "Multiple @targetName annotations node must exist in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(targetNameNode);

                assertFalse(page.getMultiSelectionLabel().isVisible());
                assertTrue(page.getToolDescArea().getText().contains("Reports the usage of multiple @targetName annotations on a single element."));
                assertTrue(page.getToolDescArea().getText().contains("Inspection ID: MultipleTargetNameAnnotations"));
                assertEquals("Warning", page.getSeverityButton().getText().trim());
                assertEquals("Warning", page.getHighlightingCombo().getValue());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());

                // 2. Select Scala/Collections category (Screenshot 1 and 2)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> collCatNode =
                        findCategoryNode(page.getTreeView().getRoot(), "Scala/Collections");
                assertNotNull(collCatNode, "Scala/Collections category node must exist in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(collCatNode);

                assertTrue(page.getMultiSelectionLabel().isVisible());
                assertEquals("Multiple inspections are selected. You can edit them as a single inspection.",
                        page.getMultiSelectionLabel().getText());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());

                // 3. Select Scala top-level category (Screenshot 1)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> scalaCatNode =
                        findCategoryNode(page.getTreeView().getRoot(), "Scala");
                assertNotNull(scalaCatNode, "Scala top-level category node must exist in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(scalaCatNode);

                assertTrue(page.getMultiSelectionLabel().isVisible());
                assertEquals("Multiple inspections are selected. You can edit them as a single inspection.",
                        page.getMultiSelectionLabel().getText());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());

                // 4. Select Specs2 category (media_1790826063653.png)
                TreeItem<SettingsInspectionsPage.InspectionTreeNode> specs2Node =
                        findCategoryNode(page.getTreeView().getRoot(), "Scala/Specs2");
                assertNotNull(specs2Node, "Scala/Specs2 category node must exist in tree");
                page.getTreeView().getSelectionModel().clearSelection();
                page.getTreeView().getSelectionModel().select(specs2Node);

                assertTrue(page.getMultiSelectionLabel().isVisible());
                assertEquals("Multiple inspections are selected. You can edit them as a single inspection.",
                        page.getMultiSelectionLabel().getText());
                assertEquals("Warning", page.getSeverityButton().getText().trim());
                assertEquals("Warning", page.getHighlightingCombo().getValue());
                assertEquals("In All Scopes", page.getScopeButton().getText().trim());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (err.get() != null) {
            if (err.get() instanceof AssertionError ae) throw ae;
            throw new RuntimeException(err.get());
        }
    }

    @Test
    void testBrandIsolation() throws Exception {
        Pattern competitorPattern = Pattern.compile("(?i)\\b(intellij|jetbrains|idea)\\b");

        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/inspections/HighlightSeverity.java",
                "src/main/java/dev/lumina/inspections/InspectionTool.java",
                "src/main/java/dev/lumina/inspections/InspectionProvider.java",
                "src/main/java/dev/lumina/inspections/InspectionRegistry.java",
                "src/main/java/dev/lumina/inspections/InspectionProfile.java",
                "src/main/java/dev/lumina/inspections/InspectionProfileManager.java",
                "src/main/java/dev/lumina/ui/SettingsInspectionsPage.java"
        );

        for (String relPath : filesToCheck) {
            Path path = Path.of(relPath);
            assertTrue(Files.exists(path), "File must exist: " + relPath);
            String content = Files.readString(path);
            var matcher = competitorPattern.matcher(content);
            assertFalse(matcher.find(), "Competitor brand found in file " + relPath + ": " + (matcher.hitEnd() ? "" : matcher.group()));
        }
    }

    private TreeItem<SettingsInspectionsPage.InspectionTreeNode> findNodeRecursively(TreeItem<SettingsInspectionsPage.InspectionTreeNode> parent, String toolId) {
        if (parent == null) return null;
        if (parent.getValue() != null && parent.getValue().getTool() != null &&
                toolId.equals(parent.getValue().getTool().getId())) {
            return parent;
        }
        for (TreeItem<SettingsInspectionsPage.InspectionTreeNode> child : parent.getChildren()) {
            TreeItem<SettingsInspectionsPage.InspectionTreeNode> found = findNodeRecursively(child, toolId);
            if (found != null) return found;
        }
        return null;
    }

    private TreeItem<SettingsInspectionsPage.InspectionTreeNode> findCategoryNode(TreeItem<SettingsInspectionsPage.InspectionTreeNode> parent, String categoryName) {
        if (parent == null) return null;
        if (parent.getValue() != null && parent.getValue().isCategory() &&
                categoryName.equalsIgnoreCase(parent.getValue().getCategory())) {
            return parent;
        }
        for (TreeItem<SettingsInspectionsPage.InspectionTreeNode> child : parent.getChildren()) {
            TreeItem<SettingsInspectionsPage.InspectionTreeNode> found = findCategoryNode(child, categoryName);
            if (found != null) return found;
        }
        return null;
    }
}
