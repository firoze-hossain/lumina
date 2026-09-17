package dev.lumina.project;

import dev.lumina.project.NodeDistMetadata.NodeRelease;
import dev.lumina.project.NodeDistMetadata.NodeVersionEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NodeDistMetadataTest {

    @Test
    void testGetReleasesCatalog() {
        List<NodeRelease> releases = NodeDistMetadata.getReleases(false);
        assertNotNull(releases, "Releases list should not be null");
        assertFalse(releases.isEmpty(), "Should contain Node.js releases");

        // Verify presence of LTS releases
        NodeRelease rel24 = releases.stream()
                .filter(r -> "24".equals(r.major()))
                .findFirst()
                .orElse(null);

        assertNotNull(rel24, "Should find Node.js 24 release");
        assertTrue(rel24.isLts(), "Node 24 should be LTS");
        assertEquals("Krypton", rel24.ltsCodename());
        assertEquals("Node.js 24  LTS: Krypton", rel24.formatDisplay());
        assertFalse(rel24.versions().isEmpty(), "Node 24 should have patch versions");

        NodeVersionEntry v24_21 = rel24.versions().getFirst();
        assertEquals("24.21.0", v24_21.version());
        assertTrue(v24_21.isLts());
        assertEquals("Krypton", v24_21.ltsCodename());
        String display = v24_21.formatDisplay();
        assertTrue(display.contains("24.21.0"));
        assertTrue(display.contains("LTS"));
        assertTrue(display.contains("npm@"));
        assertTrue(display.contains("Released on"));

        // Verify non-LTS release display format
        NodeRelease rel25 = releases.stream()
                .filter(r -> "25".equals(r.major()))
                .findFirst()
                .orElse(null);

        if (rel25 != null) {
            assertFalse(rel25.isLts());
            assertEquals("Node.js 25", rel25.formatDisplay());
        }
    }

    @Test
    void testParseIndexJson() {
        String json = """
                [
                  {
                    "version": "v24.21.0",
                    "date": "2026-09-07",
                    "files": ["osx-arm64-tar", "osx-x64-tar", "win-x64-zip"],
                    "npm": "11.19.0",
                    "lts": "Krypton"
                  },
                  {
                    "version": "v24.20.0",
                    "date": "2026-08-25",
                    "files": ["osx-arm64-tar", "win-x64-zip"],
                    "npm": "11.19.0",
                    "lts": "Krypton"
                  },
                  {
                    "version": "v25.1.0",
                    "date": "2026-10-20",
                    "files": ["osx-arm64-tar"],
                    "npm": "11.20.0",
                    "lts": false
                  }
                ]
                """;

        List<NodeRelease> parsed = NodeDistMetadata.parseIndexJson(json);
        assertNotNull(parsed);
        assertEquals(2, parsed.size(), "Should produce releases 25 and 24");

        NodeRelease rel25 = parsed.get(0);
        assertEquals("25", rel25.major());
        assertFalse(rel25.isLts());
        assertEquals(1, rel25.versions().size());
        assertEquals("25.1.0", rel25.versions().get(0).version());

        NodeRelease rel24 = parsed.get(1);
        assertEquals("24", rel24.major());
        assertTrue(rel24.isLts());
        assertEquals("Krypton", rel24.ltsCodename());
        assertEquals(2, rel24.versions().size());
        assertEquals("24.21.0", rel24.versions().get(0).version());
        assertEquals("24.20.0", rel24.versions().get(1).version());
    }

    @Test
    void testResolveDownloadUrl() {
        String url = NodeDistMetadata.resolveDownloadUrl("24.21.0");
        assertNotNull(url);
        assertTrue(url.startsWith("https://nodejs.org/dist/v24.21.0/node-v24.21.0-"));
        assertTrue(url.endsWith(".tar.gz") || url.endsWith(".zip"));

        // With leading v
        String urlWithV = NodeDistMetadata.resolveDownloadUrl("v24.21.0");
        assertEquals(url, urlWithV);
    }

    @Test
    void testGetDefaultInstallLocation() {
        String loc = NodeDistMetadata.getDefaultInstallLocation("24.21.0");
        assertNotNull(loc);
        assertTrue(loc.endsWith("24.21.0"));
        assertTrue(loc.contains("Lumina") || loc.contains("lumina"));
    }

    @Test
    void testDownloadAndExtract(@TempDir Path tempDir) throws IOException {
        Path installTarget = tempDir.resolve("node-24.21.0");
        List<String> logs = new ArrayList<>();

        Path nodeBin = NodeDistMetadata.downloadAndExtract("24.21.0", installTarget, logs::add);

        assertNotNull(nodeBin, "Downloaded/configured node binary path should not be null");
        assertTrue(Files.exists(nodeBin), "Node binary should exist on disk");
        assertFalse(logs.isEmpty(), "Should have received progress log entries");

        // Verify NodeMetadata probe on the installed interpreter
        String probed = NodeMetadata.probeVersion(nodeBin.toAbsolutePath().toString());
        assertNotNull(probed);
        assertTrue(probed.contains("24."), "Probed version should match major version 24");
    }
}
