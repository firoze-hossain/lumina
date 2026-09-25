package dev.lumina.settings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GutterIconsSettingsTest {

    private Path originalProps;
    private byte[] originalContent;

    @BeforeEach
    void setUp() throws IOException {
        Path props = Path.of(System.getProperty("user.home", "."), ".lumina", "lumina.properties");
        originalProps = props;
        if (Files.exists(props)) {
            originalContent = Files.readAllBytes(props);
        } else {
            originalContent = null;
        }
        GutterIconsSettings.getInstance().initDefaults();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (originalContent != null && originalProps != null) {
            Files.write(originalProps, originalContent);
        } else if (originalProps != null && Files.exists(originalProps)) {
            Files.deleteIfExists(originalProps);
        }
        GutterIconsSettings.getInstance().initDefaults();
    }

    @Test
    void testDefaults() {
        GutterIconsSettings s = GutterIconsSettings.getInstance();
        assertTrue(s.isShowGutterIcons());

        // Default enabled
        assertTrue(s.isIconConfiguredEnabled("java.implemented.method"));
        assertTrue(s.isIconConfiguredEnabled("java.overriding.method"));
        assertTrue(s.isIconConfiguredEnabled("common.run.line.marker"));
        assertTrue(s.isIconConfiguredEnabled("spring.bean"));
        assertTrue(s.isIconConfiguredEnabled("springweb.related.views"));
        assertTrue(s.isIconConfiguredEnabled("springweb.request.mappings"));
        assertTrue(s.isIconEnabled("java.implemented.method"));

        // Default disabled in IntelliJ
        assertFalse(s.isIconConfiguredEnabled("java.inferred.contract.annotations"));
        assertFalse(s.isIconConfiguredEnabled("java.inferred.nullability.annotations"));
        assertFalse(s.isIconEnabled("java.inferred.contract.annotations"));
        assertFalse(s.isIconEnabled("java.inferred.nullability.annotations"));
    }

    @Test
    void testMasterSwitchOverridesAll() {
        GutterIconsSettings s = GutterIconsSettings.getInstance();
        assertTrue(s.isIconEnabled("java.implemented.method"));

        s.setShowGutterIcons(false);
        assertFalse(s.isIconEnabled("java.implemented.method"));
        // Configured state is still true
        assertTrue(s.isIconConfiguredEnabled("java.implemented.method"));

        s.setShowGutterIcons(true);
        assertTrue(s.isIconEnabled("java.implemented.method"));
    }

    @Test
    void testToggleIcon() {
        GutterIconsSettings s = GutterIconsSettings.getInstance();
        assertTrue(s.isIconConfiguredEnabled("common.run.line.marker"));

        s.setIconEnabled("common.run.line.marker", false);
        assertFalse(s.isIconConfiguredEnabled("common.run.line.marker"));
        assertFalse(s.isIconEnabled("common.run.line.marker"));

        s.setIconEnabled("common.run.line.marker", true);
        assertTrue(s.isIconConfiguredEnabled("common.run.line.marker"));
        assertTrue(s.isIconEnabled("common.run.line.marker"));
    }

    @Test
    void testDynamicRegistration() {
        GutterIconsSettings s = GutterIconsSettings.getInstance();
        String customId = "myplugin.custom.marker";
        s.registerDescriptor(new GutterIconsSettings.GutterIconDescriptor(
                customId, "Custom Plugin", "Custom Marker", true, "CUSTOM"
        ));

        assertTrue(s.getAllDescriptors().stream().anyMatch(d -> d.id().equals(customId)));
        assertTrue(s.isIconEnabled(customId));
    }

    @Test
    void testPersistenceRoundTrip() {
        GutterIconsSettings s = GutterIconsSettings.getInstance();
        s.setShowGutterIcons(false);
        s.setIconEnabled("java.overriding.method", false);
        s.setIconEnabled("java.inferred.contract.annotations", true);
        s.save();

        GutterIconsSettings loaded = new GutterIconsSettings();
        loaded.load();

        assertFalse(loaded.isShowGutterIcons());
        assertFalse(loaded.isIconConfiguredEnabled("java.overriding.method"));
        assertTrue(loaded.isIconConfiguredEnabled("java.inferred.contract.annotations"));
    }

    @Test
    void testListeners() {
        GutterIconsSettings s = GutterIconsSettings.getInstance();
        AtomicBoolean fired = new AtomicBoolean(false);
        s.addListener(changed -> fired.set(true));

        s.save();
        assertTrue(fired.get());
    }
}
