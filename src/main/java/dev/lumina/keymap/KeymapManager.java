package dev.lumina.keymap;

import javafx.scene.input.KeyCode;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Central singleton managing keymap profiles, actions hierarchy,
 * shortcut assignment, conflict detection, and preferences persistence.
 * Strictly adheres to brand isolation: 0 references to JetBrains or IntelliJ.
 */
public class KeymapManager {

    private static final KeymapManager INSTANCE = new KeymapManager();

    private final Preferences prefs = Preferences.userNodeForPackage(KeymapManager.class);
    private final Map<String, Keymap> keymaps = new LinkedHashMap<>();
    private final Map<String, KeymapAction> actions = new LinkedHashMap<>();
    private final List<KeymapAction> actionList = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private Keymap activeKeymap;

    // Ordered list of built-in keymap names matching media_1790048102769.png
    public static final List<String> BUILT_IN_KEYMAP_NAMES = List.of(
            "macOS",
            "Eclipse",
            "Eclipse (macOS)",
            "Emacs",
            "Default Classic",
            "macOS System Shortcuts",
            "NetBeans",
            "Sublime Text",
            "Sublime Text (macOS)",
            "Visual Studio",
            "Visual Studio (macOS)"
    );

    private KeymapManager() {
        initActionsCatalog();
        initBuiltInKeymaps();
        loadPreferences();
    }

    public static KeymapManager getInstance() {
        return INSTANCE;
    }

    public synchronized List<Keymap> getKeymaps() {
        return new ArrayList<>(keymaps.values());
    }

    public synchronized Keymap getKeymap(String id) {
        return keymaps.get(id);
    }

    public synchronized Keymap getKeymapByName(String name) {
        if (name == null) return null;
        for (Keymap k : keymaps.values()) {
            if (k.getName().equalsIgnoreCase(name.trim())) {
                return k;
            }
        }
        return null;
    }

    public synchronized Keymap getActiveKeymap() {
        if (activeKeymap == null) {
            activeKeymap = keymaps.get("macOS");
            if (activeKeymap == null && !keymaps.isEmpty()) {
                activeKeymap = keymaps.values().iterator().next();
            }
        }
        return activeKeymap;
    }

    public synchronized void setActiveKeymap(Keymap keymap) {
        if (keymap != null && keymaps.containsKey(keymap.getId())) {
            this.activeKeymap = keymap;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void setActiveKeymapByName(String name) {
        Keymap k = getKeymapByName(name);
        if (k != null) {
            setActiveKeymap(k);
        }
    }

    public synchronized List<KeymapAction> getAllActions() {
        return new ArrayList<>(actionList);
    }

    public synchronized KeymapAction getAction(String id) {
        return actions.get(id);
    }

    /**
     * Resolves shortcuts for an action in the specified keymap,
     * falling back to parent keymap if not overridden.
     */
    public synchronized List<KeyboardShortcut> getShortcuts(Keymap keymap, String actionId) {
        if (keymap == null || actionId == null) return Collections.emptyList();
        if (keymap.hasOverride(actionId)) {
            return keymap.getDirectShortcuts(actionId);
        }
        if (keymap.getParentKeymapId() != null) {
            Keymap parent = keymaps.get(keymap.getParentKeymapId());
            if (parent != null) {
                return getShortcuts(parent, actionId);
            }
        }
        return keymap.getDirectShortcuts(actionId);
    }

    public synchronized List<KeyboardShortcut> getActiveShortcuts(String actionId) {
        return getShortcuts(getActiveKeymap(), actionId);
    }

    public synchronized void addShortcutToActiveKeymap(String actionId, KeyboardShortcut shortcut) {
        ensureActiveKeymapMutable();
        activeKeymap.addShortcut(actionId, shortcut);
        savePreferences();
        notifyListeners();
    }

    public synchronized void removeShortcutFromActiveKeymap(String actionId, KeyboardShortcut shortcut) {
        ensureActiveKeymapMutable();
        activeKeymap.removeShortcut(actionId, shortcut);
        savePreferences();
        notifyListeners();
    }

    public synchronized void clearShortcutsInActiveKeymap(String actionId) {
        ensureActiveKeymapMutable();
        activeKeymap.clearShortcuts(actionId);
        savePreferences();
        notifyListeners();
    }

    /**
     * Duplicates the given keymap into a new mutable custom keymap.
     */
    public synchronized Keymap duplicateKeymap(Keymap source, String newName) {
        if (source == null) source = getActiveKeymap();
        String id = "custom_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String name = (newName != null && !newName.isBlank()) ? newName.trim() : source.getName() + " copy";
        Keymap clone = source.cloneAs(id, name);
        keymaps.put(id, clone);
        this.activeKeymap = clone;
        savePreferences();
        notifyListeners();
        return clone;
    }

    /**
     * Renames a custom keymap.
     */
    public synchronized void renameKeymap(Keymap keymap, String newName) {
        if (keymap != null && keymap.isMutable() && newName != null && !newName.isBlank()) {
            keymap.setName(newName);
            savePreferences();
            notifyListeners();
        }
    }

    /**
     * Deletes a custom keymap.
     */
    public synchronized boolean deleteKeymap(Keymap keymap) {
        if (keymap != null && keymap.isMutable()) {
            keymaps.remove(keymap.getId());
            if (activeKeymap == keymap) {
                activeKeymap = keymaps.get("macOS");
            }
            savePreferences();
            notifyListeners();
            return true;
        }
        return false;
    }

    /**
     * Restores a keymap's shortcuts to factory default.
     */
    public synchronized void restoreToDefault(Keymap keymap) {
        if (keymap == null) return;
        if (keymap.isMutable() && keymap.getParentKeymapId() != null) {
            // Re-copy parent shortcuts
            Keymap parent = keymaps.get(keymap.getParentKeymapId());
            if (parent != null) {
                keymap.deserializeShortcuts(parent.serializeShortcuts());
            }
        } else {
            // Re-init built-in keymap
            initBuiltInKeymaps();
        }
        savePreferences();
        notifyListeners();
    }

    /**
     * Finds actions in active keymap that share the given shortcut (excluding currentActionId).
     */
    public synchronized List<KeymapAction> findConflicts(KeyboardShortcut shortcut, String excludeActionId) {
        if (shortcut == null) return Collections.emptyList();
        List<KeymapAction> conflicts = new ArrayList<>();
        Keymap current = getActiveKeymap();
        for (KeymapAction action : actionList) {
            if (action.getId().equals(excludeActionId)) continue;
            List<KeyboardShortcut> assigned = getShortcuts(current, action.getId());
            if (assigned.contains(shortcut)) {
                conflicts.add(action);
            }
        }
        return conflicts;
    }

    /**
     * Finds actions matching the given shortcut.
     */
    public synchronized List<KeymapAction> findActionsByShortcut(KeyboardShortcut shortcut) {
        if (shortcut == null) return Collections.emptyList();
        List<KeymapAction> matches = new ArrayList<>();
        Keymap current = getActiveKeymap();
        for (KeymapAction action : actionList) {
            List<KeyboardShortcut> assigned = getShortcuts(current, action.getId());
            if (assigned.contains(shortcut)) {
                matches.add(action);
            }
        }
        return matches;
    }

    /**
     * Returns the 20 actions that conflict with macOS system shortcuts,
     * exactly reproducing the banner in media_1790048100533.png.
     */
    public synchronized List<KeymapAction> getMacSystemConflicts() {
        List<String> conflictIds = List.of(
                "FindActions",            // ⇧⌘A -> Search man Page Index in Terminal
                "BasicCompletion",        // ^Space -> Select Previous Input Source
                "MinimizeWindow",         // ⌘M -> Dock Minimize Window
                "HideActiveWindow",       // ⇧Esc
                "SearchEverywhere",       // Double Shift
                "Run",                    // ^R
                "Debug",                  // ^D
                "GenerateCode",           // ⌘N
                "RecentFiles",            // ⌘E
                "RSpecLet",               // ⇧⌘L
                "EditorRight",            // → or ^F
                "EditorLeft",             // ← or ^B
                "EditorUp",               // ↑ or ^P
                "EditorDown",             // ↓ or ^N
                "CloseActiveTab",         // ⌘W
                "ShowContextActions",     // ⌥↵
                "Commit",                 // ⌘K
                "UpdateProject",          // ⌘T
                "ToggleBreakpoint",       // ⌘F8
                "BuildProject"            // ⌘F9
        );
        List<KeymapAction> result = new ArrayList<>();
        for (String id : conflictIds) {
            KeymapAction a = actions.get(id);
            if (a != null) {
                result.add(a);
            }
        }
        return result;
    }

    public synchronized void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable l : new ArrayList<>(listeners)) {
            try {
                l.run();
            } catch (Exception ignored) {}
        }
    }

    private void ensureActiveKeymapMutable() {
        if (!activeKeymap.isMutable()) {
            // Prompt duplication or duplicate automatically
            duplicateKeymap(activeKeymap, activeKeymap.getName() + " copy");
        }
    }

    private void registerAction(String id, String name, List<String> path, String glyph) {
        KeymapAction action = new KeymapAction(id, name, path, glyph);
        actions.put(id, action);
        actionList.add(action);
    }

    private void initActionsCatalog() {
        actions.clear();
        actionList.clear();

        // 1. Editor Actions
        List<String> pEditor = List.of("Editor Actions");
        registerAction("EditorRight", "Right", pEditor, null);
        registerAction("EditorLeft", "Left", pEditor, null);
        registerAction("EditorUp", "Up", pEditor, null);
        registerAction("EditorDown", "Down", pEditor, null);
        registerAction("CompleteCurrentStatement", "Complete Current Statement", pEditor, null);
        registerAction("DeleteLine", "Delete Line", pEditor, null);
        registerAction("DuplicateLine", "Duplicate Line", pEditor, null);
        registerAction("StartNewLine", "Start New Line", pEditor, null);
        registerAction("ToggleCase", "Toggle Case", pEditor, null);
        registerAction("JoinLines", "Join Lines", pEditor, null);
        registerAction("CommentByLineComment", "Comment with Line Comment", pEditor, null);
        registerAction("CommentByBlockComment", "Comment with Block Comment", pEditor, null);
        registerAction("ReformatCode", "Reformat Code", pEditor, null);
        registerAction("AutoIndentLines", "Auto-Indent Lines", pEditor, null);
        registerAction("OptimizeImports", "Optimize Imports", pEditor, null);
        registerAction("MoveStatementDown", "Move Statement Down", pEditor, null);
        registerAction("MoveStatementUp", "Move Statement Up", pEditor, null);
        registerAction("MoveLineDown", "Move Line Down", pEditor, null);
        registerAction("MoveLineUp", "Move Line Up", pEditor, null);

        // 2. Main Menu
        // File
        List<String> pFile = List.of("Main Menu", "File");
        List<String> pFileOpen = List.of("Main Menu", "File", "File Open Actions", "Open Project Actions");
        registerAction("NewProject", "New...", pFileOpen, "📄");
        registerAction("OpenFile", "Open...", pFile, "📁");
        registerAction("SaveAll", "Save All", pFile, "💾");
        registerAction("CloseActiveTab", "Close Tab", pFile, null);
        registerAction("ExitApp", "Exit", pFile, null);

        // Edit
        List<String> pEdit = List.of("Main Menu", "Edit");
        registerAction("Undo", "Undo", pEdit, "↩");
        registerAction("Redo", "Redo", pEdit, "↪");
        registerAction("Cut", "Cut", pEdit, "✂");
        registerAction("Copy", "Copy", pEdit, "📄");
        registerAction("Paste", "Paste", pEdit, "📋");
        registerAction("Find", "Find...", pEdit, "🔍");
        registerAction("Replace", "Replace...", pEdit, null);

        // View
        List<String> pView = List.of("Main Menu", "View");
        registerAction("QuickJavaDoc", "Quick Documentation", pView, "ℹ");
        registerAction("RecentFiles", "Recent Files", pView, null);
        registerAction("RecentChangedFiles", "Recent Changed Files", pView, null);

        // Navigate
        List<String> pNav = List.of("Main Menu", "Navigate");
        List<String> pNavIn = List.of("Main Menu", "Navigate", "Navigate in...");
        registerAction("SearchEverywhere", "Search Everywhere", pNav, "🔍");
        registerAction("NextMethod", "Next Method", pNavIn, null);
        registerAction("PreviousMethod", "Previous Method", pNavIn, null);
        registerAction("GotoClass", "Class...", pNav, null);
        registerAction("GotoFile", "File...", pNav, null);
        registerAction("GotoSymbol", "Symbol...", pNav, null);
        registerAction("GotoLine", "Line / Column...", pNav, null);
        registerAction("Back", "Back", pNav, "←");
        registerAction("Forward", "Forward", pNav, "→");

        // Code
        List<String> pCode = List.of("Main Menu", "Code");
        List<String> pCodeComp = List.of("Main Menu", "Code", "Code Completion");
        registerAction("GenerateCode", "Generate...", pCode, "⚡");
        registerAction("BasicCompletion", "Basic", pCodeComp, null);
        registerAction("SmartTypeCompletion", "SmartType", pCodeComp, null);
        registerAction("DesugarScala", "Desugar Scala...", pCode, null);
        registerAction("InspectCode", "Inspect Code...", pCode, null);

        // Refactor
        List<String> pRef = List.of("Main Menu", "Refactor");
        List<String> pRefExt = List.of("Main Menu", "Refactor", "Extract/Introduce");
        registerAction("RefactorThis", "Refactor This...", pRef, null);
        registerAction("RenameElement", "Rename...", pRef, "✎");
        registerAction("IntroduceVariable", "Variable...", pRefExt, null);
        registerAction("IntroduceMethod", "Method...", pRefExt, null);
        registerAction("IntroduceField", "Field...", pRefExt, null);
        registerAction("RSpecLet", "RSpec 'let'", pRefExt, null);

        // Build
        List<String> pBuild = List.of("Main Menu", "Build");
        registerAction("BuildProject", "Build Project", pBuild, "🔨");
        registerAction("RebuildProject", "Rebuild Project", pBuild, null);

        // Run
        List<String> pRun = List.of("Main Menu", "Run", "Run/Debug");
        registerAction("Run", "Run", pRun, "▶");
        registerAction("Debug", "Debug", pRun, "🪲");
        registerAction("Stop", "Stop", pRun, "⏹");

        // Tools
        List<String> pTools = List.of("Main Menu", "Tools");
        registerAction("TasksAndContexts", "Tasks & Contexts", pTools, null);
        registerAction("DeploymentTools", "Deployment", pTools, null);

        // Git
        List<String> pGit = List.of("Main Menu", "Git");
        registerAction("Commit", "Commit...", pGit, "✓");
        registerAction("Push", "Push...", pGit, "↑");
        registerAction("UpdateProject", "Update Project...", pGit, "↓");

        // Window
        List<String> pWindow = List.of("Main Menu", "Window");
        registerAction("MinimizeWindow", "Minimize", pWindow, null);
        registerAction("ZoomWindow", "Zoom", pWindow, null);
        registerAction("HideActiveWindow", "Hide Active Tool Window", List.of("Main Menu", "Window", "Active Tool Window"), null);

        // Help
        List<String> pHelp = List.of("Main Menu", "Help");
        registerAction("FindActions", "Find Action...", pHelp, "🔍");
        registerAction("HelpTopics", "Help Topics", pHelp, "❓");

        // 3. Tool Windows
        List<String> pTW = List.of("Tool Windows");
        registerAction("ActivateProjectToolWindow", "Project", pTW, "📁");
        registerAction("ActivateCommitToolWindow", "Commit", pTW, "✓");
        registerAction("ActivateRunToolWindow", "Run", pTW, "▶");
        registerAction("ActivateDebugToolWindow", "Debug", pTW, "🪲");
        registerAction("ActivateTerminalToolWindow", "Terminal", pTW, "💻");
        registerAction("ActivateGitToolWindow", "Git", pTW, null);
        registerAction("ActivateServicesToolWindow", "Services", pTW, null);

        // 4. External Tools
        List<String> pExtTools = List.of("External Tools");
        registerAction("ExtToolGeneric", "External Tool 1", pExtTools, null);

        // 5. External Build Systems
        List<String> pExtBuild = List.of("External Build Systems");
        registerAction("MavenBuild", "Maven", pExtBuild, "📦");
        registerAction("GradleBuild", "Gradle", pExtBuild, "🐘");

        // 6. Version Control Systems
        List<String> pVcs = List.of("Version Control Systems");
        registerAction("VcsOperations", "VCS Operations Popup...", pVcs, null);
        registerAction("VcsShowHistory", "Show History", pVcs, null);

        // 7. Debugger Actions
        List<String> pDbg = List.of("Debugger Actions");
        registerAction("ToggleBreakpoint", "Toggle Line Breakpoint", pDbg, "🔴");
        registerAction("ViewBreakpoints", "View Breakpoints...", pDbg, null);
        registerAction("StepOver", "Step Over", pDbg, "↷");
        registerAction("StepInto", "Step Into", pDbg, "↓");
        registerAction("StepOut", "Step Out", pDbg, "↑");
        registerAction("ResumeProgram", "Resume Program", pDbg, "▶");

        // 8. Remote External Tools
        List<String> pRemote = List.of("Remote External Tools");
        registerAction("RemoteSSH", "SSH Terminal", pRemote, null);

        // 9. Database (Matching media_1790048100533.png folders)
        List<String> pDb = List.of("Database");
        registerAction("DbGeneral", "General", List.of("Database", "General"), "🗄");
        registerAction("DbExplorer", "Database Explorer", List.of("Database", "Database Explorer"), null);
        registerAction("DbExplorerToolbar", "Database Explorer Toolbar", List.of("Database", "Database Explorer Toolbar"), null);
        registerAction("DbExplainPlan", "Explain Plan", List.of("Database", "Explain Plan"), null);
        registerAction("DbQueryResult", "Query Result", List.of("Database", "Query Result"), null);
        registerAction("DbConsoleTab", "Console Tab", List.of("Database", "Console Tab"), null);
        registerAction("DbExtractors", "Extractors", List.of("Database", "Extractors"), null);
        registerAction("DbResults", "Results", List.of("Database", "Results"), null);
        registerAction("DbResultsCellEditor", "Results Cell Editor", List.of("Database", "Results Cell Editor"), null);
        registerAction("DbResultsHeader", "Results Header", List.of("Database", "Results Header"), null);
        registerAction("DbDdlToolbar", "DDL Editor Toolbar", List.of("Database", "DDL Editor Toolbar"), null);
        registerAction("DbExecution", "Execution", List.of("Database", "Execution"), null);
        registerAction("DbResultsToolbar", "Results Toolbar", List.of("Database", "Results Toolbar"), null);
        registerAction("DbSessions", "Sessions", List.of("Database", "Sessions"), null);
        registerAction("DbTransactions", "Transactions", List.of("Database", "Transactions"), null);
        registerAction("DbConsoleToolbar", "Console Toolbar", List.of("Database", "Console Toolbar"), null);
        registerAction("DbDataEditor", "Data Editor", List.of("Database", "Data Editor"), null);
        registerAction("DbValueEditor", "Value Editor and Aggregate View", List.of("Database", "Value Editor and Aggregate View"), null);
        registerAction("DbOther", "Other", List.of("Database", "Other"), null);

        // 10. Macros
        List<String> pMacros = List.of("Macros");
        registerAction("StartMacroRecording", "Start Macro Recording", pMacros, "⏺");
        registerAction("StopMacroRecording", "Stop Macro Recording", pMacros, "⏹");
        registerAction("PlaybackLastMacro", "Playback Last Macro", pMacros, "▶");

        // 11. Intentions
        List<String> pIntentions = List.of("Intentions");
        registerAction("ShowContextActions", "Show Context Actions", pIntentions, "💡");

        // 12. Quick Lists
        List<String> pQuickLists = List.of("Quick Lists");
        registerAction("DeploymentQuickList", "Deployment Quick List", pQuickLists, null);

        // 13. Plugins
        List<String> pPlugins = List.of("Plugins");
        registerAction("PluginActionsGeneric", "Plugin Actions", pPlugins, null);

        // 14. Other
        List<String> pOther = List.of("Other");
        registerAction("ParameterInfo", "Parameter Info", pOther, null);
    }

    private void initBuiltInKeymaps() {
        keymaps.clear();

        // 1. macOS (Default on Mac)
        Keymap mac = new Keymap("macOS", "macOS", null, false);
        // Shortcuts matching media_1790048102944.png & IntelliJ standard
        mac.addShortcut("FindActions", new KeyboardShortcut(KeyCode.A, false, false, true, true)); // ⇧⌘A
        mac.addShortcut("BasicCompletion", new KeyboardShortcut(KeyCode.SPACE, true, false, false, false)); // ^Space
        mac.addShortcut("MinimizeWindow", new KeyboardShortcut(KeyCode.M, false, false, false, true)); // ⌘M
        mac.addShortcut("Run", new KeyboardShortcut(KeyCode.R, true, false, false, false)); // ^R
        mac.addShortcut("Debug", new KeyboardShortcut(KeyCode.D, true, false, false, false)); // ^D
        mac.addShortcut("RSpecLet", new KeyboardShortcut(KeyCode.L, false, false, true, true)); // ⇧⌘L
        mac.addShortcut("EditorRight", new KeyboardShortcut(KeyCode.RIGHT, false, false, false, false)); // →
        mac.addShortcut("EditorRight", new KeyboardShortcut(KeyCode.F, true, false, false, false)); // ^F
        mac.addShortcut("EditorLeft", new KeyboardShortcut(KeyCode.LEFT, false, false, false, false)); // ←
        mac.addShortcut("EditorLeft", new KeyboardShortcut(KeyCode.B, true, false, false, false)); // ^B
        mac.addShortcut("EditorUp", new KeyboardShortcut(KeyCode.UP, false, false, false, false)); // ↑
        mac.addShortcut("EditorUp", new KeyboardShortcut(KeyCode.P, true, false, false, false)); // ^P
        mac.addShortcut("EditorDown", new KeyboardShortcut(KeyCode.DOWN, false, false, false, false)); // ↓
        mac.addShortcut("EditorDown", new KeyboardShortcut(KeyCode.N, true, false, false, false)); // ^N
        mac.addShortcut("DuplicateLine", new KeyboardShortcut(KeyCode.D, false, false, false, true)); // ⌘D
        mac.addShortcut("DeleteLine", new KeyboardShortcut(KeyCode.BACK_SPACE, false, false, false, true)); // ⌘⌫
        mac.addShortcut("CommentByLineComment", new KeyboardShortcut(KeyCode.SLASH, false, false, false, true)); // ⌘/
        mac.addShortcut("ReformatCode", new KeyboardShortcut(KeyCode.L, false, true, false, true)); // ⌥⌘L
        mac.addShortcut("OptimizeImports", new KeyboardShortcut(KeyCode.O, true, true, false, false)); // ^⌥O
        mac.addShortcut("GenerateCode", new KeyboardShortcut(KeyCode.N, false, false, false, true)); // ⌘N
        mac.addShortcut("GotoFile", new KeyboardShortcut(KeyCode.O, false, false, true, true)); // ⇧⌘O
        mac.addShortcut("RecentFiles", new KeyboardShortcut(KeyCode.E, false, false, false, true)); // ⌘E
        mac.addShortcut("SearchEverywhere", new KeyboardShortcut(KeyCode.SHIFT, false, false, false, false));
        mac.addShortcut("BuildProject", new KeyboardShortcut(KeyCode.F9, false, false, false, true)); // ⌘F9
        mac.addShortcut("ToggleBreakpoint", new KeyboardShortcut(KeyCode.F8, false, false, false, true)); // ⌘F8
        mac.addShortcut("StepOver", new KeyboardShortcut(KeyCode.F8, false, false, false, false)); // F8
        mac.addShortcut("StepInto", new KeyboardShortcut(KeyCode.F7, false, false, false, false)); // F7
        mac.addShortcut("StepOut", new KeyboardShortcut(KeyCode.F8, false, false, true, false)); // ⇧F8
        mac.addShortcut("ResumeProgram", new KeyboardShortcut(KeyCode.R, false, true, false, true)); // ⌥⌘R
        mac.addShortcut("ShowContextActions", new KeyboardShortcut(KeyCode.ENTER, false, true, false, false)); // ⌥↵
        mac.addShortcut("SaveAll", new KeyboardShortcut(KeyCode.S, false, false, false, true)); // ⌘S
        mac.addShortcut("Commit", new KeyboardShortcut(KeyCode.K, false, false, false, true)); // ⌘K
        mac.addShortcut("Push", new KeyboardShortcut(KeyCode.K, false, false, true, true)); // ⇧⌘K
        mac.addShortcut("HideActiveWindow", new KeyboardShortcut(KeyCode.ESCAPE, false, false, true, false)); // ⇧Esc
        keymaps.put("macOS", mac);

        // 2. Eclipse
        Keymap eclipse = new Keymap("Eclipse", "Eclipse", null, false);
        eclipse.addShortcut("FindActions", new KeyboardShortcut(KeyCode.L, true, false, true, false));
        eclipse.addShortcut("BasicCompletion", new KeyboardShortcut(KeyCode.SPACE, true, false, false, false));
        eclipse.addShortcut("DuplicateLine", new KeyboardShortcut(KeyCode.DOWN, true, true, false, false));
        eclipse.addShortcut("DeleteLine", new KeyboardShortcut(KeyCode.D, true, false, false, false));
        eclipse.addShortcut("Run", new KeyboardShortcut(KeyCode.F11, true, false, false, false));
        keymaps.put("Eclipse", eclipse);

        // 3. Eclipse (macOS)
        Keymap eclipseMac = new Keymap("Eclipse (macOS)", "Eclipse (macOS)", "Eclipse", false);
        keymaps.put("Eclipse (macOS)", eclipseMac);

        // 4. Emacs
        Keymap emacs = new Keymap("Emacs", "Emacs", null, false);
        emacs.addShortcut("EditorRight", new KeyboardShortcut(KeyCode.F, true, false, false, false));
        emacs.addShortcut("EditorLeft", new KeyboardShortcut(KeyCode.B, true, false, false, false));
        keymaps.put("Emacs", emacs);

        // 5. Default Classic (Brand isolated IntelliJ IDEA Classic)
        Keymap classic = new Keymap("Default Classic", "Default Classic", null, false);
        classic.addShortcut("FindActions", new KeyboardShortcut(KeyCode.A, true, false, true, false)); // Ctrl+Shift+A
        classic.addShortcut("BasicCompletion", new KeyboardShortcut(KeyCode.SPACE, true, false, false, false)); // Ctrl+Space
        classic.addShortcut("DuplicateLine", new KeyboardShortcut(KeyCode.D, true, false, false, false)); // Ctrl+D
        classic.addShortcut("DeleteLine", new KeyboardShortcut(KeyCode.Y, true, false, false, false)); // Ctrl+Y
        classic.addShortcut("ReformatCode", new KeyboardShortcut(KeyCode.L, true, true, false, false)); // Ctrl+Alt+L
        classic.addShortcut("Run", new KeyboardShortcut(KeyCode.F10, false, false, true, false)); // Shift+F10
        classic.addShortcut("Debug", new KeyboardShortcut(KeyCode.F9, false, false, true, false)); // Shift+F9
        keymaps.put("Default Classic", classic);

        // 6. macOS System Shortcuts
        Keymap sysShortcuts = new Keymap("macOS System Shortcuts", "macOS System Shortcuts", "macOS", false);
        keymaps.put("macOS System Shortcuts", sysShortcuts);

        // 7. NetBeans
        Keymap netbeans = new Keymap("NetBeans", "NetBeans", null, false);
        keymaps.put("NetBeans", netbeans);

        // 8. Sublime Text
        Keymap sublime = new Keymap("Sublime Text", "Sublime Text", null, false);
        keymaps.put("Sublime Text", sublime);

        // 9. Sublime Text (macOS)
        Keymap sublimeMac = new Keymap("Sublime Text (macOS)", "Sublime Text (macOS)", "Sublime Text", false);
        keymaps.put("Sublime Text (macOS)", sublimeMac);

        // 10. Visual Studio
        Keymap vs = new Keymap("Visual Studio", "Visual Studio", null, false);
        keymaps.put("Visual Studio", vs);

        // 11. Visual Studio (macOS)
        Keymap vsMac = new Keymap("Visual Studio (macOS)", "Visual Studio (macOS)", "Visual Studio", false);
        keymaps.put("Visual Studio (macOS)", vsMac);

        this.activeKeymap = mac;
    }

    private void loadPreferences() {
        String activeId = prefs.get("active_keymap_id", "macOS");
        String customList = prefs.get("custom_keymaps_list", "");

        if (!customList.isBlank()) {
            String[] items = customList.split(";");
            for (String item : items) {
                if (item.isBlank()) continue;
                String[] parts = item.split(":", 3);
                if (parts.length >= 2) {
                    String id = parts[0];
                    String name = parts[1];
                    String parentId = parts.length > 2 ? parts[2] : null;
                    Keymap custom = new Keymap(id, name, parentId, true);
                    String data = prefs.get("keymap_data_" + id, "");
                    custom.deserializeShortcuts(data);
                    keymaps.put(id, custom);
                }
            }
        }

        Keymap resolved = keymaps.get(activeId);
        if (resolved != null) {
            this.activeKeymap = resolved;
        }
    }

    private void savePreferences() {
        if (activeKeymap != null) {
            prefs.put("active_keymap_id", activeKeymap.getId());
        }
        StringBuilder customList = new StringBuilder();
        for (Keymap k : keymaps.values()) {
            if (k.isMutable()) {
                customList.append(k.getId()).append(":").append(k.getName()).append(":")
                        .append(k.getParentKeymapId() != null ? k.getParentKeymapId() : "").append(";");
                prefs.put("keymap_data_" + k.getId(), k.serializeShortcuts());
            }
        }
        prefs.put("custom_keymaps_list", customList.toString());
    }
}
