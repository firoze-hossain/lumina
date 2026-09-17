package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ScalaSbtGeneratorTest {

    @Test
    void testSbtProjectGenerationWithOptionalBraces(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.SCALA,
                "scala-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.SBT,
                ProjectSpec.Language.SCALA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "scala-app",
                "org.example.application",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true, // addSampleCode
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
                "org.example.application",
                "scala-app"
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("scala-app"), projectDir);

        // 1. project/build.properties
        Path buildPropsPath = projectDir.resolve("project/build.properties");
        assertTrue(Files.isRegularFile(buildPropsPath), "project/build.properties must exist");
        String buildProps = Files.readString(buildPropsPath);
        assertTrue(buildProps.contains("sbt.version=2.0.9"));

        // 2. project/plugins.sbt
        Path pluginsPath = projectDir.resolve("project/plugins.sbt");
        assertTrue(Files.isRegularFile(pluginsPath), "project/plugins.sbt must exist");
        String plugins = Files.readString(pluginsPath);
        assertTrue(plugins.contains("sbt-ide-settings"));

        // 3. build.sbt
        Path buildSbtPath = projectDir.resolve("build.sbt");
        assertTrue(Files.isRegularFile(buildSbtPath), "build.sbt must exist");
        String buildSbt = Files.readString(buildSbtPath);
        assertTrue(buildSbt.contains("ThisBuild / scalaVersion := \"3.9.0\""));
        assertTrue(buildSbt.contains("name := \"scala-app\""));
        assertTrue(buildSbt.contains("idePackagePrefix := Some(\"org.example.application\")"));

        // 4. Source structure & Main.scala
        Path mainScala = projectDir.resolve("src/main/scala/org/example/application/Main.scala");
        assertTrue(Files.isRegularFile(mainScala), "Main.scala must exist");
        String mainContent = Files.readString(mainScala);
        assertTrue(mainContent.contains("package org.example.application"));
        assertTrue(mainContent.contains("@main def main(): Unit ="));
        assertTrue(mainContent.contains("for i <- 1 to 5 do"));

        // 5. .idea/sbt.xml
        Path sbtXml = projectDir.resolve(".idea/sbt.xml");
        assertTrue(Files.isRegularFile(sbtXml));
        String sbtXmlContent = Files.readString(sbtXml);
        assertTrue(sbtXmlContent.contains("value=\"2.0.9\""));
    }

    @Test
    void testSbtProjectGenerationWithoutOptionalBraces(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.SCALA,
                "scala-braces",
                tempDir,
                false,
                ProjectSpec.BuildSystem.SBT,
                ProjectSpec.Language.SCALA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.test",
                "scala-braces",
                "",
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true, // addSampleCode
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
                "2.0.8",
                "3.8.4",
                false,
                true,
                false, // optionalBraces = false (with braces)
                "",
                "scala-braces"
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        Path mainScala = projectDir.resolve("src/main/scala/Main.scala");
        assertTrue(Files.isRegularFile(mainScala));
        String mainContent = Files.readString(mainScala);
        assertTrue(mainContent.contains("@main def main(): Unit = {"));
        assertTrue(mainContent.contains("for (i <- 1 to 5) {"));
    }

    @Test
    void testScalaCliProjectGeneration(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.SCALA,
                "scala-cli-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.SCALA_CLI,
                ProjectSpec.Language.SCALA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "scala-cli-app",
                "",
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
                true,
                "",
                "scala-cli-app"
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        Path mainFile = projectDir.resolve("src/main.scala");
        assertTrue(Files.isRegularFile(mainFile), "src/main.scala must exist");
        String content = Files.readString(mainFile);
        assertTrue(content.contains("//> using scala 3.9.0"));
        assertTrue(content.contains("//> using jvm 25"));
        assertTrue(content.contains("@main def main(): Unit ="));
    }
}
