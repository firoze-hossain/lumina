package dev.lumina.livetemplates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class LiveTemplateManagerTest {

    private LiveTemplateManager manager;

    @BeforeEach
    void setUp() {
        manager = LiveTemplateManager.getInstance();
        manager.resetToDefaults();
    }

    @Test
    void testFactoryDefaultsLoaded() {
        List<LiveTemplateGroup> groups = manager.getGroups();
        assertNotNull(groups);
        assertTrue(groups.size() >= 25, "Should have at least 25 built-in groups matching IntelliJ");

        // Verify key groups from screenshots
        assertNotNull(manager.findGroup("Angular"));
        assertNotNull(manager.findGroup("Groovy"));
        assertNotNull(manager.findGroup("gRPC Request"));
        assertNotNull(manager.findGroup("HTML/XML"));
        assertNotNull(manager.findGroup("HTTP Request"));
        assertNotNull(manager.findGroup("Java"));
        assertNotNull(manager.findGroup("JavaScript"));
        assertNotNull(manager.findGroup("JavaScript Testing"));
        assertNotNull(manager.findGroup("Kotlin"));
        assertNotNull(manager.findGroup("Kubernetes"));
        assertNotNull(manager.findGroup("Maven"));
        assertNotNull(manager.findGroup("OpenAPI Specifications (.json)"));
        assertNotNull(manager.findGroup("OpenAPI Specifications (.yaml)"));
        assertNotNull(manager.findGroup("Qute"));
        assertNotNull(manager.findGroup("React"));
        assertNotNull(manager.findGroup("React hooks"));
        assertNotNull(manager.findGroup("Shell Script"));
        assertNotNull(manager.findGroup("Spring Coroutine Router DSL Kotlin"));
        assertNotNull(manager.findGroup("Spring Java"));
        assertNotNull(manager.findGroup("Spring Kotlin"));
        assertNotNull(manager.findGroup("Spring MVC Java"));
        assertNotNull(manager.findGroup("Spring MVC Kotlin"));
        assertNotNull(manager.findGroup("xsl"));
        assertNotNull(manager.findGroup("Zen CSS"));
        assertNotNull(manager.findGroup("Zen HTML"));
        assertNotNull(manager.findGroup("Zen XSL"));
    }

    @Test
    void testZenXslTemplates() {
        LiveTemplateGroup zenXsl = manager.findGroup("Zen XSL");
        assertNotNull(zenXsl);
        assertTrue(zenXsl.isEnabled());

        // Check specific items from Image 4
        LiveTemplate tExcl = zenXsl.findTemplate("!!!");
        assertNotNull(tExcl);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", tExcl.getDescription());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", tExcl.getTemplateText());
        assertTrue(tExcl.getContexts().contains(LiveTemplateContext.XML_XSL_TEXT));

        LiveTemplate tAp = zenXsl.findTemplate("ap");
        assertNotNull(tAp);
        assertEquals("<xsl:apply-templates select=\"...\" mode=\"...\" />", tAp.getDescription());

        LiveTemplate tApi = zenXsl.findTemplate("api");
        assertNotNull(tApi);
        assertEquals("<xsl:apply-imports/>", tApi.getDescription());

        LiveTemplate tAttr = zenXsl.findTemplate("attr");
        assertNotNull(tAttr);
        assertEquals("<xsl:attribute name=\"...\">...</xsl:attribute>", tAttr.getDescription());
    }

    @Test
    void testJavaImplicitAndNormalMainSubgroups() {
        LiveTemplateGroup java = manager.findGroup("Java");
        assertNotNull(java);

        boolean foundImplicitMain = false;
        boolean foundNormalMain = false;

        for (LiveTemplate t : java.getTemplates()) {
            if ("Instance 'main' methods for implicitly declared classes".equals(t.getSubgroup())) {
                if ("main".equals(t.getAbbreviation())) {
                    foundImplicitMain = true;
                    assertTrue(t.getTemplateText().contains("void main(){\n    $END$\n}"));
                }
            }
            if ("Instance 'main' methods for normal classes".equals(t.getSubgroup())) {
                if ("main".equals(t.getAbbreviation())) {
                    foundNormalMain = true;
                    assertTrue(t.getTemplateText().contains("public static void main(String[] args)"));
                }
            }
        }

        assertTrue(foundImplicitMain, "Should have implicit void main()");
        assertTrue(foundNormalMain, "Should have normal public static void main");
    }

    @Test
    void testContextFormatting() {
        assertEquals("Applicable in XML: XSL Text.",
                LiveTemplateContext.formatApplicableText(Set.of(LiveTemplateContext.XML_XSL_TEXT)));

        assertEquals("Applicable in Java: declaration inside a compact sou.",
                LiveTemplateContext.formatApplicableText(Set.of(LiveTemplateContext.JAVA_DECLARATION)));

        assertEquals("Applicable in Java: statement.",
                LiveTemplateContext.formatApplicableText(Set.of(LiveTemplateContext.JAVA_STATEMENT)));

        assertEquals("No applicable contexts.",
                LiveTemplateContext.formatApplicableText(Set.of()));
    }

    @Test
    void testDuplicateAndRemoveTemplate() {
        LiveTemplateGroup java = manager.findGroup("Java");
        LiveTemplate sout = java.findTemplate("sout");
        assertNotNull(sout);

        int countBefore = java.getTemplates().size();
        LiveTemplate copy = manager.duplicateTemplate(sout);
        assertNotNull(copy);
        assertEquals("sout_copy", copy.getAbbreviation());
        assertFalse(copy.isBuiltin());
        assertEquals(countBefore + 1, java.getTemplates().size());

        // Remove copy
        boolean removed = manager.removeTemplate(copy);
        assertTrue(removed);
        assertEquals(countBefore, java.getTemplates().size());
    }

    @Test
    void testTemplateExpansion() {
        LiveTemplate sout = manager.findTemplate("Java", "sout");
        assertNotNull(sout);

        LiveTemplateManager.ExpansionResult res = manager.expand(sout, Map.of());
        assertEquals("System.out.println();", res.text);
        assertEquals("System.out.println(".length(), res.cursorOffset);

        LiveTemplate fori = manager.findTemplate("Java", "fori");
        assertNotNull(fori);
        LiveTemplateManager.ExpansionResult res2 = manager.expand(fori, Map.of("INDEX", "k", "LIMIT", "10"));
        assertTrue(res2.text.contains("for(int k = 0; k < 10; k++)"));
    }

    @Test
    void testSaveAndReloadPersistence() {
        manager.setDefaultExpandWith("Enter");
        manager.save();

        manager.load();
        assertEquals("Enter", manager.getDefaultExpandWith());

        // Restore Tab
        manager.setDefaultExpandWith("Tab");
        manager.save();
    }
}
