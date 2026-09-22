package dev.lumina.ui;

import dev.lumina.scope.NamedScope;
import dev.lumina.scope.ScopeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FileColorManagerTest {

    private FileColorManager colorManager;

    @BeforeEach
    void setUp() {
        colorManager = FileColorManager.getInstance();
    }

    @Test
    void testDefaultConfigurationsExist() {
        List<FileColorConfiguration> configs = colorManager.getConfigurations();
        assertNotNull(configs);
        assertFalse(configs.isEmpty());

        boolean hasTests = configs.stream().anyMatch(c -> "Tests".equals(c.getScopeName()) && "Green".equalsIgnoreCase(c.getColorName()));
        boolean hasNonProject = configs.stream().anyMatch(c -> "Non-Project Files".equals(c.getScopeName()) && "Yellow".equalsIgnoreCase(c.getColorName()));
        boolean hasGenerated = configs.stream().anyMatch(c -> "Generated Files".equals(c.getScopeName()) && "Gray".equalsIgnoreCase(c.getColorName()));

        assertTrue(hasTests, "Default Tests -> Green configuration should exist");
        assertTrue(hasNonProject, "Default Non-Project Files -> Yellow configuration should exist");
        assertTrue(hasGenerated, "Default Generated Files -> Gray configuration should exist");
    }

    @Test
    void testStandardPaletteMappings() {
        assertEquals("#264065", FileColorManager.COLOR_HEX_MAP.get("Blue"));
        assertEquals("#383A42", FileColorManager.COLOR_HEX_MAP.get("Gray"));
        assertEquals("#27442D", FileColorManager.COLOR_HEX_MAP.get("Green"));
        assertEquals("#523223", FileColorManager.COLOR_HEX_MAP.get("Orange"));
        assertEquals("#4E282C", FileColorManager.COLOR_HEX_MAP.get("Rose"));
        assertEquals("#3F2D54", FileColorManager.COLOR_HEX_MAP.get("Violet"));
        assertEquals("#4A3E20", FileColorManager.COLOR_HEX_MAP.get("Yellow"));
    }

    @Test
    void testOptionsToggle() {
        boolean origEnable = colorManager.isEnableFileColors();
        boolean origTabs = colorManager.isUseInEditorTabs();
        boolean origProject = colorManager.isUseInProjectView();

        colorManager.setEnableFileColors(!origEnable);
        assertEquals(!origEnable, colorManager.isEnableFileColors());

        colorManager.setUseInEditorTabs(!origTabs);
        assertEquals(!origTabs, colorManager.isUseInEditorTabs());

        colorManager.setUseInProjectView(!origProject);
        assertEquals(!origProject, colorManager.isUseInProjectView());

        // Restore
        colorManager.setEnableFileColors(origEnable);
        colorManager.setUseInEditorTabs(origTabs);
        colorManager.setUseInProjectView(origProject);
    }

    @Test
    void testFirstMatchingScopePriority() {
        colorManager.setEnableFileColors(true);
        ScopeManager scopeManager = ScopeManager.getInstance();

        // Create two overlapping custom scopes (using non-test paths so built-in scopes do not intercept)
        NamedScope broadScope = new NamedScope("BroadScope", "file:*src/app//*", false, false);
        NamedScope narrowScope = new NamedScope("NarrowScope", "file:*src/app/special//*", false, false);
        scopeManager.addCustomScope(broadScope);
        scopeManager.addCustomScope(narrowScope);

        FileColorConfiguration cfgBroad = new FileColorConfiguration("BroadScope", "Orange", false);
        FileColorConfiguration cfgNarrow = new FileColorConfiguration("NarrowScope", "Violet", false);

        // Add broad before narrow
        colorManager.addConfiguration(cfgBroad);
        colorManager.addConfiguration(cfgNarrow);

        Path projectRoot = Path.of("/workspace/project");
        Path specialFile = projectRoot.resolve("src/app/special/SpecialFile.java");

        // Broad is placed before narrow -> Orange (#523223)
        assertEquals("#523223", colorManager.getFileColorHex(specialFile, projectRoot));

        // Move narrow above broad
        int narrowIdx = colorManager.getConfigurations().indexOf(cfgNarrow);
        colorManager.moveUp(narrowIdx);

        // Now narrow is evaluated first -> Violet (#3F2D54)
        assertEquals("#3F2D54", colorManager.getFileColorHex(specialFile, projectRoot));

        // Clean up
        colorManager.removeConfiguration(cfgBroad);
        colorManager.removeConfiguration(cfgNarrow);
        scopeManager.removeCustomScope(broadScope);
        scopeManager.removeCustomScope(narrowScope);
    }

    @Test
    void testCustomHexColor() {
        colorManager.setEnableFileColors(true);
        ScopeManager scopeManager = ScopeManager.getInstance();

        NamedScope customScope = new NamedScope("CustomScope", "file:*src/special//*", false, false);
        scopeManager.addCustomScope(customScope);

        FileColorConfiguration cfg = new FileColorConfiguration("CustomScope", "Custom", false);
        cfg.setCustomHex("#123456");
        colorManager.addConfiguration(cfg);

        // Move cfg to the top so it's matched first
        int idx = colorManager.getConfigurations().indexOf(cfg);
        while (idx > 0) {
            colorManager.moveUp(idx);
            idx--;
        }

        Path projectRoot = Path.of("/workspace/project");
        Path file = projectRoot.resolve("src/special/File.java");

        assertEquals("#123456", colorManager.getFileColorHex(file, projectRoot));

        // When disabled, null is returned
        colorManager.setEnableFileColors(false);
        assertNull(colorManager.getFileColorHex(file, projectRoot));
        colorManager.setEnableFileColors(true);

        // Clean up
        colorManager.removeConfiguration(cfg);
        scopeManager.removeCustomScope(customScope);
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        colorManager.addListener(listener);

        FileColorConfiguration temp = new FileColorConfiguration("Tests", "Blue", false);
        colorManager.addConfiguration(temp);
        assertTrue(notified.get());

        notified.set(false);
        colorManager.removeConfiguration(temp);
        assertTrue(notified.get());

        colorManager.removeListener(listener);
    }
}
