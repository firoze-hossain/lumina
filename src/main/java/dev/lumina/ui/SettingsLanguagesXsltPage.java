package dev.lumina.ui;

import dev.lumina.xslt.XsltSettings;
import dev.lumina.xslt.XsltSettingsManager;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > XSLT.
 * Matches reference screenshot media_1791604044717_ba9f771b.png:
 *  - [x] Show associated files in project view
 */
public class SettingsLanguagesXsltPage extends VBox {

    private final XsltSettingsManager manager = XsltSettingsManager.getInstance();

    private final CheckBox showAssociatedFilesCheck = new CheckBox("Show associated files in project view");

    private XsltSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesXsltPage() {
        setSpacing(12);
        setPadding(new Insets(16, 24, 24, 24));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        showAssociatedFilesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        showAssociatedFilesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        getChildren().add(showAssociatedFilesCheck);
    }

    public void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(XsltSettings s) {
        if (s == null) return;
        showAssociatedFilesCheck.setSelected(s.isShowAssociatedFilesInProjectView());
    }

    public XsltSettings getCurrentSettingsFromUI() {
        XsltSettings s = new XsltSettings();
        s.setShowAssociatedFilesInProjectView(showAssociatedFilesCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        XsltSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public CheckBox getShowAssociatedFilesCheck() {
        return showAssociatedFilesCheck;
    }
}
