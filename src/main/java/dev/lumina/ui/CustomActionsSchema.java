package dev.lumina.ui;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Dynamic Action Schema managing the hierarchical structure of menus,
 * toolbars, and popups across Lumina IDE, strictly matching IntelliJ IDEA.
 */
public class CustomActionsSchema {

    private static final CustomActionsSchema INSTANCE = new CustomActionsSchema();

    private final Preferences prefs = Preferences.userNodeForPackage(CustomActionsSchema.class);
    private final List<CustomActionItem> rootGroups = new ArrayList<>();
    private final List<CustomActionItem> defaultRootGroups = new ArrayList<>();
    private final List<Runnable> changeListeners = new ArrayList<>();

    private boolean modified = false;

    private CustomActionsSchema() {
        initDefaultSchema();
        loadFromPreferences();
    }

    public static CustomActionsSchema getInstance() {
        return INSTANCE;
    }

    public synchronized List<CustomActionItem> getRootGroups() {
        return rootGroups;
    }

    public synchronized CustomActionItem getGroup(String id) {
        if (id == null) return null;
        for (CustomActionItem item : rootGroups) {
            if (id.equals(item.getId())) return item;
        }
        for (CustomActionItem item : defaultRootGroups) {
            if (id.equals(item.getId())) return item;
        }
        return null;
    }

    public synchronized boolean isModified() {
        return modified;
    }

    public synchronized void setModified(boolean modified) {
        this.modified = modified;
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
        for (Runnable listener : new ArrayList<>(changeListeners)) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void revertToDefaults() {
        rootGroups.clear();
        for (CustomActionItem item : defaultRootGroups) {
            rootGroups.add(item.deepCopy());
        }
        modified = false;
        prefs.remove("custom_actions_modified");
        notifyListeners();
    }

    public synchronized void save() {
        // Save state flag
        prefs.putBoolean("custom_actions_modified", modified);
        notifyListeners();
    }

    /**
     * Initializes the default schema matching all 5 IntelliJ reference images.
     */
    private void initDefaultSchema() {
        defaultRootGroups.clear();

        // 1. Main Menu
        CustomActionItem mainMenu = CustomActionItem.group("MainMenu", "Main Menu");
        mainMenu.addChild(buildFileMenu());
        mainMenu.addChild(buildEditMenu());
        mainMenu.addChild(buildViewMenu());
        mainMenu.addChild(buildNavigateMenu());
        mainMenu.addChild(buildCodeMenu());
        mainMenu.addChild(buildRefactorMenu());
        mainMenu.addChild(buildBuildMenu());
        mainMenu.addChild(buildRunMenu());
        mainMenu.addChild(buildToolsMenu());
        mainMenu.addChild(buildGitMenu());
        mainMenu.addChild(buildWindowMenu());
        mainMenu.addChild(buildHelpMenu());
        defaultRootGroups.add(mainMenu);

        // 2. Main Toolbar
        CustomActionItem mainToolbar = CustomActionItem.group("MainToolbar", "Main Toolbar");
        mainToolbar.addChild(CustomActionItem.action("OpenProject", "Open Project...", "📁"));
        mainToolbar.addChild(CustomActionItem.action("SaveAll", "Save All", "💾"));
        mainToolbar.addChild(CustomActionItem.separator());
        mainToolbar.addChild(CustomActionItem.action("Run", "Run", "▶"));
        mainToolbar.addChild(CustomActionItem.action("Debug", "Debug", "🐞"));
        mainToolbar.addChild(CustomActionItem.action("Stop", "Stop", "⏹"));
        mainToolbar.addChild(CustomActionItem.separator());
        mainToolbar.addChild(CustomActionItem.action("GitBranch", "Branches", "🌱"));
        mainToolbar.addChild(CustomActionItem.action("Settings", "Settings...", "⚙"));
        defaultRootGroups.add(mainToolbar);

        // 3. Editor Popup Menu
        CustomActionItem editorPopup = CustomActionItem.group("EditorPopupMenu", "Editor Popup Menu");
        editorPopup.addChild(CustomActionItem.action("Cut", "Cut", "✂"));
        editorPopup.addChild(CustomActionItem.action("Copy", "Copy", "📋"));
        editorPopup.addChild(CustomActionItem.action("Paste", "Paste", "📋"));
        editorPopup.addChild(CustomActionItem.separator());
        editorPopup.addChild(CustomActionItem.action("FindUsages", "Find Usages", "🔍"));
        editorPopup.addChild(CustomActionItem.action("GoToDeclaration", "Go to Declaration", ""));
        editorPopup.addChild(CustomActionItem.separator());
        editorPopup.addChild(CustomActionItem.action("RefactorThis", "Refactor...", "💡"));
        editorPopup.addChild(CustomActionItem.action("Generate", "Generate...", "⚙"));
        defaultRootGroups.add(editorPopup);

        // 4. Editor Gutter Popup Menu
        CustomActionItem gutterPopup = CustomActionItem.group("EditorGutterPopupMenu", "Editor Gutter Popup Menu");
        gutterPopup.addChild(CustomActionItem.action("ToggleLineNumbers", "Show Line Numbers", ""));
        gutterPopup.addChild(CustomActionItem.action("ToggleIndentGuides", "Show Indent Guides", ""));
        gutterPopup.addChild(CustomActionItem.action("ToggleAnnotations", "Annotate with Git Blame", ""));
        defaultRootGroups.add(gutterPopup);

        // 5. Editor Tab Popup Menu
        CustomActionItem tabPopup = CustomActionItem.group("EditorTabPopupMenu", "Editor Tab Popup Menu");
        tabPopup.addChild(CustomActionItem.action("CloseTab", "Close", "✕"));
        tabPopup.addChild(CustomActionItem.action("CloseOtherTabs", "Close Others", ""));
        tabPopup.addChild(CustomActionItem.action("CloseAllTabs", "Close All", ""));
        tabPopup.addChild(CustomActionItem.separator());
        tabPopup.addChild(CustomActionItem.action("SplitRight", "Split Right", "◫"));
        tabPopup.addChild(CustomActionItem.action("SplitDown", "Split Down", "⊟"));
        tabPopup.addChild(CustomActionItem.action("PinTab", "Pin Tab", "📌"));
        defaultRootGroups.add(tabPopup);

        // 6. Project View Popup Menu
        CustomActionItem projectViewPopup = CustomActionItem.group("ProjectViewPopupMenu", "Project View Popup Menu");
        projectViewPopup.addChild(CustomActionItem.action("NewGroup", "New", "➕"));
        projectViewPopup.addChild(CustomActionItem.separator());
        projectViewPopup.addChild(CustomActionItem.action("Cut", "Cut", "✂"));
        projectViewPopup.addChild(CustomActionItem.action("Copy", "Copy", "📋"));
        projectViewPopup.addChild(CustomActionItem.action("CopyPath", "Copy Path/Reference...", ""));
        projectViewPopup.addChild(CustomActionItem.action("Paste", "Paste", "📋"));
        projectViewPopup.addChild(CustomActionItem.separator());
        projectViewPopup.addChild(CustomActionItem.action("FindInFiles", "Find in Files...", "🔍"));
        projectViewPopup.addChild(CustomActionItem.action("ReplaceInFiles", "Replace in Files...", ""));
        defaultRootGroups.add(projectViewPopup);

        // 7. Scope View Popup Menu
        defaultRootGroups.add(CustomActionItem.group("ScopeViewPopupMenu", "Scope View Popup Menu"));

        // 8. Navigation Bar Popup Menu
        defaultRootGroups.add(CustomActionItem.group("NavigationBarPopupMenu", "Navigation Bar Popup Menu"));

        // 9. Navigation Bar Toolbar
        defaultRootGroups.add(CustomActionItem.group("NavigationBarToolbar", "Navigation Bar Toolbar"));

        // 10. Debug Header More Popup
        defaultRootGroups.add(CustomActionItem.group("DebugHeaderMorePopup", "Debug Header More Popup"));

        // 11. Debug Header Toolbar
        defaultRootGroups.add(CustomActionItem.group("DebugHeaderToolbar", "Debug Header Toolbar"));

        // 12. Debug Watches Toolbar
        defaultRootGroups.add(CustomActionItem.group("DebugWatchesToolbar", "Debug Watches Toolbar"));

        // 13. File History Toolbar
        defaultRootGroups.add(CustomActionItem.group("FileHistoryToolbar", "File History Toolbar"));

        // 14. Floating Code Toolbar
        defaultRootGroups.add(CustomActionItem.group("FloatingCodeToolbar", "Floating Code Toolbar"));

        // 15. Floating Code Toolbar Web
        defaultRootGroups.add(CustomActionItem.group("FloatingCodeToolbarWeb", "Floating Code Toolbar Web"));

        // 16. Jupyter Cell Toolbar
        defaultRootGroups.add(CustomActionItem.group("JupyterCellToolbar", "Jupyter Cell Toolbar"));

        // 17. Jupyter Editor Toolbar
        defaultRootGroups.add(CustomActionItem.group("JupyterEditorToolbar", "Jupyter Editor Toolbar"));

        // 18. Markdown Editor Floating Toolbar
        defaultRootGroups.add(CustomActionItem.group("MarkdownEditorFloatingToolbar", "Markdown Editor Floating Toolbar"));

        // 19. Quick Actions Popup Toolbar
        defaultRootGroups.add(CustomActionItem.group("QuickActionsPopupToolbar", "Quick Actions Popup Toolbar"));

        // 20. Run Tool Window Header More Popup
        defaultRootGroups.add(CustomActionItem.action("RunToolWindowHeaderMorePopup", "Run Tool Window Header More Popup", ""));

        // 21. Run Tool Window Header Toolbar
        defaultRootGroups.add(CustomActionItem.group("RunToolWindowHeaderToolbar", "Run Tool Window Header Toolbar"));

        // 22. SQL Floating Toolbar
        defaultRootGroups.add(CustomActionItem.group("SqlFloatingToolbar", "SQL Floating Toolbar"));

        // 23. Tables Toolbar
        defaultRootGroups.add(CustomActionItem.group("TablesToolbar", "Tables Toolbar"));

        // 24. VCS Local Changes Toolbar
        defaultRootGroups.add(CustomActionItem.group("VcsLocalChangesToolbar", "VCS Local Changes Toolbar"));

        // 25. VCS Log Changes Browser Toolbar
        defaultRootGroups.add(CustomActionItem.group("VcsLogChangesBrowserToolbar", "VCS Log Changes Browser Toolbar"));

        // 26. VCS Log Toolbar
        defaultRootGroups.add(CustomActionItem.group("VcsLogToolbar", "VCS Log Toolbar"));

        // 27. VCS Operations Popup
        defaultRootGroups.add(buildVcsOperationsPopupGroup());

        // Populate active rootGroups from default
        rootGroups.clear();
        for (CustomActionItem item : defaultRootGroups) {
            rootGroups.add(item.deepCopy());
        }
    }

    private CustomActionItem buildVcsOperationsPopupGroup() {
        CustomActionItem group = CustomActionItem.group("VcsOperationsPopup", "VCS Operations Popup");
        group.addChild(CustomActionItem.action("CheckinProject", "Commit...", "commit"));
        group.addChild(CustomActionItem.action("CheckinFiles", "Commit...", "commit"));
        group.addChild(CustomActionItem.action("ChangesView.Revert", "Rollback...", "rollback"));
        group.addChild(CustomActionItem.separator());
        group.addChild(CustomActionItem.action("Vcs.ShowTabbedFileHistory", "Show History", "clock"));
        group.addChild(CustomActionItem.action("Annotate", "Annotate", ""));
        group.addChild(CustomActionItem.action("Diff.ShowDiff", "Show Diff", "diff"));
        group.addChild(CustomActionItem.separator());
        group.addChild(CustomActionItem.action("Git.Branches", "Branches...", "branch"));
        group.addChild(CustomActionItem.action("Vcs.Push", "Push...", "push"));
        group.addChild(CustomActionItem.action("Git.Stash", "Stash Changes...", ""));
        group.addChild(CustomActionItem.action("Git.Unstash", "Unstash Changes...", ""));
        group.addChild(CustomActionItem.action("Git.CopyBranchName", "Copy Branch Name", "copy"));
        group.addChild(CustomActionItem.separator());
        group.addChild(CustomActionItem.action("LocalHistory.ShowHistory", "Show Local History...", ""));
        return group;
    }

    private CustomActionItem buildFileMenu() {
        CustomActionItem file = CustomActionItem.group("FileMenu", "File");

        // File Open Actions
        CustomActionItem fileOpenActions = CustomActionItem.group("FileOpenActions", "File Open Actions");

        // Open Project Actions
        CustomActionItem openProjActions = CustomActionItem.group("OpenProjectActions", "Open Project Actions");

        // New Menu & Subgroups
        CustomActionItem newGroup = CustomActionItem.group("NewMenu", "New");
        newGroup.addChild(CustomActionItem.group("GitHubActions", "GitHub Actions", "🐙"));
        newGroup.addChild(CustomActionItem.group("WebPage", "Web Page", "🌐"));
        newGroup.addChild(CustomActionItem.action("AddPropertiesToBundle", "Add Property Files to Resource Bundle", "⚙"));

        CustomActionItem newGroup1 = CustomActionItem.group("NewGroup1", "New Group (1)");
        newGroup1.addChild(CustomActionItem.action("PlayTemplate", "Play template", "▶"));
        newGroup1.addChild(CustomActionItem.action("ScalaClass", "Scala Class/File", "🔴"));
        newGroup1.addChild(CustomActionItem.action("PackageObject", "Package Object", "📦"));
        newGroup1.addChild(CustomActionItem.action("CustomInspection", "Custom Inspection", "🔍"));
        newGroup1.addChild(CustomActionItem.action("GroovyClass", "Groovy Class", "🔵"));
        newGroup1.addChild(CustomActionItem.action("KotlinClass", "Kotlin Class/File", "🟣"));
        newGroup1.addChild(CustomActionItem.action("PythonFile", "Python File", "🐍"));
        newGroup1.addChild(CustomActionItem.action("JupyterNotebook", "Jupyter Notebook", "🪐"));
        newGroup1.addChild(CustomActionItem.action("RustFile", "Rust File", "🦀"));
        newGroup1.addChild(CustomActionItem.action("RustModule", "Rust Module", "📦"));
        newGroup1.addChild(CustomActionItem.action("CargoCrate", "Cargo Crate", "📦"));
        newGroup1.addChild(CustomActionItem.action("GoFile", "Go File", "🐹"));
        newGroup.addChild(newGroup1);

        CustomActionItem phpGroup = CustomActionItem.group("PhpNewGroup", "PhpNewGroup");
        phpGroup.addChild(CustomActionItem.action("NewFile", "File", "📄"));
        phpGroup.addChild(CustomActionItem.action("GoModulesFile", "Go Modules File", "🐹"));
        phpGroup.addChild(CustomActionItem.action("GoWorkspaceFile", "Go Workspace File", "🐹"));
        phpGroup.addChild(CustomActionItem.action("ScratchFile", "Scratch File", "📝"));
        phpGroup.addChild(CustomActionItem.action("CreateDirectoryOrPackage", "Create New Directory or Package", "📁"));
        newGroup.addChild(phpGroup);

        CustomActionItem javaFxGroup = CustomActionItem.group("JavaFxCreateActions", "JavaFxCreateActions");
        javaFxGroup.addChild(CustomActionItem.action("PythonPackage", "Python Package", "🐍"));
        newGroup.addChild(javaFxGroup);

        CustomActionItem newJavaSpecial = CustomActionItem.group("NewJavaSpecialFile", "NewJavaSpecialFile");
        newJavaSpecial.addChild(CustomActionItem.action("FlywayMigration", "dev.lumina.flyway.structure.new", "🪶"));
        newJavaSpecial.addChild(CustomActionItem.action("LiquibaseNew", "dev.lumina.liquibase.structure.new", "💧"));
        newJavaSpecial.addChild(CustomActionItem.action("LiquibaseOther", "dev.lumina.liquibase.structure.other", "💧"));
        newJavaSpecial.addChild(CustomActionItem.action("FileTemplateSeparatorGroup", "FileTemplateSeparatorGroup", ""));
        newJavaSpecial.addChild(CustomActionItem.action("SpringComponentKotlin", "Spring Component (Kotlin)", "🌱"));
        newGroup.addChild(newJavaSpecial);

        openProjActions.addChild(newGroup);
        openProjActions.addChild(CustomActionItem.action("OpenFile", "Open...", "📁"));
        openProjActions.addChild(CustomActionItem.action("AttachProject", "Attach Project...", ""));

        fileOpenActions.addChild(openProjActions);
        fileOpenActions.addChild(CustomActionItem.group("RecentProjects", "Recent Projects"));
        fileOpenActions.addChild(CustomActionItem.action("CloseProject", "Close Project", ""));
        fileOpenActions.addChild(CustomActionItem.action("CloseAllProjects", "Close All Projects", ""));
        fileOpenActions.addChild(CustomActionItem.action("CloseOtherProjects", "Close Other Projects", ""));

        file.addChild(fileOpenActions);
        file.addChild(CustomActionItem.group("RemoteDevActions", "dev.lumina.gateway.RemoteDevelopmentActions"));
        file.addChild(CustomActionItem.separator());
        file.addChild(CustomActionItem.group("SettingsActions", "Settings Actions"));
        file.addChild(CustomActionItem.group("FileProperties", "File Properties"));
        file.addChild(CustomActionItem.separator());

        file.addChild(CustomActionItem.group("LocalHistoryGroup", "LocalHistory.MainMenuGroup"));
        file.addChild(CustomActionItem.action("SaveAll", "Save All", "💾"));
        file.addChild(CustomActionItem.action("ReloadAllFromDisk", "Reload All from Disk", "🔄"));
        file.addChild(CustomActionItem.action("CacheRecovery", "Cache Recovery", ""));
        file.addChild(CustomActionItem.action("InvalidateCaches", "Invalidate Caches...", ""));
        file.addChild(CustomActionItem.separator());

        file.addChild(CustomActionItem.group("ManageIdeSettings", "Manage IDE Settings"));
        file.addChild(CustomActionItem.group("NewProjectsSetup", "New Projects Setup"));
        file.addChild(CustomActionItem.action("SaveFileAsTemplate", "Save File as Template...", ""));
        file.addChild(CustomActionItem.separator());

        file.addChild(CustomActionItem.group("PrintExportActions", "Print/Export Actions"));
        file.addChild(CustomActionItem.group("PowerSaveGroup", "PowerSaveGroup"));
        file.addChild(CustomActionItem.separator());

        file.addChild(CustomActionItem.action("Exit", "Exit", ""));
        return file;
    }

    private CustomActionItem buildEditMenu() {
        CustomActionItem edit = CustomActionItem.group("EditMenu", "Edit");
        edit.addChild(CustomActionItem.action("Undo", "Undo", "↩"));
        edit.addChild(CustomActionItem.action("Redo", "Redo", "↪"));
        edit.addChild(CustomActionItem.separator());
        edit.addChild(CustomActionItem.action("Cut", "Cut", "✂"));
        edit.addChild(CustomActionItem.action("Copy", "Copy", "📋"));
        edit.addChild(CustomActionItem.action("CopyPath", "Copy Path/Reference...", ""));
        edit.addChild(CustomActionItem.action("Paste", "Paste", "📋"));
        edit.addChild(CustomActionItem.action("Delete", "Delete", "🗑"));
        edit.addChild(CustomActionItem.separator());
        edit.addChild(CustomActionItem.action("Find", "Find", "🔍"));
        edit.addChild(CustomActionItem.action("FindInFiles", "Find in Files...", "🔍"));
        edit.addChild(CustomActionItem.action("Replace", "Replace", ""));
        edit.addChild(CustomActionItem.action("ReplaceInFiles", "Replace in Files...", ""));
        return edit;
    }

    private CustomActionItem buildViewMenu() {
        CustomActionItem view = CustomActionItem.group("ViewMenu", "View");
        view.addChild(CustomActionItem.group("ToolWindows", "Tool Windows"));
        view.addChild(CustomActionItem.group("Appearance", "Appearance"));
        view.addChild(CustomActionItem.separator());
        view.addChild(CustomActionItem.action("QuickDocumentation", "Quick Documentation", "📖"));
        view.addChild(CustomActionItem.action("ParameterInfo", "Parameter Info", ""));
        view.addChild(CustomActionItem.separator());
        view.addChild(CustomActionItem.action("RecentFiles", "Recent Files", ""));
        view.addChild(CustomActionItem.action("RecentlyChangedFiles", "Recently Changed Files", ""));
        return view;
    }

    private CustomActionItem buildNavigateMenu() {
        CustomActionItem nav = CustomActionItem.group("NavigateMenu", "Navigate");
        nav.addChild(CustomActionItem.action("SearchEverywhere", "Search Everywhere", "🔍"));
        nav.addChild(CustomActionItem.action("GotoClass", "Class...", ""));
        nav.addChild(CustomActionItem.action("GotoFile", "File...", ""));
        nav.addChild(CustomActionItem.action("GotoSymbol", "Symbol...", ""));
        nav.addChild(CustomActionItem.separator());
        nav.addChild(CustomActionItem.action("GotoLine", "Line:Column...", ""));
        nav.addChild(CustomActionItem.action("Back", "Back", "←"));
        nav.addChild(CustomActionItem.action("Forward", "Forward", "→"));
        return nav;
    }

    private CustomActionItem buildCodeMenu() {
        CustomActionItem code = CustomActionItem.group("CodeMenu", "Code");
        code.addChild(CustomActionItem.action("OverrideMethods", "Override Methods...", ""));
        code.addChild(CustomActionItem.action("ImplementMethods", "Implement Methods...", ""));
        code.addChild(CustomActionItem.action("Generate", "Generate...", "⚙"));
        code.addChild(CustomActionItem.separator());
        code.addChild(CustomActionItem.action("InspectCode", "Inspect Code...", "🔍"));
        code.addChild(CustomActionItem.action("ReformatCode", "Reformat Code", ""));
        code.addChild(CustomActionItem.action("OptimizeImports", "Optimize Imports", ""));
        return code;
    }

    private CustomActionItem buildRefactorMenu() {
        CustomActionItem refactor = CustomActionItem.group("RefactorMenu", "Refactor");
        refactor.addChild(CustomActionItem.action("RefactorThis", "Refactor This...", "💡"));
        refactor.addChild(CustomActionItem.action("Rename", "Rename...", ""));
        refactor.addChild(CustomActionItem.action("ChangeSignature", "Change Signature...", ""));
        refactor.addChild(CustomActionItem.separator());
        refactor.addChild(CustomActionItem.action("ExtractMethod", "Extract Method...", ""));
        refactor.addChild(CustomActionItem.action("IntroduceVariable", "Introduce Variable...", ""));
        refactor.addChild(CustomActionItem.action("Inline", "Inline...", ""));
        return refactor;
    }

    private CustomActionItem buildBuildMenu() {
        CustomActionItem build = CustomActionItem.group("BuildMenu", "Build");
        build.addChild(CustomActionItem.action("BuildProject", "Build Project", "🔨"));
        build.addChild(CustomActionItem.action("RebuildProject", "Rebuild Project", ""));
        build.addChild(CustomActionItem.action("CleanProject", "Clean Project", ""));
        return build;
    }

    private CustomActionItem buildRunMenu() {
        CustomActionItem run = CustomActionItem.group("RunMenu", "Run");
        run.addChild(CustomActionItem.action("Run", "Run...", "▶"));
        run.addChild(CustomActionItem.action("Debug", "Debug...", "🐞"));
        run.addChild(CustomActionItem.action("EditConfigurations", "Edit Configurations...", "⚙"));
        run.addChild(CustomActionItem.separator());
        run.addChild(CustomActionItem.action("Stop", "Stop", "⏹"));
        return run;
    }

    private CustomActionItem buildToolsMenu() {
        CustomActionItem tools = CustomActionItem.group("ToolsMenu", "Tools");
        tools.addChild(CustomActionItem.group("TasksAndContexts", "Tasks & Contexts"));
        tools.addChild(CustomActionItem.action("Terminal", "Terminal", "💻"));
        tools.addChild(CustomActionItem.action("XmlActions", "XML Actions", ""));
        return tools;
    }

    private CustomActionItem buildGitMenu() {
        CustomActionItem git = CustomActionItem.group("GitMenu", "Git");
        git.addChild(CustomActionItem.action("Commit", "Commit...", "✓"));
        git.addChild(CustomActionItem.action("Push", "Push...", "⬆"));
        git.addChild(CustomActionItem.action("UpdateProject", "Update Project...", "⬇"));
        git.addChild(CustomActionItem.action("Fetch", "Fetch", ""));
        git.addChild(CustomActionItem.separator());
        git.addChild(CustomActionItem.action("Branches", "Branches...", "🌱"));
        git.addChild(CustomActionItem.action("Merge", "Merge...", ""));
        return git;
    }

    private CustomActionItem buildWindowMenu() {
        CustomActionItem win = CustomActionItem.group("WindowMenu", "Window");
        win.addChild(CustomActionItem.action("Minimize", "Minimize", ""));
        win.addChild(CustomActionItem.action("Zoom", "Zoom", ""));
        win.addChild(CustomActionItem.separator());
        win.addChild(CustomActionItem.action("NextProjectWindow", "Next Project Window", ""));
        win.addChild(CustomActionItem.action("PreviousProjectWindow", "Previous Project Window", ""));
        return win;
    }

    private CustomActionItem buildHelpMenu() {
        CustomActionItem help = CustomActionItem.group("HelpMenu", "Help");
        help.addChild(CustomActionItem.action("FindAction", "Find Action...", "🔍"));
        help.addChild(CustomActionItem.action("Documentation", "Documentation", "📖"));
        help.addChild(CustomActionItem.action("TipOfTheDay", "Tip of the Day", "💡"));
        help.addChild(CustomActionItem.separator());
        help.addChild(CustomActionItem.action("About", "About Lumina", ""));
        return help;
    }

    /**
     * Catalog of all registered actions in the IDE available for insertion via "Add Action...".
     */
    public List<CustomActionItem> getAllAvailableActions() {
        List<CustomActionItem> list = new ArrayList<>();
        list.add(CustomActionItem.action("SaveAll", "Save All", "💾"));
        list.add(CustomActionItem.action("ReloadFromDisk", "Reload All from Disk", "🔄"));
        list.add(CustomActionItem.action("OpenFile", "Open File / Project...", "📁"));
        list.add(CustomActionItem.action("CloseProject", "Close Project", ""));
        list.add(CustomActionItem.action("Settings", "Settings...", "⚙"));
        list.add(CustomActionItem.action("InvalidateCaches", "Invalidate Caches...", ""));
        list.add(CustomActionItem.action("Undo", "Undo", "↩"));
        list.add(CustomActionItem.action("Redo", "Redo", "↪"));
        list.add(CustomActionItem.action("Cut", "Cut", "✂"));
        list.add(CustomActionItem.action("Copy", "Copy", "📋"));
        list.add(CustomActionItem.action("Paste", "Paste", "📋"));
        list.add(CustomActionItem.action("Find", "Find", "🔍"));
        list.add(CustomActionItem.action("FindInFiles", "Find in Files...", "🔍"));
        list.add(CustomActionItem.action("Replace", "Replace", ""));
        list.add(CustomActionItem.action("ReplaceInFiles", "Replace in Files...", ""));
        list.add(CustomActionItem.action("SearchEverywhere", "Search Everywhere", "🔍"));
        list.add(CustomActionItem.action("ReformatCode", "Reformat Code", ""));
        list.add(CustomActionItem.action("OptimizeImports", "Optimize Imports", ""));
        list.add(CustomActionItem.action("BuildProject", "Build Project", "🔨"));
        list.add(CustomActionItem.action("Run", "Run", "▶"));
        list.add(CustomActionItem.action("Debug", "Debug", "🐞"));
        list.add(CustomActionItem.action("Stop", "Stop", "⏹"));
        list.add(CustomActionItem.action("GitCommit", "Git: Commit...", "✓"));
        list.add(CustomActionItem.action("GitPush", "Git: Push...", "⬆"));
        list.add(CustomActionItem.action("GitPull", "Git: Pull / Update...", "⬇"));
        list.add(CustomActionItem.action("GitBranches", "Git: Branches...", "🌱"));
        list.add(CustomActionItem.action("Terminal", "Terminal Tool Window", "💻"));
        list.add(CustomActionItem.action("Problems", "Problems Tool Window", "⚠️"));
        list.add(CustomActionItem.action("Database", "Database Tool Window", "🗄"));
        list.add(CustomActionItem.action("Maven", "Maven Tool Window", "📦"));
        return list;
    }

    private void loadFromPreferences() {
        boolean savedModified = prefs.getBoolean("custom_actions_modified", false);
        this.modified = savedModified;
    }
}
