package dev.lumina.project;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RecentProjectsManagerTest {

    private Path tempDir;
    private Path storageFile;
    private RecentProjectsManager manager;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory(Path.of("target"), "recent-proj-test");
        storageFile = tempDir.resolve("recent-projects.json");
        manager = new RecentProjectsManager(storageFile, false);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {}
                        });
            }
        }
    }

    @Test
    void testRecordAndRetrieveRecentProjects() throws IOException {
        Path p1 = tempDir.resolve("project-alpha");
        Path p2 = tempDir.resolve("project-beta");
        Files.createDirectories(p1);
        Files.createDirectories(p2);

        manager.recordProjectOpened(p1);
        manager.recordProjectOpened(p2);

        List<RecentProjectsManager.RecentProject> recents = manager.getRecentProjects();
        assertEquals(2, recents.size());
        // p2 was opened most recently, so it should be first
        assertEquals(p2.toAbsolutePath().normalize().toString(), recents.get(0).path());
        assertEquals("project-beta", recents.get(0).name());
        assertEquals(p1.toAbsolutePath().normalize().toString(), recents.get(1).path());

        // Re-open p1; it should move to the top
        manager.recordProjectOpened(p1);
        recents = manager.getRecentProjects();
        assertEquals(2, recents.size());
        assertEquals(p1.toAbsolutePath().normalize().toString(), recents.get(0).path());
    }

    @Test
    void testPruneNonExistentDirectories() throws IOException {
        Path p1 = tempDir.resolve("valid-proj");
        Path p2 = tempDir.resolve("deleted-proj");
        Files.createDirectories(p1);
        Files.createDirectories(p2);

        manager.recordProjectOpened(p1);
        manager.recordProjectOpened(p2);
        assertEquals(2, manager.getRecentProjects().size());

        // Delete p2 from disk
        Files.delete(p2);

        List<RecentProjectsManager.RecentProject> recents = manager.getRecentProjects();
        assertEquals(1, recents.size());
        assertEquals(p1.toAbsolutePath().normalize().toString(), recents.get(0).path());
    }

    @Test
    void testCollisionDisambiguation() throws IOException {
        Path sub1 = tempDir.resolve("group1").resolve("my-service");
        Path sub2 = tempDir.resolve("group2").resolve("my-service");
        Path sub3 = tempDir.resolve("group3").resolve("unique-gateway");
        Files.createDirectories(sub1);
        Files.createDirectories(sub2);
        Files.createDirectories(sub3);

        manager.recordProjectOpened(sub1);
        manager.recordProjectOpened(sub2);
        manager.recordProjectOpened(sub3);

        List<RecentProjectsManager.RecentProject> all = manager.getRecentProjects();
        assertEquals(3, all.size());

        RecentProjectsManager.RecentProject unique = all.stream()
                .filter(p -> p.name().equals("unique-gateway"))
                .findFirst().orElseThrow();
        // Unique project displays simply as its base name
        assertEquals("unique-gateway", manager.getDisplayLabel(unique, all));

        // Duplicate base name project displays with home-relative path
        RecentProjectsManager.RecentProject dup1 = all.stream()
                .filter(p -> p.path().contains("group1"))
                .findFirst().orElseThrow();
        String label1 = manager.getDisplayLabel(dup1, all);
        assertTrue(label1.contains("group1/my-service"), "Label should contain relative path: " + label1);
        assertNotEquals("my-service", label1);
    }

    @Test
    void testGitBranchDetection() throws IOException {
        Path repo = tempDir.resolve("git-repo");
        Path gitDir = repo.resolve(".git");
        Files.createDirectories(gitDir);
        Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/feature/awesome-ui\n", StandardCharsets.UTF_8);

        String branch = RecentProjectsManager.getGitBranch(repo);
        assertEquals("feature/awesome-ui", branch);

        // Also test detached head / commit hash
        Files.writeString(gitDir.resolve("HEAD"), "a1b2c3d4e5f6\n", StandardCharsets.UTF_8);
        String detached = RecentProjectsManager.getGitBranch(repo);
        assertEquals("a1b2c3d", detached);
    }

    @Test
    void testRemoveAndClear() throws IOException {
        Path p1 = tempDir.resolve("proj-one");
        Path p2 = tempDir.resolve("proj-two");
        Files.createDirectories(p1);
        Files.createDirectories(p2);

        manager.recordProjectOpened(p1);
        manager.recordProjectOpened(p2);
        assertEquals(2, manager.getRecentProjects().size());

        manager.removeProject(p1.toString());
        List<RecentProjectsManager.RecentProject> recents = manager.getRecentProjects();
        assertEquals(1, recents.size());
        assertEquals(p2.toAbsolutePath().normalize().toString(), recents.get(0).path());

        manager.clear();
        assertTrue(manager.getRecentProjects().isEmpty());
    }

    @Test
    void testParseIntelliJRecentProjectsXml() throws IOException {
        Path projDir = tempDir.resolve("imported-idea-project");
        Files.createDirectories(projDir);

        String xmlContent = """
                <application>
                  <component name="RecentProjectsManager">
                    <option name="additionalInfo">
                      <map>
                        <entry key="%s">
                          <value>
                            <RecentProjectMetaInfo frameTitle="imported-idea-project – App.java">
                              <option name="activationTimestamp" value="1789800000000" />
                              <option name="projectOpenTimestamp" value="1789700000000" />
                            </RecentProjectMetaInfo>
                          </value>
                        </entry>
                      </map>
                    </option>
                  </component>
                </application>
                """.formatted(projDir.toAbsolutePath().normalize().toString());

        Path xmlFile = tempDir.resolve("recentProjects.xml");
        Files.writeString(xmlFile, xmlContent, StandardCharsets.UTF_8);

        Map<String, RecentProjectsManager.RecentProject> map = new HashMap<>();
        manager.parseRecentProjectsXml(xmlFile, map);

        assertEquals(1, map.size());
        RecentProjectsManager.RecentProject imported = map.get(projDir.toAbsolutePath().normalize().toString());
        assertNotNull(imported);
        assertEquals("imported-idea-project", imported.name());
        assertEquals(1789800000000L, imported.lastOpenedTimestamp());
    }
}
