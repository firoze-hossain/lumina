package dev.lumina.ui;

import dev.lumina.plugin.PluginItem;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

/**
 * Creates high-fidelity vector & canvas icons for IDE plugins matching screenshot styling.
 */
public class PluginIconFactory {

    public static Node createIcon(PluginItem item, double size) {
        if (item == null) {
            return createPlaceholder(size, "#393B40", "?");
        }

        String symbol = item.getIconSymbol();
        if (symbol == null) symbol = "";

        switch (symbol) {
            case "AI":
                return createAiSwirlIcon(size);
            case "JUNIE":
                return createJunieIcon(size);
            case "SPRING_DEBUG":
            case "SPRING":
                return createSpringLeafIcon(size, "SPRING_DEBUG".equals(symbol));
            case "KOTLIN":
                return createKotlinIcon(size);
            case "DEVELOCITY":
                return createDevelocityIcon(size);
            case "KEY_PROMOTER":
                return createKeyPromoterIcon(size);
            case "VIM":
                return createVimIcon(size);
            case "CONFLUENT":
                return createConfluentIcon(size);
            case "GH_ACTIONS":
                return createGhActionsIcon(size);
            case "TWIG":
                return createTwigIcon(size);
            case "SVN":
                return createSvnIcon(size);
            case "HTTP_CLIENT":
                return createHttpClientIcon(size);
            case "VITE":
                return createViteIcon(size);
            case "FULL_LINE":
                return createFullLineIcon(size);
            case "NOTEBOOKS":
                return createNotebooksIcon(size);
            case "CFG":
                return createCfgIcon(size);
            case "COPILOT":
                return createCopilotIcon(size);
            case "GO":
                return createGoIcon(size);
            default:
                String initial = item.getName() != null && !item.getName().isEmpty()
                        ? item.getName().substring(0, 1).toUpperCase()
                        : "P";
                String color = item.getIconBgColor() != null ? item.getIconBgColor() : "#2B2D30";
                return createPlaceholder(size, color, initial);
        }
    }

    private static Node createAiSwirlIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#1E1F22"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);

        // Purple swirling lines
        gc.setStroke(Color.web("#A855F7"));
        gc.setLineWidth(size * 0.08);
        double center = size / 2.0;
        double r = size * 0.32;
        gc.strokeArc(center - r, center - r, r * 2, r * 2, 45, 270, javafx.scene.shape.ArcType.OPEN);

        gc.setStroke(Color.web("#C084FC"));
        double r2 = size * 0.20;
        gc.strokeArc(center - r2, center - r2, r2 * 2, r2 * 2, 225, 270, javafx.scene.shape.ArcType.OPEN);

        return canvas;
    }

    private static Node createJunieIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#10B981"));
        double s = size * 0.35;
        double gap = size * 0.08;
        double x1 = (size - (s * 2 + gap)) / 2;
        double y1 = x1;
        // 3 geometric green square blocks
        gc.fillRoundRect(x1, y1 + s + gap, s, s, 3, 3);
        gc.fillRoundRect(x1 + s + gap, y1, s, s, 3, 3);
        gc.fillRoundRect(x1 + s + gap, y1 + s + gap, s, s, 3, 3);
        return canvas;
    }

    private static Node createSpringLeafIcon(double size, boolean debug) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#16A34A"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);

        // White leaf silhouette
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(size * 0.08);
        gc.strokeArc(size * 0.2, size * 0.2, size * 0.6, size * 0.6, 30, 120, javafx.scene.shape.ArcType.OPEN);
        gc.strokeLine(size * 0.25, size * 0.75, size * 0.75, size * 0.25);

        if (debug) {
            gc.setFill(Color.web("#EF4444"));
            gc.fillOval(size * 0.6, size * 0.6, size * 0.25, size * 0.25);
        }
        return canvas;
    }

    private static Node createKotlinIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        LinearGradient grad = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7F52FF")),
                new Stop(0.5, Color.web("#C711E1")),
                new Stop(1, Color.web("#E4485D")));
        gc.setFill(grad);
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);

        gc.setFill(Color.web("#1E1F22"));
        // Triangle cutout
        gc.fillPolygon(
                new double[]{size, size * 0.5, size},
                new double[]{0, size * 0.5, size},
                3
        );
        return canvas;
    }

    private static Node createDevelocityIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#06B6D4"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setFill(Color.WHITE);
        gc.fillOval(size * 0.25, size * 0.25, size * 0.5, size * 0.5);
        gc.setFill(Color.web("#06B6D4"));
        gc.fillOval(size * 0.35, size * 0.35, size * 0.3, size * 0.3);
        return canvas;
    }

    private static Node createKeyPromoterIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#1E293B"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setStroke(Color.web("#38BDF8"));
        gc.setLineWidth(size * 0.12);
        gc.strokeLine(size * 0.25, size * 0.25, size * 0.75, size * 0.75);
        gc.strokeLine(size * 0.75, size * 0.25, size * 0.25, size * 0.75);
        return canvas;
    }

    private static Node createVimIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#15803D"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, size * 0.65));
        gc.fillText("V", size * 0.28, size * 0.75);
        return canvas;
    }

    private static Node createConfluentIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0F172A"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(size * 0.06);
        double c = size / 2;
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            gc.strokeLine(c, c, c + Math.cos(angle) * size * 0.35, c + Math.sin(angle) * size * 0.35);
        }
        return canvas;
    }

    private static Node createGhActionsIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#334155"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setStroke(Color.web("#94A3B8"));
        gc.setLineWidth(size * 0.08);
        gc.strokeLine(size * 0.3, size * 0.7, size * 0.5, size * 0.3);
        gc.strokeLine(size * 0.5, size * 0.3, size * 0.7, size * 0.7);
        gc.setFill(Color.web("#38BDF8"));
        gc.fillOval(size * 0.42, size * 0.22, size * 0.16, size * 0.16);
        gc.fillOval(size * 0.22, size * 0.62, size * 0.16, size * 0.16);
        gc.fillOval(size * 0.62, size * 0.62, size * 0.16, size * 0.16);
        return canvas;
    }

    private static Node createTwigIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#65A30D"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setFill(Color.WHITE);
        gc.fillOval(size * 0.3, size * 0.25, size * 0.15, size * 0.5);
        gc.fillOval(size * 0.5, size * 0.35, size * 0.15, size * 0.4);
        return canvas;
    }

    private static Node createSvnIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#1E3A8A"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(size * 0.08);
        gc.strokeLine(size * 0.3, size * 0.2, size * 0.3, size * 0.8);
        gc.strokeLine(size * 0.3, size * 0.5, size * 0.7, size * 0.3);
        return canvas;
    }

    private static Node createHttpClientIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        // Cyan-blue squircle with right arrow
        gc.setFill(Color.web("#0284C7"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setFill(Color.WHITE);
        // Clean right arrow
        double y = size / 2.0;
        gc.fillPolygon(
                new double[]{size * 0.25, size * 0.55, size * 0.55, size * 0.75, size * 0.55, size * 0.55, size * 0.25},
                new double[]{y - size * 0.12, y - size * 0.12, y - size * 0.25, y, y + size * 0.25, y + size * 0.12, y + size * 0.12},
                7
        );
        return canvas;
    }

    private static Node createViteIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#7C3AED"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        // Yellow lightning
        gc.setFill(Color.web("#FBBF24"));
        gc.fillPolygon(
                new double[]{size * 0.55, size * 0.35, size * 0.48, size * 0.40, size * 0.65, size * 0.52},
                new double[]{size * 0.2, size * 0.52, size * 0.52, size * 0.8, size * 0.44, size * 0.44},
                6
        );
        return canvas;
    }

    private static Node createFullLineIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#D946EF"));
        gc.fillOval(0, 0, size, size);
        gc.setFill(Color.WHITE);
        gc.fillOval(size * 0.35, size * 0.35, size * 0.3, size * 0.3);
        return canvas;
    }

    private static Node createNotebooksIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#4F46E5"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(size * 0.08);
        gc.strokeRoundRect(size * 0.25, size * 0.2, size * 0.5, size * 0.6, 2, 2);
        gc.strokeLine(size * 0.35, size * 0.4, size * 0.65, size * 0.4);
        gc.strokeLine(size * 0.35, size * 0.6, size * 0.55, size * 0.6);
        return canvas;
    }

    private static Node createCfgIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#475569"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(size * 0.08);
        gc.strokeLine(size * 0.3, size * 0.3, size * 0.7, size * 0.3);
        gc.strokeLine(size * 0.5, size * 0.3, size * 0.5, size * 0.7);
        return canvas;
    }

    private static Node createCopilotIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0F172A"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        gc.setFill(Color.WHITE);
        gc.fillOval(size * 0.25, size * 0.3, size * 0.5, size * 0.45);
        gc.setFill(Color.web("#0F172A"));
        gc.fillOval(size * 0.35, size * 0.45, size * 0.1, size * 0.1);
        gc.fillOval(size * 0.55, size * 0.45, size * 0.1, size * 0.1);
        return canvas;
    }

    private static Node createGoIcon(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#00ADD8"));
        gc.fillRoundRect(0, 0, size, size, size * 0.25, size * 0.25);
        // Gopher eyes
        gc.setFill(Color.WHITE);
        gc.fillOval(size * 0.25, size * 0.32, size * 0.22, size * 0.22);
        gc.fillOval(size * 0.53, size * 0.32, size * 0.22, size * 0.22);
        gc.setFill(Color.BLACK);
        gc.fillOval(size * 0.32, size * 0.38, size * 0.08, size * 0.08);
        gc.fillOval(size * 0.60, size * 0.38, size * 0.08, size * 0.08);
        return canvas;
    }

    private static Node createPlaceholder(double size, String color, String text) {
        StackPane pane = new StackPane();
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setStyle("-fx-background-color: " + color + "; -fx-background-radius: " + (size * 0.25) + ";");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: " + (size * 0.45) + "px; -fx-font-weight: bold;");
        pane.getChildren().add(lbl);
        return pane;
    }
}
