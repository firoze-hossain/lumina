package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Dedicated code style settings and dynamic provider for HTML.
 * Strictly decoupled and brand-isolated.
 * Supports the 4 tabs matching reference screenshots:
 * 1. Tabs and Indents (media_1790582764968.png)
 * 2. Other (media_1790582786285.png, media_1790582808184.png)
 * 3. Arrangement (media_1790582819747.png)
 * 4. Code Generation (media_1790582827960.png)
 */
public class HtmlCodeStyleSettings extends LanguageCodeStyleSettings {

    // ==========================================
    // Option Choice Lists
    // ==========================================
    public static final List<String> WRAP_ATTRIBUTES_OPTIONS = List.of(
            "Do not wrap",
            "Wrap if long",
            "Chop down if long",
            "Wrap always"
    );

    public static final List<String> WRAP_ON_TYPING_OPTIONS = List.of(
            "Default: No",
            "Yes",
            "No"
    );

    public static final List<String> ATTRIBUTE_NEW_LINE_OPTIONS = List.of(
            "Never",
            "If multiline",
            "Always"
    );

    public static final List<String> JSX_ATTRIBUTES_OPTIONS = List.of(
            "Braces",
            "Quotes",
            "Based on type"
    );

    public static final List<String> GENERATED_QUOTE_MARKS_OPTIONS = List.of(
            "Double",
            "Single",
            "None"
    );

    // ==========================================
    // Property Keys - Tabs and Indents
    // ==========================================
    public static final String USE_HTML_INDENTS_WITHIN_STYLE_AND_SCRIPT = "html_use_html_indents_within_style_and_script";

    // ==========================================
    // Property Keys - Other
    // ==========================================
    public static final String HARD_WRAP_AT = "html_hard_wrap_at";
    public static final String WRAP_ON_TYPING = "html_wrap_on_typing";
    public static final String VISUAL_GUIDES = "html_visual_guides";
    public static final String KEEP_LINE_BREAKS = "html_keep_line_breaks";
    public static final String KEEP_LINE_BREAKS_IN_TEXT = "html_keep_line_breaks_in_text";
    public static final String KEEP_BLANK_LINES = "html_keep_blank_lines";
    public static final String WRAP_ATTRIBUTES = "html_wrap_attributes";

    public static final String WRAP_TEXT = "html_wrap_text";
    public static final String ALIGN_ATTRIBUTES = "html_align_attributes";
    public static final String ALIGN_TEXT = "html_align_text";
    public static final String KEEP_WHITE_SPACES = "html_keep_white_spaces";

    public static final String SPACES_AROUND_EQUALITY_IN_ATTRIBUTE = "html_spaces_around_equality_in_attribute";
    public static final String SPACES_AFTER_TAG_NAME = "html_spaces_after_tag_name";
    public static final String SPACES_IN_EMPTY_TAG = "html_spaces_in_empty_tag";

    public static final String INSERT_NEW_LINE_BEFORE = "html_insert_new_line_before";
    public static final String REMOVE_NEW_LINE_BEFORE = "html_remove_new_line_before";
    public static final String DO_NOT_INDENT_CHILDREN_OF = "html_do_not_indent_children_of";
    public static final String DO_NOT_INDENT_TAG_SIZE_MORE_THAN = "html_do_not_indent_tag_size_more_than";
    public static final String INLINE_ELEMENTS = "html_inline_elements";
    public static final String KEEP_WHITE_SPACES_INSIDE = "html_keep_white_spaces_inside";
    public static final String DONT_BREAK_IF_INLINE_CONTENT = "html_dont_break_if_inline_content";

    public static final String NEW_LINE_BEFORE_FIRST_ATTRIBUTE = "html_new_line_before_first_attribute";
    public static final String NEW_LINE_AFTER_LAST_ATTRIBUTE = "html_new_line_after_last_attribute";
    public static final String ADD_FOR_JSX_ATTRIBUTES = "html_add_for_jsx_attributes";
    public static final String GENERATED_QUOTE_MARKS = "html_generated_quote_marks";
    public static final String ENFORCE_ON_FORMAT = "html_enforce_on_format";

    // ==========================================
    // Property Keys - Code Generation
    // ==========================================
    public static final String CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN = "html_code_gen_line_comment_at_first_column";
    public static final String CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN = "html_code_gen_block_comment_at_first_column";
    public static final String CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS = "html_code_gen_add_spaces_around_block_comments";

    // Arrangement Matching Rules
    private final List<String> matchingRules = new ArrayList<>();

    public HtmlCodeStyleSettings() {
        this("HTML");
    }

    public HtmlCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        // Tab 1: Tabs and Indents (media_1790582764968.png)
        setTabSize(4);
        setIndent(4);
        setContinuationIndent(8);
        setUseTabCharacter(false);
        setSmartTabs(false);
        setKeepIndentsOnEmptyLines(false);
        setBoolean(USE_HTML_INDENTS_WITHIN_STYLE_AND_SCRIPT, false);

        // Tab 2: Other (media_1790582786285.png, media_1790582808184.png)
        setInt(HARD_WRAP_AT, 120);
        setString(WRAP_ON_TYPING, "Default: No");
        setString(VISUAL_GUIDES, "");
        setBoolean(KEEP_LINE_BREAKS, true);
        setBoolean(KEEP_LINE_BREAKS_IN_TEXT, true);
        setInt(KEEP_BLANK_LINES, 2);
        setString(WRAP_ATTRIBUTES, "Wrap if long");

        setBoolean(WRAP_TEXT, true);
        setBoolean(ALIGN_ATTRIBUTES, true);
        setBoolean(ALIGN_TEXT, false);
        setBoolean(KEEP_WHITE_SPACES, false);

        setBoolean(SPACES_AROUND_EQUALITY_IN_ATTRIBUTE, false);
        setBoolean(SPACES_AFTER_TAG_NAME, false);
        setBoolean(SPACES_IN_EMPTY_TAG, false);

        setString(INSERT_NEW_LINE_BEFORE, "body,div,p,form,h1,h2,h3");
        setString(REMOVE_NEW_LINE_BEFORE, "br");
        setString(DO_NOT_INDENT_CHILDREN_OF, "html,body,thead,tbody,tfoot");
        setString(DO_NOT_INDENT_TAG_SIZE_MORE_THAN, "");
        setString(INLINE_ELEMENTS, "strong,sub,sup,textarea,tt,u,var");
        setString(KEEP_WHITE_SPACES_INSIDE, "span,pre,textarea");
        setString(DONT_BREAK_IF_INLINE_CONTENT, "title,h1,h2,h3,h4,h5,h6,p");

        setString(NEW_LINE_BEFORE_FIRST_ATTRIBUTE, "Never");
        setString(NEW_LINE_AFTER_LAST_ATTRIBUTE, "Never");
        setString(ADD_FOR_JSX_ATTRIBUTES, "Braces");
        setString(GENERATED_QUOTE_MARKS, "Double");
        setBoolean(ENFORCE_ON_FORMAT, false);

        // Tab 3: Arrangement (media_1790582819747.png)
        matchingRules.clear();
        matchingRules.add("attribute");

        // Tab 4: Code Generation (media_1790582827960.png)
        setBoolean(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, true);
        setBoolean(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, true);
        setBoolean(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, false);
    }

    @Override
    public HtmlCodeStyleSettings copy() {
        HtmlCodeStyleSettings copy = new HtmlCodeStyleSettings(getLanguageId());
        copy.setTabSize(getTabSize());
        copy.setIndent(getIndent());
        copy.setContinuationIndent(getContinuationIndent());
        copy.setUseTabCharacter(isUseTabCharacter());
        copy.setSmartTabs(isSmartTabs());
        copy.setKeepIndentsOnEmptyLines(isKeepIndentsOnEmptyLines());
        copy.setProperties(getAllProperties());

        copy.matchingRules.clear();
        copy.matchingRules.addAll(this.matchingRules);

        return copy;
    }

    // ==========================================
    // Getters and Setters
    // ==========================================

    public boolean isUseHtmlIndentsWithinStyleAndScript() {
        return getBoolean(USE_HTML_INDENTS_WITHIN_STYLE_AND_SCRIPT, false);
    }

    public void setUseHtmlIndentsWithinStyleAndScript(boolean v) {
        setBoolean(USE_HTML_INDENTS_WITHIN_STYLE_AND_SCRIPT, v);
    }

    public int getHardWrapAt() {
        return getInt(HARD_WRAP_AT, 120);
    }

    public void setHardWrapAt(int v) {
        setInt(HARD_WRAP_AT, v);
    }

    public String getWrapOnTyping() {
        return getString(WRAP_ON_TYPING, "Default: No");
    }

    public void setWrapOnTyping(String v) {
        setString(WRAP_ON_TYPING, v);
    }

    public String getVisualGuides() {
        return getString(VISUAL_GUIDES, "");
    }

    public void setVisualGuides(String v) {
        setString(VISUAL_GUIDES, v);
    }

    public boolean isKeepLineBreaks() {
        return getBoolean(KEEP_LINE_BREAKS, true);
    }

    public void setKeepLineBreaks(boolean v) {
        setBoolean(KEEP_LINE_BREAKS, v);
    }

    public boolean isKeepLineBreaksInText() {
        return getBoolean(KEEP_LINE_BREAKS_IN_TEXT, true);
    }

    public void setKeepLineBreaksInText(boolean v) {
        setBoolean(KEEP_LINE_BREAKS_IN_TEXT, v);
    }

    public int getKeepBlankLines() {
        return getInt(KEEP_BLANK_LINES, 2);
    }

    public void setKeepBlankLines(int v) {
        setInt(KEEP_BLANK_LINES, v);
    }

    public String getWrapAttributes() {
        return getString(WRAP_ATTRIBUTES, "Wrap if long");
    }

    public void setWrapAttributes(String v) {
        setString(WRAP_ATTRIBUTES, v);
    }

    public boolean isWrapText() {
        return getBoolean(WRAP_TEXT, true);
    }

    public void setWrapText(boolean v) {
        setBoolean(WRAP_TEXT, v);
    }

    public boolean isAlignAttributes() {
        return getBoolean(ALIGN_ATTRIBUTES, true);
    }

    public void setAlignAttributes(boolean v) {
        setBoolean(ALIGN_ATTRIBUTES, v);
    }

    public boolean isAlignText() {
        return getBoolean(ALIGN_TEXT, false);
    }

    public void setAlignText(boolean v) {
        setBoolean(ALIGN_TEXT, v);
    }

    public boolean isKeepWhiteSpaces() {
        return getBoolean(KEEP_WHITE_SPACES, false);
    }

    public void setKeepWhiteSpaces(boolean v) {
        setBoolean(KEEP_WHITE_SPACES, v);
    }

    public boolean isSpacesAroundEqualityInAttribute() {
        return getBoolean(SPACES_AROUND_EQUALITY_IN_ATTRIBUTE, false);
    }

    public void setSpacesAroundEqualityInAttribute(boolean v) {
        setBoolean(SPACES_AROUND_EQUALITY_IN_ATTRIBUTE, v);
    }

    public boolean isSpacesAfterTagName() {
        return getBoolean(SPACES_AFTER_TAG_NAME, false);
    }

    public void setSpacesAfterTagName(boolean v) {
        setBoolean(SPACES_AFTER_TAG_NAME, v);
    }

    public boolean isSpacesInEmptyTag() {
        return getBoolean(SPACES_IN_EMPTY_TAG, false);
    }

    public void setSpacesInEmptyTag(boolean v) {
        setBoolean(SPACES_IN_EMPTY_TAG, v);
    }

    public String getInsertNewLineBefore() {
        return getString(INSERT_NEW_LINE_BEFORE, "body,div,p,form,h1,h2,h3");
    }

    public void setInsertNewLineBefore(String v) {
        setString(INSERT_NEW_LINE_BEFORE, v);
    }

    public String getRemoveNewLineBefore() {
        return getString(REMOVE_NEW_LINE_BEFORE, "br");
    }

    public void setRemoveNewLineBefore(String v) {
        setString(REMOVE_NEW_LINE_BEFORE, v);
    }

    public String getDoNotIndentChildrenOf() {
        return getString(DO_NOT_INDENT_CHILDREN_OF, "html,body,thead,tbody,tfoot");
    }

    public void setDoNotIndentChildrenOf(String v) {
        setString(DO_NOT_INDENT_CHILDREN_OF, v);
    }

    public String getDoNotIndentTagSizeMoreThan() {
        return getString(DO_NOT_INDENT_TAG_SIZE_MORE_THAN, "");
    }

    public void setDoNotIndentTagSizeMoreThan(String v) {
        setString(DO_NOT_INDENT_TAG_SIZE_MORE_THAN, v);
    }

    public String getInlineElements() {
        return getString(INLINE_ELEMENTS, "strong,sub,sup,textarea,tt,u,var");
    }

    public void setInlineElements(String v) {
        setString(INLINE_ELEMENTS, v);
    }

    public String getKeepWhiteSpacesInside() {
        return getString(KEEP_WHITE_SPACES_INSIDE, "span,pre,textarea");
    }

    public void setKeepWhiteSpacesInside(String v) {
        setString(KEEP_WHITE_SPACES_INSIDE, v);
    }

    public String getDontBreakIfInlineContent() {
        return getString(DONT_BREAK_IF_INLINE_CONTENT, "title,h1,h2,h3,h4,h5,h6,p");
    }

    public void setDontBreakIfInlineContent(String v) {
        setString(DONT_BREAK_IF_INLINE_CONTENT, v);
    }

    public String getNewLineBeforeFirstAttribute() {
        return getString(NEW_LINE_BEFORE_FIRST_ATTRIBUTE, "Never");
    }

    public void setNewLineBeforeFirstAttribute(String v) {
        setString(NEW_LINE_BEFORE_FIRST_ATTRIBUTE, v);
    }

    public String getNewLineAfterLastAttribute() {
        return getString(NEW_LINE_AFTER_LAST_ATTRIBUTE, "Never");
    }

    public void setNewLineAfterLastAttribute(String v) {
        setString(NEW_LINE_AFTER_LAST_ATTRIBUTE, v);
    }

    public String getAddForJsxAttributes() {
        return getString(ADD_FOR_JSX_ATTRIBUTES, "Braces");
    }

    public void setAddForJsxAttributes(String v) {
        setString(ADD_FOR_JSX_ATTRIBUTES, v);
    }

    public String getGeneratedQuoteMarks() {
        return getString(GENERATED_QUOTE_MARKS, "Double");
    }

    public void setGeneratedQuoteMarks(String v) {
        setString(GENERATED_QUOTE_MARKS, v);
    }

    public boolean isEnforceOnFormat() {
        return getBoolean(ENFORCE_ON_FORMAT, false);
    }

    public void setEnforceOnFormat(boolean v) {
        setBoolean(ENFORCE_ON_FORMAT, v);
    }

    public List<String> getMatchingRules() {
        return matchingRules;
    }

    // ==========================================
    // Sample Codes matching media_1790582764968.png & media_1790582786285.png verbatim
    // Strictly brand-isolated.
    // ==========================================
    public static final String SAMPLE_TABS_AND_INDENTS = """
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Lumina: The Smartest Modern IDE</title>
    <meta charset="utf-8">
    <meta http-equiv="x-ua-compatible"
          content="IE=edge">
    <meta
            name="viewport"
            content="width=device-width, maximum-scale=1">
    <link href="/favicon-32x32.png" rel="icon" sizes="32x32" type="image/png">
    <link rel="canonical" href="https://dev.lumina/ide/"><!-- ...117-->
    <meta class="local"
          content="A powerful IDE for modern software development with code completion and refactoring"
          name="description">
    <script>
        var current_lang = 'en-us';
        var i18n_info = {
            "current_lang": "en-us", "languages": [{
                "code": "en-us", "label": "English"
            }, {
                "code": "de-de", "label": "Deutsch"
            }]
        };
        var english_only_url_prefixes = [];
    </script>
    <link href="/_assets/common.css" rel="stylesheet" type="text/css">
    <style>
        .page__beam {
            -webkit-transform: translate(-40px, -700px);
            transform: translate(-40px, -700px);
        }

        .page__beam svg {
            position: absolute;
            left: 50%;
        }
    </style>
</head>
<body>
</body>
</html>
""";

    public static final String SAMPLE_OTHER = """
<!DOCTYPE html PUBLIC
        "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en"
      xml:lang="en">
<head>
    <title>Lumina: The Most Intelligent Modern IDE</title>
    <meta http-equiv="Content-Type"
          content="text/html; charset=iso-8859-1"/>
    <link rel="stylesheet" type="text/css" media="screen"
          href="../css/main.css"/>
    <link rel="stylesheet" type="text/css" media="screen"
          href="../css/screen.css"/>
    <link rel="stylesheet" type="text/css" media="print"
          href="../css/print.css"/>
    <link rel="Shortcut Icon" href="../favicon.ico"
          type="image/x-icon"/>

</head>

<body class="luminabg">
<div id="container">

    <div id='top'>
        <div id='logo'><a href="../index.html"><img
                src="../img/logo.gif" width="124"
                height="44" alt="Lumina home"/></a></div>

        <div id="nav">
            <ul id="topnav">
                <li class="home"><a href="../index.html">Home</a>
                </li>
                <li class="act"><a href="../products.html">Products</a>
                </li>
                <li><a href="../support">Support</a></li>
            </ul>
        </div>
    </div>
</div>
</body>
</html>
""";

    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "HTML";
            }

            @Override
            public String getDisplayName() {
                return "HTML";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents", "Other", "Arrangement", "Code Generation");
            }

            @Override
            public boolean hasPreview(String tabName) {
                // Tabs and Indents and Other have live previews; Arrangement and Code Generation take full width
                return !"Arrangement".equals(tabName) && !"Code Generation".equals(tabName);
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new HtmlCodeStyleSettings("HTML");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_TABS_AND_INDENTS;
            }

            @Override
            public String getSampleCode(String tabName) {
                if ("Other".equals(tabName)) {
                    return SAMPLE_OTHER;
                }
                return SAMPLE_TABS_AND_INDENTS;
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Tabs and Indents".equals(tabName)) {
                    CodeStyleGroup indents = CodeStyleGroup.flat("Tabs and Indents");
                    indents.addOption(CodeStyleOption.checkbox("use_tab_character", "Use tab character", false));
                    indents.addOption(CodeStyleOption.indentedCheckbox("smart_tabs", "Smart tabs", false));
                    indents.addOption(CodeStyleOption.number("tab_size", "Tab size:", 4));
                    indents.addOption(CodeStyleOption.number("indent", "Indent:", 4));
                    indents.addOption(CodeStyleOption.number("continuation_indent", "Continuation indent:", 8));
                    indents.addOption(CodeStyleOption.checkbox("keep_indents_on_empty_lines", "Keep indents on empty lines", false));
                    indents.addOption(CodeStyleOption.checkbox(USE_HTML_INDENTS_WITHIN_STYLE_AND_SCRIPT, "Use HTML indents within <style> and <script> tags", false));
                    customizer.addGroup(indents);

                } else if ("Code Generation".equals(tabName)) {
                    CodeStyleGroup comments = CodeStyleGroup.divider("Comments");
                    comments.addOption(CodeStyleOption.checkbox(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, "Line comment at first column", true));
                    comments.addOption(CodeStyleOption.checkbox(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, "Block comment at first column", true));
                    comments.addOption(CodeStyleOption.checkbox(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, "Add spaces around block comments", false));
                    customizer.addGroup(comments);
                }
            }
        };
    }
}
