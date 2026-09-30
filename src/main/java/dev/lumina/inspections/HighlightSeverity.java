package dev.lumina.inspections;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Inspection highlight severities with visual icon renderers matching IDE standards.
 */
public enum HighlightSeverity {
    ERROR("Error", "#F75464", 400),
    WARNING("Warning", "#F5D247", 300),
    WEAK_WARNING("Weak Warning", "#E0AE43", 200),
    SERVER_PROBLEM("Server Problem", "#F08C35", 350),
    GRAMMAR_ERROR("Grammar Error", "#57B670", 250),
    TYPO("Typo", "#57B670", 150),
    INFO("Info", "#54A8FF", 120),
    STYLE_SUGGESTION("Style Suggestion", "#54A8FF", 100),
    CONSIDERATION("Consideration", "#868A91", 50),
    NO_HIGHLIGHTING("No highlighting (fix available)", "#868A91", 0);

    private final String displayName;
    private final String hexColor;
    private final int level;

    HighlightSeverity(String displayName, String hexColor, int level) {
        this.displayName = displayName;
        this.hexColor = hexColor;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHexColor() {
        return hexColor;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Creates a crisp vector icon for this severity level.
     */
    public Node createIcon(double size) {
        switch (this) {
            case ERROR: {
                StackPane pane = new StackPane();
                pane.setMinSize(size, size);
                pane.setPrefSize(size, size);
                pane.setMaxSize(size, size);

                Circle circle = new Circle(size / 2.0);
                circle.setFill(Color.web(hexColor));

                Text ex = new Text("!");
                ex.setFill(Color.WHITE);
                ex.setFont(Font.font("System", FontWeight.BOLD, size * 0.75));

                pane.getChildren().addAll(circle, ex);
                return pane;
            }
            case WARNING: {
                StackPane pane = new StackPane();
                pane.setMinSize(size, size);
                pane.setPrefSize(size, size);
                pane.setMaxSize(size, size);

                Polygon triangle = new Polygon();
                triangle.getPoints().addAll(
                        size / 2.0, 1.0,
                        size - 1.0, size - 1.0,
                        1.0, size - 1.0
                );
                triangle.setFill(Color.web(hexColor));

                Text ex = new Text("!");
                ex.setFill(Color.web("#1E1F22"));
                ex.setFont(Font.font("System", FontWeight.BOLD, size * 0.65));
                ex.setTranslateY(size * 0.08);

                pane.getChildren().addAll(triangle, ex);
                return pane;
            }
            case SERVER_PROBLEM: {
                StackPane pane = new StackPane();
                pane.setMinSize(size, size);
                pane.setPrefSize(size, size);
                pane.setMaxSize(size, size);

                Polygon triangle = new Polygon();
                triangle.getPoints().addAll(
                        size / 2.0, 1.0,
                        size - 1.0, size - 1.0,
                        1.0, size - 1.0
                );
                triangle.setFill(Color.web(hexColor));

                Text ex = new Text("!");
                ex.setFill(Color.WHITE);
                ex.setFont(Font.font("System", FontWeight.BOLD, size * 0.65));
                ex.setTranslateY(size * 0.08);

                pane.getChildren().addAll(triangle, ex);
                return pane;
            }
            case WEAK_WARNING: {
                StackPane pane = new StackPane();
                pane.setMinSize(size, size);
                pane.setPrefSize(size, size);
                pane.setMaxSize(size, size);

                SVGPath chevron = new SVGPath();
                chevron.setContent("M 2 8 L 6 3 L 10 8");
                chevron.setStroke(Color.web(hexColor));
                chevron.setStrokeWidth(2.0);
                chevron.setFill(null);

                pane.getChildren().add(chevron);
                return pane;
            }
            case GRAMMAR_ERROR:
            case TYPO: {
                StackPane pane = new StackPane();
                pane.setMinSize(size, size);
                pane.setPrefSize(size, size);
                pane.setMaxSize(size, size);

                SVGPath check = new SVGPath();
                check.setContent("M 2 7 L 5 10 L 11 3");
                check.setStroke(Color.web(hexColor));
                check.setStrokeWidth(2.0);
                check.setFill(null);

                pane.getChildren().add(check);
                return pane;
            }
            case STYLE_SUGGESTION: {
                HBox dots = new HBox(1.5);
                dots.setAlignment(Pos.CENTER);
                dots.setMinSize(size, size);
                dots.setPrefSize(size, size);
                dots.setMaxSize(size, size);

                for (int i = 0; i < 3; i++) {
                    Circle dot = new Circle(1.5);
                    dot.setFill(Color.web(hexColor));
                    dots.getChildren().add(dot);
                }
                return dots;
            }
            case CONSIDERATION:
            case NO_HIGHLIGHTING:
            default: {
                StackPane pane = new StackPane();
                pane.setMinSize(size, size);
                pane.setPrefSize(size, size);
                pane.setMaxSize(size, size);
                return pane;
            }
        }
    }

    /**
     * Creates a mixed severity vector icon (half error red / half warning yellow)
     * matching the "Mixed" severity state in the reference IDE.
     */
    public static Node createMixedIcon(double size) {
        StackPane pane = new StackPane();
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);

        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(
                size / 2.0, 1.0,
                size - 1.0, size - 1.0,
                1.0, size - 1.0
        );
        triangle.setFill(Color.web("#F5D247"));

        Circle dot = new Circle(size * 0.28);
        dot.setFill(Color.web("#F75464"));
        dot.setTranslateX(size * 0.18);
        dot.setTranslateY(size * 0.18);

        Text ex = new Text("!");
        ex.setFill(Color.WHITE);
        ex.setFont(Font.font("System", FontWeight.BOLD, size * 0.5));
        ex.setTranslateX(size * 0.18);
        ex.setTranslateY(size * 0.18);

        pane.getChildren().addAll(triangle, dot, ex);
        return pane;
    }

    public static HighlightSeverity fromDisplayName(String name) {
        if (name == null) return WARNING;
        for (HighlightSeverity s : values()) {
            if (s.displayName.equalsIgnoreCase(name.trim()) || s.name().equalsIgnoreCase(name.trim())) {
                return s;
            }
        }
        return WARNING;
    }
}
