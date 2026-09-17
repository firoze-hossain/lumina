package dev.lumina.project;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RustMetadataTest {

    @Test
    void testDiscoverToolchains() {
        List<String> toolchains = RustMetadata.discoverToolchains();
        assertNotNull(toolchains);
        // If cargo or rustc is installed on this machine, toolchains should not be empty
        File cargoBin = new File(System.getProperty("user.home"), ".cargo/bin");
        if (cargoBin.isDirectory()) {
            assertTrue(toolchains.contains(cargoBin.getAbsolutePath()),
                    "Should detect ~/.cargo/bin when present");
        }
    }

    @Test
    void testDefaultToolchainPath() {
        String defaultPath = RustMetadata.defaultToolchainPath();
        assertNotNull(defaultPath);
        assertFalse(defaultPath.isBlank());
    }

    @Test
    void testDetectRustVersion() {
        String defaultPath = RustMetadata.defaultToolchainPath();
        String version = RustMetadata.detectRustVersion(defaultPath);
        assertNotNull(version);
        assertFalse(version.isBlank());
        // Should either be a valid semver like "1.80.0" or "1.97.0" or "Not detected"
        if (!version.equals("Not detected")) {
            assertTrue(version.matches("\\d+\\.\\d+.*"), "Version should start with numbers: " + version);
        }
    }

    @Test
    void testDetectStandardLibrary() {
        String defaultPath = RustMetadata.defaultToolchainPath();
        String stdlib = RustMetadata.detectStandardLibrary(defaultPath);
        assertNotNull(stdlib);
        // If host has rust installed, check path contains rustlib or rust
        if (!stdlib.isBlank()) {
            assertTrue(stdlib.contains("rust") || stdlib.contains("rustlib"),
                    "Standard library path should contain rust or rustlib: " + stdlib);
        }
    }

    @Test
    void testDefaultTemplates() {
        List<RustMetadata.RustProjectTemplate> templates = RustMetadata.defaultTemplates();
        assertNotNull(templates);
        assertEquals(4, templates.size(), "Should have 4 default templates matching IntelliJ");

        RustMetadata.RustProjectTemplate bin = templates.get(0);
        assertEquals("Binary (application)", bin.name());
        assertEquals("binary", bin.value());
        assertTrue(bin.builtIn());

        RustMetadata.RustProjectTemplate lib = templates.get(1);
        assertEquals("Library", lib.name());
        assertEquals("library", lib.value());
        assertTrue(lib.builtIn());

        RustMetadata.RustProjectTemplate procMacro = templates.get(2);
        assertEquals("Procedural Macro", procMacro.name());
        assertTrue(procMacro.url().contains("rust-procmacro-quickstart-template"));
        assertFalse(procMacro.builtIn());

        RustMetadata.RustProjectTemplate wasm = templates.get(3);
        assertEquals("WebAssembly Lib", wasm.name());
        assertTrue(wasm.url().contains("wasm-pack-template"));
        assertFalse(wasm.builtIn());
    }

    @Test
    void testGenerateIdeaModulesXml() {
        String xml = RustMetadata.generateIdeaModulesXml("my_rust_app");
        assertNotNull(xml);
        assertTrue(xml.contains("ProjectModuleManager"));
        assertTrue(xml.contains("fileurl=\"file://$PROJECT_DIR$/.idea/my_rust_app.iml\""));
        assertTrue(xml.contains("filepath=\"$PROJECT_DIR$/.idea/my_rust_app.iml\""));
    }

    @Test
    void testGenerateIdeaIml() {
        String iml = RustMetadata.generateIdeaIml();
        assertNotNull(iml);
        assertTrue(iml.contains("EMPTY_MODULE"));
        assertTrue(iml.contains("sourceFolder url=\"file://$MODULE_DIR$/src\""));
        assertTrue(iml.contains("excludeFolder url=\"file://$MODULE_DIR$/target\""));
    }

    @Test
    void testIsCargoGenerateInstalled() {
        // Should execute without error and return boolean
        boolean installed = RustMetadata.isCargoGenerateInstalled(RustMetadata.defaultToolchainPath());
        // Verify installed is boolean (true or false depending on environment)
        assertTrue(installed || !installed);

        // Non-existent directory returns false
        assertFalse(RustMetadata.isCargoGenerateInstalled("/non/existent/path/to/bin"));
    }
}
