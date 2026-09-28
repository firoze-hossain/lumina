package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.List;

/**
 * Dedicated code style settings and dynamic provider for ERB.
 * Strictly decoupled and brand-isolated.
 * Supports exactly 1 tab matching reference screenshot media_1790578204402.png:
 * - Tabs and Indents
 */
public class ErbCodeStyleSettings extends LanguageCodeStyleSettings {

    public ErbCodeStyleSettings() {
        this("ERB");
    }

    public ErbCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        setTabSize(2);
        setIndent(2);
        setContinuationIndent(2);
        setUseTabCharacter(false);
        setSmartTabs(false);
        setKeepIndentsOnEmptyLines(false);
    }

    @Override
    public ErbCodeStyleSettings copy() {
        ErbCodeStyleSettings copy = new ErbCodeStyleSettings(getLanguageId());
        copy.setTabSize(getTabSize());
        copy.setIndent(getIndent());
        copy.setContinuationIndent(getContinuationIndent());
        copy.setUseTabCharacter(isUseTabCharacter());
        copy.setSmartTabs(isSmartTabs());
        copy.setKeepIndentsOnEmptyLines(isKeepIndentsOnEmptyLines());
        copy.setProperties(getAllProperties());
        return copy;
    }

    // ==========================================
    // Sample Code matching media_1790578204402.png verbatim
    // ==========================================
    public static final String SAMPLE_ERB = """
<p id="notice"><%= notice %></p>

<h1>Listing Shops</h1>

<table>
  <thead>
    <tr>
      <th>Title</th>
      <th colspan="3"></th>
    </tr>
  </thead>

  <tbody>
    <% @shops.each do |shop| %>
      <tr>
        <td><%= shop.title %></td>
        <td><%= link_to 'Show', shop %></td>
        <td><%= link_to 'Edit', edit_shop_path(shop) %></td>
        <td><%= link_to 'Destroy', shop, method: :delete, data: { confirm: 'Are you sure?' } %></td>
      </tr>
    <% end %>
  </tbody>
</table>

<br>

<%= link_to 'New Shop', new_shop_path %>
""";

    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "ERB";
            }

            @Override
            public String getDisplayName() {
                return "ERB";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents");
            }

            @Override
            public boolean hasPreview(String tabName) {
                return true;
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new ErbCodeStyleSettings("ERB");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_ERB;
            }

            @Override
            public String getSampleCode(String tabName) {
                return SAMPLE_ERB;
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Tabs and Indents".equals(tabName)) {
                    CodeStyleGroup g = CodeStyleGroup.flat("Tabs and Indents");
                    g.addOption(CodeStyleOption.checkbox("use_tab_character", "Use tab character", false));
                    g.addOption(CodeStyleOption.indentedCheckbox("smart_tabs", "Smart tabs", false));
                    g.addOption(CodeStyleOption.number("tab_size", "Tab size:", 2));
                    g.addOption(CodeStyleOption.number("indent", "Indent:", 2));
                    g.addOption(CodeStyleOption.number("continuation_indent", "Continuation indent:", 2));
                    g.addOption(CodeStyleOption.checkbox("keep_indents_on_empty_lines", "Keep indents on empty lines", false));
                    customizer.addGroup(g);
                }
            }
        };
    }
}
