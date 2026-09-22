package dev.lumina.ui;

import dev.lumina.git.GitConfirmationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Settings page for Version Control > Confirmation.
 * Strictly matches IntelliJ IDEA's confirmation settings.
 */
public class SettingsVcsConfirmationPage extends VBox {

    private final GitConfirmationManager manager = GitConfirmationManager.getInstance();

    private final RadioButton creationAddSilently = new RadioButton("Add silently");
    private final RadioButton creationDoNotAdd = new RadioButton("Do not add");
    private final RadioButton creationAsk = new RadioButton("Ask");

    private final RadioButton deletionRemoveSilently = new RadioButton("Remove silently");
    private final RadioButton deletionDoNotRemove = new RadioButton("Do not remove");
    private final RadioButton deletionAsk = new RadioButton("Ask");

    private final CheckBox restoreWorkspaceCheck = new CheckBox("Restore workspace on branch switching");
    private final CheckBox promptCheckoutCheck = new CheckBox("Show prompt when checkout files from branch");
    private final CheckBox clearUnversionedCheck = new CheckBox("Clear unversioned files on reload");

    public SettingsVcsConfirmationPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(20);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. "When files are created" Group
        VBox creationSection = buildCreationSection();

        // 2. "When files are deleted" Group
        VBox deletionSection = buildDeletionSection();

        // 3. Additional options
        VBox optionsSection = buildOptionsSection();

        getChildren().addAll(creationSection, deletionSection, optionsSection);

        // Listen to external updates
        manager.addListener(this::syncFromManager);
    }

    private VBox buildCreationSection() {
        VBox box = new VBox(8);

        Label title = new Label("When files are created");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ToggleGroup group = new ToggleGroup();
        styleRadio(creationAddSilently, group);
        styleRadio(creationDoNotAdd, group);
        styleRadio(creationAsk, group);

        switch (manager.getFileCreationPolicy()) {
            case ADD_SILENTLY -> creationAddSilently.setSelected(true);
            case DO_NOT_ADD -> creationDoNotAdd.setSelected(true);
            case ASK -> creationAsk.setSelected(true);
        }

        group.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == creationAddSilently) {
                manager.setFileCreationPolicy(GitConfirmationManager.FileCreationPolicy.ADD_SILENTLY);
            } else if (newV == creationDoNotAdd) {
                manager.setFileCreationPolicy(GitConfirmationManager.FileCreationPolicy.DO_NOT_ADD);
            } else if (newV == creationAsk) {
                manager.setFileCreationPolicy(GitConfirmationManager.FileCreationPolicy.ASK);
            }
        });

        VBox radioList = new VBox(6, creationAddSilently, creationDoNotAdd, creationAsk);
        radioList.setPadding(new Insets(0, 0, 0, 16));

        box.getChildren().addAll(title, radioList);
        return box;
    }

    private VBox buildDeletionSection() {
        VBox box = new VBox(8);

        Label title = new Label("When files are deleted");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ToggleGroup group = new ToggleGroup();
        styleRadio(deletionRemoveSilently, group);
        styleRadio(deletionDoNotRemove, group);
        styleRadio(deletionAsk, group);

        switch (manager.getFileDeletionPolicy()) {
            case REMOVE_SILENTLY -> deletionRemoveSilently.setSelected(true);
            case DO_NOT_REMOVE -> deletionDoNotRemove.setSelected(true);
            case ASK -> deletionAsk.setSelected(true);
        }

        group.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == deletionRemoveSilently) {
                manager.setFileDeletionPolicy(GitConfirmationManager.FileDeletionPolicy.REMOVE_SILENTLY);
            } else if (newV == deletionDoNotRemove) {
                manager.setFileDeletionPolicy(GitConfirmationManager.FileDeletionPolicy.DO_NOT_REMOVE);
            } else if (newV == deletionAsk) {
                manager.setFileDeletionPolicy(GitConfirmationManager.FileDeletionPolicy.ASK);
            }
        });

        VBox radioList = new VBox(6, deletionRemoveSilently, deletionDoNotRemove, deletionAsk);
        radioList.setPadding(new Insets(0, 0, 0, 16));

        box.getChildren().addAll(title, radioList);
        return box;
    }

    private VBox buildOptionsSection() {
        VBox box = new VBox(10);

        styleCheck(restoreWorkspaceCheck);
        restoreWorkspaceCheck.setSelected(manager.isRestoreWorkspaceOnBranchSwitch());
        restoreWorkspaceCheck.setOnAction(e -> manager.setRestoreWorkspaceOnBranchSwitch(restoreWorkspaceCheck.isSelected()));

        styleCheck(promptCheckoutCheck);
        promptCheckoutCheck.setSelected(manager.isShowPromptWhenCheckoutFiles());
        promptCheckoutCheck.setOnAction(e -> manager.setShowPromptWhenCheckoutFiles(promptCheckoutCheck.isSelected()));

        styleCheck(clearUnversionedCheck);
        clearUnversionedCheck.setSelected(manager.isClearUnversionedFilesOnReload());
        clearUnversionedCheck.setOnAction(e -> manager.setClearUnversionedFilesOnReload(clearUnversionedCheck.isSelected()));

        box.getChildren().addAll(restoreWorkspaceCheck, promptCheckoutCheck, clearUnversionedCheck);
        return box;
    }

    private void styleRadio(RadioButton rb, ToggleGroup group) {
        rb.setToggleGroup(group);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void styleCheck(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void syncFromManager() {
        switch (manager.getFileCreationPolicy()) {
            case ADD_SILENTLY -> creationAddSilently.setSelected(true);
            case DO_NOT_ADD -> creationDoNotAdd.setSelected(true);
            case ASK -> creationAsk.setSelected(true);
        }
        switch (manager.getFileDeletionPolicy()) {
            case REMOVE_SILENTLY -> deletionRemoveSilently.setSelected(true);
            case DO_NOT_REMOVE -> deletionDoNotRemove.setSelected(true);
            case ASK -> deletionAsk.setSelected(true);
        }
        restoreWorkspaceCheck.setSelected(manager.isRestoreWorkspaceOnBranchSwitch());
        promptCheckoutCheck.setSelected(manager.isShowPromptWhenCheckoutFiles());
        clearUnversionedCheck.setSelected(manager.isClearUnversionedFilesOnReload());
    }
}
