package dev.lumina.ui;

import dev.lumina.git.GitConfirmationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Settings page for Version Control > Confirmation.
 * Strictly matches IntelliJ IDEA Image 5.
 */
public class SettingsVcsConfirmationPage extends VBox {

    private final GitConfirmationManager manager = GitConfirmationManager.getInstance();

    // Confirmation section
    private final ComboBox<GitConfirmationManager.FileCreationPolicy> fileCreationCombo = new ComboBox<>();
    private final CheckBox applyCreationExternalCheck = new CheckBox("Apply to files created outside Lumina");
    private final ComboBox<GitConfirmationManager.FileDeletionPolicy> fileDeletionCombo = new ComboBox<>();
    private final CheckBox showBeforeCheckoutCheck = new CheckBox("Checkout");
    private final CheckBox showBeforeUpdateCheck = new CheckBox("Update");
    private final CheckBox askUnlockReadOnlyCheck = new CheckBox("Ask to unlock files set to read-only if you attempt to edit them");
    private final CheckBox askDropCommitsCheck = new CheckBox("Ask for confirmation to drop commits");

    // Changes section
    private final CheckBox checkServerConflictsCheck = new CheckBox("Check for conflicts with the server every");
    private final Spinner<Integer> conflictMinutesSpinner = new Spinner<>(1, 1440, 60, 5);
    private final CheckBox highlightChangedDaysCheck = new CheckBox("Highlight files changed in the last");
    private final Spinner<Integer> highlightDaysSpinner = new Spinner<>(1, 365, 31, 1);
    private final CheckBox highlightDirsCheck = new CheckBox("Highlight directories that contain modified files in the Project tree");
    private final ComboBox<GitConfirmationManager.PatchCreationPolicy> patchCreationCombo = new ComboBox<>();
    private final CheckBox restoreWorkspaceCheck = new CheckBox("Restore workspace when switching branches");
    private final CheckBox limitHistoryCheck = new CheckBox("Limit history to");
    private final Spinner<Integer> limitHistorySpinner = new Spinner<>(10, 50000, 1000, 100);

    // Gutter section
    private final CheckBox highlightGutterCheck = new CheckBox("Highlight modified lines in the gutter");
    private final CheckBox highlightErrorStripeCheck = new CheckBox("Highlight modified lines in error stripe on the scrollbar");
    private final CheckBox highlightWhitespaceCheck = new CheckBox("Highlight lines with whitespace-only modifications with a different color");

    public SettingsVcsConfirmationPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Confirmation section
        HBox confirmationHeader = createSectionHeader("Confirmation");
        VBox confirmationBox = buildConfirmationBox();

        // 2. Changes section
        HBox changesHeader = createSectionHeader("Changes");
        VBox changesBox = buildChangesBox();

        // 3. Gutter section
        HBox gutterHeader = createSectionHeader("Gutter");
        VBox gutterBox = buildGutterBox();

        getChildren().addAll(
                confirmationHeader,
                confirmationBox,
                changesHeader,
                changesBox,
                gutterHeader,
                gutterBox
        );

        manager.addListener(this::syncFromManager);
    }

    private VBox buildConfirmationBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(0, 0, 0, 16));

        // When files are created row
        Label creationLbl = new Label("When files are created:");
        creationLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        fileCreationCombo.getItems().setAll(GitConfirmationManager.FileCreationPolicy.values());
        fileCreationCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(GitConfirmationManager.FileCreationPolicy p) {
                return p != null ? p.getDisplayName() : "";
            }
            @Override
            public GitConfirmationManager.FileCreationPolicy fromString(String string) {
                return null;
            }
        });
        fileCreationCombo.setValue(manager.getFileCreationPolicy());
        fileCreationCombo.setOnAction(e -> {
            if (fileCreationCombo.getValue() != null) {
                manager.setFileCreationPolicy(fileCreationCombo.getValue());
            }
        });
        styleCombo(fileCreationCombo, 130);

        styleCheck(applyCreationExternalCheck);
        applyCreationExternalCheck.setSelected(manager.isApplyCreationPolicyToExternalFiles());
        applyCreationExternalCheck.setOnAction(e -> manager.setApplyCreationPolicyToExternalFiles(applyCreationExternalCheck.isSelected()));

        HBox creationRow = new HBox(8, creationLbl, fileCreationCombo, applyCreationExternalCheck);
        creationRow.setAlignment(Pos.CENTER_LEFT);

        // When files are deleted row
        Label deletionLbl = new Label("When files are deleted:");
        deletionLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        fileDeletionCombo.getItems().setAll(GitConfirmationManager.FileDeletionPolicy.values());
        fileDeletionCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(GitConfirmationManager.FileDeletionPolicy p) {
                return p != null ? p.getDisplayName() : "";
            }
            @Override
            public GitConfirmationManager.FileDeletionPolicy fromString(String string) {
                return null;
            }
        });
        fileDeletionCombo.setValue(manager.getFileDeletionPolicy());
        fileDeletionCombo.setOnAction(e -> {
            if (fileDeletionCombo.getValue() != null) {
                manager.setFileDeletionPolicy(fileDeletionCombo.getValue());
            }
        });
        styleCombo(fileDeletionCombo, 130);

        HBox deletionRow = new HBox(8, deletionLbl, fileDeletionCombo);
        deletionRow.setAlignment(Pos.CENTER_LEFT);

        // Show options before row
        Label showBeforeLbl = new Label("Show options before:");
        showBeforeLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        styleCheck(showBeforeCheckoutCheck);
        showBeforeCheckoutCheck.setSelected(manager.isShowOptionsBeforeCheckout());
        showBeforeCheckoutCheck.setOnAction(e -> manager.setShowOptionsBeforeCheckout(showBeforeCheckoutCheck.isSelected()));

        styleCheck(showBeforeUpdateCheck);
        showBeforeUpdateCheck.setSelected(manager.isShowOptionsBeforeUpdate());
        showBeforeUpdateCheck.setOnAction(e -> manager.setShowOptionsBeforeUpdate(showBeforeUpdateCheck.isSelected()));

        HBox showBeforeRow = new HBox(12, showBeforeLbl, showBeforeCheckoutCheck, showBeforeUpdateCheck);
        showBeforeRow.setAlignment(Pos.CENTER_LEFT);

        // Unlock read-only files
        styleCheck(askUnlockReadOnlyCheck);
        askUnlockReadOnlyCheck.setSelected(manager.isAskToUnlockReadOnlyFiles());
        askUnlockReadOnlyCheck.setOnAction(e -> manager.setAskToUnlockReadOnlyFiles(askUnlockReadOnlyCheck.isSelected()));

        Label unlockSub = new Label("Applicable to Perforce and Subversion");
        unlockSub.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        VBox unlockBox = new VBox(2, askUnlockReadOnlyCheck, unlockSub);

        // Drop commits
        styleCheck(askDropCommitsCheck);
        askDropCommitsCheck.setSelected(manager.isAskConfirmationToDropCommits());
        askDropCommitsCheck.setOnAction(e -> manager.setAskConfirmationToDropCommits(askDropCommitsCheck.isSelected()));

        Label dropSub = new Label("Applicable to Git");
        dropSub.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        VBox dropBox = new VBox(2, askDropCommitsCheck, dropSub);

        box.getChildren().addAll(creationRow, deletionRow, showBeforeRow, unlockBox, dropBox);
        return box;
    }

    private VBox buildChangesBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(0, 0, 0, 16));

        // Server conflicts check
        styleCheck(checkServerConflictsCheck);
        checkServerConflictsCheck.setSelected(manager.isCheckForServerConflicts());
        checkServerConflictsCheck.setOnAction(e -> manager.setCheckForServerConflicts(checkServerConflictsCheck.isSelected()));

        conflictMinutesSpinner.getValueFactory().setValue(manager.getConflictCheckIntervalMinutes());
        conflictMinutesSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setConflictCheckIntervalMinutes(newV);
        });
        styleSpinner(conflictMinutesSpinner, 70);

        Label minutesLbl = new Label("minutes");
        minutesLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox conflictRow = new HBox(8, checkServerConflictsCheck, conflictMinutesSpinner, minutesLbl);
        conflictRow.setAlignment(Pos.CENTER_LEFT);

        // Highlight files changed in days
        styleCheck(highlightChangedDaysCheck);
        highlightChangedDaysCheck.setSelected(manager.isHighlightFilesChangedInDays());
        highlightChangedDaysCheck.setOnAction(e -> manager.setHighlightFilesChangedInDays(highlightChangedDaysCheck.isSelected()));

        highlightDaysSpinner.getValueFactory().setValue(manager.getHighlightDays());
        highlightDaysSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setHighlightDays(newV);
        });
        styleSpinner(highlightDaysSpinner, 60);

        Label daysLbl = new Label("days");
        daysLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox daysRow = new HBox(8, highlightChangedDaysCheck, highlightDaysSpinner, daysLbl);
        daysRow.setAlignment(Pos.CENTER_LEFT);

        Label daysSub = new Label("Modified files will be highlighted in the external stack trace and while debugging.");
        daysSub.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        VBox daysBox = new VBox(2, daysRow, daysSub);

        // Highlight directories with modified files in Project tree
        styleCheck(highlightDirsCheck);
        highlightDirsCheck.setSelected(manager.isHighlightDirectoriesWithModifiedFiles());
        highlightDirsCheck.setOnAction(e -> manager.setHighlightDirectoriesWithModifiedFiles(highlightDirsCheck.isSelected()));

        // Patch created
        Label patchLbl = new Label("When patch is created:");
        patchLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        patchCreationCombo.getItems().setAll(GitConfirmationManager.PatchCreationPolicy.values());
        patchCreationCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(GitConfirmationManager.PatchCreationPolicy p) {
                return p != null ? p.getDisplayName() : "";
            }
            @Override
            public GitConfirmationManager.PatchCreationPolicy fromString(String string) {
                return null;
            }
        });
        patchCreationCombo.setValue(manager.getPatchCreationPolicy());
        patchCreationCombo.setOnAction(e -> {
            if (patchCreationCombo.getValue() != null) {
                manager.setPatchCreationPolicy(patchCreationCombo.getValue());
            }
        });
        styleCombo(patchCreationCombo, 110);

        HBox patchRow = new HBox(8, patchLbl, patchCreationCombo);
        patchRow.setAlignment(Pos.CENTER_LEFT);

        // Restore workspace
        styleCheck(restoreWorkspaceCheck);
        restoreWorkspaceCheck.setSelected(manager.isRestoreWorkspaceWhenSwitchingBranches());
        restoreWorkspaceCheck.setOnAction(e -> manager.setRestoreWorkspaceWhenSwitchingBranches(restoreWorkspaceCheck.isSelected()));

        Label restoreSub = new Label("A workspace is a set of opened files, the current run configuration and breakpoints associated with a branch.");
        restoreSub.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        VBox restoreBox = new VBox(2, restoreWorkspaceCheck, restoreSub);

        // Limit history
        styleCheck(limitHistoryCheck);
        limitHistoryCheck.setSelected(manager.isLimitHistory());
        limitHistoryCheck.setOnAction(e -> manager.setLimitHistory(limitHistoryCheck.isSelected()));

        limitHistorySpinner.getValueFactory().setValue(manager.getHistoryLimitRows());
        limitHistorySpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setHistoryLimitRows(newV);
        });
        styleSpinner(limitHistorySpinner, 80);

        Label rowsLbl = new Label("rows");
        rowsLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox historyRow = new HBox(8, limitHistoryCheck, limitHistorySpinner, rowsLbl);
        historyRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(conflictRow, daysBox, highlightDirsCheck, patchRow, restoreBox, historyRow);
        return box;
    }

    private VBox buildGutterBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(0, 0, 0, 16));

        styleCheck(highlightGutterCheck);
        highlightGutterCheck.setSelected(manager.isHighlightModifiedLinesInGutter());
        highlightGutterCheck.setOnAction(e -> manager.setHighlightModifiedLinesInGutter(highlightGutterCheck.isSelected()));

        styleCheck(highlightErrorStripeCheck);
        highlightErrorStripeCheck.setSelected(manager.isHighlightModifiedLinesInErrorStripe());
        highlightErrorStripeCheck.setOnAction(e -> manager.setHighlightModifiedLinesInErrorStripe(highlightErrorStripeCheck.isSelected()));

        styleCheck(highlightWhitespaceCheck);
        highlightWhitespaceCheck.setSelected(manager.isHighlightWhitespaceOnlyModifications());
        highlightWhitespaceCheck.setOnAction(e -> manager.setHighlightWhitespaceOnlyModifications(highlightWhitespaceCheck.isSelected()));

        box.getChildren().addAll(highlightGutterCheck, highlightErrorStripeCheck, highlightWhitespaceCheck);
        return box;
    }

    private void syncFromManager() {
        fileCreationCombo.setValue(manager.getFileCreationPolicy());
        applyCreationExternalCheck.setSelected(manager.isApplyCreationPolicyToExternalFiles());
        fileDeletionCombo.setValue(manager.getFileDeletionPolicy());
        showBeforeCheckoutCheck.setSelected(manager.isShowOptionsBeforeCheckout());
        showBeforeUpdateCheck.setSelected(manager.isShowOptionsBeforeUpdate());
        askUnlockReadOnlyCheck.setSelected(manager.isAskToUnlockReadOnlyFiles());
        askDropCommitsCheck.setSelected(manager.isAskConfirmationToDropCommits());

        checkServerConflictsCheck.setSelected(manager.isCheckForServerConflicts());
        conflictMinutesSpinner.getValueFactory().setValue(manager.getConflictCheckIntervalMinutes());
        highlightChangedDaysCheck.setSelected(manager.isHighlightFilesChangedInDays());
        highlightDaysSpinner.getValueFactory().setValue(manager.getHighlightDays());
        highlightDirsCheck.setSelected(manager.isHighlightDirectoriesWithModifiedFiles());
        patchCreationCombo.setValue(manager.getPatchCreationPolicy());
        restoreWorkspaceCheck.setSelected(manager.isRestoreWorkspaceWhenSwitchingBranches());
        limitHistoryCheck.setSelected(manager.isLimitHistory());
        limitHistorySpinner.getValueFactory().setValue(manager.getHistoryLimitRows());

        highlightGutterCheck.setSelected(manager.isHighlightModifiedLinesInGutter());
        highlightErrorStripeCheck.setSelected(manager.isHighlightModifiedLinesInErrorStripe());
        highlightWhitespaceCheck.setSelected(manager.isHighlightWhitespaceOnlyModifications());
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

    private static void styleSpinner(Spinner<Integer> spinner, double width) {
        spinner.setPrefWidth(width);
        spinner.setEditable(true);
        spinner.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px;");
    }
}
