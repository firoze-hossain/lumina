package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PlayGeneratorTest {

    @Test
    void testPlayScalaProjectWithOptionalBraces(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.PLAY,
                "play-scala-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.SBT,
                ProjectSpec.Language.SCALA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "play-scala-app",
                "com.example",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN,
                "Wrapper",
                "9.2.0",
                "",
                false,
                "5.1.1",
                "2.0.9",
                "3.9.0",
                false,
                true,
                true, // optionalBraces
                "",
                "play-scala-app",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV,
                "", "", "", "", false, "Virtualenv", "", false, false, false,
                "", false,
                "", false, "",
                "", "", "", false, false,
                "3.0.11" // playVersion
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("play-scala-app"), projectDir);

        // 1. project/build.properties
        Path buildPropsPath = projectDir.resolve("project/build.properties");
        assertTrue(Files.isRegularFile(buildPropsPath), "project/build.properties must exist");
        String buildProps = Files.readString(buildPropsPath);
        assertTrue(buildProps.contains("sbt.version=2.0.9"));

        // 2. project/plugins.sbt
        Path pluginsPath = projectDir.resolve("project/plugins.sbt");
        assertTrue(Files.isRegularFile(pluginsPath), "project/plugins.sbt must exist");
        String plugins = Files.readString(pluginsPath);
        assertTrue(plugins.contains("addSbtPlugin(\"org.playframework\" % \"sbt-plugin\" % \"3.0.11\")"));

        // 3. build.sbt
        Path buildSbtPath = projectDir.resolve("build.sbt");
        assertTrue(Files.isRegularFile(buildSbtPath), "build.sbt must exist");
        String buildSbt = Files.readString(buildSbtPath);
        assertTrue(buildSbt.contains("enablePlugins(PlayScala)"));
        assertTrue(buildSbt.contains("scalaVersion := \"3.9.0\""));
        assertTrue(buildSbt.contains("name := \"play-scala-app\""));
        assertTrue(buildSbt.contains("guice"));

        // 4. conf files
        Path confApp = projectDir.resolve("conf/application.conf");
        assertTrue(Files.isRegularFile(confApp));
        Path confRoutes = projectDir.resolve("conf/routes");
        assertTrue(Files.isRegularFile(confRoutes));
        String routes = Files.readString(confRoutes);
        assertTrue(routes.contains("controllers.HomeController.index()"));

        // 5. Controller (with optional braces)
        Path controllerPath = projectDir.resolve("app/controllers/HomeController.scala");
        assertTrue(Files.isRegularFile(controllerPath));
        String controller = Files.readString(controllerPath);
        assertTrue(controller.contains("class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController:"));
        assertTrue(controller.contains("def index() = Action:"));

        // 6. Views
        assertTrue(Files.isRegularFile(projectDir.resolve("app/views/index.scala.html")));
        assertTrue(Files.isRegularFile(projectDir.resolve("app/views/main.scala.html")));

        // 7. Assets & .idea
        assertTrue(Files.isRegularFile(projectDir.resolve("public/stylesheets/main.css")));
        assertTrue(Files.isRegularFile(projectDir.resolve("public/javascripts/main.js")));
        assertTrue(Files.isRegularFile(projectDir.resolve(".idea/sbt.xml")));
        String sbtXml = Files.readString(projectDir.resolve(".idea/sbt.xml"));
        assertTrue(sbtXml.contains("value=\"2.0.9\""));
    }

    @Test
    void testPlayScalaProjectWithoutOptionalBraces(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.PLAY,
                "play-braces",
                tempDir,
                false,
                ProjectSpec.BuildSystem.SBT,
                ProjectSpec.Language.SCALA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "play-braces",
                "com.example",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN,
                "Wrapper",
                "9.2.0",
                "",
                false,
                "5.1.1",
                "2.0.9",
                "3.9.0",
                false,
                true,
                false, // optionalBraces = false
                "",
                "play-braces",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV,
                "", "", "", "", false, "Virtualenv", "", false, false, false,
                "", false,
                "", false, "",
                "", "", "", false, false,
                "3.0.10"
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        Path controllerPath = projectDir.resolve("app/controllers/HomeController.scala");
        assertTrue(Files.isRegularFile(controllerPath));
        String controller = Files.readString(controllerPath);
        assertTrue(controller.contains("class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {"));
        assertTrue(controller.contains("def index() = Action {"));
    }

    @Test
    void testPlayJavaProject(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.PLAY,
                "play-java-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.SBT,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "play-java-app",
                "com.example",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN,
                "Wrapper",
                "9.2.0",
                "",
                false,
                "5.1.1",
                "2.0.9",
                "3.9.0",
                false,
                true,
                false,
                "",
                "play-java-app",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV,
                "", "", "", "", false, "Virtualenv", "", false, false, false,
                "", false,
                "", false, "",
                "", "", "", false, false,
                "3.0.11"
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        Path buildSbtPath = projectDir.resolve("build.sbt");
        assertTrue(Files.isRegularFile(buildSbtPath));
        String buildSbt = Files.readString(buildSbtPath);
        assertTrue(buildSbt.contains("enablePlugins(PlayJava)"));

        Path controllerPath = projectDir.resolve("app/controllers/HomeController.java");
        assertTrue(Files.isRegularFile(controllerPath));
        String controller = Files.readString(controllerPath);
        assertTrue(controller.contains("public class HomeController extends Controller"));
        assertTrue(controller.contains("public Result index()"));
    }

    @Test
    void testProjectSpecPlayDefaults() {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.PLAY, "test-play", Path.of("/tmp"), false,
                ProjectSpec.BuildSystem.SBT, ProjectSpec.Language.SCALA, ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES, "com.example", "test-play",
                "com.example", "25", "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "", "", "", "", "GRADLE", true,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN, "Wrapper", "9.2.0", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, true, false, "", "test-play",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "", "", "", "",
                false, "Virtualenv", "", false, false, false, "", false, "", false, "",
                "", "", "", false, false
        );

        assertEquals(PlayMetadata.DEFAULT_PLAY_VERSION, spec.safePlayVersion());
    }
}
