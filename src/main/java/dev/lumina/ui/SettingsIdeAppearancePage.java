package dev.lumina.ui;

import java.util.prefs.Preferences;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Modern IDE Appearance settings page under:
 * Appearance & Behavior > Appearance
 *
 * Implements full 2-column layout matching the screenshots:
 * - Single "Dark" theme option (strictly isolated from external IDE brandings)
 * - Editor color scheme selector
 * - Accessibility controls (Zoom, Custom font & size, Screen reader support, Contrast scrollbars, Color vision adjustment)
 * - 2-Column UI Options (Compact mode, Full path, Project toolbar colors, Popups open, Background Image dialog trigger, Smooth scrolling, Mnemonics, Menu icons)
 * - 2-Column Tree Views options
 * - 2-Column Tool Windows options
 * - Presentation Mode zoom
 * - Antialiasing options for IDE and Editor
 */
public class SettingsIdeAppearancePage extends VBox {

    private final Preferences prefs = Preferences.userNodeForPackage(SettingsIdeAppearancePage.class);

    // Theme & Scheme
    private final ComboBox<String> themeCombo;
    private final CheckBox syncWithOsCheck;
    private final ComboBox<String> schemeCombo;

    // Accessibility
    private final ComboBox<String> zoomCombo;
    private final CheckBox customFontCheck;
    private final ComboBox<String> fontCombo;
    private final ComboBox<String> fontSizeCombo;
    private final CheckBox screenReaderCheck;
    private final CheckBox contrastScrollbarsCheck;
    private final CheckBox adjustColorsCheck;

    // UI Options Col 1
    private final CheckBox compactModeCheck;
    private final CheckBox fullPathHeaderCheck;
    private final CheckBox projectColorsCheck;
    private final CheckBox keepPopupsOpenCheck;
    private final Button backgroundImageBtn;

    // UI Options Col 2
    private final CheckBox dragDropAltCheck;
    private final CheckBox smoothScrollingCheck;
    private final CheckBox mnemonicsControlsCheck;
    private final CheckBox mnemonicsMenuCheck;
    private final CheckBox displayIconsMenuCheck;

    // Tree Views
    private final CheckBox indentGuidesCheck;
    private final CheckBox smallerIndentsCheck;

    // Tool Windows Col 1
    private final CheckBox showToolWindowBarsCheck;
    private final CheckBox showToolWindowNamesCheck;
    private final CheckBox widescreenLayoutCheck;

    // Tool Windows Col 2
    private final CheckBox sideBySideLeftCheck;
    private final CheckBox sideBySideRightCheck;
    private final CheckBox rememberSizeCheck;

    // Presentation Mode
    private final ComboBox<String> presZoomCombo;

    // Antialiasing
    private final ComboBox<String> ideAACombo;
    private final ComboBox<String> editorAACombo;

    public SettingsIdeAppearancePage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(16);
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Theme and Editor color scheme
        VBox themeSectionBox = new VBox(10);

        HBox themeRow = new HBox(12);
        themeRow.setAlignment(Pos.CENTER_LEFT);

        Label themeLabel = new Label("Theme:");
        themeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");

        themeCombo = new ComboBox<>();
        // User specification: strictly only "Dark" theme
        themeCombo.getItems().addAll("Dark");
        themeCombo.getSelectionModel().select("Dark");
        themeCombo.setPrefWidth(160);
        styleCombo(themeCombo);

        syncWithOsCheck = new CheckBox("Sync with OS");
        syncWithOsCheck.setSelected(prefs.getBoolean("appearance_sync_os", false));
        styleCheck(syncWithOsCheck);

        Button themeGear = createGearButton();

        themeRow.getChildren().addAll(themeLabel, themeCombo, syncWithOsCheck, themeGear);

        HBox schemeRow = new HBox(12);
        schemeRow.setAlignment(Pos.CENTER_LEFT);

        Label schemeLabel = new Label("Editor color scheme:");
        schemeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");

        schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll("Dark Theme default");
        schemeCombo.getSelectionModel().select("Dark Theme default");
        schemeCombo.setPrefWidth(160);
        styleCombo(schemeCombo);

        Button schemeGear = createGearButton();

        schemeRow.getChildren().addAll(schemeLabel, schemeCombo, schemeGear);

        themeSectionBox.getChildren().addAll(themeRow, schemeRow);

        // 2. Accessibility
        Label accessHeader = createSectionHeader("Accessibility");

        HBox zoomRow = new HBox(12);
        zoomRow.setAlignment(Pos.CENTER_LEFT);
        Label zoomLabel = new Label("Zoom:");
        zoomLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 60px;");

        zoomCombo = new ComboBox<>();
        zoomCombo.getItems().addAll("100%", "110%", "125%", "150%", "175%", "200%");
        zoomCombo.getSelectionModel().select(prefs.get("appearance_zoom", "100%"));
        zoomCombo.setPrefWidth(90);
        styleCombo(zoomCombo);

        Label zoomHint = new Label("Change with ⌃⌥⇧= or ⌃⌥⇧-. Set to 100% with ⌃⌥⇧0");
        zoomHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        zoomRow.getChildren().addAll(zoomLabel, zoomCombo, zoomHint);

        HBox fontRow = new HBox(12);
        fontRow.setAlignment(Pos.CENTER_LEFT);
        customFontCheck = new CheckBox("Use custom font:");
        customFontCheck.setSelected(prefs.getBoolean("appearance_custom_font", false));
        styleCheck(customFontCheck);

        fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll("Inter", "SF Pro Text", "Segoe UI", "Roboto", "Dialog");
        fontCombo.getSelectionModel().select(prefs.get("appearance_font_family", "Inter"));
        fontCombo.setPrefWidth(180);
        fontCombo.disableProperty().bind(customFontCheck.selectedProperty().not());
        styleCombo(fontCombo);

        Label sizeLabel = new Label("Size:");
        sizeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        fontSizeCombo = new ComboBox<>();
        fontSizeCombo.getItems().addAll("10", "11", "12", "13", "14", "15", "16", "18", "20");
        fontSizeCombo.getSelectionModel().select(prefs.get("appearance_font_size", "13"));
        fontSizeCombo.setPrefWidth(70);
        fontSizeCombo.disableProperty().bind(customFontCheck.selectedProperty().not());
        styleCombo(fontSizeCombo);

        fontRow.getChildren().addAll(customFontCheck, fontCombo, sizeLabel, fontSizeCombo);

        HBox screenReaderRow = new HBox(8);
        screenReaderRow.setAlignment(Pos.CENTER_LEFT);
        screenReaderCheck = new CheckBox("Support screen readers");
        screenReaderCheck.setSelected(prefs.getBoolean("appearance_screen_readers", false));
        styleCheck(screenReaderCheck);

        Label restartBadge = new Label("Requires restart");
        restartBadge.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        screenReaderRow.getChildren().addAll(screenReaderCheck, restartBadge);

        Label screenReaderHint = new Label("⌃⇥ and ⌃⇧⇥ will navigate UI controls in dialogs and will not be available for switching editor tabs or other IDE actions. Tooltips on mouse hover will be disabled.");
        screenReaderHint.setWrapText(true);
        screenReaderHint.setMaxWidth(700);
        screenReaderHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        contrastScrollbarsCheck = new CheckBox("Use contrast scrollbars");
        contrastScrollbarsCheck.setSelected(prefs.getBoolean("appearance_contrast_scrollbars", false));
        styleCheck(contrastScrollbarsCheck);

        HBox adjustColorsRow = new HBox(8);
        adjustColorsRow.setAlignment(Pos.CENTER_LEFT);
        adjustColorsCheck = new CheckBox("Adjust colors for red-green vision deficiency");
        adjustColorsCheck.setSelected(prefs.getBoolean("appearance_adjust_vision", false));
        styleCheck(adjustColorsCheck);

        Hyperlink howItWorksLink = new Hyperlink("How it works");
        howItWorksLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false;");
        howItWorksLink.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Color Vision Deficiency Adjustment");
            alert.setHeaderText(null);
            alert.setContentText("Adjusts UI colors such as diff highlights, errors, and warnings for protanopia and deuteranopia visibility.");
            alert.showAndWait();
        });
        adjustColorsRow.getChildren().addAll(adjustColorsCheck, howItWorksLink);

        Label adjustColorsHint = new Label("Requires restart. For protanopia and deuteranopia.");
        adjustColorsHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        VBox accessBox = new VBox(8, zoomRow, fontRow, screenReaderRow, screenReaderHint, contrastScrollbarsCheck, adjustColorsRow, adjustColorsHint);

        // 3. UI Options (2 Columns)
        Label uiOptionsHeader = createSectionHeader("UI Options");

        GridPane uiGrid = new GridPane();
        uiGrid.setHgap(32);
        uiGrid.setVgap(8);

        // Col 1
        VBox uiCol1 = new VBox(8);
        compactModeCheck = new CheckBox("Compact mode");
        compactModeCheck.setSelected(prefs.getBoolean("appearance_compact_mode", false));
        styleCheck(compactModeCheck);
        Label compactHint = new Label("UI elements take up less screen space");
        compactHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        fullPathHeaderCheck = new CheckBox("Always show full path in window header");
        fullPathHeaderCheck.setSelected(prefs.getBoolean("appearance_full_path", false));
        styleCheck(fullPathHeaderCheck);

        projectColorsCheck = new CheckBox("Use project colors in main toolbar");
        projectColorsCheck.setSelected(prefs.getBoolean("appearance_project_colors", true));
        styleCheck(projectColorsCheck);
        Label projectColorsHint = new Label("Distinguish projects with different toolbar colors at a glance.");
        projectColorsHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        keepPopupsOpenCheck = new CheckBox("Keep popups open for toggle items");
        keepPopupsOpenCheck.setSelected(prefs.getBoolean("appearance_keep_popups_open", true));
        styleCheck(keepPopupsOpenCheck);

        backgroundImageBtn = new Button("Background Image...");
        backgroundImageBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 12 4 12;");
        backgroundImageBtn.setOnAction(e -> {
            new BackgroundImageDialog(getScene() != null ? getScene().getWindow() : null).showAndWait();
        });

        uiCol1.getChildren().addAll(compactModeCheck, compactHint, fullPathHeaderCheck, projectColorsCheck, projectColorsHint, keepPopupsOpenCheck, backgroundImageBtn);

        // Col 2
        VBox uiCol2 = new VBox(8);
        dragDropAltCheck = new CheckBox("Drag-and-drop with Alt pressed only");
        dragDropAltCheck.setSelected(prefs.getBoolean("appearance_drag_drop_alt", false));
        styleCheck(dragDropAltCheck);

        HBox smoothScrollRow = new HBox(6);
        smoothScrollRow.setAlignment(Pos.CENTER_LEFT);
        smoothScrollingCheck = new CheckBox("Smooth scrolling");
        smoothScrollingCheck.setSelected(prefs.getBoolean("appearance_smooth_scrolling", true));
        styleCheck(smoothScrollingCheck);
        Label smoothInfo = new Label("🛈");
        smoothInfo.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-cursor: hand;");
        smoothInfo.setTooltip(new Tooltip("Enables smooth animated transitions when scrolling editor contents."));
        smoothScrollRow.getChildren().addAll(smoothScrollingCheck, smoothInfo);

        mnemonicsControlsCheck = new CheckBox("Enable mnemonics in controls");
        mnemonicsControlsCheck.setSelected(prefs.getBoolean("appearance_mnemonics_controls", true));
        styleCheck(mnemonicsControlsCheck);

        mnemonicsMenuCheck = new CheckBox("Enable mnemonics in menu");
        mnemonicsMenuCheck.setSelected(prefs.getBoolean("appearance_mnemonics_menu", false));
        styleCheck(mnemonicsMenuCheck);

        displayIconsMenuCheck = new CheckBox("Display icons in menu items");
        displayIconsMenuCheck.setSelected(prefs.getBoolean("appearance_display_icons_menu", true));
        styleCheck(displayIconsMenuCheck);

        uiCol2.getChildren().addAll(dragDropAltCheck, smoothScrollRow, mnemonicsControlsCheck, mnemonicsMenuCheck, displayIconsMenuCheck);

        uiGrid.add(uiCol1, 0, 0);
        uiGrid.add(uiCol2, 1, 0);

        // 4. Tree Views
        Label treeViewsHeader = createSectionHeader("Tree Views");
        GridPane treeGrid = new GridPane();
        treeGrid.setHgap(32);

        indentGuidesCheck = new CheckBox("Show indent guides");
        indentGuidesCheck.setSelected(prefs.getBoolean("appearance_indent_guides", false));
        styleCheck(indentGuidesCheck);

        smallerIndentsCheck = new CheckBox("Use smaller indents");
        smallerIndentsCheck.setSelected(prefs.getBoolean("appearance_smaller_indents", false));
        styleCheck(smallerIndentsCheck);

        treeGrid.add(indentGuidesCheck, 0, 0);
        treeGrid.add(smallerIndentsCheck, 1, 0);

        // 5. Tool Windows (2 Columns)
        Label toolWindowsHeader = createSectionHeader("Tool Windows");
        GridPane toolGrid = new GridPane();
        toolGrid.setHgap(32);
        toolGrid.setVgap(8);

        VBox toolCol1 = new VBox(8);
        showToolWindowBarsCheck = new CheckBox("Show tool window bars");
        showToolWindowBarsCheck.setSelected(prefs.getBoolean("appearance_tool_bars", true));
        styleCheck(showToolWindowBarsCheck);

        showToolWindowNamesCheck = new CheckBox("Show tool window names");
        showToolWindowNamesCheck.setSelected(prefs.getBoolean("appearance_tool_names", false));
        styleCheck(showToolWindowNamesCheck);

        HBox widescreenRow = new HBox(6);
        widescreenRow.setAlignment(Pos.CENTER_LEFT);
        widescreenLayoutCheck = new CheckBox("Widescreen tool window layout");
        widescreenLayoutCheck.setSelected(prefs.getBoolean("appearance_widescreen", false));
        styleCheck(widescreenLayoutCheck);
        Label wideInfo = new Label("🛈");
        wideInfo.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-cursor: hand;");
        wideInfo.setTooltip(new Tooltip("Extends vertical height of left and right tool windows to use the full IDE height."));
        widescreenRow.getChildren().addAll(widescreenLayoutCheck, wideInfo);

        toolCol1.getChildren().addAll(showToolWindowBarsCheck, showToolWindowNamesCheck, widescreenRow);

        VBox toolCol2 = new VBox(8);
        sideBySideLeftCheck = new CheckBox("Side-by-side layout on the left");
        sideBySideLeftCheck.setSelected(prefs.getBoolean("appearance_side_by_side_left", false));
        styleCheck(sideBySideLeftCheck);

        sideBySideRightCheck = new CheckBox("Side-by-side layout on the right");
        sideBySideRightCheck.setSelected(prefs.getBoolean("appearance_side_by_side_right", false));
        styleCheck(sideBySideRightCheck);

        rememberSizeCheck = new CheckBox("Remember size for each tool window");
        rememberSizeCheck.setSelected(prefs.getBoolean("appearance_remember_size", false));
        styleCheck(rememberSizeCheck);

        toolCol2.getChildren().addAll(sideBySideLeftCheck, sideBySideRightCheck, rememberSizeCheck);

        toolGrid.add(toolCol1, 0, 0);
        toolGrid.add(toolCol2, 1, 0);

        // 6. Presentation Mode
        Label presHeader = createSectionHeader("Presentation Mode");
        HBox presRow = new HBox(12);
        presRow.setAlignment(Pos.CENTER_LEFT);
        Label presZoomLabel = new Label("Zoom:");
        presZoomLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        presZoomCombo = new ComboBox<>();
        presZoomCombo.getItems().addAll("125%", "150%", "175%", "200%", "225%", "250%");
        presZoomCombo.getSelectionModel().select(prefs.get("appearance_pres_zoom", "175%"));
        presZoomCombo.setPrefWidth(90);
        styleCombo(presZoomCombo);
        presRow.getChildren().addAll(presZoomLabel, presZoomCombo);

        // 7. Antialiasing
        Label aaHeader = createSectionHeader("Antialiasing");
        HBox aaRow = new HBox(24);
        aaRow.setAlignment(Pos.CENTER_LEFT);

        HBox ideAARow = new HBox(8);
        ideAARow.setAlignment(Pos.CENTER_LEFT);
        Label ideAALabel = new Label("IDE:");
        ideAALabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        ideAACombo = new ComboBox<>();
        ideAACombo.getItems().addAll("Greyscale", "Subpixel", "No antialiasing");
        ideAACombo.getSelectionModel().select(prefs.get("appearance_ide_aa", "Greyscale"));
        ideAACombo.setPrefWidth(120);
        styleCombo(ideAACombo);
        ideAARow.getChildren().addAll(ideAALabel, ideAACombo);

        HBox editorAARow = new HBox(8);
        editorAARow.setAlignment(Pos.CENTER_LEFT);
        Label editorAALabel = new Label("Editor:");
        editorAALabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        editorAACombo = new ComboBox<>();
        editorAACombo.getItems().addAll("Greyscale", "Subpixel", "No antialiasing");
        editorAACombo.getSelectionModel().select(prefs.get("appearance_editor_aa", "Greyscale"));
        editorAACombo.setPrefWidth(120);
        styleCombo(editorAACombo);
        editorAARow.getChildren().addAll(editorAALabel, editorAACombo);

        aaRow.getChildren().addAll(ideAARow, editorAARow);

        // Assemble All
        getChildren().addAll(
                themeSectionBox,
                accessHeader, accessBox,
                uiOptionsHeader, uiGrid,
                treeViewsHeader, treeGrid,
                toolWindowsHeader, toolGrid,
                presHeader, presRow,
                aaHeader, aaRow
        );
    }

    private Label createSectionHeader(String title) {
        Label header = new Label(title);
        header.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 4 0;");
        return header;
    }

    private Button createGearButton() {
        Button gear = new Button("⚙");
        gear.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 4 2 4;");
        gear.setOnMouseEntered(e -> gear.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 4 2 4; -fx-background-radius: 4;"));
        gear.setOnMouseExited(e -> gear.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 4 2 4;"));
        return gear;
    }

    private void styleCombo(ComboBox<?> combo) {
        combo.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
    }

    private void styleCheck(CheckBox check) {
        check.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    public void save() {
        prefs.putBoolean("appearance_sync_os", syncWithOsCheck.isSelected());
        prefs.put("appearance_zoom", zoomCombo.getValue());
        prefs.putBoolean("appearance_custom_font", customFontCheck.isSelected());
        prefs.put("appearance_font_family", fontCombo.getValue());
        prefs.put("appearance_font_size", fontSizeCombo.getValue());
        prefs.putBoolean("appearance_screen_readers", screenReaderCheck.isSelected());
        prefs.putBoolean("appearance_contrast_scrollbars", contrastScrollbarsCheck.isSelected());
        prefs.putBoolean("appearance_adjust_vision", adjustColorsCheck.isSelected());
        prefs.putBoolean("appearance_compact_mode", compactModeCheck.isSelected());
        prefs.putBoolean("appearance_full_path", fullPathHeaderCheck.isSelected());
        prefs.putBoolean("appearance_project_colors", projectColorsCheck.isSelected());
        prefs.putBoolean("appearance_keep_popups_open", keepPopupsOpenCheck.isSelected());
        prefs.putBoolean("appearance_drag_drop_alt", dragDropAltCheck.isSelected());
        prefs.putBoolean("appearance_smooth_scrolling", smoothScrollingCheck.isSelected());
        prefs.putBoolean("appearance_mnemonics_controls", mnemonicsControlsCheck.isSelected());
        prefs.putBoolean("appearance_mnemonics_menu", mnemonicsMenuCheck.isSelected());
        prefs.putBoolean("appearance_display_icons_menu", displayIconsMenuCheck.isSelected());
        prefs.putBoolean("appearance_indent_guides", indentGuidesCheck.isSelected());
        prefs.putBoolean("appearance_smaller_indents", smallerIndentsCheck.isSelected());
        prefs.putBoolean("appearance_tool_bars", showToolWindowBarsCheck.isSelected());
        prefs.putBoolean("appearance_tool_names", showToolWindowNamesCheck.isSelected());
        prefs.putBoolean("appearance_widescreen", widescreenLayoutCheck.isSelected());
        prefs.putBoolean("appearance_side_by_side_left", sideBySideLeftCheck.isSelected());
        prefs.putBoolean("appearance_side_by_side_right", sideBySideRightCheck.isSelected());
        prefs.putBoolean("appearance_remember_size", rememberSizeCheck.isSelected());
        prefs.put("appearance_pres_zoom", presZoomCombo.getValue());
        prefs.put("appearance_ide_aa", ideAACombo.getValue());
        prefs.put("appearance_editor_aa", editorAACombo.getValue());
    }
}
