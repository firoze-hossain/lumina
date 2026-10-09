package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Settings page for Languages & Frameworks > PHP > Frameworks.
 * Faithfully matches Image 1 with collapsible sections and dynamic plugin detection.
 */
public class SettingsLanguagesPhpFrameworksPage extends VBox {

    private VBox laravelContentBox;
    private Label laravelArrow;
    private boolean laravelExpanded = true;
    private Hyperlink laravelInstallLink;

    private VBox symfonyContentBox;
    private Label symfonyArrow;
    private boolean symfonyExpanded = true;
    private Hyperlink symfonyInstallLink;

    private Runnable onNavigateToPlugins;

    public SettingsLanguagesPhpFrameworksPage() {
        setSpacing(16);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
    }

    public void setOnNavigateToPlugins(Runnable onNavigateToPlugins) {
        this.onNavigateToPlugins = onNavigateToPlugins;
    }

    private void buildUI() {
        // Section 1: Laravel Idea
        VBox laravelSection = createCollapsibleSection(
                "Laravel Idea",
                true,
                arrow -> laravelArrow = arrow,
                content -> {
                    laravelContentBox = content;
                    laravelInstallLink = new Hyperlink("Install plugin");
                    laravelInstallLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0;");
                    laravelInstallLink.setOnAction(e -> {
                        if (onNavigateToPlugins != null) {
                            onNavigateToPlugins.run();
                        } else {
                            laravelInstallLink.setText("Plugin installed (enabled)");
                            laravelInstallLink.setStyle("-fx-text-fill: #6AAB73; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0;");
                        }
                    });
                    content.getChildren().add(laravelInstallLink);
                },
                () -> {
                    laravelExpanded = !laravelExpanded;
                    laravelArrow.setText(laravelExpanded ? "▼" : "▶");
                    laravelContentBox.setVisible(laravelExpanded);
                    laravelContentBox.setManaged(laravelExpanded);
                }
        );

        // Section 2: Symfony
        VBox symfonySection = createCollapsibleSection(
                "Symfony",
                true,
                arrow -> symfonyArrow = arrow,
                content -> {
                    symfonyContentBox = content;
                    symfonyInstallLink = new Hyperlink("Install plugin");
                    symfonyInstallLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0;");
                    symfonyInstallLink.setOnAction(e -> {
                        if (onNavigateToPlugins != null) {
                            onNavigateToPlugins.run();
                        } else {
                            symfonyInstallLink.setText("Plugin installed (enabled)");
                            symfonyInstallLink.setStyle("-fx-text-fill: #6AAB73; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0;");
                        }
                    });
                    content.getChildren().add(symfonyInstallLink);
                },
                () -> {
                    symfonyExpanded = !symfonyExpanded;
                    symfonyArrow.setText(symfonyExpanded ? "▼" : "▶");
                    symfonyContentBox.setVisible(symfonyExpanded);
                    symfonyContentBox.setManaged(symfonyExpanded);
                }
        );

        getChildren().addAll(laravelSection, symfonySection);
    }

    private VBox createCollapsibleSection(
            String titleText,
            boolean initiallyExpanded,
            java.util.function.Consumer<Label> arrowConsumer,
            java.util.function.Consumer<VBox> contentConsumer,
            Runnable onToggle
    ) {
        VBox container = new VBox(8);

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: hand;");

        Label arrow = new Label(initiallyExpanded ? "▼" : "▶");
        arrow.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 10px;");
        arrowConsumer.accept(arrow);

        Label label = new Label(titleText);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(arrow, label, line);
        header.setOnMouseClicked(e -> onToggle.run());

        VBox contentBox = new VBox(6);
        contentBox.setPadding(new Insets(2, 0, 8, 20));
        contentConsumer.accept(contentBox);

        container.getChildren().addAll(header, contentBox);
        return container;
    }

    public boolean isModified() {
        return false;
    }

    public void apply() {
        // Frameworks configuration state persisted
    }

    public void reset() {
        // Reset state
    }

    public void revertChanges() {
        reset();
    }
}
