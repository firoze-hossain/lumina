package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Standard tool window header bar for docked bottom and side panels.
 * Provides a title label, optional sub-tabs, action buttons, an Options menu (⋮),
 * and a Hide button (—) with Shift+Escape shortcut support.
 */
public class ToolWindowHeader extends HBox {

    private final Label titleLabel = new Label();
    private final HBox leftContainer = new HBox(4);
    private final HBox tabsContainer = new HBox(4);
    private final HBox rightButtonsContainer = new HBox(4);
    private final Button optionsButton;
    private final Button hideButton;

    private Runnable onHide;
    private Runnable onMaximize;
    private Consumer<Boolean> onToolbarToggle;
    private Consumer<String> onMoveTo;

    private boolean showToolbar = true;
    private boolean groupTabs = false;
    private String activeViewMode = "Dock Pinned";
    private String activeMoveToPosition = "Bottom";

    private final List<MenuItem> extraMenuItems = new ArrayList<>();
    private final List<HBox> tabItems = new ArrayList<>();
    private HBox activeTabItem;

    public ToolWindowHeader(String title) {
        setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0; -fx-padding: 0 8 0 8; -fx-min-height: 28px; -fx-pref-height: 28px; -fx-max-height: 28px;");
        setAlignment(Pos.CENTER_LEFT);

        titleLabel.setText(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 2 8 2 2;");

        leftContainer.setAlignment(Pos.CENTER_LEFT);
        tabsContainer.setAlignment(Pos.CENTER_LEFT);
        rightButtonsContainer.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        optionsButton = createToolbarIconButton("⋮", "Options", null);
        optionsButton.setOnAction(e -> showOptionsMenu(optionsButton));

        hideButton = createToolbarIconButton("—", "Hide ⇧⎋", () -> {
            if (onHide != null) {
                onHide.run();
            }
        });

        rightButtonsContainer.getChildren().addAll(optionsButton, hideButton);

        getChildren().addAll(titleLabel, leftContainer, tabsContainer, spacer, rightButtonsContainer);

        // Shift+Escape shortcut listener
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isShiftDown() && e.getCode() == KeyCode.ESCAPE) {
                if (onHide != null) {
                    onHide.run();
                    e.consume();
                }
            }
        });
    }

    public void setOnHide(Runnable onHide) {
        this.onHide = onHide;
    }

    public void setOnMaximize(Runnable onMaximize) {
        this.onMaximize = onMaximize;
    }

    public void setOnToolbarToggle(Consumer<Boolean> onToolbarToggle) {
        this.onToolbarToggle = onToolbarToggle;
    }

    public void setOnMoveTo(Consumer<String> onMoveTo) {
        this.onMoveTo = onMoveTo;
    }

    public void setActiveMoveToPosition(String activeMoveToPosition) {
        this.activeMoveToPosition = activeMoveToPosition;
    }

    public String getActiveMoveToPosition() {
        return activeMoveToPosition;
    }

    public Button getOptionsButton() {
        return optionsButton;
    }

    public Button getHideButton() {
        return hideButton;
    }

    public Label getTitleLabel() {
        return titleLabel;
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void addOptionsMenuItem(MenuItem item) {
        extraMenuItems.add(item);
    }

    public Button addLeftIconButton(String icon, String tooltipText, Runnable action) {
        Button btn = createToolbarIconButton(icon, tooltipText, action);
        leftContainer.getChildren().add(btn);
        return btn;
    }

    public Button addRightActionButton(String text, String tooltipText, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6;");
        if (tooltipText != null && !tooltipText.isBlank()) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6;"));
        if (action != null) {
            btn.setOnAction(e -> action.run());
        }
        // Insert before options and hide buttons
        int idx = Math.max(0, rightButtonsContainer.getChildren().size() - 2);
        rightButtonsContainer.getChildren().add(idx, btn);
        return btn;
    }

    public Button addTab(String name, boolean closable, Runnable onSelect, Runnable onClose) {
        HBox tabBox = new HBox(2);
        tabBox.setAlignment(Pos.CENTER);

        Button tabButton = new Button(name);
        tabButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-padding: 4 6 4 10; -fx-cursor: hand;");

        tabBox.getChildren().add(tabButton);

        if (closable) {
            Button closeBtn = new Button("✕");
            closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 2 4;");
            closeBtn.setTooltip(new Tooltip("Close"));
            closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 2 4; -fx-background-radius: 3;"));
            closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 2 4;"));
            closeBtn.setOnAction(e -> {
                e.consume();
                tabsContainer.getChildren().remove(tabBox);
                tabItems.remove(tabBox);
                if (activeTabItem == tabBox && !tabItems.isEmpty()) {
                    selectTabBox(tabItems.get(0));
                }
                if (onClose != null) {
                    onClose.run();
                }
            });
            tabBox.getChildren().add(closeBtn);
        }

        tabButton.setOnAction(e -> {
            selectTabBox(tabBox);
            if (onSelect != null) {
                onSelect.run();
            }
        });

        tabItems.add(tabBox);
        tabsContainer.getChildren().add(tabBox);

        if (tabItems.size() == 1) {
            selectTabBox(tabBox);
        } else {
            styleTabInactive(tabBox);
        }

        return tabButton;
    }

    public void selectTabBox(HBox target) {
        activeTabItem = target;
        for (HBox item : tabItems) {
            if (item == target) {
                styleTabActive(item);
            } else {
                styleTabInactive(item);
            }
        }
    }

    private void styleTabActive(HBox tabBox) {
        tabBox.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #3574F0; -fx-border-width: 0 0 2 0;");
        if (!tabBox.getChildren().isEmpty() && tabBox.getChildren().get(0) instanceof Button btn) {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-padding: 4 6 4 10; -fx-cursor: hand;");
        }
    }

    private void styleTabInactive(HBox tabBox) {
        tabBox.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        if (!tabBox.getChildren().isEmpty() && tabBox.getChildren().get(0) instanceof Button btn) {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-padding: 4 6 4 10; -fx-cursor: hand;");
        }
    }

    public static Button createToolbarIconButton(String icon, String tooltipText, Runnable action) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3; -fx-min-width: 24px; -fx-pref-width: 24px; -fx-min-height: 24px; -fx-pref-height: 24px; -fx-alignment: center;");
        if (tooltipText != null && !tooltipText.isBlank()) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3; -fx-min-width: 24px; -fx-pref-width: 24px; -fx-min-height: 24px; -fx-pref-height: 24px; -fx-background-radius: 3; -fx-alignment: center;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3; -fx-min-width: 24px; -fx-pref-width: 24px; -fx-min-height: 24px; -fx-pref-height: 24px; -fx-alignment: center;"));
        if (action != null) {
            btn.setOnAction(e -> action.run());
        }
        return btn;
    }

    public void showOptionsMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("git-tool-options-menu");
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-padding: 4 0;");

        // Tool-specific custom menu items
        if (!extraMenuItems.isEmpty()) {
            menu.getItems().addAll(extraMenuItems);
            menu.getItems().add(new SeparatorMenuItem());
        }

        // Show Toolbar
        CheckMenuItem showToolbarItem = new CheckMenuItem("Show Toolbar");
        showToolbarItem.setSelected(showToolbar);
        showToolbarItem.setOnAction(e -> {
            showToolbar = showToolbarItem.isSelected();
            if (onToolbarToggle != null) {
                onToolbarToggle.accept(showToolbar);
            }
        });

        // Group Tabs
        CheckMenuItem groupTabsItem = new CheckMenuItem("Group Tabs");
        groupTabsItem.setSelected(groupTabs);
        groupTabsItem.setOnAction(e -> groupTabs = groupTabsItem.isSelected());

        // View Mode >
        Menu viewModeMenu = new Menu("View Mode");
        String[] viewModes = {"Dock Pinned", "Dock Unpinned", "Undock", "Float", "Window"};
        for (String mode : viewModes) {
            CheckMenuItem mi = new CheckMenuItem(mode);
            mi.setSelected(mode.equals(activeViewMode));
            mi.setOnAction(e -> {
                for (MenuItem itm : viewModeMenu.getItems()) {
                    if (itm instanceof CheckMenuItem cmi) cmi.setSelected(cmi == mi);
                }
                activeViewMode = mode;
            });
            viewModeMenu.getItems().add(mi);
        }

        // Move to >
        Menu moveToMenu = new Menu("Move to");
        String[] positions = {"Bottom", "Left", "Right"};
        for (String pos : positions) {
            CheckMenuItem mi = new CheckMenuItem(pos);
            mi.setSelected(pos.equalsIgnoreCase(activeMoveToPosition));
            mi.setOnAction(e -> {
                for (MenuItem itm : moveToMenu.getItems()) {
                    if (itm instanceof CheckMenuItem cmi) cmi.setSelected(cmi == mi);
                }
                activeMoveToPosition = pos;
                if (onMoveTo != null) {
                    onMoveTo.accept(pos);
                }
            });
            moveToMenu.getItems().add(mi);
        }

        // Resize >
        Menu resizeMenu = new Menu("Resize");
        MenuItem maximizeItem = new MenuItem("Maximize Tool Window");
        maximizeItem.setOnAction(e -> {
            if (onMaximize != null) onMaximize.run();
        });
        MenuItem stretchLeftItem = new MenuItem("Stretch to Left");
        MenuItem stretchRightItem = new MenuItem("Stretch to Right");
        resizeMenu.getItems().addAll(maximizeItem, stretchLeftItem, stretchRightItem);

        // Remove from Sidebar
        MenuItem removeFromSidebarItem = new MenuItem("Remove from Sidebar");
        removeFromSidebarItem.setOnAction(e -> {
            if (onHide != null) onHide.run();
        });

        menu.getItems().addAll(
                showToolbarItem,
                groupTabsItem,
                viewModeMenu,
                moveToMenu,
                resizeMenu,
                new SeparatorMenuItem(),
                removeFromSidebarItem
        );

        menu.show(anchor, javafx.geometry.Side.BOTTOM, -120, 2);
    }
}
