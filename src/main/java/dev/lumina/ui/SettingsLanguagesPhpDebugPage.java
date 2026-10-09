package dev.lumina.ui;

import dev.lumina.php.PhpDebugSettings;
import dev.lumina.php.PhpSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Settings page for Languages & Frameworks > PHP > Debug.
 * Faithfully matches the reference IDE layout and dynamic introspection.
 */
public class SettingsLanguagesPhpDebugPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    // Controls
    // External connections
    private CheckBox ignoreExternalConnectionsCheck;
    private CheckBox breakAtFirstLineCheck;
    private Spinner<Integer> maxSimultaneousConnectionsSpinner;

    // Xdebug
    private TextField xdebugPortField;
    private CheckBox xdebugCanAcceptExternalCheck;
    private CheckBox xdebugResolveBreakpointCheck;
    private CheckBox xdebugMoveBreakpointCheck;
    private CheckBox xdebugForceBreakNoPathMappingCheck;
    private CheckBox xdebugForceBreakOutsideProjectCheck;
    private CheckBox xdebugEnableReturnFunctionValueDebuggingCheck;
    private CheckBox xdebugPredictFutureConditionValuesCheck;
    private CheckBox xdebugGrayOutUnreachableBlocksCheck;

    // Zend Debugger
    private TextField zendDebugPortField;
    private CheckBox zendCanAcceptExternalCheck;
    private TextField zendBroadcastingPortField;
    private CheckBox zendAutoDetectIdeIpCheck;
    private TextField zendDetectedIdeIpField;
    private CheckBox zendIgnoreZRayRequestsCheck;

    // Evaluation
    private CheckBox showArrayAndObjectChildrenCheck;
    private CheckBox safeEvaluationModeCheck;
    private CheckBox importNamespaceAndUseStatementsCheck;
    private CheckBox enableToStringObjectViewCheck;
    private CheckBox enableNavigateLinksCheck;

    // Advanced / Settings
    private VBox settingsCollapsibleContent;
    private Label settingsToggleArrow;
    private boolean settingsExpanded = true;
    private CheckBox detectPathMappingsCheck;
    private CheckBox notifyIfSessionFinishedWithoutPauseCheck;
    private CheckBox passRequiredOptionsThroughCommandLineCheck;
    private CheckBox notifyIfBreakpointResolvedToDifferentLineCheck;

    // Listening toggle
    private boolean isListening = false;
    private Button listeningBtn;

    // Dirty tracking snapshot
    private PhpDebugSettings snapshot;
    private Runnable onModifiedListener;

    public SettingsLanguagesPhpDebugPage() {
        setSpacing(14);
        setPadding(new Insets(16, 22, 24, 22));
        setStyle("-fx-background-color: #1E1F22;");

        buildPreConfigurationSection();
        buildExternalConnectionsSection();
        buildXdebugSection();
        buildZendDebuggerSection();
        buildEvaluationSection();
        buildSettingsSection();

        loadFromManager();
        takeSnapshot();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // ============================================================
    // Pre-configuration Section
    // ============================================================

    private void buildPreConfigurationSection() {
        getChildren().add(createSectionHeader("Pre-configuration"));

        VBox stepsBox = new VBox(8);
        stepsBox.setPadding(new Insets(2, 0, 4, 12));

        // Step 1
        HBox step1 = new HBox(4);
        step1.setAlignment(Pos.CENTER_LEFT);
        Label s1Text1 = createLabel("1. Install ");
        Hyperlink s1XdebugLink = createLink("Xdebug", () -> showInfo("PHP Debugger", "Xdebug extension documentation and installation guide."));
        Label s1Text2 = createLabel(" or ");
        Hyperlink s1ZendLink = createLink("Zend Debugger", () -> showInfo("PHP Debugger", "Zend Debugger documentation and setup guide."));
        Label s1Text3 = createLabel(" on the Web Server.");
        step1.getChildren().addAll(s1Text1, s1XdebugLink, s1Text2, s1ZendLink, s1Text3);

        HBox step1Validate = new HBox(4);
        step1Validate.setAlignment(Pos.CENTER_LEFT);
        step1Validate.setPadding(new Insets(0, 0, 0, 16));
        Hyperlink s1ValLink = createLink("Validate", this::validateDebuggerConfig);
        Label s1ValText = createLabel(" debugger configuration on the Web Server.");
        step1Validate.getChildren().addAll(s1ValLink, s1ValText);

        // Step 2
        HBox step2 = new HBox(4);
        step2.setAlignment(Pos.CENTER_LEFT);
        Label s2Text1 = createLabel("2. Install ");
        Hyperlink s2Link = createLink("browser toolbar or bookmarklets", () -> showInfo("Browser Helpers", "Browser helpers for Xdebug / Zend Debugger sessions."));
        Label s2Text2 = createLabel(".");
        step2.getChildren().addAll(s2Text1, s2Link, s2Text2);

        // Step 3
        HBox step3 = new HBox(6);
        step3.setAlignment(Pos.CENTER_LEFT);
        Label s3Text = createLabel("3. Enable listening for PHP Debug Connections:");
        listeningBtn = new Button("⏰ Start Listening");
        listeningBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;");
        listeningBtn.setOnAction(e -> {
            isListening = !isListening;
            if (isListening) {
                listeningBtn.setText("🛑 Stop Listening");
                listeningBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5FAD65; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;");
            } else {
                listeningBtn.setText("⏰ Start Listening");
                listeningBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;");
            }
        });
        step3.getChildren().addAll(s3Text, listeningBtn);

        // Step 4
        HBox step4 = new HBox(4);
        step4.setAlignment(Pos.CENTER_LEFT);
        Label s4Text = createLabel("4. Start debug session in browser with the toolbar or bookmarklets.");
        step4.getChildren().add(s4Text);

        HBox step4Info = new HBox(4);
        step4Info.setAlignment(Pos.CENTER_LEFT);
        step4Info.setPadding(new Insets(0, 0, 0, 16));
        Label s4InfoText = createLabel("For more information follow ");
        Hyperlink s4TutorialLink = createLink("Zero-configuration Debugging tutorial", () -> showInfo("Tutorial", "Zero-configuration Debugging tutorial for Lumina IDE."));
        step4Info.getChildren().addAll(s4InfoText, s4TutorialLink);

        stepsBox.getChildren().addAll(step1, step1Validate, step2, step3, step4, step4Info);
        getChildren().add(stepsBox);
    }

    private void validateDebuggerConfig() {
        String cli = manager.findSystemPhpBinary();
        String debug = cli != null ? manager.probePhpDebugger(cli) : "None";
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Validate Debugger Configuration");
        alert.setHeaderText("PHP Debugger Configuration Status");
        alert.setContentText("Probed PHP Interpreter: " + (cli != null ? cli : "None") + "\n"
                + "Detected Debugger: " + debug + "\n\n"
                + "Debugger ports " + xdebugPortField.getText() + " are monitored for incoming connections.");
        alert.showAndWait();
    }

    // ============================================================
    // External connections Section
    // ============================================================

    private void buildExternalConnectionsSection() {
        getChildren().add(createSectionHeader("External connections"));

        VBox box = new VBox(8);
        box.setPadding(new Insets(2, 0, 4, 12));

        ignoreExternalConnectionsCheck = createCheckBox("Ignore external connections through unregistered server configurations");
        breakAtFirstLineCheck = createCheckBox("Break at first line in PHP scripts");

        HBox maxConnRow = new HBox(10);
        maxConnRow.setAlignment(Pos.CENTER_LEFT);
        Label maxLabel = createLabel("Max. simultaneous connections:");
        maxSimultaneousConnectionsSpinner = new Spinner<>(1, 100, 3);
        maxSimultaneousConnectionsSpinner.setPrefWidth(75);
        maxSimultaneousConnectionsSpinner.setEditable(true);
        maxSimultaneousConnectionsSpinner.valueProperty().addListener((obs, ov, nv) -> notifyModified());
        styleSpinner(maxSimultaneousConnectionsSpinner);
        maxConnRow.getChildren().addAll(maxLabel, maxSimultaneousConnectionsSpinner);

        box.getChildren().addAll(ignoreExternalConnectionsCheck, breakAtFirstLineCheck, maxConnRow);
        getChildren().add(box);
    }

    // ============================================================
    // Xdebug Section
    // ============================================================

    private void buildXdebugSection() {
        getChildren().add(createSectionHeader("Xdebug"));

        VBox box = new VBox(8);
        box.setPadding(new Insets(2, 0, 4, 12));

        HBox portRow = new HBox(12);
        portRow.setAlignment(Pos.CENTER_LEFT);
        Label portLabel = createLabel("Debug port:");
        xdebugPortField = createTextField("9003,9000", 140);
        xdebugCanAcceptExternalCheck = createCheckBox("Can accept external connections");
        portRow.getChildren().addAll(portLabel, xdebugPortField, xdebugCanAcceptExternalCheck);

        xdebugResolveBreakpointCheck = createCheckBox("Resolve breakpoint if it's not available on the current line (Xdebug 2.8+)");
        xdebugMoveBreakpointCheck = createCheckBox("Move breakpoint to resolved position if it's different from the source");
        xdebugForceBreakNoPathMappingCheck = createCheckBox("Force break at first line when no path mapping specified");
        xdebugForceBreakOutsideProjectCheck = createCheckBox("Force break at first line when a script is outside the project");
        xdebugEnableReturnFunctionValueDebuggingCheck = createCheckBox("Enable return function value debugging (Xdebug 3.2+)");
        xdebugPredictFutureConditionValuesCheck = createCheckBox("Predict future condition values analyzing program data flow");
        xdebugGrayOutUnreachableBlocksCheck = createCheckBox("Gray out blocks of code that are predicted to be unreachable");

        box.getChildren().addAll(
                portRow,
                xdebugResolveBreakpointCheck,
                xdebugMoveBreakpointCheck,
                xdebugForceBreakNoPathMappingCheck,
                xdebugForceBreakOutsideProjectCheck,
                xdebugEnableReturnFunctionValueDebuggingCheck,
                xdebugPredictFutureConditionValuesCheck,
                xdebugGrayOutUnreachableBlocksCheck
        );
        getChildren().add(box);
    }

    // ============================================================
    // Zend Debugger Section
    // ============================================================

    private void buildZendDebuggerSection() {
        getChildren().add(createSectionHeader("Zend Debugger"));

        VBox box = new VBox(8);
        box.setPadding(new Insets(2, 0, 4, 12));

        HBox portRow = new HBox(12);
        portRow.setAlignment(Pos.CENTER_LEFT);
        Label portLabel = createLabel("Debug port:");
        portLabel.setPrefWidth(160);
        zendDebugPortField = createTextField("10137", 100);
        zendCanAcceptExternalCheck = createCheckBox("Can accept external connections");
        portRow.getChildren().addAll(portLabel, zendDebugPortField, zendCanAcceptExternalCheck);

        HBox bcastRow = new HBox(12);
        bcastRow.setAlignment(Pos.CENTER_LEFT);
        Label bcastLabel = createLabel("Settings broadcasting port:");
        bcastLabel.setPrefWidth(160);
        zendBroadcastingPortField = createTextField("20080", 100);
        bcastRow.getChildren().addAll(bcastLabel, zendBroadcastingPortField);

        HBox ipRow = new HBox(12);
        ipRow.setAlignment(Pos.CENTER_LEFT);
        zendAutoDetectIdeIpCheck = createCheckBox("Automatically detect IDE IP:");
        zendAutoDetectIdeIpCheck.setPrefWidth(185);
        zendDetectedIdeIpField = createTextField("", 450);
        HBox.setHgrow(zendDetectedIdeIpField, Priority.ALWAYS);
        ipRow.getChildren().addAll(zendAutoDetectIdeIpCheck, zendDetectedIdeIpField);

        zendIgnoreZRayRequestsCheck = createCheckBox("Ignore Z-Ray system requests");

        box.getChildren().addAll(portRow, bcastRow, ipRow, zendIgnoreZRayRequestsCheck);
        getChildren().add(box);
    }

    // ============================================================
    // Evaluation Section
    // ============================================================

    private void buildEvaluationSection() {
        getChildren().add(createSectionHeader("Evaluation"));

        VBox box = new VBox(8);
        box.setPadding(new Insets(2, 0, 4, 12));

        showArrayAndObjectChildrenCheck = createCheckBox("Show array and object children in Debug Console");
        safeEvaluationModeCheck = createCheckBox("Safe evaluation mode in value hints and Watches Frame");
        importNamespaceAndUseStatementsCheck = createCheckBox("Import namespace and 'use' statements from evaluation context");
        enableToStringObjectViewCheck = createCheckBox("Enable '__toString' object view");
        enableNavigateLinksCheck = createCheckBox("Enable '... Navigate' links for class and member references");

        box.getChildren().addAll(
                showArrayAndObjectChildrenCheck,
                safeEvaluationModeCheck,
                importNamespaceAndUseStatementsCheck,
                enableToStringObjectViewCheck,
                enableNavigateLinksCheck
        );
        getChildren().add(box);
    }

    // ============================================================
    // Settings Section (Collapsible)
    // ============================================================

    private void buildSettingsSection() {
        HBox headerRow = new HBox(6);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setCursor(javafx.scene.Cursor.HAND);

        settingsToggleArrow = new Label("▾");
        settingsToggleArrow.setStyle("-fx-text-fill: #9DA0A8; -fx-font-size: 13px;");

        Label title = new Label("Settings");
        title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        headerRow.getChildren().addAll(settingsToggleArrow, title, line);

        settingsCollapsibleContent = new VBox(8);
        settingsCollapsibleContent.setPadding(new Insets(2, 0, 8, 12));

        detectPathMappingsCheck = createCheckBox("Detect path mappings from deployment configurations");
        notifyIfSessionFinishedWithoutPauseCheck = createCheckBox("Notify if debug session was finished without being paused");
        passRequiredOptionsThroughCommandLineCheck = createCheckBox("Pass required configuration options through command line (still need to enable debug extension manually)");
        notifyIfBreakpointResolvedToDifferentLineCheck = createCheckBox("Notify if breakpoint was resolved to a different line (Xdebug 2.8+)");

        settingsCollapsibleContent.getChildren().addAll(
                detectPathMappingsCheck,
                notifyIfSessionFinishedWithoutPauseCheck,
                passRequiredOptionsThroughCommandLineCheck,
                notifyIfBreakpointResolvedToDifferentLineCheck
        );

        headerRow.setOnMouseClicked(e -> {
            settingsExpanded = !settingsExpanded;
            settingsToggleArrow.setText(settingsExpanded ? "▾" : "▸");
            settingsCollapsibleContent.setVisible(settingsExpanded);
            settingsCollapsibleContent.setManaged(settingsExpanded);
        });

        getChildren().addAll(headerRow, settingsCollapsibleContent);
    }

    // ============================================================
    // Helpers & Styling
    // ============================================================

    private Node createSectionHeader(String titleText) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

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

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return label;
    }

    private Hyperlink createLink(String text, Runnable action) {
        Hyperlink link = new Hyperlink(text);
        link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;");
        link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
        link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
        link.setOnAction(e -> action.run());
        return link;
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, ov, nv) -> notifyModified());
        return cb;
    }

    private TextField createTextField(String text, double width) {
        TextField tf = new TextField(text);
        if (width > 0) {
            tf.setPrefWidth(width);
        }
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        tf.textProperty().addListener((obs, ov, nv) -> notifyModified());
        return tf;
    }

    private void styleSpinner(Spinner<?> spinner) {
        spinner.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4;");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ============================================================
    // Data Loading & Dirty Tracking
    // ============================================================

    public void loadFromManager() {
        PhpDebugSettings s = manager.getDebugSettings();

        ignoreExternalConnectionsCheck.setSelected(s.isIgnoreExternalConnections());
        breakAtFirstLineCheck.setSelected(s.isBreakAtFirstLine());
        maxSimultaneousConnectionsSpinner.getValueFactory().setValue(s.getMaxSimultaneousConnections());

        xdebugPortField.setText(s.getXdebugPort());
        xdebugCanAcceptExternalCheck.setSelected(s.isXdebugCanAcceptExternalConnections());
        xdebugResolveBreakpointCheck.setSelected(s.isXdebugResolveBreakpoint());
        xdebugMoveBreakpointCheck.setSelected(s.isXdebugMoveBreakpoint());
        xdebugForceBreakNoPathMappingCheck.setSelected(s.isXdebugForceBreakNoPathMapping());
        xdebugForceBreakOutsideProjectCheck.setSelected(s.isXdebugForceBreakOutsideProject());
        xdebugEnableReturnFunctionValueDebuggingCheck.setSelected(s.isXdebugEnableReturnFunctionValueDebugging());
        xdebugPredictFutureConditionValuesCheck.setSelected(s.isXdebugPredictFutureConditionValues());
        xdebugGrayOutUnreachableBlocksCheck.setSelected(s.isXdebugGrayOutUnreachableBlocks());

        zendDebugPortField.setText(s.getZendDebugPort());
        zendCanAcceptExternalCheck.setSelected(s.isZendCanAcceptExternalConnections());
        zendBroadcastingPortField.setText(s.getZendBroadcastingPort());
        zendAutoDetectIdeIpCheck.setSelected(s.isZendAutoDetectIdeIp());
        zendDetectedIdeIpField.setText(s.getZendDetectedIdeIp());
        zendIgnoreZRayRequestsCheck.setSelected(s.isZendIgnoreZRayRequests());

        showArrayAndObjectChildrenCheck.setSelected(s.isShowArrayAndObjectChildren());
        safeEvaluationModeCheck.setSelected(s.isSafeEvaluationMode());
        importNamespaceAndUseStatementsCheck.setSelected(s.isImportNamespaceAndUseStatements());
        enableToStringObjectViewCheck.setSelected(s.isEnableToStringObjectView());
        enableNavigateLinksCheck.setSelected(s.isEnableNavigateLinks());

        detectPathMappingsCheck.setSelected(s.isDetectPathMappings());
        notifyIfSessionFinishedWithoutPauseCheck.setSelected(s.isNotifyIfSessionFinishedWithoutPause());
        passRequiredOptionsThroughCommandLineCheck.setSelected(s.isPassRequiredOptionsThroughCommandLine());
        notifyIfBreakpointResolvedToDifferentLineCheck.setSelected(s.isNotifyIfBreakpointResolvedToDifferentLine());
    }

    public void takeSnapshot() {
        PhpDebugSettings current = buildCurrentSettings();
        this.snapshot = new PhpDebugSettings(current);
    }

    public PhpDebugSettings buildCurrentSettings() {
        PhpDebugSettings s = manager.getDebugSettings();
        s.setIgnoreExternalConnections(ignoreExternalConnectionsCheck.isSelected());
        s.setBreakAtFirstLine(breakAtFirstLineCheck.isSelected());
        s.setMaxSimultaneousConnections(maxSimultaneousConnectionsSpinner.getValue());

        s.setXdebugPort(xdebugPortField.getText());
        s.setXdebugCanAcceptExternalConnections(xdebugCanAcceptExternalCheck.isSelected());
        s.setXdebugResolveBreakpoint(xdebugResolveBreakpointCheck.isSelected());
        s.setXdebugMoveBreakpoint(xdebugMoveBreakpointCheck.isSelected());
        s.setXdebugForceBreakNoPathMapping(xdebugForceBreakNoPathMappingCheck.isSelected());
        s.setXdebugForceBreakOutsideProject(xdebugForceBreakOutsideProjectCheck.isSelected());
        s.setXdebugEnableReturnFunctionValueDebugging(xdebugEnableReturnFunctionValueDebuggingCheck.isSelected());
        s.setXdebugPredictFutureConditionValues(xdebugPredictFutureConditionValuesCheck.isSelected());
        s.setXdebugGrayOutUnreachableBlocks(xdebugGrayOutUnreachableBlocksCheck.isSelected());

        s.setZendDebugPort(zendDebugPortField.getText());
        s.setZendCanAcceptExternalConnections(zendCanAcceptExternalCheck.isSelected());
        s.setZendBroadcastingPort(zendBroadcastingPortField.getText());
        s.setZendAutoDetectIdeIp(zendAutoDetectIdeIpCheck.isSelected());
        s.setZendDetectedIdeIp(zendDetectedIdeIpField.getText());
        s.setZendIgnoreZRayRequests(zendIgnoreZRayRequestsCheck.isSelected());

        s.setShowArrayAndObjectChildren(showArrayAndObjectChildrenCheck.isSelected());
        s.setSafeEvaluationMode(safeEvaluationModeCheck.isSelected());
        s.setImportNamespaceAndUseStatements(importNamespaceAndUseStatementsCheck.isSelected());
        s.setEnableToStringObjectView(enableToStringObjectViewCheck.isSelected());
        s.setEnableNavigateLinks(enableNavigateLinksCheck.isSelected());

        s.setDetectPathMappings(detectPathMappingsCheck.isSelected());
        s.setNotifyIfSessionFinishedWithoutPause(notifyIfSessionFinishedWithoutPauseCheck.isSelected());
        s.setPassRequiredOptionsThroughCommandLine(passRequiredOptionsThroughCommandLineCheck.isSelected());
        s.setNotifyIfBreakpointResolvedToDifferentLine(notifyIfBreakpointResolvedToDifferentLineCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (snapshot == null) return false;
        PhpDebugSettings current = buildCurrentSettings();
        return !current.equals(snapshot);
    }

    public void apply() {
        PhpDebugSettings current = buildCurrentSettings();
        manager.setDebugSettings(current);
        takeSnapshot();
        notifyModified();
    }

    public void reset() {
        loadFromManager();
        takeSnapshot();
        notifyModified();
    }

    public void revertChanges() {
        if (snapshot != null) {
            PhpDebugSettings s = snapshot;
            ignoreExternalConnectionsCheck.setSelected(s.isIgnoreExternalConnections());
            breakAtFirstLineCheck.setSelected(s.isBreakAtFirstLine());
            maxSimultaneousConnectionsSpinner.getValueFactory().setValue(s.getMaxSimultaneousConnections());

            xdebugPortField.setText(s.getXdebugPort());
            xdebugCanAcceptExternalCheck.setSelected(s.isXdebugCanAcceptExternalConnections());
            xdebugResolveBreakpointCheck.setSelected(s.isXdebugResolveBreakpoint());
            xdebugMoveBreakpointCheck.setSelected(s.isXdebugMoveBreakpoint());
            xdebugForceBreakNoPathMappingCheck.setSelected(s.isXdebugForceBreakNoPathMapping());
            xdebugForceBreakOutsideProjectCheck.setSelected(s.isXdebugForceBreakOutsideProject());
            xdebugEnableReturnFunctionValueDebuggingCheck.setSelected(s.isXdebugEnableReturnFunctionValueDebugging());
            xdebugPredictFutureConditionValuesCheck.setSelected(s.isXdebugPredictFutureConditionValues());
            xdebugGrayOutUnreachableBlocksCheck.setSelected(s.isXdebugGrayOutUnreachableBlocks());

            zendDebugPortField.setText(s.getZendDebugPort());
            zendCanAcceptExternalCheck.setSelected(s.isZendCanAcceptExternalConnections());
            zendBroadcastingPortField.setText(s.getZendBroadcastingPort());
            zendAutoDetectIdeIpCheck.setSelected(s.isZendAutoDetectIdeIp());
            zendDetectedIdeIpField.setText(s.getZendDetectedIdeIp());
            zendIgnoreZRayRequestsCheck.setSelected(s.isZendIgnoreZRayRequests());

            showArrayAndObjectChildrenCheck.setSelected(s.isShowArrayAndObjectChildren());
            safeEvaluationModeCheck.setSelected(s.isSafeEvaluationMode());
            importNamespaceAndUseStatementsCheck.setSelected(s.isImportNamespaceAndUseStatements());
            enableToStringObjectViewCheck.setSelected(s.isEnableToStringObjectView());
            enableNavigateLinksCheck.setSelected(s.isEnableNavigateLinks());

            detectPathMappingsCheck.setSelected(s.isDetectPathMappings());
            notifyIfSessionFinishedWithoutPauseCheck.setSelected(s.isNotifyIfSessionFinishedWithoutPause());
            passRequiredOptionsThroughCommandLineCheck.setSelected(s.isPassRequiredOptionsThroughCommandLine());
            notifyIfBreakpointResolvedToDifferentLineCheck.setSelected(s.isNotifyIfBreakpointResolvedToDifferentLine());
            notifyModified();
        }
    }

    // Getters for testing
    public TextField getXdebugPortField() {
        return xdebugPortField;
    }

    public TextField getZendDebugPortField() {
        return zendDebugPortField;
    }

    public TextField getZendDetectedIdeIpField() {
        return zendDetectedIdeIpField;
    }

    public CheckBox getBreakAtFirstLineCheck() {
        return breakAtFirstLineCheck;
    }
}
