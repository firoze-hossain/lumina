package dev.lumina.copyright;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CopyrightManagerTest {

    private CopyrightManager manager;

    @BeforeEach
    void setUp() {
        manager = CopyrightManager.getInstance();
        manager.resetToDefaults();
    }

    @Test
    void testInitialState() {
        assertEquals("No copyright", manager.getDefaultProjectCopyright());
        assertTrue(manager.getProfiles().isEmpty());
        assertTrue(manager.getScopeMappings().isEmpty());
    }

    @Test
    void testProfileLifecycle() {
        CopyrightProfile profile = new CopyrightProfile("Apache 2.0", true);
        profile.setNotice("/* Licensed under Apache License 2.0 */");
        profile.setKeyword("Apache");

        manager.addProfile(profile);

        CopyrightProfile found = manager.findProfileByName("Apache 2.0");
        assertNotNull(found);
        assertEquals("Apache 2.0", found.getName());
        assertEquals("Apache", found.getKeyword());
        assertTrue(found.isShared());

        // Equivalence
        CopyrightProfile copy = found.copy();
        assertTrue(found.isEquivalentTo(copy));
        copy.setNotice("Changed");
        assertFalse(found.isEquivalentTo(copy));

        // Removal
        boolean removed = manager.removeProfile("Apache 2.0");
        assertTrue(removed);
        assertNull(manager.findProfileByName("Apache 2.0"));
    }

    @Test
    void testScopeMappings() {
        manager.addScopeMapping("Project Files", "MIT");
        manager.addScopeMapping("Tests", "GPL");
        manager.addScopeMapping("Production", "Apache");

        assertEquals(3, manager.getScopeMappings().size());
        assertEquals("Project Files", manager.getScopeMappings().get(0).getScope());
        assertEquals("Tests", manager.getScopeMappings().get(1).getScope());
        assertEquals("Production", manager.getScopeMappings().get(2).getScope());

        // Move down
        manager.moveScopeMappingDown(0);
        assertEquals("Tests", manager.getScopeMappings().get(0).getScope());
        assertEquals("Project Files", manager.getScopeMappings().get(1).getScope());

        // Move up
        manager.moveScopeMappingUp(1);
        assertEquals("Project Files", manager.getScopeMappings().get(0).getScope());
        assertEquals("Tests", manager.getScopeMappings().get(1).getScope());

        // Remove
        manager.removeScopeMapping(1);
        assertEquals(2, manager.getScopeMappings().size());
        assertEquals("Production", manager.getScopeMappings().get(1).getScope());
    }

    @Test
    void testEvaluateNotice() {
        CopyrightProfile p = new CopyrightProfile("Custom", "Copyright (c) $today.year $project.name in $file.fileName", "Copyright", true, false);
        manager.addProfile(p);

        String result = manager.evaluateNotice("Custom", "Main.java", "Lumina");
        assertNotNull(result);
        int currentYear = LocalDate.now().getYear();
        assertTrue(result.contains(String.valueOf(currentYear)));
        assertTrue(result.contains("Lumina"));
        assertTrue(result.contains("Main.java"));
    }
}
