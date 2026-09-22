package dev.lumina.ui;

import dev.lumina.git.VcsShelfSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * Version Control > Shelf settings page matching IntelliJ IDEA Image 4.
 */
public class SettingsVcsShelfPage extends VBox {

    private final VcsShelfSettingsManager manager = VcsShelfSettingsManager.getInstance();

    private final CheckBox removeAppliedFilesCheck = new CheckBox("Remove successfully applied files from the shelf");
    private final CheckBox shelveBaseRevisionsCheck = new CheckBox("Shelve base revisions of files under distributed version control systems");
    private final Label currentLocationLabel = new Label();
    private final Button changeLocationBtn = new Button("Change Shelves Location...");

    public SettingsVcsShelfPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 24, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Remove applied files
        removeAppliedFilesCheck.setSelected(manager.isRemoveAppliedFiles());
        removeAppliedFilesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        removeAppliedFilesCheck.setOnAction(e -> manager.setRemoveAppliedFiles(removeAppliedFilesCheck.isSelected()));

        // 2. Shelve base revisions
        shelveBaseRevisionsCheck.setSelected(manager.isShelveBaseRevisions());
        shelveBaseRevisionsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        shelveBaseRevisionsCheck.setOnAction(e -> manager.setShelveBaseRevisions(shelveBaseRevisionsCheck.isSelected()));

        Label shelveDesc = new Label("The base content of files larger than 500K will not be stored");
        shelveDesc.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        shelveDesc.setPadding(new Insets(0, 0, 0, 24));

        VBox shelveBox = new VBox(4, shelveBaseRevisionsCheck, shelveDesc);

        // 3. Shelves Location Row
        changeLocationBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 12 6 12; -fx-font-size: 12px; -fx-cursor: hand;");
        changeLocationBtn.setOnAction(e -> onChangeLocation());

        currentLocationLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        updateLocationLabel();

        HBox locationBox = new HBox(12, changeLocationBtn, currentLocationLabel);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        locationBox.setPadding(new Insets(6, 0, 0, 0));

        getChildren().addAll(removeAppliedFilesCheck, shelveBox, locationBox);

        manager.addListener(() -> {
            removeAppliedFilesCheck.setSelected(manager.isRemoveAppliedFiles());
            shelveBaseRevisionsCheck.setSelected(manager.isShelveBaseRevisions());
            updateLocationLabel();
        });
    }

    private void updateLocationLabel() {
        Path loc = manager.getCurrentShelvesLocation();
        currentLocationLabel.setText("Current location is " + (loc != null ? loc.toString() : ""));
    }

    private void onChangeLocation() {
        Stage owner = (Stage) getScene().getWindow();
        VcsChangeShelvesLocationDialog dlg = new VcsChangeShelvesLocationDialog(owner);
        dlg.showAndWait();
        updateLocationLabel();
    }
}
