package dev.lumina.ui;

import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Tools > Database overview settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsDatabasePage extends VBox {

    public static final List<String> DATABASE_SUBPAGES = List.of(
            "Query Execution",
            "Data Editor and Viewer",
            "Other"
    );

    private final Consumer<String> onNavigate;

    public SettingsDatabasePage() {
        this(null);
    }

    public SettingsDatabasePage(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        buildUI();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Category Description
        Label descLabel = new Label("Specify database console behavior, configure data views, and extraction options. Add custom parameter patterns in SQL queries and specify SQL dialects mapping for files.");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(850);
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-line-spacing: 3px;");

        // 2. Links Container
        VBox linksContainer = new VBox(6);
        linksContainer.setAlignment(Pos.TOP_LEFT);
        linksContainer.setPadding(new Insets(8, 0, 0, 0));

        for (String subPage : DATABASE_SUBPAGES) {
            Hyperlink link = new Hyperlink(subPage);
            link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: false;");
            link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: true;"));
            link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: false;"));
            link.setOnAction(e -> {
                if (onNavigate != null) {
                    onNavigate.accept(subPage);
                }
            });
            linksContainer.getChildren().add(link);
        }

        getChildren().addAll(descLabel, linksContainer);
    }
}