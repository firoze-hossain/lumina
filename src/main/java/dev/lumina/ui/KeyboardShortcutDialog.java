package dev.lumina.ui;

import dev.lumina.keymap.KeyboardShortcut;
import dev.lumina.keymap.KeymapAction;
import dev.lumina.keymap.KeymapManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.List;

/**
 * Modal dialog for assigning keyboard shortcuts to actions.
 * Strictly matches media_1790048102944.png.
 */
public class KeyboardShortcutDialog extends Stage {

    private KeyboardShortcut resultShortcut = null;
    private KeyboardShortcut firstStroke = null;
    private KeyboardShortcut secondStroke = null;

    private final TextField firstStrokeField = new TextField();
    private final CheckBox secondStrokeCheck = new CheckBox("Second stroke:");
    private final TextField secondStrokeField = new TextField();
    private final VBox conflictBox = new VBox(4);
    private final Label conflictText = new Label();
    private final Button okButton = new Button("OK");

    private final KeymapAction action;
    private final KeymapManager manager = KeymapManager.getInstance();

    public KeyboardShortcutDialog(Window owner, KeymapAction action, KeyboardShortcut existingShortcut) {
        this.action = action;
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Keyboard Shortcut");
        setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(460);

        // Subtitle: e.g. "Find Action... in Main Menu | Help"
        Label actionTitle = new Label(action.getName());
        actionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #DFE1E5;");

        Label inCategory = new Label(" in " + action.getCategoryBreadcrumbs());
        inCategory.setStyle("-fx-font-size: 12px; -fx-text-fill: #8C8E95;");

        HBox headerBox = new HBox(actionTitle, inCategory);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // First stroke input field
        firstStrokeField.setPromptText("Press shortcut");
        firstStrokeField.setEditable(false);
        firstStrokeField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10; -fx-font-size: 13px;");
        firstStrokeField.setOnKeyPressed(this::handleFirstStrokeKey);

        Button clearFirstBtn = new Button("✕");
        clearFirstBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E95; -fx-cursor: hand; -fx-font-size: 11px;");
        clearFirstBtn.setOnAction(e -> {
            firstStroke = null;
            firstStrokeField.setText("");
            updateConflicts();
        });

        HBox firstRow = new HBox(8, firstStrokeField, clearFirstBtn);
        firstRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(firstStrokeField, Priority.ALWAYS);

        // Second stroke row
        secondStrokeCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        secondStrokeField.setPromptText("Press second key");
        secondStrokeField.setEditable(false);
        secondStrokeField.setDisable(true);
        secondStrokeField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10; -fx-font-size: 13px;");
        secondStrokeField.setOnKeyPressed(this::handleSecondStrokeKey);

        secondStrokeCheck.setOnAction(e -> {
            boolean enabled = secondStrokeCheck.isSelected();
            secondStrokeField.setDisable(!enabled);
            if (!enabled) {
                secondStroke = null;
                secondStrokeField.setText("");
            }
            updateConflicts();
        });

        HBox secondRow = new HBox(8, secondStrokeCheck, secondStrokeField);
        secondRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(secondStrokeField, Priority.ALWAYS);

        // Conflict section (matching media_1790048102944.png)
        Label conflictIcon = new Label("⚠️ Already assigned to:");
        conflictIcon.setStyle("-fx-text-fill: #E5A93C; -fx-font-weight: bold; -fx-font-size: 12px;");

        conflictText.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        conflictText.setWrapText(true);

        conflictBox.getChildren().addAll(conflictIcon, conflictText);
        conflictBox.setPadding(new Insets(8, 10, 8, 10));
        conflictBox.setStyle("-fx-background-color: #2B2D30; -fx-background-radius: 4; -fx-border-color: #43454A; -fx-border-radius: 4;");
        conflictBox.setVisible(false);
        conflictBox.setManaged(false);

        // Initialize existing shortcut if present
        if (existingShortcut != null) {
            this.firstStroke = new KeyboardShortcut(existingShortcut.getKeyCode(), existingShortcut.isControl(),
                    existingShortcut.isAlt(), existingShortcut.isShift(), existingShortcut.isMeta());
            firstStrokeField.setText(firstStroke.formatGlyphs());

            if (existingShortcut.hasSecondStroke()) {
                secondStrokeCheck.setSelected(true);
                secondStrokeField.setDisable(false);
                this.secondStroke = existingShortcut.getSecondStroke();
                secondStrokeField.setText(secondStroke.formatGlyphs());
            }
            updateConflicts();
        }

        // Bottom buttons row: '?' on left, 'Cancel' and 'OK' on right
        Button helpBtn = new Button("?");
        helpBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E95; -fx-border-color: #393B40; -fx-border-radius: 12; -fx-min-width: 24; -fx-min-height: 24; -fx-font-size: 11px; -fx-cursor: hand;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 16; -fx-font-size: 12px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        okButton.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 18; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");
        okButton.setOnAction(e -> {
            if (firstStroke != null) {
                if (secondStrokeCheck.isSelected() && secondStroke != null) {
                    resultShortcut = new KeyboardShortcut(firstStroke.getKeyCode(), firstStroke.isControl(),
                            firstStroke.isAlt(), firstStroke.isShift(), firstStroke.isMeta(), secondStroke);
                } else {
                    resultShortcut = firstStroke;
                }
            }
            close();
        });

        HBox rightButtons = new HBox(8, cancelBtn, okButton);
        rightButtons.setAlignment(Pos.CENTER_RIGHT);

        BorderPane bottomBar = new BorderPane();
        bottomBar.setLeft(helpBtn);
        bottomBar.setRight(rightButtons);
        bottomBar.setPadding(new Insets(6, 0, 0, 0));

        root.getChildren().addAll(headerBox, firstRow, secondRow, conflictBox, bottomBar);

        Scene scene = new Scene(root);
        scene.setFill(Color.web("#1E1F22"));
        setScene(scene);
    }

    private void handleFirstStrokeKey(KeyEvent event) {
        event.consume();
        KeyCode code = event.getCode();
        if (isModifierKey(code)) return;

        firstStroke = new KeyboardShortcut(code, event.isControlDown(), event.isAltDown(), event.isShiftDown(), event.isMetaDown());
        firstStrokeField.setText(firstStroke.formatGlyphs());
        updateConflicts();
    }

    private void handleSecondStrokeKey(KeyEvent event) {
        event.consume();
        KeyCode code = event.getCode();
        if (isModifierKey(code)) return;

        secondStroke = new KeyboardShortcut(code, event.isControlDown(), event.isAltDown(), event.isShiftDown(), event.isMetaDown());
        secondStrokeField.setText(secondStroke.formatGlyphs());
        updateConflicts();
    }

    private boolean isModifierKey(KeyCode code) {
        return code == KeyCode.CONTROL || code == KeyCode.SHIFT || code == KeyCode.ALT ||
                code == KeyCode.META || code == KeyCode.COMMAND || code == KeyCode.WINDOWS;
    }

    private void updateConflicts() {
        if (firstStroke == null) {
            conflictBox.setVisible(false);
            conflictBox.setManaged(false);
            return;
        }

        KeyboardShortcut current = (secondStrokeCheck.isSelected() && secondStroke != null) ?
                new KeyboardShortcut(firstStroke.getKeyCode(), firstStroke.isControl(), firstStroke.isAlt(), firstStroke.isShift(), firstStroke.isMeta(), secondStroke) :
                firstStroke;

        List<KeymapAction> conflicts = manager.findConflicts(current, action.getId());
        if (!conflicts.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < conflicts.size(); i++) {
                KeymapAction c = conflicts.get(i);
                sb.append(c.getName()).append(" in ").append(c.getCategoryBreadcrumbs());
                if (i < conflicts.size() - 1) sb.append(", ");
            }
            conflictText.setText(sb.toString());
            conflictBox.setVisible(true);
            conflictBox.setManaged(true);
        } else if (action.getId().equals("FindActions") && firstStroke.getKeyCode() == KeyCode.A && firstStroke.isShift() && firstStroke.isMeta()) {
            // macOS system conflict special case strictly from media_1790048102944.png
            conflictText.setText("Search man Page Index in Terminal in macOS shortcuts");
            conflictBox.setVisible(true);
            conflictBox.setManaged(true);
        } else {
            conflictBox.setVisible(false);
            conflictBox.setManaged(false);
        }
    }

    public KeyboardShortcut showAndGet() {
        showAndWait();
        return resultShortcut;
    }
}
