package dev.lumina.scope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ScopeManagerTest {

    private ScopeManager scopeManager;

    @BeforeEach
    void setUp() {
        scopeManager = ScopeManager.getInstance();
    }

    @Test
    void testBuiltInScopesExist() {
        List<NamedScope> builtIn = scopeManager.getBuiltInScopes();
        assertNotNull(builtIn);
        assertFalse(builtIn.isEmpty());

        assertNotNull(scopeManager.getScope("Project Files"));
        assertNotNull(scopeManager.getScope("Tests"));
        assertNotNull(scopeManager.getScope("Non-Project Files"));
        assertNotNull(scopeManager.getScope("Generated Files"));
        assertNotNull(scopeManager.getScope("Open Files"));
        assertNotNull(scopeManager.getScope("Current File"));
        assertNotNull(scopeManager.getScope("Scratches and Consoles"));

        NamedScope testsScope = scopeManager.getScope("Tests");
        assertTrue(testsScope.isBuiltIn());
        assertEquals("file:*src/test//*", testsScope.getPattern());
    }

    @Test
    void testAddAndRemoveCustomScope() {
        NamedScope custom = new NamedScope("Frontend Files", "file:*src/frontend//*", false, false);
        scopeManager.addCustomScope(custom);

        NamedScope retrieved = scopeManager.getScope("Frontend Files");
        assertNotNull(retrieved);
        assertEquals("Frontend Files", retrieved.getName());
        assertEquals("file:*src/frontend//*", retrieved.getPattern());
        assertFalse(retrieved.isShared());
        assertFalse(retrieved.isBuiltIn());

        scopeManager.removeCustomScope(custom);
        assertNull(scopeManager.getScope("Frontend Files"));
    }

    @Test
    void testPatternMatchingTestsScope() {
        Path projectRoot = Path.of("/workspace/project");
        Path testFile = projectRoot.resolve("src/test/java/MyTest.java");
        Path mainFile = projectRoot.resolve("src/main/java/Main.java");

        assertTrue(scopeManager.matches("Tests", testFile, projectRoot));
        assertFalse(scopeManager.matches("Tests", mainFile, projectRoot));
    }

    @Test
    void testPatternMatchingGeneratedFiles() {
        Path projectRoot = Path.of("/workspace/project");
        Path genFile = projectRoot.resolve("target/generated-sources/annotations/Gen.java");
        Path regularFile = projectRoot.resolve("src/main/java/Regular.java");

        assertTrue(scopeManager.matches("Generated Files", genFile, projectRoot));
        assertFalse(scopeManager.matches("Generated Files", regularFile, projectRoot));
    }

    @Test
    void testPatternMatchingNonProjectFiles() {
        Path projectRoot = Path.of("/workspace/project");
        Path outsideFile = Path.of("/other/location/External.java");
        Path insideFile = projectRoot.resolve("src/main/java/Inside.java");

        assertTrue(scopeManager.matches("Non-Project Files", outsideFile, projectRoot));
        assertFalse(scopeManager.matches("Non-Project Files", insideFile, projectRoot));
    }

    @Test
    void testCustomScopeOrderingAndListeners() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        scopeManager.addListener(listener);

        NamedScope scopeA = new NamedScope("Scope A", "file:*a//*", false, false);
        NamedScope scopeB = new NamedScope("Scope B", "file:*b//*", true, false);

        scopeManager.addCustomScope(scopeA);
        assertTrue(notified.get());

        notified.set(false);
        scopeManager.addCustomScope(scopeB);
        assertTrue(notified.get());

        List<NamedScope> custom = scopeManager.getCustomScopes();
        int idxA = custom.indexOf(scopeA);
        int idxB = custom.indexOf(scopeB);
        assertTrue(idxA < idxB);

        // Move B up
        notified.set(false);
        scopeManager.moveUp(idxB);
        assertTrue(notified.get());

        custom = scopeManager.getCustomScopes();
        assertTrue(custom.indexOf(scopeB) < custom.indexOf(scopeA));

        // Clean up
        scopeManager.removeCustomScope(scopeA);
        scopeManager.removeCustomScope(scopeB);
        scopeManager.removeListener(listener);
    }
}
