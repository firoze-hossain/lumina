// Add this as a new file: SettingsMenusToolbarsPage.java
package dev.lumina.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ-style Menus and Toolbars settings page.
 * Shows a tree with all menus and toolbars that can be customized.
 */
public class SettingsMenusToolbarsPage extends VBox {

    private final TreeView<MenuItemNode> treeView;
    private final ObservableList<MenuItemNode> menuItems = FXCollections.observableArrayList();
    private final ListView<String> actionList = new ListView<>();
    private final Label actionLabel = new Label("Actions available for main menu:");
    private final Button addButton = new Button("Add...");
    private final Button removeButton = new Button("Remove");
    private final Button resetButton = new Button("Reset");

    private static class MenuItemNode {
        private final String name;
        private final String icon;
        private final boolean isSeparator;
        private final boolean isGroup;
        private final List<MenuItemNode> children = new ArrayList<>();

        public MenuItemNode(String name, String icon, boolean isSeparator, boolean isGroup) {
            this.name = name;
            this.icon = icon;
            this.isSeparator = isSeparator;
            this.isGroup = isGroup;
        }

        public String getName() { return name; }
        public String getIcon() { return icon; }
        public boolean isSeparator() { return isSeparator; }
        public boolean isGroup() { return isGroup; }
        public List<MenuItemNode> getChildren() { return children; }

        public void addChild(MenuItemNode child) {
            children.add(child);
        }

        @Override
        public String toString() {
            if (isSeparator) return "─────────────────────";
            if (isGroup) return icon + "  " + name;
            return icon + "  " + name;
        }
    }

    public SettingsMenusToolbarsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);

        // Build the tree structure from the screenshots
        MenuItemNode root = buildMenuTree();

        treeView = new TreeView<>(createTreeItem(root));
        treeView.setShowRoot(false);
        treeView.setPrefHeight(400);
        treeView.getStyleClass().add("settings-tree");
        treeView.setCellFactory(new Callback<>() {
            @Override
            public TreeCell<MenuItemNode> call(TreeView<MenuItemNode> tv) {
                return new TreeCell<>() {
                    @Override
                    protected void updateItem(MenuItemNode item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                            return;
                        }
                        if (item.isSeparator()) {
                            setText("──────────────");
                            setStyle("-fx-text-fill: #697089;");
                            setGraphic(null);
                            return;
                        }
                        setText(item.toString());
                        setStyle(null);
                        if (item.isGroup()) {
                            setStyle("-fx-font-weight: bold; -fx-text-fill: #A6ADC4;");
                        }
                    }
                };
            }
        });

        // Action list for "Add..." dialog
        actionList.getStyleClass().add("goto-list");
        actionList.setPrefHeight(120);
        actionList.getItems().addAll(getAvailableActions());

        // Buttons
        addButton.getStyleClass().add("dialog-secondary");
        removeButton.getStyleClass().add("dialog-secondary");
        resetButton.getStyleClass().add("dialog-secondary");
        resetButton.setStyle("-fx-border-color: #E5534B; -fx-text-fill: #E5534B;");

        addButton.setOnAction(e -> showAddActionDialog());
        removeButton.setOnAction(e -> removeSelectedAction());
        resetButton.setOnAction(e -> resetMenuTree());

        HBox buttonBar = new HBox(10, addButton, removeButton, resetButton);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(8, 0, 0, 0));

        // Layout
        VBox treeBox = new VBox(8, new Label("Menus and Toolbars"), treeView);
        treeBox.setPadding(new Insets(0, 0, 8, 0));

        VBox actionBox = new VBox(6, actionLabel, actionList);
        actionBox.setPadding(new Insets(8, 0, 0, 0));

        getChildren().addAll(treeBox, buttonBar, actionBox);
    }

    private MenuItemNode buildMenuTree() {
        // Build the complete menu structure from the screenshots
        MenuItemNode root = new MenuItemNode("Root", "", false, true);

        // File menu
        MenuItemNode fileMenu = new MenuItemNode("File", "📄", false, true);
        fileMenu.addChild(new MenuItemNode("New", "📄", false, false));
        fileMenu.addChild(new MenuItemNode("Open…", "", false, false));
        fileMenu.addChild(new MenuItemNode("Recent Projects", "", false, false));
        fileMenu.addChild(new MenuItemNode("Close Project", "", false, false));
        fileMenu.addChild(new MenuItemNode("", "", true, false));
        fileMenu.addChild(new MenuItemNode("Remote Development…", "", false, false));
        fileMenu.addChild(new MenuItemNode("", "", true, false));
        fileMenu.addChild(new MenuItemNode("Settings…", "⚙", false, false));
        fileMenu.addChild(new MenuItemNode("Project Structure…", "", false, false));
        fileMenu.addChild(new MenuItemNode("File Properties", "", false, false));
        fileMenu.addChild(new MenuItemNode("Local History", "", false, false));
        fileMenu.addChild(new MenuItemNode("", "", true, false));
        fileMenu.addChild(new MenuItemNode("Save All", "💾", false, false));
        fileMenu.addChild(new MenuItemNode("Reload All from Disk", "", false, false));
        fileMenu.addChild(new MenuItemNode("Repair IDE", "", false, false));
        fileMenu.addChild(new MenuItemNode("Invalidate Caches…", "", false, false));
        fileMenu.addChild(new MenuItemNode("", "", true, false));
        fileMenu.addChild(new MenuItemNode("Manage Settings", "", false, false));
        fileMenu.addChild(new MenuItemNode("New Projects Setup", "", false, false));
        fileMenu.addChild(new MenuItemNode("Save File as Template…", "", false, false));
        fileMenu.addChild(new MenuItemNode("", "", true, false));
        fileMenu.addChild(new MenuItemNode("Export", "", false, false));
        fileMenu.addChild(new MenuItemNode("Print…", "🖨", false, false));
        fileMenu.addChild(new MenuItemNode("", "", true, false));
        fileMenu.addChild(new MenuItemNode("Power Save Mode", "", false, false));
        fileMenu.addChild(new MenuItemNode("", "", true, false));
        fileMenu.addChild(new MenuItemNode("Exit", "✕", false, false));
        root.addChild(fileMenu);

        // Edit menu
        MenuItemNode editMenu = new MenuItemNode("Edit", "✎", false, true);
        editMenu.addChild(new MenuItemNode("Undo", "↩", false, false));
        editMenu.addChild(new MenuItemNode("Redo", "↪", false, false));
        editMenu.addChild(new MenuItemNode("", "", true, false));
        editMenu.addChild(new MenuItemNode("Cut", "✂", false, false));
        editMenu.addChild(new MenuItemNode("Copy", "📋", false, false));
        editMenu.addChild(new MenuItemNode("Copy Path/Reference…", "", false, false));
        editMenu.addChild(new MenuItemNode("Paste", "📋", false, false));
        editMenu.addChild(new MenuItemNode("Delete", "🗑", false, false));
        editMenu.addChild(new MenuItemNode("", "", true, false));
        editMenu.addChild(new MenuItemNode("Search In Selection", "", false, false));
        editMenu.addChild(new MenuItemNode("Find", "🔍", false, false));
        editMenu.addChild(new MenuItemNode("Find Usages", "", false, false));
        editMenu.addChild(new MenuItemNode("", "", true, false));
        editMenu.addChild(new MenuItemNode("Column Selection Mode", "", false, false));
        editMenu.addChild(new MenuItemNode("Select All", "", false, false));
        editMenu.addChild(new MenuItemNode("Add Carets to Ends of Selected Lines", "", false, false));
        editMenu.addChild(new MenuItemNode("Extend Selection", "", false, false));
        editMenu.addChild(new MenuItemNode("Shrink Selection", "", false, false));
        editMenu.addChild(new MenuItemNode("", "", true, false));
        editMenu.addChild(new MenuItemNode("Toggle Case", "", false, false));
        editMenu.addChild(new MenuItemNode("Join Lines", "", false, false));
        editMenu.addChild(new MenuItemNode("Duplicate Line", "", false, false));
        editMenu.addChild(new MenuItemNode("Fill Paragraph", "", false, false));
        editMenu.addChild(new MenuItemNode("Sort Lines", "", false, false));
        editMenu.addChild(new MenuItemNode("Reverse Lines", "", false, false));
        editMenu.addChild(new MenuItemNode("Transpose", "", false, false));
        editMenu.addChild(new MenuItemNode("", "", true, false));
        editMenu.addChild(new MenuItemNode("Indent Selection", "", false, false));
        editMenu.addChild(new MenuItemNode("Unindent Line or Selection", "", false, false));
        editMenu.addChild(new MenuItemNode("Convert Indents", "", false, false));
        editMenu.addChild(new MenuItemNode("", "", true, false));
        editMenu.addChild(new MenuItemNode("Macros", "", false, false));
        editMenu.addChild(new MenuItemNode("Bookmarks", "", false, false));
        editMenu.addChild(new MenuItemNode("Emoji & Symbols", "", false, false));
        root.addChild(editMenu);

        // View menu
        MenuItemNode viewMenu = new MenuItemNode("View", "👁", false, true);
        viewMenu.addChild(new MenuItemNode("Tool Windows", "", false, false));
        viewMenu.addChild(new MenuItemNode("Appearance", "", false, false));
        viewMenu.addChild(new MenuItemNode("", "", true, false));
        viewMenu.addChild(new MenuItemNode("Quick Definition", "", false, false));
        viewMenu.addChild(new MenuItemNode("Show Siblings", "", false, false));
        viewMenu.addChild(new MenuItemNode("Quick Type Definition", "", false, false));
        viewMenu.addChild(new MenuItemNode("Quick Documentation", "", false, false));
        viewMenu.addChild(new MenuItemNode("Show Bytecode", "", false, false));
        viewMenu.addChild(new MenuItemNode("Parameter Info", "", false, false));
        viewMenu.addChild(new MenuItemNode("Type Info", "", false, false));
        viewMenu.addChild(new MenuItemNode("Context Info", "", false, false));
        viewMenu.addChild(new MenuItemNode("", "", true, false));
        viewMenu.addChild(new MenuItemNode("Jump to Source", "", false, false));
        viewMenu.addChild(new MenuItemNode("Recent Locations", "", false, false));
        viewMenu.addChild(new MenuItemNode("Recent Files", "", false, false));
        viewMenu.addChild(new MenuItemNode("Recently Changed Files", "", false, false));
        viewMenu.addChild(new MenuItemNode("Recent Changes", "", false, false));
        viewMenu.addChild(new MenuItemNode("", "", true, false));
        viewMenu.addChild(new MenuItemNode("Compare With…", "", false, false));
        viewMenu.addChild(new MenuItemNode("Compare with Clipboard", "", false, false));
        viewMenu.addChild(new MenuItemNode("", "", true, false));
        viewMenu.addChild(new MenuItemNode("Quick Switch Scheme…", "", false, false));
        viewMenu.addChild(new MenuItemNode("Active Editor", "", false, false));
        viewMenu.addChild(new MenuItemNode("Increase Font Size in All Editors", "", false, false));
        viewMenu.addChild(new MenuItemNode("Decrease Font Size in All Editors", "", false, false));
        viewMenu.addChild(new MenuItemNode("Reset Font Size in All Editors", "", false, false));
        viewMenu.addChild(new MenuItemNode("Bidi Text Base Direction", "", false, false));
        root.addChild(viewMenu);

        // Navigate menu
        MenuItemNode navigateMenu = new MenuItemNode("Navigate", "🧭", false, true);
        navigateMenu.addChild(new MenuItemNode("Back", "←", false, false));
        navigateMenu.addChild(new MenuItemNode("Forward", "→", false, false));
        navigateMenu.addChild(new MenuItemNode("", "", true, false));
        navigateMenu.addChild(new MenuItemNode("Search Everywhere (double Shift)", "🔍", false, false));
        navigateMenu.addChild(new MenuItemNode("Class…", "", false, false));
        navigateMenu.addChild(new MenuItemNode("File…", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Symbol…", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Text…", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Line:Column…", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Endpoint…", "", false, false));
        navigateMenu.addChild(new MenuItemNode("", "", true, false));
        navigateMenu.addChild(new MenuItemNode("Next Highlighted Error", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Previous Highlighted Error", "", false, false));
        navigateMenu.addChild(new MenuItemNode("", "", true, false));
        navigateMenu.addChild(new MenuItemNode("Last Edit Location", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Next Edit Location", "", false, false));
        navigateMenu.addChild(new MenuItemNode("", "", true, false));
        navigateMenu.addChild(new MenuItemNode("Navigate in File", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Select In…", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Jump to Navigation Bar", "", false, false));
        navigateMenu.addChild(new MenuItemNode("", "", true, false));
        navigateMenu.addChild(new MenuItemNode("Go to Declaration", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Implementation(s)", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Type Declaration", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Super Class or Interface", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Test", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Related Symbol…", "", false, false));
        navigateMenu.addChild(new MenuItemNode("", "", true, false));
        navigateMenu.addChild(new MenuItemNode("File Structure", "", false, false));
        navigateMenu.addChild(new MenuItemNode("File Path", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Type Hierarchy", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Method Hierarchy", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Call Hierarchy", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Quick Documentation", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Parameter Info", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Find Usages", "", false, false));
        navigateMenu.addChild(new MenuItemNode("", "", true, false));
        navigateMenu.addChild(new MenuItemNode("Go to File…", "", false, false));
        navigateMenu.addChild(new MenuItemNode("Go to Line…", "", false, false));
        root.addChild(navigateMenu);

        // Code menu
        MenuItemNode codeMenu = new MenuItemNode("Code", "◉", false, true);
        codeMenu.addChild(new MenuItemNode("Override Methods…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Implement Methods…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Delegate Methods…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Generate…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Code Completion", "", false, false));
        codeMenu.addChild(new MenuItemNode("", "", true, false));
        codeMenu.addChild(new MenuItemNode("Inspect Code…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Code Cleanup…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Analyze Code", "", false, false));
        codeMenu.addChild(new MenuItemNode("Analyze Stack Trace or Thread Dump…", "", false, false));
        codeMenu.addChild(new MenuItemNode("", "", true, false));
        codeMenu.addChild(new MenuItemNode("Insert Live Template…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Save as Live Template…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Surround With…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Unwrap/Remove…", "", false, false));
        codeMenu.addChild(new MenuItemNode("", "", true, false));
        codeMenu.addChild(new MenuItemNode("Folding", "", false, false));
        codeMenu.addChild(new MenuItemNode("Comment with Line Comment", "", false, false));
        codeMenu.addChild(new MenuItemNode("Comment with Block Comment", "", false, false));
        codeMenu.addChild(new MenuItemNode("Reformat Code", "", false, false));
        codeMenu.addChild(new MenuItemNode("Reformat File…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Auto-Indent Lines", "", false, false));
        codeMenu.addChild(new MenuItemNode("Optimize Imports", "", false, false));
        codeMenu.addChild(new MenuItemNode("Rearrange Code", "", false, false));
        codeMenu.addChild(new MenuItemNode("", "", true, false));
        codeMenu.addChild(new MenuItemNode("Move Statement Down", "", false, false));
        codeMenu.addChild(new MenuItemNode("Move Statement Up", "", false, false));
        codeMenu.addChild(new MenuItemNode("Move Element Left", "", false, false));
        codeMenu.addChild(new MenuItemNode("Move Element Right", "", false, false));
        codeMenu.addChild(new MenuItemNode("Move Line Down", "", false, false));
        codeMenu.addChild(new MenuItemNode("Move Line Up", "", false, false));
        codeMenu.addChild(new MenuItemNode("", "", true, false));
        codeMenu.addChild(new MenuItemNode("Update Copyright…", "", false, false));
        codeMenu.addChild(new MenuItemNode("Generate module-info Descriptors", "", false, false));
        codeMenu.addChild(new MenuItemNode("Reformat File with Rustfmt", "", false, false));
        codeMenu.addChild(new MenuItemNode("Reformat Cargo Project with Rustfmt", "", false, false));
        codeMenu.addChild(new MenuItemNode("", "", true, false));
        codeMenu.addChild(new MenuItemNode("Convert Java File to Kotlin File", "", false, false));
        codeMenu.addChild(new MenuItemNode("Generate Test for Current Class", "", false, false));
        root.addChild(codeMenu);

        // Refactor menu
        MenuItemNode refactorMenu = new MenuItemNode("Refactor", "↺", false, true);
        refactorMenu.addChild(new MenuItemNode("Refactor This…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Rename…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Rename File…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Change Signature…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Extract/Introduce", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Inline…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Find and Replace Code Duplicates…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("", "", true, false));
        refactorMenu.addChild(new MenuItemNode("Move Class…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Copy Class…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Safe Delete…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("", "", true, false));
        refactorMenu.addChild(new MenuItemNode("Pull Members Up…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Push Members Down…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("", "", true, false));
        refactorMenu.addChild(new MenuItemNode("Type Migration…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Make Static…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Convert To Instance Method…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("", "", true, false));
        refactorMenu.addChild(new MenuItemNode("Use Interface Where Possible…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Replace Inheritance with Delegation…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Encapsulate Fields…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Migrate Packages and Classes", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Invert Boolean…", "", false, false));
        refactorMenu.addChild(new MenuItemNode("Internationalize…", "", false, false));
        root.addChild(refactorMenu);

        // Build menu
        MenuItemNode buildMenu = new MenuItemNode("Build", "⚒", false, true);
        buildMenu.addChild(new MenuItemNode("Build Project", "", false, false));
        buildMenu.addChild(new MenuItemNode("Build Module 'lumina-ide'", "", false, false));
        buildMenu.addChild(new MenuItemNode("Recompile 'ProjectSpec.java'", "", false, false));
        buildMenu.addChild(new MenuItemNode("Rebuild Project", "", false, false));
        buildMenu.addChild(new MenuItemNode("Build Artifacts…", "", false, false));
        buildMenu.addChild(new MenuItemNode("Groovy Resources", "", false, false));
        root.addChild(buildMenu);

        // Run menu
        MenuItemNode runMenu = new MenuItemNode("Run", "▶", false, true);
        runMenu.addChild(new MenuItemNode("Run 'LuminaApp'", "", false, false));
        runMenu.addChild(new MenuItemNode("Debug 'LuminaApp'", "", false, false));
        runMenu.addChild(new MenuItemNode("Run 'LuminaApp' with Coverage", "", false, false));
        runMenu.addChild(new MenuItemNode("Profile 'LuminaApp' with 'IntelliJ Profiler'", "", false, false));
        runMenu.addChild(new MenuItemNode("", "", true, false));
        runMenu.addChild(new MenuItemNode("Run…", "", false, false));
        runMenu.addChild(new MenuItemNode("Debug…", "", false, false));
        runMenu.addChild(new MenuItemNode("Attach to Process…", "", false, false));
        runMenu.addChild(new MenuItemNode("Edit Configurations…", "", false, false));
        runMenu.addChild(new MenuItemNode("Manage Targets…", "", false, false));
        runMenu.addChild(new MenuItemNode("", "", true, false));
        runMenu.addChild(new MenuItemNode("Stop", "", false, false));
        runMenu.addChild(new MenuItemNode("Stop Background Processes…", "", false, false));
        runMenu.addChild(new MenuItemNode("Show Running List", "", false, false));
        runMenu.addChild(new MenuItemNode("", "", true, false));
        runMenu.addChild(new MenuItemNode("Debugging Actions", "", false, false));
        runMenu.addChild(new MenuItemNode("Toggle Breakpoint", "", false, false));
        runMenu.addChild(new MenuItemNode("View Breakpoints…", "", false, false));
        runMenu.addChild(new MenuItemNode("Test History", "", false, false));
        runMenu.addChild(new MenuItemNode("Import Tests from File…", "", false, false));
        runMenu.addChild(new MenuItemNode("Manage Coverage Reports…", "", false, false));
        runMenu.addChild(new MenuItemNode("", "", true, false));
        runMenu.addChild(new MenuItemNode("Attach Profiler to Process…", "", false, false));
        runMenu.addChild(new MenuItemNode("Open Profiler Snapshot", "", false, false));
        runMenu.addChild(new MenuItemNode("", "", true, false));
        runMenu.addChild(new MenuItemNode("Run All Tests", "", false, false));
        runMenu.addChild(new MenuItemNode("Run Current Test Class", "", false, false));
        runMenu.addChild(new MenuItemNode("Clear Run Output", "", false, false));
        root.addChild(runMenu);

        // Git menu
        MenuItemNode gitMenu = new MenuItemNode("Git", "🔀", false, true);
        gitMenu.addChild(new MenuItemNode("Commit…", "", false, false));
        gitMenu.addChild(new MenuItemNode("Push…", "", false, false));
        gitMenu.addChild(new MenuItemNode("Update Project…", "", false, false));
        gitMenu.addChild(new MenuItemNode("Pull…", "", false, false));
        gitMenu.addChild(new MenuItemNode("Fetch", "", false, false));
        gitMenu.addChild(new MenuItemNode("", "", true, false));
        gitMenu.addChild(new MenuItemNode("Merge…", "", false, false));
        gitMenu.addChild(new MenuItemNode("Rebase…", "", false, false));
        gitMenu.addChild(new MenuItemNode("", "", true, false));
        gitMenu.addChild(new MenuItemNode("Branches…", "", false, false));
        gitMenu.addChild(new MenuItemNode("New Branch…", "", false, false));
        gitMenu.addChild(new MenuItemNode("New Tag…", "", false, false));
        gitMenu.addChild(new MenuItemNode("Reset HEAD…", "", false, false));
        gitMenu.addChild(new MenuItemNode("Show Git Log", "", false, false));
        gitMenu.addChild(new MenuItemNode("Patch", "", false, false));
        gitMenu.addChild(new MenuItemNode("Uncommitted Changes", "", false, false));
        gitMenu.addChild(new MenuItemNode("Current File", "", false, false));
        gitMenu.addChild(new MenuItemNode("GitLab", "", false, false));
        gitMenu.addChild(new MenuItemNode("GitHub", "", false, false));
        gitMenu.addChild(new MenuItemNode("Manage Remotes…", "", false, false));
        gitMenu.addChild(new MenuItemNode("Clone…", "", false, false));
        gitMenu.addChild(new MenuItemNode("", "", true, false));
        gitMenu.addChild(new MenuItemNode("VCS Operations Popup…", "", false, false));
        root.addChild(gitMenu);

        // Tools menu
        MenuItemNode toolsMenu = new MenuItemNode("Tools", "🔧", false, true);
        toolsMenu.addChild(new MenuItemNode("AI Assistant", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Tasks & Contexts", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Code With Me…", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Generate Javadoc…", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Create Command Line Launcher…", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Create Desktop Entry…", "", false, false));
        toolsMenu.addChild(new MenuItemNode("", "", true, false));
        toolsMenu.addChild(new MenuItemNode("Services", "", false, false));
        toolsMenu.addChild(new MenuItemNode("XML Actions", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Markdown", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Create IntelliJ IDEA Plugin…", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Security Analysis", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Deployment", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Start SSH Session…", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Groovy Console", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Kotlin", "", false, false));
        toolsMenu.addChild(new MenuItemNode("HTTP Client", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Qodana", "", false, false));
        toolsMenu.addChild(new MenuItemNode("GitHub Copilot", "", false, false));
        toolsMenu.addChild(new MenuItemNode("", "", true, false));
        toolsMenu.addChild(new MenuItemNode("New Project Wizard…", "", false, false));
        toolsMenu.addChild(new MenuItemNode("Plugins…", "", false, false));
        root.addChild(toolsMenu);

        // Window menu
        MenuItemNode windowMenu = new MenuItemNode("Window", "▭", false, true);
        windowMenu.addChild(new MenuItemNode("Layouts", "", false, false));
        windowMenu.addChild(new MenuItemNode("Active Tool Window", "", false, false));
        windowMenu.addChild(new MenuItemNode("Editor Tabs", "", false, false));
        windowMenu.addChild(new MenuItemNode("Notifications", "", false, false));
        windowMenu.addChild(new MenuItemNode("Processes", "", false, false));
        windowMenu.addChild(new MenuItemNode("", "", true, false));
        windowMenu.addChild(new MenuItemNode("Next Project Window", "", false, false));
        windowMenu.addChild(new MenuItemNode("Previous Project Window", "", false, false));
        windowMenu.addChild(new MenuItemNode("", "", true, false));
        windowMenu.addChild(new MenuItemNode("Toggle Project Panel", "", false, false));
        windowMenu.addChild(new MenuItemNode("Toggle Bottom Panel", "", false, false));
        windowMenu.addChild(new MenuItemNode("Lumina", "", false, false));
        root.addChild(windowMenu);

        // Help menu
        MenuItemNode helpMenu = new MenuItemNode("Help", "?", false, true);
        helpMenu.addChild(new MenuItemNode("Find Action…", "", false, false));
        helpMenu.addChild(new MenuItemNode("Help", "", false, false));
        helpMenu.addChild(new MenuItemNode("Learn IDE Features", "", false, false));
        helpMenu.addChild(new MenuItemNode("", "", true, false));
        helpMenu.addChild(new MenuItemNode("What's New in IntelliJ IDEA", "", false, false));
        helpMenu.addChild(new MenuItemNode("Getting Started", "", false, false));
        helpMenu.addChild(new MenuItemNode("IntelliJ IDEA on YouTube", "", false, false));
        helpMenu.addChild(new MenuItemNode("Keyboard Shortcuts PDF", "", false, false));
        helpMenu.addChild(new MenuItemNode("Tip of the Day", "", false, false));
        helpMenu.addChild(new MenuItemNode("", "", true, false));
        helpMenu.addChild(new MenuItemNode("My Productivity", "", false, false));
        helpMenu.addChild(new MenuItemNode("Contact Support…", "", false, false));
        helpMenu.addChild(new MenuItemNode("Submit a Bug Report…", "", false, false));
        helpMenu.addChild(new MenuItemNode("Submit Feedback…", "", false, false));
        helpMenu.addChild(new MenuItemNode("", "", true, false));
        helpMenu.addChild(new MenuItemNode("Show Log in Files", "", false, false));
        helpMenu.addChild(new MenuItemNode("Show SQL Log in Files", "", false, false));
        helpMenu.addChild(new MenuItemNode("Collect Logs and Diagnostic Data", "", false, false));
        helpMenu.addChild(new MenuItemNode("Delete Leftover IDE Directories…", "", false, false));
        helpMenu.addChild(new MenuItemNode("Diagnostic Tools", "", false, false));
        helpMenu.addChild(new MenuItemNode("Change Memory Settings", "", false, false));
        helpMenu.addChild(new MenuItemNode("Edit Custom Properties…", "", false, false));
        helpMenu.addChild(new MenuItemNode("Edit Custom VM Options…", "", false, false));
        helpMenu.addChild(new MenuItemNode("Manage Subscriptions…", "", false, false));
        helpMenu.addChild(new MenuItemNode("", "", true, false));
        helpMenu.addChild(new MenuItemNode("Check for Updates…", "", false, false));
        helpMenu.addChild(new MenuItemNode("About", "", false, false));
        root.addChild(helpMenu);

        // Additional popup menus from the second screenshot
        MenuItemNode popupMenus = new MenuItemNode("Popup Menus", "📋", false, true);
        popupMenus.addChild(new MenuItemNode("Editor Popup Menu", "", false, false));
        popupMenus.addChild(new MenuItemNode("Editor Gutter Popup Menu", "", false, false));
        popupMenus.addChild(new MenuItemNode("Editor Tab Popup Menu", "", false, false));
        popupMenus.addChild(new MenuItemNode("Project View Popup Menu", "", false, false));
        popupMenus.addChild(new MenuItemNode("Scope View Popup Menu", "", false, false));
        popupMenus.addChild(new MenuItemNode("Navigation Bar Popup Menu", "", false, false));
        root.addChild(popupMenus);

        // Toolbars from the second screenshot
        MenuItemNode toolbars = new MenuItemNode("Toolbars", "🧰", false, true);
        toolbars.addChild(new MenuItemNode("Navigation Bar Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("Debug Header More Popup", "", false, false));
        toolbars.addChild(new MenuItemNode("Debug Header Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("Debug Watches Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("File History Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("Floating Code Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("Floating Code Toolbar Web", "", false, false));
        toolbars.addChild(new MenuItemNode("Jupyter Cell Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("Jupyter Editor Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("Markdown Editor Floating Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("Quick Actions Popup Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("Run Tool Window Header More Popup", "", false, false));
        toolbars.addChild(new MenuItemNode("Run Tool Window Header Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("SQL Floating Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("Tables Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("VCS Local Changes Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("VCS Log Changes Browser Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("VCS Log Toolbar", "", false, false));
        toolbars.addChild(new MenuItemNode("VCS Operations Popup", "", false, false));
        root.addChild(toolbars);

        return root;
    }

    private TreeItem<MenuItemNode> createTreeItem(MenuItemNode node) {
        TreeItem<MenuItemNode> item = new TreeItem<>(node);
        for (MenuItemNode child : node.getChildren()) {
            item.getChildren().add(createTreeItem(child));
        }
        return item;
    }

    private List<String> getAvailableActions() {
        List<String> actions = new ArrayList<>();
        actions.add("New Project…");
        actions.add("Open…");
        actions.add("Close Project");
        actions.add("Save All");
        actions.add("Settings…");
        actions.add("Project Structure…");
        actions.add("Find…");
        actions.add("Replace…");
        actions.add("Find in Files…");
        actions.add("Search Everywhere");
        actions.add("Undo");
        actions.add("Redo");
        actions.add("Cut");
        actions.add("Copy");
        actions.add("Paste");
        actions.add("Delete");
        actions.add("Go to Declaration");
        actions.add("Go to File…");
        actions.add("Go to Line…");
        actions.add("Find Usages");
        actions.add("Quick Documentation");
        actions.add("Parameter Info");
        actions.add("Run");
        actions.add("Debug");
        actions.add("Stop");
        actions.add("Build Project");
        actions.add("Rebuild Project");
        actions.add("Rename…");
        actions.add("Refactor This…");
        actions.add("Commit…");
        actions.add("Push…");
        actions.add("Pull…");
        actions.add("Fetch");
        actions.add("New Branch…");
        actions.add("Git Status");
        actions.add("Terminal");
        actions.add("Maven Panel");
        actions.add("Database Panel");
        actions.add("Test Results");
        actions.add("Problems");
        return actions;
    }

    private void showAddActionDialog() {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Action");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label label = new Label("Select action to add to menu:");
        label.getStyleClass().add("settings-label");

        ListView<String> list = new ListView<>(FXCollections.observableArrayList(getAvailableActions()));
        list.setPrefHeight(200);
        list.getSelectionModel().selectFirst();

        Button add = new Button("Add");
        add.getStyleClass().add("dialog-primary");
        add.setOnAction(e -> {
            String selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Add to the currently selected menu item
                TreeItem<MenuItemNode> selectedItem = treeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.getValue() != null) {
                    MenuItemNode parent = selectedItem.getValue();
                    if (!parent.isSeparator()) {
                        MenuItemNode newItem = new MenuItemNode(selected, "", false, false);
                        parent.addChild(newItem);
                        // Refresh tree
                        TreeItem<MenuItemNode> newRoot = createTreeItem(buildMenuTree());
                        treeView.setRoot(newRoot);
                    }
                }
                dialog.close();
            }
        });

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, add, cancel);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        content.getChildren().addAll(label, list, buttons);

        Scene scene = new Scene(content, 400, 320);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void removeSelectedAction() {
        TreeItem<MenuItemNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getParent() != null) {
            MenuItemNode node = selected.getValue();
            if (node != null && !node.isGroup() && !node.isSeparator()) {
                TreeItem<MenuItemNode> parent = selected.getParent();
                if (parent != null) {
                    parent.getChildren().remove(selected);
                }
            }
        }
    }

    private void resetMenuTree() {
        TreeItem<MenuItemNode> newRoot = createTreeItem(buildMenuTree());
        treeView.setRoot(newRoot);
    }
}