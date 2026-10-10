package dev.lumina.ui;

import dev.lumina.advanced.AdvancedSettingItem;
import dev.lumina.advanced.AdvancedSettingsManager;
import dev.lumina.advanced.AdvancedSettingType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Settings page for "Advanced Settings", providing dynamic configuration,
 * real-time search filtering, "Show modified only" toggling, and snapshotting.
 */
public class SettingsAdvancedPage extends VBox {
    private final AdvancedSettingsManager manager;
    private final TextField searchField;
    private final CheckBox showModifiedOnlyCheckBox;
    private final VBox sectionsContainer;

    private final Map<String, Object> initialSnapshot = new HashMap<>();
    private final Map<String, Node> itemControls = new HashMap<>();
    private final Map<String, VBox> itemRowBoxes = new HashMap<>();
    private final Map<String, VBox> groupSectionBoxes = new HashMap<>();

    private Runnable onModifiedListener;

    public SettingsAdvancedPage() {
        this(AdvancedSettingsManager.getInstance());
    }

    public SettingsAdvancedPage(AdvancedSettingsManager manager) {
        this.manager = manager;
        setSpacing(14);
        setPadding(new Insets(16, 24, 24, 24));
        setStyle("-fx-background-color: #1E1F22;");

        // Top navigation row with title and navigation arrows
        HBox topHeader = new HBox(12);
        topHeader.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("Advanced Settings");
        pageTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 16px; -fx-font-weight: bold;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button backButton = new Button("←");
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: hand;");
        Button forwardButton = new Button("→");
        forwardButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: hand;");

        topHeader.getChildren().addAll(pageTitle, topSpacer, backButton, forwardButton);

        // Search bar and "Show modified only" row
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        HBox searchBox = new HBox(6);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 8 3 8;");
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        searchField = new TextField();
        searchField.setPromptText("Search advanced settings");
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 2 0 2 0;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, searchField);

        showModifiedOnlyCheckBox = new CheckBox("Show modified only");
        showModifiedOnlyCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        searchRow.getChildren().addAll(searchBox, showModifiedOnlyCheckBox);

        // Main content container for sections
        sectionsContainer = new VBox(18);
        sectionsContainer.setStyle("-fx-background-color: #1E1F22;");

        getChildren().addAll(topHeader, searchRow, sectionsContainer);

        buildSections();
        captureInitialSnapshot();

        // Search & filter listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        showModifiedOnlyCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void buildSections() {
        sectionsContainer.getChildren().clear();
        groupSectionBoxes.clear();
        itemRowBoxes.clear();
        itemControls.clear();

        List<String> groups = manager.getGroups();
        for (String group : groups) {
            VBox groupBox = new VBox(10);
            groupBox.setStyle("-fx-background-color: #1E1F22;");

            // Section title with horizontal divider line
            HBox titleRow = new HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label titleLabel = new Label(group);
            titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

            Region line = new Region();
            line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1; -fx-max-height: 1;");
            HBox.setHgrow(line, Priority.ALWAYS);

            titleRow.getChildren().addAll(titleLabel, line);
            groupBox.getChildren().add(titleRow);

            VBox itemsBox = new VBox(10);
            itemsBox.setPadding(new Insets(2, 0, 4, 16));

            List<AdvancedSettingItem> items = manager.getItemsByGroup(group);
            for (AdvancedSettingItem item : items) {
                VBox rowBox = createItemRow(item);
                itemRowBoxes.put(item.getId(), rowBox);
                itemsBox.getChildren().add(rowBox);
            }

            groupBox.getChildren().add(itemsBox);
            groupSectionBoxes.put(group, groupBox);
            sectionsContainer.getChildren().add(groupBox);
        }
    }

    private VBox createItemRow(AdvancedSettingItem item) {
        VBox rowBox = new VBox(4);

        if (item.getType() == AdvancedSettingType.BOOLEAN) {
            CheckBox checkBox = new CheckBox(item.getTitle());
            checkBox.setSelected(item.asBoolean());
            checkBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
            checkBox.selectedProperty().addListener((obs, o, n) -> {
                item.setCurrentValue(n);
                onControlChanged();
            });
            itemControls.put(item.getId(), checkBox);
            rowBox.getChildren().add(checkBox);

            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                Label descLabel = new Label(item.getDescription());
                descLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-wrap-text: true;");
                descLabel.setPadding(new Insets(1, 0, 0, 22));
                rowBox.getChildren().add(descLabel);
            }

            if (item.getLinkText() != null && !item.getLinkText().isEmpty()) {
                Hyperlink link = new Hyperlink(item.getLinkText());
                link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");
                rowBox.getChildren().add(link);
            }
        } else if (item.getType() == AdvancedSettingType.INTEGER) {
            HBox controlRow = new HBox(8);
            controlRow.setAlignment(Pos.CENTER_LEFT);

            Label titleLabel = new Label(item.getTitle());
            titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

            TextField tf = new TextField(String.valueOf(item.asInt()));
            tf.setPrefWidth(80);
            tf.setMaxWidth(100);
            tf.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 6 3 6;");

            tf.textProperty().addListener((obs, o, n) -> {
                try {
                    int val = Integer.parseInt(n.trim());
                    item.setCurrentValue(val);
                } catch (NumberFormatException ignored) {
                }
                onControlChanged();
            });
            itemControls.put(item.getId(), tf);
            controlRow.getChildren().addAll(titleLabel, tf);

            if (item.getTrailingUnit() != null && !item.getTrailingUnit().isEmpty()) {
                Label unitLabel = new Label(item.getTrailingUnit());
                unitLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                controlRow.getChildren().add(unitLabel);
            }

            rowBox.getChildren().add(controlRow);

            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                Label descLabel = new Label(item.getDescription());
                descLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-wrap-text: true;");
                descLabel.setPadding(new Insets(1, 0, 0, 0));
                rowBox.getChildren().add(descLabel);
            }
        } else if (item.getType() == AdvancedSettingType.ENUM) {
            HBox controlRow = new HBox(8);
            controlRow.setAlignment(Pos.CENTER_LEFT);

            Label titleLabel = new Label(item.getTitle());
            titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

            ComboBox<String> cb = new ComboBox<>();
            cb.getItems().addAll(item.getOptions());
            cb.setValue(item.asString());
            cb.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

            cb.valueProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    item.setCurrentValue(n);
                    onControlChanged();
                }
            });
            itemControls.put(item.getId(), cb);
            controlRow.getChildren().addAll(titleLabel, cb);

            rowBox.getChildren().add(controlRow);

            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                Label descLabel = new Label(item.getDescription());
                descLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-wrap-text: true;");
                rowBox.getChildren().add(descLabel);
            }
        } else if (item.getType() == AdvancedSettingType.STRING) {
            HBox controlRow = new HBox(8);
            controlRow.setAlignment(Pos.CENTER_LEFT);

            Label titleLabel = new Label(item.getTitle());
            titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

            TextField tf = new TextField(item.asString());
            tf.setPrefWidth(260);
            tf.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 6 3 6;");

            tf.textProperty().addListener((obs, o, n) -> {
                item.setCurrentValue(n);
                onControlChanged();
            });
            itemControls.put(item.getId(), tf);
            controlRow.getChildren().addAll(titleLabel, tf);

            rowBox.getChildren().add(controlRow);

            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                Label descLabel = new Label(item.getDescription());
                descLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-wrap-text: true;");
                rowBox.getChildren().add(descLabel);
            }
        }

        return rowBox;
    }

    private void onControlChanged() {
        if (showModifiedOnlyCheckBox.isSelected()) {
            applyFilters();
        }
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModified(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public boolean isModified() {
        for (AdvancedSettingItem item : manager.getAllItems()) {
            Object orig = initialSnapshot.get(item.getId());
            if (!Objects.equals(orig, getUiValue(item.getId()))) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        for (AdvancedSettingItem item : manager.getAllItems()) {
            Object uiVal = getUiValue(item.getId());
            item.setCurrentValue(uiVal);
        }
        manager.apply();
        captureInitialSnapshot();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void reset() {
        manager.reset();
        syncControlsFromManager();
        captureInitialSnapshot();
        applyFilters();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void revertChanges() {
        for (Map.Entry<String, Object> entry : initialSnapshot.entrySet()) {
            setUiValue(entry.getKey(), entry.getValue());
            AdvancedSettingItem item = manager.getItem(entry.getKey());
            if (item != null) {
                item.setCurrentValue(entry.getValue());
            }
        }
        applyFilters();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void syncControlsFromManager() {
        for (AdvancedSettingItem item : manager.getAllItems()) {
            setUiValue(item.getId(), item.getCurrentValue());
        }
    }

    private void captureInitialSnapshot() {
        initialSnapshot.clear();
        for (AdvancedSettingItem item : manager.getAllItems()) {
            initialSnapshot.put(item.getId(), getUiValue(item.getId()));
        }
    }

    public Object getUiValue(String id) {
        Node control = itemControls.get(id);
        if (control instanceof CheckBox cb) {
            return cb.isSelected();
        } else if (control instanceof TextField tf) {
            AdvancedSettingItem item = manager.getItem(id);
            if (item != null && item.getType() == AdvancedSettingType.STRING) {
                return tf.getText();
            }
            try {
                return Integer.parseInt(tf.getText().trim());
            } catch (NumberFormatException e) {
                return tf.getText();
            }
        } else if (control instanceof ComboBox<?> cb) {
            return cb.getValue();
        }
        return null;
    }

    public void setUiValue(String id, Object val) {
        Node control = itemControls.get(id);
        if (control instanceof CheckBox cb) {
            if (val instanceof Boolean b) {
                cb.setSelected(b);
            } else {
                cb.setSelected(Boolean.parseBoolean(String.valueOf(val)));
            }
        } else if (control instanceof TextField tf) {
            tf.setText(String.valueOf(val));
        } else if (control instanceof ComboBox<?> cb) {
            @SuppressWarnings("unchecked")
            ComboBox<String> stringCb = (ComboBox<String>) cb;
            stringCb.setValue(String.valueOf(val));
        }
    }

    public void setSearchQuery(String query) {
        searchField.setText(query);
    }

    public void setShowModifiedOnly(boolean showModifiedOnly) {
        showModifiedOnlyCheckBox.setSelected(showModifiedOnly);
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        boolean modifiedOnly = showModifiedOnlyCheckBox.isSelected();

        for (String group : manager.getGroups()) {
            VBox groupBox = groupSectionBoxes.get(group);
            if (groupBox == null) continue;

            List<AdvancedSettingItem> items = manager.getItemsByGroup(group);
            int visibleCount = 0;

            for (AdvancedSettingItem item : items) {
                VBox rowBox = itemRowBoxes.get(item.getId());
                if (rowBox == null) continue;

                boolean matchesSearch = query.isEmpty()
                        || item.getTitle().toLowerCase().contains(query)
                        || (item.getDescription() != null && item.getDescription().toLowerCase().contains(query))
                        || group.toLowerCase().contains(query);

                boolean matchesModified = !modifiedOnly || isItemModifiedFromDefault(item);

                boolean visible = matchesSearch && matchesModified;
                rowBox.setVisible(visible);
                rowBox.setManaged(visible);
                if (visible) {
                    visibleCount++;
                }
            }

            groupBox.setVisible(visibleCount > 0);
            groupBox.setManaged(visibleCount > 0);
        }
    }

    private boolean isItemModifiedFromDefault(AdvancedSettingItem item) {
        Object uiVal = getUiValue(item.getId());
        return !Objects.equals(uiVal, item.getDefaultValue());
    }

    public Map<String, Node> getItemControls() {
        return Collections.unmodifiableMap(itemControls);
    }

    public Map<String, VBox> getGroupSectionBoxes() {
        return Collections.unmodifiableMap(groupSectionBoxes);
    }

    public TextField getSearchField() {
        return searchField;
    }

    public CheckBox getShowModifiedOnlyCheckBox() {
        return showModifiedOnlyCheckBox;
    }
}
