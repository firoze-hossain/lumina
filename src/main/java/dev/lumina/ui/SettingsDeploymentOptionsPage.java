package dev.lumina.ui;

import dev.lumina.deployment.DeploymentOptions;
import dev.lumina.deployment.DeploymentSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Settings page for Build, Execution, Deployment > Deployment > Options (Screenshot 2).
 * Accurately replicates the UI and behavior shown in IntelliJ IDEA.
 */
public class SettingsDeploymentOptionsPage extends VBox {

    private final DeploymentSettingsManager manager = DeploymentSettingsManager.getInstance();

    private final TextField excludeItemsField = new TextField();
    private final ComboBox<String> operationsLoggingCombo = new ComboBox<>();

    private final CheckBox overwriteUpToDateCheck = new CheckBox("Overwrite up-to-date files");
    private final CheckBox useTempFileCheck = new CheckBox("Use a temporary file during upload");
    private final CheckBox preserveTimestampsCheck = new CheckBox("Preserve file timestamps");
    private final CheckBox deleteTargetNotExistCheck = new CheckBox("Delete target items when source ones do not exist (when transferring from Project view or Remote Host view)");
    private final CheckBox confirmDeletingRemoteCheck = new CheckBox("Confirm deleting remote files (except rsync)");
    private final CheckBox createEmptyDirsCheck = new CheckBox("Create empty directories");
    private final CheckBox promptOverwritingDeletingCheck = new CheckBox("Prompt when overwriting or deleting local items");
    private final CheckBox confirmUploadingFilesCheck = new CheckBox("Confirm uploading files");

    private final ComboBox<String> uploadChangedAutoCombo = new ComboBox<>();
    private final CheckBox skipExternalChangesCheck = new CheckBox("Skip external changes");
    private final CheckBox deleteRemoteWhenLocalDeletedCheck = new CheckBox("Delete remote files when local are deleted");

    private final ToggleGroup preservePermissionsGroup = new ToggleGroup();
    private final RadioButton preservePermNoRadio = new RadioButton("No");
    private final RadioButton preservePermYesRadio = new RadioButton("Yes");

    private final CheckBox overridePermFilesCheck = new CheckBox("Override default permissions on files:");
    private final TextField permFilesField = new TextField("(none)");
    private final Button permFilesBrowseBtn = new Button("...");

    private final CheckBox overridePermFoldersCheck = new CheckBox("Override default permissions on folders:");
    private final TextField permFoldersField = new TextField("(none)");
    private final Button permFoldersBrowseBtn = new Button("...");

    private final ComboBox<String> warnUploadingNewerCombo = new ComboBox<>();
    private final CheckBox notifyRemoteChangesCheck = new CheckBox("Notify of remote changes");

    private Consumer<String> navigationHandler;
    private DeploymentOptions initialOptions;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDeploymentOptionsPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 24, 24));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    public void setNavigationHandler(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
    }

    private void buildUI() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox content = new VBox(11);
        content.setStyle("-fx-background-color: #1E1F22;");
        content.setPadding(new Insets(4, 8, 20, 4));

        // 1. Exclude items by name
        HBox excludeRow = new HBox(12);
        excludeRow.setAlignment(Pos.CENTER_LEFT);
        Label excludeLbl = createLabel("Exclude items by name:");
        excludeLbl.setPrefWidth(180);

        excludeItemsField.setPrefWidth(420);
        styleTextField(excludeItemsField);
        excludeItemsField.textProperty().addListener((obs, o, n) -> checkModified());
        excludeRow.getChildren().addAll(excludeLbl, excludeItemsField);

        Label excludeHint = new Label("use ; as delimiter, * and ? as wildcards");
        excludeHint.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");
        VBox.setMargin(excludeHint, new Insets(-6, 0, 4, 192));

        // 2. Operations logging
        HBox loggingRow = new HBox(12);
        loggingRow.setAlignment(Pos.CENTER_LEFT);
        Label loggingLbl = createLabel("Operations logging:");
        loggingLbl.setPrefWidth(180);

        operationsLoggingCombo.getItems().addAll("Details", "Errors only", "Normal", "None");
        styleComboBox(operationsLoggingCombo);
        operationsLoggingCombo.valueProperty().addListener((obs, o, n) -> checkModified());
        loggingRow.getChildren().addAll(loggingLbl, operationsLoggingCombo);

        // 3. Main Checkboxes
        styleCheckBox(overwriteUpToDateCheck);
        styleCheckBox(useTempFileCheck);
        styleCheckBox(preserveTimestampsCheck);
        styleCheckBox(deleteTargetNotExistCheck);
        styleCheckBox(confirmDeletingRemoteCheck);
        styleCheckBox(createEmptyDirsCheck);
        styleCheckBox(promptOverwritingDeletingCheck);
        styleCheckBox(confirmUploadingFilesCheck);

        // 4. Upload changed files automatically
        HBox uploadAutoRow = new HBox(12);
        uploadAutoRow.setAlignment(Pos.CENTER_LEFT);
        Label uploadAutoLbl = createLabel("Upload changed files automatically to the default server:");
        uploadAutoLbl.setPrefWidth(350);

        uploadChangedAutoCombo.getItems().addAll("Never", "On explicit save action (Ctrl+S)", "Always");
        styleComboBox(uploadChangedAutoCombo);
        uploadChangedAutoCombo.valueProperty().addListener((obs, o, n) -> {
            boolean active = !"Never".equals(n);
            skipExternalChangesCheck.setDisable(!active);
            deleteRemoteWhenLocalDeletedCheck.setDisable(!active);
            checkModified();
        });
        uploadAutoRow.getChildren().addAll(uploadAutoLbl, uploadChangedAutoCombo);

        styleCheckBox(skipExternalChangesCheck);
        VBox.setMargin(skipExternalChangesCheck, new Insets(0, 0, 0, 20));

        styleCheckBox(deleteRemoteWhenLocalDeletedCheck);
        VBox.setMargin(deleteRemoteWhenLocalDeletedCheck, new Insets(0, 0, 0, 20));

        // 5. Preserve original file permissions
        HBox permRadioRow = new HBox(16);
        permRadioRow.setAlignment(Pos.CENTER_LEFT);
        Label permRadioLbl = createLabel("Preserve original file permissions");
        permRadioLbl.setPrefWidth(220);

        preservePermNoRadio.setToggleGroup(preservePermissionsGroup);
        preservePermYesRadio.setToggleGroup(preservePermissionsGroup);
        styleRadioButton(preservePermNoRadio);
        styleRadioButton(preservePermYesRadio);
        preservePermissionsGroup.selectedToggleProperty().addListener((obs, o, n) -> checkModified());

        permRadioRow.getChildren().addAll(permRadioLbl, preservePermNoRadio, preservePermYesRadio);

        // 6. Override default permissions on files
        HBox overrideFilesRow = new HBox(10);
        overrideFilesRow.setAlignment(Pos.CENTER_LEFT);
        styleCheckBox(overridePermFilesCheck);
        overridePermFilesCheck.setPrefWidth(260);

        permFilesField.setPrefWidth(120);
        styleTextField(permFilesField);
        permFilesField.disableProperty().bind(overridePermFilesCheck.selectedProperty().not());
        permFilesField.textProperty().addListener((obs, o, n) -> checkModified());

        styleBrowseButton(permFilesBrowseBtn);
        permFilesBrowseBtn.disableProperty().bind(overridePermFilesCheck.selectedProperty().not());

        overrideFilesRow.getChildren().addAll(overridePermFilesCheck, permFilesField, permFilesBrowseBtn);

        // 7. Override default permissions on folders
        HBox overrideFoldersRow = new HBox(10);
        overrideFoldersRow.setAlignment(Pos.CENTER_LEFT);
        styleCheckBox(overridePermFoldersCheck);
        overridePermFoldersCheck.setPrefWidth(260);

        permFoldersField.setPrefWidth(120);
        styleTextField(permFoldersField);
        permFoldersField.disableProperty().bind(overridePermFoldersCheck.selectedProperty().not());
        permFoldersField.textProperty().addListener((obs, o, n) -> checkModified());

        styleBrowseButton(permFoldersBrowseBtn);
        permFoldersBrowseBtn.disableProperty().bind(overridePermFoldersCheck.selectedProperty().not());

        overrideFoldersRow.getChildren().addAll(overridePermFoldersCheck, permFoldersField, permFoldersBrowseBtn);

        // 8. Warn when uploading over newer file
        HBox warnRow = new HBox(12);
        warnRow.setAlignment(Pos.CENTER_LEFT);
        Label warnLbl = createLabel("Warn when uploading over newer file:");
        warnLbl.setPrefWidth(260);

        warnUploadingNewerCombo.getItems().addAll("No", "Compare timestamp", "Compare content");
        styleComboBox(warnUploadingNewerCombo);
        warnUploadingNewerCombo.valueProperty().addListener((obs, o, n) -> {
            boolean active = !"No".equals(n);
            notifyRemoteChangesCheck.setDisable(!active);
            checkModified();
        });
        warnRow.getChildren().addAll(warnLbl, warnUploadingNewerCombo);

        styleCheckBox(notifyRemoteChangesCheck);
        VBox.setMargin(notifyRemoteChangesCheck, new Insets(0, 0, 0, 20));

        // 9. Bottom link to SSH Configuration
        HBox bottomLinkRow = new HBox(4);
        bottomLinkRow.setAlignment(Pos.CENTER_LEFT);
        bottomLinkRow.setPadding(new Insets(14, 0, 0, 0));

        Label prefixLabel = new Label("Set up advanced SFTP options in ");
        prefixLabel.setStyle("-fx-text-fill: #8C9199; -fx-font-size: 12px;");

        Hyperlink sshLink = new Hyperlink("SSH Configuration");
        sshLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0;");
        sshLink.setOnAction(e -> {
            if (navigationHandler != null) {
                navigationHandler.accept("SSH");
            }
        });

        Label suffixLabel = new Label(" settings.");
        suffixLabel.setStyle("-fx-text-fill: #8C9199; -fx-font-size: 12px;");

        bottomLinkRow.getChildren().addAll(prefixLabel, sshLink, suffixLabel);

        content.getChildren().addAll(
                excludeRow,
                excludeHint,
                loggingRow,
                overwriteUpToDateCheck,
                useTempFileCheck,
                preserveTimestampsCheck,
                deleteTargetNotExistCheck,
                confirmDeletingRemoteCheck,
                createEmptyDirsCheck,
                promptOverwritingDeletingCheck,
                confirmUploadingFilesCheck,
                uploadAutoRow,
                skipExternalChangesCheck,
                deleteRemoteWhenLocalDeletedCheck,
                permRadioRow,
                overrideFilesRow,
                overrideFoldersRow,
                warnRow,
                notifyRemoteChangesCheck,
                bottomLinkRow
        );

        scrollPane.setContent(content);
        getChildren().add(scrollPane);
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return lbl;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, o, n) -> checkModified());
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleBrowseButton(Button btn) {
        btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-cursor: hand; -fx-padding: 4 8 4 8; -fx-background-radius: 4;");
    }

    public void loadData() {
        suppressEvents = true;
        initialOptions = manager.getOptions();

        excludeItemsField.setText(initialOptions.getExcludeItemsByName());
        operationsLoggingCombo.setValue(initialOptions.getOperationsLogging());

        overwriteUpToDateCheck.setSelected(initialOptions.isOverwriteUpToDateFiles());
        useTempFileCheck.setSelected(initialOptions.isUseTemporaryFileDuringUpload());
        preserveTimestampsCheck.setSelected(initialOptions.isPreserveFileTimestamps());
        deleteTargetNotExistCheck.setSelected(initialOptions.isDeleteTargetItemsWhenSourceNotExist());
        confirmDeletingRemoteCheck.setSelected(initialOptions.isConfirmDeletingRemoteFiles());
        createEmptyDirsCheck.setSelected(initialOptions.isCreateEmptyDirectories());
        promptOverwritingDeletingCheck.setSelected(initialOptions.isPromptWhenOverwritingOrDeletingLocalItems());
        confirmUploadingFilesCheck.setSelected(initialOptions.isConfirmUploadingFiles());

        uploadChangedAutoCombo.setValue(initialOptions.getUploadChangedFilesAutomatically());
        boolean uploadAutoActive = !"Never".equals(initialOptions.getUploadChangedFilesAutomatically());
        skipExternalChangesCheck.setDisable(!uploadAutoActive);
        skipExternalChangesCheck.setSelected(initialOptions.isSkipExternalChanges());
        deleteRemoteWhenLocalDeletedCheck.setDisable(!uploadAutoActive);
        deleteRemoteWhenLocalDeletedCheck.setSelected(initialOptions.isDeleteRemoteFilesWhenLocalDeleted());

        if (initialOptions.isPreserveOriginalFilePermissions()) {
            preservePermYesRadio.setSelected(true);
        } else {
            preservePermNoRadio.setSelected(true);
        }

        overridePermFilesCheck.setSelected(initialOptions.isOverrideDefaultPermissionsFiles());
        permFilesField.setText(initialOptions.getPermissionsFiles());

        overridePermFoldersCheck.setSelected(initialOptions.isOverrideDefaultPermissionsFolders());
        permFoldersField.setText(initialOptions.getPermissionsFolders());

        warnUploadingNewerCombo.setValue(initialOptions.getWarnWhenUploadingOverNewerFile());
        boolean warnActive = !"No".equals(initialOptions.getWarnWhenUploadingOverNewerFile());
        notifyRemoteChangesCheck.setDisable(!warnActive);
        notifyRemoteChangesCheck.setSelected(initialOptions.isNotifyOfRemoteChanges());

        suppressEvents = false;
        checkModified();
    }

    public DeploymentOptions getCurrentOptions() {
        DeploymentOptions opt = new DeploymentOptions();
        opt.setExcludeItemsByName(excludeItemsField.getText());
        opt.setOperationsLogging(operationsLoggingCombo.getValue());

        opt.setOverwriteUpToDateFiles(overwriteUpToDateCheck.isSelected());
        opt.setUseTemporaryFileDuringUpload(useTempFileCheck.isSelected());
        opt.setPreserveFileTimestamps(preserveTimestampsCheck.isSelected());
        opt.setDeleteTargetItemsWhenSourceNotExist(deleteTargetNotExistCheck.isSelected());
        opt.setConfirmDeletingRemoteFiles(confirmDeletingRemoteCheck.isSelected());
        opt.setCreateEmptyDirectories(createEmptyDirsCheck.isSelected());
        opt.setPromptWhenOverwritingOrDeletingLocalItems(promptOverwritingDeletingCheck.isSelected());
        opt.setConfirmUploadingFiles(confirmUploadingFilesCheck.isSelected());

        opt.setUploadChangedFilesAutomatically(uploadChangedAutoCombo.getValue());
        opt.setSkipExternalChanges(skipExternalChangesCheck.isSelected());
        opt.setDeleteRemoteFilesWhenLocalDeleted(deleteRemoteWhenLocalDeletedCheck.isSelected());

        opt.setPreserveOriginalFilePermissions(preservePermYesRadio.isSelected());
        opt.setOverrideDefaultPermissionsFiles(overridePermFilesCheck.isSelected());
        opt.setPermissionsFiles(permFilesField.getText());

        opt.setOverrideDefaultPermissionsFolders(overridePermFoldersCheck.isSelected());
        opt.setPermissionsFolders(permFoldersField.getText());

        opt.setWarnWhenUploadingOverNewerFile(warnUploadingNewerCombo.getValue());
        opt.setNotifyOfRemoteChanges(notifyRemoteChangesCheck.isSelected());

        return opt;
    }

    public boolean isModified() {
        if (initialOptions == null) return false;
        return !Objects.equals(initialOptions, getCurrentOptions());
    }

    public void apply() {
        if (isModified()) {
            initialOptions = getCurrentOptions();
            manager.setOptions(initialOptions);
            checkModified();
        }
    }

    public void reset() {
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void checkModified() {
        if (suppressEvents) return;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
