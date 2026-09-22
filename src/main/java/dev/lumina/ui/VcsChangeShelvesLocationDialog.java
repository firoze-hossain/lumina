package dev.lumina.ui;

import dev.lumina.git.VcsShelfSettingsManager;
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
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;

/**
 * Modal dialog matching IntelliJ IDEA Image 4: "Change Shelves Location".
 * Allows selecting custom or default shelf directory and migrating stashed shelves.
 */
public class VcsChangeShelvesLocationDialog extends Stage {

    private final VcsShelfSettingsManager manager = VcsShelfSettingsManager.getInstance();

    private final RadioButton customRadio = new RadioButton("Custom directory:");
    private final TextField customPathField = new TextField();
    private final Button browseBtn = new Button();

    private final RadioButton defaultRadio = new RadioButton();
    private final CheckBox moveShelvesCheck = new CheckBox("Move shelves to the new location");

    private final Button changeLocationBtn = new Button("Change Location");
    private final Button cancelBtn = new Button("Cancel");

    private boolean confirmed = false;

    public VcsChangeShelvesLocationDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Change Shelves Location");
        setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(18, 20, 18, 20));
        root.setStyle("-fx-background-color: #2B2D30; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // Header label
        Label storeLabel = new Label("Store shelves in:");
        storeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ToggleGroup group = new ToggleGroup();
        customRadio.setToggleGroup(group);
        defaultRadio.setToggleGroup(group);

        customRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        defaultRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        Path defaultLoc = manager.getDefaultShelvesLocation();
        defaultRadio.setText("Default directory: " + defaultLoc.toString());

        Path currentLoc = manager.getCurrentShelvesLocation();
        boolean isCustom = manager.isCustomLocationConfigured();

        customPathField.setText(isCustom ? currentLoc.toString() : defaultLoc.toString());
        customPathField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(customPathField, Priority.ALWAYS);

        SVGPath folderIcon = new SVGPath();
        folderIcon.setContent("M 2 3 L 5 3 L 6.5 5 L 12 5 L 12 11 L 2 11 Z");
        folderIcon.setFill(Color.TRANSPARENT);
        folderIcon.setStroke(Color.web("#848BA3"));
        folderIcon.setStrokeWidth(1.2);
        browseBtn.setGraphic(folderIcon);
        browseBtn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Shelf Directory");
            File current = new File(customPathField.getText().trim());
            if (current.exists() && current.isDirectory()) {
                dc.setInitialDirectory(current);
            }
            File chosen = dc.showDialog(this);
            if (chosen != null) {
                customPathField.setText(chosen.getAbsolutePath());
                updateButtonState();
            }
        });

        HBox customBox = new HBox(8, customRadio, customPathField, browseBtn);
        customBox.setAlignment(Pos.CENTER_LEFT);

        if (isCustom) {
            customRadio.setSelected(true);
        } else {
            defaultRadio.setSelected(true);
        }

        // Enable/disable custom field according to radio selection
        customPathField.disableProperty().bind(customRadio.selectedProperty().not());
        browseBtn.disableProperty().bind(customRadio.selectedProperty().not());

        moveShelvesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        moveShelvesCheck.setSelected(false);

        // Bottom Action Bar
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        // Help ? circle button
        Label helpBtn = new Label("?");
        helpBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #6F737A; -fx-border-radius: 10; -fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-min-width: 18px; -fx-min-height: 18px; -fx-max-width: 18px; -fx-max-height: 18px; -fx-alignment: center; -fx-cursor: hand;");
        helpBtn.setTooltip(new Tooltip("Shelf allows you to store pending changes without committing them"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        changeLocationBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 16 6 16; -fx-background-radius: 4; -fx-cursor: hand;");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 16 6 16; -fx-font-size: 13px; -fx-cursor: hand;");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> close());

        group.selectedToggleProperty().addListener((obs, oldV, newV) -> updateButtonState());
        customPathField.textProperty().addListener((obs, oldV, newV) -> updateButtonState());

        changeLocationBtn.setOnAction(e -> {
            String targetPath = customRadio.isSelected() ? customPathField.getText().trim() : defaultLoc.toString();
            manager.setShelvesLocation(targetPath, moveShelvesCheck.isSelected());
            confirmed = true;
            close();
        });

        updateButtonState();

        bottomBar.getChildren().addAll(helpBtn, spacer, changeLocationBtn, cancelBtn);
        root.getChildren().addAll(storeLabel, customBox, defaultRadio, moveShelvesCheck, bottomBar);

        Scene scene = new Scene(root, 520, 220);
        setScene(scene);
    }

    private void updateButtonState() {
        Path currentLoc = manager.getCurrentShelvesLocation();
        Path defaultLoc = manager.getDefaultShelvesLocation();

        Path selectedLoc;
        if (customRadio.isSelected()) {
            String txt = customPathField.getText().trim();
            if (txt.isEmpty()) {
                changeLocationBtn.setDisable(true);
                return;
            }
            try {
                selectedLoc = Path.of(txt).toAbsolutePath().normalize();
            } catch (Exception e) {
                changeLocationBtn.setDisable(true);
                return;
            }
        } else {
            selectedLoc = defaultLoc;
        }

        boolean changed = !selectedLoc.equals(currentLoc);
        changeLocationBtn.setDisable(!changed);
        if (changed) {
            changeLocationBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 16 6 16; -fx-background-radius: 4; -fx-cursor: hand;");
        } else {
            changeLocationBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-padding: 6 16 6 16; -fx-background-radius: 4; -fx-cursor: default;");
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
