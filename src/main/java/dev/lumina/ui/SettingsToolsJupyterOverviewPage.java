package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Tools > Jupyter hub / category overview page matching Image 3.
 */
public class SettingsToolsJupyterOverviewPage extends VBox {

    private final Consumer<String> onNavigate;
    private Hyperlink generalLink;
    private Hyperlink serversLink;
    private Hyperlink vcsLink;

    public SettingsToolsJupyterOverviewPage() {
        this(null);
    }

    public SettingsToolsJupyterOverviewPage(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        buildUI();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // Header description
        Label description = new Label("Configure Jupyter Notebook settings: customize the editor, choose execution options, and manage server and VCS integration.");
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Category Links
        VBox linksBox = new VBox(8);
        linksBox.setPadding(new Insets(8, 0, 0, 0));

        generalLink = createCategoryLink("Jupyter General");
        serversLink = createCategoryLink("Jupyter Servers");
        vcsLink = createCategoryLink("Jupyter VCS");

        linksBox.getChildren().addAll(generalLink, serversLink, vcsLink);

        getChildren().addAll(description, linksBox);
    }

    private Hyperlink createCategoryLink(String text) {
        Hyperlink link = new Hyperlink(text);
        link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;");
        link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
        link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
        link.setOnAction(e -> {
            if (onNavigate != null) {
                onNavigate.accept(text);
            }
        });
        return link;
    }

    public Hyperlink getGeneralLink() {
        return generalLink;
    }

    public Hyperlink getServersLink() {
        return serversLink;
    }

    public Hyperlink getVcsLink() {
        return vcsLink;
    }
}
