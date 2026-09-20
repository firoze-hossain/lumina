package dev.lumina.ui;

import dev.lumina.project.MavenProjectModel;
import dev.lumina.project.MavenProjectModel.DependencyItem;
import dev.lumina.project.MavenProjectModel.MavenProject;
import dev.lumina.project.MavenProjectModel.PluginItem;
import dev.lumina.project.MavenProjectModel.RepositoryItem;
import dev.lumina.run.RunConfiguration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dynamic IntelliJ IDEA-style Maven tool window.
 * Matches IntelliJ across toolbar, tree view, lifecycle phases, plugins, dependencies, and repositories.
 */
public class MavenPanel extends BorderPane {

    public enum NodeType {
        ROOT,
        LIFECYCLE_FOLDER,
        LIFECYCLE_GOAL,
        PLUGINS_FOLDER,
        PLUGIN_ITEM,
        PLUGIN_GOAL,
        DEPENDENCIES_FOLDER,
        DEPENDENCY_ITEM,
        TRANSITIVE_DEPENDENCY,
        REPOSITORIES_FOLDER,
        REPOSITORY_LOCAL,
        REPOSITORY_REMOTE,
        GRADLE_TASK
    }

    public record MavenNodeData(
            NodeType type,
            String title,
            String subtitle,
            String goalToRun,
            Object payload
    ) {
        @Override
        public String toString() {
            return title;
        }
    }

    private final Consumer<String> onRunGoal;
    private final Consumer<Path> onOpenFile;

    private final TreeView<MavenNodeData> tree = new TreeView<>();
    private final MenuButton downloadBtn = new MenuButton();
    private final Button addProjectBtn = new Button("+");
    private final Button removeProjectBtn = new Button("−");
    private final Button runGoalBtn = new Button("▶");
    private final Button executeGoalBtn = new Button();
    private final ToggleButton skipTestsBtn = new ToggleButton("⊘");
    private final ToggleButton offlineBtn = new ToggleButton("⚡");
    private final Button expandAllBtn = new Button();
    private final Button collapseAllBtn = new Button();
    private final Button settingsBtn = new Button();

    private Path currentProjectRoot;
    private MavenProject currentMavenProject;

    public MavenPanel(Consumer<String> onRunGoal) {
        this(onRunGoal, null);
    }

    public MavenPanel(Consumer<String> onRunGoal, Consumer<Path> onOpenFile) {
        this.onRunGoal = onRunGoal;
        this.onOpenFile = onOpenFile;

        getStyleClass().add("maven-panel");

        // Top Toolbar
        ToolBar toolbar = buildToolbar();
        setTop(toolbar);

        // Center TreeView
        tree.setShowRoot(false);
        tree.getStyleClass().add("maven-tree");
        tree.setCellFactory(tv -> new MavenTreeCell());
        tree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<MavenNodeData> selected = tree.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() != null) {
                    handleItemActivation(selected.getValue());
                }
            }
        });

        // Dynamic reactive update of ▶ button on tree selection (Image 3)
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            updateRunGoalButtonState(selected);
        });

        // Keyboard shortcuts: Ctrl+NumPad + for Expand All, Ctrl+NumPad - for Collapse All
        addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() || e.isShortcutDown()) {
                if (e.getCode() == javafx.scene.input.KeyCode.ADD
                        || e.getCode() == javafx.scene.input.KeyCode.PLUS
                        || e.getCode() == javafx.scene.input.KeyCode.EQUALS) {
                    onExpandAll();
                    e.consume();
                } else if (e.getCode() == javafx.scene.input.KeyCode.SUBTRACT
                        || e.getCode() == javafx.scene.input.KeyCode.MINUS) {
                    onCollapseAll();
                    e.consume();
                }
            }
        });

        // Context Menu
        tree.setContextMenu(buildTreeContextMenu());

        setCenter(tree);
        setMinWidth(220);

        // Initial states
        updateRunGoalButtonState(null);
        removeProjectBtn.setDisable(true);
    }

    private void updateRunGoalButtonState(TreeItem<MavenNodeData> selected) {
        if (selected != null && selected.getValue() != null && selected.getValue().goalToRun() != null) {
            String goal = selected.getValue().goalToRun();
            runGoalBtn.setDisable(false);
            runGoalBtn.setStyle("-fx-text-fill: #59A869; -fx-font-size: 11px; -fx-font-weight: bold;");
            runGoalBtn.setTooltip(new Tooltip("Run '" + goal + "'"));
        } else {
            runGoalBtn.setDisable(true);
            runGoalBtn.setStyle("-fx-text-fill: #5A5D6B; -fx-font-size: 11px;");
            runGoalBtn.setTooltip(new Tooltip("Execute Maven Goal (select a goal in tree)"));
        }
    }

    private ToolBar buildToolbar() {
        ToolBar tb = new ToolBar();
        tb.getStyleClass().add("maven-toolbar");

        // 1. Sync / Reload button with popup menu (Image 4)
        MenuButton syncBtn = new MenuButton();
        syncBtn.setGraphic(createReloadIcon("#B9BECF"));
        syncBtn.getStyleClass().addAll("maven-tool-btn", "maven-sync-btn");
        syncBtn.setTooltip(new Tooltip("Sync / Reload All Maven Projects"));

        MenuItem syncAll = new MenuItem("Sync All Maven Projects");
        syncAll.setGraphic(createReloadIcon("#58A6FF"));
        syncAll.setOnAction(e -> reloadProject(false));

        MenuItem reloadAll = new MenuItem("Reload All Maven Projects");
        reloadAll.setGraphic(createReloadIcon("#E5534B"));
        reloadAll.setOnAction(e -> reloadProject(true));

        syncBtn.getItems().addAll(syncAll, reloadAll);

        // 2. Generate Sources and Update Folders For All Projects (Image 5)
        Button genSourcesBtn = new Button();
        genSourcesBtn.setGraphic(createGenSourcesIcon());
        genSourcesBtn.getStyleClass().add("maven-tool-btn");
        genSourcesBtn.setTooltip(new Tooltip("Generate Sources and Update Folders For All Projects"));
        genSourcesBtn.setOnAction(e -> executeGoal("generate-sources"));

        // 3. Download Sources and/or Documentation (Image 1)
        downloadBtn.setGraphic(createDownloadIcon("#B9BECF"));
        downloadBtn.getStyleClass().addAll("maven-tool-btn", "maven-menu-tool-btn");
        downloadBtn.setTooltip(new Tooltip("Download Sources and/or Documentation"));

        MenuItem dlSources = new MenuItem("Download Sources");
        dlSources.setGraphic(createDownloadIcon("#58A6FF"));
        dlSources.setOnAction(e -> executeGoal("dependency:sources"));

        MenuItem dlDocs = new MenuItem("Download Documentation");
        dlDocs.setGraphic(createDownloadIcon("#58A6FF"));
        dlDocs.setOnAction(e -> executeGoal("dependency:resolve -Dclassifier=javadoc"));

        MenuItem dlBoth = new MenuItem("Download Sources and Documentation");
        dlBoth.setGraphic(createDownloadIcon("#58A6FF"));
        dlBoth.setOnAction(e -> executeGoal("dependency:sources dependency:resolve -Dclassifier=javadoc"));

        downloadBtn.getItems().setAll(dlSources, dlDocs, dlBoth);

        // 4. Add Maven Projects (+) (Image 2)
        addProjectBtn.getStyleClass().addAll("maven-tool-btn", "maven-symbol-btn");
        addProjectBtn.setTooltip(new Tooltip("Add Maven Projects"));
        addProjectBtn.setOnAction(e -> onAddProject());

        // 5. Unlink Maven Project (−) (Image 2)
        removeProjectBtn.getStyleClass().addAll("maven-tool-btn", "maven-symbol-btn");
        removeProjectBtn.setTooltip(new Tooltip("Unlink Maven Project"));
        removeProjectBtn.setOnAction(e -> onRemoveProject());

        // 6. Run Selected Goal (▶) (Image 3)
        runGoalBtn.getStyleClass().addAll("maven-tool-btn", "maven-symbol-btn");
        runGoalBtn.setOnAction(e -> {
            TreeItem<MavenNodeData> selected = tree.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue() != null && selected.getValue().goalToRun() != null) {
                executeGoal(selected.getValue().goalToRun());
            }
        });

        // 7. Execute Maven Goal (⧉▶) (Image 4)
        executeGoalBtn.setGraphic(createExecuteGoalIcon());
        executeGoalBtn.getStyleClass().addAll("maven-tool-btn", "maven-symbol-btn");
        executeGoalBtn.setTooltip(new Tooltip("Execute Maven Goal"));
        executeGoalBtn.setOnAction(e -> onExecuteMavenGoal());

        // 8. Toggle 'Skip Tests' Mode (⊘)
        skipTestsBtn.getStyleClass().addAll("maven-tool-btn", "maven-toggle-btn");
        skipTestsBtn.setTooltip(new Tooltip("Toggle 'Skip Tests' Mode"));

        // 9. Toggle Offline Mode (⚡)
        offlineBtn.getStyleClass().addAll("maven-tool-btn", "maven-toggle-btn");
        offlineBtn.setTooltip(new Tooltip("Toggle Offline Mode"));

        // 10. Expand All (Image 1)
        expandAllBtn.setGraphic(createExpandAllIcon());
        expandAllBtn.getStyleClass().addAll("maven-tool-btn", "maven-symbol-btn");
        expandAllBtn.setTooltip(new Tooltip("Expand All  Ctrl+NumPad +"));
        expandAllBtn.setOnAction(e -> onExpandAll());

        // 11. Collapse All (Image 2)
        collapseAllBtn.setGraphic(createCollapseAllIcon());
        collapseAllBtn.getStyleClass().addAll("maven-tool-btn", "maven-symbol-btn");
        collapseAllBtn.setTooltip(new Tooltip("Collapse All  Ctrl+NumPad -"));
        collapseAllBtn.setOnAction(e -> onCollapseAll());

        // 12. Settings (⚙) (Image 3)
        settingsBtn.setGraphic(createGearIcon());
        settingsBtn.getStyleClass().addAll("maven-tool-btn", "maven-symbol-btn");
        settingsBtn.setTooltip(new Tooltip("Maven Settings"));

        ContextMenu settingsMenu = new ContextMenu();
        MenuItem autoSyncItem = new MenuItem("Auto-Sync Settings…");
        autoSyncItem.setOnAction(e -> onShowAutoSyncSettings());

        MenuItem mavenSettingsItem = new MenuItem("Maven Settings");
        mavenSettingsItem.setOnAction(e -> onShowMavenSettings());

        settingsMenu.getItems().addAll(autoSyncItem, mavenSettingsItem);

        settingsBtn.setOnAction(e -> {
            if (settingsMenu.isShowing()) {
                settingsMenu.hide();
            } else {
                settingsMenu.show(settingsBtn, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });

        tb.getItems().addAll(
                syncBtn, genSourcesBtn, downloadBtn, addProjectBtn, removeProjectBtn,
                runGoalBtn, executeGoalBtn, skipTestsBtn, offlineBtn, expandAllBtn, collapseAllBtn, settingsBtn
        );

        return tb;
    }

    private ContextMenu buildTreeContextMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setOnShowing(e -> {
            menu.getItems().clear();
            TreeItem<MavenNodeData> selected = tree.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue() == null) {
                MenuItem reload = new MenuItem("Reload All Maven Projects");
                reload.setOnAction(ev -> reloadProject(true));
                menu.getItems().add(reload);
                return;
            }

            MavenNodeData data = selected.getValue();
            if (data.goalToRun() != null) {
                String goal = data.goalToRun();
                MenuItem runItem = new MenuItem("Run '" + goal + "'");
                runItem.setGraphic(new Label("▶"));
                runItem.setOnAction(ev -> executeGoal(goal));

                MenuItem debugItem = new MenuItem("Debug '" + goal + "'");
                debugItem.setOnAction(ev -> executeGoal(goal + " -Dmaven.surefire.debug"));

                MenuItem editItem = new MenuItem("Edit Run Configuration…");
                editItem.setOnAction(ev -> onPromptAndRunGoal());

                menu.getItems().addAll(runItem, debugItem, new SeparatorMenuItem(), editItem);
            } else if (data.type() == NodeType.ROOT) {
                MenuItem reload = new MenuItem("Reload project");
                reload.setOnAction(ev -> reloadProject(true));

                MenuItem gen = new MenuItem("Generate Sources and Update Folders");
                gen.setOnAction(ev -> executeGoal("generate-sources"));

                MenuItem dlSources = new MenuItem("Download Sources");
                dlSources.setOnAction(ev -> executeGoal("dependency:sources"));

                MenuItem dlDocs = new MenuItem("Download Documentation");
                dlDocs.setOnAction(ev -> executeGoal("dependency:resolve -Dclassifier=javadoc"));

                MenuItem jumpPom = new MenuItem("Jump to Source (pom.xml)");
                jumpPom.setOnAction(ev -> openPomInEditor());

                menu.getItems().addAll(reload, gen, new SeparatorMenuItem(), dlSources, dlDocs, new SeparatorMenuItem(), jumpPom);
            } else if (data.type() == NodeType.DEPENDENCY_ITEM || data.type() == NodeType.TRANSITIVE_DEPENDENCY) {
                MenuItem jumpPom = new MenuItem("Jump to Source (pom.xml)");
                jumpPom.setOnAction(ev -> openPomInEditor());
                menu.getItems().add(jumpPom);
            }
        });
        return menu;
    }

    private void handleItemActivation(MavenNodeData data) {
        if (data == null) return;
        if (data.goalToRun() != null) {
            executeGoal(data.goalToRun());
        } else if (data.type() == NodeType.ROOT || data.type() == NodeType.DEPENDENCY_ITEM) {
            openPomInEditor();
        }
    }

    public void executeGoal(String goal) {
        if (goal == null || goal.isBlank()) return;
        String cmd = goal.trim();
        if (skipTestsBtn.isSelected() && !cmd.contains("skipTests")) {
            cmd += " -DskipTests";
        }
        if (offlineBtn.isSelected() && !cmd.contains("-o")) {
            cmd += " -o";
        }
        if (onRunGoal != null) {
            onRunGoal.accept(cmd);
        }
    }

    private void onPromptAndRunGoal() {
        onExecuteMavenGoal();
    }

    public void onExecuteMavenGoal() {
        javafx.stage.Window w = getScene() != null ? getScene().getWindow() : null;
        Stage owner = w instanceof Stage s ? s : null;
        RunAnythingDialog dialog = new RunAnythingDialog(owner, currentMavenProject, this::executeGoal);
        dialog.show();
    }

    private void onAddProject() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Maven pom.xml");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Maven POM", "pom.xml"));
        if (currentProjectRoot != null && Files.isDirectory(currentProjectRoot)) {
            fc.setInitialDirectory(currentProjectRoot.toFile());
        }
        File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (f != null && f.isFile()) {
            setProject(f.getParentFile().toPath());
        }
    }

    private void onRemoveProject() {
        TreeItem<MavenNodeData> selected = tree.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null && selected.getValue().type() == NodeType.ROOT && tree.getRoot() != null) {
            tree.getRoot().getChildren().remove(selected);
            if (tree.getRoot().getChildren().isEmpty()) {
                setProject(null);
            }
        } else {
            setProject(null);
        }
    }

    private void openPomInEditor() {
        if (currentMavenProject != null && currentMavenProject.getPomPath() != null) {
            if (onOpenFile != null) {
                onOpenFile.accept(currentMavenProject.getPomPath());
            }
        }
    }

    public void reloadProject(boolean force) {
        if (currentProjectRoot != null) {
            setProject(currentProjectRoot);
        }
    }

    public void onExpandAll() {
        if (tree.getRoot() == null) return;
        setTreeExpanded(tree.getRoot(), true);
        if (!tree.getRoot().getChildren().isEmpty()) {
            tree.getSelectionModel().select(tree.getRoot().getChildren().get(0));
        }
    }

    public void onCollapseAll() {
        if (tree.getRoot() == null) return;
        for (TreeItem<MavenNodeData> child : tree.getRoot().getChildren()) {
            setTreeExpanded(child, false);
        }
        if (!tree.getRoot().getChildren().isEmpty()) {
            TreeItem<MavenNodeData> firstProj = tree.getRoot().getChildren().get(0);
            firstProj.setExpanded(false);
            tree.getSelectionModel().select(firstProj);
        }
    }

    public void onShowAutoSyncSettings() {
        javafx.stage.Window w = getScene() != null ? getScene().getWindow() : null;
        Stage owner = w instanceof Stage s ? s : null;
        new AutoSyncSettingsDialog(owner).show();
    }

    public void onShowMavenSettings() {
        javafx.stage.Window w = getScene() != null ? getScene().getWindow() : null;
        Stage owner = w instanceof Stage s ? s : null;
        new MavenSettingsDialog(
                owner, currentMavenProject, this.onOpenFile,
                skipTestsBtn.isSelected(), offlineBtn.isSelected(),
                skipTestsBtn::setSelected, offlineBtn::setSelected
        ).show();
    }

    public Button getRunGoalBtn() { return runGoalBtn; }
    public Button getRemoveProjectBtn() { return removeProjectBtn; }
    public MenuButton getDownloadBtn() { return downloadBtn; }
    public Button getExecuteGoalBtn() { return executeGoalBtn; }
    public Button getExpandAllBtn() { return expandAllBtn; }
    public Button getCollapseAllBtn() { return collapseAllBtn; }
    public Button getSettingsBtn() { return settingsBtn; }
    public TreeView<MavenNodeData> getTree() { return tree; }

    /**
     * Dynamically refreshes contents for the given project root.
     */
    public void setProject(Path root) {
        this.currentProjectRoot = root;
        TreeItem<MavenNodeData> rootItem = new TreeItem<>(new MavenNodeData(NodeType.ROOT, "root", null, null, null));
        rootItem.setExpanded(true);

        if (root == null) {
            this.currentMavenProject = null;
            tree.setRoot(rootItem);
            updateRunGoalButtonState(null);
            removeProjectBtn.setDisable(true);
            return;
        }

        if (RunConfiguration.isMavenProject(root)) {
            MavenProject proj = MavenProjectModel.parseProject(root);
            this.currentMavenProject = proj;
            if (proj != null) {
                TreeItem<MavenNodeData> projectItem = buildProjectTreeItem(proj);
                rootItem.getChildren().add(projectItem);

                // Multi-module subprojects
                for (MavenProject sub : proj.getModules()) {
                    rootItem.getChildren().add(buildProjectTreeItem(sub));
                }
            }
        } else if (RunConfiguration.isGradleProject(root)) {
            // Support for Gradle tasks if opened in Gradle project
            this.currentMavenProject = null;
            TreeItem<MavenNodeData> gradleItem = new TreeItem<>(new MavenNodeData(
                    NodeType.ROOT, root.getFileName().toString(), null, null, null
            ));
            gradleItem.setExpanded(true);

            TreeItem<MavenNodeData> tasksFolder = new TreeItem<>(new MavenNodeData(
                    NodeType.LIFECYCLE_FOLDER, "Tasks", null, null, null
            ));
            tasksFolder.setExpanded(true);
            for (String t : List.of("clean", "classes", "test", "build", "run", "bootRun", "dependencies")) {
                tasksFolder.getChildren().add(new TreeItem<>(new MavenNodeData(
                        NodeType.GRADLE_TASK, t, null, t, null
                )));
            }
            gradleItem.getChildren().add(tasksFolder);
            rootItem.getChildren().add(gradleItem);
        }

        tree.setRoot(rootItem);
        updateRunGoalButtonState(tree.getSelectionModel().getSelectedItem());
        removeProjectBtn.setDisable(this.currentMavenProject == null);
    }

    private TreeItem<MavenNodeData> buildProjectTreeItem(MavenProject proj) {
        TreeItem<MavenNodeData> projectItem = new TreeItem<>(new MavenNodeData(
                NodeType.ROOT, proj.getName(), proj.getPackaging(), null, proj
        ));
        projectItem.setExpanded(true);

        // 1. Lifecycle
        TreeItem<MavenNodeData> lifecycleFolder = new TreeItem<>(new MavenNodeData(
                NodeType.LIFECYCLE_FOLDER, "Lifecycle", null, null, null
        ));
        lifecycleFolder.setExpanded(false);
        for (String phase : proj.getLifecyclePhases()) {
            lifecycleFolder.getChildren().add(new TreeItem<>(new MavenNodeData(
                    NodeType.LIFECYCLE_GOAL, phase, null, phase, null
            )));
        }
        projectItem.getChildren().add(lifecycleFolder);

        // 2. Plugins (Image 2)
        TreeItem<MavenNodeData> pluginsFolder = new TreeItem<>(new MavenNodeData(
                NodeType.PLUGINS_FOLDER, "Plugins", null, null, null
        ));
        pluginsFolder.setExpanded(false);
        for (PluginItem plugin : proj.getPlugins()) {
            TreeItem<MavenNodeData> pluginItem = new TreeItem<>(new MavenNodeData(
                    NodeType.PLUGIN_ITEM, plugin.displayString(), null, null, plugin
            ));
            for (String goal : plugin.goals()) {
                pluginItem.getChildren().add(new TreeItem<>(new MavenNodeData(
                        NodeType.PLUGIN_GOAL, goal, null, goal, null
                )));
            }
            pluginsFolder.getChildren().add(pluginItem);
        }
        projectItem.getChildren().add(pluginsFolder);

        // 3. Dependencies (Image 3)
        TreeItem<MavenNodeData> depsFolder = new TreeItem<>(new MavenNodeData(
                NodeType.DEPENDENCIES_FOLDER, "Dependencies", null, null, null
        ));
        depsFolder.setExpanded(false);
        for (DependencyItem dep : proj.getDependencies()) {
            TreeItem<MavenNodeData> depItem = new TreeItem<>(new MavenNodeData(
                    NodeType.DEPENDENCY_ITEM, dep.displayString(), dep.scope(), null, dep
            ));
            for (DependencyItem trans : dep.transitiveDependencies()) {
                depItem.getChildren().add(new TreeItem<>(new MavenNodeData(
                        NodeType.TRANSITIVE_DEPENDENCY, trans.displayString(), trans.scope(), null, trans
                )));
            }
            depsFolder.getChildren().add(depItem);
        }
        projectItem.getChildren().add(depsFolder);

        // 4. Repositories (Image 3)
        TreeItem<MavenNodeData> reposFolder = new TreeItem<>(new MavenNodeData(
                NodeType.REPOSITORIES_FOLDER, "Repositories", null, null, null
        ));
        reposFolder.setExpanded(false);
        for (RepositoryItem repo : proj.getRepositories()) {
            NodeType rt = repo.isLocal() ? NodeType.REPOSITORY_LOCAL : NodeType.REPOSITORY_REMOTE;
            reposFolder.getChildren().add(new TreeItem<>(new MavenNodeData(
                    rt, repo.displayString(), null, null, repo
            )));
        }
        projectItem.getChildren().add(reposFolder);

        return projectItem;
    }

    private void setTreeExpanded(TreeItem<?> item, boolean expanded) {
        if (item == null) return;
        item.setExpanded(expanded);
        for (TreeItem<?> child : item.getChildren()) {
            setTreeExpanded(child, expanded);
        }
    }

    // ------------------------------------------------------------- TreeCell Rendering

    private final class MavenTreeCell extends TreeCell<MavenNodeData> {
        @Override
        protected void updateItem(MavenNodeData item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                setText(item.title());
                setGraphic(createNodeIcon(item.type()));
                if (item.type() == NodeType.ROOT) {
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #DFE1E5;");
                } else if (item.type() == NodeType.LIFECYCLE_FOLDER
                        || item.type() == NodeType.PLUGINS_FOLDER
                        || item.type() == NodeType.DEPENDENCIES_FOLDER
                        || item.type() == NodeType.REPOSITORIES_FOLDER) {
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #BCBEC4;");
                } else {
                    setStyle("-fx-font-weight: normal; -fx-text-fill: #BCBEC4;");
                }
            }
        }
    }

    private Node createNodeIcon(NodeType type) {
        return switch (type) {
            case ROOT, REPOSITORY_LOCAL -> createMavenGlyphIcon();
            case LIFECYCLE_FOLDER -> createFolderIcon("#BCBEC4");
            case LIFECYCLE_GOAL -> createGearIcon();
            case PLUGINS_FOLDER -> createFolderIcon("#BCBEC4");
            case PLUGIN_ITEM -> createPluginIcon();
            case PLUGIN_GOAL -> createMavenGlyphIcon();
            case DEPENDENCIES_FOLDER -> createFolderIcon("#BCBEC4");
            case DEPENDENCY_ITEM -> createJarIcon("#BCBEC4");
            case TRANSITIVE_DEPENDENCY -> createJarIcon("#8B92A6");
            case REPOSITORIES_FOLDER -> createFolderIcon("#BCBEC4");
            case REPOSITORY_REMOTE -> createGlobeIcon();
            case GRADLE_TASK -> createGearIcon();
        };
    }

    // ------------------------------------------------------------- Vector Icons

    public static Node createMavenGlyphIcon() {
        Label m = new Label("m");
        m.setStyle("-fx-font-family: 'Georgia', 'DejaVu Serif', serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #3574F0;");
        StackPane sp = new StackPane(m);
        sp.setAlignment(Pos.CENTER);
        sp.setPrefSize(16, 16);
        return sp;
    }

    public static Node createMavenBadgeIcon() {
        Rectangle bg = new Rectangle(14, 14);
        bg.setArcWidth(4);
        bg.setArcHeight(4);
        bg.setFill(Color.web("#3574F0"));

        Label m = new Label("m");
        m.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-font-style: italic;");
        StackPane pane = new StackPane(bg, m);
        pane.setAlignment(Pos.CENTER);
        pane.setPrefSize(16, 16);
        return pane;
    }

    private static Node createFolderIcon(String colorHex) {
        Polygon folder = new Polygon(
                0, 2,   4, 2,   5.5, 0,   13, 0,   13, 2,
                13, 10, 0, 10);
        folder.setFill(Color.web(colorHex));
        StackPane pane = new StackPane(folder);
        pane.setAlignment(Pos.CENTER);
        pane.setPrefSize(16, 16);
        return pane;
    }

    private static Node createGearIcon() {
        Label gear = new Label("⚙");
        gear.setStyle("-fx-text-fill: #8B92A6; -fx-font-size: 11px;");
        StackPane pane = new StackPane(gear);
        pane.setAlignment(Pos.CENTER);
        pane.setPrefSize(16, 16);
        return pane;
    }

    private static Node createPluginIcon() {
        Rectangle body = new Rectangle(8, 7);
        body.setArcWidth(2);
        body.setArcHeight(2);
        body.setFill(Color.web("#8B92A6"));

        Line p1 = new Line(2, 0, 2, 2.5);
        p1.setStroke(Color.web("#8B92A6"));
        p1.setStrokeWidth(1.2);

        Line p2 = new Line(6, 0, 6, 2.5);
        p2.setStroke(Color.web("#8B92A6"));
        p2.setStrokeWidth(1.2);

        Line cord = new Line(4, 9.5, 4, 12);
        cord.setStroke(Color.web("#8B92A6"));
        cord.setStrokeWidth(1.2);

        Pane pane = new Pane(body, p1, p2, cord);
        body.setLayoutX(3);
        body.setLayoutY(3);
        p1.setLayoutX(3);
        p2.setLayoutX(3);
        cord.setLayoutX(3);
        pane.setPrefSize(16, 16);
        return pane;
    }

    private static Node createJarIcon(String colorHex) {
        Rectangle box = new Rectangle(11, 10);
        box.setArcWidth(2.5);
        box.setArcHeight(2.5);
        box.setFill(Color.web(colorHex));

        Line line = new Line(2, 3.5, 9, 3.5);
        line.setStroke(Color.web("#14161E"));
        line.setStrokeWidth(1.1);

        StackPane pane = new StackPane(box, line);
        pane.setAlignment(Pos.CENTER);
        pane.setPrefSize(16, 16);
        return pane;
    }

    private static Node createGlobeIcon() {
        Circle c = new Circle(5.5);
        c.setFill(Color.TRANSPARENT);
        c.setStroke(Color.web("#58A6FF"));
        c.setStrokeWidth(1.2);

        Line equator = new Line(-4.5, 0, 4.5, 0);
        equator.setStroke(Color.web("#58A6FF"));
        equator.setStrokeWidth(1);

        Ellipse meridian = new Ellipse(2.5, 5);
        meridian.setFill(Color.TRANSPARENT);
        meridian.setStroke(Color.web("#58A6FF"));
        meridian.setStrokeWidth(1);

        StackPane pane = new StackPane(c, meridian, equator);
        pane.setAlignment(Pos.CENTER);
        pane.setPrefSize(16, 16);
        return pane;
    }

    private static Node createTerminalIcon() {
        Label t = new Label(">_");
        t.setStyle("-fx-text-fill: #58A6FF; -fx-font-size: 8px; -fx-font-weight: bold;");
        StackPane pane = new StackPane(t);
        pane.setAlignment(Pos.CENTER);
        pane.setPrefSize(16, 16);
        return pane;
    }

    private static Node createReloadIcon(String colorHex) {
        Label l = new Label("⟳");
        l.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        return l;
    }

    private static Node createGenSourcesIcon() {
        Polygon folder = new Polygon(0, 1.5, 3, 1.5, 4.5, 0, 11, 0, 11, 1.5, 11, 8, 0, 8);
        folder.setFill(Color.web("#8B92A6"));

        Label arrow = new Label("↓");
        arrow.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 10px; -fx-font-weight: bold;");
        arrow.setTranslateX(4);
        arrow.setTranslateY(2);

        StackPane pane = new StackPane(folder, arrow);
        pane.setPrefSize(16, 16);
        pane.setAlignment(Pos.CENTER);
        return pane;
    }

    private static Node createDownloadIcon() {
        return createDownloadIcon("#B9BECF");
    }

    private static Node createDownloadIcon(String colorHex) {
        Rectangle tray = new Rectangle(10, 4);
        tray.setFill(Color.TRANSPARENT);
        tray.setStroke(Color.web(colorHex));
        tray.setStrokeWidth(1.2);
        tray.setTranslateY(4);

        Label arrow = new Label("↓");
        arrow.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        arrow.setTranslateY(-1);

        StackPane pane = new StackPane(tray, arrow);
        pane.setPrefSize(16, 16);
        pane.setAlignment(Pos.CENTER);
        return pane;
    }

    private static Node createExecuteGoalIcon() {
        Rectangle frame = new Rectangle(12, 11);
        frame.setArcWidth(3);
        frame.setArcHeight(3);
        frame.setFill(Color.TRANSPARENT);
        frame.setStroke(Color.web("#8B92A6"));
        frame.setStrokeWidth(1.2);

        Polygon play = new Polygon(
                0.0, 0.0,
                5.0, 3.0,
                0.0, 6.0
        );
        play.setFill(Color.web("#59A869"));

        StackPane pane = new StackPane(frame, play);
        pane.setPrefSize(16, 16);
        pane.setAlignment(Pos.CENTER);
        return pane;
    }

    private static Node createExpandAllIcon() {
        Polyline up = new Polyline(2.0, 5.0, 6.0, 1.5, 10.0, 5.0);
        up.setStroke(Color.web("#B9BECF"));
        up.setStrokeWidth(1.3);
        up.setFill(Color.TRANSPARENT);

        Polyline down = new Polyline(2.0, 7.5, 6.0, 11.0, 10.0, 7.5);
        down.setStroke(Color.web("#B9BECF"));
        down.setStrokeWidth(1.3);
        down.setFill(Color.TRANSPARENT);

        StackPane pane = new StackPane(up, down);
        pane.setPrefSize(16, 16);
        pane.setAlignment(Pos.CENTER);
        return pane;
    }

    private static Node createCollapseAllIcon() {
        Polyline down = new Polyline(2.0, 2.5, 6.0, 6.0, 10.0, 2.5);
        down.setStroke(Color.web("#B9BECF"));
        down.setStrokeWidth(1.3);
        down.setFill(Color.TRANSPARENT);

        Polyline up = new Polyline(2.0, 10.0, 6.0, 6.5, 10.0, 10.0);
        up.setStroke(Color.web("#B9BECF"));
        up.setStrokeWidth(1.3);
        up.setFill(Color.TRANSPARENT);

        StackPane pane = new StackPane(down, up);
        pane.setPrefSize(16, 16);
        pane.setAlignment(Pos.CENTER);
        return pane;
    }
}
