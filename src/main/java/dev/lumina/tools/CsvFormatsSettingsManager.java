package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for delimiter-separated values (CSV/TSV) formats in Lumina IDE.
 */
public class CsvFormatsSettingsManager {

    private static final String KEY_SELECTED_FORMAT = "tools.csv_formats.selected";
    private static volatile CsvFormatsSettingsManager instance;

    private CsvFormatsSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private CsvFormatsSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static CsvFormatsSettingsManager getInstance() {
        if (instance == null) {
            synchronized (CsvFormatsSettingsManager.class) {
                if (instance == null) {
                    instance = new CsvFormatsSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized CsvFormatsSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(CsvFormatsSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new CsvFormatsSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
    }

    /**
     * Parses raw CSV text into a 2D table grid according to the given CsvFormat rules.
     */
    public List<List<String>> parseData(String rawText, CsvFormat format) {
        List<List<String>> rows = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) return rows;

        char delimiter = (format != null && format.getDelimiterChar() != null && !format.getDelimiterChar().isEmpty())
                ? format.getDelimiterChar().charAt(0)
                : ',';
        String[] lines = rawText.split("\\r?\\n");

        for (String line : lines) {
            if (line.isEmpty()) continue;
            List<String> cells = new ArrayList<>();
            StringBuilder currentCell = new StringBuilder();
            boolean inQuotes = false;
            char quoteChar = '"';

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if ((c == '"' || c == '\'') && !inQuotes && currentCell.length() == 0) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (inQuotes && c == quoteChar) {
                    // Check for duplicate quote escape e.g. ""
                    if (i + 1 < line.length() && line.charAt(i + 1) == quoteChar) {
                        currentCell.append(quoteChar);
                        i++; // skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else if (!inQuotes && c == delimiter) {
                    String val = currentCell.toString();
                    if (format != null && format.isTrimWhitespaces()) {
                        val = val.trim();
                    }
                    cells.add(val);
                    currentCell.setLength(0);
                } else {
                    currentCell.append(c);
                }
            }
            String lastVal = currentCell.toString();
            if (format != null && format.isTrimWhitespaces()) {
                lastVal = lastVal.trim();
            }
            cells.add(lastVal);
            rows.add(cells);
        }
        return rows;
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private CsvFormatsSettings loadSettings() {
        CsvFormatsSettings settings = new CsvFormatsSettings();
        String selected = Settings.get(KEY_SELECTED_FORMAT);
        if (selected != null && !selected.isBlank()) {
            settings.setSelectedFormatName(selected);
        }
        return settings;
    }

    private void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_SELECTED_FORMAT, currentSettings.getSelectedFormatName());
    }
}
