package dev.lumina.ui;

import dev.lumina.inlay.InlayHintsManager;
import dev.lumina.inlay.InlayHintsSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.*;

/**
 * Faithfully mirrors IntelliJ IDEA's Editor > Inlay Hints settings page
 * with hierarchical tri-state CheckBoxTree, dynamic option panes, and realistic
 * code previews with inlay badges.
 */
public class SettingsInlayHintsPage extends BorderPane {

    public static class HintItem {
        private final String id;
        private final String title;
        private final String description;
        private final boolean hasPositionOption;
        private final List<PreviewLine> previewLines;

        public HintItem(String id, String title, String description, boolean hasPositionOption, List<PreviewLine> previewLines) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.hasPositionOption = hasPositionOption;
            this.previewLines = previewLines != null ? previewLines : List.of();
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public boolean hasPositionOption() { return hasPositionOption; }
        public List<PreviewLine> getPreviewLines() { return previewLines; }

        @Override
        public String toString() {
            return title;
        }
    }

    public record PreviewSegment(String text, String type, boolean isInlay) {}
    public record PreviewLine(List<PreviewSegment> segments) {
        public static PreviewLine of(PreviewSegment... segs) {
            return new PreviewLine(List.of(segs));
        }
    }

    // State
    private InlayHintsSettings workingSettings;
    private InlayHintsSettings originalSettings;
    private Runnable onModifiedListener;
    private boolean isUpdating = false;

    // UI components
    private final TreeView<HintItem> treeView = new TreeView<>();
    private final CheckBoxTreeItem<HintItem> rootItem = new CheckBoxTreeItem<>(new HintItem("root", "Inlay Hints", "", false, null));

    // Right detail controls
    private final VBox detailPane = new VBox(16);
    private final Label descriptionLabel = new Label();
    private final VBox controlsBox = new VBox(12);
    private final VBox previewBox = new VBox(8);
    private final VBox codeContainer = new VBox(4);

    // Code vision root controls
    private final ComboBox<String> defaultMetricsPosCombo = new ComboBox<>();
    private final Spinner<Integer> visibleMetricsAboveSpinner = new Spinner<>(1, 50, 5);
    private final Spinner<Integer> visibleMetricsNextSpinner = new Spinner<>(1, 50, 5);

    // Individual item position control
    private final ComboBox<String> itemPositionCombo = new ComboBox<>();
    private final HBox itemPositionRow = new HBox(12);

    // Current selected item
    private HintItem currentItem = null;

    public SettingsInlayHintsPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");

        initLayout();
        loadFromManager();
    }

    private void initLayout() {
        // --- Left: TreeView with Tri-state Checkboxes ---
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        treeView.setCellFactory(CheckBoxTreeCell.forTreeView());
        treeView.setPrefWidth(280);
        treeView.setMinWidth(240);
        treeView.setMaxWidth(340);
        treeView.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; -fx-border-width: 0 1 0 0;"
        );

        buildTreeNodes();

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.getValue() != null) {
                showDetail(newV.getValue());
            }
        });

        // --- Right: Detail Pane ---
        detailPane.setPadding(new Insets(16, 20, 16, 20));
        detailPane.setAlignment(Pos.TOP_LEFT);
        detailPane.setStyle("-fx-background-color: #1E1F22;");

        descriptionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        descriptionLabel.setWrapText(true);

        // Position combo boxes
        defaultMetricsPosCombo.getItems().addAll("Right", "Top");
        defaultMetricsPosCombo.setStyle(comboStyle());
        defaultMetricsPosCombo.valueProperty().addListener((obs, o, n) -> {
            if (!isUpdating && n != null) {
                workingSettings.setCodeVisionDefaultPosition(n);
                notifyModified();
            }
        });

        visibleMetricsAboveSpinner.setEditable(true);
        visibleMetricsAboveSpinner.setPrefWidth(70);
        visibleMetricsAboveSpinner.setStyle(spinnerStyle());
        visibleMetricsAboveSpinner.valueProperty().addListener((obs, o, n) -> {
            if (!isUpdating && n != null) {
                workingSettings.setCodeVisionMaxAbove(n);
                notifyModified();
            }
        });

        visibleMetricsNextSpinner.setEditable(true);
        visibleMetricsNextSpinner.setPrefWidth(70);
        visibleMetricsNextSpinner.setStyle(spinnerStyle());
        visibleMetricsNextSpinner.valueProperty().addListener((obs, o, n) -> {
            if (!isUpdating && n != null) {
                workingSettings.setCodeVisionMaxNext(n);
                notifyModified();
            }
        });

        itemPositionCombo.getItems().addAll("Default", "Top", "Right");
        itemPositionCombo.setStyle(comboStyle());
        itemPositionCombo.valueProperty().addListener((obs, o, n) -> {
            if (!isUpdating && n != null && currentItem != null) {
                workingSettings.setItemPosition(currentItem.getId(), n);
                notifyModified();
            }
        });

        Label posLabel = new Label("Position");
        posLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        itemPositionRow.setAlignment(Pos.CENTER_LEFT);
        itemPositionRow.getChildren().addAll(posLabel, itemPositionCombo);

        // Preview box
        codeContainer.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 12;"
        );
        previewBox.getChildren().add(codeContainer);
        VBox.setVgrow(previewBox, Priority.ALWAYS);

        detailPane.getChildren().addAll(descriptionLabel, controlsBox, previewBox);

        setLeft(treeView);
        setCenter(detailPane);
    }

    private void buildTreeNodes() {
        rootItem.getChildren().clear();

        // 1. Code vision
        CheckBoxTreeItem<HintItem> codeVision = createTreeItem(
                "codevision", "Code vision",
                "Provide information derived from project analysis: hierarchy details, problems, and usages.",
                false, null
        );
        codeVision.getChildren().addAll(
                createTreeItem("codevision.rename", "Rename", "Preview changes when renaming symbols.", true, null),
                createTreeItem("codevision.inheritors", "Inheritors",
                        "The number of descendants the class or interface has. Click the hint to navigate to the code that inherits from this class or interface.",
                        true, getInheritorsPreview()),
                createTreeItem("codevision.related_problems", "Related problems",
                        "The number of problems related to this declaration. Click the hint to show the problems.",
                        true, getRelatedProblemsPreview()),
                createTreeItem("codevision.usages", "Usages",
                        "The number of usages of this symbol in the project. Click the hint to show usages.",
                        true, getUsagesPreview()),
                createTreeItem("codevision.lsp_code_lens", "LSP Code Lens", "Code Lens hints provided by language servers.", true, null),
                createTreeItem("codevision.component_usages", "Component usages", "Usages of UI components across views and templates.", true, null),
                createTreeItem("codevision.java_configuration", "Java Configuration", "Spring and JVM configuration references.", true, null),
                createTreeItem("codevision.kotlin_script", "Kotlin Script", "Script configuration and dependencies.", true, null),
                createTreeItem("codevision.change_signature", "Change signature", "Signatures and refactoring hints.", true, null),
                createTreeItem("codevision.code_author", "Code author", "VCS blame author annotations above symbols.", true, null)
        );

        // 2. Parameter names
        CheckBoxTreeItem<HintItem> paramNames = createTreeItem(
                "param", "Parameter names",
                "Show parameter names for arguments in method and function calls.",
                false, null
        );
        CheckBoxTreeItem<HintItem> paramJava = createTreeItem("param.java", "Java", "Show parameter name hints for Java method calls.", false, null);
        paramJava.getChildren().addAll(
                createTreeItem("param.java.reflected", "Parameters reflected in the method name",
                        "Methods that take a single argument when the expected argument is clear from the method name, like in accessor methods.",
                        false, getJavaParamReflectedPreview()),
                createTreeItem("param.java.multiple_non_literal", "Multiple non-literal arguments with the same type",
                        "Show parameter names when multiple non-literal arguments share the same type.",
                        false, getJavaMultipleArgsPreview()),
                createTreeItem("param.java.numbered", "Numbered parameters",
                        "Parameters with names ending in numbers (e.g. param1, param2).",
                        false, null),
                createTreeItem("param.java.enum_constant", "Enum constant declarations",
                        "Arguments in enum constant constructors.",
                        false, null),
                createTreeItem("param.java.new_expressions", "'new' expressions",
                        "Constructor calls using the 'new' keyword.",
                        false, null),
                createTreeItem("param.java.complex_expressions", "Complex expressions as arguments",
                        "Arguments that are method calls, ternary expressions, or complex operations.",
                        false, null)
        );
        paramNames.getChildren().addAll(
                paramJava,
                createTreeItem("param.kotlin", "Kotlin", "Show parameter name hints for Kotlin function calls.", false, null),
                createTreeItem("param.angular", "Angular HTML template", "Show parameter names in Angular template bindings.", false, null),
                createTreeItem("param.groovy", "Groovy", "Show parameter name hints for Groovy method calls.", false, null),
                createTreeItem("param.javascript", "JavaScript", "Show parameter names in JavaScript function calls.", false, null),
                createTreeItem("param.sql", "SQL", "Show parameter names in SQL routine and procedure calls.", false, null),
                createTreeItem("param.typescript", "TypeScript", "Show parameter names in TypeScript function calls.", false, null),
                createTreeItem("param.vue", "Vue template", "Show parameter names in Vue template expressions.", false, null)
        );

        // 3. Types
        CheckBoxTreeItem<HintItem> types = createTreeItem("types", "Types", "Show inlay hints for inferred types of variables, fields, and expressions.", false, null);
        CheckBoxTreeItem<HintItem> typesJava = createTreeItem("types.java", "Java", "Show inferred types in Java declarations.", false, null);
        CheckBoxTreeItem<HintItem> typesJs = createTreeItem("types.javascript", "JavaScript", "Show inferred types for JavaScript.", false, null);
        typesJs.getChildren().addAll(
                createTreeItem("types.javascript.variables_and_fields", "Variables and fields", "Variables and fields", false, getJsTypesPreview()),
                createTreeItem("types.javascript.parameters_in_parentheses", "Parameters in parentheses", "Function parameters enclosed in parentheses.", false, null),
                createTreeItem("types.javascript.non_parenthesized", "Non-parenthesized single parameter", "Arrow functions with a single parameter without parentheses.", false, null),
                createTreeItem("types.javascript.function_return_types", "Function return types", "Inferred return types of functions.", false, null)
        );
        CheckBoxTreeItem<HintItem> typesTs = createTreeItem("types.typescript", "TypeScript", "Show inferred types for TypeScript.", false, null);
        typesTs.getChildren().addAll(
                createTreeItem("types.typescript.variables_and_fields", "Variables and fields", "Variables and fields", false, getJsTypesPreview()),
                createTreeItem("types.typescript.parameters_in_parentheses", "Parameters in parentheses", "Function parameters enclosed in parentheses.", false, null),
                createTreeItem("types.typescript.non_parenthesized", "Non-parenthesized single parameter", "Arrow functions with a single parameter without parentheses.", false, null),
                createTreeItem("types.typescript.function_return_types", "Function return types", "Inferred return types of functions.", false, null)
        );
        types.getChildren().addAll(
                typesJava,
                createTreeItem("types.kotlin", "Kotlin", "Show inferred types in Kotlin declarations.", false, null),
                createTreeItem("types.groovy", "Groovy", "Show inferred types in Groovy definitions.", false, null),
                typesJs,
                typesTs
        );

        // 4. Values
        CheckBoxTreeItem<HintItem> values = createTreeItem("values", "Values", "Show evaluated values and ranges.", false, null);
        values.getChildren().addAll(
                createTreeItem("values.kotlin", "Kotlin", "Show range and enum values in Kotlin.", false, null),
                createTreeItem("values.groovy", "Groovy", "Show evaluated constant values in Groovy.", false, null),
                createTreeItem("values.typescript", "TypeScript", "Show const enum and union values in TypeScript.", false, null)
        );

        // 5. Annotations
        CheckBoxTreeItem<HintItem> annotations = createTreeItem("annotations", "Annotations", "Show inferred annotations such as @NotNull, @Nullable, and contracts.", false, null);
        annotations.getChildren().add(createTreeItem("annotations.java", "Java", "Show inferred nullability and method contract annotations in Java.", false, null));

        // 6. Method chains
        CheckBoxTreeItem<HintItem> methodChains = createTreeItem("method_chains", "Method chains", "Show intermediate return types on chained method calls.", false, null);
        methodChains.getChildren().addAll(
                createTreeItem("method_chains.kotlin", "Kotlin", "Show intermediate return types in Kotlin chained calls.", false, null),
                createTreeItem("method_chains.javascript", "JavaScript", "Show intermediate return types in JavaScript promise or array chains.", false, null),
                createTreeItem("method_chains.typescript", "TypeScript", "Show intermediate return types in TypeScript method chains.", false, null)
        );

        // 7. Lambdas
        CheckBoxTreeItem<HintItem> lambdas = createTreeItem("lambdas", "Lambdas", "Provide type information where it is not explicit.", false, null);
        lambdas.getChildren().addAll(
                createTreeItem("lambdas.kotlin", "Kotlin", "Show implicit parameter and return types in Kotlin lambdas.", false, null),
                createTreeItem("lambdas.groovy", "Groovy", "Show implicit closure parameter types in Groovy.", false, null)
        );

        // 8. URL path
        CheckBoxTreeItem<HintItem> urlPath = createTreeItem("url_path", "URL path", "Show inlay hints for web endpoint URLs and HTTP methods.", false, null);
        urlPath.getChildren().addAll(
                createTreeItem("url_path.java", "Java", "Show URL path hints for Spring Web endpoints in Java.", false, null),
                createTreeItem("url_path.kotlin", "Kotlin", "Show URL path hints for Ktor and Spring endpoints in Kotlin.", false, null),
                createTreeItem("url_path.gradle", "Gradle Declarative Configuration", "Show repository and plugin URLs in Gradle files.", false, null),
                createTreeItem("url_path.groovy", "Groovy", "Show URL paths in Groovy scripts.", false, null),
                createTreeItem("url_path.xml", "XML", "Show URL paths in web.xml and Spring configuration.", false, null)
        );

        // 9. Other
        CheckBoxTreeItem<HintItem> other = createTreeItem("other", "Other", "Language-specific hints not categorized elsewhere.", false, null);
        other.getChildren().addAll(
                createTreeItem("other.java", "Java", "Additional Java inlay hints.", false, null),
                createTreeItem("other.kotlin", "Kotlin", "Additional Kotlin inlay hints.", false, null),
                createTreeItem("other.dockerfile", "Dockerfile", "Base image and stage hints in Dockerfile.", false, null),
                createTreeItem("other.gradle", "Gradle Declarative Configuration", "Version catalog and dependency hints.", false, null),
                createTreeItem("other.groovy", "Groovy", "Groovy dynamic member hints.", false, null),
                createTreeItem("other.markdown", "Markdown", "Table alignment and header hints in Markdown.", false, null),
                createTreeItem("other.properties", "Properties", "Standard values and property references.", false, null),
                createTreeItem("other.sql", "SQL", "Table alias and column type hints in SQL queries.", false, null),
                createTreeItem("other.xml", "XML", "Closing tag hints for long XML elements.", false, null),
                createTreeItem("other.yaml", "YAML", "Path hierarchy and anchor hints in YAML.", false, null)
        );

        rootItem.getChildren().addAll(codeVision, paramNames, types, values, annotations, methodChains, lambdas, urlPath, other);
    }

    private CheckBoxTreeItem<HintItem> createTreeItem(String id, String title, String desc, boolean hasPosition, List<PreviewLine> preview) {
        CheckBoxTreeItem<HintItem> item = new CheckBoxTreeItem<>(new HintItem(id, title, desc, hasPosition, preview));
        item.setIndependent(false); // Enables authentic tri-state inheritance!
        item.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!isUpdating && item.getValue() != null) {
                workingSettings.setHintEnabled(item.getValue().getId(), newV);
                if (currentItem != null && currentItem.getId().equals(item.getValue().getId())) {
                    renderCodePreview(currentItem);
                }
                notifyModified();
            }
        });
        return item;
    }

    private void showDetail(HintItem item) {
        this.currentItem = item;
        descriptionLabel.setText(item.getDescription() != null ? item.getDescription() : "");

        controlsBox.getChildren().clear();

        if ("codevision".equals(item.getId())) {
            // Code vision root controls
            HBox posRow = new HBox(12);
            posRow.setAlignment(Pos.CENTER_LEFT);
            Label lbl1 = new Label("Default position for metrics:");
            lbl1.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            posRow.getChildren().addAll(lbl1, defaultMetricsPosCombo);

            HBox aboveRow = new HBox(12);
            aboveRow.setAlignment(Pos.CENTER_LEFT);
            Label lbl2 = new Label("Visible metrics above declaration:");
            lbl2.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            aboveRow.getChildren().addAll(lbl2, visibleMetricsAboveSpinner);

            HBox nextRow = new HBox(12);
            nextRow.setAlignment(Pos.CENTER_LEFT);
            Label lbl3 = new Label("Visible metrics next to declaration:");
            lbl3.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            nextRow.getChildren().addAll(lbl3, visibleMetricsNextSpinner);

            controlsBox.getChildren().addAll(posRow, aboveRow, nextRow);
        } else if (item.hasPositionOption()) {
            String currentPos = workingSettings.getItemPosition(item.getId(), "Default");
            isUpdating = true;
            itemPositionCombo.setValue(currentPos);
            isUpdating = false;
            controlsBox.getChildren().add(itemPositionRow);
        }

        renderCodePreview(item);
    }

    private void renderCodePreview(HintItem item) {
        codeContainer.getChildren().clear();
        if (item == null || item.getPreviewLines().isEmpty()) {
            previewBox.setVisible(false);
            previewBox.setManaged(false);
            return;
        }

        previewBox.setVisible(true);
        previewBox.setManaged(true);

        boolean hintEnabled = workingSettings.isHintEnabled(item.getId(), true);

        for (PreviewLine line : item.getPreviewLines()) {
            TextFlow flow = new TextFlow();
            flow.setStyle("-fx-background-color: transparent;");

            for (PreviewSegment seg : line.segments()) {
                if (seg.isInlay()) {
                    Label badge = new Label(seg.text());
                    badge.setStyle(
                            "-fx-background-color: #2B2D30; " +
                            "-fx-text-fill: #868A91; " +
                            "-fx-font-family: 'JetBrains Mono', Consolas, monospace; " +
                            "-fx-font-size: 11px; " +
                            "-fx-background-radius: 3; " +
                            "-fx-padding: 1 5 1 5; " +
                            (hintEnabled ? "" : "-fx-opacity: 0.35;")
                    );
                    flow.getChildren().add(badge);
                } else {
                    Text t = new Text(seg.text());
                    t.setFont(Font.font("JetBrains Mono", 12));
                    switch (seg.type()) {
                        case "keyword" -> t.setFill(Color.web("#CF8E6D"));
                        case "string" -> t.setFill(Color.web("#6AAB73"));
                        case "number" -> t.setFill(Color.web("#2AACB8"));
                        case "comment" -> t.setFill(Color.web("#7A7E85"));
                        default -> t.setFill(Color.web("#BCBEC4"));
                    }
                    flow.getChildren().add(t);
                }
            }
            codeContainer.getChildren().add(flow);
        }
    }

    public void loadFromManager() {
        workingSettings = InlayHintsManager.getInstance().getSettings().copy();
        originalSettings = workingSettings.copy();

        isUpdating = true;

        defaultMetricsPosCombo.setValue(workingSettings.getCodeVisionDefaultPosition());
        visibleMetricsAboveSpinner.getValueFactory().setValue(workingSettings.getCodeVisionMaxAbove());
        visibleMetricsNextSpinner.getValueFactory().setValue(workingSettings.getCodeVisionMaxNext());

        applyStatesToTree(rootItem);

        isUpdating = false;

        // Select first item ("Code vision")
        if (!rootItem.getChildren().isEmpty()) {
            TreeItem<HintItem> first = rootItem.getChildren().get(0);
            treeView.getSelectionModel().select(first);
            showDetail(first.getValue());
        }
    }

    private void applyStatesToTree(TreeItem<HintItem> current) {
        if (current instanceof CheckBoxTreeItem<HintItem> cbItem && cbItem.getValue() != null) {
            String id = cbItem.getValue().getId();
            if (!"root".equals(id)) {
                boolean def = true;
                if ("param.java.reflected".equals(id)
                        || "param.java.multiple_non_literal".equals(id)
                        || "param.java.numbered".equals(id)
                        || "param.java.complex_expressions".equals(id)
                        || "types.kotlin".equals(id)
                        || "method_chains.kotlin".equals(id)
                        || "lambdas.kotlin".equals(id)) {
                    def = false;
                }
                boolean enabled = workingSettings.isHintEnabled(id, def);
                cbItem.setSelected(enabled);
            }
        }
        for (TreeItem<HintItem> child : current.getChildren()) {
            applyStatesToTree(child);
        }
    }

    public boolean isModified() {
        return !workingSettings.isEquivalentTo(originalSettings);
    }

    public void apply() {
        InlayHintsManager.getInstance().setSettings(workingSettings.copy());
        InlayHintsManager.getInstance().save();
        originalSettings = workingSettings.copy();
        notifyModified();
    }

    public void reset() {
        loadFromManager();
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public TreeView<HintItem> getTreeView() {
        return treeView;
    }

    public InlayHintsSettings getWorkingSettings() {
        return workingSettings;
    }

    // --- Sample Previews matching screenshots ---

    private List<PreviewLine> getInheritorsPreview() {
        return List.of(
                PreviewLine.of(
                        new PreviewSegment("private class ", "keyword", false),
                        new PreviewSegment("Base { ", "plain", false),
                        new PreviewSegment("no usages", "inlay", true),
                        new PreviewSegment(" }", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("    private class ", "keyword", false),
                        new PreviewSegment("MyButton { ", "plain", false),
                        new PreviewSegment("2 usages", "inlay", true),
                        new PreviewSegment(" }", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("    private class ", "keyword", false),
                        new PreviewSegment("AnimatedButton ", "plain", false),
                        new PreviewSegment("extends ", "keyword", false),
                        new PreviewSegment("MyButton { ", "plain", false),
                        new PreviewSegment("no usages", "inlay", true),
                        new PreviewSegment(" }", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("}", "plain", false)
                )
        );
    }

    private List<PreviewLine> getRelatedProblemsPreview() {
        return List.of(
                PreviewLine.of(
                        new PreviewSegment("public class ", "keyword", false),
                        new PreviewSegment("Service { ", "plain", false),
                        new PreviewSegment("1 related problem", "inlay", true),
                        new PreviewSegment(" }", "plain", false)
                )
        );
    }

    private List<PreviewLine> getUsagesPreview() {
        return List.of(
                PreviewLine.of(
                        new PreviewSegment("public void ", "keyword", false),
                        new PreviewSegment("processOrder() { ", "plain", false),
                        new PreviewSegment("5 usages", "inlay", true),
                        new PreviewSegment(" }", "plain", false)
                )
        );
    }

    private List<PreviewLine> getJavaParamReflectedPreview() {
        return List.of(
                PreviewLine.of(
                        new PreviewSegment("class ", "keyword", false),
                        new PreviewSegment("App {", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("    void ", "keyword", false),
                        new PreviewSegment("setName(String name) { }", "plain", false)
                ),
                PreviewLine.of(new PreviewSegment("", "plain", false)),
                PreviewLine.of(
                        new PreviewSegment("    void ", "keyword", false),
                        new PreviewSegment("sendMessage(String message) { }", "plain", false)
                ),
                PreviewLine.of(new PreviewSegment("", "plain", false)),
                PreviewLine.of(
                        new PreviewSegment("    public static void ", "keyword", false),
                        new PreviewSegment("main(String[] args) {", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("        App app = ", "plain", false),
                        new PreviewSegment("new ", "keyword", false),
                        new PreviewSegment("App();", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("        app.setName(", "plain", false),
                        new PreviewSegment("name: ", "inlay", true),
                        new PreviewSegment("\"My app\"", "string", false),
                        new PreviewSegment(");", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("        app.sendMessage(", "plain", false),
                        new PreviewSegment("message: ", "inlay", true),
                        new PreviewSegment("\"Hello\"", "string", false),
                        new PreviewSegment(");", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("    }", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("}", "plain", false)
                )
        );
    }

    private List<PreviewLine> getJavaMultipleArgsPreview() {
        return List.of(
                PreviewLine.of(
                        new PreviewSegment("computeRect(", "plain", false),
                        new PreviewSegment("width: ", "inlay", true),
                        new PreviewSegment("w, ", "plain", false),
                        new PreviewSegment("height: ", "inlay", true),
                        new PreviewSegment("h);", "plain", false)
                )
        );
    }

    private List<PreviewLine> getJsTypesPreview() {
        return List.of(
                PreviewLine.of(
                        new PreviewSegment("const ", "keyword", false),
                        new PreviewSegment("a", "plain", false),
                        new PreviewSegment(": number", "inlay", true),
                        new PreviewSegment(" = ", "plain", false),
                        new PreviewSegment("22", "number", false),
                        new PreviewSegment(";", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("let ", "keyword", false),
                        new PreviewSegment("b", "plain", false),
                        new PreviewSegment(": string", "inlay", true),
                        new PreviewSegment(" = ", "plain", false),
                        new PreviewSegment("'Hello World'", "string", false),
                        new PreviewSegment(";", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("var ", "keyword", false),
                        new PreviewSegment("z", "plain", false),
                        new PreviewSegment(": number[]", "inlay", true),
                        new PreviewSegment(" = [", "plain", false),
                        new PreviewSegment("1", "number", false),
                        new PreviewSegment(", ", "plain", false),
                        new PreviewSegment("2", "number", false),
                        new PreviewSegment(", ", "plain", false),
                        new PreviewSegment("3", "number", false),
                        new PreviewSegment("];", "plain", false)
                ),
                PreviewLine.of(new PreviewSegment("", "plain", false)),
                PreviewLine.of(
                        new PreviewSegment("class ", "keyword", false),
                        new PreviewSegment("TestClass {", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("    x", "plain", false),
                        new PreviewSegment(": number", "inlay", true),
                        new PreviewSegment(" = ", "plain", false),
                        new PreviewSegment("5", "number", false),
                        new PreviewSegment(";", "plain", false)
                ),
                PreviewLine.of(
                        new PreviewSegment("}", "plain", false)
                )
        );
    }

    private static String comboStyle() {
        return "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-font-size: 12px; -fx-padding: 3 8;";
    }

    private static String spinnerStyle() {
        return "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-font-size: 12px;";
    }
}
