package dev.lumina.settings;

import dev.lumina.settings.DuplicatesSettings.LanguageProfile;
import dev.lumina.util.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DuplicatesSettingsTest {

    private DuplicatesSettings settings;

    @BeforeEach
    void setUp() {
        Settings.clear();
        settings = DuplicatesSettings.getInstance();
        settings.resetToDefaults();
    }

    @AfterEach
    void tearDown() {
        Settings.clear();
    }

    @Test
    void testDefaultLanguagesMatchIntelliJIdea() {
        List<LanguageProfile> profiles = settings.getProfiles();
        assertNotNull(profiles);
        assertEquals(15, profiles.size());

        List<String> names = profiles.stream().map(LanguageProfile::getName).toList();
        assertTrue(names.contains("Kotlin"));
        assertTrue(names.contains("XML"));
        assertTrue(names.contains("HTML"));
        assertTrue(names.contains("XHTML"));
        assertTrue(names.contains("Java"));
        assertTrue(names.contains("Style Sheets"));
        assertTrue(names.contains("Rust"));
        assertTrue(names.contains("Go"));
        assertTrue(names.contains("Ruby"));
        assertTrue(names.contains("Python"));
        assertTrue(names.contains("Groovy"));
        assertTrue(names.contains("JavaScript"));
        assertTrue(names.contains("TypeScript"));
        assertTrue(names.contains("PHP"));
        assertTrue(names.contains("Scala"));
    }

    @Test
    void testDefaultDetectionOptions() {
        LanguageProfile kotlin = settings.getProfile("Kotlin");
        assertNotNull(kotlin);
        assertTrue(kotlin.isEnabled());
        assertTrue(kotlin.isDifferentVariableNames());
        assertFalse(kotlin.isDifferentFunctionNames());
        assertFalse(kotlin.isDifferentConstantValues());
    }

    @Test
    void testCopyAndIsModified() {
        DuplicatesSettings copy = settings.copy();
        assertFalse(copy.isModified(settings));

        LanguageProfile java = copy.getProfile("Java");
        assertNotNull(java);
        java.setEnabled(false);
        assertTrue(copy.isModified(settings));

        settings.applyFrom(copy);
        assertFalse(settings.getProfile("Java").isEnabled());
        assertFalse(copy.isModified(settings));
    }

    @Test
    void testPersistence() {
        LanguageProfile python = settings.getProfile("Python");
        assertNotNull(python);
        python.setDifferentConstantValues(true);
        python.setEnabled(false);
        settings.save();

        DuplicatesSettings reloaded = new DuplicatesSettings();
        reloaded.load();

        LanguageProfile reloadedPython = reloaded.getProfile("Python");
        assertNotNull(reloadedPython);
        assertFalse(reloadedPython.isEnabled());
        assertTrue(reloadedPython.isDifferentConstantValues());
    }
}
