package dev.lumina.ui;

import dev.lumina.git.VcsChangelistSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Version Control > Changelists Settings Page matching IntelliJ IDEA Images 2 & 3.
 * Supports:
 * - Automatic changelist creation
 * - Putting changes within one file into different changelists
 * - Inactive Changelist options (Highlight, Dialog on edit, Empty inactive policy: Show options / Do nothing / Remove silently)
 * - Conflicts options (Highlight files with conflicts, Ignored conflicts list, Clear button)
 */
public class SettingsVcsChangelistsPage extends VBox {

    private final VcsChangelistSettings settings = VcsChangelistSettings.getInstance();

    private final CheckBox autoCreateCheck = new CheckBox("Create changelists automatically");
    private final CheckBox allowMultiCheck = new CheckBox("Allow putting changes within one file into different changelists");

    private final CheckBox highlightInactiveCheck = new CheckBox("Highlight files from inactive changelists");
    private final CheckBox showDialogInactiveCheck = new CheckBox("Show dialog on attempt to edit file from inactive changelist");
    private final ComboBox<VcsChangelistSettings.EmptyChangelistInactiveAction> emptyActionCombo = new ComboBox<>();

    private final CheckBox highlightConflictsCheck = new CheckBox("Highlight files with changelist conflicts");
    private final ListView<String> ignoredListView = new ListView<>();
    private final Label noIgnoredLabel = new Label("No ignored files");
    private final StackPane listContainer = new StackPane();
    private final Button clearBtn = new Button("Clear");

    public SettingsVcsChangelistsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Top options
        styleCheck(autoCreateCheck);
        autoCreateCheck.setSelected(settings.isCreateChangelistsAutomatically());
        autoCreateCheck.setOnAction(e -> settings.setCreateChangelistsAutomatically(autoCreateCheck.isSelected()));

        styleCheck(allowMultiCheck);
        allowMultiCheck.setSelected(settings.isAllowPuttingChangesWithinOneFileIntoDifferentChangelists());
        allowMultiCheck.setOnAction(e -> settings.setAllowPuttingChangesWithinOneFileIntoDifferentChangelists(allowMultiCheck.isSelected()));

        // 2. Inactive Changelist section
        HBox inactiveHeader = createSectionHeader("Inactive Changelist");

        styleCheck(highlightInactiveCheck);
        highlightInactiveCheck.setSelected(settings.isHighlightFilesFromInactiveChangelists());
        highlightInactiveCheck.setOnAction(e -> settings.setHighlightFilesFromInactiveChangelists(highlightInactiveCheck.isSelected()));

        styleCheck(showDialogInactiveCheck);
        showDialogInactiveCheck.setSelected(settings.isShowDialogOnAttemptToEditFileFromInactiveChangelist());
        showDialogInactiveCheck.setOnAction(e -> settings.setShowDialogOnAttemptToEditFileFromInactiveChangelist(showDialogInactiveCheck.isSelected()));

        HBox emptyActionRow = new HBox(8);
        emptyActionRow.setAlignment(Pos.CENTER_LEFT);
        emptyActionRow.setPadding(new Insets(2, 0, 0, 0));

        Label emptyActionLbl = new Label("When an empty changelist becomes inactive:");
        emptyActionLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        emptyActionCombo.getItems().setAll(VcsChangelistSettings.EmptyChangelistInactiveAction.values());
        emptyActionCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(VcsChangelistSettings.EmptyChangelistInactiveAction object) {
                return object != null ? object.getDisplayName() : "";
            }
            @Override
            public VcsChangelistSettings.EmptyChangelistInactiveAction fromString(String string) {
                return null;
            }
        });
        emptyActionCombo.setValue(settings.getEmptyChangelistInactiveAction());
        emptyActionCombo.setOnAction(e -> {
            if (emptyActionCombo.getValue() != null) {
                settings.setEmptyChangelistInactiveAction(emptyActionCombo.getValue());
            }
        });
        styleCombo(emptyActionCombo, 140);
        emptyActionRow.getChildren().addAll(emptyActionLbl, emptyActionCombo);

        VBox inactiveBox = new VBox(8, highlightInactiveCheck, showDialogInactiveCheck, emptyActionRow);
        inactiveBox.setPadding(new Insets(0, 0, 0, 16));

        // 3. Conflicts section
        HBox conflictsHeader = createSectionHeader("Conflicts");

        styleCheck(highlightConflictsCheck);
        highlightConflictsCheck.setSelected(settings.isHighlightFilesWithChangelistConflicts());
        highlightConflictsCheck.setOnAction(e -> settings.setHighlightFilesWithChangelistConflicts(highlightConflictsCheck.isSelected()));

        Label ignoredLbl = new Label("Files with ignored conflicts:");
        ignoredLbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-padding: 4 0 0 16;");

        // List Area
        noIgnoredLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        ignoredListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background-radius: 4; -fx-border-color: transparent;");
        ignoredListView.setPrefHeight(150);

        listContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-border-width: 1;");
        listContainer.setPrefHeight(150);
        listContainer.getChildren().setAll(noIgnoredLabel);
        StackPane.setAlignment(noIgnoredLabel, Pos.CENTER);

        VBox listWrapper = new VBox(ignoredLbl, listContainer);
        listWrapper.setPadding(new Insets(0, 0, 0, 16));
        listWrapper.setSpacing(6);

        // Clear button
        clearBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #848BA3; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            settings.clearIgnoredConflicts();
            updateIgnoredList();
        });
        HBox clearBtnBox = new HBox(clearBtn);
        clearBtnBox.setAlignment(Pos.CENTER_RIGHT);
        clearBtnBox.setPadding(new Insets(2, 0, 0, 16));

        updateIgnoredList();

        getChildren().addAll(
                autoCreateCheck,
                allowMultiCheck,
                inactiveHeader,
                inactiveBox,
                conflictsHeader,
                highlightConflictsCheck,
                listWrapper,
                clearBtnBox
        );

        settings.addListener(this::syncFromSettings);
    }

    private void updateIgnoredList() {
        var list = settings.getIgnoredConflicts();
        if (list.isEmpty()) {
            listContainer.getChildren().setAll(noIgnoredLabel);
            clearBtn.setDisable(true);
            clearBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #55575E; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-opacity: 0.6;");
        } else {
            ignoredListView.getItems().setAll(list);
            listContainer.getChildren().setAll(ignoredListView);
            clearBtn.setDisable(false);
            clearBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;");
        }
    }

    private void syncFromSettings() {
        autoCreateCheck.setSelected(settings.isCreateChangelistsAutomatically());
        allowMultiCheck.setSelected(settings.isAllowPuttingChangesWithinOneFileIntoDifferentChangelists());
        highlightInactiveCheck.setSelected(settings.isHighlightFilesFromInactiveChangelists());
        showDialogInactiveCheck.setSelected(settings.isShowDialogOnAttemptToEditFileFromInactiveChangelist());
        emptyActionCombo.setValue(settings.getEmptyChangelistInactiveAction());
        highlightConflictsCheck.setSelected(settings.isHighlightFilesWithChangelistConflicts());
        updateIgnoredList();
    }

    private static HBox createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 0, 4, 0));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(lbl, line);
        return header;
    }

    private static void styleCheck(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private static void styleCombo(ComboBox<?> combo, double width) {
        combo.setPrefWidth(width);
        combo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px;");
    }
}
