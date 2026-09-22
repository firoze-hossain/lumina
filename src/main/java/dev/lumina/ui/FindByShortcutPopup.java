package dev.lumina.ui;

import dev.lumina.keymap.KeyboardShortcut;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.function.Consumer;

/**
 * Interactive popup dialog to capture a shortcut for filtering actions in Keymap tree.
 * Triggered by the keyboard icon button on the search bar.
 */
public class FindByShortcutPopup extends Stage {

    private KeyboardShortcut capturedShortcut = null;
    private final TextField shortcutField = new TextField();

    public FindByShortcutPopup(Window owner, Consumer<KeyboardShortcut> onShortcutSelected) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setTitle("Find Action by Shortcut");
        setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 18, 16, 18));
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(320);

        Label prompt = new Label("Press shortcut keys to filter:");
        prompt.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        shortcutField.setPromptText("Press shortcut");
        shortcutField.setEditable(false);
        shortcutField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10; -fx-font-size: 13px;");
        shortcutField.setOnKeyPressed(this::handleKey);

        Button clearBtn = new Button("✕");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E95; -fx-cursor: hand; -fx-font-size: 11px;");
        clearBtn.setOnAction(e -> {
            capturedShortcut = null;
            shortcutField.setText("");
        });

        HBox inputRow = new HBox(6, shortcutField, clearBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(shortcutField, Priority.ALWAYS);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        Button findBtn = new Button("Find");
        findBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");
        findBtn.setOnAction(e -> {
            if (capturedShortcut != null && onShortcutSelected != null) {
                onShortcutSelected.accept(capturedShortcut);
            }
            close();
        });

        HBox buttonRow = new HBox(8, cancelBtn, findBtn);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(prompt, inputRow, buttonRow);

        Scene scene = new Scene(root);
        scene.setFill(Color.web("#1E1F22"));
        setScene(scene);
    }

    private void handleKey(KeyEvent event) {
        event.consume();
        KeyCode code = event.getCode();
        if (code == KeyCode.CONTROL || code == KeyCode.SHIFT || code == KeyCode.ALT ||
                code == KeyCode.META || code == KeyCode.COMMAND || code == KeyCode.WINDOWS) {
            return;
        }

        capturedShortcut = new KeyboardShortcut(code, event.isControlDown(), event.isAltDown(), event.isShiftDown(), event.isMetaDown());
        shortcutField.setText(capturedShortcut.formatGlyphs());
    }
}
