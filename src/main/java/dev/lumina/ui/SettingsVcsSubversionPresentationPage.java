package dev.lumina.ui;

import dev.lumina.git.SubversionSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Version Control > Subversion > Presentation settings page matching IntelliJ IDEA Image 5.
 */
public class SettingsVcsSubversionPresentationPage extends VBox {

    private final SubversionSettingsManager manager = SubversionSettingsManager.getInstance();

    private final CheckBox checkMergeInfoCheck = new CheckBox("Check svn:mergeinfo in target subtree when preparing for merge");
    private final Spinner<Integer> maxRevisionsSpinner = new Spinner<>(1, 100000, 500);
    private final CheckBox showMergeSourceCheck = new CheckBox("Show merge source in history and annotations");
    private final CheckBox ignoreWhitespaceCheck = new CheckBox("Ignore whitespace differences in annotations");

    public SettingsVcsSubversionPresentationPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 24, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Check mergeinfo
        initCheckBox(checkMergeInfoCheck, manager.isCheckMergeInfo(), e -> manager.setCheckMergeInfo(checkMergeInfoCheck.isSelected()));

        // 2. Max revisions spinner
        HBox revisionsRow = buildRevisionsRow();

        // 3. Show merge source
        initCheckBox(showMergeSourceCheck, manager.isShowMergeSource(), e -> manager.setShowMergeSource(showMergeSourceCheck.isSelected()));

        // 4. Ignore whitespace
        initCheckBox(ignoreWhitespaceCheck, manager.isIgnoreWhitespaceInAnnotations(), e -> manager.setIgnoreWhitespaceInAnnotations(ignoreWhitespaceCheck.isSelected()));

        getChildren().addAll(checkMergeInfoCheck, revisionsRow, showMergeSourceCheck, ignoreWhitespaceCheck);

        manager.addListener(this::syncFromManager);
        syncFromManager();
    }

    private HBox buildRevisionsRow() {
        Label label = new Label("Maximum number of revisions to look back in annotations:");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        maxRevisionsSpinner.getValueFactory().setValue(manager.getMaxRevisionsLookBack());
        maxRevisionsSpinner.setPrefWidth(80);
        maxRevisionsSpinner.setEditable(true);
        maxRevisionsSpinner.setStyle("-fx-background-color: #1E1F22; -fx-font-size: 12px;");
        maxRevisionsSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setMaxRevisionsLookBack(newV);
        });

        HBox row = new HBox(8, label, maxRevisionsSpinner);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void initCheckBox(CheckBox cb, boolean initial, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        cb.setSelected(initial);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        cb.setOnAction(onAction);
    }

    private void syncFromManager() {
        checkMergeInfoCheck.setSelected(manager.isCheckMergeInfo());
        maxRevisionsSpinner.getValueFactory().setValue(manager.getMaxRevisionsLookBack());
        showMergeSourceCheck.setSelected(manager.isShowMergeSource());
        ignoreWhitespaceCheck.setSelected(manager.isIgnoreWhitespaceInAnnotations());
    }
}
