package dev.lumina.ui;

import dev.lumina.settings.EditorTabsSettings;
import dev.lumina.settings.EditorTabsSettings.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

/**
 * Pixel-perfect IntelliJ IDEA-style Editor > General > Editor Tabs settings page.
 * Completely dynamic, backed by EditorTabsSettings, supporting dirty tracking,
 * real-time tab synchronization, and all sections matching IntelliJ IDEA.
 */
public class SettingsEditorTabsPage extends VBox {

    // 1. Appearance
    private final ComboBox<TabPlacement> tabPlacementCombo = new ComboBox<>();
    private final RadioButton oneRowRadio = new RadioButton("One row, and if tabs don't fit:");
    private final RadioButton scrollTabsRadio = new RadioButton("Scroll the tabs panel");
    private final RadioButton squeezeTabsRadio = new RadioButton("Squeeze tabs");
    private final ToggleGroup oneRowFitGroup = new ToggleGroup();
    private final RadioButton multipleRowsRadio = new RadioButton("Multiple rows");
    private final ToggleGroup tabRowsModeGroup = new ToggleGroup();

    private final CheckBox showPinnedTabsInSeparateRowCheck = new CheckBox("Show pinned tabs in a separate row");
    private final CheckBox showFileIconCheck = new CheckBox("Show file icon");
    private final CheckBox showFileExtensionCheck = new CheckBox("Show file extension");
    private final CheckBox showDirectoryForNonUniqueNamesCheck = new CheckBox("Show directory for non-unique file names");
    private final CheckBox markModifiedCheck = new CheckBox("Mark modified");
    private final CheckBox showFullPathOnMouseHoverCheck = new CheckBox("Show full path on mouse hover");

    private final ComboBox<CloseButtonPosition> closeButtonPosCombo = new ComboBox<>();

    // 2. Tab Order
    private final CheckBox sortTabsAlphabeticallyCheck = new CheckBox("Sort tabs alphabetically");
    private final CheckBox openNewTabsAtEndCheck = new CheckBox("Open new tabs at the end");

    // 3. Opening Policy
    private final CheckBox enablePreviewTabCheck = new CheckBox("Enable preview tab");
    private final Label previewTabHint = new Label("The preview tab is reused to show files selected with a single click in the Project tool window, and files opened during debugging.");

    // 4. Closing Policy
    private final TextField tabLimitField = new TextField("30");

    private final RadioButton closeUnchangedRadio = new RadioButton("Close unchanged");
    private final RadioButton closeUnusedRadio = new RadioButton("Close unused");
    private final ToggleGroup exceedLimitGroup = new ToggleGroup();

    private final RadioButton activateLeftRadio = new RadioButton("The tab on the left");
    private final RadioButton activateRightRadio = new RadioButton("The tab on the right");
    private final RadioButton activateRecentRadio = new RadioButton("Most recently opened tab");
    private final ToggleGroup activateGroup = new ToggleGroup();

    // 5. Database
    private final CheckBox dbAlwaysShowQualifiedNamesCheck = new CheckBox("Always show qualified names for database objects in tab titles");
    private final CheckBox dbShortenDatasourceNamesCheck = new CheckBox("Shorten datasource and object names in tab titles");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsEditorTabsPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(8);

        buildUi();
        setupListeners();
        loadFromSettings(EditorTabsSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void styleRadio(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void styleTextField(TextField tf, double width) {
        tf.setPrefWidth(width);
        tf.setPrefHeight(25);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 6 3 6;");
    }

    private Node buildSectionSeparator(String title) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1px; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    private void buildUi() {
        // --- 1. Appearance ---
        Node appearanceSeparator = buildSectionSeparator("Appearance");

        // Tab placement
        Label tabPlacementLabel = new Label("Tab placement:");
        tabPlacementLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        tabPlacementCombo.getItems().setAll(TabPlacement.values());
        tabPlacementCombo.setConverter(new StringConverter<>() {
            @Override public String toString(TabPlacement p) { return p != null ? p.getLabel() : ""; }
            @Override public TabPlacement fromString(String s) { return TabPlacement.fromLabel(s); }
        });
        tabPlacementCombo.setPrefWidth(100);
        tabPlacementCombo.setPrefHeight(25);
        tabPlacementCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox placementRow = new HBox(8, tabPlacementLabel, tabPlacementCombo);
        placementRow.setAlignment(Pos.CENTER_LEFT);
        placementRow.setPadding(new Insets(0, 0, 4, 0));

        // Show tabs in
        Label showTabsLabel = new Label("Show tabs in:");
        showTabsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        styleRadio(oneRowRadio);
        styleRadio(multipleRowsRadio);
        oneRowRadio.setToggleGroup(tabRowsModeGroup);
        multipleRowsRadio.setToggleGroup(tabRowsModeGroup);

        styleRadio(scrollTabsRadio);
        styleRadio(squeezeTabsRadio);
        scrollTabsRadio.setToggleGroup(oneRowFitGroup);
        squeezeTabsRadio.setToggleGroup(oneRowFitGroup);

        VBox oneRowFitBox = new VBox(4, scrollTabsRadio, squeezeTabsRadio);
        oneRowFitBox.setPadding(new Insets(2, 0, 4, 20));

        oneRowRadio.selectedProperty().addListener((obs, o, n) -> {
            scrollTabsRadio.setDisable(!n);
            squeezeTabsRadio.setDisable(!n);
        });

        VBox showTabsBox = new VBox(4, showTabsLabel, oneRowRadio, oneRowFitBox, multipleRowsRadio);
        showTabsBox.setPadding(new Insets(2, 0, 4, 0));

        // Appearance checkboxes
        styleCheckBox(showPinnedTabsInSeparateRowCheck);
        styleCheckBox(showFileIconCheck);
        styleCheckBox(showFileExtensionCheck);
        styleCheckBox(showDirectoryForNonUniqueNamesCheck);
        styleCheckBox(markModifiedCheck);
        styleCheckBox(showFullPathOnMouseHoverCheck);

        VBox appearanceChecksBox = new VBox(6,
                showPinnedTabsInSeparateRowCheck,
                showFileIconCheck,
                showFileExtensionCheck,
                showDirectoryForNonUniqueNamesCheck,
                markModifiedCheck,
                showFullPathOnMouseHoverCheck
        );
        appearanceChecksBox.setPadding(new Insets(4, 0, 6, 0));

        // Close button position
        Label closePosLabel = new Label("Close button position:");
        closePosLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        closeButtonPosCombo.getItems().setAll(CloseButtonPosition.values());
        closeButtonPosCombo.setConverter(new StringConverter<>() {
            @Override public String toString(CloseButtonPosition c) { return c != null ? c.getLabel() : ""; }
            @Override public CloseButtonPosition fromString(String s) { return CloseButtonPosition.fromLabel(s); }
        });
        closeButtonPosCombo.setPrefWidth(90);
        closeButtonPosCombo.setPrefHeight(25);
        closeButtonPosCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox closePosRow = new HBox(8, closePosLabel, closeButtonPosCombo);
        closePosRow.setAlignment(Pos.CENTER_LEFT);
        closePosRow.setPadding(new Insets(2, 0, 4, 0));

        // --- 2. Tab Order ---
        Node tabOrderSeparator = buildSectionSeparator("Tab Order");

        styleCheckBox(sortTabsAlphabeticallyCheck);
        styleCheckBox(openNewTabsAtEndCheck);

        VBox tabOrderBox = new VBox(6, sortTabsAlphabeticallyCheck, openNewTabsAtEndCheck);
        tabOrderBox.setPadding(new Insets(2, 0, 4, 0));

        // --- 3. Opening Policy ---
        Node openingSeparator = buildSectionSeparator("Opening Policy");

        styleCheckBox(enablePreviewTabCheck);
        previewTabHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        previewTabHint.setWrapText(true);
        previewTabHint.setPadding(new Insets(0, 0, 2, 20));

        VBox openingBox = new VBox(4, enablePreviewTabCheck, previewTabHint);
        openingBox.setPadding(new Insets(2, 0, 4, 0));

        // --- 4. Closing Policy ---
        Node closingSeparator = buildSectionSeparator("Closing Policy");

        Label tabLimitLabel = new Label("Tab limit:");
        tabLimitLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        styleTextField(tabLimitField, 55);

        HBox tabLimitRow = new HBox(8, tabLimitLabel, tabLimitField);
        tabLimitRow.setAlignment(Pos.CENTER_LEFT);

        Label exceedLabel = new Label("When tabs exceed the limit:");
        exceedLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        styleRadio(closeUnchangedRadio);
        styleRadio(closeUnusedRadio);
        closeUnchangedRadio.setToggleGroup(exceedLimitGroup);
        closeUnusedRadio.setToggleGroup(exceedLimitGroup);

        VBox exceedBox = new VBox(4, closeUnchangedRadio, closeUnusedRadio);
        exceedBox.setPadding(new Insets(2, 0, 4, 20));

        Label activateLabel = new Label("When the current tab is closed, activate:");
        activateLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        styleRadio(activateLeftRadio);
        styleRadio(activateRightRadio);
        styleRadio(activateRecentRadio);
        activateLeftRadio.setToggleGroup(activateGroup);
        activateRightRadio.setToggleGroup(activateGroup);
        activateRecentRadio.setToggleGroup(activateGroup);

        VBox activateBox = new VBox(4, activateLeftRadio, activateRightRadio, activateRecentRadio);
        activateBox.setPadding(new Insets(2, 0, 4, 20));

        VBox closingBox = new VBox(6, tabLimitRow, exceedLabel, exceedBox, activateLabel, activateBox);
        closingBox.setPadding(new Insets(2, 0, 4, 0));

        // --- 5. Database ---
        Node databaseSeparator = buildSectionSeparator("Database");

        styleCheckBox(dbAlwaysShowQualifiedNamesCheck);
        styleCheckBox(dbShortenDatasourceNamesCheck);

        VBox databaseBox = new VBox(6, dbAlwaysShowQualifiedNamesCheck, dbShortenDatasourceNamesCheck);
        databaseBox.setPadding(new Insets(2, 0, 4, 0));

        getChildren().addAll(
                appearanceSeparator,
                placementRow,
                showTabsBox,
                appearanceChecksBox,
                closePosRow,
                tabOrderSeparator,
                tabOrderBox,
                openingSeparator,
                openingBox,
                closingSeparator,
                closingBox,
                databaseSeparator,
                databaseBox
        );
    }

    private void setupListeners() {
        tabPlacementCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        oneRowRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        multipleRowsRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scrollTabsRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        squeezeTabsRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());

        showPinnedTabsInSeparateRowCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showFileIconCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showFileExtensionCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showDirectoryForNonUniqueNamesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        markModifiedCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showFullPathOnMouseHoverCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        closeButtonPosCombo.valueProperty().addListener((obs, o, n) -> notifyModified());

        sortTabsAlphabeticallyCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        openNewTabsAtEndCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        enablePreviewTabCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        tabLimitField.textProperty().addListener((obs, o, n) -> notifyModified());

        closeUnchangedRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        closeUnusedRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());

        activateLeftRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        activateRightRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        activateRecentRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());

        dbAlwaysShowQualifiedNamesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        dbShortenDatasourceNamesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    public void loadFromSettings(EditorTabsSettings s) {
        suppressEvents = true;
        try {
            tabPlacementCombo.setValue(s.getTabPlacement());

            if (s.getTabRowsMode() == TabRowsMode.MULTIPLE_ROWS) {
                multipleRowsRadio.setSelected(true);
            } else {
                oneRowRadio.setSelected(true);
            }

            if (s.getOneRowFitPolicy() == OneRowFitPolicy.SQUEEZE) {
                squeezeTabsRadio.setSelected(true);
            } else {
                scrollTabsRadio.setSelected(true);
            }

            boolean isOneRow = (s.getTabRowsMode() == TabRowsMode.ONE_ROW);
            scrollTabsRadio.setDisable(!isOneRow);
            squeezeTabsRadio.setDisable(!isOneRow);

            showPinnedTabsInSeparateRowCheck.setSelected(s.isShowPinnedTabsInSeparateRow());
            showFileIconCheck.setSelected(s.isShowFileIcon());
            showFileExtensionCheck.setSelected(s.isShowFileExtension());
            showDirectoryForNonUniqueNamesCheck.setSelected(s.isShowDirectoryForNonUniqueNames());
            markModifiedCheck.setSelected(s.isMarkModified());
            showFullPathOnMouseHoverCheck.setSelected(s.isShowFullPathOnMouseHover());

            closeButtonPosCombo.setValue(s.getCloseButtonPosition());

            sortTabsAlphabeticallyCheck.setSelected(s.isSortTabsAlphabetically());
            openNewTabsAtEndCheck.setSelected(s.isOpenNewTabsAtEnd());

            enablePreviewTabCheck.setSelected(s.isEnablePreviewTab());

            tabLimitField.setText(String.valueOf(s.getTabLimit()));

            if (s.getTabsExceedLimitPolicy() == TabsExceedLimitPolicy.CLOSE_UNCHANGED) {
                closeUnchangedRadio.setSelected(true);
            } else {
                closeUnusedRadio.setSelected(true);
            }

            if (s.getTabCloseActivatePolicy() == TabCloseActivatePolicy.ACTIVATE_RIGHT) {
                activateRightRadio.setSelected(true);
            } else if (s.getTabCloseActivatePolicy() == TabCloseActivatePolicy.ACTIVATE_MOST_RECENT) {
                activateRecentRadio.setSelected(true);
            } else {
                activateLeftRadio.setSelected(true);
            }

            dbAlwaysShowQualifiedNamesCheck.setSelected(s.isDbAlwaysShowQualifiedNames());
            dbShortenDatasourceNamesCheck.setSelected(s.isDbShortenDatasourceNames());
        } finally {
            suppressEvents = false;
        }
    }

    public void saveToSettings(EditorTabsSettings s) {
        if (tabPlacementCombo.getValue() != null) {
            s.setTabPlacement(tabPlacementCombo.getValue());
        }
        s.setTabRowsMode(multipleRowsRadio.isSelected() ? TabRowsMode.MULTIPLE_ROWS : TabRowsMode.ONE_ROW);
        s.setOneRowFitPolicy(squeezeTabsRadio.isSelected() ? OneRowFitPolicy.SQUEEZE : OneRowFitPolicy.SCROLL);

        s.setShowPinnedTabsInSeparateRow(showPinnedTabsInSeparateRowCheck.isSelected());
        s.setShowFileIcon(showFileIconCheck.isSelected());
        s.setShowFileExtension(showFileExtensionCheck.isSelected());
        s.setShowDirectoryForNonUniqueNames(showDirectoryForNonUniqueNamesCheck.isSelected());
        s.setMarkModified(markModifiedCheck.isSelected());
        s.setShowFullPathOnMouseHover(showFullPathOnMouseHoverCheck.isSelected());

        if (closeButtonPosCombo.getValue() != null) {
            s.setCloseButtonPosition(closeButtonPosCombo.getValue());
        }

        s.setSortTabsAlphabetically(sortTabsAlphabeticallyCheck.isSelected());
        s.setOpenNewTabsAtEnd(openNewTabsAtEndCheck.isSelected());

        s.setEnablePreviewTab(enablePreviewTabCheck.isSelected());

        try {
            s.setTabLimit(Integer.parseInt(tabLimitField.getText().trim()));
        } catch (NumberFormatException ignored) {}

        s.setTabsExceedLimitPolicy(closeUnchangedRadio.isSelected() ? TabsExceedLimitPolicy.CLOSE_UNCHANGED : TabsExceedLimitPolicy.CLOSE_UNUSED);

        if (activateRightRadio.isSelected()) {
            s.setTabCloseActivatePolicy(TabCloseActivatePolicy.ACTIVATE_RIGHT);
        } else if (activateRecentRadio.isSelected()) {
            s.setTabCloseActivatePolicy(TabCloseActivatePolicy.ACTIVATE_MOST_RECENT);
        } else {
            s.setTabCloseActivatePolicy(TabCloseActivatePolicy.ACTIVATE_LEFT);
        }

        s.setDbAlwaysShowQualifiedNames(dbAlwaysShowQualifiedNamesCheck.isSelected());
        s.setDbShortenDatasourceNames(dbShortenDatasourceNamesCheck.isSelected());
    }

    public boolean isModified() {
        EditorTabsSettings current = new EditorTabsSettings();
        saveToSettings(current);
        return current.isModified(EditorTabsSettings.getInstance());
    }

    public void apply() {
        saveToSettings(EditorTabsSettings.getInstance());
        EditorTabsSettings.getInstance().save();
    }

    public void reset() {
        loadFromSettings(EditorTabsSettings.getInstance());
    }
}