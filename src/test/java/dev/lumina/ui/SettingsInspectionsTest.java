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
}
