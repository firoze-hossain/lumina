package dev.lumina.git;

import dev.lumina.git.SubversionNetworkOptionsManager.NetworkGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SubversionNetworkOptionsManagerTest {

    @Test
    void testInitialGlobalGroupExists() {
        SubversionNetworkOptionsManager mgr = new SubversionNetworkOptionsManager();
        assertTrue(mgr.getGroups().containsKey("global"));
        NetworkGroup global = mgr.getGroup("global");
        assertNotNull(global);
        assertEquals("global", global.getName());
    }

    @Test
    void testAddRemoveAndCopyGroups() {
        SubversionNetworkOptionsManager mgr = new SubversionNetworkOptionsManager();

        assertTrue(mgr.addGroup("internal"));
        assertFalse(mgr.addGroup("internal")); // duplicate
        assertFalse(mgr.addGroup("")); // invalid
        assertTrue(mgr.getGroups().containsKey("internal"));

        NetworkGroup internal = mgr.getGroup("internal");
        internal.setServer("proxy.internal.corp");
        internal.setPort("8080");

        NetworkGroup copied = internal.copy("internal_backup");
        mgr.getGroups().put("internal_backup", copied);
        assertEquals("proxy.internal.corp", mgr.getGroup("internal_backup").getServer());
        assertEquals("8080", mgr.getGroup("internal_backup").getPort());

        assertFalse(mgr.removeGroup("global")); // Cannot remove global
        assertTrue(mgr.removeGroup("internal"));
        assertNull(mgr.getGroup("internal"));
    }

    @Test
    void testSaveAndLoadFile(@TempDir Path tempDir) throws IOException {
        Path serversFile = tempDir.resolve("servers");

        SubversionNetworkOptionsManager mgr = new SubversionNetworkOptionsManager();
        NetworkGroup global = mgr.getGroup("global");
        global.setServer("default-proxy.org");
        global.setPort("3128");
        global.setUser("svc_user");
        global.setPassword("svc_pass");
        global.setTimeout("60");
        global.setCaCertFiles("/etc/ssl/certs/ca-certificates.crt");
        global.setTrustDefaultCas(true);

        mgr.addGroup("corp");
        NetworkGroup corp = mgr.getGroup("corp");
        corp.setUrlPatterns("*.corp.internal, 10.0.*");
        corp.setServer("corp-proxy.internal");
        corp.setPort("8888");

        mgr.saveToFile(serversFile);
        assertTrue(Files.exists(serversFile));

        // Load into new manager
        SubversionNetworkOptionsManager loaded = new SubversionNetworkOptionsManager();
        loaded.loadFromFile(serversFile);

        assertEquals(2, loaded.getGroups().size());
        NetworkGroup loadedGlobal = loaded.getGroup("global");
        assertNotNull(loadedGlobal);
        assertEquals("default-proxy.org", loadedGlobal.getServer());
        assertEquals("3128", loadedGlobal.getPort());
        assertEquals("svc_user", loadedGlobal.getUser());
        assertEquals("svc_pass", loadedGlobal.getPassword());
        assertEquals("60", loadedGlobal.getTimeout());
        assertEquals("/etc/ssl/certs/ca-certificates.crt", loadedGlobal.getCaCertFiles());
        assertTrue(loadedGlobal.isTrustDefaultCas());

        NetworkGroup loadedCorp = loaded.getGroup("corp");
        assertNotNull(loadedCorp);
        assertEquals("*.corp.internal, 10.0.*", loadedCorp.getUrlPatterns());
        assertEquals("corp-proxy.internal", loadedCorp.getServer());
        assertEquals("8888", loadedCorp.getPort());
    }
}
