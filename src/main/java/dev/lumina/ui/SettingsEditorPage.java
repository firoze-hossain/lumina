package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

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
        } else if (pageName.equals("Breadcrumbs")) {
            showBreadcrumbsPage();
        } else if (pageName.equals("Code Completion")) {
            showCodeCompletionPage();
        } else if (pageName.equals("Code Folding")) {
            showCodeFoldingPage();
        } else if (pageName.equals("Console")) {
            showConsolePage();
        } else if (pageName.equals("Editor Tabs")) {
            showEditorTabsPage();
        } else if (pageName.equals("Gutter Icons")) {
            showGutterIconsPage();
        } else if (pageName.equals("Inline Completion")) {
            showInlineCompletionPage();
        } else if (pageName.equals("Postfix Completion")) {
            showPostfixCompletionPage();
        } else if (pageName.equals("Sticky Lines")) {
            showStickyLinesPage();
        } else if (pageName.equals("YAML")) {
            showSmartKeysYAMLPage();
        } else if (pageName.equals("HTML/CSS")) {
            showSmartKeysHTMLCSSPage();
        } else if (pageName.equals("JSON")) {
            showSmartKeysJSONPage();
        } else if (pageName.equals("Rust")) {
            showSmartKeysRustPage();
        } else if (pageName.equals("Markdown")) {
            showSmartKeysMarkdownPage();
        } else if (pageName.equals("SQL")) {
            showSmartKeysSQLPage();
        } else if (pageName.equals("JavaScript")) {
            showSmartKeysJavaScriptPage();
        } else if (pageName.equals("Code Editing")) {
            showCodeEditingPage();
        } else if (pageName.equals("Font")) {
            showFontPage();
        } else if (pageName.equals("Inspections")) {
            showInspectionsPage();
        } else if (pageName.equals("Color Scheme")) {
            showColorSchemePage();
        } else if (pageName.equals("File and Code Templates")) {
            showFileAndCodeTemplatesPage();
        } else if (pageName.equals("File Encodings")) {
            showFileEncodingsPage();
        } else if (pageName.equals("Live Templates")) {
            showLiveTemplatesPage();
        } else if (pageName.equals("File Types")) {
            showFileTypesPage();
        } else if (pageName.equals("Copyright")) {
            showCopyrightPage();
        } else if (pageName.equals("Copyright Profiles")) {
            showCopyrightProfilesPage();
        } else if (pageName.equals("Formatting")) {
            showCopyrightFormattingPage();
        } else if (pageName.equals("CSS") ||
                pageName.equals("DTD") ||
                pageName.equals("Groovy") ||
                pageName.equals("HTML") ||
                pageName.equals("Java") ||
                pageName.equals("JavaScript") ||
                pageName.equals("JSP") ||
                pageName.equals("JSPX") ||
                pageName.equals("Kotlin") ||
                pageName.equals("Less") ||
                pageName.equals("PostCSS") ||
                pageName.equals("Properties") ||
                pageName.equals("Rust") ||
                pageName.equals("Sass") ||
                pageName.equals("SCSS") ||
                pageName.equals("Shell Script") ||
                pageName.equals("SPI") ||
                pageName.equals("SQL") ||
                pageName.equals("SVG") ||
                pageName.equals("TypeScript") ||
                pageName.equals("Vue template") ||
                pageName.equals("XHTML") ||
                pageName.equals("XML")) {
            showCopyrightFormattingSubPage(pageName);
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

    private void showCopyrightProfilesPage() {
        SettingsCopyrightProfilesPage page = new SettingsCopyrightProfilesPage();
        contentArea.getChildren().add(page);
    }

    private void showCopyrightPage() {
        SettingsCopyrightPage page = new SettingsCopyrightPage();
        contentArea.getChildren().add(page);
    }

    // In SettingsEditorPage.java, update the showCopyrightFormattingSubPage method:

    private void showCopyrightFormattingSubPage(String pageName) {
        // Show the specific page based on the language
        if (pageName.equals("CSS")) {
            SettingsCopyrightFormattingCSSPage page = new SettingsCopyrightFormattingCSSPage();
            // Set the title with the language name
            pageTitle.setText("Editor > Copyright > Formatting > CSS");
            contentArea.getChildren().add(page);
        } else if ("DTD".equals(pageName)) {
            SettingsCopyrightFormattingDTDPage page = new SettingsCopyrightFormattingDTDPage();
            pageTitle.setText("Editor > Copyright > Formatting > DTD");
            contentArea.getChildren().add(page);
        }else if ("Groovy".equals(pageName)) {
            SettingsCopyrightFormattingGroovyPage page = new SettingsCopyrightFormattingGroovyPage();
            pageTitle.setText("Editor > Copyright > Formatting > Groovy");
            contentArea.getChildren().add(page);
        } else if ("HTML".equals(pageName)) {
            SettingsCopyrightFormattingHTMLPage page = new SettingsCopyrightFormattingHTMLPage();
            pageTitle.setText("Editor > Copyright > Formatting > HTML");
            contentArea.getChildren().add(page);
        }else if ("Java".equals(pageName)) {
            SettingsCopyrightFormattingJavaPage page = new SettingsCopyrightFormattingJavaPage();
            pageTitle.setText("Editor > Copyright > Formatting > Java");
            contentArea.getChildren().add(page);
        } else if ("JavaScript".equals(pageName)) {
            SettingsCopyrightFormattingJavaScriptPage page = new SettingsCopyrightFormattingJavaScriptPage();
            pageTitle.setText("Editor > Copyright > Formatting > JavaScript");
            contentArea.getChildren().add(page);
        } else if ("JSP".equals(pageName)) {
            SettingsCopyrightFormattingJSPPage page = new SettingsCopyrightFormattingJSPPage();
            pageTitle.setText("Editor > Copyright > Formatting > JSP");
            contentArea.getChildren().add(page);
        } else if ("JSPX".equals(pageName)) {
            SettingsCopyrightFormattingJSPXPage page = new SettingsCopyrightFormattingJSPXPage();
            pageTitle.setText("Editor > Copyright > Formatting > JSPX");
            contentArea.getChildren().add(page);
        } else if ("Kotlin".equals(pageName)) {
            SettingsCopyrightFormattingKotlinPage page = new SettingsCopyrightFormattingKotlinPage();
            pageTitle.setText("Editor > Copyright > Formatting > Kotlin");
            contentArea.getChildren().add(page);
        }else if ("Less".equals(pageName)) {
            SettingsCopyrightFormattingLessPage page = new SettingsCopyrightFormattingLessPage();
            pageTitle.setText("Editor > Copyright > Formatting > Less");
            contentArea.getChildren().add(page);
        } else if ("PostCSS".equals(pageName)) {
            SettingsCopyrightFormattingPostCSSPage page = new SettingsCopyrightFormattingPostCSSPage();
            pageTitle.setText("Editor > Copyright > Formatting > PostCSS");
            contentArea.getChildren().add(page);
        } else if ("Properties".equals(pageName)) {
            SettingsCopyrightFormattingPropertiesPage page = new SettingsCopyrightFormattingPropertiesPage();
            pageTitle.setText("Editor > Copyright > Formatting > Properties");
            contentArea.getChildren().add(page);
        } else if ("Rust".equals(pageName)) {
            SettingsCopyrightFormattingRustPage page = new SettingsCopyrightFormattingRustPage();
            pageTitle.setText("Editor > Copyright > Formatting > Rust");
            contentArea.getChildren().add(page);
        } else {
            // For other languages, show the generic formatting page
            SettingsCopyrightFormattingPage page = new SettingsCopyrightFormattingPage();
            contentArea.getChildren().add(page);
        }
    }

    private void showCopyrightFormattingPage() {
        SettingsCopyrightFormattingPage page = new SettingsCopyrightFormattingPage();
        contentArea.getChildren().add(page);
    }

    private void showFileEncodingsPage() {
        SettingsFileEncodingsPage page = new SettingsFileEncodingsPage();
        contentArea.getChildren().add(page);
    }

    private void showFileTypesPage() {
        SettingsFileTypesPage page = new SettingsFileTypesPage();
        contentArea.getChildren().add(page);
    }

    private void showFileAndCodeTemplatesPage() {
        SettingsFileAndCodeTemplatesPage page = new SettingsFileAndCodeTemplatesPage();
        contentArea.getChildren().add(page);
    }

    private void showLiveTemplatesPage() {
        SettingsLiveTemplatesPage page = new SettingsLiveTemplatesPage();
        contentArea.getChildren().add(page);
    }

    private void showInspectionsPage() {
        SettingsInspectionsPage page = new SettingsInspectionsPage();
        contentArea.getChildren().add(page);
    }

    private void showCodeEditingPage() {
        SettingsCodeEditingPage page = new SettingsCodeEditingPage();
        contentArea.getChildren().add(page);
    }

    // In SettingsEditorPage.java, add this method:
    private void showColorSchemePage() {
        SettingsColorSchemePage page = new SettingsColorSchemePage();
        contentArea.getChildren().add(page);
    }

    private void showFontPage() {
        SettingsFontPage page = new SettingsFontPage();
        contentArea.getChildren().add(page);
    }

    private void showStickyLinesPage() {
        SettingsStickyLinesPage page = new SettingsStickyLinesPage();
        contentArea.getChildren().add(page);
    }

    private void showSmartKeysHTMLCSSPage() {
        SettingsSmartKeysHTMLCSSPage page = new SettingsSmartKeysHTMLCSSPage();
        contentArea.getChildren().add(page);
    }

    private void showSmartKeysJSONPage() {
        SettingsSmartKeysJSONPage page = new SettingsSmartKeysJSONPage();
        contentArea.getChildren().add(page);
    }

    private void showSmartKeysSQLPage() {
        SettingsSmartKeysSQLPage page = new SettingsSmartKeysSQLPage();
        contentArea.getChildren().add(page);
    }

    private void showSmartKeysJavaScriptPage() {
        SettingsSmartKeysJavaScriptPage page = new SettingsSmartKeysJavaScriptPage();
        contentArea.getChildren().add(page);
    }

    private void showSmartKeysRustPage() {
        SettingsSmartKeysRustPage page = new SettingsSmartKeysRustPage();
        contentArea.getChildren().add(page);
    }

    private void showSmartKeysMarkdownPage() {
        SettingsSmartKeysMarkdownPage page = new SettingsSmartKeysMarkdownPage();
        contentArea.getChildren().add(page);
    }

    private void showSmartKeysYAMLPage() {
        SettingsSmartKeysYAMLPage page = new SettingsSmartKeysYAMLPage();
        contentArea.getChildren().add(page);
    }

    private void showPostfixCompletionPage() {
        SettingsPostfixCompletionPage page = new SettingsPostfixCompletionPage();
        contentArea.getChildren().add(page);
    }

    private void showInlineCompletionPage() {
        SettingsInlineCompletionPage page = new SettingsInlineCompletionPage();
        contentArea.getChildren().add(page);
    }

    private void showGutterIconsPage() {
        SettingsGutterIconsPage page = new SettingsGutterIconsPage();
        contentArea.getChildren().add(page);
    }

    private void showEditorTabsPage() {
        SettingsEditorTabsPage page = new SettingsEditorTabsPage();
        contentArea.getChildren().add(page);
    }

    private void showConsolePage() {
        SettingsConsolePage page = new SettingsConsolePage();
        contentArea.getChildren().add(page);
    }

    private void showCodeFoldingPage() {
        SettingsCodeFoldingPage page = new SettingsCodeFoldingPage();
        contentArea.getChildren().add(page);
    }

    private void showBreadcrumbsPage() {
        SettingsBreadcrumbsPage page = new SettingsBreadcrumbsPage();
        contentArea.getChildren().add(page);
    }

    private void showCodeCompletionPage() {
        SettingsCodeCompletionPage page = new SettingsCodeCompletionPage();
        contentArea.getChildren().add(page);
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