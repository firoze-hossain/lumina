// SettingsEditorTabsPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Editor Tabs settings page.
 * Complete implementation matching both screenshots.
 */
public class SettingsEditorTabsPage extends VBox {

    public SettingsEditorTabsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Appearance section
        // ============================================================
        Label appearanceLabel = new Label("Appearance");
        appearanceLabel.getStyleClass().add("settings-section");

        // Tab placement
        HBox placementRow = new HBox(8);
        placementRow.setPadding(new Insets(4, 0, 8, 20));
        placementRow.setAlignment(Pos.CENTER_LEFT);

        Label placementLabel = new Label("Tab placement:");
        placementLabel.getStyleClass().add("settings-label");
        ComboBox<String> placementCombo = new ComboBox<>();
        placementCombo.getItems().addAll("Top", "Bottom", "Left", "Right");
        placementCombo.getSelectionModel().selectFirst();
        placementCombo.getStyleClass().add("settings-combo");
        placementCombo.setPrefWidth(120);
        placementRow.getChildren().addAll(placementLabel, placementCombo);

        // Show tabs in
        Label showTabsLabel = new Label("Show tabs in:");
        showTabsLabel.getStyleClass().add("settings-label");
        showTabsLabel.setPadding(new Insets(8, 0, 4, 20));

        VBox showTabsBox = new VBox(4);
        showTabsBox.setPadding(new Insets(4, 0, 4, 20));

        // One row radio buttons
        RadioButton oneRow = new RadioButton("One row, and if tabs don't fit:");
        oneRow.setSelected(true);
        oneRow.getStyleClass().add("settings-radio");

        VBox oneRowOptions = new VBox(4);
        oneRowOptions.setPadding(new Insets(4, 0, 4, 20));

        RadioButton scrollTabs = new RadioButton("Scroll the tabs panel");
        scrollTabs.setSelected(true);
        RadioButton squeezeTabs = new RadioButton("Squeeze tabs");

        ToggleGroup oneRowGroup = new ToggleGroup();
        scrollTabs.setToggleGroup(oneRowGroup);
        squeezeTabs.setToggleGroup(oneRowGroup);

        oneRowOptions.getChildren().addAll(scrollTabs, squeezeTabs);

        RadioButton multipleRows = new RadioButton("Multiple rows");
        multipleRows.getStyleClass().add("settings-radio");

        ToggleGroup showTabsGroup = new ToggleGroup();
        oneRow.setToggleGroup(showTabsGroup);
        multipleRows.setToggleGroup(showTabsGroup);

        // Appearance checkboxes
        CheckBox pinnedSeparateRow = new CheckBox("Show pinned tabs in a separate row");
        pinnedSeparateRow.setSelected(true);
        pinnedSeparateRow.getStyleClass().add("settings-check");

        CheckBox showFileIcon = new CheckBox("Show file icon");
        showFileIcon.setSelected(true);
        showFileIcon.getStyleClass().add("settings-check");

        CheckBox showFileExtension = new CheckBox("Show file extension");
        showFileExtension.setSelected(true);
        showFileExtension.getStyleClass().add("settings-check");

        CheckBox showDirectory = new CheckBox("Show directory for non-unique file names");
        showDirectory.setSelected(true);
        showDirectory.getStyleClass().add("settings-check");

        CheckBox markModified = new CheckBox("Mark modified");
        markModified.setSelected(true);
        markModified.getStyleClass().add("settings-check");

        CheckBox showFullPath = new CheckBox("Show full path on mouse hover");
        showFullPath.setSelected(true);
        showFullPath.getStyleClass().add("settings-check");

        // Close button position
        HBox closeRow = new HBox(8);
        closeRow.setPadding(new Insets(4, 0, 8, 20));
        closeRow.setAlignment(Pos.CENTER_LEFT);

        Label closeLabel = new Label("Close button position:");
        closeLabel.getStyleClass().add("settings-label");
        ComboBox<String> closeCombo = new ComboBox<>();
        closeCombo.getItems().addAll("Right", "Left");
        closeCombo.getSelectionModel().selectFirst();
        closeCombo.getStyleClass().add("settings-combo");
        closeCombo.setPrefWidth(120);
        closeRow.getChildren().addAll(closeLabel, closeCombo);

        // Assemble appearance section
        VBox appearanceBox = new VBox(4);
        appearanceBox.setPadding(new Insets(4, 0, 8, 20));
        appearanceBox.getChildren().addAll(
            placementRow,
            showTabsLabel,
            oneRow,
            oneRowOptions,
            multipleRows,
            pinnedSeparateRow,
            showFileIcon,
            showFileExtension,
            showDirectory,
            markModified,
            showFullPath,
            closeRow
        );

        // ============================================================
        // Opening Policy section
        // ============================================================
        Label openingLabel = new Label("Opening Policy");
        openingLabel.getStyleClass().add("settings-section");

        CheckBox previewTab = new CheckBox("Enable preview tab");
        previewTab.setSelected(true);
        previewTab.getStyleClass().add("settings-check");

        Label previewHint = new Label("The preview tab is reused to show files selected with a single click in the Project tool window, and files opened during debugging.");
        previewHint.getStyleClass().add("settings-hint");
        previewHint.setWrapText(true);
        previewHint.setPadding(new Insets(0, 0, 8, 20));

        VBox openingBox = new VBox(4);
        openingBox.setPadding(new Insets(4, 0, 8, 20));
        openingBox.getChildren().addAll(previewTab, previewHint);

        // ============================================================
        // Closing Policy section
        // ============================================================
        Label closingLabel = new Label("Closing Policy");
        closingLabel.getStyleClass().add("settings-section");

        HBox limitRow = new HBox(8);
        limitRow.setPadding(new Insets(4, 0, 8, 20));
        limitRow.setAlignment(Pos.CENTER_LEFT);

        Label limitLabel = new Label("Tab limit:");
        limitLabel.getStyleClass().add("settings-label");
        Spinner<Integer> limitSpinner = new Spinner<>(5, 100, 30, 5);
        limitSpinner.setPrefWidth(70);
        limitSpinner.getStyleClass().add("settings-spinner");
        limitRow.getChildren().addAll(limitLabel, limitSpinner);

        Label exceedLabel = new Label("When tabs exceed the limit:");
        exceedLabel.getStyleClass().add("settings-label");
        exceedLabel.setPadding(new Insets(8, 0, 4, 20));

        CheckBox closeUnchanged = new CheckBox("Close unchanged");
        closeUnchanged.getStyleClass().add("settings-check");

        CheckBox closeUnused = new CheckBox("Close unused");
        closeUnused.setSelected(true);
        closeUnused.getStyleClass().add("settings-check");

        VBox exceedBox = new VBox(4);
        exceedBox.setPadding(new Insets(4, 0, 8, 20));
        exceedBox.getChildren().addAll(closeUnchanged, closeUnused);

        Label activateLabel = new Label("When the current tab is closed, activate:");
        activateLabel.getStyleClass().add("settings-label");
        activateLabel.setPadding(new Insets(8, 0, 4, 20));

        RadioButton activateLeft = new RadioButton("The tab on the left");
        activateLeft.setSelected(true);
        RadioButton activateRight = new RadioButton("The tab on the right");
        RadioButton activateRecent = new RadioButton("Most recently opened tab");

        ToggleGroup activateGroup = new ToggleGroup();
        activateLeft.setToggleGroup(activateGroup);
        activateRight.setToggleGroup(activateGroup);
        activateRecent.setToggleGroup(activateGroup);

        VBox activateBox = new VBox(4);
        activateBox.setPadding(new Insets(4, 0, 8, 20));
        activateBox.getChildren().addAll(activateLeft, activateRight, activateRecent);

        VBox closingBox = new VBox(4);
        closingBox.setPadding(new Insets(4, 0, 8, 20));
        closingBox.getChildren().addAll(
            limitRow,
            exceedLabel,
            exceedBox,
            activateLabel,
            activateBox
        );

        // ============================================================
        // Database section
        // ============================================================
        Label dbLabel = new Label("Database");
        dbLabel.getStyleClass().add("settings-section");

        CheckBox qualifiedNames = new CheckBox("Always show qualified names for database objects in tab titles");
        qualifiedNames.getStyleClass().add("settings-check");

        CheckBox shortenNames = new CheckBox("Shorten datasource and object names in tab titles");
        shortenNames.setSelected(true);
        shortenNames.getStyleClass().add("settings-check");

        VBox dbBox = new VBox(4);
        dbBox.setPadding(new Insets(4, 0, 8, 20));
        dbBox.getChildren().addAll(qualifiedNames, shortenNames);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            appearanceLabel,
            appearanceBox,
            openingLabel,
            openingBox,
            closingLabel,
            closingBox,
            dbLabel,
            dbBox
        );
    }
}