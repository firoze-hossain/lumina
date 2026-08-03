package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor settings page.
 * This shows the content for Editor sub-pages directly.
 */
public class SettingsEditorPage extends VBox {

    private final Label pageTitle = new Label("Editor");
    private final Label description = new Label();
    private final VBox contentArea = new VBox(14);

    public SettingsEditorPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);

        // ---- Header ----
        pageTitle.getStyleClass().add("settings-page-title");
        description.getStyleClass().add("settings-hint");
        description.setWrapText(true);
        description.setText("Personalize source code appearance by changing fonts, highlighting styles, indents, etc. Customize the Editor from line numbers, caret placement and tabs to source code inspections, setting up templates and file encodings.");

        // ---- Content area ----
        contentArea.setPadding(new Insets(8, 0, 0, 0));

        // Show the overview initially
        showEditorOverview();

        getChildren().addAll(pageTitle, description, contentArea);
    }

    /**
     * Called from SettingsDialog to show the appropriate sub-page.
     */
    public void showEditorPage(String pageName) {
        contentArea.getChildren().clear();

        // Update the title based on selection
        if ("Editor".equals(pageName)) {
            pageTitle.setText("Editor");
        } else {
            pageTitle.setText("Editor > " + pageName);
        }

        if ("Editor".equals(pageName)) {
            showEditorOverview();
        } else if (pageName.equals("Auto Import")) {
            showAutoImportPage();
        } else if (pageName.equals("Appearance")) {
            showAppearancePage();
        } else if (pageName.equals("General") ||
                pageName.equals("Breadcrumbs") ||
                pageName.equals("Code Completion") ||
                pageName.equals("Code Folding") ||
                pageName.equals("Console") ||
                pageName.equals("Editor Tabs") ||
                pageName.equals("Gutter Icons") ||
                pageName.equals("Inline Completion") ||
                pageName.equals("Postfix Completion") ||
                pageName.equals("Sticky Lines")) {
            showGeneralPage();
        } else if (pageName.equals("Smart Keys") ||
                pageName.equals("YAML") ||
                pageName.equals("HTML/CSS") ||
                pageName.equals("JSON") ||
                pageName.equals("Rust") ||
                pageName.equals("Markdown") ||
                pageName.equals("SQL") ||
                pageName.equals("JavaScript")) {
            showSmartKeysPage();
        } else {
            showPlaceholderPage(pageName);
        }
    }

    private void showEditorOverview() {
        Label info = new Label("Select a category from the left to configure specific editor settings.");
        info.getStyleClass().add("settings-hint");
        info.setWrapText(true);
        info.setPadding(new Insets(20, 0, 0, 0));
        contentArea.getChildren().add(info);
    }

    private void showGeneralPage() {
        SettingsEditorGeneralPage page = new SettingsEditorGeneralPage();
        contentArea.getChildren().add(page);
    }

    private void showAppearancePage() {
        SettingsAppearancePage page = new SettingsAppearancePage();
        // Ensure the page has some height so it's visible
        page.setMinHeight(400);
        contentArea.getChildren().add(page);
    }

    private void showAutoImportPage() {
        SettingsAutoImportPage page = new SettingsAutoImportPage();
        contentArea.getChildren().add(page);
    }

    private void showSmartKeysPage() {
        SettingsSmartKeysPage page = new SettingsSmartKeysPage();
        contentArea.getChildren().add(page);
    }

    private void showPlaceholderPage(String pageName) {
        Label title = new Label(pageName);
        title.getStyleClass().add("settings-section");

        Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
        placeholder.getStyleClass().add("settings-placeholder");
        placeholder.setWrapText(true);

        VBox box = new VBox(12, title, placeholder);
        box.setPadding(new Insets(8, 0, 0, 0));
        contentArea.getChildren().add(box);
    }
}