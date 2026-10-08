package dev.lumina.ui;

import dev.lumina.debugger.DebuggerGeneralSettings;
import dev.lumina.debugger.DebuggerSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Debugger.
 * Accurately replicates the UI and behavior shown in Image 1.
 */
public class SettingsDebuggerPage extends VBox {

    private final DebuggerSettingsManager manager = DebuggerSettingsManager.getInstance();

    // Top checkboxes
    private final CheckBox showDebugWindowCheck = new CheckBox("Show debug window on breakpoint");
    private final CheckBox focusAppCheck = new CheckBox("Focus application on breakpoint");
    private final CheckBox hideOnTerminationCheck = new CheckBox("Hide debug window on process termination");
    private final CheckBox scrollPointToCenterCheck = new CheckBox("Scroll execution point to center");
    private final CheckBox clickLineNumberRunCheck = new CheckBox("Click line number to perform run to cursor");

    // Remove breakpoint section
    private final ToggleGroup removeBreakpointGroup = new ToggleGroup();
    private final RadioButton clickLeftRadio = new RadioButton("Click with left mouse button");
    private final RadioButton dragOrMiddleRadio = new RadioButton("Drag to the editor or click with middle mouse button");
    private final CheckBox confirmRemovalCheck = new CheckBox("Confirm removal of conditional or logging breakpoints");

    // Java section
    private final ToggleGroup transportGroup = new ToggleGroup();
    private final RadioButton socketRadio = new RadioButton("Socket");
    private final RadioButton sharedMemoryRadio = new RadioButton("Shared memory");
    private final CheckBox showAltSourceSwitcherCheck = new CheckBox("Show alternative source switcher");
    private final CheckBox killImmediatelyCheck = new CheckBox("Kill the debug process immediately");
    private final CheckBox attachMemoryAgentCheck = new CheckBox("Attach memory agent");

    // Kotlin section
    private final CheckBox attachCoroutineAgentCheck = new CheckBox("Attach coroutine agent");

    private DebuggerGeneralSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDebuggerPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(10);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Top General Checkboxes ---
        styleCheckBox(showDebugWindowCheck);
        styleCheckBox(focusAppCheck);
        styleCheckBox(hideOnTerminationCheck);
        styleCheckBox(scrollPointToCenterCheck);
        styleCheckBox(clickLineNumberRunCheck);

        VBox.setMargin(focusAppCheck, new Insets(0, 0, 0, 22));

        showDebugWindowCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            focusAppCheck.setDisable(!newVal);
            checkModified();
        });

        focusAppCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        hideOnTerminationCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        scrollPointToCenterCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        clickLineNumberRunCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        getChildren().addAll(
                showDebugWindowCheck,
                focusAppCheck,
                hideOnTerminationCheck,
                scrollPointToCenterCheck,
                clickLineNumberRunCheck
        );

        // --- 2. Remove breakpoint section ---
        VBox removeBpBox = new VBox(8);
        removeBpBox.setPadding(new Insets(14, 0, 8, 0));

        Label removeBpLabel = new Label("Remove breakpoint:");
        removeBpLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        clickLeftRadio.setToggleGroup(removeBreakpointGroup);
        dragOrMiddleRadio.setToggleGroup(removeBreakpointGroup);
        styleRadioButton(clickLeftRadio);
        styleRadioButton(dragOrMiddleRadio);

        VBox.setMargin(clickLeftRadio, new Insets(0, 0, 0, 20));
        VBox.setMargin(dragOrMiddleRadio, new Insets(0, 0, 0, 20));

        styleCheckBox(confirmRemovalCheck);
        VBox.setMargin(confirmRemovalCheck, new Insets(0, 0, 0, 20));

        removeBreakpointGroup.selectedToggleProperty().addListener((obs, o, n) -> checkModified());
        confirmRemovalCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        removeBpBox.getChildren().addAll(removeBpLabel, clickLeftRadio, dragOrMiddleRadio, confirmRemovalCheck);
        getChildren().add(removeBpBox);

        // --- 3. Java Section ---
        VBox javaBox = new VBox(10);
        javaBox.setPadding(new Insets(10, 0, 4, 0));

        Node javaHeader = createSectionHeader("Java");

        HBox transportRow = new HBox(12);
        transportRow.setAlignment(Pos.CENTER_LEFT);
        Label transportLabel = new Label("Transport:");
        transportLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        socketRadio.setToggleGroup(transportGroup);
        sharedMemoryRadio.setToggleGroup(transportGroup);
        styleRadioButton(socketRadio);
        styleRadioButton(sharedMemoryRadio);

        transportRow.getChildren().addAll(transportLabel, socketRadio, sharedMemoryRadio);

        styleCheckBox(showAltSourceSwitcherCheck);
        styleCheckBox(killImmediatelyCheck);
        styleCheckBox(attachMemoryAgentCheck);

        transportGroup.selectedToggleProperty().addListener((obs, o, n) -> checkModified());
        showAltSourceSwitcherCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        killImmediatelyCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        attachMemoryAgentCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        javaBox.getChildren().addAll(
                javaHeader,
                transportRow,
                showAltSourceSwitcherCheck,
                killImmediatelyCheck,
                attachMemoryAgentCheck
        );
        getChildren().add(javaBox);

        // --- 4. Kotlin Section ---
        VBox kotlinBox = new VBox(10);
        kotlinBox.setPadding(new Insets(10, 0, 0, 0));

        Node kotlinHeader = createSectionHeader("Kotlin");

        styleCheckBox(attachCoroutineAgentCheck);
        attachCoroutineAgentCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        kotlinBox.getChildren().addAll(kotlinHeader, attachCoroutineAgentCheck);
        getChildren().add(kotlinBox);
    }

    private Node createSectionHeader(String title) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #8C9199; -fx-font-size: 13px;");

        Region line = new Region();
        HBox.setHgrow(line, Priority.ALWAYS);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");

        header.getChildren().addAll(label, line);
        return header;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void loadData() {
        suppressEvents = true;
        initialSettings = manager.getGeneralSettings();

        showDebugWindowCheck.setSelected(initialSettings.isShowDebugWindowOnBreakpoint());
        focusAppCheck.setSelected(initialSettings.isFocusApplicationOnBreakpoint());
        focusAppCheck.setDisable(!initialSettings.isShowDebugWindowOnBreakpoint());

        hideOnTerminationCheck.setSelected(initialSettings.isHideDebugWindowOnProcessTermination());
        scrollPointToCenterCheck.setSelected(initialSettings.isScrollExecutionPointToCenter());
        clickLineNumberRunCheck.setSelected(initialSettings.isClickLineNumberToRunToCursor());

        if (initialSettings.getRemoveBreakpointMode() == DebuggerGeneralSettings.BreakpointRemoveMode.CLICK_LEFT) {
            clickLeftRadio.setSelected(true);
        } else {
            dragOrMiddleRadio.setSelected(true);
        }
        confirmRemovalCheck.setSelected(initialSettings.isConfirmRemovalConditionalOrLogging());

        if (initialSettings.getTransport() == DebuggerGeneralSettings.TransportMode.SOCKET) {
            socketRadio.setSelected(true);
        } else {
            sharedMemoryRadio.setSelected(true);
        }

        showAltSourceSwitcherCheck.setSelected(initialSettings.isShowAlternativeSourceSwitcher());
        killImmediatelyCheck.setSelected(initialSettings.isKillDebugProcessImmediately());
        attachMemoryAgentCheck.setSelected(initialSettings.isAttachMemoryAgent());
        attachCoroutineAgentCheck.setSelected(initialSettings.isAttachCoroutineAgent());

        suppressEvents = false;
        checkModified();
    }

    public DebuggerGeneralSettings getCurrentSettings() {
        DebuggerGeneralSettings s = new DebuggerGeneralSettings();
        s.setShowDebugWindowOnBreakpoint(showDebugWindowCheck.isSelected());
        s.setFocusApplicationOnBreakpoint(focusAppCheck.isSelected());
        s.setHideDebugWindowOnProcessTermination(hideOnTerminationCheck.isSelected());
        s.setScrollExecutionPointToCenter(scrollPointToCenterCheck.isSelected());
        s.setClickLineNumberToRunToCursor(clickLineNumberRunCheck.isSelected());

        s.setRemoveBreakpointMode(clickLeftRadio.isSelected() ?
                DebuggerGeneralSettings.BreakpointRemoveMode.CLICK_LEFT :
                DebuggerGeneralSettings.BreakpointRemoveMode.DRAG_OR_MIDDLE);
        s.setConfirmRemovalConditionalOrLogging(confirmRemovalCheck.isSelected());

        s.setTransport(socketRadio.isSelected() ?
                DebuggerGeneralSettings.TransportMode.SOCKET :
                DebuggerGeneralSettings.TransportMode.SHARED_MEMORY);
        s.setShowAlternativeSourceSwitcher(showAltSourceSwitcherCheck.isSelected());
        s.setKillDebugProcessImmediately(killImmediatelyCheck.isSelected());
        s.setAttachMemoryAgent(attachMemoryAgentCheck.isSelected());
        s.setAttachCoroutineAgent(attachCoroutineAgentCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        if (isModified()) {
            initialSettings = getCurrentSettings();
            manager.setGeneralSettings(initialSettings);
            checkModified();
        }
    }

    public void reset() {
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void checkModified() {
        if (suppressEvents) return;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}