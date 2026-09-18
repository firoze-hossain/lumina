package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AngularGeneratorTest {

    @Test
    void testGenerateAngularProjectStructure(@TempDir Path tempDir) throws IOException {
        String projectName = "demo-angular";
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.ANGULAR_CLI,
                projectName,
                tempDir,
                false,
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                projectName,
                "org.example." + projectName,
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "", "", "",
                "", "MAVEN", false,
                "", "", "", "",
                "", "", "", "", "", "MAVEN",
                "", "Netty", false, "Kotlin", "3.5.2", "YAML File", "",
                "", "v9.0.1",
                "", "", "", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.GROOVY, "Wrapper", "8.12", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", projectName,
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "", "", "", "",
                false, "Virtualenv", "", false, false, false,
                "", false,
                "", false, "",
                "/usr/bin/node",
                "22.1.8",
                "",
                true,
                true
        );

        List<String> logs = new ArrayList<>();
        Path projectDir = ProjectGenerator.generate(spec, logs::add);

        assertNotNull(projectDir);
        assertTrue(Files.isDirectory(projectDir));

        // 1. package.json
        Path packageJson = projectDir.resolve("package.json");
        assertTrue(Files.exists(packageJson));
        String pkgContent = Files.readString(packageJson);
        assertTrue(pkgContent.contains("\"name\": \"" + projectName + "\""));
        assertTrue(pkgContent.contains("@angular/core"));
        assertTrue(pkgContent.contains("@angular/cli"));
        assertTrue(pkgContent.contains("22.1.8"));

        // 2. angular.json
        Path angularJson = projectDir.resolve("angular.json");
        assertTrue(Files.exists(angularJson));
        String ngContent = Files.readString(angularJson);
        assertTrue(ngContent.contains(projectName));
        assertTrue(ngContent.contains("@angular-devkit/build-angular:application"));

        // 3. tsconfig files
        assertTrue(Files.exists(projectDir.resolve("tsconfig.json")));
        assertTrue(Files.exists(projectDir.resolve("tsconfig.app.json")));
        assertTrue(Files.exists(projectDir.resolve("tsconfig.spec.json")));

        // 4. src directory
        Path src = projectDir.resolve("src");
        assertTrue(Files.isDirectory(src));
        assertTrue(Files.exists(src.resolve("index.html")));
        assertTrue(Files.exists(src.resolve("styles.css")));
        assertTrue(Files.exists(src.resolve("main.ts")));

        String mainTs = Files.readString(src.resolve("main.ts"));
        assertTrue(mainTs.contains("bootstrapApplication"));

        // 5. src/app directory
        Path app = src.resolve("app");
        assertTrue(Files.isDirectory(app));
        assertTrue(Files.exists(app.resolve("app.config.ts")));
        assertTrue(Files.exists(app.resolve("app.routes.ts")));
        assertTrue(Files.exists(app.resolve("app.component.ts")));
        assertTrue(Files.exists(app.resolve("app.component.html")));
        assertTrue(Files.exists(app.resolve("app.component.css")));
        assertTrue(Files.exists(app.resolve("app.component.spec.ts")));

        String componentTs = Files.readString(app.resolve("app.component.ts"));
        assertTrue(componentTs.contains("standalone: true"));
        assertTrue(componentTs.contains("title = '" + projectName + "'"));

        // 6. IntelliJ .idea files
        Path ideaDir = projectDir.resolve(".idea");
        assertTrue(Files.isDirectory(ideaDir));
        assertTrue(Files.exists(ideaDir.resolve("modules.xml")));
        assertTrue(Files.exists(ideaDir.resolve(projectName + ".iml")));
        assertTrue(Files.exists(ideaDir.resolve("vcs.xml")));
    }

    @Test
    void testProjectSpecAngularDefaults() {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.ANGULAR_CLI,
                "test-angular",
                Path.of("/tmp"),
                false,
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "test-angular",
                "org.example.test",
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "", "", "",
                "", "MAVEN", false,
                "", "", "", "",
                "", "", "", "", "", "MAVEN",
                "", "Netty", false, "Kotlin", "3.5.2", "YAML File", "",
                "", "v9.0.1",
                "", "", "", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.GROOVY, "Wrapper", "8.12", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "test-angular",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "", "", "", "",
                false, "Virtualenv", "", false, false, false,
                "", false,
                "", false, ""
        );

        assertEquals("", spec.angularNodeInterpreter());
        assertEquals("", spec.angularCliVersion());
        assertEquals("", spec.angularAdditionalParameters());
        assertTrue(spec.angularStandalone());
        assertTrue(spec.angularDefaults());
    }
}
