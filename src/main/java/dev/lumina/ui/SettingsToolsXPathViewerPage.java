package dev.lumina.ui;

import dev.lumina.tools.XPathViewerSettings;
import dev.lumina.tools.XPathViewerSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Settings UI page for Tools > XPath Viewer in Lumina IDE.
 * 1:1 visual match with reference screenshot layout.
 */
public class SettingsToolsXPathViewerPage extends VBox {

    private final CheckBox scrollFirstHitCheck;
    private final CheckBox useNodeAtCursorCheck;
    private final CheckBox highlightOnlyStartTagCheck;
    private final CheckBox addErrorStripeMarkersCheck;

    private final ColorPicker highlightColorPicker;
    private final ColorPicker contextNodeColorPicker;

    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsToolsXPathViewerPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Section: Settings
        HBox settingsHeader = createSectionHeader("Settings");

        scrollFirstHitCheck = new CheckBox("Scroll first hit into visible area");
        styleCheckBox(scrollFirstHitCheck);
        scrollFirstHitCheck.setSelected(true);

        useNodeAtCursorCheck = new CheckBox("Use node at cursor as context node");
        styleCheckBox(useNodeAtCursorCheck);
        useNodeAtCursorCheck.setSelected(true);

        highlightOnlyStartTagCheck = new CheckBox("Highlight only start tag instead of whole tag content");
        styleCheckBox(highlightOnlyStartTagCheck);
        highlightOnlyStartTagCheck.setSelected(true);

        addErrorStripeMarkersCheck = new CheckBox("Add error stripe markers for each result");
        styleCheckBox(addErrorStripeMarkersCheck);
        addErrorStripeMarkersCheck.setSelected(true);

        VBox checksBox = new VBox(8,
                scrollFirstHitCheck,
                useNodeAtCursorCheck,
                highlightOnlyStartTagCheck,
                addErrorStripeMarkersCheck
        );
        checksBox.setPadding(new Insets(0, 0, 0, 16));

        // Section: Colors
        HBox colorsHeader = createSectionHeader("Colors");

        Label highlightLabel = new Label("Highlight color:");
        highlightLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        highlightColorPicker = createStyledColorPicker("#FFD578");

        HBox highlightRow = new HBox(12, highlightLabel, highlightColorPicker);
        highlightRow.setAlignment(Pos.CENTER_LEFT);
        highlightRow.setPadding(new Insets(0, 0, 0, 16));

        Label contextNodeLabel = new Label("Context node color:");
        contextNodeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        contextNodeColorPicker = createStyledColorPicker("#C2FFD4");

        HBox contextNodeRow = new HBox(12, contextNodeLabel, contextNodeColorPicker);
        contextNodeRow.setAlignment(Pos.CENTER_LEFT);
        contextNodeRow.setPadding(new Insets(0, 0, 0, 16));

        getChildren().addAll(
                settingsHeader,
                checksBox,
                colorsHeader,
                highlightRow,
                contextNodeRow
        );

        setupListeners();
        loadSettings();
    }

    private ColorPicker createStyledColorPicker(String defaultHex) {
        ColorPicker picker = new ColorPicker(Color.web(defaultHex));
        picker.setStyle("-fx-color-label-visible: true; -fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        return picker;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private HBox createSectionHeader(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox box = new HBox(12, titleLabel, line);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 0, 4, 0));
        return box;
    }

    private void setupListeners() {
        scrollFirstHitCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        useNodeAtCursorCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        highlightOnlyStartTagCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        addErrorStripeMarkersCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        highlightColorPicker.valueProperty().addListener((o, ov, nv) -> notifyModified());
        contextNodeColorPicker.valueProperty().addListener((o, ov, nv) -> notifyModified());
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            XPathViewerSettings s = XPathViewerSettingsManager.getInstance().getSettings();
            scrollFirstHitCheck.setSelected(s.isScrollFirstHitIntoVisibleArea());
            useNodeAtCursorCheck.setSelected(s.isUseNodeAtCursorAsContextNode());
            highlightOnlyStartTagCheck.setSelected(s.isHighlightOnlyStartTag());
            addErrorStripeMarkersCheck.setSelected(s.isAddErrorStripeMarkers());
            try {
                highlightColorPicker.setValue(Color.web(s.getHighlightColor()));
            } catch (Exception ignored) {
                highlightColorPicker.setValue(Color.web("#FFD578"));
            }
            try {
                contextNodeColorPicker.setValue(Color.web(s.getContextNodeColor()));
            } catch (Exception ignored) {
                contextNodeColorPicker.setValue(Color.web("#C2FFD4"));
            }
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        XPathViewerSettings saved = XPathViewerSettingsManager.getInstance().getSettings();
        if (scrollFirstHitCheck.isSelected() != saved.isScrollFirstHitIntoVisibleArea()) return true;
        if (useNodeAtCursorCheck.isSelected() != saved.isUseNodeAtCursorAsContextNode()) return true;
        if (highlightOnlyStartTagCheck.isSelected() != saved.isHighlightOnlyStartTag()) return true;
        if (addErrorStripeMarkersCheck.isSelected() != saved.isAddErrorStripeMarkers()) return true;
        if (!Objects.equals(toHex(highlightColorPicker.getValue()), saved.getHighlightColor().toUpperCase())) return true;
        return !Objects.equals(toHex(contextNodeColorPicker.getValue()), saved.getContextNodeColor().toUpperCase());
    }

    public void apply() {
        XPathViewerSettings s = new XPathViewerSettings();
        s.setScrollFirstHitIntoVisibleArea(scrollFirstHitCheck.isSelected());
        s.setUseNodeAtCursorAsContextNode(useNodeAtCursorCheck.isSelected());
        s.setHighlightOnlyStartTag(highlightOnlyStartTagCheck.isSelected());
        s.setAddErrorStripeMarkers(addErrorStripeMarkersCheck.isSelected());
        s.setHighlightColor(toHex(highlightColorPicker.getValue()));
        s.setContextNodeColor(toHex(contextNodeColorPicker.getValue()));

        XPathViewerSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    private String toHex(Color c) {
        if (c == null) return "#FFD578";
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    public CheckBox getScrollFirstHitCheck() {
        return scrollFirstHitCheck;
    }

    public ColorPicker getHighlightColorPicker() {
        return highlightColorPicker;
    }
}
