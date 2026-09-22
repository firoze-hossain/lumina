package dev.lumina.ui;

import dev.lumina.quicklist.QuickListItem;
import java.util.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Modal dialog for selecting an action to add into a Quick List.
 * Strictly matches media_1790045975645.png.
 */
public class AddActionsToQuickListDialog {

    private final Stage stage;
    private final TreeView<ActionNode> treeView = new TreeView<>();
    private final TextField searchField = new TextField();
    private TreeItem<ActionNode> rootItem;
    private QuickListItem selectedResult = null;
    private boolean allExpanded = false;

    public static class ActionNode {
        public final String id;
        public final String text;
        public final String iconGlyph;
        public final boolean isGroup;

        public ActionNode(String id, String text, String iconGlyph, boolean isGroup) {
            this.id = id != null ? id : "";
            this.text = text != null ? text : "";
            this.iconGlyph = iconGlyph != null ? iconGlyph : "";
            this.isGroup = isGroup;
        }

        public static ActionNode group(String text) {
            return new ActionNode("", text, "📁", true);
        }

        public static ActionNode action(String id, String text, String iconGlyph) {
            return new ActionNode(id, text, iconGlyph, false);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public AddActionsToQuickListDialog(Window owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Add Actions to Quick List");
        stage.setResizable(true);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
        root.setPadding(new Insets(14, 16, 14, 16));

        // 1. Top Search & Controls Bar
        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        Button expandToggleBtn = new Button("⇅");
        expandToggleBtn.setTooltip(new Tooltip("Expand / Collapse All"));
        expandToggleBtn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        expandToggleBtn.setOnAction(e -> {
            allExpanded = !allExpanded;
            toggleExpandAll(rootItem, allExpanded);
        });

        HBox searchContainer = new HBox(6);
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 2 8 2 8;");
        HBox.setHgrow(searchContainer, Priority.ALWAYS);

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px;");

        searchField.setPromptText("");
        searchField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 0 4 0;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Label filterBadge = new Label("⚙");
        filterBadge.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-cursor: hand;");

        searchContainer.getChildren().addAll(searchIcon, searchField, filterBadge);
        topBar.getChildren().addAll(expandToggleBtn, searchContainer);
        root.setTop(topBar);

        // 2. Build Tree
        buildTree();
        treeView.setShowRoot(false);
        treeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(ActionNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    String prefix = item.iconGlyph.isEmpty() ? "   " : item.iconGlyph + "  ";
                    setText(prefix + item.text);
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    String textFill = isSelected() ? "#FFFFFF" : (item.isGroup ? "#DFE1E5" : "#BCBEC4");
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px; -fx-padding: 3 6 3 6;");
                }
            }
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> filterTree(newV));

        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                confirmSelection();
            }
        });

        root.setCenter(treeView);

        // 3. Bottom Button Bar
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(12, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        cancelBtn.setOnAction(e -> stage.close());

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        okBtn.setOnAction(e -> confirmSelection());

        buttonBar.getChildren().addAll(cancelBtn, okBtn);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 420, 500);
        stage.setScene(scene);
    }

    private void confirmSelection() {
        TreeItem<ActionNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null && !selected.getValue().isGroup) {
            ActionNode node = selected.getValue();
            selectedResult = QuickListItem.action(node.id, node.text, node.iconGlyph);
            stage.close();
        }
    }

    private void buildTree() {
        rootItem = new TreeItem<>(ActionNode.group("Root"));

        // 1. Editor Actions
        TreeItem<ActionNode> editorGrp = new TreeItem<>(ActionNode.group("Editor Actions"));
        editorGrp.getChildren().addAll(
                item("Editor.Cut", "Cut", "✂"),
                item("Editor.Copy", "Copy", "📋"),
                item("Editor.Paste", "Paste", "📄"),
                item("Editor.SelectAll", "Select All", ""),
                item("Editor.DuplicateLine", "Duplicate Line or Selection", ""),
                item("Editor.DeleteLine", "Delete Line", "🗑"),
                item("Editor.ToggleComment", "Toggle Line Comment", "")
        );

        // 2. Main Menu
        TreeItem<ActionNode> menuGrp = new TreeItem<>(ActionNode.group("Main Menu"));
        TreeItem<ActionNode> toolWindows = new TreeItem<>(ActionNode.group("Tool Windows"));
        toolWindows.getChildren().addAll(
                item("ToolWindows.Project", "Project", "📁"),
                item("ToolWindows.Terminal", "Terminal", "🖥"),
                item("ToolWindows.Git", "Git", "⌥"),
                item("ToolWindows.Run", "Run", "▶")
        );
        TreeItem<ActionNode> extTools = new TreeItem<>(ActionNode.group("External Tools"));
        extTools.getChildren().addAll(
                item("ExtTools.Terminal", "Open in Terminal", "🖥"),
                item("ExtTools.Explorer", "Show in Explorer/Finder", "📁")
        );
        menuGrp.getChildren().addAll(toolWindows, extTools);

        // 3. External Build Systems
        TreeItem<ActionNode> buildGrp = new TreeItem<>(ActionNode.group("External Build Systems"));
        buildGrp.getChildren().addAll(
                item("Build.Maven", "Maven Build", "🔨"),
                item("Build.Gradle", "Gradle Build", "🐘")
        );

        // 4. Version Control Systems
        TreeItem<ActionNode> vcsGrp = new TreeItem<>(ActionNode.group("Version Control Systems"));
        vcsGrp.getChildren().addAll(
                item("Vcs.Commit", "Commit...", "💾"),
                item("Vcs.Push", "Push...", "⬆"),
                item("Vcs.Pull", "Update Project...", "⬇"),
                item("Vcs.History", "Show History", "⏱")
        );

        // 5. Debugger Actions
        TreeItem<ActionNode> debugGrp = new TreeItem<>(new ActionNode("Debug", "Debugger Actions", "⚙", true));
        debugGrp.getChildren().addAll(
                item("Debug.Resume", "Resume Program", "▶"),
                item("Debug.Pause", "Pause Program", "⏸"),
                item("Debug.Stop", "Stop", "⏹"),
                item("Debug.StepOver", "Step Over", "↷"),
                item("Debug.StepInto", "Step Into", "↘"),
                item("Debug.StepOut", "Step Out", "↗")
        );

        // 6. Remote External Tools
        TreeItem<ActionNode> remoteTools = new TreeItem<>(ActionNode.group("Remote External Tools"));
        remoteTools.getChildren().addAll(
                item("PublishGroup.Upload", "Upload to Default Server", "📤"),
                item("PublishGroup.UploadTo", "Upload To...", ""),
                item("PublishGroup.Download", "Download from Default Server", "📥"),
                item("PublishGroup.DownloadFrom", "Download From...", ""),
                item("PublishGroup.DiffWith", "Compare Local File with Deployed Version", "↹"),
                item("PublishGroup.BrowseServers", "Browse Remote Host", "🗄")
        );

        // 7. Database
        TreeItem<ActionNode> dbGrp = new TreeItem<>(ActionNode.group("Database"));
        dbGrp.getChildren().addAll(
                item("Database.Connect", "New Database Connection", "🗄"),
                item("Database.Console", "Open SQL Console", "📄"),
                item("Database.RunQuery", "Execute Query", "▶")
        );

        // 8. Macros
        TreeItem<ActionNode> macrosGrp = new TreeItem<>(ActionNode.group("Macros"));
        macrosGrp.getChildren().addAll(
                item("Macros.Play", "Playback Last Macro", "▶"),
                item("Macros.Record", "Start Macro Recording", "⏺")
        );

        // 9. Intentions
        TreeItem<ActionNode> intentionsGrp = new TreeItem<>(ActionNode.group("Intentions"));
        intentionsGrp.getChildren().addAll(
                item("Intentions.Show", "Show Context Actions", "💡"),
                item("Intentions.Fix", "Apply Quick Fix", "✨")
        );

        // 10. Quick Lists
        TreeItem<ActionNode> quickListsGrp = new TreeItem<>(ActionNode.group("Quick Lists"));
        quickListsGrp.getChildren().addAll(
                item("QuickList.Deployment", "Deployment", "🚀"),
                item("QuickList.VCS", "VCS Operations", "⌥")
        );

        // 11. Plugins
        TreeItem<ActionNode> pluginsGrp = new TreeItem<>(ActionNode.group("Plugins"));
        pluginsGrp.getChildren().addAll(
                item("Plugins.Manage", "Manage Plugins...", "🧩"),
                item("Plugins.CheckUpdates", "Check for Plugin Updates", "🔄")
        );

        // 12. Other
        TreeItem<ActionNode> otherGrp = new TreeItem<>(ActionNode.group("Other"));
        otherGrp.getChildren().addAll(
                item("WebOpenInBrowser", "Open in default browser", "🌐"),
                item("PublishGroup.SelectInServer", "Select in Remote Host", ""),
                item("PublishGroup.Directory", "Directory", "📁"),
                item("PublishGroup.ChangePermissions", "Change Permissions...", "🔑"),
                item("PublishGroup.Configuration", "Configuration...", "")
        );

        rootItem.getChildren().addAll(
                editorGrp, menuGrp, buildGrp, vcsGrp, debugGrp, remoteTools,
                dbGrp, macrosGrp, intentionsGrp, quickListsGrp, pluginsGrp, otherGrp
        );

        // By default, match screenshot: Main Menu & Version Control expanded
        menuGrp.setExpanded(true);
        vcsGrp.setExpanded(true);

        treeView.setRoot(rootItem);
    }

    private TreeItem<ActionNode> item(String id, String text, String icon) {
        return new TreeItem<>(ActionNode.action(id, text, icon));
    }

    private void toggleExpandAll(TreeItem<?> item, boolean expand) {
        if (item == null) return;
        item.setExpanded(expand);
        for (TreeItem<?> child : item.getChildren()) {
            toggleExpandAll(child, expand);
        }
    }

    private void filterTree(String query) {
        if (query == null || query.isBlank()) {
            buildTree();
            return;
        }
        String q = query.trim().toLowerCase();
        buildTree();
        filterRecursive(rootItem, q);
    }

    private boolean filterRecursive(TreeItem<ActionNode> item, String query) {
        if (item == null) return false;
        boolean matchesSelf = item.getValue() != null &&
                (item.getValue().text.toLowerCase().contains(query) ||
                        item.getValue().id.toLowerCase().contains(query));

        List<TreeItem<ActionNode>> childrenToRemove = new ArrayList<>();
        boolean childMatched = false;

        for (TreeItem<ActionNode> child : item.getChildren()) {
            boolean keep = filterRecursive(child, query);
            if (!keep) {
                childrenToRemove.add(child);
            } else {
                childMatched = true;
            }
        }

        item.getChildren().removeAll(childrenToRemove);

        if (childMatched || matchesSelf) {
            item.setExpanded(true);
            return true;
        }
        return false;
    }

    public QuickListItem showAndWait() {
        stage.showAndWait();
        return selectedResult;
    }
}
