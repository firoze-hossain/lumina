package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Separate UI page for Editor -> Color Scheme -> General.
 * Matches the polished Color Scheme General layout from the screenshot.
 */
public class SettingsColorSchemeGeneralPage extends VBox {

    public SettingsColorSchemeGeneralPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(16);

        Label title = new Label("General");
        title.getStyleClass().add("settings-page-title");

        // Scheme row with action links
        HBox schemeRow = new HBox(12);
        schemeRow.setAlignment(Pos.CENTER_LEFT);

        Label schemeLabel = new Label("Scheme:");
        schemeLabel.getStyleClass().add("settings-label");

        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll("Island's Dark Theme default", "Darcula", "IntelliJ Light");
        schemeCombo.getSelectionModel().selectFirst();
        schemeCombo.getStyleClass().add("settings-combo");
        schemeCombo.setPrefWidth(260);

        Hyperlink themeLink = new Hyperlink("Change IDE Theme...");
        themeLink.getStyleClass().add("settings-link");

        schemeRow.getChildren().addAll(schemeLabel, schemeCombo, themeLink);

        HBox buttonRow = new HBox(8);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        buttonRow.setPadding(new Insets(4, 0, 0, 0));
        buttonRow.getChildren().addAll(
                createAccentButton("Export..."),
                createAccentButton("Import..."),
                createAccentButton("Duplicate..."),
                createResetButton("Reset")
        );

        HBox contentRow = new HBox(16);
        contentRow.setAlignment(Pos.TOP_LEFT);
        contentRow.setSpacing(16);

        VBox categoriesPane = buildCategoriesPane();
        VBox previewPane = buildPreviewPane();

        contentRow.getChildren().addAll(categoriesPane, previewPane);

        getChildren().addAll(title, schemeRow, buttonRow, contentRow);
    }

    private Button createAccentButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("dialog-secondary");
        button.setPrefWidth(120);
        return button;
    }

    private Button createResetButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("dialog-secondary");
        button.setStyle("-fx-border-color: #E5534B; -fx-text-fill: #E5534B;");
        button.setPrefWidth(120);
        return button;
    }

    private VBox buildCategoriesPane() {
        VBox outer = new VBox();
        outer.getStyleClass().add("color-scheme-categories");
        outer.setPadding(new Insets(12));
        outer.setSpacing(8);
        outer.setPrefWidth(360);

        Accordion accordion = new Accordion();
        String[] categories = {
                "Code", "Editor", "Errors and Warnings", "Hyperlinks",
                "Identifiers", "Line Coverage", "Live Templates", "Popups and Hints",
                "Preview", "Search Results", "Text"
        };
        for (String category : categories) {
            TitledPane tp = new TitledPane(category, new Pane());
            tp.setCollapsible(true);
            tp.getStyleClass().add("color-scheme-category");
            accordion.getPanes().add(tp);
        }
        accordion.setExpandedPane(accordion.getPanes().get(0));

        outer.getChildren().add(accordion);
        return outer;
    }

    private VBox buildPreviewPane() {
        VBox outer = new VBox(12);
        outer.setPrefWidth(560);

        VBox previewBox = new VBox(10);
        previewBox.getStyleClass().add("color-scheme-preview");
        previewBox.setPadding(new Insets(12));
        previewBox.setSpacing(10);

        Label previewTitle = new Label("Preview");
        previewTitle.getStyleClass().add("settings-section");

        VBox codeArea = new VBox(4);
        codeArea.getStyleClass().add("code-area");
        codeArea.setPadding(new Insets(12));
        codeArea.setSpacing(4);

        Label l1 = new Label("// TODO: Visit JB Web resources:");
        l1.getStyleClass().add("sx-comment");
        Label l2 = new Label("JetBrains Home Page: http://www.jetbrains.com");
        l2.getStyleClass().add("sx-string");
        Label l3 = new Label("ReferenceHyperlink");
        l3.getStyleClass().add("sx-method");
        Label l4 = new Label("Search:");
        l4.getStyleClass().add("sx-keyword");
        Label l5 = new Label("result = \"text, text, text\";");
        l5.getStyleClass().add("sx-string");

        codeArea.getChildren().addAll(l1, l2, l3, new Label(""), l4, l5);
        ScrollPane codeScroll = new ScrollPane(codeArea);
        codeScroll.setFitToWidth(true);
        codeScroll.setPrefViewportHeight(240);
        codeScroll.getStyleClass().add("settings-scroll");

        VBox details = new VBox(8);
        details.setPadding(new Insets(8));
        details.setStyle("-fx-background-color: #171A24; -fx-border-color: #2C3042; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label deleted = new Label("Deleted text");
        deleted.getStyleClass().add("settings-label");
        deleted.setStyle("-fx-text-fill: #E5534B; -fx-strikethrough: true;");

        Label liveTemplate = new Label("Live template: active inactive $VARIABLE$");
        liveTemplate.getStyleClass().add("settings-label");
        Label injected = new Label("Injected language: \\.(gif|jpg|png)$");
        injected.getStyleClass().add("settings-label");

        Label inspectionsTitle = new Label("Code Inspections:");
        inspectionsTitle.getStyleClass().add("settings-label");

        VBox inspections = new VBox(4);
        inspections.getChildren().addAll(
                createInspectionLabel("Error", "#E5534B"),
                createInspectionLabel("Warning", "#D8A657"),
                createInspectionLabel("Weak warning", "#B9BECF"),
                createInspectionLabel("Unused symbol", "#697089"),
                createInspectionLabel("Unknown symbol", "#E5534B"),
                createInspectionLabel("Runtime problem", "#F2D9A6")
        );

        details.getChildren().addAll(deleted, liveTemplate, injected, inspectionsTitle, inspections);

        previewBox.getChildren().addAll(previewTitle, codeScroll, details);
        outer.getChildren().add(previewBox);
        return outer;
    }

    private Label createInspectionLabel(String text, String color) {
        Label label = new Label(text);
        label.getStyleClass().add("settings-label");
        label.setStyle("-fx-text-fill: " + color + ";");
        return label;
    }
}
