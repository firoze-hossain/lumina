package dev.lumina.gutter;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Vector gutter icons with high fidelity matching IntelliJ IDEA Ultimate:
 * - Implements method ((I) ↑)
 * - Implemented in method ((I) ↓)
 * - Overrides method ((O) ↑)
 * - Inferred annotations (@)
 * - Spring Bean / Autowired dependency (↙)
 * - Class implements / Interface declaration icons
 */
public final class GutterMarkerIcons {

    private static final String GREEN = "#59A869";
    private static final String RED_ARROW = "#E06C75";
    private static final String MUTED_GRAY = "#80848B";
    private static final String TEXT_LIGHT = "#DFE1E5";

    private GutterMarkerIcons() {}

    public static Node getIcon(GutterMarker.MarkerType type) {
        return switch (type) {
            case IMPLEMENTS_METHOD -> createImplementsMethodIcon();
            case OVERRIDES_METHOD -> createOverridesMethodIcon();
            case IMPLEMENTED_METHOD -> createImplementedMethodIcon();
            case IMPLEMENTS_INTERFACE -> createImplementsInterfaceIcon();
            case IMPLEMENTED_INTERFACE -> createImplementedInterfaceIcon();
            case INFERRED_ANNOTATION -> createInferredAnnotationIcon();
            case BEAN_INJECTION -> createBeanInjectionIcon();
        };
    }

    /** (I) with upward arrow ↑ in top-right corner (implements interface method). */
    public static Node createImplementsMethodIcon() {
        StackPane root = new StackPane();
        root.setPrefSize(14, 14);
        root.setMinSize(14, 14);
        root.setMaxSize(14, 14);
        root.setAlignment(Pos.CENTER);

        Circle circle = new Circle(5.0);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(GREEN));
        circle.setStrokeWidth(1.2);

        Text text = new Text("I");
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.5));
        text.setFill(Color.web(GREEN));

        SVGPath arrow = new SVGPath();
        arrow.setContent("M 0 3 L 2 0 L 4 3 M 2 0 L 2 4");
        arrow.setStroke(Color.web(RED_ARROW));
        arrow.setStrokeWidth(1.2);
        arrow.setFill(Color.TRANSPARENT);
        arrow.setTranslateX(4.5);
        arrow.setTranslateY(-4.0);

        root.getChildren().addAll(circle, text, arrow);
        applyHoverStyle(root);
        return root;
    }

    /** (O) with upward arrow ↑ (overrides superclass method). */
    public static Node createOverridesMethodIcon() {
        StackPane root = new StackPane();
        root.setPrefSize(14, 14);
        root.setMinSize(14, 14);
        root.setMaxSize(14, 14);
        root.setAlignment(Pos.CENTER);

        Circle circle = new Circle(5.0);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(GREEN));
        circle.setStrokeWidth(1.2);

        Text text = new Text("O");
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.0));
        text.setFill(Color.web(GREEN));

        SVGPath arrow = new SVGPath();
        arrow.setContent("M 0 3 L 2 0 L 4 3 M 2 0 L 2 4");
        arrow.setStroke(Color.web(GREEN));
        arrow.setStrokeWidth(1.2);
        arrow.setFill(Color.TRANSPARENT);
        arrow.setTranslateX(4.5);
        arrow.setTranslateY(-4.0);

        root.getChildren().addAll(circle, text, arrow);
        applyHoverStyle(root);
        return root;
    }

    /** (I) with downward arrow ↓ in bottom-right corner (is implemented in subclass). */
    public static Node createImplementedMethodIcon() {
        StackPane root = new StackPane();
        root.setPrefSize(14, 14);
        root.setMinSize(14, 14);
        root.setMaxSize(14, 14);
        root.setAlignment(Pos.CENTER);

        Circle circle = new Circle(5.0);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(GREEN));
        circle.setStrokeWidth(1.2);

        Text text = new Text("I");
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.5));
        text.setFill(Color.web(GREEN));

        SVGPath arrow = new SVGPath();
        arrow.setContent("M 0 1 L 2 4 L 4 1 M 2 4 L 2 0");
        arrow.setStroke(Color.web(GREEN));
        arrow.setStrokeWidth(1.2);
        arrow.setFill(Color.TRANSPARENT);
        arrow.setTranslateX(4.5);
        arrow.setTranslateY(4.0);

        root.getChildren().addAll(circle, text, arrow);
        applyHoverStyle(root);
        return root;
    }

    /** (I) with downward arrow ↓ for interface declaration. */
    public static Node createImplementedInterfaceIcon() {
        return createImplementedMethodIcon();
    }

    /** Green circle with diagonal line (class implements interface). */
    public static Node createImplementsInterfaceIcon() {
        StackPane root = new StackPane();
        root.setPrefSize(14, 14);
        root.setMinSize(14, 14);
        root.setMaxSize(14, 14);
        root.setAlignment(Pos.CENTER);

        Circle circle = new Circle(5.0);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(GREEN));
        circle.setStrokeWidth(1.2);

        Line diag = new Line(-3.0, 3.0, 3.0, -3.0);
        diag.setStroke(Color.web(GREEN));
        diag.setStrokeWidth(1.2);

        root.getChildren().addAll(circle, diag);
        applyHoverStyle(root);
        return root;
    }

    /** Subtle circle with '@' glyph (inferred annotations available). */
    public static Node createInferredAnnotationIcon() {
        StackPane root = new StackPane();
        root.setPrefSize(14, 14);
        root.setMinSize(14, 14);
        root.setMaxSize(14, 14);
        root.setAlignment(Pos.CENTER);

        Circle circle = new Circle(5.2);
        circle.setFill(Color.web("#313438"));
        circle.setStroke(Color.web(MUTED_GRAY));
        circle.setStrokeWidth(1.0);

        Text text = new Text("@");
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 8.0));
        text.setFill(Color.web(TEXT_LIGHT));

        root.getChildren().addAll(circle, text);
        applyHoverStyle(root);
        return root;
    }

    /** Green circle with down-left arrow ↙ (Spring bean / autowired dependency). */
    public static Node createBeanInjectionIcon() {
        StackPane root = new StackPane();
        root.setPrefSize(14, 14);
        root.setMinSize(14, 14);
        root.setMaxSize(14, 14);
        root.setAlignment(Pos.CENTER);

        Circle circle = new Circle(5.0);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(GREEN));
        circle.setStrokeWidth(1.2);

        SVGPath arrow = new SVGPath();
        arrow.setContent("M 3 -3 L -2 2 M -2 -1 L -2 2 L 1 2");
        arrow.setStroke(Color.web(GREEN));
        arrow.setStrokeWidth(1.2);
        arrow.setFill(Color.TRANSPARENT);

        root.getChildren().addAll(circle, arrow);
        applyHoverStyle(root);
        return root;
    }

    private static void applyHoverStyle(Node node) {
        node.setStyle("-fx-cursor: hand;");
        node.setOnMouseEntered(e -> node.setStyle("-fx-cursor: hand; -fx-opacity: 0.8; -fx-scale-x: 1.1; -fx-scale-y: 1.1;"));
        node.setOnMouseExited(e -> node.setStyle("-fx-cursor: hand; -fx-opacity: 1.0; -fx-scale-x: 1.0; -fx-scale-y: 1.0;"));
    }
}
