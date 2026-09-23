package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dialog for "Current File > Compare with Revision..." matching IntelliJ IDEA.
 */
public class CompareWithRevisionDialog extends Stage {

    @FunctionalInterface
    public interface DiffOpener {
        void open(String title, String ref, String relPath, Path localPath, String leftText, String rightText);
    }

    private final Path projectRoot;
    private final Path file;
    private final String currentText;
    private final DiffOpener diffOpener;

    private final ObservableList<GitService.FileRevision> revisions = FXCollections.observableArrayList();
    private final TableView<GitService.FileRevision> table = new TableView<>(revisions);
    private final Button compareBtn = new Button("Compare");
    private final Button cancelBtn = new Button("Cancel");

    public CompareWithRevisionDialog(Stage owner, Path projectRoot, Path file,
                                     String currentText, DiffOpener diffOpener) {
        this.projectRoot = projectRoot;
        this.file = file;
        this.currentText = currentText;
        this.diffOpener = diffOpener;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        String fileName = file != null ? file.getFileName().toString() : "";
        setTitle("Compare " + fileName + " with Revision");
        setMinWidth(620);
        setMinHeight(360);
        setWidth(680);
        setHeight(400);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1E1F22;");

        // Header
        Label headerLbl = new Label("Revisions for " + fileName + ":");
        headerLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 10 14 6 14;");
        root.setTop(headerLbl);

        // Table
        table.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No revisions found for this file"));

        TableColumn<GitService.FileRevision, String> hashCol = new TableColumn<>("Revision");
        hashCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().hash()));
        hashCol.setMinWidth(75);
        hashCol.setMaxWidth(100);

        TableColumn<GitService.FileRevision, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date()));
        dateCol.setMinWidth(90);
        dateCol.setMaxWidth(120);

        TableColumn<GitService.FileRevision, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().author()));
        authorCol.setMinWidth(110);
        authorCol.setMaxWidth(160);

        TableColumn<GitService.FileRevision, String> msgCol = new TableColumn<>("Commit Message");
        msgCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().subject()));
        msgCol.setMinWidth(220);

        table.getColumns().addAll(hashCol, dateCol, authorCol, msgCol);

        compareBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                doCompare();
            }
        });

        table.setRowFactory(tv -> {
            TableRow<GitService.FileRevision> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    doCompare();
                }
            });
            return row;
        });

        VBox center = new VBox(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        center.setPadding(new Insets(0, 14, 0, 14));
        root.setCenter(center);

        // Bottom Bar
        HBox bottomBar = new HBox(8);
        bottomBar.setPadding(new Insets(10, 14, 12, 14));
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2B2D30 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        compareBtn.setDefaultButton(true);
        compareBtn.setStyle(
                "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 4; -fx-padding: 5 18; -fx-cursor: hand;"
        );
        compareBtn.setOnAction(e -> doCompare());

        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; " +
                "-fx-background-radius: 4; -fx-padding: 5 14; -fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> close());

        bottomBar.getChildren().addAll(compareBtn, cancelBtn);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root);
        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        setScene(scene);
        loadData();
    }

    private void loadData() {
        if (projectRoot == null || file == null) return;
        String rel = projectRoot.relativize(file).toString().replace('\\', '/');
        List<GitService.FileRevision> list = GitService.fileRevisions(projectRoot, rel, 200);
        revisions.setAll(list);
        if (!revisions.isEmpty()) {
            table.getSelectionModel().select(0);
        }
    }

    private void doCompare() {
        GitService.FileRevision sel = table.getSelectionModel().getSelectedItem();
        if (sel == null || projectRoot == null || file == null) return;

        String rel = projectRoot.relativize(file).toString().replace('\\', '/');
        GitService.Result r = GitService.fileContentAtRevision(projectRoot, sel.hash(), rel);
        String oldContent = r.ok() ? r.output() : "";

        String rightContent = currentText;
        if (rightContent == null && Files.exists(file)) {
            try {
                rightContent = Files.readString(file);
            } catch (Exception ignored) {
                rightContent = "";
            }
        }
        if (rightContent == null) rightContent = "";

        close();
        if (diffOpener != null) {
            String title = "Diff " + file.getFileName() + " (" + sel.hash() + " vs Current)";
            diffOpener.open(title, sel.hash(), rel, file, oldContent, rightContent);
        }
    }

    public TableView<GitService.FileRevision> getTable() { return table; }
    public ObservableList<GitService.FileRevision> getRevisions() { return revisions; }
    public Button getCompareBtn() { return compareBtn; }
    public Button getCancelBtn() { return cancelBtn; }
}
