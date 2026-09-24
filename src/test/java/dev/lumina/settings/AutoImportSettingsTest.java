package dev.lumina.settings;

import dev.lumina.semantics.AutoImportService;
import dev.lumina.settings.AutoImportSettings.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class AutoImportSettingsTest {

    private AutoImportSettings settings;

    @BeforeEach
    void setUp() {
        dev.lumina.util.Settings.clear();
        settings = AutoImportSettings.getInstance();
        settings.resetToDefaults();
    }

    @AfterEach
    void tearDown() {
        dev.lumina.util.Settings.clear();
    }

    @Test
    void testDefaultsMatchIntelliJIdea() {
        // XML
        assertTrue(settings.isXmlShowAutoImportTooltip());

        // Java
        assertTrue(settings.isJavaShowTooltipClasses());
        assertTrue(settings.isJavaShowTooltipStaticMethods());
        assertEquals(InsertImportsMode.ALWAYS, settings.getJavaInsertImportsOnPaste());
        assertFalse(settings.isJavaAddUnambiguousImportsOnTheFly());
        assertFalse(settings.isJavaOptimizeImportsOnTheFly());
        assertTrue(settings.getJavaIncludeStaticMembers().isEmpty());
        assertTrue(settings.getJavaExcludeFromAutoImport().isEmpty());

        // Python
        assertFalse(settings.isPythonShowAutoImportTooltip());
        assertEquals(PythonImportStyle.FROM_MODULE_IMPORT_NAME, settings.getPythonPreferredImportStyle());

        // Rust - all 8 default entries from IntelliJ screenshots
        assertFalse(settings.isRustShowImportPopup());
        assertTrue(settings.isRustImportOutOfScopeItems());
        assertTrue(settings.isRustInsertImportsOnPaste());
        assertEquals(InsertImportsMode.ASK, settings.getRustAddCrateDependenciesOnPaste());
        assertFalse(settings.isRustAddUnambiguousImportsOnTheFly());
        assertEquals(8, settings.getRustExcludeEntries().size());
        assertEquals("std::borrow::Borrow", settings.getRustExcludeEntries().get(0).getItemOrModule());
        assertEquals(RustApplyTo.METHODS_ONLY, settings.getRustExcludeEntries().get(0).getApplyTo());
        assertEquals(RustScope.IDE, settings.getRustExcludeEntries().get(0).getScope());

        // Scala
        assertEquals(InsertImportsMode.ASK, settings.getScalaInsertImportsOnPaste());
        assertTrue(settings.isScalaShowPopupClasses());
        assertTrue(settings.isScalaShowPopupStaticMembers());
        assertTrue(settings.isScalaShowPopupImplicitConversions());
        assertTrue(settings.isScalaShowPopupImplicitDefinitions());
        assertTrue(settings.isScalaShowPopupExtensionMethods());
        assertFalse(settings.isScalaAddUnambiguousClasses());
        assertFalse(settings.isScalaAddUnambiguousStaticMembers());
        assertFalse(settings.isScalaOptimizeImportsOnTheFly());

        // JSP
        assertFalse(settings.isJspAddUnambiguousImportsOnTheFly());

        // Kotlin
        assertFalse(settings.isKotlinAddUnambiguousImportsOnTheFly());
        assertFalse(settings.isKotlinOptimizeImportsOnTheFly());

        // Ktor
        assertTrue(settings.isKtorAddImportsAutomatically());

        // TypeScript / JavaScript
        assertTrue(settings.isJsAddImportsAutomatically());
        assertTrue(settings.isJsOnCodeCompletion());
        assertTrue(settings.isJsWithAutoImportTooltip());
        assertTrue(settings.isTsAddImportsAutomatically());
        assertTrue(settings.isTsOnCodeCompletion());
        assertTrue(settings.isTsWithAutoImportTooltip());
        assertFalse(settings.isTsUnambiguousImportsOnTheFly());

        // PHP
        assertEquals(InsertImportsMode.ASK, settings.getPhpInsertImportsOnPaste());
        assertFalse(settings.isPhpEnableInFileScope());
        assertTrue(settings.isPhpEnableInNamespaceScope());
        assertEquals("prefer FQN", settings.getPhpClassGlobalSymbol());
        assertEquals("prefer fallback", settings.getPhpFunctionGlobalSymbol());
        assertEquals("prefer fallback", settings.getPhpConstantGlobalSymbol());
    }

    @Test
    void testPersistenceAndReload() {
        settings.setXmlShowAutoImportTooltip(false);
        settings.setJavaInsertImportsOnPaste(InsertImportsMode.NEVER);
        settings.setJavaAddUnambiguousImportsOnTheFly(true);
        settings.getJavaIncludeStaticMembers().add(new JavaImportEntry("java.util.Objects.requireNonNull", RustScope.IDE));
        settings.getJavaExcludeFromAutoImport().add(new JavaImportEntry("java.awt.*", RustScope.PROJECT));
        settings.setPythonPreferredImportStyle(PythonImportStyle.IMPORT_MODULE_NAME);
        settings.setRustAddCrateDependenciesOnPaste(InsertImportsMode.ALWAYS);
        settings.setPhpClassGlobalSymbol("prefer fallback");

        settings.save();

        AutoImportSettings loaded = new AutoImportSettings();
        assertFalse(loaded.isXmlShowAutoImportTooltip());
        assertEquals(InsertImportsMode.NEVER, loaded.getJavaInsertImportsOnPaste());
        assertTrue(loaded.isJavaAddUnambiguousImportsOnTheFly());
        assertEquals(1, loaded.getJavaIncludeStaticMembers().size());
        assertEquals("java.util.Objects.requireNonNull", loaded.getJavaIncludeStaticMembers().get(0).getItem());
        assertEquals(RustScope.IDE, loaded.getJavaIncludeStaticMembers().get(0).getScope());

        assertEquals(1, loaded.getJavaExcludeFromAutoImport().size());
        assertEquals("java.awt.*", loaded.getJavaExcludeFromAutoImport().get(0).getItem());
        assertEquals(RustScope.PROJECT, loaded.getJavaExcludeFromAutoImport().get(0).getScope());

        assertEquals(PythonImportStyle.IMPORT_MODULE_NAME, loaded.getPythonPreferredImportStyle());
        assertEquals(InsertImportsMode.ALWAYS, loaded.getRustAddCrateDependenciesOnPaste());
        assertEquals("prefer fallback", loaded.getPhpClassGlobalSymbol());
    }

    @Test
    void testIsModifiedDetection() {
        AutoImportSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setXmlShowAutoImportTooltip(!settings.isXmlShowAutoImportTooltip());
        assertTrue(settings.isModified(copy));

        copy.copyFrom(settings);
        assertFalse(settings.isModified(copy));

        copy.setJavaInsertImportsOnPaste(InsertImportsMode.NEVER);
        assertTrue(settings.isModified(copy));

        copy.copyFrom(settings);
        copy.getJavaExcludeFromAutoImport().add(new JavaImportEntry("custom.pkg.*", RustScope.IDE));
        assertTrue(settings.isModified(copy));
    }

    @Test
    void testWildcardMatchingAndExclusions() {
        settings.getJavaExcludeFromAutoImport().add(new JavaImportEntry("java.awt.*", RustScope.IDE));
        settings.getJavaExcludeFromAutoImport().add(new JavaImportEntry("com.example.InternalClass", RustScope.PROJECT));

        assertTrue(settings.isJavaExcluded("java.awt.Button"));
        assertTrue(settings.isJavaExcluded("java.awt.image.BufferedImage"));
        assertTrue(settings.isJavaExcluded("com.example.InternalClass"));
        assertFalse(settings.isJavaExcluded("java.util.List"));

        // Rust exclusions
        assertTrue(settings.isRustExcluded("core::panicking::panic_fmt", false));
        assertTrue(settings.isRustExcluded("core::borrow::BorrowMut", true));
        // Methods only: when isMethod=false, it should not exclude the trait name itself
        assertFalse(settings.isRustExcluded("core::borrow::BorrowMut", false));
    }

    @Test
    void testAutoImportServicePasteResolution() {
        String currentSource = "package com.example;\n\npublic class MyService {\n}\n";
        String pastedCode = "List<String> list = new ArrayList<>();\nMap<String, Path> map = new HashMap<>();";

        List<String> needed = AutoImportService.getInstance().resolvePastedImports(currentSource, pastedCode);
        assertTrue(needed.contains("java.util.List"));
        assertTrue(needed.contains("java.util.ArrayList"));
        assertTrue(needed.contains("java.util.Map"));
        assertTrue(needed.contains("java.util.HashMap"));
        assertTrue(needed.contains("java.nio.file.Path"));

        // If an import is excluded by user configuration, it must NOT be resolved
        settings.getJavaExcludeFromAutoImport().add(new JavaImportEntry("java.util.Map", RustScope.IDE));
        List<String> afterExclusion = AutoImportService.getInstance().resolvePastedImports(currentSource, pastedCode);
        assertFalse(afterExclusion.contains("java.util.Map"));
        assertTrue(afterExclusion.contains("java.util.List"));
    }

    @Test
    void testAutoImportServiceOptimizeImports() {
        String sourceWithMessyImports = """
                package com.example;
                
                import java.util.List;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                import java.util.ArrayList;
                import org.springframework.stereotype.Service;
                import static org.junit.jupiter.api.Assertions.assertTrue;
                import java.util.List;
                
                public class Demo {
                }
                """;

        String optimized = AutoImportService.getInstance().optimizeImports(sourceWithMessyImports);

        // Verify static imports come first
        int idxStatic = optimized.indexOf("import static org.junit.jupiter.api.Assertions.assertEquals;");
        int idxJava = optimized.indexOf("import java.util.ArrayList;");
        int idxOther = optimized.indexOf("import org.springframework.stereotype.Service;");

        assertTrue(idxStatic < idxJava, "Static imports should precede java.* imports");
        assertTrue(idxJava < idxOther, "java.* imports should precede third-party imports");

        // Verify duplicate imports removed
        int firstList = optimized.indexOf("import java.util.List;");
        int secondList = optimized.indexOf("import java.util.List;", firstList + 1);
        assertEquals(-1, secondList, "Duplicate import java.util.List should be removed");
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        AutoImportSettings.Listener listener = s -> notified.set(true);
        settings.addListener(listener);

        settings.setXmlShowAutoImportTooltip(false);
        settings.save();

        assertTrue(notified.get());
        settings.removeListener(listener);
    }

    @Test
    void testRustExclusionsFallbackWhenEmptyOrBlank() {
        // Clear rust exclusions and test that initDefaultRustExclusions repopulates
        settings.getRustExcludeEntries().clear();
        assertTrue(settings.getRustExcludeEntries().isEmpty());
        settings.initDefaultRustExclusions();
        assertEquals(8, settings.getRustExcludeEntries().size());

        // Test loading when property is blank
        dev.lumina.util.Settings.put("editor.autoimport.rust.exclude", "   ");
        AutoImportSettings reloaded = new AutoImportSettings();
        assertEquals(8, reloaded.getRustExcludeEntries().size());
        assertEquals("std::borrow::Borrow", reloaded.getRustExcludeEntries().get(0).getItemOrModule());
    }
}
