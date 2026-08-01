package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Keymap settings page.
 * Exactly as shown in the screenshot.
 */
public class SettingsKeymapPage extends VBox {

    public SettingsKeymapPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);

        // ---- Keymap selector ----
        HBox keymapRow = new HBox(10);
        keymapRow.setAlignment(Pos.CENTER_LEFT);

        Label keymapLabel = new Label("Keymap:");
        keymapLabel.getStyleClass().add("settings-label");

        ComboBox<String> keymapCombo = new ComboBox<>();
        keymapCombo.getItems().addAll("GNOME", "Windows", "macOS", "Linux", "Visual Studio", "Eclipse", "NetBeans", "Emacs");
        keymapCombo.getSelectionModel().select("GNOME");
        keymapCombo.getStyleClass().add("settings-combo");
        keymapCombo.setPrefWidth(200);

        Hyperlink getMore = new Hyperlink("Get more keymaps in Settings | Plugins");
        getMore.getStyleClass().add("settings-link");

        keymapRow.getChildren().addAll(keymapLabel, keymapCombo, getMore);

        // ---- Action tree ----
        Label actionsLabel = new Label("Editor Actions");
        actionsLabel.getStyleClass().add("settings-section");

        TreeView<String> actionTree = buildActionTree();
        actionTree.getStyleClass().add("settings-tree");
        actionTree.setPrefHeight(320);
        actionTree.setShowRoot(false);

        // Expand first level
        for (TreeItem<String> item : actionTree.getRoot().getChildren()) {
            item.setExpanded(true);
        }

        // ---- National layouts checkbox ----
        CheckBox nationalLayouts = new CheckBox("Use national layouts for shortcuts (requires restart)");
        nationalLayouts.getStyleClass().add("settings-check");

        VBox content = new VBox(12, keymapRow, actionsLabel, actionTree, nationalLayouts);
        getChildren().addAll(content);
    }

    private TreeView<String> buildActionTree() {
        TreeItem<String> root = new TreeItem<>("Root");
        root.setExpanded(true);

        // Editor Actions
        TreeItem<String> editorActions = new TreeItem<>("Editor Actions");
        editorActions.getChildren().addAll(
            createCategory("Complete Current Statement"),
            createCategory("Delete Line"),
            createCategory("Duplicate Line"),
            createCategory("Start New Line"),
            createCategory("Toggle Case"),
            createCategory("Join Lines"),
            createCategory("Comment with Line Comment"),
            createCategory("Comment with Block Comment"),
            createCategory("Reformat Code"),
            createCategory("Auto-Indent Lines"),
            createCategory("Optimize Imports"),
            createCategory("Move Statement Down"),
            createCategory("Move Statement Up"),
            createCategory("Move Line Down"),
            createCategory("Move Line Up")
        );
        root.getChildren().add(editorActions);

        // Main Menu
        TreeItem<String> mainMenu = new TreeItem<>("Main Menu");
        mainMenu.getChildren().addAll(
            createCategory("File"),
            createCategory("Edit"),
            createCategory("View"),
            createCategory("Navigate"),
            createCategory("Code"),
            createCategory("Refactor"),
            createCategory("Build"),
            createCategory("Run"),
            createCategory("Tools"),
            createCategory("Git"),
            createCategory("Window"),
            createCategory("Help")
        );
        root.getChildren().add(mainMenu);

        // Tool Windows
        TreeItem<String> toolWindows = new TreeItem<>("Tool Windows");
        toolWindows.getChildren().addAll(
            createCategory("Project"),
            createCategory("Run"),
            createCategory("Debug"),
            createCategory("Terminal"),
            createCategory("Version Control"),
            createCategory("Structure"),
            createCategory("Maven"),
            createCategory("Database"),
            createCategory("Services"),
            createCategory("Problems")
        );
        root.getChildren().add(toolWindows);

        // External Tools
        TreeItem<String> externalTools = new TreeItem<>("External Tools");
        externalTools.getChildren().addAll(
            createCategory("External Tool 1"),
            createCategory("External Tool 2")
        );
        root.getChildren().add(externalTools);

        // External Build Systems
        TreeItem<String> externalBuild = new TreeItem<>("External Build Systems");
        externalBuild.getChildren().addAll(
            createCategory("Maven"),
            createCategory("Gradle"),
            createCategory("Makefile")
        );
        root.getChildren().add(externalBuild);

        // Version Control Systems
        TreeItem<String> vcs = new TreeItem<>("Version Control Systems");
        vcs.getChildren().addAll(
            createCategory("Git"),
            createCategory("Commit"),
            createCategory("Push"),
            createCategory("Pull"),
            createCategory("Fetch"),
            createCategory("Merge"),
            createCategory("Rebase"),
            createCategory("Branch Operations"),
            createCategory("Show History")
        );
        root.getChildren().add(vcs);

        // Debugger Actions
        TreeItem<String> debugger = new TreeItem<>("Debugger Actions");
        debugger.getChildren().addAll(
            createCategory("Step Over"),
            createCategory("Step Into"),
            createCategory("Step Out"),
            createCategory("Resume Program"),
            createCategory("Pause Program"),
            createCategory("Toggle Breakpoint"),
            createCategory("Evaluate Expression"),
            createCategory("Show Execution Point")
        );
        root.getChildren().add(debugger);

        // Remote External Tools
        TreeItem<String> remoteTools = new TreeItem<>("Remote External Tools");
        remoteTools.getChildren().addAll(
            createCategory("SSH"),
            createCategory("Dev Containers")
        );
        root.getChildren().add(remoteTools);

        // Database
        TreeItem<String> database = new TreeItem<>("Database");
        database.getChildren().addAll(
            createCategory("Execute Query"),
            createCategory("Explain Plan"),
            createCategory("Data Editor"),
            createCategory("Show Table Data")
        );
        root.getChildren().add(database);

        // Macros
        TreeItem<String> macros = new TreeItem<>("Macros");
        macros.getChildren().addAll(
            createCategory("Start Macro Recording"),
            createCategory("Stop Macro Recording"),
            createCategory("Playback Macro 1")
        );
        root.getChildren().add(macros);

        // Intentions
        TreeItem<String> intentions = new TreeItem<>("Intentions");
        intentions.getChildren().addAll(
            createCategory("Show Intention Actions"),
            createCategory("Quick Fix")
        );
        root.getChildren().add(intentions);

        // Quick Lists
        TreeItem<String> quickLists = new TreeItem<>("Quick Lists");
        quickLists.getChildren().addAll(
            createCategory("Quick List 1"),
            createCategory("Quick List 2")
        );
        root.getChildren().add(quickLists);

        // Plugins
        TreeItem<String> plugins = new TreeItem<>("Plugins");
        plugins.getChildren().addAll(
            createCategory("Plugin Actions")
        );
        root.getChildren().add(plugins);

        // Other
        TreeItem<String> other = new TreeItem<>("Other");
        other.getChildren().addAll(
            createCategory("Find Action"),
            createCategory("Search Everywhere"),
            createCategory("Go to File"),
            createCategory("Recent Files"),
            createCategory("Quick Documentation"),
            createCategory("Parameter Info"),
            createCategory("Type Info")
        );
        root.getChildren().add(other);

        TreeView<String> treeView = new TreeView<>(root);
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                // Check if this is a category (has children)
                TreeItem<String> treeItem = getTreeItem();
                boolean hasChildren = treeItem != null && !treeItem.getChildren().isEmpty();
                
                if (hasChildren) {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #A6ADC4;");
                } else {
                    // Action item - show shortcut if available
                    String display = item;
                    // Add sample shortcuts for some actions
                    if (item.equals("Complete Current Statement")) display = "Complete Current Statement  \u00A0 Ctrl+Space";
                    else if (item.equals("Delete Line")) display = "Delete Line  \u00A0 Ctrl+Y";
                    else if (item.equals("Duplicate Line")) display = "Duplicate Line  \u00A0 Ctrl+D";
                    else if (item.equals("Comment with Line Comment")) display = "Comment with Line Comment  \u00A0 Ctrl+/";
                    else if (item.equals("Comment with Block Comment")) display = "Comment with Block Comment  \u00A0 Ctrl+Shift+/";
                    else if (item.equals("Reformat Code")) display = "Reformat Code  \u00A0 Ctrl+Alt+L";
                    else if (item.equals("Optimize Imports")) display = "Optimize Imports  \u00A0 Ctrl+Alt+O";
                    else if (item.equals("Find Action")) display = "Find Action  \u00A0 Ctrl+Shift+A";
                    else if (item.equals("Search Everywhere")) display = "Search Everywhere  \u00A0 Double Shift";
                    else if (item.equals("Go to File")) display = "Go to File  \u00A0 Ctrl+Shift+N";
                    else if (item.equals("Recent Files")) display = "Recent Files  \u00A0 Ctrl+E";
                    else if (item.equals("Quick Documentation")) display = "Quick Documentation  \u00A0 Ctrl+Q";
                    else if (item.equals("Parameter Info")) display = "Parameter Info  \u00A0 Ctrl+P";
                    
                    setText(display);
                    setStyle("-fx-text-fill: #D8DBE6;");
                }
                setGraphic(null);
            }
        });

        return treeView;
    }

    private TreeItem<String> createCategory(String name) {
        return new TreeItem<>(name);
    }
}