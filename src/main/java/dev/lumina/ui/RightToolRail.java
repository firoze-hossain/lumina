package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.function.IntConsumer;

/** Vertical right-hand tool-window selector, modelled after the IntelliJ new UI. */
public final class RightToolRail extends VBox {
    public RightToolRail(IntConsumer onSelect) {
        getStyleClass().add("right-tool-rail");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(8);
        setPadding(new Insets(10, 4, 10, 4));
        ToggleGroup group = new ToggleGroup();
        add(group, "♧", "Notifications", 0, onSelect);
        add(group, "◉", "AI Chat", 1, onSelect);
        add(group, "▤", "Database", 2, onSelect);
        add(group, "m", "Maven", 3, onSelect);
        add(group, "▧", "Services", 4, onSelect);
        add(group, "♧", "GitHub Copilot", 5, onSelect);
    }

    private void add(ToggleGroup group, String glyph, String tooltip, int index, IntConsumer onSelect) {
        ToggleButton button = new ToggleButton(glyph);
        button.setToggleGroup(group);
        button.getStyleClass().add("right-rail-button");
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(e -> onSelect.accept(index));
        if (index == 3) button.setSelected(true);
        getChildren().add(button);
    }
}
