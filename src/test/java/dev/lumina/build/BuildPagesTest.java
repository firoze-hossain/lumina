package dev.lumina.build;

import dev.lumina.ui.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI and lifecycle integration tests for Settings pages under Build, Execution, Deployment.
 */
public class BuildPagesTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (IllegalStateException e) {
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void resetSettings() {
        MavenSettingsManager.getInstance().setSettings(new MavenSettings());
        GradleSettingsManager.getInstance().setSettings(new GradleSettings());
        GantSettingsManager.getInstance().setSettings(new GantSettings());
        BspSettingsManager.getInstance().setSettings(new BspSettings());
        CargoSettingsManager.getInstance().setSettings(new CargoSettings());
        SbtSettingsManager.getInstance().setSettings(new SbtSettings());
        CompilerSettingsManager.getInstance().setSettings(new CompilerSettings());
        AnnotationProcessingSettingsManager.getInstance().setSettings(new AnnotationProcessingSettings());
        CompilerExcludesSettingsManager.getInstance().setSettings(new CompilerExcludesSettings());
        GroovyCompilerSettingsManager.getInstance().setSettings(new GroovyCompilerSettings());
    }

    private void runOnFx(Runnable action) {
        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for JavaFX thread");
            if (error.get() != null) {
                if (error.get() instanceof AssertionError ae) throw ae;
                fail(error.get());
            }
        } catch (InterruptedException e) {
            fail(e);
        }
    }

    @Test
    void testPythonDebuggerPageLifecycle() {
        runOnFx(() -> {
            SettingsPythonDebuggerPage page = new SettingsPythonDebuggerPage();
            assertFalse(page.isModified(), "New page should not be modified");

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modifiedNotified.set(true));

            PythonDebuggerSettings initial = page.getCurrentSettings();
            assertTrue(initial.isAttachToSubprocess());
            assertEquals("python", initial.getAttachProcessFilter());

            initial.setAttachToSubprocess(false);
            initial.setAttachProcessFilter("custom-py");

            page.loadData();
            assertFalse(page.isModified());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testApplicationServersPageLifecycle() {
        runOnFx(() -> {
            SettingsApplicationServersPage page = new SettingsApplicationServersPage();
            assertFalse(page.isModified(), "New page should not be modified");

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testBuildToolsPageLifecycle() {
        runOnFx(() -> {
            SettingsBuildToolsPage page = new SettingsBuildToolsPage();
            assertFalse(page.isModified(), "New page should not be modified");

            BuildToolsSettings s = page.getCurrentSettings();
            assertTrue(s.isSyncOnBuildScriptChanges());
            assertEquals(BuildToolsSettings.SyncTrigger.EXTERNAL_CHANGES, s.getSyncTrigger());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testMavenPageLifecycle() {
        runOnFx(() -> {
            SettingsMavenPage page = new SettingsMavenPage();
            assertFalse(page.isModified(), "New page should not be modified");

            MavenSettings s = page.getCurrentSettings();
            assertFalse(s.isWorkOffline());
            assertTrue(s.isExecuteGoalsRecursively());
            assertEquals("Info", s.getOutputLevel());
            assertEquals("Bundled (Maven 3)", s.getMavenHome());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testArchetypeCatalogsPageLifecycle() {
        runOnFx(() -> {
            SettingsMavenArchetypeCatalogsPage page = new SettingsMavenArchetypeCatalogsPage();
            assertFalse(page.isModified(), "New page should not be modified");

            assertTrue(page.getCatalogsList().stream().anyMatch(c -> "Internal".equals(c.name()) && c.isSystem()));
            assertTrue(page.getCatalogsList().stream().anyMatch(c -> "Default Local".equals(c.name()) && c.isSystem()));
            assertTrue(page.getCatalogsList().stream().anyMatch(c -> "Maven Central".equals(c.name()) && c.isSystem()));

            var custom = new dev.lumina.project.MavenArchetypeMetadata.CatalogEntry("My Team Archetypes", "Custom", "https://repo.corp/archetypes", false);
            page.getCatalogsList().add(custom);
            assertTrue(page.isModified(), "Page should be modified after adding custom catalog");

            page.apply();
            assertFalse(page.isModified(), "Page should not be modified after apply");
            assertEquals(1, page.getCurrentCustomCatalogs().size());

            page.getCatalogsList().remove(custom);
            assertTrue(page.isModified());
            page.apply();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testIgnoredFilesPageLifecycle() {
        runOnFx(() -> {
            SettingsMavenIgnoredFilesPage page = new SettingsMavenIgnoredFilesPage();
            assertFalse(page.isModified(), "New page should not be modified");

            page.getPathPatternsField().setText("**/test-pom.xml, custom*.xml");
            assertTrue(page.isModified(), "Modified after changing patterns");

            page.reset();
            assertFalse(page.isModified(), "Not modified after reset");

            assertNotNull(page.getPomCheckBoxes());
        });
    }

    @Test
    void testImportingPageLifecycle() {
        runOnFx(() -> {
            SettingsMavenImportingPage page = new SettingsMavenImportingPage();
            assertFalse(page.isModified(), "New page should not be modified");

            MavenSettings s = page.getCurrentSettings();
            assertTrue(s.isDetectCompilerAutomatically());
            assertTrue(s.isExcludeTargetDirectory());
            assertTrue(s.isUseMavenOutputDirectories());
            assertEquals("Detect automatically", s.getGeneratedSourcesMode());
            assertEquals("process-resources", s.getFoldersUpdatePhase());
            assertFalse(s.isDownloadSources());
            assertFalse(s.isDownloadDocumentation());
            assertFalse(s.isDownloadAnnotations());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testRepositoriesPageLifecycle() {
        runOnFx(() -> {
            SettingsMavenRepositoriesPage page = new SettingsMavenRepositoriesPage();
            assertFalse(page.isModified(), "New page should not be modified");

            assertTrue(page.getReposList().stream().anyMatch(r -> "Local".equals(r.type())));
            assertTrue(page.getReposList().stream().anyMatch(r -> "Remote".equals(r.type()) && r.url().contains("maven.apache.org")));

            page.getReposTable().getSelectionModel().select(0);
            page.getUpdateBtn().fire();
            assertTrue(page.isModified(), "Should be modified after updating repository timestamp");

            page.apply();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testRunnerPageLifecycle() {
        runOnFx(() -> {
            SettingsMavenRunnerPage page = new SettingsMavenRunnerPage();
            assertFalse(page.isModified(), "New page should not be modified");

            MavenSettings s = page.getCurrentSettings();
            assertFalse(s.isDelegateBuildRunToMaven());
            assertFalse(s.isSkipTests());

            page.getPropertiesList().add(new SettingsMavenRunnerPage.PropertyItem("maven.compiler.verbose", "true"));
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());

            page.getPropertiesList().clear();
            page.apply();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testRunningTestsPageLifecycle() {
        runOnFx(() -> {
            SettingsMavenRunningTestsPage page = new SettingsMavenRunningTestsPage();
            assertFalse(page.isModified(), "New page should not be modified");

            MavenSettings s = page.getCurrentSettings();
            assertTrue(s.isPassArgLine());
            assertTrue(s.isPassSystemPropertyVariables());
            assertTrue(s.isPassEnvironmentVariables());

            page.getPassArgLineCheck().setSelected(false);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertFalse(MavenSettingsManager.getInstance().getSettings().isPassArgLine());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testGradlePageLifecycle() {
        runOnFx(() -> {
            SettingsGradlePage page = new SettingsGradlePage();
            assertFalse(page.isModified(), "New Gradle page should not be modified");

            GradleSettings initial = page.getCurrentSettings();
            assertFalse(initial.isGenerateImlFiles());
            assertFalse(initial.isParallelModelFetching());
            assertTrue(GradleSettings.getDefaultGradleUserHome().contains(".gradle"));

            page.getGenerateImlFilesCheck().setSelected(true);
            page.getParallelModelFetchingCheck().setSelected(true);
            page.getGradleUserHomeField().setText("/custom/gradle/home");
            assertTrue(page.isModified(), "Page must report modified after editing values");

            page.apply();
            assertFalse(page.isModified(), "Page must not report modified after apply");
            GradleSettings applied = GradleSettingsManager.getInstance().getSettings();
            assertTrue(applied.isGenerateImlFiles());
            assertTrue(applied.isParallelModelFetching());
            assertEquals("/custom/gradle/home", applied.getGradleUserHome());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testGantPageLifecycle() {
        runOnFx(() -> {
            SettingsGantPage page = new SettingsGantPage();
            assertFalse(page.isModified(), "New Gant page should not be modified");

            page.getGantHomeField().setText("/opt/gant/custom");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("/opt/gant/custom", GantSettingsManager.getInstance().getSettings().getGantHome());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testBspPageLifecycle() {
        runOnFx(() -> {
            SettingsBspPage page = new SettingsBspPage();
            assertFalse(page.isModified(), "New BSP page should not be modified");

            BspSettings initial = page.getCurrentSettings();
            assertFalse(initial.isBspTraceLogEnabled());

            page.getEnableTraceLogCheck().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertTrue(BspSettingsManager.getInstance().getSettings().isBspTraceLogEnabled());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testCargoPageLifecycle() {
        runOnFx(() -> {
            SettingsCargoPage page = new SettingsCargoPage();
            assertFalse(page.isModified(), "New Cargo page should not be modified");

            CargoSettings initial = page.getCurrentSettings();
            assertTrue(initial.isAutoShowFirstError());
            assertFalse(initial.isOfflineMode());

            page.getOfflineModeCheck().setSelected(true);
            page.getAutoShowFirstErrorCheck().setSelected(false);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            CargoSettings applied = CargoSettingsManager.getInstance().getSettings();
            assertFalse(applied.isAutoShowFirstError());
            assertTrue(applied.isOfflineMode());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testSbtPageLifecycle() {
        runOnFx(() -> {
            SettingsSbtPage page = new SettingsSbtPage();
            assertFalse(page.isModified(), "New sbt page should not be modified");

            SbtSettings initial = page.getCurrentSettings();
            assertNotNull(initial.getJre());
            assertEquals("Bundled", initial.getLauncher());

            page.getMaximumHeapSizeField().setText("2048");
            page.getVmParametersField().setText("-Xms512m -Xmx2048m");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            SbtSettings applied = SbtSettingsManager.getInstance().getSettings();
            assertEquals("2048", applied.getMaximumHeapSizeMb());
            assertEquals("-Xms512m -Xmx2048m", applied.getVmParameters());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testCompilerPageLifecycle() {
        runOnFx(() -> {
            SettingsCompilerPage page = new SettingsCompilerPage();
            assertFalse(page.isModified(), "New compiler page should not be modified");

            CompilerSettings initial = page.getCurrentSettings();
            assertEquals(CompilerSettings.DEFAULT_RESOURCE_PATTERNS, initial.getResourcePatterns());
            assertTrue(initial.isClearOutputDirectoryOnRebuild());
            assertTrue(initial.isAddRuntimeAssertionsNotNull());
            assertTrue(initial.isAutoShowFirstErrorInEditor());
            assertTrue(initial.isDisplayNotificationOnBuildCompletion());
            assertFalse(initial.isBuildProjectAutomatically());
            assertTrue(initial.isRebuildModuleOnDependencyChange());
            assertEquals("Automatic", initial.getCompileModulesInParallel());
            assertEquals("700", initial.getSharedHeapSizeMb());

            page.getSharedHeapSizeField().setText("1024");
            page.getBuildProjectAutomaticallyCheck().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            CompilerSettings applied = CompilerSettingsManager.getInstance().getSettings();
            assertEquals("1024", applied.getSharedHeapSizeMb());
            assertTrue(applied.isBuildProjectAutomatically());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testAnnotationProcessorsPageLifecycle() {
        runOnFx(() -> {
            SettingsAnnotationProcessorsPage page = new SettingsAnnotationProcessorsPage();
            assertFalse(page.isModified(), "New annotation processors page should not be modified");

            AnnotationProcessingSettings initial = page.getCurrentSettings();
            assertEquals(2, initial.getProfiles().size());
            assertNotNull(initial.getProfileByName("Default"));
            assertNotNull(initial.getProfileByName("Maven default annotation processors profile"));

            page.getProductionSourcesField().setText("build/generated/sources/annotationProcessor/java/main");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            AnnotationProcessingSettings applied = AnnotationProcessingSettingsManager.getInstance().getSettings();
            AnnotationProcessingProfile def = applied.getProfileByName("Default");
            assertNotNull(def);
            assertEquals("build/generated/sources/annotationProcessor/java/main", def.getProductionSourcesDirectory());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testCompilerExcludesPageLifecycle() {
        runOnFx(() -> {
            SettingsCompilerExcludesPage page = new SettingsCompilerExcludesPage();
            assertFalse(page.isModified(), "New compiler excludes page should not be modified");

            page.addExcludeEntry("/path/to/excluded/source", true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            CompilerExcludesSettings applied = CompilerExcludesSettingsManager.getInstance().getSettings();
            assertEquals(1, applied.getEntries().size());
            assertEquals("/path/to/excluded/source", applied.getEntries().getFirst().getPath());
            assertTrue(applied.getEntries().getFirst().isRecursive());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testGroovyCompilerPageLifecycle() {
        runOnFx(() -> {
            SettingsGroovyCompilerPage page = new SettingsGroovyCompilerPage();
            assertFalse(page.isModified(), "New Groovy compiler page should not be modified");

            GroovyCompilerSettings initial = page.getCurrentSettings();
            assertEquals("", initial.getConfigScriptPath());
            assertFalse(initial.isInvokeDynamicSupport());

            page.getConfigScriptField().setText("/path/to/groovy-config.groovy");
            page.getInvokeDynamicSupportCheck().setSelected(true);
            page.addExcludeEntry("/path/to/stub/exclude", true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            GroovyCompilerSettings applied = GroovyCompilerSettingsManager.getInstance().getSettings();
            assertEquals("/path/to/groovy-config.groovy", applied.getConfigScriptPath());
            assertTrue(applied.isInvokeDynamicSupport());
            assertEquals(1, applied.getStubGenerationExcludes().size());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testSettingsDialogTreeAndRouting() {
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);

            // Verify Build, Execution, Deployment tree node hierarchy
            TreeItem<String> buildRoot = dialog.findItem(dialog.getTreeRoot(), "Build, Execution, Deployment");
            assertNotNull(buildRoot, "Build, Execution, Deployment root item should exist");

            assertNotNull(dialog.findItem(buildRoot, "Python Debugger"));
            assertNotNull(dialog.findItem(buildRoot, "Application Servers"));

            TreeItem<String> buildTools = dialog.findItem(buildRoot, "Build Tools");
            assertNotNull(buildTools, "Build Tools item should exist");

            TreeItem<String> maven = dialog.findItem(buildTools, "Maven");
            assertNotNull(maven, "Maven item should exist under Build Tools");

            assertNotNull(dialog.findItem(maven, "Archetype Catalogs"));
            assertNotNull(dialog.findItem(maven, "Ignored Files"));
            assertNotNull(dialog.findItem(maven, "Importing"));
            assertNotNull(dialog.findItem(maven, "Repositories"));
            assertNotNull(dialog.findItem(maven, "Runner"));
            assertNotNull(dialog.findItem(maven, "Running Tests"));

            assertNotNull(dialog.findItem(buildTools, "Gradle"));
            assertNotNull(dialog.findItem(buildTools, "Gant"));
            assertNotNull(dialog.findItem(buildTools, "BSP"));
            assertNotNull(dialog.findItem(buildTools, "Cargo"));
            assertNotNull(dialog.findItem(buildTools, "sbt"));

            TreeItem<String> compiler = dialog.findItem(buildRoot, "Compiler");
            assertNotNull(compiler, "Compiler item should exist under Build, Execution, Deployment");

            assertNotNull(dialog.findItem(compiler, "Annotation Processors"));
            assertNotNull(dialog.findItem(compiler, "Excludes"));
            assertNotNull(dialog.findItem(compiler, "Groovy Compiler"));
            assertNotNull(dialog.findItem(compiler, "Java Compiler"));
            assertNotNull(dialog.findItem(compiler, "Kotlin Compiler"));
            assertNotNull(dialog.findItem(compiler, "RMI Compiler"));
            assertNotNull(dialog.findItem(compiler, "Scala Compiler"));
            assertNotNull(dialog.findItem(compiler, "Validation"));

            assertNotNull(dialog.findItem(buildRoot, "Console"));
            assertNotNull(dialog.findItem(buildRoot, "Coverage"));
            assertNotNull(dialog.findItem(buildRoot, "Debugger"));
            assertNotNull(dialog.findItem(buildRoot, "Deployment"));
            assertNotNull(dialog.findItem(buildRoot, "Docker"));
            assertNotNull(dialog.findItem(buildRoot, "Kubernetes"));
            assertNotNull(dialog.findItem(buildRoot, "Profilers"));
            assertNotNull(dialog.findItem(buildRoot, "Remote Jar Repositories"));
            assertNotNull(dialog.findItem(buildRoot, "Run Targets"));

            // Select and verify routing
            dialog.selectCategory("Build, Execution, Deployment");
            dialog.selectCategory("Python Debugger");
            assertNotNull(dialog.getCurrentPythonDebuggerPage());

            dialog.selectCategory("Application Servers");
            assertNotNull(dialog.getCurrentApplicationServersPage());

            dialog.selectCategory("Build Tools");
            assertNotNull(dialog.getCurrentBuildToolsPage());

            dialog.selectCategory("Maven");
            assertNotNull(dialog.getCurrentMavenPage());

            dialog.selectCategory("Archetype Catalogs");
            assertNotNull(dialog.getCurrentMavenArchetypeCatalogsPage());

            dialog.selectCategory("Ignored Files");
            assertNotNull(dialog.getCurrentMavenIgnoredFilesPage());

            dialog.selectCategory("Importing");
            assertNotNull(dialog.getCurrentMavenImportingPage());

            dialog.selectCategory("Repositories");
            assertNotNull(dialog.getCurrentMavenRepositoriesPage());

            dialog.selectCategory("Runner");
            assertNotNull(dialog.getCurrentMavenRunnerPage());

            dialog.selectCategory("Running Tests");
            assertNotNull(dialog.getCurrentMavenRunningTestsPage());

            dialog.selectCategory("Gradle");
            assertNotNull(dialog.getCurrentGradlePage());

            dialog.selectCategory("Gant");
            assertNotNull(dialog.getCurrentGantPage());

            dialog.selectCategory("BSP");
            assertNotNull(dialog.getCurrentBspPage());

            dialog.selectCategory("Cargo");
            assertNotNull(dialog.getCurrentCargoPage());

            dialog.selectCategory("sbt");
            assertNotNull(dialog.getCurrentSbtPage());

            dialog.selectCategory("Compiler");
            assertNotNull(dialog.getCurrentCompilerPage());

            dialog.selectCategory("Annotation Processors");
            assertNotNull(dialog.getCurrentAnnotationProcessorsPage());

            dialog.selectCategory("Excludes");
            assertNotNull(dialog.getCurrentCompilerExcludesPage());

            dialog.selectCategory("Groovy Compiler");
            assertNotNull(dialog.getCurrentGroovyCompilerPage());
        });
    }

    @Test
    void testBrandIsolation() {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/ui/SettingsMavenArchetypeCatalogsPage.java",
                "src/main/java/dev/lumina/ui/SettingsMavenIgnoredFilesPage.java",
                "src/main/java/dev/lumina/ui/SettingsMavenImportingPage.java",
                "src/main/java/dev/lumina/ui/SettingsMavenRepositoriesPage.java",
                "src/main/java/dev/lumina/ui/SettingsMavenRunnerPage.java",
                "src/main/java/dev/lumina/ui/SettingsMavenRunningTestsPage.java",
                "src/main/java/dev/lumina/build/GradleSettings.java",
                "src/main/java/dev/lumina/build/GradleSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsGradlePage.java",
                "src/main/java/dev/lumina/build/GantSettings.java",
                "src/main/java/dev/lumina/build/GantSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsGantPage.java",
                "src/main/java/dev/lumina/build/BspSettings.java",
                "src/main/java/dev/lumina/build/BspSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsBspPage.java",
                "src/main/java/dev/lumina/build/CargoSettings.java",
                "src/main/java/dev/lumina/build/CargoSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsCargoPage.java",
                "src/main/java/dev/lumina/build/SbtSettings.java",
                "src/main/java/dev/lumina/build/SbtSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsSbtPage.java",
                "src/main/java/dev/lumina/build/CompilerSettings.java",
                "src/main/java/dev/lumina/build/CompilerSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsCompilerPage.java",
                "src/main/java/dev/lumina/build/AnnotationProcessingProfile.java",
                "src/main/java/dev/lumina/build/AnnotationProcessingSettings.java",
                "src/main/java/dev/lumina/build/AnnotationProcessingSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsAnnotationProcessorsPage.java",
                "src/main/java/dev/lumina/build/CompilerExcludeEntry.java",
                "src/main/java/dev/lumina/build/CompilerExcludesSettings.java",
                "src/main/java/dev/lumina/build/CompilerExcludesSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsCompilerExcludesPage.java",
                "src/main/java/dev/lumina/build/GroovyCompilerSettings.java",
                "src/main/java/dev/lumina/build/GroovyCompilerSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsGroovyCompilerPage.java"
        );
        for (String f : filesToCheck) {
            File file = new File(f);
            assertTrue(file.exists(), f + " must exist");
            try {
                String content = java.nio.file.Files.readString(file.toPath());
                assertFalse(content.contains("IntelliJ"), "File " + f + " must maintain strict brand isolation (found IntelliJ)");
                assertFalse(content.contains("JetBrains"), "File " + f + " must maintain strict brand isolation (found JetBrains)");
            } catch (Exception e) {
                fail(e);
            }
        }
    }
}
