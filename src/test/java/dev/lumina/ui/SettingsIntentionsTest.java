package dev.lumina.ui;

import dev.lumina.intentions.IntentionAction;
import dev.lumina.intentions.IntentionRegistry;
import dev.lumina.intentions.IntentionRegistry.TriState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Intentions settings architecture, catalog completeness,
 * tri-state calculations, search filtering, and strict brand isolation.
 */
public class SettingsIntentionsTest {

    private IntentionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = IntentionRegistry.getInstance();
        registry.resetToDefaults();
    }

    @Test
    void testCatalogContainsAll58TopCategories() {
        List<String> categories = registry.getTopCategories();
        assertNotNull(categories);
        assertEquals(58, categories.size(), "Should have exactly 58 top-level categories matching reference IDE screenshots");

        List<String> expectedCategories = List.of(
                "AI Assistant", "Angular", "Choose template data language", "Copilot", "CSS",
                "Database", "Dev Containers", "Docker", "EditorConfig", "Flow JS",
                "FreeMarker", "GitLab CI", "Go", "Go modules", "Groovy",
                "HTML", "HTTP Client", "Java", "Java EE Persistence", "JavaFX",
                "JavaScript", "JRuby", "JSON", "JSP", "Kotlin",
                "Ktor", "Kubernetes", "Language injection", "LightEdit mode", "LSP",
                "Markdown", "Micronaut", "Microservices", "MongoDB JSON", "Natural languages",
                "OpenAPI specifications", "Other", "PHP", "Properties", "Python",
                "Qodana configuration", "Quarkus", "React", "RegExp", "Ruby",
                "Rust", "Scala", "Shell script", "Spring", "Spring Boot",
                "SQL", "TOML", "TypeScript", "Velocity", "Vue.JS",
                "XML", "XSLT", "YAML"
        );

        for (String expected : expectedCategories) {
            assertTrue(categories.contains(expected), "Catalog must contain category: " + expected);
        }
    }

    @Test
    void testAiAssistantAndAngularIntentions() {
        List<IntentionAction> aiActions = registry.getAllActionsForCategory("AI Assistant");
        assertEquals(2, aiActions.size());
        assertTrue(aiActions.stream().anyMatch(a -> a.getName().equals("AI Actions...")));
        assertTrue(aiActions.stream().anyMatch(a -> a.getName().equals("Fix with AI")));

        List<IntentionAction> angularActions = registry.getAllActionsForCategory("Angular");
        assertEquals(3, angularActions.size());
        assertTrue(angularActions.stream().anyMatch(a -> a.getName().contains("Extract Angular component template")));
        assertTrue(angularActions.stream().anyMatch(a -> a.getName().contains("Inline Angular component template")));
        assertTrue(angularActions.stream().anyMatch(a -> a.getName().contains("Introduce local variable")));
    }

    @Test
    void testCssIntentionsMatchScreenshot() {
        List<IntentionAction> cssActions = registry.getAllActionsForCategory("CSS");
        assertEquals(17, cssActions.size(), "CSS must contain exactly 17 concrete intentions from Image 3");

        List<String> expectedCssNames = List.of(
                "Apply width and height of background image",
                "Change color",
                "Convert color to #-hex",
                "Convert color to gray()",
                "Convert color to hsl()",
                "Convert color to hwb()",
                "Convert color to lch()",
                "Convert color to oklch()",
                "Convert color to rgb()",
                "Create selector",
                "Expand shorthand property",
                "Extract image",
                "Extract inline CSS",
                "Extract ruleset",
                "Replace quotes",
                "Replace var() with its fallback value",
                "Replace with color name"
        );

        for (String name : expectedCssNames) {
            assertTrue(cssActions.stream().anyMatch(a -> a.getName().equals(name)), "CSS missing intention: " + name);
        }
    }

    @Test
    void testJavaSubcategoriesAndAnnotationsMatchScreenshot() {
        var subcategories = registry.getSubcategories("Java");
        assertNotNull(subcategories);
        assertTrue(subcategories.containsKey("Annotations"));
        assertTrue(subcategories.containsKey("Collections"));
        assertTrue(subcategories.containsKey("Comments"));

        // Verify Annotations items
        List<IntentionAction> annoActions = registry.getActions("Java", "Annotations");
        assertEquals(8, annoActions.size(), "Java/Annotations must contain 8 intentions from Image 4");
        assertTrue(annoActions.stream().anyMatch(a -> a.getName().equals("Add annotation")));
        assertTrue(annoActions.stream().anyMatch(a -> a.getName().equals("Annotate externally")));
        assertTrue(annoActions.stream().anyMatch(a -> a.getName().equals("Annotate overriding methods and their parameters")));
        assertTrue(annoActions.stream().anyMatch(a -> a.getName().equals("Deannotate")));
        assertTrue(annoActions.stream().anyMatch(a -> a.getName().equals("Edit method contract")));
        assertTrue(annoActions.stream().anyMatch(a -> a.getName().equals("Edit range")));
        assertTrue(annoActions.stream().anyMatch(a -> a.getName().equals("Make external annotations explicit")));
        assertTrue(annoActions.stream().anyMatch(a -> a.getName().equals("Make inferred annotations explicit")));

        // Verify "Add annotation" metadata
        IntentionAction addAnno = registry.findIntention("java.anno.add");
        assertNotNull(addAnno);
        assertTrue(addAnno.getDescription().contains("Adds an annotation"));
        assertNotNull(addAnno.getBeforeTemplate());
        assertNotNull(addAnno.getAfterTemplate());
        assertTrue(addAnno.getAfterTemplate().contains("[@Nullable]"), "After sample must highlight @Nullable");
    }

    @Test
    void testKtorIntentionsAndAttributionMatchScreenshot() {
        List<IntentionAction> ktorActions = registry.getAllActionsForCategory("Ktor");
        assertEquals(7, ktorActions.size(), "Ktor must contain 7 intentions from Image 5");

        List<String> expectedKtor = List.of(
                "Convert Ktor parameter to delegate",
                "Create test for Ktor module",
                "Create test for Ktor route",
                "Create test for Ktor routes",
                "Extract Route to a Separate Method",
                "Generate OpenAPI documentation for current module",
                "Inline Route Extension"
        );

        for (String name : expectedKtor) {
            assertTrue(ktorActions.stream().anyMatch(a -> a.getName().equals(name)), "Ktor missing intention: " + name);
        }

        IntentionAction paramDelegate = registry.findIntention("ktor.param.delegate");
        assertNotNull(paramDelegate);
        assertEquals("Ktor plugin", paramDelegate.getPluginName());
        assertEquals("Converts Ktor request parameter to delegate.", paramDelegate.getDescription());
        assertTrue(paramDelegate.getBeforeTemplate().contains("[val path: Int by call.parameters]"));
        assertTrue(paramDelegate.getAfterTemplate().contains("call.parameters[\"path\"]?.toIntOrNull()"));
    }

    @Test
    void testTriStateCheckboxCalculation() {
        // Initial state: all checked
        assertEquals(TriState.CHECKED, registry.getCategoryState("CSS"));

        // Disable one intention in CSS -> INDETERMINATE
        IntentionAction firstCss = registry.getAllActionsForCategory("CSS").get(0);
        registry.setIntentionEnabled(firstCss.getId(), false);
        assertEquals(TriState.INDETERMINATE, registry.getCategoryState("CSS"));

        // Disable all intentions in CSS -> UNCHECKED
        registry.setCategoryEnabled("CSS", false);
        assertEquals(TriState.UNCHECKED, registry.getCategoryState("CSS"));
        for (IntentionAction a : registry.getAllActionsForCategory("CSS")) {
            assertFalse(a.isEnabled());
        }

        // Enable all in CSS -> CHECKED
        registry.setCategoryEnabled("CSS", true);
        assertEquals(TriState.CHECKED, registry.getCategoryState("CSS"));
        for (IntentionAction a : registry.getAllActionsForCategory("CSS")) {
            assertTrue(a.isEnabled());
        }

        // Subcategory tri-state: Java/Annotations
        assertEquals(TriState.CHECKED, registry.getSubcategoryState("Java", "Annotations"));
        registry.setIntentionEnabled("java.anno.add", false);
        assertEquals(TriState.INDETERMINATE, registry.getSubcategoryState("Java", "Annotations"));
        assertEquals(TriState.INDETERMINATE, registry.getCategoryState("Java"));
    }

    @Test
    void testModificationTrackingAndResetApply() {
        assertFalse(registry.isModified(), "Initially should not be modified");

        registry.setIntentionEnabled("css.change.color", false);
        assertTrue(registry.isModified(), "Should be modified after disabling an action");

        // Apply
        registry.apply();
        assertFalse(registry.isModified(), "Should not be modified after apply()");
        assertFalse(registry.findIntention("css.change.color").isEnabled());

        // Modify again and reset
        registry.setIntentionEnabled("css.change.color", true);
        assertTrue(registry.isModified());

        registry.reset();
        assertFalse(registry.isModified());
        assertFalse(registry.findIntention("css.change.color").isEnabled(), "Reset should restore to applied state");
    }

    @Test
    void testBrandIsolation() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/intentions/IntentionAction.java",
                "src/main/java/dev/lumina/intentions/IntentionRegistry.java",
                "src/main/java/dev/lumina/ui/SettingsIntentionsPage.java"
        );

        String b1 = "intel" + "lij";
        String b2 = "jet" + "brains";
        String b3 = "id" + "ea";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)\\b(" + b1 + "|" + b2 + "|" + b3 + "(?!vim))\\b");

        for (String file : filesToCheck) {
            File f = new File(file);
            assertTrue(f.exists(), "File must exist: " + file);
            String content = Files.readString(f.toPath());
            java.util.regex.Matcher m = pattern.matcher(content);
            assertFalse(m.find(), "Competitor brand found in " + file + ": " + (m.reset().find() ? m.group() : ""));
        }
    }
}
