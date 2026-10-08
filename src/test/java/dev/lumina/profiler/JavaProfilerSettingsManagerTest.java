package dev.lumina.profiler;

import dev.lumina.util.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JavaProfilerSettingsManagerTest {

    private JavaProfilerSettingsManager manager;

    @BeforeEach
    void setUp() {
        Settings.clear();
        manager = JavaProfilerSettingsManager.getInstance();
        manager.setProfilers(List.of(JavaProfilerSettingsManager.createDefaultProfiler()));
        manager.resetFilterGroupsToDefaults();
    }

    @Test
    void testDefaultProfilerConfig() {
        List<JavaProfilerConfig> list = manager.getProfilers();
        assertNotNull(list);
        assertEquals(1, list.size());

        JavaProfilerConfig defaultCfg = list.get(0);
        assertEquals("IntelliJ Profiler", defaultCfg.getName());
        assertEquals("event=wall,interval=10ms,jfrsync=profile", defaultCfg.getAgentOptions());
        assertEquals("Bundled (Version: 4.1)", defaultCfg.getAgentPath());
        assertFalse(defaultCfg.isCollectNativeCalls());
    }

    @Test
    void testProfilerConfigCloneAndEquals() {
        JavaProfilerConfig cfg1 = new JavaProfilerConfig("Test Profiler", "event=cpu", "/path/to/agent.so", true);
        JavaProfilerConfig cfg2 = cfg1.clone();

        assertEquals(cfg1, cfg2);
        assertEquals(cfg1.hashCode(), cfg2.hashCode());

        cfg2.setName("Different Name");
        assertNotEquals(cfg1, cfg2);
    }

    @Test
    void testAddDuplicateRemoveMoveProfilers() {
        JavaProfilerConfig p1 = manager.getProfilers().get(0);

        JavaProfilerConfig p2 = new JavaProfilerConfig("Custom Agent", "event=alloc", "/opt/agent.so", false);
        manager.addProfiler(p2);
        assertEquals(2, manager.getProfilers().size());
        assertEquals(p2.getId(), manager.getSelectedProfilerId());

        // Duplicate
        JavaProfilerConfig p3 = manager.duplicateProfiler(p2.getId());
        assertNotNull(p3);
        assertEquals("Custom Agent (Copy)", p3.getName());
        assertEquals(3, manager.getProfilers().size());

        // Move Up
        manager.moveProfilerUp(p3.getId());
        assertEquals(p3.getId(), manager.getProfilers().get(1).getId());

        // Move Down
        manager.moveProfilerDown(p1.getId());
        assertEquals(p1.getId(), manager.getProfilers().get(1).getId());

        // Remove
        manager.removeProfiler(p3.getId());
        assertEquals(2, manager.getProfilers().size());
        assertNull(manager.getProfilerById(p3.getId()));
    }

    @Test
    void testDefaultFilterGroups() {
        List<ProfilerFilterGroup> groups = manager.getFilterGroups();
        assertNotNull(groups);
        assertEquals(14, groups.size());

        // Verify specific default groups from Screenshot 2
        assertEquals("Java", groups.get(0).getGroupName());
        assertTrue(groups.get(0).getFiltersPattern().contains("java.*"));
        assertTrue(groups.get(0).getFiltersPattern().contains("jdk.internal.*"));

        assertEquals("Google", groups.get(1).getGroupName());
        assertEquals("com.google.*", groups.get(1).getFiltersPattern());

        assertEquals("XML & JSON", groups.get(12).getGroupName());
        assertTrue(groups.get(12).getFiltersPattern().contains("org.w3c.*"));

        assertEquals("Native & Others", groups.get(13).getGroupName());
        assertEquals("[unknown],_*,*::*", groups.get(13).getFiltersPattern());
    }

    @Test
    void testFilterGroupMatchingWildcard() {
        ProfilerFilterGroup javaGroup = new ProfilerFilterGroup("Java", "java.*,javax.*,sun.*");
        assertTrue(javaGroup.matches("java.lang.String"));
        assertTrue(javaGroup.matches("javax.swing.JButton"));
        assertTrue(javaGroup.matches("sun.misc.Unsafe"));
        assertFalse(javaGroup.matches("org.apache.commons.lang3.StringUtils"));

        ProfilerFilterGroup nativeGroup = new ProfilerFilterGroup("Native", "[unknown],_*,*::*");
        assertTrue(nativeGroup.matches("[unknown]"));
        assertTrue(nativeGroup.matches("_init"));
        assertTrue(nativeGroup.matches("libc::malloc"));
        assertFalse(nativeGroup.matches("java.lang.Object"));

        // Global check on manager
        assertTrue(manager.matchesAnyFilter("java.util.List"));
        assertTrue(manager.matchesAnyFilter("com.google.common.collect.Lists"));
        assertFalse(manager.matchesAnyFilter("my.company.app.Main"));
    }

    @Test
    void testPersistence() {
        JavaProfilerConfig custom = new JavaProfilerConfig("Async Profiler Custom", "event=cache-misses", "bundled", true);
        manager.addProfiler(custom);

        ProfilerFilterGroup customGroup = new ProfilerFilterGroup("MyProject", "com.mycompany.*");
        manager.addFilterGroup(customGroup);

        manager.saveSettings();

        // Reload
        manager.loadSettings();
        assertNotNull(manager.getProfilerById(custom.getId()));
        assertTrue(manager.getFilterGroups().stream().anyMatch(g -> "MyProject".equals(g.getGroupName())));
    }
}
