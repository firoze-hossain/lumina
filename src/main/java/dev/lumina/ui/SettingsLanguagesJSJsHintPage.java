package dev.lumina.ui;

import dev.lumina.javascript.JSHintOption;
import dev.lumina.javascript.JSHintSettings;
import dev.lumina.javascript.JavaScriptSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/**
 * 1:1 dynamic replica of IntelliJ IDEA Languages & Frameworks > JavaScript > Code Quality Tools > JSHint.
 * Matching Screenshot 5.
 */
public class SettingsLanguagesJSJsHintPage extends VBox {

    private final JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox enableCheck;
    private CheckBox useConfigFilesCheck;
    private ComboBox<String> versionCombo;

    private final Map<String, CheckBox> optionCheckBoxes = new LinkedHashMap<>();
    private Label descriptionLabel;

    private JSHintSettings initialSettings;

    public SettingsLanguagesJSJsHintPage() {
        setSpacing(12);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        JSHintSettings current = manager.getJshintSettings();

        // 1. Top bar
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);

        enableCheck = new CheckBox("Enable");
        enableCheck.setSelected(current.isEnabled());
        enableCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        enableCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        useConfigFilesCheck = new CheckBox("Use config files");
        useConfigFilesCheck.setSelected(current.isUseConfigFiles());
        useConfigFilesCheck.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand;");
        useConfigFilesCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        Label versionLabel = new Label("Version:");
        versionLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");

        versionCombo = new ComboBox<>();
        versionCombo.getItems().addAll("2.13.6 (bundled)", "2.12.0", "Custom package");
        versionCombo.setValue(current.getVersion());
        versionCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        versionCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        topBar.getChildren().addAll(enableCheck, topSpacer, useConfigFilesCheck, versionLabel, versionCombo);

        // 2. Middle area: Split or two-column box
        HBox contentBox = new HBox(16);
        contentBox.setStyle("-fx-border-color: #393B40; -fx-border-width: 1 0 0 0; -fx-padding: 12 0 0 0;");
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        // Left options scroll pane
        VBox optionsList = new VBox(8);
        optionsList.setPadding(new Insets(4, 12, 12, 4));

        buildOptionsList(optionsList, current);

        ScrollPane scrollPane = new ScrollPane(optionsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        // Right description label
        VBox descPane = new VBox(10);
        descPane.setPrefWidth(280);
        descPane.setMinWidth(220);
        descPane.setPadding(new Insets(8));

        descriptionLabel = new Label("When set to true, these options will make JSHint produce more warnings about your code.");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-line-spacing: 2px;");
        descPane.getChildren().add(descriptionLabel);

        contentBox.getChildren().addAll(scrollPane, descPane);

        getChildren().addAll(topBar, contentBox);
    }

    private void buildOptionsList(VBox container, JSHintSettings current) {
        // Enforcing header
        Label enforcingHeader = new Label("— Enforcing options");
        enforcingHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 4 0 4 0;");
        container.getChildren().add(enforcingHeader);

        List<JSHintOption> enforcing = List.of(
                new JSHintOption("bitwise", "Warn about using bitwise operators", JSHintOption.Category.ENFORCING, true),
                new JSHintOption("camelcase", "Warn about variable naming", JSHintOption.Category.ENFORCING, false),
                new JSHintOption("curly", "Warn when blocks omit {}", JSHintOption.Category.ENFORCING, true),
                new JSHintOption("enforceall", "Warn when code doesn't follow the most strict configuration", JSHintOption.Category.ENFORCING, false),
                new JSHintOption("eqeqeq", "Warn about unsafe comparisons", JSHintOption.Category.ENFORCING, true),
                new JSHintOption("es3", "Warn about incompatibilities with the ES3 specification", JSHintOption.Category.ENFORCING, false),
                new JSHintOption("es5", "Warn about incompatibilities with the ES5 specification", JSHintOption.Category.ENFORCING, false),
                new JSHintOption("forin", "Warn about unsafe for..in", JSHintOption.Category.ENFORCING, true),
                new JSHintOption("freeze", "Warn about overwriting prototypes of native objects", JSHintOption.Category.ENFORCING, false),
                new JSHintOption("immed", "Warn about the use of immediate function invocations without wrapping them in parentheses", JSHintOption.Category.ENFORCING, false),
                new JSHintOption("newcap", "Warn about the use of a uncapitalized constructor", JSHintOption.Category.ENFORCING, false),
                new JSHintOption("noarg", "Warn about arguments.caller and .callee", JSHintOption.Category.ENFORCING, true),
                new JSHintOption("nocomma", "Warn about the use of the comma operator", JSHintOption.Category.ENFORCING, false),
                new JSHintOption("noempty", "Warn about empty blocks", JSHintOption.Category.ENFORCING, true),
                new JSHintOption("nonbsp", "Warn about \"non-breaking whitespace\" characters", JSHintOption.Category.ENFORCING, false),
                new JSHintOption("nonew", "Warn about new usage for side effects", JSHintOption.Category.ENFORCING, true),
                new JSHintOption("undef", "Warn when variable is undefined", JSHintOption.Category.ENFORCING, true),
                new JSHintOption("varstmt", "Warn about the use of VariableStatements", JSHintOption.Category.ENFORCING, false)
        );

        for (JSHintOption opt : enforcing) {
            container.getChildren().add(createOptionRow(opt, current));
        }

        // Additional Enforcing text options
        List<String> paramOptions = List.of(
                "Warn about incompatibilities with the specified ECMAScript version: any Set esversion",
                "Warn about the use of a variable before it was defined: false Set latedef",
                "Warn about unused variables: false Set unused",
                "Indentation: any Set indent",
                "Quotation marks: false Set quotmark",
                "Max number of formal parameter in a function: any Set maxparams",
                "Max depth of your blocks: any Set maxdepth",
                "Max number of statements in a function: any Set maxstatements",
                "Max cyclomatic complexity throughout your code: any Set maxcomplexity",
                "Max length of a line: any Set maxlen"
        );
        for (String param : paramOptions) {
            Label pLabel = new Label(param);
            pLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-padding: 3 0 3 24;");
            container.getChildren().add(pLabel);
        }

        // Relaxing header
        Label relaxingHeader = new Label("— Relaxing options");
        relaxingHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 0 4 0;");
        container.getChildren().add(relaxingHeader);

        List<JSHintOption> relaxing = List.of(
                new JSHintOption("funcscope", "Suppress warnings about variable usage outside of its declared block", JSHintOption.Category.RELAXING, false),
                new JSHintOption("futurehostile", "Warns about the use of identifiers which are defined in future versions of JavaScript", JSHintOption.Category.RELAXING, false),
                new JSHintOption("globalstrict", "Suppress warnings about the use of global strict mode", JSHintOption.Category.RELAXING, false),
                new JSHintOption("iterator", "Suppress warnings about the __iterator__ property", JSHintOption.Category.RELAXING, false),
                new JSHintOption("notypeof", "Suppress warnings about invalid typeof operator values", JSHintOption.Category.RELAXING, false),
                new JSHintOption("shadow", "Suppress warnings about variable shadowing", JSHintOption.Category.RELAXING, false)
        );

        for (JSHintOption opt : relaxing) {
            container.getChildren().add(createOptionRow(opt, current));
        }
    }

    private HBox createOptionRow(JSHintOption opt, JSHintSettings current) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        CheckBox cb = new CheckBox(opt.getLabel());
        cb.setSelected(current.isOptionEnabled(opt.getKey()));
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        cb.selectedProperty().addListener((obs, oldV, newV) -> fireModified());
        optionCheckBoxes.put(opt.getKey(), cb);

        Label tag = new Label(opt.getKey());
        tag.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");

        row.getChildren().addAll(cb, tag);
        return row;
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

    private JSHintSettings getFormSettings() {
        Map<String, Boolean> opts = new HashMap<>();
        for (Map.Entry<String, CheckBox> e : optionCheckBoxes.entrySet()) {
            opts.put(e.getKey(), e.getValue().isSelected());
        }
        return new JSHintSettings(enableCheck.isSelected(), useConfigFilesCheck.isSelected(), versionCombo.getValue(), opts);
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setJshintSettings(getFormSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        if (initialSettings != null) {
            enableCheck.setSelected(initialSettings.isEnabled());
            useConfigFilesCheck.setSelected(initialSettings.isUseConfigFiles());
            versionCombo.setValue(initialSettings.getVersion());
            for (Map.Entry<String, CheckBox> e : optionCheckBoxes.entrySet()) {
                e.getValue().setSelected(initialSettings.isOptionEnabled(e.getKey()));
            }
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public CheckBox getEnableCheck() {
        return enableCheck;
    }

    public CheckBox getUseConfigFilesCheck() {
        return useConfigFilesCheck;
    }

    public ComboBox<String> getVersionCombo() {
        return versionCombo;
    }

    public Map<String, CheckBox> getOptionCheckBoxes() {
        return optionCheckBoxes;
    }
}
