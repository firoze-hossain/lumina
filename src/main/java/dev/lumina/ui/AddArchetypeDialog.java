package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.function.Consumer;

/**
 * Modal dialog for adding a Maven Archetype matching IntelliJ IDEA.
 */
public class AddArchetypeDialog {

    public record ArchetypeRecord(String groupId, String artifactId, String version, String repository) {
        public String coordinate() {
            return groupId.trim() + ":" + artifactId.trim();
        }
    }

    private final Stage dialog = new Stage();
    private final TextField groupField = new TextField();
    private final TextField artifactField = new TextField();
    private final TextField versionField = new TextField();
    private final TextField repoField = new TextField();
    private final Consumer<ArchetypeRecord> onAdd;

    public AddArchetypeDialog(Window owner, Consumer<ArchetypeRecord> onAdd) {
        this.onAdd = onAdd;

        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Archetype");
        dialog.setResizable(true);
        dialog.setMinWidth(460);
        dialog.setMinHeight(270);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(18, 20, 12, 20));

        ColumnConstraints col0 = new ColumnConstraints(95);
        col0.setMinWidth(95);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        groupField.setPromptText("e.g. org.apache.maven.archetypes");
        groupField.setMaxWidth(Double.MAX_VALUE);
        artifactField.setPromptText("e.g. maven-archetype-quickstart");
        artifactField.setMaxWidth(Double.MAX_VALUE);
        versionField.setPromptText("e.g. 1.4");
        versionField.setMaxWidth(Double.MAX_VALUE);
        repoField.setPromptText("Optional (e.g. https://repo.maven.apache.org/maven2)");
        repoField.setMaxWidth(Double.MAX_VALUE);

        Label groupLbl = createLabel("GroupId:");
        Label artifactLbl = createLabel("ArtifactId:");
        Label versionLbl = createLabel("Version:");
        Label repoLbl = createLabel("Repository:");

        grid.add(groupLbl, 0, 0);
        grid.add(groupField, 1, 0);
        grid.add(artifactLbl, 0, 1);
        grid.add(artifactField, 1, 1);
        grid.add(versionLbl, 0, 2);
        grid.add(versionField, 1, 2);
        grid.add(repoLbl, 0, 3);
        grid.add(repoField, 1, 3);

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().setAll("dialog-primary");
        addBtn.setPrefWidth(76);
        addBtn.setMinWidth(76);
        addBtn.setOnAction(e -> handleAdd());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().setAll("dialog-secondary");
        cancelBtn.setPrefWidth(76);
        cancelBtn.setMinWidth(76);
        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttonBar = new HBox(10, addBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 20, 20, 20));

        VBox.setVgrow(grid, Priority.ALWAYS);
        VBox root = new VBox(grid, buttonBar);
        root.setStyle("-fx-background-color: #1E1F22;");

        Scene scene = new Scene(root, 480, 280);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        } catch (Exception ignored) {}

        dialog.setScene(scene);
        dialog.sizeToScene();
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return lbl;
    }

    private void handleAdd() {
        String group = groupField.getText().trim();
        String artifact = artifactField.getText().trim();
        String version = versionField.getText().trim();
        String repo = repoField.getText().trim();

        if (group.isEmpty() || artifact.isEmpty() || version.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(dialog);
            alert.setHeaderText(null);
            alert.setContentText("GroupId, ArtifactId, and Version are required.");
            try {
                alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
            } catch (Exception ignored) {}
            alert.showAndWait();
            return;
        }

        if (onAdd != null) {
            onAdd.accept(new ArchetypeRecord(group, artifact, version, repo));
        }
        dialog.close();
    }

    public void show() {
        dialog.showAndWait();
    }
}
