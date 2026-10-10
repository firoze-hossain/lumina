package dev.lumina.ui;

import dev.lumina.scala.ScalaLanguageSettingsManager;
import dev.lumina.scala.ScalaProjectViewSettings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Languages & Frameworks > Scala > Project View settings page in Lumina IDE.
 * Matches Image 1:
 *  - [ ] Group package object with package
 *  - [ ] Highlight nodes with errors
 */
public class SettingsLanguagesScalaProjectViewPage extends VBox {

    private final ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox groupPackageObjectCheckBox;
    private CheckBox highlightNodesWithErrorsCheckBox;

    private ScalaProjectViewSettings initialSettings;

    public SettingsLanguagesScalaProjectViewPage() {
        setSpacing(12);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ScalaProjectViewSettings current = manager.getProjectViewSettings();

        groupPackageObjectCheckBox = new CheckBox("Group package object with package");
        groupPackageObjectCheckBox.setSelected(current.isGroupPackageObjectWithPackage());
        styleCheckBox(groupPackageObjectCheckBox);
        groupPackageObjectCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        highlightNodesWithErrorsCheckBox = new CheckBox("Highlight nodes with errors");
        highlightNodesWithErrorsCheckBox.setSelected(current.isHighlightNodesWithErrors());
        styleCheckBox(highlightNodesWithErrorsCheckBox);
        highlightNodesWithErrorsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        getChildren().addAll(
                groupPackageObjectCheckBox,
                highlightNodesWithErrorsCheckBox
        );
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void takeSnapshot() {
        this.initialSettings = getCurrentUiSettings();
    }

    public ScalaProjectViewSettings getCurrentUiSettings() {
        ScalaProjectViewSettings s = new ScalaProjectViewSettings();
        s.setGroupPackageObjectWithPackage(groupPackageObjectCheckBox.isSelected());
        s.setHighlightNodesWithErrors(highlightNodesWithErrorsCheckBox.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentUiSettings());
    }

    public void apply() {
        manager.setProjectViewSettings(getCurrentUiSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ScalaProjectViewSettings saved = manager.getProjectViewSettings();
        applySettingsToUi(saved);
        takeSnapshot();
        fireModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            applySettingsToUi(initialSettings);
            fireModified();
        }
    }

    public void resetDefaults() {
        applySettingsToUi(new ScalaProjectViewSettings());
        fireModified();
    }

    private void applySettingsToUi(ScalaProjectViewSettings s) {
        if (s == null) return;
        groupPackageObjectCheckBox.setSelected(s.isGroupPackageObjectWithPackage());
        highlightNodesWithErrorsCheckBox.setSelected(s.isHighlightNodesWithErrors());
    }

    // Direct UI accessors for tests
    public CheckBox getGroupPackageObjectCheckBox() {
        return groupPackageObjectCheckBox;
    }

    public CheckBox getHighlightNodesWithErrorsCheckBox() {
        return highlightNodesWithErrorsCheckBox;
    }
}
