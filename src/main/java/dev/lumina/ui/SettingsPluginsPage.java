package dev.lumina.ui;

import dev.lumina.plugin.PluginItem;
import dev.lumina.plugin.PluginManager;
import dev.lumina.plugin.PluginManager.FilterOption;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * Modern Plugins settings page for Lumina IDE.
 * Implements Marketplace, Installed tabs, Gear menu options, real-time search,
 * dynamic enable/disable/update/install, and interactive details with preview carousel.
 */
public class SettingsPluginsPage extends VBox {

    private enum TabMode {
        MARKETPLACE, INSTALLED
    }

    private final PluginManager pluginManager = PluginManager.getInstance();

    private TabMode currentTab = TabMode.MARKETPLACE;
    private FilterOption currentInstalledFilter = FilterOption.DOWNLOADED;

    // Header controls
    private final ToggleButton marketplaceTabBtn = new ToggleButton("Marketplace");
    private final ToggleButton installedTabBtn = new ToggleButton("Installed 10");
    private final Button gearBtn = new Button("⚙");

    // Search and filter row
    private final TextField searchField = new TextField();
    private final Button filterDotsBtn = new Button("⋮");

    // Master list container
    private final VBox listContainer = new VBox();
    private final ScrollPane listScrollPane = new ScrollPane(listContainer);

    // Detail panel
    private final VBox detailContainer = new VBox();
    private final ScrollPane detailScrollPane = new ScrollPane(detailContainer);

    // Selected plugin
    private PluginItem selectedPlugin;
    private String activeDetailTab = "Overview";
    private int currentCarouselIndex = 0;

    public SettingsPluginsPage() {
        this(false);
    }

    public SettingsPluginsPage(boolean startOnInstalled) {
        if (startOnInstalled) {
            currentTab = TabMode.INSTALLED;
        }

        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(0));
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();

        // Listen for model state changes to dynamically re-render
        pluginManager.addListener(this::refreshUI);
    }

    private void buildUI() {
        getChildren().clear();

        // 1. Top Header Bar
        HBox topBar = buildTopBar();

        // 2. Main content split pane
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Left master column
        VBox masterColumn = buildMasterColumn();
        VBox.setVgrow(masterColumn, Priority.ALWAYS);

        // Right detail column
        VBox detailColumn = buildDetailColumn();
        VBox.setVgrow(detailColumn, Priority.ALWAYS);

        splitPane.getItems().addAll(masterColumn, detailColumn);
        splitPane.setDividerPositions(0.42);

        getChildren().addAll(topBar, splitPane);

        // Initial selection and render
        refreshMasterList();
    }

    // =========================================================================
    // Header Bar & Tabs
    // =========================================================================

    private HBox buildTopBar() {
        Label titleLabel = new Label("Plugins");
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Segmented pill buttons
        ToggleGroup group = new ToggleGroup();
        marketplaceTabBtn.setToggleGroup(group);
        installedTabBtn.setToggleGroup(group);

        updateTabButtonStyles();

        marketplaceTabBtn.setOnAction(e -> {
            currentTab = TabMode.MARKETPLACE;
            updateTabButtonStyles();
            filterDotsBtn.setVisible(false);
            refreshMasterList();
        });

        installedTabBtn.setOnAction(e -> {
            currentTab = TabMode.INSTALLED;
            updateTabButtonStyles();
            filterDotsBtn.setVisible(true);
            refreshMasterList();
        });

        if (currentTab == TabMode.MARKETPLACE) {
            marketplaceTabBtn.setSelected(true);
            filterDotsBtn.setVisible(false);
        } else {
            installedTabBtn.setSelected(true);
            filterDotsBtn.setVisible(true);
        }

        HBox segmentedBar = new HBox(0, marketplaceTabBtn, installedTabBtn);
        segmentedBar.setAlignment(Pos.CENTER);
        segmentedBar.setStyle("-fx-background-color: #2B2D30; -fx-background-radius: 6; -fx-padding: 2;");

        // Gear icon button
        gearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 4 8;");
        gearBtn.setOnMouseEntered(e -> gearBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 4;"));
        gearBtn.setOnMouseExited(e -> gearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 4 8;"));
        gearBtn.setOnAction(e -> showGearMenu());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, titleLabel, spacer, segmentedBar, gearBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle("-fx-border-color: #323438; -fx-border-width: 0 0 1 0; -fx-background-color: #1E1F22;");
        return header;
    }

    private void updateTabButtonStyles() {
        int badgeCount = pluginManager.getBadgeCount();
        installedTabBtn.setText("Installed " + badgeCount);

        String activeStyle = "-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 4 14;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #B9BECF; -fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 4 14; -fx-cursor: hand;";

        if (currentTab == TabMode.MARKETPLACE) {
            marketplaceTabBtn.setStyle(activeStyle);
            installedTabBtn.setStyle(inactiveStyle);
        } else {
            marketplaceTabBtn.setStyle(inactiveStyle);
            installedTabBtn.setStyle(activeStyle);
        }
    }

    // =========================================================================
    // Gear Menu (Image 4)
    // =========================================================================

    private void showGearMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-padding: 4 0;");

        // 1. Update Plugins Automatically
        CheckMenuItem autoUpdateItem = new CheckMenuItem("Update Plugins Automatically");
        autoUpdateItem.setSelected(pluginManager.isAutoUpdateEnabled());
        autoUpdateItem.setOnAction(e -> pluginManager.setAutoUpdateEnabled(autoUpdateItem.isSelected()));

        // 2. Manage Plugin Repositories...
        MenuItem manageReposItem = new MenuItem("Manage Plugin Repositories...");
        manageReposItem.setOnAction(e -> new PluginRepositoriesDialog(getScene() != null ? getScene().getWindow() : null).showAndWait());

        // 3. HTTP Proxy Settings...
        MenuItem httpProxyItem = new MenuItem("HTTP Proxy Settings...");
        httpProxyItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("HTTP Proxy Settings");
            alert.setHeaderText("HTTP Proxy Configuration");
            alert.setContentText("Plugin updates will use the system proxy settings configured under Settings | Appearance & Behavior | System Settings | HTTP Proxy.");
            alert.showAndWait();
        });

        // 4. Manage Plugin Certificates...
        MenuItem manageCertsItem = new MenuItem("Manage Plugin Certificates...");
        manageCertsItem.setOnAction(e -> new PluginCertificatesDialog(getScene() != null ? getScene().getWindow() : null).showAndWait());

        // 5. Install Plugin from Disk...
        MenuItem installDiskItem = new MenuItem("Install Plugin from Disk...");
        installDiskItem.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Plugin File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Plugin Archive (*.jar, *.zip)", "*.jar", "*.zip"));
            File f = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                pluginManager.installPluginFromDisk(f);
                currentTab = TabMode.INSTALLED;
                updateTabButtonStyles();
                refreshMasterList();
            }
        });

        // 6. Disable All Downloaded Plugins
        MenuItem disableAllItem = new MenuItem("Disable All Downloaded Plugins");
        disableAllItem.setOnAction(e -> pluginManager.disableAllDownloaded());

        // 7. Enable All Downloaded Plugins
        MenuItem enableAllItem = new MenuItem("Enable All Downloaded Plugins");
        enableAllItem.setOnAction(e -> pluginManager.enableAllDownloaded());

        // RozeHub Platform Integration
        MenuItem rozehubSyncItem = new MenuItem("Refresh from RozeHub Marketplace");
        rozehubSyncItem.setOnAction(e -> {
            pluginManager.refreshMarketplaceFromRozeHubAsync();
            refreshUI();
        });

        MenuItem rozehubConfigItem = new MenuItem("Configure RozeHub Server...");
        rozehubConfigItem.setOnAction(e -> new RozeHubConfigDialog(getScene() != null ? getScene().getWindow() : null).showAndWait());

        menu.getItems().addAll(
                autoUpdateItem,
                new SeparatorMenuItem(),
                rozehubSyncItem,
                rozehubConfigItem,
                manageReposItem,
                httpProxyItem,
                new SeparatorMenuItem(),
                manageCertsItem,
                installDiskItem,
                new SeparatorMenuItem(),
                disableAllItem,
                enableAllItem
        );

        menu.show(gearBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    // =========================================================================
    // Master Column (Search + List)
    // =========================================================================

    private VBox buildMasterColumn() {
        VBox col = new VBox();
        col.setStyle("-fx-background-color: #1E1F22;");

        // Search Bar row
        searchField.setPromptText("Type / to see options");
        searchField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10; -fx-font-size: 12px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, old, text) -> refreshMasterList());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SLASH && searchField.getText().isEmpty()) {
                showSearchOptionsPopup();
            }
        });

        // 3-dots filter button (for Installed tab - Image 5)
        filterDotsBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #B9BECF; -fx-font-size: 14px; -fx-cursor: hand; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
        filterDotsBtn.setOnAction(e -> showFilterMenu());

        HBox searchRow = new HBox(8, searchField, filterDotsBtn);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setPadding(new Insets(10, 12, 10, 12));

        // List scroll pane
        listScrollPane.setFitToWidth(true);
        listScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listScrollPane.setStyle("-fx-background-color: #1E1F22; -fx-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(listScrollPane, Priority.ALWAYS);

        col.getChildren().addAll(searchRow, listScrollPane);
        return col;
    }

    private void showSearchOptionsPopup() {
        ContextMenu popup = new ContextMenu();
        popup.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");
        List<String> options = List.of("/downloaded", "/update", "/enabled", "/disabled", "/bundled", "/tag:code", "/tag:editor");
        for (String opt : options) {
            MenuItem item = new MenuItem(opt);
            item.setOnAction(e -> {
                searchField.setText(opt);
                searchField.positionCaret(opt.length());
            });
            popup.getItems().add(item);
        }
        popup.show(searchField, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void showFilterMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-padding: 4 0;");

        Label header = new Label("Show");
        header.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12;");
        CustomMenuItem headerItem = new CustomMenuItem(header, false);
        menu.getItems().add(headerItem);

        for (FilterOption opt : FilterOption.values()) {
            if (opt == FilterOption.ALL) continue;
            RadioMenuItem item = new RadioMenuItem(opt.getLabel());
            item.setSelected(currentInstalledFilter == opt);
            item.setOnAction(e -> {
                currentInstalledFilter = opt;
                refreshMasterList();
            });
            menu.getItems().add(item);
        }

        menu.show(filterDotsBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    // =========================================================================
    // Master List Rendering (Marketplace vs Installed)
    // =========================================================================

    private void refreshMasterList() {
        listContainer.getChildren().clear();
        String query = searchField.getText();

        if (currentTab == TabMode.MARKETPLACE) {
            renderMarketplaceList(query);
        } else {
            renderInstalledList(query);
        }

        // Keep selected plugin active or pick first
        if (selectedPlugin == null && !listContainer.getChildren().isEmpty()) {
            List<PluginItem> all = currentTab == TabMode.MARKETPLACE
                    ? pluginManager.getFilteredMarketplacePlugins(query)
                    : pluginManager.getFilteredInstalledPlugins(currentInstalledFilter, query);
            if (!all.isEmpty()) {
                selectedPlugin = all.get(0);
            }
        }
        refreshDetailPanel();
    }

    private void renderMarketplaceList(String query) {
        List<PluginItem> filtered = pluginManager.getFilteredMarketplacePlugins(query);

        // Group into Staff Picks & New and Updated
        List<PluginItem> staffPicks = filtered.stream()
                .filter(p -> "Staff Picks".equalsIgnoreCase(p.getMarketplaceSection()) || p.getMarketplaceSection() == null)
                .toList();

        List<PluginItem> newAndUpdated = filtered.stream()
                .filter(p -> "New and Updated".equalsIgnoreCase(p.getMarketplaceSection()))
                .toList();

        if (!staffPicks.isEmpty()) {
            listContainer.getChildren().add(buildSectionHeader("Staff Picks", () -> searchField.setText("")));
            for (PluginItem item : staffPicks) {
                listContainer.getChildren().add(buildMarketplaceCell(item));
            }
        }

        if (!newAndUpdated.isEmpty()) {
            listContainer.getChildren().add(buildSectionHeader("New and Updated", () -> searchField.setText("")));
            for (PluginItem item : newAndUpdated) {
                listContainer.getChildren().add(buildMarketplaceCell(item));
            }
        }

        if (staffPicks.isEmpty() && newAndUpdated.isEmpty()) {
            Label emptyLbl = new Label("No plugins found matching '" + query + "'");
            emptyLbl.setStyle("-fx-text-fill: #6F737A; -fx-padding: 24;");
            listContainer.getChildren().add(emptyLbl);
        }
    }

    private void renderInstalledList(String query) {
        List<PluginItem> filtered = pluginManager.getFilteredInstalledPlugins(currentInstalledFilter, query);

        // Group 1: Bundled plugins (often with available updates)
        List<PluginItem> bundled = filtered.stream().filter(PluginItem::isBundled).toList();

        // Group 2: Downloaded plugins
        List<PluginItem> downloaded = filtered.stream().filter(p -> !p.isBundled()).toList();

        if (!bundled.isEmpty()) {
            for (PluginItem item : bundled) {
                listContainer.getChildren().add(buildInstalledCell(item));
            }
        }

        if (!downloaded.isEmpty()) {
            int enabledCount = pluginManager.getDownloadedEnabledCount();
            int totalCount = pluginManager.getDownloadedTotalCount();
            String sectionTitle = "Downloaded (" + enabledCount + " of " + totalCount + " enabled)";
            listContainer.getChildren().add(buildSectionHeader(sectionTitle, null));

            for (PluginItem item : downloaded) {
                listContainer.getChildren().add(buildInstalledCell(item));
            }
        }

        if (bundled.isEmpty() && downloaded.isEmpty()) {
            Label emptyLbl = new Label("No installed plugins match the filter");
            emptyLbl.setStyle("-fx-text-fill: #6F737A; -fx-padding: 24;");
            listContainer.getChildren().add(emptyLbl);
        }
    }

    private Node buildSectionHeader(String title, Runnable onShowAll) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 12, 6, 12));
        header.setStyle("-fx-background-color: #1E1F22;");

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(label, spacer);

        if (onShowAll != null) {
            Hyperlink showAll = new Hyperlink("Show all");
            showAll.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
            showAll.setOnAction(e -> onShowAll.run());
            header.getChildren().add(showAll);
        }

        return header;
    }

    private Node buildMarketplaceCell(PluginItem item) {
        HBox cell = new HBox(10);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(8, 12, 8, 12));

        boolean isSelected = item.equals(selectedPlugin);
        applyCellBackground(cell, isSelected);

        cell.setOnMouseClicked(e -> {
            selectedPlugin = item;
            refreshMasterList();
        });

        // Icon
        Node icon = PluginIconFactory.createIcon(item, 32);

        // Center text info
        VBox textInfo = new VBox(2);
        HBox.setHgrow(textInfo, Priority.ALWAYS);

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: 500;");

        // Subtitle line: downloads, rating, version, vendor
        HBox metaLine = new HBox(6);
        metaLine.setAlignment(Pos.CENTER_LEFT);

        Label downloadsLbl = new Label("↓ " + item.getDownloads());
        downloadsLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        Label ratingLbl = new Label("☆ " + item.getRating());
        ratingLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        Label vendorLbl = new Label(item.getVendor());
        vendorLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        metaLine.getChildren().addAll(downloadsLbl, ratingLbl, vendorLbl);
        textInfo.getChildren().addAll(nameLabel, metaLine);

        // Right side action control: Install button or Checkbox if already installed
        Node rightAction;
        if (item.isInstalled()) {
            CheckBox enabledCheck = new CheckBox();
            enabledCheck.setSelected(item.isEnabled());
            enabledCheck.setStyle("-fx-opacity: 1.0; -fx-cursor: hand;");
            enabledCheck.setOnAction(e -> {
                item.setEnabled(enabledCheck.isSelected());
                pluginManager.setPluginEnabled(item.getId(), enabledCheck.isSelected());
            });
            rightAction = enabledCheck;
        } else {
            Button installBtn = new Button("Install");
            installBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 12; -fx-font-size: 11px; -fx-cursor: hand;");
            installBtn.setOnAction(e -> {
                installBtn.setText("Installing...");
                installBtn.setDisable(true);
                pluginManager.installPlugin(item.getId(),
                        msg -> refreshUI(),
                        err -> {
                            installBtn.setText("Install");
                            installBtn.setDisable(false);
                            Alert a = new Alert(Alert.AlertType.ERROR, err, ButtonType.OK);
                            a.showAndWait();
                        },
                        prog -> {
                            if (prog != null && prog > 0) {
                                Platform.runLater(() -> installBtn.setText((int)(prog * 100) + "%"));
                            }
                        }
                );
            });
            rightAction = installBtn;
        }

        cell.getChildren().addAll(icon, textInfo, rightAction);
        return cell;
    }

    private Node buildInstalledCell(PluginItem item) {
        HBox cell = new HBox(10);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(8, 12, 8, 12));

        boolean isSelected = item.equals(selectedPlugin);
        applyCellBackground(cell, isSelected);

        cell.setOnMouseClicked(e -> {
            selectedPlugin = item;
            refreshMasterList();
        });

        // Icon
        Node icon = PluginIconFactory.createIcon(item, 32);

        // Text Info
        VBox textInfo = new VBox(2);
        HBox.setHgrow(textInfo, Priority.ALWAYS);

        HBox titleRow = new HBox(6);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: 500;");
        titleRow.getChildren().add(nameLabel);

        if (item.isFreemium()) {
            Label freemiumBadge = new Label("Freemium");
            freemiumBadge.setStyle("-fx-background-color: #25324D; -fx-text-fill: #589DF6; -fx-font-size: 10px; -fx-padding: 1 5; -fx-background-radius: 3;");
            titleRow.getChildren().add(freemiumBadge);
        }

        // Subtitle line
        String versionText;
        if (item.hasUpdate()) {
            versionText = item.getVersion() + " → " + item.getAvailableVersion();
        } else {
            versionText = item.getVersion() + (item.getVendor() != null ? "  " + item.getVendor() : "");
        }
        Label subLabel = new Label(versionText);
        subLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        textInfo.getChildren().addAll(titleRow, subLabel);

        // Right side: Update button (if update available) + Checkbox
        HBox rightBox = new HBox(8);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        if (item.hasUpdate()) {
            Button updateBtn = new Button("Update");
            updateBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 12; -fx-background-radius: 4; -fx-cursor: hand;");
            updateBtn.setOnAction(e -> {
                pluginManager.updatePlugin(item.getId());
                refreshUI();
            });
            rightBox.getChildren().add(updateBtn);
        }

        CheckBox enabledCheck = new CheckBox();
        enabledCheck.setSelected(item.isEnabled());
        enabledCheck.setStyle("-fx-opacity: 1.0; -fx-cursor: hand;");
        enabledCheck.setOnAction(e -> {
            item.setEnabled(enabledCheck.isSelected());
            pluginManager.setPluginEnabled(item.getId(), enabledCheck.isSelected());
            refreshUI();
        });
        rightBox.getChildren().add(enabledCheck);

        cell.getChildren().addAll(icon, textInfo, rightBox);
        return cell;
    }

    private void applyCellBackground(HBox cell, boolean isSelected) {
        if (isSelected) {
            cell.setStyle("-fx-background-color: #2E436E; -fx-background-radius: 4;");
        } else {
            cell.setStyle("-fx-background-color: transparent; -fx-background-radius: 4;");
            cell.setOnMouseEntered(e -> {
                if (!cell.getStyle().contains("#2E436E")) {
                    cell.setStyle("-fx-background-color: #2B2D30; -fx-background-radius: 4;");
                }
            });
            cell.setOnMouseExited(e -> {
                if (!cell.getStyle().contains("#2E436E")) {
                    cell.setStyle("-fx-background-color: transparent; -fx-background-radius: 4;");
                }
            });
        }
    }

    // =========================================================================
    // Detail Column (Image 1, 2, 3)
    // =========================================================================

    private VBox buildDetailColumn() {
        VBox col = new VBox();
        col.setStyle("-fx-background-color: #1E1F22; -fx-padding: 16;");

        detailScrollPane.setFitToWidth(true);
        detailScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScrollPane.setStyle("-fx-background-color: #1E1F22; -fx-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(detailScrollPane, Priority.ALWAYS);

        col.getChildren().add(detailScrollPane);
        return col;
    }

    private void refreshDetailPanel() {
        detailContainer.getChildren().clear();

        if (selectedPlugin == null) {
            Label noSelect = new Label("Select a plugin to view details");
            noSelect.setStyle("-fx-text-fill: #6F737A; -fx-padding: 40; -fx-font-size: 13px;");
            detailContainer.getChildren().add(noSelect);
            return;
        }

        PluginItem p = selectedPlugin;

        // 1. Tag Chips (Image 1)
        if (!p.getTags().isEmpty()) {
            FlowPane tagsPane = new FlowPane(6, 6);
            tagsPane.setPadding(new Insets(0, 0, 8, 0));

            for (String tag : p.getTags()) {
                Label tagChip = new Label(tag);
                if ("Freemium".equalsIgnoreCase(tag)) {
                    tagChip.setStyle("-fx-background-color: #25324D; -fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;");
                } else {
                    tagChip.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #868A91; -fx-font-size: 11px; -fx-padding: 2 8; -fx-background-radius: 10;");
                }
                tagsPane.getChildren().add(tagChip);
            }
            detailContainer.getChildren().add(tagsPane);
        }

        // 2. Large Plugin Title
        Label titleLabel = new Label(p.getName());
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 20px; -fx-font-weight: bold;");

        // 3. Vendor Line & Homepage Link
        HBox vendorRow = new HBox(12);
        vendorRow.setAlignment(Pos.CENTER_LEFT);

        if (p.getVendor() != null && !p.getVendor().isBlank()) {
            Hyperlink vendorLink = new Hyperlink(p.getVendor());
            vendorLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
            vendorRow.getChildren().add(vendorLink);
        }

        Hyperlink homepageLink = new Hyperlink("Plugin homepage ↗");
        homepageLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        vendorRow.getChildren().add(homepageLink);

        // 4. Action Bar (Install / Update / Disable split button with Uninstall)
        HBox actionBar = buildDetailActionBar(p);
        actionBar.setPadding(new Insets(10, 0, 8, 0));

        // 5. Terms Notice
        Node termsBox = null;
        if (p.getTermsNotice() != null) {
            Hyperlink termsLink = new Hyperlink(p.getTermsNotice());
            termsLink.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0 0 8 0;");
            termsBox = termsLink;
        }

        // 6. Sub-tabs bar: Overview, What's New, Reviews, Additional Info
        HBox subTabsBar = buildSubTabsBar();
        subTabsBar.setPadding(new Insets(6, 0, 12, 0));

        // 7. Active Sub-tab Content
        Node tabContent = buildSubTabContent(p);

        detailContainer.getChildren().addAll(titleLabel, vendorRow, actionBar);
        if (termsBox != null) {
            detailContainer.getChildren().add(termsBox);
        }
        detailContainer.getChildren().addAll(subTabsBar, tabContent);
    }

    private HBox buildDetailActionBar(PluginItem p) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);

        // Update button if update available (Image 3)
        if (p.hasUpdate()) {
            Button updateBtn = new Button("Update");
            updateBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 5 16; -fx-background-radius: 4; -fx-cursor: hand;");
            updateBtn.setOnAction(e -> {
                pluginManager.updatePlugin(p.getId());
                refreshUI();
            });
            bar.getChildren().add(updateBtn);
        }

        if (p.isInstalled()) {
            // Split button: Disable/Enable + Dropdown arrow for Uninstall (Image 2)
            Button disableBtn = new Button(p.isEnabled() ? "Disable" : "Enable");
            disableBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 5 14; -fx-background-radius: 4 0 0 4; -fx-border-color: #393B40; -fx-cursor: hand;");
            disableBtn.setOnAction(e -> {
                boolean newState = !p.isEnabled();
                p.setEnabled(newState);
                pluginManager.setPluginEnabled(p.getId(), newState);
                refreshUI();
            });

            Button arrowBtn = new Button("▾");
            arrowBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #868A91; -fx-font-size: 10px; -fx-padding: 6 8; -fx-background-radius: 0 4 4 0; -fx-border-color: #393B40; -fx-border-width: 1 1 1 0; -fx-cursor: hand;");
            arrowBtn.setOnAction(e -> {
                ContextMenu ctx = new ContextMenu();
                ctx.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");
                MenuItem uninstallItem = new MenuItem("Uninstall");
                uninstallItem.setOnAction(ev -> {
                    pluginManager.uninstallPlugin(p.getId());
                    refreshUI();
                });
                ctx.getItems().add(uninstallItem);
                ctx.show(arrowBtn, javafx.geometry.Side.BOTTOM, 0, 0);
            });

            HBox splitBtn = new HBox(0, disableBtn, arrowBtn);
            bar.getChildren().add(splitBtn);

            // Version text
            String verText = p.hasUpdate()
                    ? p.getVersion() + " → " + p.getAvailableVersion()
                    : p.getVersion();
            Label verLabel = new Label(verText);
            verLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
            bar.getChildren().add(verLabel);

        } else {
            // Marketplace uninstalled -> Install button
            Button installBtn = new Button("Install");
            installBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 5 18; -fx-background-radius: 4; -fx-cursor: hand;");
            installBtn.setOnAction(e -> {
                installBtn.setText("Installing...");
                installBtn.setDisable(true);
                pluginManager.installPlugin(p.getId(),
                        msg -> refreshUI(),
                        err -> {
                            installBtn.setText("Install");
                            installBtn.setDisable(false);
                            Alert a = new Alert(Alert.AlertType.ERROR, err, ButtonType.OK);
                            a.showAndWait();
                        },
                        prog -> {
                            if (prog != null && prog > 0) {
                                Platform.runLater(() -> installBtn.setText((int)(prog * 100) + "%"));
                            }
                        }
                );
            });
            bar.getChildren().add(installBtn);

            Label verLabel = new Label(p.getVersion());
            verLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
            bar.getChildren().add(verLabel);
        }

        return bar;
    }

    private HBox buildSubTabsBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-border-color: #323438; -fx-border-width: 0 0 1 0;");

        List<String> tabs = List.of("Overview", "What's New", "Reviews", "Additional Info");
        for (String tabName : tabs) {
            Button tabBtn = new Button(tabName);
            boolean isActive = tabName.equalsIgnoreCase(activeDetailTab);
            if (isActive) {
                tabBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #589DF6; -fx-font-weight: bold; -fx-font-size: 12px; -fx-border-color: #3574F0; -fx-border-width: 0 0 2 0; -fx-padding: 6 2 6 2; -fx-cursor: hand;");
            } else {
                tabBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 6 2 6 2; -fx-cursor: hand;");
            }
            tabBtn.setOnAction(e -> {
                activeDetailTab = tabName;
                refreshDetailPanel();
            });
            bar.getChildren().add(tabBtn);
        }
        return bar;
    }

    private Node buildSubTabContent(PluginItem p) {
        VBox content = new VBox(12);

        switch (activeDetailTab) {
            case "What's New":
                content.getChildren().add(buildWhatsNewContent(p));
                break;
            case "Reviews":
                content.getChildren().add(buildReviewsContent(p));
                break;
            case "Additional Info":
                content.getChildren().add(buildAdditionalInfoContent(p));
                break;
            case "Overview":
            default:
                content.getChildren().add(buildOverviewContent(p));
                break;
        }

        return content;
    }

    // =========================================================================
    // Sub-Tab: Overview (with Carousel Hero)
    // =========================================================================

    private Node buildOverviewContent(PluginItem p) {
        VBox box = new VBox(14);

        // Preview Carousel (Image 1)
        if (!p.getCarouselSlides().isEmpty()) {
            box.getChildren().add(buildCarouselBanner(p));
        }

        // Subheading
        if (p.getDescriptionHeading() != null && !p.getDescriptionHeading().isBlank()) {
            Label heading = new Label(p.getDescriptionHeading());
            heading.setWrapText(true);
            heading.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-font-weight: bold;");
            box.getChildren().add(heading);
        }

        // Short description
        if (p.getShortDescription() != null && !p.getShortDescription().isBlank()) {
            Label desc = new Label(p.getShortDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: #B9BECF; -fx-font-size: 12px; -fx-line-spacing: 3px;");
            box.getChildren().add(desc);
        }

        // Bulleted features
        if (!p.getFeatures().isEmpty()) {
            Label featureIntro = new Label("The following features are available:");
            featureIntro.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
            box.getChildren().add(featureIntro);

            VBox bulletList = new VBox(6);
            bulletList.setPadding(new Insets(0, 0, 0, 8));

            for (String f : p.getFeatures()) {
                HBox row = new HBox(6);
                Label bullet = new Label("•");
                bullet.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
                Label text = new Label(f);
                text.setWrapText(true);
                text.setStyle("-fx-text-fill: #B9BECF; -fx-font-size: 12px;");
                row.getChildren().addAll(bullet, text);
                bulletList.getChildren().add(row);
            }
            box.getChildren().add(bulletList);
        }

        // Documentation footer link
        HBox docBox = new HBox(4);
        Label seeDoc = new Label("See the ");
        seeDoc.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        Hyperlink docLink = new Hyperlink("documentation");
        docLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        Label forDetails = new Label(" for details.");
        forDetails.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        docBox.getChildren().addAll(seeDoc, docLink, forDetails);
        box.getChildren().add(docBox);

        return box;
    }

    private Node buildCarouselBanner(PluginItem p) {
        List<PluginItem.CarouselSlide> slides = p.getCarouselSlides();
        if (currentCarouselIndex >= slides.size()) {
            currentCarouselIndex = 0;
        }
        PluginItem.CarouselSlide slide = slides.get(currentCarouselIndex);

        StackPane banner = new StackPane();
        banner.setPrefHeight(200);
        banner.setStyle("-fx-background-color: #121316; -fx-background-radius: 8; -fx-border-color: #2B2D30; -fx-border-radius: 8;");

        // Slide Content
        VBox slideContent = new VBox(10);
        slideContent.setAlignment(Pos.CENTER);
        slideContent.setPadding(new Insets(20));

        Label slideTitle = new Label(slide.getTitle());
        slideTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-alignment: center;");
        slideTitle.setWrapText(true);

        Label slideSub = new Label(slide.getSubtitle());
        slideSub.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px; -fx-text-alignment: center;");

        slideContent.getChildren().addAll(slideTitle, slideSub);

        // Left and Right arrows
        Button leftArrow = new Button("‹");
        leftArrow.setStyle("-fx-background-color: rgba(30, 31, 34, 0.7); -fx-text-fill: #DFE1E5; -fx-font-size: 18px; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 2 10;");
        leftArrow.setOnAction(e -> {
            currentCarouselIndex = (currentCarouselIndex - 1 + slides.size()) % slides.size();
            refreshDetailPanel();
        });
        StackPane.setAlignment(leftArrow, Pos.CENTER_LEFT);
        StackPane.setMargin(leftArrow, new Insets(0, 0, 0, 10));

        Button rightArrow = new Button("›");
        rightArrow.setStyle("-fx-background-color: rgba(30, 31, 34, 0.7); -fx-text-fill: #DFE1E5; -fx-font-size: 18px; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 2 10;");
        rightArrow.setOnAction(e -> {
            currentCarouselIndex = (currentCarouselIndex + 1) % slides.size();
            refreshDetailPanel();
        });
        StackPane.setAlignment(rightArrow, Pos.CENTER_RIGHT);
        StackPane.setMargin(rightArrow, new Insets(0, 10, 0, 0));

        // Dot indicators
        HBox dotsBox = new HBox(6);
        dotsBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < slides.size(); i++) {
            final int idx = i;
            Label dot = new Label(i == currentCarouselIndex ? "●" : "○");
            dot.setStyle(i == currentCarouselIndex
                    ? "-fx-text-fill: #DFE1E5; -fx-font-size: 10px; -fx-cursor: hand;"
                    : "-fx-text-fill: #6F737A; -fx-font-size: 10px; -fx-cursor: hand;");
            dot.setOnMouseClicked(e -> {
                currentCarouselIndex = idx;
                refreshDetailPanel();
            });
            dotsBox.getChildren().add(dot);
        }
        StackPane.setAlignment(dotsBox, Pos.BOTTOM_CENTER);
        StackPane.setMargin(dotsBox, new Insets(0, 0, 8, 0));

        banner.getChildren().addAll(slideContent, leftArrow, rightArrow, dotsBox);
        return banner;
    }

    // =========================================================================
    // Sub-Tabs: What's New, Reviews, Additional Info
    // =========================================================================

    private Node buildWhatsNewContent(PluginItem p) {
        VBox box = new VBox(10);
        Label heading = new Label("What's New in Version " + p.getVersion());
        heading.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        String notes = p.getWhatsNew() != null ? p.getWhatsNew()
                : "• Performance improvements and bug fixes.\n• Compatibility with Lumina IDE platform 2024.1+.\n• Memory optimization for high-throughput language servers.";

        Label body = new Label(notes);
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: #B9BECF; -fx-font-size: 12px; -fx-line-spacing: 4px;");

        box.getChildren().addAll(heading, body);
        return box;
    }

    private Node buildReviewsContent(PluginItem p) {
        VBox box = new VBox(12);

        HBox summary = new HBox(12);
        summary.setAlignment(Pos.CENTER_LEFT);

        Label score = new Label(p.getRating());
        score.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label stars = new Label("★★★★☆");
        stars.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 16px;");

        Button writeReviewBtn = new Button("Write Review");
        writeReviewBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-background-radius: 4; -fx-padding: 4 12;");

        summary.getChildren().addAll(score, stars, new Region(), writeReviewBtn);
        HBox.setHgrow(summary.getChildren().get(2), Priority.ALWAYS);

        box.getChildren().add(summary);

        for (PluginItem.ReviewItem r : p.getReviews()) {
            VBox rBox = new VBox(4);
            rBox.setPadding(new Insets(8));
            rBox.setStyle("-fx-background-color: #2B2D30; -fx-background-radius: 4;");

            HBox rHeader = new HBox(8);
            Label author = new Label(r.getAuthor());
            author.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px;");
            Label date = new Label(r.getDate());
            date.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");
            rHeader.getChildren().addAll(author, date);

            Label comment = new Label(r.getComment());
            comment.setWrapText(true);
            comment.setStyle("-fx-text-fill: #B9BECF; -fx-font-size: 12px;");

            rBox.getChildren().addAll(rHeader, comment);
            box.getChildren().add(rBox);
        }

        return box;
    }

    private Node buildAdditionalInfoContent(PluginItem p) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setPadding(new Insets(8, 0, 8, 0));

        addInfoRow(grid, 0, "Downloads:", p.getDownloads());
        addInfoRow(grid, 1, "Size:", p.getSize());
        addInfoRow(grid, 2, "Vendor:", p.getVendor());
        addInfoRow(grid, 3, "License:", p.getLicense());
        addInfoRow(grid, 4, "Compatible with:", p.getCompatibleVersions());

        Hyperlink reportLink = new Hyperlink("Report abuse or security vulnerability ↗");
        reportLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");

        VBox box = new VBox(12, grid, reportLink);
        return box;
    }

    private void addInfoRow(GridPane grid, int row, String label, String val) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        Label v = new Label(val);
        v.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    // =========================================================================
    // Reactive Refresh & Public API
    // =========================================================================

    public void refreshUI() {
        Platform.runLater(() -> {
            updateTabButtonStyles();
            refreshMasterList();
        });
    }

    public void selectMarketplaceTab() {
        currentTab = TabMode.MARKETPLACE;
        marketplaceTabBtn.setSelected(true);
        updateTabButtonStyles();
        filterDotsBtn.setVisible(false);
        refreshMasterList();
    }

    public void selectInstalledTab() {
        currentTab = TabMode.INSTALLED;
        installedTabBtn.setSelected(true);
        updateTabButtonStyles();
        filterDotsBtn.setVisible(true);
        refreshMasterList();
    }

    public PluginItem getSelectedPlugin() {
        return selectedPlugin;
    }

    public void selectPlugin(String pluginId) {
        for (PluginItem p : pluginManager.getInstalledPlugins()) {
            if (p.getId().equals(pluginId)) {
                selectedPlugin = p;
                selectInstalledTab();
                return;
            }
        }
        for (PluginItem p : pluginManager.getMarketplacePlugins()) {
            if (p.getId().equals(pluginId)) {
                selectedPlugin = p;
                selectMarketplaceTab();
                return;
            }
        }
    }
}
