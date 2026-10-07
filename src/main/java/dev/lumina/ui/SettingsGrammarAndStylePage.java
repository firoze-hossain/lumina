package dev.lumina.ui;

import dev.lumina.naturallang.NaturalLanguagesManager;
import dev.lumina.naturallang.rules.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;

import java.util.*;

/**
 * Settings page for Editor > Natural Languages > Grammar and Style.
 * Faithfully reproduces the design, layout, and dynamic rule management from the reference IDE:
 * - Segmented tab bar: [Scope] | [Rules] | [Exceptions]
 * - Scope tab:
 *     - "File types:" scrollable checklist (all 18 file types: ChatInput, Go, HTML, JSON, Java,
 *       JavaScript, Kotlin, Markdown, Plain text, Properties, Python, Ruby, Rust, SQL, Scala,
 *       TOML, XML, YAML)
 *     - "Check in:" checkboxes (String literals, Comments, Documentation, Commit messages)
 * - Rules tab (Screenshots 1-5):
 *     - Domain, Writing style, Language dropdown filters
 *     - Live keyword search: "Find rules that match keywords or phrases"
 *     - Categorized rules sections: General, Punctuation, Typography, Readability
 *     - Dynamic rule cards with CheckBox, option parameters (ComboBoxes), descriptions,
 *       "Learn more ↗" links, expandable/inline examples, and cloud connectivity indicators
 * - Exceptions tab: Ignored words and regex patterns
 * - Full dirty-tracking and apply/reset lifecycle integrated with NaturalLanguagesManager and ProofreadingRulesManager
 */
public class SettingsGrammarAndStylePage extends VBox {

    private final NaturalLanguagesManager manager = NaturalLanguagesManager.getInstance();
    private final ProofreadingRulesManager rulesManager = ProofreadingRulesManager.getInstance();

    // Segmented tabs
    private final ToggleGroup tabGroup = new ToggleGroup();
    private final ToggleButton scopeTabBtn = new ToggleButton("Scope");
    private final ToggleButton rulesTabBtn = new ToggleButton("Rules");
    private final ToggleButton exceptionsTabBtn = new ToggleButton("Exceptions");

    private final StackPane tabContentContainer = new StackPane();

    // Scope Tab Controls
    private final Map<String, CheckBox> fileTypeCheckboxes = new LinkedHashMap<>();
    private final CheckBox checkStringLiterals = new CheckBox("String literals");
    private final CheckBox checkComments = new CheckBox("Comments");
    private final CheckBox checkDocumentation = new CheckBox("Documentation");
    private final CheckBox checkCommitMessages = new CheckBox("Commit messages");

    // Rules Tab Controls
    private final ComboBox<String> domainCombo = new ComboBox<>();
    private final ComboBox<String> writingStyleCombo = new ComboBox<>();
    private final ComboBox<String> languageCombo = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final VBox rulesContainer = new VBox(16);
    private final Map<String, CheckBox> ruleCheckBoxes = new HashMap<>();
    private final Map<String, ComboBox<String>> ruleOptionCombos = new HashMap<>();

    // "Other rules" hierarchical tree controls (Screenshots 3, 4, 5)
    private final Map<String, CheckBox> treeCategoryCheckBoxes = new LinkedHashMap<>();
    private final Map<String, CheckBox> treeRuleCheckBoxes = new LinkedHashMap<>();
    private final Map<String, VBox> treeCategoryChildrenBoxes = new LinkedHashMap<>();
    private final Map<String, Label> treeCategoryArrowLabels = new LinkedHashMap<>();
    private final Set<String> userExpandedCategories = new HashSet<>();

    // Exceptions Tab Controls (Image 1)
    private final javafx.collections.ObservableList<String> exceptionsList = javafx.collections.FXCollections.observableArrayList();
    private final ListView<String> exceptionsListView = new ListView<>(exceptionsList);
    private final Button removeExceptionBtn = new Button("—");
    private final VBox emptyExceptionsPlaceholder = new VBox(4);

    private final BooleanProperty modifiedProperty = new SimpleBooleanProperty(false);
    private Runnable onModifiedListener;
    private boolean updatingUI = false;

    // Rules that have inline examples shown expanded by default in screenshots
    private static final Set<String> INLINE_EXAMPLE_RULES = Set.of(
            "punctuation.avoid_exclamations",
            "typography.use_curly_apostrophes",
            "typography.add_space_between_number_and_unit",
            "typography.format_large_numbers",
            "readability.prefer_active_voice",
            "readability.avoid_all_passive_constructions",
            "readability.avoid_prepositional_chains",
            "readability.consistent_verb_forms",
            "readability.long_sentences"
    );

    public SettingsGrammarAndStylePage() {
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 18, 24));
        setSpacing(14);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();

        manager.setOnModifiedListener(() -> {
            notifyModified();
        });
        rulesManager.addOnModifiedListener(() -> {
            notifyModified();
        });
    }

    private void buildUI() {
        getChildren().clear();

        // 1. Segmented Tab Bar [Scope] [Rules] [Exceptions]
        HBox tabButtonBar = buildSegmentedTabBar();

        // 2. Tab Content Container
        VBox.setVgrow(tabContentContainer, Priority.ALWAYS);

        Node scopePane = buildScopeTabPane();
        Node rulesPane = buildRulesTabPane();
        Node exceptionsPane = buildExceptionsTabPane();

        tabGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == rulesTabBtn) {
                tabContentContainer.getChildren().setAll(rulesPane);
            } else if (newV == exceptionsTabBtn) {
                tabContentContainer.getChildren().setAll(exceptionsPane);
            } else {
                tabContentContainer.getChildren().setAll(scopePane);
            }
        });

        scopeTabBtn.setSelected(true);
        tabContentContainer.getChildren().setAll(scopePane);

        getChildren().addAll(tabButtonBar, tabContentContainer);
    }

    private HBox buildSegmentedTabBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);

        scopeTabBtn.setToggleGroup(tabGroup);
        rulesTabBtn.setToggleGroup(tabGroup);
        exceptionsTabBtn.setToggleGroup(tabGroup);

        styleSegmentedButton(scopeTabBtn, true, false);
        styleSegmentedButton(rulesTabBtn, false, false);
        styleSegmentedButton(exceptionsTabBtn, false, true);

        bar.getChildren().addAll(scopeTabBtn, rulesTabBtn, exceptionsTabBtn);
        return bar;
    }

    private void styleSegmentedButton(ToggleButton btn, boolean isLeft, boolean isRight) {
        String radius = isLeft ? "4 0 0 4" : (isRight ? "0 4 4 0" : "0");
        String baseStyle = "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 14; -fx-border-color: #3E4147; -fx-border-width: 1; -fx-border-radius: " + radius + "; -fx-background-radius: " + radius + "; -fx-cursor: hand;";
        String selectedStyle = "-fx-background-color: #35538F; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 3 14; -fx-border-color: #4C70BA; -fx-border-width: 1; -fx-border-radius: " + radius + "; -fx-background-radius: " + radius + "; -fx-cursor: hand;";

        btn.setStyle(btn.isSelected() ? selectedStyle : baseStyle);
        btn.selectedProperty().addListener((obs, oldV, isSel) -> {
            btn.setStyle(isSel ? selectedStyle : baseStyle);
        });
    }

    // =========================================================================
    // Tab 1: Scope
    // =========================================================================

    private Node buildScopeTabPane() {
        HBox columnsBox = new HBox(36);
        columnsBox.setAlignment(Pos.TOP_LEFT);
        columnsBox.setPadding(new Insets(6, 0, 0, 0));

        // Column 1: File types
        VBox fileTypesCol = new VBox(6);
        Label fileTypesHeader = new Label("File types:");
        fileTypesHeader.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        VBox fileTypesListBox = new VBox(4);
        fileTypesListBox.setPadding(new Insets(6, 8, 6, 8));
        fileTypesListBox.setStyle("-fx-background-color: #1E1F22;");

        for (String type : NaturalLanguagesManager.DEFAULT_FILE_TYPES) {
            CheckBox cb = new CheckBox(type);
            cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
            cb.setOnAction(e -> {
                if (!updatingUI) {
                    manager.setFileTypeEnabled(type, cb.isSelected());
                    notifyModified();
                }
            });
            fileTypeCheckboxes.put(type, cb);
            fileTypesListBox.getChildren().add(cb);
        }

        ScrollPane scroll = new ScrollPane(fileTypesListBox);
        scroll.setPrefWidth(180);
        scroll.setMaxWidth(220);
        scroll.setPrefHeight(270);
        scroll.setStyle("-fx-background-color: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        scroll.setFitToWidth(true);

        fileTypesCol.getChildren().addAll(fileTypesHeader, scroll);

        // Column 2: Check in
        VBox checkInCol = new VBox(8);
        Label checkInHeader = new Label("Check in:");
        checkInHeader.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        checkStringLiterals.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        checkComments.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        checkDocumentation.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        checkCommitMessages.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");

        checkStringLiterals.setOnAction(e -> {
            if (!updatingUI) {
                manager.setCheckStringLiterals(checkStringLiterals.isSelected());
                notifyModified();
            }
        });
        checkComments.setOnAction(e -> {
            if (!updatingUI) {
                manager.setCheckComments(checkComments.isSelected());
                notifyModified();
            }
        });
        checkDocumentation.setOnAction(e -> {
            if (!updatingUI) {
                manager.setCheckDocumentation(checkDocumentation.isSelected());
                notifyModified();
            }
        });
        checkCommitMessages.setOnAction(e -> {
            if (!updatingUI) {
                manager.setCheckCommitMessages(checkCommitMessages.isSelected());
                notifyModified();
            }
        });

        VBox checkInList = new VBox(6, checkStringLiterals, checkComments, checkDocumentation, checkCommitMessages);
        checkInCol.getChildren().addAll(checkInHeader, checkInList);

        columnsBox.getChildren().addAll(fileTypesCol, checkInCol);
        return columnsBox;
    }

    // =========================================================================
    // Tab 2: Rules (Screenshots 1-5)
    // =========================================================================

    private Node buildRulesTabPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(6, 0, 0, 0));
        VBox.setVgrow(box, Priority.ALWAYS);

        // 1. Description Header
        Label descLabel = new Label("Define how writing is checked in different contexts and languages. Each domain may have its own style and rules.");
        descLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        // 2. Selectors Grid / Rows
        VBox selectorsBox = new VBox(8);

        // Row 1: Domain and Writing style
        HBox row1 = new HBox(12);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label domainLabel = new Label("Domain:");
        domainLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        domainCombo.getItems().setAll(ProofreadingRulesManager.DOMAINS);
        domainCombo.setValue(rulesManager.getDomain());
        styleComboBox(domainCombo, 160);
        domainCombo.setOnAction(e -> {
            if (!updatingUI) {
                rulesManager.setDomain(domainCombo.getValue());
                notifyModified();
            }
        });

        Label styleLabel = new Label("Writing style:");
        styleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 0 0 0 16;");
        writingStyleCombo.getItems().setAll(ProofreadingRulesManager.WRITING_STYLES);
        writingStyleCombo.setValue(rulesManager.getWritingStyle());
        styleComboBox(writingStyleCombo, 180);
        writingStyleCombo.setOnAction(e -> {
            if (!updatingUI) {
                rulesManager.setWritingStyle(writingStyleCombo.getValue());
                notifyModified();
            }
        });

        row1.getChildren().addAll(domainLabel, domainCombo, styleLabel, writingStyleCombo);

        // Row 2: Language
        HBox row2 = new HBox(12);
        row2.setAlignment(Pos.CENTER_LEFT);

        Label langLabel = new Label("Language:");
        langLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        languageCombo.getItems().setAll(ProofreadingRulesManager.LANGUAGES);
        languageCombo.setValue(rulesManager.getLanguage());
        styleComboBox(languageCombo, 160);
        languageCombo.setOnAction(e -> {
            if (!updatingUI) {
                rulesManager.setLanguage(languageCombo.getValue());
                notifyModified();
            }
        });

        row2.getChildren().addAll(langLabel, languageCombo);
        selectorsBox.getChildren().addAll(row1, row2);

        // 3. Search Field
        HBox searchBox = new HBox(6);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #3E4147; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 2 8;");
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");

        searchField.setPromptText("Find rules that match keywords or phrases");
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 2 4;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldText, newText) -> {
            rebuildRulesList(newText);
        });

        searchBox.getChildren().addAll(searchIcon, searchField);

        // 4. Scrollable Rules Catalog
        rulesContainer.setPadding(new Insets(8, 0, 16, 0));
        rulesContainer.setStyle("-fx-background-color: #1E1F22;");

        ScrollPane scroll = new ScrollPane(rulesContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(380);
        scroll.setStyle("-fx-background-color: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Build initial rules list
        rebuildRulesList("");

        box.getChildren().addAll(descLabel, selectorsBox, searchBox, scroll);
        return box;
    }

    private void rebuildRulesList(String query) {
        rulesContainer.getChildren().clear();
        ruleCheckBoxes.clear();
        ruleOptionCombos.clear();
        treeCategoryCheckBoxes.clear();
        treeRuleCheckBoxes.clear();
        treeCategoryChildrenBoxes.clear();
        treeCategoryArrowLabels.clear();

        List<ProofreadingRule> allMatching = rulesManager.getFilteredRules(query);
        List<ProofreadingTreeCategory> matchingTreeCats = rulesManager.getFilteredTreeCategories(query);

        if (allMatching.isEmpty() && matchingTreeCats.isEmpty()) {
            Label noMatch = new Label("No rules found matching \"" + query + "\"");
            noMatch.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px; -fx-padding: 16;");
            rulesContainer.getChildren().add(noMatch);
            return;
        }

        // 1. Render high-level rule categories (General, Punctuation, Typography, Readability, Formality, Inclusivity)
        for (ProofreadingRuleCategory category : ProofreadingRuleCategory.values()) {
            List<ProofreadingRule> categoryRules = allMatching.stream()
                    .filter(r -> r.getCategory() == category)
                    .toList();

            if (categoryRules.isEmpty()) continue;

            // Category Header with horizontal divider line
            VBox catHeaderBox = new VBox(4);
            HBox catTitleRow = new HBox(8);
            catTitleRow.setAlignment(Pos.CENTER_LEFT);

            Label catTitle = new Label(category.getDisplayName());
            catTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #393B40;");
            HBox.setHgrow(sep, Priority.ALWAYS);

            catTitleRow.getChildren().addAll(catTitle, sep);
            catHeaderBox.getChildren().add(catTitleRow);
            catHeaderBox.setPadding(new Insets(10, 0, 4, 0));
            rulesContainer.getChildren().add(catHeaderBox);

            // Rules inside this category
            for (ProofreadingRule rule : categoryRules) {
                Node ruleNode = buildRuleNode(rule);
                rulesContainer.getChildren().add(ruleNode);
            }
        }

        // 2. Render "Other rules" hierarchical tree (Screenshots 3, 4, 5)
        if (!matchingTreeCats.isEmpty()) {
            VBox otherHeaderBox = new VBox(4);
            HBox otherTitleRow = new HBox(8);
            otherTitleRow.setAlignment(Pos.CENTER_LEFT);

            Label otherTitle = new Label("Other rules");
            otherTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #393B40;");
            HBox.setHgrow(sep, Priority.ALWAYS);

            otherTitleRow.getChildren().addAll(otherTitle, sep);
            otherHeaderBox.getChildren().add(otherTitleRow);
            otherHeaderBox.setPadding(new Insets(14, 0, 4, 0));
            rulesContainer.getChildren().add(otherHeaderBox);

            // Scrollable tree view container matching Screenshots 3 & 4
            VBox treeBox = new VBox(2);
            treeBox.setPadding(new Insets(4, 6, 8, 6));
            treeBox.setStyle("-fx-background-color: #1E1F22;");

            boolean isSearching = query != null && !query.trim().isEmpty();
            for (ProofreadingTreeCategory cat : matchingTreeCats) {
                Node catNode = buildTreeCategoryNode(cat, isSearching);
                treeBox.getChildren().add(catNode);
            }

            ScrollPane treeScroll = new ScrollPane(treeBox);
            treeScroll.setFitToWidth(true);
            treeScroll.setPrefHeight(240);
            treeScroll.setMaxHeight(320);
            treeScroll.setStyle("-fx-background-color: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
            rulesContainer.getChildren().add(treeScroll);
        }
    }

    private Node buildTreeCategoryNode(ProofreadingTreeCategory cat, boolean isSearching) {
        VBox catBox = new VBox(0);

        HBox headerRow = new HBox(6);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(3, 4, 3, 4));

        // Arrow disclosure label
        Label arrowLbl = new Label("▶");
        arrowLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 10px; -fx-min-width: 12px; -fx-cursor: hand;");
        treeCategoryArrowLabels.put(cat.getName(), arrowLbl);

        // Tri-state category checkbox
        CheckBox catCb = new CheckBox();
        catCb.setAllowIndeterminate(true);
        updateCategoryCheckBoxState(cat.getName(), catCb);
        catCb.setStyle("-fx-cursor: hand;");
        treeCategoryCheckBoxes.put(cat.getName(), catCb);

        // Category title
        Label catTitleLbl = new Label(cat.getName());
        catTitleLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");

        headerRow.getChildren().addAll(arrowLbl, catCb, catTitleLbl);

        // Indented children container
        VBox childrenBox = new VBox(2);
        childrenBox.setPadding(new Insets(2, 0, 4, 24));
        treeCategoryChildrenBoxes.put(cat.getName(), childrenBox);

        for (ProofreadingTreeRule rule : cat.getRules()) {
            HBox ruleRow = new HBox(6);
            ruleRow.setAlignment(Pos.CENTER_LEFT);
            ruleRow.setPadding(new Insets(1, 4, 1, 4));

            CheckBox ruleCb = new CheckBox(rule.getName());
            ruleCb.setSelected(rulesManager.isTreeRuleEnabled(rule.getId()));
            ruleCb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
            treeRuleCheckBoxes.put(rule.getId(), ruleCb);

            ruleCb.setOnAction(e -> {
                if (!updatingUI) {
                    rulesManager.setTreeRuleEnabled(rule.getId(), ruleCb.isSelected());
                    updateCategoryCheckBoxState(cat.getName(), catCb);
                    notifyModified();
                }
            });

            ruleRow.getChildren().add(ruleCb);
            childrenBox.getChildren().add(ruleRow);
        }

        catCb.setOnAction(e -> {
            if (!updatingUI) {
                boolean selectAll = catCb.isSelected();
                rulesManager.setCategoryEnabled(cat.getName(), selectAll);
                for (ProofreadingTreeRule rule : cat.getRules()) {
                    CheckBox childCb = treeRuleCheckBoxes.get(rule.getId());
                    if (childCb != null) {
                        childCb.setSelected(selectAll);
                    }
                }
                updateCategoryCheckBoxState(cat.getName(), catCb);
                notifyModified();
            }
        });

        // Expand/collapse logic: expand automatically if search query matches child rule
        boolean expanded = isSearching || userExpandedCategories.contains(cat.getName());
        childrenBox.setVisible(expanded);
        childrenBox.setManaged(expanded);
        arrowLbl.setText(expanded ? "▼" : "▶");

        Runnable toggleExpand = () -> {
            boolean nextState = !childrenBox.isVisible();
            childrenBox.setVisible(nextState);
            childrenBox.setManaged(nextState);
            arrowLbl.setText(nextState ? "▼" : "▶");
            if (nextState) {
                userExpandedCategories.add(cat.getName());
            } else {
                userExpandedCategories.remove(cat.getName());
            }
        };

        arrowLbl.setOnMouseClicked(e -> toggleExpand.run());
        catTitleLbl.setOnMouseClicked(e -> toggleExpand.run());

        catBox.getChildren().addAll(headerRow, childrenBox);
        return catBox;
    }

    private void updateCategoryCheckBoxState(String catName, CheckBox catCb) {
        boolean isFull = rulesManager.isCategoryFullyEnabled(catName);
        boolean isPartial = rulesManager.isCategoryPartiallyEnabled(catName);
        if (isPartial) {
            catCb.setIndeterminate(true);
            catCb.setSelected(false);
        } else if (isFull) {
            catCb.setIndeterminate(false);
            catCb.setSelected(true);
        } else {
            catCb.setIndeterminate(false);
            catCb.setSelected(false);
        }
    }

    private Node buildRuleNode(ProofreadingRule rule) {
        VBox ruleBox = new VBox(4);
        ruleBox.setPadding(new Insets(2, 0, 8, 16));

        // 1. CheckBox and optional parameter ComboBox row
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox cb = new CheckBox(rule.getTitle());
        cb.setSelected(rulesManager.isRuleEnabled(rule.getId()));
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        cb.setOnAction(e -> {
            if (!updatingUI) {
                rulesManager.setRuleEnabled(rule.getId(), cb.isSelected());
                notifyModified();
            }
        });
        ruleCheckBoxes.put(rule.getId(), cb);
        topRow.getChildren().add(cb);

        if (rule.hasOption()) {
            ComboBox<String> optionCombo = new ComboBox<>();
            optionCombo.getItems().setAll(rule.getOption().choices());
            String currentValue = rulesManager.getRuleOptionValue(rule.getId());
            optionCombo.setValue(currentValue != null ? currentValue : rule.getOption().defaultChoice());
            styleComboBox(optionCombo, 260);

            optionCombo.setOnAction(e -> {
                if (!updatingUI) {
                    rulesManager.setRuleOptionValue(rule.getId(), optionCombo.getValue());
                    notifyModified();
                }
            });
            ruleOptionCombos.put(rule.getId(), optionCombo);
            topRow.getChildren().add(optionCombo);
        }

        ruleBox.getChildren().add(topRow);

        // 2. Description and "Learn more ↗" row
        VBox detailsBox = new VBox(3);
        detailsBox.setPadding(new Insets(0, 0, 0, 22));

        FlowPane descPane = new FlowPane();
        descPane.setHgap(4);
        descPane.setVgap(2);

        Label descText = new Label(rule.getDescription());
        descText.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
        descText.setWrapText(true);
        descPane.getChildren().add(descText);

        if (rule.isRequiresCloud()) {
            Label cloudText = new Label("This rule is only available when connected to Lumina AI Cloud.");
            cloudText.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
            Hyperlink cloudLearnMore = new Hyperlink("Learn more ↗");
            styleHyperlink(cloudLearnMore);
            descPane.getChildren().addAll(cloudText, cloudLearnMore);
        }

        detailsBox.getChildren().add(descPane);

        // 3. Expanded details (for "Prefer contractions")
        if (rule.hasExpandedDetails()) {
            Label expandDetailLabel = new Label(rule.getExpandedDetails());
            expandDetailLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");
            expandDetailLabel.setWrapText(true);
            expandDetailLabel.setVisible(false);
            expandDetailLabel.setManaged(false);

            Hyperlink expandLink = new Hyperlink("Expand");
            styleHyperlink(expandLink);
            expandLink.setOnAction(e -> {
                boolean nextState = !expandDetailLabel.isVisible();
                expandDetailLabel.setVisible(nextState);
                expandDetailLabel.setManaged(nextState);
                expandLink.setText(nextState ? "Collapse" : "Expand");
            });

            detailsBox.getChildren().addAll(expandDetailLabel, expandLink);
        }

        // 4. Examples rendering
        if (rule.hasExamples()) {
            boolean isInline = INLINE_EXAMPLE_RULES.contains(rule.getId());

            VBox examplesBox = new VBox(2);
            examplesBox.setPadding(new Insets(2, 0, 0, 0));

            for (RuleExample ex : rule.getExamples()) {
                Label exLbl = new Label("Example: " + ex.example());
                exLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");
                exLbl.setWrapText(true);
                examplesBox.getChildren().add(exLbl);

                if (ex.hasCorrection()) {
                    Label corrLbl = new Label("Corrected: " + ex.corrected());
                    corrLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");
                    corrLbl.setWrapText(true);
                    examplesBox.getChildren().add(corrLbl);
                }
            }

            if (isInline) {
                // Shown directly in UI
                detailsBox.getChildren().add(examplesBox);
            } else {
                // Collapsible via "Show examples" link
                examplesBox.setVisible(false);
                examplesBox.setManaged(false);

                Hyperlink showExamplesLink = new Hyperlink("Show examples");
                styleHyperlink(showExamplesLink);
                showExamplesLink.setOnAction(e -> {
                    boolean nextState = !examplesBox.isVisible();
                    examplesBox.setVisible(nextState);
                    examplesBox.setManaged(nextState);
                    showExamplesLink.setText(nextState ? "Hide examples" : "Show examples");
                });

                detailsBox.getChildren().addAll(examplesBox, showExamplesLink);
            }
        }

        ruleBox.getChildren().add(detailsBox);
        return ruleBox;
    }

    private void styleComboBox(ComboBox<String> combo, double prefWidth) {
        combo.setPrefWidth(prefWidth);
        combo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #3E4147; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-padding: 1 4;");
    }

    private void styleHyperlink(Hyperlink link) {
        link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
        link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
    }

    // =========================================================================
    // Tab 3: Exceptions
    // =========================================================================

    private Node buildExceptionsTabPane() {
        VBox box = new VBox(0);
        box.setPadding(new Insets(6, 0, 0, 0));
        VBox.setVgrow(box, Priority.ALWAYS);

        VBox tableContainer = new VBox(0);
        tableContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        // Top toolbar matching Image 1: remove ("—") button
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(3, 8, 3, 8));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        removeExceptionBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8; -fx-border-color: transparent;");
        removeExceptionBtn.setDisable(true);
        removeExceptionBtn.setOnAction(e -> {
            String sel = exceptionsListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                manager.removeGrammarException(sel);
                refreshExceptions();
                notifyModified();
            }
        });

        toolbar.getChildren().add(removeExceptionBtn);

        // StackPane with ListView and centered empty state placeholder
        StackPane listStack = new StackPane();
        VBox.setVgrow(listStack, Priority.ALWAYS);

        exceptionsListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: transparent;");
        exceptionsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 6;");
                }
            }
        });

        exceptionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            removeExceptionBtn.setDisable(sel == null);
        });

        emptyExceptionsPlaceholder.setAlignment(Pos.CENTER);
        Label emptyTitle = new Label("No exceptions added.");
        emptyTitle.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 13px;");
        Label emptySub = new Label("To add an exception, press Alt+Enter on a grammar mistake.");
        emptySub.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        emptyExceptionsPlaceholder.getChildren().setAll(emptyTitle, emptySub);

        emptyExceptionsPlaceholder.visibleProperty().bind(javafx.beans.binding.Bindings.isEmpty(exceptionsList));
        emptyExceptionsPlaceholder.managedProperty().bind(emptyExceptionsPlaceholder.visibleProperty());

        listStack.getChildren().addAll(exceptionsListView, emptyExceptionsPlaceholder);
        tableContainer.getChildren().addAll(toolbar, listStack);
        box.getChildren().add(tableContainer);
        return box;
    }

    private void refreshExceptions() {
        exceptionsList.setAll(manager.getGrammarExceptions());
    }

    private void notifyModified() {
        modifiedProperty.set(isModified());
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void loadData() {
        updatingUI = true;
        try {
            // Scope tab
            for (Map.Entry<String, CheckBox> entry : fileTypeCheckboxes.entrySet()) {
                entry.getValue().setSelected(manager.isFileTypeEnabled(entry.getKey()));
            }

            checkStringLiterals.setSelected(manager.isCheckStringLiterals());
            checkComments.setSelected(manager.isCheckComments());
            checkDocumentation.setSelected(manager.isCheckDocumentation());
            checkCommitMessages.setSelected(manager.isCheckCommitMessages());

            // Rules tab
            domainCombo.setValue(rulesManager.getDomain());
            writingStyleCombo.setValue(rulesManager.getWritingStyle());
            languageCombo.setValue(rulesManager.getLanguage());

            for (Map.Entry<String, CheckBox> entry : ruleCheckBoxes.entrySet()) {
                entry.getValue().setSelected(rulesManager.isRuleEnabled(entry.getKey()));
            }

            for (Map.Entry<String, ComboBox<String>> entry : ruleOptionCombos.entrySet()) {
                String val = rulesManager.getRuleOptionValue(entry.getKey());
                if (val != null) {
                    entry.getValue().setValue(val);
                }
            }

            for (Map.Entry<String, CheckBox> entry : treeRuleCheckBoxes.entrySet()) {
                entry.getValue().setSelected(rulesManager.isTreeRuleEnabled(entry.getKey()));
            }

            for (Map.Entry<String, CheckBox> entry : treeCategoryCheckBoxes.entrySet()) {
                updateCategoryCheckBoxState(entry.getKey(), entry.getValue());
            }

            refreshExceptions();
        } finally {
            updatingUI = false;
        }
    }

    // =========================================================================
    // Settings Lifecycle
    // =========================================================================

    public boolean isModified() {
        return manager.isModified() || rulesManager.isModified();
    }

    public void apply() {
        manager.apply();
        rulesManager.apply();
        modifiedProperty.set(false);
    }

    public void reset() {
        manager.reset();
        rulesManager.reset();
        modifiedProperty.set(false);
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public BooleanProperty modifiedProperty() {
        return modifiedProperty;
    }

    // Accessors for testing
    public ToggleButton getScopeTabBtn() {
        return scopeTabBtn;
    }

    public ToggleButton getRulesTabBtn() {
        return rulesTabBtn;
    }

    public ToggleButton getExceptionsTabBtn() {
        return exceptionsTabBtn;
    }

    public Map<String, CheckBox> getFileTypeCheckboxes() {
        return Collections.unmodifiableMap(fileTypeCheckboxes);
    }

    public CheckBox getCheckStringLiterals() {
        return checkStringLiterals;
    }

    public CheckBox getCheckComments() {
        return checkComments;
    }

    public CheckBox getCheckDocumentation() {
        return checkDocumentation;
    }

    public CheckBox getCheckCommitMessages() {
        return checkCommitMessages;
    }

    public ComboBox<String> getDomainCombo() {
        return domainCombo;
    }

    public ComboBox<String> getWritingStyleCombo() {
        return writingStyleCombo;
    }

    public ComboBox<String> getLanguageCombo() {
        return languageCombo;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public VBox getRulesContainer() {
        return rulesContainer;
    }

    public CheckBox getRuleCheckBox(String ruleId) {
        return ruleCheckBoxes.get(ruleId);
    }

    public ComboBox<String> getRuleOptionCombo(String ruleId) {
        return ruleOptionCombos.get(ruleId);
    }

    public Map<String, CheckBox> getTreeCategoryCheckBoxes() {
        return Collections.unmodifiableMap(treeCategoryCheckBoxes);
    }

    public Map<String, CheckBox> getTreeRuleCheckBoxes() {
        return Collections.unmodifiableMap(treeRuleCheckBoxes);
    }

    public CheckBox getTreeRuleCheckBox(String ruleId) {
        return treeRuleCheckBoxes.get(ruleId);
    }

    public CheckBox getTreeCategoryCheckBox(String catName) {
        return treeCategoryCheckBoxes.get(catName);
    }

    public Set<String> getUserExpandedCategories() {
        return Collections.unmodifiableSet(userExpandedCategories);
    }

    public void expandCategory(String catName) {
        userExpandedCategories.add(catName);
        VBox box = treeCategoryChildrenBoxes.get(catName);
        if (box != null) {
            box.setVisible(true);
            box.setManaged(true);
        }
        Label arrow = treeCategoryArrowLabels.get(catName);
        if (arrow != null) {
            arrow.setText("▼");
        }
    }

    public ListView<String> getExceptionsListView() {
        return exceptionsListView;
    }

    public Button getRemoveExceptionBtn() {
        return removeExceptionBtn;
    }

    public VBox getEmptyExceptionsPlaceholder() {
        return emptyExceptionsPlaceholder;
    }

    public javafx.collections.ObservableList<String> getExceptionsList() {
        return exceptionsList;
    }
}
