package dev.lumina.ui;

import dev.lumina.plugin.PluginManifest;
import dev.lumina.plugin.PluginRegistry;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ-style Plugin Manager Dialog.
 * Shows language plugins with install/uninstall capabilities.
 * Dark theme matching Lumina IDE.
 */
public class PluginManagerDialog {

    private final Stage stage;
    private final ObservableList<PluginManifest> plugins = FXCollections.observableArrayList();
    private final ListView<PluginManifest> pluginList = new ListView<>();
    private final PluginDetailPanel detailPanel;
    private final TextField searchField = new TextField();
    private final ComboBox<String> filterCombo = new ComboBox<>();
    private final PluginRegistry registry = PluginRegistry.getInstance();
    private final Application application;

    private List<PluginManifest> allPlugins = new ArrayList<>();
    private Runnable onPluginChanged;

    public PluginManagerDialog(Stage owner) {
        this(owner, null, null);
    }

    public PluginManagerDialog(Stage owner, Runnable onPluginChanged) {
        this(owner, onPluginChanged, null);
    }

    public PluginManagerDialog(Stage owner, Runnable onPluginChanged, Application app) {
        this.onPluginChanged = onPluginChanged;
        this.application = app;
        this.detailPanel = new PluginDetailPanel(app);

        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Plugins - Lumina IDE");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "plugin-manager-dialog");

        // ---- Top: Search and filter ----
        HBox topBar = buildTopBar();
        root.setTop(topBar);

        // ---- Left: Plugin list ----
        pluginList.getStyleClass().add("plugin-list");
        pluginList.setCellFactory(lv -> new PluginCell());
        pluginList.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    if (selected != null) {
                        detailPanel.showPlugin(selected);
                    }
                });

        // ---- Center: Detail panel ----
        detailPanel.getStyleClass().add("plugin-detail");

        SplitPane split = new SplitPane(pluginList, detailPanel);
        split.setDividerPositions(0.38);
        split.getStyleClass().add("plugin-split-pane");

        root.setCenter(split);

        // ---- Bottom: Buttons ----
        HBox buttons = buildButtonBar();
        root.setBottom(buttons);

        Scene scene = new Scene(root, 860, 540);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);

        loadPlugins();
    }

    public void show() {
        stage.showAndWait();
    }

    private HBox buildTopBar() {
        searchField.setPromptText("Search plugins...");
        searchField.getStyleClass().add("plugin-search");
        searchField.textProperty().addListener((obs, old, text) -> filterPlugins());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        filterCombo.getItems().addAll("All", "Installed", "Available");
        filterCombo.getSelectionModel().selectFirst();
        filterCombo.getStyleClass().add("plugin-filter-combo");
        filterCombo.setOnAction(e -> filterPlugins());

        Button refreshBtn = new Button("\u21BB");
        refreshBtn.getStyleClass().add("plugin-refresh-btn");
        refreshBtn.setTooltip(new Tooltip("Refresh plugin list"));
        refreshBtn.setOnAction(e -> loadPlugins());

        HBox bar = new HBox(10, searchField, filterCombo, refreshBtn);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.getStyleClass().add("plugin-toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private HBox buildButtonBar() {
        Button close = new Button("Close");
        close.getStyleClass().add("plugin-close-btn");
        close.setOnAction(e -> stage.close());

        Button managePlugins = new Button("Manage plugins...");
        managePlugins.getStyleClass().add("plugin-manage-btn");
        managePlugins.setOnAction(e -> {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.initOwner(stage);
            info.setTitle("Manage Plugins");
            info.setHeaderText("Plugin Management");
            info.setContentText("You can manage installed plugins from the plugin list above.");
            info.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            info.showAndWait();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, spacer, managePlugins, close);
        bar.setPadding(new Insets(10, 20, 14, 20));
        bar.getStyleClass().add("plugin-footer");
        return bar;
    }

    private void loadPlugins() {
        plugins.clear();
        PluginManifest loading = new PluginManifest();
        loading.setId("loading");
        loading.setName("Loading plugins...");
        loading.setDescription("Fetching available plugins...");
        plugins.add(loading);

        Thread t = new Thread(() -> {
            try {
                allPlugins = registry.getAvailablePlugins();
                Platform.runLater(() -> {
                    plugins.setAll(allPlugins);
                    if (!plugins.isEmpty()) {
                        for (PluginManifest p : plugins) {
                            if (!"loading".equals(p.getId()) && !"error".equals(p.getId())) {
                                pluginList.getSelectionModel().select(p);
                                break;
                            }
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    plugins.clear();
                    PluginManifest error = new PluginManifest();
                    error.setId("error");
                    error.setName("Failed to load plugins");
                    error.setDescription("Error: " + e.getMessage());
                    plugins.add(error);
                });
            }
        }, "plugin-loader");
        t.setDaemon(true);
        t.start();
    }

    private void filterPlugins() {
        String query = searchField.getText().toLowerCase().trim();
        String filter = filterCombo.getValue();

        pluginList.setItems(FXCollections.observableArrayList(
                allPlugins.stream()
                        .filter(p -> !"loading".equals(p.getId()) && !"error".equals(p.getId()))
                        .filter(p -> {
                            if (query.isEmpty()) return true;
                            return p.getName().toLowerCase().contains(query)
                                    || p.getDescription().toLowerCase().contains(query)
                                    || (p.getVendor() != null && p.getVendor().toLowerCase().contains(query))
                                    || (p.getTags() != null && p.getTags().stream()
                                    .anyMatch(t -> t.contains(query)));
                        })
                        .filter(p -> {
                            boolean installed = registry.isInstalled(p.getId());
                            if ("Installed".equals(filter)) return installed;
                            if ("Available".equals(filter)) return !installed;
                            return true;
                        })
                        .toList()
        ));
    }

    // --------------------------------------------------- Plugin Cell

    private class PluginCell extends ListCell<PluginManifest> {
        @Override
        protected void updateItem(PluginManifest plugin, boolean empty) {
            super.updateItem(plugin, empty);
            if (empty || plugin == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            VBox content = new VBox(4);
            content.setPadding(new Insets(8, 12, 8, 12));

            HBox top = new HBox(12);
            top.setAlignment(Pos.CENTER_LEFT);

            // Icon with color
            Label iconLabel = new Label(plugin.getIcon() != null ? plugin.getIcon() :
                    String.valueOf(plugin.getName().charAt(0)));
            iconLabel.getStyleClass().add("plugin-icon");
            if (plugin.getColor() != null) {
                iconLabel.setStyle("-fx-background-color: " + plugin.getColor() + ";");
            }
            iconLabel.setMinSize(32, 32);
            iconLabel.setAlignment(Pos.CENTER);

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label name = new Label(plugin.getName());
            name.getStyleClass().add("plugin-name");

            Label desc = new Label(plugin.getDescription());
            desc.getStyleClass().add("plugin-desc");
            desc.setWrapText(true);
            desc.setMaxWidth(Double.MAX_VALUE);

            HBox meta = new HBox(10);
            meta.setAlignment(Pos.CENTER_LEFT);

            Label vendor = new Label(plugin.getVendor() != null ? plugin.getVendor() : "Lumina");
            vendor.getStyleClass().add("plugin-meta");

            Label version = new Label("v" + plugin.getVersion());
            version.getStyleClass().add("plugin-meta");

            if (plugin.getRating() != null) {
                Label rating = new Label("\u2605 " + plugin.getRating());
                rating.getStyleClass().add("plugin-meta");
                meta.getChildren().add(rating);
            }

            boolean installed = registry.isInstalled(plugin.getId());
            if (installed) {
                Label installedLabel = new Label("\u2713 Installed");
                installedLabel.getStyleClass().add("plugin-installed");
                meta.getChildren().add(installedLabel);
            }

            meta.getChildren().addAll(vendor, version);

            info.getChildren().addAll(name, desc, meta);

            // Status button - IntelliJ style
            Button statusBtn = new Button(installed ? "Installed" : "Install");
            statusBtn.getStyleClass().add(installed ? "plugin-btn-installed" : "plugin-btn-install");
            statusBtn.setOnAction(e -> {
                if (installed) {
                    uninstallPlugin(plugin);
                } else {
                    installPlugin(plugin);
                }
            });

            top.getChildren().addAll(iconLabel, info, statusBtn);
            content.getChildren().add(top);

            setGraphic(content);
            setText(null);
        }
    }

    private void installPlugin(PluginManifest plugin) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Install Plugin");
        confirm.setHeaderText("Install " + plugin.getName() + "?");
        confirm.setContentText("Version: " + plugin.getVersion() + "\nVendor: " + plugin.getVendor());
        confirm.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = registry.installPlugin(plugin.getId());
                if (success) {
                    int idx = allPlugins.indexOf(plugin);
                    if (idx >= 0) {
                        PluginManifest updated = allPlugins.get(idx);
                        updated.setInstalled(true);
                        pluginList.refresh();
                        detailPanel.showPlugin(updated);
                    }
                    filterPlugins();

                    if (onPluginChanged != null) {
                        onPluginChanged.run();
                    }

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.initOwner(stage);
                    successAlert.setTitle("Installation Complete");
                    successAlert.setHeaderText(plugin.getName() + " installed successfully");
                    successAlert.setContentText("The plugin is now available.");
                    successAlert.getDialogPane().getStylesheets().add(
                            getClass().getResource("/css/lumina-dark.css").toExternalForm());
                    successAlert.showAndWait();
                }
            }
        });
    }

    private void uninstallPlugin(PluginManifest plugin) {
        if (plugin.isBuiltIn()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.initOwner(stage);
            info.setTitle("Built-in Plugin");
            info.setHeaderText(plugin.getName() + " is a built-in plugin");
            info.setContentText("Built-in plugins cannot be uninstalled.");
            info.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            info.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Uninstall Plugin");
        confirm.setHeaderText("Uninstall " + plugin.getName() + "?");
        confirm.setContentText("This will remove the plugin from your IDE.");
        confirm.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = registry.uninstallPlugin(plugin.getId());
                if (success) {
                    int idx = allPlugins.indexOf(plugin);
                    if (idx >= 0) {
                        PluginManifest updated = allPlugins.get(idx);
                        updated.setInstalled(false);
                        pluginList.refresh();
                        detailPanel.showPlugin(updated);
                    }
                    filterPlugins();

                    if (onPluginChanged != null) {
                        onPluginChanged.run();
                    }
                }
            }
        });
    }

    // --------------------------------------------------- Detail Panel

    private static class PluginDetailPanel extends VBox {
        private final Label nameLabel = new Label();
        private final Label versionLabel = new Label();
        private final Label vendorLabel = new Label();
        private final Label ratingLabel = new Label();
        private final TextArea descriptionArea = new TextArea();
        private final Label tagsLabel = new Label();
        private final Button installBtn = new Button();
        private final Button homepageBtn = new Button("\uD83D\uDD17 Plugin homepage");
        private final Application application;

        public PluginDetailPanel(Application app) {
            this.application = app;
            getStyleClass().add("plugin-detail-panel");
            setPadding(new Insets(16, 20, 16, 20));
            setSpacing(12);

            // Header
            HBox header = new HBox(14);
            header.setAlignment(Pos.CENTER_LEFT);

            Label iconLabel = new Label("L");
            iconLabel.getStyleClass().add("plugin-detail-icon");
            iconLabel.setMinSize(48, 48);
            iconLabel.setAlignment(Pos.CENTER);

            VBox headerInfo = new VBox(4);
            nameLabel.getStyleClass().add("plugin-detail-name");
            versionLabel.getStyleClass().add("plugin-detail-version");

            HBox metaRow = new HBox(14);
            metaRow.setAlignment(Pos.CENTER_LEFT);
            vendorLabel.getStyleClass().add("plugin-detail-meta");
            ratingLabel.getStyleClass().add("plugin-detail-meta");

            metaRow.getChildren().addAll(vendorLabel, ratingLabel);

            headerInfo.getChildren().addAll(nameLabel, versionLabel, metaRow);
            header.getChildren().addAll(iconLabel, headerInfo);
            HBox.setHgrow(headerInfo, Priority.ALWAYS);

            // Description
            descriptionArea.setEditable(false);
            descriptionArea.setWrapText(true);
            descriptionArea.setPrefHeight(70);
            descriptionArea.getStyleClass().add("plugin-detail-description");

            // Tags
            tagsLabel.getStyleClass().add("plugin-detail-tags");

            // Buttons
            HBox buttons = new HBox(10);
            buttons.setAlignment(Pos.CENTER_LEFT);
            installBtn.getStyleClass().add("plugin-btn-install");
            homepageBtn.getStyleClass().add("plugin-btn-homepage");

            buttons.getChildren().addAll(installBtn, homepageBtn);

            // Tabs - IntelliJ style
            TabPane tabs = new TabPane();
            tabs.getStyleClass().add("plugin-detail-tabs");

            Tab overview = new Tab("Overview");
            Tab whatsNew = new Tab("What's New");
            Tab reviews = new Tab("Reviews");

            VBox overviewContent = new VBox(10, descriptionArea, tagsLabel);
            overviewContent.setPadding(new Insets(12, 0, 0, 0));
            overview.setContent(overviewContent);

            Label whatsNewLabel = new Label("Latest version includes performance improvements and bug fixes.");
            whatsNewLabel.getStyleClass().add("plugin-detail-text");
            whatsNewLabel.setWrapText(true);
            whatsNew.setContent(whatsNewLabel);

            Label reviewsLabel = new Label("No reviews yet.");
            reviewsLabel.getStyleClass().add("plugin-detail-text");
            reviews.setContent(reviewsLabel);

            tabs.getTabs().addAll(overview, whatsNew, reviews);

            getChildren().addAll(header, tabs, buttons);
            setVisible(false);
        }

        public void showPlugin(PluginManifest plugin) {
            nameLabel.setText(plugin.getName());
            versionLabel.setText(plugin.getVersion());
            vendorLabel.setText(plugin.getVendor() != null ? plugin.getVendor() : "Lumina");
            ratingLabel.setText(plugin.getRating() != null ? "\u2605 " + plugin.getRating() + " / 5.0" : "Not rated");
            descriptionArea.setText(plugin.getDescription());
            tagsLabel.setText(plugin.getTags() != null ? String.join("  •  ", plugin.getTags()) : "");

            boolean installed = PluginRegistry.getInstance().isInstalled(plugin.getId());

            installBtn.setText(installed ? "Installed" : "Install");
            installBtn.setDisable(installed);
            installBtn.getStyleClass().clear();
            installBtn.getStyleClass().add(installed ? "plugin-btn-installed" : "plugin-btn-install");
            installBtn.setOnAction(e -> {
                if (!installed) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Install Plugin");
                    confirm.setHeaderText("Install " + plugin.getName() + "?");
                    confirm.setContentText("Version: " + plugin.getVersion());
                    confirm.getDialogPane().getStylesheets().add(
                            getClass().getResource("/css/lumina-dark.css").toExternalForm());
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            PluginRegistry.getInstance().installPlugin(plugin.getId());
                            showPlugin(plugin);
                        }
                    });
                }
            });

            homepageBtn.setOnAction(e -> {
                if (plugin.getHomepage() != null && !plugin.getHomepage().isEmpty()) {
                    // Use the application's HostServices
                    if (application != null) {
                        try {
                            javafx.application.HostServices services = application.getHostServices();
                            if (services != null) {
                                services.showDocument(plugin.getHomepage());
                                return;
                            }
                        } catch (Exception ignored) {}
                    }

                    // Fallback: use Desktop if available
                    try {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(plugin.getHomepage()));
                    } catch (Exception ignored) {
                        // Silent fallback
                    }
                }
            });

            setVisible(true);
        }
    }
}