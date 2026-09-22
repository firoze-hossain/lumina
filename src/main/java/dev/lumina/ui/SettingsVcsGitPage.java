package dev.lumina.ui;

import dev.lumina.git.GitSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Version Control > Git settings page matching IntelliJ IDEA Images 1–4.
 */
public class SettingsVcsGitPage extends VBox {

    private final GitSettingsManager manager = GitSettingsManager.getInstance();

    // 1. Executable
    private final TextField gitPathField = new TextField();
    private final Button browseBtn = new Button();
    private final Button testBtn = new Button("Test");
    private final Label testResultLabel = new Label();
    private final CheckBox setPathOnlyForProjectCheck = new CheckBox("Set this path only for the current project");
    private final CheckBox autoExcludeIgnoredCheck = new CheckBox("Automatically exclude ignored directories from the analysis");

    // 2. Commit Section
    private final CheckBox enableStagingCheck = new CheckBox("Enable staging area");
    private final CheckBox warnCrlfCheck = new CheckBox("Warn if CRLF line separators are about to be committed");
    private final CheckBox warnDetachedCheck = new CheckBox("Warn when committing in detached HEAD or during rebase");
    private final CheckBox warnFilesLargerCheck = new CheckBox("Warn when committing files larger than");
    private final Spinner<Integer> warnFilesLargerSpinner = new Spinner<>(1, 10000, 50);
    private final CheckBox warnFilenamesCheck = new CheckBox("Warn when committing files with names that might cause issues on other systems");
    private final CheckBox addCherryPickSuffixCheck = new CheckBox("Add the 'cherry-picked from <hash>' suffix when picking commits pushed to protected branches");
    private final Button configureGpgBtn = new Button("Configure GPG Key...");
    private final Label gpgStatusLabel = new Label();

    // 3. Push Section
    private final CheckBox autoUpdateRejectedPushCheck = new CheckBox("Auto-update if push of the current branch was rejected");
    private final CheckBox showPushDialogCheck = new CheckBox("Show Push dialog for Commit and Push");
    private final CheckBox showPushOnlyProtectedCheck = new CheckBox("Show Push dialog only when committing to protected branches");
    private final TextField protectedBranchesField = new TextField();
    private final CheckBox loadBranchProtectionCheck = new CheckBox("Load branch protection rules from GitHub");

    // 4. Update Section
    private final RadioButton updateMergeRadio = new RadioButton("Merge");
    private final RadioButton updateRebaseRadio = new RadioButton("Rebase");
    private final RadioButton cleanStashRadio = new RadioButton("Stash");
    private final RadioButton cleanShelveRadio = new RadioButton("Shelve");
    private final ComboBox<String> filterUpdatePathsCombo = new ComboBox<>();
    private final ComboBox<String> incomingCommitsCombo = new ComboBox<>();
    private final ComboBox<String> fetchTagsCombo = new ComboBox<>();
    private final CheckBox useCredentialHelperCheck = new CheckBox("Use credential helper");

    // 5. Stash Section
    private final CheckBox combineStashesShelvesCheck = new CheckBox("Combine stashes and shelves in one tab");
    private final RadioButton stashLocalVersionRadio = new RadioButton("With the local version of a file");
    private final RadioButton stashParentCommitRadio = new RadioButton("With the parent commit");
    private final CheckBox activateVirtualenvCheck = new CheckBox("Activate virtualenv for hooks");

    public SettingsVcsGitPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 24, 20));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Top Executable Controls
        VBox execBox = buildExecutableSection();

        // 2. Commit Section
        VBox commitBox = buildCommitSection();

        // 3. Push Section
        VBox pushBox = buildPushSection();

        // 4. Update Section
        VBox updateBox = buildUpdateSection();

        // 5. Stash Section
        VBox stashBox = buildStashSection();

        getChildren().addAll(execBox, commitBox, pushBox, updateBox, stashBox);

        manager.addListener(this::syncFromManager);
        syncFromManager();
    }

    private VBox buildExecutableSection() {
        VBox box = new VBox(6);

        Label pathLabel = new Label("Path to Git executable:");
        pathLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        String currentPath = manager.getGitExecutablePath();
        String autoDetected = manager.getAutoDetectedGitPath();
        gitPathField.setText(currentPath.isEmpty() ? "Auto-detected: " + autoDetected : currentPath);
        gitPathField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(gitPathField, Priority.ALWAYS);

        gitPathField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.startsWith("Auto-detected:")) {
                manager.setGitExecutablePath(newV.trim());
            }
        });

        // 📁 folder browse button
        SVGPath folderIcon = new SVGPath();
        folderIcon.setContent("M 2 3 L 5 3 L 6.5 5 L 12 5 L 12 11 L 2 11 Z");
        folderIcon.setFill(Color.TRANSPARENT);
        folderIcon.setStroke(Color.web("#848BA3"));
        folderIcon.setStrokeWidth(1.2);
        browseBtn.setGraphic(folderIcon);
        browseBtn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Git Executable");
            File f = fc.showOpenDialog(getScene().getWindow());
            if (f != null) {
                gitPathField.setText(f.getAbsolutePath());
                manager.setGitExecutablePath(f.getAbsolutePath());
            }
        });

        // Test button
        testBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;");
        testBtn.setOnAction(e -> onTestGit());

        HBox pathRow = new HBox(8, pathLabel, gitPathField, browseBtn, testBtn);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        testResultLabel.setStyle("-fx-font-size: 11px; -fx-padding: 0 0 0 145;");
        testResultLabel.setVisible(false);
        testResultLabel.setManaged(false);

        initCheckBox(setPathOnlyForProjectCheck, manager.isSetPathOnlyForProject(), e -> manager.setSetPathOnlyForProject(setPathOnlyForProjectCheck.isSelected()));
        setPathOnlyForProjectCheck.setPadding(new Insets(0, 0, 0, 145));

        initCheckBox(autoExcludeIgnoredCheck, manager.isAutoExcludeIgnoredDirectories(), e -> manager.setAutoExcludeIgnoredDirectories(autoExcludeIgnoredCheck.isSelected()));

        box.getChildren().addAll(pathRow, testResultLabel, setPathOnlyForProjectCheck, autoExcludeIgnoredCheck);
        return box;
    }

    private void onTestGit() {
        String input = gitPathField.getText().trim();
        String toTest = input.startsWith("Auto-detected:") ? "" : input;
        GitSettingsManager.TestResult res = manager.testGitExecutable(toTest);

        testResultLabel.setText(res.message());
        testResultLabel.setStyle("-fx-font-size: 11px; -fx-padding: 0 0 0 145; -fx-text-fill: " + (res.success() ? "#73BD79" : "#ED5E62") + ";");
        testResultLabel.setVisible(true);
        testResultLabel.setManaged(true);
    }

    private VBox buildCommitSection() {
        VBox box = new VBox(8);
        box.getChildren().add(createSectionHeader("Commit"));

        initCheckBox(enableStagingCheck, manager.isEnableStagingArea(), e -> manager.setEnableStagingArea(enableStagingCheck.isSelected()));
        Label stagingDesc = new Label("This will disable changelists support. Only for non-modal commit interface.");
        stagingDesc.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        stagingDesc.setPadding(new Insets(0, 0, 2, 24));

        initCheckBox(warnCrlfCheck, manager.isWarnCrlf(), e -> manager.setWarnCrlf(warnCrlfCheck.isSelected()));
        initCheckBox(warnDetachedCheck, manager.isWarnDetachedHead(), e -> manager.setWarnDetachedHead(warnDetachedCheck.isSelected()));

        // Larger than files spinner
        initCheckBox(warnFilesLargerCheck, manager.isWarnFilesLargerThanEnabled(), e -> manager.setWarnFilesLargerThanEnabled(warnFilesLargerCheck.isSelected()));
        warnFilesLargerSpinner.getValueFactory().setValue(manager.getWarnFilesLargerThanMb());
        warnFilesLargerSpinner.setPrefWidth(70);
        warnFilesLargerSpinner.setStyle("-fx-background-color: #1E1F22; -fx-font-size: 12px;");
        warnFilesLargerSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setWarnFilesLargerThanMb(newV);
        });
        warnFilesLargerSpinner.disableProperty().bind(warnFilesLargerCheck.selectedProperty().not());

        Label mbLabel = new Label("MB");
        mbLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox largerBox = new HBox(8, warnFilesLargerCheck, warnFilesLargerSpinner, mbLabel);
        largerBox.setAlignment(Pos.CENTER_LEFT);

        initCheckBox(warnFilenamesCheck, manager.isWarnCrossPlatformFilenames(), e -> manager.setWarnCrossPlatformFilenames(warnFilenamesCheck.isSelected()));
        initCheckBox(addCherryPickSuffixCheck, manager.isAddCherryPickSuffix(), e -> manager.setAddCherryPickSuffix(addCherryPickSuffixCheck.isSelected()));

        // GPG Key button & label
        configureGpgBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12 4 12; -fx-font-size: 12px; -fx-cursor: hand;");
        configureGpgBtn.setOnAction(e -> {
            Stage owner = (Stage) getScene().getWindow();
            new ConfigureGpgKeyDialog(owner).showAndWait();
            updateGpgLabel();
        });

        gpgStatusLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        updateGpgLabel();

        HBox gpgRow = new HBox(12, configureGpgBtn, gpgStatusLabel);
        gpgRow.setAlignment(Pos.CENTER_LEFT);
        gpgRow.setPadding(new Insets(2, 0, 0, 0));

        box.getChildren().addAll(
                enableStagingCheck, stagingDesc,
                warnCrlfCheck, warnDetachedCheck, largerBox,
                warnFilenamesCheck, addCherryPickSuffixCheck, gpgRow
        );
        return box;
    }

    private void updateGpgLabel() {
        if (manager.isSignCommitsWithGpg()) {
            String key = manager.getGpgKeyId();
            gpgStatusLabel.setText(key.isEmpty() ? "Signed with default key" : "Signed with key " + key);
        } else {
            gpgStatusLabel.setText("Commits signing with GPG key is not configured");
        }
    }

    private VBox buildPushSection() {
        VBox box = new VBox(8);
        box.getChildren().add(createSectionHeader("Push"));

        initCheckBox(autoUpdateRejectedPushCheck, manager.isAutoUpdateOnRejectedPush(), e -> manager.setAutoUpdateOnRejectedPush(autoUpdateRejectedPushCheck.isSelected()));
        initCheckBox(showPushDialogCheck, manager.isShowPushDialog(), e -> {
            manager.setShowPushDialog(showPushDialogCheck.isSelected());
            showPushOnlyProtectedCheck.setDisable(!showPushDialogCheck.isSelected());
        });

        initCheckBox(showPushOnlyProtectedCheck, manager.isShowPushDialogOnlyProtected(), e -> manager.setShowPushDialogOnlyProtected(showPushOnlyProtectedCheck.isSelected()));
        showPushOnlyProtectedCheck.setPadding(new Insets(0, 0, 0, 24));
        showPushOnlyProtectedCheck.setDisable(!manager.isShowPushDialog());

        // Protected branches
        Label protectedLabel = new Label("Protected branches:");
        protectedLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        protectedBranchesField.setText(manager.getProtectedBranches());
        protectedBranchesField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(protectedBranchesField, Priority.ALWAYS);
        protectedBranchesField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setProtectedBranches(newV);
        });

        Label expandIcon = new Label("⤢");
        expandIcon.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0 4 0 0;");

        HBox protectedRow = new HBox(8, protectedLabel, protectedBranchesField, expandIcon);
        protectedRow.setAlignment(Pos.CENTER_LEFT);

        initCheckBox(loadBranchProtectionCheck, manager.isLoadBranchProtectionFromGitHub(), e -> manager.setLoadBranchProtectionFromGitHub(loadBranchProtectionCheck.isSelected()));
        loadBranchProtectionCheck.setPadding(new Insets(0, 0, 0, 24));

        Label ghDesc = new Label("GitHub rules are added to the local rules and synced on every fetch");
        ghDesc.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        ghDesc.setPadding(new Insets(0, 0, 0, 48));

        box.getChildren().addAll(
                autoUpdateRejectedPushCheck, showPushDialogCheck, showPushOnlyProtectedCheck,
                protectedRow, loadBranchProtectionCheck, ghDesc
        );
        return box;
    }

    private VBox buildUpdateSection() {
        VBox box = new VBox(8);
        box.getChildren().add(createSectionHeader("Update"));

        // Update method radio
        ToggleGroup updateGroup = new ToggleGroup();
        updateMergeRadio.setToggleGroup(updateGroup);
        updateRebaseRadio.setToggleGroup(updateGroup);
        initRadio(updateMergeRadio, manager.getUpdateMethod() == GitSettingsManager.UpdateMethod.MERGE);
        initRadio(updateRebaseRadio, manager.getUpdateMethod() == GitSettingsManager.UpdateMethod.REBASE);
        updateGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            manager.setUpdateMethod(newV == updateRebaseRadio ? GitSettingsManager.UpdateMethod.REBASE : GitSettingsManager.UpdateMethod.MERGE);
        });

        Label updateMethodLabel = new Label("Update method:");
        updateMethodLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox updateMethodRow = new HBox(12, updateMethodLabel, updateMergeRadio, updateRebaseRadio);
        updateMethodRow.setAlignment(Pos.CENTER_LEFT);

        // Clean working tree radio
        ToggleGroup cleanGroup = new ToggleGroup();
        cleanStashRadio.setToggleGroup(cleanGroup);
        cleanShelveRadio.setToggleGroup(cleanGroup);
        initRadio(cleanStashRadio, manager.getCleanWorkingTreeMethod() == GitSettingsManager.CleanWorkingTreeMethod.STASH);
        initRadio(cleanShelveRadio, manager.getCleanWorkingTreeMethod() == GitSettingsManager.CleanWorkingTreeMethod.SHELVE);
        cleanGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            manager.setCleanWorkingTreeMethod(newV == cleanStashRadio ? GitSettingsManager.CleanWorkingTreeMethod.STASH : GitSettingsManager.CleanWorkingTreeMethod.SHELVE);
        });

        Label cleanLabel = new Label("Clean working tree using:");
        cleanLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox cleanRow = new HBox(12, cleanLabel, cleanStashRadio, cleanShelveRadio);
        cleanRow.setAlignment(Pos.CENTER_LEFT);

        // Filter Update Project dropdown
        Label filterLabel = new Label("Filter \"Update Project\" information by paths:");
        filterLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        filterUpdatePathsCombo.getItems().addAll("All", "Select...", "Select in Tree...");
        filterUpdatePathsCombo.setValue(manager.getFilterUpdatePaths().getLabel());
        styleCombo(filterUpdatePathsCombo);
        filterUpdatePathsCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if ("Select...".equals(newV)) manager.setFilterUpdatePaths(GitSettingsManager.FilterUpdatePaths.SELECT);
            else if ("Select in Tree...".equals(newV)) manager.setFilterUpdatePaths(GitSettingsManager.FilterUpdatePaths.SELECT_IN_TREE);
            else manager.setFilterUpdatePaths(GitSettingsManager.FilterUpdatePaths.ALL);
        });
        HBox filterRow = new HBox(8, filterLabel, filterUpdatePathsCombo);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        // Explicitly check for incoming commits dropdown
        Label incomingLabel = new Label("Explicitly check for incoming commits on remotes:");
        incomingLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        incomingCommitsCombo.getItems().addAll("Auto", "Always", "Never");
        incomingCommitsCombo.setValue(manager.getIncomingCommitsCheck().getLabel());
        styleCombo(incomingCommitsCombo);
        incomingCommitsCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if ("Always".equals(newV)) manager.setIncomingCommitsCheck(GitSettingsManager.IncomingCommitsCheck.ALWAYS);
            else if ("Never".equals(newV)) manager.setIncomingCommitsCheck(GitSettingsManager.IncomingCommitsCheck.NEVER);
            else manager.setIncomingCommitsCheck(GitSettingsManager.IncomingCommitsCheck.AUTO);
        });
        HBox incomingRow = new HBox(8, incomingLabel, incomingCommitsCombo);
        incomingRow.setAlignment(Pos.CENTER_LEFT);

        // Fetch tags dropdown
        Label fetchTagsLabel = new Label("Fetch tags:");
        fetchTagsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        fetchTagsCombo.getItems().addAll(
                "Auto Follow git config",
                "Sync --prune-tags",
                "Always --tags",
                "Never --no-tags"
        );
        fetchTagsCombo.setValue(manager.getFetchTagsMode().getLabel());
        styleCombo(fetchTagsCombo);
        fetchTagsCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if ("Sync --prune-tags".equals(newV)) manager.setFetchTagsMode(GitSettingsManager.FetchTagsMode.SYNC_PRUNE_TAGS);
            else if ("Always --tags".equals(newV)) manager.setFetchTagsMode(GitSettingsManager.FetchTagsMode.ALWAYS_TAGS);
            else if ("Never --no-tags".equals(newV)) manager.setFetchTagsMode(GitSettingsManager.FetchTagsMode.NEVER_NO_TAGS);
            else manager.setFetchTagsMode(GitSettingsManager.FetchTagsMode.AUTO_FOLLOW_GIT_CONFIG);
        });
        HBox fetchTagsRow = new HBox(8, fetchTagsLabel, fetchTagsCombo);
        fetchTagsRow.setAlignment(Pos.CENTER_LEFT);

        initCheckBox(useCredentialHelperCheck, manager.isUseCredentialHelper(), e -> manager.setUseCredentialHelper(useCredentialHelperCheck.isSelected()));

        box.getChildren().addAll(
                updateMethodRow, cleanRow, filterRow, incomingRow, fetchTagsRow, useCredentialHelperCheck
        );
        return box;
    }

    private VBox buildStashSection() {
        VBox box = new VBox(8);
        box.getChildren().add(createSectionHeader("Stash"));

        initCheckBox(combineStashesShelvesCheck, manager.isCombineStashesAndShelves(), e -> manager.setCombineStashesAndShelves(combineStashesShelvesCheck.isSelected()));

        Label compareLabel = new Label("When \"Show Diff\" is called, compare stashed changes:");
        compareLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        ToggleGroup stashGroup = new ToggleGroup();
        stashLocalVersionRadio.setToggleGroup(stashGroup);
        stashParentCommitRadio.setToggleGroup(stashGroup);
        initRadio(stashLocalVersionRadio, manager.getStashDiffComparison() == GitSettingsManager.StashDiffComparison.LOCAL_VERSION);
        initRadio(stashParentCommitRadio, manager.getStashDiffComparison() == GitSettingsManager.StashDiffComparison.PARENT_COMMIT);
        stashLocalVersionRadio.setPadding(new Insets(0, 0, 0, 24));
        stashParentCommitRadio.setPadding(new Insets(0, 0, 0, 24));

        stashGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            manager.setStashDiffComparison(newV == stashParentCommitRadio
                    ? GitSettingsManager.StashDiffComparison.PARENT_COMMIT
                    : GitSettingsManager.StashDiffComparison.LOCAL_VERSION);
        });

        initCheckBox(activateVirtualenvCheck, manager.isActivateVirtualenvForHooks(), e -> manager.setActivateVirtualenvForHooks(activateVirtualenvCheck.isSelected()));

        box.getChildren().addAll(
                combineStashesShelvesCheck, compareLabel, stashLocalVersionRadio, stashParentCommitRadio, activateVirtualenvCheck
        );
        return box;
    }

    private HBox createSectionHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-border-color: #393B40;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        HBox bar = new HBox(8, lbl, sep);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 0, 2, 0));
        return bar;
    }

    private void initCheckBox(CheckBox cb, boolean initial, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        cb.setSelected(initial);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        cb.setOnAction(onAction);
    }

    private void initRadio(RadioButton rb, boolean initial) {
        rb.setSelected(initial);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private void styleCombo(ComboBox<String> combo) {
        combo.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
    }

    private void syncFromManager() {
        setPathOnlyForProjectCheck.setSelected(manager.isSetPathOnlyForProject());
        autoExcludeIgnoredCheck.setSelected(manager.isAutoExcludeIgnoredDirectories());

        enableStagingCheck.setSelected(manager.isEnableStagingArea());
        warnCrlfCheck.setSelected(manager.isWarnCrlf());
        warnDetachedCheck.setSelected(manager.isWarnDetachedHead());
        warnFilesLargerCheck.setSelected(manager.isWarnFilesLargerThanEnabled());
        warnFilesLargerSpinner.getValueFactory().setValue(manager.getWarnFilesLargerThanMb());
        warnFilenamesCheck.setSelected(manager.isWarnCrossPlatformFilenames());
        addCherryPickSuffixCheck.setSelected(manager.isAddCherryPickSuffix());
        updateGpgLabel();

        autoUpdateRejectedPushCheck.setSelected(manager.isAutoUpdateOnRejectedPush());
        showPushDialogCheck.setSelected(manager.isShowPushDialog());
        showPushOnlyProtectedCheck.setSelected(manager.isShowPushDialogOnlyProtected());
        showPushOnlyProtectedCheck.setDisable(!manager.isShowPushDialog());
        protectedBranchesField.setText(manager.getProtectedBranches());
        loadBranchProtectionCheck.setSelected(manager.isLoadBranchProtectionFromGitHub());

        if (manager.getUpdateMethod() == GitSettingsManager.UpdateMethod.REBASE) {
            updateRebaseRadio.setSelected(true);
        } else {
            updateMergeRadio.setSelected(true);
        }

        if (manager.getCleanWorkingTreeMethod() == GitSettingsManager.CleanWorkingTreeMethod.STASH) {
            cleanStashRadio.setSelected(true);
        } else {
            cleanShelveRadio.setSelected(true);
        }

        filterUpdatePathsCombo.setValue(manager.getFilterUpdatePaths().getLabel());
        incomingCommitsCombo.setValue(manager.getIncomingCommitsCheck().getLabel());
        fetchTagsCombo.setValue(manager.getFetchTagsMode().getLabel());
        useCredentialHelperCheck.setSelected(manager.isUseCredentialHelper());

        combineStashesShelvesCheck.setSelected(manager.isCombineStashesAndShelves());
        if (manager.getStashDiffComparison() == GitSettingsManager.StashDiffComparison.PARENT_COMMIT) {
            stashParentCommitRadio.setSelected(true);
        } else {
            stashLocalVersionRadio.setSelected(true);
        }

        activateVirtualenvCheck.setSelected(manager.isActivateVirtualenvForHooks());
    }
}
