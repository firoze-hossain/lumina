package dev.lumina.ui;

import dev.lumina.rbs.RbsSettings;
import dev.lumina.rbs.RbsSettingsManager;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Languages & Frameworks > RBS settings page in Lumina IDE.
 * Matches Image 2 with RBS collection type support checkbox and external documentation link.
 */
public class SettingsLanguagesRbsPage extends VBox {

    private final RbsSettingsManager manager = RbsSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox improvedTypeSupportCheckBox;
    private Hyperlink rbsCollectionLink;
    private RbsSettings initialSettings;

    public SettingsLanguagesRbsPage() {
        setSpacing(10);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        RbsSettings current = manager.getSettings();

        improvedTypeSupportCheckBox = new CheckBox("Improved type support with RBS collection");
        improvedTypeSupportCheckBox.setSelected(current.isImprovedTypeSupportWithRbsCollection());
        improvedTypeSupportCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        improvedTypeSupportCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        // Description with embedded link
        rbsCollectionLink = new Hyperlink("RBS collection \u2197");
        rbsCollectionLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;");
        rbsCollectionLink.setOnMouseEntered(e -> rbsCollectionLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
        rbsCollectionLink.setOnMouseExited(e -> rbsCollectionLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));

        Text descText = new Text(" is a community-managed collection of type signatures for gems that ship without any. " +
                "The type signatures will be automatically downloaded in the background, improving type support and " +
                "code insight for all gems used within the project. This feature does not modify the project or its files.");
        descText.setStyle("-fx-fill: #8C9099; -fx-font-size: 13px;");

        TextFlow descFlow = new TextFlow(rbsCollectionLink, descText);
        descFlow.setMaxWidth(750);
        descFlow.setPadding(new Insets(0, 0, 0, 22));

        getChildren().addAll(improvedTypeSupportCheckBox, descFlow);
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModified(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void takeSnapshot() {
        this.initialSettings = manager.getSettings();
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return initialSettings.isImprovedTypeSupportWithRbsCollection() != improvedTypeSupportCheckBox.isSelected();
    }

    public void apply() {
        RbsSettings s = new RbsSettings(improvedTypeSupportCheckBox.isSelected());
        manager.setSettings(s);
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        RbsSettings current = manager.getSettings();
        improvedTypeSupportCheckBox.setSelected(current.isImprovedTypeSupportWithRbsCollection());
        takeSnapshot();
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public CheckBox getImprovedTypeSupportCheckBox() {
        return improvedTypeSupportCheckBox;
    }

    public Hyperlink getRbsCollectionLink() {
        return rbsCollectionLink;
    }
}
