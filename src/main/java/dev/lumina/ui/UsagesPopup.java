package dev.lumina.ui;

import dev.lumina.semantics.SemanticEngine;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.stage.Screen;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * IntelliJ IDEA-style In-Editor Floating "Show Usages" popup:
 * - Floating card directly below the clicked symbol
 * - Rich header with declaration kind, signature, container class, and usage count
 * - Filter/scope toolbar ("Project Files ▾", "Usages or usages of base method")
 * - Highlighting row list with class badge (C), bold filename, cyan line number, and code snippet with highlighted matching symbol
 * - Footer showing relative file path of the selected item
 * - Navigation: single click / Enter jumps to file & line; Esc dismisses
 */
public class UsagesPopup {

    public record UsageItem(Path file, int line, String preview, boolean declaration, String relPath) {
    }

    private final Popup popup = new Popup();
    private final VBox root = new VBox();
    private final HBox headerBox = new HBox(8);
    private final Label titleLabel = new Label();
    private final Label countLabel = new Label();
    private final Label popoutButton = new Label("⧉");

    private final HBox toolbar = new HBox(8);
    private final Label scopeLabel = new Label("Project Files ▾");
    private final Label scopeDescLabel = new Label("Usages or usages of base method");
    private final Label settingsButton = new Label("⚙");

    private final ObservableList<UsageItem> items = FXCollections.observableArrayList();
    private final ListView<UsageItem> listView = new ListView<>(items);
    private final Label footerPathLabel = new Label();
    private final BiConsumer<Path, Integer> onOpenAt;
    private Runnable onPopout;
    private String symbol;
    private Path projectRoot;

    public UsagesPopup(BiConsumer<Path, Integer> onOpenAt) {
        this.onOpenAt = onOpenAt;

        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        root.getStyleClass().add("usages-popup");
        root.setPrefWidth(680);
        root.setMaxWidth(760);

        // 1. Header: [Title] [Count] ... [⧉]
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getStyleClass().add("usages-header");

        titleLabel.getStyleClass().add("usages-header-title");
        countLabel.getStyleClass().add("usages-header-count");
        popoutButton.getStyleClass().add("usages-action-icon");
        popoutButton.setOnMouseClicked(e -> {
            hide();
            if (onPopout != null) onPopout.run();
        });

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(titleLabel, countLabel, headerSpacer, popoutButton);

        // 2. Toolbar
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("usages-toolbar");

        HBox toolIcons = new HBox(4);
        toolIcons.setAlignment(Pos.CENTER_LEFT);
        String[] icons = {"⤢", "⤓", "⇄", "i", "//", "@"};
        for (String ic : icons) {
            Label btn = new Label(ic);
            btn.getStyleClass().add("usages-tool-btn");
            toolIcons.getChildren().add(btn);
        }

        Region toolSpacer = new Region();
        HBox.setHgrow(toolSpacer, Priority.ALWAYS);

        scopeLabel.getStyleClass().add("usages-scope-label");
        scopeDescLabel.getStyleClass().add("usages-scope-desc");
        settingsButton.getStyleClass().add("usages-action-icon");

        toolbar.getChildren().addAll(toolIcons, toolSpacer, scopeLabel, scopeDescLabel, settingsButton);

        // 3. ListView
        listView.getStyleClass().add("usages-list");
        listView.setPrefHeight(240);
        listView.setMaxHeight(320);
        listView.setCellFactory(lv -> new UsageCell());

        listView.setOnMouseClicked(e -> {
            UsageItem sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                hide();
                onOpenAt.accept(sel.file(), sel.line());
            }
        });

        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                UsageItem sel = listView.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    hide();
                    onOpenAt.accept(sel.file(), sel.line());
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hide();
                e.consume();
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, cur) -> {
            if (cur != null) {
                footerPathLabel.setText(cur.relPath() != null ? cur.relPath() : cur.file().toString());
            }
        });

        // 4. Footer
        HBox footer = new HBox();
        footer.getStyleClass().add("usages-footer");
        footerPathLabel.getStyleClass().add("usages-footer-path");
        footer.getChildren().add(footerPathLabel);

        root.getChildren().addAll(headerBox, toolbar, listView, footer);
        popup.getContent().add(root);
    }

    public void setOnPopout(Runnable onPopout) {
        this.onPopout = onPopout;
    }

    public void hide() {
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public void show(Node owner, Path rootDir, String symbol, String headerTitle,
                     List<UsageItem> usageList, Bounds screenBounds) {
        if (usageList == null || usageList.isEmpty() || owner == null || screenBounds == null) return;
        this.projectRoot = rootDir;
        this.symbol = symbol;

        titleLabel.setText(headerTitle != null ? headerTitle : ("Usages of " + symbol));
        int count = usageList.size();
        countLabel.setText(count + " " + (count == 1 ? "usage" : "usages"));

        items.setAll(usageList);
        listView.getSelectionModel().select(0);
        listView.scrollTo(0);

        UsageItem first = usageList.get(0);
        footerPathLabel.setText(first.relPath() != null ? first.relPath() : first.file().toString());

        double itemHeight = 28;
        double listHeight = Math.min(Math.max(usageList.size() * itemHeight, 80), 300);
        listView.setPrefHeight(listHeight);

        // Position calculation
        double targetX = screenBounds.getMinX() - 30;
        double targetY = screenBounds.getMaxY() + 4;

        javafx.collections.ObservableList<Screen> screens = Screen.getScreensForRectangle(
                screenBounds.getMinX(), screenBounds.getMinY(),
                screenBounds.getWidth(), screenBounds.getHeight());
        Rectangle2D screen = screens.isEmpty()
                ? Screen.getPrimary().getVisualBounds()
                : screens.get(0).getVisualBounds();

        double prefW = 680;
        double totalH = listHeight + 110;
        if (targetX + prefW > screen.getMaxX() - 10) {
            targetX = Math.max(screen.getMinX() + 10, screen.getMaxX() - prefW - 10);
        }
        if (targetX < screen.getMinX() + 10) {
            targetX = screen.getMinX() + 10;
        }
        if (targetY + totalH > screen.getMaxY() - 10) {
            targetY = Math.max(screen.getMinY() + 10, screenBounds.getMinY() - totalH - 6);
        }

        popup.show(owner, targetX, targetY);
        listView.requestFocus();
    }

    private class UsageCell extends ListCell<UsageItem> {
        private final HBox cellBox = new HBox(8);
        private final Label iconBadge = new Label("C");
        private final Label fileLabel = new Label();
        private final Label lineLabel = new Label();
        private final TextFlow snippetFlow = new TextFlow();

        public UsageCell() {
            cellBox.setAlignment(Pos.CENTER_LEFT);
            cellBox.getStyleClass().add("usages-row");

            iconBadge.getStyleClass().add("usages-class-badge");
            fileLabel.getStyleClass().add("usages-filename");
            fileLabel.setMinWidth(140);
            fileLabel.setPrefWidth(160);

            lineLabel.getStyleClass().add("usages-line-number");
            lineLabel.setMinWidth(45);
            lineLabel.setPrefWidth(45);
            lineLabel.setAlignment(Pos.CENTER_RIGHT);

            HBox.setHgrow(snippetFlow, Priority.ALWAYS);
            cellBox.getChildren().addAll(iconBadge, fileLabel, lineLabel, snippetFlow);
        }

        @Override
        protected void updateItem(UsageItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            fileLabel.setText(item.file().getFileName().toString());
            lineLabel.setText(String.valueOf(item.line()));

            snippetFlow.getChildren().clear();
            String preview = item.preview() != null ? item.preview().stripLeading() : "";
            if (symbol != null && !symbol.isBlank() && preview.contains(symbol)) {
                int idx = preview.indexOf(symbol);
                if (idx > 0) {
                    Text before = new Text(preview.substring(0, idx));
                    before.getStyleClass().add("usages-snippet-text");
                    snippetFlow.getChildren().add(before);
                }
                Text match = new Text(symbol);
                match.getStyleClass().add("usages-snippet-match");
                snippetFlow.getChildren().add(match);

                int afterIdx = idx + symbol.length();
                if (afterIdx < preview.length()) {
                    Text after = new Text(preview.substring(afterIdx));
                    after.getStyleClass().add("usages-snippet-text");
                    snippetFlow.getChildren().add(after);
                }
            } else {
                Text txt = new Text(preview);
                txt.getStyleClass().add("usages-snippet-text");
                snippetFlow.getChildren().add(txt);
            }

            setGraphic(cellBox);
            setText(null);
        }
    }
}
