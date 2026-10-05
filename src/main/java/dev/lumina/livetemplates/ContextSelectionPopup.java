package dev.lumina.livetemplates;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * IntelliJ IDEA-style context popup triggered by clicking 'Change ˅' / 'Define'.
 * Displays a hierarchical checklist of applicability contexts.
 */
public class ContextSelectionPopup {

    public static void show(Node anchor, LiveTemplate template, Consumer<String> onContextChanged) {
        if (anchor == null || template == null) return;

        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(6);
        content.setPadding(new Insets(10, 14, 10, 14));
        content.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 14, 0.2, 0, 4);"
        );
        content.setPrefWidth(240);
        content.setMaxHeight(320);

        Label header = new Label("Applicable in:");
        header.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-weight: bold;");
        content.getChildren().add(header);

        VBox list = new VBox(4);
        list.setStyle("-fx-background-color: transparent;");

        Set<String> contexts = template.getContexts();
        List<LiveTemplateContext.ContextNode> tree = LiveTemplateContext.getContextTree();

        for (LiveTemplateContext.ContextNode rootNode : tree) {
            CheckBox cb = createCheckBox(rootNode.displayName, rootNode.id, contexts, template, onContextChanged);
            list.getChildren().add(cb);

            for (LiveTemplateContext.ContextNode child : rootNode.children) {
                CheckBox childCb = createCheckBox(child.displayName, child.id, contexts, template, onContextChanged);
                VBox.setMargin(childCb, new Insets(0, 0, 0, 16));
                list.getChildren().add(childCb);
            }
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setPrefHeight(260);

        content.getChildren().add(scroll);
        popup.getContent().add(content);

        Point2D p = anchor.localToScreen(0, anchor.getBoundsInLocal().getHeight());
        if (p != null) {
            popup.show(anchor.getScene().getWindow(), p.getX(), p.getY() + 4);
        }
    }

    private static CheckBox createCheckBox(String label, String id, Set<String> contexts,
                                           LiveTemplate template, Consumer<String> onContextChanged) {
        CheckBox cb = new CheckBox(label);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        cb.setSelected(contexts.contains(id));
        cb.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) {
                contexts.add(id);
            } else {
                contexts.remove(id);
            }
            String formatted = LiveTemplateContext.formatApplicableText(contexts);
            if (onContextChanged != null) {
                onContextChanged.accept(formatted);
            }
        });
        return cb;
    }
}
