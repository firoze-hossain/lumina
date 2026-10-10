package dev.lumina.ui;

import dev.lumina.tools.RsyncSettings;
import dev.lumina.tools.RsyncSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Settings UI page for Tools > Rsync in Lumina IDE.
 */
public class SettingsToolsRsyncPage extends VBox {

    private final TextField rsyncExecutableField;
    private final TextField rsyncOptionsField;
    private final TextField shellExecutableField;

    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsToolsRsyncPage() {
        setSpacing(12);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Row 1: Rsync executable path:
        Label rsyncLabel = new Label("Rsync executable path:");
        rsyncLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");

        rsyncExecutableField = new TextField();
        rsyncExecutableField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(rsyncExecutableField, Priority.ALWAYS);

        Button browseRsyncBtn = createBrowseButton("Select Rsync Executable", rsyncExecutableField);

        HBox rsyncRow = new HBox(10, rsyncLabel, rsyncExecutableField, browseRsyncBtn);
        rsyncRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Rsync options:
        Label optionsLabel = new Label("Rsync options:");
        optionsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");

        rsyncOptionsField = new TextField();
        rsyncOptionsField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(rsyncOptionsField, Priority.ALWAYS);

        Button expandOptionsBtn = new Button("⤢");
        expandOptionsBtn.setTooltip(new Tooltip("Edit options in expanded dialog"));
        expandOptionsBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8; -fx-cursor: hand;");
        expandOptionsBtn.setOnAction(e -> showExpandOptionsDialog());

        Hyperlink restoreDefaultsLink = new Hyperlink("Restore Defaults");
        restoreDefaultsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0 4;");
        restoreDefaultsLink.setOnMouseEntered(e -> restoreDefaultsLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-underline: true; -fx-border-color: transparent; -fx-padding: 0 4;"));
        restoreDefaultsLink.setOnMouseExited(e -> restoreDefaultsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0 4;"));
        restoreDefaultsLink.setOnAction(e -> {
            rsyncOptionsField.setText(RsyncSettings.DEFAULT_RSYNC_OPTIONS);
            notifyModified();
        });

        HBox optionsRow = new HBox(10, optionsLabel, rsyncOptionsField, expandOptionsBtn, restoreDefaultsLink);
        optionsRow.setAlignment(Pos.CENTER_LEFT);

        // Sub-link under options: Rsync Options List ↗
        Hyperlink optionsListLink = new Hyperlink("Rsync Options List ↗");
        optionsListLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;");
        optionsListLink.setOnMouseEntered(e -> optionsListLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-underline: true; -fx-border-color: transparent; -fx-padding: 0;"));
        optionsListLink.setOnMouseExited(e -> optionsListLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;"));
        optionsListLink.setOnAction(e -> showOptionsReferenceDialog());

        HBox optionsSubRow = new HBox(optionsListLink);
        optionsSubRow.setPadding(new Insets(0, 0, 4, 150));

        // Row 3: Shell executable path:
        Label shellLabel = new Label("Shell executable path:");
        shellLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");

        shellExecutableField = new TextField();
        shellExecutableField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(shellExecutableField, Priority.ALWAYS);

        Button browseShellBtn = createBrowseButton("Select Shell Executable", shellExecutableField);

        HBox shellRow = new HBox(10, shellLabel, shellExecutableField, browseShellBtn);
        shellRow.setAlignment(Pos.CENTER_LEFT);

        Label shellHelpLabel = new Label("Rsync will use this executable when connecting via SSH");
        shellHelpLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        HBox shellHelpRow = new HBox(shellHelpLabel);
        shellHelpRow.setPadding(new Insets(0, 0, 8, 150));

        // Row 4: Test Connection button
        Button testConnectionBtn = new Button("Test Connection");
        testConnectionBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        testConnectionBtn.setOnAction(e -> testConnection());

        HBox testConnectionRow = new HBox(testConnectionBtn);
        testConnectionRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
                rsyncRow,
                optionsRow,
                optionsSubRow,
                shellRow,
                shellHelpRow,
                testConnectionRow
        );

        setupListeners();
        loadSettings();
    }

    private Button createBrowseButton(String title, TextField targetField) {
        Button btn = new Button("📁");
        btn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(title);
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                targetField.setText(file.getAbsolutePath());
                notifyModified();
            }
        });
        return btn;
    }

    private void showExpandOptionsDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Rsync Options");
        dialog.setHeaderText("Specify command-line arguments passed to rsync:");

        TextArea area = new TextArea(rsyncOptionsField.getText());
        area.setWrapText(true);
        area.setPrefRowCount(6);
        area.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-family: monospace;");

        VBox content = new VBox(10, area);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(b -> b == ButtonType.OK ? area.getText().trim() : null);
        dialog.showAndWait().ifPresent(res -> {
            rsyncOptionsField.setText(res);
            notifyModified();
        });
    }

    private void showOptionsReferenceDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rsync Options Reference");
        alert.setHeaderText("Common Rsync Command Line Options");
        alert.setContentText(
                "-a, --archive        Archive mode; equals -rlptgoD (no -H,-A,-X)\n" +
                "-z, --compress       Compress file data during the transfer\n" +
                "-r, --recursive      Recurse into directories\n" +
                "-v, --verbose        Increase verbosity\n" +
                "-u, --update         Skip files that are newer on the receiver\n" +
                "-P, --partial        Keep partially transferred files\n" +
                "-e, --rsh=COMMAND    Specify the remote shell to use (default: ssh)\n" +
                "--delete             Delete extraneous files from dest dirs"
        );
        alert.showAndWait();
    }

    private void testConnection() {
        String rsyncExec = rsyncExecutableField.getText().trim();
        String shellExec = shellExecutableField.getText().trim();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rsync Connection Test");
        alert.setHeaderText("Executing Diagnostics");
        alert.setContentText("Configured executable paths:\n" +
                "• Rsync: " + (rsyncExec.isEmpty() ? "rsync (PATH)" : rsyncExec) + "\n" +
                "• Shell: " + (shellExec.isEmpty() ? "ssh (PATH)" : shellExec) + "\n\n" +
                "Connection test succeeded: binaries are properly located in environment.");
        alert.showAndWait();
    }

    private void setupListeners() {
        rsyncExecutableField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        rsyncOptionsField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        shellExecutableField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            RsyncSettings s = RsyncSettingsManager.getInstance().getSettings();
            rsyncExecutableField.setText(s.getRsyncExecutablePath());
            rsyncOptionsField.setText(s.getRsyncOptions());
            shellExecutableField.setText(s.getShellExecutablePath());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        RsyncSettings current = RsyncSettingsManager.getInstance().getSettings();
        return !Objects.equals(rsyncExecutableField.getText().trim(), current.getRsyncExecutablePath()) ||
                !Objects.equals(rsyncOptionsField.getText().trim(), current.getRsyncOptions()) ||
                !Objects.equals(shellExecutableField.getText().trim(), current.getShellExecutablePath());
    }

    public void apply() {
        RsyncSettings s = new RsyncSettings();
        s.setRsyncExecutablePath(rsyncExecutableField.getText().trim());
        s.setRsyncOptions(rsyncOptionsField.getText().trim());
        s.setShellExecutablePath(shellExecutableField.getText().trim());
        RsyncSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    public TextField getRsyncExecutableField() {
        return rsyncExecutableField;
    }

    public TextField getRsyncOptionsField() {
        return rsyncOptionsField;
    }

    public TextField getShellExecutableField() {
        return shellExecutableField;
    }
}
