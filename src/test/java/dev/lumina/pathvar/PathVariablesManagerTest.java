package dev.lumina.pathvar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PathVariablesManagerTest {

    private PathVariablesManager manager;

    @BeforeEach
    void setUp() {
        manager = PathVariablesManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaults() {
        List<PathVariable> vars = manager.getVariables();
        assertNotNull(vars);
        assertFalse(vars.isEmpty());

        PathVariable mavenRepo = manager.getVariable("MAVEN_REPOSITORY");
        assertNotNull(mavenRepo);
        assertTrue(mavenRepo.getValue().endsWith("/.m2/repository"));

        assertEquals("", manager.getIgnoredVariables());
    }

    @Test
    void testAddUpdateRemoveVariable() {
        PathVariable customVar = new PathVariable("GRADLE_USER_HOME", "/opt/gradle");
        manager.addVariable(customVar);

        PathVariable retrieved = manager.getVariable("GRADLE_USER_HOME");
        assertNotNull(retrieved);
        assertEquals("/opt/gradle", retrieved.getValue());

        // Update variable
        PathVariable updatedVar = new PathVariable("GRADLE_USER_HOME", "/custom/gradle/cache");
        manager.updateVariable(retrieved, updatedVar);

        PathVariable retrievedUpdated = manager.getVariable("GRADLE_USER_HOME");
        assertEquals("/custom/gradle/cache", retrievedUpdated.getValue());

        // Remove variable
        manager.removeVariable(retrievedUpdated);
        assertNull(manager.getVariable("GRADLE_USER_HOME"));
    }

    @Test
    void testIgnoredVariables() {
        manager.setIgnoredVariables("TEMP;TMP;CACHE_DIR");
        assertEquals("TEMP;TMP;CACHE_DIR", manager.getIgnoredVariables());

        manager.setIgnoredVariables(null);
        assertEquals("", manager.getIgnoredVariables());
    }

    @Test
    void testExpand() {
        PathVariable sdkVar = new PathVariable("KOTLIN_BUNDLED", "/opt/lumina/plugins/kotlin");
        manager.addVariable(sdkVar);

        // Null and non-variable paths
        assertNull(manager.expand(null));
        assertEquals("/usr/local/bin", manager.expand("/usr/local/bin"));

        // Expansion with single variable
        String expanded = manager.expand("$KOTLIN_BUNDLED$/kotlinc/bin");
        assertEquals("/opt/lumina/plugins/kotlin/kotlinc/bin", expanded);

        // Expansion with MAVEN_REPOSITORY
        PathVariable m2 = manager.getVariable("MAVEN_REPOSITORY");
        String m2Expanded = manager.expand("$MAVEN_REPOSITORY$/junit/junit/4.13.2.jar");
        assertEquals(m2.getValue() + "/junit/junit/4.13.2.jar", m2Expanded);
    }

    @Test
    void testSubstitute() {
        PathVariable sdkVar = new PathVariable("JDK_HOME", "/Library/Java/JavaVirtualMachines/jdk-21");
        manager.addVariable(sdkVar);

        // Null check
        assertNull(manager.substitute(null));

        // Substituted path
        String substituted = manager.substitute("/Library/Java/JavaVirtualMachines/jdk-21/bin/javac");
        assertEquals("$JDK_HOME$/bin/javac", substituted);

        // Unmatched path remains unchanged
        String unMatched = manager.substitute("/usr/bin/gcc");
        assertEquals("/usr/bin/gcc", unMatched);
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        notified.set(false);
        manager.addVariable(new PathVariable("TEST_VAR", "/path/to/test"));
        assertTrue(notified.get());

        notified.set(false);
        manager.setIgnoredVariables("TEST_VAR");
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
