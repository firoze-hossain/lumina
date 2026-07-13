package dev.lumina.ui;

import dev.lumina.diagnostics.JavaDiagnostics;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * M3 — the Problems tool window: live compiler diagnostics for the current
 * editor. Clicking a row jumps to its line.
 */
public class ProblemsPanel extends BorderPane {

    private final ListView<JavaDiagnostics.Diag> list = new ListView<>();
    private final Label summary = new Label("No problems");
    private IntConsumer onJump;

    public ProblemsPanel() {
        getStyleClass().add("problems-panel");

        summary.getStyleClass().add("problems-summary");
        HBox top = new HBox(summary);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("problems-bar");

        list.getStyleClass().add("problems-list");
        list.setPlaceholder(new Label("No problems in the current file"));
        list.setCellFactory(lv -> new DiagCell());
        list.setOnMouseClicked(e -> {
            JavaDiagnostics.Diag diag = list.getSelectionModel().getSelectedItem();
            if (diag != null && onJump != null) onJump.accept(diag.line());
        });

        setTop(top);
        setCenter(list);
    }

    public void setOnJump(IntConsumer onJump) {
        this.onJump = onJump;
    }

    /** Replace the shown diagnostics (any thread). */
    public void show(List<JavaDiagnostics.Diag> diagnostics) {
        Runnable apply = () -> {
            list.getItems().setAll(diagnostics == null ? List.of() : diagnostics);
            long errors = list.getItems().stream()
                    .filter(d -> d.severity() == JavaDiagnostics.Severity.ERROR)
                    .count();
            long warnings = list.getItems().size() - errors;
            if (list.getItems().isEmpty()) {
                summary.setText("No problems");
                summary.setStyle("-fx-text-fill: #7CB87C;");
            } else {
                summary.setText(errors + (errors == 1 ? " error" : " errors")
                        + ", " + warnings + (warnings == 1 ? " warning" : " warnings"));
                summary.setStyle(errors > 0 ? "-fx-text-fill: #E5534B;"
                        : "-fx-text-fill: #D8A657;");
            }
        };
        if (Platform.isFxApplicationThread()) apply.run();
        else Platform.runLater(apply);
    }

    private static final class DiagCell extends ListCell<JavaDiagnostics.Diag> {
        @Override
        protected void updateItem(JavaDiagnostics.Diag diag, boolean empty) {
            super.updateItem(diag, empty);
            if (empty || diag == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            boolean error = diag.severity() == JavaDiagnostics.Severity.ERROR;
            Label badge = new Label(error ? "\u2716" : "\u26A0");
            badge.setStyle("-fx-text-fill: " + (error ? "#E5534B" : "#D8A657")
                    + "; -fx-font-size: 11px;");
            badge.setMinWidth(18);
            Label where = new Label(diag.line() + ":");
            where.getStyleClass().add("problems-line");
            Label message = new Label(firstLine(diag.message()));
            message.getStyleClass().add("problems-message");
            HBox row = new HBox(7, badge, where, message);
            row.setAlignment(Pos.CENTER_LEFT);
            setGraphic(row);
            setText(null);
        }

        private static String firstLine(String message) {
            int nl = message.indexOf('\n');
            return nl < 0 ? message : message.substring(0, nl);
        }
    }
}