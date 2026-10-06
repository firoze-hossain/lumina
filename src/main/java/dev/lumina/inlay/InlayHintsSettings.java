package dev.lumina.inlay;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration options for Inlay Hints, mirroring IntelliJ IDEA's
 * Editor > Inlay Hints settings.
 */
public class InlayHintsSettings {

    private String codeVisionDefaultPosition = "Right";
    private int codeVisionMaxAbove = 5;
    private int codeVisionMaxNext = 5;

    private Map<String, String> itemPositions = new HashMap<>();
    private Map<String, Boolean> enabledHints = new HashMap<>();

    public InlayHintsSettings() {
        initDefaults();
    }

    public void initDefaults() {
        codeVisionDefaultPosition = "Right";
        codeVisionMaxAbove = 5;
        codeVisionMaxNext = 5;
        itemPositions.clear();
        enabledHints.clear();

        // Code vision defaults (all enabled)
        enabledHints.put("codevision", true);
        enabledHints.put("codevision.rename", true);
        enabledHints.put("codevision.inheritors", true);
        enabledHints.put("codevision.related_problems", true);
        enabledHints.put("codevision.usages", true);
        enabledHints.put("codevision.lsp_code_lens", true);
        enabledHints.put("codevision.component_usages", true);
        enabledHints.put("codevision.java_configuration", true);
        enabledHints.put("codevision.kotlin_script", true);
        enabledHints.put("codevision.change_signature", true);
        enabledHints.put("codevision.code_author", true);

        // Parameter names defaults
        enabledHints.put("param", true);
        enabledHints.put("param.java", true);
        enabledHints.put("param.java.reflected", false);
        enabledHints.put("param.java.multiple_non_literal", false);
        enabledHints.put("param.java.numbered", false);
        enabledHints.put("param.java.enum_constant", true);
        enabledHints.put("param.java.new_expressions", true);
        enabledHints.put("param.java.complex_expressions", false);

        enabledHints.put("param.kotlin", true);
        enabledHints.put("param.angular", true);
        enabledHints.put("param.groovy", true);
        enabledHints.put("param.javascript", true);
        enabledHints.put("param.sql", true);
        enabledHints.put("param.typescript", true);
        enabledHints.put("param.vue", true);

        // Types defaults
        enabledHints.put("types", true);
        enabledHints.put("types.java", true);
        enabledHints.put("types.kotlin", false);
        enabledHints.put("types.groovy", true);
        enabledHints.put("types.javascript", true);
        enabledHints.put("types.javascript.variables_and_fields", true);
        enabledHints.put("types.javascript.parameters_in_parentheses", true);
        enabledHints.put("types.javascript.non_parenthesized", true);
        enabledHints.put("types.javascript.function_return_types", true);
        enabledHints.put("types.typescript", true);
        enabledHints.put("types.typescript.variables_and_fields", true);
        enabledHints.put("types.typescript.parameters_in_parentheses", true);
        enabledHints.put("types.typescript.non_parenthesized", true);
        enabledHints.put("types.typescript.function_return_types", true);

        // Values defaults
        enabledHints.put("values", true);
        enabledHints.put("values.kotlin", true);
        enabledHints.put("values.groovy", true);
        enabledHints.put("values.typescript", true);

        // Annotations defaults
        enabledHints.put("annotations", true);
        enabledHints.put("annotations.java", true);

        // Method chains defaults
        enabledHints.put("method_chains", true);
        enabledHints.put("method_chains.kotlin", false);
        enabledHints.put("method_chains.javascript", true);
        enabledHints.put("method_chains.typescript", true);

        // Lambdas defaults
        enabledHints.put("lambdas", true);
        enabledHints.put("lambdas.kotlin", false);
        enabledHints.put("lambdas.groovy", true);

        // URL path defaults
        enabledHints.put("url_path", true);
        enabledHints.put("url_path.java", true);
        enabledHints.put("url_path.kotlin", true);
        enabledHints.put("url_path.gradle", true);
        enabledHints.put("url_path.groovy", true);
        enabledHints.put("url_path.xml", true);

        // Other defaults
        enabledHints.put("other", true);
        enabledHints.put("other.java", true);
        enabledHints.put("other.kotlin", true);
        enabledHints.put("other.dockerfile", true);
        enabledHints.put("other.gradle", true);
        enabledHints.put("other.groovy", true);
        enabledHints.put("other.markdown", true);
        enabledHints.put("other.properties", true);
        enabledHints.put("other.sql", true);
        enabledHints.put("other.xml", true);
        enabledHints.put("other.yaml", true);
    }

    public InlayHintsSettings copy() {
        InlayHintsSettings clone = new InlayHintsSettings();
        clone.codeVisionDefaultPosition = this.codeVisionDefaultPosition;
        clone.codeVisionMaxAbove = this.codeVisionMaxAbove;
        clone.codeVisionMaxNext = this.codeVisionMaxNext;
        clone.itemPositions = new HashMap<>(this.itemPositions);
        clone.enabledHints = new HashMap<>(this.enabledHints);
        return clone;
    }

    public boolean isEquivalentTo(InlayHintsSettings other) {
        if (other == null) return false;
        return Objects.equals(this.codeVisionDefaultPosition, other.codeVisionDefaultPosition)
                && this.codeVisionMaxAbove == other.codeVisionMaxAbove
                && this.codeVisionMaxNext == other.codeVisionMaxNext
                && Objects.equals(this.itemPositions, other.itemPositions)
                && Objects.equals(this.enabledHints, other.enabledHints);
    }

    public String getCodeVisionDefaultPosition() {
        return codeVisionDefaultPosition != null ? codeVisionDefaultPosition : "Right";
    }

    public void setCodeVisionDefaultPosition(String codeVisionDefaultPosition) {
        this.codeVisionDefaultPosition = codeVisionDefaultPosition;
    }

    public int getCodeVisionMaxAbove() {
        return codeVisionMaxAbove;
    }

    public void setCodeVisionMaxAbove(int codeVisionMaxAbove) {
        this.codeVisionMaxAbove = codeVisionMaxAbove;
    }

    public int getCodeVisionMaxNext() {
        return codeVisionMaxNext;
    }

    public void setCodeVisionMaxNext(int codeVisionMaxNext) {
        this.codeVisionMaxNext = codeVisionMaxNext;
    }

    public Map<String, String> getItemPositions() {
        return itemPositions;
    }

    public void setItemPositions(Map<String, String> itemPositions) {
        this.itemPositions = itemPositions != null ? new HashMap<>(itemPositions) : new HashMap<>();
    }

    public String getItemPosition(String itemId, String defaultVal) {
        return itemPositions.getOrDefault(itemId, defaultVal != null ? defaultVal : "Default");
    }

    public void setItemPosition(String itemId, String position) {
        if (itemId != null) {
            itemPositions.put(itemId, position);
        }
    }

    public Map<String, Boolean> getEnabledHints() {
        return enabledHints;
    }

    public void setEnabledHints(Map<String, Boolean> enabledHints) {
        this.enabledHints = enabledHints != null ? new HashMap<>(enabledHints) : new HashMap<>();
    }

    public boolean isHintEnabled(String hintId, boolean defaultValue) {
        return enabledHints.getOrDefault(hintId, defaultValue);
    }

    public void setHintEnabled(String hintId, boolean enabled) {
        if (hintId != null) {
            enabledHints.put(hintId, enabled);
        }
    }
}
