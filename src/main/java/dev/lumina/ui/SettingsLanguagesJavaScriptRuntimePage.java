package dev.lumina.ui;

import dev.lumina.javascript.JavaScriptRuntimeSettings;
import dev.lumina.javascript.JavaScriptRuntimeSettings.RuntimeItem;
import dev.lumina.javascript.JavaScriptSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Languages & Frameworks > JavaScript Runtime settings page in Lumina IDE.
 * Provides configuration and dynamic discovery of Node.js runtimes, package managers, and coding assistance.
 */
public class SettingsLanguagesJavaScriptRuntimePage extends VBox {

    private final JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private ComboBox<String> preferredRuntimeCombo;
    private ComboBox<RuntimeItem> packageManagerCombo;
    private Button packageManagerBrowseBtn;
    private ComboBox<RuntimeItem> nodeRuntimeCombo;
    private Button nodeRuntimeBrowseBtn;
    private CheckBox codingAssistanceCheckBox;

    private JavaScriptRuntimeSettings initialSettings;

    public SettingsLanguagesJavaScriptRuntimePage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        JavaScriptRuntimeSettings current = manager.getRuntimeSettings();

        // 1. Preferred runtime row
        HBox preferredRow = new HBox(10);
        preferredRow.setAlignment(Pos.CENTER_LEFT);

        Label preferredLabel = new Label("Preferred runtime:");
        preferredLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        preferredLabel.setPrefWidth(130);

        preferredRuntimeCombo = new ComboBox<>();
        preferredRuntimeCombo.getItems().addAll(
                "Node.js Auto-detected",
                "Node.js",
                "Deno",
                "Bun"
        );
        preferredRuntimeCombo.setValue(current.getPreferredRuntime());
        preferredRuntimeCombo.setPrefWidth(420);
        styleComboBox(preferredRuntimeCombo);
        preferredRuntimeCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        Label helpIcon = new Label("\u24D8"); // circled info icon
        helpIcon.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 14px; -fx-cursor: hand;");
        helpIcon.setTooltip(new Tooltip("Select the preferred JavaScript runtime environment for your project execution and tooling."));

        preferredRow.getChildren().addAll(preferredLabel, preferredRuntimeCombo, helpIcon);

        // 2. Package manager row
        HBox packageManagerRow = new HBox(10);
        packageManagerRow.setAlignment(Pos.CENTER_LEFT);

        Label packageManagerLabel = new Label("Package manager:");
        packageManagerLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        packageManagerLabel.setPrefWidth(130);

        packageManagerCombo = new ComboBox<>();
        packageManagerCombo.setPrefWidth(420);
        styleComboBox(packageManagerCombo);
        setupRuntimeItemCellFactory(packageManagerCombo);

        // Dynamically detect package managers
        List<RuntimeItem> detectedPackageManagers = JavaScriptRuntimeSettings.detectSystemPackageManagers();
        packageManagerCombo.getItems().addAll(detectedPackageManagers);

        // Match current or select first
        RuntimeItem matchedPm = detectedPackageManagers.stream()
                .filter(item -> item.name().equalsIgnoreCase(current.getPackageManager()) ||
                                item.path().equals(current.getPackageManagerPath()))
                .findFirst()
                .orElse(detectedPackageManagers.get(0));
        packageManagerCombo.setValue(matchedPm);
        packageManagerCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        packageManagerBrowseBtn = new Button("...");
        styleBrowseButton(packageManagerBrowseBtn);
        packageManagerBrowseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Package Manager Executable");
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                RuntimeItem customItem = new RuntimeItem(file.getName(), file.getAbsolutePath(), "");
                packageManagerCombo.getItems().add(customItem);
                packageManagerCombo.setValue(customItem);
                fireModified();
            }
        });

        packageManagerRow.getChildren().addAll(packageManagerLabel, packageManagerCombo, packageManagerBrowseBtn);

        // 3. Section header: Node.js
        HBox nodeHeader = createSectionHeader("Node.js");

        // 4. Node runtime row
        HBox nodeRuntimeRow = new HBox(10);
        nodeRuntimeRow.setAlignment(Pos.CENTER_LEFT);

        Label nodeRuntimeLabel = new Label("Node runtime:");
        nodeRuntimeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nodeRuntimeLabel.setPrefWidth(130);

        nodeRuntimeCombo = new ComboBox<>();
        nodeRuntimeCombo.setPrefWidth(420);
        styleComboBox(nodeRuntimeCombo);
        setupRuntimeItemCellFactory(nodeRuntimeCombo);

        // Dynamically detect Node runtimes
        List<RuntimeItem> detectedNodes = JavaScriptRuntimeSettings.detectSystemNodeRuntimes();
        nodeRuntimeCombo.getItems().addAll(detectedNodes);

        RuntimeItem matchedNode = detectedNodes.stream()
                .filter(item -> item.path().equals(current.getNodeRuntimePath()))
                .findFirst()
                .orElse(detectedNodes.get(0));
        nodeRuntimeCombo.setValue(matchedNode);
        nodeRuntimeCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        nodeRuntimeBrowseBtn = new Button("...");
        styleBrowseButton(nodeRuntimeBrowseBtn);
        nodeRuntimeBrowseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Node.js Executable");
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                RuntimeItem customNode = new RuntimeItem("node", file.getAbsolutePath(), "");
                nodeRuntimeCombo.getItems().add(customNode);
                nodeRuntimeCombo.setValue(customNode);
                fireModified();
            }
        });

        nodeRuntimeRow.getChildren().addAll(nodeRuntimeLabel, nodeRuntimeCombo, nodeRuntimeBrowseBtn);

        // 5. Coding assistance checkbox (indented)
        HBox codingAssistanceRow = new HBox(10);
        codingAssistanceRow.setAlignment(Pos.CENTER_LEFT);
        codingAssistanceRow.setPadding(new Insets(2, 0, 0, 140));

        codingAssistanceCheckBox = new CheckBox("Coding assistance for Node.js");
        codingAssistanceCheckBox.setSelected(current.isCodingAssistanceForNode());
        codingAssistanceCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        codingAssistanceCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        codingAssistanceRow.getChildren().add(codingAssistanceCheckBox);

        getChildren().addAll(
                preferredRow,
                packageManagerRow,
                nodeHeader,
                nodeRuntimeRow,
                codingAssistanceRow
        );
    }

    private void styleComboBox(ComboBox<?> combo) {
        combo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
    }

    private void styleBrowseButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-cursor: hand; -fx-pref-width: 28px;");
    }

    private HBox createSectionHeader(String titleText) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(6, 0, 4, 0));

        Label label = new Label(titleText);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    private void setupRuntimeItemCellFactory(ComboBox<RuntimeItem> combo) {
        javafx.util.Callback<ListView<RuntimeItem>, ListCell<RuntimeItem>> factory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(RuntimeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    BorderPane pane = new BorderPane();
                    Label leftLabel = new Label(item.name() + "  " + item.path());
                    leftLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                    Label rightLabel = new Label(item.version() != null ? item.version() : "");
                    rightLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

                    pane.setLeft(leftLabel);
                    pane.setRight(rightLabel);
                    setGraphic(pane);
                    setText(null);
                }
            }
        };

        combo.setCellFactory(factory);
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(RuntimeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    BorderPane pane = new BorderPane();
                    Label leftLabel = new Label(item.name() + "  " + item.path());
                    leftLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                    Label rightLabel = new Label(item.version() != null ? item.version() : "");
                    rightLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

                    pane.setLeft(leftLabel);
                    pane.setRight(rightLabel);
                    setGraphic(pane);
                    setText(null);
                }
            }
        });
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialSettings = getFormSettings();
    }

    private JavaScriptRuntimeSettings getFormSettings() {
        JavaScriptRuntimeSettings s = new JavaScriptRuntimeSettings();
        if (preferredRuntimeCombo.getValue() != null) {
            s.setPreferredRuntime(preferredRuntimeCombo.getValue());
        }
        RuntimeItem pm = packageManagerCombo.getValue();
        if (pm != null) {
            s.setPackageManager(pm.name());
            s.setPackageManagerPath(pm.path());
            s.setPackageManagerVersion(pm.version());
        }
        RuntimeItem node = nodeRuntimeCombo.getValue();
        if (node != null) {
            s.setNodeRuntime(node.name());
            s.setNodeRuntimePath(node.path());
            s.setNodeRuntimeVersion(node.version());
        }
        s.setCodingAssistanceForNode(codingAssistanceCheckBox.isSelected());
        return s;
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setRuntimeSettings(getFormSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        if (initialSettings != null) {
            preferredRuntimeCombo.setValue(initialSettings.getPreferredRuntime());

            for (RuntimeItem item : packageManagerCombo.getItems()) {
                if (Objects.equals(item.path(), initialSettings.getPackageManagerPath())) {
                    packageManagerCombo.setValue(item);
                    break;
                }
            }

            for (RuntimeItem item : nodeRuntimeCombo.getItems()) {
                if (Objects.equals(item.path(), initialSettings.getNodeRuntimePath())) {
                    nodeRuntimeCombo.setValue(item);
                    break;
                }
            }

            codingAssistanceCheckBox.setSelected(initialSettings.isCodingAssistanceForNode());
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public ComboBox<String> getPreferredRuntimeCombo() {
        return preferredRuntimeCombo;
    }

    public ComboBox<RuntimeItem> getPackageManagerCombo() {
        return packageManagerCombo;
    }

    public Button getPackageManagerBrowseBtn() {
        return packageManagerBrowseBtn;
    }

    public ComboBox<RuntimeItem> getNodeRuntimeCombo() {
        return nodeRuntimeCombo;
    }

    public Button getNodeRuntimeBrowseBtn() {
        return nodeRuntimeBrowseBtn;
    }

    public CheckBox getCodingAssistanceCheckBox() {
        return codingAssistanceCheckBox;
    }
}
