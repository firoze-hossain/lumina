package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a configurable delimiter-separated values (CSV/TSV/DSV) format in Lumina IDE.
 */
public class CsvFormat implements Cloneable {

    private String name;
    private String valueSeparator = "Comma";
    private String rowSeparator = "Newline";
    private String nullValueText = "Empty string";
    private List<String> quotationRules = new ArrayList<>();
    private String quoteValues = "When needed";
    private boolean trimWhitespaces = false;
    private boolean firstRowIsHeader = false;
    private boolean firstColumnIsHeader = false;

    public CsvFormat() {
    }

    public CsvFormat(String name, String valueSeparator) {
        this.name = name;
        this.valueSeparator = valueSeparator;
        initDefaultQuotationRules();
    }

    public void initDefaultQuotationRules() {
        quotationRules.clear();
        quotationRules.add("\"   \"   Escape: duplicate");
        quotationRules.add("'   '   Escape: duplicate");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValueSeparator() {
        return valueSeparator;
    }

    public void setValueSeparator(String valueSeparator) {
        this.valueSeparator = valueSeparator != null ? valueSeparator : "Comma";
    }

    public String getDelimiterChar() {
        if ("Tab".equalsIgnoreCase(valueSeparator)) return "\t";
        if ("Semicolon".equalsIgnoreCase(valueSeparator)) return ";";
        if ("Pipe".equalsIgnoreCase(valueSeparator)) return "|";
        if ("Space".equalsIgnoreCase(valueSeparator)) return " ";
        return ",";
    }

    public String getRowSeparator() {
        return rowSeparator;
    }

    public void setRowSeparator(String rowSeparator) {
        this.rowSeparator = rowSeparator != null ? rowSeparator : "Newline";
    }

    public String getNullValueText() {
        return nullValueText;
    }

    public void setNullValueText(String nullValueText) {
        this.nullValueText = nullValueText != null ? nullValueText : "Empty string";
    }

    public List<String> getQuotationRules() {
        return quotationRules;
    }

    public void setQuotationRules(List<String> quotationRules) {
        this.quotationRules = quotationRules != null ? new ArrayList<>(quotationRules) : new ArrayList<>();
    }

    public String getQuoteValues() {
        return quoteValues;
    }

    public void setQuoteValues(String quoteValues) {
        this.quoteValues = quoteValues != null ? quoteValues : "When needed";
    }

    public boolean isTrimWhitespaces() {
        return trimWhitespaces;
    }

    public void setTrimWhitespaces(boolean trimWhitespaces) {
        this.trimWhitespaces = trimWhitespaces;
    }

    public boolean isFirstRowIsHeader() {
        return firstRowIsHeader;
    }

    public void setFirstRowIsHeader(boolean firstRowIsHeader) {
        this.firstRowIsHeader = firstRowIsHeader;
    }

    public boolean isFirstColumnIsHeader() {
        return firstColumnIsHeader;
    }

    public void setFirstColumnIsHeader(boolean firstColumnIsHeader) {
        this.firstColumnIsHeader = firstColumnIsHeader;
    }

    @Override
    public CsvFormat clone() {
        try {
            CsvFormat copy = (CsvFormat) super.clone();
            copy.quotationRules = new ArrayList<>(this.quotationRules);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CsvFormat csvFormat = (CsvFormat) o;
        return trimWhitespaces == csvFormat.trimWhitespaces &&
                firstRowIsHeader == csvFormat.firstRowIsHeader &&
                firstColumnIsHeader == csvFormat.firstColumnIsHeader &&
                Objects.equals(name, csvFormat.name) &&
                Objects.equals(valueSeparator, csvFormat.valueSeparator) &&
                Objects.equals(rowSeparator, csvFormat.rowSeparator) &&
                Objects.equals(nullValueText, csvFormat.nullValueText) &&
                Objects.equals(quotationRules, csvFormat.quotationRules) &&
                Objects.equals(quoteValues, csvFormat.quoteValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, valueSeparator, rowSeparator, nullValueText, quotationRules,
                quoteValues, trimWhitespaces, firstRowIsHeader, firstColumnIsHeader);
    }
}
