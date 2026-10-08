package dev.lumina.deployment;

import java.util.Objects;

/**
 * Dynamic options model for Build, Execution, Deployment > Deployment > Options (Screenshot 2).
 */
public class DeploymentOptions implements Cloneable {

    public static final String DEFAULT_EXCLUDE_ITEMS = ".svn;.cvs;.idea;.DS_Store;.git;hg;*.hprof;*.pyc";

    private String excludeItemsByName = DEFAULT_EXCLUDE_ITEMS;
    private String operationsLogging = "Details";

    private boolean overwriteUpToDateFiles = true;
    private boolean useTemporaryFileDuringUpload = false;
    private boolean preserveFileTimestamps = true;
    private boolean deleteTargetItemsWhenSourceNotExist = false;
    private boolean confirmDeletingRemoteFiles = true;
    private boolean createEmptyDirectories = false;
    private boolean promptWhenOverwritingOrDeletingLocalItems = true;
    private boolean confirmUploadingFiles = true;

    private String uploadChangedFilesAutomatically = "Never";
    private boolean skipExternalChanges = false;
    private boolean deleteRemoteFilesWhenLocalDeleted = false;

    private boolean preserveOriginalFilePermissions = false;
    private boolean overrideDefaultPermissionsFiles = false;
    private String permissionsFiles = "(none)";
    private boolean overrideDefaultPermissionsFolders = false;
    private String permissionsFolders = "(none)";

    private String warnWhenUploadingOverNewerFile = "No";
    private boolean notifyOfRemoteChanges = false;

    public DeploymentOptions() {
    }

    public DeploymentOptions(DeploymentOptions other) {
        if (other != null) {
            this.excludeItemsByName = other.excludeItemsByName;
            this.operationsLogging = other.operationsLogging;
            this.overwriteUpToDateFiles = other.overwriteUpToDateFiles;
            this.useTemporaryFileDuringUpload = other.useTemporaryFileDuringUpload;
            this.preserveFileTimestamps = other.preserveFileTimestamps;
            this.deleteTargetItemsWhenSourceNotExist = other.deleteTargetItemsWhenSourceNotExist;
            this.confirmDeletingRemoteFiles = other.confirmDeletingRemoteFiles;
            this.createEmptyDirectories = other.createEmptyDirectories;
            this.promptWhenOverwritingOrDeletingLocalItems = other.promptWhenOverwritingOrDeletingLocalItems;
            this.confirmUploadingFiles = other.confirmUploadingFiles;
            this.uploadChangedFilesAutomatically = other.uploadChangedFilesAutomatically;
            this.skipExternalChanges = other.skipExternalChanges;
            this.deleteRemoteFilesWhenLocalDeleted = other.deleteRemoteFilesWhenLocalDeleted;
            this.preserveOriginalFilePermissions = other.preserveOriginalFilePermissions;
            this.overrideDefaultPermissionsFiles = other.overrideDefaultPermissionsFiles;
            this.permissionsFiles = other.permissionsFiles;
            this.overrideDefaultPermissionsFolders = other.overrideDefaultPermissionsFolders;
            this.permissionsFolders = other.permissionsFolders;
            this.warnWhenUploadingOverNewerFile = other.warnWhenUploadingOverNewerFile;
            this.notifyOfRemoteChanges = other.notifyOfRemoteChanges;
        }
    }

    public String getExcludeItemsByName() {
        return excludeItemsByName;
    }

    public void setExcludeItemsByName(String excludeItemsByName) {
        this.excludeItemsByName = excludeItemsByName != null ? excludeItemsByName : DEFAULT_EXCLUDE_ITEMS;
    }

    public String getOperationsLogging() {
        return operationsLogging;
    }

    public void setOperationsLogging(String operationsLogging) {
        this.operationsLogging = operationsLogging != null ? operationsLogging : "Details";
    }

    public boolean isOverwriteUpToDateFiles() {
        return overwriteUpToDateFiles;
    }

    public void setOverwriteUpToDateFiles(boolean overwriteUpToDateFiles) {
        this.overwriteUpToDateFiles = overwriteUpToDateFiles;
    }

    public boolean isUseTemporaryFileDuringUpload() {
        return useTemporaryFileDuringUpload;
    }

    public void setUseTemporaryFileDuringUpload(boolean useTemporaryFileDuringUpload) {
        this.useTemporaryFileDuringUpload = useTemporaryFileDuringUpload;
    }

    public boolean isPreserveFileTimestamps() {
        return preserveFileTimestamps;
    }

    public void setPreserveFileTimestamps(boolean preserveFileTimestamps) {
        this.preserveFileTimestamps = preserveFileTimestamps;
    }

    public boolean isDeleteTargetItemsWhenSourceNotExist() {
        return deleteTargetItemsWhenSourceNotExist;
    }

    public void setDeleteTargetItemsWhenSourceNotExist(boolean deleteTargetItemsWhenSourceNotExist) {
        this.deleteTargetItemsWhenSourceNotExist = deleteTargetItemsWhenSourceNotExist;
    }

    public boolean isConfirmDeletingRemoteFiles() {
        return confirmDeletingRemoteFiles;
    }

    public void setConfirmDeletingRemoteFiles(boolean confirmDeletingRemoteFiles) {
        this.confirmDeletingRemoteFiles = confirmDeletingRemoteFiles;
    }

    public boolean isCreateEmptyDirectories() {
        return createEmptyDirectories;
    }

    public void setCreateEmptyDirectories(boolean createEmptyDirectories) {
        this.createEmptyDirectories = createEmptyDirectories;
    }

    public boolean isPromptWhenOverwritingOrDeletingLocalItems() {
        return promptWhenOverwritingOrDeletingLocalItems;
    }

    public void setPromptWhenOverwritingOrDeletingLocalItems(boolean promptWhenOverwritingOrDeletingLocalItems) {
        this.promptWhenOverwritingOrDeletingLocalItems = promptWhenOverwritingOrDeletingLocalItems;
    }

    public boolean isConfirmUploadingFiles() {
        return confirmUploadingFiles;
    }

    public void setConfirmUploadingFiles(boolean confirmUploadingFiles) {
        this.confirmUploadingFiles = confirmUploadingFiles;
    }

    public String getUploadChangedFilesAutomatically() {
        return uploadChangedFilesAutomatically;
    }

    public void setUploadChangedFilesAutomatically(String uploadChangedFilesAutomatically) {
        this.uploadChangedFilesAutomatically = uploadChangedFilesAutomatically != null ? uploadChangedFilesAutomatically : "Never";
    }

    public boolean isSkipExternalChanges() {
        return skipExternalChanges;
    }

    public void setSkipExternalChanges(boolean skipExternalChanges) {
        this.skipExternalChanges = skipExternalChanges;
    }

    public boolean isDeleteRemoteFilesWhenLocalDeleted() {
        return deleteRemoteFilesWhenLocalDeleted;
    }

    public void setDeleteRemoteFilesWhenLocalDeleted(boolean deleteRemoteFilesWhenLocalDeleted) {
        this.deleteRemoteFilesWhenLocalDeleted = deleteRemoteFilesWhenLocalDeleted;
    }

    public boolean isPreserveOriginalFilePermissions() {
        return preserveOriginalFilePermissions;
    }

    public void setPreserveOriginalFilePermissions(boolean preserveOriginalFilePermissions) {
        this.preserveOriginalFilePermissions = preserveOriginalFilePermissions;
    }

    public boolean isOverrideDefaultPermissionsFiles() {
        return overrideDefaultPermissionsFiles;
    }

    public void setOverrideDefaultPermissionsFiles(boolean overrideDefaultPermissionsFiles) {
        this.overrideDefaultPermissionsFiles = overrideDefaultPermissionsFiles;
    }

    public String getPermissionsFiles() {
        return permissionsFiles;
    }

    public void setPermissionsFiles(String permissionsFiles) {
        this.permissionsFiles = permissionsFiles != null ? permissionsFiles : "(none)";
    }

    public boolean isOverrideDefaultPermissionsFolders() {
        return overrideDefaultPermissionsFolders;
    }

    public void setOverrideDefaultPermissionsFolders(boolean overrideDefaultPermissionsFolders) {
        this.overrideDefaultPermissionsFolders = overrideDefaultPermissionsFolders;
    }

    public String getPermissionsFolders() {
        return permissionsFolders;
    }

    public void setPermissionsFolders(String permissionsFolders) {
        this.permissionsFolders = permissionsFolders != null ? permissionsFolders : "(none)";
    }

    public String getWarnWhenUploadingOverNewerFile() {
        return warnWhenUploadingOverNewerFile;
    }

    public void setWarnWhenUploadingOverNewerFile(String warnWhenUploadingOverNewerFile) {
        this.warnWhenUploadingOverNewerFile = warnWhenUploadingOverNewerFile != null ? warnWhenUploadingOverNewerFile : "No";
    }

    public boolean isNotifyOfRemoteChanges() {
        return notifyOfRemoteChanges;
    }

    public void setNotifyOfRemoteChanges(boolean notifyOfRemoteChanges) {
        this.notifyOfRemoteChanges = notifyOfRemoteChanges;
    }

    /**
     * Checks if a given file name matches the exclude pattern list.
     */
    public boolean isFileExcluded(String fileName) {
        if (fileName == null || excludeItemsByName == null || excludeItemsByName.isBlank()) {
            return false;
        }
        String[] tokens = excludeItemsByName.split(";");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            String regex = trimmed.replace(".", "\\.").replace("*", ".*").replace("?", ".");
            if (fileName.matches(regex) || fileName.equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeploymentOptions that)) return false;
        return overwriteUpToDateFiles == that.overwriteUpToDateFiles &&
                useTemporaryFileDuringUpload == that.useTemporaryFileDuringUpload &&
                preserveFileTimestamps == that.preserveFileTimestamps &&
                deleteTargetItemsWhenSourceNotExist == that.deleteTargetItemsWhenSourceNotExist &&
                confirmDeletingRemoteFiles == that.confirmDeletingRemoteFiles &&
                createEmptyDirectories == that.createEmptyDirectories &&
                promptWhenOverwritingOrDeletingLocalItems == that.promptWhenOverwritingOrDeletingLocalItems &&
                confirmUploadingFiles == that.confirmUploadingFiles &&
                skipExternalChanges == that.skipExternalChanges &&
                deleteRemoteFilesWhenLocalDeleted == that.deleteRemoteFilesWhenLocalDeleted &&
                preserveOriginalFilePermissions == that.preserveOriginalFilePermissions &&
                overrideDefaultPermissionsFiles == that.overrideDefaultPermissionsFiles &&
                overrideDefaultPermissionsFolders == that.overrideDefaultPermissionsFolders &&
                notifyOfRemoteChanges == that.notifyOfRemoteChanges &&
                Objects.equals(excludeItemsByName, that.excludeItemsByName) &&
                Objects.equals(operationsLogging, that.operationsLogging) &&
                Objects.equals(uploadChangedFilesAutomatically, that.uploadChangedFilesAutomatically) &&
                Objects.equals(permissionsFiles, that.permissionsFiles) &&
                Objects.equals(permissionsFolders, that.permissionsFolders) &&
                Objects.equals(warnWhenUploadingOverNewerFile, that.warnWhenUploadingOverNewerFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(excludeItemsByName, operationsLogging, overwriteUpToDateFiles,
                useTemporaryFileDuringUpload, preserveFileTimestamps, deleteTargetItemsWhenSourceNotExist,
                confirmDeletingRemoteFiles, createEmptyDirectories, promptWhenOverwritingOrDeletingLocalItems,
                confirmUploadingFiles, uploadChangedFilesAutomatically, skipExternalChanges,
                deleteRemoteFilesWhenLocalDeleted, preserveOriginalFilePermissions,
                overrideDefaultPermissionsFiles, permissionsFiles, overrideDefaultPermissionsFolders,
                permissionsFolders, warnWhenUploadingOverNewerFile, notifyOfRemoteChanges);
    }

    @Override
    public DeploymentOptions clone() {
        return new DeploymentOptions(this);
    }
}
