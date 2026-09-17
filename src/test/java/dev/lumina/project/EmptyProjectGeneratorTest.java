package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmptyProjectGeneratorTest {

    @Test
    void testGenerateEmptyProjectWithGit(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.EMPTY_PROJECT,
                "my-empty-project",
                tempDir,
                true, // initGit
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "", "", "", "", "", "", "", "", "", "", "", "", "", ""
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(projectDir);
        assertTrue(Files.isDirectory(projectDir));

        // Verify .idea metadata
        Path ideaDir = projectDir.resolve(".idea");
        assertTrue(Files.isDirectory(ideaDir));
        assertTrue(Files.exists(ideaDir.resolve("modules.xml")));
        assertTrue(Files.exists(ideaDir.resolve("misc.xml")));
        assertTrue(Files.exists(ideaDir.resolve(".gitignore")));

        // Verify modules.xml contains empty module list
        String modulesXml = Files.readString(ideaDir.resolve("modules.xml"));
        assertTrue(modulesXml.contains("<modules />") || modulesXml.contains("<modules>"));

        // Verify git configuration
        assertTrue(Files.exists(ideaDir.resolve("vcs.xml")));
        String vcsXml = Files.readString(ideaDir.resolve("vcs.xml"));
        assertTrue(vcsXml.contains("vcs=\"Git\""));

        assertTrue(Files.exists(projectDir.resolve(".gitignore")));
    }

    @Test
    void testGenerateEmptyProjectWithoutGit(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.EMPTY_PROJECT,
                "plain-empty-proj",
                tempDir,
                false, // initGit = false
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "", "", "", "", "", "", "", "", "", "", "", "", "", ""
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(projectDir);
        assertTrue(Files.isDirectory(projectDir));

        // Verify .idea metadata exists
        Path ideaDir = projectDir.resolve(".idea");
        assertTrue(Files.isDirectory(ideaDir));
        assertTrue(Files.exists(ideaDir.resolve("modules.xml")));
        assertTrue(Files.exists(ideaDir.resolve("misc.xml")));
        assertTrue(Files.exists(ideaDir.resolve(".gitignore")));

        // Verify vcs.xml was NOT created
        assertFalse(Files.exists(ideaDir.resolve("vcs.xml")));
    }
}
