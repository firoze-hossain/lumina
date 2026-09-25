package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Editor > Color Scheme settings page.
 * Matches 1:1 with reference screenshots media_1790339196850.png and media_1790339213998.png:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Header label: "Configure colors and the font for source code and console output:".
 * - Clean scrollable list of 69 subcategory links navigating directly to their settings pages.
 */
public class SettingsColorSchemePage extends VBox {

    public static final List<String> SCHEME_CATEGORIES = List.of(
            "General",
            "Language Defaults",
            "Color Scheme Font",
            "Console Font",
            "Code With Me",
            "Console Colors",
            "Debugger",
            "Diff & Merge",
            "JVM Logging",
            "User-Defined File Types",
            "VCS",
            "Java",
            "Angular Template",
            "Context Free Grammar",
            "CSS",
            "Data Editor and Viewer",
            "Database",
            "Diagrams",
            "Dockerfile",
            "EditorConfig",
            "ERB",
            "FreeMarker",
            "GitLab CI Expression",
            "Go",
            "Gradle Declarative Configuration",
            "Groovy",
            "HTML",
            "HTTP Request",
            "JavaScript",
            "JPA/Hibernate QL",
            "JSON",
            "JSONPath",
            "JSP",
            "Jupyter Notebooks",
            "Kotlin",
            "Kubernetes",
            "Less",
            "Lombok Config",
            "Markdown",
            "Micronaut EL",
            "MongoDB JSON",
            "PHP",
            "plan9_x86",
            "PostCSS",
            "Properties",
            "Protocol Buffer",
            "Protocol Buffer Text",
            "Python",
            "Qute",
            "RDoc",
            "RegExp",
            "Ruby",
            "Rust",
            "Sass/SCSS",
            "Scala",
            "Shell Script",
            "Smarty",
            "Spring EL",
            "SQL",
            "Table Diff",
            "TOML",
            "TypeScript",
            "Velocity",
            "XML",
            "XPath",
            "XSLT",
            "YAML",
            "By Scope",
            "Images"
    );

    private final ColorSchemeHeaderBar headerBar = new ColorSchemeHeaderBar();
    private Consumer<String> onNavigate;
    private Runnable onModifiedListener;

    private String originalScheme;

    public SettingsColorSchemePage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);

        this.originalScheme = EditorColorSchemeSettings.getInstance().getActiveSchemeName();

        buildUi();
        setupListeners();
    }

    private void buildUi() {
        Label descLabel = new Label("Configure colors and the font for source code and console output:");
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 6 0 2 0;");

        VBox linksBox = new VBox(4);
        linksBox.setStyle("-fx-background-color: transparent;");

        for (String cat : SCHEME_CATEGORIES) {
            Hyperlink link = new Hyperlink(cat);
            link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-padding: 2 0 2 0; -fx-border-color: transparent; -fx-underline: false;");
            link.setOnMouseEntered(e -> link.setUnderline(true));
            link.setOnMouseExited(e -> link.setUnderline(false));
            link.setOnAction(e -> {
                if (onNavigate != null) {
                    onNavigate.accept(cat);
                }
            });
            linksBox.getChildren().add(link);
        }

        ScrollPane scrollPane = new ScrollPane(linksBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(headerBar, descLabel, scrollPane);
    }

    private void setupListeners() {
        headerBar.setOnSchemeChanged(scheme -> notifyModified());
    }

    public void setOnNavigate(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public boolean isModified() {
        String current = EditorColorSchemeSettings.getInstance().getActiveSchemeName();
        return !java.util.Objects.equals(current, originalScheme);
    }

    public void apply() {
        EditorColorSchemeSettings.getInstance().save();
        this.originalScheme = EditorColorSchemeSettings.getInstance().getActiveSchemeName();
    }

    public void reset() {
        EditorColorSchemeSettings.getInstance().setActiveSchemeName(originalScheme);
        headerBar.refreshSchemes();
    }

    public ColorSchemeHeaderBar getHeaderBar() {
        return headerBar;
    }
}