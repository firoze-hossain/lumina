package dev.lumina.debugger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DebuggerSettingsManager and its dynamic configuration models.
 */
public class DebuggerSettingsManagerTest {

    private DebuggerSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = DebuggerSettingsManager.getInstance();
        manager.resetToDefaults();
    }

    @Test
    void testGeneralSettingsDefaultsAndCloning() {
        DebuggerGeneralSettings s1 = manager.getGeneralSettings();
        assertTrue(s1.isShowDebugWindowOnBreakpoint());
        assertTrue(s1.isFocusApplicationOnBreakpoint());
        assertFalse(s1.isHideDebugWindowOnProcessTermination());
        assertFalse(s1.isScrollExecutionPointToCenter());
        assertTrue(s1.isClickLineNumberToRunToCursor());
        assertEquals(DebuggerGeneralSettings.BreakpointRemoveMode.CLICK_LEFT, s1.getRemoveBreakpointMode());
        assertFalse(s1.isConfirmRemovalConditionalOrLogging());
        assertEquals(DebuggerGeneralSettings.TransportMode.SOCKET, s1.getTransport());
        assertTrue(s1.isShowAlternativeSourceSwitcher());
        assertFalse(s1.isKillDebugProcessImmediately());
        assertFalse(s1.isAttachMemoryAgent());
        assertTrue(s1.isAttachCoroutineAgent());

        DebuggerGeneralSettings s2 = s1.clone();
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        s2.setTransport(DebuggerGeneralSettings.TransportMode.SHARED_MEMORY);
        assertNotEquals(s1, s2);
    }

    @Test
    void testAsyncStackTraceRulesAndDefaults() {
        AsyncStackTraceSettings async = manager.getAsyncStackTraceSettings();
        assertTrue(async.isInstrumentingAgent());
        assertFalse(async.isCaptureLocalVariables());
        assertFalse(async.getRules().isEmpty());

        // Check default rule presence
        boolean hasCompletableFuture = async.getRules().stream()
                .anyMatch(r -> r.getCaptureClassName().contains("CompletableFuture"));
        assertTrue(hasCompletableFuture, "Default rules should contain CompletableFuture");

        // Clone
        AsyncStackTraceSettings cloned = async.clone();
        assertEquals(async, cloned);

        // Add custom rule
        AsyncStackTraceRule custom = new AsyncStackTraceRule(true,
                "com.example.Service", "asyncCall", "this",
                "com.example.Service$Callback", "onComplete", "this");
        async.getRules().add(custom);
        manager.setAsyncStackTraceSettings(async);

        AsyncStackTraceSettings updated = manager.getAsyncStackTraceSettings();
        assertEquals(async.getRules().size(), updated.getRules().size());
    }

    @Test
    void testDataViewsSettings() {
        DataViewsSettings dv = manager.getDataViewsSettings();
        assertFalse(dv.isSortValuesAlphabetically());
        assertTrue(dv.isEnableAutoExpressions());
        assertTrue(dv.isShowValuesInline());
        assertTrue(dv.isShowValueTooltip());
        assertEquals(700, dv.getValueTooltipDelayMs());
        assertFalse(dv.isShowValueTooltipOnCodeSelection());

        DataViewsSettings cloned = dv.clone();
        assertEquals(dv, cloned);

        cloned.setValueTooltipDelayMs(1000);
        assertNotEquals(dv, cloned);
    }

    @Test
    void testJavaDataViewsSettingsAndToStringEvaluation() {
        JavaDataViewsSettings jdv = manager.getJavaDataViewsSettings();
        assertTrue(jdv.isAutoscrollToNewLocalVariables());
        assertTrue(jdv.isPredictConditionValues());
        assertTrue(jdv.isGrayOutPredictedUnreachableCode());
        assertFalse(jdv.isShowDeclaredType());
        assertTrue(jdv.isShowSyntheticFields());
        assertTrue(jdv.isShowValFieldsAsLocalVariables());
        assertFalse(jdv.isShowFullyQualifiedNames());
        assertTrue(jdv.isShowObjectId());
        assertFalse(jdv.isShowStaticFields());
        assertFalse(jdv.isShowStaticFinalFields());
        assertFalse(jdv.isShowHexForPrimitives());
        assertTrue(jdv.isHideNullElements());
        assertTrue(jdv.isEnableToStringObjectView());
        assertEquals(JavaDataViewsSettings.ToStringMode.ALL_OVERRIDING, jdv.getToStringMode());

        // Evaluate toString in ALL_OVERRIDING mode
        assertTrue(manager.shouldFormatToString("org.example.MyClass", true));
        assertFalse(manager.shouldFormatToString("org.example.MyClass", false));

        // Switch to CLASSES_FROM_LIST mode
        jdv.setToStringMode(JavaDataViewsSettings.ToStringMode.CLASSES_FROM_LIST);
        jdv.setToStringClassPatterns(List.of("com.demo.*", "java.lang.String"));
        manager.setJavaDataViewsSettings(jdv);

        assertTrue(manager.shouldFormatToString("com.demo.User", false));
        assertTrue(manager.shouldFormatToString("java.lang.String", false));
        assertFalse(manager.shouldFormatToString("org.other.Item", true));
    }

    @Test
    void testJavaTypeRenderersDynamicLookup() {
        JavaTypeRendererSettings trSettings = manager.getJavaTypeRendererSettings();
        assertFalse(trSettings.getRenderers().isEmpty());

        // Default unnamed renderer
        JavaTypeRenderer defaultRenderer = manager.findRendererForClass("java.lang.Object");
        assertNotNull(defaultRenderer);
        assertEquals("unnamed", defaultRenderer.getName());

        // Add custom renderer with expression
        JavaTypeRenderer mapEntryRenderer = new JavaTypeRenderer("Map Entry", "java.util.Map$Entry");
        mapEntryRenderer.setNodeRendererType(JavaTypeRenderer.NodeRendererType.EXPRESSION);
        mapEntryRenderer.setNodeExpression("getKey() + \" -> \" + getValue()");
        trSettings.getRenderers().add(mapEntryRenderer);
        manager.setJavaTypeRendererSettings(trSettings);

        JavaTypeRenderer found = manager.findRendererForClass("java.util.Map$Entry");
        assertNotNull(found);
        assertEquals("Map Entry", found.getName());
        assertEquals(JavaTypeRenderer.NodeRendererType.EXPRESSION, found.getNodeRendererType());
        assertEquals("getKey() + \" -> \" + getValue()", found.getNodeExpression());
    }

    @Test
    void testChangeListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        DebuggerGeneralSettings s = manager.getGeneralSettings();
        s.setScrollExecutionPointToCenter(true);
        manager.setGeneralSettings(s);

        assertTrue(notified.get(), "Listener should be notified when settings change");
        manager.removeChangeListener(listener);
    }

    @Test
    void testJsonExportAndImport() {
        // Export async rules
        String rulesJson = manager.exportAsyncRulesToJson();
        assertNotNull(rulesJson);
        assertTrue(rulesJson.contains("CompletableFuture"));

        // Export type renderers
        String renderersJson = manager.exportTypeRenderersToJson();
        assertNotNull(renderersJson);
        assertTrue(renderersJson.contains("unnamed"));

        // Import async rules
        String newRuleJson = """
                [
                  {
                    "enabled": true,
                    "captureClassName": "com.test.Custom",
                    "captureMethodName": "run",
                    "captureKeyExpression": "this",
                    "insertClassName": "com.test.Worker",
                    "insertMethodName": "exec",
                    "insertKeyExpression": "this"
                  }
                ]
                """;
        boolean imported = manager.importAsyncRulesFromJson(newRuleJson);
        assertTrue(imported);

        boolean found = manager.getAsyncStackTraceSettings().getRules().stream()
                .anyMatch(r -> "com.test.Custom".equals(r.getCaptureClassName()));
        assertTrue(found, "Imported rule should be present in async rules");
    }

    @Test
    void testPersistenceCycle() {
        DebuggerGeneralSettings gen = manager.getGeneralSettings();
        gen.setKillDebugProcessImmediately(true);
        gen.setAttachMemoryAgent(true);
        manager.setGeneralSettings(gen);

        DataViewsSettings dv = manager.getDataViewsSettings();
        dv.setSortValuesAlphabetically(true);
        dv.setValueTooltipDelayMs(900);
        manager.setDataViewsSettings(dv);

        manager.saveSettings();

        // Reload
        manager.loadSettings();
        assertEquals(gen, manager.getGeneralSettings());
        assertEquals(dv, manager.getDataViewsSettings());
    }

    @Test
    void testJavaScriptDataViewsSettings() {
        JavaScriptDataViewsSettings js = manager.getJavaScriptDataViewsSettings();
        assertFalse(js.isShowPropertiesInObjectNode());
        assertEquals(2, js.getObjectProperties().size());
        assertTrue(js.getObjectProperties().contains("id"));
        assertTrue(js.getObjectProperties().contains("name"));

        JavaScriptDataViewsSettings cloned = js.clone();
        assertEquals(js, cloned);
        assertEquals(js.hashCode(), cloned.hashCode());

        cloned.setShowPropertiesInObjectNode(true);
        cloned.getObjectProperties().add("uuid");
        manager.setJavaScriptDataViewsSettings(cloned);

        JavaScriptDataViewsSettings updated = manager.getJavaScriptDataViewsSettings();
        assertTrue(updated.isShowPropertiesInObjectNode());
        assertEquals(3, updated.getObjectProperties().size());
        assertTrue(updated.getObjectProperties().contains("uuid"));
    }

    @Test
    void testHotSwapSettings() {
        DebuggerHotSwapSettings hs = manager.getHotSwapSettings();
        assertTrue(hs.isCompileBeforeHotSwap());
        assertFalse(hs.isShowHangWarning());
        assertTrue(hs.isSuggestHotSwap());
        assertEquals(DebuggerHotSwapSettings.ReloadClassesMode.ASK, hs.getReloadClasses());
        assertTrue(hs.isEnableGroovyHotSwap());

        DebuggerHotSwapSettings cloned = hs.clone();
        assertEquals(hs, cloned);
        assertEquals(hs.hashCode(), cloned.hashCode());

        cloned.setReloadClasses(DebuggerHotSwapSettings.ReloadClassesMode.ALWAYS);
        cloned.setCompileBeforeHotSwap(false);
        manager.setHotSwapSettings(cloned);

        DebuggerHotSwapSettings updated = manager.getHotSwapSettings();
        assertEquals(DebuggerHotSwapSettings.ReloadClassesMode.ALWAYS, updated.getReloadClasses());
        assertFalse(updated.isCompileBeforeHotSwap());
    }

    @Test
    void testSteppingSettingsAndFilterMatching() {
        DebuggerSteppingSettings ss = manager.getSteppingSettings();
        assertFalse(ss.isFilterSyntheticMethods());
        assertFalse(ss.isFilterClassConstructors());
        assertFalse(ss.isFilterClassInitializers());
        assertFalse(ss.isFilterSimpleGetters());
        assertEquals(DebuggerSteppingSettings.EvaluateFinallyMode.ASK, ss.getEvaluateFinallyOnPopFrame());
        assertTrue(ss.isGracefulStepIntoMethodCalls());
        assertTrue(ss.isFilterGroovyCoreClasses());
        assertTrue(ss.isFilterKotlinStdlib());
        assertTrue(ss.isFilterJsLibraryScripts());
        assertEquals(20, ss.getStepFilters().size());

        // Test filter pattern matching
        assertTrue(ss.matchesAnyFilter("java.lang.String"));
        assertTrue(ss.matchesAnyFilter("javax.swing.JPanel"));
        assertTrue(ss.matchesAnyFilter("com.sun.tools.javac.Main"));
        assertFalse(ss.matchesAnyFilter("com.mycompany.app.Main"));

        // Disable java.* filter and test
        SteppingFilter javaFilter = ss.getStepFilters().stream()
                .filter(f -> "java.*".equals(f.getPattern()))
                .findFirst()
                .orElse(null);
        assertNotNull(javaFilter);
        javaFilter.setEnabled(false);
        assertFalse(ss.matchesAnyFilter("java.lang.String"));

        // Add custom pattern
        ss.getStepFilters().add(new SteppingFilter(true, "com.mycompany.*"));
        assertTrue(ss.matchesAnyFilter("com.mycompany.app.Main"));

        // Clone test
        DebuggerSteppingSettings cloned = ss.clone();
        assertEquals(ss, cloned);
        assertEquals(ss.hashCode(), cloned.hashCode());
    }
}
