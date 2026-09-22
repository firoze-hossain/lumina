package dev.lumina.ui;

import dev.lumina.git.SubversionSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Version Control > Subversion settings page matching IntelliJ IDEA Image 3.
 */
public class SettingsVcsSubversionPage extends VBox {

    private final SubversionSettingsManager manager = SubversionSettingsManager.getInstance();

    private final TextField svnPathField = new TextField();
    private final Button svnBrowseBtn = new Button();

    private final CheckBox enableInteractiveCheck = new CheckBox("Enable interactive mode");
    private final Label interactiveSubtext = new Label(
            "When this option is selected, you can interact with the Subversion command-line client,\n" +
            "for example, to provide SSH passwords or accept SSL certificates. Interactive mode requires terminal emulation."
    );

    private final CheckBox useCustomConfigCheck = new CheckBox("Use custom configuration directory:");
    private final TextField configDirField = new TextField();
    private final Button configDirBrowseBtn = new Button();

    private final Button clearAuthCacheBtn = new Button("Clear Auth Cache");
    private final Label authCacheStatusLabel = new Label();

    public SettingsVcsSubversionPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 24, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Path to Subversion executable
        HBox execRow = buildExecRow();

        // 2. Interactive mode
        VBox interactiveBox = buildInteractiveBox();

        // 3. Custom configuration directory
        VBox customConfigBox = buildCustomConfigBox();

        // 4. Clear Auth Cache Button
        HBox authCacheBox = buildAuthCacheBox();

        getChildren().addAll(execRow, interactiveBox, customConfigBox, authCacheBox);

        manager.addListener(this::syncFromManager);
        syncFromManager();
    }

    private HBox buildExecRow() {
        Label label = new Label("Path to Subversion executable:");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        svnPathField.setText(manager.getSvnExecutablePath());
        styleTextField(svnPathField, 300);
        HBox.setHgrow(svnPathField, Priority.ALWAYS);
        svnPathField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setSvnExecutablePath(newV);
        });

        styleBrowseBtn(svnBrowseBtn, "Select Subversion Executable", false, f -> {
            svnPathField.setText(f.getAbsolutePath());
            manager.setSvnExecutablePath(f.getAbsolutePath());
        });

        HBox row = new HBox(8, label, svnPathField, svnBrowseBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildInteractiveBox() {
        VBox box = new VBox(6);

        enableInteractiveCheck.setSelected(manager.isEnableInteractiveMode());
        enableInteractiveCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        enableInteractiveCheck.setOnAction(e -> manager.setEnableInteractiveMode(enableInteractiveCheck.isSelected()));

        interactiveSubtext.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        interactiveSubtext.setPadding(new Insets(0, 0, 0, 24));

        box.getChildren().addAll(enableInteractiveCheck, interactiveSubtext);
        return box;
    }

    private VBox buildCustomConfigBox() {
        VBox box = new VBox(6);

        useCustomConfigCheck.setSelected(manager.isUseCustomConfigDirectory());
        useCustomConfigCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        useCustomConfigCheck.setOnAction(e -> manager.setUseCustomConfigDirectory(useCustomConfigCheck.isSelected()));

        configDirField.setText(manager.getCustomConfigDirectoryPath());
        styleTextField(configDirField, 300);
        HBox.setHgrow(configDirField, Priority.ALWAYS);
        configDirField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setCustomConfigDirectoryPath(newV);
        });

        styleBrowseBtn(configDirBrowseBtn, "Select Configuration Directory", true, f -> {
            configDirField.setText(f.getAbsolutePath());
            manager.setCustomConfigDirectoryPath(f.getAbsolutePath());
        });

        configDirField.disableProperty().bind(useCustomConfigCheck.selectedProperty().not());
        configDirBrowseBtn.disableProperty().bind(useCustomConfigCheck.selectedProperty().not());

        HBox dirRow = new HBox(8, configDirField, configDirBrowseBtn);
        dirRow.setAlignment(Pos.CENTER_LEFT);
        dirRow.setPadding(new Insets(0, 0, 0, 24));

        box.getChildren().addAll(useCustomConfigCheck, dirRow);
        return box;
    }

    private HBox buildAuthCacheBox() {
        clearAuthCacheBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;");
        clearAuthCacheBtn.setOnAction(e -> {
            boolean success = manager.clearAuthCache();
            if (success) {
                authCacheStatusLabel.setText("Authentication cache cleared successfully.");
                authCacheStatusLabel.setStyle("-fx-text-fill: #73BD79; -fx-font-size: 11px;");
            } else {
                authCacheStatusLabel.setText("Failed to clear authentication cache.");
                authCacheStatusLabel.setStyle("-fx-text-fill: #ED5E62; -fx-font-size: 11px;");
            }
            authCacheStatusLabel.setVisible(true);
            authCacheStatusLabel.setManaged(true);
        });

        authCacheStatusLabel.setVisible(false);
        authCacheStatusLabel.setManaged(false);

        HBox box = new HBox(12, clearAuthCacheBtn, authCacheStatusLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 0, 0, 0));
        return box;
    }

    private void styleTextField(TextField tf, double width) {
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
    }

    private void styleBrowseBtn(Button btn, String title, boolean isDir, java.util.function.Consumer<File> onFileChosen) {
        SVGPath folderIcon = new SVGPath();
        folderIcon.setContent("M 2 3 L 5 3 L 6.5 5 L 12 5 L 12 11 L 2 11 Z");
        folderIcon.setFill(Color.TRANSPARENT);
        folderIcon.setStroke(Color.web("#848BA3"));
        folderIcon.setStrokeWidth(1.2);
        btn.setGraphic(folderIcon);
        btn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            if (isDir) {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setTitle(title);
                File f = dc.showDialog(getScene().getWindow());
                if (f != null) onFileChosen.accept(f);
            } else {
                FileChooser fc = new FileChooser();
                fc.setTitle(title);
                File f = fc.showOpenDialog(getScene().getWindow());
                if (f != null) onFileChosen.accept(f);
            }
        });
    }

    private void syncFromManager() {
        svnPathField.setText(manager.getSvnExecutablePath());
        enableInteractiveCheck.setSelected(manager.isEnableInteractiveMode());
        useCustomConfigCheck.setSelected(manager.isUseCustomConfigDirectory());
        configDirField.setText(manager.getCustomConfigDirectoryPath());
    }
}
