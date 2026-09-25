package dev.lumina.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleSettingsTest {

    private ConsoleSettings settings;

    @BeforeEach
    void setUp() {
        settings = new ConsoleSettings();
        settings.initDefaults();
    }

    @Test
    void testDefaultsMatchIntelliJScreenshots() {
        assertFalse(settings.isUseSoftWraps(), "Soft wraps should be disabled by default");
        assertEquals(300, settings.getHistorySize(), "History size should be 300 by default");
        assertFalse(settings.isOverrideCycleBufferSize(), "Override buffer size should be false by default");
        assertEquals(1024, settings.getCycleBufferSizeKb(), "Cycle buffer size should be 1024 KB by default");
        assertEquals("<System Default: UTF-8>", settings.getDefaultEncoding(), "Encoding should be system default UTF-8");

        assertEquals(23, settings.getFoldingPatterns().size());
        assertTrue(settings.getFoldingPatterns().contains("/gems/cucumber"));
        assertTrue(settings.getFoldingPatterns().contains("at com.intellij.jpa."));
        assertTrue(settings.getFoldingPatterns().contains("at org.testng.TestRunner."));

        assertEquals(3, settings.getExceptionPatterns().size());
        assertTrue(settings.getExceptionPatterns().contains("at org.codehaus.groovy.runtime.DefaultGroovyMethods."));

        assertTrue(settings.isFoldStackTraceLongerThan());
        assertEquals(8, settings.getStackTraceLinesLimit());
    }

    @Test
    void testShouldFoldLineLogic() {
        // Folding patterns match
        assertTrue(settings.shouldFoldLine("    /gems/cucumber/lib/cucumber.rb:10"));
        assertTrue(settings.shouldFoldLine("    at com.intellij.junit4.JUnit4TestRunner.run(JUnit4TestRunner.java:50)"));
        assertTrue(settings.shouldFoldLine("    at org.springframework.web.servlet.DispatcherServlet.doDispatch"));

        // Exception patterns override
        assertFalse(settings.shouldFoldLine("    at org.codehaus.groovy.runtime.DefaultGroovyMethods.call()"));
        assertFalse(settings.shouldFoldLine("    at org.codehaus.groovy.vmplugin.v5.PluginDefaultGroovyMethods.init()"));

        // Regular line
        assertFalse(settings.shouldFoldLine("    at dev.lumina.Main.main(Main.java:25)"));
        assertFalse(settings.shouldFoldLine("Exception in thread \"main\" java.lang.NullPointerException"));
    }

    @Test
    void testIsModified() {
        ConsoleSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setUseSoftWraps(true);
        assertTrue(settings.isModified(copy));

        copy.setUseSoftWraps(false);
        assertFalse(settings.isModified(copy));

        copy.setHistorySize(500);
        assertTrue(settings.isModified(copy));

        copy.setHistorySize(300);
        assertFalse(settings.isModified(copy));

        copy.setFoldingPatterns(List.of("/custom/pattern"));
        assertTrue(settings.isModified(copy));
    }

    @Test
    void testMutationAndPersistence() {
        int orig = settings.getHistorySize();
        try {
            settings.setHistorySize(450);
            settings.save();

            ConsoleSettings loaded = new ConsoleSettings();
            assertEquals(450, loaded.getHistorySize());
        } finally {
            settings.setHistorySize(orig);
            settings.save();
        }
    }

    @Test
    void testListeners() {
        boolean[] called = {false};
        ConsoleSettings.Listener l = s -> called[0] = true;
        settings.addListener(l);

        settings.setUseSoftWraps(!settings.isUseSoftWraps());
        settings.save();

        assertTrue(called[0]);
        settings.removeListener(l);
    }
}
