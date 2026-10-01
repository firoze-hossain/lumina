package dev.lumina.ui;

import dev.lumina.inspections.*;
import dev.lumina.scope.NamedScope;
import dev.lumina.scope.ScopeManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;

/**
 * Lumina IDE Editor > Inspections settings page.
 * Provides dynamic, non-hardcoded inspection profile management, category tree with
 * tri-state checkboxes, severity icons in tree cells, search filtering, bulk editing,
 * and scope/severity customization matching the reference IDE design.
 */
public class SettingsInspectionsPage extends VBox {

    // Tree node types
    public static class InspectionTreeNode {
        private final String category;
        private final String displayName;
        private final InspectionTool tool;

        public InspectionTreeNode(String category) {
            this.category = category;
            int slash = category != null ? category.lastIndexOf('/') : -1;
            this.displayName = (slash >= 0 && slash < category.length() - 1) ? category.substring(slash + 1) : (category != null ? category : "");
            this.tool = null;
        }

        public InspectionTreeNode(InspectionTool tool) {
            this.category = tool != null ? tool.getGroupPath() : null;
            this.tool = tool;
            this.displayName = tool != null ? tool.getDisplayName() : "";
        }

        public boolean isCategory() {
            return tool == null;
        }

        public String getCategory() {
            return category;
        }

        public InspectionTool getTool() {
            return tool;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    // Profile entry for grouped combo box rendering
    public static class ProfileItem {
        private final boolean isHeader;
        private final String headerTitle;
        private final InspectionProfile profile;

        public ProfileItem(String headerTitle) {
            this.isHeader = true;
            this.headerTitle = headerTitle;
            this.profile = null;
        }

        public ProfileItem(InspectionProfile profile) {
            this.isHeader = false;
            this.headerTitle = null;
            this.profile = profile;
        }

        public boolean isHeader() {
            return isHeader;
        }

        public String getHeaderTitle() {
            return headerTitle;
        }

        public InspectionProfile getProfile() {
            return profile;
        }

        @Override
        public String toString() {
            return isHeader ? headerTitle : (profile != null ? profile.getName() : "");
        }
    }

    private final InspectionRegistry registry = InspectionRegistry.getInstance();
    private final InspectionProfileManager profileManager = InspectionProfileManager.getInstance();

    // Working profile (isolated for dialog edit/apply/cancel lifecycle)
    private InspectionProfile workingProfile;

    // UI Controls
    private final ComboBox<ProfileItem> profileCombo = new ComboBox<>();
    private final MenuButton profileGearButton = new MenuButton();
    private final TextField searchField = new TextField();
    private final MenuButton filterMenuButton = new MenuButton();
    private final Button expandAllBtn = new Button();
    private final Button collapseAllBtn = new Button();
    private final Button resetDiffBtn = new Button();
    private final MenuButton addMenuButton = new MenuButton("+");
    private final Button removeBtn = new Button("-");

    private final TreeView<InspectionTreeNode> treeView = new TreeView<>();
    private final TreeItem<InspectionTreeNode> rootItem = new TreeItem<>(new InspectionTreeNode("Root"));
    private final CheckBox disableNewInspectionsCheck = new CheckBox("Disable new inspections by default");

    // Right details panel
    private final VBox detailsContainer = new VBox();
    private final Label multiSelectionLabel = new Label("Multiple inspections are selected. You can edit them as a single inspection.");
    private final VBox singleInspectionBox = new VBox();
    private final Label toolTitleLabel = new Label();
    private final Label toolCategoryLabel = new Label();
    private final TextArea toolDescArea = new TextArea();
    private final VBox optionsContainer = new VBox(6);

    // Bottom controls
    private final MenuButton scopeButton = new MenuButton("In All Scopes");
    private final MenuButton severityButton = new MenuButton();
    private final ComboBox<String> highlightingCombo = new ComboBox<>();

    // Filter states
    private boolean filterModifiedOnly = false;
    private boolean filterEnabledOnly = false;
    private boolean filterDisabledOnly = false;
    private boolean filterBatchModeOnly = false;
    private boolean filterCleanupOnly = false;
    private boolean filterNewInspectionsOnly = false;
    private HighlightSeverity filterSeverity = null;
    private String filterLanguage = null;

    private Runnable onModifiedListener;

    public SettingsInspectionsPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(10, 16, 12, 16));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        // Initialize working copy from active profile
        initWorkingProfile();

        // 1. Breadcrumb row
        HBox breadcrumbRow = buildBreadcrumbRow();

        // 2. Profile selection row
        HBox profileRow = buildProfileRow();

        // 3. Main SplitPane (Left: Tree, Right: Details)
        SplitPane splitPane = buildMainSplitPane();
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        getChildren().addAll(breadcrumbRow, profileRow, splitPane);

        // Populate tree
        refreshTree();

        // Select initial item
        selectInitialCategory("Application servers");
    }

    private void initWorkingProfile() {
        InspectionProfile active = profileManager.getActiveProfile();
        this.workingProfile = active.cloneProfile(active.getName(), active.isProjectLevel());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public boolean isModified() {
        if (workingProfile == null) return false;
        InspectionProfile active = profileManager.getActiveProfile();
        if (workingProfile.isDisableNewInspections() != active.isDisableNewInspections()) return true;
        for (InspectionTool tool : registry.getAllTools()) {
            boolean wEnabled = workingProfile.isEnabled(tool);
            boolean aEnabled = active.isEnabled(tool);
            if (wEnabled != aEnabled) return true;

            HighlightSeverity wSev = workingProfile.getSeverity(tool);
            HighlightSeverity aSev = active.getSeverity(tool);
            if (wSev != aSev) return true;

            String wScope = workingProfile.getScope(tool);
            String aScope = active.getScope(tool);
            if (!Objects.equals(wScope, aScope)) return true;

            String wHigh = workingProfile.getHighlighting(tool);
            String aHigh = active.getHighlighting(tool);
            if (!Objects.equals(wHigh, aHigh)) return true;

            if (tool.hasOptions()) {
                for (InspectionTool.Option opt : tool.getOptions()) {
                    boolean wVal = workingProfile.getOptionBoolean(tool.getId(), opt.getId());
                    boolean aVal = active.getOptionBoolean(tool.getId(), opt.getId());
                    if (wVal != aVal) return true;
                }
            }
        }
        return false;
    }

    public void apply() {
        if (workingProfile == null) return;
        InspectionProfile active = profileManager.getActiveProfile();
        active.copyFrom(workingProfile);
        profileManager.save();
    }

    public void reset() {
        initWorkingProfile();
        refreshTree();
        selectInitialCategory("Application servers");
    }

    private HBox buildBreadcrumbRow() {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(2, 0, 4, 0));

        Label breadcrumb = new Label("Editor › Inspections");
        breadcrumb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Button copyBtn = new Button();
        copyBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 4 2 4;");
        SVGPath copyIcon = new SVGPath();
        copyIcon.setContent("M 2 2 H 8 V 8 H 2 Z M 5 5 H 11 V 11 H 5 Z");
        copyIcon.setStroke(Color.web("#868A91"));
        copyIcon.setFill(null);
        copyIcon.setStrokeWidth(1.2);
        copyBtn.setGraphic(copyIcon);
        copyBtn.setTooltip(new Tooltip("Copy Path"));
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString("Editor | Inspections");
            clipboard.setContent(content);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button("←");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        Button forwardBtn = new Button("→");
        forwardBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5A5D63; -fx-font-size: 13px; -fx-padding: 2 6 2 6;");
        forwardBtn.setDisable(true);

        box.getChildren().addAll(breadcrumb, copyBtn, spacer, backBtn, forwardBtn);
        return box;
    }

    private HBox buildProfileRow() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 0, 8, 0));

        Label profileLabel = new Label("Profile:");
        profileLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        populateProfileComboItems();
        profileCombo.setPrefWidth(220);
        profileCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: #DFE1E5;");

        profileCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProfileItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setDisable(false);
                    setStyle("-fx-background-color: transparent;");
                } else if (item.isHeader()) {
                    setText(item.getHeaderTitle());
                    setGraphic(null);
                    setDisable(true);
                    setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8 2 8; -fx-opacity: 0.9;");
                } else {
                    setText(item.getProfile().getName());
                    setGraphic(null);
                    setDisable(false);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 12 3 12;");
                }
            }
        });

        profileCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ProfileItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getProfile() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    InspectionProfile p = item.getProfile();
                    setText(p.getName() + "  " + (p.isProjectLevel() ? "Project" : "IDE"));
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                }
            }
        });

        profileCombo.setOnAction(e -> {
            ProfileItem selected = profileCombo.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.isHeader() && selected.getProfile() != null) {
                InspectionProfile p = selected.getProfile();
                this.workingProfile = p.cloneProfile(p.getName(), p.isProjectLevel());
                refreshTree();
                fireModified();
            }
        });

        // Gear button with profile actions
        profileGearButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6 4 6;");
        SVGPath gearIcon = new SVGPath();
        gearIcon.setContent("M 6 0 L 7 2 L 9 2 L 9.5 4 L 11 5 L 10 7 L 11 9 L 9.5 10 L 9 12 L 7 12 L 6 10 L 4 10 L 3.5 12 L 2 11 L 3 9 L 2 7 L 3.5 6 L 4 4 L 6 4 Z");
        gearIcon.setStroke(Color.web("#DFE1E5"));
        gearIcon.setFill(Color.web("#868A91"));
        gearIcon.setScaleX(0.9);
        gearIcon.setScaleY(0.9);
        profileGearButton.setGraphic(gearIcon);

        MenuItem copyItem = new MenuItem("Copy...");
        copyItem.setOnAction(e -> handleCopyProfile());

        MenuItem renameItem = new MenuItem("Rename...");
        renameItem.setOnAction(e -> handleRenameProfile());

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> handleDeleteProfile());

        MenuItem exportItem = new MenuItem("Export...");
        MenuItem importItem = new MenuItem("Import...");

        MenuItem resetDefaultItem = new MenuItem("Reset to Default");
        resetDefaultItem.setOnAction(e -> {
            workingProfile.resetToDefaults(registry);
            refreshTree();
            fireModified();
        });

        profileGearButton.getItems().addAll(
                copyItem, renameItem, deleteItem,
                new SeparatorMenuItem(),
                exportItem, importItem,
                new SeparatorMenuItem(),
                resetDefaultItem
        );

        box.getChildren().addAll(profileLabel, profileCombo, profileGearButton);
        return box;
    }

    private void populateProfileComboItems() {
        List<ProfileItem> items = new ArrayList<>();
        List<InspectionProfile> allProfiles = profileManager.getProfiles();

        // 1. Stored in Project
        items.add(new ProfileItem("Stored in Project"));
        ProfileItem selectedItem = null;
        for (InspectionProfile p : allProfiles) {
            if (p.isProjectLevel()) {
                ProfileItem pi = new ProfileItem(p);
                items.add(pi);
                if (workingProfile != null && p.getName().equals(workingProfile.getName())) {
                    selectedItem = pi;
                }
            }
        }

        // 2. Stored in IDE
        items.add(new ProfileItem("Stored in IDE"));
        for (InspectionProfile p : allProfiles) {
            if (!p.isProjectLevel()) {
                ProfileItem pi = new ProfileItem(p);
                items.add(pi);
                if (workingProfile != null && p.getName().equals(workingProfile.getName()) && selectedItem == null) {
                    selectedItem = pi;
                }
            }
        }

        profileCombo.getItems().setAll(items);
        if (selectedItem != null) {
            profileCombo.getSelectionModel().select(selectedItem);
        } else if (!items.isEmpty()) {
            for (ProfileItem pi : items) {
                if (!pi.isHeader()) {
                    profileCombo.getSelectionModel().select(pi);
                    break;
                }
            }
        }
    }

    private void handleCopyProfile() {
        TextInputDialog dialog = new TextInputDialog(workingProfile.getName() + " Copy");
        dialog.setTitle("Copy Profile");
        dialog.setHeaderText("Specify new inspection profile name:");
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.isBlank()) {
                InspectionProfile created = profileManager.createProfile(newName.trim(), workingProfile.isProjectLevel(), workingProfile);
                this.workingProfile = created.cloneProfile(created.getName(), created.isProjectLevel());
                populateProfileComboItems();
                refreshTree();
                fireModified();
            }
        });
    }

    private void handleRenameProfile() {
        TextInputDialog dialog = new TextInputDialog(workingProfile.getName());
        dialog.setTitle("Rename Profile");
        dialog.setHeaderText("Specify new profile name:");
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.isBlank() && !newName.equals(workingProfile.getName())) {
                profileManager.renameProfile(workingProfile.getName(), newName.trim());
                workingProfile.setName(newName.trim());
                populateProfileComboItems();
                fireModified();
            }
        });
    }

    private void handleDeleteProfile() {
        if (profileManager.deleteProfile(workingProfile.getName())) {
            InspectionProfile fallback = profileManager.getActiveProfile();
            this.workingProfile = fallback.cloneProfile(fallback.getName(), fallback.isProjectLevel());
            populateProfileComboItems();
            refreshTree();
            fireModified();
        }
    }

    private SplitPane buildMainSplitPane() {
        SplitPane split = new SplitPane();
        split.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");

        // 1. Left container: Toolbar, TreeView, Disable New CheckBox
        VBox leftPane = new VBox(6);
        leftPane.setStyle("-fx-background-color: #1E1F22;");
        leftPane.setPrefWidth(390);
        leftPane.setMinWidth(260);

        HBox toolbar = buildToolbar();
        setupTreeView();
        VBox.setVgrow(treeView, Priority.ALWAYS);

        disableNewInspectionsCheck.setSelected(workingProfile.isDisableNewInspections());
        disableNewInspectionsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        disableNewInspectionsCheck.setPadding(new Insets(6, 4, 4, 4));
        disableNewInspectionsCheck.setOnAction(e -> {
            workingProfile.setDisableNewInspections(disableNewInspectionsCheck.isSelected());
            fireModified();
        });

        leftPane.getChildren().addAll(toolbar, treeView, disableNewInspectionsCheck);

        // 2. Right container: Detail area & Bottom controls
        VBox rightPane = new VBox(12);
        rightPane.setStyle("-fx-background-color: #1E1F22; -fx-padding: 8 12 8 16;");
        rightPane.setPrefWidth(520);
        rightPane.setMinWidth(300);

        setupDetailsPanel();
        VBox.setVgrow(detailsContainer, Priority.ALWAYS);

        HBox bottomControls = buildBottomControls();

        rightPane.getChildren().addAll(detailsContainer, bottomControls, optionsContainer);

        split.getItems().addAll(leftPane, rightPane);
        split.setDividerPositions(0.42);

        return split;
    }

    private HBox buildToolbar() {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(2, 0, 4, 0));

        // Search field
        searchField.setPromptText("Filter inspections");
        searchField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #868A91; -fx-border-color: #43454A; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, oldV, newV) -> filterTree());

        // Filter button matching media_1790778509636.png
        filterMenuButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3 6 3 6;");
        SVGPath filterIcon = new SVGPath();
        filterIcon.setContent("M 1 2 L 11 2 L 7 7 L 7 11 L 5 12 L 5 7 Z");
        filterIcon.setStroke(Color.web("#868A91"));
        filterIcon.setFill(Color.web("#868A91"));
        filterMenuButton.setGraphic(filterIcon);

        buildFilterMenu();

        // Expand All button
        expandAllBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3 5 3 5;");
        SVGPath expandIcon = new SVGPath();
        expandIcon.setContent("M 1 4 L 4 1 L 7 4 M 4 1 L 4 9 M 1 8 L 4 11 L 7 8");
        expandIcon.setStroke(Color.web("#868A91"));
        expandIcon.setStrokeWidth(1.2);
        expandIcon.setFill(null);
        expandAllBtn.setGraphic(expandIcon);
        expandAllBtn.setTooltip(new Tooltip("Expand All"));
        expandAllBtn.setOnAction(e -> expandAll(rootItem, true));

        // Collapse All button
        collapseAllBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3 5 3 5;");
        SVGPath collapseIcon = new SVGPath();
        collapseIcon.setContent("M 1 1 L 4 4 L 7 1 M 4 4 L 4 10 M 1 11 L 4 8 L 7 11");
        collapseIcon.setStroke(Color.web("#868A91"));
        collapseIcon.setStrokeWidth(1.2);
        collapseIcon.setFill(null);
        collapseAllBtn.setGraphic(collapseIcon);
        collapseAllBtn.setTooltip(new Tooltip("Collapse All"));
        collapseAllBtn.setOnAction(e -> expandAll(rootItem, false));

        // Reset Diff button
        resetDiffBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3 5 3 5;");
        SVGPath diffIcon = new SVGPath();
        diffIcon.setContent("M 2 2 H 8 V 8 H 2 Z");
        diffIcon.setStroke(Color.web("#868A91"));
        diffIcon.setFill(null);
        resetDiffBtn.setGraphic(diffIcon);
        resetDiffBtn.setTooltip(new Tooltip("Reset to Default"));
        resetDiffBtn.setOnAction(e -> {
            workingProfile.resetToDefaults(registry);
            refreshTree();
            fireModified();
        });

        // Add MenuButton matching media_1790778520841.png
        addMenuButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        buildAddMenu();

        // Remove button
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        removeBtn.setTooltip(new Tooltip("Remove Custom Inspection"));
        removeBtn.setOnAction(e -> handleRemoveInspection());

        box.getChildren().addAll(searchField, filterMenuButton, expandAllBtn, collapseAllBtn, resetDiffBtn, addMenuButton, removeBtn);
        return box;
    }

    private void buildFilterMenu() {
        filterMenuButton.getItems().clear();

        MenuItem resetFilter = new MenuItem("Reset Filter");
        resetFilter.setOnAction(e -> {
            filterModifiedOnly = false;
            filterEnabledOnly = false;
            filterDisabledOnly = false;
            filterBatchModeOnly = false;
            filterCleanupOnly = false;
            filterNewInspectionsOnly = false;
            filterSeverity = null;
            filterLanguage = null;
            searchField.clear();
            buildFilterMenu();
            filterTree();
        });

        // Show New Inspections item with lightning bolt
        MenuItem showNewInspections = new MenuItem("Show New Inspections in Lumina IDE 2025.3");
        SVGPath bolt = new SVGPath();
        bolt.setContent("M 4 0 L 1 6 L 4 6 L 3 11 L 7 4 L 4 4 Z");
        bolt.setFill(Color.web("#E0AE43"));
        showNewInspections.setGraphic(bolt);
        showNewInspections.setOnAction(e -> {
            filterNewInspectionsOnly = !filterNewInspectionsOnly;
            filterTree();
        });

        CheckMenuItem enabledOnly = new CheckMenuItem("Show only enabled");
        enabledOnly.setSelected(filterEnabledOnly);
        enabledOnly.setOnAction(e -> {
            filterEnabledOnly = enabledOnly.isSelected();
            if (filterEnabledOnly) filterDisabledOnly = false;
            filterTree();
        });

        CheckMenuItem disabledOnly = new CheckMenuItem("Show only disabled");
        disabledOnly.setSelected(filterDisabledOnly);
        disabledOnly.setOnAction(e -> {
            filterDisabledOnly = disabledOnly.isSelected();
            if (filterDisabledOnly) filterEnabledOnly = false;
            filterTree();
        });

        CheckMenuItem modOnly = new CheckMenuItem("Show only modified inspections");
        modOnly.setSelected(filterModifiedOnly);
        modOnly.setOnAction(e -> {
            filterModifiedOnly = modOnly.isSelected();
            filterTree();
        });

        // Severity items
        List<MenuItem> severityItems = new ArrayList<>();
        for (HighlightSeverity s : HighlightSeverity.values()) {
            MenuItem sItem = new MenuItem(s.getDisplayName());
            sItem.setGraphic(s.createIcon(12));
            sItem.setOnAction(e -> {
                filterSeverity = (filterSeverity == s) ? null : s;
                filterTree();
            });
            severityItems.add(sItem);
        }

        // Submenu: Filter by Language
        Menu languageMenu = new Menu("Filter by Language");
        List<String> languages = List.of(
                "Angular", "CSS", "Go", "Gradle", "Groovy", "HTML",
                "Java", "JavaScript and TypeScript", "JSON", "Kotlin",
                "Markdown", "PHP", "Python", "Rust", "Scala", "SQL",
                "XML", "YAML", "General"
        );
        for (String lang : languages) {
            CheckMenuItem langItem = new CheckMenuItem(lang);
            langItem.setOnAction(e -> {
                filterLanguage = langItem.isSelected() ? lang : null;
                filterTree();
            });
            languageMenu.getItems().add(langItem);
        }

        CheckMenuItem batchOnly = new CheckMenuItem("Show only batch-mode inspections");
        batchOnly.setSelected(filterBatchModeOnly);
        batchOnly.setOnAction(e -> {
            filterBatchModeOnly = batchOnly.isSelected();
            filterTree();
        });

        CheckMenuItem cleanupOnly = new CheckMenuItem("Show only cleanup inspections");
        cleanupOnly.setSelected(filterCleanupOnly);
        cleanupOnly.setOnAction(e -> {
            filterCleanupOnly = cleanupOnly.isSelected();
            filterTree();
        });

        filterMenuButton.getItems().addAll(
                resetFilter,
                new SeparatorMenuItem(),
                showNewInspections,
                new SeparatorMenuItem(),
                enabledOnly, disabledOnly, modOnly,
                new SeparatorMenuItem()
        );
        filterMenuButton.getItems().addAll(severityItems);
        filterMenuButton.getItems().addAll(
                new SeparatorMenuItem(),
                languageMenu,
                new SeparatorMenuItem(),
                batchOnly, cleanupOnly
        );
    }

    private void buildAddMenu() {
        addMenuButton.getItems().clear();

        MenuItem addStructuralSearch = new MenuItem("Add Structural Search Inspection...");
        addStructuralSearch.setOnAction(e -> handleAddUserDefinedInspection("Structural search", "Search"));

        MenuItem addStructuralReplace = new MenuItem("Add Structural Replace Inspection...");
        addStructuralReplace.setOnAction(e -> handleAddUserDefinedInspection("Structural search", "Replace"));

        MenuItem addRegExpSearch = new MenuItem("Add RegExp Search Inspection...");
        addRegExpSearch.setOnAction(e -> handleAddUserDefinedInspection("RegExp", "Search"));

        MenuItem addRegExpReplace = new MenuItem("Add RegExp Replace Inspection...");
        addRegExpReplace.setOnAction(e -> handleAddUserDefinedInspection("RegExp", "Replace"));

        addMenuButton.getItems().addAll(
                addStructuralSearch,
                addStructuralReplace,
                addRegExpSearch,
                addRegExpReplace
        );
    }

    private void handleAddUserDefinedInspection(String group, String type) {
        TextInputDialog dialog = new TextInputDialog("Custom " + group + " " + type);
        dialog.setTitle("Add " + group + " " + type + " Inspection");
        dialog.setHeaderText("Create a user-defined " + group + " inspection rule:");
        dialog.setContentText("Inspection Name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                String id = "UserDefined." + group.replaceAll("\\s+", "") + "." + name.replaceAll("\\s+", "");
                InspectionTool tool = InspectionTool.builder(id)
                        .displayName(name.trim())
                        .groupPath("User defined")
                        .description("Custom user-defined " + group.toLowerCase(Locale.ROOT) + " " + type.toLowerCase(Locale.ROOT) + " inspection rule.")
                        .defaultSeverity(HighlightSeverity.WARNING)
                        .defaultEnabled(true)
                        .build();
                registry.register(tool);
                workingProfile.setEnabled(tool.getId(), true);
                refreshTree();
                fireModified();
            }
        });
    }

    private void handleRemoveInspection() {
        TreeItem<InspectionTreeNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null && selected.getValue().getTool() != null) {
            InspectionTool tool = selected.getValue().getTool();
            if ("User defined".equalsIgnoreCase(tool.getGroupPath())) {
                registry.unregister(tool.getId());
                refreshTree();
                fireModified();
            }
        }
    }

    private void setupTreeView() {
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2B2D30; -fx-border-radius: 4px;");

        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(InspectionTreeNode node, boolean empty) {
                super.updateItem(node, empty);
                if (empty || node == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(6);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-padding: 2 4 2 4;");

                    Label label = new Label(node.getDisplayName());
                    label.setStyle("-fx-text-fill: " + (isSelected() ? "#FFFFFF" : "#DFE1E5") + "; -fx-font-size: 12px;");
                    HBox.setHgrow(label, Priority.ALWAYS);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Node checkGraphic = createCheckGraphic(node);

                    if (!node.isCategory() && node.getTool() != null) {
                        // Leaf inspection item: show severity icon before checkbox matching screenshots
                        InspectionTool tool = node.getTool();
                        boolean enabled = workingProfile.isEnabled(tool);
                        HighlightSeverity sev = workingProfile.getSeverity(tool);
                        Node labelNode = label;
                        if (tool.isBatchModeOnly()) {
                            Label batchSuffix = new Label(" (available for Code | Inspect Code)");
                            batchSuffix.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");
                            HBox labelBox = new HBox(label, batchSuffix);
                            labelBox.setAlignment(Pos.CENTER_LEFT);
                            HBox.setHgrow(labelBox, Priority.ALWAYS);
                            labelNode = labelBox;
                        }
                        if (enabled && (sev == HighlightSeverity.ERROR || sev == HighlightSeverity.WARNING || sev == HighlightSeverity.SERVER_PROBLEM)) {
                            Node sevIcon = sev.createIcon(12);
                            row.getChildren().addAll(labelNode, spacer, sevIcon, checkGraphic);
                        } else {
                            row.getChildren().addAll(labelNode, spacer, checkGraphic);
                        }
                    } else {
                        // Category row: tri-state checkbox only
                        row.getChildren().addAll(label, spacer, checkGraphic);
                    }

                    setGraphic(row);
                    setText(null);

                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-background-radius: 3px;");
                    } else {
                        setStyle("-fx-background-color: transparent;");
                    }
                }
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            updateDetailsPanel();
        });
    }

    /**
     * Creates custom checkbox rendering:
     * - Checked: Blue rounded box with white checkmark
     * - Indeterminate: Blue rounded box with white horizontal minus line
     * - Unchecked: Dark box with border
     */
    private Node createCheckGraphic(InspectionTreeNode node) {
        StackPane box = new StackPane();
        box.setMinSize(14, 14);
        box.setPrefSize(14, 14);
        box.setMaxSize(14, 14);
        box.setCursor(javafx.scene.Cursor.HAND);

        boolean isCat = node.isCategory();
        InspectionProfile.TriState state;
        if (isCat) {
            state = workingProfile.getCategoryState(node.getCategory(), registry);
        } else {
            boolean en = workingProfile.isEnabled(node.getTool());
            state = en ? InspectionProfile.TriState.CHECKED : InspectionProfile.TriState.UNCHECKED;
        }

        Rectangle bg = new Rectangle(14, 14);
        bg.setArcWidth(4);
        bg.setArcHeight(4);

        if (state == InspectionProfile.TriState.CHECKED) {
            bg.setFill(Color.web("#3574F0"));
            bg.setStroke(Color.web("#3574F0"));
            SVGPath check = new SVGPath();
            check.setContent("M 2.5 7 L 5.5 10 L 11.5 3.5");
            check.setStroke(Color.WHITE);
            check.setStrokeWidth(1.8);
            check.setFill(null);
            box.getChildren().addAll(bg, check);
        } else if (state == InspectionProfile.TriState.INDETERMINATE) {
            bg.setFill(Color.web("#3574F0"));
            bg.setStroke(Color.web("#3574F0"));
            Line line = new Line(3, 7, 11, 7);
            line.setStroke(Color.WHITE);
            line.setStrokeWidth(2.0);
            box.getChildren().addAll(bg, line);
        } else {
            bg.setFill(Color.web("#2B2D30"));
            bg.setStroke(Color.web("#5A5D63"));
            bg.setStrokeWidth(1.0);
            box.getChildren().add(bg);
        }

        box.setOnMouseClicked(e -> {
            e.consume();
            if (isCat) {
                boolean nextState = (state == InspectionProfile.TriState.UNCHECKED);
                workingProfile.setCategoryEnabled(node.getCategory(), nextState, registry);
            } else {
                boolean current = workingProfile.isEnabled(node.getTool());
                workingProfile.setEnabled(node.getTool().getId(), !current);
            }
            treeView.refresh();
            updateDetailsPanel();
            fireModified();
        });

        return box;
    }

    private void setupDetailsPanel() {
        detailsContainer.setAlignment(Pos.TOP_LEFT);
        detailsContainer.setSpacing(14);
        detailsContainer.setPadding(new Insets(10, 8, 8, 8));

        multiSelectionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 0 0 0;");
        multiSelectionLabel.setWrapText(true);

        toolDescArea.setEditable(false);
        toolDescArea.setWrapText(true);
        toolDescArea.setStyle("-fx-control-inner-background: #1E1F22; -fx-background-color: transparent; -fx-text-fill: #BCBEC4; -fx-border-color: transparent; -fx-font-size: 13px; -fx-padding: 0;");
        VBox.setVgrow(toolDescArea, Priority.ALWAYS);

        optionsContainer.setAlignment(Pos.TOP_LEFT);
        optionsContainer.setSpacing(6);
        optionsContainer.setPadding(new Insets(6, 0, 4, 0));
        optionsContainer.setVisible(false);
        optionsContainer.setManaged(false);
    }

    private void updateDetailsPanel() {
        ObservableList<TreeItem<InspectionTreeNode>> selectedItems = treeView.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            detailsContainer.getChildren().clear();
            optionsContainer.getChildren().clear();
            optionsContainer.setVisible(false);
            optionsContainer.setManaged(false);
            return;
        }

        List<InspectionTool> selectedTools = new ArrayList<>();
        for (TreeItem<InspectionTreeNode> item : selectedItems) {
            if (item == null || item.getValue() == null) continue;
            if (item.getValue().isCategory()) {
                selectedTools.addAll(registry.getToolsForCategory(item.getValue().getCategory()));
            } else if (item.getValue().getTool() != null) {
                selectedTools.add(item.getValue().getTool());
            }
        }

        TreeItem<InspectionTreeNode> firstItem = (!selectedItems.isEmpty()) ? selectedItems.get(0) : null;
        boolean firstIsCategory = (firstItem != null && firstItem.getValue() != null && firstItem.getValue().isCategory());

        if (selectedTools.size() > 1 || (selectedItems.size() == 1 && firstIsCategory)) {
            multiSelectionLabel.setVisible(true);
            detailsContainer.getChildren().setAll(multiSelectionLabel);
            optionsContainer.getChildren().clear();
            optionsContainer.setVisible(false);
            optionsContainer.setManaged(false);
        } else if (selectedTools.size() == 1) {
            multiSelectionLabel.setVisible(false);
            InspectionTool tool = selectedTools.get(0);
            toolDescArea.setText(tool.getDescription() + "\n\nInspection ID: " + tool.getId());
            detailsContainer.getChildren().setAll(toolDescArea);

            optionsContainer.getChildren().clear();
            if (tool.hasOptions()) {
                Label optionsLabel = new Label("Options");
                optionsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 0 2 0;");
                optionsContainer.getChildren().add(optionsLabel);

                for (InspectionTool.Option opt : tool.getOptions()) {
                    CheckBox optCheck = new CheckBox(opt.getLabel());
                    optCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    optCheck.setSelected(workingProfile.getOptionBoolean(tool.getId(), opt.getId()));
                    optCheck.setOnAction(e -> {
                        workingProfile.setOptionBoolean(tool.getId(), opt.getId(), optCheck.isSelected());
                        fireModified();
                    });
                    optionsContainer.getChildren().add(optCheck);
                }
                optionsContainer.setVisible(true);
                optionsContainer.setManaged(true);
            } else {
                optionsContainer.setVisible(false);
                optionsContainer.setManaged(false);
            }
        } else {
            detailsContainer.getChildren().clear();
            optionsContainer.getChildren().clear();
            optionsContainer.setVisible(false);
            optionsContainer.setManaged(false);
        }

        // Bottom controls update
        if (!selectedTools.isEmpty()) {
            // Check if severities are mixed matching screenshots 4 and 5
            boolean mixedSeverity = false;
            HighlightSeverity firstSev = workingProfile.getSeverity(selectedTools.get(0));
            for (int i = 1; i < selectedTools.size(); i++) {
                if (workingProfile.getSeverity(selectedTools.get(i)) != firstSev) {
                    mixedSeverity = true;
                    break;
                }
            }

            if (mixedSeverity) {
                severityButton.setText(" Mixed");
                severityButton.setGraphic(HighlightSeverity.createMixedIcon(12));
                highlightingCombo.setValue("Mixed");
            } else {
                updateSeverityButton(firstSev);
                String high = workingProfile.getHighlighting(selectedTools.get(0));
                highlightingCombo.setValue(high != null ? high : firstSev.getDisplayName());
            }

            String scope = workingProfile.getScope(selectedTools.get(0));
            scopeButton.setText(scope != null ? scope : "In All Scopes");
        }
    }

    private HBox buildBottomControls() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 0, 4, 0));
        box.setStyle("-fx-border-color: #2B2D30 transparent transparent transparent; -fx-padding: 10 0 0 0;");

        // 1. Scope
        Label scopeLbl = new Label("Scope:");
        scopeLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        scopeButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 12px; -fx-pref-width: 140px;");
        buildScopeMenu();

        VBox scopeGroup = new VBox(4, scopeLbl, scopeButton);

        // 2. Severity
        Label sevLbl = new Label("Severity:");
        sevLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        severityButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 12px; -fx-pref-width: 150px;");
        buildSeverityMenu();

        VBox sevGroup = new VBox(4, sevLbl, severityButton);

        // 3. Highlighting in editor
        Label highLbl = new Label("Highlighting in editor:");
        highLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        highlightingCombo.getItems().addAll(
                "Mixed", "Error", "Warning", "Weak Warning", "Server Problem",
                "Grammar Error", "Typo", "Style Suggestion",
                "Consideration", "No highlighting (fix available)"
        );
        highlightingCombo.setValue("Error");
        highlightingCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 12px;");
        highlightingCombo.setPrefWidth(160);
        highlightingCombo.setOnAction(e -> {
            String val = highlightingCombo.getValue();
            if (val != null && !"Mixed".equals(val)) {
                applyBulkHighlighting(val);
            }
        });

        VBox highGroup = new VBox(4, highLbl, highlightingCombo);

        box.getChildren().addAll(scopeGroup, sevGroup, highGroup);
        return box;
    }

    private void buildScopeMenu() {
        scopeButton.getItems().clear();

        MenuItem header = new MenuItem("Select a Scope to Change Its Settings");
        header.setDisable(true);
        header.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");
        scopeButton.getItems().add(header);

        List<String> scopes = List.of(
                "Project Files",
                "Scratches and Consoles",
                "Production",
                "Tests",
                "Generated Files",
                "Open Files",
                "All Changed Files"
        );

        for (String scopeName : scopes) {
            MenuItem item = new MenuItem(scopeName);
            item.setOnAction(e -> {
                scopeButton.setText(scopeName);
                applyBulkScope(scopeName);
            });
            scopeButton.getItems().add(item);
        }

        scopeButton.getItems().add(new SeparatorMenuItem());

        MenuItem editScopes = new MenuItem("Edit Scopes Order...");
        scopeButton.getItems().add(editScopes);
    }

    private void buildSeverityMenu() {
        severityButton.getItems().clear();

        for (HighlightSeverity s : HighlightSeverity.values()) {
            MenuItem item = new MenuItem(s.getDisplayName());
            item.setGraphic(s.createIcon(12));
            item.setOnAction(e -> {
                updateSeverityButton(s);
                applyBulkSeverity(s);
                highlightingCombo.setValue(s.getDisplayName());
                treeView.refresh();
            });
            severityButton.getItems().add(item);
        }

        severityButton.getItems().add(new SeparatorMenuItem());
        MenuItem editSeverities = new MenuItem("Edit Severities...");
        severityButton.getItems().add(editSeverities);

        updateSeverityButton(HighlightSeverity.ERROR);
    }

    private void updateSeverityButton(HighlightSeverity severity) {
        if (severity == null) severity = HighlightSeverity.WARNING;
        severityButton.setText(" " + severity.getDisplayName());
        severityButton.setGraphic(severity.createIcon(12));
    }

    private void applyBulkSeverity(HighlightSeverity severity) {
        List<String> ids = getSelectedToolIds();
        if (!ids.isEmpty()) {
            workingProfile.setToolsSeverity(ids, severity);
            workingProfile.setToolsHighlighting(ids, severity.getDisplayName());
            fireModified();
            treeView.refresh();
        }
    }

    private void applyBulkScope(String scope) {
        List<String> ids = getSelectedToolIds();
        if (!ids.isEmpty()) {
            workingProfile.setToolsScope(ids, scope);
            fireModified();
        }
    }

    private void applyBulkHighlighting(String highlighting) {
        List<String> ids = getSelectedToolIds();
        if (!ids.isEmpty()) {
            workingProfile.setToolsHighlighting(ids, highlighting);
            fireModified();
        }
    }

    private List<String> getSelectedToolIds() {
        List<String> ids = new ArrayList<>();
        for (TreeItem<InspectionTreeNode> item : treeView.getSelectionModel().getSelectedItems()) {
            if (item == null || item.getValue() == null) continue;
            if (item.getValue().isCategory()) {
                for (InspectionTool tool : registry.getToolsForCategory(item.getValue().getCategory())) {
                    ids.add(tool.getId());
                }
            } else if (item.getValue().getTool() != null) {
                ids.add(item.getValue().getTool().getId());
            }
        }
        return ids;
    }

    private void refreshTree() {
        rootItem.getChildren().clear();
        for (String cat : registry.getAllCategories()) {
            TreeItem<InspectionTreeNode> catItem = buildCategoryNode(cat, "", false);
            if (catItem != null) {
                rootItem.getChildren().add(catItem);
            }
        }
        treeView.refresh();
    }

    private void filterTree() {
        String query = searchField.getText();
        boolean hasQuery = query != null && !query.isBlank();
        String q = hasQuery ? query.trim().toLowerCase(Locale.ROOT) : "";

        rootItem.getChildren().clear();
        for (String cat : registry.getAllCategories()) {
            TreeItem<InspectionTreeNode> catItem = buildCategoryNode(cat, q, hasQuery);
            if (catItem != null) {
                rootItem.getChildren().add(catItem);
            }
        }
        treeView.refresh();
    }

    private TreeItem<InspectionTreeNode> buildCategoryNode(String catPath, String q, boolean hasQuery) {
        List<String> subCats = registry.getAllSubCategories(catPath);
        List<InspectionTool> direct = filterMatchingTools(registry.getToolsDirectlyInCategory(catPath), catPath, q, hasQuery);

        List<TreeItem<InspectionTreeNode>> childNodes = new ArrayList<>();
        for (String subCat : subCats) {
            TreeItem<InspectionTreeNode> subItem = buildCategoryNode(subCat, q, hasQuery);
            if (subItem != null) {
                childNodes.add(subItem);
            }
        }
        for (InspectionTool t : direct) {
            childNodes.add(new TreeItem<>(new InspectionTreeNode(t)));
        }

        if (childNodes.isEmpty()) {
            return null;
        }

        TreeItem<InspectionTreeNode> catItem = new TreeItem<>(new InspectionTreeNode(catPath));
        catItem.getChildren().addAll(childNodes);
        catItem.setExpanded(hasQuery);
        return catItem;
    }

    private List<InspectionTool> filterMatchingTools(List<InspectionTool> tools, String categoryPath, String q, boolean hasQuery) {
        List<InspectionTool> matching = new ArrayList<>();
        for (InspectionTool tool : tools) {
            boolean matchesSearch = !hasQuery ||
                    tool.getDisplayName().toLowerCase(Locale.ROOT).contains(q) ||
                    categoryPath.toLowerCase(Locale.ROOT).contains(q) ||
                    tool.getDescription().toLowerCase(Locale.ROOT).contains(q);

            boolean matchesModified = !filterModifiedOnly ||
                    (workingProfile.isEnabled(tool) != tool.isDefaultEnabled() ||
                            workingProfile.getSeverity(tool) != tool.getDefaultSeverity());

            boolean matchesEnabled = !filterEnabledOnly || workingProfile.isEnabled(tool);
            boolean matchesDisabled = !filterDisabledOnly || !workingProfile.isEnabled(tool);
            boolean matchesBatch = !filterBatchModeOnly || tool.isBatchModeOnly();
            boolean matchesCleanup = !filterCleanupOnly || tool.isCleanupTool();
            boolean matchesSeverity = filterSeverity == null || workingProfile.getSeverity(tool) == filterSeverity;
            boolean matchesLang = filterLanguage == null || (tool.getLanguage() != null && tool.getLanguage().equalsIgnoreCase(filterLanguage));

            if (matchesSearch && matchesModified && matchesEnabled && matchesDisabled &&
                    matchesBatch && matchesCleanup && matchesSeverity && matchesLang) {
                matching.add(tool);
            }
        }
        return matching;
    }

    private void expandAll(TreeItem<?> item, boolean expand) {
        if (item == null) return;
        item.setExpanded(expand);
        for (TreeItem<?> child : item.getChildren()) {
            expandAll(child, expand);
        }
    }

    private void selectInitialCategory(String categoryName) {
        for (TreeItem<InspectionTreeNode> item : rootItem.getChildren()) {
            if (item.getValue() != null && categoryName.equalsIgnoreCase(item.getValue().getCategory())) {
                treeView.getSelectionModel().select(item);
                treeView.scrollTo(treeView.getRow(item));
                updateDetailsPanel();
                break;
            }
        }
    }

    // Getters for UI and testing access
    public TreeView<InspectionTreeNode> getTreeView() {
        return treeView;
    }

    public ComboBox<ProfileItem> getProfileCombo() {
        return profileCombo;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public InspectionProfile getWorkingProfile() {
        return workingProfile;
    }

    public MenuButton getScopeButton() {
        return scopeButton;
    }

    public MenuButton getSeverityButton() {
        return severityButton;
    }

    public ComboBox<String> getHighlightingCombo() {
        return highlightingCombo;
    }

    public CheckBox getDisableNewInspectionsCheck() {
        return disableNewInspectionsCheck;
    }

    public MenuButton getAddMenuButton() {
        return addMenuButton;
    }

    public MenuButton getFilterMenuButton() {
        return filterMenuButton;
    }

    public VBox getOptionsContainer() {
        return optionsContainer;
    }

    public Label getMultiSelectionLabel() {
        return multiSelectionLabel;
    }

    public TextArea getToolDescArea() {
        return toolDescArea;
    }
}