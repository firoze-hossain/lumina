package dev.lumina.ui;

import dev.lumina.advanced.AdvancedSettingItem;
import dev.lumina.advanced.AdvancedSettingsManager;
import dev.lumina.advanced.AdvancedSettingType;
import dev.lumina.util.Settings;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsAdvancedPageTest {

    @BeforeAll
    public static void initJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Already started
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @BeforeEach
    public void setup() {
        Settings.clear();
        AdvancedSettingsManager.resetInstanceForTesting();
    }

    private void runFx(Runnable r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testManagerRegistrationAndDefaults() {
        AdvancedSettingsManager manager = AdvancedSettingsManager.getInstance();
        List<AdvancedSettingItem> items = manager.getAllItems();
        assertFalse(items.isEmpty());
        assertTrue(items.size() >= 125, "Expected at least 125 advanced settings, found: " + items.size());

        List<String> groups = manager.getGroups();
        assertTrue(groups.contains("AI Assistant"));
        assertTrue(groups.contains("Angular"));
        assertTrue(groups.contains("Appearance"));
        assertTrue(groups.contains("Bookmarks"));
        assertTrue(groups.contains("Build Tools. Gradle"));
        assertTrue(groups.contains("Code Review Tools"));
        assertTrue(groups.contains("Compiler"));
        assertTrue(groups.contains("Database"));
        assertTrue(groups.contains("Debugger"));
        assertTrue(groups.contains("Dev Containers"));
        assertTrue(groups.contains("Docker"));
        assertTrue(groups.contains("Documentation Components"));
        assertTrue(groups.contains("Editor"));
        assertTrue(groups.contains("Editor Tabs"));
        assertTrue(groups.contains("Find/Replace"));
        assertTrue(groups.contains("Frameworks. Ktor"));
        assertTrue(groups.contains("Frameworks. Micronaut"));
        assertTrue(groups.contains("Frameworks. Spring Boot"));
        assertTrue(groups.contains("Gateway WSL"));
        assertTrue(groups.contains("GitHub actions"));
        assertTrue(groups.contains("Go"));
        assertTrue(groups.contains("IDE"));
        assertTrue(groups.contains("JVM languages"));
        assertTrue(groups.contains("Java"));
        assertTrue(groups.contains("Java Bytecode Decompiler"));
        assertTrue(groups.contains("JavaScript and TypeScript"));
        assertTrue(groups.contains("Kotlin"));
        assertTrue(groups.contains("Kubernetes"));
        assertTrue(groups.contains("Markdown"));
        assertTrue(groups.contains("Other"));
        assertTrue(groups.contains("PHP"));
        assertTrue(groups.contains("Project View"));
        assertTrue(groups.contains("Python"));
        assertTrue(groups.contains("Rails"));
        assertTrue(groups.contains("Rake"));
        assertTrue(groups.contains("Run/Debug"));
        assertTrue(groups.contains("Rust"));
        assertTrue(groups.contains("SSH"));
        assertTrue(groups.contains("Search Everywhere"));
        assertTrue(groups.contains("Search Scopes in Find, Replace, Rename"));
        assertTrue(groups.contains("Security Analysis"));
        assertTrue(groups.contains("Startup"));
        assertTrue(groups.contains("Suggested refactoring"));
        assertTrue(groups.contains("Terminal"));
        assertTrue(groups.contains("Tool Windows"));
        assertTrue(groups.contains("User Interface"));
        assertTrue(groups.contains("Version Control"));
        assertTrue(groups.contains("Version Control. Git"));

        // Verify key default states matching screenshots
        assertFalse(manager.getBoolean("ai.prompt.autodetection"));
        assertFalse(manager.getBoolean("angular.show.navigation.popup"));
        assertFalse(manager.getBoolean("appearance.islands.ui"));
        assertTrue(manager.getBoolean("bookmarks.show.only.line.bookmarks"));
        assertTrue(manager.getBoolean("compiler.gradle.run.using.gradle"));
        assertFalse(manager.getBoolean("gradle.download.sources"));
        assertTrue(manager.getBoolean("compiler.run.with.lower.priority"));
        assertEquals("Table-first formats", manager.getString("database.open.file.as.table.scripted.loader"));
        assertEquals(50, manager.getInt("debugger.max.recent.expressions"));
        assertEquals(100, manager.getInt("docker.image.registry.loading.limit"));
        assertEquals(3000, manager.getInt("docker.reconnection.delay.ms"));
        assertEquals(-1, manager.getInt("editor.distraction.free.left.margin"));
        assertEquals(100000, manager.getInt("editor.force.soft.wrap.line.limit"));
        assertEquals("Paste above the caret line", manager.getString("editor.paste.line.no.selection.behavior"));
        assertEquals("Horizontal line", manager.getString("editor.tab.character.rendering"));
        assertTrue(manager.getBoolean("github.actions.authoring.support"));
        assertTrue(manager.getBoolean("go.suggest.optimal.regional.goproxy"));

        // Verify batch 2 defaults
        assertEquals(50, manager.getInt("ide.max.recent.projects"));
        assertEquals(5, manager.getInt("ide.local.history.days"));
        assertTrue(manager.getBoolean("jvm.languages.highlight.terminal.class.names"));
        assertEquals(0, manager.getInt("java.code.vision.min.usages"));
        assertEquals(20000, manager.getInt("java.decompiler.max.direct.nodes"));
        assertEquals("Normal", manager.getString("js.ts.semantic.highlighting.accuracy"));
        assertTrue(manager.getBoolean("js.ts.advanced.js.annotator"));
        assertEquals(10, manager.getInt("kubernetes.max.kubeconfig.size.mb"));
        assertEquals(50, manager.getInt("other.http.client.requests.log.max"));
        assertEquals(150, manager.getInt("php.max.depth.resolving.member.references"));
        assertTrue(manager.getBoolean("project.view.move.focus.editor.enter"));
        assertEquals(20000, manager.getInt("python.debugger.attach.timeout"));
        assertTrue(manager.getBoolean("rails.load.generators.automatically"));
        assertTrue(manager.getBoolean("rake.load.tasks.automatically"));
        assertEquals(5, manager.getInt("run.debug.temporary.configurations.limit"));
        assertTrue(manager.getBoolean("rust.show.test.results.tool.window"));
        assertEquals("https://docs.rs/", manager.getString("rust.base.url.external.doc"));
        assertEquals("OpenSSH", manager.getString("ssh.config.files.parser"));
        assertEquals("ssh", manager.getString("ssh.custom.path.openssh.tool"));
        assertTrue(manager.getBoolean("search.everywhere.wait.all.contributors"));
        assertFalse(manager.getBoolean("search.scopes.keep.last.selected"));
        assertTrue(manager.getBoolean("security.analysis.show.problems.tab"));

        // Verify batch 3 defaults
        assertTrue(manager.getBoolean("startup.open.readme.no.open.files"));
        assertFalse(manager.getBoolean("suggested.refactoring.show.hint.in.editor"));
        assertFalse(manager.getBoolean("terminal.show.application.title"));
        assertEquals(5000, manager.getInt("terminal.scrollback.buffer.size"));
        assertTrue(manager.getBoolean("terminal.move.focus.to.editor.escape"));
        assertTrue(manager.getBoolean("terminal.typeahead"));
        assertEquals(100, manager.getInt("terminal.typeahead.latency.threshold.ms"));
        assertTrue(manager.getBoolean("terminal.use.one.point.zero.line.spacing.alt.screen"));
        assertEquals("UTF-8", manager.getString("terminal.character.encoding"));
        assertEquals(1024, manager.getInt("terminal.new.output.capacity.kb"));
        assertTrue(manager.getBoolean("terminal.start.ssh.terminal.in.deployment.dir"));
        assertTrue(manager.getBoolean("tool.windows.allow.dragging.by.header"));
        assertTrue(manager.getBoolean("ui.use.native.file.chooser"));
        assertTrue(manager.getBoolean("ui.merge.main.menu.window.title"));
        assertTrue(manager.getBoolean("ui.activate.menu.items.right.mouse.release"));
        assertTrue(manager.getBoolean("ui.cyclic.scrolling.lists.trees"));
        assertTrue(manager.getBoolean("vcs.open.diff.as.editor.tab"));
        assertTrue(manager.getBoolean("vcs.highlight.ignored.files"));
        assertTrue(manager.getBoolean("vcs.enable.commit.tool.window"));
        assertTrue(manager.getBoolean("vcs.auto.close.commit.window.float.mode"));
        assertTrue(manager.getBoolean("vcs.git.recursively.clone.submodules"));
        assertEquals("Filters", manager.getString("vcs.git.apply.content.transformation"));
        assertTrue(manager.getBoolean("vcs.git.use.safe.force.push"));
        assertTrue(manager.getBoolean("vcs.git.check.incoming.outgoing.commits"));
        assertEquals("-", manager.getString("vcs.git.branch.name.cleanup.symbol"));
        assertEquals("Projects with identical names", manager.getString("vcs.git.show.branch.names.recent.projects"));
        assertFalse(manager.getBoolean("vcs.git.show.tab.in.search.everywhere"));
    }

    @Test
    public void testManagerGetSetAndPersistence() {
        AdvancedSettingsManager manager = AdvancedSettingsManager.getInstance();

        manager.setBoolean("ai.prompt.autodetection", true);
        manager.setInt("debugger.max.recent.expressions", 80);
        manager.setString("editor.tab.character.rendering", "Arrow");
        manager.setString("ssh.custom.path.openssh.tool", "/usr/local/bin/ssh");

        assertTrue(manager.getBoolean("ai.prompt.autodetection"));
        assertEquals(80, manager.getInt("debugger.max.recent.expressions"));
        assertEquals("Arrow", manager.getString("editor.tab.character.rendering"));
        assertEquals("/usr/local/bin/ssh", manager.getString("ssh.custom.path.openssh.tool"));

        manager.apply();

        // Verify stored in Settings
        assertEquals("true", Settings.get("advanced.setting.ai.prompt.autodetection"));
        assertEquals("80", Settings.get("advanced.setting.debugger.max.recent.expressions"));
        assertEquals("Arrow", Settings.get("advanced.setting.editor.tab.character.rendering"));
        assertEquals("/usr/local/bin/ssh", Settings.get("advanced.setting.ssh.custom.path.openssh.tool"));

        // Reset instance and verify reloaded from storage
        AdvancedSettingsManager.resetInstanceForTesting();
        AdvancedSettingsManager reloaded = AdvancedSettingsManager.getInstance();
        assertTrue(reloaded.getBoolean("ai.prompt.autodetection"));
        assertEquals(80, reloaded.getInt("debugger.max.recent.expressions"));
        assertEquals("Arrow", reloaded.getString("editor.tab.character.rendering"));
        assertEquals("/usr/local/bin/ssh", reloaded.getString("ssh.custom.path.openssh.tool"));
    }

    @Test
    public void testSnapshottingAndRollback() {
        AdvancedSettingsManager manager = AdvancedSettingsManager.getInstance();
        Map<String, Object> snapshot = manager.createSnapshot();

        manager.setBoolean("ai.prompt.autodetection", true);
        manager.setInt("debugger.max.recent.expressions", 999);

        assertTrue(manager.getBoolean("ai.prompt.autodetection"));
        assertEquals(999, manager.getInt("debugger.max.recent.expressions"));

        manager.restoreSnapshot(snapshot);

        assertFalse(manager.getBoolean("ai.prompt.autodetection"));
        assertEquals(50, manager.getInt("debugger.max.recent.expressions"));
    }

    @Test
    public void testExtensibilityRegisterCustomSetting() {
        AdvancedSettingsManager manager = AdvancedSettingsManager.getInstance();
        AdvancedSettingItem custom = AdvancedSettingItem.builder("custom.plugin.feature.enabled", "Custom Subsystem", "Enable custom feature")
                .type(AdvancedSettingType.BOOLEAN).defaultValue(true).build();
        manager.register(custom);

        assertNotNull(manager.getItem("custom.plugin.feature.enabled"));
        assertTrue(manager.getBoolean("custom.plugin.feature.enabled"));
        assertTrue(manager.getGroups().contains("Custom Subsystem"));
    }

    @Test
    public void testPageLifecycleAndModificationTracking() throws Exception {
        runFx(() -> {
            AdvancedSettingsManager manager = AdvancedSettingsManager.getInstance();
            SettingsAdvancedPage page = new SettingsAdvancedPage(manager);

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModified(() -> modifiedNotified.set(true));

            assertFalse(page.isModified());

            // Modify a boolean checkbox
            page.setUiValue("ai.prompt.autodetection", true);
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            // Apply changes
            page.apply();
            assertFalse(page.isModified());
            assertTrue(manager.getBoolean("ai.prompt.autodetection"));

            // Modify an integer input
            modifiedNotified.set(false);
            page.setUiValue("debugger.max.recent.expressions", 120);
            assertTrue(page.isModified());

            // Revert changes
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals(50, page.getUiValue("debugger.max.recent.expressions"));
        });
    }

    @Test
    public void testLiveSearchFilter() throws Exception {
        runFx(() -> {
            AdvancedSettingsManager manager = AdvancedSettingsManager.getInstance();
            SettingsAdvancedPage page = new SettingsAdvancedPage(manager);

            // Verify all sections initially visible
            assertTrue(page.getGroupSectionBoxes().get("Build Tools. Gradle").isVisible());
            assertTrue(page.getGroupSectionBoxes().get("AI Assistant").isVisible());

            // Filter for Gradle
            page.setSearchQuery("Gradle");

            assertTrue(page.getGroupSectionBoxes().get("Build Tools. Gradle").isVisible());
            // Sections without "Gradle" (like AI Assistant) should be hidden
            assertFalse(page.getGroupSectionBoxes().get("AI Assistant").isVisible());

            // Clear search
            page.setSearchQuery("");
            assertTrue(page.getGroupSectionBoxes().get("AI Assistant").isVisible());
        });
    }

    @Test
    public void testShowModifiedOnlyFilter() throws Exception {
        runFx(() -> {
            AdvancedSettingsManager manager = AdvancedSettingsManager.getInstance();
            SettingsAdvancedPage page = new SettingsAdvancedPage(manager);

            // Modify one item
            page.setUiValue("ai.prompt.autodetection", true);

            // Enable "Show modified only"
            page.setShowModifiedOnly(true);

            // AI Assistant section has modified item, should be visible
            assertTrue(page.getGroupSectionBoxes().get("AI Assistant").isVisible());

            // Appearance section has no modified item, should be hidden
            assertFalse(page.getGroupSectionBoxes().get("Appearance").isVisible());

            // Reset back to default
            page.setUiValue("ai.prompt.autodetection", false);
            assertFalse(page.getGroupSectionBoxes().get("AI Assistant").isVisible());

            // Disable modified only
            page.setShowModifiedOnly(false);
            assertTrue(page.getGroupSectionBoxes().get("Appearance").isVisible());
        });
    }

    @Test
    public void testSettingsDialogIntegration() throws Exception {
        runFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            dialog.selectCategory("Advanced Settings");

            SettingsAdvancedPage page = dialog.getCurrentAdvancedSettingsPage();
            assertNotNull(page, "Advanced Settings page should be constructed and displayed");

            // Verify modification triggers apply button
            assertFalse(page.isModified());
            page.setUiValue("angular.show.navigation.popup", true);
            assertTrue(page.isModified());

            // Apply all
            dialog.applyAll();
            assertFalse(page.isModified());
            assertTrue(AdvancedSettingsManager.getInstance().getBoolean("angular.show.navigation.popup"));
        });
    }

    @Test
    public void testBrandIsolation() throws IOException {
        String[] paths = {
                "src/main/java/dev/lumina/advanced/AdvancedSettingType.java",
                "src/main/java/dev/lumina/advanced/AdvancedSettingItem.java",
                "src/main/java/dev/lumina/advanced/AdvancedSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsAdvancedPage.java"
        };

        String[] forbidden = {"jetbrains", "intellij", "pycharm", "webstorm", "clion", "rider"};

        for (String p : paths) {
            Path file = Path.of(p);
            assertTrue(Files.exists(file), "File must exist: " + p);
            String content = Files.readString(file).toLowerCase();
            for (String f : forbidden) {
                assertFalse(content.contains(f), "File " + p + " contains forbidden competitor keyword: " + f);
            }
        }
    }
}
