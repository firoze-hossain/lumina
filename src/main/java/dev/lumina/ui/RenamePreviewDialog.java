package dev.lumina.ui;

import dev.lumina.semantics.SemanticEngine;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * M5 — the rename preview: every resolved usage with a checkbox, IntelliJ's
 * "Refactoring Preview" pattern. Nothing is touched until Refactor is
 * pressed; unchecked rows are skipped.
 */
public class RenamePreviewDialog {

    /** One row: the usage plus its include-checkbox state. */
    private static final class Row {
        final SemanticEngine.Usage usage;
        boolean included = true;

        Row(SemanticEngine.Usage usage) {
            this.usage = usage;
        }
    }

    private final Stage dialog = new Stage();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    public RenamePreviewDialog(Stage owner, Path projectRoot, String oldName,
                               String newName,
                               List<SemanticEngine.Usage> usages,
                               Consumer<List<SemanticEngine.Usage>> onApply) {
        for (SemanticEngine.Usage usage : usages) {
            rows.add(new Row(usage));
        }

        Label header = new Label("Rename '" + oldName + "' \u2192 '" + newName
                + "'  \u2014  " + usages.size() + " usages in "
                + usages.stream().map(SemanticEngine.Usage::file).distinct().count()
                + " files");
        header.getStyleClass().add("rename-header");

        ListView<Row> list = new ListView<>(rows);
        list.getStyleClass().add("rename-list");
        list.setCellFactory(lv -> new RowCell(projectRoot));

        Button apply = new Button("Refactor");
        apply.getStyleClass().add("primary-button");
        apply.setDefaultButton(true);
        apply.setOnAction(e -> {
            List<SemanticEngine.Usage> chosen = new ArrayList<>();
            for (Row row : rows) {
                if (row.included) chosen.add(row.usage);
            }
            dialog.close();
            if (!chosen.isEmpty()) onApply.accept(chosen);
        });
        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, cancel, apply);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("rename-dialog");
        root.setPadding(new Insets(14));
        root.setTop(header);
        BorderPane.setMargin(header, new Insets(0, 0, 10, 0));
        root.setCenter(list);
        root.setBottom(buttons);

        Scene scene = new Scene(root, 640, 440);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.setScene(scene);
        dialog.setTitle("Rename \u2014 preview");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
    }

    public void show() {
        dialog.show();
    }

    private static final class RowCell extends ListCell<Row> {
        private final Path projectRoot;

        RowCell(Path projectRoot) {
            this.projectRoot = projectRoot;
        }

        @Override
        protected void updateItem(Row row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            CheckBox include = new CheckBox();
            include.setSelected(row.included);
            include.selectedProperty().addListener(
                    (obs, was, is) -> row.included = is);

            String where;
            try {
                where = projectRoot != null
                        ? projectRoot.relativize(row.usage.file()).toString()
                        : row.usage.file().getFileName().toString();
            } catch (Exception e) {
                where = row.usage.file().getFileName().toString();
            }
            Label location = new Label((row.usage.declaration() ? "\u2605 " : "")
                    + where + ":" + row.usage.line());
            location.getStyleClass().add("rename-location");
            Label preview = new Label(row.usage.preview());
            preview.getStyleClass().add("rename-preview");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox box = new HBox(9, include, location, spacer, preview);
            box.setAlignment(Pos.CENTER_LEFT);
            setGraphic(box);
            setText(null);
        }
    }
}