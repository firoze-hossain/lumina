package dev.lumina.ui;

import dev.lumina.git.VcsDirectoryMappingManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Path;

/**
 * Modal dialog for Add / Edit VCS Directory Mapping matching IntelliJ IDEA Images 2, 3, 4.
 */
public class VcsDirectoryMappingDialog extends Stage {

    private final RadioButton projectRadio = new RadioButton("Project");
    private final RadioButton directoryRadio = new RadioButton("Directory:");
    private final TextField dirField = new TextField();
    private final Button browseBtn = new Button();
    private final ComboBox<String> vcsCombo = new ComboBox<>();
    private final Button okBtn = new Button("OK");
    private final Button cancelBtn = new Button("Cancel");

    private boolean saved = false;
    private String resultDirectory;
    private String resultVcs;

    public VcsDirectoryMappingDialog(Stage owner, boolean isEdit, String initialDir, String initialVcs) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle(isEdit ? "Edit VCS Directory Mapping" : "Add VCS Directory Mapping");
        setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(18, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Project radio & description
        ToggleGroup group = new ToggleGroup();
        styleRadio(projectRadio, group);
        styleRadio(directoryRadio, group);

        Label projectDesc = new Label("Content roots of all modules, all immediate descendants of project base directory, and .idea directory contents");
        projectDesc.setWrapText(true);
        projectDesc.setMaxWidth(500);
        projectDesc.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");

        VBox projectBox = new VBox(4, projectRadio, projectDesc);

        // 2. Directory radio + textfield + browse
        dirField.setPrefWidth(420);
        dirField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");

        // Folder browse icon
        SVGPath folderIcon = new SVGPath();
        folderIcon.setContent("M 1 2 L 5 2 L 6.5 3.5 L 12 3.5 L 12 10 L 1 10 Z");
        folderIcon.setFill(Color.TRANSPARENT);
        folderIcon.setStroke(Color.web("#848BA3"));
        folderIcon.setStrokeWidth(1.1);

        browseBtn.setGraphic(folderIcon);
        browseBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3 6 3 6;");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select VCS Root Directory");
            String cur = dirField.getText().trim();
            if (!cur.isEmpty()) {
                File f = new File(cur);
                if (f.isDirectory()) dc.setInitialDirectory(f);
            }
            File selected = dc.showDialog(this);
            if (selected != null) {
                dirField.setText(selected.getAbsolutePath());
            }
        });

        HBox dirFieldBox = new HBox(dirField, browseBtn);
        dirFieldBox.setAlignment(Pos.CENTER_LEFT);
        dirFieldBox.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        HBox.setHgrow(dirField, Priority.ALWAYS);

        HBox dirRow = new HBox(8, directoryRadio, dirFieldBox);
        dirRow.setAlignment(Pos.CENTER_LEFT);

        // Radio toggle logic
        projectRadio.selectedProperty().addListener((obs, old, isProj) -> {
            dirFieldBox.setDisable(isProj);
            dirFieldBox.setOpacity(isProj ? 0.4 : 1.0);
        });

        boolean isProjectInitial = initialDir == null || initialDir.isBlank() ||
                VcsDirectoryMappingManager.PROJECT_MAPPING.equalsIgnoreCase(initialDir);
        if (isProjectInitial) {
            projectRadio.setSelected(true);
            dirFieldBox.setDisable(true);
            dirFieldBox.setOpacity(0.4);
            // Default directory path if switched
            Path projPath = VcsDirectoryMappingManager.getInstance().getCurrentProjectPath();
            dirField.setText(projPath != null ? projPath.toAbsolutePath().toString() : "/home/firoze/projects/others/lumina");
        } else {
            directoryRadio.setSelected(true);
            dirField.setText(initialDir);
            dirFieldBox.setDisable(false);
            dirFieldBox.setOpacity(1.0);
        }

        // 3. VCS Dropdown
        Label vcsLbl = new Label("VCS:");
        vcsLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        vcsCombo.getItems().setAll(VcsDirectoryMappingManager.SUPPORTED_VCS);
        String currentVcs = (initialVcs != null && !initialVcs.isBlank()) ? initialVcs : "Git";
        vcsCombo.setValue(currentVcs);
        vcsCombo.setPrefWidth(500);
        vcsCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px;");

        HBox vcsRow = new HBox(22, vcsLbl, vcsCombo);
        vcsRow.setAlignment(Pos.CENTER_LEFT);

        // 4. Dialog Action Buttons
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 22 6 22; -fx-background-radius: 4; -fx-cursor: hand;");
        okBtn.setOnAction(e -> {
            saved = true;
            resultDirectory = projectRadio.isSelected() ? VcsDirectoryMappingManager.PROJECT_MAPPING : dirField.getText().trim();
            resultVcs = vcsCombo.getValue() != null ? vcsCombo.getValue() : "Git";
            close();
        });

        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 6 18 6 18; -fx-font-size: 12px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        HBox buttonBar = new HBox(8, okBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(projectBox, dirRow, vcsRow, buttonBar);

        Scene scene = new Scene(root, 580, 240);
        setScene(scene);
    }

    private void styleRadio(RadioButton rb, ToggleGroup group) {
        rb.setToggleGroup(group);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    public boolean isSaved() {
        return saved;
    }

    public String getResultDirectory() {
        return resultDirectory;
    }

    public String getResultVcs() {
        return resultVcs;
    }
}
