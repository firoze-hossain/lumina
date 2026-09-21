package dev.lumina.notification;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

/**
 * Severity level of a notification, matching IntelliJ IDEA.
 */
public enum NotificationType {
    INFORMATION("Information", "#3574F0"),
    WARNING("Warning", "#EDA200"),
    ERROR("Error", "#DB5860");

    private final String displayName;
    private final String hexColor;

    NotificationType(String displayName, String hexColor) {
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    public String displayName() {
        return displayName;
    }

    public String hexColor() {
        return hexColor;
    }

    /**
     * Creates a vector shape node matching IntelliJ IDEA's notification icons:
     * - INFORMATION: blue circle with bold white "i"
     * - WARNING: yellow triangle with black "!"
     * - ERROR: red circle with bold white "!"
     */
    public Node createIcon(double size) {
        StackPane pane = new StackPane();
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setAlignment(Pos.CENTER);

        double r = size / 2.0;

        switch (this) {
            case INFORMATION -> {
                Circle circle = new Circle(r);
                circle.setFill(Color.web(hexColor));
                Label label = new Label("i");
                label.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: " + (size * 0.68) + "px; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono', sans-serif;");
                pane.getChildren().addAll(circle, label);
            }
            case WARNING -> {
                // Triangle
                Polygon triangle = new Polygon(
                        r, 1,
                        size - 1, size - 1,
                        1, size - 1
                );
                triangle.setFill(Color.web(hexColor));
                Label label = new Label("!");
                label.setStyle("-fx-text-fill: #1E1F22; -fx-font-size: " + (size * 0.62) + "px; -fx-font-weight: 900; -fx-font-family: 'JetBrains Mono', sans-serif; -fx-padding: " + (size * 0.15) + " 0 0 0;");
                pane.getChildren().addAll(triangle, label);
            }
            case ERROR -> {
                Circle circle = new Circle(r);
                circle.setFill(Color.web(hexColor));
                Label label = new Label("!");
                label.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: " + (size * 0.68) + "px; -fx-font-weight: 900; -fx-font-family: 'JetBrains Mono', sans-serif;");
                pane.getChildren().addAll(circle, label);
            }
        }

        return pane;
    }
}
