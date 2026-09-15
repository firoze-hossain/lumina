package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The far-left vertical tool-window bar, matching IntelliJ's own: a top
 * group (Project / Commit / Pull Requests / Structure / More) and a bottom
 * group (Run / Build / GitHub Copilot MCP Log / Services / Terminal /
 * Problems / Git), each a single-select toggle strip. Which panel actually
 * opens/closes/switches on a click is decided by the caller (LuminaApp) \u2014
 * this class only reports the click and exposes
 * {@link #selectTop}/{@link #selectBottom} so the caller can keep the
 * highlighted icon in sync with whatever panel is really showing.
 */
public final class IconRail extends VBox {

    private final List<ToggleButton> topButtons = new ArrayList<>();
    private final List<ToggleButton> bottomButtons = new ArrayList<>();
    private final ToggleGroup topGroup = new ToggleGroup();
    private final ToggleGroup bottomGroup = new ToggleGroup();

    public IconRail(IntConsumer onTopSelect, Runnable onMore, IntConsumer onBottomSelect) {
        getStyleClass().add("icon-rail");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(4);
        setPadding(new Insets(10, 4, 10, 4));

        addTop(folderIcon(), "Project", 0, onTopSelect);
        addTop(commitIcon(), "Commit", 1, onTopSelect);
        addTop(pullRequestIcon(), "Pull Requests", 2, onTopSelect);
        addTop(structureIcon(), "Structure", 3, onTopSelect);

        Button more = new Button();
        more.setGraphic(dotsIcon());
        more.getStyleClass().add("rail-button");
        more.setTooltip(new Tooltip("More Tool Windows"));
        more.setOnAction(e -> onMore.run());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        addBottom(runRailIcon(), "Run", 0, onBottomSelect);
        addBottom(buildIcon(), "Build", 1, onBottomSelect);
        addBottom(mcpIcon(), "GitHub Copilot MCP Log", 2, onBottomSelect);
        addBottom(servicesRailIcon(), "Services", 3, onBottomSelect);
        addBottom(terminalIcon(), "Terminal", 4, onBottomSelect);
        addBottom(problemsIcon(), "Problems", 5, onBottomSelect);
        addBottom(gitIcon(), "Git", 6, onBottomSelect);

        getChildren().addAll(topButtons.get(0), topButtons.get(1), topButtons.get(2),
                topButtons.get(3), more, spacer,
                bottomButtons.get(0), bottomButtons.get(1), bottomButtons.get(2),
                bottomButtons.get(3), bottomButtons.get(4), bottomButtons.get(5),
                bottomButtons.get(6));
    }

    private void addTop(Node icon, String tip, int index, IntConsumer onSelect) {
        ToggleButton b = toggle(icon, tip, topGroup);
        b.setOnAction(e -> onSelect.accept(index));
        topButtons.add(b);
    }

    private void addBottom(Node icon, String tip, int index, IntConsumer onSelect) {
        ToggleButton b = toggle(icon, tip, bottomGroup);
        b.setOnAction(e -> onSelect.accept(index));
        bottomButtons.add(b);
    }

    private ToggleButton toggle(Node icon, String tip, ToggleGroup group) {
        ToggleButton b = new ToggleButton();
        b.setGraphic(icon);
        b.setToggleGroup(group);
        b.getStyleClass().add("rail-button");
        b.setTooltip(new Tooltip(tip));
        return b;
    }

    /** Highlights top icon {@code index} (0=Project..3=Structure) without firing onTopSelect. */
    public void selectTop(int index) {
        if (index >= 0 && index < topButtons.size()) topButtons.get(index).setSelected(true);
    }

    public void clearTopSelection() {
        topGroup.selectToggle(null);
    }

    /** Highlights bottom icon {@code index} (0=Run,1=Build,2=MCP Log,
     *  3=Services,4=Terminal,5=Problems,6=Git) without firing onBottomSelect. */
    public void selectBottom(int index) {
        if (index >= 0 && index < bottomButtons.size()) bottomButtons.get(index).setSelected(true);
    }

    public void clearBottomSelection() {
        bottomGroup.selectToggle(null);
    }

    // ------------------------------------------------------------- icons
    // Hand-drawn with shapes rather than emoji: glyphs outside the Basic
    // Multilingual Plane render as a blank box without a color-emoji font.

    private static Node sized(Node n) {
        StackPane pane = new StackPane(n);
        pane.setPrefSize(16, 16);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }

    private static final String INK = "#9BA1B5";

    private static Node folderIcon() {
        Polygon folder = new Polygon(
                0, 2,   4, 2,   5.5, 0,   14, 0,   14, 2,
                14, 11, 0, 11);
        folder.setFill(Color.web(INK));
        return sized(folder);
    }

    private static Node commitIcon() {
        Circle dot = new Circle(3);
        dot.setFill(Color.TRANSPARENT);
        dot.setStroke(Color.web(INK));
        dot.setStrokeWidth(1.4);
        Line left = new Line(-8, 0, -3, 0);
        Line right = new Line(3, 0, 8, 0);
        left.setStroke(Color.web(INK));
        right.setStroke(Color.web(INK));
        left.setStrokeWidth(1.4);
        right.setStrokeWidth(1.4);
        return sized(new StackPane(left, right, dot));
    }

    private static Node pullRequestIcon() {
        Circle top = new Circle(2.2);
        top.setFill(Color.web(INK));
        top.setTranslateX(-4);
        top.setTranslateY(-5);
        Circle bottom = new Circle(2.2);
        bottom.setFill(Color.web(INK));
        bottom.setTranslateX(-4);
        bottom.setTranslateY(5);
        Circle right = new Circle(2.2);
        right.setFill(Color.web(INK));
        right.setTranslateX(4);
        right.setTranslateY(-5);
        Line stem = new Line(-4, -3, -4, 3);
        stem.setStroke(Color.web(INK));
        stem.setStrokeWidth(1.4);
        Line arm = new Line(-4, -5, 4, -5);
        arm.setStroke(Color.web(INK));
        arm.setStrokeWidth(1.4);
        Line arrow = new Line(4, -3, 4, -5);
        arrow.setStroke(Color.web(INK));
        arrow.setStrokeWidth(1.4);
        return sized(new StackPane(stem, arm, arrow, top, bottom, right));
    }

    private static Node structureIcon() {
        VBox bars = new VBox(2.4);
        bars.setAlignment(Pos.CENTER_LEFT);
        for (double w : new double[]{14, 10, 12}) {
            Rectangle r = new Rectangle(w, 2);
            r.setFill(Color.web(INK));
            bars.getChildren().add(r);
        }
        return sized(bars);
    }

    private static Node dotsIcon() {
        HBox dots = new HBox(2.5);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Circle c = new Circle(1.4);
            c.setFill(Color.web(INK));
            dots.getChildren().add(c);
        }
        return sized(dots);
    }

    private static Node runRailIcon() {
        Polygon triangle = new Polygon(-3.5, -6, -3.5, 6, 5.5, 0);
        triangle.setFill(Color.web("#6FCF7A"));
        return sized(triangle);
    }

    private static Node buildIcon() {
        Rectangle handle = new Rectangle(2, 11);
        handle.setFill(Color.web(INK));
        handle.setArcWidth(1);
        handle.setArcHeight(1);
        handle.setRotate(45);
        Rectangle head = new Rectangle(3, 6);
        head.setFill(Color.web(INK));
        head.setRotate(45);
        head.setTranslateX(-4.2);
        head.setTranslateY(-4.2);
        return sized(new StackPane(handle, head));
    }

    private static Node mcpIcon() {
        HBox sliders = new HBox(3.5);
        sliders.setAlignment(Pos.CENTER);
        double[] knobOffsets = {-2, 2, 0};
        for (double offset : knobOffsets) {
            Line line = new Line(0, -6, 0, 6);
            line.setStroke(Color.web(INK));
            line.setStrokeWidth(1.2);
            Circle knob = new Circle(1.5);
            knob.setFill(Color.web(INK));
            knob.setTranslateY(offset);
            StackPane column = new StackPane(line, knob);
            column.setPrefWidth(3);
            sliders.getChildren().add(column);
        }
        return sized(sliders);
    }

    private static Node servicesRailIcon() {
        Circle outer = new Circle(6.5);
        outer.setFill(Color.TRANSPARENT);
        outer.setStroke(Color.web(INK));
        outer.setStrokeWidth(1.3);
        Polygon play = new Polygon(-2, -3, -2, 3, 3, 0);
        play.setFill(Color.web(INK));
        return sized(new StackPane(outer, play));
    }

    private static Node terminalIcon() {
        Rectangle box = new Rectangle(15, 12);
        box.setArcWidth(2);
        box.setArcHeight(2);
        box.setFill(Color.TRANSPARENT);
        box.setStroke(Color.web(INK));
        box.setStrokeWidth(1.2);
        Line chevronA = new Line(-4.5, -1.5, -1.5, 1);
        Line chevronB = new Line(-4.5, 3.5, -1.5, 1);
        Line cursor = new Line(0.5, 3.5, 4, 3.5);
        for (Line l : List.of(chevronA, chevronB, cursor)) {
            l.setStroke(Color.web(INK));
            l.setStrokeWidth(1.3);
        }
        return sized(new StackPane(box, chevronA, chevronB, cursor));
    }

    private static Node problemsIcon() {
        Circle circle = new Circle(6.5);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(INK));
        circle.setStrokeWidth(1.3);
        Label bang = new Label("!");
        bang.setStyle("-fx-text-fill: " + INK + "; -fx-font-size: 9px; -fx-font-weight: bold;");
        return sized(new StackPane(circle, bang));
    }

    private static Node gitIcon() {
        Circle top = new Circle(2);
        top.setFill(Color.TRANSPARENT);
        top.setStroke(Color.web(INK));
        top.setStrokeWidth(1.3);
        top.setTranslateX(3.5);
        top.setTranslateY(-5);
        Circle bottomLeft = new Circle(2);
        bottomLeft.setFill(Color.TRANSPARENT);
        bottomLeft.setStroke(Color.web(INK));
        bottomLeft.setStrokeWidth(1.3);
        bottomLeft.setTranslateX(-3.5);
        bottomLeft.setTranslateY(5);
        Circle bottomRight = new Circle(2);
        bottomRight.setFill(Color.TRANSPARENT);
        bottomRight.setStroke(Color.web(INK));
        bottomRight.setStrokeWidth(1.3);
        bottomRight.setTranslateX(3.5);
        bottomRight.setTranslateY(5);
        Line trunk = new Line(3.5, -3, 3.5, 3);
        Line branch = new Line(3.5, 0, -3.5, 3);
        for (Line l : List.of(trunk, branch)) {
            l.setStroke(Color.web(INK));
            l.setStrokeWidth(1.2);
        }
        return sized(new StackPane(trunk, branch, top, bottomLeft, bottomRight));
    }
}