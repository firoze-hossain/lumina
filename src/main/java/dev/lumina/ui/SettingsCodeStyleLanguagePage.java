package dev.lumina.ui;

import dev.lumina.settings.CodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOptionType;
import dev.lumina.settings.CodeStyleSettings.CodeStyleScheme;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;
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

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsCodeStyleLanguagePage(String languageId) {
        this.languageId = languageId != null ? languageId : "Kotlin";

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
            renderImportsTab();
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
                || "keep_indents_on_empty_lines".equals(key);
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
            default -> 4;
        };
    }

    private void setStandardIndentInt(String key, int val) {
        switch (key) {
            case "tab_size" -> workingSettings.setTabSize(val);
            case "indent" -> workingSettings.setIndent(val);
            case "continuation_indent" -> workingSettings.setContinuationIndent(val);
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
        table.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
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

    private void saveCurrentTabUiToWorkingSettings() {
        if ("Imports".equals(activeTab)) {
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
                "\\b(public|private|protected|class|interface|enum|record|void|int|long|boolean|char|float|double|" +
                "try|catch|finally|throw|throws|if|else|do|while|for|switch|case|default|break|continue|return|" +
                "new|package|import|extends|implements|static|final|fun|val|var|open|where|in|init|context|def|type|func|struct|fn|let|mut|" +
                "async|await|const|export|from|when)\\b|" +
                "(@[A-Za-z0-9_]+(\\([^)]*\\))?)|" +
                "(\"[^\"]*\")|" +
                "(\\b\\d+\\b)|" +
                "(\\b[A-Z][a-zA-Z0-9_]*\\b)|" +
                "(//.*|/\\*.*\\*/)"
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
                // Keyword (orange/peach)
                token.setFill(Color.web("#CF8E6D"));
            } else if (matcher.group(2) != null) {
                // Annotation (gold/amber)
                token.setFill(Color.web("#BBB529"));
            } else if (matcher.group(3) != null) {
                // String (green)
                token.setFill(Color.web("#6AAB73"));
            } else if (matcher.group(4) != null) {
                // Number (cyan)
                token.setFill(Color.web("#2AACB8"));
            } else if (matcher.group(5) != null) {
                // Class/Type (yellow/teal)
                token.setFill(Color.web("#56A8F5"));
            } else if (matcher.group(6) != null) {
                // Comment (gray)
                token.setFill(Color.web("#7A7E85"));
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
