package dev.lumina.javascript;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaScriptSettingsManagerTest {

    private JavaScriptSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = JavaScriptSettingsManager.getInstance();
        manager.resetDefaults();
    }

    @Test
    void testDefaultValues() {
        // Language version default ECMAScript 6+ matching Screenshot 2
        assertEquals(JavaScriptLanguageVersion.ECMASCRIPT_6_PLUS, manager.getLanguageVersion());
        assertEquals("ECMAScript 6+", manager.getLanguageVersion().getDisplayName());
        assertEquals("ECMAScript 2015+, some proposals and JSX", manager.getLanguageVersion().getDescription());

        // ESLint defaults matching Screenshot 4
        ESLintSettings eslint = manager.getEslintSettings();
        assertEquals(ESLintSettings.Mode.DISABLED, eslint.getMode());
        assertEquals("{**/*,*}.{js,ts,jsx,tsx,cjs,cts,mjs,mts,html,vue}", eslint.getRunForFiles());
        assertFalse(eslint.isRunOnSave());

        // JSHint defaults matching Screenshot 5
        JSHintSettings jshint = manager.getJshintSettings();
        assertFalse(jshint.isEnabled());
        assertFalse(jshint.isUseConfigFiles());
        assertEquals("2.13.6 (bundled)", jshint.getVersion());
        assertTrue(jshint.isOptionEnabled("bitwise"));
        assertTrue(jshint.isOptionEnabled("curly"));
        assertTrue(jshint.isOptionEnabled("eqeqeq"));
        assertTrue(jshint.isOptionEnabled("forin"));
        assertTrue(jshint.isOptionEnabled("noarg"));
        assertTrue(jshint.isOptionEnabled("noempty"));
        assertTrue(jshint.isOptionEnabled("nonew"));
        assertTrue(jshint.isOptionEnabled("undef"));
        assertFalse(jshint.isOptionEnabled("camelcase"));
    }

    @Test
    void testLanguageVersionPersistence() {
        manager.setLanguageVersion(JavaScriptLanguageVersion.FLOW);
        assertEquals(JavaScriptLanguageVersion.FLOW, manager.getLanguageVersion());

        manager.loadSettings();
        assertEquals(JavaScriptLanguageVersion.FLOW, manager.getLanguageVersion());
    }

    @Test
    void testESLintSettingsPersistence() {
        ESLintSettings custom = new ESLintSettings(ESLintSettings.Mode.AUTOMATIC, "**/*.js", true);
        manager.setEslintSettings(custom);

        ESLintSettings loaded = manager.getEslintSettings();
        assertEquals(ESLintSettings.Mode.AUTOMATIC, loaded.getMode());
        assertEquals("**/*.js", loaded.getRunForFiles());
        assertTrue(loaded.isRunOnSave());
    }

    @Test
    void testJSHintSettingsPersistence() {
        JSHintSettings custom = new JSHintSettings();
        custom.setEnabled(true);
        custom.setUseConfigFiles(true);
        custom.setOptionEnabled("camelcase", true);
        manager.setJshintSettings(custom);

        JSHintSettings loaded = manager.getJshintSettings();
        assertTrue(loaded.isEnabled());
        assertTrue(loaded.isUseConfigFiles());
        assertTrue(loaded.isOptionEnabled("camelcase"));
    }

    @Test
    void testResetDefaults() {
        manager.setLanguageVersion(JavaScriptLanguageVersion.REACT_JSX);
        manager.resetDefaults();
        assertEquals(JavaScriptLanguageVersion.ECMASCRIPT_6_PLUS, manager.getLanguageVersion());
    }
}
