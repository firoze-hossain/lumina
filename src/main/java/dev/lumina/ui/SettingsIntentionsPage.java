package dev.lumina.ui;

import dev.lumina.intentions.IntentionAction;
import dev.lumina.intentions.IntentionRegistry;
import dev.lumina.intentions.IntentionRegistry.TriState;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modern Intentions settings page for Lumina IDE matching reference styling.
 * Displays hierarchical category tree with tri-state checkboxes, real-time filtering,
 * and Before/After code preview panes with line numbers and rectangular intention highlights.
 */
public class SettingsIntentionsPage extends VBox {

    private final IntentionRegistry registry = IntentionRegistry.getInstance();

    private final TreeView<TreeNodeData> treeView = new TreeView<>();
    private final TreeItem<TreeNodeData> rootItem = new TreeItem<>(new TreeNodeData("ROOT", null, null));
    private final TextField searchField = new TextField();

    // Right detail controls
    private final Label descHeaderLabel = new Label();
    private final HBox poweredByBox = new HBox(4);
    private final Label poweredByPrefix = new Label("Powered by: ");
    private final Hyperlink poweredByLink = new Hyperlink();

    private final Label beforeLabel = new Label("Before:");
    private final VBox beforeCodePane = new VBox();

    private final Label afterLabel = new Label("After:");
    private final VBox afterCodePane = new VBox();

    private Runnable onModifiedListener;
    private final BooleanProperty modifiedProperty = new SimpleBooleanProperty(false);

    public SettingsIntentionsPage() {
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(0));
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadTree("");

        // Default selection: select AI Assistant category or first top category
        if (!rootItem.getChildren().isEmpty()) {
            TreeItem<TreeNodeData> first = rootItem.getChildren().get(0);
            treeView.getSelectionModel().select(first);
            updateDetailView(first.getValue());
        }

        registry.addChangeListener(() -> {
            modifiedProperty.set(registry.isModified());
            if (onModifiedListener != null) {
                onModifiedListener.run();
            }
        });
    }

    private void buildUI() {
        getChildren().clear();

        // Master-Detail SplitPane
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        VBox leftColumn = buildLeftColumn();
        VBox rightColumn = buildRightColumn();

        splitPane.getItems().addAll(leftColumn, rightColumn);
        splitPane.setDividerPositions(0.40);

        getChildren().setAll(splitPane);
    }

    // =========================================================================
    // Left Column: Toolbar + Tree
    // =========================================================================

    private VBox buildLeftColumn() {
        VBox col = new VBox(6);
        col.setPadding(new Insets(8, 8, 8, 12));
        col.setStyle("-fx-background-color: #1E1F22;");

        // Toolbar: Expand All (⌄), Collapse All (^), Search (🔍)
        Button expandAllBtn = new Button("⌄");
        expandAllBtn.setTooltip(new Tooltip("Expand All"));
        expandAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-padding: 2 6; -fx-cursor: hand;");
        expandAllBtn.setOnAction(e -> setTreeExpanded(rootItem, true));

        Button collapseAllBtn = new Button("^");
        collapseAllBtn.setTooltip(new Tooltip("Collapse All"));
        collapseAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-padding: 2 6; -fx-cursor: hand;");
        collapseAllBtn.setOnAction(e -> setTreeExpanded(rootItem, false));

        searchField.setPromptText("🔍 Search intentions");
        searchField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, old, text) -> loadTree(text));

        HBox toolbar = new HBox(6, expandAllBtn, collapseAllBtn, searchField);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // TreeView
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        treeView.setCellFactory(tv -> new IntentionTreeCell());
        treeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && selected.getValue() != null) {
                updateDetailView(selected.getValue());
            }
        });

        col.getChildren().addAll(toolbar, treeView);
        return col;
    }

    private void setTreeExpanded(TreeItem<TreeNodeData> item, boolean expanded) {
        if (item != rootItem) {
            item.setExpanded(expanded);
        }
        for (TreeItem<TreeNodeData> child : item.getChildren()) {
            setTreeExpanded(child, expanded);
        }
    }

    // =========================================================================
    // Tree Population & Filtering
    // =========================================================================

    private void loadTree(String filter) {
        String query = filter != null ? filter.trim().toLowerCase() : "";
        rootItem.getChildren().clear();

        for (String cat : registry.getTopCategories()) {
            TreeItem<TreeNodeData> catNode = new TreeItem<>(new TreeNodeData(cat, null, null));
            boolean catMatches = cat.toLowerCase().contains(query);

            Map<String, List<IntentionAction>> subMap = registry.getSubcategories(cat);
            for (Map.Entry<String, List<IntentionAction>> subEntry : subMap.entrySet()) {
                String sub = subEntry.getKey();
                List<IntentionAction> actions = subEntry.getValue();

                if (sub.isEmpty()) {
                    // Direct leaf actions under category
                    for (IntentionAction a : actions) {
                        boolean actionMatches = query.isEmpty() || catMatches || a.getName().toLowerCase().contains(query)
                                || (a.getDescription() != null && a.getDescription().toLowerCase().contains(query));
                        if (actionMatches) {
                            catNode.getChildren().add(new TreeItem<>(new TreeNodeData(a.getName(), a, null)));
                        }
                    }
                } else {
                    // Subcategory
                    boolean subMatches = query.isEmpty() || catMatches || sub.toLowerCase().contains(query);
                    TreeItem<TreeNodeData> subNode = new TreeItem<>(new TreeNodeData(sub, null, cat));

                    for (IntentionAction a : actions) {
                        boolean actionMatches = query.isEmpty() || subMatches || a.getName().toLowerCase().contains(query)
                                || (a.getDescription() != null && a.getDescription().toLowerCase().contains(query));
                        if (actionMatches) {
                            subNode.getChildren().add(new TreeItem<>(new TreeNodeData(a.getName(), a, cat)));
                        }
                    }

                    if (!subNode.getChildren().isEmpty()) {
                        catNode.getChildren().add(subNode);
                        if (!query.isEmpty()) {
                            subNode.setExpanded(true);
                        }
                    }
                }
            }

            if (!catNode.getChildren().isEmpty()) {
                rootItem.getChildren().add(catNode);
                if (!query.isEmpty()) {
                    catNode.setExpanded(true);
                }
            }
        }
    }

    // =========================================================================
    // Right Column: Detail View & Previews
    // =========================================================================

    private VBox buildRightColumn() {
        VBox col = new VBox(14);
        col.setPadding(new Insets(16, 20, 16, 20));
        col.setStyle("-fx-background-color: #1E1F22;");

        // Top description
        descHeaderLabel.setWrapText(true);
        descHeaderLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-line-spacing: 3px;");

        // Powered by plugin link (Image 5)
        poweredByPrefix.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        poweredByLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        poweredByBox.setAlignment(Pos.CENTER_LEFT);
        poweredByBox.getChildren().addAll(poweredByPrefix, poweredByLink);
        poweredByBox.setManaged(false);
        poweredByBox.setVisible(false);

        // Before section
        beforeLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        styleCodePane(beforeCodePane);

        // After section
        afterLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        styleCodePane(afterCodePane);

        VBox beforeBox = new VBox(6, beforeLabel, beforeCodePane);
        VBox afterBox = new VBox(6, afterLabel, afterCodePane);
        VBox.setVgrow(beforeCodePane, Priority.ALWAYS);
        VBox.setVgrow(afterCodePane, Priority.ALWAYS);

        col.getChildren().addAll(descHeaderLabel, poweredByBox, beforeBox, afterBox);
        return col;
    }

    private void styleCodePane(VBox pane) {
        pane.setStyle("-fx-background-color: #121316; -fx-border-color: #2B2D30; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
        pane.setMinHeight(140);
    }

    private void updateDetailView(TreeNodeData data) {
        if (data == null) return;

        if (data.isCategory()) {
            showCategoryGuidance(data.title);
        } else if (data.isSubcategory()) {
            showCategoryGuidance(data.title);
        } else if (data.action != null) {
            showActionDetail(data.action);
        }
    }

    private void showCategoryGuidance(String categoryName) {
        descHeaderLabel.setText("You have selected the intention category " + categoryName + ".\n" +
                "By clicking the checkbox, you can enable/disable all intentions in this category.\n" +
                "To enable/disable a particular intention, select the intention inside this category.");

        poweredByBox.setManaged(false);
        poweredByBox.setVisible(false);

        renderSnippet(beforeCodePane,
                "1|The sample code featuring the selected intention will be shown here.\n" +
                "2|[Flashing rectangle] shows the place where intention is applicable.");

        renderSnippet(afterCodePane,
                "1|The result of applying the intention will be shown here.");
    }

    private void showActionDetail(IntentionAction action) {
        descHeaderLabel.setText(action.getDescription() != null && !action.getDescription().isBlank()
                ? action.getDescription()
                : action.getName() + ".");

        if (action.getPluginName() != null && !action.getPluginName().isBlank()) {
            poweredByLink.setText(action.getPluginName());
            poweredByBox.setManaged(true);
            poweredByBox.setVisible(true);
        } else {
            poweredByBox.setManaged(false);
            poweredByBox.setVisible(false);
        }

        String before = action.getBeforeTemplate() != null ? action.getBeforeTemplate()
                : "[// Intention applicable here]\nfun example() {}";
        String after = action.getAfterTemplate() != null ? action.getAfterTemplate()
                : "// Intention applied result\nfun example() {}";

        renderSnippetWithAutoNumbers(beforeCodePane, before);
        renderSnippetWithAutoNumbers(afterCodePane, after);
    }

    // =========================================================================
    // Syntax & Rectangle Code Renderer
    // =========================================================================

    private void renderSnippetWithAutoNumbers(VBox target, String code) {
        target.getChildren().clear();
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            target.getChildren().add(buildCodeLine(lineNum, lines[i]));
        }
    }

    private void renderSnippet(VBox target, String explicitNumberedCode) {
        target.getChildren().clear();
        String[] lines = explicitNumberedCode.split("\n");
        for (String line : lines) {
            int sep = line.indexOf('|');
            if (sep != -1) {
                int lineNum = Integer.parseInt(line.substring(0, sep).trim());
                String content = line.substring(sep + 1);
                target.getChildren().add(buildCodeLine(lineNum, content));
            } else {
                target.getChildren().add(buildCodeLine(1, line));
            }
        }
    }

    private Node buildCodeLine(int lineNum, String lineText) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        // Line number gutter
        Label numLabel = new Label(String.valueOf(lineNum));
        numLabel.setPrefWidth(24);
        numLabel.setAlignment(Pos.CENTER_RIGHT);
        numLabel.setStyle("-fx-text-fill: #55575E; -fx-font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; -fx-font-size: 12px;");

        // Code tokens flow
        TextFlow codeFlow = new TextFlow();
        codeFlow.setStyle("-fx-font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; -fx-font-size: 12px;");

        // Parse rectangle highlight syntax: "[highlighted text]"
        int openIdx = lineText.indexOf('[');
        int closeIdx = lineText.indexOf(']', openIdx + 1);

        if (openIdx != -1 && closeIdx != -1) {
            String prefix = lineText.substring(0, openIdx);
            String boxed = lineText.substring(openIdx + 1, closeIdx);
            String suffix = lineText.substring(closeIdx + 1);

            appendStyledTokens(codeFlow, prefix);

            Label boxLabel = new Label(boxed.replace(" ", "\u00A0"));
            boxLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; " +
                    "-fx-font-size: 12px; -fx-border-color: #8C9099; -fx-border-style: solid; -fx-border-width: 1px; " +
                    "-fx-border-radius: 2px; -fx-padding: 0 3 0 3; -fx-background-color: #26282E;");
            codeFlow.getChildren().add(boxLabel);

            appendStyledTokens(codeFlow, suffix);
        } else {
            appendStyledTokens(codeFlow, lineText);
        }

        row.getChildren().addAll(numLabel, codeFlow);
        return row;
    }

    private void appendStyledTokens(TextFlow flow, String text) {
        if (text.isEmpty()) return;

        // Keywords, strings, comments, annotations
        Pattern pattern = Pattern.compile(
                "(//.*)|(@[A-Za-z0-9_]+)|(\".*?\")|\\b(class|import|return|val|var|fun|get|by|fun|interface|public|private)\\b"
        );
        Matcher matcher = pattern.matcher(text);
        int lastIdx = 0;

        while (matcher.find()) {
            if (matcher.start() > lastIdx) {
                Text plain = new Text(text.substring(lastIdx, matcher.start()).replace(" ", "\u00A0"));
                plain.setStyle("-fx-fill: #DFE1E5;");
                flow.getChildren().add(plain);
            }

            Text matchedText = new Text(matcher.group().replace(" ", "\u00A0"));
            if (matcher.group(1) != null) {
                // Comment
                matchedText.setStyle("-fx-fill: #7A7E85; -fx-font-style: italic;");
            } else if (matcher.group(2) != null) {
                // Annotation
                matchedText.setStyle("-fx-fill: #BBB529;");
            } else if (matcher.group(3) != null) {
                // String
                matchedText.setStyle("-fx-fill: #6AAB73;");
            } else {
                // Keyword
                matchedText.setStyle("-fx-fill: #CF8E6D; -fx-font-weight: bold;");
            }
            flow.getChildren().add(matchedText);

            lastIdx = matcher.end();
        }

        if (lastIdx < text.length()) {
            Text remaining = new Text(text.substring(lastIdx).replace(" ", "\u00A0"));
            remaining.setStyle("-fx-fill: #DFE1E5;");
            flow.getChildren().add(remaining);
        }
    }

    // =========================================================================
    // Tree Cell with Tri-State Checkboxes
    // =========================================================================

    private static class TreeNodeData {
        final String title;
        final IntentionAction action;
        final String parentCategory;

        TreeNodeData(String title, IntentionAction action, String parentCategory) {
            this.title = title;
            this.action = action;
            this.parentCategory = parentCategory;
        }

        boolean isCategory() { return action == null && parentCategory == null; }
        boolean isSubcategory() { return action == null && parentCategory != null; }
        boolean isAction() { return action != null; }
    }

    private class IntentionTreeCell extends TreeCell<TreeNodeData> {
        private final CheckBox checkBox = new CheckBox();
        private final Label label = new Label();
        private final HBox cellBox = new HBox(6, checkBox, label);

        IntentionTreeCell() {
            cellBox.setAlignment(Pos.CENTER_LEFT);
            checkBox.setAllowIndeterminate(true);
            checkBox.setFocusTraversable(false);
            checkBox.setStyle("-fx-cursor: hand;");

            checkBox.setOnAction(e -> {
                TreeItem<TreeNodeData> item = getTreeItem();
                if (item == null || item.getValue() == null) return;
                TreeNodeData data = item.getValue();

                boolean selected = checkBox.isSelected();
                if (data.isCategory()) {
                    registry.setCategoryEnabled(data.title, selected);
                } else if (data.isSubcategory()) {
                    registry.setSubcategoryEnabled(data.parentCategory, data.title, selected);
                } else if (data.action != null) {
                    registry.setIntentionEnabled(data.action.getId(), selected);
                }
                treeView.refresh();
            });

            selectedProperty().addListener((obs, old, isSel) -> updateStyles(isSel));
        }

        private void updateStyles(boolean selected) {
            if (selected) {
                setStyle("-fx-background-color: #2E436E; -fx-background-radius: 3; -fx-padding: 3 6 3 4;");
                label.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px;");
            } else {
                setStyle("-fx-background-color: transparent; -fx-background-radius: 3; -fx-padding: 3 6 3 4;");
                label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            }
        }

        @Override
        protected void updateItem(TreeNodeData data, boolean empty) {
            super.updateItem(data, empty);
            if (empty || data == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                label.setText(data.title);

                if (data.isCategory()) {
                    TriState state = registry.getCategoryState(data.title);
                    applyTriState(state);
                } else if (data.isSubcategory()) {
                    TriState state = registry.getSubcategoryState(data.parentCategory, data.title);
                    applyTriState(state);
                } else if (data.action != null) {
                    checkBox.setIndeterminate(false);
                    checkBox.setSelected(data.action.isEnabled());
                }

                updateStyles(isSelected());
                setText(null);
                setGraphic(cellBox);
            }
        }

        private void applyTriState(TriState state) {
            switch (state) {
                case CHECKED -> {
                    checkBox.setIndeterminate(false);
                    checkBox.setSelected(true);
                }
                case UNCHECKED -> {
                    checkBox.setIndeterminate(false);
                    checkBox.setSelected(false);
                }
                case INDETERMINATE -> {
                    checkBox.setIndeterminate(true);
                }
            }
        }
    }

    // =========================================================================
    // Settings Lifecycle (Apply / Reset / isModified)
    // =========================================================================

    public boolean isModified() {
        return registry.isModified();
    }

    public void apply() {
        registry.apply();
        modifiedProperty.set(false);
    }

    public void reset() {
        registry.reset();
        modifiedProperty.set(false);
        treeView.refresh();
        if (treeView.getSelectionModel().getSelectedItem() != null) {
            updateDetailView(treeView.getSelectionModel().getSelectedItem().getValue());
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public BooleanProperty modifiedProperty() {
        return modifiedProperty;
    }

    public TreeView<TreeNodeData> getTreeView() {
        return treeView;
    }

    public TextField getSearchField() {
        return searchField;
    }
}
