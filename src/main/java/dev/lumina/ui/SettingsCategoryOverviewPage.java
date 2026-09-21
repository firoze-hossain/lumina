package dev.lumina.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Dynamic Overview Page for parent settings categories matching IntelliJ IDEA.
 * Displays category description and dynamically generates clickable links for each child node.
 */
public class SettingsCategoryOverviewPage extends VBox {

    private static final Map<String, String> CATEGORY_DESCRIPTIONS = new HashMap<>();

    static {
        CATEGORY_DESCRIPTIONS.put("Appearance & Behavior",
                "Customize IDE appearance and behavior: change themes and font size, tune the keymap, " +
                "configure plugins and system settings, such as password policies, HTTP proxy, and updates.");
        CATEGORY_DESCRIPTIONS.put("Editor",
                "Configure code editor behavior, fonts, color schemes, code style, inspections, templates, " +
                "and text editing preferences.");
        CATEGORY_DESCRIPTIONS.put("System Settings",
                "Configure system-level preferences including data sharing, date formats, HTTP proxy, " +
                "passwords, server certificates, and updates.");
        CATEGORY_DESCRIPTIONS.put("Tools",
                "Configure external tools, terminal, database connectivity, AI assistant, and developer integrations.");
        CATEGORY_DESCRIPTIONS.put("Build, Execution, Deployment",
                "Configure build tools, compiler options, deployment servers, and application run configurations.");
        CATEGORY_DESCRIPTIONS.put("Languages & Frameworks",
                "Configure language support, framework integrations, schemas, and language server protocols.");
        CATEGORY_DESCRIPTIONS.put("Version Control",
                "Configure version control systems, commit settings, directory mappings, and repository options.");
        CATEGORY_DESCRIPTIONS.put("Keymap",
                "Configure keyboard shortcuts and keymap schemes for editor actions, tool windows, and menus.");
    }

    public SettingsCategoryOverviewPage(TreeItem<String> categoryItem, Consumer<TreeItem<String>> onSelectChild) {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");

        String categoryName = categoryItem.getValue();

        // 1. Description label
        String descText = CATEGORY_DESCRIPTIONS.getOrDefault(categoryName,
                "Configure " + categoryName + " preferences and options.");
        Label descLabel = new Label(descText);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(750);
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-line-spacing: 3px;");

        // 2. Links Container
        VBox linksContainer = new VBox(6);
        linksContainer.setAlignment(Pos.TOP_LEFT);
        linksContainer.setPadding(new Insets(8, 0, 0, 0));

        for (TreeItem<String> child : categoryItem.getChildren()) {
            Hyperlink link = new Hyperlink(child.getValue());
            link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: false;");
            link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: true;"));
            link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: false;"));
            link.setOnAction(e -> {
                if (onSelectChild != null) {
                    onSelectChild.accept(child);
                }
            });
            linksContainer.getChildren().add(link);
        }

        getChildren().addAll(descLabel, linksContainer);
    }
}
