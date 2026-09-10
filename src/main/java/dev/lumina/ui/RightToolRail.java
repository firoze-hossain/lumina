package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Vertical right-hand tool-window selector, modelled after the IntelliJ new
 * UI: a slim, always-visible icon strip flush against the window's right
 * edge. It only reports clicks via {@code onSelect} -- whether that opens,
 * closes, or switches the docked tool window is decided by the caller, which
 * then calls {@link #select} or {@link #clearSelection} to keep this rail's
 * highlighted icon in sync with what's actually showing.
 *
 * <p>Icons are hand-drawn with JavaFX shapes rather than color emoji: glyphs
 * outside the Basic Multilingual Plane (bells, speech bubbles, and the like)
 * render as an empty box on systems with no color-emoji font, which is the
 * same "missing icon" problem the project-tree file icons had.
 */
public final class RightToolRail extends VBox {

    private final List<ToggleButton> buttons = new ArrayList<>();
    private final ToggleGroup group = new ToggleGroup();

    public RightToolRail(IntConsumer onSelect) {
        getStyleClass().add("right-tool-rail");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(8);
        setPadding(new Insets(10, 4, 10, 4));
        add(bellIcon(), "Notifications", 0, onSelect);
        add(chatIcon(), "AI Chat", 1, onSelect);
        add(databaseIcon(), "Database", 2, onSelect);
        add(letterIcon("m"), "Maven", 3, onSelect);
        add(gearIcon(), "Services", 4, onSelect);
        add(letterIcon("gh"), "GitHub Copilot", 5, onSelect);
    }

    private void add(Node icon, String tooltip, int index, IntConsumer onSelect) {
        ToggleButton button = new ToggleButton();
        button.setGraphic(icon);
        button.setToggleGroup(group);
        button.getStyleClass().add("right-rail-button");
        button.setTooltip(new Tooltip(tooltip));
        // The real deselect (when the caller decides to hide the panel)
        // always goes through clearSelection() below, driven by the
        // click -> onSelect -> LuminaApp decision, rather than JavaFX's
        // own toggle-group behavior.
        button.setOnAction(e -> onSelect.accept(index));
        buttons.add(button);
        getChildren().add(button);
    }

    /** Highlights the icon for {@code index} without firing onSelect. */
    public void select(int index) {
        if (index >= 0 && index < buttons.size()) {
            buttons.get(index).setSelected(true);
        }
    }

    /** Un-highlights every icon (the docked tool window is closed). */
    public void clearSelection() {
        group.selectToggle(null);
    }

    // ------------------------------------------------------------- icons

    private static Node icon(Node... shapes) {
        StackPane pane = new StackPane(shapes);
        pane.setPrefSize(16, 16);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }

    private static Node letterIcon(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #B9BECF; -fx-font-size: 12px; -fx-font-weight: bold;");
        return icon(label);
    }

    private static Node bellIcon() {
        Polygon bell = new Polygon(
                6, 0,   10, 0,
                12, 9,  4, 9);
        bell.setFill(Color.web("#B9BECF"));
        Rectangle clapper = new Rectangle(6.5, 9.5, 3, 2);
        clapper.setArcWidth(2);
        clapper.setArcHeight(2);
        clapper.setFill(Color.web("#B9BECF"));
        return icon(bell, clapper);
    }

    private static Node chatIcon() {
        Rectangle bubble = new Rectangle(0, 0, 14, 10);
        bubble.setArcWidth(4);
        bubble.setArcHeight(4);
        bubble.setFill(Color.TRANSPARENT);
        bubble.setStroke(Color.web("#B9BECF"));
        bubble.setStrokeWidth(1.3);
        Polygon tail = new Polygon(3, 10, 3, 14, 7, 10);
        tail.setFill(Color.web("#B9BECF"));
        StackPane pane = new StackPane(bubble, tail);
        pane.setPrefSize(16, 16);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        StackPane.setAlignment(bubble, Pos.TOP_CENTER);
        StackPane.setAlignment(tail, Pos.BOTTOM_LEFT);
        return pane;
    }

    private static Node databaseIcon() {
        Ellipse top = new Ellipse(6, 2.4);
        top.setFill(Color.TRANSPARENT);
        top.setStroke(Color.web("#B9BECF"));
        top.setStrokeWidth(1.2);
        Ellipse bottom = new Ellipse(6, 2.4);
        bottom.setFill(Color.TRANSPARENT);
        bottom.setStroke(Color.web("#B9BECF"));
        bottom.setStrokeWidth(1.2);
        bottom.setTranslateY(8.5);
        javafx.scene.shape.Line left = new javafx.scene.shape.Line(0, 2.4, 0, 10.9);
        left.setStroke(Color.web("#B9BECF"));
        left.setStrokeWidth(1.2);
        javafx.scene.shape.Line right = new javafx.scene.shape.Line(12, 2.4, 12, 10.9);
        right.setStroke(Color.web("#B9BECF"));
        right.setStrokeWidth(1.2);
        StackPane pane = new StackPane(left, right, top, bottom);
        pane.setPrefSize(16, 16);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }

    private static Node gearIcon() {
        Label label = new Label("\u2699");
        label.setStyle("-fx-text-fill: #B9BECF; -fx-font-size: 15px;");
        return icon(label);
    }
}