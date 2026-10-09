package dev.lumina.ui;

import dev.lumina.kubernetes.KubernetesLanguageSettings;
import dev.lumina.kubernetes.KubernetesLanguageSettingsManager;
import dev.lumina.kubernetes.KubernetesResourceSpec;
import dev.lumina.lombok.LombokSettings;
import dev.lumina.lombok.LombokSettingsManager;
import dev.lumina.markdown.MarkdownLanguageSettings;
import dev.lumina.markdown.MarkdownLanguageSettingsManager;
import dev.lumina.micronaut.MicronautSettings;
import dev.lumina.micronaut.MicronautSettingsManager;
import dev.lumina.openapi.OpenAPISettings;
import dev.lumina.openapi.OpenAPISettingsManager;
import dev.lumina.openapi.RemoteOpenAPISpec;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLanguagesK8sLombokMarkdownMicronautOpenAPITest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            try {
                Platform.setImplicitExit(false);
                CountDownLatch checkLatch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    javaFxAvailable = true;
                    checkLatch.countDown();
                });
                checkLatch.await(1, TimeUnit.SECONDS);
            } catch (Throwable t) {
                javaFxAvailable = false;
            }
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    // ============================================================
    // 1. Kubernetes Tests
    // ============================================================

    @Test
    void testKubernetesModelAndManager() {
        KubernetesLanguageSettings settings = new KubernetesLanguageSettings();
        assertEquals("<Cluster not configured>", settings.getCurrentCluster());
        assertEquals("<Latest Version>", settings.getApiVersion());
        assertFalse(settings.isUseApiSchemaFromActiveCluster());
        assertEquals("5.x", settings.getKustomizeVersion());
        assertTrue(settings.getSpecifications().isEmpty());

        List<String> clusters = KubernetesLanguageSettings.detectAvailableClusters();
        assertNotNull(clusters);
        assertTrue(clusters.contains("<Cluster not configured>"));

        KubernetesLanguageSettings copy = settings.copy();
        assertEquals(settings, copy);

        copy.setCurrentCluster("minikube");
        copy.setApiVersion("1.31");
        copy.setUseApiSchemaFromActiveCluster(true);
        copy.setKustomizeVersion("4.x");
        copy.getSpecifications().add(new KubernetesResourceSpec("/etc/crd.yaml", "Valid", "Project"));

        assertNotEquals(settings, copy);

        KubernetesLanguageSettingsManager manager = KubernetesLanguageSettingsManager.getInstance();
        manager.setSettings(copy);
        assertEquals("minikube", manager.getSettings().getCurrentCluster());
        assertTrue(manager.checkConfiguration());

        manager.resetDefaults();
        assertEquals("<Cluster not configured>", manager.getSettings().getCurrentCluster());
    }

    @Test
    void testKubernetesPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesKubernetesPage page = new SettingsLanguagesKubernetesPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertEquals("<Cluster not configured>", page.getCurrentClusterCombo().getValue());
                assertEquals("<Latest Version>", page.getApiVersionCombo().getValue());
                assertFalse(page.getUseApiSchemaCheckBox().isSelected());
                assertEquals("5.x", page.getKustomizeVersionCombo().getValue());

                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getApiVersionCombo().setValue("1.30");
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.getUseApiSchemaCheckBox().setSelected(true);
                page.getTableItems().add(new KubernetesResourceSpec("/path/spec.yaml", "Valid", "Project"));
                assertEquals(1, page.getTableItems().size());

                page.apply();
                assertFalse(page.isModified());
                assertEquals("1.30", KubernetesLanguageSettingsManager.getInstance().getSettings().getApiVersion());

                page.reset();
                assertFalse(page.isModified());

                KubernetesLanguageSettingsManager.getInstance().resetDefaults();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ============================================================
    // 2. Lombok Tests
    // ============================================================

    @Test
    void testLombokModelAndManager() {
        LombokSettings settings = new LombokSettings();
        assertTrue(settings.isAutoAddTrackApDependencies());

        LombokSettings copy = settings.copy();
        assertEquals(settings, copy);

        copy.setAutoAddTrackApDependencies(false);
        assertNotEquals(settings, copy);

        LombokSettingsManager manager = LombokSettingsManager.getInstance();
        manager.setSettings(copy);
        assertFalse(manager.getSettings().isAutoAddTrackApDependencies());

        manager.resetDefaults();
        assertTrue(manager.getSettings().isAutoAddTrackApDependencies());
    }

    @Test
    void testLombokPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesLombokPage page = new SettingsLanguagesLombokPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertTrue(page.getAutoAddOptionCheckBox().isSelected());

                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getAutoAddOptionCheckBox().setSelected(false);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());
                assertFalse(LombokSettingsManager.getInstance().getSettings().isAutoAddTrackApDependencies());

                page.reset();
                assertFalse(page.isModified());

                LombokSettingsManager.getInstance().resetDefaults();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ============================================================
    // 3. Markdown Tests
    // ============================================================

    @Test
    void testMarkdownModelAndManager() {
        MarkdownLanguageSettings settings = new MarkdownLanguageSettings();
        assertEquals("Chromium browser", settings.getPreviewEngine());
        assertEquals("Editor and Preview", settings.getDefaultLayout());
        assertEquals("Split vertically", settings.getPreviewLayout());
        assertEquals(13, settings.getPreviewFontSize());
        assertTrue(settings.isSyncScroll());
        assertTrue(settings.isInjectLanguagesInCodeFences());
        assertTrue(settings.isShowProblemsInCodeFences());
        assertFalse(settings.isGroupDocumentsWithSameName());
        assertTrue(settings.isDetectCommands());
        assertFalse(settings.isPlantUmlEnabled());

        MarkdownLanguageSettings copy = settings.copy();
        assertEquals(settings, copy);

        copy.setPreviewFontSize(16);
        copy.setPlantUmlEnabled(true);
        assertNotEquals(settings, copy);

        MarkdownLanguageSettingsManager manager = MarkdownLanguageSettingsManager.getInstance();
        manager.setSettings(copy);
        assertEquals(16, manager.getSettings().getPreviewFontSize());
        assertTrue(manager.getSettings().isPlantUmlEnabled());

        manager.resetDefaults();
        assertEquals(13, manager.getSettings().getPreviewFontSize());
    }

    @Test
    void testMarkdownPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                AtomicBoolean smartKeysNavigated = new AtomicBoolean(false);
                SettingsLanguagesMarkdownPage page = new SettingsLanguagesMarkdownPage(() -> smartKeysNavigated.set(true));
                assertNotNull(page);
                assertFalse(page.isModified());

                assertEquals("Chromium browser", page.getPreviewEngineCombo().getValue());
                assertEquals(13, page.getPreviewFontSizeCombo().getValue());
                assertTrue(page.getSyncScrollCheckBox().isSelected());

                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getPreviewFontSizeCombo().setValue(16);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.getPlantUmlCheckBox().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals(16, MarkdownLanguageSettingsManager.getInstance().getSettings().getPreviewFontSize());

                // Smart Keys navigation
                page.getSmartKeysLink().fire();
                assertTrue(smartKeysNavigated.get());

                page.reset();
                assertFalse(page.isModified());

                MarkdownLanguageSettingsManager.getInstance().resetDefaults();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ============================================================
    // 4. Micronaut Tests
    // ============================================================

    @Test
    void testMicronautModelAndManager() {
        MicronautSettings settings = new MicronautSettings();
        assertTrue(settings.isCreateRunConfigurationAutomatically());

        MicronautSettings copy = settings.copy();
        assertEquals(settings, copy);

        copy.setCreateRunConfigurationAutomatically(false);
        assertNotEquals(settings, copy);

        MicronautSettingsManager manager = MicronautSettingsManager.getInstance();
        manager.setSettings(copy);
        assertFalse(manager.getSettings().isCreateRunConfigurationAutomatically());

        manager.resetDefaults();
        assertTrue(manager.getSettings().isCreateRunConfigurationAutomatically());
    }

    @Test
    void testMicronautPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesMicronautPage page = new SettingsLanguagesMicronautPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertTrue(page.getCreateRunConfigCheckBox().isSelected());

                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getCreateRunConfigCheckBox().setSelected(false);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());
                assertFalse(MicronautSettingsManager.getInstance().getSettings().isCreateRunConfigurationAutomatically());

                page.reset();
                assertFalse(page.isModified());

                MicronautSettingsManager.getInstance().resetDefaults();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ============================================================
    // 5. OpenAPI Specifications Tests
    // ============================================================

    @Test
    void testOpenAPIModelAndManager() {
        OpenAPISettings settings = new OpenAPISettings();
        assertTrue(settings.isGutterIconsForEdits());
        assertEquals("api.swaggerhub.com", settings.getSwaggerHubAddress());
        assertEquals("", settings.getSwaggerHubApiKey());
        assertTrue(settings.getRemoteSpecifications().isEmpty());

        OpenAPISettings copy = settings.copy();
        assertEquals(settings, copy);

        copy.setGutterIconsForEdits(false);
        copy.setSwaggerHubAddress("custom.swagger.internal");
        copy.setSwaggerHubApiKey("secret-token");
        copy.getRemoteSpecifications().add(new RemoteOpenAPISpec("Petstore", "https://petstore.swagger.io/v2/swagger.json", "URL"));
        assertNotEquals(settings, copy);

        OpenAPISettingsManager manager = OpenAPISettingsManager.getInstance();
        manager.setSettings(copy);
        assertEquals("custom.swagger.internal", manager.getSettings().getSwaggerHubAddress());
        assertEquals(1, manager.getSettings().getRemoteSpecifications().size());

        manager.resetDefaults();
        assertEquals("api.swaggerhub.com", manager.getSettings().getSwaggerHubAddress());
    }

    @Test
    void testOpenAPIPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesOpenAPIPage page = new SettingsLanguagesOpenAPIPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertTrue(page.getGutterIconsCheckBox().isSelected());
                assertEquals("api.swaggerhub.com", page.getSwaggerHubAddressField().getText());
                assertEquals("", page.getSwaggerHubApiKeyField().getText());

                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getGutterIconsCheckBox().setSelected(false);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.getTableItems().add(new RemoteOpenAPISpec("Petstore", "https://petstore.swagger.io/v2/swagger.json", "URL"));
                assertEquals(1, page.getTableItems().size());

                page.apply();
                assertFalse(page.isModified());
                assertFalse(OpenAPISettingsManager.getInstance().getSettings().isGutterIconsForEdits());

                page.reset();
                assertFalse(page.isModified());

                OpenAPISettingsManager.getInstance().resetDefaults();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ============================================================
    // 6. SettingsDialog Integration & Navigation Tests
    // ============================================================

    @Test
    void testSettingsDialogTreeAndNavigationForNewPages() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);
                assertNotNull(dialog);

                TreeItem<String> languagesNode = null;
                for (TreeItem<String> child : dialog.getTree().getRoot().getChildren()) {
                    if ("Languages & Frameworks".equals(child.getValue())) {
                        languagesNode = child;
                        break;
                    }
                }
                assertNotNull(languagesNode);

                // 1. Kubernetes
                TreeItem<String> k8sItem = findChild(languagesNode, "Kubernetes");
                assertNotNull(k8sItem);
                dialog.getTree().getSelectionModel().select(k8sItem);
                assertNotNull(dialog.getCurrentLanguagesKubernetesPage());

                // 2. Lombok
                TreeItem<String> lombokItem = findChild(languagesNode, "Lombok");
                assertNotNull(lombokItem);
                dialog.getTree().getSelectionModel().select(lombokItem);
                assertNotNull(dialog.getCurrentLanguagesLombokPage());

                // 3. Markdown
                TreeItem<String> markdownItem = findChild(languagesNode, "Markdown");
                assertNotNull(markdownItem);
                dialog.getTree().getSelectionModel().select(markdownItem);
                assertNotNull(dialog.getCurrentLanguagesMarkdownPage());

                // 4. Micronaut
                TreeItem<String> micronautItem = findChild(languagesNode, "Micronaut");
                assertNotNull(micronautItem);
                dialog.getTree().getSelectionModel().select(micronautItem);
                assertNotNull(dialog.getCurrentLanguagesMicronautPage());

                // 5. OpenAPI Specifications
                TreeItem<String> openApiItem = findChild(languagesNode, "OpenAPI Specifications");
                assertNotNull(openApiItem);
                dialog.getTree().getSelectionModel().select(openApiItem);
                assertNotNull(dialog.getCurrentLanguagesOpenApiPage());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private TreeItem<String> findChild(TreeItem<String> parent, String value) {
        for (TreeItem<String> child : parent.getChildren()) {
            if (value.equals(child.getValue())) {
                return child;
            }
        }
        return null;
    }
}
