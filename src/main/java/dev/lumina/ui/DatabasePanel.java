package dev.lumina.ui;

import dev.lumina.database.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Professional Database Tool Window for Lumina, matching DBNavigator.
 * Supports all 8 database engines, hierarchical schema/table/column exploration,
 * and opens Query Consoles and Table Data viewers directly in the main editor area.
 */
public class DatabasePanel extends BorderPane {

    public enum NodeType {
        ROOT,
        DATA_SOURCE,
        SCHEMA,
        TABLES_GROUP,
        VIEWS_GROUP,
        TABLE,
        VIEW,
        COLUMNS_GROUP,
        COLUMN,
        LOADING,
        EMPTY
    }

    public static class DbNode {
        private final NodeType type;
        private final String name;
        private final String extra;
        private final DbConnectionProfile profile;
        private final String schema;
        private final String tableName;

        public DbNode(NodeType type, String name, String extra, DbConnectionProfile profile, String schema, String tableName) {
            this.type = type;
            this.name = name;
            this.extra = extra;
            this.profile = profile;
            this.schema = schema;
            this.tableName = tableName;
        }

        public NodeType getType() { return type; }
        public String getName() { return name; }
        public String getExtra() { return extra; }
        public DbConnectionProfile getProfile() { return profile; }
        public String getSchema() { return schema; }
        public String getTableName() { return tableName; }

        @Override
        public String toString() {
            if (extra != null && !extra.isBlank()) {
                return name + " " + extra;
            }
            return name;
        }
    }

    private final TreeView<DbNode> treeView = new TreeView<>();
    private final TreeItem<DbNode> rootItem = new TreeItem<>(new DbNode(NodeType.ROOT, "Databases", "", null, null, null));
    private final TextField searchField = new TextField();
    private final Label statusLabel = new Label("Ready");

    private Consumer<DbConnectionProfile> onOpenConsole;
    private BiConsumer<DbConnectionProfile, String[]> onOpenTable; // profile, [schema, tableName]

    public DatabasePanel() {
        this(null, null);
    }

    public DatabasePanel(Consumer<DbConnectionProfile> onOpenConsole, BiConsumer<DbConnectionProfile, String[]> onOpenTable) {
        this.onOpenConsole = onOpenConsole;
        this.onOpenTable = onOpenTable;

        getStyleClass().add("side-panel");

        // Header and Toolbar
        Label header = new Label("Database");
        header.getStyleClass().add("panel-header");

        MenuButton addSourceBtn = new MenuButton("+");
        addSourceBtn.getStyleClass().add("db-add-source");
        addSourceBtn.setTooltip(new Tooltip("Add Data Source"));

        // Populate all 8 database engine options
        for (DbEngineType engine : DbEngineType.values()) {
            MenuItem item = new MenuItem(engine.getDisplayName());
            item.setOnAction(e -> openNewConnectionDialog(engine));
            addSourceBtn.getItems().add(item);
        }

        Button refreshBtn = new Button("\u27F3");
        refreshBtn.getStyleClass().add("db-add-source");
        refreshBtn.setTooltip(new Tooltip("Refresh All Data Sources"));
        refreshBtn.setOnAction(e -> refreshTree());

        Button openConsoleBtn = new Button("\uD83D\uDCBB");
        openConsoleBtn.getStyleClass().add("db-add-source");
        openConsoleBtn.setTooltip(new Tooltip("Open Query Console"));
        openConsoleBtn.setOnAction(e -> openConsoleForSelected());

        Button editSourceBtn = new Button("\u2699");
        editSourceBtn.getStyleClass().add("db-add-source");
        editSourceBtn.setTooltip(new Tooltip("Edit Data Source Properties"));
        editSourceBtn.setOnAction(e -> editSelectedProfile());

        Button deleteSourceBtn = new Button("\u2715");
        deleteSourceBtn.getStyleClass().add("db-add-source");
        deleteSourceBtn.setTooltip(new Tooltip("Remove Data Source"));
        deleteSourceBtn.setOnAction(e -> deleteSelectedProfile());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerRow = new HBox(6, header, spacer, addSourceBtn, refreshBtn, openConsoleBtn, editSourceBtn, deleteSourceBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(8, 10, 6, 10));

        // Filter / Search Bar
        searchField.setPromptText("Filter schemas and tables\u2026");
        searchField.setPadding(new Insets(4, 8, 4, 8));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox searchRow = new HBox(searchField);
        searchRow.setPadding(new Insets(0, 10, 8, 10));

        VBox topBox = new VBox(headerRow, searchRow);
        setTop(topBox);

        // TreeView configuration
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("goal-list");
        treeView.setCellFactory(tv -> new DbTreeCell());

        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<DbNode> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() != null) {
                    DbNode node = selected.getValue();
                    if (node.getType() == NodeType.TABLE || node.getType() == NodeType.VIEW) {
                        if (onOpenTable != null) {
                            onOpenTable.accept(node.getProfile(), new String[]{node.getSchema(), node.getTableName()});
                        }
                    } else if (node.getType() == NodeType.DATA_SOURCE) {
                        if (onOpenConsole != null) {
                            onOpenConsole.accept(node.getProfile());
                        }
                    }
                }
            }
        });

        setCenter(treeView);

        // Status bar
        statusLabel.setStyle("-fx-text-fill: #697089; -fx-font-size: 11px;");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.setStyle("-fx-background-color: #14161E; -fx-border-color: #262A3A transparent transparent transparent;");
        setBottom(statusBar);

        // Listen for profile changes from DbConnectionManager
        DbConnectionManager.getInstance().addListener(this::refreshTree);

        // Initial load
        refreshTree();
    }

    public void setOnOpenConsole(Consumer<DbConnectionProfile> onOpenConsole) {
        this.onOpenConsole = onOpenConsole;
    }

    public void setOnOpenTable(BiConsumer<DbConnectionProfile, String[]> onOpenTable) {
        this.onOpenTable = onOpenTable;
    }

    public void shutdown() {
        DbConnectionManager.getInstance().shutdown();
    }

    private void openNewConnectionDialog(DbEngineType engine) {
        DbConnectionProfile profile = new DbConnectionProfile(engine);
        DbConnectionDialog dialog = new DbConnectionDialog(profile);
        dialog.showAndWait().ifPresent(res -> {
            DbConnectionManager.getInstance().addOrUpdateProfile(res);
            refreshTree();
        });
    }

    private void editSelectedProfile() {
        TreeItem<DbNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;
        DbConnectionProfile p = selected.getValue().getProfile();
        if (p == null) return;

        DbConnectionDialog dialog = new DbConnectionDialog(p);
        dialog.showAndWait().ifPresent(res -> {
            DbConnectionManager.getInstance().addOrUpdateProfile(res);
            refreshTree();
        });
    }

    private void deleteSelectedProfile() {
        TreeItem<DbNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;
        DbConnectionProfile p = selected.getValue().getProfile();
        if (p == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Data Source");
        alert.setHeaderText("Remove " + p.getName() + "?");
        alert.setContentText("Are you sure you want to remove this data source configuration?");
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        } catch (Throwable ignored) {}

        alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(btn -> {
            DbConnectionManager.getInstance().removeProfile(p.getId());
            refreshTree();
        });
    }

    private void openConsoleForSelected() {
        TreeItem<DbNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) {
            List<DbConnectionProfile> profiles = DbConnectionManager.getInstance().getProfiles();
            if (!profiles.isEmpty() && onOpenConsole != null) {
                onOpenConsole.accept(profiles.get(0));
            }
            return;
        }
        DbConnectionProfile p = selected.getValue().getProfile();
        if (p != null && onOpenConsole != null) {
            onOpenConsole.accept(p);
        }
    }

    public void refreshTree() {
        Platform.runLater(() -> {
            rootItem.getChildren().clear();
            List<DbConnectionProfile> profiles = DbConnectionManager.getInstance().getProfiles();
            for (DbConnectionProfile profile : profiles) {
                TreeItem<DbNode> dsItem = createDataSourceTreeItem(profile);
                rootItem.getChildren().add(dsItem);
            }
            statusLabel.setText(profiles.size() + " data source(s) configured");
        });
    }

    private TreeItem<DbNode> createDataSourceTreeItem(DbConnectionProfile profile) {
        boolean connected = DbConnectionManager.getInstance().isConnected(profile.getId());
        String statusDot = connected ? "\u25CF " : "\u25CB ";
        String extra = "(" + profile.getEngine().getDisplayName() + ")";

        TreeItem<DbNode> dsItem = new TreeItem<>(new DbNode(
                NodeType.DATA_SOURCE,
                statusDot + profile.getName(),
                extra,
                profile,
                null,
                null
        ));

        // Lazy load child schemas on expand
        dsItem.getChildren().add(new TreeItem<>(new DbNode(NodeType.LOADING, "Expand to connect\u2026", "", profile, null, null)));

        dsItem.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            if (isExpanded && dsItem.getChildren().size() == 1
                    && dsItem.getChildren().get(0).getValue().getType() == NodeType.LOADING) {
                loadDataSourceSchemas(dsItem, profile);
            }
        });

        return dsItem;
    }

    private void loadDataSourceSchemas(TreeItem<DbNode> dsItem, DbConnectionProfile profile) {
        dsItem.getChildren().setAll(new TreeItem<>(new DbNode(NodeType.LOADING, "Connecting\u2026", "", profile, null, null)));

        Thread t = new Thread(() -> {
            try {
                Connection conn = DbConnectionManager.getInstance().getConnection(profile);
                List<DbMetadataService.SchemaInfo> schemas = DbMetadataService.loadSchemasAndTables(conn, profile.getEngine());

                Platform.runLater(() -> {
                    dsItem.getChildren().clear();
                    // Update connection status dot to connected
                    dsItem.setValue(new DbNode(
                            NodeType.DATA_SOURCE,
                            "\u25CF " + profile.getName(),
                            "(" + profile.getEngine().getDisplayName() + ")",
                            profile,
                            null,
                            null
                    ));

                    for (DbMetadataService.SchemaInfo s : schemas) {
                        TreeItem<DbNode> schemaItem = new TreeItem<>(new DbNode(
                                NodeType.SCHEMA,
                                s.name(),
                                "(" + (s.tables().size() + s.views().size()) + ")",
                                profile,
                                s.name(),
                                null
                        ));

                        // Tables folder
                        if (!s.tables().isEmpty()) {
                            TreeItem<DbNode> tablesGroup = new TreeItem<>(new DbNode(
                                    NodeType.TABLES_GROUP,
                                    "tables",
                                    "(" + s.tables().size() + ")",
                                    profile,
                                    s.name(),
                                    null
                            ));

                            for (DbMetadataService.TableInfo tbl : s.tables()) {
                                TreeItem<DbNode> tableItem = new TreeItem<>(new DbNode(
                                        NodeType.TABLE,
                                        tbl.name(),
                                        "",
                                        profile,
                                        s.name(),
                                        tbl.name()
                                ));

                                // Lazy load columns on table expand
                                tableItem.getChildren().add(new TreeItem<>(new DbNode(NodeType.LOADING, "Loading columns\u2026", "", profile, s.name(), tbl.name())));
                                tableItem.expandedProperty().addListener((o, wasExp, isExp) -> {
                                    if (isExp && tableItem.getChildren().size() == 1
                                            && tableItem.getChildren().get(0).getValue().getType() == NodeType.LOADING) {
                                        loadTableColumns(tableItem, profile, s.name(), tbl.name());
                                    }
                                });

                                tablesGroup.getChildren().add(tableItem);
                            }
                            schemaItem.getChildren().add(tablesGroup);
                        }

                        // Views folder
                        if (!s.views().isEmpty()) {
                            TreeItem<DbNode> viewsGroup = new TreeItem<>(new DbNode(
                                    NodeType.VIEWS_GROUP,
                                    "views",
                                    "(" + s.views().size() + ")",
                                    profile,
                                    s.name(),
                                    null
                            ));
                            for (DbMetadataService.TableInfo v : s.views()) {
                                TreeItem<DbNode> viewItem = new TreeItem<>(new DbNode(
                                        NodeType.VIEW,
                                        v.name(),
                                        "",
                                        profile,
                                        s.name(),
                                        v.name()
                                ));
                                viewsGroup.getChildren().add(viewItem);
                            }
                            schemaItem.getChildren().add(viewsGroup);
                        }

                        dsItem.getChildren().add(schemaItem);
                    }
                    if (dsItem.getChildren().isEmpty()) {
                        dsItem.getChildren().add(new TreeItem<>(new DbNode(NodeType.EMPTY, "No schemas found", "", profile, null, null)));
                    }
                });
            } catch (Throwable ex) {
                Platform.runLater(() -> {
                    dsItem.getChildren().setAll(new TreeItem<>(new DbNode(
                            NodeType.EMPTY,
                            "\u2717 " + ex.getMessage(),
                            "",
                            profile,
                            null,
                            null
                    )));
                    statusLabel.setText("\u2717 Connection failed: " + ex.getMessage());
                });
            }
        }, "lumina-db-schema-loader");
        t.setDaemon(true);
        t.start();
    }

    private void loadTableColumns(TreeItem<DbNode> tableItem, DbConnectionProfile profile, String schema, String tableName) {
        Thread t = new Thread(() -> {
            try {
                Connection conn = DbConnectionManager.getInstance().getConnection(profile);
                List<DbMetadataService.ColumnInfo> cols = DbMetadataService.loadColumns(conn, schema, tableName);
                Platform.runLater(() -> {
                    tableItem.getChildren().clear();
                    TreeItem<DbNode> colsGroup = new TreeItem<>(new DbNode(
                            NodeType.COLUMNS_GROUP,
                            "columns",
                            "(" + cols.size() + ")",
                            profile,
                            schema,
                            tableName
                    ));
                    for (DbMetadataService.ColumnInfo col : cols) {
                        String pkBadge = col.primaryKey() ? " [PK]" : "";
                        String typeStr = col.typeName() + (col.columnSize() > 0 ? "(" + col.columnSize() + ")" : "") + pkBadge;
                        colsGroup.getChildren().add(new TreeItem<>(new DbNode(
                                NodeType.COLUMN,
                                col.name(),
                                ": " + typeStr,
                                profile,
                                schema,
                                tableName
                        )));
                    }
                    tableItem.getChildren().add(colsGroup);
                    colsGroup.setExpanded(true);
                });
            } catch (Throwable ignored) {}
        }, "lumina-db-col-loader");
        t.setDaemon(true);
        t.start();
    }

    // Custom TreeCell with icons and context menus
    private class DbTreeCell extends TreeCell<DbNode> {
        @Override
        protected void updateItem(DbNode node, boolean empty) {
            super.updateItem(node, empty);
            if (empty || node == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }

            setText(node.toString());

            // Styling and icons based on NodeType
            switch (node.getType()) {
                case DATA_SOURCE:
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #E5E9F0;");
                    setContextMenu(createDataSourceContextMenu(node.getProfile()));
                    break;
                case SCHEMA:
                    setStyle("-fx-text-fill: #88C0D0; -fx-font-weight: bold;");
                    setContextMenu(createSchemaContextMenu(node.getProfile(), node.getSchema()));
                    break;
                case TABLES_GROUP:
                case VIEWS_GROUP:
                case COLUMNS_GROUP:
                    setStyle("-fx-text-fill: #697089; -fx-font-style: italic;");
                    setContextMenu(null);
                    break;
                case TABLE:
                    setStyle("-fx-text-fill: #D8DEE9;");
                    setContextMenu(createTableContextMenu(node.getProfile(), node.getSchema(), node.getTableName()));
                    break;
                case VIEW:
                    setStyle("-fx-text-fill: #B48EAD;");
                    setContextMenu(createTableContextMenu(node.getProfile(), node.getSchema(), node.getTableName()));
                    break;
                case COLUMN:
                    if (node.getExtra().contains("[PK]")) {
                        setStyle("-fx-text-fill: #EBCB8B; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #A6ADC4;");
                    }
                    setContextMenu(null);
                    break;
                case LOADING:
                case EMPTY:
                    setStyle("-fx-text-fill: #697089; -fx-font-style: italic;");
                    setContextMenu(null);
                    break;
                default:
                    setStyle("");
                    setContextMenu(null);
            }
        }
    }

    private ContextMenu createDataSourceContextMenu(DbConnectionProfile profile) {
        ContextMenu menu = new ContextMenu();
        MenuItem openConsole = new MenuItem("Open Query Console");
        openConsole.setOnAction(e -> {
            if (onOpenConsole != null) onOpenConsole.accept(profile);
        });

        MenuItem refresh = new MenuItem("Refresh");
        refresh.setOnAction(e -> refreshTree());

        MenuItem edit = new MenuItem("Properties\u2026");
        edit.setOnAction(e -> {
            DbConnectionDialog dialog = new DbConnectionDialog(profile);
            dialog.showAndWait().ifPresent(res -> {
                DbConnectionManager.getInstance().addOrUpdateProfile(res);
                refreshTree();
            });
        });

        MenuItem disconnect = new MenuItem("Disconnect");
        disconnect.setOnAction(e -> {
            DbConnectionManager.getInstance().disconnect(profile.getId());
            refreshTree();
        });

        MenuItem delete = new MenuItem("Remove Data Source");
        delete.setOnAction(e -> {
            DbConnectionManager.getInstance().removeProfile(profile.getId());
            refreshTree();
        });

        menu.getItems().addAll(openConsole, new SeparatorMenuItem(), refresh, edit, disconnect, new SeparatorMenuItem(), delete);
        return menu;
    }

    private ContextMenu createSchemaContextMenu(DbConnectionProfile profile, String schema) {
        ContextMenu menu = new ContextMenu();
        MenuItem openConsole = new MenuItem("Open Query Console for " + schema);
        openConsole.setOnAction(e -> {
            if (onOpenConsole != null) onOpenConsole.accept(profile);
        });
        menu.getItems().add(openConsole);
        return menu;
    }

    private ContextMenu createTableContextMenu(DbConnectionProfile profile, String schema, String table) {
        ContextMenu menu = new ContextMenu();
        MenuItem viewData = new MenuItem("View Table Data");
        viewData.setOnAction(e -> {
            if (onOpenTable != null) onOpenTable.accept(profile, new String[]{schema, table});
        });

        MenuItem queryConsole = new MenuItem("Select First 100 Rows");
        queryConsole.setOnAction(e -> {
            if (onOpenConsole != null) {
                onOpenConsole.accept(profile);
            }
        });

        menu.getItems().addAll(viewData, queryConsole);
        return menu;
    }
}
