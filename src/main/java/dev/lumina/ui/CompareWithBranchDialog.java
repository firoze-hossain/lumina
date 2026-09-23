package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for "Current File > Compare with Branch or Tag..." matching IntelliJ IDEA.
 */
public class CompareWithBranchDialog extends Stage {

    public interface DiffOpener {
        void open(String title, String ref, String relPath, Path localPath, String leftText, String rightText);
    }

    private final Path projectRoot;
    private final Path file;
    private final String currentText;
    private final DiffOpener diffOpener;

    private final ObservableList<String> allRefs = FXCollections.observableArrayList();
    private final FilteredList<String> filteredRefs = new FilteredList<>(allRefs, p -> true);
    private final ListView<String> refList = new ListView<>(filteredRefs);
    private final TextField filterField = new TextField();
    private final Button compareBtn = new Button("Compare");
    private final Button cancelBtn = new Button("Cancel");

    public CompareWithBranchDialog(Stage owner, Path projectRoot, Path file,
                                   String currentText, DiffOpener diffOpener) {
        this.projectRoot = projectRoot;
        this.file = file;
        this.currentText = currentText;
        this.diffOpener = diffOpener;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        String fileName = file != null ? file.getFileName().toString() : "";
        setTitle("Compare " + fileName + " with Branch or Tag");
        setMinWidth(480);
        setMinHeight(360);
        setWidth(520);
        setHeight(400);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1E1F22;");

        // Top Filter
        VBox topBox = new VBox(8);
        topBox.setPadding(new Insets(12, 14, 8, 14));
        Label prompt = new Label("Select branch or tag to compare with:");
        prompt.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        filterField.setPromptText("Filter branches and tags...");
        filterField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 6 10;");
        filterField.textProperty().addListener((obs, o, text) -> {
            String q = text != null ? text.trim().toLowerCase() : "";
            filteredRefs.setPredicate(item -> q.isEmpty() || item.toLowerCase().contains(q));
        });

        topBox.getChildren().addAll(prompt, filterField);
        root.setTop(topBox);

        // Center List
        refList.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2B2D30;");
        refList.setPlaceholder(new Label("No branches or tags found"));

        compareBtn.disableProperty().bind(refList.getSelectionModel().selectedItemProperty().isNull());

        refList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                doCompare();
            }
        });

        refList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && refList.getSelectionModel().getSelectedItem() != null) {
                doCompare();
            }
        });

        VBox center = new VBox(refList);
        VBox.setVgrow(refList, Priority.ALWAYS);
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
        loadRefs();
    }

    private void loadRefs() {
        allRefs.clear();
        if (projectRoot == null) return;

        List<String> list = new ArrayList<>();
        // 1. Local branches
        list.addAll(GitService.localBranches(projectRoot));
        // 2. Remote branches
        list.addAll(GitService.remoteBranches(projectRoot));
        // 3. Tags
        list.addAll(GitService.tags(projectRoot));

        allRefs.setAll(list);
        if (!allRefs.isEmpty()) {
            refList.getSelectionModel().select(0);
        }
    }

    private void doCompare() {
        String selectedRef = refList.getSelectionModel().getSelectedItem();
        if (selectedRef == null || projectRoot == null || file == null) return;

        String rel = projectRoot.relativize(file).toString().replace('\\', '/');
        GitService.Result r = GitService.fileContentAtRevision(projectRoot, selectedRef, rel);
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
            String title = "Diff " + file.getFileName() + " (" + selectedRef + " vs Current)";
            diffOpener.open(title, selectedRef, rel, file, oldContent, rightContent);
        }
    }

    public ListView<String> getRefList() { return refList; }
    public ObservableList<String> getAllRefs() { return allRefs; }
    public TextField getFilterField() { return filterField; }
    public Button getCompareBtn() { return compareBtn; }
    public Button getCancelBtn() { return cancelBtn; }
}
