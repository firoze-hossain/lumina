package dev.lumina.scala;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Scala settings in Lumina IDE.
 * Manages:
 *  - Editor
 *  - X-Ray Mode
 *  - Project View
 *  - Performance
 *  - Worksheet
 *  - Base Package
 *  - Misc
 *  - Updates
 *  - Extensions
 */
public class ScalaLanguageSettingsManager {

    // Editor Keys
    public static final String KEY_SCALA_EDITOR_HINTS_TYPE_MISMATCH = "scala.editor.hints.type.mismatch";
    public static final String KEY_SCALA_EDITOR_HINTS_NO_IMPLICIT = "scala.editor.hints.no.implicit";
    public static final String KEY_SCALA_EDITOR_HINTS_AMBIGUOUS_IMPLICIT = "scala.editor.hints.ambiguous.implicit";
    public static final String KEY_SCALA_EDITOR_EXPORT_ALIASES = "scala.editor.export.aliases";
    public static final String KEY_SCALA_EDITOR_HIGHLIGHT_IMPLICIT_CONVERSIONS = "scala.editor.highlight.implicit.conversions";
    public static final String KEY_SCALA_EDITOR_HIGHLIGHT_BY_NAME_ARGS = "scala.editor.highlight.by.name.args";
    public static final String KEY_SCALA_EDITOR_INCLUDE_BLOCK_EXPRESSIONS = "scala.editor.include.block.expressions";
    public static final String KEY_SCALA_EDITOR_INCLUDE_LITERALS = "scala.editor.include.literals";
    public static final String KEY_SCALA_EDITOR_CUSTOM_SCALATEST_HIGHLIGHTING = "scala.editor.custom.scalatest.highlighting";
    public static final String KEY_SCALA_EDITOR_COLLECTION_TYPE_HIGHLIGHTING = "scala.editor.collection.type.highlighting";
    public static final String KEY_SCALA_EDITOR_AHEAD_OF_TIME_COMPLETION = "scala.editor.ahead.of.time.completion";
    public static final String KEY_SCALA_EDITOR_PRIORITIZE_SCALA_CLASSES = "scala.editor.prioritize.scala.classes";
    public static final String KEY_SCALA_EDITOR_CONVERT_JAVA_ON_PASTE = "scala.editor.convert.java.on.paste";
    public static final String KEY_SCALA_EDITOR_DONT_SHOW_PASTE_DIALOG = "scala.editor.dont.show.paste.dialog";
    public static final String KEY_SCALA_EDITOR_ADD_OVERRIDE_KEYWORD = "scala.editor.add.override.keyword";

    // X-Ray Keys
    public static final String KEY_SCALA_XRAY_DOUBLE_PRESS_CTRL = "scala.xray.double.press.ctrl";
    public static final String KEY_SCALA_XRAY_PRESS_CTRL = "scala.xray.press.ctrl";
    public static final String KEY_SCALA_XRAY_PARAMETER_NAME_HINTS = "scala.xray.parameter.name.hints";
    public static final String KEY_SCALA_XRAY_FOR_ALL_PARAMETERS = "scala.xray.for.all.parameters";
    public static final String KEY_SCALA_XRAY_BY_NAME_ARGS = "scala.xray.by.name.args";
    public static final String KEY_SCALA_XRAY_APPLY_METHOD_HINTS = "scala.xray.apply.method.hints";
    public static final String KEY_SCALA_XRAY_TYPE_HINTS = "scala.xray.type.hints";
    public static final String KEY_SCALA_XRAY_MEMBER_VARIABLES = "scala.xray.member.variables";
    public static final String KEY_SCALA_XRAY_LOCAL_VARIABLES = "scala.xray.local.variables";
    public static final String KEY_SCALA_XRAY_METHOD_RESULTS = "scala.xray.method.results";
    public static final String KEY_SCALA_XRAY_LAMBDA_PARAMETERS = "scala.xray.lambda.parameters";
    public static final String KEY_SCALA_XRAY_LAMBDA_PLACEHOLDERS = "scala.xray.lambda.placeholders";
    public static final String KEY_SCALA_XRAY_VARIABLE_PATTERNS = "scala.xray.variable.patterns";
    public static final String KEY_SCALA_XRAY_METHOD_CHAIN_HINTS = "scala.xray.method.chain.hints";
    public static final String KEY_SCALA_XRAY_TYPE_ARGUMENTS = "scala.xray.type.arguments";
    public static final String KEY_SCALA_XRAY_IMPLICIT_HINTS = "scala.xray.implicit.hints";
    public static final String KEY_SCALA_XRAY_INDENT_GUIDES = "scala.xray.indent.guides";
    public static final String KEY_SCALA_XRAY_METHOD_SEPARATORS = "scala.xray.method.separators";
    public static final String KEY_SCALA_XRAY_WIDGET_DISPLAY = "scala.xray.widget.display";

    // Project View Keys
    public static final String KEY_SCALA_PROJECT_VIEW_GROUP_PACKAGE_OBJECT = "scala.project.view.group.package.object";
    public static final String KEY_SCALA_PROJECT_VIEW_HIGHLIGHT_NODES_WITH_ERRORS = "scala.project.view.highlight.nodes.with.errors";

    // Performance Keys
    public static final String KEY_SCALA_PERF_IMPLICIT_DEPTH = "scala.performance.implicit.depth";
    public static final String KEY_SCALA_PERF_META_PROGRAMS = "scala.performance.meta.programs";
    public static final String KEY_SCALA_PERF_IVY_CACHE_MODE = "scala.performance.ivy.cache.mode";
    public static final String KEY_SCALA_PERF_TRIM_META_BODIES = "scala.performance.trim.meta.bodies";
    public static final String KEY_SCALA_PERF_SEARCH_ALL_SYMBOLS = "scala.performance.search.all.symbols";
    public static final String KEY_SCALA_PERF_DISABLE_DOC_COMMENTS = "scala.performance.disable.doc.comments";
    public static final String KEY_SCALA_PERF_DISABLE_LANGUAGE_INJECTION = "scala.performance.disable.language.injection";
    public static final String KEY_SCALA_PERF_DONT_CACHE_COMPOUND_TYPES = "scala.performance.dont.cache.compound.types";

    // Worksheet Keys
    public static final String KEY_SCALA_WORKSHEET_TREAT_SC_AS = "scala.worksheet.treat.sc.as";
    public static final String KEY_SCALA_WORKSHEET_RUN_IN_COMPILER_PROCESS = "scala.worksheet.run.in.compiler.process";
    public static final String KEY_SCALA_WORKSHEET_ECLIPSE_COMPATIBILITY = "scala.worksheet.eclipse.compatibility";
    public static final String KEY_SCALA_WORKSHEET_TREAT_SCRATCH_AS_WORKSHEET = "scala.worksheet.treat.scratch.as.worksheet";
    public static final String KEY_SCALA_WORKSHEET_COLLAPSE_OUTPUT = "scala.worksheet.collapse.output";
    public static final String KEY_SCALA_WORKSHEET_OUTPUT_CUTOFF = "scala.worksheet.output.cutoff";
    public static final String KEY_SCALA_WORKSHEET_DELAY_AUTO_RUN = "scala.worksheet.delay.auto.run";

    // Base Package Keys
    public static final String KEY_SCALA_BASE_PACKAGE_INHERIT = "scala.base.package.inherit";
    public static final String KEY_SCALA_BASE_PACKAGE_CUSTOM_ENTRIES = "scala.base.package.custom.entries";

    // Misc Keys
    public static final String KEY_SCALA_MISC_SCALATEST_SUPER_CLASS = "scala.misc.scalatest.super.class";
    public static final String KEY_SCALA_MISC_TRAILING_COMMAS = "scala.misc.trailing.commas";
    public static final String KEY_SCALA_MISC_INJECTIONS = "scala.misc.injections";

    // Updates Keys
    public static final String KEY_SCALA_UPDATES_CHANNEL = "scala.updates.channel";
    public static final String KEY_SCALA_UPDATES_LAST_CHECKED = "scala.updates.last.checked";

    // Extensions Keys
    public static final String KEY_SCALA_EXTENSIONS_ENABLED = "scala.extensions.enabled";
    public static final String KEY_SCALA_EXTENSIONS_LIBRARIES = "scala.extensions.libraries";

    private static ScalaLanguageSettingsManager instance;

    private ScalaEditorSettings editorSettings = new ScalaEditorSettings();
    private ScalaXRaySettings xRaySettings = new ScalaXRaySettings();
    private ScalaProjectViewSettings projectViewSettings = new ScalaProjectViewSettings();
    private ScalaPerformanceSettings performanceSettings = new ScalaPerformanceSettings();
    private ScalaWorksheetSettings worksheetSettings = new ScalaWorksheetSettings();
    private ScalaBasePackageSettings basePackageSettings = new ScalaBasePackageSettings();
    private ScalaMiscSettings miscSettings = new ScalaMiscSettings();
    private ScalaUpdatesSettings updatesSettings = new ScalaUpdatesSettings();
    private ScalaExtensionsSettings extensionsSettings = new ScalaExtensionsSettings();

    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private ScalaLanguageSettingsManager() {
        loadSettings();
    }

    public static synchronized ScalaLanguageSettingsManager getInstance() {
        if (instance == null) {
            instance = new ScalaLanguageSettingsManager();
        }
        return instance;
    }

    // Editor
    public synchronized ScalaEditorSettings getEditorSettings() {
        return editorSettings.copy();
    }

    public synchronized void setEditorSettings(ScalaEditorSettings settings) {
        this.editorSettings = settings != null ? settings.copy() : new ScalaEditorSettings();
        saveSettings();
        notifyListeners();
    }

    // X-Ray
    public synchronized ScalaXRaySettings getXRaySettings() {
        return xRaySettings.copy();
    }

    public synchronized void setXRaySettings(ScalaXRaySettings settings) {
        this.xRaySettings = settings != null ? settings.copy() : new ScalaXRaySettings();
        saveSettings();
        notifyListeners();
    }

    // Project View
    public synchronized ScalaProjectViewSettings getProjectViewSettings() {
        return projectViewSettings.copy();
    }

    public synchronized void setProjectViewSettings(ScalaProjectViewSettings settings) {
        this.projectViewSettings = settings != null ? settings.copy() : new ScalaProjectViewSettings();
        saveSettings();
        notifyListeners();
    }

    // Performance
    public synchronized ScalaPerformanceSettings getPerformanceSettings() {
        return performanceSettings.copy();
    }

    public synchronized void setPerformanceSettings(ScalaPerformanceSettings settings) {
        this.performanceSettings = settings != null ? settings.copy() : new ScalaPerformanceSettings();
        saveSettings();
        notifyListeners();
    }

    // Worksheet
    public synchronized ScalaWorksheetSettings getWorksheetSettings() {
        return worksheetSettings.copy();
    }

    public synchronized void setWorksheetSettings(ScalaWorksheetSettings settings) {
        this.worksheetSettings = settings != null ? settings.copy() : new ScalaWorksheetSettings();
        saveSettings();
        notifyListeners();
    }

    // Base Package
    public synchronized ScalaBasePackageSettings getBasePackageSettings() {
        return basePackageSettings.copy();
    }

    public synchronized void setBasePackageSettings(ScalaBasePackageSettings settings) {
        this.basePackageSettings = settings != null ? settings.copy() : new ScalaBasePackageSettings();
        saveSettings();
        notifyListeners();
    }

    // Misc
    public synchronized ScalaMiscSettings getMiscSettings() {
        return miscSettings.copy();
    }

    public synchronized void setMiscSettings(ScalaMiscSettings settings) {
        this.miscSettings = settings != null ? settings.copy() : new ScalaMiscSettings();
        saveSettings();
        notifyListeners();
    }

    // Updates
    public synchronized ScalaUpdatesSettings getUpdatesSettings() {
        return updatesSettings.copy();
    }

    public synchronized void setUpdatesSettings(ScalaUpdatesSettings settings) {
        this.updatesSettings = settings != null ? settings.copy() : new ScalaUpdatesSettings();
        saveSettings();
        notifyListeners();
    }

    // Extensions
    public synchronized ScalaExtensionsSettings getExtensionsSettings() {
        return extensionsSettings.copy();
    }

    public synchronized void setExtensionsSettings(ScalaExtensionsSettings settings) {
        this.extensionsSettings = settings != null ? settings.copy() : new ScalaExtensionsSettings();
        saveSettings();
        notifyListeners();
    }

    public synchronized void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public synchronized void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }

    public synchronized void saveSettings() {
        // Save Editor
        Settings.put(KEY_SCALA_EDITOR_HINTS_TYPE_MISMATCH, String.valueOf(editorSettings.isShowHintsOnTypeMismatch()));
        Settings.put(KEY_SCALA_EDITOR_HINTS_NO_IMPLICIT, String.valueOf(editorSettings.isShowHintsIfNoImplicitArgumentsFound()));
        Settings.put(KEY_SCALA_EDITOR_HINTS_AMBIGUOUS_IMPLICIT, String.valueOf(editorSettings.isShowHintsIfAmbiguousImplicitArgumentsFound()));
        Settings.put(KEY_SCALA_EDITOR_EXPORT_ALIASES, editorSettings.getExportAliases());
        Settings.put(KEY_SCALA_EDITOR_HIGHLIGHT_IMPLICIT_CONVERSIONS, String.valueOf(editorSettings.isHighlightImplicitConversions()));
        Settings.put(KEY_SCALA_EDITOR_HIGHLIGHT_BY_NAME_ARGS, String.valueOf(editorSettings.isHighlightArgumentsToByNameParameters()));
        Settings.put(KEY_SCALA_EDITOR_INCLUDE_BLOCK_EXPRESSIONS, String.valueOf(editorSettings.isIncludeBlockExpressions()));
        Settings.put(KEY_SCALA_EDITOR_INCLUDE_LITERALS, String.valueOf(editorSettings.isIncludeLiterals()));
        Settings.put(KEY_SCALA_EDITOR_CUSTOM_SCALATEST_HIGHLIGHTING, String.valueOf(editorSettings.isCustomScalaTestKeywordsHighlighting()));
        Settings.put(KEY_SCALA_EDITOR_COLLECTION_TYPE_HIGHLIGHTING, editorSettings.getCollectionTypeHighlighting());
        Settings.put(KEY_SCALA_EDITOR_AHEAD_OF_TIME_COMPLETION, String.valueOf(editorSettings.isAheadOfTimeCompletion()));
        Settings.put(KEY_SCALA_EDITOR_PRIORITIZE_SCALA_CLASSES, String.valueOf(editorSettings.isUseScalaClassesPriorityOverJavaClasses()));
        Settings.put(KEY_SCALA_EDITOR_CONVERT_JAVA_ON_PASTE, String.valueOf(editorSettings.isConvertJavaCodeToScalaOnCopyPaste()));
        Settings.put(KEY_SCALA_EDITOR_DONT_SHOW_PASTE_DIALOG, String.valueOf(editorSettings.isDontShowDialogOnPasteAndAutomaticallyConvert()));
        Settings.put(KEY_SCALA_EDITOR_ADD_OVERRIDE_KEYWORD, String.valueOf(editorSettings.isAddOverrideKeywordToMethodImplementation()));

        // Save X-Ray
        Settings.put(KEY_SCALA_XRAY_DOUBLE_PRESS_CTRL, String.valueOf(xRaySettings.isDoublePressAndHoldCtrl()));
        Settings.put(KEY_SCALA_XRAY_PRESS_CTRL, String.valueOf(xRaySettings.isPressAndHoldCtrl()));
        Settings.put(KEY_SCALA_XRAY_PARAMETER_NAME_HINTS, String.valueOf(xRaySettings.isParameterNameHints()));
        Settings.put(KEY_SCALA_XRAY_FOR_ALL_PARAMETERS, String.valueOf(xRaySettings.isForAllParameters()));
        Settings.put(KEY_SCALA_XRAY_BY_NAME_ARGS, String.valueOf(xRaySettings.isByNameArgumentHints()));
        Settings.put(KEY_SCALA_XRAY_APPLY_METHOD_HINTS, String.valueOf(xRaySettings.isApplyMethodHints()));
        Settings.put(KEY_SCALA_XRAY_TYPE_HINTS, String.valueOf(xRaySettings.isTypeHints()));
        Settings.put(KEY_SCALA_XRAY_MEMBER_VARIABLES, String.valueOf(xRaySettings.isMemberVariables()));
        Settings.put(KEY_SCALA_XRAY_LOCAL_VARIABLES, String.valueOf(xRaySettings.isLocalVariables()));
        Settings.put(KEY_SCALA_XRAY_METHOD_RESULTS, String.valueOf(xRaySettings.isMethodResults()));
        Settings.put(KEY_SCALA_XRAY_LAMBDA_PARAMETERS, String.valueOf(xRaySettings.isLambdaParameters()));
        Settings.put(KEY_SCALA_XRAY_LAMBDA_PLACEHOLDERS, String.valueOf(xRaySettings.isLambdaPlaceholders()));
        Settings.put(KEY_SCALA_XRAY_VARIABLE_PATTERNS, String.valueOf(xRaySettings.isVariablePatterns()));
        Settings.put(KEY_SCALA_XRAY_METHOD_CHAIN_HINTS, String.valueOf(xRaySettings.isMethodChainHints()));
        Settings.put(KEY_SCALA_XRAY_TYPE_ARGUMENTS, String.valueOf(xRaySettings.isTypeArguments()));
        Settings.put(KEY_SCALA_XRAY_IMPLICIT_HINTS, String.valueOf(xRaySettings.isImplicitHints()));
        Settings.put(KEY_SCALA_XRAY_INDENT_GUIDES, String.valueOf(xRaySettings.isIndentGuides()));
        Settings.put(KEY_SCALA_XRAY_METHOD_SEPARATORS, String.valueOf(xRaySettings.isMethodSeparators()));
        Settings.put(KEY_SCALA_XRAY_WIDGET_DISPLAY, xRaySettings.getWidgetDisplay());

        // Save Project View
        Settings.put(KEY_SCALA_PROJECT_VIEW_GROUP_PACKAGE_OBJECT, String.valueOf(projectViewSettings.isGroupPackageObjectWithPackage()));
        Settings.put(KEY_SCALA_PROJECT_VIEW_HIGHLIGHT_NODES_WITH_ERRORS, String.valueOf(projectViewSettings.isHighlightNodesWithErrors()));

        // Save Performance
        Settings.put(KEY_SCALA_PERF_IMPLICIT_DEPTH, String.valueOf(performanceSettings.getImplicitParametersSearchDepth()));
        Settings.put(KEY_SCALA_PERF_META_PROGRAMS, performanceSettings.getScalaMetaProgramsExecution());
        Settings.put(KEY_SCALA_PERF_IVY_CACHE_MODE, performanceSettings.getLocalIvyCacheIndexingMode());
        Settings.put(KEY_SCALA_PERF_TRIM_META_BODIES, String.valueOf(performanceSettings.isTrimMethodBodiesExpandedByScalaMeta()));
        Settings.put(KEY_SCALA_PERF_SEARCH_ALL_SYMBOLS, String.valueOf(performanceSettings.isSearchAllSymbolsIncludeLocals()));
        Settings.put(KEY_SCALA_PERF_DISABLE_DOC_COMMENTS, String.valueOf(performanceSettings.isDisableParsingOfDocComments()));
        Settings.put(KEY_SCALA_PERF_DISABLE_LANGUAGE_INJECTION, String.valueOf(performanceSettings.isDisableLanguageInjectionInScalaFiles()));
        Settings.put(KEY_SCALA_PERF_DONT_CACHE_COMPOUND_TYPES, String.valueOf(performanceSettings.isDontCacheCompoundTypes()));

        // Save Worksheet
        Settings.put(KEY_SCALA_WORKSHEET_TREAT_SC_AS, worksheetSettings.getTreatScFilesAs());
        Settings.put(KEY_SCALA_WORKSHEET_RUN_IN_COMPILER_PROCESS, String.valueOf(worksheetSettings.isRunWorksheetInCompilerProcessPlainMode()));
        Settings.put(KEY_SCALA_WORKSHEET_ECLIPSE_COMPATIBILITY, String.valueOf(worksheetSettings.isUseEclipseCompatibilityMode()));
        Settings.put(KEY_SCALA_WORKSHEET_TREAT_SCRATCH_AS_WORKSHEET, String.valueOf(worksheetSettings.isTreatScalaScratchFilesAsWorksheet()));
        Settings.put(KEY_SCALA_WORKSHEET_COLLAPSE_OUTPUT, String.valueOf(worksheetSettings.isCollapseLongOutputByDefault()));
        Settings.put(KEY_SCALA_WORKSHEET_OUTPUT_CUTOFF, String.valueOf(worksheetSettings.getOutputCutoffLimit()));
        Settings.put(KEY_SCALA_WORKSHEET_DELAY_AUTO_RUN, String.valueOf(worksheetSettings.getDelayBeforeAutoRunMs()));

        // Save Base Package
        Settings.put(KEY_SCALA_BASE_PACKAGE_INHERIT, String.valueOf(basePackageSettings.isInheritFromPackagePrefix()));
        StringBuilder bpBuilder = new StringBuilder();
        for (ScalaBasePackageEntry entry : basePackageSettings.getCustomBasePackages()) {
            if (bpBuilder.length() > 0) bpBuilder.append("###");
            bpBuilder.append(entry.getModule()).append("@@@").append(entry.getBasePackage());
        }
        Settings.put(KEY_SCALA_BASE_PACKAGE_CUSTOM_ENTRIES, bpBuilder.toString());

        // Save Misc
        Settings.put(KEY_SCALA_MISC_SCALATEST_SUPER_CLASS, miscSettings.getScalaTestDefaultSuperClass());
        Settings.put(KEY_SCALA_MISC_TRAILING_COMMAS, miscSettings.getTrailingCommas());
        StringBuilder injBuilder = new StringBuilder();
        for (ScalaInterpolatedStringInjection inj : miscSettings.getInjections()) {
            if (injBuilder.length() > 0) injBuilder.append("###");
            injBuilder.append(inj.getPrefix()).append("@@@").append(inj.getLanguageId());
        }
        Settings.put(KEY_SCALA_MISC_INJECTIONS, injBuilder.toString());

        // Save Updates
        Settings.put(KEY_SCALA_UPDATES_CHANNEL, updatesSettings.getUpdateChannel());
        Settings.put(KEY_SCALA_UPDATES_LAST_CHECKED, String.valueOf(updatesSettings.getLastCheckedTimestamp()));

        // Save Extensions
        Settings.put(KEY_SCALA_EXTENSIONS_ENABLED, String.valueOf(extensionsSettings.isEnableLoadingExternalExtensions()));
        StringBuilder extBuilder = new StringBuilder();
        for (ScalaExtensionLibrary lib : extensionsSettings.getKnownLibraries()) {
            if (extBuilder.length() > 0) extBuilder.append("###");
            extBuilder.append(lib.getName()).append("@@@")
                    .append(lib.getPath()).append("@@@")
                    .append(lib.isEnabled()).append("@@@")
                    .append(String.join(",", lib.getExtensions()));
        }
        Settings.put(KEY_SCALA_EXTENSIONS_LIBRARIES, extBuilder.toString());
    }

    public synchronized void loadSettings() {
        // Load Editor
        ScalaEditorSettings ed = new ScalaEditorSettings();
        String stm = Settings.get(KEY_SCALA_EDITOR_HINTS_TYPE_MISMATCH);
        if (stm != null) ed.setShowHintsOnTypeMismatch(Boolean.parseBoolean(stm));
        String sni = Settings.get(KEY_SCALA_EDITOR_HINTS_NO_IMPLICIT);
        if (sni != null) ed.setShowHintsIfNoImplicitArgumentsFound(Boolean.parseBoolean(sni));
        String sai = Settings.get(KEY_SCALA_EDITOR_HINTS_AMBIGUOUS_IMPLICIT);
        if (sai != null) ed.setShowHintsIfAmbiguousImplicitArgumentsFound(Boolean.parseBoolean(sai));
        String ea = Settings.get(KEY_SCALA_EDITOR_EXPORT_ALIASES);
        if (ea != null) ed.setExportAliases(ea);
        String hic = Settings.get(KEY_SCALA_EDITOR_HIGHLIGHT_IMPLICIT_CONVERSIONS);
        if (hic != null) ed.setHighlightImplicitConversions(Boolean.parseBoolean(hic));
        String hbn = Settings.get(KEY_SCALA_EDITOR_HIGHLIGHT_BY_NAME_ARGS);
        if (hbn != null) ed.setHighlightArgumentsToByNameParameters(Boolean.parseBoolean(hbn));
        String ibe = Settings.get(KEY_SCALA_EDITOR_INCLUDE_BLOCK_EXPRESSIONS);
        if (ibe != null) ed.setIncludeBlockExpressions(Boolean.parseBoolean(ibe));
        String il = Settings.get(KEY_SCALA_EDITOR_INCLUDE_LITERALS);
        if (il != null) ed.setIncludeLiterals(Boolean.parseBoolean(il));
        String cst = Settings.get(KEY_SCALA_EDITOR_CUSTOM_SCALATEST_HIGHLIGHTING);
        if (cst != null) ed.setCustomScalaTestKeywordsHighlighting(Boolean.parseBoolean(cst));
        String cth = Settings.get(KEY_SCALA_EDITOR_COLLECTION_TYPE_HIGHLIGHTING);
        if (cth != null) ed.setCollectionTypeHighlighting(cth);
        String aot = Settings.get(KEY_SCALA_EDITOR_AHEAD_OF_TIME_COMPLETION);
        if (aot != null) ed.setAheadOfTimeCompletion(Boolean.parseBoolean(aot));
        String psc = Settings.get(KEY_SCALA_EDITOR_PRIORITIZE_SCALA_CLASSES);
        if (psc != null) ed.setUseScalaClassesPriorityOverJavaClasses(Boolean.parseBoolean(psc));
        String cjp = Settings.get(KEY_SCALA_EDITOR_CONVERT_JAVA_ON_PASTE);
        if (cjp != null) ed.setConvertJavaCodeToScalaOnCopyPaste(Boolean.parseBoolean(cjp));
        String dsd = Settings.get(KEY_SCALA_EDITOR_DONT_SHOW_PASTE_DIALOG);
        if (dsd != null) ed.setDontShowDialogOnPasteAndAutomaticallyConvert(Boolean.parseBoolean(dsd));
        String aok = Settings.get(KEY_SCALA_EDITOR_ADD_OVERRIDE_KEYWORD);
        if (aok != null) ed.setAddOverrideKeywordToMethodImplementation(Boolean.parseBoolean(aok));
        this.editorSettings = ed;

        // Load X-Ray
        ScalaXRaySettings xr = new ScalaXRaySettings();
        String dpc = Settings.get(KEY_SCALA_XRAY_DOUBLE_PRESS_CTRL);
        if (dpc != null) xr.setDoublePressAndHoldCtrl(Boolean.parseBoolean(dpc));
        String pc = Settings.get(KEY_SCALA_XRAY_PRESS_CTRL);
        if (pc != null) xr.setPressAndHoldCtrl(Boolean.parseBoolean(pc));
        String pnh = Settings.get(KEY_SCALA_XRAY_PARAMETER_NAME_HINTS);
        if (pnh != null) xr.setParameterNameHints(Boolean.parseBoolean(pnh));
        String fap = Settings.get(KEY_SCALA_XRAY_FOR_ALL_PARAMETERS);
        if (fap != null) xr.setForAllParameters(Boolean.parseBoolean(fap));
        String bna = Settings.get(KEY_SCALA_XRAY_BY_NAME_ARGS);
        if (bna != null) xr.setByNameArgumentHints(Boolean.parseBoolean(bna));
        String amh = Settings.get(KEY_SCALA_XRAY_APPLY_METHOD_HINTS);
        if (amh != null) xr.setApplyMethodHints(Boolean.parseBoolean(amh));
        String th = Settings.get(KEY_SCALA_XRAY_TYPE_HINTS);
        if (th != null) xr.setTypeHints(Boolean.parseBoolean(th));
        String mv = Settings.get(KEY_SCALA_XRAY_MEMBER_VARIABLES);
        if (mv != null) xr.setMemberVariables(Boolean.parseBoolean(mv));
        String lv = Settings.get(KEY_SCALA_XRAY_LOCAL_VARIABLES);
        if (lv != null) xr.setLocalVariables(Boolean.parseBoolean(lv));
        String mr = Settings.get(KEY_SCALA_XRAY_METHOD_RESULTS);
        if (mr != null) xr.setMethodResults(Boolean.parseBoolean(mr));
        String lp = Settings.get(KEY_SCALA_XRAY_LAMBDA_PARAMETERS);
        if (lp != null) xr.setLambdaParameters(Boolean.parseBoolean(lp));
        String lph = Settings.get(KEY_SCALA_XRAY_LAMBDA_PLACEHOLDERS);
        if (lph != null) xr.setLambdaPlaceholders(Boolean.parseBoolean(lph));
        String vp = Settings.get(KEY_SCALA_XRAY_VARIABLE_PATTERNS);
        if (vp != null) xr.setVariablePatterns(Boolean.parseBoolean(vp));
        String mch = Settings.get(KEY_SCALA_XRAY_METHOD_CHAIN_HINTS);
        if (mch != null) xr.setMethodChainHints(Boolean.parseBoolean(mch));
        String ta = Settings.get(KEY_SCALA_XRAY_TYPE_ARGUMENTS);
        if (ta != null) xr.setTypeArguments(Boolean.parseBoolean(ta));
        String ih = Settings.get(KEY_SCALA_XRAY_IMPLICIT_HINTS);
        if (ih != null) xr.setImplicitHints(Boolean.parseBoolean(ih));
        String ig = Settings.get(KEY_SCALA_XRAY_INDENT_GUIDES);
        if (ig != null) xr.setIndentGuides(Boolean.parseBoolean(ig));
        String ms = Settings.get(KEY_SCALA_XRAY_METHOD_SEPARATORS);
        if (ms != null) xr.setMethodSeparators(Boolean.parseBoolean(ms));
        String wd = Settings.get(KEY_SCALA_XRAY_WIDGET_DISPLAY);
        if (wd != null) xr.setWidgetDisplay(wd);
        this.xRaySettings = xr;

        // Load Project View
        ScalaProjectViewSettings pv = new ScalaProjectViewSettings();
        String gpo = Settings.get(KEY_SCALA_PROJECT_VIEW_GROUP_PACKAGE_OBJECT);
        if (gpo != null) pv.setGroupPackageObjectWithPackage(Boolean.parseBoolean(gpo));
        String hne = Settings.get(KEY_SCALA_PROJECT_VIEW_HIGHLIGHT_NODES_WITH_ERRORS);
        if (hne != null) pv.setHighlightNodesWithErrors(Boolean.parseBoolean(hne));
        this.projectViewSettings = pv;

        // Load Performance
        ScalaPerformanceSettings pf = new ScalaPerformanceSettings();
        String psd = Settings.get(KEY_SCALA_PERF_IMPLICIT_DEPTH);
        if (psd != null) {
            try { pf.setImplicitParametersSearchDepth(Integer.parseInt(psd)); } catch (NumberFormatException ignored) {}
        }
        String smp = Settings.get(KEY_SCALA_PERF_META_PROGRAMS);
        if (smp != null) pf.setScalaMetaProgramsExecution(smp);
        String lic = Settings.get(KEY_SCALA_PERF_IVY_CACHE_MODE);
        if (lic != null) pf.setLocalIvyCacheIndexingMode(lic);
        String tmb = Settings.get(KEY_SCALA_PERF_TRIM_META_BODIES);
        if (tmb != null) pf.setTrimMethodBodiesExpandedByScalaMeta(Boolean.parseBoolean(tmb));
        String sas = Settings.get(KEY_SCALA_PERF_SEARCH_ALL_SYMBOLS);
        if (sas != null) pf.setSearchAllSymbolsIncludeLocals(Boolean.parseBoolean(sas));
        String dpd = Settings.get(KEY_SCALA_PERF_DISABLE_DOC_COMMENTS);
        if (dpd != null) pf.setDisableParsingOfDocComments(Boolean.parseBoolean(dpd));
        String dli = Settings.get(KEY_SCALA_PERF_DISABLE_LANGUAGE_INJECTION);
        if (dli != null) pf.setDisableLanguageInjectionInScalaFiles(Boolean.parseBoolean(dli));
        String dcc = Settings.get(KEY_SCALA_PERF_DONT_CACHE_COMPOUND_TYPES);
        if (dcc != null) pf.setDontCacheCompoundTypes(Boolean.parseBoolean(dcc));
        this.performanceSettings = pf;

        // Load Worksheet
        ScalaWorksheetSettings ws = new ScalaWorksheetSettings();
        String tsc = Settings.get(KEY_SCALA_WORKSHEET_TREAT_SC_AS);
        if (tsc != null) ws.setTreatScFilesAs(tsc);
        String rwc = Settings.get(KEY_SCALA_WORKSHEET_RUN_IN_COMPILER_PROCESS);
        if (rwc != null) ws.setRunWorksheetInCompilerProcessPlainMode(Boolean.parseBoolean(rwc));
        String uec = Settings.get(KEY_SCALA_WORKSHEET_ECLIPSE_COMPATIBILITY);
        if (uec != null) ws.setUseEclipseCompatibilityMode(Boolean.parseBoolean(uec));
        String tss = Settings.get(KEY_SCALA_WORKSHEET_TREAT_SCRATCH_AS_WORKSHEET);
        if (tss != null) ws.setTreatScalaScratchFilesAsWorksheet(Boolean.parseBoolean(tss));
        String clo = Settings.get(KEY_SCALA_WORKSHEET_COLLAPSE_OUTPUT);
        if (clo != null) ws.setCollapseLongOutputByDefault(Boolean.parseBoolean(clo));
        String ocl = Settings.get(KEY_SCALA_WORKSHEET_OUTPUT_CUTOFF);
        if (ocl != null) {
            try { ws.setOutputCutoffLimit(Integer.parseInt(ocl)); } catch (NumberFormatException ignored) {}
        }
        String dba = Settings.get(KEY_SCALA_WORKSHEET_DELAY_AUTO_RUN);
        if (dba != null) {
            try { ws.setDelayBeforeAutoRunMs(Integer.parseInt(dba)); } catch (NumberFormatException ignored) {}
        }
        this.worksheetSettings = ws;

        // Load Base Package
        ScalaBasePackageSettings bp = new ScalaBasePackageSettings();
        String bpi = Settings.get(KEY_SCALA_BASE_PACKAGE_INHERIT);
        if (bpi != null) bp.setInheritFromPackagePrefix(Boolean.parseBoolean(bpi));
        String bpe = Settings.get(KEY_SCALA_BASE_PACKAGE_CUSTOM_ENTRIES);
        if (bpe != null && !bpe.trim().isEmpty()) {
            List<ScalaBasePackageEntry> list = new ArrayList<>();
            String[] entries = bpe.split("###");
            for (String entryStr : entries) {
                String[] parts = entryStr.split("@@@", 2);
                if (parts.length == 2) {
                    list.add(new ScalaBasePackageEntry(parts[0], parts[1]));
                }
            }
            bp.setCustomBasePackages(list);
        }
        this.basePackageSettings = bp;

        // Load Misc
        ScalaMiscSettings misc = new ScalaMiscSettings();
        String ssc = Settings.get(KEY_SCALA_MISC_SCALATEST_SUPER_CLASS);
        if (ssc != null) misc.setScalaTestDefaultSuperClass(ssc);
        String tc = Settings.get(KEY_SCALA_MISC_TRAILING_COMMAS);
        if (tc != null) misc.setTrailingCommas(tc);
        String inj = Settings.get(KEY_SCALA_MISC_INJECTIONS);
        if (inj != null && !inj.trim().isEmpty()) {
            List<ScalaInterpolatedStringInjection> list = new ArrayList<>();
            String[] entries = inj.split("###");
            for (String entryStr : entries) {
                String[] parts = entryStr.split("@@@", 2);
                if (parts.length == 2) {
                    list.add(new ScalaInterpolatedStringInjection(parts[0], parts[1]));
                }
            }
            misc.setInjections(list);
        }
        this.miscSettings = misc;

        // Load Updates
        ScalaUpdatesSettings upd = new ScalaUpdatesSettings();
        String upc = Settings.get(KEY_SCALA_UPDATES_CHANNEL);
        if (upc != null) upd.setUpdateChannel(upc);
        String upl = Settings.get(KEY_SCALA_UPDATES_LAST_CHECKED);
        if (upl != null) {
            try { upd.setLastCheckedTimestamp(Long.parseLong(upl)); } catch (NumberFormatException ignored) {}
        }
        this.updatesSettings = upd;

        // Load Extensions
        ScalaExtensionsSettings ext = new ScalaExtensionsSettings();
        String ete = Settings.get(KEY_SCALA_EXTENSIONS_ENABLED);
        if (ete != null) ext.setEnableLoadingExternalExtensions(Boolean.parseBoolean(ete));
        String etl = Settings.get(KEY_SCALA_EXTENSIONS_LIBRARIES);
        if (etl != null && !etl.trim().isEmpty()) {
            List<ScalaExtensionLibrary> list = new ArrayList<>();
            String[] libEntries = etl.split("###");
            for (String entryStr : libEntries) {
                String[] parts = entryStr.split("@@@", 4);
                if (parts.length >= 3) {
                    List<String> extensions = new ArrayList<>();
                    if (parts.length == 4 && !parts[3].isEmpty()) {
                        extensions = Arrays.asList(parts[3].split(","));
                    }
                    list.add(new ScalaExtensionLibrary(parts[0], parts[1], Boolean.parseBoolean(parts[2]), extensions));
                }
            }
            ext.setKnownLibraries(list);
        }
        this.extensionsSettings = ext;
    }

    public synchronized void resetDefaults() {
        this.editorSettings = new ScalaEditorSettings();
        this.xRaySettings = new ScalaXRaySettings();
        this.projectViewSettings = new ScalaProjectViewSettings();
        this.performanceSettings = new ScalaPerformanceSettings();
        this.worksheetSettings = new ScalaWorksheetSettings();
        this.basePackageSettings = new ScalaBasePackageSettings();
        this.miscSettings = new ScalaMiscSettings();
        this.updatesSettings = new ScalaUpdatesSettings();
        this.extensionsSettings = new ScalaExtensionsSettings();
        saveSettings();
        notifyListeners();
    }
}
