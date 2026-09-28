package dev.lumina.ui;

import dev.lumina.settings.AngularHtmlCodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings.CodeStyleScheme;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings.LineSeparator;
import dev.lumina.settings.EditorConfigCodeStyleSettings;
import dev.lumina.settings.ErbCodeStyleSettings;
import dev.lumina.settings.GoCodeStyleSettings;
import dev.lumina.settings.GradleDeclarativeCodeStyleSettings;
import dev.lumina.settings.GroovyCodeStyleSettings;
import dev.lumina.settings.HtmlCodeStyleSettings;
import dev.lumina.settings.HttpRequestCodeStyleSettings;
import dev.lumina.settings.JavaScriptCodeStyleSettings;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SettingsCodeStyleTest {

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

    @Test
    void testCodeStyleSettingsDefaultsAndPersistence() {
        CodeStyleSettings settings = CodeStyleSettings.getInstance();
        assertNotNull(settings);

        CodeStyleScheme active = settings.getActiveScheme();
        assertNotNull(active, "Active code style scheme must exist");
        assertTrue("Default".equals(active.getName()) || "Project".equals(active.getName()));

        assertEquals(120, active.getHardWrapAt(), "Default hard wrap should be 120");
        assertFalse(active.isWrapOnTyping(), "Default wrap on typing should be false");
        assertTrue(active.isDetectAndUseExistingFileIndents(), "Default detect indents should be true");
        assertTrue(active.isEnableEditorConfigSupport(), "Default editorconfig support should be true");
        assertEquals("@formatter:off", active.getFormatterOffMarker());
        assertEquals("@formatter:on", active.getFormatterOnMarker());

        // Modify and test save/load
        int oldHardWrap = active.getHardWrapAt();
        try {
            active.setHardWrapAt(100);
            settings.saveSettings();
            settings.loadSettings();
            assertEquals(100, settings.getActiveScheme().getHardWrapAt());
        } finally {
            active.setHardWrapAt(oldHardWrap);
            settings.saveSettings();
        }
    }

    @Test
    void testLanguageCodeStyleProviders() {
        List<LanguageCodeStyleProvider> providers = LanguageCodeStyleProvider.getAllProviders();
        assertFalse(providers.isEmpty(), "Providers must not be empty");

        // Verify key languages from reference screenshots are present
        List<String> requiredLanguages = List.of(
                "Java", "Kotlin", "Angular HTML template", "EditorConfig", "ERB", "Go",
                "Gradle Declarative Configuration", "Groovy", "HTML", "HTTP Request",
                "JavaScript", "JSON", "JSP", "JSPX", "Markdown", "PHP", "Properties",
                "Protocol Buffer", "Protocol Buffer Text", "Python", "Qute", "Ruby",
                "Rust", "Scala", "Shell Script", "SQL", "TOML", "TypeScript", "XML", "YAML"
        );

        for (String lang : requiredLanguages) {
            LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(lang);
            assertNotNull(provider, "Provider for " + lang + " must be registered");
            assertEquals(lang, provider.getDisplayName());
            assertNotNull(provider.getSampleCode(), "Sample code for " + lang + " must not be null");
            assertFalse(provider.getSupportedTabs().isEmpty(), "Tabs for " + lang + " must not be empty");
        }

        // Verify specific defaults
        LanguageCodeStyleProvider javaProvider = LanguageCodeStyleProvider.getProvider("Java");
        LanguageCodeStyleSettings javaSettings = javaProvider.createDefaultSettings();
        assertEquals(4, javaSettings.getTabSize());
        assertEquals(4, javaSettings.getIndent());
        assertEquals(8, javaSettings.getContinuationIndent());
        assertFalse(javaSettings.isUseTabCharacter());

        LanguageCodeStyleProvider goProvider = LanguageCodeStyleProvider.getProvider("Go");
        LanguageCodeStyleSettings goSettings = goProvider.createDefaultSettings();
        assertTrue(goSettings.isUseTabCharacter(), "Go should use tab characters by default");

        LanguageCodeStyleProvider jsProvider = LanguageCodeStyleProvider.getProvider("JavaScript");
        LanguageCodeStyleSettings jsSettings = jsProvider.createDefaultSettings();
        assertEquals(4, jsSettings.getIndent(), "JavaScript default indent should be 4");
    }

    @Test
    void testCodeSampleDynamicFormatting() {
        LanguageCodeStyleProvider javaProvider = LanguageCodeStyleProvider.getProvider("Java");
        String sample = javaProvider.getSampleCode();

        LanguageCodeStyleSettings settings = javaProvider.createDefaultSettings();
        settings.setIndent(4);
        String formatted4 = CodeStyleSettings.formatCodeSample(sample, settings);
        assertTrue(formatted4.contains("    public int[] X"), "Should have 4 spaces for 1 level indent");
        assertTrue(formatted4.contains("label1:"), "Label should be present");

        // Change indent to 2
        settings.setIndent(2);
        String formatted2 = CodeStyleSettings.formatCodeSample(sample, settings);
        assertTrue(formatted2.contains("  public int[] X"), "Should have 2 spaces for 1 level indent when indent=2");

        // Change to tabs
        settings.setIndent(4);
        settings.setUseTabCharacter(true);
        settings.setTabSize(4);
        String formattedTab = CodeStyleSettings.formatCodeSample(sample, settings);
        assertTrue(formattedTab.contains("\tpublic int[] X"), "Should have tab character when useTabCharacter=true");
    }

    @Test
    void testSchemeDuplicationAndExport() {
        CodeStyleSettings settings = CodeStyleSettings.getInstance();
        CodeStyleScheme custom = settings.duplicateScheme("Default", "Custom Team Scheme");

        assertNotNull(custom);
        assertEquals("Custom Team Scheme", custom.getName());
        assertEquals("Custom Team Scheme", settings.getActiveScheme().getName());

        // Test XML export
        String xml = settings.exportToXml(custom);
        assertNotNull(xml);
        assertTrue(xml.contains("<code_scheme name=\"Custom Team Scheme\""), "XML export must contain scheme name");
        assertTrue(xml.contains("<codeStyleSettings language=\"Java\">"), "XML export must contain Java settings");

        // Test EditorConfig export
        String ec = settings.exportToEditorConfig(custom);
        assertNotNull(ec);
        assertTrue(ec.contains("root = true"), "EditorConfig must contain root = true");
        assertTrue(ec.contains("[*.java]"), "EditorConfig must contain [*.java]");

        // Clean up
        settings.removeScheme("Custom Team Scheme");
        assertEquals("Default", settings.getActiveScheme().getName());
    }

    @Test
    void testSettingsCodeStylePageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeStylePage page = new SettingsCodeStylePage();
                assertNotNull(page);
                assertNotNull(page.getHeaderBar());
                assertFalse(page.isModified());

                // Test modifying settings
                page.getHeaderBar().getSchemeCombo().setValue("Project");
                assertNotNull(CodeStyleSettings.getInstance().getActiveScheme());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsCodeStyleLanguagePageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeStyleLanguagePage page = new SettingsCodeStyleLanguagePage("Java");
                assertNotNull(page);
                assertEquals("Java", page.getLanguageId());
                assertNotNull(page.getHeaderBar());
                assertFalse(page.isModified());

                // Reset and apply
                page.reset();
                page.apply();
                assertFalse(page.isModified());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJavaCodeStyleSettingsModel() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();
        assertEquals(4, js.getTabSize());
        assertEquals(4, js.getIndent());
        assertEquals(8, js.getContinuationIndent());
        assertFalse(js.isUseTabCharacter());
        assertEquals(120, js.getHardWrapAt());

        // Test Google Style preset
        js.applyGoogleStyle();
        assertEquals(2, js.getIndent());
        assertEquals(2, js.getTabSize());
        assertEquals(100, js.getHardWrapAt());
        assertEquals(999, js.getClassCountToUseImportOnDemand());

        // Test Platform Default preset
        js.applyPlatformDefault();
        assertEquals(4, js.getIndent());
        assertEquals(120, js.getHardWrapAt());
        assertEquals(5, js.getClassCountToUseImportOnDemand());

        // Test sync with LanguageCodeStyleSettings
        CodeStyleSettings.LanguageCodeStyleSettings lcs = new CodeStyleSettings.LanguageCodeStyleSettings("Java");
        lcs.setIndent(6);
        lcs.setUseTabCharacter(true);
        js.syncFrom(lcs);
        assertEquals(6, js.getIndent());
        assertTrue(js.isUseTabCharacter());

        js.setIndent(8);
        js.syncTo(lcs);
        assertEquals(8, lcs.getIndent());
    }

    @Test
    void testWrappingAndBlankLinesSettings() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // Blank lines defaults
        assertEquals(2, js.getKeepBlankLinesInDeclarations());
        assertEquals(2, js.getKeepBlankLinesInCode());
        assertEquals(2, js.getKeepBlankLinesBeforeRBrace());
        assertEquals(2, js.getKeepBlankLinesBetweenHeaderAndPackage());
        assertEquals(0, js.getBlankLinesBeforePackage());
        assertEquals(1, js.getBlankLinesAfterPackage());
        assertEquals(1, js.getBlankLinesBeforeImports());
        assertEquals(1, js.getBlankLinesAfterImports());
        assertEquals(1, js.getBlankLinesAroundClass());
        assertEquals(0, js.getBlankLinesAfterClassHeader());
        assertEquals(0, js.getBlankLinesBeforeClassEnd());
        assertEquals(0, js.getBlankLinesAfterAnonymousClassHeader());
        assertEquals(0, js.getBlankLinesBeforeFieldInInterface());
        assertEquals(0, js.getBlankLinesBeforeFieldWithoutAnnotations());
        assertEquals(0, js.getBlankLinesBeforeFieldWithAnnotations());
        assertEquals(1, js.getBlankLinesAroundMethodInInterface());
        assertEquals(1, js.getBlankLinesAroundMethod());
        assertEquals(0, js.getBlankLinesBeforeMethodBody());
        assertEquals(1, js.getBlankLinesAroundInitializer());
        assertEquals(0, js.getBlankLinesBetweenRecordComponents());

        // Wrapping and Braces defaults
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getMethodCallArgumentsWrap());
        assertFalse(js.isAlignMethodCallArguments());
        assertFalse(js.isTakePriorityOverCallChainWrapping());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getChainedMethodCallsWrap());
        assertFalse(js.isWrapFirstCall());
        assertFalse(js.isAlignChainedCallsMultiline());
        assertEquals("Method names", js.getBuilderMethods());
        assertFalse(js.isKeepBuilderMethodsIndents());
        assertFalse(js.isMoveSemicolonToNewLine());

        assertEquals("Do not force", js.getIfForceBraces());
        assertFalse(js.isElseOnNewLine());
        assertTrue(js.isSpecialElseIfTreatment());

        assertEquals("Do not force", js.getForForceBraces());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getForStatementWrap());
        assertTrue(js.isAlignForMultiline());

        assertEquals("Do not force", js.getWhileForceBraces());
        assertEquals("Do not force", js.getDoWhileForceBraces());
        assertFalse(js.isWhileOnNewLine());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_IF_LONG, js.getSwitchWrap());
        assertTrue(js.isIndentCaseBranches());
        assertTrue(js.isEachCaseOnSeparateLine());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getTryWithResourcesWrap());
        assertTrue(js.isAlignTryWithResourcesMultiline());
        assertFalse(js.isCatchOnNewLine());
        assertFalse(js.isFinallyOnNewLine());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getBinaryExpressionsWrap());
        assertFalse(js.isAlignBinaryExpressionsMultiline());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getAssignmentWrap());
        assertFalse(js.isAlignAssignmentMultiline());
        assertFalse(js.isAlignFieldsInColumns());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getTernaryWrap());
        assertFalse(js.isAlignTernaryMultiline());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getArrayInitializerWrap());
        assertFalse(js.isAlignArrayInitializerMultiline());
        assertFalse(js.isWrapAfterModifierList());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getAssertStatementWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getEnumConstantsWrap());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_ALWAYS, js.getClassAnnotationsWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_ALWAYS, js.getMethodAnnotationsWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_ALWAYS, js.getFieldAnnotationsWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getParameterAnnotationsWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getLocalVariableAnnotationsWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getEnumFieldAnnotationsWrap());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getAnnotationParametersWrap());
        assertFalse(js.isAlignAnnotationParametersMultiline());

        assertFalse(js.isAlignTextBlocksMultiline());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_IF_LONG, js.getRecordComponentsWrap());
        assertTrue(js.isAlignRecordComponentsMultiline());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_IF_LONG, js.getDeconstructionPatternsWrap());
        assertTrue(js.isAlignDeconstructionPatternsMultiline());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setBlankLinesAroundClass(3);
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setChainedMethodCallsWrap(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_ALWAYS);
        assertNotEquals(js, copy2);
    }

    @Test
    void testJavaDocAndImportsSettings() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // JavaDoc defaults matching Screenshot 1
        assertTrue(js.isEnableJavaDocFormatting());
        assertTrue(js.isAlignParamDescriptions());
        assertTrue(js.isAlignThrownExceptions());
        assertTrue(js.isBlankLinesAfterDescription());
        assertFalse(js.isBlankLinesAfterParamDescriptions());
        assertFalse(js.isBlankLinesAfterReturnTag());
        assertTrue(js.isKeepInvalidTags());
        assertTrue(js.isKeepEmptyParamTags());
        assertTrue(js.isKeepEmptyReturnTags());
        assertTrue(js.isKeepEmptyThrowsTags());
        assertFalse(js.isWrapAtRightMargin());
        assertTrue(js.isEnableLeadingAsterisks());
        assertTrue(js.isUseThrowsRatherThanException());
        assertTrue(js.isGeneratePOnEmptyLines());
        assertTrue(js.isKeepEmptyLines());
        assertFalse(js.isDoNotWrapOneLineComments());
        assertFalse(js.isPreserveLineFeeds());
        assertFalse(js.isParamDescriptionsOnNewLine());
        assertFalse(js.isIndentContinuationLines());

        // Imports defaults matching Screenshots 2, 3, 4, 5
        assertTrue(js.isUseSingleClassImport());
        assertFalse(js.isUseFullyQualifiedClassNames());
        assertFalse(js.isInsertInnerClassImports());
        assertTrue(js.getExcludedInnerClasses().isEmpty());
        assertTrue(js.isDoNotSeparateModuleImports());
        assertFalse(js.isDeleteUnusedModuleImports());
        assertEquals("If not already imported", js.getUseFqNamesInJavadoc());
        assertEquals(5, js.getClassCountToUseImportOnDemand());
        assertEquals(3, js.getNamesCountToUseStaticImportOnDemand());

        // Packages to Use Import with '*'
        assertNotNull(js.getPackagesToUseImportOnDemand());
        assertEquals(2, js.getPackagesToUseImportOnDemand().size());
        assertEquals("java.awt.*", js.getPackagesToUseImportOnDemand().get(0).getPackageName());
        assertFalse(js.getPackagesToUseImportOnDemand().get(0).isStatic());
        assertFalse(js.getPackagesToUseImportOnDemand().get(0).isWithSubpackages());
        assertEquals("javax.swing.*", js.getPackagesToUseImportOnDemand().get(1).getPackageName());

        // Options
        assertTrue(js.isPlaceOnDemandImportBeforeSingleClassImports());
        assertTrue(js.isLayoutStaticImportsSeparately());

        // Import layout
        assertNotNull(js.getImportLayout());
        assertEquals(7, js.getImportLayout().size());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry.EntryType.MODULE_IMPORTS, js.getImportLayout().get(0).getType());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry.EntryType.ALL_OTHER_IMPORTS, js.getImportLayout().get(1).getType());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry.EntryType.BLANK_LINE, js.getImportLayout().get(2).getType());
        assertEquals("javax.*", js.getImportLayout().get(3).getPackageName());
        assertTrue(js.getImportLayout().get(3).isWithSubpackages());
        assertEquals("java.*", js.getImportLayout().get(4).getPackageName());
        assertTrue(js.getImportLayout().get(4).isWithSubpackages());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry.EntryType.BLANK_LINE, js.getImportLayout().get(5).getType());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry.EntryType.STATIC_ALL_OTHER_IMPORTS, js.getImportLayout().get(6).getType());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setEnableJavaDocFormatting(false);
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setClassCountToUseImportOnDemand(10);
        assertNotEquals(js, copy2);

        dev.lumina.settings.JavaCodeStyleSettings copy3 = js.copy();
        copy3.getPackagesToUseImportOnDemand().add(new dev.lumina.settings.JavaCodeStyleSettings.ImportEntry(false, "org.junit.*", false));
        assertNotEquals(js, copy3);
    }

    @Test
    void testSettingsCodeStyleJavaPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeStyleJavaPage page = new SettingsCodeStyleJavaPage();
                assertNotNull(page);
                assertNotNull(page.getHeaderBar());
                assertEquals("Tabs and Indents", page.getActiveTab());
                assertFalse(page.isModified());

                // Test switching tabs across all 9 Java tabs
                List<String> tabs = List.of(
                        "Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines",
                        "JavaDoc", "Imports", "Arrangement", "Code Generation", "Java EE Names"
                );
                for (String tab : tabs) {
                    page.setActiveTab(tab);
                    assertEquals(tab, page.getActiveTab());
                }

                // Verify Wrapping and Braces tab sample
                page.setActiveTab("Wrapping and Braces");
                assertTrue(page.getSampleForActiveTab().contains("ThisIsASampleClass"));
                assertTrue(page.getSampleForActiveTab().contains("0x0051"));

                // Verify Blank Lines tab sample
                page.setActiveTab("Blank Lines");
                assertTrue(page.getSampleForActiveTab().contains("dev.lumina.samples"));
                assertTrue(page.getSampleForActiveTab().contains("@NotNull"));

                // Modify setting
                page.getCurrentSettings().setIndent(2);
                assertTrue(page.isModified());

                // Reset and apply
                page.reset();
                assertFalse(page.isModified());

                // Modify wrapping setting
                page.getCurrentSettings().setMethodCallArgumentsWrap(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_ALWAYS);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Modify blank lines setting
                page.getCurrentSettings().setBlankLinesAroundClass(3);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify JavaDoc tab sample and modification
                page.setActiveTab("JavaDoc");
                assertTrue(page.getSampleForActiveTab().contains("package sample;"));
                assertTrue(page.getSampleForActiveTab().contains("public class Sample"));
                assertTrue(page.getSampleForActiveTab().contains("@param i"));
                assertTrue(page.getSampleForActiveTab().contains("@throws XXXException"));
                page.getCurrentSettings().setEnableJavaDocFormatting(false);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify Spaces tab and modification
                page.setActiveTab("Spaces");
                assertEquals("Spaces", page.getActiveTab());
                page.getCurrentSettings().setSpaceBeforeMethodDeclParen(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify Imports tab and modification
                page.setActiveTab("Imports");
                assertEquals("Imports", page.getActiveTab());
                page.getCurrentSettings().setClassCountToUseImportOnDemand(8);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify Arrangement tab and modification
                page.setActiveTab("Arrangement");
                assertEquals("Arrangement", page.getActiveTab());
                page.getCurrentSettings().setKeepGettersAndSettersTogether(false);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify Code Generation tab and modification
                page.setActiveTab("Code Generation");
                assertEquals("Code Generation", page.getActiveTab());
                page.getCurrentSettings().setFieldPrefix("m_");
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify Java EE Names tab and modification
                page.setActiveTab("Java EE Names");
                assertEquals("Java EE Names", page.getActiveTab());
                page.getCurrentSettings().setEntityBeanSuffix("EJB");
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsDialogCodeStyleRouting() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null, "Code Style");
                ref.set(dialog);

                assertNotNull(dialog.getCurrentCodeStylePage(), "Code Style page should be loaded");

                // Test selecting Java under Code Style -> Dedicated SettingsCodeStyleJavaPage
                dialog.selectCategory("Java");
                assertNotNull(dialog.getCurrentCodeStyleJavaPage(), "Dedicated Java Code Style page should be loaded for Java");

                // Test selecting Kotlin under Code Style -> Generic SettingsCodeStyleLanguagePage
                dialog.selectCategory("Kotlin");
                assertNotNull(dialog.getCurrentCodeStyleLanguagePage(), "Code Style Language page should be loaded for Kotlin");
                assertEquals("Kotlin", dialog.getCurrentCodeStyleLanguagePage().getLanguageId());

                // Test selecting JavaScript under Code Style -> SettingsCodeStyleLanguagePage (NOT Smart Keys!)
                dialog.selectCategory("JavaScript");
                assertNotNull(dialog.getCurrentCodeStyleLanguagePage(), "Code Style Language page should be loaded for JavaScript");
                assertEquals("JavaScript", dialog.getCurrentCodeStyleLanguagePage().getLanguageId());
                assertNull(dialog.getCurrentSmartKeysJsPage(), "Smart Keys JavaScript page must NOT be loaded when selecting Code Style > JavaScript");

                // Test selecting Smart Keys > JavaScript -> Dedicated SettingsSmartKeysJavaScriptPage
                dialog.selectCategory("Smart Keys", "JavaScript");
                assertNotNull(dialog.getCurrentSmartKeysJsPage(), "Smart Keys JavaScript page should be loaded for Smart Keys > JavaScript");

                // Test selecting Code Style > JavaScript again -> SettingsCodeStyleLanguagePage
                dialog.selectCategory("Code Style", "JavaScript");
                assertNotNull(dialog.getCurrentCodeStyleLanguagePage(), "Code Style Language page should be loaded for Code Style > JavaScript");
                assertEquals("JavaScript", dialog.getCurrentCodeStyleLanguagePage().getLanguageId());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSpacesBeforeParenthesesAndAroundOperators() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // 1. Before parentheses matching reference screenshots 1 & 2
        assertFalse(js.isSpaceBeforeMethodDeclParen());
        assertFalse(js.isSpaceBeforeMethodCallParen());
        assertTrue(js.isSpaceBeforeIfParen());
        assertTrue(js.isSpaceBeforeForParen());
        assertTrue(js.isSpaceBeforeWhileParen());
        assertTrue(js.isSpaceBeforeSwitchParen());
        assertTrue(js.isSpaceBeforeTryParen());
        assertTrue(js.isSpaceBeforeCatchParen());
        assertTrue(js.isSpaceBeforeSynchronizedParen());
        assertFalse(js.isSpaceBeforeAnnotationParens());
        assertFalse(js.isSpaceBeforeDeconstructionList());

        // 2. Around operators matching reference screenshots 1 & 2
        assertTrue(js.isSpaceAroundAssignmentOps());
        assertTrue(js.isSpaceAroundLogicalOps());
        assertTrue(js.isSpaceAroundEqualityOps());
        assertTrue(js.isSpaceAroundRelationalOps());
        assertTrue(js.isSpaceAroundBitwiseOps());
        assertTrue(js.isSpaceAroundAdditiveOps());
        assertTrue(js.isSpaceAroundMultiplicativeOps());
        assertTrue(js.isSpaceAroundShiftOps());
        assertFalse(js.isSpaceAroundUnaryOps());
        assertTrue(js.isSpaceAroundLambdaArrow());
        assertFalse(js.isSpaceAroundMethodRefDoubleColon());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setSpaceBeforeMethodDeclParen(true);
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setSpaceAroundUnaryOps(true);
        assertNotEquals(js, copy2);

        dev.lumina.settings.JavaCodeStyleSettings copy3 = js.copy();
        copy3.setSpaceBeforeSynchronizedParen(false);
        assertNotEquals(js, copy3);
    }

    @Test
    void testArrangementSettings() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // Grouping rules defaults matching reference screenshots
        assertTrue(js.isKeepGettersAndSettersTogether());
        assertFalse(js.isKeepOverriddenMethodsTogether());
        assertEquals("keep order", js.getOverriddenMethodsOrder());
        assertFalse(js.isKeepDependentMethodsTogether());
        assertEquals("breadth-first order", js.getDependentMethodsOrder());

        // Matching rules default count is 26
        List<dev.lumina.settings.JavaCodeStyleSettings.ArrangementRule> rules = js.getMatchingRules();
        assertNotNull(rules);
        assertEquals(26, rules.size());

        // Verify key default rules matching reference screenshots
        assertEquals(List.of("field", "public", "static", "final"), rules.get(0).getTags());
        assertEquals(List.of("field", "protected", "static", "final"), rules.get(1).getTags());
        assertEquals(List.of("field", "package private", "static", "final"), rules.get(2).getTags());
        assertEquals(List.of("field", "private", "static", "final"), rules.get(3).getTags());
        assertEquals(List.of("initializer block", "static"), rules.get(8).getTags());
        assertEquals(List.of("constructor"), rules.get(19).getTags());
        assertEquals(List.of("method", "static"), rules.get(20).getTags());
        assertEquals(List.of("method"), rules.get(21).getTags());
        assertEquals(List.of("enum"), rules.get(22).getTags());
        assertEquals(List.of("interface"), rules.get(23).getTags());
        assertEquals(List.of("class", "static"), rules.get(24).getTags());
        assertEquals(List.of("class"), rules.get(25).getTags());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setKeepGettersAndSettersTogether(false);
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setOverriddenMethodsOrder("order by name");
        assertNotEquals(js, copy2);

        dev.lumina.settings.JavaCodeStyleSettings copy3 = js.copy();
        copy3.getMatchingRules().remove(0);
        assertNotEquals(js, copy3);
    }

    @Test
    void testCodeGenerationSettings() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // Naming prefixes/suffixes defaults matching reference screenshots
        assertTrue(js.isPreferLongerNames());
        assertEquals("", js.getFieldPrefix());
        assertEquals("", js.getFieldSuffix());
        assertEquals("", js.getStaticFieldPrefix());
        assertEquals("", js.getStaticFieldSuffix());
        assertEquals("", js.getParameterPrefix());
        assertEquals("", js.getParameterSuffix());
        assertEquals("", js.getLocalVariablePrefix());
        assertEquals("", js.getLocalVariableSuffix());
        assertEquals("", js.getSubclassPrefix());
        assertEquals("Impl", js.getSubclassSuffix());
        assertEquals("", js.getTestClassPrefix());
        assertEquals("Test", js.getTestClassSuffix());

        // Default visibility
        assertEquals("Public", js.getDefaultVisibility());

        // Variable declaration
        assertFalse(js.isMakeGeneratedLocalsFinal());
        assertFalse(js.isMakeGeneratedParametersFinal());
        assertFalse(js.isUseVarForLocalVariables());

        // Comment code
        assertTrue(js.isLineCommentAtFirstColumn());
        assertFalse(js.isAddSpaceAtLineCommentStart());
        assertFalse(js.isEnforceOnReformat());
        assertTrue(js.isBlockCommentAtFirstColumn());
        assertFalse(js.isAddSpacesAroundBlockComments());

        // Override method signature
        assertTrue(js.isInsertOverride());
        assertFalse(js.isRepeatSynchronized());
        assertNotNull(js.getAnnotationsToCopy());
        assertTrue(js.getAnnotationsToCopy().isEmpty());
        assertFalse(js.isUseExternalAnnotations());
        assertTrue(js.isInsertTypeUseAnnotationsBeforeType());

        // Lambda body
        assertFalse(js.isUseClassIsInstanceAndCast());
        assertTrue(js.isReplaceNullCheckWithObjectsNonNull());
        assertTrue(js.isUseIntegerSumWhenPossible());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setDefaultVisibility("Private");
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setSubclassSuffix("Base");
        assertNotEquals(js, copy2);

        dev.lumina.settings.JavaCodeStyleSettings copy3 = js.copy();
        copy3.setAnnotationsToCopy(List.of("java.lang.Deprecated"));
        assertNotEquals(js, copy3);
    }

    @Test
    void testJavaEeNamesSettings() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // Entity Bean defaults matching reference screenshots
        assertEquals("", js.getEntityEjbClassPrefix());
        assertEquals("Bean", js.getEntityEjbClassSuffix());
        assertEquals("", js.getEntityHomeInterfacePrefix());
        assertEquals("Home", js.getEntityHomeInterfaceSuffix());
        assertEquals("", js.getEntityRemoteInterfacePrefix());
        assertEquals("", js.getEntityRemoteInterfaceSuffix());
        assertEquals("Local", js.getEntityLocalHomeInterfacePrefix());
        assertEquals("Home", js.getEntityLocalHomeInterfaceSuffix());
        assertEquals("Local", js.getEntityLocalInterfacePrefix());
        assertEquals("", js.getEntityLocalInterfaceSuffix());
        assertEquals("", js.getEntityEjbNameTagPrefix());
        assertEquals("EJB", js.getEntityEjbNameTagSuffix());
        assertEquals("", js.getEntityTransferObjectPrefix());
        assertEquals("VO", js.getEntityTransferObjectSuffix());
        assertEquals("java.lang.String", js.getEntityDefaultPkClass());

        // Session Bean
        assertEquals("", js.getSessionEjbClassPrefix());
        assertEquals("Bean", js.getSessionEjbClassSuffix());
        assertEquals("", js.getSessionHomeInterfacePrefix());
        assertEquals("Home", js.getSessionHomeInterfaceSuffix());
        assertEquals("", js.getSessionRemoteInterfacePrefix());
        assertEquals("", js.getSessionRemoteInterfaceSuffix());
        assertEquals("Local", js.getSessionLocalHomeInterfacePrefix());
        assertEquals("Home", js.getSessionLocalHomeInterfaceSuffix());
        assertEquals("Local", js.getSessionLocalInterfacePrefix());
        assertEquals("", js.getSessionLocalInterfaceSuffix());
        assertEquals("", js.getSessionServiceEndpointPrefix());
        assertEquals("Service", js.getSessionServiceEndpointSuffix());
        assertEquals("", js.getSessionEjbNameTagPrefix());
        assertEquals("EJB", js.getSessionEjbNameTagSuffix());

        // Servlet, MDB, Filter, Listener
        assertEquals("", js.getServletClassPrefix());
        assertEquals("", js.getServletClassSuffix());
        assertEquals("", js.getServletNameTagPrefix());
        assertEquals("", js.getServletNameTagSuffix());

        assertEquals("", js.getMdbEjbClassPrefix());
        assertEquals("Bean", js.getMdbEjbClassSuffix());
        assertEquals("", js.getMdbEjbNameTagPrefix());
        assertEquals("EJB", js.getMdbEjbNameTagSuffix());

        assertEquals("", js.getFilterClassPrefix());
        assertEquals("", js.getFilterClassSuffix());
        assertEquals("", js.getFilterNameTagPrefix());
        assertEquals("", js.getFilterNameTagSuffix());

        assertEquals("", js.getListenerClassPrefix());
        assertEquals("", js.getListenerClassSuffix());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setEntityDefaultPkClass("java.lang.Long");
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setSessionEjbClassSuffix("EJB");
        assertNotEquals(js, copy2);
    }

    @Test
    void testKotlinCodeStyleSettingsDefaults() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Kotlin");
        assertNotNull(provider, "Kotlin provider must be registered");
        assertEquals("Kotlin", provider.getDisplayName());

        List<String> tabs = provider.getSupportedTabs();
        assertEquals(List.of(
                "Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines",
                "Imports", "Other", "Code Generation", "Load/Save"
        ), tabs);

        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof dev.lumina.settings.KotlinCodeStyleSettings);
        dev.lumina.settings.KotlinCodeStyleSettings ks = (dev.lumina.settings.KotlinCodeStyleSettings) settings;

        // Tabs and Indents
        assertEquals(4, ks.getTabSize());
        assertEquals(4, ks.getIndent());
        assertEquals(8, ks.getContinuationIndent());
        assertFalse(ks.isUseTabCharacter());
        assertFalse(ks.isSmartTabs());
        assertFalse(ks.isKeepIndentsOnEmptyLines());

        // Spaces - Before parentheses
        assertTrue(ks.isSpaceBeforeIfParentheses());
        assertTrue(ks.isSpaceBeforeForParentheses());
        assertTrue(ks.isSpaceBeforeWhileParentheses());
        assertTrue(ks.isSpaceBeforeCatchParentheses());
        assertTrue(ks.isSpaceBeforeWhenParentheses());

        // Spaces - Around operators
        assertTrue(ks.isSpaceAroundAssignmentOperators());
        assertTrue(ks.isSpaceAroundLogicalOperators());
        assertTrue(ks.isSpaceAroundEqualityOperators());
        assertTrue(ks.isSpaceAroundRelationalOperators());
        assertTrue(ks.isSpaceAroundAdditiveOperators());
        assertTrue(ks.isSpaceAroundMultiplicativeOperators());
        assertFalse(ks.isSpaceAroundUnaryOperators());
        assertFalse(ks.isSpaceAroundRangeOperators());
        assertTrue(ks.isSpaceAroundElvisOperator());

        // Spaces - Other
        assertFalse(ks.isSpaceBeforeComma());
        assertTrue(ks.isSpaceAfterComma());
        assertFalse(ks.isSpaceBeforeColonAfterDeclarationName());
        assertTrue(ks.isSpaceAfterColonBeforeDeclarationType());
        assertTrue(ks.isSpaceBeforeColonInNewTypeDefinition());
        assertTrue(ks.isSpaceAfterColonInNewTypeDefinition());
        assertTrue(ks.isSpaceInSimpleOneLineMethods());
        assertTrue(ks.isSpaceAroundArrowInFunctionTypes());
        assertTrue(ks.isSpaceAroundArrowInWhenClause());
        assertTrue(ks.isSpaceBeforeLambdaArrow());

        // Wrapping and Braces
        assertTrue(ks.isLineBreaksKeepWhenReformatting());
        assertFalse(ks.isCommentAtFirstColumnKeepWhenReformatting());
        assertEquals("Wrap if long", ks.getExtendsListWrap());
        assertFalse(ks.isAlignMultilineExtendsList());
        assertEquals("Chop down if long", ks.getFunctionParametersWrap());
        assertTrue(ks.isAlignMultilineFunctionParameters());
        assertTrue(ks.isNewLineAfterOpenParenFunctionParameters());
        assertTrue(ks.isPlaceCloseParenOnNewLineFunctionParameters());
        assertEquals("Chop down if long", ks.getFunctionArgumentsWrap());
        assertFalse(ks.isAlignMultilineFunctionArguments());
        assertTrue(ks.isNewLineAfterOpenParenFunctionArguments());
        assertTrue(ks.isPlaceCloseParenOnNewLineFunctionArguments());
        assertFalse(ks.isAlignMultilineFunctionParentheses());
        assertEquals("Wrap if long", ks.getChainedCallsWrap());
        assertFalse(ks.isWrapFirstCallChainedCalls());
        assertFalse(ks.isElseOnNewLine());
        assertTrue(ks.isIfCloseParenOnNewLine());
        assertFalse(ks.isWhileOnNewLine());
        assertFalse(ks.isCatchOnNewLine());
        assertFalse(ks.isFinallyOnNewLine());
        assertFalse(ks.isAlignMultilineBinaryExpressions());

        assertEquals("Wrap if long", ks.getAssignmentWrap());
        assertEquals("Do not wrap", ks.getEnumConstantsWrap());
        assertEquals("Wrap always", ks.getClassAnnotationsWrap());
        assertEquals("Wrap always", ks.getFunctionAnnotationsWrap());
        assertEquals("Wrap always", ks.getPropertyAnnotationsWrap());
        assertEquals("Do not wrap", ks.getParameterAnnotationsWrap());
        assertEquals("Do not wrap", ks.getLocalVariableAnnotationsWrap());
        assertEquals("Wrap always", ks.getPropertyContextParametersWrap());
        assertEquals("Wrap always", ks.getFunctionContextParametersWrap());

        assertFalse(ks.isAlignWhenBranchesInColumns());
        assertTrue(ks.isNewLineAfterMultilineWhenEntry());
        assertTrue(ks.isIndentBeforeArrowOnNewLine());
        assertFalse(ks.isPutLeftBraceOnNewLine());
        assertEquals("Wrap if long", ks.getExpressionBodyFunctionsWrap());
        assertEquals("Wrap if long", ks.getElvisExpressionsWrap());

        // Blank lines
        assertEquals(2, ks.getKeepBlankLinesInDeclarations());
        assertEquals(2, ks.getKeepBlankLinesInCode());
        assertEquals(2, ks.getKeepBlankLinesBeforeClosingBrace());
        assertEquals(0, ks.getMinBlankLinesAfterClassHeader());
        assertEquals(0, ks.getMinBlankLinesAroundWhenBranchesWithBraces());
        assertEquals(1, ks.getMinBlankLinesBeforeDeclarationWithCommentOrAnnotation());

        // Copy test
        dev.lumina.settings.KotlinCodeStyleSettings copy = ks.copy();
        assertEquals(ks.getAllProperties(), copy.getAllProperties());
        copy.setSpaceAroundRangeOperators(true);
        assertNotEquals(ks.getAllProperties(), copy.getAllProperties());
    }

    @Test
    void testKotlinCodeStyleSamples() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Kotlin");
        assertNotNull(provider);

        String tabsSample = provider.getSampleCode("Tabs and Indents");
        assertTrue(tabsSample.contains("open class Some"));
        assertTrue(tabsSample.contains("10..<42"));
        assertTrue(tabsSample.contains("bar ?: 12"));

        String spacesSample = provider.getSampleCode("Spaces");
        assertTrue(spacesSample.contains("open class Some"));
        assertTrue(spacesSample.contains("when (test)"));

        String wrapSample = provider.getSampleCode("Wrapping and Braces");
        assertTrue(wrapSample.contains("@Deprecated(\"Foo\")"));
        assertTrue(wrapSample.contains("public class ThisIsASampleClass :"));
        assertTrue(wrapSample.contains("Comparable<*>"));
        assertTrue(wrapSample.contains("fun multilineMethod("));

        String blankLinesSample = provider.getSampleCode("Blank Lines");
        assertTrue(blankLinesSample.contains("class Foo"));
        assertTrue(blankLinesSample.contains("private var field1: Int = 1"));
        assertTrue(blankLinesSample.contains("when (field1)"));

        String importsSample = provider.getSampleCode("Imports");
        assertTrue(importsSample.contains("package dev.lumina.demo"));
        assertTrue(importsSample.contains("import java.util.List"));
    }

    @Test
    void testDynamicSettingsCodeStyleLanguagePageKotlinUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeStyleLanguagePage page = new SettingsCodeStyleLanguagePage("Kotlin");
                assertNotNull(page);
                assertEquals("Kotlin", page.getLanguageId());
                assertEquals("Tabs and Indents", page.getActiveTab());
                assertFalse(page.isModified());

                // Test switching tabs across all 8 Kotlin tabs
                List<String> tabs = List.of(
                        "Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines",
                        "Imports", "Other", "Code Generation", "Load/Save"
                );
                for (String tab : tabs) {
                    page.setActiveTab(tab);
                    assertEquals(tab, page.getActiveTab());
                }

                // Verify samples per tab
                page.setActiveTab("Wrapping and Braces");
                assertTrue(page.getSampleForActiveTab().contains("ThisIsASampleClass"));

                page.setActiveTab("Blank Lines");
                assertTrue(page.getSampleForActiveTab().contains("class Foo"));

                // Test modification of standard indent
                page.setActiveTab("Tabs and Indents");
                page.getCurrentSettings().setIndent(2);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Test modification of dynamic spaces property
                page.setActiveTab("Spaces");
                page.getCurrentSettings().setBoolean(dev.lumina.settings.KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Test modification of wrapping combo
                page.setActiveTab("Wrapping and Braces");
                page.getCurrentSettings().setString(dev.lumina.settings.KotlinCodeStyleSettings.WRAP_FUNCTION_PARAMETERS, "Wrap always");
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Test modification of blank lines number
                page.setActiveTab("Blank Lines");
                page.getCurrentSettings().setInt(dev.lumina.settings.KotlinCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, 4);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testKotlinDynamicPersistence() {
        CodeStyleSettings settings = CodeStyleSettings.getInstance();
        CodeStyleScheme scheme = settings.getActiveScheme();
        assertNotNull(scheme);

        LanguageCodeStyleSettings langSettings = scheme.getLanguageSettings("Kotlin");
        assertNotNull(langSettings);

        boolean oldRange = langSettings.getBoolean(dev.lumina.settings.KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, false);
        int oldBlankLines = langSettings.getInt(dev.lumina.settings.KotlinCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, 2);

        try {
            langSettings.setBoolean(dev.lumina.settings.KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, true);
            langSettings.setInt(dev.lumina.settings.KotlinCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, 3);
            settings.saveSettings();

            // Reload and verify
            settings.loadSettings();
            LanguageCodeStyleSettings reloaded = settings.getActiveScheme().getLanguageSettings("Kotlin");
            assertTrue(reloaded.getBoolean(dev.lumina.settings.KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, false));
            assertEquals(3, reloaded.getInt(dev.lumina.settings.KotlinCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, 2));
        } finally {
            langSettings.setBoolean(dev.lumina.settings.KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, oldRange);
            langSettings.setInt(dev.lumina.settings.KotlinCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, oldBlankLines);
            settings.saveSettings();
        }
    }

    @Test
    void testKotlinTabsDynamicPreviewVisibility() throws Exception {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Kotlin");
        assertNotNull(provider);

        // Preview presence: Tabs 1-4 true, Tabs 5-8 false
        assertTrue(provider.hasPreview("Tabs and Indents"));
        assertTrue(provider.hasPreview("Spaces"));
        assertTrue(provider.hasPreview("Wrapping and Braces"));
        assertTrue(provider.hasPreview("Blank Lines"));
        assertFalse(provider.hasPreview("Imports"));
        assertFalse(provider.hasPreview("Other"));
        assertFalse(provider.hasPreview("Code Generation"));
        assertFalse(provider.hasPreview("Load/Save"));

        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeStyleLanguagePage page = new SettingsCodeStyleLanguagePage("Kotlin");

                // Tab 1: preview visible
                page.setActiveTab("Tabs and Indents");
                assertEquals("Tabs and Indents", page.getActiveTab());

                // Tab 5: Imports - full width options, preview pane removed
                page.setActiveTab("Imports");
                assertEquals("Imports", page.getActiveTab());

                // Tab 6: Other
                page.setActiveTab("Other");
                assertEquals("Other", page.getActiveTab());

                // Tab 7: Code Generation
                page.setActiveTab("Code Generation");
                assertEquals("Code Generation", page.getActiveTab());

                // Tab 8: Load/Save
                page.setActiveTab("Load/Save");
                assertEquals("Load/Save", page.getActiveTab());

                // Back to Spaces: preview pane restored
                page.setActiveTab("Spaces");
                assertEquals("Spaces", page.getActiveTab());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testKotlinImportsAndTrailingCommaAndCodeGenOptions() {
        dev.lumina.settings.KotlinCodeStyleSettings ks = new dev.lumina.settings.KotlinCodeStyleSettings();

        // Imports
        assertEquals("WHEN_AT_LEAST", ks.getTopLevelImportMode());
        assertEquals(5, ks.getTopLevelImportThreshold());
        assertEquals("WHEN_AT_LEAST", ks.getJavaStaticsImportMode());
        assertEquals(3, ks.getJavaStaticsImportThreshold());
        assertFalse(ks.isInsertImportsForNestedClasses());
        assertTrue(ks.isImportAliasesSeparately());

        assertFalse(ks.getPackagesToUseImportOnDemand().isEmpty());
        assertEquals(3, ks.getPackagesToUseImportOnDemand().size());
        assertEquals("import java.util.*", ks.getPackagesToUseImportOnDemand().get(0).getPackageName());
        assertFalse(ks.getPackagesToUseImportOnDemand().get(0).isWithSubpackages());
        assertTrue(ks.getPackagesToUseImportOnDemand().get(1).isWithSubpackages());

        assertFalse(ks.getImportLayout().isEmpty());
        assertEquals(3, ks.getImportLayout().size());
        assertEquals("import javax.*", ks.getImportLayout().get(0).getPackageName());
        assertTrue(ks.getImportLayout().get(0).isWithSubpackages());
        assertEquals("import all alias imports", ks.getImportLayout().get(2).getPackageName());

        // Other (Trailing comma)
        assertFalse(ks.isTrailingCommaEnabled());
        assertTrue(ks.isTrailingCommaTypeParameterList());
        assertFalse(ks.isTrailingCommaDestructuringDeclaration());
        assertTrue(ks.isTrailingCommaWhenEntry());
        assertTrue(ks.isTrailingCommaFunctionLiteral());
        assertTrue(ks.isTrailingCommaValueParameterList());
        assertTrue(ks.isTrailingCommaContextReceiverList());
        assertFalse(ks.isTrailingCommaCollectionLiteralExpression());
        assertFalse(ks.isTrailingCommaTypeArgumentList());
        assertFalse(ks.isTrailingCommaIndices());
        assertFalse(ks.isTrailingCommaValueArgumentList());

        // Code Generation
        assertTrue(ks.isLineCommentAtFirstColumn());
        assertFalse(ks.isAddSpaceAtLineCommentStart());
        assertFalse(ks.isEnforceOnReformat());
        assertTrue(ks.isBlockCommentAtFirstColumn());
        assertFalse(ks.isAddSpacesAroundBlockComments());

        // Load/Save
        assertEquals("<ide defaults>", ks.getString(dev.lumina.settings.KotlinCodeStyleSettings.LOAD_SAVE_USE_DEFAULTS_FROM, "<ide defaults>"));
        ks.applyKotlinCodingConventions();
        assertEquals(4, ks.getContinuationIndent());
        assertEquals("Kotlin Coding Conventions", ks.getString(dev.lumina.settings.KotlinCodeStyleSettings.LOAD_SAVE_USE_DEFAULTS_FROM, ""));

        ks.applyIdeDefaults();
        assertEquals(8, ks.getContinuationIndent());
        assertEquals("<ide defaults>", ks.getString(dev.lumina.settings.KotlinCodeStyleSettings.LOAD_SAVE_USE_DEFAULTS_FROM, ""));
    }

    @Test
    void testAngularHtmlCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Angular HTML template");
        assertNotNull(provider, "Angular HTML template provider must be registered");
        assertEquals("Angular HTML template", provider.getLanguageId());
        assertEquals("Angular HTML template", provider.getDisplayName());

        assertEquals(List.of("Spaces", "Wrapping and Braces", "Arrangement"), provider.getSupportedTabs());
        assertTrue(provider.hasPreview("Spaces"));
        assertTrue(provider.hasPreview("Wrapping and Braces"));
        assertFalse(provider.hasPreview("Arrangement"), "Arrangement tab must have no preview pane");

        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof AngularHtmlCodeStyleSettings);
        AngularHtmlCodeStyleSettings as = (AngularHtmlCodeStyleSettings) settings;

        // Tabs and indents defaults
        assertEquals(2, as.getTabSize());
        assertEquals(2, as.getIndent());
        assertEquals(4, as.getContinuationIndent());
        assertFalse(as.isUseTabCharacter());

        // Spaces tab defaults (media_1790574549424.png)
        assertTrue(as.isSpacesWithinInterpolations());

        // Wrapping and Braces defaults (media_1790574558704.png)
        assertEquals("Default: None", as.getVisualGuides());
        assertEquals("Do not wrap", as.getInterpolationsWrap());
        assertTrue(as.isNewLineAfterOpenInterpolation());
        assertTrue(as.isNewLineBeforeCloseInterpolation());

        // Arrangement tab defaults (media_1790574570361.png)
        assertEquals(List.of("attribute"), as.getMatchingRules());

        // Copy independence
        AngularHtmlCodeStyleSettings copy = as.copy();
        assertEquals(as.getMatchingRules(), copy.getMatchingRules());
        copy.getMatchingRules().add("tag");
        assertNotEquals(as.getMatchingRules(), copy.getMatchingRules());
    }

    @Test
    void testEditorConfigCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("EditorConfig");
        assertNotNull(provider, "EditorConfig provider must be registered");
        assertEquals("EditorConfig", provider.getLanguageId());
        assertEquals("EditorConfig", provider.getDisplayName());

        assertEquals(List.of("Spaces", "Wrapping and Braces"), provider.getSupportedTabs());
        assertTrue(provider.hasPreview("Spaces"));
        assertTrue(provider.hasPreview("Wrapping and Braces"));

        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof EditorConfigCodeStyleSettings);
        EditorConfigCodeStyleSettings es = (EditorConfigCodeStyleSettings) settings;

        // Tabs and indents defaults
        assertEquals(4, es.getTabSize());
        assertEquals(4, es.getIndent());
        assertEquals(8, es.getContinuationIndent());
        assertFalse(es.isUseTabCharacter());

        // Spaces tab defaults (media_1790574580900.png)
        assertTrue(es.isSpacesAroundSeparator());
        assertFalse(es.isSpacesBeforeColon());
        assertFalse(es.isSpacesAfterColon());
        assertFalse(es.isSpacesBeforeComma());
        assertTrue(es.isSpacesAfterComma());

        // Wrapping and Braces defaults (media_1790574588939.png)
        assertEquals("Default: None", es.getVisualGuides());
        assertFalse(es.isAlignFieldsInColumns());

        // Option groups and right-aligned checkbox verification
        List<CodeStyleSettings.CodeStyleGroup> wrappingGroups = provider.getOptionGroups("Wrapping and Braces");
        assertNotNull(wrappingGroups);
        boolean foundAlignFields = false;
        for (CodeStyleSettings.CodeStyleGroup g : wrappingGroups) {
            for (CodeStyleSettings.CodeStyleOption opt : g.getOptions()) {
                if (EditorConfigCodeStyleSettings.WRAP_ALIGN_FIELDS_IN_COLUMNS.equals(opt.getKey())) {
                    assertTrue(opt.isRightAligned(), "Align fields in columns must be right aligned");
                    foundAlignFields = true;
                }
            }
        }
        assertTrue(foundAlignFields, "Align fields in columns option must be present in Wrapping and Braces");

        // Copy independence
        EditorConfigCodeStyleSettings copy = es.copy();
        assertEquals(es.isAlignFieldsInColumns(), copy.isAlignFieldsInColumns());
        copy.setAlignFieldsInColumns(true);
        assertNotEquals(es.isAlignFieldsInColumns(), copy.isAlignFieldsInColumns());
    }

    @Test
    void testAngularHtmlAndEditorConfigLanguagePages() {
        if (!javaFxAvailable) {
            System.out.println("JavaFX not available, skipping testAngularHtmlAndEditorConfigLanguagePages");
            return;
        }

        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // Test Angular HTML template page
                SettingsCodeStyleLanguagePage angularPage = new SettingsCodeStyleLanguagePage("Angular HTML template");
                assertNotNull(angularPage);
                assertEquals("Spaces", angularPage.getActiveTab(), "Angular HTML template must default to Spaces tab");

                angularPage.setActiveTab("Wrapping and Braces");
                assertEquals("Wrapping and Braces", angularPage.getActiveTab());

                angularPage.setActiveTab("Arrangement");
                assertEquals("Arrangement", angularPage.getActiveTab());
                assertFalse(angularPage.isModified());

                // Test EditorConfig page
                SettingsCodeStyleLanguagePage editorConfigPage = new SettingsCodeStyleLanguagePage("EditorConfig");
                assertNotNull(editorConfigPage);
                assertEquals("Spaces", editorConfigPage.getActiveTab(), "EditorConfig must default to Spaces tab");

                editorConfigPage.setActiveTab("Wrapping and Braces");
                assertEquals("Wrapping and Braces", editorConfigPage.getActiveTab());
                assertFalse(editorConfigPage.isModified());

            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Language pages test timed out");
        } catch (InterruptedException e) {
            fail("Language pages test interrupted");
        }

        if (error.get() != null) {
            fail("Exception in JavaFX thread: " + error.get().getMessage(), error.get());
        }
    }

    @Test
    void testErbCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("ERB");
        assertNotNull(provider, "ERB provider must be registered");
        assertEquals("ERB", provider.getLanguageId());
        assertEquals("ERB", provider.getDisplayName());

        assertEquals(List.of("Tabs and Indents"), provider.getSupportedTabs());
        assertTrue(provider.hasPreview("Tabs and Indents"));

        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof ErbCodeStyleSettings);
        ErbCodeStyleSettings es = (ErbCodeStyleSettings) settings;

        // Tabs and indents defaults (media_1790578204402.png)
        assertEquals(2, es.getTabSize());
        assertEquals(2, es.getIndent());
        assertEquals(2, es.getContinuationIndent(), "ERB continuation indent must be 2");
        assertFalse(es.isUseTabCharacter());
        assertFalse(es.isSmartTabs());
        assertFalse(es.isKeepIndentsOnEmptyLines());

        // Sample code
        assertEquals(ErbCodeStyleSettings.SAMPLE_ERB, provider.getSampleCode());

        // Copy independence
        ErbCodeStyleSettings copy = es.copy();
        assertEquals(es.getContinuationIndent(), copy.getContinuationIndent());
        copy.setContinuationIndent(4);
        assertNotEquals(es.getContinuationIndent(), copy.getContinuationIndent());
    }

    @Test
    void testGoCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Go");
        assertNotNull(provider, "Go provider must be registered");
        assertEquals("Go", provider.getLanguageId());
        assertEquals("Go", provider.getDisplayName());

        assertEquals(List.of("Tabs and Indents", "Wrapping and Braces", "Imports", "Other"), provider.getSupportedTabs());
        assertTrue(provider.hasPreview("Tabs and Indents"));
        assertTrue(provider.hasPreview("Wrapping and Braces"));
        assertTrue(provider.hasPreview("Imports"));
        assertTrue(provider.hasPreview("Other"));

        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof GoCodeStyleSettings);
        GoCodeStyleSettings gs = (GoCodeStyleSettings) settings;

        // Tabs and indents defaults (media_1790578230039.png)
        assertEquals(4, gs.getTabSize());
        assertEquals(4, gs.getIndent());
        assertEquals(4, gs.getContinuationIndent(), "Go continuation indent must be 4");
        assertTrue(gs.isUseTabCharacter(), "Go must default to using tab character");
        assertFalse(gs.isSmartTabs());
        assertFalse(gs.isKeepIndentsOnEmptyLines());

        // Wrapping and Braces defaults (media_1790578216851.png)
        assertEquals("Default: 120", gs.getString(GoCodeStyleSettings.WRAP_HARD_WRAP_AT, ""));
        assertEquals("Default: No", gs.getString(GoCodeStyleSettings.WRAP_ON_TYPING, ""));
        assertEquals("Default: None", gs.getString(GoCodeStyleSettings.WRAP_VISUAL_GUIDES, ""));
        assertEquals("Do not wrap", gs.getString(GoCodeStyleSettings.WRAP_CALL_ARGUMENTS, ""));
        assertTrue(gs.getBoolean(GoCodeStyleSettings.WRAP_CALL_ARGUMENTS_NEW_LINE_AFTER_LPAREN, false));
        assertTrue(gs.getBoolean(GoCodeStyleSettings.WRAP_CALL_ARGUMENTS_RPAREN_ON_NEW_LINE, false));
        assertEquals("Do not wrap", gs.getString(GoCodeStyleSettings.WRAP_COMPOSITE_LITERALS, ""));
        assertTrue(gs.getBoolean(GoCodeStyleSettings.WRAP_COMPOSITE_LITERALS_NEW_LINE_AFTER_LBRACE, false));
        assertTrue(gs.getBoolean(GoCodeStyleSettings.WRAP_COMPOSITE_LITERALS_RBRACE_ON_NEW_LINE, false));
        assertEquals("Do not wrap", gs.getString(GoCodeStyleSettings.WRAP_FUNCTION_PARAMETERS, ""));
        assertTrue(gs.getBoolean(GoCodeStyleSettings.WRAP_FUNCTION_PARAMETERS_NEW_LINE_AFTER_LPAREN, false));
        assertTrue(gs.getBoolean(GoCodeStyleSettings.WRAP_FUNCTION_PARAMETERS_RPAREN_ON_NEW_LINE, false));
        assertEquals("Do not wrap", gs.getString(GoCodeStyleSettings.WRAP_FUNCTION_RESULT_PARAMETERS, ""));
        assertTrue(gs.getBoolean(GoCodeStyleSettings.WRAP_FUNCTION_RESULT_PARAMETERS_NEW_LINE_AFTER_LPAREN, false));
        assertTrue(gs.getBoolean(GoCodeStyleSettings.WRAP_FUNCTION_RESULT_PARAMETERS_RPAREN_ON_NEW_LINE, false));

        // Imports defaults (media_1790578241168.png)
        assertFalse(gs.getBoolean(GoCodeStyleSettings.IMPORTS_USE_BACKQUOTES, true));
        assertFalse(gs.getBoolean(GoCodeStyleSettings.IMPORTS_ADD_PARENTHESES_SINGLE, true));
        assertFalse(gs.getBoolean(GoCodeStyleSettings.IMPORTS_REMOVE_REDUNDANT_ALIASES, true));
        assertEquals("goimports", gs.getString(GoCodeStyleSettings.IMPORTS_SORTING_TYPE, ""));
        assertFalse(gs.getBoolean(GoCodeStyleSettings.IMPORTS_MOVE_ALL_SINGLE_DECLARATION, true));
        assertFalse(gs.getBoolean(GoCodeStyleSettings.IMPORTS_GROUP_SDK_PACKAGES, true));
        assertFalse(gs.getBoolean(GoCodeStyleSettings.IMPORTS_MOVE_ALL_SINGLE_GROUP, true));
        assertFalse(gs.getBoolean(GoCodeStyleSettings.IMPORTS_GROUP_ENABLED, true));
        assertEquals("PROJECT", gs.getString(GoCodeStyleSettings.IMPORTS_GROUP_MODE, ""));

        // Other defaults (media_1790578252165.png)
        assertFalse(gs.getBoolean(GoCodeStyleSettings.OTHER_ADD_LEADING_SPACE_COMMENTS, true));
        assertEquals(80, gs.getInt(GoCodeStyleSettings.OTHER_COLUMN_WIDTH_FILL_PARAGRAPH, 0));
        assertTrue(gs.getBoolean(GoCodeStyleSettings.OTHER_RUN_GOFMT_ON_REFORMAT, false));
        assertTrue(gs.getCommentExceptions().isEmpty());

        // Sample codes per tab
        assertEquals(GoCodeStyleSettings.SAMPLE_TABS_AND_INDENTS, provider.getSampleCode("Tabs and Indents"));
        assertEquals(GoCodeStyleSettings.SAMPLE_WRAPPING_AND_BRACES, provider.getSampleCode("Wrapping and Braces"));
        assertEquals(GoCodeStyleSettings.SAMPLE_IMPORTS, provider.getSampleCode("Imports"));
        assertEquals(GoCodeStyleSettings.SAMPLE_OTHER, provider.getSampleCode("Other"));

        // Copy independence
        GoCodeStyleSettings copy = gs.copy();
        assertEquals(gs.getCommentExceptions(), copy.getCommentExceptions());
        copy.getCommentExceptions().add("//custom");
        assertNotEquals(gs.getCommentExceptions(), copy.getCommentExceptions());
    }

    @Test
    void testErbAndGoLanguagePages() {
        if (!javaFxAvailable) {
            System.out.println("JavaFX not available, skipping testErbAndGoLanguagePages");
            return;
        }

        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // Test ERB page
                SettingsCodeStyleLanguagePage erbPage = new SettingsCodeStyleLanguagePage("ERB");
                assertNotNull(erbPage);
                assertEquals("Tabs and Indents", erbPage.getActiveTab(), "ERB must have Tabs and Indents as active tab");
                assertFalse(erbPage.isModified());

                // Test Go page
                SettingsCodeStyleLanguagePage goPage = new SettingsCodeStyleLanguagePage("Go");
                assertNotNull(goPage);
                assertEquals("Tabs and Indents", goPage.getActiveTab(), "Go must default to Tabs and Indents tab");

                goPage.setActiveTab("Wrapping and Braces");
                assertEquals("Wrapping and Braces", goPage.getActiveTab());

                goPage.setActiveTab("Imports");
                assertEquals("Imports", goPage.getActiveTab());

                goPage.setActiveTab("Other");
                assertEquals("Other", goPage.getActiveTab());
                assertFalse(goPage.isModified());

            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Language pages test timed out");
        } catch (InterruptedException e) {
            fail("Language pages test interrupted");
        }

        if (error.get() != null) {
            fail("Exception in JavaFX thread: " + error.get().getMessage(), error.get());
        }
    }

    @Test
    void testGradleDeclarativeCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Gradle Declarative Configuration");
        assertNotNull(provider, "Gradle Declarative provider must be registered");
        assertEquals("Gradle Declarative Configuration", provider.getLanguageId());
        assertEquals("Gradle Declarative Configuration", provider.getDisplayName());
        assertEquals(List.of("Tabs and Indents"), provider.getSupportedTabs(), "Must support exactly 1 tab: Tabs and Indents");
        assertTrue(provider.hasPreview("Tabs and Indents"));

        LanguageCodeStyleSettings defaultSettings = provider.createDefaultSettings();
        assertTrue(defaultSettings instanceof GradleDeclarativeCodeStyleSettings);
        GradleDeclarativeCodeStyleSettings gds = (GradleDeclarativeCodeStyleSettings) defaultSettings;

        // Tabs and Indents defaults (media_1790578358987.png)
        assertEquals(4, gds.getTabSize());
        assertEquals(4, gds.getIndent());
        assertEquals(8, gds.getContinuationIndent());
        assertFalse(gds.isUseTabCharacter());
        assertFalse(gds.isSmartTabs());
        assertFalse(gds.isKeepIndentsOnEmptyLines());

        // Sample code
        assertEquals(GradleDeclarativeCodeStyleSettings.SAMPLE_GRADLE_DECLARATIVE, provider.getSampleCode("Tabs and Indents"));

        // Copy independence
        GradleDeclarativeCodeStyleSettings copy = gds.copy();
        assertEquals(gds.getTabSize(), copy.getTabSize());
        copy.setTabSize(2);
        assertNotEquals(gds.getTabSize(), copy.getTabSize());
    }

    @Test
    void testGroovyCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Groovy");
        assertNotNull(provider, "Groovy provider must be registered");
        assertEquals("Groovy", provider.getLanguageId());
        assertEquals("Groovy", provider.getDisplayName());
        assertEquals(List.of("Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines", "GroovyDoc", "Imports", "Code Generation"),
                provider.getSupportedTabs(), "Must support exactly 7 tabs matching reference screenshots");
        assertTrue(provider.hasPreview("Tabs and Indents"));
        assertTrue(provider.hasPreview("Spaces"));

        LanguageCodeStyleSettings defaultSettings = provider.createDefaultSettings();
        assertTrue(defaultSettings instanceof GroovyCodeStyleSettings);
        GroovyCodeStyleSettings gs = (GroovyCodeStyleSettings) defaultSettings;

        // Tabs and Indents defaults (media_1790578371136.png)
        assertEquals(4, gs.getTabSize());
        assertEquals(4, gs.getIndent());
        assertEquals(8, gs.getContinuationIndent());
        assertFalse(gs.isUseTabCharacter());
        assertFalse(gs.isSmartTabs());
        assertFalse(gs.isKeepIndentsOnEmptyLines());
        assertEquals(0, gs.getLabelIndent());
        assertEquals("Indent statements after label", gs.getString(GroovyCodeStyleSettings.LABEL_INDENT_STYLE, ""));

        // Spaces - Before parentheses (media_1790578385278.png)
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_METHOD_DECLARATION_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_EMPTY_METHOD_CALL_PARENTHESES, true));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_IF_PARENTHESES, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_FOR_PARENTHESES, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_WHILE_PARENTHESES, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_SWITCH_PARENTHESES, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_TRY_PARENTHESES, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_CATCH_PARENTHESES, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_SYNCHRONIZED_PARENTHESES, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_TYPE_CAST_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_ANNOTATION_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_LIST_AND_MAPS_LITERALS, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_GSTRING_INJECTION_BRACES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_TUPLE_ASSIGNMENT, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_RECORD_PARAMETER_LIST, true));

        // Spaces - Around operators (media_1790578385278.png)
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_LOGICAL_OPERATORS, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_EQUALITY_OPERATORS, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_RELATIONAL_OPERATORS, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_BITWISE_OPERATORS, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_ADDITIVE_OPERATORS, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_SHIFT_OPERATORS, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_LAMBDA_ARROW, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_REGEXP_OPERATORS, false));

        // Spaces - Before left brace (media_1790578385278.png, media_1790578412189.png)
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_CLASS_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_METHOD_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_IF_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_ELSE_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_FOR_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_WHILE_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_DO_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_SWITCH_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_TRY_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_CATCH_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_FINALLY_LBRACE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_SYNCHRONIZED_LBRACE, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_ARRAY_INITIALIZER_LBRACE, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_CLOSURE_LBRACE_IN_CALLS, true));

        // Spaces - Before keywords (media_1790578412189.png)
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_ELSE_KEYWORD, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_WHILE_KEYWORD, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_CATCH_KEYWORD, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_FINALLY_KEYWORD, false));

        // Spaces - Within (media_1790578412189.png)
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_CODE_BRACES, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_BRACKETS, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_ARRAY_INITIALIZER_BRACES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_EMPTY_ARRAY_INITIALIZER_BRACES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_GROUPING_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_METHOD_DECLARATION_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_METHOD_CALL_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_EMPTY_METHOD_CALL_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_IF_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_FOR_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_WHILE_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_SWITCH_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_TRY_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_CATCH_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_SYNCHRONIZED_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_TYPE_CAST_PARENTHESES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_WITHIN_ANNOTATION_PARENTHESES, true));

        // Spaces - In ternary operator (media_1790578441103.png)
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_TERNARY_QUESTION, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AFTER_TERNARY_QUESTION, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_TERNARY_COLON, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AFTER_TERNARY_COLON, false));

        // Spaces - Within type arguments (media_1790578441103.png)
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS, false));

        // Spaces - Other (media_1790578441103.png)
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_COMMA, true));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AFTER_COMMA, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_FOR_SEMICOLON, true));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AFTER_FOR_SEMICOLON, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AFTER_TYPE_CAST, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_IN_NAMED_ARGUMENT_BEFORE_COLON, true));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_IN_NAMED_ARGUMENT_AFTER_COLON, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_ASSERT_SEPARATOR, true));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.SPACE_AFTER_ASSERT_SEPARATOR, false));

        // Wrapping and Braces defaults (media_1790578808143.png, media_1790578849775.png, media_1790578902174.png, media_1790578944643.png)
        assertEquals(120, gs.getInt(GroovyCodeStyleSettings.HARD_WRAP_AT, 0));
        assertEquals("Default: No", gs.getString(GroovyCodeStyleSettings.WRAP_ON_TYPING, ""));
        assertEquals("Default: None", gs.getString(GroovyCodeStyleSettings.VISUAL_GUIDES, ""));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_KEEP_LINE_BREAKS, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_KEEP_COMMENT_AT_FIRST_COLUMN, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_KEEP_CONTROL_STATEMENT_IN_ONE_LINE, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_KEEP_SIMPLE_BLOCKS_IN_ONE_LINE, true));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_KEEP_SIMPLE_METHODS_IN_ONE_LINE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_KEEP_SIMPLE_CLASSES_IN_ONE_LINE, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ENSURE_RIGHT_MARGIN_NOT_EXCEEDED, true));
        assertEquals("End of line", gs.getString(GroovyCodeStyleSettings.BRACE_PLACEMENT_CLASS, ""));
        assertEquals("End of line", gs.getString(GroovyCodeStyleSettings.BRACE_PLACEMENT_METHOD, ""));
        assertEquals("End of line", gs.getString(GroovyCodeStyleSettings.BRACE_PLACEMENT_LAMBDA, ""));
        assertEquals("End of line", gs.getString(GroovyCodeStyleSettings.BRACE_PLACEMENT_OTHER, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.BRACE_USE_FLYING_GEESE, true));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_EXTENDS_LIST, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_EXTENDS_LIST, true));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_EXTENDS_KEYWORD, ""));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_THROWS_LIST, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_THROWS_LIST, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_THROWS_TO_METHOD_START, true));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_THROWS_KEYWORD, ""));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_METHOD_PARAMETERS, ""));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_METHOD_PARAMETERS, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_NEW_LINE_AFTER_LPAREN_METHOD_PARAMETERS, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_PLACE_RPAREN_ON_NEW_LINE_METHOD_PARAMETERS, true));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_METHOD_ARGUMENTS, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_METHOD_ARGUMENTS, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_METHOD_ARGUMENTS_TAKE_PRIORITY_OVER_CALL_CHAIN, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_NEW_LINE_AFTER_LPAREN_METHOD_ARGUMENTS, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_PLACE_RPAREN_ON_NEW_LINE_METHOD_ARGUMENTS, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_METHOD_PARENTHESES, true));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_CHAINED_CALLS, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_CHAINED_CALLS, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_CHAINED_CALLS_WRAP_AFTER_DOT, true));
        assertEquals("Do not force", gs.getString(GroovyCodeStyleSettings.WRAP_IF_FORCE_BRACES, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_IF_ELSE_ON_NEW_LINE, true));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_IF_SPECIAL_ELSE_IF, false));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_FOR_STATEMENT, ""));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_FOR_STATEMENT, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_NEW_LINE_AFTER_LPAREN_FOR, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_PLACE_RPAREN_ON_NEW_LINE_FOR, true));
        assertEquals("Do not force", gs.getString(GroovyCodeStyleSettings.WRAP_FOR_FORCE_BRACES, ""));
        assertEquals("Do not force", gs.getString(GroovyCodeStyleSettings.WRAP_WHILE_FORCE_BRACES, ""));
        assertEquals("Do not force", gs.getString(GroovyCodeStyleSettings.WRAP_DO_WHILE_FORCE_BRACES, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_DO_WHILE_ON_NEW_LINE, true));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_SWITCH_INDENT_CASE_BRANCHES, false));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_TRY_WITH_RESOURCES, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_TRY_WITH_RESOURCES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_NEW_LINE_AFTER_LPAREN_TRY_WITH_RESOURCES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_PLACE_RPAREN_ON_NEW_LINE_TRY_WITH_RESOURCES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_TRY_CATCH_ON_NEW_LINE, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_TRY_FINALLY_ON_NEW_LINE, true));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_BINARY_EXPRESSIONS, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_BINARY_EXPRESSIONS, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_NEW_LINE_AFTER_LPAREN_BINARY_EXPRESSIONS, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_PLACE_RPAREN_ON_NEW_LINE_BINARY_EXPRESSIONS, true));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_ASSIGNMENT_STATEMENT, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_ASSIGNMENT_STATEMENT, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_FIELDS_IN_COLUMNS, true));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_TERNARY_OPERATION, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_TERNARY_OPERATION, true));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_ARRAY_INITIALIZER, ""));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_ARRAY_INITIALIZER, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_NEW_LINE_AFTER_LBRACE_ARRAY_INITIALIZER, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_PLACE_RBRACE_ON_NEW_LINE_ARRAY_INITIALIZER, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.WRAP_AFTER_MODIFIER_LIST, true));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_ASSERT_STATEMENT, ""));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_ENUM_CONSTANTS, ""));
        assertEquals("Wrap always", gs.getString(GroovyCodeStyleSettings.WRAP_CLASS_ANNOTATIONS, ""));
        assertEquals("Wrap always", gs.getString(GroovyCodeStyleSettings.WRAP_METHOD_ANNOTATIONS, ""));
        assertEquals("Wrap always", gs.getString(GroovyCodeStyleSettings.WRAP_FIELD_ANNOTATIONS, ""));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_PARAMETER_ANNOTATIONS, ""));
        assertEquals("Do not wrap", gs.getString(GroovyCodeStyleSettings.WRAP_LOCAL_VARIABLE_ANNOTATIONS, ""));
        assertEquals("Wrap always", gs.getString(GroovyCodeStyleSettings.WRAP_IMPORT_ANNOTATIONS, ""));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_LIST_MAP_MULTIPLE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_ALIGN_LIST_MAP_MULTILINE_NAMED_ARGS, false));
        assertEquals("Wrap always", gs.getString(GroovyCodeStyleSettings.WRAP_GINQ_CLAUSES, ""));
        assertEquals("Wrap if long", gs.getString(GroovyCodeStyleSettings.WRAP_GINQ_ON_CLAUSE, ""));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_GINQ_INDENT_ON_CLAUSE, false));
        assertEquals("Wrap if long", gs.getString(GroovyCodeStyleSettings.WRAP_GINQ_HAVING_CLAUSE, ""));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_GINQ_INDENT_HAVING_CLAUSE, false));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.WRAP_GINQ_PUT_SPACE_AFTER_KEYWORDS, false));

        // Blank Lines defaults (media_1790578964702.png)
        assertEquals(2, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, 0));
        assertEquals(2, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_KEEP_IN_CODE, 0));
        assertEquals(2, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_KEEP_BEFORE_RBRACE, 0));
        assertEquals(0, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_BEFORE_PACKAGE, -1));
        assertEquals(1, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_AFTER_PACKAGE, -1));
        assertEquals(1, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_BEFORE_IMPORTS, -1));
        assertEquals(1, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_AFTER_IMPORTS, -1));
        assertEquals(1, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_AROUND_CLASS, -1));
        assertEquals(0, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_AFTER_CLASS_HEADER, -1));
        assertEquals(0, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_AROUND_FIELD_IN_INTERFACE, -1));
        assertEquals(0, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_AROUND_FIELD, -1));
        assertEquals(1, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_AROUND_METHOD_IN_INTERFACE, -1));
        assertEquals(1, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_AROUND_METHOD, -1));
        assertEquals(0, gs.getInt(GroovyCodeStyleSettings.BLANK_LINES_BEFORE_METHOD_BODY, -1));

        // Has Preview verification (media_1790579404607.png, media_1790579415188.png, media_1790579425910.png)
        assertTrue(provider.hasPreview("Tabs and Indents"));
        assertTrue(provider.hasPreview("Spaces"));
        assertTrue(provider.hasPreview("Wrapping and Braces"));
        assertTrue(provider.hasPreview("Blank Lines"));
        assertFalse(provider.hasPreview("GroovyDoc"));
        assertFalse(provider.hasPreview("Imports"));
        assertFalse(provider.hasPreview("Code Generation"));

        // GroovyDoc defaults (media_1790579404607.png)
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.GROOVY_DOC_ENABLE_FORMATTING, false));

        // Imports defaults (media_1790579415188.png)
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.IMPORTS_USE_SINGLE_CLASS_IMPORT, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.IMPORTS_USE_FQ_CLASS_NAMES, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.IMPORTS_INSERT_FOR_INNER_CLASSES, true));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.IMPORTS_USE_FQ_CLASS_NAMES_IN_JAVADOC, false));
        assertEquals(5, gs.getInt(GroovyCodeStyleSettings.IMPORTS_CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND, 0));
        assertEquals(3, gs.getInt(GroovyCodeStyleSettings.IMPORTS_NAMES_COUNT_TO_USE_STATIC_IMPORT_ON_DEMAND, 0));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.IMPORTS_LAYOUT_STATIC_IMPORTS_SEPARATELY, false));

        assertEquals(2, gs.getPackagesToUseImportOnDemand().size());
        assertEquals("import java.awt.*", gs.getPackagesToUseImportOnDemand().get(0).getPackageName());
        assertFalse(gs.getPackagesToUseImportOnDemand().get(0).isStatic());
        assertFalse(gs.getPackagesToUseImportOnDemand().get(0).isWithSubpackages());

        assertEquals("import javax.swing.*", gs.getPackagesToUseImportOnDemand().get(1).getPackageName());
        assertFalse(gs.getPackagesToUseImportOnDemand().get(1).isStatic());
        assertFalse(gs.getPackagesToUseImportOnDemand().get(1).isWithSubpackages());

        assertEquals(6, gs.getImportLayout().size());
        assertEquals("import all other imports", gs.getImportLayout().get(0).getPackageName());
        assertTrue(gs.getImportLayout().get(0).isSpecial());
        assertEquals("<blank line>", gs.getImportLayout().get(1).getPackageName());
        assertTrue(gs.getImportLayout().get(1).isSpecial());
        assertEquals("import javax.*", gs.getImportLayout().get(2).getPackageName());
        assertTrue(gs.getImportLayout().get(2).isWithSubpackages());
        assertFalse(gs.getImportLayout().get(2).isSpecial());
        assertEquals("import java.*", gs.getImportLayout().get(3).getPackageName());
        assertTrue(gs.getImportLayout().get(3).isWithSubpackages());
        assertFalse(gs.getImportLayout().get(3).isSpecial());
        assertEquals("<blank line>", gs.getImportLayout().get(4).getPackageName());
        assertTrue(gs.getImportLayout().get(4).isSpecial());
        assertEquals("import static all other imports", gs.getImportLayout().get(5).getPackageName());
        assertTrue(gs.getImportLayout().get(5).isSpecial());

        // Code Generation defaults (media_1790579425910.png)
        assertEquals(7, gs.getOrderOfMembers().size());
        assertEquals("Static fields", gs.getOrderOfMembers().get(0));
        assertEquals("Instance fields", gs.getOrderOfMembers().get(1));
        assertEquals("Constructors", gs.getOrderOfMembers().get(2));
        assertEquals("Static methods", gs.getOrderOfMembers().get(3));
        assertEquals("Instance methods", gs.getOrderOfMembers().get(4));
        assertEquals("Static inner classes", gs.getOrderOfMembers().get(5));
        assertEquals("Inner classes", gs.getOrderOfMembers().get(6));

        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, true));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.CODE_GEN_ENFORCE_ON_REFORMAT, true));
        assertTrue(gs.getBoolean(GroovyCodeStyleSettings.CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, false));
        assertFalse(gs.getBoolean(GroovyCodeStyleSettings.CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, true));

        // Sample codes
        assertEquals(GroovyCodeStyleSettings.SAMPLE_TABS_AND_INDENTS, provider.getSampleCode("Tabs and Indents"));
        assertEquals(GroovyCodeStyleSettings.SAMPLE_SPACES, provider.getSampleCode("Spaces"));
        assertEquals(GroovyCodeStyleSettings.SAMPLE_WRAPPING_AND_BRACES, provider.getSampleCode("Wrapping and Braces"));
        assertEquals(GroovyCodeStyleSettings.SAMPLE_BLANK_LINES, provider.getSampleCode("Blank Lines"));

        // Copy independence
        GroovyCodeStyleSettings copy = gs.copy();
        assertEquals(gs.getLabelIndent(), copy.getLabelIndent());
        copy.setLabelIndent(4);
        assertNotEquals(gs.getLabelIndent(), copy.getLabelIndent());

        copy.getPackagesToUseImportOnDemand().remove(0);
        assertNotEquals(gs.getPackagesToUseImportOnDemand().size(), copy.getPackagesToUseImportOnDemand().size());

        copy.getImportLayout().remove(0);
        assertNotEquals(gs.getImportLayout().size(), copy.getImportLayout().size());

        copy.getOrderOfMembers().remove(0);
        assertNotEquals(gs.getOrderOfMembers().size(), copy.getOrderOfMembers().size());
    }

    @Test
    void testGradleDeclarativeAndGroovyLanguagePages() {
        if (!javaFxAvailable) {
            System.out.println("JavaFX not available, skipping testGradleDeclarativeAndGroovyLanguagePages");
            return;
        }

        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // Test Gradle Declarative page
                SettingsCodeStyleLanguagePage gradlePage = new SettingsCodeStyleLanguagePage("Gradle Declarative Configuration");
                assertNotNull(gradlePage);
                assertEquals("Tabs and Indents", gradlePage.getActiveTab(), "Gradle Declarative must have Tabs and Indents as active tab");
                assertFalse(gradlePage.isModified());

                // Test Groovy page
                SettingsCodeStyleLanguagePage groovyPage = new SettingsCodeStyleLanguagePage("Groovy");
                assertNotNull(groovyPage);
                assertEquals("Tabs and Indents", groovyPage.getActiveTab(), "Groovy must default to Tabs and Indents tab");

                groovyPage.setActiveTab("Spaces");
                assertEquals("Spaces", groovyPage.getActiveTab());

                groovyPage.setActiveTab("Wrapping and Braces");
                assertEquals("Wrapping and Braces", groovyPage.getActiveTab());

                groovyPage.setActiveTab("Blank Lines");
                assertEquals("Blank Lines", groovyPage.getActiveTab());

                groovyPage.setActiveTab("GroovyDoc");
                assertEquals("GroovyDoc", groovyPage.getActiveTab());

                groovyPage.setActiveTab("Imports");
                assertEquals("Imports", groovyPage.getActiveTab());

                groovyPage.setActiveTab("Code Generation");
                assertEquals("Code Generation", groovyPage.getActiveTab());
                assertFalse(groovyPage.isModified());

            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Language pages test timed out");
        } catch (InterruptedException e) {
            fail("Language pages test interrupted");
        }

        if (error.get() != null) {
            fail("Exception in JavaFX thread: " + error.get().getMessage(), error.get());
        }
    }

    @Test
    void testHtmlCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("HTML");
        assertNotNull(provider, "HTML provider must be registered");
        assertEquals("HTML", provider.getLanguageId());
        assertEquals("HTML", provider.getDisplayName());

        // 4 tabs matching reference images
        List<String> tabs = provider.getSupportedTabs();
        assertEquals(List.of("Tabs and Indents", "Other", "Arrangement", "Code Generation"), tabs);

        // Previews
        assertTrue(provider.hasPreview("Tabs and Indents"));
        assertTrue(provider.hasPreview("Other"));
        assertFalse(provider.hasPreview("Arrangement"));
        assertFalse(provider.hasPreview("Code Generation"));

        // Defaults
        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof HtmlCodeStyleSettings);
        HtmlCodeStyleSettings html = (HtmlCodeStyleSettings) settings;

        // Tab 1: Tabs and Indents
        assertEquals(4, html.getTabSize());
        assertEquals(4, html.getIndent());
        assertEquals(8, html.getContinuationIndent());
        assertFalse(html.isUseTabCharacter());
        assertFalse(html.isSmartTabs());
        assertFalse(html.isKeepIndentsOnEmptyLines());
        assertFalse(html.isUseHtmlIndentsWithinStyleAndScript());

        // Tab 2: Other
        assertEquals(120, html.getHardWrapAt());
        assertEquals("Default: No", html.getWrapOnTyping());
        assertTrue(html.isKeepLineBreaks());
        assertTrue(html.isKeepLineBreaksInText());
        assertEquals(2, html.getKeepBlankLines());
        assertEquals("Wrap if long", html.getWrapAttributes());
        assertTrue(html.isWrapText());
        assertTrue(html.isAlignAttributes());
        assertFalse(html.isAlignText());
        assertFalse(html.isKeepWhiteSpaces());
        assertFalse(html.isSpacesAroundEqualityInAttribute());
        assertFalse(html.isSpacesAfterTagName());
        assertFalse(html.isSpacesInEmptyTag());
        assertEquals("body,div,p,form,h1,h2,h3", html.getInsertNewLineBefore());
        assertEquals("br", html.getRemoveNewLineBefore());
        assertEquals("html,body,thead,tbody,tfoot", html.getDoNotIndentChildrenOf());
        assertEquals("strong,sub,sup,textarea,tt,u,var", html.getInlineElements());
        assertEquals("span,pre,textarea", html.getKeepWhiteSpacesInside());
        assertEquals("title,h1,h2,h3,h4,h5,h6,p", html.getDontBreakIfInlineContent());
        assertEquals("Never", html.getNewLineBeforeFirstAttribute());
        assertEquals("Never", html.getNewLineAfterLastAttribute());
        assertEquals("Braces", html.getAddForJsxAttributes());
        assertEquals("Double", html.getGeneratedQuoteMarks());
        assertFalse(html.isEnforceOnFormat());

        // Tab 3: Arrangement
        assertEquals(List.of("attribute"), html.getMatchingRules());

        // Tab 4: Code Generation
        assertTrue(html.getBoolean(HtmlCodeStyleSettings.CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, false));
        assertTrue(html.getBoolean(HtmlCodeStyleSettings.CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, false));
        assertFalse(html.getBoolean(HtmlCodeStyleSettings.CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, true));

        // Copy
        HtmlCodeStyleSettings copy = html.copy();
        assertEquals(html.getTabSize(), copy.getTabSize());
        assertEquals(html.getHardWrapAt(), copy.getHardWrapAt());
        assertEquals(html.getWrapAttributes(), copy.getWrapAttributes());
        assertEquals(html.getMatchingRules(), copy.getMatchingRules());
        assertNotSame(html.getMatchingRules(), copy.getMatchingRules());
    }

    @Test
    void testHtmlLanguagePage() {
        if (!javaFxAvailable) {
            System.out.println("JavaFX not available, skipping testHtmlLanguagePage");
            return;
        }

        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SettingsCodeStyleLanguagePage page = new SettingsCodeStyleLanguagePage("HTML");
                assertNotNull(page);
                assertEquals("Tabs and Indents", page.getActiveTab());

                page.setActiveTab("Other");
                assertEquals("Other", page.getActiveTab());

                page.setActiveTab("Arrangement");
                assertEquals("Arrangement", page.getActiveTab());

                page.setActiveTab("Code Generation");
                assertEquals("Code Generation", page.getActiveTab());

                assertFalse(page.isModified());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "HTML language page test timed out");
        } catch (InterruptedException e) {
            fail("HTML language page test interrupted");
        }

        if (error.get() != null) {
            fail("Exception in JavaFX thread: " + error.get().getMessage(), error.get());
        }
    }

    @Test
    void testHttpRequestCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("HTTP Request");
        assertNotNull(provider, "HTTP Request provider must be registered");
        assertEquals("HTTP Request", provider.getLanguageId());
        assertEquals("HTTP Request", provider.getDisplayName());

        // 3 tabs matching reference screenshots
        List<String> tabs = provider.getSupportedTabs();
        assertEquals(List.of("Tabs and Indents", "Wrapping and Braces", "Spaces"), tabs);

        // Previews
        assertTrue(provider.hasPreview("Tabs and Indents"));
        assertTrue(provider.hasPreview("Wrapping and Braces"));
        assertTrue(provider.hasPreview("Spaces"));

        // Defaults
        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof HttpRequestCodeStyleSettings);
        HttpRequestCodeStyleSettings http = (HttpRequestCodeStyleSettings) settings;

        // Tab 1: Tabs and Indents (media_1790582880182.png)
        assertEquals(4, http.getIndent());
        assertEquals(4, http.getTabSize());
        assertEquals(4, http.getUrlPartsIndent());

        // Tab 2: Wrapping and Braces (media_1790582889076.png)
        assertEquals("Default: None", http.getVisualGuides());
        assertEquals("Wrap always", http.getFormUrlencodedParamsWrap());
        assertEquals("Wrap if long", http.getQueryParamsWrap());

        // Tab 3: Spaces (media_1790582896367.png)
        assertTrue(http.isSpacesAroundEqualityInForm());
        assertTrue(http.isSpaceBeforeAmpersandInForm());

        // Copy
        HttpRequestCodeStyleSettings copy = http.copy();
        assertEquals(http.getIndent(), copy.getIndent());
        assertEquals(http.getUrlPartsIndent(), copy.getUrlPartsIndent());
        assertEquals(http.getVisualGuides(), copy.getVisualGuides());
        assertEquals(http.getFormUrlencodedParamsWrap(), copy.getFormUrlencodedParamsWrap());
        assertEquals(http.getQueryParamsWrap(), copy.getQueryParamsWrap());
        assertEquals(http.isSpacesAroundEqualityInForm(), copy.isSpacesAroundEqualityInForm());
        assertEquals(http.isSpaceBeforeAmpersandInForm(), copy.isSpaceBeforeAmpersandInForm());

        copy.setUrlPartsIndent(8);
        assertEquals(4, http.getUrlPartsIndent());
        assertEquals(8, copy.getUrlPartsIndent());
    }

    @Test
    void testHttpRequestLanguagePage() {
        if (!javaFxAvailable) {
            System.out.println("JavaFX not available, skipping testHttpRequestLanguagePage");
            return;
        }

        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SettingsCodeStyleLanguagePage page = new SettingsCodeStyleLanguagePage("HTTP Request");
                assertNotNull(page);
                assertEquals("Tabs and Indents", page.getActiveTab());

                page.setActiveTab("Wrapping and Braces");
                assertEquals("Wrapping and Braces", page.getActiveTab());

                page.setActiveTab("Spaces");
                assertEquals("Spaces", page.getActiveTab());

                assertFalse(page.isModified());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "HTTP Request language page test timed out");
        } catch (InterruptedException e) {
            fail("HTTP Request language page test interrupted");
        }

        if (error.get() != null) {
            fail("Exception in JavaFX thread: " + error.get().getMessage(), error.get());
        }
    }

    @Test
    void testJavaScriptCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("JavaScript");
        assertNotNull(provider, "JavaScript provider must be registered");
        assertEquals("JavaScript", provider.getLanguageId());
        assertEquals("JavaScript", provider.getDisplayName());

        // 8 tabs matching reference screenshots
        List<String> tabs = provider.getSupportedTabs();
        assertEquals(List.of(
                "Tabs and Indents",
                "Spaces",
                "Wrapping and Braces",
                "Blank Lines",
                "Punctuation",
                "Code Generation",
                "Imports",
                "Arrangement"
        ), tabs);

        // Previews
        assertTrue(provider.hasPreview("Tabs and Indents"));
        assertTrue(provider.hasPreview("Spaces"));
        assertTrue(provider.hasPreview("Wrapping and Braces"));
        assertTrue(provider.hasPreview("Blank Lines"));
        assertTrue(provider.hasPreview("Punctuation"));
        assertFalse(provider.hasPreview("Code Generation"));
        assertFalse(provider.hasPreview("Imports"));
        assertFalse(provider.hasPreview("Arrangement"));

        // Defaults
        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof JavaScriptCodeStyleSettings);
        JavaScriptCodeStyleSettings js = (JavaScriptCodeStyleSettings) settings;

        // Tab 1: Tabs and Indents (media_1790582906008.png)
        assertEquals(4, js.getTabSize());
        assertEquals(4, js.getIndent());
        assertEquals(4, js.getContinuationIndent());
        assertFalse(js.isUseTabCharacter());
        assertFalse(js.isSmartTabs());
        assertFalse(js.isKeepIndentsOnEmptyLines());
        assertTrue(js.isIndentChainedMethods());
        assertFalse(js.isIndentAllChainedCallsInGroup());

        // Tab 2: Spaces (media_1790582923812.png)
        assertFalse(js.isSpaceBeforeFunctionDeclarationParentheses());
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FUNCTION_CALL_PARENTHESES, false));
        assertTrue(js.isSpaceBeforeIfParentheses());
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FOR_PARENTHESES, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_WHILE_PARENTHESES, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_SWITCH_PARENTHESES, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_CATCH_PARENTHESES, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FUNCTION_EXPRESSION_PARENTHESES, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_ASYNC_ARROW_PARENTHESES, true));

        assertTrue(js.isSpaceAroundAssignmentOperators());
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_LOGICAL_OPERATORS, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_EQUALITY_OPERATORS, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_RELATIONAL_OPERATORS, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_BITWISE_OPERATORS, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_ADDITIVE_OPERATORS, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_SHIFT_OPERATORS, true));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_UNARY_ADDITIVE_OPERATORS, false));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_ARROW_FUNCTION, true));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_UNARY_NOT, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AFTER_UNARY_NOT, false));

        assertTrue(js.isSpaceBeforeFunctionLeftBrace());
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_IF_LEFT_BRACE, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_ELSE_LEFT_BRACE, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FOR_LEFT_BRACE, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_WHILE_LEFT_BRACE, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_DO_LEFT_BRACE, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_SWITCH_LEFT_BRACE, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_TRY_LEFT_BRACE, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_CATCH_LEFT_BRACE, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FINALLY_LEFT_BRACE, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_CLASS_LEFT_BRACE, true));

        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_ELSE_KEYWORD, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_WHILE_KEYWORD, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_CATCH_KEYWORD, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FINALLY_KEYWORD, true));

        assertFalse(js.isSpacesWithinCodeBraces());
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_INDEX_ACCESS_BRACKETS, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_GROUPING_PARENTHESES, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_FUNCTION_DECLARATION_PARENTHESES, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_FUNCTION_CALL_PARENTHESES, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_IF_PARENTHESES, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_FOR_PARENTHESES, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_WHILE_PARENTHESES, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_SWITCH_PARENTHESES, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_CATCH_PARENTHESES, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_OBJECT_LITERAL_BRACES, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_ES6_IMPORT_EXPORT_BRACES, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_ARRAY_BRACKETS, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_INTERPOLATION_EXPRESSIONS, false));

        assertTrue(js.isSpaceBeforeTernaryQuestion());
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AFTER_TERNARY_QUESTION, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_TERNARY_COLON, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AFTER_TERNARY_COLON, true));

        assertFalse(js.isSpaceBeforeComma());
        assertTrue(js.isSpaceAfterComma());
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FOR_SEMICOLON, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_PROPERTY_NAME_VALUE_SEPARATOR, false));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AFTER_PROPERTY_NAME_VALUE_SEPARATOR, true));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AFTER_REST_SPREAD, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_GENERATOR_STAR, false));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPACE_AFTER_GENERATOR_STAR, true));

        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.FLOW_SPACE_BEFORE_TYPE_REFERENCE_COLON, false));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.FLOW_SPACE_AFTER_TYPE_REFERENCE_COLON, true));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.FLOW_OBJECT_LITERAL_TYPE_BRACES, false));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.FLOW_UNION_AND_INTERSECTION_TYPES, true));

        // Tab 3: Wrapping and Braces (media_1790584332985.png, media_1790584373383.png, media_1790584397014.png)
        assertEquals(120, js.getInt(JavaScriptCodeStyleSettings.HARD_WRAP_AT, 120));
        assertEquals("Default: No", js.getString(JavaScriptCodeStyleSettings.WRAP_ON_TYPING, "Default: No"));
        assertEquals("Default: None", js.getString(JavaScriptCodeStyleSettings.VISUAL_GUIDES, "Default: None"));

        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.KEEP_LINE_BREAKS, true));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.KEEP_COMMENT_AT_FIRST_COLUMN, true));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.KEEP_SIMPLE_METHODS_IN_ONE_LINE, false));

        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.WRAP_COMMENTS_AT_RIGHT_MARGIN, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.ALIGN_MULTILINE_COMMENTS, false));

        assertEquals("End of line", js.getBracePlacementFunction());
        assertEquals("End of line", js.getString(JavaScriptCodeStyleSettings.BRACE_PLACEMENT_CLASS, "End of line"));
        assertEquals("End of line", js.getString(JavaScriptCodeStyleSettings.BRACE_PLACEMENT_FUNCTION_EXPRESSION, "End of line"));
        assertEquals("End of line", js.getString(JavaScriptCodeStyleSettings.BRACE_PLACEMENT_OTHER, "End of line"));

        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_EXTENDS_LIST, "Do not wrap"));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.ALIGN_MULTILINE_EXTENDS_LIST, false));
        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_EXTENDS_KEYWORD, "Do not wrap"));

        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_FUNCTION_PARAMETERS, "Do not wrap"));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.ALIGN_MULTILINE_FUNCTION_PARAMETERS, true));
        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_FUNCTION_ARGUMENTS, "Do not wrap"));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.ALIGN_MULTILINE_FUNCTION_ARGUMENTS, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.CALL_ARGUMENTS_TAKE_PRIORITY_OVER_CALL_CHAIN, false));

        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_CHAINED_METHOD_CALLS, "Do not wrap"));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.CHAINED_METHOD_DOT_ON_NEW_LINE, true));

        assertEquals("Do not force", js.getString(JavaScriptCodeStyleSettings.IF_FORCE_BRACES, "Do not force"));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.SPECIAL_ELSE_IF_TREATMENT, true));

        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_FOR_STATEMENT, "Do not wrap"));
        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.ALIGN_MULTILINE_FOR_STATEMENT, true));
        assertEquals("Do not force", js.getString(JavaScriptCodeStyleSettings.FOR_FORCE_BRACES, "Do not force"));

        assertEquals("Do not force", js.getString(JavaScriptCodeStyleSettings.WHILE_FORCE_BRACES, "Do not force"));
        assertEquals("Do not force", js.getString(JavaScriptCodeStyleSettings.DO_WHILE_FORCE_BRACES, "Do not force"));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.WRAP_DO_WHILE_ON_NEW_LINE, false));

        assertTrue(js.getBoolean(JavaScriptCodeStyleSettings.WRAP_SWITCH_INDENT_CASE_BRANCHES, true));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.WRAP_TRY_CATCH_ON_NEW_LINE, false));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.WRAP_TRY_FINALLY_ON_NEW_LINE, false));

        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_BINARY_EXPRESSIONS, "Do not wrap"));
        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_ASSIGNMENT_STATEMENT, "Do not wrap"));
        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_TERNARY_OPERATION, "Do not wrap"));
        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_ARRAYS, "Do not wrap"));

        assertEquals("Chop down if long", js.getString(JavaScriptCodeStyleSettings.WRAP_OBJECTS, "Chop down if long"));
        assertEquals("Do not align", js.getString(JavaScriptCodeStyleSettings.OBJECTS_ALIGN, "Do not align"));

        assertEquals("Wrap if long", js.getString(JavaScriptCodeStyleSettings.WRAP_VARIABLE_DECLARATIONS, "Wrap if long"));
        assertEquals("Do not align", js.getString(JavaScriptCodeStyleSettings.VARIABLE_DECLARATIONS_ALIGN, "Do not align"));

        assertEquals("Chop down if long", js.getString(JavaScriptCodeStyleSettings.WRAP_ES6_IMPORT_EXPORT, "Chop down if long"));
        assertFalse(js.getBoolean(JavaScriptCodeStyleSettings.ES6_ALIGN_FROM_CLAUSES, false));

        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_FUNCTION_PARAMETER_DECORATORS, "Do not wrap"));
        assertEquals("Wrap always", js.getString(JavaScriptCodeStyleSettings.WRAP_CLASS_DECORATORS, "Wrap always"));
        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_CLASS_FIELD_DECORATORS, "Do not wrap"));
        assertEquals("Do not wrap", js.getString(JavaScriptCodeStyleSettings.WRAP_CLASS_METHOD_DECORATORS, "Do not wrap"));

        // Tab 4: Blank Lines (media_1790587078488.png)
        assertEquals(2, js.getBlankLinesKeepInCode());
        assertEquals(2, js.getBlankLinesKeepInDeclarations());
        assertEquals(1, js.getBlankLinesAfterImports());
        assertEquals(1, js.getBlankLinesAroundClass());
        assertEquals(0, js.getBlankLinesAroundField());
        assertEquals(1, js.getBlankLinesAroundMethod());
        assertEquals(1, js.getBlankLinesAroundFunction());

        // Tab 5: Punctuation (media_1790587087019.png)
        assertEquals("Use", js.getUseSemicolon());
        assertEquals("in code generated by IDE", js.getSemicolonScope());
        assertEquals("double", js.getQuoteStyle());
        assertEquals("in code generated by IDE", js.getQuoteScope());
        assertEquals("Keep", js.getTrailingComma());

        // Tab 6: Code Generation (media_1790587100814.png)
        assertEquals("_", js.getFieldPrefix());
        assertEquals("", js.getPropertyPrefix());
        assertEquals("Reuse case of current file", js.getFilenameConvention());
        assertFalse(js.isCodeGenLineCommentAtFirstColumn());
        assertTrue(js.isCodeGenAddSpaceAtLineCommentStart());
        assertTrue(js.isCodeGenBlockCommentAtFirstColumn());
        assertFalse(js.isCodeGenAddSpacesAroundBlockComments());

        // Tab 7: Imports (media_1790587112114.png)
        assertTrue(js.isMergeImportsSameModule());
        assertFalse(js.isUseRelativePaths());
        assertTrue(js.isUseDirectoryImport());
        assertEquals("Auto", js.getUseFileExtension());
        assertEquals("Always", js.getUsePathAliases());
        assertEquals("rxjs,@angular/material/typings/**", js.getDoNotImportExactlyFrom());
        assertTrue(js.isSortImportedMembers());
        assertFalse(js.isSortImportsByModules());

        // Tab 8: Arrangement (media_1790587120119.png)
        assertTrue(js.isGroupPropertyFieldWithGetterSetter());
        assertTrue(js.isGroupFieldsWithArrowFunctions());
        assertFalse(js.isKeepOverriddenMethodsTogether());
        assertEquals("keep order", js.getOverriddenMethodsOrder());
        assertEquals(List.of("field, static", "field", "constructor", "property, static", "property", "method, static", "method"), js.getMatchingRules());

        // Copy
        JavaScriptCodeStyleSettings copy = js.copy();
        assertEquals(js.getTabSize(), copy.getTabSize());
        assertEquals(js.getIndent(), copy.getIndent());
        assertEquals(js.getContinuationIndent(), copy.getContinuationIndent());
        assertEquals(js.isIndentChainedMethods(), copy.isIndentChainedMethods());
        assertEquals(js.getMatchingRules(), copy.getMatchingRules());
        assertNotSame(js.getMatchingRules(), copy.getMatchingRules());

        copy.getMatchingRules().add("other");
        assertEquals(7, js.getMatchingRules().size());
        assertEquals(8, copy.getMatchingRules().size());
    }

    @Test
    void testJavaScriptLanguagePage() {
        if (!javaFxAvailable) {
            System.out.println("JavaFX not available, skipping testJavaScriptLanguagePage");
            return;
        }

        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SettingsCodeStyleLanguagePage page = new SettingsCodeStyleLanguagePage("JavaScript");
                assertNotNull(page);
                assertEquals("Tabs and Indents", page.getActiveTab());

                page.setActiveTab("Spaces");
                assertEquals("Spaces", page.getActiveTab());

                page.setActiveTab("Wrapping and Braces");
                assertEquals("Wrapping and Braces", page.getActiveTab());

                page.setActiveTab("Blank Lines");
                assertEquals("Blank Lines", page.getActiveTab());
                assertTrue(page.getSampleForActiveTab().contains("class Foo"));

                page.setActiveTab("Punctuation");
                assertEquals("Punctuation", page.getActiveTab());
                assertTrue(page.getSampleForActiveTab().contains("const myLink ="));

                page.setActiveTab("Code Generation");
                assertEquals("Code Generation", page.getActiveTab());

                page.setActiveTab("Imports");
                assertEquals("Imports", page.getActiveTab());

                page.setActiveTab("Arrangement");
                assertEquals("Arrangement", page.getActiveTab());

                assertFalse(page.isModified());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaScript language page test timed out");
        } catch (InterruptedException e) {
            fail("JavaScript language page test interrupted");
        }

        if (error.get() != null) {
            fail("Exception in JavaFX thread: " + error.get().getMessage(), error.get());
        }
    }

    @Test
    void testBrandIsolation() throws Exception {
        Pattern competitorPattern = Pattern.compile("(?i)\\b(intellij|jetbrains|idea)\\b");

        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/settings/CodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/JavaCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/KotlinCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/AngularHtmlCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/EditorConfigCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/ErbCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/GoCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/GradleDeclarativeCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/GroovyCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/HtmlCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/HttpRequestCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/JavaScriptCodeStyleSettings.java",
                "src/main/java/dev/lumina/ui/CodeStyleHeaderBar.java",
                "src/main/java/dev/lumina/ui/SettingsCodeStylePage.java",
                "src/main/java/dev/lumina/ui/SettingsCodeStyleLanguagePage.java",
                "src/main/java/dev/lumina/ui/SettingsCodeStyleJavaPage.java"
        );

        for (String relPath : filesToCheck) {
            Path path = Path.of(relPath);
            assertTrue(Files.exists(path), "File must exist: " + relPath);
            String content = Files.readString(path);
            var matcher = competitorPattern.matcher(content);
            assertFalse(matcher.find(), "Competitor brand found in file " + relPath + ": " + (matcher.hitEnd() ? "" : matcher.group()));
        }
    }
}
