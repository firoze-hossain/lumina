package dev.lumina.ui;

import dev.lumina.settings.CodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOptionType;
import dev.lumina.settings.CodeStyleSettings.CodeStyleScheme;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;
import dev.lumina.settings.AngularHtmlCodeStyleSettings;
import dev.lumina.settings.EditorConfigCodeStyleSettings;
import dev.lumina.settings.ErbCodeStyleSettings;
import dev.lumina.settings.GoCodeStyleSettings;
import dev.lumina.settings.GradleDeclarativeCodeStyleSettings;
import dev.lumina.settings.GroovyCodeStyleSettings;
import dev.lumina.settings.HtmlCodeStyleSettings;
import dev.lumina.settings.HttpRequestCodeStyleSettings;
import dev.lumina.settings.JavaScriptCodeStyleSettings;
import dev.lumina.settings.KotlinCodeStyleSettings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal dynamic configuration and live preview page for Editor > Code Style > [Language]
 * (e.g. Kotlin, Python, Go, Rust, TypeScript, HTML, etc.).
 *
 * Implements a completely dynamic, provider-driven architecture:
 * - Language provider supplies tabs, groups, options, and tab-specific sample code dynamically.
 * - Zero hardcoding: controls, dropdowns, and sections are created and bound dynamically.
 * - Matches 1:1 with reference screenshots for Kotlin and other languages.
 */
public class SettingsCodeStyleLanguagePage extends VBox {

    private final String languageId;
    private final CodeStyleHeaderBar headerBar;
    private final Hyperlink setFromLink = new Hyperlink("Set from...");

    // Tab Bar
    private final HBox tabBar = new HBox(4);
    private final ToggleGroup tabGroup = new ToggleGroup();
    private String activeTab = "Tabs and Indents";

    // Split layout: Left options pane, Right preview pane
    private final SplitPane splitPane = new SplitPane();
    private final ScrollPane leftScrollPane = new ScrollPane();
    private final VBox leftContentBox = new VBox(12);

    // Right Preview
    private VBox rightPane;
    private final ScrollPane previewScrollPane = new ScrollPane();
    private final VBox previewLinesBox = new VBox(1);

    // Working settings copy and baseline snapshot for modification tracking
    private LanguageCodeStyleSettings workingSettings;
    private LanguageCodeStyleSettings baselineSettings;

    // Dynamically registered UI controls for the active tab
    private final Map<String, CheckBox> checkboxControls = new HashMap<>();
    private final Map<String, TextField> numberControls = new HashMap<>();
    private final Map<String, ComboBox<String>> comboControls = new HashMap<>();
    private final Map<String, TextField> textControls = new HashMap<>();

    // Controls for Imports Tab
    private ToggleGroup topLevelGroup;
    private TextField topLevelThresholdField;
    private ToggleGroup staticsGroup;
    private TextField staticsThresholdField;
    private CheckBox nestedClassesCheckBox;
    private CheckBox importAliasesCheckBox;
    private TableView<KotlinCodeStyleSettings.ImportEntry> packagesOnDemandTable;
    private TableView<KotlinCodeStyleSettings.ImportEntry> importLayoutTable;

    private static final List<String> TRAILING_COMMA_CHILDREN = List.of(
            KotlinCodeStyleSettings.TRAILING_COMMA_TYPE_PARAMETER_LIST,
            KotlinCodeStyleSettings.TRAILING_COMMA_DESTRUCTURING_DECLARATION,
            KotlinCodeStyleSettings.TRAILING_COMMA_WHEN_ENTRY,
            KotlinCodeStyleSettings.TRAILING_COMMA_FUNCTION_LITERAL,
            KotlinCodeStyleSettings.TRAILING_COMMA_VALUE_PARAMETER_LIST,
            KotlinCodeStyleSettings.TRAILING_COMMA_CONTEXT_RECEIVER_LIST,
            KotlinCodeStyleSettings.TRAILING_COMMA_COLLECTION_LITERAL_EXPRESSION,
            KotlinCodeStyleSettings.TRAILING_COMMA_TYPE_ARGUMENT_LIST,
            KotlinCodeStyleSettings.TRAILING_COMMA_INDICES,
            KotlinCodeStyleSettings.TRAILING_COMMA_VALUE_ARGUMENT_LIST
    );

    // Controls for Go Imports Tab
    private CheckBox goBackquotesCb;
    private CheckBox goSingleImportParensCb;
    private CheckBox goRemoveRedundantCb;
    private ComboBox<String> goSortingCombo;
    private CheckBox goMoveAllSingleDeclCb;
    private CheckBox goGroupSdkCb;
    private CheckBox goMoveAllSingleGroupCb;
    private CheckBox goGroupCb;
    private ToggleGroup goGroupModeGroup;
    private RadioButton goRbProject;
    private RadioButton goRbPrefixes;
    private TextArea goPrefixesArea;

    // Controls for Go Other Tab
    private CheckBox goLeadingSpaceCb;
    private TextField goColWidthField;
    private CheckBox goRunGofmtCb;

    // Controls for Groovy Imports Tab
    private CheckBox groovyUseSingleClassImportCb;
    private CheckBox groovyUseFqClassNamesCb;
    private CheckBox groovyInsertForInnerClassesCb;
    private CheckBox groovyUseFqClassNamesInJavadocCb;
    private TextField groovyClassCountStarField;
    private TextField groovyStaticCountStarField;
    private TableView<GroovyCodeStyleSettings.GroovyImportEntry> groovyPackagesOnDemandTable;
    private CheckBox groovyLayoutStaticSeparatelyCb;
    private TableView<GroovyCodeStyleSettings.GroovyImportEntry> groovyImportLayoutTable;

    // Controls for Groovy Code Generation Tab
    private ListView<String> groovyOrderOfMembersListView;
    private CheckBox groovyLineCommentFirstColCb;
    private CheckBox groovyAddSpaceLineCommentCb;
    private CheckBox groovyEnforceOnReformatCb;
    private CheckBox groovyBlockCommentFirstColCb;
    private CheckBox groovyAddSpacesAroundBlockCommentsCb;

    // Controls for HTML Other Tab
    private TextField htmlHardWrapField;
    private ComboBox<String> htmlWrapOnTypingCombo;
    private TextField htmlVisualGuidesField;
    private CheckBox htmlKeepLineBreaksCb;
    private CheckBox htmlKeepLineBreaksInTextCb;
    private TextField htmlKeepBlankLinesField;
    private ComboBox<String> htmlWrapAttributesCombo;
    private CheckBox htmlWrapTextCb;
    private CheckBox htmlAlignAttributesCb;
    private CheckBox htmlAlignTextCb;
    private CheckBox htmlKeepWhiteSpacesCb;
    private CheckBox htmlSpacesAroundEqCb;
    private CheckBox htmlSpacesAfterTagCb;
    private CheckBox htmlSpacesInEmptyTagCb;
    private TextField htmlInsertNewLineBeforeField;
    private TextField htmlRemoveNewLineBeforeField;
    private TextField htmlDoNotIndentChildrenOfField;
    private TextField htmlDoNotIndentTagSizeMoreThanField;
    private TextField htmlInlineElementsField;
    private TextField htmlKeepWhiteSpacesInsideField;
    private TextField htmlDontBreakIfInlineContentField;
    private ComboBox<String> htmlNewLineBeforeFirstAttrCombo;
    private ComboBox<String> htmlNewLineAfterLastAttrCombo;
    private ComboBox<String> htmlAddForJsxAttributesCombo;
    private ComboBox<String> htmlGeneratedQuoteMarksCombo;
    private CheckBox htmlEnforceOnFormatCb;

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsCodeStyleLanguagePage(String languageId) {
        this.languageId = languageId != null ? languageId : "Kotlin";

        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(this.languageId);
        if (provider != null && !provider.getSupportedTabs().isEmpty()) {
            this.activeTab = provider.getSupportedTabs().get(0);
        }

        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(12, 16, 16, 16));
        setSpacing(10);

        headerBar = new CodeStyleHeaderBar();
        headerBar.setOnSchemeChanged(scheme -> {
            loadFromCurrentScheme();
            notifyModified();
        });
        headerBar.setOnSettingsModified(this::notifyModified);

        initWorkingSettings();
        buildUi();
        loadFromCurrentScheme();
        renderActiveTabControls();
        updatePreview();
    }

    private void initWorkingSettings() {
        CodeStyleScheme scheme = CodeStyleSettings.getInstance().getActiveScheme();
        LanguageCodeStyleSettings original = scheme != null ? scheme.getLanguageSettings(languageId) : null;
        if (original != null) {
            workingSettings = original.copy();
            baselineSettings = original.copy();
        } else {
            LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(languageId);
            workingSettings = provider != null ? provider.createDefaultSettings() : new LanguageCodeStyleSettings(languageId);
            baselineSettings = workingSettings.copy();
        }
    }

    private void buildUi() {
        // --- 1. Top Bar with Scheme and "Set from..." link ---
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerBar, Priority.ALWAYS);

        setFromLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false;");
        setFromLink.setOnMouseEntered(e -> setFromLink.setUnderline(true));
        setFromLink.setOnMouseExited(e -> setFromLink.setUnderline(false));

        ContextMenu setFromMenu = new ContextMenu();
        setFromMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");

        MenuItem googleStyle = new MenuItem("Google Style Guide");
        googleStyle.setOnAction(e -> applyPredefinedGoogleStyle());

        MenuItem platformDefault = new MenuItem("Platform Default Style");
        platformDefault.setOnAction(e -> applyPredefinedDefaultStyle());

        Menu setFromLangMenu = new Menu("Set from language...");
        for (LanguageCodeStyleProvider p : LanguageCodeStyleProvider.getAllProviders()) {
            if (!p.getLanguageId().equals(this.languageId)) {
                MenuItem item = new MenuItem(p.getDisplayName());
                item.setOnAction(ev -> copyFromOtherLanguage(p.getLanguageId()));
                setFromLangMenu.getItems().add(item);
            }
        }

        setFromMenu.getItems().addAll(googleStyle, platformDefault, new SeparatorMenuItem(), setFromLangMenu);
        setFromLink.setOnAction(e -> setFromMenu.show(setFromLink, Side.BOTTOM, 0, 2));

        topBar.getChildren().addAll(headerBar, setFromLink);
        getChildren().add(topBar);

        // --- 2. Tab Bar ---
        buildTabBar();
        ScrollPane tabScrollPane = new ScrollPane(tabBar);
        tabScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tabScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tabScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        tabScrollPane.setFitToHeight(true);
        getChildren().add(tabScrollPane);

        // --- 3. Split Pane ---
        leftContentBox.setStyle("-fx-background-color: #1E1F22;");
        leftContentBox.setPadding(new Insets(8, 16, 12, 4));

        leftScrollPane.setContent(leftContentBox);
        leftScrollPane.setFitToWidth(true);
        leftScrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: transparent; -fx-border-color: transparent;");

        rightPane = buildPreviewPane();

        splitPane.getItems().addAll(leftScrollPane, rightPane);
        splitPane.setDividerPositions(0.44);
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        getChildren().add(splitPane);
    }

    private void buildTabBar() {
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(2, 0, 6, 0));

        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(languageId);
        List<String> tabs = provider != null ? provider.getSupportedTabs() : List.of("Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines");

        if (!tabs.contains(activeTab) && !tabs.isEmpty()) {
            activeTab = tabs.get(0);
        }

        for (String tabTitle : tabs) {
            ToggleButton btn = new ToggleButton(tabTitle);
            btn.setToggleGroup(tabGroup);
            styleTabButton(btn);

            if (tabTitle.equals(activeTab)) {
                btn.setSelected(true);
            }

            btn.setOnAction(e -> {
                saveCurrentTabUiToWorkingSettings();
                activeTab = tabTitle;
                renderActiveTabControls();
                updatePreview();
            });

            tabBar.getChildren().add(btn);
        }
    }

    private void styleTabButton(ToggleButton btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-padding: 4 10 4 10; -fx-cursor: hand; -fx-border-color: transparent;");
        btn.selectedProperty().addListener((obs, old, isSelected) -> {
            if (isSelected) {
                btn.setStyle("-fx-background-color: #35538F; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 10 4 10; -fx-background-radius: 4;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-padding: 4 10 4 10; -fx-cursor: hand; -fx-border-color: transparent;");
            }
        });
        if (btn.isSelected()) {
            btn.setStyle("-fx-background-color: #35538F; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 10 4 10; -fx-background-radius: 4;");
        }
    }

    private VBox buildPreviewPane() {
        VBox pane = new VBox();
        pane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        pane.setPadding(new Insets(6));

        previewLinesBox.setStyle("-fx-background-color: #1E1F22;");
        previewLinesBox.setPadding(new Insets(10, 14, 14, 14));

        previewScrollPane.setContent(previewLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        pane.getChildren().add(previewScrollPane);
        return pane;
    }

    /**
     * Dynamically builds and binds the left pane controls for the currently active tab.
     */
    private void renderActiveTabControls() {
        leftContentBox.getChildren().clear();
        checkboxControls.clear();
        numberControls.clear();
        comboControls.clear();
        textControls.clear();

        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(languageId);
        boolean hasPreview = provider != null && provider.hasPreview(activeTab);
        if (hasPreview) {
            if (rightPane != null && !splitPane.getItems().contains(rightPane)) {
                splitPane.getItems().setAll(leftScrollPane, rightPane);
                splitPane.setDividerPositions(0.44);
            }
        } else {
            if (rightPane != null && splitPane.getItems().contains(rightPane)) {
                splitPane.getItems().setAll(leftScrollPane);
            }
        }

        if ("Imports".equals(activeTab)) {
            if ("Go".equals(languageId)) {
                renderGoImportsTab();
                return;
            } else if ("Groovy".equals(languageId)) {
                renderGroovyImportsTab();
                return;
            } else if (!"JavaScript".equals(languageId)) {
                renderImportsTab();
                return;
            }
        }

        if ("Punctuation".equals(activeTab) && "JavaScript".equals(languageId)) {
            renderJavaScriptPunctuationTab();
            return;
        }

        if ("Code Generation".equals(activeTab) && "Groovy".equals(languageId)) {
            renderGroovyCodeGenerationTab();
            return;
        }

        if ("Other".equals(activeTab)) {
            if ("Go".equals(languageId)) {
                renderGoOtherTab();
                return;
            } else if ("HTML".equals(languageId)) {
                renderHtmlOtherTab();
                return;
            }
        }

        if ("Arrangement".equals(activeTab)) {
            renderArrangementTab();
            return;
        }

        List<CodeStyleGroup> groups = provider != null ? provider.getOptionGroups(activeTab) : Collections.emptyList();

        if (groups.isEmpty() && "Tabs and Indents".equals(activeTab)) {
            // Render standard fallback indents
            renderStandardTabsAndIndents();
            return;
        }

        for (CodeStyleGroup group : groups) {
            if (group.isDivider()) {
                // Section with horizontal divider line (e.g. Blank Lines)
                VBox sectionBox = new VBox(6);
                HBox headerRow = new HBox(8);
                headerRow.setAlignment(Pos.CENTER_LEFT);

                Label sectionLabel = new Label(group.getName());
                sectionLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-font-weight: bold;");

                Separator sep = new Separator();
                sep.setStyle("-fx-background-color: #393B40; -fx-border-color: transparent;");
                HBox.setHgrow(sep, Priority.ALWAYS);

                headerRow.getChildren().addAll(sectionLabel, sep);
                sectionBox.getChildren().add(headerRow);

                VBox optionsBox = new VBox(6);
                optionsBox.setPadding(new Insets(4, 0, 8, 4));
                for (CodeStyleOption opt : group.getOptions()) {
                    optionsBox.getChildren().add(buildOptionNode(opt));
                }
                sectionBox.getChildren().add(optionsBox);
                leftContentBox.getChildren().add(sectionBox);

            } else if (group.isCollapsible()) {
                // Collapsible section with triangle toggle and optional header combo
                VBox groupBox = new VBox(4);
                BooleanProperty expandedProp = new SimpleBooleanProperty(group.isExpanded());

                HBox headerRow = new HBox(8);
                headerRow.setAlignment(Pos.CENTER_LEFT);
                headerRow.setStyle("-fx-cursor: hand; -fx-padding: 3 0 3 0;");

                Label arrowLabel = new Label(expandedProp.get() ? "▼" : "▶");
                arrowLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 10px; -fx-min-width: 14px;");

                Label titleLabel = new Label(group.getName());
                titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                headerRow.getChildren().addAll(arrowLabel, titleLabel, spacer);

                if (group.getHeaderComboKey() != null) {
                    ComboBox<String> headerCombo = new ComboBox<>();
                    headerCombo.getItems().addAll(group.getHeaderComboChoices());
                    String currentVal = workingSettings.getString(group.getHeaderComboKey(), group.getHeaderComboDefault());
                    headerCombo.setValue(currentVal);
                    styleComboBox(headerCombo);
                    headerCombo.setOnAction(e -> {
                        if (!suppressEvents) {
                            workingSettings.setString(group.getHeaderComboKey(), headerCombo.getValue());
                            updatePreview();
                            notifyModified();
                        }
                    });
                    comboControls.put(group.getHeaderComboKey(), headerCombo);
                    headerRow.getChildren().add(headerCombo);
                }

                VBox contentBox = new VBox(6);
                contentBox.setPadding(new Insets(2, 0, 4, 22));
                for (CodeStyleOption opt : group.getOptions()) {
                    contentBox.getChildren().add(buildOptionNode(opt));
                }

                contentBox.visibleProperty().bind(expandedProp);
                contentBox.managedProperty().bind(expandedProp);

                headerRow.setOnMouseClicked(e -> {
                    // Avoid toggling collapse when clicking directly inside the header combo
                    if (e.getTarget() instanceof ComboBox || e.getTarget() instanceof ListCell) return;
                    boolean next = !expandedProp.get();
                    expandedProp.set(next);
                    arrowLabel.setText(next ? "▼" : "▶");
                });

                groupBox.getChildren().addAll(headerRow, contentBox);
                leftContentBox.getChildren().add(groupBox);

            } else {
                // Flat group without collapse header (e.g. General top options or Standalone Combos)
                VBox flatBox = new VBox(6);
                flatBox.setPadding(new Insets(2, 0, 6, 0));
                for (CodeStyleOption opt : group.getOptions()) {
                    flatBox.getChildren().add(buildOptionNode(opt));
                }
                leftContentBox.getChildren().add(flatBox);
            }
        }
    }

    private Node buildOptionNode(CodeStyleOption opt) {
        if (opt.getType() == CodeStyleOptionType.CHECKBOX) {
            if (opt.isRightAligned()) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);

                Label lbl = new Label(opt.getLabel());
                lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                CheckBox cb = new CheckBox();
                cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                boolean val = isStandardIndentOption(opt.getKey())
                        ? getStandardIndentBoolean(opt.getKey())
                        : workingSettings.getBoolean(opt.getKey(), (Boolean) opt.getDefaultValue());
                cb.setSelected(val);

                cb.selectedProperty().addListener((obs, oldV, newV) -> {
                    if (!suppressEvents) {
                        if (isStandardIndentOption(opt.getKey())) {
                            setStandardIndentBoolean(opt.getKey(), newV);
                        } else {
                            workingSettings.setBoolean(opt.getKey(), newV);
                        }
                        updateDependentControls(opt.getKey(), newV);
                        updatePreview();
                        notifyModified();
                    }
                });

                checkboxControls.put(opt.getKey(), cb);
                row.getChildren().addAll(lbl, spacer, cb);
                return row;
            }

            CheckBox cb = new CheckBox(opt.getLabel());
            cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
            if (opt.getIndentLevel() > 0) {
                cb.setPadding(new Insets(0, 0, 0, opt.getIndentLevel() * 20));
            }

            boolean val = isStandardIndentOption(opt.getKey())
                    ? getStandardIndentBoolean(opt.getKey())
                    : workingSettings.getBoolean(opt.getKey(), (Boolean) opt.getDefaultValue());
            cb.setSelected(val);

            // Trailing Comma child checkboxes disable state
            if (TRAILING_COMMA_CHILDREN.contains(opt.getKey())) {
                boolean masterEnabled = workingSettings.getBoolean(KotlinCodeStyleSettings.TRAILING_COMMA_ENABLED, false);
                cb.setDisable(!masterEnabled);
            }

            // Code generation enforce on reformat disable state
            if (KotlinCodeStyleSettings.CODE_GEN_ENFORCE_ON_REFORMAT.equals(opt.getKey())) {
                boolean parentEnabled = workingSettings.getBoolean(KotlinCodeStyleSettings.CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, false);
                cb.setDisable(!parentEnabled);
            }

            cb.selectedProperty().addListener((obs, oldV, newV) -> {
                if (!suppressEvents) {
                    if (isStandardIndentOption(opt.getKey())) {
                        setStandardIndentBoolean(opt.getKey(), newV);
                    } else {
                        workingSettings.setBoolean(opt.getKey(), newV);
                    }
                    updateDependentControls(opt.getKey(), newV);
                    updatePreview();
                    notifyModified();
                }
            });

            checkboxControls.put(opt.getKey(), cb);

            // Smart tabs disable binding
            if ("smart_tabs".equals(opt.getKey())) {
                CheckBox useTab = checkboxControls.get("use_tab_character");
                if (useTab != null) {
                    cb.disableProperty().bind(useTab.selectedProperty().not());
                }
            }

            return cb;

        } else if (opt.getType() == CodeStyleOptionType.NUMBER) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);

            Label lbl = new Label(opt.getLabel());
            // Wider label width for long labels
            int minWidth = opt.getLabel().length() > 22 ? 220 : 140;
            lbl.setMinWidth(minWidth);
            lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

            TextField field = new TextField();
            int val = isStandardIndentOption(opt.getKey())
                    ? getStandardIndentInt(opt.getKey())
                    : workingSettings.getInt(opt.getKey(), (Integer) opt.getDefaultValue());
            field.setText(String.valueOf(val));
            field.setPrefWidth(55);
            field.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");

            field.textProperty().addListener((obs, oldV, newV) -> {
                if (!suppressEvents) {
                    try {
                        int intVal = Integer.parseInt(newV.trim());
                        if (isStandardIndentOption(opt.getKey())) {
                            setStandardIndentInt(opt.getKey(), intVal);
                        } else {
                            workingSettings.setInt(opt.getKey(), intVal);
                        }
                    } catch (Exception ignored) {}
                    updatePreview();
                    notifyModified();
                }
            });

            numberControls.put(opt.getKey(), field);
            row.getChildren().addAll(lbl, field);
            return row;

        } else if (opt.getType() == CodeStyleOptionType.COMBO) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);

            Label lbl = new Label(opt.getLabel());
            lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().addAll(opt.getChoices());
            String val = workingSettings.getString(opt.getKey(), (String) opt.getDefaultValue());
            combo.setValue(val);
            styleComboBox(combo);

            if (KotlinCodeStyleSettings.LOAD_SAVE_USE_DEFAULTS_FROM.equals(opt.getKey())) {
                combo.setPrefWidth(220);
                combo.setOnAction(e -> {
                    if (!suppressEvents) {
                        String selected = combo.getValue();
                        workingSettings.setString(opt.getKey(), selected);
                        if ("Kotlin Coding Conventions".equals(selected)) {
                            workingSettings.setContinuationIndent(4);
                        } else if ("<ide defaults>".equals(selected) || "Kotlin obsolete codestyle".equals(selected)) {
                            workingSettings.setContinuationIndent(8);
                        }
                        updatePreview();
                        notifyModified();
                    }
                });
                comboControls.put(opt.getKey(), combo);
                row.getChildren().addAll(lbl, combo);
                return row;
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            combo.setOnAction(e -> {
                if (!suppressEvents) {
                    workingSettings.setString(opt.getKey(), combo.getValue());
                    updatePreview();
                    notifyModified();
                }
            });

            comboControls.put(opt.getKey(), combo);
            row.getChildren().addAll(lbl, spacer, combo);
            return row;

        } else if (opt.getType() == CodeStyleOptionType.TEXT) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);

            Label lbl = new Label(opt.getLabel());
            int minWidth = opt.getLabel().length() > 22 ? 220 : 140;
            lbl.setMinWidth(minWidth);
            lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

            TextField field = new TextField();
            String val = workingSettings.getString(opt.getKey(), (String) opt.getDefaultValue());
            field.setText(val != null ? val : "");
            if (val != null && val.length() > 12) {
                field.setPrefWidth(260);
            } else {
                field.setPrefWidth(120);
            }
            field.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");

            field.textProperty().addListener((obs, oldV, newV) -> {
                if (!suppressEvents) {
                    workingSettings.setString(opt.getKey(), newV);
                    updatePreview();
                    notifyModified();
                }
            });

            textControls.put(opt.getKey(), field);
            row.getChildren().addAll(lbl, field);
            return row;
        }

        return new Region();
    }

    private void styleComboBox(ComboBox<String> combo) {
        combo.setPrefWidth(150);
        combo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
    }

    private boolean isStandardIndentOption(String key) {
        return "use_tab_character".equals(key)
                || "smart_tabs".equals(key)
                || "tab_size".equals(key)
                || "indent".equals(key)
                || "continuation_indent".equals(key)
                || "keep_indents_on_empty_lines".equals(key)
                || "label_indent".equals(key);
    }

    private boolean getStandardIndentBoolean(String key) {
        return switch (key) {
            case "use_tab_character" -> workingSettings.isUseTabCharacter();
            case "smart_tabs" -> workingSettings.isSmartTabs();
            case "keep_indents_on_empty_lines" -> workingSettings.isKeepIndentsOnEmptyLines();
            default -> false;
        };
    }

    private void setStandardIndentBoolean(String key, boolean val) {
        switch (key) {
            case "use_tab_character" -> workingSettings.setUseTabCharacter(val);
            case "smart_tabs" -> workingSettings.setSmartTabs(val);
            case "keep_indents_on_empty_lines" -> workingSettings.setKeepIndentsOnEmptyLines(val);
        }
    }

    private int getStandardIndentInt(String key) {
        return switch (key) {
            case "tab_size" -> workingSettings.getTabSize();
            case "indent" -> workingSettings.getIndent();
            case "continuation_indent" -> workingSettings.getContinuationIndent();
            case "label_indent" -> workingSettings.getLabelIndent();
            default -> 4;
        };
    }

    private void setStandardIndentInt(String key, int val) {
        switch (key) {
            case "tab_size" -> workingSettings.setTabSize(val);
            case "indent" -> workingSettings.setIndent(val);
            case "continuation_indent" -> workingSettings.setContinuationIndent(val);
            case "label_indent" -> workingSettings.setLabelIndent(val);
        }
    }

    private void updateDependentControls(String key, boolean val) {
        if ("use_tab_character".equals(key)) {
            CheckBox smartTabs = checkboxControls.get("smart_tabs");
            if (smartTabs != null) {
                smartTabs.setDisable(!val);
            }
        } else if (KotlinCodeStyleSettings.TRAILING_COMMA_ENABLED.equals(key)) {
            for (String childKey : TRAILING_COMMA_CHILDREN) {
                CheckBox child = checkboxControls.get(childKey);
                if (child != null) {
                    child.setDisable(!val);
                }
            }
        } else if (KotlinCodeStyleSettings.CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START.equals(key)) {
            CheckBox enforce = checkboxControls.get(KotlinCodeStyleSettings.CODE_GEN_ENFORCE_ON_REFORMAT);
            if (enforce != null) {
                enforce.setDisable(!val);
            }
        }
    }

    private void renderStandardTabsAndIndents() {
        CodeStyleGroup g = CodeStyleGroup.flat("Tabs and Indents");
        g.addOption(CodeStyleOption.checkbox("use_tab_character", "Use tab character", false));
        g.addOption(CodeStyleOption.indentedCheckbox("smart_tabs", "Smart tabs", false));
        g.addOption(CodeStyleOption.number("tab_size", "Tab size:", 4));
        g.addOption(CodeStyleOption.number("indent", "Indent:", 4));
        g.addOption(CodeStyleOption.number("continuation_indent", "Continuation indent:", 8));
        g.addOption(CodeStyleOption.checkbox("keep_indents_on_empty_lines", "Keep indents on empty lines", false));
        for (CodeStyleOption opt : g.getOptions()) {
            leftContentBox.getChildren().add(buildOptionNode(opt));
        }
    }

    private void renderImportsTab() {
        topLevelGroup = new ToggleGroup();
        staticsGroup = new ToggleGroup();

        // 1. Top-Level Symbols
        VBox topLevelSection = new VBox(6);
        Label topLevelLabel = new Label("Top-Level Symbols");
        topLevelLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        String currentTopMode = workingSettings.getString(KotlinCodeStyleSettings.TOP_LEVEL_IMPORT_MODE, "WHEN_AT_LEAST");
        int currentTopThreshold = workingSettings.getInt(KotlinCodeStyleSettings.TOP_LEVEL_IMPORT_THRESHOLD, 5);

        RadioButton rbTopSingle = new RadioButton("Use single name import");
        rbTopSingle.setUserData("SINGLE_NAME");
        rbTopSingle.setToggleGroup(topLevelGroup);
        styleRadioButton(rbTopSingle);

        RadioButton rbTopAll = new RadioButton("Use import with '*'");
        rbTopAll.setUserData("ALL");
        rbTopAll.setToggleGroup(topLevelGroup);
        styleRadioButton(rbTopAll);

        HBox rbTopWhenRow = new HBox(8);
        rbTopWhenRow.setAlignment(Pos.CENTER_LEFT);
        RadioButton rbTopWhen = new RadioButton("Use import with '*' when at least");
        rbTopWhen.setUserData("WHEN_AT_LEAST");
        rbTopWhen.setToggleGroup(topLevelGroup);
        styleRadioButton(rbTopWhen);

        topLevelThresholdField = new TextField(String.valueOf(currentTopThreshold));
        topLevelThresholdField.setPrefWidth(50);
        styleNumberField(topLevelThresholdField);
        topLevelThresholdField.textProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                try {
                    int val = Integer.parseInt(newV.trim());
                    workingSettings.setInt(KotlinCodeStyleSettings.TOP_LEVEL_IMPORT_THRESHOLD, val);
                    notifyModified();
                } catch (Exception ignored) {}
            }
        });

        Label topLevelSuffix = new Label("names used");
        topLevelSuffix.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        rbTopWhenRow.getChildren().addAll(rbTopWhen, topLevelThresholdField, topLevelSuffix);

        if ("SINGLE_NAME".equals(currentTopMode)) rbTopSingle.setSelected(true);
        else if ("ALL".equals(currentTopMode)) rbTopAll.setSelected(true);
        else rbTopWhen.setSelected(true);

        topLevelThresholdField.setDisable(!rbTopWhen.isSelected());
        topLevelGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            boolean isWhen = newT == rbTopWhen;
            topLevelThresholdField.setDisable(!isWhen);
            if (!suppressEvents && newT != null && newT.getUserData() != null) {
                workingSettings.setString(KotlinCodeStyleSettings.TOP_LEVEL_IMPORT_MODE, newT.getUserData().toString());
                notifyModified();
            }
        });

        topLevelSection.getChildren().addAll(topLevelLabel, rbTopSingle, rbTopAll, rbTopWhenRow);
        leftContentBox.getChildren().add(topLevelSection);

        // 2. Java Statics and Enum Members
        VBox staticsSection = new VBox(6);
        staticsSection.setPadding(new Insets(6, 0, 0, 0));
        Label staticsLabel = new Label("Java Statics and Enum Members");
        staticsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        String currentStaticMode = workingSettings.getString(KotlinCodeStyleSettings.JAVA_STATICS_IMPORT_MODE, "WHEN_AT_LEAST");
        int currentStaticThreshold = workingSettings.getInt(KotlinCodeStyleSettings.JAVA_STATICS_IMPORT_THRESHOLD, 3);

        RadioButton rbStatSingle = new RadioButton("Use single name import");
        rbStatSingle.setUserData("SINGLE_NAME");
        rbStatSingle.setToggleGroup(staticsGroup);
        styleRadioButton(rbStatSingle);

        RadioButton rbStatAll = new RadioButton("Use import with '*'");
        rbStatAll.setUserData("ALL");
        rbStatAll.setToggleGroup(staticsGroup);
        styleRadioButton(rbStatAll);

        HBox rbStatWhenRow = new HBox(8);
        rbStatWhenRow.setAlignment(Pos.CENTER_LEFT);
        RadioButton rbStatWhen = new RadioButton("Use import with '*' when at least");
        rbStatWhen.setUserData("WHEN_AT_LEAST");
        rbStatWhen.setToggleGroup(staticsGroup);
        styleRadioButton(rbStatWhen);

        staticsThresholdField = new TextField(String.valueOf(currentStaticThreshold));
        staticsThresholdField.setPrefWidth(50);
        styleNumberField(staticsThresholdField);
        staticsThresholdField.textProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                try {
                    int val = Integer.parseInt(newV.trim());
                    workingSettings.setInt(KotlinCodeStyleSettings.JAVA_STATICS_IMPORT_THRESHOLD, val);
                    notifyModified();
                } catch (Exception ignored) {}
            }
        });

        Label staticsSuffix = new Label("names used");
        staticsSuffix.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        rbStatWhenRow.getChildren().addAll(rbStatWhen, staticsThresholdField, staticsSuffix);

        if ("SINGLE_NAME".equals(currentStaticMode)) rbStatSingle.setSelected(true);
        else if ("ALL".equals(currentStaticMode)) rbStatAll.setSelected(true);
        else rbStatWhen.setSelected(true);

        staticsThresholdField.setDisable(!rbStatWhen.isSelected());
        staticsGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            boolean isWhen = newT == rbStatWhen;
            staticsThresholdField.setDisable(!isWhen);
            if (!suppressEvents && newT != null && newT.getUserData() != null) {
                workingSettings.setString(KotlinCodeStyleSettings.JAVA_STATICS_IMPORT_MODE, newT.getUserData().toString());
                notifyModified();
            }
        });

        staticsSection.getChildren().addAll(staticsLabel, rbStatSingle, rbStatAll, rbStatWhenRow);
        leftContentBox.getChildren().add(staticsSection);

        // 3. Other
        VBox otherSection = new VBox(6);
        otherSection.setPadding(new Insets(6, 0, 0, 0));
        HBox otherHeader = createDividerHeader("Other");
        nestedClassesCheckBox = new CheckBox("Insert imports for nested classes");
        nestedClassesCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nestedClassesCheckBox.setSelected(workingSettings.getBoolean(KotlinCodeStyleSettings.INSERT_IMPORTS_FOR_NESTED_CLASSES, false));
        nestedClassesCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(KotlinCodeStyleSettings.INSERT_IMPORTS_FOR_NESTED_CLASSES, newV);
                notifyModified();
            }
        });
        otherSection.getChildren().addAll(otherHeader, nestedClassesCheckBox);
        leftContentBox.getChildren().add(otherSection);

        // 4. Packages to Use Import with '*'
        VBox packagesSection = new VBox(6);
        packagesSection.setPadding(new Insets(6, 0, 0, 0));
        HBox packagesHeader = createDividerHeader("Packages to Use Import with '*'");

        HBox packagesToolbar = new HBox(4);
        Button addPkgBtn = createToolbarButton("+");
        Button remPkgBtn = createToolbarButton("−");
        packagesToolbar.getChildren().addAll(addPkgBtn, remPkgBtn);

        packagesOnDemandTable = createImportTable(false);
        if (workingSettings instanceof KotlinCodeStyleSettings kSettings) {
            packagesOnDemandTable.getItems().setAll(kSettings.getPackagesToUseImportOnDemand());
        }

        addPkgBtn.setOnAction(e -> {
            KotlinCodeStyleSettings.ImportEntry newEntry = new KotlinCodeStyleSettings.ImportEntry("import com.example.*", false);
            packagesOnDemandTable.getItems().add(newEntry);
            packagesOnDemandTable.getSelectionModel().select(newEntry);
            notifyModified();
        });
        remPkgBtn.setOnAction(e -> {
            KotlinCodeStyleSettings.ImportEntry sel = packagesOnDemandTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                packagesOnDemandTable.getItems().remove(sel);
                notifyModified();
            }
        });

        packagesSection.getChildren().addAll(packagesHeader, packagesToolbar, packagesOnDemandTable);
        leftContentBox.getChildren().add(packagesSection);

        // 5. Import Layout
        VBox layoutSection = new VBox(6);
        layoutSection.setPadding(new Insets(6, 0, 0, 0));
        HBox layoutHeader = createDividerHeader("Import Layout");

        importAliasesCheckBox = new CheckBox("Import aliases separately");
        importAliasesCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        importAliasesCheckBox.setSelected(workingSettings.getBoolean(KotlinCodeStyleSettings.IMPORT_ALIASES_SEPARATELY, true));
        importAliasesCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(KotlinCodeStyleSettings.IMPORT_ALIASES_SEPARATELY, newV);
                notifyModified();
            }
        });

        HBox layoutToolbar = new HBox(4);
        Button addLayoutBtn = createToolbarButton("+");
        Button remLayoutBtn = createToolbarButton("−");
        Button upLayoutBtn = createToolbarButton("↑");
        Button downLayoutBtn = createToolbarButton("↓");
        layoutToolbar.getChildren().addAll(addLayoutBtn, remLayoutBtn, upLayoutBtn, downLayoutBtn);

        importLayoutTable = createImportTable(true);
        if (workingSettings instanceof KotlinCodeStyleSettings kSettings) {
            importLayoutTable.getItems().setAll(kSettings.getImportLayout());
        }

        addLayoutBtn.setOnAction(e -> {
            KotlinCodeStyleSettings.ImportEntry newEntry = new KotlinCodeStyleSettings.ImportEntry("import java.*", true);
            importLayoutTable.getItems().add(newEntry);
            importLayoutTable.getSelectionModel().select(newEntry);
            notifyModified();
        });
        remLayoutBtn.setOnAction(e -> {
            KotlinCodeStyleSettings.ImportEntry sel = importLayoutTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                importLayoutTable.getItems().remove(sel);
                notifyModified();
            }
        });
        upLayoutBtn.setOnAction(e -> {
            int idx = importLayoutTable.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                KotlinCodeStyleSettings.ImportEntry item = importLayoutTable.getItems().remove(idx);
                importLayoutTable.getItems().add(idx - 1, item);
                importLayoutTable.getSelectionModel().select(idx - 1);
                notifyModified();
            }
        });
        downLayoutBtn.setOnAction(e -> {
            int idx = importLayoutTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < importLayoutTable.getItems().size() - 1) {
                KotlinCodeStyleSettings.ImportEntry item = importLayoutTable.getItems().remove(idx);
                importLayoutTable.getItems().add(idx + 1, item);
                importLayoutTable.getSelectionModel().select(idx + 1);
                notifyModified();
            }
        });

        layoutSection.getChildren().addAll(layoutHeader, importAliasesCheckBox, layoutToolbar, importLayoutTable);
        leftContentBox.getChildren().add(layoutSection);
    }

    private TableView<KotlinCodeStyleSettings.ImportEntry> createImportTable(boolean isLayoutTable) {
        TableView<KotlinCodeStyleSettings.ImportEntry> table = new TableView<>();
        table.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-control-inner-background-alt: #1E1F22; -fx-background: #1E1F22; -fx-selection-bar: #35538F; -fx-selection-bar-non-focused: #35538F; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        table.setPrefHeight(115);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<KotlinCodeStyleSettings.ImportEntry, String> pkgCol = new TableColumn<>("Package");
        pkgCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPackageName()));
        pkgCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    TextFlow flow = new TextFlow();
                    if (item.startsWith("import ")) {
                        Text kw = new Text("import ");
                        kw.setFill(Color.web("#CF8E6D"));
                        kw.setFont(Font.font("monospace", 12));
                        Text rest = new Text(item.substring(7));
                        rest.setFill(Color.web("#DFE1E5"));
                        rest.setFont(Font.font("monospace", 12));
                        flow.getChildren().addAll(kw, rest);
                    } else {
                        Text t = new Text(item);
                        t.setFill(Color.web("#DFE1E5"));
                        t.setFont(Font.font("monospace", 12));
                        flow.getChildren().add(t);
                    }
                    setGraphic(flow);
                    setText(null);
                }
            }
        });

        TableColumn<KotlinCodeStyleSettings.ImportEntry, Boolean> subCol = new TableColumn<>("With Subpackag...");
        subCol.setPrefWidth(140);
        subCol.setMaxWidth(160);
        subCol.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().isWithSubpackages()));
        subCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    KotlinCodeStyleSettings.ImportEntry entry = getTableView().getItems().get(getIndex());
                    entry.setWithSubpackages(cb.isSelected());
                    notifyModified();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    KotlinCodeStyleSettings.ImportEntry entry = getTableView().getItems().get(getIndex());
                    if (isLayoutTable && entry != null && entry.getPackageName().contains("alias")) {
                        setGraphic(null);
                    } else {
                        cb.setSelected(item != null && item);
                        setGraphic(cb);
                    }
                }
            }
        });

        table.getColumns().addAll(pkgCol, subCol);
        return table;
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleNumberField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");
    }

    private HBox createDividerHeader(String title) {
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label sectionLabel = new Label(title);
        sectionLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-font-weight: bold;");
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-border-color: transparent;");
        HBox.setHgrow(sep, Priority.ALWAYS);
        headerRow.getChildren().addAll(sectionLabel, sep);
        return headerRow;
    }

    private Button createToolbarButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 2 10 2 10; -fx-font-size: 12px; -fx-cursor: hand;");
        return btn;
    }

    private void renderArrangementTab() {
        if (workingSettings instanceof JavaScriptCodeStyleSettings jsSettings) {
            VBox groupingSection = new VBox(6);
            groupingSection.setPadding(new Insets(4, 0, 8, 4));

            HBox groupHeader = createDividerHeader("Grouping rules:");
            groupingSection.getChildren().add(groupHeader);

            VBox groupingOptions = new VBox(8);
            groupingOptions.setPadding(new Insets(4, 0, 4, 4));

            CheckBox groupPropCb = new CheckBox("Group property field with corresponding getter/setter");
            groupPropCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
            groupPropCb.setSelected(jsSettings.isGroupPropertyFieldWithGetterSetter());
            groupPropCb.selectedProperty().addListener((obs, o, n) -> {
                if (!suppressEvents) {
                    jsSettings.setGroupPropertyFieldWithGetterSetter(n);
                    notifyModified();
                }
            });

            CheckBox groupArrowCb = new CheckBox("Group fields initialized with arrow functions with methods");
            groupArrowCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
            groupArrowCb.setSelected(jsSettings.isGroupFieldsWithArrowFunctions());
            groupArrowCb.selectedProperty().addListener((obs, o, n) -> {
                if (!suppressEvents) {
                    jsSettings.setGroupFieldsWithArrowFunctions(n);
                    notifyModified();
                }
            });

            HBox overriddenRow = new HBox(8);
            overriddenRow.setAlignment(Pos.CENTER_LEFT);
            CheckBox overriddenCb = new CheckBox("Keep overridden methods together");
            overriddenCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
            overriddenCb.setSelected(jsSettings.isKeepOverriddenMethodsTogether());

            ComboBox<String> overriddenOrderCombo = new ComboBox<>();
            overriddenOrderCombo.getItems().addAll(JavaScriptCodeStyleSettings.OVERRIDDEN_METHODS_ORDER_OPTIONS);
            overriddenOrderCombo.setValue(jsSettings.getOverriddenMethodsOrder());
            styleComboBox(overriddenOrderCombo);
            overriddenOrderCombo.setPrefWidth(120);
            overriddenOrderCombo.setDisable(!jsSettings.isKeepOverriddenMethodsTogether());

            overriddenCb.selectedProperty().addListener((obs, o, n) -> {
                if (!suppressEvents) {
                    jsSettings.setKeepOverriddenMethodsTogether(n);
                    overriddenOrderCombo.setDisable(!n);
                    notifyModified();
                }
            });
            overriddenOrderCombo.setOnAction(e -> {
                if (!suppressEvents) {
                    jsSettings.setOverriddenMethodsOrder(overriddenOrderCombo.getValue());
                    notifyModified();
                }
            });

            overriddenRow.getChildren().addAll(overriddenCb, overriddenOrderCombo);
            groupingOptions.getChildren().addAll(groupPropCb, groupArrowCb, overriddenRow);
            groupingSection.getChildren().add(groupingOptions);

            leftContentBox.getChildren().add(groupingSection);
        }

        VBox sectionBox = new VBox(8);
        sectionBox.setPadding(new Insets(4, 0, 8, 4));

        Label sectionLabel = new Label("Matching rules:");
        sectionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = createToolbarButton("+");
        Button addStaticBtn = createToolbarButton("+s");
        Button removeBtn = createToolbarButton("−");
        Button upBtn = createToolbarButton("↑");
        Button downBtn = createToolbarButton("↓");
        toolbar.getChildren().addAll(addBtn, addStaticBtn, removeBtn, upBtn, downBtn);

        VBox rulesContainer = new VBox(4);
        rulesContainer.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
        rulesContainer.setMinHeight(160);

        List<String> rulesList;
        if (workingSettings instanceof AngularHtmlCodeStyleSettings aSettings) {
            rulesList = aSettings.getMatchingRules();
        } else if (workingSettings instanceof HtmlCodeStyleSettings hSettings) {
            rulesList = hSettings.getMatchingRules();
        } else if (workingSettings instanceof JavaScriptCodeStyleSettings jsSettings) {
            rulesList = jsSettings.getMatchingRules();
        } else {
            rulesList = new ArrayList<>();
        }

        final int[] selectedIndex = new int[]{-1};
        Runnable rebuildRulesView = new Runnable() {
            @Override
            public void run() {
                rulesContainer.getChildren().clear();
                for (int i = 0; i < rulesList.size(); i++) {
                    final int idx = i;
                    String ruleText = rulesList.get(i);

                    HBox ruleRow = new HBox(8);
                    ruleRow.setAlignment(Pos.CENTER_LEFT);
                    ruleRow.setPadding(new Insets(4, 8, 4, 8));
                    ruleRow.setStyle(idx == selectedIndex[0]
                            ? "-fx-background-color: #2E436E; -fx-background-radius: 4; -fx-cursor: hand;"
                            : "-fx-background-color: transparent; -fx-background-radius: 4; -fx-cursor: hand;");

                    // Circle badge with index: (1)
                    Label circleBadge = new Label(String.valueOf(idx + 1));
                    circleBadge.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 18px; -fx-min-height: 18px; -fx-max-width: 18px; -fx-max-height: 18px; -fx-alignment: center; -fx-background-radius: 9px;");

                    // Sort arrow ⇅
                    Label sortIcon = new Label("⇅");
                    sortIcon.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");

                    ruleRow.getChildren().addAll(circleBadge, sortIcon);

                    // Pill badges (split by comma for multiple tokens, e.g. "field, static" -> [field] [static])
                    String[] tokens = ruleText.split(",\\s*");
                    for (String token : tokens) {
                        Label pillBadge = new Label(token.trim());
                        pillBadge.setStyle("-fx-background-color: #35538F; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 2 10 2 10; -fx-background-radius: 12px;");
                        ruleRow.getChildren().add(pillBadge);
                    }

                    ruleRow.setOnMouseClicked(e -> {
                        selectedIndex[0] = idx;
                        run();
                    });

                    rulesContainer.getChildren().add(ruleRow);
                }
            }
        };

        rebuildRulesView.run();

        addBtn.setOnAction(e -> {
            String newRule = "JavaScript".equals(languageId) ? "method" : "attribute";
            rulesList.add(newRule);
            selectedIndex[0] = rulesList.size() - 1;
            rebuildRulesView.run();
            notifyModified();
        });

        addStaticBtn.setOnAction(e -> {
            String newRule = "JavaScript".equals(languageId) ? "method, static" : "static, attribute";
            rulesList.add(newRule);
            selectedIndex[0] = rulesList.size() - 1;
            rebuildRulesView.run();
            notifyModified();
        });

        removeBtn.setOnAction(e -> {
            if (selectedIndex[0] >= 0 && selectedIndex[0] < rulesList.size()) {
                rulesList.remove(selectedIndex[0]);
                if (selectedIndex[0] >= rulesList.size()) {
                    selectedIndex[0] = rulesList.size() - 1;
                }
                rebuildRulesView.run();
                notifyModified();
            }
        });

        upBtn.setOnAction(e -> {
            if (selectedIndex[0] > 0 && selectedIndex[0] < rulesList.size()) {
                String item = rulesList.remove(selectedIndex[0]);
                selectedIndex[0]--;
                rulesList.add(selectedIndex[0], item);
                rebuildRulesView.run();
                notifyModified();
            }
        });

        downBtn.setOnAction(e -> {
            if (selectedIndex[0] >= 0 && selectedIndex[0] < rulesList.size() - 1) {
                String item = rulesList.remove(selectedIndex[0]);
                selectedIndex[0]++;
                rulesList.add(selectedIndex[0], item);
                rebuildRulesView.run();
                notifyModified();
            }
        });

        sectionBox.getChildren().addAll(sectionLabel, toolbar, rulesContainer);
        leftContentBox.getChildren().add(sectionBox);
    }

    private void renderJavaScriptPunctuationTab() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(8, 0, 8, 4));

        // Row 1: [Use v] semicolon [in code generated by IDE v]
        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> semiCombo = new ComboBox<>();
        semiCombo.getItems().addAll(JavaScriptCodeStyleSettings.USE_SEMICOLON_OPTIONS);
        semiCombo.setValue(workingSettings.getString(JavaScriptCodeStyleSettings.USE_SEMICOLON, "Use"));
        styleComboBox(semiCombo);
        semiCombo.setPrefWidth(110);
        semiCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(JavaScriptCodeStyleSettings.USE_SEMICOLON, semiCombo.getValue());
                updatePreview();
                notifyModified();
            }
        });

        Label semiLabel = new Label("semicolon");
        semiLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ComboBox<String> semiScopeCombo = new ComboBox<>();
        semiScopeCombo.getItems().addAll(JavaScriptCodeStyleSettings.SEMICOLON_SCOPE_OPTIONS);
        semiScopeCombo.setValue(workingSettings.getString(JavaScriptCodeStyleSettings.SEMICOLON_SCOPE, "in code generated by IDE"));
        styleComboBox(semiScopeCombo);
        semiScopeCombo.setPrefWidth(190);
        semiScopeCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(JavaScriptCodeStyleSettings.SEMICOLON_SCOPE, semiScopeCombo.getValue());
                updatePreview();
                notifyModified();
            }
        });

        row1.getChildren().addAll(semiCombo, semiLabel, semiScopeCombo);

        // Row 2: Use [double v] quotes [in code generated by IDE v]
        HBox row2 = new HBox(8);
        row2.setAlignment(Pos.CENTER_LEFT);

        Label useQuoteLabel = new Label("Use");
        useQuoteLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ComboBox<String> quoteCombo = new ComboBox<>();
        quoteCombo.getItems().addAll(JavaScriptCodeStyleSettings.QUOTE_STYLE_OPTIONS);
        quoteCombo.setValue(workingSettings.getString(JavaScriptCodeStyleSettings.QUOTE_STYLE, "double"));
        styleComboBox(quoteCombo);
        quoteCombo.setPrefWidth(110);
        quoteCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(JavaScriptCodeStyleSettings.QUOTE_STYLE, quoteCombo.getValue());
                updatePreview();
                notifyModified();
            }
        });

        Label quotesLabel = new Label("quotes");
        quotesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ComboBox<String> quoteScopeCombo = new ComboBox<>();
        quoteScopeCombo.getItems().addAll(JavaScriptCodeStyleSettings.QUOTE_SCOPE_OPTIONS);
        quoteScopeCombo.setValue(workingSettings.getString(JavaScriptCodeStyleSettings.QUOTE_SCOPE, "in code generated by IDE"));
        styleComboBox(quoteScopeCombo);
        quoteScopeCombo.setPrefWidth(190);
        quoteScopeCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(JavaScriptCodeStyleSettings.QUOTE_SCOPE, quoteScopeCombo.getValue());
                updatePreview();
                notifyModified();
            }
        });

        row2.getChildren().addAll(useQuoteLabel, quoteCombo, quotesLabel, quoteScopeCombo);

        // Row 3: Trailing comma: [Keep v]
        HBox row3 = new HBox(8);
        row3.setAlignment(Pos.CENTER_LEFT);

        Label commaLabel = new Label("Trailing comma:");
        commaLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ComboBox<String> commaCombo = new ComboBox<>();
        commaCombo.getItems().addAll(JavaScriptCodeStyleSettings.TRAILING_COMMA_OPTIONS);
        commaCombo.setValue(workingSettings.getString(JavaScriptCodeStyleSettings.TRAILING_COMMA, "Keep"));
        styleComboBox(commaCombo);
        commaCombo.setPrefWidth(170);
        commaCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(JavaScriptCodeStyleSettings.TRAILING_COMMA, commaCombo.getValue());
                updatePreview();
                notifyModified();
            }
        });

        row3.getChildren().addAll(commaLabel, commaCombo);

        comboControls.put(JavaScriptCodeStyleSettings.USE_SEMICOLON, semiCombo);
        comboControls.put(JavaScriptCodeStyleSettings.SEMICOLON_SCOPE, semiScopeCombo);
        comboControls.put(JavaScriptCodeStyleSettings.QUOTE_STYLE, quoteCombo);
        comboControls.put(JavaScriptCodeStyleSettings.QUOTE_SCOPE, quoteScopeCombo);
        comboControls.put(JavaScriptCodeStyleSettings.TRAILING_COMMA, commaCombo);

        box.getChildren().addAll(row1, row2, row3);
        leftContentBox.getChildren().add(box);
    }

    private void renderGoImportsTab() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(2, 0, 6, 0));

        goBackquotesCb = new CheckBox("Use backquotes for imports");
        goBackquotesCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        goBackquotesCb.setSelected(workingSettings.getBoolean(GoCodeStyleSettings.IMPORTS_USE_BACKQUOTES, false));
        goBackquotesCb.selectedProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_USE_BACKQUOTES, n);
                updatePreview();
                notifyModified();
            }
        });

        goSingleImportParensCb = new CheckBox("Add parentheses for a single import");
        goSingleImportParensCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        goSingleImportParensCb.setSelected(workingSettings.getBoolean(GoCodeStyleSettings.IMPORTS_ADD_PARENTHESES_SINGLE, false));
        goSingleImportParensCb.selectedProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_ADD_PARENTHESES_SINGLE, n);
                updatePreview();
                notifyModified();
            }
        });

        goRemoveRedundantCb = new CheckBox("Remove redundant import aliases");
        goRemoveRedundantCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        goRemoveRedundantCb.setSelected(workingSettings.getBoolean(GoCodeStyleSettings.IMPORTS_REMOVE_REDUNDANT_ALIASES, false));
        goRemoveRedundantCb.selectedProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_REMOVE_REDUNDANT_ALIASES, n);
                updatePreview();
                notifyModified();
            }
        });

        HBox sortingRow = new HBox(12);
        sortingRow.setAlignment(Pos.CENTER_LEFT);
        Label sortingLabel = new Label("Sorting type");
        sortingLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        goSortingCombo = new ComboBox<>();
        goSortingCombo.getItems().addAll("goimports", "gofmt", "none");
        goSortingCombo.setValue(workingSettings.getString(GoCodeStyleSettings.IMPORTS_SORTING_TYPE, "goimports"));
        styleComboBox(goSortingCombo);
        goSortingCombo.setOnAction(e -> {
            if (!suppressEvents && goSortingCombo.getValue() != null) {
                workingSettings.setString(GoCodeStyleSettings.IMPORTS_SORTING_TYPE, goSortingCombo.getValue());
                updatePreview();
                notifyModified();
            }
        });
        sortingRow.getChildren().addAll(sortingLabel, goSortingCombo);

        goMoveAllSingleDeclCb = new CheckBox("Move all imports to a single declaration");
        goMoveAllSingleDeclCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        goMoveAllSingleDeclCb.setSelected(workingSettings.getBoolean(GoCodeStyleSettings.IMPORTS_MOVE_ALL_SINGLE_DECLARATION, false));
        goMoveAllSingleDeclCb.selectedProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_MOVE_ALL_SINGLE_DECLARATION, n);
                updatePreview();
                notifyModified();
            }
        });

        goGroupSdkCb = new CheckBox("Group packages from Go SDK");
        goGroupSdkCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        goGroupSdkCb.setSelected(workingSettings.getBoolean(GoCodeStyleSettings.IMPORTS_GROUP_SDK_PACKAGES, false));
        goGroupSdkCb.selectedProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_GROUP_SDK_PACKAGES, n);
                updatePreview();
                notifyModified();
            }
        });

        goMoveAllSingleGroupCb = new CheckBox("Move all packages to a single group");
        goMoveAllSingleGroupCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        goMoveAllSingleGroupCb.setPadding(new Insets(0, 0, 0, 20));
        goMoveAllSingleGroupCb.setSelected(workingSettings.getBoolean(GoCodeStyleSettings.IMPORTS_MOVE_ALL_SINGLE_GROUP, false));
        goMoveAllSingleGroupCb.selectedProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_MOVE_ALL_SINGLE_GROUP, n);
                updatePreview();
                notifyModified();
            }
        });

        goGroupCb = new CheckBox("Group");
        goGroupCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        boolean groupEnabled = workingSettings.getBoolean(GoCodeStyleSettings.IMPORTS_GROUP_ENABLED, false);
        goGroupCb.setSelected(groupEnabled);

        VBox groupSubBox = new VBox(6);
        groupSubBox.setPadding(new Insets(0, 0, 0, 20));

        goGroupModeGroup = new ToggleGroup();
        goRbProject = new RadioButton("Current project packages");
        goRbProject.setUserData("PROJECT");
        goRbProject.setToggleGroup(goGroupModeGroup);
        styleRadioButton(goRbProject);

        goRbPrefixes = new RadioButton("Imports starting with:");
        goRbPrefixes.setUserData("PREFIXES");
        goRbPrefixes.setToggleGroup(goGroupModeGroup);
        styleRadioButton(goRbPrefixes);

        String mode = workingSettings.getString(GoCodeStyleSettings.IMPORTS_GROUP_MODE, "PROJECT");
        if ("PREFIXES".equals(mode)) {
            goRbPrefixes.setSelected(true);
        } else {
            goRbProject.setSelected(true);
        }

        goPrefixesArea = new TextArea(workingSettings.getString(GoCodeStyleSettings.IMPORTS_CUSTOM_PREFIXES, ""));
        goPrefixesArea.setPrefRowCount(3);
        goPrefixesArea.setPrefHeight(65);
        goPrefixesArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-family: monospace; -fx-font-size: 12px;");
        goPrefixesArea.textProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                workingSettings.setString(GoCodeStyleSettings.IMPORTS_CUSTOM_PREFIXES, n);
                notifyModified();
            }
        });

        Label prefixesHint = new Label("Comma-separated list of prefixes, same as 'goimports -local'");
        prefixesHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

        groupSubBox.getChildren().addAll(goRbProject, goRbPrefixes, goPrefixesArea, prefixesHint);

        groupSubBox.disableProperty().bind(goGroupCb.selectedProperty().not());
        goPrefixesArea.disableProperty().bind(goGroupCb.selectedProperty().not().or(goRbPrefixes.selectedProperty().not()));

        goGroupCb.selectedProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_GROUP_ENABLED, n);
                notifyModified();
            }
        });

        goGroupModeGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (!suppressEvents && n != null && n.getUserData() != null) {
                workingSettings.setString(GoCodeStyleSettings.IMPORTS_GROUP_MODE, n.getUserData().toString());
                notifyModified();
            }
        });

        box.getChildren().addAll(
                goBackquotesCb,
                goSingleImportParensCb,
                goRemoveRedundantCb,
                sortingRow,
                goMoveAllSingleDeclCb,
                goGroupSdkCb,
                goMoveAllSingleGroupCb,
                goGroupCb,
                groupSubBox
        );

        leftContentBox.getChildren().add(box);
    }

    private void renderGoOtherTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(2, 0, 6, 0));

        goLeadingSpaceCb = new CheckBox("Add a leading space to comments");
        goLeadingSpaceCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        boolean leadingSpace = workingSettings.getBoolean(GoCodeStyleSettings.OTHER_ADD_LEADING_SPACE_COMMENTS, false);
        goLeadingSpaceCb.setSelected(leadingSpace);

        VBox exceptionsBox = new VBox(6);
        exceptionsBox.setPadding(new Insets(0, 0, 0, 20));

        Label exceptLabel = new Label("Except for comments starting with:");
        exceptLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Button addBtn = createToolbarButton("+");
        Button removeBtn = createToolbarButton("−");
        Button editBtn = createToolbarButton("✎");
        toolbar.getChildren().addAll(addBtn, removeBtn, editBtn);

        VBox listContainer = new VBox(4);
        listContainer.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
        listContainer.setMinHeight(120);

        List<String> exceptionsList;
        if (workingSettings instanceof GoCodeStyleSettings gSettings) {
            exceptionsList = gSettings.getCommentExceptions();
        } else {
            exceptionsList = new ArrayList<>();
        }

        final int[] selectedIdx = new int[]{-1};
        Runnable rebuildExceptionsView = new Runnable() {
            @Override
            public void run() {
                listContainer.getChildren().clear();
                if (exceptionsList.isEmpty()) {
                    Label noExceptions = new Label("No exceptions");
                    noExceptions.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px; -fx-padding: 30 0 0 0;");
                    HBox centerBox = new HBox(noExceptions);
                    centerBox.setAlignment(Pos.CENTER);
                    listContainer.getChildren().add(centerBox);
                } else {
                    for (int i = 0; i < exceptionsList.size(); i++) {
                        final int idx = i;
                        String item = exceptionsList.get(i);
                        HBox row = new HBox(8);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setPadding(new Insets(3, 8, 3, 8));
                        row.setStyle(idx == selectedIdx[0]
                                ? "-fx-background-color: #2E436E; -fx-background-radius: 4; -fx-cursor: hand;"
                                : "-fx-background-color: transparent; -fx-background-radius: 4; -fx-cursor: hand;");
                        Label textLbl = new Label(item);
                        textLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-family: monospace; -fx-font-size: 12px;");
                        row.getChildren().add(textLbl);
                        row.setOnMouseClicked(e -> {
                            selectedIdx[0] = idx;
                            run();
                        });
                        listContainer.getChildren().add(row);
                    }
                }
            }
        };

        rebuildExceptionsView.run();

        addBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("//");
            dialog.setTitle("Add Comment Prefix Exception");
            dialog.setHeaderText("Enter comment prefix exception:");
            dialog.showAndWait().ifPresent(text -> {
                if (!text.isBlank()) {
                    exceptionsList.add(text.trim());
                    selectedIdx[0] = exceptionsList.size() - 1;
                    rebuildExceptionsView.run();
                    notifyModified();
                }
            });
        });

        removeBtn.setOnAction(e -> {
            if (selectedIdx[0] >= 0 && selectedIdx[0] < exceptionsList.size()) {
                exceptionsList.remove(selectedIdx[0]);
                if (selectedIdx[0] >= exceptionsList.size()) {
                    selectedIdx[0] = exceptionsList.size() - 1;
                }
                rebuildExceptionsView.run();
                notifyModified();
            }
        });

        editBtn.setOnAction(e -> {
            if (selectedIdx[0] >= 0 && selectedIdx[0] < exceptionsList.size()) {
                String current = exceptionsList.get(selectedIdx[0]);
                TextInputDialog dialog = new TextInputDialog(current);
                dialog.setTitle("Edit Comment Prefix Exception");
                dialog.setHeaderText("Edit comment prefix exception:");
                dialog.showAndWait().ifPresent(text -> {
                    if (!text.isBlank()) {
                        exceptionsList.set(selectedIdx[0], text.trim());
                        rebuildExceptionsView.run();
                        notifyModified();
                    }
                });
            }
        });

        exceptionsBox.getChildren().addAll(exceptLabel, toolbar, listContainer);
        exceptionsBox.disableProperty().bind(goLeadingSpaceCb.selectedProperty().not());

        goLeadingSpaceCb.selectedProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GoCodeStyleSettings.OTHER_ADD_LEADING_SPACE_COMMENTS, n);
                updatePreview();
                notifyModified();
            }
        });

        // Column width for Fill paragraph
        HBox colWidthRow = new HBox(12);
        colWidthRow.setAlignment(Pos.CENTER_LEFT);
        Label colWidthLabel = new Label("Column width for Fill paragraph:");
        colWidthLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        goColWidthField = new TextField(String.valueOf(workingSettings.getInt(GoCodeStyleSettings.OTHER_COLUMN_WIDTH_FILL_PARAGRAPH, 80)));
        goColWidthField.setPrefWidth(55);
        styleNumberField(goColWidthField);
        goColWidthField.textProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                try {
                    int val = Integer.parseInt(n.trim());
                    workingSettings.setInt(GoCodeStyleSettings.OTHER_COLUMN_WIDTH_FILL_PARAGRAPH, val);
                    notifyModified();
                } catch (Exception ignored) {}
            }
        });
        colWidthRow.getChildren().addAll(colWidthLabel, goColWidthField);

        // Run gofmt section
        HBox runGofmtHeader = createDividerHeader("Run gofmt");

        HBox runGofmtRow = new HBox(12);
        runGofmtRow.setAlignment(Pos.CENTER_LEFT);
        goRunGofmtCb = new CheckBox("On Reformat Code action");
        goRunGofmtCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        goRunGofmtCb.setSelected(workingSettings.getBoolean(GoCodeStyleSettings.OTHER_RUN_GOFMT_ON_REFORMAT, true));
        goRunGofmtCb.selectedProperty().addListener((obs, o, n) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GoCodeStyleSettings.OTHER_RUN_GOFMT_ON_REFORMAT, n);
                notifyModified();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label shortcutLabel = new Label("Ctrl+Alt+L");
        shortcutLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        runGofmtRow.getChildren().addAll(goRunGofmtCb, spacer, shortcutLabel);

        box.getChildren().addAll(
                goLeadingSpaceCb,
                exceptionsBox,
                colWidthRow,
                runGofmtHeader,
                runGofmtRow
        );

        leftContentBox.getChildren().add(box);
    }

    private void renderGroovyImportsTab() {
        GroovyCodeStyleSettings gSettings = workingSettings instanceof GroovyCodeStyleSettings gs ? gs : null;

        VBox box = new VBox(12);
        box.setPadding(new Insets(2, 0, 10, 0));

        // 1. General
        VBox generalBox = new VBox(6);
        HBox generalHeader = createDividerHeader("General");

        groovyUseSingleClassImportCb = new CheckBox("Use single class import");
        groovyUseSingleClassImportCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyUseSingleClassImportCb.setSelected(workingSettings.getBoolean(GroovyCodeStyleSettings.IMPORTS_USE_SINGLE_CLASS_IMPORT, true));
        groovyUseSingleClassImportCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GroovyCodeStyleSettings.IMPORTS_USE_SINGLE_CLASS_IMPORT, newV);
                notifyModified();
            }
        });

        groovyUseFqClassNamesCb = new CheckBox("Use fully qualified class names");
        groovyUseFqClassNamesCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyUseFqClassNamesCb.setSelected(workingSettings.getBoolean(GroovyCodeStyleSettings.IMPORTS_USE_FQ_CLASS_NAMES, false));
        groovyUseFqClassNamesCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GroovyCodeStyleSettings.IMPORTS_USE_FQ_CLASS_NAMES, newV);
                notifyModified();
            }
        });

        groovyInsertForInnerClassesCb = new CheckBox("Insert imports for inner classes");
        groovyInsertForInnerClassesCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyInsertForInnerClassesCb.setSelected(workingSettings.getBoolean(GroovyCodeStyleSettings.IMPORTS_INSERT_FOR_INNER_CLASSES, false));
        groovyInsertForInnerClassesCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GroovyCodeStyleSettings.IMPORTS_INSERT_FOR_INNER_CLASSES, newV);
                notifyModified();
            }
        });

        groovyUseFqClassNamesInJavadocCb = new CheckBox("Use fully qualified class names in javadoc");
        groovyUseFqClassNamesInJavadocCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyUseFqClassNamesInJavadocCb.setSelected(workingSettings.getBoolean(GroovyCodeStyleSettings.IMPORTS_USE_FQ_CLASS_NAMES_IN_JAVADOC, true));
        groovyUseFqClassNamesInJavadocCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GroovyCodeStyleSettings.IMPORTS_USE_FQ_CLASS_NAMES_IN_JAVADOC, newV);
                notifyModified();
            }
        });

        HBox classCountRow = new HBox(8);
        classCountRow.setAlignment(Pos.CENTER_LEFT);
        Label classCountLabel = new Label("Class count to use import with '*':");
        classCountLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyClassCountStarField = new TextField(String.valueOf(workingSettings.getInt(GroovyCodeStyleSettings.IMPORTS_CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND, 5)));
        groovyClassCountStarField.setPrefWidth(50);
        styleNumberField(groovyClassCountStarField);
        groovyClassCountStarField.textProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                try {
                    int val = Integer.parseInt(newV.trim());
                    workingSettings.setInt(GroovyCodeStyleSettings.IMPORTS_CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND, val);
                    notifyModified();
                } catch (Exception ignored) {}
            }
        });
        classCountRow.getChildren().addAll(classCountLabel, groovyClassCountStarField);

        HBox staticCountRow = new HBox(8);
        staticCountRow.setAlignment(Pos.CENTER_LEFT);
        Label staticCountLabel = new Label("Names count to use static import with '*':");
        staticCountLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyStaticCountStarField = new TextField(String.valueOf(workingSettings.getInt(GroovyCodeStyleSettings.IMPORTS_NAMES_COUNT_TO_USE_STATIC_IMPORT_ON_DEMAND, 3)));
        groovyStaticCountStarField.setPrefWidth(50);
        styleNumberField(groovyStaticCountStarField);
        groovyStaticCountStarField.textProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                try {
                    int val = Integer.parseInt(newV.trim());
                    workingSettings.setInt(GroovyCodeStyleSettings.IMPORTS_NAMES_COUNT_TO_USE_STATIC_IMPORT_ON_DEMAND, val);
                    notifyModified();
                } catch (Exception ignored) {}
            }
        });
        staticCountRow.getChildren().addAll(staticCountLabel, groovyStaticCountStarField);

        generalBox.getChildren().addAll(
                generalHeader,
                groovyUseSingleClassImportCb,
                groovyUseFqClassNamesCb,
                groovyInsertForInnerClassesCb,
                groovyUseFqClassNamesInJavadocCb,
                classCountRow,
                staticCountRow
        );

        // 2. Packages to Use Import with '*'
        VBox packagesBox = new VBox(6);
        Label packagesLabel = new Label("Packages to Use Import with '*':");
        packagesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox packagesToolbar = new HBox(4);
        Button addPkgBtn = createToolbarButton("+");
        Button remPkgBtn = createToolbarButton("−");
        packagesToolbar.getChildren().addAll(addPkgBtn, remPkgBtn);

        groovyPackagesOnDemandTable = createGroovyImportTable(false);
        if (gSettings != null) {
            groovyPackagesOnDemandTable.getItems().setAll(gSettings.getPackagesToUseImportOnDemand());
        }

        addPkgBtn.setOnAction(e -> {
            GroovyCodeStyleSettings.GroovyImportEntry newEntry = new GroovyCodeStyleSettings.GroovyImportEntry(false, "import com.example.*", false);
            groovyPackagesOnDemandTable.getItems().add(newEntry);
            groovyPackagesOnDemandTable.getSelectionModel().select(newEntry);
            notifyModified();
        });
        remPkgBtn.setOnAction(e -> {
            GroovyCodeStyleSettings.GroovyImportEntry sel = groovyPackagesOnDemandTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                groovyPackagesOnDemandTable.getItems().remove(sel);
                notifyModified();
            }
        });

        packagesBox.getChildren().addAll(packagesLabel, packagesToolbar, groovyPackagesOnDemandTable);

        // 3. Layout static imports separately
        groovyLayoutStaticSeparatelyCb = new CheckBox("Layout static imports separately");
        groovyLayoutStaticSeparatelyCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyLayoutStaticSeparatelyCb.setSelected(workingSettings.getBoolean(GroovyCodeStyleSettings.IMPORTS_LAYOUT_STATIC_IMPORTS_SEPARATELY, true));
        groovyLayoutStaticSeparatelyCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GroovyCodeStyleSettings.IMPORTS_LAYOUT_STATIC_IMPORTS_SEPARATELY, newV);
                notifyModified();
            }
        });

        // 4. Import layout:
        VBox layoutBox = new VBox(6);
        Label layoutLabel = new Label("Import layout:");
        layoutLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox layoutToolbar = new HBox(4);
        Button addLayoutBtn = createToolbarButton("+");
        Button remLayoutBtn = createToolbarButton("−");
        Button upLayoutBtn = createToolbarButton("↑");
        Button downLayoutBtn = createToolbarButton("↓");
        layoutToolbar.getChildren().addAll(addLayoutBtn, remLayoutBtn, upLayoutBtn, downLayoutBtn);

        groovyImportLayoutTable = createGroovyImportTable(true);
        if (gSettings != null) {
            groovyImportLayoutTable.getItems().setAll(gSettings.getImportLayout());
        }

        addLayoutBtn.setOnAction(e -> {
            GroovyCodeStyleSettings.GroovyImportEntry newEntry = new GroovyCodeStyleSettings.GroovyImportEntry(false, "import java.*", true);
            groovyImportLayoutTable.getItems().add(newEntry);
            groovyImportLayoutTable.getSelectionModel().select(newEntry);
            notifyModified();
        });
        remLayoutBtn.setOnAction(e -> {
            GroovyCodeStyleSettings.GroovyImportEntry sel = groovyImportLayoutTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                groovyImportLayoutTable.getItems().remove(sel);
                notifyModified();
            }
        });
        upLayoutBtn.setOnAction(e -> {
            int idx = groovyImportLayoutTable.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                GroovyCodeStyleSettings.GroovyImportEntry item = groovyImportLayoutTable.getItems().remove(idx);
                groovyImportLayoutTable.getItems().add(idx - 1, item);
                groovyImportLayoutTable.getSelectionModel().select(idx - 1);
                notifyModified();
            }
        });
        downLayoutBtn.setOnAction(e -> {
            int idx = groovyImportLayoutTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < groovyImportLayoutTable.getItems().size() - 1) {
                GroovyCodeStyleSettings.GroovyImportEntry item = groovyImportLayoutTable.getItems().remove(idx);
                groovyImportLayoutTable.getItems().add(idx + 1, item);
                groovyImportLayoutTable.getSelectionModel().select(idx + 1);
                notifyModified();
            }
        });

        layoutBox.getChildren().addAll(layoutLabel, layoutToolbar, groovyImportLayoutTable);

        box.getChildren().addAll(generalBox, packagesBox, groovyLayoutStaticSeparatelyCb, layoutBox);
        leftContentBox.getChildren().add(box);
    }

    private TableView<GroovyCodeStyleSettings.GroovyImportEntry> createGroovyImportTable(boolean isLayoutTable) {
        TableView<GroovyCodeStyleSettings.GroovyImportEntry> table = new TableView<>();
        table.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-control-inner-background-alt: #1E1F22; -fx-background: #1E1F22; -fx-selection-bar: #35538F; -fx-selection-bar-non-focused: #35538F; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        table.setPrefHeight(isLayoutTable ? 150 : 100);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Column 1: Static
        TableColumn<GroovyCodeStyleSettings.GroovyImportEntry, Boolean> staticCol = new TableColumn<>("Static");
        staticCol.setPrefWidth(65);
        staticCol.setMaxWidth(80);
        staticCol.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().isStatic()));
        staticCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    GroovyCodeStyleSettings.GroovyImportEntry entry = getTableView().getItems().get(getIndex());
                    entry.setStatic(cb.isSelected());
                    notifyModified();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    GroovyCodeStyleSettings.GroovyImportEntry entry = getTableView().getItems().get(getIndex());
                    if (isLayoutTable && entry != null && entry.isSpecial()) {
                        setGraphic(null);
                    } else {
                        cb.setSelected(item != null && item);
                        setGraphic(cb);
                    }
                }
            }
        });

        // Column 2: Package
        TableColumn<GroovyCodeStyleSettings.GroovyImportEntry, String> pkgCol = new TableColumn<>("Package");
        pkgCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPackageName()));
        pkgCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if ("<blank line>".equals(item)) {
                        Text t = new Text("<blank line>");
                        t.setFill(Color.web("#848BA3"));
                        t.setFont(Font.font("monospace", 12));
                        setGraphic(t);
                    } else if (item.startsWith("import static ")) {
                        TextFlow flow = new TextFlow();
                        Text kw = new Text("import static ");
                        kw.setFill(Color.web("#CF8E6D"));
                        kw.setFont(Font.font("monospace", 12));
                        Text rest = new Text(item.substring("import static ".length()));
                        rest.setFill(Color.web("#DFE1E5"));
                        rest.setFont(Font.font("monospace", 12));
                        flow.getChildren().addAll(kw, rest);
                        setGraphic(flow);
                    } else if (item.startsWith("import ")) {
                        TextFlow flow = new TextFlow();
                        Text kw = new Text("import ");
                        kw.setFill(Color.web("#CF8E6D"));
                        kw.setFont(Font.font("monospace", 12));
                        Text rest = new Text(item.substring("import ".length()));
                        rest.setFill(Color.web("#DFE1E5"));
                        rest.setFont(Font.font("monospace", 12));
                        flow.getChildren().addAll(kw, rest);
                        setGraphic(flow);
                    } else {
                        Text t = new Text(item);
                        t.setFill(Color.web("#DFE1E5"));
                        t.setFont(Font.font("monospace", 12));
                        setGraphic(t);
                    }
                    setText(null);
                }
            }
        });

        // Column 3: With Subpackages
        TableColumn<GroovyCodeStyleSettings.GroovyImportEntry, Boolean> subCol = new TableColumn<>("With Subpackages");
        subCol.setPrefWidth(140);
        subCol.setMaxWidth(160);
        subCol.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().isWithSubpackages()));
        subCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    GroovyCodeStyleSettings.GroovyImportEntry entry = getTableView().getItems().get(getIndex());
                    entry.setWithSubpackages(cb.isSelected());
                    notifyModified();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    GroovyCodeStyleSettings.GroovyImportEntry entry = getTableView().getItems().get(getIndex());
                    if (isLayoutTable && entry != null && entry.isSpecial()) {
                        setGraphic(null);
                    } else {
                        cb.setSelected(item != null && item);
                        setGraphic(cb);
                    }
                }
            }
        });

        table.getColumns().addAll(staticCol, pkgCol, subCol);
        return table;
    }

    private void renderGroovyCodeGenerationTab() {
        GroovyCodeStyleSettings gSettings = workingSettings instanceof GroovyCodeStyleSettings gs ? gs : null;

        VBox box = new VBox(12);
        box.setPadding(new Insets(2, 0, 10, 0));

        // 1. Order of Members
        VBox orderBox = new VBox(6);
        Label orderLabel = new Label("Order of Members");
        orderLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox orderToolbar = new HBox(4);
        Button upBtn = createToolbarButton("↑");
        Button downBtn = createToolbarButton("↓");
        orderToolbar.getChildren().addAll(upBtn, downBtn);

        groovyOrderOfMembersListView = new ListView<>();
        groovyOrderOfMembersListView.setStyle(
            "-fx-background-color: #1E1F22; " +
            "-fx-control-inner-background: #1E1F22; " +
            "-fx-control-inner-background-alt: #1E1F22; " +
            "-fx-background: #1E1F22; " +
            "-fx-selection-bar: #35538F; " +
            "-fx-selection-bar-non-focused: #35538F; " +
            "-fx-border-color: #393B40; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;"
        );
        groovyOrderOfMembersListView.setPrefHeight(180);
        if (gSettings != null) {
            groovyOrderOfMembersListView.getItems().setAll(gSettings.getOrderOfMembers());
        }

        groovyOrderOfMembersListView.setCellFactory(lv -> new ListCell<>() {
            {
                setOnMouseEntered(e -> {
                    if (!isEmpty() && !isSelected()) {
                        setStyle("-fx-background-color: #26282E; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 8 3 8;");
                    }
                });
                setOnMouseExited(e -> {
                    if (!isEmpty() && !isSelected()) {
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 8 3 8;");
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                updateCell(item, empty, isSelected());
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                updateCell(getItem(), isEmpty(), selected);
            }

            private void updateCell(String item, boolean empty, boolean selected) {
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    String bg = selected ? "-fx-background-color: #35538F;" : "-fx-background-color: transparent;";
                    setStyle(bg + " -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 8 3 8;");
                }
            }
        });

        if (!groovyOrderOfMembersListView.getItems().isEmpty()) {
            groovyOrderOfMembersListView.getSelectionModel().select(0);
        }

        upBtn.setOnAction(e -> {
            int idx = groovyOrderOfMembersListView.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                String item = groovyOrderOfMembersListView.getItems().remove(idx);
                groovyOrderOfMembersListView.getItems().add(idx - 1, item);
                groovyOrderOfMembersListView.getSelectionModel().select(idx - 1);
                notifyModified();
            }
        });

        downBtn.setOnAction(e -> {
            int idx = groovyOrderOfMembersListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < groovyOrderOfMembersListView.getItems().size() - 1) {
                String item = groovyOrderOfMembersListView.getItems().remove(idx);
                groovyOrderOfMembersListView.getItems().add(idx + 1, item);
                groovyOrderOfMembersListView.getSelectionModel().select(idx + 1);
                notifyModified();
            }
        });

        orderBox.getChildren().addAll(orderLabel, orderToolbar, groovyOrderOfMembersListView);

        // 2. Comment Code
        VBox commentCodeBox = new VBox(6);
        HBox commentHeader = createDividerHeader("Comment Code");

        groovyLineCommentFirstColCb = new CheckBox("Line comment at first column");
        groovyLineCommentFirstColCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyLineCommentFirstColCb.setSelected(workingSettings.getBoolean(GroovyCodeStyleSettings.CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, true));
        groovyLineCommentFirstColCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GroovyCodeStyleSettings.CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, newV);
                notifyModified();
            }
        });

        groovyAddSpaceLineCommentCb = new CheckBox("Add a space at line comment start");
        groovyAddSpaceLineCommentCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyAddSpaceLineCommentCb.setSelected(workingSettings.getBoolean(GroovyCodeStyleSettings.CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, false));

        groovyEnforceOnReformatCb = new CheckBox("Enforce on reformat");
        groovyEnforceOnReformatCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyEnforceOnReformatCb.setPadding(new Insets(0, 0, 0, 22));
        groovyEnforceOnReformatCb.setSelected(workingSettings.getBoolean(GroovyCodeStyleSettings.CODE_GEN_ENFORCE_ON_REFORMAT, false));
        groovyEnforceOnReformatCb.setDisable(!groovyAddSpaceLineCommentCb.isSelected());

        groovyAddSpaceLineCommentCb.selectedProperty().addListener((obs, oldV, newV) -> {
            groovyEnforceOnReformatCb.setDisable(!newV);
            if (!suppressEvents) {
                workingSettings.setBoolean(GroovyCodeStyleSettings.CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, newV);
                notifyModified();
            }
        });

        groovyEnforceOnReformatCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GroovyCodeStyleSettings.CODE_GEN_ENFORCE_ON_REFORMAT, newV);
                notifyModified();
            }
        });

        groovyBlockCommentFirstColCb = new CheckBox("Block comment at first column");
        groovyBlockCommentFirstColCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyBlockCommentFirstColCb.setSelected(workingSettings.getBoolean(GroovyCodeStyleSettings.CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, true));
        groovyBlockCommentFirstColCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GroovyCodeStyleSettings.CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, newV);
                notifyModified();
            }
        });

        groovyAddSpacesAroundBlockCommentsCb = new CheckBox("Add spaces around block comments");
        groovyAddSpacesAroundBlockCommentsCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        groovyAddSpacesAroundBlockCommentsCb.setSelected(workingSettings.getBoolean(GroovyCodeStyleSettings.CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, false));
        groovyAddSpacesAroundBlockCommentsCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(GroovyCodeStyleSettings.CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, newV);
                notifyModified();
            }
        });

        commentCodeBox.getChildren().addAll(
                commentHeader,
                groovyLineCommentFirstColCb,
                groovyAddSpaceLineCommentCb,
                groovyEnforceOnReformatCb,
                groovyBlockCommentFirstColCb,
                groovyAddSpacesAroundBlockCommentsCb
        );

        box.getChildren().addAll(orderBox, commentCodeBox);
        leftContentBox.getChildren().add(box);
    }

    private void renderHtmlOtherTab() {
        HtmlCodeStyleSettings hSettings = workingSettings instanceof HtmlCodeStyleSettings hs ? hs : null;

        VBox box = new VBox(8);
        box.setPadding(new Insets(2, 0, 10, 0));

        // 1. Hard wrap at: [120] columns
        HBox hardWrapRow = new HBox(8);
        hardWrapRow.setAlignment(Pos.CENTER_LEFT);
        Label hardWrapLbl = new Label("Hard wrap at:");
        hardWrapLbl.setMinWidth(140);
        hardWrapLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlHardWrapField = new TextField(hSettings != null ? String.valueOf(hSettings.getHardWrapAt()) : "120");
        htmlHardWrapField.setPrefWidth(70);
        styleNumberField(htmlHardWrapField);
        Label hardWrapSuffix = new Label("columns");
        hardWrapSuffix.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        hardWrapRow.getChildren().addAll(hardWrapLbl, htmlHardWrapField, hardWrapSuffix);

        htmlHardWrapField.textProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                try {
                    int val = Integer.parseInt(newV.trim());
                    workingSettings.setInt(HtmlCodeStyleSettings.HARD_WRAP_AT, val);
                    updatePreview();
                    notifyModified();
                } catch (Exception ignored) {}
            }
        });

        // 2. Wrap on typing: [Default: No ▾]
        HBox wrapOnTypingRow = new HBox(8);
        wrapOnTypingRow.setAlignment(Pos.CENTER_LEFT);
        Label wrapOnTypingLbl = new Label("Wrap on typing");
        wrapOnTypingLbl.setMinWidth(140);
        wrapOnTypingLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlWrapOnTypingCombo = new ComboBox<>();
        htmlWrapOnTypingCombo.getItems().addAll(HtmlCodeStyleSettings.WRAP_ON_TYPING_OPTIONS);
        htmlWrapOnTypingCombo.setValue(workingSettings.getString(HtmlCodeStyleSettings.WRAP_ON_TYPING, "Default: No"));
        styleComboBox(htmlWrapOnTypingCombo);
        htmlWrapOnTypingCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(HtmlCodeStyleSettings.WRAP_ON_TYPING, htmlWrapOnTypingCombo.getValue());
                notifyModified();
            }
        });
        wrapOnTypingRow.getChildren().addAll(wrapOnTypingLbl, htmlWrapOnTypingCombo);

        // 3. Visual guides: [Default: ] columns
        VBox visualGuidesBox = new VBox(2);
        HBox visualGuidesRow = new HBox(8);
        visualGuidesRow.setAlignment(Pos.CENTER_LEFT);
        Label visualGuidesLbl = new Label("Visual guides:");
        visualGuidesLbl.setMinWidth(140);
        visualGuidesLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlVisualGuidesField = new TextField(workingSettings.getString(HtmlCodeStyleSettings.VISUAL_GUIDES, ""));
        htmlVisualGuidesField.setPromptText("Default: ");
        htmlVisualGuidesField.setPrefWidth(120);
        htmlVisualGuidesField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");
        Label visualGuidesSuffix = new Label("columns");
        visualGuidesSuffix.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        visualGuidesRow.getChildren().addAll(visualGuidesLbl, htmlVisualGuidesField, visualGuidesSuffix);

        Label visualGuidesHint = new Label("Specify one guide (80) or several (80, 120)");
        visualGuidesHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 148;");
        visualGuidesBox.getChildren().addAll(visualGuidesRow, visualGuidesHint);

        htmlVisualGuidesField.textProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setString(HtmlCodeStyleSettings.VISUAL_GUIDES, newV.trim());
                notifyModified();
            }
        });

        // 4. Keep line breaks & Keep line breaks in text
        htmlKeepLineBreaksCb = new CheckBox("Keep line breaks");
        htmlKeepLineBreaksCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlKeepLineBreaksCb.setSelected(workingSettings.getBoolean(HtmlCodeStyleSettings.KEEP_LINE_BREAKS, true));
        htmlKeepLineBreaksCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(HtmlCodeStyleSettings.KEEP_LINE_BREAKS, newV);
                updatePreview();
                notifyModified();
            }
        });

        htmlKeepLineBreaksInTextCb = new CheckBox("Keep line breaks in text");
        htmlKeepLineBreaksInTextCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlKeepLineBreaksInTextCb.setSelected(workingSettings.getBoolean(HtmlCodeStyleSettings.KEEP_LINE_BREAKS_IN_TEXT, true));
        htmlKeepLineBreaksInTextCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(HtmlCodeStyleSettings.KEEP_LINE_BREAKS_IN_TEXT, newV);
                updatePreview();
                notifyModified();
            }
        });

        // 5. Keep blank lines: [ 2 ]
        HBox keepBlankLinesRow = new HBox(8);
        keepBlankLinesRow.setAlignment(Pos.CENTER_LEFT);
        Label keepBlankLinesLbl = new Label("Keep blank lines:");
        keepBlankLinesLbl.setMinWidth(140);
        keepBlankLinesLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlKeepBlankLinesField = new TextField(hSettings != null ? String.valueOf(hSettings.getKeepBlankLines()) : "2");
        htmlKeepBlankLinesField.setPrefWidth(55);
        styleNumberField(htmlKeepBlankLinesField);
        keepBlankLinesRow.getChildren().addAll(keepBlankLinesLbl, htmlKeepBlankLinesField);

        htmlKeepBlankLinesField.textProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                try {
                    int val = Integer.parseInt(newV.trim());
                    workingSettings.setInt(HtmlCodeStyleSettings.KEEP_BLANK_LINES, val);
                    updatePreview();
                    notifyModified();
                } catch (Exception ignored) {}
            }
        });

        // 6. Wrap attributes: [Wrap if long ▾]
        HBox wrapAttributesRow = new HBox(8);
        wrapAttributesRow.setAlignment(Pos.CENTER_LEFT);
        Label wrapAttributesLbl = new Label("Wrap attributes:");
        wrapAttributesLbl.setMinWidth(140);
        wrapAttributesLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlWrapAttributesCombo = new ComboBox<>();
        htmlWrapAttributesCombo.getItems().addAll(HtmlCodeStyleSettings.WRAP_ATTRIBUTES_OPTIONS);
        htmlWrapAttributesCombo.setValue(workingSettings.getString(HtmlCodeStyleSettings.WRAP_ATTRIBUTES, "Wrap if long"));
        styleComboBox(htmlWrapAttributesCombo);
        htmlWrapAttributesCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(HtmlCodeStyleSettings.WRAP_ATTRIBUTES, htmlWrapAttributesCombo.getValue());
                updatePreview();
                notifyModified();
            }
        });
        wrapAttributesRow.getChildren().addAll(wrapAttributesLbl, htmlWrapAttributesCombo);

        // 7. Two-column section: Left checkboxes, Right Spaces section
        HBox twoColBox = new HBox(24);
        twoColBox.setPadding(new Insets(4, 0, 4, 0));

        VBox leftCol = new VBox(6);
        leftCol.setPrefWidth(220);

        htmlWrapTextCb = new CheckBox("Wrap text");
        htmlWrapTextCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlWrapTextCb.setSelected(workingSettings.getBoolean(HtmlCodeStyleSettings.WRAP_TEXT, true));
        htmlWrapTextCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(HtmlCodeStyleSettings.WRAP_TEXT, newV);
                updatePreview();
                notifyModified();
            }
        });

        htmlAlignAttributesCb = new CheckBox("Align attributes");
        htmlAlignAttributesCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlAlignAttributesCb.setSelected(workingSettings.getBoolean(HtmlCodeStyleSettings.ALIGN_ATTRIBUTES, true));
        htmlAlignAttributesCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(HtmlCodeStyleSettings.ALIGN_ATTRIBUTES, newV);
                updatePreview();
                notifyModified();
            }
        });

        htmlAlignTextCb = new CheckBox("Align text");
        htmlAlignTextCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlAlignTextCb.setSelected(workingSettings.getBoolean(HtmlCodeStyleSettings.ALIGN_TEXT, false));
        htmlAlignTextCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(HtmlCodeStyleSettings.ALIGN_TEXT, newV);
                updatePreview();
                notifyModified();
            }
        });

        htmlKeepWhiteSpacesCb = new CheckBox("Keep white spaces");
        htmlKeepWhiteSpacesCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlKeepWhiteSpacesCb.setSelected(workingSettings.getBoolean(HtmlCodeStyleSettings.KEEP_WHITE_SPACES, false));
        htmlKeepWhiteSpacesCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(HtmlCodeStyleSettings.KEEP_WHITE_SPACES, newV);
                updatePreview();
                notifyModified();
            }
        });

        leftCol.getChildren().addAll(htmlWrapTextCb, htmlAlignAttributesCb, htmlAlignTextCb, htmlKeepWhiteSpacesCb);

        VBox rightCol = new VBox(6);
        HBox spacesHeader = createDividerHeader("Spaces");
        spacesHeader.setPrefWidth(220);

        htmlSpacesAroundEqCb = new CheckBox("Around \"=\" in attribute");
        htmlSpacesAroundEqCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlSpacesAroundEqCb.setSelected(workingSettings.getBoolean(HtmlCodeStyleSettings.SPACES_AROUND_EQUALITY_IN_ATTRIBUTE, false));
        htmlSpacesAroundEqCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(HtmlCodeStyleSettings.SPACES_AROUND_EQUALITY_IN_ATTRIBUTE, newV);
                updatePreview();
                notifyModified();
            }
        });

        htmlSpacesAfterTagCb = new CheckBox("After tag name");
        htmlSpacesAfterTagCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlSpacesAfterTagCb.setSelected(workingSettings.getBoolean(HtmlCodeStyleSettings.SPACES_AFTER_TAG_NAME, false));
        htmlSpacesAfterTagCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(HtmlCodeStyleSettings.SPACES_AFTER_TAG_NAME, newV);
                updatePreview();
                notifyModified();
            }
        });

        htmlSpacesInEmptyTagCb = new CheckBox("In empty tag");
        htmlSpacesInEmptyTagCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlSpacesInEmptyTagCb.setSelected(workingSettings.getBoolean(HtmlCodeStyleSettings.SPACES_IN_EMPTY_TAG, false));
        htmlSpacesInEmptyTagCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(HtmlCodeStyleSettings.SPACES_IN_EMPTY_TAG, newV);
                updatePreview();
                notifyModified();
            }
        });

        rightCol.getChildren().addAll(spacesHeader, htmlSpacesAroundEqCb, htmlSpacesAfterTagCb, htmlSpacesInEmptyTagCb);
        twoColBox.getChildren().addAll(leftCol, rightCol);

        // 8. Tag Lists with expand button ⤢
        VBox tagListsBox = new VBox(6);
        tagListsBox.setPadding(new Insets(4, 0, 4, 0));

        Node insertNewLineBeforeRow = createTagFieldRow("Insert new line before:", HtmlCodeStyleSettings.INSERT_NEW_LINE_BEFORE, "body,div,p,form,h1,h2,h3", 220, f -> htmlInsertNewLineBeforeField = f);
        Node removeNewLineBeforeRow = createTagFieldRow("Remove new line before:", HtmlCodeStyleSettings.REMOVE_NEW_LINE_BEFORE, "br", 220, f -> htmlRemoveNewLineBeforeField = f);
        Node doNotIndentChildrenRow = createTagFieldRow("Do not indent children of:", HtmlCodeStyleSettings.DO_NOT_INDENT_CHILDREN_OF, "html,body,thead,tbody,tfoot", 220, f -> htmlDoNotIndentChildrenOfField = f);

        // Special: or if tag size more than [ ] lines
        HBox tagSizeRow = new HBox(8);
        tagSizeRow.setAlignment(Pos.CENTER_LEFT);
        Label tagSizeLbl = new Label("or if tag size more than");
        tagSizeLbl.setMinWidth(220);
        tagSizeLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlDoNotIndentTagSizeMoreThanField = new TextField(workingSettings.getString(HtmlCodeStyleSettings.DO_NOT_INDENT_TAG_SIZE_MORE_THAN, ""));
        htmlDoNotIndentTagSizeMoreThanField.setPrefWidth(55);
        styleNumberField(htmlDoNotIndentTagSizeMoreThanField);
        Label tagSizeSuffix = new Label("lines");
        tagSizeSuffix.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        tagSizeRow.getChildren().addAll(tagSizeLbl, htmlDoNotIndentTagSizeMoreThanField, tagSizeSuffix);

        htmlDoNotIndentTagSizeMoreThanField.textProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setString(HtmlCodeStyleSettings.DO_NOT_INDENT_TAG_SIZE_MORE_THAN, newV.trim());
                notifyModified();
            }
        });

        Node inlineElementsRow = createTagFieldRow("Inline elements:", HtmlCodeStyleSettings.INLINE_ELEMENTS, "strong,sub,sup,textarea,tt,u,var", 220, f -> htmlInlineElementsField = f);
        Node keepWhiteSpacesInsideRow = createTagFieldRow("Keep white spaces inside:", HtmlCodeStyleSettings.KEEP_WHITE_SPACES_INSIDE, "span,pre,textarea", 220, f -> htmlKeepWhiteSpacesInsideField = f);
        Node dontBreakIfInlineRow = createTagFieldRow("Don't break if inline content:", HtmlCodeStyleSettings.DONT_BREAK_IF_INLINE_CONTENT, "title,h1,h2,h3,h4,h5,h6,p", 220, f -> htmlDontBreakIfInlineContentField = f);

        tagListsBox.getChildren().addAll(
                insertNewLineBeforeRow,
                removeNewLineBeforeRow,
                doNotIndentChildrenRow,
                tagSizeRow,
                inlineElementsRow,
                keepWhiteSpacesInsideRow,
                dontBreakIfInlineRow
        );

        // 9. Attribute placement & Quote options
        VBox bottomCombosBox = new VBox(6);
        bottomCombosBox.setPadding(new Insets(4, 0, 4, 0));

        HBox firstAttrRow = new HBox(8);
        firstAttrRow.setAlignment(Pos.CENTER_LEFT);
        Label firstAttrLbl = new Label("New line before first attribute:");
        firstAttrLbl.setMinWidth(220);
        firstAttrLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlNewLineBeforeFirstAttrCombo = new ComboBox<>();
        htmlNewLineBeforeFirstAttrCombo.getItems().addAll(HtmlCodeStyleSettings.ATTRIBUTE_NEW_LINE_OPTIONS);
        htmlNewLineBeforeFirstAttrCombo.setValue(workingSettings.getString(HtmlCodeStyleSettings.NEW_LINE_BEFORE_FIRST_ATTRIBUTE, "Never"));
        styleComboBox(htmlNewLineBeforeFirstAttrCombo);
        htmlNewLineBeforeFirstAttrCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(HtmlCodeStyleSettings.NEW_LINE_BEFORE_FIRST_ATTRIBUTE, htmlNewLineBeforeFirstAttrCombo.getValue());
                updatePreview();
                notifyModified();
            }
        });
        firstAttrRow.getChildren().addAll(firstAttrLbl, htmlNewLineBeforeFirstAttrCombo);

        HBox lastAttrRow = new HBox(8);
        lastAttrRow.setAlignment(Pos.CENTER_LEFT);
        Label lastAttrLbl = new Label("New line after last attribute:");
        lastAttrLbl.setMinWidth(220);
        lastAttrLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlNewLineAfterLastAttrCombo = new ComboBox<>();
        htmlNewLineAfterLastAttrCombo.getItems().addAll(HtmlCodeStyleSettings.ATTRIBUTE_NEW_LINE_OPTIONS);
        htmlNewLineAfterLastAttrCombo.setValue(workingSettings.getString(HtmlCodeStyleSettings.NEW_LINE_AFTER_LAST_ATTRIBUTE, "Never"));
        styleComboBox(htmlNewLineAfterLastAttrCombo);
        htmlNewLineAfterLastAttrCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(HtmlCodeStyleSettings.NEW_LINE_AFTER_LAST_ATTRIBUTE, htmlNewLineAfterLastAttrCombo.getValue());
                updatePreview();
                notifyModified();
            }
        });
        lastAttrRow.getChildren().addAll(lastAttrLbl, htmlNewLineAfterLastAttrCombo);

        HBox jsxAttrRow = new HBox(8);
        jsxAttrRow.setAlignment(Pos.CENTER_LEFT);
        Label jsxAttrLbl = new Label("Add for JSX attributes:");
        jsxAttrLbl.setMinWidth(220);
        jsxAttrLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlAddForJsxAttributesCombo = new ComboBox<>();
        htmlAddForJsxAttributesCombo.getItems().addAll(HtmlCodeStyleSettings.JSX_ATTRIBUTES_OPTIONS);
        htmlAddForJsxAttributesCombo.setValue(workingSettings.getString(HtmlCodeStyleSettings.ADD_FOR_JSX_ATTRIBUTES, "Braces"));
        styleComboBox(htmlAddForJsxAttributesCombo);
        htmlAddForJsxAttributesCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(HtmlCodeStyleSettings.ADD_FOR_JSX_ATTRIBUTES, htmlAddForJsxAttributesCombo.getValue());
                notifyModified();
            }
        });
        jsxAttrRow.getChildren().addAll(jsxAttrLbl, htmlAddForJsxAttributesCombo);

        HBox quoteMarksRow = new HBox(8);
        quoteMarksRow.setAlignment(Pos.CENTER_LEFT);
        Label quoteMarksLbl = new Label("Generated quote marks:");
        quoteMarksLbl.setMinWidth(220);
        quoteMarksLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlGeneratedQuoteMarksCombo = new ComboBox<>();
        htmlGeneratedQuoteMarksCombo.getItems().addAll(HtmlCodeStyleSettings.GENERATED_QUOTE_MARKS_OPTIONS);
        htmlGeneratedQuoteMarksCombo.setValue(workingSettings.getString(HtmlCodeStyleSettings.GENERATED_QUOTE_MARKS, "Double"));
        styleComboBox(htmlGeneratedQuoteMarksCombo);
        htmlGeneratedQuoteMarksCombo.setOnAction(e -> {
            if (!suppressEvents) {
                workingSettings.setString(HtmlCodeStyleSettings.GENERATED_QUOTE_MARKS, htmlGeneratedQuoteMarksCombo.getValue());
                updatePreview();
                notifyModified();
            }
        });
        quoteMarksRow.getChildren().addAll(quoteMarksLbl, htmlGeneratedQuoteMarksCombo);

        htmlEnforceOnFormatCb = new CheckBox("Enforce on format");
        htmlEnforceOnFormatCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        htmlEnforceOnFormatCb.setPadding(new Insets(0, 0, 0, 220));
        htmlEnforceOnFormatCb.setSelected(workingSettings.getBoolean(HtmlCodeStyleSettings.ENFORCE_ON_FORMAT, false));
        htmlEnforceOnFormatCb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setBoolean(HtmlCodeStyleSettings.ENFORCE_ON_FORMAT, newV);
                notifyModified();
            }
        });

        bottomCombosBox.getChildren().addAll(firstAttrRow, lastAttrRow, jsxAttrRow, quoteMarksRow, htmlEnforceOnFormatCb);

        box.getChildren().addAll(
                hardWrapRow,
                wrapOnTypingRow,
                visualGuidesBox,
                htmlKeepLineBreaksCb,
                htmlKeepLineBreaksInTextCb,
                keepBlankLinesRow,
                wrapAttributesRow,
                twoColBox,
                tagListsBox,
                bottomCombosBox
        );

        leftContentBox.getChildren().add(box);
    }

    private Node createTagFieldRow(String labelText, String propertyKey, String defaultVal, int minLabelWidth, java.util.function.Consumer<TextField> fieldConsumer) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(labelText);
        lbl.setMinWidth(minLabelWidth);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField tf = new TextField(workingSettings.getString(propertyKey, defaultVal));
        tf.setPrefWidth(220);
        tf.setMaxWidth(300);
        HBox.setHgrow(tf, Priority.ALWAYS);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");

        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                workingSettings.setString(propertyKey, newV.trim());
                notifyModified();
            }
        });

        if (fieldConsumer != null) {
            fieldConsumer.accept(tf);
        }

        Button expandBtn = new Button("⤢");
        expandBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 2 6 2 6; -fx-font-size: 11px; -fx-cursor: hand;");
        expandBtn.setOnAction(e -> showTagEditorDialog(labelText, tf, propertyKey));

        row.getChildren().addAll(lbl, tf, expandBtn);
        return row;
    }

    private void showTagEditorDialog(String title, TextField targetField, String propertyKey) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title.replace(":", ""));
        dialog.setHeaderText("Edit tag list (separated by comma or new lines):");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #1E1F22;");
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea ta = new TextArea();
        String current = targetField.getText();
        if (current != null && !current.isEmpty()) {
            ta.setText(String.join("\n", current.split(",")));
        }
        ta.setPrefRowCount(10);
        ta.setPrefColumnCount(30);
        ta.setStyle("-fx-control-inner-background: #1E1F22; -fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-font-family: monospace; -fx-font-size: 12px;");

        VBox content = new VBox(8, ta);
        content.setPadding(new Insets(10));
        pane.setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String[] lines = ta.getText().split("[\\r\\n,]+");
                List<String> cleaned = new ArrayList<>();
                for (String l : lines) {
                    String trim = l.trim();
                    if (!trim.isEmpty()) {
                        cleaned.add(trim);
                    }
                }
                return String.join(",", cleaned);
            }
            return null;
        });

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(val -> {
            targetField.setText(val);
            workingSettings.setString(propertyKey, val);
            notifyModified();
        });
    }

    private void saveCurrentTabUiToWorkingSettings() {
        if ("Groovy".equals(languageId)) {
            if ("Imports".equals(activeTab)) {
                if (groovyUseSingleClassImportCb != null) workingSettings.setBoolean(GroovyCodeStyleSettings.IMPORTS_USE_SINGLE_CLASS_IMPORT, groovyUseSingleClassImportCb.isSelected());
                if (groovyUseFqClassNamesCb != null) workingSettings.setBoolean(GroovyCodeStyleSettings.IMPORTS_USE_FQ_CLASS_NAMES, groovyUseFqClassNamesCb.isSelected());
                if (groovyInsertForInnerClassesCb != null) workingSettings.setBoolean(GroovyCodeStyleSettings.IMPORTS_INSERT_FOR_INNER_CLASSES, groovyInsertForInnerClassesCb.isSelected());
                if (groovyUseFqClassNamesInJavadocCb != null) workingSettings.setBoolean(GroovyCodeStyleSettings.IMPORTS_USE_FQ_CLASS_NAMES_IN_JAVADOC, groovyUseFqClassNamesInJavadocCb.isSelected());
                if (groovyClassCountStarField != null) {
                    try {
                        workingSettings.setInt(GroovyCodeStyleSettings.IMPORTS_CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND, Integer.parseInt(groovyClassCountStarField.getText().trim()));
                    } catch (Exception ignored) {}
                }
                if (groovyStaticCountStarField != null) {
                    try {
                        workingSettings.setInt(GroovyCodeStyleSettings.IMPORTS_NAMES_COUNT_TO_USE_STATIC_IMPORT_ON_DEMAND, Integer.parseInt(groovyStaticCountStarField.getText().trim()));
                    } catch (Exception ignored) {}
                }
                if (groovyLayoutStaticSeparatelyCb != null) workingSettings.setBoolean(GroovyCodeStyleSettings.IMPORTS_LAYOUT_STATIC_IMPORTS_SEPARATELY, groovyLayoutStaticSeparatelyCb.isSelected());

                if (workingSettings instanceof GroovyCodeStyleSettings gSettings) {
                    if (groovyPackagesOnDemandTable != null) {
                        gSettings.getPackagesToUseImportOnDemand().clear();
                        for (GroovyCodeStyleSettings.GroovyImportEntry e : groovyPackagesOnDemandTable.getItems()) {
                            gSettings.getPackagesToUseImportOnDemand().add(e.copy());
                        }
                    }
                    if (groovyImportLayoutTable != null) {
                        gSettings.getImportLayout().clear();
                        for (GroovyCodeStyleSettings.GroovyImportEntry e : groovyImportLayoutTable.getItems()) {
                            gSettings.getImportLayout().add(e.copy());
                        }
                    }
                }
                return;
            } else if ("Code Generation".equals(activeTab)) {
                if (groovyLineCommentFirstColCb != null) workingSettings.setBoolean(GroovyCodeStyleSettings.CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, groovyLineCommentFirstColCb.isSelected());
                if (groovyAddSpaceLineCommentCb != null) workingSettings.setBoolean(GroovyCodeStyleSettings.CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, groovyAddSpaceLineCommentCb.isSelected());
                if (groovyEnforceOnReformatCb != null) workingSettings.setBoolean(GroovyCodeStyleSettings.CODE_GEN_ENFORCE_ON_REFORMAT, groovyEnforceOnReformatCb.isSelected());
                if (groovyBlockCommentFirstColCb != null) workingSettings.setBoolean(GroovyCodeStyleSettings.CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, groovyBlockCommentFirstColCb.isSelected());
                if (groovyAddSpacesAroundBlockCommentsCb != null) workingSettings.setBoolean(GroovyCodeStyleSettings.CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, groovyAddSpacesAroundBlockCommentsCb.isSelected());

                if (workingSettings instanceof GroovyCodeStyleSettings gSettings) {
                    if (groovyOrderOfMembersListView != null) {
                        gSettings.getOrderOfMembers().clear();
                        gSettings.getOrderOfMembers().addAll(groovyOrderOfMembersListView.getItems());
                    }
                }
                return;
            }
        }
        if ("Go".equals(languageId)) {
            if ("Imports".equals(activeTab)) {
                if (goBackquotesCb != null) workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_USE_BACKQUOTES, goBackquotesCb.isSelected());
                if (goSingleImportParensCb != null) workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_ADD_PARENTHESES_SINGLE, goSingleImportParensCb.isSelected());
                if (goRemoveRedundantCb != null) workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_REMOVE_REDUNDANT_ALIASES, goRemoveRedundantCb.isSelected());
                if (goSortingCombo != null && goSortingCombo.getValue() != null) workingSettings.setString(GoCodeStyleSettings.IMPORTS_SORTING_TYPE, goSortingCombo.getValue());
                if (goMoveAllSingleDeclCb != null) workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_MOVE_ALL_SINGLE_DECLARATION, goMoveAllSingleDeclCb.isSelected());
                if (goGroupSdkCb != null) workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_GROUP_SDK_PACKAGES, goGroupSdkCb.isSelected());
                if (goMoveAllSingleGroupCb != null) workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_MOVE_ALL_SINGLE_GROUP, goMoveAllSingleGroupCb.isSelected());
                if (goGroupCb != null) workingSettings.setBoolean(GoCodeStyleSettings.IMPORTS_GROUP_ENABLED, goGroupCb.isSelected());
                if (goGroupModeGroup != null && goGroupModeGroup.getSelectedToggle() != null) {
                    workingSettings.setString(GoCodeStyleSettings.IMPORTS_GROUP_MODE, goGroupModeGroup.getSelectedToggle().getUserData().toString());
                }
                if (goPrefixesArea != null) workingSettings.setString(GoCodeStyleSettings.IMPORTS_CUSTOM_PREFIXES, goPrefixesArea.getText());
                return;
            } else if ("Other".equals(activeTab)) {
                if (goLeadingSpaceCb != null) workingSettings.setBoolean(GoCodeStyleSettings.OTHER_ADD_LEADING_SPACE_COMMENTS, goLeadingSpaceCb.isSelected());
                if (goColWidthField != null) {
                    try {
                        workingSettings.setInt(GoCodeStyleSettings.OTHER_COLUMN_WIDTH_FILL_PARAGRAPH, Integer.parseInt(goColWidthField.getText().trim()));
                    } catch (Exception ignored) {}
                }
                if (goRunGofmtCb != null) workingSettings.setBoolean(GoCodeStyleSettings.OTHER_RUN_GOFMT_ON_REFORMAT, goRunGofmtCb.isSelected());
                return;
            }
        }

        if ("HTML".equals(languageId)) {
            if ("Other".equals(activeTab)) {
                if (htmlHardWrapField != null) {
                    try { workingSettings.setInt(HtmlCodeStyleSettings.HARD_WRAP_AT, Integer.parseInt(htmlHardWrapField.getText().trim())); } catch (Exception ignored) {}
                }
                if (htmlWrapOnTypingCombo != null && htmlWrapOnTypingCombo.getValue() != null) workingSettings.setString(HtmlCodeStyleSettings.WRAP_ON_TYPING, htmlWrapOnTypingCombo.getValue());
                if (htmlVisualGuidesField != null) workingSettings.setString(HtmlCodeStyleSettings.VISUAL_GUIDES, htmlVisualGuidesField.getText().trim());
                if (htmlKeepLineBreaksCb != null) workingSettings.setBoolean(HtmlCodeStyleSettings.KEEP_LINE_BREAKS, htmlKeepLineBreaksCb.isSelected());
                if (htmlKeepLineBreaksInTextCb != null) workingSettings.setBoolean(HtmlCodeStyleSettings.KEEP_LINE_BREAKS_IN_TEXT, htmlKeepLineBreaksInTextCb.isSelected());
                if (htmlKeepBlankLinesField != null) {
                    try { workingSettings.setInt(HtmlCodeStyleSettings.KEEP_BLANK_LINES, Integer.parseInt(htmlKeepBlankLinesField.getText().trim())); } catch (Exception ignored) {}
                }
                if (htmlWrapAttributesCombo != null && htmlWrapAttributesCombo.getValue() != null) workingSettings.setString(HtmlCodeStyleSettings.WRAP_ATTRIBUTES, htmlWrapAttributesCombo.getValue());
                if (htmlWrapTextCb != null) workingSettings.setBoolean(HtmlCodeStyleSettings.WRAP_TEXT, htmlWrapTextCb.isSelected());
                if (htmlAlignAttributesCb != null) workingSettings.setBoolean(HtmlCodeStyleSettings.ALIGN_ATTRIBUTES, htmlAlignAttributesCb.isSelected());
                if (htmlAlignTextCb != null) workingSettings.setBoolean(HtmlCodeStyleSettings.ALIGN_TEXT, htmlAlignTextCb.isSelected());
                if (htmlKeepWhiteSpacesCb != null) workingSettings.setBoolean(HtmlCodeStyleSettings.KEEP_WHITE_SPACES, htmlKeepWhiteSpacesCb.isSelected());
                if (htmlSpacesAroundEqCb != null) workingSettings.setBoolean(HtmlCodeStyleSettings.SPACES_AROUND_EQUALITY_IN_ATTRIBUTE, htmlSpacesAroundEqCb.isSelected());
                if (htmlSpacesAfterTagCb != null) workingSettings.setBoolean(HtmlCodeStyleSettings.SPACES_AFTER_TAG_NAME, htmlSpacesAfterTagCb.isSelected());
                if (htmlSpacesInEmptyTagCb != null) workingSettings.setBoolean(HtmlCodeStyleSettings.SPACES_IN_EMPTY_TAG, htmlSpacesInEmptyTagCb.isSelected());
                if (htmlInsertNewLineBeforeField != null) workingSettings.setString(HtmlCodeStyleSettings.INSERT_NEW_LINE_BEFORE, htmlInsertNewLineBeforeField.getText().trim());
                if (htmlRemoveNewLineBeforeField != null) workingSettings.setString(HtmlCodeStyleSettings.REMOVE_NEW_LINE_BEFORE, htmlRemoveNewLineBeforeField.getText().trim());
                if (htmlDoNotIndentChildrenOfField != null) workingSettings.setString(HtmlCodeStyleSettings.DO_NOT_INDENT_CHILDREN_OF, htmlDoNotIndentChildrenOfField.getText().trim());
                if (htmlDoNotIndentTagSizeMoreThanField != null) workingSettings.setString(HtmlCodeStyleSettings.DO_NOT_INDENT_TAG_SIZE_MORE_THAN, htmlDoNotIndentTagSizeMoreThanField.getText().trim());
                if (htmlInlineElementsField != null) workingSettings.setString(HtmlCodeStyleSettings.INLINE_ELEMENTS, htmlInlineElementsField.getText().trim());
                if (htmlKeepWhiteSpacesInsideField != null) workingSettings.setString(HtmlCodeStyleSettings.KEEP_WHITE_SPACES_INSIDE, htmlKeepWhiteSpacesInsideField.getText().trim());
                if (htmlDontBreakIfInlineContentField != null) workingSettings.setString(HtmlCodeStyleSettings.DONT_BREAK_IF_INLINE_CONTENT, htmlDontBreakIfInlineContentField.getText().trim());
                if (htmlNewLineBeforeFirstAttrCombo != null && htmlNewLineBeforeFirstAttrCombo.getValue() != null) workingSettings.setString(HtmlCodeStyleSettings.NEW_LINE_BEFORE_FIRST_ATTRIBUTE, htmlNewLineBeforeFirstAttrCombo.getValue());
                if (htmlNewLineAfterLastAttrCombo != null && htmlNewLineAfterLastAttrCombo.getValue() != null) workingSettings.setString(HtmlCodeStyleSettings.NEW_LINE_AFTER_LAST_ATTRIBUTE, htmlNewLineAfterLastAttrCombo.getValue());
                if (htmlAddForJsxAttributesCombo != null && htmlAddForJsxAttributesCombo.getValue() != null) workingSettings.setString(HtmlCodeStyleSettings.ADD_FOR_JSX_ATTRIBUTES, htmlAddForJsxAttributesCombo.getValue());
                if (htmlGeneratedQuoteMarksCombo != null && htmlGeneratedQuoteMarksCombo.getValue() != null) workingSettings.setString(HtmlCodeStyleSettings.GENERATED_QUOTE_MARKS, htmlGeneratedQuoteMarksCombo.getValue());
                if (htmlEnforceOnFormatCb != null) workingSettings.setBoolean(HtmlCodeStyleSettings.ENFORCE_ON_FORMAT, htmlEnforceOnFormatCb.isSelected());
                return;
            }
        }

        if ("Imports".equals(activeTab) && !"JavaScript".equals(languageId)) {
            if (topLevelGroup != null && topLevelGroup.getSelectedToggle() != null) {
                Object ud = topLevelGroup.getSelectedToggle().getUserData();
                if (ud != null) {
                    workingSettings.setString(KotlinCodeStyleSettings.TOP_LEVEL_IMPORT_MODE, ud.toString());
                }
            }
            if (topLevelThresholdField != null) {
                try {
                    int th = Integer.parseInt(topLevelThresholdField.getText().trim());
                    workingSettings.setInt(KotlinCodeStyleSettings.TOP_LEVEL_IMPORT_THRESHOLD, th);
                } catch (Exception ignored) {}
            }
            if (staticsGroup != null && staticsGroup.getSelectedToggle() != null) {
                Object ud = staticsGroup.getSelectedToggle().getUserData();
                if (ud != null) {
                    workingSettings.setString(KotlinCodeStyleSettings.JAVA_STATICS_IMPORT_MODE, ud.toString());
                }
            }
            if (staticsThresholdField != null) {
                try {
                    int th = Integer.parseInt(staticsThresholdField.getText().trim());
                    workingSettings.setInt(KotlinCodeStyleSettings.JAVA_STATICS_IMPORT_THRESHOLD, th);
                } catch (Exception ignored) {}
            }
            if (nestedClassesCheckBox != null) {
                workingSettings.setBoolean(KotlinCodeStyleSettings.INSERT_IMPORTS_FOR_NESTED_CLASSES, nestedClassesCheckBox.isSelected());
            }
            if (importAliasesCheckBox != null) {
                workingSettings.setBoolean(KotlinCodeStyleSettings.IMPORT_ALIASES_SEPARATELY, importAliasesCheckBox.isSelected());
            }
            if (workingSettings instanceof KotlinCodeStyleSettings kSettings) {
                if (packagesOnDemandTable != null) {
                    kSettings.getPackagesToUseImportOnDemand().clear();
                    for (KotlinCodeStyleSettings.ImportEntry ie : packagesOnDemandTable.getItems()) {
                        kSettings.getPackagesToUseImportOnDemand().add(ie.copy());
                    }
                }
                if (importLayoutTable != null) {
                    kSettings.getImportLayout().clear();
                    for (KotlinCodeStyleSettings.ImportEntry ie : importLayoutTable.getItems()) {
                        kSettings.getImportLayout().add(ie.copy());
                    }
                }
            }
            return;
        }

        for (Map.Entry<String, CheckBox> e : checkboxControls.entrySet()) {
            if (isStandardIndentOption(e.getKey())) {
                setStandardIndentBoolean(e.getKey(), e.getValue().isSelected());
            } else {
                workingSettings.setBoolean(e.getKey(), e.getValue().isSelected());
            }
        }
        for (Map.Entry<String, TextField> e : numberControls.entrySet()) {
            try {
                int val = Integer.parseInt(e.getValue().getText().trim());
                if (isStandardIndentOption(e.getKey())) {
                    setStandardIndentInt(e.getKey(), val);
                } else {
                    workingSettings.setInt(e.getKey(), val);
                }
            } catch (Exception ignored) {}
        }
        for (Map.Entry<String, ComboBox<String>> e : comboControls.entrySet()) {
            if (e.getValue().getValue() != null) {
                workingSettings.setString(e.getKey(), e.getValue().getValue());
            }
        }
        for (Map.Entry<String, TextField> e : textControls.entrySet()) {
            if (e.getValue().getText() != null) {
                workingSettings.setString(e.getKey(), e.getValue().getText());
            }
        }
    }

    public void loadFromCurrentScheme() {
        CodeStyleScheme scheme = CodeStyleSettings.getInstance().getActiveScheme();
        if (scheme == null) return;
        LanguageCodeStyleSettings current = scheme.getLanguageSettings(this.languageId);
        if (current == null) return;

        suppressEvents = true;
        try {
            workingSettings = current.copy();
            baselineSettings = current.copy();
            renderActiveTabControls();
            headerBar.updateWarningBanner();
        } finally {
            suppressEvents = false;
        }
    }

    public void apply() {
        saveCurrentTabUiToWorkingSettings();

        CodeStyleScheme scheme = CodeStyleSettings.getInstance().getActiveScheme();
        if (scheme == null) return;

        LanguageCodeStyleSettings target = scheme.getLanguageSettings(this.languageId);
        if (target != null) {
            target.setUseTabCharacter(workingSettings.isUseTabCharacter());
            target.setSmartTabs(workingSettings.isSmartTabs());
            target.setTabSize(workingSettings.getTabSize());
            target.setIndent(workingSettings.getIndent());
            target.setContinuationIndent(workingSettings.getContinuationIndent());
            target.setKeepIndentsOnEmptyLines(workingSettings.isKeepIndentsOnEmptyLines());
            target.setLabelIndent(workingSettings.getLabelIndent());
            target.setAbsoluteLabelIndent(workingSettings.isAbsoluteLabelIndent());
            target.setDoNotIndentTopLevelMembers(workingSettings.isDoNotIndentTopLevelMembers());
            target.setUseIndentsRelativeToExpressionStart(workingSettings.isUseIndentsRelativeToExpressionStart());
            target.setProperties(workingSettings.getAllProperties());

            if (workingSettings instanceof KotlinCodeStyleSettings kCur && target instanceof KotlinCodeStyleSettings kTarget) {
                kTarget.getPackagesToUseImportOnDemand().clear();
                for (KotlinCodeStyleSettings.ImportEntry e : kCur.getPackagesToUseImportOnDemand()) {
                    kTarget.getPackagesToUseImportOnDemand().add(e.copy());
                }
                kTarget.getImportLayout().clear();
                for (KotlinCodeStyleSettings.ImportEntry e : kCur.getImportLayout()) {
                    kTarget.getImportLayout().add(e.copy());
                }
            }

            if (workingSettings instanceof AngularHtmlCodeStyleSettings aCur && target instanceof AngularHtmlCodeStyleSettings aTarget) {
                aTarget.getMatchingRules().clear();
                aTarget.getMatchingRules().addAll(aCur.getMatchingRules());
            }

            if (workingSettings instanceof HtmlCodeStyleSettings hCur && target instanceof HtmlCodeStyleSettings hTarget) {
                hTarget.getMatchingRules().clear();
                hTarget.getMatchingRules().addAll(hCur.getMatchingRules());
            }

            if (workingSettings instanceof JavaScriptCodeStyleSettings jsCur && target instanceof JavaScriptCodeStyleSettings jsTarget) {
                jsTarget.getMatchingRules().clear();
                jsTarget.getMatchingRules().addAll(jsCur.getMatchingRules());
            }

            if (workingSettings instanceof GoCodeStyleSettings gCur && target instanceof GoCodeStyleSettings gTarget) {
                gTarget.getCommentExceptions().clear();
                gTarget.getCommentExceptions().addAll(gCur.getCommentExceptions());
            }

            if (workingSettings instanceof GroovyCodeStyleSettings gCur && target instanceof GroovyCodeStyleSettings gTarget) {
                gTarget.getPackagesToUseImportOnDemand().clear();
                for (GroovyCodeStyleSettings.GroovyImportEntry e : gCur.getPackagesToUseImportOnDemand()) {
                    gTarget.getPackagesToUseImportOnDemand().add(e.copy());
                }
                gTarget.getImportLayout().clear();
                for (GroovyCodeStyleSettings.GroovyImportEntry e : gCur.getImportLayout()) {
                    gTarget.getImportLayout().add(e.copy());
                }
                gTarget.getOrderOfMembers().clear();
                gTarget.getOrderOfMembers().addAll(gCur.getOrderOfMembers());
            }
        }

        CodeStyleSettings.getInstance().saveSettings();
        baselineSettings = workingSettings.copy();
        notifyModified();
    }

    public void reset() {
        loadFromCurrentScheme();
        updatePreview();
        notifyModified();
    }

    public boolean isModified() {
        if (workingSettings == null || baselineSettings == null) return false;
        saveCurrentTabUiToWorkingSettings();

        if (workingSettings.isUseTabCharacter() != baselineSettings.isUseTabCharacter()) return true;
        if (workingSettings.isSmartTabs() != baselineSettings.isSmartTabs()) return true;
        if (workingSettings.getTabSize() != baselineSettings.getTabSize()) return true;
        if (workingSettings.getIndent() != baselineSettings.getIndent()) return true;
        if (workingSettings.getContinuationIndent() != baselineSettings.getContinuationIndent()) return true;
        if (workingSettings.isKeepIndentsOnEmptyLines() != baselineSettings.isKeepIndentsOnEmptyLines()) return true;
        if (workingSettings.getLabelIndent() != baselineSettings.getLabelIndent()) return true;
        if (workingSettings.isAbsoluteLabelIndent() != baselineSettings.isAbsoluteLabelIndent()) return true;
        if (workingSettings.isDoNotIndentTopLevelMembers() != baselineSettings.isDoNotIndentTopLevelMembers()) return true;
        if (workingSettings.isUseIndentsRelativeToExpressionStart() != baselineSettings.isUseIndentsRelativeToExpressionStart()) return true;

        if (workingSettings instanceof KotlinCodeStyleSettings kCur && baselineSettings instanceof KotlinCodeStyleSettings kBase) {
            if (!Objects.equals(kCur.getPackagesToUseImportOnDemand(), kBase.getPackagesToUseImportOnDemand())) return true;
            if (!Objects.equals(kCur.getImportLayout(), kBase.getImportLayout())) return true;
        }

        if (workingSettings instanceof AngularHtmlCodeStyleSettings aCur && baselineSettings instanceof AngularHtmlCodeStyleSettings aBase) {
            if (!Objects.equals(aCur.getMatchingRules(), aBase.getMatchingRules())) return true;
        }

        if (workingSettings instanceof HtmlCodeStyleSettings hCur && baselineSettings instanceof HtmlCodeStyleSettings hBase) {
            if (!Objects.equals(hCur.getMatchingRules(), hBase.getMatchingRules())) return true;
        }

        if (workingSettings instanceof JavaScriptCodeStyleSettings jsCur && baselineSettings instanceof JavaScriptCodeStyleSettings jsBase) {
            if (!Objects.equals(jsCur.getMatchingRules(), jsBase.getMatchingRules())) return true;
        }

        if (workingSettings instanceof GoCodeStyleSettings gCur && baselineSettings instanceof GoCodeStyleSettings gBase) {
            if (!Objects.equals(gCur.getCommentExceptions(), gBase.getCommentExceptions())) return true;
        }

        if (workingSettings instanceof GroovyCodeStyleSettings gCur && baselineSettings instanceof GroovyCodeStyleSettings gBase) {
            if (!Objects.equals(gCur.getPackagesToUseImportOnDemand(), gBase.getPackagesToUseImportOnDemand())) return true;
            if (!Objects.equals(gCur.getImportLayout(), gBase.getImportLayout())) return true;
            if (!Objects.equals(gCur.getOrderOfMembers(), gBase.getOrderOfMembers())) return true;
        }

        Map<String, Object> curProps = workingSettings.getAllProperties();
        Map<String, Object> baseProps = baselineSettings.getAllProperties();

        if (!Objects.equals(curProps, baseProps)) return true;

        return false;
    }

    private void applyPredefinedGoogleStyle() {
        suppressEvents = true;
        try {
            workingSettings.setUseTabCharacter(false);
            workingSettings.setTabSize(2);
            workingSettings.setIndent(2);
            workingSettings.setContinuationIndent(4);
            renderActiveTabControls();
        } finally {
            suppressEvents = false;
        }
        updatePreview();
        notifyModified();
    }

    private void applyPredefinedDefaultStyle() {
        LanguageCodeStyleProvider p = LanguageCodeStyleProvider.getProvider(languageId);
        if (p == null) return;
        LanguageCodeStyleSettings def = p.createDefaultSettings();

        suppressEvents = true;
        try {
            workingSettings = def.copy();
            renderActiveTabControls();
        } finally {
            suppressEvents = false;
        }
        updatePreview();
        notifyModified();
    }

    private void copyFromOtherLanguage(String otherLangId) {
        CodeStyleScheme scheme = CodeStyleSettings.getInstance().getActiveScheme();
        if (scheme == null) return;
        LanguageCodeStyleSettings other = scheme.getLanguageSettings(otherLangId);
        if (other == null) return;

        suppressEvents = true;
        try {
            workingSettings.setUseTabCharacter(other.isUseTabCharacter());
            workingSettings.setSmartTabs(other.isSmartTabs());
            workingSettings.setTabSize(other.getTabSize());
            workingSettings.setIndent(other.getIndent());
            workingSettings.setContinuationIndent(other.getContinuationIndent());
            workingSettings.setKeepIndentsOnEmptyLines(other.isKeepIndentsOnEmptyLines());
            workingSettings.setLabelIndent(other.getLabelIndent());
            renderActiveTabControls();
        } finally {
            suppressEvents = false;
        }
        updatePreview();
        notifyModified();
    }

    /**
     * Formats and renders the live preview in real time.
     */
    public void updatePreview() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(languageId);
        String rawSample = provider != null ? provider.getSampleCode(activeTab) : "// No preview available";

        String formattedCode = CodeStyleSettings.formatCodeSample(rawSample, workingSettings);

        // Apply interactive transforms based on active settings
        formattedCode = applyInteractiveTransforms(formattedCode);

        previewLinesBox.getChildren().clear();
        String[] lines = formattedCode.split("\n", -1);

        for (String line : lines) {
            TextFlow flow = buildLineTextFlow(line);
            previewLinesBox.getChildren().add(flow);
        }
    }

    private String applyInteractiveTransforms(String code) {
        if (code == null) return "";

        // Range operator space
        boolean rangeSpaces = workingSettings.getBoolean(KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, false);
        if (rangeSpaces) {
            code = code.replace("10..<42", "10 ..< 42").replace("10..42", "10 .. 42");
        } else {
            code = code.replace("10 ..< 42", "10..<42").replace("10 .. 42", "10..42");
        }

        // Before parentheses spaces
        if (!workingSettings.getBoolean(KotlinCodeStyleSettings.SPACE_BEFORE_WHEN_PARENTHESES, true)) {
            code = code.replace("when (", "when(");
        }
        if (!workingSettings.getBoolean(KotlinCodeStyleSettings.SPACE_BEFORE_IF_PARENTHESES, true)) {
            code = code.replace("if (", "if(");
        }
        if (!workingSettings.getBoolean(KotlinCodeStyleSettings.SPACE_BEFORE_WHILE_PARENTHESES, true)) {
            code = code.replace("while (", "while(");
        }
        if (!workingSettings.getBoolean(KotlinCodeStyleSettings.SPACE_BEFORE_CATCH_PARENTHESES, true)) {
            code = code.replace("catch (", "catch(");
        }

        // Elvis operator space
        if (!workingSettings.getBoolean(KotlinCodeStyleSettings.SPACE_AROUND_ELVIS_OPERATOR, true)) {
            code = code.replace(" ?: ", "?:");
        }

        // Colon type space
        if (!workingSettings.getBoolean(KotlinCodeStyleSettings.SPACE_AFTER_COLON_BEFORE_DECLARATION_TYPE, true)) {
            code = code.replace(": Int", ":Int").replace(": String", ":String");
        }

        // Arrow spaces
        if (!workingSettings.getBoolean(KotlinCodeStyleSettings.SPACE_AROUND_ARROW_IN_WHEN_CLAUSE, true)) {
            code = code.replace(" -> ", "->");
        }

        // Braces placement
        if (workingSettings.getBoolean(KotlinCodeStyleSettings.WRAP_PUT_LEFT_BRACE_ON_NEW_LINE, false)) {
            code = code.replaceAll(" (\\{)", "\n$1");
        }

        // Angular HTML template transforms
        if (workingSettings instanceof AngularHtmlCodeStyleSettings || "Angular HTML template".equals(languageId)) {
            boolean spacesWithin = workingSettings.getBoolean(AngularHtmlCodeStyleSettings.SPACES_WITHIN_INTERPOLATIONS, true);
            if (!spacesWithin) {
                code = code.replace("{{ user.name }}", "{{user.name}}");
            }

            boolean newLineAfterOpen = workingSettings.getBoolean(AngularHtmlCodeStyleSettings.WRAP_NEW_LINE_AFTER_OPEN_INTERPOLATION, true);
            boolean newLineBeforeClose = workingSettings.getBoolean(AngularHtmlCodeStyleSettings.WRAP_NEW_LINE_BEFORE_CLOSE_INTERPOLATION, true);
            if (!newLineAfterOpen && !newLineBeforeClose) {
                code = code.replace("{{\n      user.name\n   }}", "{{ user.name }}");
            } else if (!newLineAfterOpen) {
                code = code.replace("{{\n      user.name", "{{ user.name");
            } else if (!newLineBeforeClose) {
                code = code.replace("user.name\n   }}", "user.name }}");
            }
        }

        // EditorConfig transforms
        if (workingSettings instanceof EditorConfigCodeStyleSettings || "EditorConfig".equals(languageId)) {
            boolean spacesAroundSep = workingSettings.getBoolean(EditorConfigCodeStyleSettings.SPACES_AROUND_SEPARATOR, true);
            if (!spacesAroundSep) {
                code = code.replace(" = ", "=");
            }

            boolean beforeColon = workingSettings.getBoolean(EditorConfigCodeStyleSettings.SPACES_BEFORE_COLON, false);
            boolean afterColon = workingSettings.getBoolean(EditorConfigCodeStyleSettings.SPACES_AFTER_COLON, false);
            if (beforeColon && afterColon) {
                code = code.replace("value4:value5", "value4 : value5");
            } else if (beforeColon) {
                code = code.replace("value4:value5", "value4 :value5");
            } else if (afterColon) {
                code = code.replace("value4:value5", "value4: value5");
            }

            boolean beforeComma = workingSettings.getBoolean(EditorConfigCodeStyleSettings.SPACES_BEFORE_COMMA, false);
            boolean afterComma = workingSettings.getBoolean(EditorConfigCodeStyleSettings.SPACES_AFTER_COMMA, true);
            if (beforeComma && !afterComma) {
                code = code.replace("value1, value2, value3", "value1 ,value2 ,value3");
            } else if (beforeComma) {
                code = code.replace("value1, value2, value3", "value1 , value2 , value3");
            } else if (!afterComma) {
                code = code.replace("value1, value2, value3", "value1,value2,value3");
            }

            boolean alignColumns = workingSettings.getBoolean(EditorConfigCodeStyleSettings.WRAP_ALIGN_FIELDS_IN_COLUMNS, false);
            if (alignColumns) {
                code = code.replace("charset = utf-8\nkey = value1", "charset = utf-8\nkey     = value1")
                           .replace("key = value1, value2, value3\nkey2 = value4:value5", "key     = value1, value2, value3\nkey2    = value4:value5");
            }
        }

        // Go transforms
        if (workingSettings instanceof GoCodeStyleSettings || "Go".equals(languageId)) {
            boolean leadingSpace = workingSettings.getBoolean(GoCodeStyleSettings.OTHER_ADD_LEADING_SPACE_COMMENTS, false);
            if (leadingSpace) {
                code = code.replace("//Foo docs", "// Foo docs");
            } else {
                code = code.replace("// Foo docs", "//Foo docs");
            }

            boolean useBackquotes = workingSettings.getBoolean(GoCodeStyleSettings.IMPORTS_USE_BACKQUOTES, false);
            if (useBackquotes) {
                code = code.replace("\"bytes\"", "`bytes`")
                           .replace("\"fmt\"", "`fmt`")
                           .replace("\"localPackage\"", "`localPackage`")
                           .replace("\"appengine\"", "`appengine`")
                           .replace("\"errors\"", "`errors`");
            }
        }

        // Groovy transforms
        if (workingSettings instanceof GroovyCodeStyleSettings || "Groovy".equals(languageId)) {
            // Before parentheses
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_IF_PARENTHESES, true)) {
                code = code.replace("if (", "if(");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_FOR_PARENTHESES, true)) {
                code = code.replace("for (", "for(");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_WHILE_PARENTHESES, true)) {
                code = code.replace("while (", "while(");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_CATCH_PARENTHESES, true)) {
                code = code.replace("catch (", "catch(");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_SWITCH_PARENTHESES, true)) {
                code = code.replace("switch (", "switch(");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_TRY_PARENTHESES, true)) {
                code = code.replace("try (", "try(");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_SYNCHRONIZED_PARENTHESES, true)) {
                code = code.replace("synchronized (", "synchronized(");
            }
            if (workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_METHOD_DECLARATION_PARENTHESES, false)) {
                code = code.replace("foo(int x", "foo (int x").replace("inject(x)", "inject (x)");
            }
            if (workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES, false)) {
                code = code.replace("obtainResource()", "obtainResource ()")
                           .replace("getCode()", "getCode ()")
                           .replace("operation()", "operation ()")
                           .replace("ckl(2)", "ckl (2)");
            }

            // Around operators
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS, true)) {
                code = code.replace(" = ", "=").replace(" += ", "+=");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_LOGICAL_OPERATORS, true)) {
                code = code.replace(" && ", "&&");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_EQUALITY_OPERATORS, true)) {
                code = code.replace(" != ", "!=");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_RELATIONAL_OPERATORS, true)) {
                code = code.replace(" < ", "<").replace(" >= ", ">=");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_AROUND_LAMBDA_ARROW, true)) {
                code = code.replace(" -> ", "->");
            }

            // Ternary
            boolean beforeQ = workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_TERNARY_QUESTION, true);
            boolean afterQ = workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_AFTER_TERNARY_QUESTION, true);
            boolean beforeC = workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_TERNARY_COLON, true);
            boolean afterC = workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_AFTER_TERNARY_COLON, true);
            String q = (beforeQ ? " " : "") + "?" + (afterQ ? " " : "");
            String c = (beforeC ? " " : "") + ":" + (afterC ? " " : "");
            code = code.replace(" ? ", q).replace(" : ", c);

            // Assert
            boolean beforeAssert = workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_BEFORE_ASSERT_SEPARATOR, false);
            boolean afterAssert = workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_AFTER_ASSERT_SEPARATOR, true);
            String assertSep = (beforeAssert ? " " : "") + ":" + (afterAssert ? " " : "");
            code = code.replace(": message", assertSep + "message");

            // Named argument
            boolean beforeNamed = workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_IN_NAMED_ARGUMENT_BEFORE_COLON, false);
            boolean afterNamed = workingSettings.getBoolean(GroovyCodeStyleSettings.SPACE_IN_NAMED_ARGUMENT_AFTER_COLON, true);
            String namedSep = (beforeNamed ? " " : "") + ":" + (afterNamed ? " " : "");
            code = code.replace(": \"foo\"", namedSep + "\"foo\"").replace(": e", namedSep + "e");

            // Wrapping and Braces transforms
            String classBrace = workingSettings.getString(GroovyCodeStyleSettings.BRACE_PLACEMENT_CLASS, "End of line");
            if ("Next line".equals(classBrace)) {
                code = code.replace("class Foo {", "class Foo\n{")
                           .replace("I4, I5 {", "I4, I5\n{");
            }
            String methodBrace = workingSettings.getString(GroovyCodeStyleSettings.BRACE_PLACEMENT_METHOD, "End of line");
            if ("Next line".equals(methodBrace)) {
                code = code.replace(") {", ")\n{");
            }
            if (workingSettings.getBoolean(GroovyCodeStyleSettings.WRAP_IF_ELSE_ON_NEW_LINE, false)) {
                code = code.replace("} else", "}\nelse");
            }
            if (workingSettings.getBoolean(GroovyCodeStyleSettings.WRAP_TRY_CATCH_ON_NEW_LINE, false)) {
                code = code.replace("} catch", "}\ncatch");
            }
            if (workingSettings.getBoolean(GroovyCodeStyleSettings.WRAP_TRY_FINALLY_ON_NEW_LINE, false)) {
                code = code.replace("} finally", "}\nfinally");
            }
            if (!workingSettings.getBoolean(GroovyCodeStyleSettings.WRAP_SWITCH_INDENT_CASE_BRANCHES, true)) {
                code = code.replace("            case 0:", "        case 0:")
                           .replace("            default:", "        default:");
            }
        }

        // HTML transforms
        if (workingSettings instanceof HtmlCodeStyleSettings || "HTML".equals(languageId)) {
            // Tab 1: Tabs and Indents
            if (workingSettings.getBoolean(HtmlCodeStyleSettings.USE_HTML_INDENTS_WITHIN_STYLE_AND_SCRIPT, false)) {
                code = code.replace("        var current_lang", "            var current_lang")
                           .replace("        var i18n_info", "            var i18n_info")
                           .replace("        var english_only", "            var english_only")
                           .replace("        .page__beam", "            .page__beam");
            }

            // Tab 2: Other
            // Spaces around '=' in attributes
            if (workingSettings.getBoolean(HtmlCodeStyleSettings.SPACES_AROUND_EQUALITY_IN_ATTRIBUTE, false)) {
                code = code.replace("=\"", " = \"").replace("='", " = '");
            }

            // Spaces after tag name
            if (workingSettings.getBoolean(HtmlCodeStyleSettings.SPACES_AFTER_TAG_NAME, false)) {
                code = code.replace("<html ", "<html  ")
                           .replace("<head>", "<head >")
                           .replace("<body>", "<body >")
                           .replace("<div ", "<div  ")
                           .replace("<div>", "<div >");
            }

            // Spaces in empty tag
            if (!workingSettings.getBoolean(HtmlCodeStyleSettings.SPACES_IN_EMPTY_TAG, false)) {
                code = code.replace(" />", "/>");
            } else {
                code = code.replace("/>", " />").replace("  />", " />");
            }

            // Generated quote marks
            String quotes = workingSettings.getString(HtmlCodeStyleSettings.GENERATED_QUOTE_MARKS, "Double");
            if ("Single".equals(quotes)) {
                code = code.replace("=\"", "='").replace("\" ", "' ").replace("\">", "'>");
            }

            // Wrap attributes
            String wrapAttr = workingSettings.getString(HtmlCodeStyleSettings.WRAP_ATTRIBUTES, "Wrap if long");
            if ("Do not wrap".equals(wrapAttr)) {
                code = code.replace("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\"\n      xml:lang=\"en\">",
                                    "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">")
                           .replace("<meta http-equiv=\"Content-Type\"\n          content=\"text/html; charset=iso-8859-1\"/>",
                                    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"/>")
                           .replace("<img\n                src=\"../img/logo.gif\" width=\"124\"\n                height=\"44\" alt=\"Lumina home\"/>",
                                    "<img src=\"../img/logo.gif\" width=\"124\" height=\"44\" alt=\"Lumina home\"/>");
            } else if ("Wrap always".equals(wrapAttr) || "Chop down if long".equals(wrapAttr)) {
                code = code.replace("<link rel=\"stylesheet\" type=\"text/css\" media=\"screen\"",
                                    "<link\n        rel=\"stylesheet\"\n        type=\"text/css\"\n        media=\"screen\"")
                           .replace("<link rel=\"stylesheet\" type=\"text/css\" media=\"print\"",
                                    "<link\n        rel=\"stylesheet\"\n        type=\"text/css\"\n        media=\"print\"");
            }

            // Align attributes
            if (!workingSettings.getBoolean(HtmlCodeStyleSettings.ALIGN_ATTRIBUTES, true)) {
                code = code.replace("      xml:lang=\"en\">", "    xml:lang=\"en\">")
                           .replace("          content=\"text/html;", "    content=\"text/html;")
                           .replace("                height=\"44\"", "        height=\"44\"");
            }

            // Keep blank lines
            int keepBlank = workingSettings.getInt(HtmlCodeStyleSettings.KEEP_BLANK_LINES, 2);
            if (keepBlank == 0) {
                code = code.replaceAll("(?m)^\\s*$\\n+", "");
            } else if (keepBlank == 1) {
                code = code.replaceAll("(?m)^\\s*$\\n(\\s*$\\n)+", "\n");
            }
        }

        // HTTP Request transforms
        if (workingSettings instanceof HttpRequestCodeStyleSettings || "HTTP Request".equals(languageId)) {
            // URL parts indent
            int urlPartsIndent = workingSettings.getInt(HttpRequestCodeStyleSettings.URL_PARTS_INDENT, 4);
            if (urlPartsIndent != 4) {
                String indentSpaces = " ".repeat(Math.max(0, urlPartsIndent));
                code = code.replace("    my-url-path?", indentSpaces + "my-url-path?");
            }

            // Tabs and indents general indent
            int ind = workingSettings.getIndent();
            if (ind != 4) {
                String outer = " ".repeat(ind);
                String inner = " ".repeat(ind * 2);
                code = code.replace("    if (response.status", outer + "if (response.status")
                           .replace("    } else {", outer + "} else {")
                           .replace("    }", outer + "}")
                           .replace("        client.log(\"Success\")", inner + "client.log(\"Success\")")
                           .replace("        client.log(\"Other\")", inner + "client.log(\"Other\")");
            }

            // Wrapping and Braces
            String queryWrap = workingSettings.getString(HttpRequestCodeStyleSettings.QUERY_PARAMS_WRAP, "Wrap if long");
            if ("Do not wrap".equals(queryWrap)) {
                code = code.replace("GET https://localhost:8080/my-url-path?\n    param1=value1&\n    param2=value2&p3=v3",
                                    "GET https://localhost:8080/my-url-path?param1=value1&param2=value2&p3=v3");
            }

            String formWrap = workingSettings.getString(HttpRequestCodeStyleSettings.FORM_URLENCODED_PARAMS_WRAP, "Wrap always");
            if ("Do not wrap".equals(formWrap)) {
                code = code.replace("param1 = value1 &\nparam2 = value2 &\nparam3 = value3",
                                    "param1 = value1 & param2 = value2 & param3 = value3");
            }

            // Spaces
            boolean spacesAroundEq = workingSettings.getBoolean(HttpRequestCodeStyleSettings.SPACES_AROUND_EQUALITY_IN_FORM, true);
            boolean spaceBeforeAmp = workingSettings.getBoolean(HttpRequestCodeStyleSettings.SPACE_BEFORE_AMPERSAND_IN_FORM, true);
            if (!spacesAroundEq) {
                code = code.replace(" = ", "=");
            }
            if (!spaceBeforeAmp) {
                code = code.replace(" &", "&");
            }
        }

        // JavaScript transforms
        if (workingSettings instanceof JavaScriptCodeStyleSettings || "JavaScript".equals(languageId)) {
            // Tab / Indent
            if (workingSettings.isUseTabCharacter()) {
                code = code.replace("    ", "\t");
            }

            // Indent chained methods
            boolean indentChained = workingSettings.getBoolean(JavaScriptCodeStyleSettings.INDENT_CHAINED_METHODS, true);
            if (!indentChained) {
                code = code.replace("            .secondCall();", "        .secondCall();");
            }

            // Quote style
            String quoteStyle = workingSettings.getString(JavaScriptCodeStyleSettings.QUOTE_STYLE, "double");
            if ("Double".equalsIgnoreCase(quoteStyle)) {
                code = code.replace("'react'", "\"react\"")
                           .replace("'World'", "\"World\"")
                           .replace("'Lumina'", "\"Lumina\"")
                           .replace("','", "\",\"")
                           .replace("'a'", "\"a\"")
                           .replace("'b'", "\"b\"")
                           .replace("'transparent'", "\"transparent\"")
                           .replace("'lime'", "\"lime\"")
                           .replace("'Arial'", "\"Arial\"")
                           .replace("'one'", "\"one\"")
                           .replace("'two'", "\"two\"")
                           .replace("'three'", "\"three\"")
                           .replace("'btn.gif'", "\"btn.gif\"")
                           .replace("'bold'", "\"bold\"")
                           .replace("'red'", "\"red\"");
            } else if ("Single".equalsIgnoreCase(quoteStyle)) {
                code = code.replace("\"demo\"", "'demo'")
                           .replace("\"Demo\"", "'Demo'")
                           .replace("\"greeting\"", "'greeting'")
                           .replace("\"zero\"", "'zero'")
                           .replace("\"one\"", "'one'")
                           .replace("\"String\"", "'String'")
                           .replace("\"Int\"", "'Int'")
                           .replace("\"btn.gif\"", "'btn.gif'")
                           .replace("\"bold\"", "'bold'")
                           .replace("\"red\"", "'red'");
            }

            // Semicolons
            String semicolons = workingSettings.getString(JavaScriptCodeStyleSettings.USE_SEMICOLON, "Use");
            String semicolonScope = workingSettings.getString(JavaScriptCodeStyleSettings.SEMICOLON_SCOPE, "in code generated by IDE");
            if ("Don't use".equals(semicolons)) {
                code = code.replace("yield current;", "yield current")
                           .replace("current + next);", "current + next)")
                           .replace("var i = 0;", "var i = 0")
                           .replace("\"one\"};", "\"one\"}")
                           .replace("[0, 1, 2];", "[0, 1, 2]")
                           .replace(".secondCall();", ".secondCall()")
                           .replace("field1 = 1;", "field1 = 1")
                           .replace("return 0;", "return 0")
                           .replace("font = 'Arial';", "font = 'Arial'")
                           .replace("initial = -1;", "initial = -1")
                           .replace("break;", "break")
                           .replace("};", "}")
                           .replace("];", "]");
            } else if ("Use".equals(semicolons) && "always".equals(semicolonScope)) {
                if (code.contains("width: 128\n}")) {
                    code = code.replace("width: 128\n}", "width: 128\n};");
                }
                if (code.contains("const cssClasses = [\"bold\", \"red\",]") && !code.contains("const cssClasses = [\"bold\", \"red\",];")) {
                    code = code.replace("const cssClasses = [\"bold\", \"red\",]", "const cssClasses = [\"bold\", \"red\",];");
                }
                if (code.contains("const cssClasses = ['bold', 'red',]") && !code.contains("const cssClasses = ['bold', 'red',];")) {
                    code = code.replace("const cssClasses = ['bold', 'red',]", "const cssClasses = ['bold', 'red',];");
                }
            }

            // Trailing comma
            String trailingComma = workingSettings.getString(JavaScriptCodeStyleSettings.TRAILING_COMMA, "Keep");
            if ("Remove".equals(trailingComma)) {
                code = code.replace("\"red\",", "\"red\"").replace("'red',", "'red'");
            }

            // Blank Lines
            int keepBlankInCode = workingSettings.getInt(JavaScriptCodeStyleSettings.BLANK_LINES_KEEP_IN_CODE, 2);
            if (keepBlankInCode == 0) {
                code = code.replaceAll("(?m)^\\s*$\\n+", "");
            } else if (keepBlankInCode == 1) {
                code = code.replaceAll("(?m)^\\s*$\\n(\\s*$\\n)+", "\n");
            }

            int afterImports = workingSettings.getInt(JavaScriptCodeStyleSettings.BLANK_LINES_AFTER_IMPORTS, 1);
            if (afterImports == 0) {
                code = code.replace("from 'utils';\n\nclass Foo", "from 'utils';\nclass Foo");
            } else if (afterImports == 2) {
                code = code.replace("from 'utils';\n\nclass Foo", "from 'utils';\n\n\nclass Foo");
            }

            int aroundMethod = workingSettings.getInt(JavaScriptCodeStyleSettings.BLANK_LINES_AROUND_METHOD, 1);
            if (aroundMethod == 0) {
                code = code.replace("    field2 = 2;\n\n    foo()", "    field2 = 2;\n    foo()")
                           .replace("    }\n\n    static bar()", "    }\n    static bar()");
            } else if (aroundMethod == 2) {
                code = code.replace("    field2 = 2;\n\n    foo()", "    field2 = 2;\n\n\n    foo()")
                           .replace("    }\n\n    static bar()", "    }\n\n\n    static bar()");
            }

            // Spaces around operators
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS, true)) {
                code = code.replace(" = ", "=");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_ARROW_FUNCTION, true)) {
                code = code.replace(" => ", "=>");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_ADDITIVE_OPERATORS, true)) {
                code = code.replace(" + ", "+");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS, true)) {
                code = code.replace(" * ", "*");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_RELATIONAL_OPERATORS, true)) {
                code = code.replace(" > ", ">").replace(" < ", "<");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_EQUALITY_OPERATORS, true)) {
                code = code.replace(" == ", "==");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AROUND_LOGICAL_OPERATORS, true)) {
                code = code.replace(" || ", "||").replace(" && ", "&&");
            }

            // Generator star spacing
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_GENERATOR_STAR, false)) {
                code = code.replace("function*", "function *");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AFTER_GENERATOR_STAR, true)) {
                code = code.replace("function* ", "function*").replace("yield* ", "yield*");
            }

            // Rest / spread spacing
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AFTER_REST_SPREAD, false)) {
                code = code.replace("...rest", "... rest");
            }

            // Property name-value separator ':'
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_PROPERTY_NAME_VALUE_SEPARATOR, false)) {
                code = code.replace(": \"", " : \"").replace(": '", " : '").replace(": {", " : {");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AFTER_PROPERTY_NAME_VALUE_SEPARATOR, true)) {
                code = code.replace(": \"", ":\"").replace(": '", ":'").replace(": {", ":{");
            }

            // Spaces before left brace
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FUNCTION_LEFT_BRACE, true)) {
                code = code.replace(") {", "){");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_ELSE_LEFT_BRACE, true)) {
                code = code.replace("else {", "else{");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_TRY_LEFT_BRACE, true)) {
                code = code.replace("try {", "try{");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_DO_LEFT_BRACE, true)) {
                code = code.replace("do {", "do{");
            }

            // Spaces before keywords
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_ELSE_KEYWORD, true)) {
                code = code.replace("} else", "}else");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_WHILE_KEYWORD, true)) {
                code = code.replace("} while", "}while");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_CATCH_KEYWORD, true)) {
                code = code.replace("} catch", "}catch");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FINALLY_KEYWORD, true)) {
                code = code.replace("} finally", "}finally");
            }

            // Spaces before parentheses
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FUNCTION_DECLARATION_PARENTHESES, false)) {
                code = code.replace("foo(x", "foo (x").replace("fibonacci(current", "fibonacci (current");
            }
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FUNCTION_CALL_PARENTHESES, false)) {
                code = code.replace("take(fibonacci()", "take (fibonacci ()");
            }

            // Space after comma
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_AFTER_COMMA, true)) {
                code = code.replace(", ", ",");
            }
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_COMMA, false)) {
                code = code.replace(", ", " , ");
            }

            // For loop semicolon
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACE_BEFORE_FOR_SEMICOLON, false)) {
                code = code.replace("j = 0; j < 10; j++", "j = 0 ; j < 10 ; j++");
            }

            // Spaces within array / object braces
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_ARRAY_BRACKETS, false)) {
                code = code.replace("[0, 1, 2]", "[ 0, 1, 2 ]").replace("[first, second, ...rest]", "[ first, second, ...rest ]");
            }
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.SPACES_WITHIN_OBJECT_LITERAL_BRACES, false)) {
                code = code.replace("{0: \"zero\", 1: \"one\"}", "{ 0: \"zero\", 1: \"one\" }");
            }

            // Wrapping and Braces transforms
            if ("Next line".equals(workingSettings.getString(JavaScriptCodeStyleSettings.BRACE_PLACEMENT_CLASS, "End of line"))) {
                code = code.replace("class Foo extends BarComponent implements BazService, QuuxProvider {",
                                    "class Foo extends BarComponent implements BazService, QuuxProvider\n{");
            }
            if ("Next line".equals(workingSettings.getString(JavaScriptCodeStyleSettings.BRACE_PLACEMENT_FUNCTION, "End of line"))) {
                code = code.replace("function buzz() {", "function buzz()\n{");
            }
            if (!workingSettings.getBoolean(JavaScriptCodeStyleSettings.WRAP_SWITCH_INDENT_CASE_BRANCHES, true)) {
                code = code.replace("        case 0:", "    case 0:").replace("        case 1:", "    case 1:");
            }
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.WRAP_IF_ELSE_ON_NEW_LINE, false)) {
                code = code.replace("} else", "}\nelse");
            }
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.WRAP_TRY_CATCH_ON_NEW_LINE, false)) {
                code = code.replace("} catch", "}\ncatch");
            }
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.WRAP_TRY_FINALLY_ON_NEW_LINE, false)) {
                code = code.replace("} finally", "}\nfinally");
            }
            if (workingSettings.getBoolean(JavaScriptCodeStyleSettings.WRAP_DO_WHILE_ON_NEW_LINE, false)) {
                code = code.replace("} while", "}\nwhile");
            }
            if ("Do not wrap".equals(workingSettings.getString(JavaScriptCodeStyleSettings.WRAP_CLASS_DECORATORS, "Wrap always"))) {
                code = code.replace("@ClassDecorator(param1, param2)\n@ClassDecoratorExt\nclass Foo",
                                    "@ClassDecorator(param1, param2) @ClassDecoratorExt class Foo");
            }
        }

        return code;
    }

    private TextFlow buildLineTextFlow(String line) {
        TextFlow flow = new TextFlow();
        flow.setPrefWidth(Region.USE_COMPUTED_SIZE);

        int leadingWhitespace = 0;
        while (leadingWhitespace < line.length() && (line.charAt(leadingWhitespace) == ' ' || line.charAt(leadingWhitespace) == '\t')) {
            leadingWhitespace++;
        }

        String indentPrefix = line.substring(0, leadingWhitespace);
        String codeText = line.substring(leadingWhitespace);

        // Render indent guides (subtle · dots for spaces, → for tabs)
        if (!indentPrefix.isEmpty()) {
            StringBuilder guideBuilder = new StringBuilder();
            for (char c : indentPrefix.toCharArray()) {
                if (c == '\t') {
                    guideBuilder.append("→   ");
                } else {
                    guideBuilder.append("·");
                }
            }
            Text indentGuideText = new Text(guideBuilder.toString());
            indentGuideText.setFont(Font.font("monospace", 12));
            indentGuideText.setFill(Color.web("#393B40"));
            flow.getChildren().add(indentGuideText);
        }

        // Syntax highlighting
        highlightAndAppend(flow, codeText);
        return flow;
    }

    private void highlightAndAppend(TextFlow flow, String code) {
        if (code.isEmpty()) return;

        Pattern tokenPattern = Pattern.compile(
                "(</?[A-Za-z0-9_-]+|/?>|>)|" +
                "(<%={1,2}|<%-?|-?%>|%>)|" +
                "(@(if|else\\s+if|else))|" +
                "(\\{\\{|\\}\\})|" +
                "(\\*ng[A-Za-z0-9_]+|#[A-Za-z0-9_-]+|\\[[^\\]\\r\\n]+\\])|" +
                "(//.*|/\\*.*?\\*/|;.*|<!--.*?-->|###.*)|" +
                "\\b(public|private|protected|class|interface|enum|record|void|int|long|boolean|char|float|double|" +
                "try|catch|finally|throw|throws|if|else|do|while|for|switch|case|default|break|continue|return|" +
                "new|package|import|extends|implements|static|final|fun|val|var|open|where|in|init|context|def|type|func|struct|fn|let|mut|" +
                "async|await|const|export|from|when|root|charset|end_of_line|insert_final_newline|trim_trailing_whitespace|indent_style|indent_size|true|false|" +
                "println|print|assert|synchronized|go|chan|defer|select|map|nil|iota|each|end|function|yield|GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\\b|" +
                "(:[a-zA-Z0-9_]+|@[a-zA-Z0-9_]+(\\([^)]*\\))?)|" +
                "(\"[^\"]*\"|'[^']*'|`[^`]*`)|" +
                "(\\b\\d+\\b)|" +
                "(\\b[A-Z][a-zA-Z0-9_]*\\b)"
        );

        Matcher matcher = tokenPattern.matcher(code);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                Text plain = new Text(code.substring(lastEnd, matcher.start()));
                plain.setFont(Font.font("monospace", 12));
                plain.setFill(Color.web("#BCBEC4"));
                flow.getChildren().add(plain);
            }

            Text token = new Text(matcher.group());
            token.setFont(Font.font("monospace", 12));

            if (matcher.group(1) != null) {
                // HTML tag or bracket
                token.setFill(Color.web("#E8BF6A"));
            } else if (matcher.group(2) != null) {
                // ERB delimiter
                token.setFill(Color.web("#E8BF6A"));
            } else if (matcher.group(3) != null) {
                // Angular control flow
                token.setFill(Color.web("#CF8E6D"));
            } else if (matcher.group(4) != null) {
                // Interpolation {{ }}
                token.setFill(Color.web("#E8BF6A"));
            } else if (matcher.group(5) != null) {
                // Directives / bindings / Section headers
                token.setFill(Color.web("#BBB529"));
            } else if (matcher.group(6) != null) {
                // Comment (gray)
                token.setFill(Color.web("#7A7E85"));
            } else if (matcher.group(7) != null) {
                // Keyword (orange/peach)
                token.setFill(Color.web("#CF8E6D"));
            } else if (matcher.group(8) != null) {
                // Ruby symbol or annotation (gold/amber)
                token.setFill(Color.web("#BBB529"));
            } else if (matcher.group(9) != null) {
                // String (green)
                token.setFill(Color.web("#6AAB73"));
            } else if (matcher.group(10) != null) {
                // Number (cyan)
                token.setFill(Color.web("#2AACB8"));
            } else if (matcher.group(11) != null) {
                // Class/Type (yellow/teal)
                token.setFill(Color.web("#56A8F5"));
            } else {
                token.setFill(Color.web("#BCBEC4"));
            }

            flow.getChildren().add(token);
            lastEnd = matcher.end();
        }

        if (lastEnd < code.length()) {
            Text plain = new Text(code.substring(lastEnd));
            plain.setFont(Font.font("monospace", 12));
            plain.setFill(Color.web("#BCBEC4"));
            flow.getChildren().add(plain);
        }
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public CodeStyleHeaderBar getHeaderBar() {
        return headerBar;
    }

    public String getLanguageId() {
        return languageId;
    }

    public String getActiveTab() {
        return activeTab;
    }

    public void setActiveTab(String tabName) {
        if (tabName != null && !tabName.equals(activeTab)) {
            saveCurrentTabUiToWorkingSettings();
            this.activeTab = tabName;
            for (Toggle t : tabGroup.getToggles()) {
                if (t instanceof ToggleButton btn && tabName.equals(btn.getText())) {
                    btn.setSelected(true);
                    break;
                }
            }
            renderActiveTabControls();
            updatePreview();
        }
    }

    public String getSampleForActiveTab() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(languageId);
        return provider != null ? provider.getSampleCode(activeTab) : "";
    }

    public LanguageCodeStyleSettings getCurrentSettings() {
        saveCurrentTabUiToWorkingSettings();
        return workingSettings;
    }
}
