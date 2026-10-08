package dev.lumina.ui;

import dev.lumina.javafx.JavaFXSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * 1:1 dynamic replica of IntelliJ IDEA Languages & Frameworks > JavaFX settings page.
 * Displays Path to SceneBuilder with executable file chooser.
 */
public class SettingsLanguagesJavaFXPage extends VBox {

    private final JavaFXSettingsManager manager = JavaFXSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private TextField sceneBuilderField;
    private Button browseButton;
    private String initialPath;

    public SettingsLanguagesJavaFXPage() {
        setSpacing(16);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("Path to SceneBuilder:");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        label.setPrefWidth(150);

        sceneBuilderField = new TextField(manager.getPathToSceneBuilder());
        sceneBuilderField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 5 8 5 8;");
        sceneBuilderField.setPrefWidth(420);
        sceneBuilderField.setMaxWidth(600);
        sceneBuilderField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        browseButton = new Button();
        SVGPath folderSvg = new SVGPath();
        folderSvg.setContent("M 2 3 L 6 3 L 7 5 L 14 5 L 14 12 L 2 12 Z");
        folderSvg.setFill(Color.web("#848BA3"));
        folderSvg.setScaleX(0.85);
        folderSvg.setScaleY(0.85);
        browseButton.setGraphic(folderSvg);
        browseButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px; -fx-padding: 2 8 2 8; -fx-cursor: hand;");
        browseButton.setOnMouseEntered(e -> browseButton.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-border-color: #589DF6; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px; -fx-padding: 2 8 2 8; -fx-cursor: hand;"));
        browseButton.setOnMouseExited(e -> browseButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px; -fx-padding: 2 8 2 8; -fx-cursor: hand;"));
        browseButton.setTooltip(new Tooltip("Select SceneBuilder Executable"));

        browseButton.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select SceneBuilder Executable");
            File file = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                sceneBuilderField.setText(file.getAbsolutePath());
            }
        });

        row.getChildren().addAll(label, sceneBuilderField, browseButton);
        getChildren().add(row);
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialPath = sceneBuilderField.getText();
    }

    public boolean isModified() {
        return !Objects.equals(sceneBuilderField.getText(), initialPath);
    }

    public void apply() {
        manager.setPathToSceneBuilder(sceneBuilderField.getText());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        sceneBuilderField.setText(initialPath != null ? initialPath : "");
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public TextField getSceneBuilderField() {
        return sceneBuilderField;
    }

    public Button getBrowseButton() {
        return browseButton;
    }
}
