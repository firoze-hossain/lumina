package dev.lumina.ui;

import dev.lumina.javascript.JSHintOption;
import dev.lumina.javascript.JSHintSettings;
import dev.lumina.javascript.JavaScriptSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Languages & Frameworks > JavaScript > Code Quality Tools > JSHint settings page in Lumina IDE.
 */
public class SettingsLanguagesJSJsHintPage extends VBox {

    private final JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox enableCheck;
    private CheckBox useConfigFilesCheck;
    private ComboBox<String> versionCombo;

    private ScrollPane scrollPane;
    private final Map<String, CheckBox> optionCheckBoxes = new LinkedHashMap<>();
    private final Map<String, Label> paramValueLabels = new LinkedHashMap<>();
    private final Map<String, String> paramValues = new LinkedHashMap<>();

    private Label descriptionLabel;
    private static final String DEFAULT_DESC_ENFORCING = "When set to true, these options will make JSHint produce more warnings about your code.";
    private static final String DEFAULT_DESC_RELAXING = "When set to true, these options will make JSHint produce fewer warnings about your code.";
    private static final String DEFAULT_DESC_ENVIRONMENTS = "These options pre-define global variables that are exposed by popular JavaScript libraries and runtime environments.";

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
        enableCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            updateEnabledState();
            fireModified();
        });

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        useConfigFilesCheck = new CheckBox("Use config files");
        useConfigFilesCheck.setSelected(current.isUseConfigFiles());
        useConfigFilesCheck.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand;");
        useConfigFilesCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            updateEnabledState();
            fireModified();
        });

        Label versionLabel = new Label("Version:");
        versionLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");

        versionCombo = new ComboBox<>();
        versionCombo.getItems().addAll("2.13.6 (bundled)", "2.12.0", "Custom package");
        versionCombo.setValue(current.getVersion());
        versionCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        versionCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        topBar.getChildren().addAll(enableCheck, topSpacer, useConfigFilesCheck, versionLabel, versionCombo);

        // 2. Main split area: Options list on left, description pane on right
        HBox contentBox = new HBox(16);
        contentBox.setStyle("-fx-border-color: #393B40; -fx-border-width: 1 0 0 0; -fx-padding: 12 0 0 0;");
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        VBox optionsContainer = new VBox(10);
        optionsContainer.setPadding(new Insets(4, 12, 16, 4));

        buildAllSections(optionsContainer, current);

        scrollPane = new ScrollPane(optionsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        // Right description pane
        VBox descPane = new VBox(10);
        descPane.setPrefWidth(280);
        descPane.setMinWidth(220);
        descPane.setPadding(new Insets(6, 8, 8, 8));

        descriptionLabel = new Label(DEFAULT_DESC_ENFORCING);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-line-spacing: 2px;");
        descPane.getChildren().add(descriptionLabel);

        contentBox.getChildren().addAll(scrollPane, descPane);

        getChildren().addAll(topBar, contentBox);

        updateEnabledState();
    }

    private void updateEnabledState() {
        boolean enabled = enableCheck.isSelected();
        boolean useConfigs = useConfigFilesCheck.isSelected();
        scrollPane.setDisable(!enabled || useConfigs);
        scrollPane.setOpacity((!enabled || useConfigs) ? 0.65 : 1.0);
    }

    private void buildAllSections(VBox container, JSHintSettings current) {
        List<JSHintOption> all = JSHintOption.getAllOptions();

        // 1. Enforcing options
        List<JSHintOption> enforcingList = all.stream()
                .filter(o -> o.getCategory() == JSHintOption.Category.ENFORCING)
                .toList();
        CollapsibleSection enforcingSection = createSection("Enforcing options", DEFAULT_DESC_ENFORCING);
        for (JSHintOption opt : enforcingList) {
            enforcingSection.getContentBox().getChildren().add(createOptionRow(opt, current));
        }
        container.getChildren().addAll(enforcingSection.getHeader(), enforcingSection.getContentBox());

        // 2. Relaxing options
        List<JSHintOption> relaxingList = all.stream()
                .filter(o -> o.getCategory() == JSHintOption.Category.RELAXING)
                .toList();
        CollapsibleSection relaxingSection = createSection("Relaxing options", DEFAULT_DESC_RELAXING);
        for (JSHintOption opt : relaxingList) {
            relaxingSection.getContentBox().getChildren().add(createOptionRow(opt, current));
        }
        container.getChildren().addAll(relaxingSection.getHeader(), relaxingSection.getContentBox());

        // 3. Environments
        List<JSHintOption> envList = all.stream()
                .filter(o -> o.getCategory() == JSHintOption.Category.ENVIRONMENT)
                .toList();
        CollapsibleSection envSection = createSection("Environments", DEFAULT_DESC_ENVIRONMENTS);
        for (JSHintOption opt : envList) {
            envSection.getContentBox().getChildren().add(createOptionRow(opt, current));
        }
        container.getChildren().addAll(envSection.getHeader(), envSection.getContentBox());

        // 4. Trailing options
        List<JSHintOption> trailingList = all.stream()
                .filter(o -> o.getCategory() == JSHintOption.Category.TRAILING)
                .toList();
        VBox trailingBox = new VBox(6);
        trailingBox.setPadding(new Insets(6, 0, 6, 0));
        for (JSHintOption opt : trailingList) {
            trailingBox.getChildren().add(createOptionRow(opt, current));
        }
        container.getChildren().add(trailingBox);
    }

    private static class CollapsibleSection {
        private final HBox header;
        private final VBox contentBox;

        public CollapsibleSection(HBox header, VBox contentBox) {
            this.header = header;
            this.contentBox = contentBox;
        }

        public HBox getHeader() {
            return header;
        }

        public VBox getContentBox() {
            return contentBox;
        }
    }

    private CollapsibleSection createSection(String title, String sectionDescription) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #2B2D30; -fx-padding: 4 8 4 8; -fx-background-radius: 4; -fx-cursor: hand;");

        Label toggleIcon = new Label(" \u2212 "); // minus sign
        toggleIcon.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        header.getChildren().addAll(toggleIcon, titleLabel);

        VBox contentBox = new VBox(6);
        contentBox.setPadding(new Insets(4, 0, 8, 4));

        header.setOnMouseClicked(e -> {
            boolean isVisible = contentBox.isVisible();
            contentBox.setVisible(!isVisible);
            contentBox.setManaged(!isVisible);
            toggleIcon.setText(!isVisible ? " \u2212 " : " + ");
        });

        header.setOnMouseEntered(e -> {
            header.setStyle("-fx-background-color: #35373B; -fx-padding: 4 8 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
            descriptionLabel.setText(sectionDescription);
        });

        header.setOnMouseExited(e -> {
            header.setStyle("-fx-background-color: #2B2D30; -fx-padding: 4 8 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
        });

        return new CollapsibleSection(header, contentBox);
    }

    private Node createOptionRow(JSHintOption opt, JSHintSettings current) {
        if (opt.isParametric()) {
            return createParametricRow(opt, current);
        }
        return createCheckboxRow(opt, current);
    }

    private Node createCheckboxRow(JSHintOption opt, JSHintSettings current) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 4, 2, 4));
        row.setStyle("-fx-background-radius: 3; -fx-cursor: hand;");

        CheckBox cb = new CheckBox();
        cb.setSelected(current.isOptionEnabled(opt.getKey()));
        cb.setStyle("-fx-cursor: hand;");
        cb.selectedProperty().addListener((obs, oldV, newV) -> fireModified());
        optionCheckBoxes.put(opt.getKey(), cb);

        HBox labelBox = new HBox(4);
        labelBox.setAlignment(Pos.CENTER_LEFT);

        // Format label with code badges if applicable
        renderLabelWithBadges(labelBox, opt.getLabel(), opt.getCodeBadges());

        Label tag = new Label(opt.getKey());
        tag.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");

        row.getChildren().addAll(cb, labelBox, tag);

        // Hover & click interactions
        row.setOnMouseClicked(e -> {
            if (e.getTarget() != cb) {
                cb.setSelected(!cb.isSelected());
            }
        });

        row.setOnMouseEntered(e -> {
            row.setStyle("-fx-background-color: #26282E; -fx-background-radius: 3; -fx-cursor: hand;");
            if (!opt.getDescription().isBlank()) {
                descriptionLabel.setText(opt.getDescription());
            }
        });

        row.setOnMouseExited(e -> {
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 3; -fx-cursor: hand;");
        });

        return row;
    }

    private Node createParametricRow(JSHintOption opt, JSHintSettings current) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 4, 2, 24));
        row.setStyle("-fx-background-radius: 3; -fx-cursor: hand;");

        Label label = new Label(opt.getLabel());
        label.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        String val = current.getParamOption(opt.getKey());
        if (val == null || val.isBlank()) {
            val = opt.getParamDefaultValue();
        }
        paramValues.put(opt.getKey(), val);

        Label valLabel = new Label(val);
        valLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        paramValueLabels.put(opt.getKey(), valLabel);

        Hyperlink setLink = new Hyperlink("Set");
        setLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;");
        setLink.setOnMouseEntered(e -> setLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: true;"));
        setLink.setOnMouseExited(e -> setLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;"));

        setLink.setOnAction(e -> promptForParamValue(opt, valLabel));

        Label tag = new Label(opt.getKey());
        tag.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");

        row.getChildren().addAll(label, valLabel, setLink, tag);

        row.setOnMouseEntered(e -> {
            row.setStyle("-fx-background-color: #26282E; -fx-background-radius: 3; -fx-cursor: hand;");
            if (!opt.getDescription().isBlank()) {
                descriptionLabel.setText(opt.getDescription());
            }
        });

        row.setOnMouseExited(e -> {
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 3; -fx-cursor: hand;");
        });

        return row;
    }

    private void promptForParamValue(JSHintOption opt, Label valLabel) {
        TextInputDialog dialog = new TextInputDialog(paramValues.getOrDefault(opt.getKey(), ""));
        dialog.setTitle("Set JSHint Option");
        dialog.setHeaderText("Set value for " + opt.getKey());
        dialog.setContentText(opt.getLabel());
        dialog.showAndWait().ifPresent(newVal -> {
            paramValues.put(opt.getKey(), newVal);
            valLabel.setText(newVal);
            fireModified();
        });
    }

    private void renderLabelWithBadges(HBox container, String text, List<String> badges) {
        if (badges.isEmpty()) {
            Label l = new Label(text);
            l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            container.getChildren().add(l);
            return;
        }

        // Split text around badges
        String remaining = text;
        for (String badge : badges) {
            int idx = remaining.indexOf(badge);
            if (idx >= 0) {
                String prefix = remaining.substring(0, idx);
                if (!prefix.isEmpty()) {
                    Label preLabel = new Label(prefix);
                    preLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    container.getChildren().add(preLabel);
                }

                Label badgeLabel = new Label(badge);
                badgeLabel.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #3E4044; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 0 4 0 4; -fx-font-family: monospace; -fx-font-size: 11px;");
                container.getChildren().add(badgeLabel);

                remaining = remaining.substring(idx + badge.length());
            }
        }
        if (!remaining.isEmpty()) {
            Label postLabel = new Label(remaining);
            postLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            container.getChildren().add(postLabel);
        }
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
        return new JSHintSettings(
                enableCheck.isSelected(),
                useConfigFilesCheck.isSelected(),
                versionCombo.getValue(),
                opts,
                new HashMap<>(paramValues)
        );
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

            paramValues.clear();
            paramValues.putAll(initialSettings.getParamOptions());
            for (Map.Entry<String, Label> e : paramValueLabels.entrySet()) {
                e.getValue().setText(initialSettings.getParamOption(e.getKey()));
            }

            updateEnabledState();
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

    public Map<String, String> getParamValues() {
        return paramValues;
    }

    public Label getDescriptionLabel() {
        return descriptionLabel;
    }
}
