package dev.lumina.kubernetes;

import dev.lumina.util.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KubernetesSettingsManagerTest {

    private KubernetesSettingsManager manager;

    @BeforeEach
    void setUp() {
        Settings.clear();
        manager = KubernetesSettingsManager.getInstance();
        manager.resetToDefaults();
    }

    @Test
    void testDefaultSettingsValues() {
        KubernetesSettings s = manager.getSettings();
        assertNotNull(s);

        // Tool locations
        assertEquals("kubectl", s.getKubectlPath());
        assertEquals("helm", s.getHelmPath());

        // Configuration
        assertTrue(s.isReloadConfigAutomatically());
        assertTrue(s.isRefreshClusterResources());
        assertTrue(s.getConfigFiles().isEmpty());

        // Pod Shell
        assertEquals("/bin/sh", s.getShellCommand());
        assertTrue(s.isShellCommandGlobal());

        // Appearance
        assertEquals("Always Show", s.getFloatingToolbarMode());

        // Logs
        assertEquals("SCRATCHES", s.getDownloadLogsMode());
        assertEquals("Download to Scratches", s.getCustomDownloadPath());
        assertFalse(s.isAppendTimestampToLogFileName());
        assertFalse(s.isDropAnsiSymbols());
        assertTrue(s.isDisplayTimestamp());
        assertTrue(s.isDisplaySource());
        assertTrue(s.isDisplayMessage());
        assertEquals("In editor", s.getClusterEventsPresentationMode());
        assertEquals(300, s.getLogCacheSizeMb());
        assertEquals(250, s.getLogEditorUpdateDelayMs());

        // Default Log Filters (Screenshot 4)
        List<KubernetesLogFilter> filters = s.getLogFilters();
        assertEquals(3, filters.size());

        KubernetesLogFilter f1 = filters.get(0);
        assertTrue(f1.isEnabled());
        assertEquals("(?i)\\b(e(rr(or)?)?|severe)\\b", f1.getPattern());
        assertTrue(f1.isBold());
        assertFalse(f1.isItalic());
        assertEquals("#E05555", f1.getColorHex());

        KubernetesLogFilter f2 = filters.get(1);
        assertTrue(f2.isEnabled());
        assertEquals("(?i)\\b(w(arn(ing)?)?)\\b", f2.getPattern());
        assertFalse(f2.isBold());
        assertFalse(f2.isItalic());
        assertEquals("#E5A84B", f2.getColorHex());

        KubernetesLogFilter f3 = filters.get(2);
        assertFalse(f3.isEnabled());
        assertEquals("(?i)\\b(i(nfo)?)\\b", f3.getPattern());
        assertFalse(f3.isBold());
        assertFalse(f3.isItalic());
        assertEquals("#59A869", f3.getColorHex());

        // Namespaces, custom args, ephemeral
        assertTrue(s.getNamespaces().isEmpty());
        assertTrue(s.isAppendServerPathFlag());
        assertTrue(s.getCustomArgs().isEmpty());
        assertTrue(s.getEphemeralContainers().isEmpty());
    }

    @Test
    void testLogFilterMatching() {
        KubernetesLogFilter errorFilter = new KubernetesLogFilter(true, "(?i)\\b(e(rr(or)?)?|severe)\\b", true, false, "#E05555");
        assertTrue(errorFilter.matches("2026-10-08 12:00:00 [ERROR] Connection lost"));
        assertTrue(errorFilter.matches("2026-10-08 12:00:00 [ERR] Connection lost"));
        assertTrue(errorFilter.matches("A severe failure occurred in pod-1"));
        assertFalse(errorFilter.matches("Everything running normally"));

        KubernetesLogFilter warnFilter = new KubernetesLogFilter(true, "(?i)\\b(w(arn(ing)?)?)\\b", false, false, "#E5A84B");
        assertTrue(warnFilter.matches("Disk space warning on node-a"));
        assertTrue(warnFilter.matches("WARN: CPU throttle"));
        assertFalse(warnFilter.matches("Success info message"));
    }

    @Test
    void testPersistence() {
        KubernetesSettings s = manager.getSettings();
        s.setKubectlPath("/usr/local/bin/kubectl");
        s.setHelmPath("/usr/local/bin/helm");
        s.setShellCommand("/bin/bash");
        s.setFloatingToolbarMode("Never Show");
        s.setDownloadLogsMode("ASK");
        s.setLogCacheSizeMb(500);

        s.getConfigFiles().add(new KubernetesConfigFile(true, "/home/user/.kube/prod-config", "Project"));
        s.getNamespaces().add(new KubernetesNamespaceItem("prod-billing", "Project"));
        s.getCustomArgs().add(new KubernetesCustomArg("prod-cluster", "--insecure-skip-tls-verify"));
        s.getEphemeralContainers().add(new KubernetesEphemeralContainer("gdb-debug", "-i -t --image=gdb:latest"));

        manager.setSettings(s);

        // Reload
        manager.loadSettings();
        KubernetesSettings loaded = manager.getSettings();

        assertEquals("/usr/local/bin/kubectl", loaded.getKubectlPath());
        assertEquals("/usr/local/bin/helm", loaded.getHelmPath());
        assertEquals("/bin/bash", loaded.getShellCommand());
        assertEquals("Never Show", loaded.getFloatingToolbarMode());
        assertEquals("ASK", loaded.getDownloadLogsMode());
        assertEquals(500, loaded.getLogCacheSizeMb());

        assertEquals(1, loaded.getConfigFiles().size());
        assertEquals("/home/user/.kube/prod-config", loaded.getConfigFiles().get(0).getPath());

        assertEquals(1, loaded.getNamespaces().size());
        assertEquals("prod-billing", loaded.getNamespaces().get(0).getNamespace());

        assertEquals(1, loaded.getCustomArgs().size());
        assertEquals("prod-cluster", loaded.getCustomArgs().get(0).getCluster());

        assertEquals(1, loaded.getEphemeralContainers().size());
        assertEquals("gdb-debug", loaded.getEphemeralContainers().get(0).getName());
    }

    @Test
    void testToolVerification() {
        KubernetesSettingsManager.ToolTestResult res1 = manager.testTool("", "kubectl");
        assertFalse(res1.success());

        // Test with a binary known to exist on Linux like 'sh' or 'echo'
        KubernetesSettingsManager.ToolTestResult resEcho = manager.testTool("echo", "echo");
        assertTrue(resEcho.success());
    }
}
