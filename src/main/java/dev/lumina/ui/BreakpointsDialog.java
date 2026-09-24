package dev.lumina.ui;

import dev.lumina.debugger.DebugBreakpoint;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * IntelliJ IDEA-style Breakpoints dialog (Ctrl+Shift+F8).
 */
public class BreakpointsDialog extends Stage {

    public BreakpointsDialog(Stage owner, List<DebugBreakpoint> initialBreakpoints,
                             Consumer<List<DebugBreakpoint>> onApply) {
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Breakpoints");

        List<DebugBreakpoint> breakpoints = new ArrayList<>(initialBreakpoints);
        ListView<DebugBreakpoint> listView = new ListView<>();
        listView.getItems().setAll(breakpoints);
        listView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22;");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DebugBreakpoint item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    CheckBox cb = new CheckBox(item.displayLabel());
                    cb.setSelected(item.enabled());
                    cb.setStyle("-fx-text-fill: #DFE1E5;");
                    cb.setOnAction(e -> {
                        int idx = getIndex();
                        if (idx >= 0 && idx < breakpoints.size()) {
                            breakpoints.set(idx, new DebugBreakpoint(item.fqcn(), item.line(), cb.isSelected(), item.condition()));
                        }
                    });

                    HBox box = new HBox(6, new Circle(5, Color.web("#E54B4B")), cb);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        Button removeBtn = new Button("−");
        removeBtn.setTooltip(new Tooltip("Remove Breakpoint"));
        removeBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-weight: bold;");
        removeBtn.setOnAction(e -> {
            DebugBreakpoint sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                breakpoints.remove(sel);
                listView.getItems().remove(sel);
            }
        });

        HBox listToolbar = new HBox(6, removeBtn);
        listToolbar.setPadding(new Insets(4));

        VBox leftPane = new VBox(listToolbar, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        leftPane.setPrefWidth(260);

        Label conditionLabel = new Label("Condition:");
        conditionLabel.setStyle("-fx-text-fill: #A8ADBD;");
        TextField conditionField = new TextField();
        conditionField.setPromptText("e.g. request.getEmail() != null");
        conditionField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40;");

        VBox rightPane = new VBox(10, conditionLabel, conditionField);
        rightPane.setPadding(new Insets(10));
        rightPane.setStyle("-fx-background-color: #26282E;");

        SplitPane split = new SplitPane(leftPane, rightPane);
        split.setDividerPositions(0.45);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-padding: 5 16 5 16;");
        closeBtn.setOnAction(e -> {
            if (onApply != null) onApply.accept(breakpoints);
            close();
        });

        HBox bottom = new HBox(closeBtn);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(10));
        bottom.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #32353A transparent transparent transparent;");

        BorderPane root = new BorderPane();
        root.setCenter(split);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 600, 380);
        setScene(scene);
    }
}
