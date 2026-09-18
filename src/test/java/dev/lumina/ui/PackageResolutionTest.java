package dev.lumina.ui;

import dev.lumina.LuminaApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PackageResolutionTest {

    @Test
    void testFindSourceRootAndInferPackage(@TempDir Path tempDir) throws IOException {
        // Build typical Spring Boot Maven structure:
        // tempDir/my_spring_project/src/main/java/org/example/spring_boot_depency
        Path projectRoot = tempDir.resolve("my_spring_project");
        Path srcMainJava = projectRoot.resolve("src/main/java");
        Path packageDir = srcMainJava.resolve("org/example/spring_boot_depency");
        Files.createDirectories(packageDir);

        Path detectedSourceRoot = LuminaApp.findSourceRoot(packageDir);
        assertNotNull(detectedSourceRoot, "Source root should be detected");
        assertEquals(srcMainJava.toAbsolutePath().normalize(), detectedSourceRoot.toAbsolutePath().normalize());

        // Infer package for package directory
        String pkg = LuminaApp.inferPackageName(packageDir, projectRoot);
        assertEquals("org.example.spring_boot_depency", pkg);

        // Infer package for src/main/java itself
        String rootPkg = LuminaApp.inferPackageName(srcMainJava, projectRoot);
        assertEquals("", rootPkg, "Source root itself should infer empty package name");
    }

    @Test
    void testMultiModuleSourceRootAndInferPackage(@TempDir Path tempDir) throws IOException {
        // Multi-module Maven project:
        // parent/child_module/src/main/java/com/corp/service
        Path childModule = tempDir.resolve("parent").resolve("child_module");
        Path srcMainJava = childModule.resolve("src/main/java");
        Path subPkg = srcMainJava.resolve("com/corp/service");
        Files.createDirectories(subPkg);

        Path detectedSourceRoot = LuminaApp.findSourceRoot(subPkg);
        assertNotNull(detectedSourceRoot);
        assertEquals(srcMainJava.toAbsolutePath().normalize(), detectedSourceRoot.toAbsolutePath().normalize());

        String pkg = LuminaApp.inferPackageName(subPkg, tempDir.resolve("parent"));
        assertEquals("com.corp.service", pkg);
    }

    @Test
    void testTestSourceRootDetection(@TempDir Path tempDir) throws IOException {
        Path srcTestJava = tempDir.resolve("src/test/java");
        Path testPkg = srcTestJava.resolve("org/example/test");
        Files.createDirectories(testPkg);

        Path detected = LuminaApp.findSourceRoot(testPkg);
        assertNotNull(detected);
        assertEquals(srcTestJava.toAbsolutePath().normalize(), detected.toAbsolutePath().normalize());

        String pkg = LuminaApp.inferPackageName(testPkg, tempDir);
        assertEquals("org.example.test", pkg);
    }

    @Test
    void testPackageCreationResolution(@TempDir Path tempDir) throws IOException {
        Path srcMainJava = tempDir.resolve("src/main/java");
        Path basePkg = srcMainJava.resolve("org/example/spring_boot_depency");
        Files.createDirectories(basePkg);

        String currentPkg = LuminaApp.inferPackageName(basePkg, tempDir);
        assertEquals("org.example.spring_boot_depency", currentPkg);

        // Scenario 1: User typed "org.example.spring_boot_depency.entity" (Screenshot 3)
        String inputFull = "org.example.spring_boot_depency.entity";
        Path sourceRoot = LuminaApp.findSourceRoot(basePkg);
        assertNotNull(sourceRoot);
        Path target1 = sourceRoot.resolve(inputFull.replace('.', '/'));
        assertEquals("entity", target1.getFileName().toString());
        assertEquals(basePkg.resolve("entity"), target1);

        // Scenario 2: User typed relative "entity"
        String inputRelative = "entity";
        Path target2 = basePkg.resolve(inputRelative);
        assertEquals(target1, target2);

        // Create the directory and verify inferPackage on the new package
        Files.createDirectories(target1);
        assertTrue(Files.isDirectory(target1));
        String newPkg = LuminaApp.inferPackageName(target1, tempDir);
        assertEquals("org.example.spring_boot_depency.entity", newPkg);
    }
}
