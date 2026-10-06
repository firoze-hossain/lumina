package dev.lumina.filetypes;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Generates crisp, modern JetBrains-style vector file type icons
 * for display in the File Types list and dialogs.
 */
public final class FileTypeIcon {

    private FileTypeIcon() {
    }

    public static Node getIcon(String iconKind, double size) {
        if (iconKind == null) iconKind = "generic";
        String kind = iconKind.toLowerCase();

        StackPane container = new StackPane();
        container.setPrefSize(size, size);
        container.setMinSize(size, size);
        container.setMaxSize(size, size);

        Rectangle base = new Rectangle(size, size);
        base.setArcWidth(size * 0.3);
        base.setArcHeight(size * 0.3);

        Label badge = new Label();
        badge.setAlignment(Pos.CENTER);
        double fontSize = Math.max(8.0, size * 0.52);

        switch (kind) {
            case "docker":
            case "dockerignore":
                base.setFill(Color.web("#0db7ed"));
                badge.setText("🐳");
                badge.setStyle("-fx-font-size: " + (size * 0.6) + "px;");
                container.getChildren().addAll(base, badge);
                break;

            case "git":
            case "gitignore":
            case "ignore":
                base.setFill(Color.web("#F05032"));
                badge.setText("GIT");
                badge.setStyle("-fx-font-size: " + (size * 0.38) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "angular":
            case "angular_html":
            case "angular_svg":
                base.setFill(Color.web("#DD0031"));
                badge.setText("A");
                badge.setStyle("-fx-font-size: " + fontSize + "px; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', sans-serif;");
                container.getChildren().addAll(base, badge);
                break;

            case "archive":
            case "zip":
            case "jar":
                base.setFill(Color.web("#E6A23C"));
                badge.setText("ZIP");
                badge.setStyle("-fx-font-size: " + (size * 0.38) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "csharp":
            case "cs":
                base.setFill(Color.web("#68217A"));
                badge.setText("C#");
                badge.setStyle("-fx-font-size: " + (size * 0.44) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "cpp":
            case "c":
                base.setFill(Color.web("#00599C"));
                badge.setText("C++");
                badge.setStyle("-fx-font-size: " + (size * 0.40) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "css":
                base.setFill(Color.web("#264DE4"));
                badge.setText("#");
                badge.setStyle("-fx-font-size: " + (size * 0.60) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "html":
                base.setFill(Color.web("#E34F26"));
                badge.setText("<>");
                badge.setStyle("-fx-font-size: " + (size * 0.42) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "java":
                base.setFill(Color.web("#EA2D2E"));
                badge.setText("☕");
                badge.setStyle("-fx-font-size: " + (size * 0.58) + "px;");
                container.getChildren().addAll(base, badge);
                break;

            case "javascript":
            case "js":
                base.setFill(Color.web("#F7DF1E"));
                badge.setText("JS");
                badge.setStyle("-fx-font-size: " + (size * 0.44) + "px; -fx-text-fill: #1E1F22; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "json":
                base.setFill(Color.web("#F5A623"));
                badge.setText("{ }");
                badge.setStyle("-fx-font-size: " + (size * 0.40) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "kotlin":
            case "kt":
                base.setFill(Color.web("#7F52FF"));
                badge.setText("K");
                badge.setStyle("-fx-font-size: " + fontSize + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "python":
            case "py":
                base.setFill(Color.web("#3776AB"));
                badge.setText("Py");
                badge.setStyle("-fx-font-size: " + (size * 0.44) + "px; -fx-text-fill: #FFD43B; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "rust":
            case "rs":
                base.setFill(Color.web("#DEA584"));
                badge.setText("Rs");
                badge.setStyle("-fx-font-size: " + (size * 0.44) + "px; -fx-text-fill: #1E1F22; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "shell":
            case "bash":
            case "sh":
                base.setFill(Color.web("#4EAA25"));
                badge.setText(">_");
                badge.setStyle("-fx-font-size: " + (size * 0.46) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "sql":
                base.setFill(Color.web("#336791"));
                badge.setText("SQL");
                badge.setStyle("-fx-font-size: " + (size * 0.38) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "typescript":
            case "ts":
                base.setFill(Color.web("#3178C6"));
                badge.setText("TS");
                badge.setStyle("-fx-font-size: " + (size * 0.44) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "xml":
                base.setFill(Color.web("#F16529"));
                badge.setText("XML");
                badge.setStyle("-fx-font-size: " + (size * 0.36) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "yaml":
            case "yml":
                base.setFill(Color.web("#CB171E"));
                badge.setText("YML");
                badge.setStyle("-fx-font-size: " + (size * 0.34) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "markdown":
            case "md":
                base.setFill(Color.web("#4078C0"));
                badge.setText("M↓");
                badge.setStyle("-fx-font-size: " + (size * 0.44) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            case "text":
            case "txt":
                base.setFill(Color.web("#6C757D"));
                badge.setText("≡");
                badge.setStyle("-fx-font-size: " + (size * 0.58) + "px; -fx-text-fill: white; -fx-font-weight: bold;");
                container.getChildren().addAll(base, badge);
                break;

            default:
                base.setFill(Color.web("#4E5157"));
                badge.setText("📄");
                badge.setStyle("-fx-font-size: " + (size * 0.54) + "px;");
                container.getChildren().addAll(base, badge);
                break;
        }

        return container;
    }
}
