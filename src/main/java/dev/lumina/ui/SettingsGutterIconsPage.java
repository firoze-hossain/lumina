package dev.lumina.ui;

import dev.lumina.settings.GutterIconsSettings;
import dev.lumina.settings.GutterIconsSettings.GutterIconDescriptor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;

/**
 * Pixel-perfect settings page for Editor > General > Gutter Icons matching IntelliJ IDEA.
 * Features:
 * - Master "Show gutter icons" toggle with list enabling/disabling
 * - Centered divider line category headers matching IntelliJ
 * - Vector graphic gutter icons aligned in front of each checkbox
 * - Dynamic registry driven by GutterIconsSettings without hardcoding
 * - Real-time dirty detection, apply, and reset support
 */
public class SettingsGutterIconsPage extends VBox {

    private final CheckBox showGutterIcons;
    private final Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();
    private final Map<String, HBox> rowBoxes = new LinkedHashMap<>();
    private HBox selectedRow = null;

    private Runnable onModifiedListener;

    public SettingsGutterIconsPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        GutterIconsSettings settings = GutterIconsSettings.getInstance();

        // Master toggle
        showGutterIcons = new CheckBox("Show gutter icons");
        showGutterIcons.setSelected(settings.isShowGutterIcons());
        showGutterIcons.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        // List container
        VBox listContainer = new VBox(4);
        listContainer.disableProperty().bind(showGutterIcons.selectedProperty().not());
        listContainer.opacityProperty().bind(
                javafx.beans.binding.Bindings.when(showGutterIcons.selectedProperty())
                        .then(1.0)
                        .otherwise(0.5)
        );

        Map<String, List<GutterIconDescriptor>> categorized = settings.getDescriptorsByCategory();

        for (Map.Entry<String, List<GutterIconDescriptor>> entry : categorized.entrySet()) {
            String category = entry.getKey();
            List<GutterIconDescriptor> descriptors = entry.getValue();

            // Centered divider header
            HBox header = createCategoryHeader(category);
            listContainer.getChildren().add(header);

            // Item rows
            for (GutterIconDescriptor d : descriptors) {
                HBox row = createItemRow(d, settings.isIconConfiguredEnabled(d.id()));
                listContainer.getChildren().add(row);
                rowBoxes.put(d.id(), row);
            }
        }

        showGutterIcons.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        getChildren().addAll(showGutterIcons, listContainer);
    }

    private HBox createCategoryHeader(String category) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(12, 0, 6, 0));

        Region leftLine = new Region();
        HBox.setHgrow(leftLine, Priority.ALWAYS);
        leftLine.setStyle("-fx-border-color: #43454A transparent transparent transparent; -fx-border-width: 1 0 0 0; -fx-max-height: 1px;");

        Label label = new Label(category);
        label.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 10 0 10;");

        Region rightLine = new Region();
        HBox.setHgrow(rightLine, Priority.ALWAYS);
        rightLine.setStyle("-fx-border-color: #43454A transparent transparent transparent; -fx-border-width: 1 0 0 0; -fx-max-height: 1px;");

        header.getChildren().addAll(leftLine, label, rightLine);
        return header;
    }

    private HBox createItemRow(GutterIconDescriptor d, boolean initiallySelected) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 8, 3, 8));
        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 4; -fx-cursor: hand;");

        // 16x16 fixed space for icon
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(16, 16);
        iconBox.setPrefSize(16, 16);
        iconBox.setMaxSize(16, 16);
        iconBox.setAlignment(Pos.CENTER);

        Node iconNode = createIcon(d.iconType());
        if (iconNode != null) {
            iconBox.getChildren().add(iconNode);
        }

        CheckBox cb = new CheckBox(d.name());
        cb.setSelected(initiallySelected);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, oldV, newV) -> fireModified());
        checkBoxes.put(d.id(), cb);

        row.getChildren().addAll(iconBox, cb);

        // Row hover & selection highlight matching IntelliJ
        row.setOnMouseEntered(e -> {
            if (row != selectedRow) {
                row.setStyle("-fx-background-color: #2B2D30; -fx-background-radius: 4; -fx-cursor: hand;");
            }
        });
        row.setOnMouseExited(e -> {
            if (row != selectedRow) {
                row.setStyle("-fx-background-color: transparent; -fx-background-radius: 4; -fx-cursor: hand;");
            }
        });
        row.setOnMouseClicked(e -> {
            if (e.getTarget() != cb) {
                cb.setSelected(!cb.isSelected());
            }
            selectRow(row);
        });

        return row;
    }

    private void selectRow(HBox row) {
        if (selectedRow != null && selectedRow != row) {
            selectedRow.setStyle("-fx-background-color: transparent; -fx-background-radius: 4; -fx-cursor: hand;");
        }
        selectedRow = row;
        if (selectedRow != null) {
            selectedRow.setStyle("-fx-background-color: #2E436E; -fx-background-radius: 4; -fx-cursor: hand;");
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public boolean isModified() {
        GutterIconsSettings s = GutterIconsSettings.getInstance();
        if (showGutterIcons.isSelected() != s.isShowGutterIcons()) {
            return true;
        }
        for (Map.Entry<String, CheckBox> e : checkBoxes.entrySet()) {
            if (e.getValue().isSelected() != s.isIconConfiguredEnabled(e.getKey())) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        GutterIconsSettings s = GutterIconsSettings.getInstance();
        s.setShowGutterIcons(showGutterIcons.isSelected());
        for (Map.Entry<String, CheckBox> e : checkBoxes.entrySet()) {
            s.setIconEnabled(e.getKey(), e.getValue().isSelected());
        }
        s.save();
        fireModified();
    }

    public void reset() {
        GutterIconsSettings s = GutterIconsSettings.getInstance();
        showGutterIcons.setSelected(s.isShowGutterIcons());
        for (Map.Entry<String, CheckBox> e : checkBoxes.entrySet()) {
            e.getValue().setSelected(s.isIconConfiguredEnabled(e.getKey()));
        }
        fireModified();
    }

    public CheckBox getShowGutterIconsCheckBox() {
        return showGutterIcons;
    }

    public CheckBox getItemCheckBox(String id) {
        return checkBoxes.get(id);
    }

    // ---------------------------------------------------- High-Fidelity Vector Icons

    private static Node createIcon(String type) {
        if (type == null || "NONE".equals(type)) return null;

        StackPane pane = new StackPane();
        pane.setPrefSize(14, 14);
        pane.setMinSize(14, 14);
        pane.setMaxSize(14, 14);
        pane.setAlignment(Pos.CENTER);

        switch (type) {
            case "AOP" -> {
                Circle circle = new Circle(6.0, Color.web("#CC342D"));
                Text text = new Text("M");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.5));
                text.setFill(Color.WHITE);
                pane.getChildren().addAll(circle, text);
            }
            case "COLOR_PREVIEW" -> {
                GridPane grid = new GridPane();
                grid.setAlignment(Pos.CENTER);
                grid.setHgap(1.5);
                grid.setVgap(1.5);
                grid.add(new Rectangle(4.5, 4.5, Color.web("#E06C75")), 0, 0); // red
                grid.add(new Rectangle(4.5, 4.5, Color.web("#98C379")), 1, 0); // green
                grid.add(new Rectangle(4.5, 4.5, Color.web("#61AFEF")), 0, 1); // blue
                grid.add(new Rectangle(4.5, 4.5, Color.web("#E5C07B")), 1, 1); // yellow
                pane.getChildren().add(grid);
            }
            case "DOC_COMMENTS" -> {
                Rectangle doc = new Rectangle(10, 12, Color.TRANSPARENT);
                doc.setStroke(Color.web("#59A869"));
                doc.setStrokeWidth(1.0);
                doc.setArcWidth(2);
                doc.setArcHeight(2);
                Line l1 = new Line(2, 4, 8, 4); l1.setStroke(Color.web("#59A869")); l1.setStrokeWidth(0.9);
                Line l2 = new Line(2, 7, 8, 7); l2.setStroke(Color.web("#59A869")); l2.setStrokeWidth(0.9);
                Line l3 = new Line(2, 10, 6, 10); l3.setStroke(Color.web("#59A869")); l3.setStrokeWidth(0.9);
                pane.getChildren().addAll(doc, l1, l2, l3);
            }
            case "RUN_MARKER", "PLAY" -> {
                Polygon triangle = new Polygon(0.0, 0.0, 0.0, 10.0, 8.5, 5.0);
                triangle.setFill(Color.web("#59A869"));
                pane.getChildren().add(triangle);
            }
            case "RECURSIVE_CALL" -> {
                SVGPath path = new SVGPath();
                path.setContent("M 6 2 A 4.5 4.5 0 1 1 2 6.5 L 0 5 M 2 6.5 L 4 5");
                path.setStroke(Color.web("#589DF6"));
                path.setStrokeWidth(1.3);
                path.setFill(Color.TRANSPARENT);
                pane.getChildren().add(path);
            }
            case "BOX_CONTAINER" -> {
                SVGPath box = new SVGPath();
                box.setContent("M 1 4 L 7 1 L 13 4 L 13 11 L 7 14 L 1 11 Z M 1 4 L 7 7 L 13 4 M 7 7 L 7 14");
                box.setStroke(Color.web("#3574F0"));
                box.setStrokeWidth(1.0);
                box.setFill(Color.TRANSPARENT);
                pane.getChildren().add(box);
            }
            case "DATABASE_TABLE", "DATABASE_PACKAGE", "DATABASE_CYLINDER", "JPA_CONFIG", "JPA_MAPPINGS" -> {
                SVGPath db = new SVGPath();
                db.setContent("M 2 3 C 2 1.5 12 1.5 12 3 C 12 4.5 2 4.5 2 3 M 2 3 L 2 11 C 2 12.5 12 12.5 12 11 L 12 3 M 2 7 C 2 8.5 12 8.5 12 7");
                db.setStroke(Color.web("#389FD6"));
                db.setStrokeWidth(1.1);
                db.setFill(Color.TRANSPARENT);
                pane.getChildren().add(db);
            }
            case "GOTO_IMPLEMENTATIONS", "IMPLEMENTING_METHOD" -> {
                Circle circle = new Circle(5.0, Color.TRANSPARENT);
                circle.setStroke(Color.web("#59A869"));
                circle.setStrokeWidth(1.2);
                Text text = new Text("I");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.5));
                text.setFill(Color.web("#59A869"));
                SVGPath arrow = new SVGPath();
                arrow.setContent("M 0 3 L 2 0 L 4 3 M 2 0 L 2 4");
                arrow.setStroke(Color.web("#59A869"));
                arrow.setStrokeWidth(1.2);
                arrow.setFill(Color.TRANSPARENT);
                arrow.setTranslateX(4.5);
                arrow.setTranslateY(-4.0);
                pane.getChildren().addAll(circle, text, arrow);
            }
            case "GOTO_INTERFACES", "IMPLEMENTED_METHOD" -> {
                Circle circle = new Circle(5.0, Color.TRANSPARENT);
                circle.setStroke(Color.web("#59A869"));
                circle.setStrokeWidth(1.2);
                Text text = new Text("I");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.5));
                text.setFill(Color.web("#59A869"));
                SVGPath arrow = new SVGPath();
                arrow.setContent("M 0 1 L 2 4 L 4 1 M 2 4 L 2 0");
                arrow.setStroke(Color.web("#59A869"));
                arrow.setStrokeWidth(1.2);
                arrow.setFill(Color.TRANSPARENT);
                arrow.setTranslateX(4.5);
                arrow.setTranslateY(4.0);
                pane.getChildren().addAll(circle, text, arrow);
            }
            case "EXCHANGE_ARROWS", "ARROW_RIGHT", "DIFF_ARROWS" -> {
                SVGPath arrows = new SVGPath();
                arrows.setContent("M 1 4 L 11 4 M 8 1 L 11 4 L 8 7 M 11 9 L 1 9 M 4 6 L 1 9 L 4 12");
                arrows.setStroke(Color.web("#59A869"));
                arrows.setStrokeWidth(1.2);
                arrows.setFill(Color.TRANSPARENT);
                pane.getChildren().add(arrows);
            }
            case "HTTP_CLIENT" -> {
                Rectangle rect = new Rectangle(12, 10, Color.TRANSPARENT);
                rect.setStroke(Color.web("#389FD6"));
                rect.setStrokeWidth(1.1);
                rect.setArcWidth(2);
                rect.setArcHeight(2);
                Line l = new Line(2, 6, 6, 6);
                l.setStroke(Color.web("#59A869"));
                l.setStrokeWidth(1.2);
                pane.getChildren().addAll(rect, l);
            }
            case "KEY_CERTIFICATE" -> {
                SVGPath key = new SVGPath();
                key.setContent("M 4 8 A 3 3 0 1 1 8 4 L 12 8 L 10 10 L 9 9 L 7 11 Z");
                key.setStroke(Color.web("#E5C07B"));
                key.setStrokeWidth(1.1);
                key.setFill(Color.TRANSPARENT);
                pane.getChildren().add(key);
            }
            case "EE_BADGE" -> {
                Rectangle badge = new Rectangle(14, 10, Color.web("#2B5B84"));
                badge.setArcWidth(3); badge.setArcHeight(3);
                Text text = new Text("EE");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.5));
                text.setFill(Color.WHITE);
                pane.getChildren().addAll(badge, text);
            }
            case "INJECTION_POINT", "DISPOSER" -> {
                Circle circle = new Circle(5.5, Color.TRANSPARENT);
                circle.setStroke(Color.web("#E06C75"));
                circle.setStrokeWidth(1.2);
                Line slash = new Line(-3.5, 3.5, 3.5, -3.5);
                slash.setStroke(Color.web("#E06C75"));
                slash.setStrokeWidth(1.2);
                pane.getChildren().addAll(circle, slash);
            }
            case "GLOBE_LINK", "GLOBE" -> {
                Circle globe = new Circle(5.5, Color.TRANSPARENT);
                globe.setStroke(Color.web("#389FD6"));
                globe.setStrokeWidth(1.1);
                Line h = new Line(-5, 0, 5, 0); h.setStroke(Color.web("#389FD6")); h.setStrokeWidth(0.9);
                pane.getChildren().addAll(globe, h);
            }
            case "AT_SIGN" -> {
                Text at = new Text("@");
                at.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                at.setFill(Color.web("#80848B"));
                pane.getChildren().add(at);
            }
            case "LAMBDA" -> {
                Circle circle = new Circle(6.0, Color.web("#CC342D"));
                Text lambda = new Text("\u03BB");
                lambda.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
                lambda.setFill(Color.WHITE);
                pane.getChildren().addAll(circle, lambda);
            }
            case "OVERRIDDEN_METHOD" -> {
                Circle circle = new Circle(5.0, Color.TRANSPARENT);
                circle.setStroke(Color.web("#589DF6"));
                circle.setStrokeWidth(1.2);
                Text text = new Text("O");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.0));
                text.setFill(Color.web("#589DF6"));
                SVGPath arrow = new SVGPath();
                arrow.setContent("M 0 1 L 2 4 L 4 1 M 2 4 L 2 0");
                arrow.setStroke(Color.web("#589DF6"));
                arrow.setStrokeWidth(1.2);
                arrow.setFill(Color.TRANSPARENT);
                arrow.setTranslateX(4.5);
                arrow.setTranslateY(4.0);
                pane.getChildren().addAll(circle, text, arrow);
            }
            case "OVERRIDING_METHOD" -> {
                Circle circle = new Circle(5.0, Color.TRANSPARENT);
                circle.setStroke(Color.web("#589DF6"));
                circle.setStrokeWidth(1.2);
                Text text = new Text("O");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.0));
                text.setFill(Color.web("#589DF6"));
                SVGPath arrow = new SVGPath();
                arrow.setContent("M 0 3 L 2 0 L 4 3 M 2 0 L 2 4");
                arrow.setStroke(Color.web("#589DF6"));
                arrow.setStrokeWidth(1.2);
                arrow.setFill(Color.TRANSPARENT);
                arrow.setTranslateX(4.5);
                arrow.setTranslateY(-4.0);
                pane.getChildren().addAll(circle, text, arrow);
            }
            case "SERVICE_STAR" -> {
                SVGPath star = new SVGPath();
                star.setContent("M 6 0 L 7.5 4.5 L 12 6 L 7.5 7.5 L 6 12 L 4.5 7.5 L 0 6 L 4.5 4.5 Z");
                star.setFill(Color.web("#389FD6"));
                pane.getChildren().add(star);
            }
            case "SIBLING_INHERITED" -> {
                Circle circle = new Circle(5.0, Color.TRANSPARENT);
                circle.setStroke(Color.web("#59A869"));
                circle.setStrokeWidth(1.2);
                Text text = new Text("I");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.0));
                text.setFill(Color.web("#59A869"));
                pane.getChildren().addAll(circle, text);
            }
            case "JS_BADGE" -> {
                Rectangle badge = new Rectangle(12, 12, Color.web("#F7DF1E"));
                badge.setArcWidth(2); badge.setArcHeight(2);
                Text text = new Text("JS");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7));
                text.setFill(Color.BLACK);
                pane.getChildren().addAll(badge, text);
            }
            case "TS_BADGE" -> {
                Rectangle badge = new Rectangle(12, 12, Color.web("#3178C6"));
                badge.setArcWidth(2); badge.setArcHeight(2);
                Text text = new Text("TS");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7));
                text.setFill(Color.WHITE);
                pane.getChildren().addAll(badge, text);
            }
            case "ACTUAL_DECLARATION" -> {
                Polygon tri = new Polygon(6.0, 1.0, 1.0, 11.0, 11.0, 11.0);
                tri.setFill(Color.web("#D8A657"));
                Text text = new Text("A");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 6.5));
                text.setFill(Color.BLACK);
                pane.getChildren().addAll(tri, text);
            }
            case "EXPECT_DECLARATION" -> {
                Polygon dia = new Polygon(6.0, 0.0, 12.0, 6.0, 6.0, 12.0, 0.0, 6.0);
                dia.setFill(Color.web("#CC342D"));
                Text text = new Text("E");
                text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 6.5));
                text.setFill(Color.WHITE);
                pane.getChildren().addAll(dia, text);
            }
            case "HELM_WHEEL", "K8S_WHEEL" -> {
                Circle circle = new Circle(5.5, Color.TRANSPARENT);
                circle.setStroke(Color.web("#326CE5"));
                circle.setStrokeWidth(1.2);
                pane.getChildren().add(circle);
            }
            case "CHART_VALUES" -> {
                SVGPath chart = new SVGPath();
                chart.setContent("M 2 10 L 2 6 M 5 10 L 5 3 M 8 10 L 8 7 M 11 10 L 11 1");
                chart.setStroke(Color.web("#389FD6"));
                chart.setStrokeWidth(1.4);
                pane.getChildren().add(chart);
            }
            case "FOLDER" -> {
                SVGPath folder = new SVGPath();
                folder.setContent("M 1 2 L 5 2 L 6 4 L 12 4 L 12 11 L 1 11 Z");
                folder.setFill(Color.web("#E5C07B"));
                pane.getChildren().add(folder);
            }
            case "HEADPHONES" -> {
                SVGPath hp = new SVGPath();
                hp.setContent("M 2 7 A 4.5 4.5 0 0 1 11 7 L 11 10 A 1 1 0 0 1 9 10 L 9 7 M 2 7 L 2 10 A 1 1 0 0 0 4 10 L 4 7");
                hp.setStroke(Color.web("#59A869"));
                hp.setStrokeWidth(1.1);
                hp.setFill(Color.TRANSPARENT);
                pane.getChildren().add(hp);
            }
            case "CACHE_SERVER" -> {
                SVGPath s = new SVGPath();
                s.setContent("M 2 3 L 11 3 M 2 6 L 11 6 M 2 9 L 11 9");
                s.setStroke(Color.web("#E5C07B"));
                s.setStrokeWidth(1.3);
                pane.getChildren().add(s);
            }
            case "MICRONAUT_MU" -> {
                Text mu = new Text("\u03BC");
                mu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                mu.setFill(Color.web("#DFE1E5"));
                pane.getChildren().add(mu);
            }
            case "LEAF_DB", "SPRING_LEAF", "SPRINGBOOT_LEAF", "TEST_LEAF", "SPRING_BEAN" -> {
                SVGPath leaf = new SVGPath();
                leaf.setContent("M 2 10 C 2 3 7 1 11 1 C 11 6 10 10 2 10 Z");
                leaf.setFill(Color.web("#6DB33F"));
                pane.getChildren().add(leaf);
            }
            case "MESSAGE_QUEUE" -> {
                SVGPath q = new SVGPath();
                q.setContent("M 2 3 L 11 3 L 11 7 L 2 7 Z M 4 10 L 9 10");
                q.setStroke(Color.web("#389FD6"));
                q.setStrokeWidth(1.1);
                q.setFill(Color.TRANSPARENT);
                pane.getChildren().add(q);
            }
            case "WEBSOCKET" -> {
                SVGPath ws = new SVGPath();
                ws.setContent("M 2 6 L 5 3 L 5 9 Z M 7 4 C 8 5 8 7 7 8 M 9 2 C 11 4 11 8 9 10");
                ws.setStroke(Color.web("#E06C75"));
                ws.setStrokeWidth(1.1);
                ws.setFill(Color.TRANSPARENT);
                pane.getChildren().add(ws);
            }
            case "BLUE_STAR" -> {
                SVGPath star = new SVGPath();
                star.setContent("M 6 0 L 7.5 4.5 L 12 6 L 7.5 7.5 L 6 12 L 4.5 7.5 L 0 6 L 4.5 4.5 Z");
                star.setFill(Color.web("#3574F0"));
                pane.getChildren().add(star);
            }
            case "CLOCK_TIMER" -> {
                Circle c = new Circle(5.5, Color.TRANSPARENT);
                c.setStroke(Color.web("#DFE1E5"));
                c.setStrokeWidth(1.1);
                Line h = new Line(0, 0, 0, -3.5); h.setStroke(Color.web("#DFE1E5")); h.setStrokeWidth(1.0);
                Line m = new Line(0, 0, 2.5, 0); m.setStroke(Color.web("#DFE1E5")); m.setStrokeWidth(1.0);
                pane.getChildren().addAll(c, h, m);
            }
            case "FILE_TEXT", "FILE_CODE" -> {
                Rectangle f = new Rectangle(9, 11, Color.TRANSPARENT);
                f.setStroke(Color.web("#80848B"));
                f.setStrokeWidth(1.1);
                f.setArcWidth(2); f.setArcHeight(2);
                pane.getChildren().add(f);
            }
            case "RUBY_DIAMOND", "RBS_DIAMOND" -> {
                Polygon dia = new Polygon(6.0, 1.0, 11.0, 6.0, 6.0, 11.0, 1.0, 6.0);
                dia.setFill(Color.web("#CC342D"));
                pane.getChildren().add(dia);
            }
            case "MODULE_I" -> {
                Rectangle r = new Rectangle(11, 11, Color.web("#3178C6"));
                r.setArcWidth(2); r.setArcHeight(2);
                Text t = new Text("I");
                t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7));
                t.setFill(Color.WHITE);
                pane.getChildren().addAll(r, t);
            }
            case "RAILS_ACTION", "RAILS_MODEL", "RAILS_SCHEMA", "RAILS_VIEW" -> {
                Circle circle = new Circle(5.5, Color.web("#CC342D"));
                pane.getChildren().add(circle);
            }
            case "BOOK_DOC" -> {
                SVGPath book = new SVGPath();
                book.setContent("M 2 3 C 4 2 6 3 6 4 L 6 11 C 6 10 4 9 2 10 Z M 10 3 C 8 2 6 3 6 4 L 6 11 C 6 10 8 9 10 10 Z");
                book.setStroke(Color.web("#DFE1E5"));
                book.setStrokeWidth(1.0);
                book.setFill(Color.TRANSPARENT);
                pane.getChildren().add(book);
            }
            case "CIRCLE_C" -> {
                Circle c = new Circle(5.5, Color.TRANSPARENT);
                c.setStroke(Color.web("#3574F0"));
                c.setStrokeWidth(1.2);
                Text t = new Text("C");
                t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7));
                t.setFill(Color.web("#3574F0"));
                pane.getChildren().addAll(c, t);
            }
            case "AUTOWIRED" -> {
                Circle circle = new Circle(5.0, Color.TRANSPARENT);
                circle.setStroke(Color.web("#59A869"));
                circle.setStrokeWidth(1.2);
                SVGPath arrow = new SVGPath();
                arrow.setContent("M 4 1 L 0 5 M 0 5 L 4 5 M 0 5 L 0 1");
                arrow.setStroke(Color.web("#59A869"));
                arrow.setStrokeWidth(1.2);
                arrow.setFill(Color.TRANSPARENT);
                pane.getChildren().addAll(circle, arrow);
            }
            case "XML_TAG" -> {
                Text tag = new Text("</>");
                tag.setFont(Font.font("Segoe UI", FontWeight.BOLD, 8.5));
                tag.setFill(Color.web("#E5C07B"));
                pane.getChildren().add(tag);
            }
            case "GRAPH_ICON" -> {
                SVGPath g = new SVGPath();
                g.setContent("M 2 2 L 6 6 L 10 3 M 6 6 L 6 10");
                g.setStroke(Color.web("#389FD6"));
                g.setStrokeWidth(1.2);
                pane.getChildren().add(g);
            }
            case "PROPERTIES_P" -> {
                Circle cp = new Circle(5.5, Color.web("#D8A657"));
                Text tp = new Text("P");
                tp.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7));
                tp.setFill(Color.BLACK);
                pane.getChildren().addAll(cp, tp);
            }
            case "TESTING_BEANS" -> {
                SVGPath bean = new SVGPath();
                bean.setContent("M 2 8 C 2 3 6 1 10 1 C 10 5 9 8 2 8 Z");
                bean.setFill(Color.web("#6DB33F"));
                SVGPath check = new SVGPath();
                check.setContent("M 4 5 L 6 7 L 10 3");
                check.setStroke(Color.WHITE);
                check.setStrokeWidth(1.2);
                check.setFill(Color.TRANSPARENT);
                pane.getChildren().addAll(bean, check);
            }
            case "RUNTIME_BEANS", "RUNTIME_BEANS_XML", "RUNTIME_CONDITIONS", "SPRING_AOT" -> {
                Circle c = new Circle(5.5, Color.TRANSPARENT);
                c.setStroke(Color.web("#6DB33F"));
                c.setStrokeWidth(1.2);
                pane.getChildren().add(c);
            }
            case "CLOUD_STREAM" -> {
                SVGPath cloud = new SVGPath();
                cloud.setContent("M 3 8 A 2 2 0 0 1 3 5 A 3 3 0 0 1 8 4 A 2 2 0 0 1 11 6 A 2 2 0 0 1 9 9 L 3 9 Z");
                cloud.setStroke(Color.web("#389FD6"));
                cloud.setStrokeWidth(1.1);
                cloud.setFill(Color.TRANSPARENT);
                pane.getChildren().add(cloud);
            }
            case "SPRINGDATA_REPO" -> {
                Rectangle r = new Rectangle(11, 9, Color.TRANSPARENT);
                r.setStroke(Color.web("#6DB33F"));
                r.setStrokeWidth(1.1);
                r.setArcWidth(2); r.setArcHeight(2);
                Line l = new Line(4, 5, 8, 5); l.setStroke(Color.web("#6DB33F")); l.setStrokeWidth(1.0);
                pane.getChildren().addAll(r, l);
            }
            case "QUERY_CONSOLE", "MONGO_CONSOLE" -> {
                Rectangle rect = new Rectangle(11, 9, Color.TRANSPARENT);
                rect.setStroke(Color.web("#589DF6"));
                rect.setStrokeWidth(1.1);
                Text t = new Text(">");
                t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7));
                t.setFill(Color.web("#589DF6"));
                pane.getChildren().addAll(rect, t);
            }
            case "PROJECTIONS" -> {
                Rectangle r1 = new Rectangle(8, 8, Color.TRANSPARENT);
                r1.setStroke(Color.web("#389FD6"));
                r1.setStrokeWidth(1.0);
                Rectangle r2 = new Rectangle(8, 8, Color.TRANSPARENT);
                r2.setStroke(Color.web("#59A869"));
                r2.setStrokeWidth(1.0);
                r2.setTranslateX(2); r2.setTranslateY(2);
                pane.getChildren().addAll(r1, r2);
            }
            case "SPRING_WEB_VIEWS" -> {
                Circle circle = new Circle(5.5, Color.TRANSPARENT);
                circle.setStroke(Color.web("#59A869"));
                circle.setStrokeWidth(1.1);
                Line h = new Line(-4.5, 0, 4.5, 0);
                h.setStroke(Color.web("#59A869"));
                h.setStrokeWidth(0.9);
                Line v = new Line(0, -4.5, 0, 4.5);
                v.setStroke(Color.web("#59A869"));
                v.setStrokeWidth(0.9);
                pane.getChildren().addAll(circle, h, v);
            }
            case "SPRING_REQUEST_MAPPINGS" -> {
                Circle circle = new Circle(5.5, Color.TRANSPARENT);
                circle.setStroke(Color.web("#59A869"));
                circle.setStrokeWidth(1.1);
                SVGPath arrow = new SVGPath();
                arrow.setContent("M -2 0 L 2 0 M 0 -2 L 2 0 L 0 2");
                arrow.setStroke(Color.web("#59A869"));
                arrow.setStrokeWidth(1.0);
                arrow.setFill(Color.TRANSPARENT);
                pane.getChildren().addAll(circle, arrow);
            }
            default -> {
                Circle circle = new Circle(4.5, Color.web("#80848B"));
                pane.getChildren().add(circle);
            }
        }
        return pane;
    }
}