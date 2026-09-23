package dev.lumina.folding;

import dev.lumina.util.Settings;

/**
 * Manages configuration and default states for code folding matching IntelliJ IDEA.
 */
public class CodeFoldingSettings {

    private static final String KEY_FOLD_IMPORTS = "editor.folding.imports";
    private static final String KEY_FOLD_DOC_COMMENTS = "editor.folding.doc_comments";
    private static final String KEY_FOLD_METHOD_BODIES = "editor.folding.method_bodies";
    private static final String KEY_FOLD_ANNOTATIONS = "editor.folding.annotations";
    private static final String KEY_FOLD_CUSTOM_REGIONS = "editor.folding.custom_regions";
    private static final String KEY_SHOW_FOLDING_ARROWS = "editor.folding.show_arrows";
    private static final String KEY_SHOW_BOTTOM_ARROWS = "editor.folding.show_bottom_arrows";

    private static final CodeFoldingSettings INSTANCE = new CodeFoldingSettings();

    public static CodeFoldingSettings getInstance() {
        return INSTANCE;
    }

    private boolean foldImportsByDefault = true;
    private boolean foldDocCommentsByDefault = true;
    private boolean foldMethodBodiesByDefault = false;
    private boolean foldAnnotationsByDefault = false;
    private boolean foldCustomRegionsByDefault = true;
    private boolean showFoldingArrows = true;
    private boolean showBottomArrows = true;

    private CodeFoldingSettings() {
        load();
    }

    public void load() {
        foldImportsByDefault = getBoolean(KEY_FOLD_IMPORTS, true);
        foldDocCommentsByDefault = getBoolean(KEY_FOLD_DOC_COMMENTS, true);
        foldMethodBodiesByDefault = getBoolean(KEY_FOLD_METHOD_BODIES, false);
        foldAnnotationsByDefault = getBoolean(KEY_FOLD_ANNOTATIONS, false);
        foldCustomRegionsByDefault = getBoolean(KEY_FOLD_CUSTOM_REGIONS, true);
        showFoldingArrows = getBoolean(KEY_SHOW_FOLDING_ARROWS, true);
        showBottomArrows = getBoolean(KEY_SHOW_BOTTOM_ARROWS, true);
    }

    public void save() {
        Settings.put(KEY_FOLD_IMPORTS, String.valueOf(foldImportsByDefault));
        Settings.put(KEY_FOLD_DOC_COMMENTS, String.valueOf(foldDocCommentsByDefault));
        Settings.put(KEY_FOLD_METHOD_BODIES, String.valueOf(foldMethodBodiesByDefault));
        Settings.put(KEY_FOLD_ANNOTATIONS, String.valueOf(foldAnnotationsByDefault));
        Settings.put(KEY_FOLD_CUSTOM_REGIONS, String.valueOf(foldCustomRegionsByDefault));
        Settings.put(KEY_SHOW_FOLDING_ARROWS, String.valueOf(showFoldingArrows));
        Settings.put(KEY_SHOW_BOTTOM_ARROWS, String.valueOf(showBottomArrows));
    }

    private static boolean getBoolean(String key, boolean def) {
        String val = Settings.get(key);
        if (val == null || val.isBlank()) return def;
        return Boolean.parseBoolean(val.trim());
    }

    public boolean isFoldImportsByDefault() {
        return foldImportsByDefault;
    }

    public void setFoldImportsByDefault(boolean foldImportsByDefault) {
        this.foldImportsByDefault = foldImportsByDefault;
        save();
    }

    public boolean isFoldDocCommentsByDefault() {
        return foldDocCommentsByDefault;
    }

    public void setFoldDocCommentsByDefault(boolean foldDocCommentsByDefault) {
        this.foldDocCommentsByDefault = foldDocCommentsByDefault;
        save();
    }

    public boolean isFoldMethodBodiesByDefault() {
        return foldMethodBodiesByDefault;
    }

    public void setFoldMethodBodiesByDefault(boolean foldMethodBodiesByDefault) {
        this.foldMethodBodiesByDefault = foldMethodBodiesByDefault;
        save();
    }

    public boolean isFoldAnnotationsByDefault() {
        return foldAnnotationsByDefault;
    }

    public void setFoldAnnotationsByDefault(boolean foldAnnotationsByDefault) {
        this.foldAnnotationsByDefault = foldAnnotationsByDefault;
        save();
    }

    public boolean isFoldCustomRegionsByDefault() {
        return foldCustomRegionsByDefault;
    }

    public void setFoldCustomRegionsByDefault(boolean foldCustomRegionsByDefault) {
        this.foldCustomRegionsByDefault = foldCustomRegionsByDefault;
        save();
    }

    public boolean isShowFoldingArrows() {
        return showFoldingArrows;
    }

    public void setShowFoldingArrows(boolean showFoldingArrows) {
        this.showFoldingArrows = showFoldingArrows;
        save();
    }

    public boolean isShowBottomArrows() {
        return showBottomArrows;
    }

    public void setShowBottomArrows(boolean showBottomArrows) {
        this.showBottomArrows = showBottomArrows;
        save();
    }
}
