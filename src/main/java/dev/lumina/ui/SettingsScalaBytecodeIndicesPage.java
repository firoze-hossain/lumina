package dev.lumina.ui;

import dev.lumina.build.ScalaBytecodeIndicesSettings;
import dev.lumina.build.ScalaBytecodeIndicesSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Compiler > Scala Compiler > Bytecode Indices.
 * Matches 1:1 with reference screenshot media_1791449926979_4ed8cb9f.png:
 *  - Index .class files
 *  - Delete indices button
 *  - Use indices to search for usages of:
 *    - Implicit definitions
 *    - apply / unapply methods
 *    - SAM types
 *    - For-comprehension methods (map, withFilter, flatMap, foreach)
 */
public class SettingsScalaBytecodeIndicesPage extends VBox {

    private final ScalaBytecodeIndicesSettingsManager manager = ScalaBytecodeIndicesSettingsManager.getInstance();

    private final CheckBox indexClassFilesCheck = new CheckBox("Index .class files");
    private final Button deleteIndicesBtn = new Button("Delete indices");

    private final CheckBox implicitDefinitionsCheck = new CheckBox("Implicit definitions");
    private final CheckBox applyUnapplyCheck = new CheckBox("apply / unapply methods");
    private final CheckBox samTypesCheck = new CheckBox("SAM types");
    private final CheckBox forComprehensionCheck = new CheckBox("For-comprehension methods (map, withFilter, flatMap, foreach)");

    private final VBox searchUsagesBox = new VBox(8);

    private ScalaBytecodeIndicesSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsScalaBytecodeIndicesPage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // 1. Top row: Index .class files + Delete indices button
        styleCheckBox(indexClassFilesCheck);
        indexClassFilesCheck.setOnAction(e -> {
            updateEnabledStates();
            fireModified();
        });

        deleteIndicesBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand; -fx-font-size: 12px;");
        deleteIndicesBtn.setOnAction(e -> {
            deleteIndicesBtn.setText("Indices cleared");
            deleteIndicesBtn.setDisable(true);
        });

        HBox topRow = new HBox(12, indexClassFilesCheck, deleteIndicesBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Section: Use indices to search for usages of:
        Label searchLabel = new Label("Use indices to search for usages of:");
        searchLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        styleCheckBox(implicitDefinitionsCheck);
        styleCheckBox(applyUnapplyCheck);
        styleCheckBox(samTypesCheck);
        styleCheckBox(forComprehensionCheck);

        implicitDefinitionsCheck.setOnAction(e -> fireModified());
        applyUnapplyCheck.setOnAction(e -> fireModified());
        samTypesCheck.setOnAction(e -> fireModified());
        forComprehensionCheck.setOnAction(e -> fireModified());

        searchUsagesBox.getChildren().addAll(
                implicitDefinitionsCheck,
                applyUnapplyCheck,
                samTypesCheck,
                forComprehensionCheck
        );

        getChildren().addAll(topRow, searchLabel, searchUsagesBox);
    }

    private void updateEnabledStates() {
        boolean enabled = indexClassFilesCheck.isSelected();
        deleteIndicesBtn.setDisable(!enabled);
        searchUsagesBox.setDisable(!enabled);
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        indexClassFilesCheck.setSelected(initialSettings.isIndexClassFiles());
        implicitDefinitionsCheck.setSelected(initialSettings.isImplicitDefinitions());
        applyUnapplyCheck.setSelected(initialSettings.isApplyUnapplyMethods());
        samTypesCheck.setSelected(initialSettings.isSamTypes());
        forComprehensionCheck.setSelected(initialSettings.isForComprehensionMethods());

        deleteIndicesBtn.setText("Delete indices");
        updateEnabledStates();
        updating = false;
    }

    public ScalaBytecodeIndicesSettings getCurrentSettings() {
        ScalaBytecodeIndicesSettings s = new ScalaBytecodeIndicesSettings();
        s.setIndexClassFiles(indexClassFilesCheck.isSelected());
        s.setImplicitDefinitions(implicitDefinitionsCheck.isSelected());
        s.setApplyUnapplyMethods(applyUnapplyCheck.isSelected());
        s.setSamTypes(samTypesCheck.isSelected());
        s.setForComprehensionMethods(forComprehensionCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        ScalaBytecodeIndicesSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadData();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public CheckBox getIndexClassFilesCheck() { return indexClassFilesCheck; }
    public Button getDeleteIndicesBtn() { return deleteIndicesBtn; }
    public CheckBox getImplicitDefinitionsCheck() { return implicitDefinitionsCheck; }
    public CheckBox getApplyUnapplyCheck() { return applyUnapplyCheck; }
    public CheckBox getSamTypesCheck() { return samTypesCheck; }
    public CheckBox getForComprehensionCheck() { return forComprehensionCheck; }
}
