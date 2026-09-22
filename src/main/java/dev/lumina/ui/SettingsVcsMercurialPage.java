package dev.lumina.ui;

import dev.lumina.git.MercurialSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Version Control > Mercurial settings page matching IntelliJ IDEA Image 5.
 */
public class SettingsVcsMercurialPage extends VBox {

    private final MercurialSettingsManager manager = MercurialSettingsManager.getInstance();

    private final TextField hgPathField = new TextField();
    private final Button browseBtn = new Button();
    private final Button testBtn = new Button("Test");
    private final Label testResultLabel = new Label();
    private final CheckBox setPathOnlyForProjectCheck = new CheckBox("Set this path only for the current project");

    private final CheckBox checkChangesetsCheck = new CheckBox("Check for incoming and outgoing changesets");
    private final CheckBox ignoreWhitespaceCheck = new CheckBox("Ignore whitespace differences in annotations");

    public SettingsVcsMercurialPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 24, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Executable Path Row
        Label pathLabel = new Label("Path to Mercurial executable:");
        pathLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        String currentPath = manager.getHgExecutablePath();
        String autoDetected = manager.getAutoDetectedHgPath();
        hgPathField.setText(currentPath.isEmpty() ? "Auto-detected: " + autoDetected : currentPath);
        hgPathField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(hgPathField, Priority.ALWAYS);

        hgPathField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.startsWith("Auto-detected:")) {
                manager.setHgExecutablePath(newV.trim());
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
            fc.setTitle("Select Mercurial Executable");
            File f = fc.showOpenDialog(getScene().getWindow());
            if (f != null) {
                hgPathField.setText(f.getAbsolutePath());
                manager.setHgExecutablePath(f.getAbsolutePath());
            }
        });

        // Test button
        testBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;");
        testBtn.setOnAction(e -> onTestHg());

        HBox pathRow = new HBox(8, pathLabel, hgPathField, browseBtn, testBtn);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        testResultLabel.setStyle("-fx-font-size: 11px; -fx-padding: 0 0 0 185;");
        testResultLabel.setVisible(false);
        testResultLabel.setManaged(false);

        setPathOnlyForProjectCheck.setSelected(manager.isSetPathOnlyForProject());
        setPathOnlyForProjectCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        setPathOnlyForProjectCheck.setPadding(new Insets(0, 0, 0, 185));
        setPathOnlyForProjectCheck.setOnAction(e -> manager.setSetPathOnlyForProject(setPathOnlyForProjectCheck.isSelected()));

        VBox execSection = new VBox(6, pathRow, testResultLabel, setPathOnlyForProjectCheck);

        // 2. Options Checkboxes
        checkChangesetsCheck.setSelected(manager.isCheckIncomingOutgoingChangesets());
        checkChangesetsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        checkChangesetsCheck.setOnAction(e -> manager.setCheckIncomingOutgoingChangesets(checkChangesetsCheck.isSelected()));

        ignoreWhitespaceCheck.setSelected(manager.isIgnoreWhitespaceInAnnotations());
        ignoreWhitespaceCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        ignoreWhitespaceCheck.setOnAction(e -> manager.setIgnoreWhitespaceInAnnotations(ignoreWhitespaceCheck.isSelected()));

        getChildren().addAll(execSection, checkChangesetsCheck, ignoreWhitespaceCheck);

        manager.addListener(this::syncFromManager);
    }

    private void onTestHg() {
        String input = hgPathField.getText().trim();
        String toTest = input.startsWith("Auto-detected:") ? "" : input;
        MercurialSettingsManager.TestResult res = manager.testHgExecutable(toTest);

        testResultLabel.setText(res.message());
        testResultLabel.setStyle("-fx-font-size: 11px; -fx-padding: 0 0 0 185; -fx-text-fill: " + (res.success() ? "#73BD79" : "#ED5E62") + ";");
        testResultLabel.setVisible(true);
        testResultLabel.setManaged(true);
    }

    private void syncFromManager() {
        setPathOnlyForProjectCheck.setSelected(manager.isSetPathOnlyForProject());
        checkChangesetsCheck.setSelected(manager.isCheckIncomingOutgoingChangesets());
        ignoreWhitespaceCheck.setSelected(manager.isIgnoreWhitespaceInAnnotations());
    }
}
