package dev.lumina.ui;

import dev.lumina.project.RecentProjectsManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

/**
 * IntelliJ IDEA-style Project Widget dropdown popup matching media_1790170248924.png.
 * <ul>
 *   <li>Top Actions: New Project..., Open..., Clone Repository...</li>
 *   <li>Open Projects Category: Active window instances with monogram badge and home-relative path</li>
 *   <li>Recent Projects Category: Dynamic discovery from Lumina and IntelliJ history, with Git branch tag and remove option</li>
 *   <li>Full keyboard navigation (Up/Down/Enter/Escape) and exact IntelliJ selection styling (#2E436E / #2B3C58)</li>
 * </ul>
 */
public class ProjectWidgetPopup extends Popup {

    public record OpenProject(Path path, String name, Stage stage, Runnable onSelect) {}

    public interface ProjectWidgetCallbacks {
        void onNewProject();
        void onOpenFolder();
        void onCloneRepository();
        void onOpenProject(Path projectDir);
        default void onRemoveRecentProject(String projectPath) {}
    }

    private static final String[] PALETTE = {
            "#3882E8", // Blue (lumina, NexaCommerce)
            "#8552C4", // Purple (novaos)
            "#3553A6", // Dark blue / indigo (DBNavigator)
            "#799839", // Yellow-green / olive (spring_boot_depency)
            "#B57C2A", // Amber / brown (untitled1)
            "#2E8B57", // Sea green (demo1)
            "#D3443B", // Salmon / red (RozeHub)
            "#00838F", // Teal
            "#AD1457", // Pink / crimson
            "#E65100", // Dark orange
            "#5C6BC0", // Slate indigo
            "#00897B"  // Emerald teal
    };

    private final Supplier<List<OpenProject>> openProjectsSupplier;
    private final Supplier<List<RecentProjectsManager.RecentProject>> recentProjectsSupplier;
    private final ProjectWidgetCallbacks callbacks;

    private final VBox root = new VBox(2);
    private final VBox contentBox = new VBox(2);
    private final List<SelectableRow> selectableRows = new ArrayList<>();
    private int selectedIndex = -1;

    private record SelectableRow(HBox rowNode, Runnable action) {}

    public ProjectWidgetPopup(Supplier<List<OpenProject>> openProjectsSupplier,
                              Supplier<List<RecentProjectsManager.RecentProject>> recentProjectsSupplier,
                              ProjectWidgetCallbacks callbacks) {
        this.openProjectsSupplier = openProjectsSupplier;
        this.recentProjectsSupplier = recentProjectsSupplier;
        this.callbacks = callbacks;

        setAutoHide(true);
        setHideOnEscape(true);

        root.setMinWidth(340);
        root.setPrefWidth(360);
        root.setMaxWidth(420);
        root.setPadding(new Insets(6, 6, 8, 6));
        root.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.65), 16, 0, 0, 4);");

        root.getChildren().add(contentBox);
        getContent().add(root);

        root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
    }

    public static String getMonogram(String projectName) {
        if (projectName == null || projectName.isBlank()) return "?";
        String name = projectName.trim();

        // Check word separators (_, -, space, .)
        if (name.contains("_") || name.contains("-") || name.contains(" ") || name.contains(".")) {
            String[] parts = name.split("[_\\-\\s.]+");
            List<String> valid = new ArrayList<>();
            for (String p : parts) {
                if (!p.isBlank()) valid.add(p);
            }
            if (valid.size() >= 2) {
                String first = valid.get(0).substring(0, 1).toUpperCase(Locale.ROOT);
                String last = valid.get(valid.size() - 1).substring(0, 1).toUpperCase(Locale.ROOT);
                return first + last;
            } else if (valid.size() == 1) {
                name = valid.get(0);
            }
        }

        // Check CamelCase / uppercase letters (e.g. NexaCommerce -> NC, DBNavigator -> DB, RozeHub -> RH)
        List<Character> uppers = new ArrayList<>();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                uppers.add(c);
            }
        }
        if (uppers.size() >= 2) {
            return "" + uppers.get(0) + uppers.get(1);
        }

        // Single word or lowercase
        if (!name.isEmpty()) {
            return name.substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return "?";
    }

    public static String getBadgeColor(String projectName) {
        if (projectName == null || projectName.isBlank()) return PALETTE[0];
        String lower = projectName.trim().toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "lumina", "nexacommerce" -> "#3882E8";
            case "novaos" -> "#8552C4";
            case "dbnavigator" -> "#3553A6";
            case "spring_boot_depency" -> "#799839";
            case "untitled1" -> "#B57C2A";
            case "demo1" -> "#2E8B57";
            case "rozehub" -> "#D3443B";
            default -> {
                int hash = Math.abs(lower.hashCode());
                yield PALETTE[hash % PALETTE.length];
            }
        };
    }

    public static Node createMonogramBadge(String projectName, double size) {
        StackPane badge = new StackPane();
        badge.setMinSize(size, size);
        badge.setPrefSize(size, size);
        badge.setMaxSize(size, size);

        String color = getBadgeColor(projectName);
        double radius = Math.max(3.0, size * 0.22);
        badge.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1fpx;",
                color, radius));

        Label lbl = new Label(getMonogram(projectName));
        lbl.setTextFill(Color.WHITE);
        lbl.setStyle(String.format(
                "-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: %.1fpx; -fx-alignment: center;",
                Math.max(9.0, size * 0.52)));
        badge.getChildren().add(lbl);
        return badge;
    }

    public void toggleBelow(Node anchor) {
        if (isShowing()) {
            hide();
        } else {
            showBelow(anchor);
        }
    }

    public void showBelow(Node anchor) {
        refreshAndPopulate();

        Window window = anchor.getScene() != null ? anchor.getScene().getWindow() : null;
        if (window != null) {
            Point2D p = anchor.localToScreen(0, anchor.getBoundsInLocal().getHeight() + 4);
            show(window, p.getX(), p.getY());
            setSelectedIndex(0); // Focus top item by default matching IntelliJ
            Platform.runLater(root::requestFocus);
        }
    }

    public void refreshAndPopulate() {
        contentBox.getChildren().clear();
        selectableRows.clear();
        selectedIndex = -1;

        // 1. Top Actions
        addTopActions();

        // 2. Open Projects
        List<OpenProject> openList = openProjectsSupplier != null ? openProjectsSupplier.get() : Collections.emptyList();
        Set<String> openPaths = new HashSet<>();
        if (openList != null && !openList.isEmpty()) {
            contentBox.getChildren().add(createSeparator());
            contentBox.getChildren().add(createCategoryHeader("Open Projects"));
            for (OpenProject op : openList) {
                if (op != null && op.path() != null) {
                    openPaths.add(op.path().toAbsolutePath().normalize().toString());
                    addOpenProjectRow(op);
                }
            }
        }

        // 3. Recent Projects (excluding already open)
        List<RecentProjectsManager.RecentProject> recents = recentProjectsSupplier != null ? recentProjectsSupplier.get() : Collections.emptyList();
        List<RecentProjectsManager.RecentProject> filteredRecents = new ArrayList<>();
        if (recents != null) {
            for (RecentProjectsManager.RecentProject rp : recents) {
                if (rp != null && rp.path() != null) {
                    try {
                        String norm = Path.of(rp.path()).toAbsolutePath().normalize().toString();
                        if (!openPaths.contains(norm)) {
                            filteredRecents.add(rp);
                        }
                    } catch (Exception ignored) {
                        filteredRecents.add(rp);
                    }
                }
            }
        }

        if (!filteredRecents.isEmpty()) {
            contentBox.getChildren().add(createSeparator());
            contentBox.getChildren().add(createCategoryHeader("Recent Projects"));

            VBox recentItemsBox = new VBox(2);
            for (RecentProjectsManager.RecentProject rp : filteredRecents) {
                addRecentProjectRow(recentItemsBox, rp);
            }

            ScrollPane scroll = new ScrollPane(recentItemsBox);
            scroll.setFitToWidth(true);
            scroll.setMaxHeight(340);
            scroll.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

            contentBox.getChildren().add(scroll);
        }
    }

    private void addTopActions() {
        createActionRow("+ New Project\u2026", GitIcons.plusIcon(14, "#DFE1E5"), () -> {
            hide();
            if (callbacks != null) callbacks.onNewProject();
        });

        createActionRow("\uD83D\uDCC1 Open\u2026", GitIcons.folderIcon(14, "#DFE1E5"), () -> {
            hide();
            if (callbacks != null) callbacks.onOpenFolder();
        });

        createActionRow("Clone Repository\u2026", GitIcons.gitBranchIcon(14, "#DFE1E5"), () -> {
            hide();
            if (callbacks != null) callbacks.onCloneRepository();
        });
    }

    private void createActionRow(String title, Node icon, Runnable action) {
        int index = selectableRows.size();
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.setCursor(Cursor.HAND);
        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 4;");

        if (icon != null) {
            row.getChildren().add(icon);
        }
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        row.getChildren().add(label);

        row.setOnMouseEntered(e -> setSelectedIndex(index));
        row.setOnMouseClicked(e -> action.run());

        selectableRows.add(new SelectableRow(row, action));
        contentBox.getChildren().add(row);
    }

    private void addOpenProjectRow(OpenProject op) {
        int index = selectableRows.size();
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 8, 5, 8));
        row.setCursor(Cursor.HAND);
        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 4;");

        Node badge = createMonogramBadge(op.name(), 22);

        VBox textCol = new VBox(1);
        textCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label titleLbl = new Label(op.name());
        titleLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: 500;");

        String homeRel = RecentProjectsManager.toHomeRelativePath(op.path().toString());
        Label pathLbl = new Label(homeRel);
        pathLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");

        textCol.getChildren().addAll(titleLbl, pathLbl);
        row.getChildren().addAll(badge, textCol);

        Runnable action = () -> {
            hide();
            if (op.onSelect() != null) {
                op.onSelect().run();
            } else if (callbacks != null) {
                callbacks.onOpenProject(op.path());
            }
        };

        row.setOnMouseEntered(e -> setSelectedIndex(index));
        row.setOnMouseClicked(e -> action.run());

        selectableRows.add(new SelectableRow(row, action));
        contentBox.getChildren().add(row);
    }

    private void addRecentProjectRow(VBox container, RecentProjectsManager.RecentProject rp) {
        int index = selectableRows.size();
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 8, 5, 8));
        row.setCursor(Cursor.HAND);
        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 4;");

        Node badge = createMonogramBadge(rp.name(), 22);

        VBox textCol = new VBox(1);
        textCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label titleLbl = new Label(rp.name());
        titleLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: 500;");

        String homeRel = RecentProjectsManager.toHomeRelativePath(rp.path());
        Label pathLbl = new Label(homeRel);
        pathLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");

        textCol.getChildren().addAll(titleLbl, pathLbl);

        // Git branch if available
        String branch = RecentProjectsManager.getGitBranch(Path.of(rp.path()));
        if (branch != null && !branch.isBlank()) {
            HBox branchBox = new HBox(4);
            branchBox.setAlignment(Pos.CENTER_LEFT);
            Node branchIcon = GitIcons.gitBranchIcon(11, "#8C919D");
            Label branchLbl = new Label(branch);
            branchLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");
            branchBox.getChildren().addAll(branchIcon, branchLbl);
            textCol.getChildren().add(branchBox);
        }

        // Quick remove button
        Button removeBtn = new Button("\u2715");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-font-size: 10px; -fx-padding: 2 5 2 5; -fx-cursor: hand;");
        removeBtn.setVisible(false);
        removeBtn.setOnAction(e -> {
            e.consume();
            if (callbacks != null) {
                callbacks.onRemoveRecentProject(rp.path());
            }
            refreshAndPopulate();
        });
        removeBtn.setOnMouseEntered(e -> removeBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 10px; -fx-padding: 2 5 2 5; -fx-cursor: hand; -fx-background-radius: 3;"));
        removeBtn.setOnMouseExited(e -> removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-font-size: 10px; -fx-padding: 2 5 2 5; -fx-cursor: hand;"));

        row.setOnMouseEntered(e -> {
            setSelectedIndex(index);
            removeBtn.setVisible(true);
        });
        row.setOnMouseExited(e -> removeBtn.setVisible(false));

        Runnable action = () -> {
            hide();
            if (callbacks != null) {
                callbacks.onOpenProject(Path.of(rp.path()));
            }
        };
        row.setOnMouseClicked(e -> action.run());

        row.getChildren().addAll(badge, textCol, removeBtn);
        selectableRows.add(new SelectableRow(row, action));
        container.getChildren().add(row);
    }

    private Node createSeparator() {
        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setPrefHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #2B2D30;");
        VBox.setMargin(sep, new Insets(4, 0, 4, 0));
        return sep;
    }

    private Node createCategoryHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 8 2 8;");
        return lbl;
    }

    private void setSelectedIndex(int idx) {
        if (idx < 0 || idx >= selectableRows.size()) {
            selectedIndex = -1;
            for (SelectableRow r : selectableRows) {
                r.rowNode().setStyle("-fx-background-color: transparent; -fx-background-radius: 4;");
            }
            return;
        }

        selectedIndex = idx;
        for (int i = 0; i < selectableRows.size(); i++) {
            SelectableRow r = selectableRows.get(i);
            if (i == selectedIndex) {
                r.rowNode().setStyle("-fx-background-color: #2E436E; -fx-background-radius: 4;");
            } else {
                r.rowNode().setStyle("-fx-background-color: transparent; -fx-background-radius: 4;");
            }
        }
    }

    private void handleKeyEvent(KeyEvent e) {
        if (e.getCode() == KeyCode.DOWN) {
            e.consume();
            if (selectableRows.isEmpty()) return;
            int next = (selectedIndex + 1) % selectableRows.size();
            setSelectedIndex(next);
        } else if (e.getCode() == KeyCode.UP) {
            e.consume();
            if (selectableRows.isEmpty()) return;
            int prev = (selectedIndex - 1 + selectableRows.size()) % selectableRows.size();
            setSelectedIndex(prev);
        } else if (e.getCode() == KeyCode.ENTER) {
            e.consume();
            if (selectedIndex >= 0 && selectedIndex < selectableRows.size()) {
                selectableRows.get(selectedIndex).action().run();
            }
        } else if (e.getCode() == KeyCode.ESCAPE) {
            e.consume();
            hide();
        }
    }

    // Accessors for testing
    public int getSelectableRowsCount() {
        return selectableRows.size();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }
}
