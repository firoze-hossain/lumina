package dev.lumina.copyright;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * Dialog for adding a scope-to-copyright mapping in the Editor > Copyright page.
 */
public class AddScopeMappingDialog {

    private final Stage stage;
    private final ComboBox<String> scopeCombo = new ComboBox<>();
    private final ComboBox<String> copyrightCombo = new ComboBox<>();
    private ScopeCopyrightMapping result = null;

    public AddScopeMappingDialog(Window owner, List<String> availableScopes, List<String> availableProfiles) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Add Scope Copyright Mapping");
        stage.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(18, 22, 16, 22));
        root.setStyle("-fx-background-color: #2B2D30;");
        root.setPrefWidth(420);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        Label scopeLabel = new Label("Scope:");
        scopeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        scopeCombo.setStyle(comboStyle());
        scopeCombo.setPrefWidth(260);
        if (availableScopes != null && !availableScopes.isEmpty()) {
            scopeCombo.getItems().addAll(availableScopes);
            scopeCombo.getSelectionModel().selectFirst();
        } else {
            scopeCombo.getItems().addAll("All Places", "Project Files", "Production", "Tests", "Scratches and Consoles");
            scopeCombo.getSelectionModel().selectFirst();
        }

        Label copyrightLabel = new Label("Copyright:");
        copyrightLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        copyrightCombo.setStyle(comboStyle());
        copyrightCombo.setPrefWidth(260);
        if (availableProfiles != null && !availableProfiles.isEmpty()) {
            copyrightCombo.getItems().addAll(availableProfiles);
            copyrightCombo.getSelectionModel().selectFirst();
        } else {
            copyrightCombo.getItems().add("No copyright");
            copyrightCombo.getSelectionModel().selectFirst();
        }

        grid.add(scopeLabel, 0, 0);
        grid.add(scopeCombo, 1, 0);
        grid.add(copyrightLabel, 0, 1);
        grid.add(copyrightCombo, 1, 1);

        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle(
                "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-padding: 5 18; -fx-background-radius: 4; -fx-cursor: hand;"
        );

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle(
                "-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; " +
                "-fx-padding: 5 16; -fx-background-radius: 4; -fx-cursor: hand;"
        );

        okBtn.setOnAction(e -> {
            String scope = scopeCombo.getValue();
            String profile = copyrightCombo.getValue();
            if (scope != null && !scope.trim().isEmpty() && profile != null && !profile.trim().isEmpty()) {
                result = new ScopeCopyrightMapping(scope.trim(), profile.trim());
                stage.close();
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        buttonBar.getChildren().addAll(okBtn, cancelBtn);

        root.getChildren().addAll(grid, buttonBar);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private static String comboStyle() {
        return "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 3 6; -fx-font-size: 12px;";
    }

    public static Optional<ScopeCopyrightMapping> show(Window owner, List<String> availableScopes, List<String> availableProfiles) {
        AddScopeMappingDialog dialog = new AddScopeMappingDialog(owner, availableScopes, availableProfiles);
        dialog.stage.showAndWait();
        return Optional.ofNullable(dialog.result);
    }
}
