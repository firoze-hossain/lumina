package dev.lumina.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PostfixCompletionSettingsTest {

    private PostfixCompletionSettings settings;

    @BeforeEach
    void setUp() {
        settings = new PostfixCompletionSettings();
        settings.initDefaults();
    }

    @Test
    void testDefaultsMatchScreenshots() {
        assertTrue(settings.isEnablePostfixCompletion());
        assertFalse(settings.isShowAsCommandCompletions());
        assertEquals("Tab", settings.getExpandShortcut());

        for (String lang : PostfixCompletionSettings.ALL_LANGUAGES) {
            assertFalse(settings.isLanguageDisabled(lang));
            assertFalse(settings.getTemplatesForLanguage(lang).isEmpty());
        }

        // Verify key Java templates exist and enabled
        var javaSout = settings.getTemplate("java.sout");
        assertNotNull(javaSout);
        assertTrue(javaSout.isEnabled());
        assertEquals("sout", javaSout.getKey());
        assertEquals("Java", javaSout.getLanguage());

        var javaNull = settings.getTemplate("java.null");
        assertNotNull(javaNull);
        assertTrue(javaNull.isEnabled());

        var rustDbg = settings.getTemplate("rust.dbg");
        assertNotNull(rustDbg);
        assertTrue(rustDbg.isEnabled());
    }

    @Test
    void testIsModified() {
        PostfixCompletionSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setEnablePostfixCompletion(false);
        assertTrue(settings.isModified(copy));
        copy.setEnablePostfixCompletion(true);
        assertFalse(settings.isModified(copy));

        copy.setShowAsCommandCompletions(true);
        assertTrue(settings.isModified(copy));
        copy.setShowAsCommandCompletions(false);
        assertFalse(settings.isModified(copy));

        copy.setExpandShortcut("Enter");
        assertTrue(settings.isModified(copy));
        copy.setExpandShortcut("Tab");
        assertFalse(settings.isModified(copy));

        copy.setLanguageEnabled("Rust", false);
        assertTrue(settings.isModified(copy));
        copy.setLanguageEnabled("Rust", true);
        assertFalse(settings.isModified(copy));

        var t = copy.getTemplate("java.sout");
        t.setEnabled(false);
        assertTrue(settings.isModified(copy));
        t.setEnabled(true);
        assertFalse(settings.isModified(copy));
    }

    @Test
    void testCustomTemplateAddRemove() {
        int initialSize = settings.getAllTemplates().size();

        settings.addCustomTemplate("Java", "mytest", "System.out.println(\"test: \" + expr)",
                "[expr].mytest", "System.out.println(\"test: \" + expr);", "Custom test logger");

        assertEquals(initialSize + 1, settings.getAllTemplates().size());

        String customId = settings.getAllTemplates().keySet().stream()
                .filter(k -> k.startsWith("custom.java.mytest"))
                .findFirst()
                .orElse(null);

        assertNotNull(customId);
        var item = settings.getTemplate(customId);
        assertTrue(item.isCustom());
        assertEquals("mytest", item.getKey());

        settings.removeTemplate(customId);
        assertEquals(initialSize, settings.getAllTemplates().size());
        assertNull(settings.getTemplate(customId));
    }

    @Test
    void testLiveExpansion() {
        Optional<String> sout = settings.expand("Java", "sout", "message");
        assertTrue(sout.isPresent());
        assertEquals("System.out.println(message);", sout.get());

        Optional<String> nullCheck = settings.expand("Java", "null", "user");
        assertTrue(nullCheck.isPresent());
        assertTrue(nullCheck.get().contains("if (user == null)"));

        Optional<String> not = settings.expand("Java", "not", "isAvailable");
        assertTrue(not.isPresent());
        assertEquals("!isAvailable", not.get());

        Optional<String> rustDbg = settings.expand("Rust", "dbg", "state");
        assertTrue(rustDbg.isPresent());
        assertEquals("dbg!(state);", rustDbg.get());

        Optional<String> tsLog = settings.expand("TypeScript", "log", "response");
        assertTrue(tsLog.isPresent());
        assertEquals("console.log(response);", tsLog.get());

        // When language is disabled, expansion returns empty
        settings.setLanguageEnabled("Java", false);
        Optional<String> disabledExp = settings.expand("Java", "sout", "message");
        assertFalse(disabledExp.isPresent());

        // When master toggle is disabled, expansion returns empty
        settings.setLanguageEnabled("Java", true);
        settings.setEnablePostfixCompletion(false);
        assertFalse(settings.expand("Java", "sout", "message").isPresent());
    }

    @Test
    void testSaveAndLoad() {
        settings.setEnablePostfixCompletion(false);
        settings.setShowAsCommandCompletions(true);
        settings.setExpandShortcut("Space");
        settings.setLanguageEnabled("Rust", false);
        settings.getTemplate("java.sout").setEnabled(false);

        settings.save();

        PostfixCompletionSettings loaded = new PostfixCompletionSettings();
        loaded.load();

        assertFalse(loaded.isEnablePostfixCompletion());
        assertTrue(loaded.isShowAsCommandCompletions());
        assertEquals("Space", loaded.getExpandShortcut());
        assertTrue(loaded.isLanguageDisabled("Rust"));
        assertFalse(loaded.getTemplate("java.sout").isEnabled());

        // Reset
        settings.initDefaults();
        settings.save();
    }
}
