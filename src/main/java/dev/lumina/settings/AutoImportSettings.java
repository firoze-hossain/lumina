package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Dynamic persistent configuration and model for IntelliJ IDEA-style Editor > General > Auto Import settings.
 * Backed by ~/.lumina/lumina.properties, supporting dynamic listeners,
 * real-time UI synchronization, and language-specific import management.
 */
public final class AutoImportSettings {

    private static final AutoImportSettings INSTANCE = new AutoImportSettings();

    public static AutoImportSettings getInstance() {
        return INSTANCE;
    }

    public enum InsertImportsMode {
        ALWAYS("Always"),
        ASK("Ask"),
        NEVER("Never");

        private final String label;
        InsertImportsMode(String label) { this.label = label; }
        public String getLabel() { return label; }

        public static InsertImportsMode fromLabel(String label) {
            for (InsertImportsMode mode : values()) {
                if (mode.label.equalsIgnoreCase(label) || mode.name().equalsIgnoreCase(label)) {
                    return mode;
                }
            }
            return ASK;
        }
    }

    public enum PythonImportStyle {
        FROM_MODULE_IMPORT_NAME("from <module> import <name>"),
        IMPORT_MODULE_NAME("import <module>.<name>");

        private final String label;
        PythonImportStyle(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum RustApplyTo {
        METHODS_ONLY("Methods only"),
        EVERYTHING("Everything");

        private final String label;
        RustApplyTo(String label) { this.label = label; }
        public String getLabel() { return label; }

        public static RustApplyTo fromLabel(String label) {
            for (RustApplyTo val : values()) {
                if (val.label.equalsIgnoreCase(label) || val.name().equalsIgnoreCase(label)) {
                    return val;
                }
            }
            return METHODS_ONLY;
        }
    }

    public enum RustScope {
        IDE("IDE"),
        PROJECT("Project");

        private final String label;
        RustScope(String label) { this.label = label; }
        public String getLabel() { return label; }

        public static RustScope fromLabel(String label) {
            for (RustScope val : values()) {
                if (val.label.equalsIgnoreCase(label) || val.name().equalsIgnoreCase(label)) {
                    return val;
                }
            }
            return IDE;
        }
    }

    public static class JavaImportEntry {
        private String item;
        private RustScope scope;

        public JavaImportEntry(String item, RustScope scope) {
            this.item = item != null ? item : "";
            this.scope = scope != null ? scope : RustScope.IDE;
        }

        public String getItem() { return item; }
        public void setItem(String item) { this.item = item != null ? item : ""; }

        public RustScope getScope() { return scope; }
        public void setScope(RustScope scope) { this.scope = scope != null ? scope : RustScope.IDE; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JavaImportEntry that = (JavaImportEntry) o;
            return Objects.equals(item, that.item) && scope == that.scope;
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, scope);
        }

        @Override
        public String toString() {
            return item + "|" + scope.name();
        }

        public static JavaImportEntry fromString(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] parts = raw.split("\\|", -1);
            if (parts.length >= 2) {
                return new JavaImportEntry(parts[0].trim(), RustScope.valueOf(parts[1].trim()));
            } else if (parts.length == 1) {
                return new JavaImportEntry(parts[0].trim(), RustScope.IDE);
            }
            return null;
        }
    }

    public static class RustExcludeEntry {
        private String itemOrModule;
        private RustApplyTo applyTo;
        private RustScope scope;

        public RustExcludeEntry(String itemOrModule, RustApplyTo applyTo, RustScope scope) {
            this.itemOrModule = itemOrModule != null ? itemOrModule : "";
            this.applyTo = applyTo != null ? applyTo : RustApplyTo.METHODS_ONLY;
            this.scope = scope != null ? scope : RustScope.IDE;
        }

        public String getItemOrModule() { return itemOrModule; }
        public void setItemOrModule(String itemOrModule) { this.itemOrModule = itemOrModule != null ? itemOrModule : ""; }

        public RustApplyTo getApplyTo() { return applyTo; }
        public void setApplyTo(RustApplyTo applyTo) { this.applyTo = applyTo != null ? applyTo : RustApplyTo.METHODS_ONLY; }

        public RustScope getScope() { return scope; }
        public void setScope(RustScope scope) { this.scope = scope != null ? scope : RustScope.IDE; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RustExcludeEntry that = (RustExcludeEntry) o;
            return Objects.equals(itemOrModule, that.itemOrModule) &&
                    applyTo == that.applyTo &&
                    scope == that.scope;
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemOrModule, applyTo, scope);
        }

        @Override
        public String toString() {
            return itemOrModule + "|" + applyTo.name() + "|" + scope.name();
        }

        public static RustExcludeEntry fromString(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] parts = raw.split("\\|", -1);
            if (parts.length >= 3) {
                return new RustExcludeEntry(
                        parts[0].trim(),
                        RustApplyTo.valueOf(parts[1].trim()),
                        RustScope.valueOf(parts[2].trim())
                );
            } else if (parts.length == 1) {
                return new RustExcludeEntry(parts[0].trim(), RustApplyTo.METHODS_ONLY, RustScope.IDE);
            }
            return null;
        }
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(AutoImportSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // 1. XML
    private boolean xmlShowAutoImportTooltip = true;

    // 2. Java
    private boolean javaShowTooltipClasses = true;
    private boolean javaShowTooltipStaticMethods = true;
    private InsertImportsMode javaInsertImportsOnPaste = InsertImportsMode.ALWAYS;
    private boolean javaAddUnambiguousImportsOnTheFly = false;
    private boolean javaOptimizeImportsOnTheFly = false;
    private final List<JavaImportEntry> javaIncludeStaticMembers = new ArrayList<>();
    private final List<JavaImportEntry> javaExcludeFromAutoImport = new ArrayList<>();

    // 3. Python
    private boolean pythonShowAutoImportTooltip = false;
    private PythonImportStyle pythonPreferredImportStyle = PythonImportStyle.FROM_MODULE_IMPORT_NAME;

    // 4. Rust
    private boolean rustShowImportPopup = false;
    private boolean rustImportOutOfScopeItems = true;
    private boolean rustInsertImportsOnPaste = true;
    private InsertImportsMode rustAddCrateDependenciesOnPaste = InsertImportsMode.ASK;
    private boolean rustAddUnambiguousImportsOnTheFly = false;
    private final List<RustExcludeEntry> rustExcludeEntries = new ArrayList<>();

    // 5. Scala
    private InsertImportsMode scalaInsertImportsOnPaste = InsertImportsMode.ASK;
    private boolean scalaShowPopupClasses = true;
    private boolean scalaShowPopupStaticMembers = true;
    private boolean scalaShowPopupImplicitConversions = true;
    private boolean scalaShowPopupImplicitDefinitions = true;
    private boolean scalaShowPopupExtensionMethods = true;
    private boolean scalaAddUnambiguousClasses = false;
    private boolean scalaAddUnambiguousStaticMembers = false;
    private boolean scalaOptimizeImportsOnTheFly = false;

    // 6. JSP
    private boolean jspAddUnambiguousImportsOnTheFly = false;

    // 7. Kotlin
    private boolean kotlinAddUnambiguousImportsOnTheFly = false;
    private boolean kotlinOptimizeImportsOnTheFly = false;

    // 8. Ktor
    private boolean ktorAddImportsAutomatically = true;

    // 9. TypeScript / JavaScript
    private boolean jsAddImportsAutomatically = true;
    private boolean jsOnCodeCompletion = true;
    private boolean jsWithAutoImportTooltip = true;
    private boolean tsAddImportsAutomatically = true;
    private boolean tsOnCodeCompletion = true;
    private boolean tsWithAutoImportTooltip = true;
    private boolean tsUnambiguousImportsOnTheFly = false;

    // 10. PHP
    private InsertImportsMode phpInsertImportsOnPaste = InsertImportsMode.ASK;
    private boolean phpEnableInFileScope = false;
    private boolean phpEnableInNamespaceScope = true;
    private String phpClassGlobalSymbol = "prefer FQN";
    private String phpFunctionGlobalSymbol = "prefer fallback";
    private String phpConstantGlobalSymbol = "prefer fallback";

    public AutoImportSettings() {
        initDefaults();
        load();
    }

    private void initDefaults() {
        xmlShowAutoImportTooltip = true;

        javaShowTooltipClasses = true;
        javaShowTooltipStaticMethods = true;
        javaInsertImportsOnPaste = InsertImportsMode.ALWAYS;
        javaAddUnambiguousImportsOnTheFly = false;
        javaOptimizeImportsOnTheFly = false;
        javaIncludeStaticMembers.clear();
        javaExcludeFromAutoImport.clear();

        pythonShowAutoImportTooltip = false;
        pythonPreferredImportStyle = PythonImportStyle.FROM_MODULE_IMPORT_NAME;

        rustShowImportPopup = false;
        rustImportOutOfScopeItems = true;
        rustInsertImportsOnPaste = true;
        rustAddCrateDependenciesOnPaste = InsertImportsMode.ASK;
        rustAddUnambiguousImportsOnTheFly = false;
        initDefaultRustExclusions();

        scalaInsertImportsOnPaste = InsertImportsMode.ASK;
        scalaShowPopupClasses = true;
        scalaShowPopupStaticMembers = true;
        scalaShowPopupImplicitConversions = true;
        scalaShowPopupImplicitDefinitions = true;
        scalaShowPopupExtensionMethods = true;
        scalaAddUnambiguousClasses = false;
        scalaAddUnambiguousStaticMembers = false;
        scalaOptimizeImportsOnTheFly = false;

        jspAddUnambiguousImportsOnTheFly = false;

        kotlinAddUnambiguousImportsOnTheFly = false;
        kotlinOptimizeImportsOnTheFly = false;

        ktorAddImportsAutomatically = true;

        jsAddImportsAutomatically = true;
        jsOnCodeCompletion = true;
        jsWithAutoImportTooltip = true;

        tsAddImportsAutomatically = true;
        tsOnCodeCompletion = true;
        tsWithAutoImportTooltip = true;
        tsUnambiguousImportsOnTheFly = false;

        phpInsertImportsOnPaste = InsertImportsMode.ASK;
        phpEnableInFileScope = false;
        phpEnableInNamespaceScope = true;
        phpClassGlobalSymbol = "prefer FQN";
        phpFunctionGlobalSymbol = "prefer fallback";
        phpConstantGlobalSymbol = "prefer fallback";
    }

    public void initDefaultRustExclusions() {
        rustExcludeEntries.clear();
        rustExcludeEntries.add(new RustExcludeEntry("std::borrow::Borrow", RustApplyTo.METHODS_ONLY, RustScope.IDE));
        rustExcludeEntries.add(new RustExcludeEntry("std::borrow::BorrowMut", RustApplyTo.METHODS_ONLY, RustScope.IDE));
        rustExcludeEntries.add(new RustExcludeEntry("core::borrow::Borrow", RustApplyTo.METHODS_ONLY, RustScope.IDE));
        rustExcludeEntries.add(new RustExcludeEntry("core::borrow::BorrowMut", RustApplyTo.METHODS_ONLY, RustScope.IDE));
        rustExcludeEntries.add(new RustExcludeEntry("alloc::borrow::Borrow", RustApplyTo.METHODS_ONLY, RustScope.IDE));
        rustExcludeEntries.add(new RustExcludeEntry("alloc::borrow::BorrowMut", RustApplyTo.METHODS_ONLY, RustScope.IDE));
        rustExcludeEntries.add(new RustExcludeEntry("core::panicking::*", RustApplyTo.EVERYTHING, RustScope.IDE));
        rustExcludeEntries.add(new RustExcludeEntry("std::intrinsics::unreachable", RustApplyTo.EVERYTHING, RustScope.IDE));
    }

    public synchronized void resetToDefaults() {
        initDefaults();
        fireChanged();
    }

    public synchronized void load() {
        String val;

        // XML
        val = Settings.get("editor.autoimport.xml.showTooltip");
        if (val != null) xmlShowAutoImportTooltip = Boolean.parseBoolean(val);

        // Java
        val = Settings.get("editor.autoimport.java.tooltipClasses");
        if (val != null) javaShowTooltipClasses = Boolean.parseBoolean(val);

        val = Settings.get("editor.autoimport.java.tooltipStatic");
        if (val != null) javaShowTooltipStaticMethods = Boolean.parseBoolean(val);

        val = Settings.get("editor.autoimport.java.pasteMode");
        if (val != null) {
            try { javaInsertImportsOnPaste = InsertImportsMode.valueOf(val); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.autoimport.java.unambiguousOnFly");
        if (val != null) javaAddUnambiguousImportsOnTheFly = Boolean.parseBoolean(val);

        val = Settings.get("editor.autoimport.java.optimizeOnFly");
        if (val != null) javaOptimizeImportsOnTheFly = Boolean.parseBoolean(val);

        val = Settings.get("editor.autoimport.java.includeStaticMembers");
        if (val != null) {
            javaIncludeStaticMembers.clear();
            if (!val.isBlank()) {
                for (String s : val.split(";")) {
                    JavaImportEntry e = JavaImportEntry.fromString(s);
                    if (e != null && !e.getItem().isBlank()) javaIncludeStaticMembers.add(e);
                }
            }
        }

        val = Settings.get("editor.autoimport.java.exclude");
        if (val != null) {
            javaExcludeFromAutoImport.clear();
            if (!val.isBlank()) {
                for (String s : val.split(";")) {
                    JavaImportEntry e = JavaImportEntry.fromString(s);
                    if (e != null && !e.getItem().isBlank()) javaExcludeFromAutoImport.add(e);
                }
            }
        }

        // Python
        val = Settings.get("editor.autoimport.python.showTooltip");
        if (val != null) pythonShowAutoImportTooltip = Boolean.parseBoolean(val);

        val = Settings.get("editor.autoimport.python.style");
        if (val != null) {
            try { pythonPreferredImportStyle = PythonImportStyle.valueOf(val); } catch (Exception ignored) {}
        }

        // Rust
        val = Settings.get("editor.autoimport.rust.showPopup");
        if (val != null) rustShowImportPopup = Boolean.parseBoolean(val);

        val = Settings.get("editor.autoimport.rust.outOfScope");
        if (val != null) rustImportOutOfScopeItems = Boolean.parseBoolean(val);

        val = Settings.get("editor.autoimport.rust.paste");
        if (val != null) rustInsertImportsOnPaste = Boolean.parseBoolean(val);

        val = Settings.get("editor.autoimport.rust.crateDependencies");
        if (val != null) {
            try { rustAddCrateDependenciesOnPaste = InsertImportsMode.valueOf(val); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.autoimport.rust.unambiguousOnFly");
        if (val != null) rustAddUnambiguousImportsOnTheFly = Boolean.parseBoolean(val);

        val = Settings.get("editor.autoimport.rust.exclude");
        if (val != null && !val.isBlank()) {
            rustExcludeEntries.clear();
            for (String entryRaw : val.split(";")) {
                RustExcludeEntry e = RustExcludeEntry.fromString(entryRaw);
                if (e != null && !e.getItemOrModule().isBlank()) rustExcludeEntries.add(e);
            }
        }
        if (rustExcludeEntries.isEmpty()) {
            initDefaultRustExclusions();
        }

        // Scala
        val = Settings.get("editor.autoimport.scala.pasteMode");
        if (val != null) {
            try { scalaInsertImportsOnPaste = InsertImportsMode.valueOf(val); } catch (Exception ignored) {}
        }
        val = Settings.get("editor.autoimport.scala.popupClasses");
        if (val != null) scalaShowPopupClasses = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.scala.popupStatic");
        if (val != null) scalaShowPopupStaticMembers = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.scala.popupConversions");
        if (val != null) scalaShowPopupImplicitConversions = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.scala.popupDefinitions");
        if (val != null) scalaShowPopupImplicitDefinitions = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.scala.popupExtensions");
        if (val != null) scalaShowPopupExtensionMethods = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.scala.unambiguousClasses");
        if (val != null) scalaAddUnambiguousClasses = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.scala.unambiguousStatic");
        if (val != null) scalaAddUnambiguousStaticMembers = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.scala.optimizeOnFly");
        if (val != null) scalaOptimizeImportsOnTheFly = Boolean.parseBoolean(val);

        // JSP
        val = Settings.get("editor.autoimport.jsp.unambiguousOnFly");
        if (val != null) jspAddUnambiguousImportsOnTheFly = Boolean.parseBoolean(val);

        // Kotlin
        val = Settings.get("editor.autoimport.kotlin.unambiguousOnFly");
        if (val != null) kotlinAddUnambiguousImportsOnTheFly = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.kotlin.optimizeOnFly");
        if (val != null) kotlinOptimizeImportsOnTheFly = Boolean.parseBoolean(val);

        // Ktor
        val = Settings.get("editor.autoimport.ktor.autoImports");
        if (val != null) ktorAddImportsAutomatically = Boolean.parseBoolean(val);

        // TypeScript / JavaScript
        val = Settings.get("editor.autoimport.js.autoImports");
        if (val != null) jsAddImportsAutomatically = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.js.onCompletion");
        if (val != null) jsOnCodeCompletion = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.js.withTooltip");
        if (val != null) jsWithAutoImportTooltip = Boolean.parseBoolean(val);

        val = Settings.get("editor.autoimport.ts.autoImports");
        if (val != null) tsAddImportsAutomatically = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.ts.onCompletion");
        if (val != null) tsOnCodeCompletion = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.ts.withTooltip");
        if (val != null) tsWithAutoImportTooltip = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.ts.unambiguousOnFly");
        if (val != null) tsUnambiguousImportsOnTheFly = Boolean.parseBoolean(val);

        // PHP
        val = Settings.get("editor.autoimport.php.pasteMode");
        if (val != null) {
            try { phpInsertImportsOnPaste = InsertImportsMode.valueOf(val); } catch (Exception ignored) {}
        }
        val = Settings.get("editor.autoimport.php.fileScope");
        if (val != null) phpEnableInFileScope = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.php.namespaceScope");
        if (val != null) phpEnableInNamespaceScope = Boolean.parseBoolean(val);
        val = Settings.get("editor.autoimport.php.classGlobal");
        if (val != null && !val.isBlank()) phpClassGlobalSymbol = val;
        val = Settings.get("editor.autoimport.php.functionGlobal");
        if (val != null && !val.isBlank()) phpFunctionGlobalSymbol = val;
        val = Settings.get("editor.autoimport.php.constantGlobal");
        if (val != null && !val.isBlank()) phpConstantGlobalSymbol = val;
    }

    public synchronized void save() {
        // XML
        Settings.put("editor.autoimport.xml.showTooltip", String.valueOf(xmlShowAutoImportTooltip));

        // Java
        Settings.put("editor.autoimport.java.tooltipClasses", String.valueOf(javaShowTooltipClasses));
        Settings.put("editor.autoimport.java.tooltipStatic", String.valueOf(javaShowTooltipStaticMethods));
        Settings.put("editor.autoimport.java.pasteMode", javaInsertImportsOnPaste.name());
        Settings.put("editor.autoimport.java.unambiguousOnFly", String.valueOf(javaAddUnambiguousImportsOnTheFly));
        Settings.put("editor.autoimport.java.optimizeOnFly", String.valueOf(javaOptimizeImportsOnTheFly));

        List<String> javaStaticParts = new ArrayList<>();
        for (JavaImportEntry e : javaIncludeStaticMembers) javaStaticParts.add(e.toString());
        Settings.put("editor.autoimport.java.includeStaticMembers", String.join(";", javaStaticParts));

        List<String> javaExcludeParts = new ArrayList<>();
        for (JavaImportEntry e : javaExcludeFromAutoImport) javaExcludeParts.add(e.toString());
        Settings.put("editor.autoimport.java.exclude", String.join(";", javaExcludeParts));

        // Python
        Settings.put("editor.autoimport.python.showTooltip", String.valueOf(pythonShowAutoImportTooltip));
        Settings.put("editor.autoimport.python.style", pythonPreferredImportStyle.name());

        // Rust
        Settings.put("editor.autoimport.rust.showPopup", String.valueOf(rustShowImportPopup));
        Settings.put("editor.autoimport.rust.outOfScope", String.valueOf(rustImportOutOfScopeItems));
        Settings.put("editor.autoimport.rust.paste", String.valueOf(rustInsertImportsOnPaste));
        Settings.put("editor.autoimport.rust.crateDependencies", rustAddCrateDependenciesOnPaste.name());
        Settings.put("editor.autoimport.rust.unambiguousOnFly", String.valueOf(rustAddUnambiguousImportsOnTheFly));
        List<String> rustParts = new ArrayList<>();
        for (RustExcludeEntry e : rustExcludeEntries) rustParts.add(e.toString());
        Settings.put("editor.autoimport.rust.exclude", String.join(";", rustParts));

        // Scala
        Settings.put("editor.autoimport.scala.pasteMode", scalaInsertImportsOnPaste.name());
        Settings.put("editor.autoimport.scala.popupClasses", String.valueOf(scalaShowPopupClasses));
        Settings.put("editor.autoimport.scala.popupStatic", String.valueOf(scalaShowPopupStaticMembers));
        Settings.put("editor.autoimport.scala.popupConversions", String.valueOf(scalaShowPopupImplicitConversions));
        Settings.put("editor.autoimport.scala.popupDefinitions", String.valueOf(scalaShowPopupImplicitDefinitions));
        Settings.put("editor.autoimport.scala.popupExtensions", String.valueOf(scalaShowPopupExtensionMethods));
        Settings.put("editor.autoimport.scala.unambiguousClasses", String.valueOf(scalaAddUnambiguousClasses));
        Settings.put("editor.autoimport.scala.unambiguousStatic", String.valueOf(scalaAddUnambiguousStaticMembers));
        Settings.put("editor.autoimport.scala.optimizeOnFly", String.valueOf(scalaOptimizeImportsOnTheFly));

        // JSP
        Settings.put("editor.autoimport.jsp.unambiguousOnFly", String.valueOf(jspAddUnambiguousImportsOnTheFly));

        // Kotlin
        Settings.put("editor.autoimport.kotlin.unambiguousOnFly", String.valueOf(kotlinAddUnambiguousImportsOnTheFly));
        Settings.put("editor.autoimport.kotlin.optimizeOnFly", String.valueOf(kotlinOptimizeImportsOnTheFly));

        // Ktor
        Settings.put("editor.autoimport.ktor.autoImports", String.valueOf(ktorAddImportsAutomatically));

        // TypeScript / JavaScript
        Settings.put("editor.autoimport.js.autoImports", String.valueOf(jsAddImportsAutomatically));
        Settings.put("editor.autoimport.js.onCompletion", String.valueOf(jsOnCodeCompletion));
        Settings.put("editor.autoimport.js.withTooltip", String.valueOf(jsWithAutoImportTooltip));

        Settings.put("editor.autoimport.ts.autoImports", String.valueOf(tsAddImportsAutomatically));
        Settings.put("editor.autoimport.ts.onCompletion", String.valueOf(tsOnCodeCompletion));
        Settings.put("editor.autoimport.ts.withTooltip", String.valueOf(tsWithAutoImportTooltip));
        Settings.put("editor.autoimport.ts.unambiguousOnFly", String.valueOf(tsUnambiguousImportsOnTheFly));

        // PHP
        Settings.put("editor.autoimport.php.pasteMode", phpInsertImportsOnPaste.name());
        Settings.put("editor.autoimport.php.fileScope", String.valueOf(phpEnableInFileScope));
        Settings.put("editor.autoimport.php.namespaceScope", String.valueOf(phpEnableInNamespaceScope));
        Settings.put("editor.autoimport.php.classGlobal", phpClassGlobalSymbol);
        Settings.put("editor.autoimport.php.functionGlobal", phpFunctionGlobalSymbol);
        Settings.put("editor.autoimport.php.constantGlobal", phpConstantGlobalSymbol);

        fireChanged();
    }

    public synchronized void copyFrom(AutoImportSettings o) {
        this.xmlShowAutoImportTooltip = o.xmlShowAutoImportTooltip;

        this.javaShowTooltipClasses = o.javaShowTooltipClasses;
        this.javaShowTooltipStaticMethods = o.javaShowTooltipStaticMethods;
        this.javaInsertImportsOnPaste = o.javaInsertImportsOnPaste;
        this.javaAddUnambiguousImportsOnTheFly = o.javaAddUnambiguousImportsOnTheFly;
        this.javaOptimizeImportsOnTheFly = o.javaOptimizeImportsOnTheFly;
        this.javaIncludeStaticMembers.clear();
        for (JavaImportEntry e : o.javaIncludeStaticMembers) {
            this.javaIncludeStaticMembers.add(new JavaImportEntry(e.getItem(), e.getScope()));
        }
        this.javaExcludeFromAutoImport.clear();
        for (JavaImportEntry e : o.javaExcludeFromAutoImport) {
            this.javaExcludeFromAutoImport.add(new JavaImportEntry(e.getItem(), e.getScope()));
        }

        this.pythonShowAutoImportTooltip = o.pythonShowAutoImportTooltip;
        this.pythonPreferredImportStyle = o.pythonPreferredImportStyle;

        this.rustShowImportPopup = o.rustShowImportPopup;
        this.rustImportOutOfScopeItems = o.rustImportOutOfScopeItems;
        this.rustInsertImportsOnPaste = o.rustInsertImportsOnPaste;
        this.rustAddCrateDependenciesOnPaste = o.rustAddCrateDependenciesOnPaste;
        this.rustAddUnambiguousImportsOnTheFly = o.rustAddUnambiguousImportsOnTheFly;
        this.rustExcludeEntries.clear();
        for (RustExcludeEntry e : o.rustExcludeEntries) {
            this.rustExcludeEntries.add(new RustExcludeEntry(e.getItemOrModule(), e.getApplyTo(), e.getScope()));
        }

        this.scalaInsertImportsOnPaste = o.scalaInsertImportsOnPaste;
        this.scalaShowPopupClasses = o.scalaShowPopupClasses;
        this.scalaShowPopupStaticMembers = o.scalaShowPopupStaticMembers;
        this.scalaShowPopupImplicitConversions = o.scalaShowPopupImplicitConversions;
        this.scalaShowPopupImplicitDefinitions = o.scalaShowPopupImplicitDefinitions;
        this.scalaShowPopupExtensionMethods = o.scalaShowPopupExtensionMethods;
        this.scalaAddUnambiguousClasses = o.scalaAddUnambiguousClasses;
        this.scalaAddUnambiguousStaticMembers = o.scalaAddUnambiguousStaticMembers;
        this.scalaOptimizeImportsOnTheFly = o.scalaOptimizeImportsOnTheFly;

        this.jspAddUnambiguousImportsOnTheFly = o.jspAddUnambiguousImportsOnTheFly;

        this.kotlinAddUnambiguousImportsOnTheFly = o.kotlinAddUnambiguousImportsOnTheFly;
        this.kotlinOptimizeImportsOnTheFly = o.kotlinOptimizeImportsOnTheFly;

        this.ktorAddImportsAutomatically = o.ktorAddImportsAutomatically;

        this.jsAddImportsAutomatically = o.jsAddImportsAutomatically;
        this.jsOnCodeCompletion = o.jsOnCodeCompletion;
        this.jsWithAutoImportTooltip = o.jsWithAutoImportTooltip;

        this.tsAddImportsAutomatically = o.tsAddImportsAutomatically;
        this.tsOnCodeCompletion = o.tsOnCodeCompletion;
        this.tsWithAutoImportTooltip = o.tsWithAutoImportTooltip;
        this.tsUnambiguousImportsOnTheFly = o.tsUnambiguousImportsOnTheFly;

        this.phpInsertImportsOnPaste = o.phpInsertImportsOnPaste;
        this.phpEnableInFileScope = o.phpEnableInFileScope;
        this.phpEnableInNamespaceScope = o.phpEnableInNamespaceScope;
        this.phpClassGlobalSymbol = o.phpClassGlobalSymbol;
        this.phpFunctionGlobalSymbol = o.phpFunctionGlobalSymbol;
        this.phpConstantGlobalSymbol = o.phpConstantGlobalSymbol;
    }

    public synchronized AutoImportSettings copy() {
        AutoImportSettings clone = new AutoImportSettings();
        clone.copyFrom(this);
        return clone;
    }

    public synchronized boolean isModified(AutoImportSettings o) {
        return this.xmlShowAutoImportTooltip != o.xmlShowAutoImportTooltip
                || this.javaShowTooltipClasses != o.javaShowTooltipClasses
                || this.javaShowTooltipStaticMethods != o.javaShowTooltipStaticMethods
                || this.javaInsertImportsOnPaste != o.javaInsertImportsOnPaste
                || this.javaAddUnambiguousImportsOnTheFly != o.javaAddUnambiguousImportsOnTheFly
                || this.javaOptimizeImportsOnTheFly != o.javaOptimizeImportsOnTheFly
                || !this.javaIncludeStaticMembers.equals(o.javaIncludeStaticMembers)
                || !this.javaExcludeFromAutoImport.equals(o.javaExcludeFromAutoImport)
                || this.pythonShowAutoImportTooltip != o.pythonShowAutoImportTooltip
                || this.pythonPreferredImportStyle != o.pythonPreferredImportStyle
                || this.rustShowImportPopup != o.rustShowImportPopup
                || this.rustImportOutOfScopeItems != o.rustImportOutOfScopeItems
                || this.rustInsertImportsOnPaste != o.rustInsertImportsOnPaste
                || this.rustAddCrateDependenciesOnPaste != o.rustAddCrateDependenciesOnPaste
                || this.rustAddUnambiguousImportsOnTheFly != o.rustAddUnambiguousImportsOnTheFly
                || !this.rustExcludeEntries.equals(o.rustExcludeEntries)
                || this.scalaInsertImportsOnPaste != o.scalaInsertImportsOnPaste
                || this.scalaShowPopupClasses != o.scalaShowPopupClasses
                || this.scalaShowPopupStaticMembers != o.scalaShowPopupStaticMembers
                || this.scalaShowPopupImplicitConversions != o.scalaShowPopupImplicitConversions
                || this.scalaShowPopupImplicitDefinitions != o.scalaShowPopupImplicitDefinitions
                || this.scalaShowPopupExtensionMethods != o.scalaShowPopupExtensionMethods
                || this.scalaAddUnambiguousClasses != o.scalaAddUnambiguousClasses
                || this.scalaAddUnambiguousStaticMembers != o.scalaAddUnambiguousStaticMembers
                || this.scalaOptimizeImportsOnTheFly != o.scalaOptimizeImportsOnTheFly
                || this.jspAddUnambiguousImportsOnTheFly != o.jspAddUnambiguousImportsOnTheFly
                || this.kotlinAddUnambiguousImportsOnTheFly != o.kotlinAddUnambiguousImportsOnTheFly
                || this.kotlinOptimizeImportsOnTheFly != o.kotlinOptimizeImportsOnTheFly
                || this.ktorAddImportsAutomatically != o.ktorAddImportsAutomatically
                || this.jsAddImportsAutomatically != o.jsAddImportsAutomatically
                || this.jsOnCodeCompletion != o.jsOnCodeCompletion
                || this.jsWithAutoImportTooltip != o.jsWithAutoImportTooltip
                || this.tsAddImportsAutomatically != o.tsAddImportsAutomatically
                || this.tsOnCodeCompletion != o.tsOnCodeCompletion
                || this.tsWithAutoImportTooltip != o.tsWithAutoImportTooltip
                || this.tsUnambiguousImportsOnTheFly != o.tsUnambiguousImportsOnTheFly
                || this.phpInsertImportsOnPaste != o.phpInsertImportsOnPaste
                || this.phpEnableInFileScope != o.phpEnableInFileScope
                || this.phpEnableInNamespaceScope != o.phpEnableInNamespaceScope
                || !Objects.equals(this.phpClassGlobalSymbol, o.phpClassGlobalSymbol)
                || !Objects.equals(this.phpFunctionGlobalSymbol, o.phpFunctionGlobalSymbol)
                || !Objects.equals(this.phpConstantGlobalSymbol, o.phpConstantGlobalSymbol);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void fireChanged() {
        for (Listener listener : listeners) {
            try {
                listener.onSettingsChanged(this);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Checks if a Java class or package is excluded from auto-import and completion.
     */
    public boolean isJavaExcluded(String fqcn) {
        if (fqcn == null) return false;
        for (JavaImportEntry entry : javaExcludeFromAutoImport) {
            if (matchesWildcard(entry.getItem(), fqcn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a Rust item/module is excluded.
     */
    public boolean isRustExcluded(String itemPath, boolean isMethod) {
        if (itemPath == null) return false;
        for (RustExcludeEntry entry : rustExcludeEntries) {
            if (!isMethod && entry.getApplyTo() == RustApplyTo.METHODS_ONLY) {
                continue;
            }
            if (matchesWildcard(entry.getItemOrModule(), itemPath)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesWildcard(String pattern, String value) {
        if (pattern == null || value == null) return false;
        String trimmed = pattern.trim();
        if (trimmed.equals(value)) return true;
        if (trimmed.endsWith(".*")) {
            String prefix = trimmed.substring(0, trimmed.length() - 2);
            return value.startsWith(prefix + ".");
        }
        if (trimmed.endsWith("::*")) {
            String prefix = trimmed.substring(0, trimmed.length() - 3);
            return value.startsWith(prefix + "::");
        }
        if (trimmed.contains("*") || trimmed.contains("?")) {
            String regex = "^" + Pattern.quote(trimmed)
                    .replace("*", "\\E.*\\Q")
                    .replace("?", "\\E.\\Q") + "$";
            regex = regex.replace("\\Q\\E", "");
            try {
                return Pattern.compile(regex).matcher(value).matches();
            } catch (Exception ignored) {}
        }
        return false;
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public boolean isXmlShowAutoImportTooltip() { return xmlShowAutoImportTooltip; }
    public void setXmlShowAutoImportTooltip(boolean v) { this.xmlShowAutoImportTooltip = v; }

    public boolean isJavaShowTooltipClasses() { return javaShowTooltipClasses; }
    public void setJavaShowTooltipClasses(boolean v) { this.javaShowTooltipClasses = v; }

    public boolean isJavaShowTooltipStaticMethods() { return javaShowTooltipStaticMethods; }
    public void setJavaShowTooltipStaticMethods(boolean v) { this.javaShowTooltipStaticMethods = v; }

    public InsertImportsMode getJavaInsertImportsOnPaste() { return javaInsertImportsOnPaste; }
    public void setJavaInsertImportsOnPaste(InsertImportsMode v) { this.javaInsertImportsOnPaste = v; }

    public boolean isJavaAddUnambiguousImportsOnTheFly() { return javaAddUnambiguousImportsOnTheFly; }
    public void setJavaAddUnambiguousImportsOnTheFly(boolean v) { this.javaAddUnambiguousImportsOnTheFly = v; }

    public boolean isJavaOptimizeImportsOnTheFly() { return javaOptimizeImportsOnTheFly; }
    public void setJavaOptimizeImportsOnTheFly(boolean v) { this.javaOptimizeImportsOnTheFly = v; }

    public List<JavaImportEntry> getJavaIncludeStaticMembers() { return javaIncludeStaticMembers; }
    public List<JavaImportEntry> getJavaExcludeFromAutoImport() { return javaExcludeFromAutoImport; }

    public boolean isPythonShowAutoImportTooltip() { return pythonShowAutoImportTooltip; }
    public void setPythonShowAutoImportTooltip(boolean v) { this.pythonShowAutoImportTooltip = v; }

    public PythonImportStyle getPythonPreferredImportStyle() { return pythonPreferredImportStyle; }
    public void setPythonPreferredImportStyle(PythonImportStyle v) { this.pythonPreferredImportStyle = v; }

    public boolean isRustShowImportPopup() { return rustShowImportPopup; }
    public void setRustShowImportPopup(boolean v) { this.rustShowImportPopup = v; }

    public boolean isRustImportOutOfScopeItems() { return rustImportOutOfScopeItems; }
    public void setRustImportOutOfScopeItems(boolean v) { this.rustImportOutOfScopeItems = v; }

    public boolean isRustInsertImportsOnPaste() { return rustInsertImportsOnPaste; }
    public void setRustInsertImportsOnPaste(boolean v) { this.rustInsertImportsOnPaste = v; }

    public InsertImportsMode getRustAddCrateDependenciesOnPaste() { return rustAddCrateDependenciesOnPaste; }
    public void setRustAddCrateDependenciesOnPaste(InsertImportsMode v) { this.rustAddCrateDependenciesOnPaste = v; }

    public boolean isRustAddUnambiguousImportsOnTheFly() { return rustAddUnambiguousImportsOnTheFly; }
    public void setRustAddUnambiguousImportsOnTheFly(boolean v) { this.rustAddUnambiguousImportsOnTheFly = v; }

    public List<RustExcludeEntry> getRustExcludeEntries() { return rustExcludeEntries; }

    public InsertImportsMode getScalaInsertImportsOnPaste() { return scalaInsertImportsOnPaste; }
    public void setScalaInsertImportsOnPaste(InsertImportsMode v) { this.scalaInsertImportsOnPaste = v; }

    public boolean isScalaShowPopupClasses() { return scalaShowPopupClasses; }
    public void setScalaShowPopupClasses(boolean v) { this.scalaShowPopupClasses = v; }

    public boolean isScalaShowPopupStaticMembers() { return scalaShowPopupStaticMembers; }
    public void setScalaShowPopupStaticMembers(boolean v) { this.scalaShowPopupStaticMembers = v; }

    public boolean isScalaShowPopupImplicitConversions() { return scalaShowPopupImplicitConversions; }
    public void setScalaShowPopupImplicitConversions(boolean v) { this.scalaShowPopupImplicitConversions = v; }

    public boolean isScalaShowPopupImplicitDefinitions() { return scalaShowPopupImplicitDefinitions; }
    public void setScalaShowPopupImplicitDefinitions(boolean v) { this.scalaShowPopupImplicitDefinitions = v; }

    public boolean isScalaShowPopupExtensionMethods() { return scalaShowPopupExtensionMethods; }
    public void setScalaShowPopupExtensionMethods(boolean v) { this.scalaShowPopupExtensionMethods = v; }

    public boolean isScalaAddUnambiguousClasses() { return scalaAddUnambiguousClasses; }
    public void setScalaAddUnambiguousClasses(boolean v) { this.scalaAddUnambiguousClasses = v; }

    public boolean isScalaAddUnambiguousStaticMembers() { return scalaAddUnambiguousStaticMembers; }
    public void setScalaAddUnambiguousStaticMembers(boolean v) { this.scalaAddUnambiguousStaticMembers = v; }

    public boolean isScalaOptimizeImportsOnTheFly() { return scalaOptimizeImportsOnTheFly; }
    public void setScalaOptimizeImportsOnTheFly(boolean v) { this.scalaOptimizeImportsOnTheFly = v; }

    public boolean isJspAddUnambiguousImportsOnTheFly() { return jspAddUnambiguousImportsOnTheFly; }
    public void setJspAddUnambiguousImportsOnTheFly(boolean v) { this.jspAddUnambiguousImportsOnTheFly = v; }

    public boolean isKotlinAddUnambiguousImportsOnTheFly() { return kotlinAddUnambiguousImportsOnTheFly; }
    public void setKotlinAddUnambiguousImportsOnTheFly(boolean v) { this.kotlinAddUnambiguousImportsOnTheFly = v; }

    public boolean isKotlinOptimizeImportsOnTheFly() { return kotlinOptimizeImportsOnTheFly; }
    public void setKotlinOptimizeImportsOnTheFly(boolean v) { this.kotlinOptimizeImportsOnTheFly = v; }

    public boolean isKtorAddImportsAutomatically() { return ktorAddImportsAutomatically; }
    public void setKtorAddImportsAutomatically(boolean v) { this.ktorAddImportsAutomatically = v; }

    public boolean isJsAddImportsAutomatically() { return jsAddImportsAutomatically; }
    public void setJsAddImportsAutomatically(boolean v) { this.jsAddImportsAutomatically = v; }

    public boolean isJsOnCodeCompletion() { return jsOnCodeCompletion; }
    public void setJsOnCodeCompletion(boolean v) { this.jsOnCodeCompletion = v; }

    public boolean isJsWithAutoImportTooltip() { return jsWithAutoImportTooltip; }
    public void setJsWithAutoImportTooltip(boolean v) { this.jsWithAutoImportTooltip = v; }

    public boolean isTsAddImportsAutomatically() { return tsAddImportsAutomatically; }
    public void setTsAddImportsAutomatically(boolean v) { this.tsAddImportsAutomatically = v; }

    public boolean isTsOnCodeCompletion() { return tsOnCodeCompletion; }
    public void setTsOnCodeCompletion(boolean v) { this.tsOnCodeCompletion = v; }

    public boolean isTsWithAutoImportTooltip() { return tsWithAutoImportTooltip; }
    public void setTsWithAutoImportTooltip(boolean v) { this.tsWithAutoImportTooltip = v; }

    public boolean isTsUnambiguousImportsOnTheFly() { return tsUnambiguousImportsOnTheFly; }
    public void setTsUnambiguousImportsOnTheFly(boolean v) { this.tsUnambiguousImportsOnTheFly = v; }

    public InsertImportsMode getPhpInsertImportsOnPaste() { return phpInsertImportsOnPaste; }
    public void setPhpInsertImportsOnPaste(InsertImportsMode v) { this.phpInsertImportsOnPaste = v; }

    public boolean isPhpEnableInFileScope() { return phpEnableInFileScope; }
    public void setPhpEnableInFileScope(boolean v) { this.phpEnableInFileScope = v; }

    public boolean isPhpEnableInNamespaceScope() { return phpEnableInNamespaceScope; }
    public void setPhpEnableInNamespaceScope(boolean v) { this.phpEnableInNamespaceScope = v; }

    public String getPhpClassGlobalSymbol() { return phpClassGlobalSymbol; }
    public void setPhpClassGlobalSymbol(String v) { this.phpClassGlobalSymbol = v; }

    public String getPhpFunctionGlobalSymbol() { return phpFunctionGlobalSymbol; }
    public void setPhpFunctionGlobalSymbol(String v) { this.phpFunctionGlobalSymbol = v; }

    public String getPhpConstantGlobalSymbol() { return phpConstantGlobalSymbol; }
    public void setPhpConstantGlobalSymbol(String v) { this.phpConstantGlobalSymbol = v; }
}
