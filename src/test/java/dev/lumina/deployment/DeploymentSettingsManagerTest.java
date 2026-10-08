package dev.lumina.deployment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeploymentSettingsManager and Deployment models.
 */
public class DeploymentSettingsManagerTest {

    private DeploymentSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = DeploymentSettingsManager.getInstance();
        manager.resetToDefaults();
    }

    @Test
    void testDeploymentOptionsDefaultsAndCloning() {
        DeploymentOptions opt = manager.getOptions();
        assertNotNull(opt);
        assertEquals(".svn;.cvs;.idea;.DS_Store;.git;hg;*.hprof;*.pyc", opt.getExcludeItemsByName());
        assertEquals("Details", opt.getOperationsLogging());
        assertTrue(opt.isOverwriteUpToDateFiles());
        assertFalse(opt.isUseTemporaryFileDuringUpload());
        assertTrue(opt.isPreserveFileTimestamps());
        assertFalse(opt.isDeleteTargetItemsWhenSourceNotExist());
        assertTrue(opt.isConfirmDeletingRemoteFiles());
        assertFalse(opt.isCreateEmptyDirectories());
        assertTrue(opt.isPromptWhenOverwritingOrDeletingLocalItems());
        assertTrue(opt.isConfirmUploadingFiles());
        assertEquals("Never", opt.getUploadChangedFilesAutomatically());
        assertFalse(opt.isSkipExternalChanges());
        assertFalse(opt.isDeleteRemoteFilesWhenLocalDeleted());
        assertFalse(opt.isPreserveOriginalFilePermissions());
        assertFalse(opt.isOverrideDefaultPermissionsFiles());
        assertEquals("(none)", opt.getPermissionsFiles());
        assertFalse(opt.isOverrideDefaultPermissionsFolders());
        assertEquals("(none)", opt.getPermissionsFolders());
        assertEquals("No", opt.getWarnWhenUploadingOverNewerFile());
        assertFalse(opt.isNotifyOfRemoteChanges());

        DeploymentOptions clone = opt.clone();
        assertEquals(opt, clone);
        assertEquals(opt.hashCode(), clone.hashCode());

        clone.setOperationsLogging("None");
        clone.setUploadChangedFilesAutomatically("Always");
        assertNotEquals(opt, clone);
    }

    @Test
    void testFileExclusionPatternMatching() {
        DeploymentOptions opt = manager.getOptions();

        assertTrue(opt.isFileExcluded(".git"));
        assertTrue(opt.isFileExcluded(".svn"));
        assertTrue(opt.isFileExcluded(".idea"));
        assertTrue(opt.isFileExcluded(".DS_Store"));
        assertTrue(opt.isFileExcluded("dump.hprof"));
        assertTrue(opt.isFileExcluded("script.pyc"));
        assertFalse(opt.isFileExcluded("Main.java"));
        assertFalse(opt.isFileExcluded("pom.xml"));
        assertFalse(opt.isFileExcluded("index.html"));

        opt.setExcludeItemsByName("*.bak;temp_*;test.txt");
        assertTrue(opt.isFileExcluded("file.bak"));
        assertTrue(opt.isFileExcluded("temp_data"));
        assertTrue(opt.isFileExcluded("test.txt"));
        assertFalse(opt.isFileExcluded("prod_data"));
    }

    @Test
    void testDeploymentServerManagement() {
        assertEquals(0, manager.getServers().size());
        assertNull(manager.getDefaultServerId());

        DeploymentServer s1 = new DeploymentServer("Web Staging", DeploymentServerType.SFTP);
        s1.setHost("staging.example.com");
        s1.setPort(22);
        s1.setUsername("deployer");
        manager.addServer(s1);

        assertEquals(1, manager.getServers().size());
        assertEquals(s1.getId(), manager.getDefaultServerId());
        assertTrue(manager.getServerById(s1.getId()).isDefaultServer());

        DeploymentServer s2 = new DeploymentServer("Local Preview", DeploymentServerType.LOCAL);
        s2.setLocalFolderPath("/var/www/html");
        manager.addServer(s2);

        assertEquals(2, manager.getServers().size());

        // Change default server
        manager.setDefaultServerId(s2.getId());
        assertEquals(s2.getId(), manager.getDefaultServerId());
        assertTrue(manager.getServerById(s2.getId()).isDefaultServer());
        assertFalse(manager.getServerById(s1.getId()).isDefaultServer());

        // Remove server
        manager.removeServer(s1.getId());
        assertEquals(1, manager.getServers().size());
        assertEquals(s2.getId(), manager.getDefaultServerId());
    }

    @Test
    void testPersistenceCycle() {
        DeploymentServer s = new DeploymentServer("Production Cluster", DeploymentServerType.SFTP);
        s.setHost("prod.internal");
        s.setPort(2222);
        manager.addServer(s);

        DeploymentOptions opt = manager.getOptions();
        opt.setUploadChangedFilesAutomatically("Always");
        opt.setOverwriteUpToDateFiles(false);
        manager.setOptions(opt);

        manager.saveSettings();

        // Reload
        manager.loadSettings();
        assertEquals(1, manager.getServers().size());
        assertEquals("prod.internal", manager.getServers().get(0).getHost());
        assertEquals(2222, manager.getServers().get(0).getPort());
        assertEquals("Always", manager.getOptions().getUploadChangedFilesAutomatically());
        assertFalse(manager.getOptions().isOverwriteUpToDateFiles());
    }
}
