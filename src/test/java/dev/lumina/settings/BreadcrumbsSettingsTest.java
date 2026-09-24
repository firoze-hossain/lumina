package dev.lumina.settings;

import dev.lumina.settings.BreadcrumbsSettings.BreadcrumbsPlacement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class BreadcrumbsSettingsTest {

    private BreadcrumbsSettings settings;

    @BeforeEach
    void setUp() {
        dev.lumina.util.Settings.clear();
        settings = BreadcrumbsSettings.getInstance();
        settings.resetToDefaults();
    }

    @AfterEach
    void tearDown() {
        dev.lumina.util.Settings.clear();
    }

    @Test
    void testDefaultsMatchIntelliJIdea() {
        assertTrue(settings.isShowBreadcrumbs());
        assertEquals(BreadcrumbsPlacement.BOTTOM, settings.getPlacement());

        assertEquals(29, settings.getLanguages().size());

        // Default 6 enabled languages in IntelliJ IDEA screenshot
        assertTrue(settings.isLanguageEnabled("ERB"));
        assertTrue(settings.isLanguageEnabled("Go"));
        assertTrue(settings.isLanguageEnabled("PHP"));
        assertTrue(settings.isLanguageEnabled("protobuf"));
        assertTrue(settings.isLanguageEnabled("Ruby"));
        assertTrue(settings.isLanguageEnabled("Scala"));

        // Others disabled by default
        assertFalse(settings.isLanguageEnabled("Java"));
        assertFalse(settings.isLanguageEnabled("Rust"));
        assertFalse(settings.isLanguageEnabled("Python"));
        assertFalse(settings.isLanguageEnabled("CSS"));
        assertFalse(settings.isLanguageEnabled("HTML"));
    }

    @Test
    void testLanguageEnabledForFile() {
        assertTrue(settings.isLanguageEnabledForFile(Path.of("/project/main.go")));
        assertTrue(settings.isLanguageEnabledForFile(Path.of("/project/index.php")));
        assertTrue(settings.isLanguageEnabledForFile(Path.of("/project/app.rb")));
        assertFalse(settings.isLanguageEnabledForFile(Path.of("/project/Main.java")));
        assertFalse(settings.isLanguageEnabledForFile(Path.of("/project/src/lib.rs")));

        settings.setLanguageEnabled("Java", true);
        assertTrue(settings.isLanguageEnabledForFile(Path.of("/project/Main.java")));
    }

    @Test
    void testPersistenceAndReload() {
        settings.setShowBreadcrumbs(false);
        settings.setPlacement(BreadcrumbsPlacement.TOP);
        settings.setLanguageEnabled("Java", true);
        settings.setLanguageEnabled("Rust", true);
        settings.setLanguageEnabled("PHP", false);

        settings.save();

        BreadcrumbsSettings loaded = new BreadcrumbsSettings();
        assertFalse(loaded.isShowBreadcrumbs());
        assertEquals(BreadcrumbsPlacement.TOP, loaded.getPlacement());
        assertTrue(loaded.isLanguageEnabled("Java"));
        assertTrue(loaded.isLanguageEnabled("Rust"));
        assertFalse(loaded.isLanguageEnabled("PHP"));
        assertTrue(loaded.isLanguageEnabled("Go"));
    }

    @Test
    void testIsModifiedDetection() {
        BreadcrumbsSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setPlacement(BreadcrumbsPlacement.TOP);
        assertTrue(settings.isModified(copy));

        copy.copyFrom(settings);
        assertFalse(settings.isModified(copy));

        copy.setLanguageEnabled("Java", true);
        assertTrue(settings.isModified(copy));
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        BreadcrumbsSettings.Listener listener = s -> notified.set(true);
        settings.addListener(listener);

        settings.setPlacement(BreadcrumbsPlacement.TOP);
        settings.save();

        assertTrue(notified.get());
        settings.removeListener(listener);
    }
}
