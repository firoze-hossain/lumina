package dev.lumina.project;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MavenProjectModelTest {

    @Test
    void testParseCurrentProjectPom() {
        Path root = Path.of(".").toAbsolutePath().normalize();
        MavenProjectModel.MavenProject project = MavenProjectModel.parseProject(root);

        assertNotNull(project, "Project should be parsed from pom.xml");
        assertEquals("Lumina IDE", project.getName());
        assertEquals("dev.lumina", project.getGroupId());
        assertEquals("lumina-ide", project.getArtifactId());
        assertEquals("0.1.0", project.getVersion());

        // Lifecycle phases
        List<String> phases = project.getLifecyclePhases();
        assertEquals(9, phases.size());
        assertEquals("clean", phases.get(0));
        assertEquals("compile", phases.get(2));
        assertEquals("test", phases.get(3));
        assertEquals("package", phases.get(4));
        assertEquals("install", phases.get(6));
        assertEquals("deploy", phases.get(8));

        // Dependencies with property resolution
        List<MavenProjectModel.DependencyItem> deps = project.getDependencies();
        assertFalse(deps.isEmpty());

        boolean foundJavaFxControls = false;
        boolean foundJunitTestScope = false;

        for (MavenProjectModel.DependencyItem dep : deps) {
            if ("javafx-controls".equals(dep.artifactId())) {
                foundJavaFxControls = true;
                assertEquals("23.0.2", dep.version(), "Property ${javafx.version} must resolve to 23.0.2");
                assertEquals("org.openjfx:javafx-controls:23.0.2", dep.displayString());
            }
            if ("junit-jupiter".equals(dep.artifactId())) {
                foundJunitTestScope = true;
                assertEquals("test", dep.scope());
                assertEquals("org.junit.jupiter:junit-jupiter:5.10.2 (test)", dep.displayString());
            }
        }
        assertTrue(foundJavaFxControls, "Should find resolved javafx-controls");
        assertTrue(foundJunitTestScope, "Should find junit-jupiter with (test) scope");

        // Plugins: core defaults + pom overrides
        List<MavenProjectModel.PluginItem> plugins = project.getPlugins();
        assertFalse(plugins.isEmpty());

        // Verify alphabetical sorting by prefix matching Image 2
        for (int i = 0; i < plugins.size() - 1; i++) {
            assertTrue(plugins.get(i).prefix().compareTo(plugins.get(i + 1).prefix()) <= 0,
                    "Plugins must be sorted alphabetically by prefix: " + plugins.get(i).prefix() + " vs " + plugins.get(i + 1).prefix());
        }

        boolean foundJavaFxPlugin = false;
        boolean foundCompilerPlugin = false;
        boolean foundSurefirePlugin = false;

        for (MavenProjectModel.PluginItem plugin : plugins) {
            if ("javafx".equals(plugin.prefix())) {
                foundJavaFxPlugin = true;
                assertEquals("org.openjfx", plugin.groupId());
                assertEquals("javafx-maven-plugin", plugin.artifactId());
                assertEquals("0.0.8", plugin.version());
                assertTrue(plugin.goals().contains("javafx:run"));
            }
            if ("compiler".equals(plugin.prefix())) {
                foundCompilerPlugin = true;
                assertEquals("3.13.0", plugin.version(), "Compiler plugin should have version 3.13.0 from pom.xml");
            }
            if ("surefire".equals(plugin.prefix())) {
                foundSurefirePlugin = true;
                assertEquals("3.2.5", plugin.version(), "Surefire plugin should have version 3.2.5 from pom.xml");
            }
        }
        assertTrue(foundJavaFxPlugin, "Should find custom javafx plugin from pom.xml");
        assertTrue(foundCompilerPlugin, "Should find compiler plugin");
        assertTrue(foundSurefirePlugin, "Should find surefire plugin");

        // Repositories
        List<MavenProjectModel.RepositoryItem> repos = project.getRepositories();
        assertEquals(2, repos.size());
        assertEquals("local (~/.m2/repository)", repos.get(0).displayString());
        assertEquals("central (https://repo.maven.apache.org/maven2)", repos.get(1).displayString());
    }

    @Test
    void testParseNullOrNonExistentProject() {
        assertNull(MavenProjectModel.parseProject(null));
        assertNull(MavenProjectModel.parseProject(Path.of("/non/existent/path/at/all")));
    }
}
