package dev.lumina.ui;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Dynamic background image service managing configurations, recent files,
 * real-time change listeners, and canvas rendering for Lumina IDE.
 */
public class BackgroundImageService {

    public enum Target {
        EDITOR_AND_TOOLS,
        EMPTY_FRAME
    }

    private static final BackgroundImageService INSTANCE = new BackgroundImageService();

    private final Preferences prefs = Preferences.userNodeForPackage(BackgroundImageService.class);
    private final List<Consumer<Target>> listeners = new ArrayList<>();
    private final List<String> recentImages = new ArrayList<>();

    // Cached JavaFX Images
    private Image cachedEditorImage = null;
    private String cachedEditorPath = "";
    private Image cachedFrameImage = null;
    private String cachedFramePath = "";

    // Target States
    public static class TargetConfig {
        public String imagePath = "";
        public int opacity = 15;
        public int scaleMode = 1; // 0 = Center, 1 = Fill/Stretch, 2 = Tile
        public int anchorRow = 1;  // 0 = Top, 1 = Center, 2 = Bottom
        public int anchorCol = 1;  // 0 = Left, 1 = Center, 2 = Right
        public boolean flipH = false;
        public boolean flipV = false;
        public boolean projectOnly = false;

        public TargetConfig copy() {
            TargetConfig c = new TargetConfig();
            c.imagePath = this.imagePath;
            c.opacity = this.opacity;
            c.scaleMode = this.scaleMode;
            c.anchorRow = this.anchorRow;
            c.anchorCol = this.anchorCol;
            c.flipH = this.flipH;
            c.flipV = this.flipV;
            c.projectOnly = this.projectOnly;
            return c;
        }
    }

    private final TargetConfig editorConfig = new TargetConfig();
    private final TargetConfig frameConfig = new TargetConfig();

    private BackgroundImageService() {
        loadPreferences();
    }

    public static BackgroundImageService getInstance() {
        return INSTANCE;
    }

    public synchronized TargetConfig getConfig(Target target) {
        return (target == Target.EDITOR_AND_TOOLS ? editorConfig : frameConfig).copy();
    }

    public synchronized void setConfig(Target target, TargetConfig newConfig) {
        if (newConfig == null) return;
        TargetConfig current = (target == Target.EDITOR_AND_TOOLS) ? editorConfig : frameConfig;
        current.imagePath = newConfig.imagePath != null ? newConfig.imagePath.trim() : "";
        current.opacity = Math.max(0, Math.min(100, newConfig.opacity));
        current.scaleMode = newConfig.scaleMode;
        current.anchorRow = newConfig.anchorRow;
        current.anchorCol = newConfig.anchorCol;
        current.flipH = newConfig.flipH;
        current.flipV = newConfig.flipV;
        current.projectOnly = newConfig.projectOnly;

        if (!current.imagePath.isEmpty()) {
            addRecentImage(current.imagePath);
        }

        savePreferences();
        notifyListeners(target);
    }

    public synchronized void clear(Target target) {
        TargetConfig current = (target == Target.EDITOR_AND_TOOLS) ? editorConfig : frameConfig;
        current.imagePath = "";
        current.opacity = 15;
        savePreferences();
        notifyListeners(target);
    }

    public synchronized List<String> getRecentImages() {
        return new ArrayList<>(recentImages);
    }

    public synchronized void addRecentImage(String path) {
        if (path == null || path.trim().isEmpty()) return;
        String clean = path.trim();
        recentImages.remove(clean);
        recentImages.add(0, clean);
        if (recentImages.size() > 10) {
            recentImages.remove(recentImages.size() - 1);
        }
        saveRecentImages();
    }

    public synchronized void addListener(Consumer<Target> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Consumer<Target> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Target target) {
        for (Consumer<Target> l : new ArrayList<>(listeners)) {
            try {
                l.accept(target);
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void renderBackground(GraphicsContext gc, double width, double height, Target target) {
        if (gc == null || width <= 0 || height <= 0) return;

        TargetConfig cfg = (target == Target.EDITOR_AND_TOOLS) ? editorConfig : frameConfig;
        if (cfg.imagePath == null || cfg.imagePath.isEmpty()) return;

        Image img = getImageForTarget(target, cfg.imagePath);
        if (img == null || img.isError() || img.getWidth() <= 0 || img.getHeight() <= 0) return;

        double imgW = img.getWidth();
        double imgH = img.getHeight();

        gc.save();
        gc.setGlobalAlpha(Math.max(0.0, Math.min(1.0, cfg.opacity / 100.0)));

        if (cfg.flipH || cfg.flipV) {
            gc.translate(cfg.flipH ? width : 0, cfg.flipV ? height : 0);
            gc.scale(cfg.flipH ? -1 : 1, cfg.flipV ? -1 : 1);
        }

        if (cfg.scaleMode == 1) {
            // Fill / Stretch
            gc.drawImage(img, 0, 0, width, height);
        } else if (cfg.scaleMode == 0) {
            // Center / natural size with anchor
            double drawX;
            if (cfg.anchorCol == 0) drawX = 0;
            else if (cfg.anchorCol == 2) drawX = width - imgW;
            else drawX = (width - imgW) / 2.0;

            double drawY;
            if (cfg.anchorRow == 0) drawY = 0;
            else if (cfg.anchorRow == 2) drawY = height - imgH;
            else drawY = (height - imgH) / 2.0;

            gc.drawImage(img, drawX, drawY);
        } else if (cfg.scaleMode == 2) {
            // Tile
            double tileW = Math.min(imgW, width);
            double tileH = Math.min(imgH, height);
            if (tileW <= 0) tileW = 100;
            if (tileH <= 0) tileH = 100;

            for (double x = 0; x < width; x += tileW) {
                for (double y = 0; y < height; y += tileH) {
                    gc.drawImage(img, x, y, tileW, tileH);
                }
            }
        }

        gc.restore();
    }

    private Image getImageForTarget(Target target, String path) {
        if (target == Target.EDITOR_AND_TOOLS) {
            if (cachedEditorImage != null && path.equals(cachedEditorPath)) {
                return cachedEditorImage;
            }
            try {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    cachedEditorImage = new Image(f.toURI().toString());
                    cachedEditorPath = path;
                    return cachedEditorImage;
                }
            } catch (Exception ignored) {
            }
            cachedEditorImage = null;
            cachedEditorPath = "";
            return null;
        } else {
            if (cachedFrameImage != null && path.equals(cachedFramePath)) {
                return cachedFrameImage;
            }
            try {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    cachedFrameImage = new Image(f.toURI().toString());
                    cachedFramePath = path;
                    return cachedFrameImage;
                }
            } catch (Exception ignored) {
            }
            cachedFrameImage = null;
            cachedFramePath = "";
            return null;
        }
    }

    private void loadPreferences() {
        // Editor
        editorConfig.imagePath = prefs.get("bg_editor_path", prefs.get("bg_image_path", ""));
        editorConfig.opacity = prefs.getInt("bg_editor_opacity", prefs.getInt("bg_opacity", 15));
        editorConfig.scaleMode = prefs.getInt("bg_editor_scale", prefs.getInt("bg_scale_mode", 1));
        editorConfig.anchorRow = prefs.getInt("bg_editor_anchor_row", prefs.getInt("bg_anchor_row", 1));
        editorConfig.anchorCol = prefs.getInt("bg_editor_anchor_col", prefs.getInt("bg_anchor_col", 1));
        editorConfig.flipH = prefs.getBoolean("bg_editor_flip_h", false);
        editorConfig.flipV = prefs.getBoolean("bg_editor_flip_v", false);
        editorConfig.projectOnly = prefs.getBoolean("bg_editor_project_only", prefs.getBoolean("bg_project_only", false));

        // Frame
        frameConfig.imagePath = prefs.get("bg_frame_path", "");
        frameConfig.opacity = prefs.getInt("bg_frame_opacity", 15);
        frameConfig.scaleMode = prefs.getInt("bg_frame_scale", 1);
        frameConfig.anchorRow = prefs.getInt("bg_frame_anchor_row", 1);
        frameConfig.anchorCol = prefs.getInt("bg_frame_anchor_col", 1);
        frameConfig.flipH = prefs.getBoolean("bg_frame_flip_h", false);
        frameConfig.flipV = prefs.getBoolean("bg_frame_flip_v", false);
        frameConfig.projectOnly = prefs.getBoolean("bg_frame_project_only", false);

        // Recent images
        recentImages.clear();
        String rec = prefs.get("bg_recent_images", "");
        if (!rec.isEmpty()) {
            String[] parts = rec.split("\n");
            for (String p : parts) {
                if (!p.trim().isEmpty() && !recentImages.contains(p.trim())) {
                    recentImages.add(p.trim());
                }
            }
        }
        if (!editorConfig.imagePath.isEmpty() && !recentImages.contains(editorConfig.imagePath)) {
            recentImages.add(0, editorConfig.imagePath);
        }
        if (!frameConfig.imagePath.isEmpty() && !recentImages.contains(frameConfig.imagePath)) {
            recentImages.add(0, frameConfig.imagePath);
        }
    }

    private void savePreferences() {
        prefs.put("bg_editor_path", editorConfig.imagePath);
        prefs.put("bg_image_path", editorConfig.imagePath);
        prefs.putInt("bg_editor_opacity", editorConfig.opacity);
        prefs.putInt("bg_opacity", editorConfig.opacity);
        prefs.putInt("bg_editor_scale", editorConfig.scaleMode);
        prefs.putInt("bg_scale_mode", editorConfig.scaleMode);
        prefs.putInt("bg_editor_anchor_row", editorConfig.anchorRow);
        prefs.putInt("bg_anchor_row", editorConfig.anchorRow);
        prefs.putInt("bg_editor_anchor_col", editorConfig.anchorCol);
        prefs.putInt("bg_anchor_col", editorConfig.anchorCol);
        prefs.putBoolean("bg_editor_flip_h", editorConfig.flipH);
        prefs.putBoolean("bg_editor_flip_v", editorConfig.flipV);
        prefs.putBoolean("bg_editor_project_only", editorConfig.projectOnly);
        prefs.putBoolean("bg_project_only", editorConfig.projectOnly);

        prefs.put("bg_frame_path", frameConfig.imagePath);
        prefs.putInt("bg_frame_opacity", frameConfig.opacity);
        prefs.putInt("bg_frame_scale", frameConfig.scaleMode);
        prefs.putInt("bg_frame_anchor_row", frameConfig.anchorRow);
        prefs.putInt("bg_frame_anchor_col", frameConfig.anchorCol);
        prefs.putBoolean("bg_frame_flip_h", frameConfig.flipH);
        prefs.putBoolean("bg_frame_flip_v", frameConfig.flipV);
        prefs.putBoolean("bg_frame_project_only", frameConfig.projectOnly);
    }

    private void saveRecentImages() {
        StringBuilder sb = new StringBuilder();
        for (String p : recentImages) {
            sb.append(p).append("\n");
        }
        prefs.put("bg_recent_images", sb.toString());
    }
}
