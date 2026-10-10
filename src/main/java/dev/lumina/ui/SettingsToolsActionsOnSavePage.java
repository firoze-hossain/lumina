package dev.lumina.ui;

import dev.lumina.tools.ActionOnSaveItem;
import dev.lumina.tools.ActionsOnSaveManager;
import dev.lumina.tools.ActionsOnSaveSettings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * Actions on Save settings page in Lumina IDE matching 1:1 design of the reference IDE.
 * Allows enabling/disabling formatting, imports, linters, and external tools on save.
 */
public class SettingsToolsActionsOnSavePage extends VBox {

    private final ActionsOnSaveManager manager;
    private ActionsOnSaveSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private final Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();
    private final Map<String, MenuButton> scopeButtons = new LinkedHashMap<>();
    private final Map<String, MenuButton> modeButtons = new LinkedHashMap<>();
    private final Map<String, MenuButton> profileButtons = new LinkedHashMap<>();
    private final Map<String, VBox> rowBoxes = new LinkedHashMap<>();

    private Consumer<String> onNavigate;
    private Runnable onNavigateToAutosave;

    public SettingsToolsActionsOnSavePage() {
        this(null, null);
    }

    public SettingsToolsActionsOnSavePage(Consumer<String> onNavigate, Runnable onNavigateToAutosave) {
        this.manager = ActionsOnSaveManager.getInstance();
        this.onNavigate = onNavigate;
        this.onNavigateToAutosave = onNavigateToAutosave;
        buildUI();
        loadData();
    }

    public void setOnNavigate(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
    }

    public void setOnNavigateToAutosave(Runnable onNavigateToAutosave) {
        this.onNavigateToAutosave = onNavigateToAutosave;
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Table Header (Action & Activated On)
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 8, 4, 8));

        Label actionHeader = new Label("Action");
        actionHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label activatedHeader = new Label("Activated On");
        activatedHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        headerBox.getChildren().addAll(actionHeader, headerSpacer, activatedHeader);

        // 2. Action Rows Container
        VBox rowsContainer = new VBox(2);
        rowsContainer.setStyle("-fx-background-color: #1E1F22;");

        ActionsOnSaveSettings current = manager.getSettings();
        for (ActionOnSaveItem item : current.getItems()) {
            VBox row = buildActionRow(item);
            rowBoxes.put(item.getId(), row);
            rowsContainer.getChildren().add(row);
        }

        ScrollPane scrollPane = new ScrollPane(rowsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 3. Footer link: Configure autosave options...
        Hyperlink autosaveLink = new Hyperlink("Configure autosave options...");
        autosaveLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 8 0 0 0; -fx-underline: false;");
        autosaveLink.setOnMouseEntered(e -> autosaveLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 8 0 0 0; -fx-underline: true;"));
        autosaveLink.setOnMouseExited(e -> autosaveLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 8 0 0 0; -fx-underline: false;"));
        autosaveLink.setOnAction(e -> {
            if (onNavigateToAutosave != null) {
                onNavigateToAutosave.run();
            }
        });

        getChildren().addAll(headerBox, scrollPane, autosaveLink);
    }

    private VBox buildActionRow(ActionOnSaveItem item) {
        VBox rowBox = new VBox(2);
        rowBox.setPadding(new Insets(5, 8, 5, 8));
        rowBox.setStyle("-fx-background-color: transparent; -fx-background-radius: 4px;");

        // Click to focus/select row styling
        rowBox.setOnMouseClicked(e -> {
            for (VBox r : rowBoxes.values()) {
                r.setStyle("-fx-background-color: transparent; -fx-background-radius: 4px;");
            }
            rowBox.setStyle("-fx-background-color: #2B2D30; -fx-background-radius: 4px;");
        });

        // Top line: CheckBox + Dropdowns/Configure links + Activated On
        HBox topLine = new HBox(8);
        topLine.setAlignment(Pos.CENTER_LEFT);

        CheckBox cb = new CheckBox(item.getTitle());
        cb.setSelected(item.isEnabled());
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        checkBoxes.put(item.getId(), cb);
        topLine.getChildren().add(cb);

        // Scope options (e.g. Files: Go files ⌵)
        if (!item.getScopeOptions().isEmpty()) {
            MenuButton scopeBtn = createInlineDropdown(
                    item.getSelectedScope() != null ? "Files: " + item.getSelectedScope() : "Files: " + item.getScopeOptions().get(0),
                    item.getScopeOptions(),
                    selected -> {
                        scopeButtons.get(item.getId()).setText("Files: " + selected);
                        notifyModified();
                    }
            );
            scopeButtons.put(item.getId(), scopeBtn);
            topLine.getChildren().add(scopeBtn);
        }

        // Mode options (e.g. Whole file ⌵)
        if (!item.getModeOptions().isEmpty()) {
            MenuButton modeBtn = createInlineDropdown(
                    item.getSelectedMode() != null ? item.getSelectedMode() : item.getModeOptions().get(0),
                    item.getModeOptions(),
                    selected -> {
                        modeButtons.get(item.getId()).setText(selected);
                        notifyModified();
                    }
            );
            modeButtons.put(item.getId(), modeBtn);
            topLine.getChildren().add(modeBtn);
        }

        // Profile options (e.g. Project Profile ⌵)
        if (!item.getProfileOptions().isEmpty()) {
            MenuButton profileBtn = createInlineDropdown(
                    item.getSelectedProfile() != null ? item.getSelectedProfile() : item.getProfileOptions().get(0),
                    item.getProfileOptions(),
                    selected -> {
                        profileButtons.get(item.getId()).setText(selected);
                        notifyModified();
                    }
            );
            profileButtons.put(item.getId(), profileBtn);
            topLine.getChildren().add(profileBtn);
        }

        // Configure link (e.g. Configure... linking to Black)
        if (item.getConfigureTarget() != null) {
            Hyperlink configLink = new Hyperlink("Configure...");
            configLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;");
            configLink.setOnMouseEntered(e -> configLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: true;"));
            configLink.setOnMouseExited(e -> configLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;"));
            configLink.setOnAction(e -> {
                if (onNavigate != null) {
                    onNavigate.accept(item.getConfigureTarget());
                }
            });
            topLine.getChildren().add(configLink);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topLine.getChildren().add(spacer);

        // Activated On label
        Label activatedLabel = new Label(item.getActivatedOn());
        activatedLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        topLine.getChildren().add(activatedLabel);

        rowBox.getChildren().add(topLine);

        // Subtext / Warning row (if any)
        if (item.getSubtext() != null && !item.getSubtext().isBlank()) {
            HBox subBox = new HBox(6);
            subBox.setAlignment(Pos.CENTER_LEFT);
            subBox.setPadding(new Insets(0, 0, 0, 22));

            if (item.isWarning()) {
                SVGPath warnIcon = new SVGPath();
                warnIcon.setContent("M 6 1 L 11 11 L 1 11 Z M 6 4 L 6 7 M 6 9 L 6 9.5");
                warnIcon.setFill(javafx.scene.paint.Color.web("#E0AC38"));
                warnIcon.setStroke(javafx.scene.paint.Color.web("#E0AC38"));
                warnIcon.setStrokeWidth(0.5);
                subBox.getChildren().add(warnIcon);
            }

            Label subLabel = new Label(item.getSubtext());
            subLabel.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");
            subBox.getChildren().add(subLabel);

            rowBox.getChildren().add(subBox);
        }

        return rowBox;
    }

    private MenuButton createInlineDropdown(String initialText, List<String> options, Consumer<String> onSelect) {
        MenuButton mb = new MenuButton(initialText);
        mb.setStyle("-fx-background-color: transparent; -fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0 4 0 4; -fx-border-color: transparent;");
        for (String opt : options) {
            MenuItem mi = new MenuItem(opt);
            mi.setOnAction(e -> onSelect.accept(opt));
            mb.getItems().add(mi);
        }
        return mb;
    }

    private void loadData() {
        updating = true;
        manager.refreshDynamicStatuses();
        ActionsOnSaveSettings settings = manager.getSettings();
        for (ActionOnSaveItem item : settings.getItems()) {
            CheckBox cb = checkBoxes.get(item.getId());
            if (cb != null) {
                cb.setSelected(item.isEnabled());
            }
            MenuButton scopeBtn = scopeButtons.get(item.getId());
            if (scopeBtn != null && item.getSelectedScope() != null) {
                scopeBtn.setText("Files: " + item.getSelectedScope());
            }
            MenuButton modeBtn = modeButtons.get(item.getId());
            if (modeBtn != null && item.getSelectedMode() != null) {
                modeBtn.setText(item.getSelectedMode());
            }
            MenuButton profileBtn = profileButtons.get(item.getId());
            if (profileBtn != null && item.getSelectedProfile() != null) {
                profileBtn.setText(item.getSelectedProfile());
            }
        }
        initialSettings = getCurrentSettingsFromUI();
        updating = false;
    }

    public ActionsOnSaveSettings getCurrentSettingsFromUI() {
        ActionsOnSaveSettings s = manager.getSettings();
        for (ActionOnSaveItem item : s.getItems()) {
            CheckBox cb = checkBoxes.get(item.getId());
            if (cb != null) {
                item.setEnabled(cb.isSelected());
            }
            MenuButton scopeBtn = scopeButtons.get(item.getId());
            if (scopeBtn != null) {
                String txt = scopeBtn.getText();
                if (txt.startsWith("Files: ")) {
                    item.setSelectedScope(txt.substring(7));
                }
            }
            MenuButton modeBtn = modeButtons.get(item.getId());
            if (modeBtn != null) {
                item.setSelectedMode(modeBtn.getText());
            }
            MenuButton profileBtn = profileButtons.get(item.getId());
            if (profileBtn != null) {
                item.setSelectedProfile(profileBtn.getText());
            }
        }
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        ActionsOnSaveSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public Map<String, CheckBox> getCheckBoxes() {
        return checkBoxes;
    }
}
