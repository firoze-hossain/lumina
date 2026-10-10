package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings configuration model holding all CSV/TSV format definitions.
 */
public class CsvFormatsSettings implements Cloneable {

    private List<CsvFormat> formats = new ArrayList<>();
    private String selectedFormatName = "CSV";

    public CsvFormatsSettings() {
        initDefaults();
    }

    private void initDefaults() {
        formats.clear();

        CsvFormat csv = new CsvFormat("CSV", "Comma");
        formats.add(csv);

        CsvFormat tsv = new CsvFormat("TSV", "Tab");
        formats.add(tsv);

        CsvFormat pipe = new CsvFormat("Pipe-separated", "Pipe");
        formats.add(pipe);

        CsvFormat semi = new CsvFormat("Semicolon-separated", "Semicolon");
        formats.add(semi);
    }

    public List<CsvFormat> getFormats() {
        return formats;
    }

    public void setFormats(List<CsvFormat> formats) {
        this.formats = formats != null ? new ArrayList<>(formats) : new ArrayList<>();
    }

    public String getSelectedFormatName() {
        return selectedFormatName;
    }

    public void setSelectedFormatName(String selectedFormatName) {
        this.selectedFormatName = selectedFormatName != null ? selectedFormatName : "CSV";
    }

    public CsvFormat findFormatByName(String name) {
        if (name == null) return null;
        for (CsvFormat f : formats) {
            if (name.equalsIgnoreCase(f.getName())) {
                return f;
            }
        }
        return null;
    }

    public CsvFormat getFormatByName(String name) {
        return findFormatByName(name);
    }

    public CsvFormat getSelectedFormat() {
        CsvFormat f = findFormatByName(selectedFormatName);
        if (f != null) return f;
        return formats.isEmpty() ? null : formats.get(0);
    }

    @Override
    public CsvFormatsSettings clone() {
        try {
            CsvFormatsSettings copy = (CsvFormatsSettings) super.clone();
            copy.formats = new ArrayList<>();
            for (CsvFormat f : this.formats) {
                copy.formats.add(f.clone());
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CsvFormatsSettings that = (CsvFormatsSettings) o;
        return Objects.equals(formats, that.formats) &&
                Objects.equals(selectedFormatName, that.selectedFormatName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formats, selectedFormatName);
    }
}
