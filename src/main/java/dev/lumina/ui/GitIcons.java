package dev.lumina.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

/**
 * High-fidelity vector SVG icons matching IntelliJ IDEA's Git UI.
 */
public final class GitIcons {

    private GitIcons() {}

    private static StackPane box(int size, Node... children) {
        StackPane sp = new StackPane(children);
        sp.setMinSize(size, size);
        sp.setPrefSize(size, size);
        sp.setMaxSize(size, size);
        sp.setAlignment(Pos.CENTER);
        return sp;
    }

    /** GitHub Octocat vector icon. */
    public static Node gitHubIcon(double size, String colorHex) {
        SVGPath p = new SVGPath();
        p.setContent("M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 "
                + "0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53 "
                + ".63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 "
                + "0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27 "
                + ".68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 "
                + "0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38 "
                + "A8.012 8.012 0 0 0 16 8c0-4.42-3.58-8-8-8z");
        p.setFill(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        p.setScaleX(size / 16.0);
        p.setScaleY(size / 16.0);
        return box((int) size, p);
    }

    public static Node gitHubIcon() {
        return gitHubIcon(15, "#DFE1E5");
    }

    /** GitLab Orange Fox vector icon matching IntelliJ. */
    public static Node gitLabIcon(double size) {
        // Fox head polygon path
        SVGPath back = new SVGPath();
        back.setContent("M 15.97 9.06 L 15.16 6.56 C 15.02 6.13 14.43 6.13 14.29 6.56 L 13.48 9.06 H 2.52 L 1.71 6.56 C 1.57 6.13 0.98 6.13 0.84 6.56 L 0.03 9.06 C -0.1 9.47 0.05 9.92 0.39 10.17 L 7.6 15.41 C 7.84 15.58 8.16 15.58 8.4 15.41 L 15.61 10.17 C 15.95 9.92 16.1 9.47 15.97 9.06 Z");
        back.setFill(Color.web("#FC6D26"));

        SVGPath center = new SVGPath();
        center.setContent("M 8 15.5 L 5 6.5 L 7 0.5 L 8 3.5 L 9 0.5 L 11 6.5 Z");
        center.setFill(Color.web("#E24329"));

        Group g = new Group(back, center);
        g.setScaleX(size / 16.0);
        g.setScaleY(size / 16.0);
        return box((int) size, g);
    }

    public static Node gitLabIcon() {
        return gitLabIcon(15);
    }

    /** Git Branch/Fork icon for Repository URL tab. */
    public static Node gitBranchIcon(double size, String colorHex) {
        SVGPath p = new SVGPath();
        p.setContent("M 11 3 C 11 1.9 10.1 1 9 1 C 7.9 1 7 1.9 7 3 C 7 3.8 7.5 4.5 8.2 4.8 L 6.5 8.2 C 5.8 7.5 4.8 7 3.7 7 C 2.2 7 1 8.2 1 9.7 C 1 11.2 2.2 12.4 3.7 12.4 C 4.8 12.4 5.8 11.9 6.5 11.2 L 9.8 11.2 C 10.2 11.7 10.8 12 11.5 12 C 12.6 12 13.5 11.1 13.5 10 C 13.5 8.9 12.6 8 11.5 8 C 10.8 8 10.2 8.3 9.8 8.8 L 6.5 8.8 L 8.2 5.2 C 8.5 5.2 8.7 5.2 9 5.2 C 10.1 5.2 11 4.3 11 3.2 Z");
        p.setFill(Color.web(colorHex != null ? colorHex : "#3574F0"));
        p.setScaleX(size / 14.0);
        p.setScaleY(size / 14.0);
        return box((int) size, p);
    }

    public static Node gitBranchIcon() {
        return gitBranchIcon(15, "#3574F0");
    }

    /** Circular Sync icon (for Sync Fork). */
    public static Node syncIcon(double size, String colorHex) {
        SVGPath p = new SVGPath();
        p.setContent("M 12 4 V 1 L 8 5 L 12 9 V 6 C 15.31 6 18 8.69 18 12 C 18 13.01 17.75 13.97 17.3 14.8 L 18.76 16.26 C 19.53 15.04 20 13.58 20 12 C 20 7.58 16.42 4 12 4 Z M 12 18 C 8.69 18 6 15.31 6 12 C 6 10.99 6.25 10.03 6.7 9.2 L 5.24 7.74 C 4.47 8.96 4 10.42 4 12 C 4 16.42 7.58 20 12 20 V 23 L 16 19 L 12 15 V 18 Z");
        p.setFill(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        p.setScaleX(size / 24.0);
        p.setScaleY(size / 24.0);
        return box((int) size, p);
    }

    public static Node syncIcon() {
        return syncIcon(15, "#DFE1E5");
    }

    /** Globe icon (for View in browser). */
    public static Node globeIcon(double size, String colorHex) {
        SVGPath p = new SVGPath();
        p.setContent("M 8 1 A 7 7 0 1 0 15 8 A 7 7 0 0 0 8 1 Z M 7.3 2.05 A 5.95 5.95 0 0 1 7.3 4.25 H 4.1 A 5.95 5.95 0 0 1 7.3 2.05 Z M 3.4 5.45 H 7.3 V 8 H 2.6 A 5.9 5.9 0 0 1 3.4 5.45 Z M 2.6 9.2 H 7.3 V 11.75 H 3.4 A 5.9 5.9 0 0 1 2.6 9.2 Z M 4.1 12.95 H 7.3 A 5.95 5.95 0 0 1 4.1 12.95 Z M 8.7 13.95 A 5.95 5.95 0 0 1 8.7 11.75 H 11.9 A 5.95 5.95 0 0 1 8.7 13.95 Z M 12.6 10.55 H 8.7 V 8 H 13.4 A 5.9 5.9 0 0 1 12.6 10.55 Z M 13.4 6.8 H 8.7 V 4.25 H 12.6 A 5.9 5.9 0 0 1 13.4 6.8 Z M 11.9 3.05 A 5.95 5.95 0 0 1 8.7 3.05 V 2.05 A 5.95 5.95 0 0 1 11.9 3.05 Z");
        p.setFill(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        p.setScaleX(size / 16.0);
        p.setScaleY(size / 16.0);
        return box((int) size, p);
    }

    public static Node globeIcon() {
        return globeIcon(15, "#DFE1E5");
    }

    /** Clock icon (for Show History). */
    public static Node clockIcon(double size, String colorHex) {
        Circle circle = new Circle(size / 2.0 - 1.0);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        circle.setStrokeWidth(1.2);

        Line h = new Line(0, 0, size * 0.22, 0);
        h.setStroke(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        h.setStrokeWidth(1.2);

        Line m = new Line(0, 0, 0, -size * 0.3);
        m.setStroke(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        m.setStrokeWidth(1.2);

        StackPane sp = new StackPane(circle, h, m);
        sp.setAlignment(Pos.CENTER);
        return box((int) size, sp);
    }

    public static Node clockIcon() {
        return clockIcon(14, "#DFE1E5");
    }

    /** Diff icon: two arrows pointing opposite directions (←→). */
    public static Node diffIcon(double size, String colorHex) {
        SVGPath p = new SVGPath();
        p.setContent("M 2 5 L 12 5 M 9 2 L 12 5 L 9 8 M 14 11 L 4 11 M 7 8 L 4 11 L 7 14");
        p.setStroke(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        p.setStrokeWidth(1.3);
        p.setFill(null);
        p.setScaleX(size / 16.0);
        p.setScaleY(size / 16.0);
        return box((int) size, p);
    }

    public static Node diffIcon() {
        return diffIcon(14, "#DFE1E5");
    }

    /** Plus icon for toolbar. */
    public static Node plusIcon(double size, String colorHex) {
        Line h = new Line(-size * 0.35, 0, size * 0.35, 0);
        Line v = new Line(0, -size * 0.35, 0, size * 0.35);
        Color c = Color.web(colorHex != null ? colorHex : "#DFE1E5");
        h.setStroke(c);
        h.setStrokeWidth(1.6);
        v.setStroke(c);
        v.setStrokeWidth(1.6);
        return box((int) size, new StackPane(h, v));
    }

    public static Node plusIcon() {
        return plusIcon(14, "#DFE1E5");
    }

    /** Minus icon for toolbar. */
    public static Node minusIcon(double size, String colorHex) {
        Line h = new Line(-size * 0.35, 0, size * 0.35, 0);
        h.setStroke(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        h.setStrokeWidth(1.6);
        return box((int) size, new StackPane(h));
    }

    public static Node minusIcon() {
        return minusIcon(14, "#DFE1E5");
    }

    /** Pencil edit icon for toolbar. */
    public static Node editIcon(double size, String colorHex) {
        SVGPath p = new SVGPath();
        p.setContent("M 9.5 2.5 L 11.5 4.5 L 4 12 L 2 12 L 2 10 Z");
        p.setStroke(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        p.setStrokeWidth(1.2);
        p.setFill(Color.web(colorHex != null ? colorHex : "#DFE1E5", 0.4));
        p.setScaleX(size / 14.0);
        p.setScaleY(size / 14.0);
        return box((int) size, p);
    }

    public static Node editIcon() {
        return editIcon(14, "#DFE1E5");
    }

    /** Folder browse icon. */
    public static Node folderIcon(double size, String colorHex) {
        SVGPath folder = new SVGPath();
        folder.setContent("M 2 4 L 5.5 4 L 7 5.5 L 14 5.5 L 14 12.5 L 2 12.5 Z");
        folder.setStroke(Color.web(colorHex != null ? colorHex : "#8C919D"));
        folder.setStrokeWidth(1.2);
        folder.setFill(Color.web(colorHex != null ? colorHex : "#8C919D", 0.15));
        folder.setScaleX(size / 16.0);
        folder.setScaleY(size / 16.0);
        return box((int) size, folder);
    }

    public static Node folderIcon() {
        return folderIcon(15, "#8C919D");
    }

    /** Search magnifying glass icon. */
    public static Node searchIcon(double size, String colorHex) {
        Circle c = new Circle(size * 0.28);
        c.setFill(Color.TRANSPARENT);
        c.setStroke(Color.web(colorHex != null ? colorHex : "#8C919D"));
        c.setStrokeWidth(1.3);
        c.setTranslateX(-size * 0.08);
        c.setTranslateY(-size * 0.08);

        Line handle = new Line(size * 0.08, size * 0.08, size * 0.36, size * 0.36);
        handle.setStroke(Color.web(colorHex != null ? colorHex : "#8C919D"));
        handle.setStrokeWidth(1.5);

        StackPane sp = new StackPane(c, handle);
        sp.setAlignment(Pos.CENTER);
        return box((int) size, sp);
    }

    public static Node searchIcon() {
        return searchIcon(14, "#8C919D");
    }

    /** Commit node icon for VCS popup and tool windows. */
    public static Node commitIcon(double size, String colorHex) {
        Circle c = new Circle(size * 0.22);
        c.setFill(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        Line l1 = new Line(-size * 0.45, 0, -size * 0.22, 0);
        l1.setStroke(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        l1.setStrokeWidth(1.4);
        Line l2 = new Line(size * 0.22, 0, size * 0.45, 0);
        l2.setStroke(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        l2.setStrokeWidth(1.4);
        StackPane sp = new StackPane(l1, c, l2);
        sp.setAlignment(Pos.CENTER);
        return box((int) size, sp);
    }

    public static Node commitIcon() {
        return commitIcon(14, "#DFE1E5");
    }

    /** Rollback curved arrow icon. */
    public static Node rollbackIcon(double size, String colorHex) {
        SVGPath p = new SVGPath();
        p.setContent("M 12 5 L 8 1 L 8 4 C 4 4 1 7 1 11 C 1 12.5 1.5 13.8 2.3 15 L 3.8 13.5 C 3.3 12.8 3 11.9 3 11 C 3 8.2 5.2 6 8 6 L 8 9 Z");
        p.setFill(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        p.setScaleX(size / 16.0);
        p.setScaleY(size / 16.0);
        return box((int) size, p);
    }

    public static Node rollbackIcon() {
        return rollbackIcon(14, "#DFE1E5");
    }

    /** Push diagonal arrow up-right icon. */
    public static Node pushIcon(double size, String colorHex) {
        SVGPath p = new SVGPath();
        p.setContent("M 4 12 L 10.5 5.5 L 7 5.5 L 7 4 L 13 4 L 13 10 L 11.5 10 L 11.5 6.5 L 5 13 Z");
        p.setFill(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        p.setScaleX(size / 16.0);
        p.setScaleY(size / 16.0);
        return box((int) size, p);
    }

    public static Node pushIcon() {
        return pushIcon(14, "#DFE1E5");
    }

    /** Copy document icon for Copy Branch Name. */
    public static Node copyIcon(double size, String colorHex) {
        SVGPath p = new SVGPath();
        p.setContent("M 2 2 C 1.4 2 1 2.4 1 3 L 1 10 L 2.2 10 L 2.2 3.2 L 9 3.2 L 9 2 Z M 3.5 4 C 2.9 4 2.5 4.4 2.5 5 L 2.5 13 C 2.5 13.6 2.9 14 3.5 14 L 11.5 14 C 12.1 14 12.5 13.6 12.5 13 L 12.5 5 C 12.5 4.4 12.1 4 11.5 4 Z M 3.8 5.3 L 11.2 5.3 L 11.2 12.7 L 3.8 12.7 Z");
        p.setFill(Color.web(colorHex != null ? colorHex : "#DFE1E5"));
        p.setScaleX(size / 14.0);
        p.setScaleY(size / 14.0);
        return box((int) size, p);
    }

    public static Node copyIcon() {
        return copyIcon(14, "#DFE1E5");
    }
}
