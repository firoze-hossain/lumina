package dev.lumina.ui;

import dev.lumina.git.VcsLogSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Version Control > Log settings page matching IntelliJ IDEA Image 3.
 */
public class SettingsVcsLogPage extends VBox {

    private final VcsLogSettingsManager manager = VcsLogSettingsManager.getInstance();

    // 1. View Options
    private final CheckBox showOnlyFirstRef = new CheckBox("Show only the first reference for a commit in the table");
    private final CheckBox showTagNames = new CheckBox("Show tag names in the table");
    private final CheckBox showCommitTime = new CheckBox("Show the time when the change was committed rather than authored");
    private final CheckBox showLeftReferences = new CheckBox("Show references on the left of commit message");
    private final CheckBox displayMergedCommitsSeparately = new CheckBox("Display changes to each merged commit separately");
    private final CheckBox showDiffPreview = new CheckBox("Show the diff preview panel");
    private final RadioButton diffLocBottom = new RadioButton("Bottom");
    private final RadioButton diffLocRight = new RadioButton("Right");

    // View Options Visible Columns
    private final CheckBox colAuthor = new CheckBox("Author");
    private final CheckBox colHash = new CheckBox("Hash");
    private final CheckBox colDate = new CheckBox("Date");
    private final CheckBox colGpg = new CheckBox("GPG Signature");
    private final CheckBox colChecks = new CheckBox("GitHub Commit Checks");

    // 2. Indexing
    private final CheckBox enableIndexing = new CheckBox("Enable indexing for project");

    // 3. File History
    private final CheckBox fhDisplayDetails = new CheckBox("Display details panel");
    private final CheckBox fhShowFileNames = new CheckBox("Show file names");
    private final CheckBox fhDiffPreview = new CheckBox("Show the diff preview panel");
    private final RadioButton fhDiffLocBottom = new RadioButton("Bottom");
    private final RadioButton fhDiffLocRight = new RadioButton("Right");

    // File History Visible Columns
    private final CheckBox fhColAuthor = new CheckBox("Author");
    private final CheckBox fhColHash = new CheckBox("Hash");
    private final CheckBox fhColDate = new CheckBox("Date");
    private final CheckBox fhColGpg = new CheckBox("GPG Signature");
    private final CheckBox fhColChecks = new CheckBox("GitHub Commit Checks");

    public SettingsVcsLogPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 24, 20));
        setSpacing(18);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // ---------------- Section 1: View Options ----------------
        VBox viewOptionsBox = new VBox(8);
        Label viewOptionsTitle = createSectionHeader("View Options");

        initCheckBox(showOnlyFirstRef, manager.isShowOnlyFirstRef(), e -> manager.setShowOnlyFirstRef(showOnlyFirstRef.isSelected()));
        initCheckBox(showTagNames, manager.isShowTagNames(), e -> manager.setShowTagNames(showTagNames.isSelected()));
        initCheckBox(showCommitTime, manager.isShowCommitTime(), e -> manager.setShowCommitTime(showCommitTime.isSelected()));
        initCheckBox(showLeftReferences, manager.isShowLeftReferences(), e -> manager.setShowLeftReferences(showLeftReferences.isSelected()));
        initCheckBox(displayMergedCommitsSeparately, manager.isDisplayMergedCommitsSeparately(), e -> manager.setDisplayMergedCommitsSeparately(displayMergedCommitsSeparately.isSelected()));
        initCheckBox(showDiffPreview, manager.isShowDiffPreview(), e -> {
            manager.setShowDiffPreview(showDiffPreview.isSelected());
            updateDiffPreviewControls();
        });

        // Sub options: Location
        ToggleGroup diffGroup = new ToggleGroup();
        diffLocBottom.setToggleGroup(diffGroup);
        diffLocRight.setToggleGroup(diffGroup);
        initRadio(diffLocBottom, manager.getDiffPreviewLocation() == VcsLogSettingsManager.DiffPreviewLocation.BOTTOM);
        initRadio(diffLocRight, manager.getDiffPreviewLocation() == VcsLogSettingsManager.DiffPreviewLocation.RIGHT);
        diffGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == diffLocRight) {
                manager.setDiffPreviewLocation(VcsLogSettingsManager.DiffPreviewLocation.RIGHT);
            } else {
                manager.setDiffPreviewLocation(VcsLogSettingsManager.DiffPreviewLocation.BOTTOM);
            }
        });

        Label locLabel = new Label("Location:");
        locLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        HBox diffLocBox = new HBox(12, locLabel, diffLocBottom, diffLocRight);
        diffLocBox.setAlignment(Pos.CENTER_LEFT);
        diffLocBox.setPadding(new Insets(0, 0, 4, 24));
        updateDiffPreviewControls();

        // Visible Columns
        Label visibleColTitle = createSubHeader("Visible Columns");
        visibleColTitle.setPadding(new Insets(6, 0, 2, 0));

        initCheckBox(colAuthor, manager.isAuthorVisible(), e -> manager.setAuthorVisible(colAuthor.isSelected()));
        initCheckBox(colHash, manager.isHashVisible(), e -> manager.setHashVisible(colHash.isSelected()));
        initCheckBox(colDate, manager.isDateVisible(), e -> manager.setDateVisible(colDate.isSelected()));
        initCheckBox(colGpg, manager.isGpgSignatureVisible(), e -> manager.setGpgSignatureVisible(colGpg.isSelected()));
        initCheckBox(colChecks, manager.isGithubCommitChecksVisible(), e -> manager.setGithubCommitChecksVisible(colChecks.isSelected()));

        viewOptionsBox.getChildren().addAll(
                viewOptionsTitle,
                showOnlyFirstRef, showTagNames, showCommitTime, showLeftReferences,
                displayMergedCommitsSeparately, showDiffPreview, diffLocBox,
                visibleColTitle, colAuthor, colHash, colDate, colGpg, colChecks
        );

        // ---------------- Section 2: Indexing ----------------
        VBox indexingBox = new VBox(4);
        Label indexingTitle = createSectionHeader("Indexing");
        initCheckBox(enableIndexing, manager.isEnableIndexing(), e -> manager.setEnableIndexing(enableIndexing.isSelected()));

        Label indexingDesc = new Label("To improve working with history of changes across IDE");
        indexingDesc.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        indexingDesc.setPadding(new Insets(0, 0, 0, 24));

        indexingBox.getChildren().addAll(indexingTitle, enableIndexing, indexingDesc);

        // ---------------- Section 3: File History ----------------
        VBox fileHistoryBox = new VBox(8);
        Label fileHistoryTitle = createSectionHeader("File History");

        initCheckBox(fhDisplayDetails, manager.isFileHistoryDetailsPanel(), e -> manager.setFileHistoryDetailsPanel(fhDisplayDetails.isSelected()));
        initCheckBox(fhShowFileNames, manager.isFileHistoryShowFileNames(), e -> manager.setFileHistoryShowFileNames(fhShowFileNames.isSelected()));
        initCheckBox(fhDiffPreview, manager.isFileHistoryDiffPreview(), e -> {
            manager.setFileHistoryDiffPreview(fhDiffPreview.isSelected());
            updateFhDiffPreviewControls();
        });

        // Sub options: Location
        ToggleGroup fhDiffGroup = new ToggleGroup();
        fhDiffLocBottom.setToggleGroup(fhDiffGroup);
        fhDiffLocRight.setToggleGroup(fhDiffGroup);
        initRadio(fhDiffLocBottom, manager.getFileHistoryDiffPreviewLocation() == VcsLogSettingsManager.DiffPreviewLocation.BOTTOM);
        initRadio(fhDiffLocRight, manager.getFileHistoryDiffPreviewLocation() == VcsLogSettingsManager.DiffPreviewLocation.RIGHT);
        fhDiffGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == fhDiffLocRight) {
                manager.setFileHistoryDiffPreviewLocation(VcsLogSettingsManager.DiffPreviewLocation.RIGHT);
            } else {
                manager.setFileHistoryDiffPreviewLocation(VcsLogSettingsManager.DiffPreviewLocation.BOTTOM);
            }
        });

        Label fhLocLabel = new Label("Location:");
        fhLocLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        HBox fhDiffLocBox = new HBox(12, fhLocLabel, fhDiffLocBottom, fhDiffLocRight);
        fhDiffLocBox.setAlignment(Pos.CENTER_LEFT);
        fhDiffLocBox.setPadding(new Insets(0, 0, 4, 24));
        updateFhDiffPreviewControls();

        // Visible Columns for File History
        Label fhVisibleColTitle = createSubHeader("Visible Columns");
        fhVisibleColTitle.setPadding(new Insets(6, 0, 2, 0));

        initCheckBox(fhColAuthor, manager.isFileHistoryAuthorVisible(), e -> manager.setFileHistoryAuthorVisible(fhColAuthor.isSelected()));
        initCheckBox(fhColHash, manager.isFileHistoryHashVisible(), e -> manager.setFileHistoryHashVisible(fhColHash.isSelected()));
        initCheckBox(fhColDate, manager.isFileHistoryDateVisible(), e -> manager.setFileHistoryDateVisible(fhColDate.isSelected()));
        initCheckBox(fhColGpg, manager.isFileHistoryGpgSignatureVisible(), e -> manager.setFileHistoryGpgSignatureVisible(fhColGpg.isSelected()));
        initCheckBox(fhColChecks, manager.isFileHistoryGithubCommitChecksVisible(), e -> manager.setFileHistoryGithubCommitChecksVisible(fhColChecks.isSelected()));

        fileHistoryBox.getChildren().addAll(
                fileHistoryTitle,
                fhDisplayDetails, fhShowFileNames, fhDiffPreview, fhDiffLocBox,
                fhVisibleColTitle, fhColAuthor, fhColHash, fhColDate, fhColGpg, fhColChecks
        );

        getChildren().addAll(viewOptionsBox, indexingBox, fileHistoryBox);

        manager.addListener(this::syncFromManager);
    }

    private Label createSectionHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return lbl;
    }

    private Label createSubHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
        return lbl;
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

    private void updateDiffPreviewControls() {
        boolean enabled = showDiffPreview.isSelected();
        diffLocBottom.setDisable(!enabled);
        diffLocRight.setDisable(!enabled);
    }

    private void updateFhDiffPreviewControls() {
        boolean enabled = fhDiffPreview.isSelected();
        fhDiffLocBottom.setDisable(!enabled);
        fhDiffLocRight.setDisable(!enabled);
    }

    private void syncFromManager() {
        showOnlyFirstRef.setSelected(manager.isShowOnlyFirstRef());
        showTagNames.setSelected(manager.isShowTagNames());
        showCommitTime.setSelected(manager.isShowCommitTime());
        showLeftReferences.setSelected(manager.isShowLeftReferences());
        displayMergedCommitsSeparately.setSelected(manager.isDisplayMergedCommitsSeparately());
        showDiffPreview.setSelected(manager.isShowDiffPreview());
        if (manager.getDiffPreviewLocation() == VcsLogSettingsManager.DiffPreviewLocation.RIGHT) {
            diffLocRight.setSelected(true);
        } else {
            diffLocBottom.setSelected(true);
        }
        updateDiffPreviewControls();

        colAuthor.setSelected(manager.isAuthorVisible());
        colHash.setSelected(manager.isHashVisible());
        colDate.setSelected(manager.isDateVisible());
        colGpg.setSelected(manager.isGpgSignatureVisible());
        colChecks.setSelected(manager.isGithubCommitChecksVisible());

        enableIndexing.setSelected(manager.isEnableIndexing());

        fhDisplayDetails.setSelected(manager.isFileHistoryDetailsPanel());
        fhShowFileNames.setSelected(manager.isFileHistoryShowFileNames());
        fhDiffPreview.setSelected(manager.isFileHistoryDiffPreview());
        if (manager.getFileHistoryDiffPreviewLocation() == VcsLogSettingsManager.DiffPreviewLocation.RIGHT) {
            fhDiffLocRight.setSelected(true);
        } else {
            fhDiffLocBottom.setSelected(true);
        }
        updateFhDiffPreviewControls();

        fhColAuthor.setSelected(manager.isFileHistoryAuthorVisible());
        fhColHash.setSelected(manager.isFileHistoryHashVisible());
        fhColDate.setSelected(manager.isFileHistoryDateVisible());
        fhColGpg.setSelected(manager.isFileHistoryGpgSignatureVisible());
        fhColChecks.setSelected(manager.isFileHistoryGithubCommitChecksVisible());
    }
}
