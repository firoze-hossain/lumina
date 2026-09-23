package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ IDEA-style "VCS Operations" popup (Alt+` / Git > VCS Operations Popup...).
 * Matches exact visual design and behavior of media_1790168152014.png:
 * - Centered "VCS Operations" header with "Git" category tag.
 * - Dynamic action resolution from CustomActionsSchema ("VcsOperationsPopup" group).
 * - Context-aware file history ("Show History for <activeFile>").
 * - Numbered accelerator mnemonics (1..9, 0) allowing direct keypress execution.
 * - Global shortcut hints (Ctrl+K, Ctrl+Alt+Z, Ctrl+D, Ctrl+Shift+`, Ctrl+Shift+K).
 * - Blue row hover and arrow key selection (#2B3C58).
 */
public class VcsOperationsPopup extends Popup {

    public interface VcsContext {
        Path getProjectRoot();
        Path getActiveFile();
        String getActiveBranch();
    }

    public interface VcsCallbacks {
        void onCommit();
        default void onCommitSelected() {}
        void onRollback();
        void onShowHistory(Path file);
        default void onAnnotate(Path file) {}
        void onShowDiff(Path file);
        void onBranches();
        void onPush();
        void onStash();
        void onUnstash();
        void onCopyBranchName(String branch);
        default void onShowLocalHistory(Path file) {}
    }

    public static final class VcsItem {
        private final String id;
        private final String mnemonic;
        private final String text;
        private final String shortcut;
        private final Node icon;
        private final boolean isSeparator;
        private final boolean enabled;
        private final Runnable action;

        public VcsItem(String id, String mnemonic, String text, String shortcut, Node icon,
                       boolean isSeparator, boolean enabled, Runnable action) {
            this.id = id;
            this.mnemonic = mnemonic;
            this.text = text;
            this.shortcut = shortcut;
            this.icon = icon;
            this.isSeparator = isSeparator;
            this.enabled = enabled;
            this.action = action;
        }

        public static VcsItem separator() {
            return new VcsItem(null, null, null, null, null, true, false, null);
        }

        public String getId() { return id; }
        public String getMnemonic() { return mnemonic; }
        public String getText() { return text; }
        public String getShortcut() { return shortcut; }
        public Node getIcon() { return icon; }
        public boolean isSeparator() { return isSeparator; }
        public boolean isEnabled() { return enabled; }
        public Runnable getAction() { return action; }
    }

    private final VcsContext context;
    private final VcsCallbacks callbacks;

    private final VBox root = new VBox(2);
    private final VBox itemsBox = new VBox(1);
    private final List<VcsItem> currentItems = new ArrayList<>();
    private final List<HBox> itemRows = new ArrayList<>();
    private int selectedIndex = -1;

    public VcsOperationsPopup(VcsContext context, VcsCallbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;

        setAutoHide(true);
        setHideOnEscape(true);

        root.setPrefWidth(370);
        root.setMinWidth(350);
        root.setMaxWidth(400);
        root.setPadding(new Insets(6, 6, 8, 6));
        root.setStyle(
                "-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.65), 16, 0, 0, 6);"
        );

        // 1. Header: centered title
        StackPane headerPane = new StackPane();
        headerPane.setAlignment(Pos.CENTER);
        headerPane.setPadding(new Insets(4, 8, 4, 8));

        Label titleLbl = new Label("VCS Operations");
        titleLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        headerPane.getChildren().add(titleLbl);

        // 2. Category tag: "Git"
        Label catLbl = new Label("Git");
        catLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10 2 10;");

        root.getChildren().addAll(headerPane, catLbl, itemsBox);
        getContent().add(root);

        // Keyboard handler
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    public List<VcsItem> getCurrentItems() {
        return List.copyOf(currentItems);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Builds and updates the action items dynamically from CustomActionsSchema and runtime context.
     */
    public void refreshItems() {
        itemsBox.getChildren().clear();
        currentItems.clear();
        itemRows.clear();
        selectedIndex = -1;

        Path rootPath = context != null ? context.getProjectRoot() : null;
        boolean inRepo = rootPath != null && GitService.isRepository(rootPath);
        Path activeFile = context != null ? context.getActiveFile() : null;
        String activeBranch = context != null ? context.getActiveBranch() : "master";

        // Query configurable group from CustomActionsSchema
        CustomActionItem group = CustomActionsSchema.getInstance().getGroup("VcsOperationsPopup");
        List<CustomActionItem> schemaItems = group != null ? group.getChildren() : List.of();

        if (schemaItems.isEmpty()) {
            // Fallback default list if schema group is unpopulated
            buildDefaultItems(inRepo, activeFile, activeBranch);
        } else {
            int mnemonicCounter = 1;
            for (CustomActionItem ci : schemaItems) {
                if (ci.isSeparator()) {
                    currentItems.add(VcsItem.separator());
                } else {
                    String id = ci.getId();
                    VcsItem item = resolveSchemaAction(id, ci.getText(), mnemonicCounter, inRepo, activeFile, activeBranch);
                    if (item.getMnemonic() != null) {
                        mnemonicCounter = (mnemonicCounter + 1) % 10;
                    }
                    currentItems.add(item);
                }
            }
        }

        // Render UI rows
        int visualRowIndex = 0;
        for (int i = 0; i < currentItems.size(); i++) {
            VcsItem item = currentItems.get(i);
            if (item.isSeparator()) {
                Separator sep = new Separator();
                sep.setStyle("-fx-background-color: transparent; -fx-border-color: #393B40 transparent transparent transparent; -fx-padding: 3 4;");
                itemsBox.getChildren().add(sep);
                itemRows.add(null);
            } else {
                final int index = i;
                HBox row = buildItemRow(item);
                row.setOnMouseEntered(e -> selectIndex(index));
                row.setOnMouseClicked(e -> {
                    if (item.isEnabled()) {
                        executeItem(item);
                    }
                });
                itemsBox.getChildren().add(row);
                itemRows.add(row);
                if (selectedIndex == -1 && item.isEnabled()) {
                    selectedIndex = index;
                }
                visualRowIndex++;
            }
        }

        // Highlight initial item
        updateRowHighlights();
    }

    private void buildDefaultItems(boolean inRepo, Path activeFile, String activeBranch) {
        String fileName = activeFile != null ? activeFile.getFileName().toString() : null;

        currentItems.add(new VcsItem("CheckinProject", "1", "Commit...", "Ctrl+K", GitIcons.commitIcon(), false, inRepo,
                () -> { if (callbacks != null) callbacks.onCommit(); }));
        currentItems.add(new VcsItem("CheckinFiles", "2", "Commit...", null, null, false, false,
                () -> { if (callbacks != null) callbacks.onCommitSelected(); }));
        currentItems.add(new VcsItem("ChangesView.Revert", "3", "Rollback...", "Ctrl+Alt+Z", GitIcons.rollbackIcon(), false, inRepo,
                () -> { if (callbacks != null) callbacks.onRollback(); }));

        currentItems.add(VcsItem.separator());

        String histLabel = fileName != null ? "Show History for " + fileName : "Show History";
        currentItems.add(new VcsItem("Vcs.ShowTabbedFileHistory", "4", histLabel, null, GitIcons.clockIcon(), false, inRepo,
                () -> { if (callbacks != null) callbacks.onShowHistory(activeFile); }));
        currentItems.add(new VcsItem("Annotate", "5", "Annotate", null, null, false, false,
                () -> { if (callbacks != null) callbacks.onAnnotate(activeFile); }));
        currentItems.add(new VcsItem("Diff.ShowDiff", "6", "Show Diff", "Ctrl+D", GitIcons.diffIcon(), false, inRepo && activeFile != null,
                () -> { if (callbacks != null) callbacks.onShowDiff(activeFile); }));

        currentItems.add(VcsItem.separator());

        currentItems.add(new VcsItem("Git.Branches", "7", "Branches...", "Ctrl+Shift+`", GitIcons.gitBranchIcon(), false, inRepo,
                () -> { if (callbacks != null) callbacks.onBranches(); }));
        currentItems.add(new VcsItem("Vcs.Push", "8", "Push...", "Ctrl+Shift+K", GitIcons.pushIcon(), false, inRepo,
                () -> { if (callbacks != null) callbacks.onPush(); }));
        currentItems.add(new VcsItem("Git.Stash", "9", "Stash Changes...", null, null, false, inRepo,
                () -> { if (callbacks != null) callbacks.onStash(); }));
        currentItems.add(new VcsItem("Git.Unstash", "0", "Unstash Changes...", null, null, false, inRepo,
                () -> { if (callbacks != null) callbacks.onUnstash(); }));
        currentItems.add(new VcsItem("Git.CopyBranchName", null, "Copy Branch Name", null, GitIcons.copyIcon(), false, inRepo && activeBranch != null && !activeBranch.isBlank(),
                () -> { if (callbacks != null) callbacks.onCopyBranchName(activeBranch); }));

        currentItems.add(VcsItem.separator());

        currentItems.add(new VcsItem("LocalHistory.ShowHistory", null, "Show Local History...", null, null, false, false,
                () -> { if (callbacks != null) callbacks.onShowLocalHistory(activeFile); }));
    }

    private VcsItem resolveSchemaAction(String id, String text, int mnemonicNum, boolean inRepo, Path activeFile, String activeBranch) {
        String fileName = activeFile != null ? activeFile.getFileName().toString() : null;

        return switch (id) {
            case "CheckinProject" -> new VcsItem(id, String.valueOf(mnemonicNum), "Commit...", "Ctrl+K", GitIcons.commitIcon(), false, inRepo,
                    () -> { if (callbacks != null) callbacks.onCommit(); });
            case "CheckinFiles" -> new VcsItem(id, String.valueOf(mnemonicNum), "Commit...", null, null, false, false,
                    () -> { if (callbacks != null) callbacks.onCommitSelected(); });
            case "ChangesView.Revert" -> new VcsItem(id, String.valueOf(mnemonicNum), "Rollback...", "Ctrl+Alt+Z", GitIcons.rollbackIcon(), false, inRepo,
                    () -> { if (callbacks != null) callbacks.onRollback(); });
            case "Vcs.ShowTabbedFileHistory" -> {
                String lbl = fileName != null ? "Show History for " + fileName : "Show History";
                yield new VcsItem(id, String.valueOf(mnemonicNum), lbl, null, GitIcons.clockIcon(), false, inRepo,
                        () -> { if (callbacks != null) callbacks.onShowHistory(activeFile); });
            }
            case "Annotate" -> new VcsItem(id, String.valueOf(mnemonicNum), "Annotate", null, null, false, false,
                    () -> { if (callbacks != null) callbacks.onAnnotate(activeFile); });
            case "Diff.ShowDiff" -> new VcsItem(id, String.valueOf(mnemonicNum), "Show Diff", "Ctrl+D", GitIcons.diffIcon(), false, inRepo && activeFile != null,
                    () -> { if (callbacks != null) callbacks.onShowDiff(activeFile); });
            case "Git.Branches" -> new VcsItem(id, String.valueOf(mnemonicNum), "Branches...", "Ctrl+Shift+`", GitIcons.gitBranchIcon(), false, inRepo,
                    () -> { if (callbacks != null) callbacks.onBranches(); });
            case "Vcs.Push" -> new VcsItem(id, String.valueOf(mnemonicNum), "Push...", "Ctrl+Shift+K", GitIcons.pushIcon(), false, inRepo,
                    () -> { if (callbacks != null) callbacks.onPush(); });
            case "Git.Stash" -> new VcsItem(id, String.valueOf(mnemonicNum), "Stash Changes...", null, null, false, inRepo,
                    () -> { if (callbacks != null) callbacks.onStash(); });
            case "Git.Unstash" -> new VcsItem(id, String.valueOf(mnemonicNum), "Unstash Changes...", null, null, false, inRepo,
                    () -> { if (callbacks != null) callbacks.onUnstash(); });
            case "Git.CopyBranchName" -> new VcsItem(id, null, "Copy Branch Name", null, GitIcons.copyIcon(), false, inRepo && activeBranch != null && !activeBranch.isBlank(),
                    () -> { if (callbacks != null) callbacks.onCopyBranchName(activeBranch); });
            case "LocalHistory.ShowHistory" -> new VcsItem(id, null, "Show Local History...", null, null, false, false,
                    () -> { if (callbacks != null) callbacks.onShowLocalHistory(activeFile); });
            default -> new VcsItem(id, null, text != null ? text : id, null, null, false, inRepo, () -> {});
        };
    }

    private HBox buildItemRow(VcsItem item) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 8, 3, 8));
        row.setStyle("-fx-background-radius: 4; -fx-cursor: hand;");

        // 1. Icon container (width 16)
        StackPane iconPane = new StackPane();
        iconPane.setMinSize(16, 16);
        iconPane.setPrefSize(16, 16);
        iconPane.setMaxSize(16, 16);
        iconPane.setAlignment(Pos.CENTER);
        if (item.getIcon() != null) {
            iconPane.getChildren().add(item.getIcon());
        }

        // 2. Mnemonic accelerator number (width 12)
        Label mnemonicLbl = new Label(item.getMnemonic() != null ? item.getMnemonic() : "");
        mnemonicLbl.setPrefWidth(12);
        mnemonicLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11.5px;");

        // 3. Action title
        Label textLbl = new Label(item.getText() != null ? item.getText() : "");
        textLbl.setStyle(item.isEnabled()
                ? "-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px;"
                : "-fx-text-fill: #5A5D63; -fx-font-size: 12.5px;");

        // 4. Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 5. Shortcut text
        Label shortcutLbl = new Label(item.getShortcut() != null ? item.getShortcut() : "");
        shortcutLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");

        row.getChildren().addAll(iconPane, mnemonicLbl, textLbl, spacer, shortcutLbl);
        return row;
    }

    public void selectIndex(int index) {
        if (index < 0 || index >= currentItems.size()) return;
        VcsItem item = currentItems.get(index);
        if (item.isSeparator() || !item.isEnabled()) return;
        this.selectedIndex = index;
        updateRowHighlights();
    }

    private void updateRowHighlights() {
        for (int i = 0; i < itemRows.size(); i++) {
            HBox row = itemRows.get(i);
            if (row == null) continue;
            VcsItem item = currentItems.get(i);

            Label mnemonicLbl = (Label) row.getChildren().get(1);
            Label textLbl = (Label) row.getChildren().get(2);
            Label shortcutLbl = (Label) row.getChildren().get(4);

            if (i == selectedIndex) {
                row.setStyle("-fx-background-color: #2B3C58; -fx-background-radius: 4; -fx-cursor: hand;");
                textLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12.5px; -fx-font-weight: normal;");
                mnemonicLbl.setStyle("-fx-text-fill: #B0B7C6; -fx-font-size: 11.5px;");
                shortcutLbl.setStyle("-fx-text-fill: #B0B7C6; -fx-font-size: 11px;");
            } else {
                row.setStyle("-fx-background-color: transparent; -fx-background-radius: 4; -fx-cursor: hand;");
                textLbl.setStyle(item.isEnabled()
                        ? "-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px;"
                        : "-fx-text-fill: #5A5D63; -fx-font-size: 12.5px;");
                mnemonicLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11.5px;");
                shortcutLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");
            }
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();

        if (code == KeyCode.ESCAPE) {
            hide();
            event.consume();
            return;
        }

        if (code == KeyCode.DOWN) {
            moveSelection(1);
            event.consume();
            return;
        }

        if (code == KeyCode.UP) {
            moveSelection(-1);
            event.consume();
            return;
        }

        if (code == KeyCode.ENTER) {
            if (selectedIndex >= 0 && selectedIndex < currentItems.size()) {
                VcsItem item = currentItems.get(selectedIndex);
                if (item.isEnabled()) {
                    executeItem(item);
                }
            }
            event.consume();
            return;
        }

        // Direct accelerator numbers: '1'..'9', '0'
        String keyChar = event.getText();
        if (keyChar != null && !keyChar.isEmpty() && Character.isDigit(keyChar.charAt(0))) {
            String digitStr = keyChar.substring(0, 1);
            for (VcsItem item : currentItems) {
                if (digitStr.equals(item.getMnemonic())) {
                    if (item.isEnabled()) {
                        executeItem(item);
                    }
                    event.consume();
                    return;
                }
            }
        }
    }

    private void moveSelection(int step) {
        if (currentItems.isEmpty()) return;
        int n = currentItems.size();
        int cur = selectedIndex;
        for (int i = 0; i < n; i++) {
            cur = (cur + step + n) % n;
            VcsItem item = currentItems.get(cur);
            if (!item.isSeparator() && item.isEnabled()) {
                selectIndex(cur);
                break;
            }
        }
    }

    private void executeItem(VcsItem item) {
        hide();
        if (item.getAction() != null) {
            Platform.runLater(item.getAction());
        }
    }

    /**
     * Shows the VCS Operations popup centered in the provided IDE stage.
     */
    public void showCentered(Stage stage) {
        if (stage == null || !stage.isShowing()) return;
        refreshItems();

        double x = stage.getX() + (stage.getWidth() - 370) / 2.0;
        double y = stage.getY() + (stage.getHeight() - 420) / 2.0;
        show(stage, Math.max(stage.getX() + 10, x), Math.max(stage.getY() + 30, y));

        Platform.runLater(root::requestFocus);
    }
}
