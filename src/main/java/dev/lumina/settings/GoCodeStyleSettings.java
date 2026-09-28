package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated code style settings and dynamic provider for Go.
 * Strictly decoupled and brand-isolated.
 * Supports the 4 tabs matching reference screenshots:
 * 1. Tabs and Indents (media_1790578230039.png)
 * 2. Wrapping and Braces (media_1790578216851.png)
 * 3. Imports (media_1790578241168.png)
 * 4. Other (media_1790578252165.png)
 */
public class GoCodeStyleSettings extends LanguageCodeStyleSettings {

    public static final List<String> WRAP_OPTIONS = List.of(
            "Do not wrap",
            "Wrap if long",
            "Chop down if long",
            "Wrap always"
    );

    // ==========================================
    // Property Keys - Wrapping and Braces
    // ==========================================
    public static final String WRAP_HARD_WRAP_AT = "go_wrap_hard_wrap_at";
    public static final String WRAP_ON_TYPING = "go_wrap_on_typing";
    public static final String WRAP_VISUAL_GUIDES = "go_wrap_visual_guides";

    public static final String WRAP_CALL_ARGUMENTS = "go_wrap_call_arguments";
    public static final String WRAP_CALL_ARGUMENTS_NEW_LINE_AFTER_LPAREN = "go_wrap_call_args_nl_after_lparen";
    public static final String WRAP_CALL_ARGUMENTS_RPAREN_ON_NEW_LINE = "go_wrap_call_args_rparen_nl";

    public static final String WRAP_COMPOSITE_LITERALS = "go_wrap_composite_literals";
    public static final String WRAP_COMPOSITE_LITERALS_NEW_LINE_AFTER_LBRACE = "go_wrap_composite_nl_after_lbrace";
    public static final String WRAP_COMPOSITE_LITERALS_RBRACE_ON_NEW_LINE = "go_wrap_composite_rbrace_nl";

    public static final String WRAP_FUNCTION_PARAMETERS = "go_wrap_function_parameters";
    public static final String WRAP_FUNCTION_PARAMETERS_NEW_LINE_AFTER_LPAREN = "go_wrap_fn_params_nl_after_lparen";
    public static final String WRAP_FUNCTION_PARAMETERS_RPAREN_ON_NEW_LINE = "go_wrap_fn_params_rparen_nl";

    public static final String WRAP_FUNCTION_RESULT_PARAMETERS = "go_wrap_function_result_parameters";
    public static final String WRAP_FUNCTION_RESULT_PARAMETERS_NEW_LINE_AFTER_LPAREN = "go_wrap_fn_res_nl_after_lparen";
    public static final String WRAP_FUNCTION_RESULT_PARAMETERS_RPAREN_ON_NEW_LINE = "go_wrap_fn_res_rparen_nl";

    // ==========================================
    // Property Keys - Imports
    // ==========================================
    public static final String IMPORTS_USE_BACKQUOTES = "go_imports_use_backquotes";
    public static final String IMPORTS_ADD_PARENTHESES_SINGLE = "go_imports_add_parentheses_single";
    public static final String IMPORTS_REMOVE_REDUNDANT_ALIASES = "go_imports_remove_redundant_aliases";
    public static final String IMPORTS_SORTING_TYPE = "go_imports_sorting_type";
    public static final String IMPORTS_MOVE_ALL_SINGLE_DECLARATION = "go_imports_move_all_single_declaration";
    public static final String IMPORTS_GROUP_SDK_PACKAGES = "go_imports_group_sdk_packages";
    public static final String IMPORTS_MOVE_ALL_SINGLE_GROUP = "go_imports_move_all_single_group";
    public static final String IMPORTS_GROUP_ENABLED = "go_imports_group_enabled";
    public static final String IMPORTS_GROUP_MODE = "go_imports_group_mode"; // "PROJECT" or "PREFIXES"
    public static final String IMPORTS_CUSTOM_PREFIXES = "go_imports_custom_prefixes";

    // ==========================================
    // Property Keys - Other
    // ==========================================
    public static final String OTHER_ADD_LEADING_SPACE_COMMENTS = "go_other_add_leading_space_comments";
    public static final String OTHER_COLUMN_WIDTH_FILL_PARAGRAPH = "go_other_column_width_fill_paragraph";
    public static final String OTHER_RUN_GOFMT_ON_REFORMAT = "go_other_run_gofmt_on_reformat";

    private final List<String> commentExceptions = new ArrayList<>();

    public GoCodeStyleSettings() {
        this("Go");
    }

    public GoCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        // Tabs and Indents (media_1790578230039.png)
        setTabSize(4);
        setIndent(4);
        setContinuationIndent(4);
        setUseTabCharacter(true);
        setSmartTabs(false);
        setKeepIndentsOnEmptyLines(false);

        // Wrapping and Braces (media_1790578216851.png)
        setString(WRAP_HARD_WRAP_AT, "Default: 120");
        setString(WRAP_ON_TYPING, "Default: No");
        setString(WRAP_VISUAL_GUIDES, "Default: None");

        setString(WRAP_CALL_ARGUMENTS, "Do not wrap");
        setBoolean(WRAP_CALL_ARGUMENTS_NEW_LINE_AFTER_LPAREN, true);
        setBoolean(WRAP_CALL_ARGUMENTS_RPAREN_ON_NEW_LINE, true);

        setString(WRAP_COMPOSITE_LITERALS, "Do not wrap");
        setBoolean(WRAP_COMPOSITE_LITERALS_NEW_LINE_AFTER_LBRACE, true);
        setBoolean(WRAP_COMPOSITE_LITERALS_RBRACE_ON_NEW_LINE, true);

        setString(WRAP_FUNCTION_PARAMETERS, "Do not wrap");
        setBoolean(WRAP_FUNCTION_PARAMETERS_NEW_LINE_AFTER_LPAREN, true);
        setBoolean(WRAP_FUNCTION_PARAMETERS_RPAREN_ON_NEW_LINE, true);

        setString(WRAP_FUNCTION_RESULT_PARAMETERS, "Do not wrap");
        setBoolean(WRAP_FUNCTION_RESULT_PARAMETERS_NEW_LINE_AFTER_LPAREN, true);
        setBoolean(WRAP_FUNCTION_RESULT_PARAMETERS_RPAREN_ON_NEW_LINE, true);

        // Imports (media_1790578241168.png)
        setBoolean(IMPORTS_USE_BACKQUOTES, false);
        setBoolean(IMPORTS_ADD_PARENTHESES_SINGLE, false);
        setBoolean(IMPORTS_REMOVE_REDUNDANT_ALIASES, false);
        setString(IMPORTS_SORTING_TYPE, "goimports");
        setBoolean(IMPORTS_MOVE_ALL_SINGLE_DECLARATION, false);
        setBoolean(IMPORTS_GROUP_SDK_PACKAGES, false);
        setBoolean(IMPORTS_MOVE_ALL_SINGLE_GROUP, false);
        setBoolean(IMPORTS_GROUP_ENABLED, false);
        setString(IMPORTS_GROUP_MODE, "PROJECT");
        setString(IMPORTS_CUSTOM_PREFIXES, "");

        // Other (media_1790578252165.png)
        setBoolean(OTHER_ADD_LEADING_SPACE_COMMENTS, false);
        commentExceptions.clear();
        setInt(OTHER_COLUMN_WIDTH_FILL_PARAGRAPH, 80);
        setBoolean(OTHER_RUN_GOFMT_ON_REFORMAT, true);
    }

    @Override
    public GoCodeStyleSettings copy() {
        GoCodeStyleSettings copy = new GoCodeStyleSettings(getLanguageId());
        copy.setTabSize(getTabSize());
        copy.setIndent(getIndent());
        copy.setContinuationIndent(getContinuationIndent());
        copy.setUseTabCharacter(isUseTabCharacter());
        copy.setSmartTabs(isSmartTabs());
        copy.setKeepIndentsOnEmptyLines(isKeepIndentsOnEmptyLines());
        copy.setProperties(getAllProperties());

        copy.commentExceptions.clear();
        copy.commentExceptions.addAll(this.commentExceptions);

        return copy;
    }

    public List<String> getCommentExceptions() {
        return commentExceptions;
    }

    // ==========================================
    // Sample Codes matching reference screenshots verbatim
    // ==========================================
    public static final String SAMPLE_TABS_AND_INDENTS = """
package main

import "fmt"

func main() {
	fmt.Println("Hello")
}
""";

    public static final String SAMPLE_WRAPPING_AND_BRACES = """
package main

func f() {
	println(100, 101, 102, 103, 104, 105)
}

func g() {
	_ = []int{100, 101, 102, 103, 104, 105}
}

func h(p1 int, p2 int, p3 int, p4 int, p5 int, p6 int) {
}

func j() (r1 int, r2 int, r3 int, r4 int, r5 int, r6 int) {
	return 0, 0, 0, 0, 0, 0
}
""";

    public static final String SAMPLE_IMPORTS = """
package main

import (
	"bytes"
	"fmt"
	"localPackage"

	"golang.org/x/tools/cmd/gotype"

	"appengine"

	"errors"
)
import "github.com/dlsniper/go-metrics"
""";

    public static final String SAMPLE_OTHER = """
package foo

import "fmt"

//Foo docs
func Foo() {
	fmt.Println("Hello")
}
""";

    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "Go";
            }

            @Override
            public String getDisplayName() {
                return "Go";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents", "Wrapping and Braces", "Imports", "Other");
            }

            @Override
            public boolean hasPreview(String tabName) {
                return true;
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new GoCodeStyleSettings("Go");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_TABS_AND_INDENTS;
            }

            @Override
            public String getSampleCode(String tabName) {
                if ("Wrapping and Braces".equals(tabName)) return SAMPLE_WRAPPING_AND_BRACES;
                if ("Imports".equals(tabName)) return SAMPLE_IMPORTS;
                if ("Other".equals(tabName)) return SAMPLE_OTHER;
                return SAMPLE_TABS_AND_INDENTS;
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Tabs and Indents".equals(tabName)) {
                    CodeStyleGroup g = CodeStyleGroup.flat("Tabs and Indents");
                    g.addOption(CodeStyleOption.checkbox("use_tab_character", "Use tab character", true));
                    g.addOption(CodeStyleOption.indentedCheckbox("smart_tabs", "Smart tabs", false));
                    g.addOption(CodeStyleOption.number("tab_size", "Tab size:", 4));
                    g.addOption(CodeStyleOption.number("indent", "Indent:", 4));
                    g.addOption(CodeStyleOption.number("continuation_indent", "Continuation indent:", 4));
                    g.addOption(CodeStyleOption.checkbox("keep_indents_on_empty_lines", "Keep indents on empty lines", false));
                    customizer.addGroup(g);

                } else if ("Wrapping and Braces".equals(tabName)) {
                    CodeStyleGroup general = CodeStyleGroup.flat("General");
                    general.addOption(CodeStyleOption.combo(
                            WRAP_HARD_WRAP_AT,
                            "Hard wrap at:",
                            List.of("Default: 120", "None", "80", "100", "120", "140"),
                            "Default: 120"
                    ));
                    general.addOption(CodeStyleOption.combo(
                            WRAP_ON_TYPING,
                            "Wrap on typing",
                            List.of("Default: No", "Yes", "No"),
                            "Default: No"
                    ));
                    general.addOption(CodeStyleOption.combo(
                            WRAP_VISUAL_GUIDES,
                            "Visual guides",
                            List.of("Default: None", "None", "80", "120"),
                            "Default: None"
                    ));
                    customizer.addGroup(general);

                    CodeStyleGroup callArgs = CodeStyleGroup.collapsibleWithCombo(
                            "Function call arguments",
                            WRAP_CALL_ARGUMENTS,
                            WRAP_OPTIONS,
                            "Do not wrap"
                    );
                    callArgs.addOption(CodeStyleOption.checkbox(
                            WRAP_CALL_ARGUMENTS_NEW_LINE_AFTER_LPAREN,
                            "New line after '('",
                            true
                    ));
                    callArgs.addOption(CodeStyleOption.checkbox(
                            WRAP_CALL_ARGUMENTS_RPAREN_ON_NEW_LINE,
                            "Place ')' on new line",
                            true
                    ));
                    customizer.addGroup(callArgs);

                    CodeStyleGroup compLit = CodeStyleGroup.collapsibleWithCombo(
                            "Composite literals",
                            WRAP_COMPOSITE_LITERALS,
                            WRAP_OPTIONS,
                            "Do not wrap"
                    );
                    compLit.addOption(CodeStyleOption.checkbox(
                            WRAP_COMPOSITE_LITERALS_NEW_LINE_AFTER_LBRACE,
                            "New line after '{'",
                            true
                    ));
                    compLit.addOption(CodeStyleOption.checkbox(
                            WRAP_COMPOSITE_LITERALS_RBRACE_ON_NEW_LINE,
                            "Place '}' on new line",
                            true
                    ));
                    customizer.addGroup(compLit);

                    CodeStyleGroup fnParams = CodeStyleGroup.collapsibleWithCombo(
                            "Function parameters",
                            WRAP_FUNCTION_PARAMETERS,
                            WRAP_OPTIONS,
                            "Do not wrap"
                    );
                    fnParams.addOption(CodeStyleOption.checkbox(
                            WRAP_FUNCTION_PARAMETERS_NEW_LINE_AFTER_LPAREN,
                            "New line after '('",
                            true
                    ));
                    fnParams.addOption(CodeStyleOption.checkbox(
                            WRAP_FUNCTION_PARAMETERS_RPAREN_ON_NEW_LINE,
                            "Place ')' on new line",
                            true
                    ));
                    customizer.addGroup(fnParams);

                    CodeStyleGroup fnRes = CodeStyleGroup.collapsibleWithCombo(
                            "Function result parameters",
                            WRAP_FUNCTION_RESULT_PARAMETERS,
                            WRAP_OPTIONS,
                            "Do not wrap"
                    );
                    fnRes.addOption(CodeStyleOption.checkbox(
                            WRAP_FUNCTION_RESULT_PARAMETERS_NEW_LINE_AFTER_LPAREN,
                            "New line after '('",
                            true
                    ));
                    fnRes.addOption(CodeStyleOption.checkbox(
                            WRAP_FUNCTION_RESULT_PARAMETERS_RPAREN_ON_NEW_LINE,
                            "Place ')' on new line",
                            true
                    ));
                    customizer.addGroup(fnRes);
                }
            }
        };
    }
}
