package dev.lumina.inlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Manages Inlay Hints configuration, persistence to ~/.lumina/inlay_hints.json,
 * and dynamic state access across the Lumina IDE.
 */
public class InlayHintsManager {

    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".lumina";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "inlay_hints.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static InlayHintsManager instance;

    private InlayHintsSettings settings;

    private InlayHintsManager() {
        this.settings = new InlayHintsSettings();
        load();
    }

    public static synchronized InlayHintsManager getInstance() {
        if (instance == null) {
            instance = new InlayHintsManager();
        }
        return instance;
    }

    public InlayHintsSettings getSettings() {
        return settings;
    }

    public void setSettings(InlayHintsSettings settings) {
        if (settings != null) {
            this.settings = settings.copy();
        }
    }

    public void resetToDefaults() {
        this.settings = new InlayHintsSettings();
    }

    public void save() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(this.settings, writer);
            }
        } catch (IOException e) {
            System.err.println("[InlayHints] Failed to save config: " + e.getMessage());
        }
    }

    public void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            InlayHintsSettings loaded = GSON.fromJson(reader, InlayHintsSettings.class);
            if (loaded != null) {
                this.settings = loaded;
            }
        } catch (Exception e) {
            System.err.println("[InlayHints] Failed to load config: " + e.getMessage());
        }
    }
}
