package dev.lumina.ui;

import dev.lumina.templates.FileTemplate;
import dev.lumina.templates.FileTemplateCategory;
import dev.lumina.templates.FileTemplateManager;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.util.*;

/**
 * Complete, dynamic IntelliJ IDEA Editor > File and Code Templates page for Lumina IDE.
 * Fully data-driven via FileTemplateManager with no hardcoded template checks.
 * Reproduces IntelliJ IDEA layout, icons, toolbar (+, copy, -, revert), Velocity variables, and schemes.
 * Dark theme with #1E1F22 palette - eliminates white backgrounds and duplicate breadcrumbs.
 */
public class SettingsFileAndCodeTemplatesPage extends VBox {

    private final FileTemplateManager templateManager = FileTemplateManager.getInstance();

    // Scheme Selector
    private final ComboBox<String> schemeCombo = new ComboBox<>();
    private final Hyperlink revertChangesLink = new Hyperlink("Revert changes");
    private final BooleanProperty canRevertProperty = new SimpleBooleanProperty(false);

    // Category Tabs
    private final ToggleGroup categoryGroup = new ToggleGroup();
    private final ToggleButton filesBtn = new ToggleButton("Files");
    private final ToggleButton includesBtn = new ToggleButton("Includes");
    private final ToggleButton codeBtn = new ToggleButton("Code");
    private final ToggleButton otherBtn = new ToggleButton("Other");

    // Left Toolbar
    private final Button addBtn = new Button();
    private final Button copyBtn = new Button();
    private final Button removeBtn = new Button();
    private final Button revertBtn = new Button();

    // Left Navigation: ListView (Files, Includes, Code) and TreeView (Other)
    private final StackPane leftNavStack = new StackPane();
    private final ListView<FileTemplate> templateList = new ListView<>();
    private final TreeView<TreeTemplateNode> templateTree = new TreeView<>();
    private final ObservableList<FileTemplate> currentCategoryTemplates = FXCollections.observableArrayList();

    // Right Editor Area: Custom / Name / Extension Fields
    private final VBox customFieldsBox = new VBox(6);
    private final TextField nameField = new TextField();
    private final TextField extensionField = new TextField();
    private final TextField fileNameField = new TextField();
    private final HBox fileNameRow = new HBox(8);

    // Right Editor Area: Code Editor & Checkboxes
    private final TextArea codeEditor = new TextArea();
    private final CheckBox reformatCheck = new CheckBox("Reformat according to style");
    private final CheckBox liveTemplatesCheck = new CheckBox("Enable Live Templates");

    // Right Editor Area: IntelliJ-Style Description Pane
    private final ScrollPane descriptionScrollPane = new ScrollPane();
    private final VBox descriptionContentBox = new VBox(6);
    private final Label descHeaderLabel = new Label("Description:");
    private final Label descBodyLabel = new Label();
    private final VBox variablesBox = new VBox(4);
    private final Label varsHeaderLabel = new Label("Predefined variables take the following values:");
    private final GridPane varsGrid = new GridPane();
    private final Separator footerSep = new Separator();
    private final HBox velocityFooter = new HBox(0);

    private FileTemplate selectedTemplate;
    private boolean isUpdatingUi = false;
    private Runnable onModifiedListener;

    public SettingsFileAndCodeTemplatesPage() {
        getStyleClass().addAll("settings-page", "file-templates-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(10);

        buildHeader();
        buildCategoryTabs();
        buildMainLayout();

        // Switch to initial category
        switchCategory(FileTemplateCategory.FILES);
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public BooleanProperty canRevertProperty() {
        return canRevertProperty;
    }

    public void revertCurrent() {
        handleRevertCurrent();
    }

    public void reset() {
        templateManager.setCurrentScheme(schemeCombo.getValue());
        switchCategory(getSelectedCategory());
        updateRevertLinkVisibility();
    }

    private void notifyModified() {
        updateRevertLinkVisibility();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public boolean isModified() {
        return templateManager.hasAnyModifiedTemplates();
    }

    public void apply() {
        if (selectedTemplate != null && !isUpdatingUi) {
            saveCurrentEditorToTemplate();
        }
        templateManager.save();
        updateRevertLinkVisibility();
    }

    // =========================================================================
    // UI LAYOUT BUILDERS
    // =========================================================================

    private void buildHeader() {
        // Scheme selector row (no duplicate breadcrumb!)
        HBox schemeRow = new HBox(8);
        schemeRow.setAlignment(Pos.CENTER_LEFT);

        Label schemeLabel = new Label("Scheme:");
        schemeLabel.setStyle("-fx-text-fill: #9DA0A8; -fx-font-size: 12px;");

        schemeCombo.getItems().setAll(templateManager.getAvailableSchemes());
        schemeCombo.setValue(templateManager.getCurrentScheme());
        schemeCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        schemeCombo.setPrefWidth(180);

        schemeCombo.setOnAction(e -> {
            String selectedScheme = schemeCombo.getValue();
            if (selectedScheme != null && !selectedScheme.equals(templateManager.getCurrentScheme())) {
                templateManager.setCurrentScheme(selectedScheme);
                switchCategory(getSelectedCategory());
            }
        });

        revertChangesLink.setStyle("-fx-text-fill: #5799F7; -fx-font-size: 12px; -fx-underline: true; -fx-padding: 0 0 0 16;");
        revertChangesLink.setVisible(false);
        revertChangesLink.setOnAction(e -> handleRevertCurrent());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        schemeRow.getChildren().addAll(schemeLabel, schemeCombo, revertChangesLink, spacer);
        getChildren().add(schemeRow);
    }

    private void buildCategoryTabs() {
        HBox tabsRow = new HBox(0);
        tabsRow.setAlignment(Pos.CENTER_LEFT);
        tabsRow.setPadding(new Insets(2, 0, 4, 0));

        filesBtn.setToggleGroup(categoryGroup);
        includesBtn.setToggleGroup(categoryGroup);
        codeBtn.setToggleGroup(categoryGroup);
        otherBtn.setToggleGroup(categoryGroup);

        String segmentStyle = "-fx-background-color: #2B2D30; -fx-text-fill: #9DA0A8; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 4 14 4 14; -fx-border-color: #393B40;";
        filesBtn.setStyle(segmentStyle + " -fx-background-radius: 4 0 0 4; -fx-border-radius: 4 0 0 4;");
        includesBtn.setStyle(segmentStyle);
        codeBtn.setStyle(segmentStyle);
        otherBtn.setStyle(segmentStyle + " -fx-background-radius: 0 4 4 0; -fx-border-radius: 0 4 4 0;");

        categoryGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                if (oldVal != null) oldVal.setSelected(true);
                return;
            }
            updateTabStyles();
            switchCategory(getSelectedCategory());
        });

        filesBtn.setSelected(true);
        updateTabStyles();

        tabsRow.getChildren().addAll(filesBtn, includesBtn, codeBtn, otherBtn);
        getChildren().add(tabsRow);
    }

    private void updateTabStyles() {
        String active = "-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;";
        String inactive = "-fx-background-color: #2B2D30; -fx-text-fill: #9DA0A8; -fx-font-weight: 600;";

        filesBtn.setStyle((filesBtn.isSelected() ? active : inactive) + " -fx-font-size: 12px; -fx-padding: 4 14 4 14; -fx-border-color: #393B40; -fx-background-radius: 4 0 0 4; -fx-border-radius: 4 0 0 4;");
        includesBtn.setStyle((includesBtn.isSelected() ? active : inactive) + " -fx-font-size: 12px; -fx-padding: 4 14 4 14; -fx-border-color: #393B40;");
        codeBtn.setStyle((codeBtn.isSelected() ? active : inactive) + " -fx-font-size: 12px; -fx-padding: 4 14 4 14; -fx-border-color: #393B40;");
        otherBtn.setStyle((otherBtn.isSelected() ? active : inactive) + " -fx-font-size: 12px; -fx-padding: 4 14 4 14; -fx-border-color: #393B40; -fx-background-radius: 0 4 4 0; -fx-border-radius: 0 4 4 0;");
    }

    private FileTemplateCategory getSelectedCategory() {
        if (includesBtn.isSelected()) return FileTemplateCategory.INCLUDES;
        if (codeBtn.isSelected()) return FileTemplateCategory.CODE;
        if (otherBtn.isSelected()) return FileTemplateCategory.OTHER;
        return FileTemplateCategory.FILES;
    }

    private void buildMainLayout() {
        HBox mainSplit = new HBox(16);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        // ---- Left Column: Toolbar + (ListView / TreeView) ----
        VBox leftColumn = new VBox(6);
        leftColumn.setPrefWidth(240);
        leftColumn.setMinWidth(210);
        leftColumn.setMaxWidth(260);

        HBox toolbar = buildToolbar();

        // 1. ListView for Files, Includes, Code
        templateList.setItems(currentCategoryTemplates);
        templateList.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(templateList, Priority.ALWAYS);

        templateList.setCellFactory(lv -> new ListCell<>() {
            private final HBox cellBox = new HBox(8);
            private final Label label = new Label();

            {
                cellBox.setAlignment(Pos.CENTER_LEFT);
                label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            }

            @Override
            protected void updateItem(FileTemplate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Node icon = createIconForTemplate(item);
                    label.setText(item.getName());
                    if (item.isModified()) {
                        label.setStyle("-fx-text-fill: #5799F7; -fx-font-size: 12px; -fx-font-weight: bold;");
                    } else {
                        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: normal;");
                    }
                    cellBox.getChildren().setAll(icon, label);
                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });

        templateList.getSelectionModel().selectedItemProperty().addListener((obs, oldT, newT) -> {
            if (oldT != null && !isUpdatingUi) {
                saveCurrentEditorToTemplate(oldT);
            }
            if (newT != null) {
                selectedTemplate = newT;
                populateEditor(newT);
                updateToolbarButtons();
            }
        });

        // 2. TreeView for Other category
        templateTree.setShowRoot(false);
        templateTree.getStyleClass().add("file-templates-tree");
        templateTree.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(templateTree, Priority.ALWAYS);

        templateTree.setCellFactory(tv -> new TreeCell<>() {
            private final HBox cellBox = new HBox(8);
            private final Label label = new Label();

            {
                cellBox.setAlignment(Pos.CENTER_LEFT);
                label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            }

            @Override
            protected void updateItem(TreeTemplateNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Node icon = createIconForNode(item);
                    label.setText(item.getTitle());
                    if (item.getTemplate() != null && item.getTemplate().isModified()) {
                        label.setStyle("-fx-text-fill: #5799F7; -fx-font-size: 12px; -fx-font-weight: bold;");
                    } else {
                        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: normal;");
                    }
                    cellBox.getChildren().setAll(icon, label);
                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });

        templateTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && oldVal.getValue() != null && oldVal.getValue().getTemplate() != null && !isUpdatingUi) {
                saveCurrentEditorToTemplate(oldVal.getValue().getTemplate());
            }
            if (newVal != null && newVal.getValue() != null) {
                TreeTemplateNode node = newVal.getValue();
                if (node.isGroup()) {
                    selectedTemplate = null;
                    populateGroupNode(node.getTitle());
                } else {
                    selectedTemplate = node.getTemplate();
                    populateEditor(selectedTemplate);
                }
                updateToolbarButtons();
            }
        });

        leftNavStack.getChildren().addAll(templateList, templateTree);
        VBox.setVgrow(leftNavStack, Priority.ALWAYS);

        leftColumn.getChildren().addAll(toolbar, leftNavStack);

        // ---- Right Column: Editor & Details ----
        VBox rightColumn = new VBox(8);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        // 1. Name & Extension fields (for Files / Other)
        buildCustomFields();

        // 2. Code Editor (solid dark #1E1F22, no white viewport!)
        codeEditor.getStyleClass().addAll("file-templates-code-editor", "text-area");
        codeEditor.setStyle(
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-highlight-fill: #2E436E; " +
                "-fx-highlight-text-fill: #FFFFFF; " +
                "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                "-fx-font-size: 13px;"
        );
        codeEditor.setWrapText(false);
        VBox.setVgrow(codeEditor, Priority.ALWAYS);
        codeEditor.setPrefHeight(250);

        // Enforce dark styling on internal .content region
        codeEditor.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(() -> {
                    Node content = codeEditor.lookup(".content");
                    if (content != null) {
                        content.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22;");
                    }
                });
            }
        });

        codeEditor.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingUi && selectedTemplate != null) {
                selectedTemplate.setText(newVal);
                if (getSelectedCategory() == FileTemplateCategory.OTHER) {
                    templateTree.refresh();
                } else {
                    templateList.refresh();
                }
                notifyModified();
            }
        });

        // 3. Checkboxes (Reformat according to style, Enable Live Templates)
        HBox checksRow = new HBox(20);
        checksRow.setAlignment(Pos.CENTER_LEFT);

        reformatCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        liveTemplatesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        reformatCheck.setOnAction(e -> {
            if (selectedTemplate != null) {
                selectedTemplate.setReformatCode(reformatCheck.isSelected());
                notifyModified();
            }
        });

        liveTemplatesCheck.setOnAction(e -> {
            if (selectedTemplate != null) {
                selectedTemplate.setLiveTemplatesEnabled(liveTemplatesCheck.isSelected());
                notifyModified();
            }
        });

        checksRow.getChildren().addAll(reformatCheck, liveTemplatesCheck);

        // 4. Description & Variables section (Matching IntelliJ IDEA layout)
        buildDescriptionPane();

        rightColumn.getChildren().addAll(customFieldsBox, codeEditor, checksRow, descriptionScrollPane);

        mainSplit.getChildren().addAll(leftColumn, rightColumn);
        getChildren().add(mainSplit);
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Toolbar buttons matching IntelliJ order: Add, Copy, Remove, Revert
        configureToolBtn(addBtn, "+", "Add template", e -> handleAddTemplate());
        configureToolBtnSvg(copyBtn, "M 2 4 L 8 4 L 8 10 L 2 10 Z M 5 2 L 11 2 L 11 8", "Copy template", e -> handleCopyTemplate());
        configureToolBtn(removeBtn, "−", "Remove template", e -> handleRemoveTemplate());
        configureToolBtnSvg(revertBtn, "M 6 2 A 4 4 0 1 0 10 6 M 10 3 L 10 6 L 7 6", "Reset to default", e -> handleRevertCurrent());

        toolbar.getChildren().addAll(addBtn, copyBtn, removeBtn, revertBtn);
        return toolbar;
    }

    private void configureToolBtn(Button btn, String text, String tooltip, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        btn.setText(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9DA0A8; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-border-radius: 3; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-border-radius: 3; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9DA0A8; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-border-radius: 3; -fx-cursor: hand;"));
        btn.setOnAction(handler);
    }

    private void configureToolBtnSvg(Button btn, String svgContent, String tooltip, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        SVGPath svg = new SVGPath();
        svg.setContent(svgContent);
        svg.setFill(Color.TRANSPARENT);
        svg.setStroke(Color.web("#9DA0A8"));
        svg.setStrokeWidth(1.2);

        btn.setGraphic(svg);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6 4 6; -fx-border-radius: 3; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: #393B40; -fx-padding: 4 6 4 6; -fx-border-radius: 3; -fx-cursor: hand;");
            svg.setStroke(Color.web("#FFFFFF"));
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6 4 6; -fx-border-radius: 3; -fx-cursor: hand;");
            svg.setStroke(Color.web("#9DA0A8"));
        });
        btn.setOnAction(handler);
    }

    private void buildCustomFields() {
        customFieldsBox.setSpacing(6);

        // Row 1: Name & Extension
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        nameField.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        nameField.setPrefWidth(240);
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingUi && selectedTemplate != null) {
                selectedTemplate.setName(newVal);
                if (getSelectedCategory() == FileTemplateCategory.OTHER) {
                    templateTree.refresh();
                } else {
                    templateList.refresh();
                }
                notifyModified();
            }
        });

        Label extLabel = new Label("Extension:");
        extLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        extensionField.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        extensionField.setPrefWidth(120);
        extensionField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingUi && selectedTemplate != null) {
                selectedTemplate.setExtension(newVal);
                notifyModified();
            }
        });

        nameRow.getChildren().addAll(nameLabel, nameField, extLabel, extensionField);

        // Row 2: File Name Pattern (for custom templates)
        fileNameRow.setAlignment(Pos.CENTER_LEFT);
        Label fnLabel = new Label("File name:");
        fnLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        fileNameField.setPromptText("Template to generate file name and path (optional)");
        fileNameField.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox.setHgrow(fileNameField, Priority.ALWAYS);
        fileNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingUi && selectedTemplate != null) {
                selectedTemplate.setFileNameTemplate(newVal);
                notifyModified();
            }
        });

        fileNameRow.getChildren().addAll(fnLabel, fileNameField);

        customFieldsBox.getChildren().addAll(nameRow, fileNameRow);
    }

    private void buildDescriptionPane() {
        descriptionScrollPane.setFitToWidth(true);
        descriptionScrollPane.setPrefHeight(130);
        descriptionScrollPane.setMinHeight(100);
        descriptionScrollPane.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-background-insets: 0; " +
                "-fx-padding: 0; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4;"
        );

        descriptionContentBox.setPadding(new Insets(8, 10, 8, 10));
        descriptionContentBox.setSpacing(6);
        descriptionContentBox.setStyle("-fx-background-color: #1E1F22;");

        descHeaderLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
        descBodyLabel.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 12px; -fx-line-spacing: 2;");
        descBodyLabel.setWrapText(true);

        varsHeaderLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        varsGrid.setHgap(20);
        varsGrid.setVgap(4);
        varsGrid.setPadding(new Insets(2, 0, 4, 12));

        variablesBox.setSpacing(4);
        variablesBox.getChildren().addAll(varsHeaderLabel, varsGrid);

        footerSep.setStyle("-fx-background-color: #393B40; -fx-border-color: #393B40; -fx-border-width: 0.5;");

        Hyperlink velocityLink = new Hyperlink("Apache Velocity");
        velocityLink.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 12px; -fx-padding: 0; -fx-border-width: 0; -fx-underline: false;");
        velocityLink.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://velocity.apache.org/engine/devel/vtl-reference.html"));
            } catch (Throwable ignored) {}
        });

        Label velocitySuffix = new Label(" template language is used");
        velocitySuffix.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        velocityFooter.setAlignment(Pos.CENTER_LEFT);
        velocityFooter.getChildren().addAll(velocityLink, velocitySuffix);

        descriptionContentBox.getChildren().addAll(descHeaderLabel, descBodyLabel, variablesBox, footerSep, velocityFooter);
        descriptionScrollPane.setContent(descriptionContentBox);
    }

    // =========================================================================
    // DYNAMIC DATA BINDINGS & ACTIONS
    // =========================================================================

    private void switchCategory(FileTemplateCategory category) {
        isUpdatingUi = true;

        if (category == FileTemplateCategory.OTHER) {
            // Show TreeView for OTHER
            templateList.setVisible(false);
            templateList.setManaged(false);
            templateTree.setVisible(true);
            templateTree.setManaged(true);

            populateOtherCategoryTree();
        } else {
            // Show ListView for FILES, INCLUDES, CODE
            templateTree.setVisible(false);
            templateTree.setManaged(false);
            templateList.setVisible(true);
            templateList.setManaged(true);

            List<FileTemplate> templates = templateManager.getTemplates(category);
            currentCategoryTemplates.setAll(templates);

            if (!currentCategoryTemplates.isEmpty()) {
                templateList.getSelectionModel().selectFirst();
            } else {
                selectedTemplate = null;
                clearEditor();
            }
        }

        isUpdatingUi = false;
        updateToolbarButtons();
        updateRevertLinkVisibility();
    }

    private void populateOtherCategoryTree() {
        TreeItem<TreeTemplateNode> root = new TreeItem<>(new TreeTemplateNode("Root", "root"));
        root.setExpanded(true);

        List<FileTemplate> otherTemplates = templateManager.getTemplates(FileTemplateCategory.OTHER);

        // Group templates hierarchically matching IntelliJ IDEA screenshot 4
        Map<String, List<FileTemplate>> groups = new LinkedHashMap<>();
        for (FileTemplate t : otherTemplates) {
            String grp = t.getGroup();
            if (grp == null || grp.isBlank()) grp = "Other";
            groups.computeIfAbsent(grp, k -> new ArrayList<>()).add(t);
        }

        // Parent categories in exact order seen in IntelliJ IDEA:
        // Application, CDI, JAX-RS, JBoss/WildFly Server, JPA, Java Enterprise, JavaFX, Maven, Spring, Tomcat Server, Web
        String[] displayOrder = {
                "Application", "Deployment descriptors", "CDI", "JAX-RS",
                "JBoss/WildFly Server", "JPA", "Java Enterprise", "JavaFX",
                "Maven", "Spring", "Tomcat Server", "Web"
        };

        TreeItem<TreeTemplateNode> appNode = null;
        TreeItem<TreeTemplateNode> mavenNodeToSelect = null;

        for (String grpName : displayOrder) {
            if ("Deployment descriptors".equals(grpName)) {
                // Nested under Application
                if (appNode != null && groups.containsKey("Deployment descriptors")) {
                    TreeItem<TreeTemplateNode> depDesc = new TreeItem<>(new TreeTemplateNode("Deployment descriptors", "xml"));
                    for (FileTemplate t : groups.get("Deployment descriptors")) {
                        depDesc.getChildren().add(new TreeItem<>(new TreeTemplateNode(t)));
                    }
                    appNode.getChildren().add(depDesc);
                    depDesc.setExpanded(true);
                }
                continue;
            }

            if (groups.containsKey(grpName)) {
                String iconKey = getGroupIconKey(grpName);
                TreeItem<TreeTemplateNode> groupItem = new TreeItem<>(new TreeTemplateNode(grpName, iconKey));
                groupItem.setExpanded(true);

                if ("Application".equals(grpName)) {
                    appNode = groupItem;
                }

                for (FileTemplate t : groups.get(grpName)) {
                    groupItem.getChildren().add(new TreeItem<>(new TreeTemplateNode(t)));
                }

                root.getChildren().add(groupItem);

                if ("Maven".equals(grpName)) {
                    mavenNodeToSelect = groupItem;
                }
            }
        }

        // Add any remaining groups
        for (Map.Entry<String, List<FileTemplate>> entry : groups.entrySet()) {
            String gName = entry.getKey();
            if (!List.of(displayOrder).contains(gName)) {
                TreeItem<TreeTemplateNode> customGroup = new TreeItem<>(new TreeTemplateNode(gName, "generic"));
                for (FileTemplate t : entry.getValue()) {
                    customGroup.getChildren().add(new TreeItem<>(new TreeTemplateNode(t)));
                }
                root.getChildren().add(customGroup);
            }
        }

        templateTree.setRoot(root);

        // Select Maven node by default (matching Image 4)
        if (mavenNodeToSelect != null) {
            templateTree.getSelectionModel().select(mavenNodeToSelect);
        } else if (!root.getChildren().isEmpty()) {
            templateTree.getSelectionModel().select(root.getChildren().get(0));
        }
    }

    private String getGroupIconKey(String groupName) {
        return switch (groupName) {
            case "Application", "Java Enterprise" -> "ee";
            case "CDI" -> "cdi";
            case "JAX-RS", "Web" -> "web";
            case "JBoss/WildFly Server" -> "jboss";
            case "JPA" -> "jpa";
            case "JavaFX" -> "javafx";
            case "Maven" -> "maven";
            case "Spring" -> "spring";
            case "Tomcat Server" -> "tomcat";
            default -> "generic";
        };
    }

    private void populateGroupNode(String groupTitle) {
        isUpdatingUi = true;

        // Custom fields show Name & Extension, but disabled when a category group is selected (matching Image 4)
        customFieldsBox.setVisible(true);
        customFieldsBox.setManaged(true);
        nameField.setText("");
        nameField.setDisable(true);
        extensionField.setText("");
        extensionField.setDisable(true);
        fileNameRow.setVisible(false);
        fileNameRow.setManaged(false);

        codeEditor.setText("");
        codeEditor.setDisable(true);

        reformatCheck.setSelected(false);
        reformatCheck.setDisable(true);
        liveTemplatesCheck.setSelected(false);
        liveTemplatesCheck.setDisable(true);

        // Populate description with general file templates documentation (matching Image 4)
        updateDescriptionContent(FileTemplateManager.GENERAL_FILE_TEMPLATES_DESCRIPTION, Collections.emptyMap());

        isUpdatingUi = false;
        updateRevertLinkVisibility();
    }

    private void populateEditor(FileTemplate template) {
        if (template == null) return;
        isUpdatingUi = true;

        codeEditor.setDisable(false);
        reformatCheck.setDisable(false);
        liveTemplatesCheck.setDisable(false);

        FileTemplateCategory category = getSelectedCategory();

        if (category == FileTemplateCategory.OTHER || category == FileTemplateCategory.FILES) {
            customFieldsBox.setVisible(true);
            customFieldsBox.setManaged(true);
            nameField.setText(template.getName());
            nameField.setDisable(template.isBuiltin());
            extensionField.setText(template.getExtension());
            extensionField.setDisable(template.isBuiltin());

            if (!template.isBuiltin()) {
                fileNameRow.setVisible(true);
                fileNameRow.setManaged(true);
                fileNameField.setText(template.getFileNameTemplate());
                fileNameField.setDisable(false);
            } else {
                fileNameRow.setVisible(false);
                fileNameRow.setManaged(false);
            }
        } else {
            // Includes and Code categories: hide Name/Extension fields to give code editor full vertical space (Images 2, 3, 5)
            customFieldsBox.setVisible(false);
            customFieldsBox.setManaged(false);
        }

        codeEditor.setText(template.getText());
        reformatCheck.setSelected(template.isReformatCode());
        liveTemplatesCheck.setSelected(template.isLiveTemplatesEnabled());

        updateDescriptionContent(template.getDescription(), template.getVariables());

        isUpdatingUi = false;
        updateRevertLinkVisibility();
    }

    private void updateDescriptionContent(String description, Map<String, String> vars) {
        descBodyLabel.setText(description != null ? description : "");

        varsGrid.getChildren().clear();
        if (vars != null && !vars.isEmpty()) {
            variablesBox.setVisible(true);
            variablesBox.setManaged(true);
            int row = 0;
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                Label varNameLbl = new Label("${" + entry.getKey() + "}");
                varNameLbl.setStyle("-fx-text-fill: #E5C07B; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");

                Label varDescLbl = new Label(entry.getValue());
                varDescLbl.setStyle("-fx-text-fill: #9DA0A8; -fx-font-size: 12px;");

                varsGrid.addRow(row++, varNameLbl, varDescLbl);
            }
        } else {
            variablesBox.setVisible(false);
            variablesBox.setManaged(false);
        }
    }

    private void clearEditor() {
        isUpdatingUi = true;
        customFieldsBox.setVisible(false);
        customFieldsBox.setManaged(false);
        codeEditor.clear();
        codeEditor.setDisable(true);
        updateDescriptionContent("", Collections.emptyMap());
        isUpdatingUi = false;
    }

    private void saveCurrentEditorToTemplate() {
        if (selectedTemplate != null) {
            saveCurrentEditorToTemplate(selectedTemplate);
        }
    }

    private void saveCurrentEditorToTemplate(FileTemplate t) {
        if (t == null) return;
        t.setText(codeEditor.getText());
        t.setReformatCode(reformatCheck.isSelected());
        t.setLiveTemplatesEnabled(liveTemplatesCheck.isSelected());
        if (!t.isBuiltin()) {
            t.setName(nameField.getText());
            t.setExtension(extensionField.getText());
            t.setFileNameTemplate(fileNameField.getText());
        }
    }

    private void handleAddTemplate() {
        FileTemplateCategory cat = getSelectedCategory();
        String baseName = "Unnamed";
        int suffix = 1;
        String name = baseName;
        while (templateManager.findTemplateByName(name, cat) != null) {
            name = baseName + (suffix++);
        }

        FileTemplate custom = new FileTemplate(name, "java", "", cat, "",
                FileTemplateManager.GENERAL_FILE_TEMPLATES_DESCRIPTION,
                "java", false);

        templateManager.addTemplate(custom);
        if (cat == FileTemplateCategory.OTHER) {
            populateOtherCategoryTree();
        } else {
            currentCategoryTemplates.add(custom);
            templateList.getSelectionModel().select(custom);
        }
        nameField.requestFocus();
        nameField.selectAll();
        notifyModified();
    }

    private void handleRemoveTemplate() {
        if (selectedTemplate != null && !selectedTemplate.isBuiltin()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete custom template '" + selectedTemplate.getName() + "'?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    templateManager.removeTemplate(selectedTemplate);
                    if (getSelectedCategory() == FileTemplateCategory.OTHER) {
                        populateOtherCategoryTree();
                    } else {
                        currentCategoryTemplates.remove(selectedTemplate);
                    }
                    notifyModified();
                }
            });
        }
    }

    private void handleCopyTemplate() {
        if (selectedTemplate == null) return;
        String copyName = selectedTemplate.getName() + " Copy";
        FileTemplate copy = templateManager.duplicateTemplate(selectedTemplate, copyName);
        if (getSelectedCategory() == FileTemplateCategory.OTHER) {
            populateOtherCategoryTree();
        } else {
            currentCategoryTemplates.add(copy);
            templateList.getSelectionModel().select(copy);
        }
        nameField.requestFocus();
        notifyModified();
    }

    private void handleRevertCurrent() {
        if (selectedTemplate != null && selectedTemplate.isBuiltin()) {
            templateManager.revertTemplate(selectedTemplate);
            populateEditor(selectedTemplate);
            if (getSelectedCategory() == FileTemplateCategory.OTHER) {
                templateTree.refresh();
            } else {
                templateList.refresh();
            }
            notifyModified();
        }
    }

    private void updateToolbarButtons() {
        if (selectedTemplate == null) {
            removeBtn.setDisable(true);
            copyBtn.setDisable(true);
            revertBtn.setDisable(true);
        } else {
            removeBtn.setDisable(selectedTemplate.isBuiltin());
            copyBtn.setDisable(false);
            revertBtn.setDisable(!selectedTemplate.isBuiltin() || !selectedTemplate.isModified());
        }
    }

    private void updateRevertLinkVisibility() {
        boolean canRevert = (selectedTemplate != null && selectedTemplate.isBuiltin() && selectedTemplate.isModified());
        revertChangesLink.setVisible(canRevert);
        canRevertProperty.set(canRevert);
        if (selectedTemplate != null) {
            revertBtn.setDisable(!canRevert);
        }
    }

    // =========================================================================
    // INTELLIJ-STYLE ICON FACTORY
    // =========================================================================

    private Node createIconForTemplate(FileTemplate template) {
        return createIconByKey(template.getIconKey());
    }

    private Node createIconForNode(TreeTemplateNode node) {
        return createIconByKey(node.getIconKey());
    }

    private Node createIconByKey(String key) {
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(16, 16);
        iconPane.setMinSize(16, 16);
        iconPane.setMaxSize(16, 16);

        if (key == null) key = "generic";
        key = key.toLowerCase();

        switch (key) {
            case "java" -> {
                Label javaIco = new Label("☕");
                javaIco.setStyle("-fx-font-size: 11px; -fx-text-fill: #E58639;");
                iconPane.getChildren().add(javaIco);
            }
            case "groovy" -> {
                StackPane gBox = new StackPane();
                gBox.setPrefSize(14, 14);
                gBox.setStyle("-fx-background-color: #499844; -fx-background-radius: 2;");
                Label gLabel = new Label("G");
                gLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
                gBox.getChildren().add(gLabel);
                iconPane.getChildren().add(gBox);
            }
            case "html", "xml" -> {
                Label xmlIco = new Label("</>");
                xmlIco.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #4A88C7;");
                iconPane.getChildren().add(xmlIco);
            }
            case "css" -> {
                Label cssIco = new Label("CSS");
                cssIco.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: #3399FF;");
                iconPane.getChildren().add(cssIco);
            }
            case "docker" -> {
                Label dIco = new Label("🐋");
                dIco.setStyle("-fx-font-size: 11px; -fx-text-fill: #2496ED;");
                iconPane.getChildren().add(dIco);
            }
            case "vue" -> {
                Label vIco = new Label("V");
                vIco.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #42B883;");
                iconPane.getChildren().add(vIco);
            }
            case "json", "yaml" -> {
                Label jIco = new Label("{}");
                jIco.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #C678DD;");
                iconPane.getChildren().add(jIco);
            }
            case "gradle" -> {
                Label grIco = new Label("⚙");
                grIco.setStyle("-fx-font-size: 11px; -fx-text-fill: #61AFEF;");
                iconPane.getChildren().add(grIco);
            }
            case "ee" -> {
                StackPane eeBox = new StackPane();
                eeBox.setPrefSize(14, 14);
                eeBox.setStyle("-fx-background-color: #2D5DA7; -fx-background-radius: 2;");
                Label eeLabel = new Label("EE");
                eeLabel.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
                eeBox.getChildren().add(eeLabel);
                iconPane.getChildren().add(eeBox);
            }
            case "maven" -> {
                Label mIco = new Label("m");
                mIco.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #3574F0;");
                iconPane.getChildren().add(mIco);
            }
            case "spring" -> {
                Label spIco = new Label("🍃");
                spIco.setStyle("-fx-font-size: 11px; -fx-text-fill: #6DB33F;");
                iconPane.getChildren().add(spIco);
            }
            case "tomcat" -> {
                Label tcIco = new Label("🐱");
                tcIco.setStyle("-fx-font-size: 11px; -fx-text-fill: #F8DC75;");
                iconPane.getChildren().add(tcIco);
            }
            case "jboss" -> {
                Label jbIco = new Label("W");
                jbIco.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #CC0000;");
                iconPane.getChildren().add(jbIco);
            }
            case "jpa", "cdi" -> {
                Label jpaIco = new Label("⚙");
                jpaIco.setStyle("-fx-font-size: 11px; -fx-text-fill: #9DA0A8;");
                iconPane.getChildren().add(jpaIco);
            }
            case "web" -> {
                Label webIco = new Label("🌐");
                webIco.setStyle("-fx-font-size: 11px; -fx-text-fill: #4A88C7;");
                iconPane.getChildren().add(webIco);
            }
            case "ts" -> {
                StackPane tsBox = new StackPane();
                tsBox.setPrefSize(14, 14);
                tsBox.setStyle("-fx-background-color: #3178C6; -fx-background-radius: 2;");
                Label tsLabel = new Label("TS");
                tsLabel.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
                tsBox.getChildren().add(tsLabel);
                iconPane.getChildren().add(tsBox);
            }
            case "js" -> {
                Label jsIco = new Label("⚛");
                jsIco.setStyle("-fx-font-size: 11px; -fx-text-fill: #61DAFB;");
                iconPane.getChildren().add(jsIco);
            }
            default -> {
                Label docIco = new Label("📄");
                docIco.setStyle("-fx-font-size: 10px; -fx-text-fill: #9DA0A8;");
                iconPane.getChildren().add(docIco);
            }
        }

        return iconPane;
    }

    /**
     * Node representation for the hierarchical TreeView in the Other category.
     */
    public static class TreeTemplateNode {
        private final String title;
        private final FileTemplate template;
        private final boolean isGroup;
        private final String iconKey;

        public TreeTemplateNode(String title, String iconKey) {
            this.title = title;
            this.template = null;
            this.isGroup = true;
            this.iconKey = iconKey;
        }

        public TreeTemplateNode(FileTemplate template) {
            this.title = template.getName();
            this.template = template;
            this.isGroup = false;
            this.iconKey = template.getIconKey();
        }

        public String getTitle() { return title; }
        public FileTemplate getTemplate() { return template; }
        public boolean isGroup() { return isGroup; }
        public String getIconKey() { return iconKey; }

        @Override
        public String toString() { return title; }
    }
}