package dev.lumina.ui;

import dev.lumina.util.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;

/**
 * Settings &gt; Tools &gt; Terminal. Laid out like IntelliJ's own page, but
 * only the controls that map to something {@link dev.lumina.ui.TerminalPane}
 * and {@link dev.lumina.ui.TerminalToolWindow} actually read from
 * ({@link Settings}) are wired to persist and take effect on the next
 * terminal session: shell path, start directory, font size, cursor shape,
 * and default tab name. The rest (terminal engine, command-completion
 * behavior, most of the checkboxes) are shown for visual completeness but
 * aren't backed by real functionality yet \u2014 there's no second terminal
 * engine, no parameter-completion popup, etc. to switch between.
 */
public class SettingsTerminalPage extends VBox {

    public SettingsTerminalPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        Label engineLabel = new Label("Terminal engine:");
        engineLabel.getStyleClass().add("settings-label");
        ComboBox<String> engine = new ComboBox<>();
        engine.getItems().addAll("Reworked 2025", "Classic");
        engine.setValue("Reworked 2025");
        engine.getStyleClass().add("settings-combo");
        HBox engineRow = new HBox(8, engineLabel, engine);
        engineRow.setAlignment(Pos.CENTER_LEFT);

        Label completionLabel = new Label("Command Completion");
        completionLabel.getStyleClass().add("settings-section");
        CheckBox showCompletion = new CheckBox("Show a completion popup as you type");
        showCompletion.setSelected(true);
        showCompletion.getStyleClass().add("settings-check");

        Label projectLabel = new Label("Project Settings");
        projectLabel.getStyleClass().add("settings-section");

        TextField startDir = new TextField(startDirectoryOrDefault());
        Button browseDir = new Button("\u2026");
        browseDir.getStyleClass().add("console-button");
        browseDir.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Start Directory");
            File current = new File(startDir.getText());
            if (current.isDirectory()) chooser.setInitialDirectory(current);
            File chosen = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (chosen != null) startDir.setText(chosen.getAbsolutePath());
        });
        startDir.textProperty().addListener((obs, old, val) ->
                Settings.put(Settings.TERMINAL_START_DIR, val));
        HBox startDirRow = new HBox(8, new Label("Start directory:"), startDir, browseDir);
        startDirRow.setAlignment(Pos.CENTER_LEFT);
        startDir.setPrefWidth(360);

        Label fontLabel = new Label("Font Settings");
        fontLabel.getStyleClass().add("settings-section");

        TextField fontSize = new TextField(fontSizeOrDefault());
        fontSize.setPrefWidth(60);
        fontSize.textProperty().addListener((obs, old, val) -> {
            if (val.matches("\\d{1,2}(\\.\\d)?")) Settings.put(Settings.TERMINAL_FONT_SIZE, val);
        });
        HBox fontRow = new HBox(8, new Label("Size:"), fontSize);
        fontRow.setAlignment(Pos.CENTER_LEFT);

        Label appLabel = new Label("Application Settings");
        appLabel.getStyleClass().add("settings-section");

        TextField shellPath = new TextField(shellPathOrDefault());
        shellPath.setPrefWidth(300);
        shellPath.setPromptText("Auto-detected from $SHELL");
        shellPath.textProperty().addListener((obs, old, val) ->
                Settings.put(Settings.TERMINAL_SHELL_PATH, val));
        Button browseShell = new Button("\u2026");
        browseShell.getStyleClass().add("console-button");
        browseShell.setOnAction(e -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Select Shell Executable");
            File chosen = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (chosen != null) shellPath.setText(chosen.getAbsolutePath());
        });
        HBox shellRow = new HBox(8, new Label("Shell path:"), shellPath, browseShell);
        shellRow.setAlignment(Pos.CENTER_LEFT);

        TextField tabName = new TextField(tabNameOrDefault());
        tabName.setPrefWidth(150);
        tabName.textProperty().addListener((obs, old, val) ->
                Settings.put(Settings.TERMINAL_TAB_NAME, val.isBlank() ? "Local" : val));
        HBox tabRow = new HBox(8, new Label("Default tab name:"), tabName);
        tabRow.setAlignment(Pos.CENTER_LEFT);

        Label cursorLabel = new Label("Cursor shape:");
        ComboBox<String> cursorShape = new ComboBox<>();
        cursorShape.getItems().addAll("Block", "Underline", "Vertical Line");
        cursorShape.setValue(cursorShapeOrDefault());
        cursorShape.getStyleClass().add("settings-combo");
        cursorShape.valueProperty().addListener((obs, old, val) ->
                Settings.put(Settings.TERMINAL_CURSOR_SHAPE, val));
        HBox cursorRow = new HBox(8, cursorLabel, cursorShape);
        cursorRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox audibleBell = new CheckBox("Audible bell");
        audibleBell.setSelected(true);
        audibleBell.getStyleClass().add("settings-check");
        CheckBox closeOnEnd = new CheckBox("Close session when it ends");
        closeOnEnd.setSelected(true);
        closeOnEnd.getStyleClass().add("settings-check");

        VBox appBox = new VBox(6, shellRow, tabRow, cursorRow, audibleBell, closeOnEnd);
        appBox.setPadding(new Insets(4, 0, 0, 0));

        Label note = new Label(
                "Shell path, start directory, font size, cursor shape, and default tab "
                        + "name take effect for new terminal sessions. The engine picker, "
                        + "command-completion popup, and remaining checkboxes are shown to "
                        + "match IntelliJ's layout but aren't wired to real behavior yet.");
        note.getStyleClass().add("settings-placeholder");
        note.setWrapText(true);

        getChildren().addAll(
                engineRow,
                completionLabel, showCompletion,
                projectLabel, startDirRow,
                fontLabel, fontRow,
                appLabel, appBox,
                note);
    }

    private static String startDirectoryOrDefault() {
        String v = Settings.get(Settings.TERMINAL_START_DIR);
        return v != null ? v : "";
    }

    private static String shellPathOrDefault() {
        String v = Settings.get(Settings.TERMINAL_SHELL_PATH);
        return v != null ? v : "";
    }

    private static String fontSizeOrDefault() {
        String v = Settings.get(Settings.TERMINAL_FONT_SIZE);
        return v != null ? v : "12";
    }

    private static String tabNameOrDefault() {
        String v = Settings.get(Settings.TERMINAL_TAB_NAME);
        return v != null ? v : "Local";
    }

    private static String cursorShapeOrDefault() {
        String v = Settings.get(Settings.TERMINAL_CURSOR_SHAPE);
        return v != null ? v : "Block";
    }
}