package dev.lumina.ui;

import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * IntelliJ-style Settings dialog — exactly matching the two screenshots.
 * Left: category tree. Right: the Appearance panel with all controls.
 */
public class SettingsDialog {

    private final Stage stage;
    private final SettingsPage currentPage = new SettingsPage();

    public SettingsDialog(Stage owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Settings");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "settings-dialog");

        // ---- Left: category tree ----
        TreeView<String> tree = buildCategoryTree();
        tree.setPrefWidth(260);
        tree.setMinWidth(240);
        tree.getStyleClass().add("settings-tree");

        // ---- Right: content panel ----
        currentPage.getStyleClass().add("settings-page");

        // ---- Bottom: buttons ----
        HBox buttons = buildButtonBar();

        root.setLeft(tree);
        root.setCenter(currentPage);
        root.setBottom(buttons);

        // Initial selection: Appearance & Behavior → Appearance
        TreeItem<String> appearanceItem = findItem(tree.getRoot(), "Appearance");
        if (appearanceItem != null) {
            tree.getSelectionModel().select(appearanceItem);
        }

        // When tree selection changes, update the page
        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                currentPage.showPage(selected);
            }
        });

        Scene scene = new Scene(root, 920, 620);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        stage.showAndWait();
    }

    // --------------------------------------------------- category tree

    private TreeView<String> buildCategoryTree() {
        TreeItem<String> root = new TreeItem<>("Settings");
        root.setExpanded(true);

        // Appearance & Behavior
        TreeItem<String> appearance = new TreeItem<>("Appearance & Behavior");

        TreeItem<String> appearanceSub = new TreeItem<>("Appearance");
        TreeItem<String> menus = new TreeItem<>("Menus and Toolbars");

        // System Settings with all sub-pages
        TreeItem<String> system = new TreeItem<>("System Settings");
        TreeItem<String> dataSharing = new TreeItem<>("Data Sharing");
        TreeItem<String> dateFormats = new TreeItem<>("Date Formats");
        TreeItem<String> httpProxy = new TreeItem<>("HTTP Proxy");
        TreeItem<String> languageRegion = new TreeItem<>("Language and Region");
        TreeItem<String> passwords = new TreeItem<>("Passwords");
        TreeItem<String> processElevation = new TreeItem<>("Process Elevation");
        TreeItem<String> serverCertificates = new TreeItem<>("Server Certificates");
        TreeItem<String> trustedHosts = new TreeItem<>("Trusted Hosts");
        TreeItem<String> updates = new TreeItem<>("Updates");
        system.getChildren().addAll(
                dataSharing, dateFormats, httpProxy, languageRegion,
                passwords, processElevation, serverCertificates,
                trustedHosts, updates
        );

        TreeItem<String> fileColors = new TreeItem<>("File Colors");
        TreeItem<String> scopes = new TreeItem<>("Scopes");
        TreeItem<String> notifications = new TreeItem<>("Notifications");
        TreeItem<String> dataEditor = new TreeItem<>("Data Editor and Viewer");
        TreeItem<String> quickLists = new TreeItem<>("Quick Lists");
        TreeItem<String> requiredPlugins = new TreeItem<>("Required Plugins");
        TreeItem<String> trustedLocations = new TreeItem<>("Trusted Locations");
        TreeItem<String> pathVariables = new TreeItem<>("Path Variables");
        TreeItem<String> presentationAssistant = new TreeItem<>("Presentation Assistant");

        appearance.getChildren().addAll(
                appearanceSub, menus, system, fileColors, scopes,
                notifications, dataEditor, quickLists, requiredPlugins,
                trustedLocations, pathVariables, presentationAssistant
        );

        // Keymap
        TreeItem<String> keymap = new TreeItem<>("Keymap");
//        keymap.getChildren().addAll(
//                new TreeItem<>("Editor"),
//                new TreeItem<>("Plugins"),
//                new TreeItem<>("Version Control")
//        );

        // Editor
        TreeItem<String> editor = new TreeItem<>("Editor");
        TreeItem<String> general = new TreeItem<>("General");
        general.getChildren().addAll(
                new TreeItem<>("Auto Import"),
                new TreeItem<>("Appearance"),
                new TreeItem<>("Breadcrumbs"),
                new TreeItem<>("Code Completion"),
                new TreeItem<>("Code Folding"),
                new TreeItem<>("Console"),
                new TreeItem<>("Editor Tabs"),
                new TreeItem<>("Gutter Icons"),
                new TreeItem<>("Inline Completion"),
                new TreeItem<>("Postfix Completion"),
               // new TreeItem<>("Smart Keys"),
                new TreeItem<>("Sticky Lines")

        );
        // 🔴 ADD: Smart Keys with sub-items
        TreeItem<String> smartKeys = new TreeItem<>("Smart Keys");
        smartKeys.getChildren().addAll(
                new TreeItem<>("YAML"),
                new TreeItem<>("HTML/CSS"),
                new TreeItem<>("JSON"),
                new TreeItem<>("Rust"),
                new TreeItem<>("Markdown"),
                new TreeItem<>("SQL"),
                new TreeItem<>("JavaScript")
        );

// Add Smart Keys to General
        general.getChildren().add(smartKeys);
        // Build a Color Scheme node with sub-pages so it is expandable in the left tree
        TreeItem<String> colorSchemeNode = new TreeItem<>("Color Scheme");
        colorSchemeNode.getChildren().addAll(
            new TreeItem<>("General"),
            new TreeItem<>("Language Defaults"),
            new TreeItem<>("Color Scheme Font"),
            new TreeItem<>("Console Font"),
            new TreeItem<>("Code With Me"),
            new TreeItem<>("Console Colors"),
            new TreeItem<>("Debugger"),
            new TreeItem<>("Diff & Merge"),
            new TreeItem<>("JVM Logging"),
            new TreeItem<>("User-Defined File Types"),
            new TreeItem<>("VCS"),
            new TreeItem<>("Java"),
            new TreeItem<>("Angular Template"),
            new TreeItem<>("Context Free Grammar"),
            new TreeItem<>("CSS"),
            new TreeItem<>("Data Editor and Viewer"),
            new TreeItem<>("Database"),
            new TreeItem<>("Diagrams"),
            new TreeItem<>("Dockerfile"),
            new TreeItem<>("EditorConfig"),
            new TreeItem<>("FreeMarker"),
            new TreeItem<>("GitLab CI Expression"),
            new TreeItem<>("Gradle Declarative Configuration"),
            new TreeItem<>("Groovy"),
            new TreeItem<>("HTML"),
            new TreeItem<>("HTTP Request"),
            new TreeItem<>("JavaScript"),
            new TreeItem<>("JPA/Hibernate QL"),
            new TreeItem<>("JSON"),
            new TreeItem<>("JSONPath"),
            new TreeItem<>("JSP"),
            new TreeItem<>("Jupyter Notebooks"),
            new TreeItem<>("Kotlin"),
            new TreeItem<>("Kubernetes"),
            new TreeItem<>("Less"),
            new TreeItem<>("Lombok Config"),
            new TreeItem<>("Markdown"),
            new TreeItem<>("Micronaut EL"),
            new TreeItem<>("MongoDB JSON"),
            new TreeItem<>("PostCSS"),
            new TreeItem<>("Properties"),
            new TreeItem<>("Protocol Buffer"),
            new TreeItem<>("Protocol Buffer Text"),
            new TreeItem<>("Qute"),
            new TreeItem<>("RegExp"),
            new TreeItem<>("Rust"),
            new TreeItem<>("Sass/SCSS"),
            new TreeItem<>("Shell Script"),
            new TreeItem<>("Spring EL"),
            new TreeItem<>("SQL"),
            new TreeItem<>("Table Diff"),
            new TreeItem<>("TOML"),
            new TreeItem<>("TypeScript"),
            new TreeItem<>("Velocity"),
            new TreeItem<>("XML"),
            new TreeItem<>("XPath"),
            new TreeItem<>("XSLT"),
            new TreeItem<>("YAML"),
            new TreeItem<>("By Scope"),
            new TreeItem<>("Images")
        );

        editor.getChildren().addAll(
            general,
            new TreeItem<>("Code Editing"),
            new TreeItem<>("Font"),
            colorSchemeNode,
            new TreeItem<>("Code Style"),
            new TreeItem<>("Inspections"),
            new TreeItem<>("File and Code Templates"),
            new TreeItem<>("File Encodings"),
            new TreeItem<>("Live Templates"),
            new TreeItem<>("File Types"),
            new TreeItem<>("Copyright"),
            new TreeItem<>("Inlay Hints"),
            new TreeItem<>("Duplicates"),
            new TreeItem<>("Emmet"),
            new TreeItem<>("Intentions"),
            new TreeItem<>("Language Injections"),
            new TreeItem<>("Natural Languages"),
            new TreeItem<>("Reader Mode"),
            new TreeItem<>("TextMate Bundles"),
            new TreeItem<>("TODO")
        );
        // Plugins
        TreeItem<String> plugins = new TreeItem<>("Plugins");

        // Version Control
        TreeItem<String> versionControl = new TreeItem<>("Version Control");

        // Build, Execution, Deployment
        TreeItem<String> build = new TreeItem<>("Build, Execution, Deployment");

        // Languages & Frameworks
        TreeItem<String> languages = new TreeItem<>("Languages & Frameworks");

        // Tools
        TreeItem<String> tools = new TreeItem<>("Tools");

        // Backup and Sync
        TreeItem<String> backup = new TreeItem<>("Backup and Sync");

        // Advanced Settings
        TreeItem<String> advanced = new TreeItem<>("Advanced Settings");

        root.getChildren().addAll(appearance, keymap, editor, plugins, versionControl,
                build, languages, tools, backup, advanced);

        TreeView<String> tree = new TreeView<>(root);
        tree.setShowRoot(false);
        tree.getStyleClass().add("settings-tree");

        return tree;
    }

    private TreeItem<String> findItem(TreeItem<String> root, String text) {
        if (root.getValue() != null && root.getValue().equals(text)) {
            return root;
        }
        for (TreeItem<String> child : root.getChildren()) {
            TreeItem<String> found = findItem(child, text);
            if (found != null) return found;
        }
        return null;
    }

    // --------------------------------------------------- button bar

    private HBox buildButtonBar() {
        Button ok = new Button("OK");
        ok.getStyleClass().add("dialog-primary");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            currentPage.save();
            stage.close();
        });

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> stage.close());

        Button apply = new Button("Apply");
        apply.getStyleClass().add("dialog-secondary");
        apply.setOnAction(e -> currentPage.save());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, spacer, apply, cancel, ok);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(12, 20, 14, 20));
        bar.getStyleClass().add("dialog-footer");
        return bar;
    }

    // --------------------------------------------------- settings page

    /**
     * The right-hand panel that shows the Appearance & Behavior → Appearance
     * page, exactly as in the two screenshots.
     */
    private static class SettingsPage extends VBox {

        private Label pageTitle;
        private VBox contentArea;

        // ---- Appearance page controls ----
        private CheckBox lighterBackground;
        private Slider zoomSlider;
        private CheckBox customFont;
        private ComboBox<String> fontFamily;
        private Spinner<Integer> fontSize;
        private CheckBox screenReader;
        private CheckBox contrastScrollbars;
        private CheckBox adjustColors;
        private CheckBox simplifiedSplash;
        private CheckBox compactMode;
        private CheckBox fullPathInHeader;
        private CheckBox projectColors;
        private CheckBox keepPopupsOpen;
        private CheckBox hamburgerMenu;
        private Button backgroundImage;
        private CheckBox indentGuides;
        private CheckBox smallerIndents;
        private CheckBox showToolWindowBars;
        private CheckBox showToolWindowNames;
        private CheckBox widescreenLayout;
        private CheckBox sideBySideLeft;
        private CheckBox sideBySideRight;
        private CheckBox rememberSize;
        private Spinner<Integer> presentationZoom;
        private ComboBox<String> ideAntialiasing;
        private ComboBox<String> editorAntialiasing;

        public SettingsPage() {
            getStyleClass().add("settings-page");
            setPadding(new Insets(20, 24, 20, 24));
            setSpacing(16);

            buildAppearancePage();
        }

        private void buildAppearancePage() {
            // ---- Header ----
            pageTitle = new Label("Appearance & Behavior → Appearance");
            pageTitle.getStyleClass().add("settings-page-title");

            // ---- Scrollable content ----
            contentArea = new VBox(18);
            contentArea.getStyleClass().add("settings-content");

            // ---- Appearance section ----
            Label appearanceSection = sectionLabel("Appearance");

            // Editor color scheme
            Label colorSchemeLabel = new Label("Editor color scheme:");
            colorSchemeLabel.getStyleClass().add("settings-label");
            ComboBox<String> colorScheme = new ComboBox<>();
            colorScheme.getItems().addAll("Island's Dark Theme default");
            colorScheme.getSelectionModel().selectFirst();
            colorScheme.getStyleClass().add("settings-combo");

            // Different tool window background
            lighterBackground = new CheckBox("Use lighter color in the dark theme and darker color in the light theme as a background");
            lighterBackground.getStyleClass().add("settings-check");

            VBox colorSchemeBox = new VBox(4, colorSchemeLabel, colorScheme, lighterBackground);

            // ---- Accessibility section ----
            Label accessibilitySection = sectionLabel("Accessibility");

            // Zoom
            Label zoomLabel = new Label("Zoom:");
            zoomLabel.getStyleClass().add("settings-label");
            zoomSlider = new Slider(50, 200, 100);
            zoomSlider.setShowTickLabels(true);
            zoomSlider.setShowTickMarks(true);
            zoomSlider.setMajorTickUnit(25);
            zoomSlider.setBlockIncrement(5);
            zoomSlider.setPrefWidth(240);
            Label zoomValue = new Label("100%");
            zoomValue.getStyleClass().add("settings-value");
            zoomSlider.valueProperty().addListener((obs, old, v) ->
                    zoomValue.setText(String.format("%.0f%%", v)));

            Label zoomHint = new Label("Change with Ctrl+Alt+Shift+ or Ctrl+Alt+Shift+Minus. Set to 100% with Ctrl+Alt+Shift+0");
            zoomHint.getStyleClass().add("settings-hint");

            HBox zoomRow = new HBox(12, zoomLabel, zoomSlider, zoomValue);
            zoomRow.setAlignment(Pos.CENTER_LEFT);

            // Custom font
            customFont = new CheckBox("Use custom font:");
            customFont.getStyleClass().add("settings-check");
            fontFamily = new ComboBox<>();
            fontFamily.getItems().addAll("Inter", "Segoe UI", "SF Pro Text", "JetBrains Mono");
            fontFamily.getSelectionModel().select("Inter");
            fontFamily.setPrefWidth(150);

            Label sizeLabel = new Label("Size:");
            sizeLabel.getStyleClass().add("settings-label");
            fontSize = new Spinner<>(8, 24, 13);
            fontSize.setPrefWidth(70);

            HBox fontRow = new HBox(8, fontFamily, sizeLabel, fontSize);
            fontRow.setAlignment(Pos.CENTER_LEFT);
            VBox fontBox = new VBox(4, customFont, fontRow);

            // Screen readers
            screenReader = new CheckBox("Support screen readers");
            screenReader.getStyleClass().add("settings-check");
            Label readerHint = new Label("Requires restart. Ctrl+Tab and Ctrl+Shift+Tab will navigate UI controls in dialogs and will not be available for switching editor tabs or other IDE actions. Tooltips on mouse hover will be disabled.");
            readerHint.getStyleClass().add("settings-hint");
            readerHint.setWrapText(true);

            // Contrast scrollbars
            contrastScrollbars = new CheckBox("Use contrast scrollbars");
            contrastScrollbars.getStyleClass().add("settings-check");

            // Adjust colors for red-green vision deficiency
            adjustColors = new CheckBox("Adjust colors for red-green vision deficiency");
            adjustColors.getStyleClass().add("settings-check");
            Label adjustHint = new Label("Requires restart. For protanopia and deuteranopia.");
            adjustHint.getStyleClass().add("settings-hint");

            // Simplified splash
            simplifiedSplash = new CheckBox("Use simplified splash screen");
            simplifiedSplash.getStyleClass().add("settings-check");

            VBox accessibilityBox = new VBox(10, zoomRow, zoomHint, fontBox,
                    screenReader, readerHint, contrastScrollbars,
                    adjustColors, adjustHint, simplifiedSplash);

            // ---- UI Options section ----
            Label uiSection = sectionLabel("UI Options");

            compactMode = new CheckBox("Compact mode");
            compactMode.getStyleClass().add("settings-check");
            Label compactHint = new Label("UI elements take up less screen space");
            compactHint.getStyleClass().add("settings-hint");

            fullPathInHeader = new CheckBox("Always show full path in window header");
            fullPathInHeader.getStyleClass().add("settings-check");

            projectColors = new CheckBox("Use project colors in main toolbar");
            projectColors.getStyleClass().add("settings-check");
            Label projectHint = new Label("Distinguish projects with different toolbar colors at a glance.");
            projectHint.getStyleClass().add("settings-hint");

            keepPopupsOpen = new CheckBox("Keep popups open for toggle items");
            keepPopupsOpen.getStyleClass().add("settings-check");

            VBox uiBox = new VBox(8, compactMode, compactHint,
                    fullPathInHeader, projectColors, projectHint,
                    keepPopupsOpen);

            // ---- Main menu section ----
            Label mainMenuSection = sectionLabel("Main menu");

            hamburgerMenu = new CheckBox("Hide under Hamburger Button");
            hamburgerMenu.getStyleClass().add("settings-check");
            Label hamburgerHint = new Label("Requires restart");
            hamburgerHint.getStyleClass().add("settings-hint");

            backgroundImage = new Button("Background Image...");
            backgroundImage.getStyleClass().add("dialog-secondary");

            VBox mainMenuBox = new VBox(6, hamburgerMenu, hamburgerHint, backgroundImage);

            // ---- Tree Views section ----
            Label treeSection = sectionLabel("Tree Views");

            indentGuides = new CheckBox("Show indent guides");
            indentGuides.getStyleClass().add("settings-check");
            smallerIndents = new CheckBox("Use smaller indents");
            smallerIndents.getStyleClass().add("settings-check");

            VBox treeBox = new VBox(6, indentGuides, smallerIndents);

            // ---- Tool Windows section ----
            Label toolWindowsSection = sectionLabel("Tool Windows");

            showToolWindowBars = new CheckBox("Show tool window bars");
            showToolWindowBars.getStyleClass().add("settings-check");
            showToolWindowNames = new CheckBox("Show tool window names");
            showToolWindowNames.getStyleClass().add("settings-check");
            widescreenLayout = new CheckBox("Widescreen tool window layout");
            widescreenLayout.getStyleClass().add("settings-check");
            sideBySideLeft = new CheckBox("Side-by-side layout on the left");
            sideBySideLeft.getStyleClass().add("settings-check");
            sideBySideRight = new CheckBox("Side-by-side layout on the right");
            sideBySideRight.getStyleClass().add("settings-check");
            rememberSize = new CheckBox("Remember size for each tool window");
            rememberSize.getStyleClass().add("settings-check");

            VBox toolWindowsBox = new VBox(6, showToolWindowBars, showToolWindowNames,
                    widescreenLayout, sideBySideLeft, sideBySideRight,
                    rememberSize);

            // ---- Presentation Mode section ----
            Label presentationSection = sectionLabel("Presentation Mode");

            Label presentationZoomLabel = new Label("Zoom:");
            presentationZoomLabel.getStyleClass().add("settings-label");
            presentationZoom = new Spinner<>(100, 300, 175);
            presentationZoom.setPrefWidth(80);

            HBox presentationRow = new HBox(12, presentationZoomLabel, presentationZoom);
            presentationRow.setAlignment(Pos.CENTER_LEFT);

            // ---- Antialiasing section ----
            Label antialiasingSection = sectionLabel("Antialiasing");

            Label ideAA = new Label("IDE:");
            ideAA.getStyleClass().add("settings-label");
            ideAntialiasing = new ComboBox<>();
            ideAntialiasing.getItems().addAll("Subpixel", "Grayscale");
            ideAntialiasing.getSelectionModel().select("Subpixel");

            Label editorAA = new Label("Editor:");
            editorAA.getStyleClass().add("settings-label");
            editorAntialiasing = new ComboBox<>();
            editorAntialiasing.getItems().addAll("Subpixel", "Grayscale");
            editorAntialiasing.getSelectionModel().select("Subpixel");

            HBox aaRow = new HBox(20, ideAA, ideAntialiasing, editorAA, editorAntialiasing);
            aaRow.setAlignment(Pos.CENTER_LEFT);

            VBox aaBox = new VBox(6, aaRow);

            // ---- Assemble the page ----
            contentArea.getChildren().addAll(
                    colorSchemeBox,
                    accessibilitySection,
                    accessibilityBox,
                    uiSection,
                    uiBox,
                    mainMenuSection,
                    mainMenuBox,
                    treeSection,
                    treeBox,
                    toolWindowsSection,
                    toolWindowsBox,
                    presentationSection,
                    presentationRow,
                    antialiasingSection,
                    aaBox
            );

            // ---- ScrollPane wrapper ----
            ScrollPane scroll = new ScrollPane(contentArea);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("settings-scroll");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            VBox.setVgrow(scroll, Priority.ALWAYS);

            getChildren().addAll(pageTitle, scroll);

            // ---- Load saved values ----
            load();
        }

        private Label sectionLabel(String text) {
            Label label = new Label(text);
            label.getStyleClass().add("settings-section");
            return label;
        }

        /**
         * Show a page based on the selected tree item.
         */
//        public void showPage(String pageName) {
//            getChildren().clear();
//
//            if ("Appearance".equals(pageName)) {
//                buildAppearancePage();
//            } else if ("Menus and Toolbars".equals(pageName)) {
//                buildMenusToolbarsPage();
//            } else if ("System Settings".equals(pageName)) {
//                buildSystemSettingsPage();
//            } else if (isSystemSettingsSubPage(pageName)) {
//                buildSystemSettingsSubPage(pageName);
//            }
//            else if ("File Colors".equals(pageName)) {
//                buildFileColorsPage();
//            } else if ("Scopes".equals(pageName)) {
//                buildScopesPage();
//            } else if ("Notifications".equals(pageName)) {
//                buildNotificationsPage();
//            } else if ("Data Editor and Viewer".equals(pageName)) {
//                buildDataEditorPage();
//            } else if ("Quick Lists".equals(pageName)) {
//                buildQuickListsPage();
//            } else if ("Required Plugins".equals(pageName)) {
//                buildRequiredPluginsPage();
//            } else if ("Trusted Locations".equals(pageName)) {
//                buildTrustedLocationsPage();
//            } else if ("Path Variables".equals(pageName)) {
//                buildPathVariablesPage();
//            } else if ("Presentation Assistant".equals(pageName)) {
//                buildPresentationAssistantPage();
//            }
//            else if ("Keymap".equals(pageName) ||
//                    pageName.equals("Editor Actions") ||
//                    pageName.equals("Main Menu") ||
//                    pageName.equals("Tool Windows") ||
//                    pageName.equals("External Tools") ||
//                    pageName.equals("External Build Systems") ||
//                    pageName.equals("Version Control Systems") ||
//                    pageName.equals("Debugger Actions") ||
//                    pageName.equals("Remote External Tools") ||
//                    pageName.equals("Database") ||
//                    pageName.equals("Macros") ||
//                    pageName.equals("Intentions") ||
//                    pageName.equals("Quick Lists") ||
//                    pageName.equals("Plugins") ||
//                    pageName.equals("Other")) {
//                buildKeymapPage();
//            }else if ("Editor".equals(pageName) ||
//                    pageName.equals("General") ||
//                    pageName.equals("Code Editing") ||
//                    pageName.equals("Font") ||
//                    pageName.equals("Color Scheme") ||
//                    pageName.equals("Code Style") ||
//                    pageName.equals("Inspections") ||
//                    pageName.equals("File and Code Templates") ||
//                    pageName.equals("File Encodings") ||
//                    pageName.equals("Live Templates") ||
//                    pageName.equals("File Types") ||
//                    pageName.equals("Copyright") ||
//                    pageName.equals("Inlay Hints") ||
//                    pageName.equals("Duplicates") ||
//                    pageName.equals("Emmet") ||
//                    pageName.equals("Intentions") ||
//                    pageName.equals("Language Injections") ||
//                    pageName.equals("Natural Languages") ||
//                    pageName.equals("Reader Mode") ||
//                    pageName.equals("TextMate Bundles") ||
//                    pageName.equals("TODO")) {
//                // Pass the page name to the editor page
//                buildEditorPage(pageName);
//            }
//            else {
//                // Placeholder for other pages
//                Label title = new Label(pageName);
//                title.getStyleClass().add("settings-page-title");
//
//                Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
//                placeholder.getStyleClass().add("settings-placeholder");
//
//                VBox box = new VBox(20, title, placeholder);
//                box.setPadding(new Insets(40, 24, 20, 24));
//                box.getStyleClass().add("settings-page");
//
//                ScrollPane scroll = new ScrollPane(box);
//                scroll.setFitToWidth(true);
//                scroll.getStyleClass().add("settings-scroll");
//                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
//                VBox.setVgrow(scroll, Priority.ALWAYS);
//
//                getChildren().addAll(scroll);
//            }
//        }
        /**
         * Show a page based on the selected tree item.
         */
//        public void showPage(String pageName) {
//            getChildren().clear();
//
//            if ("Appearance".equals(pageName)) {
//                buildAppearancePage();
//            } else if ("Menus and Toolbars".equals(pageName)) {
//                buildMenusToolbarsPage();
//            } else if ("System Settings".equals(pageName)) {
//                buildSystemSettingsPage();
//            } else if (isSystemSettingsSubPage(pageName)) {
//                buildSystemSettingsSubPage(pageName);
//            } else if ("File Colors".equals(pageName)) {
//                buildFileColorsPage();
//            } else if ("Scopes".equals(pageName)) {
//                buildScopesPage();
//            } else if ("Notifications".equals(pageName)) {
//                buildNotificationsPage();
//            } else if ("Data Editor and Viewer".equals(pageName)) {
//                buildDataEditorPage();
//            } else if ("Quick Lists".equals(pageName)) {
//                buildQuickListsPage();
//            } else if ("Required Plugins".equals(pageName)) {
//                buildRequiredPluginsPage();
//            } else if ("Trusted Locations".equals(pageName)) {
//                buildTrustedLocationsPage();
//            } else if ("Path Variables".equals(pageName)) {
//                buildPathVariablesPage();
//            } else if ("Presentation Assistant".equals(pageName)) {
//                buildPresentationAssistantPage();
//            } else if ("Keymap".equals(pageName) ||
//                    pageName.equals("Editor Actions") ||
//                    pageName.equals("Main Menu") ||
//                    pageName.equals("Tool Windows") ||
//                    pageName.equals("External Tools") ||
//                    pageName.equals("External Build Systems") ||
//                    pageName.equals("Version Control Systems") ||
//                    pageName.equals("Debugger Actions") ||
//                    pageName.equals("Remote External Tools") ||
//                    pageName.equals("Database") ||
//                    pageName.equals("Macros") ||
//                    pageName.equals("Intentions") ||
//                    pageName.equals("Quick Lists") ||
//                    pageName.equals("Plugins") ||
//                    pageName.equals("Other")) {
//                buildKeymapPage();
//            }
//            // 🔴 FIX: Add Smart Keys and all its sub-items here
//            else if ("Editor".equals(pageName) ||
//                    pageName.equals("General") ||
//                    pageName.equals("Auto Import") ||
//                    pageName.equals("Appearance") ||
//                    pageName.equals("Breadcrumbs") ||
//                    pageName.equals("Code Completion") ||
//                    pageName.equals("Code Folding") ||
//                    pageName.equals("Console") ||
//                    pageName.equals("Editor Tabs") ||
//                    pageName.equals("Gutter Icons") ||
//                    pageName.equals("Inline Completion") ||
//                    pageName.equals("Postfix Completion") ||
//                    pageName.equals("Sticky Lines") ||
//                    // 🔴 ADD Smart Keys and sub-items
//                    pageName.equals("Smart Keys") ||
//                    pageName.equals("YAML") ||
//                    pageName.equals("HTML/CSS") ||
//                    pageName.equals("JSON") ||
//                    pageName.equals("Rust") ||
//                    pageName.equals("Markdown") ||
//                    pageName.equals("SQL") ||
//                    pageName.equals("JavaScript") ||
//                    pageName.equals("Code Editing") ||
//                    pageName.equals("Font") ||
//                    pageName.equals("Color Scheme") ||
//                    pageName.equals("Code Style") ||
//                    pageName.equals("Inspections") ||
//                    pageName.equals("File and Code Templates") ||
//                    pageName.equals("File Encodings") ||
//                    pageName.equals("Live Templates") ||
//                    pageName.equals("File Types") ||
//                    pageName.equals("Copyright") ||
//                    pageName.equals("Inlay Hints") ||
//                    pageName.equals("Duplicates") ||
//                    pageName.equals("Emmet") ||
//                    pageName.equals("Intentions") ||
//                    pageName.equals("Language Injections") ||
//                    pageName.equals("Natural Languages") ||
//                    pageName.equals("Reader Mode") ||
//                    pageName.equals("TextMate Bundles") ||
//                    pageName.equals("TODO")) {
//                // Pass the page name to the editor page
//                buildEditorPage(pageName);
//            } else {
//                // Placeholder for other pages
//                Label title = new Label(pageName);
//                title.getStyleClass().add("settings-page-title");
//
//                Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
//                placeholder.getStyleClass().add("settings-placeholder");
//
//                VBox box = new VBox(20, title, placeholder);
//                box.setPadding(new Insets(40, 24, 20, 24));
//                box.getStyleClass().add("settings-page");
//
//                ScrollPane scroll = new ScrollPane(box);
//                scroll.setFitToWidth(true);
//                scroll.getStyleClass().add("settings-scroll");
//                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
//                VBox.setVgrow(scroll, Priority.ALWAYS);
//
//                getChildren().addAll(scroll);
//            }
//        }
        /**
         * Show a page based on the selected tree item.
         */
        public void showPage(TreeItem<String> selected) {
            getChildren().clear();

            String pageName = selected.getValue();

            // Determine ancestor path to disambiguate identical names (e.g. "Appearance")
            TreeItem<String> ancestor = selected.getParent();
            boolean underAppearanceGroup = false;
            boolean underEditorGroup = false;
            while (ancestor != null) {
                String v = ancestor.getValue();
                if (v != null) {
                    if (v.equals("Appearance & Behavior")) underAppearanceGroup = true;
                    if (v.equals("Editor")) underEditorGroup = true;
                }
                ancestor = ancestor.getParent();
            }

            // If the selected node is under the top-level "Appearance & Behavior" group,
            // treat its "Appearance" as the top-level appearance page. Otherwise if it's
            // under "Editor" treat it as an editor sub-page.
            if (underAppearanceGroup && "Appearance".equals(pageName)) {
                buildAppearancePage();
                return;
            }

            if (underEditorGroup || "Editor".equals(pageName) ||
                    pageName.equals("General") ||
                    pageName.equals("Auto Import") ||
                    pageName.equals("Appearance") ||
                    pageName.equals("Breadcrumbs") ||
                    pageName.equals("Code Completion") ||
                    pageName.equals("Code Folding") ||
                    pageName.equals("Console") ||
                    pageName.equals("Editor Tabs") ||
                    pageName.equals("Gutter Icons") ||
                    pageName.equals("Inline Completion") ||
                    pageName.equals("Postfix Completion") ||
                    pageName.equals("Sticky Lines") ||
                    pageName.equals("Smart Keys") ||
                    pageName.equals("YAML") ||
                    pageName.equals("HTML/CSS") ||
                    pageName.equals("JSON") ||
                    pageName.equals("Rust") ||
                    pageName.equals("Markdown") ||
                    pageName.equals("SQL") ||
                    pageName.equals("JavaScript") ||
                    pageName.equals("Code Editing") ||
                    pageName.equals("Font") ||
                    pageName.equals("Color Scheme") ||
                    pageName.equals("Code Style") ||
                    pageName.equals("Inspections") ||
                    pageName.equals("File and Code Templates") ||
                    pageName.equals("File Encodings") ||
                    pageName.equals("Live Templates") ||
                    pageName.equals("File Types") ||
                    pageName.equals("Copyright") ||
                    pageName.equals("Inlay Hints") ||
                    pageName.equals("Duplicates") ||
                    pageName.equals("Emmet") ||
                    pageName.equals("Intentions") ||
                    pageName.equals("Language Injections") ||
                    pageName.equals("Natural Languages") ||
                    pageName.equals("Reader Mode") ||
                    pageName.equals("TextMate Bundles") ||
                    pageName.equals("TODO")) {
                // Pass the page name to the editor page
                buildEditorPage(pageName);
                return;
            }

            if ("Menus and Toolbars".equals(pageName)) {
                buildMenusToolbarsPage();
            } else if ("System Settings".equals(pageName)) {
                buildSystemSettingsPage();
            } else if (isSystemSettingsSubPage(pageName)) {
                buildSystemSettingsSubPage(pageName);
            } else if ("File Colors".equals(pageName)) {
                buildFileColorsPage();
            } else if ("Scopes".equals(pageName)) {
                buildScopesPage();
            } else if ("Notifications".equals(pageName)) {
                buildNotificationsPage();
            } else if ("Data Editor and Viewer".equals(pageName)) {
                buildDataEditorPage();
            } else if ("Quick Lists".equals(pageName)) {
                buildQuickListsPage();
            } else if ("Required Plugins".equals(pageName)) {
                buildRequiredPluginsPage();
            } else if ("Trusted Locations".equals(pageName)) {
                buildTrustedLocationsPage();
            } else if ("Path Variables".equals(pageName)) {
                buildPathVariablesPage();
            } else if ("Presentation Assistant".equals(pageName)) {
                buildPresentationAssistantPage();
            } else if ("Keymap".equals(pageName) ||
                    pageName.equals("Editor Actions") ||
                    pageName.equals("Main Menu") ||
                    pageName.equals("Tool Windows") ||
                    pageName.equals("External Tools") ||
                    pageName.equals("External Build Systems") ||
                    pageName.equals("Version Control Systems") ||
                    pageName.equals("Debugger Actions") ||
                    pageName.equals("Remote External Tools") ||
                    pageName.equals("Database") ||
                    pageName.equals("Macros") ||
                    pageName.equals("Intentions") ||
                    pageName.equals("Quick Lists") ||
                    pageName.equals("Plugins") ||
                    pageName.equals("Other")) {
                buildKeymapPage();
            } else {
                // Placeholder for other pages
                Label title = new Label(pageName);
                title.getStyleClass().add("settings-page-title");

                Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
                placeholder.getStyleClass().add("settings-placeholder");

                VBox box = new VBox(20, title, placeholder);
                box.setPadding(new Insets(40, 24, 20, 24));
                box.getStyleClass().add("settings-page");

                ScrollPane scroll = new ScrollPane(box);
                scroll.setFitToWidth(true);
                scroll.getStyleClass().add("settings-scroll");
                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                VBox.setVgrow(scroll, Priority.ALWAYS);

                getChildren().addAll(scroll);
            }
        }
        private void buildEditorPage() {
            SettingsEditorPage page = new SettingsEditorPage();
            wrapInScroll(page);
        }
        // Update buildEditorPage to accept a page name parameter
        private void buildEditorPage(String pageName) {
            SettingsEditorPage page = new SettingsEditorPage();
            page.showEditorPage(pageName);
            wrapInScroll(page);
        }

        // 🔴 ADD this method after buildPresentationAssistantPage()
        private void buildKeymapPage() {
            SettingsKeymapPage page = new SettingsKeymapPage();
            wrapInScroll(page);
        }

        /**
         * Builds the Menus and Toolbars page.
         */
        private void buildMenusToolbarsPage() {
            SettingsMenusToolbarsPage page = new SettingsMenusToolbarsPage();
            ScrollPane scroll = new ScrollPane(page);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("settings-scroll");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            VBox.setVgrow(scroll, Priority.ALWAYS);
            getChildren().addAll(scroll);
        }
        private void buildFileColorsPage() {
            SettingsFileColorsPage page = new SettingsFileColorsPage();
            wrapInScroll(page);
        }

        private void buildScopesPage() {
            SettingsScopesPage page = new SettingsScopesPage();
            wrapInScroll(page);
        }

        private void buildNotificationsPage() {
            SettingsNotificationsPage page = new SettingsNotificationsPage();
            wrapInScroll(page);
        }

        private void buildDataEditorPage() {
            SettingsDataEditorPage page = new SettingsDataEditorPage();
            wrapInScroll(page);
        }

        private void buildQuickListsPage() {
            SettingsQuickListsPage page = new SettingsQuickListsPage();
            wrapInScroll(page);
        }

        private void buildRequiredPluginsPage() {
            SettingsRequiredPluginsPage page = new SettingsRequiredPluginsPage();
            wrapInScroll(page);
        }

        private void buildTrustedLocationsPage() {
            SettingsTrustedLocationsPage page = new SettingsTrustedLocationsPage();
            wrapInScroll(page);
        }

        private void buildPathVariablesPage() {
            SettingsPathVariablesPage page = new SettingsPathVariablesPage();
            wrapInScroll(page);
        }

        private void buildPresentationAssistantPage() {
            SettingsPresentationAssistantPage page = new SettingsPresentationAssistantPage();
            wrapInScroll(page);
        }

        // 🔴 ADD: Helper method to wrap in scroll pane
//        private void wrapInScroll(VBox page) {
//            ScrollPane scroll = new ScrollPane(page);
//            scroll.setFitToWidth(true);
//            scroll.getStyleClass().add("settings-scroll");
//            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
//            VBox.setVgrow(scroll, Priority.ALWAYS);
//            getChildren().addAll(scroll);
//        }
        private void wrapInScroll(javafx.scene.Node page) {
            ScrollPane scroll = new ScrollPane(page);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("settings-scroll");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            VBox.setVgrow(scroll, Priority.ALWAYS);
            getChildren().addAll(scroll);
        }
        /**
         * Helper method to check if a page is a System Settings sub-page.
         */
        private boolean isSystemSettingsSubPage(String pageName) {
            return List.of(
                    "Data Sharing",
                    "Date Formats",
                    "HTTP Proxy",
                    "Language and Region",
                    "Passwords",
                    "Process Elevation",
                    "Server Certificates",
                    "Trusted Hosts",
                    "Updates"
            ).contains(pageName);
        }

        /**
         * Build the System Settings page (shows Data Sharing by default).
         */
        private void buildSystemSettingsPage() {
            SettingsSystemPage page = new SettingsSystemPage();
            ScrollPane scroll = new ScrollPane(page);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("settings-scroll");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            VBox.setVgrow(scroll, Priority.ALWAYS);
            getChildren().addAll(scroll);
        }

        /**
         * Build a specific System Settings sub-page.
         */
        private void buildSystemSettingsSubPage(String pageName) {
            SettingsSystemPage page = new SettingsSystemPage();
            page.showSubPage(pageName);
            ScrollPane scroll = new ScrollPane(page);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("settings-scroll");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            VBox.setVgrow(scroll, Priority.ALWAYS);
            getChildren().addAll(scroll);
        }

        /**
         * Load saved settings from disk.
         */
        private void load() {
            // Read from Settings.properties if you want persistence
            // For now, use defaults that match the screenshots
            // (all checkboxes off by default, matching the screenshots)
        }

        /**
         * Save settings to disk.
         */
        public void save() {
            // Persist settings using dev.lumina.util.Settings
            // Example:
            // Settings.put("settings.zoom", String.valueOf((int)zoomSlider.getValue()));
            // ...
        }
    }
}