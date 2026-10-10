package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.AttributesDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Comprehensive Settings dialog with:
 * - Left: Settings category tree with interactive search filtering
 * - Right Top: Dynamic breadcrumbs header (e.g. Appearance & Behavior › Appearance) and ← → navigation history
 * - Right Center: Dynamic page routing (Category overview for parent nodes, SettingsIdeAppearancePage for Appearance, etc.)
 * - Bottom: Status bar with help button and dialog actions (Cancel, Apply, OK)
 */
public class SettingsDialog {

    private final Stage stage;
    private final TreeView<String> tree;
    private final TextField searchField;

    // Header & History navigation
    private final HBox breadcrumbBox = new HBox(6);
    private final Button backButton = new Button("\u2190");
    private final Button forwardButton = new Button("\u2192");
    private final List<TreeItem<String>> navHistory = new ArrayList<>();
    private int navHistoryIndex = -1;
    private boolean isNavigatingHistory = false;

    // Content container
    private final StackPane contentContainer = new StackPane();
    private SettingsIdeAppearancePage currentIdeAppearancePage;
    private SettingsMenusToolbarsPage currentMenusToolbarsPage;
    private SettingsQuickListsPage currentQuickListsPage;
    private SettingsSystemPage currentSystemPage;
    private SettingsEditorGeneralPage currentEditorGeneralPage;
    private SettingsAutoImportPage currentAutoImportPage;
    private SettingsAppearancePage currentEditorAppearancePage;
    private SettingsBreadcrumbsPage currentBreadcrumbsPage;
    private SettingsCodeCompletionPage currentCodeCompletionPage;
    private SettingsCodeFoldingPage currentCodeFoldingPage;
    private SettingsConsolePage currentConsolePage;
    private SettingsEditorTabsPage currentEditorTabsPage;
    private SettingsGutterIconsPage currentGutterIconsPage;
    private SettingsInlineCompletionPage currentInlineCompletionPage;
    private SettingsPostfixCompletionPage currentPostfixCompletionPage;
    private SettingsSmartKeysPage currentSmartKeysPage;
    private SettingsSmartKeysYAMLPage currentSmartKeysYamlPage;
    private SettingsSmartKeysHTMLCSSPage currentSmartKeysHtmlCssPage;
    private SettingsSmartKeysPythonPage currentSmartKeysPythonPage;
    private SettingsSmartKeysJSONPage currentSmartKeysJsonPage;
    private SettingsSmartKeysRustPage currentSmartKeysRustPage;
    private SettingsSmartKeysMarkdownPage currentSmartKeysMarkdownPage;
    private SettingsSmartKeysScalaPage currentSmartKeysScalaPage;
    private SettingsSmartKeysSQLPage currentSmartKeysSqlPage;
    private SettingsSmartKeysRubyPage currentSmartKeysRubyPage;
    private SettingsSmartKeysJavaScriptPage currentSmartKeysJsPage;
    private SettingsSmartKeysPHPPage currentSmartKeysPhpPage;
    private SettingsStickyLinesPage currentStickyLinesPage;
    private SettingsCodeEditingPage currentCodeEditingPage;
    private SettingsInspectionsPage currentInspectionsPage;
    private SettingsFileAndCodeTemplatesPage currentFileAndCodeTemplatesPage;
    private SettingsFileEncodingsPage currentFileEncodingsPage;
    private SettingsLiveTemplatesPage currentLiveTemplatesPage;
    private SettingsFileTypesPage currentFileTypesPage;
    private SettingsCopyrightPage currentCopyrightPage;
    private SettingsCopyrightProfilesPage currentCopyrightProfilesPage;
    private SettingsCopyrightFormattingPage currentCopyrightFormattingPage;
    private final java.util.Map<String, SettingsCopyrightFormattingLanguagePage> languageFormattingPages = new java.util.HashMap<>();
    private SettingsInlayHintsPage currentInlayHintsPage;
    private SettingsDuplicatesPage currentDuplicatesPage;
    private SettingsEmmetPage currentEmmetPage;
    private SettingsEmmetCssPage currentEmmetCssPage;
    private SettingsEmmetHtmlPage currentEmmetHtmlPage;
    private SettingsEmmetJsxPage currentEmmetJsxPage;
    private SettingsPluginsPage currentPluginsPage;
    private SettingsIntentionsPage currentIntentionsPage;
    private SettingsLanguageInjectionsPage currentLanguageInjectionsPage;
    private SettingsLanguageInjectionsAdvancedPage currentLanguageInjectionsAdvancedPage;
    private SettingsNaturalLanguagesPage currentNaturalLanguagesPage;
    private SettingsGrammarAndStylePage currentGrammarAndStylePage;
    private SettingsSpellingPage currentSpellingPage;
    private SettingsReaderModePage currentReaderModePage;
    private SettingsTextMateBundlesPage currentTextMateBundlesPage;
    private SettingsTodoPage currentTodoPage;
    private SettingsPythonDebuggerPage currentPythonDebuggerPage;
    private SettingsDebuggerPage currentDebuggerPage;
    private SettingsDebuggerAsyncStackTracesPage currentDebuggerAsyncStackTracesPage;
    private SettingsDebuggerDataViewsPage currentDebuggerDataViewsPage;
    private SettingsDebuggerDataViewsJavaPage currentDebuggerDataViewsJavaPage;
    private SettingsDebuggerDataViewsTypeRenderersPage currentDebuggerDataViewsTypeRenderersPage;
    private SettingsDebuggerHotSwapPage currentDebuggerHotSwapPage;
    private SettingsDebuggerSteppingPage currentDebuggerSteppingPage;
    private SettingsDebuggerDataViewsJavaScriptPage currentDebuggerDataViewsJavaScriptPage;
    private SettingsDeploymentPage currentDeploymentPage;
    private SettingsDeploymentOptionsPage currentDeploymentOptionsPage;
    private SettingsDockerPage currentDockerPage;
    private SettingsDockerConsolePage currentDockerConsolePage;
    private SettingsDockerRegistryPage currentDockerRegistryPage;
    private SettingsJavaProfilerPage currentJavaProfilerPage;
    private SettingsJavaProfilerFiltersPage currentJavaProfilerFiltersPage;
    private SettingsBuildKubernetesPage currentBuildKubernetesPage;
    private SettingsRemoteJarRepositoriesPage currentRemoteJarRepositoriesPage;
    private SettingsRunTargetsPage currentRunTargetsPage;
    private SettingsLanguagesPHPPage currentLanguagesPhpPage;
    private SettingsLanguagesPhpDebugPage currentLanguagesPhpDebugPage;
    private SettingsLanguagesPhpDebugTemplatesPage currentLanguagesPhpDebugTemplatesPage;
    private SettingsLanguagesPhpDebugDbgpProxyPage currentLanguagesPhpDebugDbgpProxyPage;
    private SettingsLanguagesPhpDebugSkippedPathsPage currentLanguagesPhpDebugSkippedPathsPage;
    private SettingsLanguagesPhpDebugStepFiltersPage currentLanguagesPhpDebugStepFiltersPage;
    private SettingsLanguagesPhpDebugXdebugCloudPage currentLanguagesPhpDebugXdebugCloudPage;
    private SettingsLanguagesPhpServersPage currentLanguagesPhpServersPage;
    private SettingsLanguagesPhpComposerPage currentLanguagesPhpComposerPage;
    private SettingsLanguagesPhpTestFrameworksPage currentLanguagesPhpTestFrameworksPage;
    private SettingsLanguagesPhpQualityToolsPage currentPhpQualityToolsPage;
    private SettingsLanguagesPhpQualityToolsCodeSnifferPage currentPhpCodeSnifferPage;
    private SettingsLanguagesPhpQualityToolsCsFixerPage currentPhpCsFixerPage;
    private SettingsLanguagesPhpQualityToolsLaravelPintPage currentPhpLaravelPintPage;
    private SettingsLanguagesPhpQualityToolsMessDetectorPage currentPhpMessDetectorPage;
    private SettingsLanguagesPhpFrameworksPage currentPhpFrameworksPage;
    private SettingsLanguagesPhpSmartyPage currentPhpSmartyPage;
    private SettingsPythonTemplateLanguagesPage currentPythonTemplateLanguagesPage;
    private SettingsLanguagesGoPage currentLanguagesGoPage;
    private SettingsLanguagesGoGoRootPage currentLanguagesGoGoRootPage;
    private SettingsLanguagesGoGoPathPage currentLanguagesGoGoPathPage;
    private SettingsLanguagesGoModulesPage currentLanguagesGoModulesPage;
    private SettingsLanguagesGoBuildTagsPage currentLanguagesGoBuildTagsPage;
    private SettingsLanguagesGoFormattingFunctionsPage currentLanguagesGoFormattingFunctionsPage;
    private SettingsLanguagesGoImportsPage currentLanguagesGoImportsPage;
    private SettingsRustPage currentRustPage;
    private SettingsRustExternalLintersPage currentRustExternalLintersPage;
    private SettingsRustfmtPage currentRustfmtPage;
    private SettingsLanguagesJavaFXPage currentLanguagesJavaFxPage;
    private SettingsLanguagesJavaScriptPage currentLanguagesJavaScriptPage;
    private SettingsLanguagesJSEsLintPage currentLanguagesJsEsLintPage;
    private SettingsLanguagesJSJsHintPage currentLanguagesJsJsHintPage;
    private SettingsLanguagesJSLibrariesPage currentLanguagesJsLibrariesPage;
    private SettingsLanguagesJSPrettierPage currentLanguagesJsPrettierPage;
    private SettingsLanguagesJSStyledComponentsPage currentLanguagesJsStyledComponentsPage;
    private SettingsLanguagesJSVitePage currentLanguagesJsVitePage;
    private SettingsLanguagesJSWebpackPage currentLanguagesJsWebpackPage;
    private SettingsLanguagesJavaScriptRuntimePage currentLanguagesJavaScriptRuntimePage;
    private SettingsLanguagesJvmLoggingPage currentLanguagesJvmLoggingPage;
    private SettingsLanguagesKotlinPage currentLanguagesKotlinPage;
    private SettingsLanguagesKotlinScriptingPage currentLanguagesKotlinScriptingPage;
    private SettingsLanguagesKtorPage currentLanguagesKtorPage;
    private SettingsLanguagesKubernetesPage currentLanguagesKubernetesPage;
    private SettingsLanguagesLombokPage currentLanguagesLombokPage;
    private SettingsLanguagesMarkdownPage currentLanguagesMarkdownPage;
    private SettingsLanguagesMicronautPage currentLanguagesMicronautPage;
    private SettingsLanguagesOpenAPIPage currentLanguagesOpenApiPage;
    private SettingsLanguagesPlayPage currentLanguagesPlayPage;
    private SettingsLanguagesProtobufPage currentLanguagesProtobufPage;
    private SettingsLanguagesProtobufTextFormatPage currentLanguagesProtobufTextFormatPage;
    private SettingsLanguagesQuarkusPage currentLanguagesQuarkusPage;
    private SettingsLanguagesRbsPage currentLanguagesRbsPage;
    private SettingsLanguagesScalaEditorPage currentLanguagesScalaEditorPage;
    private SettingsLanguagesScalaXRayPage currentLanguagesScalaXRayPage;
    private SettingsLanguagesScalaProjectViewPage currentLanguagesScalaProjectViewPage;
    private SettingsLanguagesScalaPerformancePage currentLanguagesScalaPerformancePage;
    private SettingsLanguagesScalaWorksheetPage currentLanguagesScalaWorksheetPage;
    private SettingsLanguagesScalaBasePackagePage currentLanguagesScalaBasePackagePage;
    private SettingsLanguagesScalaMiscPage currentLanguagesScalaMiscPage;
    private SettingsLanguagesScalaUpdatesPage currentLanguagesScalaUpdatesPage;
    private SettingsLanguagesScalaExtensionsPage currentLanguagesScalaExtensionsPage;
    private SettingsLanguagesSchemasAndDtdsPage currentLanguagesSchemasAndDtdsPage;
    private SettingsLanguagesDefaultXmlSchemasPage currentLanguagesDefaultXmlSchemasPage;
    private SettingsLanguagesJsonSchemaMappingsPage currentLanguagesJsonSchemaMappingsPage;
    private SettingsLanguagesRemoteJsonSchemasPage currentLanguagesRemoteJsonSchemasPage;
    private SettingsLanguagesXmlCatalogPage currentLanguagesXmlCatalogPage;
    private SettingsLanguagesSpringPage currentLanguagesSpringPage;
    private SettingsLanguagesSqlDialectsPage currentLanguagesSqlDialectsPage;
    private SettingsLanguagesSqlResolutionScopesPage currentLanguagesSqlResolutionScopesPage;
    private SettingsLanguagesStyleSheetsDialectsPage currentLanguagesStyleSheetsDialectsPage;
    private SettingsLanguagesStyleSheetsStylelintPage currentLanguagesStyleSheetsStylelintPage;
    private SettingsLanguagesStyleSheetsTailwindPage currentLanguagesStyleSheetsTailwindPage;
    private SettingsLanguagesTablesPage currentLanguagesTablesPage;
    private SettingsLanguagesTemplateDataLanguagesPage currentLanguagesTemplateDataLanguagesPage;
    private SettingsLanguagesTypeScriptPage currentLanguagesTypeScriptPage;
    private SettingsLanguagesTypeScriptAngularPage currentLanguagesTypeScriptAngularPage;
    private SettingsLanguagesTypeScriptTsLintPage currentLanguagesTypeScriptTsLintPage;
    private SettingsLanguagesTypeScriptVuePage currentLanguagesTypeScriptVuePage;
    private SettingsLanguagesWebContextsPage currentLanguagesWebContextsPage;
    private SettingsLanguagesXsltPage currentLanguagesXsltPage;
    private SettingsLanguagesXsltFileAssociationsPage currentLanguagesXsltFileAssociationsPage;
    private SettingsApplicationServersPage currentApplicationServersPage;
    private SettingsBuildToolsPage currentBuildToolsPage;
    private SettingsMavenPage currentMavenPage;
    private SettingsMavenArchetypeCatalogsPage currentMavenArchetypeCatalogsPage;
    private SettingsMavenIgnoredFilesPage currentMavenIgnoredFilesPage;
    private SettingsMavenImportingPage currentMavenImportingPage;
    private SettingsMavenRepositoriesPage currentMavenRepositoriesPage;
    private SettingsMavenRunnerPage currentMavenRunnerPage;
    private SettingsMavenRunningTestsPage currentMavenRunningTestsPage;
    private SettingsGradlePage currentGradlePage;
    private SettingsGantPage currentGantPage;
    private SettingsBspPage currentBspPage;
    private SettingsCargoPage currentCargoPage;
    private SettingsSbtPage currentSbtPage;
    private SettingsCompilerPage currentCompilerPage;
    private SettingsAnnotationProcessorsPage currentAnnotationProcessorsPage;
    private SettingsCompilerExcludesPage currentCompilerExcludesPage;
    private SettingsGroovyCompilerPage currentGroovyCompilerPage;
    private SettingsJavaCompilerPage currentJavaCompilerPage;
    private SettingsKotlinCompilerPage currentKotlinCompilerPage;
    private SettingsRmiCompilerPage currentRmiCompilerPage;
    private SettingsScalaCompilerPage currentScalaCompilerPage;
    private SettingsScalaBytecodeIndicesPage currentScalaBytecodeIndicesPage;
    private SettingsScalaCompileServerPage currentScalaCompileServerPage;
    private SettingsValidationPage currentValidationPage;
    private SettingsBuildConsolePage currentBuildConsolePage;
    private SettingsPythonConsolePage currentPythonConsolePage;
    private SettingsCoveragePage currentCoveragePage;
    private SettingsFontPage currentFontPage;
    private SettingsColorSchemePage currentColorSchemePage;
    private SettingsColorSchemeGeneralPage currentColorSchemeGeneralPage;
    private SettingsColorSchemeLanguageDefaultsPage currentColorSchemeLanguageDefaultsPage;
    private SettingsColorSchemeFontPage currentColorSchemeFontPage;
    private SettingsConsoleFontPage currentConsoleFontPage;
    private SettingsConsoleColorsPage currentConsoleColorsPage;
    private SettingsCodeWithMePage currentCodeWithMePage;
    private SettingsColorSchemeDebuggerPage currentColorSchemeDebuggerPage;
    private SettingsColorSchemeDiffMergePage currentColorSchemeDiffMergePage;
    private SettingsColorSchemeJvmLoggingPage currentColorSchemeJvmLoggingPage;
    private SettingsColorSchemeUserDefinedFileTypesPage currentColorSchemeUserDefinedFileTypesPage;
    private SettingsColorSchemeVcsPage currentColorSchemeVcsPage;
    private SettingsColorSchemeJavaPage currentColorSchemeJavaPage;
    private SettingsColorSchemeAngularTemplatePage currentColorSchemeAngularTemplatePage;
    private SettingsColorSchemeContextFreeGrammarPage currentColorSchemeContextFreeGrammarPage;
    private SettingsColorSchemeCssPage currentColorSchemeCssPage;
    private SettingsColorSchemeDataEditorViewerPage currentColorSchemeDataEditorViewerPage;
    private SettingsColorSchemeDatabasePage currentColorSchemeDatabasePage;
    private SettingsColorSchemeDiagramsPage currentColorSchemeDiagramsPage;
    private SettingsColorSchemeDockerfilePage currentColorSchemeDockerfilePage;
    private SettingsColorSchemeEditorConfigPage currentColorSchemeEditorConfigPage;
    private SettingsColorSchemeErbPage currentColorSchemeErbPage;
    private SettingsColorSchemeFreeMarkerPage currentColorSchemeFreeMarkerPage;
    private SettingsColorSchemeGitLabCiExpressionPage currentColorSchemeGitLabCiExpressionPage;
    private SettingsColorSchemeGoPage currentColorSchemeGoPage;
    private SettingsColorSchemeGradleDeclarativePage currentColorSchemeGradleDeclarativePage;
    private SettingsColorSchemeGroovyPage currentColorSchemeGroovyPage;
    private SettingsColorSchemeHtmlPage currentColorSchemeHtmlPage;
    private SettingsColorSchemeHttpRequestPage currentColorSchemeHttpRequestPage;
    private SettingsColorSchemeJavaScriptPage currentColorSchemeJavaScriptPage;
    private SettingsColorSchemeJpaHibernateQlPage currentColorSchemeJpaHibernateQlPage;
    private SettingsColorSchemeJsonPage currentColorSchemeJsonPage;
    private SettingsColorSchemeJsonPathPage currentColorSchemeJsonPathPage;
    private SettingsColorSchemeJspPage currentColorSchemeJspPage;
    private SettingsColorSchemeJupyterNotebooksPage currentColorSchemeJupyterNotebooksPage;
    private SettingsColorSchemeKotlinPage currentColorSchemeKotlinPage;
    private SettingsColorSchemeKubernetesPage currentColorSchemeKubernetesPage;
    private SettingsColorSchemeLessPage currentColorSchemeLessPage;
    private SettingsColorSchemeLombokConfigPage currentColorSchemeLombokConfigPage;
    private SettingsColorSchemeMarkdownPage currentColorSchemeMarkdownPage;
    private SettingsColorSchemeMicronautELPage currentColorSchemeMicronautELPage;
    private SettingsColorSchemePHPPage currentColorSchemePhpPage;
    private SettingsColorSchemePlan9X86Page currentColorSchemePlan9X86Page;
    private SettingsColorSchemePostCSSPage currentColorSchemePostCSSPage;
    private SettingsColorSchemeProtocolBufferPage currentColorSchemeProtocolBufferPage;
    private SettingsColorSchemeMongoDBJSONPage currentColorSchemeMongoDbJsonPage;
    private SettingsColorSchemePropertiesPage currentColorSchemePropertiesPage;
    private SettingsColorSchemeProtocolBufferTextPage currentColorSchemeProtocolBufferTextPage;
    private SettingsColorSchemePythonPage currentColorSchemePythonPage;
    private SettingsColorSchemeQutePage currentColorSchemeQutePage;
    private SettingsColorSchemeRDocPage currentColorSchemeRDocPage;
    private SettingsColorSchemeRegExpPage currentColorSchemeRegExpPage;
    private SettingsColorSchemeRubyPage currentColorSchemeRubyPage;
    private SettingsColorSchemeRustPage currentColorSchemeRustPage;
    private SettingsColorSchemeSassPage currentColorSchemeSassPage;
    private SettingsColorSchemeScalaPage currentColorSchemeScalaPage;
    private SettingsColorSchemeShellScriptPage currentColorSchemeShellScriptPage;
    private SettingsColorSchemeSmartyPage currentColorSchemeSmartyPage;
    private SettingsColorSchemeSpringELPage currentColorSchemeSpringELPage;
    private SettingsColorSchemeSQLPage currentColorSchemeSQLPage;
    private SettingsColorSchemeTableDiffPage currentColorSchemeTableDiffPage;
    private SettingsColorSchemeTOMLPage currentColorSchemeTomlPage;
    private SettingsColorSchemeTypeScriptPage currentColorSchemeTypeScriptPage;
    private SettingsColorSchemeVelocityPage currentColorSchemeVelocityPage;
    private SettingsColorSchemeXMLPage currentColorSchemeXmlPage;
    private SettingsColorSchemeXPathPage currentColorSchemeXPathPage;
    private SettingsColorSchemeXSLTPage currentColorSchemeXsltPage;
    private SettingsColorSchemeYAMLPage currentColorSchemeYamlPage;
    private SettingsColorSchemeByScopePage currentColorSchemeByScopePage;
    private SettingsColorSchemeImagesPage currentColorSchemeImagesPage;
    private SettingsCodeStylePage currentCodeStylePage;
    private SettingsCodeStyleLanguagePage currentCodeStyleLanguagePage;
    private SettingsCodeStyleJavaPage currentCodeStyleJavaPage;
    private Button applyButton;

    public SettingsDialog(Stage owner) {
        this(owner, "Appearance");
    }

    public SettingsDialog(Stage owner, String initialCategory) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Settings");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "settings-dialog");
        root.setStyle("-fx-background-color: #1E1F22;");

        // ---- Left: Category search + tree ----
        tree = buildCategoryTree();
        tree.setPrefWidth(260);
        tree.setMinWidth(240);
        tree.getStyleClass().add("settings-tree");
        tree.setStyle("-fx-background-color: #1E1F22;");

        HBox searchBox = new HBox(6);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(8, 12, 8, 12));
        searchBox.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #393B40 transparent; -fx-border-width: 0 0 1 0;");

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");

        searchField = new TextField();
        searchField.setPromptText("Search settings");
        searchField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, searchField);

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.trim().isEmpty()) {
                TreeItem<String> match = searchTree(tree.getRoot(), newV.trim().toLowerCase());
                if (match != null) {
                    expandAncestors(match);
                    tree.getSelectionModel().select(match);
                    tree.scrollTo(tree.getRow(match));
                }
            }
        });

        VBox.setVgrow(tree, Priority.ALWAYS);
        VBox leftPane = new VBox(searchBox, tree);
        leftPane.setPrefWidth(260);
        leftPane.setMinWidth(240);
        leftPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent #393B40 transparent transparent; -fx-border-width: 0 1 0 0;");

        // ---- Right: Header + Content ----
        BorderPane rightPane = new BorderPane();
        rightPane.setStyle("-fx-background-color: #1E1F22;");

        HBox header = buildHeader();
        rightPane.setTop(header);
        rightPane.setCenter(contentContainer);

        // ---- Bottom: Buttons ----
        HBox buttons = buildButtonBar();

        root.setLeft(leftPane);
        root.setCenter(rightPane);
        root.setBottom(buttons);

        // Selection listener
        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                if (!isNavigatingHistory) {
                    if (navHistoryIndex >= 0 && navHistoryIndex < navHistory.size() - 1) {
                        navHistory.subList(navHistoryIndex + 1, navHistory.size()).clear();
                    }
                    navHistory.add(selected);
                    navHistoryIndex = navHistory.size() - 1;
                    updateNavButtons();
                }
                updateBreadcrumbs(selected);
                showPage(selected);
            }
        });

        // Initial selection
        TreeItem<String> initialItem = findItem(tree.getRoot(), initialCategory);
        if (initialItem == null) initialItem = findItem(tree.getRoot(), "Appearance");
        if (initialItem != null) {
            expandAncestors(initialItem);
            tree.getSelectionModel().select(initialItem);
            if (contentContainer.getChildren().isEmpty()) {
                updateBreadcrumbs(initialItem);
                showPage(initialItem);
            }
        }

        Scene scene = new Scene(root, 960, 640);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        stage.showAndWait();
    }

    StackPane getContentContainer() {
        return contentContainer;
    }

    public TreeView<String> getTree() {
        return tree;
    }

    public TreeItem<String> getTreeRoot() {
        return tree != null ? tree.getRoot() : null;
    }

    // --------------------------------------------------- Header & Navigation

    private HBox buildHeader() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 12, 24));
        bar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #393B40 transparent; -fx-border-width: 0 0 1 0;");

        breadcrumbBox.setAlignment(Pos.CENTER_LEFT);
        breadcrumbBox.setSpacing(6);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: default; -fx-padding: 0 4 0 4;");
        backButton.setDisable(true);
        backButton.setOnAction(e -> navigateHistory(-1));

        forwardButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: default; -fx-padding: 0 4 0 4;");
        forwardButton.setDisable(true);
        forwardButton.setOnAction(e -> navigateHistory(1));

        bar.getChildren().addAll(breadcrumbBox, spacer, backButton, forwardButton);
        return bar;
    }

    private void updateBreadcrumbs(TreeItem<String> selected) {
        breadcrumbBox.getChildren().clear();
        List<TreeItem<String>> chain = new ArrayList<>();
        TreeItem<String> it = selected;
        while (it != null && it.getParent() != null) {
            chain.add(0, it);
            it = it.getParent();
        }

        for (int i = 0; i < chain.size(); i++) {
            final TreeItem<String> item = chain.get(i);
            if (i < chain.size() - 1) {
                Hyperlink link = new Hyperlink(item.getValue());
                link.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
                link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: true;"));
                link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;"));
                link.setOnAction(e -> {
                    expandAncestors(item);
                    tree.getSelectionModel().select(item);
                });

                Label chevron = new Label("\u203A");
                chevron.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 14px; -fx-font-weight: bold;");
                breadcrumbBox.getChildren().addAll(link, chevron);
            } else {
                Label current = new Label(item.getValue());
                current.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
                breadcrumbBox.getChildren().add(current);

                if ("Menus and Toolbars".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("🗄 Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentMenusToolbarsPage != null) {
                            currentMenusToolbarsPage.revertChanges();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Quick Lists".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentQuickListsPage != null) {
                            currentQuickListsPage.revert();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Code Completion".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCodeCompletionPage != null) {
                            currentCodeCompletionPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Code Folding".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCodeFoldingPage != null) {
                            currentCodeFoldingPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Console".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentConsolePage != null) {
                            currentConsolePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Editor Tabs".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentEditorTabsPage != null) {
                            currentEditorTabsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Gutter Icons".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentGutterIconsPage != null) {
                            currentGutterIconsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Inline Completion".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentInlineCompletionPage != null) {
                            currentInlineCompletionPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Postfix Completion".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPostfixCompletionPage != null) {
                            currentPostfixCompletionPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Smart Keys".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysPage != null) {
                            currentSmartKeysPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("YAML".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysYamlPage != null) {
                            currentSmartKeysYamlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("HTML/CSS".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysHtmlCssPage != null) {
                            currentSmartKeysHtmlCssPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Python".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysPythonPage != null) {
                            currentSmartKeysPythonPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JSON".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysJsonPage != null) {
                            currentSmartKeysJsonPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Rust".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysRustPage != null) {
                            currentSmartKeysRustPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Markdown".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysMarkdownPage != null) {
                            currentSmartKeysMarkdownPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Scala".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysScalaPage != null) {
                            currentSmartKeysScalaPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("SQL".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysSqlPage != null) {
                            currentSmartKeysSqlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Ruby".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysRubyPage != null) {
                            currentSmartKeysRubyPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Python Template Languages".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPythonTemplateLanguagesPage != null) {
                            currentPythonTemplateLanguagesPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Go".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesGoPage != null) {
                            currentLanguagesGoPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("GOROOT".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesGoGoRootPage != null) {
                            currentLanguagesGoGoRootPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("GOPATH".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesGoGoPathPage != null) {
                            currentLanguagesGoGoPathPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Go Modules".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesGoModulesPage != null) {
                            currentLanguagesGoModulesPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Build Tags".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesGoBuildTagsPage != null) {
                            currentLanguagesGoBuildTagsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Formatting Functions".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesGoFormattingFunctionsPage != null) {
                            currentLanguagesGoFormattingFunctionsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Imports".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesGoImportsPage != null) {
                            currentLanguagesGoImportsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JavaFX".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJavaFxPage != null) {
                            currentLanguagesJavaFxPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JavaScript".equals(item.getValue())) {
                    if (chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                        Hyperlink revertLink = new Hyperlink("Revert changes");
                        revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                        revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                        revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                        revertLink.setOnAction(e -> {
                            if (currentLanguagesJavaScriptPage != null) {
                                currentLanguagesJavaScriptPage.revertChanges();
                                updateApplyButtonState();
                            }
                        });
                        breadcrumbBox.getChildren().add(revertLink);
                    } else {
                        Hyperlink revertLink = new Hyperlink("Revert changes");
                        revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                        revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                        revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                        revertLink.setOnAction(e -> {
                            if (currentSmartKeysJsPage != null) {
                                currentSmartKeysJsPage.reset();
                                updateApplyButtonState();
                            }
                        });
                        breadcrumbBox.getChildren().add(revertLink);
                    }
                } else if ("ESLint".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJsEsLintPage != null) {
                            currentLanguagesJsEsLintPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JSHint".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJsJsHintPage != null) {
                            currentLanguagesJsJsHintPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Libraries".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJsLibrariesPage != null) {
                            currentLanguagesJsLibrariesPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Prettier".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJsPrettierPage != null) {
                            currentLanguagesJsPrettierPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Styled Components".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJsStyledComponentsPage != null) {
                            currentLanguagesJsStyledComponentsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Vite".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJsVitePage != null) {
                            currentLanguagesJsVitePage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Webpack".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJsWebpackPage != null) {
                            currentLanguagesJsWebpackPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JavaScript Runtime".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJavaScriptRuntimePage != null) {
                            currentLanguagesJavaScriptRuntimePage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JVM Logging".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJvmLoggingPage != null) {
                            currentLanguagesJvmLoggingPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Kotlin Scripting".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesKotlinScriptingPage != null) {
                            currentLanguagesKotlinScriptingPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Ktor".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesKtorPage != null) {
                            currentLanguagesKtorPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Kubernetes".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesKubernetesPage != null) {
                            currentLanguagesKubernetesPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Lombok".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesLombokPage != null) {
                            currentLanguagesLombokPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Markdown".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesMarkdownPage != null) {
                            currentLanguagesMarkdownPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Micronaut".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesMicronautPage != null) {
                            currentLanguagesMicronautPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("OpenAPI Specifications".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesOpenApiPage != null) {
                            currentLanguagesOpenApiPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Play".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesPlayPage != null) {
                            currentLanguagesPlayPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Protocol Buffers".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesProtobufPage != null) {
                            currentLanguagesProtobufPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Text Format".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesProtobufTextFormatPage != null) {
                            currentLanguagesProtobufTextFormatPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Quarkus".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesQuarkusPage != null) {
                            currentLanguagesQuarkusPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("RBS".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesRbsPage != null) {
                            currentLanguagesRbsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Editor".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Scala".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesScalaEditorPage != null) {
                            currentLanguagesScalaEditorPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("X-Ray Mode".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Scala".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesScalaXRayPage != null) {
                            currentLanguagesScalaXRayPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Project View".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Scala".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesScalaProjectViewPage != null) {
                            currentLanguagesScalaProjectViewPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Performance".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Scala".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesScalaPerformancePage != null) {
                            currentLanguagesScalaPerformancePage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Worksheet".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Scala".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesScalaWorksheetPage != null) {
                            currentLanguagesScalaWorksheetPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Base Package".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Scala".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesScalaBasePackagePage != null) {
                            currentLanguagesScalaBasePackagePage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Misc".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Scala".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesScalaMiscPage != null) {
                            currentLanguagesScalaMiscPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Updates".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Scala".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesScalaUpdatesPage != null) {
                            currentLanguagesScalaUpdatesPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Extensions".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Scala".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesScalaExtensionsPage != null) {
                            currentLanguagesScalaExtensionsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Schemas and DTDs".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesSchemasAndDtdsPage != null) {
                            currentLanguagesSchemasAndDtdsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Default XML Schemas".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesDefaultXmlSchemasPage != null) {
                            currentLanguagesDefaultXmlSchemasPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JSON Schema Mappings".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesJsonSchemaMappingsPage != null) {
                            currentLanguagesJsonSchemaMappingsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Remote JSON Schemas".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesRemoteJsonSchemasPage != null) {
                            currentLanguagesRemoteJsonSchemasPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("XML Catalog".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesXmlCatalogPage != null) {
                            currentLanguagesXmlCatalogPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Spring".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesSpringPage != null) {
                            currentLanguagesSpringPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("SQL Dialects".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesSqlDialectsPage != null) {
                            currentLanguagesSqlDialectsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("SQL Resolution Scopes".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesSqlResolutionScopesPage != null) {
                            currentLanguagesSqlResolutionScopesPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Dialects".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Style Sheets".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesStyleSheetsDialectsPage != null) {
                            currentLanguagesStyleSheetsDialectsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Stylelint".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Style Sheets".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesStyleSheetsStylelintPage != null) {
                            currentLanguagesStyleSheetsStylelintPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Tailwind CSS".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Style Sheets".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesStyleSheetsTailwindPage != null) {
                            currentLanguagesStyleSheetsTailwindPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Tables".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesTablesPage != null) {
                            currentLanguagesTablesPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Template Data Languages".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesTemplateDataLanguagesPage != null) {
                            currentLanguagesTemplateDataLanguagesPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("TypeScript".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesTypeScriptPage != null) {
                            currentLanguagesTypeScriptPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Angular".equals(item.getValue()) && chain.stream().anyMatch(ci -> "TypeScript".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesTypeScriptAngularPage != null) {
                            currentLanguagesTypeScriptAngularPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("TSLint".equals(item.getValue()) && chain.stream().anyMatch(ci -> "TypeScript".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesTypeScriptTsLintPage != null) {
                            currentLanguagesTypeScriptTsLintPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Vue".equals(item.getValue()) && chain.stream().anyMatch(ci -> "TypeScript".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesTypeScriptVuePage != null) {
                            currentLanguagesTypeScriptVuePage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Web Contexts".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesWebContextsPage != null) {
                            currentLanguagesWebContextsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("XSLT".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesXsltPage != null) {
                            currentLanguagesXsltPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("XSLT File Associations".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesXsltFileAssociationsPage != null) {
                            currentLanguagesXsltFileAssociationsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("PHP".equals(item.getValue())) {
                    if (chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                        Hyperlink revertLink = new Hyperlink("Revert changes");
                        revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                        revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                        revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                        revertLink.setOnAction(e -> {
                            if (currentLanguagesPhpPage != null) {
                                currentLanguagesPhpPage.revertChanges();
                                updateApplyButtonState();
                            }
                        });
                        breadcrumbBox.getChildren().add(revertLink);
                    } else {
                        Hyperlink revertLink = new Hyperlink("Revert changes");
                        revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                        revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                        revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                        revertLink.setOnAction(e -> {
                            if (currentSmartKeysPhpPage != null) {
                                currentSmartKeysPhpPage.reset();
                                updateApplyButtonState();
                            }
                        });
                        breadcrumbBox.getChildren().add(revertLink);
                    }
                } else if ("Debug".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesPhpDebugPage != null) {
                            currentLanguagesPhpDebugPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("DBGp Proxy".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesPhpDebugDbgpProxyPage != null) {
                            currentLanguagesPhpDebugDbgpProxyPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Skipped Paths".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesPhpDebugSkippedPathsPage != null) {
                            currentLanguagesPhpDebugSkippedPathsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Step Filters".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesPhpDebugStepFiltersPage != null) {
                            currentLanguagesPhpDebugStepFiltersPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Xdebug Cloud".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesPhpDebugXdebugCloudPage != null) {
                            currentLanguagesPhpDebugXdebugCloudPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Servers".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesPhpServersPage != null) {
                            currentLanguagesPhpServersPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Composer".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesPhpComposerPage != null) {
                            currentLanguagesPhpComposerPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Test Frameworks".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguagesPhpTestFrameworksPage != null) {
                            currentLanguagesPhpTestFrameworksPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Quality Tools".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPhpQualityToolsPage != null) {
                            currentPhpQualityToolsPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("PHP_CodeSniffer".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPhpCodeSnifferPage != null) {
                            currentPhpCodeSnifferPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("PHP CS Fixer".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPhpCsFixerPage != null) {
                            currentPhpCsFixerPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Laravel Pint".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPhpLaravelPintPage != null) {
                            currentPhpLaravelPintPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Mess Detector".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPhpMessDetectorPage != null) {
                            currentPhpMessDetectorPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Frameworks".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPhpFrameworksPage != null) {
                            currentPhpFrameworksPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Smarty".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPhpSmartyPage != null) {
                            currentPhpSmartyPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Rust".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentRustPage != null) {
                            currentRustPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("External Linters".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentRustExternalLintersPage != null) {
                            currentRustExternalLintersPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Rustfmt".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Languages & Frameworks".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentRustfmtPage != null) {
                            currentRustfmtPage.revertChanges();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Sticky Lines".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentStickyLinesPage != null) {
                            currentStickyLinesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Code Editing".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCodeEditingPage != null) {
                            currentCodeEditingPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Font".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentFontPage != null) {
                            currentFontPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Color Scheme".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemePage != null) {
                            currentColorSchemePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("General".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeGeneralPage != null) {
                            currentColorSchemeGeneralPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Language Defaults".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeLanguageDefaultsPage != null) {
                            currentColorSchemeLanguageDefaultsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Debugger".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDebuggerPage != null) {
                            currentColorSchemeDebuggerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Diff & Merge".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDiffMergePage != null) {
                            currentColorSchemeDiffMergePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JVM Logging".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJvmLoggingPage != null) {
                            currentColorSchemeJvmLoggingPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("User-Defined File Types".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeUserDefinedFileTypesPage != null) {
                            currentColorSchemeUserDefinedFileTypesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("VCS".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeVcsPage != null) {
                            currentColorSchemeVcsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Java".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJavaPage != null) {
                            currentColorSchemeJavaPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Angular Template".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeAngularTemplatePage != null) {
                            currentColorSchemeAngularTemplatePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Context Free Grammar".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeContextFreeGrammarPage != null) {
                            currentColorSchemeContextFreeGrammarPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("CSS".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeCssPage != null) {
                            currentColorSchemeCssPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Data Editor and Viewer".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDataEditorViewerPage != null) {
                            currentColorSchemeDataEditorViewerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Database".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDatabasePage != null) {
                            currentColorSchemeDatabasePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Diagrams".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDiagramsPage != null) {
                            currentColorSchemeDiagramsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Dockerfile".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDockerfilePage != null) {
                            currentColorSchemeDockerfilePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("EditorConfig".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeEditorConfigPage != null) {
                            currentColorSchemeEditorConfigPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("ERB".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeErbPage != null) {
                            currentColorSchemeErbPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("FreeMarker".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeFreeMarkerPage != null) {
                            currentColorSchemeFreeMarkerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("GitLab CI Expression".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeGitLabCiExpressionPage != null) {
                            currentColorSchemeGitLabCiExpressionPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Go".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeGoPage != null) {
                            currentColorSchemeGoPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Gradle Declarative Configuration".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeGradleDeclarativePage != null) {
                            currentColorSchemeGradleDeclarativePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Groovy".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeGroovyPage != null) {
                            currentColorSchemeGroovyPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("HTML".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeHtmlPage != null) {
                            currentColorSchemeHtmlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("HTTP Request".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeHttpRequestPage != null) {
                            currentColorSchemeHttpRequestPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JavaScript".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJavaScriptPage != null) {
                            currentColorSchemeJavaScriptPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JPA/Hibernate QL".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJpaHibernateQlPage != null) {
                            currentColorSchemeJpaHibernateQlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JSON".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJsonPage != null) {
                            currentColorSchemeJsonPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JSONPath".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJsonPathPage != null) {
                            currentColorSchemeJsonPathPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JSP".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJspPage != null) {
                            currentColorSchemeJspPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Jupyter Notebooks".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJupyterNotebooksPage != null) {
                            currentColorSchemeJupyterNotebooksPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Kotlin".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeKotlinPage != null) {
                            currentColorSchemeKotlinPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Kubernetes".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeKubernetesPage != null) {
                            currentColorSchemeKubernetesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Less".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeLessPage != null) {
                            currentColorSchemeLessPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Lombok Config".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeLombokConfigPage != null) {
                            currentColorSchemeLombokConfigPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Markdown".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeMarkdownPage != null) {
                            currentColorSchemeMarkdownPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Micronaut EL".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeMicronautELPage != null) {
                            currentColorSchemeMicronautELPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("PHP".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemePhpPage != null) {
                            currentColorSchemePhpPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("plan9_x86".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemePlan9X86Page != null) {
                            currentColorSchemePlan9X86Page.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("PostCSS".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemePostCSSPage != null) {
                            currentColorSchemePostCSSPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Protocol Buffer".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeProtocolBufferPage != null) {
                            currentColorSchemeProtocolBufferPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("MongoDB JSON".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeMongoDbJsonPage != null) {
                            currentColorSchemeMongoDbJsonPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Properties".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemePropertiesPage != null) {
                            currentColorSchemePropertiesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Protocol Buffer Text".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeProtocolBufferTextPage != null) {
                            currentColorSchemeProtocolBufferTextPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Python".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemePythonPage != null) {
                            currentColorSchemePythonPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Qute".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeQutePage != null) {
                            currentColorSchemeQutePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("RDoc".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeRDocPage != null) {
                            currentColorSchemeRDocPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("RegExp".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeRegExpPage != null) {
                            currentColorSchemeRegExpPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Ruby".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeRubyPage != null) {
                            currentColorSchemeRubyPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Rust".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeRustPage != null) {
                            currentColorSchemeRustPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Sass/SCSS".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeSassPage != null) {
                            currentColorSchemeSassPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Scala".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeScalaPage != null) {
                            currentColorSchemeScalaPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Shell Script".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeShellScriptPage != null) {
                            currentColorSchemeShellScriptPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Smarty".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeSmartyPage != null) {
                            currentColorSchemeSmartyPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Spring EL".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeSpringELPage != null) {
                            currentColorSchemeSpringELPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("SQL".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeSQLPage != null) {
                            currentColorSchemeSQLPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Table Diff".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeTableDiffPage != null) {
                            currentColorSchemeTableDiffPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("TOML".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeTomlPage != null) {
                            currentColorSchemeTomlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("TypeScript".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeTypeScriptPage != null) {
                            currentColorSchemeTypeScriptPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Velocity".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeVelocityPage != null) {
                            currentColorSchemeVelocityPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("XML".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeXmlPage != null) {
                            currentColorSchemeXmlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("XPath".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeXPathPage != null) {
                            currentColorSchemeXPathPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("XSLT".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeXsltPage != null) {
                            currentColorSchemeXsltPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("YAML".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeYamlPage != null) {
                            currentColorSchemeYamlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("By Scope".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeByScopePage != null) {
                            currentColorSchemeByScopePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Images".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeImagesPage != null) {
                            currentColorSchemeImagesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Code Style".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCodeStylePage != null) {
                            currentCodeStylePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Java".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Code Style".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCodeStyleJavaPage != null) {
                            currentCodeStyleJavaPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if (chain.stream().anyMatch(ci -> "Code Style".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCodeStyleLanguagePage != null) {
                            currentCodeStyleLanguagePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("File and Code Templates".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentFileAndCodeTemplatesPage != null) {
                            currentFileAndCodeTemplatesPage.revertCurrent();
                            updateApplyButtonState();
                        }
                    });
                    if (currentFileAndCodeTemplatesPage != null) {
                        revertLink.visibleProperty().bind(currentFileAndCodeTemplatesPage.canRevertProperty());
                        revertLink.managedProperty().bind(revertLink.visibleProperty());
                    } else {
                        revertLink.setVisible(false);
                        revertLink.setManaged(false);
                    }
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("File Types".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentFileTypesPage != null) {
                            currentFileTypesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Copyright".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCopyrightPage != null) {
                            currentCopyrightPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Copyright Profiles".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCopyrightProfilesPage != null) {
                            currentCopyrightProfilesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Formatting".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Copyright".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCopyrightFormattingPage != null) {
                            currentCopyrightFormattingPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if (chain.stream().anyMatch(ci -> "Formatting".equals(ci.getValue())) && chain.stream().anyMatch(ci -> "Copyright".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        SettingsCopyrightFormattingLanguagePage lp = languageFormattingPages.get(item.getValue());
                        if (lp != null) {
                            lp.reset();
                            updateApplyButtonState();
                        }
                    });
                } else if ("Intentions".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentIntentionsPage != null) {
                            currentIntentionsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Language Injections".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguageInjectionsPage != null) {
                            currentLanguageInjectionsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Advanced".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Language Injections".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentLanguageInjectionsAdvancedPage != null) {
                            currentLanguageInjectionsAdvancedPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Natural Languages".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentNaturalLanguagesPage != null) {
                            currentNaturalLanguagesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Grammar and Style".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentGrammarAndStylePage != null) {
                            currentGrammarAndStylePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Spelling".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSpellingPage != null) {
                            currentSpellingPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Reader Mode".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentReaderModePage != null) {
                            currentReaderModePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("TextMate Bundles".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentTextMateBundlesPage != null) {
                            currentTextMateBundlesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("TODO".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentTodoPage != null) {
                            currentTodoPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Python Debugger".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPythonDebuggerPage != null) {
                            currentPythonDebuggerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Async Stack Traces".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentDebuggerAsyncStackTracesPage != null) {
                            currentDebuggerAsyncStackTracesPage.revert();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Java Type Renderers".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentDebuggerDataViewsTypeRenderersPage != null) {
                            currentDebuggerDataViewsTypeRenderersPage.revert();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Docker".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentDockerPage != null) {
                            currentDockerPage.revert();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Docker Registry".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentDockerRegistryPage != null) {
                            currentDockerRegistryPage.revert();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Java Profiler".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentJavaProfilerPage != null) {
                            currentJavaProfilerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Filters".equals(item.getValue()) && isUnderJavaProfiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentJavaProfilerFiltersPage != null) {
                            currentJavaProfilerFiltersPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Kubernetes".equals(item.getValue()) && isUnderBuild(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentBuildKubernetesPage != null) {
                            currentBuildKubernetesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Remote Jar Repositories".equals(item.getValue()) && isUnderBuild(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentRemoteJarRepositoriesPage != null) {
                            currentRemoteJarRepositoriesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Run Targets".equals(item.getValue()) && isUnderBuild(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentRunTargetsPage != null) {
                            currentRunTargetsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Application Servers".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentApplicationServersPage != null) {
                            currentApplicationServersPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Build Tools".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentBuildToolsPage != null) {
                            currentBuildToolsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Maven".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentMavenPage != null) {
                            currentMavenPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Archetype Catalogs".equals(item.getValue()) && isUnderMaven(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentMavenArchetypeCatalogsPage != null) {
                            currentMavenArchetypeCatalogsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Ignored Files".equals(item.getValue()) && isUnderMaven(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentMavenIgnoredFilesPage != null) {
                            currentMavenIgnoredFilesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Importing".equals(item.getValue()) && isUnderMaven(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentMavenImportingPage != null) {
                            currentMavenImportingPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Repositories".equals(item.getValue()) && isUnderMaven(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentMavenRepositoriesPage != null) {
                            currentMavenRepositoriesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Runner".equals(item.getValue()) && isUnderMaven(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentMavenRunnerPage != null) {
                            currentMavenRunnerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Running Tests".equals(item.getValue()) && isUnderMaven(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentMavenRunningTestsPage != null) {
                            currentMavenRunningTestsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Gradle".equals(item.getValue()) && isUnderBuildTools(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentGradlePage != null) {
                            currentGradlePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Gant".equals(item.getValue()) && isUnderBuildTools(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentGantPage != null) {
                            currentGantPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("BSP".equals(item.getValue()) && isUnderBuildTools(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentBspPage != null) {
                            currentBspPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Cargo".equals(item.getValue()) && isUnderBuildTools(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCargoPage != null) {
                            currentCargoPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("sbt".equals(item.getValue()) && isUnderBuildTools(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSbtPage != null) {
                            currentSbtPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Compiler".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCompilerPage != null) {
                            currentCompilerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Annotation Processors".equals(item.getValue()) && isUnderCompiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentAnnotationProcessorsPage != null) {
                            currentAnnotationProcessorsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Excludes".equals(item.getValue()) && isUnderCompiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCompilerExcludesPage != null) {
                            currentCompilerExcludesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Groovy Compiler".equals(item.getValue()) && isUnderCompiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentGroovyCompilerPage != null) {
                            currentGroovyCompilerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Java Compiler".equals(item.getValue()) && isUnderCompiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentJavaCompilerPage != null) {
                            currentJavaCompilerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Kotlin Compiler".equals(item.getValue()) && isUnderCompiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentKotlinCompilerPage != null) {
                            currentKotlinCompilerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("RMI Compiler".equals(item.getValue()) && isUnderCompiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentRmiCompilerPage != null) {
                            currentRmiCompilerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Scala Compiler".equals(item.getValue()) && isUnderCompiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentScalaCompilerPage != null) {
                            currentScalaCompilerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Bytecode Indices".equals(item.getValue()) && isUnderCompiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentScalaBytecodeIndicesPage != null) {
                            currentScalaBytecodeIndicesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Scala Compile Server".equals(item.getValue()) && isUnderCompiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentScalaCompileServerPage != null) {
                            currentScalaCompileServerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Validation".equals(item.getValue()) && isUnderCompiler(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentValidationPage != null) {
                            currentValidationPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Console".equals(item.getValue()) && isUnderBuild(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentBuildConsolePage != null) {
                            currentBuildConsolePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Python Console".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPythonConsolePage != null) {
                            currentPythonConsolePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Coverage".equals(item.getValue()) && isUnderBuild(item)) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCoveragePage != null) {
                            currentCoveragePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Inlay Hints".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentInlayHintsPage != null) {
                            currentInlayHintsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Duplicates".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentDuplicatesPage != null) {
                            currentDuplicatesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Emmet".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentEmmetPage != null) {
                            currentEmmetPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("CSS".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Emmet".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentEmmetCssPage != null) {
                            currentEmmetCssPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("HTML".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Emmet".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentEmmetHtmlPage != null) {
                            currentEmmetHtmlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JSX".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Emmet".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentEmmetJsxPage != null) {
                            currentEmmetJsxPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Required Plugins".equals(item.getValue())) {
                    Label projectIcon = new Label("📦");
                    projectIcon.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 6;");
                    breadcrumbBox.getChildren().add(projectIcon);
                }

                if (isProjectSetting(item)) {
                    SVGPath pIcon = new SVGPath();
                    pIcon.setContent("M 1 2 L 11 2 L 11 10 L 1 10 Z M 1 4 L 11 4");
                    pIcon.setFill(Color.TRANSPARENT);
                    pIcon.setStroke(Color.web("#848BA3"));
                    pIcon.setStrokeWidth(0.9);
                    HBox.setMargin(pIcon, new Insets(0, 0, 0, 4));
                    breadcrumbBox.getChildren().add(pIcon);
                }
            }
        }
    }

    private void navigateHistory(int delta) {
        int target = navHistoryIndex + delta;
        if (target >= 0 && target < navHistory.size()) {
            isNavigatingHistory = true;
            navHistoryIndex = target;
            TreeItem<String> item = navHistory.get(target);
            expandAncestors(item);
            tree.getSelectionModel().select(item);
            isNavigatingHistory = false;
            updateNavButtons();
        }
    }

    private void updateNavButtons() {
        boolean canBack = navHistoryIndex > 0;
        boolean canFwd = navHistoryIndex < navHistory.size() - 1;

        backButton.setDisable(!canBack);
        backButton.setStyle(canBack
                ? "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4 0 4;"
                : "-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: default; -fx-padding: 0 4 0 4;");

        forwardButton.setDisable(!canFwd);
        forwardButton.setStyle(canFwd
                ? "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4 0 4;"
                : "-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: default; -fx-padding: 0 4 0 4;");
    }

    // --------------------------------------------------- Page Routing

    private void showPage(TreeItem<String> selected) {
        if (currentIdeAppearancePage != null) {
            currentIdeAppearancePage.save();
            currentIdeAppearancePage = null;
        }
        if (currentSystemPage != null) {
            currentSystemPage.save();
            currentSystemPage = null;
        }
        contentContainer.getChildren().clear();

        String pageName = selected.getValue();

        // 1. If a category node with children is selected (e.g. Appearance & Behavior), show Category Overview
        if (!selected.getChildren().isEmpty() &&
                (selected.getParent() == tree.getRoot() || "Appearance & Behavior".equals(pageName))) {
            SettingsCategoryOverviewPage overview = new SettingsCategoryOverviewPage(selected, child -> {
                expandAncestors(child);
                tree.getSelectionModel().select(child);
            });
            wrapInScroll(overview);
            return;
        }

        // 2. Ancestor hierarchy detection
        TreeItem<String> ancestor = selected.getParent();
        boolean underAppearanceGroup = false;
        boolean underEditorGroup = false;
        boolean underColorScheme = false;
        boolean underCodeStyle = false;
        boolean underSmartKeys = false;
        boolean underCopyright = false;
        boolean underFormatting = false;
        boolean underEmmet = false;
        boolean underDebugger = false;
        boolean underDataViews = false;
        boolean underLanguages = false;

        while (ancestor != null) {
            String v = ancestor.getValue();
            if (v != null) {
                if (v.equals("Appearance & Behavior")) underAppearanceGroup = true;
                if (v.equals("Editor")) underEditorGroup = true;
                if (v.equals("Color Scheme")) underColorScheme = true;
                if (v.equals("Code Style")) underCodeStyle = true;
                if (v.equals("Smart Keys")) underSmartKeys = true;
                if (v.equals("Copyright")) underCopyright = true;
                if (v.equals("Formatting")) underFormatting = true;
                if (v.equals("Emmet")) underEmmet = true;
                if (v.equals("Debugger")) underDebugger = true;
                if (v.equals("Data Views")) underDataViews = true;
                if (v.equals("Languages & Frameworks")) underLanguages = true;
            }
            ancestor = ancestor.getParent();
        }

        // Languages & Frameworks subpages
        if (underLanguages) {
            if ("Updates".equals(pageName) && isUnderScala(selected)) {
                buildLanguagesScalaUpdatesPage();
                return;
            }
            if ("Extensions".equals(pageName) && isUnderScala(selected)) {
                buildLanguagesScalaExtensionsPage();
                return;
            }
            if ("Editor".equals(pageName) && isUnderScala(selected)) {
                buildLanguagesScalaEditorPage();
                return;
            }
            if ("X-Ray Mode".equals(pageName) && isUnderScala(selected)) {
                buildLanguagesScalaXRayPage();
                return;
            }
            if ("Project View".equals(pageName) && isUnderScala(selected)) {
                buildLanguagesScalaProjectViewPage();
                return;
            }
            if ("Performance".equals(pageName) && isUnderScala(selected)) {
                buildLanguagesScalaPerformancePage();
                return;
            }
            if ("Worksheet".equals(pageName) && isUnderScala(selected)) {
                buildLanguagesScalaWorksheetPage();
                return;
            }
            if ("Base Package".equals(pageName) && isUnderScala(selected)) {
                buildLanguagesScalaBasePackagePage();
                return;
            }
            if ("Misc".equals(pageName) && isUnderScala(selected)) {
                buildLanguagesScalaMiscPage();
                return;
            }
            if ("Schemas and DTDs".equals(pageName)) {
                buildLanguagesSchemasAndDtdsPage();
                return;
            }
            if ("Default XML Schemas".equals(pageName)) {
                buildLanguagesDefaultXmlSchemasPage();
                return;
            }
            if ("JSON Schema Mappings".equals(pageName)) {
                buildLanguagesJsonSchemaMappingsPage();
                return;
            }
            if ("Remote JSON Schemas".equals(pageName)) {
                buildLanguagesRemoteJsonSchemasPage();
                return;
            }
            if ("XML Catalog".equals(pageName)) {
                buildLanguagesXmlCatalogPage();
                return;
            }
            if ("Spring".equals(pageName)) {
                buildLanguagesSpringPage();
                return;
            }
            if ("SQL Dialects".equals(pageName)) {
                buildLanguagesSqlDialectsPage();
                return;
            }
            if ("SQL Resolution Scopes".equals(pageName)) {
                buildLanguagesSqlResolutionScopesPage();
                return;
            }
            if ("Dialects".equals(pageName) && isUnderStyleSheets(selected)) {
                buildLanguagesStyleSheetsDialectsPage();
                return;
            }
            if ("Stylelint".equals(pageName) && isUnderStyleSheets(selected)) {
                buildLanguagesStyleSheetsStylelintPage();
                return;
            }
            if ("Tailwind CSS".equals(pageName) && isUnderStyleSheets(selected)) {
                buildLanguagesStyleSheetsTailwindPage();
                return;
            }
            if ("Tables".equals(pageName)) {
                buildLanguagesTablesPage();
                return;
            }
            if ("Template Data Languages".equals(pageName)) {
                buildLanguagesTemplateDataLanguagesPage();
                return;
            }
            if ("TypeScript".equals(pageName)) {
                buildLanguagesTypeScriptPage();
                return;
            }
            if ("Angular".equals(pageName) && isUnderTypeScript(selected)) {
                buildLanguagesTypeScriptAngularPage();
                return;
            }
            if ("TSLint".equals(pageName) && isUnderTypeScript(selected)) {
                buildLanguagesTypeScriptTsLintPage();
                return;
            }
            if ("Vue".equals(pageName) && isUnderTypeScript(selected)) {
                buildLanguagesTypeScriptVuePage();
                return;
            }
            if ("Web Contexts".equals(pageName)) {
                buildLanguagesWebContextsPage();
                return;
            }
            if ("XSLT".equals(pageName)) {
                buildLanguagesXsltPage();
                return;
            }
            if ("XSLT File Associations".equals(pageName)) {
                buildLanguagesXsltFileAssociationsPage();
                return;
            }
            if ("PHP".equals(pageName)) {
                buildLanguagesPhpPage();
                return;
            }
            if ("Debug".equals(pageName)) {
                buildLanguagesPhpDebugPage();
                return;
            }
            if ("Templates".equals(pageName)) {
                buildLanguagesPhpDebugTemplatesPage();
                return;
            }
            if ("DBGp Proxy".equals(pageName)) {
                buildLanguagesPhpDebugDbgpProxyPage();
                return;
            }
            if ("Skipped Paths".equals(pageName)) {
                buildLanguagesPhpDebugSkippedPathsPage();
                return;
            }
            if ("Step Filters".equals(pageName)) {
                buildLanguagesPhpDebugStepFiltersPage();
                return;
            }
            if ("Xdebug Cloud".equals(pageName)) {
                buildLanguagesPhpDebugXdebugCloudPage();
                return;
            }
            if ("Servers".equals(pageName)) {
                buildLanguagesPhpServersPage();
                return;
            }
            if ("Composer".equals(pageName)) {
                buildLanguagesPhpComposerPage();
                return;
            }
            if ("Test Frameworks".equals(pageName)) {
                buildLanguagesPhpTestFrameworksPage();
                return;
            }
            if ("Quality Tools".equals(pageName)) {
                buildLanguagesPhpQualityToolsPage();
                return;
            }
            if ("PHP_CodeSniffer".equals(pageName)) {
                buildLanguagesPhpCodeSnifferPage();
                return;
            }
            if ("PHP CS Fixer".equals(pageName)) {
                buildLanguagesPhpCsFixerPage();
                return;
            }
            if ("Laravel Pint".equals(pageName)) {
                buildLanguagesPhpLaravelPintPage();
                return;
            }
            if ("Mess Detector".equals(pageName)) {
                buildLanguagesPhpMessDetectorPage();
                return;
            }
            if ("Frameworks".equals(pageName)) {
                buildLanguagesPhpFrameworksPage();
                return;
            }
            if ("Smarty".equals(pageName)) {
                buildLanguagesPhpSmartyPage();
                return;
            }
            if ("Python Template Languages".equals(pageName)) {
                buildLanguagesPythonTemplateLanguagesPage();
                return;
            }
            if ("Go".equals(pageName)) {
                buildLanguagesGoPage();
                return;
            }
            if ("GOROOT".equals(pageName)) {
                buildLanguagesGoGoRootPage();
                return;
            }
            if ("GOPATH".equals(pageName)) {
                buildLanguagesGoGoPathPage();
                return;
            }
            if ("Go Modules".equals(pageName)) {
                buildLanguagesGoModulesPage();
                return;
            }
            if ("Build Tags".equals(pageName)) {
                buildLanguagesGoBuildTagsPage();
                return;
            }
            if ("Formatting Functions".equals(pageName)) {
                buildLanguagesGoFormattingFunctionsPage();
                return;
            }
            if ("Imports".equals(pageName)) {
                buildLanguagesGoImportsPage();
                return;
            }
            if ("Rust".equals(pageName)) {
                buildLanguagesRustPage();
                return;
            }
            if ("External Linters".equals(pageName)) {
                buildLanguagesRustExternalLintersPage();
                return;
            }
            if ("Rustfmt".equals(pageName)) {
                buildLanguagesRustfmtPage();
                return;
            }
            if ("JavaFX".equals(pageName)) {
                buildLanguagesJavaFxPage();
                return;
            }
            if ("JavaScript".equals(pageName)) {
                buildLanguagesJavaScriptPage();
                return;
            }
            if ("Code Quality Tools".equals(pageName)) {
                buildLanguagesCodeQualityToolsPage();
                return;
            }
            if ("ESLint".equals(pageName)) {
                buildLanguagesEsLintPage();
                return;
            }
            if ("JSHint".equals(pageName)) {
                buildLanguagesJsHintPage();
                return;
            }
            if ("Libraries".equals(pageName)) {
                buildLanguagesLibrariesPage();
                return;
            }
            if ("Prettier".equals(pageName)) {
                buildLanguagesPrettierPage();
                return;
            }
            if ("Styled Components".equals(pageName)) {
                buildLanguagesStyledComponentsPage();
                return;
            }
            if ("Vite".equals(pageName)) {
                buildLanguagesVitePage();
                return;
            }
            if ("Webpack".equals(pageName)) {
                buildLanguagesWebpackPage();
                return;
            }
        }

        // 3. Appearance & Behavior > Appearance
        if (underAppearanceGroup && "Appearance".equals(pageName)) {
            currentIdeAppearancePage = new SettingsIdeAppearancePage();
            wrapInScroll(currentIdeAppearancePage);
            return;
        }

        // 4. Color Scheme subpages
        if (underColorScheme) {
            Runnable navigateToTheme = () -> {
                TreeItem<String> appRoot = findItem(tree.getRoot(), "Appearance & Behavior");
                if (appRoot != null) {
                    TreeItem<String> appItem = findItem(appRoot, "Appearance");
                    if (appItem != null) {
                        expandAncestors(appItem);
                        tree.getSelectionModel().select(appItem);
                    }
                }
            };

            if ("General".equals(pageName)) {
                if (currentColorSchemeGeneralPage == null) {
                    currentColorSchemeGeneralPage = new SettingsColorSchemeGeneralPage();
                }
                currentColorSchemeGeneralPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeGeneralPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeGeneralPage);
                updateApplyButtonState();
                return;
            }
            if ("Language Defaults".equals(pageName)) {
                if (currentColorSchemeLanguageDefaultsPage == null) {
                    currentColorSchemeLanguageDefaultsPage = new SettingsColorSchemeLanguageDefaultsPage();
                }
                currentColorSchemeLanguageDefaultsPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeLanguageDefaultsPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeLanguageDefaultsPage);
                updateApplyButtonState();
                return;
            }
            if ("Color Scheme Font".equals(pageName)) {
                if (currentColorSchemeFontPage == null) {
                    currentColorSchemeFontPage = new SettingsColorSchemeFontPage();
                }
                currentColorSchemeFontPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeFontPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeFontPage);
                updateApplyButtonState();
                return;
            }
            if ("Console Font".equals(pageName)) {
                if (currentConsoleFontPage == null) {
                    currentConsoleFontPage = new SettingsConsoleFontPage();
                }
                currentConsoleFontPage.setOnModifiedListener(this::updateApplyButtonState);
                currentConsoleFontPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentConsoleFontPage);
                updateApplyButtonState();
                return;
            }
            if ("Console Colors".equals(pageName)) {
                if (currentConsoleColorsPage == null) {
                    currentConsoleColorsPage = new SettingsConsoleColorsPage();
                }
                currentConsoleColorsPage.setOnModifiedListener(this::updateApplyButtonState);
                currentConsoleColorsPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentConsoleColorsPage);
                updateApplyButtonState();
                return;
            }
            if ("Code With Me".equals(pageName)) {
                if (currentCodeWithMePage == null) {
                    currentCodeWithMePage = new SettingsCodeWithMePage();
                }
                currentCodeWithMePage.setOnModifiedListener(this::updateApplyButtonState);
                currentCodeWithMePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentCodeWithMePage);
                updateApplyButtonState();
                return;
            }
            if ("Debugger".equals(pageName)) {
                if (currentColorSchemeDebuggerPage == null) {
                    currentColorSchemeDebuggerPage = new SettingsColorSchemeDebuggerPage();
                }
                currentColorSchemeDebuggerPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDebuggerPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeDebuggerPage);
                updateApplyButtonState();
                return;
            }
            if ("Diff & Merge".equals(pageName)) {
                if (currentColorSchemeDiffMergePage == null) {
                    currentColorSchemeDiffMergePage = new SettingsColorSchemeDiffMergePage();
                }
                currentColorSchemeDiffMergePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDiffMergePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeDiffMergePage);
                updateApplyButtonState();
                return;
            }
            if ("JVM Logging".equals(pageName)) {
                if (currentColorSchemeJvmLoggingPage == null) {
                    currentColorSchemeJvmLoggingPage = new SettingsColorSchemeJvmLoggingPage();
                }
                currentColorSchemeJvmLoggingPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJvmLoggingPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeJvmLoggingPage);
                updateApplyButtonState();
                return;
            }
            if ("User-Defined File Types".equals(pageName)) {
                if (currentColorSchemeUserDefinedFileTypesPage == null) {
                    currentColorSchemeUserDefinedFileTypesPage = new SettingsColorSchemeUserDefinedFileTypesPage();
                }
                currentColorSchemeUserDefinedFileTypesPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeUserDefinedFileTypesPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeUserDefinedFileTypesPage);
                updateApplyButtonState();
                return;
            }
            if ("VCS".equals(pageName)) {
                if (currentColorSchemeVcsPage == null) {
                    currentColorSchemeVcsPage = new SettingsColorSchemeVcsPage();
                }
                currentColorSchemeVcsPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeVcsPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeVcsPage);
                updateApplyButtonState();
                return;
            }
            if ("Java".equals(pageName)) {
                if (currentColorSchemeJavaPage == null) {
                    currentColorSchemeJavaPage = new SettingsColorSchemeJavaPage();
                }
                currentColorSchemeJavaPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJavaPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeJavaPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeJavaPage);
                updateApplyButtonState();
                return;
            }
            if ("Angular Template".equals(pageName)) {
                if (currentColorSchemeAngularTemplatePage == null) {
                    currentColorSchemeAngularTemplatePage = new SettingsColorSchemeAngularTemplatePage();
                }
                currentColorSchemeAngularTemplatePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeAngularTemplatePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeAngularTemplatePage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeAngularTemplatePage);
                updateApplyButtonState();
                return;
            }
            if ("Context Free Grammar".equals(pageName)) {
                if (currentColorSchemeContextFreeGrammarPage == null) {
                    currentColorSchemeContextFreeGrammarPage = new SettingsColorSchemeContextFreeGrammarPage();
                }
                currentColorSchemeContextFreeGrammarPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeContextFreeGrammarPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeContextFreeGrammarPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeContextFreeGrammarPage);
                updateApplyButtonState();
                return;
            }
            if ("CSS".equals(pageName)) {
                if (currentColorSchemeCssPage == null) {
                    currentColorSchemeCssPage = new SettingsColorSchemeCssPage();
                }
                currentColorSchemeCssPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeCssPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeCssPage);
                updateApplyButtonState();
                return;
            }
            if ("Data Editor and Viewer".equals(pageName)) {
                if (currentColorSchemeDataEditorViewerPage == null) {
                    currentColorSchemeDataEditorViewerPage = new SettingsColorSchemeDataEditorViewerPage();
                }
                currentColorSchemeDataEditorViewerPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDataEditorViewerPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeDataEditorViewerPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("General");
                    if (currentColorSchemeGeneralPage != null) {
                        currentColorSchemeGeneralPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeDataEditorViewerPage);
                updateApplyButtonState();
                return;
            }
            if ("Database".equals(pageName)) {
                if (currentColorSchemeDatabasePage == null) {
                    currentColorSchemeDatabasePage = new SettingsColorSchemeDatabasePage();
                }
                currentColorSchemeDatabasePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDatabasePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeDatabasePage);
                updateApplyButtonState();
                return;
            }
            if ("Diagrams".equals(pageName)) {
                if (currentColorSchemeDiagramsPage == null) {
                    currentColorSchemeDiagramsPage = new SettingsColorSchemeDiagramsPage();
                }
                currentColorSchemeDiagramsPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDiagramsPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeDiagramsPage);
                updateApplyButtonState();
                return;
            }
            if ("Dockerfile".equals(pageName)) {
                if (currentColorSchemeDockerfilePage == null) {
                    currentColorSchemeDockerfilePage = new SettingsColorSchemeDockerfilePage();
                }
                currentColorSchemeDockerfilePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDockerfilePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeDockerfilePage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeDockerfilePage);
                updateApplyButtonState();
                return;
            }
            if ("EditorConfig".equals(pageName)) {
                if (currentColorSchemeEditorConfigPage == null) {
                    currentColorSchemeEditorConfigPage = new SettingsColorSchemeEditorConfigPage();
                }
                currentColorSchemeEditorConfigPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeEditorConfigPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeEditorConfigPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeEditorConfigPage);
                updateApplyButtonState();
                return;
            }
            if ("ERB".equals(pageName)) {
                if (currentColorSchemeErbPage == null) {
                    currentColorSchemeErbPage = new SettingsColorSchemeErbPage();
                }
                currentColorSchemeErbPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeErbPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeErbPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeErbPage);
                updateApplyButtonState();
                return;
            }
            if ("FreeMarker".equals(pageName)) {
                if (currentColorSchemeFreeMarkerPage == null) {
                    currentColorSchemeFreeMarkerPage = new SettingsColorSchemeFreeMarkerPage();
                }
                currentColorSchemeFreeMarkerPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeFreeMarkerPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeFreeMarkerPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeFreeMarkerPage);
                updateApplyButtonState();
                return;
            }
            if ("GitLab CI Expression".equals(pageName)) {
                if (currentColorSchemeGitLabCiExpressionPage == null) {
                    currentColorSchemeGitLabCiExpressionPage = new SettingsColorSchemeGitLabCiExpressionPage();
                }
                currentColorSchemeGitLabCiExpressionPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeGitLabCiExpressionPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeGitLabCiExpressionPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeGitLabCiExpressionPage);
                updateApplyButtonState();
                return;
            }
            if ("Go".equals(pageName)) {
                if (currentColorSchemeGoPage == null) {
                    currentColorSchemeGoPage = new SettingsColorSchemeGoPage();
                }
                currentColorSchemeGoPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeGoPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeGoPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeGoPage);
                updateApplyButtonState();
                return;
            }
            if ("Gradle Declarative Configuration".equals(pageName)) {
                if (currentColorSchemeGradleDeclarativePage == null) {
                    currentColorSchemeGradleDeclarativePage = new SettingsColorSchemeGradleDeclarativePage();
                }
                currentColorSchemeGradleDeclarativePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeGradleDeclarativePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeGradleDeclarativePage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeGradleDeclarativePage);
                updateApplyButtonState();
                return;
            }
            if ("Groovy".equals(pageName)) {
                if (currentColorSchemeGroovyPage == null) {
                    currentColorSchemeGroovyPage = new SettingsColorSchemeGroovyPage();
                }
                currentColorSchemeGroovyPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeGroovyPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeGroovyPage.setOnNavigateToInheritedListener(targetKey -> {
                    if (targetKey != null && targetKey.endsWith("(Groovy)")) {
                        String baseKey = targetKey.substring(0, targetKey.indexOf("(Groovy)")).trim();
                        currentColorSchemeGroovyPage.selectCategory(baseKey);
                    } else {
                        selectCategory("Language Defaults");
                        if (currentColorSchemeLanguageDefaultsPage != null) {
                            currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                        }
                    }
                });
                wrapInScroll(currentColorSchemeGroovyPage);
                updateApplyButtonState();
                return;
            }
            if ("HTML".equals(pageName)) {
                if (currentColorSchemeHtmlPage == null) {
                    currentColorSchemeHtmlPage = new SettingsColorSchemeHtmlPage();
                }
                currentColorSchemeHtmlPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeHtmlPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeHtmlPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeHtmlPage);
                updateApplyButtonState();
                return;
            }
            if ("HTTP Request".equals(pageName)) {
                if (currentColorSchemeHttpRequestPage == null) {
                    currentColorSchemeHttpRequestPage = new SettingsColorSchemeHttpRequestPage();
                }
                currentColorSchemeHttpRequestPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeHttpRequestPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeHttpRequestPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeHttpRequestPage);
                updateApplyButtonState();
                return;
            }
            if ("JavaScript".equals(pageName)) {
                if (currentColorSchemeJavaScriptPage == null) {
                    currentColorSchemeJavaScriptPage = new SettingsColorSchemeJavaScriptPage();
                }
                currentColorSchemeJavaScriptPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJavaScriptPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeJavaScriptPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeJavaScriptPage);
                updateApplyButtonState();
                return;
            }
            if ("JPA/Hibernate QL".equals(pageName)) {
                if (currentColorSchemeJpaHibernateQlPage == null) {
                    currentColorSchemeJpaHibernateQlPage = new SettingsColorSchemeJpaHibernateQlPage();
                }
                currentColorSchemeJpaHibernateQlPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJpaHibernateQlPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeJpaHibernateQlPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeJpaHibernateQlPage);
                updateApplyButtonState();
                return;
            }
            if ("JSON".equals(pageName)) {
                if (currentColorSchemeJsonPage == null) {
                    currentColorSchemeJsonPage = new SettingsColorSchemeJsonPage();
                }
                currentColorSchemeJsonPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJsonPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeJsonPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeJsonPage);
                updateApplyButtonState();
                return;
            }
            if ("JSONPath".equals(pageName)) {
                if (currentColorSchemeJsonPathPage == null) {
                    currentColorSchemeJsonPathPage = new SettingsColorSchemeJsonPathPage();
                }
                currentColorSchemeJsonPathPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJsonPathPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeJsonPathPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeJsonPathPage);
                updateApplyButtonState();
                return;
            }
            if ("JSP".equals(pageName)) {
                if (currentColorSchemeJspPage == null) {
                    currentColorSchemeJspPage = new SettingsColorSchemeJspPage();
                }
                currentColorSchemeJspPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJspPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeJspPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeJspPage);
                updateApplyButtonState();
                return;
            }
            if ("Jupyter Notebooks".equals(pageName)) {
                if (currentColorSchemeJupyterNotebooksPage == null) {
                    currentColorSchemeJupyterNotebooksPage = new SettingsColorSchemeJupyterNotebooksPage();
                }
                currentColorSchemeJupyterNotebooksPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJupyterNotebooksPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeJupyterNotebooksPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeJupyterNotebooksPage);
                updateApplyButtonState();
                return;
            }
            if ("Kotlin".equals(pageName)) {
                if (currentColorSchemeKotlinPage == null) {
                    currentColorSchemeKotlinPage = new SettingsColorSchemeKotlinPage();
                }
                currentColorSchemeKotlinPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeKotlinPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeKotlinPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeKotlinPage);
                updateApplyButtonState();
                return;
            }
            if ("Kubernetes".equals(pageName)) {
                if (currentColorSchemeKubernetesPage == null) {
                    currentColorSchemeKubernetesPage = new SettingsColorSchemeKubernetesPage();
                }
                currentColorSchemeKubernetesPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeKubernetesPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeKubernetesPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeKubernetesPage);
                updateApplyButtonState();
                return;
            }
            if ("Less".equals(pageName)) {
                if (currentColorSchemeLessPage == null) {
                    currentColorSchemeLessPage = new SettingsColorSchemeLessPage();
                }
                currentColorSchemeLessPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeLessPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeLessPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("CSS");
                    if (currentColorSchemeCssPage != null) {
                        currentColorSchemeCssPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeLessPage);
                updateApplyButtonState();
                return;
            }
            if ("Lombok Config".equals(pageName)) {
                if (currentColorSchemeLombokConfigPage == null) {
                    currentColorSchemeLombokConfigPage = new SettingsColorSchemeLombokConfigPage();
                }
                currentColorSchemeLombokConfigPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeLombokConfigPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeLombokConfigPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeLombokConfigPage);
                updateApplyButtonState();
                return;
            }
            if ("Markdown".equals(pageName)) {
                if (currentColorSchemeMarkdownPage == null) {
                    currentColorSchemeMarkdownPage = new SettingsColorSchemeMarkdownPage();
                }
                currentColorSchemeMarkdownPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeMarkdownPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeMarkdownPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeMarkdownPage);
                updateApplyButtonState();
                return;
            }
            if ("Micronaut EL".equals(pageName)) {
                if (currentColorSchemeMicronautELPage == null) {
                    currentColorSchemeMicronautELPage = new SettingsColorSchemeMicronautELPage();
                }
                currentColorSchemeMicronautELPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeMicronautELPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeMicronautELPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeMicronautELPage);
                updateApplyButtonState();
                return;
            }
            if ("PHP".equals(pageName)) {
                if (currentColorSchemePhpPage == null) {
                    currentColorSchemePhpPage = new SettingsColorSchemePHPPage();
                }
                currentColorSchemePhpPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemePhpPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemePhpPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemePhpPage);
                updateApplyButtonState();
                return;
            }
            if ("plan9_x86".equals(pageName)) {
                if (currentColorSchemePlan9X86Page == null) {
                    currentColorSchemePlan9X86Page = new SettingsColorSchemePlan9X86Page();
                }
                currentColorSchemePlan9X86Page.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemePlan9X86Page.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemePlan9X86Page.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemePlan9X86Page);
                updateApplyButtonState();
                return;
            }
            if ("PostCSS".equals(pageName)) {
                if (currentColorSchemePostCSSPage == null) {
                    currentColorSchemePostCSSPage = new SettingsColorSchemePostCSSPage();
                }
                currentColorSchemePostCSSPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemePostCSSPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemePostCSSPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("CSS");
                    if (currentColorSchemeCssPage != null) {
                        currentColorSchemeCssPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemePostCSSPage);
                updateApplyButtonState();
                return;
            }
            if ("Protocol Buffer".equals(pageName)) {
                if (currentColorSchemeProtocolBufferPage == null) {
                    currentColorSchemeProtocolBufferPage = new SettingsColorSchemeProtocolBufferPage();
                }
                currentColorSchemeProtocolBufferPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeProtocolBufferPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeProtocolBufferPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeProtocolBufferPage);
                updateApplyButtonState();
                return;
            }
            if ("MongoDB JSON".equals(pageName)) {
                if (currentColorSchemeMongoDbJsonPage == null) {
                    currentColorSchemeMongoDbJsonPage = new SettingsColorSchemeMongoDBJSONPage();
                }
                currentColorSchemeMongoDbJsonPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeMongoDbJsonPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeMongoDbJsonPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeMongoDbJsonPage);
                updateApplyButtonState();
                return;
            }
            if ("Properties".equals(pageName)) {
                if (currentColorSchemePropertiesPage == null) {
                    currentColorSchemePropertiesPage = new SettingsColorSchemePropertiesPage();
                }
                currentColorSchemePropertiesPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemePropertiesPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemePropertiesPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemePropertiesPage);
                updateApplyButtonState();
                return;
            }
            if ("Protocol Buffer Text".equals(pageName)) {
                if (currentColorSchemeProtocolBufferTextPage == null) {
                    currentColorSchemeProtocolBufferTextPage = new SettingsColorSchemeProtocolBufferTextPage();
                }
                currentColorSchemeProtocolBufferTextPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeProtocolBufferTextPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeProtocolBufferTextPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeProtocolBufferTextPage);
                updateApplyButtonState();
                return;
            }
            if ("Python".equals(pageName)) {
                if (currentColorSchemePythonPage == null) {
                    currentColorSchemePythonPage = new SettingsColorSchemePythonPage();
                }
                currentColorSchemePythonPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemePythonPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemePythonPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemePythonPage);
                updateApplyButtonState();
                return;
            }
            if ("Qute".equals(pageName)) {
                if (currentColorSchemeQutePage == null) {
                    currentColorSchemeQutePage = new SettingsColorSchemeQutePage();
                }
                currentColorSchemeQutePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeQutePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeQutePage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeQutePage);
                updateApplyButtonState();
                return;
            }
            if ("RDoc".equals(pageName)) {
                if (currentColorSchemeRDocPage == null) {
                    currentColorSchemeRDocPage = new SettingsColorSchemeRDocPage();
                }
                currentColorSchemeRDocPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeRDocPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeRDocPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeRDocPage);
                updateApplyButtonState();
                return;
            }
            if ("RegExp".equals(pageName)) {
                if (currentColorSchemeRegExpPage == null) {
                    currentColorSchemeRegExpPage = new SettingsColorSchemeRegExpPage();
                }
                currentColorSchemeRegExpPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeRegExpPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeRegExpPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeRegExpPage);
                updateApplyButtonState();
                return;
            }
            if ("Ruby".equals(pageName)) {
                if (currentColorSchemeRubyPage == null) {
                    currentColorSchemeRubyPage = new SettingsColorSchemeRubyPage();
                }
                currentColorSchemeRubyPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeRubyPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeRubyPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeRubyPage);
                updateApplyButtonState();
                return;
            }
            if ("Rust".equals(pageName)) {
                if (currentColorSchemeRustPage == null) {
                    currentColorSchemeRustPage = new SettingsColorSchemeRustPage();
                }
                currentColorSchemeRustPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeRustPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeRustPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeRustPage);
                updateApplyButtonState();
                return;
            }
            if ("Sass/SCSS".equals(pageName)) {
                if (currentColorSchemeSassPage == null) {
                    currentColorSchemeSassPage = new SettingsColorSchemeSassPage();
                }
                currentColorSchemeSassPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeSassPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeSassPage.setOnNavigateToInheritedListener(targetKey -> {
                    AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(currentColorSchemeSassPage.getSelectedKey());
                    String scope = desc != null ? desc.getInheritScope() : "(CSS)";
                    if (scope != null && scope.contains("CSS")) {
                        selectCategory("CSS");
                        if (currentColorSchemeCssPage != null) {
                            currentColorSchemeCssPage.selectTreeItem(targetKey);
                        }
                    } else {
                        selectCategory("Language Defaults");
                        if (currentColorSchemeLanguageDefaultsPage != null) {
                            currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                        }
                    }
                });
                wrapInScroll(currentColorSchemeSassPage);
                updateApplyButtonState();
                return;
            }
            if ("Scala".equals(pageName)) {
                if (currentColorSchemeScalaPage == null) {
                    currentColorSchemeScalaPage = new SettingsColorSchemeScalaPage();
                }
                currentColorSchemeScalaPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeScalaPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeScalaPage.setOnNavigateToInheritedListener(targetKey -> {
                    AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(currentColorSchemeScalaPage.getSelectedKey());
                    String scope = desc != null ? desc.getInheritScope() : "(Java)";
                    if (scope != null && scope.contains("Java")) {
                        selectCategory("Java");
                        if (currentColorSchemeJavaPage != null) {
                            currentColorSchemeJavaPage.selectTreeItem(targetKey);
                        }
                    } else {
                        selectCategory("Language Defaults");
                        if (currentColorSchemeLanguageDefaultsPage != null) {
                            currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                        }
                    }
                });
                wrapInScroll(currentColorSchemeScalaPage);
                updateApplyButtonState();
                return;
            }
            if ("Shell Script".equals(pageName)) {
                if (currentColorSchemeShellScriptPage == null) {
                    currentColorSchemeShellScriptPage = new SettingsColorSchemeShellScriptPage();
                }
                currentColorSchemeShellScriptPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeShellScriptPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeShellScriptPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeShellScriptPage);
                updateApplyButtonState();
                return;
            }
            if ("Smarty".equals(pageName)) {
                if (currentColorSchemeSmartyPage == null) {
                    currentColorSchemeSmartyPage = new SettingsColorSchemeSmartyPage();
                }
                currentColorSchemeSmartyPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeSmartyPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeSmartyPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeSmartyPage);
                updateApplyButtonState();
                return;
            }
            if ("Spring EL".equals(pageName)) {
                if (currentColorSchemeSpringELPage == null) {
                    currentColorSchemeSpringELPage = new SettingsColorSchemeSpringELPage();
                }
                currentColorSchemeSpringELPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeSpringELPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeSpringELPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeSpringELPage);
                updateApplyButtonState();
                return;
            }
            if ("SQL".equals(pageName)) {
                if (currentColorSchemeSQLPage == null) {
                    currentColorSchemeSQLPage = new SettingsColorSchemeSQLPage();
                }
                currentColorSchemeSQLPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeSQLPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeSQLPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeSQLPage);
                updateApplyButtonState();
                return;
            }
            if ("Table Diff".equals(pageName)) {
                if (currentColorSchemeTableDiffPage == null) {
                    currentColorSchemeTableDiffPage = new SettingsColorSchemeTableDiffPage();
                }
                currentColorSchemeTableDiffPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeTableDiffPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeTableDiffPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Color Scheme", "General");
                    if (currentColorSchemeGeneralPage != null) {
                        currentColorSchemeGeneralPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeTableDiffPage);
                updateApplyButtonState();
                return;
            }
            if ("TOML".equals(pageName)) {
                if (currentColorSchemeTomlPage == null) {
                    currentColorSchemeTomlPage = new SettingsColorSchemeTOMLPage();
                }
                currentColorSchemeTomlPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeTomlPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeTomlPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeTomlPage);
                updateApplyButtonState();
                return;
            }
            if ("TypeScript".equals(pageName)) {
                if (currentColorSchemeTypeScriptPage == null) {
                    currentColorSchemeTypeScriptPage = new SettingsColorSchemeTypeScriptPage();
                }
                currentColorSchemeTypeScriptPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeTypeScriptPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeTypeScriptPage.setOnNavigateToInheritedListener(targetKey -> {
                    if (targetKey.contains("Static property")) {
                        currentColorSchemeTypeScriptPage.selectTreeItem(targetKey);
                    } else {
                        selectCategory("Language Defaults");
                        if (currentColorSchemeLanguageDefaultsPage != null) {
                            currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                        }
                    }
                });
                wrapInScroll(currentColorSchemeTypeScriptPage);
                updateApplyButtonState();
                return;
            }
            if ("Velocity".equals(pageName)) {
                if (currentColorSchemeVelocityPage == null) {
                    currentColorSchemeVelocityPage = new SettingsColorSchemeVelocityPage();
                }
                currentColorSchemeVelocityPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeVelocityPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeVelocityPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeVelocityPage);
                updateApplyButtonState();
                return;
            }
            if ("XML".equals(pageName)) {
                if (currentColorSchemeXmlPage == null) {
                    currentColorSchemeXmlPage = new SettingsColorSchemeXMLPage();
                }
                currentColorSchemeXmlPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeXmlPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeXmlPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeXmlPage);
                updateApplyButtonState();
                return;
            }
            if ("XPath".equals(pageName)) {
                if (currentColorSchemeXPathPage == null) {
                    currentColorSchemeXPathPage = new SettingsColorSchemeXPathPage();
                }
                currentColorSchemeXPathPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeXPathPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeXPathPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeXPathPage);
                updateApplyButtonState();
                return;
            }
            if ("XSLT".equals(pageName)) {
                if (currentColorSchemeXsltPage == null) {
                    currentColorSchemeXsltPage = new SettingsColorSchemeXSLTPage();
                }
                currentColorSchemeXsltPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeXsltPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeXsltPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeXsltPage);
                updateApplyButtonState();
                return;
            }
            if ("YAML".equals(pageName)) {
                if (currentColorSchemeYamlPage == null) {
                    currentColorSchemeYamlPage = new SettingsColorSchemeYAMLPage();
                }
                currentColorSchemeYamlPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeYamlPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeYamlPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeYamlPage);
                updateApplyButtonState();
                return;
            }
            if ("By Scope".equals(pageName)) {
                if (currentColorSchemeByScopePage == null) {
                    currentColorSchemeByScopePage = new SettingsColorSchemeByScopePage();
                }
                currentColorSchemeByScopePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeByScopePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeByScopePage.setOnManageScopesListener(() -> selectCategory("Scopes"));
                wrapInScroll(currentColorSchemeByScopePage);
                updateApplyButtonState();
                return;
            }
            if ("Images".equals(pageName)) {
                if (currentColorSchemeImagesPage == null) {
                    currentColorSchemeImagesPage = new SettingsColorSchemeImagesPage();
                }
                currentColorSchemeImagesPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeImagesPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeImagesPage);
                updateApplyButtonState();
                return;
            }
            buildColorSchemePage(pageName);
            return;
        }

        // Code Style and its language subpages
        if (underCodeStyle || "Code Style".equals(pageName)) {
            if ("Code Style".equals(pageName)) {
                if (currentCodeStylePage == null) {
                    currentCodeStylePage = new SettingsCodeStylePage();
                }
                currentCodeStylePage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentCodeStylePage);
                updateApplyButtonState();
                return;
            }
            if ("Java".equals(pageName)) {
                currentCodeStyleJavaPage = new SettingsCodeStyleJavaPage();
                currentCodeStyleJavaPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentCodeStyleJavaPage);
                updateApplyButtonState();
                return;
            }
            currentCodeStyleLanguagePage = new SettingsCodeStyleLanguagePage(pageName);
            currentCodeStyleLanguagePage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentCodeStyleLanguagePage);
            updateApplyButtonState();
            return;
        }

        // 5. Editor and subpages
        if ((underEditorGroup || "Editor".equals(pageName)) && !isUnderBuild(selected) && !underLanguages) {
            if ("General".equals(pageName)) {
                if (currentEditorGeneralPage == null) {
                    currentEditorGeneralPage = new SettingsEditorGeneralPage();
                }
                currentEditorGeneralPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentEditorGeneralPage);
                updateApplyButtonState();
                return;
            }
            if ("Auto Import".equals(pageName)) {
                if (currentAutoImportPage == null) {
                    currentAutoImportPage = new SettingsAutoImportPage();
                }
                currentAutoImportPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentAutoImportPage);
                updateApplyButtonState();
                return;
            }
            if ("Appearance".equals(pageName)) {
                if (currentEditorAppearancePage == null) {
                    currentEditorAppearancePage = new SettingsAppearancePage();
                }
                currentEditorAppearancePage.setOnModifiedListener(this::updateApplyButtonState);
                currentEditorAppearancePage.setOnNavigateReaderMode(() -> {
                    TreeItem<String> readerModeItem = findItem(tree.getRoot(), "Reader Mode");
                    if (readerModeItem != null) {
                        expandAncestors(readerModeItem);
                        tree.getSelectionModel().select(readerModeItem);
                    }
                });
                wrapInScroll(currentEditorAppearancePage);
                updateApplyButtonState();
                return;
            }
            if ("Breadcrumbs".equals(pageName)) {
                if (currentBreadcrumbsPage == null) {
                    currentBreadcrumbsPage = new SettingsBreadcrumbsPage();
                }
                currentBreadcrumbsPage.setOnModifiedListener(this::updateApplyButtonState);
                currentBreadcrumbsPage.setOnManageColors(() -> {
                    TreeItem<String> csRoot = findItem(tree.getRoot(), "Color Scheme");
                    if (csRoot != null) {
                        TreeItem<String> csGeneral = findItem(csRoot, "General");
                        if (csGeneral != null) {
                            expandAncestors(csGeneral);
                            tree.getSelectionModel().select(csGeneral);
                        }
                    }
                });
                wrapInScroll(currentBreadcrumbsPage);
                updateApplyButtonState();
                return;
            }
            if ("Code Completion".equals(pageName)) {
                if (currentCodeCompletionPage == null) {
                    currentCodeCompletionPage = new SettingsCodeCompletionPage();
                }
                currentCodeCompletionPage.setOnModifiedListener(this::updateApplyButtonState);
                currentCodeCompletionPage.setOnNavigateInlineCompletion(() -> {
                    TreeItem<String> item = findItem(tree.getRoot(), "Inline Completion");
                    if (item != null) {
                        expandAncestors(item);
                        tree.getSelectionModel().select(item);
                    }
                });
                wrapInScroll(currentCodeCompletionPage);
                updateApplyButtonState();
                return;
            }
            if ("Code Folding".equals(pageName)) {
                if (currentCodeFoldingPage == null) {
                    currentCodeFoldingPage = new SettingsCodeFoldingPage();
                }
                currentCodeFoldingPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentCodeFoldingPage);
                updateApplyButtonState();
                return;
            }
            if ("Console".equals(pageName)) {
                if (currentConsolePage == null) {
                    currentConsolePage = new SettingsConsolePage();
                }
                currentConsolePage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentConsolePage);
                updateApplyButtonState();
                return;
            }
            if ("Editor Tabs".equals(pageName)) {
                if (currentEditorTabsPage == null) {
                    currentEditorTabsPage = new SettingsEditorTabsPage();
                }
                currentEditorTabsPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentEditorTabsPage);
                updateApplyButtonState();
                return;
            }
            if ("Gutter Icons".equals(pageName)) {
                if (currentGutterIconsPage == null) {
                    currentGutterIconsPage = new SettingsGutterIconsPage();
                }
                currentGutterIconsPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentGutterIconsPage);
                updateApplyButtonState();
                return;
            }
            if ("Inline Completion".equals(pageName)) {
                if (currentInlineCompletionPage == null) {
                    currentInlineCompletionPage = new SettingsInlineCompletionPage();
                }
                currentInlineCompletionPage.setOnModifiedListener(this::updateApplyButtonState);
                currentInlineCompletionPage.setOnNavigateCodeCompletion(() -> {
                    TreeItem<String> item = findItem(tree.getRoot(), "Code Completion");
                    if (item != null) {
                        expandAncestors(item);
                        tree.getSelectionModel().select(item);
                    }
                });
                wrapInScroll(currentInlineCompletionPage);
                updateApplyButtonState();
                return;
            }
            if ("Postfix Completion".equals(pageName)) {
                if (currentPostfixCompletionPage == null) {
                    currentPostfixCompletionPage = new SettingsPostfixCompletionPage();
                }
                currentPostfixCompletionPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentPostfixCompletionPage);
                updateApplyButtonState();
                return;
            }
            if ("Smart Keys".equals(pageName)) {
                if (currentSmartKeysPage == null) {
                    currentSmartKeysPage = new SettingsSmartKeysPage();
                }
                currentSmartKeysPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysPage);
                updateApplyButtonState();
                return;
            }
            if (underSmartKeys) {
                if ("YAML".equals(pageName)) {
                    if (currentSmartKeysYamlPage == null) {
                        currentSmartKeysYamlPage = new SettingsSmartKeysYAMLPage();
                    }
                    currentSmartKeysYamlPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysYamlPage);
                    updateApplyButtonState();
                    return;
                }
                if ("HTML/CSS".equals(pageName)) {
                    if (currentSmartKeysHtmlCssPage == null) {
                        currentSmartKeysHtmlCssPage = new SettingsSmartKeysHTMLCSSPage();
                    }
                    currentSmartKeysHtmlCssPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysHtmlCssPage);
                    updateApplyButtonState();
                    return;
                }
                if ("Python".equals(pageName)) {
                    if (currentSmartKeysPythonPage == null) {
                        currentSmartKeysPythonPage = new SettingsSmartKeysPythonPage();
                    }
                    currentSmartKeysPythonPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysPythonPage);
                    updateApplyButtonState();
                    return;
                }
                if ("JSON".equals(pageName)) {
                    if (currentSmartKeysJsonPage == null) {
                        currentSmartKeysJsonPage = new SettingsSmartKeysJSONPage();
                    }
                    currentSmartKeysJsonPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysJsonPage);
                    updateApplyButtonState();
                    return;
                }
                if ("Rust".equals(pageName)) {
                    if (currentSmartKeysRustPage == null) {
                        currentSmartKeysRustPage = new SettingsSmartKeysRustPage();
                    }
                    currentSmartKeysRustPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysRustPage);
                    updateApplyButtonState();
                    return;
                }
                if ("Markdown".equals(pageName)) {
                    if (currentSmartKeysMarkdownPage == null) {
                        currentSmartKeysMarkdownPage = new SettingsSmartKeysMarkdownPage();
                    }
                    currentSmartKeysMarkdownPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysMarkdownPage);
                    updateApplyButtonState();
                    return;
                }
                if ("Scala".equals(pageName)) {
                    if (currentSmartKeysScalaPage == null) {
                        currentSmartKeysScalaPage = new SettingsSmartKeysScalaPage();
                    }
                    currentSmartKeysScalaPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysScalaPage);
                    updateApplyButtonState();
                    return;
                }
                if ("SQL".equals(pageName)) {
                    if (currentSmartKeysSqlPage == null) {
                        currentSmartKeysSqlPage = new SettingsSmartKeysSQLPage();
                    }
                    currentSmartKeysSqlPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysSqlPage);
                    updateApplyButtonState();
                    return;
                }
                if ("Ruby".equals(pageName)) {
                    if (currentSmartKeysRubyPage == null) {
                        currentSmartKeysRubyPage = new SettingsSmartKeysRubyPage();
                    }
                    currentSmartKeysRubyPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysRubyPage);
                    updateApplyButtonState();
                    return;
                }
                if ("JavaScript".equals(pageName)) {
                    if (currentSmartKeysJsPage == null) {
                        currentSmartKeysJsPage = new SettingsSmartKeysJavaScriptPage();
                    }
                    currentSmartKeysJsPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysJsPage);
                    updateApplyButtonState();
                    return;
                }
                if ("PHP".equals(pageName)) {
                    if (currentSmartKeysPhpPage == null) {
                        currentSmartKeysPhpPage = new SettingsSmartKeysPHPPage();
                    }
                    currentSmartKeysPhpPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentSmartKeysPhpPage);
                    updateApplyButtonState();
                    return;
                }
            }
            if ("Sticky Lines".equals(pageName)) {
                if (currentStickyLinesPage == null) {
                    currentStickyLinesPage = new SettingsStickyLinesPage();
                }
                currentStickyLinesPage.setOnModifiedListener(this::updateApplyButtonState);
                currentStickyLinesPage.setOnManageColors(() -> {
                    TreeItem<String> csRoot = findItem(tree.getRoot(), "Color Scheme");
                    if (csRoot != null) {
                        TreeItem<String> csGeneral = findItem(csRoot, "General");
                        if (csGeneral != null) {
                            expandAncestors(csGeneral);
                            tree.getSelectionModel().select(csGeneral);
                        }
                    }
                });
                wrapInScroll(currentStickyLinesPage);
                updateApplyButtonState();
                return;
            }
            if ("Code Editing".equals(pageName)) {
                if (currentCodeEditingPage == null) {
                    currentCodeEditingPage = new SettingsCodeEditingPage();
                }
                currentCodeEditingPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentCodeEditingPage);
                updateApplyButtonState();
                return;
            }
            if ("Font".equals(pageName)) {
                if (currentFontPage == null) {
                    currentFontPage = new SettingsFontPage();
                }
                currentFontPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentFontPage);
                updateApplyButtonState();
                return;
            }
            if ("Color Scheme".equals(pageName)) {
                if (currentColorSchemePage == null) {
                    currentColorSchemePage = new SettingsColorSchemePage();
                }
                currentColorSchemePage.setOnNavigate(cat -> {
                    TreeItem<String> csRoot = findItem(tree.getRoot(), "Color Scheme");
                    if (csRoot != null) {
                        TreeItem<String> item = findItem(csRoot, cat);
                        if (item != null) {
                            expandAncestors(item);
                            tree.getSelectionModel().select(item);
                        }
                    }
                });
                currentColorSchemePage.getHeaderBar().setOnNavigateToTheme(() -> {
                    TreeItem<String> appRoot = findItem(tree.getRoot(), "Appearance & Behavior");
                    if (appRoot != null) {
                        TreeItem<String> appItem = findItem(appRoot, "Appearance");
                        if (appItem != null) {
                            expandAncestors(appItem);
                            tree.getSelectionModel().select(appItem);
                        }
                    }
                });
                currentColorSchemePage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentColorSchemePage);
                updateApplyButtonState();
                return;
            }
            if ("Inspections".equals(pageName)) {
                if (currentInspectionsPage == null) {
                    currentInspectionsPage = new SettingsInspectionsPage();
                }
                currentInspectionsPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentInspectionsPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentInspectionsPage);
                updateApplyButtonState();
                return;
            }
            if ("File and Code Templates".equals(pageName)) {
                if (currentFileAndCodeTemplatesPage == null) {
                    currentFileAndCodeTemplatesPage = new SettingsFileAndCodeTemplatesPage();
                }
                currentFileAndCodeTemplatesPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentFileAndCodeTemplatesPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentFileAndCodeTemplatesPage);
                updateApplyButtonState();
                return;
            }
            if ("File Encodings".equals(pageName)) {
                if (currentFileEncodingsPage == null) {
                    currentFileEncodingsPage = new SettingsFileEncodingsPage();
                }
                currentFileEncodingsPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentFileEncodingsPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentFileEncodingsPage);
                updateApplyButtonState();
                return;
            }
            if ("Live Templates".equals(pageName)) {
                if (currentLiveTemplatesPage == null) {
                    currentLiveTemplatesPage = new SettingsLiveTemplatesPage();
                }
                currentLiveTemplatesPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentLiveTemplatesPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentLiveTemplatesPage);
                updateApplyButtonState();
                return;
            }
            if ("File Types".equals(pageName)) {
                if (currentFileTypesPage == null) {
                    currentFileTypesPage = new SettingsFileTypesPage();
                }
                currentFileTypesPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentFileTypesPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentFileTypesPage);
                updateApplyButtonState();
                return;
            }
            if ("Copyright".equals(pageName)) {
                if (currentCopyrightPage == null) {
                    currentCopyrightPage = new SettingsCopyrightPage();
                    currentCopyrightPage.setOnNavigateToScopes(() -> {
                        TreeItem<String> scopesItem = findItem(tree.getRoot(), "Scopes");
                        if (scopesItem != null) {
                            expandAncestors(scopesItem);
                            tree.getSelectionModel().select(scopesItem);
                        }
                    });
                }
                currentCopyrightPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentCopyrightPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentCopyrightPage);
                updateApplyButtonState();
                return;
            }
            if ("Copyright Profiles".equals(pageName)) {
                if (currentCopyrightProfilesPage == null) {
                    currentCopyrightProfilesPage = new SettingsCopyrightProfilesPage();
                }
                currentCopyrightProfilesPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentCopyrightProfilesPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentCopyrightProfilesPage);
                updateApplyButtonState();
                return;
            }
            if ("Formatting".equals(pageName) && (underCopyright || "Copyright".equals(selected.getParent() != null ? selected.getParent().getValue() : ""))) {
                if (currentCopyrightFormattingPage == null) {
                    currentCopyrightFormattingPage = new SettingsCopyrightFormattingPage();
                }
                currentCopyrightFormattingPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentCopyrightFormattingPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentCopyrightFormattingPage);
                updateApplyButtonState();
                return;
            }
            if (underFormatting) {
                SettingsCopyrightFormattingLanguagePage langPage =
                        languageFormattingPages.computeIfAbsent(pageName, k -> {
                            SettingsCopyrightFormattingLanguagePage p = new SettingsCopyrightFormattingLanguagePage(k);
                            p.setOnModifiedListener(this::updateApplyButtonState);
                            return p;
                        });
                VBox.setVgrow(langPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(langPage);
                updateApplyButtonState();
                return;
            }
            if ("Inlay Hints".equals(pageName)) {
                if (currentInlayHintsPage == null) {
                    currentInlayHintsPage = new SettingsInlayHintsPage();
                }
                currentInlayHintsPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentInlayHintsPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentInlayHintsPage);
                updateApplyButtonState();
                return;
            }
            if ("Duplicates".equals(pageName)) {
                if (currentDuplicatesPage == null) {
                    currentDuplicatesPage = new SettingsDuplicatesPage();
                }
                currentDuplicatesPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentDuplicatesPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentDuplicatesPage);
                updateApplyButtonState();
                return;
            }
            if ("Emmet".equals(pageName)) {
                if (currentEmmetPage == null) {
                    currentEmmetPage = new SettingsEmmetPage();
                }
                currentEmmetPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentEmmetPage);
                updateApplyButtonState();
                return;
            }
            if (underEmmet) {
                if ("CSS".equals(pageName)) {
                    if (currentEmmetCssPage == null) {
                        currentEmmetCssPage = new SettingsEmmetCssPage();
                    }
                    currentEmmetCssPage.setOnModifiedListener(this::updateApplyButtonState);
                    VBox.setVgrow(currentEmmetCssPage, Priority.ALWAYS);
                    contentContainer.setStyle("-fx-background-color: #1E1F22;");
                    contentContainer.getChildren().setAll(currentEmmetCssPage);
                    updateApplyButtonState();
                    return;
                }
                if ("HTML".equals(pageName)) {
                    if (currentEmmetHtmlPage == null) {
                        currentEmmetHtmlPage = new SettingsEmmetHtmlPage();
                    }
                    currentEmmetHtmlPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentEmmetHtmlPage);
                    updateApplyButtonState();
                    return;
                }
                if ("JSX".equals(pageName)) {
                    if (currentEmmetJsxPage == null) {
                        currentEmmetJsxPage = new SettingsEmmetJsxPage();
                    }
                    currentEmmetJsxPage.setOnModifiedListener(this::updateApplyButtonState);
                    wrapInScroll(currentEmmetJsxPage);
                    updateApplyButtonState();
                    return;
                }
            }
            if ("Intentions".equals(pageName)) {
                if (currentIntentionsPage == null) {
                    currentIntentionsPage = new SettingsIntentionsPage();
                }
                currentIntentionsPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentIntentionsPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentIntentionsPage);
                updateApplyButtonState();
                return;
            }
            if ("Language Injections".equals(pageName)) {
                if (currentLanguageInjectionsPage == null) {
                    currentLanguageInjectionsPage = new SettingsLanguageInjectionsPage();
                }
                currentLanguageInjectionsPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentLanguageInjectionsPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentLanguageInjectionsPage);
                updateApplyButtonState();
                return;
            }
            if ("Advanced".equals(pageName)) {
                if (currentLanguageInjectionsAdvancedPage == null) {
                    currentLanguageInjectionsAdvancedPage = new SettingsLanguageInjectionsAdvancedPage();
                }
                currentLanguageInjectionsAdvancedPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentLanguageInjectionsAdvancedPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentLanguageInjectionsAdvancedPage);
                updateApplyButtonState();
                return;
            }
            if ("Natural Languages".equals(pageName)) {
                if (currentNaturalLanguagesPage == null) {
                    currentNaturalLanguagesPage = new SettingsNaturalLanguagesPage();
                }
                currentNaturalLanguagesPage.setOnModifiedListener(this::updateApplyButtonState);
                currentNaturalLanguagesPage.setOnNavigateToInspections(() -> {
                    TreeItem<String> inspItem = findItem(tree.getRoot(), "Inspections");
                    if (inspItem != null) {
                        expandAncestors(inspItem);
                        tree.getSelectionModel().select(inspItem);
                    }
                });
                VBox.setVgrow(currentNaturalLanguagesPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentNaturalLanguagesPage);
                updateApplyButtonState();
                return;
            }
            if ("Grammar and Style".equals(pageName)) {
                if (currentGrammarAndStylePage == null) {
                    currentGrammarAndStylePage = new SettingsGrammarAndStylePage();
                }
                currentGrammarAndStylePage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentGrammarAndStylePage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentGrammarAndStylePage);
                updateApplyButtonState();
                return;
            }
            if ("Spelling".equals(pageName)) {
                if (currentSpellingPage == null) {
                    currentSpellingPage = new SettingsSpellingPage();
                }
                currentSpellingPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentSpellingPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentSpellingPage);
                updateApplyButtonState();
                return;
            }
            if ("Reader Mode".equals(pageName)) {
                if (currentReaderModePage == null) {
                    currentReaderModePage = new SettingsReaderModePage();
                }
                currentReaderModePage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentReaderModePage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentReaderModePage);
                updateApplyButtonState();
                return;
            }
            if ("TextMate Bundles".equals(pageName)) {
                if (currentTextMateBundlesPage == null) {
                    currentTextMateBundlesPage = new SettingsTextMateBundlesPage();
                }
                currentTextMateBundlesPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentTextMateBundlesPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentTextMateBundlesPage);
                updateApplyButtonState();
                return;
            }
            if ("TODO".equals(pageName)) {
                if (currentTodoPage == null) {
                    currentTodoPage = new SettingsTodoPage();
                }
                currentTodoPage.setOnModifiedListener(this::updateApplyButtonState);
                VBox.setVgrow(currentTodoPage, Priority.ALWAYS);
                contentContainer.setStyle("-fx-background-color: #1E1F22;");
                contentContainer.getChildren().setAll(currentTodoPage);
                updateApplyButtonState();
                return;
            }
            buildEditorPage(pageName);
            return;
        }

        // 6. Other Appearance & Behavior or root pages
        if ("Menus and Toolbars".equals(pageName)) {
            buildMenusToolbarsPage();
        } else if ("System Settings".equals(pageName)) {
            buildSystemSettingsPage();
        } else if (isSystemSettingsSubPage(pageName)) {
            buildSystemSettingsSubPage(pageName);
        } else if ("File Colors".equals(pageName)) {
            buildFileColorsPage();
        } else if ("Scopes".equals(pageName)) {
            buildScopesPage();
        } else if ("Notifications".equals(pageName)) {
            buildNotificationsPage();
        } else if ("Data Editor and Viewer".equals(pageName)) {
            buildDataEditorPage();
        } else if ("Quick Lists".equals(pageName)) {
            buildQuickListsPage();
        } else if ("Plugins".equals(pageName)) {
            buildPluginsPage();
        } else if ("Required Plugins".equals(pageName)) {
            buildRequiredPluginsPage();
        } else if ("Trusted Locations".equals(pageName)) {
            buildTrustedLocationsPage();
        } else if ("Path Variables".equals(pageName)) {
            buildPathVariablesPage();
        } else if ("Presentation Assistant".equals(pageName)) {
            buildPresentationAssistantPage();
        } else if (isKeymapPage(pageName)) {
            buildKeymapPage();
        } else if ("Changelists".equals(pageName)) {
            buildVcsChangelistsPage();
        } else if ("Commit".equals(pageName)) {
            buildVcsCommitPage();
        } else if ("Confirmation".equals(pageName)) {
            buildVcsConfirmationPage();
        } else if ("Directory Mappings".equals(pageName)) {
            buildVcsDirectoryMappingsPage();
        } else if ("File Status Colors".equals(pageName)) {
            buildVcsFileStatusColorsPage();
        } else if ("Issue Navigation".equals(pageName)) {
            buildVcsIssueNavigationPage();
        } else if ("Log".equals(pageName)) {
            buildVcsLogPage();
        } else if ("Shelf".equals(pageName)) {
            buildVcsShelfPage();
        } else if ("Git".equals(pageName)) {
            buildVcsGitPage();
        } else if ("GitHub".equals(pageName)) {
            buildVcsGitHubPage();
        } else if ("GitLab".equals(pageName)) {
            buildVcsGitLabPage();
        } else if ("Mercurial".equals(pageName)) {
            buildVcsMercurialPage();
        } else if ("Perforce".equals(pageName)) {
            buildVcsPerforcePage();
        } else if ("Perforce MCP".equals(pageName)) {
            buildVcsPerforceMcpPage();
        } else if ("Subversion".equals(pageName)) {
            buildVcsSubversionPage();
        } else if ("Network".equals(pageName) && isUnderSubversion(selected)) {
            buildVcsSubversionNetworkPage();
        } else if ("Presentation".equals(pageName) && isUnderSubversion(selected)) {
            buildVcsSubversionPresentationPage();
        } else if ("SSH".equals(pageName) && isUnderSubversion(selected)) {
            buildVcsSubversionSshPage();
        } else if ("Terminal".equals(pageName)) {
            buildTerminalSettingsPage();
        } else if ("Python Debugger".equals(pageName)) {
            buildPythonDebuggerPage();
        } else if ("Debugger".equals(pageName) && !underColorScheme) {
            buildDebuggerPage();
        } else if ("Async Stack Traces".equals(pageName)) {
            buildDebuggerAsyncStackTracesPage();
        } else if ("Data Views".equals(pageName)) {
            buildDebuggerDataViewsPage();
        } else if ("Java".equals(pageName) && isUnderDataViews(selected)) {
            buildDebuggerDataViewsJavaPage();
        } else if ("Java Type Renderers".equals(pageName)) {
            buildDebuggerDataViewsTypeRenderersPage();
        } else if ("JavaScript".equals(pageName) && isUnderDataViews(selected)) {
            buildDebuggerDataViewsJavaScriptPage();
        } else if ("HotSwap".equals(pageName) && isUnderDebugger(selected)) {
            buildDebuggerHotSwapPage();
        } else if ("Stepping".equals(pageName) && isUnderDebugger(selected)) {
            buildDebuggerSteppingPage();
        } else if ("Application Servers".equals(pageName)) {
            buildApplicationServersPage();
        } else if ("Build Tools".equals(pageName)) {
            buildBuildToolsPage();
        } else if ("Maven".equals(pageName)) {
            buildMavenPage();
        } else if ("Archetype Catalogs".equals(pageName) && isUnderMaven(selected)) {
            buildMavenArchetypeCatalogsPage();
        } else if ("Ignored Files".equals(pageName) && isUnderMaven(selected)) {
            buildMavenIgnoredFilesPage();
        } else if ("Importing".equals(pageName) && isUnderMaven(selected)) {
            buildMavenImportingPage();
        } else if ("Repositories".equals(pageName) && isUnderMaven(selected)) {
            buildMavenRepositoriesPage();
        } else if ("Runner".equals(pageName) && isUnderMaven(selected)) {
            buildMavenRunnerPage();
        } else if ("Running Tests".equals(pageName) && isUnderMaven(selected)) {
            buildMavenRunningTestsPage();
        } else if ("Gradle".equals(pageName) && isUnderBuildTools(selected)) {
            buildGradlePage();
        } else if ("Gant".equals(pageName) && isUnderBuildTools(selected)) {
            buildGantPage();
        } else if ("BSP".equals(pageName) && isUnderBuildTools(selected)) {
            buildBspPage();
        } else if ("Cargo".equals(pageName) && isUnderBuildTools(selected)) {
            buildCargoPage();
        } else if ("sbt".equals(pageName) && isUnderBuildTools(selected)) {
            buildSbtPage();
        } else if ("Compiler".equals(pageName)) {
            buildCompilerPage();
        } else if ("Annotation Processors".equals(pageName) && isUnderCompiler(selected)) {
            buildAnnotationProcessorsPage();
        } else if ("Excludes".equals(pageName) && isUnderCompiler(selected)) {
            buildCompilerExcludesPage();
        } else if ("Groovy Compiler".equals(pageName) && isUnderCompiler(selected)) {
            buildGroovyCompilerPage();
        } else if ("Java Compiler".equals(pageName) && isUnderCompiler(selected)) {
            buildJavaCompilerPage();
        } else if ("Kotlin Compiler".equals(pageName) && isUnderCompiler(selected)) {
            buildKotlinCompilerPage();
        } else if ("RMI Compiler".equals(pageName) && isUnderCompiler(selected)) {
            buildRmiCompilerPage();
        } else if ("Scala Compiler".equals(pageName) && isUnderCompiler(selected)) {
            buildScalaCompilerPage();
        } else if ("Bytecode Indices".equals(pageName) && isUnderCompiler(selected)) {
            buildScalaBytecodeIndicesPage();
        } else if ("Scala Compile Server".equals(pageName) && isUnderCompiler(selected)) {
            buildScalaCompileServerPage();
        } else if ("Validation".equals(pageName) && isUnderCompiler(selected)) {
            buildValidationPage();
        } else if ("Console".equals(pageName) && isUnderDocker(selected)) {
            buildDockerConsolePage();
        } else if ("Console".equals(pageName) && isUnderBuild(selected)) {
            buildBuildConsolePage();
        } else if ("Python Console".equals(pageName)) {
            buildPythonConsolePage();
        } else if ("Coverage".equals(pageName) && isUnderBuild(selected)) {
            buildCoveragePage();
        } else if ("Deployment".equals(pageName) && isUnderBuild(selected)) {
            buildDeploymentPage();
        } else if ("Options".equals(pageName) && isUnderDeployment(selected)) {
            buildDeploymentOptionsPage();
        } else if ("Docker".equals(pageName) && isUnderBuild(selected)) {
            buildDockerPage();
        } else if ("Docker Registry".equals(pageName) && isUnderDocker(selected)) {
            buildDockerRegistryPage();
        } else if ("Java Profiler".equals(pageName) && isUnderBuild(selected)) {
            buildJavaProfilerPage();
        } else if ("Filters".equals(pageName) && isUnderJavaProfiler(selected)) {
            buildJavaProfilerFiltersPage();
        } else if ("Kubernetes".equals(pageName) && isUnderBuild(selected)) {
            buildKubernetesPage();
        } else if ("Remote Jar Repositories".equals(pageName) && isUnderBuild(selected)) {
            buildRemoteJarRepositoriesPage();
        } else if ("Run Targets".equals(pageName) && isUnderBuild(selected)) {
            buildRunTargetsPage();
        } else if ("PHP".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPhpPage();
        } else if ("Quality Tools".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPhpQualityToolsPage();
        } else if ("PHP_CodeSniffer".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPhpCodeSnifferPage();
        } else if ("PHP CS Fixer".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPhpCsFixerPage();
        } else if ("Laravel Pint".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPhpLaravelPintPage();
        } else if ("Mess Detector".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPhpMessDetectorPage();
        } else if ("Frameworks".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPhpFrameworksPage();
        } else if ("Smarty".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPhpSmartyPage();
        } else if ("Python Template Languages".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPythonTemplateLanguagesPage();
        } else if ("Go".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesGoPage();
        } else if ("GOROOT".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesGoGoRootPage();
        } else if ("GOPATH".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesGoGoPathPage();
        } else if ("Go Modules".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesGoModulesPage();
        } else if ("Build Tags".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesGoBuildTagsPage();
        } else if ("Formatting Functions".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesGoFormattingFunctionsPage();
        } else if ("Imports".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesGoImportsPage();
        } else if ("Rust".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesRustPage();
        } else if ("External Linters".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesRustExternalLintersPage();
        } else if ("Rustfmt".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesRustfmtPage();
        } else if ("JavaFX".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesJavaFxPage();
        } else if ("JavaScript".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesJavaScriptPage();
        } else if ("Code Quality Tools".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesCodeQualityToolsPage();
        } else if ("ESLint".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesEsLintPage();
        } else if ("JSHint".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesJsHintPage();
        } else if ("Libraries".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesLibrariesPage();
        } else if ("Prettier".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPrettierPage();
        } else if ("Styled Components".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesStyledComponentsPage();
        } else if ("Vite".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesVitePage();
        } else if ("Webpack".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesWebpackPage();
        } else if ("JavaScript Runtime".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesJavaScriptRuntimePage();
        } else if ("JVM Logging".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesJvmLoggingPage();
        } else if ("Kotlin".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesKotlinPage();
        } else if ("Kotlin Scripting".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesKotlinScriptingPage();
        } else if ("Ktor".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesKtorPage();
        } else if ("Kubernetes".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesKubernetesPage();
        } else if ("Lombok".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesLombokPage();
        } else if ("Markdown".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesMarkdownPage();
        } else if ("Micronaut".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesMicronautPage();
        } else if ("OpenAPI Specifications".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesOpenApiPage();
        } else if ("Play".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesPlayPage();
        } else if ("Protocol Buffers".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesProtobufPage();
        } else if ("Text Format".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesProtobufTextFormatPage();
        } else if ("Quarkus".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesQuarkusPage();
        } else if ("RBS".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesRbsPage();
        } else if ("Editor".equals(pageName) && isUnderScala(selected)) {
            buildLanguagesScalaEditorPage();
        } else if ("X-Ray Mode".equals(pageName) && isUnderScala(selected)) {
            buildLanguagesScalaXRayPage();
        } else if ("Project View".equals(pageName) && isUnderScala(selected)) {
            buildLanguagesScalaProjectViewPage();
        } else if ("Performance".equals(pageName) && isUnderScala(selected)) {
            buildLanguagesScalaPerformancePage();
        } else if ("Worksheet".equals(pageName) && isUnderScala(selected)) {
            buildLanguagesScalaWorksheetPage();
        } else if ("Base Package".equals(pageName) && isUnderScala(selected)) {
            buildLanguagesScalaBasePackagePage();
        } else if ("Misc".equals(pageName) && isUnderScala(selected)) {
            buildLanguagesScalaMiscPage();
        } else if ("Updates".equals(pageName) && isUnderScala(selected)) {
            buildLanguagesScalaUpdatesPage();
        } else if ("Extensions".equals(pageName) && isUnderScala(selected)) {
            buildLanguagesScalaExtensionsPage();
        } else if ("Schemas and DTDs".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesSchemasAndDtdsPage();
        } else if ("Default XML Schemas".equals(pageName) && isUnderSchemas(selected)) {
            buildLanguagesDefaultXmlSchemasPage();
        } else if ("JSON Schema Mappings".equals(pageName) && isUnderSchemas(selected)) {
            buildLanguagesJsonSchemaMappingsPage();
        } else if ("Remote JSON Schemas".equals(pageName) && isUnderSchemas(selected)) {
            buildLanguagesRemoteJsonSchemasPage();
        } else if ("XML Catalog".equals(pageName) && isUnderSchemas(selected)) {
            buildLanguagesXmlCatalogPage();
        } else if ("Spring".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesSpringPage();
        } else if ("SQL Dialects".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesSqlDialectsPage();
        } else if ("SQL Resolution Scopes".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesSqlResolutionScopesPage();
        } else if ("Dialects".equals(pageName) && isUnderStyleSheets(selected)) {
            buildLanguagesStyleSheetsDialectsPage();
        } else if ("Stylelint".equals(pageName) && isUnderStyleSheets(selected)) {
            buildLanguagesStyleSheetsStylelintPage();
        } else if ("Tailwind CSS".equals(pageName) && isUnderStyleSheets(selected)) {
            buildLanguagesStyleSheetsTailwindPage();
        } else if ("Tables".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesTablesPage();
        } else if ("Template Data Languages".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesTemplateDataLanguagesPage();
        } else if ("TypeScript".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesTypeScriptPage();
        } else if ("Angular".equals(pageName) && isUnderTypeScript(selected)) {
            buildLanguagesTypeScriptAngularPage();
        } else if ("TSLint".equals(pageName) && isUnderTypeScript(selected)) {
            buildLanguagesTypeScriptTsLintPage();
        } else if ("Vue".equals(pageName) && isUnderTypeScript(selected)) {
            buildLanguagesTypeScriptVuePage();
        } else if ("Web Contexts".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesWebContextsPage();
        } else if ("XSLT".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesXsltPage();
        } else if ("XSLT File Associations".equals(pageName) && isUnderLanguages(selected)) {
            buildLanguagesXsltFileAssociationsPage();
        } else {
            // If it has children, show category overview
            if (!selected.getChildren().isEmpty()) {
                SettingsCategoryOverviewPage overview = new SettingsCategoryOverviewPage(selected, child -> {
                    expandAncestors(child);
                    tree.getSelectionModel().select(child);
                });
                wrapInScroll(overview);
            } else {
                // Placeholder for other pages
                Label title = new Label(pageName);
                title.getStyleClass().add("settings-page-title");

                Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
                placeholder.getStyleClass().add("settings-placeholder");

                VBox box = new VBox(20, title, placeholder);
                box.setPadding(new Insets(40, 24, 20, 24));
                box.getStyleClass().add("settings-page");
                wrapInScroll(box);
            }
        }
    }

    private void wrapInScroll(javafx.scene.Node page) {
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("settings-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: #1E1F22; -fx-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(scroll);
    }

    private void buildEditorPage(String pageName) {
        SettingsEditorPage page = new SettingsEditorPage();
        page.showEditorPage(pageName);
        wrapInScroll(page);
    }

    private void buildKeymapPage() {
        SettingsKeymapPage page = new SettingsKeymapPage(() -> {
            TreeItem<String> pluginsItem = findItem(tree.getRoot(), "Plugins");
            if (pluginsItem != null) {
                expandAncestors(pluginsItem);
                tree.getSelectionModel().select(pluginsItem);
            }
        });
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.getChildren().setAll(page);
    }

    private void buildMenusToolbarsPage() {
        currentMenusToolbarsPage = new SettingsMenusToolbarsPage();
        VBox.setVgrow(currentMenusToolbarsPage, Priority.ALWAYS);
        contentContainer.getChildren().setAll(currentMenusToolbarsPage);
    }

    private void buildFileColorsPage() {
        SettingsFileColorsPage page = new SettingsFileColorsPage(() -> {
            TreeItem<String> scopesItem = findItem(tree.getRoot(), "Scopes");
            if (scopesItem != null) {
                expandAncestors(scopesItem);
                tree.getSelectionModel().select(scopesItem);
            }
        });
        wrapInScroll(page);
    }

    private void buildTerminalSettingsPage() {
        SettingsTerminalPage page = new SettingsTerminalPage();
        wrapInScroll(page);
    }

    private void buildVcsChangelistsPage() {
        SettingsVcsChangelistsPage page = new SettingsVcsChangelistsPage();
        wrapInScroll(page);
    }

    private void buildVcsCommitPage() {
        SettingsVcsCommitPage page = new SettingsVcsCommitPage();
        wrapInScroll(page);
    }

    private void buildVcsConfirmationPage() {
        SettingsVcsConfirmationPage page = new SettingsVcsConfirmationPage();
        wrapInScroll(page);
    }

    private void buildVcsDirectoryMappingsPage() {
        SettingsVcsDirectoryMappingsPage page = new SettingsVcsDirectoryMappingsPage();
        wrapInScroll(page);
    }

    private void buildVcsFileStatusColorsPage() {
        SettingsVcsFileStatusColorsPage page = new SettingsVcsFileStatusColorsPage();
        wrapInScroll(page);
    }

    private void buildVcsIssueNavigationPage() {
        SettingsVcsIssueNavigationPage page = new SettingsVcsIssueNavigationPage();
        wrapInScroll(page);
    }

    private void buildVcsLogPage() {
        SettingsVcsLogPage page = new SettingsVcsLogPage();
        wrapInScroll(page);
    }

    private void buildVcsShelfPage() {
        SettingsVcsShelfPage page = new SettingsVcsShelfPage();
        wrapInScroll(page);
    }

    private void buildVcsGitPage() {
        SettingsVcsGitPage page = new SettingsVcsGitPage();
        wrapInScroll(page);
    }

    private void buildVcsGitHubPage() {
        SettingsVcsGitHubPage page = new SettingsVcsGitHubPage();
        wrapInScroll(page);
    }

    private void buildVcsGitLabPage() {
        SettingsVcsGitLabPage page = new SettingsVcsGitLabPage();
        wrapInScroll(page);
    }

    private void buildVcsMercurialPage() {
        SettingsVcsMercurialPage page = new SettingsVcsMercurialPage();
        wrapInScroll(page);
    }

    private void buildVcsPerforcePage() {
        SettingsVcsPerforcePage page = new SettingsVcsPerforcePage();
        wrapInScroll(page);
    }

    private void buildVcsPerforceMcpPage() {
        SettingsVcsPerforceMcpPage page = new SettingsVcsPerforceMcpPage();
        wrapInScroll(page);
    }

    private void buildVcsSubversionPage() {
        SettingsVcsSubversionPage page = new SettingsVcsSubversionPage();
        wrapInScroll(page);
    }

    private void buildVcsSubversionNetworkPage() {
        SettingsVcsSubversionNetworkPage page = new SettingsVcsSubversionNetworkPage();
        wrapInScroll(page);
    }

    private void buildVcsSubversionPresentationPage() {
        SettingsVcsSubversionPresentationPage page = new SettingsVcsSubversionPresentationPage();
        wrapInScroll(page);
    }

    private void buildVcsSubversionSshPage() {
        SettingsVcsSubversionSshPage page = new SettingsVcsSubversionSshPage();
        wrapInScroll(page);
    }

    private boolean isUnderSubversion(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Subversion".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderMaven(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Maven".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderBuildTools(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Build Tools".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderCompiler(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Compiler".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderBuild(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Build, Execution, Deployment".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderLanguages(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Languages & Frameworks".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderScala(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Scala".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderSchemas(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Schemas and DTDs".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderStyleSheets(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Style Sheets".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderTypeScript(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("TypeScript".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderConsole(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Console".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderEmmet(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Emmet".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private void buildScopesPage() {
        SettingsScopesPage page = new SettingsScopesPage();
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.getChildren().setAll(page);
    }

    private void buildNotificationsPage() {
        SettingsNotificationsPage page = new SettingsNotificationsPage();
        wrapInScroll(page);
    }

    private void buildDataEditorPage() {
        SettingsDataEditorPage page = new SettingsDataEditorPage();
        wrapInScroll(page);
    }

    private void buildQuickListsPage() {
        currentQuickListsPage = new SettingsQuickListsPage();
        VBox.setVgrow(currentQuickListsPage, Priority.ALWAYS);
        contentContainer.getChildren().setAll(currentQuickListsPage);
    }

    private void buildPluginsPage() {
        if (currentPluginsPage == null) {
            currentPluginsPage = new SettingsPluginsPage();
        }
        VBox.setVgrow(currentPluginsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentPluginsPage);
    }

    public SettingsPluginsPage getCurrentPluginsPage() {
        return currentPluginsPage;
    }

    public SettingsIntentionsPage getCurrentIntentionsPage() {
        return currentIntentionsPage;
    }

    public SettingsLanguageInjectionsPage getCurrentLanguageInjectionsPage() {
        return currentLanguageInjectionsPage;
    }

    public SettingsLanguageInjectionsAdvancedPage getCurrentLanguageInjectionsAdvancedPage() {
        return currentLanguageInjectionsAdvancedPage;
    }

    public SettingsNaturalLanguagesPage getCurrentNaturalLanguagesPage() {
        return currentNaturalLanguagesPage;
    }

    public SettingsGrammarAndStylePage getCurrentGrammarAndStylePage() {
        return currentGrammarAndStylePage;
    }

    public SettingsSpellingPage getCurrentSpellingPage() {
        return currentSpellingPage;
    }

    public SettingsReaderModePage getCurrentReaderModePage() {
        return currentReaderModePage;
    }

    public SettingsTextMateBundlesPage getCurrentTextMateBundlesPage() {
        return currentTextMateBundlesPage;
    }

    public SettingsTodoPage getCurrentTodoPage() {
        return currentTodoPage;
    }

    public SettingsPythonDebuggerPage getCurrentPythonDebuggerPage() {
        return currentPythonDebuggerPage;
    }

    public SettingsApplicationServersPage getCurrentApplicationServersPage() {
        return currentApplicationServersPage;
    }

    public SettingsBuildToolsPage getCurrentBuildToolsPage() {
        return currentBuildToolsPage;
    }

    public SettingsMavenPage getCurrentMavenPage() {
        return currentMavenPage;
    }

    public SettingsMavenArchetypeCatalogsPage getCurrentMavenArchetypeCatalogsPage() {
        return currentMavenArchetypeCatalogsPage;
    }

    public SettingsMavenIgnoredFilesPage getCurrentMavenIgnoredFilesPage() {
        return currentMavenIgnoredFilesPage;
    }

    public SettingsMavenImportingPage getCurrentMavenImportingPage() {
        return currentMavenImportingPage;
    }

    public SettingsMavenRepositoriesPage getCurrentMavenRepositoriesPage() {
        return currentMavenRepositoriesPage;
    }

    public SettingsMavenRunnerPage getCurrentMavenRunnerPage() {
        return currentMavenRunnerPage;
    }

    public SettingsMavenRunningTestsPage getCurrentMavenRunningTestsPage() {
        return currentMavenRunningTestsPage;
    }

    public SettingsGradlePage getCurrentGradlePage() {
        return currentGradlePage;
    }

    public SettingsGantPage getCurrentGantPage() {
        return currentGantPage;
    }

    public SettingsBspPage getCurrentBspPage() {
        return currentBspPage;
    }

    public SettingsCargoPage getCurrentCargoPage() {
        return currentCargoPage;
    }

    public SettingsSbtPage getCurrentSbtPage() {
        return currentSbtPage;
    }

    public SettingsCompilerPage getCurrentCompilerPage() {
        return currentCompilerPage;
    }

    public SettingsAnnotationProcessorsPage getCurrentAnnotationProcessorsPage() {
        return currentAnnotationProcessorsPage;
    }

    public SettingsCompilerExcludesPage getCurrentCompilerExcludesPage() {
        return currentCompilerExcludesPage;
    }

    public SettingsGroovyCompilerPage getCurrentGroovyCompilerPage() {
        return currentGroovyCompilerPage;
    }

    public SettingsJavaCompilerPage getCurrentJavaCompilerPage() {
        return currentJavaCompilerPage;
    }

    public SettingsKotlinCompilerPage getCurrentKotlinCompilerPage() {
        return currentKotlinCompilerPage;
    }

    public SettingsRmiCompilerPage getCurrentRmiCompilerPage() {
        return currentRmiCompilerPage;
    }

    public SettingsScalaCompilerPage getCurrentScalaCompilerPage() {
        return currentScalaCompilerPage;
    }

    public SettingsScalaBytecodeIndicesPage getCurrentScalaBytecodeIndicesPage() {
        return currentScalaBytecodeIndicesPage;
    }

    public SettingsScalaCompileServerPage getCurrentScalaCompileServerPage() {
        return currentScalaCompileServerPage;
    }

    public SettingsValidationPage getCurrentValidationPage() {
        return currentValidationPage;
    }

    public SettingsBuildConsolePage getCurrentBuildConsolePage() {
        return currentBuildConsolePage;
    }

    public SettingsPythonConsolePage getCurrentPythonConsolePage() {
        return currentPythonConsolePage;
    }

    public SettingsCoveragePage getCurrentCoveragePage() {
        return currentCoveragePage;
    }

    private void buildPythonDebuggerPage() {
        if (currentPythonDebuggerPage == null) {
            currentPythonDebuggerPage = new SettingsPythonDebuggerPage();
        }
        currentPythonDebuggerPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentPythonDebuggerPage);
        updateApplyButtonState();
    }

    private void buildDebuggerPage() {
        if (currentDebuggerPage == null) {
            currentDebuggerPage = new SettingsDebuggerPage();
        }
        currentDebuggerPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentDebuggerPage);
        updateApplyButtonState();
    }

    private void buildDebuggerAsyncStackTracesPage() {
        if (currentDebuggerAsyncStackTracesPage == null) {
            currentDebuggerAsyncStackTracesPage = new SettingsDebuggerAsyncStackTracesPage();
        }
        currentDebuggerAsyncStackTracesPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentDebuggerAsyncStackTracesPage, Priority.ALWAYS);
        contentContainer.getChildren().setAll(currentDebuggerAsyncStackTracesPage);
        updateApplyButtonState();
    }

    private void buildDebuggerDataViewsPage() {
        if (currentDebuggerDataViewsPage == null) {
            currentDebuggerDataViewsPage = new SettingsDebuggerDataViewsPage();
        }
        currentDebuggerDataViewsPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentDebuggerDataViewsPage);
        updateApplyButtonState();
    }

    private void buildDebuggerDataViewsJavaPage() {
        if (currentDebuggerDataViewsJavaPage == null) {
            currentDebuggerDataViewsJavaPage = new SettingsDebuggerDataViewsJavaPage();
        }
        currentDebuggerDataViewsJavaPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentDebuggerDataViewsJavaPage);
        updateApplyButtonState();
    }

    private void buildDebuggerDataViewsTypeRenderersPage() {
        if (currentDebuggerDataViewsTypeRenderersPage == null) {
            currentDebuggerDataViewsTypeRenderersPage = new SettingsDebuggerDataViewsTypeRenderersPage();
        }
        currentDebuggerDataViewsTypeRenderersPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentDebuggerDataViewsTypeRenderersPage, Priority.ALWAYS);
        contentContainer.getChildren().setAll(currentDebuggerDataViewsTypeRenderersPage);
        updateApplyButtonState();
    }

    private void buildDebuggerHotSwapPage() {
        if (currentDebuggerHotSwapPage == null) {
            currentDebuggerHotSwapPage = new SettingsDebuggerHotSwapPage();
        }
        currentDebuggerHotSwapPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentDebuggerHotSwapPage);
        updateApplyButtonState();
    }

    private void buildDebuggerSteppingPage() {
        if (currentDebuggerSteppingPage == null) {
            currentDebuggerSteppingPage = new SettingsDebuggerSteppingPage();
        }
        currentDebuggerSteppingPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentDebuggerSteppingPage);
        updateApplyButtonState();
    }

    private void buildDebuggerDataViewsJavaScriptPage() {
        if (currentDebuggerDataViewsJavaScriptPage == null) {
            currentDebuggerDataViewsJavaScriptPage = new SettingsDebuggerDataViewsJavaScriptPage();
        }
        currentDebuggerDataViewsJavaScriptPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentDebuggerDataViewsJavaScriptPage);
        updateApplyButtonState();
    }

    private boolean isUnderDebugger(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Debugger".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderDataViews(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Data Views".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    public SettingsDebuggerPage getCurrentDebuggerPage() {
        return currentDebuggerPage;
    }

    public SettingsDebuggerAsyncStackTracesPage getCurrentDebuggerAsyncStackTracesPage() {
        return currentDebuggerAsyncStackTracesPage;
    }

    public SettingsDebuggerDataViewsPage getCurrentDebuggerDataViewsPage() {
        return currentDebuggerDataViewsPage;
    }

    public SettingsDebuggerDataViewsJavaPage getCurrentDebuggerDataViewsJavaPage() {
        return currentDebuggerDataViewsJavaPage;
    }

    public SettingsDebuggerDataViewsTypeRenderersPage getCurrentDebuggerDataViewsTypeRenderersPage() {
        return currentDebuggerDataViewsTypeRenderersPage;
    }

    public SettingsDebuggerHotSwapPage getCurrentDebuggerHotSwapPage() {
        return currentDebuggerHotSwapPage;
    }

    public SettingsDebuggerSteppingPage getCurrentDebuggerSteppingPage() {
        return currentDebuggerSteppingPage;
    }

    public SettingsDebuggerDataViewsJavaScriptPage getCurrentDebuggerDataViewsJavaScriptPage() {
        return currentDebuggerDataViewsJavaScriptPage;
    }

    private boolean isUnderDeployment(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Deployment".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private boolean isUnderDocker(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Docker".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private void buildDeploymentPage() {
        if (currentDeploymentPage == null) {
            currentDeploymentPage = new SettingsDeploymentPage();
        }
        currentDeploymentPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentDeploymentPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentDeploymentPage);
        updateApplyButtonState();
    }

    private void buildDeploymentOptionsPage() {
        if (currentDeploymentOptionsPage == null) {
            currentDeploymentOptionsPage = new SettingsDeploymentOptionsPage();
            currentDeploymentOptionsPage.setNavigationHandler(this::selectCategory);
        }
        currentDeploymentOptionsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentDeploymentOptionsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentDeploymentOptionsPage);
        updateApplyButtonState();
    }

    private void buildDockerPage() {
        if (currentDockerPage == null) {
            currentDockerPage = new SettingsDockerPage();
        }
        currentDockerPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentDockerPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentDockerPage);
        updateApplyButtonState();
    }

    private void buildDockerConsolePage() {
        if (currentDockerConsolePage == null) {
            currentDockerConsolePage = new SettingsDockerConsolePage();
        }
        currentDockerConsolePage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentDockerConsolePage);
        updateApplyButtonState();
    }

    private void buildDockerRegistryPage() {
        if (currentDockerRegistryPage == null) {
            currentDockerRegistryPage = new SettingsDockerRegistryPage();
        }
        currentDockerRegistryPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentDockerRegistryPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentDockerRegistryPage);
        updateApplyButtonState();
    }

    public SettingsDeploymentPage getCurrentDeploymentPage() {
        return currentDeploymentPage;
    }

    public SettingsDeploymentOptionsPage getCurrentDeploymentOptionsPage() {
        return currentDeploymentOptionsPage;
    }

    public SettingsDockerPage getCurrentDockerPage() {
        return currentDockerPage;
    }

    public SettingsDockerConsolePage getCurrentDockerConsolePage() {
        return currentDockerConsolePage;
    }

    public SettingsDockerRegistryPage getCurrentDockerRegistryPage() {
        return currentDockerRegistryPage;
    }

    private boolean isUnderJavaProfiler(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Java Profiler".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private void buildJavaProfilerPage() {
        if (currentJavaProfilerPage == null) {
            currentJavaProfilerPage = new SettingsJavaProfilerPage();
        }
        currentJavaProfilerPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentJavaProfilerPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentJavaProfilerPage);
        updateApplyButtonState();
    }

    private void buildJavaProfilerFiltersPage() {
        if (currentJavaProfilerFiltersPage == null) {
            currentJavaProfilerFiltersPage = new SettingsJavaProfilerFiltersPage();
        }
        currentJavaProfilerFiltersPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentJavaProfilerFiltersPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentJavaProfilerFiltersPage);
        updateApplyButtonState();
    }

    private void buildKubernetesPage() {
        if (currentBuildKubernetesPage == null) {
            currentBuildKubernetesPage = new SettingsBuildKubernetesPage();
        }
        currentBuildKubernetesPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentBuildKubernetesPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentBuildKubernetesPage);
        updateApplyButtonState();
    }

    public SettingsJavaProfilerPage getCurrentJavaProfilerPage() {
        return currentJavaProfilerPage;
    }

    public SettingsJavaProfilerFiltersPage getCurrentJavaProfilerFiltersPage() {
        return currentJavaProfilerFiltersPage;
    }

    public SettingsBuildKubernetesPage getCurrentBuildKubernetesPage() {
        return currentBuildKubernetesPage;
    }

    private void buildRemoteJarRepositoriesPage() {
        if (currentRemoteJarRepositoriesPage == null) {
            currentRemoteJarRepositoriesPage = new SettingsRemoteJarRepositoriesPage();
        }
        currentRemoteJarRepositoriesPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentRemoteJarRepositoriesPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentRemoteJarRepositoriesPage);
        updateApplyButtonState();
    }

    private void buildRunTargetsPage() {
        if (currentRunTargetsPage == null) {
            currentRunTargetsPage = new SettingsRunTargetsPage();
        }
        currentRunTargetsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentRunTargetsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentRunTargetsPage);
        updateApplyButtonState();
    }

    public SettingsRemoteJarRepositoriesPage getCurrentRemoteJarRepositoriesPage() {
        return currentRemoteJarRepositoriesPage;
    }

    public SettingsRunTargetsPage getCurrentRunTargetsPage() {
        return currentRunTargetsPage;
    }

    private void buildLanguagesPhpPage() {
        if (currentLanguagesPhpPage == null) {
            currentLanguagesPhpPage = new SettingsLanguagesPHPPage();
        }
        currentLanguagesPhpPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesPhpPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesPhpPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesPHPPage getCurrentLanguagesPhpPage() {
        return currentLanguagesPhpPage;
    }

    private void buildLanguagesPhpDebugPage() {
        if (currentLanguagesPhpDebugPage == null) {
            currentLanguagesPhpDebugPage = new SettingsLanguagesPhpDebugPage();
        }
        currentLanguagesPhpDebugPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesPhpDebugPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpDebugTemplatesPage() {
        if (currentLanguagesPhpDebugTemplatesPage == null) {
            currentLanguagesPhpDebugTemplatesPage = new SettingsLanguagesPhpDebugTemplatesPage();
        }
        wrapInScroll(currentLanguagesPhpDebugTemplatesPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpDebugDbgpProxyPage() {
        if (currentLanguagesPhpDebugDbgpProxyPage == null) {
            currentLanguagesPhpDebugDbgpProxyPage = new SettingsLanguagesPhpDebugDbgpProxyPage();
        }
        currentLanguagesPhpDebugDbgpProxyPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesPhpDebugDbgpProxyPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpDebugSkippedPathsPage() {
        if (currentLanguagesPhpDebugSkippedPathsPage == null) {
            currentLanguagesPhpDebugSkippedPathsPage = new SettingsLanguagesPhpDebugSkippedPathsPage();
        }
        currentLanguagesPhpDebugSkippedPathsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesPhpDebugSkippedPathsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesPhpDebugSkippedPathsPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpDebugStepFiltersPage() {
        if (currentLanguagesPhpDebugStepFiltersPage == null) {
            currentLanguagesPhpDebugStepFiltersPage = new SettingsLanguagesPhpDebugStepFiltersPage();
        }
        currentLanguagesPhpDebugStepFiltersPage.setOnModified(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesPhpDebugStepFiltersPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesPhpDebugStepFiltersPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpDebugXdebugCloudPage() {
        if (currentLanguagesPhpDebugXdebugCloudPage == null) {
            currentLanguagesPhpDebugXdebugCloudPage = new SettingsLanguagesPhpDebugXdebugCloudPage();
        }
        currentLanguagesPhpDebugXdebugCloudPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesPhpDebugXdebugCloudPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpServersPage() {
        if (currentLanguagesPhpServersPage == null) {
            currentLanguagesPhpServersPage = new SettingsLanguagesPhpServersPage();
        }
        currentLanguagesPhpServersPage.setOnModified(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesPhpServersPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesPhpServersPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpComposerPage() {
        if (currentLanguagesPhpComposerPage == null) {
            currentLanguagesPhpComposerPage = new SettingsLanguagesPhpComposerPage();
        }
        currentLanguagesPhpComposerPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesPhpComposerPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpTestFrameworksPage() {
        if (currentLanguagesPhpTestFrameworksPage == null) {
            currentLanguagesPhpTestFrameworksPage = new SettingsLanguagesPhpTestFrameworksPage();
        }
        currentLanguagesPhpTestFrameworksPage.setOnModified(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesPhpTestFrameworksPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesPhpTestFrameworksPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesPhpDebugPage getCurrentLanguagesPhpDebugPage() {
        return currentLanguagesPhpDebugPage;
    }

    public SettingsLanguagesPhpDebugDbgpProxyPage getCurrentLanguagesPhpDebugDbgpProxyPage() {
        return currentLanguagesPhpDebugDbgpProxyPage;
    }

    public SettingsLanguagesPhpDebugSkippedPathsPage getCurrentLanguagesPhpDebugSkippedPathsPage() {
        return currentLanguagesPhpDebugSkippedPathsPage;
    }

    public SettingsLanguagesPhpDebugStepFiltersPage getCurrentLanguagesPhpDebugStepFiltersPage() {
        return currentLanguagesPhpDebugStepFiltersPage;
    }

    public SettingsLanguagesPhpDebugXdebugCloudPage getCurrentLanguagesPhpDebugXdebugCloudPage() {
        return currentLanguagesPhpDebugXdebugCloudPage;
    }

    public SettingsLanguagesPhpServersPage getCurrentLanguagesPhpServersPage() {
        return currentLanguagesPhpServersPage;
    }

    public SettingsLanguagesPhpComposerPage getCurrentLanguagesPhpComposerPage() {
        return currentLanguagesPhpComposerPage;
    }

    public SettingsLanguagesPhpTestFrameworksPage getCurrentLanguagesPhpTestFrameworksPage() {
        return currentLanguagesPhpTestFrameworksPage;
    }

    private void buildLanguagesPhpQualityToolsPage() {
        if (currentPhpQualityToolsPage == null) {
            currentPhpQualityToolsPage = new SettingsLanguagesPhpQualityToolsPage();
        }
        currentPhpQualityToolsPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentPhpQualityToolsPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpCodeSnifferPage() {
        if (currentPhpCodeSnifferPage == null) {
            currentPhpCodeSnifferPage = new SettingsLanguagesPhpQualityToolsCodeSnifferPage();
        }
        currentPhpCodeSnifferPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentPhpCodeSnifferPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpCsFixerPage() {
        if (currentPhpCsFixerPage == null) {
            currentPhpCsFixerPage = new SettingsLanguagesPhpQualityToolsCsFixerPage();
        }
        currentPhpCsFixerPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentPhpCsFixerPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpLaravelPintPage() {
        if (currentPhpLaravelPintPage == null) {
            currentPhpLaravelPintPage = new SettingsLanguagesPhpQualityToolsLaravelPintPage();
        }
        currentPhpLaravelPintPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentPhpLaravelPintPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpMessDetectorPage() {
        if (currentPhpMessDetectorPage == null) {
            currentPhpMessDetectorPage = new SettingsLanguagesPhpQualityToolsMessDetectorPage();
        }
        currentPhpMessDetectorPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentPhpMessDetectorPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesPhpQualityToolsPage getCurrentPhpQualityToolsPage() {
        return currentPhpQualityToolsPage;
    }

    public SettingsLanguagesPhpQualityToolsCodeSnifferPage getCurrentPhpCodeSnifferPage() {
        return currentPhpCodeSnifferPage;
    }

    public SettingsLanguagesPhpQualityToolsCsFixerPage getCurrentPhpCsFixerPage() {
        return currentPhpCsFixerPage;
    }

    public SettingsLanguagesPhpQualityToolsLaravelPintPage getCurrentPhpLaravelPintPage() {
        return currentPhpLaravelPintPage;
    }

    public SettingsLanguagesPhpQualityToolsMessDetectorPage getCurrentPhpMessDetectorPage() {
        return currentPhpMessDetectorPage;
    }

    private void buildLanguagesPhpFrameworksPage() {
        if (currentPhpFrameworksPage == null) {
            currentPhpFrameworksPage = new SettingsLanguagesPhpFrameworksPage();
            currentPhpFrameworksPage.setOnNavigateToPlugins(() -> selectCategory("Plugins"));
        }
        wrapInScroll(currentPhpFrameworksPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPhpSmartyPage() {
        if (currentPhpSmartyPage == null) {
            currentPhpSmartyPage = new SettingsLanguagesPhpSmartyPage();
        }
        currentPhpSmartyPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentPhpSmartyPage);
        updateApplyButtonState();
    }

    private void buildLanguagesPythonTemplateLanguagesPage() {
        if (currentPythonTemplateLanguagesPage == null) {
            currentPythonTemplateLanguagesPage = new SettingsPythonTemplateLanguagesPage();
        }
        currentPythonTemplateLanguagesPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentPythonTemplateLanguagesPage);
        updateApplyButtonState();
    }

    public SettingsPythonTemplateLanguagesPage getCurrentPythonTemplateLanguagesPage() {
        return currentPythonTemplateLanguagesPage;
    }

    private void buildLanguagesGoPage() {
        if (currentLanguagesGoPage == null) {
            currentLanguagesGoPage = new SettingsLanguagesGoPage();
        }
        currentLanguagesGoPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesGoPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesGoPage getCurrentLanguagesGoPage() {
        return currentLanguagesGoPage;
    }

    private void buildLanguagesGoGoRootPage() {
        if (currentLanguagesGoGoRootPage == null) {
            currentLanguagesGoGoRootPage = new SettingsLanguagesGoGoRootPage();
        }
        currentLanguagesGoGoRootPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesGoGoRootPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesGoGoRootPage getCurrentLanguagesGoGoRootPage() {
        return currentLanguagesGoGoRootPage;
    }

    private void buildLanguagesGoGoPathPage() {
        if (currentLanguagesGoGoPathPage == null) {
            currentLanguagesGoGoPathPage = new SettingsLanguagesGoGoPathPage();
        }
        currentLanguagesGoGoPathPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesGoGoPathPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesGoGoPathPage getCurrentLanguagesGoGoPathPage() {
        return currentLanguagesGoGoPathPage;
    }

    private void buildLanguagesGoModulesPage() {
        if (currentLanguagesGoModulesPage == null) {
            currentLanguagesGoModulesPage = new SettingsLanguagesGoModulesPage();
        }
        currentLanguagesGoModulesPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesGoModulesPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesGoModulesPage getCurrentLanguagesGoModulesPage() {
        return currentLanguagesGoModulesPage;
    }

    private void buildLanguagesGoBuildTagsPage() {
        if (currentLanguagesGoBuildTagsPage == null) {
            currentLanguagesGoBuildTagsPage = new SettingsLanguagesGoBuildTagsPage();
        }
        currentLanguagesGoBuildTagsPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesGoBuildTagsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesGoBuildTagsPage getCurrentLanguagesGoBuildTagsPage() {
        return currentLanguagesGoBuildTagsPage;
    }

    private void buildLanguagesGoFormattingFunctionsPage() {
        if (currentLanguagesGoFormattingFunctionsPage == null) {
            currentLanguagesGoFormattingFunctionsPage = new SettingsLanguagesGoFormattingFunctionsPage();
        }
        currentLanguagesGoFormattingFunctionsPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesGoFormattingFunctionsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesGoFormattingFunctionsPage getCurrentLanguagesGoFormattingFunctionsPage() {
        return currentLanguagesGoFormattingFunctionsPage;
    }

    private void buildLanguagesGoImportsPage() {
        if (currentLanguagesGoImportsPage == null) {
            currentLanguagesGoImportsPage = new SettingsLanguagesGoImportsPage();
        }
        currentLanguagesGoImportsPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesGoImportsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesGoImportsPage getCurrentLanguagesGoImportsPage() {
        return currentLanguagesGoImportsPage;
    }

    private void buildLanguagesRustPage() {
        if (currentRustPage == null) {
            currentRustPage = new SettingsRustPage();
        }
        currentRustPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentRustPage);
        updateApplyButtonState();
    }

    private void buildLanguagesRustExternalLintersPage() {
        if (currentRustExternalLintersPage == null) {
            currentRustExternalLintersPage = new SettingsRustExternalLintersPage();
        }
        currentRustExternalLintersPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentRustExternalLintersPage);
        updateApplyButtonState();
    }

    private void buildLanguagesRustfmtPage() {
        if (currentRustfmtPage == null) {
            currentRustfmtPage = new SettingsRustfmtPage();
            currentRustfmtPage.setOnNavigateToActionsOnSave(() -> selectCategory("Actions on Save"));
        }
        currentRustfmtPage.setOnModified(this::updateApplyButtonState);
        wrapInScroll(currentRustfmtPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesPhpFrameworksPage getCurrentPhpFrameworksPage() {
        return currentPhpFrameworksPage;
    }

    public SettingsLanguagesPhpSmartyPage getCurrentPhpSmartyPage() {
        return currentPhpSmartyPage;
    }

    public SettingsRustPage getCurrentRustPage() {
        return currentRustPage;
    }

    public SettingsRustExternalLintersPage getCurrentRustExternalLintersPage() {
        return currentRustExternalLintersPage;
    }

    public SettingsRustfmtPage getCurrentRustfmtPage() {
        return currentRustfmtPage;
    }

    private void buildLanguagesJavaFxPage() {
        if (currentLanguagesJavaFxPage == null) {
            currentLanguagesJavaFxPage = new SettingsLanguagesJavaFXPage();
        }
        currentLanguagesJavaFxPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesJavaFxPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJavaFxPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJavaFXPage getCurrentLanguagesJavaFxPage() {
        return currentLanguagesJavaFxPage;
    }

    private void buildLanguagesJavaScriptPage() {
        if (currentLanguagesJavaScriptPage == null) {
            currentLanguagesJavaScriptPage = new SettingsLanguagesJavaScriptPage();
        }
        currentLanguagesJavaScriptPage.setOnModifiedListener(this::updateApplyButtonState);
        currentLanguagesJavaScriptPage.setOnNavigateToCodeCompletion(() -> selectCategory("Code Completion"));
        VBox.setVgrow(currentLanguagesJavaScriptPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJavaScriptPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJavaScriptPage getCurrentLanguagesJavaScriptPage() {
        return currentLanguagesJavaScriptPage;
    }

    private void buildLanguagesCodeQualityToolsPage() {
        SettingsLanguagesJSCodeQualityToolsPage page = new SettingsLanguagesJSCodeQualityToolsPage(this::selectCategory);
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(page);
    }

    private void buildLanguagesEsLintPage() {
        if (currentLanguagesJsEsLintPage == null) {
            currentLanguagesJsEsLintPage = new SettingsLanguagesJSEsLintPage();
        }
        currentLanguagesJsEsLintPage.setOnModifiedListener(this::updateApplyButtonState);
        currentLanguagesJsEsLintPage.setOnNavigateToActionsOnSave(() -> selectCategory("Actions on Save"));
        VBox.setVgrow(currentLanguagesJsEsLintPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJsEsLintPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJSEsLintPage getCurrentLanguagesJsEsLintPage() {
        return currentLanguagesJsEsLintPage;
    }

    private void buildLanguagesJsHintPage() {
        if (currentLanguagesJsJsHintPage == null) {
            currentLanguagesJsJsHintPage = new SettingsLanguagesJSJsHintPage();
        }
        currentLanguagesJsJsHintPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesJsJsHintPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJsJsHintPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJSJsHintPage getCurrentLanguagesJsJsHintPage() {
        return currentLanguagesJsJsHintPage;
    }

    private void buildLanguagesLibrariesPage() {
        if (currentLanguagesJsLibrariesPage == null) {
            currentLanguagesJsLibrariesPage = new SettingsLanguagesJSLibrariesPage();
        }
        currentLanguagesJsLibrariesPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesJsLibrariesPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJsLibrariesPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJSLibrariesPage getCurrentLanguagesJsLibrariesPage() {
        return currentLanguagesJsLibrariesPage;
    }

    private void buildLanguagesPrettierPage() {
        if (currentLanguagesJsPrettierPage == null) {
            currentLanguagesJsPrettierPage = new SettingsLanguagesJSPrettierPage();
        }
        currentLanguagesJsPrettierPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesJsPrettierPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJsPrettierPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJSPrettierPage getCurrentLanguagesJsPrettierPage() {
        return currentLanguagesJsPrettierPage;
    }

    private void buildLanguagesStyledComponentsPage() {
        if (currentLanguagesJsStyledComponentsPage == null) {
            currentLanguagesJsStyledComponentsPage = new SettingsLanguagesJSStyledComponentsPage();
        }
        currentLanguagesJsStyledComponentsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesJsStyledComponentsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJsStyledComponentsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJSStyledComponentsPage getCurrentLanguagesJsStyledComponentsPage() {
        return currentLanguagesJsStyledComponentsPage;
    }

    private void buildLanguagesVitePage() {
        if (currentLanguagesJsVitePage == null) {
            currentLanguagesJsVitePage = new SettingsLanguagesJSVitePage();
        }
        currentLanguagesJsVitePage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesJsVitePage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJsVitePage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJSVitePage getCurrentLanguagesJsVitePage() {
        return currentLanguagesJsVitePage;
    }

    private void buildLanguagesWebpackPage() {
        if (currentLanguagesJsWebpackPage == null) {
            currentLanguagesJsWebpackPage = new SettingsLanguagesJSWebpackPage();
        }
        currentLanguagesJsWebpackPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesJsWebpackPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJsWebpackPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJSWebpackPage getCurrentLanguagesJsWebpackPage() {
        return currentLanguagesJsWebpackPage;
    }

    private void buildLanguagesJavaScriptRuntimePage() {
        if (currentLanguagesJavaScriptRuntimePage == null) {
            currentLanguagesJavaScriptRuntimePage = new SettingsLanguagesJavaScriptRuntimePage();
        }
        currentLanguagesJavaScriptRuntimePage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesJavaScriptRuntimePage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJavaScriptRuntimePage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJavaScriptRuntimePage getCurrentLanguagesJavaScriptRuntimePage() {
        return currentLanguagesJavaScriptRuntimePage;
    }

    private void buildLanguagesJvmLoggingPage() {
        if (currentLanguagesJvmLoggingPage == null) {
            currentLanguagesJvmLoggingPage = new SettingsLanguagesJvmLoggingPage();
        }
        currentLanguagesJvmLoggingPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesJvmLoggingPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJvmLoggingPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJvmLoggingPage getCurrentLanguagesJvmLoggingPage() {
        return currentLanguagesJvmLoggingPage;
    }

    private void buildLanguagesKotlinPage() {
        if (currentLanguagesKotlinPage == null) {
            currentLanguagesKotlinPage = new SettingsLanguagesKotlinPage();
        }
        currentLanguagesKotlinPage.setOnNavigateToKotlinScripting(() -> selectCategory("Kotlin Scripting"));
        VBox.setVgrow(currentLanguagesKotlinPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesKotlinPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesKotlinPage getCurrentLanguagesKotlinPage() {
        return currentLanguagesKotlinPage;
    }

    private void buildLanguagesKotlinScriptingPage() {
        if (currentLanguagesKotlinScriptingPage == null) {
            currentLanguagesKotlinScriptingPage = new SettingsLanguagesKotlinScriptingPage();
        }
        currentLanguagesKotlinScriptingPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesKotlinScriptingPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesKotlinScriptingPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesKotlinScriptingPage getCurrentLanguagesKotlinScriptingPage() {
        return currentLanguagesKotlinScriptingPage;
    }

    private void buildLanguagesKtorPage() {
        if (currentLanguagesKtorPage == null) {
            currentLanguagesKtorPage = new SettingsLanguagesKtorPage();
        }
        currentLanguagesKtorPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesKtorPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesKtorPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesKtorPage getCurrentLanguagesKtorPage() {
        return currentLanguagesKtorPage;
    }

    private void buildLanguagesKubernetesPage() {
        if (currentLanguagesKubernetesPage == null) {
            currentLanguagesKubernetesPage = new SettingsLanguagesKubernetesPage();
        }
        currentLanguagesKubernetesPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesKubernetesPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesKubernetesPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesKubernetesPage getCurrentLanguagesKubernetesPage() {
        return currentLanguagesKubernetesPage;
    }

    private void buildLanguagesLombokPage() {
        if (currentLanguagesLombokPage == null) {
            currentLanguagesLombokPage = new SettingsLanguagesLombokPage();
        }
        currentLanguagesLombokPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesLombokPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesLombokPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesLombokPage getCurrentLanguagesLombokPage() {
        return currentLanguagesLombokPage;
    }

    private void buildLanguagesMarkdownPage() {
        if (currentLanguagesMarkdownPage == null) {
            currentLanguagesMarkdownPage = new SettingsLanguagesMarkdownPage(() -> selectCategory("Smart Keys"));
        }
        currentLanguagesMarkdownPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesMarkdownPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesMarkdownPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesMarkdownPage getCurrentLanguagesMarkdownPage() {
        return currentLanguagesMarkdownPage;
    }

    private void buildLanguagesMicronautPage() {
        if (currentLanguagesMicronautPage == null) {
            currentLanguagesMicronautPage = new SettingsLanguagesMicronautPage();
        }
        currentLanguagesMicronautPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesMicronautPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesMicronautPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesMicronautPage getCurrentLanguagesMicronautPage() {
        return currentLanguagesMicronautPage;
    }

    private void buildLanguagesOpenApiPage() {
        if (currentLanguagesOpenApiPage == null) {
            currentLanguagesOpenApiPage = new SettingsLanguagesOpenAPIPage();
        }
        currentLanguagesOpenApiPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesOpenApiPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesOpenApiPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesOpenAPIPage getCurrentLanguagesOpenApiPage() {
        return currentLanguagesOpenApiPage;
    }

    private void buildLanguagesPlayPage() {
        if (currentLanguagesPlayPage == null) {
            currentLanguagesPlayPage = new SettingsLanguagesPlayPage();
        }
        currentLanguagesPlayPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesPlayPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesPlayPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesPlayPage getCurrentLanguagesPlayPage() {
        return currentLanguagesPlayPage;
    }

    private void buildLanguagesProtobufPage() {
        if (currentLanguagesProtobufPage == null) {
            currentLanguagesProtobufPage = new SettingsLanguagesProtobufPage();
        }
        currentLanguagesProtobufPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesProtobufPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesProtobufPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesProtobufPage getCurrentLanguagesProtobufPage() {
        return currentLanguagesProtobufPage;
    }

    private void buildLanguagesProtobufTextFormatPage() {
        if (currentLanguagesProtobufTextFormatPage == null) {
            currentLanguagesProtobufTextFormatPage = new SettingsLanguagesProtobufTextFormatPage();
        }
        currentLanguagesProtobufTextFormatPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesProtobufTextFormatPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesProtobufTextFormatPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesProtobufTextFormatPage getCurrentLanguagesProtobufTextFormatPage() {
        return currentLanguagesProtobufTextFormatPage;
    }

    private void buildLanguagesQuarkusPage() {
        if (currentLanguagesQuarkusPage == null) {
            currentLanguagesQuarkusPage = new SettingsLanguagesQuarkusPage();
        }
        currentLanguagesQuarkusPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesQuarkusPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesQuarkusPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesQuarkusPage getCurrentLanguagesQuarkusPage() {
        return currentLanguagesQuarkusPage;
    }

    private void buildLanguagesRbsPage() {
        if (currentLanguagesRbsPage == null) {
            currentLanguagesRbsPage = new SettingsLanguagesRbsPage();
        }
        currentLanguagesRbsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesRbsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesRbsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesRbsPage getCurrentLanguagesRbsPage() {
        return currentLanguagesRbsPage;
    }

    private void buildLanguagesScalaEditorPage() {
        if (currentLanguagesScalaEditorPage == null) {
            currentLanguagesScalaEditorPage = new SettingsLanguagesScalaEditorPage();
        }
        currentLanguagesScalaEditorPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesScalaEditorPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesScalaEditorPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesScalaEditorPage getCurrentLanguagesScalaEditorPage() {
        return currentLanguagesScalaEditorPage;
    }

    private void buildLanguagesScalaXRayPage() {
        if (currentLanguagesScalaXRayPage == null) {
            currentLanguagesScalaXRayPage = new SettingsLanguagesScalaXRayPage();
        }
        currentLanguagesScalaXRayPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesScalaXRayPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesScalaXRayPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesScalaXRayPage getCurrentLanguagesScalaXRayPage() {
        return currentLanguagesScalaXRayPage;
    }

    private void buildLanguagesScalaProjectViewPage() {
        if (currentLanguagesScalaProjectViewPage == null) {
            currentLanguagesScalaProjectViewPage = new SettingsLanguagesScalaProjectViewPage();
        }
        currentLanguagesScalaProjectViewPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesScalaProjectViewPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesScalaProjectViewPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesScalaProjectViewPage getCurrentLanguagesScalaProjectViewPage() {
        return currentLanguagesScalaProjectViewPage;
    }

    private void buildLanguagesScalaPerformancePage() {
        if (currentLanguagesScalaPerformancePage == null) {
            currentLanguagesScalaPerformancePage = new SettingsLanguagesScalaPerformancePage();
        }
        currentLanguagesScalaPerformancePage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesScalaPerformancePage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesScalaPerformancePage);
        updateApplyButtonState();
    }

    public SettingsLanguagesScalaPerformancePage getCurrentLanguagesScalaPerformancePage() {
        return currentLanguagesScalaPerformancePage;
    }

    private void buildLanguagesScalaWorksheetPage() {
        if (currentLanguagesScalaWorksheetPage == null) {
            currentLanguagesScalaWorksheetPage = new SettingsLanguagesScalaWorksheetPage();
        }
        currentLanguagesScalaWorksheetPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesScalaWorksheetPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesScalaWorksheetPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesScalaWorksheetPage getCurrentLanguagesScalaWorksheetPage() {
        return currentLanguagesScalaWorksheetPage;
    }

    private void buildLanguagesScalaBasePackagePage() {
        if (currentLanguagesScalaBasePackagePage == null) {
            currentLanguagesScalaBasePackagePage = new SettingsLanguagesScalaBasePackagePage();
        }
        currentLanguagesScalaBasePackagePage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesScalaBasePackagePage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesScalaBasePackagePage);
        updateApplyButtonState();
    }

    public SettingsLanguagesScalaBasePackagePage getCurrentLanguagesScalaBasePackagePage() {
        return currentLanguagesScalaBasePackagePage;
    }

    private void buildLanguagesScalaMiscPage() {
        if (currentLanguagesScalaMiscPage == null) {
            currentLanguagesScalaMiscPage = new SettingsLanguagesScalaMiscPage();
        }
        currentLanguagesScalaMiscPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesScalaMiscPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesScalaMiscPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesScalaMiscPage getCurrentLanguagesScalaMiscPage() {
        return currentLanguagesScalaMiscPage;
    }

    private void buildLanguagesScalaUpdatesPage() {
        if (currentLanguagesScalaUpdatesPage == null) {
            currentLanguagesScalaUpdatesPage = new SettingsLanguagesScalaUpdatesPage();
        }
        currentLanguagesScalaUpdatesPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesScalaUpdatesPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesScalaUpdatesPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesScalaUpdatesPage getCurrentLanguagesScalaUpdatesPage() {
        return currentLanguagesScalaUpdatesPage;
    }

    private void buildLanguagesScalaExtensionsPage() {
        if (currentLanguagesScalaExtensionsPage == null) {
            currentLanguagesScalaExtensionsPage = new SettingsLanguagesScalaExtensionsPage();
        }
        currentLanguagesScalaExtensionsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesScalaExtensionsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesScalaExtensionsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesScalaExtensionsPage getCurrentLanguagesScalaExtensionsPage() {
        return currentLanguagesScalaExtensionsPage;
    }

    private void buildLanguagesSchemasAndDtdsPage() {
        if (currentLanguagesSchemasAndDtdsPage == null) {
            currentLanguagesSchemasAndDtdsPage = new SettingsLanguagesSchemasAndDtdsPage();
        }
        currentLanguagesSchemasAndDtdsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesSchemasAndDtdsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesSchemasAndDtdsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesSchemasAndDtdsPage getCurrentLanguagesSchemasAndDtdsPage() {
        return currentLanguagesSchemasAndDtdsPage;
    }

    private void buildLanguagesDefaultXmlSchemasPage() {
        if (currentLanguagesDefaultXmlSchemasPage == null) {
            currentLanguagesDefaultXmlSchemasPage = new SettingsLanguagesDefaultXmlSchemasPage();
        }
        currentLanguagesDefaultXmlSchemasPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesDefaultXmlSchemasPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesDefaultXmlSchemasPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesDefaultXmlSchemasPage getCurrentLanguagesDefaultXmlSchemasPage() {
        return currentLanguagesDefaultXmlSchemasPage;
    }

    private void buildLanguagesJsonSchemaMappingsPage() {
        if (currentLanguagesJsonSchemaMappingsPage == null) {
            currentLanguagesJsonSchemaMappingsPage = new SettingsLanguagesJsonSchemaMappingsPage();
        }
        currentLanguagesJsonSchemaMappingsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesJsonSchemaMappingsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesJsonSchemaMappingsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesJsonSchemaMappingsPage getCurrentLanguagesJsonSchemaMappingsPage() {
        return currentLanguagesJsonSchemaMappingsPage;
    }

    private void buildLanguagesRemoteJsonSchemasPage() {
        if (currentLanguagesRemoteJsonSchemasPage == null) {
            currentLanguagesRemoteJsonSchemasPage = new SettingsLanguagesRemoteJsonSchemasPage();
        }
        currentLanguagesRemoteJsonSchemasPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesRemoteJsonSchemasPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesRemoteJsonSchemasPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesRemoteJsonSchemasPage getCurrentLanguagesRemoteJsonSchemasPage() {
        return currentLanguagesRemoteJsonSchemasPage;
    }

    private void buildLanguagesXmlCatalogPage() {
        if (currentLanguagesXmlCatalogPage == null) {
            currentLanguagesXmlCatalogPage = new SettingsLanguagesXmlCatalogPage();
        }
        currentLanguagesXmlCatalogPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesXmlCatalogPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesXmlCatalogPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesXmlCatalogPage getCurrentLanguagesXmlCatalogPage() {
        return currentLanguagesXmlCatalogPage;
    }

    private void buildLanguagesSpringPage() {
        if (currentLanguagesSpringPage == null) {
            currentLanguagesSpringPage = new SettingsLanguagesSpringPage();
        }
        currentLanguagesSpringPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentLanguagesSpringPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesSpringPage getCurrentLanguagesSpringPage() {
        return currentLanguagesSpringPage;
    }

    private void buildLanguagesSqlDialectsPage() {
        if (currentLanguagesSqlDialectsPage == null) {
            currentLanguagesSqlDialectsPage = new SettingsLanguagesSqlDialectsPage();
        }
        currentLanguagesSqlDialectsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesSqlDialectsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesSqlDialectsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesSqlDialectsPage getCurrentLanguagesSqlDialectsPage() {
        return currentLanguagesSqlDialectsPage;
    }

    private void buildLanguagesSqlResolutionScopesPage() {
        if (currentLanguagesSqlResolutionScopesPage == null) {
            currentLanguagesSqlResolutionScopesPage = new SettingsLanguagesSqlResolutionScopesPage();
        }
        currentLanguagesSqlResolutionScopesPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesSqlResolutionScopesPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesSqlResolutionScopesPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesSqlResolutionScopesPage getCurrentLanguagesSqlResolutionScopesPage() {
        return currentLanguagesSqlResolutionScopesPage;
    }

    private void buildLanguagesStyleSheetsDialectsPage() {
        if (currentLanguagesStyleSheetsDialectsPage == null) {
            currentLanguagesStyleSheetsDialectsPage = new SettingsLanguagesStyleSheetsDialectsPage();
        }
        currentLanguagesStyleSheetsDialectsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesStyleSheetsDialectsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesStyleSheetsDialectsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesStyleSheetsDialectsPage getCurrentLanguagesStyleSheetsDialectsPage() {
        return currentLanguagesStyleSheetsDialectsPage;
    }

    private void buildLanguagesStyleSheetsStylelintPage() {
        if (currentLanguagesStyleSheetsStylelintPage == null) {
            currentLanguagesStyleSheetsStylelintPage = new SettingsLanguagesStyleSheetsStylelintPage();
        }
        currentLanguagesStyleSheetsStylelintPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesStyleSheetsStylelintPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesStyleSheetsStylelintPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesStyleSheetsStylelintPage getCurrentLanguagesStyleSheetsStylelintPage() {
        return currentLanguagesStyleSheetsStylelintPage;
    }

    private void buildLanguagesStyleSheetsTailwindPage() {
        if (currentLanguagesStyleSheetsTailwindPage == null) {
            currentLanguagesStyleSheetsTailwindPage = new SettingsLanguagesStyleSheetsTailwindPage();
        }
        currentLanguagesStyleSheetsTailwindPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesStyleSheetsTailwindPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesStyleSheetsTailwindPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesStyleSheetsTailwindPage getCurrentLanguagesStyleSheetsTailwindPage() {
        return currentLanguagesStyleSheetsTailwindPage;
    }

    private void buildLanguagesTablesPage() {
        if (currentLanguagesTablesPage == null) {
            currentLanguagesTablesPage = new SettingsLanguagesTablesPage();
        }
        currentLanguagesTablesPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesTablesPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesTablesPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesTablesPage getCurrentLanguagesTablesPage() {
        return currentLanguagesTablesPage;
    }

    private void buildLanguagesTemplateDataLanguagesPage() {
        if (currentLanguagesTemplateDataLanguagesPage == null) {
            currentLanguagesTemplateDataLanguagesPage = new SettingsLanguagesTemplateDataLanguagesPage();
        }
        currentLanguagesTemplateDataLanguagesPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesTemplateDataLanguagesPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesTemplateDataLanguagesPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesTemplateDataLanguagesPage getCurrentLanguagesTemplateDataLanguagesPage() {
        return currentLanguagesTemplateDataLanguagesPage;
    }

    private void buildLanguagesTypeScriptPage() {
        if (currentLanguagesTypeScriptPage == null) {
            currentLanguagesTypeScriptPage = new SettingsLanguagesTypeScriptPage();
        }
        currentLanguagesTypeScriptPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesTypeScriptPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesTypeScriptPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesTypeScriptPage getCurrentLanguagesTypeScriptPage() {
        return currentLanguagesTypeScriptPage;
    }

    private void buildLanguagesTypeScriptAngularPage() {
        if (currentLanguagesTypeScriptAngularPage == null) {
            currentLanguagesTypeScriptAngularPage = new SettingsLanguagesTypeScriptAngularPage();
        }
        currentLanguagesTypeScriptAngularPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesTypeScriptAngularPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesTypeScriptAngularPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesTypeScriptAngularPage getCurrentLanguagesTypeScriptAngularPage() {
        return currentLanguagesTypeScriptAngularPage;
    }

    private void buildLanguagesTypeScriptTsLintPage() {
        if (currentLanguagesTypeScriptTsLintPage == null) {
            currentLanguagesTypeScriptTsLintPage = new SettingsLanguagesTypeScriptTsLintPage();
        }
        currentLanguagesTypeScriptTsLintPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesTypeScriptTsLintPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesTypeScriptTsLintPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesTypeScriptTsLintPage getCurrentLanguagesTypeScriptTsLintPage() {
        return currentLanguagesTypeScriptTsLintPage;
    }

    private void buildLanguagesTypeScriptVuePage() {
        if (currentLanguagesTypeScriptVuePage == null) {
            currentLanguagesTypeScriptVuePage = new SettingsLanguagesTypeScriptVuePage();
        }
        currentLanguagesTypeScriptVuePage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesTypeScriptVuePage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesTypeScriptVuePage);
        updateApplyButtonState();
    }

    public SettingsLanguagesTypeScriptVuePage getCurrentLanguagesTypeScriptVuePage() {
        return currentLanguagesTypeScriptVuePage;
    }

    private void buildLanguagesWebContextsPage() {
        if (currentLanguagesWebContextsPage == null) {
            currentLanguagesWebContextsPage = new SettingsLanguagesWebContextsPage();
        }
        currentLanguagesWebContextsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesWebContextsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesWebContextsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesWebContextsPage getCurrentLanguagesWebContextsPage() {
        return currentLanguagesWebContextsPage;
    }

    private void buildLanguagesXsltPage() {
        if (currentLanguagesXsltPage == null) {
            currentLanguagesXsltPage = new SettingsLanguagesXsltPage();
        }
        currentLanguagesXsltPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesXsltPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesXsltPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesXsltPage getCurrentLanguagesXsltPage() {
        return currentLanguagesXsltPage;
    }

    private void buildLanguagesXsltFileAssociationsPage() {
        if (currentLanguagesXsltFileAssociationsPage == null) {
            currentLanguagesXsltFileAssociationsPage = new SettingsLanguagesXsltFileAssociationsPage();
        }
        currentLanguagesXsltFileAssociationsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentLanguagesXsltFileAssociationsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentLanguagesXsltFileAssociationsPage);
        updateApplyButtonState();
    }

    public SettingsLanguagesXsltFileAssociationsPage getCurrentLanguagesXsltFileAssociationsPage() {
        return currentLanguagesXsltFileAssociationsPage;
    }

    private void buildApplicationServersPage() {
        if (currentApplicationServersPage == null) {
            currentApplicationServersPage = new SettingsApplicationServersPage();
        }
        currentApplicationServersPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentApplicationServersPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentApplicationServersPage);
        updateApplyButtonState();
    }

    private void buildBuildToolsPage() {
        if (currentBuildToolsPage == null) {
            currentBuildToolsPage = new SettingsBuildToolsPage();
        }
        currentBuildToolsPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentBuildToolsPage);
        updateApplyButtonState();
    }

    private void buildMavenPage() {
        if (currentMavenPage == null) {
            currentMavenPage = new SettingsMavenPage();
        }
        currentMavenPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentMavenPage);
        updateApplyButtonState();
    }

    private void buildMavenArchetypeCatalogsPage() {
        if (currentMavenArchetypeCatalogsPage == null) {
            currentMavenArchetypeCatalogsPage = new SettingsMavenArchetypeCatalogsPage();
        }
        currentMavenArchetypeCatalogsPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentMavenArchetypeCatalogsPage);
        updateApplyButtonState();
    }

    private void buildMavenIgnoredFilesPage() {
        if (currentMavenIgnoredFilesPage == null) {
            currentMavenIgnoredFilesPage = new SettingsMavenIgnoredFilesPage();
        }
        currentMavenIgnoredFilesPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentMavenIgnoredFilesPage);
        updateApplyButtonState();
    }

    private void buildMavenImportingPage() {
        if (currentMavenImportingPage == null) {
            currentMavenImportingPage = new SettingsMavenImportingPage();
        }
        currentMavenImportingPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentMavenImportingPage);
        updateApplyButtonState();
    }

    private void buildMavenRepositoriesPage() {
        if (currentMavenRepositoriesPage == null) {
            currentMavenRepositoriesPage = new SettingsMavenRepositoriesPage();
        }
        currentMavenRepositoriesPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentMavenRepositoriesPage);
        updateApplyButtonState();
    }

    private void buildMavenRunnerPage() {
        if (currentMavenRunnerPage == null) {
            currentMavenRunnerPage = new SettingsMavenRunnerPage();
        }
        currentMavenRunnerPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentMavenRunnerPage);
        updateApplyButtonState();
    }

    private void buildMavenRunningTestsPage() {
        if (currentMavenRunningTestsPage == null) {
            currentMavenRunningTestsPage = new SettingsMavenRunningTestsPage();
        }
        currentMavenRunningTestsPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentMavenRunningTestsPage);
        updateApplyButtonState();
    }

    private void buildGradlePage() {
        if (currentGradlePage == null) {
            currentGradlePage = new SettingsGradlePage();
        }
        currentGradlePage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentGradlePage);
        updateApplyButtonState();
    }

    private void buildGantPage() {
        if (currentGantPage == null) {
            currentGantPage = new SettingsGantPage();
        }
        currentGantPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentGantPage);
        updateApplyButtonState();
    }

    private void buildBspPage() {
        if (currentBspPage == null) {
            currentBspPage = new SettingsBspPage();
        }
        currentBspPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentBspPage);
        updateApplyButtonState();
    }

    private void buildCargoPage() {
        if (currentCargoPage == null) {
            currentCargoPage = new SettingsCargoPage();
        }
        currentCargoPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentCargoPage);
        updateApplyButtonState();
    }

    private void buildSbtPage() {
        if (currentSbtPage == null) {
            currentSbtPage = new SettingsSbtPage();
        }
        currentSbtPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentSbtPage);
        updateApplyButtonState();
    }

    private void buildCompilerPage() {
        if (currentCompilerPage == null) {
            currentCompilerPage = new SettingsCompilerPage();
        }
        currentCompilerPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentCompilerPage);
        updateApplyButtonState();
    }

    private void buildAnnotationProcessorsPage() {
        if (currentAnnotationProcessorsPage == null) {
            currentAnnotationProcessorsPage = new SettingsAnnotationProcessorsPage();
        }
        currentAnnotationProcessorsPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentAnnotationProcessorsPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentAnnotationProcessorsPage);
        updateApplyButtonState();
    }

    private void buildCompilerExcludesPage() {
        if (currentCompilerExcludesPage == null) {
            currentCompilerExcludesPage = new SettingsCompilerExcludesPage();
        }
        currentCompilerExcludesPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentCompilerExcludesPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentCompilerExcludesPage);
        updateApplyButtonState();
    }

    private void buildGroovyCompilerPage() {
        if (currentGroovyCompilerPage == null) {
            currentGroovyCompilerPage = new SettingsGroovyCompilerPage();
            currentGroovyCompilerPage.setNavigationHandler(this::selectCategory);
        }
        currentGroovyCompilerPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentGroovyCompilerPage);
        updateApplyButtonState();
    }

    private void buildJavaCompilerPage() {
        if (currentJavaCompilerPage == null) {
            currentJavaCompilerPage = new SettingsJavaCompilerPage();
        }
        currentJavaCompilerPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentJavaCompilerPage);
        updateApplyButtonState();
    }

    private void buildKotlinCompilerPage() {
        if (currentKotlinCompilerPage == null) {
            currentKotlinCompilerPage = new SettingsKotlinCompilerPage();
        }
        currentKotlinCompilerPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentKotlinCompilerPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentKotlinCompilerPage);
        updateApplyButtonState();
    }

    private void buildRmiCompilerPage() {
        if (currentRmiCompilerPage == null) {
            currentRmiCompilerPage = new SettingsRmiCompilerPage();
        }
        currentRmiCompilerPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentRmiCompilerPage);
        updateApplyButtonState();
    }

    private void buildScalaCompilerPage() {
        if (currentScalaCompilerPage == null) {
            currentScalaCompilerPage = new SettingsScalaCompilerPage();
        }
        currentScalaCompilerPage.setOnModifiedListener(this::updateApplyButtonState);
        VBox.setVgrow(currentScalaCompilerPage, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(currentScalaCompilerPage);
        updateApplyButtonState();
    }

    private void buildScalaBytecodeIndicesPage() {
        if (currentScalaBytecodeIndicesPage == null) {
            currentScalaBytecodeIndicesPage = new SettingsScalaBytecodeIndicesPage();
        }
        currentScalaBytecodeIndicesPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentScalaBytecodeIndicesPage);
        updateApplyButtonState();
    }

    private void buildScalaCompileServerPage() {
        if (currentScalaCompileServerPage == null) {
            currentScalaCompileServerPage = new SettingsScalaCompileServerPage();
        }
        currentScalaCompileServerPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentScalaCompileServerPage);
        updateApplyButtonState();
    }

    private void buildValidationPage() {
        if (currentValidationPage == null) {
            currentValidationPage = new SettingsValidationPage();
        }
        currentValidationPage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentValidationPage);
        updateApplyButtonState();
    }

    private void buildBuildConsolePage() {
        if (currentBuildConsolePage == null) {
            currentBuildConsolePage = new SettingsBuildConsolePage();
        }
        currentBuildConsolePage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentBuildConsolePage);
        updateApplyButtonState();
    }

    private void buildPythonConsolePage() {
        if (currentPythonConsolePage == null) {
            currentPythonConsolePage = new SettingsPythonConsolePage();
        }
        currentPythonConsolePage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentPythonConsolePage);
        updateApplyButtonState();
    }

    private void buildCoveragePage() {
        if (currentCoveragePage == null) {
            currentCoveragePage = new SettingsCoveragePage();
        }
        currentCoveragePage.setOnModifiedListener(this::updateApplyButtonState);
        wrapInScroll(currentCoveragePage);
        updateApplyButtonState();
    }

    private void buildRequiredPluginsPage() {
        SettingsRequiredPluginsPage page = new SettingsRequiredPluginsPage();
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.getChildren().setAll(page);
    }

    private void buildTrustedLocationsPage() {
        SettingsTrustedLocationsPage page = new SettingsTrustedLocationsPage();
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.getChildren().setAll(page);
    }

    private void buildPathVariablesPage() {
        SettingsPathVariablesPage page = new SettingsPathVariablesPage();
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.getChildren().setAll(page);
    }

    private void buildPresentationAssistantPage() {
        SettingsPresentationAssistantPage page = new SettingsPresentationAssistantPage();
        wrapInScroll(page);
    }

    private void buildColorSchemePage(String pageName) {
        if ("Color Scheme Font".equals(pageName)) {
            if (currentColorSchemeFontPage == null) {
                currentColorSchemeFontPage = new SettingsColorSchemeFontPage();
            }
            currentColorSchemeFontPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeFontPage);
            return;
        }
        if ("Console Font".equals(pageName)) {
            if (currentConsoleFontPage == null) {
                currentConsoleFontPage = new SettingsConsoleFontPage();
            }
            currentConsoleFontPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentConsoleFontPage);
            return;
        }
        if ("Console Colors".equals(pageName)) {
            if (currentConsoleColorsPage == null) {
                currentConsoleColorsPage = new SettingsConsoleColorsPage();
            }
            currentConsoleColorsPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentConsoleColorsPage);
            return;
        }
        if ("Code With Me".equals(pageName)) {
            if (currentCodeWithMePage == null) {
                currentCodeWithMePage = new SettingsCodeWithMePage();
            }
            currentCodeWithMePage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentCodeWithMePage);
            return;
        }
        if ("Debugger".equals(pageName)) {
            buildDebuggerPage();
            return;
        }
        if ("Diff & Merge".equals(pageName)) {
            if (currentColorSchemeDiffMergePage == null) {
                currentColorSchemeDiffMergePage = new SettingsColorSchemeDiffMergePage();
            }
            currentColorSchemeDiffMergePage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeDiffMergePage);
            return;
        }
        if ("JVM Logging".equals(pageName)) {
            if (currentColorSchemeJvmLoggingPage == null) {
                currentColorSchemeJvmLoggingPage = new SettingsColorSchemeJvmLoggingPage();
            }
            currentColorSchemeJvmLoggingPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeJvmLoggingPage);
            return;
        }
        if ("User-Defined File Types".equals(pageName)) {
            if (currentColorSchemeUserDefinedFileTypesPage == null) {
                currentColorSchemeUserDefinedFileTypesPage = new SettingsColorSchemeUserDefinedFileTypesPage();
            }
            currentColorSchemeUserDefinedFileTypesPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeUserDefinedFileTypesPage);
            return;
        }
        if ("VCS".equals(pageName)) {
            if (currentColorSchemeVcsPage == null) {
                currentColorSchemeVcsPage = new SettingsColorSchemeVcsPage();
            }
            currentColorSchemeVcsPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeVcsPage);
            return;
        }
        if ("Java".equals(pageName)) {
            if (currentColorSchemeJavaPage == null) {
                currentColorSchemeJavaPage = new SettingsColorSchemeJavaPage();
            }
            currentColorSchemeJavaPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeJavaPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeJavaPage);
            return;
        }
        if ("Angular Template".equals(pageName)) {
            if (currentColorSchemeAngularTemplatePage == null) {
                currentColorSchemeAngularTemplatePage = new SettingsColorSchemeAngularTemplatePage();
            }
            currentColorSchemeAngularTemplatePage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeAngularTemplatePage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeAngularTemplatePage);
            return;
        }
        if ("Context Free Grammar".equals(pageName)) {
            if (currentColorSchemeContextFreeGrammarPage == null) {
                currentColorSchemeContextFreeGrammarPage = new SettingsColorSchemeContextFreeGrammarPage();
            }
            currentColorSchemeContextFreeGrammarPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeContextFreeGrammarPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeContextFreeGrammarPage);
            return;
        }
        if ("CSS".equals(pageName)) {
            if (currentColorSchemeCssPage == null) {
                currentColorSchemeCssPage = new SettingsColorSchemeCssPage();
            }
            currentColorSchemeCssPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeCssPage);
            return;
        }
        if ("Data Editor and Viewer".equals(pageName)) {
            if (currentColorSchemeDataEditorViewerPage == null) {
                currentColorSchemeDataEditorViewerPage = new SettingsColorSchemeDataEditorViewerPage();
            }
            currentColorSchemeDataEditorViewerPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeDataEditorViewerPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("General");
                if (currentColorSchemeGeneralPage != null) {
                    currentColorSchemeGeneralPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeDataEditorViewerPage);
            return;
        }
        if ("Database".equals(pageName)) {
            if (currentColorSchemeDatabasePage == null) {
                currentColorSchemeDatabasePage = new SettingsColorSchemeDatabasePage();
            }
            currentColorSchemeDatabasePage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeDatabasePage);
            return;
        }
        if ("Diagrams".equals(pageName)) {
            if (currentColorSchemeDiagramsPage == null) {
                currentColorSchemeDiagramsPage = new SettingsColorSchemeDiagramsPage();
            }
            currentColorSchemeDiagramsPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeDiagramsPage);
            return;
        }
        if ("Dockerfile".equals(pageName)) {
            if (currentColorSchemeDockerfilePage == null) {
                currentColorSchemeDockerfilePage = new SettingsColorSchemeDockerfilePage();
            }
            currentColorSchemeDockerfilePage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeDockerfilePage);
            return;
        }
        if ("EditorConfig".equals(pageName)) {
            if (currentColorSchemeEditorConfigPage == null) {
                currentColorSchemeEditorConfigPage = new SettingsColorSchemeEditorConfigPage();
            }
            currentColorSchemeEditorConfigPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeEditorConfigPage);
            return;
        }
        if ("ERB".equals(pageName)) {
            if (currentColorSchemeErbPage == null) {
                currentColorSchemeErbPage = new SettingsColorSchemeErbPage();
            }
            currentColorSchemeErbPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeErbPage);
            return;
        }
        if ("FreeMarker".equals(pageName)) {
            if (currentColorSchemeFreeMarkerPage == null) {
                currentColorSchemeFreeMarkerPage = new SettingsColorSchemeFreeMarkerPage();
            }
            currentColorSchemeFreeMarkerPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeFreeMarkerPage);
            return;
        }
        if ("GitLab CI Expression".equals(pageName)) {
            if (currentColorSchemeGitLabCiExpressionPage == null) {
                currentColorSchemeGitLabCiExpressionPage = new SettingsColorSchemeGitLabCiExpressionPage();
            }
            currentColorSchemeGitLabCiExpressionPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeGitLabCiExpressionPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeGitLabCiExpressionPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeGitLabCiExpressionPage);
            return;
        }
        if ("Go".equals(pageName)) {
            if (currentColorSchemeGoPage == null) {
                currentColorSchemeGoPage = new SettingsColorSchemeGoPage();
            }
            currentColorSchemeGoPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeGoPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeGoPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeGoPage);
            return;
        }
        if ("Gradle Declarative Configuration".equals(pageName)) {
            if (currentColorSchemeGradleDeclarativePage == null) {
                currentColorSchemeGradleDeclarativePage = new SettingsColorSchemeGradleDeclarativePage();
            }
            currentColorSchemeGradleDeclarativePage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeGradleDeclarativePage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeGradleDeclarativePage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeGradleDeclarativePage);
            return;
        }
        if ("Groovy".equals(pageName)) {
            if (currentColorSchemeGroovyPage == null) {
                currentColorSchemeGroovyPage = new SettingsColorSchemeGroovyPage();
            }
            currentColorSchemeGroovyPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeGroovyPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeGroovyPage.setOnNavigateToInheritedListener(targetKey -> {
                if (targetKey != null && targetKey.endsWith("(Groovy)")) {
                    String baseKey = targetKey.substring(0, targetKey.indexOf("(Groovy)")).trim();
                    currentColorSchemeGroovyPage.selectCategory(baseKey);
                } else {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                }
            });
            wrapInScroll(currentColorSchemeGroovyPage);
            return;
        }
        if ("HTML".equals(pageName)) {
            if (currentColorSchemeHtmlPage == null) {
                currentColorSchemeHtmlPage = new SettingsColorSchemeHtmlPage();
            }
            currentColorSchemeHtmlPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeHtmlPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeHtmlPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeHtmlPage);
            return;
        }
        if ("HTTP Request".equals(pageName)) {
            if (currentColorSchemeHttpRequestPage == null) {
                currentColorSchemeHttpRequestPage = new SettingsColorSchemeHttpRequestPage();
            }
            currentColorSchemeHttpRequestPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeHttpRequestPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeHttpRequestPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeHttpRequestPage);
            return;
        }
        if ("JavaScript".equals(pageName)) {
            if (currentColorSchemeJavaScriptPage == null) {
                currentColorSchemeJavaScriptPage = new SettingsColorSchemeJavaScriptPage();
            }
            currentColorSchemeJavaScriptPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeJavaScriptPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeJavaScriptPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeJavaScriptPage);
            return;
        }
        if ("JPA/Hibernate QL".equals(pageName)) {
            if (currentColorSchemeJpaHibernateQlPage == null) {
                currentColorSchemeJpaHibernateQlPage = new SettingsColorSchemeJpaHibernateQlPage();
            }
            currentColorSchemeJpaHibernateQlPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeJpaHibernateQlPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeJpaHibernateQlPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeJpaHibernateQlPage);
            return;
        }
        if ("JSON".equals(pageName)) {
            if (currentColorSchemeJsonPage == null) {
                currentColorSchemeJsonPage = new SettingsColorSchemeJsonPage();
            }
            currentColorSchemeJsonPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeJsonPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeJsonPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeJsonPage);
            return;
        }
        if ("JSONPath".equals(pageName)) {
            if (currentColorSchemeJsonPathPage == null) {
                currentColorSchemeJsonPathPage = new SettingsColorSchemeJsonPathPage();
            }
            currentColorSchemeJsonPathPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeJsonPathPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeJsonPathPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeJsonPathPage);
            return;
        }
        if ("JSP".equals(pageName)) {
            if (currentColorSchemeJspPage == null) {
                currentColorSchemeJspPage = new SettingsColorSchemeJspPage();
            }
            currentColorSchemeJspPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeJspPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeJspPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeJspPage);
            return;
        }
        if ("Jupyter Notebooks".equals(pageName)) {
            if (currentColorSchemeJupyterNotebooksPage == null) {
                currentColorSchemeJupyterNotebooksPage = new SettingsColorSchemeJupyterNotebooksPage();
            }
            currentColorSchemeJupyterNotebooksPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeJupyterNotebooksPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeJupyterNotebooksPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeJupyterNotebooksPage);
            return;
        }
        if ("Kotlin".equals(pageName)) {
            if (currentColorSchemeKotlinPage == null) {
                currentColorSchemeKotlinPage = new SettingsColorSchemeKotlinPage();
            }
            currentColorSchemeKotlinPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeKotlinPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeKotlinPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeKotlinPage);
            return;
        }
        if ("Kubernetes".equals(pageName)) {
            if (currentColorSchemeKubernetesPage == null) {
                currentColorSchemeKubernetesPage = new SettingsColorSchemeKubernetesPage();
            }
            currentColorSchemeKubernetesPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeKubernetesPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeKubernetesPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeKubernetesPage);
            return;
        }
        if ("Less".equals(pageName)) {
            if (currentColorSchemeLessPage == null) {
                currentColorSchemeLessPage = new SettingsColorSchemeLessPage();
            }
            currentColorSchemeLessPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeLessPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeLessPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("CSS");
                if (currentColorSchemeCssPage != null) {
                    currentColorSchemeCssPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeLessPage);
            return;
        }
        if ("Lombok Config".equals(pageName)) {
            if (currentColorSchemeLombokConfigPage == null) {
                currentColorSchemeLombokConfigPage = new SettingsColorSchemeLombokConfigPage();
            }
            currentColorSchemeLombokConfigPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeLombokConfigPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeLombokConfigPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeLombokConfigPage);
            return;
        }
        if ("Markdown".equals(pageName)) {
            if (currentColorSchemeMarkdownPage == null) {
                currentColorSchemeMarkdownPage = new SettingsColorSchemeMarkdownPage();
            }
            currentColorSchemeMarkdownPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeMarkdownPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeMarkdownPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeMarkdownPage);
            return;
        }
        if ("Micronaut EL".equals(pageName)) {
            if (currentColorSchemeMicronautELPage == null) {
                currentColorSchemeMicronautELPage = new SettingsColorSchemeMicronautELPage();
            }
            currentColorSchemeMicronautELPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeMicronautELPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeMicronautELPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeMicronautELPage);
            return;
        }
        if ("PHP".equals(pageName)) {
            if (currentColorSchemePhpPage == null) {
                currentColorSchemePhpPage = new SettingsColorSchemePHPPage();
            }
            currentColorSchemePhpPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemePhpPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemePhpPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemePhpPage);
            return;
        }
        if ("plan9_x86".equals(pageName)) {
            if (currentColorSchemePlan9X86Page == null) {
                currentColorSchemePlan9X86Page = new SettingsColorSchemePlan9X86Page();
            }
            currentColorSchemePlan9X86Page.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemePlan9X86Page.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemePlan9X86Page.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemePlan9X86Page);
            return;
        }
        if ("PostCSS".equals(pageName)) {
            if (currentColorSchemePostCSSPage == null) {
                currentColorSchemePostCSSPage = new SettingsColorSchemePostCSSPage();
            }
            currentColorSchemePostCSSPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemePostCSSPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemePostCSSPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("CSS");
                if (currentColorSchemeCssPage != null) {
                    currentColorSchemeCssPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemePostCSSPage);
            return;
        }
        if ("Protocol Buffer".equals(pageName)) {
            if (currentColorSchemeProtocolBufferPage == null) {
                currentColorSchemeProtocolBufferPage = new SettingsColorSchemeProtocolBufferPage();
            }
            currentColorSchemeProtocolBufferPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeProtocolBufferPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeProtocolBufferPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeProtocolBufferPage);
            return;
        }
        if ("MongoDB JSON".equals(pageName)) {
            if (currentColorSchemeMongoDbJsonPage == null) {
                currentColorSchemeMongoDbJsonPage = new SettingsColorSchemeMongoDBJSONPage();
            }
            currentColorSchemeMongoDbJsonPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeMongoDbJsonPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeMongoDbJsonPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeMongoDbJsonPage);
            return;
        }
        if ("Properties".equals(pageName)) {
            if (currentColorSchemePropertiesPage == null) {
                currentColorSchemePropertiesPage = new SettingsColorSchemePropertiesPage();
            }
            currentColorSchemePropertiesPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemePropertiesPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemePropertiesPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemePropertiesPage);
            return;
        }
        if ("Protocol Buffer Text".equals(pageName)) {
            if (currentColorSchemeProtocolBufferTextPage == null) {
                currentColorSchemeProtocolBufferTextPage = new SettingsColorSchemeProtocolBufferTextPage();
            }
            currentColorSchemeProtocolBufferTextPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeProtocolBufferTextPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeProtocolBufferTextPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeProtocolBufferTextPage);
            return;
        }
        if ("Python".equals(pageName)) {
            if (currentColorSchemePythonPage == null) {
                currentColorSchemePythonPage = new SettingsColorSchemePythonPage();
            }
            currentColorSchemePythonPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemePythonPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemePythonPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemePythonPage);
            return;
        }
        if ("Qute".equals(pageName)) {
            if (currentColorSchemeQutePage == null) {
                currentColorSchemeQutePage = new SettingsColorSchemeQutePage();
            }
            currentColorSchemeQutePage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeQutePage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeQutePage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeQutePage);
            return;
        }
        if ("RDoc".equals(pageName)) {
            if (currentColorSchemeRDocPage == null) {
                currentColorSchemeRDocPage = new SettingsColorSchemeRDocPage();
            }
            currentColorSchemeRDocPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeRDocPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeRDocPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeRDocPage);
            return;
        }
        if ("RegExp".equals(pageName)) {
            if (currentColorSchemeRegExpPage == null) {
                currentColorSchemeRegExpPage = new SettingsColorSchemeRegExpPage();
            }
            currentColorSchemeRegExpPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeRegExpPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeRegExpPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeRegExpPage);
            return;
        }
        if ("Ruby".equals(pageName)) {
            if (currentColorSchemeRubyPage == null) {
                currentColorSchemeRubyPage = new SettingsColorSchemeRubyPage();
            }
            currentColorSchemeRubyPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeRubyPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeRubyPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeRubyPage);
            return;
        }
        if ("Rust".equals(pageName)) {
            if (currentColorSchemeRustPage == null) {
                currentColorSchemeRustPage = new SettingsColorSchemeRustPage();
            }
            currentColorSchemeRustPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeRustPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeRustPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeRustPage);
            return;
        }
        if ("Sass/SCSS".equals(pageName)) {
            if (currentColorSchemeSassPage == null) {
                currentColorSchemeSassPage = new SettingsColorSchemeSassPage();
            }
            currentColorSchemeSassPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeSassPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeSassPage.setOnNavigateToInheritedListener(targetKey -> {
                AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(currentColorSchemeSassPage.getSelectedKey());
                String scope = desc != null ? desc.getInheritScope() : "(CSS)";
                if (scope != null && scope.contains("CSS")) {
                    selectCategory("CSS");
                    if (currentColorSchemeCssPage != null) {
                        currentColorSchemeCssPage.selectTreeItem(targetKey);
                    }
                } else {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                }
            });
            wrapInScroll(currentColorSchemeSassPage);
            return;
        }
        if ("Scala".equals(pageName)) {
            if (currentColorSchemeScalaPage == null) {
                currentColorSchemeScalaPage = new SettingsColorSchemeScalaPage();
            }
            currentColorSchemeScalaPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeScalaPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeScalaPage.setOnNavigateToInheritedListener(targetKey -> {
                AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(currentColorSchemeScalaPage.getSelectedKey());
                String scope = desc != null ? desc.getInheritScope() : "(Java)";
                if (scope != null && scope.contains("Java")) {
                    selectCategory("Java");
                    if (currentColorSchemeJavaPage != null) {
                        currentColorSchemeJavaPage.selectTreeItem(targetKey);
                    }
                } else {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                }
            });
            wrapInScroll(currentColorSchemeScalaPage);
            return;
        }
        if ("Shell Script".equals(pageName)) {
            if (currentColorSchemeShellScriptPage == null) {
                currentColorSchemeShellScriptPage = new SettingsColorSchemeShellScriptPage();
            }
            currentColorSchemeShellScriptPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeShellScriptPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeShellScriptPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeShellScriptPage);
            return;
        }
        if ("Smarty".equals(pageName)) {
            if (currentColorSchemeSmartyPage == null) {
                currentColorSchemeSmartyPage = new SettingsColorSchemeSmartyPage();
            }
            currentColorSchemeSmartyPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeSmartyPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeSmartyPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeSmartyPage);
            return;
        }
        if ("Spring EL".equals(pageName)) {
            if (currentColorSchemeSpringELPage == null) {
                currentColorSchemeSpringELPage = new SettingsColorSchemeSpringELPage();
            }
            currentColorSchemeSpringELPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeSpringELPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeSpringELPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeSpringELPage);
            return;
        }
        if ("SQL".equals(pageName)) {
            if (currentColorSchemeSQLPage == null) {
                currentColorSchemeSQLPage = new SettingsColorSchemeSQLPage();
            }
            currentColorSchemeSQLPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeSQLPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeSQLPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeSQLPage);
            return;
        }
        if ("Table Diff".equals(pageName)) {
            if (currentColorSchemeTableDiffPage == null) {
                currentColorSchemeTableDiffPage = new SettingsColorSchemeTableDiffPage();
            }
            currentColorSchemeTableDiffPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeTableDiffPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeTableDiffPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Color Scheme", "General");
                if (currentColorSchemeGeneralPage != null) {
                    currentColorSchemeGeneralPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeTableDiffPage);
            return;
        }
        if ("TOML".equals(pageName)) {
            if (currentColorSchemeTomlPage == null) {
                currentColorSchemeTomlPage = new SettingsColorSchemeTOMLPage();
            }
            currentColorSchemeTomlPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeTomlPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeTomlPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeTomlPage);
            return;
        }
        if ("TypeScript".equals(pageName)) {
            if (currentColorSchemeTypeScriptPage == null) {
                currentColorSchemeTypeScriptPage = new SettingsColorSchemeTypeScriptPage();
            }
            currentColorSchemeTypeScriptPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeTypeScriptPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeTypeScriptPage.setOnNavigateToInheritedListener(targetKey -> {
                if (targetKey.contains("Static property")) {
                    currentColorSchemeTypeScriptPage.selectTreeItem(targetKey);
                } else {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                }
            });
            wrapInScroll(currentColorSchemeTypeScriptPage);
            return;
        }
        if ("Velocity".equals(pageName)) {
            if (currentColorSchemeVelocityPage == null) {
                currentColorSchemeVelocityPage = new SettingsColorSchemeVelocityPage();
            }
            currentColorSchemeVelocityPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeVelocityPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeVelocityPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeVelocityPage);
            return;
        }
        if ("XML".equals(pageName)) {
            if (currentColorSchemeXmlPage == null) {
                currentColorSchemeXmlPage = new SettingsColorSchemeXMLPage();
            }
            currentColorSchemeXmlPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeXmlPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeXmlPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeXmlPage);
            return;
        }
        if ("XPath".equals(pageName)) {
            if (currentColorSchemeXPathPage == null) {
                currentColorSchemeXPathPage = new SettingsColorSchemeXPathPage();
            }
            currentColorSchemeXPathPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeXPathPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeXPathPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeXPathPage);
            return;
        }
        if ("XSLT".equals(pageName)) {
            if (currentColorSchemeXsltPage == null) {
                currentColorSchemeXsltPage = new SettingsColorSchemeXSLTPage();
            }
            currentColorSchemeXsltPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeXsltPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeXsltPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeXsltPage);
            return;
        }
        if ("YAML".equals(pageName)) {
            if (currentColorSchemeYamlPage == null) {
                currentColorSchemeYamlPage = new SettingsColorSchemeYAMLPage();
            }
            currentColorSchemeYamlPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeYamlPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeYamlPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeYamlPage);
            return;
        }
        if ("By Scope".equals(pageName)) {
            if (currentColorSchemeByScopePage == null) {
                currentColorSchemeByScopePage = new SettingsColorSchemeByScopePage();
            }
            currentColorSchemeByScopePage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeByScopePage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeByScopePage.setOnManageScopesListener(() -> selectCategory("Scopes"));
            wrapInScroll(currentColorSchemeByScopePage);
            return;
        }
        if ("Images".equals(pageName)) {
            if (currentColorSchemeImagesPage == null) {
                currentColorSchemeImagesPage = new SettingsColorSchemeImagesPage();
            }
            currentColorSchemeImagesPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeImagesPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            wrapInScroll(currentColorSchemeImagesPage);
            return;
        }
        VBox page = new VBox(12);
        page.setPadding(new Insets(16, 24, 20, 24));
        page.setStyle("-fx-background-color: #1E1F22;");
        ColorSchemeHeaderBar bar = new ColorSchemeHeaderBar();
        Label title = new Label(pageName);
        title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label desc = new Label("Configure color scheme settings for " + pageName + ".");
        desc.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");
        page.getChildren().addAll(bar, title, desc);
        wrapInScroll(page);
    }

    private void buildSystemSettingsPage() {
        currentSystemPage = new SettingsSystemPage();
        currentSystemPage.showSubPage("System Settings");
        wrapInScroll(currentSystemPage);
    }

    private void buildSystemSettingsSubPage(String pageName) {
        currentSystemPage = new SettingsSystemPage();
        currentSystemPage.showSubPage(pageName);
        wrapInScroll(currentSystemPage);
    }

    private boolean isSystemSettingsSubPage(String pageName) {
        return List.of(
                "Data Sharing", "Date Formats", "HTTP Proxy", "Language and Region",
                "Passwords", "Process Elevation", "Server Certificates", "Trusted Hosts", "Updates"
        ).contains(pageName);
    }

    private boolean isEditorSubPage(String pageName) {
        return List.of(
                "General", "Auto Import", "Appearance", "Breadcrumbs", "Code Completion",
                "Code Folding", "Console", "Editor Tabs", "Gutter Icons", "Inline Completion",
                "Postfix Completion", "Sticky Lines", "Smart Keys",
                "Code Editing", "Font", "Color Scheme",
                "Code Style", "Inspections", "File and Code Templates", "File Encodings",
                "Live Templates", "File Types", "Copyright", "Inlay Hints", "Duplicates",
                "Emmet", "Intentions", "Language Injections", "Advanced", "Natural Languages",
                "Grammar and Style", "Spelling",
                "Reader Mode", "TextMate Bundles", "TODO"
        ).contains(pageName) || dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider.getProvider(pageName) != null;
    }

    private boolean isKeymapPage(String pageName) {
        return "Keymap".equals(pageName) || List.of(
                "Editor Actions", "Main Menu", "Tool Windows", "External Tools",
                "External Build Systems", "Version Control Systems", "Debugger Actions",
                "Remote External Tools", "Database", "Macros",
                "Quick Lists", "Other"
        ).contains(pageName);
    }

    // --------------------------------------------------- Category Tree

    private TreeItem<String> buildCodeStyleTree() {
        TreeItem<String> codeStyle = new TreeItem<>("Code Style");
        for (dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider p : dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider.getAllProviders()) {
            codeStyle.getChildren().add(new TreeItem<>(p.getDisplayName()));
        }
        return codeStyle;
    }

    private TreeItem<String> buildEmmetTree() {
        TreeItem<String> emmet = new TreeItem<>("Emmet");
        emmet.getChildren().addAll(
                new TreeItem<>("CSS"),
                new TreeItem<>("HTML"),
                new TreeItem<>("JSX")
        );
        return emmet;
    }

    private TreeItem<String> buildLanguageInjectionsTree() {
        TreeItem<String> root = new TreeItem<>("Language Injections");
        root.getChildren().add(new TreeItem<>("Advanced"));
        root.setExpanded(true);
        return root;
    }

    private TreeItem<String> buildNaturalLanguagesTree() {
        TreeItem<String> root = new TreeItem<>("Natural Languages");
        root.getChildren().addAll(
                new TreeItem<>("Grammar and Style"),
                new TreeItem<>("Spelling")
        );
        root.setExpanded(true);
        return root;
    }

    private TreeView<String> buildCategoryTree() {
        TreeItem<String> root = new TreeItem<>("Settings");
        root.setExpanded(true);

        // Appearance & Behavior
        TreeItem<String> appearance = new TreeItem<>("Appearance & Behavior");
        TreeItem<String> appearanceSub = new TreeItem<>("Appearance");
        TreeItem<String> menus = new TreeItem<>("Menus and Toolbars");

        // System Settings with sub-pages
        TreeItem<String> system = new TreeItem<>("System Settings");
        system.getChildren().addAll(
                new TreeItem<>("Data Sharing"),
                new TreeItem<>("Date Formats"),
                new TreeItem<>("HTTP Proxy"),
                new TreeItem<>("Language and Region"),
                new TreeItem<>("Passwords"),
                new TreeItem<>("Process Elevation"),
                new TreeItem<>("Server Certificates"),
                new TreeItem<>("Trusted Hosts"),
                new TreeItem<>("Updates")
        );

        appearance.getChildren().addAll(
                appearanceSub, menus, system,
                new TreeItem<>("File Colors"),
                new TreeItem<>("Scopes"),
                new TreeItem<>("Notifications"),
                new TreeItem<>("Data Editor and Viewer"),
                new TreeItem<>("Quick Lists"),
                new TreeItem<>("Required Plugins"),
                new TreeItem<>("Trusted Locations"),
                new TreeItem<>("Path Variables"),
                new TreeItem<>("Presentation Assistant")
        );

        // Keymap
        TreeItem<String> keymap = new TreeItem<>("Keymap");

        // Editor
        TreeItem<String> editor = new TreeItem<>("Editor");
        TreeItem<String> general = new TreeItem<>("General");

        TreeItem<String> smartKeys = new TreeItem<>("Smart Keys");
        smartKeys.getChildren().addAll(
                new TreeItem<>("YAML"),
                new TreeItem<>("HTML/CSS"),
                new TreeItem<>("Python"),
                new TreeItem<>("JSON"),
                new TreeItem<>("Rust"),
                new TreeItem<>("Markdown"),
                new TreeItem<>("Scala"),
                new TreeItem<>("SQL"),
                new TreeItem<>("Ruby"),
                new TreeItem<>("JavaScript"),
                new TreeItem<>("PHP")
        );

        general.getChildren().addAll(
                new TreeItem<>("Auto Import"),
                new TreeItem<>("Appearance"),
                new TreeItem<>("Breadcrumbs"),
                new TreeItem<>("Code Completion"),
                new TreeItem<>("Code Folding"),
                new TreeItem<>("Console"),
                new TreeItem<>("Editor Tabs"),
                new TreeItem<>("Gutter Icons"),
                new TreeItem<>("Inline Completion"),
                new TreeItem<>("Postfix Completion"),
                smartKeys,
                new TreeItem<>("Sticky Lines")
        );

        TreeItem<String> colorSchemeNode = new TreeItem<>("Color Scheme");
        colorSchemeNode.getChildren().addAll(
                new TreeItem<>("General"),
                new TreeItem<>("Language Defaults"),
                new TreeItem<>("Color Scheme Font"),
                new TreeItem<>("Console Font"),
                new TreeItem<>("Code With Me"),
                new TreeItem<>("Console Colors"),
                new TreeItem<>("Debugger"),
                new TreeItem<>("Diff & Merge"),
                new TreeItem<>("JVM Logging"),
                new TreeItem<>("User-Defined File Types"),
                new TreeItem<>("VCS"),
                new TreeItem<>("Java"),
                new TreeItem<>("Angular Template"),
                new TreeItem<>("Context Free Grammar"),
                new TreeItem<>("CSS"),
                new TreeItem<>("Data Editor and Viewer"),
                new TreeItem<>("Database"),
                new TreeItem<>("Diagrams"),
                new TreeItem<>("Dockerfile"),
                new TreeItem<>("EditorConfig"),
                new TreeItem<>("ERB"),
                new TreeItem<>("FreeMarker"),
                new TreeItem<>("GitLab CI Expression"),
                new TreeItem<>("Go"),
                new TreeItem<>("Gradle Declarative Configuration"),
                new TreeItem<>("Groovy"),
                new TreeItem<>("HTML"),
                new TreeItem<>("HTTP Request"),
                new TreeItem<>("JavaScript"),
                new TreeItem<>("JPA/Hibernate QL"),
                new TreeItem<>("JSON"),
                new TreeItem<>("JSONPath"),
                new TreeItem<>("JSP"),
                new TreeItem<>("Jupyter Notebooks"),
                new TreeItem<>("Kotlin"),
                new TreeItem<>("Kubernetes"),
                new TreeItem<>("Less"),
                new TreeItem<>("Lombok Config"),
                new TreeItem<>("Markdown"),
                new TreeItem<>("Micronaut EL"),
                new TreeItem<>("MongoDB JSON"),
                new TreeItem<>("PHP"),
                new TreeItem<>("plan9_x86"),
                new TreeItem<>("PostCSS"),
                new TreeItem<>("Properties"),
                new TreeItem<>("Protocol Buffer"),
                new TreeItem<>("Protocol Buffer Text"),
                new TreeItem<>("Python"),
                new TreeItem<>("Qute"),
                new TreeItem<>("RDoc"),
                new TreeItem<>("RegExp"),
                new TreeItem<>("Ruby"),
                new TreeItem<>("Rust"),
                new TreeItem<>("Sass/SCSS"),
                new TreeItem<>("Scala"),
                new TreeItem<>("Shell Script"),
                new TreeItem<>("Smarty"),
                new TreeItem<>("Spring EL"),
                new TreeItem<>("SQL"),
                new TreeItem<>("Table Diff"),
                new TreeItem<>("TOML"),
                new TreeItem<>("TypeScript"),
                new TreeItem<>("Velocity"),
                new TreeItem<>("XML"),
                new TreeItem<>("XPath"),
                new TreeItem<>("XSLT"),
                new TreeItem<>("YAML"),
                new TreeItem<>("By Scope"),
                new TreeItem<>("Images")
        );

        TreeItem<String> copyright = new TreeItem<>("Copyright");
        TreeItem<String> copyrightProfiles = new TreeItem<>("Copyright Profiles");
        TreeItem<String> formatting = new TreeItem<>("Formatting");
        formatting.getChildren().addAll(
                new TreeItem<>("CSS"), new TreeItem<>("DTD"), new TreeItem<>("Groovy"),
                new TreeItem<>("HTML"), new TreeItem<>("Java"), new TreeItem<>("JavaScript"),
                new TreeItem<>("JSP"), new TreeItem<>("JSPX"), new TreeItem<>("Kotlin"),
                new TreeItem<>("Less"), new TreeItem<>("PostCSS"), new TreeItem<>("Properties"),
                new TreeItem<>("Rust"), new TreeItem<>("Sass"), new TreeItem<>("SCSS"),
                new TreeItem<>("Shell Script"), new TreeItem<>("SPI"), new TreeItem<>("Spring Boot SPI"),
                new TreeItem<>("SQL"), new TreeItem<>("SVG"), new TreeItem<>("TypeScript"),
                new TreeItem<>("Vue template"), new TreeItem<>("XHTML"), new TreeItem<>("XML")
        );
        copyright.getChildren().addAll(copyrightProfiles, formatting);

        editor.getChildren().addAll(
                general,
                new TreeItem<>("Code Editing"),
                new TreeItem<>("Font"),
                colorSchemeNode,
                buildCodeStyleTree(),
                new TreeItem<>("Inspections"),
                new TreeItem<>("File and Code Templates"),
                new TreeItem<>("File Encodings"),
                new TreeItem<>("Live Templates"),
                new TreeItem<>("File Types"),
                copyright,
                new TreeItem<>("Inlay Hints"),
                new TreeItem<>("Duplicates"),
                buildEmmetTree(),
                new TreeItem<>("Intentions"),
                buildLanguageInjectionsTree(),
                buildNaturalLanguagesTree(),
                new TreeItem<>("Reader Mode"),
                new TreeItem<>("TextMate Bundles"),
                new TreeItem<>("TODO")
        );

        // Additional root categories
        TreeItem<String> plugins = new TreeItem<>("Plugins");
        TreeItem<String> versionControl = new TreeItem<>("Version Control");
        TreeItem<String> perforce = new TreeItem<>("Perforce");
        perforce.getChildren().add(new TreeItem<>("Perforce MCP"));
        TreeItem<String> subversion = new TreeItem<>("Subversion");
        subversion.getChildren().addAll(
                new TreeItem<>("Network"),
                new TreeItem<>("Presentation"),
                new TreeItem<>("SSH")
        );

        versionControl.getChildren().addAll(
                new TreeItem<>("Changelists"),
                new TreeItem<>("Commit"),
                new TreeItem<>("Confirmation"),
                new TreeItem<>("Directory Mappings"),
                new TreeItem<>("File Status Colors"),
                new TreeItem<>("Issue Navigation"),
                new TreeItem<>("Log"),
                new TreeItem<>("Shelf"),
                new TreeItem<>("Git"),
                new TreeItem<>("GitHub"),
                new TreeItem<>("GitLab"),
                new TreeItem<>("Mercurial"),
                perforce,
                subversion
        );
        TreeItem<String> build = new TreeItem<>("Build, Execution, Deployment");

        TreeItem<String> buildTools = new TreeItem<>("Build Tools");
        TreeItem<String> maven = new TreeItem<>("Maven");
        maven.getChildren().addAll(
                new TreeItem<>("Archetype Catalogs"),
                new TreeItem<>("Ignored Files"),
                new TreeItem<>("Importing"),
                new TreeItem<>("Repositories"),
                new TreeItem<>("Runner"),
                new TreeItem<>("Running Tests")
        );
        buildTools.getChildren().addAll(
                maven,
                new TreeItem<>("Gradle"),
                new TreeItem<>("Gant"),
                new TreeItem<>("BSP"),
                new TreeItem<>("Cargo"),
                new TreeItem<>("sbt")
        );

        TreeItem<String> compiler = new TreeItem<>("Compiler");
        TreeItem<String> scalaCompiler = new TreeItem<>("Scala Compiler");
        scalaCompiler.getChildren().addAll(
                new TreeItem<>("Bytecode Indices"),
                new TreeItem<>("Scala Compile Server")
        );
        compiler.getChildren().addAll(
                new TreeItem<>("Annotation Processors"),
                new TreeItem<>("Excludes"),
                new TreeItem<>("Groovy Compiler"),
                new TreeItem<>("Java Compiler"),
                new TreeItem<>("Kotlin Compiler"),
                new TreeItem<>("RMI Compiler"),
                scalaCompiler,
                new TreeItem<>("Validation")
        );

        TreeItem<String> console = new TreeItem<>("Console");
        console.getChildren().addAll(
                new TreeItem<>("Gant"),
                new TreeItem<>("Groovy Console"),
                new TreeItem<>("Python Console")
        );

        TreeItem<String> debugger = new TreeItem<>("Debugger");
        TreeItem<String> dataViews = new TreeItem<>("Data Views");
        dataViews.getChildren().addAll(
                new TreeItem<>("Java"),
                new TreeItem<>("Java Type Renderers"),
                new TreeItem<>("JavaScript")
        );
        debugger.getChildren().addAll(
                new TreeItem<>("Async Stack Traces"),
                dataViews,
                new TreeItem<>("HotSwap"),
                new TreeItem<>("Stepping")
        );

        TreeItem<String> deployment = new TreeItem<>("Deployment");
        deployment.getChildren().addAll(
                new TreeItem<>("Options")
        );

        TreeItem<String> docker = new TreeItem<>("Docker");
        docker.getChildren().addAll(
                new TreeItem<>("Console"),
                new TreeItem<>("Docker Registry")
        );

        TreeItem<String> javaProfiler = new TreeItem<>("Java Profiler");
        javaProfiler.getChildren().addAll(
                new TreeItem<>("Filters")
        );

        build.getChildren().addAll(
                new TreeItem<>("Python Debugger"),
                new TreeItem<>("Application Servers"),
                buildTools,
                compiler,
                console,
                new TreeItem<>("Coverage"),
                debugger,
                deployment,
                docker,
                javaProfiler,
                new TreeItem<>("Kubernetes"),
                new TreeItem<>("Remote Jar Repositories"),
                new TreeItem<>("Run Targets")
        );
        TreeItem<String> languages = new TreeItem<>("Languages & Frameworks");
        TreeItem<String> phpItem = new TreeItem<>("PHP");
        TreeItem<String> debugItem = new TreeItem<>("Debug");
        debugItem.getChildren().addAll(
                new TreeItem<>("Templates"),
                new TreeItem<>("DBGp Proxy"),
                new TreeItem<>("Skipped Paths"),
                new TreeItem<>("Step Filters"),
                new TreeItem<>("Xdebug Cloud")
        );
        TreeItem<String> qualityToolsItem = new TreeItem<>("Quality Tools");
        qualityToolsItem.getChildren().addAll(
                new TreeItem<>("PHP_CodeSniffer"),
                new TreeItem<>("PHP CS Fixer"),
                new TreeItem<>("Laravel Pint"),
                new TreeItem<>("Mess Detector")
        );
        phpItem.getChildren().addAll(
                debugItem,
                new TreeItem<>("Servers"),
                new TreeItem<>("Composer"),
                new TreeItem<>("Test Frameworks"),
                qualityToolsItem,
                new TreeItem<>("Frameworks"),
                new TreeItem<>("Smarty")
        );
        TreeItem<String> javaScriptItem = new TreeItem<>("JavaScript");
        TreeItem<String> codeQualityToolsItem = new TreeItem<>("Code Quality Tools");
        codeQualityToolsItem.getChildren().addAll(
                new TreeItem<>("ESLint"),
                new TreeItem<>("JSHint")
        );
        javaScriptItem.getChildren().addAll(
                codeQualityToolsItem,
                new TreeItem<>("Libraries"),
                new TreeItem<>("Prettier"),
                new TreeItem<>("Styled Components"),
                new TreeItem<>("Vite"),
                new TreeItem<>("Webpack")
        );
        TreeItem<String> kotlinItem = new TreeItem<>("Kotlin");
        kotlinItem.getChildren().add(new TreeItem<>("Kotlin Scripting"));
        TreeItem<String> protoItem = new TreeItem<>("Protocol Buffers");
        protoItem.getChildren().add(new TreeItem<>("Text Format"));

        TreeItem<String> scalaItem = new TreeItem<>("Scala");
        scalaItem.getChildren().addAll(
                new TreeItem<>("Editor"),
                new TreeItem<>("X-Ray Mode"),
                new TreeItem<>("Project View"),
                new TreeItem<>("Performance"),
                new TreeItem<>("Worksheet"),
                new TreeItem<>("Base Package"),
                new TreeItem<>("Misc"),
                new TreeItem<>("Updates"),
                new TreeItem<>("Extensions")
        );

        TreeItem<String> schemasItem = new TreeItem<>("Schemas and DTDs");
        schemasItem.getChildren().addAll(
                new TreeItem<>("Default XML Schemas"),
                new TreeItem<>("JSON Schema Mappings"),
                new TreeItem<>("Remote JSON Schemas"),
                new TreeItem<>("XML Catalog")
        );

        TreeItem<String> styleSheetsItem = new TreeItem<>("Style Sheets");
        styleSheetsItem.getChildren().addAll(
                new TreeItem<>("Dialects"),
                new TreeItem<>("Stylelint"),
                new TreeItem<>("Tailwind CSS")
        );

        TreeItem<String> typeScriptItem = new TreeItem<>("TypeScript");
        typeScriptItem.getChildren().addAll(
                new TreeItem<>("Angular"),
                new TreeItem<>("TSLint"),
                new TreeItem<>("Vue")
        );

        TreeItem<String> rustItem = new TreeItem<>("Rust");
        rustItem.getChildren().addAll(
                new TreeItem<>("External Linters"),
                new TreeItem<>("Rustfmt")
        );

        TreeItem<String> goItem = new TreeItem<>("Go");
        goItem.getChildren().addAll(
                new TreeItem<>("GOROOT"),
                new TreeItem<>("GOPATH"),
                new TreeItem<>("Go Modules"),
                new TreeItem<>("Build Tags"),
                new TreeItem<>("Formatting Functions"),
                new TreeItem<>("Imports")
        );

        languages.getChildren().addAll(
                phpItem,
                rustItem,
                new TreeItem<>("Python Template Languages"),
                goItem,
                new TreeItem<>("JavaFX"),
                javaScriptItem,
                new TreeItem<>("JavaScript Runtime"),
                new TreeItem<>("JVM Logging"),
                kotlinItem,
                new TreeItem<>("Ktor"),
                new TreeItem<>("Kubernetes"),
                new TreeItem<>("Lombok"),
                new TreeItem<>("Markdown"),
                new TreeItem<>("Micronaut"),
                new TreeItem<>("OpenAPI Specifications"),
                new TreeItem<>("Play"),
                protoItem,
                new TreeItem<>("Quarkus"),
                new TreeItem<>("RBS"),
                scalaItem,
                schemasItem,
                new TreeItem<>("Spring"),
                new TreeItem<>("SQL Dialects"),
                new TreeItem<>("SQL Resolution Scopes"),
                styleSheetsItem,
                new TreeItem<>("Tables"),
                new TreeItem<>("Template Data Languages"),
                typeScriptItem,
                new TreeItem<>("Web Contexts"),
                new TreeItem<>("XSLT"),
                new TreeItem<>("XSLT File Associations")
        );

        TreeItem<String> tools = new TreeItem<>("Tools");
        tools.getChildren().addAll(
                new TreeItem<>("Actions on Save"), new TreeItem<>("AI Assistant"),
                new TreeItem<>("Code Provenance"), new TreeItem<>("Code With Me"),
                new TreeItem<>("CSV Formats"), new TreeItem<>("Database"),
                new TreeItem<>("Database Versioning"), new TreeItem<>("Diagrams"),
                new TreeItem<>("Diff & Merge"), new TreeItem<>("External Tools"),
                new TreeItem<>("Features Suggester"), new TreeItem<>("Features Trainer"),
                new TreeItem<>("HTTP Client"), new TreeItem<>("JPA Entity Declaration"),
                new TreeItem<>("JPA Reverse Engineering"), new TreeItem<>("Junie"),
                new TreeItem<>("Jupyter"), new TreeItem<>("Kotlin Notebook"),
                new TreeItem<>("MCP Server"), new TreeItem<>("Qodana"),
                new TreeItem<>("Remote SSH External Tools"), new TreeItem<>("Rsync"),
                new TreeItem<>("Shared Indexes"), new TreeItem<>("SSH Configurations"),
                new TreeItem<>("SSH Terminal"), new TreeItem<>("Startup Tasks"),
                new TreeItem<>("Tasks"), new TreeItem<>("Terminal"),
                new TreeItem<>("Web Browsers and Preview"), new TreeItem<>("XPath Viewer")
        );

        TreeItem<String> backup = new TreeItem<>("Backup and Sync");
        TreeItem<String> advanced = new TreeItem<>("Advanced Settings");

        root.getChildren().addAll(
                appearance, keymap, editor, plugins, versionControl,
                build, languages, tools, backup, advanced
        );

        TreeView<String> tv = new TreeView<>(root);
        tv.setShowRoot(false);
        tv.getStyleClass().add("settings-tree");

        tv.setCellFactory(view -> new TreeCell<>() {
            private final Label titleLabel = new Label();
            private final Region spacer = new Region();
            private final Label badgeLabel = new Label();
            private final SVGPath projectIcon = new SVGPath();
            private final HBox cellBox = new HBox(4, titleLabel, spacer);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                cellBox.setAlignment(Pos.CENTER_LEFT);
                projectIcon.setContent("M 1 2 L 11 2 L 11 10 L 1 10 Z M 1 4 L 11 4");
                projectIcon.setFill(Color.TRANSPARENT);
                projectIcon.setStroke(Color.web("#6F737A"));
                projectIcon.setStrokeWidth(0.8);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    titleLabel.setText(item);
                    titleLabel.setStyle("-fx-text-fill: inherit; -fx-font-size: 13px;");

                    if ("Angular".equals(item) && isUnderTypeScript(getTreeItem())) {
                        badgeLabel.setText("Beta");
                        badgeLabel.setStyle("-fx-background-color: #5E2794; -fx-text-fill: #DFE1E5; -fx-font-size: 9px; -fx-padding: 0 4 0 4; -fx-background-radius: 4; -fx-font-weight: bold;");
                        HBox titleBadgeBox = new HBox(6, titleLabel, badgeLabel);
                        titleBadgeBox.setAlignment(Pos.CENTER_LEFT);
                        cellBox.getChildren().setAll(titleBadgeBox, spacer);
                    } else if ("Plugins".equals(item)) {
                        badgeLabel.setText(String.valueOf(dev.lumina.plugin.PluginManager.getInstance().getBadgeCount()));
                        badgeLabel.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 10px; -fx-padding: 1 6 1 6; -fx-background-radius: 8;");
                        cellBox.getChildren().setAll(titleLabel, spacer, badgeLabel);
                    } else {
                        cellBox.getChildren().setAll(titleLabel, spacer);
                    }

                    if (isProjectSetting(getTreeItem())) {
                        cellBox.getChildren().add(projectIcon);
                    }
                    setText(null);
                    setGraphic(cellBox);
                }
            }
        });

        return tv;
    }

    private static final Set<String> PROJECT_SETTINGS = new HashSet<>(Arrays.asList(
            "Version Control", "Changelists", "Commit", "Confirmation",
            "Directory Mappings", "Issue Navigation", "Log", "Shelf",
            "Git", "GitHub", "GitLab", "Mercurial", "Perforce", "Subversion",
            "Perforce MCP", "Network", "Presentation", "SSH",
            "Python Debugger", "Build Tools", "Maven", "Archetype Catalogs",
            "Ignored Files", "Importing", "Repositories", "Runner", "Running Tests",
            "Gradle", "Gant", "BSP", "Cargo", "sbt", "Compiler", "Console",
            "Coverage", "Deployment", "Docker", "Kubernetes", "Remote Jar Repositories",
            "Run Targets", "Debugger", "Async Stack Traces", "Data Views", "Java Type Renderers",
            "HotSwap", "Stepping", "Options", "Java Profiler", "Filters",
            "PHP", "JavaScript", "JavaScript Runtime", "JVM Logging", "Kotlin Scripting", "Ktor",
            "Lombok", "Markdown", "Micronaut", "OpenAPI Specifications",
            "Protocol Buffers", "Text Format", "Quarkus", "Schemas and DTDs", "Spring",
            "SQL Dialects", "SQL Resolution Scopes", "Style Sheets", "Dialects", "Stylelint", "Tailwind CSS", "Tables",
            "Template Data Languages", "TypeScript", "Angular", "TSLint", "Vue", "Web Contexts", "XSLT",
            "XSLT File Associations", "Code Quality Tools", "ESLint", "JSHint",
            "Libraries", "Prettier", "Styled Components", "Vite", "Webpack",
            "Debug", "Templates", "DBGp Proxy", "Skipped Paths", "Step Filters", "Xdebug Cloud",
            "Servers", "Composer", "Test Frameworks", "Quality Tools",
            "PHP_CodeSniffer", "PHP CS Fixer", "Laravel Pint", "Mess Detector",
            "Frameworks", "Smarty", "Rust", "External Linters", "Rustfmt",
            "Python Template Languages", "GOROOT", "GOPATH", "Go Modules", "Build Tags", "Formatting Functions", "Imports",
            "Play", "RBS", "Scala"
    ));

    private static boolean isProjectSetting(String name) {
        return name != null && PROJECT_SETTINGS.contains(name);
    }

    private boolean isProjectSetting(TreeItem<String> item) {
        if (item == null || item.getValue() == null) return false;
        String val = item.getValue();
        if ("Languages & Frameworks".equals(val)) return false;
        if (isUnderLanguages(item)) return true;
        return isProjectSetting(val);
    }

    private TreeItem<String> searchTree(TreeItem<String> root, String query) {
        for (TreeItem<String> child : root.getChildren()) {
            if (child.getValue() != null && child.getValue().toLowerCase().contains(query)) {
                return child;
            }
            TreeItem<String> sub = searchTree(child, query);
            if (sub != null) return sub;
        }
        return null;
    }

    public TreeItem<String> findItem(TreeItem<String> root, String text) {
        if (root == null || text == null) return null;
        if (root.getValue() != null && root.getValue().equals(text)) {
            return root;
        }
        if ("Profilers".equals(text) && "Java Profiler".equals(root.getValue())) {
            return root;
        }
        for (TreeItem<String> child : root.getChildren()) {
            TreeItem<String> found = findItem(child, text);
            if (found != null) return found;
        }
        return null;
    }

    public void selectCategory(String categoryName) {
        if (categoryName == null || tree == null) return;
        TreeItem<String> item = null;
        TreeItem<String> selected = tree.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TreeItem<String> p = selected;
            boolean underCS = false;
            boolean underCodeStyle = false;
            boolean underSmartKeys = false;
            boolean underBuild = false;
            boolean underLanguages = false;
            while (p != null) {
                if ("Color Scheme".equals(p.getValue())) {
                    underCS = true;
                    break;
                }
                if ("Code Style".equals(p.getValue())) {
                    underCodeStyle = true;
                    break;
                }
                if ("Smart Keys".equals(p.getValue())) {
                    underSmartKeys = true;
                    break;
                }
                if ("Build, Execution, Deployment".equals(p.getValue())) {
                    underBuild = true;
                    break;
                }
                if ("Languages & Frameworks".equals(p.getValue())) {
                    underLanguages = true;
                    break;
                }
                p = p.getParent();
            }
            if (underCS) {
                TreeItem<String> csRoot = findItem(tree.getRoot(), "Color Scheme");
                if (csRoot != null) {
                    item = findItem(csRoot, categoryName);
                }
            } else if (underCodeStyle) {
                TreeItem<String> csRoot = findItem(tree.getRoot(), "Code Style");
                if (csRoot != null) {
                    item = findItem(csRoot, categoryName);
                }
            } else if (underSmartKeys) {
                TreeItem<String> skRoot = findItem(tree.getRoot(), "Smart Keys");
                if (skRoot != null) {
                    item = findItem(skRoot, categoryName);
                }
            } else if (underBuild) {
                TreeItem<String> bRoot = findItem(tree.getRoot(), "Build, Execution, Deployment");
                if (bRoot != null) {
                    item = findItem(bRoot, categoryName);
                }
            } else if (underLanguages) {
                TreeItem<String> langRoot = findItem(tree.getRoot(), "Languages & Frameworks");
                if (langRoot != null) {
                    item = findItem(langRoot, categoryName);
                }
            }
        }
        if (item == null) {
            if ("PHP".equals(categoryName)) {
                TreeItem<String> langRoot = findItem(tree.getRoot(), "Languages & Frameworks");
                if (langRoot != null) {
                    item = findItem(langRoot, "PHP");
                }
            }
        }
        if (item == null) {
            item = findItem(tree.getRoot(), categoryName);
        }
        if (item != null) {
            expandAncestors(item);
            tree.getSelectionModel().select(item);
        }
    }

    public void selectCategory(String parentCategory, String categoryName) {
        if (categoryName == null || tree == null) return;
        if (parentCategory == null) {
            selectCategory(categoryName);
            return;
        }
        TreeItem<String> parentItem = findItem(tree.getRoot(), parentCategory);
        if (parentItem != null) {
            TreeItem<String> item = findItem(parentItem, categoryName);
            if (item != null) {
                expandAncestors(item);
                tree.getSelectionModel().select(item);
                return;
            }
        }
        selectCategory(categoryName);
    }

    private void expandAncestors(TreeItem<String> item) {
        TreeItem<String> p = item.getParent();
        while (p != null) {
            p.setExpanded(true);
            p = p.getParent();
        }
    }

    public void applyAll() {
        if (currentIdeAppearancePage != null) {
            currentIdeAppearancePage.save();
        }
        if (currentSystemPage != null) {
            currentSystemPage.save();
        }
        if (currentEditorGeneralPage != null && currentEditorGeneralPage.isModified()) {
            currentEditorGeneralPage.apply();
        }
        if (currentAutoImportPage != null && currentAutoImportPage.isModified()) {
            currentAutoImportPage.apply();
        }
        if (currentEditorAppearancePage != null && currentEditorAppearancePage.isModified()) {
            currentEditorAppearancePage.apply();
        }
        if (currentBreadcrumbsPage != null && currentBreadcrumbsPage.isModified()) {
            currentBreadcrumbsPage.apply();
        }
        if (currentCodeCompletionPage != null && currentCodeCompletionPage.isModified()) {
            currentCodeCompletionPage.apply();
        }
        if (currentCodeFoldingPage != null && currentCodeFoldingPage.isModified()) {
            currentCodeFoldingPage.apply();
        }
        if (currentConsolePage != null && currentConsolePage.isModified()) {
            currentConsolePage.apply();
        }
        if (currentEditorTabsPage != null && currentEditorTabsPage.isModified()) {
            currentEditorTabsPage.apply();
        }
        if (currentGutterIconsPage != null && currentGutterIconsPage.isModified()) {
            currentGutterIconsPage.apply();
        }
        if (currentInlineCompletionPage != null && currentInlineCompletionPage.isModified()) {
            currentInlineCompletionPage.apply();
        }
        if (currentPostfixCompletionPage != null && currentPostfixCompletionPage.isModified()) {
            currentPostfixCompletionPage.apply();
        }
        if (currentSmartKeysPage != null && currentSmartKeysPage.isModified()) {
            currentSmartKeysPage.apply();
        }
        if (currentSmartKeysYamlPage != null && currentSmartKeysYamlPage.isModified()) {
            currentSmartKeysYamlPage.apply();
        }
        if (currentSmartKeysHtmlCssPage != null && currentSmartKeysHtmlCssPage.isModified()) {
            currentSmartKeysHtmlCssPage.apply();
        }
        if (currentSmartKeysPythonPage != null && currentSmartKeysPythonPage.isModified()) {
            currentSmartKeysPythonPage.apply();
        }
        if (currentSmartKeysJsonPage != null && currentSmartKeysJsonPage.isModified()) {
            currentSmartKeysJsonPage.apply();
        }
        if (currentSmartKeysRustPage != null && currentSmartKeysRustPage.isModified()) {
            currentSmartKeysRustPage.apply();
        }
        if (currentSmartKeysMarkdownPage != null && currentSmartKeysMarkdownPage.isModified()) {
            currentSmartKeysMarkdownPage.apply();
        }
        if (currentSmartKeysScalaPage != null && currentSmartKeysScalaPage.isModified()) {
            currentSmartKeysScalaPage.apply();
        }
        if (currentSmartKeysSqlPage != null && currentSmartKeysSqlPage.isModified()) {
            currentSmartKeysSqlPage.apply();
        }
        if (currentSmartKeysRubyPage != null && currentSmartKeysRubyPage.isModified()) {
            currentSmartKeysRubyPage.apply();
        }
        if (currentSmartKeysJsPage != null && currentSmartKeysJsPage.isModified()) {
            currentSmartKeysJsPage.apply();
        }
        if (currentSmartKeysPhpPage != null && currentSmartKeysPhpPage.isModified()) {
            currentSmartKeysPhpPage.apply();
        }
        if (currentStickyLinesPage != null && currentStickyLinesPage.isModified()) {
            currentStickyLinesPage.apply();
        }
        if (currentCodeEditingPage != null && currentCodeEditingPage.isModified()) {
            currentCodeEditingPage.apply();
        }
        if (currentInspectionsPage != null && currentInspectionsPage.isModified()) {
            currentInspectionsPage.apply();
        }
        if (currentFileAndCodeTemplatesPage != null && currentFileAndCodeTemplatesPage.isModified()) {
            currentFileAndCodeTemplatesPage.apply();
        }
        if (currentFileEncodingsPage != null && currentFileEncodingsPage.isModified()) {
            currentFileEncodingsPage.apply();
        }
        if (currentLiveTemplatesPage != null && currentLiveTemplatesPage.isModified()) {
            currentLiveTemplatesPage.apply();
        }
        if (currentFileTypesPage != null && currentFileTypesPage.isModified()) {
            currentFileTypesPage.apply();
        }
        if (currentCopyrightPage != null && currentCopyrightPage.isModified()) {
            currentCopyrightPage.apply();
        }
        if (currentCopyrightProfilesPage != null && currentCopyrightProfilesPage.isModified()) {
            currentCopyrightProfilesPage.apply();
        }
        if (currentCopyrightFormattingPage != null && currentCopyrightFormattingPage.isModified()) {
            currentCopyrightFormattingPage.apply();
        }
        for (SettingsCopyrightFormattingLanguagePage lp : languageFormattingPages.values()) {
            if (lp.isModified()) {
                lp.apply();
            }
        }
        if (currentInlayHintsPage != null && currentInlayHintsPage.isModified()) {
            currentInlayHintsPage.apply();
        }
        if (currentDuplicatesPage != null && currentDuplicatesPage.isModified()) {
            currentDuplicatesPage.apply();
        }
        if (currentEmmetPage != null && currentEmmetPage.isModified()) {
            currentEmmetPage.apply();
        }
        if (currentEmmetCssPage != null && currentEmmetCssPage.isModified()) {
            currentEmmetCssPage.apply();
        }
        if (currentEmmetHtmlPage != null && currentEmmetHtmlPage.isModified()) {
            currentEmmetHtmlPage.apply();
        }
        if (currentEmmetJsxPage != null && currentEmmetJsxPage.isModified()) {
            currentEmmetJsxPage.apply();
        }
        if (currentIntentionsPage != null && currentIntentionsPage.isModified()) {
            currentIntentionsPage.apply();
        }
        if (currentLanguageInjectionsPage != null && currentLanguageInjectionsPage.isModified()) {
            currentLanguageInjectionsPage.apply();
        }
        if (currentLanguageInjectionsAdvancedPage != null && currentLanguageInjectionsAdvancedPage.isModified()) {
            currentLanguageInjectionsAdvancedPage.apply();
        }
        if (currentNaturalLanguagesPage != null && currentNaturalLanguagesPage.isModified()) {
            currentNaturalLanguagesPage.apply();
        }
        if (currentGrammarAndStylePage != null && currentGrammarAndStylePage.isModified()) {
            currentGrammarAndStylePage.apply();
        }
        if (currentSpellingPage != null && currentSpellingPage.isModified()) {
            currentSpellingPage.apply();
        }
        if (currentReaderModePage != null && currentReaderModePage.isModified()) {
            currentReaderModePage.apply();
        }
        if (currentTextMateBundlesPage != null && currentTextMateBundlesPage.isModified()) {
            currentTextMateBundlesPage.apply();
        }
        if (currentTodoPage != null && currentTodoPage.isModified()) {
            currentTodoPage.apply();
        }
        if (currentPythonDebuggerPage != null && currentPythonDebuggerPage.isModified()) {
            currentPythonDebuggerPage.apply();
        }
        if (currentDebuggerPage != null && currentDebuggerPage.isModified()) {
            currentDebuggerPage.apply();
        }
        if (currentDebuggerAsyncStackTracesPage != null && currentDebuggerAsyncStackTracesPage.isModified()) {
            currentDebuggerAsyncStackTracesPage.apply();
        }
        if (currentDebuggerDataViewsPage != null && currentDebuggerDataViewsPage.isModified()) {
            currentDebuggerDataViewsPage.apply();
        }
        if (currentDebuggerDataViewsJavaPage != null && currentDebuggerDataViewsJavaPage.isModified()) {
            currentDebuggerDataViewsJavaPage.apply();
        }
        if (currentDebuggerDataViewsTypeRenderersPage != null && currentDebuggerDataViewsTypeRenderersPage.isModified()) {
            currentDebuggerDataViewsTypeRenderersPage.apply();
        }
        if (currentDebuggerHotSwapPage != null && currentDebuggerHotSwapPage.isModified()) {
            currentDebuggerHotSwapPage.apply();
        }
        if (currentDebuggerSteppingPage != null && currentDebuggerSteppingPage.isModified()) {
            currentDebuggerSteppingPage.apply();
        }
        if (currentDebuggerDataViewsJavaScriptPage != null && currentDebuggerDataViewsJavaScriptPage.isModified()) {
            currentDebuggerDataViewsJavaScriptPage.apply();
        }
        if (currentDeploymentPage != null && currentDeploymentPage.isModified()) {
            currentDeploymentPage.apply();
        }
        if (currentDeploymentOptionsPage != null && currentDeploymentOptionsPage.isModified()) {
            currentDeploymentOptionsPage.apply();
        }
        if (currentDockerPage != null && currentDockerPage.isModified()) {
            currentDockerPage.apply();
        }
        if (currentDockerConsolePage != null && currentDockerConsolePage.isModified()) {
            currentDockerConsolePage.apply();
        }
        if (currentDockerRegistryPage != null && currentDockerRegistryPage.isModified()) {
            currentDockerRegistryPage.apply();
        }
        if (currentJavaProfilerPage != null && currentJavaProfilerPage.isModified()) {
            currentJavaProfilerPage.apply();
        }
        if (currentJavaProfilerFiltersPage != null && currentJavaProfilerFiltersPage.isModified()) {
            currentJavaProfilerFiltersPage.apply();
        }
        if (currentBuildKubernetesPage != null && currentBuildKubernetesPage.isModified()) {
            currentBuildKubernetesPage.apply();
        }
        if (currentRemoteJarRepositoriesPage != null && currentRemoteJarRepositoriesPage.isModified()) {
            currentRemoteJarRepositoriesPage.apply();
        }
        if (currentRunTargetsPage != null && currentRunTargetsPage.isModified()) {
            currentRunTargetsPage.apply();
        }
        if (currentLanguagesPhpPage != null && currentLanguagesPhpPage.isModified()) {
            currentLanguagesPhpPage.apply();
        }
        if (currentLanguagesPhpDebugPage != null && currentLanguagesPhpDebugPage.isModified()) {
            currentLanguagesPhpDebugPage.apply();
        }
        if (currentLanguagesPhpDebugDbgpProxyPage != null && currentLanguagesPhpDebugDbgpProxyPage.isModified()) {
            currentLanguagesPhpDebugDbgpProxyPage.apply();
        }
        if (currentLanguagesPhpDebugSkippedPathsPage != null && currentLanguagesPhpDebugSkippedPathsPage.isModified()) {
            currentLanguagesPhpDebugSkippedPathsPage.apply();
        }
        if (currentLanguagesPhpDebugStepFiltersPage != null && currentLanguagesPhpDebugStepFiltersPage.isModified()) {
            currentLanguagesPhpDebugStepFiltersPage.apply();
        }
        if (currentLanguagesPhpDebugXdebugCloudPage != null && currentLanguagesPhpDebugXdebugCloudPage.isModified()) {
            currentLanguagesPhpDebugXdebugCloudPage.apply();
        }
        if (currentLanguagesPhpServersPage != null && currentLanguagesPhpServersPage.isModified()) {
            currentLanguagesPhpServersPage.apply();
        }
        if (currentLanguagesPhpComposerPage != null && currentLanguagesPhpComposerPage.isModified()) {
            currentLanguagesPhpComposerPage.apply();
        }
        if (currentLanguagesPhpTestFrameworksPage != null && currentLanguagesPhpTestFrameworksPage.isModified()) {
            currentLanguagesPhpTestFrameworksPage.apply();
        }
        if (currentPhpQualityToolsPage != null && currentPhpQualityToolsPage.isModified()) {
            currentPhpQualityToolsPage.apply();
        }
        if (currentPhpCodeSnifferPage != null && currentPhpCodeSnifferPage.isModified()) {
            currentPhpCodeSnifferPage.apply();
        }
        if (currentPhpCsFixerPage != null && currentPhpCsFixerPage.isModified()) {
            currentPhpCsFixerPage.apply();
        }
        if (currentPhpLaravelPintPage != null && currentPhpLaravelPintPage.isModified()) {
            currentPhpLaravelPintPage.apply();
        }
        if (currentPhpMessDetectorPage != null && currentPhpMessDetectorPage.isModified()) {
            currentPhpMessDetectorPage.apply();
        }
        if (currentPhpFrameworksPage != null && currentPhpFrameworksPage.isModified()) {
            currentPhpFrameworksPage.apply();
        }
        if (currentPhpSmartyPage != null && currentPhpSmartyPage.isModified()) {
            currentPhpSmartyPage.apply();
        }
        if (currentPythonTemplateLanguagesPage != null && currentPythonTemplateLanguagesPage.isModified()) {
            currentPythonTemplateLanguagesPage.apply();
        }
        if (currentLanguagesGoPage != null && currentLanguagesGoPage.isModified()) {
            currentLanguagesGoPage.apply();
        }
        if (currentLanguagesGoGoRootPage != null && currentLanguagesGoGoRootPage.isModified()) {
            currentLanguagesGoGoRootPage.apply();
        }
        if (currentLanguagesGoGoPathPage != null && currentLanguagesGoGoPathPage.isModified()) {
            currentLanguagesGoGoPathPage.apply();
        }
        if (currentLanguagesGoModulesPage != null && currentLanguagesGoModulesPage.isModified()) {
            currentLanguagesGoModulesPage.apply();
        }
        if (currentLanguagesGoBuildTagsPage != null && currentLanguagesGoBuildTagsPage.isModified()) {
            currentLanguagesGoBuildTagsPage.apply();
        }
        if (currentLanguagesGoFormattingFunctionsPage != null && currentLanguagesGoFormattingFunctionsPage.isModified()) {
            currentLanguagesGoFormattingFunctionsPage.apply();
        }
        if (currentLanguagesGoImportsPage != null && currentLanguagesGoImportsPage.isModified()) {
            currentLanguagesGoImportsPage.apply();
        }
        if (currentRustPage != null && currentRustPage.isModified()) {
            currentRustPage.apply();
        }
        if (currentRustExternalLintersPage != null && currentRustExternalLintersPage.isModified()) {
            currentRustExternalLintersPage.apply();
        }
        if (currentRustfmtPage != null && currentRustfmtPage.isModified()) {
            currentRustfmtPage.apply();
        }
        if (currentLanguagesJavaFxPage != null && currentLanguagesJavaFxPage.isModified()) {
            currentLanguagesJavaFxPage.apply();
        }
        if (currentLanguagesJavaScriptPage != null && currentLanguagesJavaScriptPage.isModified()) {
            currentLanguagesJavaScriptPage.apply();
        }
        if (currentLanguagesJsEsLintPage != null && currentLanguagesJsEsLintPage.isModified()) {
            currentLanguagesJsEsLintPage.apply();
        }
        if (currentLanguagesJsJsHintPage != null && currentLanguagesJsJsHintPage.isModified()) {
            currentLanguagesJsJsHintPage.apply();
        }
        if (currentLanguagesJsLibrariesPage != null && currentLanguagesJsLibrariesPage.isModified()) {
            currentLanguagesJsLibrariesPage.apply();
        }
        if (currentLanguagesJsPrettierPage != null && currentLanguagesJsPrettierPage.isModified()) {
            currentLanguagesJsPrettierPage.apply();
        }
        if (currentLanguagesJsStyledComponentsPage != null && currentLanguagesJsStyledComponentsPage.isModified()) {
            currentLanguagesJsStyledComponentsPage.apply();
        }
        if (currentLanguagesJsVitePage != null && currentLanguagesJsVitePage.isModified()) {
            currentLanguagesJsVitePage.apply();
        }
        if (currentLanguagesJsWebpackPage != null && currentLanguagesJsWebpackPage.isModified()) {
            currentLanguagesJsWebpackPage.apply();
        }
        if (currentLanguagesJavaScriptRuntimePage != null && currentLanguagesJavaScriptRuntimePage.isModified()) {
            currentLanguagesJavaScriptRuntimePage.apply();
        }
        if (currentLanguagesJvmLoggingPage != null && currentLanguagesJvmLoggingPage.isModified()) {
            currentLanguagesJvmLoggingPage.apply();
        }
        if (currentLanguagesKotlinScriptingPage != null && currentLanguagesKotlinScriptingPage.isModified()) {
            currentLanguagesKotlinScriptingPage.apply();
        }
        if (currentLanguagesKtorPage != null && currentLanguagesKtorPage.isModified()) {
            currentLanguagesKtorPage.apply();
        }
        if (currentLanguagesKubernetesPage != null && currentLanguagesKubernetesPage.isModified()) {
            currentLanguagesKubernetesPage.apply();
        }
        if (currentLanguagesLombokPage != null && currentLanguagesLombokPage.isModified()) {
            currentLanguagesLombokPage.apply();
        }
        if (currentLanguagesMarkdownPage != null && currentLanguagesMarkdownPage.isModified()) {
            currentLanguagesMarkdownPage.apply();
        }
        if (currentLanguagesMicronautPage != null && currentLanguagesMicronautPage.isModified()) {
            currentLanguagesMicronautPage.apply();
        }
        if (currentLanguagesOpenApiPage != null && currentLanguagesOpenApiPage.isModified()) {
            currentLanguagesOpenApiPage.apply();
        }
        if (currentLanguagesPlayPage != null && currentLanguagesPlayPage.isModified()) {
            currentLanguagesPlayPage.apply();
        }
        if (currentLanguagesProtobufPage != null && currentLanguagesProtobufPage.isModified()) {
            currentLanguagesProtobufPage.apply();
        }
        if (currentLanguagesProtobufTextFormatPage != null && currentLanguagesProtobufTextFormatPage.isModified()) {
            currentLanguagesProtobufTextFormatPage.apply();
        }
        if (currentLanguagesQuarkusPage != null && currentLanguagesQuarkusPage.isModified()) {
            currentLanguagesQuarkusPage.apply();
        }
        if (currentLanguagesRbsPage != null && currentLanguagesRbsPage.isModified()) {
            currentLanguagesRbsPage.apply();
        }
        if (currentLanguagesScalaEditorPage != null && currentLanguagesScalaEditorPage.isModified()) {
            currentLanguagesScalaEditorPage.apply();
        }
        if (currentLanguagesScalaXRayPage != null && currentLanguagesScalaXRayPage.isModified()) {
            currentLanguagesScalaXRayPage.apply();
        }
        if (currentLanguagesScalaProjectViewPage != null && currentLanguagesScalaProjectViewPage.isModified()) {
            currentLanguagesScalaProjectViewPage.apply();
        }
        if (currentLanguagesScalaPerformancePage != null && currentLanguagesScalaPerformancePage.isModified()) {
            currentLanguagesScalaPerformancePage.apply();
        }
        if (currentLanguagesScalaWorksheetPage != null && currentLanguagesScalaWorksheetPage.isModified()) {
            currentLanguagesScalaWorksheetPage.apply();
        }
        if (currentLanguagesScalaBasePackagePage != null && currentLanguagesScalaBasePackagePage.isModified()) {
            currentLanguagesScalaBasePackagePage.apply();
        }
        if (currentLanguagesScalaMiscPage != null && currentLanguagesScalaMiscPage.isModified()) {
            currentLanguagesScalaMiscPage.apply();
        }
        if (currentLanguagesScalaUpdatesPage != null && currentLanguagesScalaUpdatesPage.isModified()) {
            currentLanguagesScalaUpdatesPage.apply();
        }
        if (currentLanguagesScalaExtensionsPage != null && currentLanguagesScalaExtensionsPage.isModified()) {
            currentLanguagesScalaExtensionsPage.apply();
        }
        if (currentLanguagesSchemasAndDtdsPage != null && currentLanguagesSchemasAndDtdsPage.isModified()) {
            currentLanguagesSchemasAndDtdsPage.apply();
        }
        if (currentLanguagesDefaultXmlSchemasPage != null && currentLanguagesDefaultXmlSchemasPage.isModified()) {
            currentLanguagesDefaultXmlSchemasPage.apply();
        }
        if (currentLanguagesJsonSchemaMappingsPage != null && currentLanguagesJsonSchemaMappingsPage.isModified()) {
            currentLanguagesJsonSchemaMappingsPage.apply();
        }
        if (currentLanguagesRemoteJsonSchemasPage != null && currentLanguagesRemoteJsonSchemasPage.isModified()) {
            currentLanguagesRemoteJsonSchemasPage.apply();
        }
        if (currentLanguagesXmlCatalogPage != null && currentLanguagesXmlCatalogPage.isModified()) {
            currentLanguagesXmlCatalogPage.apply();
        }
        if (currentLanguagesSpringPage != null && currentLanguagesSpringPage.isModified()) {
            currentLanguagesSpringPage.apply();
        }
        if (currentLanguagesSqlDialectsPage != null && currentLanguagesSqlDialectsPage.isModified()) {
            currentLanguagesSqlDialectsPage.apply();
        }
        if (currentLanguagesSqlResolutionScopesPage != null && currentLanguagesSqlResolutionScopesPage.isModified()) {
            currentLanguagesSqlResolutionScopesPage.apply();
        }
        if (currentLanguagesStyleSheetsDialectsPage != null && currentLanguagesStyleSheetsDialectsPage.isModified()) {
            currentLanguagesStyleSheetsDialectsPage.apply();
        }
        if (currentLanguagesStyleSheetsStylelintPage != null && currentLanguagesStyleSheetsStylelintPage.isModified()) {
            currentLanguagesStyleSheetsStylelintPage.apply();
        }
        if (currentLanguagesStyleSheetsTailwindPage != null && currentLanguagesStyleSheetsTailwindPage.isModified()) {
            currentLanguagesStyleSheetsTailwindPage.apply();
        }
        if (currentLanguagesTablesPage != null && currentLanguagesTablesPage.isModified()) {
            currentLanguagesTablesPage.apply();
        }
        if (currentLanguagesTemplateDataLanguagesPage != null && currentLanguagesTemplateDataLanguagesPage.isModified()) {
            currentLanguagesTemplateDataLanguagesPage.apply();
        }
        if (currentLanguagesTypeScriptPage != null && currentLanguagesTypeScriptPage.isModified()) {
            currentLanguagesTypeScriptPage.apply();
        }
        if (currentLanguagesTypeScriptAngularPage != null && currentLanguagesTypeScriptAngularPage.isModified()) {
            currentLanguagesTypeScriptAngularPage.apply();
        }
        if (currentLanguagesTypeScriptTsLintPage != null && currentLanguagesTypeScriptTsLintPage.isModified()) {
            currentLanguagesTypeScriptTsLintPage.apply();
        }
        if (currentLanguagesTypeScriptVuePage != null && currentLanguagesTypeScriptVuePage.isModified()) {
            currentLanguagesTypeScriptVuePage.apply();
        }
        if (currentLanguagesWebContextsPage != null && currentLanguagesWebContextsPage.isModified()) {
            currentLanguagesWebContextsPage.apply();
        }
        if (currentLanguagesXsltPage != null && currentLanguagesXsltPage.isModified()) {
            currentLanguagesXsltPage.apply();
        }
        if (currentLanguagesXsltFileAssociationsPage != null && currentLanguagesXsltFileAssociationsPage.isModified()) {
            currentLanguagesXsltFileAssociationsPage.apply();
        }
        if (currentApplicationServersPage != null && currentApplicationServersPage.isModified()) {
            currentApplicationServersPage.apply();
        }
        if (currentBuildToolsPage != null && currentBuildToolsPage.isModified()) {
            currentBuildToolsPage.apply();
        }
        if (currentMavenPage != null && currentMavenPage.isModified()) {
            currentMavenPage.apply();
        }
        if (currentMavenArchetypeCatalogsPage != null && currentMavenArchetypeCatalogsPage.isModified()) {
            currentMavenArchetypeCatalogsPage.apply();
        }
        if (currentMavenIgnoredFilesPage != null && currentMavenIgnoredFilesPage.isModified()) {
            currentMavenIgnoredFilesPage.apply();
        }
        if (currentMavenImportingPage != null && currentMavenImportingPage.isModified()) {
            currentMavenImportingPage.apply();
        }
        if (currentMavenRepositoriesPage != null && currentMavenRepositoriesPage.isModified()) {
            currentMavenRepositoriesPage.apply();
        }
        if (currentMavenRunnerPage != null && currentMavenRunnerPage.isModified()) {
            currentMavenRunnerPage.apply();
        }
        if (currentMavenRunningTestsPage != null && currentMavenRunningTestsPage.isModified()) {
            currentMavenRunningTestsPage.apply();
        }
        if (currentGradlePage != null && currentGradlePage.isModified()) {
            currentGradlePage.apply();
        }
        if (currentGantPage != null && currentGantPage.isModified()) {
            currentGantPage.apply();
        }
        if (currentBspPage != null && currentBspPage.isModified()) {
            currentBspPage.apply();
        }
        if (currentCargoPage != null && currentCargoPage.isModified()) {
            currentCargoPage.apply();
        }
        if (currentSbtPage != null && currentSbtPage.isModified()) {
            currentSbtPage.apply();
        }
        if (currentCompilerPage != null && currentCompilerPage.isModified()) {
            currentCompilerPage.apply();
        }
        if (currentAnnotationProcessorsPage != null && currentAnnotationProcessorsPage.isModified()) {
            currentAnnotationProcessorsPage.apply();
        }
        if (currentCompilerExcludesPage != null && currentCompilerExcludesPage.isModified()) {
            currentCompilerExcludesPage.apply();
        }
        if (currentGroovyCompilerPage != null && currentGroovyCompilerPage.isModified()) {
            currentGroovyCompilerPage.apply();
        }
        if (currentJavaCompilerPage != null && currentJavaCompilerPage.isModified()) {
            currentJavaCompilerPage.apply();
        }
        if (currentKotlinCompilerPage != null && currentKotlinCompilerPage.isModified()) {
            currentKotlinCompilerPage.apply();
        }
        if (currentRmiCompilerPage != null && currentRmiCompilerPage.isModified()) {
            currentRmiCompilerPage.apply();
        }
        if (currentScalaCompilerPage != null && currentScalaCompilerPage.isModified()) {
            currentScalaCompilerPage.apply();
        }
        if (currentScalaBytecodeIndicesPage != null && currentScalaBytecodeIndicesPage.isModified()) {
            currentScalaBytecodeIndicesPage.apply();
        }
        if (currentScalaCompileServerPage != null && currentScalaCompileServerPage.isModified()) {
            currentScalaCompileServerPage.apply();
        }
        if (currentValidationPage != null && currentValidationPage.isModified()) {
            currentValidationPage.apply();
        }
        if (currentBuildConsolePage != null && currentBuildConsolePage.isModified()) {
            currentBuildConsolePage.apply();
        }
        if (currentPythonConsolePage != null && currentPythonConsolePage.isModified()) {
            currentPythonConsolePage.apply();
        }
        if (currentCoveragePage != null && currentCoveragePage.isModified()) {
            currentCoveragePage.apply();
        }
        if (currentFontPage != null && currentFontPage.isModified()) {
            currentFontPage.apply();
        }
        if (currentColorSchemePage != null && currentColorSchemePage.isModified()) {
            currentColorSchemePage.apply();
        }
        if (currentColorSchemeGeneralPage != null && currentColorSchemeGeneralPage.isModified()) {
            currentColorSchemeGeneralPage.apply();
        }
        if (currentColorSchemeLanguageDefaultsPage != null && currentColorSchemeLanguageDefaultsPage.isModified()) {
            currentColorSchemeLanguageDefaultsPage.apply();
        }
        if (currentColorSchemeFontPage != null && currentColorSchemeFontPage.isModified()) {
            currentColorSchemeFontPage.apply();
        }
        if (currentConsoleFontPage != null && currentConsoleFontPage.isModified()) {
            currentConsoleFontPage.apply();
        }
        if (currentConsoleColorsPage != null && currentConsoleColorsPage.isModified()) {
            currentConsoleColorsPage.apply();
        }
        if (currentCodeWithMePage != null && currentCodeWithMePage.isModified()) {
            currentCodeWithMePage.apply();
        }
        if (currentColorSchemeDebuggerPage != null && currentColorSchemeDebuggerPage.isModified()) {
            currentColorSchemeDebuggerPage.apply();
        }
        if (currentColorSchemeDiffMergePage != null && currentColorSchemeDiffMergePage.isModified()) {
            currentColorSchemeDiffMergePage.apply();
        }
        if (currentColorSchemeJvmLoggingPage != null && currentColorSchemeJvmLoggingPage.isModified()) {
            currentColorSchemeJvmLoggingPage.apply();
        }
        if (currentColorSchemeUserDefinedFileTypesPage != null && currentColorSchemeUserDefinedFileTypesPage.isModified()) {
            currentColorSchemeUserDefinedFileTypesPage.apply();
        }
        if (currentColorSchemeVcsPage != null && currentColorSchemeVcsPage.isModified()) {
            currentColorSchemeVcsPage.apply();
        }
        if (currentColorSchemeJavaPage != null && currentColorSchemeJavaPage.isModified()) {
            currentColorSchemeJavaPage.apply();
        }
        if (currentColorSchemeAngularTemplatePage != null && currentColorSchemeAngularTemplatePage.isModified()) {
            currentColorSchemeAngularTemplatePage.apply();
        }
        if (currentColorSchemeContextFreeGrammarPage != null && currentColorSchemeContextFreeGrammarPage.isModified()) {
            currentColorSchemeContextFreeGrammarPage.apply();
        }
        if (currentColorSchemeCssPage != null && currentColorSchemeCssPage.isModified()) {
            currentColorSchemeCssPage.apply();
        }
        if (currentColorSchemeDataEditorViewerPage != null && currentColorSchemeDataEditorViewerPage.isModified()) {
            currentColorSchemeDataEditorViewerPage.apply();
        }
        if (currentColorSchemeDatabasePage != null && currentColorSchemeDatabasePage.isModified()) {
            currentColorSchemeDatabasePage.apply();
        }
        if (currentColorSchemeDiagramsPage != null && currentColorSchemeDiagramsPage.isModified()) {
            currentColorSchemeDiagramsPage.apply();
        }
        if (currentColorSchemeDockerfilePage != null && currentColorSchemeDockerfilePage.isModified()) {
            currentColorSchemeDockerfilePage.apply();
        }
        if (currentColorSchemeEditorConfigPage != null && currentColorSchemeEditorConfigPage.isModified()) {
            currentColorSchemeEditorConfigPage.apply();
        }
        if (currentColorSchemeErbPage != null && currentColorSchemeErbPage.isModified()) {
            currentColorSchemeErbPage.apply();
        }
        if (currentColorSchemeFreeMarkerPage != null && currentColorSchemeFreeMarkerPage.isModified()) {
            currentColorSchemeFreeMarkerPage.apply();
        }
        if (currentColorSchemeGitLabCiExpressionPage != null && currentColorSchemeGitLabCiExpressionPage.isModified()) {
            currentColorSchemeGitLabCiExpressionPage.apply();
        }
        if (currentColorSchemeGoPage != null && currentColorSchemeGoPage.isModified()) {
            currentColorSchemeGoPage.apply();
        }
        if (currentColorSchemeGradleDeclarativePage != null && currentColorSchemeGradleDeclarativePage.isModified()) {
            currentColorSchemeGradleDeclarativePage.apply();
        }
        if (currentColorSchemeGroovyPage != null && currentColorSchemeGroovyPage.isModified()) {
            currentColorSchemeGroovyPage.apply();
        }
        if (currentColorSchemeHtmlPage != null && currentColorSchemeHtmlPage.isModified()) {
            currentColorSchemeHtmlPage.apply();
        }
        if (currentColorSchemeHttpRequestPage != null && currentColorSchemeHttpRequestPage.isModified()) {
            currentColorSchemeHttpRequestPage.apply();
        }
        if (currentColorSchemeJavaScriptPage != null && currentColorSchemeJavaScriptPage.isModified()) {
            currentColorSchemeJavaScriptPage.apply();
        }
        if (currentColorSchemeJpaHibernateQlPage != null && currentColorSchemeJpaHibernateQlPage.isModified()) {
            currentColorSchemeJpaHibernateQlPage.apply();
        }
        if (currentColorSchemeJsonPage != null && currentColorSchemeJsonPage.isModified()) {
            currentColorSchemeJsonPage.apply();
        }
        if (currentColorSchemeJsonPathPage != null && currentColorSchemeJsonPathPage.isModified()) {
            currentColorSchemeJsonPathPage.apply();
        }
        if (currentColorSchemeJspPage != null && currentColorSchemeJspPage.isModified()) {
            currentColorSchemeJspPage.apply();
        }
        if (currentColorSchemeJupyterNotebooksPage != null && currentColorSchemeJupyterNotebooksPage.isModified()) {
            currentColorSchemeJupyterNotebooksPage.apply();
        }
        if (currentColorSchemeKotlinPage != null && currentColorSchemeKotlinPage.isModified()) {
            currentColorSchemeKotlinPage.apply();
        }
        if (currentColorSchemeKubernetesPage != null && currentColorSchemeKubernetesPage.isModified()) {
            currentColorSchemeKubernetesPage.apply();
        }
        if (currentColorSchemeLessPage != null && currentColorSchemeLessPage.isModified()) {
            currentColorSchemeLessPage.apply();
        }
        if (currentColorSchemeLombokConfigPage != null && currentColorSchemeLombokConfigPage.isModified()) {
            currentColorSchemeLombokConfigPage.apply();
        }
        if (currentColorSchemeMarkdownPage != null && currentColorSchemeMarkdownPage.isModified()) {
            currentColorSchemeMarkdownPage.apply();
        }
        if (currentColorSchemeMicronautELPage != null && currentColorSchemeMicronautELPage.isModified()) {
            currentColorSchemeMicronautELPage.apply();
        }
        if (currentColorSchemePhpPage != null && currentColorSchemePhpPage.isModified()) {
            currentColorSchemePhpPage.apply();
        }
        if (currentColorSchemePlan9X86Page != null && currentColorSchemePlan9X86Page.isModified()) {
            currentColorSchemePlan9X86Page.apply();
        }
        if (currentColorSchemePostCSSPage != null && currentColorSchemePostCSSPage.isModified()) {
            currentColorSchemePostCSSPage.apply();
        }
        if (currentColorSchemeProtocolBufferPage != null && currentColorSchemeProtocolBufferPage.isModified()) {
            currentColorSchemeProtocolBufferPage.apply();
        }
        if (currentColorSchemeMongoDbJsonPage != null && currentColorSchemeMongoDbJsonPage.isModified()) {
            currentColorSchemeMongoDbJsonPage.apply();
        }
        if (currentColorSchemePropertiesPage != null && currentColorSchemePropertiesPage.isModified()) {
            currentColorSchemePropertiesPage.apply();
        }
        if (currentColorSchemeProtocolBufferTextPage != null && currentColorSchemeProtocolBufferTextPage.isModified()) {
            currentColorSchemeProtocolBufferTextPage.apply();
        }
        if (currentColorSchemePythonPage != null && currentColorSchemePythonPage.isModified()) {
            currentColorSchemePythonPage.apply();
        }
        if (currentColorSchemeQutePage != null && currentColorSchemeQutePage.isModified()) {
            currentColorSchemeQutePage.apply();
        }
        if (currentColorSchemeRDocPage != null && currentColorSchemeRDocPage.isModified()) {
            currentColorSchemeRDocPage.apply();
        }
        if (currentColorSchemeRegExpPage != null && currentColorSchemeRegExpPage.isModified()) {
            currentColorSchemeRegExpPage.apply();
        }
        if (currentColorSchemeRubyPage != null && currentColorSchemeRubyPage.isModified()) {
            currentColorSchemeRubyPage.apply();
        }
        if (currentColorSchemeRustPage != null && currentColorSchemeRustPage.isModified()) {
            currentColorSchemeRustPage.apply();
        }
        if (currentColorSchemeSassPage != null && currentColorSchemeSassPage.isModified()) {
            currentColorSchemeSassPage.apply();
        }
        if (currentColorSchemeScalaPage != null && currentColorSchemeScalaPage.isModified()) {
            currentColorSchemeScalaPage.apply();
        }
        if (currentColorSchemeShellScriptPage != null && currentColorSchemeShellScriptPage.isModified()) {
            currentColorSchemeShellScriptPage.apply();
        }
        if (currentColorSchemeSmartyPage != null && currentColorSchemeSmartyPage.isModified()) {
            currentColorSchemeSmartyPage.apply();
        }
        if (currentColorSchemeSpringELPage != null && currentColorSchemeSpringELPage.isModified()) {
            currentColorSchemeSpringELPage.apply();
        }
        if (currentColorSchemeSQLPage != null && currentColorSchemeSQLPage.isModified()) {
            currentColorSchemeSQLPage.apply();
        }
        if (currentColorSchemeTableDiffPage != null && currentColorSchemeTableDiffPage.isModified()) {
            currentColorSchemeTableDiffPage.apply();
        }
        if (currentColorSchemeTomlPage != null && currentColorSchemeTomlPage.isModified()) {
            currentColorSchemeTomlPage.apply();
        }
        if (currentColorSchemeTypeScriptPage != null && currentColorSchemeTypeScriptPage.isModified()) {
            currentColorSchemeTypeScriptPage.apply();
        }
        if (currentColorSchemeVelocityPage != null && currentColorSchemeVelocityPage.isModified()) {
            currentColorSchemeVelocityPage.apply();
        }
        if (currentColorSchemeXmlPage != null && currentColorSchemeXmlPage.isModified()) {
            currentColorSchemeXmlPage.apply();
        }
        if (currentColorSchemeXPathPage != null && currentColorSchemeXPathPage.isModified()) {
            currentColorSchemeXPathPage.apply();
        }
        if (currentColorSchemeXsltPage != null && currentColorSchemeXsltPage.isModified()) {
            currentColorSchemeXsltPage.apply();
        }
        if (currentColorSchemeYamlPage != null && currentColorSchemeYamlPage.isModified()) {
            currentColorSchemeYamlPage.apply();
        }
        if (currentColorSchemeByScopePage != null && currentColorSchemeByScopePage.isModified()) {
            currentColorSchemeByScopePage.apply();
        }
        if (currentColorSchemeImagesPage != null && currentColorSchemeImagesPage.isModified()) {
            currentColorSchemeImagesPage.apply();
        }
        if (currentCodeStylePage != null && currentCodeStylePage.isModified()) {
            currentCodeStylePage.apply();
        }
        if (currentCodeStyleJavaPage != null && currentCodeStyleJavaPage.isModified()) {
            currentCodeStyleJavaPage.apply();
        }
        if (currentCodeStyleLanguagePage != null && currentCodeStyleLanguagePage.isModified()) {
            currentCodeStyleLanguagePage.apply();
        }
        updateApplyButtonState();
    }

    private void updateApplyButtonState() {
        if (applyButton == null) return;
        boolean modified = (currentEditorGeneralPage != null && currentEditorGeneralPage.isModified())
                || (currentAutoImportPage != null && currentAutoImportPage.isModified())
                || (currentEditorAppearancePage != null && currentEditorAppearancePage.isModified())
                || (currentBreadcrumbsPage != null && currentBreadcrumbsPage.isModified())
                || (currentCodeCompletionPage != null && currentCodeCompletionPage.isModified())
                || (currentCodeFoldingPage != null && currentCodeFoldingPage.isModified())
                || (currentConsolePage != null && currentConsolePage.isModified())
                || (currentEditorTabsPage != null && currentEditorTabsPage.isModified())
                || (currentGutterIconsPage != null && currentGutterIconsPage.isModified())
                || (currentInlineCompletionPage != null && currentInlineCompletionPage.isModified())
                || (currentPostfixCompletionPage != null && currentPostfixCompletionPage.isModified())
                || (currentSmartKeysPage != null && currentSmartKeysPage.isModified())
                || (currentSmartKeysYamlPage != null && currentSmartKeysYamlPage.isModified())
                || (currentSmartKeysHtmlCssPage != null && currentSmartKeysHtmlCssPage.isModified())
                || (currentSmartKeysPythonPage != null && currentSmartKeysPythonPage.isModified())
                || (currentSmartKeysJsonPage != null && currentSmartKeysJsonPage.isModified())
                || (currentSmartKeysRustPage != null && currentSmartKeysRustPage.isModified())
                || (currentSmartKeysMarkdownPage != null && currentSmartKeysMarkdownPage.isModified())
                || (currentSmartKeysScalaPage != null && currentSmartKeysScalaPage.isModified())
                || (currentSmartKeysSqlPage != null && currentSmartKeysSqlPage.isModified())
                || (currentSmartKeysRubyPage != null && currentSmartKeysRubyPage.isModified())
                || (currentSmartKeysJsPage != null && currentSmartKeysJsPage.isModified())
                || (currentSmartKeysPhpPage != null && currentSmartKeysPhpPage.isModified())
                || (currentStickyLinesPage != null && currentStickyLinesPage.isModified())
                || (currentCodeEditingPage != null && currentCodeEditingPage.isModified())
                || (currentInspectionsPage != null && currentInspectionsPage.isModified())
                || (currentFileAndCodeTemplatesPage != null && currentFileAndCodeTemplatesPage.isModified())
                || (currentFileEncodingsPage != null && currentFileEncodingsPage.isModified())
                || (currentLiveTemplatesPage != null && currentLiveTemplatesPage.isModified())
                || (currentFileTypesPage != null && currentFileTypesPage.isModified())
                || (currentCopyrightPage != null && currentCopyrightPage.isModified())
                || (currentCopyrightProfilesPage != null && currentCopyrightProfilesPage.isModified())
                || (currentCopyrightFormattingPage != null && currentCopyrightFormattingPage.isModified())
                || languageFormattingPages.values().stream().anyMatch(SettingsCopyrightFormattingLanguagePage::isModified)
                || (currentInlayHintsPage != null && currentInlayHintsPage.isModified())
                || (currentDuplicatesPage != null && currentDuplicatesPage.isModified())
                || (currentEmmetPage != null && currentEmmetPage.isModified())
                || (currentEmmetCssPage != null && currentEmmetCssPage.isModified())
                || (currentEmmetHtmlPage != null && currentEmmetHtmlPage.isModified())
                || (currentEmmetJsxPage != null && currentEmmetJsxPage.isModified())
                || (currentIntentionsPage != null && currentIntentionsPage.isModified())
                || (currentLanguageInjectionsPage != null && currentLanguageInjectionsPage.isModified())
                || (currentLanguageInjectionsAdvancedPage != null && currentLanguageInjectionsAdvancedPage.isModified())
                || (currentNaturalLanguagesPage != null && currentNaturalLanguagesPage.isModified())
                || (currentGrammarAndStylePage != null && currentGrammarAndStylePage.isModified())
                || (currentSpellingPage != null && currentSpellingPage.isModified())
                || (currentReaderModePage != null && currentReaderModePage.isModified())
                || (currentTextMateBundlesPage != null && currentTextMateBundlesPage.isModified())
                || (currentTodoPage != null && currentTodoPage.isModified())
                || (currentPythonDebuggerPage != null && currentPythonDebuggerPage.isModified())
                || (currentDebuggerPage != null && currentDebuggerPage.isModified())
                || (currentDebuggerAsyncStackTracesPage != null && currentDebuggerAsyncStackTracesPage.isModified())
                || (currentDebuggerDataViewsPage != null && currentDebuggerDataViewsPage.isModified())
                || (currentDebuggerDataViewsJavaPage != null && currentDebuggerDataViewsJavaPage.isModified())
                || (currentDebuggerDataViewsTypeRenderersPage != null && currentDebuggerDataViewsTypeRenderersPage.isModified())
                || (currentDebuggerHotSwapPage != null && currentDebuggerHotSwapPage.isModified())
                || (currentDebuggerSteppingPage != null && currentDebuggerSteppingPage.isModified())
                || (currentDebuggerDataViewsJavaScriptPage != null && currentDebuggerDataViewsJavaScriptPage.isModified())
                || (currentDeploymentPage != null && currentDeploymentPage.isModified())
                || (currentDeploymentOptionsPage != null && currentDeploymentOptionsPage.isModified())
                || (currentDockerPage != null && currentDockerPage.isModified())
                || (currentDockerConsolePage != null && currentDockerConsolePage.isModified())
                || (currentDockerRegistryPage != null && currentDockerRegistryPage.isModified())
                || (currentJavaProfilerPage != null && currentJavaProfilerPage.isModified())
                || (currentJavaProfilerFiltersPage != null && currentJavaProfilerFiltersPage.isModified())
                || (currentBuildKubernetesPage != null && currentBuildKubernetesPage.isModified())
                || (currentRemoteJarRepositoriesPage != null && currentRemoteJarRepositoriesPage.isModified())
                || (currentRunTargetsPage != null && currentRunTargetsPage.isModified())
                || (currentLanguagesPhpPage != null && currentLanguagesPhpPage.isModified())
                || (currentLanguagesPhpDebugPage != null && currentLanguagesPhpDebugPage.isModified())
                || (currentLanguagesPhpDebugDbgpProxyPage != null && currentLanguagesPhpDebugDbgpProxyPage.isModified())
                || (currentLanguagesPhpDebugSkippedPathsPage != null && currentLanguagesPhpDebugSkippedPathsPage.isModified())
                || (currentLanguagesPhpDebugStepFiltersPage != null && currentLanguagesPhpDebugStepFiltersPage.isModified())
                || (currentLanguagesPhpDebugXdebugCloudPage != null && currentLanguagesPhpDebugXdebugCloudPage.isModified())
                || (currentLanguagesPhpServersPage != null && currentLanguagesPhpServersPage.isModified())
                || (currentLanguagesPhpComposerPage != null && currentLanguagesPhpComposerPage.isModified())
                || (currentLanguagesPhpTestFrameworksPage != null && currentLanguagesPhpTestFrameworksPage.isModified())
                || (currentPhpQualityToolsPage != null && currentPhpQualityToolsPage.isModified())
                || (currentPhpCodeSnifferPage != null && currentPhpCodeSnifferPage.isModified())
                || (currentPhpCsFixerPage != null && currentPhpCsFixerPage.isModified())
                || (currentPhpLaravelPintPage != null && currentPhpLaravelPintPage.isModified())
                || (currentPhpMessDetectorPage != null && currentPhpMessDetectorPage.isModified())
                || (currentPhpFrameworksPage != null && currentPhpFrameworksPage.isModified())
                || (currentPhpSmartyPage != null && currentPhpSmartyPage.isModified())
                || (currentPythonTemplateLanguagesPage != null && currentPythonTemplateLanguagesPage.isModified())
                || (currentLanguagesGoPage != null && currentLanguagesGoPage.isModified())
                || (currentLanguagesGoGoRootPage != null && currentLanguagesGoGoRootPage.isModified())
                || (currentLanguagesGoGoPathPage != null && currentLanguagesGoGoPathPage.isModified())
                || (currentLanguagesGoModulesPage != null && currentLanguagesGoModulesPage.isModified())
                || (currentLanguagesGoBuildTagsPage != null && currentLanguagesGoBuildTagsPage.isModified())
                || (currentLanguagesGoFormattingFunctionsPage != null && currentLanguagesGoFormattingFunctionsPage.isModified())
                || (currentLanguagesGoImportsPage != null && currentLanguagesGoImportsPage.isModified())
                || (currentRustPage != null && currentRustPage.isModified())
                || (currentRustExternalLintersPage != null && currentRustExternalLintersPage.isModified())
                || (currentRustfmtPage != null && currentRustfmtPage.isModified())
                || (currentLanguagesJavaFxPage != null && currentLanguagesJavaFxPage.isModified())
                || (currentLanguagesJavaScriptPage != null && currentLanguagesJavaScriptPage.isModified())
                || (currentLanguagesJsEsLintPage != null && currentLanguagesJsEsLintPage.isModified())
                || (currentLanguagesJsJsHintPage != null && currentLanguagesJsJsHintPage.isModified())
                || (currentLanguagesJsLibrariesPage != null && currentLanguagesJsLibrariesPage.isModified())
                || (currentLanguagesJsPrettierPage != null && currentLanguagesJsPrettierPage.isModified())
                || (currentLanguagesJsStyledComponentsPage != null && currentLanguagesJsStyledComponentsPage.isModified())
                || (currentLanguagesJsVitePage != null && currentLanguagesJsVitePage.isModified())
                || (currentLanguagesJsWebpackPage != null && currentLanguagesJsWebpackPage.isModified())
                || (currentLanguagesJavaScriptRuntimePage != null && currentLanguagesJavaScriptRuntimePage.isModified())
                || (currentLanguagesJvmLoggingPage != null && currentLanguagesJvmLoggingPage.isModified())
                || (currentLanguagesKotlinScriptingPage != null && currentLanguagesKotlinScriptingPage.isModified())
                || (currentLanguagesKtorPage != null && currentLanguagesKtorPage.isModified())
                || (currentLanguagesKubernetesPage != null && currentLanguagesKubernetesPage.isModified())
                || (currentLanguagesLombokPage != null && currentLanguagesLombokPage.isModified())
                || (currentLanguagesMarkdownPage != null && currentLanguagesMarkdownPage.isModified())
                || (currentLanguagesMicronautPage != null && currentLanguagesMicronautPage.isModified())
                || (currentLanguagesOpenApiPage != null && currentLanguagesOpenApiPage.isModified())
                || (currentLanguagesPlayPage != null && currentLanguagesPlayPage.isModified())
                || (currentLanguagesProtobufPage != null && currentLanguagesProtobufPage.isModified())
                || (currentLanguagesProtobufTextFormatPage != null && currentLanguagesProtobufTextFormatPage.isModified())
                || (currentLanguagesQuarkusPage != null && currentLanguagesQuarkusPage.isModified())
                || (currentLanguagesRbsPage != null && currentLanguagesRbsPage.isModified())
                || (currentLanguagesScalaEditorPage != null && currentLanguagesScalaEditorPage.isModified())
                || (currentLanguagesScalaXRayPage != null && currentLanguagesScalaXRayPage.isModified())
                || (currentLanguagesScalaProjectViewPage != null && currentLanguagesScalaProjectViewPage.isModified())
                || (currentLanguagesScalaPerformancePage != null && currentLanguagesScalaPerformancePage.isModified())
                || (currentLanguagesScalaWorksheetPage != null && currentLanguagesScalaWorksheetPage.isModified())
                || (currentLanguagesScalaBasePackagePage != null && currentLanguagesScalaBasePackagePage.isModified())
                || (currentLanguagesScalaMiscPage != null && currentLanguagesScalaMiscPage.isModified())
                || (currentLanguagesScalaUpdatesPage != null && currentLanguagesScalaUpdatesPage.isModified())
                || (currentLanguagesScalaExtensionsPage != null && currentLanguagesScalaExtensionsPage.isModified())
                || (currentLanguagesSchemasAndDtdsPage != null && currentLanguagesSchemasAndDtdsPage.isModified())
                || (currentLanguagesDefaultXmlSchemasPage != null && currentLanguagesDefaultXmlSchemasPage.isModified())
                || (currentLanguagesJsonSchemaMappingsPage != null && currentLanguagesJsonSchemaMappingsPage.isModified())
                || (currentLanguagesRemoteJsonSchemasPage != null && currentLanguagesRemoteJsonSchemasPage.isModified())
                || (currentLanguagesXmlCatalogPage != null && currentLanguagesXmlCatalogPage.isModified())
                || (currentLanguagesSpringPage != null && currentLanguagesSpringPage.isModified())
                || (currentLanguagesSqlDialectsPage != null && currentLanguagesSqlDialectsPage.isModified())
                || (currentLanguagesSqlResolutionScopesPage != null && currentLanguagesSqlResolutionScopesPage.isModified())
                || (currentLanguagesStyleSheetsDialectsPage != null && currentLanguagesStyleSheetsDialectsPage.isModified())
                || (currentLanguagesStyleSheetsStylelintPage != null && currentLanguagesStyleSheetsStylelintPage.isModified())
                || (currentLanguagesStyleSheetsTailwindPage != null && currentLanguagesStyleSheetsTailwindPage.isModified())
                || (currentLanguagesTablesPage != null && currentLanguagesTablesPage.isModified())
                || (currentLanguagesTemplateDataLanguagesPage != null && currentLanguagesTemplateDataLanguagesPage.isModified())
                || (currentLanguagesTypeScriptPage != null && currentLanguagesTypeScriptPage.isModified())
                || (currentLanguagesTypeScriptAngularPage != null && currentLanguagesTypeScriptAngularPage.isModified())
                || (currentLanguagesTypeScriptTsLintPage != null && currentLanguagesTypeScriptTsLintPage.isModified())
                || (currentLanguagesTypeScriptVuePage != null && currentLanguagesTypeScriptVuePage.isModified())
                || (currentLanguagesWebContextsPage != null && currentLanguagesWebContextsPage.isModified())
                || (currentLanguagesXsltPage != null && currentLanguagesXsltPage.isModified())
                || (currentLanguagesXsltFileAssociationsPage != null && currentLanguagesXsltFileAssociationsPage.isModified())
                || (currentApplicationServersPage != null && currentApplicationServersPage.isModified())
                || (currentBuildToolsPage != null && currentBuildToolsPage.isModified())
                || (currentMavenPage != null && currentMavenPage.isModified())
                || (currentMavenArchetypeCatalogsPage != null && currentMavenArchetypeCatalogsPage.isModified())
                || (currentMavenIgnoredFilesPage != null && currentMavenIgnoredFilesPage.isModified())
                || (currentMavenImportingPage != null && currentMavenImportingPage.isModified())
                || (currentMavenRepositoriesPage != null && currentMavenRepositoriesPage.isModified())
                || (currentMavenRunnerPage != null && currentMavenRunnerPage.isModified())
                || (currentMavenRunningTestsPage != null && currentMavenRunningTestsPage.isModified())
                || (currentGradlePage != null && currentGradlePage.isModified())
                || (currentGantPage != null && currentGantPage.isModified())
                || (currentBspPage != null && currentBspPage.isModified())
                || (currentCargoPage != null && currentCargoPage.isModified())
                || (currentSbtPage != null && currentSbtPage.isModified())
                || (currentCompilerPage != null && currentCompilerPage.isModified())
                || (currentAnnotationProcessorsPage != null && currentAnnotationProcessorsPage.isModified())
                || (currentCompilerExcludesPage != null && currentCompilerExcludesPage.isModified())
                || (currentGroovyCompilerPage != null && currentGroovyCompilerPage.isModified())
                || (currentJavaCompilerPage != null && currentJavaCompilerPage.isModified())
                || (currentKotlinCompilerPage != null && currentKotlinCompilerPage.isModified())
                || (currentRmiCompilerPage != null && currentRmiCompilerPage.isModified())
                || (currentScalaCompilerPage != null && currentScalaCompilerPage.isModified())
                || (currentScalaBytecodeIndicesPage != null && currentScalaBytecodeIndicesPage.isModified())
                || (currentScalaCompileServerPage != null && currentScalaCompileServerPage.isModified())
                || (currentValidationPage != null && currentValidationPage.isModified())
                || (currentBuildConsolePage != null && currentBuildConsolePage.isModified())
                || (currentPythonConsolePage != null && currentPythonConsolePage.isModified())
                || (currentCoveragePage != null && currentCoveragePage.isModified())
                || (currentFontPage != null && currentFontPage.isModified())
                || (currentColorSchemePage != null && currentColorSchemePage.isModified())
                || (currentColorSchemeGeneralPage != null && currentColorSchemeGeneralPage.isModified())
                || (currentColorSchemeLanguageDefaultsPage != null && currentColorSchemeLanguageDefaultsPage.isModified())
                || (currentColorSchemeFontPage != null && currentColorSchemeFontPage.isModified())
                || (currentConsoleFontPage != null && currentConsoleFontPage.isModified())
                || (currentConsoleColorsPage != null && currentConsoleColorsPage.isModified())
                || (currentCodeWithMePage != null && currentCodeWithMePage.isModified())
                || (currentColorSchemeDebuggerPage != null && currentColorSchemeDebuggerPage.isModified())
                || (currentColorSchemeDiffMergePage != null && currentColorSchemeDiffMergePage.isModified())
                || (currentColorSchemeJvmLoggingPage != null && currentColorSchemeJvmLoggingPage.isModified())
                || (currentColorSchemeUserDefinedFileTypesPage != null && currentColorSchemeUserDefinedFileTypesPage.isModified())
                || (currentColorSchemeVcsPage != null && currentColorSchemeVcsPage.isModified())
                || (currentColorSchemeJavaPage != null && currentColorSchemeJavaPage.isModified())
                || (currentColorSchemeAngularTemplatePage != null && currentColorSchemeAngularTemplatePage.isModified())
                || (currentColorSchemeContextFreeGrammarPage != null && currentColorSchemeContextFreeGrammarPage.isModified())
                || (currentColorSchemeCssPage != null && currentColorSchemeCssPage.isModified())
                || (currentColorSchemeDataEditorViewerPage != null && currentColorSchemeDataEditorViewerPage.isModified())
                || (currentColorSchemeDatabasePage != null && currentColorSchemeDatabasePage.isModified())
                || (currentColorSchemeDiagramsPage != null && currentColorSchemeDiagramsPage.isModified())
                || (currentColorSchemeDockerfilePage != null && currentColorSchemeDockerfilePage.isModified())
                || (currentColorSchemeEditorConfigPage != null && currentColorSchemeEditorConfigPage.isModified())
                || (currentColorSchemeErbPage != null && currentColorSchemeErbPage.isModified())
                || (currentColorSchemeFreeMarkerPage != null && currentColorSchemeFreeMarkerPage.isModified())
                || (currentColorSchemeGitLabCiExpressionPage != null && currentColorSchemeGitLabCiExpressionPage.isModified())
                || (currentColorSchemeGoPage != null && currentColorSchemeGoPage.isModified())
                || (currentColorSchemeGradleDeclarativePage != null && currentColorSchemeGradleDeclarativePage.isModified())
                || (currentColorSchemeGroovyPage != null && currentColorSchemeGroovyPage.isModified())
                || (currentColorSchemeHtmlPage != null && currentColorSchemeHtmlPage.isModified())
                || (currentColorSchemeHttpRequestPage != null && currentColorSchemeHttpRequestPage.isModified())
                || (currentColorSchemeJavaScriptPage != null && currentColorSchemeJavaScriptPage.isModified())
                || (currentColorSchemeJpaHibernateQlPage != null && currentColorSchemeJpaHibernateQlPage.isModified())
                || (currentColorSchemeJsonPage != null && currentColorSchemeJsonPage.isModified())
                || (currentColorSchemeJsonPathPage != null && currentColorSchemeJsonPathPage.isModified())
                || (currentColorSchemeJspPage != null && currentColorSchemeJspPage.isModified())
                || (currentColorSchemeJupyterNotebooksPage != null && currentColorSchemeJupyterNotebooksPage.isModified())
                || (currentColorSchemeKotlinPage != null && currentColorSchemeKotlinPage.isModified())
                || (currentColorSchemeKubernetesPage != null && currentColorSchemeKubernetesPage.isModified())
                || (currentColorSchemeLessPage != null && currentColorSchemeLessPage.isModified())
                || (currentColorSchemeLombokConfigPage != null && currentColorSchemeLombokConfigPage.isModified())
                || (currentColorSchemeMarkdownPage != null && currentColorSchemeMarkdownPage.isModified())
                || (currentColorSchemeMicronautELPage != null && currentColorSchemeMicronautELPage.isModified())
                || (currentColorSchemePhpPage != null && currentColorSchemePhpPage.isModified())
                || (currentColorSchemePlan9X86Page != null && currentColorSchemePlan9X86Page.isModified())
                || (currentColorSchemePostCSSPage != null && currentColorSchemePostCSSPage.isModified())
                || (currentColorSchemeProtocolBufferPage != null && currentColorSchemeProtocolBufferPage.isModified())
                || (currentColorSchemeMongoDbJsonPage != null && currentColorSchemeMongoDbJsonPage.isModified())
                || (currentColorSchemePropertiesPage != null && currentColorSchemePropertiesPage.isModified())
                || (currentColorSchemeProtocolBufferTextPage != null && currentColorSchemeProtocolBufferTextPage.isModified())
                || (currentColorSchemePythonPage != null && currentColorSchemePythonPage.isModified())
                || (currentColorSchemeQutePage != null && currentColorSchemeQutePage.isModified())
                || (currentColorSchemeRDocPage != null && currentColorSchemeRDocPage.isModified())
                || (currentColorSchemeRegExpPage != null && currentColorSchemeRegExpPage.isModified())
                || (currentColorSchemeRubyPage != null && currentColorSchemeRubyPage.isModified())
                || (currentColorSchemeRustPage != null && currentColorSchemeRustPage.isModified())
                || (currentColorSchemeSassPage != null && currentColorSchemeSassPage.isModified())
                || (currentColorSchemeScalaPage != null && currentColorSchemeScalaPage.isModified())
                || (currentColorSchemeShellScriptPage != null && currentColorSchemeShellScriptPage.isModified())
                || (currentColorSchemeSmartyPage != null && currentColorSchemeSmartyPage.isModified())
                || (currentColorSchemeSpringELPage != null && currentColorSchemeSpringELPage.isModified())
                || (currentColorSchemeSQLPage != null && currentColorSchemeSQLPage.isModified())
                || (currentColorSchemeTableDiffPage != null && currentColorSchemeTableDiffPage.isModified())
                || (currentColorSchemeTomlPage != null && currentColorSchemeTomlPage.isModified())
                || (currentColorSchemeTypeScriptPage != null && currentColorSchemeTypeScriptPage.isModified())
                || (currentColorSchemeVelocityPage != null && currentColorSchemeVelocityPage.isModified())
                || (currentColorSchemeXmlPage != null && currentColorSchemeXmlPage.isModified())
                || (currentColorSchemeXPathPage != null && currentColorSchemeXPathPage.isModified())
                || (currentColorSchemeXsltPage != null && currentColorSchemeXsltPage.isModified())
                || (currentColorSchemeYamlPage != null && currentColorSchemeYamlPage.isModified())
                || (currentColorSchemeByScopePage != null && currentColorSchemeByScopePage.isModified())
                || (currentColorSchemeImagesPage != null && currentColorSchemeImagesPage.isModified())
                || (currentCodeStylePage != null && currentCodeStylePage.isModified())
                || (currentCodeStyleJavaPage != null && currentCodeStyleJavaPage.isModified())
                || (currentCodeStyleLanguagePage != null && currentCodeStyleLanguagePage.isModified());
        applyButton.setDisable(!modified);
        applyButton.setStyle(modified
                ? "-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 6 16 6 16; -fx-background-radius: 4; -fx-cursor: hand;"
                : "-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-text-fill: #6F737A; -fx-font-size: 12px; -fx-padding: 6 16 6 16; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: default;");
    }

    // --------------------------------------------------- Button Bar

    private HBox buildButtonBar() {
        Button helpBtn = new Button("?");
        helpBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px; -fx-max-width: 24px; -fx-max-height: 24px; -fx-padding: 0;");

        Button ok = new Button("OK");
        ok.getStyleClass().add("dialog-primary");
        ok.setDefaultButton(true);
        ok.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 18 6 18; -fx-background-radius: 4; -fx-cursor: hand;");
        ok.setOnAction(e -> {
            applyAll();
            stage.close();
            if (dev.lumina.plugin.PluginManager.getInstance().hasPendingRestart()) {
                List<String> names = dev.lumina.plugin.PluginManager.getInstance().getPendingRestartPlugins()
                        .stream().map(dev.lumina.plugin.PluginItem::getName).toList();
                dev.lumina.util.IdeRestartHelper.promptAndRestart(stage.getOwner(), null, names);
            }
        });

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 6 16 6 16; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        cancel.setOnAction(e -> {
            if (currentEditorGeneralPage != null) {
                currentEditorGeneralPage.reset();
            }
            if (currentAutoImportPage != null) {
                currentAutoImportPage.reset();
            }
            if (currentEditorAppearancePage != null) {
                currentEditorAppearancePage.reset();
            }
            if (currentBreadcrumbsPage != null) {
                currentBreadcrumbsPage.reset();
            }
            if (currentCodeCompletionPage != null) {
                currentCodeCompletionPage.reset();
            }
            if (currentCodeFoldingPage != null) {
                currentCodeFoldingPage.reset();
            }
            if (currentConsolePage != null) {
                currentConsolePage.reset();
            }
            if (currentEditorTabsPage != null) {
                currentEditorTabsPage.reset();
            }
            if (currentGutterIconsPage != null) {
                currentGutterIconsPage.reset();
            }
            if (currentInlineCompletionPage != null) {
                currentInlineCompletionPage.reset();
            }
            if (currentPostfixCompletionPage != null) {
                currentPostfixCompletionPage.reset();
            }
            if (currentSmartKeysPage != null) {
                currentSmartKeysPage.reset();
            }
            if (currentSmartKeysYamlPage != null) {
                currentSmartKeysYamlPage.reset();
            }
            if (currentSmartKeysHtmlCssPage != null) {
                currentSmartKeysHtmlCssPage.reset();
            }
            if (currentSmartKeysPythonPage != null) {
                currentSmartKeysPythonPage.reset();
            }
            if (currentSmartKeysJsonPage != null) {
                currentSmartKeysJsonPage.reset();
            }
            if (currentSmartKeysRustPage != null) {
                currentSmartKeysRustPage.reset();
            }
            if (currentSmartKeysMarkdownPage != null) {
                currentSmartKeysMarkdownPage.reset();
            }
            if (currentSmartKeysScalaPage != null) {
                currentSmartKeysScalaPage.reset();
            }
            if (currentSmartKeysSqlPage != null) {
                currentSmartKeysSqlPage.reset();
            }
            if (currentSmartKeysRubyPage != null) {
                currentSmartKeysRubyPage.reset();
            }
            if (currentSmartKeysJsPage != null) {
                currentSmartKeysJsPage.reset();
            }
            if (currentSmartKeysPhpPage != null) {
                currentSmartKeysPhpPage.reset();
            }
            if (currentStickyLinesPage != null) {
                currentStickyLinesPage.reset();
            }
            if (currentCodeEditingPage != null) {
                currentCodeEditingPage.reset();
            }
            if (currentInspectionsPage != null) {
                currentInspectionsPage.reset();
            }
            if (currentIntentionsPage != null) {
                currentIntentionsPage.reset();
            }
            if (currentLanguageInjectionsPage != null) {
                currentLanguageInjectionsPage.reset();
            }
            if (currentLanguageInjectionsAdvancedPage != null) {
                currentLanguageInjectionsAdvancedPage.reset();
            }
            if (currentNaturalLanguagesPage != null) {
                currentNaturalLanguagesPage.reset();
            }
            if (currentGrammarAndStylePage != null) {
                currentGrammarAndStylePage.reset();
            }
            if (currentSpellingPage != null) {
                currentSpellingPage.reset();
            }
            if (currentReaderModePage != null) {
                currentReaderModePage.reset();
            }
            if (currentTextMateBundlesPage != null) {
                currentTextMateBundlesPage.reset();
            }
            if (currentTodoPage != null) {
                currentTodoPage.reset();
            }
            if (currentPythonDebuggerPage != null) {
                currentPythonDebuggerPage.reset();
            }
            if (currentDebuggerPage != null) {
                currentDebuggerPage.reset();
            }
            if (currentDebuggerAsyncStackTracesPage != null) {
                currentDebuggerAsyncStackTracesPage.reset();
            }
            if (currentDebuggerDataViewsPage != null) {
                currentDebuggerDataViewsPage.reset();
            }
            if (currentDebuggerDataViewsJavaPage != null) {
                currentDebuggerDataViewsJavaPage.reset();
            }
            if (currentDebuggerDataViewsTypeRenderersPage != null) {
                currentDebuggerDataViewsTypeRenderersPage.reset();
            }
            if (currentDebuggerHotSwapPage != null) {
                currentDebuggerHotSwapPage.reset();
            }
            if (currentDebuggerSteppingPage != null) {
                currentDebuggerSteppingPage.reset();
            }
            if (currentDebuggerDataViewsJavaScriptPage != null) {
                currentDebuggerDataViewsJavaScriptPage.reset();
            }
            if (currentDeploymentPage != null) {
                currentDeploymentPage.reset();
            }
            if (currentDeploymentOptionsPage != null) {
                currentDeploymentOptionsPage.reset();
            }
            if (currentDockerPage != null) {
                currentDockerPage.reset();
            }
            if (currentDockerConsolePage != null) {
                currentDockerConsolePage.reset();
            }
            if (currentDockerRegistryPage != null) {
                currentDockerRegistryPage.reset();
            }
            if (currentJavaProfilerPage != null) {
                currentJavaProfilerPage.reset();
            }
            if (currentJavaProfilerFiltersPage != null) {
                currentJavaProfilerFiltersPage.reset();
            }
            if (currentBuildKubernetesPage != null) {
                currentBuildKubernetesPage.reset();
            }
            if (currentRemoteJarRepositoriesPage != null) {
                currentRemoteJarRepositoriesPage.reset();
            }
            if (currentRunTargetsPage != null) {
                currentRunTargetsPage.reset();
            }
            if (currentLanguagesPhpPage != null) {
                currentLanguagesPhpPage.reset();
            }
            if (currentLanguagesPhpDebugPage != null) {
                currentLanguagesPhpDebugPage.reset();
            }
            if (currentLanguagesPhpDebugDbgpProxyPage != null) {
                currentLanguagesPhpDebugDbgpProxyPage.reset();
            }
            if (currentLanguagesPhpDebugSkippedPathsPage != null) {
                currentLanguagesPhpDebugSkippedPathsPage.reset();
            }
            if (currentLanguagesPhpDebugStepFiltersPage != null) {
                currentLanguagesPhpDebugStepFiltersPage.reset();
            }
            if (currentLanguagesPhpDebugXdebugCloudPage != null) {
                currentLanguagesPhpDebugXdebugCloudPage.reset();
            }
            if (currentLanguagesPhpServersPage != null) {
                currentLanguagesPhpServersPage.reset();
            }
            if (currentLanguagesPhpComposerPage != null) {
                currentLanguagesPhpComposerPage.reset();
            }
            if (currentLanguagesPhpTestFrameworksPage != null) {
                currentLanguagesPhpTestFrameworksPage.reset();
            }
            if (currentPhpQualityToolsPage != null) {
                currentPhpQualityToolsPage.reset();
            }
            if (currentPhpCodeSnifferPage != null) {
                currentPhpCodeSnifferPage.reset();
            }
            if (currentPhpCsFixerPage != null) {
                currentPhpCsFixerPage.reset();
            }
            if (currentPhpLaravelPintPage != null) {
                currentPhpLaravelPintPage.reset();
            }
            if (currentPhpMessDetectorPage != null) {
                currentPhpMessDetectorPage.reset();
            }
            if (currentPhpFrameworksPage != null) {
                currentPhpFrameworksPage.reset();
            }
            if (currentPhpSmartyPage != null) {
                currentPhpSmartyPage.reset();
            }
            if (currentPythonTemplateLanguagesPage != null) {
                currentPythonTemplateLanguagesPage.reset();
            }
            if (currentLanguagesGoPage != null) {
                currentLanguagesGoPage.reset();
            }
            if (currentLanguagesGoGoRootPage != null) {
                currentLanguagesGoGoRootPage.reset();
            }
            if (currentLanguagesGoGoPathPage != null) {
                currentLanguagesGoGoPathPage.reset();
            }
            if (currentLanguagesGoModulesPage != null) {
                currentLanguagesGoModulesPage.reset();
            }
            if (currentLanguagesGoBuildTagsPage != null) {
                currentLanguagesGoBuildTagsPage.reset();
            }
            if (currentLanguagesGoFormattingFunctionsPage != null) {
                currentLanguagesGoFormattingFunctionsPage.reset();
            }
            if (currentLanguagesGoImportsPage != null) {
                currentLanguagesGoImportsPage.reset();
            }
            if (currentRustPage != null) {
                currentRustPage.reset();
            }
            if (currentRustExternalLintersPage != null) {
                currentRustExternalLintersPage.reset();
            }
            if (currentRustfmtPage != null) {
                currentRustfmtPage.reset();
            }
            if (currentLanguagesJavaFxPage != null) {
                currentLanguagesJavaFxPage.reset();
            }
            if (currentLanguagesJavaScriptPage != null) {
                currentLanguagesJavaScriptPage.reset();
            }
            if (currentLanguagesJsEsLintPage != null) {
                currentLanguagesJsEsLintPage.reset();
            }
            if (currentLanguagesJsJsHintPage != null) {
                currentLanguagesJsJsHintPage.reset();
            }
            if (currentLanguagesJsLibrariesPage != null) {
                currentLanguagesJsLibrariesPage.reset();
            }
            if (currentLanguagesJsPrettierPage != null) {
                currentLanguagesJsPrettierPage.reset();
            }
            if (currentLanguagesJsStyledComponentsPage != null) {
                currentLanguagesJsStyledComponentsPage.reset();
            }
            if (currentLanguagesJsVitePage != null) {
                currentLanguagesJsVitePage.reset();
            }
            if (currentLanguagesJsWebpackPage != null) {
                currentLanguagesJsWebpackPage.reset();
            }
            if (currentLanguagesJavaScriptRuntimePage != null) {
                currentLanguagesJavaScriptRuntimePage.reset();
            }
            if (currentLanguagesJvmLoggingPage != null) {
                currentLanguagesJvmLoggingPage.reset();
            }
            if (currentLanguagesKotlinScriptingPage != null) {
                currentLanguagesKotlinScriptingPage.reset();
            }
            if (currentLanguagesKtorPage != null) {
                currentLanguagesKtorPage.reset();
            }
            if (currentLanguagesKubernetesPage != null) {
                currentLanguagesKubernetesPage.reset();
            }
            if (currentLanguagesLombokPage != null) {
                currentLanguagesLombokPage.reset();
            }
            if (currentLanguagesMarkdownPage != null) {
                currentLanguagesMarkdownPage.reset();
            }
            if (currentLanguagesMicronautPage != null) {
                currentLanguagesMicronautPage.reset();
            }
            if (currentLanguagesOpenApiPage != null) {
                currentLanguagesOpenApiPage.reset();
            }
            if (currentLanguagesPlayPage != null) {
                currentLanguagesPlayPage.reset();
            }
            if (currentLanguagesProtobufPage != null) {
                currentLanguagesProtobufPage.reset();
            }
            if (currentLanguagesProtobufTextFormatPage != null) {
                currentLanguagesProtobufTextFormatPage.reset();
            }
            if (currentLanguagesQuarkusPage != null) {
                currentLanguagesQuarkusPage.reset();
            }
            if (currentLanguagesRbsPage != null) {
                currentLanguagesRbsPage.reset();
            }
            if (currentLanguagesScalaEditorPage != null) {
                currentLanguagesScalaEditorPage.reset();
            }
            if (currentLanguagesScalaXRayPage != null) {
                currentLanguagesScalaXRayPage.reset();
            }
            if (currentLanguagesScalaProjectViewPage != null) {
                currentLanguagesScalaProjectViewPage.reset();
            }
            if (currentLanguagesScalaPerformancePage != null) {
                currentLanguagesScalaPerformancePage.reset();
            }
            if (currentLanguagesScalaWorksheetPage != null) {
                currentLanguagesScalaWorksheetPage.reset();
            }
            if (currentLanguagesScalaBasePackagePage != null) {
                currentLanguagesScalaBasePackagePage.reset();
            }
            if (currentLanguagesScalaMiscPage != null) {
                currentLanguagesScalaMiscPage.reset();
            }
            if (currentLanguagesScalaUpdatesPage != null) {
                currentLanguagesScalaUpdatesPage.reset();
            }
            if (currentLanguagesScalaExtensionsPage != null) {
                currentLanguagesScalaExtensionsPage.reset();
            }
            if (currentLanguagesSchemasAndDtdsPage != null) {
                currentLanguagesSchemasAndDtdsPage.reset();
            }
            if (currentLanguagesDefaultXmlSchemasPage != null) {
                currentLanguagesDefaultXmlSchemasPage.reset();
            }
            if (currentLanguagesJsonSchemaMappingsPage != null) {
                currentLanguagesJsonSchemaMappingsPage.reset();
            }
            if (currentLanguagesRemoteJsonSchemasPage != null) {
                currentLanguagesRemoteJsonSchemasPage.reset();
            }
            if (currentLanguagesXmlCatalogPage != null) {
                currentLanguagesXmlCatalogPage.reset();
            }
            if (currentLanguagesSpringPage != null) {
                currentLanguagesSpringPage.reset();
            }
            if (currentLanguagesSqlDialectsPage != null) {
                currentLanguagesSqlDialectsPage.reset();
            }
            if (currentLanguagesSqlResolutionScopesPage != null) {
                currentLanguagesSqlResolutionScopesPage.reset();
            }
            if (currentLanguagesStyleSheetsDialectsPage != null) {
                currentLanguagesStyleSheetsDialectsPage.reset();
            }
            if (currentLanguagesStyleSheetsStylelintPage != null) {
                currentLanguagesStyleSheetsStylelintPage.reset();
            }
            if (currentLanguagesStyleSheetsTailwindPage != null) {
                currentLanguagesStyleSheetsTailwindPage.reset();
            }
            if (currentLanguagesTablesPage != null) {
                currentLanguagesTablesPage.reset();
            }
            if (currentLanguagesTemplateDataLanguagesPage != null) {
                currentLanguagesTemplateDataLanguagesPage.reset();
            }
            if (currentLanguagesTypeScriptPage != null) {
                currentLanguagesTypeScriptPage.reset();
            }
            if (currentLanguagesTypeScriptAngularPage != null) {
                currentLanguagesTypeScriptAngularPage.reset();
            }
            if (currentLanguagesTypeScriptTsLintPage != null) {
                currentLanguagesTypeScriptTsLintPage.reset();
            }
            if (currentLanguagesTypeScriptVuePage != null) {
                currentLanguagesTypeScriptVuePage.reset();
            }
            if (currentLanguagesWebContextsPage != null) {
                currentLanguagesWebContextsPage.reset();
            }
            if (currentLanguagesXsltPage != null) {
                currentLanguagesXsltPage.reset();
            }
            if (currentLanguagesXsltFileAssociationsPage != null) {
                currentLanguagesXsltFileAssociationsPage.reset();
            }
            if (currentApplicationServersPage != null) {
                currentApplicationServersPage.reset();
            }
            if (currentBuildToolsPage != null) {
                currentBuildToolsPage.reset();
            }
            if (currentMavenPage != null) {
                currentMavenPage.reset();
            }
            if (currentMavenArchetypeCatalogsPage != null) {
                currentMavenArchetypeCatalogsPage.reset();
            }
            if (currentMavenIgnoredFilesPage != null) {
                currentMavenIgnoredFilesPage.reset();
            }
            if (currentMavenImportingPage != null) {
                currentMavenImportingPage.reset();
            }
            if (currentMavenRepositoriesPage != null) {
                currentMavenRepositoriesPage.reset();
            }
            if (currentMavenRunnerPage != null) {
                currentMavenRunnerPage.reset();
            }
            if (currentMavenRunningTestsPage != null) {
                currentMavenRunningTestsPage.reset();
            }
            if (currentGradlePage != null) {
                currentGradlePage.reset();
            }
            if (currentGantPage != null) {
                currentGantPage.reset();
            }
            if (currentBspPage != null) {
                currentBspPage.reset();
            }
            if (currentCargoPage != null) {
                currentCargoPage.reset();
            }
            if (currentSbtPage != null) {
                currentSbtPage.reset();
            }
            if (currentCompilerPage != null) {
                currentCompilerPage.reset();
            }
            if (currentAnnotationProcessorsPage != null) {
                currentAnnotationProcessorsPage.reset();
            }
            if (currentCompilerExcludesPage != null) {
                currentCompilerExcludesPage.reset();
            }
            if (currentGroovyCompilerPage != null) {
                currentGroovyCompilerPage.reset();
            }
            if (currentJavaCompilerPage != null) {
                currentJavaCompilerPage.reset();
            }
            if (currentKotlinCompilerPage != null) {
                currentKotlinCompilerPage.reset();
            }
            if (currentRmiCompilerPage != null) {
                currentRmiCompilerPage.reset();
            }
            if (currentScalaCompilerPage != null) {
                currentScalaCompilerPage.reset();
            }
            if (currentScalaBytecodeIndicesPage != null) {
                currentScalaBytecodeIndicesPage.reset();
            }
            if (currentScalaCompileServerPage != null) {
                currentScalaCompileServerPage.reset();
            }
            if (currentValidationPage != null) {
                currentValidationPage.reset();
            }
            if (currentBuildConsolePage != null) {
                currentBuildConsolePage.reset();
            }
            if (currentPythonConsolePage != null) {
                currentPythonConsolePage.reset();
            }
            if (currentCoveragePage != null) {
                currentCoveragePage.reset();
            }
            if (currentFontPage != null) {
                currentFontPage.reset();
            }
            if (currentColorSchemePage != null) {
                currentColorSchemePage.reset();
            }
            if (currentColorSchemeGeneralPage != null) {
                currentColorSchemeGeneralPage.reset();
            }
            if (currentColorSchemeLanguageDefaultsPage != null) {
                currentColorSchemeLanguageDefaultsPage.reset();
            }
            if (currentColorSchemeDebuggerPage != null) {
                currentColorSchemeDebuggerPage.reset();
            }
            if (currentColorSchemeDiffMergePage != null) {
                currentColorSchemeDiffMergePage.reset();
            }
            if (currentColorSchemeJvmLoggingPage != null) {
                currentColorSchemeJvmLoggingPage.reset();
            }
            if (currentColorSchemeUserDefinedFileTypesPage != null) {
                currentColorSchemeUserDefinedFileTypesPage.reset();
            }
            if (currentColorSchemeVcsPage != null) {
                currentColorSchemeVcsPage.reset();
            }
            if (currentColorSchemeJavaPage != null) {
                currentColorSchemeJavaPage.reset();
            }
            if (currentColorSchemeAngularTemplatePage != null) {
                currentColorSchemeAngularTemplatePage.reset();
            }
            if (currentColorSchemeContextFreeGrammarPage != null) {
                currentColorSchemeContextFreeGrammarPage.reset();
            }
            if (currentColorSchemeCssPage != null) {
                currentColorSchemeCssPage.reset();
            }
            if (currentColorSchemeDataEditorViewerPage != null) {
                currentColorSchemeDataEditorViewerPage.reset();
            }
            if (currentColorSchemeDatabasePage != null) {
                currentColorSchemeDatabasePage.reset();
            }
            if (currentColorSchemeDiagramsPage != null) {
                currentColorSchemeDiagramsPage.reset();
            }
            if (currentColorSchemeDockerfilePage != null) {
                currentColorSchemeDockerfilePage.reset();
            }
            if (currentColorSchemeEditorConfigPage != null) {
                currentColorSchemeEditorConfigPage.reset();
            }
            if (currentColorSchemeErbPage != null) {
                currentColorSchemeErbPage.reset();
            }
            if (currentColorSchemeFreeMarkerPage != null) {
                currentColorSchemeFreeMarkerPage.reset();
            }
            if (currentColorSchemeGitLabCiExpressionPage != null) {
                currentColorSchemeGitLabCiExpressionPage.reset();
            }
            if (currentColorSchemeGoPage != null) {
                currentColorSchemeGoPage.reset();
            }
            if (currentColorSchemeGradleDeclarativePage != null) {
                currentColorSchemeGradleDeclarativePage.reset();
            }
            if (currentColorSchemeGroovyPage != null) {
                currentColorSchemeGroovyPage.reset();
            }
            if (currentColorSchemeHtmlPage != null) {
                currentColorSchemeHtmlPage.reset();
            }
            if (currentColorSchemeHttpRequestPage != null) {
                currentColorSchemeHttpRequestPage.reset();
            }
            if (currentColorSchemeJavaScriptPage != null) {
                currentColorSchemeJavaScriptPage.reset();
            }
            if (currentColorSchemeJpaHibernateQlPage != null) {
                currentColorSchemeJpaHibernateQlPage.reset();
            }
            if (currentColorSchemeJsonPage != null) {
                currentColorSchemeJsonPage.reset();
            }
            if (currentColorSchemeJsonPathPage != null) {
                currentColorSchemeJsonPathPage.reset();
            }
            if (currentColorSchemeJspPage != null) {
                currentColorSchemeJspPage.reset();
            }
            if (currentColorSchemeJupyterNotebooksPage != null) {
                currentColorSchemeJupyterNotebooksPage.reset();
            }
            if (currentColorSchemeKotlinPage != null) {
                currentColorSchemeKotlinPage.reset();
            }
            if (currentColorSchemeKubernetesPage != null) {
                currentColorSchemeKubernetesPage.reset();
            }
            if (currentColorSchemeLessPage != null) {
                currentColorSchemeLessPage.reset();
            }
            if (currentColorSchemeLombokConfigPage != null) {
                currentColorSchemeLombokConfigPage.reset();
            }
            if (currentColorSchemeMarkdownPage != null) {
                currentColorSchemeMarkdownPage.reset();
            }
            if (currentColorSchemeMicronautELPage != null) {
                currentColorSchemeMicronautELPage.reset();
            }
            if (currentColorSchemePhpPage != null) {
                currentColorSchemePhpPage.reset();
            }
            if (currentColorSchemePlan9X86Page != null) {
                currentColorSchemePlan9X86Page.reset();
            }
            if (currentColorSchemePostCSSPage != null) {
                currentColorSchemePostCSSPage.reset();
            }
            if (currentColorSchemeProtocolBufferPage != null) {
                currentColorSchemeProtocolBufferPage.reset();
            }
            if (currentColorSchemeMongoDbJsonPage != null) {
                currentColorSchemeMongoDbJsonPage.reset();
            }
            if (currentColorSchemePropertiesPage != null) {
                currentColorSchemePropertiesPage.reset();
            }
            if (currentColorSchemeProtocolBufferTextPage != null) {
                currentColorSchemeProtocolBufferTextPage.reset();
            }
            if (currentColorSchemePythonPage != null) {
                currentColorSchemePythonPage.reset();
            }
            if (currentColorSchemeQutePage != null) {
                currentColorSchemeQutePage.reset();
            }
            if (currentColorSchemeRDocPage != null) {
                currentColorSchemeRDocPage.reset();
            }
            if (currentColorSchemeRegExpPage != null) {
                currentColorSchemeRegExpPage.reset();
            }
            if (currentColorSchemeRubyPage != null) {
                currentColorSchemeRubyPage.reset();
            }
            if (currentColorSchemeRustPage != null) {
                currentColorSchemeRustPage.reset();
            }
            if (currentColorSchemeSassPage != null) {
                currentColorSchemeSassPage.reset();
            }
            if (currentColorSchemeScalaPage != null) {
                currentColorSchemeScalaPage.reset();
            }
            if (currentColorSchemeShellScriptPage != null) {
                currentColorSchemeShellScriptPage.reset();
            }
            if (currentColorSchemeSmartyPage != null) {
                currentColorSchemeSmartyPage.reset();
            }
            if (currentColorSchemeSpringELPage != null) {
                currentColorSchemeSpringELPage.reset();
            }
            if (currentColorSchemeSQLPage != null) {
                currentColorSchemeSQLPage.reset();
            }
            if (currentColorSchemeTableDiffPage != null) {
                currentColorSchemeTableDiffPage.reset();
            }
            if (currentColorSchemeTomlPage != null) {
                currentColorSchemeTomlPage.reset();
            }
            if (currentColorSchemeTypeScriptPage != null) {
                currentColorSchemeTypeScriptPage.reset();
            }
            if (currentColorSchemeVelocityPage != null) {
                currentColorSchemeVelocityPage.reset();
            }
            if (currentColorSchemeXmlPage != null) {
                currentColorSchemeXmlPage.reset();
            }
            if (currentColorSchemeXPathPage != null) {
                currentColorSchemeXPathPage.reset();
            }
            if (currentColorSchemeXsltPage != null) {
                currentColorSchemeXsltPage.reset();
            }
            if (currentColorSchemeYamlPage != null) {
                currentColorSchemeYamlPage.reset();
            }
            if (currentColorSchemeByScopePage != null) {
                currentColorSchemeByScopePage.reset();
            }
            if (currentColorSchemeImagesPage != null) {
                currentColorSchemeImagesPage.reset();
            }
            if (currentCodeStylePage != null) {
                currentCodeStylePage.reset();
            }
            if (currentCodeStyleJavaPage != null) {
                currentCodeStyleJavaPage.reset();
            }
            if (currentCodeStyleLanguagePage != null) {
                currentCodeStyleLanguagePage.reset();
            }
            if (currentFileAndCodeTemplatesPage != null) {
                currentFileAndCodeTemplatesPage.reset();
            }
            if (currentFileEncodingsPage != null) {
                currentFileEncodingsPage.reset();
            }
            if (currentLiveTemplatesPage != null) {
                currentLiveTemplatesPage.reset();
            }
            if (currentFileTypesPage != null) {
                currentFileTypesPage.reset();
            }
            if (currentCopyrightPage != null) {
                currentCopyrightPage.reset();
            }
            if (currentCopyrightProfilesPage != null) {
                currentCopyrightProfilesPage.reset();
            }
            if (currentCopyrightFormattingPage != null) {
                currentCopyrightFormattingPage.reset();
            }
            for (SettingsCopyrightFormattingLanguagePage lp : languageFormattingPages.values()) {
                lp.reset();
            }
            if (currentInlayHintsPage != null) {
                currentInlayHintsPage.reset();
            }
            if (currentDuplicatesPage != null) {
                currentDuplicatesPage.reset();
            }
            if (currentEmmetPage != null) {
                currentEmmetPage.reset();
            }
            if (currentEmmetCssPage != null) {
                currentEmmetCssPage.reset();
            }
            if (currentEmmetHtmlPage != null) {
                currentEmmetHtmlPage.reset();
            }
            if (currentEmmetJsxPage != null) {
                currentEmmetJsxPage.reset();
            }
            stage.close();
        });

        applyButton = new Button("Apply");
        applyButton.getStyleClass().add("dialog-secondary");
        applyButton.setOnAction(e -> {
            applyAll();
            if (dev.lumina.plugin.PluginManager.getInstance().hasPendingRestart()) {
                List<String> names = dev.lumina.plugin.PluginManager.getInstance().getPendingRestartPlugins()
                        .stream().map(dev.lumina.plugin.PluginItem::getName).toList();
                dev.lumina.util.IdeRestartHelper.promptAndRestart(stage, null, names);
            }
        });
        updateApplyButtonState();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, helpBtn, spacer, ok, cancel, applyButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 20, 12, 20));
        bar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        return bar;
    }

    public SettingsLiveTemplatesPage getCurrentLiveTemplatesPage() {
        return currentLiveTemplatesPage;
    }

    public SettingsFileTypesPage getCurrentFileTypesPage() {
        return currentFileTypesPage;
    }

    public SettingsCopyrightPage getCurrentCopyrightPage() {
        return currentCopyrightPage;
    }

    public SettingsCopyrightProfilesPage getCurrentCopyrightProfilesPage() {
        return currentCopyrightProfilesPage;
    }

    public SettingsCopyrightFormattingPage getCurrentCopyrightFormattingPage() {
        return currentCopyrightFormattingPage;
    }

    public Map<String, SettingsCopyrightFormattingLanguagePage> getLanguageFormattingPages() {
        return languageFormattingPages;
    }

    public SettingsInlayHintsPage getCurrentInlayHintsPage() {
        return currentInlayHintsPage;
    }

    public SettingsColorSchemeDebuggerPage getCurrentColorSchemeDebuggerPage() {
        return currentColorSchemeDebuggerPage;
    }

    public SettingsColorSchemeDiffMergePage getCurrentColorSchemeDiffMergePage() {
        return currentColorSchemeDiffMergePage;
    }

    public SettingsColorSchemeJvmLoggingPage getCurrentColorSchemeJvmLoggingPage() {
        return currentColorSchemeJvmLoggingPage;
    }

    public SettingsColorSchemeUserDefinedFileTypesPage getCurrentColorSchemeUserDefinedFileTypesPage() {
        return currentColorSchemeUserDefinedFileTypesPage;
    }

    public SettingsColorSchemeVcsPage getCurrentColorSchemeVcsPage() {
        return currentColorSchemeVcsPage;
    }

    public SettingsColorSchemeJavaPage getCurrentColorSchemeJavaPage() {
        return currentColorSchemeJavaPage;
    }

    public SettingsColorSchemeAngularTemplatePage getCurrentColorSchemeAngularTemplatePage() {
        return currentColorSchemeAngularTemplatePage;
    }

    public SettingsColorSchemeContextFreeGrammarPage getCurrentColorSchemeContextFreeGrammarPage() {
        return currentColorSchemeContextFreeGrammarPage;
    }

    public SettingsColorSchemeCssPage getCurrentColorSchemeCssPage() {
        return currentColorSchemeCssPage;
    }

    public SettingsColorSchemeDataEditorViewerPage getCurrentColorSchemeDataEditorViewerPage() {
        return currentColorSchemeDataEditorViewerPage;
    }

    public SettingsColorSchemeDatabasePage getCurrentColorSchemeDatabasePage() {
        return currentColorSchemeDatabasePage;
    }

    public SettingsColorSchemeDiagramsPage getCurrentColorSchemeDiagramsPage() {
        return currentColorSchemeDiagramsPage;
    }

    public SettingsColorSchemeDockerfilePage getCurrentColorSchemeDockerfilePage() {
        return currentColorSchemeDockerfilePage;
    }

    public SettingsColorSchemeEditorConfigPage getCurrentColorSchemeEditorConfigPage() {
        return currentColorSchemeEditorConfigPage;
    }

    public SettingsColorSchemeErbPage getCurrentColorSchemeErbPage() {
        return currentColorSchemeErbPage;
    }

    public SettingsColorSchemeFreeMarkerPage getCurrentColorSchemeFreeMarkerPage() {
        return currentColorSchemeFreeMarkerPage;
    }

    public SettingsColorSchemeGitLabCiExpressionPage getCurrentColorSchemeGitLabCiExpressionPage() {
        return currentColorSchemeGitLabCiExpressionPage;
    }

    public SettingsColorSchemeGoPage getCurrentColorSchemeGoPage() {
        return currentColorSchemeGoPage;
    }

    public SettingsColorSchemeGradleDeclarativePage getCurrentColorSchemeGradleDeclarativePage() {
        return currentColorSchemeGradleDeclarativePage;
    }

    public SettingsColorSchemeGroovyPage getCurrentColorSchemeGroovyPage() {
        return currentColorSchemeGroovyPage;
    }

    public SettingsColorSchemeHtmlPage getCurrentColorSchemeHtmlPage() {
        return currentColorSchemeHtmlPage;
    }

    public SettingsColorSchemeHttpRequestPage getCurrentColorSchemeHttpRequestPage() {
        return currentColorSchemeHttpRequestPage;
    }

    public SettingsColorSchemeJavaScriptPage getCurrentColorSchemeJavaScriptPage() {
        return currentColorSchemeJavaScriptPage;
    }

    public SettingsColorSchemeJpaHibernateQlPage getCurrentColorSchemeJpaHibernateQlPage() {
        return currentColorSchemeJpaHibernateQlPage;
    }

    public SettingsColorSchemeJsonPage getCurrentColorSchemeJsonPage() {
        return currentColorSchemeJsonPage;
    }

    public SettingsColorSchemeJsonPathPage getCurrentColorSchemeJsonPathPage() {
        return currentColorSchemeJsonPathPage;
    }

    public SettingsColorSchemeJspPage getCurrentColorSchemeJspPage() {
        return currentColorSchemeJspPage;
    }

    public SettingsColorSchemeJupyterNotebooksPage getCurrentColorSchemeJupyterNotebooksPage() {
        return currentColorSchemeJupyterNotebooksPage;
    }

    public SettingsColorSchemeKotlinPage getCurrentColorSchemeKotlinPage() {
        return currentColorSchemeKotlinPage;
    }

    public SettingsColorSchemeKubernetesPage getCurrentColorSchemeKubernetesPage() {
        return currentColorSchemeKubernetesPage;
    }

    public SettingsColorSchemeLessPage getCurrentColorSchemeLessPage() {
        return currentColorSchemeLessPage;
    }

    public SettingsColorSchemeLombokConfigPage getCurrentColorSchemeLombokConfigPage() {
        return currentColorSchemeLombokConfigPage;
    }

    public SettingsColorSchemeMarkdownPage getCurrentColorSchemeMarkdownPage() {
        return currentColorSchemeMarkdownPage;
    }

    public SettingsColorSchemeMicronautELPage getCurrentColorSchemeMicronautELPage() {
        return currentColorSchemeMicronautELPage;
    }

    public SettingsColorSchemePHPPage getCurrentColorSchemePhpPage() {
        return currentColorSchemePhpPage;
    }

    public SettingsColorSchemePlan9X86Page getCurrentColorSchemePlan9X86Page() {
        return currentColorSchemePlan9X86Page;
    }

    public SettingsColorSchemePostCSSPage getCurrentColorSchemePostCSSPage() {
        return currentColorSchemePostCSSPage;
    }

    public SettingsColorSchemeProtocolBufferPage getCurrentColorSchemeProtocolBufferPage() {
        return currentColorSchemeProtocolBufferPage;
    }

    public SettingsColorSchemeMongoDBJSONPage getCurrentColorSchemeMongoDbJsonPage() {
        return currentColorSchemeMongoDbJsonPage;
    }

    public SettingsColorSchemeMongoDBJSONPage getCurrentColorSchemeMongoDBJSONPage() {
        return currentColorSchemeMongoDbJsonPage;
    }

    public SettingsColorSchemePropertiesPage getCurrentColorSchemePropertiesPage() {
        return currentColorSchemePropertiesPage;
    }

    public SettingsColorSchemeProtocolBufferTextPage getCurrentColorSchemeProtocolBufferTextPage() {
        return currentColorSchemeProtocolBufferTextPage;
    }

    public SettingsColorSchemePythonPage getCurrentColorSchemePythonPage() {
        return currentColorSchemePythonPage;
    }

    public SettingsColorSchemeQutePage getCurrentColorSchemeQutePage() {
        return currentColorSchemeQutePage;
    }

    public SettingsColorSchemeRDocPage getCurrentColorSchemeRDocPage() {
        return currentColorSchemeRDocPage;
    }

    public SettingsColorSchemeRegExpPage getCurrentColorSchemeRegExpPage() {
        return currentColorSchemeRegExpPage;
    }

    public SettingsColorSchemeRubyPage getCurrentColorSchemeRubyPage() {
        return currentColorSchemeRubyPage;
    }

    public SettingsColorSchemeRustPage getCurrentColorSchemeRustPage() {
        return currentColorSchemeRustPage;
    }

    public SettingsColorSchemeSassPage getCurrentColorSchemeSassPage() {
        return currentColorSchemeSassPage;
    }

    public SettingsColorSchemeScalaPage getCurrentColorSchemeScalaPage() {
        return currentColorSchemeScalaPage;
    }

    public SettingsColorSchemeShellScriptPage getCurrentColorSchemeShellScriptPage() {
        return currentColorSchemeShellScriptPage;
    }

    public SettingsColorSchemeSmartyPage getCurrentColorSchemeSmartyPage() {
        return currentColorSchemeSmartyPage;
    }

    public SettingsColorSchemeSpringELPage getCurrentColorSchemeSpringELPage() {
        return currentColorSchemeSpringELPage;
    }

    public SettingsColorSchemeSQLPage getCurrentColorSchemeSQLPage() {
        return currentColorSchemeSQLPage;
    }

    public SettingsColorSchemeTableDiffPage getCurrentColorSchemeTableDiffPage() {
        return currentColorSchemeTableDiffPage;
    }

    public SettingsColorSchemeTOMLPage getCurrentColorSchemeTOMLPage() {
        return currentColorSchemeTomlPage;
    }

    public SettingsColorSchemeTypeScriptPage getCurrentColorSchemeTypeScriptPage() {
        return currentColorSchemeTypeScriptPage;
    }

    public SettingsColorSchemeVelocityPage getCurrentColorSchemeVelocityPage() {
        return currentColorSchemeVelocityPage;
    }

    public SettingsColorSchemeXMLPage getCurrentColorSchemeXMLPage() {
        return currentColorSchemeXmlPage;
    }

    public SettingsColorSchemeXPathPage getCurrentColorSchemeXPathPage() {
        return currentColorSchemeXPathPage;
    }

    public SettingsColorSchemeXSLTPage getCurrentColorSchemeXSLTPage() {
        return currentColorSchemeXsltPage;
    }

    public SettingsColorSchemeYAMLPage getCurrentColorSchemeYAMLPage() {
        return currentColorSchemeYamlPage;
    }

    public SettingsColorSchemeByScopePage getCurrentColorSchemeByScopePage() {
        return currentColorSchemeByScopePage;
    }

    public SettingsColorSchemeImagesPage getCurrentColorSchemeImagesPage() {
        return currentColorSchemeImagesPage;
    }

    public SettingsCodeStylePage getCurrentCodeStylePage() {
        return currentCodeStylePage;
    }

    public SettingsCodeStyleJavaPage getCurrentCodeStyleJavaPage() {
        return currentCodeStyleJavaPage;
    }

    public SettingsCodeStyleLanguagePage getCurrentCodeStyleLanguagePage() {
        return currentCodeStyleLanguagePage;
    }

    public SettingsSmartKeysJavaScriptPage getCurrentSmartKeysJsPage() {
        return currentSmartKeysJsPage;
    }

    public Button getApplyButton() {
        return applyButton;
    }
}