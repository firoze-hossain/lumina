//package dev.lumina.ui;
//
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.layout.*;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextFlow;
//
///**
// * IntelliJ-style Editor settings page.
// * Left: Editor category tree. Right: Description and settings.
// */
//public class SettingsEditorPage extends BorderPane {
//
//    private final VBox contentArea = new VBox(14);
//    private final Label pageTitle = new Label("Editor");
//    private final Label description = new Label();
//
//    public SettingsEditorPage() {
//        getStyleClass().add("settings-page");
//        setPadding(new Insets(16, 20, 16, 20));
//
//        // ---- Left: Editor category tree ----
//        TreeView<String> editorTree = buildEditorTree();
//        editorTree.setPrefWidth(220);
//        editorTree.setMinWidth(200);
//        editorTree.getStyleClass().add("settings-tree");
//        editorTree.setShowRoot(false);
//
//        // Expand first level
//        for (TreeItem<String> item : editorTree.getRoot().getChildren()) {
//            item.setExpanded(true);
//        }
//
//        // ---- Right: Content panel ----
//        VBox rightPanel = new VBox(16);
//        rightPanel.setPadding(new Insets(0, 0, 0, 20));
//        rightPanel.getStyleClass().add("settings-page");
//
//        // Title
//        pageTitle.getStyleClass().add("settings-page-title");
//
//        // Description
//        description.getStyleClass().add("settings-hint");
//        description.setWrapText(true);
//        description.setText("Personalize source code appearance by changing fonts, highlighting styles, indents, etc. Customize the Editor from line numbers, caret placement and tabs to source code inspections, setting up templates and file encodings.");
//
//        // Content area for sub-pages
//        contentArea.setPadding(new Insets(8, 0, 0, 0));
//
//        rightPanel.getChildren().addAll(pageTitle, description, contentArea);
//
//        // ---- Assemble ----
//        setLeft(editorTree);
//        setCenter(rightPanel);
//
//        // Initial selection: Editor
//        TreeItem<String> editorItem = findItem(editorTree.getRoot(), "Editor");
//        if (editorItem != null) {
//            editorTree.getSelectionModel().select(editorItem);
//        }
//
//        // When tree selection changes, update the page
//        editorTree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
//            if (selected != null) {
//                showEditorPage(selected.getValue());
//            }
//        });
//    }
//
//    private TreeView<String> buildEditorTree() {
//        TreeItem<String> root = new TreeItem<>("Editor");
//        root.setExpanded(true);
//
//        // Editor sub-items
//        TreeItem<String> general = new TreeItem<>("General");
//        TreeItem<String> codeEditing = new TreeItem<>("Code Editing");
//        TreeItem<String> font = new TreeItem<>("Font");
//        TreeItem<String> colorScheme = new TreeItem<>("Color Scheme");
//        TreeItem<String> codeStyle = new TreeItem<>("Code Style");
//        TreeItem<String> inspections = new TreeItem<>("Inspections");
//        TreeItem<String> fileTemplates = new TreeItem<>("File and Code Templates");
//        TreeItem<String> fileEncodings = new TreeItem<>("File Encodings");
//        TreeItem<String> liveTemplates = new TreeItem<>("Live Templates");
//        TreeItem<String> fileTypes = new TreeItem<>("File Types");
//        TreeItem<String> copyright = new TreeItem<>("Copyright");
//        TreeItem<String> inlayHints = new TreeItem<>("Inlay Hints");
//        TreeItem<String> duplicates = new TreeItem<>("Duplicates");
//        TreeItem<String> emmet = new TreeItem<>("Emmet");
//        TreeItem<String> intentions = new TreeItem<>("Intentions");
//        TreeItem<String> languageInjections = new TreeItem<>("Language Injections");
//        TreeItem<String> naturalLanguages = new TreeItem<>("Natural Languages");
//        TreeItem<String> readerMode = new TreeItem<>("Reader Mode");
//        TreeItem<String> textMateBundles = new TreeItem<>("TextMate Bundles");
//        TreeItem<String> todo = new TreeItem<>("TODO");
//
//        root.getChildren().addAll(
//            general, codeEditing, font, colorScheme, codeStyle,
//            inspections, fileTemplates, fileEncodings, liveTemplates,
//            fileTypes, copyright, inlayHints, duplicates, emmet,
//            intentions, languageInjections, naturalLanguages,
//            readerMode, textMateBundles, todo
//        );
//
//        TreeView<String> tree = new TreeView<>(root);
//        tree.setShowRoot(false);
//        tree.getStyleClass().add("settings-tree");
//
//        return tree;
//    }
//
//    private TreeItem<String> findItem(TreeItem<String> root, String text) {
//        if (root.getValue() != null && root.getValue().equals(text)) {
//            return root;
//        }
//        for (TreeItem<String> child : root.getChildren()) {
//            TreeItem<String> found = findItem(child, text);
//            if (found != null) return found;
//        }
//        return null;
//    }
//
////    private void showEditorPage(String pageName) {
////        contentArea.getChildren().clear();
////
////        if ("Editor".equals(pageName)) {
////            showEditorOverview();
////        } else {
////            showPlaceholderPage(pageName);
////        }
////    }
//public void showEditorPage(String pageName) {
//    contentArea.getChildren().clear();
//
//    // Update the title based on selection
//    if ("Editor".equals(pageName)) {
//        pageTitle.setText("Editor");
//    } else {
//        pageTitle.setText("Editor > " + pageName);
//    }
//
//    if ("Editor".equals(pageName)) {
//        showEditorOverview();
//    } else if ("General".equals(pageName)) {
//        showGeneralPage();
//    } else {
//        showPlaceholderPage(pageName);
//    }
//}
//
//    private void showEditorOverview() {
//        // This is the main Editor page with the description
//        // The description is already shown in the right panel
//        // Just show a placeholder or additional content
//        Label info = new Label("Select a category from the left to configure specific editor settings.");
//        info.getStyleClass().add("settings-hint");
//        info.setWrapText(true);
//        info.setPadding(new Insets(20, 0, 0, 0));
//        contentArea.getChildren().add(info);
//    }
//
//    private void showPlaceholderPage(String pageName) {
//        Label title = new Label(pageName);
//        title.getStyleClass().add("settings-section");
//
//        Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
//        placeholder.getStyleClass().add("settings-placeholder");
//        placeholder.setWrapText(true);
//
//        VBox box = new VBox(12, title, placeholder);
//        box.setPadding(new Insets(8, 0, 0, 0));
//        contentArea.getChildren().add(box);
//    }
//    private void showGeneralPage() {
//        SettingsEditorGeneralPage page = new SettingsEditorGeneralPage();
//        contentArea.getChildren().add(page);
//    }
//}

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
//    public void showEditorPage(String pageName) {
//        contentArea.getChildren().clear();
//
//        // Update the title based on selection
//        if ("Editor".equals(pageName)) {
//            pageTitle.setText("Editor");
//        } else {
//            pageTitle.setText("Editor > " + pageName);
//        }
//
//        if ("Editor".equals(pageName)) {
//            showEditorOverview();
//        } else if ("General".equals(pageName)) {
//            showGeneralPage();
//        } else {
//            showPlaceholderPage(pageName);
//        }
//    }
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
        } else if (pageName.equals("General") ||
                pageName.equals("Auto Import") ||
                pageName.equals("Appearance") ||
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
        }
        // 🔴 FIX: Handle Smart Keys and all its sub-items
        else if (pageName.equals("Smart Keys") ||
                pageName.equals("YAML") ||
                pageName.equals("HTML/CSS") ||
                pageName.equals("JSON") ||
                pageName.equals("Rust") ||
                pageName.equals("Markdown") ||
                pageName.equals("SQL") ||
                pageName.equals("JavaScript")) {
            showSmartKeysPage();
        }
        else {
            showPlaceholderPage(pageName);
        }
    }
    // 🔴 ADD: Smart Keys page method
    private void showSmartKeysPage() {
        SettingsSmartKeysPage page = new SettingsSmartKeysPage();
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