package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for IntelliJ IDEA-style Editor > General > Editor Tabs settings.
 * Backed by ~/.lumina/lumina.properties, supporting listeners, dirty tracking, and real-time tab updates.
 */
public final class EditorTabsSettings {

    public enum TabPlacement {
        TOP("Top"),
        LEFT("Left"),
        BOTTOM("Bottom"),
        RIGHT("Right"),
        NONE("None");

        private final String label;

        TabPlacement(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static TabPlacement fromLabel(String label) {
            for (TabPlacement p : values()) {
                if (p.label.equalsIgnoreCase(label) || p.name().equalsIgnoreCase(label)) {
                    return p;
                }
            }
            return TOP;
        }
    }

    public enum TabRowsMode {
        ONE_ROW("One row, and if tabs don't fit:"),
        MULTIPLE_ROWS("Multiple rows");

        private final String label;

        TabRowsMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static TabRowsMode fromLabel(String label) {
            for (TabRowsMode m : values()) {
                if (m.label.equalsIgnoreCase(label) || m.name().equalsIgnoreCase(label)) {
                    return m;
                }
            }
            return ONE_ROW;
        }
    }

    public enum OneRowFitPolicy {
        SCROLL("Scroll the tabs panel"),
        SQUEEZE("Squeeze tabs");

        private final String label;

        OneRowFitPolicy(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static OneRowFitPolicy fromLabel(String label) {
            for (OneRowFitPolicy p : values()) {
                if (p.label.equalsIgnoreCase(label) || p.name().equalsIgnoreCase(label)) {
                    return p;
                }
            }
            return SCROLL;
        }
    }

    public enum CloseButtonPosition {
        LEFT("Left"),
        RIGHT("Right"),
        NONE("None");

        private final String label;

        CloseButtonPosition(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static CloseButtonPosition fromLabel(String label) {
            for (CloseButtonPosition p : values()) {
                if (p.label.equalsIgnoreCase(label) || p.name().equalsIgnoreCase(label)) {
                    return p;
                }
            }
            return RIGHT;
        }
    }

    public enum TabsExceedLimitPolicy {
        CLOSE_UNCHANGED("Close unchanged"),
        CLOSE_UNUSED("Close unused");

        private final String label;

        TabsExceedLimitPolicy(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static TabsExceedLimitPolicy fromLabel(String label) {
            for (TabsExceedLimitPolicy p : values()) {
                if (p.label.equalsIgnoreCase(label) || p.name().equalsIgnoreCase(label)) {
                    return p;
                }
            }
            return CLOSE_UNUSED;
        }
    }

    public enum TabCloseActivatePolicy {
        ACTIVATE_LEFT("The tab on the left"),
        ACTIVATE_RIGHT("The tab on the right"),
        ACTIVATE_MOST_RECENT("Most recently opened tab");

        private final String label;

        TabCloseActivatePolicy(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static TabCloseActivatePolicy fromLabel(String label) {
            for (TabCloseActivatePolicy p : values()) {
                if (p.label.equalsIgnoreCase(label) || p.name().equalsIgnoreCase(label)) {
                    return p;
                }
            }
            return ACTIVATE_LEFT;
        }
    }

    private static final EditorTabsSettings INSTANCE = new EditorTabsSettings();

    public static EditorTabsSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(EditorTabsSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // 1. Appearance
    private TabPlacement tabPlacement = TabPlacement.TOP;
    private TabRowsMode tabRowsMode = TabRowsMode.ONE_ROW;
    private OneRowFitPolicy oneRowFitPolicy = OneRowFitPolicy.SCROLL;
    private boolean showPinnedTabsInSeparateRow = false;
    private boolean showFileIcon = true;
    private boolean showFileExtension = true;
    private boolean showDirectoryForNonUniqueNames = true;
    private boolean markModified = false;
    private boolean showFullPathOnMouseHover = true;
    private CloseButtonPosition closeButtonPosition = CloseButtonPosition.RIGHT;

    // 2. Tab Order
    private boolean sortTabsAlphabetically = false;
    private boolean openNewTabsAtEnd = false;

    // 3. Opening Policy
    private boolean enablePreviewTab = false;

    // 4. Closing Policy
    private int tabLimit = 30;
    private TabsExceedLimitPolicy tabsExceedLimitPolicy = TabsExceedLimitPolicy.CLOSE_UNUSED;
    private TabCloseActivatePolicy tabCloseActivatePolicy = TabCloseActivatePolicy.ACTIVATE_LEFT;

    // 5. Database
    private boolean dbAlwaysShowQualifiedNames = false;
    private boolean dbShortenDatasourceNames = true;

    public EditorTabsSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        tabPlacement = TabPlacement.TOP;
        tabRowsMode = TabRowsMode.ONE_ROW;
        oneRowFitPolicy = OneRowFitPolicy.SCROLL;
        showPinnedTabsInSeparateRow = false;
        showFileIcon = true;
        showFileExtension = true;
        showDirectoryForNonUniqueNames = true;
        markModified = false;
        showFullPathOnMouseHover = true;
        closeButtonPosition = CloseButtonPosition.RIGHT;

        sortTabsAlphabetically = false;
        openNewTabsAtEnd = false;

        enablePreviewTab = false;

        tabLimit = 30;
        tabsExceedLimitPolicy = TabsExceedLimitPolicy.CLOSE_UNUSED;
        tabCloseActivatePolicy = TabCloseActivatePolicy.ACTIVATE_LEFT;

        dbAlwaysShowQualifiedNames = false;
        dbShortenDatasourceNames = true;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void load() {
        tabPlacement = TabPlacement.fromLabel(getStr("tab_placement", tabPlacement.getLabel()));
        tabRowsMode = TabRowsMode.fromLabel(getStr("tab_rows_mode", tabRowsMode.getLabel()));
        oneRowFitPolicy = OneRowFitPolicy.fromLabel(getStr("one_row_fit_policy", oneRowFitPolicy.getLabel()));
        showPinnedTabsInSeparateRow = getBool("show_pinned_tabs_separate_row", showPinnedTabsInSeparateRow);
        showFileIcon = getBool("show_file_icon", showFileIcon);
        showFileExtension = getBool("show_file_extension", showFileExtension);
        showDirectoryForNonUniqueNames = getBool("show_directory_non_unique", showDirectoryForNonUniqueNames);
        markModified = getBool("mark_modified", markModified);
        showFullPathOnMouseHover = getBool("show_full_path_hover", showFullPathOnMouseHover);
        closeButtonPosition = CloseButtonPosition.fromLabel(getStr("close_button_position", closeButtonPosition.getLabel()));

        sortTabsAlphabetically = getBool("sort_tabs_alphabetically", sortTabsAlphabetically);
        openNewTabsAtEnd = getBool("open_new_tabs_at_end", openNewTabsAtEnd);

        enablePreviewTab = getBool("enable_preview_tab", enablePreviewTab);

        tabLimit = getInt("tab_limit", tabLimit);
        tabsExceedLimitPolicy = TabsExceedLimitPolicy.fromLabel(getStr("tabs_exceed_limit_policy", tabsExceedLimitPolicy.getLabel()));
        tabCloseActivatePolicy = TabCloseActivatePolicy.fromLabel(getStr("tab_close_activate_policy", tabCloseActivatePolicy.getLabel()));

        dbAlwaysShowQualifiedNames = getBool("db_always_show_qualified_names", dbAlwaysShowQualifiedNames);
        dbShortenDatasourceNames = getBool("db_shorten_datasource_names", dbShortenDatasourceNames);
    }

    public void save() {
        putStr("tab_placement", tabPlacement.getLabel());
        putStr("tab_rows_mode", tabRowsMode.getLabel());
        putStr("one_row_fit_policy", oneRowFitPolicy.getLabel());
        putBool("show_pinned_tabs_separate_row", showPinnedTabsInSeparateRow);
        putBool("show_file_icon", showFileIcon);
        putBool("show_file_extension", showFileExtension);
        putBool("show_directory_non_unique", showDirectoryForNonUniqueNames);
        putBool("mark_modified", markModified);
        putBool("show_full_path_hover", showFullPathOnMouseHover);
        putStr("close_button_position", closeButtonPosition.getLabel());

        putBool("sort_tabs_alphabetically", sortTabsAlphabetically);
        putBool("open_new_tabs_at_end", openNewTabsAtEnd);

        putBool("enable_preview_tab", enablePreviewTab);

        putInt("tab_limit", tabLimit);
        putStr("tabs_exceed_limit_policy", tabsExceedLimitPolicy.getLabel());
        putStr("tab_close_activate_policy", tabCloseActivatePolicy.getLabel());

        putBool("db_always_show_qualified_names", dbAlwaysShowQualifiedNames);
        putBool("db_shorten_datasource_names", dbShortenDatasourceNames);

        notifyListeners();
    }

    public void copyFrom(EditorTabsSettings o) {
        this.tabPlacement = o.tabPlacement;
        this.tabRowsMode = o.tabRowsMode;
        this.oneRowFitPolicy = o.oneRowFitPolicy;
        this.showPinnedTabsInSeparateRow = o.showPinnedTabsInSeparateRow;
        this.showFileIcon = o.showFileIcon;
        this.showFileExtension = o.showFileExtension;
        this.showDirectoryForNonUniqueNames = o.showDirectoryForNonUniqueNames;
        this.markModified = o.markModified;
        this.showFullPathOnMouseHover = o.showFullPathOnMouseHover;
        this.closeButtonPosition = o.closeButtonPosition;

        this.sortTabsAlphabetically = o.sortTabsAlphabetically;
        this.openNewTabsAtEnd = o.openNewTabsAtEnd;

        this.enablePreviewTab = o.enablePreviewTab;

        this.tabLimit = o.tabLimit;
        this.tabsExceedLimitPolicy = o.tabsExceedLimitPolicy;
        this.tabCloseActivatePolicy = o.tabCloseActivatePolicy;

        this.dbAlwaysShowQualifiedNames = o.dbAlwaysShowQualifiedNames;
        this.dbShortenDatasourceNames = o.dbShortenDatasourceNames;
    }

    public EditorTabsSettings copy() {
        EditorTabsSettings c = new EditorTabsSettings();
        c.copyFrom(this);
        return c;
    }

    public boolean isModified(EditorTabsSettings o) {
        if (o == null) return true;
        return this.tabPlacement != o.tabPlacement
                || this.tabRowsMode != o.tabRowsMode
                || this.oneRowFitPolicy != o.oneRowFitPolicy
                || this.showPinnedTabsInSeparateRow != o.showPinnedTabsInSeparateRow
                || this.showFileIcon != o.showFileIcon
                || this.showFileExtension != o.showFileExtension
                || this.showDirectoryForNonUniqueNames != o.showDirectoryForNonUniqueNames
                || this.markModified != o.markModified
                || this.showFullPathOnMouseHover != o.showFullPathOnMouseHover
                || this.closeButtonPosition != o.closeButtonPosition
                || this.sortTabsAlphabetically != o.sortTabsAlphabetically
                || this.openNewTabsAtEnd != o.openNewTabsAtEnd
                || this.enablePreviewTab != o.enablePreviewTab
                || this.tabLimit != o.tabLimit
                || this.tabsExceedLimitPolicy != o.tabsExceedLimitPolicy
                || this.tabCloseActivatePolicy != o.tabCloseActivatePolicy
                || this.dbAlwaysShowQualifiedNames != o.dbAlwaysShowQualifiedNames
                || this.dbShortenDatasourceNames != o.dbShortenDatasourceNames;
    }

    private boolean getBool(String key, boolean def) {
        String val = Settings.get("editor.tabs." + key);
        return val != null ? Boolean.parseBoolean(val) : def;
    }

    private void putBool(String key, boolean val) {
        Settings.put("editor.tabs." + key, String.valueOf(val));
    }

    private int getInt(String key, int def) {
        String val = Settings.get("editor.tabs." + key);
        if (val != null) {
            try { return Integer.parseInt(val.trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private void putInt(String key, int val) {
        Settings.put("editor.tabs." + key, String.valueOf(val));
    }

    private String getStr(String key, String def) {
        String val = Settings.get("editor.tabs." + key);
        return val != null ? val : def;
    }

    private void putStr(String key, String val) {
        Settings.put("editor.tabs." + key, val != null ? val : "");
    }

    // Getters and Setters
    public TabPlacement getTabPlacement() { return tabPlacement; }
    public void setTabPlacement(TabPlacement tabPlacement) { this.tabPlacement = tabPlacement != null ? tabPlacement : TabPlacement.TOP; }

    public TabRowsMode getTabRowsMode() { return tabRowsMode; }
    public void setTabRowsMode(TabRowsMode tabRowsMode) { this.tabRowsMode = tabRowsMode != null ? tabRowsMode : TabRowsMode.ONE_ROW; }

    public OneRowFitPolicy getOneRowFitPolicy() { return oneRowFitPolicy; }
    public void setOneRowFitPolicy(OneRowFitPolicy oneRowFitPolicy) { this.oneRowFitPolicy = oneRowFitPolicy != null ? oneRowFitPolicy : OneRowFitPolicy.SCROLL; }

    public boolean isShowPinnedTabsInSeparateRow() { return showPinnedTabsInSeparateRow; }
    public void setShowPinnedTabsInSeparateRow(boolean showPinnedTabsInSeparateRow) { this.showPinnedTabsInSeparateRow = showPinnedTabsInSeparateRow; }

    public boolean isShowFileIcon() { return showFileIcon; }
    public void setShowFileIcon(boolean showFileIcon) { this.showFileIcon = showFileIcon; }

    public boolean isShowFileExtension() { return showFileExtension; }
    public void setShowFileExtension(boolean showFileExtension) { this.showFileExtension = showFileExtension; }

    public boolean isShowDirectoryForNonUniqueNames() { return showDirectoryForNonUniqueNames; }
    public void setShowDirectoryForNonUniqueNames(boolean showDirectoryForNonUniqueNames) { this.showDirectoryForNonUniqueNames = showDirectoryForNonUniqueNames; }

    public boolean isMarkModified() { return markModified; }
    public void setMarkModified(boolean markModified) { this.markModified = markModified; }

    public boolean isShowFullPathOnMouseHover() { return showFullPathOnMouseHover; }
    public void setShowFullPathOnMouseHover(boolean showFullPathOnMouseHover) { this.showFullPathOnMouseHover = showFullPathOnMouseHover; }

    public CloseButtonPosition getCloseButtonPosition() { return closeButtonPosition; }
    public void setCloseButtonPosition(CloseButtonPosition closeButtonPosition) { this.closeButtonPosition = closeButtonPosition != null ? closeButtonPosition : CloseButtonPosition.RIGHT; }

    public boolean isSortTabsAlphabetically() { return sortTabsAlphabetically; }
    public void setSortTabsAlphabetically(boolean sortTabsAlphabetically) { this.sortTabsAlphabetically = sortTabsAlphabetically; }

    public boolean isOpenNewTabsAtEnd() { return openNewTabsAtEnd; }
    public void setOpenNewTabsAtEnd(boolean openNewTabsAtEnd) { this.openNewTabsAtEnd = openNewTabsAtEnd; }

    public boolean isEnablePreviewTab() { return enablePreviewTab; }
    public void setEnablePreviewTab(boolean enablePreviewTab) { this.enablePreviewTab = enablePreviewTab; }

    public int getTabLimit() { return tabLimit; }
    public void setTabLimit(int tabLimit) { this.tabLimit = tabLimit; }

    public TabsExceedLimitPolicy getTabsExceedLimitPolicy() { return tabsExceedLimitPolicy; }
    public void setTabsExceedLimitPolicy(TabsExceedLimitPolicy tabsExceedLimitPolicy) { this.tabsExceedLimitPolicy = tabsExceedLimitPolicy != null ? tabsExceedLimitPolicy : TabsExceedLimitPolicy.CLOSE_UNUSED; }

    public TabCloseActivatePolicy getTabCloseActivatePolicy() { return tabCloseActivatePolicy; }
    public void setTabCloseActivatePolicy(TabCloseActivatePolicy tabCloseActivatePolicy) { this.tabCloseActivatePolicy = tabCloseActivatePolicy != null ? tabCloseActivatePolicy : TabCloseActivatePolicy.ACTIVATE_LEFT; }

    public boolean isDbAlwaysShowQualifiedNames() { return dbAlwaysShowQualifiedNames; }
    public void setDbAlwaysShowQualifiedNames(boolean dbAlwaysShowQualifiedNames) { this.dbAlwaysShowQualifiedNames = dbAlwaysShowQualifiedNames; }

    public boolean isDbShortenDatasourceNames() { return dbShortenDatasourceNames; }
    public void setDbShortenDatasourceNames(boolean dbShortenDatasourceNames) { this.dbShortenDatasourceNames = dbShortenDatasourceNames; }
}
