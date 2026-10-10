package dev.lumina.ui;

import dev.lumina.tools.DiagramsSettings;
import dev.lumina.tools.DiagramsSettingsManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Tools > Diagrams settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsDiagramsPage extends HBox {

    private final DiagramsSettingsManager manager;
    private DiagramsSettings initialSettings;
    private DiagramsSettings currentSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Left pane
    private TreeView<String> categoryTreeView;
    private final Map<String, SimpleBooleanProperty> itemSelectionProperties = new LinkedHashMap<>();

    // Right pane
    private ComboBox<String> defaultScopeCombo;

    private ComboBox<String> nodeItemStyleCombo;
    private TextField shortenLengthField;
    private CheckBox showGridByDefaultCheck;
    private CheckBox enableAnimationsCheck;
    private CheckBox enableSyntaxHighlightingCheck;

    private ComboBox<String> defaultLayoutCombo;
    private ComboBox<String> layoutOnSwitchCombo;
    private TextField layoutAnimationDurationField;
    private CheckBox fitContentAfterLayoutCheck;
    private CheckBox doRelayoutNewElementsCheck;

    public SettingsToolsDiagramsPage() {
        this.manager = DiagramsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(24);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Left side: Category tree
        VBox leftPane = new VBox(8);
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        Label treeHeader = new Label("Select categories that will be enabled on showing the diagram");
        treeHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        categoryTreeView = buildCategoryTreeView();
        VBox.setVgrow(categoryTreeView, Priority.ALWAYS);

        leftPane.getChildren().addAll(treeHeader, categoryTreeView);

        // 2. Right side: Controls
        VBox rightPane = new VBox(10);
        rightPane.setPrefWidth(380);
        rightPane.setMinWidth(320);

        // Scope row
        Label scopeLabel = createFieldLabel("Default Scope:", 160);
        defaultScopeCombo = new ComboBox<>();
        defaultScopeCombo.getItems().addAll("All", "Project Files", "Production Files", "Test Files", "Open Files");
        defaultScopeCombo.setValue("All");
        defaultScopeCombo.setPrefWidth(180);
        styleComboBox(defaultScopeCombo);
        defaultScopeCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox scopeRow = new HBox(12, scopeLabel, defaultScopeCombo);
        scopeRow.setAlignment(Pos.CENTER_LEFT);

        // Appearance section
        Label appHeader = new Label("Appearance");
        appHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 0 2 0;");

        Label styleLabel = createFieldLabel("Node item style", 160);
        nodeItemStyleCombo = new ComboBox<>();
        nodeItemStyleCombo.getItems().addAll("With types aligned", "Simple", "UML Classic");
        nodeItemStyleCombo.setValue("With types aligned");
        nodeItemStyleCombo.setPrefWidth(180);
        styleComboBox(nodeItemStyleCombo);
        nodeItemStyleCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox styleRow = new HBox(12, styleLabel, nodeItemStyleCombo);
        styleRow.setAlignment(Pos.CENTER_LEFT);

        Label lenLabel = createFieldLabel("Shorten node items on length:", 160);
        shortenLengthField = createTextField(180);
        shortenLengthField.setText("60");
        HBox lenRow = new HBox(12, lenLabel, shortenLengthField);
        lenRow.setAlignment(Pos.CENTER_LEFT);

        showGridByDefaultCheck = createCheckBox("Show grid by default");
        enableAnimationsCheck = createCheckBox("Enable animations");
        enableSyntaxHighlightingCheck = createCheckBox("Enable node items syntax highlighting");

        // Layout section
        Label layoutHeader = new Label("Layout");
        layoutHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 0 2 0;");

        Label defLayoutLabel = createFieldLabel("Default Layout:", 160);
        defaultLayoutCombo = new ComboBox<>();
        defaultLayoutCombo.getItems().addAll("Hierarchic Compact", "Hierarchic", "Organic", "Orthogonal", "Circular", "Tree");
        defaultLayoutCombo.setValue("Hierarchic Compact");
        defaultLayoutCombo.setPrefWidth(180);
        styleComboBox(defaultLayoutCombo);
        defaultLayoutCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox defLayoutRow = new HBox(12, defLayoutLabel, defaultLayoutCombo);
        defLayoutRow.setAlignment(Pos.CENTER_LEFT);

        Label onSwitchLabel = createFieldLabel("Layout on category switch:", 160);
        layoutOnSwitchCombo = new ComboBox<>();
        layoutOnSwitchCombo.getItems().addAll("With light layouter", "Full relayout", "Keep positions");
        layoutOnSwitchCombo.setValue("With light layouter");
        layoutOnSwitchCombo.setPrefWidth(180);
        styleComboBox(layoutOnSwitchCombo);
        layoutOnSwitchCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox onSwitchRow = new HBox(12, onSwitchLabel, layoutOnSwitchCombo);
        onSwitchRow.setAlignment(Pos.CENTER_LEFT);

        Label durLabel = createFieldLabel("Layout animation duration:", 160);
        layoutAnimationDurationField = createTextField(180);
        layoutAnimationDurationField.setText("1500");
        HBox durRow = new HBox(12, durLabel, layoutAnimationDurationField);
        durRow.setAlignment(Pos.CENTER_LEFT);

        fitContentAfterLayoutCheck = createCheckBox("Fit content after layout");
        doRelayoutNewElementsCheck = createCheckBox("Do relayout when new elements were added");

        rightPane.getChildren().addAll(
                scopeRow,
                appHeader,
                styleRow,
                lenRow,
                showGridByDefaultCheck,
                enableAnimationsCheck,
                enableSyntaxHighlightingCheck,
                layoutHeader,
                defLayoutRow,
                onSwitchRow,
                durRow,
                fitContentAfterLayoutCheck,
                doRelayoutNewElementsCheck
        );

        getChildren().addAll(leftPane, rightPane);
    }

    private TreeView<String> buildCategoryTreeView() {
        CheckBoxTreeItem<String> root = new CheckBoxTreeItem<>("Root");
        root.setExpanded(true);

        Map<String, List<String>> hierarchy = new LinkedHashMap<>();
        hierarchy.put("Show Difference", List.of("Details"));
        hierarchy.put("Java Classes", List.of("Fields", "Constructors", "Methods", "Properties", "Inner Classes"));
        hierarchy.put("Dependencies", List.of("Show One To One", "Show One To Many", "Show Usages In Code", "Show Links In Documentation", "Show Cyclic", "Others"));
        hierarchy.put("Project Modules", List.of("Libraries", "Test dependencies", "Show Paths: Root -> Selection", "Show Neighbors of Selected Nodes", "Show Borders"));
        hierarchy.put("CDI Dependencies", List.of("@Inject", "@Produces", "@Decorator"));
        hierarchy.put("Spring Beans Dependencies", List.of("Show Neighbors of Selected Nodes", "Local context", "Show beans from libraries", "Properties", "Show Borders"));
        hierarchy.put("Spring Context Dependencies", List.of("Show library models", "Show filesets as groups", "Show Borders"));
        hierarchy.put("Ruby Class Diagram", List.of("Fields", "Methods", "Constants"));
        hierarchy.put("Rails Model Dependency Diagram", List.of("Association fields", "Migration fields", "System DB fields", "Own fields"));
        hierarchy.put("Spring Integration", List.of("Show Labels"));
        hierarchy.put("Python Class Diagram", List.of("Methods", "Inner Classes", "Fields"));
        hierarchy.put("SQLAlchemy Model Dependency Diagram", List.of("Fields"));
        hierarchy.put("Maven dependencies", List.of("Show 'groupId':'artifactId':'version'", "Show Conflicts/Duplicates", "Show Paths: Root -> Selection", "Show Neighbors of Selected Nodes", "Show Borders"));
        hierarchy.put("Gradle Dependencies", List.of("Show 'groupId':'artifactId':'version'", "Show Conflicts/Duplicates", "Show Paths: Root -> Selection", "Show Neighbors of Selected Nodes", "Show Borders"));
        hierarchy.put("Database Schema Diagram", List.of("Key columns", "Columns", "Virtual foreign keys", "Comments"));
        hierarchy.put("Graphical Explain Plan", List.of("Attributes"));
        hierarchy.put("JPA ER Diagram", List.of("Properties", "Embeddables", "Superclasses"));
        hierarchy.put("PHP Class Diagrams", List.of("Fields", "Constants", "Constructors", "Methods"));
        hierarchy.put("Services diagram", List.of("From tests", "From libraries", "Show Neighbors of Selected Nodes", "Show Borders"));

        for (Map.Entry<String, List<String>> entry : hierarchy.entrySet()) {
            String parentCat = entry.getKey();
            CheckBoxTreeItem<String> parentItem = new CheckBoxTreeItem<>(parentCat);
            parentItem.setExpanded(true);
            parentItem.setIndependent(true);

            SimpleBooleanProperty parentProp = new SimpleBooleanProperty(false);
            parentProp.addListener((obs, o, n) -> {
                if (!updating) notifyModified();
            });
            itemSelectionProperties.put(parentCat, parentProp);
            parentItem.selectedProperty().bindBidirectional(parentProp);

            for (String sub : entry.getValue()) {
                CheckBoxTreeItem<String> subItem = new CheckBoxTreeItem<>(sub);
                subItem.setIndependent(true);
                String fullKey = parentCat + "/" + sub;
                SimpleBooleanProperty subProp = new SimpleBooleanProperty(false);
                subProp.addListener((obs, o, n) -> {
                    if (!updating) notifyModified();
                });
                itemSelectionProperties.put(fullKey, subProp);
                subItem.selectedProperty().bindBidirectional(subProp);
                parentItem.getChildren().add(subItem);
            }

            root.getChildren().add(parentItem);
        }

        TreeView<String> tv = new TreeView<>(root);
        tv.setShowRoot(false);
        tv.setCellFactory(CheckBoxTreeCell.forTreeView());
        tv.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        return tv;
    }

    private Label createFieldLabel(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private TextField createTextField(double width) {
        TextField tf = new TextField();
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 7 3 7;");
        tf.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return tf;
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return cb;
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            currentSettings = initialSettings.clone();

            defaultScopeCombo.setValue(currentSettings.getDefaultScope());
            nodeItemStyleCombo.setValue(currentSettings.getNodeItemStyle());
            shortenLengthField.setText(String.valueOf(currentSettings.getShortenNodeItemsLength()));
            showGridByDefaultCheck.setSelected(currentSettings.isShowGridByDefault());
            enableAnimationsCheck.setSelected(currentSettings.isEnableAnimations());
            enableSyntaxHighlightingCheck.setSelected(currentSettings.isEnableNodeItemsSyntaxHighlighting());

            defaultLayoutCombo.setValue(currentSettings.getDefaultLayout());
            layoutOnSwitchCombo.setValue(currentSettings.getLayoutOnCategorySwitch());
            layoutAnimationDurationField.setText(String.valueOf(currentSettings.getLayoutAnimationDuration()));
            fitContentAfterLayoutCheck.setSelected(currentSettings.isFitContentAfterLayout());
            doRelayoutNewElementsCheck.setSelected(currentSettings.isDoRelayoutWhenNewElementsAdded());

            Map<String, Boolean> selections = currentSettings.getCategorySelections();
            for (Map.Entry<String, SimpleBooleanProperty> entry : itemSelectionProperties.entrySet()) {
                Boolean val = selections.get(entry.getKey());
                entry.getValue().set(val != null && val);
            }
        } finally {
            updating = false;
        }
    }

    private DiagramsSettings getCurrentSettingsFromUI() {
        DiagramsSettings s = new DiagramsSettings();
        s.setDefaultScope(defaultScopeCombo.getValue());
        s.setNodeItemStyle(nodeItemStyleCombo.getValue());

        try {
            s.setShortenNodeItemsLength(Integer.parseInt(shortenLengthField.getText().trim()));
        } catch (NumberFormatException ignored) {}

        s.setShowGridByDefault(showGridByDefaultCheck.isSelected());
        s.setEnableAnimations(enableAnimationsCheck.isSelected());
        s.setEnableNodeItemsSyntaxHighlighting(enableSyntaxHighlightingCheck.isSelected());

        s.setDefaultLayout(defaultLayoutCombo.getValue());
        s.setLayoutOnCategorySwitch(layoutOnSwitchCombo.getValue());

        try {
            s.setLayoutAnimationDuration(Integer.parseInt(layoutAnimationDurationField.getText().trim()));
        } catch (NumberFormatException ignored) {}

        s.setFitContentAfterLayout(fitContentAfterLayoutCheck.isSelected());
        s.setDoRelayoutWhenNewElementsAdded(doRelayoutNewElementsCheck.isSelected());

        Map<String, Boolean> selections = new LinkedHashMap<>();
        for (Map.Entry<String, SimpleBooleanProperty> entry : itemSelectionProperties.entrySet()) {
            selections.put(entry.getKey(), entry.getValue().get());
        }
        s.setCategorySelections(selections);

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DiagramsSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for testing
    public TreeView<String> getCategoryTreeView() { return categoryTreeView; }
    public ComboBox<String> getDefaultScopeCombo() { return defaultScopeCombo; }
    public ComboBox<String> getNodeItemStyleCombo() { return nodeItemStyleCombo; }
    public TextField getShortenLengthField() { return shortenLengthField; }
    public CheckBox getShowGridByDefaultCheck() { return showGridByDefaultCheck; }
    public CheckBox getEnableAnimationsCheck() { return enableAnimationsCheck; }
    public CheckBox getEnableSyntaxHighlightingCheck() { return enableSyntaxHighlightingCheck; }
    public ComboBox<String> getDefaultLayoutCombo() { return defaultLayoutCombo; }
    public ComboBox<String> getLayoutOnSwitchCombo() { return layoutOnSwitchCombo; }
    public TextField getLayoutAnimationDurationField() { return layoutAnimationDurationField; }
    public CheckBox getFitContentAfterLayoutCheck() { return fitContentAfterLayoutCheck; }
    public CheckBox getDoRelayoutNewElementsCheck() { return doRelayoutNewElementsCheck; }
    public Map<String, SimpleBooleanProperty> getItemSelectionProperties() { return itemSelectionProperties; }
}
